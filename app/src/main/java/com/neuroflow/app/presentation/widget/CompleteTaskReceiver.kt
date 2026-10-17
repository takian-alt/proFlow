package com.neuroflow.app.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Priority 3 (ADR-003): Interactive Home Screen Widget Task Completion
 *
 * BroadcastReceiver executing zero-app-launch 1-tap task completion for RemoteViews widget.
 * Updates Room v22 and refreshes widget in <150ms.
 */
class CompleteTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION_COMPLETE_TASK) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CompleteTaskWidgetAction.execute(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.neuroflow.app.action.COMPLETE_TASK_WIDGET"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
