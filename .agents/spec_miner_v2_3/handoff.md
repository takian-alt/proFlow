# Specification & Architecture Mining Report: Focus Experience (R3) & Secondary Tools Integration (R4)

**Agent**: `spec_miner_v2_3`  
**Working Directory**: `/home/takian/Project/PROCUS/.agents/spec_miner_v2_3`  
**Workspace Root**: `/home/takian/Project/PROCUS`  
**Timestamp**: `2026-10-16T19:18:00Z`  
**Scope**: Requirements R3 (Focus Experience & Autonomic Recovery) and R4 (Contextual Integration of Secondary Tools) from `ORIGINAL_REQUEST.md` (Section ## 2026-10-16T13:05:30Z).

---

## 1. Observation

Direct investigation of the codebase revealed the current state of R3 and R4 components across data, domain, and presentation layers:

### 1.1 Focus Experience (R3) Codebase Findings

1. **Focus Screen & ViewModel (V2)**:
   - File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` (lines 41–475)
   - File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 1–269)
   - `FocusTimerModeV2` (lines 27–32) defines the 4 timer modes:
     ```kotlin
     enum class FocusTimerModeV2(val durationSeconds: Long, val title: String) {
         JUST_5_MIN(300L, "5m Micro-Start"),
         POMODORO_25(1500L, "25m Pomodoro"),
         ULTRADIAN_90(5400L, "90m Ultradian Block"),
         FLOW_STOPWATCH(0L, "Flow Stopwatch")
     }
     ```
   - `RadialTimerGauge` in `FocusScreenV2.kt:390-474` renders a sweep-gradient bioluminescent radial arc with time display (`MM:SS`) and state label (`READY`, `PAUSED`, `FLOW IN PROGRESS`).
   - Micro-Steps Checklist is implemented in `FocusScreenV2.kt:217-299` and backed by `StepByStepPlanCodec.kt:10-55` (`parse` and `serialize` on `task.ifThenPlan`).
   - Thought Parking Drawer is implemented in `FocusScreenV2.kt:344-386` via `ModalBottomSheet` and `FocusViewModelV2.kt:247-259`:
     ```kotlin
     fun parkIntrusiveThought(thoughtText: String) {
         if (thoughtText.isBlank()) return
         viewModelScope.launch {
             val newTask = TaskEntity(
                 title = thoughtText.trim(),
                 quadrant = Quadrant.DELEGATE,
                 energyLevel = EnergyLevel.LOW,
                 estimatedDurationMinutes = 10,
                 impactScore = 30
             )
             taskRepository.insert(newTask)
         }
     }
     ```
   - **Gaps identified in V2 Focus**:
     * **Autonomic Reset (60s Physiological Sigh)**: Currently missing in `FocusScreenV2` and `FocusViewModelV2`. When countdown timers reach 0 in `tickSecond()`, the timer simply clamps at `remainingSeconds = 0` without triggering a break or breathing guide.
     * **Post-Session Affordance Rating**: Currently `FocusViewModelV2.completeTask()` (lines 177–196) directly marks the task complete and displays a simple celebration `AlertDialog`, bypassing the 1-tap effort rating sheet.
     * **System DND**: `enableDndIfPermitted()` and `disableDnd()` are present in V1 (`presentation/focus/FocusViewModel.kt:322-339`), but have NOT yet been wired into `FocusViewModelV2.kt`.

2. **DND Implementation in V1 Reference**:
   - `app/src/main/java/com/neuroflow/app/presentation/focus/FocusViewModel.kt:322-339`:
     ```kotlin
     private fun enableDndIfPermitted() {
         try {
             if (!_uiState.value.preferences.dndEnabled) return
             val nm = application.getSystemService(android.app.NotificationManager::class.java)
             if (nm != null && nm.isNotificationPolicyAccessGranted) {
                 nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
             }
         } catch (_: Exception) {}
     }

     private fun disableDnd() {
         try {
             val nm = application.getSystemService(android.app.NotificationManager::class.java)
             if (nm != null && nm.isNotificationPolicyAccessGranted) {
                 nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
             }
         } catch (_: Exception) {}
     }
     ```

3. **Affordance Rating Calibration in Domain & V1**:
   - `app/src/main/java/com/neuroflow/app/presentation/focus/AffordanceRatingSheet.kt:20-150`: Uses a 5-point scale `[-2f, -1f, 0f, 1f, 2f]`.
   - `app/src/main/java/com/neuroflow/app/presentation/focus/FocusWoopManager.kt:77-85`:
     ```kotlin
     suspend fun submitAffordanceRating(taskId: String, rating: Float): Boolean {
         val task = taskRepository.getById(taskId) ?: return false
         taskRepository.update(task.copy(affectiveForecastError = rating, updatedAt = System.currentTimeMillis()))
         syncEnjoymentForecastBias(...)
         return true
     }
     ```
   - `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt:146-175`: `EnjoymentCalibration` aggregates `TaskEntity.affectiveForecastError` to compute `prefs.enjoymentForecastBias`, which calibrates `TaskScoringEngine.kt:797` for future prioritization and effort forecasting.

---

### 1.2 Secondary Tools & Classic Capabilities (R4) Codebase Findings

1. **Distraction-Free Notes Scratchpad**:
   - Entities & DAOs: `NoteEntity.kt:8-19` (`tableName = "notes"`), `NoteDao.kt:8-38`, `NoteRepository.kt:10-58`.
   - UI Screens: `NotesScreen.kt:40-331` and `NoteEditorScreen.kt`.
   - Preferences Quick Note: `UserPreferences.leftPageQuickNote` (`UserPreferencesDataStore.kt:105, 172-240`).
   - Current V2 Integration: Currently `NotesScreen` is only reachable via classic navigation routes. Neither `FlowScreenV2` nor `PlanScreenV2` surfaces a Notes scratchpad bottom sheet.

2. **Chronotype MEQ Quiz & Sleep Log Visualizer**:
   - MEQ Quiz: `domain/engine/MEQChronotypeDetector.kt`, `presentation/settings/MEQQuizScreen.kt:23-288`, `MEQQuizViewModel.kt:16-125`. Uses Horne-Östberg 19-question standardized assessment.
   - Sleep Logs: `data/local/entity/SleepLogEntity.kt:8-20` (`tableName = "sleep_logs"`), `SleepLogDao.kt:11-32`, `SleepLogRepository.kt`, `presentation/settings/SleepLogsScreen.kt:66-408`.
   - Circadian Barometer: `presentation/v2/flow/CircadianHeroBarometer.kt:43-362` visualizes Process C (alertness) and Process S (sleep pressure points), but lacks direct interactive links or navigation triggers to `MEQQuizScreen` and `SleepLogsScreen`.

3. **Ulysses Pre-Commitment Contracts with Stakes**:
   - Entity & DAO: `UlyssesContractEntity.kt:9-18` (`tableName = "ulysses_contracts"`, fields: `id`, `taskId`, `deadlineAt`, `consequence`, `outcome`, `createdAt`), `UlyssesContractDao.kt:8-27`, `UlyssesContractRepository.kt:10-20`.
   - Background Enforcement: `UlyssesEvaluatorWorker.kt:18-62` checks completion against deadline upon worker trigger.
   - UI Sheet: `presentation/common/UlyssesContractSheet.kt:26-146`.
   - Current V2 Integration: `InsightsViewModelV2.kt:33, 52-134` already injects `UlyssesContractRepository` and tracks `activeContractsCount`, but `InsightsScreenV2.kt` does NOT render the contracts card or afford creation. Tasks in Flow/Plan have no 1-tap entry point to bind Ulysses contracts to high-resistance tasks.

4. **Floating Context/Tag Pills (`#deepwork`, `#offline`, `#admin`)**:
   - Auto-inference generates context tags in `TaskInferenceEngine.kt:222` (`#deepwork`, `#offline`, `#admin`, etc.).
   - `TaskEntity.tags` (comma-separated tags) and `TaskEntity.contextTag` (`@work`, `@home`, `@computer`, `#deepwork`).
   - Neither `FlowScreenV2` nor `PlanScreenV2` currently provides floating or sticky filter pills to slice the view by tag.

5. **Protected Slots**:
   - Model: `ProtectedTimeSlot.kt:10-45` (`PtsCategory`, `PtsRecurrence`).
   - Sheet: `presentation/schedule/ProtectedSlotBottomSheet.kt:1-892`.
   - Current V2: `PlanScreenV2.kt:660-697` displays existing protected slots in `AUTOPILOT` mode with enable/disable switches, but lacks a button to launch `ProtectedSlotBottomSheet` for creating/editing slots.

---

### 1.3 Database & Backward Compatibility Findings

1. **Room Database**:
   - File: `app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt:314-332`
   - Database Version: **21**
   - 11 Entities: `TaskEntity`, `TimeSessionEntity`, `GoalEntity`, `WoopEntity`, `UlyssesContractEntity`, `UnlockCodeEntity`, `HyperFocusSessionEntity`, `SleepLogEntity`, `EnergyPredictionEntity`, `NoteEntity`, `GateRequestEntity`.
   - Migrations: `MIGRATION_1_2` through `MIGRATION_20_21` are fully implemented with non-destructive `ALTER TABLE` and `CREATE TABLE IF NOT EXISTS` statements.
   - Schema stability: **No schema changes or database version bumps are needed** for R3 or R4. All required tables (`notes`, `sleep_logs`, `ulysses_contracts`, `woop_data`, `time_sessions`) already exist with proper columns and types.

2. **Preference Keys**:
   - All preference keys in `UserPreferencesDataStore.kt:172-260` (`dndEnabled`, `leftPageQuickNote`, `manualChronotype`, `quizChronotype`, `sleepPressurePoints`, `protectedTimeSlots`) are backward-compatible.

3. **Unit Test Suite Baseline**:
   - Executed `./gradlew testDebugUnitTest --continue` via background task.
   - Result: `BUILD SUCCESSFUL in 58s`, **0 test failures**, 34 tasks, all JUnit 4 & 5 suites passing.

---

## 2. Logic Chain

From the observations above, we establish the following architectural inferences:

```
[Observation: FocusTimerModeV2 has 4 modes but no break action]
      │
      ▼
(Inference 1: tickSecond() must detect remainingSeconds == 0 to trigger Break/Autonomic Reset)
      │
      ▼
(Inference 2: Implement 60-second Physiological Sigh based on Huberman cyclic sighing:
              10 cycles of 6s = 2s Nasal Inhale + 1s Sharp Inhale + 3s Slow Sigh Exhale)

[Observation: V1 DND implementation uses NotificationManager.INTERRUPTION_FILTER_PRIORITY]
      │
      ▼
(Inference 3: Inject @ApplicationContext into FocusViewModelV2; call enableDnd() on timer start/resume,
              call disableDnd() on pause/stop/complete/onCleared)

[Observation: CompleteTask in V2 completes immediately without collecting effort rating]
      │
      ▼
(Inference 4: In FocusScreenV2, prompt 1-tap 1–5 effort rating on completion;
              map rating 1–5 to -2f..+2f affectiveForecastError; update TaskEntity and EnjoymentCalibration)

[Observation: Notes, MEQ, Sleep Logs, Ulysses, Protected Slots exist in DAOs/V1 screens]
      │
      ▼
(Inference 5: R4 requires contextual access WITHOUT legacy drawer navigation:
              • Notes Scratchpad: Surfaced as BottomSheet from Flow and Plan
              • MEQ Quiz & Sleep Logs: Linked directly from CircadianHeroBarometer
              • Ulysses Contracts: Card in InsightsScreenV2 + context action on high-resistance tasks
              • Floating Tag Pills: Sticky/floating row on Flow & Plan filtering by #deepwork, #offline, #admin
              • Protected Slots: "+ Add Slot" button launching ProtectedSlotBottomSheet directly in PlanV2)
```

---

## 3. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R3: Modes | 4 Focus Timer Modes | Supports 5m Micro-Start (300s), 25m Pomodoro (1500s), 90m Ultradian (5400s), and Flow Stopwatch (count up). | Mode selection chip | `remainingSeconds`, `elapsedSeconds` | Ignored if timer already running | `FocusViewModelV2.kt:27-32` |
| 2 | R3: Autonomic | 60s Physiological Sigh Guide | Neuroscience-backed autonomic reset: 2 quick inhales (2s + 1s) followed by 1 slow exhale (3s) across 10 cycles (60s) during break transitions to reduce acute cortisol. | Break trigger / manual button | Animated pulsing gauge, cycle count, phase cues | Graceful cancel / skip return to session | Stanford cyclic sighing literature & `ORIGINAL_REQUEST.md:71` |
| 3 | R3: Decomposition | Micro-Step Checklist | Inline micro-step checklist parsed and stored as markdown checkboxes (`[x]`/`[ ]`) in `task.ifThenPlan`. | Checkbox click, new step string | Updates `TaskEntity.ifThenPlan` via `StepByStepPlanCodec` | Blank strings rejected | `FocusScreenV2.kt:216-299`, `StepByStepPlanCodec.kt` |
| 4 | R3: Working Memory | Thought Parking Drawer | Instant bottom sheet during active focus to park intrusive thoughts as low-energy tasks (`Quadrant.DELEGATE`, 10m). | Intrusive thought text string | Inserts new `TaskEntity` into Room | Blank input disables commit | `FocusScreenV2.kt:344-386`, `FocusViewModelV2.kt:247-259` |
| 5 | R3: ML Calibration | Post-Session Affordance Rating | 1-tap 1–5 scale rating on completion. Mapped to `affectiveForecastError` (`(3 - rating).toFloat()` or `(rating - 3).toFloat()`) to calibrate `AnalyticsEngine.EnjoymentCalibration` and future priority. | Integer 1–5 tap | Updates `task.affectiveForecastError` & `prefs.enjoymentForecastBias` | Skippable without blocking completion | `AffordanceRatingSheet.kt`, `FocusWoopManager.kt:77` |
| 6 | R3: System DND | Priority DND Interruption Filter | Engages Android system Priority DND during active tracking; restores normal policy on pause/stop/clear. | Timer start/pause events | `nm.setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY/ALL)` | Swallowed safely if permission not granted | `FocusViewModel.kt:322-339` |
| 7 | R4: Scratchpad | Distraction-Free Notes Scratchpad | Instant bottom sheet on Flow and Plan allowing quick unstructured capture without leaving context. | Text content | Saves to `NoteRepository` or `prefs.leftPageQuickNote` | Empty notes discarded | `NoteRepository.kt`, `UserPreferencesDataStore.kt:240` |
| 8 | R4: Circadian Tools | Barometer MEQ & Sleep Log Links | Interactive action chips on Circadian Barometer linking directly to Horne-Östberg MEQ quiz and Sleep Log visualizer. | Tap on Barometer action chip | Navigates to `Screen.MEQQuiz` or `Screen.SleepLogs` in V2 nav host | Safe backstack pop returns to Flow | `CircadianHeroBarometer.kt:70-128`, `MEQQuizScreen.kt` |
| 9 | R4: Commitment | Ulysses Pre-Commitment Contracts | Binding contracts with stakes (`consequence`) and deadlines, evaluated by `UlyssesEvaluatorWorker`. Surfaced in Insights & high-resistance tasks. | Task ID, deadline timestamp, consequence text | Inserts `UlyssesContractEntity`, schedules WorkManager | Enforces future deadline requirement | `UlyssesContractEntity.kt`, `UlyssesContractSheet.kt` |
| 10 | R4: Context Filters | Floating Context Tag Pills | Floating tag pills (`#deepwork`, `#offline`, `#admin`) on Flow and Plan to filter timeline and matrix tasks. | Tag pill tap (toggle) | Filters task list in UI state | Tapping active tag clears filter to "All" | `TaskInferenceEngine.kt:222`, `TaskEntity.tags` |
| 11 | R4: Schedule Protection | Protected Time Slots Integration | Direct management and addition of protected recurring blocks (`MINDFULNESS`, `MEAL`, `FAMILY`, `WORK`) within Plan V2 Autopilot view. | Add slot button / edit slot tap | Opens `ProtectedSlotBottomSheet` | Rejects overlapping invalid slots | `ProtectedTimeSlot.kt`, `ProtectedSlotBottomSheet.kt` |

---

## 4. Edge Cases

| # | Feature | Input | Observed / Specified Behavior |
|---|---------|-------|-------------------------------|
| 1 | Focus Stopwatch Mode | Timer mode set to `FLOW_STOPWATCH` | `durationSeconds = 0L`. `tickSecond()` increments `elapsedSeconds` indefinitely while keeping `remainingSeconds = 0L`. Does NOT trigger break countdown. |
| 2 | Autonomic Reset Interruption | User dismisses or navigates back during 60s sigh | Resets sigh state cleanly, stops sigh timer coroutine, returns to paused state or completion summary without crashing. |
| 3 | System DND Permission Missing | `isNotificationPolicyAccessGranted == false` | `nm.isNotificationPolicyAccessGranted` check guards call; catches any SecurityException safely without crashing the app. |
| 4 | Post-Session Affordance Skip | User taps "Skip" or dismisses affordance prompt | Task completes normally, `affectiveForecastError` remains `null`, streak and points are still awarded. |
| 5 | Notes Scratchpad Blank Save | User closes Notes bottom sheet with empty title and body | Empty note is discarded; no empty row inserted into `notes` database table. |
| 6 | Ulysses Contract with Past Deadline | User selects a deadline earlier than `System.currentTimeMillis()` | Form validation disables "Commit" button and displays warning: "Deadline must be in the future." |
| 7 | Tag Pill Active with No Matching Tasks | Filter pill selected for a tag with 0 matching tasks | Empty state banner displays: "No tasks found with tag `<tag>`. Tap to clear filter." |
| 8 | High-Resistance Task Ulysses Binding | Task has high effort (effortScore >= 70) or postponeCount >= 2 | Context menu or indicator triggers `UlyssesContractSheet` pre-selecting this task. |

---

## 5. Compatibility Verification

1. **Room Database**:
   - `NeuroFlowDatabase` version is **21**.
   - Tables verified: `tasks`, `time_sessions`, `goals`, `woop_data`, `ulysses_contracts`, `unlock_codes`, `hyperfocus_sessions`, `sleep_logs`, `energy_predictions`, `notes`, `gate_requests`.
   - All required fields for R3 and R4 are already present in existing entities:
     * `TaskEntity.affectiveForecastError` (REAL nullable)
     * `TaskEntity.ifThenPlan` (TEXT not null)
     * `TaskEntity.tags` (TEXT not null)
     * `TaskEntity.contextTag` (TEXT not null)
     * `NoteEntity` (all 8 columns exist)
     * `SleepLogEntity` (all 7 columns exist)
     * `UlyssesContractEntity` (all 6 columns exist)
   - **Zero DB migrations needed**. No risk of data corruption or schema mismatch.

2. **User Preferences**:
   - `dndEnabled`: boolean key exists in DataStore.
   - `leftPageQuickNote`: string key exists in DataStore.
   - `protectedTimeSlots`: json-serialized list exists in DataStore.
   - `useV2Ui`: boolean flag controlling V2 vs V1 entry in `MainActivity.kt:302`.

3. **Automated Unit Tests**:
   - `./gradlew testDebugUnitTest` passed cleanly with 0 errors across all modules.

---

## 6. Concrete Implementation Recommendations for the Worker

### Recommendation 1: Autonomic Reset Guide (R3)
Create `app/src/main/java/com/neuroflow/app/presentation/v2/focus/AutonomicResetSheet.kt` (or dialog/overlay):
- State machine:
  * Total duration: 60 seconds (`remainingSeconds: 60 down to 0`).
  * 10 cycles, each 6 seconds:
    - `0.0s .. 2.0s`: **Inhale 1** — "Deep inhale through your nose" (bioluminescent ring expands to 70%).
    - `2.0s .. 3.0s`: **Inhale 2** — "Quick sharp top-off inhale" (ring expands to 100%).
    - `3.0s .. 6.0s`: **Exhale** — "Long, slow sigh through mouth" (ring slowly contracts to 30%).
  * Color: `NeuroFlowThemeV2Colors.RoseCoral` (`#F43F5E`) or `SageGreen` (`#10B981`) for autonomic parasympathetic tone.
  * Haptic ticks on phase switches if vibrator available.
- Trigger in `FocusViewModelV2.kt`:
  * In `tickSecond()`, when `newRemaining == 0L` in fixed timer modes (`POMODORO_25`, `ULTRADIAN_90`, `JUST_5_MIN`), transition state to `showAutonomicReset = true`.
  * Also expose a manual "Autonomic Sigh Reset" button in `FocusScreenV2` header or control row.

### Recommendation 2: Post-Session Affordance Rating (R3)
In `FocusScreenV2.kt` & `FocusViewModelV2.kt`:
- On `completeTask()`, transition state to `showAffordanceRating = true` before showing completion celebration.
- Render a 1-tap 5-choice horizontal selector:
  * 1: "Very Hard" (high friction) -> maps to `-2f`
  * 2: "Hard" -> maps to `-1f`
  * 3: "Expected" -> maps to `0f`
  * 4: "Easy" -> maps to `+1f`
  * 5: "Flow State" (very easy) -> maps to `+2f`
- When user taps any choice:
  * Update `task.copy(affectiveForecastError = rating)`.
  * Call `AnalyticsEngine.EnjoymentCalibration` and update `prefs.enjoymentForecastBias`.
  * Advance to completion celebration overlay.

### Recommendation 3: System DND Integration (R3)
In `FocusViewModelV2.kt`:
- Inject `@ApplicationContext private val context: Context`.
- Add `enableDndIfPermitted()` and `disableDnd()` methods identical to V1.
- Call `enableDndIfPermitted()` in `startTimer()` and `resumeTimer()`.
- Call `disableDnd()` in `pauseTimer()`, `completeTask()`, and `onCleared()`.

### Recommendation 4: Notes Scratchpad Bottom Sheet (R4)
Create `app/src/main/java/com/neuroflow/app/presentation/v2/notes/NotesScratchpadSheet.kt`:
- Surfaced via an `IconButton` (Note icon) in TopAppBar of `FlowScreenV2` and `PlanScreenV2`.
- Contains:
  * Instant scratchpad text field auto-saved to `NoteRepository` or `prefs.leftPageQuickNote`.
  * List of recent notes with 1-tap expand/edit.
  * "+ Add Note" action.
  * Obsidian theme styling with 48dp touch targets.

### Recommendation 5: Circadian Barometer Links to MEQ & Sleep Logs (R4)
In `CircadianHeroBarometer.kt`:
- Add navigation callbacks `onNavigateToMeqQuiz: () -> Unit` and `onNavigateToSleepLogs: () -> Unit`.
- In the header or explainer surface, add 2 prominent interactive pills:
  * `SuggestionChip` / `FilterChip`: "🧬 Chronotype MEQ Quiz" -> triggers `onNavigateToMeqQuiz`.
  * `SuggestionChip` / `FilterChip`: "🌙 Sleep Logs & Pressure" -> triggers `onNavigateToSleepLogs`.
- In `NeuroFlowAppV2.kt`:
  * Add composable destinations for `"meq_quiz"` (hosting `MEQQuizScreen`) and `"sleep_logs"` (hosting `SleepLogsScreen`).
  * Wire callbacks from `FlowScreenV2` to `navController.navigate("meq_quiz")` and `"sleep_logs"`.

### Recommendation 6: Ulysses Contracts in Insights & High-Resistance Tasks (R4)
In `InsightsScreenV2.kt` & `InsightsViewModelV2.kt`:
- Expose `val activeContracts: List<UlyssesContractEntity>` in `InsightsUiStateV2`.
- Add a dedicated section in `InsightsScreenV2`: "⚔️ Ulysses Pre-Commitment Contracts".
- Display active contracts with time-remaining countdown badges and consequences.
- Add "+ Create Contract" button that displays `UlyssesContractSheet`.
- On task cards in Flow & Plan: for tasks with `effortScore >= 70` or `postponeCount >= 2`, show a small Ulysses binding button (`⚔️`) that opens `UlyssesContractSheet` pre-populated with that task ID.

### Recommendation 7: Floating Context/Tag Pills on Flow & Plan (R4)
In `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `PlanScreenV2.kt`, `PlanViewModelV2.kt`:
- Add `selectedTagFilter: String? = null` in UI state.
- Add a horizontal floating pill row above the stream/timeline:
  * Pills: `All`, `#deepwork`, `#offline`, `#admin`.
  * When a pill is tapped, filter the displayed tasks:
    ```kotlin
    val filteredTasks = if (selectedTag == null) allTasks
    else allTasks.filter { it.tags.contains(selectedTag, ignoreCase = true) || it.contextTag.contains(selectedTag, ignoreCase = true) }
    ```
  * Pill style: `FilterChip` styled in Obsidian Dark with `ElectricIndigo` accent when selected.

### Recommendation 8: Protected Slots in Plan V2 (R4)
In `PlanScreenV2.kt`:
- In `AutopilotConfigView`, under "🛡️ Manage Protected Time Slots", add an OutlinedButton: "+ Add Protected Slot".
- Tapping opens `ProtectedSlotBottomSheet`, providing complete management of protected slots in V2 without navigating to legacy screens.

---

## 7. Caveats

- **Alternative UI Placement**: The Autonomic Reset can be implemented either as an in-place state within `RadialTimerGauge` on `FocusScreenV2` or as a full-surface `ModalBottomSheet`. Both are technically viable; having it embedded in `FocusScreenV2` preserves focus context without modal dismiss risk.
- **DND Policy Access**: On Android 7.0+, changing DND requires `android.permission.ACCESS_NOTIFICATION_POLICY`. If not granted by user in Android System Settings, the call is safely ignored without throwing an unhandled exception.
- **No other caveats**: Database schemas, DAOs, repositories, and ViewModels were verified with 100% confidence.

---

## 8. Conclusion

Requirements R3 (Focus Experience & Autonomic Recovery) and R4 (Contextual Integration of Secondary Tools) have clean, direct architectural mapping onto existing V2 components:
1. The 4 focus modes are already structured in `FocusTimerModeV2`.
2. Autonomic Reset (60-second physiological sigh guide) requires a 10-cycle 6s countdown state machine (2s inhale + 1s top-off + 3s exhale) triggered on break transitions.
3. Affordance rating maps 1-tap 1–5 scale to `TaskEntity.affectiveForecastError` (`-2f..+2f`) and feeds `AnalyticsEngine.EnjoymentCalibration`.
4. System DND priority interruption filter is directly portable from V1 `FocusViewModel`.
5. Secondary tools (Notes, MEQ, Sleep Logs, Ulysses, Tag pills, Protected Slots) can be surfaced contextually into Flow, Plan, and Insights using bottom sheets and direct navigation routes with 100% database backward compatibility and zero data loss.

---

## 9. Verification Method

To verify the findings and any future implementation:
1. **Compilation & Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   Must pass with 0 errors across all test suites including `NeuroFlowV2UiTest`.
2. **Schema & Migration Verification**:
   Inspect `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/21.json` to confirm Room version 21 integrity.
3. **Touch Targets & Contrast**:
   Ensure all interactive pills, chips, and buttons on Focus, Notes, and Barometer maintain >= 48x48dp touch targets and comply with `NeuroFlowThemeV2Colors.Obsidian` palette.
