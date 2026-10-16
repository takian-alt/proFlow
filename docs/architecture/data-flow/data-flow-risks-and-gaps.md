# Data flow — risks, divergences, and gaps

Findings from mapping the data flow in `data-flow.html`. Each one is a place where two parts of the
app answer the same question differently, or where data is written but never consumed, or where the
map could not be completed from source. Severity is about **user-visible wrongness**, not code taste.

Confidence: **hi** = shown by the cited lines · **med** = mechanism in source, effect inferred ·
**low** = the map assumes something source does not prove.

---

## A. Behavioural hazards

| # | Hazard | Severity | Conf | Mechanism and evidence | User-visible effect | Validate by |
| --- | --- | --- | --- | --- | --- | --- |
| A1 | Stranded focus session | High | hi | `FocusViewModel.kt:592-601` deliberately leaves the `time_sessions` row open ("timer resumes on next visit"). Five close paths exist (`FocusSessionManager.kt:100,121`, `FocusCompletionManager.kt:32`, `TaskRepository.kt:129,279`) and none of them is a reconciliation sweep. Since the session pin landed, `ScheduleAutoTasksWorker.kt:116,290` also treats any open session as a reason never to replan that task. | "Focusing now" stuck on a card all day; today's deep-work total short by the length of the abandoned run; that block never moves again. | Run a session, kill the process mid-run, reopen the app, then force a replan and watch whether the task is skipped. Grep for any query on `endedAt IS NULL` at startup. |
| A2 | Notes destroyed by schema upgrade | High | hi | `MIGRATION_18_19` and `MIGRATION_17_19` each `DROP TABLE notes` and recreate the *identical* schema — no copy-out (`NeuroFlowDatabase.kt:246-284`). `MIGRATION_17_18` (`:228`) only creates the table. | Every note written while the database sat at v17 or v18 is gone after the update that reaches v19. | Install a v17/v18 database with notes, upgrade to v20, count rows. Then check whether any shipped build was ever at v17/v18 (if not, this is latent rather than live). |
| A3 | "Replan now" silently dropped | Med | hi | The one-time force request and the periodic tick share one unique worker name (`ScheduleViewModel.kt:83`), and `doWork` returns early under `synchronized(workerLock)` when an instance is running (`ScheduleAutoTasksWorker.kt:89`). `TaskRepository.kt:260` also enqueues with `REPLACE` and **no `inputData`**, so a replacement loses the force flag. | The button appears to do nothing; the user's manual nudge is swallowed by a background pass. | Enqueue a periodic tick and a forced one back to back; log `getWorkInfosForUniqueWork` and whether `FORCE_REPLAN` survives. |
| A4 | Adaptive state does not survive a restore | Med | hi | The backup snapshot carries 31 of ~104 preference keys; restore is additive and cannot delete local rows; `energy_predictions` restores hit an ABORT-conflict insert wrapped in `runCatching` (`CloudBackupManager.kt:179,189`). | A restored install behaves like a new install for peak detection, sleep pressure, self-tuning, and streaks — while showing the old task list. | Back up on device A, restore on B, then diff `user_preferences` keys and `peak_*` values. |
| A5 | Backup deletes history | Med | hi | The success path ends with `firestoreRepo.pruneOldBackups(uid, keepCount = 3)` (`CloudBackupManager.kt:92`, defined at `FirestoreBackupRepository.kt:228`). | Backing up is also a remote delete; three bad backups in a row erase the recoverable past. | Read the prune call site and its ordering relative to the write; test with 4 successive backups. |
| A6 | Sleep pressure is self-supplied | Med | hi | `SleepPressureRepository.kt:169` writes `AUTO_DEFAULT` sleep rows, then the same subsystem reads sleep logs as ground truth (`:208` publishes `sleepPressurePoints`). | The fatigue model can be built on data the user never entered, and the user cannot tell which rows are theirs. | Query `sleep_logs` for `AUTO_DEFAULT` rows on a device that never logged sleep; trace whether they are distinguishable in the UI. |
| A7 | Two inert fields with live consumers | Med | hi | `goals.completedTaskCount` has no writer anywhere yet still round-trips through the cloud. `tasks.goalRiskLevel` is written only by the editor (`NewTaskFlowViewModel.kt:112`); readers are `TaskScoringEngine.kt:694,1098-1153` (a loss-aversion term worth 60–130 points) and `TaskRow.kt:154,155` (⚠/🚨 badges). `DeadlineEscalationWorker` posts a notification and mutates nothing. | A feature that reads as live signal is permanently 0, or permanently whatever the user last typed. | Grep for writers of both columns (done — none); decide per field whether to wire the writer or delete the consumer. |
| A8 | `hyperfocus_sessions` goes neither way | Low | hi | The DAO is provided and injected, but `HyperFocusManagerImpl` (`HyperFocusManager.kt:45-50`) persists state to `hyperfocus_prefs` JSON (`HyperFocusDataStore.kt:20,67`). The table's only writer is the restore loop (`CloudBackupManager.kt:191,192`), and the backup always builds `hyperFocusSessions = emptyList()` (`:85`). `getById` has no caller. | No visible effect today — which is the finding: lock-down history is invisible to backup, restore, analytics, and every screen that assumes the table matters. | Confirm with a Room query count after a Hyper-Focus session on a real device. |

