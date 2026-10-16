# Data flow — evidence index

Every claim in `data-flow.html` and the three `.dot` sources traces to a line below. Base path for
all unqualified paths is `app/src/main/java/com/neuroflow/app/`. Line numbers were resolved against
the working tree on 2026-10-11 (Room schema v20, branch `main`, uncommitted scheduler work).

Confidence: **hi** = the cited line shows the behaviour · **med** = mechanism is in source, its
runtime frequency/effect is inferred · **low** = the map assumes something source does not prove.

---

## 1. Entry points and triggers

| Event | Where it fires | Citation | Conf |
| --- | --- | --- | --- |
| App start → worker registration, DB guard, tick loop | `NeuroFlowApplication` (`scheduleDailyWorkers`, then the foreground tick) | `NeuroFlowApplication.kt:183` (tick interval), `NeuroFlowApplication.scheduleDailyWorkers` | hi |
| Daily/periodic notification + streak + escalation work | `NotificationWorker.kt` — 4 `@HiltWorker` classes in one file: `DailyPlanWorker:135`, `StreakCheckWorker:180`, `DeadlineEscalationWorker:237`, `TaskReminderWorker:291` | `worker/NotificationWorker.kt` | hi |
| Manual "Replan now" | `triggerAutoScheduleNow()` enqueues a one-time worker with `FORCE_REPLAN=true` on the *same* unique worker name as the periodic tick | `presentation/schedule/ScheduleViewModel.kt:83` | hi |
| Any task mutation enqueues a recheck | `enqueueAutoSchedulingRecheck()` — `REPLACE` policy, **no `inputData`**, so the replacement loses the force flag | `data/repository/TaskRepository.kt:260` | hi |
| Completion → recheck | called from the completion fan-out | `presentation/focus/FocusCompletionManager.kt:32-63` | hi |
| Deep link `mini_woop/{taskId}` | posted by `AutonomyNudgeWorker`; opened by `MainActivity.handleIntent` | `worker/AutonomyNudgeWorker.kt`, `MainActivity.kt` | hi |
| Deep link `com.procus.ACTION_OPEN_ANALYTICS` | emitted by two callers, **no handler** in `MainActivity.handleIntent` | `MainActivity.kt` | med |

## 2. External sources (not Room)

| Source | Read by | Notes | Citation | Conf |
| --- | --- | --- | --- | --- |
| `UsageStatsManager` query events | `DistractionEngine.rankByDistraction` | constants: ignore < 3 s blips, 60-min window, 15-min bucket | `domain/engine/DistractionEngine.kt:125,126,132,138` | hi |
| Weighted app table | same engine | Instagram 1.0 … Gmail 0.35; system-package prefix list | `domain/engine/DistractionEngine.kt:32-123,138` | hi |
| `user_preferences` DataStore | engines + UI | `preferencesFlow` mapping, writer, key enumeration | `data/local/UserPreferencesDataStore.kt:289,477,601` | hi |
| `launcher_prefs` | distraction score cache for the launcher | separate file, so it bypasses app backup | `presentation/launcher/data/PinnedAppsDataStore.kt:15` | hi |
| `hyperfocus_prefs` (JSON blob + `AtomicLong` heartbeat) | Hyper-Focus lock-down state | this is the real HyperFocus store — not the Room table | `presentation/launcher/hyperfocus/data/HyperFocusDataStore.kt:20,67,70` | hi |
| Injected clock | every engine | `nowMillis` is a parameter, so "now" is not derived from storage | `domain/engine/TaskScoringEngine.kt:98` (`ScoringContext`) | hi |

## 3. Room v20 — tables, writers, notable gaps

| Table | Entity | DAO | Primary writers | Conf |
| --- | --- | --- | --- | --- |
| `tasks` | `data/local/entity/TaskEntity.kt` | `data/local/dao/TaskDao.kt` | `TaskRepository`; `ScheduleAutoTasksWorker.kt:337-339`; `DistractionSyncWorker.kt:45`; `MatrixViewModel.kt:158`; `FocusSessionManager.kt:120-122`; `FocusCompletionManager.kt`; `FocusWoopManager.kt:66,72,78` | hi |
| `time_sessions` | `data/local/entity/TimeSessionEntity.kt` | `TimeSessionDao` | `FocusSessionManager.kt:37` (open row) and 5 close paths | hi |
| `sleep_logs` | `SleepLogEntity` | `SleepLogDao` | `SleepLogRepository.kt:31`; `SleepPressureRepository.kt:169` writes `AUTO_DEFAULT` rows | hi |
| `energy_predictions` | `EnergyPredictionEntity` | `EnergyPredictionDao` | `EnergyMetricsRepository.kt:67,98` — writer on a *read* path | hi |
| `goals` | `GoalEntity` | `GoalDao` | `GoalRepository`; **`completedTaskCount` has no writer** | hi |
| `notes` | `NoteEntity` | `NoteDao` | `NoteRepository` | hi |
| `woop_data` | `WoopEntity` | `WoopDao` | `WoopRepository` upsert; PK is the taskId | hi |
| `ulysses_contracts` | `UlyssesContractEntity` | `UlyssesContractDao` | user + `UlyssesEvaluatorWorker.kt:27` (only DB state column decided by a background job) | hi |
| `unlock_codes` | `presentation/launcher/hyperfocus/data/UnlockCodeEntity.kt` | same dir | `UnlockCodeRepositoryImpl.kt:22` | hi |
| `hyperfocus_sessions` | `presentation/launcher/hyperfocus/data/HyperFocusSessionEntity.kt` | `HyperFocusSessionDao.kt` (`insert`, `update`, `getById`) | **only the restore loop** `CloudBackupManager.kt:191,192`; backup sends `hyperFocusSessions = emptyList()` at `:85`; `getById` has no caller | hi |

