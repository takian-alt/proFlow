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
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.presentation.widget.CompleteTaskReceiver
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.roundToInt

@HiltWorker
class FocusWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val tasks = taskRepository.getActiveTasks()
            val preferences = preferencesDataStore.preferencesFlow.first()
            val nowMillis = System.currentTimeMillis()

            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val wakeHour = preferences.wakeUpHour
            val hoursAwake = (currentHour - wakeHour).let { if (it < 0) it + 24 else it }
            val wakeTimeMillis = nowMillis - (hoursAwake * 3600_000L)
            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = 480
            )

            val sortedWithScores = tasks.map { task ->
                val schedulable = SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = when (task.taskType) {
                        TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
                        TaskType.CREATIVE -> TaskCategory.CREATIVE
                        TaskType.ADMIN -> TaskCategory.ADMIN
                        TaskType.PHYSICAL -> TaskCategory.EXERCISE
                        TaskType.GENERAL -> TaskCategory.GENERAL
                        TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
                    },
                    priority = when (task.priority) {
                        Priority.HIGH -> 2
                        Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        EnergyLevel.HIGH -> EnergyDemand.HIGH
                        EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                        else -> EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId
                )
                val breakdown = taskSchedulerService.evaluateTaskScore(schedulable, snapshot)
                task to breakdown
            }.sortedByDescending { it.second.totalScore }

            val topPair = sortedWithScores.firstOrNull()
            val topTask = topPair?.first
            val topScore = topPair?.second?.totalScore?.roundToInt() ?: 0

            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            // 1. Update FocusWidgetProvider (widget_focus)
            val focusViews = RemoteViews(appContext.packageName, R.layout.widget_focus)
            if (topTask != null) {
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
                val durationText = if (topTask.estimatedDurationMinutes > 0) "${topTask.estimatedDurationMinutes}m" else "Quick"
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

        } catch (_: Exception) {
            // Stale widget preferred over crash
        }

        return Result.success()
    }

    companion object {
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
