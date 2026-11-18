package com.neuroflow.app.domain.model

import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PredictedEnergyCurveUiModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val circadianService = DefaultCircadianEnergyService()
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cold start without historical tasks returns 24 baseline points and zero confidence`() {
        val dayStart = LocalDate.of(2026, 5, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeMillis = dayStart + 7 * 3600_000L
        val timeline = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            stepMinutes = 30,
            zoneId = zoneId,
            naps = emptyList(),
            chronotype = ChronotypeProfile.INTERMEDIATE,
            manualPeakStartHour = null
        )

        val curve = buildPredictedEnergyCurveUiModel(
            timeline = timeline,
            completedTasks = emptyList(),
            sessions = emptyList(),
            circadianSummary = null,
            learnedProfile = null,
            workDayStart = 8,
            workDayEnd = 20,
            zoneId = zoneId
        )

        assertEquals(24, curve.hourlyPoints.size)
        assertFalse(curve.hasHistoricalData)
        assertEquals(0, curve.historicalCompletionCount)
        assertEquals(0, curve.modelConfidencePercent)
        assertTrue(curve.recoveryThresholdPercent in 25..75)
        assertTrue(curve.isRecoveryThresholdPersonalized)
        assertTrue(curve.hourlyPoints.all { it.predictedEnergy in 0..100 })
        assertTrue(curve.hourlyPoints.all { it.historicalActivityScore == 0 })
    }

    @Test
    fun `historical task completions calibrate energy curve and peak activity hour`() {
        val baseDate = LocalDate.of(2026, 5, 10)
        val dayStart = baseDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeMillis = dayStart + 7 * 3600_000L

        val afternoonTasks = (1..6).map { idx ->
            val completionTs = baseDate.minusDays(idx.toLong())
                .atTime(16, 15)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            TaskEntity(
                id = "hist_$idx",
                title = "Deep Work Block $idx",
                energyLevel = EnergyLevel.HIGH,
                status = TaskStatus.COMPLETED,
                completedAt = completionTs,
                updatedAt = completionTs
            )
        }

        val observations = buildBehavioralObservations(
            completedTasks = afternoonTasks,
            sessions = emptyList(),
            zoneId = zoneId
        )
        val learnedProfile = AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = 9.0)

        val timeline = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            stepMinutes = 30,
            zoneId = zoneId,
            naps = emptyList(),
            chronotype = ChronotypeProfile.INTERMEDIATE,
            manualPeakStartHour = null,
            cognitiveBlocks = emptyList(),
            learnedProfile = learnedProfile
        )
        val summary = circadianService.computeDailySummary(
            timestampMillis = dayStart + 9 * 3600_000L,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = emptyList(),
            chronotype = ChronotypeProfile.INTERMEDIATE,
            manualPeakStartHour = null,
            cognitiveBlocks = emptyList(),
            learnedProfile = learnedProfile
        )

        val curve = buildPredictedEnergyCurveUiModel(
            timeline = timeline,
            completedTasks = afternoonTasks,
            sessions = emptyList(),
            circadianSummary = summary,
            learnedProfile = learnedProfile,
            workDayStart = 8,
            workDayEnd = 20,
            currentHour = 13,
            zoneId = zoneId
        )

        assertTrue(curve.hasHistoricalData)
        assertEquals(6, curve.historicalCompletionCount)
        assertTrue(curve.modelConfidencePercent > 50)
        assertEquals(100, curve.hourlyPoints[16].historicalActivityScore)
        assertEquals(6, curve.hourlyPoints[16].historicalCompletionCount)
        assertTrue(
            "Expected bestAutoScheduleHour (${curve.bestAutoScheduleHour}) to be >= currentHour (13)",
            curve.bestAutoScheduleHour >= 13
        )
    }

    @Test
    fun `PlanViewModelV2 populates predictedEnergyCurve from historical completed tasks`() = runTest {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)
        val schedulerService = mockk<TaskSchedulerService>(relaxed = true)

        val sysZone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val completedYesterday = LocalDate.now(sysZone)
            .minusDays(1)
            .atTime(10, 30)
            .atZone(sysZone)
            .toInstant()
            .toEpochMilli()

        val completedTasks = listOf(
            TaskEntity(
                id = "comp_1",
                title = "Morning Deep Work",
                energyLevel = EnergyLevel.HIGH,
                status = TaskStatus.COMPLETED,
                completedAt = completedYesterday,
                updatedAt = completedYesterday
            ),
            TaskEntity(
                id = "comp_2",
                title = "System Design",
                energyLevel = EnergyLevel.HIGH,
                isFrog = true,
                status = TaskStatus.COMPLETED,
                completedAt = completedYesterday - 86_400_000L,
                updatedAt = completedYesterday - 86_400_000L
            )
        )

        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(workDayStart = 8, workDayEnd = 20)
        )
        every { taskRepo.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepo.observeCompletedTasks() } returns flowOf(completedTasks)

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            circadianEnergyService = circadianService,
            taskSchedulerService = schedulerService
        )

        val state = viewModel.uiState.filter { !it.isLoading }.first()
        val curve = state.predictedEnergyCurve

        assertNotNull(curve)
        assertTrue(curve!!.hasHistoricalData)
        assertEquals(2, curve.historicalCompletionCount)
        assertEquals(24, curve.hourlyPoints.size)
        assertEquals(2, curve.hourlyPoints[10].historicalCompletionCount)
        assertEquals(100, curve.hourlyPoints[10].historicalActivityScore)
        assertTrue(curve.modelConfidencePercent > 0)
    }
}
