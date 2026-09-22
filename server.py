from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import subprocess
import urllib.parse
import threading
import time
from collections import deque

app = FastAPI()

# Shared Stream Relay:
# one source/ffmpeg process is shared by all listeners requesting the same
# source + output profile. When the last listener leaves, the relay is stopped.
class SharedRelay:
    def __init__(self, source_url: str, kbps: int, codec: str, channels: int, sample_rate: int):
        self.source_url = source_url
        self.kbps = kbps
        self.codec = codec
        self.channels = channels
        self.sample_rate = sample_rate
        self.process = None
        self.buffer = deque(maxlen=256)  # (sequence, bytes)
        self.sequence = 0
        self.condition = threading.Condition()
        self.clients = 0
        self.last_client_time = time.time()
        self.running = False
        self.thread = None
        self.media_type = "audio/ogg" if codec == "opus" else "audio/aac"

    def start(self):
        with self.condition:
            if self.running:
                return
            if self.codec == "opus":
                command = [
                    "ffmpeg", "-hide_banner", "-loglevel", "error",
                    "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5",
                    "-i", self.source_url,
                    "-vn", "-c:a", "libopus", "-b:a", f"{self.kbps}k",
                    "-ac", str(self.channels), "-ar", str(self.sample_rate),
                    "-application", "audio", "-f", "ogg", "pipe:1"
                ]
            else:
                command = [
                    "ffmpeg", "-hide_banner", "-loglevel", "error",
                    "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5",
                    "-i", self.source_url,
                    "-vn", "-c:a", "aac", "-b:a", f"{self.kbps}k",
                    "-ac", str(self.channels), "-ar", str(self.sample_rate),
                    "-f", "adts", "pipe:1"
                ]
            self.process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
            self.running = True
            self.thread = threading.Thread(target=self._pump, daemon=True)
            self.thread.start()

    def _pump(self):
        try:
            while True:
                if self.process is None or self.process.stdout is None:
                    break
                chunk = self.process.stdout.read(8192)
                if not chunk:
                    break
                with self.condition:
                    self.sequence += 1
                    self.buffer.append((self.sequence, chunk))
                    self.condition.notify_all()
        finally:
            with self.condition:
                self.running = False
                self.condition.notify_all()

    def subscribe(self):
        self.start()
        with self.condition:
            self.clients += 1
            self.last_client_time = time.time()
            cursor = self.sequence + 1

        try:
            while True:
                with self.condition:
                    while self.running and self.sequence < cursor:
                        self.condition.wait(timeout=2.0)
                    if self.sequence >= cursor:
                        if self.buffer and cursor < self.buffer[0][0]:
                            cursor = self.buffer[0][0]
                        chosen = None
                        for seq, chunk in self.buffer:
                            if seq == cursor:
                                chosen = chunk
                                break
                        if chosen is None:
                            continue
                        cursor += 1
                    elif not self.running:
                        break
                    else:
                        continue
                yield chosen
        finally:
            with self.condition:
                self.clients = max(0, self.clients - 1)
                self.last_client_time = time.time()

    def stop_if_idle(self):
        with self.condition:
            if self.clients == 0 and self.running and time.time() - self.last_client_time > 15:
                try:
                    if self.process:
                        self.process.kill()
                except Exception:
                    pass
                self.running = False
                self.condition.notify_all()

relays = {}
relays_lock = threading.Lock()


def get_relay(source_url, kbps, codec, channels, sample_rate):
    key = (source_url, kbps, codec, channels, sample_rate)
    with relays_lock:
        relay = relays.get(key)
        if relay is None or (not relay.running and relay.clients == 0):
            relay = SharedRelay(source_url, kbps, codec, channels, sample_rate)
            relays[key] = relay
        return relay


def cleanup_loop():
    while True:
        time.sleep(5)
        with relays_lock:
            dead = []
            for key, relay in relays.items():
                relay.stop_if_idle()
                if relay.clients == 0 and not relay.running:
                    dead.append(key)
            for key in dead:
                relays.pop(key, None)

threading.Thread(target=cleanup_loop, daemon=True).start()


@app.get("/health")
def health():
    return {
        "ok": True,
        "stream_relay": True,
        "service": "Fast Radio Relay",
        "version": 1,
        "profiles": {"opus_mono": True, "aac": True, "shared_stream": True}
    }

@app.get("/discover")
def discover():
    # The app can use this endpoint to verify that a relay node is compatible.
    return {
        "ok": True,
        "stream_relay": True,
        "version": 1,
        "recommended": {"codec": "opus", "mono": 1, "sample_rate": 24000, "mobile_kbps": 24, "wifi_kbps": 48}
    }

@app.get("/stream")
async def proxy_stream(
    url: str,
    kbps: int = 24,
    codec: str = "opus",
    mono: int = 1,
    sample_rate: int = 24000,
):
    decoded_url = urllib.parse.unquote(url).strip()
    parsed = urllib.parse.urlparse(decoded_url)
    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        return StreamingResponse(iter([b"invalid source url"]), status_code=400, media_type="text/plain")
    if len(decoded_url) > 4096:
        return StreamingResponse(iter([b"source url too long"]), status_code=400, media_type="text/plain")
    kbps = max(8, min(128, int(kbps)))
    codec = "opus" if str(codec).lower() == "opus" else "aac"
    channels = 1 if int(mono) else 2
    sample_rate = max(8000, min(48000, int(sample_rate)))

    relay = get_relay(decoded_url, kbps, codec, channels, sample_rate)
    return StreamingResponse(
        relay.subscribe(),
        media_type=relay.media_type if relay.running else ("audio/ogg" if codec == "opus" else "audio/aac"),
        headers={
            "Cache-Control": "no-store",
            "X-Fast-Radio-Kbps": str(kbps),
            "X-Fast-Radio-Codec": codec,
            "X-Fast-Radio-Channels": str(channels),
            "X-Fast-Radio-SampleRate": str(sample_rate),
            "X-Fast-Radio-Shared-Relay": "1",
        },
    )

# Run with:
# uvicorn server:app --host 0.0.0.0 --port 8000
