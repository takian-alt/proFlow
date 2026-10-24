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
import kotlin.math.max
import kotlin.math.min
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
    fatigueZone: FatigueZone? = null,
    refreshedAtMillis: Long = System.currentTimeMillis(),
    baselineBiologicalEnergy: Int = usableEnergy,
    physiologicalImpulse: Double = 0.0,
    learnedAdjustmentPoints: Double = 0.0,
    predictionConfidence: Double = 1.0,
    effectivePeakProfile: EffectivePeakProfile? = null
): EnergyUiModel {
    val usable = usableEnergy.coerceIn(0, 100)
    val fatiguePct = (sleepPressureFactor * 100).toInt().coerceIn(0, 100)
    val effectivePressurePoints = if (sleepPressurePoints > 0) {
        sleepPressurePoints
    } else {
        (sleepPressureFactor.coerceIn(0f, 1f) * FatigueZone.SOFT_MAX_REFERENCE).roundToInt()
    }
    val resolvedZone = fatigueZone ?: if (sleepPressurePoints > 0) {
        FatigueZone.fromPressurePoints(sleepPressurePoints)
    } else {
        when {
            fatiguePct <= 24 -> FatigueZone.RESTED
            fatiguePct <= 49 -> FatigueZone.MODERATE
            fatiguePct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    }
    val peakVal = 4000
    val curPeakVal = (usable * 40).coerceIn(0, 4000)
    return EnergyUiModel(
        availableEnergy = usable,
        rawEnergy = usable.toFloat(),
        baselineRawEnergy = baselineBiologicalEnergy.toFloat(),
        peakScore = usable.toFloat(),
        fatiguePenalty = (sleepPressureFactor * 100f).coerceIn(0f, 100f),
        momentAdjustment = (physiologicalImpulse + learnedAdjustmentPoints).toFloat(),
        momentConfidence = predictionConfidence.toFloat(),
        currentPeakValue = curPeakVal,
        peakValue = peakVal,
        peakDrop = maxOf(0, peakVal - curPeakVal),
        minutesSincePeak = 0,
        minutesUntilPeakReset = 0,
        sleepPressurePoints = effectivePressurePoints,
        fatiguePercent = fatiguePct,
        fatigueZone = resolvedZone,
        circadianFactor = circadianFactor.coerceIn(0f, 1f),
        reservoirFactor = 1.0f,
        confidenceFactor = predictionConfidence.toFloat(),
        effectivePeakProfile = effectivePeakProfile,
        momentSignalSummary = if (physiologicalImpulse != 0.0) String.format(java.util.Locale.ROOT, "Impulse: %+.1f pts", physiologicalImpulse) else "",
        overallFreshnessAgeMillis = maxOf(0L, System.currentTimeMillis() - refreshedAtMillis),
        hasRecentData = true,
        refreshedAtMillis = refreshedAtMillis
    )
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> {
    // Bug 5 Fix (ADR-025): Populate explicitStartMillis and explicitEndMillis across both yesterday and today
    // so late-evening meals taper off smoothly across midnight without circular wrap-around phantom dips.
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStart = targetDate.atStartOfDay(zoneId)
        val dayMillis = dayStart.toInstant().toEpochMilli()
        slots
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
                val startZdt = targetDate.atTime((startMin / 60).coerceIn(0, 23), (startMin % 60).coerceIn(0, 59)).atZone(zoneId)
                val totalEndMinutes = startMin + slot.durationMinutes
                val endZdt = if (totalEndMinutes < 1440) {
                    targetDate.atTime((totalEndMinutes / 60).coerceIn(0, 23), (totalEndMinutes % 60).coerceIn(0, 59)).atZone(zoneId)
                } else {
                    targetDate.plusDays((totalEndMinutes / 1440).toLong())
                        .atTime(((totalEndMinutes % 1440) / 60).coerceIn(0, 23), ((totalEndMinutes % 1440) % 60).coerceIn(0, 59))
                        .atZone(zoneId)
                }
                val startMillis = startZdt.toInstant().toEpochMilli()
                val endMillis = endZdt.toInstant().toEpochMilli()
                CognitiveBlockWindow(
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    category = category,
                    intensity = 1.0,
                    label = slot.label,
                    explicitStartMillis = startMillis,
                    explicitEndMillis = endMillis
                )
            }
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

fun extractStimulantDosesFromSlots(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<com.neuroflow.app.domain.service.StimulantDoseBout> {
    // Bug 3 Fix: Evaluate both yesterday and today so late-evening doses remain in 24h PK window across midnight
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStartMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        slots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .mapNotNull { slot ->
                val label = slot.label.lowercase()
                val words = label.split(Regex("[^a-z0-9_]+")).filter { it.isNotBlank() }.toSet()
                val type = when {
                    words.any { it in setOf("tea", "theanine", "matcha", "greentea") } && !label.contains("chamomile") && !label.contains("herbal") ->
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE
                    words.any { it in setOf("coffee", "caffeine", "espresso", "latte", "cappuccino", "coldbrew", "americano") } ->
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE
                    words.any { it in setOf("ritalin", "methylphenidate") } || label.contains("meds ir") || label.contains("stimulant ir") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR
                    words.any { it in setOf("adderall", "vyvanse", "concerta", "amphetamine", "dexedrine") } || label.contains("meds xr") || label.contains("stimulant xr") || words.contains("meds") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR
                    else -> null
                } ?: return@mapNotNull null

                val doseZdt = targetDate.atTime((slot.startMinuteOfDay / 60).coerceIn(0, 23), (slot.startMinuteOfDay % 60).coerceIn(0, 59)).atZone(zoneId)
                val doseMillis = doseZdt.toInstant().toEpochMilli()
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = doseMillis,
                    doseMg = when (type) {
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE -> 100.0
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE -> 50.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR -> 10.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR -> 20.0
                    },
                    type = type
                )
            }
    }.distinctBy { it.timestampMillis to it.type }
}

fun buildBehavioralObservations(
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<BehavioralObservation> {
    data class TimedObservation(
        val timestampMillis: Long,
        val observation: BehavioralObservation
    )

    val timedObservations = mutableListOf<TimedObservation>()

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
            // Feature 2 (ADR-025): Cognitive demand weighting (2.0x for deep work or >= 45m)
            val isDeepFocus = session.sessionType == "DEEP_WORK" || durationMins >= 45.0
            val demandMultiplier = if (isDeepFocus) 2.0 else 1.0
            val weight = ((durationMins / 30.0) * demandMultiplier).coerceIn(0.5, 6.0)
            timedObservations.add(
                TimedObservation(
                    timestampMillis = session.startedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = if (isDeepFocus) 0.95 else 0.85,
                        weight = weight
                    )
                )
            )
        }
    }

    for (task in completedTasks) {
        // Bug 3 Fix (ADR-025): Exclude low-energy chores from shifting the peak-hour state estimate
        if (task.energyLevel == EnergyLevel.LOW) continue

        val completedAt = task.completedAt ?: task.updatedAt
        if (completedAt > 0L) {
            val zdt = Instant.ofEpochMilli(completedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val weight = when (task.energyLevel) {
                EnergyLevel.HIGH -> 1.5
                EnergyLevel.MEDIUM -> 1.0
                else -> 1.0
            }
            val rating = when (task.energyLevel) {
                EnergyLevel.HIGH -> 0.95
                EnergyLevel.MEDIUM -> 0.80
                else -> 0.80
            }
            timedObservations.add(
                TimedObservation(
                    timestampMillis = completedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = rating,
                        weight = weight
                    )
                )
            )
        }
    }

    // Bug 3 Fix (ADR-025): Sort chronologically before sequential Markov-chain Kalman update
    return timedObservations.sortedBy { it.timestampMillis }.map { it.observation }
}

