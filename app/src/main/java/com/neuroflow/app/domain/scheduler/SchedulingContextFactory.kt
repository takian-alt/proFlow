package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.PhysiologicalEventLog
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
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
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.LearnedCircadianProfile
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TimeSlotWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Shared user-level scheduling signals computed once per scheduling pass (single-day or multi-day)
 * so SchedulePreparationOrchestrator, DailyBlueprintOrchestrator, MultiDayScheduleOrchestrator,
 * and ScheduleAutoTasksWorker always operate on identical circadian, calibration, and behavioral inputs.
 */
data class SchedulingHorizonContext(
    val planningCalibration: AnalyticsEngine.PlanningCalibration,
    val enjoymentCalibration: AnalyticsEngine.EnjoymentCalibration,
    val globalCalibrationRatio: Double,
    val categoryCalibrationRatios: Map<TaskCategory, Double>,
    val priorityWeights: PriorityWeightProfile,
    val chronotype: ChronotypeProfile,
    val manualPeakStartHour: Int?,
    val rollingAverageSleepMinutes: Double?,
    val socialJetlagLagHours: Double,
    val learnedProfile: LearnedCircadianProfile?,
    val activeUncompletedTaskIds: Set<String>,
    val allQuickPhysLogs: List<PhysiologicalEventLog>,
    val parsedStimulantLogs: List<StimulantDoseBout>,
    val sleepLogs: List<SleepLogEntity>
) {
    fun calibrationRatioFor(category: TaskCategory): Double =
        (categoryCalibrationRatios[category] ?: globalCalibrationRatio).coerceIn(0.8, 1.6)
}

/**
 * Per-day resolved scheduling context built from a [SchedulingHorizonContext].
 */
data class SchedulingDayContext(
    val date: LocalDate,
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dayNowMillis: Long,
    val wakeTimeMillis: Long,
    val lastSleepDurationMinutes: Int,
    val protectedSlots: List<ProtectedLocalSlot>,
    val naps: List<DaytimeNapBout>,
    val physiologicalBlocks: List<CognitiveBlockWindow>,
    val stimulantDoses: List<StimulantDoseBout>
)

object SchedulingContextFactory {

