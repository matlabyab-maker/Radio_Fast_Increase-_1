# Fast Radio

Dashboard-oriented internet radio Android project.

## Included
- Custom Radio list
- Iran Radio list from RadioBrowser country search
- World Radio: America, Europe, Africa, Asia, National country grouping
- RadioBrowser station search and saved JSON lists
- Favorites stored in app-private `favorites.json`
- Special News Stations panel: Sputnik Persian, BBC Persian, Iran International, VOA Persian, BBC News, NHK Japan
- Vertical ruler-style bitrate selector from 1 to 400 kbps
- Estimated radio data usage per minute, highlighted with an orange bar
- 5–10 second low-buffer playback target
- Media3 ExoPlayer 1.11.0 + HLS + DASH + FFmpeg decoder extension
- Android MediaPlayer compatibility class retained for direct-stream fallback integration
- MediaSession background playback
- GitHub Actions debug APK and build log

## Important
The 1–400 kbps ruler is a target/selection control. It cannot transcode an arbitrary source stream locally. Actual lower-bitrate restreaming requires a real server-side transcoder/restream endpoint; this project does not insert fake endpoints.

Build with GitHub Actions using Java 17 and Gradle 8.13.


Build fix: the FFmpeg decoder uses the Jellyfin Media3 FFmpeg decoder artifact version 1.9.0+1 because the Jellyfin artifact is published at that version do not publish `androidx.media3:media3-decoder-ffmpeg` as a directly consumable app dependency. The FFmpeg decoder is kept enabled through DefaultRenderersFactory.


## Bandwidth-reduction middleware

The project now includes `server.py`, based on the supplied middleware code. Install FFmpeg on the VPS and run the server with `uvicorn server:app --host 0.0.0.0 --port 8000`.

In `app/src/main/java/com/fast/radio/ProxyConfig.java`, set `SERVER_BASE_URL` to the public base URL of the VPS. When it is non-empty, selected station URLs are sent through `/stream?url=...`; when it is empty, the app keeps using the original station URL.

The Android playback path is wired in `MainActivity.playSelected()`.


## Fast Radio 3.4
Custom Radio now contains the 112 stations supplied in `stations.txt`.
The supplied list is preserved as `stations_CustomRadio_Source.txt`.
