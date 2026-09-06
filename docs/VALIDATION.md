# Validation record and release checklist

## Executed in the coding sandbox

| Check | Result |
|---|---|
| Pure Kotlin recurrence, health, calendar, interval/layout, and alarm-planner tests | **49 passed** |
| SQLite integrity / indexed-query / XML / privacy-manifest smoke tests | **11 passed** |
| Kotlin compiler PSI syntax parsing across app + core sources/tests | **46 files passed** |
| `git diff --check` | Passed |

The standalone core suite was compiled and executed using an available **Kotlin 2.0.21 compiler and JDK 17**, with the JUnit 4.13.2 release sources. The project itself is pinned to **Kotlin 2.2.10**; the declared Gradle toolchain is not claimed verified by that standalone run. Downloaded verification tools stayed outside tracked source in the workspace cache.

Reproduce the pure SQLite checks with Python 3 (no SDK or third-party package needed):

```bash
python3 tools/check_sqlite_integrity.py
```

That script derives table columns/FKs/indexes from `Entities.kt`, executes the application's actual trigger predicates, and tests referential integrity, unique overrides/claims, invalid inputs, cascade/SET NULL deletion, negative epoch weekdays, indexed queries, XML validity, and explicit removal of network permissions. It does **not** exercise Room's generated implementation.

An optional SDK/Gradle-free Kotlin runner is also included. Supply your locally installed Kotlin 2.x compiler and test jars; it downloads nothing:

```bash
JAVA_HOME=/path/to/jdk17 \
KOTLINC=/path/to/kotlinc/bin/kotlinc \
JUNIT_JAR=/path/to/junit-4.13.2.jar \
HAMCREST_JAR=/path/to/hamcrest-core-1.3.jar \
./tools/test-core.sh
```

`COROUTINES_JAR` can override the compiler distribution's bundled coroutines jar. Results are written to ignored `core/build/standalone`.

## Blocked here, not represented as passed

The full command attempted was:

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --stacktrace
```

It failed before Gradle configuration because `services.gradle.org` terminated the TLS handshake while downloading Gradle 8.13 (`SSLHandshakeException`, caused by EOF). Direct Google/Maven and SDK downloads were also unavailable. Therefore:

- Android compilation, Room KSP generation, and R8 **have not been validated here**.
- Android lint **has not run**.
- No emulator/device was available; UI rendering, widget host behavior, actual permission flows, alarms during Doze, and reboot delivery **have not been device-tested**.
- No built APK is claimed.

`.github/workflows/android.yml` contains the standard JDK-17 / API-36 build, unit/lint checks, and an API-35 emulator job. Those jobs are provided, **not claimed to have run**. KSP's initial schema export should be retained from the first successful build before a later database version bump.

## Additional tests included for Android builds

- **5 converter unit tests:** nullable time/date round trips, pre-1970 dates, weekday/category encoding, minute precision, invalid stored minutes.
- **7 Room instrumentation tests:** synchronous calendar seeding; FK enablement/orphan rejection; subject detachment/routine cascades; unique override updates; raw-SQL invariants; delivery-claim deduplication; transactional reactive resolution and subject changes.
- **2 Compose tests:** navigation through all four views and fast-add; accessing Settings without granting a permission.

## Device checklist before a release

1. Install fresh; verify a genuinely empty agenda and the populated 2026/27 holiday calendar. No account or permission should be required to plan.
2. Add/edit/delete a subject; verify deletion detaches, rather than removes, its routines and milestones.
3. Add a one-off and a weekly block; reschedule just one occurrence and compare the following week. Skip/restore an overnight carry-in from either day.
4. Complete a block and verify the next week is not completed. Test app recreation and reopening to confirm persisted data and selected-date restoration.
5. Add timed/all-day exams and IB markers. Check day ordering, month dots, yearly radar, and large-font accessibility.
6. Check the exact 90/180/300/120-minute rule boundaries, school transition, overlaps, and real versus overlapping recovery.
7. Deny notification/exact-alarm access, then grant each through Settings. Verify no crash, clear fallback status, correct cancellation/rearming, and silent school-window delivery.
8. Schedule reminders across midnight and the Ljubljana DST transitions; change the device zone/time and verify the same occurrence is not delivered twice.
9. Reboot/unlock and verify upcoming reminders are reconstructed. Force-stop/open and verify the documented platform limitation.
10. Add/resize/delete the Glance widget on multiple launchers. Check NOW / UP NEXT, scrollability, non-wakeup boundary updates, stale timestamp, and cold/warm Fast-Add launches.
11. Inspect the merged release manifest for absence of Internet/network-state permissions, run lint/R8, retain Room schema JSON, and inspect Compose reports on a physical low-end device.
