#!/bin/sh
# Minimal wrapper launcher. Uses the original Gradle wrapper JAR and pinned distribution.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi
if ! "$JAVACMD" -version >/dev/null 2>&1; then
    echo "Java not found. Select JDK 17 (Android Studio Flamingo bundled JDK)." >&2
    exit 1
fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
