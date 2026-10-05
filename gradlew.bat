@rem Gradle wrapper for Language Reader Mobile
@echo off
setlocal
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo [Language Reader] gradle-wrapper.jar is missing. Trying one-time repair...
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%prepare_gradle_wrapper.ps1"
  if errorlevel 1 (
    echo.
    echo Gradle wrapper repair failed. Check network/VPN, then run prepare_gradle_wrapper.ps1 again.
    exit /b 1
  )
)

if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
goto execute

:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

:execute
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
