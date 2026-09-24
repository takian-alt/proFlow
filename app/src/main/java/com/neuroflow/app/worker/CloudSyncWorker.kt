package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.sync.BackupStatus
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.GoogleSignInManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Unique work name used to identify the periodic cloud sync job in WorkManager. */
const val CLOUD_SYNC_WORK_NAME = "cloud_sync_daily"

/**
 * A daily [CoroutineWorker] that triggers a full proFlow cloud backup via
 * [CloudBackupManager].
 *
 * The worker runs only when the device has network connectivity and the user
 * is currently signed in.  On a transient failure (e.g. Firestore timeout) the
 * job is retried by WorkManager's built-in back-off policy.
 *
 * Schedule via [schedule] and cancel via [cancel] from wherever WorkManager is
 * accessible (e.g. Application, ViewModel, or another Worker).
 */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudBackupManager: CloudBackupManager,
    private val signInManager: GoogleSignInManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Skip silently when the user is not signed in — no credentials, no sync
        if (signInManager.currentAuthState() !is GoogleAuthState.SignedIn) {
            return Result.success()
        }

        return when (cloudBackupManager.backup()) {
            is BackupStatus.Success -> Result.success()
            is BackupStatus.Error   -> Result.retry()
            else                    -> Result.retry()
        }
    }


    companion object {

        /**
         * Enqueues a once-daily periodic sync constrained to network connectivity.
         *
         * Uses [ExistingPeriodicWorkPolicy.UPDATE] so that calling this multiple
         * times (e.g. on every app launch) updates the constraints without losing
         * the existing work entry.
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                CLOUD_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Cancels the periodic cloud sync job.
         *
         * Call this when the user signs out so that no further backup attempts
         * are made without valid credentials.
         */
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(CLOUD_SYNC_WORK_NAME)
        }
    }
}
