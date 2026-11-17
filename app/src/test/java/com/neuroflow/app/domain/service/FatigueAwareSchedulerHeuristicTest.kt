package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FatigueAwareSchedulerHeuristicTest {

    private val zoneId = ZoneId.of("UTC")
    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Test
    fun fatiguedState_penalizesLongerDurationProportionallyForHighCognitiveLoad() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(LocalTime.of(14, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val fatiguedSnapshot = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 42,
            homeostaticPressureS = 0.62,
            circadianAlertnessC = 0.45,
            focusDepletionPenalty = 0.08,
            phase = EnergyPhase.DIP
        )

        val shortDeep = SchedulableTask(
            id = "short_deep",
            title = "Focused Refactor",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 25,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = now
        )
        val longDeep = shortDeep.copy(
            id = "long_deep",
            title = "Marathon Architecture Overhaul",
            estimatedMinutes = 120
        )

        val shortScore = schedulerService.evaluateTaskScore(shortDeep, fatiguedSnapshot, calibrationRatio = 1.0)
        val longScore = schedulerService.evaluateTaskScore(longDeep, fatiguedSnapshot, calibrationRatio = 1.0)

        assertTrue("Short HIGH task under fatigue should have negative fatigueAdjustment (${shortScore.fatigueAdjustment})", shortScore.fatigueAdjustment < 0.0)
        assertTrue(
            "Long 120m HIGH task (${longScore.fatigueAdjustment}) must incur a stricter fatigue penalty than 25m task (${shortScore.fatigueAdjustment})",
            longScore.fatigueAdjustment < shortScore.fatigueAdjustment - 5.0
        )
        assertTrue(
            "Long HIGH task totalScore (${longScore.totalScore}) must fall below short task (${shortScore.totalScore}) under fatigue",
            longScore.totalScore < shortScore.totalScore
        )
    }

    @Test
    fun fatiguedState_differentiatesCognitiveLoadAndBoostsRestorativeTasks() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(LocalTime.of(16, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val recoveryNeededSnapshot = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 36,
            homeostaticPressureS = 0.68,
            circadianAlertnessC = 0.40,
            focusDepletionPenalty = 0.11,
            phase = EnergyPhase.RECOVERY_NEEDED
        )

        val heavyDeep = SchedulableTask(
            id = "heavy_deep",
            title = "Deep Systems Proof",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = now
        )
        val moderateGeneral = heavyDeep.copy(
            id = "mod_general",
            title = "Routine Review",
            category = TaskCategory.GENERAL,
            energyDemand = EnergyDemand.MODERATE
        )
        val restorativeMindfulness = SchedulableTask(
            id = "restorative",
            title = "Guided Decompression Breathwork",
            category = TaskCategory.MINDFULNESS,
            priority = 1,
            estimatedMinutes = 25,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            createdAtMillis = now
        )

        val heavyBreakdown = schedulerService.evaluateTaskScore(heavyDeep, recoveryNeededSnapshot, calibrationRatio = 1.0)
        val modBreakdown = schedulerService.evaluateTaskScore(moderateGeneral, recoveryNeededSnapshot, calibrationRatio = 1.0)
        val restBreakdown = schedulerService.evaluateTaskScore(restorativeMindfulness, recoveryNeededSnapshot, calibrationRatio = 1.0)

        assertTrue(
            "Heavy DEEP_WORK must suffer stronger burnout penalty (${heavyBreakdown.fatigueAdjustment}) than moderate work (${modBreakdown.fatigueAdjustment})",
            heavyBreakdown.fatigueAdjustment < modBreakdown.fatigueAdjustment - 4.0
        )
        assertTrue(
            "Restorative MINDFULNESS task must receive a positive fatigue-relief adjustment (${restBreakdown.fatigueAdjustment})",
            restBreakdown.fatigueAdjustment > 4.0
        )
        assertEquals(
            "Urgency score remains untouched by fatigue heuristic",
            heavyBreakdown.urgencyScore,
            modBreakdown.urgencyScore,
            0.001
        )
        assertTrue(heavyBreakdown.totalScore in 0.0..100.0)
        assertTrue(restBreakdown.totalScore in 0.0..100.0)
    }

    @Test
    fun restedPeakState_keepsStandardTasksUndistortedWhileGuardingMarathonOverruns() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val restedPeak = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 85,
            homeostaticPressureS = 0.20,
            circadianAlertnessC = 0.90,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val normalBlock = SchedulableTask(
            id = "normal_45",
            title = "Focused Sprint",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = now
        )
        val marathonBlock = normalBlock.copy(
            id = "marathon_150",
            title = "Three-Hour Unbroken Grind",
            estimatedMinutes = 150
        )

        val normalScore = schedulerService.evaluateTaskScore(normalBlock, restedPeak, calibrationRatio = 1.0)
        val marathonScore = schedulerService.evaluateTaskScore(marathonBlock, restedPeak, calibrationRatio = 1.0)

        assertEquals(0.0, normalScore.fatigueAdjustment, 0.001)
        assertTrue(
            "Marathon >90m high-load block should still carry an intrinsic burnout guard even when rested (${marathonScore.fatigueAdjustment})",
            marathonScore.fatigueAdjustment < -4.0
        )
    }

    @Test
    fun cumulativeCognitiveLoad_interleavesRestorativeWorkOverBurnoutHeavyQueue() {
        val day = LocalDate.of(2026, 11, 17)
        val wake = day.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val now = day.atTime(LocalTime.of(8, 30)).atZone(zoneId).toInstant().toEpochMilli()

        // Already locked 2.5h of heavy cognitive work early morning (cumulative load > 180)
        val morningHeavy = SchedulableTask(
            id = "locked_heavy_am",
            title = "Locked Systems Build",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 120,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            isLocked = true,
            scheduledStartMillis = day.atTime(LocalTime.of(8, 30)).atZone(zoneId).toInstant().toEpochMilli(),
            createdAtMillis = now
        )

        // With 160m budget total (120m used + 40m left -> tighten slightly to force choosing between a 35m light reset or second heavy block):
        // Better: compare cumulative load scoring AND queue selection in a tight 170m budget where light (35m) fits comfortably vs another 100m heavy block,
        // or equal 45m tasks competing for 170m remaining budget (120m locked + 45m slot = 165m budget)!
        val extraHeavy = SchedulableTask(
            id = "heavy_2",
            title = "Second Intense Deep Work",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            createdAtMillis = now
        )
        val lightRecovery = SchedulableTask(
            id = "light_recovery",
            title = "Walking Reflection & Notes",
            category = TaskCategory.MINDFULNESS,
            priority = 1,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            createdAtMillis = now
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(morningHeavy, extraHeavy, lightRecovery),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 8,
            workDayEndHour = 19,
            dailyCognitiveCapacityMinutes = 170 // 120m locked + room for only ONE of the two 45m tasks
        )

        assertTrue(outcome.placements.any { it.task.id == "locked_heavy_am" })
        assertTrue(
            "Under cumulative cognitive fatigue from 120m DEEP_WORK, scheduler should pick restorative task over piling another HIGH block into tight capacity",
            outcome.placements.any { it.task.id == "light_recovery" }
        )
        assertTrue(
            "Second heavy block should be deferred past tight fatigue capacity",
            outcome.deferrals.any { it.taskId == "heavy_2" }
        )
    }
}