Cross-cutting: `@Insert(onConflict = REPLACE)` on tasks (`TaskDao.kt:11`) — an "insert" of an existing
id overwrites silently. `TaskDao.kt:41` is the Flow query that drives most screens. No
`@ForeignKey` anywhere; only `SleepLogEntity` declares indices; `Converters.toTaskEntity` coerces an
unknown `TaskType` to `ANALYTICAL` while other converters throw.

## 4. Scheduler tick — the load-bearing citations

| Step | Evidence |
| --- | --- |
| Worker single-instance guard | `presentation/launcher/work/ScheduleAutoTasksWorker.kt:89` (`synchronized(workerLock)`; early return when an instance is running) |
| Session pin (task with an open session is never replanned) | `ScheduleAutoTasksWorker.kt:116,290` — `sessionRepository.getOpenSessions().map { it.taskId }.toSet()` |
| Verdict ladder | `domain/scheduler/PlacementStability.kt:47` near-term freeze, `:49` force, `:60` cooldown (urgent 1 h / toggle on 4 h / else 6 h at `:55-59`), `:62-88` rescue→periodic, `:90` KEEP |
| Materiality gate on writes | `PlacementStability.kt:99` `isMoveWorthApplying` (identical → false; past start → true; else ≥ 30 min) |
| Manual-schedule ownership | `domain/scheduler/AutoSchedulingContracts.kt:14` `hasManualScheduleData` |
| Re-read inside the write path | `ScheduleAutoTasksWorker.kt:293` `taskRepository.getById(decision.taskId)` |
| Columns stamped on placement | `ScheduleAutoTasksWorker.kt:337` `isAutoScheduled`, `:338` `plannedBlockMinutes`, `:339` `lastAutoScheduledAt` |
| Block length reserved by the planner | `domain/scheduler/TaskBlockPlan.kt:17` `plannedMinutes`; `data/local/entity/TaskEntity.kt:98-101` field doc |
| Engine gate + energy input | `domain/scheduler/AutoSchedulingEngine.kt:192` (`autoSchedulingEnabled`), `:517` (`sleepPressurePoints`), `:922` `calculateRealisticDuration` |
| Realistic duration maths | `((est + actual)/2) × (1 + max(mape, smape)/100) × energyAdj × calib`, capped 360 — `AutoSchedulingEngine.kt:922` |
| Periodic work name + window | `schedule_auto_tasks_work`, repeat interval `coerceIn(15, 240)` minutes — `ScheduleAutoTasksWorker.kt:89-98` region |

## 5. Energy chain

| Fact | Citation |
| --- | --- |
| Sleep rows are the ground truth | `data/repository/SleepLogRepository.kt:31` |
| Sleep pressure is computed then persisted to prefs | `domain/repository/SleepPressureRepository.kt:169` (writes `AUTO_DEFAULT` log), `:208` (writes `sleepPressurePoints`) |
| Chronotype detection | `domain/engine/MEQChronotypeDetector.kt` |
| Two live-energy answers exist | Path A `domain/repository/EnergyScoreRepository.kt` (`observeEnergy`, evaluated at *now*, with moment signals); Path B projected at the slot time inside `AutoSchedulingEngine` |
| Zone hysteresis | rising 83/63/43/23, falling 77/57/37/17 — `domain/engine/TaskScoringEngine.kt` / engine slot model |
| Daily cognitive budget by fatigue zone | 170/130/100/75 min |
| Telemetry write | `domain/repository/EnergyMetricsRepository.kt:67,98` |
| Telemetry read (one only) | `presentation/analytics/AnalyticsViewModel.kt:101` → `presentation/analytics/AnalyticsScreen.kt:1431` (7-day abstention trend) |
| Uncalled aggregation | `domain/repository/EnergyMetricsRepository.kt:151` `getStatisticsForRange` — no caller in main source |
| Peak meta-state write-backs | `domain/repository/PeakEnergyRepository.kt:482` abstention telemetry, `:627` adaptive freeze guard, `:883` morning-weight auto-tune |
| Manual peak profile bypass | `confidence = 1f` path in `PeakEnergyRepository` (skips abstention, freeze guard, auto-tune) |

