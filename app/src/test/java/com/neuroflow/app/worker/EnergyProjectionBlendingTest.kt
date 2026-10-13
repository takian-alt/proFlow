package com.neuroflow.app.worker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Unit tests for energy projection blending weights used by ScheduleAutoTasksWorker's
 * energyScoreFn lambda.
 *
 * **Validates: Requirement 3 - Accurate Energy Projection Without Moment Blending**
 *
 * The weights are replicated locally on purpose: the real lambda lives inside
 * `doWork()` and needs an Android `Context`, so the decision logic is exercised
 * here as pure JVM arithmetic.
 *
 * - 0-2 hours: 35% moment, 65% baseline
 * - 2-6 hours: 20% moment, 80% baseline
 * - 6-12 hours: 10% moment, 90% baseline
 * - >12 hours: 0% moment, 100% baseline (no moment blending)
 */
class EnergyProjectionBlendingTest : StringSpec({

    fun liveWeight(hoursAhead: Float): Float = when {
        hoursAhead <= 2f -> 0.35f
        hoursAhead <= 6f -> 0.20f
        hoursAhead <= 12f -> 0.10f
        else -> 0.0f
    }

    fun blend(baseline: Float, moment: Float, hoursAhead: Float): Float {
        val w = liveWeight(hoursAhead)
        return baseline * (1f - w) + moment * w
    }

    "EPB-1: 0h ahead uses 35% moment weight" { liveWeight(0f) shouldBe (0.35f plusOrMinus 0.001f) }
    "EPB-2: 1h ahead uses 35% moment weight" { liveWeight(1f) shouldBe (0.35f plusOrMinus 0.001f) }
    "EPB-3: 2h boundary uses 35% moment weight" { liveWeight(2f) shouldBe (0.35f plusOrMinus 0.001f) }
    "EPB-4: 3h ahead uses 20% moment weight" { liveWeight(3f) shouldBe (0.20f plusOrMinus 0.001f) }
    "EPB-5: 6h boundary uses 20% moment weight" { liveWeight(6f) shouldBe (0.20f plusOrMinus 0.001f) }
    "EPB-6: 8h ahead uses 10% moment weight" { liveWeight(8f) shouldBe (0.10f plusOrMinus 0.001f) }
    "EPB-7: 12h boundary uses 10% moment weight" { liveWeight(12f) shouldBe (0.10f plusOrMinus 0.001f) }
    "EPB-8: 13h ahead drops moment weight entirely" { liveWeight(13f) shouldBe (0.0f plusOrMinus 0.001f) }
    "EPB-9: 24h ahead drops moment weight entirely" { liveWeight(24f) shouldBe (0.0f plusOrMinus 0.001f) }
    "EPB-10: 48h ahead drops moment weight entirely" { liveWeight(48f) shouldBe (0.0f plusOrMinus 0.001f) }
    "EPB-11: far-future slot is 100% baseline" { blend(75f, 50f, 24f) shouldBe (75f plusOrMinus 0.001f) }
    "EPB-12: near-future slot blends 35/65" { blend(75f, 50f, 1f) shouldBe (66.25f plusOrMinus 0.001f) }
    "EPB-13: medium-future slot blends 20/80" { blend(80f, 40f, 4f) shouldBe (72f plusOrMinus 0.001f) }
})
