package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

object RewardEngine {

    private data class TierThresholds(
        val micro: Int?,
        val partial: Int?,
        val earned: Int?
    )

    private fun computeThresholds(totalTarget: Int): TierThresholds {
        val target = totalTarget.coerceAtLeast(1)
        val maxIntermediate = target - 1
        if (maxIntermediate <= 0) {
            return TierThresholds(micro = null, partial = null, earned = null)
        }

        val microBase = (target * 0.25f).toInt().coerceAtLeast(1)
        val micro = microBase.coerceAtMost(maxIntermediate)

        val partialBase = (target * 0.50f).toInt().coerceAtLeast(1)
        val partialCandidate = partialBase.coerceAtLeast(micro + 1)
        val partial = partialCandidate.takeIf { it <= maxIntermediate }

        val earnedBase = (target * 0.75f).toInt().coerceAtLeast(1)
        val earnedMin = (partial ?: micro) + 1
        val earnedCandidate = earnedBase.coerceAtLeast(earnedMin)
        val earned = earnedCandidate.takeIf { it <= maxIntermediate }

        return TierThresholds(micro = micro, partial = partial, earned = earned)
    }

    fun isTierReachable(tier: RewardTier, totalTarget: Int, emergencyUsed: Boolean = false): Boolean {
        if (tier == RewardTier.NONE) return false
        if (tier == RewardTier.FULL) return true
        if (emergencyUsed) return false

        val thresholds = computeThresholds(totalTarget)
        return when (tier) {
            RewardTier.MICRO -> thresholds.micro != null
            RewardTier.PARTIAL -> thresholds.partial != null
            RewardTier.EARNED -> thresholds.earned != null
            else -> false
        }
    }

    fun isTierEarned(
        tier: RewardTier,
        completedSinceActivation: Int,
        totalTarget: Int,
        emergencyUsed: Boolean = false
    ): Boolean {
        val completed = completedSinceActivation.coerceAtLeast(0)
        val target = totalTarget.coerceAtLeast(1)

        if (tier == RewardTier.FULL) return completed >= target
        if (!isTierReachable(tier, totalTarget, emergencyUsed)) return false

        val thresholds = computeThresholds(totalTarget)
        return when (tier) {
            RewardTier.MICRO -> completed >= (thresholds.micro ?: Int.MAX_VALUE)
            RewardTier.PARTIAL -> completed >= (thresholds.partial ?: Int.MAX_VALUE)
            RewardTier.EARNED -> completed >= (thresholds.earned ?: Int.MAX_VALUE)
            else -> false
        }
    }

    fun computeTier(completedSinceActivation: Int, totalTarget: Int, emergencyUsed: Boolean = false): RewardTier {
        if (completedSinceActivation <= 0) return RewardTier.NONE
        val target = totalTarget.coerceAtLeast(1)
        if (completedSinceActivation >= target) return RewardTier.FULL
        if (emergencyUsed) return RewardTier.NONE // Skips intermediate rewards if emergency used

        val thresholds = computeThresholds(target)
        return when {
            thresholds.earned != null && completedSinceActivation >= thresholds.earned -> RewardTier.EARNED
            thresholds.partial != null && completedSinceActivation >= thresholds.partial -> RewardTier.PARTIAL
            thresholds.micro != null && completedSinceActivation >= thresholds.micro -> RewardTier.MICRO
            else -> RewardTier.NONE
        }
    }

    fun tasksToNextTier(completed: Int, target: Int, emergencyUsed: Boolean = false): Int {
        if (completed <= 0 && !emergencyUsed) return 1
        val t = target.coerceAtLeast(1)
        if (completed >= t) return 0
        if (emergencyUsed) return t - completed // Must complete all remaining tasks

        val thresholds = computeThresholds(t)

        val nextThreshold = when {
            thresholds.micro != null && completed < thresholds.micro -> thresholds.micro
            thresholds.partial != null && completed < thresholds.partial -> thresholds.partial
            thresholds.earned != null && completed < thresholds.earned -> thresholds.earned
            else -> t
        }

        return (nextThreshold - completed).coerceAtLeast(0)
    }

    fun isPassActive(prefs: HyperFocusPreferences, nowMillis: Long): Boolean =
        prefs.activePassPackage != null && (prefs.activePassExpiresAt ?: 0L) > nowMillis

    /**
     * E1: Leaving the granted app ends the pass for that app, but the budget deadline
     * in DataStore keeps ticking until it runs out.
     */
    fun endsPassOnForeground(
        packageName: String,
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        nowMillis: Long
    ): Boolean {
        val granted = prefs.activePassPackage ?: return false
        val deadline = prefs.activePassExpiresAt ?: return false
        if (deadline <= nowMillis) return false
        if (packageName == granted) return false
        if (packageName in systemExempt) return false
        if (packageName == com.neuroflow.app.BuildConfig.APPLICATION_ID) return false
        return true
    }

    fun passSecondsRemaining(prefs: HyperFocusPreferences, nowMillis: Long): Long? {
        if (!isPassActive(prefs, nowMillis)) return null
        return ((prefs.activePassExpiresAt ?: return null) - nowMillis) / 1000
    }

    /** E1: a live budget is inherited, not extended; only a spent or absent budget restarts the clock. */
    fun passDeadline(existingExpiresAt: Long?, nowMillis: Long, durationMinutes: Int): Long =
        existingExpiresAt?.takeIf { it > nowMillis }
            ?: nowMillis + durationMinutes.toLong() * 60_000L
}
