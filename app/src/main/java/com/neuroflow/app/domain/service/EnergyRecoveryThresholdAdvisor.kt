package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.model.HourlyEnergyPrediction
import com.neuroflow.app.domain.model.PredictedEnergyCurveUiModel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Evaluates a user's personalized recovery energy threshold from their Vico-plotted
 * [PredictedEnergyCurveUiModel] and decides when to trigger a short-break local notification.
 */
object EnergyRecoveryThresholdAdvisor {

    const val MIN_THRESHOLD_PERCENT = 25
    const val MAX_THRESHOLD_PERCENT = 75
    const val DEFAULT_BASE_THRESHOLD_PERCENT = 45
    const val ALERT_COOLDOWN_MILLIS = 3L * 3_600_000L

    data class RecoveryThresholdProfile(
        val effectiveThresholdPercent: Int,
        val baseThresholdPercent: Int,
        val isPersonalized: Boolean,
        val dipFloorPercent: Int,
        val sleepPressureAdjustmentPercent: Int,
        val confidencePercent: Int,
        val summaryReason: String
    )

    data class BreakSuggestion(
        val shouldNotify: Boolean,
        val currentHour: Int,
        val currentEnergyPercent: Int,
        val recoveryThresholdPercent: Int,
        val deficitPercent: Int,
        val recommendedBreakMinutes: Int,
        val nextReboundHour: Int?,
        val nextReboundEnergyPercent: Int?,
        val notificationTitle: String,
        val notificationMessage: String,
        val breakActivitySuggestion: String,
        val suppressionReason: String? = null
    )

