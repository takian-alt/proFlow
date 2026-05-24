package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Calendar

/**
 * Phase 3: AutoSchedulingEngine
 *
 * Orchestrates auto-scheduling decisions by:
 * 1. Building capacity horizon across 3-day window with time slots
 * 2. Scoring task-slot fit using tag profiles + energy context
 * 3. Inserting recovery breaks based on cognitive load
 * 4. Generating ranked scheduling decisions with telemetry
 *
 * Consumes:
 * - AutoSchedulingContracts (safety gates, gating functions)
 * - TaskTagSchedulingProfile (tag-to-profile mapping)
 * - UserPreferencesDataStore (auto-scheduling settings)
 */
@Singleton
class AutoSchedulingEngine @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) {

    // ==================== Data Classes ====================

    data class TimeSlot(
        val startMillis: Long,
        val endMillis: Long,
        val dayIndex: Int,
        val hourOfDay: Int,
        val availableCapacityMinutes: Int,
        val availableEnergy: Int,
        val energyProfile: EnergyProfile,
        var assignedMinutes: Int = 0,
        var reservedBreakMinutes: Int = 0,
        val cognitiveLoadPercent: Int = 0
    )

    data class EnergyProfile(
        val zone: EnergyZone,
        val confidence: Float,
        val circadianBonus: Float
    ) {
        companion object {
            fun from(energyScore: Int, momentConfidence: Float, circadianFactor: Float): EnergyProfile {
                val zone = when (energyScore) {
                    in 80..100 -> EnergyZone.PEAK
                    in 60..79 -> EnergyZone.HIGH
                    in 40..59 -> EnergyZone.MODERATE
                    in 1..39 -> EnergyZone.LOW
                    else -> EnergyZone.CRITICAL
                }
                return EnergyProfile(zone, momentConfidence, circadianFactor)
            }
        }
    }

    enum class EnergyZone { PEAK, HIGH, MODERATE, LOW, CRITICAL }

    data class TaskSlotFitScore(
        val taskId: String,
        val slotIndex: Int,
        val overallScore: Float,
        val energyMatch: Float,
        val tagFit: Float,
        val deadlineUrgency: Float,
        val fragmentationTolerance: Float,
        val breakPlacementBonus: Float = 0.0f
    ) : Comparable<TaskSlotFitScore> {
        override fun compareTo(other: TaskSlotFitScore): Int = other.overallScore.compareTo(this.overallScore)
    }

    data class ScheduleDecision(
        val taskId: String,
        val assignedSlotIndex: Int,
        val scheduledStartMillis: Long,
        val estimatedDurationMinutes: Int,
        val assignmentReason: String,
        val fitScore: TaskSlotFitScore,
        val telemetry: AutoScheduleDecisionTelemetry
    )

    data class CapacityHorizon(
        val slots: List<TimeSlot>,
        val horizonStartMillis: Long,
        val horizonEndMillis: Long,
        val totalAvailableMinutes: Int,
        val averageEnergyLevel: Int
    )

    data class DeadlinePressure(
        val daysUntilDeadline: Long,
        val pressureLevel: PressureLevel, // URGENT, HIGH, MODERATE, LOW
        val recommendedPacingDensity: Float // 0.3 (spread out) to 1.0 (pack tight)
    )

    enum class PressureLevel { URGENT, HIGH, MODERATE, LOW }

    // ==================== Public API ====================

    /**
     * Main entry point: Generate auto-schedule decisions for eligible unscheduled tasks.
     * Returns ranked list of scheduling assignments ready for transactional apply (Phase 4).
     *
     * **Mis-planning prevention strategy:**
     * 1. Respect energy boundaries: don't schedule high-effort tasks in low-energy slots
     * 2. Leave buffer capacity: never fill slots > 70% to allow for breaks and unexpected tasks
     * 3. Cluster compatible tasks: group low-fragmentation tasks together
     * 4. Respect dependencies: tasks are only eligible if their blockers are complete
     * 5. Prevent high-effort clustering: avoid stacking multiple high-effort tasks
     */
    suspend fun planAutoSchedule(
        unscheduledTasks: List<TaskEntity>,
        nowMillis: Long,
        energyScoreFn: suspend (Long) -> Pair<Int, Float>,
        busySlotStartMillis: Set<Long> = emptySet()
    ): List<ScheduleDecision> {
        // Filter: only include tasks eligible for auto-scheduling per Phase 1 contracts
        val eligibleTasks = unscheduledTasks.filter { task ->
            AutoSchedulingContracts.isMutableByAutoScheduler(task) &&
                !AutoSchedulingContracts.hasManualScheduleData(task) &&
                hasNoDependencyBlockers(task, unscheduledTasks)
        }

        if (eligibleTasks.isEmpty()) {
            return emptyList()
        }

        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.autoSchedulingEnabled) {
            return emptyList()
        }

        // Build capacity horizon
        val horizon = calculateCapacityHorizon(
            nowMillis = nowMillis,
            horizonDays = prefs.autoSchedulingHorizonDays,
            prefs = prefs,
            energyScoreFn = energyScoreFn
        )

        if (horizon.slots.isEmpty()) {
            return emptyList()
        }

        // **Mis-planning prevention: Sort tasks by urgency to assign deadline-critical tasks first**
        val sortedTasks = eligibleTasks.sortedWith(
            compareByDescending<TaskEntity> { priorityLevelScore(it) + impactPriorityBlend(it) }
                .thenBy { it.deadlineDate ?: Long.MAX_VALUE }
        )

        // Score all task-slot combinations per task
        val fitScoresByTask = mutableMapOf<String, MutableList<TaskSlotFitScore>>()
        for (task in sortedTasks) {
            val tagProfiles = TaskTagSchedulingProfile.profilesFor(task.tags)
            val deadlinePressure = calculateDeadlinePressure(task, nowMillis)

            for ((slotIndex, slot) in horizon.slots.withIndex()) {
                if (slot.startMillis <= nowMillis) {
                    continue
                }

                if (slot.availableCapacityMinutes <= 0) {
                    continue
                }

                if (busySlotStartMillis.contains(slot.startMillis)) {
                    continue
                }

                val deadlineMillis = task.deadlineDate?.plus(task.deadlineTime ?: 0L)
                if (deadlineMillis != null && slot.startMillis > deadlineMillis) {
                    continue
                }

                // **Mis-planning prevention: Skip slots that are already over-packed**
                val slotUtilization = calculateSlotUtilization(slot)
                if (slotUtilization > 0.70f) { // Leave 30% buffer
                    continue
                }

                // **Mis-planning prevention: Check energy-demand mismatch**
                if (!isEnergyDemandSatisfied(task, slot)) {
                    continue // Skip this slot, too risky for this task
                }

                val fitScore = scoreTaskSlotFit(
                    task = task,
                    slot = slot,
                    slotIndex = slotIndex,
                    tagProfiles = tagProfiles,
                    deadlinePressure = deadlinePressure,
                    prefs = prefs,
                    nowMillis = nowMillis
                )
                fitScoresByTask.getOrPut(task.id) { mutableListOf() }.add(fitScore)
            }
        }

        // Greedy assignment: iterate by task urgency and select best viable slot per task.
        val decisions = mutableListOf<ScheduleDecision>()
        val blockedSlotIndices = mutableSetOf<Int>()
        val highCognitiveMinutesByDay = mutableMapOf<Int, Int>()
        val highCognitiveHoursByDay = mutableMapOf<Int, MutableSet<Int>>()
        val highCognitiveMinutesSinceBreakByDay = mutableMapOf<Int, Int>()
        val breakPolicy = resolveBreakPolicy(prefs)

