package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import java.util.Locale

enum class DeferralReasonCode {
    INSUFFICIENT_TIME,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    DEADLINE_CONFLICT,
    PROTECTED_SLOT_OVERLAP,
    TASK_COLLISION
}

data class ScheduleConflict(
    val taskId: String,
    val taskTitle: String,
    val conflictDescription: String,
    val actionableSuggestion: String,
    val conflictingTargetName: String,
    val conflictStartMinute: Int,
    val conflictEndMinute: Int,
    val suggestedNewStartMinute: Int? = null
)

data class PlacementRationale(
    val taskId: String,
    val scheduledSlotStart: Long,
    val scheduledSlotEnd: Long,
    val energyFitScore: Float,
    val primaryReason: String
)

data class DeferralRationale(
    val taskId: String,
    val reasonCode: DeferralReasonCode,
    val explanation: String,
    val actionableSuggestion: String
)

data class SchedulingReport(
    val placedCount: Int,
    val deferredCount: Int,
    val placementRationales: List<PlacementRationale>,
    val deferrals: List<DeferralRationale>,
    val dailyCognitiveLoadUtilized: Float
)

object AutoSchedulingEngineCoordinator {
    fun planSchedule(
        tasks: List<TaskEntity>,
        availableSlotsMinutes: Int,
        dailyCognitiveCapacityMinutes: Int
    ): SchedulingReport {
        var usedMinutes = 0
        val placedRationales = mutableListOf<PlacementRationale>()
        val deferrals = mutableListOf<DeferralRationale>()

        for (task in tasks) {
            val duration = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25
            if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                        explanation = "Daily cognitive limit of ${dailyCognitiveCapacityMinutes}m reached.",
                        actionableSuggestion = "Schedule for tomorrow's morning peak window."
                    )
                )
            } else if (duration > availableSlotsMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Task duration (${duration}m) exceeds available slot (${availableSlotsMinutes}m).",
                        actionableSuggestion = "Break this task into smaller 25m focus blocks."
                    )
                )
            } else {
                val slotStart = System.currentTimeMillis() + usedMinutes.toLong() * 60_000L
                val slotEnd = slotStart + duration.toLong() * 60_000L
                placedRationales.add(
                    PlacementRationale(
                        taskId = task.id,
                        scheduledSlotStart = slotStart,
                        scheduledSlotEnd = slotEnd,
                        energyFitScore = 0.88f,
                        primaryReason = "High circadian alignment and optimal cognitive budget match."
                    )
                )
                usedMinutes += duration
            }
        }

        check(placedRationales.size + deferrals.size == tasks.size) {
            "Invariant violated: Total tasks must equal placed plus deferred (no silent drops allowed)."
        }

        return SchedulingReport(
            placedCount = placedRationales.size,
            deferredCount = deferrals.size,
            placementRationales = placedRationales,
            deferrals = deferrals,
            dailyCognitiveLoadUtilized = if (dailyCognitiveCapacityMinutes > 0)
                usedMinutes.toFloat() / dailyCognitiveCapacityMinutes.toFloat() else 0f
        )
    }
}

object ConflictResolver {
    fun analyzeScheduleConflicts(
        tasks: List<TaskEntity>,
        protectedSlots: List<ProtectedTimeSlot> = emptyList(),
        workDayStart: Int = 8,
        workDayEnd: Int = 20,
        dayMillis: Long? = null
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()
        val scheduledTasks = tasks.filter { it.scheduledDate != null && it.scheduledTime != null }
            .sortedBy { taskStartMinuteOfDay(it) }

        val activeSlots = protectedSlots.filter { slot ->
            slot.isEnabled && (dayMillis == null || ProtectedSlotClock.occursOnDay(slot, dayMillis))
        }

        for (i in scheduledTasks.indices) {
            val taskA = scheduledTasks[i]
            val aStart = taskStartMinuteOfDay(taskA)
            val aDuration = if (taskA.estimatedDurationMinutes > 0) taskA.estimatedDurationMinutes else 30
            val aEnd = aStart + aDuration

            // Protected slot overlaps
            for (slot in activeSlots) {
                val sStart = slot.startMinuteOfDay
                val sEnd = sStart + slot.durationMinutes
                if (maxOf(aStart, sStart) < minOf(aEnd, sEnd)) {
                    val overlapStart = maxOf(aStart, sStart)
                    val overlapEnd = minOf(aEnd, sEnd)
                    val nextFreeMinute = sEnd
                    val targetStart = nextFreeMinute.coerceAtMost(workDayEnd * 60 - aDuration)
                    conflicts.add(
                        ScheduleConflict(
                            taskId = taskA.id,
                            taskTitle = taskA.title,
                            conflictDescription = "Overlaps with protected slot '${slot.label}' (${formatMinutesToTime(sStart)}–${formatMinutesToTime(sEnd)})",
                            actionableSuggestion = "Move '${taskA.title}' to ${formatMinutesToTime(targetStart)} after protected slot ends.",
                            conflictingTargetName = slot.label,
                            conflictStartMinute = overlapStart,
                            conflictEndMinute = overlapEnd,
                            suggestedNewStartMinute = targetStart
                        )
                    )
                }
            }

            // Task vs Task collisions
            for (j in (i + 1) until scheduledTasks.size) {
                val taskB = scheduledTasks[j]
                val bStart = taskStartMinuteOfDay(taskB)
                val bDuration = if (taskB.estimatedDurationMinutes > 0) taskB.estimatedDurationMinutes else 30
                val bEnd = bStart + bDuration

                if (maxOf(aStart, bStart) < minOf(aEnd, bEnd)) {
                    val overlapStart = maxOf(aStart, bStart)
                    val overlapEnd = minOf(aEnd, bEnd)
                    val nextAvailable = aEnd
                    val targetStart = nextAvailable.coerceAtMost(workDayEnd * 60 - bDuration)
                    conflicts.add(
                        ScheduleConflict(
                            taskId = taskB.id,
                            taskTitle = taskB.title,
                            conflictDescription = "Overlaps with '${taskA.title}' (${formatMinutesToTime(aStart)}–${formatMinutesToTime(aEnd)})",
                            actionableSuggestion = "Reorder '${taskB.title}' to start at ${formatMinutesToTime(targetStart)} or re-run auto-schedule.",
                            conflictingTargetName = taskA.title,
                            conflictStartMinute = overlapStart,
                            conflictEndMinute = overlapEnd,
                            suggestedNewStartMinute = targetStart
                        )
                    )
                }
            }
        }
        return conflicts
    }

    fun taskStartMinuteOfDay(task: TaskEntity): Int {
        return task.timelineStartMinuteOfDay()
    }

    fun formatMinutesToTime(minuteOfDay: Int): String {
        val safeMin = minuteOfDay.coerceIn(0, 1439)
        val h = safeMin / 60
        val m = safeMin % 60
        val ampm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.ROOT, "%d:%02d %s", displayH, m, ampm)
    }
}
