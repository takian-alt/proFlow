package com.neuroflow.app.domain.inference

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

data class InferredTaskMetadata(
    val sanitizedTitle: String,
    val durationMinutes: Int,
    val isDurationExplicit: Boolean = false,
    val energyDemand: EnergyLevel,
    val taskType: TaskType,
    val contextTags: List<String>,
    val isFrog: Boolean,
    val priority: Priority = Priority.MEDIUM,
    val quadrant: Quadrant = Quadrant.SCHEDULE,
    val scheduledDate: Long? = null,
    val scheduledTime: Long? = null,
    val deadlineDate: Long? = null,
    val deadlineTime: Long? = null,
    val isHighImpact: Boolean = false,
    val rawDurationMinutes: Int = durationMinutes,
    val calibrationFactorApplied: Float = 1.0f,
    val isEnergyExplicit: Boolean = false,
    val isPriorityExplicit: Boolean = false,
    val isQuadrantExplicit: Boolean = false
) {
    val estimatedDurationMinutes: Int get() = durationMinutes
    val energyLevel: EnergyLevel get() = energyDemand
}

object TaskInferenceEngine {
    private val DURATION_REGEX = Regex(
        """(?i)(?<!\bin\s)\b(?:(\d+)\s*(?:h|hr|hrs|hours?))?\s*(?:(\d+)\s*(?:m|min|mins|minutes?))\b|(?<!\bin\s)\b(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hours?)\b|\b(?:half\s+(?:an?\s+)?hour)\b|\b(?:an?\s+hour)\b"""
    )
    private val TAG_REGEX = Regex("""[@#]([a-zA-Z0-9_\-]+)""")
    private val FROG_REGEX = Regex("""(?i)(?:\uD83D\uDC38|(?:^|\s)(?:!frog|#frog|frog:|\b(?:eat\s+the\s+frog)\b))""")
    private val LIGHTNING_REGEX = Regex("""\u26A1""")

    private val EXPLICIT_HIGH_ENERGY = Regex("""(?i)(?:^|\s)(?:!high\b|#high\b|\bhigh\s+energy\b|\bdeep\s+energy\b|\bheavy\b)""")
    private val EXPLICIT_LOW_ENERGY = Regex("""(?i)(?:^|\s)(?:!low\b|#low\b|\blow\s+energy\b|\bquick\s+energy\b|\blight\b)""")
    private val EXPLICIT_MED_ENERGY = Regex("""(?i)(?:^|\s)(?:!med\b|#med\b|\bmedium\s+energy\b|\bmed\s+energy\b)""")

    private val PRIORITY_P1_REGEX = Regex("""(?i)(?:^|\s)(?:!p1\b|\bp1\b|!urgent\b|\burgent\b|\basap\b)""")
    private val PRIORITY_P2_REGEX = Regex("""(?i)(?:^|\s)(?:!p2\b|\bp2\b)""")
    private val PRIORITY_P3_REGEX = Regex("""(?i)(?:^|\s)(?:!p3\b|\bp3\b)""")

    private val EXPLICIT_Q1 = Regex("""(?i)(?:^|\s)(?:!q1\b|#q1\b|!dofirst\b|#dofirst\b|!do-first\b|#do-first\b|!do_first\b|#do_first\b)""")
    private val EXPLICIT_Q2 = Regex("""(?i)(?:^|\s)(?:!q2\b|#q2\b|!schedule\b|#schedule\b)""")
    private val EXPLICIT_Q3 = Regex("""(?i)(?:^|\s)(?:!q3\b|#q3\b|!delegate\b|#delegate\b)""")
    private val EXPLICIT_Q4 = Regex("""(?i)(?:^|\s)(?:!q4\b|#q4\b|!eliminate\b|#eliminate\b)""")

    private val HIGH_ENERGY_KEYWORDS = Regex("""(?i)\b(deep(\s+work)?|coding|write\s+report|architect|study|exam|complex|thesis|research|debug|refactor)\b""")
    private val LOW_ENERGY_KEYWORDS = Regex("""(?i)\b(quick\s+call|clean\s+desk|water\s+plants|tidy|file\s+expense|errand|inbox|admin)\b""")