        sortedTasks.forEach { task ->
            if (decisions.any { it.taskId == task.id }) {
                return@forEach
            }

            val taskFits = fitScoresByTask[task.id].orEmpty().sortedWith(
                // Always prioritize by score first, then by timing
                // This ensures we pick the BEST slot, not just the earliest
                compareByDescending<TaskSlotFitScore> { it.overallScore }
                    .thenBy { horizon.slots[it.slotIndex].startMillis }
            )

            if (taskFits.isEmpty()) {
                return@forEach
            }

            val deadlineMillis = task.deadlineDate?.plus(task.deadlineTime ?: 0L)

            // Collect all viable candidates, categorized by timing preference
            val todayPreferredCandidates = mutableListOf<Pair<TaskSlotFitScore, Int>>()
            val todayFallbackCandidates = mutableListOf<Pair<TaskSlotFitScore, Int>>()
            val futurePreferredCandidates = mutableListOf<Pair<TaskSlotFitScore, Int>>()
            val futureFallbackCandidates = mutableListOf<Pair<TaskSlotFitScore, Int>>()

            taskFits.forEach { fitScore ->
                if (blockedSlotIndices.contains(fitScore.slotIndex)) {
                    return@forEach
                }

                val slot = horizon.slots[fitScore.slotIndex]
                val estimatedDuration = calculateRealisticDuration(task, slot)

                if (!hasContiguousAvailability(
                        slots = horizon.slots,
                        startIndex = fitScore.slotIndex,
                        durationMinutes = estimatedDuration,
                        blockedSlotIndices = blockedSlotIndices,
                        busySlotStartMillis = busySlotStartMillis
                    )
                ) {
                    return@forEach
                }

                if (!canAssignWithoutBurnout(
                        task = task,
                        slot = slot,
                        durationMinutes = estimatedDuration,
                        nowMillis = nowMillis,
                        prefs = prefs,
                        highCognitiveMinutesByDay = highCognitiveMinutesByDay,
                        highCognitiveHoursByDay = highCognitiveHoursByDay
                    )
                ) {
                    return@forEach
                }

                val preferredLatestStartMillis = resolvePreferredLatestStartMillis(
                    task = task,
                    nowMillis = nowMillis,
                    deadlineMillis = deadlineMillis,
                    estimatedDurationMinutes = estimatedDuration
                )

                val isTodaySlot = slot.dayIndex == 0
                val isWithinPreferredWindow = slot.startMillis <= preferredLatestStartMillis

                // Categorize candidates by timing preference
                when {
                    isTodaySlot && isWithinPreferredWindow -> {
                        todayPreferredCandidates.add(fitScore to estimatedDuration)
                    }
                    isTodaySlot && !isWithinPreferredWindow -> {
                        todayFallbackCandidates.add(fitScore to estimatedDuration)
                    }
                    !isTodaySlot && isWithinPreferredWindow -> {
                        futurePreferredCandidates.add(fitScore to estimatedDuration)
                    }
                    else -> {
                        futureFallbackCandidates.add(fitScore to estimatedDuration)
                    }
                }
            }

            // Select the best candidate from the highest-priority category that has options
            // Priority: today+preferred > today+fallback > future+preferred > future+fallback
            // Within each category, choose the slot with the HIGHEST fit score (best energy/breaks/tags)
            val chosen = todayPreferredCandidates.maxByOrNull { it.first.overallScore }
                ?: todayFallbackCandidates.maxByOrNull { it.first.overallScore }
                ?: futurePreferredCandidates.maxByOrNull { it.first.overallScore }
                ?: futureFallbackCandidates.maxByOrNull { it.first.overallScore }
                ?: return@forEach
            val fitScore = chosen.first
            val estimatedDuration = chosen.second
            val slot = horizon.slots[fitScore.slotIndex]

            decisions.add(
                ScheduleDecision(
                    taskId = fitScore.taskId,
                    assignedSlotIndex = fitScore.slotIndex,
                    scheduledStartMillis = slot.startMillis,
                    estimatedDurationMinutes = estimatedDuration,
                    assignmentReason = buildAssignmentReason(fitScore),
                    fitScore = fitScore,
                    telemetry = AutoScheduleDecisionTelemetry(
                        taskId = fitScore.taskId,
                        generatedAtMillis = System.currentTimeMillis(),
                        horizonDays = prefs.autoSchedulingHorizonDays,
                        wasApplied = false,
                        inputs = AutoScheduleInputsSnapshot(
                            priorityScore = task.impactScore.toFloat(),
                            energyScore = slot.availableEnergy.toFloat(),
                            sleepPressurePoints = prefs.sleepPressurePoints,
                            hasDependencies = task.dependsOnTaskIds.isNotEmpty(),
                            estimatedDurationMinutes = estimatedDuration,
                            tagProfileHints = TaskTagSchedulingProfile.profilesFor(task.tags).map { it.tag }
                        )
                    )
                )
            )

            // Reserve all occupied slot-hours for this assignment.
            occupyTaskSlots(
                slots = horizon.slots,
                startIndex = fitScore.slotIndex,
                durationMinutes = estimatedDuration,
                blockedSlotIndices = blockedSlotIndices
            )

            trackCognitiveLoad(
                task = task,
                slot = slot,
                durationMinutes = estimatedDuration,
                highCognitiveMinutesByDay = highCognitiveMinutesByDay,
                highCognitiveHoursByDay = highCognitiveHoursByDay
            )

            if (isCognitivelyIntense(task)) {
                val dayIndex = slot.dayIndex
                val accumulated = (highCognitiveMinutesSinceBreakByDay[dayIndex] ?: 0) + estimatedDuration
                if (accumulated >= breakPolicy.intervalMinutes) {
                    reserveBreakSlotsAfterTask(
                        slots = horizon.slots,
                        startIndex = fitScore.slotIndex,
                        taskDurationMinutes = estimatedDuration,
                        breakMinutes = breakPolicy.durationMinutes,
                        blockedSlotIndices = blockedSlotIndices
                    )
                    highCognitiveMinutesSinceBreakByDay[dayIndex] = 0
                } else {
                    highCognitiveMinutesSinceBreakByDay[dayIndex] = accumulated
                }
            }
        }

