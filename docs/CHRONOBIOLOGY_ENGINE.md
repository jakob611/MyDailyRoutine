# Offline chronobiology and schedule repair

This extends the existing single-Activity `core` / `app` architecture. It does not add a solver,
network client, second schedule database, or physiological telemetry. The supplied research text
contains missing equation renderings and an incomplete Kotlin listing. The explicit formulas and
policies below are the implemented interpretation, not claims that the physiological assertions
are universally established or personally measured.

## Canonical data and migration

Room v3 introduces `TimeBlockEntity` with integer `startMinutes` and `durationMinutes`, minimum duration,
elasticity, priority, fixed-commitment status, raw estimate, and optional actual completion minutes.
The indexed `routine_blocks` table name and IDs are retained for existing foreign keys. Categories
migrate SCHOOL→SCHOOL, FOCUS_STUDY→FOCUS_ANALYTICAL, PROJECT→FOCUS_SYNTHESIZING,
PERSONAL→ADMIN, REST_BREAK→REST_BUFFER. Existing school and personal commitments are fixed;
existing recovery minimums are protected. No actual-time history is invented for old completions.

A separate v3→v4 migration preserves raw estimates in backlog rows (recovering the source estimate where available). It does not change a released schema in place.

A v2→v3 migration temporarily copies parent and child rows, recreates their schema, restores all
IDs/overrides/completions/delivery claims, and verifies foreign keys. FK enforcement is never
switched off inside Room's migration transaction. The full v1→v2→v3→v4 path remains supported.

`HistoricalVelocityEntity`, `StudyTopicEntity`, `SpacedReviewEntity`, and `BacklogEntryEntity` are
indexed and FK-linked. Scheduled reviews are real one-off time blocks; review metadata is updated
in the same transaction when healing changes their date. Topic deletion cascades to its reviews
and blocks. Subject deletion detaches data; deleted routines leave anonymized source references in
useful historical velocity rows. A milestone stores epoch-day deadline, estimated effort, and an
explicit terminal-exam flag while retaining the existing date/time editor.

Weekly blueprints are not modified by healing. Changes use origin-date overrides with a bounded
0–7 day shift. The resolver searches the corresponding carry-in horizon, keeps occurrence identity,
and clips only display intervals. This preserves midnight crossings, alarms and completion identity.

## Five-phase deterministic recovery

`DeterministicReschedulingEngine.recover` accepts immutable numerical projections of resolved
occurrences; this is not a second domain/store. All returned start/duration values are integers.

1. **Slack search**: project unfinished work from the actual restart minute up to the next fixed
   boundary (including terminal timed milestone barriers). Existing gaps absorb delay before any
   duration is changed. School/fixed commitments and completed work are untouched.
2. **CCPM consumption**: consume emergency reserves, then only the portion of recovery/buffer blocks
   above their protected minima. A zero-duration consumed buffer is removed via a date exception,
   never written as an invalid zero-length Room block.
3. **Buttazzo compression**: minimize `sum(x_i² / (2 e_i))` for reductions `x_i`, bounded by
   `0 <= x_i <= d_i - d_min`. Active-set water filling repeatedly locks saturated tasks and
   redistributes the remaining deficit. Floor allocations plus deterministic largest remainders
   give an exact integer total. Elasticity zero is never compressed.
4. **Knapsack-style deferral**: greedily remove the lowest priority-per-remaining-minute tasks,
   retesting feasibility after each removal. This is an O(N²) deterministic heuristic, not a claim
   of exact 0/1-knapsack optimality. The backlog retains each deferred task's original uncompleted
   duration and metadata; nothing is silently deleted. User removal is an explicit action.
5. **Forward regeneration**: `start_new = max(previous_end, start_old)`, with a single bounded
   adjacent-swap pass that improves circadian cost only while feasibility is preserved. If actual
   time has passed the boundary, soft work goes to backlog rather than moving the boundary.

The planner uses a bounded two-day projection to include overnight work and the next fixed morning
boundary. Explicit overrides can represent shifted occurrences without wrapping 00:30 to yesterday.
After repair, review and daily study budgets are rechecked per destination day; excess non-fixed
work is deferred. Repeated identical healing requests do not reapply the original delay to already
shifted starts. No red overdue or failure state is introduced.

Water filling, density-greedy deferral with forward scans, and the single circadian improvement pass
are O(N²); sorts are O(N log N). No repeated global search, MILP/QP dependency, or remote solver is used.

## Velocity and uncertainty

