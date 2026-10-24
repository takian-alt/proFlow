package com.neuroflow.app.domain.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class ChronobiologySignalNormalizerTest : StringSpec({

    val normalizer = ChronobiologySignalNormalizer()
    val baseMillis = 1770000000000L

    "coalesceWasoEpisodes merges intervals with wake gaps <= 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L) // 3h sleep
        val sleep2 = TimeSlotWindow(baseMillis + 205 * 60_000L, baseMillis + 360 * 60_000L) // 25m wake gap (WASO)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 1
        sessions[0].startMillis shouldBe baseMillis
        sessions[0].endMillis shouldBe baseMillis + 360 * 60_000L
        wasoMinutes shouldBe 25L
    }

    "coalesceWasoEpisodes keeps separate sessions when wake gap > 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 240 * 60_000L, baseMillis + 360 * 60_000L) // 60m wake gap (> 45m)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 2
        sessions[0].endMillis shouldBe baseMillis + 180 * 60_000L
        sessions[1].startMillis shouldBe baseMillis + 240 * 60_000L
        wasoMinutes shouldBe 0L
    }

    "coalesceWasoEpisodes handles empty and overlapping inputs" {
        val (emptySessions, emptyWaso) = normalizer.coalesceWasoEpisodes(emptyList())
        emptySessions.shouldBeEmpty()
        emptyWaso shouldBe 0L

        val overlapping = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 120 * 60_000L),
            TimeSlotWindow(baseMillis + 60 * 60_000L, baseMillis + 240 * 60_000L)
        )
        val (ovSessions, ovWaso) = normalizer.coalesceWasoEpisodes(overlapping)
        ovSessions shouldHaveSize 1
        ovSessions[0].endMillis shouldBe baseMillis + 240 * 60_000L
        ovWaso shouldBe 0L
    }

    "computeEffectiveDuration weights restorative sleep with clamp [0.75, 1.25]" {
        // Zero restorative sleep (deep + rem = 0): factor = 0.85
        val poor = normalizer.computeEffectiveDuration(480.0, 0.0, 0.0)
        poor shouldBe (480.0 * 0.85 plusOrMinus 0.001)

        // Balanced sleep: 20% deep, 20% rem (40% total): factor = 0.85 + 0.75 * 0.40 = 1.15
        val healthy = normalizer.computeEffectiveDuration(480.0, 96.0, 96.0)
        healthy shouldBe (480.0 * 1.15 plusOrMinus 0.001)

        // Extremely high restorative sleep (75% total): raw factor = 0.85 + 0.75 * 0.75 = 1.4125 -> clamped to 1.25
        val superRestorative = normalizer.computeEffectiveDuration(480.0, 180.0, 180.0)
        superRestorative shouldBe (480.0 * 1.25 plusOrMinus 0.001)

        // Zero total minutes returns 0.0
        normalizer.computeEffectiveDuration(0.0, 0.0, 0.0) shouldBe 0.0
    }

    "blendDuration applies Bayesian confidence weighting smoothly" {
        val dEff = 500.0
        val dBase = 420.0

        // Confidence 1.0 -> d_eff
        normalizer.blendDuration(dEff, dBase, 1.0) shouldBe (500.0 plusOrMinus 0.001)

        // Confidence 0.0 -> d_base
        normalizer.blendDuration(dEff, dBase, 0.0) shouldBe (420.0 plusOrMinus 0.001)

        // Confidence 0.5 -> exact midpoint
        normalizer.blendDuration(dEff, dBase, 0.5) shouldBe (460.0 plusOrMinus 0.001)

        // Out of bounds confidence clamped
        normalizer.blendDuration(dEff, dBase, 1.5) shouldBe (500.0 plusOrMinus 0.001)
        normalizer.blendDuration(dEff, dBase, -0.2) shouldBe (420.0 plusOrMinus 0.001)
    }

    "clampWakeShift enforces +-90m anti-whiplash safety window" {
        val baseWake = 1000000000L
        val min90 = 90 * 60_000L

        // Small shift (30m later) with c = 1.0 -> exactly +30m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 1.0) shouldBe (baseWake + 30 * 60_000L)

        // Small shift (30m later) with c = 0.5 -> +15m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 0.5) shouldBe (baseWake + 15 * 60_000L)

        // Extreme late shift (180m later) with c = 1.0 -> clamped to +90m
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 1.0) shouldBe (baseWake + min90)

        // Extreme early shift (200m earlier) with c = 1.0 -> clamped to -90m
        normalizer.clampWakeShift(baseWake, baseWake - 200 * 60_000L, 1.0) shouldBe (baseWake - min90)

        // Zero confidence -> strictly unchanged
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 0.0) shouldBe baseWake
    }

    "normalize executes full pipeline correctly" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 195 * 60_000L, baseMillis + 480 * 60_000L) // 15m WASO
        val telemetry = RawSleepTelemetry(
            intervals = listOf(sleep1, sleep2),
            deepSleepMinutes = 90.0,
            remSleepMinutes = 90.0,
            confidence = 0.8
        )

        val baseWake = baseMillis + 480 * 60_000L
        val baseSleep = 460.0

        val signal = normalizer.normalize(telemetry, baseWake, baseSleep)
        signal.totalWasoMinutes shouldBe 15.0
        signal.consolidatedSessions shouldHaveSize 1
        signal.effectiveDurationMinutes shouldBe ((480.0 - 15.0) * (0.85 + 0.75 * (180.0 / 465.0)) plusOrMinus 1.0)
    }

    "extractDaytimeNaps filters valid 10-180m bouts after morning wake" {
        val morningWake = baseMillis + 480 * 60_000L
        val microNap = TimeSlotWindow(morningWake + 60 * 60_000L, morningWake + 65 * 60_000L) // 5m (< 10m, invalid)
        val powerNap = TimeSlotWindow(morningWake + 120 * 60_000L, morningWake + 140 * 60_000L) // 20m (valid)
        val longSleep = TimeSlotWindow(morningWake + 200 * 60_000L, morningWake + 400 * 60_000L) // 200m (> 180m, invalid)

        val naps = normalizer.extractDaytimeNaps(listOf(microNap, powerNap, longSleep), morningWake)
        naps shouldHaveSize 1
        naps[0].durationMinutes shouldBe 20
        naps[0].startMillis shouldBe (morningWake + 120 * 60_000L)
    }

    "normalize extracts secondary daytime nap without corrupting morning wake anchor" {
        val nocturnalSleep = TimeSlotWindow(baseMillis, baseMillis + 480 * 60_000L) // 8h sleep
        val daytimeNap = TimeSlotWindow(baseMillis + 840 * 60_000L, baseMillis + 870 * 60_000L) // 30m nap at 14:00

        val baseWake = baseMillis + 480 * 60_000L
        val telemetry = RawSleepTelemetry(
            intervals = listOf(nocturnalSleep, daytimeNap),
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseWake, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseWake // Wake anchor NOT corrupted to 14:30
        signal.totalSleepMinutes shouldBe 480.0 // Nap not counted as nocturnal sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }

    "normalize with ONLY daytime nap preserves baseWakeMillis and does not shift morning wake" {
        val daytimeNap = TimeSlotWindow(baseMillis + 420 * 60_000L, baseMillis + 450 * 60_000L) // 30m nap at 14:00
        val telemetry = RawSleepTelemetry(
            intervals = listOf(daytimeNap),
            detectedWakeMillis = daytimeNap.endMillis,
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseMillis, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseMillis // Strictly preserved, NOT shifted by +90m
        signal.totalSleepMinutes shouldBe 480.0 // Falls back to baseline sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }
})
