package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class Adr024RobustnessTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: DefaultTaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    // --- Bug 1: SleepPressureRepository 7-Day Rolling Sleep Average on Early Usage Days ---

    @Test
    fun testSleepPressureRollingAverageOnEarlyInstallDays() {
        val now = LocalDate.of(2026, 10, 22).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wakeToday = LocalDate.of(2026, 10, 22).atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val bedYesterday = LocalDate.of(2026, 10, 21).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Day 1 after install: single 8-hour sleep log (480 minutes)
        val singleLog = listOf(
            SleepLogEntity(
                id = "1",
                startAt = bedYesterday,
                endAt = wakeToday,
                durationMinutes = 480
            )
        )

        val rollingAverage = computeRollingDailySleepAverage(singleLog, now, zoneId)
        // Must evaluate to 480.0 minutes (8.0h/day), NOT 480 / 7 = 68.57 min/day!
        assertEquals(480.0, rollingAverage, 0.5)

        val chronicMultiplier = FatigueZone.calculateChronicRestrictionMultiplier(rollingAverage.toFloat())
        // Rested user on Day 1 must have a multiplier of 1.0, NOT 1.49x
        assertEquals(1.0f, chronicMultiplier, 0.01f)
    }

    // --- Bug 2: computeDailySummary Wake Anchoring for Post-Midnight Sessions ---

    @Test
    fun testComputeDailySummaryPostMidnightSessionPreservesWakeTime() {
        // Target: 2026-10-22 at 00:45 AM (post-midnight session)
        val targetDate = LocalDate.of(2026, 10, 22)
        val timestampMillis = targetDate.atTime(0, 45).atZone(zoneId).toInstant().toEpochMilli()

        // User woke up yesterday morning at 08:00 AM (2026-10-21) and has been awake for 16.75 hours
        val yesterdayWakeMillis = LocalDate.of(2026, 10, 21).atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        val summary = circadianService.computeDailySummary(
            timestampMillis = timestampMillis,
            wakeTimeMillis = yesterdayWakeMillis,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )

        // Snapshot at 00:45 AM must reflect 16.75 hours of wakefulness, NOT 0 hours of wakefulness!
        // At 16.75h awake, Process S must be well above 0.50 and usable energy should be low (wind-down / recovery)
        assertTrue(
            "Homeostatic pressure S at 00:45 after waking yesterday morning must reflect wakefulness (> 0.50), was ${summary.currentSnapshot.homeostaticPressureS}",
            summary.currentSnapshot.homeostaticPressureS > 0.50
        )
        assertTrue(
            "Usable energy at 00:45 AM after 16.75h awake should be depleted (< 50), was ${summary.currentSnapshot.usableEnergy}",
            summary.currentSnapshot.usableEnergy < 50
        )
    }

    // --- Bug 3: extractStimulantDosesFromSlots Across Midnight Boundary ---

    @Test
    fun testExtractStimulantDosesFromSlotsCrossesMidnight() {
        val today = LocalDate.of(2026, 10, 22)

        // Slot configured for evening coffee at 21:00 (minute 1260)
        val eveningCoffeeSlot = ProtectedTimeSlot(
            id = "evening_coffee",
            label = "Evening Coffee",
            startMinuteOfDay = 1260,
            durationMinutes = 15,
            recurrence = PtsRecurrence.DAILY
        )

        val extractedDoses = extractStimulantDosesFromSlots(
            slots = listOf(eveningCoffeeSlot),
            date = today,
            zoneId = zoneId
        )

        // Both yesterday's 21:00 dose and today's 21:00 dose should be extracted
        assertEquals(2, extractedDoses.size)
        val yesterdayStart = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val yesterdayDoseMillis = yesterdayStart + 1260 * 60_000L

        assertTrue(
            "Yesterday's evening coffee dose must be preserved across midnight",
            extractedDoses.any { it.timestampMillis == yesterdayDoseMillis && it.type == StimulantType.CAFFEINE }
        )
    }

    // --- Bug 4: ConflictResolver TaskBlockPlan Durations & Forward Gap Scanning ---

    @Test
    fun testConflictResolverUsesPlannedMinutesAndScansForwardWithoutBackwardClamping() {
        val taskDate = LocalDate.of(2026, 10, 22).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Task with estimatedDurationMinutes = 20, but Bayesian calibrated plannedBlockMinutes = 60 => TaskBlockPlan.plannedMinutes evaluates to 60
        val highEffortTask = TaskEntity(
            id = "task_heavy",
            title = "Heavy Task",
            scheduledDate = taskDate,
            scheduledTime = 18 * 3600_000L, // 18:00 (1080 min)
            estimatedDurationMinutes = 20,
            plannedBlockMinutes = 60,
            effortScore = 85
        )
        assertEquals(60, TaskBlockPlan.plannedMinutes(highEffortTask))

        // Protected slot from 18:30 to 19:45 (1110 to 1185)
        val protectedSlot = ProtectedTimeSlot(
            id = "slot_dinner",
            label = "Dinner",
            startMinuteOfDay = 1110, // 18:30
            durationMinutes = 75,    // 19:45
            recoveryBufferMinutes = 0
        )

        // Analyze conflicts with workDayEnd = 20 (1200 min)
        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(highEffortTask),
            protectedSlots = listOf(protectedSlot),
            workDayEnd = 20
        )

        assertEquals(1, conflicts.size)
        val conflict = conflicts.first()

        // SEnd is 19:45 (1185). Task duration is 60m. 1185 + 60 = 1245 > 1200 (workDayEnd * 60).
        // It must NOT clamp backward to 1200 - 60 = 1140 (which was inside the dinner slot)!
        // Instead, suggestedNewStartMinute must be null!
        assertNull(
            "ConflictResolver must set suggestedNewStartMinute to null when no free gap remains today, rather than clamping backward into the conflict",
            conflict.suggestedNewStartMinute
        )
        assertTrue(
            "Suggestion should mention moving to tomorrow",
            conflict.actionableSuggestion.contains("tomorrow")
        )
    }

    // --- Bug 5: Hysteresis-Anchored & Locked Tasks Obey DAG Prerequisites ---

    @Test
    fun testHysteresisAndLockedTasksObeyDagPrerequisitesInStep3() {
        val dayDate = LocalDate.of(2026, 10, 22)
        val dayStart = dayDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = dayDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Task B has an existing scheduled start at 09:30 AM and is locked, but depends on Task A
        val taskBStart = dayDate.atTime(LocalTime.of(9, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Task B (Dependent)",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            energyDemand = EnergyDemand.HIGH,
            estimatedMinutes = 30,
            prerequisiteIds = setOf("task_a"),
            scheduledStartMillis = taskBStart,
            isLocked = true
        )

        // Task A is unscheduled
        val taskA = SchedulableTask(
            id = "task_a",
            title = "Task A (Prerequisite)",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            energyDemand = EnergyDemand.HIGH,
            estimatedMinutes = 30,
            scheduledStartMillis = null,
            isLocked = false
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskB, taskA),
            dayDate = dayDate,
            nowMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        val placementA = outcome.placements.firstOrNull { it.task.id == "task_a" }
        val placementB = outcome.placements.firstOrNull { it.task.id == "task_b" }

        assertNotNull("Task A must be placed", placementA)
        assertNotNull("Task B must be placed", placementB)

        // Task A must complete BEFORE Task B starts!
        assertTrue(
            "Task A (prereq) end (${placementA!!.window.endMillis}) must be <= Task B start (${placementB!!.window.startMillis})",
            placementA.window.endMillis <= placementB.window.startMillis
        )
    }

    // --- Bug 6 & Feature 4: Atomic Two-Phase Ultradian Split Verifier ---

    @Test
    fun testAtomicTwoPhaseUltradianSplitVerifierAndDeferralKeySanitization() {
        val dayDate = LocalDate.of(2026, 10, 22)
        val dayStart = dayDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = dayDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 90-minute task with high fragmentation tolerance
        val longTask = SchedulableTask(
            id = "long_task_1",
            title = "Long Architecture Session",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            energyDemand = EnergyDemand.HIGH,
            estimatedMinutes = 90,
            fragmentationTolerance = 0.8f
        )

        // Only a single 30-minute free window available: 10:00 to 10:30 (all other times busy)
        val busy1 = TimeSlotWindow(
            dayDate.atTime(6, 0).atZone(zoneId).toInstant().toEpochMilli(),
            dayDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy2 = TimeSlotWindow(
            dayDate.atTime(10, 30).atZone(zoneId).toInstant().toEpochMilli(),
            dayDate.atTime(22, 30).atZone(zoneId).toInstant().toEpochMilli()
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(longTask),
            dayDate = dayDate,
            nowMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busy1, busy2),
            zoneId = zoneId
        )

        // Phase 2 check fails because part2 cannot fit anywhere.
        // Therefore, neither part1 nor part2 should be committed to placements!
        assertTrue(
            "No partial split parts should be placed when part2 cannot fit",
            outcome.placements.none { it.task.id.startsWith("long_task_1") }
        )

        // Deferral rationale must be keyed by the base task ID "long_task_1", NOT "long_task_1#part1" or "#part2"
        assertEquals(1, outcome.deferrals.size)
        val deferral = outcome.deferrals.first()
        assertEquals("long_task_1", deferral.taskId)
        assertEquals(DeferralReasonCode.INSUFFICIENT_TIME, deferral.reasonCode)
    }

    // --- Bug 7: ChronobiologySignalNormalizer Excludes Prior-Day Afternoon Naps ---

    @Test
    fun testChronobiologySignalNormalizerExcludesPriorDayAfternoonNaps() {
        val baseWake = LocalDate.of(2026, 10, 22).atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Multi-day telemetry intervals:
        // 1. Yesterday afternoon nap: 2026-10-21 14:00 to 14:45 (duration 45m, start < baseWake - 16h)
        val yesterdayNap = TimeSlotWindow(
            LocalDate.of(2026, 10, 21).atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli(),
            LocalDate.of(2026, 10, 21).atTime(14, 45).atZone(zoneId).toInstant().toEpochMilli()
        )

        // 2. Nocturnal sleep: 2026-10-21 23:30 to 2026-10-22 07:30 (480 minutes)
        val nocturnalSleep = TimeSlotWindow(
            LocalDate.of(2026, 10, 21).atTime(23, 30).atZone(zoneId).toInstant().toEpochMilli(),
            LocalDate.of(2026, 10, 22).atTime(7, 30).atZone(zoneId).toInstant().toEpochMilli()
        )

        val telemetry = RawSleepTelemetry(
            intervals = listOf(yesterdayNap, nocturnalSleep),
            deepSleepMinutes = 90.0,
            remSleepMinutes = 110.0,
            confidence = 0.95
        )

        val normalizer = ChronobiologySignalNormalizer()
        val normalized = normalizer.normalize(
            telemetry = telemetry,
            baseWakeMillis = baseWake,
            baseSleepMinutes = 480.0
        )

        val rawSleepHours = normalized.totalSleepMinutes / 60.0
        // Nocturnal sleep duration should reflect 8.0 hours (480 minutes),
        // and must NOT sum yesterday's 45m afternoon nap into last night's nocturnal sleep!
        assertTrue(
            "Raw nocturnal sleep duration should be 8.0h (within 7.8h..8.2h), was $rawSleepHours",
            rawSleepHours in 7.8..8.2
        )
        // Yesterday's nap (before nocturnal window) should not be captured as a daytime nap of today
        assertTrue(
            "Yesterday's nap should not appear in today's daytime naps",
            normalized.daytimeNaps.isEmpty()
        )
    }

    // --- Feature 1: Bedtime SWS Suppression -> Process S0 Coupling ---

    @Test
    fun testBedtimeSwsSuppressionElevatesInitialWakePressureS0() {
        val wakeTime = LocalDate.of(2026, 10, 22).atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val plannedBedtime = wakeTime - (480 * 60_000L) // 23:00 yesterday

        // Baseline: 8h sleep with NO late-evening caffeine
        val baselineSnap = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = wakeTime + 30 * 60_000L, // 07:30 (after morning inertia)
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            stimulantDoses = emptyList()
        )

        // With late-evening caffeine: 200mg at 21:30 (90 min before bedtime)
        val lateCaffeine = listOf(
            StimulantDoseBout(
                timestampMillis = plannedBedtime - 90 * 60_000L,
                doseMg = 200.0,
                type = StimulantType.CAFFEINE
            )
        )

        val suppressedSnap = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = wakeTime + 30 * 60_000L,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            stimulantDoses = lateCaffeine
        )

        // SWS suppression from late caffeine must reduce restorative sleep efficacy and degrade baseline biological energy
        assertTrue(
            "Late caffeine SWS suppression must degrade morning baseline biological energy (${suppressedSnap.baselineBiologicalEnergy} vs ${baselineSnap.baselineBiologicalEnergy})",
            suppressedSnap.baselineBiologicalEnergy < baselineSnap.baselineBiologicalEnergy
        )

        // In the afternoon (14:00, 16.5h post-dose), acute caffeine masking has decayed, so accumulated Process S is strictly higher
        val afternoonTimestamp = wakeTime + 7 * 3600_000L // 14:00
        val afternoonBaseline = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = afternoonTimestamp,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            stimulantDoses = emptyList()
        )
        val afternoonSuppressed = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = afternoonTimestamp,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            stimulantDoses = lateCaffeine
        )

        assertTrue(
            "After acute caffeine clears in afternoon, homeostatic pressure S must be elevated by S0 debt (${afternoonSuppressed.homeostaticPressureS} vs ${afternoonBaseline.homeostaticPressureS})",
            afternoonSuppressed.homeostaticPressureS > afternoonBaseline.homeostaticPressureS
        )
    }

    // --- Feature 2: Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation ---

    @Test
    fun testReverseDagCeilingPropagationConstrainsPrerequisitesOfLockedTasks() {
        val dayDate = LocalDate.of(2026, 10, 22)
        val dayStart = dayDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = dayDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Task B is locked at 14:00 (14:00 - 15:00)
        val taskBLockedStart = dayDate.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val taskB = SchedulableTask(
            id = "locked_meeting",
            title = "Locked Client Meeting",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            energyDemand = EnergyDemand.HIGH,
            estimatedMinutes = 60,
            scheduledStartMillis = taskBLockedStart,
            isLocked = true,
            prerequisiteIds = setOf("prep_work")
        )

        // Task A is a prerequisite of Task B (estimated 45m, unconstrained deadline)
        val taskA = SchedulableTask(
            id = "prep_work",
            title = "Prepare Meeting Slides",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            energyDemand = EnergyDemand.HIGH,
            estimatedMinutes = 45
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskA, taskB),
            dayDate = dayDate,
            nowMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        val placementA = outcome.placements.firstOrNull { it.task.id == "prep_work" }
        assertNotNull("Prerequisite Task A must be placed", placementA)

        // Reverse-DAG ceiling propagation must constrain Task A to complete on or before 13:55 (14:00 - 5m transition buffer)
        val maxAllowedEnd = taskBLockedStart - 5 * 60_000L
        assertTrue(
            "Prerequisite Task A end (${placementA!!.window.endMillis}) must be <= locked dependent ceiling ($maxAllowedEnd)",
            placementA.window.endMillis <= maxAllowedEnd
        )
    }
}
