package com.neuroflow.app.presentation.v2.flow

import com.neuroflow.app.data.local.entity.TaskEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimelineTaskCardVisualPolishTest {

    @Test
    fun testParseSplitProgress_fromTitleParentheses() {
        val task1 = TaskEntity(id = "task1", title = "Write Document (Part 1/3)")
        val progress1 = parseSplitProgress(task1)
        assertNotNull(progress1)
        assertEquals(1, progress1!!.partNumber)
        assertEquals(3, progress1.totalParts)
        assertEquals(1f / 3f, progress1.progress, 0.001f)

        val task2 = TaskEntity(id = "task2", title = "Refactor Database (Part 2/4)")
        val progress2 = parseSplitProgress(task2)
        assertNotNull(progress2)
        assertEquals(2, progress2!!.partNumber)
        assertEquals(4, progress2.totalParts)
        assertEquals(0.5f, progress2.progress, 0.001f)
    }

    @Test
    fun testParseSplitProgress_fromIdHashPart() {
        val task1 = TaskEntity(id = "task_doc#part1", title = "Write Document")
        val progress1 = parseSplitProgress(task1)
        assertNotNull(progress1)
        assertEquals(1, progress1!!.partNumber)
        assertEquals(2, progress1.totalParts)
        assertEquals(0.5f, progress1.progress, 0.001f)

        val task2 = TaskEntity(id = "task_doc#part2", title = "Write Document")
        val progress2 = parseSplitProgress(task2)
        assertNotNull(progress2)
        assertEquals(2, progress2!!.partNumber)
        assertEquals(2, progress2.totalParts)
        assertEquals(1.0f, progress2.progress, 0.001f)
    }

    @Test
    fun testParseSplitProgress_returnsNullForNonSplitTask() {
        val task = TaskEntity(id = "task_normal", title = "Clean Desk")
        assertNull(parseSplitProgress(task))
    }

    @Test
    fun testFormatDurationChip_formatsCorrectly() {
        assertEquals("0m", formatDurationChip(0))
        assertEquals("15m", formatDurationChip(15))
        assertEquals("45m", formatDurationChip(45))
        assertEquals("1h", formatDurationChip(60))
        assertEquals("1h 15m", formatDurationChip(75))
        assertEquals("1h 30m", formatDurationChip(90))
        assertEquals("2h", formatDurationChip(120))
        assertEquals("2h 25m", formatDurationChip(145))
    }

    @Test
    fun testFormatTimeRange_sameAmPm() {
        val zoneId = ZoneId.of("UTC")
        val startMillis = LocalDateTime.of(2026, 11, 15, 9, 30).atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = LocalDateTime.of(2026, 11, 15, 10, 15).atZone(zoneId).toInstant().toEpochMilli()

        val formatted = formatTimeRange(startMillis, endMillis, zoneId)
        assertEquals("09:30 – 10:15 AM", formatted)
    }

    @Test
    fun testFormatTimeRange_crossingAmPm() {
        val zoneId = ZoneId.of("UTC")
        val startMillis = LocalDateTime.of(2026, 11, 15, 11, 30).atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = LocalDateTime.of(2026, 11, 15, 12, 15).atZone(zoneId).toInstant().toEpochMilli()

        val formatted = formatTimeRange(startMillis, endMillis, zoneId)
        assertEquals("11:30 AM – 12:15 PM", formatted)
    }

    @Test
    fun testFormatTimeRange_withDurationMinutes() {
        val zoneId = ZoneId.of("UTC")
        val startMillis = LocalDateTime.of(2026, 11, 15, 14, 0).atZone(zoneId).toInstant().toEpochMilli()

        val formatted = formatTimeRange(startMillis, 45, zoneId)
        assertEquals("02:00 – 02:45 PM", formatted)
    }
}
