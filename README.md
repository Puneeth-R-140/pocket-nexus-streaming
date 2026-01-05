# Pocket Nexus - Streaming App

> [!CAUTION]
> **LEGAL DISCLAIMER - Educational Purpose Only**
> 
> This application is developed for **educational and personal use only**. The developer does not own, host, or distribute any content accessible through this application. All media content is streamed from third-party sources over which the developer has no control.
> 
> **Users are solely responsible for:**
> - Ensuring their usage complies with local laws and regulations
> - Respecting copyright and intellectual property rights
> - Adhering to the terms of service of content providers
> 
> **The developer:**
> - Does NOT endorse piracy or copyright infringement
> - Provides this software "as is" without warranties
> - Is NOT liable for any misuse of this application

---

## About

Pocket Nexus is a modern Android streaming application built with Jetpack Compose and Material 3. It features a sleek black and purple theme, advanced player controls, and multi-language subtitle support.

## Features

### 🎨 Modern UI/UX
- **Rich black & purple theme** - Premium, eye-catching design
- **Material 3 components** - Latest Android design guidelines
- **Smooth animations** - Polished user experience
- **Dark mode optimized** - Easy on the eyes

### 🎬 Advanced Player
- **Custom subtitle engine** - Native SRT/VTT support with auto-discovery
- **Gesture controls** - Double-tap to seek, long-press for 2x speed
- **Rotation lock** - Manual orientation control (Auto/Portrait/Landscape)
- **Quality selection** - Choose your preferred video quality
- **Immersive mode** - Full-screen playback with hidden system bars

### 📱 Smart Features
- **Config-based subtitle discovery** - Automatically finds available subtitles
- **Multi-language support** - Ready for international content
- **Lifecycle management** - Pauses playback when app is minimized
- **Error handling** - Robust retry mechanism with clear error messages

## Screenshots

*Coming soon*

## Installation

### Download APK
1. Go to [Releases](../../releases)
2. Download the latest `pocket-nexus-v1.0.0.apk`
3. Enable "Install from Unknown Sources" in Android settings
4. Install the APK

### Build from Source

**Prerequisites:**
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34

**Steps:**
```bash
# Clone the repository
git clone https://github.com/Puneeth-R-140/pocket-nexus-streaming.git
cd pocket-nexus-streaming

# Build debug APK
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Repository pattern
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp
- **Database:** Room
- **Video Player:** ExoPlayer
- **Image Loading:** Coil
- **Navigation:** Jetpack Navigation Compose

## Project Structure

```
app/
├── src/main/
│   ├── java/com/vidora/app/
│   │   ├── data/          # Data layer (repositories, models)
│   │   ├── ui/            # UI layer (screens, components, viewmodels)
│   │   ├── player/        # Custom player implementation
│   │   └── di/            # Dependency injection modules
│   └── res/               # Resources (layouts, drawables, etc.)
```

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Video playback powered by [ExoPlayer](https://github.com/google/ExoPlayer)
- Icons from [Material Icons](https://fonts.google.com/icons)

## Disclaimer

This is an educational project. The developer is not responsible for any content accessed through this application or any misuse of the software. Users must comply with all applicable laws and respect intellectual property rights.

---

**Made with ❤️ for learning Android development**
