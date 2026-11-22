package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.CircadianSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Instant on-device slot recommender used in Task Creation and "Push to Today" from Goals.
 * Evaluates true open gaps between scheduled tasks and protected time blocks, scores every
 * candidate window against multi-day circadian energy curves, and returns the top match
 * plus up to 2 spaced alternates with human-readable gap context.
 */
object TaskSlotRecommender {

    private const val STEP_MINUTES = 15
    private const val BUFFER_MINUTES = 5
    private const val RECOVERY_BUFFER_MINUTES = 15
    private const val HEAVY_STREAK_CEILING_MINUTES = 75
    private const val NEIGHBOR_CONTEXT_WINDOW_MS = 3 * 3_600_000L

    private data class NamedBusyInterval(
        val startMillis: Long,
        val endMillis: Long,
        val rawEndMillis: Long,
        val durationMinutes: Int,
        val label: String,
        val isHeavy: Boolean
    )

    fun recommendSlots(
        durationMinutes: Int,
        energyLevel: EnergyLevel,
        targetDateMillis: Long?,
        deadlineMillis: Long?,
        existingTasks: List<TaskEntity>,
        energyTimeline: List<CircadianSnapshot>,
        prefs: UserPreferences,
        nowMillis: Long = System.currentTimeMillis(),
        maxProposals: Int = 3,
        zoneId: ZoneId = ZoneId.systemDefault(),
        excludeTaskId: String? = null,
        isFrog: Boolean = false,
        priority: Priority = Priority.MEDIUM,
        burnoutRiskState: BurnoutRiskState? = null
    ): List<RecommendedSlotProposal> {
        val dur = durationMinutes.coerceIn(10, 240)
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = nowZdt.toLocalDate()
        val initialTargetDate = targetDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
        } ?: today

        val tomorrowWorkStartMillis = today.plusDays(1)
            .atTime(LocalTime.of(prefs.workDayStart.coerceIn(5, 12), 0))
            .atZone(zoneId).toInstant().toEpochMilli()
        val isUrgentTomorrowMorning = deadlineMillis != null && deadlineMillis in nowMillis..tomorrowWorkStartMillis
        val isOvertimeToday = prefs.overtimeEnabledEpochDay == today.toEpochDay()

        val candidateDates = buildList {
            add(initialTargetDate)
            if (initialTargetDate == today) add(today.plusDays(1))
        }.distinct()

        val primaryDayStartMillis = initialTargetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val allProposals = mutableListOf<RecommendedSlotProposal>()
        var todayHadNoSlots = false

        for (date in candidateDates) {
            val isDateToday = date == today
            // Prefer regular daytime slots first; only unlock evening overtime slots if
            // Overtime Mode is explicitly enabled OR no regular daytime slots fit before an urgent deadline.
            val primaryAllowOvertime = isDateToday && isOvertimeToday
            var dateProposals = findSlotsForDate(
                date = date,
                durationMinutes = dur,
                energyLevel = energyLevel,
                deadlineMillis = deadlineMillis,
                existingTasks = existingTasks,
                energyTimeline = if (isDateToday) energyTimeline else emptyList(),
                prefs = prefs,
                nowMillis = nowMillis,
                allowOvertime = primaryAllowOvertime,
                isUrgentTomorrowMorning = isUrgentTomorrowMorning,
                zoneId = zoneId,
                excludeTaskId = excludeTaskId,
                isFrog = isFrog,
                priority = priority,
                isTodayFullFallback = !isDateToday && todayHadNoSlots,
                burnoutRiskState = if (isDateToday) burnoutRiskState else null
            )
            if (isDateToday && dateProposals.isEmpty() && !primaryAllowOvertime && isUrgentTomorrowMorning) {
                dateProposals = findSlotsForDate(
                    date = date,
                    durationMinutes = dur,
                    energyLevel = energyLevel,
                    deadlineMillis = deadlineMillis,
                    existingTasks = existingTasks,
                    energyTimeline = energyTimeline,
                    prefs = prefs,
                    nowMillis = nowMillis,
                    allowOvertime = true,
                    isUrgentTomorrowMorning = true,
                    zoneId = zoneId,
                    excludeTaskId = excludeTaskId,
                    isFrog = isFrog,
                    priority = priority,
                    isTodayFullFallback = false,
                    burnoutRiskState = burnoutRiskState
                )
            }
            if (isDateToday && dateProposals.isEmpty()) {
                todayHadNoSlots = true
            }
            allProposals.addAll(dateProposals)
            if (allProposals.size >= maxProposals && date == initialTargetDate) {
                break
            }
        }

