package com.neuroflow.app.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.MainActivity
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.PredictedEnergyCurveUiModel
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.buildPredictedEnergyCurveUiModel
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyRecoveryThresholdAdvisor
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.receiver.EnergyRecoverySnoozeReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Periodically evaluates the user's Vico-plotted [PredictedEnergyCurveUiModel] against their
 * personalized recovery threshold and triggers a local notification suggesting a short break
 * whenever predicted energy dips below threshold during waking hours.
 */
@HiltWorker
class EnergyRecoveryNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val circadianEnergyService: CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.energyRecoveryAlertsEnabled) {
            Result.success()
        } else {
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val curve = buildTodayCurve(prefs, nowMillis, zoneId)
            val suggestion = evaluateCurveForRecoveryNotification(
                curve = curve,
                prefs = prefs,
                nowMillis = nowMillis,
                zoneId = zoneId
            )
            if (suggestion.shouldNotify) {
                val dispatched = dispatchRecoveryNotification(applicationContext, suggestion)
                if (dispatched) {
                    preferencesDataStore.recordRecoveryAlertSent(nowMillis)
                }
            }
            Result.success()
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        android.util.Log.e("EnergyRecoveryWorker", "Failed to evaluate recovery threshold", e)
        Result.failure()
    }

    private suspend fun buildTodayCurve(
        prefs: UserPreferences,
        nowMillis: Long,
        zoneId: ZoneId
    ): PredictedEnergyCurveUiModel {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val localDate = nowZdt.toLocalDate()
        val dayStart = startOfLocalDay(nowMillis)
        val dayEnd = startOfNextLocalDay(dayStart) - 1L

        var anchor = localDate
            .atTime(prefs.wakeUpHour.coerceIn(0, 23), prefs.wakeTimeMinute.coerceIn(0, 59))
            .atZone(zoneId)
        val anchorMillis = anchor.toInstant().toEpochMilli()
        if (anchorMillis > nowMillis && anchorMillis - nowMillis > 2 * 3600_000L) {
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
        val physBlocks = com.neuroflow.app.domain.assistant.EnergyBoosterEngine.buildBoosterCognitiveBlocks(
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

        val completedTasks = runCatching { taskRepository.getCompletedTasks() }.getOrDefault(emptyList())
        val allSessions = runCatching { sessionRepository.getAllSessions() }.getOrDefault(emptyList())
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

        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
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
        }.getOrNull()

        val energyTimeline = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dayStart,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 30,
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
        }.getOrDefault(emptyList())

        return buildPredictedEnergyCurveUiModel(
            timeline = energyTimeline,
            completedTasks = completedTasks,
            sessions = allSessions,
            circadianSummary = circadianSummary,
            learnedProfile = learnedProfile,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            currentHour = nowZdt.hour,
            zoneId = zoneId,
            recoveryThresholdBasePercent = prefs.recoveryThresholdBasePercent,
            recoveryThresholdAutoPersonalized = prefs.recoveryThresholdAutoPersonalized,
            wakeUpHour = prefs.wakeUpHour,
            sleepHour = prefs.sleepHour,
            sleepPressurePoints = prefs.sleepPressurePoints,
            recommendedBreakMinutes = prefs.autoSchedulingBreakDurationMinutes
        )
    }

    companion object {
        const val NOTIFICATION_ID_RECOVERY = 10_003
        const val WORK_SNOOZE_RECOVERY = "energy_recovery_snooze_check"

        internal fun evaluateCurveForRecoveryNotification(
            curve: PredictedEnergyCurveUiModel,
            prefs: UserPreferences,
            nowMillis: Long = System.currentTimeMillis(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): EnergyRecoveryThresholdAdvisor.BreakSuggestion =
            EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
                curve = curve,
                prefs = prefs,
                nowMillis = nowMillis,
                zoneId = zoneId
            )

        internal fun dispatchRecoveryNotification(
            context: Context,
            suggestion: EnergyRecoveryThresholdAdvisor.BreakSuggestion
        ): Boolean {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false
            val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            if (!canNotify) return false

            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_SCHEDULE"
                putExtra("notificationId", NOTIFICATION_ID_RECOVERY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_RECOVERY,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, EnergyRecoverySnoozeReceiver::class.java).apply {
                putExtra("notificationId", NOTIFICATION_ID_RECOVERY)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_RECOVERY + 1,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_RECOVERY)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(suggestion.notificationTitle)
                .setContentText(suggestion.notificationMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(suggestion.notificationMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_recent_history,
                    "Snooze 1h",
                    snoozePendingIntent
                )
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_ID_RECOVERY, notification)
            return true
        }

        fun enqueueSnoozedCheck(context: Context, delayHours: Long = 1L) {
            val request = OneTimeWorkRequestBuilder<EnergyRecoveryNotificationWorker>()
                .setInitialDelay(delayHours.coerceAtLeast(1L), TimeUnit.HOURS)
                .addTag(WORK_ENERGY_RECOVERY)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_SNOOZE_RECOVERY,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelRecoveryNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID_RECOVERY)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_SNOOZE_RECOVERY)
        }
    }
}
