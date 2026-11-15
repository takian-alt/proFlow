package com.neuroflow.app.presentation.v2.plan

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.GoalRepository
import com.neuroflow.app.data.repository.MindmapRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.isCurriculumTask
import com.neuroflow.app.domain.scheduler.isPersonalTask
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskScoreBreakdown
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.presentation.v2.mindmap.MindmapViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AutoBalanceAndMindmapNavigationTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "autoScheduleDay clears scheduledTime and scheduledDate on deferred candidate tasks and posts balance message" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
            val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
            val circadianService = DefaultCircadianEnergyService()

            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

            // Task 1: Fits in today's schedule
            val taskPlaced = TaskEntity(
                id = "task-placed-1",
                title = "Important Review",
                scheduledDate = dayStart,
                scheduledTime = 36_000_000L,
                status = TaskStatus.ACTIVE,
                isScheduleLocked = false,
                isAutoScheduled = true
            )
            // Task 2: Cannot fit in cognitive budget, deferred
            val taskDeferred = TaskEntity(
                id = "task-deferred-2",
                title = "Heavy Overbooked Work",
                scheduledDate = dayStart,
                scheduledTime = 40_000_000L,
                status = TaskStatus.ACTIVE,
                isScheduleLocked = false,
                isAutoScheduled = true
            )
            // Task 3: Locked task, even if deferred, should remain untouched
            val taskLockedDeferred = TaskEntity(
                id = "task-locked-3",
                title = "Client Call Locked",
                scheduledDate = dayStart,
                scheduledTime = 45_000_000L,
                status = TaskStatus.ACTIVE,
                isScheduleLocked = true
            )

            coEvery { taskRepo.getAllTasks() } returns listOf(taskPlaced, taskDeferred, taskLockedDeferred)
            coEvery { taskRepo.getActiveTasks() } returns listOf(taskPlaced, taskDeferred, taskLockedDeferred)
            coEvery { taskRepo.getById("task-placed-1") } returns taskPlaced
            coEvery { taskRepo.getById("task-deferred-2") } returns taskDeferred
            coEvery { taskRepo.getById("task-locked-3") } returns taskLockedDeferred
            every { taskRepo.observeActiveTasks() } returns flowOf(listOf(taskPlaced, taskDeferred, taskLockedDeferred))
            every { taskRepo.observeCompletedTasks() } returns flowOf(emptyList())

            val defaultPrefs = UserPreferences(
                autoSchedulingEnabled = true,
                workDayStart = 8,
                workDayEnd = 18
            )
            every { prefsDataStore.preferencesFlow } returns flowOf(defaultPrefs)

            // Mock scheduler service to place task 1 and defer tasks 2 and 3
            val schedPlacement = ScheduledPlacement(
                task = SchedulableTask(
                    id = "task-placed-1",
                    title = "Important Review",
                    category = TaskCategory.DEEP_WORK,
                    priority = 1,
                    estimatedMinutes = 60,
                    energyDemand = EnergyDemand.HIGH
                ),
                window = TimeSlotWindow(dayStart + 36_000_000L, dayStart + 39_600_000L),
                scoreBreakdown = TaskScoreBreakdown(
                    taskId = "task-placed-1",
                    totalScore = 80.0,
                    urgencyScore = 80.0,
                    impactScore = 80.0,
                    energyFitScore = 80.0,
                    momentumScore = 80.0,
                    bufferedDurationMinutes = 60
                )
            )
            val deferrals = listOf(
                DeferralRationale(
                    taskId = "task-deferred-2",
                    reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                    explanation = "Cognitive budget exceeded for today.",
                    actionableSuggestion = "Move to backlog."
                ),
                DeferralRationale(
                    taskId = "task-locked-3",
                    reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                    explanation = "No time slot available.",
                    actionableSuggestion = "Adjust schedule."
                )
            )

            coEvery {
                schedulerService.scheduleDayWithOutcome(
                    tasks = any(),
                    dayDate = any(),
                    nowMillis = any(),
                    wakeTimeMillis = any(),
                    lastSleepDurationMinutes = any(),
                    busyWindows = any(),
                    protectedSlots = any(),
                    calibrationRatio = any(),
                    forceReplan = any(),
                    zoneId = any(),
                    naps = any(),
                    chronotype = any(),
                    manualPeakStartHour = any(),
                    priorityWeights = any(),
                    learnedProfile = any(),
                    rollingAverageSleepMinutes = any(),
                    physiologicalBlocks = any(),
                    stimulantDoses = any(),
                    socialJetlagLagHours = any(),
                    activeUncompletedTaskIds = any(),
                    workDayStartHour = any(),
                    workDayEndHour = any(),
                    dailyCognitiveCapacityMinutes = any()
                )
            } returns SchedulingOutcome(
                placements = listOf(schedPlacement),
                deferrals = deferrals
            )

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = workManager,
                taskSchedulerService = schedulerService,
                circadianEnergyService = circadianService,
                calendarSyncRepository = calendarRepo
            )

            var emittedMessage: String? = null
            val job = backgroundScope.launch {
                viewModel.userActionMessage.collect { emittedMessage = it }
            }

            viewModel.autoScheduleDay(dayStart, forceReplan = false)
            advanceUntilIdle()

            // 1. Placed task was updated
            coVerify { taskRepo.update(match { it.id == "task-placed-1" && it.scheduledTime != null }) }

            // 2. Unlocked deferred task had scheduledTime and scheduledDate cleared to drop into backlog!
            coVerify {
                taskRepo.update(match {
                    it.id == "task-deferred-2" &&
                        it.scheduledTime == null &&
                        it.scheduledDate == null &&
                        !it.isAutoScheduled
                })
            }

            // 3. Locked task was NEVER modified
            coVerify(exactly = 0) { taskRepo.update(match { it.id == "task-locked-3" }) }

            // 4. Exact user action message was emitted
            emittedMessage shouldBe "Auto-balanced: scheduled 1 tasks, deferred 1 tasks to backlog to protect your cognitive budget."

            job.cancel()
        }
    }

    "Personal tasks receive priority boost and outrank roadmap curriculum tasks" {
        val personalTask = TaskEntity(
            id = "personal-task-1",
            title = "File Taxes and Renew Passport",
            tags = "finance,urgent",
            goalId = null,
            priority = Priority.MEDIUM,
            impactScore = 50,
            energyLevel = EnergyLevel.HIGH,
            taskType = TaskType.ANALYTICAL
        )

        val curriculumTask = TaskEntity(
            id = "task_s1_m1_item1",
            title = "LEARN: Virtual Memory, Stack vs Heap",
            tags = "systems,backend,linux,c,jvm,stage1,learn",
            goalId = "goal_stage_1",
            priority = Priority.HIGH,
            impactScore = 70,
            energyLevel = EnergyLevel.HIGH,
            taskType = TaskType.ANALYTICAL
        )

        // 1. Identification logic
        curriculumTask.isCurriculumTask() shouldBe true
        curriculumTask.isPersonalTask() shouldBe false

        personalTask.isCurriculumTask() shouldBe false
        personalTask.isPersonalTask() shouldBe true

        // 2. SchedulableTask personal flag & TaskSchedulerService readyQueue invariant
        val circadianService = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadianService)

        val schedPersonal = SchedulableTask(
            id = personalTask.id,
            title = personalTask.title,
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isPersonalTask = true
        )
        val schedCurriculum = SchedulableTask(
            id = curriculumTask.id,
            title = curriculumTask.title,
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isPersonalTask = false
        )

        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 6)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Schedulable day where there is only room for 1 task
        val outcome = scheduler.scheduleDayWithOutcome(
            tasks = listOf(schedCurriculum, schedPersonal),
            dayDate = today,
            nowMillis = dayStart + 8 * 3600_000L,
            wakeTimeMillis = dayStart + 7 * 3600_000L,
            lastSleepDurationMinutes = 480,
            workDayStartHour = 8,
            workDayEndHour = 10, // only 2 hours available!
            zoneId = zoneId
        )

        // The personal task MUST be placed first and must not be pushed out by curriculum task
        outcome.placements.isNotEmpty() shouldBe true
        outcome.placements.first().task.id shouldBe "personal-task-1"
    }

    "TaskSchedulerService defers tasks with COGNITIVE_BUDGET_EXCEEDED when daily cognitive capacity is reached" {
        val circadianService = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadianService)

        val task1 = SchedulableTask(
            id = "task_focus_1",
            title = "Core Engine Optimization",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 120,
            energyDemand = EnergyDemand.HIGH
        )
        val task2 = SchedulableTask(
            id = "task_focus_2",
            title = "Database Query Tuning",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 90,
            energyDemand = EnergyDemand.HIGH
        )
        val task3 = SchedulableTask(
            id = "task_low_3",
            title = "Clean Up Documentation Stubs",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.LOW
        )

        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 11, 8)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Cognitive budget capped at 180 minutes (tasks 1 + 2 = 210m, so task2 or task3 will exceed budget)
        val outcome = scheduler.scheduleDayWithOutcome(
            tasks = listOf(task1, task2, task3),
            dayDate = today,
            nowMillis = dayStart + 8 * 3600_000L,
            wakeTimeMillis = dayStart + 7 * 3600_000L,
            lastSleepDurationMinutes = 480,
            workDayStartHour = 8,
            workDayEndHour = 22,
            dailyCognitiveCapacityMinutes = 180,
            zoneId = zoneId
        )

        outcome.deferrals.any { it.reasonCode == DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED } shouldBe true
        val placedMinutes = outcome.placements.sumOf { it.window.durationMinutes }
        (placedMinutes <= 180) shouldBe true
    }

    "TaskSchedulerService avoids fragmentation and slivers by packing tasks flush to interval boundaries" {
        val circadianService = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadianService)

        val taskDeepWork = SchedulableTask(
            id = "task_deep_work_45m",
            title = "Architecture Review",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH
        )

        val zoneId = ZoneId.of("UTC")
        val tomorrow = LocalDate.of(2026, 11, 9)
        val dayStart = tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Free window is from 9:00 to 12:00 (180 mins)
        // With anti-fragmentation, deep work should pack flush to start (9:00) during morning peak,
        // preserving a contiguous block for subsequent tasks.
        val outcome = scheduler.scheduleDayWithOutcome(
            tasks = listOf(taskDeepWork),
            dayDate = tomorrow,
            nowMillis = dayStart + 8 * 3600_000L,
            wakeTimeMillis = dayStart + 7 * 3600_000L,
            lastSleepDurationMinutes = 480,
            workDayStartHour = 9,
            workDayEndHour = 12,
            zoneId = zoneId
        )

        outcome.placements.isNotEmpty() shouldBe true
        val placed = outcome.placements.first()
        val startMins = (placed.window.startMillis - dayStart) / 60_000L
        val endMins = (placed.window.endMillis - dayStart) / 60_000L
        startMins shouldBe 540L // Flush-left at 9:00 AM (540 mins)
    }

    "MindmapViewModel computes default collapsed state collapsing other stages and months except Stage 1 and Month 1" {
        val goal1 = GoalEntity(id = "goal_stage_1", title = "Stage 1: Memory, Linux & Core Backend")
        val goal2 = GoalEntity(id = "goal_stage_2", title = "Stage 2: Async Runtimes & Distributed Systems")
        val goal3 = GoalEntity(id = "goal_stage_3", title = "Stage 3: Advanced Storage & Consensus")
        val goal4 = GoalEntity(id = "goal_stage_4", title = "Stage 4: Production Kernel & SRE")

        val month1 = TaskEntity(id = "task_s1_m1", title = "Month 1: Raw C & OS Memory Mechanics", goalId = goal1.id)
        val month1Item1 = TaskEntity(id = "task_s1_m1_item1", title = "LEARN: Virtual Memory", parentTaskId = month1.id, goalId = goal1.id)
        val month1Item2 = TaskEntity(id = "task_s1_m1_item2", title = "BUILD: Malloc", parentTaskId = month1.id, goalId = goal1.id)

        val month2 = TaskEntity(id = "task_s1_m2", title = "Month 2: POSIX & Concurrency", goalId = goal1.id)
        val month2Item1 = TaskEntity(id = "task_s1_m2_item1", title = "LEARN: Threads", parentTaskId = month2.id, goalId = goal1.id)

        val month3 = TaskEntity(id = "task_s1_m3", title = "Month 3: Network Mechanics", goalId = goal1.id)
        val month3Item1 = TaskEntity(id = "task_s1_m3_item1", title = "LEARN: TCP", parentTaskId = month3.id, goalId = goal1.id)

        val repository = mockk<MindmapRepository>(relaxed = true)
        val viewModel = MindmapViewModel(repository)

        val goals = listOf(goal1, goal2, goal3, goal4)
        val tasks = listOf(month1, month1Item1, month1Item2, month2, month2Item1, month3, month3Item1)

        val defaultCollapsed = viewModel.computeDefaultCollapsedNodeIds(goals, tasks)

        // Stage 1 must NOT be collapsed
        defaultCollapsed shouldNotContain "goal_stage_1"
        // Month 1 must NOT be collapsed
        defaultCollapsed shouldNotContain "task_s1_m1"

        // Stages 2, 3, 4 MUST be collapsed
        defaultCollapsed shouldContain "goal_stage_2"
        defaultCollapsed shouldContain "goal_stage_3"
        defaultCollapsed shouldContain "goal_stage_4"

        // Month 2 and Month 3 subtrees MUST be collapsed
        defaultCollapsed shouldContain "task_s1_m2"
        defaultCollapsed shouldContain "task_s1_m3"
    }

    "MindmapViewModel identifies Month 1 Item 1 as activeStartingNodeId and advances when completed" {
        val month1 = TaskEntity(id = "task_s1_m1", title = "Month 1: Raw C & OS Memory Mechanics")
        val item1 = TaskEntity(
            id = "task_s1_m1_item1",
            title = "LEARN: Virtual Memory, Stack vs. Heap",
            parentTaskId = month1.id,
            dependsOnTaskIds = "",
            status = TaskStatus.ACTIVE
        )
        val item2 = TaskEntity(
            id = "task_s1_m1_item2",
            title = "BUILD: Custom malloc in C",
            parentTaskId = month1.id,
            dependsOnTaskIds = "task_s1_m1_item1",
            status = TaskStatus.ACTIVE
        )

        val repository = mockk<MindmapRepository>(relaxed = true)
        val viewModel = MindmapViewModel(repository)

        // Initially item 1 is uncompleted and has no prereqs
        val activeBeacon1 = viewModel.findActiveStartingNodeId(listOf(month1, item1, item2))
        activeBeacon1 shouldBe "task_s1_m1_item1"

        // Once item 1 is completed, item 2 becomes actionable and prerequisites are met
        val item1Completed = item1.copy(status = TaskStatus.COMPLETED)
        val activeBeacon2 = viewModel.findActiveStartingNodeId(listOf(month1, item1Completed, item2))
        activeBeacon2 shouldBe "task_s1_m1_item2"
    }
})