    fun buildHorizonContext(
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        referenceNowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        defaultGlobalCalibrationWhenEmpty: Double = 1.0
    ): SchedulingHorizonContext {
        val planningCalibration = if (allKnownTasks.isNotEmpty()) {
            AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        } else {
            AnalyticsEngine.PlanningCalibration.UNIFORM
        }
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(allKnownTasks)
        val globalCalibrationRatio = if (allKnownTasks.isNotEmpty()) {
            planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)
        } else {
            defaultGlobalCalibrationWhenEmpty.coerceIn(0.8, 1.6)
        }
        fun averageFactors(vararg schedulerCategories: com.neuroflow.app.domain.scheduler.TaskCategory): Double? {
            val values = schedulerCategories.mapNotNull { planningCalibration.categoryFactor(it)?.toDouble() }
            return if (values.isEmpty()) null else values.average().coerceIn(0.8, 1.6)
        }
        val categoryCalibrationRatios = buildMap<TaskCategory, Double> {
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL,
                com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK
            )?.let { put(TaskCategory.DEEP_WORK, it) }
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE
            )?.let { put(TaskCategory.CREATIVE, it) }
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE
            )?.let { put(TaskCategory.ADMIN, it) }
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL
            )?.let { put(TaskCategory.EXERCISE, it) }
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS
            )?.let { put(TaskCategory.MINDFULNESS, it) }
            averageFactors(
                com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE
            )?.let { put(TaskCategory.GENERAL, it) }
        }

        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, referenceNowMillis, zoneId)
        val socialJetlagLagHours = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) },
            nowMillis = referenceNowMillis,
            zoneId = zoneId
        ).mondayMorningPhaseLagHours

        val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
        val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else null

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val completionObs = if (completedTasks.isNotEmpty()) {
            buildBehavioralObservations(completedTasks, closedSessions, zoneId)
        } else emptyList()
        val approvedFeedbackObs = SchedulingFeedbackStore.extractApprovedCircadianObservations(
            json = prefs.schedulingRejectionsJson,
            nowMillis = referenceNowMillis
        )
        val observations = completionObs + approvedFeedbackObs
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val activeUncompletedTaskIds = allKnownTasks
            .filter { it.status == TaskStatus.ACTIVE }
            .map { it.id }
            .toSet()

        return SchedulingHorizonContext(
            planningCalibration = planningCalibration,
            enjoymentCalibration = enjoymentCalibration,
            globalCalibrationRatio = globalCalibrationRatio,
            categoryCalibrationRatios = categoryCalibrationRatios,
            priorityWeights = SchedulableTaskMapper.toPriorityWeights(prefs),
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            rollingAverageSleepMinutes = rollingAvgSleep,
            socialJetlagLagHours = socialJetlagLagHours,
            learnedProfile = learnedProfile,
            activeUncompletedTaskIds = activeUncompletedTaskIds,
            allQuickPhysLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson),
            parsedStimulantLogs = parseStimulantLogs(prefs.stimulantLogsJson),
            sleepLogs = sleepLogs
        )
    }

    fun buildDayContext(
        horizon: SchedulingHorizonContext,
        dayDate: LocalDate,
        prefs: UserPreferences,
        referenceNowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        sleepTelemetryRepository: SleepTelemetryRepository? = null
    ): SchedulingDayContext {
        val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEndMillis = dayDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val isToday = referenceNowMillis in dayStartMillis until dayEndMillis
        val dayNowMillis = if (isToday) referenceNowMillis else dayStartMillis

        val wakeHour = prefs.wakeTimeHour.coerceIn(0, 23)
        val wakeMinute = prefs.wakeTimeMinute.coerceIn(0, 59)
        var wakeAnchor = dayDate.atTime(wakeHour, wakeMinute).atZone(zoneId)
        val todayWakeMillis = wakeAnchor.toInstant().toEpochMilli()
        if (isToday && todayWakeMillis > referenceNowMillis) {
            if (todayWakeMillis - referenceNowMillis > 2 * 3600_000L) {
                wakeAnchor = wakeAnchor.minusDays(1)
            }
        }
        val baseWakeMillis = wakeAnchor.toInstant().toEpochMilli()

        val recentSleep = horizon.sleepLogs.maxByOrNull { it.wakeTimeMillis }
        val sleepSignal = if (isToday && sleepTelemetryRepository != null && horizon.sleepLogs.isNotEmpty()) {
            sleepTelemetryRepository.normalizeLogs(
                logs = horizon.sleepLogs,
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
        } else null

        val wakeTimeMillis = when {
            sleepSignal != null && sleepSignal.normalizedWakeTimeMillis > 0L -> sleepSignal.normalizedWakeTimeMillis
            isToday && recentSleep != null && recentSleep.wakeTimeMillis in dayStartMillis..dayEndMillis ->
                recentSleep.wakeTimeMillis
            else -> baseWakeMillis
        }
        val lastSleepDurationMinutes = when {
            sleepSignal != null && sleepSignal.blendedDurationMinutes > 0.0 ->
                sleepSignal.blendedDurationMinutes.roundToInt().coerceIn(60, 960)
            recentSleep != null -> ((recentSleep.wakeTimeMillis - recentSleep.bedTimeMillis) / 60_000L)
                .toInt().coerceIn(240, 660)
            else -> 480
        }

        val protectedSlots = prefs.protectedTimeSlots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .map {
                ProtectedLocalSlot(
                    startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                    startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                    durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                )
            }

        val physLogs = horizon.allQuickPhysLogs
            .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }
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

        val allNaps = ((sleepSignal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }
        val basePhysioBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = dayDate,
            zoneId = zoneId
        ).filter {
            it.category == CognitiveBlockCategory.MEAL ||
                it.category == CognitiveBlockCategory.EXERCISE
        }
        val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks
        val stimulantDoses = (
            horizon.parsedStimulantLogs +
                extractStimulantDosesFromSlots(prefs.protectedTimeSlots, dayDate, zoneId)
            ).distinctBy { it.timestampMillis to it.type }

        return SchedulingDayContext(
            date = dayDate,
            dayStartMillis = dayStartMillis,
            dayEndMillis = dayEndMillis,
            dayNowMillis = dayNowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            protectedSlots = protectedSlots,
            naps = allNaps,
            physiologicalBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses
        )
    }
}
