# Connected source integration

The canonical `core` / `app` implementation is retained from agent4, commit
`839b840ca936c259df855d7d68f398d65f06f79a`, on the session's fixed
`arena/01a0766c-mydailyroutine` branch. No alternate architecture, entity hierarchy,
navigation graph, or `ResolvedTimelineItem` was merged.

## Selective ports

- **Agent3** (`arena/01a0767a-mydailyroutine`, `d3cc16b6ffd66eb09e045a2b6ce5b7b7ae16a120`):
  adapted the time-gutter/card/action composition and the `DateNavigator`, `DayLoadBar`,
  `HealthWarningCard`, and `CalendarNoticeCard` patterns into `ui/components`. The old
  private daily card, metric tile, date navigator, and duplicate legend implementations
  were replaced, not retained alongside them. NOW/UP-NEXT use agent4's resolved instants.
- **Agent2** (`arena/01a0766d-mydailyroutine`, `d439b635b3ee29eb6cdd0b8c8925da0162a6ffb9`):
  ported the actual 63-block weekly IB layout and 15 example milestone dates from its
  `DemoDataSeeder`. All copy is resource-backed Slovenian. Import is explicit, confirmed,
  transactional, idempotent, bounded to 2026/27, labelled **Primer / neuradni datum**, and
  all example reminders are disabled. Existing user rows are never replaced. The original
  seeder's fallback/guessed IDs and startup invocation were deliberately not reused.
- Agent2's recovery/transition wording informed the concise Slovenian resource copy, while
  agent4's interval-union and recovery rules are unchanged at their default settings.
- Agent1 was not needed; no data layer or screen was copied from it.

## One source of truth

- One pure-JVM domain model and recurrence resolver; one Room database and repository.
- One fixed OLED palette (`DesignSystem.kt`) and bundled Roboto Flex font, shared with Glance.
- One UDF screen graph (`TimelineMode` / `TimelineAction`), including existing CRUD and Settings.
- Subject presets are derived, not duplicated database rows: every subject yields stable
  lesson/study/test keys. Renames, colors, durations, and deletion immediately propagate.
- User health configuration is persisted atomically in DataStore and combined with Room
  snapshots before evaluation. Rule copy is exclusively in Android string resources.
- Break insertion re-evaluates a warning in a Room transaction, preserving study duration and
  the weekly blueprint. It never moves fixed school/personal/project commitments or exams.
  A conflict falls back to a real free slot; no safe slot produces a localized explanation,
  not an overlapping fake break. Already-resolved warnings do not insert duplicates.
- Room v2 adds only an opt-in demo-import receipt. An explicit v1→v2 migration preserves
  all existing subjects, routines, overrides, completion history, and milestones.
- Glance stores only a render revision, observes Room while its bounded session is alive,
  and uses non-wakeup boundary updates. Android TextView interop provides tabular widget text.

## School profile

The regular teaching end (3rd-year timetable) remains **24 June 2027**. Settings also offers
**21 May 2027** for final-year students and a custom date. Changing the countdown does not
silently cancel real routine templates. The western winter break remains **22–26 February
2027**. Existing calendar sources and no-extrapolation policy are documented in `CALENDAR.md`.
