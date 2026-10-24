package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.model.buildEnergyUiModel
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.scheduler.TaskCategory
import com.neuroflow.app.domain.scheduler.determineCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class Adr026RobustnessTest {

    private lateinit var normalizer: ChronobiologySignalNormalizer
    private lateinit var circadianService: DefaultCircadianEnergyService
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        normalizer = ChronobiologySignalNormalizer()
        circadianService = DefaultCircadianEnergyService()
    }

    // --- Bug 1: Unstaged & Manual Sleep Log Stage Efficiency Weighting ---

    @Test
    fun testComputeEffectiveDurationDoesNotPenalizeUnstagedSleepLogs() {
        val rawMinutes = 480.0 // 8 hours

        // Unstaged / manual log: deepSleep = 0, remSleep = 0, lightSleep = 0
        val effectiveUnstaged = normalizer.computeEffectiveDuration(
            totalMinutes = rawMinutes,
            deepSleepMinutes = 0.0,
            remSleepMinutes = 0.0,
            lightSleepMinutes = 0.0
        )

        // Must NOT apply 0.85x penalty (408.0m); should assume normative 1.0x (480.0m)
        assertEquals("Unstaged sleep logs must not be penalized by 15%", 480.0, effectiveUnstaged, 0.001)

        // Staged log: with 25% SWS+REM restorative sleep (120m out of 480m)
        // restorativeRatio = 0.25, stageWeight = 0.85 + 0.75 * 0.25 = 1.0375
        val effectiveStaged = normalizer.computeEffectiveDuration(
            totalMinutes = rawMinutes,
            deepSleepMinutes = 60.0,
            remSleepMinutes = 60.0,
            lightSleepMinutes = 360.0
        )
        assertEquals(480.0 * 1.0375, effectiveStaged, 0.001)
    }

    // --- Bug 2 & Feature 3: Expired-Block Auto-Rescue in PlacementStability ---

    @Test
    fun testPlacementStabilityRescuesExpiredPastTasksInsteadOfFreezing() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val hours = 3_600_000L

        val prefs = UserPreferences(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

        // Task scheduled 3 hours ago (11:00 AM) with 60 min duration.
        // Ended at 12:00 PM; now is 14:00 (2 hours expired > 15m grace period)
        val expiredPastTask = TaskEntity(
            id = "expired_past",
            title = "Missed Morning Task",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            effortScore = 60,
            estimatedDurationMinutes = 60,
            scheduledDate = now - 3 * hours,
            scheduledTime = 0L,
            isAutoScheduled = true,
            updatedAt = now - 24 * hours
        )

        val verdict = PlacementStability.verdict(expiredPastTask, now, prefs, forceReplan = false)
        assertEquals(
            "Expired past task must receive PAST_EXPIRED_RESCUE",
            PlacementStability.ReplanVerdict.PAST_EXPIRED_RESCUE,
            verdict
        )
        assertTrue("PAST_EXPIRED_RESCUE must allow replanning", verdict.allowsReplan)

        // In contrast, an upcoming task starting 1 hour from now must be NEAR_TERM_FREEZE
        val upcomingTask = TaskEntity(
            id = "upcoming",
            title = "Imminent Task",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            effortScore = 60,
            estimatedDurationMinutes = 60,
            scheduledDate = now + 1 * hours,
            scheduledTime = 0L,
            isAutoScheduled = true,
            updatedAt = now - 24 * hours
        )
        val upcomingVerdict = PlacementStability.verdict(upcomingTask, now, prefs, forceReplan = false)
        assertEquals(
            "Upcoming task within 2 hours must receive NEAR_TERM_FREEZE",
            PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE,
            upcomingVerdict
        )
        assertFalse("NEAR_TERM_FREEZE must disallow replanning", upcomingVerdict.allowsReplan)
    }

    // --- Bug 3: Unestimated Tasks Preserve Expected Minutes in TaskBlockPlan ---

    @Test
    fun testUnestimatedTasksFallBackToTaskBlockPlanExpectedMinutes() {
        val highEffortTask = TaskEntity(
            id = "unestimated_high",
            title = "Complex Architecture",
            effortScore = 85,
            estimatedDurationMinutes = 0 // unestimated
        )
        val medEffortTask = TaskEntity(
            id = "unestimated_med",
            title = "Code Review",
            effortScore = 65,
            estimatedDurationMinutes = 0
        )
        val lowEffortTask = TaskEntity(
            id = "unestimated_low",
            title = "Quick Note",
            effortScore = 30,
            estimatedDurationMinutes = 0
        )

        assertEquals(60, TaskBlockPlan.expectedMinutes(highEffortTask))
        assertEquals(45, TaskBlockPlan.expectedMinutes(medEffortTask))
        assertEquals(30, TaskBlockPlan.expectedMinutes(lowEffortTask))
    }

    // --- Bug 4: TaskType.MINDFULNESS and TaskType.GENERAL Category Mapping ---

    @Test
    fun testTaskTypeCategoryMappingPreservesMindfulnessAndGeneral() {
        val mindfulnessTask = TaskEntity(
            id = "mindfulness_task",
            title = "Afternoon Breathing",
            taskType = TaskType.MINDFULNESS,
            tags = ""
        )
        val generalTask = TaskEntity(
            id = "general_task",
            title = "Call Landlord",
            taskType = TaskType.GENERAL,
            tags = ""
        )

        assertEquals(
            "TaskType.MINDFULNESS must map to TaskCategory.MINDFULNESS",
            TaskCategory.MINDFULNESS,
            mindfulnessTask.determineCategory()
        )
        assertEquals(
            "TaskType.GENERAL must map to TaskCategory.FLEXIBLE",
            TaskCategory.FLEXIBLE,
            generalTask.determineCategory()
        )
    }

    // --- Bug 5 & Feature 1: Morning Ramp-Up Classification & Sliding-Window Daily Summary ---

    @Test
    fun testMorningRampUpDoesNotClassifyAsWindDown() {
        val today = LocalDate.of(2026, 10, 22)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morningTime = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli() // 1 hour awake

        val snapshot = circadianService.evaluateSnapshot(
            timestampMillis = morningTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 450,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )

        // At 8:00 AM (usableEnergy ~ 60-70), must be REBOUND or PEAK, NEVER WIND_DOWN
        assertNotEquals("8:00 AM morning slot must not be WIND_DOWN", EnergyPhase.WIND_DOWN, snapshot.phase)
        assertTrue(
            "Morning ramp-up phase must be REBOUND or PEAK (was ${snapshot.phase})",
            snapshot.phase == EnergyPhase.REBOUND || snapshot.phase == EnergyPhase.PEAK
        )
    }

    @Test
    fun testComputeDailySummaryUsesSlidingWindowIntegral() {
        val today = LocalDate.of(2026, 10, 22)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val evalTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val summary = circadianService.computeDailySummary(
            timestampMillis = evalTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // Peak 3-hour window
        assertTrue("Peak window start must be in morning [7..12], was ${summary.peakWindowStartHour}", summary.peakWindowStartHour in 7..12)
        assertEquals((summary.peakWindowStartHour + 3) % 24, summary.peakWindowEndHour)
        assertTrue("Peak energy score must be >= 70, was ${summary.peakEnergyScore}", summary.peakEnergyScore >= 70)

        // Dip 2-hour window
        assertTrue("Dip window start must be in afternoon [11..18], was ${summary.dipWindowStartHour}", summary.dipWindowStartHour in 11..18)
        assertEquals((summary.dipWindowStartHour + 2) % 24, summary.dipWindowEndHour)
    }

    // --- Bug 6 & Feature 2: C1-Continuous Exercise Autonomic Transition Curve ---

    @Test
    fun testComputeExerciseImpulseEliminatesCliffDiscontinuityAtWorkoutEnd() {
        val workout = CognitiveBlockWindow(
            startMinuteOfDay = 600, // 10:00
            endMinuteOfDay = 660,   // 11:00
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0
        )

        // 1. In-workout at 10:30 (minute 630): -8.0
        val duringImpulse = PhysiologicalImpulseCalculator.computeExerciseImpulse(630, listOf(workout))
        assertEquals(-8.0, duringImpulse, 1e-4)

        // 2. Exactly at workout end at 11:00 (minute 660): deltaMinutes = 0
        // Autonomic transition ramp starts at exactly -8.0 (zero jump cliff!)
        val atEndImpulse = PhysiologicalImpulseCalculator.computeExerciseImpulse(660, listOf(workout))
        assertEquals("At delta = 0m, impulse must match -8.0 exertion penalty", -8.0, atEndImpulse, 1e-4)

        // 3. 1 minute after workout end (minute 661): smoothly recovering from -8.0
        val rampImpulse1m = PhysiologicalImpulseCalculator.computeExerciseImpulse(661, listOf(workout))
        assertTrue(
            "At 1m post-workout, impulse should be recovering smoothly (was $rampImpulse1m)",
            rampImpulse1m > -8.0 && rampImpulse1m < 0.0
        )

        // 4. 15 minutes after workout end (minute 675): fully transitioned into BDNF boost
        // 12.0 * exp(-15 / 45.0) = 8.598
        val peakBoost15m = PhysiologicalImpulseCalculator.computeExerciseImpulse(675, listOf(workout))
        assertEquals("At 15m post-workout, impulse reaches full decayed boost (~8.6)", 8.598, peakBoost15m, 0.05)
    }

    // --- Bug 7 & Feature 4: Telemetry Unification in EnergyUiModel ---

    @Test
    fun testBuildEnergyUiModelUnifiesBaselineMomentAdjustmentAndFatigueZone() {
        val model = buildEnergyUiModel(
            usableEnergy = 78,
            circadianFactor = 0.82f,
            sleepPressureFactor = 0.20f,
            sleepPressurePoints = 0,
            fatigueZone = null,
            refreshedAtMillis = 1000L,
            baselineBiologicalEnergy = 72,
            physiologicalImpulse = 4.0,
            learnedAdjustmentPoints = 2.0,
            predictionConfidence = 0.95
        )

        // 1. Baseline and moment adjustment telemetry wired from snapshot
        assertEquals(78, model.availableEnergy)
        assertEquals(72f, model.baselineRawEnergy, 0.01f)
        assertEquals(6.0f, model.momentAdjustment, 0.01f)
        assertEquals(0.95f, model.momentConfidence, 0.01f)

        // 2. FatigueZone unified with sleepPressureFactor (0.20 => 20% => RESTED)
        assertEquals(20, model.fatiguePercent)
        assertEquals(
            "FatigueZone must be unified with sleepPressureFactor (RESTED)",
            FatigueZone.RESTED,
            model.fatigueZone
        )
    }

    @Test
    fun testComputeEnergyUiStateUnfreezesFatigueZoneWhenNapsOrStimulantsPresent() {
        val prefs = UserPreferences(
            wakeUpHour = 7,
            sleepPressurePoints = 1400 // High fatigue points counter
        )
        val now = 14 * 3600_000L
        val (_, energyUi) = com.neuroflow.app.domain.model.computeEnergyUiState(
            circadianEnergyService = circadianService,
            sleepTelemetryRepository = null,
            prefs = prefs,
            sleepLogs = emptyList(),
            nowMillis = now,
            stimulantDoses = listOf(
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = now - 30 * 60_000L,
                    doseMg = 100.0,
                    type = com.neuroflow.app.domain.service.StimulantType.CAFFEINE
                )
            )
        )

        // With caffeine active, fatigueZone must reflect adjusted homeostatic pressure (RESTED/MODERATE),
        // not remaining stuck on prefs.sleepPressurePoints = 1400 (which would be HIGH)
        assertNotEquals(FatigueZone.HIGH, energyUi.fatigueZone)
    }
}
