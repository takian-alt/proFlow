# Handoff Report: R2 Progressive Disclosure & Anti-Paralysis Scaffolding

**Agent**: `explorer_v2_2`  
**Date**: 2026-10-16  
**Scope**: R2. Progressive Disclosure & Anti-Paralysis Scaffolding (neuroFlow V2)  
**Parent Task ID**: `7d403ceb-270c-4305-9953-41a2f85cacb5`  

---

## 1. Observation

### 1.1 Present Flow & V2 Architecture
- **Navigation & Entry Point**:
  - `MainActivity.kt` lines 302–306: launches `NeuroFlowAppV2` when `preferences.useV2Ui == true`.
  - `NeuroFlowAppV2.kt` lines 81–86: maps `NavHubsV2.Flow.route` to `FlowScreenV2`.
- **Flow Screen & ViewModel**:
  - `FlowScreenV2.kt` (lines 1–134): Renders `CircadianHeroBarometer`, `EnergyTimelineStream`, and a FloatingActionButton launching `SmartTaskComposerSheetV2`.
  - `FlowViewModelV2.kt` (lines 1–95): Combines `taskRepository.observeActiveTasks()`, `taskRepository.observeCompletedTasks()`, `energyScoreRepository.observeEnergy()`, and `preferencesDataStore.preferencesFlow`.
  - Naive Recommendation Selection (`FlowViewModelV2.kt` lines 49–53):
    ```kotlin
    val recommended = activeTasks
        .filter { it.energyLevel == currentEnergyZone }
        .maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }
        ?: activeTasks.maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }
    ```
    This completely bypasses `TaskScoringEngine`'s 16 neuroscience frameworks and live energy calculation.
- **Circadian Barometer**:
  - `CircadianHeroBarometer.kt` (lines 1–364): Visualizes Process C (circadian alertness oscillation), Process S (homeostatic sleep pressure accumulation), current time cursor beacon, and bio-battery energy level.
  - Currently nests the "Optimal Next Action" card inside its own Surface container (lines 296–360), rather than providing a separate prominent hero card on the Flow screen.
- **Timeline Stream**:
  - `EnergyTimelineStream.kt` (lines 1–211): Takes `scheduledTasks: List<TaskEntity>`. Renders a simple flat list of `TimelineTaskCard`s.
  - Currently lacks task partitioning (overdue vs scheduled vs later), overdue visual treatment, and "Break It Down" scaffolding.

### 1.2 Legacy V1 Capabilities & Reference Implementations
- **Undo Buffer**:
  - `TodayViewModel.kt` lines 39–49, 347–392, 407–412: Implements a 4000ms delay coroutine job with `UndoAction(task, actionType, job)` and `pendingDismissIds: Set<String>`.
  - `TodayScreen.kt` lines 96–112: `LaunchedEffect(uiState.pendingUndoAction)` showing snackbar with "UNDO" action.
  - `FlowViewModelV2.kt` currently performs instantaneous, destructive database writes on `completeTask` and `deferTask` with no undo buffer.
- **Overdue Tasks & Alarmist Red**:
  - `TodayScreen.kt` line 284: `SectionHeader("Overdue", badge = "${uiState.overdueTasks.size}", badgeColor = NeuroFlowColors.DoFirstText)` renders intense alarmist red (`#C62828`).
  - V2 sensory design requires eliminating red warning badges and replacing them with low-anxiety indicators (Muted Coral, Warm Amber, or Slate).
- **Evening Reflection & Mental Loop Closure**:
  - `TodayViewModel.kt` lines 289–291:
    ```kotlin
    val wrapUpStartHour = maxOf(19, prefs.sleepHour - 2)
    val showEveningWrapUp = hour >= wrapUpStartHour && hour < 24
    ```
  - `TodayScreen.kt` lines 864–911: `EveningWrapUpCard` showing completed task count, focus minutes logged, and "Push Remaining →" button (`pushRemainingToTomorrow()`).
  - Neither feature currently exists in V2 (`FlowScreenV2` / `FlowViewModelV2`).

### 1.3 Micro-Step Checklist & Initiation Scaffolding
- **StepByStepPlanCodec**:
  - `StepByStepPlanCodec.kt` (lines 1–56): Encodes/decodes markdown checklists (`[ ] step`, `[x] step`) to/from plain text.
  - `FocusViewModelV2.kt` lines 79, 216–245: Parses `task.ifThenPlan` via `StepByStepPlanCodec.parse(it.ifThenPlan)` and exposes `microSteps: List<StepByStepPlanStep>` with `toggleMicroStep(index)` and `addMicroStep(stepText)`.
  - `TaskEntity.kt` line 67: Contains `val ifThenPlan: String = ""` and line 41 `val parentTaskId: String? = null`.
  - No heuristic decomposition engine exists yet to automatically break down a task title into 3 micro-steps upon tapping a "magic wand".