        return allProposals
            .distinctBy { it.startMillis }
            .sortedWith(
                compareByDescending<RecommendedSlotProposal> { it.dateMillis == primaryDayStartMillis }
                    .thenByDescending { it.matchScore }
                    .thenBy { it.startMillis }
            )
            .take(maxProposals)
    }

    private fun findSlotsForDate(
        date: LocalDate,
        durationMinutes: Int,
        energyLevel: EnergyLevel,
        deadlineMillis: Long?,
        existingTasks: List<TaskEntity>,
        energyTimeline: List<CircadianSnapshot>,
        prefs: UserPreferences,
        nowMillis: Long,
        allowOvertime: Boolean,
        isUrgentTomorrowMorning: Boolean,
        zoneId: ZoneId,
        excludeTaskId: String?,
        isFrog: Boolean,
        priority: Priority,
        isTodayFullFallback: Boolean,
        burnoutRiskState: BurnoutRiskState?
    ): List<RecommendedSlotProposal> {
        val dayStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEndBoundary = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        val workStartHour = prefs.workDayStart.coerceIn(5, 14)
        val regularWorkEndHour = prefs.workDayEnd.coerceIn(workStartHour + 2, 23)
        val overtimeEndHour = max(regularWorkEndHour, min(23, prefs.sleepHour.coerceIn(21, 23)))

        val horizonStartBase = date.atTime(LocalTime.of(workStartHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val regularHorizonEnd = date.atTime(LocalTime.of(regularWorkEndHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val effectiveHorizonEnd = if (allowOvertime) {
            max(regularHorizonEnd, date.atTime(LocalTime.of(overtimeEndHour, 45)).atZone(zoneId).toInstant().toEpochMilli())
        } else {
            regularHorizonEnd
        }

        val roundedNow = roundUpToStep(nowMillis + 5 * 60_000L, STEP_MINUTES)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val horizonStart = if (date == today) max(horizonStartBase, roundedNow) else horizonStartBase
        val finalHorizonEnd = if (deadlineMillis != null && deadlineMillis in (horizonStart + durationMinutes * 60_000L)..effectiveHorizonEnd) {
            deadlineMillis
        } else {
            effectiveHorizonEnd
        }
        if (horizonStart + durationMinutes * 60_000L > finalHorizonEnd) return emptyList()

        val isNewTaskHeavy = energyLevel == EnergyLevel.HIGH ||
            isFrog ||
            (energyLevel == EnergyLevel.MEDIUM && durationMinutes >= 45)
        val activeBurnoutRecoveryMins = if (date == today && burnoutRiskState?.isElevated == true && isNewTaskHeavy) {
            burnoutRiskState.recommendedRestMinutes.coerceIn(RECOVERY_BUFFER_MINUTES, 20)
        } else {
            0
        }
        val activeBurnoutReadyMillis = if (activeBurnoutRecoveryMins > 0) {
            roundUpToStep(nowMillis + activeBurnoutRecoveryMins * 60_000L, STEP_MINUTES)
        } else {
            0L
        }

        val busy = mutableListOf<NamedBusyInterval>()
        existingTasks.forEach { task ->
            if (task.id == excludeTaskId || task.status != TaskStatus.ACTIVE) return@forEach
            val sDate = task.effectiveScheduledDateMillis()
            val start = task.resolveScheduledEpochMillis(dayStartMillis) ?: return@forEach
            if (task.scheduledTime == null) return@forEach
            if ((sDate != null && sDate in dayStartMillis..dayEndBoundary) || start in dayStartMillis..dayEndBoundary) {
                val dur = TaskBlockPlan.plannedMinutes(task).coerceIn(5, 480)
                val rawEnd = start + dur * 60_000L
                busy.add(
                    NamedBusyInterval(
                        startMillis = start,
                        endMillis = rawEnd + BUFFER_MINUTES * 60_000L,
                        rawEndMillis = rawEnd,
                        durationMinutes = dur,
                        label = task.title.trim().ifBlank { "Task" },
                        isHeavy = BurnoutGuardEngine.isHeavyTask(task)
                    )
                )
            }
        }

        prefs.protectedTimeSlots.filter { it.isEnabled }.forEach { slot ->
            val startMin = slot.startMinuteOfDay.coerceIn(0, 1439)
            val start = date.atTime(startMin / 60, startMin % 60).atZone(zoneId).toInstant().toEpochMilli()
            val rawEnd = start + slot.durationMinutes * 60_000L
            val end = rawEnd + slot.recoveryBufferMinutes * 60_000L
            busy.add(
                NamedBusyInterval(
                    startMillis = start,
                    endMillis = end,
                    rawEndMillis = rawEnd,
                    durationMinutes = slot.durationMinutes,
                    label = slot.label.trim().ifBlank { "Protected Block" },
                    isHeavy = false
                )
            )
        }

        val sortedBusy = busy.sortedBy { it.startMillis }

        val targetEnergy = when (energyLevel) {
            EnergyLevel.HIGH -> 84
            EnergyLevel.MEDIUM -> 58
            EnergyLevel.LOW -> 34
        }

        val scoredCandidates = mutableListOf<RecommendedSlotProposal>()
        var cursor = horizonStart
        val durMillis = durationMinutes * 60_000L
        val stepMillis = STEP_MINUTES * 60_000L

        while (cursor + durMillis <= finalHorizonEnd) {
            val end = cursor + durMillis
            val collides = sortedBusy.any { b -> cursor < b.endMillis && end > b.startMillis }
            if (!collides) {
                val predictedEnergy = estimateEnergyAt(cursor, energyTimeline, prefs, zoneId)
                val energyMatch = (100 - abs(predictedEnergy - targetEnergy)).coerceIn(20, 100)
                val isOvertimeSlot = cursor >= regularHorizonEnd

                // Find immediate surrounding schedule anchors for gap context
                val prevBusy = sortedBusy.lastOrNull { it.endMillis <= cursor }
                val nextBusy = sortedBusy.firstOrNull { it.startMillis >= end }
                val gapStart = max(horizonStart, prevBusy?.endMillis ?: horizonStart)
                val gapEnd = min(finalHorizonEnd, nextBusy?.startMillis ?: finalHorizonEnd)
                val gapMinutes = ((gapEnd - gapStart) / 60_000L).toInt().coerceAtLeast(durationMinutes)

                // Snug-fit bonus: prefer slotting cleanly into bounded gaps rather than fragmenting open blocks
                val slackMinutes = (gapMinutes - durationMinutes).coerceAtLeast(0)
                val gapFitBonus = when {
                    (prevBusy != null || nextBusy != null) && slackMinutes in 0..20 -> 5
                    (prevBusy != null || nextBusy != null) && slackMinutes in 21..45 -> 3
                    else -> 0
                }

                val urgencyBonus = if (isUrgentTomorrowMorning && date == today) 14 else 0
                val priorityPeakBonus = when {
                    (isFrog || priority == Priority.HIGH) && energyLevel == EnergyLevel.HIGH && predictedEnergy >= 75 -> 6
                    isFrog && date == today -> 4
                    else -> 0
                }
                val earlinessBonus = if (date == today) {
                    val effectiveStartRef = if (activeBurnoutReadyMillis > horizonStart) activeBurnoutReadyMillis else horizonStart
                    val hoursOut = ((cursor - effectiveStartRef) / 3_600_000.0).coerceAtLeast(0.0)
                    (8.0 - hoursOut * 0.85).roundToInt().coerceIn(0, 8)
                } else 0
                val overtimePenalty = if (isOvertimeSlot && !isUrgentTomorrowMorning) -8 else 0

                // Proactive Burnout Prevention: recovery buffers + heavy-streak breaking
                var burnoutDelta = 0
                var includesRecoveryBuffer = false
                var recoveryBufferMins = 0
                var burnoutSafetyBadge: String? = null

                if (activeBurnoutRecoveryMins > 0 && activeBurnoutReadyMillis > 0L) {
                    if (cursor < activeBurnoutReadyMillis) {
                        burnoutDelta -= 30
                        burnoutSafetyBadge = "Skipping ${activeBurnoutRecoveryMins}m rest"
                    } else if ((cursor - activeBurnoutReadyMillis) <= 30 * 60_000L) {
                        burnoutDelta += 12
                        includesRecoveryBuffer = true
                        recoveryBufferMins = activeBurnoutRecoveryMins
                        burnoutSafetyBadge = "After ${activeBurnoutRecoveryMins}m rest"
                    }
                }

                if (isNewTaskHeavy) {
                    val prevIdx = sortedBusy.indexOfLast { it.endMillis <= cursor }
                    val nextIdx = sortedBusy.indexOfFirst { it.startMillis >= end }

                    // Measure contiguous heavy load immediately before cursor (broken by >= 15m gap)
                    var precedingHeavyMins = 0
                    var prevHeavyGapMins = Int.MAX_VALUE
                    if (prevIdx >= 0 && sortedBusy[prevIdx].isHeavy) {
                        prevHeavyGapMins = ((cursor - sortedBusy[prevIdx].rawEndMillis) / 60_000L).toInt().coerceAtLeast(0)
                        if (prevHeavyGapMins < RECOVERY_BUFFER_MINUTES) {
                            var k = prevIdx
                            var anchorStart = sortedBusy[k].startMillis
                            precedingHeavyMins += sortedBusy[k].durationMinutes
                            k--
                            while (k >= 0 && sortedBusy[k].isHeavy) {
                                val interGap = ((anchorStart - sortedBusy[k].rawEndMillis) / 60_000L).toInt()
                                if (interGap >= RECOVERY_BUFFER_MINUTES) break
                                precedingHeavyMins += sortedBusy[k].durationMinutes
                                anchorStart = sortedBusy[k].startMillis
                                k--
                            }
                        } else if (prevHeavyGapMins in RECOVERY_BUFFER_MINUTES..40) {
                            // Clean recovery buffer right after a heavy block!
                            burnoutDelta += 9
                            if (!includesRecoveryBuffer) {
                                includesRecoveryBuffer = true
                                recoveryBufferMins = min(prevHeavyGapMins, 20)
                                burnoutSafetyBadge = "Burnout-safe · ${recoveryBufferMins}m rest"
                            }
                        }
                    }

                    // Measure contiguous heavy load immediately after end (broken by >= 15m gap)
                    var followingHeavyMins = 0
                    if (nextIdx >= 0 && sortedBusy[nextIdx].isHeavy) {
                        val nextGapMins = ((sortedBusy[nextIdx].startMillis - end) / 60_000L).toInt().coerceAtLeast(0)
                        if (nextGapMins < RECOVERY_BUFFER_MINUTES) {
                            var k = nextIdx
                            var anchorEnd = sortedBusy[k].rawEndMillis
                            followingHeavyMins += sortedBusy[k].durationMinutes
                            k++
                            while (k < sortedBusy.size && sortedBusy[k].isHeavy) {
                                val interGap = ((sortedBusy[k].startMillis - anchorEnd) / 60_000L).toInt()
                                if (interGap >= RECOVERY_BUFFER_MINUTES) break
                                followingHeavyMins += sortedBusy[k].durationMinutes
                                anchorEnd = sortedBusy[k].rawEndMillis
                                k++
                            }
                        }
                    }

                    val combinedHeavyStreak = precedingHeavyMins + durationMinutes + followingHeavyMins
                    if ((precedingHeavyMins > 0 || followingHeavyMins > 0) && combinedHeavyStreak > HEAVY_STREAK_CEILING_MINUTES) {
                        val streakPenalty = if (combinedHeavyStreak >= 100) -30 else -22
                        burnoutDelta += streakPenalty
                        if (burnoutSafetyBadge == null) {
                            burnoutSafetyBadge = "Heavy streak (${combinedHeavyStreak}m)"
                        }
                    } else if (precedingHeavyMins >= 45 && prevHeavyGapMins < RECOVERY_BUFFER_MINUTES) {
                        burnoutDelta -= 18
                        if (burnoutSafetyBadge == null) {
                            burnoutSafetyBadge = "Needs 15m recovery gap"
                        }
                    }
                }

                val score = (energyMatch + gapFitBonus + urgencyBonus + priorityPeakBonus + earlinessBonus + overtimePenalty + burnoutDelta)
                    .coerceIn(10, 100)

                val energyBadge = when {
                    isOvertimeSlot -> "Overtime · $predictedEnergy%"
                    energyLevel == EnergyLevel.HIGH && predictedEnergy >= 72 -> "Peak $predictedEnergy%"
                    energyLevel == EnergyLevel.LOW && predictedEnergy <= 55 -> "Easy Fit $predictedEnergy%"
                    energyMatch >= 82 -> "Great Fit $predictedEnergy%"
                    else -> "Steady $predictedEnergy%"
                }

                val rawGapContext = buildGapContextLabel(
                    slotStartMillis = cursor,
                    slotEndMillis = end,
                    gapMinutes = gapMinutes,
                    prevBusy = prevBusy,
                    nextBusy = nextBusy,
                    isTodayFullFallback = isTodayFullFallback,
                    isUrgentTomorrowMorning = isUrgentTomorrowMorning,
                    isOvertimeSlot = isOvertimeSlot,
                    zoneId = zoneId
                )
                val gapContext = if (includesRecoveryBuffer && recoveryBufferMins > 0) {
                    "After ${recoveryBufferMins}m recovery · $rawGapContext"
                } else {
                    rawGapContext
                }

                val reason = when {
                    isTodayFullFallback -> "Today is full · $gapContext"
                    isUrgentTomorrowMorning && isOvertimeSlot -> "Due tomorrow AM · $gapContext"
                    isOvertimeSlot -> "Tonight stretch · $gapContext"
                    else -> gapContext
                }

                scoredCandidates.add(
                    RecommendedSlotProposal(
                        startMillis = cursor,
                        endMillis = end,
                        dateMillis = dayStartMillis,
                        timeOfDayOffsetMillis = (cursor - dayStartMillis).coerceAtLeast(0L),
                        matchScore = score,
                        predictedEnergyPercent = predictedEnergy,
                        reasonLabel = reason,
                        isOvertimeSlot = isOvertimeSlot,
                        energyMatchLabel = energyBadge,
                        gapContextLabel = gapContext,
                        gapDurationMinutes = gapMinutes,
                        isTodayFullFallback = isTodayFullFallback,
                        includesRecoveryBuffer = includesRecoveryBuffer,
                        recoveryBufferMinutes = recoveryBufferMins,
                        burnoutSafetyBadge = burnoutSafetyBadge
                    )
                )
            }
            cursor += stepMillis
        }

        // Pick non-overlapping diverse top proposals (spaced by at least 45 min)
        val picked = mutableListOf<RecommendedSlotProposal>()
        for (cand in scoredCandidates.sortedWith(compareByDescending<RecommendedSlotProposal> { it.matchScore }.thenBy { it.startMillis })) {
            if (picked.none { abs(it.startMillis - cand.startMillis) < 45 * 60_000L }) {
                picked.add(cand)
                if (picked.size >= 3) break
            }
        }
        return picked
    }

    private fun buildGapContextLabel(
        slotStartMillis: Long,
        slotEndMillis: Long,
        gapMinutes: Int,
        prevBusy: NamedBusyInterval?,
        nextBusy: NamedBusyInterval?,
        isTodayFullFallback: Boolean,
        isUrgentTomorrowMorning: Boolean,
        isOvertimeSlot: Boolean,
        zoneId: ZoneId
    ): String {
        val prevClose = prevBusy?.takeIf { (slotStartMillis - it.endMillis) <= NEIGHBOR_CONTEXT_WINDOW_MS }
        val nextClose = nextBusy?.takeIf { (it.startMillis - slotEndMillis) <= NEIGHBOR_CONTEXT_WINDOW_MS }

        val baseGap = when {
            prevClose != null && nextClose != null ->
                "Fits ${gapMinutes}m gap between '${compactLabel(prevClose.label)}' & '${compactLabel(nextClose.label)}'"
            nextClose != null ->
                "Fits ${gapMinutes}m gap before '${compactLabel(nextClose.label)}'"
            prevClose != null ->
                "Open right after '${compactLabel(prevClose.label)}'"
            else -> {
                val hour = Instant.ofEpochMilli(slotStartMillis).atZone(zoneId).hour
                val period = when {
                    hour < 12 -> "morning"
                    hour < 17 -> "afternoon"
                    else -> "evening"
                }
                "Open $period window"
            }
        }

        return when {
            isUrgentTomorrowMorning && isOvertimeSlot -> "$baseGap · before tomorrow AM"
            isTodayFullFallback -> baseGap
            else -> baseGap
        }
    }

    private fun compactLabel(raw: String): String {
        val cleaned = raw.trim()
        return if (cleaned.length <= 16) cleaned else cleaned.take(15).trimEnd() + "…"
    }

    /**
     * Estimates usable cognitive energy [10..100] at [millis].
     * Uses the live Borbély circadian timeline when within 45 minutes; otherwise evaluates a
     * smooth multi-day circadian alertness + homeostatic sleep-pressure curve anchored on the
     * user's wake/sleep hours and personal peak focus window.
     */
    private fun estimateEnergyAt(
        millis: Long,
        energyTimeline: List<CircadianSnapshot>,
        prefs: UserPreferences,
        zoneId: ZoneId
    ): Int {
        if (energyTimeline.isNotEmpty()) {
            val closest = energyTimeline.minByOrNull { abs(it.timestampMillis - millis) }
            if (closest != null && abs(closest.timestampMillis - millis) <= 45 * 60_000L) {
                return closest.usableEnergy.coerceIn(10, 100)
            }
        }
        val zdt = Instant.ofEpochMilli(millis).atZone(zoneId)
        val hourDecimal = zdt.hour + zdt.minute / 60.0
        val wakeHour = prefs.wakeUpHour.coerceIn(4, 11).toDouble()
        val sleepHour = prefs.sleepHour.coerceIn(20, 24).toDouble()
        val peakStart = (prefs.effectivePeakStart.takeIf { it in 0..23 } ?: prefs.peakEnergyStart.coerceIn(0, 23)).toDouble()
        val peakEnd = (prefs.effectivePeakEnd.takeIf { it in 0..23 } ?: prefs.peakEnergyEnd.coerceIn(0, 23)).toDouble()
        val peakCenter = if (peakEnd > peakStart) (peakStart + peakEnd) / 2.0 else (peakStart + 1.5).coerceAtMost(22.0)

        val hoursAwake = (hourDecimal - wakeHour).coerceAtLeast(0.0)
        val wakeSpan = (sleepHour - wakeHour).coerceAtLeast(10.0)

        // Smooth Gaussian peak centered on user's biological peak window
        val peakBump = 28.0 * exp(-((hourDecimal - peakCenter) * (hourDecimal - peakCenter)) / 6.5)
        // Post-prandial circadian dip ~7.2 hours after wake
        val dipCenter = (wakeHour + 7.2).coerceIn(12.5, 16.0)
        val dipDrop = -18.0 * exp(-((hourDecimal - dipCenter) * (hourDecimal - dipCenter)) / 2.4)
        // Secondary late-afternoon rebound ~10 hours after wake
        val reboundCenter = (wakeHour + 10.2).coerceIn(16.0, 19.5)
        val reboundBump = 10.0 * exp(-((hourDecimal - reboundCenter) * (hourDecimal - reboundCenter)) / 4.0)
        // Progressive homeostatic sleep pressure toward bedtime
        val pressureDecay = -24.0 * ((hoursAwake / wakeSpan).coerceIn(0.0, 1.35))
        // Gentle circadian cosine baseline
        val circadianBase = 64.0 + 8.0 * cos(2.0 * PI * (hourDecimal - peakCenter) / 24.0)

        return (circadianBase + peakBump + dipDrop + reboundBump + pressureDecay)
            .roundToInt()
            .coerceIn(18, 95)
    }

    private fun roundUpToStep(millis: Long, stepMinutes: Int): Long {
        val stepMs = stepMinutes * 60_000L
        val rem = millis % stepMs
        return if (rem == 0L) millis else millis + (stepMs - rem)
    }
}
