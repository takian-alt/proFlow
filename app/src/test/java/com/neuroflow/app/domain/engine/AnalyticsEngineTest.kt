package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class AnalyticsEngineTest : StringSpec({

    "computeMape returns correct value" {
        AnalyticsEngine.computeMape(estimated = 60f, actual = 90f) shouldBe (50f plusOrMinus 0.1f)
    }

    "computeMape returns 0 when estimated is 0" {
        AnalyticsEngine.computeMape(0f, 50f) shouldBe 0f
    }

    "computeSmape returns correct value" {
        // |90-60| / ((60+90)/2) * 100 = 30/75 * 100 = 40%
        AnalyticsEngine.computeSmape(estimated = 60f, actual = 90f) shouldBe (40f plusOrMinus 0.1f)
    }

    "computeSmape returns 0 when both are 0" {
        AnalyticsEngine.computeSmape(0f, 0f) shouldBe 0f
    }

    "computeWeightedMape weights by estimated minutes" {
        val tasks = listOf(
            TaskEntity(
                title = "Task 1",
                estimatedDurationMinutes = 60,
                actualDurationMinutes = 90f,
                status = TaskStatus.COMPLETED
            ),
            TaskEntity(
                title = "Task 2",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = 30f,
                status = TaskStatus.COMPLETED
            )
        )

        // Task1: MAPE 50% weight 60; Task2: MAPE 0% weight 30 -> 3000/90
        AnalyticsEngine.computeWeightedMape(tasks) shouldBe (33.33f plusOrMinus 0.5f)
    }

    "computeWeightedSmape ignores tasks without an actual duration" {
        val tasks = listOf(
            TaskEntity(title = "Tracked", estimatedDurationMinutes = 60, actualDurationMinutes = 90f),
            TaskEntity(title = "Untracked", estimatedDurationMinutes = 120, actualDurationMinutes = null)
        )

        AnalyticsEngine.computeWeightedSmape(tasks) shouldBe (40f plusOrMinus 0.1f)
    }

    "mapeGrade returns correct labels" {
        AnalyticsEngine.mapeGrade(0f) shouldBe
            "No data yet — complete tasks with time tracking to see your accuracy"
        AnalyticsEngine.mapeGrade(5f) shouldBe "✨ Excellent time estimation!"
        AnalyticsEngine.mapeGrade(15f) shouldBe "👍 Good estimation, minor drift"
        AnalyticsEngine.mapeGrade(35f) shouldBe "⚠ Moderate estimation error — try breaking tasks into smaller chunks"
        AnalyticsEngine.mapeGrade(75f) shouldBe "🚨 High estimation error — recalibrate your estimates"
    }

    "computeWeightedMape with no valid tasks returns 0" {
        AnalyticsEngine.computeWeightedMape(listOf(TaskEntity(title = "No estimate", estimatedDurationMinutes = 0))) shouldBe 0f
    }
})
