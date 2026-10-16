package com.neuroflow.app.presentation.focus

enum class FocusInterventionTier {
    ZEN,        // Mindful pause, breathing prompt
    SHIELD,     // Cognitive gate, deliberate friction & reflection
    FORTRESS    // Strict boundary, intentional barrier
}

data class FocusIntervention(
    val tier: FocusInterventionTier,
    val heading: String,
    val message: String,
    val primaryActionLabel: String,
    val dismissActionLabel: String
)

object FocusInterventionManager {
    fun getTierIntervention(tier: FocusInterventionTier): FocusIntervention {
        return when (tier) {
            FocusInterventionTier.ZEN -> FocusIntervention(
                tier = tier,
                heading = "Take a Mindful Breath",
                message = "Notice the urge to switch. Take a slow inhale before deciding.",
                primaryActionLabel = "Return to Focus",
                dismissActionLabel = "Pause Mindfully"
            )
            FocusInterventionTier.SHIELD -> FocusIntervention(
                tier = tier,
                heading = "Reflective Pause",
                message = "You are in a protected focus block. Is this switch truly aligned with your current goal?",
                primaryActionLabel = "Stay Focused",
                dismissActionLabel = "Proceed Consciously"
            )
            FocusInterventionTier.FORTRESS -> FocusIntervention(
                tier = tier,
                heading = "Deep Work Sanctuary Active",
                message = "Strict boundary engaged for high-leverage execution. Take a 10s cooldown before opening emergency bypass.",
                primaryActionLabel = "Protect Deep Work",
                dismissActionLabel = "Emergency Exit (Preserves Streak)"
            )
        }
    }
}
