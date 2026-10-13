package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * How much clock time a scheduled task owns.
 *
 * The planner blocks a slot with the raw estimate inflated by the user's learned planning bias and
 * the slot's energy zone, then records that length on the task. Anything downstream that asks
 * "did this task get its time?" or "is this hour already taken?" reads the recorded block, so the
 * answer matches what was actually reserved instead of a second, cheaper guess at it.
 */
object TaskBlockPlan {

    private const val MAX_MINUTES = 360

    fun plannedMinutes(task: TaskEntity): Int {
        val recorded = task.plannedBlockMinutes
        if (recorded > 0) return recorded.coerceAtMost(MAX_MINUTES)
        return expectedMinutes(task)
    }

    /** The planner's own starting figure, before bias and energy inflate it. */
    fun expectedMinutes(task: TaskEntity): Int =
        if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else defaultMinutes(task)

    /** What a task of this effort is worth when the user never gave it a length. */
    fun defaultMinutes(task: TaskEntity): Int = when {
        task.effortScore >= 80 -> 60
        task.effortScore >= 60 -> 45
        else -> 30
    }

    fun scheduledEndMillis(startMillis: Long, task: TaskEntity): Long =
        startMillis + plannedMinutes(task) * 60_000L

    /**
     * True once the task's reserved block is over and the grace period has run out. Comparing
     * against the raw estimate instead would call a task missed while it still has time on the clock.
     */
    fun blockHasExpired(
        task: TaskEntity,
        startMillis: Long,
        nowMillis: Long,
        gracePeriodMillis: Long
    ): Boolean = nowMillis > scheduledEndMillis(startMillis, task) + gracePeriodMillis
}
