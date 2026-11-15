package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.bedTimeMillis
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.wakeTimeMillis
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DailyBlueprintCoachingResult
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
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
class DailyBlueprintOrchestrator @Inject constructor(
    private val taskRepository: TaskRepository,
    private val schedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val geminiAiOnlineService: GeminiAiOnlineService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

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
        val candidateTasks = allActiveTasks.filter { task ->
            val effectiveDate = task.effectiveScheduledDateMillis()
            val isEligible = (effectiveDate != null && effectiveDate in dayStart..dayEnd) || (isToday && task.scheduledDate == null)
            isEligible && AutoSchedulingContracts.isMutableByAutoScheduler(task, dayNowMillis)
        }
        val taskById = candidateTasks.associateBy { it.id }

        val busyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd + 1) ?: emptyList()

        val schedulableTasks = candidateTasks.map { task ->
            val category = when (task.quadrant) {
                Quadrant.DO_FIRST -> TaskCategory.DEEP_WORK
                Quadrant.SCHEDULE -> TaskCategory.DEEP_WORK
                Quadrant.DELEGATE -> TaskCategory.ADMIN
                Quadrant.ELIMINATE -> TaskCategory.GENERAL
            }
            val energyDemand = when (task.energyLevel) {
                EnergyLevel.HIGH -> EnergyDemand.HIGH
                EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                else -> EnergyDemand.LOW
            }
            val priority = when (task.quadrant) {
                Quadrant.DO_FIRST -> 2
                Quadrant.SCHEDULE -> 1
                else -> 0
            }
            SchedulableTask(
                id = task.id,
                title = task.title,
                category = category,
                priority = priority,
                estimatedMinutes = TaskBlockPlan.plannedMinutes(task),
                energyDemand = energyDemand,
                isFrog = task.isFrog,
                deadlineMillis = task.resolveDeadlineEpochMillis(dayStart),
                isLocked = task.isScheduleLocked
            )
        }

        val outcome = if (schedulableTasks.isNotEmpty()) {
            schedulerService.scheduleDayWithOutcome(
                tasks = schedulableTasks,
                dayDate = targetDate,
                nowMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                busyWindows = busyWindows,
                zoneId = zoneId,
                dailyCognitiveCapacityMinutes = dailyCapacityMinutes,
                rejectionPatternsJson = prefs.schedulingRejectionsJson
            )
        } else {
            SchedulingOutcome(emptyList(), emptyList())
        }

        val dailySummary = circadianEnergyService.computeDailySummary(
            timestampMillis = dayStart,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            zoneId = zoneId
        )

        val includedItems = mutableListOf<BlueprintTaskItem>()
        val overflowTaskList = mutableListOf<TaskEntity>()
        val includedTaskIds = mutableSetOf<String>()
        var accumulatedMinutes = 0

        for (placement in outcome.placements) {
            val task = taskById[placement.task.id] ?: continue
            val dur = TaskBlockPlan.plannedMinutes(task).coerceAtLeast(1)
            if (!task.isScheduleLocked && (accumulatedMinutes + dur > dailyCapacityMinutes)) {
                if (overflowTaskList.none { it.id == task.id }) {
                    overflowTaskList.add(task)
                }
            } else {
                includedTaskIds.add(task.id)
                accumulatedMinutes += dur

                val snapshot = circadianEnergyService.evaluateSnapshot(
                    timestampMillis = placement.window.startMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = sleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId
                )
                val zdt = Instant.ofEpochMilli(placement.window.startMillis).atZone(zoneId)
                val h = zdt.hour
                val inPeakHour = if (dailySummary.peakWindowStartHour <= dailySummary.peakWindowEndHour) {
                    h in dailySummary.peakWindowStartHour until dailySummary.peakWindowEndHour
                } else {
                    h >= dailySummary.peakWindowStartHour || h < dailySummary.peakWindowEndHour
                }
                val isDuringPeak = snapshot.phase == EnergyPhase.PEAK || snapshot.phase == EnergyPhase.SURGE || inPeakHour

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
                        task = task,
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

        for (item in tasksToApply) {
            val existing = taskRepository.getById(item.task.id) ?: item.task
            val updatedTask = existing.copy(
                scheduledDate = blueprint.targetDateMillis,
                scheduledTime = item.proposedStartMillis,
                isAutoScheduled = true,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updatedTask)
            calendarSyncRepository?.handleTaskStateChanged(updatedTask)
            committedTaskIds.add(updatedTask.id)
        }

        BlueprintApplyResult(
            committedCount = committedTaskIds.size,
            committedTaskIds = committedTaskIds
        )
    }
}
