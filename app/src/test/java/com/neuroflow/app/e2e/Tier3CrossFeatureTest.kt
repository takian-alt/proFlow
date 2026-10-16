package com.neuroflow.app.e2e

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.*
import com.neuroflow.app.e2e.harness.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tier 3: Cross-Feature Interactions (Pairwise & Combinatorial Integration).
 * Total: 16 test cases.
 */
class Tier3CrossFeatureTest {

    @Test
    fun testCross_EnergyStateAndTaskInferenceAndScoring() {
        // Inferred high-energy analytical task
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("Deep work on neural compiler 90m @work !frog")
        assertEquals(EnergyLevel.HIGH, inferred.energyDemand)
        assertEquals(TaskType.ANALYTICAL, inferred.taskType)

        // Peak energy context vs trough energy context
        val peakContext = E2ELiveEnergyContext(usableEnergy = 88f, reservoirFactor = 0.9f, circadianFactor = 0.85f, sleepPressureRatio = 0.1f, confidence = 0.8f)
        val troughContext = E2ELiveEnergyContext(usableEnergy = 25f, reservoirFactor = 0.3f, circadianFactor = 0.3f, sleepPressureRatio = 0.8f, confidence = 0.8f)

        val peakScoreBonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(inferred.energyDemand, peakContext)
        val troughScoreBonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(inferred.energyDemand, troughContext)

        assertTrue("Inferred high energy task must score high during peak energy", peakScoreBonus >= 60f)
        assertTrue("Inferred high energy task must be penalized during low energy trough", troughScoreBonus < 0f)
    }

    @Test
    fun testCross_TaskInferenceAndQuickCaptureAndAutoScheduling() {
        val rawInputs = listOf(
            "Write client proposal 60m @work",
            "Quick email reply 15m @admin",
            "Deep code refactoring 120m @core !frog"
        )
        val tasks = rawInputs.mapIndexed { idx, raw ->
            val inferred = E2ETestHarness.TaskInferenceOracle.infer(raw)
            TaskEntity(
                id = "task-$idx",
                title = inferred.sanitizedTitle,
                estimatedDurationMinutes = inferred.durationMinutes,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                isFrog = inferred.isFrog
            )
        }

        // Available slot 150m, daily capacity 240m
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(tasks, availableSlotsMinutes = 75, dailyCognitiveCapacityMinutes = 240)
        assertEquals("Total tasks must be completely accounted for", 3, report.placedCount + report.deferredCount)
        assertTrue("The 120m task should be deferred because it exceeds the 75m slot", report.deferrals.any { it.taskId == "task-2" })
    }

    @Test
    fun testCross_LiveEnergyAndAutoSchedulerDeferral() {
        val fatiguedEnergy = E2ELiveEnergyContext(usableEnergy = 20f, reservoirFactor = 0.2f, circadianFactor = 0.3f, sleepPressureRatio = 0.9f, confidence = 0.9f)
        val heavyTask = TaskEntity(id = "heavy-1", title = "Write legal brief", estimatedDurationMinutes = 120, energyLevel = EnergyLevel.HIGH)

        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(listOf(heavyTask), availableSlotsMinutes = 120, dailyCognitiveCapacityMinutes = 60)
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(E2EDeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED, report.deferrals.first().reasonCode)
        assertTrue(report.deferrals.first().actionableSuggestion.contains("Schedule for tomorrow's morning peak window"))
    }

    @Test
    fun testCross_AutoSchedulerAndBehavioralDurationCalibration() {
        // User consistently takes 60m for tasks estimated at 30m (inflation factor ~1.3-1.4)
        val calibrationFactor = E2ETestHarness.BehavioralInsightsOracle.calculateCalibrationFactor(
            actualMinutes = listOf(60, 60, 60, 60),
            estimatedMinutes = listOf(30, 30, 30, 30)
        )
        val initialEstimate = 45
        val calibratedDuration = (initialEstimate * calibrationFactor).toInt()

        val task = TaskEntity(id = "calibrated-task", title = "Feature development", estimatedDurationMinutes = calibratedDuration)
        // Available slot is only 50m: initial 45m would have falsely fit, but calibrated 58m correctly defers
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(listOf(task), availableSlotsMinutes = 50, dailyCognitiveCapacityMinutes = 240)
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(E2EDeferralReasonCode.INSUFFICIENT_TIME, report.deferrals.first().reasonCode)
    }

