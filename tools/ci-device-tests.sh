#!/usr/bin/env bash
set -uo pipefail
status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log || status=$?

# Collect the ui-audit screenshots. Gradle uninstalls both APKs when the connected-test task ends,
# which wipes the app's own dirs, so the tests also write a copy into Pictures/ui-audit via
# MediaStore; that public folder survives the uninstall and is the first thing pulled here. The
# app-dir routes stay as fallbacks. Every attempt is echoed as a workflow notice because log ZIP
# downloads are blocked in some sandboxes while check-run annotations stay readable through the API.
pkg=com.example.mydailyroutine
public_dir="/sdcard/Pictures/ui-audit"
external="/sdcard/Android/data/$pkg/files/ui-audit"
internal="/data/data/$pkg/files/ui-audit"
out=app/build/ui-audit
mkdir -p "$out"

notice() { echo "::notice title=ui-audit::$1"; }
found() { [ -n "$(find "$out" -type f -name '*.png' 2>/dev/null)" ]; }
count() { find "$out" -type f -name '*.png' 2>/dev/null | wc -l | tr -d ' '; }

notice "devices: $(adb devices | tr '\n' ' ')"

harvest() {
  # AGP writes pulled test outputs under app/build/outputs/androidTest-results/<device>/.
  local png
  while IFS= read -r png; do
    [ -n "$png" ] || continue
    cp -f "$png" "$out/$(basename "$png")" 2>/dev/null || true
  done < <(find app/build/outputs/androidTest-results app/build/reports/androidTests -type f -name '*.png' 2>/dev/null)
}
harvest
notice "after harvesting Gradle test outputs: $(count) png"

if ! found; then
  adb pull "$public_dir" "$out/" > /tmp/pull0.txt 2>&1 || true
  notice "pull public Pictures/ui-audit: $(count) png | $(tr '\n' ' ' < /tmp/pull0.txt | cut -c1-160)"
fi
if ! found; then
  adb pull "$external" "$out/" > /tmp/pull1.txt 2>&1 || true
  notice "pull external: $(count) png | $(tr '\n' ' ' < /tmp/pull1.txt | cut -c1-160)"
fi
if ! found; then
  adb root > /tmp/root.txt 2>&1 || true
  adb wait-for-device > /dev/null 2>&1 || true
  sleep 3
  adb pull "$external" "$out/" > /tmp/pull2.txt 2>&1 || true
  notice "pull external as root ($(tr '\n' ' ' < /tmp/root.txt | cut -c1-80)): $(count) png | $(tr '\n' ' ' < /tmp/pull2.txt | cut -c1-160)"
fi
if ! found; then
  adb pull "$internal" "$out/" > /tmp/pull3.txt 2>&1 || true
  notice "pull internal: $(count) png | $(tr '\n' ' ' < /tmp/pull3.txt | cut -c1-160)"
fi
if ! found; then
  names="$(adb exec-out run-as "$pkg" ls files/ui-audit 2>/dev/null | tr -d '\r' | grep '\.png$' || true)"
  notice "run-as listing: ${names:-<empty>} ($(printf '%s' "$names" | grep -c . || true) files)"
  mkdir -p "$out"
  for name in $names; do
    adb exec-out run-as "$pkg" cat "files/ui-audit/$name" > "$out/$name" 2>/dev/null || true
  done
  notice "after run-as copy: $(count) png"
fi
if ! found; then
  notice "remote public dir: $(adb shell "ls $public_dir 2>&1 | tr '\n' ' '" | cut -c1-200)"
  notice "remote external dir: $(adb shell "ls /sdcard/Android/data/$pkg/files/ui-audit 2>&1 | tr '\n' ' '" | cut -c1-200)"
  notice "remote internal dir: $(adb shell "run-as $pkg ls files/ui-audit 2>&1 | tr '\n' ' '" | cut -c1-200)"
fi

notice "collected: $(count) png"
find "$out" -type f -printf '  %p (%s B)\n' 2>/dev/null || true

exit "$status"
