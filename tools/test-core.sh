#!/usr/bin/env bash
# Optional SDK/Gradle-free runner for the pure Kotlin module. Supply locally installed compiler
# and test libraries; this script deliberately never downloads tools or dependencies.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${KOTLINC:=$(command -v kotlinc || true)}"
: "${JUNIT_JAR:?Set JUNIT_JAR to a JUnit 4.13.2 jar}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR to hamcrest-core 1.3}"
if [[ -z "$KOTLINC" ]]; then echo 'A Kotlin 2.x compiler is required.' >&2; exit 1; fi
KOTLIN_HOME="${KOTLIN_HOME:-$(cd "$(dirname "$KOTLINC")/.." && pwd)}"
COROUTINES_JAR="${COROUTINES_JAR:-$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
CP="$JUNIT_JAR:$HAMCREST_JAR:$COROUTINES_JAR"
mkdir -p core/build/standalone
mapfile -t sources < <(find core/src/main core/src/test -name '*.kt' | sort)
"$KOTLINC" "${sources[@]}" -jvm-target 17 -cp "$CP" -d core/build/standalone/tests.jar
mapfile -t tests < <(find core/src/test -name '*Test.kt' | sort | sed -E 's@.*/([^/]+)\.kt@com.example.mydailyroutine.domain.\1@')
"$JAVA" -cp "core/build/standalone/tests.jar:$CP:$KOTLIN_HOME/lib/kotlin-stdlib.jar" org.junit.runner.JUnitCore "${tests[@]}"
