package com.neuroflow.app.domain.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class SleepInertiaAndChronicRestrictionTest : StringSpec({

    fun closeTo(actual: Float, expected: Float, tolerance: Float = 0.001f): Boolean {
        return abs(actual - expected) <= tolerance
    }

    "calculateSleepInertiaPenalty: decays linearly from max penalty at 0 min to 0 at 30 min" {
        // At 0 minutes, full penalty (0.35f)
        closeTo(SleepPressureDetector.calculateSleepInertiaPenalty(0), 0.35f) shouldBe true

        // At 15 minutes, exactly half penalty (0.175f)
        closeTo(SleepPressureDetector.calculateSleepInertiaPenalty(15), 0.175f) shouldBe true

        // At 30 minutes, 0 penalty
        closeTo(SleepPressureDetector.calculateSleepInertiaPenalty(30), 0.0f) shouldBe true

        // Beyond 30 minutes, stays 0
        closeTo(SleepPressureDetector.calculateSleepInertiaPenalty(45), 0.0f) shouldBe true
        closeTo(SleepPressureDetector.calculateSleepInertiaPenalty(120), 0.0f) shouldBe true
    }

    "isSleepInertiaActive: true when wake recency is under 30 minutes, false otherwise or when null" {
        SleepPressureDetector.isSleepInertiaActive(0) shouldBe true
        SleepPressureDetector.isSleepInertiaActive(15) shouldBe true
        SleepPressureDetector.isSleepInertiaActive(29) shouldBe true
        SleepPressureDetector.isSleepInertiaActive(30) shouldBe false
        SleepPressureDetector.isSleepInertiaActive(31) shouldBe false
        SleepPressureDetector.isSleepInertiaActive(null) shouldBe false
    }

    "calculateChronicRestrictionMultiplier: baseline 1.0x when rolling daily sleep is 6 hours or greater" {
        // 6 hours (360 min)
        SleepPressureDetector.calculateChronicRestrictionMultiplier(360f) shouldBe 1.0f

        // 7 hours (420 min)
        SleepPressureDetector.calculateChronicRestrictionMultiplier(420f) shouldBe 1.0f

        // 8 hours (480 min)
        SleepPressureDetector.calculateChronicRestrictionMultiplier(480f) shouldBe 1.0f
    }

    "calculateChronicRestrictionMultiplier: scales up to 1.6x as sleep deficit drops below 6 hours" {
        // 5 hours (300 min): deficit = 1 hour. penalty = (1 / 6) * 0.6 = 0.10f -> 1.10f
        closeTo(SleepPressureDetector.calculateChronicRestrictionMultiplier(300f), 1.10f) shouldBe true

        // 3 hours (180 min): deficit = 3 hours. penalty = (3 / 6) * 0.6 = 0.30f -> 1.30f
        closeTo(SleepPressureDetector.calculateChronicRestrictionMultiplier(180f), 1.30f) shouldBe true

        // 0 hours: capped at 1.60f
        closeTo(SleepPressureDetector.calculateChronicRestrictionMultiplier(0f), 1.60f) shouldBe true
    }

    "adjustedFatigueRatio: accounts for baseline pressure, chronic debt multiplier, and sleep inertia" {
        // pressurePoints = 200 (at softMaxReference 1000, base fatigue ratio is approx ~0.29)
        // With inertia (0m post wake, +0.35f) and chronic deficit (3h daily, mult = 1.3f)
        val baseRatio = SleepPressureDetector.fatigueRatio(200)
        val expected = (baseRatio * 1.3f + 0.35f).coerceIn(0f, 1f)

        val adjusted = SleepPressureDetector.adjustedFatigueRatio(
            pressurePoints = 200,
            minutesSinceWake = 0,
            rollingAverageSleepMinutes = 180f
        )
        closeTo(adjusted, expected) shouldBe true
    }

    "adjustedFatigueZone: escalates zone based on adjusted fatigue" {
        // Zero pressure without inertia: RESTED
        val restedZone = SleepPressureDetector.adjustedFatigueZone(
            pressurePoints = 0,
            minutesSinceWake = 60,
            rollingAverageSleepMinutes = 480f
        )
        restedZone shouldBe SleepPressureDetector.FatigueZone.RESTED

        // Zero pressure with immediate wake inertia (+0.35f -> 35%): escalates to MODERATE (25..49)
        val inertiaZone = SleepPressureDetector.adjustedFatigueZone(
            pressurePoints = 0,
            minutesSinceWake = 0,
            rollingAverageSleepMinutes = 480f
        )
        inertiaZone shouldBe SleepPressureDetector.FatigueZone.MODERATE

        // High base pressure with chronic restriction: escalates to CRITICAL
        val criticalZone = SleepPressureDetector.adjustedFatigueZone(
            pressurePoints = 900,
            minutesSinceWake = 0,
            rollingAverageSleepMinutes = 0f
        )
        criticalZone shouldBe SleepPressureDetector.FatigueZone.CRITICAL
    }
})
