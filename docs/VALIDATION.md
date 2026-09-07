# Research planner verification

## Successful CI

**Run [34097272103](https://github.com/jakob611/MyDailyRoutine/actions/runs/34097272103), code commit
`f9241d7f8246100c01c59eda36a5bb93daab5a4c`: build and device-test jobs both succeeded.**

| Check | Result |
|---|---|
| `:core:test` | 107 tests passed |
| `:app:testDebugUnitTest` | 12 tests passed |
| `:app:assembleDebug` | Debug APK built |
| `:app:lintDebug` | Passed |
| `:app:connectedDebugAndroidTest` | 24 tests passed on API 35 |
| SQLite/migration/manifest smoke checks | 13 passed |
| Presentation checks | 458 Slovenian resources, bundled font, tabular widget text, one canonical model |

The declared Kotlin 2.2.10/JDK 17/API 36 stack was used in CI. Reports and actual KSP schema JSON
are retained in its artifacts; no database identity hash was fabricated. The APK artifact is
[`debug-apk`](https://github.com/jakob611/MyDailyRoutine/actions/runs/34097272103/artifacts/10009372989).

## What is exercised

- All original recurrence, overnight, override, holiday, health-rule and interval tests.
- Five-phase slippage recovery: slack, explicit buffer use, weighted elasticity saturation,
  exact integer allocation, stable low-priority deferral, fixed-boundary preservation,
  Int.MAX_VALUE delay, zero-minimum tasks, immutable input and deterministic ordering.
- Gaussian center/sigma/category weights and midnight periodicity; RSEM pooling.
- Ratio-of-sums calibration, invalid-sample rejection, recent-history bound, exact rational
  rounding and ULP-safe duration ceilings; raw estimates retained through backlog restoration.
- Geometric spacing, jitter, strict date order, exact daily 20% cap, capacity exhaustion,
  backplanning effort conservation and earlier terminal deadlines.
- Active widget progress, next-two projection and unioned remaining reserves.
- Room FK/cascade/detach behavior and migration of existing records through v4, including
  recovering a backlog row's source raw estimate instead of silently changing schema v3.
- Actual completion history updates/undo, idempotent backlog healing, real scheduled review
  blocks, topic deletion, and review links that do not block preparation generation.
- Slovenian UI navigation, advanced Settings, subject presets, actual persisted test entries,
  maximum-length subject names, warm quick-add, and absence of merged network permissions.

## Local environment limitation

Direct `./gradlew :core:test :app:testDebugUnitTest --stacktrace` could not bootstrap because
`services.gradle.org` terminated the TLS handshake; SDK/Maven downloads are also restricted.
The pure suite was additionally run with a locally available Kotlin 2.0.21/JDK 17 compiler,
and 85 Kotlin files passed compiler PSI syntax parsing. Those local checks alone are not an
Android build; the successful Android build and emulator validation above were remote CI.
A temporary GitHub authentication error cleared after renewing the Arena session. No credentials
were requested in chat and no CI files needed to be excluded from the push.

## Reproduce

```bash
python3 tools/check_sqlite_integrity.py
python3 tools/check_presentation.py
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

`tools/test-core.sh` is an SDK-free alternative with locally supplied Kotlin/JUnit jars.
`tools/report_ci_failures.py` exposes structured compiler/lint/JUnit diagnostics without requiring
this client to download CI log archives.

## Still requires physical-device / release verification

- Feel and amplitude of haptics on actual hardware, with app and system haptics disabled/enabled.
- Vendor-specific exact-alarm restrictions, Doze throttling, reboot/unlock and force-stop behavior.
- Glance appearance/resize/refresh on multiple launchers and older API-24 devices. The progress
  bar is a timestamped snapshot, not an app-owned polling loop.
- Long-duration usability, font scaling and performance on low-end hardware; release R8 and
  signing with a stable production key before treating CI debug artifacts as a release.
- Real-world validity of the paper's physiological claims is not established by software tests.
  These are explicit, configurable planning heuristics; missing equations/policy choices are
  documented in `CHRONOBIOLOGY_ENGINE.md`, not presented as measured biological facts.
