#!/usr/bin/env bash
set -euo pipefail
status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log || status=$?

# Pull the ui-audit screenshots the instrumented tests wrote into the app's external files dir.
# adb pull of another app's Android/data needs root on newer images, so escalate and retry.
pkg=com.example.mydailyroutine
remote="/sdcard/Android/data/$pkg/files/ui-audit"
mkdir -p app/build/ui-audit
adb pull "$remote" app/build/ui-audit/ >/dev/null 2>&1 || true
if [ -z "$(find app/build/ui-audit -type f -name '*.png' 2>/dev/null)" ]; then
  echo "ui-audit: plain pull found nothing, retrying as root" >&2
  adb root >/dev/null 2>&1 || true
  adb wait-for-device >/dev/null 2>&1 || true
  sleep 2
  adb pull "$remote" app/build/ui-audit/ >/dev/null 2>&1 || true
fi
if [ -z "$(find app/build/ui-audit -type f -name '*.png' 2>/dev/null)" ]; then
  echo "ui-audit: still nothing, copying to a public dir first" >&2
  adb shell "cp -r '$remote' /sdcard/Download/ui-audit 2>/dev/null" || true
  adb pull /sdcard/Download/ui-audit app/build/ui-audit/ >/dev/null 2>&1 || true
  adb shell "rm -rf /sdcard/Download/ui-audit" >/dev/null 2>&1 || true
fi
echo "ui-audit screenshots pulled:"
find app/build/ui-audit -type f -printf '  %p (%s B)\n' 2>/dev/null || true

exit "$status"
