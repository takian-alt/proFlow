package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.BlueprintRecoveryBuffer
import com.neuroflow.app.domain.scheduler.BlueprintTaskItem
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.TimeSlotWindow
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Unified, non-enforcing Burnout & Recovery Engine.
 *
 * Single source of truth consumed by:
 * 1. Flow Spotlight Hero Card (co-equal Next Task + Rest companion)
 * 2. Persistent 25-minute "Have you rested?" assistant check-in loop
 * 3. Task Creation Auto-Slot Recommender (recovery buffers & heavy-streak breaking)
 * 4. Daily Auto-Plan Blueprint Optimizer (heavy-streak interleaving & recovery buffer insertion)
 */
object BurnoutGuardEngine {

    const val REST_CHECK_IN_FOLLOW_UP_MINUTES = 25
    const val DEFAULT_RECOVERY_BUFFER_MINUTES = 15
    const val HIGH_BURNOUT_RECOVERY_BUFFER_MINUTES = 20
    const val HEAVY_STREAK_CEILING_MINUTES = 75
    private const val QUALIFYING_REST_MINUTES = 10

    data class BurnoutPlanOptimizationResult(
        val items: List<BlueprintTaskItem>,
        val recoveryBuffers: List<BlueprintRecoveryBuffer>,
        val maxHeavyStreakMinutes: Int,
        val burnoutPacingSummary: String
    )

    private data class WorkBout(
        val startMillis: Long,
        val endMillis: Long,
        val rawMinutes: Int,
        val weightedMinutes: Double,
        val isHeavy: Boolean
    )

    fun isHeavyTask(task: TaskEntity): Boolean {
        if (task.energyLevel == EnergyLevel.LOW && !task.isFrog) return false
        val planned = TaskBlockPlan.plannedMinutes(task)
        return task.energyLevel == EnergyLevel.HIGH ||
            task.isFrog ||
            (task.energyLevel == EnergyLevel.MEDIUM && (planned >= 45 || (task.taskType == TaskType.ANALYTICAL && planned >= 35)))
    }

