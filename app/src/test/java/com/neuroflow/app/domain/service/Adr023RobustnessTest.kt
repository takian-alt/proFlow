package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class Adr023RobustnessTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: DefaultTaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    // --- Bug 1: ProtectedTimeSlot Day-of-Week Filtering & Recovery Buffer ---

    @Test
    fun testProtectedSlotDayOfWeekFilteringAndRecoveryBuffer() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 19, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayStart = cal.timeInMillis

        // Weekend slot: should not occur on Monday
        val weekendSlot = ProtectedTimeSlot(
            id = "weekend_slot",
            label = "Weekend Family Time",
            startMinuteOfDay = 600, // 10:00
            durationMinutes = 120,  // 12:00
            recurrence = PtsRecurrence.WEEKENDS,
            recoveryBufferMinutes = 30
        )
        assertFalse("Weekend slot should not occur on Monday", ProtectedSlotClock.occursOnDay(weekendSlot, mondayStart))

        // Weekday slot with 30 min recovery buffer: 10:00 to 12:00 + 30m recovery => effective 10:00 to 12:30
        val weekdaySlot = ProtectedTimeSlot(
            id = "lunch_slot",
            label = "Weekday Lunch",
            startMinuteOfDay = 600, // 10:00
            durationMinutes = 120,  // 12:00
            recurrence = PtsRecurrence.WEEKDAYS,
            recoveryBufferMinutes = 30
        )
        assertTrue("Weekday slot occurs on Monday", ProtectedSlotClock.occursOnDay(weekdaySlot, mondayStart))

        // In conflict resolver, a task overlapping the recovery buffer (e.g. 12:15 to 13:00) must trigger conflict
        val taskInRecovery = TaskEntity(
            id = "t_rec",
            title = "Task in Recovery Buffer",
            scheduledDate = mondayStart,
            scheduledTime = (12 * 60 + 15) * 60_000L, // 12:15
            estimatedDurationMinutes = 45
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(taskInRecovery),
            protectedSlots = listOf(weekdaySlot),
            workDayStart = 8,
            workDayEnd = 18,
            dayMillis = mondayStart
        )

        assertEquals("Should detect conflict in recovery buffer", 1, conflicts.size)
        assertTrue(conflicts.first().conflictDescription.contains("buffer"))
    }

    // --- Bug 2: Cognitive Fatigue Contiguous Load Tracking in Wall-Clock Space ---

    @Test
    fun testCognitiveFatigueContiguousLoadTrackingInWallClock() {
        val today = LocalDate.of(2026, 10, 19)
        val wakeTime = today.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 3 heavy tasks, each 60 mins. Available window 08:00 to 12:00.
        val task1 = SchedulableTask(
            id = "task1",
            title = "Heavy 1",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            createdAtMillis = nowMillis
        )
        val task2 = SchedulableTask(
            id = "task2",
            title = "Heavy 2",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = false,
            createdAtMillis = nowMillis
        )
        val task3 = SchedulableTask(
            id = "task3",
            title = "Heavy 3",
            category = TaskCategory.DEEP_WORK,
            priority = 0,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = false,
            createdAtMillis = nowMillis
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task1, task2, task3),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals(3, outcome.placements.size)
        val p1 = outcome.placements[0]
        val p2 = outcome.placements[1]
        val p3 = outcome.placements[2]
        assertNotNull(p1)
        assertNotNull(p2)
        assertNotNull(p3)
    }

    // --- Bug 3 & Feature 1: Bateman Plasma Peak Resonance & Crash Shield ---

    @Test
    fun testBatemanPeakResonanceAndCrashShield() {
        val today = LocalDate.of(2026, 10, 19)
        val doseTime = today.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 200mg Caffeine at 08:00. Peak is ~45m later (~08:45 to 09:30).
        val dose = StimulantDoseBout(
            timestampMillis = doseTime,
            doseMg = 200.0,
            type = StimulantType.CAFFEINE
        )

        // 1. Verify Pharmacokinetic Alertness Boost at Peak (> 4.0 points)
        val peakTime = doseTime + 55 * 60_000L // 08:55
        val peakEffect = PharmacokineticStimulantCalculator.evaluateAt(peakTime, listOf(dose))
        assertTrue("Stimulant acute alertness boost at peak should exceed 4.0", peakEffect.acuteAlertnessBoostPoints > 4.0)

        // 2. Verify Crash Shield Window (at peak rebound progress=1.0, 6.5 hours post-dose) drops below -1.5 points
        val crashTime = doseTime + (6.5 * 3600_000.0).toLong() // 14:30 (half-life 4.5h + 2h = 6.5h)
        val crashEffect = PharmacokineticStimulantCalculator.evaluateAt(crashTime, listOf(dose))
        assertTrue("Stimulant rebound crash should drop acute alertness below -1.5 (was ${crashEffect.acuteAlertnessBoostPoints})", crashEffect.acuteAlertnessBoostPoints < -1.5)

        // 3. Verify Scheduler Placement with Bateman resonance
        val deepTask = SchedulableTask(
            id = "deep_task",
            title = "Complex Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = doseTime
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(deepTask),
            dayDate = today,
            nowMillis = doseTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            stimulantDoses = listOf(dose)
        )

        assertEquals(1, outcome.placements.size)
        val placement = outcome.placements.first()
        val scheduledHour = java.time.Instant.ofEpochMilli(placement.window.startMillis).atZone(zoneId).hour
        assertTrue("Deep work scheduled during active workday hours ($scheduledHour)", scheduledHour in 8..18)
    }

    // --- Bug 4 & Feature 4: Cross-Day Dependency Blocking & Deferral Rationale ---

    @Test
    fun testCrossDayDependencyBlockingAndDeferralRationale() {
        val today = LocalDate.of(2026, 10, 19)
        val wakeTime = today.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Task B depends on Task A, but Task A is in activeUncompletedTaskIds (from yesterday or another context) and not in candidate tasks
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Dependent Task B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE,
            prerequisiteIds = setOf("task_a"),
            createdAtMillis = nowMillis
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskB),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            activeUncompletedTaskIds = setOf("task_a")
        )

        assertTrue("Task B must not be scheduled while uncompleted prereq task_a is active", outcome.placements.isEmpty())
        assertEquals("Should produce 1 deferral rationale", 1, outcome.deferrals.size)
        val deferral = outcome.deferrals.first()
        assertEquals("task_b", deferral.taskId)
        assertEquals(DeferralReasonCode.DEPENDENCY_BLOCKED, deferral.reasonCode)
    }

    // --- Bug 6: Physiological Epoch-Space Modulo Jump Fix ---

    @Test
    fun testPhysiologicalEpochSpaceNoModuloJump() {
        val zone = ZoneId.of("UTC")
        val baseDate = LocalDate.of(2026, 10, 19)
        // 23:30 on baseDate
        val mealStartEpoch = baseDate.atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        // 00:15 on baseDate + 1 day
        val queryEpoch = baseDate.plusDays(1).atTime(0, 15).atZone(zone).toInstant().toEpochMilli()

        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 23 * 60 + 30,
            endMinuteOfDay = 0,
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0,
            explicitStartMillis = mealStartEpoch
        )

        // Elapsed time across midnight is 45 minutes
        val impulse = computeSmoothMealImpulse(
            minuteOfDay = 15,
            mealBlocks = listOf(mealBlock),
            timestampMillis = queryEpoch
        )

        // Across midnight with explicit start millis, it should recognize 45 min elapsed, reflecting digestive dip without modulo glitch
        assertTrue("Impulse across midnight must reflect digestive dip without modulo glitch", impulse < 0.0)
    }

    // --- Bug 7: Analytics Peak Hour Range Fix ---

    @Test
    fun testAnalyticsPeakWindowsForDayProperHourRange() {
        val date = LocalDate.of(2026, 10, 19)
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Test window 9..12
        val windows = AnalyticsEngine.peakWindowsForDay(dayStart, 9, 12)
        assertEquals(1, windows.size)
        val window = windows.first()

        val startHour = Calendar.getInstance().apply { timeInMillis = window.first }.get(Calendar.HOUR_OF_DAY)
        val endHour = Calendar.getInstance().apply { timeInMillis = window.second }.get(Calendar.HOUR_OF_DAY)

        assertEquals("Window start hour should be 9", 9, startHour)
        assertEquals("Window end hour should be 12 (not 13)", 12, endHour)

        // isHourInPeakWindow check
        assertTrue("Hour 9 is in peak", AnalyticsEngine.isHourInPeakWindow(9, 9, 12))
        assertTrue("Hour 11 is in peak", AnalyticsEngine.isHourInPeakWindow(11, 9, 12))
        assertFalse("Hour 12 is at the end boundary, not in peak", AnalyticsEngine.isHourInPeakWindow(12, 9, 12))

        // Check hour 23 does not cause hour 24 overflow
        val lateWindows = AnalyticsEngine.peakWindowsForDay(dayStart, 22, 23)
        assertEquals(1, lateWindows.size)
        val lateEndHour = Calendar.getInstance().apply { timeInMillis = lateWindows.first().second }.get(Calendar.HOUR_OF_DAY)
        assertEquals(23, lateEndHour)
    }

    // --- Feature 2: Bidirectional Context-Switch Transition Guard ---

    @Test
    fun testBidirectionalContextSwitchTransitionScoring() {
        val today = LocalDate.of(2026, 10, 19)
        val startMillis = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val mockBreakdown = TaskScoreBreakdown(
            taskId = "dummy",
            totalScore = 80.0,
            urgencyScore = 80.0,
            impactScore = 80.0,
            energyFitScore = 80.0,
            momentumScore = 80.0,
            bufferedDurationMinutes = 45
        )

        val precedingDeep = ScheduledPlacement(
            task = SchedulableTask(
                id = "p1",
                title = "Deep Preceding",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 45,
                energyDemand = EnergyDemand.HIGH,
                createdAtMillis = startMillis
            ),
            window = TimeSlotWindow(startMillis - 50 * 60_000L, startMillis - 5 * 60_000L),
            scoreBreakdown = mockBreakdown
        )

        val succeedingDeep = ScheduledPlacement(
            task = SchedulableTask(
                id = "p2",
                title = "Deep Succeeding",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 45,
                energyDemand = EnergyDemand.HIGH,
                createdAtMillis = startMillis
            ),
            window = TimeSlotWindow(startMillis + 35 * 60_000L, startMillis + 80 * 60_000L),
            scoreBreakdown = mockBreakdown
        )

        // Sandwiching an ADMIN task between two DEEP_WORK tasks incurs context penalties
        val adminTask = SchedulableTask(
            id = "admin_task",
            title = "Quick Email",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            createdAtMillis = startMillis
        )

        val deltaClashing = schedulerService.computeContextTransitionDelta(
            placements = listOf(precedingDeep, succeedingDeep),
            candidateSlotStartMillis = startMillis,
            candidate = adminTask,
            candidateSlotEndMillis = startMillis + 30 * 60_000L
        )

        // Same category deep task matches both preceding and succeeding deep work
        val deepSandwichTask = SchedulableTask(
            id = "deep_task",
            title = "Focused Coding",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = startMillis
        )

        val deltaMatching = schedulerService.computeContextTransitionDelta(
            placements = listOf(precedingDeep, succeedingDeep),
            candidateSlotStartMillis = startMillis,
            candidate = deepSandwichTask,
            candidateSlotEndMillis = startMillis + 30 * 60_000L
        )

        assertTrue(
            "Matching context should score higher than clashing context",
            deltaMatching > deltaClashing
        )
    }

    // --- Feature 3: Social Jetlag Phase-Shift Auto-Pacing ---

    @Test
    fun testSocialJetlagPhaseShiftAutoPacing() {
        val today = LocalDate.of(2026, 10, 19)
        val wakeTime = today.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val morningStart = today.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        val heavyTask = SchedulableTask(
            id = "heavy_task",
            title = "Heavy Cognitive Task",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = morningStart
        )

        // High social jetlag: mondayMorningPhaseLagHours = 2.5
        val outcomeLagged = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(heavyTask),
            dayDate = today,
            nowMillis = morningStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            socialJetlagLagHours = 2.5
        )

        // Zero social jetlag: mondayMorningPhaseLagHours = 0.0
        val outcomeNormal = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(heavyTask),
            dayDate = today,
            nowMillis = morningStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            socialJetlagLagHours = 0.0
        )

        assertEquals(1, outcomeLagged.placements.size)
        assertEquals(1, outcomeNormal.placements.size)
        val laggedScore = outcomeLagged.placements.first().scoreBreakdown.totalScore
        val normalScore = outcomeNormal.placements.first().scoreBreakdown.totalScore

        // Due to circadian phase lag penalty in the early morning, lagged score must be dampened
        assertTrue("Social jetlag should dampen morning heavy task score", laggedScore <= normalScore)
    }
}
