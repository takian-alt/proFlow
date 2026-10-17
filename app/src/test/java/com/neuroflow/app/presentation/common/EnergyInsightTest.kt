package com.neuroflow.app.presentation.common

import com.neuroflow.app.domain.model.ConfidenceComponents
import com.neuroflow.app.domain.model.EffectivePeakProfile
import com.neuroflow.app.domain.model.PeakWindow
import com.neuroflow.app.domain.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyInsightTest {

    @Test
    fun `profile confidence line includes component breakdown`() {
        val profile = EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = 360,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = 210, amplitude = 1.0f),
                PeakWindow(startMinuteOffset = 570, durationMinutes = 150, amplitude = 0.8f),
                PeakWindow(startMinuteOffset = 810, durationMinutes = 60, amplitude = 0.6f)
            ),
            confidence = ConfidenceComponents(
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

    @Test
    fun `window confidence helpers prefer per-window value`() {
        val profile = EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = 360,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = 210, amplitude = 1.0f),
                PeakWindow(startMinuteOffset = 480, durationMinutes = 150, amplitude = 0.78f),
                PeakWindow(startMinuteOffset = 720, durationMinutes = 60, amplitude = 0.58f)
            ),
            confidence = ConfidenceComponents(
                sleepCoverage = 0.8f,
                wakeConsistency = 0.8f,
                behaviorPerformance = 0.8f,
                overall = 0.82f
            ),
            windowConfidences = listOf(0.84f, 0.61f, 0.49f),
            explanation = "Adaptive profile"
        )

        assertEquals(61, EnergyInsight.windowConfidencePercent(profile, 1))
        assertEquals("Low", EnergyInsight.windowConfidenceTier(profile, 2))
    }

    @Test
    fun `confidence improvement actions include peak 2 and 3 guidance when low`() {
        val profile = EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = 360,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = 210, amplitude = 1.0f),
                PeakWindow(startMinuteOffset = 480, durationMinutes = 150, amplitude = 0.78f),
                PeakWindow(startMinuteOffset = 720, durationMinutes = 60, amplitude = 0.58f)
            ),
            confidence = ConfidenceComponents(
                sleepCoverage = 0.55f,
                wakeConsistency = 0.58f,
                behaviorPerformance = 0.50f,
                overall = 0.57f
            ),
            windowConfidences = listOf(0.72f, 0.52f, 0.42f),
            explanation = "Adaptive profile",
            driftStatus = "later_drift",
            weeklyBacktestErrorMinutes = 92f
        )

        val actions = EnergyInsight.confidenceImprovementActions(profile)
        assertTrue(actions.any { it.contains("Peak 2/3") })
        assertTrue(actions.any { it.contains("wake time") })
    }
}
