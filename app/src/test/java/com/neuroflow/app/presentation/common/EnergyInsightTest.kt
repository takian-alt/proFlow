package com.neuroflow.app.presentation.common

import com.neuroflow.app.domain.engine.PeakEnergyEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyInsightTest {

    @Test
    fun `profile confidence line includes component breakdown`() {
        val profile = PeakEnergyEngine.EffectivePeakProfile(
            profileType = PeakEnergyEngine.ProfileType.WORKDAY,
            anchorMinuteOfDay = 360,
            windows = PeakEnergyEngine.defaultCircadianProfile(
                com.neuroflow.app.domain.engine.MEQChronotypeDetector.Chronotype.MODERATE_MORNING
            ).windows,
            confidence = PeakEnergyEngine.ConfidenceComponents(
                sleepCoverage = 0.8f,
                wakeConsistency = 0.7f,
                behaviorPerformance = 0.6f,
                overall = 0.72f
            ),
            explanation = "Adaptive morning profile",
            driftStatus = "stable",
            weeklyBacktestErrorMinutes = 34f
        )

        val line = EnergyInsight.profileConfidenceLine(profile)
        assertTrue(line.contains("Confidence"))
        assertTrue(line.contains("sleep"))
        assertTrue(line.contains("wake"))
        assertTrue(line.contains("behavior"))
    }
}
