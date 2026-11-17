package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import android.content.Context
import androidx.work.WorkManager
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FatigueScoreEvaluatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 17)
    private val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val dayEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    private val morningNow = dayStart + 11 * 3600_000L + 30 * 60_000L // 11:30 AM

    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(AutonomyNudgeEngine)
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun peakSnapshot(atMillis: Long = morningNow) = CircadianSnapshot(
        timestampMillis = atMillis,
        usableEnergy = 82,
        homeostaticPressureS = 0.25,
        circadianAlertnessC = 0.85,
        focusDepletionPenalty = 0.0,
        phase = EnergyPhase.PEAK
    )

    @Test
    fun `high load task after two long completed sessions escalates to HIGH_BURNOUT_RISK and lowers score`() {
        val highLoadTask = TaskEntity(
            id = "deep-arch",
            title = "Core Architecture Refactor",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            priority = Priority.HIGH,
            quadrant = Quadrant.DO_FIRST,
            estimatedDurationMinutes = 60,
            scheduledDate = dayStart,
            scheduledTime = 11 * 3600_000L + 35 * 60_000L
        )

        val restedResult = FatigueScoreEvaluator.evaluateTasks(
            tasks = listOf(highLoadTask),
            allContextTasks = listOf(highLoadTask),
            completedTasks = emptyList(),
            allSessions = emptyList(),
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            nowMillis = morningNow,
            timeline = listOf(peakSnapshot()),
            fallbackSnapshot = peakSnapshot(),
            prefs = UserPreferences(),
            taskSchedulerService = schedulerService,
            zoneId = zoneId
        ).getValue(highLoadTask.id)

        assertEquals(FatigueWarningLevel.OPTIMAL, restedResult.warningLevel)
        assertEquals(0, restedResult.priorLongSessionCount)

        // Now record two long sessions right before 11:35
        val session1 = TimeSessionEntity(
            id = "sess-1",
            taskId = "prev-task-1",
            startedAt = dayStart + 9 * 3600_000L,
            endedAt = dayStart + 10 * 3600_000L,
            durationMinutes = 60f
        )
        val session2 = TimeSessionEntity(
            id = "sess-2",
            taskId = "prev-task-2",
            startedAt = dayStart + 10 * 3600_000L + 10 * 60_000L,
            endedAt = dayStart + 11 * 3600_000L + 20 * 60_000L,
            durationMinutes = 70f
        )
        val prevTask1 = TaskEntity(
            id = "prev-task-1",
            title = "System Design",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.COMPLETED,
            estimatedDurationMinutes = 60
        )
        val prevTask2 = TaskEntity(
            id = "prev-task-2",
            title = "API Migration",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.COMPLETED,
            estimatedDurationMinutes = 70
        )

        val fatiguedResult = FatigueScoreEvaluator.evaluateTasks(
            tasks = listOf(highLoadTask),
            allContextTasks = listOf(highLoadTask),
            completedTasks = listOf(prevTask1, prevTask2),
            allSessions = listOf(session1, session2),
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            nowMillis = morningNow,
            timeline = listOf(peakSnapshot()),
            fallbackSnapshot = peakSnapshot(),
            prefs = UserPreferences(),
            taskSchedulerService = schedulerService,
            zoneId = zoneId
        ).getValue(highLoadTask.id)

        assertEquals(FatigueWarningLevel.HIGH_BURNOUT_RISK, fatiguedResult.warningLevel)
        assertTrue(
            "Expected priorLongSessionCount >= 2, got ${fatiguedResult.priorLongSessionCount}",
            fatiguedResult.priorLongSessionCount >= 2
        )
        assertTrue(
            "Fatigued score (${fatiguedResult.priorityScore}) should drop below rested (${restedResult.priorityScore})",
            fatiguedResult.priorityScore < restedResult.priorityScore
        )
        assertTrue(
            "Expected warning reason mentioning long sessions, got '${fatiguedResult.warningReason}'",
            fatiguedResult.warningReason.contains("long session", ignoreCase = true)
        )
    }

    @Test
    fun `scheduled chain of two long tasks triggers warning on trailing high-load task while restorative mindfulness is boosted`() {
        val block1 = TaskEntity(
            id = "sched-1",
            title = "Compiler Pass 1",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = dayStart,
            scheduledTime = 8 * 3600_000L
        )
        val block2 = TaskEntity(
            id = "sched-2",
            title = "Compiler Pass 2",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = dayStart,
            scheduledTime = 9 * 3600_000L + 10 * 60_000L
        )
        val trailingHighTask = TaskEntity(
            id = "sched-3",
            title = "Benchmark Kernel",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = dayStart,
            scheduledTime = 10 * 3600_000L + 20 * 60_000L
        )
        val restorativeBreak = TaskEntity(
            id = "rest-break",
            title = "Breathwork Mindfulness Reset",
            tags = "mindfulness",
            energyLevel = EnergyLevel.LOW,
            estimatedDurationMinutes = 15,
            scheduledDate = dayStart,
            scheduledTime = 10 * 3600_000L + 20 * 60_000L
        )

        val scores = FatigueScoreEvaluator.evaluateTasks(
            tasks = listOf(block1, block2, trailingHighTask, restorativeBreak),
            allContextTasks = listOf(block1, block2, trailingHighTask, restorativeBreak),
            completedTasks = emptyList(),
            allSessions = emptyList(),
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            nowMillis = dayStart + 7 * 3600_000L,
            timeline = listOf(peakSnapshot(dayStart + 10 * 3600_000L)),
            fallbackSnapshot = peakSnapshot(dayStart + 10 * 3600_000L),
            prefs = UserPreferences(),
            taskSchedulerService = schedulerService,
            zoneId = zoneId
        )

        assertEquals(FatigueWarningLevel.HIGH_BURNOUT_RISK, scores.getValue(trailingHighTask.id).warningLevel)
        assertEquals(FatigueWarningLevel.RESTORATIVE, scores.getValue(restorativeBreak.id).warningLevel)
        assertTrue(scores.getValue(restorativeBreak.id).fatigueAdjustment > 0.0)
    }

    @Test
    fun `FlowViewModelV2 updates fatigueScoresByTaskId reactively when new long sessions complete`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val nowLocalStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        val evalNow = nowLocalStart + 12 * 3600_000L

        val targetTask = TaskEntity(
            id = "target-high-task",
            title = "Deep Systems Spec",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            priority = Priority.HIGH,
            quadrant = Quadrant.DO_FIRST,
            estimatedDurationMinutes = 60,
            scheduledDate = nowLocalStart,
            scheduledTime = 13 * 3600_000L
        )

        val activeFlow = MutableStateFlow(listOf(targetTask))
        val completedFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
        val sessionsFlow = MutableStateFlow<List<TimeSessionEntity>>(emptyList())
        val prefsFlow = MutableStateFlow(UserPreferences())

        every { taskRepo.observeActiveTasks() } returns activeFlow
        every { taskRepo.observeCompletedTasks() } returns completedFlow
        every { sessionRepo.observeAll() } returns sessionsFlow
        every { prefsDataStore.preferencesFlow } returns prefsFlow

        val vm = FlowViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            sessionRepository = sessionRepo,
            nowProvider = { evalNow }
        )

        val initialState = vm.uiState.filter { !it.isLoading }.first()
        val initialScore = initialState.fatigueScoresByTaskId[targetTask.id]
        assertTrue("Expected initial score to exist", initialScore != null)
        assertEquals(FatigueWarningLevel.OPTIMAL, initialScore!!.warningLevel)

        // Emit two completed long-duration sessions today
        val s1 = TimeSessionEntity(
            id = "live-s1",
            taskId = "other-1",
            startedAt = nowLocalStart + 9 * 3600_000L,
            endedAt = nowLocalStart + 10 * 3600_000L,
            durationMinutes = 60f
        )
        val s2 = TimeSessionEntity(
            id = "live-s2",
            taskId = "other-2",
            startedAt = nowLocalStart + 10 * 3600_000L + 15 * 60_000L,
            endedAt = nowLocalStart + 11 * 3600_000L + 45 * 60_000L,
            durationMinutes = 90f
        )
        sessionsFlow.value = listOf(s1, s2)
        advanceUntilIdle()

        val updatedState = vm.uiState.filter {
            it.fatigueScoresByTaskId[targetTask.id]?.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK
        }.first()
        val updatedScore = updatedState.fatigueScoresByTaskId.getValue(targetTask.id)

        assertTrue(updatedScore.priorLongSessionCount >= 2)
        assertTrue(updatedScore.priorityScore < initialScore.priorityScore)
    }

    @Test
    fun `PlanViewModelV2 exposes reactive fatigueScoresByTaskId for planned tasks`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())

        val task1 = TaskEntity(
            id = "plan-1",
            title = "Heavy Engineering 1",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L
        )
        val task2 = TaskEntity(
            id = "plan-2",
            title = "Heavy Engineering 2",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L + 10 * 60_000L
        )
        val task3 = TaskEntity(
            id = "plan-3",
            title = "Heavy Engineering 3",
            taskType = TaskType.ANALYTICAL,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = todayStart,
            scheduledTime = 11 * 3600_000L + 20 * 60_000L
        )

        val activeFlow = MutableStateFlow(listOf(task1, task2, task3))
        every { taskRepo.observeActiveTasks() } returns activeFlow
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { sessionRepo.observeAll() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val vm = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = mockk<WorkManager>(relaxed = true),
            sessionRepository = sessionRepo
        )

        val state = vm.uiState.filter { !it.isLoading }.first()
        val trailingScore = state.fatigueScoresByTaskId[task3.id]
        assertTrue("Expected fatigue score for scheduled task3", trailingScore != null)
        assertEquals(FatigueWarningLevel.HIGH_BURNOUT_RISK, trailingScore!!.warningLevel)
        assertTrue(trailingScore.priorLongSessionCount >= 2)
    }
}
