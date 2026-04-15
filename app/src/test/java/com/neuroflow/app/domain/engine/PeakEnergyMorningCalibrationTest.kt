package com.neuroflow.app.domain.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PeakEnergyMorningCalibrationTest : StringSpec({

    "low sleep debt and high consistency keep stronger confidence" {
        val baseline = morningResult()
        val highQualitySignals = PeakEnergyEngine.MorningPersonalizationSignals(
            averageSleepMinutes = 480,
            wakeVarianceMinutes = 18,
            sleepLogCoverage = 0.9f,
            behaviorCoverage = 0.85f,
            behaviorSlots = listOf(
                PeakEnergyEngine.SlotPerformanceAggregate(
                    bucketStartMinute = 120,
                    qualityWeightedCompletionRate = 0.9f,
                    abortRate = 0.08f,
                    distractionRate = 0.18f,
                    sampleCount = 8
                )
            )
        )
        val lowerQualitySignals = PeakEnergyEngine.MorningPersonalizationSignals(
            averageSleepMinutes = 360,
            wakeVarianceMinutes = 130,
            sleepLogCoverage = 0.35f,
            behaviorCoverage = 0.3f,
            behaviorSlots = listOf(
                PeakEnergyEngine.SlotPerformanceAggregate(
                    bucketStartMinute = 300,
                    qualityWeightedCompletionRate = 0.3f,
                    abortRate = 0.55f,
                    distractionRate = 0.7f,
                    sampleCount = 4
                )
            )
        )

        val highQuality = PeakEnergyEngine.detect(
            meqResult = baseline,
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 700,
            personalizationSignals = highQualitySignals
        )
        val lowerQuality = PeakEnergyEngine.detect(
            meqResult = baseline,
            wakeUpHour = 6,
            sleepHour = 0,
            sleepPressurePoints = 2200,
            personalizationSignals = lowerQualitySignals
        )

        lowerQuality.confidence shouldBeLessThan highQuality.confidence
    }

    "inconsistent wake timing increases phase shift compared to consistent case" {
        val baseline = morningResult()
        val consistent = PeakEnergyEngine.detect(
            meqResult = baseline,
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 900,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                averageSleepMinutes = 470,
                wakeVarianceMinutes = 12,
                sleepLogCoverage = 0.9f
            )
        )
        val inconsistent = PeakEnergyEngine.detect(
            meqResult = baseline,
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 900,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                averageSleepMinutes = 470,
                wakeVarianceMinutes = 165,
                sleepLogCoverage = 0.9f
            )
        )

        inconsistent.circadianProfile.phaseShiftMinutes shouldNotBe consistent.circadianProfile.phaseShiftMinutes
        (inconsistent.circadianProfile.phaseShiftMinutes > consistent.circadianProfile.phaseShiftMinutes) shouldBe true
    }

    "weekend profile type is preserved in effective profile" {
        val detected = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 7,
            sleepHour = 23,
            sleepPressurePoints = 800,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                profileType = PeakEnergyEngine.ProfileType.WEEKEND,
                averageSleepMinutes = 510,
                wakeVarianceMinutes = 45,
                sleepLogCoverage = 0.7f
            )
        )

        detected.effectiveProfile.profileType shouldBe PeakEnergyEngine.ProfileType.WEEKEND
    }

    "morning windows keep stable durations 210 150 60" {
        val detected = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 1000,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                averageSleepMinutes = 460,
                wakeVarianceMinutes = 35,
                sleepLogCoverage = 0.8f
            )
        )

        detected.circadianProfile.windows.map { it.durationMinutes } shouldBe listOf(210, 150, 60)
    }

    "cold start dampens behavior shift magnitude" {
        val richSignals = PeakEnergyEngine.MorningPersonalizationSignals(
            averageSleepMinutes = 460,
            wakeVarianceMinutes = 35,
            sleepLogCoverage = 0.8f,
            behaviorSlots = listOf(
                PeakEnergyEngine.SlotPerformanceAggregate(
                    bucketStartMinute = 360,
                    qualityWeightedCompletionRate = 0.9f,
                    abortRate = 0.05f,
                    distractionRate = 0.15f,
                    sampleCount = 12
                )
            ),
            coldStartFactor = 1f
        )
        val coldSignals = richSignals.copy(coldStartFactor = 0.35f)
        val rich = PeakEnergyEngine.detect(morningResult(), 6, 22, 900, richSignals)
        val cold = PeakEnergyEngine.detect(morningResult(), 6, 22, 900, coldSignals)
        (kotlin.math.abs(cold.circadianProfile.phaseShiftMinutes) <= kotlin.math.abs(rich.circadianProfile.phaseShiftMinutes)) shouldBe true
    }

    "task type performance contributes to anchor shift" {
        val base = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 800,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                sleepLogCoverage = 0.8f,
                wakeVarianceMinutes = 25,
                averageSleepMinutes = 470
            )
        )
        val shifted = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 800,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                sleepLogCoverage = 0.8f,
                wakeVarianceMinutes = 25,
                averageSleepMinutes = 470,
                taskTypePerformance = listOf(
                    PeakEnergyEngine.TaskTypePerformanceAggregate(
                        taskType = "ANALYTICAL",
                        averageBestBucketMinute = 300,
                        weightedQuality = 0.9f,
                        sampleCount = 10
                    )
                )
            )
        )
        shifted.peakMinuteOfDay shouldNotBe base.peakMinuteOfDay
    }

    "drift status is exposed in effective profile" {
        val detected = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 900,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                sleepLogCoverage = 0.9f,
                wakeVarianceMinutes = 22,
                averageSleepMinutes = 470,
                driftMinutes = 24,
                weeklyBacktestErrorMinutes = 38f
            )
        )
        detected.effectiveProfile.driftStatus shouldBe "later_drift"
        detected.effectiveProfile.weeklyBacktestErrorMinutes shouldBe 38f
        detected.effectiveProfile.confidence.shouldBeInstanceOf<PeakEnergyEngine.ConfidenceComponents>()
    }

    "manual profile override replaces adaptive anchor and windows" {
        val detected = PeakEnergyEngine.detect(
            meqResult = morningResult(),
            wakeUpHour = 6,
            sleepHour = 22,
            sleepPressurePoints = 1400,
            personalizationSignals = PeakEnergyEngine.MorningPersonalizationSignals(
                profileOverride = PeakEnergyEngine.ProfileOverride(
                    enabled = true,
                    profileType = PeakEnergyEngine.ProfileType.WEEKEND,
                    anchorMinuteOfDay = 510,
                    windows = listOf(
                        PeakEnergyEngine.PeakWindow(0, 180, 1.0f),
                        PeakEnergyEngine.PeakWindow(570, 120, 0.7f),
                        PeakEnergyEngine.PeakWindow(810, 90, 0.5f)
                    )
                )
            )
        )

        detected.peakMinuteOfDay shouldBe 510
        detected.circadianProfile.windows.map { it.durationMinutes } shouldBe listOf(180, 120, 90)
        detected.effectiveProfile.profileType shouldBe PeakEnergyEngine.ProfileType.WEEKEND
    }
})

private fun morningResult(): MEQChronotypeDetector.Result {
    return MEQChronotypeDetector.Result(
        totalScore = 64,
        chronotype = MEQChronotypeDetector.Chronotype.MODERATE_MORNING,
        baselinePeakStartHour = 7,
        baselinePeakEndHour = 12,
        answeredQuestions = MEQChronotypeDetector.QUESTION_COUNT,
        confidence = 1f
    )
}
