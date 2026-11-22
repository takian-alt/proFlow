package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * Non-negotiable safety contracts for all auto-scheduling flows.
 */
object AutoSchedulingContracts {

    /**
     * A manual schedule claims a full timestamp. A date with no time-of-day is not a finished
     * schedule — the scheduler is still expected to place the task, on that day.
     * Dual representation epoch timestamps (scheduledTime >= ONE_DAY_MILLIS) contain both date and time (BUG-452).
     */
    fun hasManualScheduleData(task: TaskEntity): Boolean {
        val hasTime = task.scheduledTime != null
        val hasDate = task.scheduledDate != null || (hasTime && task.scheduledTime!! >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)
        return hasDate && hasTime && !task.isAutoScheduled
    }

    /**
     * A task is mutable by the auto-scheduler whenever it is NOT explicitly schedule-locked
     * (`isScheduleLocked == false`) and NOT in the Eliminate/Icebox quadrant.
     *
     * Explicit manual pins (drag-and-drop, assign-time, defer, calendar external move, or toggling
     * "Schedule Locked (Pinned)") set `isScheduleLocked = true`. Leaving Schedule Unlocked
     * explicitly allows the auto-scheduler to rescue incomplete/missed slots and optimize placement.
     */
    fun isMutableByAutoScheduler(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.isScheduleLocked || task.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE) {
            return false
        }
        return true
    }

    /**
     * True when a task has a deadline anchor but no explicit scheduled slot yet.
     */
    fun hasDeadlineData(task: TaskEntity): Boolean {
        return task.deadlineDate != null
    }

    val CURRICULUM_ROADMAP_KEYWORDS = listOf("stage", "month", "learn", "build", "project")

    /**
     * Identifies curriculum roadmap tasks (e.g. tagged with #stage, #month, #learn, #build, #project,
     * or associated with goal_stage_ goals).
     */
    fun isCurriculumTask(task: TaskEntity): Boolean {
        if (task.goalId?.contains("goal_stage_", ignoreCase = true) == true) return true
        val lowerTags = task.tags.lowercase()
        val splitTags = lowerTags.split(",", " ", ";", "#")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
        return splitTags.any { tag ->
            CURRICULUM_ROADMAP_KEYWORDS.any { kw -> tag.contains(kw) }
        }
    }

    /**
     * Personal tasks are tasks without curriculum/roadmap tags or goal_stage_ goals.
     */
    fun isPersonalTask(task: TaskEntity): Boolean = !isCurriculumTask(task)
}

fun TaskEntity.isCurriculumTask(): Boolean = AutoSchedulingContracts.isCurriculumTask(this)
fun TaskEntity.isPersonalTask(): Boolean = AutoSchedulingContracts.isPersonalTask(this)

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
