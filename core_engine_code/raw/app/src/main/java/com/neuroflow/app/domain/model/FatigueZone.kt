package com.neuroflow.app.domain.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class FatigueZone {
    RESTED,
    MODERATE,
    HIGH,
    CRITICAL;

    val label: String get() = label(this)

    companion object {
        const val SOFT_MAX_REFERENCE = 3000
        const val AWAKE_POINTS_PER_MINUTE = 1
        const val MAX_PRESSURE_POINTS = 14_400
        const val SLEEP_INERTIA_DURATION_MINUTES = 30
        const val DEFAULT_MAX_SLEEP_INERTIA_PENALTY = 0.35f
        const val CHRONIC_RESTRICTION_THRESHOLD_HOURS = 6.0f
        const val CHRONIC_RESTRICTION_THRESHOLD_MINUTES = 360
        const val OPTIMAL_DAILY_SLEEP_MINUTES = 480

        private const val FATIGUE_CURVE_GAMMA = 0.82f
        private const val CYCLE_MINUTES = 90
        private const val PHASE_ONE_END_MINUTE = 60
        private const val PHASE_ONE_BONUS = 126
        private const val FULL_CYCLE_RECOVERY = 279

        data class PressureResult(
            val pressurePoints: Int,
            val awakeMinutes: Int = 0,
            val sleepMinutes: Int = 0,
            val recoveryPointsEarned: Int = 0,
            val recoveryPointsApplied: Int = 0
        )

        fun pressureFromAwakeMinutes(awakeMinutes: Int): Int {
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            return (safeAwakeMinutes * AWAKE_POINTS_PER_MINUTE).coerceAtMost(MAX_PRESSURE_POINTS)
        }

        fun applyAwakeMinutes(currentPressure: Int, awakeMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            val addedPressure = pressureFromAwakeMinutes(safeAwakeMinutes)
            return PressureResult(
                pressurePoints = (safeCurrentPressure + addedPressure).coerceAtMost(MAX_PRESSURE_POINTS),
                awakeMinutes = safeAwakeMinutes
            )
        }

        fun applySleepSession(currentPressure: Int, sleepMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            val recoveryPoints = recoveryForSleepMinutes(safeSleepMinutes)
            val applied = minOf(safeCurrentPressure, recoveryPoints)
            val finalPressure = (safeCurrentPressure - applied).coerceAtLeast(0)
            return PressureResult(
                pressurePoints = finalPressure,
                sleepMinutes = safeSleepMinutes,
                recoveryPointsEarned = recoveryPoints,
                recoveryPointsApplied = applied
            )
        }

        fun fromPressurePoints(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone {
            val percent = fatiguePercent(pressurePoints, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueZone(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone =
            fromPressurePoints(pressurePoints, softMaxReference)

        fun fromUsableEnergy(usableEnergy: Int): FatigueZone {
            return when {
                usableEnergy >= 70 -> RESTED
                usableEnergy >= 45 -> MODERATE
                usableEnergy >= 25 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueRatio(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Float {
            val safeMax = softMaxReference.coerceAtLeast(1)
            val safePressure = pressurePoints.coerceAtLeast(0)
            val ratio = (safePressure.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
            return ratio.pow(FATIGUE_CURVE_GAMMA).coerceIn(0f, 1f)
        }

        fun fatiguePercent(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Int {
            return (fatigueRatio(pressurePoints, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun label(zone: FatigueZone): String {
            return when (zone) {
                RESTED -> "Rested"
                MODERATE -> "Moderate"
                HIGH -> "High"
                CRITICAL -> "Critical"
            }
        }

        fun fatigueZoneLabel(zone: FatigueZone): String = label(zone)

        fun calculateSleepInertiaPenalty(
            minutesSinceWake: Int,
            maxPenalty: Float = DEFAULT_MAX_SLEEP_INERTIA_PENALTY
        ): Float {
            if (minutesSinceWake < 0 || minutesSinceWake >= SLEEP_INERTIA_DURATION_MINUTES) {
                return 0f
            }
            val safeMax = maxPenalty.coerceIn(0f, 1f)
            val decayFraction = 1f - (minutesSinceWake.toFloat() / SLEEP_INERTIA_DURATION_MINUTES.toFloat())
            return (safeMax * decayFraction).coerceIn(0f, 1f)
        }

        fun isSleepInertiaActive(minutesSinceWake: Int?): Boolean {
            if (minutesSinceWake == null) return false
            return minutesSinceWake in 0 until SLEEP_INERTIA_DURATION_MINUTES
        }

        fun calculateChronicRestrictionMultiplier(rollingAverageDailySleepMinutes: Float): Float {
            if (rollingAverageDailySleepMinutes <= 0f) {
                return 1.6f
            }
            val averageHours = rollingAverageDailySleepMinutes / 60f
            if (averageHours >= CHRONIC_RESTRICTION_THRESHOLD_HOURS) {
                return 1.0f
            }
            val deficitHours = (CHRONIC_RESTRICTION_THRESHOLD_HOURS - averageHours).coerceAtLeast(0f)
            val penalty = (deficitHours / CHRONIC_RESTRICTION_THRESHOLD_HOURS) * 0.6f
            return (1.0f + penalty).coerceIn(1.0f, 1.6f)
        }

        fun calculateChronicRestrictionMultiplierFromDays(dailySleepMinutes: List<Int>): Float {
            if (dailySleepMinutes.isEmpty()) return 1.0f
            val validDays = dailySleepMinutes.filter { it >= 0 }
            if (validDays.isEmpty()) return 1.0f
            val avg = validDays.average().toFloat()
            return calculateChronicRestrictionMultiplier(avg)
        }

        fun adjustedFatigueRatio(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Float {
            val baseRatio = fatigueRatio(pressurePoints, softMaxReference)
            val chronicMultiplier = rollingAverageSleepMinutes?.let {
                calculateChronicRestrictionMultiplier(it)
            } ?: 1.0f
            val inertiaPenalty = minutesSinceWake?.let {
                calculateSleepInertiaPenalty(it)
            } ?: 0f

            val compounded = baseRatio * chronicMultiplier + inertiaPenalty
            return compounded.coerceIn(0f, 1f)
        }

        fun adjustedFatiguePercent(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Int {
            return (adjustedFatigueRatio(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun adjustedFatigueZone(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): FatigueZone {
            val percent = adjustedFatiguePercent(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun recoveryForSleepMinutes(sleepMinutes: Int): Int {
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            if (safeSleepMinutes == 0) return 0

            val fullCycles = safeSleepMinutes / CYCLE_MINUTES
            val remainderMinutes = safeSleepMinutes % CYCLE_MINUTES
            val remainderRecovery = (remainderMinutes * (FULL_CYCLE_RECOVERY.toDouble() / CYCLE_MINUTES.toDouble())).roundToInt()

            return (fullCycles * FULL_CYCLE_RECOVERY) + remainderRecovery
        }
    }
}
