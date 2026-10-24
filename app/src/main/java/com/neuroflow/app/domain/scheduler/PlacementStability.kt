package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority

/**
 * Whether an already-placed block is worth moving, and whether a new placement is worth writing.
 *
 * The scheduler wakes up every few minutes, so a decision that only asks "where is best right now"
 * keeps rewriting the same afternoon. This object makes the question "is the difference big enough
 * to disturb the user for" — the answers are ordered so stability checks run before optimisation.
 */
object PlacementStability {

    /** A block this close to starting is the user's current plan, not the scheduler's draft. */
    private const val NEAR_TERM_FREEZE_MILLIS = 2 * 60 * 60 * 1000L

    /** Smallest shift that justifies rewriting a placement the user can already see. */
    private const val MIN_MOVE_MILLIS = 30 * 60 * 1000L

    enum class ReplanVerdict(val allowsReplan: Boolean) {
        NEAR_TERM_FREEZE(false),
        COOLDOWN(false),
        KEEP(false),
        FORCED(true),
        DEADLINE_RESCUE(true),
        POSTPONE_PATTERN(true),
        FAR_FUTURE_PULL_IN(true),
        PERIODIC_REFRESH(true),
        PAST_EXPIRED_RESCUE(true)
    }

    /**
     * Ordered so that the two checks the user can feel — an imminent block and a recently written
     * one — run before any optimisation rule, including the manual trigger.
     */
    fun verdict(
        task: TaskEntity,
        nowMillis: Long,
        prefs: UserPreferences,
        forceReplan: Boolean
    ): ReplanVerdict {
        if (task.isScheduleLocked) return ReplanVerdict.KEEP

        val scheduledMillis = task.resolveScheduledEpochMillis()
            ?: return ReplanVerdict.KEEP
        val hoursUntilScheduled = (scheduledMillis - nowMillis) / 3_600_000f

        // Bug 2 & Feature 3 (ADR-026): Expired-Block Auto-Rescue
        // If a task's reserved block has ended and the 15-minute grace period has passed,
        // it is eligible for same-day rescue rescheduling rather than stranded permanently in the past.
        if (TaskBlockPlan.blockHasExpired(task, scheduledMillis, nowMillis, gracePeriodMillis = 15 * 60_000L)) {
            return ReplanVerdict.PAST_EXPIRED_RESCUE
        }

        if (scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE

        if (forceReplan) return ReplanVerdict.FORCED

        val deadlineMillis = task.resolveDeadlineEpochMillis()

        val taskAge = nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt)
        val isUrgentDeadline = deadlineMillis?.let { (it - nowMillis) < 6 * 60 * 60 * 1000L } ?: false
        val cooldownMillis = when {
            isUrgentDeadline -> 60 * 60 * 1000L
            prefs.isAutoScheduleToggleOn -> 4 * 60 * 60 * 1000L
            else -> 6 * 60 * 60 * 1000L
        }
        if (taskAge <= cooldownMillis) return ReplanVerdict.COOLDOWN

        if (deadlineMillis != null) {
            val hoursUntilDeadline = (deadlineMillis - nowMillis) / 3_600_000f
            if (hoursUntilDeadline < 6f && hoursUntilScheduled > hoursUntilDeadline * 0.5f) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
            if (hoursUntilDeadline < 12f && task.priority == Priority.LOW) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
        }

        if (task.postponeCount >= 3) return ReplanVerdict.POSTPONE_PATTERN

        if (hoursUntilScheduled > 24f && !task.isScheduleLocked) {
            val isImportant = task.priority == Priority.HIGH || task.impactScore >= 80 || task.isFrog
            if (isImportant) return ReplanVerdict.FAR_FUTURE_PULL_IN
        }

        if (prefs.autoSchedulingEnabled) {
            val shouldRefresh = if (prefs.isAutoScheduleToggleOn) {
                true
            } else {
                val cycleNumber = (nowMillis / (6 * 60 * 60 * 1000L)) % 5
                val taskBucket = (task.id.hashCode().toLong() and 0x7FFFFFFF) % 5
                cycleNumber == taskBucket
            }
            if (shouldRefresh) return ReplanVerdict.PERIODIC_REFRESH
        }

        return ReplanVerdict.KEEP
    }

    /**
     * A write is only worth it when the user sees a different plan afterwards. An identical start
     * touches the database and the update timestamp for nothing, and a future block that shifts by
     * minutes is churn dressed as an improvement — unless the old slot is already behind us, in
     * which case staying put would leave the task stranded in the past.
     */
    fun isMoveWorthApplying(
        previousStartMillis: Long?,
        nextStartMillis: Long,
        nowMillis: Long
    ): Boolean {
        val previous = previousStartMillis ?: return true
        if (nextStartMillis == previous) return false
        if (previous <= nowMillis) return true
        return kotlin.math.abs(nextStartMillis - previous) >= MIN_MOVE_MILLIS
    }
}
