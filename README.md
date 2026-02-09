# Pocket Nexus - Streaming Application

> **Note:**
> If you are upgrading from a previous version signed with the same key, you can install this update directly. If you encounter an "App not installed" error, please uninstall the previous version first.

**LEGAL DISCLAIMER:** This project is for educational purposes only. The developer does not own or host any content accessible through this application. All content is streamed from third-party sources. Users are responsible for complying with local laws and copyright regulations.

## Overview

Pocket Nexus (Vidora) is a native Android streaming application developed to demonstrate modern Android development practices. It features a custom-built video player, automated subtitle integration, and a refined user interface.

## Key Features

**Advanced Video Player**
- **Gesture Controls:**
  - Single tap to toggle UI controls.
  - Double tap to seek (forward/backward).
  - Swipe gestures for brightness and volume control.
- **Playback Features:**
  - Auto-play next episode with season intelligence.
  - Manual "Next Episode" navigation.
  - Selection of video quality and playback speed.
  - Immersive full-screen mode with rotation lock.

**Subtitle System**
- **Automated Retrieval:** Instant integration with `sub.wyzie.ru` API for multi-language support.
- **Format Support:** Compatibility with SRT and VTT formats.
- **Accessibility:** Options for hearing impaired (HI/SDH) subtitles.

**User Interface**
- Material 3 Design implementation.
- Dark mode optimization.
- Smooth transitions and responsive layout.

## Technical Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Video Engine:** ExoPlayer / Media3
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp
- **Local Storage:** Room Database
- **Image Loading:** Coil

## Installation

**Option 1: Download APK**
1. Navigate to the Releases section.
2. Download the latest APK file.
3. specific "Install unknown apps" permission if prompted.
4. Install the application.

**Option 2: Build from Source**
```bash
git clone https://github.com/Puneeth-R-140/pocket-nexus-streaming.git
cd pocket-nexus-streaming
./gradlew assembleRelease
```
The output APK will be located in `app/build/outputs/apk/release/`.

## Contributing

Contributions are welcome. Please ensure all changes are tested before submitting a Pull Request.

## License

MIT License - see LICENSE file for details.

---

**Developed by Puneeth R**

