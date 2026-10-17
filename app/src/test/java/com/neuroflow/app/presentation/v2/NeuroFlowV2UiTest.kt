package com.neuroflow.app.presentation.v2

import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2
import com.neuroflow.app.presentation.v2.focus.FocusViewModelV2
import com.neuroflow.app.presentation.v2.insights.InsightsViewModelV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModeV2
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class NeuroFlowV2UiTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun makeTask(
        id: String = "task-1",
        title: String = "Design Core Flow",
        energyLevel: EnergyLevel = EnergyLevel.HIGH,
        isFrog: Boolean = false,
        impactScore: Int = 80,
        quadrant: Quadrant = Quadrant.DO_FIRST,
        scheduledDate: Long? = null,
        scheduledTime: Long? = null,
        ifThenPlan: String = "",
        postponeCount: Int = 0,
        tags: String = ""
    ) = TaskEntity(
        id = id,
        title = title,
        energyLevel = energyLevel,
        isFrog = isFrog,
        impactScore = impactScore,
        quadrant = quadrant,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        ifThenPlan = ifThenPlan,
        postponeCount = postponeCount,
        tags = tags
    )

    "FlowViewModelV2 recommends the highest leverage task matching biological battery" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val activeTasks = listOf(
                makeTask(id = "low-1", title = "Check emails", energyLevel = EnergyLevel.LOW, impactScore = 20),
                makeTask(id = "med-1", title = "Team sync", energyLevel = EnergyLevel.MEDIUM, impactScore = 50),
                makeTask(id = "frog-1", title = "Ship Engine V2", energyLevel = EnergyLevel.HIGH, isFrog = true, impactScore = 95)
            )

            val circadianService = mockk<CircadianEnergyService>(relaxed = true)
            every { circadianService.evaluateSnapshot(any(), any(), any(), any(), any()) } returns CircadianSnapshot(
                timestampMillis = System.currentTimeMillis(),
                usableEnergy = 80,
                homeostaticPressureS = 0.2,
                circadianAlertnessC = 0.8,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.PEAK
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(activeTasks)
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                circadianEnergyService = circadianService
            )
            val state = viewModel.uiState.filter { !it.isLoading }.first()

            state.activeTasks shouldHaveSize 3
            state.recommendedTask?.id shouldBe "frog-1"
        }
    }

    "FlowViewModelV2 integrates SleepTelemetryRepository to evaluate energy state from sleep logs" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val circadianService = mockk<CircadianEnergyService>(relaxed = true)
            val sleepTelemetryRepo = mockk<SleepTelemetryRepository>(relaxed = true)

            every { circadianService.evaluateSnapshot(any(), any(), any(), any(), any()) } returns CircadianSnapshot(
                timestampMillis = System.currentTimeMillis(),
                usableEnergy = 85,
                homeostaticPressureS = 0.15,
                circadianAlertnessC = 0.9,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.PEAK
            )
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
            every { sleepTelemetryRepo.observeAllSleepLogs() } returns MutableStateFlow(emptyList())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                circadianEnergyService = circadianService,
                sleepTelemetryRepository = sleepTelemetryRepo
            )
            val state = viewModel.uiState.filter { !it.isLoading }.first()

            state.energyUi?.availableEnergy shouldBe 85
            state.energyUi?.fatigueZone shouldBe FatigueZone.RESTED
        }
    }

    "FlowViewModelV2 defers a task by 2 hours cleanly" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val task = makeTask(id = "defer-task", scheduledTime = 10_000_000L)
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(taskRepo, prefsDataStore)
            viewModel.deferTask(task, hours = 2)
            advanceUntilIdle()

            val updatedSlot = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(updatedSlot)) }
            updatedSlot.captured.scheduledTime shouldBe (10_000_000L + 2 * 3600_000L)
            updatedSlot.captured.postponeCount shouldBe 1
        }
    }

    "PlanViewModelV2 segments tasks into correct Quadrants and handles ViewMode switching" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)

            val tasks = listOf(
                makeTask(id = "q1", quadrant = Quadrant.DO_FIRST),
                makeTask(id = "q2", quadrant = Quadrant.SCHEDULE),
                makeTask(id = "q3", quadrant = Quadrant.DELEGATE),
                makeTask(id = "q4", quadrant = Quadrant.ELIMINATE)
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(tasks)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(taskRepo, prefsDataStore, workManager)
            val state = viewModel.uiState.filter { !it.isLoading }.first()

            state.tasksByQuadrant[Quadrant.DO_FIRST]?.size shouldBe 1
            state.tasksByQuadrant[Quadrant.SCHEDULE]?.size shouldBe 1
            state.tasksByQuadrant[Quadrant.DELEGATE]?.size shouldBe 1
            state.tasksByQuadrant[Quadrant.ELIMINATE]?.size shouldBe 1

            viewModel.setViewMode(PlanViewModeV2.MATRIX)
            advanceUntilIdle()
            viewModel.uiState.value.viewMode shouldBe PlanViewModeV2.MATRIX

            viewModel.moveTaskToQuadrant(tasks[0], Quadrant.SCHEDULE)
            advanceUntilIdle()
            coVerify { taskRepo.update(match { it.id == "q1" && it.quadrant == Quadrant.SCHEDULE }) }
        }
    }

    "FocusViewModelV2 supports 'Just 5 Minutes' dopamine micro-start and micro-steps" {
        runTest(testDispatcher) {
            val task = makeTask(
                id = "focus-1",
                title = "Write Architecture Spec",
                ifThenPlan = "[ ] Step 1\n[ ] Step 2"
            )
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "focus-1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("focus-1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            coEvery { woopRepo.getByTaskId("focus-1") } returns WoopEntity(taskId = "focus-1", wish = "Flawless V2")
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.task?.id shouldBe "focus-1"
            state.microSteps shouldHaveSize 2
            state.woopData?.wish shouldBe "Flawless V2"

            // Select 5m micro-start
            viewModel.selectTimerMode(FocusTimerModeV2.JUST_5_MIN)
            viewModel.uiState.value.timerMode shouldBe FocusTimerModeV2.JUST_5_MIN
            viewModel.uiState.value.remainingSeconds shouldBe 300L

            // Start timer
            viewModel.startTimer()
            viewModel.uiState.value.isRunning shouldBe true
            repeat(3) { viewModel.tickSecond() }
            viewModel.uiState.value.elapsedSeconds shouldBe 3L
            viewModel.uiState.value.remainingSeconds shouldBe 297L

            // Pause timer
            viewModel.pauseTimer()
            viewModel.uiState.value.isPaused shouldBe true

            // Toggle micro-step
            viewModel.toggleMicroStep(0)
            viewModel.uiState.value.microSteps[0].completed shouldBe true

            // Park thought
            viewModel.parkIntrusiveThought("Order cat food")
            advanceUntilIdle()
            coVerify { taskRepo.insert(match { it.title == "Order cat food" && it.quadrant == Quadrant.DELEGATE }) }

            // Complete task
            viewModel.completeTask()
            advanceUntilIdle()
            viewModel.uiState.value.isCompleted shouldBe true
            coVerify { taskRepo.completeAndRecur(match { it.id == "focus-1" }, any()) }
        }
    }

    "InsightsViewModelV2 evaluates Borbély Two-Process metrics and circadian alignment" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val contractRepo = mockk<UlyssesContractRepository>(relaxed = true)

            val now = System.currentTimeMillis()
            val sessions = listOf(
                TimeSessionEntity(
                    id = "s-1",
                    taskId = "t-1",
                    startedAt = now - 3600_000L,
                    endedAt = now,
                    durationMinutes = 60f,
                    pauseResumeCount = 2,
                    appSwitchCount = 3
                )
            )

            val tasks = listOf(
                makeTask(id = "t-1", energyLevel = EnergyLevel.HIGH, scheduledTime = 10 * 3600_000L) // 10:00 AM (in 9-12 peak)
            )

            val contracts = listOf(
                UlyssesContractEntity(id = "c-1", taskId = "t-1", deadlineAt = now + 86400_000L, consequence = "10 Pushups")
            )

            every { taskRepo.observeAll() } returns MutableStateFlow(tasks)
            every { sessionRepo.observeAll() } returns MutableStateFlow(sessions)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
                UserPreferences(peakEnergyStart = 9, peakEnergyEnd = 12, dailyStreak = 5, sleepPressurePoints = 820)
            )
            every { contractRepo.observeActive() } returns MutableStateFlow(contracts)

            val viewModel = InsightsViewModelV2(
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsDataStore,
                contractRepository = contractRepo
            )
            val state = viewModel.uiState.filter { !it.isLoading }.first()

            state.energyUi?.sleepPressurePoints shouldBe 820
            state.energyUi?.fatigueZone shouldBe FatigueZone.MODERATE
            state.dailyStreak shouldBe 5
            state.activeContractsCount shouldBe 1
            state.interruptionPauseResumeCount shouldBe 2
            state.interruptionAppSwitchCount shouldBe 3
            state.circadianAlignmentRate shouldBe 100
            state.neuroscienceInterventions.isNotEmpty() shouldBe true
        }
    }

    "FlowViewModelV2 triggers Zero-Guilt Fresh Start rollover moving overdue tasks to Icebox" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val now = System.currentTimeMillis()
            val overdue1 = makeTask(id = "od-1", title = "Procrastinated 1", postponeCount = 4, quadrant = Quadrant.DO_FIRST)
            val overdue2 = makeTask(id = "od-2", title = "Procrastinated 2", scheduledTime = now - 4 * 3600_000L, quadrant = Quadrant.SCHEDULE)
            val freshTask = makeTask(id = "fresh-1", title = "New Task", postponeCount = 0, scheduledTime = now + 3600_000L)

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(overdue1, overdue2, freshTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(taskRepo, prefsDataStore)
            val state = viewModel.uiState.filter { !it.isLoading }.first()
            state.overdueTasksCount shouldBe 2

            viewModel.freshStartRollover()
            advanceUntilIdle()

            coVerify {
                taskRepo.update(match { it.id == "od-1" && it.quadrant == Quadrant.ELIMINATE && it.postponeCount == 0 })
            }
            coVerify {
                taskRepo.update(match { it.id == "od-2" && it.quadrant == Quadrant.ELIMINATE && it.postponeCount == 0 })
            }
            viewModel.uiState.value.lastActionMessage shouldContain "Fresh start active!"
        }
    }

    "FlowViewModelV2 supports 4-second non-destructive undo for task completion" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val task = makeTask(id = "task-complete-undo", title = "Deep Focus Coding")
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(taskRepo, prefsDataStore)
            viewModel.uiState.filter { !it.isLoading }.first()

            viewModel.completeTask(task)
            advanceUntilIdle()

            val completeMsg = viewModel.uiState.filter { it.lastActionMessage != null }.first().lastActionMessage
            completeMsg shouldBe "Completed: Deep Focus Coding"
            coVerify { taskRepo.completeAndRecur(match { it.id == "task-complete-undo" }, any()) }

            viewModel.undoCompleteTask()
            advanceUntilIdle()

            coVerify { taskRepo.update(match { it.id == "task-complete-undo" && it.status == TaskStatus.ACTIVE && it.completedAt == null }) }
            val restoreMsg = viewModel.uiState.filter { it.lastActionMessage?.startsWith("Restored") == true }.first().lastActionMessage
            restoreMsg shouldBe "Restored: Deep Focus Coding"
        }
    }

    "FlowViewModelV2 supports 4-second non-destructive undo for task deferral" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val task = makeTask(id = "defer-undo", title = "Call Accountant", scheduledTime = 20_000_000L, postponeCount = 1)
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(taskRepo, prefsDataStore)
            viewModel.uiState.filter { !it.isLoading }.first()

            viewModel.deferTask(task, hours = 3)
            advanceUntilIdle()

            coVerify { taskRepo.update(match { it.id == "defer-undo" && it.scheduledTime == 20_000_000L + 3 * 3600_000L && it.postponeCount == 2 }) }
            val deferMsg = viewModel.uiState.filter { it.lastActionMessage != null }.first().lastActionMessage
            deferMsg shouldBe "Deferred by 3h: Call Accountant"

            viewModel.undoDeferTask()
            advanceUntilIdle()

            coVerify { taskRepo.update(match { it.id == "defer-undo" && it.scheduledTime == 20_000_000L && it.postponeCount == 0 }) }
            val undoMsg = viewModel.uiState.filter { it.lastActionMessage?.startsWith("Undid") == true }.first().lastActionMessage
            undoMsg shouldBe "Undid deferral for: Call Accountant"
        }
    }

    "FlowViewModelV2 breaks down daunting task into 3 micro-steps with Magic Wand" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val dauntingTask = makeTask(id = "daunting-1", title = "Write Doctoral Dissertation", ifThenPlan = "")
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(dauntingTask))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FlowViewModelV2(taskRepo, prefsDataStore)
            viewModel.uiState.filter { !it.isLoading }.first()

            viewModel.breakDownTask(dauntingTask)
            advanceUntilIdle()

            val updatedSlot = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(updatedSlot)) }
            val parsedSteps = StepByStepPlanCodec.parse(updatedSlot.captured.ifThenPlan)
            parsedSteps shouldHaveSize 3
            parsedSteps[0].text shouldContain "Open workspace & gather materials"
            parsedSteps[1].text shouldContain "Spend 5 uninterrupted minutes"
            parsedSteps[2].text shouldContain "Review progress & polish"
            val wandMsg = viewModel.uiState.filter { it.lastActionMessage != null }.first().lastActionMessage
            wandMsg shouldContain "Decomposed 'Write Doctoral Dissertation' into 3 micro-steps"
        }
    }

    "FocusViewModelV2 autoBreakDownTask scaffolds 3 steps and submits post-session affordance rating" {
        runTest(testDispatcher) {
            val task = makeTask(
                id = "focus-scaffold",
                title = "Refactor Database Schema",
                ifThenPlan = ""
            )
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "focus-scaffold"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("focus-scaffold") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
            advanceUntilIdle()

            viewModel.uiState.value.microSteps shouldHaveSize 0

            // Scaffold 3 steps
            viewModel.autoBreakDownTask()
            advanceUntilIdle()

            viewModel.uiState.value.microSteps shouldHaveSize 3
            coVerify { taskRepo.update(match { it.id == "focus-scaffold" && it.ifThenPlan.contains("Open workspace") }) }

            // Complete task and verify affordance rating prompt
            viewModel.completeTask()
            advanceUntilIdle()

            viewModel.uiState.value.isCompleted shouldBe true
            viewModel.uiState.value.showAffordanceRating shouldBe true

            // Submit affordance rating
            viewModel.submitAffordanceRating(4.0f)
            advanceUntilIdle()

            viewModel.uiState.value.showAffordanceRating shouldBe false
            viewModel.uiState.value.affordanceRatingSubmitted shouldBe true
            coVerify { taskRepo.update(match { it.id == "focus-scaffold" && it.affectiveForecastError == 4.0f }) }
        }
    }

    "PlanViewModelV2 filters tasks by selected tag strip" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)

            val tasks = listOf(
                makeTask(id = "tag-1", title = "Write Core Algorithm", tags = "#deepwork"),
                makeTask(id = "tag-2", title = "Answer Emails", tags = "#admin"),
                makeTask(id = "tag-3", title = "Read Paper", tags = "#offline")
            )

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(tasks)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = PlanViewModelV2(taskRepo, prefsDataStore, workManager)
            val stateAll = viewModel.uiState.filter { !it.isLoading }.first()
            stateAll.unscheduledTasks shouldHaveSize 3

            viewModel.selectTag("#deepwork")
            advanceUntilIdle()

            val filteredState = viewModel.uiState.value
            filteredState.selectedTag shouldBe "#deepwork"
            filteredState.unscheduledTasks shouldHaveSize 1
            filteredState.unscheduledTasks[0].id shouldBe "tag-1"
        }
    }

    "FlowViewModelV2 logPastTime records MANUAL_LOG session, updates task, and increments totalFocusMinutes" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val task = makeTask(id = "flow-log-1", title = "Deep Work Session").copy(
                totalTimeTrackedMinutes = 25f,
                sessionCount = 1,
                actualDurationMinutes = 25f
            )

            var savedPrefs = UserPreferences(totalFocusMinutes = 100)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
            coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
                val update = firstArg<(UserPreferences) -> UserPreferences>()
                savedPrefs = update(savedPrefs)
            }
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
            every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())

            val viewModel = FlowViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                sessionRepository = sessionRepo
            )
            viewModel.uiState.filter { !it.isLoading }.first()

            viewModel.logPastTime(task, 45f)
            advanceUntilIdle()

            val sessionSlot = slot<TimeSessionEntity>()
            coVerify { sessionRepo.insert(capture(sessionSlot)) }
            sessionSlot.captured.taskId shouldBe "flow-log-1"
            sessionSlot.captured.sessionType shouldBe "MANUAL_LOG"
            sessionSlot.captured.durationMinutes shouldBe 45f

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(taskSlot)) }
            taskSlot.captured.id shouldBe "flow-log-1"
            taskSlot.captured.totalTimeTrackedMinutes shouldBe 70f
            taskSlot.captured.actualDurationMinutes shouldBe 70f
            taskSlot.captured.sessionCount shouldBe 2
            taskSlot.captured.lastSessionDurationMinutes shouldBe 45f

            savedPrefs.totalFocusMinutes shouldBe 145

            val actionMsg = viewModel.uiState.filter { it.lastActionMessage != null }.first().lastActionMessage
            actionMsg shouldContain "Logged 45m past time for Deep Work Session"
        }
    }

    "PlanViewModelV2 logPastTime records MANUAL_LOG session, updates task, and increments totalFocusMinutes" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val workManager = mockk<WorkManager>(relaxed = true)

            var savedPrefs = UserPreferences(totalFocusMinutes = 50)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
            coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
                val update = firstArg<(UserPreferences) -> UserPreferences>()
                savedPrefs = update(savedPrefs)
            }

            val task = makeTask(id = "plan-log-1", title = "Sprint Architecture Review").copy(
                totalTimeTrackedMinutes = 0f,
                sessionCount = 0,
                actualDurationMinutes = 0f
            )

            val viewModel = PlanViewModelV2(
                taskRepository = taskRepo,
                preferencesDataStore = prefsDataStore,
                workManager = workManager,
                sessionRepository = sessionRepo
            )

            viewModel.logPastTime(task, 30f)
            advanceUntilIdle()

            val sessionSlot = slot<TimeSessionEntity>()
            coVerify { sessionRepo.insert(capture(sessionSlot)) }
            sessionSlot.captured.taskId shouldBe "plan-log-1"
            sessionSlot.captured.sessionType shouldBe "MANUAL_LOG"
            sessionSlot.captured.durationMinutes shouldBe 30f

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.update(capture(taskSlot)) }
            taskSlot.captured.id shouldBe "plan-log-1"
            taskSlot.captured.totalTimeTrackedMinutes shouldBe 30f
            taskSlot.captured.actualDurationMinutes shouldBe 30f
            taskSlot.captured.sessionCount shouldBe 1
            taskSlot.captured.lastSessionDurationMinutes shouldBe 30f

            savedPrefs.totalFocusMinutes shouldBe 80
        }
    }

    "FocusViewModelV2 saveWoop persists WoopEntity to WoopRepository and updates uiState" {
        runTest(testDispatcher) {
            val task = makeTask(
                id = "woop-focus-1",
                title = "Write Novel Chapter"
            )
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "woop-focus-1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("woop-focus-1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            coEvery { woopRepo.getByTaskId("woop-focus-1") } returns null
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
            advanceUntilIdle()

            viewModel.uiState.value.woopData shouldBe null

            val wish = "Complete chapter draft"
            val outcome = "Sense of milestone completion"
            val obstacle = "Notification distraction"
            val plan = "Put phone on silent in drawer"

            viewModel.saveWoop(wish, outcome, obstacle, plan)
            advanceUntilIdle()

            val woopSlot = slot<WoopEntity>()
            coVerify { woopRepo.upsert(capture(woopSlot)) }
            woopSlot.captured.taskId shouldBe "woop-focus-1"
            woopSlot.captured.wish shouldBe wish
            woopSlot.captured.outcome shouldBe outcome
            woopSlot.captured.obstacle shouldBe obstacle
            woopSlot.captured.plan shouldBe plan

            val currentWoop = viewModel.uiState.value.woopData
            currentWoop shouldBe woopSlot.captured
            currentWoop?.wish shouldBe wish
            currentWoop?.outcome shouldBe outcome
            currentWoop?.obstacle shouldBe obstacle
            currentWoop?.plan shouldBe plan
        }
    }

    "FocusViewModelV2 completeTask triggers 3-minute Decompression Zone, guided sigh, and bypass" {
        runTest(testDispatcher) {
            val task = makeTask(
                id = "decomp-task-1",
                title = "Deep Architectural Refactoring",
                impactScore = 80,
                isFrog = true
            )
            val savedStateHandle = SavedStateHandle(mapOf("taskId" to "decomp-task-1"))
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val woopRepo = mockk<WoopRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            coEvery { taskRepo.getById("decomp-task-1") } returns task
            coEvery { taskRepo.getActiveTasks() } returns listOf(task)
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
            advanceUntilIdle()

            viewModel.startTimer()
            viewModel.completeTask()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            state.isCompleted shouldBe true
            state.showDecompressionZone shouldBe true
            state.decompressionRemainingSeconds shouldBe 180
            state.isDecompressionSighActive shouldBe false
            state.isDecompressionBypassed shouldBe false

            // Tick 10 seconds
            repeat(10) { viewModel.tickDecompressionSecond() }
            viewModel.uiState.value.decompressionRemainingSeconds shouldBe 170

            // Trigger Physiological Sigh
            viewModel.triggerDecompressionSigh()
            viewModel.uiState.value.isDecompressionSighActive shouldBe true
            viewModel.uiState.value.decompressionSighRemainingSeconds shouldBe 60

            // Tick 20 seconds
            repeat(20) { viewModel.tickDecompressionSecond() }
            viewModel.uiState.value.decompressionRemainingSeconds shouldBe 150
            viewModel.uiState.value.decompressionSighRemainingSeconds shouldBe 40

            // User taps "Ready Now" autonomy bypass
            viewModel.readyNowBypassDecompression()
            viewModel.uiState.value.showDecompressionZone shouldBe false
            viewModel.uiState.value.isDecompressionBypassed shouldBe true
            viewModel.uiState.value.decompressionRemainingSeconds shouldBe 0
            viewModel.uiState.value.isDecompressionSighActive shouldBe false
        }
    }
})