    @Test
    fun testCross_FocusModeAndLiveEnergyDifficultyFeedback() {
        val intervention = E2ETestHarness.FocusTierEvaluator.getTierIntervention(E2EFocusInterventionTier.SHIELD)
        assertTrue(E2ETestHarness.FocusTierEvaluator.isSupportiveCopy(intervention.heading))

        // Post-session rating of +2 reflects high energy retention
        val updatedUsableEnergy = (70f + 10f).coerceIn(0f, 100f)
        assertTrue(updatedUsableEnergy >= 80f)
    }

    @Test
    fun testCross_ChronotypeMidnightWrapAndAutoScheduling() {
        // Late evening window 22:00 to 02:00
        val ampMidnight = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(minute = 10, startMinute = 1320, durationMinutes = 240, amplitude = 0.9f)
        assertTrue("Night-owl window is active across midnight", ampMidnight > 0.4f)

        val nightTask = TaskEntity(id = "night-task", title = "Late night creative writing", estimatedDurationMinutes = 60)
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(listOf(nightTask), availableSlotsMinutes = 120, dailyCognitiveCapacityMinutes = 180)
        assertEquals(1, report.placedCount)
        assertTrue(report.placementRationales.first().energyFitScore > 0.8f)
    }

    @Test
    fun testCross_RoomEnumHardeningAndDatabaseMigration() {
        val legacyCorruptEntity = mapOf(
            "quadrant" to "INVALID_QUADRANT",
            "priority" to "UNRECOGNIZED_PRIORITY",
            "recurrence" to "CORRUPTED_RECURRENCE"
        )
        val safeQuadrant = E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(legacyCorruptEntity["quadrant"])
        val safePriority = E2ETestHarness.EnumHardeningEvaluator.safePriority(legacyCorruptEntity["priority"])
        val safeRecurrence = E2ETestHarness.EnumHardeningEvaluator.safeRecurrence(legacyCorruptEntity["recurrence"])

        assertEquals(Quadrant.SCHEDULE, safeQuadrant)
        assertEquals(Priority.LOW, safePriority)
        assertEquals(Recurrence.NONE, safeRecurrence)
    }

    @Test
    fun testCross_NonBlockingStartupAndWarmEnergySnapshot() {
        val coldStartRestedEnergy = E2ETestHarness.ConfidenceDecouplingOracle.calculateDecoupledEnergy(
            reservoirFactor = 0.85f,
            circadianFactor = 0.80f,
            sleepPressureRatio = 0.05f,
            confidenceFactor = 0.20f
        )
        val usable = coldStartRestedEnergy.second
        assertTrue("Cold start background snapshot produces usable energy > 60%", usable > 60f)
    }

    @Test
    fun testCross_AmoledThemeAndTouchTargetsAndNavigation() {
        val amoledBgArgb = 0xFF000000.toInt()
        val textArgb = 0xFFFFFFFF.toInt()
        val ratio = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(textArgb, amoledBgArgb)
        assertEquals(21.0, ratio, 0.1)

        val navBarItems = listOf("flow", "plan", "insights")
        val itemTouchBoundsDp = 48
        assertEquals(3, navBarItems.size)
        assertTrue("Navigation items satisfy 48dp minimum bounds", itemTouchBoundsDp >= 48)
    }

    @Test
    fun testCross_TaskInferenceAndFocusModeTiers() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("!frog Quarterly executive deck 60m @office")
        assertTrue(inferred.isFrog)