    fun computePersonalizedThreshold(
        hourlyPoints: List<HourlyEnergyPrediction>,
        dipStartHour: Int = 13,
        dipEndHour: Int = 15,
        peakEnergyValue: Int = 80,
        modelConfidencePercent: Int = 0,
        hasHistoricalData: Boolean = false,
        baseThresholdPercent: Int = DEFAULT_BASE_THRESHOLD_PERCENT,
        autoPersonalized: Boolean = true,
        wakeUpHour: Int = 7,
        sleepHour: Int = 23,
        sleepPressurePoints: Int = 0
    ): RecoveryThresholdProfile {
        val clampedBase = baseThresholdPercent.coerceIn(20, MAX_THRESHOLD_PERCENT)
        if (!autoPersonalized || hourlyPoints.isEmpty()) {
            val fixed = clampedBase.coerceIn(MIN_THRESHOLD_PERCENT, MAX_THRESHOLD_PERCENT)
            return RecoveryThresholdProfile(
                effectiveThresholdPercent = fixed,
                baseThresholdPercent = clampedBase,
                isPersonalized = false,
                dipFloorPercent = fixed,
                sleepPressureAdjustmentPercent = 0,
                confidencePercent = modelConfidencePercent.coerceIn(0, 100),
                summaryReason = "Manual threshold ($fixed%)"
            )
        }

        val wakingPoints = filterWakingPoints(hourlyPoints, wakeUpHour, sleepHour)
            .ifEmpty { hourlyPoints }

        val dipHours = expandHourRange(dipStartHour, dipEndHour)
        val dipEnergyValues = wakingPoints
            .filter { it.hour in dipHours }
            .map { it.predictedEnergy }
            .ifEmpty {
                wakingPoints.map { it.predictedEnergy }
                    .sorted()
                    .take((wakingPoints.size / 4).coerceAtLeast(1))
            }

        val dipFloor = dipEnergyValues.average().roundToInt().coerceIn(20, 80)

        // Personalize using empirical completion troughs when historical data exists
        val completedHours = wakingPoints.filter { it.historicalCompletionCount > 0 }
        val historicalAdjustment = if (hasHistoricalData && completedHours.isNotEmpty()) {
            val lowestProductiveHourEnergy = completedHours.minOf { it.predictedEnergy }
            val confidenceWeight = (modelConfidencePercent.coerceIn(0, 100) / 100.0)
            // When the user consistently completes tasks down to a lower/higher energy, gently adapt
            ((lowestProductiveHourEnergy - clampedBase) * 0.25 * confidenceWeight).roundToInt()
                .coerceIn(-6, 8)
        } else {
            0
        }

        // High peak amplitude makes mid-day troughs feel sharper; raise threshold gently
        val dynamicRangeAdjustment = when {
            peakEnergyValue >= 88 && dipFloor <= 55 -> 2
            peakEnergyValue <= 68 -> -2
            else -> 0
        }

        // Elevated sleep pressure warrants earlier recovery breaks
        val sleepPressureAdjustment = when {
            sleepPressurePoints >= 750 -> 5
            sleepPressurePoints >= 600 -> 3
            else -> 0
        }

        // Blend user base (55%) with circadian dip floor + early-warning lead (45%)
        val dipEarlyTrigger = (dipFloor - 3).coerceIn(MIN_THRESHOLD_PERCENT, MAX_THRESHOLD_PERCENT)
        val blended = ((clampedBase * 0.55) + (dipEarlyTrigger * 0.45)).roundToInt() +
            historicalAdjustment +
            dynamicRangeAdjustment +
            sleepPressureAdjustment

        val minBound = (clampedBase - 10).coerceAtLeast(MIN_THRESHOLD_PERCENT)
        val maxBound = (clampedBase + 15).coerceAtMost(MAX_THRESHOLD_PERCENT)
        val effectiveThreshold = blended.coerceIn(minBound, maxBound)
            .coerceIn(MIN_THRESHOLD_PERCENT, MAX_THRESHOLD_PERCENT)

        val reasonParts = buildList {
            add("dip floor ${dipFloor}%")
            if (sleepPressureAdjustment > 0) add("sleep load +${sleepPressureAdjustment}%")
            if (hasHistoricalData && modelConfidencePercent > 0) add("${modelConfidencePercent}% model fit")
        }

        return RecoveryThresholdProfile(
            effectiveThresholdPercent = effectiveThreshold,
            baseThresholdPercent = clampedBase,
            isPersonalized = true,
            dipFloorPercent = dipFloor,
            sleepPressureAdjustmentPercent = sleepPressureAdjustment,
            confidencePercent = modelConfidencePercent.coerceIn(0, 100),
            summaryReason = "Personalized (${reasonParts.joinToString(" · ")})"
        )
    }

