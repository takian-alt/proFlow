package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskSchedulerService
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

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
    private val calendarSyncRepository: CalendarSyncRepository? = null
) {
    suspend fun orchestrateMultiDaySchedule(
        tasks: List<SchedulableTask>,
        startDate: LocalDate,
        horizonDays: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceNowMillis: Long = System.currentTimeMillis(),
        rejectionPatternsJson: String = "{}"
    ): MultiDaySchedulingOutcome {
        val dayOutcomes = mutableMapOf<LocalDate, SchedulingOutcome>()
        val crossDaySplits = mutableListOf<CrossDaySplitInfo>()
        val carriedOverTasks = tasks.toMutableList()

        for (dayOffset in 0 until horizonDays.coerceIn(1, 14)) {
            val currentDate = startDate.plusDays(dayOffset.toLong())
            val dayStartEpoch = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEndEpoch = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            val dayNowMillis = if (dayOffset == 0) {
                referenceNowMillis.coerceIn(dayStartEpoch, dayEndEpoch - 60_000L)
            } else {
                dayStartEpoch
            }

            val wakeTimeMillis = dayStartEpoch + (7 * 3600_000L) // 07:00 wake anchor

            val busyWindows = calendarSyncRepository?.getBusyWindows(dayStartEpoch, dayEndEpoch) ?: emptyList()

            val todayCandidates = carriedOverTasks.toList()
            if (todayCandidates.isEmpty()) {
                dayOutcomes[currentDate] = SchedulingOutcome(emptyList(), emptyList())
                continue
            }

            val outcome = schedulerService.scheduleDayWithOutcome(
                tasks = todayCandidates,
                dayDate = currentDate,
                nowMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = 480,
                zoneId = zoneId,
                busyWindows = busyWindows,
                rejectionPatternsJson = rejectionPatternsJson
            )

            dayOutcomes[currentDate] = outcome

            // Placed tasks are removed from the carried-over pool
            val placedIds = outcome.placements.map { it.task.id }.toSet()
            carriedOverTasks.removeAll { it.id in placedIds }

            // Detect cross-day splits: Part 1 placed today without Part 2 fitting today
            for (p in outcome.placements) {
                if (p.task.id.endsWith("_part1") || p.task.id.endsWith("#part1")) {
                    val baseId = p.task.id.substringBefore("_part1").substringBefore("#part1")
                    val part2Placed = outcome.placements.any {
                        it.task.id.startsWith(baseId) && (it.task.id.endsWith("_part2") || it.task.id.endsWith("#part2"))
                    }
                    if (!part2Placed && dayOffset + 1 < horizonDays) {
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