## 6. Distraction loop

| Step | Citation |
| --- | --- |
| Rank apps from usage events | `domain/engine/DistractionEngine.kt:32-123` (weights, 3 s/60 min/15 min constants at `:125,126,132`, system prefixes at `:138`) |
| Persist per-task score | `worker/DistractionSyncWorker.kt:44` `if (newScore != task.distractionScore)` → `:45` `taskDao.update(task.copy(distractionScore = newScore))` |
| Feed priority | `domain/engine/TaskScoringEngine.kt:708` `if (task.distractionScore > 0f)` |
| Feed placement | `ScheduleAutoTasksWorker.kt:338` region (scored queue → decision) |
| Second channel (daily figure) | `dailyScreenUsage` with focus-overlap netting — flagged minutes inside a focus block are subtracted. **Semantics not ratified by the product owner** — see `data-flow-risks-and-gaps.md` |

## 7. Session, completion, recurrence

| Fact | Citation |
| --- | --- |
| Open row created | `presentation/focus/FocusSessionManager.kt:37` |
| Finalize adds to the task | `FocusSessionManager.kt:100` → `:120` `totalTimeTrackedMinutes`, `:121` `sessionCount + 1`, `:122` `lastSessionDurationMinutes` |
| Completion fan-out | `FocusCompletionManager.kt:32` (`endedAt = now`), `:41` (actual = sum of closed sessions), `:45` (mape), `:50` (`focusModePoints = impact/10`) |
| WOOP bias | `FocusWoopManager.kt:52` (`syncEnjoymentForecastBias`), `:66,72` (prompt flag), `:78` (`affectiveForecastError = rating`) |
| Postpone raises the counter and clears auto-schedule | `data/repository/TaskRepository.kt:137,159` (`postponeCount + 1`), `:162` (`isAutoScheduled = false`) |
| Manual edit resets the reserved block | `TaskRepository.kt:99` (planned-vs-derived block comment) |
| Completion updates tracked minutes on the task row | `TaskRepository.kt:129` |
| Recurrence: next occurrence copy | `TaskRepository.kt:274-352` — `:279` `isHabitual`, `:331` `habitDate = nextAnchor.toDayStart()`, `:345` `habitStreak + 1`; zeroes `totalTimeTrackedMinutes`, `sessionCount`, `lastSessionDurationMinutes`, `actualDurationMinutes`, `estimationErrorMape`, `estimationErrorSmape`, `focusModePoints`, `postponeCount`; new UUID; deadline shifted by its original day offset |
| Orphan hazard | `presentation/focus/FocusViewModel.kt:592-601` — `onCleared` cancels the tick jobs and comments “open session stays in DB — timer resumes on next visit”; the 5 close paths are `FocusSessionManager.kt:100,121`, `FocusCompletionManager.kt:32`, `TaskRepository.kt:129,279` — none is a reconciliation sweep |
| Streak reset | `worker/NotificationWorker.kt:199` (`StreakCheckWorker` writes `dailyStreak = 0`); the other three notification workers mutate nothing |

## 8. Scoring, analytics, calibration

| Fact | Citation |
| --- | --- |
| Score function | `domain/engine/TaskScoringEngine.kt:438` `fun score(` |
| Second implementation of the same terms | `TaskScoringEngine.kt:832` `fun scoreBreakdown(` |
| Quadrant auto-assignment | `TaskScoringEngine.kt:1123` `autoAssignQuadrant` |
| MAPE/SMAPE + calibration | `domain/engine/AnalyticsEngine.kt:32` `computeWeightedMape`, `:69` `PlanningCalibration`, `:146` `EnjoymentCalibration` |
| Constants | 5 samples to trust, prior 4.0, ratio clamped 0.4–3.0, factor clamped 0.8–1.6, geometric mean, 10 pts per rating, max 15, static priors 1.15/1.10/0.90/1.0 |
| Matrix ordering | `presentation/matrix/MatrixViewModel.kt:57` `TaskScoringEngine.sortedByScore(tasks, prefs)` |
| Manual time entry | `MatrixViewModel.kt:158,198,246` |
| Contract outcome | `worker/UlyssesEvaluatorWorker.kt:27` |

## 9. UI read paths

