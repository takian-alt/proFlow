package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.localTimeOfDayOffset
import com.neuroflow.app.data.local.entity.startOfLocalDay

/**
 * Non-negotiable safety contracts for all auto-scheduling flows.
 */
object AutoSchedulingContracts {

    fun hasManualScheduleData(task: TaskEntity): Boolean {
        return (task.scheduledDate != null || task.scheduledTime != null) && !task.isAutoScheduled
    }

    fun isMutableByAutoScheduler(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.isScheduleLocked || task.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE) {
            return false
        }
        val isPastSchedule = task.scheduledDate?.let { date ->
            val time = task.scheduledTime ?: 0L
            (date + time) < nowMillis
        } ?: false
        return !hasManualScheduleData(task) || task.isAutoScheduled || isPastSchedule
    }

    /**
     * True when a task has a deadline anchor but no explicit scheduled slot yet.
     */
    fun hasDeadlineData(task: TaskEntity): Boolean {
        return task.deadlineDate != null
    }

    /**
     * Normalizes recurring anchors through local-safe helpers to avoid timezone and
     * minute-of-day drift when recurrence data is reused across cycles.
     */
    fun normalizedRecurringAnchorMillis(task: TaskEntity): Long? {
        val anchor = task.habitDate ?: return null
        return startOfLocalDay(anchor) + localTimeOfDayOffset(anchor)
    }
}

enum class AutoScheduleRejectionReason {
    LOCKED_TASK,
    MANUAL_SCHEDULE_PRESENT,
    DEPENDENCY_BLOCKED,
    NO_CAPACITY_IN_HORIZON,
    LOW_ENERGY_PROTECTION,
    OUTSIDE_WORKDAY,
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
