package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory as ServiceTaskCategory
import kotlin.math.roundToInt

/**
 * Single source of truth for translating domain [TaskEntity] and [UserPreferences]
 * records into rich [SchedulableTask] and [PriorityWeightProfile] inputs across
 * Auto-Schedule, Auto-Plan (Daily Blueprint), Auto-Balance (Multi-Day), Flow recommendations,
 * home screen widgets, and notification workers.
 */
object SchedulableTaskMapper {

    fun toServiceCategory(task: TaskEntity): ServiceTaskCategory =
        when (task.determineCategory()) {
            TaskCategory.HARD_WORK,
            TaskCategory.ANALYTICAL -> ServiceTaskCategory.DEEP_WORK
            TaskCategory.CREATIVE -> ServiceTaskCategory.CREATIVE
            TaskCategory.ROUTINE -> ServiceTaskCategory.ADMIN
            TaskCategory.EXERCISE,
            TaskCategory.PHYSICAL -> ServiceTaskCategory.EXERCISE
            TaskCategory.MINDFULNESS -> ServiceTaskCategory.MINDFULNESS
            TaskCategory.FLEXIBLE -> ServiceTaskCategory.GENERAL
        }

    fun toPriorityWeights(prefs: UserPreferences): PriorityWeightProfile = PriorityWeightProfile(
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

    fun toSchedulableTask(
        task: TaskEntity,
        allContextTasks: List<TaskEntity> = emptyList(),
        dayStartMillis: Long? = null,
        enjoymentCalibration: AnalyticsEngine.EnjoymentCalibration? = null,
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap(),
        usePlannedBlockForScoring: Boolean = false,
        goalsById: Map<String, GoalEntity> = emptyMap()
    ): SchedulableTask {
        val parentGoal = task.goalId?.let { goalsById[it] }
        val effectiveTagsCsv = mergeTagsCsv(task.tags, parentGoal?.tags.orEmpty())
        val effectiveTaskForRouting = if (effectiveTagsCsv != task.tags) {
            task.copy(tags = effectiveTagsCsv)
        } else {
            task
        }
        val tagProfiles = TaskTagSchedulingProfile.profilesFor(effectiveTagsCsv)
        val fragTolerance = tagProfiles.minOfOrNull { it.fragmentationTolerance } ?: 0.5f
        val inferredContext = task.contextTag.ifBlank {
            tagProfiles.firstNotNullOfOrNull { it.preferredContext }.orEmpty()
        }
        val perTaskAdjustedEnjoyment = task.affectiveForecastError?.let { error ->
            (task.enjoymentScore + error).roundToInt().coerceIn(0, 100)
        } ?: task.enjoymentScore
        val rawEnjoyment = enjoymentCalibration?.forecast(perTaskAdjustedEnjoyment) ?: perTaskAdjustedEnjoyment
        val calibratedEnjoyment = (rawEnjoyment / 10.0).roundToInt().coerceIn(0, 10)

        val isPart1 = task.id.contains(Regex("""[#_]part1(\D|$)""")) || task.title.contains("(Part 1/")
        val isPart2Continuation = !isPart1 && (
            task.id.endsWith("_part2") ||
            task.id.contains(Regex("""[#_]part[2-9]""")) ||
            (task.parentTaskId != null && (task.id.contains(Regex("""[#_]part""")) || task.title.contains(Regex("""\((Part|part) \d+/\d+\)""")))) ||
            task.title.contains(Regex("""\(Part (?!1/)\d+/\d+\)"""))
        )

        val cleanTitle = if (!isPart2Continuation) {
            task.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").replace(Regex("""\s*#part\d+"""), "").trim()
        } else {
            task.title
        }

        val cleanId = cleanSplitTaskId(task.id)
        val hasContinuationInCandidates = allContextTasks.any { other ->
            other.id != task.id && (
                other.parentTaskId == task.id ||
                other.parentTaskId == cleanId ||
                other.id == "${task.id}_part2" ||
                other.id == "${task.id}#part2" ||
                other.id == "${cleanId}_part2" ||
                other.id == "${cleanId}#part2" ||
                (cleanSplitTaskId(other.id) == cleanId && other.id != cleanId)
            )
        }

        val estimatedDuration = if (usePlannedBlockForScoring) {
            TaskBlockPlan.plannedMinutes(task)
        } else {
            val taskToEstimate = if (!isPart2Continuation && !task.isScheduleLocked && isPart1 && !hasContinuationInCandidates) {
                task.copy(title = cleanTitle, plannedBlockMinutes = 0)
            } else {
                task
            }
            TaskBlockPlan.expectedMinutes(taskToEstimate)
        }

        val isDurationExplicit = task.isScheduleLocked || isPart2Continuation

        val parsedPrereqs = task.dependsOnTaskIds.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        val parent = task.parentTaskId?.trim().orEmpty()
        if (parent.isNotBlank() && parent != task.id && !isPart1) {
            parsedPrereqs.add(parent)
        }
        val partMatch = Regex("""[#_]part(\d+)""").find(task.id)
        val partNumber = partMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (partNumber != null && partNumber >= 2) {
            val inferredParent = cleanId
            if (inferredParent.isNotBlank() && inferredParent != task.id) {
                parsedPrereqs.add(inferredParent)
                val p1Hash = "${inferredParent}#part1"
                val p1Underscore = "${inferredParent}_part1"
                if (allContextTasks.any { it.id == p1Hash }) parsedPrereqs.add(p1Hash)
                if (allContextTasks.any { it.id == p1Underscore }) parsedPrereqs.add(p1Underscore)
                if (partNumber > 2) {
                    val prevHash = "${inferredParent}#part${partNumber - 1}"
                    val prevUnderscore = "${inferredParent}_part${partNumber - 1}"
                    if (allContextTasks.any { it.id == prevHash }) parsedPrereqs.add(prevHash)
                    if (allContextTasks.any { it.id == prevUnderscore }) parsedPrereqs.add(prevUnderscore)
                }
            }
        } else if (task.id.endsWith("_part2")) {
            val inferredParent = task.id.removeSuffix("_part2")
            if (inferredParent.isNotBlank() && inferredParent != task.id) {
                parsedPrereqs.add(inferredParent)
            }
        }

        val isPersonal = task.isPersonalTask()
        val scheduledStartMillis = if (task.scheduledTime != null) {
            if (dayStartMillis != null) task.resolveScheduledEpochMillis(dayStartMillis)
            else task.resolveScheduledEpochMillis()
        } else {
            null
        }
        val deadlineMillis = if (dayStartMillis != null) {
            task.resolveDeadlineEpochMillis(dayStartMillis)
        } else {
            task.resolveDeadlineEpochMillis()
        }

        val effectiveGoalRiskLevel = run {
            var risk = task.goalRiskLevel.coerceIn(0, 2)
            if (parentGoal != null) {
                if (parentGoal.priority == Priority.HIGH) {
                    risk = maxOf(risk, 1)
                }
                val targetDate = parentGoal.targetDate
                val anchor = dayStartMillis ?: System.currentTimeMillis()
                if (targetDate != null && targetDate - anchor <= 7L * 24 * 3600_000L) {
                    risk = maxOf(risk, 2)
                }
            }
            risk
        }

        return SchedulableTask(
            id = task.id,
            title = if (!task.isScheduleLocked && !isPart2Continuation && !usePlannedBlockForScoring) cleanTitle else task.title,
            category = toServiceCategory(effectiveTaskForRouting),
            priority = when (task.priority) {
                Priority.HIGH -> 2
                Priority.MEDIUM -> 1
                Priority.LOW -> 0
            },
            estimatedMinutes = estimatedDuration,
            isDurationExplicit = isDurationExplicit,
            energyDemand = when (task.energyLevel) {
                EnergyLevel.HIGH -> EnergyDemand.HIGH
                EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                else -> EnergyDemand.LOW
            },
            isFrog = task.isFrog,
            scheduledStartMillis = scheduledStartMillis,
            deadlineMillis = deadlineMillis,
            isLocked = task.isScheduleLocked,
            parentTaskId = task.parentTaskId,
            completedMicroSteps = microStepCounts[task.id]?.first ?: 0,
            totalMicroSteps = microStepCounts[task.id]?.second ?: 0,
            isCurrentlyActive = activeTaskId != null && (
                task.id == activeTaskId ||
                task.id.startsWith("$activeTaskId#") ||
                task.id.startsWith("${activeTaskId}_") ||
                task.parentTaskId == activeTaskId
            ),
            createdAtMillis = task.createdAt,
            prerequisiteIds = parsedPrereqs,
            impactScore = task.impactScore,
            estimatedEnjoyment = calibratedEnjoyment,
            postponeCount = task.postponeCount,
            contextTag = inferredContext,
            isAnxietyTask = task.isAnxietyTask,
            hasImplementationIntention = task.ifThenPlan.isNotBlank(),
            fragmentationTolerance = fragTolerance,
            distractionScore = task.distractionScore,
            isPersonalTask = isPersonal,
            userSchedulingRating = task.userSchedulingRating,
            schedulingRejectedSlotHour = task.schedulingRejectedSlotHour,
            quadrant = task.quadrant,
            valueScore = task.valueScore,
            effortScore = task.effortScore,
            preferredDayStartMillis = task.effectiveScheduledDateMillis() ?: task.habitDate,
            isWaitingFor = task.waitingFor.isNotBlank(),
            goalRiskLevel = effectiveGoalRiskLevel,
            hasGoalAlignment = !task.goalId.isNullOrBlank(),
            isPublicCommitment = task.isPublicCommitment,
            isHabitual = task.isHabitual || task.recurrence != Recurrence.NONE,
            habitStreak = task.habitStreak.coerceAtLeast(0),
            priorSessionCount = task.sessionCount.coerceAtLeast(0)
        )
    }

    fun mergeTagsCsv(primaryCsv: String, secondaryCsv: String): String {
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<String>()
        listOf(primaryCsv, secondaryCsv).forEach { csv ->
            csv.split(',')
                .map { it.trim().trimStart('#', '@') }
                .filter { it.isNotBlank() }
                .forEach { tag ->
                    val key = tag.lowercase()
                    if (seen.add(key)) {
                        result.add(tag)
                    }
                }
        }
        return result.joinToString(",")
    }

    fun computeDependentCounts(tasks: List<SchedulableTask>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (task in tasks) {
            for (prereqId in task.prerequisiteIds) {
                counts[prereqId] = (counts[prereqId] ?: 0) + 1
            }
        }
        return counts
    }

    fun cleanSplitTaskId(id: String): String =
        id.replace(Regex("""[#_]part\d+.*$"""), "")
}
