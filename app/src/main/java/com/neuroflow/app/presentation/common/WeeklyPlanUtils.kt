package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduleAnchorMillis
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import java.util.Calendar

fun getWeekDays(referenceMillis: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    }

    val days = mutableListOf<Long>()
    for (i in 0 until 7) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return days
}

fun isSameCalendarDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun isTaskOnDay(task: TaskEntity, dayStart: Long, dayEnd: Long): Boolean {
    val anchor = task.effectiveScheduleAnchorMillis()
    if (anchor != null && anchor in dayStart until dayEnd) return true
    val deadline = task.deadlineDate
    if (anchor == null && deadline != null && deadline in dayStart until dayEnd) return true
    return false
}

fun isProtectedSlotOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
    return slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, dayMillis)
}
