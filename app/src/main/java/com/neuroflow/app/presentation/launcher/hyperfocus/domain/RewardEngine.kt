package com.neuroflow.app.presentation.launcher.hyperfocus.domain

import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusPreferences

object RewardEngine {

    fun computeTier(completedSinceActivation: Int, totalTarget: Int): RewardTier {
        return when {
            completedSinceActivation >= totalTarget -> RewardTier.FULL
            completedSinceActivation >= 5 -> RewardTier.EARNED
            completedSinceActivation >= 3 -> RewardTier.PARTIAL
            completedSinceActivation >= 1 -> RewardTier.MICRO
            else -> RewardTier.NONE
        }
    }

    fun tasksToNextTier(completed: Int, target: Int): Int {
        return when {
            completed >= target -> 0
            completed >= 5 -> target - completed
            completed >= 3 -> 5 - completed
            completed >= 1 -> 3 - completed
            else -> 1
        }
    }

    fun isUnlockActive(prefs: HyperFocusPreferences): Boolean {
        return prefs.activeUnlockExpiresAt != null && prefs.activeUnlockExpiresAt > System.currentTimeMillis()
    }

    fun secondsRemaining(prefs: HyperFocusPreferences): Long? {
        if (!isUnlockActive(prefs)) return null
        return (prefs.activeUnlockExpiresAt!! - System.currentTimeMillis()) / 1000
    }
}
