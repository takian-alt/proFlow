package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import java.util.Calendar

enum class SlotPhase {
    /** Inside the lead-in window but not started yet */
    LEAD_IN,

    /** The wall clock sits inside the slot */
    RUNNING,

    /** Just past the end, still worth showing as finishing */
    WRAPPING
}

data class DueSlot(
    val slot: ProtectedTimeSlot,
    val phase: SlotPhase,
    val startMillis: Long,
    val endMillis: Long,
    /** 0 unless the slot has not started yet */
    val minutesToStart: Int,
    /** Minutes until the slot ends; 0 once it is over */
    val minutesLeft: Int
)

/**
 * Answers "is one of the user's protected blocks happening right now, or about to?". One place
 * computes it so the Today hero, the launcher card and the Focus screen cannot disagree.
 *
 * A slot's own recovery buffer is deliberately not part of the takeover: the buffer exists to stop
 * the auto-scheduler from packing work immediately after exercise, not to keep a card on screen.
 */
object ProtectedSlotClock {

    const val DEFAULT_LEAD_IN_MINUTES = 120

    const val DEFAULT_WRAP_MINUTES = 10

    private const val MINUTE_MILLIS = 60_000L

    /** Which days a slot falls on. Ignores [ProtectedTimeSlot.isEnabled] — that is the caller's filter. */
    fun occursOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        return when (slot.recurrence) {
            PtsRecurrence.DAILY -> true
            PtsRecurrence.WEEKDAYS -> cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
            PtsRecurrence.WEEKENDS -> {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            }
            PtsRecurrence.CUSTOM -> {
                // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
                val bit = 1 shl (cal.get(Calendar.DAY_OF_WEEK) - 1)
                (slot.weekdayBitmask and bit) != 0
            }
            PtsRecurrence.SPECIFIC_DATE -> slot.dateMillis?.let {
                sameCalendarDay(it, dayMillis)
            } == true
        }
    }

    /**
     * Slots overlapping `now - wrapMinutes .. now + leadInMinutes`, best candidate first: one that
     * is running or just ended takes the top spot, most recent start winning, then the ones still
     * to come in start order.
     *
     * The lead-in edge is inclusive, matching how the Today screen already buckets tasks at
     * `now + 2h`; the wrap edge is exclusive so a slot is off screen at exactly ten minutes past.
     */
    fun dueSlots(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long,
        leadInMinutes: Int = DEFAULT_LEAD_IN_MINUTES,
        wrapMinutes: Int = DEFAULT_WRAP_MINUTES
    ): List<DueSlot> {
        val windowStart = nowMillis - wrapMinutes * MINUTE_MILLIS
        val windowEnd = nowMillis + leadInMinutes * MINUTE_MILLIS

        return slots
            .filter { it.isEnabled }
            .flatMap { slot -> occurrences(slot, nowMillis).map { slot to it } }
            .filter { (_, span) -> span.endMillis > windowStart && span.startMillis <= windowEnd }
            .mapNotNull { (slot, span) -> dueSlotOf(slot, span, nowMillis, wrapMinutes) }
            .sortedWith(SlotOrder)
    }

    /** The slot that should hold the hero position, or null while every slot is still ahead. */
    fun takeoverOf(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long
    ): DueSlot? = dueSlots(slots, nowMillis).firstOrNull { it.phase != SlotPhase.LEAD_IN }

    /** The slots starting within the lead-in window, soonest first — the "Up Next" queue. */
    fun approaching(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long
    ): List<DueSlot> = dueSlots(slots, nowMillis).filter { it.phase == SlotPhase.LEAD_IN }

    private data class Span(val startMillis: Long, val endMillis: Long)

    /**
     * Yesterday is checked as well, because a block that began before midnight must still read as
     * running at 00:05. Tomorrow covers a lead-in that starts just after midnight.
     */
    private fun occurrences(slot: ProtectedTimeSlot, nowMillis: Long): List<Span> {
        val todaysStart = startOfDay(nowMillis)
        return (-1..1).mapNotNull { offset ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = todaysStart
                add(Calendar.DAY_OF_YEAR, offset)
            }.timeInMillis
            if (!occursOnDay(slot, dayStart)) return@mapNotNull null
            val start = dayStart + slot.startMinuteOfDay * MINUTE_MILLIS
            Span(start, start + slot.durationMinutes * MINUTE_MILLIS)
        }
    }

    private fun dueSlotOf(
        slot: ProtectedTimeSlot,
        span: Span,
        nowMillis: Long,
        wrapMinutes: Int
    ): DueSlot? = when {
        nowMillis < span.startMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.LEAD_IN,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = minutesUp(span.startMillis - nowMillis),
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.RUNNING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis + wrapMinutes * MINUTE_MILLIS -> DueSlot(
            slot = slot,
            phase = SlotPhase.WRAPPING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = 0
        )

        else -> null
    }

    private fun minutesUp(millis: Long): Int =
        ((millis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).coerceAtLeast(0L).toInt()

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sameCalendarDay(a: Long, b: Long): Boolean {
        val left = Calendar.getInstance().apply { timeInMillis = a }
        val right = Calendar.getInstance().apply { timeInMillis = b }
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    private object SlotOrder : Comparator<DueSlot> {
        override fun compare(a: DueSlot, b: DueSlot): Int {
            val groupA = if (a.phase == SlotPhase.LEAD_IN) 1 else 0
            val groupB = if (b.phase == SlotPhase.LEAD_IN) 1 else 0
            if (groupA != groupB) return groupA - groupB
            return if (groupA == 0) b.startMillis.compareTo(a.startMillis)
            else a.startMillis.compareTo(b.startMillis)
        }
    }
}