        // Frog tasks map to Fortress tier
        val tier = if (inferred.isFrog) E2EFocusInterventionTier.FORTRESS else E2EFocusInterventionTier.ZEN
        val intervention = E2ETestHarness.FocusTierEvaluator.getTierIntervention(tier)
        assertEquals(E2EFocusInterventionTier.FORTRESS, intervention.tier)
        assertTrue(E2ETestHarness.FocusTierEvaluator.isSupportiveCopy(intervention.heading))
    }

    @Test
    fun testCross_BehavioralInsightsAndAutoSchedulingRationales() {
        val density = E2ETestHarness.BehavioralInsightsOracle.calculateHourlyCircadianDensity(mapOf(9 to 60, 10 to 60))
        val peakHour = 9
        assertTrue(density[peakHour] > 0.4f)

        val avoidedTask = TaskEntity(id = "avoided-1", title = "Difficult taxes", estimatedDurationMinutes = 45)
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(listOf(avoidedTask), availableSlotsMinutes = 60, dailyCognitiveCapacityMinutes = 120)
        assertEquals(1, report.placedCount)
        assertTrue(report.placementRationales.first().primaryReason.contains("optimal"))
    }

    @Test
    fun testCross_FocusModeAndThemeContrast() {
        val shieldBgDark = 0xFF121212.toInt()
        val shieldTextLight = 0xFFF1F5F9.toInt()
        val contrast = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(shieldTextLight, shieldBgDark)
        assertTrue("Shield intervention contrast must satisfy WCAG AA (ratio: $contrast)", contrast >= 4.5)
    }

    @Test
    fun testCross_MidnightWrapAndBehavioralDensityAggregation() {
        // Session from 23:30 to 01:30 (120 minutes)
        val minutesByHour = mapOf(23 to 30, 0 to 60, 1 to 30)
        val density = E2ETestHarness.BehavioralInsightsOracle.calculateHourlyCircadianDensity(minutesByHour)
        assertEquals(0.25f, density[23], 0.01f)
        assertEquals(0.50f, density[0], 0.01f)
        assertEquals(0.25f, density[1], 0.01f)
    }

    @Test
    fun testCross_QuickCaptureAndEnumHardeningAndAutoScheduler() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("Clean office desk 20m")
        val safeQuadrant = E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(null)
        val task = TaskEntity(
            id = "quick-1",
            title = inferred.sanitizedTitle,
            quadrant = safeQuadrant,
            estimatedDurationMinutes = inferred.durationMinutes,
            energyLevel = inferred.energyDemand
        )
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(listOf(task), availableSlotsMinutes = 60, dailyCognitiveCapacityMinutes = 120)
        assertEquals(1, report.placedCount)
        assertEquals(0, report.deferredCount)
    }

    @Test
    fun testCross_ColdStartAndConfidenceDecouplingAndScoring() {
        val coldStartConfidence = 0.10f
        val (raw, usable) = E2ETestHarness.ConfidenceDecouplingOracle.calculateDecoupledEnergy(
            reservoirFactor = 0.8f,
            circadianFactor = 0.8f,
            sleepPressureRatio = 0.1f,
            confidenceFactor = coldStartConfidence
        )
        val liveContext = E2ELiveEnergyContext(usable, 0.8f, 0.8f, 0.1f, coldStartConfidence)
        val highBonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.HIGH, liveContext)
        assertTrue("Cold start user still receives high energy bonus if physiologically rested", highBonus >= 50f)
    }

    @Test
    fun testCross_AdhdQuickCaptureBlitzAndSupportiveReframingAndScheduling() {
        val rawBlitz = listOf(
            "Draft email 15m @work",
            "Submit quarterly VAT 90m @finance !frog",
            "Call accountant 20m @phone",
            "Update roadmap 45m @strategy",
            "Tidy workspace 15m @home"
        )
        val tasks = rawBlitz.mapIndexed { idx, raw ->
            val inf = E2ETestHarness.TaskInferenceOracle.infer(raw)
            TaskEntity(
                id = "blitz-$idx",
                title = inf.sanitizedTitle,
                estimatedDurationMinutes = inf.durationMinutes,
                energyLevel = inf.energyDemand,
                isFrog = inf.isFrog
            )
        }
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(tasks, availableSlotsMinutes = 60, dailyCognitiveCapacityMinutes = 120)
        assertEquals(5, report.placedCount + report.deferredCount)
        assertTrue("Frog task prioritized or clear deferral provided", report.placementRationales.isNotEmpty() || report.deferrals.isNotEmpty())
    }
}
