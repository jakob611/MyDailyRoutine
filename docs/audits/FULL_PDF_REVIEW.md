# Audit of the complete research PDF

Source: [`Cognitive Time-Blocking Engine Design.pdf`](../research/Cognitive%20Time-Blocking%20Engine%20Design.pdf),
19 pages, supplied by the user in commit `dc5a8a9` (`research added`). All 19 pages were extracted
and reviewed, including equations, the complete code examples and references. The PDF has native
text/math source (no embedded page images).

SHA-256: `2ef84679d04d451b8e1a6a21b9a2011698e8fc85a9442839e879407321ad73b8`

## Findings and implementation decisions

| PDF | Finding in previous implementation / source | Correction / interpretation |
|---|---|---|
| p.17 | Velocity was bounded 0.5–2.5 and could shorten a raw estimate. | Use `max(1, sum(actual)/sum(raw planned))`; no arbitrary 2.5 multiplier ceiling. The persistent block maximum remains an explicit storage bound, not a physiological bound. Exact integer ceiling retains the 60→81 example. |
| pp.12–13 | Reviews went straight to backlog when nominal duration did not fit. | Try full duration within strict jitter, then meaningful compact retrieval (minimum 10 min, or the requested duration if shorter). Zero capacity remains unscheduled/backlogged, never a fake slot. |
| p.12 | Review share used nominal configured capacity even on a much shorter available day. | Use `min(configured study capacity, scheduled study + actually free independent-study time)` and an exact integer fifth, with contiguous slot capacity. |
| p.14 vs p.12 | Example forces at least one jitter day, unlike `floor(0.15 * ISI)`. | Keep the stated 15% mathematical bound; do not expand it silently. User-requested short horizons remain supported, with honest unallocated results. |
| pp.13–15 | Example nominal 45-minute reviews exceed its own default 180×20%=36 capacity; its fallback can return an infeasible target day. | Compress to 36 or less where appropriate and validate final placement; never copy the over-cap fallback. |
| p.8 | Old unfinished history could be replayed by manual healing. | Preserve blocks ending at/before the repair frontier. A running task is explicitly tracked and excluded from downstream repair, rather than guessed from an unchecked box. |
| p.7 | Previous regeneration retained nominal start times as release constraints. | Soft work is packed from the actual restart, consistent with the complete formula. Fixed boundaries still constrain the operational window. |
| p.10 vs p.18 | Printed sample moves a hard commitment when overrun. | Reject that unsafe example behavior. School, explicit fixed commitments and deliberate recovery remain immovable. |
| p.9 | Sample mutates the deficit while calculating proportional shares and can shorten e=0 tasks in its fallback. | Retain deterministic active-set water filling and exact integer budget allocation; never divide by a zero total elasticity or compress e=0 work. |
| pp.5,18 | Legacy defaults allowed 300 min/day / 30 min school transition; recovery could be consumed as slack. | Automatic planning is bounded to 270 min and at least 45 min recovery. New warning defaults match. Existing deliberate REST_BUFFER blocks are protected; only emergency reserve is consumed. |
| p.3 | Gaussian-only placement can choose the wrong task window. | Prefer whole analytical slots 09:00–12:30 and synthesis 16:30–19:30, phase-shifted with the configured dip, then optimize Gaussian cost. Fixed availability wins. |
| p.12 | Deliverable preparation was one flat distributed list. | Generate ordered deliverable stages backwards, each with its own feeding buffer. Persist stage order, prevent within-chain swaps, expose reserve shortfall, and disallow automatic backlog placement after a linked deadline. |
| p.16 | Healing was manual-only and completed card height stayed nominal. | Explicit start/finish session; reconcile on visible minute ticks/resume/manual widget refresh; actual completion geometry and history persist. No passive monitoring, background polling, or fabricated activity. |
| p.16 + battery rule | “Real time” cannot mean an always-awake offline service. | A session survives process death via stored instants. While the app is not visible, elapsed time is reconstructed on resume; no claim of continuous background rescheduling is made. |

## What is not claimed

This PDF is a design/reference document, not proof of an individual's neurobiological state. It
contains unresolved citation markers and internally inconsistent code examples. Passing software
tests cannot validate assertions such as universal cognitive efficiency percentages or personal
adenosine clearance. S/C/W and retention equations provide theoretical context; the app does not
claim a personalized SAFTE or memory-stability measurement without the empirical inputs and
coefficients. No passive device interactions or physiological telemetry were added.

The 0.85 virtual-deadline factor, default 30/50/20 stage weights, and compact-review minimum are
explicit scheduling policies, not invented “exact” constants from the paper. Stage labels are
resource-backed templates; users still own their dates, estimates and actual work records.

## Architecture and UX audit

- Feature-first Android packages, not duplicated solver/data/UI stacks or dozens of unnecessary Gradle modules.
- Pure algorithms and repository contracts in the JVM `core` module.
- Shared Room schema, DAOs grouped by responsibility, explicit migration v5, immutable presentation contracts.
- Thin, stable Android entrypoints; app composition root and navigation/UDF coordinator separate from feature presentation/data.
- Shared color/icon/typography/haptic vocabulary; sample data receives its colors through DI instead of importing feature UI.
- Fast-add has a fixed save footer, visible input errors, category icons and a collapsed advanced section.
- Drag conversion uses the same 2 dp/min scale as block placement; fixed/recovery cards cannot accidentally drag.
- Input, scheduling, execution and settings are real persistent flows; no mock screens or new network dependencies.

## Verification

`FullPaperAuditTest` links regressions to PDF pages. `FullPaperIntegrationTest` exercises active
sessions, safe fixed-boundary stopping, restart persistence, actual geometry, compact reviews and
deadline enforcement. Existing tests are retained; default-threshold tests now assert the full
paper's 270/45 defaults. Changes to expected behavior are justified above, not hidden by deleting tests.

Current build/test outcome is recorded in `VALIDATION.md`; source inspection or syntax parsing is
not presented as proof of successful Android compilation, screenshots or physical-device behavior.
