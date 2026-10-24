# neuroFlow Core Engine — Part 3: Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines

**Description:** Regex & Heuristic TaskNaturalLanguageParser, TaskInferenceEngine (duration heuristics, DST midnight bounds, EWMA duration calibration), GeminiAiOnlineService (BYOK, multi-model upstream cascade, TTL caching, 429 backoff, single-call enrichment, weekly avoidance audit), SmartTaskDecompositionService (6-archetype local fallback), BehavioralSupportService (WOOP, implementation intentions, doorway micro-steps), and AnalyticsEngine.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 9  
**Lines of Code:** 2,484  
**Uncompressed Size:** 111.4 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`](#appsrcmainjavacomneuroflowappdomainservicetasknaturallanguageparserkt)
- [`app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`](#appsrcmainjavacomneuroflowappdomaininferencetaskinferenceenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`](#appsrcmainjavacomneuroflowappdomainservicegeminiaionlineservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`](#appsrcmainjavacomneuroflowappdomainservicesmarttaskdecompositionservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`](#appsrcmainjavacomneuroflowappdomainservicebehavioralsupportservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`](#appsrcmainjavacomneuroflowappdomainenginewoopenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`](#appsrcmainjavacomneuroflowappdomainengineautonomynudgeenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`](#appsrcmainjavacomneuroflowappdomainenginefreshstartenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`](#appsrcmainjavacomneuroflowappdomainengineanalyticsenginekt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowappdomainservicetasknaturallanguageparserkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`
- **Lines of Code:** 116
- **Size:** 5,082 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTaskInput(
    val cleanTitle: String,
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean,
    val energyDemand: EnergyDemand,
    val category: TaskCategory,
    val isFrog: Boolean
)

interface TaskNaturalLanguageParser {
    fun parse(rawInput: String): ParsedTaskInput
}

@Singleton
class DefaultTaskNaturalLanguageParser @Inject constructor() : TaskNaturalLanguageParser {

    private val durationRegex = Regex("""\b(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)\b""", RegexOption.IGNORE_CASE)
    private val frogRegex = Regex("""(\uD83D\uDC38|#frog|\bfrog\b)""", RegexOption.IGNORE_CASE)
    private val lightningRegex = Regex("""\u26A1""")

    override fun parse(rawInput: String): ParsedTaskInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedTaskInput(
                cleanTitle = "",
                estimatedMinutes = 25,
                isDurationExplicit = false,
                energyDemand = EnergyDemand.MODERATE,
                category = TaskCategory.GENERAL,
                isFrog = false
            )
        }

        var workingTitle = trimmed
        var estimatedMinutes = 25
        var isDurationExplicit = false

        // 1. Check for verbal phrases
        when {
            workingTitle.contains("half hour", ignoreCase = true) -> {
                estimatedMinutes = 30
                isDurationExplicit = true
                workingTitle = workingTitle.replace(Regex("""\bhalf hour\b""", RegexOption.IGNORE_CASE), "")
            }
            workingTitle.contains("an hour", ignoreCase = true) || workingTitle.contains("1 hour", ignoreCase = true) -> {
                estimatedMinutes = 60
                isDurationExplicit = true
                workingTitle = workingTitle.replace(Regex("""\b(an|1)\s+hour\b""", RegexOption.IGNORE_CASE), "")
            }
            else -> {
                val match = durationRegex.find(workingTitle)
                if (match != null) {
                    val amount = match.groupValues[1].toIntOrNull() ?: 25
                    val unit = match.groupValues[2].lowercase()
                    estimatedMinutes = if (unit.startsWith("h")) amount * 60 else amount
                    isDurationExplicit = true
                    workingTitle = workingTitle.removeRange(match.range)
                }
            }
        }

        // 2. Frog Detection
        val isFrog = frogRegex.containsMatchIn(workingTitle)
        if (isFrog) {
            workingTitle = frogRegex.replace(workingTitle, "")
        }

        // 3. Energy Demand Detection
        val hasLightning = lightningRegex.containsMatchIn(workingTitle)
        if (hasLightning) {
            workingTitle = lightningRegex.replace(workingTitle, "")
        }

        val lower = workingTitle.lowercase()
        val energyDemand = when {
            hasLightning || lower.contains("high energy") || lower.contains("heavy") || lower.contains("intense") -> {
                workingTitle = workingTitle.replace(Regex("""\b(high energy|heavy|intense)\b""", RegexOption.IGNORE_CASE), "")
                EnergyDemand.HIGH
            }
            lower.contains("low energy") || lower.contains("light") || lower.contains("chill") || lower.contains("easy") -> {
                workingTitle = workingTitle.replace(Regex("""\b(low energy|light|chill|easy)\b""", RegexOption.IGNORE_CASE), "")
                EnergyDemand.LOW
            }
            else -> EnergyDemand.MODERATE
        }

        // 4. Category Detection
        val category = when {
            lower.contains("deep") || lower.contains("study") || lower.contains("code") || lower.contains("program") || lower.contains("research") -> TaskCategory.DEEP_WORK
            lower.contains("creative") || lower.contains("design") || lower.contains("draw") || lower.contains("music") || lower.contains("brainstorm") -> TaskCategory.CREATIVE
            lower.contains("admin") || lower.contains("email") || lower.contains("bill") || lower.contains("call") || lower.contains("clean") || lower.contains("errand") -> TaskCategory.ADMIN
            lower.contains("workout") || lower.contains("gym") || lower.contains("run") || lower.contains("exercise") -> TaskCategory.EXERCISE
            lower.contains("meditate") || lower.contains("breath") || lower.contains("journal") || lower.contains("mindful") -> TaskCategory.MINDFULNESS
            else -> TaskCategory.GENERAL
        }

        // Clean up whitespace and punctuation artifacts
        val cleaned = workingTitle
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', ',')

        return ParsedTaskInput(
            cleanTitle = cleaned.ifBlank { trimmed },
            estimatedMinutes = estimatedMinutes.coerceAtLeast(5),
            isDurationExplicit = isDurationExplicit,
            energyDemand = energyDemand,
            category = category,
            isFrog = isFrog
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomaininferencetaskinferenceenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`
- **Lines of Code:** 342
- **Size:** 16,941 bytes (16.5 KB)

```kotlin
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
    val calibrationFactorApplied: Float = 1.0f
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

    private val HIGH_ENERGY_KEYWORDS = Regex("""(?i)\b(deep(\s+work)?|coding|write\s+report|architect|study|exam|complex|thesis|research|debug|refactor)\b""")
    private val LOW_ENERGY_KEYWORDS = Regex("""(?i)\b(quick\s+call|clean\s+desk|water\s+plants|tidy|file\s+expense|errand|inbox|admin)\b""")

    private val ANALYTICAL_KEYWORDS = Regex("""(?i)\b(code|coding|debug|math|analysis|algorithm|compile|refactor|architect|finance|audit)\b""")
    private val CREATIVE_KEYWORDS = Regex("""(?i)\b(design|write|draft|brainstorm|compose|sketch|draw|illustrate|ideate)\b""")
    private val ADMIN_KEYWORDS = Regex("""(?i)\b(call|meet|email|sync|discuss|errand|admin|organize|clean|invoice|schedule|order|buy|purchase|bill)\b""")
    private val PHYSICAL_KEYWORDS = Regex("""(?i)\b(gym|run|walk|exercise|workout|stretch|yoga|cardio|lift)\b""")
    private val MINDFULNESS_KEYWORDS = Regex("""(?i)\b(meditate|meditation|breathwork|breathe|journal|mindful|nsdr)\b""")

    private val HIGH_IMPACT_REGEX = Regex("""(?i)(?:^|\s)(?:!impact\b|!star\b|#impact\b|\b(?:high\s+impact|critical|strategic)\b)""")
    private val TODAY_REGEX = Regex("""(?i)\b(today|tonight)\b""")
    private val TOMORROW_REGEX = Regex("""(?i)\b(tomorrow|tmrw)\b""")
    private val DUE_DATE_REGEX = Regex("""(?i)\b(?:due|by|deadline)\s*:?\s*(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val SCHEDULE_DATE_REGEX = Regex("""(?i)\b(?:scheduled?|on|next|this)\s*:?\s*(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val CLOCK_TIME_REGEX = Regex("""(?i)\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""")
    private val RELATIVE_OFFSET_REGEX = Regex("""(?i)\bin\s+(\d+)\s*(m|min|mins|minutes?|h|hr|hrs|hours?)\b""")
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
                calibrationFactorApplied = safeCalibration
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
        val reservedTokens = setOf("high", "low", "med", "p1", "p2", "p3", "frog", "urgent", "impact", "star")
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
        val isUrgent = PRIORITY_P1_REGEX.containsMatchIn(text)
        val priority = when {
            isFrog || isUrgent || isHighImpact -> Priority.HIGH
            PRIORITY_P3_REGEX.containsMatchIn(text) -> Priority.LOW
            PRIORITY_P2_REGEX.containsMatchIn(text) -> Priority.MEDIUM
            else -> Priority.MEDIUM
        }

        // 6. Energy demand heuristic
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
        var inferredScheduledDate: Long? = null
        var inferredScheduledTimeMillis: Long? = null

        val dueMatch = DUE_DATE_REGEX.find(text)
        if (dueMatch != null) {
            val token = dueMatch.groupValues[1].lowercase(Locale.ROOT)
            inferredDeadlineDate = resolveDateTokenMillis(token, todayLocal, zoneId)
        } else if (TODAY_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = todayStartMillis
        } else if (TOMORROW_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = tomorrowStartMillis
        }

        val scheduleMatch = SCHEDULE_DATE_REGEX.find(text)
        if (scheduleMatch != null) {
            val token = scheduleMatch.groupValues[1].lowercase(Locale.ROOT)
            inferredScheduledDate = resolveDateTokenMillis(token, todayLocal, zoneId)
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
            inferredScheduledTimeMillis = LocalTime.of(hour24, rawMin).toSecondOfDay() * 1000L
            if (inferredScheduledDate == null) {
                inferredScheduledDate = todayStartMillis
            }
        } else {
            val relMatch = RELATIVE_OFFSET_REGEX.find(text)
            if (relMatch != null) {
                val amount = relMatch.groupValues[1].toLongOrNull() ?: 1L
                val unit = relMatch.groupValues[2].lowercase(Locale.ROOT)
                val offsetMins = if (unit.startsWith("h")) amount * 60L else amount
                val targetTime = LocalTime.now(zoneId).plusMinutes(offsetMins)
                inferredScheduledTimeMillis = targetTime.toSecondOfDay() * 1000L
                if (inferredScheduledDate == null) {
                    inferredScheduledDate = todayStartMillis
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
                        inferredScheduledTimeMillis = targetLocalTime.toSecondOfDay() * 1000L
                        if (inferredScheduledDate == null) {
                            inferredScheduledDate = todayStartMillis
                        }
                    }
                }
            }
        }

        val quadrant = when {
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
            isHighImpact = isHighImpact,
            rawDurationMinutes = rawDurationMinutes,
            calibrationFactorApplied = safeCalibration
        )
    }

    private fun resolveDateTokenMillis(
        token: String,
        todayLocal: LocalDate,
        zoneId: ZoneId
    ): Long {
        val targetDate = when (token) {
            "today", "tonight" -> todayLocal
            "tomorrow", "tmrw" -> todayLocal.plusDays(1)
            "monday", "mon" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            "tuesday", "tue" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            "wednesday", "wed" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
            "thursday", "thu" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            "friday", "fri" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            "saturday", "sat" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            "sunday", "sun" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            else -> todayLocal
        }
        return targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicegeminiaionlineservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`
- **Lines of Code:** 867
- **Size:** 38,218 bytes (37.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max
import kotlin.math.min

data class TaskEnrichmentResult(
    val microSteps: List<MicroStepSuggestion>,
    val suggestedSlotStartHour: Int,
    val suggestedSlotStartMinute: Int,
    val suggestedDurationMinutes: Int,
    val activationTip: String,
    val recommendedEnergyDemand: EnergyDemand,
    val cognitiveFrictionWarning: String? = null
)

data class SlotRecommendation(
    val suggestedStartMillis: Long,
    val suggestedEndMillis: Long,
    val confidence: Int,
    val circadianPhase: String,
    val rationale: String,
    val alternativeStartMillis: Long? = null
)

data class EnergyAdvisory(
    val statusTitle: String,
    val actionAdvice: String,
    val suggestedCategory: String,
    val suggestedEnergyDemand: EnergyDemand,
    val powerNapRecommended: Boolean
)

data class QuickNoteSynthesis(
    val extractedTaskTitle: String,
    val suggestedCategory: String,
    val estimatedMinutes: Int,
    val tags: List<String>,
    val firstActionStep: String
)

data class WeeklyAvoidanceAuditResult(
    val avoidanceAuditJson: String,
    val newSystemDirective: String
)

interface GeminiAiOnlineService {
    suspend fun enrichTaskWithAi(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand = EnergyDemand.MODERATE,
        currentHour: Int = 10,
        isFrog: Boolean = false,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): TaskEnrichmentResult

    suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion>

    suspend fun suggestTaskSlot(
        taskTitle: String,
        estimatedMinutes: Int,
        energyDemand: EnergyDemand,
        isFrog: Boolean,
        currentEpochMillis: Long,
        candidateWindows: List<TimeSlotWindow>,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): SlotRecommendation

    suspend fun adviseEnergyControl(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        hoursAwake: Float,
        continuousFocusMinutes: Int,
        hasSleepDebt: Boolean,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): EnergyAdvisory

    suspend fun conductWeeklyAuditAndDirective(
        weeklyTelemetryJson: String,
        recentlyDeletedTasksJson: String,
        chronicallyPostponedTasksJson: String,
        apiKey: String? = null,
        useAi: Boolean = false
    ): WeeklyAvoidanceAuditResult

    suspend fun synthesizeQuickNote(
        rawContent: String,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): QuickNoteSynthesis

    fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion>

    suspend fun testConnection(apiKey: String): Result<String>
}

@Singleton
class DefaultGeminiAiOnlineService @Inject constructor() : GeminiAiOnlineService {

    private data class CachedEntry(val jsonResponse: String, val timestampMillis: Long)

    private val responseCache = ConcurrentHashMap<String, CachedEntry>()
    private val cacheTtlMillis = 20 * 60 * 1000L // 20 minutes

    @Volatile
    private var rateLimitedUntilMillis: Long = 0L

    companion object {
        const val MODEL_ID = "gemini-flash-latest"
        const val ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_ID:generateContent"

        val CANDIDATE_MODELS = listOf(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )
        private const val TAG = "GeminiAiOnlineService"
    }

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntilMillis
    }

    private fun checkCache(cacheKey: String): String? {
        val entry = responseCache[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.timestampMillis < cacheTtlMillis) {
            return entry.jsonResponse
        }
        responseCache.remove(cacheKey)
        return null
    }

    private fun storeCache(cacheKey: String, json: String) {
        responseCache[cacheKey] = CachedEntry(json, System.currentTimeMillis())
    }

    override suspend fun enrichTaskWithAi(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand,
        currentHour: Int,
        isFrog: Boolean,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): TaskEnrichmentResult {
        val cleanTitle = title.trim().take(70).ifBlank { "Focus Task" }
        val boundedMinutes = durationMinutes.coerceIn(5, 240)
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
        }

        val cacheKey = "enrich_${cleanTitle.lowercase()}_${boundedMinutes}_${energyDemand.name}_${currentHour}_${isFrog}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseEnrichmentResponse(cached)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are an elite ADHD Executive Function & Chronobiology Coach. Analyze and enrich the following task.
            Task: "$cleanTitle" (Duration: $boundedMinutes mins, Demand: ${energyDemand.name}, Frog: $isFrog, Current Hour: $currentHour:00)

            Requirements:
            1. Decompose into 3 to 4 actionable micro-steps. Step 1 MUST be a 2-to-4 minute zero-friction activation doorway.
            2. Suggest the optimal clock start hour (0-23) and minute (0, 15, 30, 45) respecting circadian peak/dip.
            3. Provide a 1-sentence activation tip that removes initiation anxiety.
            4. Return ONLY a valid JSON object with keys:
               "microSteps": [{"title": string, "estimatedMinutes": int}],
               "suggestedStartHour": int,
               "suggestedStartMinute": int,
               "activationTip": string,
               "recommendedEnergyDemand": "HIGH" | "MODERATE" | "LOW",
               "cognitiveFrictionWarning": string or null
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 500)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseEnrichmentResponse(responseText) ?: localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
            } else {
                localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
            }
        } catch (_: Exception) {
            localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
        }
    }

    override suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        val cleanTitle = title.trim().take(70).ifBlank { "Task" }
        val boundedMinutes = durationMinutes.coerceIn(5, 240)
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return decomposeTaskLocally(cleanTitle, boundedMinutes)
        }

        val cacheKey = "decomp_${cleanTitle.lowercase()}_${boundedMinutes}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            val parsed = parseDecompositionResponse(cached)
            if (parsed.isNotEmpty()) return parsed
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val archetype = DefaultSmartTaskDecompositionService.classifyArchetype(cleanTitle)
        val prompt = """
            $directiveBlock
            You are an executive function coach. Decompose this task into 3 to 4 concrete low-friction micro-steps.
            Task: "$cleanTitle" (Archetype: ${archetype.name}, Total: $boundedMinutes mins)

            Requirements:
            1. Step 1 MUST be a 2-4 minute frictionless activation spark.
            2. Sum of estimatedMinutes across steps roughly equals $boundedMinutes.
            3. Return ONLY a valid JSON array of objects with keys "title" (string) and "estimatedMinutes" (integer).
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 400)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                val parsed = parseDecompositionResponse(responseText)
                if (parsed.isNotEmpty()) parsed else decomposeTaskLocally(cleanTitle, boundedMinutes)
            } else {
                decomposeTaskLocally(cleanTitle, boundedMinutes)
            }
        } catch (_: Exception) {
            decomposeTaskLocally(cleanTitle, boundedMinutes)
        }
    }

    override suspend fun suggestTaskSlot(
        taskTitle: String,
        estimatedMinutes: Int,
        energyDemand: EnergyDemand,
        isFrog: Boolean,
        currentEpochMillis: Long,
        candidateWindows: List<TimeSlotWindow>,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): SlotRecommendation {
        val firstWindow = candidateWindows.firstOrNull() ?: TimeSlotWindow(
            currentEpochMillis + 15 * 60_000L,
            currentEpochMillis + (15 + estimatedMinutes) * 60_000L
        )

        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localSlotRecommendation(firstWindow, energyDemand, isFrog)
        }

        val cacheKey = "slot_${taskTitle.take(30).lowercase()}_${estimatedMinutes}_${energyDemand.name}_${isFrog}_${firstWindow.startMillis}_${firstWindow.endMillis}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseSlotResponse(cached, firstWindow)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are a chronobiologist scheduler. Recommend the best time slot for the task.
            Task: "$taskTitle", Duration: $estimatedMinutes mins, Demand: ${energyDemand.name}, Frog: $isFrog
            Candidate window start: ${firstWindow.startMillis}, end: ${firstWindow.endMillis}

            Return ONLY a valid JSON object:
            {
               "confidence": int (0-100),
               "circadianPhase": string (e.g. "Morning Peak", "Afternoon Rebound"),
               "rationale": string (1 concise sentence explaining biological alignment)
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseSlotResponse(responseText, firstWindow) ?: localSlotRecommendation(firstWindow, energyDemand, isFrog)
            } else {
                localSlotRecommendation(firstWindow, energyDemand, isFrog)
            }
        } catch (_: Exception) {
            localSlotRecommendation(firstWindow, energyDemand, isFrog)
        }
    }

    override suspend fun adviseEnergyControl(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        hoursAwake: Float,
        continuousFocusMinutes: Int,
        hasSleepDebt: Boolean,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): EnergyAdvisory {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
        }

        val cacheKey = "energy_${phase.name}_${usableEnergyPercent / 10}_${continuousFocusMinutes / 30}_${hasSleepDebt}_${(hoursAwake * 2).toInt()}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseEnergyResponse(cached)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are a circadian energy advisor. Provide real-time biological pacing guidance.
            Current State: Phase=${phase.name}, UsableEnergy=$usableEnergyPercent%, HoursAwake=${"%.1f".format(Locale.ROOT, hoursAwake)}, ContinuousFocus=${continuousFocusMinutes}m, SleepDebt=$hasSleepDebt.

            Return ONLY a valid JSON object:
            {
               "statusTitle": string (e.g. "Peak Focus Zone", "Post-Lunch Energy Dip", "Synaptic Saturation"),
               "actionAdvice": string (1 concise, actionable science-backed recommendation),
               "suggestedCategory": "DEEP_WORK" | "ADMIN" | "RECOVERY" | "REST",
               "suggestedEnergyDemand": "HIGH" | "MODERATE" | "LOW",
               "powerNapRecommended": boolean
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseEnergyResponse(responseText) ?: localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
            } else {
                localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
            }
        } catch (_: Exception) {
            localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
        }
    }

    override suspend fun conductWeeklyAuditAndDirective(
        weeklyTelemetryJson: String,
        recentlyDeletedTasksJson: String,
        chronicallyPostponedTasksJson: String,
        apiKey: String?,
        useAi: Boolean
    ): WeeklyAvoidanceAuditResult {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localWeeklyAudit(recentlyDeletedTasksJson)
        }

        val prompt = """
            You are an elite Clinical Neuropsychologist and Chronobiologist specializing in adult ADHD executive dysfunction, 
            the Borbély Two-Process Circadian Model, and cognitive friction mechanics.
            Conduct a weekly forensic audit of avoided, postponed, and deleted tasks, and generate an updated Adaptive System Directive.

            NON-NEGOTIABLE DIRECTIVES:
            1. STRICT BAN ON GENERIC PLATITUDES: Never say "Stay focused", "Manage your time", "Just do it", or "Eat that frog".
            2. GROUND IN EXECUTIVE FUNCTION: Frame all diagnoses via initiation threshold deficits, affective forecasting error, working memory saturation, or circadian dip mismatch.
            3. PRESCRIPTIONS MUST BE 120-SECOND PROTOCOLS: Specific, micro-actionable tactics.

            TELEMETRY:
            Weekly Metrics: $weeklyTelemetryJson
            Recently Deleted Tasks: $recentlyDeletedTasksJson
            Chronically Postponed Tasks: $chronicallyPostponedTasksJson

            Return ONLY a valid JSON object conforming strictly to this schema:
            {
              "avoidanceAudit": {
                "primaryAvoidancePattern": string,
                "biologicalCircadianMismatch": string,
                "neuropsychologicalTrigger": string,
                "tacticalPrescriptions": [
                  {
                    "strategyName": string,
                    "scientificRationale": string,
                    "immediateActionProtocol": string
                  }
                ]
              },
              "adaptiveSystemDirective": {
                "summary": string,
                "estimationBiasBuffer": double,
                "mandatoryFrictionRule": string,
                "circadianRoutingRule": string
              }
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 800)
            if (responseText != null) {
                val root = JSONObject(extractCleanJson(responseText))
                val auditObj = root.optJSONObject("avoidanceAudit") ?: JSONObject()
                val directiveObj = root.optJSONObject("adaptiveSystemDirective") ?: JSONObject()

                val directiveFormatted = buildString {
                    appendLine("[Adaptive System Directive — Calibrated via Weekly Audit]")
                    appendLine("- Profile: ${directiveObj.optString("summary", "Standard cognitive pacing.")}")
                    appendLine("- Estimation Bias Buffer: ${directiveObj.optDouble("estimationBiasBuffer", 1.25)}x")
                    appendLine("- Friction Rule: ${directiveObj.optString("mandatoryFrictionRule", "Decompose tasks >45m into a 2m starter step.")}")
                    appendLine("- Circadian Routing: ${directiveObj.optString("circadianRoutingRule", "Lock high cognitive work into morning peak.")}")
                }.trim()

                WeeklyAvoidanceAuditResult(
                    avoidanceAuditJson = auditObj.toString(),
                    newSystemDirective = directiveFormatted
                )
            } else {
                localWeeklyAudit(recentlyDeletedTasksJson)
            }
        } catch (_: Exception) {
            localWeeklyAudit(recentlyDeletedTasksJson)
        }
    }

    override suspend fun synthesizeQuickNote(
        rawContent: String,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): QuickNoteSynthesis {
        val trimmed = rawContent.trim()
        val firstLine = trimmed.lines().firstOrNull().orEmpty().take(50).ifBlank { "Quick Thought" }
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return QuickNoteSynthesis(
                extractedTaskTitle = firstLine,
                suggestedCategory = "GENERAL",
                estimatedMinutes = 20,
                tags = listOf("quick-note"),
                firstActionStep = "Review scratchpad notes and outline details"
            )
        }

        val prompt = """
            Extract a clean, actionable task from this raw scratchpad note:
            "$trimmed"

            Return ONLY valid JSON:
            {
               "extractedTaskTitle": string (clear, verb-driven title <= 50 chars),
               "suggestedCategory": "DEEP_WORK" | "ADMIN" | "CREATIVE" | "CHORES" | "GENERAL",
               "estimatedMinutes": int (5-120),
               "tags": [string],
               "firstActionStep": string (2-minute activation step)
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                val obj = JSONObject(extractCleanJson(responseText))
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val tagsList = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                QuickNoteSynthesis(
                    extractedTaskTitle = obj.optString("extractedTaskTitle", firstLine),
                    suggestedCategory = obj.optString("suggestedCategory", "GENERAL"),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 20).coerceIn(5, 180),
                    tags = tagsList.ifEmpty { listOf("quick-note") },
                    firstActionStep = obj.optString("firstActionStep", "Open workspace and review note details")
                )
            } else {
                QuickNoteSynthesis(firstLine, "GENERAL", 20, listOf("quick-note"), "Review scratchpad note")
            }
        } catch (_: Exception) {
            QuickNoteSynthesis(firstLine, "GENERAL", 20, listOf("quick-note"), "Review scratchpad note")
        }
    }

    private suspend fun executeGeminiCall(
        prompt: String,
        apiKey: String,
        maxTokens: Int
    ): String? = withContext(Dispatchers.IO) {
        val reqObj = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", maxTokens)
                put("responseMimeType", "application/json")
            })
        }

        val requestBytes = reqObj.toString().toByteArray(Charsets.UTF_8)

        var encountered429 = false
        for (model in CANDIDATE_MODELS) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            var connection: HttpsURLConnection? = null
            try {
                connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    doOutput = true
                }

                connection.outputStream.use { os ->
                    os.write(requestBytes)
                    os.flush()
                }

                val code = connection.responseCode
                if (code in 200..299) {
                    val resp = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    Log.d(TAG, "[$model] Success (HTTP $code)")
                    return@withContext resp
                } else if (code == 429) {
                    encountered429 = true
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.w(TAG, "[$model] Rate limited (HTTP 429): $err. Cascading to next candidate model...")
                } else if (code == 400) {
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.e(TAG, "[$model] HTTP 400 Bad Request / Invalid Key: $err")
                    return@withContext null
                } else {
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.w(TAG, "[$model] HTTP $code: $err. Trying next candidate model...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$model] Network error / timeout (${e.javaClass.simpleName}: ${e.message}). Trying next candidate...")
            } finally {
                connection?.disconnect()
            }
        }
        if (encountered429) {
            Log.w(TAG, "All candidate Gemini models exhausted with rate limits (429). Engaging 60s cooldown.")
            rateLimitedUntilMillis = System.currentTimeMillis() + 60_000L
        } else {
            Log.w(TAG, "All candidate Gemini models exhausted. Gracefully degrading to local heuristics.")
        }
        null
    }

    override suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty."))
        }

        val pingObj = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "ping")))
            ))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }
        val pingBytes = pingObj.toString().toByteArray(Charsets.UTF_8)
        var lastError = "Unable to connect."

        for (model in CANDIDATE_MODELS) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
            var conn: HttpsURLConnection? = null
            try {
                conn = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    doOutput = true
                }
                conn.outputStream.use { os ->
                    os.write(pingBytes)
                    os.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    return@withContext Result.success("Connected successfully to $model (HTTP $code)")
                } else if (code == 400) {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    return@withContext Result.failure(Exception("HTTP 400: API Key Invalid. Please check your key at aistudio.google.com/apikey"))
                } else if (code == 429) {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    lastError = "HTTP 429 on $model: Rate limited or quota exhausted: $err"
                    Log.w(TAG, "[$model] Test connection rate limited (HTTP 429). Cascading to next candidate...")
                } else {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    lastError = "HTTP $code on $model: $err"
                }
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                conn?.disconnect()
            }
        }
        Result.failure(Exception("Connection failed: $lastError"))
    }

    override fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
        return DefaultSmartTaskDecompositionService.buildArchetypeMicroSteps(title, durationMinutes)
    }

    private fun localTaskEnrichment(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand,
        currentHour: Int,
        isFrog: Boolean
    ): TaskEnrichmentResult {
        val steps = decomposeTaskLocally(title, durationMinutes)
        val suggestedHour = when {
            isFrog || energyDemand == EnergyDemand.HIGH -> 10
            energyDemand == EnergyDemand.LOW -> 16
            else -> (currentHour + 1).coerceIn(8, 20)
        }
        return TaskEnrichmentResult(
            microSteps = steps,
            suggestedSlotStartHour = suggestedHour,
            suggestedSlotStartMinute = 0,
            suggestedDurationMinutes = durationMinutes,
            activationTip = "Take 2 minutes to open the relevant file or tools before doing deep work.",
            recommendedEnergyDemand = energyDemand,
            cognitiveFrictionWarning = if (durationMinutes >= 90) "Large session: 15-minute recovery break recommended after completion." else null
        )
    }

    private fun localSlotRecommendation(
        window: TimeSlotWindow,
        energyDemand: EnergyDemand,
        isFrog: Boolean
    ): SlotRecommendation {
        return SlotRecommendation(
            suggestedStartMillis = window.startMillis,
            suggestedEndMillis = window.endMillis,
            confidence = if (isFrog) 90 else 80,
            circadianPhase = if (energyDemand == EnergyDemand.HIGH) "Morning Peak" else "Standard Window",
            rationale = "Aligned with schedule availability and baseline circadian energy curve."
        )
    }

    private fun localEnergyAdvisory(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        continuousFocusMinutes: Int
    ): EnergyAdvisory {
        return when {
            continuousFocusMinutes >= 90 -> EnergyAdvisory(
                statusTitle = "Synaptic Saturation Warning",
                actionAdvice = "You have completed 90+ minutes of continuous focus. Take a 15-minute restorative walk or eye-rest break.",
                suggestedCategory = "RECOVERY",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.PEAK -> EnergyAdvisory(
                statusTitle = "Biological Peak Window",
                actionAdvice = "Optimal neurochemical alertness ($usableEnergyPercent%). Ideal for Daily Frog or complex analytical challenges.",
                suggestedCategory = "DEEP_WORK",
                suggestedEnergyDemand = EnergyDemand.HIGH,
                powerNapRecommended = false
            )
            phase == EnergyPhase.DIP -> EnergyAdvisory(
                statusTitle = "Circadian Post-Lunch Dip",
                actionAdvice = "Usable energy dips ($usableEnergyPercent%). Switch to routine admin chores or take a 20-minute power nap.",
                suggestedCategory = "ADMIN",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = true
            )
            else -> EnergyAdvisory(
                statusTitle = "Stable Operating Energy",
                actionAdvice = "Energy is steady at $usableEnergyPercent%. Maintain focus cadences with regular hydration.",
                suggestedCategory = "GENERAL",
                suggestedEnergyDemand = EnergyDemand.MODERATE,
                powerNapRecommended = false
            )
        }
    }

    private fun localWeeklyAudit(recentlyDeletedJson: String): WeeklyAvoidanceAuditResult {
        val auditJson = JSONObject().apply {
            put("primaryAvoidancePattern", "Avoidance of unstructured tasks exceeding 60 minutes.")
            put("biologicalCircadianMismatch", "Tasks placed during afternoon energy dips experienced higher postponement frequency.")
            put("neuropsychologicalTrigger", "Initiation threshold friction and affective forecasting dread.")
            put("tacticalPrescriptions", JSONArray().apply {
                put(JSONObject().apply {
                    put("strategyName", "120-Second Gateway Doorway")
                    put("scientificRationale", "Removes outcome pressure from the basal ganglia.")
                    put("immediateActionProtocol", "Schedule only the first 2-minute starter step before committing to full duration.")
                })
            })
        }.toString()

        val directive = """
            [Adaptive System Directive — Calibrated via Local Neuro-Profile]
            - Profile: High velocity on <45m structured tasks; friction on large blocks.
            - Estimation Bias Buffer: 1.25x
            - Friction Rule: Any task >45m must be preceded by a <=3-minute activation step.
            - Circadian Routing: High cognitive tasks locked to morning peak (09:30-12:30).
        """.trimIndent()

        return WeeklyAvoidanceAuditResult(auditJson, directive)
    }

    private fun extractCleanJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("{") && clean.contains("\"candidates\"")) {
            try {
                val envelope = JSONObject(clean)
                val candidate = envelope.optJSONArray("candidates")?.optJSONObject(0)
                val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                val text = part?.optString("text")
                if (!text.isNullOrBlank()) {
                    clean = text.trim()
                }
            } catch (_: Exception) {
                val textMatch = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""").find(clean)
                if (textMatch != null) {
                    clean = textMatch.groupValues[1]
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "")
                        .replace("\\\\", "\\")
                }
            }
        }

        if (clean.startsWith("```json")) clean = clean.removePrefix("```json").trim()
        if (clean.startsWith("```")) clean = clean.removePrefix("```").trim()
        if (clean.endsWith("```")) clean = clean.removeSuffix("```").trim()

        val firstBrace = clean.indexOf('{')
        val firstBracket = clean.indexOf('[')
        val startIdx = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> 0
        }
        val lastBrace = clean.lastIndexOf('}')
        val lastBracket = clean.lastIndexOf(']')
        val endIdx = maxOf(lastBrace, lastBracket)
        if (startIdx >= 0 && endIdx > startIdx) {
            clean = clean.substring(startIdx, endIdx + 1)
        }
        return clean
    }

    private fun parseEnrichmentResponse(raw: String): TaskEnrichmentResult? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val stepsArr = root.optJSONArray("microSteps") ?: JSONArray()
            val steps = mutableListOf<MicroStepSuggestion>()
            for (i in 0 until stepsArr.length()) {
                val item = stepsArr.getJSONObject(i)
                steps.add(MicroStepSuggestion(item.optString("title", "Step"), item.optInt("estimatedMinutes", 5)))
            }
            val startHour = root.optInt("suggestedStartHour", 10).coerceIn(0, 23)
            val startMinute = root.optInt("suggestedStartMinute", 0).coerceIn(0, 59)
            val tip = root.optString("activationTip", "Start with a 2-minute setup step.")
            val demandStr = root.optString("recommendedEnergyDemand", "MODERATE")
            val demand = when (demandStr.uppercase(Locale.ROOT)) {
                "HIGH" -> EnergyDemand.HIGH
                "LOW" -> EnergyDemand.LOW
                else -> EnergyDemand.MODERATE
            }
            val warning = root.optString("cognitiveFrictionWarning").takeIf { it.isNotBlank() && it != "null" }
            TaskEnrichmentResult(
                microSteps = steps.ifEmpty { decomposeTaskLocally("Task", 30) },
                suggestedSlotStartHour = startHour,
                suggestedSlotStartMinute = startMinute,
                suggestedDurationMinutes = steps.sumOf { it.estimatedMinutes }.coerceAtLeast(15),
                activationTip = tip,
                recommendedEnergyDemand = demand,
                cognitiveFrictionWarning = warning
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDecompositionResponse(raw: String): List<MicroStepSuggestion> {
        return try {
            val clean = extractCleanJson(raw)
            val arr = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else {
                val obj = JSONObject(clean)
                obj.optJSONArray("microSteps") ?: obj.optJSONArray("steps") ?: JSONArray()
            }
            val result = mutableListOf<MicroStepSuggestion>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val title = item.optString("title", "").trim()
                val minutes = item.optInt("estimatedMinutes", 5).coerceIn(1, 180)
                if (title.isNotBlank()) result.add(MicroStepSuggestion(title, minutes))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSlotResponse(raw: String, fallbackWindow: TimeSlotWindow): SlotRecommendation? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            SlotRecommendation(
                suggestedStartMillis = fallbackWindow.startMillis,
                suggestedEndMillis = fallbackWindow.endMillis,
                confidence = root.optInt("confidence", 85).coerceIn(0, 100),
                circadianPhase = root.optString("circadianPhase", "Morning Peak"),
                rationale = root.optString("rationale", "Aligned with circadian alertness curve.")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEnergyResponse(raw: String): EnergyAdvisory? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val demandStr = root.optString("suggestedEnergyDemand", "MODERATE")
            val demand = when (demandStr.uppercase(Locale.ROOT)) {
                "HIGH" -> EnergyDemand.HIGH
                "LOW" -> EnergyDemand.LOW
                else -> EnergyDemand.MODERATE
            }
            EnergyAdvisory(
                statusTitle = root.optString("statusTitle", "Stable Energy"),
                actionAdvice = root.optString("actionAdvice", "Maintain healthy pacing."),
                suggestedCategory = root.optString("suggestedCategory", "GENERAL"),
                suggestedEnergyDemand = demand,
                powerNapRecommended = root.optBoolean("powerNapRecommended", false)
            )
        } catch (_: Exception) {
            null
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicesmarttaskdecompositionservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`
- **Lines of Code:** 221
- **Size:** 10,824 bytes (10.6 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

interface SmartTaskDecompositionService {
    suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion>

    fun decomposeTaskLocally(
        title: String,
        durationMinutes: Int
    ): List<MicroStepSuggestion>
}

enum class TaskArchetype {
    CODING_TECH,
    WRITING_RESEARCH,
    STUDY_ACADEMIC,
    CHORE_MAINTENANCE,
    ADMIN_LOGISTICS,
    GENERAL
}

@Singleton
class DefaultSmartTaskDecompositionService @Inject constructor(
    private val geminiAiOnlineService: GeminiAiOnlineService
) : SmartTaskDecompositionService {

    constructor() : this(DefaultGeminiAiOnlineService())

    override suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank()) {
            return decomposeTaskLocally(title, durationMinutes)
        }

        return geminiAiOnlineService.decomposeTask(
            title = title,
            durationMinutes = durationMinutes,
            userDirective = "",
            apiKey = trimmedKey,
            useAi = useAi
        )
    }

    override fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
        return buildArchetypeMicroSteps(title, durationMinutes)
    }

    companion object {
        fun classifyArchetype(title: String): TaskArchetype {
            val lower = title.lowercase(Locale.ROOT)
            val tokens = lower.split(Regex("[\\s,._\\-:/]+")).filter { it.isNotBlank() }.toSet()

            fun hasAny(vararg words: String): Boolean = words.any { it in tokens || lower.contains(it) }

            return when {
                hasAny("code", "fix", "bug", "refactor", "api", "test", "impl", "build", "debug", "pr", "git", "backend", "frontend", "sql", "db", "database", "endpoint", "deploy", "script", "compile", "lint", "module") ->
                    TaskArchetype.CODING_TECH
                hasAny("write", "draft", "blog", "report", "essay", "document", "doc", "article", "summary", "copy", "proposal", "review", "edit", "outline", "notes", "research") ->
                    TaskArchetype.WRITING_RESEARCH
                hasAny("read", "study", "learn", "chapter", "exam", "textbook", "lecture", "paper", "quiz", "homework", "flashcards", "course", "revision", "assignment") ->
                    TaskArchetype.STUDY_ACADEMIC
                hasAny("clean", "wash", "laundry", "organize", "buy", "grocery", "shop", "dishes", "tidy", "pack", "trash", "cook", "meal", "prepare", "declutter", "car") ->
                    TaskArchetype.CHORE_MAINTENANCE
                hasAny("email", "call", "invoice", "taxes", "schedule", "file", "bill", "bank", "form", "application", "submit", "register", "renew", "pay", "book", "flight", "appointment") ->
                    TaskArchetype.ADMIN_LOGISTICS
                else -> TaskArchetype.GENERAL
            }
        }

        fun buildArchetypeMicroSteps(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
            val clean = title.trim().take(50).ifBlank { "Task" }
            val boundedDuration = durationMinutes.coerceIn(10, 240)
            val archetype = classifyArchetype(clean)

            // Step 1 is ALWAYS a 2-to-4 minute zero-friction physical activation doorway
            val doorwayMins = min(4, max(2, boundedDuration / 12))
            val reviewMins = min(10, max(3, boundedDuration / 6))
            val coreMins = max(5, boundedDuration - doorwayMins - reviewMins)
            val part1Mins = max(5, coreMins / 2)
            val part2Mins = max(5, coreMins - part1Mins)

            return when (archetype) {
                TaskArchetype.CODING_TECH -> listOf(
                    MicroStepSuggestion("Open IDE, checkout branch & locate target file for $clean", doorwayMins),
                    MicroStepSuggestion("Write failing test or stub signature for core logic", part1Mins),
                    MicroStepSuggestion("Implement happy-path logic without premature optimization", part2Mins),
                    MicroStepSuggestion("Run local test suite & clean up compiler warnings", reviewMins)
                )
                TaskArchetype.WRITING_RESEARCH -> listOf(
                    MicroStepSuggestion("Open blank doc & type 3 messy bullet headings for $clean", doorwayMins),
                    MicroStepSuggestion("Draft rough body paragraphs without editing or backspacing", part1Mins),
                    MicroStepSuggestion("Fill in supporting data points, citations, or examples", part2Mins),
                    MicroStepSuggestion("Read aloud once & polish formatting", reviewMins)
                )
                TaskArchetype.STUDY_ACADEMIC -> listOf(
                    MicroStepSuggestion("Open study material to exact page & clear phone from desk", doorwayMins),
                    MicroStepSuggestion("Skim section headers & summarize key concepts in own words", part1Mins),
                    MicroStepSuggestion("Work through active recall questions or practice problems", part2Mins),
                    MicroStepSuggestion("Flag 2 weak areas for tomorrow's spaced repetition", reviewMins)
                )
                TaskArchetype.CHORE_MAINTENANCE -> listOf(
                    MicroStepSuggestion("Put on headphones/timer & clear one small surface for $clean", doorwayMins),
                    MicroStepSuggestion("Complete the highest-visibility 50% of $clean", part1Mins + part2Mins),
                    MicroStepSuggestion("Put away tools & do a 2-minute final reset", reviewMins)
                )
                TaskArchetype.ADMIN_LOGISTICS -> listOf(
                    MicroStepSuggestion("Open browser tab, portal, or document needed for $clean", doorwayMins),
                    MicroStepSuggestion("Fill in required fields or draft message body", part1Mins + part2Mins),
                    MicroStepSuggestion("Verify attachments/numbers & hit submit/send", reviewMins)
                )
                TaskArchetype.GENERAL -> listOf(
                    MicroStepSuggestion("Open workspace & do 2 minutes of setup for $clean", doorwayMins),
                    MicroStepSuggestion("Complete the first concrete milestone of $clean", part1Mins),
                    MicroStepSuggestion("Push through the main execution block of $clean", part2Mins),
                    MicroStepSuggestion("Review progress & polish final result", reviewMins)
                )
            }
        }

        fun parseGeminiResponse(rawJson: String): List<MicroStepSuggestion> {
            try {
                val root = JSONObject(rawJson)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "").trim()
                        if (rawText.isNotBlank()) {
                            val parsed = parseJsonArrayString(rawText)
                            if (parsed.isNotEmpty()) return parsed
                        }
                    }
                }
            } catch (_: Exception) {}

            return parseViaRegexFallback(rawJson)
        }

        private fun parseJsonArrayString(rawText: String): List<MicroStepSuggestion> {
            return try {
                var jsonStr = rawText.trim()
                if (jsonStr.startsWith("```json")) {
                    jsonStr = jsonStr.removePrefix("```json").trim()
                } else if (jsonStr.startsWith("```")) {
                    jsonStr = jsonStr.removePrefix("```").trim()
                }
                if (jsonStr.endsWith("```")) {
                    jsonStr = jsonStr.removeSuffix("```").trim()
                }

                val arr = JSONArray(jsonStr)
                val result = mutableListOf<MicroStepSuggestion>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val title = item.optString("title", "").trim()
                    val minutes = item.optInt("estimatedMinutes", 5).coerceIn(1, 180)
                    if (title.isNotBlank()) {
                        result.add(MicroStepSuggestion(title, minutes))
                    }
                }
                result
            } catch (_: Exception) {
                parseItemsFromText(rawText)
            }
        }

        private fun parseViaRegexFallback(raw: String): List<MicroStepSuggestion> {
            val textRegex = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
            val textMatch = textRegex.find(raw)?.groupValues?.get(1) ?: return emptyList()
            val unescaped = textMatch.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "")
            return parseItemsFromText(unescaped)
        }

        private fun parseItemsFromText(text: String): List<MicroStepSuggestion> {
            var sanitized = text.trim()
            if (sanitized.startsWith("```json")) {
                sanitized = sanitized.removePrefix("```json").trim()
            } else if (sanitized.startsWith("```")) {
                sanitized = sanitized.removePrefix("```").trim()
            }
            if (sanitized.endsWith("```")) {
                sanitized = sanitized.removeSuffix("```").trim()
            }

            val objectPattern = Regex("""\{[^{}]*\}""")
            val result = mutableListOf<MicroStepSuggestion>()
            for (match in objectPattern.findAll(sanitized)) {
                val block = match.value
                val titleMatch = Regex(""""title"\s*:\s*"([^"]+)"""").find(block)
                val minMatch = Regex(""""estimatedMinutes"\s*:\s*(\d+)""").find(block)
                if (titleMatch != null) {
                    val title = titleMatch.groupValues[1].trim()
                    val minutes = minMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 180) ?: 5
                    if (title.isNotBlank()) {
                        result.add(MicroStepSuggestion(title, minutes))
                    }
                }
            }
            return result
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicebehavioralsupportservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`
- **Lines of Code:** 160
- **Size:** 6,085 bytes (5.9 KB)

```kotlin
package com.neuroflow.app.domain.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

data class MicroStepSuggestion(
    val title: String,
    val estimatedMinutes: Int
)

data class DistractionFeedback(
    val shouldPromptBreakdown: Boolean,
    val shouldPromptDecompression: Boolean,
    val supportiveMessage: String
)

interface BehavioralSupportService {
    fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask>
    fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion>
    suspend fun generateMicroStepsWithAi(
        taskTitle: String,
        estimatedMinutes: Int,
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion> = generateMicroSteps(taskTitle, estimatedMinutes)
    fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback

    companion object {
        val DEFAULT_DISTRACTING_PACKAGES: Set<String> = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.x.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.reddit.frontpage",
            "com.linkedin.android",
            "com.pinterest",
            "com.tumblr",
            "com.vkontakte.android",
            "com.bereal.app",
            "com.threads.android",
            "com.quora.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "com.discord",
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "com.hbo.hbonow",
            "com.hulu.plus",
            "com.twitch.android.app",
            "tv.twitch.android.app",
            "com.vanced.android.youtube",
            "com.king.candycrushsaga",
            "com.king.candycrushsodasaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.supercell.brawlstars",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.activision.callofduty.shooter",
            "com.garena.free.fire",
            "com.epicgames.fortnite",
            "com.ea.game.pvzfh_row",
            "com.halfbrick.fruitninjafree",
            "com.imangi.templerun2",
            "com.outfit7.talkingtomgoldrun2",
            "com.miniclip.eightballpool",
            "com.chess",
            "com.buzzfeed.android"
        )

        fun hasUsagePermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun openUsagePermissionSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Singleton
class DefaultBehavioralSupportService @Inject constructor(
    private val decompositionService: SmartTaskDecompositionService
) : BehavioralSupportService {

    constructor() : this(DefaultSmartTaskDecompositionService())

    override fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask> {
        // Unlock unpinned tasks to allow a completely clean replan
        return tasks.map { task ->
            if (task.isLocked) task else task.copy(scheduledStartMillis = null)
        }
    }

    override fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion> {
        return decompositionService.decomposeTaskLocally(taskTitle, estimatedMinutes)
    }

    override suspend fun generateMicroStepsWithAi(
        taskTitle: String,
        estimatedMinutes: Int,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        return decompositionService.decomposeTask(taskTitle, estimatedMinutes, apiKey, useAi)
    }

    override fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback {
        // Non-punitive: High distraction on tasks signals cognitive friction or excessive task size,
        // never boosting task priority.
        val isHighInterruption = interruptionCount >= 3 || (interruptionCount >= 2 && durationMinutes <= 25)
        val isLongSlog = durationMinutes >= 60 && interruptionCount >= 2

        return when {
            isLongSlog -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = true,
                supportiveMessage = "This task seems heavy right now. Would you like to break it into 5-minute micro-steps or take a 3-minute decompression breather?"
            )
            isHighInterruption -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = false,
                supportiveMessage = "Frequent interruptions detected. Breaking this task down into bite-sized steps can help rebuild momentum."
            )
            else -> DistractionFeedback(
                shouldPromptBreakdown = false,
                shouldPromptDecompression = false,
                supportiveMessage = "Great focus! You are maintaining healthy cognitive flow."
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginewoopenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`
- **Lines of Code:** 22
- **Size:** 1,055 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.WoopEntity

object WoopEngine {
    /** Returns true if the WOOP prompt should be shown for this task (never shown before). */
    fun shouldShowPrompt(woopData: WoopEntity?, woopPromptShown: Boolean): Boolean =
        woopData == null && !woopPromptShown

    /** Generates a default if-then plan string from the obstacle text. */
    fun generateIfThenPlan(obstacle: String): String =
        "If $obstacle, then I will "

    /** Returns the affective forecast insight message if ≥5 tasks were rated worse than expected. */
    fun dreadedTaskInsight(completedTasks: List<TaskEntity>): String? {
        val negativeCount = completedTasks.count { it.affectiveForecastError != null && it.affectiveForecastError < 0 }
        return if (negativeCount >= 5) {
            "You've rated $negativeCount tasks as worse than expected — but you finished them anyway. That's the real win."
        } else null
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainengineautonomynudgeenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`
- **Lines of Code:** 49
- **Size:** 1,872 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.domain.engine

import android.content.Context
import com.neuroflow.app.data.local.entity.TaskEntity

object AutonomyNudgeEngine {
    private const val TAG_PREFIX = "autonomy_nudge_"
    private const val GLOBAL_TAG = "autonomy_nudge_all"
    private const val DELAY_HOURS = 2L

    fun uniqueWorkName(taskId: String): String = "$TAG_PREFIX$taskId"
    fun workTag(taskId: String): String = "$TAG_PREFIX$taskId"
    fun globalTag(): String = GLOBAL_TAG

    fun scheduleNudge(context: Context, task: TaskEntity) {
        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
        }

        val data = androidx.work.Data.Builder()
            .putString("taskId", task.id)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<com.neuroflow.app.worker.AutonomyNudgeWorker>()
            .setInitialDelay(DELAY_HOURS, java.util.concurrent.TimeUnit.HOURS)
            .setInputData(data)
            .addTag(workTag(task.id))
            .addTag(globalTag())
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(task.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelNudge(context: Context, taskId: String) {
        androidx.work.WorkManager.getInstance(context)
            .cancelAllWorkByTag(workTag(taskId))
        androidx.work.WorkManager.getInstance(context)
            .cancelUniqueWork(uniqueWorkName(taskId))
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginefreshstartenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`
- **Lines of Code:** 97
- **Size:** 4,301 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.domain.engine

object FreshStartEngine {
    private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

    fun isFreshStart(
        nowMillis: Long,
        lastOpenMillis: Long,
        dailyStreak: Int,
        lastActiveDate: Long,
        lastFreshStartShownWeek: Int,
        lastFreshStartShownYear: Int
    ): Boolean {
        // Guard: already shown this ISO week
        if (isoWeekNumber(nowMillis) == lastFreshStartShownWeek &&
            isoYear(nowMillis) == lastFreshStartShownYear) return false

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)

        // Monday (first day of ISO week)
        val isMonday = dayOfWeek == java.util.Calendar.MONDAY
        // First day of month
        val isFirstOfMonth = dayOfMonth == 1
        // Day after streak break (had a streak, now it's 0)
        val isAfterStreakBreak = dailyStreak == 0 && lastActiveDate > 0L
        // 3+ day absence
        val isLongAbsence = lastOpenMillis > 0L && (nowMillis - lastOpenMillis) >= THREE_DAYS_MS

        return isMonday || isFirstOfMonth || isAfterStreakBreak || isLongAbsence
    }

    fun isoWeekNumber(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    }

    fun isoYear(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
    }

    data class EmergencyResetPlan(
        val tasksToReschedule: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val tasksPushedToBacklog: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val streakFreezeApplied: Boolean,
        val summaryMessage: String
    )

    /**
     * Instant Frictionless One-Tap Recovery Algorithm:
     * Reshuffles remaining tasks without punishing streaks or clogging the calendar.
     *
     * 1. Protects Streaks: Preserves current streak state or applies a streak-freeze safety buffer.
     * 2. Clears Calendar Overpack: Un-schedules overdue/missed tasks for today, pushing low-impact/non-urgent tasks to Backlog.
     * 3. Keeps Only High-Impact frog/urgent tasks for immediate action today.
     */
    fun buildEmergencyResetPlan(
        activeTasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        nowMillis: Long
    ): EmergencyResetPlan {
        val todayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Find active tasks scheduled today or in the past
        val scheduledOverdueOrToday = activeTasks.filter { task ->
            task.scheduledDate != null && task.scheduledDate!! <= todayStart && !task.isScheduleLocked
        }

        // Partition by urgency: High-impact (isFrog, impactScore >= 70, or urgent deadline) vs low-impact
        val (highPriority, lowPriority) = scheduledOverdueOrToday.partition { task ->
            task.isFrog || task.impactScore >= 70 ||
                (task.deadlineDate != null && (task.deadlineDate!! - nowMillis) <= 24 * 60 * 60 * 1000L)
        }

        // Keep top 2 high-priority tasks for today, push the rest to backlog (clear scheduleDate/Time)
        val keepForToday = highPriority.take(2)
        val pushToBacklog = lowPriority + highPriority.drop(2)

        return EmergencyResetPlan(
            tasksToReschedule = keepForToday,
            tasksPushedToBacklog = pushToBacklog,
            streakFreezeApplied = true,
            summaryMessage = "Schedule reset! ${keepForToday.size} essential tasks kept for today, ${pushToBacklog.size} moved to backlog. Streak protected!"
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainengineanalyticsenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`
- **Lines of Code:** 610
- **Size:** 29,645 bytes (29.0 KB)

```kotlin
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
```

---

