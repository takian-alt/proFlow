package com.neuroflow.app.domain.model

import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.EnergyPhase
import java.time.LocalTime

typealias EnergyPhase = com.neuroflow.app.domain.service.EnergyPhase

data class CircadianEnergySummary(
    val currentEnergyScore: Int,
    val currentPhase: EnergyPhase,
    val phaseConfidence: Float = 0.9f,
    val peakStart: LocalTime = LocalTime.of(9, 30),
    val peakEnd: LocalTime = LocalTime.of(12, 30),
    val dipStart: LocalTime = LocalTime.of(13, 30),
    val dipEnd: LocalTime = LocalTime.of(15, 30),
    val windDownStart: LocalTime = LocalTime.of(21, 30),
    val sleepPressureNormalized: Float = 0.45f,
    val cognitiveBudgetMinutesRemaining: Int = 240,
    val calibrationQuality: Float = 0.85f,
)

fun buildCircadianEnergySummary(
    dailySummary: DailyCircadianSummary?,
    snapshot: CircadianSnapshot,
    sleepPressurePoints: Int = 450,
    dailyDeepWorkTargetMinutes: Int? = null,
    sleepHour: Int = 23,
    completedDeepWorkMinutes: Int = 0
): CircadianEnergySummary {
    val peakStart = dailySummary?.let { LocalTime.of(it.peakWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(9, 30)
    val peakEnd = dailySummary?.let { LocalTime.of(it.peakWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(12, 30)
    val dipStart = dailySummary?.let { LocalTime.of(it.dipWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(13, 30)
    val dipEnd = dailySummary?.let { LocalTime.of(it.dipWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(15, 30)
    val targetWindDownHour = ((sleepHour - 2) % 24 + 24) % 24
    val windDownStart = LocalTime.of(targetWindDownHour, 0)
    val sleepPressureNormalized = if (snapshot.homeostaticPressureS > 0.0 && !snapshot.homeostaticPressureS.isNaN()) {
        snapshot.homeostaticPressureS.toFloat().coerceIn(0f, 1f)
    } else {
        (sleepPressurePoints / 1000f).coerceIn(0f, 1f)
    }
    val totalBudget = dailyDeepWorkTargetMinutes?.takeIf { it > 0 } ?: dailySummary?.recommendedDeepWorkMinutes ?: 240
    val cognitiveBudget = kotlin.math.max(0, totalBudget - completedDeepWorkMinutes)
    val confidence = snapshot.predictionConfidence.toFloat().coerceIn(0.1f, 1.0f)

    return CircadianEnergySummary(
        currentEnergyScore = snapshot.usableEnergy,
        currentPhase = snapshot.phase,
        phaseConfidence = confidence,
        peakStart = peakStart,
        peakEnd = peakEnd,
        dipStart = dipStart,
        dipEnd = dipEnd,
        windDownStart = windDownStart,
        sleepPressureNormalized = sleepPressureNormalized,
        cognitiveBudgetMinutesRemaining = cognitiveBudget,
        calibrationQuality = confidence
    )
}
