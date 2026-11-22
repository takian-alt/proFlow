package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.assistant.BurnoutGuardEngine
import com.neuroflow.app.domain.assistant.EnergyBoosterEngine
import com.neuroflow.app.domain.assistant.ProactiveWellnessNudgePlanner
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Periodic WorkManager worker that computes the user's live circadian energy snapshot,
 * burnout load, hydration gap, and eligible physiological boosters, then dispatches
 * at most one context-aware, actionable wellness notification via
 * [ProactiveWellnessNotificationService].
 */
@HiltWorker
class ProactiveWellnessNudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val circadianEnergyService: CircadianEnergyService,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val wellnessNotificationService: ProactiveWellnessNotificationService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val forcePreview = inputData.getBoolean(
            ProactiveWellnessNotificationService.INPUT_FORCE_PREVIEW,
            false
        )
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.wellnessNudgesEnabled && !forcePreview) {
            Result.success()
        } else {
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val dayStart = startOfLocalDay(nowMillis)

            val allSessions = runCatching { sessionRepository.getAllSessions() }.getOrDefault(emptyList())
            val openSessions = runCatching { sessionRepository.getOpenSessions() }.getOrDefault(emptyList())
            val activeTasks = runCatching { taskRepository.getActiveTasks() }.getOrDefault(emptyList())
            val completedTasks = runCatching { taskRepository.getCompletedTasks() }.getOrDefault(emptyList())

            val isFocusSessionActive = (FocusService.activeSession.value?.isPaused == false) ||
                openSessions.any { it.endedAt == null && it.pausedAt == null }

            val hfPrefs = runCatching { hyperFocusDataStore.current() }.getOrNull()
            val isHyperFocusActive = hfPrefs != null && hfPrefs.isActive && !(
                hfPrefs.sessionMode == HyperFocusSessionMode.TIME_BASED &&
                    hfPrefs.sessionEndsAtMillis != null &&
                    hfPrefs.sessionEndsAtMillis <= nowMillis
                )

            val continuousFocusMinutes = computeContinuousFocusMinutes(
                sessions = allSessions,
                prefs = prefs,
                dayStartMillis = dayStart,
                nowMillis = nowMillis
            )

            val evalContext = buildLiveCircadianContext(
                prefs = prefs,
                completedTasks = completedTasks,
                allSessions = allSessions,
                continuousFocusMinutes = continuousFocusMinutes,
                nowMillis = nowMillis,
                dayStart = dayStart,
                zoneId = zoneId
            )

            val snapshot = evalContext.snapshot
            val burnoutRisk = BurnoutGuardEngine.evaluateBurnoutRisk(
                prefs = prefs,
                sessions = allSessions,
                completedTasks = completedTasks,
                activeTasks = activeTasks,
                snapshot = snapshot,
                nowMillis = nowMillis,
                zoneId = zoneId
            )
            val recommendations = EnergyBoosterEngine.recommendBoosters(
                prefs = prefs,
                snapshot = snapshot,
                burnoutRiskState = burnoutRisk,
                stimulants = evalContext.stimulants,
                naps = evalContext.naps,
                nowMillis = nowMillis,
                zoneId = zoneId,
                maxResults = 4
            )

            val decision = ProactiveWellnessNudgePlanner.evaluate(
                prefs = prefs,
                snapshot = snapshot,
                burnoutRisk = burnoutRisk,
                recommendations = recommendations,
                stimulants = evalContext.stimulants,
                naps = evalContext.naps,
                continuousFocusMinutes = continuousFocusMinutes,
                nowMillis = nowMillis,
                isFocusSessionActive = isFocusSessionActive,
                isHyperFocusActive = isHyperFocusActive,
                zoneId = zoneId,
                ignoreCooldownForPreview = forcePreview
            )

            if (decision.shouldNotify && decision.payload != null) {
                val dispatched = wellnessNotificationService.dispatchNotification(
                    payload = decision.payload,
                    context = applicationContext
                )
                if (dispatched && !forcePreview) {
                    preferencesDataStore.recordWellnessNotificationSent(nowMillis)
                }
            }
            Result.success()
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        android.util.Log.e("WellnessNudgeWorker", "Failed to evaluate proactive wellness nudge", e)
        Result.retry()
    }

    private data class LiveCircadianContext(
        val snapshot: com.neuroflow.app.domain.service.CircadianSnapshot,
        val stimulants: List<com.neuroflow.app.domain.service.StimulantDoseBout>,
        val naps: List<DaytimeNapBout>
    )

    private suspend fun buildLiveCircadianContext(
        prefs: UserPreferences,
        completedTasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        allSessions: List<TimeSessionEntity>,
        continuousFocusMinutes: Int,
        nowMillis: Long,
        dayStart: Long,
        zoneId: ZoneId
    ): LiveCircadianContext {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val localDate = nowZdt.toLocalDate()

        var anchor = localDate
            .atTime(prefs.wakeUpHour.coerceIn(0, 23), prefs.wakeTimeMinute.coerceIn(0, 59))
            .atZone(zoneId)
        val anchorMillis = anchor.toInstant().toEpochMilli()
        if (anchorMillis > nowMillis && anchorMillis - nowMillis > 2 * 3_600_000L) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val sleepLogs = runCatching {
            sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()
        }.getOrDefault(emptyList())
        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        val quickPhysNaps = physLogs
            .filter { it.type == PhysiologicalEventType.NAP }
            .map { log ->
                val napDuration = log.durationMinutes.coerceIn(5, 180)
                DaytimeNapBout(
                    startMillis = log.timestampMillis,
                    endMillis = log.timestampMillis + napDuration * 60_000L
                )
            }
        val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

        val resolvedChronotype = when {
            !prefs.manualChronotype.isNullOrBlank() -> ChronotypeProfile.fromString(prefs.manualChronotype)
            !prefs.quizChronotype.isNullOrBlank() -> ChronotypeProfile.fromString(prefs.quizChronotype)
            prefs.meqScore > 0 -> when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
            else -> ChronotypeProfile.INTERMEDIATE
        }

        val allStimulants = parseStimulantLogs(prefs.stimulantLogsJson)
        val slotBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = localDate,
            zoneId = zoneId
        )
        val physBlocks = EnergyBoosterEngine.buildBoosterCognitiveBlocks(
            prefs = prefs,
            dayStartMillis = dayStart,
            zoneId = zoneId
        )
        val dailyBlocks = slotBlocks + physBlocks

        val rollingAverageSleepMinutes = computeRollingDailySleepAverage(
            sleepLogs = sleepLogs,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val socialJetlagLagHours = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) },
            nowMillis = nowMillis,
            zoneId = zoneId
        ).mondayMorningPhaseLagHours

        val approvedFeedbackObs = SchedulingFeedbackStore.extractApprovedCircadianObservations(
            json = prefs.schedulingRejectionsJson,
            nowMillis = nowMillis
        )
        val behavioralObservations = buildBehavioralObservations(
            completedTasks = completedTasks,
            sessions = allSessions,
            zoneId = zoneId
        ) + approvedFeedbackObs
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (behavioralObservations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(
                observations = behavioralObservations,
                initialPeakHour = initialPeak
            )
        } else {
            null
        }
        val manualPeakHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else {
            null
        }

        val summary = circadianEnergyService.computeDailySummary(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousFocusMinutes,
            zoneId = zoneId,
            naps = naps,
            chronotype = resolvedChronotype,
            manualPeakStartHour = manualPeakHour,
            cognitiveBlocks = dailyBlocks,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            stimulantDoses = allStimulants,
            socialJetlagLagHours = socialJetlagLagHours
        )

        return LiveCircadianContext(
            snapshot = summary.currentSnapshot,
            stimulants = allStimulants,
            naps = naps
        )
    }

    private fun computeContinuousFocusMinutes(
        sessions: List<TimeSessionEntity>,
        prefs: UserPreferences,
        dayStartMillis: Long,
        nowMillis: Long
    ): Int {
        val restLogsToday = BurnoutGuardEngine.collectTodayRestLogs(prefs, dayStartMillis, nowMillis)
        val lastQualifyingRestAt = restLogsToday
            .filter { it.durationMinutes >= 10 }
            .maxOfOrNull { it.completedAtMillis } ?: dayStartMillis
        val windowStart = maxOf(dayStartMillis, lastQualifyingRestAt)
        val recentSessions = sessions
            .filter { it.startedAt >= windowStart && it.startedAt <= nowMillis }
            .sortedBy { it.startedAt }

        var streakMins = 0.0
        var lastEnd = 0L
        for (session in recentSessions) {
            val mins = session.trackedMinutes(nowMillis).toDouble()
            if (mins <= 0.0) continue
            if (lastEnd > 0L && (session.startedAt - lastEnd) > 25 * 60_000L) {
                streakMins = 0.0
            }
            streakMins += mins
            val sessionEnd = (session.endedAt ?: nowMillis).coerceAtMost(nowMillis)
            lastEnd = maxOf(lastEnd, sessionEnd)
        }
        if (lastEnd > 0L && (nowMillis - lastEnd) > 35 * 60_000L) {
            return 0
        }
        return streakMins.roundToInt().coerceAtLeast(0)
    }
}
