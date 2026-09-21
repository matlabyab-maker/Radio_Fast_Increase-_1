from fastapi import FastAPI, Response
import subprocess
import urllib.parse

app = FastAPI()

@app.get("/stream")
async def proxy_stream(url: str):
    decoded_url = urllib.parse.unquote(url)
    command = [
        "ffmpeg",
        "-i", decoded_url,
        "-acodec", "libfdk_aac",
        "-b:a", "48k",
        "-f", "adts",
        "pipe:1"
    ]
    process = subprocess.Popen(
        command,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL
    )

    def generate():
        while True:
            chunk = process.stdout.read(4096)
            if not chunk:
                break
            yield chunk

    return Response(content=generate(), media_type="audio/aac")

# Run with:
# uvicorn server:app --host 0.0.0.0 --port 8000
