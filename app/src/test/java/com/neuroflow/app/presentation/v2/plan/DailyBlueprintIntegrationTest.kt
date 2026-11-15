package com.neuroflow.app.presentation.v2.plan

import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.scheduler.BlueprintApplyResult
import com.neuroflow.app.domain.scheduler.BlueprintTaskItem
import com.neuroflow.app.domain.scheduler.DailyBlueprint
import com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestrator
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBlueprintIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val orchestrator = mockk<DailyBlueprintOrchestrator>(relaxed = true)

    private val defaultPrefs = UserPreferences(
        autoSchedulingEnabled = true,
        dailyDeepWorkTargetMinutes = 240
    )

    private val sampleTask1 = TaskEntity(
        id = "t1",
        title = "Deep Work Session",
        estimatedDurationMinutes = 60
    )
    private val sampleTask2 = TaskEntity(
        id = "t2",
        title = "Code Review",
        estimatedDurationMinutes = 45
    )

    private val sampleBlueprint = DailyBlueprint(
        targetDateMillis = 1700000000000L,
        plannedMinutes = 105,
        capacityTargetMinutes = 240,
        capacityUtilizationPercent = 43.75f,
        aiStrategyRationale = "Prioritize Deep Work during morning peak.",
        items = listOf(
            BlueprintTaskItem(
                task = sampleTask1,
                proposedStartMillis = 1700035200000L,
                proposedEndMillis = 1700038800000L,
                energyPhase = EnergyPhase.PEAK,
                isDuringPeak = true,
                isIncluded = true
            ),
            BlueprintTaskItem(
                task = sampleTask2,
                proposedStartMillis = 1700039400000L,
                proposedEndMillis = 1700042100000L,
                energyPhase = EnergyPhase.REBOUND,
                isDuringPeak = false,
                isIncluded = true
            )
        ),
        overflowTasks = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesDataStore.preferencesFlow } returns flowOf(defaultPrefs)
        every { taskRepository.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepository.observeCompletedTasks() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createPlanViewModel(): PlanViewModelV2 {
        val vm = PlanViewModelV2(
            taskRepository = taskRepository,
            preferencesDataStore = preferencesDataStore,
            workManager = workManager,
            dailyBlueprintOrchestrator = orchestrator
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        return vm
    }

    private fun TestScope.createFlowViewModel(): FlowViewModelV2 {
        val vm = FlowViewModelV2(
            taskRepository = taskRepository,
            preferencesDataStore = preferencesDataStore,
            dailyBlueprintOrchestrator = orchestrator
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        return vm
    }

    @Test
    fun testPlanViewModel_openBlueprint_generatesAndMapsState() = runTest(testDispatcher) {
        coEvery { orchestrator.generateBlueprint(any()) } returns sampleBlueprint
        val vm = createPlanViewModel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.dailyBlueprint)
        assertFalse(vm.uiState.value.isBlueprintLoading)

        vm.openDailyBlueprint(1700000000000L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.dailyBlueprint)
        assertEquals(105, state.dailyBlueprint?.plannedMinutes)
        assertEquals(setOf("t1", "t2"), state.blueprintIncludedTaskIds)
        assertFalse(state.isBlueprintLoading)
    }

    @Test
    fun testPlanViewModel_toggleBlueprintTask_updatesInclusion() = runTest(testDispatcher) {
        coEvery { orchestrator.generateBlueprint(any()) } returns sampleBlueprint
        val vm = createPlanViewModel()
        advanceUntilIdle()

        vm.openDailyBlueprint(1700000000000L)
        advanceUntilIdle()

        vm.toggleBlueprintTask("t1", isIncluded = false)
        advanceUntilIdle()
        assertEquals(setOf("t2"), vm.uiState.value.blueprintIncludedTaskIds)

        vm.toggleBlueprintTask("t1", isIncluded = true)
        advanceUntilIdle()
        assertEquals(setOf("t2", "t1"), vm.uiState.value.blueprintIncludedTaskIds)
    }

    @Test
    fun testPlanViewModel_applyBlueprint_commitsAndDismisses() = runTest(testDispatcher) {
        coEvery { orchestrator.generateBlueprint(any()) } returns sampleBlueprint
        coEvery { orchestrator.applyBlueprint(sampleBlueprint, setOf("t1")) } returns BlueprintApplyResult(
            committedCount = 1,
            committedTaskIds = listOf("t1")
        )

        val vm = createPlanViewModel()
        advanceUntilIdle()

        vm.openDailyBlueprint(1700000000000L)
        advanceUntilIdle()

        vm.toggleBlueprintTask("t2", isIncluded = false)
        vm.applyDailyBlueprint()
        advanceUntilIdle()

        coVerify { orchestrator.applyBlueprint(sampleBlueprint, setOf("t1")) }
        assertNull(vm.uiState.value.dailyBlueprint)
    }

    @Test
    fun testPlanViewModel_dismissDailyBlueprint_clearsState() = runTest(testDispatcher) {
        coEvery { orchestrator.generateBlueprint(any()) } returns sampleBlueprint
        val vm = createPlanViewModel()
        advanceUntilIdle()

        vm.openDailyBlueprint(1700000000000L)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.dailyBlueprint)

        vm.dismissDailyBlueprint()
        advanceUntilIdle()
        assertNull(vm.uiState.value.dailyBlueprint)
    }

    @Test
    fun testFlowViewModel_blueprintWorkflow() = runTest(testDispatcher) {
        coEvery { orchestrator.generateBlueprint(any()) } returns sampleBlueprint
        coEvery { orchestrator.applyBlueprint(sampleBlueprint, setOf("t1", "t2")) } returns BlueprintApplyResult(
            committedCount = 2,
            committedTaskIds = listOf("t1", "t2")
        )

        val vm = createFlowViewModel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.dailyBlueprint)

        vm.openDailyBlueprint(1700000000000L)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.dailyBlueprint)
        assertEquals(setOf("t1", "t2"), vm.uiState.value.blueprintIncludedTaskIds)

        vm.applyDailyBlueprint()
        advanceUntilIdle()

        coVerify { orchestrator.applyBlueprint(sampleBlueprint, setOf("t1", "t2")) }
        assertNull(vm.uiState.value.dailyBlueprint)
        assertEquals("Scheduled 2 tasks", vm.uiState.value.lastActionMessage)
    }
}