    /**
     * Collects all recovery events today: explicit RestBreakLogs plus logged daytime Naps.
     */
    fun collectTodayRestLogs(
        prefs: UserPreferences,
        dayStartMillis: Long,
        nowMillis: Long
    ): List<RestBreakLog> {
        val explicit = SmartAssistantCodecs.parseRestBreakLogs(prefs.restBreakLogsJson)
            .filter { it.completedAtMillis in dayStartMillis..(nowMillis + 5 * 60_000L) }
        val napRests = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.type == PhysiologicalEventType.NAP && it.timestampMillis in dayStartMillis..(nowMillis + 5 * 60_000L) }
            .map { nap ->
                val dur = nap.durationMinutes.coerceIn(10, 120)
                RestBreakLog(
                    id = "nap_${nap.timestampMillis}",
                    completedAtMillis = nap.timestampMillis + dur * 60_000L,
                    durationMinutes = dur,
                    source = RestBreakSource.MANUAL_CONFIRM
                )
            }
        return (explicit + napRests).sortedBy { it.completedAtMillis }
    }

    /**
     * Converts a rest break duration into discharged cognitive load minutes.
     */
    fun restDischargeMinutes(durationMinutes: Int): Double {
        val safe = durationMinutes.coerceIn(1, 180)
        return min(180.0, safe * 4.9)
    }

    /**
     * Evaluates current real-time burnout risk from today's focus sessions, completed tasks,
     * biological circadian snapshot, and explicit logged rest breaks.
     */
    fun evaluateBurnoutRisk(
        prefs: UserPreferences,
        sessions: List<TimeSessionEntity>,
        completedTasks: List<TaskEntity>,
        activeTasks: List<TaskEntity>,
        snapshot: CircadianSnapshot,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BurnoutRiskState {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val dayStartMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEndMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

        val restLogsToday = collectTodayRestLogs(prefs, dayStartMillis, nowMillis)
        val lastRest = restLogsToday.maxByOrNull { it.completedAtMillis }
        val lastQualifyingRest = restLogsToday
            .filter { it.durationMinutes >= QUALIFYING_REST_MINUTES }
            .maxByOrNull { it.completedAtMillis }

        val taskLookup = (activeTasks + completedTasks).associateBy { it.id }

        // Build chronological work bouts from actual sessions + completed tasks without sessions
        val daySessions = sessions
            .filter { it.startedAt in dayStartMillis..nowMillis }
            .sortedBy { it.startedAt }
        val sessionTaskIds = daySessions.map { it.taskId }.toSet()

        val bouts = mutableListOf<WorkBout>()
        daySessions.forEach { session ->
            val rawMins = session.trackedMinutes(nowMillis).roundToInt()
            if (rawMins <= 0) return@forEach
            val end = (session.endedAt ?: nowMillis).coerceAtMost(nowMillis)
            val linked = taskLookup[session.taskId]
            val weight = when {
                linked == null -> 1.05
                linked.energyLevel == EnergyLevel.HIGH || linked.isFrog -> 1.25
                linked.energyLevel == EnergyLevel.LOW -> 0.65
                else -> 1.0
            }
            val heavy = linked?.let { isHeavyTask(it) } ?: (rawMins >= 35)
            bouts.add(
                WorkBout(
                    startMillis = session.startedAt,
                    endMillis = max(end, session.startedAt + rawMins * 60_000L),
                    rawMinutes = rawMins,
                    weightedMinutes = rawMins * weight,
                    isHeavy = heavy
                )
            )
        }

        completedTasks.forEach { task ->
            if (task.status != TaskStatus.COMPLETED || task.id in sessionTaskIds) return@forEach
            val completedAt = task.completedAt ?: task.updatedAt
            if (completedAt !in dayStartMillis..nowMillis) return@forEach
            val rawMins = (task.actualDurationMinutes?.roundToInt()
                ?: task.totalTimeTrackedMinutes.roundToInt().takeIf { it > 0 }
                ?: TaskBlockPlan.plannedMinutes(task)).coerceIn(5, 240)
            val start = (completedAt - rawMins * 60_000L).coerceAtLeast(dayStartMillis)
            val weight = when {
                task.energyLevel == EnergyLevel.HIGH || task.isFrog -> 1.25
                task.energyLevel == EnergyLevel.LOW -> 0.65
                else -> 1.0
            }
            bouts.add(
                WorkBout(
                    startMillis = start,
                    endMillis = completedAt,
                    rawMinutes = rawMins,
                    weightedMinutes = rawMins * weight,
                    isHeavy = isHeavyTask(task)
                )
            )
        }

        val sortedBouts = bouts.sortedBy { it.startMillis }

        // Compute unbroken load since the last qualifying rest, applying passive gap decay and rest discharge
        var unbrokenWeightedLoad = 0.0
        var unbrokenRawMinutes = 0
        var consecutiveHeavyCount = 0
        var lastBoutEnd = 0L

        for (bout in sortedBouts) {
            // Check if a logged rest occurred between lastBoutEnd and this bout
            if (lastBoutEnd > 0L) {
                val interveningRests = restLogsToday.filter {
                    it.completedAtMillis in lastBoutEnd..bout.startMillis
                }
                interveningRests.forEach { rest ->
                    val discharged = restDischargeMinutes(rest.durationMinutes)
                    unbrokenWeightedLoad = (unbrokenWeightedLoad - discharged).coerceAtLeast(0.0)
                    if (rest.durationMinutes >= QUALIFYING_REST_MINUTES) {
                        val retention = (1.0 - (rest.durationMinutes / 18.0).coerceIn(0.55, 0.92))
                        unbrokenWeightedLoad *= retention
                        unbrokenRawMinutes = (unbrokenRawMinutes * retention).roundToInt()
                        consecutiveHeavyCount = 0
                    } else {
                        unbrokenRawMinutes = (unbrokenRawMinutes - rest.durationMinutes * 3).coerceAtLeast(0)
                    }
                }
                val passiveGapMinutes = ((bout.startMillis - lastBoutEnd) / 60_000L).coerceAtLeast(0L)
                if (passiveGapMinutes >= 45L && interveningRests.isEmpty()) {
                    // Long natural idle gap partially decays strain
                    unbrokenWeightedLoad *= 0.55
                    unbrokenRawMinutes = (unbrokenRawMinutes * 0.6).roundToInt()
                    consecutiveHeavyCount = 0
                }
            }
            unbrokenWeightedLoad += bout.weightedMinutes
            unbrokenRawMinutes += bout.rawMinutes
            if (bout.isHeavy) {
                consecutiveHeavyCount++
            } else if (bout.rawMinutes >= 20) {
                consecutiveHeavyCount = max(0, consecutiveHeavyCount - 1)
            }
            lastBoutEnd = max(lastBoutEnd, bout.endMillis)
        }

        // Apply any logged rests that happened AFTER the last work bout up to nowMillis
        val trailingRests = restLogsToday.filter { it.completedAtMillis > lastBoutEnd && it.completedAtMillis <= nowMillis + 60_000L }
        trailingRests.forEach { rest ->
            val discharged = restDischargeMinutes(rest.durationMinutes)
            unbrokenWeightedLoad = (unbrokenWeightedLoad - discharged).coerceAtLeast(0.0)
            if (rest.durationMinutes >= QUALIFYING_REST_MINUTES) {
                val retention = (1.0 - (rest.durationMinutes / 18.0).coerceIn(0.60, 0.92))
                unbrokenWeightedLoad *= retention
                unbrokenRawMinutes = (unbrokenRawMinutes * retention).roundToInt()
                consecutiveHeavyCount = 0
            } else {
                unbrokenRawMinutes = (unbrokenRawMinutes - rest.durationMinutes * 3).coerceAtLeast(0)
            }
        }

        // Passive decay if user has been completely idle for > 40m since last work bout
        if (lastBoutEnd > 0L && nowMillis > lastBoutEnd) {
            val idleMinutes = ((nowMillis - lastBoutEnd) / 60_000L).toDouble()
            if (idleMinutes >= 40.0) {
                val decayFactor = (1.0 - ((idleMinutes - 30.0) / 120.0)).coerceIn(0.35, 1.0)
                unbrokenWeightedLoad *= decayFactor
            }
        }

        val minutesSinceLastRest = lastRest?.let {
            ((nowMillis - it.completedAtMillis) / 60_000L).toInt().coerceAtLeast(0)
        }
        val inRecentRestGrace = lastRest != null &&
            minutesSinceLastRest != null &&
            minutesSinceLastRest < max(15, lastRest.durationMinutes * 2) &&
            unbrokenRawMinutes < 30

        // Biological & circadian fatigue components
        val usableEnergy = snapshot.usableEnergy.coerceIn(5, 100)
        val energyStrain = when {
            usableEnergy <= 25 -> 0.58f
            usableEnergy <= 35 -> 0.42f
            usableEnergy <= 45 -> 0.24f
            usableEnergy <= 55 -> 0.10f
            else -> 0f
        }
        val phaseStrain = when (snapshot.phase) {
            EnergyPhase.CRASH, EnergyPhase.RECOVERY_NEEDED -> 0.28f
            EnergyPhase.DIP -> 0.08f
            else -> 0f
        }
        val loadStrain = (unbrokenWeightedLoad / 105.0).toFloat().coerceIn(0f, 0.85f)
        val streakStrain = if (consecutiveHeavyCount >= 2) 0.14f else 0f

        var rawRiskScore = (loadStrain * 0.72f + energyStrain * 0.65f + phaseStrain + streakStrain)
            .coerceIn(0f, 1f)

        if (inRecentRestGrace) {
            rawRiskScore = (rawRiskScore * 0.35f).coerceAtMost(0.32f)
        }

        val effectiveLoadMinutes = unbrokenWeightedLoad.roundToInt().coerceAtLeast(0)

        val riskLevel = when {
            inRecentRestGrace -> BurnoutRiskLevel.CALM
            rawRiskScore >= 0.68f ||
                effectiveLoadMinutes >= 90 ||
                (usableEnergy <= 26 && effectiveLoadMinutes >= 25) -> BurnoutRiskLevel.HIGH
            rawRiskScore >= 0.42f ||
                effectiveLoadMinutes >= 55 ||
                (usableEnergy <= 38 && effectiveLoadMinutes >= 20) ||
                ((snapshot.phase == EnergyPhase.CRASH || snapshot.phase == EnergyPhase.RECOVERY_NEEDED) && lastQualifyingRest == null) ->
                BurnoutRiskLevel.APPROACHING
            else -> BurnoutRiskLevel.CALM
        }

        val recommendedRestMinutes = when {
            riskLevel == BurnoutRiskLevel.HIGH && (effectiveLoadMinutes >= 105 || usableEnergy <= 28) ->
                HIGH_BURNOUT_RECOVERY_BUFFER_MINUTES
            riskLevel == BurnoutRiskLevel.HIGH -> DEFAULT_RECOVERY_BUFFER_MINUTES
            riskLevel == BurnoutRiskLevel.APPROACHING -> if (effectiveLoadMinutes >= 70) 15 else 10
            else -> 10
        }

        val reasonSummary = when (riskLevel) {
            BurnoutRiskLevel.HIGH -> when {
                effectiveLoadMinutes >= 75 ->
                    "${effectiveLoadMinutes}m unbroken load without rest · ${recommendedRestMinutes}m recharge recommended"
                usableEnergy <= 32 ->
                    "Energy dipped to $usableEnergy% · ${recommendedRestMinutes}m rest helps prevent burnout"
                else ->
                    "High cognitive strain building · ${recommendedRestMinutes}m break recommended"
            }
            BurnoutRiskLevel.APPROACHING -> when {
                effectiveLoadMinutes >= 50 ->
                    "${effectiveLoadMinutes}m heavy focus since last break · ${recommendedRestMinutes}m breather advised"
                snapshot.phase == EnergyPhase.CRASH || snapshot.phase == EnergyPhase.RECOVERY_NEEDED ->
                    "Circadian dip ($usableEnergy%) · a quick ${recommendedRestMinutes}m reset will restore focus"
                else ->
                    "Burnout load rising · consider a ${recommendedRestMinutes}m reset alongside your next task"
            }
            BurnoutRiskLevel.CALM -> if (lastRest != null && minutesSinceLastRest != null && minutesSinceLastRest <= 45) {
                "Rested ${minutesSinceLastRest}m ago (${lastRest.durationMinutes}m) · Ready for deep work"
            } else {
                "Energy balanced · sustainable focus pace"
            }
        }

        return BurnoutRiskState(
            riskScore = rawRiskScore,
            riskLevel = riskLevel,
            unbrokenLoadMinutes = effectiveLoadMinutes,
            consecutiveHeavyTasks = consecutiveHeavyCount,
            minutesSinceLastRest = minutesSinceLastRest,
            lastRestAtMillis = lastRest?.completedAtMillis ?: 0L,
            lastRestDurationMinutes = lastRest?.durationMinutes ?: 0,
            recommendedRestMinutes = recommendedRestMinutes,
            reasonSummary = reasonSummary,
            usableEnergy = usableEnergy
        )
    }

    /**
     * Evaluates the persistent "Have you rested?" assistant check-in loop.
     * Re-prompts every [REST_CHECK_IN_FOLLOW_UP_MINUTES] (25 min) while burnout risk stays elevated
     * and no rest has been logged. Never interrupts an active focus timer or active break timer.
     */
    fun evaluatePersistentRestCheckIn(
        prefs: UserPreferences,
        riskState: BurnoutRiskState,
        nowMillis: Long,
        isFocusSessionActive: Boolean,
        isBreakTimerActive: Boolean = false
    ): RestCheckInProposal? {
        if (!prefs.wellnessNudgesEnabled || isFocusSessionActive || isBreakTimerActive) return null
        if (!riskState.isElevated) return null

        // Respect the 25-minute "Not yet" follow-up window
        if (prefs.nextRestCheckInAllowedAtMillis > nowMillis) {
            return null
        }

        // If user just logged a rest very recently, wait until grace expires
        if (riskState.lastRestAtMillis > 0L) {
            val minsSinceRest = ((nowMillis - riskState.lastRestAtMillis) / 60_000L).toInt()
            val graceMinutes = max(15, riskState.lastRestDurationMinutes * 2)
            if (minsSinceRest in 0 until graceMinutes && riskState.unbrokenLoadMinutes < 30) {
                return null
            }
        }

        val title = if (riskState.riskLevel == BurnoutRiskLevel.HIGH) {
            "Burnout near — have you rested?"
        } else {
            "Have you rested?"
        }

        val message = when {
            riskState.unbrokenLoadMinutes >= 60 ->
                "You've built up ${riskState.unbrokenLoadMinutes}m of focus load without a logged break. Log a quick rest or start a ${riskState.recommendedRestMinutes}m break timer — zero pressure."
            riskState.usableEnergy <= 38 ->
                "Your energy is at ${riskState.usableEnergy}%. Have you stepped away to recharge? Logging rest keeps your schedule burnout-safe."
            else ->
                "${riskState.reasonSummary}. Let me know if you've rested, or start a quick timer."
        }

        return RestCheckInProposal(
            riskState = riskState,
            title = title,
            message = message,
            recommendedRestMinutes = riskState.recommendedRestMinutes,
            followUpMinutes = REST_CHECK_IN_FOLLOW_UP_MINUTES
        )
    }

    /**
     * Proactive burnout-prevention optimizer for Auto-Plan (`DailyBlueprint`).
     *
     * 1. Interleaves lighter (LOW energy) tasks to break up back-to-back heavy streaks whenever possible.
     * 2. Inserts explicit 15–20 minute Recovery Buffers after sustained heavy stretches (>= 60–75 min)
     *    and before the first heavy block when burnout risk is already elevated today.
     * 3. Never creates overlaps with external calendar / locked busy windows and never deletes user tasks.
     */
    fun optimizeBlueprintForBurnout(
        items: List<BlueprintTaskItem>,
        busyWindows: List<TimeSlotWindow>,
        dayEndMillis: Long,
        nowMillis: Long,
        isToday: Boolean,
        burnoutRisk: BurnoutRiskState,
        breakDurationMinutes: Int = DEFAULT_RECOVERY_BUFFER_MINUTES
    ): BurnoutPlanOptimizationResult {
        if (items.isEmpty()) {
            return BurnoutPlanOptimizationResult(
                items = emptyList(),
                recoveryBuffers = emptyList(),
                maxHeavyStreakMinutes = 0,
                burnoutPacingSummary = ""
            )
        }

        val safeBufferMins = breakDurationMinutes.coerceIn(10, 30)
        val working = items.sortedBy { it.proposedStartMillis }.toMutableList()

        // Pass 1: Break up back-to-back heavy streaks by interleaving a lighter LOW-energy task when safe
        var idx = 0
        var runningHeavyMins = 0
        while (idx < working.size) {
            val current = working[idx]
            val durMins = ((current.proposedEndMillis - current.proposedStartMillis) / 60_000L).toInt().coerceAtLeast(5)
            val currentHeavy = isHeavyTask(current.task)

            if (currentHeavy) {
                val prevGapMins = if (idx > 0) {
                    ((current.proposedStartMillis - working[idx - 1].proposedEndMillis) / 60_000L).toInt().coerceAtLeast(0)
                } else 999
                if (prevGapMins >= safeBufferMins) {
                    runningHeavyMins = durMins
                } else {
                    runningHeavyMins += durMins
                }

                // If stacking this heavy task exceeds the safe heavy ceiling, try swapping with a later light task
                if (idx > 0 && runningHeavyMins > HEAVY_STREAK_CEILING_MINUTES && !current.task.isScheduleLocked) {
                    val lightIdx = ((idx + 1) until working.size).firstOrNull { j ->
                        val cand = working[j]
                        !cand.task.isScheduleLocked &&
                            !isHeavyTask(cand.task) &&
                            cand.task.energyLevel == EnergyLevel.LOW
                    }
                    if (lightIdx != null) {
                        val lightItem = working[lightIdx]
                        val heavyDurMs = current.proposedEndMillis - current.proposedStartMillis
                        val lightDurMs = lightItem.proposedEndMillis - lightItem.proposedStartMillis
                        val newLightStart = current.proposedStartMillis
                        val newLightEnd = newLightStart + lightDurMs
                        val newHeavyStart = lightItem.proposedStartMillis
                        val newHeavyEnd = newHeavyStart + heavyDurMs

                        val heavyDeadline = current.task.resolveDeadlineEpochMillis()
                        val deadlineOk = heavyDeadline == null || newHeavyEnd <= heavyDeadline
                        val nextAfterIdxStart = working.getOrNull(idx + 1)?.proposedStartMillis ?: dayEndMillis
                        val nextAfterLightStart = working.getOrNull(lightIdx + 1)?.proposedStartMillis ?: dayEndMillis

                        val lightFits = newLightEnd <= nextAfterIdxStart &&
                            busyWindows.none { newLightStart < it.endMillis && newLightEnd > it.startMillis }
                        val heavyFits = newHeavyEnd <= min(nextAfterLightStart, dayEndMillis) &&
                            deadlineOk &&
                            busyWindows.none { newHeavyStart < it.endMillis && newHeavyEnd > it.startMillis }

                        if (lightFits && heavyFits) {
                            working[idx] = lightItem.copy(
                                proposedStartMillis = newLightStart,
                                proposedEndMillis = newLightEnd
                            )
                            working[lightIdx] = current.copy(
                                proposedStartMillis = newHeavyStart,
                                proposedEndMillis = newHeavyEnd
                            )
                            runningHeavyMins = 0
                        }
                    }
                }
            } else {
                runningHeavyMins = 0
            }
            idx++
        }

        working.sortBy { it.proposedStartMillis }

        val recoveryBuffers = mutableListOf<BlueprintRecoveryBuffer>()

        // Helper to check whether shifting items[startIndex..] forward by shiftMs stays collision-free
        fun canShiftFrom(startIndex: Int, shiftMs: Long): Boolean {
            if (shiftMs <= 0L) return true
            for (i in startIndex until working.size) {
                val item = working[i]
                if (item.task.isScheduleLocked) {
                    // Check if previous shifted item would collide with this locked item
                    if (i > startIndex) {
                        val prevShiftedEnd = working[i - 1].proposedEndMillis + shiftMs
                        if (prevShiftedEnd > item.proposedStartMillis) return false
                    } else {
                        return false
                    }
                    break
                }
                val newStart = item.proposedStartMillis + shiftMs
                val newEnd = item.proposedEndMillis + shiftMs
                if (newEnd > dayEndMillis) return false
                val deadline = item.task.resolveDeadlineEpochMillis()
                if (deadline != null && newEnd > deadline) return false
                if (busyWindows.any { newStart < it.endMillis && newEnd > it.startMillis }) return false
            }
            return true
        }

        fun applyShiftFrom(startIndex: Int, shiftMs: Long) {
            if (shiftMs <= 0L) return
            for (i in startIndex until working.size) {
                val item = working[i]
                if (item.task.isScheduleLocked) break
                working[i] = item.copy(
                    proposedStartMillis = item.proposedStartMillis + shiftMs,
                    proposedEndMillis = item.proposedEndMillis + shiftMs
                )
            }
        }

        // Pass 2: Opening recovery buffer if user is already near burnout right now
        if (isToday && burnoutRisk.isElevated && working.isNotEmpty()) {
            val first = working.first()
            if (isHeavyTask(first.task) && !first.task.isScheduleLocked) {
                val gapFromNowMins = ((first.proposedStartMillis - nowMillis) / 60_000L).toInt()
                val desiredRestMins = burnoutRisk.recommendedRestMinutes.coerceIn(10, 20)
                if (gapFromNowMins in -5 until desiredRestMins) {
                    val shiftMins = (desiredRestMins - max(0, gapFromNowMins)).coerceAtLeast(5)
                    val shiftMs = shiftMins * 60_000L
                    if (canShiftFrom(0, shiftMs)) {
                        val bufStart = max(nowMillis, first.proposedStartMillis - max(0, gapFromNowMins) * 60_000L)
                        applyShiftFrom(0, shiftMs)
                        val bufEnd = working.first().proposedStartMillis
                        val actualBufMins = ((bufEnd - bufStart) / 60_000L).toInt().coerceAtLeast(desiredRestMins)
                        recoveryBuffers.add(
                            BlueprintRecoveryBuffer(
                                id = "recovery_opening_$bufStart",
                                startMillis = bufStart,
                                endMillis = bufEnd,
                                durationMinutes = actualBufMins,
                                label = "Recovery Buffer (${actualBufMins}m)",
                                reason = "Pre-focus recharge · ${burnoutRisk.reasonSummary}"
                            )
                        )
                    }
                }
            }
        }

        // Pass 3: Enforce 15–20m recovery buffers between sustained heavy streaks (>= 60m)
        var streakHeavyMins = 0
        var maxObservedHeavyStreak = 0

        for (i in working.indices) {
            val item = working[i]
            val durMins = ((item.proposedEndMillis - item.proposedStartMillis) / 60_000L).toInt().coerceAtLeast(5)
            val heavy = isHeavyTask(item.task)

            if (i > 0) {
                val prev = working[i - 1]
                val gapMins = ((item.proposedStartMillis - prev.proposedEndMillis) / 60_000L).toInt().coerceAtLeast(0)
                val needsRecovery = heavy && streakHeavyMins >= 60

                if (needsRecovery && gapMins < safeBufferMins && !item.task.isScheduleLocked) {
                    val neededShiftMins = safeBufferMins - gapMins
                    val shiftMs = neededShiftMins * 60_000L
                    if (canShiftFrom(i, shiftMs)) {
                        applyShiftFrom(i, shiftMs)
                        val updatedItem = working[i]
                        val bufStart = prev.proposedEndMillis
                        val bufEnd = updatedItem.proposedStartMillis
                        val actualMins = ((bufEnd - bufStart) / 60_000L).toInt().coerceAtLeast(safeBufferMins)
                        recoveryBuffers.add(
                            BlueprintRecoveryBuffer(
                                id = "recovery_${i}_$bufStart",
                                startMillis = bufStart,
                                endMillis = bufEnd,
                                durationMinutes = actualMins,
                                label = "Recovery Buffer (${actualMins}m)",
                                reason = "Breaks up ${streakHeavyMins}m heavy streak to prevent burnout"
                            )
                        )
                        streakHeavyMins = 0
                    }
                } else if (streakHeavyMins >= 60 && gapMins in safeBufferMins..45) {
                    // Surface natural recovery gap after a heavy stretch as an explicit recovery buffer
                    val bufDuration = min(gapMins, safeBufferMins)
                    val bufStart = prev.proposedEndMillis
                    recoveryBuffers.add(
                        BlueprintRecoveryBuffer(
                            id = "recovery_natural_${i}_$bufStart",
                            startMillis = bufStart,
                            endMillis = bufStart + bufDuration * 60_000L,
                            durationMinutes = bufDuration,
                            label = "Recovery Buffer (${bufDuration}m)",
                            reason = "Ultradian recharge after ${streakHeavyMins}m heavy focus"
                        )
                    )
                    streakHeavyMins = 0
                } else if (gapMins >= safeBufferMins) {
                    streakHeavyMins = 0
                }
            }

            if (heavy) {
                streakHeavyMins += durMins
                if (streakHeavyMins > maxObservedHeavyStreak) {
                    maxObservedHeavyStreak = streakHeavyMins
                }
            } else {
                streakHeavyMins = 0
            }
        }

        val summary = when {
            recoveryBuffers.isNotEmpty() -> {
                val bufWord = if (recoveryBuffers.size == 1) "recovery buffer" else "recovery buffers"
                "${recoveryBuffers.size} $bufWord added · Max heavy streak ${maxObservedHeavyStreak}m"
            }
            maxObservedHeavyStreak > 0 ->
                "Paced naturally · Max heavy streak ${maxObservedHeavyStreak}m"
            else ->
                "Light & balanced load · Burnout-safe pace"
        }

        return BurnoutPlanOptimizationResult(
            items = working,
            recoveryBuffers = recoveryBuffers.sortedBy { it.startMillis },
            maxHeavyStreakMinutes = maxObservedHeavyStreak,
            burnoutPacingSummary = summary
        )
    }
}
