# My Daily Routine

An offline-first Android time-blocking app for school, focused study, personal routines, recovery, and IB milestones. Kotlin 2.x, Jetpack Compose / Material 3, Room, Coroutines / Flow, AlarmManager, and Glance. No account, HTTP client, telemetry, or runtime network permission.

## What is implemented

- **Daily timeline:** time-scaled gaps, subject colors, deep-work borders, muted recovery cards, current-block highlighting, inline health guidance, haptics, spring placement and expansion animations.
- **Weekly grid:** all seven days, proportional placement, separate overlap lanes, daily allocation indicators, and dated milestones.
- **Monthly heatmap:** focus density, exam/deadline markers, and no-school days; tap through to a day.
- **School-year view:** editable teaching-end countdown, twelve-month holiday distribution, eight-week milestone density, and upcoming deadlines.
- **Fast add:** saved subject chips; 90-minute deep work, 45-minute Pomodoro, 15-minute walk, IB revision, and exam presets. One-off by default; weekly recurrence is an explicit choice.
- **Editing:** rename/reschedule one occurrence or its weekly blueprint, per-occurrence completion, skip/restore (including overnight carry-ins), reminder toggles, milestone editing, subject management, and confirmed deletion.
- **Notifications:** five-minute previews for ordinary blocks; recovery reminders at their start. Configurable silent school window, runtime permission handling, and an explicit approximate fallback when precise alarm access is unavailable.
- **Home-screen widget:** remaining agenda, NOW / UP NEXT labels, school-year countdown, and a direct Fast-Add activity action.

No sample timetable or invented IB deadlines are inserted. Add your own subjects and blocks. The **verified 2026/27 Slovenian Western-region secondary-school calendar** is bundled; see [calendar sources and scope](docs/CALENDAR.md).

## Build and run

Use **JDK 17**, Android SDK **36**, and Build Tools **36.0.0**. Open the project in Android Studio and choose JDK 17 as the Gradle JDK, or set `JAVA_HOME` and your local SDK location (`ANDROID_HOME` / ignored `local.properties`).

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
# With an emulator/device connected:
./gradlew :app:connectedDebugAndroidTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The build is pinned to AGP 8.13.2 / Gradle 8.13 / Kotlin 2.2.10 / KSP 2.2.10-2.0.2. The starter's JDK-25-specific daemon configuration was removed in favor of a portable JDK-17 toolchain. Minimum Android version remains API 24, with `java.time` core-library desugaring. Dependency downloads happen at **build time only**; the installed app has no network access.

Release builds enable R8 and resource shrinking. To collect Compose stability / composition reports:

```bash
./gradlew :app:assembleRelease -PcomposeReports=true
```

Reports go under `app/build/compose-reports` and `app/build/compose-metrics`.

## Architecture and entry points

```text
Compose → TimelineAction → ViewModel → domain repository interfaces
   ↑                         ↓                    ↓
immutable StateFlow ← resolved snapshot ← Room transaction + invalidation Flow
                                     ↘ health engine
                                     ↘ alarm planner / receiver
                                     ↘ Glance agenda
```

`core` is a pure Kotlin/JVM module. It has no Android or Compose dependency. `app` supplies persistence, presentation, preferences, and Android scheduling. `AppGraph` is the explicit composition root; no reflection-based DI or network service is involved.

| Phase | Main files |
|---|---|
| Room schema, converters, indexed DAOs | `app/src/main/java/com/example/mydailyroutine/data/local/` |
| Database creation, synchronous calendar seed, SQLite invariants | `RoutineDatabase.kt`, `DatabaseIntegrity.kt` |
| Transactional, reactive repository | `data/repository/RoomTimelineRepository.kt` |
| Recurrence / overnight resolution | `core/src/main/kotlin/com/example/mydailyroutine/domain/repository/TimelineResolver.kt` |
| Deterministic ergonomic rules | `core/.../domain/health/ScheduleHealthEngine.kt` |
| UDF state and commands | `app/.../ui/timeline/TimelineState.kt`, `TimelineViewModel.kt` |
| Main timeline / four views | `app/.../ui/timeline/TimelineScreen.kt`, `DailyTimeline.kt`, `ui/overview/OverviewScreens.kt` |
| Fast add / editing / settings | `app/.../ui/editor/`, `app/.../ui/settings/` |
| Exact-alarm chain, permission fallback, notifications, reboot recovery | `app/.../scheduling/` |
| Pure scheduling / DST logic | `core/.../domain/scheduling/AlarmPlanner.kt` |
| Glance widget | `app/.../widget/AgendaWidget.kt`, `res/xml/agenda_widget_info.xml` |

