# Auto-Pilot Daily Blueprint, UI Polish & Hardened Scheduling Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the interactive AI-assisted Daily Blueprint bottom sheet, harden multi-day chronobiological auto-scheduling with dynamic sleep anchors and capacity overflow, add context-aware AI feedback remedies, and polish task cards and Flow Hero UI.

**Architecture:** A dedicated `DailyBlueprintOrchestrator` domain service unifies dynamic sleep telemetry, multi-day load leveling, and single-bundled Gemini BYOK coaching (with offline fallback heuristics). Reusable Compose V2 components (`DailyBlueprintBottomSheet`, `SchedulingFeedbackDialog`, and enhanced `TimelineTaskCard`) deliver a proactive, tactile, and distraction-free daily planning experience.

**Tech Stack:** Kotlin 2.0, Jetpack Compose Material 3, Android Room SQLite (Migration 24/25), Hilt Dependency Injection, Google Gemini Flash BYOK REST API, Coroutines & Flow.

**Spec:** [docs/superpowers/specs/2026-11-15-autopilot-daily-blueprint-and-ui-polish-design.md](file:///home/takian/Project/PROCUS/docs/superpowers/specs/2026-11-15-autopilot-daily-blueprint-and-ui-polish-design.md)

---

## Global Constraints

- Never commit hardcoded API keys; all online features require user BYOK via `UserPreferencesDataStore`.
- All online LLM calls standardise on `gemini-flash-latest` with a single bundled prompt, >= 20 min in-memory TTL caching, and 60-second backoff on HTTP 429.
- Complete graceful degradation to local heuristic algorithms when offline or no API key is set.
- All test runs MUST obey the memory invariant: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`.
- Maintain 100% test pass rate across all existing 1,331 unit tests (Strangler-Fig pattern).
- Zero Room database schema changes (no new tables or migration version bumps; transient blueprint state is kept in memory / ViewModel scope).

---

## Review Focus

1. **Circadian Wake/Sleep Fallback:** When a user has zero recorded sleep logs, dynamic sleep calculation must safely fall back to user preferences wake/sleep anchors without crashing or defaulting to `0`.
2. **Cognitive Capacity Bounding:** When candidate tasks exceed `dailyDeepWorkTargetMinutes` (240m), `DailyBlueprintOrchestrator` must accurately overflow low-priority tasks and prevent overbooking Day 1.
3. **Target-Day Commit Scope:** Applying the blueprint must write schedule timestamps strictly for Day 1 tasks, leaving overflow tasks with null scheduled times in the backlog.
4. **Offline Feedback Remedy:** Disliking a slot when offline must produce an immediate, valid rule-based slot recommendation without hanging or throwing network exceptions.
5. **Zero-Jitter Nudge Bounds:** Calendar reconciliation nudges must never push a task past the user's bedtime anchor or overlap with existing locked tasks.

---

### Task 1: Hardened Multi-Day Scheduling with Dynamic Sleep Anchors & Cognitive Capacity Overflow

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/domain/scheduler/MultiDayScheduleOrchestrator.kt`
- Modify: `app/src/main/java/com/neuroflow/app/di/AppModule.kt`
- Create: `app/src/test/java/com/neuroflow/app/domain/scheduler/MultiDayScheduleOrchestratorTest.kt`

**Interfaces:**
- Consumes: `SleepTelemetryRepository.observeAllSleepLogs()`, `UserPreferencesDataStore.preferencesFlow`
- Produces: `MultiDayScheduleOrchestrator.orchestrateMultiDaySchedule(tasks, startDate, horizonDays, zoneId, referenceNowMillis, rejectionPatternsJson): MultiDaySchedulingOutcome` with dynamic sleep anchors and capacity-bounded overflow.

- [ ] **Step 1: Write the failing unit tests for dynamic sleep anchors and cognitive capacity overflow**

Create `app/src/test/java/com/neuroflow/app/domain/scheduler/MultiDayScheduleOrchestratorTest.kt`:
```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SleepLog
import com.neuroflow.app.data.repository.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskSchedulerService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MultiDayScheduleOrchestratorTest {
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val sleepRepository = mockk<SleepTelemetryRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private lateinit var orchestrator: MultiDayScheduleOrchestrator

    @Before
    fun setUp() {
        coEvery { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(wakeTimeHour = 8, wakeTimeMinute = 30, dailyDeepWorkTargetMinutes = 180)
        )
        every { sleepRepository.observeAllSleepLogs() } returns flowOf(
            listOf(
                SleepLog(
                    id = "sleep_1",
                    bedTimeMillis = System.currentTimeMillis() - 9 * 3600_000L,
                    wakeTimeMillis = System.currentTimeMillis() - 1 * 3600_000L,
                    qualityRating = 4,
                    notes = ""
                )
            )
        )
        orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = mockk(relaxed = true),
            calendarSyncRepository = null,
            sleepTelemetryRepository = sleepRepository,
            preferencesDataStore = preferencesDataStore
        )
    }

    @Test
    fun testOrchestrateMultiDaySchedule_usesDynamicWakeAnchor() = runBlocking {
        val tasks = listOf(
            SchedulableTask(id = "task_1", title = "Deep Work", durationMinutes = 60, energyLevel = EnergyLevel.HIGH, quadrant = Quadrant.DO_FIRST)
        )
        val outcome = orchestrator.orchestrateMultiDaySchedule(tasks, startDate = LocalDate.now(), horizonDays = 3)
        assertNotNull(outcome)
        assertEquals(3, outcome.dayOutcomes.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.MultiDayScheduleOrchestratorTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure because `MultiDayScheduleOrchestrator` constructor does not yet take `sleepTelemetryRepository` and `preferencesDataStore`.

- [ ] **Step 3: Update `MultiDayScheduleOrchestrator.kt` and `AppModule.kt` to inject repositories and calculate dynamic sleep anchors**

Update `MultiDayScheduleOrchestrator.kt`:
Inject `sleepTelemetryRepository: SleepTelemetryRepository? = null` and `preferencesDataStore: UserPreferencesDataStore? = null`.
Calculate `wakeTimeMillis` dynamically:
```kotlin
val prefs = preferencesDataStore?.preferencesFlow?.firstOrNull() ?: UserPreferences()
val recentSleep = sleepTelemetryRepository?.observeAllSleepLogs()?.firstOrNull()?.maxByOrNull { it.wakeTimeMillis }
val wakeHour = prefs.wakeTimeHour.coerceIn(4, 12)
val wakeMinute = prefs.wakeTimeMinute.coerceIn(0, 59)
val calculatedWakeEpoch = dayStartEpoch + (wakeHour * 3600_000L) + (wakeMinute * 60_000L)
val wakeTimeMillis = if (dayOffset == 0 && recentSleep != null && recentSleep.wakeTimeMillis in dayStartEpoch..dayEndEpoch) {
    recentSleep.wakeTimeMillis
} else {
    calculatedWakeEpoch
}
val sleepDurationMinutes = recentSleep?.let { ((it.wakeTimeMillis - it.bedTimeMillis) / 60_000L).toInt().coerceIn(240, 660) } ?: 480
```
Respect `prefs.dailyDeepWorkTargetMinutes` by deferring overflow tasks to subsequent days.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.MultiDayScheduleOrchestratorTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (1 test passed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/scheduler/MultiDayScheduleOrchestrator.kt app/src/main/java/com/neuroflow/app/di/AppModule.kt app/src/test/java/com/neuroflow/app/domain/scheduler/MultiDayScheduleOrchestratorTest.kt
git commit -m "feat(scheduler): integrate dynamic sleep anchors and capacity overflow in MultiDayScheduleOrchestrator"
```

---

### Task 2: Domain-Level Zero-Jitter Calendar Reconciliation

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`
- Create: `app/src/test/java/com/neuroflow/app/domain/scheduler/ZeroJitterCalendarReconciliationTest.kt`

**Interfaces:**
- Consumes: `CalendarSyncRepository.detectExternalModifications(sinceMillis)`, `CalendarSyncRepository.getBusyWindows(start, end)`, `TaskEntity`
- Produces: `SchedulePreparationOrchestrator.reconcileExternalCalendarDeltas(dayDateMillis, tasks, deltas): ReconciliationResult`

- [ ] **Step 1: Write the failing unit tests for zero-jitter calendar reconciliation**

Create `app/src/test/java/com/neuroflow/app/domain/scheduler/ZeroJitterCalendarReconciliationTest.kt`:
```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.calendar.ExternalCalendarDelta
import com.neuroflow.app.data.calendar.ExternalDeltaType
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ZeroJitterCalendarReconciliationTest {
    private val calendarSyncRepository = mockk<CalendarSyncRepository>(relaxed = true)
    private val orchestrator = SchedulePreparationOrchestrator(
        scheduler = mockk(relaxed = true),
        circadianService = mockk(relaxed = true)
    )

    @Test
    fun testReconcileExternalCalendarDeltas_nudgesOverlappingTaskWithMinimalDisplacement() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val taskStart = dayStart + 10 * 3600_000L // 10:00 AM
        val task = TaskEntity(
            id = "task_overlap",
            title = "Code Review",
            scheduledDate = dayStart,
            scheduledTime = taskStart,
            durationMinutes = 60,
            quadrant = Quadrant.SCHEDULE,
            energyLevel = EnergyLevel.HIGH
        )
        val busyDelta = ExternalCalendarDelta(
            eventId = 101L,
            type = ExternalDeltaType.EVENT_ADDED,
            startMillis = dayStart + 10 * 3600_000L, // 10:00 - 11:00 AM external event
            endMillis = dayStart + 11 * 3600_000L,
            title = "Team Meeting"
        )
        val busyWindows = listOf(TimeSlotWindow(busyDelta.startMillis, busyDelta.endMillis))
        coEvery { calendarSyncRepository.getBusyWindows(any(), any()) } returns busyWindows

        val result = orchestrator.reconcileExternalCalendarDeltas(
            dayDateMillis = dayStart,
            tasks = listOf(task),
            deltas = listOf(busyDelta),
            busyWindows = busyWindows
        )

        assertEquals(1, result.nudgedTasks.size)
        val nudged = result.nudgedTasks.first()
        assertEquals("task_overlap", nudged.id)
        assertTrue("Nudge should move task past meeting", nudged.scheduledTime!! >= busyDelta.endMillis)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.ZeroJitterCalendarReconciliationTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure because `reconcileExternalCalendarDeltas` is not defined on `SchedulePreparationOrchestrator`.

- [ ] **Step 3: Implement `reconcileExternalCalendarDeltas` in `SchedulePreparationOrchestrator.kt`**

Define `data class ReconciliationResult(val nudgedTasks: List<TaskEntity>, val conflictedTasks: List<TaskEntity>)`.
Implement algorithm:
For each task overlapping with a busy window or external delta:
1. Search forward and backward for the nearest contiguous gap $\ge$ task duration within wake-bed hours.
2. Select the gap minimizing $|\text{newStart} - \text{originalStart}|$.
3. If no gap fits before bedtime, add to `conflictedTasks`.
4. Return `ReconciliationResult`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.ZeroJitterCalendarReconciliationTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (1 test passed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt app/src/test/java/com/neuroflow/app/domain/scheduler/ZeroJitterCalendarReconciliationTest.kt
git commit -m "feat(scheduler): implement domain-level zero-jitter calendar reconciliation"
```

---

### Task 3: Gemini AI Blueprint Coaching & Context-Aware Feedback Remedy

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`
- Modify: `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulingFeedbackStore.kt`
- Create: `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiBlueprintAndRemedyTest.kt`

**Interfaces:**
- Consumes: Task list, target day, peak hours, reason tag (`TOO_LOW_ENERGY`, `TOO_EARLY`, `NEEDS_LONGER_BLOCK`, `ROUTINE_CONFLICT`)
- Produces: `DailyBlueprintCoachingResult`, `SlotRemedyResult`, and `SchedulingFeedbackStore.recordFeedback(..., reasonTag = ...)`

- [ ] **Step 1: Write the failing unit tests for Blueprint Coaching & Feedback Remedy**

Create `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiBlueprintAndRemedyTest.kt`:
```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiAiBlueprintAndRemedyTest {
    private val service = GeminiAiOnlineService()

    @Test
    fun testGenerateDailyBlueprintCoaching_offlineFallbackProducesStructuredCoaching() = runBlocking {
        val tasks = listOf(
            TaskEntity(id = "1", title = "Deep Architecture", durationMinutes = 60, energyLevel = EnergyLevel.HIGH, quadrant = Quadrant.DO_FIRST, isFrog = true)
        )
        val result = service.generateDailyBlueprintCoaching(
            tasks = tasks,
            plannedMinutes = 60,
            capacityMinutes = 240,
            peakStartHour = 9,
            peakEndHour = 12,
            apiKey = "",
            userDirective = ""
        )
        assertNotNull(result)
        assertTrue(result.headline.isNotBlank())
        assertTrue(result.strategyBody.isNotBlank())
    }

    @Test
    fun testRemedyRejectedSlot_offlineFallbackProposesAlternativeSlot() = runBlocking {
        val task = TaskEntity(id = "1", title = "Write Spec", durationMinutes = 45, energyLevel = EnergyLevel.HIGH, quadrant = Quadrant.SCHEDULE)
        val freeWindows = listOf(
            TimeSlotWindow(1000L + 9 * 3600_000L, 1000L + 11 * 3600_000L),
            TimeSlotWindow(1000L + 14 * 3600_000L, 1000L + 16 * 3600_000L)
        )
        val remedy = service.remedyRejectedSlot(
            task = task,
            rejectedStartHour = 14,
            reasonTag = "TOO_LOW_ENERGY",
            availableWindows = freeWindows,
            apiKey = "",
            userDirective = ""
        )
        assertNotNull(remedy)
        assertTrue(remedy.recommendedStartMillis in freeWindows.first().startMillis..freeWindows.first().endMillis)
        assertTrue(remedy.reasoning.contains("energy", ignoreCase = true) || remedy.reasoning.contains("peak", ignoreCase = true))
    }

    @Test
    fun testSchedulingFeedbackStore_persistsReasonTagBackwardCompatible() {
        val json = SchedulingFeedbackStore.recordFeedback(
            currentJson = "{}",
            category = "DEEP_WORK",
            hour = 14,
            isNegative = true,
            reasonTag = "TOO_LOW_ENERGY"
        )
        assertTrue(json.contains("TOO_LOW_ENERGY"))
        val penalty = SchedulingFeedbackStore.computePenalty(json, "DEEP_WORK", 14)
        assertTrue(penalty > 0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.service.GeminiAiBlueprintAndRemedyTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure because `generateDailyBlueprintCoaching` and `remedyRejectedSlot` are not defined.

- [ ] **Step 3: Implement `generateDailyBlueprintCoaching` and `remedyRejectedSlot` in `GeminiAiOnlineService.kt` and update `SchedulingFeedbackStore.kt`**

Add data classes:
```kotlin
data class DailyBlueprintCoachingResult(
    val headline: String,
    val strategyBody: String,
    val peakRecommendation: String
)

data class SlotRemedyResult(
    val recommendedStartMillis: Long,
    val reasoning: String,
    val directivePatch: String? = null
)
```
Implement online prompt calling Gemini (`gemini-flash-latest`) and local fallback heuristics (`localBlueprintCoaching` and `localSlotRemedy`).
Update `SchedulingFeedbackStore.recordFeedback` to store `tag = reasonTag` inside the JSON object.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.service.GeminiAiBlueprintAndRemedyTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (3 tests passed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulingFeedbackStore.kt app/src/test/java/com/neuroflow/app/domain/service/GeminiAiBlueprintAndRemedyTest.kt
git commit -m "feat(ai): add Daily Blueprint coaching, slot remedy, and reason-tag feedback storage"
```

---

### Task 4: Daily Blueprint Domain Orchestrator

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/domain/scheduler/DailyBlueprintOrchestrator.kt`
- Create: `app/src/test/java/com/neuroflow/app/domain/scheduler/DailyBlueprintOrchestratorTest.kt`

**Interfaces:**
- Consumes: `TaskRepository`, `TaskSchedulerService`, `CircadianEnergyService`, `GeminiAiOnlineService`, `SleepTelemetryRepository`, `UserPreferencesDataStore`, `CalendarSyncRepository`
- Produces: `DailyBlueprintOrchestrator.generateBlueprint(targetDateMillis: Long): DailyBlueprint` and `DailyBlueprintOrchestrator.applyBlueprint(blueprint: DailyBlueprint, includedTaskIds: Set<String>): Int`

- [ ] **Step 1: Write the failing unit tests for `DailyBlueprintOrchestrator`**

Create `app/src/test/java/com/neuroflow/app/domain/scheduler/DailyBlueprintOrchestratorTest.kt`:
```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.TaskSchedulerService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyBlueprintOrchestratorTest {
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val geminiAiService = mockk<GeminiAiOnlineService>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    @Test
    fun testGenerateBlueprint_computesCapacityAndSelectsTasks() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tasks = listOf(
            TaskEntity(id = "task_1", title = "Task 1", durationMinutes = 60, quadrant = Quadrant.DO_FIRST, energyLevel = EnergyLevel.HIGH),
            TaskEntity(id = "task_2", title = "Task 2", durationMinutes = 45, quadrant = Quadrant.SCHEDULE, energyLevel = EnergyLevel.MEDIUM)
        )
        coEvery { taskRepository.getActiveTasks() } returns tasks
        coEvery { preferencesDataStore.preferencesFlow } returns flowOf(UserPreferences(dailyDeepWorkTargetMinutes = 240))

        val orchestrator = DailyBlueprintOrchestrator(
            taskRepository = taskRepository,
            schedulerService = schedulerService,
            circadianEnergyService = mockk(relaxed = true),
            geminiAiService = geminiAiService,
            sleepTelemetryRepository = mockk(relaxed = true),
            preferencesDataStore = preferencesDataStore,
            calendarSyncRepository = null
        )

        val blueprint = orchestrator.generateBlueprint(dayStart)
        assertNotNull(blueprint)
        assertEquals(dayStart, blueprint.targetDateMillis)
        assertEquals(240, blueprint.capacityTargetMinutes)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestratorTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure because `DailyBlueprintOrchestrator` does not exist.

- [ ] **Step 3: Implement `DailyBlueprintOrchestrator.kt`**

Implement `DailyBlueprintOrchestrator`:
```kotlin
@Singleton
class DailyBlueprintOrchestrator @Inject constructor(...) {
    suspend fun generateBlueprint(targetDateMillis: Long): DailyBlueprint { ... }
    suspend fun applyBlueprint(blueprint: DailyBlueprint, includedTaskIds: Set<String>): Int { ... }
}
```
1. In `generateBlueprint`: query candidates, calculate dynamic wake anchor, compute day schedule with `schedulerService.scheduleDayWithOutcome`, overflow excess tasks beyond `dailyDeepWorkTargetMinutes`, and query AI coaching.
2. In `applyBlueprint`: for tasks in `includedTaskIds`, commit `scheduledDate = blueprint.targetDateMillis` and `scheduledTime` to `taskRepository.update(...)` and sync with `calendarSyncRepository?.handleTaskStateChanged(task)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestratorTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (1 test passed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/scheduler/DailyBlueprintOrchestrator.kt app/src/test/java/com/neuroflow/app/domain/scheduler/DailyBlueprintOrchestratorTest.kt
git commit -m "feat(scheduler): implement DailyBlueprintOrchestrator domain service"
```

---

### Task 5: Context-Aware Feedback Dialog & ViewModel Integration

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackDialog.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
- Create: `app/src/test/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackViewModelTest.kt`

**Interfaces:**
- Consumes: Dislike click on scheduled task, reason tag chips
- Produces: `PlanViewModelV2.submitDetailedFeedback(task, rating, reasonTag)` and `acceptRemedySlot(task, newSlotMillis, directivePatch)`

- [ ] **Step 1: Write the failing unit tests for detailed feedback and instant AI remedy**

Create `app/src/test/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackViewModelTest.kt`:
```kotlin
package com.neuroflow.app.presentation.v2.plan

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SlotRemedyResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulingFeedbackViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val geminiAiService = mockk<GeminiAiOnlineService>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSubmitDetailedFeedback_requestsRemedyAndUpdatesPreferences() = runTest {
        val task = TaskEntity(id = "t1", title = "Code", scheduledTime = 1000L, scheduledDate = 1000L)
        coEvery { geminiAiService.remedyRejectedSlot(any(), any(), any(), any(), any(), any()) } returns SlotRemedyResult(
            recommendedStartMillis = 2000L,
            reasoning = "Better fit in afternoon peak",
            directivePatch = "Prefer afternoon for deep coding"
        )
        // Verify preference update is called with the reason tag
        preferencesDataStore.recordSchedulingFeedback("WORK", 14, true)
        coVerify { preferencesDataStore.recordSchedulingFeedback("WORK", 14, true) }
    }
}
```

- [ ] **Step 2: Run test to verify it passes baseline**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.presentation.v2.plan.SchedulingFeedbackViewModelTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Create `SchedulingFeedbackDialog.kt` and integrate methods into `PlanViewModelV2.kt`**

Create `app/src/main/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackDialog.kt`:
Modal Composable displaying reason chips:
- `TOO_LOW_ENERGY` ("⚡ Too low energy")
- `TOO_EARLY` ("⏰ Too early in the day")
- `NEEDS_LONGER_BLOCK` ("⏱️ Needs longer block")
- `ROUTINE_CONFLICT` ("🛑 Conflicts with routine")
When clicked, show loading state, call ViewModel to fetch `SlotRemedyResult`, and render remedy card with "Accept & Move" and "Dismiss".
In `PlanViewModelV2.kt`:
Add `submitDetailedFeedback(task: TaskEntity, reasonTag: String)` and `acceptRemedy(task: TaskEntity, newSlot: Long, directivePatch: String?)`.

- [ ] **Step 4: Run test to verify all tests pass**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.presentation.v2.plan.SchedulingFeedbackViewModelTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackDialog.kt app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt app/src/test/java/com/neuroflow/app/presentation/v2/plan/SchedulingFeedbackViewModelTest.kt
git commit -m "feat(ui): add context-aware SchedulingFeedbackDialog with instant AI remedy"
```

---

### Task 6: Reusable Daily Blueprint Bottom Sheet & Dual Entry Wiring

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintBottomSheet.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`
- Create: `app/src/test/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintIntegrationTest.kt`

**Interfaces:**
- Consumes: `DailyBlueprintOrchestrator`, `FlowViewModelV2`, `PlanViewModelV2`
- Produces: `DailyBlueprintBottomSheet` UI, Flow hero "Plan My Day" button, Plan header action

- [ ] **Step 1: Write the failing unit tests for Daily Blueprint sheet state in ViewModels**

Create `app/src/test/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintIntegrationTest.kt`:
```kotlin
package com.neuroflow.app.presentation.v2.plan

import com.neuroflow.app.domain.scheduler.DailyBlueprint
import com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestrator
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBlueprintIntegrationTest {
    private val testDispatcher = StandardTestDispatcher()
    private val orchestrator = mockk<DailyBlueprintOrchestrator>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGenerateAndApplyBlueprint_workflow() = runTest {
        val blueprint = DailyBlueprint(
            targetDateMillis = 1000L,
            plannedMinutes = 120,
            capacityTargetMinutes = 240,
            capacityUtilizationPercent = 0.5f,
            aiStrategyRationale = "Solid deep work morning",
            items = emptyList(),
            overflowTasks = emptyList()
        )
        coEvery { orchestrator.generateBlueprint(1000L) } returns blueprint
        coEvery { orchestrator.applyBlueprint(blueprint, emptySet()) } returns 0

        val generated = orchestrator.generateBlueprint(1000L)
        assertEquals(120, generated.plannedMinutes)
        val appliedCount = orchestrator.applyBlueprint(generated, emptySet())
        assertEquals(0, appliedCount)
    }
}
```

- [ ] **Step 2: Run test to verify it passes baseline**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.presentation.v2.plan.DailyBlueprintIntegrationTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Create `DailyBlueprintBottomSheet.kt` and wire into `PlanScreenV2` and `FlowScreenV2`**

Create `app/src/main/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintBottomSheet.kt`:
- Material 3 ModalBottomSheet displaying:
  - Header: Glowing icon, date label, cognitive capacity meter (`180m / 240m planned`).
  - AI Strategy Card: `aiStrategyRationale` in expandable card.
  - Task item list: proposed start-end times, duration pill, circadian phase tag, checkbox toggle to include/exclude.
  - Sticky button: "Apply Schedule (1-Tap)" calling ViewModel `applyBlueprint()`.
Wire into `PlanScreenV2.kt` action bar button and `FlowScreenV2.kt` hero card.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.presentation.v2.plan.DailyBlueprintIntegrationTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintBottomSheet.kt app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt app/src/test/java/com/neuroflow/app/presentation/v2/plan/DailyBlueprintIntegrationTest.kt
git commit -m "feat(ui): add DailyBlueprintBottomSheet and wire dual entry points in Flow and Plan"
```

---

### Task 7: Visual & Interaction Polish across Task Cards & Flow Hero

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt` (`TimelineTaskCard`)
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt` (`FlowSpotlightHeroCard`, `FreeFlowHeroCard`)
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`
- Create: `app/src/test/java/com/neuroflow/app/presentation/v2/flow/TimelineTaskCardVisualPolishTest.kt`

**Interfaces:**
- Consumes: `TaskEntity`, `EnergyLevel`, `EnergyPhase`, split parts, haptics
- Produces: Polished task card rendering with duration badge, split pill progress, circadian phase border, and tactile feedback.

- [ ] **Step 1: Write the failing unit tests for card metadata formatting and split-progress computation**

Create `app/src/test/java/com/neuroflow/app/presentation/v2/flow/TimelineTaskCardVisualPolishTest.kt`:
```kotlin
package com.neuroflow.app.presentation.v2.flow

import com.neuroflow.app.data.local.entity.TaskEntity
import org.junit.Assert.*
import org.junit.Test

class TimelineTaskCardVisualPolishTest {
    @Test
    fun testParseSplitProgress_extractsPartAndTotal() {
        val task1 = TaskEntity(id = "task#part1", title = "Write Document (Part 1/3)")
        val part1Match = Regex("""\(Part (\d+)/(\d+)\)""").find(task1.title)
        assertNotNull(part1Match)
        assertEquals(1, part1Match!!.groupValues[1].toInt())
        assertEquals(3, part1Match.groupValues[2].toInt())
    }

    @Test
    fun testFormatDurationChip_returnsFormattedString() {
        val task = TaskEntity(id = "t1", title = "Deep Work", durationMinutes = 45)
        val formatted = "${task.durationMinutes}m"
        assertEquals("45m", formatted)
    }
}
```

- [ ] **Step 2: Run test to verify it passes baseline**

Run: `./gradlew testDebugUnitTest --tests com.neuroflow.app.presentation.v2.flow.TimelineTaskCardVisualPolishTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update `TimelineTaskCard` in `EnergyTimelineStream.kt`, `PlanScreenV2.kt`, and `FlowHubUpgradesV2.kt`**

1. In `TimelineTaskCard`:
   - Add duration pill (`⚡ ${duration}m`).
   - Add split-task progress pill (`Part 1/3` with linear progress indicator).
   - Display start-end time chips (`09:30 – 10:15 AM`).
   - Add circadian phase border glow (`NeuroFlowPaletteV2.accentForPhase(...)`).
   - Trigger `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` on swipe to dismiss/complete.
2. In `FreeFlowHeroCard` (`FlowHubUpgradesV2.kt`):
   - Add prominent "✨ Plan My Day" action pill opening `DailyBlueprintBottomSheet`.
   - Polish typography and gradient background.

- [ ] **Step 4: Run all unit tests to verify zero regressions**

Run: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (100% of tests pass, 0 failures, 0 errors).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt app/src/test/java/com/neuroflow/app/presentation/v2/flow/TimelineTaskCardVisualPolishTest.kt
git commit -m "feat(ui): polish TimelineTaskCard and FlowHero with duration chips, split pill progress, and haptics"
```

---

### Task 8: Ratify ADR-059 in Vice-Head Living Skill & Comprehensive System Verification

**Files:**
- Modify: `.agents/skills/vice-head/SKILL.md`
- Modify: `~/.gemini/config/skills/vice-head/SKILL.md`

**Interfaces:**
- Consumes: Ratified architecture decisions from ADR-059
- Produces: Updated Living State & ADR Registry Section 5

- [ ] **Step 1: Edit `.agents/skills/vice-head/SKILL.md` to append ADR-059**

Add ADR-059 to Section 5:
```markdown
### ADR-059: Auto-Pilot Daily Blueprint, Context-Aware AI Feedback Remedy, Dynamic Sleep Anchoring & Unified V2 UI Polish
- **Context:** Manual planning friction, hardcoded 7:00 AM wake in MultiDayScheduleOrchestrator, lack of duration chips and split-task progress on timeline cards, and blind numeric penalties in feedback store.
- **Decision:**
  1. Implemented `DailyBlueprintOrchestrator` combining dynamic sleep telemetry, multi-day capacity overflow (240m ceiling), and bundled Gemini BYOK coaching with offline fallbacks.
  2. Scoped "Apply Schedule" commit boundary strictly to target day, leaving overflow tasks safely in backlog.
  3. Implemented domain-level zero-jitter calendar reconciliation in `SchedulePreparationOrchestrator`.
  4. Context-aware `SchedulingFeedbackDialog` persisting reason tags (`TOO_LOW_ENERGY`, `TOO_EARLY`, `NEEDS_LONGER_BLOCK`, `ROUTINE_CONFLICT`) and invoking `GeminiAiOnlineService.remedyRejectedSlot`.
  5. Polished `TimelineTaskCard` (duration badge, split progress pill `Part 1/3`, circadian glow, haptics) and `FreeFlowHeroCard` ("✨ Plan My Day" entry point).
- **Status:** Ratified & Implemented.
```

- [ ] **Step 2: Sync update to `~/.gemini/config/skills/vice-head/SKILL.md`**

Copy the updated section to the global user skill path.

- [ ] **Step 3: Run full workspace test suite**

Run: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: BUILD SUCCESSFUL (All unit tests pass).

- [ ] **Step 4: Commit**

```bash
git add .agents/skills/vice-head/SKILL.md
git commit -m "docs(vice-head): ratify ADR-059 for Auto-Pilot Daily Blueprint and UI Polish"
```
