# Integration validation (2026-09-06)

## Current local results

- 74 core JVM tests passed (Kotlin 2.0.21/JDK 17 standalone; project still pins Kotlin 2.2.10).
- 11 SQLite/manifest/resource tests passed.
- Presentation check passed: 384 Slovenian string resources, no direct literal UI copy,
  bundled font, tabular widget text, one canonical domain model and no Android imports in `core`.
- Compiler PSI syntax parsing passed for all 63 Kotlin files. This is not an Android typecheck.
- Added 4 Android-module health preference codec tests (9 local Android-module unit tests total).
- Added 5 connected tests for opt-in/idempotent demo data, persisted subject presets/tests,
  transactional recovery, live threshold changes, and preservation during the v1→v2 migration.
- Updated the Compose tests to use Slovenian resources and verify advanced Settings, merged
  privacy permissions, and the warm widget → fresh fast-add route. Along with the 7 original
  Room tests, the connected suite now contains 17 tests (including maximum-length localized subject presets).

## Remote build status — passed

**GitHub Actions run [34052593518](https://github.com/jakob611/MyDailyRoutine/actions/runs/34052593518), code commit `9c504308bc3209730e0903d9b84655cae47c318b`: both jobs succeeded.**

| Check | Result |
|---|---|
| `:core:test` | 74 tests passed |
| `:app:testDebugUnitTest` | 9 tests passed |
| `:app:assembleDebug` | APK built |
| `:app:lintDebug` | Passed |
| `:app:connectedDebugAndroidTest` | 17 tests passed on API 35 |
| SQLite/resource + presentation checks | Passed |

The job used the declared Kotlin 2.2.10/JDK 17/API 36 build, not the standalone
Kotlin 2.0.21 fallback. Artifacts contain the debug APK, test/lint reports and actual
Room/KSP-generated v2 schema JSON. No schema identity hash was fabricated.

Issues found and fixed during CI:

- Inherited indentation in the selectively ported seeder triggered `SuspiciousIndentation`;
  formatting was fixed without disabling lint.
- A legacy expression-bodied Room test returned a list; all suspend-backed JUnit methods now
  explicitly return `Unit`.
- FAB lookup now uses a stable semantics tag rather than relying on merged label text.
- AndroidX ActivityScenario filters lifecycle callbacks by the launch intent. A real warm
  widget launch correctly calls production `setIntent`; the test restores only its original
  harness intent in `finally` so teardown can observe DESTROYED, while asserting the actual
  app is RESUMED and the correct fresh sheet is visible.

A temporary GitHub authentication error cleared on retry. It was not a workflow-file
permission rejection; all fixes were pushed on the same branch. Direct local Gradle download
still fails its TLS handshake, which is why the successful Android verification was remote.

## Reproduction

```bash
python3 tools/check_sqlite_integrity.py
python3 tools/check_presentation.py
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

`tools/test-core.sh` is the SDK-free alternative with locally supplied Kotlin/JUnit jars.
The GitHub workflow publishes structured compiler/lint/JUnit annotations via
`tools/report_ci_failures.py`, so diagnostics do not depend on downloading log archives.

## Remaining device / release checklist

1. Verify empty first launch, fixed OLED colors, Slovenian labels and accessible font scaling.
2. Create a subject; check its three presets, rename/recolor/change duration, save a one-tap
   test, and delete the subject without deleting its linked milestones.
3. Check editing an IA/EE deadline does not convert it to an exam when selecting a subject.
4. Change each advanced threshold/switch without editing the database; observe changed badges.
5. Tap a badge: verify real recovery, preserved focus minutes, untouched next week/fixed
   commitments, and clear behavior when no safe slot exists. Long-press drag in 15-minute steps.
6. Verify demo loading is explicit, confirmed, idempotent, unofficially labelled and silent.
7. Verify system + app haptic opt-out and tap/drag/completion/warning patterns on hardware.
8. Grant/deny exact alarm and notification access; test quiet windows, Doze, reboot/unlock,
   force-stop/reopen, time-zone changes, midnight and Ljubljana DST transitions.
9. Add/resize the Glance widget, check live-session DB refresh, NOW/UP-NEXT, date rollover,
   tabular figures and a cold/warm quick-add launch. Non-wakeup refresh may be deferred by Android.
10. Retain generated Room schema JSON; run release R8 and inspect Compose performance reports
    on a lower-end device before claiming a production-ready release.
