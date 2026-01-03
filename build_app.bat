@echo off
echo Building Vidora Debug APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo Build Successful!
    echo APK Location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Build Failed!
)
pause
