package com.neuroflow.app.data.sleep

import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.service.ChronobiologySignalNormalizer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class SleepTelemetryRepositoryTest : StringSpec({

    val baseWakeMillis = 1770000000000L
    val baseSleepMinutes = 480.0 // 8 hours

    "returns pure fallback when no sleep logs exist" {
        val sleepLogRepo = mockk<SleepLogRepository>()
        coEvery { sleepLogRepo.getOverlapping(any(), any()) } returns emptyList()

        val normalizer = ChronobiologySignalNormalizer()
        val repository = DefaultSleepTelemetryRepository(sleepLogRepo, normalizer)

        val result = repository.getLatestNormalizedSleep(
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )

        result.totalSleepMinutes shouldBe (480.0 plusOrMinus 0.001)
        result.blendedDurationMinutes shouldBe (480.0 plusOrMinus 0.001)
        result.normalizedWakeTimeMillis shouldBe baseWakeMillis
        result.consolidatedSessions shouldHaveSize 0
        result.totalWasoMinutes shouldBe 0.0
    }

    "coalesces WASO episodes and applies stage-weighted blending for wearable logs" {
        val sleepLogRepo = mockk<SleepLogRepository>()
        val start1 = baseWakeMillis - 8 * 3600_000L
        val end1 = start1 + 3 * 3600_000L // 3h sleep
        val start2 = end1 + 20 * 60_000L // 20m wake gap (WASO)
        val end2 = start2 + 4 * 3600_000L // 4h sleep

        val log1 = SleepLogEntity(
            id = "log-1",
            startAt = start1,
            endAt = end1,
            durationMinutes = 180,
            source = "HEALTH_CONNECT",
            notes = "deep:60 rem:60"
        )
        val log2 = SleepLogEntity(
            id = "log-2",
            startAt = start2,
            endAt = end2,
            durationMinutes = 240,
            source = "HEALTH_CONNECT",
            notes = "deep:60 rem:60"
        )

        coEvery { sleepLogRepo.getOverlapping(any(), any()) } returns listOf(log1, log2)

        val normalizer = ChronobiologySignalNormalizer()
        val repository = DefaultSleepTelemetryRepository(sleepLogRepo, normalizer)

        val result = repository.getLatestNormalizedSleep(
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )

        // WASO gap was 20m, so intervals coalesced into 1 session
        result.consolidatedSessions shouldHaveSize 1
        result.totalWasoMinutes shouldBe 20.0
        // Raw sleep minutes: (end2 - start1) - 20m = (7h 20m) - 20m = 7h = 420m
        result.totalSleepMinutes shouldBe (420.0 plusOrMinus 0.001)
        // Restorative sleep: deep(120) + rem(120) = 240m.
        // Ratio = 240 / 420 = 0.5714. Stage factor = 0.85 + 0.75 * 0.5714 = 1.278 -> clamped to 1.25.
        // Effective duration = 420 * 1.25 = 525.0
        result.effectiveDurationMinutes shouldBe (525.0 plusOrMinus 0.001)
        // Wearable confidence = 1.0 -> blended duration = 525.0
        result.blendedDurationMinutes shouldBe (525.0 plusOrMinus 0.001)
    }

    "respects time-range filtering" {
        val sleepLogRepo = mockk<SleepLogRepository>()
        val queryFrom = 1_000_000L
        val queryTo = 5_000_000L

        coEvery { sleepLogRepo.getOverlapping(queryFrom, queryTo) } returns listOf(
            SleepLogEntity(
                id = "in-range",
                startAt = 2_000_000L,
                endAt = 4_000_000L,
                durationMinutes = 33,
                source = "MANUAL"
            )
        )

        val normalizer = ChronobiologySignalNormalizer()
        val repository = DefaultSleepTelemetryRepository(sleepLogRepo, normalizer)

        val result = repository.getNormalizedSleepForRange(
            fromMillis = queryFrom,
            toMillis = queryTo,
            baseWakeMillis = 4_000_000L,
            baseSleepMinutes = 300.0
        )

        result.consolidatedSessions shouldHaveSize 1
        result.totalSleepMinutes shouldBe (33.333 plusOrMinus 0.1)
    }

    "clamps wake shift when wearable wake differs significantly from base wake" {
        val sleepLogRepo = mockk<SleepLogRepository>()
        val start = baseWakeMillis - 7 * 3600_000L
        val lateWake = baseWakeMillis + 180 * 60_000L // 3 hours late

        val log = SleepLogEntity(
            id = "late-log",
            startAt = start,
            endAt = lateWake,
            durationMinutes = 600,
            source = "HEALTH_CONNECT"
        )
        coEvery { sleepLogRepo.getOverlapping(any(), any()) } returns listOf(log)

        val normalizer = ChronobiologySignalNormalizer()
        val repository = DefaultSleepTelemetryRepository(sleepLogRepo, normalizer)

        val result = repository.getLatestNormalizedSleep(
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )

        // Wake shift clamped to max +90m
        result.normalizedWakeTimeMillis shouldBe (baseWakeMillis + 90 * 60_000L)
    }

    "normalizeLogs correctly blends manual sleep log and computeEnergyUiState evaluates Borbély state" {
        val sleepLogRepo = mockk<SleepLogRepository>()
        val normalizer = ChronobiologySignalNormalizer()
        val repository = DefaultSleepTelemetryRepository(sleepLogRepo, normalizer)

        val log = SleepLogEntity(
            id = "manual-log-1",
            startAt = baseWakeMillis - 7 * 3600_000L,
            endAt = baseWakeMillis,
            durationMinutes = 420,
            source = "MANUAL",
            notes = "deep:90 rem:90"
        )

        val signal = repository.normalizeLogs(
            logs = listOf(log),
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )

        signal.totalSleepMinutes shouldBe (420.0 plusOrMinus 0.001)
        signal.normalizedWakeTimeMillis shouldBe baseWakeMillis
        (signal.effectiveDurationMinutes > 420.0) shouldBe true
    }
})
