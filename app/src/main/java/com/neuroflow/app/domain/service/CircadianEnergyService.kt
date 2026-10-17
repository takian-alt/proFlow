package com.neuroflow.app.domain.service

import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class EnergyDemand(val targetScore: Double) {
    LOW(25.0),
    MODERATE(50.0),
    HIGH(80.0)
}

enum class TaskCategory {
    DEEP_WORK, CREATIVE, ADMIN, EXERCISE, MINDFULNESS, GENERAL
}

enum class EnergyPhase {
    PEAK, REBOUND, DIP, WIND_DOWN, RECOVERY_NEEDED
}

data class CircadianSnapshot(
    val timestampMillis: Long,
    val usableEnergy: Int,            // [0..100]
    val homeostaticPressureS: Double, // [0.0..1.0]
    val circadianAlertnessC: Double,  // [0.0..1.0]
    val focusDepletionPenalty: Double,// [0.0..0.15]
    val phase: EnergyPhase
)

interface CircadianEnergyService {
    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CircadianSnapshot

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CircadianSnapshot>

    fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double
}

@Singleton
class DefaultCircadianEnergyService @Inject constructor() : CircadianEnergyService {

    companion object {
        private const val S_MIN = 0.10
        private const val S_MAX = 0.85
        private const val TAU_RECOVERY_HOURS = 3.6
        private const val TAU_WAKE_HOURS = 18.2
        private const val PEAK_OFFSET_FROM_WAKE_HOURS = 4.5
        private const val MAX_FOCUS_DEPLETION = 0.15
    }

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId
    ): CircadianSnapshot {
        // 1. Continuous Sleep Recovery -> Initial Wake Pressure S0 (No 60m cliff)
        val sleepHours = max(0.0, lastSleepDurationMinutes / 60.0)
        val s0 = S_MIN + (S_MAX - S_MIN) * exp(-sleepHours / TAU_RECOVERY_HOURS)

        // 2. Continuous Homeostatic Wake Pressure S(t_w)
        val hoursAwake = max(0.0, (timestampMillis - wakeTimeMillis) / 3_600_000.0)
        val processS = (1.0 - (1.0 - s0) * exp(-hoursAwake / TAU_WAKE_HOURS)).coerceIn(0.0, 1.0)

        // 3. DST-Safe Wall-Clock Extraction for Process C(t)
        val zonedNow = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val clockHour = zonedNow.hour + (zonedNow.minute / 60.0)
        val wakeClockHour = zonedWake.hour + (zonedWake.minute / 60.0)
        val phasePeakPhi = (wakeClockHour + PEAK_OFFSET_FROM_WAKE_HOURS) % 24.0

        // Bimodal sinusoidal model (captures morning peak, afternoon dip, early evening rebound)
        val primaryHarmonic = 0.38 * cos((2.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val secondaryHarmonic = 0.12 * cos((4.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val processC = (0.50 + primaryHarmonic - secondaryHarmonic).coerceIn(0.0, 1.0)

        // 4. Synaptic Depletion from unbroken focus > 45m (Fixes Flaw #1: Never penalizes entering flow)
        val excessFocusMins = max(0, continuousActiveFocusMinutes - 45).toDouble()
        val focusDepletion = if (excessFocusMins > 0.0) {
            MAX_FOCUS_DEPLETION * (1.0 - exp(-excessFocusMins / 60.0))
        } else {
            0.0
        }

        // 5. Unified Usable Energy [0..100] (Fixes Flaw #8: Single fatigue representation)
        val rawEnergy = 100.0 * (0.55 * processC + 0.45 * (1.0 - processS) - focusDepletion)
        val usableEnergy = rawEnergy.roundToInt().coerceIn(0, 100)

        val phase = when {
            continuousActiveFocusMinutes >= 90 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 72 -> EnergyPhase.PEAK
            hoursAwake in 6.0..9.5 && processC < 0.48 -> EnergyPhase.DIP
            hoursAwake > 9.5 && usableEnergy >= 55 -> EnergyPhase.REBOUND
            else -> EnergyPhase.WIND_DOWN
        }

        return CircadianSnapshot(
            timestampMillis = timestampMillis,
            usableEnergy = usableEnergy,
            homeostaticPressureS = processS,
            circadianAlertnessC = processC,
            focusDepletionPenalty = focusDepletion,
            phase = phase
        )
    }

    override fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId
    ): List<CircadianSnapshot> {
        val stepMillis = stepMinutes * 60_000L
        val steps = (24 * 60) / stepMinutes
        return List(steps) { idx ->
            val t = dayStartMillis + idx * stepMillis
            evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId
            )
        }
    }

    override fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double {
        val e = snapshot.usableEnergy.toDouble()
        return when (category) {
            TaskCategory.DEEP_WORK -> e // Linearly favors peak energy
            TaskCategory.CREATIVE -> {
                // Favors high alertness AND moderate associative disinhibition (rebound/peak)
                if (snapshot.phase == EnergyPhase.REBOUND) min(100.0, e + 15.0) else e * 0.9
            }
            TaskCategory.ADMIN -> {
                // Peaks around 45-60 energy (ideal for afternoon dip; doesn't waste peak hours)
                100.0 * exp(-((e - 50.0) * (e - 50.0)) / (2.0 * 22.0 * 22.0))
            }
            TaskCategory.EXERCISE -> {
                // Ideal in afternoon rebound or moderate energy
                100.0 * exp(-((e - 65.0) * (e - 65.0)) / (2.0 * 25.0 * 25.0))
            }
            TaskCategory.MINDFULNESS -> {
                // Best during wind-down, dip, or recovery needed
                if (snapshot.phase == EnergyPhase.RECOVERY_NEEDED) 100.0 else (100.0 - e * 0.5).coerceIn(30.0, 95.0)
            }
            TaskCategory.GENERAL -> 70.0
        }.coerceIn(0.0, 100.0)
    }
}
