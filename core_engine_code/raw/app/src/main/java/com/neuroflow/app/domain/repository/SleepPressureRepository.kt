package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

        fun coalesceSleepIntervals(sleepIntervals: List<SleepInterval>): List<SleepInterval> {
            if (sleepIntervals.isEmpty()) return emptyList()
            val sorted = sleepIntervals.sortedBy { it.startAt }
            val merged = mutableListOf<SleepInterval>()
            var current = sorted[0]
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.startAt <= current.endAt) {
                    current = current.copy(endAt = maxOf(current.endAt, next.endAt))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged
        }
    }

    sealed interface AddSleepLogResult {
        data class Added(val snapshot: Snapshot) : AddSleepLogResult
        data object OverlapsExisting : AddSleepLogResult
        data class TooLong(val maxMinutes: Int = MAX_SLEEP_LOG_DURATION_MINUTES) : AddSleepLogResult
    }

    data class Snapshot(
        val pressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: FatigueZone,
        val refreshedAtMillis: Long,
        val minutesSinceWake: Int? = null,
        val isSleepInertiaActive: Boolean = false,
        val sleepInertiaPenalty: Float = 0f,
        val chronicRestrictionMultiplier: Float = 1.0f,
        val adjustedFatiguePercent: Int = fatiguePercent,
        val adjustedFatigueZone: FatigueZone = fatigueZone
    )

    data class SleepInterval(
        val startAt: Long,
        val endAt: Long
    )

    private val dismissedFallbackIntervals = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val refreshMutex = Mutex()

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
        val realOverlapping = overlapping.filter { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
        if (realOverlapping.isNotEmpty()) {
            return AddSleepLogResult.OverlapsExisting
        }

        val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
        if (autoOverlapping.isNotEmpty()) {
            val dismissedKeys = autoOverlapping.map { "${it.startAt}_${it.endAt}" }.toSet()
            dismissedFallbackIntervals.addAll(dismissedKeys)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + dismissedKeys)
            }
            autoOverlapping.forEach { autoLog ->
                sleepLogRepository.deleteById(autoLog.id)
            }
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
        val existing = sleepLogRepository.getById(logId)
        if (existing != null && existing.source == "AUTO_DEFAULT") {
            val key = "${existing.startAt}_${existing.endAt}"
            dismissedFallbackIntervals.add(key)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + key)
            }
        }
        sleepLogRepository.deleteById(logId)
        return refreshCurrentPressure()
    }

    suspend fun clearSleepLogsAndReset(nowMillis: Long = System.currentTimeMillis()): Snapshot {
        dismissedFallbackIntervals.clear()
        sleepLogRepository.deleteAll()

        val prefs = preferencesDataStore.preferencesFlow.first()
        val trackingStart = defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        val pressure = minutesBetween(trackingStart, nowMillis)

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressureTrackingStartedAtMillis = trackingStart,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressurePoints = pressure,
                dismissedSleepFallbacks = emptySet()
            )
        }

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val wakeMins = minutesBetween(trackingStart, nowMillis)

        return Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = wakeMins,
            isSleepInertiaActive = FatigueZone.isSleepInertiaActive(wakeMins),
            sleepInertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(wakeMins),
            chronicRestrictionMultiplier = 1.0f,
            adjustedFatiguePercent = fatiguePercent,
            adjustedFatigueZone = zone
        )
    }

    suspend fun refreshCurrentPressure(nowMillis: Long = System.currentTimeMillis()): Snapshot = refreshMutex.withLock {
        val prefs = preferencesDataStore.preferencesFlow.first()

        // Phase 1 boundary fix: use the preference-stored tracking start when valid
        // Otherwise default to configured wake-up hour
        val trackingStart = if (prefs.sleepPressureTrackingStartedAtMillis > 0L &&
            prefs.sleepPressureTrackingStartedAtMillis < nowMillis
        ) {
            maxOf(prefs.sleepPressureTrackingStartedAtMillis, nowMillis - 7 * 86_400_000L)
        } else {
            defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        }

        var pressure = 0
        var cursor = trackingStart

        val logs = sleepLogRepository.getOverlapping(trackingStart, nowMillis)
        val sleepIntervals = logs
            .map { SleepInterval(it.startAt, it.endAt) }
            .toMutableList()

        buildAutoFallbackSleepIntervals(
            prefs = prefs,
            trackingStart = trackingStart,
            nowMillis = nowMillis
        ).forEach { fallback ->
            val persisted = sleepLogRepository.addLog(
                startAt = fallback.startAt,
                endAt = fallback.endAt,
                source = "AUTO_DEFAULT"
            )
            sleepIntervals += SleepInterval(persisted.startAt, persisted.endAt)
        }

        sleepIntervals.sortBy { it.startAt }

        // Bug 4 Fix (ADR-030): Sweep-line merge overlapping or contiguous sleep intervals
        // so that multi-record sleep sessions receive continuous non-linear 90m SWS cycle recovery bonuses
        val coalescedIntervals = coalesceSleepIntervals(sleepIntervals)

        coalescedIntervals.forEach { interval ->
            val boundedStart = maxOf(cursor, interval.startAt)
            val boundedEnd = minOf(nowMillis, interval.endAt)
            if (boundedEnd <= boundedStart) {
                return@forEach
            }

            if (boundedStart > cursor) {
                val awakeMinutes = minutesBetween(cursor, boundedStart)
                pressure = FatigueZone
                    .applyAwakeMinutes(pressure, awakeMinutes)
                    .pressurePoints
            }

            val sleepMinutes = minutesBetween(boundedStart, boundedEnd)
            pressure = FatigueZone
                .applySleepSession(pressure, sleepMinutes)
                .pressurePoints

            cursor = boundedEnd
        }

        if (cursor < nowMillis) {
            val awakeMinutes = minutesBetween(cursor, nowMillis)
            pressure = FatigueZone
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

        val lastIntervalEnd = coalescedIntervals.map { it.endAt }.filter { it <= nowMillis }.maxOrNull()
        val wakeAnchor = lastIntervalEnd ?: trackingStart
        val minutesSinceWake = minutesBetween(wakeAnchor, nowMillis)

        val sevenDaysAgo = nowMillis - 7 * 24 * 60 * 60_000L
        val rollingLogs = sleepLogRepository.getOverlapping(sevenDaysAgo, nowMillis)
        val rollingAvgDailyMinutes = computeRollingDailySleepAverage(rollingLogs, nowMillis, ZoneId.systemDefault()).toFloat()
        val chronicMultiplier = FatigueZone.calculateChronicRestrictionMultiplier(rollingAvgDailyMinutes)
        val inertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(minutesSinceWake)
        val inertiaActive = FatigueZone.isSleepInertiaActive(minutesSinceWake)

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val adjFatiguePercent = FatigueZone.adjustedFatiguePercent(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )
        val adjZone = FatigueZone.adjustedFatigueZone(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )

        Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = minutesSinceWake,
            isSleepInertiaActive = inertiaActive,
            sleepInertiaPenalty = inertiaPenalty,
            chronicRestrictionMultiplier = chronicMultiplier,
            adjustedFatiguePercent = adjFatiguePercent,
            adjustedFatigueZone = adjZone
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

    private suspend fun buildAutoFallbackSleepIntervals(
        prefs: UserPreferences,
        trackingStart: Long,
        nowMillis: Long
    ): List<SleepInterval> {
        if (!prefs.autoFallbackSleepInsertionEnabled) {
            return emptyList()
        }

        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val wakeHour = prefs.wakeUpHour.coerceIn(0, 23)
        val sleepHour = prefs.sleepHour.coerceIn(0, 23)

        val result = mutableListOf<SleepInterval>()

        // Check up to 7 past days for missing automatic fallback sleep logs
        for (dayOffset in 0..7) {
            val targetDate = now.toLocalDate().minusDays(dayOffset.toLong())

            val defaultSleepStart = targetDate
                .atTime(sleepHour, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

            val defaultWakeEnd = if (wakeHour <= sleepHour) {
                targetDate.plusDays(1).atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            } else {
                targetDate.atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            }

            // 12-hour post-wake delay check relative to actual default wake time
            if (nowMillis < defaultWakeEnd + (AUTO_FALLBACK_AFTER_WAKE_MINUTES * 60_000L)) {
                continue
            }

            if (defaultWakeEnd < trackingStart) {
                continue
            }

            // Bug 2 Fix (ADR-025): Scope query strictly to the target night's window
            // to prevent newer manual logs from blocking fallback sleep for older unlogged nights.
            val queryStart = defaultSleepStart - 4 * HOUR_MILLIS
            val queryEnd = defaultWakeEnd + 4 * HOUR_MILLIS
            val logsInWindow = sleepLogRepository.getOverlapping(queryStart, queryEnd)
            val hasManualLogForNight = logsInWindow.any { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
            if (hasManualLogForNight) {
                continue
            }

            val boundedStart = defaultSleepStart
            val boundedEnd = minOf(nowMillis, defaultWakeEnd)
            if (boundedEnd <= boundedStart) {
                continue
            }

            val duration = boundedEnd - boundedStart
            if (duration < MIN_SLEEP_LOG_DURATION_MILLIS || duration > MAX_SLEEP_LOG_DURATION_MILLIS) {
                continue
            }

            val overlapsExisting = logsInWindow.any {
                it.endAt > boundedStart && it.startAt < boundedEnd
            }
            if (overlapsExisting) {
                continue
            }

            val intervalKey = "${boundedStart}_${boundedEnd}"
            if (dismissedFallbackIntervals.contains(intervalKey) || prefs.dismissedSleepFallbacks.contains(intervalKey)) {
                continue
            }

            result.add(SleepInterval(startAt = boundedStart, endAt = boundedEnd))
        }

        result.sortBy { it.startAt }
        return result
    }
}