/**
 * Computes 7-day rolling daily sleep average by aggregating total sleep per calendar day
 * (preventing daytime naps from artificially halving the per-entry average).
 */
fun computeRollingDailySleepAverage(
    sleepLogs: List<SleepLogEntity>,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Double {
    val sevenDaysAgo = nowMillis - 7 * 86_400_000L
    val recentSleepLogs = sleepLogs.filter { it.endAt > sevenDaysAgo && it.startAt < nowMillis && it.endAt > it.startAt }
    if (recentSleepLogs.isEmpty()) return 480.0

    // Sweep-line merge overlapping sleep intervals across the 7-day window to prevent wearable + manual double-counting
    val rawIntervals = recentSleepLogs.map { max(sevenDaysAgo, it.startAt) to min(nowMillis, it.endAt) }
        .filter { it.second > it.first }
        .sortedBy { it.first }

    val mergedIntervals = mutableListOf<Pair<Long, Long>>()
    for (interval in rawIntervals) {
        if (mergedIntervals.isEmpty()) {
            mergedIntervals.add(interval)
        } else {
            val last = mergedIntervals.last()
            if (interval.first <= last.second) {
                mergedIntervals[mergedIntervals.size - 1] = last.first to max(last.second, interval.second)
            } else {
                mergedIntervals.add(interval)
            }
        }
    }

    val minutesByLocalDay = mutableMapOf<LocalDate, Double>()
    for ((start, end) in mergedIntervals) {
        val mins = (end - start) / 60_000.0
        val wakeDay = Instant.ofEpochMilli(end).atZone(zoneId).toLocalDate()
        minutesByLocalDay[wakeDay] = (minutesByLocalDay[wakeDay] ?: 0.0) + mins
    }
    return if (minutesByLocalDay.isNotEmpty()) {
        minutesByLocalDay.values.average().coerceIn(120.0, 720.0)
    } else {
        480.0
    }
}

fun computeEnergyUiState(
    circadianEnergyService: CircadianEnergyService,
    sleepTelemetryRepository: SleepTelemetryRepository?,
    prefs: UserPreferences,
    sleepLogs: List<SleepLogEntity> = emptyList(),
    continuousActiveFocusMinutes: Int = 0,
    nowMillis: Long = System.currentTimeMillis(),
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    stimulantDoses: List<com.neuroflow.app.domain.service.StimulantDoseBout> = emptyList()
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

    val dayStartMillis = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    val physLogs = com.neuroflow.app.data.local.parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }

    val quickPhysBlocks = physLogs.mapNotNull { log ->
        val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
        val minOfDay = logZdt.hour * 60 + logZdt.minute
        val duration = log.durationMinutes.coerceIn(5, 240)
        when (log.type) {
            com.neuroflow.app.data.local.PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.MEAL,
                intensity = log.intensity,
                label = "Quick Logged Meal #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.EXERCISE,
                intensity = log.intensity,
                label = "Quick Logged Workout #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.NAP -> null
        }
    }

    val quickPhysNaps = physLogs
        .filter { it.type == com.neuroflow.app.data.local.PhysiologicalEventType.NAP }
        .map { log ->
            val napDuration = log.durationMinutes.coerceIn(5, 180)
            com.neuroflow.app.domain.service.DaytimeNapBout(
                startMillis = log.timestampMillis,
                endMillis = log.timestampMillis + napDuration * 60_000L
            )
        }

    val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

    val rollingAverageSleepMinutes = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

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

    val cognitiveBlocks = buildDailyCognitiveBlocks(
        slots = prefs.protectedTimeSlots,
        date = zonedDateTime.toLocalDate(),
        zoneId = zoneId
    ) + quickPhysBlocks

    val observations = buildBehavioralObservations(completedTasks, sessions, zoneId)
    val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
    val learnedProfile = if (observations.isNotEmpty()) {
        AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
    } else null

    // Bug 2 Fix (ADR-030): Unify stimulant dose resolution across logs, slots, and explicit parameters
    val effectiveStimulantDoses = (
        stimulantDoses +
        com.neuroflow.app.data.local.parseStimulantLogs(prefs.stimulantLogsJson) +
        extractStimulantDosesFromSlots(prefs.protectedTimeSlots, zonedDateTime.toLocalDate(), zoneId)
    ).distinctBy { it.timestampMillis to it.type }

    // Feature 3 (ADR-023) & Bug 2 Fix: compute and pass social jetlag phase shift
    val sleepWindows = sleepLogs.map { com.neuroflow.app.domain.service.TimeSlotWindow(it.startAt, it.endAt) }
    val sjlReport = com.neuroflow.app.domain.service.SocialJetlagCalculator.evaluateFromSleepWindows(
        sleepWindows = sleepWindows,
        nowMillis = nowMillis,
        zoneId = zoneId
    )
    val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

    val hasAdvancedParameters = naps.isNotEmpty() ||
        cognitiveBlocks.isNotEmpty() ||
        learnedProfile != null ||
        (rollingAverageSleepMinutes != 480.0) ||
        manualPeakHour != null ||
        chronotype != ChronotypeProfile.INTERMEDIATE ||
        effectiveStimulantDoses.isNotEmpty() ||
        kotlin.math.abs(socialJetlagLagHours) > 0.0

    val snapshot = if (effectiveStimulantDoses.isNotEmpty() || kotlin.math.abs(socialJetlagLagHours) > 0.0) {
        circadianEnergyService.evaluateSnapshotWithStimulants(
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
            cognitiveBlocks = cognitiveBlocks,
            stimulantDoses = effectiveStimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
    } else if (hasAdvancedParameters) {
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

    // Bug 7 Fix (ADR-026): When daytime power naps or stimulant adenosine masking are active,
    // homeostatic pressure has diverged from unadjusted static point counters. Derive fatigueZone
    // from snapshot.homeostaticSleepPressure rather than leaving it stuck on prefs.sleepPressurePoints.
    val isHomeostaticAdjusted = naps.isNotEmpty() || effectiveStimulantDoses.isNotEmpty()
    val resolvedFatigueZone = if (isHomeostaticAdjusted) {
        val pct = (snapshot.homeostaticSleepPressure * 100).toInt().coerceIn(0, 100)
        when {
            pct <= 24 -> FatigueZone.RESTED
            pct <= 49 -> FatigueZone.MODERATE
            pct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    } else null

    val effectiveProfile = if (prefs.manualPeakProfileEnabled) {
        val w1 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow1StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow1DurationMinutes,
            amplitude = prefs.manualPeakWindow1Amplitude
        )
        val w2 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow2StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow2DurationMinutes,
            amplitude = prefs.manualPeakWindow2Amplitude
        )
        val w3 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow3StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow3DurationMinutes,
            amplitude = prefs.manualPeakWindow3Amplitude
        )
        EffectivePeakProfile(
            profileType = if (prefs.manualPeakProfileType == "WEEKEND") ProfileType.WEEKEND else ProfileType.WORKDAY,
            anchorMinuteOfDay = prefs.manualPeakAnchorMinuteOfDay,
            windows = listOf(w1, w2, w3),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence
            ),
            windowConfidences = listOf(0.9f, 0.85f, 0.8f),
            explanation = "Manual Peak Profile Active",
            driftStatus = "configured",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    } else {
        val peakAnchorMin = prefs.detectedPeakMinuteOfDay.takeIf { it > 0 } ?: (prefs.peakEnergyStart * 60)
        val rawSpan = ((prefs.peakEnergyEnd - prefs.peakEnergyStart) % 24 + 24) % 24
        val safePeakDurationHours = (if (rawSpan == 0) 4 else rawSpan).coerceIn(1, 8)
        EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = peakAnchorMin,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = safePeakDurationHours * 60, amplitude = 1.0f)
            ),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence.takeIf { it > 0f } ?: 0.85f
            ),
            windowConfidences = listOf(0.85f),
            explanation = "Automated Circadian Calibration",
            driftStatus = "synchronized",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    }

    val energyUi = buildEnergyUiModel(
        usableEnergy = snapshot.usableEnergy,
        circadianFactor = snapshot.circadianAlertness.toFloat(),
        sleepPressureFactor = snapshot.homeostaticSleepPressure.toFloat(),
        sleepPressurePoints = prefs.sleepPressurePoints,
        fatigueZone = resolvedFatigueZone,
        refreshedAtMillis = nowMillis,
        baselineBiologicalEnergy = snapshot.baselineBiologicalEnergy,
        physiologicalImpulse = snapshot.physiologicalImpulse,
        learnedAdjustmentPoints = snapshot.learnedAdjustmentPoints,
        predictionConfidence = snapshot.predictionConfidence,
        effectivePeakProfile = effectiveProfile
    )

    return Pair(snapshot, energyUi)
}
