package com.neuroflow.app.data.calendar

import com.neuroflow.app.domain.service.TimeSlotWindow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CalendarSyncRepositoryTest : StringSpec({

    val baseMillis = 1770000000000L // arbitrary fixed epoch ms

    "coalesceWindows handles empty input" {
        val result = CalendarSyncDataSource.coalesceWindows(emptyList())
        result.shouldBeEmpty()
    }

    "coalesceWindows ignores invalid or zero-duration windows" {
        val invalid = listOf(
            TimeSlotWindow(baseMillis + 1000, baseMillis + 1000),
            TimeSlotWindow(baseMillis + 2000, baseMillis + 1000)
        )
        val result = CalendarSyncDataSource.coalesceWindows(invalid)
        result.shouldBeEmpty()
    }

    "coalesceWindows leaves short meetings (< 45m) unbuffered" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L) // 30 min meeting
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[0].durationMinutes shouldBe 30
    }

    "coalesceWindows adds 5m decompression buffer to meetings >= 45m" {
        val raw45 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 45 * 60_000L) // exactly 45 min
        )
        val result45 = CalendarSyncDataSource.coalesceWindows(raw45)
        result45 shouldHaveSize 1
        result45[0].startMillis shouldBe baseMillis
        result45[0].endMillis shouldBe baseMillis + 50 * 60_000L // 45 + 5m buffer
        result45[0].durationMinutes shouldBe 50

        val raw60 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 60 * 60_000L) // 60 min
        )
        val result60 = CalendarSyncDataSource.coalesceWindows(raw60)
        result60 shouldHaveSize 1
        result60[0].durationMinutes shouldBe 65
    }

    "coalesceWindows merges directly overlapping intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L),
            TimeSlotWindow(baseMillis + 15 * 60_000L, baseMillis + 60 * 60_000L)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged duration is 60m >= 45m -> gets +5m buffer
        result[0].endMillis shouldBe baseMillis + 65 * 60_000L
    }

    "coalesceWindows coalesces adjacent intervals separated by < 5m" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 25 * 60_000L),
            TimeSlotWindow(baseMillis + 28 * 60_000L, baseMillis + 55 * 60_000L) // 3 min gap (< 5m)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged span: [0..55m] = 55m >= 45m -> gets +5m buffer -> 60m total
        result[0].endMillis shouldBe baseMillis + 60 * 60_000L
    }

    "coalesceWindows preserves distinct separated intervals (gap >= 5m)" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L), // 30m (<45m, no buffer)
            TimeSlotWindow(baseMillis + 40 * 60_000L, baseMillis + 70 * 60_000L) // 10 min gap
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 2
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[1].startMillis shouldBe baseMillis + 40 * 60_000L
        result[1].endMillis shouldBe baseMillis + 70 * 60_000L
    }

    "coalesceWindows handles unordered input and nested intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis + 10 * 60_000L, baseMillis + 20 * 60_000L),
            TimeSlotWindow(baseMillis, baseMillis + 50 * 60_000L) // Outer containing window
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // 50 min >= 45m -> 55m with decompression buffer
        result[0].endMillis shouldBe baseMillis + 55 * 60_000L
    }
})
