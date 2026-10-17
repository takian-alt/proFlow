package com.neuroflow.app.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyPhase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.math.ln

/**
 * Runs once daily (or on-demand) to recompute distraction scores for all active tasks
 * using UsageStatsManager data correlated with circadian troughs, then persists the scores back to Room.
 *
 * Requires PACKAGE_USAGE_STATS permission — silently skips if not granted.
 */
@HiltWorker
class DistractionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val sessionDao: TimeSessionDao,
    private val behavioralSupportService: BehavioralSupportService,
    private val circadianEnergyService: CircadianEnergyService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!BehavioralSupportService.hasUsagePermission(applicationContext)) return Result.success()

        return try {
            val tasks = taskDao.getActiveTasks()
            val sessions = sessionDao.getAll()
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return Result.success()

            val nowMillis = System.currentTimeMillis()
            val lookbackStart = nowMillis - (30L * 24 * 60 * 60 * 1000L) // 30 days
            val distractingPackages = BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES

            tasks.forEach { task ->
                val taskSessions = sessions.filter {
                    it.taskId == task.id && it.endedAt != null && it.startedAt >= lookbackStart
                }
                if (taskSessions.isEmpty()) return@forEach

                var totalDistractionCount = 0
                var totalDistractedMs = 0L
                var peakDistractionCount = 0
                var dipDistractionCount = 0

                for (session in taskSessions) {
                    val sessionEnd = session.endedAt ?: continue
                    val rawEvents = usm.queryEvents(session.startedAt, sessionEnd)
                    val buf = UsageEvents.Event()
                    val fgEvents = mutableListOf<Pair<String, Long>>()

                    while (rawEvents.hasNextEvent()) {
                        rawEvents.getNextEvent(buf)
                        @Suppress("DEPRECATION")
                        if (buf.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                        fgEvents.add(buf.packageName to buf.timeStamp)
                    }

                    fgEvents.forEachIndexed { i, (pkg, timeMs) ->
                        if (pkg == applicationContext.packageName) return@forEachIndexed
                        if (!distractingPackages.contains(pkg)) return@forEachIndexed

                        val nextTime = fgEvents.getOrNull(i + 1)?.second ?: sessionEnd
                        val duration = (nextTime - timeMs).coerceIn(0L, 5 * 60_000L)
                        if (duration < 3_000L) return@forEachIndexed

                        totalDistractionCount++
                        totalDistractedMs += duration

                        // Circadian correlation: evaluate snapshot at distraction timestamp
                        val snapshot = circadianEnergyService.evaluateSnapshot(
                            timestampMillis = timeMs,
                            wakeTimeMillis = timeMs - 8 * 3600_000L,
                            lastSleepDurationMinutes = 480
                        )
                        if (snapshot.phase == EnergyPhase.PEAK) {
                            peakDistractionCount++
                        } else if (snapshot.phase == EnergyPhase.DIP) {
                            dipDistractionCount++
                        }
                    }
                }

                if (totalDistractionCount > 0) {
                    val freqScore = ln(1f + totalDistractionCount) * 15f
                    val depthScore = (totalDistractedMs / 60_000f * 1.5f).coerceAtMost(40f)
                    // Peak distraction penalty is severe (wasting prime energy), dip distraction has lower friction
                    val circadianModifier = (peakDistractionCount.toFloat() / totalDistractionCount) * 20f
                    val score = (freqScore + depthScore + circadianModifier).coerceIn(0f, 100f)

                    if (score != task.distractionScore) {
                        taskDao.update(task.copy(distractionScore = score))
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "distraction_sync"

        /** Enqueue a periodic daily sync. Safe to call multiple times — deduped by name. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<DistractionSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Run immediately (e.g. after user grants permission). */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DistractionSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
