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

class TaskScoringEngineLiveEnergyTest : StringSpec({

    val prefs = UserPreferences(
        workDayStart = 9,
        workDayEnd = 17,
        peakEnergyStart = 10,
        peakEnergyEnd = 12,
        weightQuadrant = 1.0f,
        weightPriorityLevel = 1.0f
    )

    fun testTask(
        id: String = "task_1",
        energy: EnergyLevel = EnergyLevel.MEDIUM,
        type: TaskType = TaskType.ADMIN
    ): TaskEntity = TaskEntity(
        id = id,
        title = "Test Task $id",
        description = "",
        quadrant = Quadrant.SCHEDULE,
        priority = Priority.MEDIUM,
        status = TaskStatus.ACTIVE,
        energyLevel = energy,
        taskType = type
    )

    "high live energy gives strong bonus to HIGH energy tasks and penalizes LOW energy tasks" {
        val highEnergyContext = TaskScoringEngine.LiveEnergyContext(
            usableEnergy = 85f,
            reservoirFactor = 0.9f,
            circadianFactor = 1.0f
        )

        val highTask = testTask(energy = EnergyLevel.HIGH)
        val medTask = testTask(energy = EnergyLevel.MEDIUM)
        val lowTask = testTask(energy = EnergyLevel.LOW)

        val highTaskScore = TaskScoringEngine.score(highTask, prefs, liveEnergy = highEnergyContext)
        val medTaskScore = TaskScoringEngine.score(medTask, prefs, liveEnergy = highEnergyContext)
        val lowTaskScore = TaskScoringEngine.score(lowTask, prefs, liveEnergy = highEnergyContext)

        // HIGH gets +70f, MED gets +20f, LOW gets -35f -> difference between HIGH and LOW is 105f
        (highTaskScore - lowTaskScore) shouldBe 105f
        (highTaskScore - medTaskScore) shouldBe 50f
    }

    "depleted live energy promotes LOW energy tasks and penalizes HIGH energy tasks" {
        val depletedContext = TaskScoringEngine.LiveEnergyContext(
            usableEnergy = 25f,
            reservoirFactor = 0.2f,
            circadianFactor = 0.4f
        )

        val highTask = testTask(energy = EnergyLevel.HIGH)
        val medTask = testTask(energy = EnergyLevel.MEDIUM)
        val lowTask = testTask(energy = EnergyLevel.LOW)

        val highTaskScore = TaskScoringEngine.score(highTask, prefs, liveEnergy = depletedContext)
        val medTaskScore = TaskScoringEngine.score(medTask, prefs, liveEnergy = depletedContext)
        val lowTaskScore = TaskScoringEngine.score(lowTask, prefs, liveEnergy = depletedContext)

        // When depleted: LOW gets +70f, MED gets +10f, HIGH gets -35f -> LOW beats HIGH by 105f
        (lowTaskScore - highTaskScore) shouldBe 105f
        (lowTaskScore - medTaskScore) shouldBe 60f
    }

    "moderate live energy provides balanced load matching" {
        val moderateContext = TaskScoringEngine.LiveEnergyContext(
            usableEnergy = 55f,
            reservoirFactor = 0.6f,
            circadianFactor = 0.7f
        )

        val highTask = testTask(energy = EnergyLevel.HIGH)
        val medTask = testTask(energy = EnergyLevel.MEDIUM)
        val lowTask = testTask(energy = EnergyLevel.LOW)

        val highTaskScore = TaskScoringEngine.score(highTask, prefs, liveEnergy = moderateContext)
        val medTaskScore = TaskScoringEngine.score(medTask, prefs, liveEnergy = moderateContext)
        val lowTaskScore = TaskScoringEngine.score(lowTask, prefs, liveEnergy = moderateContext)

        // Moderate: MED gets +30f, LOW gets +15f, HIGH gets +10f
        (medTaskScore - highTaskScore) shouldBe 20f
        (medTaskScore - lowTaskScore) shouldBe 15f
    }

    "live energy modulates cognitive analytical task bonuses" {
        val highEnergyContext = TaskScoringEngine.LiveEnergyContext(usableEnergy = 80f)
        val lowEnergyContext = TaskScoringEngine.LiveEnergyContext(usableEnergy = 30f)

        val analyticalTask = testTask(energy = EnergyLevel.MEDIUM, type = TaskType.ANALYTICAL)

        val highEnergyScore = TaskScoringEngine.score(analyticalTask, prefs, liveEnergy = highEnergyContext)
        val lowEnergyScore = TaskScoringEngine.score(analyticalTask, prefs, liveEnergy = lowEnergyContext)

        // Analytical bonus difference: +60f (high) vs -25f (low) = 85f, plus energyBonus difference (+20f vs +10f = +10f) -> total diff 95f
        (highEnergyScore - lowEnergyScore) shouldBe 95f
    }

    "live energy passed via ScoringContext automatically flows into score" {
        val depletedContext = TaskScoringEngine.LiveEnergyContext(usableEnergy = 20f)
        val lowTask = testTask(id = "low_1", energy = EnergyLevel.LOW)
        val highTask = testTask(id = "high_1", energy = EnergyLevel.HIGH)

        val context = TaskScoringEngine.ScoringContext.from(listOf(lowTask, highTask), liveEnergy = depletedContext)

        val lowScore = TaskScoringEngine.score(lowTask, prefs, context = context)
        val highScore = TaskScoringEngine.score(highTask, prefs, context = context)

        (lowScore > highScore) shouldBe true
        (lowScore - highScore) shouldBe 105f
    }

    "sortedByScore ranks low-effort and low-energy tasks first when user is exhausted" {
        val depletedContext = TaskScoringEngine.LiveEnergyContext(usableEnergy = 15f)
        val tasks = listOf(
            testTask(id = "high_task", energy = EnergyLevel.HIGH),
            testTask(id = "low_task", energy = EnergyLevel.LOW)
        )

        val sorted = TaskScoringEngine.sortedByScore(tasks, prefs, liveEnergy = depletedContext)
        sorted.first().id shouldBe "low_task"
    }

    "scoreBreakdown reflects live energy matching explanation" {
        val highEnergyContext = TaskScoringEngine.LiveEnergyContext(usableEnergy = 90f)
        val highTask = testTask(energy = EnergyLevel.HIGH)

        val breakdown = TaskScoringEngine.scoreBreakdown(highTask, prefs, liveEnergy = highEnergyContext)
        val energyEntry = breakdown.find { it.first == "Energy matching" }

        energyEntry shouldBe ("Energy matching" to 70f)
    }
})
