# My Daily Routine

A Slovenian-language, OLED-dark-first Android time-blocking app for school, focused study, personal routines, recovery, and IB milestones. Kotlin 2.x, Jetpack Compose / Material 3, Room, Coroutines / Flow, AlarmManager, and Glance. No account, HTTP client, telemetry, or runtime network permission.

## Latest operational review

The follow-up [logic audit](docs/audits/LOGIC_REVIEW.md) addresses active-session safety,
stage ordering, review metadata, overnight budgets and measured timestamps through DST.
Room v6 preserves legacy records through explicit migrations. Local core/SQLite checks pass;
Android verification of this latest follow-up is being rerun, not inferred from prior green runs.

## Complete PDF audit and feature-first architecture

The user's complete 19-page PDF is now retained in `docs/research/` and has been read in full.
[The page-by-page audit](docs/audits/FULL_PDF_REVIEW.md) identifies both previous implementation gaps
and contradictions in the paper's example code; unsafe fixed-boundary movement is not copied.

The current review adds max(1, ν) calibration, compact review fallback, real daily review capacity,
protected recovery, ordered deliverable stages, explicit start/finish with foreground/resume healing,
actual completion geometry and a faster editor with a persistent save footer. App packages are now
feature-first with shared persistence/design primitives; see [ARCHITECTURE.md](docs/ARCHITECTURE.md).

Local checks currently pass **117 core tests**, **14 SQLite checks**, **480 Slovenian resources**
and syntax parsing for **103 Kotlin files**. Android compilation and connected verification of
this latest audit are being rerun; the prior CI records below are not claimed to verify new changes.

## Computational planning extension

The current branch adds minute-duration blocks with explicit Room v3/v4 migrations, explicit actual-time history, five-phase
slippage recovery, RSEM reserves, calibrated estimates, Gaussian circadian costs, capacity-limited
spaced reviews, reverse milestone preparation and a persistent backlog. All features reuse the
existing app/resolver/alarms/widget. See [the mathematical model and integration policies](docs/CHRONOBIOLOGY_ENGINE.md).

