package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.EnergyScoreEngine
import com.neuroflow.app.domain.engine.MomentEnergyEngine
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import com.neuroflow.app.domain.engine.SleepPressureDetector
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.launcher.data.NotificationBadgeManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlin.math.min

@Singleton
class EnergyScoreRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val peakEnergyRepository: PeakEnergyRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val sleepLogRepository: SleepLogRepository,
    private val notificationBadgeManager: NotificationBadgeManager,
    private val energyMetricsRepository: EnergyMetricsRepository
) {

    companion object {
        private const val RETENTION_ENFORCE_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L

        internal data class RecentSessionMetrics(
            val recentFocusMinutes: Float,
            val recentInterruptionCount: Int,
            val recentAppSwitchCount: Int,
            val recentPauseResumeCount: Int,
            val activeSessionCount: Int
        )

        /**
         * Session activity for several look-back windows at once. The live-energy signal asks for
         * five nested windows on every emission, so the session table is walked once and each
         * session's own figures are computed once and then tallied into every window it touches.
         */
        internal fun summarizeRecentSessionWindows(
            sessions: List<TimeSessionEntity>,
            nowMillis: Long,
            windowMinutes: List<Int>
        ): List<RecentSessionMetrics> {
            val windowCount = windowMinutes.size
            val cutoffs = LongArray(windowCount) { nowMillis - windowMinutes[it] * 60_000L }
            val focus = FloatArray(windowCount)
            val interruptions = IntArray(windowCount)
            val appSwitches = IntArray(windowCount)
            val pauseResumes = IntArray(windowCount)
            val activeSessions = IntArray(windowCount)

            val maxOpenSessionMillis = 120 * 60_000L
            sessions.forEach { session ->
                val endedAt = session.endedAt ?: min(nowMillis, session.startedAt + maxOpenSessionMillis)
                val durationMinutes = if (session.durationMinutes > 0f) {
                    session.durationMinutes
                } else {
                    ((endedAt - session.startedAt - session.totalPausedMs).coerceAtLeast(0L) / 60_000f)
                }.coerceAtLeast(0f)
                val interruptionCount =
                    session.pauseResumeCount + session.appSwitchCount + session.interruptionBurstCount

                for (i in 0 until windowCount) {
                    if (session.startedAt >= cutoffs[i] || endedAt >= cutoffs[i]) {
                        focus[i] += durationMinutes
                        interruptions[i] += interruptionCount
                        appSwitches[i] += session.appSwitchCount
                        pauseResumes[i] += session.pauseResumeCount
                        if (session.endedAt == null) activeSessions[i] += 1
                    }
                }
            }

            return (0 until windowCount).map { i ->
                RecentSessionMetrics(
                    recentFocusMinutes = focus[i].coerceAtMost(windowMinutes[i].toFloat()),
                    recentInterruptionCount = interruptions[i],
                    recentAppSwitchCount = appSwitches[i],
                    recentPauseResumeCount = pauseResumes[i],
                    activeSessionCount = activeSessions[i]
                )
            }
        }
    }

    @Volatile
    private var lastRetentionEnforcedAtMillis: Long = 0L

    private data class MomentSignalContext(
        val sessions: List<TimeSessionEntity>,
        val tasks: List<TaskEntity>,
        val badgeCounts: Map<String, Int>,
        val latestSleepLogEndAt: Long?  // null = no sleep logs recorded yet
    )

    data class EnergyUiModel(
        val availableEnergy: Int,
        val rawEnergy: Float,
        val baselineRawEnergy: Float,
        val peakScore: Float,
        val fatiguePenalty: Float,
        val momentAdjustment: Float,
        val momentConfidence: Float,
        val currentPeakValue: Int,
        val peakValue: Int,
        val peakDrop: Int,
        val minutesSincePeak: Int,
        val minutesUntilPeakReset: Int,
        val sleepPressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: SleepPressureDetector.FatigueZone,
        val circadianFactor: Float,
        val reservoirFactor: Float,
        val confidenceFactor: Float,
        val effectivePeakProfile: PeakEnergyEngine.EffectivePeakProfile? = null,
        val momentSignalSummary: String = "",
        val peakDetectionAgeMillis: Long = 0L,
        // Freshness tracking: age of input data sources
        val sessionDataAgeMillis: Long = 0L,
        val sleepLogDataAgeMillis: Long = 0L,
        val taskDataAgeMillis: Long = 0L,
        val overallFreshnessAgeMillis: Long = 0L,
        val hasRecentData: Boolean = true,
        val refreshedAtMillis: Long
    )

    fun observeEnergy(refreshIntervalMillis: Long = 60_000L): Flow<EnergyUiModel> {
        val ticker = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(refreshIntervalMillis)
            }
        }

        val momentSignals = combine(
            sessionRepository.observeAll(),
            taskRepository.observeAll(),
            sleepLogRepository.observeAll(),
            notificationBadgeManager.badgeCounts
        ) { sessions, tasks, sleepLogs, badgeCounts ->
            MomentSignalContext(
                sessions = sessions,
                tasks = tasks,
                badgeCounts = badgeCounts,
                latestSleepLogEndAt = sleepLogs.maxByOrNull { it.endAt }?.endAt
            )
        }

        return combine(
            peakEnergyRepository.peakEnergyFlow,
            preferencesDataStore.preferencesFlow,
            momentSignals,
            ticker
        ) { peakDetection, prefs, momentSignals, nowMillis ->
            val sleepPressurePoints = prefs.sleepPressurePoints.coerceAtLeast(0)
            val baselineScore = EnergyScoreEngine.calculateDetailed(
                EnergyScoreEngine.EnergySnapshot(
                    peakEnergy = peakDetection,
                    sleepPressurePoints = sleepPressurePoints,
                    nowMillis = nowMillis
                )
            )

            val recentWindowMinutes = 180
            val sessionWindowMetrics = summarizeRecentSessionWindows(
                sessions = momentSignals.sessions,
                nowMillis = nowMillis,
                windowMinutes = listOf(5, 15, 30, 60, recentWindowMinutes)
            )
            val sessionMetrics5m = sessionWindowMetrics[0]
            val sessionMetrics15m = sessionWindowMetrics[1]
            val sessionMetrics30m = sessionWindowMetrics[2]
            val sessionMetrics60m = sessionWindowMetrics[3]
            val sessionMetrics180m = sessionWindowMetrics[4]
            val recentSessionMetrics = sessionMetrics180m
            val taskMetrics = summarizeTasks(momentSignals.tasks, nowMillis)
            val notificationCount = momentSignals.badgeCounts.values.sum()

            val sessionDataAgeMillis = momentSignals.sessions
                .maxByOrNull { it.startedAt }
                ?.let { (nowMillis - it.startedAt).coerceAtLeast(0L) }
                ?: Long.MAX_VALUE
            val taskDataAgeMillis = momentSignals.tasks
                .maxByOrNull { it.updatedAt }
                ?.let { (nowMillis - it.updatedAt).coerceAtLeast(0L) }
                ?: Long.MAX_VALUE
            val sleepLogDataAgeMillis = momentSignals.latestSleepLogEndAt
                ?.let { (nowMillis - it).coerceAtLeast(0L) }
                ?: Long.MAX_VALUE
            // Telemetry freshness tracks real-time signal recency; sleep logs inform sleep pressure, not live telemetry staleness
            val overallFreshnessAgeMillis = listOfNotNull(
                peakDetection?.let { (nowMillis - it.detectedAtMillis).coerceAtLeast(0L) },
                sessionDataAgeMillis.takeIf { it != Long.MAX_VALUE },
                taskDataAgeMillis.takeIf { it != Long.MAX_VALUE }
            ).minOrNull() ?: 0L

            val multiHorizonFeatures = MomentEnergyEngine.MultiHorizonFeatures(
                window5m = MomentEnergyEngine.MultiHorizonFeatures.WindowMetrics(
                    focusMinutes = sessionMetrics5m.recentFocusMinutes,
                    interruptionCount = sessionMetrics5m.recentInterruptionCount,
                    appSwitchCount = sessionMetrics5m.recentAppSwitchCount
                ),
                window15m = MomentEnergyEngine.MultiHorizonFeatures.WindowMetrics(
                    focusMinutes = sessionMetrics15m.recentFocusMinutes,
                    interruptionCount = sessionMetrics15m.recentInterruptionCount,
                    appSwitchCount = sessionMetrics15m.recentAppSwitchCount
                ),
                window30m = MomentEnergyEngine.MultiHorizonFeatures.WindowMetrics(
                    focusMinutes = sessionMetrics30m.recentFocusMinutes,
                    interruptionCount = sessionMetrics30m.recentInterruptionCount,
                    appSwitchCount = sessionMetrics30m.recentAppSwitchCount
                ),
                window60m = MomentEnergyEngine.MultiHorizonFeatures.WindowMetrics(
                    focusMinutes = sessionMetrics60m.recentFocusMinutes,
                    interruptionCount = sessionMetrics60m.recentInterruptionCount,
                    appSwitchCount = sessionMetrics60m.recentAppSwitchCount
                ),
                window180m = MomentEnergyEngine.MultiHorizonFeatures.WindowMetrics(
                    focusMinutes = sessionMetrics180m.recentFocusMinutes,
                    interruptionCount = sessionMetrics180m.recentInterruptionCount,
                    appSwitchCount = sessionMetrics180m.recentAppSwitchCount
                )
            )

            val peakConfidence = peakDetection?.confidence ?: 0f
            val minutesSincePeak = peakDetection?.minutesSincePeak(nowMillis) ?: 0

            val momentResult = MomentEnergyEngine.predict(
                MomentEnergyEngine.MomentSignalSnapshot(
                    baselineRawEnergy = baselineScore.rawEnergy,
                    sleepPressurePoints = sleepPressurePoints,
                    peakConfidence = peakConfidence,
                    minutesSincePeak = minutesSincePeak,
                    recentFocusMinutes = recentSessionMetrics.recentFocusMinutes,
                    recentInterruptionCount = recentSessionMetrics.recentInterruptionCount,
                    recentAppSwitchCount = recentSessionMetrics.recentAppSwitchCount,
                    recentPauseResumeCount = recentSessionMetrics.recentPauseResumeCount,
                    notificationCount = notificationCount,
                    activeTaskCount = taskMetrics.activeTaskCount,
                    overdueTaskCount = taskMetrics.overdueTaskCount,
                    dueSoonTaskCount = taskMetrics.dueSoonTaskCount,
                    activeSessionCount = recentSessionMetrics.activeSessionCount,
                    interruptionSensitivity = prefs.momentInterruptionSensitivity,
                    notificationSensitivity = prefs.momentNotificationSensitivity,
                    taskPressureSensitivity = prefs.momentTaskPressureSensitivity,
                    recentWindowMinutes = recentWindowMinutes,
                    multiHorizonFeatures = multiHorizonFeatures,
                    signalFreshnessAgeMillis = overallFreshnessAgeMillis
                )
            )

            val currentPeakValue = peakDetection?.currentValueAt(nowMillis) ?: 0
            val peakValue = peakDetection?.peakValue ?: PeakEnergyEngine.PEAK_VALUE
            val peakDrop = (peakValue - currentPeakValue).coerceAtLeast(0)
            val minutesUntilPeakReset = ((24 * 60) - minutesSincePeak).coerceAtLeast(0)
            val peakDetectionAgeMillis = peakDetection?.let { (nowMillis - it.detectedAtMillis).coerceAtLeast(0L) } ?: Long.MAX_VALUE

            // Data is considered "recent" if all sources are less than 5 minutes old
            val freshnessThresholdMillis = 5 * 60_000L
            val hasRecentData = overallFreshnessAgeMillis < freshnessThresholdMillis
            val fatiguePercent = SleepPressureDetector.fatiguePercent(sleepPressurePoints)
            val fatigueZone = SleepPressureDetector.fatigueZone(sleepPressurePoints)

            val energyUiModel = EnergyUiModel(
                availableEnergy = momentResult.usableEnergy.toInt().coerceIn(0, 100),
                rawEnergy = momentResult.adjustedRawEnergy,
                baselineRawEnergy = baselineScore.rawEnergy,
                peakScore = baselineScore.peakScore,
                fatiguePenalty = baselineScore.fatiguePenalty,
                momentAdjustment = momentResult.adjustment,
                momentConfidence = momentResult.confidence,
                currentPeakValue = currentPeakValue,
                peakValue = peakValue,
                peakDrop = peakDrop,
                minutesSincePeak = minutesSincePeak,
                minutesUntilPeakReset = minutesUntilPeakReset,
                sleepPressurePoints = sleepPressurePoints,
                fatiguePercent = fatiguePercent,
                fatigueZone = fatigueZone,
                circadianFactor = baselineScore.circadianFactor,
                reservoirFactor = baselineScore.reservoirFactor,
                confidenceFactor = baselineScore.confidenceFactor,
                effectivePeakProfile = peakDetection?.effectiveProfile,
                momentSignalSummary = momentResult.summary,
                peakDetectionAgeMillis = peakDetectionAgeMillis,
                sessionDataAgeMillis = sessionDataAgeMillis,
                sleepLogDataAgeMillis = sleepLogDataAgeMillis,
                taskDataAgeMillis = taskDataAgeMillis,
                overallFreshnessAgeMillis = overallFreshnessAgeMillis,
                hasRecentData = hasRecentData,
                refreshedAtMillis = nowMillis
            )

            val telemetryEnabled = prefs.energyTelemetryEnabled
            val telemetryRetentionDays = prefs.energyTelemetryRetentionDays
                .coerceIn(EnergyMetricsRepository.MIN_RETENTION_DAYS, EnergyMetricsRepository.MAX_RETENTION_DAYS)

            if (telemetryEnabled) {
                try {
                    energyMetricsRepository.recordEnergyPrediction(
                        predictedAtMillis = nowMillis,
                        baselineRawEnergy = baselineScore.rawEnergy,
                        peakScore = baselineScore.peakScore,
                        fatiguePenalty = baselineScore.fatiguePenalty,
                        sleepPressurePoints = sleepPressurePoints,
                        fatiguePercent = fatiguePercent,
                        momentAdjustment = momentResult.adjustment,
                        momentConfidence = momentResult.confidence,
                        momentSupportScore = momentResult.supportScore,
                        momentPressureScore = momentResult.pressureScore,
                        adjustedRawEnergy = momentResult.adjustedRawEnergy,
                        usableEnergy = momentResult.usableEnergy.toInt().coerceIn(0, 100),
                        chronotype = peakDetection?.chronotype?.name ?: "UNKNOWN",
                        wakeUpHour = peakDetection?.wakeUpHour ?: 7,
                        peakMinuteOfDay = peakDetection?.peakMinuteOfDay ?: 600,
                        peakConfidence = peakConfidence,
                        peakDetectionAgeMillis = peakDetectionAgeMillis,
                        sleepLogAgeMillis = sleepLogDataAgeMillis,
                        sessionDataAgeMillis = sessionDataAgeMillis,
                        recentFocusMinutes = recentSessionMetrics.recentFocusMinutes,
                        recentInterruptionCount = recentSessionMetrics.recentInterruptionCount,
                        recentAppSwitchCount = recentSessionMetrics.recentAppSwitchCount,
                        activeTaskCount = taskMetrics.activeTaskCount,
                        notificationCount = notificationCount
                    )
                } catch (_: Exception) {
                    // Telemetry persistence must never block live energy updates.
                }

                if (nowMillis - lastRetentionEnforcedAtMillis >= RETENTION_ENFORCE_INTERVAL_MILLIS) {
                    try {
                        energyMetricsRepository.enforceRetentionPolicy(
                            retentionDays = telemetryRetentionDays,
                            nowMillis = nowMillis
                        )
                        lastRetentionEnforcedAtMillis = nowMillis
                    } catch (_: Exception) {
                        // Retention should not interfere with live scoring.
                    }
                }
            }

            energyUiModel
        }
    }

    private data class TaskMetrics(
        val activeTaskCount: Int,
        val overdueTaskCount: Int,
        val dueSoonTaskCount: Int
    )

    private fun summarizeTasks(
        tasks: List<TaskEntity>,
        nowMillis: Long
    ): TaskMetrics {
        val twoHoursMs = 2 * 60 * 60_000L
        val activeTaskCount = tasks.count { it.status == TaskStatus.ACTIVE }
        val overdueTaskCount = tasks.count { task ->
            task.status != TaskStatus.COMPLETED && (task.deadlineDate ?: Long.MAX_VALUE) < nowMillis
        }
        val dueSoonTaskCount = tasks.count { task ->
            if (task.status == TaskStatus.COMPLETED) return@count false
            val deadlineDelta = (task.deadlineDate ?: Long.MAX_VALUE) - nowMillis
            val scheduledDelta = (task.scheduledDate ?: Long.MAX_VALUE) - nowMillis
            deadlineDelta in 0 until twoHoursMs || scheduledDelta in 0 until twoHoursMs
        }

        return TaskMetrics(
            activeTaskCount = activeTaskCount,
            overdueTaskCount = overdueTaskCount,
            dueSoonTaskCount = dueSoonTaskCount
        )
    }
}