### 1.4 Room Database & Schema Status
- **Database Version**: 21 (`NeuroFlowDatabase.kt` line 316).
- **Converters**:
  - `Converters.kt` lines 37–44:
    ```kotlin
    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String): TaskStatus =
        try {
            TaskStatus.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown TaskStatus: '$value', defaulting to ACTIVE", e)
            TaskStatus.ACTIVE
        }
    ```
- **TaskStatus Enum**:
  - `Enums.kt` lines 16–20: Currently contains `ACTIVE`, `COMPLETED`, `ARCHIVED`. Does NOT yet contain `ICEBOX`.
  - SQLite column affinity for `status` is `TEXT NOT NULL` (`21.json` line 180–184).
- **TaskDao & Repository**:
  - `TaskDao.kt` line 36: `observeActiveTasks()` queries `SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY createdAt DESC`.
  - Line 30: `observeByStatus(status: TaskStatus)` queries `SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC`.
  - Tasks with status `ICEBOX` will automatically be excluded from active tasks, auto-scheduling, and daily streams.

---

## 2. Logic Chain & Gap Analysis

```
[R2 Requirements]
       │
       ├── 1. Flow Screen Layout: Barometer + Prominent Hero Card + Vertical Timeline Stream
       │      └── Current Gap: Hero card is embedded inside the barometer Canvas wrapper; needs clean separation.
       │
       ├── 2. Recommended Next Action Scoring
       │      └── Current Gap: Naive battery check in FlowViewModelV2; needs TaskScoringEngine.rankTasks with LiveEnergyContext.
       │
       ├── 3. Vertical Timeline & Overdue Tasks (Zero Alarmist Red)
       │      └── Current Gap: Unpartitioned list; overdue tasks lack gentle, low-arousal styling.
       │
       ├── 4. Zero-Guilt Fresh Start (>3 Overdue Tasks)
       │      └── Current Gap: No trigger logic in FlowViewModelV2; TaskStatus lacks ICEBOX enum; no 1-tap rollover.
       │
       ├── 5. "Break It Down" Magic Wand (3 Microscopic Steps)
       │      └── Current Gap: No heuristic generator from task title -> 3 steps; UI lacks wand button.
       │
       ├── 6. 4-Second Non-Destructive Undo
       │      └── Current Gap: Complete/defer in FlowViewModelV2 are instantaneous; no snackbar host or timer delay.
       │
       └── 7. Evening Shutdown Complete Reflection Card (After 19:00)
              └── Current Gap: No time check or wrap-up card on Flow screen.
```

### Detailed Component Logic

#### 2.1 Hero Card & Circadian Barometer Separation
- **Design Intent**: The user should see at most 3 distinct cognitive sections on Flow:
  1. The **Circadian Barometer**: High-level biological state (Process C & S curves + Bio-Battery %).
  2. The **Recommended Next Action**: A single prominent, distraction-free hero card answering *"What should I do right now?"*.
  3. The **Energy Timeline Stream**: The chronological task queue for today.
- **Action**: Extract `RecommendedNextActionHeroCard` out of `CircadianHeroBarometer.kt` into a standalone composable component.

#### 2.2 Algorithmic Recommendation via LiveEnergyContext
- `TaskScoringEngine` already contains `LiveEnergyContext(usableEnergy, reservoirFactor, circadianFactor, sleepPressureRatio, confidence)` and `rankTasks(tasks, prefs, liveEnergy, now)`.
- In `FlowViewModelV2`:
  ```kotlin
  val liveEnergy = energyUi?.let { ui ->
      TaskScoringEngine.LiveEnergyContext(
          usableEnergy = ui.availableEnergy.toFloat(),
          reservoirFactor = ui.reservoirFactor,
          circadianFactor = ui.circadianFactor,
          sleepPressureRatio = (ui.sleepPressurePoints / 1000f).coerceIn(0f, 1f),
          confidence = ui.confidenceFactor
      )
  }
  val ranked = TaskScoringEngine.rankTasks(todayOrActiveTasks, prefs, liveEnergy, now)
  val topAction = ranked.firstOrNull()
  ```
