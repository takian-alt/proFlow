package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.inference.TaskInferenceEngine
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTaskInput(
    val cleanTitle: String,
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean,
    val energyDemand: EnergyDemand,
    val category: TaskCategory,
    val isFrog: Boolean
)

interface TaskNaturalLanguageParser {
    fun parse(rawInput: String): ParsedTaskInput
}

@Singleton
class DefaultTaskNaturalLanguageParser @Inject constructor() : TaskNaturalLanguageParser {

    override fun parse(rawInput: String): ParsedTaskInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedTaskInput(
                cleanTitle = "",
                estimatedMinutes = 25,
                isDurationExplicit = false,
                energyDemand = EnergyDemand.MODERATE,
                category = TaskCategory.GENERAL,
                isFrog = false
            )
        }

        val inferred = TaskInferenceEngine.inferFromInput(trimmed)
        val demand = when (inferred.energyDemand) {
            com.neuroflow.app.domain.model.EnergyLevel.HIGH -> EnergyDemand.HIGH
            com.neuroflow.app.domain.model.EnergyLevel.LOW -> EnergyDemand.LOW
            com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
        }
        val category = when (inferred.taskType) {
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
            com.neuroflow.app.domain.model.TaskType.CREATIVE -> TaskCategory.CREATIVE
            com.neuroflow.app.domain.model.TaskType.ADMIN -> TaskCategory.ADMIN
            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> TaskCategory.EXERCISE
            com.neuroflow.app.domain.model.TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
            com.neuroflow.app.domain.model.TaskType.GENERAL -> TaskCategory.GENERAL
        }

        return ParsedTaskInput(
            cleanTitle = inferred.sanitizedTitle.ifBlank { trimmed },
            estimatedMinutes = inferred.durationMinutes.coerceAtLeast(5),
            isDurationExplicit = inferred.isDurationExplicit,
            energyDemand = demand,
            category = category,
            isFrog = inferred.isFrog
        )
    }
}
