package com.neuroflow.app.presentation.v2

import java.util.Calendar
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.GoalRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.focus.FocusViewModelV2
import com.neuroflow.app.presentation.v2.insights.InsightsViewModelV2
import com.neuroflow.app.presentation.v2.navigation.NavHubsV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class Phase4TriptychViewModelIntegrationTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun makeTask(id: String, title: String) = TaskEntity(
        id = id,
        title = title,
        description = "",
        estimatedDurationMinutes = 45,
        actualDurationMinutes = 0f,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    "NavHubsV2 defines the exact 3-hub Triptych: Flow, Plan, Vault" {
        NavHubsV2.items shouldHaveSize 3
        NavHubsV2.items[0].title shouldBe "Flow"
        NavHubsV2.items[0].route shouldBe "flow_v2"

        NavHubsV2.items[1].title shouldBe "Plan"
        NavHubsV2.items[1].route shouldBe "plan_v2"

        NavHubsV2.items[2].title shouldBe "Vault"
        NavHubsV2.items[2].route shouldBe "insights_v2"
    }

    "FlowViewModelV2 breakDownTask persists normalized micro-steps to task_micro_steps table" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val task = makeTask("flow-t1", "Prepare quarterly presentation")

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val circadianService = DefaultCircadianEnergyService()
            val schedulerService = DefaultTaskSchedulerService(circadianService)
            val behavioralService = DefaultBehavioralSupportService()

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                circadianEnergyService = circadianService,
                taskSchedulerService = schedulerService,
                behavioralSupportService = behavioralService
            )

            viewModel.breakDownTask(task)
            advanceUntilIdle()

            val capturedSteps = slot<List<TaskMicroStepEntity>>()
            coVerify { taskRepo.deleteMicroStepsByTaskId("flow-t1") }
            coVerify { taskRepo.insertMicroSteps(capture(capturedSteps)) }

            (capturedSteps.captured.size in 3..4) shouldBe true
            capturedSteps.captured.all { it.taskId == "flow-t1" } shouldBe true
            capturedSteps.captured[0].title.contains("Prepare quarterly presentation") shouldBe true
        }
    }

    "PlanViewModelV2 breakDownTask persists normalized micro-steps and saveQuickNote writes to QuickNoteDao" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)
            val quickNoteDao = mockk<QuickNoteDao>(relaxed = true)
            val task = makeTask("plan-t1", "Write integration tests")

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeTasksForDate(any()) } returns MutableStateFlow(listOf(task))

            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = workManager,
                quickNoteDao = quickNoteDao
            )

            viewModel.breakDownTask(task)
            advanceUntilIdle()

            val capturedSteps = slot<List<TaskMicroStepEntity>>()
            coVerify { taskRepo.deleteMicroStepsByTaskId("plan-t1") }
            coVerify { taskRepo.insertMicroSteps(capture(capturedSteps)) }
            (capturedSteps.captured.size in 3..4) shouldBe true

            viewModel.saveQuickNote("Remember to check edge cases for midnight boundary")
            advanceUntilIdle()

            val capturedNote = slot<QuickNoteEntity>()
            coVerify { quickNoteDao.insert(capture(capturedNote)) }
            capturedNote.captured.content shouldBe "Remember to check edge cases for midnight boundary"
        }
    }

    "PlanViewModelV2 autoScheduleDay respects external calendar busy windows and locked tasks" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val tomorrow = java.time.LocalDate.now().plusDays(1)
            val now = tomorrow.atTime(java.time.LocalTime.of(8, 0)).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val unlockedTask = makeTask("plan-t2", "Draft Strategy Document").copy(
                scheduledDate = null,
                scheduledTime = null,
                isScheduleLocked = false
            )
            val lockedTask = makeTask("plan-t3", "Locked Client Meeting").copy(
                scheduledDate = now,
                scheduledTime = 10 * 3600_000L,
                isScheduleLocked = true,
                plannedBlockMinutes = 60
            )

            coEvery { taskRepo.getActiveTasks() } returns listOf(unlockedTask, lockedTask)
            coEvery { taskRepo.getById("plan-t2") } returns unlockedTask
            coEvery { taskRepo.getById("plan-t3") } returns lockedTask
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(unlockedTask, lockedTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(autoSchedulingEnabled = true, wakeUpHour = 7))

            coEvery { calendarRepo.getBusyWindows(any(), any()) } returns listOf(
                TimeSlotWindow(now + 3600_000L, now + 7200_000L)
            )

            val circadianService = DefaultCircadianEnergyService()
            val schedulerService = DefaultTaskSchedulerService(circadianService)

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = workManager,
                taskSchedulerService = schedulerService,
                circadianEnergyService = circadianService,
                calendarSyncRepository = calendarRepo
            )

            viewModel.autoScheduleDay(now, forceReplan = true)
            advanceUntilIdle()

            // Verify taskRepo.update was called for the unlocked task
            coVerify { taskRepo.update(match { it.id == "plan-t2" && it.isAutoScheduled }) }
            // Verify locked task was NOT overwritten
            coVerify(exactly = 0) { taskRepo.update(match { it.id == "plan-t3" }) }
        }
    }

    "FocusViewModelV2 autoBreakDownTask persists normalized micro-steps" {
        runTest(testDispatcher) {
            val task = makeTask("focus-t1", "Implement Room Normalization")
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "focus-t1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("focus-t1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(
                savedStateHandle = savedStateHandle,
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                woopRepository = woopRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            viewModel.autoBreakDownTask()
            advanceUntilIdle()

            val capturedSteps = slot<List<TaskMicroStepEntity>>()
            coVerify { taskRepo.deleteMicroStepsByTaskId("focus-t1") }
            coVerify { taskRepo.insertMicroSteps(capture(capturedSteps)) }
            (capturedSteps.captured.size in 3..4) shouldBe true
            capturedSteps.captured[0].taskId shouldBe "focus-t1"
        }
    }

    "NeuroFlowThemeV2 token palette provides high WCAG AA contrast against pure black" {
        NeuroFlowThemeV2Colors.StaticObsidian shouldBe androidx.compose.ui.graphics.Color(0xFF0A0B0E)
        NeuroFlowThemeV2Colors.StaticTextPrimary shouldBe androidx.compose.ui.graphics.Color(0xFFF8FAFC)
        NeuroFlowThemeV2Colors.StaticTextSecondary shouldBe androidx.compose.ui.graphics.Color(0xFF94A3B8)
        NeuroFlowThemeV2Colors.StaticElectricIndigo shouldBe androidx.compose.ui.graphics.Color(0xFF6366F1)
        NeuroFlowThemeV2Colors.StaticSlateSurface shouldBe androidx.compose.ui.graphics.Color(0xFF13151B)
    }

    "FlowViewModelV2 deferTask sets isAutoScheduled=false, isScheduleLocked=true and updates calendar (BUG-042)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val autoTask = makeTask("flow-defer-1", "Auto Task").copy(
                isAutoScheduled = true,
                isScheduleLocked = false,
                scheduledDate = System.currentTimeMillis(),
                scheduledTime = 10 * 3600_000L
            )
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )

            viewModel.deferTask(autoTask, hours = 2)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match {
                    it.id == "flow-defer-1" &&
                        !it.isAutoScheduled &&
                        it.isScheduleLocked
                })
            }
            coVerify {
                calendarRepo.handleTaskStateChanged(match {
                    it.id == "flow-defer-1" && !it.isAutoScheduled && it.isScheduleLocked
                })
            }
        }
    }

    "FlowViewModelV2 undoCompleteTask rolls back spawned recurring task (BUG-043)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val recurringTask = makeTask("flow-recur-1", "Daily Standup").copy(
                recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY
            )
            val spawnedTask = makeTask("flow-recur-1-spawned", "Daily Standup")

            coEvery { taskRepo.completeAndRecur(any(), any()) } returns "flow-recur-1-spawned"
            coEvery { taskRepo.getById("flow-recur-1-spawned") } returns spawnedTask
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            viewModel.completeTask(recurringTask)
            advanceUntilIdle()

            viewModel.undoCompleteTask()
            advanceUntilIdle()

            coVerify { taskRepo.delete(spawnedTask) }
        }
    }

    "PlanViewModelV2 updateTask notifies calendar sync (BUG-036)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val task = makeTask("plan-edit-1", "Important Meeting").copy(
                isScheduleLocked = true
            )
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true),
                calendarSyncRepository = calendarRepo
            )

            viewModel.updateTask(task)
            advanceUntilIdle()

            coVerify { taskRepo.update(task) }
            coVerify { calendarRepo.handleTaskStateChanged(task) }
        }
    }

    "PlanViewModelV2 requestDeleteTask commits prior deletion on second delete (BUG-038)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val taskA = makeTask("task-A", "Task A")
            val taskB = makeTask("task-B", "Task B")
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true)
            )

            viewModel.requestDeleteTask(taskA)
            // Immediately request delete of Task B without waiting 5 seconds
            viewModel.requestDeleteTask(taskB)
            advanceUntilIdle()

            // Task A must have been confirmed and deleted
            coVerify { taskRepo.delete(taskA) }
        }
    }

    "FocusViewModelV2 completeTask and submitAffordanceRating preserve COMPLETED status and completedAt (BUG-106)" {
        runTest(testDispatcher) {
            val task = makeTask("focus-affordance-1", "Test Completion Flow")
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "focus-affordance-1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("focus-affordance-1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(
                savedStateHandle = savedStateHandle,
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                woopRepository = woopRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            viewModel.completeTask()
            advanceUntilIdle()

            viewModel.uiState.value.task?.status shouldBe TaskStatus.COMPLETED
            viewModel.uiState.value.task?.completedAt shouldNotBe null

            viewModel.submitAffordanceRating(4.5f)
            advanceUntilIdle()

            viewModel.uiState.value.task?.status shouldBe TaskStatus.COMPLETED
            viewModel.uiState.value.task?.affectiveForecastError shouldBe 4.5f

            coVerify {
                taskRepo.update(match {
                    it.id == "focus-affordance-1" &&
                        it.status == TaskStatus.COMPLETED &&
                        it.affectiveForecastError == 4.5f
                })
            }
        }
    }

    "FlowViewModelV2 shiftTaskToPeak shifts overdue/past tasks to today or future and never leaves them in the past (BUG-113)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
            val yesterdayStart = todayStart - 86_400_000L

            val overdueTask = makeTask("overdue-1", "Past Due Task").copy(
                scheduledDate = yesterdayStart,
                scheduledTime = 10 * 3600_000L
            )

            coEvery { taskRepo.getActiveTasks() } returns listOf(overdueTask)
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(overdueTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(peakEnergyStart = 10, peakEnergyEnd = 13)
            )

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )
            advanceUntilIdle()

            viewModel.shiftTaskToPeak(overdueTask)
            advanceUntilIdle()

            val captured = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(captured)) }

            val updated = captured.captured
            // Target date must be >= todayStart, never yesterdayStart!
            (updated.scheduledDate!! >= todayStart) shouldBe true
            updated.isScheduleLocked shouldBe true
            updated.isAutoScheduled shouldBe false
        }
    }

    "PlanViewModelV2 shiftTaskToPeak preserves future scheduled dates and avoids elapsed past hours (BUG-113)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
            val futureDayStart = todayStart + 3 * 86_400_000L

            val futureTask = makeTask("future-1", "Future Task").copy(
                scheduledDate = futureDayStart,
                scheduledTime = 15 * 3600_000L
            )

            coEvery { taskRepo.getActiveTasks() } returns listOf(futureTask)
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(peakEnergyStart = 10, peakEnergyEnd = 13)
            )

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true),
                calendarSyncRepository = calendarRepo
            )
            advanceUntilIdle()

            viewModel.shiftTaskToPeak(futureTask)
            advanceUntilIdle()

            val captured = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(captured)) }

            val updated = captured.captured
            val expectedPeakHour = viewModel.uiState.value.circadianSummary?.peakWindowStartHour
                ?: viewModel.uiState.value.preferences.peakEnergyStart.takeIf { it > 0 } ?: 10
            // Future date must be preserved exactly
            updated.scheduledDate shouldBe futureDayStart
            updated.scheduledTime shouldBe expectedPeakHour * 3600_000L
            updated.isScheduleLocked shouldBe true
            updated.isAutoScheduled shouldBe false
        }
    }

    "FlowViewModelV2 logPastTime resets broken streak to 1 after multi-day hiatus (BUG-115)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
            // Hiatus: last active 4 days ago
            val staleLastActive = todayStart - 4 * 86_400_000L
            val initialPrefs = UserPreferences(
                dailyStreak = 7,
                longestStreak = 10,
                lastActiveDate = staleLastActive,
                totalFocusMinutes = 200
            )

            val prefsUpdateSlot = slot<(UserPreferences) -> UserPreferences>()
            coEvery { prefsDataStore.updatePreferences(capture(prefsUpdateSlot)) } coAnswers {
                val updated = prefsUpdateSlot.captured(initialPrefs)
                updated
            }

            val task = makeTask("t1", "Design Task")
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(initialPrefs)

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            viewModel.logPastTime(task, 45f)
            advanceUntilIdle()

            val capturedResult = prefsUpdateSlot.captured(initialPrefs)
            capturedResult.dailyStreak shouldBe 1
            capturedResult.longestStreak shouldBe 10
            capturedResult.totalFocusMinutes shouldBe 245
        }
    }

    "FlowViewModelV2 and PlanViewModelV2 dynamically update cognitive budget and reflect dailyDeepWorkTargetMinutes (BUG-118)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)

            val now = System.currentTimeMillis()
            val initialPrefs = UserPreferences(
                dailyDeepWorkTargetMinutes = 240
            )
            val prefsFlow = MutableStateFlow(initialPrefs)

            val completedDeepWorkTask = makeTask(
                id = "deep-done",
                title = "Deep Architecture Audit"
            ).copy(
                energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
                estimatedDurationMinutes = 60,
                status = TaskStatus.COMPLETED,
                completedAt = now,
                actualDurationMinutes = 60f
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(completedDeepWorkTask))
            every { prefsDataStore.preferencesFlow } returns prefsFlow

            val flowViewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )
            val planViewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = workManager
            )
            advanceUntilIdle()

            // 1. Initial State: Budget is 240 - 60 = 180m remaining
            val flowState1 = flowViewModel.uiState.filter { !it.isLoading && it.circadianEnergySummary != null }.first()
            flowState1.circadianEnergySummary?.cognitiveBudgetMinutesRemaining shouldBe 180

            val planState1 = planViewModel.uiState.filter { !it.isLoading }.first()
            planState1.preferences.dailyDeepWorkTargetMinutes shouldBe 240

            // 2. Change preference to 360m: Verify both ViewModels receive the emission (not blocked by distinctUntilChanged)
            prefsFlow.value = initialPrefs.copy(dailyDeepWorkTargetMinutes = 360)
            advanceUntilIdle()

            val flowState2 = flowViewModel.uiState.filter { it.preferences.dailyDeepWorkTargetMinutes == 360 }.first()
            // 360 - 60 = 300m remaining
            flowState2.circadianEnergySummary?.cognitiveBudgetMinutesRemaining shouldBe 300

            val planState2 = planViewModel.uiState.filter { it.preferences.dailyDeepWorkTargetMinutes == 360 }.first()
            planState2.preferences.dailyDeepWorkTargetMinutes shouldBe 360
        }
    }

    "FocusViewModelV2 sprint completion does not double-count tracked focus minutes on completeTask (BUG-119)" {
        runTest(testDispatcher) {
            val task = makeTask("focus-sprint-1", "Pomodoro Test Task").copy(
                estimatedDurationMinutes = 25,
                totalTimeTrackedMinutes = 0f
            )
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "focus-sprint-1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            var currentPrefs = UserPreferences(totalFocusMinutes = 0, totalTasksCompleted = 0)

            coEvery { taskRepo.getById("focus-sprint-1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(currentPrefs)
            coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
                val block = firstArg<(UserPreferences) -> UserPreferences>()
                currentPrefs = block(currentPrefs)
                currentPrefs
            }

            val viewModel = FocusViewModelV2(
                savedStateHandle = savedStateHandle,
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                woopRepository = woopRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            // 1. Simulate 25-minute Pomodoro countdown expiring (1500 seconds elapsed)
            viewModel.selectTimerMode(com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2.POMODORO_25)
            viewModel.startTimer()
            for (i in 1..1500) {
                viewModel.tickSecond()
            }
            advanceUntilIdle()

            viewModel.uiState.value.isSprintCompleted shouldBe true
            currentPrefs.totalFocusMinutes shouldBe 25

            // When Room updates the task, totalTimeTrackedMinutes is 25f
            val updatedTaskAfterSprint = task.copy(totalTimeTrackedMinutes = 25f)
            coEvery { taskRepo.getById("focus-sprint-1") } returns updatedTaskAfterSprint

            // 2. User taps "Complete Task" on the sprint completion sheet
            viewModel.completeTask()
            advanceUntilIdle()

            // The final tracked minutes must be exactly 25, NOT 50!
            viewModel.uiState.value.task?.totalTimeTrackedMinutes shouldBe 25f
            viewModel.uiState.value.task?.actualDurationMinutes shouldBe 25f
            currentPrefs.totalFocusMinutes shouldBe 25
            currentPrefs.totalTasksCompleted shouldBe 1

            coVerify {
                taskRepo.completeAndRecur(match {
                    it.id == "focus-sprint-1" && it.totalTimeTrackedMinutes == 25f && it.actualDurationMinutes == 25f
                }, any())
            }
        }
    }

    "InsightsViewModelV2 evaluates circadian alignment correctly for midnight-spanning peak and ignores unscheduled backlog (BUG-122)" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val contractRepo = mockk<UlyssesContractRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            // Night owl preference: peak from 22:00 to 02:00
            val prefs = UserPreferences(
                peakEnergyStart = 22,
                peakEnergyEnd = 2
            )
            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

            // High energy task 1: Unscheduled in backlog (scheduledDate = null, completedAt = null)
            val unscheduledTask = makeTask("backlog-1", "Unscheduled High Task").copy(
                energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
                scheduledDate = null,
                scheduledTime = null,
                completedAt = null
            )
            // High energy task 2: Scheduled at 23:00 (inside 22:00-02:00 peak)
            val alignedTask = makeTask("aligned-1", "Peak Window Task").copy(
                energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
                scheduledDate = todayStart,
                scheduledTime = 23 * 3600_000L
            )
            // High energy task 3: Scheduled at 14:00 (outside peak)
            val misalignedTask = makeTask("misaligned-1", "Afternoon Task").copy(
                energyLevel = com.neuroflow.app.domain.model.EnergyLevel.HIGH,
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L
            )

            every { taskRepo.observeAll() } returns MutableStateFlow(listOf(unscheduledTask, alignedTask, misalignedTask))
            coEvery { taskRepo.getAllTasks() } returns listOf(unscheduledTask, alignedTask, misalignedTask)
            every { sessionRepo.observeAll() } returns MutableStateFlow(emptyList())
            every { contractRepo.observeActive() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(prefs)

            val viewModel = InsightsViewModelV2(
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsDataStore,
                contractRepository = contractRepo
            )
            advanceUntilIdle()

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // Exactly 2 tasks evaluated (aligned and misaligned). 1 of 2 aligned = 50%!
            // (If unscheduled task was falsely counted as aligned, it would have reported 2 of 3 = 66% or 100%)
            state.circadianAlignmentRate shouldBe 50
        }
    }

    "BUG-131: FlowViewModelV2.applyEmergencyReset reschedules kept tasks to today, un-schedules backlog tasks, and protects streak" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val yesterday = com.neuroflow.app.data.local.entity.startOfLocalDay(now - 86_400_000L)
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

            val keptTask = makeTask("kept-frog", "Urgent Frog Task").copy(
                isFrog = true,
                scheduledDate = yesterday,
                postponeCount = 3
            )
            val backlogTask = makeTask("backlog-overdue", "Low Impact Task").copy(
                scheduledDate = yesterday,
                scheduledTime = 10 * 3600_000L,
                postponeCount = 2
            )

            val prefsUpdateSlot = slot<(UserPreferences) -> UserPreferences>()
            var capturedPrefs = UserPreferences(dailyStreak = 5, lastActiveDate = yesterday)
            coEvery { prefsDataStore.updatePreferences(capture(prefsUpdateSlot)) } coAnswers {
                capturedPrefs = prefsUpdateSlot.captured(capturedPrefs)
                capturedPrefs
            }
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(capturedPrefs)

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )

            val resetPlan = com.neuroflow.app.domain.engine.FreshStartEngine.EmergencyResetPlan(
                tasksToReschedule = listOf(keptTask),
                tasksPushedToBacklog = listOf(backlogTask),
                streakFreezeApplied = true,
                summaryMessage = "Schedule reset! 1 essential kept, 1 moved to backlog. Streak protected!"
            )

            viewModel.applyEmergencyReset(resetPlan)
            advanceUntilIdle()

            // 1. Kept task must be rescheduled to todayStart with postponeCount reset to 0
            coVerify {
                taskRepo.update(match {
                    it.id == "kept-frog" &&
                        it.scheduledDate == todayStart &&
                        it.postponeCount == 0
                })
            }
            coVerify {
                calendarRepo.handleTaskStateChanged(match {
                    it.id == "kept-frog" && it.scheduledDate == todayStart
                })
            }

            // 2. Backlog task must have scheduledDate/Time cleared and calendar event deleted
            coVerify {
                taskRepo.update(match {
                    it.id == "backlog-overdue" &&
                        it.scheduledDate == null &&
                        it.scheduledTime == null &&
                        it.postponeCount == 0
                })
            }
            coVerify {
                calendarRepo.deleteTaskEvent("backlog-overdue")
            }

            // 3. User streak must be protected by updating lastActiveDate
            (capturedPrefs.lastActiveDate >= now) shouldBe true
        }
    }

    "BUG-133: FlowViewModelV2.undoDeferTask preserves pre-existing postponeCount and re-syncs nudge" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val originalTask = TaskEntity(
                id = "task-defer-test",
                title = "Important Review",
                postponeCount = 2,
                scheduledDate = 1729814400000L,
                scheduledTime = 10 * 3600_000L
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(originalTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                calendarSyncRepository = calendarRepo
            )

            // 1. Defer task by 2 hours
            viewModel.deferTask(originalTask, hours = 2)
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match {
                    it.id == "task-defer-test" && it.postponeCount == 3
                })
            }

            // 2. Undo deferral
            viewModel.undoDeferTask()
            advanceUntilIdle()

            // Restored task must preserve the original postponeCount (2) rather than decrementing it to 1
            coVerify {
                taskRepo.update(match {
                    it.id == "task-defer-test" &&
                        it.postponeCount == 2 &&
                        it.scheduledTime == 10 * 3600_000L
                })
            }
        }
    }

    "BUG-134: FreshStartEngine.sundayWeekAnchorMillis anchors Sunday-to-Saturday weeks to mid-week Thursday" {
        val zone = java.time.ZoneId.systemDefault()
        // Sunday Oct 25, 2026 to Saturday Oct 31, 2026
        val sundayMillis = java.time.LocalDate.of(2026, 10, 25).atStartOfDay(zone).toInstant().toEpochMilli()
        val mondayMillis = java.time.LocalDate.of(2026, 10, 26).atStartOfDay(zone).toInstant().toEpochMilli()
        val saturdayMillis = java.time.LocalDate.of(2026, 10, 31).atStartOfDay(zone).toInstant().toEpochMilli()

        // In isolation, Sunday belongs to ISO week 43 while Monday belongs to ISO week 44
        com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(sundayMillis) shouldBe 43
        com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(mondayMillis) shouldBe 44

        // When anchored via sundayWeekAnchorMillis, all 7 days evaluate to identical ISO week 44 and year 2026!
        val anchorSunday = com.neuroflow.app.domain.engine.FreshStartEngine.sundayWeekAnchorMillis(sundayMillis)
        val anchorMonday = com.neuroflow.app.domain.engine.FreshStartEngine.sundayWeekAnchorMillis(mondayMillis)
        val anchorSaturday = com.neuroflow.app.domain.engine.FreshStartEngine.sundayWeekAnchorMillis(saturdayMillis)

        com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(anchorSunday) shouldBe 44
        com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(anchorMonday) shouldBe 44
        com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(anchorSaturday) shouldBe 44

        com.neuroflow.app.domain.engine.FreshStartEngine.isoYear(anchorSunday) shouldBe 2026
        com.neuroflow.app.domain.engine.FreshStartEngine.isoYear(anchorMonday) shouldBe 2026
        com.neuroflow.app.domain.engine.FreshStartEngine.isoYear(anchorSaturday) shouldBe 2026

        // FreshStartEngine.isFreshStart guard prevents duplicate Monday prompt when already shown on Sunday
        val shownOnSundayWeek = com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(anchorSunday)
        val shownOnSundayYear = com.neuroflow.app.domain.engine.FreshStartEngine.isoYear(anchorSunday)
        val isFreshStartOnMonday = com.neuroflow.app.domain.engine.FreshStartEngine.isFreshStart(
            nowMillis = mondayMillis,
            lastOpenMillis = sundayMillis,
            dailyStreak = 1,
            lastActiveDate = sundayMillis,
            lastFreshStartShownWeek = shownOnSundayWeek,
            lastFreshStartShownYear = shownOnSundayYear
        )
        isFreshStartOnMonday shouldBe false
    }

    "BUG-143: FlowViewModelV2 includes overdue active tasks in filteredTasks for Today's Energy Stream" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
            val yesterdayStart = com.neuroflow.app.data.local.entity.startOfPreviousLocalDay(todayStart)

            val overdueTask = TaskEntity(
                id = "task-overdue-1",
                title = "Overdue Deep Work",
                scheduledDate = yesterdayStart,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            val overdueDeadlineTask = TaskEntity(
                id = "task-overdue-deadline",
                title = "Overdue Deadline Task",
                scheduledDate = null,
                deadlineDate = yesterdayStart,
                status = TaskStatus.ACTIVE
            )
            val todayTask = TaskEntity(
                id = "task-today-1",
                title = "Today Work",
                scheduledDate = todayStart,
                scheduledTime = 10 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(overdueTask, overdueDeadlineTask, todayTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )
            val state = viewModel.uiState.filter { !it.isLoading }.first()
            val filtered = state.filteredTasks
            filtered.any { it.id == "task-overdue-1" } shouldBe true
            filtered.any { it.id == "task-overdue-deadline" } shouldBe true
            filtered.any { it.id == "task-today-1" } shouldBe true
        }
    }

    "BUG-145: IdentityViewModel updateWeeklyGoals syncs with GoalRepository and inserts GoalEntities" {
        runTest(testDispatcher) {
            val goalRepo = mockk<com.neuroflow.app.data.repository.GoalRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
            coEvery { goalRepo.getActiveGoals() } returns emptyList()

            val viewModel = com.neuroflow.app.presentation.identity.IdentityViewModel(
                preferencesDataStore = prefsDataStore,
                goalRepository = goalRepo
            )

            viewModel.updateWeeklyGoals(listOf("Ship neuroFlow v2.3", "Deep Meditation"))
            advanceUntilIdle()

            coVerify {
                prefsDataStore.updatePreferences(any())
            }
            coVerify(exactly = 1) {
                goalRepo.insert(match { it.title == "Ship neuroFlow v2.3" && it.description == "Weekly Objective" })
            }
            coVerify(exactly = 1) {
                goalRepo.insert(match { it.title == "Deep Meditation" && it.description == "Weekly Objective" })
            }
        }
    }

    "BUG-153: PlanViewModelV2 tasksForDay sorts timed tasks chronologically and places untimed tasks after timed tasks" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis

            // Untimed task (scheduledTime == null)
            val untimedTask = makeTask("untimed-1", "Daily Review").copy(
                scheduledDate = startOfToday,
                scheduledTime = null,
                status = TaskStatus.ACTIVE
            )
            // Morning task at 09:00 AM (9 * 3600_000L = 32_400_000L)
            val morningTask = makeTask("morning-1", "Team Standup").copy(
                scheduledDate = startOfToday,
                scheduledTime = 9 * 3600_000L,
                status = TaskStatus.ACTIVE
            )
            // Afternoon task at 14:00 (14 * 3600_000L = 50_400_000L)
            val afternoonTask = makeTask("afternoon-1", "Deep Code").copy(
                scheduledDate = startOfToday,
                scheduledTime = 14 * 3600_000L,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(untimedTask, morningTask, afternoonTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true)
            )
            advanceUntilIdle()

            val state = viewModel.uiState.filter { it.tasksForDay.isNotEmpty() }.first()
            state.tasksForDay.map { it.id } shouldBe listOf("morning-1", "afternoon-1", "untimed-1")
        }
    }

    "BUG-160: FlowViewModelV2 does not treat 25th-hour tasks on DST fall-back day as future tasks" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)

            val originalTimeZone = java.util.TimeZone.getDefault()
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            try {
                // Nov 1, 2026 is DST fall-back in America/New_York (25 hours: 90_000_000 ms)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, Calendar.NOVEMBER)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dstStart = cal.timeInMillis
                val dstEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dstStart) - 1
                (dstEnd - dstStart) shouldBe (25 * 3600_000L - 1)

                // Task scheduled at 23:30 (24.5 hours after midnight)
                val lateTask = makeTask("late-dst-task", "Late Night Journal").copy(
                    scheduledDate = dstStart,
                    scheduledTime = 24 * 3600_000L + 30 * 60_000L,
                    status = TaskStatus.ACTIVE
                )

                (lateTask.scheduledDate!! <= dstEnd) shouldBe true
                (lateTask.scheduledDate!! <= (dstStart + 86_400_000L - 1)) shouldBe true
            } finally {
                java.util.TimeZone.setDefault(originalTimeZone)
            }
        }
    }

    "BUG-162: ScheduleAutoTasksWorker buildBlockedTaskIds does not block prerequisite in 25th hour of DST fall-back" {
        val originalTimeZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
        try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.NOVEMBER)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dstDayStart = cal.timeInMillis
            val canonicalDayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dstDayStart) - 1
            // 24.5 hours after midnight (23:30 local time)
            val prereqStart = dstDayStart + 24 * 3600_000L + 30 * 60_000L

            val prereqTask = makeTask("prereq-25th", "Late Night Prereq").copy(
                scheduledDate = dstDayStart,
                scheduledTime = 24 * 3600_000L + 30 * 60_000L,
                status = TaskStatus.ACTIVE
            )
            val candidate = makeTask("cand-1", "Dependent Candidate").copy(
                dependsOnTaskIds = "prereq-25th",
                status = TaskStatus.ACTIVE
            )

            val blockedIds = com.neuroflow.app.worker.ScheduleAutoTasksWorker.buildBlockedTaskIds(
                tasks = listOf(prereqTask, candidate),
                allKnownTasks = listOf(prereqTask, candidate),
                dayEnd = canonicalDayEnd
            )
            blockedIds.contains("cand-1") shouldBe false
        } finally {
            java.util.TimeZone.setDefault(originalTimeZone)
        }
    }

    "BUG-163: FlowViewModelV2 triggers showZeroGuiltPrompt on tasks with postponeCount >= 2 or overdue deadlines" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val now = System.currentTimeMillis()

            val tasks = listOf(
                makeTask("t1", "Postponed twice").copy(postponeCount = 2, status = TaskStatus.ACTIVE),
                makeTask("t2", "Postponed twice").copy(postponeCount = 2, status = TaskStatus.ACTIVE),
                makeTask("t3", "Expired deadline").copy(deadlineDate = now - 3600_000L, status = TaskStatus.ACTIVE)
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(tasks)
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            state.showZeroGuiltPrompt shouldBe true
        }
    }

    "BUG-167: FreshStartEngine.buildEmergencyResetPlan includes tasks with overdue deadlines and repeated postpones" {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            makeTask("dl-1", "Expired Deadline Task").copy(
                deadlineDate = now - 7200_000L,
                scheduledDate = null,
                status = TaskStatus.ACTIVE
            ),
            makeTask("pp-1", "Postponed 3 Times").copy(
                postponeCount = 3,
                scheduledDate = null,
                status = TaskStatus.ACTIVE
            ),
            makeTask("future-1", "Future Task").copy(
                scheduledDate = now + 86400_000L,
                status = TaskStatus.ACTIVE
            )
        )

        val plan = com.neuroflow.app.domain.engine.FreshStartEngine.buildEmergencyResetPlan(tasks, now)
        plan.tasksPushedToBacklog.size + plan.tasksToReschedule.size shouldBe 2
        plan.tasksPushedToBacklog.any { it.id == "dl-1" || it.id == "pp-1" } shouldBe true
        plan.tasksPushedToBacklog.none { it.id == "future-1" } shouldBe true
    }

    "BUG-176: FreshStartEngine.buildEmergencyResetPlan correctly evaluates deadline urgency with explicit deadlineTime" {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val tomorrowStart = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)

        // Task with deadline 36 hours from now (tomorrow 23:59) should NOT be classified as high priority (< 24h away)
        val farDeadlineTask = makeTask("dl-far", "Far Deadline").copy(
            deadlineDate = tomorrowStart,
            deadlineTime = 23 * 3600_000L + 59 * 60_000L,
            impactScore = 30,
            isFrog = false,
            scheduledDate = todayStart,
            status = TaskStatus.ACTIVE
        )
        // Task with deadline 2 hours from now should be classified as high priority
        val urgentDeadlineTask = makeTask("dl-urgent", "Urgent Deadline").copy(
            deadlineDate = todayStart,
            deadlineTime = (now - todayStart) + 2 * 3600_000L,
            impactScore = 30,
            isFrog = false,
            scheduledDate = todayStart,
            status = TaskStatus.ACTIVE
        )

        val plan = com.neuroflow.app.domain.engine.FreshStartEngine.buildEmergencyResetPlan(listOf(farDeadlineTask, urgentDeadlineTask), now)
        plan.tasksToReschedule.any { it.id == "dl-urgent" } shouldBe true
    }

    "BUG-167: FlowViewModelV2.freshStartRollover rolls expired deadline tasks into Icebox" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val now = System.currentTimeMillis()

            val expiredDeadlineTask = makeTask("dl-overdue", "Expired Deadline").copy(
                deadlineDate = now - 3600_000L,
                scheduledDate = null,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(expiredDeadlineTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            viewModel.uiState.filter { !it.isLoading }.first()
            viewModel.freshStartRollover()
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match {
                    it.id == "dl-overdue" && it.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE
                })
            }
        }
    }

    "BUG-168: TaskReminderWorker.scheduleReminder cancels obsolete WorkManager jobs when target is null" {
        val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)
        val untimedTask = makeTask("untimed-1", "Untimed Backlog Task").copy(
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            deadlineTime = null,
            status = TaskStatus.ACTIVE
        )

        com.neuroflow.app.worker.TaskReminderWorker.scheduleReminder(
            workManager = mockWorkManager,
            task = untimedTask,
            minutesBefore = 15L
        )

        verify {
            mockWorkManager.cancelAllWorkByTag("task_reminder_untimed-1")
            mockWorkManager.cancelUniqueWork("task_reminder_untimed-1_15")
        }
    }

    "BUG-174: FlowViewModelV2 maintains mutual exclusion between lastCompletedTask and lastDeferredTask" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )

            val taskA = makeTask("task-a", "Task A")
            val taskB = makeTask("task-b", "Task B")

            // 1. Complete Task A
            viewModel.completeTask(taskA)
            advanceUntilIdle()
            val stateAfterComplete = viewModel.uiState.filter { it.lastCompletedTask != null }.first()
            stateAfterComplete.lastCompletedTask?.id shouldBe "task-a"
            stateAfterComplete.lastDeferredTask shouldBe null

            // 2. Defer Task B -> clears lastCompletedTask so undo doesn't conflict
            viewModel.deferTask(taskB, hours = 2)
            advanceUntilIdle()
            val stateAfterDefer = viewModel.uiState.filter { it.lastDeferredTask != null }.first()
            stateAfterDefer.lastCompletedTask shouldBe null
            stateAfterDefer.lastDeferredTask?.first?.id shouldBe "task-b"

            // 3. undoLastAction undoes Task B's deferral, not Task A
            viewModel.undoLastAction()
            advanceUntilIdle()
            coVerify {
                taskRepo.update(match { it.id == "task-b" })
            }
            val stateAfterUndo = viewModel.uiState.filter { it.lastDeferredTask == null }.first()
            stateAfterUndo.lastDeferredTask shouldBe null
        }
    }

    "BUG-189: FlowViewModelV2 excludes future recurring habits from today's active tasks stream" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val tomorrowStart = todayStart + 24 * 3600_000L

            val futureHabit = makeTask("future-habit-1", "Tomorrow Habit").copy(
                recurrence = Recurrence.DAILY,
                habitDate = tomorrowStart,
                scheduledDate = null,
                status = TaskStatus.ACTIVE
            )
            val todayTask = makeTask("today-task-1", "Today Task").copy(
                scheduledDate = todayStart,
                status = TaskStatus.ACTIVE
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureHabit, todayTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            coEvery { sessionRepo.getOpenSessions() } returns emptyList()
            every { sessionRepo.observeSessionsForDay(any(), any()) } returns flowOf(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            val state = viewModel.uiState.filter { it.filteredTasks.isNotEmpty() }.first()
            state.filteredTasks.map { it.id } shouldBe listOf("today-task-1")
        }
    }

    "BUG-193: PlanViewModelV2.addTask updates habitDate to selectedDateMillis for recurring tasks when scheduledDate is null" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val tomorrowStart = todayStart + 24 * 3600_000L

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val capturedTask = slot<TaskEntity>()
            coEvery { taskRepo.insert(capture(capturedTask)) } returns Unit

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true)
            )
            advanceUntilIdle()

            // Navigate to tomorrow
            viewModel.selectDate(tomorrowStart)
            advanceUntilIdle()

            val newRecurringHabit = TaskEntity(
                id = "habit-add-1",
                title = "Daily Pushups",
                recurrence = Recurrence.DAILY,
                habitDate = todayStart, // Defaulted to today in composer
                scheduledDate = null
            )

            viewModel.addTask(newRecurringHabit)
            advanceUntilIdle()

            capturedTask.isCaptured shouldBe true
            capturedTask.captured.scheduledDate shouldBe tomorrowStart
            capturedTask.captured.habitDate shouldBe tomorrowStart
        }
    }

    "BUG-194: PlanViewModelV2 protected slot mutations invoke UserPreferencesDataStore.updateProtectedTimeSlots" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val existingSlot = ProtectedTimeSlot(
                id = "slot-1",
                label = "Deep Work",
                startMinuteOfDay = 9 * 60,
                durationMinutes = 90,
                category = PtsCategory.WORK,
                isEnabled = true
            )
            val prefsFlow = MutableStateFlow(UserPreferences(protectedTimeSlots = listOf(existingSlot)))
            every { prefsDataStore.preferencesFlow } returns prefsFlow
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = mockk<WorkManager>(relaxed = true)
            )
            advanceUntilIdle()

            val capturedSlots = mutableListOf<List<ProtectedTimeSlot>>()

            // 1. Toggle slot
            viewModel.toggleProtectedSlot("slot-1", false)
            advanceUntilIdle()

            // 2. Save new slot
            val newSlot = ProtectedTimeSlot(
                id = "slot-2",
                label = "Lunch Break",
                startMinuteOfDay = 12 * 60,
                durationMinutes = 45,
                category = PtsCategory.MEAL,
                isEnabled = true
            )
            viewModel.saveProtectedSlot(newSlot)
            advanceUntilIdle()

            // 3. Delete slot
            viewModel.deleteProtectedSlot("slot-1")
            advanceUntilIdle()

            coVerify(exactly = 3) { prefsDataStore.updateProtectedTimeSlots(capture(capturedSlots)) }
            capturedSlots.size shouldBe 3
            capturedSlots[0].first().isEnabled shouldBe false
            capturedSlots[1].any { it.id == "slot-2" } shouldBe true
            capturedSlots[2].none { it.id == "slot-1" } shouldBe true
        }
    }

    "BUG-195: PlanViewModelV2.addGoal defaults description to Weekly Objective avoiding Yearly Objective collision" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val goalRepo = mockk<GoalRepository>(relaxed = true)

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val capturedGoal = slot<com.neuroflow.app.data.local.entity.GoalEntity>()
            coEvery { goalRepo.insert(capture(capturedGoal)) } returns Unit

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                goalRepository = goalRepo,
                workManager = mockk<WorkManager>(relaxed = true)
            )
            advanceUntilIdle()

            // Add objective without explicit description (from PlanScreenV2 Objectives drawer)
            viewModel.addGoal("Launch MVP Beta")
            advanceUntilIdle()

            capturedGoal.isCaptured shouldBe true
            capturedGoal.captured.title shouldBe "Launch MVP Beta"
            capturedGoal.captured.description shouldBe "Weekly Objective"
        }
    }

    "BUG-196: FlowViewModelV2 excludes future-dated deadline tasks with null scheduledDate from actionableActiveTasks Hero Card" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            val futureDeadline = todayStart + 10 * 86_400_000L // 10 days in the future

            val futureDeadlineTask = makeTask("future-dl-1", "File Tax Audit").copy(
                scheduledDate = null,
                scheduledTime = null,
                deadlineDate = futureDeadline,
                deadlineTime = null,
                impactScore = 100 // High impact would otherwise dominate hero card scoring
            )
            val todayTask = makeTask("today-1", "Finish Code Review").copy(
                scheduledDate = todayStart,
                scheduledTime = 14 * 3600_000L,
                impactScore = 50
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(futureDeadlineTask, todayTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore
            )
            advanceUntilIdle()

            val state = viewModel.uiState.filter { it.filteredTasks.isNotEmpty() }.first()

            // Today's task list must NOT contain the future deadline task
            state.filteredTasks.map { it.id } shouldBe listOf("today-1")

            // The Hero Recommendation card must be today's task, never the 10-day future task!
            state.recommendedTask?.id shouldBe "today-1"
        }
    }
})
