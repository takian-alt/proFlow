package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import kotlin.math.abs

/**
 * Empirical Adversarial Stress Test Suite for Milestone 1 Iteration 2.
 *
 * Exhaustively stress-tests:
 * 1. Rested wake peak energy (circadianFactor >= 0.85f, usableEnergy >= 80f) across all chronotypes.
 * 2. Fine-grained floating-point boundary scans around 70.0f and 40.0f in TaskScoringEngine.
 * 3. Morning wake-to-peak transition boundaries (ramp continuity, delta <= 0.05f).
 * 4. Afternoon decay transition into trough and evening rebound (no cliff drops, floor >= 0.35f, usable > 50%).
 * 5. 1440-minute full 24-hour cycle sweep verifying mathematical stability (no NaN, bounds [0, 1] and [0, 100]).
 */
class Milestone1AdversarialStressTest : StringSpec({

    val defaultPrefs = UserPreferences(
        workDayStart = 9,
        workDayEnd = 17,
        peakEnergyStart = 10,
        peakEnergyEnd = 12,
        weightQuadrant = 1.0f,
        weightPriorityLevel = 1.0f
    )

    fun buildTask(
        id: String,
        type: TaskType = TaskType.ANALYTICAL,
        energy: EnergyLevel = EnergyLevel.HIGH,
        priority: Priority = Priority.MEDIUM,
        quadrant: Quadrant = Quadrant.SCHEDULE
    ) = TaskEntity(
        id = id,
        title = "Stress Task $id",
        taskType = type,
        energyLevel = energy,
        priority = priority,
        quadrant = quadrant,
        status = TaskStatus.ACTIVE
    )

    // =========================================================================
    // OBJECTIVE 1: Rested Wake at Peak Minute Across All Chronotypes
    // =========================================================================

    "OBJECTIVE 1: At nowMinuteOfDay = peakMinuteOfDay, circadianFactor >= 0.85f and usableEnergy >= 80f for all chronotypes" {
        val chronotypes = listOf(
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING to 6,   // wake 6:00, peak 6:00 (offset 0h)
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING to 7,   // wake 7:00, peak 9:30 (offset 2.5h)
            MEQChronotypeDetector.Chronotype.INTERMEDIATE to 8,       // wake 8:00, peak 10:30 (offset 2.5h)
            MEQChronotypeDetector.Chronotype.MODERATE_EVENING to 9,   // wake 9:00, peak 14:30 (offset 5.5h)
            MEQChronotypeDetector.Chronotype.DEFINITE_EVENING to 10   // wake 10:00, peak 15:30 (offset 5.5h)
        )

        for ((chrono, wakeHour) in chronotypes) {
            val detection = PeakEnergyEngine.detect(
                meqResult = MEQChronotypeDetector.Result(
                    totalScore = 60,
                    chronotype = chrono,
                    baselinePeakStartHour = 8,
                    baselinePeakEndHour = 12,
                    answeredQuestions = MEQChronotypeDetector.QUESTION_COUNT,
                    confidence = 1.0f
                ),
                wakeUpHour = wakeHour
            )

            val snapshotAtPeak = EnergyScoreEngine.EnergySnapshot(
                peakEnergy = detection,
                sleepPressurePoints = 0, // Rested wake
                nowMillis = millisAtMinute(detection.peakMinuteOfDay)
            )

            val result = EnergyScoreEngine.calculateDetailed(snapshotAtPeak)

            println("Testing Chronotype $chrono (Wake: $wakeHour:00, Peak: ${detection.peakMinuteOfDay / 60}:${detection.peakMinuteOfDay % 60})")
            println(" -> circadianFactor=${result.circadianFactor}, peakScore=${result.peakScore}, usableEnergy=${result.usableEnergy}")

            (result.circadianFactor >= 0.85f) shouldBe true
            (result.usableEnergy >= 80.0f) shouldBe true
            (result.circadianFactor <= 1.0f) shouldBe true
            (result.usableEnergy <= 100.0f) shouldBe true
        }
    }

    // =========================================================================
    // OBJECTIVE 2: Float Boundary Sweep around 70f and 40f
    // =========================================================================

    "OBJECTIVE 2: Dense floating-point scan around 70f and 40f boundaries partitions cleanly" {
        val highTask = buildTask("high_task", TaskType.ANALYTICAL, EnergyLevel.HIGH)
        val medTask = buildTask("med_task", TaskType.ANALYTICAL, EnergyLevel.MEDIUM)
        val lowTask = buildTask("low_task", TaskType.ANALYTICAL, EnergyLevel.LOW)

        // Test points around 70.0f boundary
        val highBoundaries = listOf(100.0f, 85.0f, 70.001f, 70.0001f, 70.0f)
        for (energy in highBoundaries) {
            val ctx = TaskScoringEngine.LiveEnergyContext(usableEnergy = energy)
            val breakdownHigh = TaskScoringEngine.scoreBreakdown(highTask, defaultPrefs, liveEnergy = ctx)
            val matchHigh = breakdownHigh.find { it.first == "Energy matching" }?.second
            val circHigh = breakdownHigh.find { it.first == "Circadian task matching" }?.second
            matchHigh shouldBe 70f
            circHigh shouldBe 60f

            val breakdownMed = TaskScoringEngine.scoreBreakdown(medTask, defaultPrefs, liveEnergy = ctx)
            val matchMed = breakdownMed.find { it.first == "Energy matching" }?.second
            matchMed shouldBe 20f

            val breakdownLow = TaskScoringEngine.scoreBreakdown(lowTask, defaultPrefs, liveEnergy = ctx)
            val matchLow = breakdownLow.find { it.first == "Energy matching" }?.second
            matchLow shouldBe -35f
        }

        // Test points in Moderate range [40.0f, 70.0f), specifically checking near-70 gaps
        val moderateBoundaries = listOf(69.99999f, 69.9995f, 69.999f, 69.9f, 69.0f, 55.0f, 40.001f, 40.0f)
        for (energy in moderateBoundaries) {
            val ctx = TaskScoringEngine.LiveEnergyContext(usableEnergy = energy)
            val breakdownHigh = TaskScoringEngine.scoreBreakdown(highTask, defaultPrefs, liveEnergy = ctx)
            val matchHigh = breakdownHigh.find { it.first == "Energy matching" }?.second
            val circHigh = breakdownHigh.find { it.first == "Circadian task matching" }?.second
            matchHigh shouldBe 10f
            circHigh shouldBe 15f

            val breakdownMed = TaskScoringEngine.scoreBreakdown(medTask, defaultPrefs, liveEnergy = ctx)
            val matchMed = breakdownMed.find { it.first == "Energy matching" }?.second
            matchMed shouldBe 30f

            val breakdownLow = TaskScoringEngine.scoreBreakdown(lowTask, defaultPrefs, liveEnergy = ctx)
            val matchLow = breakdownLow.find { it.first == "Energy matching" }?.second
            matchLow shouldBe 15f
        }

        // Test points in Depleted range < 40.0f, specifically checking near-40 boundary
        val depletedBoundaries = listOf(39.99999f, 39.9995f, 39.999f, 39.9f, 30.0f, 15.0f, 0.0f)
        for (energy in depletedBoundaries) {
            val ctx = TaskScoringEngine.LiveEnergyContext(usableEnergy = energy)
            val breakdownHigh = TaskScoringEngine.scoreBreakdown(highTask, defaultPrefs, liveEnergy = ctx)
            val matchHigh = breakdownHigh.find { it.first == "Energy matching" }?.second
            val circHigh = breakdownHigh.find { it.first == "Circadian task matching" }?.second
            matchHigh shouldBe -35f
            circHigh shouldBe -25f

            val breakdownMed = TaskScoringEngine.scoreBreakdown(medTask, defaultPrefs, liveEnergy = ctx)
            val matchMed = breakdownMed.find { it.first == "Energy matching" }?.second
            matchMed shouldBe 10f

            val breakdownLow = TaskScoringEngine.scoreBreakdown(lowTask, defaultPrefs, liveEnergy = ctx)
            val matchLow = breakdownLow.find { it.first == "Energy matching" }?.second
            matchLow shouldBe 70f
        }
    }

    "OBJECTIVE 2: score() and scoreBreakdown() sum are 100% consistent across float boundary values" {
        val task = buildTask("boundary_task", TaskType.ANALYTICAL, EnergyLevel.HIGH)
        val testValues = listOf(0f, 39.999f, 40f, 40.001f, 55f, 69.999f, 69.9995f, 70f, 70.001f, 85f, 100f)

        for (v in testValues) {
            val ctx = TaskScoringEngine.LiveEnergyContext(usableEnergy = v)
            val directScore = TaskScoringEngine.score(task, defaultPrefs, liveEnergy = ctx)
            val breakdown = TaskScoringEngine.scoreBreakdown(task, defaultPrefs, liveEnergy = ctx)
            val breakdownSum = breakdown.sumOf { it.second.toDouble() }.toFloat()

            abs(directScore - breakdownSum) shouldBe 0f
        }
    }

    // =========================================================================
    // OBJECTIVE 3: Morning Wake to Peak & Afternoon Decay Transitions
    // =========================================================================

    "OBJECTIVE 3: Morning wake-to-peak transition is continuous with max single-minute delta <= 0.05f" {
        val detection = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.MODERATE_MORNING,
            wakeUpHour = 7,
            peakOffsetHours = 2.5f,
            peakHourOfDay = 9,
            peakMinuteOfDay = 570, // 9:30 AM (wake at 420 -> 150 min ramp)
            peakValue = 4000,
            confidence = 1.0f
        )

        var prevCircadian = -1.0f
        var maxDelta = 0.0f

        // Scan every minute from wake (420) to peak (570) and beyond to 580
        for (minute in 420..580) {
            val snapshot = EnergyScoreEngine.EnergySnapshot(
                peakEnergy = detection,
                sleepPressurePoints = 0,
                nowMillis = millisAtMinute(minute)
            )
            val result = EnergyScoreEngine.calculateDetailed(snapshot)

            // During pre-peak ramp (420..569), circadian factor must be monotonic non-decreasing
            if (minute < 570 && prevCircadian >= 0f) {
                (result.circadianFactor >= prevCircadian - 0.0001f) shouldBe true
            }

            // At peak minute (570), circadianFactor must be >= 0.85f and usableEnergy >= 80f
            if (minute == 570) {
                (result.circadianFactor >= 0.85f) shouldBe true
                (result.usableEnergy >= 80.0f) shouldBe true
            }

            if (prevCircadian >= 0f) {
                val delta = abs(result.circadianFactor - prevCircadian)
                if (delta > maxDelta) maxDelta = delta
                // Delta between consecutive minutes must never exceed 0.05 (no cliff drops)
                (delta <= 0.05f) shouldBe true
            }

            prevCircadian = result.circadianFactor
        }

        println("Morning ramp to peak max single-minute delta: $maxDelta")
    }

    "OBJECTIVE 3: Afternoon decay from peak through post-lunch trough into evening rebound" {
        val detection = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.MODERATE_MORNING,
            wakeUpHour = 7,
            peakOffsetHours = 2.5f,
            peakHourOfDay = 9,
            peakMinuteOfDay = 570, // 9:30 AM
            peakValue = 4000,
            confidence = 1.0f
        )

        var maxAfternoonDelta = 0.0f
        var prevCircadian = -1.0f

        // Minute 570 to 1200 (9:30 AM to 8:00 PM) covers:
        // - Window 1 (570 to 780, peak at 675)
        // - Afternoon trough (780 to 1140, baseline 0.35f)
        // - Window 2 evening rebound (1140 to 1290)
        for (minute in 570..1200) {
            val snapshot = EnergyScoreEngine.EnergySnapshot(
                peakEnergy = detection,
                sleepPressurePoints = 0, // Rested
                nowMillis = millisAtMinute(minute)
            )
            val result = EnergyScoreEngine.calculateDetailed(snapshot)

            // Invariant 1: Circadian factor never drops below 0.35f baseline floor
            (result.circadianFactor >= 0.35f) shouldBe true
            (result.circadianFactor <= 1.0f) shouldBe true

            // Invariant 2: Usable energy for rested user never drops below 50%
            (result.usableEnergy >= 50.0f) shouldBe true

            // Invariant 3: Single-minute delta across transition points is smooth (<= 0.05f)
            if (prevCircadian >= 0f) {
                val delta = abs(result.circadianFactor - prevCircadian)
                if (delta > maxAfternoonDelta) maxAfternoonDelta = delta
                (delta <= 0.05f) shouldBe true
            }

            prevCircadian = result.circadianFactor
        }

        println("Afternoon decay max single-minute delta: $maxAfternoonDelta")
    }

    "Full 1440-minute 24-hour cycle stress test for extreme chronotypes" {
        val chronos = listOf(
            PeakEnergyEngine.DetectionResult( // Night owl
                chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_EVENING,
                wakeUpHour = 23,
                peakOffsetHours = 3.0f,
                peakHourOfDay = 2,
                peakMinuteOfDay = 120,
                peakValue = 4000,
                confidence = 1.0f
            ),
            PeakEnergyEngine.DetectionResult( // Midnight waker
                chronotype = MEQChronotypeDetector.Chronotype.INTERMEDIATE,
                wakeUpHour = 0,
                peakOffsetHours = 5.5f,
                peakHourOfDay = 5,
                peakMinuteOfDay = 330,
                peakValue = 4000,
                confidence = 1.0f
            ),
            PeakEnergyEngine.DetectionResult( // Zero offset early morning
                chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
                wakeUpHour = 5,
                peakOffsetHours = 0f,
                peakHourOfDay = 5,
                peakMinuteOfDay = 300,
                peakValue = 4000,
                confidence = 1.0f
            )
        )

        for (detection in chronos) {
            for (minute in 0..1439) {
                val snapshot = EnergyScoreEngine.EnergySnapshot(
                    peakEnergy = detection,
                    sleepPressurePoints = 0,
                    nowMillis = millisAtMinute(minute)
                )
                val result = EnergyScoreEngine.calculateDetailed(snapshot)

                // No NaN, No Inf
                result.circadianFactor.isNaN() shouldBe false
                result.usableEnergy.isNaN() shouldBe false
                result.peakScore.isNaN() shouldBe false
                result.rawEnergy.isNaN() shouldBe false

                // Mathematical domain bounds
                (result.circadianFactor in 0f..1f) shouldBe true
                (result.reservoirFactor in 0f..1f) shouldBe true
                (result.peakScore in 0f..100f) shouldBe true
                (result.rawEnergy in -100f..100f) shouldBe true
                (result.usableEnergy in 0f..100f) shouldBe true
            }
        }
    }
})

private fun millisAtMinute(minuteOfDay: Int): Long {
    val safeMinuteOfDay = ((minuteOfDay % 1440) + 1440) % 1440
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, safeMinuteOfDay / 60)
        set(Calendar.MINUTE, safeMinuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
