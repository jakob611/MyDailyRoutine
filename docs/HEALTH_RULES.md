# Deterministic schedule-health semantics

`ScheduleHealthEngine.evaluate(List<ResolvedTimelineItem>)` is pure Kotlin and accepts **one display date**. It performs no I/O, reads no clock, and changes no schedule. The requested text is emitted verbatim, but the UI explicitly describes these as **planning heuristics, not medical advice or universal scientific limits**.

## Exact boundaries

| Warning | Trigger | Does not trigger at |
|---|---|---|
| `CONCENTRATION_LIMIT` | A single focus occurrence lasts **>90 minutes** | 90 minutes |
| `HIGH_COGNITIVE_LOAD` | Unioned SCHOOL + FOCUS work in one recovery-delimited cluster is **>180 minutes** | 180 minutes |
| `INSUFFICIENT_TRANSITION` | Focus starts **<30 minutes** after the most recent preceding/overlapping school block ends | 30 minutes |
| `BURNOUT_RISK` | Unioned focus time on the display day is **>300 minutes** | 300 minutes |
| `PHYSICAL_RESET` | A deskwork cluster spans **>120 minutes** | 120 minutes |
| `FRAGMENTED_TIME` | A genuinely unallocated gap between focus intervals is **45–90 minutes inclusive** | 44 or 91 minutes |

## Interval policy

- All intervals are half-open `[start, end)`, sorted before evaluation; touching intervals join, overlapping time counts **once**.
- Daily totals and clusters use the display day's clipped civil-time intervals. The single-focus rule uses the full occurrence's nominal duration so a long overnight block cannot evade it by crossing midnight.
- A cognitive reset needs **20 uninterrupted minutes** of explicit `REST_BREAK` or truly unallocated time. Short gaps do not reset accumulated load. Adjacent rest and free intervals can join.
- Deskwork means `SCHOOL`, `FOCUS_STUDY`, and `PROJECT`. A **five-minute** rest/free interval resets its span. Personal activities are not assumed to be sedentary or restorative.
- A rest block overlapping work does not create recovery. Work is subtracted from the possible recovery intervals first.
- Another allocated routine inside a 45–90-minute gap makes it intentional, not fragmented. Milestone markers have no duration and do not occupy a gap.
- Completed blocks still represent work done and remain in load calculations. Holiday-inactive school blocks and all milestones are excluded.
- Each saturated cluster gets one warning of its type, associated with its relevant block keys. Output order is deterministic by time and warning type.
- Time-zone/DST conversion is a separate alarm/display concern. Health thresholds express **planned civil minutes**, not a clinical measurement of elapsed effort. The UI labels clock-change occurrences and uses real instants for the active-block indicator.

Tests cover each exact boundary, overlap/union behavior, insufficient and sufficient recovery, intentional gaps, holiday suppression, empty input, deterministic ordering, and completion semantics. No hidden model, remote service, or probabilistic score is involved.
