# neuroFlow Core Engine — Part 2: Auto-Scheduling & Mathematical Placement Engine

**Description:** 4-Vector Normalized Prioritization (Urgency, Impact, Energy Fit, Momentum), Demand-Elastic Utility Damping λ(τ), Chronotype-Aware Slot Alignment, Gap-Fitting, Topological DAG Sorting, Simulated Cognitive Load, 90-min Ultradian Buffers, Android Calendar Read & Write-Back Sync with Decompression, and Background Re-scheduling Workers.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 14  
**Lines of Code:** 4,029  
**Uncompressed Size:** 178.2 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`](#appsrcmainjavacomneuroflowappdomainservicetaskschedulerservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`](#appsrcmainjavacomneuroflowappdomainschedulerautoschedulingcontractskt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`](#appsrcmainjavacomneuroflowappdomainschedulerautoschedulingrationaleskt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`](#appsrcmainjavacomneuroflowappdomainschedulerplacementstabilitykt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`](#appsrcmainjavacomneuroflowappdomainschedulerprotectedslotclockkt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`](#appsrcmainjavacomneuroflowappdomainschedulertaskblockplankt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`](#appsrcmainjavacomneuroflowappdomainschedulertasktagschedulingprofilekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`](#appsrcmainjavacomneuroflowappdomainschedulerbehavioralinsightsenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`](#appsrcmainjavacomneuroflowappdomainschedulerdependencygraphvalidatorkt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`](#appsrcmainjavacomneuroflowappdomainschedulerschedulepreparationorchestratorkt)
- [`app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`](#appsrcmainjavacomneuroflowappdatacalendarcalendarsyncdatasourcekt)
- [`app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`](#appsrcmainjavacomneuroflowappworkerscheduleautotasksworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`](#appsrcmainjavacomneuroflowappworkernotificationworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`](#appsrcmainjavacomneuroflowappworkerfocuswidgetupdateworkerkt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowappdomainservicetaskschedulerservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`
- **Lines of Code:** 1,154
- **Size:** 53,965 bytes (52.7 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class SchedulingOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

data class SchedulableTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: Int, // 0 = LOW, 1 = MEDIUM, 2 = HIGH
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean = false,
    val energyDemand: EnergyDemand,
    val isFrog: Boolean = false,
    val scheduledStartMillis: Long? = null,
    val deadlineMillis: Long? = null,
    val isLocked: Boolean = false,
    val parentTaskId: String? = null,
    val completedMicroSteps: Int = 0,
    val totalMicroSteps: Int = 0,
    val isCurrentlyActive: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val prerequisiteIds: Set<String> = emptySet(),
    val impactScore: Int = 50,
    val estimatedEnjoyment: Int = 5,
    val postponeCount: Int = 0,
    val contextTag: String = "",
    val isAnxietyTask: Boolean = false,
    val hasImplementationIntention: Boolean = false,
    val fragmentationTolerance: Float = 0.5f,
    val distractionScore: Float = 0f
)

data class TaskScoreBreakdown(
    val taskId: String,
    val totalScore: Double,      // Guaranteed [0.0..100.0]
    val urgencyScore: Double,    // [0.0..100.0]
    val impactScore: Double,     // [0.0..100.0]
    val energyFitScore: Double,  // [0.0..100.0]
    val momentumScore: Double,   // [0.0..100.0]
    val bufferedDurationMinutes: Int
)

data class TimeSlotWindow(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/** DST-safe local wall-clock rule for recurring protected slots */
data class ProtectedLocalSlot(
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int
) {
    fun resolveForDate(date: LocalDate, zoneId: ZoneId): TimeSlotWindow {
        val startZdt = date.atTime(LocalTime.of(startHour, startMinute)).atZone(zoneId)
        val endZdt = startZdt.plusMinutes(durationMinutes.toLong())
        return TimeSlotWindow(
            startMillis = startZdt.toInstant().toEpochMilli(),
            endMillis = endZdt.toInstant().toEpochMilli()
        )
    }
}

data class ScheduledPlacement(
    val task: SchedulableTask,
    val window: TimeSlotWindow,
    val scoreBreakdown: TaskScoreBreakdown
)

data class PriorityWeightProfile(
    val urgencyWeight: Double = 1.0,
    val impactWeight: Double = 1.0,
    val circadianMatchWeight: Double = 1.0,
    val momentumWeight: Double = 1.0,
    val frogBonusMultiplier: Double = 1.0,
    val quadrantWeight: Double = 1.0,
    val priorityLevelWeight: Double = 1.0,
    val durationWeight: Double = 1.0,
    val focusModeWeight: Double = 1.0,
    val planBoostWeight: Double = 1.0,
    val enjoymentWeight: Double = 1.0
) {
    companion object {
        val DEFAULT = PriorityWeightProfile()
    }
}

interface TaskSchedulerService {
    fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20
    ): TaskScoreBreakdown

    fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20,
        weights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        referenceNowMillis: Long? = null
    ): TaskScoreBreakdown

    fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double

    fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): List<ScheduledPlacement>

    fun scheduleDayWithOutcome(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): SchedulingOutcome

    companion object {
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
    }
}

@Singleton
class DefaultTaskSchedulerService @Inject constructor(
    private val circadianService: CircadianEnergyService
) : TaskSchedulerService {

    companion object {
        private const val W_URGENCY = 0.30
        private const val W_IMPACT = 0.30
        private const val W_ENERGY = 0.25
        private const val W_MOMENTUM = 0.15

        private const val EWMA_ALPHA = 0.25
        private const val TRANSITION_BUFFER_MINUTES = 5
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
    }

    override fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double
    ): TaskScoreBreakdown = evaluateTaskScoreWithWeights(
        task = task,
        snapshot = snapshot,
        dependentCount = dependentCount,
        calibrationRatio = calibrationRatio,
        weights = PriorityWeightProfile.DEFAULT,
        referenceNowMillis = null
    )

    override fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double,
        weights: PriorityWeightProfile,
        referenceNowMillis: Long?
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)
        // Bug 1 Fix (ADR-025): Anchor urgency calculation to referenceNowMillis to prevent procrastination bias
        val nowAnchor = referenceNowMillis ?: snapshot.timestampMillis

        // 1. Urgency Vector U in [0..100] with Parkinson's Law ADHD Urgency Ignition
        val postponeBonus = min(15.0, task.postponeCount * 3.0)
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - nowAnchor) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            val baseLogistic = 100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))
            val ignitionBoost = if (slackHours in 0.25..6.0) {
                12.0 * exp(-((slackHours - 2.5) * (slackHours - 2.5)) / 4.0)
            } else 0.0
            (baseLogistic + ignitionBoost + postponeBonus).coerceIn(0.0, 100.0)
        } else {
            val ageDays = max(0.0, (nowAnchor - task.createdAtMillis) / 86_400_000.0)
            min(40.0, 15.0 + 25.0 * (1.0 - exp(-ageDays / 14.0)) + postponeBonus)
        }

        // 2. Impact Vector I in [0..100]
        val rawBasePriority = when (task.priority) {
            2 -> 65.0
            1 -> 40.0
            else -> 15.0
        }
        val basePriority = (rawBasePriority * weights.priorityLevelWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.30
        } else 0.0
        val frogBonus = if (task.isFrog) (25.0 * weights.frogBonusMultiplier.coerceIn(0.5, 2.0)) else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val impact = (basePriority + impactAdj + frogBonus + dependencyBonus).coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] + Dread Desensitization Routing
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val rawCategoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val categoryAffinity = (rawCategoryAffinity * weights.focusModeWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val dreadAlignmentAdj = if (task.isAnxietyTask || task.postponeCount >= 3) {
            when (snapshot.phase) {
                EnergyPhase.PEAK, EnergyPhase.REBOUND -> 8.0
                EnergyPhase.DIP, EnergyPhase.WIND_DOWN -> -10.0
                else -> 0.0
            }
        } else 0.0
        val distractionAdj = if (task.distractionScore > 0.3f && snapshot.phase == EnergyPhase.DIP) {
            -8.0 * task.distractionScore
        } else 0.0
        val energyFit = (0.70 * demandGaussian + 0.30 * categoryAffinity + dreadAlignmentAdj + distractionAdj).coerceIn(0.0, 100.0)

        // 4. Momentum Vector M in [0..100] with Zeigarnik Pull & WOOP Implementation Intention Shield
        val progressFraction = if (task.totalMicroSteps > 0) {
            (task.completedMicroSteps.toDouble() / task.totalMicroSteps).coerceIn(0.0, 1.0)
        } else 0.0
        val zeigarnikPull = if (progressFraction > 0.0 && progressFraction < 1.0) {
            30.0 * progressFraction + 10.0 * sin(PI * progressFraction)
        } else {
            30.0 * progressFraction
        }
        val activeBonus = if (task.isCurrentlyActive) 35.0 else 0.0
        val microStepBonus = if (task.totalMicroSteps > 0) 20.0 else 0.0
        val woopBonus = if (task.hasImplementationIntention) {
            12.0 * weights.planBoostWeight.coerceIn(0.1, 3.0)
        } else 0.0
        val lowFrictionBonus = (15.0 * exp(-task.estimatedMinutes / 45.0)) * weights.durationWeight.coerceIn(0.2, 3.0)
        val enjoymentBonus = if (task.estimatedEnjoyment != 5) {
            (task.estimatedEnjoyment - 5) * 2.0 * weights.enjoymentWeight.coerceIn(0.2, 3.0)
        } else 0.0
        val momentum = (zeigarnikPull + activeBonus + microStepBonus + woopBonus + lowFrictionBonus + enjoymentBonus)
            .coerceIn(0.0, 100.0)

        // Eisenhower Quadrant Modulation
        val isImportant = task.priority >= 1 || task.impactScore >= 50 || task.isFrog
        val isUrgent = urgency >= 50.0
        val quadrantSpread = weights.quadrantWeight.coerceIn(0.0, 3.0)
        val quadrantModifier = when {
            isImportant && isUrgent -> 1.0 + 0.15 * quadrantSpread // DO_FIRST
            isImportant && !isUrgent -> 1.0 + 0.08 * quadrantSpread // SCHEDULE
            !isImportant && isUrgent -> 1.0 - 0.08 * quadrantSpread // DELEGATE
            else -> 1.0 - 0.15 * quadrantSpread // ELIMINATE
        }.coerceIn(0.5, 2.0)

        val rawWu = W_URGENCY * weights.urgencyWeight.coerceIn(0.1, 3.0)
        val rawWi = W_IMPACT * weights.impactWeight.coerceIn(0.1, 3.0)
        val rawWe = W_ENERGY * weights.circadianMatchWeight.coerceIn(0.1, 3.0)
        val rawWm = W_MOMENTUM * weights.momentumWeight.coerceIn(0.1, 3.0)
        val weightSum = (rawWu + rawWi + rawWe + rawWm).coerceAtLeast(0.001)

        val totalScore = (
            ((rawWu * urgency + rawWi * impact + rawWe * energyFit + rawWm * momentum) / weightSum) * quadrantModifier
        ).coerceIn(0.0, 100.0)

        return TaskScoreBreakdown(
            taskId = task.id,
            totalScore = totalScore,
            urgencyScore = urgency,
            impactScore = impact,
            energyFitScore = energyFit,
            momentumScore = momentum,
            bufferedDurationMinutes = bufferedMins
        )
    }

    override fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double {
        if (estimatedMinutes <= 0 || actualMinutes <= 0) return previousRatio
        val observedRatio = actualMinutes.toDouble() / estimatedMinutes.toDouble()
        return ((1.0 - EWMA_ALPHA) * previousRatio + EWMA_ALPHA * observedRatio).coerceIn(0.5, 2.5)
    }

    override fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double,
        forceReplan: Boolean,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): List<ScheduledPlacement> = scheduleDayWithOutcome(
        tasks = tasks,
        dayDate = dayDate,
        nowMillis = nowMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        busyWindows = busyWindows,
        protectedSlots = protectedSlots,
        calibrationRatio = calibrationRatio,
        forceReplan = forceReplan,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        priorityWeights = priorityWeights,
        learnedProfile = learnedProfile,
        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
        physiologicalBlocks = physiologicalBlocks,
        stimulantDoses = stimulantDoses,
        socialJetlagLagHours = socialJetlagLagHours,
        activeUncompletedTaskIds = activeUncompletedTaskIds
    ).placements

    override fun scheduleDayWithOutcome(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double,
        forceReplan: Boolean,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): SchedulingOutcome {
        if (tasks.isEmpty()) return SchedulingOutcome(emptyList())

        // 0. Cycle-Safe DAG Sanitization (ADR-020): guarantees circular dependencies never starve Kahn's queue
        val sanitizedTasks = DependencyGraphValidator.sanitizeDag(tasks).tasks

        // 1. Construct DST-safe day horizon [max(now, 06:00, wakeTime + 15m), 22:30]
        val nowLocalDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val isToday = dayDate == nowLocalDate
        val isPast = dayDate < nowLocalDate

        val earliestDayStart = dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val latestDayEnd = dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val wakeTodayMillis = dayDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
        val effectiveWakeStart = max(earliestDayStart, wakeTodayMillis + 15 * 60_000L)
        val dayEndMillis = max(latestDayEnd, effectiveWakeStart + 4 * 3600_000L)
        val dayStartMillis = when {
            isPast -> dayEndMillis
            isToday -> max(nowMillis + 5 * 60_000L, effectiveWakeStart)
            else -> effectiveWakeStart
        }
        if (dayStartMillis >= dayEndMillis) {
            return SchedulingOutcome(
                placements = emptyList(),
                deferrals = tasks.map {
                    DeferralRationale(
                        taskId = it.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Past workday end for this date",
                        actionableSuggestion = "Reschedule for tomorrow or advance wake time."
                    )
                }
            )
        }

        // 2. Build Exact Minute-Resolution Free Intervals
        var freeIntervals = mutableListOf(TimeSlotWindow(dayStartMillis, dayEndMillis))
        val napWindows = naps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val physioWindows = physiologicalBlocks.map { it.toTimeSlotWindow(dayDate, zoneId) }
        val allBlocked = busyWindows + protectedSlots.map { it.resolveForDate(dayDate, zoneId) } + napWindows + physioWindows
        for (blocked in allBlocked) {
            freeIntervals = subtractInterval(freeIntervals, blocked)
        }

        // 3. Separate Locked / Hysteresis-Anchored Tasks
        val placements = mutableListOf<ScheduledPlacement>()
        val completionEndMap = mutableMapOf<String, Long>()
        val dependentsMap = mutableMapOf<String, MutableSet<String>>()
        for (task in sanitizedTasks) {
            val allPrereqs = task.prerequisiteIds
            for (p in allPrereqs) {
                dependentsMap.getOrPut(p) { mutableSetOf() }.add(task.id)
            }
        }

        val sanitizedTaskMap = sanitizedTasks.associateBy { it.id }
        val remainingTasks = mutableListOf<SchedulableTask>()

        // Bug 5 Fix: Sort candidate anchor tasks by scheduledStartMillis ascending
        // and only anchor a task if all of its prerequisites in sanitizedTasks are already anchored
        // at an earlier timestamp (prereqEndMillis <= existingWindow.startMillis) and not blocked by active uncompleted cross-day tasks.
        val candidateAnchors = sanitizedTasks.sortedWith(
            compareBy<SchedulableTask> { it.scheduledStartMillis ?: Long.MAX_VALUE }
                .thenBy { it.id }
        )

        for (task in candidateAnchors) {
            val existingStart = task.scheduledStartMillis
            val durationMins = computeBufferedDuration(task, calibrationRatio)
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            val isHysteresisCandidate = existingWindow != null &&
                (task.isLocked || (!forceReplan && existingWindow.startMillis >= nowMillis - 15 * 60_000L)) &&
                fitsInFreeIntervals(freeIntervals, existingWindow)

            val allPrereqs = task.prerequisiteIds
            val prereqsSatisfied = isHysteresisCandidate && allPrereqs.all { prereqId ->
                if (prereqId in sanitizedTaskMap) {
                    val prereqEnd = completionEndMap[prereqId]
                    prereqEnd != null && prereqEnd <= existingWindow!!.startMillis
                } else {
                    prereqId !in activeUncompletedTaskIds
                }
            }

            if (isHysteresisCandidate && prereqsSatisfied) {
                val snap = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = existingWindow!!.startMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task, snap, dependentsMap[task.id]?.size ?: 0, calibrationRatio, priorityWeights,
                    referenceNowMillis = nowMillis
                )
                placements.add(ScheduledPlacement(task, existingWindow, breakdown))
                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(
                        existingWindow.startMillis,
                        existingWindow.endMillis + TRANSITION_BUFFER_MINUTES * 60_000L
                    )
                )
                completionEndMap[task.id] = existingWindow.endMillis
            } else {
                remainingTasks.add(task)
            }
        }

        // Feature 2 (ADR-024): Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation
        val latestAllowedEndMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val deadlineCeiling = task.deadlineMillis ?: Long.MAX_VALUE
            val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
            val directCeiling = when {
                placedWindow != null -> placedWindow.startMillis
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                else -> deadlineCeiling
            }
            latestAllowedEndMap[task.id] = min(deadlineCeiling, directCeiling)
        }

        var dagCeilingChanged = true
        var dagIterations = 0
        while (dagCeilingChanged && dagIterations < sanitizedTasks.size + 1) {
            dagCeilingChanged = false
            dagIterations++
            for (task in sanitizedTasks) {
                val childCeiling = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
                if (childCeiling == Long.MAX_VALUE) continue

                val childBufferedMins = computeBufferedDuration(task, calibrationRatio)
                val childDurationMillis = childBufferedMins * 60_000L
                val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
                val childLatestStart = when {
                    placedWindow != null -> placedWindow.startMillis
                    task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                    else -> childCeiling - childDurationMillis
                }
                val isUltradianDep = task.id.endsWith("#part2") ||
                    task.prerequisiteIds.any { it.endsWith("#part1") }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val parentMaxEnd = childLatestStart - bufferBeforeChild * 60_000L

                val allPrereqs = task.prerequisiteIds
                for (prereqId in allPrereqs) {
                    val currentParentCeiling = latestAllowedEndMap[prereqId] ?: Long.MAX_VALUE
                    if (parentMaxEnd < currentParentCeiling) {
                        latestAllowedEndMap[prereqId] = parentMaxEnd
                        dagCeilingChanged = true
                    }
                }
            }
        }

        // 4. Multi-Prerequisite DAG Topological Sort + Priority Scheduling
        val deferrals = mutableListOf<DeferralRationale>()
        val remainingIds = remainingTasks.map { it.id }.toSet()

        // Bug 4 Fix: exclude tasks with uncompleted cross-day prerequisites from today's active ready queue
        val schedulableTodayTasks = mutableListOf<SchedulableTask>()
        for (task in remainingTasks) {
            val allPrereqs = task.prerequisiteIds
            val blockedByPrereq = allPrereqs.firstOrNull { prereqId ->
                prereqId in activeUncompletedTaskIds &&
                prereqId !in remainingIds &&
                prereqId !in completionEndMap
            }
            if (blockedByPrereq != null) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.DEPENDENCY_BLOCKED,
                        explanation = "Prerequisite task '$blockedByPrereq' is active and pending on a future date.",
                        actionableSuggestion = "Complete prerequisite '$blockedByPrereq' first or adjust scheduled dates."
                    )
                )
            } else {
                schedulableTodayTasks.add(task)
            }
        }

        val schedulableTodayIds = schedulableTodayTasks.map { it.id }.toSet()
        val inDegree = mutableMapOf<String, Int>()
        val children = mutableMapOf<String, MutableList<SchedulableTask>>()

        for (task in schedulableTodayTasks) {
            val allPrereqs = task.prerequisiteIds.filter { it in schedulableTodayIds }.toSet()
            inDegree[task.id] = allPrereqs.size
            for (prereq in allPrereqs) {
                children.getOrPut(prereq) { mutableListOf() }.add(task)
            }
        }

        val nowSnapshot = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = dayStartMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
        val scoreCache = schedulableTodayTasks.associate { t ->
            t.id to evaluateTaskScoreWithWeights(t, nowSnapshot, dependentsMap[t.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
        }.toMutableMap()

        val readyQueue = PriorityQueue<SchedulableTask>(
            compareByDescending { task ->
                val baseScore = scoreCache[task.id]?.totalScore ?: 0.0
                val scarcityBonus = when (task.energyDemand) {
                    EnergyDemand.HIGH -> 12.0
                    EnergyDemand.MODERATE -> 4.0
                    EnergyDemand.LOW -> 0.0
                }
                baseScore + scarcityBonus
            }
        )
        schedulableTodayTasks.filter { (inDegree[it.id] ?: 0) == 0 }.forEach { readyQueue.add(it) }

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache[current.id]
                ?: evaluateTaskScoreWithWeights(current, nowSnapshot, dependentsMap[current.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L
            val rawDurationMillis = max(5, current.estimatedMinutes) * 60_000L

            val allPrereqs = current.prerequisiteIds
            val isUltradianPrereq = current.id.endsWith("#part2") || allPrereqs.any { it.endsWith("#part1") }
            val prereqBufferMinutes = if (isUltradianPrereq) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
            val maxPrereqEnd = allPrereqs.mapNotNull { completionEndMap[it] }.maxOrNull() ?: dayStartMillis
            val earliestStart = max(dayStartMillis, maxPrereqEnd + prereqBufferMinutes * 60_000L)

            val taskCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE
            val bestSlot = findBestCircadianWindow(
                task = current,
                durationMillis = durationMillis,
                earliestStartMillis = earliestStart,
                freeIntervals = freeIntervals,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                dependentCount = dependentsMap[current.id]?.size ?: 0,
                calibrationRatio = calibrationRatio,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = priorityWeights,
                placements = placements,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                latestAllowedEndMillis = taskCeiling,
                referenceNowMillis = nowMillis
            ) ?: if (!current.isDurationExplicit && rawDurationMillis < durationMillis) {
                findBestCircadianWindow(
                    task = current,
                    durationMillis = rawDurationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    dependentCount = dependentsMap[current.id]?.size ?: 0,
                    calibrationRatio = calibrationRatio,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    priorityWeights = priorityWeights,
                    placements = placements,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    physiologicalBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours,
                    latestAllowedEndMillis = taskCeiling,
                    referenceNowMillis = nowMillis
                )
            } else null

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                val finalBreakdown = if (window.durationMinutes < slotBreakdown.bufferedDurationMinutes) {
                    slotBreakdown.copy(bufferedDurationMinutes = window.durationMinutes)
                } else {
                    slotBreakdown
                }
                placements.add(ScheduledPlacement(current, window, finalBreakdown))
                completionEndMap[current.id] = window.endMillis
                if (current.id.endsWith("#part2")) {
                    completionEndMap[current.id.removeSuffix("#part2")] = window.endMillis
                }

                val isCognitiveHeavy = current.energyDemand == EnergyDemand.HIGH ||
                    current.category == TaskCategory.DEEP_WORK ||
                    current.category == TaskCategory.CREATIVE

                // Bug 2 Fix: compute simulated focus load on wall-clock timeline
                val contiguousCognitiveMins = if (isCognitiveHeavy) {
                    computeSimulatedFocusLoadAt(window.startMillis, placements) + window.durationMinutes
                } else {
                    0
                }

                val bufferMins = if (current.id.endsWith("#part1")) {
                    ULTRADIAN_RECOVERY_BUFFER_MINUTES
                } else if (contiguousCognitiveMins >= 90) {
                    15
                } else {
                    TRANSITION_BUFFER_MINUTES
                }

                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + bufferMins * 60_000L)
                )

                val unlockKey = if (current.id.endsWith("#part2")) current.id.removeSuffix("#part2") else current.id
                children[unlockKey]?.forEach { child ->
                    val newDeg = (inDegree[child.id] ?: 1) - 1
                    inDegree[child.id] = newDeg
                    if (newDeg <= 0) readyQueue.add(child)
                }
                if (unlockKey != current.id) {
                    children[current.id]?.forEach { child ->
                        val newDeg = (inDegree[child.id] ?: 1) - 1
                        inDegree[child.id] = newDeg
                        if (newDeg <= 0) readyQueue.add(child)
                    }
                }
            } else {
                // Bug 6 Fix & Feature 4 (ADR-024): Atomic Two-Phase Ultradian Split Verifier
                val splitPair = splitIntoUltradianParts(current, breakdown.bufferedDurationMinutes)
                var splitSuccess = false
                if (splitPair != null) {
                    val (part1, part2) = splitPair
                    val p1DurationMillis = max(5, part1.estimatedMinutes) * 60_000L
                    val p2DurationMillis = max(5, part2.estimatedMinutes) * 60_000L
                    val currentCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE

                    // Phase 1: Test if part1 can fit in current freeIntervals
                    val p1Slot = findBestCircadianWindow(
                        task = part1,
                        durationMillis = p1DurationMillis,
                        earliestStartMillis = earliestStart,
                        freeIntervals = freeIntervals,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        dependentCount = 1,
                        calibrationRatio = 1.0,
                        zoneId = zoneId,
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        priorityWeights = priorityWeights,
                        placements = placements,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours,
                        latestAllowedEndMillis = currentCeiling
                    )

                    if (p1Slot != null) {
                        val (p1Window, p1Breakdown) = p1Slot
                        // Require at least a 15-minute ultradian recovery buffer between part1 and part2
                        val p1BufferMins = 15
                        val simulatedIntervalsAfterP1 = subtractInterval(
                            freeIntervals,
                            TimeSlotWindow(p1Window.startMillis, p1Window.endMillis + p1BufferMins * 60_000L)
                        )
                        val earliestP2Start = p1Window.endMillis + p1BufferMins * 60_000L
                        val simulatedPlacementsWithP1 = placements + ScheduledPlacement(part1, p1Window, p1Breakdown)

                        // Phase 2: Test if part2 can fit in simulated remaining intervals
                        val p2Slot = findBestCircadianWindow(
                            task = part2,
                            durationMillis = p2DurationMillis,
                            earliestStartMillis = earliestP2Start,
                            freeIntervals = simulatedIntervalsAfterP1,
                            wakeTimeMillis = wakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            dependentCount = dependentsMap[current.id]?.size ?: 0,
                            calibrationRatio = 1.0,
                            zoneId = zoneId,
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            priorityWeights = priorityWeights,
                            placements = simulatedPlacementsWithP1,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours,
                            latestAllowedEndMillis = currentCeiling
                        )

                        if (p2Slot != null) {
                            // Both parts verified! Commit both atomically to placements.
                            val (p2Window, p2Breakdown) = p2Slot
                            placements.add(ScheduledPlacement(part1, p1Window, p1Breakdown))
                            placements.add(ScheduledPlacement(part2, p2Window, p2Breakdown))

                            val p2ContiguousMins = computeSimulatedFocusLoadAt(p2Window.startMillis, simulatedPlacementsWithP1) + p2Window.durationMinutes
                            val p2BufferMins = if (p2ContiguousMins >= 90) 15 else TRANSITION_BUFFER_MINUTES
                            freeIntervals = subtractInterval(
                                simulatedIntervalsAfterP1,
                                TimeSlotWindow(p2Window.startMillis, p2Window.endMillis + p2BufferMins * 60_000L)
                            )

                            completionEndMap[part1.id] = p1Window.endMillis
                            completionEndMap[part2.id] = p2Window.endMillis
                            completionEndMap[current.id] = p2Window.endMillis

                            val unlockKey = current.id
                            children[unlockKey]?.forEach { child ->
                                val newDeg = (inDegree[child.id] ?: 1) - 1
                                inDegree[child.id] = newDeg
                                if (newDeg <= 0) readyQueue.add(child)
                            }
                            splitSuccess = true
                        }
                    }
                }

                if (!splitSuccess) {
                    val baseId = current.id.substringBefore("#part")
                    if (deferrals.none { it.taskId == baseId }) {
                        deferrals.add(
                            DeferralRationale(
                                taskId = baseId,
                                reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                                explanation = "Task '${current.title}' (${breakdown.bufferedDurationMinutes}m) could not find sufficient contiguous time or two-phase ultradian split slots today.",
                                actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                            )
                        )
                    }
                }
            }
        }

        // Invariant Safeguard (ADR-024): ensure no orphaned partial split remains in placements
        val orphanedPart1 = placements.filter { it.task.id.endsWith("#part1") }
            .filter { p1 ->
                val p2Id = p1.task.id.replace("#part1", "#part2")
                placements.none { it.task.id == p2Id }
            }
        if (orphanedPart1.isNotEmpty()) {
            val orphanIds = orphanedPart1.map { it.task.id }.toSet()
            placements.removeAll { it.task.id in orphanIds }
            for (orphan in orphanedPart1) {
                val baseId = orphan.task.id.substringBefore("#part")
                if (deferrals.none { it.taskId == baseId }) {
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                            explanation = "Task '${orphan.task.title}' had an orphaned partial placement rolled back due to unplaced part 2.",
                            actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                        )
                    )
                }
            }
        }

        // Clean deferral task IDs so none carry #part suffixes
        val sanitizedDeferrals = deferrals.map { d ->
            if (d.taskId.contains("#part")) d.copy(taskId = d.taskId.substringBefore("#part")) else d
        }.distinctBy { it.taskId }

        return SchedulingOutcome(placements.sortedBy { it.window.startMillis }, sanitizedDeferrals)
    }

    internal fun splitIntoUltradianParts(
        task: SchedulableTask,
        totalDurationMinutes: Int
    ): Pair<SchedulableTask, SchedulableTask>? {
        if (totalDurationMinutes < 60 || task.isLocked || task.id.contains("#part")) return null
        if (task.fragmentationTolerance < 0.15f) return null

        val part1Mins = (totalDurationMinutes / 2).coerceAtLeast(25)
        val part2Mins = (totalDurationMinutes - part1Mins).coerceAtLeast(25)
        if (part1Mins + part2Mins > totalDurationMinutes + 5) return null

        val part1Id = "${task.id}#part1"
        val part2Id = "${task.id}#part2"

        val part1 = task.copy(
            id = part1Id,
            title = "${task.title} (Part 1/2)",
            estimatedMinutes = part1Mins,
            isDurationExplicit = true
        )
        val part2 = task.copy(
            id = part2Id,
            title = "${task.title} (Part 2/2)",
            estimatedMinutes = part2Mins,
            isDurationExplicit = true,
            prerequisiteIds = task.prerequisiteIds + part1Id
        )
        return part1 to part2
    }

    internal fun computeContextTransitionDelta(
        placements: List<ScheduledPlacement>,
        candidateSlotStartMillis: Long,
        candidate: SchedulableTask,
        candidateSlotEndMillis: Long = candidateSlotStartMillis
    ): Double {
        if (placements.isEmpty()) return 0.0

        val preceding = placements
            .filter { it.window.endMillis <= candidateSlotStartMillis }
            .maxByOrNull { it.window.endMillis }

        val succeeding = placements
            .filter { it.window.startMillis >= candidateSlotEndMillis }
            .minByOrNull { it.window.startMillis }

        fun scoreNeighbor(neighborTask: SchedulableTask, gapMinutes: Long): Double {
            if (gapMinutes < 0 || gapMinutes > 45) return 0.0
            val sameContextTag = neighborTask.contextTag.isNotBlank() &&
                neighborTask.contextTag.equals(candidate.contextTag, ignoreCase = true)

            if (neighborTask.category == candidate.category && sameContextTag) {
                return 8.5
            }
            if (neighborTask.category == candidate.category || sameContextTag) {
                return 6.0
            }

            val isHighEntropy = (neighborTask.category == TaskCategory.EXERCISE && candidate.category == TaskCategory.DEEP_WORK && gapMinutes < 15) ||
                (neighborTask.category == TaskCategory.DEEP_WORK && candidate.category == TaskCategory.ADMIN && gapMinutes < 10)

            return if (isHighEntropy) -8.5 else -1.5
        }

        val precDelta = preceding?.let {
            val gap = (candidateSlotStartMillis - it.window.endMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }
        val succDelta = succeeding?.let {
            val gap = (it.window.startMillis - candidateSlotEndMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }

        return when {
            precDelta != null && succDelta != null -> ((precDelta + succDelta) * 0.75).coerceIn(-12.0, 12.0)
            precDelta != null -> precDelta
            succDelta != null -> succDelta
            else -> 0.0
        }
    }

    private fun computeBufferedDuration(task: SchedulableTask, calibrationRatio: Double): Int {
        val raw = max(5, task.estimatedMinutes)
        return if (task.isDurationExplicit) raw else (raw * calibrationRatio).roundToInt().coerceAtLeast(5)
    }

    fun computeSimulatedFocusLoadAt(
        timestampMillis: Long,
        placements: List<ScheduledPlacement>
    ): Int {
        if (placements.isEmpty()) return 0
        var currentCheck = timestampMillis
        var accumulatedMins = 0
        val sortedPlacements = placements
            .filter { it.window.endMillis <= timestampMillis }
            .sortedByDescending { it.window.endMillis }

        for (p in sortedPlacements) {
            val gapMinutes = (currentCheck - p.window.endMillis) / 60_000L
            if (gapMinutes <= 15L) {
                val isCognitive = p.task.energyDemand == EnergyDemand.HIGH ||
                    p.task.category == TaskCategory.DEEP_WORK ||
                    p.task.category == TaskCategory.CREATIVE
                if (isCognitive) {
                    accumulatedMins += p.window.durationMinutes
                    currentCheck = p.window.startMillis
                } else {
                    break
                }
            } else {
                break
            }
        }
        return accumulatedMins
    }

    private fun findBestCircadianWindow(
        task: SchedulableTask,
        durationMillis: Long,
        earliestStartMillis: Long,
        freeIntervals: List<TimeSlotWindow>,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        dependentCount: Int,
        calibrationRatio: Double,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        placements: List<ScheduledPlacement> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        latestAllowedEndMillis: Long = Long.MAX_VALUE,
        referenceNowMillis: Long? = null
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            var candidateStart = max(interval.startMillis, earliestStartMillis)
            val effectiveIntervalEnd = min(interval.endMillis, latestAllowedEndMillis)
            while (candidateStart + durationMillis <= effectiveIntervalEnd) {
                val candidateEnd = candidateStart + durationMillis
                val midpoint = candidateStart + durationMillis / 2
                val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)
                val snap = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = midpoint,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = simulatedFocus,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task = task,
                    snapshot = snap,
                    dependentCount = dependentCount,
                    calibrationRatio = calibrationRatio,
                    weights = priorityWeights,
                    referenceNowMillis = referenceNowMillis ?: earliestStartMillis
                )
                val hourlyDecayLambda = when {
                    task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE -> 0.08
                    task.energyDemand == EnergyDemand.MODERATE -> 0.22
                    else -> 0.45
                }

                val peakCannibalizationPenalty = if (
                    snap.phase == EnergyPhase.PEAK &&
                    breakdown.urgencyScore < 75.0 &&
                    (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
                ) {
                    16.0
                } else {
                    0.0
                }

                // Feature 1 (ADR-023): Pharmacokinetic-Aware Scheduling & Crash Shield
                val stimEffect = if (stimulantDoses.isNotEmpty()) {
                    PharmacokineticStimulantCalculator.evaluateAt(candidateStart, stimulantDoses)
                } else null

                val stimAlignmentDelta = if (stimEffect != null) {
                    val alertBoost = stimEffect.acuteAlertnessBoostPoints
                    if (task.energyDemand == EnergyDemand.HIGH || task.isFrog || task.category == TaskCategory.DEEP_WORK) {
                        when {
                            alertBoost > 4.0 -> 8.0 // Aligns deep focus with Bateman peak plasma concentration
                            alertBoost < -1.5 -> -18.0 // Crash Shield: actively penalizes deep work during adenosine rebound crash
                            else -> 0.0
                        }
                    } else if (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN || task.category == TaskCategory.MINDFULNESS) {
                        if (alertBoost < -1.5) 6.0 else 0.0 // Crash Recovery: routes light administrative work into crash window
                    } else 0.0
                } else 0.0

                // Feature 2 (ADR-023): Bidirectional Context-Cluster Scoring
                val contextDelta = computeContextTransitionDelta(placements, candidateStart, task, candidateEnd)
                val hoursFromNow = (candidateStart - earliestStartMillis) / 3_600_000.0

                // Bug 1 & Feature 1 (ADR-025): Hard Deadline Breach Penalty (-35.0 pts)
                val deadlineBreachPenalty = if (task.deadlineMillis != null && candidateEnd > task.deadlineMillis) {
                    35.0
                } else {
                    0.0
                }

                val utility = breakdown.totalScore - (hourlyDecayLambda * hoursFromNow) - peakCannibalizationPenalty + contextDelta + stimAlignmentDelta - deadlineBreachPenalty

                if (utility > bestUtility) {
                    bestUtility = utility
                    bestWindow = TimeSlotWindow(candidateStart, candidateStart + durationMillis)
                    bestBreakdown = breakdown
                }
                candidateStart += stepMillis
            }
        }
        return if (bestWindow != null && bestBreakdown != null) bestWindow to bestBreakdown else null
    }

    private fun fitsInFreeIntervals(freeIntervals: List<TimeSlotWindow>, target: TimeSlotWindow): Boolean {
        return freeIntervals.any { it.startMillis <= target.startMillis && it.endMillis >= target.endMillis }
    }

    private fun subtractInterval(
        intervals: List<TimeSlotWindow>,
        blocked: TimeSlotWindow
    ): MutableList<TimeSlotWindow> {
        val result = mutableListOf<TimeSlotWindow>()
        for (current in intervals) {
            if (blocked.endMillis <= current.startMillis || blocked.startMillis >= current.endMillis) {
                result.add(current)
            } else {
                if (blocked.startMillis > current.startMillis) {
                    result.add(TimeSlotWindow(current.startMillis, blocked.startMillis))
                }
                if (blocked.endMillis < current.endMillis) {
                    result.add(TimeSlotWindow(blocked.endMillis, current.endMillis))
                }
            }
        }
        return result
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerautoschedulingcontractskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`
- **Lines of Code:** 62
- **Size:** 1,899 bytes (1.9 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * Non-negotiable safety contracts for all auto-scheduling flows.
 */
object AutoSchedulingContracts {

    /**
     * A manual schedule claims a full timestamp. A date with no time-of-day is not a finished
     * schedule — the scheduler is still expected to place the task, on that day.
     */
    fun hasManualScheduleData(task: TaskEntity): Boolean {
        return task.scheduledDate != null && task.scheduledTime != null && !task.isAutoScheduled
    }

    fun isMutableByAutoScheduler(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.isScheduleLocked || task.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE) {
            return false
        }
        return !hasManualScheduleData(task) || task.isAutoScheduled
    }

    /**
     * True when a task has a deadline anchor but no explicit scheduled slot yet.
     */
    fun hasDeadlineData(task: TaskEntity): Boolean {
        return task.deadlineDate != null
    }
}

enum class AutoScheduleRejectionReason {
    LOCKED_TASK,
    MANUAL_SCHEDULE_PRESENT,
    DEPENDENCY_BLOCKED,
    NO_CAPACITY_IN_HORIZON,
    LOW_ENERGY_PROTECTION,
    OUTSIDE_WORKDAY,
    PROTECTED_SLOT,
    UNKNOWN
}

data class AutoScheduleDecisionTelemetry(
    val taskId: String,
    val generatedAtMillis: Long,
    val horizonDays: Int,
    val wasApplied: Boolean,
    val selectedSlotDate: Long? = null,
    val selectedSlotTime: Long? = null,
    val rejectionReason: AutoScheduleRejectionReason? = null,
    val inputs: AutoScheduleInputsSnapshot
)

data class AutoScheduleInputsSnapshot(
    val priorityScore: Float,
    val energyScore: Float,
    val sleepPressurePoints: Int,
    val hasDependencies: Boolean,
    val estimatedDurationMinutes: Int,
    val tagProfileHints: List<String>
)
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerautoschedulingrationaleskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`
- **Lines of Code:** 297
- **Size:** 12,896 bytes (12.6 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class DeferralReasonCode {
    INSUFFICIENT_TIME,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    DEADLINE_CONFLICT,
    PROTECTED_SLOT_OVERLAP,
    TASK_COLLISION,
    DEPENDENCY_BLOCKED
}

data class ScheduleConflict(
    val taskId: String,
    val taskTitle: String,
    val conflictDescription: String,
    val actionableSuggestion: String,
    val conflictingTargetName: String,
    val conflictStartMinute: Int,
    val conflictEndMinute: Int,
    val suggestedNewStartMinute: Int? = null
)

data class PlacementRationale(
    val taskId: String,
    val scheduledSlotStart: Long,
    val scheduledSlotEnd: Long,
    val energyFitScore: Float,
    val primaryReason: String
)

data class DeferralRationale(
    val taskId: String,
    val reasonCode: DeferralReasonCode,
    val explanation: String,
    val actionableSuggestion: String
)

data class SchedulingReport(
    val placedCount: Int,
    val deferredCount: Int,
    val placementRationales: List<PlacementRationale>,
    val deferrals: List<DeferralRationale>,
    val dailyCognitiveLoadUtilized: Float
)

object AutoSchedulingEngineCoordinator {
    fun planSchedule(
        tasks: List<TaskEntity>,
        availableSlotsMinutes: Int,
        dailyCognitiveCapacityMinutes: Int
    ): SchedulingReport {
        var usedMinutes = 0
        val placedRationales = mutableListOf<PlacementRationale>()
        val deferrals = mutableListOf<DeferralRationale>()

        for (task in tasks) {
            val duration = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25
            if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                        explanation = "Daily cognitive limit of ${dailyCognitiveCapacityMinutes}m reached.",
                        actionableSuggestion = "Schedule for tomorrow's morning peak window."
                    )
                )
            } else if (duration > availableSlotsMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Task duration (${duration}m) exceeds available slot (${availableSlotsMinutes}m).",
                        actionableSuggestion = "Break this task into smaller 25m focus blocks."
                    )
                )
            } else {
                val slotStart = System.currentTimeMillis() + usedMinutes.toLong() * 60_000L
                val slotEnd = slotStart + duration.toLong() * 60_000L
                placedRationales.add(
                    PlacementRationale(
                        taskId = task.id,
                        scheduledSlotStart = slotStart,
                        scheduledSlotEnd = slotEnd,
                        energyFitScore = 0.88f,
                        primaryReason = "High circadian alignment and optimal cognitive budget match."
                    )
                )
                usedMinutes += duration
            }
        }

        check(placedRationales.size + deferrals.size == tasks.size) {
            "Invariant violated: Total tasks must equal placed plus deferred (no silent drops allowed)."
        }

        return SchedulingReport(
            placedCount = placedRationales.size,
            deferredCount = deferrals.size,
            placementRationales = placedRationales,
            deferrals = deferrals,
            dailyCognitiveLoadUtilized = if (dailyCognitiveCapacityMinutes > 0)
                usedMinutes.toFloat() / dailyCognitiveCapacityMinutes.toFloat() else 0f
        )
    }
}