    fun evaluateBreakTrigger(
        curve: PredictedEnergyCurveUiModel,
        prefs: UserPreferences,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BreakSuggestion {
        val currentHour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour.coerceIn(0, 23)
        val point = curve.hourlyPoints.firstOrNull { it.hour == currentHour }
        val currentEnergy = point?.predictedEnergy ?: 50
        val profile = computePersonalizedThreshold(
            hourlyPoints = curve.hourlyPoints,
            dipStartHour = curve.dipStartHour,
            dipEndHour = curve.dipEndHour,
            peakEnergyValue = curve.peakEnergyValue,
            modelConfidencePercent = curve.modelConfidencePercent,
            hasHistoricalData = curve.hasHistoricalData,
            baseThresholdPercent = prefs.recoveryThresholdBasePercent,
            autoPersonalized = prefs.recoveryThresholdAutoPersonalized,
            wakeUpHour = prefs.wakeUpHour,
            sleepHour = prefs.sleepHour,
            sleepPressurePoints = prefs.sleepPressurePoints
        )

        val threshold = profile.effectiveThresholdPercent
        val deficit = (threshold - currentEnergy).coerceAtLeast(0)
        val baseBreakMins = prefs.autoSchedulingBreakDurationMinutes.coerceIn(5, 30)
        val recommendedBreakMinutes = if (deficit >= 12) {
            baseBreakMins.coerceAtLeast(15)
        } else {
            baseBreakMins.coerceIn(5, 15)
        }

        val upcomingWakingHours = upcomingWakingHours(
            currentHour = currentHour,
            wakeUpHour = prefs.wakeUpHour,
            sleepHour = prefs.sleepHour
        )
        val reboundPoint = curve.hourlyPoints
            .filter { it.hour in upcomingWakingHours }
            .firstOrNull { it.predictedEnergy >= threshold + 8 }

        val breakActivity = when {
            deficit >= 12 -> "Step away for a ${recommendedBreakMinutes}-min walk, water, or eyes-closed rest"
            currentHour in 13..16 -> "Take a ${recommendedBreakMinutes}-min afternoon reset away from screens"
            else -> "Pause for a ${recommendedBreakMinutes}-min stretch, water refill, and deep breaths"
        }

        val reboundClause = if (reboundPoint != null) {
            String.format(
                Locale.ROOT,
                " Your curve rebounds around %02d:00 (%d%%).",
                reboundPoint.hour,
                reboundPoint.predictedEnergy
            )
        } else {
            " Protect your remaining evening energy."
        }

        val title = "Energy dip: take a ${recommendedBreakMinutes}-min recovery break"
        val message = "Predicted energy is $currentEnergy% (below your $threshold% recovery threshold). $breakActivity.$reboundClause"

        val withinWakingHours = isHourInWakingWindow(currentHour, prefs.wakeUpHour, prefs.sleepHour)
        val cooldownElapsed = prefs.lastRecoveryAlertSentAtMillis <= 0L ||
            (nowMillis - prefs.lastRecoveryAlertSentAtMillis) >= ALERT_COOLDOWN_MILLIS

        val suppressionReason = when {
            !prefs.energyRecoveryAlertsEnabled -> "alerts_disabled"
            !withinWakingHours -> "outside_waking_hours"
            point == null -> "no_hourly_prediction"
            currentEnergy >= threshold -> "above_threshold"
            !cooldownElapsed -> "cooldown_active"
            else -> null
        }

        return BreakSuggestion(
            shouldNotify = suppressionReason == null,
            currentHour = currentHour,
            currentEnergyPercent = currentEnergy,
            recoveryThresholdPercent = threshold,
            deficitPercent = deficit,
            recommendedBreakMinutes = recommendedBreakMinutes,
            nextReboundHour = reboundPoint?.hour,
            nextReboundEnergyPercent = reboundPoint?.predictedEnergy,
            notificationTitle = title,
            notificationMessage = message,
            breakActivitySuggestion = breakActivity,
            suppressionReason = suppressionReason
        )
    }

    internal fun isHourInWakingWindow(hour: Int, wakeUpHour: Int, sleepHour: Int): Boolean {
        val h = hour.coerceIn(0, 23)
        val wake = wakeUpHour.coerceIn(0, 23)
        val sleep = sleepHour.coerceIn(0, 23)
        if (wake == sleep) return true
        return if (wake < sleep) {
            h in wake until sleep
        } else {
            h >= wake || h < sleep
        }
    }

    private fun filterWakingPoints(
        points: List<HourlyEnergyPrediction>,
        wakeUpHour: Int,
        sleepHour: Int
    ): List<HourlyEnergyPrediction> = points.filter {
        isHourInWakingWindow(it.hour, wakeUpHour, sleepHour)
    }

    private fun expandHourRange(startHour: Int, endHour: Int): Set<Int> {
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(0, 23)
        if (start == end) return setOf(start)
        return if (start < end) {
            (start..end).toSet()
        } else {
            ((start..23) + (0..end)).toSet()
        }
    }

    private fun upcomingWakingHours(
        currentHour: Int,
        wakeUpHour: Int,
        sleepHour: Int
    ): Set<Int> {
        val result = mutableSetOf<Int>()
        for (offset in 1..16) {
            val h = (currentHour + offset) % 24
            if (!isHourInWakingWindow(h, wakeUpHour, sleepHour)) break
            result.add(h)
        }
        return result
    }
}
