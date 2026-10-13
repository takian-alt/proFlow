package com.neuroflow.app.presentation.common

import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.SlotPhase

fun DueSlot.title(): String = slot.label

fun DueSlot.statusText(): String = when (phase) {
    SlotPhase.RUNNING -> "In progress"
    SlotPhase.WRAPPING -> "Winding down"
    SlotPhase.LEAD_IN -> "Coming up"
}

fun DueSlot.timeText(): String = when (phase) {
    SlotPhase.RUNNING -> "${spokenMinutes(minutesLeft)} left"
    SlotPhase.WRAPPING -> "Wrapping up"
    SlotPhase.LEAD_IN -> "Starts in ${spokenMinutes(minutesToStart)}"
}

/** Blocks are usually an hour or more, so "90 min" reads slower than "1 h 30 min". */
private fun spokenMinutes(minutes: Int): String =
    if (minutes < 60) "$minutes min"
    else "${minutes / 60} h".let { hours ->
        val rest = minutes % 60
        if (rest == 0) hours else "$hours $rest min"
    }
