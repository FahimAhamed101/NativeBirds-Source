@echo off
REM Installs the freshly built NativeBirds APK on the connected phone.
REM Run this by double-clicking, or:  install.bat
setlocal
set ADB=C:\Users\Admin\adb_tool\adb.exe
set APK=%~dp0build\AngryBirds.apk

if not exist "%APK%" (
    echo APK not found at "%APK%"
    echo Run "bash build.sh" first.
    pause
    exit /b 1
)

echo Installing %APK%
"%ADB%" install -r -t "%APK%"
if errorlevel 1 (
    echo.
    echo Direct install failed - falling back to push + pm install.
    "%ADB%" push "%APK%" /data/local/tmp/ab.apk
    "%ADB%" shell pm install -r -t /data/local/tmp/ab.apk
)

echo.
echo Launching...
"%ADB%" shell am force-stop com.nativebirds.game
"%ADB%" shell am start -n com.nativebirds.game/com.nativebirds.game.MainActivity
pause
