package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * Non-negotiable safety contracts for all auto-scheduling flows.
 */
object AutoSchedulingContracts {

    /**
     * A manual schedule claims a full timestamp. A date with no time-of-day is not a finished
     * schedule — the scheduler is still expected to place the task, on that day.
     */
    fun hasManualScheduleData(task: TaskEntity): Boolean {
        return task.scheduledDate != null && task.scheduledTime != null && !task.isAutoScheduled
    }

    fun isMutableByAutoScheduler(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.isScheduleLocked || task.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE) {
            return false
        }
        return !hasManualScheduleData(task) || task.isAutoScheduled
    }

    /**
     * True when a task has a deadline anchor but no explicit scheduled slot yet.
     */
    fun hasDeadlineData(task: TaskEntity): Boolean {
        return task.deadlineDate != null
    }
}

enum class AutoScheduleRejectionReason {
    LOCKED_TASK,
    MANUAL_SCHEDULE_PRESENT,
    DEPENDENCY_BLOCKED,
    NO_CAPACITY_IN_HORIZON,
    LOW_ENERGY_PROTECTION,
    OUTSIDE_WORKDAY,
    PROTECTED_SLOT,
    UNKNOWN
}

data class AutoScheduleDecisionTelemetry(
    val taskId: String,
    val generatedAtMillis: Long,
    val horizonDays: Int,
    val wasApplied: Boolean,
    val selectedSlotDate: Long? = null,
    val selectedSlotTime: Long? = null,
    val rejectionReason: AutoScheduleRejectionReason? = null,
    val inputs: AutoScheduleInputsSnapshot
)

data class AutoScheduleInputsSnapshot(
    val priorityScore: Float,
    val energyScore: Float,
    val sleepPressurePoints: Int,
    val hasDependencies: Boolean,
    val estimatedDurationMinutes: Int,
    val tagProfileHints: List<String>
)
