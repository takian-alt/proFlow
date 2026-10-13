package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.domain.model.RewardTier
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for RewardEngine.
 *
 * Validates: Requirements 2.1, 2.2
 *
 * Tests:
 * - HF-1: Tier is monotonically non-decreasing as completed tasks increase
 * - HF-2: FULL tier iff completed >= target
 * - HF-4: tasksToNextTier returns 0 when at FULL tier
 *
 * Pass validity moved to PassLifecycleTest when the device-wide unlock became a per-app pass.
 */
class RewardEngineTest : StringSpec({

    // HF-1: Tier is monotonically non-decreasing for completed 0..target+5
    "HF-1: tier is monotonically non-decreasing as completed tasks increase" {
        val target = 5
        val tiers = (0..10).map { completed ->
            RewardEngine.computeTier(completed, target)
        }
        for (i in 1 until tiers.size) {
            (tiers[i].ordinal >= tiers[i - 1].ordinal) shouldBe true
        }
    }

    // HF-2: FULL tier iff completed >= target
    "HF-2: FULL tier when completed >= target, not FULL otherwise" {
        val target = 5
        RewardEngine.computeTier(0, target) shouldBe RewardTier.NONE
        RewardEngine.computeTier(1, target) shouldBe RewardTier.MICRO
        RewardEngine.computeTier(2, target) shouldBe RewardTier.PARTIAL
        RewardEngine.computeTier(3, target) shouldBe RewardTier.EARNED
        RewardEngine.computeTier(4, target) shouldBe RewardTier.EARNED
        RewardEngine.computeTier(5, target) shouldBe RewardTier.FULL
        RewardEngine.computeTier(6, target) shouldBe RewardTier.FULL
    }

    // HF-4: tasksToNextTier returns 0 when at FULL tier
    "HF-4: tasksToNextTier returns 0 when completed >= target" {
        RewardEngine.tasksToNextTier(5, 5) shouldBe 0
        RewardEngine.tasksToNextTier(6, 5) shouldBe 0
        RewardEngine.tasksToNextTier(10, 5) shouldBe 0
    }

    "computeTier returns NONE for 0 completed" {
        RewardEngine.computeTier(0, 10) shouldBe RewardTier.NONE
    }

    "computeTier returns MICRO for 1 completed (target=10)" {
        RewardEngine.computeTier(1, 10) shouldBe RewardTier.NONE
    }

    "computeTier returns PARTIAL for 3 completed (target=10)" {
        RewardEngine.computeTier(3, 10) shouldBe RewardTier.MICRO
    }

    "computeTier returns EARNED for 5 completed (target=10)" {
        RewardEngine.computeTier(5, 10) shouldBe RewardTier.PARTIAL
    }
})