### Data contracts

- Foreign keys are enabled and checked at database open. Deleting a subject **detaches** its routines and milestones; deleting a routine **cascades** to its overrides, completion records, and delivery claims.
- `(routineBlockId, overrideDate)` is unique. Room writes never use destructive `REPLACE` for parent entities. Additional SQLite triggers reject invalid enum values, empty titles, out-of-range times, zero-duration blocks, and exceptions/completions that do not belong to an occurrence.
- Dates are epoch-day integers, times are minute-of-day integers, weekdays use ISO 1–7, and categories use stable names. Second/nanosecond precision is rejected, not silently truncated.
- `validFrom` / `validUntil` extend the weekly template so fast-add can create genuine one-off blocks and future-starting weekly routines. Equal bounds mean one-off. An earlier end time means the next day; equal start/end is invalid.
- Completion is stored per **origin date**, not on the weekly template. A midnight-spanning occurrence shares completion and cancellation across both display days.
- A calendar work-free day tags the whole **origin-date SCHOOL occurrence** as inactive. It remains visible but contributes neither alarms nor health load. Other categories and milestones are not suppressed.
- All-day milestones sort at midnight, timed milestones at their due time, with deterministic tie-breaking. Markers do not reserve duration or send block reminders.
- Room table invalidations cause one transactional snapshot read, preventing mixed blueprint/override states. UI transformations run off the main thread. Presentation uses persistent collections and immutable state; collectors stop when the UI is not subscribed.

The database is version 1: no previous Room schema exists in the starter. Schema export is configured under `app/schemas`; KSP generates the initial JSON during the first Android build. Commit that generated schema before a future version bump and ship explicit migrations. There is deliberately **no destructive migration fallback**.

## Scheduling and battery policy

See [scheduling behavior and Android limitations](docs/SCHEDULING.md) and [health-rule semantics](docs/HEALTH_RULES.md).

The scheduler resolves all active blueprints in a 370-day window but arms only the **next reminder batch**, then chains forward. This avoids AlarmManager's per-app alarm-count ceiling. Simultaneous reminders are batched and notifications use unique tags, not collision-prone integer hashes. Unusually distant bounded routines are rechecked with a non-wakeup maintenance alarm.

Widget updates use a separate **non-wakeup** alarm at the next start/end/midnight boundary, plus explicit updates after edits and on app resume. There is no polling service, repeating exact alarm, or app-owned CPU wakelock. Glance may use its own bounded AndroidX worker internally.

Health thresholds are **planning heuristics, not medical advice or universal cognitive limits**. Their requested warning text is accompanied by that clarification in the UI.

## Verification status

- **49 core JVM tests passed** with the standalone runner.
- **11 SQLite / resource smoke tests passed** (`python3 tools/check_sqlite_integrity.py`).
- All 46 Kotlin source files passed compiler PSI syntax parsing.
- Five Android-module converter unit tests, seven Room instrumentation tests, and two Compose navigation tests are included, plus GitHub Actions build/lint/device-test jobs.
- **Full Android compilation, lint, and device tests are not yet verified in this sandbox.** The Gradle distribution download failed with `SSLHandshakeException`; Google/Maven artifact downloads were also unavailable. No APK or device-rendered screenshot is claimed.

See [validation details and device checklist](docs/VALIDATION.md). No generated toolchains, binaries, or build outputs are tracked.

## Local-data privacy

No Internet or network-state permission survives manifest merging. No analytics/crash-reporting SDK is installed. Cloud backup and automatic device transfer are disabled. **Uninstalling the app or clearing its storage deletes the schedule.** All notification processing and recurrence calculations use local data; no credentials are required.
