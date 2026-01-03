# Vidora Android App - Manual Build Guide

Since you are not using Android Studio, follow these instructions to build and manage the application from the command line.

## Prerequisites
1. **Java JDK 17:** Ensure `java -version` shows version 17.
2. **Android SDK:** You must have the Android SDK installed.
3. **Environment Variables:**
   - Set `ANDROID_HOME` to your Android SDK location.
   - Add `platform-tools` and `build-tools` to your `PATH`.

## How to Build
### Option A: Use System Gradle (Recommended if installed)
If you have Gradle installed on your computer, run:
```powershell
gradle assembleDebug
```

### Option B: Use Gradle Wrapper
The Gradle wrapper is now fully configured. Run:
```powershell
./gradlew assembleDebug
```

The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

## Common Commands
| Action | Command |
|--------|---------|
| **Clean Project** | `./gradlew clean` |
| **Build Release APK** | `./gradlew assembleRelease` |
| **Install on Device** | `./gradlew installDebug` |
| **Run Unit Tests** | `./gradlew test` |

## Project Structure
- `app/src/main/java`: Quality Kotlin source code.
- `app/src/main/res`: App resources (icons, strings, layouts).
- `app/src/main/AndroidManifest.xml`: App configuration and permissions.

## Troubleshooting
If you get a "SDK location not found" error, create a file named `local.properties` in the root directory and add:
`sdk.dir=C:\\Path\\To\\Your\\Android\\Sdk`
