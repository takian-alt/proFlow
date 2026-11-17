package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.bedTimeMillis
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.wakeTimeMillis
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DailyBlueprintCoachingResult
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class BlueprintTaskItem(
    val task: TaskEntity,
    val proposedStartMillis: Long,
    val proposedEndMillis: Long,
    val energyPhase: EnergyPhase,
    val isDuringPeak: Boolean,
    val isIncluded: Boolean = true,
    val conflictWarning: String? = null
)

data class DailyBlueprint(
    val targetDateMillis: Long,
    val plannedMinutes: Int,
    val capacityTargetMinutes: Int,
    val capacityUtilizationPercent: Float,
    val aiStrategyRationale: String,
    val items: List<BlueprintTaskItem>,
    val overflowTasks: List<TaskEntity>
)

data class BlueprintApplyResult(
    val committedCount: Int,
    val committedTaskIds: List<String>
)

@Singleton
class DailyBlueprintOrchestrator(
    private val taskRepository: TaskRepository,
    private val schedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val geminiAiOnlineService: GeminiAiOnlineService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    @Inject
    constructor(
        taskRepository: TaskRepository,
        schedulerService: TaskSchedulerService,
        circadianEnergyService: CircadianEnergyService,
        geminiAiOnlineService: GeminiAiOnlineService,
        sleepTelemetryRepository: SleepTelemetryRepository? = null,
        preferencesDataStore: UserPreferencesDataStore,
        calendarSyncRepository: CalendarSyncRepository? = null
    ) : this(
        taskRepository,
        schedulerService,
        circadianEnergyService,
        geminiAiOnlineService,
        sleepTelemetryRepository,
        preferencesDataStore,
        calendarSyncRepository,
        Dispatchers.IO
    )

    suspend fun generateBlueprint(targetDateMillis: Long): DailyBlueprint = withContext(ioDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val targetInstant = Instant.ofEpochMilli(targetDateMillis)
        val targetDate = targetInstant.atZone(zoneId).toLocalDate()
        val dayStart = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val today = LocalDate.now(zoneId)
        val isToday = targetDate == today
        val nowMillis = System.currentTimeMillis()
        val dayNowMillis = if (isToday && nowMillis in dayStart..dayEnd) nowMillis else dayStart

        val prefs = preferencesDataStore.preferencesFlow.firstOrNull() ?: UserPreferences()
        val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.firstOrNull() ?: emptyList()
        val recentSleep = sleepLogs.maxByOrNull { it.wakeTimeMillis }

        val dailyCapacityMinutes = prefs.dailyDeepWorkTargetMinutes.coerceIn(30, 720)

        val wakeHour = prefs.wakeTimeHour.coerceIn(4, 12)
        val wakeMinute = prefs.wakeTimeMinute.coerceIn(0, 59)
        val sleepDurationMinutes = recentSleep?.let {
            ((it.wakeTimeMillis - it.bedTimeMillis) / 60_000L).toInt().coerceIn(240, 660)
        } ?: 480

        val calculatedWakeEpoch = dayStart + (wakeHour * 3600_000L) + (wakeMinute * 60_000L)
        val wakeTimeMillis = if (recentSleep != null && recentSleep.wakeTimeMillis in dayStart..dayEnd) {
            recentSleep.wakeTimeMillis
        } else {
            calculatedWakeEpoch
        }

        val allActiveTasks = taskRepository.getActiveTasks()
        val allKnownTasks = taskRepository.getAllTasks().ifEmpty { allActiveTasks }
        val planningCalibration = AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(allKnownTasks)
        val globalCalibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)
        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, dayNowMillis, zoneId)
        val socialJetlagLagHours = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) },
            nowMillis = dayNowMillis,
            zoneId = zoneId
        ).mondayMorningPhaseLagHours

        val candidateTasks = allActiveTasks.filter { task ->
            val effectiveDate = task.effectiveScheduledDateMillis()
            val isEligible = (effectiveDate != null && effectiveDate in dayStart..dayEnd) || (isToday && task.scheduledDate == null)
            isEligible && AutoSchedulingContracts.isMutableByAutoScheduler(task, dayNowMillis)
        }
        val taskById = candidateTasks.associateBy { it.id }

        val externalBusyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd + 1) ?: emptyList()
        val fixedTaskEntries = allActiveTasks.mapNotNull { t ->
            if (t.id in taskById) return@mapNotNull null
            if (t.scheduledTime == null) return@mapNotNull null
            val start = t.resolveScheduledEpochMillis(dayStart) ?: return@mapNotNull null
            val dur = TaskBlockPlan.plannedMinutes(t).coerceIn(5, 480)
            val end = start + dur * 60_000L
            if (start < dayEnd + 1 && end > dayStart) {
                t to TimeSlotWindow(start, end)
            } else null
        }
        val fixedTaskBusyWindows = fixedTaskEntries.map { it.second }
        val busyWindows = externalBusyWindows + fixedTaskBusyWindows

        fun isCognitiveDemand(t: TaskEntity): Boolean =
            t.energyLevel != EnergyLevel.LOW || t.isFrog || t.taskType == TaskType.ANALYTICAL

        val completedMinutesToday = allKnownTasks
            .filter {
                it.status == TaskStatus.COMPLETED &&
                    it.effectiveScheduledDateMillis() in dayStart..dayEnd &&
                    isCognitiveDemand(it)
            }
            .sumOf { TaskBlockPlan.plannedMinutes(it) }
        val fixedCommittedMinutes = fixedTaskEntries
            .filter { (t, _) -> t.quadrant != Quadrant.ELIMINATE && isCognitiveDemand(t) }
            .sumOf { (_, win) -> win.durationMinutes }
        val remainingCapacity = (dailyCapacityMinutes - (completedMinutesToday + fixedCommittedMinutes)).coerceAtLeast(0)

        val protectedSlots = prefs.protectedTimeSlots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStart) }
            .map {
                ProtectedLocalSlot(
                    startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                    startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                    durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                )
            }
        val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
        val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else null
        val weights = SchedulableTaskMapper.toPriorityWeights(prefs)
        val activeUncompletedTaskIds = allActiveTasks.map { it.id }.toSet()

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStart - 8 * 3600_000L)..(dayStart + 26 * 3600_000L) }
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
        val allNaps = quickPhysNaps.sortedBy { it.startMillis }

        val stimulantDoses = (
            parseStimulantLogs(prefs.stimulantLogsJson) +
                extractStimulantDosesFromSlots(prefs.protectedTimeSlots, targetDate, zoneId)
            ).distinctBy { it.timestampMillis to it.type }
        val basePhysioBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = targetDate,
            zoneId = zoneId
        ).filter {
            it.category == CognitiveBlockCategory.MEAL ||
                it.category == CognitiveBlockCategory.EXERCISE
        }
        val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val observations = buildBehavioralObservations(completedTasks, emptyList(), zoneId)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val rawSchedulableTasks = candidateTasks.map { task ->
            SchedulableTaskMapper.toSchedulableTask(
                task = task,
                allContextTasks = candidateTasks,
                dayStartMillis = dayStart,
                enjoymentCalibration = enjoymentCalibration
            )
        }
        val schedulableTasks = DependencyGraphValidator.sanitizeDag(rawSchedulableTasks).tasks

        val outcome = if (schedulableTasks.isNotEmpty()) {
            schedulerService.scheduleDayWithOutcome(
                tasks = schedulableTasks,
                dayDate = targetDate,
                nowMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                busyWindows = busyWindows,
                protectedSlots = protectedSlots,
                calibrationRatio = globalCalibrationRatio,
                zoneId = zoneId,
                naps = allNaps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = weights,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAvgSleep,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                activeUncompletedTaskIds = activeUncompletedTaskIds,
                workDayStartHour = prefs.workDayStart,
                workDayEndHour = prefs.workDayEnd,
                dailyCognitiveCapacityMinutes = remainingCapacity,
                rejectionPatternsJson = prefs.schedulingRejectionsJson
            )
        } else {
            SchedulingOutcome(emptyList(), emptyList())
        }

        val dailySummary = circadianEnergyService.computeDailySummary(
            timestampMillis = dayStart,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            zoneId = zoneId,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            cognitiveBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses
        )

        val includedItems = mutableListOf<BlueprintTaskItem>()
        val overflowTaskList = mutableListOf<TaskEntity>()
        val includedTaskIds = mutableSetOf<String>()
        var accumulatedMinutes = 0

        for (placement in outcome.placements) {
            val baseId = placement.task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val sourceTask = taskById[placement.task.id] ?: taskById[baseId] ?: continue
            val isSplitPart = placement.task.id != sourceTask.id &&
                (placement.task.id.contains("#part") || placement.task.id.contains("_part"))
            val displayTask = if (isSplitPart) {
                sourceTask.copy(
                    id = placement.task.id,
                    title = placement.task.title,
                    parentTaskId = baseId,
                    plannedBlockMinutes = placement.window.durationMinutes
                )
            } else {
                sourceTask
            }
            val dur = if (isSplitPart) {
                placement.window.durationMinutes.takeIf { it > 0 }
                    ?: TaskBlockPlan.plannedMinutes(displayTask).coerceAtLeast(1)
            } else {
                TaskBlockPlan.plannedMinutes(displayTask).coerceAtLeast(1)
            }
            if (!displayTask.isScheduleLocked && (accumulatedMinutes + dur > remainingCapacity)) {
                if (overflowTaskList.none { it.id == sourceTask.id }) {
                    overflowTaskList.add(sourceTask)
                }
            } else {
                includedTaskIds.add(sourceTask.id)
                accumulatedMinutes += dur

                val useBasicSnapshot = chronotype == ChronotypeProfile.INTERMEDIATE &&
                    manualPeakStartHour == null &&
                    stimulantDoses.isEmpty() &&
                    physiologicalBlocks.isEmpty() &&
                    allNaps.isEmpty() &&
                    learnedProfile == null
                val snapshot = if (useBasicSnapshot) {
                    circadianEnergyService.evaluateSnapshot(
                        timestampMillis = placement.window.startMillis,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = sleepDurationMinutes,
                        continuousActiveFocusMinutes = 0,
                        zoneId = zoneId
                    )
                } else {
                    circadianEnergyService.evaluateSnapshotWithStimulants(
                        timestampMillis = placement.window.startMillis,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = sleepDurationMinutes,
                        continuousActiveFocusMinutes = 0,
                        zoneId = zoneId,
                        naps = allNaps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAvgSleep,
                        cognitiveBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours
                    )
                }
                val zdt = Instant.ofEpochMilli(placement.window.startMillis).atZone(zoneId)
                val h = zdt.hour
                val inPeakHour = if (dailySummary.peakWindowStartHour <= dailySummary.peakWindowEndHour) {
                    h in dailySummary.peakWindowStartHour until dailySummary.peakWindowEndHour
                } else {
                    h >= dailySummary.peakWindowStartHour || h < dailySummary.peakWindowEndHour
                }
                val isDuringPeak = snapshot.phase == EnergyPhase.PEAK || snapshot.phase == EnergyPhase.SURGE || snapshot.phase == EnergyPhase.MORNING_RAMP || inPeakHour

                val hasConflict = busyWindows.any { bw ->
                    placement.window.startMillis < bw.endMillis && placement.window.endMillis > bw.startMillis
                }

                val proposedEnd = if (placement.window.endMillis > placement.window.startMillis) {
                    placement.window.endMillis
                } else {
                    placement.window.startMillis + dur * 60_000L
                }

                includedItems.add(
                    BlueprintTaskItem(
                        task = displayTask,
                        proposedStartMillis = placement.window.startMillis,
                        proposedEndMillis = proposedEnd,
                        energyPhase = snapshot.phase,
                        isDuringPeak = isDuringPeak,
                        isIncluded = true,
                        conflictWarning = if (hasConflict) "Overlaps with external calendar event" else null
                    )
                )
            }
        }

        for (candidate in candidateTasks) {
            if (candidate.id !in includedTaskIds && overflowTaskList.none { it.id == candidate.id }) {
                overflowTaskList.add(candidate)
            }
        }

        val plannedMinutes = accumulatedMinutes
        val capacityUtilizationPercent = if (dailyCapacityMinutes > 0) {
            (plannedMinutes.toFloat() / dailyCapacityMinutes) * 100f
        } else {
            0f
        }

        val peakStartHour = prefs.effectivePeakStart.takeIf { it in 0..23 }
            ?: dailySummary.peakWindowStartHour.takeIf { it in 0..23 }
            ?: prefs.peakEnergyStart.coerceIn(0, 23)

        val peakEndHour = prefs.effectivePeakEnd.takeIf { it in 0..23 }
            ?: dailySummary.peakWindowEndHour.takeIf { it in 0..23 }
            ?: prefs.peakEnergyEnd.coerceIn(0, 23)

        val coachingResult = try {
            geminiAiOnlineService.generateDailyBlueprintCoaching(
                tasks = includedItems.map { it.task },
                plannedMinutes = plannedMinutes,
                capacityMinutes = dailyCapacityMinutes,
                peakStartHour = peakStartHour,
                peakEndHour = peakEndHour,
                apiKey = prefs.geminiApiKey.takeIf { it.isNotBlank() },
                userDirective = prefs.adaptiveSystemDirective
            )
        } catch (e: Exception) {
            DailyBlueprintCoachingResult(
                headline = "Daily Blueprint Plan",
                strategyBody = "Planned $plannedMinutes mins across ${includedItems.size} tasks against ${dailyCapacityMinutes}m target.",
                peakRecommendation = "Focus on highest-priority tasks during peak hours."
            )
        }

        val aiStrategyRationale = when {
            coachingResult.headline.isNotBlank() && coachingResult.strategyBody.isNotBlank() ->
                "${coachingResult.headline}: ${coachingResult.strategyBody}"
            coachingResult.strategyBody.isNotBlank() -> coachingResult.strategyBody
            else -> coachingResult.headline
        }

        DailyBlueprint(
            targetDateMillis = dayStart,
            plannedMinutes = plannedMinutes,
            capacityTargetMinutes = dailyCapacityMinutes,
            capacityUtilizationPercent = capacityUtilizationPercent,
            aiStrategyRationale = aiStrategyRationale,
            items = includedItems,
            overflowTasks = overflowTaskList
        )
    }

    suspend fun applyBlueprint(
        blueprint: DailyBlueprint,
        includedTaskIds: Set<String>
    ): BlueprintApplyResult = withContext(ioDispatcher) {
        val tasksToApply = blueprint.items.filter { it.task.id in includedTaskIds }
        val committedTaskIds = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (item in tasksToApply) {
            val isPart2 = item.task.id.endsWith("#part2") || item.task.id.endsWith("_part2")
            val baseId = item.task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val persistId = if (isPart2) item.task.id else baseId
            val existing = taskRepository.getById(persistId)
                ?: if (!isPart2) taskRepository.getById(item.task.id) else null
            val startDay = com.neuroflow.app.data.local.entity.startOfLocalDay(item.proposedStartMillis)
            val timeOffset = (item.proposedStartMillis - startDay).coerceAtLeast(0L)
            val durMinutes = ((item.proposedEndMillis - item.proposedStartMillis) / 60_000L).toInt().coerceAtLeast(1)

            if (isPart2 && existing == null) {
                val baseTask = taskRepository.getById(baseId) ?: item.task
                val part2Task = baseTask.copy(
                    id = item.task.id,
                    title = item.task.title,
                    parentTaskId = baseId,
                    dependsOnTaskIds = baseId,
                    estimatedDurationMinutes = durMinutes,
                    plannedBlockMinutes = durMinutes,
                    scheduledDate = startDay,
                    scheduledTime = timeOffset,
                    isAutoScheduled = true,
                    isFrog = false,
                    recurrence = com.neuroflow.app.domain.model.Recurrence.NONE,
                    recurrenceIntervalDays = 0,
                    isHabitual = false,
                    habitDate = null,
                    updatedAt = now
                )
                taskRepository.insert(part2Task)
                calendarSyncRepository?.handleTaskStateChanged(part2Task)
                committedTaskIds.add(part2Task.id)
            } else {
                val targetTask = existing ?: item.task
                val isPart1 = item.task.id.contains("#part1") || item.task.id.contains("_part1")
                val updatedTask = targetTask.copy(
                    scheduledDate = startDay,
                    scheduledTime = timeOffset,
                    habitDate = if (targetTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) startDay else targetTask.habitDate,
                    isAutoScheduled = true,
                    plannedBlockMinutes = if (isPart2 || isPart1) durMinutes else targetTask.plannedBlockMinutes,
                    lastAutoScheduledAt = now,
                    updatedAt = now
                )
                taskRepository.update(updatedTask)
                calendarSyncRepository?.handleTaskStateChanged(updatedTask)
                committedTaskIds.add(updatedTask.id)

                if (!isPart2 && !isPart1) {
                    val stalePart2 = taskRepository.getById("${baseId}#part2")
                        ?: taskRepository.getById("${baseId}_part2")
                    if (stalePart2 != null && stalePart2.status != TaskStatus.COMPLETED) {
                        calendarSyncRepository?.deleteTaskEvent(stalePart2.id)
                        taskRepository.delete(stalePart2)
                    }
                }
            }
        }

        BlueprintApplyResult(
            committedCount = committedTaskIds.size,
            committedTaskIds = committedTaskIds
        )
    }
}
