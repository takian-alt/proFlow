package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TimeBasedHyperFocusExpiryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val hyperFocusManager: HyperFocusManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = hyperFocusDataStore.current()
        if (prefs.isActive &&
            prefs.sessionMode == HyperFocusSessionMode.TIME_BASED &&
            (prefs.sessionEndsAtMillis ?: Long.MAX_VALUE) <= System.currentTimeMillis()
        ) {
            hyperFocusManager.deactivate()
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "time_based_hyperfocus_expiry"
    }
}
