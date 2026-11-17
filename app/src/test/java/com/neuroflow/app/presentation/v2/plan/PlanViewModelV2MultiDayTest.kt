package com.neuroflow.app.presentation.v2.plan

import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.calendar.ExternalCalendarDelta
import com.neuroflow.app.data.calendar.ExternalDeltaType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.MultiDayScheduleOrchestrator
import com.neuroflow.app.domain.scheduler.MultiDaySchedulingOutcome
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskScoreBreakdown
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelV2MultiDayTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepo = mockk<TaskRepository>(relaxed = true)
    private val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val circadianService: CircadianEnergyService = DefaultCircadianEnergyService()
    private val multiDayOrchestrator = mockk<MultiDayScheduleOrchestrator>(relaxed = true)

    private val defaultPrefs = UserPreferences(
        autoSchedulingEnabled = true,
        dailyDeepWorkTargetMinutes = 240,
        workDayStart = 8,
        workDayEnd = 18,
        lastCalendarInboundSyncAt = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { prefsDataStore.preferencesFlow } returns flowOf(defaultPrefs)
        every { taskRepo.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepo.observeCompletedTasks() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSevenDayLoadDistribution_computesDailyLoadFractions() = runTest(testDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val todayEpoch = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowEpoch = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val task1 = TaskEntity(
            id = "task-1",
            title = "Task 1",
            scheduledDate = todayEpoch,
            scheduledTime = 36_000_000L,
            plannedBlockMinutes = 120,
            status = TaskStatus.ACTIVE
        )
        val task2 = TaskEntity(
            id = "task-2",
            title = "Task 2",
            scheduledDate = tomorrowEpoch,
            scheduledTime = 36_000_000L,
            plannedBlockMinutes = 240,
            status = TaskStatus.ACTIVE
        )

        every { taskRepo.observeActiveTasks() } returns flowOf(listOf(task1, task2))

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            circadianEnergyService = circadianService,
            taskSchedulerService = schedulerService,
            calendarSyncRepository = calendarRepo,
            multiDayScheduleOrchestrator = multiDayOrchestrator
        )

        advanceUntilIdle()

        val loads = viewModel.sevenDayLoadDistribution.first { it.isNotEmpty() }
        assertEquals(7, loads.size)
        assertTrue(loads.containsKey(today))
        assertTrue(loads.containsKey(today.plusDays(1)))

        // 120 / 240 = 0.5
        assertEquals(0.5f, loads[today] ?: 0f, 0.01f)
        // 240 / 240 = 1.0
        assertEquals(1.0f, loads[today.plusDays(1)] ?: 0f, 0.01f)
        // Day 2..6 have 0 tasks -> 0.0
        assertEquals(0.0f, loads[today.plusDays(2)] ?: 0f, 0.01f)
    }

    @Test
    fun testOnScreenResume_syncsInboundDeltas() = runTest(testDispatcher) {
        val movedTask = TaskEntity(
            id = "task-moved",
            title = "External Moved Task",
            scheduledDate = 5000L,
            scheduledTime = 1000L,
            status = TaskStatus.ACTIVE
        )
        val deletedTask = TaskEntity(
            id = "task-deleted",
            title = "External Deleted Task",
            scheduledDate = 5000L,
            scheduledTime = 1000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )

        coEvery { taskRepo.getById("task-moved") } returns movedTask
        coEvery { taskRepo.getById("task-deleted") } returns deletedTask

        val zoneId = ZoneId.systemDefault()
        val newTargetEpoch = LocalDate.now(zoneId).plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli() + 36_000_000L

        coEvery { calendarRepo.detectExternalModifications(1000L) } returns listOf(
            ExternalCalendarDelta("task-moved", ExternalDeltaType.MOVED, newStartMillis = newTargetEpoch),
            ExternalCalendarDelta("task-deleted", ExternalDeltaType.DELETED)
        )

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            calendarSyncRepository = calendarRepo,
            multiDayScheduleOrchestrator = multiDayOrchestrator
        )

        viewModel.onScreenResume()
        advanceUntilIdle()

        coVerify {
            taskRepo.update(match {
                it.id == "task-moved" &&
                    it.isScheduleLocked &&
                    it.scheduledTime == 36_000_000L
            })
        }

        coVerify {
            taskRepo.update(match {
                it.id == "task-deleted" &&
                    it.scheduledDate == null &&
                    it.scheduledTime == null &&
                    !it.isAutoScheduled
            })
        }

        coVerify { prefsDataStore.setLastCalendarInboundSyncAt(any()) }
    }

    @Test
    fun testAutoBalance_orchestratesMultiDayHorizon() = runTest(testDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val task1 = TaskEntity(
            id = "task-multi-1",
            title = "Multi-Day Task 1",
            status = TaskStatus.ACTIVE,
            isScheduleLocked = false
        )
        coEvery { taskRepo.getActiveTasks() } returns listOf(task1)
        coEvery { taskRepo.getById("task-multi-1") } returns task1

        val placement = ScheduledPlacement(
            task = SchedulableTask(
                id = "task-multi-1",
                title = "Multi-Day Task 1",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 60,
                energyDemand = com.neuroflow.app.domain.service.EnergyDemand.HIGH
            ),
            window = TimeSlotWindow(dayStart + 36_000_000L, dayStart + 39_600_000L),
            scoreBreakdown = TaskScoreBreakdown(
                taskId = "task-multi-1",
                totalScore = 90.0,
                urgencyScore = 90.0,
                impactScore = 90.0,
                energyFitScore = 90.0,
                momentumScore = 90.0,
                bufferedDurationMinutes = 60
            )
        )

        coEvery {
            multiDayOrchestrator.orchestrateMultiDayScheduleWithContext(
                tasks = any(),
                startDate = any(),
                horizonDays = any(),
                zoneId = any(),
                referenceNowMillis = any(),
                rejectionPatternsJson = any(),
                allKnownTasks = any(),
                closedSessions = any(),
                extraActiveUncompletedIds = any(),
                externalAndFixedBusyWindowsByDate = any(),
                precommittedCognitiveMinutesByDate = any(),
                forceReplan = any()
            )
        } returns MultiDaySchedulingOutcome(
            dayOutcomes = mapOf(today to SchedulingOutcome(placements = listOf(placement), deferrals = emptyList())),
            totalScheduledTasks = 1,
            crossDaySplits = emptyList(),
            finalDeferred = emptyList()
        )

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            calendarSyncRepository = calendarRepo,
            multiDayScheduleOrchestrator = multiDayOrchestrator
        )

        viewModel.autoBalance(horizonDays = 7)
        advanceUntilIdle()

        coVerify {
            taskRepo.update(match {
                it.id == "task-multi-1" &&
                    it.scheduledDate == dayStart &&
                    it.scheduledTime == 36_000_000L &&
                    it.isAutoScheduled
            })
        }
    }

    @Test
    fun testAutoBalance_forwardsLockedBusyWindowsAndPrecommittedCognitiveMinutes() = runTest(testDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val lockedTask = TaskEntity(
            id = "locked-morning",
            title = "Locked Architecture Review",
            scheduledDate = dayStart,
            scheduledTime = 9 * 3600_000L,
            plannedBlockMinutes = 90,
            estimatedDurationMinutes = 90,
            isScheduleLocked = true,
            isAutoScheduled = false,
            energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
            status = TaskStatus.ACTIVE
        )
        val completedTodayTask = TaskEntity(
            id = "done-today",
            title = "Completed Morning Deep Work",
            scheduledDate = dayStart,
            plannedBlockMinutes = 45,
            estimatedDurationMinutes = 45,
            energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
            status = TaskStatus.COMPLETED,
            completedAt = dayStart + 8 * 3600_000L
        )
        val mutableTask = TaskEntity(
            id = "mutable-1",
            title = "Flexible Task",
            estimatedDurationMinutes = 60,
            status = TaskStatus.ACTIVE,
            isScheduleLocked = false
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(lockedTask, mutableTask)
        coEvery { taskRepo.getAllTasks() } returns listOf(lockedTask, completedTodayTask, mutableTask)
        coEvery {
            multiDayOrchestrator.orchestrateMultiDayScheduleWithContext(
                tasks = any(),
                startDate = any(),
                horizonDays = any(),
                zoneId = any(),
                referenceNowMillis = any(),
                rejectionPatternsJson = any(),
                allKnownTasks = any(),
                closedSessions = any(),
                extraActiveUncompletedIds = any(),
                externalAndFixedBusyWindowsByDate = any(),
                precommittedCognitiveMinutesByDate = any(),
                forceReplan = any()
            )
        } returns MultiDaySchedulingOutcome(
            dayOutcomes = emptyMap(),
            totalScheduledTasks = 0,
            crossDaySplits = emptyList(),
            finalDeferred = emptyList()
        )

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            calendarSyncRepository = calendarRepo,
            multiDayScheduleOrchestrator = multiDayOrchestrator
        )

        viewModel.autoBalance(horizonDays = 7).join()
        advanceUntilIdle()

        coVerify {
            multiDayOrchestrator.orchestrateMultiDayScheduleWithContext(
                tasks = match { it.size == 1 && it.first().id == "mutable-1" },
                startDate = today,
                horizonDays = 7,
                zoneId = any(),
                referenceNowMillis = any(),
                rejectionPatternsJson = any(),
                allKnownTasks = match { it.size == 3 },
                closedSessions = any(),
                extraActiveUncompletedIds = any(),
                externalAndFixedBusyWindowsByDate = match { map ->
                    val wins = map[today].orEmpty()
                    wins.any { w -> w.startMillis == dayStart + 9 * 3600_000L && w.durationMinutes == 90 }
                },
                precommittedCognitiveMinutesByDate = match { map ->
                    map[today] == 135
                },
                forceReplan = true
            )
        }
    }
}