Current local research-extension checks: **107 core tests**, **13 SQLite checks**, **458 Slovenian
resources**, and syntax parsing for **85 Kotlin files**. All research changes are verified in [CI run 34097272103](https://github.com/jakob611/MyDailyRoutine/actions/runs/34097272103), code commit `f9241d7`: APK assembly, lint, core/app JVM tests and API-35 connected tests passed.

## What is implemented

- **Daily timeline:** time-scaled gaps, subject colors, deep-work borders, muted recovery cards, current-block highlighting, inline health guidance, haptics, spring placement and expansion animations.
- **Weekly grid:** all seven days, proportional placement, separate overlap lanes, daily allocation indicators, and dated milestones.
- **Monthly heatmap:** focus density, exam/deadline markers, and no-school days; tap through to a day.
- **School-year view:** editable teaching-end countdown, twelve-month holiday distribution, eight-week milestone density, and upcoming deadlines.
- **Fast add:** saved subject chips; 90-minute deep work, 45-minute Pomodoro, 15-minute walk, IB revision, and exam presets. One-off by default; weekly recurrence is an explicit choice.
- **Editing:** rename/reschedule one occurrence or its weekly blueprint, per-occurrence completion, skip/restore (including overnight carry-ins), reminder toggles, milestone editing, subject management, and confirmed deletion.
- **Notifications:** five-minute previews for ordinary blocks; recovery reminders at their start. Configurable silent school window, runtime permission handling, and an explicit approximate fallback when precise alarm access is unavailable.
- **Home-screen widget:** remaining agenda, NOW / UP NEXT labels, school-year countdown, and a direct Fast-Add activity action.

No example timetable is inserted at startup. Settings offers a confirmed, one-time **Naloži primer podatkov** action: 6 sample subjects, 63 weekly blocks and 15 explicitly unofficial example deadlines, with all sample reminders off. Existing data is preserved. The **verified 2026/27 Slovenian Western-region secondary-school calendar** is bundled; see [calendar sources and scope](docs/CALENDAR.md).

## Connected Slovenian / OLED integration

- Shared OLED palette, hairline cards, bundled Roboto Flex, tabular clock/countdown figures, and 180 ms period transitions.
- Agent3's adapted components replace the older daily card, navigator, load bar and calendar/warning UI; there is no second model or navigation stack.
- Live NOW spine, real 15-minute-step long-press rescheduling, and distinct opt-out haptics for tap, drag, completion and new warnings.
- Each saved subject automatically provides **Pouk / Učenje / Test** presets. Presets react to name, color and duration edits; deletion removes them without orphan rows. The milestone adder offers one-tap **Predpisan test** entries for the selected date/time.
- **Napredne nastavitve** exposes every rule's threshold and on/off switch. DataStore changes re-evaluate health warnings without requiring a database edit.
- A health badge requests transactional recovery insertion. It can split/move focus while preserving study minutes and the weekly template; it never displaces fixed commitments. When unsafe, it uses a real free slot or reports that nothing changed.
- All app-owned display copy is Slovenian Android resources, including notices, errors, presets, notifications and widget text. User-entered titles are never silently translated.

See [source integration and behavior](docs/SOURCE_INTEGRATION.md) for the exact source commits and adaptations.

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

The database is version 4. Explicit v1→v2→v3→v4 migrations preserve IDs, overrides, completions and alarm claims; introduce minute-duration blocks, learning/history/backlog tables; and preserve raw estimates across backlog restoration. No destructive fallback is used. Schema export remains configured under `app/schemas`. A baseline v1 SQL fixture and a device migration test exercise the upgrade. Retain generated schema JSON before a future version bump. There is deliberately **no destructive migration fallback**.

## Scheduling and battery policy

See [scheduling behavior and Android limitations](docs/SCHEDULING.md) and [health-rule semantics](docs/HEALTH_RULES.md).

The scheduler resolves all active blueprints in a 370-day window but arms only the **next reminder batch**, then chains forward. This avoids AlarmManager's per-app alarm-count ceiling. Simultaneous reminders are batched and notifications use unique tags, not collision-prone integer hashes. Unusually distant bounded routines are rechecked with a non-wakeup maintenance alarm.

Widget updates use a separate **non-wakeup** alarm at the next start/end/midnight boundary, plus explicit updates after edits and on app resume. There is no polling service, repeating exact alarm, or app-owned CPU wakelock. Glance may use its own bounded AndroidX worker internally.

Health thresholds are **planning heuristics, not medical advice or universal cognitive limits**. Their requested warning text is accompanied by that clarification in the UI.

## Verification status

**Verified in GitHub Actions:** [run 34097272103](https://github.com/jakob611/MyDailyRoutine/actions/runs/34097272103), application/test code commit `f9241d7`.

- Android debug APK assembly and Android lint **passed** with Kotlin 2.2.10 / JDK 17 / API 36.
- **107 core JVM tests** and **12 app JVM tests passed**.
- **24 connected tests passed on the API-35 emulator**: Room integrity, migrations through v4, actual-time velocity, backlog preservation/restoration, real review blocks, topic cascades, linked review/preparation plans, locale/privacy, four-view navigation and warm widget quick-add.
- **13 SQLite checks** and presentation checks for **458 Slovenian strings**, bundled font, tabular text and a single canonical domain model passed.
- Local standalone core tests and syntax parsing for **85 Kotlin files** also passed. Direct sandbox Gradle/SDK downloads remain TLS-blocked; full Android verification was performed in CI, not claimed locally.

[Download the test APK artifact (ZIP)](https://github.com/jakob611/MyDailyRoutine/actions/runs/34097272103/artifacts/10009372989).
The run also retains test/lint reports and actual Room/KSP schema exports. Its success is not a guarantee about every vendor's Doze behavior, launcher rendering or physical haptic hardware; those, and release R8/signing, remain on the [release checklist](docs/VALIDATION.md).

## Local-data privacy

No Internet or network-state permission survives manifest merging. No analytics/crash-reporting SDK is installed. Cloud backup and automatic device transfer are disabled. **Uninstalling the app or clearing its storage deletes the schedule.** All notification processing and recurrence calculations use local data; no credentials are required.
