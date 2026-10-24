package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskCategory
import com.neuroflow.app.domain.scheduler.determineCategory
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

object AnalyticsEngine {

    // ── MAPE / SMAPE ─────────────────────────────────────────────────────────

    fun computeMape(estimated: Float, actual: Float): Float {
        if (estimated == 0f) return 0f
        return (abs(actual - estimated) / estimated) * 100f
    }

    fun computeSmape(estimated: Float, actual: Float): Float {
        val denom = (estimated + actual) / 2f
        if (denom == 0f) return 0f
        return (abs(actual - estimated) / denom) * 100f
    }

    fun computeWeightedMape(tasks: List<TaskEntity>): Float {
        val valid = tasks.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        if (valid.isEmpty()) return 0f
        val totalWeight = valid.sumOf { it.estimatedDurationMinutes }.toFloat()
        if (totalWeight <= 0f) return 0f
        val weightedSum = valid.sumOf {
            (computeMape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) * it.estimatedDurationMinutes).toDouble()
        }
        return (weightedSum / totalWeight).toFloat()
    }

    fun computeWeightedSmape(tasks: List<TaskEntity>): Float {
        val valid = tasks.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        if (valid.isEmpty()) return 0f
        val totalWeight = valid.sumOf { it.estimatedDurationMinutes }.toFloat()
        if (totalWeight <= 0f) return 0f
        val weightedSum = valid.sumOf {
            (computeSmape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) * it.estimatedDurationMinutes).toDouble()
        }
        return (weightedSum / totalWeight).toFloat()
    }

    fun mapeGrade(mape: Float): String = when {
        mape == 0f  -> "No data yet — complete tasks with time tracking to see your accuracy"
        mape < 10f  -> "✨ Excellent time estimation!"
        mape < 25f  -> "👍 Good estimation, minor drift"
        mape < 50f  -> "⚠ Moderate estimation error — try breaking tasks into smaller chunks"
        else        -> "🚨 High estimation error — recalibrate your estimates"
    }

    // ── PLANNING CALIBRATION ──────────────────────────────────────────────────

    /**
     * The user's planning bias, learned from completed tasks. MAPE was already computed here but
     * only displayed, so chronic under-booking kept producing plans that were too short.
     *
     * Estimate/actual ratios are multiplicative, so the central tendency is a geometric mean, shrunk
     * toward "no adjustment" by sample count (category -> user global -> 1.0f).
     */
    data class PlanningCalibration(
        val globalFactor: Float,
        private val categoryFactors: Map<String, Float>
    ) {
        /** Category-level factor, or null when there is no history for that kind of work. */
        fun categoryFactor(category: TaskCategory): Float? = categoryFactors[category.name]

        companion object {
            val UNIFORM = PlanningCalibration(1.0f, emptyMap())

            /** Below this many outcomes there is nothing trustworthy to learn from. */
            private const val MIN_SAMPLES = 5

            /** Pseudo-observations of the prior; higher means slower to adopt learned bias. */
            private const val PRIOR_STRENGTH = 4.0

            /** Outlier outcomes are capped so one bad task cannot dominate the learned bias. */
            private const val MIN_RATIO = 0.4f
            private const val MAX_RATIO = 3.0f

            /** Even a confidently-learned bias only moves a plan by this much. */
            private const val MIN_FACTOR = 0.8f
            private const val MAX_FACTOR = 1.6f

            fun from(tasks: List<TaskEntity>): PlanningCalibration {
                val samples = tasks.asSequence()
                    .filter { it.status == TaskStatus.COMPLETED && it.estimatedDurationMinutes > 0 }
                    .mapNotNull { task ->
                        val actual = task.actualDurationMinutes ?: return@mapNotNull null
                        if (actual <= 0f) return@mapNotNull null
                        task.determineCategory() to (actual / task.estimatedDurationMinutes)
                            .coerceIn(MIN_RATIO, MAX_RATIO)
                    }
                    .toList()

                if (samples.size < MIN_SAMPLES) return UNIFORM

                val global = shrinkToward(
                    observed = geometricMean(samples.map { it.second }),
                    prior = 1.0,
                    samples = samples.size
                )

                val categoryFactors = samples
                    .groupBy { it.first }
                    .mapKeys { (category, _) -> category.name }
                    .mapValues { (_, group) ->
                        shrinkToward(
                            observed = geometricMean(group.map { it.second }),
                            prior = global.toDouble(),
                            samples = group.size
                        )
                    }

                return PlanningCalibration(global, categoryFactors)
            }

            private fun geometricMean(ratios: List<Float>): Double =
                ratios.map { ln(it.toDouble()) }.average().let(::exp)

            /** Blend toward the prior by sample count, then clamp so history can never swing a plan far. */
            private fun shrinkToward(observed: Double, prior: Double, samples: Int): Float {
                val weight = samples / (samples + PRIOR_STRENGTH)
                val blended = prior + (observed - prior) * weight
                return blended.toFloat().coerceIn(MIN_FACTOR, MAX_FACTOR)
            }
        }
    }

    /**
     * How the user's enjoyment forecasts compare with how the work actually felt.
     *
     * `enjoymentScore` is a guess made when the task is created, and it feeds temptation-bundling
     * priority for every task afterwards. The affordance rating collected after each session is the
     * measured error of that guess — this turns it into a bounded correction so a user who keeps
     * overestimating how much they'll enjoy something stops being steered by that optimism.
     */
    data class EnjoymentCalibration(val offsetPoints: Float) {

        /** A new task's predicted enjoyment, corrected by measured history. */
        fun forecast(enjoymentScore: Int): Int =
            (enjoymentScore + offsetPoints).roundToInt().coerceIn(0, 100)

        companion object {
            val NEUTRAL = EnjoymentCalibration(0f)

            private const val MIN_SAMPLES = 5
            private const val PRIOR_STRENGTH = 4.0
            /** Ratings run -2..+2; a whole unit of sustained bias is worth this many enjoyment points. */
            private const val POINTS_PER_RATING = 10f
            private const val MAX_OFFSET_POINTS = 15f

            fun from(tasks: List<TaskEntity>): EnjoymentCalibration {
                val ratings = tasks
                    .filter { it.status == TaskStatus.COMPLETED }
                    .mapNotNull { it.affectiveForecastError }
                    .filter { it.isFinite() }
                if (ratings.size < MIN_SAMPLES) return NEUTRAL

                val observed = ratings.average().toFloat() * POINTS_PER_RATING
                val weight = (ratings.size / (ratings.size + PRIOR_STRENGTH)).toFloat()
                return EnjoymentCalibration(
                    (observed * weight).coerceIn(-MAX_OFFSET_POINTS, MAX_OFFSET_POINTS)
                )
            }
        }
    }

    // ── SESSION HELPERS ───────────────────────────────────────────────────────

    /** Returns start-of-day millis for a given timestamp */
    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }

    internal fun isHourInPeakWindow(hour: Int, peakStartHour: Int, peakEndHour: Int): Boolean {
        val h = normalizeHour(hour)
        val start = normalizeHour(peakStartHour)
        val end = normalizeHour(peakEndHour)
        return if (start < end) {
            h in start until end
        } else if (start > end) {
            h >= start || h < end
        } else {
            h == start
        }
    }

    internal fun peakWindowsForDay(dayStartMillis: Long, peakStartHour: Int, peakEndHour: Int): List<Pair<Long, Long>> {
        val start = normalizeHour(peakStartHour)
        val end = normalizeHour(peakEndHour)
        fun atHour(hour: Int): Long = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDayStart = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        return if (start <= end) {
            listOf(atHour(start) to atHour(end))
        } else {
            // Night window: e.g. 22:00 to 02:00
            // Day 1: start hour to next local midnight; day 2: midnight through end hour.
            listOf(
                atHour(start) to nextDayStart,
                dayStartMillis to atHour(end)
            )
        }
    }

    // ── 7-DAY FOCUS TREND ────────────────────────────────────────────────────

    /** Returns list of (dayLabel, focusMinutes) for the last 7 days, oldest first */
    fun sevenDayFocusTrend(
        sessions: List<TimeSessionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Pair<String, Float>> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val todayStart = startOfDay(nowMillis)
        val closedSessions = sessions.filter { it.endedAt != null && it.durationMinutes > 0f }

        return (6 downTo 0).map { daysAgo ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.timeInMillis
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            // Bug 7 & Feature 4 (ADR-025): Proportionally slice cross-midnight sessions across day boundaries
            val totalMins = closedSessions.sumOf { session ->
                val sessionStart = session.startedAt
                val sessionEnd = session.endedAt ?: session.startedAt
                val overlapStart = maxOf(sessionStart, dayStart)
                val overlapEnd = minOf(sessionEnd, dayEnd, nowMillis)
                if (overlapEnd > overlapStart) ((overlapEnd - overlapStart) / 60_000.0) else 0.0
            }.toFloat()

            val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
            val label = dayLabels[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]
            label to totalMins
        }
    }

    // ── SUMMARY ───────────────────────────────────────────────────────────────

    data class AnalyticsSummary(
        // Task counts
        val totalTasks: Int,
        val completedTasks: Int,
        val remainingTasks: Int,
        val completionRate: Float,
        val completedToday: Int,
        // Quadrant / priority breakdown
        val tasksByQuadrant: Map<Quadrant, Int>,
        val tasksByPriority: Map<Priority, Int>,
        val completedTodayByQuadrant: Map<Quadrant, Int>,
        val totalRemainingMinutes: Int,
        // Time tracking
        val focusMinutesToday: Float,
        val focusMinutesTotal: Float,
        val avgSessionMinutes: Float,
        val totalSessions: Int,
        val sessionsToday: Int,
        val mostFocusedTaskTitle: String?,
        val sevenDayTrend: List<Pair<String, Float>>,
        // Estimation accuracy
        val overallMape: Float,
        val weightedMape: Float,
        val underestimatedPct: Float,
        val overestimatedPct:Float,
        val overallSmape: Float,
        val weightedSmape: Float,
        // Habits
        val habitTasksTotal: Int,
        val habitTasksCompleted: Int,
        val habitCompletionRate: Float,
        val longestHabitStreak: Int,
        val activeHabitStreaks: List<Pair<String, Int>>,  // title -> current streak, top 5
        val recurringTasksTotal: Int,
        val recurringTasksCompleted: Int,
        val lockedScheduleTasksTotal: Int,
        val lockedScheduleTasksCompleted: Int,
        // Streaks
        val currentStreak: Int,
        val longestStreak: Int,
        // Procrastination
        val topProcrastinatedTasks: List<TaskEntity>,
        // Neuro Boost stats
        val frogTasksTotal: Int,
        val frogTasksCompleted: Int,
        val anxietyTasksTotal: Int,
        val anxietyTasksCompleted: Int,
        val publicCommitmentTotal: Int,
        val publicCommitmentCompleted: Int,
        val ifThenPlanUsageRate: Float,       // % of tasks with if-then plan
        val contextTagBreakdown: Map<String, Int>,  // tag -> count
        val taskTagBreakdown: Map<String, Int>,     // user tags -> count
        val taskTypeDistribution: Map<String, Int>,  // type name -> count
        // XP / points
        val totalXp: Int,
        val xpToday: Int,
        val xpThisWeek: Int,
        val topXpTasks: List<Pair<String, Int>>,       // title -> points, top 5
        // Peak-hour productivity
        val peakHourFocusMinutes: Float,
        val offPeakFocusMinutes: Float,
        val peakHourTasksCompleted: Int
    )
    fun buildSummary(
        allTasks: List<TaskEntity>,
        allSessions: List<TimeSessionEntity>,
        prefs: UserPreferences,
        nowMillis: Long = System.currentTimeMillis()
    ): AnalyticsSummary {
        val active = allTasks.filter { it.status == TaskStatus.ACTIVE }
        val completed = allTasks.filter { it.status == TaskStatus.COMPLETED }

        // ── Task counts ──────────────────────────────────────────────────────
        val todayStart = startOfDay(nowMillis)
        val todayEnd = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        // Bug 7 Part 1 (ADR-025) & Bug 9 (ADR-030): Clamp today metrics with upper bound <= nowMillis
        val completedToday = completed.count {
            val c = it.completedAt ?: 0L
            c >= todayStart && c <= nowMillis
        }
        val completionRate = if (allTasks.isNotEmpty()) completed.size.toFloat() / allTasks.size * 100f else 0f

        // ── Quadrant / priority ──────────────────────────────────────────────
        val tasksByQuadrant = Quadrant.entries.associateWith { q -> allTasks.count { it.quadrant == q } }
        val tasksByPriority = Priority.entries.associateWith { p -> allTasks.count { it.priority == p } }
        val completedTodayByQuadrant = Quadrant.entries.associateWith { q ->
            completed.count {
                val c = it.completedAt ?: 0L
                it.quadrant == q && c >= todayStart && c <= nowMillis
            }
        }
        val totalRemainingMinutes = active.sumOf { it.estimatedDurationMinutes }

        // ── Session stats ────────────────────────────────────────────────────
        val closedSessions = allSessions.filter { it.endedAt != null && it.durationMinutes > 0f }
        // Bug 7 & Feature 4 (ADR-025): Proportionally slice cross-midnight sessions into today's focus minutes
        val focusMinutesToday = closedSessions.sumOf { session ->
            val sessionStart = session.startedAt
            val sessionEnd = session.endedAt ?: session.startedAt
            val overlapStart = maxOf(sessionStart, todayStart)
            val overlapEnd = minOf(sessionEnd, todayEnd, nowMillis)
            if (overlapEnd > overlapStart) ((overlapEnd - overlapStart) / 60_000.0) else 0.0
        }.toFloat()
        val sessionsToday = closedSessions.count { it.startedAt in todayStart..nowMillis }
        val focusMinutesTotal = closedSessions.sumOf { it.durationMinutes.toDouble() }.toFloat()
        val avgSessionMinutes = if (closedSessions.isNotEmpty())
            focusMinutesTotal / closedSessions.size else 0f
        // Most focused task (most total tracked minutes)
        val mostFocusedTask = allTasks
            .filter { it.totalTimeTrackedMinutes > 0f }
            .maxByOrNull { it.totalTimeTrackedMinutes }

        val sevenDayTrend = sevenDayFocusTrend(allSessions, nowMillis)

        val tracked = completed.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        val overallMape = if (tracked.isNotEmpty())
            tracked.map { computeMape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) }.average().toFloat()
        else 0f
        val overallSmape = if (tracked.isNotEmpty())
            tracked.map { computeSmape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) }.average().toFloat()
        else 0f
        val weightedMape = computeWeightedMape(completed)
        val weightedSmape = computeWeightedSmape(completed)

        val underestimated = tracked.filter { it.actualDurationMinutes!! > it.estimatedDurationMinutes }
        val overestimated = tracked.filter { it.actualDurationMinutes!! < it.estimatedDurationMinutes }
        val underestimatedPct = if (tracked.isNotEmpty()) underestimated.size.toFloat() / tracked.size * 100f else 0f
        val overestimatedPct = if (tracked.isNotEmpty()) overestimated.size.toFloat() / tracked.size * 100f else 0f

