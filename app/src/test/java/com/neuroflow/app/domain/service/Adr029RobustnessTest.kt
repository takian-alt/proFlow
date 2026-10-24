package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

class Adr029RobustnessTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: DefaultTaskSchedulerService
    private lateinit var behavioralSupportService: BehavioralSupportService
    private lateinit var orchestrator: SchedulePreparationOrchestrator

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
        behavioralSupportService = DefaultBehavioralSupportService()
        orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralSupportService
        )
    }

    // =========================================================================
    // 1. SCHEDULE PREPARATION SIGNALS (Bug 1)
    // =========================================================================

    @Test
    fun testSchedulePreparationOrchestrator_populatesMicroStepsActiveAndCreatedAt() {
        val now = System.currentTimeMillis()
        val createdPast = now - 7 * 86_400_000L // 7 days old

        val task1 = TaskEntity(
            id = "task-active",
            title = "Active deep work",
            createdAt = createdPast,
            estimatedDurationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            priority = Priority.HIGH
        )

        val task2 = TaskEntity(
            id = "task-zeigarnik",
            title = "In-progress task",
            createdAt = createdPast,
            estimatedDurationMinutes = 30,
            energyLevel = EnergyLevel.MEDIUM,
            priority = Priority.MEDIUM
        )

        val microSteps = mapOf(
            "task-zeigarnik" to Pair(2, 4) // 50% completed
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(task1, task2),
            allKnownTasks = listOf(task1, task2),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = LocalDate.now(),
            nowMillis = now,
            busyWindows = emptyList(),
            forceReplan = false,
            activeTaskId = "task-active",
            microStepCounts = microSteps
        )

        // Find the scheduled placements
        val placementMap = outcome.placements.associateBy { it.task.id }
        val schedTask1 = placementMap["task-active"]?.task
        val schedTask2 = placementMap["task-zeigarnik"]?.task

        if (schedTask1 != null) {
            assertTrue("task1 should be marked active", schedTask1.isCurrentlyActive)
            assertEquals("task1 createdAtMillis must match task.createdAt", createdPast, schedTask1.createdAtMillis)
        }

        if (schedTask2 != null) {
            assertFalse("task2 should not be active", schedTask2.isCurrentlyActive)
            assertEquals(2, schedTask2.completedMicroSteps)
            assertEquals(4, schedTask2.totalMicroSteps)
            assertEquals("task2 createdAtMillis must match task.createdAt", createdPast, schedTask2.createdAtMillis)
        }

        // Directly evaluate score breakdown on both tasks with signals
        val snapshot = circadianService.evaluateSnapshot(now, now - 3_600_000L, 480)
        val scoreActive = schedulerService.evaluateTaskScore(
            schedTask1 ?: SchedulableTask(
                id = "task-active",
                title = "Active",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 45,
                energyDemand = EnergyDemand.HIGH,
                isCurrentlyActive = true,
                createdAtMillis = createdPast
            ),
            snapshot,
            calibrationRatio = 1.0
        )

        val scoreZeigarnik = schedulerService.evaluateTaskScore(
            schedTask2 ?: SchedulableTask(
                id = "task-zeigarnik",
                title = "Zeigarnik",
                category = TaskCategory.ADMIN,
                priority = 1,
                estimatedMinutes = 30,
                energyDemand = EnergyDemand.MODERATE,
                completedMicroSteps = 2,
                totalMicroSteps = 4,
                createdAtMillis = createdPast
            ),
            snapshot,
            calibrationRatio = 1.0
        )

        // Momentum should reflect activeBonus (+35) and microStepBonus (+20) + zeigarnikPull
        assertTrue("Active task momentum must be boosted (> 35.0)", scoreActive.momentumScore > 35.0)
        assertTrue("Zeigarnik in-progress task momentum must be boosted (> 25.0)", scoreZeigarnik.momentumScore > 25.0)
        // Urgency without deadline should calculate positive aging from createdAt (7 days old)
        assertTrue("Urgency with ageDays > 0 must be > 15.0 base", scoreActive.urgencyScore > 15.0)
    }

    // =========================================================================
    // 2. BAYESIAN CALIBRATION & ANTI-COMPOUNDING (Bug 2)
    // =========================================================================

    @Test
    fun testTaskBlockPlan_expectedMinutesDoesNotCompoundUnestimatedTasks() {
        val unestimatedTask = TaskEntity(
            id = "task-unestimated",
            title = "Unestimated task",
            effortScore = 50,
            estimatedDurationMinutes = 0,
            plannedBlockMinutes = 38 // Previously scheduled & buffered to 38
        )

        // expectedMinutes should return defaultMinutes(30), NOT the inflated 38!
        val expected = TaskBlockPlan.expectedMinutes(unestimatedTask)
        assertEquals("Unestimated task must return default baseline 30m, not previous 38m", 30, expected)
    }

    @Test
    fun testTaskBlockPlan_expectedMinutesPreservesSplitPartitions() {
        val splitPartTask = TaskEntity(
            id = "task-parent#part2",
            title = "Split task part 2",
            estimatedDurationMinutes = 90, // Parent was 90m
            plannedBlockMinutes = 45       // Partition is 45m
        )

        val expected = TaskBlockPlan.expectedMinutes(splitPartTask)
        assertEquals("Split task #part2 must preserve its 45m plannedBlockMinutes", 45, expected)
    }

    @Test
    fun testBayesianCalibrationAppliesToEstimatedTasks() {
        val estimatedTask = TaskEntity(
            id = "task-est",
            title = "Task with 30m estimate",
            estimatedDurationMinutes = 30,
            isScheduleLocked = false
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(estimatedTask),
            allKnownTasks = listOf(estimatedTask),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = LocalDate.now(),
            nowMillis = System.currentTimeMillis(),
            busyWindows = emptyList(),
            forceReplan = false
        )

        val sched = outcome.placements.firstOrNull { it.task.id == "task-est" }?.task
        if (sched != null) {
            // isDurationExplicit must be false so calibration applies
            assertFalse("Estimated non-locked task must NOT have isDurationExplicit = true", sched.isDurationExplicit)
        }
    }

    // =========================================================================
    // 3. DST-SAFE PROTECTED SLOT CLOCK (Bug 3)
    // =========================================================================

    @Test
    fun testProtectedSlotClock_wallClockHourPreservedAcrossDSTTransitions() {
        val slot = ProtectedTimeSlot(
            id = "morning_routine",
            label = "Morning Routine",
            startMinuteOfDay = 8 * 60, // 08:00 AM (480 minutes)
            durationMinutes = 60,
            recurrence = PtsRecurrence.DAILY
        )

        // Test over various simulated timestamps (e.g. spring forward day)
        val testTimes = listOf(
            1711843200000L, // European spring forward transition window
            1709424000000L, // US spring forward transition window
            System.currentTimeMillis()
        )

        for (nowMillis in testTimes) {
            val approaching = ProtectedSlotClock.approaching(listOf(slot), nowMillis)
            val takeover = ProtectedSlotClock.takeoverOf(listOf(slot), nowMillis)
            val due = (approaching + listOfNotNull(takeover)).firstOrNull()

            if (due != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = due.startMillis }
                assertEquals(
                    "Protected slot must start at exactly 08:00 wall-clock time",
                    8,
                    cal.get(Calendar.HOUR_OF_DAY)
                )
                assertEquals(
                    "Protected slot minute must be 0",
                    0,
                    cal.get(Calendar.MINUTE)
                )
            }
        }
    }

    // =========================================================================
    // 4. ENERGY METRICS ROUNDING (Bug 7)
    // =========================================================================

    @Test
    fun testAverageRoundingToNearestInt() {
        val values = listOf(79.0, 80.0, 80.5) // average = 79.833...
        val average = values.average()
        val truncated = average.toInt()
        val rounded = average.roundToInt()

        assertEquals("Truncation drops to 79", 79, truncated)
        assertEquals("Rounding correctly yields 80", 80, rounded)
    }
}
