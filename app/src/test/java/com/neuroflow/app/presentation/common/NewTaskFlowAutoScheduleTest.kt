package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * The auto-scheduler leaves a full timestamp behind, so the editor has to tell "the scheduler placed
 * this" apart from "the user picked this" — only the persisted flag can do that.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewTaskFlowAutoScheduleTest : StringSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeTest { Dispatchers.setMain(testDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun viewModel() = NewTaskFlowViewModel(
        mockk<UserPreferencesDataStore> {
            every { preferencesFlow } returns flowOf(UserPreferences(autoSchedulingEnabled = true))
        }
    )

    fun autoPlaced(withDeadline: Boolean = false) = TaskEntity(
        id = "report",
        title = "Write the monthly report",
        status = TaskStatus.ACTIVE,
        quadrant = Quadrant.DO_FIRST,
        scheduledDate = 1_760_000_000_000L,
        scheduledTime = 1_760_010_000_000L,
        deadlineDate = if (withDeadline) 1_760_300_000_000L else null,
        estimatedDurationMinutes = 60,
        isAutoScheduled = true
    )

    "a task the auto-scheduler placed reopens with the auto-schedule switch on" {
        val viewModel = viewModel()

        viewModel.initialize(editTask = autoPlaced(), prefilledQuadrant = null, availableTasks = emptyList())

        viewModel.uiState.value.isAutoScheduleMode shouldBe true
    }

    "the preview keeps offering to auto-schedule a task it already placed" {
        val viewModel = viewModel()
        viewModel.initialize(
            editTask = autoPlaced(withDeadline = true),
            prefilledQuadrant = null,
            availableTasks = emptyList()
        )

        val preview = viewModel.autoSchedulePreview()

        preview.willAutoSchedule shouldBe true
    }

    "saving a placed task without touching the timing keeps the scheduler's ownership" {
        val viewModel = viewModel()
        val task = autoPlaced()
        viewModel.initialize(editTask = task, prefilledQuadrant = null, availableTasks = emptyList())

        viewModel.buildTaskPayload(existing = task).isAutoScheduled shouldBe true
    }

    "choosing a new start time hands ownership of the slot to the user" {
        val viewModel = viewModel()
        val task = autoPlaced()
        viewModel.initialize(editTask = task, prefilledQuadrant = null, availableTasks = emptyList())

        viewModel.updateScheduledTime(1_760_020_000_000L)

        viewModel.buildTaskPayload(existing = task).isAutoScheduled shouldBe false
    }

    "turning auto-schedule on releases the placed time" {
        val viewModel = viewModel()
        val task = autoPlaced()
        viewModel.initialize(editTask = task, prefilledQuadrant = null, availableTasks = emptyList())

        viewModel.updateScheduledDate(null)
        viewModel.updateScheduledTime(null)
        viewModel.updateScheduleLocked(false)

        viewModel.uiState.value.isAutoScheduleMode shouldBe true
        viewModel.buildTaskPayload(existing = task).isAutoScheduled shouldBe false
    }
})
