# Operational logic review

This follow-up checks behavior, not only syntax or file organization. It builds on the complete
19-page PDF audit and keeps the same single Activity, database, resolver and feature boundaries.

## Defects fixed

| Scenario | Previous risk | Correction |
|---|---|---|
| Android scheduling adapter after entity split | Stale import prevented compilation. | Correct `AlarmDeliveryEntity` import; no deleted validation or suppressed lint. |
| Automatic shifting disabled while timing | Fixed-boundary stopping was disabled too. | Boundary protection always runs; only downstream shifting is optional. |
| A new fixed event is added before an existing projected end | Timer stopped but the visible active block could still extend past the event. | Shorten the active projection when stopping, even if the new boundary is earlier than `lastHealedEnd`. |
| No explicit post-school rest block | Starting work could bypass required recovery. | Shared protection windows include the configured post-school interval even if it is unallocated. |
| Tap completion checkbox on an actively timed block | Dialog default could log the planned duration instead of elapsed time. | Route to the same measured finish operation as the timer banner. |
| Edit, skip, delete, or insert a break into running work | Timer could outlive a hidden/mutated occurrence. | Typed active-session conflict; finish/cancel explicitly first. Topic deletion checks active child work too. |
| Cancel measurement | Work could be dropped without the meaning being obvious. | Explicit confirmation; distinguish “finish and save” from “cancel without a completion record.” |
| Reset a moved review | Review metadata could retain the shifted date. | Every override/reset synchronizes the linked review in the same transaction. |
| Delete an unscheduled review from backlog | Orphan review metadata could remain. | Remove the linked review/one-off source consistently; other topic records remain. |
| Place a middle deliverable stage | It could be placed before scheduled prerequisites or after successors. | Resolve stage bounds through the canonical resolver and constrain slot placement. |
| Start a later stage | Checking only earlier backlog ignored unfinished scheduled prerequisites. | Check both backlog and unfinished earlier stage work. |
| Capacity-based deferral after normal repair | Stage dependencies could be broken outside the engine's normal deferral pass. | Apply one shared dependency-closure rule to both paths. |
| Work crossing midnight | Entire duration could be charged to the start date. | Apply destination-day capacity accounting to clipped minutes on each date. |
| Record work across a DST fold | Civil-time arithmetic could invent an extra hour. | Room v6 preserves measured start/end instants and recording zone; old records retain their documented fallback. Clock labels use the measured interval, while the timeline remains a civil-minute view. |

## Safety and scope

- Fixed commitments and deliberate recovery are not moved automatically.
- Fully historical work is not replayed. Unknown human activity is not inferred from a checkbox.
- Actual work may exceed planning limits; recorded facts are not falsified to fit a target.
- No background polling or foreground service is added. Explicit sessions are reconciled while
  the app is visible, on resume, or on a requested widget refresh.
- Core algorithms remain solver-free and bounded. Stage-bound reads are off-main-thread and
  use the existing resolver rather than a second recurrence implementation.
- v1→v6 migration steps remain explicit. New actual-time fields are nullable for legacy data;
  no historical timestamps are guessed.

## Verification

Local checks after these fixes: 122 core JVM tests, 15 SQLite/migration/resource checks, presentation
checks for 487 Slovenian resources, and Kotlin syntax parsing. `FullPaperIntegrationTest` adds
runtime scenarios for the issues above. These results do not substitute for Android compilation,
lint, emulator results, or physical haptic/launcher/Doze validation. Latest CI status is recorded in
`docs/VALIDATION.md` and the pull request.