## B. Divergent derivations — 14 ways the same question is answered twice

None of these are crashes. They are why the app can say two different things about the same task on
two different screens, and why changing one formula must be mirrored in another file.

| # | Question | Answer 1 | Answer 2 | Conf |
| --- | --- | --- | --- | --- |
| B1 | What is this task's score? | `TaskScoringEngine.score()` `:438` | `scoreBreakdown()` `:832` — a second full implementation of the same terms | hi |
| B2 | How good is this slot for this category? | scoring: raw points (×14 energy, +8 context, ×10 window) | planner: normalised 0..1 weight 0.18 | hi |
| B3 | How valuable is a frog? | scoring: `(120\|90×tier\|20\|50) × (0.5 + effort/100 × 0.5)` | planner: flat +0.05 when placed in PEAK | hi |
| B4 | How important is this task? | scoring: `impact × 0.55 + value × 0.45` | planner: `((imp+val)/200) × 0.7 + level × 0.3` | hi |
| B5 | Is now inside a peak window? | exclusive end | inclusive end — plus a third `endExclusive` variant | hi |
| B6 | How much did I focus in peak hours? | per-session bucketing | per-minute overlap, at two granularities | hi |
| B7 | How loaded is today? | seeded from *estimated* durations | accumulated from *planned* durations (`plannedBlockMinutes`) | hi |
| B8 | Default block length | `TaskBlockPlan.defaultMinutes` 60/45/30 by effort | planner inline fallback: 30 | hi |
| B9 | What is my energy? | Path A: evaluated at *now*, with moment signals (`EnergyScoreRepository.observeEnergy`) | Path B: projected at the slot time, blended back with a weight that hits 0 past 12 h | hi |
| B10 | Confidence in a manual peak profile | `1f` — bypasses abstention, freeze guard, and auto-tune | everything else: a 4-term weighted estimate | hi |
| B11 | Minutes spent on a task | `SUM(durationMinutes)` (live rows count 0) | `trackedMinutes()` (live rows, capped 120) and `totalTimeTrackedMinutes` | hi |
| B12 | Distraction, as a number | per-task `distractionScore` column | per-day `dailyDistractionScore`, computed live with focus-overlap netting | hi |
| B13 | When is a recurring task due? | reminder path: 09:00 fallback (`effectiveReminderTargetMillis`, `TaskTimeExtensions.kt:23`) | schedule path: raw `habitDate`, no fallback (`effectiveScheduleAnchorMillis`, `:37`) | hi |
| B14 | Protected slot width | `ProtectedSlotClock.takeoverOf` excludes `recoveryBufferMinutes` | the planner's blocked intervals include it (`AutoSchedulingEngine.kt:1258,2429`; buffer capped 0..120 at `UserPreferencesDataStore.kt:443`) while `ProtectedSlotClock.takeoverOf` (`:91`) never mentions it → up to 120 min of disagreement | hi |

