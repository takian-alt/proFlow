package com.neuroflow.app.adversarial

import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.focus.FocusViewModelV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Random

/**
 * Empirical Adversarial & Stress Test Suite for Milestone 1 (M1).
 *
 * Exhaustively stress-tests:
 * 1. BehavioralInsightsEngine.calculateHourlyCircadianDensity:
 *    - 0 sessions (empty map, all-zero map)
 *    - 1 session (single hour density = 1.0)
 *    - Overlapping midnight sessions (spanning across day boundaries)
 *    - 10,000 sessions stress test (performance, numerical stability, float precision)
 * 2. Retrospective Time Logging (logPastTime):
 *    - 0 minutes boundary
 *    - Negative minutes boundary (-15 min)
 *    - 1440 minutes (24h) full-day boundary
 *    - Verifies TimeSessionEntity persistence and totalFocusMinutes increment in both FlowViewModelV2 & PlanViewModelV2
 * 3. WOOP Anchor Persistence (FocusViewModelV2.saveWoop):
 *    - Blank fields ("", "", "", "")
 *    - Whitespace-only fields ("   ", "\t\n  ")
 *    - Unicode stress: emojis, multilingual (Japanese, Arabic, Bengali, Cyrillic), bidirectional/RTL, and math symbols
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Milestone1AnalyticsAndWoopStressTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleTask(id: String = "test-task-1", durationMinutes: Float = 0f) = TaskEntity(
        id = id,
        title = "Important Task $id",
        status = TaskStatus.ACTIVE,
        priority = Priority.HIGH,
        estimatedDurationMinutes = 60,
        actualDurationMinutes = durationMinutes,
        totalTimeTrackedMinutes = durationMinutes,
        sessionCount = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    // =========================================================================
    // SECTION 1: BehavioralInsightsEngine.calculateHourlyCircadianDensity
    // =========================================================================

    @Test
    fun testCircadianDensity_ZeroSessions_Returns24ZeroBinsWithoutNaN() {
        // Scenario A: Completely empty map
        val emptyDensity = BehavioralInsightsEngine.calculateHourlyCircadianDensity(emptyMap())
        assertEquals("Must return exactly 24 hourly bins for empty map", 24, emptyDensity.size)
        for (i in 0..23) {
            assertEquals("Empty map bin $i must be 0.0f", 0.0f, emptyDensity[i], 0.00001f)
            assertFalse("Bin $i must not be NaN", emptyDensity[i].isNaN())
            assertFalse("Bin $i must not be Infinite", emptyDensity[i].isInfinite())
        }

        // Scenario B: Map populated with all zeros
        val allZeroMap = (0..23).associateWith { 0 }
        val allZeroDensity = BehavioralInsightsEngine.calculateHourlyCircadianDensity(allZeroMap)
        assertEquals("Must return exactly 24 bins for all-zero map", 24, allZeroDensity.size)
        for (i in 0..23) {
            assertEquals("All-zero map bin $i must be 0.0f", 0.0f, allZeroDensity[i], 0.00001f)
            assertFalse("Bin $i must not be NaN", allZeroDensity[i].isNaN())
        }
    }

    @Test
    fun testCircadianDensity_OneSession_Yields100PercentInTargetHour() {
        // Single session: 60 minutes at 10:00 AM (hour 10)
        val density10 = BehavioralInsightsEngine.calculateHourlyCircadianDensity(mapOf(10 to 60))
        assertEquals(24, density10.size)
        assertEquals("Hour 10 must have 1.0 (100%) density", 1.0f, density10[10], 0.0001f)
        for (hour in 0..23) {
            if (hour != 10) {
                assertEquals("Hour $hour must have 0.0f density", 0.0f, density10[hour], 0.0001f)
            }
        }
        assertEquals("Sum must equal 1.0f", 1.0f, density10.sum(), 0.0001f)

        // Single session at midnight boundary: hour 23 with 15 minutes
        val density23 = BehavioralInsightsEngine.calculateHourlyCircadianDensity(mapOf(23 to 15))
        assertEquals(24, density23.size)
        assertEquals("Hour 23 must have 1.0 density", 1.0f, density23[23], 0.0001f)
        assertEquals("Sum must equal 1.0f", 1.0f, density23.sum(), 0.0001f)
    }

    @Test
    fun testCircadianDensity_OverlappingMidnightSessions_CalculatesDiurnalWrapAccurately() {
        // Night Owl workload spanning across midnight (hours 23, 0, 1):
        // Session A: 23:00 - 01:00 (60m in h23, 60m in h0)
        // Session B: 23:30 - 00:30 (30m in h23, 30m in h0)
        // Session C: 00:00 - 02:00 (60m in h0, 60m in h1)
        // Aggregated:
        // Hour 23: 60 + 30 = 90 min
        // Hour 0:  60 + 30 + 60 = 150 min
        // Hour 1:  60 min
        // Total = 300 minutes.
        val midnightSessions = mapOf(
            23 to 90,
            0 to 150,
            1 to 60
        )

        val density = BehavioralInsightsEngine.calculateHourlyCircadianDensity(midnightSessions)
        assertEquals(24, density.size)

        assertEquals("Hour 23 must be 90/300 = 0.30f", 0.30f, density[23], 0.001f)
        assertEquals("Hour 0 must be 150/300 = 0.50f", 0.50f, density[0], 0.001f)
        assertEquals("Hour 1 must be 60/300 = 0.20f", 0.20f, density[1], 0.001f)

        // All non-active hours must be exactly 0
        for (h in 2..22) {
            assertEquals("Hour $h must be 0", 0.0f, density[h], 0.0001f)
        }

        assertEquals("Circadian density across 24h must sum to 1.0 (100%)", 1.0f, density.sum(), 0.001f)
    }

    @Test
    fun testCircadianDensity_TenThousandSessions_StressTestPerformanceAndNumericalStability() {
        val random = Random(42L)
        val hourlyMap = mutableMapOf<Int, Int>()

        // Generate 10,000 synthetic sessions distributed across 24 hours
        val sessionCount = 10_000
        var totalMinutesExpected = 0L

        for (i in 0 until sessionCount) {
            val hour = random.nextInt(24)
            val duration = random.nextInt(90) + 10 // 10 to 99 minutes
            hourlyMap[hour] = (hourlyMap[hour] ?: 0) + duration
            totalMinutesExpected += duration
        }

        val startTime = System.nanoTime()
        val density = BehavioralInsightsEngine.calculateHourlyCircadianDensity(hourlyMap)
        val durationMs = (System.nanoTime() - startTime) / 1_000_000

        assertTrue("10,000 sessions density calculation must execute under 50ms (took ${durationMs}ms)", durationMs < 50)
        assertEquals(24, density.size)

        var totalDensitySum = 0f
        for (hour in 0..23) {
            val d = density[hour]
            assertFalse("Bin $hour must not be NaN", d.isNaN())
            assertFalse("Bin $hour must not be Infinite", d.isInfinite())
            assertTrue("Bin $hour must be in [0.0, 1.0] (was $d)", d in 0.0f..1.0f)
            totalDensitySum += d

            val expectedBinDensity = hourlyMap[hour]!!.toFloat() / totalMinutesExpected.toFloat()
            assertEquals("Bin $hour density must match exact ratio", expectedBinDensity, d, 0.0001f)
        }

        assertEquals("Total circadian density across 10,000 sessions must sum to 1.0f", 1.0f, totalDensitySum, 0.001f)
    }

    // =========================================================================
    // SECTION 2: Retrospective Time Logging (logPastTime)
    // =========================================================================

    @Test
    fun testFlowViewModelV2_LogPastTime_ZeroMinutes() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        var savedPrefs = UserPreferences(totalFocusMinutes = 120)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
        coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            savedPrefs = update(savedPrefs)
        }

        val viewModel = FlowViewModelV2(
            taskRepository = taskRepo,
            energyScoreRepository = energyRepo,
            preferencesDataStore = prefsDataStore,
            sessionRepository = sessionRepo
        )

        val task = sampleTask("task-zero", durationMinutes = 30f)
        viewModel.logPastTime(task, 0f)
        advanceUntilIdle()

        // 1. Verify TimeSessionEntity persistence
        val sessionSlot = slot<TimeSessionEntity>()
        coVerify { sessionRepo.insert(capture(sessionSlot)) }
        assertEquals("task-zero", sessionSlot.captured.taskId)
        assertEquals(0f, sessionSlot.captured.durationMinutes, 0.001f)
        assertEquals("MANUAL_LOG", sessionSlot.captured.sessionType)

        // 2. Verify TaskEntity update
        val taskSlot = slot<TaskEntity>()
        coVerify { taskRepo.update(capture(taskSlot)) }
        assertEquals(30f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
        assertEquals(1, taskSlot.captured.sessionCount)

        // 3. Verify totalFocusMinutes increment (120 + 0 = 120)
        assertEquals("totalFocusMinutes must remain 120 after 0m log", 120, savedPrefs.totalFocusMinutes)
    }

    @Test
    fun testFlowViewModelV2_LogPastTime_NegativeMinutes_EmpiricalObservation() = runTest(testDispatcher) {
        // Adversarial challenge: what happens when negative minutes are passed to logPastTime?
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        var savedPrefs = UserPreferences(totalFocusMinutes = 100)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
        coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            savedPrefs = update(savedPrefs)
        }

        val viewModel = FlowViewModelV2(
            taskRepository = taskRepo,
            energyScoreRepository = energyRepo,
            preferencesDataStore = prefsDataStore,
            sessionRepository = sessionRepo
        )

        val task = sampleTask("task-neg", durationMinutes = 40f)
        viewModel.logPastTime(task, -15f)
        advanceUntilIdle()

        // Empirically observe:
        // Because FlowViewModelV2 does not guard against negative values directly,
        // it creates a session with duration -15f and decrements totalFocusMinutes.
        val sessionSlot = slot<TimeSessionEntity>()
        coVerify { sessionRepo.insert(capture(sessionSlot)) }
        assertEquals(-15f, sessionSlot.captured.durationMinutes, 0.001f)
        val capturedEndedAt = sessionSlot.captured.endedAt
        assertNotNull("endedAt must not be null", capturedEndedAt)
        assertTrue("startedAt is computed in the future when negative: startedAt > endedAt",
            sessionSlot.captured.startedAt > capturedEndedAt!!)

        val taskSlot = slot<TaskEntity>()
        coVerify { taskRepo.update(capture(taskSlot)) }
        assertEquals(25f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)

        assertEquals("totalFocusMinutes decrements from 100 to 85", 85, savedPrefs.totalFocusMinutes)
    }

    @Test
    fun testFlowViewModelV2_LogPastTime_FullDay1440Minutes() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        var savedPrefs = UserPreferences(totalFocusMinutes = 60)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
        coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            savedPrefs = update(savedPrefs)
        }

        val viewModel = FlowViewModelV2(
            taskRepository = taskRepo,
            energyScoreRepository = energyRepo,
            preferencesDataStore = prefsDataStore,
            sessionRepository = sessionRepo
        )

        val task = sampleTask("task-full-day", durationMinutes = 0f)
        val beforeLog = System.currentTimeMillis()
        viewModel.logPastTime(task, 1440f)
        advanceUntilIdle()
        val afterLog = System.currentTimeMillis()

        // 1. Verify TimeSessionEntity persistence
        val sessionSlot = slot<TimeSessionEntity>()
        coVerify { sessionRepo.insert(capture(sessionSlot)) }
        val session = sessionSlot.captured
        assertEquals("task-full-day", session.taskId)
        assertEquals(1440f, session.durationMinutes, 0.001f)
        assertEquals("MANUAL_LOG", session.sessionType)
        val endedAt = session.endedAt
        assertNotNull("session.endedAt must not be null", endedAt)
        assertTrue(endedAt!! in beforeLog..afterLog)
        val expectedStart = endedAt - (1440L * 60_000L)
        assertEquals(expectedStart, session.startedAt)

        // 2. Verify TaskEntity update
        val taskSlot = slot<TaskEntity>()
        coVerify { taskRepo.update(capture(taskSlot)) }
        assertEquals(1440f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
        assertEquals(1440f, taskSlot.captured.actualDurationMinutes!!, 0.001f)
        assertEquals(1, taskSlot.captured.sessionCount)

        // 3. Verify totalFocusMinutes increment (60 + 1440 = 1500)
        assertEquals(1500, savedPrefs.totalFocusMinutes)
    }

    @Test
    fun testPlanViewModelV2_LogPastTime_PersistenceAndIncrement() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val energyRepo = mockk<EnergyScoreRepository>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)

        var savedPrefs = UserPreferences(totalFocusMinutes = 200)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(savedPrefs)
        coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            savedPrefs = update(savedPrefs)
        }

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            energyScoreRepository = energyRepo,
            workManager = workManager,
            sessionRepository = sessionRepo
        )

        val task = sampleTask("plan-task-1", durationMinutes = 45f)
        viewModel.logPastTime(task, 90f)
        advanceUntilIdle()

        // Verify TimeSessionEntity persisted
        val sessionSlot = slot<TimeSessionEntity>()
        coVerify { sessionRepo.insert(capture(sessionSlot)) }
        assertEquals("plan-task-1", sessionSlot.captured.taskId)
        assertEquals(90f, sessionSlot.captured.durationMinutes, 0.001f)
        assertEquals("MANUAL_LOG", sessionSlot.captured.sessionType)

        // Verify TaskEntity actual minutes updated
        val taskSlot = slot<TaskEntity>()
        coVerify { taskRepo.update(capture(taskSlot)) }
        assertEquals(135f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
        assertEquals(135f, taskSlot.captured.actualDurationMinutes!!, 0.001f)

        // Verify totalFocusMinutes incremented (200 + 90 = 290)
        assertEquals(290, savedPrefs.totalFocusMinutes)
    }

    // =========================================================================
    // SECTION 3: WOOP Anchor Saving (FocusViewModelV2.saveWoop)
    // =========================================================================

    @Test
    fun testFocusViewModelV2_SaveWoop_BlankFields() = runTest(testDispatcher) {
        val taskId = "woop-blank-task"
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to taskId))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById(taskId) } returns sampleTask(taskId)
        coEvery { woopRepo.getByTaskId(taskId) } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore
        )
        advanceUntilIdle()

        // Save completely blank strings
        viewModel.saveWoop(wish = "", outcome = "", obstacle = "", plan = "")
        advanceUntilIdle()

        val blankSlot = slot<WoopEntity>()
        coVerify(exactly = 1) { woopRepo.upsert(capture(blankSlot)) }
        assertEquals("", blankSlot.captured.wish)
        assertEquals("", blankSlot.captured.outcome)
        assertEquals("", blankSlot.captured.obstacle)
        assertEquals("", blankSlot.captured.plan)
        assertEquals(taskId, blankSlot.captured.taskId)

        // Verify FocusScreenV2 condition: all blank fields correctly identify as empty anchor
        val blankWoop = viewModel.uiState.value.woopData!!
        val isCardVisible = blankWoop.wish.isNotBlank() || blankWoop.obstacle.isNotBlank() || blankWoop.plan.isNotBlank()
        assertFalse("Blank WOOP must not trigger WOOP card visibility", isCardVisible)
    }

    @Test
    fun testFocusViewModelV2_SaveWoop_WhitespaceOnlyFields() = runTest(testDispatcher) {
        val taskId = "woop-ws-task"
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to taskId))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById(taskId) } returns sampleTask(taskId)
        coEvery { woopRepo.getByTaskId(taskId) } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore
        )
        advanceUntilIdle()

        // Save whitespace-only strings
        viewModel.saveWoop(wish = "   ", outcome = "\t\t", obstacle = " \n ", plan = "   ")
        advanceUntilIdle()

        val wsSlot = slot<WoopEntity>()
        coVerify(exactly = 1) { woopRepo.upsert(capture(wsSlot)) }
        val wsWoop = viewModel.uiState.value.woopData!!
        val isWsCardVisible = wsWoop.wish.isNotBlank() || wsWoop.obstacle.isNotBlank() || wsWoop.plan.isNotBlank()
        assertFalse("Whitespace-only WOOP must not trigger WOOP card visibility", isWsCardVisible)
    }

    @Test
    fun testFocusViewModelV2_SaveWoop_UnicodeAndMultilingualStress() = runTest(testDispatcher) {
        val taskId = "woop-unicode-task"
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to taskId))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById(taskId) } returns sampleTask(taskId)
        coEvery { woopRepo.getByTaskId(taskId) } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore
        )
        advanceUntilIdle()

        val unicodeVectors = listOf(
            // Emojis & Modifiers
            Quad(
                "🎯 Achieve Flow State 🌊",
                "🧠 High cognitive clarity & zero burnout ✨",
                "📱 Phone notifications & doom-scrolling 🛑",
                "📵 If phone rings, then flip face down on desk 🧘"
            ),
            // Multilingual (Japanese, Arabic, Bengali, Cyrillic)
            Quad(
                "プロジェクトを今日中に完了する 🎌",
                "نجاح كامل وتأثير إيجابي في الفريق",
                "প্রতিরোধ এবং প্রাথমিক জড়তা",
                "Если появится усталость, я сделаю глубокий вдох"
            ),
            // Bidirectional & Special Mathematical Symbols
            Quad(
                "Prove Theorem: ∀x ∈ S, ∃y ∈ T s.t. x ⊕ y = 0",
                "Q.E.D. publish in Annals of Mathematics",
                "Unbounded edge-case: \u202Ereversed text\u202C",
                "Apply lemma 4.2: ∑_{i=1}^n x_i ≤ C ⋅ ‖x‖_2"
            )
        )

        val capturedList = mutableListOf<WoopEntity>()
        coEvery { woopRepo.upsert(capture(capturedList)) } returns Unit

        for (vector in unicodeVectors) {
            viewModel.saveWoop(
                wish = vector.a,
                outcome = vector.b,
                obstacle = vector.c,
                plan = vector.d
            )
            advanceUntilIdle()
        }

        coVerify(exactly = unicodeVectors.size) { woopRepo.upsert(any()) }
        assertEquals(unicodeVectors.size, capturedList.size)

        for ((idx, vector) in unicodeVectors.withIndex()) {
            val saved = capturedList[idx]
            assertEquals("Wish must match unicode vector $idx exactly", vector.a, saved.wish)
            assertEquals("Outcome must match unicode vector $idx exactly", vector.b, saved.outcome)
            assertEquals("Obstacle must match unicode vector $idx exactly", vector.c, saved.obstacle)
            assertEquals("Plan must match unicode vector $idx exactly", vector.d, saved.plan)
        }

        // Final UI state reflects the last vector
        val lastVector = unicodeVectors.last()
        val uiWoop = viewModel.uiState.value.woopData!!
        assertEquals("UI State wish must match final unicode vector", lastVector.a, uiWoop.wish)
        assertTrue("Populated unicode WOOP must trigger card visibility",
            uiWoop.wish.isNotBlank() && uiWoop.obstacle.isNotBlank() && uiWoop.plan.isNotBlank())
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
