# Pocket Nexus - Streaming Application

> **Note:**
> If you encounter an "App not installed" error, please uninstall the previous version first.
>
> **IMPORTANT HIGHLIGHTS:**
> *   **Check for Updates:** Please check for updates in the **Settings Menu** when you open the app and **before streaming** to ensure you have the latest fixes.
> *   **Server Status:** The app works fine. If you see any discrepancies, they are likely from the server side.
> *   **Support:** Any bugs within the app itself are fixed within **48 hours**.
> *   **Known Issue:** The "Continue Now" button might not work as expected; this will be fixed in the next update.
> *   **Playback Tips:** If the stream stalls, please try restarting the stream. If it stalls again, it is likely a server-side response error.

**LEGAL DISCLAIMER:** This project is for educational purposes only. The developer does not own or host any content accessible through this application. All content is streamed from third-party sources. Users are responsible for complying with local laws and copyright regulations.

## Overview

Pocket Nexus is a native Android streaming application developed to demonstrate modern Android development practices. It features a custom-built video player, automated subtitle integration, and a refined user interface.

## Key Features

**Advanced Video Player**
- **Gesture Controls:**
  - Single tap to toggle UI controls.
  - Double tap to seek (forward/backward).
- **Playback Features:**
  - Auto-play next episode with season intelligence.
  - Manual "Next Episode" navigation.
  - Selection of video quality and playback speed.

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

