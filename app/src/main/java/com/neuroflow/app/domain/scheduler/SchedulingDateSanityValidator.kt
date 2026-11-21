package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.service.SchedulableTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reason why a task was rejected by [SchedulingDateSanityValidator] for a target scheduling day.
 */
enum class DateIneligibilityReason {
    /** Task's scheduledDate (or effective date anchor) is strictly after the target scheduling date. */
    FUTURE_SCHEDULED_DATE,
    /** Task's absolute scheduled timestamp falls strictly after the target scheduling date. */
    FUTURE_SCHEDULED_TIMESTAMP,
    /** Task's habitDate anchor falls strictly after the target scheduling date. */
    FUTURE_HABIT_DATE,
    /** SchedulableTask's preferredDayStartMillis falls strictly after the target scheduling date. */
    FUTURE_PREFERRED_DAY,
    /** Unscheduled task has a deadline strictly in the future and future-deadlined backlog is disallowed. */
    FUTURE_ONLY_DEADLINE,
    /** When scheduling a non-today date, task is anchored to a different date. */
    OTHER_DAY_ANCHOR
}

/**
 * Diagnostic record for a task rejected by [SchedulingDateSanityValidator].
 */
data class RejectedTaskEntry(
    val taskId: String,
    val reason: DateIneligibilityReason,
    val explanation: String
)

data class EntityValidationResult(
    val eligibleTasks: List<TaskEntity>,
    val rejectedTasks: List<RejectedTaskEntry>
)

data class SchedulableValidationResult(
    val eligibleTasks: List<SchedulableTask>,
    val rejectedTasks: List<RejectedTaskEntry>
)

/**
 * Defense-in-depth date-eligibility sanity check validator for the task scheduler.
 *
 * Guarantees that future-dated tasks (tomorrow, next week, future auto-scheduled blocks,
 * future date-only tasks, future dual-epoch timestamps, and future recurring habits) are NEVER
 * automatically pulled backward onto Today (or another earlier date) when a Today task is deleted
 * or when single-day auto-scheduling/reconciliation runs.
 *
 * Enforced across 4 layers:
 * 1. Candidate selection (`ScheduleAutoTasksWorker.selectCandidateBuckets`, `PlanViewModelV2`, `DailyBlueprintOrchestrator`)
 * 2. Orchestration (`SchedulePreparationOrchestrator.orchestrateDayScheduleWithOutcome`)
 * 3. Core scheduler (`DefaultTaskSchedulerService.scheduleDayWithOutcome`)
 * 4. Write-time persistence gate (`canApplyPlacementToEntity` before `taskRepository.update`)
 */
object SchedulingDateSanityValidator {