- Generate a dynamic, neuroscience-grounded rationale badge:
  - "🐸 Daily Frog (Eat First)"
  - "⚡ Peak Biological Window Match"
  - "🎯 High Strategic Impact (Score: X)"
  - "⏰ Approaching Due Time"

#### 2.3 Zero-Guilt Fresh Start (>3 Overdue Tasks -> Icebox Rollover)
- Calculate overdue count: `overdueTasks = activeTasks.filter { (it.scheduledDate != null && it.scheduledDate < todayStart) || (it.deadlineDate != null && it.deadlineDate < todayStart) }`.
- When `overdueTasks.size > 3`:
  - Show `ZeroGuiltFreshStartCard` above the timeline.
  - Visual: Slate surface (`#14171F`), gentle Sage Green / Indigo accent, no red text or warning badges.
  - Copy: *"Fresh Start Available: You have ${overdueTasks.size} tasks from previous days. It's completely normal to fall behind. Clear the mental clutter and roll them over to your Icebox in one tap — clean slate, zero guilt."*
  - Action: "1-Tap Rollover to Icebox" -> updates `overdueTasks` with `status = TaskStatus.ICEBOX`, `scheduledDate = null`, `scheduledTime = null`.

#### 2.4 "Break It Down" Magic Wand (3 Microscopic Steps)
- Executive dysfunction in ADHD often stems from task initiation friction when titles are abstract or intimidating (e.g. "Prepare Quarterly Taxes", "Fix Memory Leak").
- Domain Engine: `BreakItDownEngine` (in `com.neuroflow.app.domain.engine`):
  - Infers initiation category from title, `taskType`, and keywords.
  - Produces exactly 3 microscopic, low-friction steps:
    1. *Setup / Environment*: "Open the project and locate the relevant file / gather receipts"
    2. *Microscopic Initiation (2 mins)*: "Write the first 2 sentences / inspect line 1 / fill box 1"
    3. *Momentum Anchor*: "Draft outline / run single test / review first section"
  - Encodes steps via `StepByStepPlanCodec.serialize(steps)` into `task.ifThenPlan`.
  - When stored in `ifThenPlan`, the steps immediately appear on the `TimelineTaskCard` (with interactive checkboxes) AND automatically hydrate the micro-step checklist on `FocusScreenV2` when the user enters focus mode!

#### 2.5 4-Second Non-Destructive Undo
- Floating snackbar on Flow screen:
  - When user taps "Complete" or "Defer":
    1. Task ID is added to `pendingDismissIds` in `FlowViewModelV2` -> immediate visual removal.
    2. A coroutine delay of `4000L` is scheduled with a cancellable `Job`.
    3. UI displays a floating `Snackbar` with action label `"UNDO"`.
    4. If the user taps "UNDO" within 4 seconds: `job.cancel()`, task ID removed from `pendingDismissIds`, task immediately reappears.
    5. If 4 seconds elapse: permanent database update is committed (`completeAndRecur` or `deferTask`).

#### 2.6 Evening Shutdown Complete (After 19:00 / 7:00 PM)
- When wall-clock hour $\ge 19$:
  - Flow screen displays `EveningShutdownCard`.
  - Displays: Completed tasks count today, deep focus minutes logged today.
  - Actions:
    - "Push Remaining to Tomorrow": 1-tap reschedule of all open scheduled tasks to tomorrow midnight, emptying today's stream.
    - "Complete Shutdown": Closes mental loops, showing a tranquil Obsidian badge: *"🌙 Evening Shutdown Complete. Your mental slate is clear. Rest and recover your biological battery."*

---

## 3. Caveats & Risks

| Category | Risk | Mitigation |
|---|---|---|
| **Room Schema & Migrations** | Adding `ICEBOX` to `TaskStatus` enum could cause migration mismatch if Room validates enum constants. | In Room, `TaskEntity.status` is mapped to SQLite `TEXT NOT NULL` via `Converters`. Room's schema json (`21.json`) only records affinity `TEXT`. Enum additions do NOT alter table structure or schema hash. `toTaskStatus` has try/catch fallback to `ACTIVE`. Database version remains 21 without migration. |
| **Undo State Timing** | Fast multi-tap completions could overwrite pending undo jobs or leave orphaned dismissals. | Key `pendingUndoAction` with unique job tracking; cancelling prior job before enqueuing a new one, or supporting individual dismiss set cleanup on coroutine completion. |
| **Performance in Combine** | `TaskScoringEngine.rankTasks` on large task lists might stutter if executed on the Main thread. | Keep scoring computation inside `FlowViewModelV2`'s `combine` block running on `Dispatchers.Default` via `SharingStarted.WhileSubscribed(5000)`. |
| **Clock Boundary Recomposition** | Transitioning across 19:00 (7 PM) or midnight while app is open without triggering recomposition. | Add a minute-tick flow (as in `TodayViewModel.kt`) or evaluate `nowMillis` on foreground resume to update `showEveningShutdown`. |

