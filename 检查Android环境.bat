@echo off
chcp 65001 >nul
echo ===== Language Reader Android 环境检查 =====
echo.
echo ANDROID_HOME=%ANDROID_HOME%
echo ANDROID_SDK_ROOT=%ANDROID_SDK_ROOT%
echo JAVA_HOME=%JAVA_HOME%
echo.
echo local.properties:
if exist "%~dp0local.properties" (
  type "%~dp0local.properties"
) else (
  echo [不存在]
)
echo.
echo 默认 SDK 路径:
if exist "%LOCALAPPDATA%\Android\Sdk" (
  echo [找到] %LOCALAPPDATA%\Android\Sdk
) else (
  echo [未找到] %LOCALAPPDATA%\Android\Sdk
)
echo.
pause
