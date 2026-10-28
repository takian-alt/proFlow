package com.neuroflow.app.domain.engine

import android.content.Context
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis

object AutonomyNudgeEngine {
    private const val TAG_PREFIX = "autonomy_nudge_"
    private const val GLOBAL_TAG = "autonomy_nudge_all"
    private const val DELAY_HOURS = 2L

    fun uniqueWorkName(taskId: String): String = "$TAG_PREFIX$taskId"
    fun workTag(taskId: String): String = "$TAG_PREFIX$taskId"
    fun globalTag(): String = GLOBAL_TAG

    fun scheduleNudge(workManager: androidx.work.WorkManager, task: TaskEntity) {
        val now = System.currentTimeMillis()
        val dueAt = task.effectiveReminderTargetMillis()
        val delayMillis = if (dueAt != null && dueAt > now) {
            dueAt - now
        } else {
            java.util.concurrent.TimeUnit.HOURS.toMillis(DELAY_HOURS)
        }

        val data = androidx.work.Data.Builder()
            .putString("taskId", task.id)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<com.neuroflow.app.worker.AutonomyNudgeWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workTag(task.id))
            .addTag(globalTag())
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(task.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNudge(context: Context, task: TaskEntity) {
        scheduleNudge(androidx.work.WorkManager.getInstance(context), task)
    }

    fun cancelNudge(workManager: androidx.work.WorkManager, taskId: String) {
        workManager.cancelAllWorkByTag(workTag(taskId))
        workManager.cancelUniqueWork(uniqueWorkName(taskId))
    }

    fun cancelNudge(context: Context, taskId: String) {
        cancelNudge(androidx.work.WorkManager.getInstance(context), taskId)
    }
}
