#!/usr/bin/env bash
set -uo pipefail
status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log || status=$?

# Collect the ui-audit screenshots the instrumented tests wrote. They land in both the app's
# external and internal files dir; `adb pull` of Android/data is blocked on newer platform
# levels, so escalate to root and finally fall back to `run-as` (works on debuggable builds).
pkg=com.example.mydailyroutine
external="/sdcard/Android/data/$pkg/files/ui-audit"
internal="/data/data/$pkg/files/ui-audit"
out=app/build/ui-audit
mkdir -p "$out"

found() { [ -n "$(find "$out" -type f -name '*.png' 2>/dev/null)" ]; }

echo "== ui-audit: devices"
adb devices -l || true

if ! found; then
  echo "== ui-audit: pull external ($external)"
  adb pull "$external" "$out/" || true
fi
if ! found; then
  echo "== ui-audit: adb root, then pull external"
  adb root || true
  adb wait-for-device || true
  sleep 3
  adb pull "$external" "$out/" || true
fi
if ! found; then
  echo "== ui-audit: pull internal ($internal)"
  adb pull "$internal" "$out/" || true
fi
if ! found; then
  echo "== ui-audit: copy internal files out via run-as"
  names="$(adb exec-out run-as "$pkg" ls files/ui-audit 2>/dev/null | tr -d '\r' | grep '\.png$' || true)"
  if [ -z "$names" ]; then
    echo "ui-audit: run-as listed nothing" >&2
  else
    mkdir -p "$out/ui-audit"
    for name in $names; do
      adb exec-out run-as "$pkg" cat "files/ui-audit/$name" > "$out/ui-audit/$name" 2>/dev/null || true
    done
  fi
fi
if ! found; then
  echo "== ui-audit: last resort, list remote dirs for diagnosis"
  adb shell "ls -la /sdcard/Android/data/$pkg/files/ 2>&1 | head -20" || true
  adb shell "run-as $pkg ls -la files/ 2>&1 | head -20" || true
fi

echo "== ui-audit screenshots collected:"
find "$out" -type f -printf '  %p (%s B)\n' 2>/dev/null || true

exit "$status"
