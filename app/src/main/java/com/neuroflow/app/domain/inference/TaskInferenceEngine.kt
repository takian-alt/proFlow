package com.neuroflow.app.domain.inference

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import java.util.Locale
import kotlin.math.roundToInt

data class InferredTaskMetadata(
    val sanitizedTitle: String,
    val durationMinutes: Int,
    val energyDemand: EnergyLevel,
    val taskType: TaskType,
    val contextTags: List<String>,
    val isFrog: Boolean,
    val priority: Priority = Priority.MEDIUM,
    val scheduledDate: Long? = null,
    val rawDurationMinutes: Int = durationMinutes,
    val calibrationFactorApplied: Float = 1.0f
)

object TaskInferenceEngine {
    private val DURATION_REGEX = Regex(
        """(?i)\b(?:(\d+)\s*h(?:ours?)?)?\s*(?:(\d+)\s*(?:m|min|mins|minutes?))\b|\b(\d+(?:\.\d+)?)\s*h(?:ours?)?\b|\b(?:half\s+(?:an?\s+)?hour)\b|\b(?:an?\s+hour)\b"""
    )
    private val TAG_REGEX = Regex("""[@#]([a-zA-Z0-9_\-]+)""")
    private val FROG_REGEX = Regex("""(?i)(?:^|\s)(?:!frog|frog:|\b(?:eat\s+the\s+frog|high\s+impact)\b)""")

    private val EXPLICIT_HIGH_ENERGY = Regex("""(?i)(?:^|\s)(?:!high\b|#high\b|\bhigh\s+energy\b|\bdeep\s+energy\b)""")
    private val EXPLICIT_LOW_ENERGY = Regex("""(?i)(?:^|\s)(?:!low\b|#low\b|\blow\s+energy\b|\bquick\s+energy\b)""")
    private val EXPLICIT_MED_ENERGY = Regex("""(?i)(?:^|\s)(?:!med\b|#med\b|\bmedium\s+energy\b|\bmed\s+energy\b)""")

    private val PRIORITY_P1_REGEX = Regex("""(?i)(?:^|\s)(?:!p1\b|\bp1\b|!urgent\b|\burgent\b)""")
    private val PRIORITY_P2_REGEX = Regex("""(?i)(?:^|\s)(?:!p2\b|\bp2\b)""")
    private val PRIORITY_P3_REGEX = Regex("""(?i)(?:^|\s)(?:!p3\b|\bp3\b)""")

    private val HIGH_ENERGY_KEYWORDS = Regex("""(?i)\b(deep(\s+work)?|coding|write\s+report|architect|study|exam|complex|thesis|research)\b""")
    private val LOW_ENERGY_KEYWORDS = Regex("""(?i)\b(quick\s+call|clean\s+desk|water\s+plants|tidy|file\s+expense|errand)\b""")

