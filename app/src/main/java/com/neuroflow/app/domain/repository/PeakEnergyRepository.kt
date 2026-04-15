package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.MEQChronotypeDetector
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wires PeakEnergyEngine with user preferences.
 * Provides the current peak energy value based on user's chronotype and wake time.
 */
@Singleton
class PeakEnergyRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepLogRepository: SleepLogRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository
) {
    companion object {
        private const val LOOKBACK_DAYS = 14
        private const val RECENCY_LAMBDA = 0.18
        private const val QUALITY_MINUTES = 20f
        private const val ABORT_MINUTES = 12f
        private const val COMPLETION_PROXIMITY_MINUTES = 120L
        private const val MAX_DISTRACTION_SCORE = 100f
    }
    /**
     * Provides a flow of peak energy detection results based on current preferences.
     * Updates whenever user's chronotype or wake time changes.
     */
    val peakEnergyFlow: Flow<PeakEnergyEngine.DetectionResult> = preferencesDataStore.preferencesFlow.map { prefs ->
        buildDetectionFromPreferences(
            quizPeakEnabled = prefs.quizPeakEnabled,
            quizChronotype = prefs.quizChronotype,
            manualChronotype = prefs.manualChronotype,
            wakeUpHour = prefs.wakeUpHour,
            sleepHour = prefs.sleepHour,
            sleepPressurePoints = prefs.sleepPressurePoints,
            manualPeakStart = prefs.peakEnergyStart,
            manualPeakEnd = prefs.peakEnergyEnd,
            profileOverrideEnabled = prefs.manualPeakProfileEnabled,
            profileOverrideType = prefs.manualPeakProfileType,
            profileOverrideAnchorMinute = prefs.manualPeakAnchorMinuteOfDay,
            manualWindow1DurationMinutes = prefs.manualPeakWindow1DurationMinutes,
            manualWindow2DurationMinutes = prefs.manualPeakWindow2DurationMinutes,
            manualWindow3DurationMinutes = prefs.manualPeakWindow3DurationMinutes,
            manualWindow1Amplitude = prefs.manualPeakWindow1Amplitude,
            manualWindow2Amplitude = prefs.manualPeakWindow2Amplitude,
            manualWindow3Amplitude = prefs.manualPeakWindow3Amplitude
        )
    }
    
    /**
     * Get the peak energy result with all metadata (peak time, decay value, etc.)
     */
    suspend fun getPeakEnergyDetection(): PeakEnergyEngine.DetectionResult {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return buildDetectionFromPreferences(
            quizPeakEnabled = prefs.quizPeakEnabled,
            quizChronotype = prefs.quizChronotype,
            manualChronotype = prefs.manualChronotype,
            wakeUpHour = prefs.wakeUpHour,
            sleepHour = prefs.sleepHour,
            sleepPressurePoints = prefs.sleepPressurePoints,
            manualPeakStart = prefs.peakEnergyStart,
            manualPeakEnd = prefs.peakEnergyEnd,
            profileOverrideEnabled = prefs.manualPeakProfileEnabled,
            profileOverrideType = prefs.manualPeakProfileType,
            profileOverrideAnchorMinute = prefs.manualPeakAnchorMinuteOfDay,
            manualWindow1DurationMinutes = prefs.manualPeakWindow1DurationMinutes,
            manualWindow2DurationMinutes = prefs.manualPeakWindow2DurationMinutes,
            manualWindow3DurationMinutes = prefs.manualPeakWindow3DurationMinutes,
            manualWindow1Amplitude = prefs.manualPeakWindow1Amplitude,
            manualWindow2Amplitude = prefs.manualPeakWindow2Amplitude,
            manualWindow3Amplitude = prefs.manualPeakWindow3Amplitude
        )
    }

    private suspend fun buildDetectionFromPreferences(
        quizPeakEnabled: Boolean,
        quizChronotype: String?,
        manualChronotype: String?,
        wakeUpHour: Int,
        sleepHour: Int,
        sleepPressurePoints: Int,
        manualPeakStart: Int,
        manualPeakEnd: Int,
        profileOverrideEnabled: Boolean,
        profileOverrideType: String,
        profileOverrideAnchorMinute: Int,
        manualWindow1DurationMinutes: Int,
        manualWindow2DurationMinutes: Int,
        manualWindow3DurationMinutes: Int,
        manualWindow1Amplitude: Float,
        manualWindow2Amplitude: Float,
        manualWindow3Amplitude: Float
    ): PeakEnergyEngine.DetectionResult {
        val parsedQuizChronotype = parseChronotype(quizChronotype)
        val profileOverride = buildProfileOverride(
            enabled = profileOverrideEnabled,
            typeRaw = profileOverrideType,
            anchorMinute = profileOverrideAnchorMinute,
            w1Duration = manualWindow1DurationMinutes,
            w2Duration = manualWindow2DurationMinutes,
            w3Duration = manualWindow3DurationMinutes,
            w1Amplitude = manualWindow1Amplitude,
            w2Amplitude = manualWindow2Amplitude,
            w3Amplitude = manualWindow3Amplitude
        )
        maybeAutoTuneMorningWeights()
        if (quizPeakEnabled && parsedQuizChronotype != null) {
            val profileType = currentProfileType()
            val morningSignals = if (
                parsedQuizChronotype == MEQChronotypeDetector.Chronotype.DEFINITE_MORNING ||
                parsedQuizChronotype == MEQChronotypeDetector.Chronotype.MODERATE_MORNING
            ) {
                loadMorningPersonalizationSignals(
                    profileType = profileType,
                    wakeUpHour = wakeUpHour,
                    chronotype = parsedQuizChronotype,
                    profileOverride = profileOverride
                )
            } else {
                null
            }
            val meqResult = toMeqResult(parsedQuizChronotype)
            return PeakEnergyEngine.detect(
                meqResult = meqResult,
                wakeUpHour = wakeUpHour,
                sleepHour = sleepHour,
                sleepPressurePoints = sleepPressurePoints,
                personalizationSignals = morningSignals
            )
        }

        val fallbackChronotype = parseChronotype(manualChronotype)
            ?: parsedQuizChronotype
            ?: MEQChronotypeDetector.Chronotype.INTERMEDIATE

        val peakMinuteOfDay = midpointMinuteOfDay(manualPeakStart, manualPeakEnd)
        val safeWakeHour = normalizeHour(wakeUpHour)
        val offsetMinutes = minutesDifferenceWrapped(startMinute = safeWakeHour * 60, endMinute = peakMinuteOfDay)

        return PeakEnergyEngine.DetectionResult(
            chronotype = fallbackChronotype,
            wakeUpHour = safeWakeHour,
            peakOffsetHours = offsetMinutes / 60f,
            peakHourOfDay = peakMinuteOfDay / 60,
            peakMinuteOfDay = peakMinuteOfDay,
            peakValue = PeakEnergyEngine.PEAK_VALUE,
            confidence = 1f,
            circadianProfile = PeakEnergyEngine.defaultCircadianProfile(fallbackChronotype)
        )
    }

    private suspend fun loadMorningPersonalizationSignals(
        profileType: PeakEnergyEngine.ProfileType,
        wakeUpHour: Int,
        chronotype: MEQChronotypeDetector.Chronotype,
        profileOverride: PeakEnergyEngine.ProfileOverride?,
        lookbackDays: Int = LOOKBACK_DAYS
    ): PeakEnergyEngine.MorningPersonalizationSignals {
        val allLogs = sleepLogRepository.getAll()
        val allSessions = sessionRepository.getAllSessions()
        val allTasks = taskRepository.getAllTasks()
        if (allLogs.isEmpty() && allSessions.isEmpty()) {
            return PeakEnergyEngine.MorningPersonalizationSignals(
                profileType = profileType,
                profileOverride = profileOverride
            )
        }

        val cutoffMillis = System.currentTimeMillis() - (lookbackDays * 24L * 60L * 60L * 1000L)
        val recentLogs = allLogs
            .asSequence()
            .filter { it.endAt >= cutoffMillis }
            .filter { profileTypeForMillis(it.endAt) == profileType }
            .sortedByDescending { it.startAt }
            .take(lookbackDays)
            .toList()

        val averageSleepMinutes = recentLogs
            .takeIf { it.isNotEmpty() }
            ?.map { it.durationMinutes.coerceIn(60, 16 * 60) }
            ?.average()
            ?.roundToInt()

        val wakeVarianceMinutes = circularWakeVarianceMinutes(recentLogs).takeIf { recentLogs.isNotEmpty() }
        val expectedCoverage = lookbackDays.coerceAtLeast(1).toFloat()
        val coverage = (recentLogs.size / expectedCoverage).coerceIn(0f, 1f)
        val behavior = behaviorSignals(
            sessions = allSessions,
            tasks = allTasks,
            cutoffMillis = cutoffMillis,
            profileType = profileType,
            lookbackDays = lookbackDays
        )
        val baselineAnchorMinuteOfDay = (
            normalizeHour(wakeUpHour) * 60 +
                (PeakEnergyEngine.chronotypeOffset(chronotype) * 60f).roundToInt()
            ) % (24 * 60)
        val driftMinutes = driftMinutes(
            slots = behavior.slots,
            baselineAnchorMinute = baselineAnchorMinuteOfDay
        )
        val coldStart = coldStartFactor(behavior.coverage, coverage)
        val weeklyBacktestErrorMinutes = weeklyBacktestError(
            slots = behavior.slots,
            baselineAnchorMinute = baselineAnchorMinuteOfDay
        )
        val prefs = preferencesDataStore.preferencesFlow.first()
        val tuning = PeakEnergyEngine.TuningCoefficients(
            sleepWeight = prefs.morningTuneSleepWeight,
            wakeWeight = prefs.morningTuneWakeWeight,
            behaviorWeight = prefs.morningTuneBehaviorWeight,
            baseWeight = prefs.morningTuneBaseWeight
        )

        return PeakEnergyEngine.MorningPersonalizationSignals(
            averageSleepMinutes = averageSleepMinutes,
            wakeVarianceMinutes = wakeVarianceMinutes,
            sleepLogCoverage = coverage,
            profileType = profileType,
            baselineAnchorMinuteOfDay = baselineAnchorMinuteOfDay,
            behaviorSlots = behavior.slots,
            taskTypePerformance = behavior.taskTypePerformance,
            behaviorCoverage = behavior.coverage,
            recentWeight = behavior.recentWeight,
            coldStartFactor = coldStart,
            driftMinutes = driftMinutes,
            weeklyBacktestErrorMinutes = weeklyBacktestErrorMinutes,
            tuning = tuning,
            profileOverride = profileOverride
        )
    }

    private data class BehaviorSignals(
        val slots: List<PeakEnergyEngine.SlotPerformanceAggregate>,
        val taskTypePerformance: List<PeakEnergyEngine.TaskTypePerformanceAggregate>,
        val coverage: Float,
        val recentWeight: Float
    )

    private suspend fun behaviorSignals(
        sessions: List<TimeSessionEntity>,
        tasks: List<TaskEntity>,
        cutoffMillis: Long,
        profileType: PeakEnergyEngine.ProfileType,
        lookbackDays: Int
    ): BehaviorSignals {
        val tasksById = tasks.associateBy { it.id }
        data class Acc(
            var weight: Double = 0.0,
            var completion: Double = 0.0,
            var abort: Double = 0.0,
            var distraction: Double = 0.0,
            var samples: Int = 0
        )
        val bucket = mutableMapOf<Int, Acc>()
        data class TaskTypeAcc(
            var weightedBucketSum: Double = 0.0,
            var weightedQualitySum: Double = 0.0,
            var weight: Double = 0.0,
            var samples: Int = 0
        )
        val taskTypeBuckets = mutableMapOf<String, TaskTypeAcc>()
        var totalWeightedSamples = 0.0
        var recentWeighted = 0.0

        sessions.asSequence()
            .filter { it.startedAt >= cutoffMillis }
            .filter { it.endedAt != null }
            .filter { profileTypeForMillis(it.startedAt) == profileType }
            .forEach { session ->
                val endedAt = session.endedAt ?: return@forEach
                val task = tasksById[session.taskId]
                val duration = session.durationMinutes.coerceAtLeast(0f)
                val isAbort = duration in 0f..ABORT_MINUTES
                val distractionScore = (task?.distractionScore ?: -1f).let {
                    if (it < 0f) 0.3f else (it / MAX_DISTRACTION_SCORE).coerceIn(0f, 1f)
                }
                val completion = if (task != null && task.completedAt != null) {
                    val deltaMinutes = kotlin.math.abs(task.completedAt - endedAt) / 60_000L
                    if (deltaMinutes <= COMPLETION_PROXIMITY_MINUTES) 1f else 0f
                } else {
                    0f
                }
                val qualityWeight = when {
                    duration >= QUALITY_MINUTES && distractionScore <= 0.5f -> 1f
                    duration >= (QUALITY_MINUTES * 0.7f) -> 0.7f
                    else -> 0.35f
                }
                val recencyWeight = recencyWeight(session.startedAt, lookbackDays)
                val sampleWeight = (qualityWeight * recencyWeight).coerceAtLeast(0.05f)
                val bucketMinute = PeakEnergyEngine.bucketMinuteOfDay(session.startedAt)
                val acc = bucket.getOrPut(bucketMinute) { Acc() }
                acc.weight += sampleWeight
                acc.completion += completion * sampleWeight
                acc.abort += (if (isAbort) 1f else 0f) * sampleWeight
                acc.distraction += distractionScore * sampleWeight
                acc.samples += 1

                val taskType = task?.taskType?.name ?: "UNKNOWN"
                val quality = (
                    completion * 0.6f +
                        (1f - if (isAbort) 1f else 0f) * 0.2f +
                        (1f - distractionScore) * 0.2f
                    ).coerceIn(0f, 1f)
                val typeAcc = taskTypeBuckets.getOrPut(taskType) { TaskTypeAcc() }
                typeAcc.weightedBucketSum += bucketMinute * sampleWeight
                typeAcc.weightedQualitySum += quality * sampleWeight
                typeAcc.weight += sampleWeight
                typeAcc.samples += 1
                totalWeightedSamples += sampleWeight
                recentWeighted += recencyWeight
            }

        if (bucket.isEmpty()) return BehaviorSignals(emptyList(), emptyList(), 0f, 1f)

        val slots = bucket.entries.map { (bucketStart, acc) ->
            val safeWeight = acc.weight.coerceAtLeast(1e-6)
            PeakEnergyEngine.SlotPerformanceAggregate(
                bucketStartMinute = bucketStart,
                qualityWeightedCompletionRate = (acc.completion / safeWeight).toFloat().coerceIn(0f, 1f),
                abortRate = (acc.abort / safeWeight).toFloat().coerceIn(0f, 1f),
                distractionRate = (acc.distraction / safeWeight).toFloat().coerceIn(0f, 1f),
                sampleCount = acc.samples
            )
        }.sortedBy { it.bucketStartMinute }
        val typePerf = taskTypeBuckets.entries.map { (taskType, acc) ->
            val safeWeight = acc.weight.coerceAtLeast(1e-6)
            PeakEnergyEngine.TaskTypePerformanceAggregate(
                taskType = taskType,
                averageBestBucketMinute = (acc.weightedBucketSum / safeWeight).roundToInt().coerceIn(0, 1439),
                weightedQuality = (acc.weightedQualitySum / safeWeight).toFloat().coerceIn(0f, 1f),
                sampleCount = acc.samples
            )
        }

        val expectedSamples = (lookbackDays * 2).toFloat().coerceAtLeast(1f)
        val coverage = (totalWeightedSamples.toFloat() / expectedSamples).coerceIn(0f, 1f)
        val recentWeight = (recentWeighted / bucket.values.sumOf { it.samples.toDouble() }.coerceAtLeast(1.0))
            .toFloat()
            .coerceIn(0.5f, 1.2f)

        return BehaviorSignals(
            slots = slots,
            taskTypePerformance = typePerf,
            coverage = coverage,
            recentWeight = recentWeight
        )
    }

    private fun coldStartFactor(behaviorCoverage: Float, sleepCoverage: Float): Float {
        val blended = (behaviorCoverage * 0.7f + sleepCoverage * 0.3f).coerceIn(0f, 1f)
        return (0.35f + blended * 0.65f).coerceIn(0.35f, 1f)
    }

    private fun driftMinutes(
        slots: List<PeakEnergyEngine.SlotPerformanceAggregate>,
        baselineAnchorMinute: Int
    ): Int {
        if (slots.isEmpty()) return 0
        val top = slots.maxByOrNull {
            (it.qualityWeightedCompletionRate * 0.6f) + ((1f - it.abortRate) * 0.25f) + ((1f - it.distractionRate) * 0.15f)
        } ?: return 0
        val day = 24 * 60
        val forward = (top.bucketStartMinute - baselineAnchorMinute + day) % day
        val signed = if (forward > day / 2) forward - day else forward
        return (signed * 0.2f).roundToInt().coerceIn(-35, 35)
    }

    private fun weeklyBacktestError(
        slots: List<PeakEnergyEngine.SlotPerformanceAggregate>,
        baselineAnchorMinute: Int
    ): Float? {
        if (slots.isEmpty()) return null
        val top = slots.maxByOrNull {
            (it.qualityWeightedCompletionRate * 0.6f) + ((1f - it.abortRate) * 0.25f) + ((1f - it.distractionRate) * 0.15f)
        } ?: return null
        val day = 24 * 60
        val forward = (top.bucketStartMinute - baselineAnchorMinute + day) % day
        val signed = if (forward > day / 2) forward - day else forward
        return abs(signed).toFloat()
    }

    private fun buildProfileOverride(
        enabled: Boolean,
        typeRaw: String,
        anchorMinute: Int,
        w1Duration: Int,
        w2Duration: Int,
        w3Duration: Int,
        w1Amplitude: Float,
        w2Amplitude: Float,
        w3Amplitude: Float
    ): PeakEnergyEngine.ProfileOverride? {
        if (!enabled) return null
        val profileType = when (typeRaw.uppercase()) {
            "WORKDAY" -> PeakEnergyEngine.ProfileType.WORKDAY
            "WEEKEND" -> PeakEnergyEngine.ProfileType.WEEKEND
            else -> null
        }
        return PeakEnergyEngine.ProfileOverride(
            enabled = true,
            profileType = profileType,
            anchorMinuteOfDay = anchorMinute.coerceIn(0, 1439),
            windows = listOf(
                PeakEnergyEngine.PeakWindow(0, w1Duration.coerceIn(30, 360), w1Amplitude.coerceIn(0.2f, 1f)),
                PeakEnergyEngine.PeakWindow(570, w2Duration.coerceIn(30, 360), w2Amplitude.coerceIn(0.2f, 1f)),
                PeakEnergyEngine.PeakWindow(810, w3Duration.coerceIn(30, 360), w3Amplitude.coerceIn(0.2f, 1f))
            )
        )
    }

    private suspend fun maybeAutoTuneMorningWeights() {
        val prefs = preferencesDataStore.preferencesFlow.first()
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        if (prefs.morningTuneUpdatedAtMillis > 0L && now - prefs.morningTuneUpdatedAtMillis < sevenDaysMs) return

        val sessions = sessionRepository.getAllSessions()
        if (sessions.isEmpty()) return
        val lookbackStart = now - (LOOKBACK_DAYS * 24L * 60L * 60L * 1000L)
        val recent = sessions.filter { it.startedAt >= lookbackStart && it.endedAt != null }
        if (recent.size < 8) return

        val interruptionRate = recent
            .map { (it.pauseResumeCount + it.interruptionBurstCount).toFloat() / maxOf(it.durationMinutes, 1f) }
            .average()
            .toFloat()
            .coerceIn(0f, 1f)
        val qualityRate = recent
            .map { (it.durationMinutes / 45f).coerceIn(0f, 1f) * (1f - (it.appSwitchCount / 6f).coerceIn(0f, 1f)) }
            .average()
            .toFloat()
            .coerceIn(0f, 1f)

        val deltaBehavior = ((qualityRate - interruptionRate) * 0.05f).coerceIn(-0.03f, 0.03f)
        val deltaWake = ((interruptionRate - 0.25f) * 0.03f).coerceIn(-0.02f, 0.02f)
        val deltaSleep = ((0.7f - qualityRate) * 0.02f).coerceIn(-0.015f, 0.015f)

        val tunedSleep = (prefs.morningTuneSleepWeight + deltaSleep).coerceIn(0.15f, 0.45f)
        val tunedWake = (prefs.morningTuneWakeWeight + deltaWake).coerceIn(0.15f, 0.4f)
        val tunedBehavior = (prefs.morningTuneBehaviorWeight + deltaBehavior).coerceIn(0.15f, 0.45f)
        val tunedBase = prefs.morningTuneBaseWeight.coerceIn(0.1f, 0.3f)
        val total = (tunedSleep + tunedWake + tunedBehavior + tunedBase).coerceAtLeast(1e-6f)

        preferencesDataStore.updatePreferences {
            it.copy(
                morningTuneSleepWeight = tunedSleep / total,
                morningTuneWakeWeight = tunedWake / total,
                morningTuneBehaviorWeight = tunedBehavior / total,
                morningTuneBaseWeight = tunedBase / total,
                morningTuneUpdatedAtMillis = now,
                morningTuneVersion = maxOf(1, it.morningTuneVersion)
            )
        }
    }

    private fun recencyWeight(timestampMillis: Long, lookbackDays: Int): Float {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        val date = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(date, today).toInt().coerceAtLeast(0)
        val bounded = daysAgo.coerceAtMost(lookbackDays)
        return exp(-RECENCY_LAMBDA * bounded.toDouble()).toFloat().coerceIn(0.08f, 1f)
    }

    private fun circularWakeVarianceMinutes(logs: List<SleepLogEntity>): Int {
        if (logs.isEmpty()) return 0
        val zoneId = ZoneId.systemDefault()
        val wakeMinutes = logs.map { log ->
            val local = Instant.ofEpochMilli(log.endAt).atZone(zoneId).toLocalTime()
            local.hour * 60 + local.minute
        }
        val radians = wakeMinutes.map { (it.toDouble() / (24.0 * 60.0)) * (2.0 * Math.PI) }
        val meanSin = radians.map(::sin).average()
        val meanCos = radians.map(::cos).average()
        val resultantLength = kotlin.math.sqrt((meanSin * meanSin) + (meanCos * meanCos)).coerceIn(0.0, 1.0)
        val circularStd = kotlin.math.sqrt((-2.0 * kotlin.math.ln(resultantLength.coerceAtLeast(1e-6))))
        val minutesStd = (circularStd * (24.0 * 60.0) / (2.0 * Math.PI)).roundToInt()
        return minutesStd.coerceIn(0, 240)
    }

    private fun currentProfileType(nowMillis: Long = System.currentTimeMillis()): PeakEnergyEngine.ProfileType {
        return profileTypeForMillis(nowMillis)
    }

    private fun profileTypeForMillis(millis: Long): PeakEnergyEngine.ProfileType {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY -> PeakEnergyEngine.ProfileType.WEEKEND
            else -> PeakEnergyEngine.ProfileType.WORKDAY
        }
    }

    private fun parseChronotype(raw: String?): MEQChronotypeDetector.Chronotype? {
        if (raw.isNullOrBlank()) return null
        return try {
            MEQChronotypeDetector.Chronotype.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun toMeqResult(chronotype: MEQChronotypeDetector.Chronotype): MEQChronotypeDetector.Result {
        val totalScore = when (chronotype) {
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING -> 75
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING -> 64
            MEQChronotypeDetector.Chronotype.INTERMEDIATE -> 50
            MEQChronotypeDetector.Chronotype.MODERATE_EVENING -> 36
            MEQChronotypeDetector.Chronotype.DEFINITE_EVENING -> 24
        }
        val (peakStart, peakEnd) = MEQChronotypeDetector.baselinePeakWindow(chronotype)
        return MEQChronotypeDetector.Result(
            totalScore = totalScore,
            chronotype = chronotype,
            baselinePeakStartHour = peakStart,
            baselinePeakEndHour = peakEnd,
            answeredQuestions = MEQChronotypeDetector.QUESTION_COUNT,
            confidence = 1f
        )
    }

    private fun midpointMinuteOfDay(startHour: Int, endHour: Int): Int {
        val startMinute = normalizeHour(startHour) * 60
        var endMinute = normalizeHour(endHour) * 60
        if (endMinute < startMinute) {
            endMinute += 24 * 60
        }
        val midpoint = (startMinute + endMinute) / 2
        return midpoint % (24 * 60)
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }

    private fun minutesDifferenceWrapped(startMinute: Int, endMinute: Int): Int {
        val dayMinutes = 24 * 60
        val start = ((startMinute % dayMinutes) + dayMinutes) % dayMinutes
        val end = ((endMinute % dayMinutes) + dayMinutes) % dayMinutes
        return if (end >= start) end - start else (dayMinutes - start) + end
    }
}
