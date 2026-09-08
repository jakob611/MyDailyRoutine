#!/usr/bin/env bash
set -euo pipefail
status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log || status=$?
mkdir -p app/build/ui-audit
adb pull /sdcard/Android/data/com.example.mydailyroutine/files/ui-audit app/build/ui-audit/ >/dev/null 2>&1 || true
exit "$status"