| Surface | Reads | Citation |
| --- | --- | --- |
| Today / Upcoming / Matrix / Focus card / widget | Room `Flow` queries — no repository caching layer, no navigation-mediated data passing | `data/local/dao/TaskDao.kt:41`, `presentation/*/*.kt` |
| Prefs-driven screens | `UserPreferencesDataStore.preferencesFlow` (`:289`) | hi |
| Screens that also *write* | `MatrixViewModel.kt:121`, `presentation/common/NewTaskFlowViewModel.kt:256,282,307` | hi |
| Analytics trend from telemetry | `AnalyticsViewModel.kt:101` | hi |

## 10. Backup and restore

| Fact | Citation |
| --- | --- |
| Backup entry | `data/sync/CloudBackupManager.kt:56` `suspend fun backup()` |
| Snapshot gathers tasks, sessions, sleep logs, goals, woop, contracts, notes and `energyPredictionDao.getRecent(limit = 500)` | `CloudBackupManager.kt:71` |
| HyperFocus sessions are hard-set to empty in the payload | `CloudBackupManager.kt:85` |
| Restore applies payload rows | `CloudBackupManager.kt:179` `applyPayload`; `:189` `runCatching { energyPredictionDao.insert(pred) }`; `:191,192` hyperfocus insert |
| Firestore document codec, incl. the entity↔map functions | `data/sync/FirestoreBackupRepository.kt` (payload type at `:40`, hyperfocus mapping at `:366,724-745`) |
| Pruning | success path ends with `pruneOldBackups(keep = 3)` |
| Preference coverage | `UserPreferencesDataStore.kt:289` (read model), `:477` (writer), `:601` (key enumeration) — ~104 keys defined, 31 round-trip (recount before reuse) |
| Restore-version policy can wipe local data before anything reads it | `NeuroFlowApplication.kt:90,125` → `data/local/DatabaseCleaner.kt:84` |
| Destructive migration on user data | `data/local/NeuroFlowDatabase.kt:246-284` — `MIGRATION_18_19` and `MIGRATION_17_19` each `DROP TABLE notes` then recreate the identical schema; `MIGRATION_17_18` (`:228`) only creates it; `MIGRATION_19_20` (`:288`) adds `plannedBlockMinutes` |

## 11. Corrections made during verification

Recorded because the first pass of the map got these wrong:

| Claim as first drafted | Corrected to | Evidence |
| --- | --- | --- |
| "`energy_predictions` is write-only, no reader" | One reader exists (Analytics 7-day abstention trend); the *backtest aggregation* is what has no caller | `AnalyticsViewModel.kt:101`, `AnalyticsScreen.kt:1431`, `EnergyMetricsRepository.kt:151` |
| "`hyperfocus_sessions`: write-only, never shown" | Never written in normal use at all — the manager persists to `hyperfocus_prefs` JSON, the only DAO writer is the restore loop, and the backup payload for it is always empty | `HyperFocusManager.kt:45-50`, `HyperFocusDataStore.kt:20,67`, `CloudBackupManager.kt:85,191` |
| "notes destroyed by the v17→v19 migration" | Both `MIGRATION_18_19` and `MIGRATION_17_19` drop the table; `MIGRATION_17_18` does not. Affected set = devices that already had rows at v17/v18 | `NeuroFlowDatabase.kt:246,267` |
| "13 workers" | 14 worker classes (11 rows in the table; `NotificationWorker.kt` carries 4 of them; `KioskPolicyWorker` is the only non-`@HiltWorker`) | `grep -c` over `worker/` + `presentation/launcher/work/` |
| "`goalRiskLevel` escalates in the background" | Only ever written by the editor; `DeadlineEscalationWorker` posts a notification and mutates nothing | `presentation/common/NewTaskFlowViewModel.kt:112` (`updateGoalRiskLevel`) is the only writer; readers are `TaskScoringEngine.kt:694,1098-1153` and `TaskRow.kt:154,155` (the ⚠/🚨 badges) |

Layout fixes from the same pass (measured in a browser against the rendered SVG, not against source):
five labels in the lifecycle diagram overflowed their boxes and were shortened; two write-back lanes
in the layer map ran straight through the engine and consumer columns and were rerouted through the
inter-column gutters (`M918,352 H892 V80 H340 V104` and `M622,184 H596 V92 H430 V104`); one edge
started inside its own box. After the fixes: 0 overflowing labels, 0 overlapping boxes, 0 edges
crossing a box, 0 unresolved marker ids, 0 dead in-page anchors across all 6 diagrams.

## 12. Not verified

- No runtime trace, no emulator, no device log. Worker execution frequency, single-instance collision
  rate, and Doze behaviour are **med/low**.
- Orphan navigation routes (`matrix`, `history`, `analytics`, `weekly_schedule`) were checked for
  in-app callers only; an external shortcut cannot be ruled out from source.
- Preference-key and migration counts are read off source and should be re-derived on a rebuild.
