package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val NOW = 5_000_000L

class PassLifecycleTest : StringSpec({

    "PL-1: a package with no deadline is not an active pass" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "com.instagram.android"), NOW
        ).shouldBeFalse()
    }

    "PL-2: a deadline with no package is not an active pass" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassExpiresAt = NOW + 60_000), NOW
        ).shouldBeFalse()
    }

    "PL-3: both present and in the future is an active pass" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW + 60_000)
        RewardEngine.isPassActive(prefs, NOW).shouldBeTrue()
        RewardEngine.passSecondsRemaining(prefs, NOW) shouldBe 60L
    }

    "PL-4: an expired pass reports inactive and no remaining seconds" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW - 1)
        RewardEngine.isPassActive(prefs, NOW).shouldBeFalse()
        RewardEngine.passSecondsRemaining(prefs, NOW).shouldBeNull()
    }

    "PL-5: at the exact deadline the pass is gone, not pending" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "x", activePassExpiresAt = NOW), NOW
        ).shouldBeFalse()
    }

    "PL-6: FULL tier's Int.MAX_VALUE minutes yield a deadline that never arrives" {
        val minutes = RewardTier.FULL.unlockMinutes
        val deadline = NOW + minutes.toLong() * 60_000L
        deadline shouldBe 5_000_000L + 2147483647L * 60_000L
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "x", activePassExpiresAt = deadline),
            System.currentTimeMillis()
        ).shouldBeTrue()
    }

    "PL-7: clearing the package keeps the budget deadline so E1's clock survives the switch" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW + 300_000)
        val cleared = prefs.copy(activePassPackage = null)
        cleared.activePassExpiresAt shouldBe prefs.activePassExpiresAt
        RewardEngine.isPassActive(cleared, NOW).shouldBeFalse()
    }

    "PL-8: pass validity ignores isActive - the session check belongs to the caller" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(isActive = false, state = HyperFocusState.INACTIVE,
                activePassPackage = "x", activePassExpiresAt = NOW + 60_000), NOW
        ).shouldBeTrue()
    }

    "PL-9: E1 - a live budget is inherited, not extended, and only a spent budget restarts the clock" {
        RewardEngine.passDeadline(NOW + 300_000, NOW, 10) shouldBe NOW + 300_000
        RewardEngine.passDeadline(NOW - 1, NOW, 10) shouldBe NOW + 600_000
        RewardEngine.passDeadline(null, NOW, 10) shouldBe NOW + 600_000
        RewardEngine.passDeadline(NOW + 1, NOW, Int.MAX_VALUE) shouldBe NOW + 1
    }
})
