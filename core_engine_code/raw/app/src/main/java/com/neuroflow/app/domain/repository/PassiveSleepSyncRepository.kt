package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for passive sleep session synchronization.
 * Abstracts Google Health Connect, wearable providers, or platform health services
 * to complement manual sleep logs and auto-default estimations with measured sleep.
 */
interface PassiveSleepSyncRepository {

    sealed interface SyncResult {
        data class Success(
            val importedLogs: List<SleepLogEntity>,
            val skippedDuplicates: Int,
            val replacedFallbackCount: Int
        ) : SyncResult

        data class PermissionRequired(
            val missingPermissions: Set<String>
        ) : SyncResult

        data class ProviderUnavailable(
            val reason: String
        ) : SyncResult

        data class Failure(
            val throwable: Throwable
        ) : SyncResult
    }

    data class RawSleepSession(
        val externalRecordId: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val sourceAppPackage: String = SOURCE_HEALTH_CONNECT,
        val stageSummary: String? = null,
        val notes: String = ""
    )

    enum class HealthProviderStatus {
        AVAILABLE,
        NOT_INSTALLED,
        UPDATE_REQUIRED,
        NOT_SUPPORTED
    }

    companion object {
        const val SOURCE_HEALTH_CONNECT = "HEALTH_CONNECT"
        const val PERMISSION_READ_SLEEP = "android.permission.health.READ_SLEEP"
    }

    fun observeProviderStatus(): Flow<HealthProviderStatus>
    suspend fun getProviderStatus(): HealthProviderStatus
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession>
    suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult
}
