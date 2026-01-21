# Pocket Nexus v1.1.0 
> [!IMPORTANT]
> **FRESH INSTALLATION REQUIRED**
> Since I have added new signing keys and major feature updates, please **uninstall the old version** completely before installing this update. 
> 1. Long press the existing app icon -> Uninstall
> 2. Install the new APK
> 3. Enjoy the new features!

**LEGAL DISCLAIMER:** This is an educational project. I don't own or host any of the content accessible through this app. Everything is streamed from third-party sources that I have no control over. Use this responsibly and make sure you're following your local laws and respecting copyright. I'm not responsible for how you use this.

## What is this?

A streaming app I built while learning Android development. Started as a simple video player and ended up with a bunch of cool features like gesture controls, automatic subtitle discovery, and a custom player.

## Features

**Player Controls:**
- **Center area:** Tap to show/hide controls, access play/pause, seek bar, and +10/-10 buttons
- **Left side gestures:**
  - Double-tap to seek backward 10 seconds
  - Swipe up/down to adjust screen brightness
  - Long-press for 2x playback speed
- **Right side gestures:**
  - Double-tap to seek forward 10 seconds
  - Swipe up/down to adjust volume
  - Long-press for 2x playback speed
- **Rotation lock:** Auto/Portrait/Landscape modes
- **Quality selection:** Choose video quality
- **Full-screen:** Video zooms to fill entire screen (no black bars)
- **Auto-hide controls:** Controls disappear after 3 seconds of inactivity
- **Immersive mode:** Full-screen without system bars

**Subtitles (NEW!):**
- **Instant subtitle loading:** 50+ languages available immediately using `sub.wyzie.ru` API
- **Proactive fetching:** Subtitles are fetched and ready before you even click play
- **Works mid-stream:** Select subtitles anytime during playback - they appear instantly
- **Multi-language support:** English, Spanish, French, German, Italian, Portuguese, Russian, Japanese, Korean, Hindi, Chinese, Arabic, and many more
- **Multiple formats:** Supports SRT and VTT subtitle formats
- **Hearing impaired options:** HI/SDH and regular subtitles available

**UI:**
- Material 3 design with black and purple theme
- Smooth animations and transitions
- Dark mode optimized
- Auto-hiding floating controls

**Technical:**
- Clean API-based subtitle integration (removed legacy WebView polling)
- Optimized gesture zones (70% height) - seek bar always clickable
- Synced control visibility (floating buttons + ExoPlayer controls)
- Persistent playback state on minimize/restore

## Known Issues

- Some streams may have limited subtitle availability (depends on source)
- Subtitle selection requires network connectivity

## Installation

**Option 1: Download APK**
1. Go to Releases
2. Download the latest APK
3. Enable "Unknown Sources" in Android settings
4. Install it

**Option 2: Build from source**
```bash
git clone https://github.com/Puneeth-R-140/pocket-nexus-streaming.git
cd pocket-nexus-streaming
./gradlew assembleDebug
```

APK will be in `app/build/outputs/apk/debug/`

## Tech Stack

- Kotlin
- Jetpack Compose for UI
- ExoPlayer for video playback
- Hilt for dependency injection
- Retrofit + OkHttp for networking
- Room for local database
- Coil for image loading

## Project Structure

```
app/src/main/java/com/vidora/app/
├── data/       # repositories, API, database
├── ui/         # screens, components, viewmodels
├── player/     # custom player and subtitle engine
└── di/         # dependency injection
```

## Contributing

Found a bug or want to add something? Open an issue or PR. Just make sure to test your changes.

## License

MIT License - see LICENSE file for details

## Disclaimer

This is purely educational. Don't use it for anything illegal. Respect copyright laws and content creators.

---

Built while learning Android dev. Still improving it.
