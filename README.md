# Fast Radio 5.0 — Automatic Relay

Fixes based on the 2026-09-22 device screenshot and requested changes:
- Portrait layout restored: Custom Radio, Iran Radio, World Radio and control/news panels are all visible.
- Built-in Favorites contain real stream URLs and are shown through the World/Favorites panel.
- Iranian news RSS sources added to the web-news ticker, with slow ticker timing.
- TV ticker timing slowed.
- Playback buffer increased to 15–45 seconds.
- Minute/second usage timer continues during buffering and no longer stops because `isPlaying()` temporarily becomes false.
- Explicit STOP marks playback as user-stopped, preventing automatic retry after STOP.
- Playback service is restarted/activated on PLAY so automatic recovery remains enabled.
- kbps and volume rulers are 2x wider in portrait layout (184dp each).
- Previous features are retained.

Build note: Gradle is not installed in the current execution environment, so a local Gradle build was not run.

## Low-data relay (4.9)

The project now includes the cost-reduction features described in the supplied optimization notes:

- Opus output for low bitrates (default relay profile: 24 kbps).
- Mono output (`mono=1`).
- Reduced sample-rate output (24 kHz relay profile).
- Shared Stream Relay: identical source/profile requests share one FFmpeg process instead of starting one process per listener.
- Idle relay cleanup after the last listener disconnects.
- Android network-aware target: mobile data is capped at 24 kbps and Wi-Fi/Ethernet at 48 kbps when automatic network quality is enabled.

To activate the relay, deploy `server.py` on a server with FFmpeg and set `ProxyConfig.SERVER_BASE_URL` to its public base URL. Leaving it empty keeps direct station playback unchanged.


## Automatic Relay Discovery

The Android app no longer requires `ProxyConfig.SERVER_BASE_URL`. On startup it searches the bundled `app/src/main/assets/relay_directory.json`, checks each candidate with `/health`, caches the first healthy Fast Radio relay, and automatically routes compatible radio streams through it. If no relay is reachable, the app falls back to the original direct stream; playback is not blocked by relay discovery.

A relay node exposes `/health`, `/discover`, and `/stream`. Multiple listeners requesting the same source/profile share one FFmpeg process on that node. This keeps the low-data Opus/mono profile and shared-relay behavior from 4.9.

### One-time deployment requirement

A public relay node still has to exist somewhere on the Internet; an Android app cannot create a publicly reachable server behind arbitrary mobile/NAT networks by itself. After deploying `server.py` on one or more servers, place their public HTTPS base URLs in `app/src/main/assets/relay_directory.json`. From then on, the app discovers and selects a healthy node automatically and caches it.

Example:
```json
{
  "version": 1,
  "relays": [
    "https://relay1.example.com",
    "https://relay2.example.com"
  ]
}
```

For production deployment, use HTTPS and restrict the relay to approved source domains or authenticated clients so it is not an unrestricted public proxy.


## Relay node deployment

Install the packages from `requirements.txt` and FFmpeg, then run:
`uvicorn server:app --host 0.0.0.0 --port 8000`

Put the public HTTPS URL of each deployed node into `app/src/main/assets/relay_directory.json`. The app performs health checks automatically and uses the first healthy node.
