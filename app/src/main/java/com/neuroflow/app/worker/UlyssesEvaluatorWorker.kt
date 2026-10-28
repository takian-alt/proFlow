package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UlyssesEvaluatorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contractRepository: UlyssesContractRepository,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val contractId = inputData.getString("contractId") ?: return Result.success()

        val contract = contractRepository.getById(contractId) ?: return Result.success()
        if (contract.outcome != null) return Result.success()
        val task = taskRepository.getById(contract.taskId)

        val now = System.currentTimeMillis()
        // If the task is still active and the deadline has not arrived yet, do not evaluate prematurely
        if (now < contract.deadlineAt && task != null && task.completedAt == null) {
            val remainingDelay = (contract.deadlineAt - now).coerceAtLeast(1000L)
            val request = androidx.work.OneTimeWorkRequestBuilder<UlyssesEvaluatorWorker>()
                .setInitialDelay(remainingDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(androidx.work.workDataOf("contractId" to contract.id))
                .addTag("ulysses_eval_${contract.id}")
                .build()
            runCatching { androidx.work.WorkManager.getInstance(applicationContext) }.getOrNull()?.enqueueUniqueWork(
                "ulysses_eval_${contract.id}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
            return Result.success()
        }

        val outcome = if (task == null) {
            ContractOutcome.LOSS
        } else if (task.completedAt != null && task.completedAt <= contract.deadlineAt) {
            ContractOutcome.WIN
        } else {
            ContractOutcome.LOSS
        }

        contractRepository.update(contract.copy(outcome = outcome))

        runCatching {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            if (manager != null) {
                val taskTitle = task?.title ?: "Committed Task"
                val (title, message) = if (outcome == ContractOutcome.WIN) {
                    "🎉 Ulysses Contract Honored!" to "Great job! You finished '$taskTitle' before the deadline and protected your commitment."
                } else {
                    "⚔️ Ulysses Contract Expired" to "Task '$taskTitle' was not completed before the deadline. Consequence: ${contract.consequence}"
                }
                val intent = android.content.Intent(applicationContext, com.neuroflow.app.MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", contract.taskId)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    applicationContext,
                    contract.id.hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_TASKS)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                manager.notify(contract.id.hashCode(), notification)
            }
        }

        return Result.success()
    }
}