object ConflictResolver {
    fun findConflictFreeStart(
        searchStartMinute: Int,
        duration: Int,
        workDayStartHour: Int,
        workDayEndHour: Int,
        occupiedIntervals: List<Pair<Int, Int>>
    ): Int? {
        val sortedIntervals = occupiedIntervals.sortedBy { it.first }
        // Bug 5 Fix (ADR-030): Clamp suggested conflict-free start to workDayStart
        var candidate = maxOf(searchStartMinute, workDayStartHour.coerceIn(0, 23) * 60)
        val maxMinute = workDayEndHour * 60
        while (candidate + duration <= maxMinute) {
            val overlapping = sortedIntervals.firstOrNull { (start, end) ->
                maxOf(candidate, start) < minOf(candidate + duration, end)
            }
            if (overlapping == null) {
                return candidate
            } else {
                candidate = maxOf(candidate + 1, overlapping.second)
            }
        }
        return null
    }

    fun analyzeScheduleConflicts(
        tasks: List<TaskEntity>,
        protectedSlots: List<ProtectedTimeSlot> = emptyList(),
        workDayStart: Int = 8,
        workDayEnd: Int = 20,
        dayMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduleConflict> {
        val scheduledTasks = tasks.filter { it.scheduledDate != null && it.scheduledTime != null }
        if (scheduledTasks.isEmpty()) return emptyList()

        fun analyzeForSingleDay(
            dayTasks: List<TaskEntity>,
            targetDayMillis: Long
        ): List<ScheduleConflict> {
            val conflicts = mutableListOf<ScheduleConflict>()
            val sortedTasks = dayTasks.sortedBy { taskStartMinuteOfDay(it) }

            val activeSlots = protectedSlots.filter { slot ->
                slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, targetDayMillis)
            }

            val allSlotIntervals = activeSlots.map { slot ->
                slot.startMinuteOfDay to (slot.startMinuteOfDay + slot.durationMinutes + slot.recoveryBufferMinutes)
            }

            for (i in sortedTasks.indices) {
                val taskA = sortedTasks[i]
                val aStart = taskStartMinuteOfDay(taskA)
                val aDuration = TaskBlockPlan.plannedMinutes(taskA)
                val aEnd = aStart + aDuration

                val otherTasksForA = sortedTasks.filterIndexed { idx, _ -> idx != i }.map { t ->
                    val start = taskStartMinuteOfDay(t)
                    start to (start + TaskBlockPlan.plannedMinutes(t))
                }
                val occupiedForA = allSlotIntervals + otherTasksForA

                // Protected slot overlaps
                for (slot in activeSlots) {
                    val sStart = slot.startMinuteOfDay
                    val sEnd = sStart + slot.durationMinutes + slot.recoveryBufferMinutes
                    if (maxOf(aStart, sStart) < minOf(aEnd, sEnd)) {
                        val overlapStart = maxOf(aStart, sStart)
                        val overlapEnd = minOf(aEnd, sEnd)
                        val bufferNote = if (slot.recoveryBufferMinutes > 0) " (incl. ${slot.recoveryBufferMinutes}m buffer)" else ""
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = aDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForA
                        )
                        val suggestion = if (targetStart != null) {
                            "Move '${taskA.title}' to ${formatMinutesToTime(targetStart)} after protected slot ends."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(workDayEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskA.id,
                                taskTitle = taskA.title,
                                conflictDescription = "Overlaps with protected slot '${slot.label}' (${formatMinutesToTime(sStart)}–${formatMinutesToTime(sEnd)}$bufferNote)",
                                actionableSuggestion = suggestion,
                                conflictingTargetName = slot.label,
                                conflictStartMinute = overlapStart,
                                conflictEndMinute = overlapEnd,
                                suggestedNewStartMinute = targetStart
                            )
                        )
                    }
                }

                // Task vs Task collisions
                for (j in (i + 1) until sortedTasks.size) {
                    val taskB = sortedTasks[j]
                    val bStart = taskStartMinuteOfDay(taskB)
                    val bDuration = TaskBlockPlan.plannedMinutes(taskB)
                    val bEnd = bStart + bDuration

                    if (maxOf(aStart, bStart) < minOf(aEnd, bEnd)) {
                        val overlapStart = maxOf(aStart, bStart)
                        val overlapEnd = minOf(aEnd, bEnd)
                        val otherTasksForB = sortedTasks.filterIndexed { idx, _ -> idx != j }.map { t ->
                            val start = taskStartMinuteOfDay(t)
                            start to (start + TaskBlockPlan.plannedMinutes(t))
                        }
                        val occupiedForB = allSlotIntervals + otherTasksForB
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = bDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForB
                        )
                        val suggestion = if (targetStart != null) {
                            "Reorder '${taskB.title}' to start at ${formatMinutesToTime(targetStart)} or re-run auto-schedule."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(workDayEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskB.id,
                                taskTitle = taskB.title,
                                conflictDescription = "Overlaps with '${taskA.title}' (${formatMinutesToTime(aStart)}–${formatMinutesToTime(aEnd)})",
                                actionableSuggestion = suggestion,
                                conflictingTargetName = taskA.title,
                                conflictStartMinute = overlapStart,
                                conflictEndMinute = overlapEnd,
                                suggestedNewStartMinute = targetStart
                            )
                        )
                    }
                }
            }
            return conflicts
        }

        return if (dayMillis != null) {
            val targetDate = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate()
            val dayTasks = scheduledTasks.filter {
                Instant.ofEpochMilli(it.scheduledDate!!).atZone(zoneId).toLocalDate() == targetDate
            }
            analyzeForSingleDay(dayTasks, dayMillis)
        } else {
            val groups = scheduledTasks.groupBy {
                Instant.ofEpochMilli(it.scheduledDate!!).atZone(zoneId).toLocalDate()
            }
            groups.flatMap { (date, dateTasks) ->
                val dateStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                analyzeForSingleDay(dateTasks, dateStartMillis)
            }
        }
    }

    fun taskStartMinuteOfDay(task: TaskEntity): Int {
        val rawTime = task.scheduledTime
        if (rawTime != null && rawTime in 0 until ONE_DAY_MILLIS) {
            return (rawTime / 60_000L).toInt().coerceIn(0, 1439)
        }
        return task.timelineStartMinuteOfDay()
    }

    fun formatMinutesToTime(minuteOfDay: Int): String {
        val safeMin = ((minuteOfDay % 1440) + 1440) % 1440
        val h = safeMin / 60
        val m = safeMin % 60
        val ampm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.ROOT, "%d:%02d %s", displayH, m, ampm)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerplacementstabilitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`
- **Lines of Code:** 121
- **Size:** 5,139 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority

/**
 * Whether an already-placed block is worth moving, and whether a new placement is worth writing.
 *
 * The scheduler wakes up every few minutes, so a decision that only asks "where is best right now"
 * keeps rewriting the same afternoon. This object makes the question "is the difference big enough
 * to disturb the user for" — the answers are ordered so stability checks run before optimisation.
 */
object PlacementStability {

    /** A block this close to starting is the user's current plan, not the scheduler's draft. */
    private const val NEAR_TERM_FREEZE_MILLIS = 2 * 60 * 60 * 1000L

    /** Smallest shift that justifies rewriting a placement the user can already see. */
    private const val MIN_MOVE_MILLIS = 30 * 60 * 1000L

    enum class ReplanVerdict(val allowsReplan: Boolean) {
        NEAR_TERM_FREEZE(false),
        COOLDOWN(false),
        KEEP(false),
        FORCED(true),
        DEADLINE_RESCUE(true),
        POSTPONE_PATTERN(true),
        FAR_FUTURE_PULL_IN(true),
        PERIODIC_REFRESH(true),
        PAST_EXPIRED_RESCUE(true)
    }

    /**
     * Ordered so that the two checks the user can feel — an imminent block and a recently written
     * one — run before any optimisation rule, including the manual trigger.
     */
    fun verdict(
        task: TaskEntity,
        nowMillis: Long,
        prefs: UserPreferences,
        forceReplan: Boolean
    ): ReplanVerdict {
        if (task.isScheduleLocked) return ReplanVerdict.KEEP

        val scheduledMillis = task.resolveScheduledEpochMillis()
            ?: return ReplanVerdict.KEEP
        val hoursUntilScheduled = (scheduledMillis - nowMillis) / 3_600_000f

        // Bug 2 & Feature 3 (ADR-026): Expired-Block Auto-Rescue
        // If a task's reserved block has ended and the 15-minute grace period has passed,
        // it is eligible for same-day rescue rescheduling rather than stranded permanently in the past.
        if (TaskBlockPlan.blockHasExpired(task, scheduledMillis, nowMillis, gracePeriodMillis = 15 * 60_000L)) {
            return ReplanVerdict.PAST_EXPIRED_RESCUE
        }

        if (scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE

        if (forceReplan) return ReplanVerdict.FORCED

        val deadlineMillis = task.resolveDeadlineEpochMillis()

        val taskAge = nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt)
        val isUrgentDeadline = deadlineMillis?.let { (it - nowMillis) < 6 * 60 * 60 * 1000L } ?: false
        val cooldownMillis = when {
            isUrgentDeadline -> 60 * 60 * 1000L
            prefs.isAutoScheduleToggleOn -> 4 * 60 * 60 * 1000L
            else -> 6 * 60 * 60 * 1000L
        }
        if (taskAge <= cooldownMillis) return ReplanVerdict.COOLDOWN

        if (deadlineMillis != null) {
            val hoursUntilDeadline = (deadlineMillis - nowMillis) / 3_600_000f
            if (hoursUntilDeadline < 6f && hoursUntilScheduled > hoursUntilDeadline * 0.5f) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
            if (hoursUntilDeadline < 12f && task.priority == Priority.LOW) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
        }

        if (task.postponeCount >= 3) return ReplanVerdict.POSTPONE_PATTERN

        if (hoursUntilScheduled > 24f && !task.isScheduleLocked) {
            val isImportant = task.priority == Priority.HIGH || task.impactScore >= 80 || task.isFrog
            if (isImportant) return ReplanVerdict.FAR_FUTURE_PULL_IN
        }

        if (prefs.autoSchedulingEnabled) {
            val shouldRefresh = if (prefs.isAutoScheduleToggleOn) {
                true
            } else {
                val cycleNumber = (nowMillis / (6 * 60 * 60 * 1000L)) % 5
                val taskBucket = (task.id.hashCode().toLong() and 0x7FFFFFFF) % 5
                cycleNumber == taskBucket
            }
            if (shouldRefresh) return ReplanVerdict.PERIODIC_REFRESH
        }

        return ReplanVerdict.KEEP
    }

    /**
     * A write is only worth it when the user sees a different plan afterwards. An identical start
     * touches the database and the update timestamp for nothing, and a future block that shifts by
     * minutes is churn dressed as an improvement — unless the old slot is already behind us, in
     * which case staying put would leave the task stranded in the past.
     */
    fun isMoveWorthApplying(
        previousStartMillis: Long?,
        nextStartMillis: Long,
        nowMillis: Long
    ): Boolean {
        val previous = previousStartMillis ?: return true
        if (nextStartMillis == previous) return false
        if (previous <= nowMillis) return true
        return kotlin.math.abs(nextStartMillis - previous) >= MIN_MOVE_MILLIS
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerprotectedslotclockkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`
- **Lines of Code:** 190
- **Size:** 7,448 bytes (7.3 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import java.util.Calendar

enum class SlotPhase {
    /** Inside the lead-in window but not started yet */
    LEAD_IN,

    /** The wall clock sits inside the slot */
    RUNNING,

    /** Just past the end, still worth showing as finishing */
    WRAPPING
}

data class DueSlot(
    val slot: ProtectedTimeSlot,
    val phase: SlotPhase,
    val startMillis: Long,
    val endMillis: Long,
    /** 0 unless the slot has not started yet */
    val minutesToStart: Int,
    /** Minutes until the slot ends; 0 once it is over */
    val minutesLeft: Int
)

/**
 * Answers "is one of the user's protected blocks happening right now, or about to?". One place
 * computes it so the Today hero and the Focus screen cannot disagree.
 *
 * A slot's own recovery buffer is deliberately not part of the takeover: the buffer exists to stop
 * the auto-scheduler from packing work immediately after exercise, not to keep a card on screen.
 */
object ProtectedSlotClock {

    const val DEFAULT_LEAD_IN_MINUTES = 120

    const val DEFAULT_WRAP_MINUTES = 10

    private const val MINUTE_MILLIS = 60_000L

    /** Which days a slot falls on. Ignores [ProtectedTimeSlot.isEnabled] — that is the caller's filter. */
    fun occursOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        return when (slot.recurrence) {
            PtsRecurrence.DAILY -> true
            PtsRecurrence.WEEKDAYS -> cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
            PtsRecurrence.WEEKENDS -> {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            }
            PtsRecurrence.CUSTOM -> {
                // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
                val bit = 1 shl (cal.get(Calendar.DAY_OF_WEEK) - 1)
                (slot.weekdayBitmask and bit) != 0
            }
            PtsRecurrence.SPECIFIC_DATE -> slot.dateMillis?.let {
                sameCalendarDay(it, dayMillis)
            } == true
        }
    }

    /**
     * Slots overlapping `now - wrapMinutes .. now + leadInMinutes`, best candidate first: one that
     * is running or just ended takes the top spot, most recent start winning, then the ones still
     * to come in start order.
     *
     * The lead-in edge is inclusive, matching how the Today screen already buckets tasks at
     * `now + 2h`; the wrap edge is exclusive so a slot is off screen at exactly ten minutes past.
     */
    fun dueSlots(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long,
        leadInMinutes: Int = DEFAULT_LEAD_IN_MINUTES,
        wrapMinutes: Int = DEFAULT_WRAP_MINUTES
    ): List<DueSlot> {
        val windowStart = nowMillis - wrapMinutes * MINUTE_MILLIS
        val windowEnd = nowMillis + leadInMinutes * MINUTE_MILLIS

        return slots
            .filter { it.isEnabled }
            .flatMap { slot -> occurrences(slot, nowMillis).map { slot to it } }
            .filter { (_, span) -> span.endMillis > windowStart && span.startMillis <= windowEnd }
            .mapNotNull { (slot, span) -> dueSlotOf(slot, span, nowMillis, wrapMinutes) }
            .sortedWith(SlotOrder)
    }

    /** The slot that should hold the hero position, or null while every slot is still ahead. */
    fun takeoverOf(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long
    ): DueSlot? = dueSlots(slots, nowMillis).firstOrNull { it.phase != SlotPhase.LEAD_IN }

    /** The slots starting within the lead-in window, soonest first — the "Up Next" queue. */
    fun approaching(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long,
        leadInMinutes: Int = DEFAULT_LEAD_IN_MINUTES
    ): List<DueSlot> = dueSlots(slots, nowMillis, leadInMinutes = leadInMinutes).filter { it.phase == SlotPhase.LEAD_IN }

    private data class Span(val startMillis: Long, val endMillis: Long)

    /**
     * Yesterday is checked as well, because a block that began before midnight must still read as
     * running at 00:05. Tomorrow covers a lead-in that starts just after midnight.
     */
    private fun occurrences(slot: ProtectedTimeSlot, nowMillis: Long): List<Span> {
        val todaysStart = startOfDay(nowMillis)
        return (-1..1).mapNotNull { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = todaysStart
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayStart = cal.timeInMillis
            if (!occursOnDay(slot, dayStart)) return@mapNotNull null
            cal.set(Calendar.HOUR_OF_DAY, slot.startMinuteOfDay / 60)
            cal.set(Calendar.MINUTE, slot.startMinuteOfDay % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            Span(start, start + slot.durationMinutes * MINUTE_MILLIS)
        }
    }

    private fun dueSlotOf(
        slot: ProtectedTimeSlot,
        span: Span,
        nowMillis: Long,
        wrapMinutes: Int
    ): DueSlot? = when {
        nowMillis < span.startMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.LEAD_IN,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = minutesUp(span.startMillis - nowMillis),
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.RUNNING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis + wrapMinutes * MINUTE_MILLIS -> DueSlot(
            slot = slot,
            phase = SlotPhase.WRAPPING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = 0
        )

        else -> null
    }

    private fun minutesUp(millis: Long): Int =
        ((millis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).coerceAtLeast(0L).toInt()

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sameCalendarDay(a: Long, b: Long): Boolean {
        val left = Calendar.getInstance().apply { timeInMillis = a }
        val right = Calendar.getInstance().apply { timeInMillis = b }
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    private object SlotOrder : Comparator<DueSlot> {
        override fun compare(a: DueSlot, b: DueSlot): Int {
            val groupA = if (a.phase == SlotPhase.LEAD_IN) 1 else 0
            val groupB = if (b.phase == SlotPhase.LEAD_IN) 1 else 0
            if (groupA != groupB) return groupA - groupB
            return if (groupA == 0) b.startMillis.compareTo(a.startMillis)
            else a.startMillis.compareTo(b.startMillis)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulertaskblockplankt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`
- **Lines of Code:** 50
- **Size:** 2,121 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * How much clock time a scheduled task owns.
 *
 * The planner blocks a slot with the raw estimate inflated by the user's learned planning bias and
 * the slot's energy zone, then records that length on the task. Anything downstream that asks
 * "did this task get its time?" or "is this hour already taken?" reads the recorded block, so the
 * answer matches what was actually reserved instead of a second, cheaper guess at it.
 */
object TaskBlockPlan {

    private const val MAX_MINUTES = 360

    fun plannedMinutes(task: TaskEntity): Int {
        val recorded = task.plannedBlockMinutes
        if (recorded > 0) return recorded.coerceAtMost(MAX_MINUTES)
        return expectedMinutes(task)
    }

    /** The planner's own starting figure, before bias and energy inflate it. */
    fun expectedMinutes(task: TaskEntity): Int = when {
        task.plannedBlockMinutes > 0 && (task.id.contains("#part") || task.id.contains("_part") || task.parentTaskId != null || task.title.contains("#part") || task.title.contains("_part") || task.title.contains("(Part ")) -> task.plannedBlockMinutes
        task.estimatedDurationMinutes > 0 -> task.estimatedDurationMinutes
        else -> defaultMinutes(task)
    }

    /** What a task of this effort is worth when the user never gave it a length. */
    fun defaultMinutes(task: TaskEntity): Int = when {
        task.effortScore >= 80 -> 60
        task.effortScore >= 60 -> 45
        else -> 30
    }

    fun scheduledEndMillis(startMillis: Long, task: TaskEntity): Long =
        startMillis + plannedMinutes(task) * 60_000L

    /**
     * True once the task's reserved block is over and the grace period has run out. Comparing
     * against the raw estimate instead would call a task missed while it still has time on the clock.
     */
    fun blockHasExpired(
        task: TaskEntity,
        startMillis: Long,
        nowMillis: Long,
        gracePeriodMillis: Long
    ): Boolean = nowMillis > scheduledEndMillis(startMillis, task) + gracePeriodMillis
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulertasktagschedulingprofilekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`
- **Lines of Code:** 222
- **Size:** 10,746 bytes (10.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import java.util.Locale

enum class TagEnergyDemand {
    LOW,
    MEDIUM,
    HIGH
}

enum class TagPreferredWindow {
    MORNING,
    MIDDAY,
    EVENING,
    FLEXIBLE
}

enum class TaskCategory {
    MINDFULNESS,
    EXERCISE,
    PHYSICAL,
    ANALYTICAL,
    HARD_WORK,
    CREATIVE,
    ROUTINE,
    FLEXIBLE
}

data class TagSchedulingProfile(
    val tag: String,
    val energyDemand: TagEnergyDemand,
    val preferredContext: String?,
    val fragmentationTolerance: Float,
    val preferredWindow: TagPreferredWindow,
    val category: TaskCategory = TaskCategory.FLEXIBLE
)

object TaskTagSchedulingProfile {
    val starterTags: List<String> = listOf(
        "study",
        "chores",
        "physical",
        "admin",
        "creative",
        "deep work",
        "meetings",
        "errands",
        "health",
        "finance",
        "social",
        "maintenance",
        "planning",
        "learning",
        "review",
        "writing",
        "coding",
        "reading",
        "household",
        "mindfulness",
        "meditation",
        "breathwork",
        "yoga",
        "journaling",
        "reflection",
        "exercise",
        "workout",
        "gym",
        "running",
        "stretching",
        "walking",
        "hard work"
    )

    private val profilesByTag: Map<String, TagSchedulingProfile> = listOf(
        TagSchedulingProfile("study", TagEnergyDemand.MEDIUM, "@computer", 0.35f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("chores", TagEnergyDemand.LOW, "@home", 0.9f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("physical", TagEnergyDemand.HIGH, null, 0.45f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("admin", TagEnergyDemand.LOW, "@computer", 0.95f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("creative", TagEnergyDemand.MEDIUM, null, 0.4f, TagPreferredWindow.EVENING, TaskCategory.CREATIVE),
        TagSchedulingProfile("deep work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("meetings", TagEnergyDemand.MEDIUM, "@work", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("errands", TagEnergyDemand.LOW, "@errands", 1.0f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("health", TagEnergyDemand.MEDIUM, null, 0.6f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("finance", TagEnergyDemand.HIGH, "@computer", 0.3f, TagPreferredWindow.MORNING, TaskCategory.ANALYTICAL),
        TagSchedulingProfile("social", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("maintenance", TagEnergyDemand.LOW, "@home", 0.85f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("planning", TagEnergyDemand.MEDIUM, "@computer", 0.5f, TagPreferredWindow.MORNING, TaskCategory.ROUTINE),
        TagSchedulingProfile("learning", TagEnergyDemand.MEDIUM, "@computer", 0.45f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("review", TagEnergyDemand.LOW, "@computer", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("writing", TagEnergyDemand.MEDIUM, "@computer", 0.4f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("coding", TagEnergyDemand.HIGH, "@computer", 0.25f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("reading", TagEnergyDemand.LOW, null, 0.75f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("household", TagEnergyDemand.LOW, "@home", 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),

        // Smart category tags
        TagSchedulingProfile("mindfulness", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("meditation", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("breathwork", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("yoga", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("journaling", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("reflection", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("exercise", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("workout", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("gym", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("running", TagEnergyDemand.MEDIUM, null, 0.7f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("stretching", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("walking", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.EXERCISE),
        TagSchedulingProfile("hard work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK)
    ).associateBy { normalize(it.tag) }

    fun profileFor(tag: String): TagSchedulingProfile? = profilesByTag[normalize(tag)]

    fun profilesFor(tagsCsv: String): List<TagSchedulingProfile> {
        return tagsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { profileFor(it) }
    }

    private fun normalize(tag: String): String = tag.trim().trimStart('@').lowercase(Locale.getDefault())

    fun tagMatchesKeywords(tag: String, keywords: List<String>): Boolean {
        val words = tag.split(Regex("[^a-zA-Z0-9_-]+")).filter { it.isNotBlank() }
        return keywords.any { kw ->
            if (kw.contains(' ') || kw.contains('-')) {
                tag.equals(kw, ignoreCase = true) || tag.contains(kw)
            } else {
                words.any { it.equals(kw, ignoreCase = true) }
            }
        }
    }
}

fun TaskEntity.determineCategory(): TaskCategory {
    // 0. Respect explicitly persisted category (user override or previously inferred+saved)
    if (!schedulingCategory.isNullOrBlank()) {
        try {
            return TaskCategory.valueOf(schedulingCategory)
        } catch (_: IllegalArgumentException) {
            // Unknown value in DB — fall through to inference
        }
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    // 1. Try to find a matching tag and use its profile category if available
    for (tag in tagsList) {
        val profile = TaskTagSchedulingProfile.profileFor(tag)
        if (profile != null && profile.category != TaskCategory.FLEXIBLE) {
            return profile.category
        }
    }

    // 2. Explicit keywords fallback (catches custom tags not in profilesByTag)
    return when {
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("mindfulness", "meditation", "breathwork", "yoga", "reflection", "journaling", "journal", "mental health", "calm"))
        } -> {
            TaskCategory.MINDFULNESS
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("exercise", "workout", "gym", "running", "run", "cardio", "fitness", "training", "sports", "stretching", "walking", "swim"))
        } -> {
            TaskCategory.EXERCISE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("hard work", "deep work", "coding", "programming", "dev", "software", "writing", "study", "learning", "finance", "focus", "math", "research", "analysis"))
        } -> {
            TaskCategory.HARD_WORK
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("creative", "design", "art", "ui", "ux", "drafting", "music", "painting", "drawing", "brainstorm"))
        } -> {
            TaskCategory.CREATIVE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("physical", "chores", "errands", "household", "cleaning", "maintenance", "laundry", "repairs"))
        } -> {
            TaskCategory.PHYSICAL
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("admin", "meetings", "email", "bills", "invoices", "paperwork", "review", "planning", "organize"))
        } -> {
            TaskCategory.ROUTINE
        }
        // 3. Fall back to TaskType mapping
        // Bug 4 Fix (ADR-026): Map MINDFULNESS to MINDFULNESS and GENERAL to FLEXIBLE
        else -> when (taskType) {
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> TaskCategory.ANALYTICAL
            com.neuroflow.app.domain.model.TaskType.CREATIVE -> TaskCategory.CREATIVE
            com.neuroflow.app.domain.model.TaskType.ADMIN -> TaskCategory.ROUTINE
            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> TaskCategory.PHYSICAL
            com.neuroflow.app.domain.model.TaskType.GENERAL -> TaskCategory.FLEXIBLE
            com.neuroflow.app.domain.model.TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
        }
    }
}

fun TaskEntity.isNonWorkOrErrandTask(): Boolean {
    val category = determineCategory()
    if (category in listOf(TaskCategory.MINDFULNESS, TaskCategory.EXERCISE, TaskCategory.PHYSICAL)) {
        return true
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    val nonWorkKeywords = listOf(
        "errand", "errands", "chore", "chores", "personal", "life", "home",
        "household", "cleaning", "maintenance", "shopping", "groceries", "health",
        "routine", "leisure", "break", "family", "social", "meditation", "mindfulness",
        "exercise", "workout", "gym", "running", "walking", "stretching", "non-work",
        "not work", "not-work", "off-hours"
    )

    return tagsList.any { tag -> TaskTagSchedulingProfile.tagMatchesKeywords(tag, nonWorkKeywords) }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerbehavioralinsightsenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`
- **Lines of Code:** 41
- **Size:** 1,505 bytes (1.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

object BehavioralInsightsEngine {
    /**
     * Bayesian shrinkage formula for duration estimation calibration.
     * shrinkageFactor = (k0 * prior + n * observedGeometricMean) / (k0 + n)
     */
    fun calculateCalibrationFactor(
        actualMinutes: List<Int>,
        estimatedMinutes: List<Int>,
        k0: Float = 4.0f,
        prior: Float = 1.0f
    ): Float {
        if (actualMinutes.isEmpty() || estimatedMinutes.isEmpty()) return prior
        val n = min(actualMinutes.size, estimatedMinutes.size)
        if (n == 0) return prior

        val ratios = (0 until n).map { i ->
            val est = estimatedMinutes[i].coerceAtLeast(1).toFloat()
            val act = actualMinutes[i].coerceAtLeast(1).toFloat()
            (act / est).coerceIn(0.4f, 3.0f)
        }
        val geometricMean = exp(ratios.map { ln(it.toDouble()) }.average()).toFloat()
        val calibrated = (k0 * prior + n * geometricMean) / (k0 + n)
        return calibrated.coerceIn(0.8f, 1.6f)
    }

    /**
     * Calculates empirical circadian density distribution across 24 hourly bins.
     */
    fun calculateHourlyCircadianDensity(focusMinutesByHour: Map<Int, Int>): List<Float> {
        val totalMinutes = focusMinutesByHour.values.sum().coerceAtLeast(1).toFloat()
        return (0..23).map { hour ->
            (focusMinutesByHour[hour] ?: 0).toFloat() / totalMinutes
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerdependencygraphvalidatorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`
- **Lines of Code:** 117
- **Size:** 4,332 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.service.SchedulableTask

/**
 * ADR-020: Cycle-Safe Dependency Graph Validator & Sanitizer.
 *
 * Ensures that circular dependencies (e.g. A -> B -> A) never cause Kahn's topological sort
 * in `TaskSchedulerService.scheduleDay` to silently drop tasks.
 */
object DependencyGraphValidator {

    data class SanitizedDagResult(
        val tasks: List<SchedulableTask>,
        val brokenCycleEdges: List<Pair<String, String>>
    )

    /**
     * Detects and breaks any cycle in [tasks]' prerequisite graph before Kahn's algorithm runs.
     * Guarantees 100% of input tasks reach the priority queue during `scheduleDay`.
     */
    fun sanitizeDag(tasks: List<SchedulableTask>): SanitizedDagResult {
        if (tasks.size <= 1) return SanitizedDagResult(tasks, emptyList())

        val taskMap = tasks.associateBy { it.id }
        val adj = mutableMapOf<String, MutableSet<String>>() // prereq -> dependent
        val inDegree = mutableMapOf<String, Int>()
        val prereqMap = mutableMapOf<String, MutableSet<String>>()

        for (t in tasks) {
            val validPrereqs = t.prerequisiteIds
                .filter { it != t.id && it in taskMap }
                .toMutableSet()
            prereqMap[t.id] = validPrereqs
            inDegree[t.id] = validPrereqs.size
            for (p in validPrereqs) {
                adj.getOrPut(p) { mutableSetOf() }.add(t.id)
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.forEach { (id, deg) -> if (deg == 0) queue.add(id) }

        val visited = mutableSetOf<String>()
        val brokenEdges = mutableListOf<Pair<String, String>>()

        while (visited.size < tasks.size) {
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (!visited.add(curr)) continue
                for (dep in adj[curr].orEmpty()) {
                    val newDeg = (inDegree[dep] ?: 1) - 1
                    inDegree[dep] = newDeg
                    if (newDeg == 0 && dep !in visited) {
                        queue.add(dep)
                    }
                }
            }

            if (visited.size < tasks.size) {
                // Cycle detected among unvisited nodes!
                // Deterministically select the highest-leverage unvisited task and sever its unvisited incoming edges.
                val victim = tasks
                    .filter { it.id !in visited }
                    .maxWithOrNull(
                        compareBy<SchedulableTask> { it.isFrog }
                            .thenBy { it.priority }
                            .thenBy { it.impactScore }
                            .thenByDescending { it.id }
                    ) ?: break

                val unresolvedPrereqs = prereqMap[victim.id].orEmpty().filter { it !in visited }
                for (p in unresolvedPrereqs) {
                    prereqMap[victim.id]?.remove(p)
                    adj[p]?.remove(victim.id)
                    brokenEdges.add(p to victim.id)
                }
                inDegree[victim.id] = 0
                queue.add(victim.id)
            }
        }

        val sanitizedTasks = tasks.map { task ->
            val cleanPrereqs = prereqMap[task.id].orEmpty()
            task.copy(
                prerequisiteIds = cleanPrereqs,
                parentTaskId = task.parentTaskId
            )
        }

        return SanitizedDagResult(sanitizedTasks, brokenEdges)
    }

    /**
     * Checks whether adding [candidatePrerequisiteId] as a prerequisite of [taskId]
     * would introduce a cycle in [existingDepsByTask].
     */
    fun wouldCreateCycle(
        taskId: String,
        candidatePrerequisiteId: String,
        existingDepsByTask: Map<String, Set<String>>
    ): Boolean {
        if (taskId == candidatePrerequisiteId) return true
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(candidatePrerequisiteId)

        while (stack.isNotEmpty()) {
            val curr = stack.removeLast()
            if (curr == taskId) return true
            if (visited.add(curr)) {
                existingDepsByTask[curr]?.forEach { next -> stack.add(next) }
            }
        }
        return false
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerschedulepreparationorchestratorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`
- **Lines of Code:** 322
- **Size:** 16,064 bytes (15.7 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import kotlin.math.roundToInt
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class OrchestrationOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

/**
 * ADR-020: Unified Foreground & Background Schedule Preparation Orchestrator.
 *
 * Eliminates the split-brain divergence between `PlanViewModelV2.autoScheduleDay` and
 * `ScheduleAutoTasksWorker.doWork` by running the exact same Bayesian calibration,
 * 32-tag profile mapping, cycle-safe DAG sanitization, and Kalman circadian learning pipeline.
 */
@Singleton
class SchedulePreparationOrchestrator @Inject constructor(
    private val taskSchedulerService: TaskSchedulerService,
    private val behavioralSupportService: BehavioralSupportService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) {
    fun orchestrateDaySchedule(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): List<ScheduledPlacement> = orchestrateDayScheduleWithOutcome(
        candidateTasks = candidateTasks,
        allKnownTasks = allKnownTasks,
        closedSessions = closedSessions,
        sleepLogs = sleepLogs,
        prefs = prefs,
        dayDate = dayDate,
        nowMillis = nowMillis,
        busyWindows = busyWindows,
        forceReplan = forceReplan,
        zoneId = zoneId,
        activeTaskId = activeTaskId,
        microStepCounts = microStepCounts
    ).placements

    fun orchestrateDayScheduleWithOutcome(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): OrchestrationOutcome {
        if (candidateTasks.isEmpty()) return OrchestrationOutcome(emptyList())

        val planningCalibration = AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(allKnownTasks)
        val globalCalibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)

        val rawSchedulable = candidateTasks.map { task ->
            val resolvedCategory = when (task.determineCategory()) {
                com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> TaskCategory.DEEP_WORK
                com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> TaskCategory.CREATIVE
                com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> TaskCategory.ADMIN
                com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> TaskCategory.EXERCISE
                com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> TaskCategory.MINDFULNESS
                com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> TaskCategory.GENERAL
            }
            val tagProfiles = TaskTagSchedulingProfile.profilesFor(task.tags)
            val fragTolerance = tagProfiles.minOfOrNull { it.fragmentationTolerance } ?: 0.5f
            val inferredContext = task.contextTag.ifBlank {
                tagProfiles.firstNotNullOfOrNull { it.preferredContext }.orEmpty()
            }
            val calibratedEnjoyment = (enjoymentCalibration.forecast(task.enjoymentScore) / 10.0).roundToInt().coerceIn(0, 10)

            val expectedDuration = TaskBlockPlan.expectedMinutes(task)
            val isDurationExplicit = task.isScheduleLocked

            SchedulableTask(
                id = task.id,
                title = task.title,
                category = resolvedCategory,
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = expectedDuration,
                isDurationExplicit = isDurationExplicit,
                energyDemand = when (task.energyLevel) {
                    EnergyLevel.HIGH -> EnergyDemand.HIGH
                    EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                    else -> EnergyDemand.LOW
                },
                isFrog = task.isFrog,
                scheduledStartMillis = task.resolveScheduledEpochMillis(),
                deadlineMillis = task.resolveDeadlineEpochMillis(),
                isLocked = task.isScheduleLocked,
                parentTaskId = task.parentTaskId,
                completedMicroSteps = microStepCounts[task.id]?.first ?: 0,
                totalMicroSteps = microStepCounts[task.id]?.second ?: 0,
                isCurrentlyActive = (activeTaskId != null && (task.id == activeTaskId || task.id.startsWith("$activeTaskId#"))),
                createdAtMillis = task.createdAt,
                prerequisiteIds = task.dependsOnTaskIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = calibratedEnjoyment,
                postponeCount = task.postponeCount,
                contextTag = inferredContext,
                isAnxietyTask = task.isAnxietyTask,
                hasImplementationIntention = task.ifThenPlan.isNotBlank(),
                fragmentationTolerance = fragTolerance,
                distractionScore = task.distractionScore
            )
        }

        val sanitizedDag = DependencyGraphValidator.sanitizeDag(rawSchedulable).tasks
        val preparedTasks = if (forceReplan) {
            behavioralSupportService.resetScheduleAnchors(sanitizedDag)
        } else {
            sanitizedDag
        }

        val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var wakeAnchor = zonedNow.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (wakeAnchor.toInstant().toEpochMilli() > nowMillis) {
            wakeAnchor = wakeAnchor.minusDays(1)
        }
        val baseWakeMillis = wakeAnchor.toInstant().toEpochMilli()

        val sleepSignal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val lastSleepDurationMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

        val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Bug 1 Fix: filter protected slots by day of week & add recovery buffer
        val protectedSlots = prefs.protectedTimeSlots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .map {
                ProtectedLocalSlot(
                    startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                    startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                    durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                )
            }

        // Bug 3 Fix: parse quick-logged physiological events (meals, workouts, naps) and stimulants
        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
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
                val napDuration = log.durationMinutes.coerceIn(5, 180)
                DaytimeNapBout(
                    startMillis = log.timestampMillis,
                    endMillis = log.timestampMillis + napDuration * 60_000L
                )
            }

        val allNaps = ((sleepSignal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }
        val basePhysioBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = dayDate,
            zoneId = zoneId
        ).filter { it.category == CognitiveBlockCategory.MEAL || it.category == CognitiveBlockCategory.EXERCISE }
        val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks
        val stimulantDoses = (
            parseStimulantLogs(prefs.stimulantLogsJson) +
            com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(prefs.protectedTimeSlots, dayDate, zoneId)
        ).distinctBy { it.timestampMillis to it.type }

        // Bug 4 Fix: pass active uncompleted task IDs to prevent premature placement of cross-day dependents
        val activeUncompletedTaskIds = allKnownTasks
            .filter { it.status == TaskStatus.ACTIVE }
            .map { it.id }
            .toSet()

        // Feature 3 (ADR-023): Social Jetlag Phase-Shift Auto-Pacing
        val sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) }
        val sjlReport = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepWindows,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

        val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
        val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else null

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val observations = buildBehavioralObservations(completedTasks, closedSessions, zoneId)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val weights = PriorityWeightProfile(
            urgencyWeight = prefs.weightDeadlineUrgency.toDouble(),
            impactWeight = prefs.weightImpact.toDouble(),
            circadianMatchWeight = prefs.weightCircadianMatch.toDouble(),
            momentumWeight = prefs.weightSessionProgress.toDouble(),
            frogBonusMultiplier = prefs.weightFrogBonus.toDouble(),
            quadrantWeight = prefs.weightQuadrant.toDouble(),
            priorityLevelWeight = prefs.weightPriorityLevel.toDouble(),
            durationWeight = prefs.weightDuration.toDouble(),
            focusModeWeight = prefs.weightFocusMode.toDouble(),
            planBoostWeight = prefs.weightPlanBoost.toDouble(),
            enjoymentWeight = prefs.weightEnjoyment.toDouble()
        )

        val outcome = taskSchedulerService.scheduleDayWithOutcome(
            tasks = preparedTasks,
            dayDate = dayDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            busyWindows = busyWindows,
            protectedSlots = protectedSlots,
            calibrationRatio = globalCalibrationRatio,
            forceReplan = forceReplan,
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
            activeUncompletedTaskIds = activeUncompletedTaskIds
        )

        return OrchestrationOutcome(outcome.placements, outcome.deferrals)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatacalendarcalendarsyncdatasourcekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`
- **Lines of Code:** 458
- **Size:** 19,880 bytes (19.4 KB)

```kotlin
package com.neuroflow.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface CalendarSyncRepository {
    suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow>
    suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long? = null,
        durationMinutes: Int? = null
    ): Long?
    suspend fun deleteTaskEvent(taskId: String): Boolean
    suspend fun handleTaskStateChanged(task: TaskEntity): Boolean
}

interface AndroidCalendarSyncRepository : CalendarSyncRepository {
    suspend fun findPrimaryWritableCalendarId(): Long?
    suspend fun findEventIdByTaskId(taskId: String): Long?
}

/**
 * ADR-002 Native Android Calendar Sync DataSource
 *
 * Bridges local Android OS Calendar Provider via CalendarContract.Instances and CalendarContract.Events.
 * 100% offline-first, multi-provider (Google, Outlook, Exchange, DAVx5), zero OAuth tokens.
 *
 * Enforces:
 * 1. Filtering out declined events, canceled events, and free availability.
 * 2. Excluding self-authored neuroFlow blocks ("com.neuroflow.app" / "neuroflow://task/...").
 * 3. Sweep-line interval coalescing with 5-minute post-meeting decompression buffer for meetings >= 45m.
 * 4. Loop-safe 2-way write-back for Daily Frogs and locked Focus Blocks with idempotent upsert and lifecycle cleanup.
 */
@Singleton
class CalendarSyncDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarSyncRepository, AndroidCalendarSyncRepository {

    override suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        return withContext(Dispatchers.IO) {
            val raw = queryInstances(startMillis, endMillis)
            coalesceWindows(raw)
        }
    }

    internal fun queryInstances(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return emptyList()
        }

        val results = mutableListOf<TimeSlotWindow>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        )

        val selection = null
        val selectionArgs = null
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val availIdx = cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                val attendeeIdx = cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val statusIdx = cursor.getColumnIndex(CalendarContract.Instances.STATUS)
                val orgIdx = cursor.getColumnIndex(CalendarContract.Instances.ORGANIZER)
                val pkgIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                val appUriIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_URI)

                while (cursor.moveToNext()) {
                    val s = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val e = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val desc = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                    val avail = if (availIdx >= 0) cursor.getInt(availIdx) else CalendarContract.Events.AVAILABILITY_BUSY
                    val attendeeStatus = if (attendeeIdx >= 0) cursor.getInt(attendeeIdx) else CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else CalendarContract.Events.STATUS_CONFIRMED
                    val organizer = if (orgIdx >= 0) cursor.getString(orgIdx) ?: "" else ""
                    val customPkg = if (pkgIdx >= 0) cursor.getString(pkgIdx) ?: "" else ""
                    val customUri = if (appUriIdx >= 0) cursor.getString(appUriIdx) ?: "" else ""

                    // 1. Filter out free availability
                    if (avail == CalendarContract.Events.AVAILABILITY_FREE) {
                        continue
                    }

                    // 2. Filter out declined or canceled events
                    if (attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
                        status == CalendarContract.Events.STATUS_CANCELED) {
                        continue
                    }

                    // 3. Filter out self-authored neuroFlow blocks (loop prevention)
                    val isNeuroFlowBlock = customPkg.equals(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        customUri.startsWith(CUSTOM_APP_URI_PREFIX, ignoreCase = true) ||
                        title.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        desc.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        organizer.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        title.contains("[neuroflow]", ignoreCase = true) ||
                        title.contains("[Flow]", ignoreCase = true) ||
                        desc.contains("[neuroflow]", ignoreCase = true)

                    if (isNeuroFlowBlock) {
                        continue
                    }

                    if (e > s) {
                        results.add(TimeSlotWindow(startMillis = s, endMillis = e))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query calendar provider", e)
        }

        return results
    }

    override suspend fun findPrimaryWritableCalendarId(): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        // 1. Primary writable calendar: IS_PRIMARY == 1 and access >= CAL_ACCESS_CONTRIBUTOR
        try {
            val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Primary calendar query failed, trying fallback", e)
        }

        // 2. Fallback: First writable calendar
        try {
            val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Fallback writable calendar query failed", e)
        }

        null
    }

    override suspend fun findEventIdByTaskId(taskId: String): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        )
        val selection = "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?"
        val selectionArgs = arrayOf(NEUROFLOW_PACKAGE_ID, "$CUSTOM_APP_URI_PREFIX$taskId")

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query event by taskId $taskId", e)
        }

        null
    }

    override suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long?,
        durationMinutes: Int?
    ): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val calendarId = findPrimaryWritableCalendarId() ?: return@withContext null

        val effectiveStart = startMillis
            ?: task.resolveScheduledEpochMillis()
            ?: task.scheduledDate
            ?: System.currentTimeMillis()

        val effectiveDuration = durationMinutes
            ?: (if (task.plannedBlockMinutes > 0) task.plannedBlockMinutes else if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25)

        val endMillis = effectiveStart + (effectiveDuration * 60_000L)

        val cleanTitle = task.title
            .removePrefix("✓ [Flow] ")
            .removePrefix("🎯 [Flow] ")
            .removePrefix("[Flow] ")
            .trim()

        val formattedTitle = when {
            task.status == TaskStatus.COMPLETED -> "✓ [Flow] $cleanTitle"
            task.isFrog -> "🎯 [Flow] $cleanTitle"
            else -> "[Flow] $cleanTitle"
        }

        val formattedDesc = if (task.description.isBlank()) {
            "[neuroflow] Managed focus block"
        } else {
            "${task.description}\n\n[neuroflow] Managed focus block"
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, formattedTitle)
            put(CalendarContract.Events.DESCRIPTION, formattedDesc)
            put(CalendarContract.Events.DTSTART, effectiveStart)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, NEUROFLOW_PACKAGE_ID)
            put(CalendarContract.Events.CUSTOM_APP_URI, "$CUSTOM_APP_URI_PREFIX${task.id}")
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        val existingEventId = findEventIdByTaskId(task.id)

        try {
            if (existingEventId != null) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                context.contentResolver.update(updateUri, values, null, null)
                existingEventId
            } else {
                val insertUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                insertUri?.lastPathSegment?.toLongOrNull()
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to upsert calendar event for task ${task.id}", e)
            null
        }
    }

    override suspend fun deleteTaskEvent(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext false

        val existingEventId = findEventIdByTaskId(taskId) ?: return@withContext false

        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
            val deletedRows = context.contentResolver.delete(deleteUri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to delete calendar event for task $taskId", e)
            false
        }
    }

    override suspend fun handleTaskStateChanged(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        when (task.status) {
            TaskStatus.COMPLETED -> {
                val existingEventId = findEventIdByTaskId(task.id)
                if (existingEventId != null) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
            TaskStatus.ARCHIVED -> {
                deleteTaskEvent(task.id)
            }
            TaskStatus.ACTIVE -> {
                if (task.scheduledDate == null && task.scheduledTime == null) {
                    deleteTaskEvent(task.id)
                } else if (task.isScheduleLocked || task.isFrog) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
        }
    }

    companion object {
        const val NEUROFLOW_PACKAGE_ID = "com.neuroflow.app"
        const val CUSTOM_APP_URI_PREFIX = "neuroflow://task/"
        private const val DECOMPRESSION_THRESHOLD_MILLIS = 45 * 60_000L
        private const val BUFFER_MILLIS = 5 * 60_000L

        /**
         * Sweep-line interval coalescing and post-meeting decompression buffer.
         *
         * Formula (ADR-002 Section 6.4):
         * BusyWindow_effective = [s_merged, e_merged + I(e_merged - s_merged >= 45m) * 5m]
         * Overlapping intervals and adjacent intervals separated by < 5m are coalesced into a unified non-overlapping blocked horizon.
         */
        fun coalesceWindows(rawWindows: List<TimeSlotWindow>): List<TimeSlotWindow> {
            val valid = rawWindows.filter { it.endMillis > it.startMillis }
            if (valid.isEmpty()) return emptyList()

            val sorted = valid.sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
            val merged = mutableListOf<TimeSlotWindow>()

            var curStart = sorted[0].startMillis
            var curEnd = sorted[0].endMillis

            for (i in 1 until sorted.size) {
                val next = sorted[i]
                // Coalesce if overlapping or adjacent separated by < 5 minutes
                if (next.startMillis <= curEnd + BUFFER_MILLIS) {
                    curEnd = max(curEnd, next.endMillis)
                } else {
                    val duration = curEnd - curStart
                    val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                        curEnd + BUFFER_MILLIS
                    } else {
                        curEnd
                    }
                    merged.add(TimeSlotWindow(curStart, effectiveEnd))
                    curStart = next.startMillis
                    curEnd = next.endMillis
                }
            }

            // Final interval
            val duration = curEnd - curStart
            val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                curEnd + BUFFER_MILLIS
            } else {
                curEnd
            }
            merged.add(TimeSlotWindow(curStart, effectiveEnd))

            if (merged.size <= 1) return merged

            // Final sweep to resolve any secondary overlap created by decompression expansion
            val finalPass = mutableListOf<TimeSlotWindow>()
            var pStart = merged[0].startMillis
            var pEnd = merged[0].endMillis

            for (i in 1 until merged.size) {
                val win = merged[i]
                if (win.startMillis <= pEnd) {
                    pEnd = max(pEnd, win.endMillis)
                } else {
                    finalPass.add(TimeSlotWindow(pStart, pEnd))
                    pStart = win.startMillis
                    pEnd = win.endMillis
                }
            }
            finalPass.add(TimeSlotWindow(pStart, pEnd))
            return finalPass
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerscheduleautotasksworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`
- **Lines of Code:** 349
- **Size:** 16,139 bytes (15.8 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleAutoTasksWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val behavioralSupportService: BehavioralSupportService,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionRepository: SessionRepository,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    private val scheduleOrchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = taskSchedulerService,
        behavioralSupportService = behavioralSupportService,
        sleepTelemetryRepository = sleepTelemetryRepository
    )

    companion object {
        const val WORK_NAME = "schedule_auto_tasks_work"
        const val FOREGROUND_TICK_WORK_NAME = "schedule_auto_tasks_foreground_tick"

        @Volatile
        private var isWorkerRunning = false
        private val workerLock = Any()

        fun buildPeriodicWorkRequest(throttleMinutes: Int): PeriodicWorkRequest {
            val throttleInterval = throttleMinutes.coerceIn(15, 240)
            return PeriodicWorkRequest.Builder(
                ScheduleAutoTasksWorker::class.java,
                throttleInterval.toLong(),
                TimeUnit.MINUTES
            ).build()
        }

        fun buildOneTimeWorkRequest(forceReplan: Boolean = false): OneTimeWorkRequest {
            val data = Data.Builder().putBoolean("FORCE_REPLAN", forceReplan).build()
            return OneTimeWorkRequest.Builder(ScheduleAutoTasksWorker::class.java)
                .setInputData(data)
                .build()
        }

        /**
         * Anchors wake time to the exact local `:00` minute of [wakeHour] using DST-safe ZonedDateTime,
         * preventing the up-to-59-minute drift of raw hour subtraction.
         */
        internal fun calculateWakeTimeMillis(
            nowMillis: Long,
            wakeHour: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
            var anchor = zonedNow.toLocalDate()
                .atTime(wakeHour.coerceIn(0, 23), 0)
                .atZone(zoneId)
            if (anchor.toInstant().toEpochMilli() > nowMillis) {
                anchor = anchor.minusDays(1)
            }
            return anchor.toInstant().toEpochMilli()
        }

        internal fun splitMillisToDateAndTime(millis: Long): Pair<Long, Long> {
            val dateStart = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = dateStart.timeInMillis
            val timeOffset = millis - dateMillis
            return dateMillis to timeOffset
        }
    }

    override suspend fun doWork(): Result {
        synchronized(workerLock) {
            if (isWorkerRunning) {
                android.util.Log.w(
                    "ScheduleAutoTasks",
                    "Worker already running - skipping execution"
                )
                return Result.success()
            }
            isWorkerRunning = true
        }

        return try {
            val forceReplan = inputData.getBoolean("FORCE_REPLAN", false)
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.autoSchedulingEnabled) {
                return Result.success()
            }

            val allKnownTasks = taskRepository.getAllTasks()
            val allTasks = allKnownTasks.filter { it.status == TaskStatus.ACTIVE }
            val nowMillis = System.currentTimeMillis()
            val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks)
            val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()

            val zoneId = ZoneId.systemDefault()
            val dayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

            // Separate tasks into unscheduled, missed, and replan candidates
            val unscheduledTasks = allTasks.filter { task ->
                (task.scheduledDate == null || task.scheduledTime == null) &&
                    !AutoSchedulingContracts.hasManualScheduleData(task)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val gracePeriodMillis = if (prefs.isAutoScheduleToggleOn) 5 * 60_000L else 30 * 60_000L
            val missedTasks = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.isAutoScheduled &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    let {
                        val scheduledStart = task.scheduledDate + task.scheduledTime
                        TaskBlockPlan.blockHasExpired(
                            task = task,
                            startMillis = scheduledStart,
                            nowMillis = nowMillis,
                            gracePeriodMillis = gracePeriodMillis
                        ) || (forceReplan && scheduledStart < nowMillis)
                    }
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val replanCandidates = allTasks.filter { task ->
                val scheduledMillis = task.resolveScheduledEpochMillis()
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    scheduledMillis != null &&
                    scheduledMillis >= nowMillis &&
                    PlacementStability.verdict(task, nowMillis, prefs, forceReplan).allowsReplan
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val targetTasksMap = LinkedHashMap<String, TaskEntity>()
            (unscheduledTasks + missedTasks + replanCandidates).forEach { t ->
                val isDateOnlyAnchor = t.scheduledDate != null && t.scheduledTime == null && !t.isAutoScheduled
                targetTasksMap[t.id] = if (isDateOnlyAnchor) t else t.copy(scheduledDate = null, scheduledTime = null)
            }
            val targetTasks = targetTasksMap.values.toList()

            if (targetTasks.isEmpty()) {
                return Result.success()
            }

            // Fixed tasks are active tasks NOT being rescheduled (locked or manual future tasks)
            val fixedTasks = allTasks.filterNot { targetTasksMap.containsKey(it.id) }
            val fixedWindows = fixedTasks.mapNotNull { task ->
                val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
                val duration = TaskBlockPlan.plannedMinutes(task)
                val end = start + duration * 60_000L
                if (end > nowMillis) TimeSlotWindow(start, end) else null
            }

            // Fetch external busy windows from Android Native Calendar Sync
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24 * 3600_000L - 1
            val externalBusy = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
            val totalBusyWindows = externalBusy + fixedWindows

            val allSessions = runCatching {
                sessionRepository.getAllSessions()
            }.getOrDefault(emptyList())
            val closedSessions = allSessions.filter { it.endedAt != null }
            val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId
            val microStepCounts = targetTasks.associate { task ->
                val steps = taskRepository.getMicroSteps(task.id)
                task.id to Pair(steps.count { it.isCompleted }, steps.size)
            }
            val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

            val placements = scheduleOrchestrator.orchestrateDaySchedule(
                candidateTasks = targetTasks,
                allKnownTasks = allKnownTasks,
                closedSessions = closedSessions,
                sleepLogs = sleepLogs,
                prefs = prefs,
                dayDate = dayDate,
                nowMillis = nowMillis,
                busyWindows = totalBusyWindows,
                forceReplan = forceReplan,
                zoneId = zoneId,
                activeTaskId = activeTaskId,
                microStepCounts = microStepCounts
            )

            if (placements.isNotEmpty()) {
                applyPlacementsTransactionally(placements)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ScheduleAutoTasksWorker", "Failed in auto-scheduling pass", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            synchronized(workerLock) {
                isWorkerRunning = false
            }
        }
    }

    private suspend fun applyPlacementsTransactionally(
        placements: List<ScheduledPlacement>
    ) {
        val nowMillis = System.currentTimeMillis()
        val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()
        val placementsByBaseId = placements.groupBy { it.task.id.substringBefore("#part") }

        for ((baseTaskId, taskPlacements) in placementsByBaseId) {
            val primaryPlacement = taskPlacements.first()
            val freshTask = taskRepository.getById(baseTaskId) ?: continue
            if (baseTaskId in focusedTaskIds) continue

            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis)
            ) {
                continue
            }

            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(primaryPlacement.window.startMillis)
            val previousStartMillis = freshTask.resolveScheduledEpochMillis()
            val blockLengthChanged = freshTask.plannedBlockMinutes != primaryPlacement.window.durationMinutes

            if (!PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = primaryPlacement.window.startMillis,
                    nowMillis = nowMillis
                ) && !blockLengthChanged
            ) {
                continue
            }

            val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
            val titleToUse = if (isSplit && !freshTask.title.contains("(Part ")) "${freshTask.title} (Part 1/2)" else freshTask.title
            val updatedTask = freshTask.copy(
                title = titleToUse,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                lastAutoScheduledAt = nowMillis,
                updatedAt = nowMillis
            )

            taskRepository.update(updatedTask)
            if (updatedTask.isFrog) {
                calendarSyncRepository?.upsertTaskEvent(
                    updatedTask,
                    startMillis = primaryPlacement.window.startMillis,
                    durationMinutes = primaryPlacement.window.durationMinutes
                )
            }

            // Persist Ultradian Split Part 2 as a linked continuation block if split occurred
            val part2Placement = taskPlacements.getOrNull(1)
            if (part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
                val (p2Date, p2Time) = splitMillisToDateAndTime(part2Placement.window.startMillis)
                val part2Id = "${baseTaskId}_part2"
                val existingPart2 = taskRepository.getById(part2Id)
                val part2Title = if (!part2Placement.task.title.contains("(Part ")) "${freshTask.title.substringBefore(" (Part ")} (Part 2/2)" else part2Placement.task.title
                val part2Entity = (existingPart2 ?: freshTask).copy(
                    id = part2Id,
                    title = part2Title,
                    parentTaskId = baseTaskId,
                    dependsOnTaskIds = baseTaskId,
                    estimatedDurationMinutes = part2Placement.window.durationMinutes,
                    plannedBlockMinutes = part2Placement.window.durationMinutes,
                    scheduledDate = p2Date,
                    scheduledTime = p2Time,
                    isAutoScheduled = true,
                    isFrog = false,
                    lastAutoScheduledAt = nowMillis,
                    updatedAt = nowMillis
                )
                if (existingPart2 != null) {
                    taskRepository.update(part2Entity)
                } else {
                    taskRepository.insert(part2Entity)
                }
            }
        }
    }

    private fun buildBlockedTaskIds(
        tasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity> = emptyList()
    ): Set<String> {
        val taskMap = (allKnownTasks.ifEmpty { tasks }).associateBy { it.id }
        val candidateIds = tasks.map { it.id }.toSet()
        return tasks.filter { task ->
            if (task.waitingFor.isNotBlank()) {
                return@filter true
            }
            val deps = task.dependsOnTaskIds
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            deps.any { depId ->
                val depTask = taskMap[depId]
                if (depTask == null) return@any true
                if (depTask.status == TaskStatus.COMPLETED) return@any false
                // Candidate tasks scheduled together are ordered by the topological DAG scheduler
                if (depTask.status == TaskStatus.ACTIVE && depId in candidateIds) return@any false
                // Split continuation parts are not blocked by their active parent task
                if (task.parentTaskId == depId && depTask.status == TaskStatus.ACTIVE) return@any false
                true
            }
        }.map { it.id }.toSet()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkernotificationworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`
- **Lines of Code:** 420
- **Size:** 18,657 bytes (18.2 KB)

```kotlin
package com.neuroflow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val CHANNEL_TASKS = "tasks"
const val CHANNEL_FOCUS = "focus"
const val CHANNEL_DAILY = "daily_plan"
const val WORK_DAILY_PLAN = "daily_plan"
const val WORK_STREAK_CHECK = "streak_check"
const val WORK_DEADLINE_ESCALATION = "deadline_escalation"

internal data class NotificationWorkerPlan(
    val dailyPlanEnabled: Boolean,
    val dailyPlanDelayMs: Long,
    val streakEnabled: Boolean,
    val streakDelayMs: Long,
    val escalationEnabled: Boolean
)

internal fun delayUntilHour(hour: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
    return target.timeInMillis - now.timeInMillis
}

internal fun buildNotificationWorkerPlan(
    prefs: UserPreferences,
    nowMillis: Long = System.currentTimeMillis()
): NotificationWorkerPlan {
    return NotificationWorkerPlan(
        dailyPlanEnabled = prefs.dailyPlanNotificationsEnabled,
        dailyPlanDelayMs = delayUntilHour(prefs.dailyPlanNotificationHour, nowMillis),
        streakEnabled = prefs.streakNotificationsEnabled,
        streakDelayMs = delayUntilHour(prefs.streakCheckNotificationHour, nowMillis),
        escalationEnabled = prefs.deadlineEscalationNotificationsEnabled
    )
}

fun scheduleNotificationWorkers(context: Context, prefs: UserPreferences) {
    val workManager = WorkManager.getInstance(context)
    val plan = buildNotificationWorkerPlan(prefs)

    if (plan.dailyPlanEnabled) {
        val dailyPlanRequest = PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.dailyPlanDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_PLAN,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            dailyPlanRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DAILY_PLAN)
    }

    if (plan.streakEnabled) {
        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.streakDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_STREAK_CHECK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            streakCheckRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_STREAK_CHECK)
    }

    if (plan.escalationEnabled) {
        val escalationRequest = PeriodicWorkRequestBuilder<DeadlineEscalationWorker>(4, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DEADLINE_ESCALATION,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            escalationRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DEADLINE_ESCALATION)
    }
}

fun createNotificationChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val taskChannel = NotificationChannel(
        CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Reminders for upcoming task deadlines" }

    val focusChannel = NotificationChannel(
        CHANNEL_FOCUS, "Focus & Streaks", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Focus time and streak notifications" }

    val dailyChannel = NotificationChannel(
        CHANNEL_DAILY, "Daily Plan", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Morning planning notifications" }

    val autonomyNudgeChannel = NotificationChannel(
        "autonomy_nudge", "Autonomy Nudges", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Gentle nudges when a task hasn't been started" }

    manager.createNotificationChannels(listOf(taskChannel, focusChannel, dailyChannel, autonomyNudgeChannel))
}

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: com.neuroflow.app.domain.service.TaskSchedulerService,
    private val circadianEnergyService: com.neuroflow.app.domain.service.CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.dailyPlanNotificationsEnabled) Result.success()
        else {
            val tasks = taskRepository.getActiveTasks()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, prefs.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(prefs.manualChronotype ?: prefs.quizChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val topTasks = tasks.map { task ->
                val resolvedCategory = when (task.determineCategory()) {
                    com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                    com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                    com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> com.neuroflow.app.domain.service.TaskCategory.CREATIVE
                    com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> com.neuroflow.app.domain.service.TaskCategory.ADMIN
                    com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                    com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                    com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> com.neuroflow.app.domain.service.TaskCategory.MINDFULNESS
                    com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> com.neuroflow.app.domain.service.TaskCategory.GENERAL
                }
                val schedulable = com.neuroflow.app.domain.service.SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = resolvedCategory,
                    priority = when (task.priority) {
                        com.neuroflow.app.domain.model.Priority.HIGH -> 2
                        com.neuroflow.app.domain.model.Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        com.neuroflow.app.domain.model.EnergyLevel.HIGH -> com.neuroflow.app.domain.service.EnergyDemand.HIGH
                        com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> com.neuroflow.app.domain.service.EnergyDemand.MODERATE
                        else -> com.neuroflow.app.domain.service.EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId,
                    impactScore = task.impactScore,
                    postponeCount = task.postponeCount,
                    isAnxietyTask = task.isAnxietyTask,
                    hasImplementationIntention = task.ifThenPlan.isNotBlank()
                )
                val score = taskSchedulerService.evaluateTaskScore(schedulable, snapshot).totalScore
                task to score
            }.sortedByDescending { it.second }.take(3).map { it.first }

            if (topTasks.isNotEmpty()) {
                val taskNames = topTasks.joinToString(", ") { it.title }
                showNotification(
                    "Your top tasks today",
                    "Your top 3 tasks: $taskNames. Tap to confirm.",
                    CHANNEL_DAILY
                )
            }
            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DailyPlanWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_DAILY_PLAN, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_DAILY_PLAN = 10_000
    }
}

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        var prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.streakNotificationsEnabled) Result.success()
        else {
            val today = startOfLocalDay(System.currentTimeMillis())
            val yesterday = startOfPreviousLocalDay(today)

            val completedToday = taskRepository.getCompletedTasks().any { task ->
                task.completedAt != null && task.completedAt >= today
            }

            if (!completedToday && prefs.lastActiveDate < yesterday && prefs.dailyStreak > 0) {
                preferencesDataStore.updatePreferences { it.copy(dailyStreak = 0) }
                prefs = prefs.copy(dailyStreak = 0)
            }

            if (!completedToday && prefs.dailyStreak > 0 && taskRepository.getActiveTasks().isNotEmpty()) {
                showNotification(
                    "Streak at risk! 🔥",
                    "Your ${prefs.dailyStreak}-day streak ends tonight. Complete 1 task!",
                    CHANNEL_FOCUS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "StreakCheckWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_STREAK, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_STREAK = 10_001
    }
}

@HiltWorker
class DeadlineEscalationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.deadlineEscalationNotificationsEnabled) Result.success()
        else {
            val now = System.currentTimeMillis()
            val threshold = 48 * 3_600_000L

            val urgentTasks = taskRepository.getActiveTasks().filter { task ->
                task.quadrant != com.neuroflow.app.domain.model.Quadrant.DO_FIRST &&
                task.recurrence == com.neuroflow.app.domain.model.Recurrence.NONE &&
                task.deadlineDate != null &&
                (task.deadlineDate + (task.deadlineTime ?: 0L) - now) in 0L..threshold
            }

            if (urgentTasks.isNotEmpty()) {
                val names = urgentTasks.take(3).joinToString(", ") { it.title }
                val more = if (urgentTasks.size > 3) " +${urgentTasks.size - 3} more" else ""
                showNotification(
                    "⚠️ ${urgentTasks.size} task${if (urgentTasks.size > 1) "s" else ""} due soon",
                    "$names$more — deadline within 48 hours.",
                    CHANNEL_TASKS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DeadlineEscalationWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_ESCALATION, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_ESCALATION = 10_002
    }
}

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.deadlineReminderNotificationsEnabled) return Result.success()

            val taskId = inputData.getString("taskId") ?: return Result.failure()
            val task = taskRepository.getById(taskId) ?: return Result.success()
            if (task.status != TaskStatus.ACTIVE) return Result.success()
            val taskTitle = inputData.getString("taskTitle") ?: task.title
            val minutesBefore = inputData.getLong("minutesBefore", 0L)
            val expectedTargetMs = inputData.getLong("targetMs", -1L)

            val actualTargetMs = task.effectiveReminderTargetMillis() ?: return Result.success()

            if (expectedTargetMs > 0 && kotlin.math.abs(actualTargetMs - expectedTargetMs) > 5 * 60_000L) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            if (actualTargetMs < now - 10 * 60_000L) return Result.success()

            val timeLabel = when (minutesBefore) {
                15L   -> "15 minutes"
                30L   -> "30 minutes"
                60L   -> "1 hour"
                1440L -> "1 day"
                else  -> "$minutesBefore minutes"
            }

            showNotification(
                "⏰ Task due in $timeLabel",
                taskTitle,
                CHANNEL_TASKS
            )

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "TaskReminderWorker failed", e)
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val taskId = inputData.getString("taskId") ?: ""
        manager.notify(taskId.hashCode(), notification)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerfocuswidgetupdateworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`
- **Lines of Code:** 226
- **Size:** 11,652 bytes (11.4 KB)

```kotlin
package com.neuroflow.app.worker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.presentation.widget.CompleteTaskReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import kotlin.math.roundToInt

@HiltWorker
class FocusWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val tasks = taskRepository.getActiveTasks()
            val preferences = preferencesDataStore.preferencesFlow.first()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()

            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, preferences.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(preferences.manualChronotype ?: preferences.quizChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val sortedWithScores = tasks.map { task ->
                val resolvedCategory = when (task.determineCategory()) {
                    com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                    com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> TaskCategory.DEEP_WORK
                    com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> TaskCategory.CREATIVE
                    com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> TaskCategory.ADMIN
                    com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                    com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> TaskCategory.EXERCISE
                    com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> TaskCategory.MINDFULNESS
                    com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> TaskCategory.GENERAL
                }
                val schedulable = SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = resolvedCategory,
                    priority = when (task.priority) {
                        Priority.HIGH -> 2
                        Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        EnergyLevel.HIGH -> EnergyDemand.HIGH
                        EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                        else -> EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId,
                    impactScore = task.impactScore,
                    postponeCount = task.postponeCount,
                    isAnxietyTask = task.isAnxietyTask,
                    hasImplementationIntention = task.ifThenPlan.isNotBlank()
                )
                val breakdown = taskSchedulerService.evaluateTaskScore(schedulable, snapshot)
                task to breakdown
            }.sortedByDescending { it.second.totalScore }

            val topPair = sortedWithScores.firstOrNull()
            val topTask = topPair?.first
            val topScore = topPair?.second?.totalScore?.roundToInt() ?: 0

            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            // 1. Update FocusWidgetProvider (widget_focus)
            val focusViews = RemoteViews(appContext.packageName, R.layout.widget_focus)
            if (topTask != null) {
                focusViews.setTextViewText(R.id.widget_task_title, topTask.title)
                focusViews.setTextViewText(
                    R.id.widget_task_score,
                    "Priority Score: $topScore"
                )

                val startIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val startPendingIntent = PendingIntent.getActivity(
                    appContext, 1001, startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                focusViews.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)
            } else {
                focusViews.setTextViewText(R.id.widget_task_title, "No tasks — add one in neuroFlow")
                focusViews.setTextViewText(R.id.widget_task_score, "Score: —")
            }

            val focusComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.FocusWidgetProvider"
            )
            appWidgetManager.updateAppWidget(focusComponentName, focusViews)

            // 2. Update NextActionWidgetProvider (widget_next_action)
            val nextActionViews = RemoteViews(appContext.packageName, R.layout.widget_next_action)
            val energyText = "⚡ ${snapshot.usableEnergy}% · ${FatigueZone.fromUsableEnergy(snapshot.usableEnergy).label}"
            nextActionViews.setTextViewText(R.id.widget_energy_pill, energyText)

            if (topTask != null) {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, topTask.title)
                val durationText = if (topTask.estimatedDurationMinutes > 0) "${topTask.estimatedDurationMinutes}m" else "Quick"
                val quadrantText = topTask.quadrant.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "$quadrantText · $durationText")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.VISIBLE)

                val focusIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val focusPendingIntent = PendingIntent.getActivity(
                    appContext, 2001, focusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_next_start_button, focusPendingIntent)

                val completeIntent = Intent(appContext, CompleteTaskReceiver::class.java).apply {
                    action = CompleteTaskReceiver.ACTION_COMPLETE_TASK
                    putExtra(CompleteTaskReceiver.EXTRA_TASK_ID, topTask.id)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    appContext, topTask.id.hashCode(), completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_complete_button, completePendingIntent)
            } else {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, "All clear! Plan what's next")
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "No active tasks")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.GONE)
            }

            val containerIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val containerPendingIntent = PendingIntent.getActivity(
                appContext, 2002, containerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            nextActionViews.setOnClickPendingIntent(R.id.widget_next_action_container, containerPendingIntent)

            val nextActionComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.NextActionWidgetProvider"
            )
            appWidgetManager.updateAppWidget(nextActionComponentName, nextActionViews)

        } catch (_: Exception) {
            // Stale widget preferred over crash
        }

        return Result.success()
    }

    companion object {
        fun enqueueImmediateUpdate(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "FocusWidgetUpdateWorker_immediate",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
```

---