    private val ANALYTICAL_KEYWORDS = Regex("""(?i)\b(code|coding|debug|math|analyze|analysis|algorithm|compile|refactor|architect|architecture|finance|audit)\b""")
    private val CREATIVE_KEYWORDS = Regex("""(?i)\b(design|write|draft|brainstorm|compose|sketch|draw|illustrate|ideate)\b""")
    private val ADMIN_KEYWORDS = Regex("""(?i)\b(call|meet|email|sync|discuss|errand|admin|organize|clean|invoice|schedule|order|buy|purchase|bill)\b""")
    private val PHYSICAL_KEYWORDS = Regex("""(?i)\b(gym|run|walk|exercise|workout|stretch|yoga|cardio|lift)\b""")
    private val MINDFULNESS_KEYWORDS = Regex("""(?i)\b(meditate|meditation|breathwork|breathe|journal|mindful|nsdr)\b""")

    private val HIGH_IMPACT_REGEX = Regex("""(?i)(?:^|\s)(?:!impact\b|!star\b|#impact\b|\b(?:high\s+impact|critical|strategic)\b)""")
    private val TODAY_REGEX = Regex("""(?i)\b(today|tonight)\b""")
    private val TOMORROW_REGEX = Regex("""(?i)\b(tomorrow|tmrw)\b""")
    private val DUE_DATE_REGEX = Regex("""(?i)\b(?:due|by|deadline)\s*:?\s*(?:next\s+|this\s+)?(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val SCHEDULE_DATE_REGEX = Regex("""(?i)\b(?:scheduled?(?:\s+for)?|on|next|this)\s*:?\s*(?:next\s+|this\s+)?(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val CLOCK_TIME_REGEX = Regex("""(?i)\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""")
    private val RELATIVE_OFFSET_REGEX = Regex("""(?i)\b(?:(due|deadline)\s+)?in\s+(\d+)\s*(m|min|mins|minutes?|h|hr|hrs|hours?)\b""")
    private val TIME_OF_DAY_REGEX = Regex("""(?i)\b(?:this\s+)?(morning|afternoon|evening|tonight)\b""")

    fun calculateCalibrationFactorFromHistory(tasks: List<TaskEntity>): Float {
        val validPairs = tasks.filter { task ->
            task.status == TaskStatus.COMPLETED &&
                (task.actualDurationMinutes != null && task.actualDurationMinutes > 0f) &&
                task.estimatedDurationMinutes > 0
        }
        if (validPairs.isEmpty()) return 1.0f

        val actuals = validPairs.map { it.actualDurationMinutes!!.roundToInt() }
        val estimates = validPairs.map { it.estimatedDurationMinutes }
        return BehavioralInsightsEngine.calculateCalibrationFactor(
            actualMinutes = actuals,
            estimatedMinutes = estimates
        )
    }

    fun inferFromInput(
        rawInput: String,
        completedTasks: List<TaskEntity>
    ): InferredTaskMetadata {
        val factor = calculateCalibrationFactorFromHistory(completedTasks)
        return inferFromInput(rawInput, calibrationFactor = factor)
    }

    fun inferFromInput(
        rawInput: String,
        calibrationFactor: Float = 1.0f,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): InferredTaskMetadata {
        val text = rawInput.trim()
        val safeCalibration = calibrationFactor.coerceIn(0.8f, 1.6f)
        if (text.isEmpty()) {
            val defaultDuration = (25 * safeCalibration).roundToInt().coerceAtLeast(5)
            return InferredTaskMetadata(
                sanitizedTitle = "",
                durationMinutes = defaultDuration,
                isDurationExplicit = false,
                energyDemand = EnergyLevel.MEDIUM,
                taskType = TaskType.GENERAL,
                contextTags = emptyList(),
                isFrog = false,
                priority = Priority.MEDIUM,
                rawDurationMinutes = 25,
                calibrationFactorApplied = safeCalibration,
                isEnergyExplicit = false,
                isPriorityExplicit = false,
                isQuadrantExplicit = false
            )
        }

        // 1. Task type inference first so untimed tasks get an intelligent baseline duration
        val taskType = when {
            MINDFULNESS_KEYWORDS.containsMatchIn(text) -> TaskType.MINDFULNESS
            ANALYTICAL_KEYWORDS.containsMatchIn(text) -> TaskType.ANALYTICAL
            CREATIVE_KEYWORDS.containsMatchIn(text) -> TaskType.CREATIVE
            PHYSICAL_KEYWORDS.containsMatchIn(text) -> TaskType.PHYSICAL
            ADMIN_KEYWORDS.containsMatchIn(text) -> TaskType.ADMIN
            else -> TaskType.GENERAL
        }

        val defaultArchetypeMinutes = when (taskType) {
            TaskType.ANALYTICAL -> 45
            TaskType.CREATIVE -> 40
            TaskType.PHYSICAL -> 30
            TaskType.MINDFULNESS -> 15
            TaskType.ADMIN -> 15
            TaskType.GENERAL -> 25
        }

        // 2. Duration extraction (preserves exact duration when explicitly stated by user)
        var rawDurationMinutes = defaultArchetypeMinutes
        var isDurationExplicit = false
        val durationMatch = DURATION_REGEX.find(text)
        if (durationMatch != null) {
            isDurationExplicit = true
            val matchedStr = durationMatch.value.lowercase(Locale.ROOT)
            val hoursPart = durationMatch.groups[1]?.value?.toIntOrNull() ?: 0
            val minsPart = durationMatch.groups[2]?.value?.toIntOrNull() ?: 0
            val decimalHours = durationMatch.groups[3]?.value?.toFloatOrNull()

            rawDurationMinutes = when {
                matchedStr.contains("half") -> 30
                matchedStr == "an hour" || matchedStr == "a hour" -> 60
                decimalHours != null -> (decimalHours * 60).roundToInt()
                hoursPart > 0 || minsPart > 0 -> hoursPart * 60 + minsPart
                else -> defaultArchetypeMinutes
            }.coerceIn(5, 720)
        }

        val calibratedDuration = (rawDurationMinutes * safeCalibration).roundToInt().coerceIn(5, 720)

        // 3. Context tags extraction (@work, #finance)
        val reservedTokens = setOf(
            "high", "low", "med", "medium", "p1", "p2", "p3", "frog", "urgent", "impact", "star",
            "q1", "q2", "q3", "q4", "dofirst", "do-first", "do_first", "delegate", "eliminate", "schedule"
        )
        val tags = TAG_REGEX.findAll(text)
            .map { it.groupValues[1].lowercase(Locale.ROOT) }
            .filterNot { it in reservedTokens }
            .distinct()
            .toList()

        // 4. Frog & Impact detection
        val isFrog = FROG_REGEX.containsMatchIn(text)
        val isHighImpact = isFrog || HIGH_IMPACT_REGEX.containsMatchIn(text)
        val hasLightning = LIGHTNING_REGEX.containsMatchIn(text)

        // 5. Priority & Eisenhower Quadrant detection
        val isExplicitPriority = isFrog || isHighImpact || PRIORITY_P1_REGEX.containsMatchIn(text) || PRIORITY_P2_REGEX.containsMatchIn(text) || PRIORITY_P3_REGEX.containsMatchIn(text)
        val isUrgent = PRIORITY_P1_REGEX.containsMatchIn(text)
        val priority = when {
            isFrog || isUrgent || isHighImpact -> Priority.HIGH
            PRIORITY_P3_REGEX.containsMatchIn(text) -> Priority.LOW
            PRIORITY_P2_REGEX.containsMatchIn(text) -> Priority.MEDIUM
            else -> Priority.MEDIUM
        }

        // 6. Energy demand heuristic
        val isExplicitEnergy = hasLightning || EXPLICIT_HIGH_ENERGY.containsMatchIn(text) || EXPLICIT_LOW_ENERGY.containsMatchIn(text) || EXPLICIT_MED_ENERGY.containsMatchIn(text)
        val energyDemand = when {
            hasLightning || EXPLICIT_HIGH_ENERGY.containsMatchIn(text) -> EnergyLevel.HIGH
            EXPLICIT_LOW_ENERGY.containsMatchIn(text) -> EnergyLevel.LOW
            EXPLICIT_MED_ENERGY.containsMatchIn(text) -> EnergyLevel.MEDIUM
            HIGH_ENERGY_KEYWORDS.containsMatchIn(text) || isFrog -> EnergyLevel.HIGH
            LOW_ENERGY_KEYWORDS.containsMatchIn(text) || taskType == TaskType.MINDFULNESS -> EnergyLevel.LOW
            rawDurationMinutes >= 45 -> EnergyLevel.HIGH
            rawDurationMinutes <= 15 -> EnergyLevel.LOW
            else -> EnergyLevel.MEDIUM
        }

        // 7. DST-safe Date & Clock-Time inference
        val todayLocal = LocalDate.now(zoneId)
        val todayStartMillis = todayLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStartMillis = todayLocal.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        var inferredDeadlineDate: Long? = null
        var inferredDeadlineTimeMillis: Long? = null
        var inferredScheduledDate: Long? = null
        var inferredScheduledTimeMillis: Long? = null

        val dueMatch = DUE_DATE_REGEX.find(text)
        if (dueMatch != null) {
            val token = dueMatch.groupValues[1].lowercase(Locale.ROOT)
            val isNext = dueMatch.value.contains("next", ignoreCase = true) || text.contains(Regex("""(?i)\bnext\s+""" + token))
            inferredDeadlineDate = resolveDateTokenMillis(token, todayLocal, zoneId, isNext)
        } else if (TODAY_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = todayStartMillis
        } else if (TOMORROW_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = tomorrowStartMillis
        }

        val scheduleMatch = SCHEDULE_DATE_REGEX.find(text)?.takeIf { match ->
            dueMatch == null || (match.range.first > dueMatch.range.last || match.range.last < dueMatch.range.first)
        }
        if (scheduleMatch != null) {
            val token = scheduleMatch.groupValues[1].lowercase(Locale.ROOT)
            val isNext = scheduleMatch.value.contains("next", ignoreCase = true) || text.contains(Regex("""(?i)\bnext\s+""" + token))
            inferredScheduledDate = resolveDateTokenMillis(token, todayLocal, zoneId, isNext)
        } else if (inferredDeadlineDate == null) {
            if (TODAY_REGEX.containsMatchIn(text)) inferredScheduledDate = todayStartMillis
            else if (TOMORROW_REGEX.containsMatchIn(text)) inferredScheduledDate = tomorrowStartMillis
        }

        val clockMatch = CLOCK_TIME_REGEX.find(text)
        if (clockMatch != null) {
            val rawHour = clockMatch.groupValues[1].toIntOrNull() ?: 9
            val rawMin = clockMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val meridiem = clockMatch.groupValues[3].lowercase(Locale.ROOT)
            val hour24 = when {
                meridiem == "pm" && rawHour in 1..11 -> rawHour + 12
                meridiem == "am" && rawHour == 12 -> 0
                meridiem.isEmpty() && rawHour in 1..6 -> rawHour + 12
                else -> rawHour.coerceIn(0, 23)
            }
            val clockMillis = LocalTime.of(hour24, rawMin).toSecondOfDay() * 1000L
            val isDueTime = (dueMatch != null && scheduleMatch == null) ||
                (inferredDeadlineDate != null && inferredScheduledDate == null && (text.contains("due at", ignoreCase = true) || text.contains("deadline at", ignoreCase = true) || text.contains("by", ignoreCase = true)))
            if (isDueTime) {
                inferredDeadlineTimeMillis = clockMillis
            } else {
                inferredScheduledTimeMillis = clockMillis
                if (inferredScheduledDate == null) {
                    inferredScheduledDate = todayStartMillis
                }
            }
        } else {
            val relMatch = RELATIVE_OFFSET_REGEX.find(text)
            if (relMatch != null) {
                val isDueKeyword = relMatch.groupValues[1].isNotBlank()
                val amount = relMatch.groupValues[2].toLongOrNull() ?: 1L
                val unit = relMatch.groupValues[3].lowercase(Locale.ROOT)
                val offsetMins = if (unit.startsWith("h")) amount * 60L else amount
                val targetDateTime = LocalDateTime.now(zoneId).plusMinutes(offsetMins)
                val isDueOffset = isDueKeyword ||
                    (dueMatch != null && scheduleMatch == null)
                if (isDueOffset) {
                    inferredDeadlineTimeMillis = targetDateTime.toLocalTime().toSecondOfDay() * 1000L
                    if (inferredDeadlineDate == null) {
                        inferredDeadlineDate = targetDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                    }
                } else {
                    inferredScheduledTimeMillis = targetDateTime.toLocalTime().toSecondOfDay() * 1000L
                    if (inferredScheduledDate == null) {
                        inferredScheduledDate = targetDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                    }
                }
            } else {
                val todMatch = TIME_OF_DAY_REGEX.find(text)
                if (todMatch != null) {
                    val tod = todMatch.groupValues[1].lowercase(Locale.ROOT)
                    val targetLocalTime = when (tod) {
                        "morning" -> LocalTime.of(9, 30)
                        "afternoon" -> LocalTime.of(14, 0)
                        "evening", "tonight" -> LocalTime.of(19, 30)
                        else -> null
                    }
                    if (targetLocalTime != null) {
                        val isDueTime = (dueMatch != null && scheduleMatch == null) ||
                            (inferredDeadlineDate != null && inferredScheduledDate == null && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true) || text.contains("by", ignoreCase = true)))
                        if (isDueTime) {
                            inferredDeadlineTimeMillis = targetLocalTime.toSecondOfDay() * 1000L
                        } else {
                            inferredScheduledTimeMillis = targetLocalTime.toSecondOfDay() * 1000L
                            if (inferredScheduledDate == null) {
                                inferredScheduledDate = todayStartMillis
                            }
                        }
                    }
                }
            }
        }

        val explicitQuadrant = when {
            EXPLICIT_Q1.containsMatchIn(text) -> Quadrant.DO_FIRST
            EXPLICIT_Q2.containsMatchIn(text) -> Quadrant.SCHEDULE
            EXPLICIT_Q3.containsMatchIn(text) -> Quadrant.DELEGATE
            EXPLICIT_Q4.containsMatchIn(text) -> Quadrant.ELIMINATE
            else -> null
        }
        val isExplicitQuadrant = explicitQuadrant != null

        val quadrant = explicitQuadrant ?: when {
            isUrgent && isHighImpact -> Quadrant.DO_FIRST
            isHighImpact || isFrog -> Quadrant.SCHEDULE
            isUrgent -> Quadrant.DELEGATE
            else -> Quadrant.SCHEDULE
        }

        // 8. Title sanitization
        var cleanTitle = text
            .replace(FROG_REGEX, " ")
            .replace(LIGHTNING_REGEX, " ")
            .replace(RELATIVE_OFFSET_REGEX, " ")
            .replace(DURATION_REGEX, " ")
            .replace(EXPLICIT_HIGH_ENERGY, " ")
            .replace(EXPLICIT_LOW_ENERGY, " ")
            .replace(EXPLICIT_MED_ENERGY, " ")
            .replace(PRIORITY_P1_REGEX, " ")
            .replace(PRIORITY_P2_REGEX, " ")
            .replace(PRIORITY_P3_REGEX, " ")
            .replace(EXPLICIT_Q1, " ")
            .replace(EXPLICIT_Q2, " ")
            .replace(EXPLICIT_Q3, " ")
            .replace(EXPLICIT_Q4, " ")
            .replace(HIGH_IMPACT_REGEX, " ")
            .replace(DUE_DATE_REGEX, " ")
            .replace(SCHEDULE_DATE_REGEX, " ")
            .replace(CLOCK_TIME_REGEX, " ")
            .replace(TIME_OF_DAY_REGEX, " ")
            .replace(TODAY_REGEX, " ")
            .replace(TOMORROW_REGEX, " ")
            .replace(TAG_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', ',')

        if (cleanTitle.isEmpty()) {
            cleanTitle = text.take(40).trim()
        }

        return InferredTaskMetadata(
            sanitizedTitle = cleanTitle,
            durationMinutes = calibratedDuration,
            isDurationExplicit = isDurationExplicit,
            energyDemand = energyDemand,
            taskType = taskType,
            contextTags = tags,
            isFrog = isFrog,
            priority = priority,
            quadrant = quadrant,
            scheduledDate = inferredScheduledDate,
            scheduledTime = inferredScheduledTimeMillis,
            deadlineDate = inferredDeadlineDate,
            deadlineTime = inferredDeadlineTimeMillis,
            isHighImpact = isHighImpact,
            rawDurationMinutes = rawDurationMinutes,
            calibrationFactorApplied = safeCalibration,
            isEnergyExplicit = isExplicitEnergy,
            isPriorityExplicit = isExplicitPriority,
            isQuadrantExplicit = isExplicitQuadrant
        )
    }

    private fun resolveDateTokenMillis(
        token: String,
        todayLocal: LocalDate,
        zoneId: ZoneId,
        isNextModifier: Boolean = false
    ): Long {
        val targetDate = when (token) {
            "today", "tonight" -> todayLocal
            "tomorrow", "tmrw" -> todayLocal.plusDays(1)
            "monday", "mon" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.MONDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.MONDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            "tuesday", "tue" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.TUESDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.TUESDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            "wednesday", "wed" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.WEDNESDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
            "thursday", "thu" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.THURSDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.THURSDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            "friday", "fri" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.FRIDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            "saturday", "sat" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.SATURDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            "sunday", "sun" -> if (isNextModifier || todayLocal.dayOfWeek == DayOfWeek.SUNDAY) todayLocal.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)) else todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            else -> todayLocal
        }
        return targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
