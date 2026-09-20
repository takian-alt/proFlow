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
