@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Lirelia Mobile V5.1 Font Picker
findstr /C:"versionName = \"5.1-font-picker\"" "app\build.gradle.kts" >nul
if %ERRORLEVEL%==0 (echo [正确] 当前版本: V5.1) else (echo [错误] versionName 不匹配)
echo.
echo Android Studio 请打开当前目录 LireliaMobile_V5.1，而不是 app 子目录。
pause