    private val ANALYTICAL_KEYWORDS = Regex("""(?i)\b(code|coding|debug|math|analysis|algorithm|compile|refactor)\b""")
    private val CREATIVE_KEYWORDS = Regex("""(?i)\b(design|write|draft|brainstorm|compose|sketch|draw|illustrate)\b""")
    private val ADMIN_KEYWORDS = Regex("""(?i)\b(call|meet|email|sync|discuss|errand|admin|organize|clean|invoice|schedule|order|buy|purchase)\b""")
    private val PHYSICAL_KEYWORDS = Regex("""(?i)\b(gym|run|walk|exercise|workout|stretch|yoga)\b""")

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
        calibrationFactor: Float = 1.0f
    ): InferredTaskMetadata {
        val text = rawInput.trim()
        val safeCalibration = calibrationFactor.coerceIn(0.5f, 3.0f)
        if (text.isEmpty()) {
            val defaultDuration = (25 * safeCalibration).roundToInt().coerceAtLeast(5)
            return InferredTaskMetadata(
                sanitizedTitle = "",
                durationMinutes = defaultDuration,
                energyDemand = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                contextTags = emptyList(),
                isFrog = false,
                priority = Priority.MEDIUM,
                rawDurationMinutes = 25,
                calibrationFactorApplied = safeCalibration
            )
        }

        // Duration extraction
        var rawDurationMinutes = 25
        val durationMatch = DURATION_REGEX.find(text)
        if (durationMatch != null) {
            val matchedStr = durationMatch.value.lowercase(Locale.ROOT)
            val hoursPart = durationMatch.groups[1]?.value?.toIntOrNull() ?: 0
            val minsPart = durationMatch.groups[2]?.value?.toIntOrNull() ?: 0
            val decimalHours = durationMatch.groups[3]?.value?.toFloatOrNull()

            rawDurationMinutes = when {
                matchedStr.contains("half") -> 30
                matchedStr == "an hour" || matchedStr == "a hour" -> 60
                decimalHours != null -> (decimalHours * 60).roundToInt()
                hoursPart > 0 || minsPart > 0 -> hoursPart * 60 + minsPart
                else -> 25
            }
        }

        val calibratedDuration = (rawDurationMinutes * safeCalibration).roundToInt().coerceAtLeast(5)

        // Context tags extraction (@work, #finance)
        val tags = TAG_REGEX.findAll(text)
            .map { it.groupValues[1].lowercase(Locale.ROOT) }
            .filterNot { it in setOf("high", "low", "med", "p1", "p2", "p3", "frog", "urgent") }
            .distinct()
            .toList()

        // Frog priority detection
        val isFrog = FROG_REGEX.containsMatchIn(text)

        // Priority detection
        val priority = when {
            isFrog || PRIORITY_P1_REGEX.containsMatchIn(text) -> Priority.HIGH
            PRIORITY_P3_REGEX.containsMatchIn(text) -> Priority.LOW
            PRIORITY_P2_REGEX.containsMatchIn(text) -> Priority.MEDIUM
            else -> Priority.MEDIUM
        }

        // Energy demand heuristic (explicit overrides take precedence)
        val energyDemand = when {
            EXPLICIT_HIGH_ENERGY.containsMatchIn(text) || text.contains("#high", ignoreCase = true) -> EnergyLevel.HIGH
            EXPLICIT_LOW_ENERGY.containsMatchIn(text) || text.contains("#low", ignoreCase = true) -> EnergyLevel.LOW
            EXPLICIT_MED_ENERGY.containsMatchIn(text) || text.contains("#med", ignoreCase = true) -> EnergyLevel.MEDIUM
            HIGH_ENERGY_KEYWORDS.containsMatchIn(text) -> EnergyLevel.HIGH
            LOW_ENERGY_KEYWORDS.containsMatchIn(text) -> EnergyLevel.LOW
            else -> EnergyLevel.MEDIUM
        }

        // Task type heuristic
        val taskType = when {
            ANALYTICAL_KEYWORDS.containsMatchIn(text) -> TaskType.ANALYTICAL
            CREATIVE_KEYWORDS.containsMatchIn(text) -> TaskType.CREATIVE
            ADMIN_KEYWORDS.containsMatchIn(text) -> TaskType.ADMIN
            PHYSICAL_KEYWORDS.containsMatchIn(text) -> TaskType.PHYSICAL
            else -> TaskType.ANALYTICAL
        }

        // Title sanitization
        var cleanTitle = text
            .replace(FROG_REGEX, " ")
            .replace(DURATION_REGEX, " ")
            .replace(EXPLICIT_HIGH_ENERGY, " ")
            .replace(EXPLICIT_LOW_ENERGY, " ")
            .replace(EXPLICIT_MED_ENERGY, " ")
            .replace(PRIORITY_P1_REGEX, " ")
            .replace(PRIORITY_P2_REGEX, " ")
            .replace(PRIORITY_P3_REGEX, " ")
            .replace(Regex("""[@#][a-zA-Z0-9_\-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleanTitle.isEmpty()) {
            cleanTitle = text.take(30)
        }

        return InferredTaskMetadata(
            sanitizedTitle = cleanTitle,
            durationMinutes = calibratedDuration,
            energyDemand = energyDemand,
            taskType = taskType,
            contextTags = tags,
            isFrog = isFrog,
            priority = priority,
            rawDurationMinutes = rawDurationMinutes.coerceAtLeast(5),
            calibrationFactorApplied = safeCalibration
        )
    }
}
