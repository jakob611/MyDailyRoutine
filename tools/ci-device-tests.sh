#!/usr/bin/env bash
set -uo pipefail
status=0

# The device is put into the language the app is written for before anything is installed. The app
# speaks the phone's language where it has a complete translation and Slovenian otherwise, so this
# decides what the run is evidence of: the screenshots the design audit reads, the stress of the
# longest strings (Slovenian runs longer than English), and the language the seeded school calendar
# is written in. The tests themselves assert the rule, not this device's answer, so the suite also
# passes on an English device — this only says which device this run describes.
locale_now="$(adb shell getprop persist.sys.locale 2>/dev/null | tr -d '\r')"
if [ "$locale_now" != "sl-SI" ]; then
  adb root > /dev/null 2>&1 || true
  adb wait-for-device > /dev/null 2>&1 || true
  adb shell "setprop persist.sys.locale sl-SI" > /dev/null 2>&1 || true
  # The framework reads the property while starting, so it is restarted rather than guessed at.
  adb shell stop > /dev/null 2>&1 || true
  adb shell start > /dev/null 2>&1 || true
  adb wait-for-device > /dev/null 2>&1 || true
  for _ in $(seq 1 40); do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 3
  done
fi
locale_now="$(adb shell getprop persist.sys.locale 2>/dev/null | tr -d '\r')"
if [ "$locale_now" = "sl-SI" ]; then
  echo "::notice title=device language::sl-SI (the app is Slovenian-first; screenshots and seeded calendar follow it)"
else
  echo "::warning title=device language::could not set sl-SI (device says '$locale_now'); the run describes that language instead"
fi
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log || status=$?

# Collect the ui-audit screenshots. Gradle uninstalls both APKs when the connected-test task ends,
# which wipes the app's own dirs, so the tests also write a copy into Pictures/ui-audit via
# MediaStore; that public folder survives the uninstall and is the first thing pulled here. The
# app-dir routes stay as fallbacks. Every attempt is echoed as a workflow notice because log ZIP
# downloads are blocked in some sandboxes while check-run annotations stay readable through the API.
# Crash evidence. Log ZIP downloads are blocked in some sandboxes while check-run annotations stay
# readable through the API, so a failing run publishes the fatal trace itself: first whatever Gradle
# printed, then the emulator's crash buffer and the last AndroidRuntime lines. Without this a failed
# instrumentation run is indistinguishable from a broken emulator.
if [ "$status" -ne 0 ]; then
  adb logcat -d -b crash -v threadtime > ci-device-crash.log 2>&1 || true
  adb logcat -d -v threadtime > ci-device-logcat.log 2>&1 || true
  esc() { printf '%s' "$1" | tr -d '\r' | sed 's/%/%25/g' | cut -c1-400; }
  emit() { echo "::error title=device-test failure::$(esc "$1")"; }
  # The failing test name and its assertion go out first: log ZIP downloads are blocked in some
  # sandboxes, the annotation budget per step is small, and "1 test failed" without a name is
  # worthless. Parsed straight out of the XML Gradle wrote.
  python3 - > /tmp/testfail.txt 2>&1 <<'PY' || true
import pathlib
import xml.etree.ElementTree as ET
for report in pathlib.Path('app/build').rglob('TEST-*.xml'):
    try:
        tree = ET.parse(report)
    except ET.ParseError:
        continue
    for case in tree.iter('testcase'):
        for error in list(case.findall('failure')) + list(case.findall('error')):
            detail = (error.attrib.get('message', '') + ' | ' + (error.text or ''))[:1500]
            print('%s.%s :: %s' % (case.attrib.get('classname', ''), case.attrib.get('name', ''),
                                   ' '.join(detail.split())))
PY
  while IFS= read -r line; do [ -n "$line" ] && echo "::error title=failing test::$(esc "$line")"; done < /tmp/testfail.txt
  grep -nE "FATAL EXCEPTION|Fatal signal|Process crashed|SIGSEGV|SIGABRT|Caused by:|> Task .*FAILED|There w(as|ere) [0-9]+ failure|Test failed|FAILED$|Instrumentation run failed|Unable to find instrumentation" \
    ci-device.log 2>/dev/null | head -30 | while IFS= read -r line; do emit "gradle: $line"; done
  grep -nE "FATAL EXCEPTION|Fatal signal|Process crashed|SIGSEGV|SIGABRT|AndroidRuntime:|backdrop|kyant|compose" \
    ci-device-crash.log 2>/dev/null | head -40 | while IFS= read -r line; do emit "crash buffer: $line"; done
  grep -nE "FATAL EXCEPTION|Fatal signal|Process crashed|SIGSEGV|SIGABRT" ci-device-logcat.log 2>/dev/null | head -10 | \
    while IFS= read -r line; do
      lineno="${line%%:*}"
      emit "logcat: $line"
      sed -n "$((lineno + 1)),$((lineno + 25))p" ci-device-logcat.log 2>/dev/null | \
        while IFS= read -r trace; do emit "logcat: $trace"; done
    done
  emit "no test results: $(find app/build -name 'TEST-*.xml' 2>/dev/null | wc -l | tr -d ' ') xml, $(find app/build/outputs/androidTest-results -type f 2>/dev/null | wc -l | tr -d ' ') output files"
fi

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
