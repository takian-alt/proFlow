package com.neuroflow.app.domain.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SleepPressureDetectorTest : StringSpec({

    "awake accumulation is one point per minute" {
        SleepPressureDetector.detect(24f).pressurePoints shouldBe 1440
    }

    "negative awake hours are clamped to zero" {
        val result = SleepPressureDetector.detect(-2f)

        result.pressurePoints shouldBe 0
        result.awakeMinutes shouldBe 0
    }

    "recovery milestones match the specification" {
        SleepPressureDetector.recoveryForSleepMinutes(30) shouldBe 30
        SleepPressureDetector.recoveryForSleepMinutes(60) shouldBe 186
        SleepPressureDetector.recoveryForSleepMinutes(75) shouldBe 216
        SleepPressureDetector.recoveryForSleepMinutes(90) shouldBe 279
        SleepPressureDetector.recoveryForSleepMinutes(100) shouldBe 289
        SleepPressureDetector.recoveryForSleepMinutes(180) shouldBe 558
        SleepPressureDetector.recoveryForSleepMinutes(420) shouldBe 1302
    }

    "interruption boundaries apply only eligible bonuses" {
        SleepPressureDetector.recoveryForSleepMinutes(59) shouldBe 59
        SleepPressureDetector.recoveryForSleepMinutes(60) shouldBe 186
        SleepPressureDetector.recoveryForSleepMinutes(61) shouldBe 188
        SleepPressureDetector.recoveryForSleepMinutes(89) shouldBe 244
        SleepPressureDetector.recoveryForSleepMinutes(90) shouldBe 279
    }

    "full cycle repeats then starts at phase one again" {
        // 2 full cycles (558) + 10 minutes in new phase one (10)
        SleepPressureDetector.recoveryForSleepMinutes(190) shouldBe 568
    }

    "sleep recovery is floored at zero pressure" {
        val result = SleepPressureDetector.applySleepSession(
            currentPressure = 1020,
            sleepMinutes = 420
        )

        result.pressurePoints shouldBe 0
        result.recoveryPointsEarned shouldBe 1302
        result.recoveryPointsApplied shouldBe 1020
    }

    "awake accumulation adds to current pressure" {
        val result = SleepPressureDetector.applyAwakeMinutes(
            currentPressure = 200,
            awakeMinutes = 95
        )

        result.pressurePoints shouldBe 295
        result.awakeMinutes shouldBe 95
    }

    "negative inputs are safely clamped" {
        SleepPressureDetector.recoveryForSleepMinutes(-10) shouldBe 0

        val awakeResult = SleepPressureDetector.applyAwakeMinutes(
            currentPressure = -20,
            awakeMinutes = -5
        )
        awakeResult.pressurePoints shouldBe 0

        val sleepResult = SleepPressureDetector.applySleepSession(
            currentPressure = -20,
            sleepMinutes = -40
        )
        sleepResult.pressurePoints shouldBe 0
        sleepResult.recoveryPointsEarned shouldBe 0
        sleepResult.recoveryPointsApplied shouldBe 0
    }

    "fatigue percent maps raw pressure to 0 to 100" {
        SleepPressureDetector.fatiguePercent(0) shouldBe 0
        SleepPressureDetector.fatiguePercent(1500) shouldBe 57
        SleepPressureDetector.fatiguePercent(940) shouldBe 39
        SleepPressureDetector.fatiguePercent(3000) shouldBe 100
        SleepPressureDetector.fatiguePercent(9000) shouldBe 100
    }

    "fatigue ratio is monotonic with pressure" {
        val low = SleepPressureDetector.fatigueRatio(600)
        val mid = SleepPressureDetector.fatigueRatio(1400)
        val high = SleepPressureDetector.fatigueRatio(2400)

        (low < mid) shouldBe true
        (mid < high) shouldBe true
    }

    "fatigue zones follow configured thresholds" {
        SleepPressureDetector.fatigueZone(100) shouldBe SleepPressureDetector.FatigueZone.RESTED
        SleepPressureDetector.fatigueZone(900) shouldBe SleepPressureDetector.FatigueZone.MODERATE
        SleepPressureDetector.fatigueZone(1800) shouldBe SleepPressureDetector.FatigueZone.HIGH
        SleepPressureDetector.fatigueZone(2600) shouldBe SleepPressureDetector.FatigueZone.CRITICAL
    }
})
