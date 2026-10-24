package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class RealWorldChronobiologyAndSchedulerTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testCircularClockHourBlendingHandlesMidnightWrapAround() {
        // Linear average of 23:00 and 01:00 would incorrectly be 12:00 (noon)
        // Circular unit-vector average correctly produces 00:00 (midnight)
        val blended = blendClockHoursCircular(
            listOf(23.0 to 1.0, 1.0 to 1.0)
        )
        // Should be approximately 0.0 or 24.0 (mod 24 = 0.0)
        val error = kotlin.math.min(blended, kotlin.math.abs(24.0 - blended))
        assertTrue("Blended angle across midnight should be near 0.0 (midnight), was $blended", error < 0.05)
    }

    @Test
    fun testLateWakerIsNeverScheduledWhileAsleep() {
        val testDate = LocalDate.of(2026, 10, 20)
        val dayStart = testDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val tasks = listOf(
            SchedulableTask(
                id = "morning_deep",
                title = "Deep Work",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 60,
                energyDemand = EnergyDemand.HIGH,
                isFrog = true
            )
        )

        val placements = schedulerService.scheduleDay(
            tasks = tasks,
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertTrue("Placements must not be empty", placements.isNotEmpty())
        val firstStart = placements.first().window.startMillis
        val minAllowedStart = wakeTime + 15 * 60_000L // Wake time + 15m buffer
        assertTrue(
            "Task scheduled start ($firstStart) must be >= wake + 15m ($minAllowedStart), user was asleep!",
            firstStart >= minAllowedStart
        )
    }

    @Test
    fun testMultiPrerequisiteDAGScheduleOrder() {
        val testDate = LocalDate.of(2026, 10, 20)
        val wakeTime = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // DAG: A -> C and B -> C (C depends on both A and B)
        val taskA = SchedulableTask(
            id = "task_A",
            title = "Prerequisite A",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskB = SchedulableTask(
            id = "task_B",
            title = "Prerequisite B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskC = SchedulableTask(
            id = "task_C",
            title = "Downstream Target C",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("task_A", "task_B")
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(taskC, taskA, taskB),
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertEquals("All 3 tasks should be placed", 3, placements.size)
        val placementMap = placements.associateBy { it.task.id }
        val endA = placementMap["task_A"]!!.window.endMillis
        val endB = placementMap["task_B"]!!.window.endMillis
        val startC = placementMap["task_C"]!!.window.startMillis

        assertTrue(
            "Task C ($startC) must start after Task A finishes ($endA)",
            startC >= endA
        )
        assertTrue(
            "Task C ($startC) must start after Task B finishes ($endB)",
            startC >= endB
        )
    }

    @Test
    fun testDualRepresentationTimestampsResolveConsistently() {
        val todayStart = 1770000000000L
        val nineAmOffset = 9 * 3600_000L

        // Format 1: Intraday ms offset (< 86_400_000L)
        val taskOffset = TaskEntity(
            id = "1",
            title = "Offset Task",
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset
        )

        // Format 2: Absolute epoch millis (>= 86_400_000L)
        val taskEpoch = TaskEntity(
            id = "2",
            title = "Epoch Task",
            scheduledDate = todayStart,
            scheduledTime = todayStart + nineAmOffset
        )

        val resolvedOffset = taskOffset.resolveScheduledEpochMillis()
        val resolvedEpoch = taskEpoch.resolveScheduledEpochMillis()

        assertNotNull(resolvedOffset)
        assertNotNull(resolvedEpoch)
        assertEquals(
            "Both representations must resolve to the identical timestamp without 2082 or 1970 corruption",
            resolvedOffset,
            resolvedEpoch
        )
        assertEquals(todayStart + nineAmOffset, resolvedEpoch)
    }

    @Test
    fun testSimulatedFocusLoadAccumulatesAcrossConsecutiveDeepWork() {
        val baseTime = 1770000000000L
        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "1", title = "Block 1", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime, baseTime + 45 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("1", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )
        val p2 = ScheduledPlacement(
            task = SchedulableTask(id = "2", title = "Block 2", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime + 50 * 60_000L, baseTime + 95 * 60_000L), // 5m gap
            scoreBreakdown = TaskScoreBreakdown("2", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )

        val defaultService = schedulerService as DefaultTaskSchedulerService
        val loadRightAfter = defaultService.computeSimulatedFocusLoadAt(baseTime + 100 * 60_000L, listOf(p1, p2))
        assertEquals("Simulated focus load should accumulate 45 + 45 = 90 minutes", 90, loadRightAfter)

        val loadAfterLongRest = defaultService.computeSimulatedFocusLoadAt(baseTime + 160 * 60_000L, listOf(p1, p2)) // 65m gap
        assertEquals("Simulated focus load should reset to 0 after long recovery gap", 0, loadAfterLongRest)
    }
}
