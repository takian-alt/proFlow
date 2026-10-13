package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskCategory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PlanningCalibrationTest : StringSpec({

    "thin history leaves the plan untouched" {
        val fourSamples = (1..4).map { outcome(TaskCategory.HARD_WORK, 30, 60f) }

        AnalyticsEngine.PlanningCalibration.from(fourSamples) shouldBe
            AnalyticsEngine.PlanningCalibration.UNIFORM
    }

    "consistent under-estimation learns a factor above one" {
        val calibration = AnalyticsEngine.PlanningCalibration.from(
            (1..8).map { outcome(TaskCategory.HARD_WORK, 30, 45f) }
        )

        // geometric mean 1.5 shrunk by 8/(8+4) toward 1.0
        closeTo(calibration.globalFactor, 1.333f) shouldBe true
    }

    "consistent over-estimation learns the other direction" {
        val calibration = AnalyticsEngine.PlanningCalibration.from(
            (1..8).map { outcome(TaskCategory.HARD_WORK, 30, 20f) }
        )

        // raw 0.67 blended by 8/(8+4) then held at the lower bound
        closeTo(calibration.globalFactor, 0.8f) shouldBe true
        (calibration.globalFactor < 1.0f) shouldBe true
    }

    "a learned bias is bounded however bad the history is" {
        val bloated = AnalyticsEngine.PlanningCalibration.from(
            (1..12).map { outcome(TaskCategory.HARD_WORK, 10, 300f) }
        )
        val padded = AnalyticsEngine.PlanningCalibration.from(
            (1..12).map { outcome(TaskCategory.HARD_WORK, 300, 1f) }
        )

        closeTo(bloated.globalFactor, 1.6f) shouldBe true
        closeTo(padded.globalFactor, 0.8f) shouldBe true
    }

    "each category gets its own factor instead of one number for everything" {
        val calibration = AnalyticsEngine.PlanningCalibration.from(
            (1..6).map { outcome(TaskCategory.HARD_WORK, 30, 60f) } +
                (1..6).map { outcome(TaskCategory.ROUTINE, 30, 30f) }
        )

        closeTo(calibration.globalFactor, 1.311f) shouldBe true
        closeTo(calibration.categoryFactor(TaskCategory.HARD_WORK) ?: 0f, 1.6f) shouldBe true
        closeTo(calibration.categoryFactor(TaskCategory.ROUTINE) ?: 0f, 1.124f) shouldBe true
    }

    "a category without history gets no factor at all" {
        val calibration = AnalyticsEngine.PlanningCalibration.from(
            (1..8).map { outcome(TaskCategory.HARD_WORK, 30, 60f) }
        )

        calibration.categoryFactor(TaskCategory.CREATIVE) shouldBe null
    }

    "a sparse category blends toward the user's overall bias instead of trusting its own two samples" {
        val calibration = AnalyticsEngine.PlanningCalibration.from(
            (1..12).map { outcome(TaskCategory.HARD_WORK, 30, 30f) } +
                (1..2).map { outcome(TaskCategory.ROUTINE, 30, 60f) }
        )
        val routine = calibration.categoryFactor(TaskCategory.ROUTINE) ?: 0f

        // raw 2.0, user global ~1.08, blended with weight 2/(2+4)
        closeTo(calibration.globalFactor, 1.081f) shouldBe true
        closeTo(routine, 1.387f) shouldBe true
        (routine > calibration.globalFactor) shouldBe true
    }
})

private fun outcome(
    category: TaskCategory,
    estimated: Int,
    actual: Float
): TaskEntity = TaskEntity(
    title = "Calibration sample",
    status = TaskStatus.COMPLETED,
    estimatedDurationMinutes = estimated,
    actualDurationMinutes = actual,
    schedulingCategory = category.name
)

private fun closeTo(actual: Float, expected: Float, tolerance: Float = 0.02f): Boolean =
    kotlin.math.abs(actual - expected) <= tolerance