`VelocityCalibrator.getCalibratedDuration(rawMinutes, subjectId: String)` uses
`nu = sum(actual) / sum(raw planned)`, not a mean of individual ratios. It takes the latest 120 valid
samples per subject, defaults to 1.0 without history, and bounds the multiplier to 0.5–2.5 as an
explicit robustness policy. Its ceiling uses integer rational arithmetic, avoiding errors such as
`90 * 1.1` rounding up to 100. Stored block durations remain 1–1439 minutes. Historical inputs remain
raw estimates so estimates are not repeatedly inflated by applying the multiplier to themselves.

Only explicit actual-time entries create velocity samples. Editing a completion updates its unique
sample; undo removes that sample. Actuals belong to an occurrence, not every week of a template.

`RsemBufferSizer` implements `ceil(sqrt(sum((t90 - t50)^2)))`. Calibrated nominal estimates are t50;
conservative history uses a bounded empirical 90th percentile ratio. With no history, 1.5× is an
explicit conservative planning default, not a measured completion probability. Existing protected
physical recovery is not assumed freely consumable.

## Circadian cost

`P = omega * priority * duration * exp(-0.5 * (distance(midpoint, 14:15) / 45)^2)`.
Weights are analytical 1.0, synthesizing 0.60, administrative 0.05; buffers/rest and fixed school
are not optimized for this cost. Time is periodic across midnight. Center/spread are configurable.
This is the supplied midpoint Gaussian cost, not a full personal SAFTE/sleep-debt estimator.

Fixed commitments win over the soft circadian preference. A constrained day may still contain
analytical work during the dip; the engine does not pretend an infeasible circadian ideal is solved.
Slot selection evaluates interval endpoints and the opposite phase instead of repeatedly scanning
all possible start minutes; per-day occupancy is cached in a bounded 1440-minute grid.

## Spacing and reverse planning

`SpacedRepetitionPlanner` uses `initial + round((final-initial) * (k/N)^1.8)`, then enforces strictly
increasing whole days. A too-short horizon produces unscheduled records/backlog, not duplicate
same-day sessions. Each ideal date searches `±floor(0.15 * ISI)` days, respects the terminal cutoff,
and chooses least study congestion, then nearest ideal date, then earlier date. Daily review budget
is exactly integer `studyCapacity / 5`; existing reviews count toward it. Time-slot failure remains
visible in the backlog rather than creating an overlapping review.

`MilestoneBackPlanner` calibrates effort, creates chunks of at most the target 75 minutes (configurable
25–90), and distributes them backwards over the available horizon. Terminal goals use an earlier
virtual deadline at 85% of the release-to-deadline interval; that factor is an explicit policy for
an omitted paper parameter. RSEM reserve is allocated near the chain's end. All fixed blocks, exam
points, existing study/reviews, configured study windows, daily capacity (default 270), and protected
post-school recovery (default 45) are respected. Unplaced effort stays in backlog. Existing linked
reviews do not incorrectly prevent preparation generation; repeated preparation requests are
idempotent while their generated tasks remain.

## UI and background behavior

The existing Slovenian OLED UI uses a minute-proportional 2 dp/min block body with a 112 dp accessibility minimum for short blocks, and exposes actual completion minutes, elastic block parameters,
calibration preview, a one-tap delay repair, reserve creation, backlog scheduling, learning topics,
review planning, milestone effort and terminal exams. Planning settings control capacity, study
window, dip center/spread, default delay, focus target and school recovery. Guidance is calm and
non-punitive, with explicit model limitations.

The Glance widget renders the active block, a snapshot progress bar, remaining emergency reserve,
and the next two blocks. Its timestamp/manual refresh makes staleness clear. Progress is not driven
by a polling service: the existing non-wakeup boundary refresh and explicit updates remain in use.
Emergency reserves do not create reminder wakeups. Exact reminder chaining, permission fallback,
quiet channels, reboot/time-zone recovery, and the shared resolver remain intact.

## Verification

Pure JVM tests cover saturation, exact integer sums, extreme Int.MAX_VALUE slippage, midnight,
fixed commitments, deterministic order, velocity ratio/bounds, Gaussian weights, RSEM, geometric
spacing, jitter/capacity deferral and reverse-plan effort conservation. SQLite smoke tests execute
migration and integrity rules. Connected tests cover persistence, backlog restoration, actual-time
history, real review blocks, topic cascades, and linked review/preparation planning.
Android compilation/lint/device verification is recorded in `VALIDATION.md`; do not infer runtime
verification from successful file generation or syntax parsing alone.