---

## 4. Conclusion & Implementation Recommendations for Worker

### Concrete Implementation Tasks

#### Task 1: Add `ICEBOX` to `TaskStatus` Enum & Repository Helpers
1. Edit `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`:
   ```kotlin
   enum class TaskStatus {
       ACTIVE,
       COMPLETED,
       ARCHIVED,
       ICEBOX
   }
   ```
2. In `TaskRepository.kt`:
   - Add `suspend fun rolloverToIcebox(tasks: List<TaskEntity>)` which sets `status = TaskStatus.ICEBOX`, `scheduledDate = null`, `scheduledTime = null`, `updatedAt = now`.
   - Add `fun observeIceboxTasks(): Flow<List<TaskEntity>> = taskDao.observeByStatus(TaskStatus.ICEBOX)`.
3. In `TaskDao.kt`:
   - Existing `observeByStatus(status: TaskStatus)` already handles `TaskStatus.ICEBOX`.

#### Task 2: Create `BreakItDownEngine`
Create `app/src/main/java/com/neuroflow/app/domain/engine/BreakItDownEngine.kt`:
```kotlin
package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.focus.StepByStepPlanCodec
import com.neuroflow.app.presentation.focus.StepByStepPlanStep
import java.util.Locale

object BreakItDownEngine {
    fun generateMicroSteps(task: TaskEntity): List<StepByStepPlanStep> {
        val title = task.title.trim()
        val lower = title.lowercase(Locale.ROOT)

        val steps = when {
            lower.contains("tax") || lower.contains("finance") || lower.contains("invoice") || lower.contains("pay") -> listOf(
                "Gather receipts, portal login, and relevant documents",
                "Open form and complete the first required section",
                "Verify calculations and download confirmation"
            )
            lower.contains("code") || lower.contains("bug") || lower.contains("refactor") || lower.contains("test") -> listOf(
                "Open IDE, checkout branch, and locate the primary class",
                "Write a failing unit test or reproduce the exact issue",
                "Draft the first 5 lines of implementation or patch"
            )
            lower.contains("write") || lower.contains("draft") || lower.contains("doc") || lower.contains("report") -> listOf(
                "Open blank document and write the working title",
                "Jot down 3 bullet points outlining the core argument",
                "Draft the opening paragraph without editing"
            )
            lower.contains("email") || lower.contains("reply") || lower.contains("message") -> listOf(
                "Open email client and find recipient thread",
                "Draft a 1-to-2 sentence direct response or question",
                "Review clarity, attach files, and send"
            )
            lower.contains("clean") || lower.contains("organize") || lower.contains("desk") -> listOf(
                "Clear one small flat surface or grab a single bin",
                "Set a 5-minute timer and pick up first 3 items",
                "Wipe down the cleared space and pause"
            )
            task.taskType == TaskType.ADMIN -> listOf(
                "Open necessary website or app and log in",
                "Complete the easiest preliminary input field",
                "Review remaining details and submit"
            )
            task.taskType == TaskType.CREATIVE -> listOf(
                "Set up canvas or document and eliminate distractions",
                "Spend 3 minutes brainstorming wild concepts without filter",
                "Pick the most promising direction and sketch the structure"
            )
            else -> listOf(
                "Open workspace and assemble tools needed for \"$title\"",
                "Spend 3 minutes on the easiest single action",
                "Evaluate momentum and decide next step"
            )
        }

        return steps.map { StepByStepPlanStep(it, completed = false) }
    }

    fun applyBreakDown(task: TaskEntity): TaskEntity {
        val microSteps = generateMicroSteps(task)
        val serialized = StepByStepPlanCodec.serialize(microSteps)
        return task.copy(
            ifThenPlan = serialized,
            updatedAt = System.currentTimeMillis()
        )
    }
}
```

#### Task 3: Enhance `FlowViewModelV2`
Update `FlowViewModelV2.kt`:
1. Add `UndoAction` state management:
   - `private val pendingDismissIds = MutableStateFlow<Set<String>>(emptySet())`
   - `private val pendingUndoActionState = MutableStateFlow<UndoAction?>(null)`
   - `completeTaskWithUndo(task: TaskEntity)` (4000ms delay coroutine)
   - `deferTaskWithUndo(task: TaskEntity, hours: Int = 2)` (4000ms delay coroutine)
   - `undo()` (cancels job, restores task ID)
