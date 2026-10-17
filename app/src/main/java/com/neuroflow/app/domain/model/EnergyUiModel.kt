package com.neuroflow.app.domain.model

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class PeakWindow(
    val startMinuteOffset: Int = 0,
    val durationMinutes: Int = 180,
    val amplitude: Float = 1.0f
)

data class CircadianProfile(
    val windows: List<PeakWindow> = emptyList(),
    val phaseShiftMinutes: Int = 0
)

enum class ProfileType {
    WORKDAY,
    WEEKEND
}

data class ConfidenceComponents(
    val sleepCoverage: Float = 0.8f,
    val wakeConsistency: Float = 0.8f,
    val behaviorPerformance: Float = 0.8f,
    val overall: Float = 0.8f
)

data class EffectivePeakProfile(
    val profileType: ProfileType = ProfileType.WORKDAY,
    val anchorMinuteOfDay: Int = 150,
    val windows: List<PeakWindow> = emptyList(),
    val confidence: ConfidenceComponents = ConfidenceComponents(),
    val windowConfidences: List<Float> = emptyList(),
    val explanation: String = "",
    val driftStatus: String = "stable",
    val weeklyBacktestErrorMinutes: Float? = null,
    val confidenceGatedAbstention: Boolean = false,
    val adaptiveFreezeMode: Boolean = false,
    val abstentionReason: String = ""
)

data class EnergyUiModel(
    val availableEnergy: Int = 50,
    val rawEnergy: Float = 50f,
    val baselineRawEnergy: Float = 50f,
    val peakScore: Float = 50f,
    val fatiguePenalty: Float = 0f,
    val momentAdjustment: Float = 0f,
    val momentConfidence: Float = 1f,
    val currentPeakValue: Int = 4000,
    val peakValue: Int = 4000,
    val peakDrop: Int = 0,
    val minutesSincePeak: Int = 0,
    val minutesUntilPeakReset: Int = 0,
    val sleepPressurePoints: Int = 0,
    val fatiguePercent: Int = 0,
    val fatigueZone: FatigueZone = FatigueZone.RESTED,
    val circadianFactor: Float = 1f,
    val reservoirFactor: Float = 1f,
    val confidenceFactor: Float = 1f,
    val effectivePeakProfile: EffectivePeakProfile? = null,
    val momentSignalSummary: String = "",
    val peakDetectionAgeMillis: Long = 0L,
    val sessionDataAgeMillis: Long = 0L,
    val sleepLogDataAgeMillis: Long = 0L,
    val taskDataAgeMillis: Long = 0L,
    val overallFreshnessAgeMillis: Long = 0L,
    val hasRecentData: Boolean = true,
    val refreshedAtMillis: Long = System.currentTimeMillis()
)

fun buildEnergyUiModel(
    usableEnergy: Int,
    circadianFactor: Float = 0.85f,
    sleepPressureFactor: Float = 0.25f,
    sleepPressurePoints: Int = 0,
    fatigueZone: FatigueZone = if (sleepPressurePoints > 0) FatigueZone.fromPressurePoints(sleepPressurePoints) else FatigueZone.fromUsableEnergy(usableEnergy),
    refreshedAtMillis: Long = System.currentTimeMillis()
): EnergyUiModel {
    val usable = usableEnergy.coerceIn(0, 100)
    val fatiguePct = (sleepPressureFactor * 100).toInt().coerceIn(0, 100)
    return EnergyUiModel(
        availableEnergy = usable,
        rawEnergy = usable.toFloat(),
        baselineRawEnergy = usable.toFloat(),
        peakScore = usable.toFloat(),
        sleepPressurePoints = sleepPressurePoints,
        fatiguePercent = fatiguePct,
        fatigueZone = fatigueZone,
        circadianFactor = circadianFactor.coerceIn(0f, 1f),
        reservoirFactor = 1.0f,
        confidenceFactor = 1.0f,
        hasRecentData = true,
        refreshedAtMillis = refreshedAtMillis
    )
}

fun computeEnergyUiState(
    circadianEnergyService: CircadianEnergyService,
    sleepTelemetryRepository: SleepTelemetryRepository?,
    prefs: UserPreferences,
    sleepLogs: List<SleepLogEntity> = emptyList(),
    nowMillis: Long = System.currentTimeMillis()
): Pair<CircadianSnapshot, EnergyUiModel> {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
    if (anchor.toInstant().toEpochMilli() > nowMillis) {
        anchor = anchor.minusDays(1)
    }
    val baseWakeMillis = anchor.toInstant().toEpochMilli()

    val signal = sleepTelemetryRepository?.normalizeLogs(
        logs = sleepLogs,
        baseWakeMillis = baseWakeMillis,
        baseSleepMinutes = 480.0
    )

    val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
    val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

    val snapshot = circadianEnergyService.evaluateSnapshot(
        timestampMillis = nowMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = sleepDurationMinutes,
        zoneId = zoneId
    )

    val energyUi = buildEnergyUiModel(
        usableEnergy = snapshot.usableEnergy,
        circadianFactor = snapshot.circadianAlertness.toFloat(),
        sleepPressureFactor = snapshot.homeostaticSleepPressure.toFloat(),
        sleepPressurePoints = prefs.sleepPressurePoints,
        refreshedAtMillis = nowMillis
    )

    return Pair(snapshot, energyUi)
}

