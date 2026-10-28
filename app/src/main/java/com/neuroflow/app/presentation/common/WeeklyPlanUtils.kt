package com.neuroflow.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduleAnchorMillis
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
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
    val sDate = task.effectiveScheduledDateMillis()
    if (sDate != null) {
        return sDate in dayStart until dayEnd
    }
    val rawScheduledTime = task.scheduledTime
    if (rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS) {
        return rawScheduledTime in dayStart until dayEnd
    }
    val deadline = task.resolveDeadlineEpochMillis()
    if (deadline != null) {
        return deadline in dayStart until dayEnd
    }
    if (rawScheduledTime != null && rawScheduledTime in 0 until ONE_DAY_MILLIS) {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        if (todayStart in dayStart until dayEnd) return true
    }
    return false
}

fun isProtectedSlotOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
    return slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, dayMillis)
}

@Composable
fun getQuadrantBgColor(quadrant: Quadrant): Color {
    return when (quadrant) {
        Quadrant.DO_FIRST  -> NeuroFlowColors.DoFirstBg
        Quadrant.SCHEDULE  -> NeuroFlowColors.ScheduleBg
        Quadrant.DELEGATE  -> NeuroFlowColors.DelegateBg
        Quadrant.ELIMINATE -> NeuroFlowColors.EliminateBg
    }
}

@Composable
fun getQuadrantTextColor(quadrant: Quadrant): Color {
    return when (quadrant) {
        Quadrant.DO_FIRST  -> NeuroFlowColors.DoFirstText
        Quadrant.SCHEDULE  -> NeuroFlowColors.ScheduleText
        Quadrant.DELEGATE  -> NeuroFlowColors.DelegateText
        Quadrant.ELIMINATE -> NeuroFlowColors.EliminateText
    }
}

fun getQuadrantLabel(quadrant: Quadrant): String = when (quadrant) {
    Quadrant.DO_FIRST  -> "DO FIRST"
    Quadrant.SCHEDULE  -> "SCHEDULE"
    Quadrant.DELEGATE  -> "DELEGATE"
    Quadrant.ELIMINATE -> "ON HOLD"
}
