package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class NewTaskFlowViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun buildViewModel(prefs: UserPreferences = UserPreferences(autoSchedulingEnabled = true)): NewTaskFlowViewModel {
        val dataStore = mockk<UserPreferencesDataStore>()
        every { dataStore.preferencesFlow } returns MutableStateFlow(prefs)
        val vm = NewTaskFlowViewModel(dataStore)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    "buildTaskPayload saves execution details regardless of the section toggle" {
        val vm = buildViewModel()
        vm.updateTitle("Test task")
        vm.updateEstimatedDuration(0)
        vm.updateWaitingFor("External approval")
        vm.updatePlan("Step 1\nStep 2")
        vm.toggleDependency("dep1")
        vm.setIncludeExecutionStep(false)

        val task = vm.buildTaskPayload(existing = null)
        task.title shouldBe "Test task"
        task.estimatedDurationMinutes shouldBe 0
        task.waitingFor shouldBe "External approval"
        task.dependsOnTaskIds shouldBe "dep1"
        task.ifThenPlan shouldBe "Step 1\nStep 2"
    }

    "buildTaskPayload retains execution details when includeExecutionStep is true" {
        val vm = buildViewModel()
        vm.updateTitle("Test task with execution details")
        vm.updateEstimatedDuration(45)
        vm.updateWaitingFor("External approval")
        vm.updatePlan("Step 1\nStep 2")
        vm.toggleDependency("dep1")
        vm.setIncludeExecutionStep(true)

        val task = vm.buildTaskPayload(existing = null)
        task.estimatedDurationMinutes shouldBe 45
        task.waitingFor shouldBe "External approval"
        task.dependsOnTaskIds shouldBe "dep1"
        task.ifThenPlan shouldBe "Step 1\nStep 2"
    }

    "setDateToday selects the start of the current local day" {
        val vm = buildViewModel()
        val expected = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        vm.setDateToday()

        vm.uiState.value.scheduledDate shouldBe expected
    }

    "setDateTomorrow selects the start of the next local day" {
        val vm = buildViewModel()
        val expected = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        vm.setDateTomorrow()

        vm.uiState.value.scheduledDate shouldBe expected
    }

    "buildTaskPayload preserves a quick-selected scheduled date" {
        val vm = buildViewModel()
        vm.updateTitle("Scheduled task")
        vm.setDateTomorrow()
        val selectedDate = vm.uiState.value.scheduledDate

        val task = vm.buildTaskPayload(existing = null)

        task.scheduledDate shouldBe selectedDate
    }

    "autoSchedulePreview indicates not eligible when waitingFor is set" {
        val vm = buildViewModel()
        vm.updateTitle("Waiting task")
        vm.setIncludeExecutionStep(true)
        vm.updateWaitingFor("Client feedback")

        val preview = vm.autoSchedulePreview()
        preview.willAutoSchedule shouldBe false
        preview.reason.contains("external dependency") shouldBe true
    }

    "autoSchedulePreview indicates not eligible when dependencies are set" {
        val vm = buildViewModel()
        vm.updateTitle("Dependent task")
        vm.setIncludeExecutionStep(true)
        vm.toggleDependency("dep1")

        val preview = vm.autoSchedulePreview()
        preview.willAutoSchedule shouldBe false
        preview.reason.contains("prerequisite task") shouldBe true
    }

    "autoSchedulePreview indicates eligible when no blockers exist" {
        val vm = buildViewModel()
        vm.updateTitle("Normal task")
        vm.setIncludeExecutionStep(false)

        val preview = vm.autoSchedulePreview()
        preview.willAutoSchedule shouldBe true
        preview.reason.contains("Eligible for automatic scheduling") shouldBe true
    }
})
