package com.neuroflow.app.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
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
        val clean = title.trim().take(40).ifBlank { "Task" }
        val boundedDuration = durationMinutes.coerceAtLeast(15)
        val s1 = min(10, max(5, boundedDuration / 4))
        val s3 = min(10, max(5, boundedDuration / 4))
        val s2 = max(5, boundedDuration - s1 - s3)

        return listOf(
            MicroStepSuggestion("Open workspace & gather materials for $clean", s1),
            MicroStepSuggestion("Spend 5 uninterrupted minutes on the very first sub-step", s2),
            MicroStepSuggestion("Review progress & polish final result", s3)
        )
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
