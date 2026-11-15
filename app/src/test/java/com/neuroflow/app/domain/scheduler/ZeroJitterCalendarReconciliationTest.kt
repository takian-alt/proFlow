package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.calendar.ExternalCalendarDelta
import com.neuroflow.app.data.calendar.ExternalDeltaType
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ZeroJitterCalendarReconciliationTest {
    private val calendarSyncRepository = mockk<CalendarSyncRepository>(relaxed = true)
    private val orchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = mockk(relaxed = true),
        behavioralSupportService = mockk(relaxed = true)
    )

    @Test
    fun testReconcileExternalCalendarDeltas_nudgesOverlappingTaskWithMinimalDisplacement() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val taskStart = dayStart + 10 * 3600_000L // 10:00 AM
        val task = TaskEntity(
            id = "task_overlap",
            title = "Code Review",
            scheduledDate = dayStart,
            scheduledTime = taskStart,
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.SCHEDULE,
            energyLevel = EnergyLevel.HIGH
        )
        val busyDelta = ExternalCalendarDelta(
            eventId = 101L,
            type = ExternalDeltaType.EVENT_ADDED,
            startMillis = dayStart + 10 * 3600_000L, // 10:00 - 11:00 AM external event
            endMillis = dayStart + 11 * 3600_000L,
            title = "Team Meeting"
        )
        val busyWindows = listOf(TimeSlotWindow(busyDelta.startMillis!!, busyDelta.endMillis!!))
        coEvery { calendarSyncRepository.getBusyWindows(any(), any()) } returns busyWindows

        val result = orchestrator.reconcileExternalCalendarDeltas(
            dayDateMillis = dayStart,
            tasks = listOf(task),
            deltas = listOf(busyDelta),
            busyWindows = busyWindows
        )

        assertEquals(1, result.nudgedTasks.size)
        assertTrue(result.conflictedTasks.isEmpty())
        val nudged = result.nudgedTasks.first()
        assertEquals("task_overlap", nudged.id)
        assertTrue("Nudge should move task past meeting", nudged.scheduledTime!! >= busyDelta.endMillis!!)
    }

    @Test
    fun testReconcileExternalCalendarDeltas_zeroJitterForNonOverlappingTasks() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()

        val safeTask = TaskEntity(
            id = "task_safe",
            title = "Morning Planning",
            scheduledDate = dayStart,
            scheduledTime = dayStart + 8 * 3600_000L, // 8:00 AM - 9:00 AM
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.SCHEDULE
        )
        val overlappingTask = TaskEntity(
            id = "task_overlap",
            title = "Deep Focus",
            scheduledDate = dayStart,
            scheduledTime = dayStart + 10 * 3600_000L, // 10:00 AM - 11:00 AM
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.SCHEDULE
        )

        val busyDelta = ExternalCalendarDelta(
            eventId = 102L,
            type = ExternalDeltaType.EVENT_ADDED,
            startMillis = dayStart + 10 * 3600_000L,
            endMillis = dayStart + 11 * 3600_000L,
            title = "Client Call"
        )
        val busyWindows = listOf(TimeSlotWindow(busyDelta.startMillis!!, busyDelta.endMillis!!))

        val result = orchestrator.reconcileExternalCalendarDeltas(
            dayDateMillis = dayStart,
            tasks = listOf(safeTask, overlappingTask),
            deltas = listOf(busyDelta),
            busyWindows = busyWindows
        )

        // Only the overlapping task is nudged; the safe task remains untouched (zero jitter)
        assertEquals(1, result.nudgedTasks.size)
        assertEquals("task_overlap", result.nudgedTasks.first().id)
        assertTrue(result.conflictedTasks.isEmpty())
        assertTrue("Safe task must not be in nudged tasks", result.nudgedTasks.none { it.id == "task_safe" })
    }

    @Test
    fun testReconcileExternalCalendarDeltas_marksConflictedWhenDayIsSaturated() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()

        val task = TaskEntity(
            id = "task_unfittable",
            title = "Large Architectural Design",
            scheduledDate = dayStart,
            scheduledTime = dayStart + 10 * 3600_000L,
            estimatedDurationMinutes = 120, // 2 hours
            quadrant = Quadrant.SCHEDULE
        )

        // Saturate wake hours (7 AM to 11 PM) with busy windows
        val busyWindows = listOf(
            TimeSlotWindow(dayStart + 7 * 3600_000L, dayStart + 23 * 3600_000L)
        )

        val result = orchestrator.reconcileExternalCalendarDeltas(
            dayDateMillis = dayStart,
            tasks = listOf(task),
            deltas = emptyList(),
            busyWindows = busyWindows
        )

        assertTrue(result.nudgedTasks.isEmpty())
        assertEquals(1, result.conflictedTasks.size)
        assertEquals("task_unfittable", result.conflictedTasks.first().id)
        // Ensure task is preserved non-destructively
        assertEquals(task.scheduledTime, result.conflictedTasks.first().scheduledTime)
    }

    @Test
    fun testReconcileExternalCalendarDeltas_choosesCloserGapBackward() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Task at 14:00 (1 hour duration)
        val task = TaskEntity(
            id = "task_afternoon",
            title = "Design Sync",
            scheduledDate = dayStart,
            scheduledTime = dayStart + 14 * 3600_000L, // 2:00 PM
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.SCHEDULE
        )

        // External meeting from 14:00 to 18:00 (4 hours)
        // Gap before: 13:00 to 14:00 (free) -> distance is 1 hour
        // Gap after: 18:00+ -> distance is 4 hours
        val busyWindows = listOf(
            TimeSlotWindow(dayStart + 7 * 3600_000L, dayStart + 13 * 3600_000L), // Morning busy until 1 PM
            TimeSlotWindow(dayStart + 14 * 3600_000L, dayStart + 18 * 3600_000L) // Meeting 2 PM - 6 PM
        )

        val result = orchestrator.reconcileExternalCalendarDeltas(
            dayDateMillis = dayStart,
            tasks = listOf(task),
            deltas = emptyList(),
            busyWindows = busyWindows
        )

        assertEquals(1, result.nudgedTasks.size)
        val nudged = result.nudgedTasks.first()
        assertEquals("task_afternoon", nudged.id)
        // Should choose 13:00 (1 hour displacement backwards) over 18:00 (4 hours displacement forwards)
        assertEquals(dayStart + 13 * 3600_000L, nudged.scheduledTime)
    }
}
