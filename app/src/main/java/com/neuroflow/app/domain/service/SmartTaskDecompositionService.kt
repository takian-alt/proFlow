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
        useAi: Boolean = false,
        userDirective: String = ""
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
        useAi: Boolean,
        userDirective: String
    ): List<MicroStepSuggestion> {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank()) {
            return decomposeTaskLocally(title, durationMinutes)
        }

        return geminiAiOnlineService.decomposeTask(
            title = title,
            durationMinutes = durationMinutes,
            userDirective = userDirective,
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
