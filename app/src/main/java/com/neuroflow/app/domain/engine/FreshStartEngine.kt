package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis

object FreshStartEngine {
    private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

    fun isFreshStart(
        nowMillis: Long,
        lastOpenMillis: Long,
        dailyStreak: Int,
        lastActiveDate: Long,
        lastFreshStartShownWeek: Int,
        lastFreshStartShownYear: Int
    ): Boolean {
        // Guard: already shown this Sunday-to-Saturday schedule week
        val anchorMillis = sundayWeekAnchorMillis(nowMillis)
        if (isoWeekNumber(anchorMillis) == lastFreshStartShownWeek &&
            isoYear(anchorMillis) == lastFreshStartShownYear) return false

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)

        // Monday (first day of ISO week)
        val isMonday = dayOfWeek == java.util.Calendar.MONDAY
        // First day of month
        val isFirstOfMonth = dayOfMonth == 1
        // Day after streak break (had a streak, now it's 0)
        val isAfterStreakBreak = dailyStreak == 0 && lastActiveDate > 0L
        // 3+ day absence
        val isLongAbsence = lastOpenMillis > 0L && (nowMillis - lastOpenMillis) >= THREE_DAYS_MS

        return isMonday || isFirstOfMonth || isAfterStreakBreak || isLongAbsence
    }

    /**
     * In Sunday-to-Saturday schedules, mid-week Thursday canonically anchors the ISO-8601
     * week number so all days from Sunday through Saturday resolve to the identical ISO week.
     */
    fun sundayWeekAnchorMillis(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            val currentDayOfWeek = get(java.util.Calendar.DAY_OF_WEEK)
            val daysFromSunday = currentDayOfWeek - java.util.Calendar.SUNDAY
            add(java.util.Calendar.DAY_OF_YEAR, -daysFromSunday + 4) // Thursday
        }
        return cal.timeInMillis
    }

    fun isoWeekNumber(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    }

    fun isoYear(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
    }

    data class EmergencyResetPlan(
        val tasksToReschedule: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val tasksPushedToBacklog: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val streakFreezeApplied: Boolean,
        val summaryMessage: String
    )

    /**
     * Instant Frictionless One-Tap Recovery Algorithm:
     * Reshuffles remaining tasks without punishing streaks or clogging the calendar.
     *
     * 1. Protects Streaks: Preserves current streak state or applies a streak-freeze safety buffer.
     * 2. Clears Calendar Overpack: Un-schedules overdue/missed tasks for today, pushing low-impact/non-urgent tasks to Backlog.
     * 3. Keeps Only High-Impact frog/urgent tasks for immediate action today.
     */
    fun buildEmergencyResetPlan(
        activeTasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        nowMillis: Long
    ): EmergencyResetPlan {
        val todayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Find active tasks scheduled today or in the past, or overdue by deadline/postpone/habit
        val scheduledOverdueOrToday = activeTasks.filter { task ->
            if (task.isScheduleLocked) return@filter false
            val isScheduledTodayOrPast = task.scheduledDate != null && task.scheduledDate!! <= todayStart
            val isTimedTodayOrPast = task.resolveScheduledEpochMillis()?.let { it <= nowMillis } == true
            val isOverdueDeadline = task.deadlineDate != null && task.resolveDeadlineEpochMillis()?.let { it <= nowMillis } == true
            val isChronicPostpone = task.postponeCount >= 2
            val isHabitTodayOrPast = task.habitDate != null && task.habitDate!! <= todayStart
            isScheduledTodayOrPast || isTimedTodayOrPast || isOverdueDeadline || isChronicPostpone || isHabitTodayOrPast
        }

        // Partition by urgency: High-impact (isFrog, impactScore >= 70, or urgent deadline) vs low-impact
        val (highPriority, lowPriority) = scheduledOverdueOrToday.partition { task ->
            val resolvedDeadline = task.resolveDeadlineEpochMillis()
            task.isFrog || task.impactScore >= 70 ||
                (resolvedDeadline != null && (resolvedDeadline - nowMillis) <= 24 * 60 * 60 * 1000L)
        }

        // Keep top 2 high-priority tasks for today, push the rest to backlog (clear scheduleDate/Time)
        val keepForToday = highPriority.take(2)
        val pushToBacklog = lowPriority + highPriority.drop(2)

        return EmergencyResetPlan(
            tasksToReschedule = keepForToday,
            tasksPushedToBacklog = pushToBacklog,
            streakFreezeApplied = true,
            summaryMessage = "Schedule reset! ${keepForToday.size} essential tasks kept for today, ${pushToBacklog.size} moved to backlog. Streak protected!"
        )
    }
}
