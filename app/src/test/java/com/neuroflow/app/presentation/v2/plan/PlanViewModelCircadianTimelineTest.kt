package com.neuroflow.app.presentation.v2.plan

import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.TaskSchedulerService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class PlanViewModelCircadianTimelineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepo = mockk<TaskRepository>(relaxed = true)
    private val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val circadianService: CircadianEnergyService = DefaultCircadianEnergyService()

    private val defaultPrefs = UserPreferences(
        autoSchedulingEnabled = true,
        dailyDeepWorkTargetMinutes = 240,
        workDayStart = 8,
        workDayEnd = 18
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { prefsDataStore.preferencesFlow } returns flowOf(defaultPrefs)
        every { taskRepo.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepo.observeCompletedTasks() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTaskInPeakWindowReceivesOptimalAlignmentScore() {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val scheduledMillis = todayStart + 10 * 3600_000L // 10:00 AM

        val timeline = listOf(
            CircadianSnapshot(
                timestampMillis = scheduledMillis,
                usableEnergy = 85,
                homeostaticPressureS = 0.25,
                circadianAlertnessC = 0.9,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.PEAK,
                sleepInertiaPenalty = 0.0
            )
        )

        val task = TaskEntity(
            id = "task_peak_high",
            title = "Deep Architecture Refactor",
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        val alignment = PlanViewModelV2.evaluateCircadianAlignmentForSlot(task, scheduledMillis, timeline)
        assertEquals(CircadianAlignmentStatus.OPTIMAL_PEAK, alignment.status)
        assertTrue(alignment.label.contains("Optimal Peak Alignment"))
        assertEquals(0, alignment.penaltyPercent)
    }

    @Test
    fun testTaskInDipWindowReceivesDipMismatchWarning() {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val scheduledMillis = todayStart + 14 * 3600_000L // 14:00 (2 PM)

        val timeline = listOf(
            CircadianSnapshot(
                timestampMillis = scheduledMillis,
                usableEnergy = 42,
                homeostaticPressureS = 0.55,
                circadianAlertnessC = 0.4,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.DIP,
                sleepInertiaPenalty = 0.0
            )
        )

        val task = TaskEntity(
            id = "task_dip_high",
            title = "High Energy Design in Midday Lull",
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 90,
            scheduledDate = todayStart,
            scheduledTime = 14 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        val alignment = PlanViewModelV2.evaluateCircadianAlignmentForSlot(task, scheduledMillis, timeline)
        assertEquals(CircadianAlignmentStatus.DIP_MISMATCH, alignment.status)
        assertTrue(alignment.label.contains("Dip Mismatch"))
        assertEquals(20, alignment.penaltyPercent)
    }

    @Test
    fun testLowEnergyTaskInPeakWindowReceivesCannibalizesPeakWarning() {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val scheduledMillis = todayStart + 10 * 3600_000L // 10:00 AM

        val timeline = listOf(
            CircadianSnapshot(
                timestampMillis = scheduledMillis,
                usableEnergy = 88,
                homeostaticPressureS = 0.20,
                circadianAlertnessC = 0.95,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.PEAK,
                sleepInertiaPenalty = 0.0
            )
        )

        val task = TaskEntity(
            id = "task_low_in_peak",
            title = "File Expense Receipt",
            energyLevel = EnergyLevel.LOW,
            estimatedDurationMinutes = 15,
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        val alignment = PlanViewModelV2.evaluateCircadianAlignmentForSlot(task, scheduledMillis, timeline)
        assertEquals(CircadianAlignmentStatus.CANNIBALIZES_PEAK, alignment.status)
        assertTrue(alignment.label.contains("Cannibalizes Peak Focus"))
    }

    @Test
    fun testShiftTaskToPeakByIdFindsAndSchedulesTask() = runTest {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val task = TaskEntity(
            id = "task_to_shift",
            title = "Important Work",
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 60,
            scheduledDate = todayStart,
            scheduledTime = 14 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        every { taskRepo.observeActiveTasks() } returns flowOf(listOf(task))
        coEvery { taskRepo.getById("task_to_shift") } returns task

        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            circadianEnergyService = circadianService,
            taskSchedulerService = schedulerService
        )

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.shiftTaskToPeak("task_to_shift")
        advanceUntilIdle()

        coVerify { taskRepo.update(match { it.id == "task_to_shift" && it.isScheduleLocked }) }
    }

    @Test
    fun testCircadianSummaryUsesLiveTimestampWhenTargetingToday() = runTest {
        val viewModel = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = workManager,
            circadianEnergyService = circadianService,
            taskSchedulerService = schedulerService
        )

        val beforeCall = System.currentTimeMillis()
        val state = viewModel.uiState.filter { !it.isLoading }.first()
        val afterCall = System.currentTimeMillis()

        val summaryTs = state.circadianSummary?.currentSnapshot?.timestampMillis ?: 0L
        assertTrue(
            "circadianSummary timestamp ($summaryTs) should reflect live now [$beforeCall..$afterCall] rather than midnight",
            summaryTs in (beforeCall - 5_000L)..(afterCall + 5_000L)
        )
    }
}
