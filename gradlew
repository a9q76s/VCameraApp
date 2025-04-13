#!/bin/sh
# -------------------------------------------
# gradlew - Gradle Wrapper script
# -------------------------------------------

# تحديد مسار السكربت الحالي
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_EXE=java

# تشغيل الجريدل باستخدام ملف الجار
exec "$JAVA_EXE" -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"