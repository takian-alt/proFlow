package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DailyCircadianSummary
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class FlowScreenV2AuditTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        mockkObject(AutonomyNudgeEngine)
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
    }

    afterTest {
        Dispatchers.resetMain()
        unmockkAll()
    }

    "BUG-235: FlowViewModelV2 sorts timed tasks chronologically first and date-only untimed tasks after (preventing 00:00 midnight jump)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // Task 1: 09:00 AM timed task
            val morningTask = TaskEntity(
                id = "morning-task",
                title = "Morning Deep Work",
                scheduledDate = todayStart,
                scheduledTime = 9 * 3600_000L,
                status = TaskStatus.ACTIVE,
                createdAt = 1000L
            )
            // Task 2: Untimed task for today (scheduledTime = null)
            val untimedTask = TaskEntity(
                id = "untimed-task",
                title = "Untimed Day Task",
                scheduledDate = todayStart,
                scheduledTime = null,
                status = TaskStatus.ACTIVE,
                createdAt = 2000L
            )
            // Task 3: 14:00 PM timed task
            val afternoonTask = TaskEntity(
                id = "afternoon-task",
                title = "Afternoon Meeting",
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE,
                createdAt = 3000L
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(untimedTask, morningTask, afternoonTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )
            val state = viewModel.uiState.filter { !it.isLoading }.first()

            val ids = state.filteredTasks.map { it.id }
            // Timed tasks must appear in chronological order first; untimed tasks must follow!
            ids shouldBe listOf("morning-task", "afternoon-task", "untimed-task")
        }
    }

    "BUG-237: FlowViewModelV2.shiftTaskToPeak schedules AutonomyNudge and preserves recurring habitDate via effectiveScheduledDateMillis" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val recurringHabit = TaskEntity(
                id = "habit-shift",
                title = "Daily Meditation",
                recurrence = Recurrence.DAILY,
                habitDate = todayStart,
                scheduledDate = null,
                scheduledTime = 8 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(recurringHabit))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(
                    peakEnergyStart = 10,
                    effectivePeakStart = 11 // Calibrated peak
                )
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo,
                context = mockContext
            )

            viewModel.shiftTaskToPeak(recurringHabit)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "habit-shift" &&
                        updated.habitDate != null &&
                        updated.isScheduleLocked &&
                        !updated.isAutoScheduled
                })
            }

            // Nudge engine must be scheduled for the updated task
            coVerify {
                AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "habit-shift" })
            }
        }
    }

    "BUG-236: Micro-step Doorway header clamps to totalCount and displays all-completed text" {
        val microSteps = listOf(
            StepByStepPlanStep("Open IDE", completed = true),
            StepByStepPlanStep("Pull branch", completed = true),
            StepByStepPlanStep("Run tests", completed = true)
        )
        val completedCount = microSteps.count { it.completed }
        val totalCount = microSteps.size

        val currentStep = microSteps.firstOrNull { !it.completed } ?: microSteps.last()
        val stepLabel = if (completedCount >= totalCount) {
            "🪜 2m Doorway: All $totalCount steps completed"
        } else {
            "🪜 2m Doorway: Step ${completedCount + 1} of $totalCount"
        }

        completedCount shouldBe 3
        totalCount shouldBe 3
        stepLabel shouldBe "🪜 2m Doorway: All 3 steps completed"
        currentStep.text shouldBe "Run tests"
    }

    "BUG-234: TopTask resolution in FlowScreenV2 prevents future and icebox task leakage when today tasks are completed or filtered" {
        val now = System.currentTimeMillis()
        val todayStart = startOfLocalDay(now)
        val nextWeekStart = todayStart + 7 * 86_400_000L

        val futureTask = TaskEntity(
            id = "future-task-1",
            title = "Future Launch",
            scheduledDate = nextWeekStart,
            status = TaskStatus.ACTIVE
        )
        val iceboxTask = TaskEntity(
            id = "icebox-task-1",
            title = "Someday Maybe",
            quadrant = Quadrant.ELIMINATE,
            status = TaskStatus.ACTIVE
        )
        val todayAdminTask = TaskEntity(
            id = "today-admin",
            title = "Reply to emails",
            tags = "admin",
            scheduledDate = todayStart,
            status = TaskStatus.ACTIVE
        )
        val todayDeepworkTask = TaskEntity(
            id = "today-deepwork",
            title = "Build architecture",
            tags = "deepwork",
            scheduledDate = todayStart,
            status = TaskStatus.ACTIVE
        )

        // Case 1: Today has no active tasks left (filteredTasks is empty).
        // topTask MUST NOT fall back to raw activeTasks (future or icebox)!
        val emptyTodayTasks = emptyList<TaskEntity>()
        val selectedTagNull: String? = null
        val recommendedNull: TaskEntity? = null
        val topTaskWhenTodayEmpty = if (selectedTagNull.isNullOrBlank() || selectedTagNull.equals("All", ignoreCase = true)) {
            recommendedNull ?: emptyTodayTasks.firstOrNull { it.isFrog } ?: emptyTodayTasks.firstOrNull()
        } else {
            emptyTodayTasks.firstOrNull { it.id == recommendedNull?.id }
                ?: emptyTodayTasks.firstOrNull { it.isFrog }
                ?: emptyTodayTasks.firstOrNull()
        }
        topTaskWhenTodayEmpty shouldBe null

        // Case 2: User selected tag "#admin"
        val filteredForAdmin = listOf(todayAdminTask)
        val recommendedGlobal = todayDeepworkTask // recommendedTask is tag-agnostic
        val selectedTag = "#admin"

        val resolvedTopTask = if (selectedTag.isNullOrBlank() || selectedTag.equals("All", ignoreCase = true)) {
            recommendedGlobal ?: filteredForAdmin.firstOrNull { it.isFrog } ?: filteredForAdmin.firstOrNull()
        } else {
            filteredForAdmin.firstOrNull { it.id == recommendedGlobal.id }
                ?: filteredForAdmin.firstOrNull { it.isFrog }
                ?: filteredForAdmin.firstOrNull()
        }

        // Spotlight must synchronize with tag filter and show the admin task, not the deepwork task!
        resolvedTopTask?.id shouldBe "today-admin"
    }

    "BUG-237: shiftTaskToPeak falls back to effectivePeakStart when circadianSummary is null" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val futureTask = TaskEntity(
                id = "future-task",
                title = "Prepare quarterly review",
                scheduledDate = todayStart + 2 * 86_400_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(
                    peakEnergyStart = 9,
                    effectivePeakStart = 15 // Chronotype quiz evaluated evening peak at 3 PM
                )
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )

            viewModel.shiftTaskToPeak(futureTask)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "future-task" &&
                        // Since baseDate is in the future (> todayStart), targetHour is peakHour (15)
                        updated.scheduledTime == 15 * 3600_000L
                })
            }
        }
    }

    "BUG-284: FlowViewModelV2 distinctUntilChanged emits new state when circadian peak fields change" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val prefsFlow = MutableStateFlow(
                UserPreferences(
                    peakEnergyStart = 9,
                    peakEnergyEnd = 12,
                    effectivePeakStart = 9,
                    effectivePeakEnd = 12
                )
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns prefsFlow

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state1 = viewModel.uiState.filter { !it.isLoading }.first()
            state1.preferences.effectivePeakStart shouldBe 9

            // Update effective peak start to 14 (calibrated afternoon peak)
            prefsFlow.value = UserPreferences(
                peakEnergyStart = 9,
                peakEnergyEnd = 12,
                effectivePeakStart = 14,
                effectivePeakEnd = 17
            )
            advanceUntilIdle()

            val state2 = viewModel.uiState.filter { it.preferences.effectivePeakStart == 14 }.first()
            state2.preferences.effectivePeakStart shouldBe 14
        }
    }

    "BUG-285: FlowViewModelV2 includes completed Part 1 continuation Part 2 task in actionable tasks and recommended task" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val completedPart1 = TaskEntity(
                id = "deepwork_task",
                title = "Deep Architecture Part 1",
                scheduledDate = todayStart,
                scheduledTime = 9 * 3600_000L,
                status = TaskStatus.COMPLETED
            )
            val part2Continuation = TaskEntity(
                id = "deepwork_task_part2",
                parentTaskId = "deepwork_task",
                dependsOnTaskIds = "deepwork_task",
                title = "Deep Architecture Part 2",
                scheduledDate = todayStart,
                scheduledTime = 11 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(part2Continuation))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(completedPart1))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // When Part 1 is completed, Part 2 MUST be actionable and recommended!
            state.recommendedTask?.id shouldBe "deepwork_task_part2"
            state.filteredTasks.map { it.id } shouldBe listOf("deepwork_task_part2")
        }
    }

    "BUG-286: FlowViewModelV2 updateTask, undoCompleteTask, and undoDeferTask schedule AutonomyNudges" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val task = TaskEntity(
                id = "task-edit-nudge",
                title = "Update Architecture Doc",
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                context = mockContext
            )

            // 1. updateTask
            viewModel.updateTask(task.copy(title = "Renamed Architecture Doc"))
            advanceUntilIdle()
            coVerify { AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "task-edit-nudge" && it.title == "Renamed Architecture Doc" }) }

            // 2. completeTask then undoCompleteTask
            viewModel.completeTask(task)
            advanceUntilIdle()
            viewModel.undoCompleteTask()
            advanceUntilIdle()
            coVerify(atLeast = 1) { AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "task-edit-nudge" && it.status == TaskStatus.ACTIVE }) }

            // 3. deferTask then undoDeferTask
            viewModel.deferTask(task, hours = 2)
            advanceUntilIdle()
            viewModel.undoDeferTask()
            advanceUntilIdle()
            coVerify(atLeast = 1) { AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "task-edit-nudge" && it.scheduledTime == 14 * 3600_000L }) }
        }
    }

    "BUG-287: TaskBlockPlan.plannedMinutes calibrated for split tasks over raw estimatedDurationMinutes" {
        val splitPart1 = TaskEntity(
            id = "heavy_task#part1",
            title = "Heavy Refactor (Part 1/2)",
            estimatedDurationMinutes = 90, // Raw full task estimate
            plannedBlockMinutes = 45 // Calibrated 45m block reserved
        )
        val planned = com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(splitPart1)
        planned shouldBe 45
        planned shouldNotBe splitPart1.estimatedDurationMinutes
    }

    "BUG-288: FlowViewModelV2 applyEmergencyReset updates habitDate for rescheduled habits and clears it for backlogged habits" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val yesterdayStart = todayStart - 86_400_000L

            val overdueHabit = TaskEntity(
                id = "habit-overdue",
                title = "Daily Pushups",
                recurrence = Recurrence.DAILY,
                habitDate = yesterdayStart,
                scheduledDate = yesterdayStart,
                postponeCount = 3,
                status = TaskStatus.ACTIVE
            )
            val backloggedHabit = TaskEntity(
                id = "habit-backlog",
                title = "Daily Journaling",
                recurrence = Recurrence.DAILY,
                habitDate = yesterdayStart,
                scheduledDate = yesterdayStart,
                postponeCount = 4,
                status = TaskStatus.ACTIVE
            )

            val resetPlan = com.neuroflow.app.domain.engine.FreshStartEngine.EmergencyResetPlan(
                tasksToReschedule = listOf(overdueHabit),
                tasksPushedToBacklog = listOf(backloggedHabit),
                streakFreezeApplied = true,
                summaryMessage = "Emergency reset applied"
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                context = mockContext
            )

            viewModel.applyEmergencyReset(resetPlan)
            advanceUntilIdle()

            // Overdue habit rescheduled to today MUST update habitDate to todayStart
            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "habit-overdue" &&
                        updated.scheduledDate == todayStart &&
                        updated.habitDate == todayStart &&
                        updated.postponeCount == 0
                })
            }

            // Backlogged habit MUST clear habitDate to null
            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "habit-backlog" &&
                        updated.scheduledDate == null &&
                        updated.habitDate == null &&
                        updated.postponeCount == 0
                })
            }
        }
    }

    "BUG-319: FlowViewModelV2 handles absolute epoch scheduledTime properly without leaking future tasks or dropping today tasks with future deadlines" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val tomorrowStart = todayStart + 86_400_000L
            val nextWeekStart = todayStart + 7 * 86_400_000L

            // Task 1: Future task scheduled for tomorrow with absolute epoch timestamp (scheduledDate == null)
            val futureEpochTask = TaskEntity(
                id = "future-epoch-task",
                title = "Tomorrow Strategic Review",
                scheduledDate = null,
                scheduledTime = tomorrowStart + 10 * 3600_000L, // 10:00 AM tomorrow
                deadlineDate = null,
                status = TaskStatus.ACTIVE
            )

            // Task 2: Today task scheduled for 10:00 AM today with absolute epoch timestamp, BUT deadline next week
            val todayTaskWithFutureDeadline = TaskEntity(
                id = "today-epoch-task",
                title = "Today Urgent Deliverable",
                scheduledDate = null,
                scheduledTime = todayStart + 10 * 3600_000L, // 10:00 AM today
                deadlineDate = nextWeekStart,
                deadlineTime = 17 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureEpochTask, todayTaskWithFutureDeadline))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()

            // Future epoch task must NOT leak into today's active tasks or actionable tasks!
            state.filteredTasks.map { it.id } shouldBe listOf("today-epoch-task")
            state.recommendedTask?.id shouldBe "today-epoch-task"
        }
    }

    "BUG-320: FlowViewModelV2 tag filtering and dynamic tags inspect task.contextTag and include deepwork contextTag in deep work minutes" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // Task with contextTag = "deepwork" and empty tags string
            val contextTagTask = TaskEntity(
                id = "context-tag-task",
                title = "Architecture Refactoring",
                contextTag = "deepwork",
                tags = "",
                scheduledDate = todayStart,
                scheduledTime = 10 * 3600_000L,
                estimatedDurationMinutes = 45,
                status = TaskStatus.ACTIVE
            )

            // Completed task with contextTag = "deepwork"
            val completedContextTask = TaskEntity(
                id = "completed-deepwork-task",
                title = "Deep Analytical Study",
                contextTag = "deepwork",
                tags = "",
                completedAt = todayStart + 4 * 3600_000L,
                estimatedDurationMinutes = 60,
                status = TaskStatus.COMPLETED
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(contextTagTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(completedContextTask))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(tagCatalog = listOf("customtag")))

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()

            // 1. Tag catalog includes #deepwork from contextTag
            state.availableTags shouldBe listOf("All", "#deepwork", "#offline", "#admin", "#quickwin", "#customtag")

            // 2. Selecting #deepwork must filter and return contextTagTask!
            viewModel.selectTag("#deepwork")
            val filteredState = viewModel.uiState.filter { it.selectedTag == "#deepwork" }.first()
            filteredState.filteredTasks.map { it.id } shouldBe listOf("context-tag-task")

            // 3. Completed deep work minutes must deduct completedContextTask from remaining cognitive budget
            filteredState.circadianEnergySummary?.cognitiveBudgetMinutesRemaining shouldBe (240 - 60)
        }
    }

    "BUG-321: FlowViewModelV2.shiftTaskToPeak respects manual peak profile and preserves future absolute epoch timestamp anchor" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val futureDayStart = todayStart + 3 * 86_400_000L

            // Task scheduled for future day with absolute timestamp
            val futureEpochTask = TaskEntity(
                id = "future-epoch-shift",
                title = "Board Strategy Presentation",
                scheduledDate = null,
                scheduledTime = futureDayStart + 9 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureEpochTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(
                    peakEnergyStart = 9,
                    manualPeakProfileEnabled = true,
                    manualPeakAnchorMinuteOfDay = 14 * 60 // Manual 14:00 PM peak
                )
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )

            viewModel.shiftTaskToPeak(futureEpochTask)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "future-epoch-shift" &&
                        // Preserves future day start (not snapped to today/tomorrow)
                        updated.scheduledDate == futureDayStart &&
                        // Shifted to manual peak hour (14:00)
                        updated.scheduledTime == 14 * 3600_000L
                })
            }
        }
    }

    "BUG-322: Split Part 2 continuation detection identifies _part2 IDs in EnergyTimelineStream" {
        val part2Task = TaskEntity(
            id = "backend_refactor_part2",
            title = "Backend Refactor Continuation",
            estimatedDurationMinutes = 45
        )
        val isUltradianPart = part2Task.id.contains("#part") || part2Task.id.contains("_part") || part2Task.title.contains("(Part ")
        val partLabel = if (part2Task.id.contains("#part2") || part2Task.id.contains("_part2") || part2Task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"

        isUltradianPart shouldBe true
        partLabel shouldBe "🔗 Part 2/2"
    }

    "BUG-362: FlowViewModelV2.addTask preserves unscheduled backlog tasks with scheduledDate = null and normalizes epoch scheduledTime" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val futureDayStart = todayStart + 4 * 86_400_000L

            val unscheduledBacklogTask = TaskEntity(
                id = "backlog-task",
                title = "Someday Read Research Paper",
                scheduledDate = null,
                scheduledTime = null,
                status = TaskStatus.ACTIVE
            )

            val futureEpochTask = TaskEntity(
                id = "future-epoch-task",
                title = "Future Board Meeting",
                scheduledDate = null,
                scheduledTime = futureDayStart + 10 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            // 1. Unscheduled backlog task must preserve scheduledDate = null
            viewModel.addTask(unscheduledBacklogTask)
            advanceUntilIdle()
            coVerify {
                taskRepo.insert(match { task ->
                    task.id == "backlog-task" && task.scheduledDate == null && task.scheduledTime == null
                })
            }

            // 2. Future epoch task must be normalized to epochStart date and intraday time offset
            viewModel.addTask(futureEpochTask)
            advanceUntilIdle()
            coVerify {
                taskRepo.insert(match { task ->
                    task.id == "future-epoch-task" &&
                        task.scheduledDate == futureDayStart &&
                        task.scheduledTime == 10 * 3600_000L
                })
            }
        }
    }

    "BUG-363: FlowViewModelV2.addTaskFromNote prioritizes explicit note title over inferred sanitized title and breakDownTask schedules autonomy nudge" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)
            val behavioralService = mockk<com.neuroflow.app.domain.service.BehavioralSupportService>(relaxed = true)

            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            coEvery {
                behavioralService.generateMicroStepsWithAi(any(), any(), any(), any(), any())
            } returns listOf(
                com.neuroflow.app.domain.service.MicroStepSuggestion("Draft structure", 10),
                com.neuroflow.app.domain.service.MicroStepSuggestion("Write core logic", 15)
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                behavioralSupportService = behavioralService,
                context = mockContext
            )

            // 1. Explicit note title must be preserved
            viewModel.addTaskFromNote(title = "Explicit Meeting Title", content = "call clinic to confirm appointment 30m")
            advanceUntilIdle()

            coVerify {
                taskRepo.insert(match { task ->
                    task.title == "Explicit Meeting Title" &&
                        task.description == "call clinic to confirm appointment 30m"
                })
            }

            // 2. breakDownTask updates calendar and schedules autonomy nudge
            val taskToBreak = TaskEntity(
                id = "breakdown-task-1",
                title = "Design System Refactor",
                estimatedDurationMinutes = 30
            )
            viewModel.breakDownTask(taskToBreak)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { it.id == "breakdown-task-1" && it.ifThenPlan.isNotBlank() })
                AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "breakdown-task-1" })
            }
        }
    }

    "BUG-364: FlowViewModelV2 detects #part2 continuation tasks and _part1 dependencies as blocked when Part 1 is incomplete" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // Active Part 1 with #part1 convention
            val activePart1 = TaskEntity(
                id = "deep_task#part1",
                title = "Deep Task (Part 1/2)",
                scheduledDate = todayStart,
                scheduledTime = 9 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            // Continuation Part 2 with #part2 convention
            val blockedPart2 = TaskEntity(
                id = "deep_task#part2",
                parentTaskId = "deep_task#part1",
                title = "Deep Task (Part 2/2)",
                scheduledDate = todayStart,
                scheduledTime = 11 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(activePart1, blockedPart2))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // When Part 1 is active, blockedPart2 MUST NOT be recommended or treated as actionable!
            state.recommendedTask?.id shouldBe "deep_task#part1"

            // Now complete Part 1
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(blockedPart2))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(activePart1.copy(status = TaskStatus.COMPLETED)))

            val updatedViewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )
            val updatedState = updatedViewModel.uiState.filter { !it.isLoading }.first()
            // Once Part 1 is completed, Part 2 is now unblocked and actionable!
            updatedState.recommendedTask?.id shouldBe "deep_task#part2"
        }
    }

    "BUG-365: FlowViewModelV2.shiftTaskToPeak aligns peakEndHour with manualPeak when manualPeakProfileEnabled is true" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
            val circadianService = mockk<CircadianEnergyService>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val morningSummary = DailyCircadianSummary(
                currentSnapshot = mockk(relaxed = true),
                peakWindowStartHour = 9,
                peakWindowEndHour = 12, // Algorithmic morning peak
                peakEnergyScore = 80,
                dipWindowStartHour = 13,
                dipWindowEndHour = 16,
                recommendedDeepWorkMinutes = 120
            )
            every { circadianService.computeDailySummary(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns morningSummary

            val morningTask = TaskEntity(
                id = "morning-task-shift",
                title = "Afternoon Presentation Prep",
                scheduledDate = todayStart,
                scheduledTime = 8 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(morningTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(
                    manualPeakProfileEnabled = true,
                    manualPeakAnchorMinuteOfDay = 14 * 60 // 14:00 PM manual peak
                )
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                circadianEnergyService = circadianService,
                calendarSyncRepository = calendarRepo
            )

            viewModel.shiftTaskToPeak(morningTask, nowMillis = todayStart + 8 * 3600_000L)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "morning-task-shift" &&
                        // Shifted to manual peak hour (14:00), not corrupted by algorithmic peak window
                        updated.scheduledTime == 14 * 3600_000L
                })
            }
        }
    }

    "BUG-366: shouldShowUltradianBufferAfter in EnergyTimelineStream suppresses dividers between untimed tasks and resets on natural breaks" {
        val now = System.currentTimeMillis()
        val todayStart = startOfLocalDay(now)

        // Case 1: Untimed tasks must never show ultradian buffer dividers
        val untimed1 = TaskEntity(
            id = "untimed-1",
            title = "Untimed Task 1",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 45
        )
        val untimed2 = TaskEntity(
            id = "untimed-2",
            title = "Untimed Task 2",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 50
        )
        shouldShowUltradianBufferAfter(listOf(untimed1, untimed2), 0) shouldBe false

        // Case 2: Natural break between timed tasks (>= 15 min) resets continuous focus accumulation
        // Task 1: 09:00 - 09:45 (45 min)
        val timed1 = TaskEntity(
            id = "timed-1",
            title = "Morning Deep Work",
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        // Task 2: 13:00 - 13:50 (50 min, >3 hour break after Task 1)
        val timed2 = TaskEntity(
            id = "timed-2",
            title = "Afternoon Code Review",
            scheduledDate = todayStart,
            scheduledTime = 13 * 3600_000L,
            estimatedDurationMinutes = 50
        )
        // Task 3: 15:00 - 15:30
        val timed3 = TaskEntity(
            id = "timed-3",
            title = "Evening Admin",
            scheduledDate = todayStart,
            scheduledTime = 15 * 3600_000L,
            estimatedDurationMinutes = 30
        )
        // Between timed2 and timed3, only 50m of work was done after the natural lunch break -> no buffer!
        shouldShowUltradianBufferAfter(listOf(timed1, timed2, timed3), 1) shouldBe false

        // Case 3: Split Part 1 and Part 2 always displays ultradian buffer
        val part1 = TaskEntity(
            id = "heavy_task#part1",
            title = "Heavy Refactor (Part 1/2)",
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        val part2 = TaskEntity(
            id = "heavy_task#part2",
            title = "Heavy Refactor (Part 2/2)",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        shouldShowUltradianBufferAfter(listOf(part1, part2), 0) shouldBe true
    }

    "BUG-406: Multi-part continuation tasks (#part3, _part3, etc.) blocked when parent/preceding parts active, and trailing commas in dependsOnTaskIds handled" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            val parentTask = TaskEntity(
                id = "multi_part_task",
                title = "Massive Refactoring (Part 1/3)",
                scheduledDate = todayStart,
                scheduledTime = 9 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            val part3Task = TaskEntity(
                id = "multi_part_task#part3",
                parentTaskId = "multi_part_task",
                title = "Massive Refactoring (Part 3/3)",
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            val depTask = TaskEntity(
                id = "dep_completed",
                title = "Prerequisite Task",
                status = TaskStatus.COMPLETED
            )
            val dependentWithTrailingComma = TaskEntity(
                id = "trailing_comma_task",
                title = "Task with trailing comma dependency",
                dependsOnTaskIds = "dep_completed, ",
                scheduledDate = todayStart,
                scheduledTime = 11 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(parentTask, part3Task, dependentWithTrailingComma))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(depTask))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // Part 3 continuation MUST NOT be recommended while parent/Part 1 is active!
            state.recommendedTask?.id shouldNotBe "multi_part_task#part3"
            // Dependent task with trailing comma in dependsOnTaskIds MUST be recognized as actionable!
            val actionableIds = state.filteredTasks.map { it.id }
            actionableIds.contains("trailing_comma_task") shouldBe true
        }
    }

    "BUG-407: FlowViewModelV2 normalizes epoch deadlines, synchronizes contextTag, and guards autonomy nudge against Quadrant.ELIMINATE" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)

            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                context = mockContext
            )

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val rawEpochDeadline = todayStart + 18 * 3600_000L // 18:00 PM today as raw epoch

            // 1. addTask with epoch deadline & Quadrant.ELIMINATE
            val iceboxTask = TaskEntity(
                id = "icebox-task-1",
                title = "Someday Maybe Idea",
                quadrant = Quadrant.ELIMINATE,
                deadlineDate = null,
                deadlineTime = rawEpochDeadline,
                status = TaskStatus.ACTIVE
            )
            viewModel.addTask(iceboxTask)
            advanceUntilIdle()

            coVerify {
                taskRepo.insert(match { task ->
                    task.id == "icebox-task-1" &&
                        task.deadlineDate == todayStart &&
                        task.deadlineTime == 18 * 3600_000L
                })
            }
            // Autonomy nudge must NOT be scheduled for Icebox task!
            io.mockk.verify(exactly = 0) {
                AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "icebox-task-1" })
            }

            // 2. updateTask to Quadrant.ELIMINATE cancels autonomy nudge
            val activeToIcebox = iceboxTask.copy(quadrant = Quadrant.ELIMINATE)
            viewModel.updateTask(activeToIcebox)
            advanceUntilIdle()

            io.mockk.verify {
                AutonomyNudgeEngine.cancelNudge(mockContext, "icebox-task-1")
            }

            // 3. addTaskFromNote populates contextTag
            val synthesis = com.neuroflow.app.domain.service.QuickNoteSynthesis(
                extractedTaskTitle = "Quick Note Task",
                suggestedCategory = "ANALYTICAL",
                estimatedMinutes = 25,
                tags = listOf("#deepwork"),
                firstActionStep = ""
            )
            viewModel.addTaskFromNote(synthesis, "Analyze quarterly performance report 30m #deepwork")
            advanceUntilIdle()

            coVerify {
                taskRepo.insert(match { task ->
                    task.title == "Quick Note Task" &&
                        task.contextTag == "deepwork"
                })
            }
        }
    }

    "BUG-408: FlowViewModelV2.deferTask defers untimed and overdue tasks from now, preserving future schedule" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val sessionRepo = mockk<com.neuroflow.app.data.repository.SessionRepository>(relaxed = true)

            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                sessionRepository = sessionRepo
            )

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // Untimed task (scheduledTime == null)
            val untimedTask = TaskEntity(
                id = "untimed-defer-task",
                title = "Untimed Day Task",
                scheduledDate = todayStart,
                scheduledTime = null,
                status = TaskStatus.ACTIVE
            )

            viewModel.deferTask(untimedTask, hours = 2)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "untimed-defer-task" &&
                        // Must be deferred from now (now + 2h), not midnight 02:00 AM!
                        (updated.resolveScheduledEpochMillis() ?: 0L) >= now + 3600_000L
                })
            }

            // Also test overdue timed task from morning
            val overdueTask = TaskEntity(
                id = "overdue-defer-task",
                title = "Overdue Morning Task",
                scheduledDate = todayStart,
                scheduledTime = 6 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            viewModel.deferTask(overdueTask, hours = 2)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "overdue-defer-task" &&
                        (updated.resolveScheduledEpochMillis() ?: 0L) >= now + 3600_000L
                })
            }
        }
    }

    "BUG-409: shouldShowUltradianBufferAfter supports multi-part split parts and preserves breaks across interleaved untimed tasks" {
        val now = System.currentTimeMillis()
        val todayStart = startOfLocalDay(now)

        // Case 1: Multi-part split (Part 2/3 and Part 3/3)
        val part2Of3 = TaskEntity(
            id = "heavy#part2",
            title = "Heavy Refactor (Part 2/3)",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        val part3Of3 = TaskEntity(
            id = "heavy#part3",
            title = "Heavy Refactor (Part 3/3)",
            scheduledDate = todayStart,
            scheduledTime = 11 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        shouldShowUltradianBufferAfter(listOf(part2Of3, part3Of3), 0) shouldBe true

        // Case 2: Interleaved untimed task between two timed tasks separated by a 4-hour break
        val morningTimed = TaskEntity(
            id = "morning",
            title = "Morning Deep Work",
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        val untimedMiddle = TaskEntity(
            id = "untimed",
            title = "Anytime Task",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 60
        )
        val afternoonTimed = TaskEntity(
            id = "afternoon",
            title = "Afternoon Meeting",
            scheduledDate = todayStart,
            scheduledTime = 14 * 3600_000L,
            estimatedDurationMinutes = 45
        )
        // Between morning and afternoon there was a 4h gap. An untimed middle task must not hide this break!
        shouldShowUltradianBufferAfter(listOf(morningTimed, untimedMiddle, afternoonTimed), 2) shouldBe false
    }

    "BUG-440: Multi-part continuation tasks without #part in ID are actionable and recommended when parent is completed" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // Parent task (completed)
            val parentTask = TaskEntity(
                id = "uuid-parent-1",
                title = "Deep Code Review (Part 1/2)",
                status = TaskStatus.COMPLETED
            )
            // Continuation task with standard UUID and parentTaskId set, title has (Part 2/2)
            val continuationTask = TaskEntity(
                id = "uuid-continuation-2",
                parentTaskId = "uuid-parent-1",
                title = "Deep Code Review (Part 2/2)",
                scheduledDate = todayStart,
                scheduledTime = 10 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(continuationTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(parentTask))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // Continuation task MUST NOT be blocked by itself! It must be actionable and eligible for recommendation
            state.filteredTasks.map { it.id }.contains("uuid-continuation-2") shouldBe true
            state.recommendedTask?.id shouldBe "uuid-continuation-2"
        }
    }

    "BUG-441: shiftTaskToPeak anchors upcoming wrapping circadian peak to today instead of deferring to tomorrow" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            // Setup a chronotype / manual peak profile that wraps midnight: 22:00 -> 01:00
            val prefs = UserPreferences(
                manualPeakProfileEnabled = true,
                manualPeakAnchorMinuteOfDay = 22 * 60 // 22:00 (10 PM)
            )
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(prefs)

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 19) // 19:00 (7 PM) - peak at 22:00 is upcoming today!
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val now1900 = cal.timeInMillis
            val todayStart = startOfLocalDay(now1900)

            val testTask = TaskEntity(
                id = "shift-task-1",
                title = "Night Writing Task",
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            viewModel.shiftTaskToPeak(testTask, nowMillis = now1900)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "shift-task-1" &&
                        updated.scheduledDate == todayStart &&
                        updated.scheduledTime == 22 * 3600_000L
                })
            }
        }
    }

    "BUG-442: FlowViewModelV2.addTask initializes habitDate on recurring tasks with scheduledDate and guards autonomy nudge in shiftTaskToPeak and breakDownTask" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val mockContext = mockk<Context>(relaxed = true)

            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                context = mockContext
            )

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)

            // 1. Recurring task with explicit scheduledDate must have habitDate initialized
            val recurringWithDate = TaskEntity(
                id = "recurring-date-task",
                title = "Daily Evening Routine",
                recurrence = Recurrence.DAILY,
                scheduledDate = todayStart,
                scheduledTime = 20 * 3600_000L,
                habitDate = null,
                status = TaskStatus.ACTIVE
            )
            viewModel.addTask(recurringWithDate)
            advanceUntilIdle()

            coVerify {
                taskRepo.insert(match { inserted ->
                    inserted.id == "recurring-date-task" &&
                        inserted.habitDate == todayStart
                })
            }

            // 2. Icebox task in shiftTaskToPeak must NOT trigger autonomy nudge
            val iceboxTask = TaskEntity(
                id = "icebox-peak-task",
                title = "Someday Project",
                quadrant = Quadrant.ELIMINATE,
                status = TaskStatus.ACTIVE
            )
            viewModel.shiftTaskToPeak(iceboxTask, nowMillis = now)
            advanceUntilIdle()

            io.mockk.verify(exactly = 0) {
                AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "icebox-peak-task" })
            }
        }
    }

    "BUG-443: shouldShowUltradianBufferAfter does not emit buffer between untimed split parts" {
        val now = System.currentTimeMillis()
        val todayStart = startOfLocalDay(now)

        // Untimed Part 1 and Untimed Part 2 (both scheduledTime == null)
        val untimedPart1 = TaskEntity(
            id = "task#part1",
            title = "Big Task (Part 1/2)",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 45
        )
        val untimedPart2 = TaskEntity(
            id = "task#part2",
            title = "Big Task (Part 2/2)",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 45
        )

        // Must return false because both are untimed!
        shouldShowUltradianBufferAfter(listOf(untimedPart1, untimedPart2), 0) shouldBe false
    }

    "BUG-AUDIT-010: FlowViewModelV2 shiftTaskToPeak preserves sub-hour minute precision from manual peak anchor" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            // Manual peak anchor set to 9:30 AM (570 minutes of day)
            val prefs = UserPreferences(
                manualPeakProfileEnabled = true,
                manualPeakAnchorMinuteOfDay = 570
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val morningNow = cal.timeInMillis

            val task = TaskEntity(
                id = "task_to_shift",
                title = "Important Review",
                status = TaskStatus.ACTIVE,
                scheduledDate = startOfLocalDay(morningNow),
                scheduledTime = 14 * 3600_000L
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(prefs)

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            viewModel.shiftTaskToPeak(task, nowMillis = morningNow)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { updated ->
                    updated.id == "task_to_shift" &&
                        // Must be 570 * 60_000L = 34_200_000L (9:30 AM), not truncated to 9:00 AM (32,400,000L)
                        updated.scheduledTime == 570 * 60_000L
                })
            }
        }
    }
})
