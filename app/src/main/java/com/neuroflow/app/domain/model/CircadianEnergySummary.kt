package com.neuroflow.app.domain.model

import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.EnergyPhase
import java.time.Duration
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

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
    val isOvertimeActive: Boolean = false,
    val isStretchMode: Boolean = false,
    val overtimeMinutesWorked: Int = 0
)

fun buildCircadianEnergySummary(
    dailySummary: DailyCircadianSummary?,
    snapshot: CircadianSnapshot,
    sleepPressurePoints: Int = 450,
    dailyDeepWorkTargetMinutes: Int? = null,
    sleepHour: Int = 23,
    completedDeepWorkMinutes: Int = 0,
    currentTime: LocalTime = LocalTime.now(),
    isOvertimeActive: Boolean = false
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
    val sleepTime = LocalTime.of(sleepHour.coerceIn(0, 23), 0)
    val sleepPressureNormalized = if (snapshot.homeostaticPressureS > 0.0 && !snapshot.homeostaticPressureS.isNaN()) {
        snapshot.homeostaticPressureS.toFloat().coerceIn(0f, 1f)
    } else {
        (sleepPressurePoints / 1000f).coerceIn(0f, 1f)
    }
    val totalBudget = dailyDeepWorkTargetMinutes?.takeIf { it > 0 } ?: dailySummary?.recommendedDeepWorkMinutes ?: 240
    val unworkedBudget = max(0, totalBudget - completedDeepWorkMinutes)
    val overtimeWorked = max(0, completedDeepWorkMinutes - totalBudget)
    val energyFactor = (snapshot.usableEnergy.coerceIn(10, 100) / 100.0)
    val physiologicalBudget = (totalBudget * energyFactor).roundToInt().coerceAtLeast(25)

    val isEveningOrLowPhase =
        snapshot.phase == EnergyPhase.WIND_DOWN ||
        snapshot.phase == EnergyPhase.TROUGH ||
        snapshot.phase == EnergyPhase.RECOVERY_NEEDED ||
        snapshot.phase == EnergyPhase.RECOVERY ||
        snapshot.phase == EnergyPhase.CRASH ||
        currentTime >= windDownStart

    val minutesUntilSleep = if (currentTime < sleepTime) {
        Duration.between(currentTime, sleepTime).toMinutes().toInt().coerceAtLeast(30)
    } else {
        // Past nominal sleep hour (late-night deadline work) -> still allow realistic stretch block
        60
    }

    val targetReached = completedDeepWorkMinutes >= totalBudget
    val isStretchMode = isOvertimeActive || isEveningOrLowPhase || targetReached

    val cognitiveBudget = when {
        isOvertimeActive -> {
            val overtimeReserve = max(unworkedBudget, 90)
            minOf(overtimeReserve, minutesUntilSleep.coerceIn(35, 240)).coerceAtLeast(30)
        }
        isEveningOrLowPhase -> {
            // Never clamp evening budget to 0: surface realistic light/stretch capacity
            val stretchAllowance = ((snapshot.usableEnergy.coerceIn(15, 100) / 100.0) * 80.0)
                .roundToInt()
                .coerceIn(25, 90)
            val boundedBySleep = minOf(stretchAllowance, minutesUntilSleep).coerceAtLeast(25)
            if (unworkedBudget > 0) minOf(unworkedBudget, boundedBySleep).coerceAtLeast(25) else boundedBySleep
        }
        targetReached -> {
            // Daily target hit during daytime: keep a soft stretch reserve instead of 0m
            val temporalToWindDown = Duration.between(currentTime, windDownStart).toMinutes().toInt().coerceAtLeast(30)
            minOf(physiologicalBudget, temporalToWindDown, 60).coerceAtLeast(25)
        }
        else -> {
            val temporalBudget = Duration.between(currentTime, windDownStart).toMinutes().toInt().coerceAtLeast(30)
            minOf(unworkedBudget, physiologicalBudget, temporalBudget).coerceAtLeast(20)
        }
    }
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
        calibrationQuality = confidence,
        isOvertimeActive = isOvertimeActive,
        isStretchMode = isStretchMode,
        overtimeMinutesWorked = overtimeWorked
    )
}
