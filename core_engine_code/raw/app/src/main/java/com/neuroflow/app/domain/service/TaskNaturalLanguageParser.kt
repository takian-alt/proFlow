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
