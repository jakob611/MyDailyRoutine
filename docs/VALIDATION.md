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

## Remote build status

- Canonical base `839b840`: GitHub build job passed; its emulator job failed.
- Integration `0e7c067`, run `34050382229`: build reached Android lint, which reported
  `SuspiciousIndentation` in the selectively ported demo seeder. The indentation is corrected
  in the local follow-up; lint has not been disabled.
- The first integration emulator run reported two failures: a legacy expression-bodied Room
  test did not return `Unit`, and the FAB label was not found in the merged semantics tree.
  Tests now explicitly return `Unit` and target the FAB's stable test tag. Warm widget navigation
  and deadline-type preservation are also corrected. Follow-up CI is pending.
- A temporary GitHub authentication error cleared on retry; it was not a workflow-file rejection.
- Follow-up `8b71abf`, run `34051695928`: **APK assembly, core/app JVM tests and lint all passed**.
  The emulator suite reached execution with one remaining failure in `ActivityScenario` cleanup
  after a warm widget intent. AndroidX filters lifecycle callbacks by the original intent; the
  test now restores that harness intent in `finally`, preserving production `setIntent` behavior
  and asserting the real activity remains RESUMED. Final verification of that correction is pending.
- Direct local `./gradlew :core:test :app:testDebugUnitTest --stacktrace` remains blocked by
  a TLS handshake failure downloading Gradle 8.13. The newer cleanup/polish changes and an all-green connected suite are not claimed verified yet.

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
