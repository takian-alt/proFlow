package com.neuroflow.app.presentation.today

import android.content.Context
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.SlotPhase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Calendar

/**
 * The slot fields are derived from the wall clock, so the fixtures are built from "now" rather than
 * fixed dates. Offsets sit well inside the windows, so a minute ticking over mid-test cannot flip
 * a phase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelProtectedSlotTest : StringSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeTest { Dispatchers.setMain(testDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun slot(label: String, offsetMinutes: Int, durationMinutes: Int): ProtectedTimeSlot {
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, offsetMinutes) }
        return ProtectedTimeSlot(
            id = label,
            label = label,
            startMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE),
            durationMinutes = durationMinutes,
            recurrence = PtsRecurrence.DAILY
        )
    }

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun quietTask() = TaskEntity(
        id = "quiet",
        title = "File the expense report",
        quadrant = Quadrant.ELIMINATE,
        status = TaskStatus.ACTIVE,
        priority = Priority.LOW,
        impactScore = 1,
        valueScore = 1,
        scheduledDate = todayStart
    )

    fun loudTask() = TaskEntity(
        id = "loud",
        title = "Write the proposal",
        quadrant = Quadrant.DO_FIRST,
        status = TaskStatus.ACTIVE,
        priority = Priority.HIGH,
        impactScore = 95,
        valueScore = 95,
        isFrog = true,
        scheduledDate = todayStart
    )

    fun viewModelWith(
        slots: List<ProtectedTimeSlot>,
        tasks: List<TaskEntity>
    ) = TodayViewModel(
        context = mockk<Context>(relaxed = true),
        taskRepository = mockk<TaskRepository> {
            every { observeActiveTasks() } returns flowOf(tasks)
            every { observeCompletedTasks() } returns flowOf(emptyList())
        },
        sessionRepository = mockk<SessionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        },
        preferencesDataStore = mockk<UserPreferencesDataStore> {
            every { preferencesFlow } returns flowOf(UserPreferences(protectedTimeSlots = slots))
        }
    )

    "a running block takes the hero position and the recommendation drops to the head of the queue" {
        val viewModel = viewModelWith(
            slots = listOf(slot("Gym", offsetMinutes = -5, durationMinutes = 30)),
            tasks = listOf(quietTask(), loudTask())
        )

        val state = viewModel.uiState.first { it.todayStartMillis != 0L }

        state.dueProtectedSlot?.slot?.label shouldBe "Gym"
        state.dueProtectedSlot?.phase shouldBe SlotPhase.RUNNING
        state.recommendedTask?.id shouldBe "loud"
        state.upNextTasks.map { it.id } shouldBe listOf("loud", "quiet")
    }

    "the queue keeps its own order while no block is running" {
        val viewModel = viewModelWith(
            slots = listOf(slot("Gym", offsetMinutes = 8 * 60, durationMinutes = 30)),
            tasks = listOf(quietTask(), loudTask())
        )

        val state = viewModel.uiState.first { it.todayStartMillis != 0L }

        state.dueProtectedSlot shouldBe null
        state.approachingProtectedSlots.isEmpty() shouldBe true
        state.recommendedTask?.id shouldBe "loud"
        state.upNextTasks.map { it.id } shouldBe listOf("quiet", "loud")
    }

    "a block that has not started yet queues without taking the top" {
        val viewModel = viewModelWith(
            slots = listOf(slot("Dinner", offsetMinutes = 45, durationMinutes = 60)),
            tasks = listOf(quietTask())
        )

        val state = viewModel.uiState.first { it.todayStartMillis != 0L }

        state.dueProtectedSlot shouldBe null
        state.approachingProtectedSlots.single().slot.label shouldBe "Dinner"
        state.approachingProtectedSlots.single().phase shouldBe SlotPhase.LEAD_IN
        state.upNextTasks.map { it.id } shouldBe listOf("quiet")
    }
})
