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
 * Empirical Adversarial Challenge Test Suite for Milestone 1 (R1).
 *
 * Tests boundary conditions, extreme chronotypes, midnight wrap transitions,
 * confidence decoupling, and TaskScoringEngine under extreme LiveEnergyContexts.
 */
class Milestone1AdversarialChallengeTest : StringSpec({

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
        title = "Task $id",
        taskType = type,
        energyLevel = energy,
        priority = priority,
        quadrant = quadrant,
        status = TaskStatus.ACTIVE
    )

    // =========================================================================
    // SECTION 1: Extreme Chronotypes, Midnight Wrap, Pre-Peak Ramp, Extreme Offsets
    // =========================================================================

    "PeakEnergyEngine: night-shift chronotype waking at 23:00 with peak at 02:00 ramps smoothly across midnight" {
        val nightOwl = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_EVENING,
            wakeUpHour = 23,
            peakOffsetHours = 3.0f,
            peakHourOfDay = 2,
            peakMinuteOfDay = 120, // 02:00 AM
            peakValue = 4000,
            confidence = 1.0f
        )

        // Wake time at 23:00 (minute 1380): baseline 80% = 3200
        val wakeVal = nightOwl.currentValueAt(millisAtMinute(1380))
        wakeVal shouldBe 3200

        // 23:59 (minute 1439, 59 mins into 180 min ramp): ~3462
        val preMidnightVal = nightOwl.currentValueAt(millisAtMinute(1439))
        (preMidnightVal in 3450..3480) shouldBe true

        // 00:00 (minute 0, 60 mins into 180 min ramp): exactly 3200 + 800*(60/180) = 3467
        val midnightVal = nightOwl.currentValueAt(millisAtMinute(0))
        midnightVal shouldBe 3467

        // 00:01 (minute 1, 61 mins into ramp): ~3471
        val postMidnightVal = nightOwl.currentValueAt(millisAtMinute(1))
        (postMidnightVal in 3468..3475) shouldBe true

        // Discontinuity check across midnight: difference between 1439 -> 0 -> 1 must be <= 5 points
        abs(midnightVal - preMidnightVal) shouldBe (midnightVal - preMidnightVal) // must be increasing
        (midnightVal - preMidnightVal <= 10) shouldBe true
        (postMidnightVal - midnightVal <= 10) shouldBe true

        // At peak (02:00 AM, minute 120): reaches full 4000
        val peakVal = nightOwl.currentValueAt(millisAtMinute(120))
        peakVal shouldBe 4000
    }

    "PeakEnergyEngine: zero-offset definite morning waking at 06:00 reaches full peak immediately" {
        val morning = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
            wakeUpHour = 6,
            peakOffsetHours = 0f,
            peakHourOfDay = 6,
            peakMinuteOfDay = 360,
            peakValue = 4000,
            confidence = 1.0f
        )

        // At 06:00, minutesSincePeak = 0 -> value is 4000
        morning.currentValueAt(millisAtMinute(360)) shouldBe 4000

        // At 05:59 (1 min before wake), user was asleep / decayed from yesterday -> 0
        morning.currentValueAt(millisAtMinute(359)) shouldBe 0
    }

    "PeakEnergyEngine: extreme midnight wake-up (hour 0) with peak at 05:30" {
        val midnightWaker = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.INTERMEDIATE,
            wakeUpHour = 0,
            peakOffsetHours = 5.5f,
            peakHourOfDay = 5,
            peakMinuteOfDay = 330,
            peakValue = 4000,
            confidence = 1.0f
        )

        // At 00:00 (minute 0, wake time): baseline 80% = 3200
        midnightWaker.currentValueAt(millisAtMinute(0)) shouldBe 3200

        // At 02:45 (minute 165, halfway to peak): 90% = 3600
        midnightWaker.currentValueAt(millisAtMinute(165)) shouldBe 3600

        // At 05:30 (minute 330, peak): 100% = 4000
        midnightWaker.currentValueAt(millisAtMinute(330)) shouldBe 4000
    }

    "PeakEnergyEngine: 12-hour wake-to-peak boundary ramp" {
        val longOffset = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_EVENING,
            wakeUpHour = 6,
            peakOffsetHours = 12f,
            peakHourOfDay = 18,
            peakMinuteOfDay = 1080,
            peakValue = 4000,
            confidence = 1.0f
        )

        // Exactly 12 hours (720 min) is inside 1..(12*60)
        // At 06:00 (wake): 3200
        longOffset.currentValueAt(millisAtMinute(360)) shouldBe 3200

        // At 12:00 (halfway, 360 min): 3600
        longOffset.currentValueAt(millisAtMinute(720)) shouldBe 3600

        // At 18:00 (peak): 4000
        longOffset.currentValueAt(millisAtMinute(1080)) shouldBe 4000
    }

    "EnergyScoreEngine: circadianFactor cosine window wrapping exactly across midnight (1439 -> 0 -> 1)" {
        val midnightSpanningProfile = PeakEnergyEngine.CircadianProfile(
            windows = listOf(
                PeakEnergyEngine.PeakWindow(
                    startMinuteOffset = 1380, // 23:00
                    durationMinutes = 120,    // 2 hours -> ends at 01:00 (minute 60)
                    amplitude = 1.0f
                )
            )
        )

        // Cosine window starts at 1380 (23:00) with duration 120 min.
        // Apex (phase = 0.5) is at 1380 + 60 = 1440 = 0 (00:00 midnight)!
        val at1439 = EnergyScoreEngine.circadianFactor(1439, midnightSpanningProfile)
        val at0 = EnergyScoreEngine.circadianFactor(0, midnightSpanningProfile)
        val at1 = EnergyScoreEngine.circadianFactor(1, midnightSpanningProfile)

        // At minute 0 (midnight), phase is exactly 60/120 = 0.5, raw is 0.5 * (1 - cos(PI)) = 1.0!
        closeTo(at0, 1.0f, 0.001f)

        // At 1439 (23:59), phase is 59/120 = 0.491667 -> very close to 1.0
        (at1439 > 0.99f) shouldBe true

        // At 1 (00:01), phase is 61/120 = 0.508333 -> symmetric with 1439!
        (at1 > 0.99f) shouldBe true
        closeTo(at1439, at1, 0.001f)
    }

    "EMPIRICAL CHALLENGE: EnergyScoreEngine continuity at exact peak minute" {
        val detection = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.MODERATE_MORNING,
            wakeUpHour = 7,
            peakOffsetHours = 2.5f,
            peakHourOfDay = 9,
            peakMinuteOfDay = 570, // 9:30 AM
            peakValue = 4000,
            confidence = 1.0f
        )

        // At 9:29 AM (minute 569, 1 min before peak)
        val snapshotBefore = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detection,
            sleepPressurePoints = 0,
            nowMillis = millisAtMinute(569)
        )
        val resultBefore = EnergyScoreEngine.calculateDetailed(snapshotBefore)

        // At 9:30 AM (minute 570, exact peak minute)
        val snapshotAtPeak = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detection,
            sleepPressurePoints = 0,
            nowMillis = millisAtMinute(570)
        )
        val resultAtPeak = EnergyScoreEngine.calculateDetailed(snapshotAtPeak)

        println("=== ADVERSARIAL DISCOVERY CHECK ===")
        println("Minute 569 (9:29 AM): circadianFactor=${resultBefore.circadianFactor}, peakScore=${resultBefore.peakScore}, usableEnergy=${resultBefore.usableEnergy}")
        println("Minute 570 (9:30 AM): circadianFactor=${resultAtPeak.circadianFactor}, peakScore=${resultAtPeak.peakScore}, usableEnergy=${resultAtPeak.usableEnergy}")

        // Log whether there is a discontinuity cliff drop
        val drop = resultBefore.circadianFactor - resultAtPeak.circadianFactor
        println("Circadian factor drop at peak minute: $drop")

        // Invalidation conditions: continuous baseline support with no cliff drop
        (resultAtPeak.circadianFactor >= 0.85f) shouldBe true
        (resultAtPeak.usableEnergy >= 80f) shouldBe true
        (drop <= 0.05f) shouldBe true
    }

    // =========================================================================
    // SECTION 2: Confidence Factor Decoupling Across 0.0, 0.1, 0.5, 1.0
    // =========================================================================

    "EnergyScoreEngine: physical energy magnitude is identical across confidence 0.0, 0.1, 0.5, 1.0" {
        val confidences = listOf(0.0f, 0.1f, 0.5f, 1.0f)
        val results = confidences.map { conf ->
            val det = PeakEnergyEngine.DetectionResult(
                chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
                wakeUpHour = 6,
                peakOffsetHours = 0f,
                peakHourOfDay = 6,
                peakMinuteOfDay = 360,
                peakValue = 4000,
                confidence = conf
            )
            val snapshot = EnergyScoreEngine.EnergySnapshot(
                peakEnergy = det,
                sleepPressurePoints = 500,
                nowMillis = millisAtMinute(360)
            )
            EnergyScoreEngine.calculateDetailed(snapshot)
        }

        // Verify that confidenceFactor tracks the input exactly
        results.map { it.confidenceFactor } shouldBe confidences

        // Verify that physical magnitude (peakScore, rawEnergy, usableEnergy, fatiguePenalty)
        // is 100% IDENTICAL across all confidence levels
        val baseline = results.first()
        results.forEach { r ->
            closeTo(r.peakScore, baseline.peakScore, 0.0001f)
            closeTo(r.rawEnergy, baseline.rawEnergy, 0.0001f)
            closeTo(r.usableEnergy, baseline.usableEnergy, 0.0001f)
            closeTo(r.fatiguePenalty, baseline.fatiguePenalty, 0.0001f)
        }
    }
})

private fun closeTo(actual: Float, expected: Float, tolerance: Float = 0.001f) {
    (abs(actual - expected) <= tolerance) shouldBe true
}

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
