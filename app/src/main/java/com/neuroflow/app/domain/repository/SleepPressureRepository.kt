package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.engine.SleepPressureDetector
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepPressureRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepLogRepository: SleepLogRepository
) {

    companion object {
        const val MIN_SLEEP_LOG_DURATION_MILLIS = 60_000L
        const val MAX_SLEEP_LOG_DURATION_MINUTES = 16 * 60
        const val MAX_SLEEP_LOG_DURATION_MILLIS = MAX_SLEEP_LOG_DURATION_MINUTES * 60_000L
        private const val AUTO_FALLBACK_AFTER_WAKE_MINUTES = 12 * 60
        private const val HOUR_MILLIS = 60 * 60_000L
    }

    sealed interface AddSleepLogResult {
        data class Added(val snapshot: Snapshot) : AddSleepLogResult
        data object OverlapsExisting : AddSleepLogResult
        data class TooLong(val maxMinutes: Int = MAX_SLEEP_LOG_DURATION_MINUTES) : AddSleepLogResult
    }

    data class Snapshot(
        val pressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: SleepPressureDetector.FatigueZone,
        val refreshedAtMillis: Long
    )

    private data class SleepInterval(
        val startAt: Long,
        val endAt: Long
    )

    fun observeSleepLogs(): Flow<List<SleepLogEntity>> = sleepLogRepository.observeAll()

    suspend fun addSleepLog(startAtMillis: Long, endAtMillis: Long): AddSleepLogResult {
        val safeStart = minOf(startAtMillis, endAtMillis)
        val safeEnd = maxOf(startAtMillis, endAtMillis)
        val normalizedEnd = if (safeEnd <= safeStart) safeStart + MIN_SLEEP_LOG_DURATION_MILLIS else safeEnd

        val durationMillis = normalizedEnd - safeStart
        if (durationMillis > MAX_SLEEP_LOG_DURATION_MILLIS) {
            return AddSleepLogResult.TooLong()
        }

        val overlapping = sleepLogRepository.getOverlapping(safeStart, normalizedEnd)
        if (overlapping.isNotEmpty()) {
            return AddSleepLogResult.OverlapsExisting
        }

        sleepLogRepository.addLog(
            startAt = safeStart,
            endAt = normalizedEnd,
            source = "MANUAL"
        )

        val prefs = preferencesDataStore.preferencesFlow.first()
        val currentTrackingStart = prefs.sleepPressureTrackingStartedAtMillis
        val newTrackingStart = if (currentTrackingStart > 0L) {
            minOf(currentTrackingStart, safeStart)
        } else {
            safeStart
        }

        if (newTrackingStart != currentTrackingStart) {
            preferencesDataStore.updatePreferences {
                it.copy(sleepPressureTrackingStartedAtMillis = newTrackingStart)
            }
        }

        return AddSleepLogResult.Added(refreshCurrentPressure())
    }

    suspend fun deleteSleepLog(logId: String): Snapshot {
        sleepLogRepository.deleteById(logId)
        return refreshCurrentPressure()
    }

    suspend fun clearSleepLogsAndReset(nowMillis: Long = System.currentTimeMillis()): Snapshot {
        sleepLogRepository.deleteAll()

        val prefs = preferencesDataStore.preferencesFlow.first()
        val trackingStart = defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        val pressure = minutesBetween(trackingStart, nowMillis)

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressureTrackingStartedAtMillis = trackingStart,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressurePoints = pressure
            )
        }

        val fatiguePercent = SleepPressureDetector.fatiguePercent(pressure)
        val zone = SleepPressureDetector.fatigueZone(pressure)

        return Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis
        )
    }

    suspend fun refreshCurrentPressure(nowMillis: Long = System.currentTimeMillis()): Snapshot {
        val prefs = preferencesDataStore.preferencesFlow.first()

        // Phase 1 boundary fix: use the preference-stored tracking start when valid
        // Otherwise default to configured wake-up hour
        val trackingStart = if (prefs.sleepPressureTrackingStartedAtMillis > 0L &&
            prefs.sleepPressureTrackingStartedAtMillis < nowMillis
        ) {
            prefs.sleepPressureTrackingStartedAtMillis
        } else {
            defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        }

        var pressure = 0
        var cursor = trackingStart

        val logs = sleepLogRepository.getOverlapping(trackingStart, nowMillis)
        val sleepIntervals = logs
            .map { SleepInterval(it.startAt, it.endAt) }
            .toMutableList()

        buildAutoFallbackSleepInterval(
            prefs = prefs,
            trackingStart = trackingStart,
            nowMillis = nowMillis
        )?.let { fallback ->
            val persisted = sleepLogRepository.addLog(
                startAt = fallback.startAt,
                endAt = fallback.endAt,
                source = "AUTO_DEFAULT"
            )
            sleepIntervals += SleepInterval(persisted.startAt, persisted.endAt)
        }

        sleepIntervals.sortBy { it.startAt }

        sleepIntervals.forEach { interval ->
            val boundedStart = maxOf(cursor, interval.startAt)
            val boundedEnd = minOf(nowMillis, interval.endAt)
            if (boundedEnd <= boundedStart) {
                return@forEach
            }

            if (boundedStart > cursor) {
                val awakeMinutes = minutesBetween(cursor, boundedStart)
                pressure = SleepPressureDetector
                    .applyAwakeMinutes(pressure, awakeMinutes)
                    .pressurePoints
            }

            val sleepMinutes = minutesBetween(boundedStart, boundedEnd)
            pressure = SleepPressureDetector
                .applySleepSession(pressure, sleepMinutes)
                .pressurePoints

            cursor = boundedEnd
        }

        if (cursor < nowMillis) {
            val awakeMinutes = minutesBetween(cursor, nowMillis)
            pressure = SleepPressureDetector
                .applyAwakeMinutes(pressure, awakeMinutes)
                .pressurePoints
        }

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressurePoints = pressure,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressureTrackingStartedAtMillis = trackingStart
            )
        }

        val fatiguePercent = SleepPressureDetector.fatiguePercent(pressure)
        val zone = SleepPressureDetector.fatigueZone(pressure)

        return Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis
        )
    }

    private fun minutesBetween(startMillis: Long, endMillis: Long): Int {
        // Phase 1 boundary guard: prevent negative or zero duration calculations
        if (endMillis <= startMillis) return 0
        return ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    private fun defaultTrackingStartMillis(nowMillis: Long, wakeUpHour: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var anchor = now.toLocalDate()
            .atTime(wakeUpHour.coerceIn(0, 23), 0)
            .atZone(zoneId)

        if (anchor.toInstant().toEpochMilli() > nowMillis) {
            anchor = anchor.minusDays(1)
        }

        return anchor.toInstant().toEpochMilli()
    }

    private suspend fun buildAutoFallbackSleepInterval(
        prefs: UserPreferences,
        trackingStart: Long,
        nowMillis: Long
    ): SleepInterval? {
                // Phase 1: respect the auto-fallback flag from preferences
                if (!prefs.autoFallbackSleepInsertionEnabled) {
                    return null
                }
        
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val wakeHour = prefs.wakeUpHour.coerceIn(0, 23)
        val sleepHour = prefs.sleepHour.coerceIn(0, 23)

        val todayWake = now.toLocalDate()
            .atTime(wakeHour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        if (nowMillis < todayWake + (AUTO_FALLBACK_AFTER_WAKE_MINUTES * 60_000L)) {
            return null
        }

        val startOfToday = now.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfYesterday = now.toLocalDate().minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val hasUpdateYesterday = prefs.sleepPressureLastComputedAtMillis in startOfYesterday until startOfToday
        if (hasUpdateYesterday) {
            return null
        }

        val defaultSleepStart = now.toLocalDate().minusDays(1)
            .atTime(sleepHour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        var defaultWakeEnd = now.toLocalDate().minusDays(1)
            .atTime(wakeHour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        if (defaultWakeEnd <= defaultSleepStart) {
                        // Phase 1 boundary: handle cross-day sleep (e.g., 22:00 sleep to 06:00 wake)
            defaultWakeEnd += 24 * HOUR_MILLIS
        }

        // If user has logged manually at any time since the default sleep-hour window started,
        // skip auto fallback because user interaction implies manual intent.
        val logsSinceDefaultSleep = sleepLogRepository.getOverlapping(defaultSleepStart, nowMillis)
        val hasManualLogSinceDefaultSleep = logsSinceDefaultSleep.any { it.source == "MANUAL" }
        if (hasManualLogSinceDefaultSleep) {
            return null
        }

        val boundedStart = maxOf(trackingStart, defaultSleepStart)
        val boundedEnd = minOf(nowMillis, defaultWakeEnd)
        // Phase 1 boundary check: ensure end > start to avoid invalid time windows
        if (boundedEnd <= boundedStart) {
            return null
        }

        val duration = boundedEnd - boundedStart
            // Phase 1 boundary guard: ensure duration is positive and within bounds
        if (duration < MIN_SLEEP_LOG_DURATION_MILLIS || duration > MAX_SLEEP_LOG_DURATION_MILLIS) {
            return null
        }

        val overlapsExisting = logsSinceDefaultSleep.any {
            it.endAt > boundedStart && it.startAt < boundedEnd
        }
        if (overlapsExisting) {
            return null
        }

        return SleepInterval(
            startAt = boundedStart,
            endAt = boundedEnd
        )
    }
}