2. Add Overdue Detection & Zero-Guilt Rollover:
   - Calculate `overdueTasks = activeTasks.filter { (it.scheduledDate != null && it.scheduledDate < todayStart) || (it.deadlineDate != null && it.deadlineDate < todayStart) }`
   - Expose `showZeroGuiltFreshStart = overdueTasks.size > 3`
   - `rolloverOverdueToIcebox()` -> calls `taskRepository.rolloverToIcebox(overdueTasks)`
3. Connect `TaskScoringEngine.rankTasks`:
   - Compute `LiveEnergyContext` from `energyUi`.
   - Score and rank active tasks, extracting `recommendedTask` and `recommendationReason`.
4. Add "Break It Down" Action:
   - `breakDownTask(task: TaskEntity)` -> calls `BreakItDownEngine.applyBreakDown(task)` and updates repository.
5. Add Evening Shutdown Logic:
   - Calculate `showEveningShutdown = currentHour >= 19`
   - `pushRemainingToTomorrow()` -> reschedules remaining today tasks to tomorrow midnight.
   - `confirmEveningShutdown()` -> updates preference or session state for shutdown completion.

#### Task 4: UI Components Modernization
1. **FlowScreenV2.kt**:
   - Add `SnackbarHost(snackbarHostState)` inside `Scaffold`.
   - `LaunchedEffect(state.pendingUndoAction)` showing snackbar with `"UNDO"` button.
   - Separate layout into:
     1. Top App Bar (Title + Settings button)
     2. `CircadianHeroBarometer` (curves + bio-battery)
     3. `RecommendedNextActionHeroCard` (single prominent hero card with reason badge, 1-tap focus, wand button)
     4. `ZeroGuiltFreshStartCard` (appears when `showZeroGuiltFreshStart == true`)
     5. `EveningShutdownCard` (appears when `showEveningShutdown == true`)
     6. `EnergyTimelineStream` (timeline stream with calm indicators, wand button, micro-steps checklist preview)
2. **RecommendedNextActionHeroCard.kt**:
   - Dedicated component in `presentation/v2/flow/`.
   - High visual prominence: SlateSurfaceAlt container, ElectricIndigo accent, 48dp touch targets.
3. **EnergyTimelineStream.kt**:
   - Calibrate overdue indicators to use `NeuroFlowThemeV2Colors.WarmAmber` or `SlateSurfaceAlt` with zero alarmist red text or badges.
   - Add Magic Wand icon button (`Icons.Default.AutoAwesome`) on each card.
   - Render expandable micro-steps checklist if `task.ifThenPlan` is present.

---

## 5. Verification Method

### 5.1 Automated Unit Tests
Run `./gradlew testDebugUnitTest` to verify test suite health.
Add new unit tests:
1. `BreakItDownEngineTest.kt`:
   - Verify Analytical, Admin, Writing, Financial, and default titles generate exactly 3 non-empty `StepByStepPlanStep`s.
   - Verify serialization into `ifThenPlan` parses correctly with `StepByStepPlanCodec`.
2. `FlowViewModelV2UndoTest.kt`:
   - Verify `completeTaskWithUndo` hides task immediately from UI state.
   - Verify tapping `undo()` before 4 seconds aborts the repository completion.
   - Verify waiting past 4 seconds invokes `completeAndRecur`.
3. `ZeroGuiltFreshStartTest.kt`:
   - Verify 4 overdue tasks trigger `showZeroGuiltFreshStart == true`.
   - Verify `rolloverOverdueToIcebox` sets all overdue tasks to `TaskStatus.ICEBOX` and clears scheduled dates.
   - Verify `observeActiveTasks()` excludes ICEBOX tasks.
4. `EveningShutdownTimeTest.kt`:
   - Verify `showEveningShutdown` is false before 19:00 and true at/after 19:00.

### 5.2 Manual UI Verification Checklist
- [ ] Overdue tasks display calm Warm Amber / Slate badges; no `#C62828` or alarmist red warnings.
- [ ] Flow screen displays Circadian Barometer -> Recommended Next Action hero card -> Timeline stream cleanly.
- [ ] Tapping "Break It Down" magic wand instantly displays 3 micro-steps under the task.
- [ ] Completing or deferring a task shows the floating 4-second undo snackbar with instant recovery.
- [ ] After 7:00 PM (19:00), the Evening Shutdown card appears with 1-tap "Push Remaining".
