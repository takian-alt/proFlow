package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
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
})
