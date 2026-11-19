package com.neuroflow.app.domain.scheduler

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AutoPlanAndBalanceDateIsolationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.systemDefault()
    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun workerCandidateSelection_neverPullsTomorrowOrFutureAutoScheduledTasksIntoToday() {
        val today = LocalDate.now(zoneId)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val day3Start = today.plusDays(3).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nowMillis = todayStart + 10 * 3600_000L

        val prefs = UserPreferences(
            autoSchedulingEnabled = true,
            isAutoScheduleToggleOn = true
        )

        val tomorrowAutoTask = TaskEntity(
            id = "tmrw-auto",
            title = "Tomorrow Auto Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 9 * 3600_000L,
            isAutoScheduled = true,
            isScheduleLocked = false,
            lastAutoScheduledAt = nowMillis - 12 * 3600_000L,
            status = TaskStatus.ACTIVE
        )
        val day3AutoTask = TaskEntity(
            id = "day3-auto",
            title = "Day 3 Auto Task",
            scheduledDate = day3Start,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            isScheduleLocked = false,
            lastAutoScheduledAt = nowMillis - 24 * 3600_000L,
            priority = Priority.HIGH,
            isFrog = true,
            status = TaskStatus.ACTIVE
        )
        val tomorrowDateOnlyTask = TaskEntity(
            id = "tmrw-date-only",
            title = "Tomorrow Date-Only Task",
            scheduledDate = tomorrowStart,
            scheduledTime = null,
            isAutoScheduled = false,
            status = TaskStatus.ACTIVE
        )
        val tomorrowHabit = TaskEntity(
            id = "tmrw-habit",
            title = "Tomorrow Recurring Habit",
            recurrence = Recurrence.DAILY,
            habitDate = tomorrowStart,
            scheduledDate = null,
            scheduledTime = null,
            status = TaskStatus.ACTIVE
        )
        val todayEligibleReplan = TaskEntity(
            id = "today-auto",
            title = "Today Afternoon Auto Task",
            scheduledDate = todayStart,
            scheduledTime = 16 * 3600_000L,
            isAutoScheduled = true,
            isScheduleLocked = false,
            lastAutoScheduledAt = nowMillis - 6 * 3600_000L,
            status = TaskStatus.ACTIVE
        )
        val backlogTask = TaskEntity(
            id = "backlog-1",
            title = "Unscheduled Inbox Task",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            status = TaskStatus.ACTIVE
        )

        val buckets = ScheduleAutoTasksWorker.selectCandidateBuckets(
            allTasks = listOf(
                tomorrowAutoTask,
                day3AutoTask,
                tomorrowDateOnlyTask,
                tomorrowHabit,
                todayEligibleReplan,
                backlogTask
            ),
            dayStart = todayStart,
            dayEnd = todayEnd,
            nowMillis = nowMillis,
            prefs = prefs,
            forceReplan = true
        )

        val allSelectedIds = (buckets.unscheduledTasks + buckets.missedTasks + buckets.replanCandidates)
            .map { it.id }
            .toSet()

        assertFalse("Tomorrow auto-scheduled task must NOT enter Today worker", "tmrw-auto" in allSelectedIds)
        assertFalse("Day 3 high-priority auto-scheduled task must NOT enter Today worker", "day3-auto" in allSelectedIds)
        assertFalse("Tomorrow date-only task must NOT enter Today worker", "tmrw-date-only" in allSelectedIds)
        assertFalse("Tomorrow recurring habit must NOT enter Today worker", "tmrw-habit" in allSelectedIds)
        assertTrue("Today eligible auto-scheduled task should stay eligible for Today", "today-auto" in allSelectedIds)
        assertTrue("Unscheduled backlog task should stay eligible for Today", "backlog-1" in allSelectedIds)
    }

    @Test
    fun multiDayAutoBalance_doesNotPullTomorrowOrFutureTasksIntoEmptyToday() = runTest(testDispatcher) {
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                wakeTimeHour = 7,
                wakeTimeMinute = 0,
                workDayStart = 8,
                workDayEnd = 19,
                dailyDeepWorkTargetMinutes = 240
            )
        )
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = circadianService,
            preferencesDataStore = prefsDataStore
        )

        val utc = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 24)
        val tomorrow = today.plusDays(1)
        val day3 = today.plusDays(3)
        val todayNow = today.atTime(LocalTime.of(9, 0)).atZone(utc).toInstant().toEpochMilli()
        val tomorrowStart = tomorrow.atStartOfDay(utc).toInstant().toEpochMilli()
        val day3Start = day3.atStartOfDay(utc).toInstant().toEpochMilli()

        // Today has NO tasks (user deleted their today's task).
        // Tomorrow has an unlocked auto-scheduled task, and Day 3 has a user-dated task.
        val tomorrowAutoTask = SchedulableTask(
            id = "tmrw-auto",
            title = "Tomorrow Auto-Scheduled Deep Work",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isLocked = false,
            scheduledStartMillis = tomorrowStart + 9 * 3600_000L,
            preferredDayStartMillis = tomorrowStart
        )
        val day3UserDatedTask = SchedulableTask(
            id = "day3-dated",
            title = "Day 3 Planned Task",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.MODERATE,
            isLocked = false,
            scheduledStartMillis = null,
            preferredDayStartMillis = day3Start
        )

        val outcome = orchestrator.orchestrateMultiDayScheduleWithContext(
            tasks = listOf(tomorrowAutoTask, day3UserDatedTask),
            startDate = today,
            horizonDays = 5,
            zoneId = utc,
            referenceNowMillis = todayNow
        )

        val todayPlacements = outcome.dayOutcomes[today]?.placements.orEmpty()
        val tomorrowPlacements = outcome.dayOutcomes[tomorrow]?.placements.orEmpty()
        val day3Placements = outcome.dayOutcomes[day3]?.placements.orEmpty()

        assertTrue(
            "Empty Today must NOT steal tomorrow or Day 3 tasks (placed=${todayPlacements.map { it.task.id }})",
            todayPlacements.isEmpty()
        )
        assertTrue(
            "Tomorrow's task must remain scheduled on Tomorrow",
            tomorrowPlacements.any { it.task.id == "tmrw-auto" }
        )
        assertTrue(
            "Day 3's task must remain scheduled on Day 3",
            day3Placements.any { it.task.id == "day3-dated" }
        )
        assertTrue("No tasks should be deferred", outcome.finalDeferred.isEmpty())
    }

    @Test
    fun multiDayAutoBalance_pullsFutureAnchoredTaskToTodayOnlyWhenHardDeadlineIsToday() = runTest(testDispatcher) {
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                wakeTimeHour = 7,
                wakeTimeMinute = 0,
                workDayStart = 8,
                workDayEnd = 19,
                dailyDeepWorkTargetMinutes = 240
            )
        )
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = circadianService,
            preferencesDataStore = prefsDataStore
        )

        val utc = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 24)
        val tomorrow = today.plusDays(1)
        val todayNow = today.atTime(LocalTime.of(9, 0)).atZone(utc).toInstant().toEpochMilli()
        val todayDeadline = today.atTime(LocalTime.of(17, 0)).atZone(utc).toInstant().toEpochMilli()
        val tomorrowStart = tomorrow.atStartOfDay(utc).toInstant().toEpochMilli()

        val urgentMislocatedTask = SchedulableTask(
            id = "due-today-but-dated-tmrw",
            title = "Tax Filing Due Today",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isLocked = false,
            preferredDayStartMillis = tomorrowStart,
            deadlineMillis = todayDeadline
        )

        val outcome = orchestrator.orchestrateMultiDayScheduleWithContext(
            tasks = listOf(urgentMislocatedTask),
            startDate = today,
            horizonDays = 3,
            zoneId = utc,
            referenceNowMillis = todayNow
        )

        val todayPlacements = outcome.dayOutcomes[today]?.placements.orEmpty()
        assertTrue(
            "Task with hard deadline strictly Today must be rescued onto Today",
            todayPlacements.any { it.task.id == "due-today-but-dated-tmrw" }
        )
    }

    @Test
    fun planViewModelAutoBalance_excludesBeyondHorizonTasksAndPreservesTodayWhenViewingTomorrow() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)
        val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
        val multiDayOrch = mockk<MultiDayScheduleOrchestrator>(relaxed = true)

        val today = LocalDate.now(zoneId)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val day10Start = today.plusDays(10).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val todayTask = TaskEntity(
            id = "today-task",
            title = "Today Scheduled Task",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val tomorrowTask = TaskEntity(
            id = "tmrw-task",
            title = "Tomorrow Scheduled Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val beyondHorizonTask = TaskEntity(
            id = "day10-task",
            title = "Next Month Prep",
            scheduledDate = day10Start,
            scheduledTime = 11 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )

        val allTasks = listOf(todayTask, tomorrowTask, beyondHorizonTask)
        coEvery { taskRepo.getActiveTasks() } returns allTasks
        coEvery { taskRepo.getAllTasks() } returns allTasks
        every { taskRepo.observeActiveTasks() } returns flowOf(allTasks)
        every { taskRepo.observeCompletedTasks() } returns flowOf(emptyList())
        every { prefsDataStore.preferencesFlow } returns flowOf(UserPreferences(autoSchedulingEnabled = true))

        var capturedCandidateIds: Set<String> = emptySet()
        coEvery {
            multiDayOrch.orchestrateMultiDayScheduleWithContext(
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
        } answers {
            val passedTasks = firstArg<List<SchedulableTask>>()
            capturedCandidateIds = passedTasks.map { it.id }.toSet()
            MultiDaySchedulingOutcome(
                dayOutcomes = emptyMap(),
                totalScheduledTasks = 0,
                crossDaySplits = emptyList(),
                finalDeferred = emptyList()
            )
        }

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            calendarSyncRepository = calendarRepo,
            multiDayScheduleOrchestrator = multiDayOrch
        )

        // User navigates to Tomorrow and taps Balance (7-day horizon from Tomorrow..Day+7)
        viewModel.selectDate(tomorrowStart)
        viewModel.autoBalance(horizonDays = 7).join()
        advanceUntilIdle()

        assertEquals(
            "Only Tomorrow's in-horizon task should be passed; Today and Day+10 must stay untouched",
            setOf("tmrw-task"),
            capturedCandidateIds
        )
        coVerify(exactly = 0) { taskRepo.update(match { it.id == "day10-task" }) }
        coVerify(exactly = 0) { taskRepo.update(match { it.id == "today-task" }) }
    }

    @Test
    fun dailyBlueprintOrchestrator_neverPullsTomorrowAutoScheduledTasksIntoEmptyToday() = runTest(testDispatcher) {
        val today = LocalDate.now(zoneId)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val tomorrowAutoTask = TaskEntity(
            id = "tmrw-auto-bp",
            title = "Tomorrow Auto Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.DO_FIRST,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.ACTIVE
        )

        val taskRepo = mockk<TaskRepository>(relaxed = true)
        coEvery { taskRepo.getActiveTasks() } returns listOf(tomorrowAutoTask)
        coEvery { taskRepo.getAllTasks() } returns listOf(tomorrowAutoTask)

        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyDeepWorkTargetMinutes = 240, workDayStart = 8, workDayEnd = 20)
        )

        val orchestrator = DailyBlueprintOrchestrator(
            taskRepository = taskRepo,
            schedulerService = schedulerService,
            circadianEnergyService = circadianService,
            geminiAiOnlineService = mockk<GeminiAiOnlineService>(relaxed = true),
            preferencesDataStore = prefsDataStore
        )

        val blueprint = orchestrator.generateBlueprint(todayStart)

        assertTrue("Today's blueprint must NOT include tomorrow's auto-scheduled task", blueprint.items.isEmpty())
        assertTrue("Today's overflow must NOT include tomorrow's auto-scheduled task", blueprint.overflowTasks.isEmpty())
    }

    @Test
    fun taskRepositoryUpdate_doesNotRetriggerSingleDayWorkerOnAutoSchedulerWrites() = runTest(testDispatcher) {
        val taskDao = mockk<TaskDao>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)
        val hyperFocusManager = mockk<HyperFocusManager>(relaxed = true)
        coEvery { hyperFocusManager.isTaskDeletionBlocked(any()) } returns false

        val repo = TaskRepository(
            taskDao = taskDao,
            workManager = workManager,
            hyperFocusManager = Lazy { hyperFocusManager }
        )

        val today = LocalDate.now(zoneId)
        val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val original = TaskEntity(
            id = "bal-task-1",
            title = "Balanced Tomorrow Task",
            scheduledDate = null,
            scheduledTime = null,
            isAutoScheduled = false,
            lastAutoScheduledAt = null,
            status = TaskStatus.ACTIVE
        )
        coEvery { taskDao.getById("bal-task-1") } returns original

        // 1. Auto-Balance / Blueprint write stamps isAutoScheduled = true and new lastAutoScheduledAt
        val balancedWrite = original.copy(
            scheduledDate = tomorrowStart,
            scheduledTime = 9 * 3600_000L,
            isAutoScheduled = true,
            lastAutoScheduledAt = now,
            lastStabilizedAt = now
        )
        repo.update(balancedWrite)

        verify(exactly = 0) {
            workManager.enqueueUniqueWork(
                "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>()
            )
        }

        // 2. Manual user edit (priority change without touching lastAutoScheduledAt) DOES still trigger recheck
        coEvery { taskDao.getById("bal-task-1") } returns balancedWrite
        val userEdit = balancedWrite.copy(priority = Priority.HIGH, updatedAt = now + 5000L)
        repo.update(userEdit)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>()
            )
        }
    }
}
