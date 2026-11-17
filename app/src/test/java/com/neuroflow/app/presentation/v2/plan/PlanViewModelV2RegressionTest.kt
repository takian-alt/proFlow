package com.neuroflow.app.presentation.v2.plan

import android.content.Context
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.MultiDayScheduleOrchestrator
import com.neuroflow.app.domain.scheduler.MultiDaySchedulingOutcome
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskScoreBreakdown
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelV2RegressionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(AutonomyNudgeEngine)
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AutonomyNudgeEngine)
        Dispatchers.resetMain()
    }

    @Test
    fun `requestDeleteTask is blocked when HyperFocusManager locks task`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val hyperFocusManager = mockk<HyperFocusManager>(relaxed = true)
        val task = TaskEntity(id = "locked-hf-1", title = "Locked Deliverable")

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
        coEvery { hyperFocusManager.isTaskDeletionBlocked("locked-hf-1") } returns true

        val vm = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = mockk<WorkManager>(relaxed = true),
            hyperFocusManager = hyperFocusManager
        )

        var msg: String? = null
        val collectJob = backgroundScope.launch {
            vm.userActionMessage.collect { msg = it }
        }

        vm.requestDeleteTask(task)
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingDeletedTask)
        assertTrue(msg?.contains("Locked by active HyperFocus") == true)
        coVerify(exactly = 0) { taskRepo.delete(any()) }
        collectJob.cancel()
    }

    @Test
    fun `acceptRemedySlot locks task slot and merges directivePatch cleanly`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
            UserPreferences(adaptiveSystemDirective = "Avoid early meetings")
        )

        val vm = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = mockk<WorkManager>(relaxed = true)
        )

        val task = TaskEntity(id = "remedy-1", title = "Deep Spec", isAutoScheduled = true)
        val zoneId = ZoneId.systemDefault()
        val newSlotMillis = LocalDate.now(zoneId).atTime(10, 30).atZone(zoneId).toInstant().toEpochMilli()

        vm.acceptRemedySlot(task, newSlotMillis, "Prefer 10:30 AM deep blocks")
        advanceUntilIdle()

        coVerify {
            taskRepo.update(match {
                it.id == "remedy-1" &&
                    !it.isAutoScheduled &&
                    it.isScheduleLocked &&
                    it.scheduledTime == (10 * 60 + 30) * 60_000L
            })
        }
        coVerify {
            prefsDataStore.updateAdaptiveSystemDirective(
                directive = "Avoid early meetings\nPrefer 10:30 AM deep blocks",
                auditJson = any()
            )
        }
    }

    @Test
    fun `scheduleTaskForToday and parkTaskInNotes triage inbox tasks properly`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val quickNoteDao = mockk<QuickNoteDao>(relaxed = true)

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val vm = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = mockk<WorkManager>(relaxed = true),
            quickNoteDao = quickNoteDao
        )

        val inboxTask = TaskEntity(id = "inbox-1", title = "Read Paper", estimatedDurationMinutes = 25)
        vm.scheduleTaskForToday(inboxTask)
        advanceUntilIdle()

        coVerify {
            taskRepo.update(match {
                it.id == "inbox-1" && it.scheduledDate != null && !it.isAutoScheduled
            })
        }

        val parkTask = TaskEntity(id = "inbox-2", title = "Future Idea", description = "Check GraphQL tooling")
        vm.parkTaskInNotes(parkTask)
        advanceUntilIdle()

        val noteSlot = slot<QuickNoteEntity>()
        coVerify { quickNoteDao.insert(capture(noteSlot)) }
        assertEquals("Check GraphQL tooling", noteSlot.captured.content)
        coVerify { taskRepo.delete(parkTask) }
    }

    @Test
    fun `autoBalance splits long task into parent and chained hash part2 without inheriting frog or recurrence`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val multiDayOrch = mockk<MultiDayScheduleOrchestrator>(relaxed = true)

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val longTask = TaskEntity(
            id = "long-1",
            title = "System Redesign",
            estimatedDurationMinutes = 90,
            energyLevel = EnergyLevel.HIGH,
            isFrog = true,
            recurrence = Recurrence.DAILY,
            status = TaskStatus.ACTIVE
        )

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(longTask))
        coEvery { taskRepo.getActiveTasks() } returns listOf(longTask)
        coEvery { taskRepo.getById("long-1") } returns longTask
        coEvery { taskRepo.getById("long-1#part2") } returns null
        coEvery { taskRepo.getById("long-1_part2") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val schedTaskPart1 = SchedulableTask(
            id = "long-1#part1",
            title = "System Redesign",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true
        )
        val schedTaskPart2 = schedTaskPart1.copy(id = "long-1#part2", isFrog = false)
        val placement1 = ScheduledPlacement(
            task = schedTaskPart1,
            window = TimeSlotWindow(dayStart + 9 * 3600_000L, dayStart + 9 * 3600_000L + 45 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("long-1#part1", 90.0, 80.0, 80.0, 90.0, 80.0, 45)
        )
        val placement2 = ScheduledPlacement(
            task = schedTaskPart2,
            window = TimeSlotWindow(dayStart + 11 * 3600_000L, dayStart + 11 * 3600_000L + 45 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("long-1#part2", 85.0, 80.0, 80.0, 85.0, 80.0, 45)
        )

        coEvery {
            multiDayOrch.orchestrateMultiDayScheduleWithContext(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()
            )
        } returns MultiDaySchedulingOutcome(
            dayOutcomes = mapOf(today to SchedulingOutcome(placements = listOf(placement1, placement2))),
            totalScheduledTasks = 2,
            crossDaySplits = emptyList(),
            finalDeferred = emptyList()
        )

        val vm = PlanViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            workManager = mockk<WorkManager>(relaxed = true),
            multiDayScheduleOrchestrator = multiDayOrch
        )

        vm.autoBalance(horizonDays = 7).join()
        advanceUntilIdle()

        coVerify {
            taskRepo.update(match {
                it.id == "long-1" &&
                    it.title == "System Redesign (Part 1/2)" &&
                    it.plannedBlockMinutes == 45
            })
        }
        val part2Slot = slot<TaskEntity>()
        coVerify { taskRepo.insert(capture(part2Slot)) }
        assertEquals("long-1#part2", part2Slot.captured.id)
        assertEquals("System Redesign (Part 2/2)", part2Slot.captured.title)
        assertEquals("long-1", part2Slot.captured.parentTaskId)
        assertEquals("long-1", part2Slot.captured.dependsOnTaskIds)
        assertFalse(part2Slot.captured.isFrog)
        assertEquals(Recurrence.NONE, part2Slot.captured.recurrence)
    }
}
