# Faster lessons, weekly routines and planned sleep

## What already existed

Subject presets supplied a default duration once, but editing the start field did not recompute
the end. Repeating entries were restricted to the selected date's single weekday. The study window
was not a sleep schedule. This extension closes those gaps without introducing another agenda store.

## Input behavior

- `TimeEntryState` preserves the selected duration when a valid start is typed. Partial/invalid
  start input does not destroy the previous end. Editing a valid end intentionally changes the
  duration preserved by subsequent start edits. Midnight wrap is explicit and tested.
- School without a subject uses the configured lesson default (45 minutes initially); saved
  subjects retain their own default. Duration chips provide quick overrides. Settings can change
  defaults for new subjects and the default short break.
- Repeating entries expose seven weekdays plus Workdays/Every day shortcuts. One transaction creates
  one indexed weekly blueprint per chosen day, with a shared opaque series key. There are no fake
  cancellation rows for unselected days. Viewing a saved series opens its first matching date.
- A whole-series edit/delete operates on every selected weekday; editing/skipping just one date
  remains a date exception. Running work prevents an unsafe group mutation.
- Lunch/snack presets choose a fixed time slot on workdays, with editable days/times. They clear a
  previously selected school subject instead of accidentally associating lunch with it.
- “Save and add next lesson” opens a fresh editor at lesson end plus the chosen break, preserving
  repetition choices. If midnight is crossed, the selected weekday mask rotates with the date.

## Optional five-minute school breaks

Companions are real Room blocks with a self-FK to their lesson. They are derived from the resolved
parent end, so time edits/overrides/overnight endings stay aligned. Parent cancellation/disabled
state hides the child; a school holiday also suppresses it. Parent deletion cascades to companions.

A break never moves the next class or pretends overlapping work is recovery. A conflict (including
an already allocated rest) makes the companion inactive for that occurrence; it contributes neither
health recovery nor a reminder. The compact card explains that behavior. Independent skip/delete of
a lesson break does not delete its parent/whole series. Break reminders are off by default.

## Sleep and wake

Sleep settings are explicit and off initially. They generate silent, fixed managed time blocks in
Room; planner availability, execution protection and the widget use the same canonical resolver.
They are a **plan, not sleep measurement or a wake-up alarm**. Defaults are 23:00–07:00 and an optional
30-minute morning transition, all editable.

Weekdays mean the day of bedtime. “Nights before workdays” selects Sunday–Thursday so wake-ups are
Monday–Friday. An earlier wake clock time means the next date. Existing started nights are preserved;
changes apply at the next eligible bedtime. Saving unchanged settings is idempotent. Future explicit
skip exceptions are transferred to the new managed pattern; removed days do not keep orphan events.
Disabled settings retain editable times/days without generating future active blocks.

Sleep and linked breaks render compactly rather than filling the screen with hours of empty card
space. They do not count as permanently incomplete tasks. Sleep is excluded from the daytime weekly
grid/load bar, but remains a protected scheduling interval.

## Persistence and verification

Room v7 adds nullable series/parent references plus provenance/enabled flags. Existing v6 rows keep
their IDs, dates and durations and become enabled USER entries. Foreign keys remain enabled during
migration. A unique parent index and integrity triggers prevent cycles, duplicate companions and
mismatched ownership weekdays. The app retains one `ResolvedTimelineItem` model and one resolver.

Tests cover duration edits, partial times, manual ends, midnight, weekday expansion, companion
movement/cancellation/holiday/conflict, grouped edits/deletion, opt-in sleep, preserved nights and
future skips, idempotency, disabled sleep, v6→v7 migration and Compose input interactions.
Actual Android build/device results are recorded separately in `docs/VALIDATION.md` and CI.