        return decisions.sortedByDescending { it.fitScore.overallScore }
    }

    /**
     * **Dynamic rescheduling**: Called when a previously-planned task wasn't completed.
     * Adjusts duration based on actual incomplete time and reschedules with updated urgency.
     *
     * **Anti-procrastination**: Schedules earlier if deadline pressure increased,
     * or earlier in the day to prevent "I'll do it tomorrow" procrastination.
     */
    suspend fun replanIncompleteTask(
        task: TaskEntity,
        timeSpentMinutes: Int, // How long was worked on before incompleteness detected
        nowMillis: Long,
        energyScoreFn: suspend (Long) -> Pair<Int, Float>,
        busySlotStartMillis: Set<Long> = emptySet()
    ): ScheduleDecision? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.autoSchedulingEnabled) {
            return null
        }

        // Calculate how much time is actually remaining using task history.
        val remainingMinutes = estimateRemainingMinutes(task, timeSpentMinutes)

        // Calculate deadline pressure to adjust scheduling aggressiveness
        val deadlinePressure = calculateDeadlinePressure(task, nowMillis)

        // With high deadline pressure, schedule as soon as possible (PEAK energy preferred)
        val pressureAdjustment = when (deadlinePressure.pressureLevel) {
            PressureLevel.URGENT -> 1.5f // Compress more aggressively
            PressureLevel.HIGH -> 1.2f
            PressureLevel.MODERATE -> 1.0f
            PressureLevel.LOW -> 0.8f // Can afford to wait for better energy
        }

        // Build horizon
        val horizon = calculateCapacityHorizon(
            nowMillis = nowMillis,
            horizonDays = prefs.autoSchedulingHorizonDays,
            prefs = prefs,
            energyScoreFn = energyScoreFn
        )

        val projectedDurationMinutes = (remainingMinutes * pressureAdjustment).toInt().coerceAtLeast(15)

        // Find the best available slot with priority to PEAK energy when under pressure
        val tagProfiles = TaskTagSchedulingProfile.profilesFor(task.tags)
        val deadlineMillis = task.deadlineDate?.plus(task.deadlineTime ?: 0L)
        val allCandidates = horizon.slots.withIndex()
            .filter { (slotIndex, slot) ->
                slot.startMillis > nowMillis &&
                slot.availableCapacityMinutes > 0 &&
                !busySlotStartMillis.contains(slot.startMillis) &&
                hasContiguousAvailability(
                    slots = horizon.slots,
                    startIndex = slotIndex,
                    durationMinutes = projectedDurationMinutes,
                    blockedSlotIndices = emptySet(),
                    busySlotStartMillis = busySlotStartMillis
                ) &&
                // Filter by energy demand satisfaction
                isEnergyDemandSatisfied(task, slot) &&
                // Filter by capacity (allow up to 85% when deadline is urgent)
                calculateSlotUtilization(slot) <= (if (deadlinePressure.pressureLevel == PressureLevel.URGENT) 0.85f else 0.70f)
            }
        val prioritizedCandidates = allCandidates.let { candidates ->
            val today = candidates.filter { (_, slot) -> slot.dayIndex == 0 }
            if (today.isNotEmpty()) today else candidates
        }
        val bestCandidate = prioritizedCandidates
            .maxByOrNull { (_, slot) ->
                // Rank slots: prefer PEAK energy when under deadline pressure
                val energyBonus = when (slot.energyProfile.zone) {
                    EnergyZone.PEAK -> 2.0f * pressureAdjustment
                    EnergyZone.HIGH -> 1.5f * pressureAdjustment
                    EnergyZone.MODERATE -> 1.0f
                    else -> 0.0f
                }
                // Also prefer earlier slots to prevent procrastination
                val dayBonus = if (slot.dayIndex == 0) 0.5f else if (slot.dayIndex == 1) 0.2f else 0f
                val deadlineBufferBonus = deadlineMillis?.let {
                    calculateDeadlineSafetyBufferFit(
                        task = task,
                        slotStartMillis = slot.startMillis,
                        deadlineMillis = it,
                        nowMillis = nowMillis
                    )
                } ?: 0.7f
                energyBonus + dayBonus + (deadlineBufferBonus * 1.2f)
            } ?: return null

        val slotIndex = bestCandidate.index
        val bestSlot = bestCandidate.value
        val fitScore = scoreTaskSlotFit(
            task = task,
            slot = bestSlot,
            slotIndex = slotIndex,
            tagProfiles = tagProfiles,
            deadlinePressure = deadlinePressure,
            prefs = prefs,
            nowMillis = nowMillis
        )

        return ScheduleDecision(
            taskId = task.id,
            assignedSlotIndex = slotIndex,
            scheduledStartMillis = bestSlot.startMillis,
            estimatedDurationMinutes = projectedDurationMinutes,
            assignmentReason = "rescheduled incomplete: ${remainingMinutes}min remaining, ${deadlinePressure.pressureLevel}",
            fitScore = fitScore,
            telemetry = AutoScheduleDecisionTelemetry(
                taskId = task.id,
                generatedAtMillis = System.currentTimeMillis(),
                horizonDays = prefs.autoSchedulingHorizonDays,
                wasApplied = false,
                inputs = AutoScheduleInputsSnapshot(
                    priorityScore = task.impactScore.toFloat(),
                    energyScore = bestSlot.availableEnergy.toFloat(),
                    sleepPressurePoints = prefs.sleepPressurePoints,
                    hasDependencies = false,
                    estimatedDurationMinutes = remainingMinutes,
                    tagProfileHints = tagProfiles.map { it.tag }
                )
            )
        )
    }

    /**
     * **Anti-procrastination spacing**: Returns optimal task distribution.
     * - Close deadline → pack tasks tighter, fill momentum windows
     * - Far deadline → spread out, maintain anti-procrastination pace (1-2 tasks per day)
     * - Prevents "I have time" procrastination trap
     */
    suspend fun calculateOptimalSpacing(
        unscheduledTasks: List<TaskEntity>,
        nowMillis: Long
    ): Map<String, Float> {
        val prefs = preferencesDataStore.preferencesFlow.first()

        // Calculate deadline pressure for each task
        val spacingFactors = mutableMapOf<String, Float>()

        for (task in unscheduledTasks) {
            val pressure = calculateDeadlinePressure(task, nowMillis)

            // Spacing factor = how to distribute tasks across available time
            // 1.0 = normal packing
            // 0.5 = spread out (anti-procrastination)
            // 1.5 = compress aggressively (deadline urgent)
            val spacingFactor = when (pressure.pressureLevel) {
                PressureLevel.URGENT -> 1.5f // Pack tight, maintain momentum
                PressureLevel.HIGH -> 1.2f   // Moderately compress
                PressureLevel.MODERATE -> 0.9f // Gentle spacing
                PressureLevel.LOW -> 0.5f   // Anti-procrastination: spread far out, but not too sparse
            }

            spacingFactors[task.id] = spacingFactor
        }

        return spacingFactors
    }

    // ==================== Private Implementation ====================

    /**
     * Mis-planning prevention: Check if slot energy level satisfies task requirements.
     * High-effort tasks must land in PEAK or HIGH energy slots.
     * Medium-effort tasks need at least MODERATE energy.
     * Low-effort tasks can land anywhere.
     */
    private fun isEnergyDemandSatisfied(task: TaskEntity, slot: TimeSlot): Boolean {
        if (task.taskType == com.neuroflow.app.domain.model.TaskType.ANALYTICAL &&
            slot.energyProfile.zone !in listOf(EnergyZone.PEAK, EnergyZone.HIGH, EnergyZone.MODERATE)
        ) {
            return false
        }

        return when {
            task.effortScore >= 80 -> slot.energyProfile.zone in listOf(EnergyZone.PEAK, EnergyZone.HIGH)
            task.effortScore >= 60 -> slot.energyProfile.zone in listOf(EnergyZone.PEAK, EnergyZone.HIGH, EnergyZone.MODERATE)
            else -> true // Low-effort tasks are flexible
        }
    }

    /**
     * **Dynamic rescheduling helper**: Calculate deadline pressure to determine pacing.
     * - URGENT: deadline < 1 day away
     * - HIGH: deadline 1–3 days away
     * - MODERATE: deadline 3–7 days away
     * - LOW: deadline > 7 days away
     *
     * Returns pacing density (how tightly to pack tasks).
     */
    private fun calculateDeadlinePressure(task: TaskEntity, nowMillis: Long): DeadlinePressure {
        if (task.deadlineDate == null) {
            return DeadlinePressure(Long.MAX_VALUE, PressureLevel.LOW, 0.5f)
        }

        val deadlineMillis = task.deadlineDate + (task.deadlineTime ?: 0L)
        val daysUntil = (deadlineMillis - nowMillis) / (24 * 60 * 60 * 1000L)

        return when {
            daysUntil <= 1 -> DeadlinePressure(
                daysUntil,
                PressureLevel.URGENT,
                1.5f // Pack tight to maintain momentum
            )
            daysUntil <= 3 -> DeadlinePressure(
                daysUntil,
                PressureLevel.HIGH,
                1.2f // Moderately compress
            )
            daysUntil <= 7 -> DeadlinePressure(
                daysUntil,
                PressureLevel.MODERATE,
                0.9f // Gentle spacing, but don't procrastinate
            )
            else -> DeadlinePressure(
                daysUntil,
                PressureLevel.LOW,
                0.5f // Anti-procrastination: spread tasks, prevent "I have time" trap
            )
        }
    }

    /**
     * Mis-planning prevention: Calculate current slot utilization to prevent over-packing.
     * Returns fraction 0.0-1.0 of capacity used.
     */
    private fun calculateSlotUtilization(slot: TimeSlot): Float {
        if (slot.availableCapacityMinutes <= 0) {
            return 1.0f
        }
        val usedMinutes = slot.assignedMinutes + slot.reservedBreakMinutes
        return min(1.0f, usedMinutes.toFloat() / max(1, slot.availableCapacityMinutes))
    }

    /**
     * Mis-planning prevention: Calculate realistic task duration based on effort and slot energy.
     * Don't optimistically assume tasks will complete in estimated time if energy is low.
     */
    private fun calculateRealisticDuration(task: TaskEntity, slot: TimeSlot): Int {
        val defaultBase = when {
            task.effortScore >= 80 -> 60
            task.effortScore >= 60 -> 45
            else -> 30
        }

        val baseEstimate = if (task.estimatedDurationMinutes > 0) {
            task.estimatedDurationMinutes
        } else {
            defaultBase
        }

        val historicalEstimate = if (task.actualDurationMinutes != null && task.actualDurationMinutes > 0) {
            ((baseEstimate + task.actualDurationMinutes.toInt()) / 2f).roundToInt()
        } else {
            baseEstimate
        }

        val errorAdjustment = 1.0f + (
            max(task.estimationErrorMape ?: 0f, task.estimationErrorSmape ?: 0f) / 100f
        ).coerceIn(0.0f, 1.0f)

        val adjustedEstimate = (historicalEstimate * errorAdjustment).toInt().coerceAtLeast(defaultBase)

        val energyAdjustment = when (slot.energyProfile.zone) {
            EnergyZone.PEAK -> 1.0f
            EnergyZone.HIGH -> 1.1f
            EnergyZone.MODERATE -> 1.25f
            EnergyZone.LOW -> 1.45f
            EnergyZone.CRITICAL -> 1.85f
        }

        return (adjustedEstimate * energyAdjustment).toInt().coerceAtMost(180)
    }

    private fun estimateRemainingMinutes(task: TaskEntity, timeSpentMinutes: Int): Int {
        val effectiveSpend = when {
            timeSpentMinutes > 0 -> timeSpentMinutes
            task.totalTimeTrackedMinutes > 0f -> task.totalTimeTrackedMinutes.roundToInt().coerceAtMost(task.estimatedDurationMinutes)
            else -> 0
        }

        val baseEstimate = if (task.estimatedDurationMinutes > 0) {
            task.estimatedDurationMinutes
        } else {
            when {
                task.effortScore >= 80 -> 60
                task.effortScore >= 60 -> 45
                else -> 30
            }
        }

        val adjustedEstimate = if (task.actualDurationMinutes != null && task.actualDurationMinutes > 0) {
            ((baseEstimate + task.actualDurationMinutes.toInt()) / 2f).roundToInt()
        } else {
            baseEstimate
        }

        return (adjustedEstimate - effectiveSpend).coerceAtLeast(15)
    }

    private fun hasContiguousAvailability(
        slots: List<TimeSlot>,
        startIndex: Int,
        durationMinutes: Int,
        blockedSlotIndices: Set<Int>,
        busySlotStartMillis: Set<Long>
    ): Boolean {
        val neededSlots = slotSpanForDuration(durationMinutes)
        if (startIndex + neededSlots > slots.size) return false

        val dayIndex = slots[startIndex].dayIndex
        for (offset in 0 until neededSlots) {
            val idx = startIndex + offset
            val slot = slots[idx]
            if (slot.dayIndex != dayIndex) return false
            if (slot.availableCapacityMinutes <= 0) return false
            if (idx in blockedSlotIndices) return false
            if (busySlotStartMillis.contains(slot.startMillis)) return false
            if (calculateSlotUtilization(slot) > 0.70f) return false
        }

        return true
    }

    private fun slotSpanForDuration(durationMinutes: Int): Int {
        return ((durationMinutes.coerceAtLeast(1) + 59) / 60).coerceAtLeast(1)
    }

    private fun occupyTaskSlots(
        slots: List<TimeSlot>,
        startIndex: Int,
        durationMinutes: Int,
        blockedSlotIndices: MutableSet<Int>
    ) {
        val neededSlots = slotSpanForDuration(durationMinutes)
        val dayIndex = slots[startIndex].dayIndex
        for (offset in 0 until neededSlots) {
            val idx = startIndex + offset
            if (idx >= slots.size) break
            val slot = slots[idx]
            if (slot.dayIndex != dayIndex) break

            blockedSlotIndices += idx
            slot.assignedMinutes = slot.availableCapacityMinutes
        }
    }

    private fun reserveBreakSlotsAfterTask(
        slots: List<TimeSlot>,
        startIndex: Int,
        taskDurationMinutes: Int,
        breakMinutes: Int,
        blockedSlotIndices: MutableSet<Int>
    ) {
        var remainingBreak = breakMinutes.coerceAtLeast(0)
        if (remainingBreak <= 0) return

        val dayIndex = slots[startIndex].dayIndex
        var idx = startIndex + slotSpanForDuration(taskDurationMinutes)
        while (idx < slots.size && remainingBreak > 0) {
            val slot = slots[idx]
            if (slot.dayIndex != dayIndex) break

            blockedSlotIndices += idx
            slot.reservedBreakMinutes = slot.availableCapacityMinutes
            remainingBreak -= 60
            idx++
        }
    }

    private suspend fun calculateCapacityHorizon(
        nowMillis: Long,
        horizonDays: Int,
        prefs: UserPreferences,
        energyScoreFn: suspend (Long) -> Pair<Int, Float>
    ): CapacityHorizon {
        val slots = mutableListOf<TimeSlot>()
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

        val (dayStartHour, dayEndHourInclusive) = resolveDailyPlanningWindow(prefs)

        // For day 0 (today), start from current hour or next hour, not from dayStartHour
        // This ensures we don't create slots in the past
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var totalMinutes = 0
        var totalEnergy = 0
        var slotCount = 0

        for (dayIdx in 0 until horizonDays) {
            // For today (dayIdx 0), start from current/next hour
            // For future days, start from dayStartHour
            val startHour = if (dayIdx == 0) {
                // Start from next hour to avoid past slots
                (currentHour + 1).coerceIn(dayStartHour, dayEndHourInclusive)
            } else {
                dayStartHour
            }

            for (hour in startHour..dayEndHourInclusive) {
                cal.set(Calendar.HOUR_OF_DAY, hour)
                val slotStartMillis = cal.timeInMillis

                // Skip if slot is in the past (safety check)
                if (slotStartMillis <= nowMillis) {
                    continue
                }

                val slotEndCalendar = Calendar.getInstance().apply { timeInMillis = slotStartMillis }
                slotEndCalendar.add(Calendar.HOUR_OF_DAY, 1)
                val slotEndMillis = slotEndCalendar.timeInMillis

                // Query energy for this hour
                val (energyScore, confidence) = energyScoreFn(slotStartMillis)
                val energyProfile = EnergyProfile.from(energyScore, confidence, 0.0f)

                // Capacity in this slot (hours with high energy get more slots)
                val availableCapacityMinutes = when (energyProfile.zone) {
                    EnergyZone.PEAK -> 60
                    EnergyZone.HIGH -> 50
                    EnergyZone.MODERATE -> 35
                    EnergyZone.LOW -> 15
                    EnergyZone.CRITICAL -> 0
                }

                val slot = TimeSlot(
                    startMillis = slotStartMillis,
                    endMillis = slotEndMillis,
                    dayIndex = dayIdx,
                    hourOfDay = hour,
                    availableCapacityMinutes = availableCapacityMinutes,
                    availableEnergy = energyScore,
                    energyProfile = energyProfile
                )

                slots.add(slot)
                totalMinutes += availableCapacityMinutes
                totalEnergy += energyScore
                slotCount++
            }

            // Move to next day
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, dayStartHour)
        }

        return CapacityHorizon(
            slots = slots,
            horizonStartMillis = slots.firstOrNull()?.startMillis ?: nowMillis,
            horizonEndMillis = slots.lastOrNull()?.endMillis ?: nowMillis,
            totalAvailableMinutes = totalMinutes,
            averageEnergyLevel = if (slotCount > 0) totalEnergy / slotCount else 0
        )
    }

    private fun scoreTaskSlotFit(
        task: TaskEntity,
        slot: TimeSlot,
        slotIndex: Int,
        tagProfiles: List<TagSchedulingProfile>,
        deadlinePressure: DeadlinePressure,
        prefs: UserPreferences,
        nowMillis: Long
    ): TaskSlotFitScore {
        var score = 0.0f

        // 1. Energy match: task energy demand vs slot energy availability
        val taskEnergyDemand = when {
            task.effortScore in 80..100 -> 0.8f
            task.effortScore in 60..79 -> 0.6f
            task.effortScore in 40..59 -> 0.4f
            else -> 0.2f
        }

        val slotEnergySupply = when (slot.energyProfile.zone) {
            EnergyZone.PEAK -> 1.0f
            EnergyZone.HIGH -> 0.8f
            EnergyZone.MODERATE -> 0.6f
            EnergyZone.LOW -> 0.3f
            EnergyZone.CRITICAL -> 0.0f
        }

        val energyMatch = if (taskEnergyDemand > 0.0f) {
            min(1.0f, slotEnergySupply / taskEnergyDemand)
        } else {
            1.0f
        }
        score += energyMatch * 0.20f

        val confidenceReliability = slot.energyProfile.confidence.coerceIn(0.2f, 1.0f)
        score += confidenceReliability * 0.03f

        // 2. Tag-based fit: profile window + context alignment
        var tagFit = 0.5f
        if (tagProfiles.isNotEmpty()) {
            val avgTagSuitability = tagProfiles.map { profile ->
                val windowMatch = when (profile.preferredWindow) {
                    TagPreferredWindow.MORNING -> if (slot.hourOfDay in 6..11) 1.0f else 0.5f
                    TagPreferredWindow.MIDDAY -> if (slot.hourOfDay in 11..14) 1.0f else 0.6f
                    TagPreferredWindow.EVENING -> if (slot.hourOfDay in 17..21) 1.0f else 0.4f
                    TagPreferredWindow.FLEXIBLE -> 0.8f
                }

                val energyDemandMatch = when (profile.energyDemand) {
                    TagEnergyDemand.HIGH -> when (slot.energyProfile.zone) {
                        EnergyZone.PEAK, EnergyZone.HIGH -> 1.0f
                        EnergyZone.MODERATE -> 0.65f
                        EnergyZone.LOW -> 0.35f
                        EnergyZone.CRITICAL -> 0.15f
                    }
                    TagEnergyDemand.MEDIUM -> when (slot.energyProfile.zone) {
                        EnergyZone.CRITICAL -> 0.25f
                        EnergyZone.LOW -> 0.65f
                        else -> 0.95f
                    }
                    TagEnergyDemand.LOW -> when (slot.energyProfile.zone) {
                        EnergyZone.CRITICAL -> 0.55f
                        else -> 1.0f
                    }
                }

                val contextMatch = when {
                    profile.preferredContext.isNullOrBlank() -> 0.85f
                    task.contextTag.isBlank() -> 0.6f
                    profile.preferredContext.equals(task.contextTag, ignoreCase = true) -> 1.0f
                    else -> 0.35f
                }

                (windowMatch * 0.45f) + (energyDemandMatch * 0.35f) + (contextMatch * 0.20f)
            }.average()

            val avgWindowMatch = tagProfiles.map { profile ->
                when (profile.preferredWindow) {
                    TagPreferredWindow.MORNING -> if (slot.hourOfDay in 6..11) 1.0f else 0.5f
                    TagPreferredWindow.MIDDAY -> if (slot.hourOfDay in 11..14) 1.0f else 0.6f
                    TagPreferredWindow.EVENING -> if (slot.hourOfDay in 17..21) 1.0f else 0.4f
                    TagPreferredWindow.FLEXIBLE -> 0.8f
                }
            }.average()
            val avgFragmentation = tagProfiles.map { it.fragmentationTolerance }.average()
            tagFit = (avgTagSuitability.toFloat() * 0.75f) + ((avgWindowMatch.toFloat() * avgFragmentation.toFloat()) * 0.25f)
        }
        score += tagFit * 0.12f

        // 3. Deadline urgency: tasks with near deadlines get higher score
        var deadlineUrgency = 0.0f
        if (task.deadlineDate != null) {
            val deadlineMillis = task.deadlineDate + (task.deadlineTime ?: 0L)
            val daysUntilDeadline = (deadlineMillis - slot.startMillis) / (24 * 60 * 60 * 1000L)
            deadlineUrgency = when {
                daysUntilDeadline <= 1L -> 1.0f
                daysUntilDeadline <= 3L -> 0.75f
                daysUntilDeadline <= 7L -> 0.45f
                else -> 0.25f
            }
        }
        score += deadlineUrgency * 0.16f

        // 4. Deadline placement / spacing: account for pacing density and prevent procrastination.
        val deadlinePlacement = calculateDeadlinePlacementScore(slot.dayIndex, deadlinePressure)
        score += deadlinePlacement * 0.12f

        // 5. Priority / value pressure: higher-impact and explicitly high-priority work should land sooner.
        val priorityPressure = (priorityLevelScore(task) * 0.6f + impactPriorityBlend(task) * 0.4f).coerceIn(0f, 1f)
        score += priorityPressure * 0.12f

        // 6. Temporal proximity: all else equal, choose the nearest viable free slot.
        val proximity = calculateProximityScore(slot.startMillis, nowMillis)
        score += proximity * 0.12f

        // 6.5 Priority timing fit: keep high-priority work from drifting too far out.
        val priorityTimingFit = calculatePriorityTimingFit(task, slot.startMillis, nowMillis)
        score += priorityTimingFit * 0.10f

        // 7. Circadian + task-type fit.
        val chronoFit = calculateChronotypeTaskFit(task, slot, prefs, tagProfiles)
        score += chronoFit * 0.08f

        // 8.5. Respect preferred wake/work windows to reduce burnout from off-hours work.
        val wakeWorkFit = calculateWakeWorkAlignment(slot, prefs)
        score += wakeWorkFit * 0.03f

        // 8. Sleep pressure realism: heavy cognitive tasks are less effective late with high pressure.
        val sleepPressureFit = calculateSleepPressureFit(task, slot, prefs)
        score += sleepPressureFit * 0.06f

        // 9. Duration fit: avoid squeezing long work into fragile windows.
        val durationFit = calculateDurationFit(task, slot)
        score += durationFit * 0.05f

        // 10. Fragmentation fit
        val fragmentationTolerance = if (tagProfiles.isNotEmpty()) {
            tagProfiles.map { it.fragmentationTolerance }.average().toFloat()
        } else {
            0.5f
        }
        score += fragmentationTolerance * 0.03f

        // 11. Hard tasks should prefer true peak windows.
        if (task.effortScore >= 80 && slot.energyProfile.zone == EnergyZone.PEAK) {
            score += 0.05f
        }

        // 6. Micro-adjustments
        if (slot.dayIndex == 0 && slot.hourOfDay in 9..11) {
            score += 0.05f
        }

        return TaskSlotFitScore(
            taskId = task.id,
            slotIndex = slotIndex,
            overallScore = score.coerceIn(0.0f, 1.0f),
            energyMatch = energyMatch,
            tagFit = tagFit,
            deadlineUrgency = deadlineUrgency,
            fragmentationTolerance = fragmentationTolerance
        )
    }

    private fun calculateDeadlinePlacementScore(dayIndex: Int, deadlinePressure: DeadlinePressure): Float {
        return when (deadlinePressure.pressureLevel) {
            PressureLevel.URGENT -> when (dayIndex) {
                0 -> 1.0f
                1 -> 0.5f
                else -> 0.25f
            }
            PressureLevel.HIGH -> when (dayIndex) {
                0 -> 1.0f
                1 -> 0.82f
                2 -> 0.65f
                else -> 0.45f
            }
            PressureLevel.MODERATE -> when (dayIndex) {
                0 -> 1.0f
                1 -> 0.86f
                2 -> 0.72f
                else -> 0.55f
            }
            PressureLevel.LOW -> when (dayIndex) {
                0 -> 1.0f
                1 -> 0.8f
                2 -> 0.62f
                else -> 0.45f
            }
        }
    }

    private fun priorityLevelScore(task: TaskEntity): Float {
        return when (task.priority) {
            com.neuroflow.app.domain.model.Priority.HIGH -> 1.0f
            com.neuroflow.app.domain.model.Priority.MEDIUM -> 0.65f
            com.neuroflow.app.domain.model.Priority.LOW -> 0.35f
        }
    }

    private fun impactPriorityBlend(task: TaskEntity): Float {
        val impactBlend = ((task.impactScore + task.valueScore) / 200f).coerceIn(0f, 1f)
        return ((impactBlend * 0.7f) + (priorityLevelScore(task) * 0.3f)).coerceIn(0f, 1f)
    }

    private fun calculateProximityScore(slotStartMillis: Long, nowMillis: Long): Float {
        val hoursAway = ((slotStartMillis - nowMillis).coerceAtLeast(0L) / 3_600_000f)
        return when {
            hoursAway <= 2f -> 1.0f
            hoursAway <= 6f -> 0.85f
            hoursAway <= 12f -> 0.68f
            hoursAway <= 24f -> 0.48f
            else -> 0.25f
        }
    }

    private fun calculatePriorityTimingFit(task: TaskEntity, slotStartMillis: Long, nowMillis: Long): Float {
        val hoursAway = ((slotStartMillis - nowMillis).coerceAtLeast(0L) / 3_600_000f)
        return when (task.priority) {
            com.neuroflow.app.domain.model.Priority.HIGH -> when {
                hoursAway <= 2f -> 1.0f
                hoursAway <= 6f -> 0.96f
                hoursAway <= 12f -> 0.85f
                hoursAway <= 24f -> 0.7f
                hoursAway <= 36f -> 0.45f
                else -> 0.2f
            }
            com.neuroflow.app.domain.model.Priority.MEDIUM -> when {
                hoursAway <= 4f -> 1.0f
                hoursAway <= 12f -> 0.88f
                hoursAway <= 24f -> 0.72f
                hoursAway <= 48f -> 0.52f
                else -> 0.3f
            }
            com.neuroflow.app.domain.model.Priority.LOW -> when {
                hoursAway <= 6f -> 0.9f
                hoursAway <= 24f -> 0.85f
                hoursAway <= 48f -> 0.75f
                else -> 0.62f
            }
        }
    }

    private fun resolvePreferredLatestStartMillis(
        task: TaskEntity,
        nowMillis: Long,
        deadlineMillis: Long?,
        estimatedDurationMinutes: Int
    ): Long {
        val priorityWindowMillis = when (task.priority) {
            com.neuroflow.app.domain.model.Priority.HIGH -> 24L * 60L * 60L * 1000L
            com.neuroflow.app.domain.model.Priority.MEDIUM -> 36L * 60L * 60L * 1000L
            com.neuroflow.app.domain.model.Priority.LOW -> 60L * 60L * 60L * 1000L
        }

        val preferred = nowMillis + priorityWindowMillis
        if (deadlineMillis == null) {
            return preferred
        }

        val safetyBufferMinutes = calculateDeadlineSafetyBufferMinutes(
            task = task,
            nowMillis = nowMillis,
            deadlineMillis = deadlineMillis,
            estimatedDurationMinutes = estimatedDurationMinutes
        )

        val deadlineBufferedLatestStart = deadlineMillis - (safetyBufferMinutes * 60_000L)
        return min(preferred, deadlineBufferedLatestStart)
    }

    private fun calculateDeadlineSafetyBufferMinutes(
        task: TaskEntity,
        nowMillis: Long,
        deadlineMillis: Long,
        estimatedDurationMinutes: Int
    ): Int {
        val minutesUntilDeadline = ((deadlineMillis - nowMillis) / 60_000L).toInt().coerceAtLeast(0)

        // Base buffer proportional to task duration (not fixed)
        // Longer tasks need more buffer for unexpected issues
        val durationBasedBuffer = (estimatedDurationMinutes * 0.5f).roundToInt()

        // Priority multiplier: HIGH priority gets more buffer for safety
        val priorityMultiplier = when (task.priority) {
            com.neuroflow.app.domain.model.Priority.HIGH -> 1.4f
            com.neuroflow.app.domain.model.Priority.MEDIUM -> 1.2f
            com.neuroflow.app.domain.model.Priority.LOW -> 1.0f
        }

        // Effort multiplier: High-effort tasks need more buffer
        val effortMultiplier = when {
            task.effortScore >= 80 -> 1.3f
            task.effortScore >= 60 -> 1.15f
            else -> 1.0f
        }

        // Historical error multiplier: If task has history of taking longer, add buffer
        val errorMultiplier = if (task.estimationErrorMape != null && task.estimationErrorMape > 0f) {
            1.0f + (task.estimationErrorMape / 200f).coerceIn(0f, 0.5f)
        } else {
            1.0f
        }

        val targetBuffer = (durationBasedBuffer * priorityMultiplier * effortMultiplier * errorMultiplier)
            .roundToInt()
            .coerceIn(30, 180)  // Min 30 min, max 3 hours (reduced from 4)

        // Minimum buffer: at least 25% of task duration
        val minimumBuffer = (estimatedDurationMinutes * 0.25f).roundToInt().coerceAtLeast(15)

        return when {
            minutesUntilDeadline <= minimumBuffer -> (minutesUntilDeadline / 2).coerceAtLeast(15)
            minutesUntilDeadline <= targetBuffer ->
                max(minimumBuffer, (minutesUntilDeadline * 0.4f).roundToInt())
            else -> targetBuffer
        }
    }

    private fun calculateDeadlineSafetyBufferFit(
        task: TaskEntity,
        slotStartMillis: Long,
        deadlineMillis: Long,
        nowMillis: Long
    ): Float {
        val minutesBeforeDeadline = ((deadlineMillis - slotStartMillis) / 60_000L).toInt()
        if (minutesBeforeDeadline <= 0) return 0.0f

        val targetBuffer = calculateDeadlineSafetyBufferMinutes(
            task = task,
            nowMillis = nowMillis,
            deadlineMillis = deadlineMillis,
            estimatedDurationMinutes = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 45
        )

        return when {
            minutesBeforeDeadline < 30 -> 0.2f
            minutesBeforeDeadline < targetBuffer / 2 -> 0.45f
            minutesBeforeDeadline <= targetBuffer + 60 -> 1.0f
            minutesBeforeDeadline <= targetBuffer + 240 -> 0.82f
            else -> 0.65f
        }
    }

    private data class BreakPolicy(
        val intervalMinutes: Int,
        val durationMinutes: Int
    )

    private fun resolveBreakPolicy(prefs: UserPreferences): BreakPolicy {
        val baseInterval = prefs.autoSchedulingBreakAfterCognitiveMinutes.coerceIn(30, 180)
        val baseDuration = prefs.autoSchedulingBreakDurationMinutes.coerceIn(5, 30)

        return when {
            prefs.sleepPressurePoints >= 75 -> BreakPolicy(
                intervalMinutes = min(baseInterval, 75),
                durationMinutes = max(baseDuration, 20)
            )
            prefs.sleepPressurePoints >= 60 -> BreakPolicy(
                intervalMinutes = min(baseInterval, 90),
                durationMinutes = max(baseDuration, 15)
            )
            prefs.sleepPressurePoints <= 30 -> BreakPolicy(
                intervalMinutes = max(baseInterval, 120),
                durationMinutes = max(baseDuration, 10)
            )
            else -> BreakPolicy(baseInterval, baseDuration)
        }
    }

    private fun calculateChronotypeTaskFit(
        task: TaskEntity,
        slot: TimeSlot,
        prefs: UserPreferences,
        tagProfiles: List<TagSchedulingProfile>
    ): Float {
        val peakStart = if (prefs.effectivePeakStart >= 0) prefs.effectivePeakStart else prefs.peakEnergyStart
        val peakEnd = if (prefs.effectivePeakEnd >= 0) prefs.effectivePeakEnd else prefs.peakEnergyEnd
        val inPeakWindow = isHourInWindow(slot.hourOfDay, peakStart, peakEnd)

        val (chronotypeStart, chronotypeEnd) = resolveChronotypePeakWindow(prefs)
        val inChronotypeWindow = isHourInWindow(slot.hourOfDay, chronotypeStart, chronotypeEnd)

        val typeFit = when (task.taskType) {
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> when {
                inPeakWindow && inChronotypeWindow -> 1.0f
                inPeakWindow -> 0.85f
                inChronotypeWindow -> 0.8f
                else -> 0.45f
            }
            com.neuroflow.app.domain.model.TaskType.CREATIVE -> when {
                slot.hourOfDay in 10..16 -> 1.0f
                inChronotypeWindow -> 0.85f
                else -> 0.65f
            }
            com.neuroflow.app.domain.model.TaskType.ADMIN -> if (slot.hourOfDay in 10..18) 1.0f else 0.7f
            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> when {
                slot.energyProfile.zone == EnergyZone.PEAK -> 0.72f
                slot.hourOfDay in 7..20 && slot.energyProfile.zone in listOf(EnergyZone.HIGH, EnergyZone.MODERATE) -> 1.0f
                slot.hourOfDay in 7..20 -> 0.82f
                else -> 0.6f
            }
        }

        val explicitEnergyFit = when (task.energyLevel) {
            com.neuroflow.app.domain.model.EnergyLevel.HIGH -> if (slot.energyProfile.zone in listOf(EnergyZone.PEAK, EnergyZone.HIGH)) 1.0f else 0.4f
            com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> if (slot.energyProfile.zone == EnergyZone.CRITICAL) 0.4f else 0.9f
            com.neuroflow.app.domain.model.EnergyLevel.LOW -> if (slot.energyProfile.zone == EnergyZone.CRITICAL) 0.6f else 1.0f
        }

        val tagWindowFit = if (tagProfiles.isEmpty()) 0.85f else {
            tagProfiles.map { profile ->
                when (profile.preferredWindow) {
                    TagPreferredWindow.MORNING -> if (slot.hourOfDay in 6..11) 1.0f else 0.55f
                    TagPreferredWindow.MIDDAY -> if (slot.hourOfDay in 11..14) 1.0f else 0.65f
                    TagPreferredWindow.EVENING -> if (slot.hourOfDay in 17..21) 1.0f else 0.5f
                    TagPreferredWindow.FLEXIBLE -> 0.9f
                }
            }.average().toFloat()
        }

        return ((typeFit * 0.45f) + (explicitEnergyFit * 0.35f) + (tagWindowFit * 0.2f)).coerceIn(0f, 1f)
    }

    private fun calculateWakeWorkAlignment(slot: TimeSlot, prefs: UserPreferences): Float {
        val wakeBufferedStart = (prefs.wakeUpHour + 1).coerceIn(0, 23)
        val sleepGuardEnd = (prefs.sleepHour - 1).coerceIn(0, 23)
        val workStart = prefs.workDayStart.coerceIn(0, 23)
        val workEndInclusive = (prefs.workDayEnd - 1).coerceIn(0, 23)

        val withinWakeWindow = isHourInWindow(slot.hourOfDay, wakeBufferedStart, sleepGuardEnd + 1)
        val withinWorkWindow = isHourInWindow(slot.hourOfDay, workStart, workEndInclusive + 1)

        return when {
            withinWakeWindow && withinWorkWindow -> 1.0f
            withinWakeWindow -> 0.75f
            else -> 0.35f
        }
    }

    private fun resolveChronotypePeakWindow(prefs: UserPreferences): Pair<Int, Int> {
        val mapped = when (prefs.quizChronotype ?: prefs.manualChronotype) {
            "DEFINITE_MORNING" -> 6 to 11
            "MODERATE_MORNING" -> 7 to 12
            "INTERMEDIATE" -> 9 to 14
            "MODERATE_EVENING" -> 13 to 18
            "DEFINITE_EVENING" -> 15 to 21
            else -> null
        }

        val (start, end) = mapped
            ?: if (prefs.effectivePeakStart >= 0 && prefs.effectivePeakEnd >= 0) {
                prefs.effectivePeakStart to prefs.effectivePeakEnd
            } else {
                prefs.peakEnergyStart to prefs.peakEnergyEnd
            }

        val safeStart = start.coerceIn(0, 23)
        val safeEnd = end.coerceIn(1, 24)
        return if (safeEnd <= safeStart) {
            safeStart to (safeStart + 4).coerceAtMost(24)
        } else {
            safeStart to safeEnd
        }
    }

    private fun isHourInWindow(hour: Int, startInclusive: Int, endExclusive: Int): Boolean {
        val normalizedHour = hour.coerceIn(0, 23)
        val start = startInclusive.coerceIn(0, 23)
        val end = endExclusive.coerceIn(0, 24)
        if (start == end) return true
        return if (start < end) {
            normalizedHour in start until end
        } else {
            normalizedHour >= start || normalizedHour < end
        }
    }

    private fun resolveDailyPlanningWindow(prefs: UserPreferences): Pair<Int, Int> {
        val wakeBufferedStart = (prefs.wakeUpHour + 1).coerceIn(0, 23)
        val workStart = prefs.workDayStart.coerceIn(0, 23)
        val workEndInclusive = (prefs.workDayEnd - 1).coerceIn(0, 23)
        val sleepGuardEnd = (prefs.sleepHour - 1).coerceIn(0, 23)

        var start = max(workStart, wakeBufferedStart)
        var end = min(workEndInclusive, sleepGuardEnd)

        if (end < start || (end - start) < 4) {
            start = max(6, wakeBufferedStart)
            end = min(22, max(start + 4, sleepGuardEnd))
            if (end < start) {
                start = 7
                end = 21
            }
        }

        return start.coerceIn(0, 23) to end.coerceIn(start.coerceIn(0, 23), 23)
    }

    private fun canAssignWithoutBurnout(
        task: TaskEntity,
        slot: TimeSlot,
        durationMinutes: Int,
        nowMillis: Long,
        prefs: UserPreferences,
        highCognitiveMinutesByDay: MutableMap<Int, Int>,
        highCognitiveHoursByDay: MutableMap<Int, MutableSet<Int>>
    ): Boolean {
        if (!isCognitivelyIntense(task)) return true

        val deadlineMillis = task.deadlineDate?.plus(task.deadlineTime ?: 0L)
        val urgentDeadline = deadlineMillis != null && (deadlineMillis - nowMillis) <= (24 * 60 * 60 * 1000L)

        val baseBudget = calculateDailyCognitiveBudgetMinutes(prefs)
        val budget = if (urgentDeadline) (baseBudget * 1.2f).roundToInt() else baseBudget
        val used = highCognitiveMinutesByDay[slot.dayIndex] ?: 0
        if (used + durationMinutes > budget) {
            return false
        }

        val sameDayHighHours = highCognitiveHoursByDay[slot.dayIndex].orEmpty()
        val adjacentHeavy = (slot.hourOfDay - 1 in sameDayHighHours) || (slot.hourOfDay + 1 in sameDayHighHours)
        if (!urgentDeadline && adjacentHeavy) {
            return false
        }

        val nearBedtime = slot.hourOfDay >= (prefs.sleepHour - 2).coerceAtLeast(18)
        if (!urgentDeadline && prefs.sleepPressurePoints >= 70 && nearBedtime) {
            return false
        }

        return true
    }

    private fun trackCognitiveLoad(
        task: TaskEntity,
        slot: TimeSlot,
        durationMinutes: Int,
        highCognitiveMinutesByDay: MutableMap<Int, Int>,
        highCognitiveHoursByDay: MutableMap<Int, MutableSet<Int>>
    ) {
        if (!isCognitivelyIntense(task)) return
        highCognitiveMinutesByDay[slot.dayIndex] =
            (highCognitiveMinutesByDay[slot.dayIndex] ?: 0) + durationMinutes
        highCognitiveHoursByDay.getOrPut(slot.dayIndex) { mutableSetOf() }.add(slot.hourOfDay)
    }

    private fun isCognitivelyIntense(task: TaskEntity): Boolean {
        return task.effortScore >= 70 || task.taskType in listOf(
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL,
            com.neuroflow.app.domain.model.TaskType.CREATIVE
        )
    }

    private fun calculateDailyCognitiveBudgetMinutes(prefs: UserPreferences): Int {
        val base = when {
            prefs.sleepPressurePoints >= 80 -> 75
            prefs.sleepPressurePoints >= 65 -> 100
            prefs.sleepPressurePoints >= 50 -> 130
            else -> 170
        }

        val awakeHours = ((prefs.sleepHour - prefs.wakeUpHour + 24) % 24).coerceAtLeast(1)
        val awakeAdjustment = when {
            awakeHours <= 13 -> 0.75f
            awakeHours <= 15 -> 0.9f
            awakeHours >= 18 -> 1.1f
            else -> 1.0f
        }

        return (base * awakeAdjustment).roundToInt().coerceAtLeast(60)
    }

    private fun calculateSleepPressureFit(task: TaskEntity, slot: TimeSlot, prefs: UserPreferences): Float {
        val pressure = prefs.sleepPressurePoints.coerceIn(0, 100)
        val lateEvening = slot.hourOfDay >= (prefs.sleepHour - 2).coerceAtLeast(18)
        if (!lateEvening || pressure < 40) return 1.0f

        val cognitivelyHeavy = task.effortScore >= 70 || task.taskType == com.neuroflow.app.domain.model.TaskType.ANALYTICAL
        val penalty = when {
            pressure >= 80 && cognitivelyHeavy -> 0.35f
            pressure >= 65 && cognitivelyHeavy -> 0.55f
            pressure >= 80 -> 0.65f
            else -> 0.8f
        }
        return penalty
    }

    private fun calculateDurationFit(task: TaskEntity, slot: TimeSlot): Float {
        val estimate = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 30
        val capacity = slot.availableCapacityMinutes.coerceAtLeast(1)
        val ratio = estimate.toFloat() / capacity.toFloat()
        return when {
            ratio <= 0.75f -> 1.0f
            ratio <= 1.0f -> 0.8f
            ratio <= 1.35f -> 0.55f
            else -> 0.3f
        }
    }
    private fun buildAssignmentReason(fitScore: TaskSlotFitScore): String {
        return when {
            fitScore.deadlineUrgency > 0.8f -> "deadline urgency (${(fitScore.deadlineUrgency * 100).roundToInt()}%)"
            fitScore.energyMatch > 0.8f -> "strong energy-demand match"
            fitScore.tagFit > 0.75f -> "optimal tag-window alignment"
            else -> "composite fit score: ${(fitScore.overallScore * 100).roundToInt()}%"
        }
    }

    /**
     * Check if task has any incomplete dependencies that would block scheduling.
     * Returns true if task can be scheduled (no blockers), false if blocked.
     */
    private fun hasNoDependencyBlockers(task: TaskEntity, allTasks: List<TaskEntity>): Boolean {
        if (task.dependsOnTaskIds.isBlank()) return true

        val dependencies = task.dependsOnTaskIds.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (dependencies.isEmpty()) return true

        // Check if all dependencies are completed
        val taskMap = allTasks.associateBy { it.id }
        return dependencies.all { depId ->
            val depTask = taskMap[depId]
            depTask == null || depTask.status == TaskStatus.COMPLETED
        }
    }
}
