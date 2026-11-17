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
        extraActiveUncompletedIds: Set<String> = emptySet()
    ): MultiDaySchedulingOutcome {
        val dayOutcomes = mutableMapOf<LocalDate, SchedulingOutcome>()
        val crossDaySplits = mutableListOf<CrossDaySplitInfo>()
        val carriedOverTasks = tasks.toMutableList()

        val prefs = preferencesDataStore?.preferencesFlow?.firstOrNull() ?: UserPreferences()
        val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.firstOrNull() ?: emptyList()
        val recentSleep = sleepLogs.maxByOrNull { it.wakeTimeMillis }

        val dailyCapacityMinutes = prefs.dailyDeepWorkTargetMinutes.coerceIn(30, 720)
        val priorityWeights = SchedulableTaskMapper.toPriorityWeights(prefs)

        val rawWakeHour = prefs.wakeTimeHour.coerceIn(4, 12)
        val wakeHour = rawWakeHour
        val wakeMinute = prefs.wakeTimeMinute.coerceIn(0, 59)

        val sleepDurationMinutes = recentSleep?.let {
            ((it.wakeTimeMillis - it.bedTimeMillis) / 60_000L).toInt().coerceIn(240, 660)
        } ?: 480
        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, referenceNowMillis, zoneId)
        val socialJetlagLagHours = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) },
            nowMillis = referenceNowMillis,
            zoneId = zoneId
        ).mondayMorningPhaseLagHours

        val planningCalibration = if (allKnownTasks.isNotEmpty()) {
            AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        } else null
        val calibrationRatio = planningCalibration?.globalFactor?.toDouble()?.coerceIn(0.8, 1.6) ?: 1.20

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val observations = if (completedTasks.isNotEmpty()) {
            buildBehavioralObservations(completedTasks, closedSessions, zoneId)
        } else emptyList()
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val safeHorizon = horizonDays.coerceIn(1, 14)

        for (dayOffset in 0 until safeHorizon) {
            val currentDate = startDate.plusDays(dayOffset.toLong())
            val dayStartEpoch = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEndEpoch = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            val dayNowMillis = if (dayOffset == 0 && referenceNowMillis in dayStartEpoch until dayEndEpoch) {
                referenceNowMillis
            } else {
                dayStartEpoch
            }

            val calculatedWakeEpoch = dayStartEpoch + (wakeHour * 3600_000L) + (wakeMinute * 60_000L)
            val wakeTimeMillis = if (dayOffset == 0 && recentSleep != null && recentSleep.wakeTimeMillis in dayStartEpoch..dayEndEpoch) {
                recentSleep.wakeTimeMillis
            } else {
                calculatedWakeEpoch
            }

            val busyWindows = calendarSyncRepository?.getBusyWindows(dayStartEpoch, dayEndEpoch) ?: emptyList()

            // Isolate locked tasks pinned to future dates so they are only evaluated on their target date
            val todayCandidates = carriedOverTasks.filter { task ->
                val lockedStart = task.scheduledStartMillis
                if (task.isLocked && lockedStart != null) {
                    val pinnedDate = Instant.ofEpochMilli(lockedStart).atZone(zoneId).toLocalDate()
                    !pinnedDate.isAfter(currentDate)
                } else {
                    true
                }
            }
            if (todayCandidates.isEmpty()) {
                dayOutcomes[currentDate] = SchedulingOutcome(emptyList(), emptyList())
                continue
            }

            val protectedSlots = prefs.protectedTimeSlots
                .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartEpoch) }
                .map {
                    com.neuroflow.app.domain.service.ProtectedLocalSlot(
                        startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                        startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                        durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                    )
                }
            val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
            val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
                (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
            } else null

            val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
                .filter { it.timestampMillis in (dayStartEpoch - 8 * 3600_000L)..(dayStartEpoch + 26 * 3600_000L) }
            val quickPhysBlocks = physLogs.mapNotNull { log ->
                val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
                val minOfDay = logZdt.hour * 60 + logZdt.minute
                val duration = log.durationMinutes.coerceIn(5, 240)
                when (log.type) {
                    PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + duration) % 1440,
                        category = CognitiveBlockCategory.MEAL,
                        intensity = log.intensity,
                        label = "Quick Logged Meal #${log.timestampMillis}",
                        explicitStartMillis = log.timestampMillis,
                        explicitEndMillis = log.timestampMillis + duration * 60_000L
                    )
                    PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + duration) % 1440,
                        category = CognitiveBlockCategory.EXERCISE,
                        intensity = log.intensity,
                        label = "Quick Logged Workout #${log.timestampMillis}",
                        explicitStartMillis = log.timestampMillis,
                        explicitEndMillis = log.timestampMillis + duration * 60_000L
                    )
                    PhysiologicalEventType.NAP -> null
                }
            }
            val quickPhysNaps = physLogs
                .filter { it.type == PhysiologicalEventType.NAP }
                .map { log ->
                    DaytimeNapBout(
                        startMillis = log.timestampMillis,
                        endMillis = log.timestampMillis + log.durationMinutes.coerceIn(5, 180) * 60_000L
                    )
                }

            val stimulantDoses = (
                parseStimulantLogs(prefs.stimulantLogsJson) +
                    extractStimulantDosesFromSlots(prefs.protectedTimeSlots, currentDate, zoneId)
                ).distinctBy { it.timestampMillis to it.type }
            val basePhysioBlocks = buildDailyCognitiveBlocks(
                slots = prefs.protectedTimeSlots,
                date = currentDate,
                zoneId = zoneId
            ).filter {
                it.category == CognitiveBlockCategory.MEAL ||
                    it.category == CognitiveBlockCategory.EXERCISE
            }
            val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks

            // Adaptive cross-day load balancing: blend urgent deadlines through tomorrow with smoothed horizon load
            // Measure all task durations in calibrated buffered minutes so the budget unit matches TaskSchedulerService
            fun bufferedMinutes(t: SchedulableTask): Int {
                val raw = max(5, t.estimatedMinutes)
                return if (t.isDurationExplicit) raw else (raw * calibrationRatio).roundToInt().coerceAtLeast(5)
            }
            val remainingHorizonDays = (safeHorizon - dayOffset).coerceAtLeast(1)
            val unlockedCandidates = todayCandidates.filterNot { it.isLocked }
            val lockedMinutesToday = todayCandidates
                .filter { it.isLocked && it.scheduledStartMillis != null }
                .sumOf { bufferedMinutes(it) }
            val remainingBaseCapacity = (dailyCapacityMinutes - lockedMinutesToday).coerceAtLeast(0)
            val totalRemainingUnlockedMinutes = unlockedCandidates.sumOf { bufferedMinutes(it) }
            val horizonDeadlineEpoch = currentDate.plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val urgentTodayMinutes = unlockedCandidates
                .filter { t ->
                    (t.deadlineMillis != null && t.deadlineMillis <= horizonDeadlineEpoch) ||
                        t.isFrog ||
                        unlockedCandidates.any { child -> t.id in child.prerequisiteIds }
                }
                .sumOf { bufferedMinutes(it) }
            val topPriorityTaskMinutes = unlockedCandidates
                .maxByOrNull { (it.priority * 20) + (if (it.isFrog) 30 else 0) + (if (it.isPersonalTask) 15 else 0) }
                ?.let { bufferedMinutes(it) }
                ?: 0
            val minUnlockedFloor = min(remainingBaseCapacity, topPriorityTaskMinutes)
            val smoothedShare = ceil((totalRemainingUnlockedMinutes.toDouble() / remainingHorizonDays) * 1.15).toInt()
            val targetUnlockedBudget = maxOf(smoothedShare, urgentTodayMinutes, minUnlockedFloor)
                .coerceIn(0, remainingBaseCapacity)
            val dayCognitiveBudget = (lockedMinutesToday + targetUnlockedBudget).coerceIn(0, dailyCapacityMinutes)

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
                calibrationRatio = calibrationRatio,
                naps = quickPhysNaps.sortedBy { it.startMillis },
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = priorityWeights,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAvgSleep,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                activeUncompletedTaskIds = activeUncompletedIds,
                workDayStartHour = prefs.workDayStart,
                workDayEndHour = prefs.workDayEnd,
                dailyCognitiveCapacityMinutes = dayCognitiveBudget,
                rejectionPatternsJson = rejectionPatternsJson
            )

            // Enforce cognitive capacity boundary on placed tasks to guarantee overflow
            var accumulatedMinutes = 0
            val effectivePlacements = mutableListOf<ScheduledPlacement>()
            val overflowDeferrals = mutableListOf<DeferralRationale>()
            for (p in outcome.placements) {
                val dur = p.window.durationMinutes
                if (!p.task.isLocked && accumulatedMinutes + dur > dayCognitiveBudget) {
                    overflowDeferrals.add(
                        DeferralRationale(
                            taskId = p.task.id,
                            reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                            explanation = "Exceeded daily ${dayCognitiveBudget}m budget; carried to next day.",
                            actionableSuggestion = "Scheduled on subsequent horizon day."
                        )
                    )
                    continue
                }
                effectivePlacements.add(p)
                accumulatedMinutes += dur
            }

            val combinedDeferrals = (outcome.deferrals + overflowDeferrals).distinctBy { it.taskId }
            dayOutcomes[currentDate] = SchedulingOutcome(effectivePlacements, combinedDeferrals)

            // Placed tasks are removed from the carried-over pool
            val placedIds = effectivePlacements.map { it.task.id }.toSet()
            carriedOverTasks.removeAll { it.id in placedIds }

            // Detect cross-day splits: Part i placed today without Part i+1 fitting today
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
                    val hasNextPart = tasks.any { t ->
                        (t.id.startsWith(baseId) || t.parentTaskId == baseId) && (
                            t.id.endsWith("_part$nextPartNum") ||
                            t.id.endsWith("#part$nextPartNum") ||
                            t.id.contains(Regex("""[#_]part$nextPartNum(\D|$)""")) ||
                            t.title.contains("(Part $nextPartNum/")
                        )
                    }
                    val nextPartPlaced = effectivePlacements.any {
                        it.task.id.startsWith(baseId) && (
                            it.task.id.endsWith("_part$nextPartNum") ||
                            it.task.id.endsWith("#part$nextPartNum") ||
                            it.task.id.contains(Regex("""[#_]part$nextPartNum(\D|$)""")) ||
                            it.task.title.contains("(Part $nextPartNum/")
                        )
                    }
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
