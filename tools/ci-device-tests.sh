#!/usr/bin/env bash
set -euo pipefail
./gradlew :app:connectedDebugAndroidTest --stacktrace 2>&1 | tee ci-device.log
