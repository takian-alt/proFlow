package com.neuroflow.app.presentation.today

import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.domain.engine.DistractionEngine
import kotlin.math.roundToInt

/**
 * Sessions that started today, including the one still running. A block that opened just before
 * midnight stays yesterday's.
 */
internal fun todayFocusSessions(sessions: List<TimeSessionEntity>, now: Long): List<TimeSessionEntity> {
    val start = startOfLocalDay(now)
    val endExclusive = startOfNextLocalDay(start)
    return sessions.filter {
        it.startedAt in start until endExclusive && it.trackedMinutes(now) > 0f
    }
}

internal fun focusMinutesOf(sessions: List<TimeSessionEntity>, now: Long): Float =
    sessions.sumOf { it.trackedMinutes(now).toDouble() }.toFloat()

/** Closed intervals of today's focus blocks, for telling distraction inside and outside them apart. */
internal fun focusWindows(sessions: List<TimeSessionEntity>, now: Long): List<Pair<Long, Long>> =
    sessions.mapNotNull { session ->
        val end = minOf(session.endedAt ?: now, now)
        if (end > session.startedAt) session.startedAt to end else null
    }

internal fun formatDurationMinutes(minutes: Float): String {
    val total = minutes.roundToInt().coerceAtLeast(0)
    val hours = total / 60
    val mins = total % 60
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> "$hours h"
        else -> "$hours h $mins m"
    }
}

internal fun deepWorkProgress(doneMinutes: Float, targetMinutes: Int): Float =
    if (targetMinutes <= 0) 0f else (doneMinutes / targetMinutes).coerceIn(0f, 1f)

/**
 * The point of the card is a decision, not a number: is another deep block realistic,
 * or is today's deep work already paid for?
 */
internal fun deepWorkVerdict(doneMinutes: Float, targetMinutes: Int, hourOfDay: Int): String {
    val remaining = targetMinutes - doneMinutes
    return when {
        doneMinutes < 25f -> "No real deep block logged yet — one 25 minutes starts the count."
        remaining <= 0f -> "Target cleared. Anything more today is bonus, not budget."
        hourOfDay >= 19 && remaining < 60f ->
            "${formatDurationMinutes(remaining)} left and the day is nearly over — a short block still counts."
        hourOfDay >= 19 ->
            "${formatDurationMinutes(remaining)} still missing with little day left. Consider banking it to tomorrow."
        remaining >= 90f -> "${formatDurationMinutes(remaining)} of deep work still fits in today."
        else -> "${formatDurationMinutes(remaining)} between you and today's target."
    }
}

internal fun distractionVerdict(state: DailyDistractionState): String = when {
    !state.permissionGranted -> "Screen time is unreadable until usage access is granted."
    state.flaggedAppCount == 0 -> "Nothing is flagged, so distraction is not being measured at all."
    !state.hasEnoughSignal -> "Not enough deep work or flagged-app time tracked yet to judge today."
    else -> DistractionEngine.label(state.distractionScore) + when {
        state.distractionScore >= 75f -> " — distracting apps took almost all of your tracked attention."
        state.distractionScore >= 50f -> " — flagged apps outspent deep work today."
        state.distractionScore >= 25f -> " — deep work led, but the leaks were real."
        else -> " — distraction was a rounding error today."
    }
}

internal fun distractionAdvice(state: DailyDistractionState, deepWorkRemainingMinutes: Float): String = when {
    !state.permissionGranted -> "Grant usage access to let the app read today's screen time."
    state.flaggedAppCount == 0 -> "Flag the apps you lose time in and this starts reporting."
    !state.hasEnoughSignal -> "Log a focus session and this starts reporting."
    state.distractionScore >= 50f && deepWorkRemainingMinutes > 0f ->
        "Next block: run Hyper Focus so the flagged apps are actually blocked."
    state.distractionScore >= 25f && state.topApps.isNotEmpty() ->
        "${state.topApps.first().first} is your biggest single leak today."
    state.distractionScore >= 25f -> "Keep the phone out of the room for the next block."
    else -> "Nothing to fix — protect what is working."
}
