package com.neuroflow.app.presentation.v2.flow

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class FlowViewModelSimulationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepo = mockk<TaskRepository>(relaxed = true)
    private val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testToggleNapLeverUpdatesSimulationOutcomeAndApplyCreatesProtectedSlot() = runTest(testDispatcher) {
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
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // 1. Activate simulation mode
        viewModel.toggleSimulationMode(true)
        advanceUntilIdle()
        assertTrue("Simulation should be active", viewModel.uiState.value.simulationState.isActive)

        // 2. Toggle NAP lever
        viewModel.toggleSimulatedLever(SimulationLeverType.NAP)
        advanceUntilIdle()
        val simState = viewModel.uiState.value.simulationState
        assertTrue("Should have 1 simulated nap", simState.simulatedNaps.isNotEmpty())
        assertNotNull("Should have non-null simulation outcome", simState.outcome)
        assertTrue("Net energy delta should be positive", simState.outcome!!.netEnergyDeltaPercent > 0)

        // 3. Apply to plan
        viewModel.applySimulationToPlan()
        advanceUntilIdle()

        // Simulation should now be deactivated
        assertFalse("Simulation should be deactivated after apply", viewModel.uiState.value.simulationState.isActive)

        // ProtectedTimeSlot should be saved with label containing Nap
        val slotsCapture = slot<List<ProtectedTimeSlot>>()
        coVerify { prefsDataStore.updateProtectedTimeSlots(capture(slotsCapture)) }
        val napSlot = slotsCapture.captured.firstOrNull { it.label.contains("Nap", ignoreCase = true) }
        assertNotNull("Should have a protected nap slot", napSlot)
        assertEquals(20, napSlot!!.durationMinutes)
    }
}
