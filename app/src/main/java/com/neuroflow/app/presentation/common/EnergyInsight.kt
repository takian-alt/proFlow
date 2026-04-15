package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import java.util.Calendar
import kotlin.math.abs

object EnergyInsight {

    fun isMorningType(chronotype: String?): Boolean {
        return chronotype == "MODERATE_MORNING" || chronotype == "DEFINITE_MORNING"
    }

    fun effectivePeakMinuteOfDay(prefs: UserPreferences): Int {
        return if (
            prefs.quizPeakEnabled &&
            prefs.effectivePeakMinuteOfDay in 0 until (24 * 60)
        ) {
            prefs.effectivePeakMinuteOfDay
        } else {
            (prefs.peakEnergyStart.coerceIn(0, 23) * 60)
        }
    }

    fun detectedPeakMinuteOfDayOrNull(prefs: UserPreferences): Int? {
        return prefs.detectedPeakMinuteOfDay.takeIf { it in 0 until (24 * 60) }
    }

    fun confidencePercent(prefs: UserPreferences): Int {
        return (prefs.peakDetectionConfidence.coerceIn(0f, 1f) * 100f).toInt()
    }

    fun minuteLabel(minuteOfDay: Int): String {
        val normalized = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
        val hour = normalized / 60
        val minute = normalized % 60
        val amPm = if (hour < 12) "am" else "pm"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d%s", displayHour, minute, amPm)
    }

    fun timingHintForNow(targetMinuteOfDay: Int, now: Calendar = Calendar.getInstance()): String {
        val nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val minutesToPeak = minuteDistance(nowMinute, targetMinuteOfDay)
        return if (minutesToPeak <= 10) {
            "Peak zone now"
        } else {
            "Peak around ${minuteLabel(targetMinuteOfDay)}"
        }
    }

    fun confidenceTier(confidence: Float): String {
        val safe = confidence.coerceIn(0f, 1f)
        return when {
            safe >= 0.8f -> "High"
            safe >= 0.6f -> "Moderate"
            else -> "Low"
        }
    }

    fun profileSummary(profile: PeakEnergyEngine.EffectivePeakProfile?): String {
        if (profile == null) return "Baseline profile"
        val windows = profile.windows.joinToString(separator = ", ") { w ->
            "${(w.durationMinutes / 60f)}h"
        }
        return "${profile.profileType.name.lowercase().replaceFirstChar { it.uppercase() }} profile • windows $windows"
    }

    fun profileConfidenceLine(profile: PeakEnergyEngine.EffectivePeakProfile?): String {
        if (profile == null) return "Confidence unavailable"
        val c = profile.confidence
        return "Confidence ${confidenceTier(c.overall)} (${(c.overall * 100).toInt()}%) • " +
            "sleep ${(c.sleepCoverage * 100).toInt()}% • wake ${(c.wakeConsistency * 100).toInt()}% • behavior ${(c.behaviorPerformance * 100).toInt()}%"
    }

    fun adaptiveHint(profile: PeakEnergyEngine.EffectivePeakProfile?): String {
        return profile?.explanation ?: "Using baseline peak estimate."
    }

    fun backtestSummary(profile: PeakEnergyEngine.EffectivePeakProfile?): String {
        val error = profile?.weeklyBacktestErrorMinutes
        if (error == null) return "Backtesting: insufficient data"
        val rounded = error.toInt()
        val quality = when {
            rounded <= 30 -> "strong"
            rounded <= 75 -> "moderate"
            else -> "weak"
        }
        return "Backtesting error: ${rounded}m ($quality fit)"
    }

    fun profileModeLabel(manualOverrideEnabled: Boolean, profile: PeakEnergyEngine.EffectivePeakProfile?): String {
        return when {
            manualOverrideEnabled -> "Profile mode: Manual override"
            profile != null -> "Profile mode: Adaptive ${profile.profileType.name.lowercase()}"
            else -> "Profile mode: Baseline"
        }
    }

    private fun minuteDistance(a: Int, b: Int): Int {
        val raw = abs(a - b)
        return minOf(raw, (24 * 60) - raw)
    }
}
