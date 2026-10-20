package com.neuroflow.app.domain.model

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralObservation
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import java.time.Instant
import java.time.LocalDate
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

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> {
    val dayMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    return slots
        .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayMillis) }
        .map { slot ->
            val category = when (slot.category) {
                PtsCategory.MEAL -> CognitiveBlockCategory.MEAL
                PtsCategory.EXERCISE -> CognitiveBlockCategory.EXERCISE
                PtsCategory.WORK, PtsCategory.STUDY -> CognitiveBlockCategory.FOCUS_WORK
                PtsCategory.BREAK, PtsCategory.MINDFULNESS -> CognitiveBlockCategory.WIND_DOWN
                else -> CognitiveBlockCategory.WIND_DOWN
            }
            val startMin = slot.startMinuteOfDay
            val endMin = (startMin + slot.durationMinutes) % 1440
            CognitiveBlockWindow(
                startMinuteOfDay = startMin,
                endMinuteOfDay = endMin,
                category = category,
                intensity = 1.0,
                label = slot.label
            )
        }
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    dayMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> = buildDailyCognitiveBlocks(
    slots = slots,
    date = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate(),
    zoneId = zoneId
)

fun buildBehavioralObservations(
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<BehavioralObservation> {
    val observations = mutableListOf<BehavioralObservation>()

    for (session in sessions) {
        if (session.startedAt > 0L) {
            val zdt = Instant.ofEpochMilli(session.startedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val durationMins = if (session.endedAt != null && session.endedAt > session.startedAt) {
                ((session.endedAt - session.startedAt) / 60_000L).toDouble()
            } else if (session.durationMinutes > 0f) {
                session.durationMinutes.toDouble()
            } else {
                25.0
            }
            val weight = (durationMins / 30.0).coerceIn(0.5, 3.0)
            observations.add(
                BehavioralObservation(
                    minuteOfDay = minuteOfDay,
                    productivityRating = 0.85,
                    weight = weight
                )
            )
        }
    }

    for (task in completedTasks) {
        val completedAt = task.completedAt ?: task.updatedAt
        if (completedAt > 0L) {
            val zdt = Instant.ofEpochMilli(completedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val weight = when (task.energyLevel) {
                EnergyLevel.HIGH -> 1.5
                EnergyLevel.MEDIUM -> 1.0
                EnergyLevel.LOW -> 0.6
                else -> 1.0
            }
            val rating = when (task.energyLevel) {
                EnergyLevel.HIGH -> 0.95
                EnergyLevel.MEDIUM -> 0.80
                EnergyLevel.LOW -> 0.65
                else -> 0.80
            }
            observations.add(
                BehavioralObservation(
                    minuteOfDay = minuteOfDay,
                    productivityRating = rating,
                    weight = weight
                )
            )
        }
    }

    return observations
}

fun computeEnergyUiState(
    circadianEnergyService: CircadianEnergyService,
    sleepTelemetryRepository: SleepTelemetryRepository?,
    prefs: UserPreferences,
    sleepLogs: List<SleepLogEntity> = emptyList(),
    continuousActiveFocusMinutes: Int = 0,
    nowMillis: Long = System.currentTimeMillis(),
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList()
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
    val naps = signal?.daytimeNaps ?: emptyList()

    // 7-day rolling average sleep minutes for allostatic debt
    val sevenDaysAgo = nowMillis - 7 * 86_400_000L
    val recentSleepLogs = sleepLogs.filter { it.startAt >= sevenDaysAgo || it.endAt >= sevenDaysAgo }
    val rollingAverageSleepMinutes = if (recentSleepLogs.isNotEmpty()) {
        recentSleepLogs.map { if (it.durationMinutes > 0) it.durationMinutes.toDouble() else (it.endAt - it.startAt) / 60_000.0 }.average()
    } else {
        480.0
    }

    // Chronotype from preferences (quizChronotype or manualChronotype or derived from MEQ score)
    val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.quizChronotype)
    } else if (!prefs.manualChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.manualChronotype)
    } else if (prefs.meqScore > 0) {
        when {
            prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
            prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
            prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
            prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
            else -> ChronotypeProfile.DEFINITE_EVENING
        }
    } else {
        ChronotypeProfile.INTERMEDIATE
    }

    val manualPeakHour = if (prefs.manualPeakProfileEnabled) {
        (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
    } else null

    // Cognitive blocks for today
    val cognitiveBlocks = buildDailyCognitiveBlocks(
        slots = prefs.protectedTimeSlots,
        date = zonedDateTime.toLocalDate(),
        zoneId = zoneId
    )

    // Behavioral observations and learned profile
    val observations = buildBehavioralObservations(completedTasks, sessions, zoneId)
    val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
    val learnedProfile = if (observations.isNotEmpty()) {
        AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
    } else null

    val hasAdvancedParameters = naps.isNotEmpty() ||
            cognitiveBlocks.isNotEmpty() ||
            learnedProfile != null ||
            (rollingAverageSleepMinutes != 480.0) ||
            manualPeakHour != null ||
            chronotype != ChronotypeProfile.INTERMEDIATE

    val snapshot = if (hasAdvancedParameters) {
        if (cognitiveBlocks.isEmpty() && learnedProfile == null && rollingAverageSleepMinutes == 480.0 && manualPeakHour == null && chronotype == ChronotypeProfile.INTERMEDIATE) {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps
            )
        } else {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks
            )
        }
    } else {
        circadianEnergyService.evaluateSnapshot(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId
        )
    }

    val energyUi = buildEnergyUiModel(
        usableEnergy = snapshot.usableEnergy,
        circadianFactor = snapshot.circadianAlertness.toFloat(),
        sleepPressureFactor = snapshot.homeostaticSleepPressure.toFloat(),
        sleepPressurePoints = prefs.sleepPressurePoints,
        refreshedAtMillis = nowMillis
    )

    return Pair(snapshot, energyUi)
}

