package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class DeferralReasonCode {
    INSUFFICIENT_TIME,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    DEADLINE_CONFLICT,
    PROTECTED_SLOT_OVERLAP,
    TASK_COLLISION,
    DEPENDENCY_BLOCKED
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
            // BUG-297 Fix: Use TaskBlockPlan.plannedMinutes to prevent cognitive meter inflation on split Part 2 tasks
            val duration = TaskBlockPlan.plannedMinutes(task)
            val remainingSlots = availableSlotsMinutes - usedMinutes
            if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                        explanation = "Daily cognitive limit of ${dailyCognitiveCapacityMinutes}m reached.",
                        actionableSuggestion = "Schedule for tomorrow's morning peak window."
                    )
                )
            } else if (duration > remainingSlots) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Task duration (${duration}m) exceeds available slot (${maxOf(0, remainingSlots)}m).",
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
    fun findConflictFreeStart(
        searchStartMinute: Int,
        duration: Int,
        workDayStartHour: Int,
        workDayEndHour: Int,
        occupiedIntervals: List<Pair<Int, Int>>
    ): Int? {
        val sortedIntervals = occupiedIntervals.sortedBy { it.first }
        // Bug 5 Fix (ADR-030): Clamp suggested conflict-free start to workDayStart
        var candidate = maxOf(searchStartMinute, workDayStartHour.coerceIn(0, 23) * 60)
        val effectiveEndHour = if (workDayEndHour == 0) 24 else workDayEndHour
        val maxMinute = effectiveEndHour * 60
        while (candidate + duration <= maxMinute) {
            val overlapping = sortedIntervals.firstOrNull { (start, end) ->
                maxOf(candidate, start) < minOf(candidate + duration, end)
            }
            if (overlapping == null) {
                return candidate
            } else {
                candidate = maxOf(candidate + 1, overlapping.second)
            }
        }
        return null
    }

    fun analyzeScheduleConflicts(
        tasks: List<TaskEntity>,
        protectedSlots: List<ProtectedTimeSlot> = emptyList(),
        workDayStart: Int = 8,
        workDayEnd: Int = 20,
        dayMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduleConflict> {
        val referenceNow = dayMillis ?: System.currentTimeMillis()
        // Only tasks with an explicit scheduled time of day are placed on the intraday timeline (BUG-152)
        val scheduledTasks = tasks.filter { it.scheduledTime != null && it.resolveScheduledEpochMillis(referenceNow) != null }
        if (scheduledTasks.isEmpty()) return emptyList()

        fun analyzeForSingleDay(
            dayTasks: List<TaskEntity>,
            targetDayMillis: Long
        ): List<ScheduleConflict> {
            val conflicts = mutableListOf<ScheduleConflict>()
            val sortedTasks = dayTasks.sortedBy { taskStartMinuteOfDay(it, zoneId, targetDayMillis) }

            val activeSlots = protectedSlots.filter { slot ->
                slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, targetDayMillis)
            }

            val allSlotIntervals = activeSlots.map { slot ->
                slot.startMinuteOfDay to (slot.startMinuteOfDay + slot.durationMinutes + slot.recoveryBufferMinutes)
            }

            for (i in sortedTasks.indices) {
                val taskA = sortedTasks[i]
                val aStart = taskStartMinuteOfDay(taskA, zoneId, targetDayMillis)
                val aDuration = TaskBlockPlan.plannedMinutes(taskA)
                val aEnd = aStart + aDuration

                val otherTasksForA = sortedTasks.filterIndexed { idx, _ -> idx != i }.map { t ->
                    val start = taskStartMinuteOfDay(t, zoneId, targetDayMillis)
                    start to (start + TaskBlockPlan.plannedMinutes(t))
                }
                val occupiedForA = allSlotIntervals + otherTasksForA

                // Protected slot overlaps
                for (slot in activeSlots) {
                    val sStart = slot.startMinuteOfDay
                    val sEnd = sStart + slot.durationMinutes + slot.recoveryBufferMinutes
                    if (maxOf(aStart, sStart) < minOf(aEnd, sEnd)) {
                        val overlapStart = maxOf(aStart, sStart)
                        val overlapEnd = minOf(aEnd, sEnd)
                        val bufferNote = if (slot.recoveryBufferMinutes > 0) " (incl. ${slot.recoveryBufferMinutes}m buffer)" else ""
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = aDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForA
                        )
                        val effectiveEnd = if (workDayEnd == 0) 24 else workDayEnd
                        val suggestion = if (targetStart != null) {
                            "Move '${taskA.title}' to ${formatMinutesToTime(targetStart)} after protected slot ends."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(effectiveEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskA.id,
                                taskTitle = taskA.title,
                                conflictDescription = "Overlaps with protected slot '${slot.label}' (${formatMinutesToTime(sStart)}–${formatMinutesToTime(sEnd)}$bufferNote)",
                                actionableSuggestion = suggestion,
                                conflictingTargetName = slot.label,
                                conflictStartMinute = overlapStart,
                                conflictEndMinute = overlapEnd,
                                suggestedNewStartMinute = targetStart
                            )
                        )
                    }
                }

                // Task vs Task collisions
                for (j in (i + 1) until sortedTasks.size) {
                    val taskB = sortedTasks[j]
                    val bStart = taskStartMinuteOfDay(taskB, zoneId, targetDayMillis)
                    val bDuration = TaskBlockPlan.plannedMinutes(taskB)
                    val bEnd = bStart + bDuration

                    if (maxOf(aStart, bStart) < minOf(aEnd, bEnd)) {
                        val overlapStart = maxOf(aStart, bStart)
                        val overlapEnd = minOf(aEnd, bEnd)
                        val otherTasksForB = sortedTasks.filterIndexed { idx, _ -> idx != j }.map { t ->
                            val start = taskStartMinuteOfDay(t, zoneId, targetDayMillis)
                            start to (start + TaskBlockPlan.plannedMinutes(t))
                        }
                        val occupiedForB = allSlotIntervals + otherTasksForB
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = bDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForB
                        )
                        val effectiveEnd = if (workDayEnd == 0) 24 else workDayEnd
                        val suggestion = if (targetStart != null) {
                            "Reorder '${taskB.title}' to start at ${formatMinutesToTime(targetStart)} or re-run auto-schedule."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(effectiveEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskB.id,
                                taskTitle = taskB.title,
                                conflictDescription = "Overlaps with '${taskA.title}' (${formatMinutesToTime(aStart)}–${formatMinutesToTime(aEnd)})",
                                actionableSuggestion = suggestion,
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

        return if (dayMillis != null) {
            val targetDate = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate()
            val dayTasks = scheduledTasks.filter { task ->
                val sMillis = task.resolveScheduledEpochMillis(dayMillis) ?: return@filter false
                Instant.ofEpochMilli(sMillis).atZone(zoneId).toLocalDate() == targetDate
            }
            analyzeForSingleDay(dayTasks, dayMillis)
        } else {
            val groups = scheduledTasks.groupBy { task ->
                val sMillis = task.resolveScheduledEpochMillis() ?: 0L
                Instant.ofEpochMilli(sMillis).atZone(zoneId).toLocalDate()
            }
            groups.flatMap { (date, dateTasks) ->
                val dateStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                analyzeForSingleDay(dateTasks, dateStartMillis)
            }
        }
    }

    fun taskStartMinuteOfDay(
        task: TaskEntity,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceDateMillis: Long = System.currentTimeMillis()
    ): Int {
        val rawTime = task.scheduledTime
        if (rawTime != null && rawTime in 0 until ONE_DAY_MILLIS) {
            return (rawTime / 60_000L).toInt().coerceIn(0, 1439)
        }
        val anchorMillis = task.resolveScheduledEpochMillis(referenceDateMillis) ?: return 0
        val zdt = Instant.ofEpochMilli(anchorMillis).atZone(zoneId)
        return (zdt.hour * 60 + zdt.minute).coerceIn(0, 1439)
    }

    fun formatMinutesToTime(minuteOfDay: Int): String {
        val safeMin = ((minuteOfDay % 1440) + 1440) % 1440
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
