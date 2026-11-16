package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.bedTimeMillis
import com.neuroflow.app.data.local.entity.wakeTimeMillis
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskSchedulerService
import kotlinx.coroutines.flow.firstOrNull
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
    ): MultiDaySchedulingOutcome {
        val dayOutcomes = mutableMapOf<LocalDate, SchedulingOutcome>()
        val crossDaySplits = mutableListOf<CrossDaySplitInfo>()
        val carriedOverTasks = tasks.toMutableList()

        val prefs = preferencesDataStore?.preferencesFlow?.firstOrNull() ?: UserPreferences()
        val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.firstOrNull() ?: emptyList()
        val recentSleep = sleepLogs.maxByOrNull { it.wakeTimeMillis }

        val dailyCapacityMinutes = prefs.dailyDeepWorkTargetMinutes.coerceIn(30, 720)

        val rawWakeHour = prefs.wakeTimeHour.coerceIn(4, 12)
        val wakeHour = rawWakeHour
        val wakeMinute = prefs.wakeTimeMinute.coerceIn(0, 59)

        val sleepDurationMinutes = recentSleep?.let {
            ((it.wakeTimeMillis - it.bedTimeMillis) / 60_000L).toInt().coerceIn(240, 660)
        } ?: 480

        for (dayOffset in 0 until horizonDays.coerceIn(1, 14)) {
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

            val todayCandidates = carriedOverTasks.toList()
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
            val chronotype = com.neuroflow.app.domain.service.ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
            val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
                (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
            } else null
            val stimulantDoses = (
                com.neuroflow.app.data.local.parseStimulantLogs(prefs.stimulantLogsJson) +
                com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(prefs.protectedTimeSlots, currentDate, zoneId)
            ).distinctBy { it.timestampMillis to it.type }
            val physiologicalBlocks = com.neuroflow.app.domain.model.buildDailyCognitiveBlocks(
                slots = prefs.protectedTimeSlots,
                date = currentDate,
                zoneId = zoneId
            ).filter {
                it.category == com.neuroflow.app.domain.service.CognitiveBlockCategory.MEAL ||
                    it.category == com.neuroflow.app.domain.service.CognitiveBlockCategory.EXERCISE
            }

            val outcome = schedulerService.scheduleDayWithOutcome(
                tasks = todayCandidates,
                dayDate = currentDate,
                nowMillis = dayNowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId,
                busyWindows = busyWindows,
                protectedSlots = protectedSlots,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                workDayStartHour = prefs.workDayStart,
                workDayEndHour = prefs.workDayEnd,
                dailyCognitiveCapacityMinutes = dailyCapacityMinutes,
                rejectionPatternsJson = rejectionPatternsJson
            )

            // Enforce cognitive capacity boundary on placed tasks to guarantee overflow
            var accumulatedMinutes = 0
            val effectivePlacements = mutableListOf<ScheduledPlacement>()
            val overflowDeferrals = mutableListOf<DeferralRationale>()
            for (p in outcome.placements) {
                val dur = p.window.durationMinutes
                if (!p.task.isLocked && accumulatedMinutes + dur > dailyCapacityMinutes) {
                    overflowDeferrals.add(
                        DeferralRationale(
                            taskId = p.task.id,
                            reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                            explanation = "Exceeded daily ${dailyCapacityMinutes}m capacity; carried to next day.",
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
                    if (hasNextPart && !nextPartPlaced && dayOffset + 1 < horizonDays) {
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
