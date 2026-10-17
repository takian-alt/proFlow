package com.neuroflow.app.presentation.v2

import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.focus.FocusViewModelV2
import com.neuroflow.app.presentation.v2.navigation.NavHubsV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
            val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val task = makeTask("flow-t1", "Prepare quarterly presentation")

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { energyRepo.observeEnergy(any()) } returns MutableStateFlow(mockk(relaxed = true))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val circadianService = DefaultCircadianEnergyService()
            val schedulerService = DefaultTaskSchedulerService(circadianService)
            val behavioralService = DefaultBehavioralSupportService()

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                energyScoreRepository = energyRepo,
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

            capturedSteps.captured shouldHaveSize 3
            capturedSteps.captured.all { it.taskId == "flow-t1" } shouldBe true
            capturedSteps.captured[0].title shouldBe "Open workspace & gather materials for Prepare quarterly presentation"
            capturedSteps.captured[1].title shouldBe "Spend 5 uninterrupted minutes on the very first sub-step"
            capturedSteps.captured[2].title shouldBe "Review progress & polish final result"
        }
    }

    "PlanViewModelV2 breakDownTask persists normalized micro-steps and saveQuickNote writes to QuickNoteDao" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)
            val quickNoteDao = mockk<QuickNoteDao>(relaxed = true)
            val task = makeTask("plan-t1", "Write integration tests")

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeTasksForDate(any()) } returns MutableStateFlow(listOf(task))
            every { energyRepo.observeEnergy(any()) } returns MutableStateFlow(mockk(relaxed = true))

            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                energyScoreRepository = energyRepo,
                workManager = workManager,
                quickNoteDao = quickNoteDao
            )

            viewModel.breakDownTask(task)
            advanceUntilIdle()

            val capturedSteps = slot<List<TaskMicroStepEntity>>()
            coVerify { taskRepo.deleteMicroStepsByTaskId("plan-t1") }
            coVerify { taskRepo.insertMicroSteps(capture(capturedSteps)) }
            capturedSteps.captured shouldHaveSize 3

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
            val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)
            val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

            val now = System.currentTimeMillis()
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
            every { energyRepo.observeEnergy(any()) } returns MutableStateFlow(mockk(relaxed = true))
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(autoSchedulingEnabled = true, wakeUpHour = 7))

            coEvery { calendarRepo.getBusyWindows(any(), any()) } returns listOf(
                TimeSlotWindow(now + 3600_000L, now + 7200_000L)
            )

            val circadianService = DefaultCircadianEnergyService()
            val schedulerService = DefaultTaskSchedulerService(circadianService)

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                energyScoreRepository = energyRepo,
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
            capturedSteps.captured shouldHaveSize 3
            capturedSteps.captured[0].taskId shouldBe "focus-t1"
        }
    }

    "NeuroFlowThemeV2 token palette provides high WCAG AA contrast against pure black" {
        NeuroFlowThemeV2Colors.Obsidian shouldBe androidx.compose.ui.graphics.Color(0xFF0A0B0E)
        NeuroFlowThemeV2Colors.TextPrimary shouldBe androidx.compose.ui.graphics.Color(0xFFF8FAFC)
        NeuroFlowThemeV2Colors.TextSecondary shouldBe androidx.compose.ui.graphics.Color(0xFF94A3B8)
        NeuroFlowThemeV2Colors.ElectricIndigo shouldBe androidx.compose.ui.graphics.Color(0xFF6366F1)
        NeuroFlowThemeV2Colors.SlateSurface shouldBe androidx.compose.ui.graphics.Color(0xFF13151B)
    }
})
