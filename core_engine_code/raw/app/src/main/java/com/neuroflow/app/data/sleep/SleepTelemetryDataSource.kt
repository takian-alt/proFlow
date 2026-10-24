package com.neuroflow.app.data.sleep

import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.service.ChronobiologySignalNormalizer
import com.neuroflow.app.domain.service.NormalizedChronobiologySignal
import com.neuroflow.app.domain.service.RawSleepTelemetry
import com.neuroflow.app.domain.service.TimeSlotWindow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Priority 2 (ADR-003): Abstract Sleep Telemetry Boundary
 *
 * Provides a clean interface and default implementation decoupling domain and UI consumers
 * from Health Connect SDK dependencies while executing passive chronobiological normalization.
 */
interface SleepTelemetryRepository {
    suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0,
        lookbackMillis: Long = 24 * 60 * 60 * 1000L
    ): NormalizedChronobiologySignal

    suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal

    fun observeAllSleepLogs(): Flow<List<SleepLogEntity>>

    fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal
}

@Singleton
class DefaultSleepTelemetryRepository @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val normalizer: ChronobiologySignalNormalizer
) : SleepTelemetryRepository {

    override fun observeAllSleepLogs(): Flow<List<SleepLogEntity>> =
        sleepLogRepository.observeAll()

    override suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double,
        lookbackMillis: Long
    ): NormalizedChronobiologySignal {
        val now = System.currentTimeMillis()
        val toMillis = maxOf(baseWakeMillis, now)
        val fromMillis = toMillis - lookbackMillis
        return getNormalizedSleepForRange(fromMillis, toMillis, baseWakeMillis, baseSleepMinutes)
    }

    override suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val logs = sleepLogRepository.getOverlapping(fromMillis, toMillis)
        return normalizeLogs(logs, baseWakeMillis, baseSleepMinutes)
    }

    override fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val validLogs = logs.filter { it.endAt > it.startAt }.sortedBy { it.startAt }

        if (validLogs.isEmpty()) {
            return normalizer.normalize(
                telemetry = RawSleepTelemetry(
                    intervals = emptyList(),
                    confidence = 0.0
                ),
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = baseSleepMinutes
            )
        }

        val intervals = validLogs.map { TimeSlotWindow(it.startAt, it.endAt) }

        var totalDeep = 0.0
        var totalRem = 0.0
        var anyStageParsed = false

        for (log in validLogs) {
            val deepParsed = extractMinutes(log.notes, "deep")
            val remParsed = extractMinutes(log.notes, "rem")
            if (deepParsed != null || remParsed != null) {
                anyStageParsed = true
                totalDeep += deepParsed ?: 0.0
                totalRem += remParsed ?: 0.0
            }
        }

        val hasWearableSource = validLogs.any {
            it.source.contains("HEALTH", ignoreCase = true) || it.source.contains("WEARABLE", ignoreCase = true)
        }
        val confidence = when {
            hasWearableSource -> 1.0
            validLogs.any { it.source.contains("MANUAL", ignoreCase = true) || it.source.contains("USER", ignoreCase = true) } -> 0.85
            validLogs.any { it.source == "AUTO_DEFAULT" } -> 0.4
            else -> 0.85
        }

        val nocturnalLogs = validLogs.filter { it.startAt < baseWakeMillis || (it.endAt - it.startAt) > 180 * 60_000L }
        val effectiveNocturnalLogs = if (nocturnalLogs.isNotEmpty()) nocturnalLogs else emptyList()
        val (nocturnalConsolidated, nocturnalWaso) = normalizer.coalesceWasoEpisodes(effectiveNocturnalLogs.map { TimeSlotWindow(it.startAt, it.endAt) })
        val nocturnalRawMinutes = (nocturnalConsolidated.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - nocturnalWaso).coerceAtLeast(0.0)

        val finalDeep = if (anyStageParsed) totalDeep else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val finalRem = if (anyStageParsed) totalRem else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val latestWake = nocturnalLogs.maxOfOrNull { it.endAt }

        val telemetry = RawSleepTelemetry(
            intervals = intervals,
            deepSleepMinutes = finalDeep,
            remSleepMinutes = finalRem,
            detectedWakeMillis = latestWake,
            confidence = confidence
        )

        return normalizer.normalize(
            telemetry = telemetry,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )
    }

    private fun extractMinutes(notes: String, tag: String): Double? {
        val regex = Regex("""$tag[:=]\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(notes) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
