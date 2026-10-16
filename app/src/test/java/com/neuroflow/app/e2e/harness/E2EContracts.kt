package com.neuroflow.app.e2e.harness

import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskType

/**
 * Interface contracts defined in PROJECT.md and TEST_INFRA.md for neuroFlow E2E test suite.
 */

data class E2ELiveEnergyContext(
    val usableEnergy: Float,       // 0f .. 100f
    val reservoirFactor: Float,    // 0f .. 1f
    val circadianFactor: Float,    // 0f .. 1f
    val sleepPressureRatio: Float, // 0f .. 1f
    val confidence: Float          // 0f .. 1f (statistical weight, does not zero magnitude)
)

data class E2EInferredTaskMetadata(
    val sanitizedTitle: String,
    val durationMinutes: Int,
    val energyDemand: EnergyLevel,
    val taskType: TaskType,
    val contextTags: List<String>,
    val isFrog: Boolean,
    val scheduledDate: Long? = null
)

enum class E2EDeferralReasonCode {
    INSUFFICIENT_TIME,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    DEADLINE_CONFLICT
}

data class E2EPlacementRationale(
    val taskId: String,
    val scheduledSlotStart: Long,
    val scheduledSlotEnd: Long,
    val energyFitScore: Float,
    val primaryReason: String
)

data class E2EDeferralRationale(
    val taskId: String,
    val reasonCode: E2EDeferralReasonCode,
    val explanation: String,
    val actionableSuggestion: String
)

data class E2ESchedulingReport(
    val placedCount: Int,
    val deferredCount: Int,
    val placementRationales: List<E2EPlacementRationale>,
    val deferrals: List<E2EDeferralRationale>,
    val dailyCognitiveLoadUtilized: Float
)

enum class E2EFocusInterventionTier {
    ZEN,        // Mindful pause, breathing prompt
    SHIELD,     // Cognitive gate, deliberate friction & reflection
    FORTRESS    // Strict boundary, intentional barrier
}

data class E2EFocusIntervention(
    val tier: E2EFocusInterventionTier,
    val heading: String,
    val message: String,
    val primaryActionLabel: String,
    val dismissActionLabel: String
)
