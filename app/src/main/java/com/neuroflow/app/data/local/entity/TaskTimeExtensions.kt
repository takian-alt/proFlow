package com.neuroflow.app.data.local.entity

import com.neuroflow.app.domain.model.Recurrence
import java.util.Calendar

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Ids of the tasks this task depends on.
 */
fun TaskEntity.dependencyIds(): List<String> =
    dependsOnTaskIds.split(',').map { it.trim() }.filter { it.isNotBlank() }

/**
 * Recurring tasks should anchor on habitDate when present.
 */
fun TaskEntity.isRecurringWithAnchor(): Boolean =
    recurrence != Recurrence.NONE && habitDate != null

/**
 * Returns the effective date anchor (midnight start-of-day) for scheduling and filtering.
 * For recurring habits, this is [habitDate]; otherwise it is [scheduledDate].
 */
fun TaskEntity.effectiveScheduledDateMillis(): Long? =
    if (isRecurringWithAnchor()) habitDate else scheduledDate

const val ONE_DAY_MILLIS = 86_400_000L

/**
 * Resolves the scheduled epoch millis handling dual representation:
 * - scheduledTime as an intraday offset (< 86_400_000L)
 * - scheduledTime as an absolute epoch timestamp (>= 86_400_000L)
 */
fun TaskEntity.resolveScheduledEpochMillis(referenceDateMillis: Long = System.currentTimeMillis()): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: 0L
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    val rawTime = scheduledTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) {
        return rawTime
    }
    if (scheduledDate != null) {
        return scheduledDate + (rawTime ?: 0L)
    }
    if (rawTime != null && rawTime >= 0L) {
        return startOfLocalDay(referenceDateMillis) + rawTime
    }
    return habitDate?.let { startOfLocalDay(it) }
}

/**
 * Resolves the deadline epoch millis handling dual representation:
 * - deadlineTime as intraday offset vs absolute epoch millis
 * - anchors to deadlineDate, habitDate, scheduledDate, or referenceDateMillis when date is omitted
 */
fun TaskEntity.resolveDeadlineEpochMillis(referenceDateMillis: Long = System.currentTimeMillis()): Long? {
    val rawTime = deadlineTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) return rawTime
    if (deadlineDate != null) {
        return deadlineDate + (rawTime ?: 0L)
    }
    if (isRecurringWithAnchor() && habitDate != null && rawTime != null) {
        return startOfLocalDay(habitDate) + rawTime
    }
    if (scheduledDate != null && rawTime != null) {
        return scheduledDate + rawTime
    }
    if (rawTime != null && rawTime >= 0L) {
        return startOfLocalDay(referenceDateMillis) + rawTime
    }
    return null
}

/**
 * Normalizes an epoch timestamp into standard date (start of day) and time offset pair.
 */
fun buildNormalizedScheduleTimestamps(startEpochMillis: Long): Pair<Long, Long> {
    val dayStart = startOfLocalDay(startEpochMillis)
    val timeOffset = localTimeOfDayOffset(startEpochMillis)
    return dayStart to timeOffset
}

/**
 * Target used by reminder/notification flows.
 */
fun TaskEntity.effectiveReminderTargetMillis(): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: (9 * 3600_000L)
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    // Only return a target if there is an explicit timed component (deadlineTime or scheduledTime).
    // Untimed date-only tasks (scheduledTime == null && deadlineTime == null) resolve to midnight (00:00:00),
    // which would cause TaskReminderWorker to enqueue false late-night alerts at 11:45 PM the night before (BUG-157).
    if (deadlineTime != null) {
        val deadline = resolveDeadlineEpochMillis()
        if (deadline != null) return deadline
    }
    if (scheduledTime != null) {
        val scheduled = resolveScheduledEpochMillis()
        if (scheduled != null) return scheduled
    }
    return null
}

/**
 * Anchor used by schedule/time-block style flows.
 */
fun TaskEntity.effectiveScheduleAnchorMillis(): Long? {
    return resolveScheduledEpochMillis()
}

/**
 * Local minute-of-day for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val cal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    return (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
        .coerceIn(0, MINUTES_PER_DAY - 1)
}

/**
 * Local minute-of-day with explicit ZoneId for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(zoneId: java.time.ZoneId): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val zdt = java.time.Instant.ofEpochMilli(anchorMillis).atZone(zoneId)
    return (zdt.hour * 60 + zdt.minute).coerceIn(0, MINUTES_PER_DAY - 1)
}

fun startOfLocalDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun startOfNextLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

fun startOfPreviousLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

fun localTimeOfDayOffset(millis: Long): Long {
    val dayStart = startOfLocalDay(millis)
    return (millis - dayStart).coerceAtLeast(0L)
}

fun splitToLocalDateAndTime(millis: Long): Pair<Long, Long> {
    val date = startOfLocalDay(millis)
    val time = localTimeOfDayOffset(millis)
    return date to time
}

/**
 * Converts Material 3 DatePicker UTC epoch millis (00:00:00 UTC on picked day)
 * to local start-of-day millis in the system timezone (BUG-037).
 */
fun utcDateMillisToLocalStartOfDay(utcMillis: Long, zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
    val localDate = java.time.Instant.ofEpochMilli(utcMillis).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

/**
 * Converts local start-of-day millis to UTC start-of-day millis for initializing
 * Material 3 DatePicker state (BUG-037).
 */
fun localStartOfDayToUtcDateMillis(localMillis: Long, zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
    val localDate = java.time.Instant.ofEpochMilli(localMillis).atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
}
