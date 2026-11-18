package com.neuroflow.app.worker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.presentation.widget.CompleteTaskReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import kotlin.math.roundToInt

@HiltWorker
class FocusWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val tasks = taskRepository.getActiveTasks()
            val preferences = preferencesDataStore.preferencesFlow.first()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val dayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(nowMillis)
            val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dayStart) - 1
            val candidateTasks = filterCandidateTasks(tasks, dayEnd)

            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, preferences.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(preferences.quizChronotype ?: preferences.manualChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val priorityWeights = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toPriorityWeights(preferences)
            val schedulableMap = candidateTasks.associate { task ->
                task.id to com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toSchedulableTask(
                    task = task,
                    allContextTasks = tasks,
                    dayStartMillis = dayStart,
                    usePlannedBlockForScoring = true
                )
            }
            val dependentCounts = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.computeDependentCounts(
                schedulableMap.values.toList()
            )
            val sortedWithScores = candidateTasks.map { task ->
                val schedulable = schedulableMap[task.id]!!
                val breakdown = taskSchedulerService.evaluateTaskScoreWithFatigue(
                    task = schedulable,
                    snapshot = snapshot,
                    dependentCount = dependentCounts[task.id] ?: 0,
                    weights = priorityWeights,
                    referenceNowMillis = nowMillis,
                    cumulativeCognitiveLoadMinutes = 0.0
                )
                val frogBoost = if (task.isFrog) 8.0 * preferences.weightFrogBonus else 0.0
                val personalBoost = if (schedulable.isPersonalTask) 6.0 else 0.0
                val totalPriority = breakdown.totalScore + frogBoost + personalBoost
                Triple(task, breakdown, totalPriority)
            }.sortedByDescending { it.third }

            val topTriple = sortedWithScores.firstOrNull()
            val topTask = topTriple?.first
            val topScore = topTriple?.second?.totalScore?.roundToInt() ?: 0

            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            // 1. Update FocusWidgetProvider (widget_focus)
            val focusViews = RemoteViews(appContext.packageName, R.layout.widget_focus)
            val focusContainerIntent = Intent(appContext, MainActivity::class.java).apply {
                if (topTask != null) {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                } else {
                    action = "com.procus.ACTION_OPEN_NEW_TASK"
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val focusContainerPendingIntent = PendingIntent.getActivity(
                appContext, 1002, focusContainerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            focusViews.setOnClickPendingIntent(R.id.widget_focus_container, focusContainerPendingIntent)

            if (topTask != null) {
                focusViews.setViewVisibility(R.id.widget_start_button, android.view.View.VISIBLE)
                focusViews.setTextViewText(R.id.widget_task_title, topTask.title)
                focusViews.setTextViewText(
                    R.id.widget_task_score,
                    "Priority Score: $topScore"
                )

                val startIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val startPendingIntent = PendingIntent.getActivity(
                    appContext, 1001, startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                focusViews.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)
            } else {
                focusViews.setViewVisibility(R.id.widget_start_button, android.view.View.GONE)
                focusViews.setTextViewText(R.id.widget_task_title, "No tasks — add one in neuroFlow")
                focusViews.setTextViewText(R.id.widget_task_score, "Score: —")
            }

            val focusComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.FocusWidgetProvider"
            )
            appWidgetManager.updateAppWidget(focusComponentName, focusViews)

            // 2. Update NextActionWidgetProvider (widget_next_action)
            val nextActionViews = RemoteViews(appContext.packageName, R.layout.widget_next_action)
            val energyText = "⚡ ${snapshot.usableEnergy}% · ${FatigueZone.fromUsableEnergy(snapshot.usableEnergy).label}"
            nextActionViews.setTextViewText(R.id.widget_energy_pill, energyText)

            if (topTask != null) {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, topTask.title)
                val plannedMins = TaskBlockPlan.plannedMinutes(topTask)
                val durationText = if (plannedMins > 0) "${plannedMins}m" else "Quick"
                val quadrantText = topTask.quadrant.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "$quadrantText · $durationText")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.VISIBLE)

                val focusIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val focusPendingIntent = PendingIntent.getActivity(
                    appContext, 2001, focusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_next_start_button, focusPendingIntent)

                val completeIntent = Intent(appContext, CompleteTaskReceiver::class.java).apply {
                    action = CompleteTaskReceiver.ACTION_COMPLETE_TASK
                    putExtra(CompleteTaskReceiver.EXTRA_TASK_ID, topTask.id)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    appContext, topTask.id.hashCode(), completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_complete_button, completePendingIntent)
            } else {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, "All clear! Plan what's next")
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "No active tasks")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.GONE)
            }

            val containerIntent = Intent(appContext, MainActivity::class.java).apply {
                if (topTask != null) {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                } else {
                    action = "com.procus.ACTION_OPEN_NEW_TASK"
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val containerPendingIntent = PendingIntent.getActivity(
                appContext, 2002, containerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            nextActionViews.setOnClickPendingIntent(R.id.widget_next_action_container, containerPendingIntent)

            val nextActionComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.NextActionWidgetProvider"
            )
            appWidgetManager.updateAppWidget(nextActionComponentName, nextActionViews)

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Stale widget preferred over crash
        }

        return Result.success()
    }

    companion object {
        internal fun filterCandidateTasks(tasks: List<TaskEntity>, dayEnd: Long): List<TaskEntity> {
            val activeTaskIds = tasks.map { it.id }.toSet()
            return tasks.filter { task ->
                val sDate = task.effectiveScheduledDateMillis()
                val dDate = task.resolveDeadlineEpochMillis()
                val scheduledStart = task.resolveScheduledEpochMillis()
                val isScheduledInFuture = scheduledStart != null && scheduledStart > dayEnd
                val isNotFuture = !isScheduledInFuture && (
                    (sDate != null && sDate <= dayEnd) ||
                    (sDate == null && dDate != null && dDate <= dayEnd) ||
                    (sDate == null && dDate == null)
                )

                // Continuation parts (Part 2, 3, etc.) blocked if parent or preceding part is still active
                val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
                val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
                val isContinuation = (partNum != null && partNum >= 2) ||
                    task.id.contains(Regex("""[#_]part\d+""")) ||
                    task.parentTaskId != null
                val isContinuationBlocked = if (!isContinuation) {
                    false
                } else {
                    val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                    val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
                        (parentSlug != task.id && parentSlug in activeTaskIds)
                    val isPart1Active = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeTaskIds
                    val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
                        task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeTaskIds
                    }
                    isParentActive || isPart1Active || isPrecedingPartActive
                }

                // Prerequisites check: blocked if any prerequisite is still active
                val isBlockedByPrereq = task.dependsOnTaskIds.isNotBlank() &&
                    task.dependsOnTaskIds.split(',')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .any { it in activeTaskIds }

                task.status == TaskStatus.ACTIVE &&
                    task.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE &&
                    !isContinuationBlocked &&
                    !isBlockedByPrereq &&
                    task.waitingFor.isBlank() &&
                    isNotFuture
            }
        }

        fun enqueueImmediateUpdate(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "FocusWidgetUpdateWorker_immediate",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