    /**
     * Inspects a single [TaskEntity] for eligibility to be scheduled on [targetDate].
     *
     * @return null if the task is eligible, or the specific [DateIneligibilityReason] why it must be excluded.
     */
    fun inspectEntityForDay(
        task: TaskEntity,
        targetDate: LocalDate,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        allowFutureDeadlinedBacklog: Boolean = false,
        allowHardDeadlineRescue: Boolean = false
    ): DateIneligibilityReason? {
        val dayStart = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        val nowDate = epochToLocalDate(nowMillis, zoneId)
        val isTargetingToday = targetDate == nowDate

        val hasHardDeadlineOnOrBeforeTarget = allowHardDeadlineRescue &&
            hasDeadlineOnOrBefore(task, targetDate, dayStart, dayEnd, zoneId)

        // 1. Habit anchor date check
        task.habitDate?.let { habitMillis ->
            val habitLocalDate = epochToLocalDate(habitMillis, zoneId)
            if (habitLocalDate.isAfter(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.FUTURE_HABIT_DATE
            }
            if (!isTargetingToday && !habitLocalDate.isEqual(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.OTHER_DAY_ANCHOR
            }
        }

        // 2. Explicit scheduledDate check
        task.scheduledDate?.let { schedDateMillis ->
            val schedLocalDate = epochToLocalDate(schedDateMillis, zoneId)
            if (schedLocalDate.isAfter(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.FUTURE_SCHEDULED_DATE
            }
            if (!isTargetingToday && !schedLocalDate.isEqual(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.OTHER_DAY_ANCHOR
            }
        }

        // 3. Dual-epoch absolute scheduledTime check (>= ONE_DAY_MILLIS)
        val rawTime = task.scheduledTime
        val isDualEpochTime = rawTime != null && rawTime >= ONE_DAY_MILLIS
        if (isDualEpochTime && rawTime != null) {
            val rawTimeDate = epochToLocalDate(rawTime, zoneId)
            if (rawTimeDate.isAfter(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.FUTURE_SCHEDULED_TIMESTAMP
            }
            if (!isTargetingToday && !rawTimeDate.isEqual(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.OTHER_DAY_ANCHOR
            }
        }

        // 4. Effective scheduled date & resolved scheduled timestamp cross-check
        val effectiveDate = task.effectiveScheduledDateMillis()
        if (effectiveDate != null) {
            val effectiveLocalDate = epochToLocalDate(effectiveDate, zoneId)
            if (effectiveLocalDate.isAfter(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.FUTURE_SCHEDULED_DATE
            }
            if (!isTargetingToday && !effectiveLocalDate.isEqual(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                return DateIneligibilityReason.OTHER_DAY_ANCHOR
            }
        }

        val hasExplicitDateAnchor = task.scheduledDate != null || task.habitDate != null || isDualEpochTime
        if (hasExplicitDateAnchor) {
            val resolvedStart = task.resolveScheduledEpochMillis(dayStart)
            if (resolvedStart != null) {
                val resolvedDate = epochToLocalDate(resolvedStart, zoneId)
                if (resolvedDate.isAfter(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                    return DateIneligibilityReason.FUTURE_SCHEDULED_TIMESTAMP
                }
                if (!isTargetingToday && !resolvedDate.isEqual(targetDate) && !hasHardDeadlineOnOrBeforeTarget) {
                    return DateIneligibilityReason.OTHER_DAY_ANCHOR
                }
            }
        }

        // 5. Unscheduled backlog with future-only deadline check (disallowed during automatic background passes)
        if (!hasExplicitDateAnchor && effectiveDate == null && !allowFutureDeadlinedBacklog) {
            if (hasStrictlyFutureDeadline(task, targetDate, zoneId)) {
                return DateIneligibilityReason.FUTURE_ONLY_DEADLINE
            }
        }

        return null
    }

    /**
     * Convenience predicate returning true iff [task] passes all date-eligibility sanity checks for [targetDate].
     */
    fun isEntityEligibleForDay(
        task: TaskEntity,
        targetDate: LocalDate,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        allowFutureDeadlinedBacklog: Boolean = false,
        allowHardDeadlineRescue: Boolean = false
    ): Boolean = inspectEntityForDay(
        task = task,
        targetDate = targetDate,
        nowMillis = nowMillis,
        zoneId = zoneId,
        allowFutureDeadlinedBacklog = allowFutureDeadlinedBacklog,
        allowHardDeadlineRescue = allowHardDeadlineRescue
    ) == null

    /**
     * Partitions [tasks] into date-eligible entities and rejected entries with explicit reasons.
     */
    fun validateEntitiesForDay(
        tasks: List<TaskEntity>,
        targetDate: LocalDate,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        allowFutureDeadlinedBacklog: Boolean = false,
        allowHardDeadlineRescue: Boolean = false
    ): EntityValidationResult {
        if (tasks.isEmpty()) return EntityValidationResult(emptyList(), emptyList())
        val eligible = ArrayList<TaskEntity>(tasks.size)
        val rejected = mutableListOf<RejectedTaskEntry>()
        for (task in tasks) {
            val reason = inspectEntityForDay(
                task = task,
                targetDate = targetDate,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = allowFutureDeadlinedBacklog,
                allowHardDeadlineRescue = allowHardDeadlineRescue
            )
            if (reason == null) {
                eligible.add(task)
            } else {
                rejected.add(
                    RejectedTaskEntry(
                        taskId = task.id,
                        reason = reason,
                        explanation = buildReasonExplanation(task.id, reason, targetDate)
                    )
                )
            }
        }
        return EntityValidationResult(eligibleTasks = eligible, rejectedTasks = rejected)
    }

    /**
     * Core scheduler-level sanity check for [SchedulableTask] inputs.
     *
     * Strips any task whose [SchedulableTask.preferredDayStartMillis] or
     * [SchedulableTask.scheduledStartMillis] belongs to a date strictly after [targetDate],
     * unless [allowHardDeadlineRescue] is true and the task has a hard deadline on or before [targetDate].
     */
    fun validateSchedulableTasksForDay(
        tasks: List<SchedulableTask>,
        targetDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        allowHardDeadlineRescue: Boolean = true
    ): SchedulableValidationResult {
        if (tasks.isEmpty()) return SchedulableValidationResult(emptyList(), emptyList())
        val dayEnd = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        val eligible = ArrayList<SchedulableTask>(tasks.size)
        val rejected = mutableListOf<RejectedTaskEntry>()

        for (task in tasks) {
            val hasHardDeadlineRescue = allowHardDeadlineRescue &&
                task.deadlineMillis != null &&
                task.deadlineMillis <= dayEnd

            val preferredReason = task.preferredDayStartMillis?.let { prefMillis ->
                val prefDate = epochToLocalDate(prefMillis, zoneId)
                if (prefDate.isAfter(targetDate) && !hasHardDeadlineRescue) {
                    DateIneligibilityReason.FUTURE_PREFERRED_DAY
                } else {
                    null
                }
            }
            if (preferredReason != null) {
                rejected.add(
                    RejectedTaskEntry(
                        taskId = task.id,
                        reason = preferredReason,
                        explanation = buildReasonExplanation(task.id, preferredReason, targetDate)
                    )
                )
                continue
            }

            val scheduledReason = task.scheduledStartMillis?.let { schedMillis ->
                val schedDate = epochToLocalDate(schedMillis, zoneId)
                if (schedDate.isAfter(targetDate) && !hasHardDeadlineRescue) {
                    DateIneligibilityReason.FUTURE_SCHEDULED_TIMESTAMP
                } else {
                    null
                }
            }
            if (scheduledReason != null) {
                rejected.add(
                    RejectedTaskEntry(
                        taskId = task.id,
                        reason = scheduledReason,
                        explanation = buildReasonExplanation(task.id, scheduledReason, targetDate)
                    )
                )
                continue
            }

            eligible.add(task)
        }

        return SchedulableValidationResult(eligibleTasks = eligible, rejectedTasks = rejected)
    }

    /**
     * Write-time sanity gate invoked immediately before persisting a scheduled placement to Room.
     *
     * Verifies that writing a placement at [proposedStartMillis] will NEVER pull an existing
     * future-anchored task (`scheduledDate`, `scheduledTime`, `habitDate`, or resolved anchor strictly
     * after the proposed date) backward onto an earlier date (e.g., Today).
     *
     * Also protects against mid-flight race conditions where a user rescheduled a task to a future
     * date while a background worker pass was running after a deletion.
     */
    fun canApplyPlacementToEntity(
        existingTask: TaskEntity,
        proposedStartMillis: Long,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        allowFutureDeadlinedBacklog: Boolean = true,
        allowHardDeadlineRescue: Boolean = false
    ): Boolean {
        val proposedDate = epochToLocalDate(proposedStartMillis, zoneId)
        val proposedDayStart = proposedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val proposedDayEnd = proposedDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

        val hasHardDeadlineRescue = allowHardDeadlineRescue &&
            hasDeadlineOnOrBefore(existingTask, proposedDate, proposedDayStart, proposedDayEnd, zoneId)
        if (hasHardDeadlineRescue) return true

        existingTask.habitDate?.let { habitMillis ->
            if (epochToLocalDate(habitMillis, zoneId).isAfter(proposedDate)) return false
        }

        existingTask.scheduledDate?.let { schedDateMillis ->
            if (epochToLocalDate(schedDateMillis, zoneId).isAfter(proposedDate)) return false
        }

        val rawTime = existingTask.scheduledTime
        val isDualEpochTime = rawTime != null && rawTime >= ONE_DAY_MILLIS
        if (isDualEpochTime && rawTime != null) {
            if (epochToLocalDate(rawTime, zoneId).isAfter(proposedDate)) return false
        }

        val effectiveDate = existingTask.effectiveScheduledDateMillis()
        if (effectiveDate != null && epochToLocalDate(effectiveDate, zoneId).isAfter(proposedDate)) {
            return false
        }

        val hasExplicitDateAnchor = existingTask.scheduledDate != null || existingTask.habitDate != null || isDualEpochTime
        if (hasExplicitDateAnchor) {
            val resolvedStart = existingTask.resolveScheduledEpochMillis(proposedDayStart)
            if (resolvedStart != null && epochToLocalDate(resolvedStart, zoneId).isAfter(proposedDate)) {
                return false
            }
        } else if (!allowFutureDeadlinedBacklog) {
            if (hasStrictlyFutureDeadline(existingTask, proposedDate, zoneId)) {
                return false
            }
        }

        return true
    }

    private fun epochToLocalDate(epochMillis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    private fun hasDeadlineOnOrBefore(
        task: TaskEntity,
        targetDate: LocalDate,
        dayStart: Long,
        dayEnd: Long,
        zoneId: ZoneId
    ): Boolean {
        val rawDeadlineTime = task.deadlineTime
        if (rawDeadlineTime != null && rawDeadlineTime >= ONE_DAY_MILLIS) {
            return rawDeadlineTime <= dayEnd
        }
        val dDate = task.deadlineDate
        if (dDate != null) {
            val deadlineLocalDate = epochToLocalDate(dDate, zoneId)
            return !deadlineLocalDate.isAfter(targetDate)
        }
        val resolved = task.resolveDeadlineEpochMillis(dayStart)
        return resolved != null && resolved <= dayEnd
    }

    private fun hasStrictlyFutureDeadline(
        task: TaskEntity,
        targetDate: LocalDate,
        zoneId: ZoneId
    ): Boolean {
        val rawDeadlineTime = task.deadlineTime
        if (rawDeadlineTime != null && rawDeadlineTime >= ONE_DAY_MILLIS) {
            return epochToLocalDate(rawDeadlineTime, zoneId).isAfter(targetDate)
        }
        val dDate = task.deadlineDate
        if (dDate != null) {
            return epochToLocalDate(dDate, zoneId).isAfter(targetDate)
        }
        return false
    }

    private fun buildReasonExplanation(
        taskId: String,
        reason: DateIneligibilityReason,
        targetDate: LocalDate
    ): String = when (reason) {
        DateIneligibilityReason.FUTURE_SCHEDULED_DATE ->
            "Task $taskId is scheduled on a future date after $targetDate"
        DateIneligibilityReason.FUTURE_SCHEDULED_TIMESTAMP ->
            "Task $taskId has a scheduled timestamp after $targetDate"
        DateIneligibilityReason.FUTURE_HABIT_DATE ->
            "Task $taskId has a future habit date after $targetDate"
        DateIneligibilityReason.FUTURE_PREFERRED_DAY ->
            "Task $taskId has a preferred day after $targetDate"
        DateIneligibilityReason.FUTURE_ONLY_DEADLINE ->
            "Unscheduled task $taskId has a future deadline after $targetDate"
        DateIneligibilityReason.OTHER_DAY_ANCHOR ->
            "Task $taskId is anchored to a different date than $targetDate"
    }
}
