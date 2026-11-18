package com.neuroflow.app.domain.model

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.EnergyRecoveryThresholdAdvisor
import com.neuroflow.app.domain.service.LearnedCircadianProfile
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class HourlyEnergyPrediction(
    val hour: Int,
    val predictedEnergy: Int,
    val historicalCompletionCount: Int,
    val historicalActivityScore: Int,
    val phase: EnergyPhase
)

data class PredictedEnergyCurveUiModel(
    val hourlyPoints: List<HourlyEnergyPrediction> = emptyList(),
    val peakStartHour: Int = 9,
    val peakEndHour: Int = 12,
    val peakEnergyValue: Int = 0,
    val dipStartHour: Int = 13,
    val dipEndHour: Int = 15,
    val bestAutoScheduleHour: Int = 9,
    val historicalCompletionCount: Int = 0,
    val modelConfidencePercent: Int = 0,
    val learnedPeakHour: Double? = null,
    val currentHour: Int? = null,
    val hasHistoricalData: Boolean = false,
    val recoveryThresholdPercent: Int = EnergyRecoveryThresholdAdvisor.DEFAULT_BASE_THRESHOLD_PERCENT,
    val recoveryThresholdSummary: String = "",
    val isRecoveryThresholdPersonalized: Boolean = false,
    val currentHourBelowRecoveryThreshold: Boolean = false,
    val currentHourEnergyPercent: Int? = null,
    val recommendedBreakMinutes: Int = 15
)

/**
 * Combines projected 24-hour circadian energy snapshots (personalized with [learnedProfile])
 * with historical task completion and focus session timestamps to visualize predicted daily
 * energy alongside the user's empirical completion rhythm for auto-scheduling.
 */