// ── Habits ───────────────────────────────────────────────────────────
        // Use recurrence as source of truth — isHabitual is only set on completion
        val habitTasks = allTasks.filter { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE || it.isHabitual }
        val habitCompleted = habitTasks.filter { it.status == TaskStatus.COMPLETED }
        val recurringTasksTotal = allTasks.count { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE }
        val recurringTasksCompleted = completed.count { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE }
        val lockedScheduleTasksTotal = allTasks.count { it.isScheduleLocked }
        val lockedScheduleTasksCompleted = completed.count { it.isScheduleLocked }
        val habitCompletionRate = if (habitTasks.isNotEmpty())
            habitCompleted.size.toFloat() / habitTasks.size * 100f else 0f
        // Best streak across all tasks (active recurring tasks carry the streak forward)
        val longestHabitStreak = allTasks.maxOfOrNull { it.habitStreak } ?: 0
        // Active habits with their current streaks for display
        val activeHabitStreaks = active
            .filter { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE || it.isHabitual }
            .filter { it.habitStreak > 0 }
            .sortedByDescending { it.habitStreak }
            .take(5)
            .map { it.title to it.habitStreak }

        // ── Procrastination ──────────────────────────────────────────────────
        val topProcrastinated = active
            .filter { it.postponeCount > 0 }
            .sortedByDescending { it.postponeCount }
            .take(5)

        // ── Neuro Boost stats ─────────────────────────────────────────────────
        val frogTasks = allTasks.filter { it.isFrog }
        val frogCompleted = frogTasks.filter { it.status == TaskStatus.COMPLETED }

        val anxietyTasks = allTasks.filter { it.isAnxietyTask }
        val anxietyCompleted = anxietyTasks.filter { it.status == TaskStatus.COMPLETED }

        val publicCommitmentTasks = allTasks.filter { it.isPublicCommitment }
        val publicCommitmentCompleted = publicCommitmentTasks.filter { it.status == TaskStatus.COMPLETED }

        val ifThenPlanUsageRate = if (allTasks.isNotEmpty())
            allTasks.count { it.ifThenPlan.isNotBlank() }.toFloat() / allTasks.size * 100f else 0f

        val contextTagBreakdown = allTasks
            .filter { it.contextTag.isNotBlank() }
            .groupBy { it.contextTag }
            .mapValues { it.value.size }

        val taskTagBreakdown = allTasks
            .flatMap { task -> task.tags.split(",").map { it.trim() }.filter { it.isNotBlank() } }
            .groupingBy { it }
            .eachCount()

        val taskTypeDistribution = allTasks
            .groupBy { it.taskType.name }
            .mapValues { it.value.size }

        // ── XP / Points ───────────────────────────────────────────────────────
        val weekStart = Calendar.getInstance().apply {
            timeInMillis = startOfDay(nowMillis)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        val totalXp = completed.sumOf { it.focusModePoints }
        // Bug 9 Fix (ADR-030): Clamp XP metrics so future completions never leak into current period
        val xpToday = completed
            .filter {
                val c = it.completedAt ?: 0L
                c >= todayStart && c <= nowMillis
            }
            .sumOf { it.focusModePoints }
        val xpThisWeek = completed
            .filter {
                val c = it.completedAt ?: 0L
                c >= weekStart && c <= nowMillis
            }
            .sumOf { it.focusModePoints }
        val topXpTasks = completed
            .filter { it.focusModePoints > 0 }
            .sortedByDescending { it.focusModePoints }
            .take(5)
            .map { it.title to it.focusModePoints }

        val (peakStartHour, peakEndHour) = when {
            prefs.manualPeakProfileEnabled -> {
                val startH = (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
                val durHours = maxOf(1, prefs.manualPeakWindow1DurationMinutes / 60)
                val endH = (startH + durHours).coerceIn(0, 23)
                startH to (if (endH == startH) (startH + 1).coerceIn(0, 23) else endH)
            }
            prefs.quizPeakEnabled && prefs.effectivePeakStart >= 0 && prefs.effectivePeakEnd >= 0 -> {
                prefs.effectivePeakStart to prefs.effectivePeakEnd
            }
            else -> {
                prefs.peakEnergyStart to prefs.peakEnergyEnd
            }
        }

        // ── Peak-hour productivity ────────────────────────────────────────────
        // Split each session's duration at peak window boundaries so only the
        // portion that actually fell within peak hours is counted.
        var peakHourFocusMinutes = 0f
        for (session in closedSessions) {
            val sessionStart = session.startedAt
            val sessionEnd = session.endedAt ?: continue
            if (sessionEnd <= sessionStart) continue

            // Build peak windows for the days this session spans (handles overnight sessions)
            val startDay = startOfDay(sessionStart)
            val endDay = startOfDay(sessionEnd)
            var day = startDay
            while (day <= endDay) {
                val windows = peakWindowsForDay(day, peakStartHour, peakEndHour)
                for ((peakWindowStart, peakWindowEnd) in windows) {
                    val overlapStart = maxOf(sessionStart, peakWindowStart)
                    val overlapEnd = minOf(sessionEnd, peakWindowEnd)
                    if (overlapEnd > overlapStart) {
                        peakHourFocusMinutes += (overlapEnd - overlapStart) / 60_000f
                    }
                }
                day = Calendar.getInstance().apply {
                    timeInMillis = day
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            }
        }
        val offPeakFocusMinutes = (focusMinutesTotal - peakHourFocusMinutes).coerceAtLeast(0f)
        val peakHourTasksCompleted = completed.count { task ->
            val completedMs = task.completedAt ?: return@count false
            val cal = Calendar.getInstance().apply { timeInMillis = completedMs }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            isHourInPeakWindow(h, peakStartHour, peakEndHour)
        }

        return AnalyticsSummary(
            totalTasks = allTasks.size,
            completedTasks = completed.size,
            remainingTasks = active.size,
            completionRate = completionRate,
            completedToday = completedToday,
            tasksByQuadrant = tasksByQuadrant,
            tasksByPriority = tasksByPriority,
            completedTodayByQuadrant = completedTodayByQuadrant,
            totalRemainingMinutes = totalRemainingMinutes,
            focusMinutesToday = focusMinutesToday,
            focusMinutesTotal = focusMinutesTotal,
            avgSessionMinutes = avgSessionMinutes,
            totalSessions = closedSessions.size,
            sessionsToday = sessionsToday,
            mostFocusedTaskTitle = mostFocusedTask?.title,
            sevenDayTrend = sevenDayTrend,
            overallMape = overallMape,
            weightedMape = weightedMape,
            underestimatedPct = underestimatedPct,
            overestimatedPct = overestimatedPct,
            overallSmape = overallSmape,
            weightedSmape = weightedSmape,
            habitTasksTotal = habitTasks.size,
            habitTasksCompleted = habitCompleted.size,
            habitCompletionRate = habitCompletionRate,
            longestHabitStreak = longestHabitStreak,
            activeHabitStreaks = activeHabitStreaks,
            recurringTasksTotal = recurringTasksTotal,
            recurringTasksCompleted = recurringTasksCompleted,
            lockedScheduleTasksTotal = lockedScheduleTasksTotal,
            lockedScheduleTasksCompleted = lockedScheduleTasksCompleted,
            currentStreak = prefs.dailyStreak,
            longestStreak = prefs.longestStreak,
            topProcrastinatedTasks = topProcrastinated,
            frogTasksTotal = frogTasks.size,
            frogTasksCompleted = frogCompleted.size,
            anxietyTasksTotal = anxietyTasks.size,
            anxietyTasksCompleted = anxietyCompleted.size,
            publicCommitmentTotal = publicCommitmentTasks.size,
            publicCommitmentCompleted = publicCommitmentCompleted.size,
            ifThenPlanUsageRate = ifThenPlanUsageRate,
            contextTagBreakdown = contextTagBreakdown,
            taskTagBreakdown = taskTagBreakdown,
            taskTypeDistribution = taskTypeDistribution,
            totalXp = totalXp,
            xpToday = xpToday,
            xpThisWeek = xpThisWeek,
            topXpTasks = topXpTasks,
            peakHourFocusMinutes = peakHourFocusMinutes,
            offPeakFocusMinutes = offPeakFocusMinutes,
            peakHourTasksCompleted = peakHourTasksCompleted
        )
    }
}
