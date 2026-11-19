package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.bedTimeMillis
import com.neuroflow.app.data.local.entity.wakeTimeMillis
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CrossDaySplitInfo(
    val parentTaskId: String,
    val part1Placement: ScheduledPlacement,
    val part2Date: LocalDate,
    val part2WarmupMinutes: Int
)

data class MultiDaySchedulingOutcome(
    val dayOutcomes: Map<LocalDate, SchedulingOutcome>,
    val totalScheduledTasks: Int,
    val crossDaySplits: List<CrossDaySplitInfo>,
    val finalDeferred: List<SchedulableTask>
)

@Singleton
class MultiDayScheduleOrchestrator @Inject constructor(
    private val schedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val preferencesDataStore: UserPreferencesDataStore? = null
) {
    suspend fun orchestrateMultiDaySchedule(
        tasks: List<SchedulableTask>,
        startDate: LocalDate,
        horizonDays: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceNowMillis: Long = System.currentTimeMillis(),
        rejectionPatternsJson: String = "{}"
    ): MultiDaySchedulingOutcome = orchestrateMultiDayScheduleWithContext(
        tasks = tasks,
        startDate = startDate,
        horizonDays = horizonDays,
        zoneId = zoneId,
        referenceNowMillis = referenceNowMillis,
        rejectionPatternsJson = rejectionPatternsJson
    )

    suspend fun orchestrateMultiDayScheduleWithContext(
        tasks: List<SchedulableTask>,
        startDate: LocalDate,
        horizonDays: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceNowMillis: Long = System.currentTimeMillis(),
        rejectionPatternsJson: String = "{}",
        allKnownTasks: List<TaskEntity> = emptyList(),
        closedSessions: List<TimeSessionEntity> = emptyList(),
        extraActiveUncompletedIds: Set<String> = emptySet(),
        externalAndFixedBusyWindowsByDate: Map<LocalDate, List<TimeSlotWindow>> = emptyMap(),
        precommittedCognitiveMinutesByDate: Map<LocalDate, Int> = emptyMap(),
        forceReplan: Boolean = true
    ): MultiDaySchedulingOutcome {
        val dayOutcomes = mutableMapOf<LocalDate, SchedulingOutcome>()
        val crossDaySplits = mutableListOf<CrossDaySplitInfo>()
        val carriedOverTasks = tasks.toMutableList()

        val prefs = preferencesDataStore?.preferencesFlow?.firstOrNull() ?: UserPreferences()
        val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.firstOrNull() ?: emptyList()

        val dailyCapacityMinutes = prefs.dailyDeepWorkTargetMinutes.coerceIn(30, 720)
        val safeHorizon = horizonDays.coerceIn(1, 14)

        val horizonShared = SchedulingContextFactory.buildHorizonContext(
            allKnownTasks = allKnownTasks,
            closedSessions = closedSessions,
            sleepLogs = sleepLogs,
            prefs = prefs,
            referenceNowMillis = referenceNowMillis,
            zoneId = zoneId,
            defaultGlobalCalibrationWhenEmpty = 1.20
        )

        data class DayHorizonContext(
            val dayContext: SchedulingDayContext,
            val busyWindows: List<TimeSlotWindow>,
            val openCalendarMinutes: Int
        )

        fun computeOpenMinutesInWindow(
            windowStart: Long,
            windowEnd: Long,
            blockedWindows: List<TimeSlotWindow>
        ): Int {
            if (windowEnd <= windowStart) return 0
            var intervals = mutableListOf(TimeSlotWindow(windowStart, windowEnd))
            for (blocked in blockedWindows) {
                if (blocked.endMillis <= windowStart || blocked.startMillis >= windowEnd || blocked.startMillis >= blocked.endMillis) {
                    continue
                }
                val next = mutableListOf<TimeSlotWindow>()
                for (iv in intervals) {
                    if (blocked.endMillis <= iv.startMillis || blocked.startMillis >= iv.endMillis) {
                        next.add(iv)
                    } else {
                        if (blocked.startMillis > iv.startMillis) next.add(TimeSlotWindow(iv.startMillis, blocked.startMillis))
                        if (blocked.endMillis < iv.endMillis) next.add(TimeSlotWindow(blocked.endMillis, iv.endMillis))
                    }
                }
                intervals = next
            }
            return intervals.sumOf { iv ->
                val mins = ((iv.endMillis - iv.startMillis) / 60_000L).toInt()
                // Ignore tiny unusable slivers (< 20 min) when estimating true schedulable calendar capacity
                if (mins >= 20) mins else 0
            }
        }

        // Precompute full-horizon calendar, protected, and physiological context once so Day 0..N pacing
        // accurately sees crowded upcoming meeting days before deciding Today's workload.
        val horizonContexts = (0 until safeHorizon).map { dayOffset ->
            val currentDate = startDate.plusDays(dayOffset.toLong())
            val resolvedDay = SchedulingContextFactory.buildDayContext(
                horizon = horizonShared,
                dayDate = currentDate,
                prefs = prefs,
                referenceNowMillis = referenceNowMillis,
                zoneId = zoneId,
                sleepTelemetryRepository = sleepTelemetryRepository
            )
            val calendarBusy = calendarSyncRepository?.getBusyWindows(resolvedDay.dayStartMillis, resolvedDay.dayEndMillis) ?: emptyList()
            val externalBusy = externalAndFixedBusyWindowsByDate[currentDate].orEmpty()
            val busyWindows = (calendarBusy + externalBusy).distinctBy { it.startMillis to it.endMillis }

            val startHour = prefs.workDayStart.coerceIn(0, 23)
            val endHour = prefs.workDayEnd.coerceIn(0, 23)
            val workStartMillis = currentDate.atTime(java.time.LocalTime.of(startHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
            val workEndMillis = when {
                endHour == 0 -> resolvedDay.dayEndMillis - 1L
                endHour <= startHour -> currentDate.plusDays(1).atTime(java.time.LocalTime.of(endHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
                else -> currentDate.atTime(java.time.LocalTime.of(endHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
            }
            val effectiveStart = max(
                max(workStartMillis, resolvedDay.wakeTimeMillis + 15 * 60_000L),
                if (dayOffset == 0) resolvedDay.dayNowMillis + 5 * 60_000L else workStartMillis
            )
            val allBlockedForCapacity = busyWindows +
                resolvedDay.protectedSlots.map { it.resolveForDate(currentDate, zoneId) } +
                resolvedDay.naps.map { TimeSlotWindow(it.startMillis, it.endMillis) } +
                resolvedDay.physiologicalBlocks.map { it.toTimeSlotWindow(currentDate, zoneId) }
            val openCalendarMinutes = computeOpenMinutesInWindow(effectiveStart, workEndMillis, allBlockedForCapacity)

            DayHorizonContext(
                dayContext = resolvedDay,
                busyWindows = busyWindows,
                openCalendarMinutes = openCalendarMinutes
            )
        }

        for (dayOffset in 0 until safeHorizon) {
            val horizonDay = horizonContexts[dayOffset]
            val dayCtx = horizonDay.dayContext
            val currentDate = dayCtx.date
            val dayStartEpoch = dayCtx.dayStartMillis
            val dayEndEpoch = dayCtx.dayEndMillis
            val dayNowMillis = dayCtx.dayNowMillis
            val wakeTimeMillis = dayCtx.wakeTimeMillis
            val sleepDurationMinutes = dayCtx.lastSleepDurationMinutes
            val busyWindows = horizonDay.busyWindows
            val protectedSlots = dayCtx.protectedSlots
            val dayNaps = dayCtx.naps
            val physiologicalBlocks = dayCtx.physiologicalBlocks
            val stimulantDoses = dayCtx.stimulantDoses

            fun bufferedMinutes(t: SchedulableTask): Int {
                val raw = max(5, t.estimatedMinutes)
                val ratio = horizonShared.calibrationRatioFor(t.category)
                return if (t.isDurationExplicit) raw else (raw * ratio).roundToInt().coerceAtLeast(5)
            }

            // Compute propagated effective deadlines across all remaining unlocked tasks BEFORE
            // filtering todayCandidates so future-anchored tasks stay on their intended day
            // unless a hard deadline (or urgent dependent chain) strictly requires execution today.
            val allUnlockedCarriedOver = carriedOverTasks.filterNot { it.isLocked }
            val effectiveDeadlineMap = allUnlockedCarriedOver
                .associate { it.id to (it.deadlineMillis ?: Long.MAX_VALUE) }
                .toMutableMap()
            repeat(allUnlockedCarriedOver.size) {
                var changed = false
                for (child in allUnlockedCarriedOver) {
                    val childDl = effectiveDeadlineMap[child.id] ?: Long.MAX_VALUE
                    if (childDl == Long.MAX_VALUE) continue
                    val childNeedMillis = (bufferedMinutes(child) + 10) * 60_000L
                    for (prereqId in child.prerequisiteIds) {
                        if (prereqId !in effectiveDeadlineMap) continue
                        val parentDl = childDl - childNeedMillis
                        if (parentDl < (effectiveDeadlineMap[prereqId] ?: Long.MAX_VALUE)) {
                            effectiveDeadlineMap[prereqId] = parentDl
                            changed = true
                        }
                    }
                }
                if (!changed) return@repeat
            }

            // Isolate locked tasks to their exact pinned date (or Day 0 for overdue locked tasks)
            // and never pull future-anchored unlocked tasks (tomorrow/future auto-scheduled blocks,
            // user-dated future tasks, or future recurring habits) backward onto an earlier day
            // unless a hard deadline strictly requires completion before dayEndEpoch.
            val todayCandidates = carriedOverTasks.filter { task ->
                val lockedStart = task.scheduledStartMillis
                if (task.isLocked && lockedStart != null) {
                    val pinnedDate = Instant.ofEpochMilli(lockedStart).atZone(zoneId).toLocalDate()
                    pinnedDate == currentDate || (dayOffset == 0 && pinnedDate.isBefore(currentDate))
                } else {
                    val anchorMillis = task.preferredDayStartMillis ?: task.scheduledStartMillis
                    if (anchorMillis != null) {
                        val anchorDate = Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate()
                        if (anchorDate.isAfter(currentDate)) {
                            val effDl = effectiveDeadlineMap[task.id] ?: Long.MAX_VALUE
                            effDl < dayEndEpoch
                        } else {
                            true
                        }
                    } else {
                        true
                    }
                }
            }
            if (todayCandidates.isEmpty()) {
                dayOutcomes[currentDate] = SchedulingOutcome(emptyList(), emptyList())
                continue
            }

            val candidateIds = todayCandidates.map { it.id }.toSet()

            // Calendar-aware adaptive cross-day load balancing: measure both cognitive budget AND
            // true open calendar time across every remaining day in the horizon so crowded upcoming
            // meeting days automatically shift flexible share to open days.
            val unlockedCandidates = todayCandidates.filterNot { it.isLocked }
            val externalPrecommittedMinutes = (precommittedCognitiveMinutesByDate[currentDate] ?: 0).coerceAtLeast(0)
            val availableDayCapacity = (dailyCapacityMinutes - externalPrecommittedMinutes).coerceAtLeast(0)
            val lockedCandidatesMinutesToday = todayCandidates
                .filter { it.isLocked && it.scheduledStartMillis != null }
                .sumOf { bufferedMinutes(it) }
            val remainingBaseCapacity = (availableDayCapacity - lockedCandidatesMinutesToday).coerceAtLeast(0)
            val totalRemainingUnlockedMinutes = unlockedCandidates.sumOf { bufferedMinutes(it) }

            val strictlyDueTodayMinutes = unlockedCandidates
                .filter { (effectiveDeadlineMap[it.id] ?: Long.MAX_VALUE) <= dayEndEpoch }
                .sumOf { bufferedMinutes(it) }
            val preferredTodayMinutes = unlockedCandidates
                .filter { t ->
                    val pref = t.preferredDayStartMillis
                    pref != null && pref < dayEndEpoch
                }
                .sumOf { bufferedMinutes(it) }
            val urgentTodayMinutes = max(strictlyDueTodayMinutes, preferredTodayMinutes)

            val remainingDayEffectiveCapacities = (dayOffset until safeHorizon).map { offset ->
                val futureCtx = horizonContexts[offset]
                val futureDate = futureCtx.dayContext.date
                val futurePre = (precommittedCognitiveMinutesByDate[futureDate] ?: 0).coerceAtLeast(0)
                val futureLocked = carriedOverTasks.filter { t ->
                    val ls = t.scheduledStartMillis
                    t.isLocked && ls != null && Instant.ofEpochMilli(ls).atZone(zoneId).toLocalDate() == futureDate
                }.sumOf { bufferedMinutes(it) }
                val futureAnchored = if (offset > dayOffset) {
                    carriedOverTasks.filter { t ->
                        val anchor = t.preferredDayStartMillis ?: t.scheduledStartMillis
                        !t.isLocked && anchor != null &&
                            Instant.ofEpochMilli(anchor).atZone(zoneId).toLocalDate() == futureDate
                    }.sumOf { bufferedMinutes(it) }
                } else {
                    0
                }
                val cognitiveFree = (dailyCapacityMinutes - futurePre - futureLocked - futureAnchored).coerceAtLeast(0)
                val calendarFree = (futureCtx.openCalendarMinutes - futureLocked - futureAnchored).coerceAtLeast(0)
                min(cognitiveFree, calendarFree)
            }
            val todayEffectiveOpenCap = remainingDayEffectiveCapacities.firstOrNull() ?: remainingBaseCapacity
            val totalHorizonOpenCap = remainingDayEffectiveCapacities.sum().coerceAtLeast(1)
            val activeRemainingDays = remainingDayEffectiveCapacities.count { it >= 30 }.coerceAtLeast(1)

            val daySnapshot = circadianEnergyService.evaluateSnapshotWithStimulants(
                timestampMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = externalPrecommittedMinutes.coerceAtLeast(0),
                zoneId = zoneId,
                naps = dayNaps,
                chronotype = horizonShared.chronotype,
                manualPeakStartHour = horizonShared.manualPeakStartHour,
                learnedProfile = horizonShared.learnedProfile,
                rollingAverageSleepMinutes = horizonShared.rollingAverageSleepMinutes,
                cognitiveBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = horizonShared.socialJetlagLagHours
            )
            val dependentCounts = SchedulableTaskMapper.computeDependentCounts(unlockedCandidates)
            val topPriorityTaskMinutes = unlockedCandidates
                .maxByOrNull { task ->
                    schedulerService.evaluateTaskScoreWithFatigue(
                        task = task,
                        snapshot = daySnapshot,
                        dependentCount = dependentCounts[task.id] ?: 0,
                        calibrationRatio = horizonShared.calibrationRatioFor(task.category),
                        weights = horizonShared.priorityWeights,
                        referenceNowMillis = dayNowMillis,
                        cumulativeCognitiveLoadMinutes = externalPrecommittedMinutes.toDouble()
                    ).totalScore
                }
                ?.let { bufferedMinutes(it) }
                ?: 0
            val minUnlockedFloor = min(remainingBaseCapacity, topPriorityTaskMinutes)
            val sustainableDailyPace = max(30, (dailyCapacityMinutes * 0.72).roundToInt())
            val effectivePacingDays = if (totalRemainingUnlockedMinutes <= remainingBaseCapacity) {
                1
            } else {
                val neededDaysAtPace = ceil(totalRemainingUnlockedMinutes.toDouble() / sustainableDailyPace).toInt()
                min(activeRemainingDays, max(2, neededDaysAtPace)).coerceAtLeast(1)
            }
            val equalSmoothedShare = ceil((totalRemainingUnlockedMinutes.toDouble() / effectivePacingDays) * 1.05).toInt()
            // Capacity-weighted share automatically pulls more flexible work onto open days when future days are meeting-heavy
            val calendarWeightedShare = if (activeRemainingDays > 1 && totalRemainingUnlockedMinutes > 0) {
                val proportion = todayEffectiveOpenCap.toDouble() / totalHorizonOpenCap.toDouble()
                ceil(totalRemainingUnlockedMinutes * proportion * 1.08).toInt()
            } else {
                equalSmoothedShare
            }
            val futureHorizonTight = remainingDayEffectiveCapacities.drop(1).any { it < sustainableDailyPace / 2 }
            val smoothedShare = if (futureHorizonTight) {
                max(equalSmoothedShare, calendarWeightedShare)
            } else {
                equalSmoothedShare
            }
            val targetUnlockedBudget = maxOf(smoothedShare, urgentTodayMinutes, minUnlockedFloor)
                .coerceIn(0, remainingBaseCapacity)
            val dayCognitiveBudget = (lockedCandidatesMinutesToday + targetUnlockedBudget)
                .coerceIn(0, availableDayCapacity)

            val activeUncompletedIds = (carriedOverTasks.map { it.id } + extraActiveUncompletedIds).toSet()

            val outcome = schedulerService.scheduleDayWithOutcome(
                tasks = todayCandidates,
                dayDate = currentDate,
                nowMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId,
                busyWindows = busyWindows,
                protectedSlots = protectedSlots,
                calibrationRatio = horizonShared.globalCalibrationRatio,
                forceReplan = forceReplan,
                naps = dayNaps,
                chronotype = horizonShared.chronotype,
                manualPeakStartHour = horizonShared.manualPeakStartHour,
                priorityWeights = horizonShared.priorityWeights,
                learnedProfile = horizonShared.learnedProfile,
                rollingAverageSleepMinutes = horizonShared.rollingAverageSleepMinutes,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = horizonShared.socialJetlagLagHours,
                activeUncompletedTaskIds = activeUncompletedIds,
                workDayStartHour = prefs.workDayStart,
                workDayEndHour = prefs.workDayEnd,
                dailyCognitiveCapacityMinutes = dayCognitiveBudget,
                rejectionPatternsJson = rejectionPatternsJson,
                categoryCalibrationRatios = horizonShared.categoryCalibrationRatios
            )

            // Enforce cognitive capacity boundary in priority order while grouping dynamic splits atomically
            data class PlacementGroup(
                val groupId: String,
                val isDynamicSplit: Boolean,
                val placements: List<ScheduledPlacement>,
                val durationMinutes: Int,
                val allLocked: Boolean,
                val priorityScore: Double,
                val earliestStartMillis: Long
            )

            val candidateGroups = mutableListOf<PlacementGroup>()
            val processedIndices = mutableSetOf<Int>()
            for (idx in outcome.placements.indices) {
                if (idx in processedIndices) continue
                val p = outcome.placements[idx]
                val isDynamicSplit = p.task.id !in candidateIds &&
                    (p.task.id.contains("#part") || p.task.id.contains("_part"))
                val groupPlacements = if (isDynamicSplit) {
                    val baseId = SchedulableTaskMapper.cleanSplitTaskId(p.task.id)
                    val groupIndices = outcome.placements.indices.filter { j ->
                        val q = outcome.placements[j]
                        q.task.id !in candidateIds &&
                            (q.task.id.contains("#part") || q.task.id.contains("_part")) &&
                            SchedulableTaskMapper.cleanSplitTaskId(q.task.id) == baseId
                    }
                    processedIndices.addAll(groupIndices)
                    groupIndices.map { outcome.placements[it] }
                } else {
                    processedIndices.add(idx)
                    listOf(p)
                }
                val groupId = if (isDynamicSplit) SchedulableTaskMapper.cleanSplitTaskId(p.task.id) else p.task.id
                val effDl = effectiveDeadlineMap[groupId] ?: Long.MAX_VALUE
                val dueTodayBoost = if (effDl <= dayEndEpoch) 25.0 else 0.0
                val quadrantBoost = when (p.task.quadrant) {
                    com.neuroflow.app.domain.model.Quadrant.DO_FIRST -> 12.0
                    com.neuroflow.app.domain.model.Quadrant.SCHEDULE -> 5.0
                    com.neuroflow.app.domain.model.Quadrant.DELEGATE -> -4.0
                    com.neuroflow.app.domain.model.Quadrant.ELIMINATE -> -15.0
                    null -> if (p.task.priority >= 2) 8.0 else 0.0
                }
                val frogBoost = if (groupPlacements.any { it.task.isFrog }) 8.0 else 0.0
                val baseScore = groupPlacements.maxOfOrNull { it.scoreBreakdown.totalScore } ?: 0.0
                candidateGroups.add(
                    PlacementGroup(
                        groupId = groupId,
                        isDynamicSplit = isDynamicSplit,
                        placements = groupPlacements,
                        durationMinutes = groupPlacements.sumOf { it.window.durationMinutes },
                        allLocked = groupPlacements.all { it.task.isLocked },
                        priorityScore = baseScore + dueTodayBoost + quadrantBoost + frogBoost,
                        earliestStartMillis = groupPlacements.minOf { it.window.startMillis }
                    )
                )
            }

            candidateGroups.sortWith(
                compareByDescending<PlacementGroup> { it.allLocked }
                    .thenByDescending { it.priorityScore }
                    .thenBy { it.earliestStartMillis }
            )

            var accumulatedMinutes = 0
            val effectivePlacements = mutableListOf<ScheduledPlacement>()
            val overflowDeferrals = mutableListOf<DeferralRationale>()
            for (group in candidateGroups) {
                if (!group.allLocked && accumulatedMinutes + group.durationMinutes > dayCognitiveBudget) {
                    overflowDeferrals.add(
                        DeferralRationale(
                            taskId = group.groupId,
                            reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                            explanation = "Exceeded daily ${dayCognitiveBudget}m budget; carried to next day.",
                            actionableSuggestion = "Scheduled on subsequent horizon day."
                        )
                    )
                    continue
                }
                effectivePlacements.addAll(group.placements)
                accumulatedMinutes += group.durationMinutes
            }
            effectivePlacements.sortBy { it.window.startMillis }

            val combinedDeferrals = (outcome.deferrals + overflowDeferrals).distinctBy { it.taskId }
            dayOutcomes[currentDate] = SchedulingOutcome(effectivePlacements, combinedDeferrals)

            // Placed tasks (including unsplit tasks dynamically split into #part1/#part2) are removed from the carried-over pool
            val placedIds = effectivePlacements.map { it.task.id }.toSet()
            val placedDynamicBaseIds = effectivePlacements
                .filter { it.task.id !in candidateIds }
                .map { SchedulableTaskMapper.cleanSplitTaskId(it.task.id) }
                .toSet()
            carriedOverTasks.removeAll { it.id in placedIds || it.id in placedDynamicBaseIds }

            // Detect cross-day splits: Part i placed today without Part i+1 fitting today,
            // and anchor Part i+1 onto nextDay with warm-up continuity so it resumes smoothly.
            for (p in effectivePlacements) {
                val partMatch = Regex("""^(.*?)(?:[#_]part(\d+)|\s*\(Part\s*(\d+)/\d+\))$""").find(p.task.id)
                    ?: Regex("""^(.*?)\s*\(Part\s*(\d+)/\d+\)""").find(p.task.title)
                val baseId = partMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: p.task.parentTaskId
                    ?: p.task.id.substringBefore("_part1").substringBefore("#part1")
                val partNum = partMatch?.let { m ->
                    m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                        ?: m.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                } ?: if (p.task.id.endsWith("_part1") || p.task.id.endsWith("#part1")) 1 else null

                if (partNum != null) {
                    val nextPartNum = partNum + 1
                    val isMatchingNextPart: (SchedulableTask) -> Boolean = { t ->
                        (t.id.startsWith(baseId) || t.parentTaskId == baseId) && (
                            t.id.endsWith("_part$nextPartNum") ||
                            t.id.endsWith("#part$nextPartNum") ||
                            t.id.contains(Regex("""[#_]part$nextPartNum(\D|$)""")) ||
                            t.title.contains("(Part $nextPartNum/")
                        )
                    }
                    val hasNextPart = tasks.any(isMatchingNextPart)
                    val nextPartPlaced = effectivePlacements.any { isMatchingNextPart(it.task) }
                    if (hasNextPart && !nextPartPlaced && dayOffset + 1 < safeHorizon) {
                        val part2Date = currentDate.plusDays(1)
                        val warmupMins = 15
                        crossDaySplits.add(
                            CrossDaySplitInfo(
                                parentTaskId = baseId,
                                part1Placement = p,
                                part2Date = part2Date,
                                part2WarmupMinutes = warmupMins
                            )
                        )
                        // Anchor Part i+1 in carriedOverTasks to next day for immediate continuity
                        val nextDayStartEpoch = part2Date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                        for (idx in carriedOverTasks.indices) {
                            val candidate = carriedOverTasks[idx]
                            if (isMatchingNextPart(candidate) && !candidate.isLocked && candidate.preferredDayStartMillis == null) {
                                carriedOverTasks[idx] = candidate.copy(
                                    preferredDayStartMillis = nextDayStartEpoch,
                                    priorSessionCount = max(1, candidate.priorSessionCount)
                                )
                            }
                        }
                    }
                }
            }
        }

        val totalScheduled = dayOutcomes.values.sumOf { it.placements.size }
        return MultiDaySchedulingOutcome(
            dayOutcomes = dayOutcomes,
            totalScheduledTasks = totalScheduled,
            crossDaySplits = crossDaySplits,
            finalDeferred = carriedOverTasks
        )
    }
}