**Regression rule while these stay doubled:** any change to a term in B1–B4 must be checked in both
implementations, and any change to B5/B13/B14 must be checked on both the reminder and the schedule
path. The `#rebuild` checklist in `data-flow.html` (invariants 6, 8) exists to make the doubling
unnecessary rather than merely documented.

## C. Unknowns — validate before trusting the map further

| # | Unknown | Conf | Why it matters | Validation action |
| --- | --- | --- | --- | --- |
| C1 | Runtime behaviour | low | Everything here is static source reading — no traces, no device, no emulator. Whether the foreground tick survives Doze, and how often the single-instance guard actually fires, is unmeasured. | Instrument one day of `WorkManager` getWorkInfo logs on a real device; count skipped vs executed ticks. |
| C2 | Orphan navigation | med | `Screen.Matrix/History/Analytics/WeeklySchedule` are defined (`NeuroFlowApp.kt:79,81,99`) and registered, but no file outside that one references them. `com.procus.ACTION_OPEN_ANALYTICS` is emitted by `HomeScreenPages.kt:709` and `LauncherActivity.kt:308`, while `MainActivity.kt:74-88` handles only NEW_TASK / FOCUS / GUIDE / SCHEDULE. Either an external shortcut exists or those screens are unreachable. | Search the manifest intent filters and any launcher shortcut provider; then check on device whether a pinned shortcut points at them. |
| C3 | Field counts | med | "~104 preference keys" and "31 backed up" were read off `UserPreferencesDataStore.kt:289,477,601` and the backup codec. A rebuild should recount rather than reuse. | Generate the key list programmatically and diff against the backup codec. |
| C4 | Which divergences are intentional | low | Several double implementations may be deliberate (a presentation-grade formula kept simple on purpose). Nothing in the repo documents intent — there is no ADR or `docs/` precedent — so intent is unknown, not absent. | Ask the owner per row of table B; record the answers as short ADRs next to this folder so the next map does not have to guess. |
| C5 | Distraction overlap semantics | med | Flagged app time that falls inside a focus block is netted out of deep work. This is my unilateral reading of `dailyScreenUsage`, not a ratified product decision. | Confirm with the owner; if the semantics are wrong, B12 becomes a bug rather than a divergence. |

## D. Suggested remediation order

Sequenced by (user-visible harm) ÷ (change surface). Not a plan — a ranking of what the map says is
worth deciding first.

1. **A1 stranded sessions** — one reconciliation query at startup closes or marks abandoned rows. It
   also un-pins the placements A1 currently freezes.
2. **A3 forced replan dropped** — give the one-time worker its own name or pass `inputData` through
   the `REPLACE`; the user's explicit action should not be swallowed.
3. **A2 notes migration** — if any shipped build reached v17/v18, this needs a data recovery story;
   if not, mark the migrations as known-destructive so nobody reuses the pattern (invariant 12).
4. **A7 / A8 inert fields and dead table** — decide wire-or-delete per field; each deletion removes a
   whole class of "the UI implies something the data never says".
5. **B1–B4 formula pairs** — collapse only after a test pins both outputs for the same input
   (kotest `StringSpec`; JUnit4 `@Test` classes in this repo compile but never execute).
6. **A4/A5 backup symmetry** — widen the snapshot or state the limit in the UI; change the prune
   policy from implicit to explicit.

---

Generated 2026-10-11 from `app/src/main/java/com/neuroflow/app/`. Companion files:
[`data-flow.html`](data-flow.html) (the visual map), [`data-flow.md`](data-flow.md) (index),
[`data-flow-evidence.md`](data-flow-evidence.md) (citation index, including the list of claims the
first pass got wrong).
