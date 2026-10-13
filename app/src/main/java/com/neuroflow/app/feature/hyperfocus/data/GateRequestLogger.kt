package com.neuroflow.app.feature.hyperfocus.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single writer for the audit row, because that row is the only proof a grant was asked for and
 * why. The gate, the gateway and the emergency sit on three surfaces driven by two ViewModels; a
 * second copy of this in the home screen would let one of them drift and quietly hand out an access
 * nobody logged.
 */
@Singleton
class GateRequestLogger @Inject constructor(
    private val gateRequestDao: GateRequestDao,
    private val appCatalogRepository: AppCatalogRepository,
    private val hyperFocusDataStore: HyperFocusDataStore
) {
    suspend fun log(
        packageName: String,
        statedReason: String,
        taskType: String,
        mismatchCount: Int,
        elapsedMillis: Long,
        grantSource: GateGrantSource
    ) {
        val prefs = hyperFocusDataStore.current()
        runCatching {
            gateRequestDao.insert(
                GateRequestEntity(
                    sessionId = prefs.sessionId,
                    packageName = packageName,
                    appLabel = appCatalogRepository.labelFor(packageName),
                    statedReason = statedReason,
                    taskType = taskType,
                    mismatchCount = mismatchCount,
                    elapsedMillis = elapsedMillis,
                    grantSource = grantSource.name,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            gateRequestDao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MILLIS)
        }.onFailure {
            Log.w(TAG, "gate request log write failed for $packageName", it)
        }
    }

    /** The caller grants the emergency; this records that it happened, and to which app. */
    suspend fun logEmergencyGrant(packageName: String) {
        log(
            packageName = packageName,
            statedReason = EMERGENCY_REASON,
            taskType = EMERGENCY_TASK_TYPE,
            mismatchCount = 0,
            elapsedMillis = 0L,
            grantSource = GateGrantSource.EMERGENCY
        )
    }

    companion object {
        const val EMERGENCY_REASON = "Emergency bypass (no gate)"
        const val EMERGENCY_TASK_TYPE = "NONE"
        private const val TAG = "HyperFocusGate"
        private const val RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
