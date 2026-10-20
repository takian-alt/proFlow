package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

fun formatRelativeTime(millis: Long, hasTime: Boolean, now: Long = System.currentTimeMillis()): String {
    if (!hasTime) {
        val zone = ZoneId.systemDefault()
        val nowDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val targetDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val diffDays = ChronoUnit.DAYS.between(nowDate, targetDate).toInt()
        return when {
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Tomorrow"
            diffDays == -1 -> "Yesterday"
            diffDays > 1 -> "In $diffDays days"
            else -> "Overdue by ${kotlin.math.abs(diffDays)} days"
        }
    }

    val diff = millis - now
    val absDiff = kotlin.math.abs(diff)

    if (absDiff < 60_000L) {
        return if (diff < 0) "Overdue" else "Right now"
    }

    val totalMinutes = absDiff / 60_000
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60

    if (diff < 0) {
        val parts = mutableListOf<String>()
        if (days > 0) parts += "$days day${if (days > 1) "s" else ""}"
        if (hours > 0) parts += "$hours hr${if (hours > 1) "s" else ""}"
        if (minutes > 0) parts += "$minutes min${if (minutes > 1) "s" else ""}"
        val timeStr = parts.joinToString(" ").ifBlank { "now" }
        return "Overdue by $timeStr"
    }

    val parts = mutableListOf<String>()
    if (days > 0) parts += "$days day${if (days > 1) "s" else ""}"
    if (hours > 0) parts += "$hours hr${if (hours > 1) "s" else ""}"
    if (minutes > 0 && days == 0L) parts += "$minutes min${if (minutes > 1) "s" else ""}"

    val timeStr = parts.joinToString(" ").ifBlank { "now" }
    return when {
        timeStr == "now" -> "Right now"
        else -> "In $timeStr"
    }
}

fun formatFullDate(millis: Long, hasTime: Boolean): String {
    val formatStr = if (hasTime) "MMM d, h:mm a" else "MMM d, yyyy"
    val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
    return sdf.format(Date(millis))
}

/**
 * A live session outranks the calendar: the block is happening, so the card must stop announcing
 * when it was going to start.
 */
fun timeBlockLabel(isRecurring: Boolean, isTracking: Boolean): String = when {
    isTracking -> "🎯 FOCUSING NOW"
    isRecurring -> "🔁 RECURRENCE TIME"
    else -> "📅 SCHEDULED"
}

fun timeBlockHeadline(
    anchorMillis: Long,
    hasTime: Boolean,
    isTracking: Boolean,
    now: Long = System.currentTimeMillis()
): String = if (isTracking) "Focusing now" else formatRelativeTime(anchorMillis, hasTime, now)

@Composable
fun TagChip(tag: String) {
    val chipColors = listOf(
        Color(0xFF1C1F28),
        Color(0xFF222736),
        Color(0xFF2A2E3B),
        Color(0xFF1F232E),
        Color(0xFF252936),
        Color(0xFF181B22)
    )
    val colorIndex = tag.hashCode().let { kotlin.math.abs(it) } % chipColors.size
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipColors[colorIndex],
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

fun formatDurationFloat(minutes: Float): String {
    val totalMinutes = minutes.toInt()
    return when {
        totalMinutes < 60 -> "${String.format(Locale.getDefault(), "%.1f", minutes)} min"
        else -> {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            "${h}h ${m}m"
        }
    }
}
