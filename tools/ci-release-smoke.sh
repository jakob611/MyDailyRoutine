#!/usr/bin/env bash
# Starts the APK the reader actually installs.
#
# The instrumentation suite runs against the debug variant. The release variant is a different
# program: R8 has renamed, inlined, merged and deleted things, and resource shrinking has thrown
# away everything it believed unreachable. Until it has been started on a device, nobody has run
# it — and the failure it hides is the worst kind, because the app installs and opens before it
# goes wrong.
#
# What this catches, concretely: a keep rule that should exist and does not. This app persists
# enum constants by name and its SQLite triggers spell those names out in CHECK constraints, so a
# renamed constant would let the app start and then refuse every write.
set -uo pipefail
pkg=com.example.mydailyroutine

echo "::group::release smoke: build"
./gradlew :app:assembleRelease --stacktrace 2>&1 | tail -20
echo "::endgroup::"

apk="$(ls app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)"
if [ -z "$apk" ]; then
  echo "::error title=release smoke::no release APK was produced"
  exit 1
fi
echo "::notice title=release smoke::testing $(basename "$apk") ($(stat -c%s "$apk" 2>/dev/null || echo '?') B)"

adb uninstall "$pkg" > /dev/null 2>&1 || true
if ! adb install -r "$apk" > /tmp/smoke-install.txt 2>&1; then
  echo "::error title=release smoke::install failed: $(tr -d '\r' < /tmp/smoke-install.txt | tr '\n' ' ' | cut -c1-300)"
  exit 1
fi

adb logcat -c > /dev/null 2>&1 || true
adb shell am start -W -n "$pkg/.MainActivity" > /tmp/smoke-start.txt 2>&1
sleep 8

fail=0
report() { echo "::error title=release smoke::$(printf '%s' "$1" | tr -d '\r' | sed 's/%/%25/g' | cut -c1-400)"; fail=1; }

# 1. Did it survive its own start? A crash here is the graph, Room, DataStore or Compose failing
#    under R8 — the whole cold-start path this app does before it draws anything.
if [ -z "$(adb shell pidof "$pkg" 2>/dev/null | tr -d '\r')" ]; then
  report "the process is not running eight seconds after launch"
  adb logcat -d -b crash -v threadtime 2>/dev/null | grep -A20 -E "AndroidRuntime|FATAL" | head -30 | \
    while IFS= read -r l; do report "crash: $l"; done
fi

# 2. Exercise it. A seeded monkey taps its way through the real screens, which is what reaches the
#    sheets, the dialogs and the writes; `-s` keeps the walk reproducible between runs.
echo "::group::release smoke: monkey"
adb shell monkey -p "$pkg" -s 20260928 --throttle 250 --pct-syskeys 0 --ignore-timeouts -v 140 \
  > /tmp/smoke-monkey.txt 2>&1
tail -25 /tmp/smoke-monkey.txt
echo "::endgroup::"

if grep -qE "// CRASH|// Short Msg: Fatal" /tmp/smoke-monkey.txt; then
  report "the app crashed while being used"
  grep -A12 -E "// CRASH" /tmp/smoke-monkey.txt | head -20 | while IFS= read -r l; do report "monkey: $l"; done
fi

# 3. Whatever the monkey did, the log is the record. Only our own process counts.
adb logcat -d -v threadtime > /tmp/smoke-logcat.txt 2>&1 || true
if grep -E "FATAL EXCEPTION|Fatal signal" /tmp/smoke-logcat.txt | grep -q "$pkg"; then
  report "a fatal exception was logged for $pkg"
  grep -B2 -A25 -E "FATAL EXCEPTION|Fatal signal" /tmp/smoke-logcat.txt | head -35 | \
    while IFS= read -r l; do report "logcat: $l"; done
fi

adb uninstall "$pkg" > /dev/null 2>&1 || true
if [ "$fail" -eq 0 ]; then
  echo "::notice title=release smoke::the minified APK starts, survives 140 input events and logs no fatal"
fi
exit "$fail"