fun buildPredictedEnergyCurveUiModel(
    timeline: List<CircadianSnapshot>,
    completedTasks: List<TaskEntity>,
    sessions: List<TimeSessionEntity> = emptyList(),
    circadianSummary: DailyCircadianSummary? = null,
    learnedProfile: LearnedCircadianProfile? = null,
    workDayStart: Int = 8,
    workDayEnd: Int = 20,
    currentHour: Int? = null,
    zoneId: ZoneId = ZoneId.systemDefault(),
    recoveryThresholdBasePercent: Int = EnergyRecoveryThresholdAdvisor.DEFAULT_BASE_THRESHOLD_PERCENT,
    recoveryThresholdAutoPersonalized: Boolean = true,
    wakeUpHour: Int = 7,
    sleepHour: Int = 23,
    sleepPressurePoints: Int = 0,
    recommendedBreakMinutes: Int = 15
): PredictedEnergyCurveUiModel {
    val snapshotsByHour = timeline.groupBy { snap ->
        Instant.ofEpochMilli(snap.timestampMillis).atZone(zoneId).hour
    }

    val countsByHour = IntArray(24)
    val weightedScoresByHour = DoubleArray(24)

    for (task in completedTasks) {
        val ts = task.completedAt ?: task.updatedAt
        if (ts <= 0L) continue
        val hour = Instant.ofEpochMilli(ts).atZone(zoneId).hour.coerceIn(0, 23)
        countsByHour[hour] += 1
        val effortWeight = when {
            task.isFrog -> 1.6
            task.energyLevel == EnergyLevel.HIGH -> 1.5
            task.energyLevel == EnergyLevel.MEDIUM -> 1.15
            else -> 0.85
        }
        weightedScoresByHour[hour] += effortWeight
    }

    val closedSessions = sessions.filter { it.endedAt != null && it.startedAt > 0L }
    for (session in closedSessions) {
        val hour = Instant.ofEpochMilli(session.startedAt).atZone(zoneId).hour.coerceIn(0, 23)
        val durationMins = if ((session.endedAt ?: 0L) > session.startedAt) {
            ((session.endedAt!! - session.startedAt) / 60_000.0).coerceIn(5.0, 240.0)
        } else {
            session.durationMinutes.toDouble().coerceIn(5.0, 240.0)
        }
        countsByHour[hour] += 1
        weightedScoresByHour[hour] += (durationMins / 30.0).coerceIn(0.4, 2.5)
    }

    val maxWeightedActivity = weightedScoresByHour.maxOrNull() ?: 0.0

    val hourlyPoints = (0 until 24).map { hour ->
        val snaps = snapshotsByHour[hour].orEmpty()
        val energy = if (snaps.isNotEmpty()) {
            snaps.map { it.usableEnergy }.average().roundToInt().coerceIn(0, 100)
        } else {
            // Smooth fallback when timeline is empty or sparse
            when (hour) {
                in 0..5 -> 25
                in 6..8 -> 62
                in 9..12 -> 84
                in 13..15 -> 56
                in 16..18 -> 72
                in 19..21 -> 52
                else -> 32
            }
        }
        val phase = if (snaps.isNotEmpty()) {
            snaps[snaps.size / 2].phase
        } else {
            when {
                energy >= 78 -> EnergyPhase.PEAK
                energy >= 62 -> EnergyPhase.REBOUND
                energy >= 45 -> EnergyPhase.DIP
                else -> EnergyPhase.WIND_DOWN
            }
        }
        val normalizedActivity = if (maxWeightedActivity > 0.0) {
            ((weightedScoresByHour[hour] / maxWeightedActivity) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        HourlyEnergyPrediction(
            hour = hour,
            predictedEnergy = energy,
            historicalCompletionCount = countsByHour[hour],
            historicalActivityScore = normalizedActivity,
            phase = phase
        )
    }

    val bestOverallPoint = hourlyPoints.maxByOrNull { it.predictedEnergy }
    val inferredPeakStart = circadianSummary?.peakWindowStartHour ?: (bestOverallPoint?.hour ?: 9)
    val inferredPeakEnd = circadianSummary?.peakWindowEndHour ?: ((inferredPeakStart + 3) % 24)
    val peakWindowHours = if (inferredPeakStart <= inferredPeakEnd) {
        (inferredPeakStart until inferredPeakEnd).toList().ifEmpty { listOf(inferredPeakStart) }
    } else {
        ((inferredPeakStart until 24) + (0 until inferredPeakEnd)).toList()
    }
    val peakEnergy = peakWindowHours
        .mapNotNull { h -> hourlyPoints.getOrNull(h)?.predictedEnergy }
        .maxOrNull()
        ?: bestOverallPoint?.predictedEnergy
        ?: 0

    val safeStart = workDayStart.coerceIn(0, 22)
    val safeEnd = workDayEnd.coerceIn(safeStart + 1, 23)
    val workWindowPoints = hourlyPoints.filter { it.hour in safeStart..safeEnd }
    val upcomingWorkPoints = if (currentHour != null) {
        workWindowPoints.filter { it.hour >= currentHour }
    } else {
        emptyList()
    }
    val bestSchedulePoint = (upcomingWorkPoints.ifEmpty { workWindowPoints }.ifEmpty { hourlyPoints })
        .maxByOrNull { it.predictedEnergy }

    val totalSamples = completedTasks.size + closedSessions.size
    val confidencePercent = if (learnedProfile != null && totalSamples > 0) {
        (learnedProfile.confidence * 100.0).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    val dipStart = (circadianSummary?.dipWindowStartHour ?: 13).coerceIn(0, 23)
    val dipEnd = (circadianSummary?.dipWindowEndHour ?: 15).coerceIn(0, 23)
    val recoveryProfile = EnergyRecoveryThresholdAdvisor.computePersonalizedThreshold(
        hourlyPoints = hourlyPoints,
        dipStartHour = dipStart,
        dipEndHour = dipEnd,
        peakEnergyValue = peakEnergy,
        modelConfidencePercent = confidencePercent,
        hasHistoricalData = totalSamples > 0,
        baseThresholdPercent = recoveryThresholdBasePercent,
        autoPersonalized = recoveryThresholdAutoPersonalized,
        wakeUpHour = wakeUpHour,
        sleepHour = sleepHour,
        sleepPressurePoints = sleepPressurePoints
    )

    val currentEnergy = currentHour?.let { h ->
        hourlyPoints.firstOrNull { it.hour == h }?.predictedEnergy
    }
    val isCurrentHourBelowRecovery = currentHour != null &&
        currentEnergy != null &&
        EnergyRecoveryThresholdAdvisor.isHourInWakingWindow(currentHour, wakeUpHour, sleepHour) &&
        currentEnergy < recoveryProfile.effectiveThresholdPercent

    val effectiveBreakMinutes = if (isCurrentHourBelowRecovery && currentEnergy != null) {
        val deficit = (recoveryProfile.effectiveThresholdPercent - currentEnergy).coerceAtLeast(0)
        if (deficit >= 12) recommendedBreakMinutes.coerceIn(15, 30) else recommendedBreakMinutes.coerceIn(5, 15)
    } else {
        recommendedBreakMinutes.coerceIn(5, 30)
    }

    return PredictedEnergyCurveUiModel(
        hourlyPoints = hourlyPoints,
        peakStartHour = inferredPeakStart.coerceIn(0, 23),
        peakEndHour = inferredPeakEnd.coerceIn(0, 23),
        peakEnergyValue = peakEnergy,
        dipStartHour = dipStart,
        dipEndHour = dipEnd,
        bestAutoScheduleHour = bestSchedulePoint?.hour ?: inferredPeakStart.coerceIn(0, 23),
        historicalCompletionCount = totalSamples,
        modelConfidencePercent = confidencePercent,
        learnedPeakHour = if (totalSamples > 0) learnedProfile?.peakHourMean else null,
        currentHour = currentHour,
        hasHistoricalData = totalSamples > 0,
        recoveryThresholdPercent = recoveryProfile.effectiveThresholdPercent,
        recoveryThresholdSummary = recoveryProfile.summaryReason,
        isRecoveryThresholdPersonalized = recoveryProfile.isPersonalized,
        currentHourBelowRecoveryThreshold = isCurrentHourBelowRecovery,
        currentHourEnergyPercent = currentEnergy,
        recommendedBreakMinutes = effectiveBreakMinutes
    )
}
