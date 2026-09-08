# Full-PDF and operational verification

## Verified code

**Commit `143ed6b987189bb280bba8fd92e39b06f88970f8` passed both jobs in
[GitHub Actions run 34157205599](https://github.com/jakob611/MyDailyRoutine/actions/runs/34157205599).**

| Check | Result |
|---|---|
| `:core:test` | 122 JVM tests passed |
| `:app:testDebugUnitTest` | 12 JVM tests passed |
| `:app:assembleDebug` | Debug APK built |
| `:app:lintDebug` | Passed |
| `:app:connectedDebugAndroidTest` | 36 tests passed on API 35 |
| SQLite/structure/migration checks | 15 passed |
| Presentation/architecture checks | 487 Slovenian resources; bundled font; one domain model; feature-boundary checks |

The declared Kotlin 2.2.10 / JDK 17 / API 36 build stack was used in CI. Local standalone
Kotlin checks also passed, but are not misrepresented as an Android build. Direct sandbox
Gradle/SDK downloads remain restricted; full Android verification was remote.

[Test APK artifact](https://github.com/jakob611/MyDailyRoutine/actions/runs/34157205599/artifacts/10031493805).
[Device test reports](https://github.com/jakob611/MyDailyRoutine/actions/runs/34157205599/artifacts/10031494633).

## Research review

All 19 pages of the exact user-provided PDF were reviewed. The file is retained under
`docs/research/`; the content hash, page references, discrepancies and interpretation choices are
in `docs/audits/FULL_PDF_REVIEW.md`. The PDF's unsafe example that moves fixed commitments was
not copied. Biomedical accuracy is not proven by application tests and is not claimed.

## Operational regression coverage

`docs/audits/LOGIC_REVIEW.md` describes the defects found and corrected. Tests cover:

- max(1, ν) calibration, exact rounding, protected recovery, real capacity and compact reviews;
- scope-safe delay handling, no replay of history, stage closure after deferral, preserved backlog;
- explicit start/finish, process recreation, measured completion instead of a nominal timer default;
- fixed stopping with automatic shifting disabled and after adding an earlier fixed boundary;
- blocking mutation/deletion of running work until an explicit finish/cancel;
- synchronized review metadata after move/reset and cleanup of deleted review backlog entries;
- stage scheduling bounds and unfinished predecessor checks;
- moving a goal earlier without leaving unfinished generated preparation after its deadline;
- true measured instants through the Ljubljana autumn DST fold;
- explicit schema migrations through v6, FK/cascade integrity, locale and privacy permissions;
- four-view navigation, advanced Settings, warm widget navigation, visible quick-add save footer.

UI tests also execute native `captureToImage` snapshots for the day/week/month/year views and
quick-add. The CI script attempts to collect them with device artifacts. This client could not
download the artifact ZIP from Azure's delivery host (EOF), so no local pixel-by-pixel review or
claim that screenshots were visually inspected here is made. The executable UI assertions passed.

## Reproduce

```bash
python3 tools/check_sqlite_integrity.py
python3 tools/check_presentation.py
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

`tools/test-core.sh` is available for a local Kotlin compiler plus JUnit jars. CI publishes compiler,
lint and test diagnostics through `tools/report_ci_failures.py`; no identity hashes are fabricated.

## Remaining physical / release checks

- Physical haptic feel, amplitude support and app/system opt-out on actual hardware.
- OEM Doze, exact-alarm access, boot/unlock and force-stop behavior on multiple manufacturers.
- Glance resizing/rendering across launchers and older API-24 devices; its progress is a timestamped
  snapshot, not a continuously polling service.
- Manual wall-clock/time-zone changes during long running sessions, plus long-duration usability.
- Release R8/signing and low-end performance before calling debug artifacts a production release.
- Personal physiological/retention effectiveness: the app uses explicit planning policies, not a
  clinical SAFTE or measured memory-stability model.
