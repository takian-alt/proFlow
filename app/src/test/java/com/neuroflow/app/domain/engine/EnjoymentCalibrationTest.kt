package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnjoymentCalibrationTest : StringSpec({

    fun rated(rating: Float, status: TaskStatus = TaskStatus.COMPLETED) = TaskEntity(
        title = "t",
        status = status,
        affectiveForecastError = rating
    )

    fun unrated() = TaskEntity(title = "t", status = TaskStatus.COMPLETED)

    "fewer than five ratings leave a forecast untouched" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(List(4) { rated(-2f) } + unrated())
        calibration shouldBe AnalyticsEngine.EnjoymentCalibration.NEUTRAL
        calibration.forecast(60) shouldBe 60
    }

    "a consistent worse-than-expected bias lowers later forecasts" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(List(8) { rated(-1f) })
        // mean -1 of a -2..2 scale, 10 points per unit, shrunk by 8/(8+4)
        calibration.forecast(60) shouldBe 53
    }

    "a consistent better-than-expected bias raises later forecasts" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(List(8) { rated(1f) })
        calibration.forecast(60) shouldBe 67
    }

    "the correction is capped however strong the history is" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(List(20) { rated(-2f) })
        calibration.forecast(60) shouldBe 45
    }

    "a corrected forecast never leaves the rating range" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(List(20) { rated(-2f) })
        calibration.forecast(5) shouldBe 0
        AnalyticsEngine.EnjoymentCalibration.from(List(20) { rated(2f) }).forecast(97) shouldBe 100
    }

    "only finished tasks contribute their rating" {
        val calibration = AnalyticsEngine.EnjoymentCalibration.from(
            List(8) { rated(-1f) } + List(5) { rated(-2f, TaskStatus.ACTIVE) }
        )
        calibration.forecast(60) shouldBe 53
    }

    "scoring reads the stored bias instead of the raw forecast" {
        val task = TaskEntity(
            id = "enjoy-1",
            title = "Sparkling",
            enjoymentScore = 80,
            effortScore = 80,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L
        )
        val noHistory = com.neuroflow.app.data.local.UserPreferences(enjoymentForecastBias = 0f)
        val overestimates = com.neuroflow.app.data.local.UserPreferences(enjoymentForecastBias = -15f)

        // 80 minus the measured 15 points reads as 65 — no longer the "enjoyable hard work" band.
        val optimistic = TaskScoringEngine.displayScore(task, noHistory)
        val corrected = TaskScoringEngine.displayScore(task, overestimates)
        (corrected < optimistic) shouldBe true
    }
})
