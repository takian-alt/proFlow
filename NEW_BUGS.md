# PROCUS Codebase Comprehensive Audit — NEW BUGS & REMEDIATION LOG

**Date:** 2026-11-04  
**Auditor:** Antigravity AI  
**Target:** Deep file-by-file audit of all UI, Logic, Database, Background Workers, and Cloud Sync in `/home/takian/Project/PROCUS`

---

## Summary of Remediated & Discovered Bugs

| ID | Category | Status | File | Impact / Root Cause |
|---|---|---|---|---|
| **BUG-UI-01** | UI | **FIXED** | `MindmapCanvasScreen.kt` | Pointer drag deltas added raw without `1f / zoomScale` inverse scaling. |
| **BUG-UI-02** | UI | **FIXED** | `NeuroFlowAppV2.kt` | Bottom navigation bar leaked on full-screen routes (`mindmap`, `identity`, `analytics`). |
| **BUG-UI-03** | UI | **FIXED** | `FlowScreenV2.kt` | Dynamic Regex re-compiled every minute inside Composable recomposition loop. |
| **BUG-UI-04** | UI | **FIXED** | `WeeklyScheduleScreen.kt` | Overlapping calendar cards occluded each other; fixed with greedy sub-column lane layout. |
| **BUG-LOGIC-01** | Security / Domain | **FIXED** | `GeminiAiOnlineService.kt` | API key was passed in URL query param; migrated to `x-goog-api-key` HTTP header. |
| **BUG-LOGIC-02** | Network / Domain | **FIXED** | `GeminiAiOnlineService.kt` | 100-second fallback cascade reduced to 6s connect / 8s read timeouts. |
| **BUG-LOGIC-03** | Domain | **FIXED** | `TaskInferenceEngine.kt` | Sub-minute actual task durations rounded to 0 minutes, corrupting velocity calibration. |
| **BUG-WORKER-01** | Worker / Domain | **FIXED** | `ScheduleAutoTasksWorker.kt` | Hardcoded 2-part limit (#part1, #part2) dropping remaining duration on >100m tasks; generalized to arbitrary N parts. |
| **BUG-DATA-01** | Database | **FIXED** | `TimeSessionEntity.kt` & `NeuroFlowDatabase.kt` | Missing indices on `time_sessions(taskId)` and `time_sessions(startedAt)` causing table scans; added Migration 23->24. |
| **BUG-DATA-02** | Database | **FIXED** | `TaskEntity.kt` & `NeuroFlowDatabase.kt` | Missing composite indices on `tasks(goalId)` and `tasks(status, quadrant)`; added Migration 23->24. |
| **BUG-DATA-03** | Relational / DB | **FIXED** | `TaskRepository.kt` | Deleting tasks left orphaned mindmap node positions and edge wires; added cascade deletion. |
| **BUG-DATA-04** | Relational / DB | **FIXED** | `TaskDao.kt` & `GoalRepository.kt` | Deleting goals left dangling `goalId` foreign keys on active tasks; added `clearGoalId` trigger. |
| **BUG-SYNC-01** | Cloud Sync | **FIXED** | `CloudBackupManager.kt` & `FirestoreBackupRepository.kt` & `SyncModule.kt` | Mindmap entities completely excluded from cloud backup payloads; added upload/download sync. |
| **BUG-ARCH-01** | Architecture | **FIXED** | `PlanViewModelV2.kt` | Pending 3-second undo task deletions cancelled on screen exit; added IO flush on `onCleared()`. |
| **BUG-NOTES-01** | UI / Architecture | **FIXED** | `NotesViewModel.kt` | Selecting notes spawned concurrent un-cancelled `Flow.collect` loops leaking memory. |
| **BUG-NOTES-02** | UI / Performance | **FIXED** | `NotesViewModel.kt` | Keystroke text changes triggered immediate un-debounced Room SQLite writes; added 400ms debounce. |
| **BUG-WORKER-03** | Worker / Sync | **FIXED** | `AutonomyNudgeWorker.kt` & `TaskRepository.kt` | Delayed Autonomy Nudges were not cancelled on task completion or replaced on reschedule; added unique work enqueueing & cancellation on completion. |
| **BUG-HF-01** | Performance / Memory | **FIXED** | `feature/hyperfocus/service/AppBlockingService.kt` | `getWindowTextSnapshot` traversed up to 120 `AccessibilityNodeInfo` objects per check without recycling, exhausting IPC binder memory on older Android versions; added recycle lifecycle. |
| **BUG-ARCH-02** | Dead Code / Architecture | **FIXED** | `com/neuroflow/app/service/AppBlockingService.kt` | Zombie legacy `AppBlockingService` file not registered in manifest caused confusion with active `feature.hyperfocus.service.AppBlockingService`; removed. |

---

## Detailed Remediation & Defect Catalog

### BUG-UI-01: Mindmap Canvas Drag Math Scale Inversion [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/v2/mindmap/MindmapCanvasScreen.kt`
* **Description:** At lines 544 and 706, pointer drag deltas were added raw to node coordinates: `localOffset += dragAmount`. When zoomed in or out (`zoomScale != 1f`), nodes moved disproportionately fast or slow relative to finger motion.
* **Remediation:** Passed `zoomScale` into `GoalHubNodeView` and `TaskNodeCardView` and scaled pointer delta by `dragAmount / effectiveScale`.

### BUG-UI-02: Bottom Navigation Bar Leaks on Non-Hub Routes [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`
* **Description:** `showBottomBar` only excluded focus routes. Secondary full-screen views like `MindmapCanvasScreen`, `IdentityScreen`, and `AnalyticsScreen` displayed the 3-tab hub bar, obscuring bottom canvas controls.
* **Remediation:** Replaced route prefix exclusions with `NavHubsV2.isHubRoute(currentRoute)`.

### BUG-UI-03: FlowScreen V2 Recomposition Regex Overhead [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`
* **Description:** Regex patterns (`Regex("""[#_]part(\d+)""")`, `Regex("""\(Part (\d+)/""")`, `Regex("""[#_]part\d+.*$""")`) were constructed dynamically on every recomposition tick.
* **Remediation:** Extracted precompiled regex constants to private top-level singletons (`PART_NUM_ID_REGEX`, `PART_NUM_TITLE_REGEX`, `PARENT_SLUG_REGEX`, `PART_PREFIX_REGEX`).

### BUG-UI-04: Weekly Schedule Card Overlap / Collision [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt`
* **Description:** Overlapping task items on the weekly calendar rendered with full width at identical coordinates, completely obscuring earlier tasks.
* **Remediation:** Implemented `layoutOverlappingTasks` clustering and greedy sub-column allocation (`colIndex`, `colCount`, `xOffset = taskWidth * colIndex`).

### BUG-LOGIC-01: Gemini API Key Exposure in URL Query Parameters [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`
* **Description:** API key was sent in the query parameters (`?key=...`), exposing it to loggers and proxies.
* **Remediation:** Migrated the API key to the request header `x-goog-api-key: $apiKey`.

### BUG-LOGIC-02: Gemini Fallback Cascade 100-Second Latency [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`
* **Description:** 4 fallback models with 10s connect / 15s read created a worst-case 100s blocking freeze.
* **Remediation:** Reduced timeouts to 6s connect and 8s read.

### BUG-LOGIC-03: Duration Rounding Edge Case in Task Inference Engine [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`
* **Description:** Tasks with duration < 30 seconds rounded to 0 minutes, corrupting historical velocity calibrations.
* **Remediation:** Coerced rounded actual duration to at least 1 minute (`roundToInt().coerceAtLeast(1)`).

### BUG-WORKER-01: Hardcoded 2-Part Cap in `ScheduleAutoTasksWorker` [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`
* **Description:** Splitting assumed only `#part1` and `#part2`, silently ignoring parts 3+ for long tasks (>100 min).
* **Remediation:** Replaced hardcoded pair logic with arbitrary N-part scheduling sorted by date/time, properly allocating `Part i/N`.

### BUG-DATA-01 & BUG-DATA-02: Missing Room Database Indices [FIXED]
* **Files:** `TimeSessionEntity.kt`, `TaskEntity.kt`, `NeuroFlowDatabase.kt`, `AppModule.kt`
* **Description:** Lack of indices on `time_sessions(taskId, startedAt)` and `tasks(goalId, status+quadrant)` triggered full table scans.
* **Remediation:** Created `MIGRATION_23_24`, bumped Room database version to 24, registered migration in `AppModule.kt`.

### BUG-DATA-03: Orphaned Mindmap Nodes on Task Deletion [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`
* **Description:** Deleting a task left orphaned records in `mindmap_node_positions` and `mindmap_edges`.
* **Remediation:** Injected `MindmapDao` into `TaskRepository` and cascaded deletions.

### BUG-DATA-04: Dangling Foreign Key in `tasks.goalId` Upon Goal Deletion [FIXED]
* **Files:** `TaskDao.kt`, `GoalRepository.kt`
* **Description:** Deleting a goal left active tasks pointing to the deleted `goalId`.
* **Remediation:** Added `clearGoalId(goalId)` query in `TaskDao` and cascaded it in `GoalRepository.delete()`.

### BUG-SYNC-01: Mindmap Entities Excluded from Cloud Backup [FIXED]
* **Files:** `CloudBackupManager.kt`, `FirestoreBackupRepository.kt`, `MindmapDao.kt`, `SyncModule.kt`
* **Description:** Mindmap positions and edge wires were omitted from cloud sync payloads.
* **Remediation:** Added `mindmapPositions` and `mindmapEdges` to `BackupPayload`, implemented upload and restore logic, and injected `MindmapDao` into `SyncModule`.

### BUG-ARCH-01: In-Flight Task Deletion Cancellation in `PlanViewModelV2` [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
* **Description:** Leaving the Plan tab within 3 seconds of deleting a task cancelled the coroutine, resurrecting the deleted task.
* **Remediation:** Added `onCleared()` flush to commit pending deletions to `taskRepository` asynchronously.

### BUG-NOTES-01: Leaking Flow Collection on Note Selection [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt`
* **Description:** Multiple calls to `selectNote()` spawned independent unending collection coroutines.
* **Remediation:** Tracked `selectNoteJob` and cancelled previous collectors before starting a new one.

### BUG-NOTES-02: Undebounced Keystroke Database Writes [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt`
* **Description:** Every keystroke triggered immediate Room SQLite disk writes, saturating IO.
* **Remediation:** Implemented 400ms debounce via `scheduleSave()` with an explicit `onCleared()` flush.

### BUG-WORKER-03: Stale Autonomy Nudge Notifications After Reschedule or Completion [FIXED]
* **Files:** `app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt` & `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`
* **Description:** When tasks were rescheduled or completed, pending delayed autonomy nudge work remained queued in WorkManager, delivering obsolete notifications.
* **Remediation:** Switched enqueueing to `enqueueUniqueWork(..., ExistingWorkPolicy.REPLACE)` and added `AutonomyNudgeEngine.cancelNudge(workManager, taskId)` to `TaskRepository.completeTask()`.

### BUG-HF-01: AccessibilityNodeInfo IPC Leak in Window Inspection [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/AppBlockingService.kt`
* **Description:** During anti-tamper window inspection, `getWindowTextSnapshot` traversed up to 120 `AccessibilityNodeInfo` nodes every 2 seconds without calling `recycle()`. On Android < 14, un-recycled nodes consume IPC binder node cache entries, eventually throwing `IllegalStateException` or crashing the system accessibility service.
* **Remediation:** Added `node.recycle()` invocation guarded on `< Android 14 (UPSIDE_DOWN_CAKE)`.

### BUG-ARCH-02: Zombie Duplicate Service File [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/service/AppBlockingService.kt`
* **Description:** Legacy unmaintained service duplicate existed in `app/service/` outside the active architecture in `feature/hyperfocus/service/`, leading to conflicting overlay implementations and stale imports.
* **Remediation:** Deleted the orphaned file and directory.

### BUG-NOTIF-01: Unchecked Notification Permission in UlyssesEvaluatorWorker [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/worker/UlyssesEvaluatorWorker.kt`
* **Description:** Direct call to `manager.notify()` without verifying `POST_NOTIFICATIONS` permission on Android 13+ (API 33+) would throw a runtime SecurityException when notifications were denied.
* **Remediation:** Guarded notification dispatch with `ContextCompat.checkSelfPermission` for `POST_NOTIFICATIONS` on API 33+.

### BUG-PERF-01: Redundant Full-Table getCompletedTasks() Scan in StreakCheckWorker [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`
* **Description:** `StreakCheckWorker` called `taskRepository.getCompletedTasks()` twice consecutively to evaluate `completedToday` and `completedYesterday`, duplicating disk reads and object deserialization.
* **Remediation:** Evaluated completed tasks once into a local list variable.

### BUG-ANALYTICS-01: Calendar.set(DAY_OF_WEEK) Week Start Calculation Anomaly [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsViewModel.kt`
* **Description:** In locales where `firstDayOfWeek` is Monday (e.g. ISO-8601), calling `cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)` on a Sunday (which Java Calendar treats as day 1) mutated the date forward to the *following* Monday in the future instead of the start of the current week, returning an empty inverted time window `(startTime > endTime)`.
* **Remediation:** Implemented exact modular day subtraction `cal.add(Calendar.DAY_OF_YEAR, -diff)` ensuring start-of-week always resolves backward or equal to current day.

### BUG-AUDIO-01: NeuralSoundscapePlayer Unhandled NaN in Volume Fuzzing [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`
* **Description:** In `setVolume()`, passing `Float.NaN` to `volume.coerceIn(0f, 1f)` preserved NaN or produced unexpected bounds instead of falling back cleanly to 0f silence, failing adversarial fuzzing stress tests.
* **Remediation:** Added `if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)` in `setVolume()`.

### BUG-CIRCADIAN-02: Post-Prandial Dip Energy Threshold Misalignment [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`
* **Description:** In `determinePhase()`, the post-prandial dip trough gate strictly required `usableEnergy < 0.60f`. During normal baseline chronobiological cycles without accumulated debt or ultradian exhaustion, mid-afternoon energy sits between 0.60f and 0.64f, causing the system to misclassify the afternoon circadian trough as `DECLINE`.
* **Remediation:** Aligned the trough threshold to `usableEnergy < 0.65f` in `determinePhase()`.

### BUG-NOTIF-03: Missing POST_NOTIFICATIONS Check in AccessibilityWatchdogWorker [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/worker/AccessibilityWatchdogWorker.kt`
* **Description:** `AccessibilityWatchdogWorker.showWarningNotification()` called `notificationManager.notify()` unconditionally on Android 13+ (API 33+) without checking runtime permission `POST_NOTIFICATIONS`, risking crashes or SecurityExceptions.
* **Remediation:** Added `ContextCompat.checkSelfPermission` gate prior to notifying.

### BUG-HF-02: Stale Blocked Package on Single-Top HyperFocusActivity Re-Entry [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt`, `HyperFocusViewModel.kt`
* **Description:** `AppBlockingService` launches `HyperFocusActivity` with `FLAG_ACTIVITY_SINGLE_TOP`. If `HyperFocusActivity` was already visible or in backstack when a second blocked app was opened, `onCreate` was not re-run and `onNewIntent` was unimplemented, causing the UI to permanently display the previous app package name instead of the newly blocked app.
* **Remediation:** Implemented `onNewIntent` in `HyperFocusActivity` to dispatch new package intents to `HyperFocusViewModel.setBlockedPackage`, dynamically refreshing `BlockingOverlayScreen`.

### BUG-SCHED-01: Global Priority Sort Destroys Topological Dependency Ordering in SchedulePreparationOrchestrator [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`
* **Description:** Topological sorting was performed into a list, but `return sorted.sortedWith(priorityComparator)` was called at the end on the entire list. This completely wiped out the prerequisite ordering, allowing child tasks with DO quadrant to be placed ahead of their uncompleted parent dependency tasks.
* **Remediation:** Integrated `PriorityQueue<TaskEntity>(priorityComparator)` directly into Kahn's topological sort algorithm, picking the highest priority task among *ready* nodes (in-degree 0) while strictly preserving dependency ordering.

### BUG-CIRCADIAN-01: Post-Midnight Dip Search Window Desynchronization in CircadianEnergyService [FIXED]
* **File:** `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`
* **Description:** In `computeDailySummary()`, when evaluating a night-owl session where `isPostMidnightAwake` is true, the timeline is projected using actual `wakeTimeMillis` from the previous calendar day. However, `dipStartSearchMillis` and `dipEndSearchMillis` were hardcoded to `targetDateWakeMillis` (the future wake time on the current day), causing the 2-hour post-prandial dip window search to miss the actual dip period and trigger the unbounded fallback window.
* **Remediation:** Switched the dip window search bounds to `effectiveWakeMillis = if (isPostMidnightAwake) wakeTimeMillis else targetDateWakeMillis`.

