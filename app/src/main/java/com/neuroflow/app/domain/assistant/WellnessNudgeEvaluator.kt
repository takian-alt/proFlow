package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.service.EnergyPhase

object WellnessNudgeEvaluator {

    private const val DISMISSAL_COOLDOWN_MS = 45 * 60_000L

    fun evaluateWellnessNudge(
        prefs: UserPreferences,
        usableEnergy: Int,
        phase: EnergyPhase,
        isCrashWindow: Boolean,
        continuousFocusMinutes: Int,
        nowMillis: Long,
        isFocusSessionActive: Boolean
    ): WellnessNudgeProposal? {
        if (!prefs.wellnessNudgesEnabled || isFocusSessionActive) return null

        val inDismissCooldown = prefs.lastWellnessNudgeDismissedAtMillis > 0L &&
            (nowMillis - prefs.lastWellnessNudgeDismissedAtMillis) in 0 until DISMISSAL_COOLDOWN_MS
        if (inDismissCooldown) return null

        // 1. Long unbroken focus stretch -> suggest water & 5-min reset
        val breakThreshold = prefs.focusBreakThresholdMinutes.coerceIn(25, 120)
        if (continuousFocusMinutes >= breakThreshold) {
            return WellnessNudgeProposal(
                type = WellnessNudgeType.LONG_FOCUS_BREAK,
                title = "Time for a quick reset",
                message = "You've stayed focused for ${continuousFocusMinutes}m straight. Grab a glass of water and rest your eyes for 5 minutes to stay sharp.",
                primaryActionLabel = "Drank Water ✓",
                secondaryActionLabel = "Later (45m)",
                emoji = "💧"
            )
        }

        // 2. Crash or deep energy dip -> suggest Easy Win or short rest
        if (isCrashWindow || phase == EnergyPhase.CRASH || phase == EnergyPhase.RECOVERY_NEEDED) {
            return WellnessNudgeProposal(
                type = WellnessNudgeType.CRASH_EASY_WIN,
                title = "Energy dip detected ($usableEnergy%)",
                message = "Your battery is running low right now. Drink some water, take a 5-minute breather, or switch to a quick Easy Win.",
                primaryActionLabel = "Easy Win",
                secondaryActionLabel = "Drank Water ✓",
                emoji = "🔋"
            )
        }

        // 3. Low usable energy -> gentle hydration & rest suggestion
        if (usableEnergy < 35) {
            return WellnessNudgeProposal(
                type = WellnessNudgeType.LOW_ENERGY_REST,
                title = "Low battery check ($usableEnergy%)",
                message = "A quick stretch, water break, or 10-minute pause now will pay back double in focus later.",
                primaryActionLabel = "Drank Water ✓",
                secondaryActionLabel = "Easy Win",
                emoji = "🌿"
            )
        }

        // 4. Periodic hydration check
        val hydrationIntervalMs = prefs.hydrationIntervalMinutes.coerceIn(30, 180) * 60_000L
        val lastHydration = prefs.lastHydrationLoggedAtMillis
        val hydrationOverdue = if (lastHydration <= 0L) {
            // Only prompt if user has been active at least 15m or has some focus today
            continuousFocusMinutes >= 15
        } else {
            (nowMillis - lastHydration) >= hydrationIntervalMs
        }
        if (hydrationOverdue) {
            return WellnessNudgeProposal(
                type = WellnessNudgeType.HYDRATION,
                title = "Hydration check",
                message = "Even mild dehydration drops focus and lifts fatigue. Have you had water recently?",
                primaryActionLabel = "Drank Water ✓",
                secondaryActionLabel = "Remind Later",
                emoji = "💧"
            )
        }

        return null
    }
}
