@echo off
setlocal
set "APP_HOME=%~dp0"
if defined JAVA_HOME (set "JAVA_EXE=%JAVA_HOME%\bin\java.exe") else (set "JAVA_EXE=java.exe")
"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
  echo Java not found. Select JDK 17 in Android Studio Gradle settings.
  exit /b 1
)
"%JAVA_EXE%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
