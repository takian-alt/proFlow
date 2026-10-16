package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.PERMISSION_READ_SLEEP
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.SOURCE_HEALTH_CONNECT
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.HealthProviderStatus
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.RawSleepSession
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult
import com.neuroflow.app.domain.repository.SleepPressureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveSleepSyncManager @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val sleepPressureRepository: SleepPressureRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : PassiveSleepSyncRepository {

    private val providerStatusFlow = MutableStateFlow(HealthProviderStatus.AVAILABLE)
    private val externalProviderSource = mutableListOf<RawSleepSession>()

    /**
     * Injects mock or bridged external sessions (e.g. from Health Connect API or wearable SDK).
     */
    fun setExternalSessions(sessions: List<RawSleepSession>) {
        externalProviderSource.clear()
        externalProviderSource.addAll(sessions)
    }

    override fun observeProviderStatus(): Flow<HealthProviderStatus> = providerStatusFlow.asStateFlow()

    override suspend fun getProviderStatus(): HealthProviderStatus = providerStatusFlow.value

    fun setProviderStatus(status: HealthProviderStatus) {
        providerStatusFlow.value = status
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        return providerStatusFlow.value == HealthProviderStatus.AVAILABLE
    }

    override suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession> {
        val safeStart = minOf(fromMillis, toMillis)
        val safeEnd = maxOf(fromMillis, toMillis)
        return externalProviderSource.filter { session ->
            session.endTimeMillis > safeStart && session.startTimeMillis < safeEnd
        }
    }

    override suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult {
        try {
            val status = getProviderStatus()
            if (status != HealthProviderStatus.AVAILABLE) {
                return SyncResult.ProviderUnavailable("Health provider unavailable: $status")
            }

            if (!hasRequiredPermissions()) {
                return SyncResult.PermissionRequired(setOf(PERMISSION_READ_SLEEP))
            }

            val externalSessions = fetchExternalSleepSessions(fromMillis, toMillis)
            if (externalSessions.isEmpty()) {
                return SyncResult.Success(
                    importedLogs = emptyList(),
                    skippedDuplicates = 0,
                    replacedFallbackCount = 0
                )
            }

            val imported = mutableListOf<SleepLogEntity>()
            var skipped = 0
            var replacedFallback = 0

            for (session in externalSessions) {
                val safeStart = minOf(session.startTimeMillis, session.endTimeMillis)
                val safeEnd = maxOf(session.startTimeMillis, session.endTimeMillis)
                val duration = safeEnd - safeStart

                // Bounds validation
                if (duration < SleepPressureRepository.MIN_SLEEP_LOG_DURATION_MILLIS ||
                    duration > SleepPressureRepository.MAX_SLEEP_LOG_DURATION_MILLIS
                ) {
                    skipped++
                    continue
                }

                val overlapping = sleepLogRepository.getOverlapping(safeStart, safeEnd)

                // 1. Manual sleep logs take precedence over passive detection
                val manualOverlapping = overlapping.filter { it.source == "MANUAL" }
                if (manualOverlapping.isNotEmpty()) {
                    skipped++
                    continue
                }

                // 2. Health Connect deduplication (avoid importing identical/near-identical session twice)
                val healthOverlapping = overlapping.filter { it.source == SOURCE_HEALTH_CONNECT }
                val isDuplicate = healthOverlapping.any { existing ->
                    val overlap = minOf(existing.endAt, safeEnd) - maxOf(existing.startAt, safeStart)
                    overlap > 0.8f * duration
                }
                if (isDuplicate) {
                    skipped++
                    continue
                }

                // 3. Health Connect measured sleep supersedes AUTO_DEFAULT fallback estimations
                val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
                autoOverlapping.forEach { autoLog ->
                    sleepLogRepository.deleteById(autoLog.id)
                    replacedFallback++
                }

                val note = if (session.notes.isNotBlank()) session.notes else "Synced via Health Connect"
                val addedLog = sleepLogRepository.addLog(
                    startAt = safeStart,
                    endAt = safeEnd,
                    source = SOURCE_HEALTH_CONNECT,
                    notes = note
                )
                imported.add(addedLog)
            }

            if (imported.isNotEmpty() || replacedFallback > 0) {
                sleepPressureRepository.refreshCurrentPressure()
            }

            return SyncResult.Success(
                importedLogs = imported,
                skippedDuplicates = skipped,
                replacedFallbackCount = replacedFallback
            )
        } catch (e: Exception) {
            return SyncResult.Failure(e)
        }
    }
}
