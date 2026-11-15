package com.neuroflow.app.presentation.v2.flow

import com.neuroflow.app.data.local.entity.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data model representing split-task progress information.
 */
data class SplitTaskProgress(
    val partNumber: Int,
    val totalParts: Int
) {
    val progress: Float
        get() = if (totalParts > 0) (partNumber.toFloat() / totalParts.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Parses split-task progress from title and/or id.
 * Detects patterns like:
 * - Title: "(Part 1/3)" -> partNumber = 1, totalParts = 3
 * - Id/Title: "#part1" or "_part1" -> partNumber = 1, totalParts = maxOf(2, partNumber)
 */
fun parseSplitProgress(title: String, id: String = ""): SplitTaskProgress? {
    // 1. Check title for (Part X/Y)
    val titleMatch = Regex("""\(Part (\d+)/(\d+)\)""", RegexOption.IGNORE_CASE).find(title)
    if (titleMatch != null) {
        val partNum = titleMatch.groupValues[1].toIntOrNull() ?: 1
        val totalParts = titleMatch.groupValues[2].toIntOrNull() ?: 2
        return SplitTaskProgress(partNum, totalParts)
    }

    // 2. Check id or title for #part(\d+) or _part(\d+)
    val partMatch = Regex("""[#_]part(\d+)""", RegexOption.IGNORE_CASE).find(id)
        ?: Regex("""#part(\d+)""", RegexOption.IGNORE_CASE).find(title)

    if (partMatch != null) {
        val partNum = partMatch.groupValues[1].toIntOrNull() ?: 1
        val totalParts = maxOf(2, partNum)
        return SplitTaskProgress(partNum, totalParts)
    }

    return null
}

/**
 * Convenience extension for TaskEntity.
 */
fun parseSplitProgress(task: TaskEntity): SplitTaskProgress? = parseSplitProgress(task.title, task.id)

/**
 * Formats duration in minutes into a concise badge string (e.g. "45m", "1h", "1h 30m").
 */
fun formatDurationChip(minutes: Int): String {
    if (minutes <= 0) return "0m"
    return if (minutes < 60) {
        "${minutes}m"
    } else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if (remainingMinutes == 0) {
            "${hours}h"
        } else {
            "${hours}h ${remainingMinutes}m"
        }
    }
}

/**
 * Formats start and end epoch timestamps into a time range chip (e.g. "09:30 – 10:15 AM").
 */
fun formatTimeRange(startMillis: Long, endMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val startZdt = Instant.ofEpochMilli(startMillis).atZone(zoneId)
    val endZdt = Instant.ofEpochMilli(endMillis).atZone(zoneId)

    val startHourMin = startZdt.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    val endHourMinAmPm = endZdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
    val startAmPm = startZdt.format(DateTimeFormatter.ofPattern("a", Locale.US))
    val endAmPm = endZdt.format(DateTimeFormatter.ofPattern("a", Locale.US))

    return if (startAmPm.equals(endAmPm, ignoreCase = true)) {
        "$startHourMin – $endHourMinAmPm"
    } else {
        val startFull = startZdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
        "$startFull – $endHourMinAmPm"
    }
}

/**
 * Overload computing endMillis from startMillis and duration in minutes.
 */
fun formatTimeRange(startMillis: Long, durationMinutes: Int, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val endMillis = startMillis + durationMinutes * 60_000L
    return formatTimeRange(startMillis, endMillis, zoneId)
}
