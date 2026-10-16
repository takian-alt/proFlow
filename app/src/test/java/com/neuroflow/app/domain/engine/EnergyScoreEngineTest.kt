package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Calendar
import kotlin.math.abs

class EnergyScoreEngineTest : StringSpec({

    "circadian factor follows configured peak amplitudes" {
        closeTo(EnergyScoreEngine.circadianFactor(105), 1.0f)
        closeTo(EnergyScoreEngine.circadianFactor(645), 0.8f)
        closeTo(EnergyScoreEngine.circadianFactor(840), 0.6f)
        closeTo(EnergyScoreEngine.circadianFactor(400), 0.0f)
    }

    "raw and usable energy combine peak and fatigue" {
        val snapshot = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(),
            sleepPressurePoints = 1500,
            nowMillis = millisAtMinuteOfDay(105)
        )

        val result = EnergyScoreEngine.calculateDetailed(snapshot)

        closeTo(result.reservoirFactor, 0.89075f)
        closeTo(result.circadianFactor, 1.0f)
        closeTo(result.peakScore, 89.075f)
        closeTo(result.fatiguePenalty, 56.64419f, tolerance = 0.01f)
        closeTo(result.rawEnergy, 32.4308f, tolerance = 0.01f)
        closeTo(result.usableEnergy, 66.2154f, tolerance = 0.01f)
    }

    "sleep pressure penalty clamps at soft max" {
        val result = EnergyScoreEngine.calculateDetailed(
            EnergyScoreEngine.EnergySnapshot(
                sleepPressurePoints = 9999,
                softMaxReference = 3000
            )
        )

        closeTo(result.fatiguePenalty, 100.0f)
        closeTo(result.rawEnergy, -100.0f)
        closeTo(result.usableEnergy, 0.0f)
    }

    "calculate returns raw energy for compatibility" {
        val snapshot = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 800,
            nowMillis = millisAtMinuteOfDay(105)
        )

        val rawFromCalculate = EnergyScoreEngine.calculate(snapshot)
        val rawFromDetailed = EnergyScoreEngine.calculateDetailed(snapshot).rawEnergy

        closeTo(rawFromCalculate, rawFromDetailed)
    }

    "usable energy is normalized from raw energy bounds" {
        val max = EnergyScoreEngine.calculateDetailed(
            EnergyScoreEngine.EnergySnapshot(
                peakEnergy = detectionResult(confidence = 1.0f),
                sleepPressurePoints = 0,
                nowMillis = millisAtMinuteOfDay(105)
            )
        )
        val min = EnergyScoreEngine.calculateDetailed(
            EnergyScoreEngine.EnergySnapshot(
                sleepPressurePoints = 9999
            )
        )

        closeTo(max.usableEnergy, ((max.rawEnergy + 100f) / 2f).coerceIn(0f, 100f))
        closeTo(min.usableEnergy, ((min.rawEnergy + 100f) / 2f).coerceIn(0f, 100f))
    }

    "usable energy stays above 50 percent for rested user outside peak windows" {
        val snapshot = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 0,
            nowMillis = millisAtMinuteOfDay(400) // minute 400 is outside all 3 peak windows
        )

        val result = EnergyScoreEngine.calculateDetailed(snapshot)
        (result.usableEnergy > 50.0f) shouldBe true
        (result.circadianFactor >= 0.35f) shouldBe true
    }

    "PeakEnergyEngine currentValueAt approaches zero after 16 hours" {
        val result = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
            wakeUpHour = 6,
            peakOffsetHours = 0f,
            peakHourOfDay = 6,
            peakMinuteOfDay = 6 * 60,
            peakValue = PeakEnergyEngine.PEAK_VALUE,
            confidence = 1.0f
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val lateDayValue = result.currentValueAt(cal.timeInMillis)
        lateDayValue shouldBe 0
    }

    "FreshStartEngine isoWeekNumber and isoYear match java time standard" {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val millis = cal.timeInMillis

        val expectedWeek = LocalDate.of(2026, 1, 1).get(WeekFields.ISO.weekOfWeekBasedYear())
        val expectedYear = LocalDate.of(2026, 1, 1).get(WeekFields.ISO.weekBasedYear())

        FreshStartEngine.isoWeekNumber(millis) shouldBe expectedWeek
        FreshStartEngine.isoYear(millis) shouldBe expectedYear
    }

    "AutoSchedulingContracts protects manual schedule even if in the past" {
        val now = System.currentTimeMillis()
        val pastTask = TaskEntity(
            title = "Past manual task",
            scheduledDate = now - (24 * 3600_000L),
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = false
        )

        AutoSchedulingContracts.hasManualScheduleData(pastTask) shouldBe true
        AutoSchedulingContracts.isMutableByAutoScheduler(pastTask, now) shouldBe false
    }

    "TaskTimeExtensions effectiveReminderTargetMillis defaults to 9 AM for recurring habit without time" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val habitTask = TaskEntity(
            title = "Morning habit",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledTime = null,
            deadlineTime = null
        )

        val target = habitTask.effectiveReminderTargetMillis()
        target shouldBe (todayStart + 9 * 3600_000L)
    }

    "pre-peak morning hours maintain high energy baseline without zeroing" {
        val morningDetection = PeakEnergyEngine.DetectionResult(
            chronotype = MEQChronotypeDetector.Chronotype.MODERATE_MORNING,
            wakeUpHour = 7,
            peakOffsetHours = 2.5f,
            peakHourOfDay = 9,
            peakMinuteOfDay = 570, // 9:30 AM
            peakValue = PeakEnergyEngine.PEAK_VALUE,
            confidence = 1.0f
        )

        // At 7:00 AM (wake time), value must be 80% baseline (3200), not zero
        val wakeVal = morningDetection.currentValueAt(millisAtMinuteOfDay(420))
        wakeVal shouldBe 3200

        // At 8:15 AM (halfway to peak), value must be 90% (3600)
        val midRampVal = morningDetection.currentValueAt(millisAtMinuteOfDay(495))
        midRampVal shouldBe 3600

        // At 9:30 AM (peak), value must be 100% (4000)
        val peakVal = morningDetection.currentValueAt(millisAtMinuteOfDay(570))
        peakVal shouldBe 4000

        // In EnergyScoreEngine at 7:00 AM for rested user, usable energy must remain high (> 60%)
        val snapshot = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = morningDetection,
            sleepPressurePoints = 0,
            nowMillis = millisAtMinuteOfDay(420)
        )
        val scoreResult = EnergyScoreEngine.calculateDetailed(snapshot)
        (scoreResult.usableEnergy >= 65f) shouldBe true
        (scoreResult.peakScore >= 60f) shouldBe true
    }

    "circadian peakWindow wraps smoothly across midnight for evening chronotypes" {
        val eveningProfile = PeakEnergyEngine.CircadianProfile(
            windows = listOf(
                PeakEnergyEngine.PeakWindow(
                    startMinuteOffset = 1320, // 22:00
                    durationMinutes = 240,    // 4 hours (ends at 02:00 AM)
                    amplitude = 1.0f
                )
            )
        )

        // At 23:00 (60 minutes past window start)
        val preMidnight = EnergyScoreEngine.circadianFactor(1380, eveningProfile)
        (preMidnight > 0.4f) shouldBe true

        // At 00:00 midnight (120 minutes past window start, apex of cosine)
        val midnight = EnergyScoreEngine.circadianFactor(0, eveningProfile)
        (midnight >= 0.99f) shouldBe true

        // At 00:30 AM (150 minutes past window start, across midnight boundary)
        val postMidnight = EnergyScoreEngine.circadianFactor(30, eveningProfile)
        (postMidnight > 0.7f) shouldBe true

        // At 03:00 AM (outside window)
        val outsideWindow = EnergyScoreEngine.circadianFactor(180, eveningProfile)
        outsideWindow shouldBe 0.0f
    }

    "cold start statistical confidence does not penalize physical energy magnitude" {
        val lowConfidenceDetection = detectionResult(confidence = 0.20f)
        val snapshot = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = lowConfidenceDetection,
            sleepPressurePoints = 0,
            nowMillis = millisAtMinuteOfDay(105)
        )

        val result = EnergyScoreEngine.calculateDetailed(snapshot)

        // Confidence should be tracked for telemetry
        result.confidenceFactor shouldBe 0.20f

        // But peakScore should NOT be crushed down by 80%
        (result.peakScore > 80f) shouldBe true
        (result.usableEnergy > 80f) shouldBe true
    }

    "EnergyScoreEngine applies sleep inertia penalty within 30 minutes of waking" {
        val snapshotWaking = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 0,
            nowMillis = millisAtMinuteOfDay(105),
            minutesSinceWake = 0 // Just woke up -> maximum sleep inertia penalty (35%)
        )
        val wakingResult = EnergyScoreEngine.calculateDetailed(snapshotWaking)

        val snapshotRested = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 0,
            nowMillis = millisAtMinuteOfDay(105),
            minutesSinceWake = 60 // Fully awake, inertia expired
        )
        val restedResult = EnergyScoreEngine.calculateDetailed(snapshotRested)

        // Usable energy right at waking should be depressed by sleep inertia grogginess
        (wakingResult.fatiguePenalty > restedResult.fatiguePenalty) shouldBe true
        (wakingResult.usableEnergy < restedResult.usableEnergy) shouldBe true
        closeTo(wakingResult.fatiguePenalty, 35.0f, tolerance = 0.01f)
    }

    "EnergyScoreEngine compounds fatigue penalty under chronic sleep restriction (<6h)" {
        val snapshotChronicDebt = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 1000,
            nowMillis = millisAtMinuteOfDay(105),
            minutesSinceWake = 120,
            rollingAverageSleepMinutes = 300f // 5 hours/night (<6h threshold)
        )
        val chronicResult = EnergyScoreEngine.calculateDetailed(snapshotChronicDebt)

        val snapshotNormalSleep = EnergyScoreEngine.EnergySnapshot(
            peakEnergy = detectionResult(confidence = 1.0f),
            sleepPressurePoints = 1000,
            nowMillis = millisAtMinuteOfDay(105),
            minutesSinceWake = 120,
            rollingAverageSleepMinutes = 480f // 8 hours/night (optimal)
        )
        val normalResult = EnergyScoreEngine.calculateDetailed(snapshotNormalSleep)

        // Chronic debt compounds the fatigue penalty
        (chronicResult.fatiguePenalty > normalResult.fatiguePenalty) shouldBe true
        (chronicResult.usableEnergy < normalResult.usableEnergy) shouldBe true
    }
})

private fun closeTo(actual: Float, expected: Float, tolerance: Float = 0.001f) {
    (abs(actual - expected) <= tolerance) shouldBe true
}

private fun detectionResult(confidence: Float = 1.0f): PeakEnergyEngine.DetectionResult {
    return PeakEnergyEngine.DetectionResult(
        chronotype = MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
        wakeUpHour = 6,
        peakOffsetHours = 0f,
        peakHourOfDay = 6,
        peakMinuteOfDay = 0,
        peakValue = PeakEnergyEngine.PEAK_VALUE,
        confidence = confidence
    )
}

private fun millisAtMinuteOfDay(minuteOfDay: Int): Long {
    val safeMinuteOfDay = ((minuteOfDay % 1440) + 1440) % 1440
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, safeMinuteOfDay / 60)
        set(Calendar.MINUTE, safeMinuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
