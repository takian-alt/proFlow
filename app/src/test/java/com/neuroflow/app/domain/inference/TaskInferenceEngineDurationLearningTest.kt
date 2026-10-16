package com.neuroflow.app.domain.inference

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt

class TaskInferenceEngineDurationLearningTest : StringSpec({

    fun completedTask(id: String, estimatedMinutes: Int, actualMinutes: Float) = TaskEntity(
        id = id,
        title = "Completed $id",
        taskType = TaskType.ANALYTICAL,
        energyLevel = EnergyLevel.HIGH,
        priority = Priority.MEDIUM,
        quadrant = Quadrant.SCHEDULE,
        status = TaskStatus.COMPLETED,
        estimatedDurationMinutes = estimatedMinutes,
        actualDurationMinutes = actualMinutes
    )

    "calculateCalibrationFactorFromHistory: returns 1.0f for empty history or tasks without duration" {
        TaskInferenceEngine.calculateCalibrationFactorFromHistory(emptyList()) shouldBe 1.0f

        val activeTask = TaskEntity(
            id = "t1",
            title = "Active task",
            taskType = TaskType.ADMIN,
            status = TaskStatus.ACTIVE,
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 45f
        )
        TaskInferenceEngine.calculateCalibrationFactorFromHistory(listOf(activeTask)) shouldBe 1.0f
    }

    "calculateCalibrationFactorFromHistory: underestimates (actual > estimated) produce factor > 1.0" {
        val history = listOf(
            completedTask("1", 30, 45f), // 1.5x
            completedTask("2", 60, 90f), // 1.5x
            completedTask("3", 40, 60f)  // 1.5x
        )
        val factor = TaskInferenceEngine.calculateCalibrationFactorFromHistory(history)
        factor shouldBeGreaterThan 1.0f
        factor shouldBeLessThan 1.51f
    }

    "calculateCalibrationFactorFromHistory: overestimates (actual < estimated) produce factor < 1.0" {
        val history = listOf(
            completedTask("1", 60, 30f), // 0.5x
            completedTask("2", 40, 20f), // 0.5x
            completedTask("3", 50, 25f)  // 0.5x
        )
        val factor = TaskInferenceEngine.calculateCalibrationFactorFromHistory(history)
        factor shouldBeLessThan 1.0f
        factor shouldBeGreaterThan 0.49f
    }

    "inferFromInput with completedTasks: adapts duration based on learned calibration factor" {
        // User consistently takes ~1.5x longer
        val history = listOf(
            completedTask("1", 30, 45f),
            completedTask("2", 60, 90f),
            completedTask("3", 40, 60f),
            completedTask("4", 50, 75f)
        )
        val factor = TaskInferenceEngine.calculateCalibrationFactorFromHistory(history)

        val inferred = TaskInferenceEngine.inferFromInput(
            rawInput = "Write design document 60m",
            completedTasks = history
        )

        inferred.rawDurationMinutes shouldBe 60
        inferred.calibrationFactorApplied shouldBe factor
        inferred.durationMinutes shouldBe (60 * factor).roundToInt()
        inferred.durationMinutes shouldBeGreaterThan 60
    }

    "inferFromInput with explicit calibrationFactor: preserves rawDuration and records factor" {
        val inferred = TaskInferenceEngine.inferFromInput(
            rawInput = "Review pull request 30m",
            calibrationFactor = 1.25f
        )

        inferred.rawDurationMinutes shouldBe 30
        inferred.durationMinutes shouldBe 38 // 30 * 1.25 = 37.5 -> 38
        inferred.calibrationFactorApplied shouldBe 1.25f
    }
})
