package com.neuroflow.app.domain.service

import android.util.Log
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
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

data class DailyBlueprintCoachingResult(
    val headline: String,
    val strategyBody: String,
    val peakRecommendation: String
)

data class SlotRemedyResult(
    val recommendedStartMillis: Long,
    val reasoning: String,
    val directivePatch: String? = null
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
        recentRejectionsJson: String = "",
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

    suspend fun generateDailyBlueprintCoaching(
        tasks: List<TaskEntity>,
        plannedMinutes: Int,
        capacityMinutes: Int,
        peakStartHour: Int = 9,
        peakEndHour: Int = 12,
        apiKey: String? = null,
        userDirective: String = ""
    ): DailyBlueprintCoachingResult

    suspend fun remedyRejectedSlot(
        task: TaskEntity,
        rejectedStartHour: Int,
        reasonTag: String,
        availableWindows: List<TimeSlotWindow>,
        apiKey: String? = null,
        userDirective: String = ""
    ): SlotRemedyResult

    companion object {
        operator fun invoke(): GeminiAiOnlineService = DefaultGeminiAiOnlineService()
    }
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
        recentRejectionsJson: String,
        apiKey: String?,
        useAi: Boolean
    ): WeeklyAvoidanceAuditResult {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localWeeklyAudit(recentlyDeletedTasksJson, recentRejectionsJson)
        }

        val cacheKey = "weekly_audit_${weeklyTelemetryJson.hashCode()}_${recentlyDeletedTasksJson.hashCode()}_${chronicallyPostponedTasksJson.hashCode()}_${recentRejectionsJson.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            try {
                val root = JSONObject(extractCleanJson(cached))
                val auditObj = root.optJSONObject("avoidanceAudit") ?: JSONObject()
                val directiveObj = root.optJSONObject("adaptiveSystemDirective") ?: JSONObject()

                val directiveFormatted = buildString {
                    appendLine("[Adaptive System Directive — Calibrated via Weekly Audit]")
                    appendLine("- Profile: ${directiveObj.optString("summary", "Standard cognitive pacing.")}")
                    appendLine("- Estimation Bias Buffer: ${directiveObj.optDouble("estimationBiasBuffer", 1.25)}x")
                    appendLine("- Friction Rule: ${directiveObj.optString("mandatoryFrictionRule", "Decompose tasks >45m into a 2m starter step.")}")
                    appendLine("- Circadian Routing: ${directiveObj.optString("circadianRoutingRule", "Lock high cognitive work into morning peak.")}")
                }.trim()

                return WeeklyAvoidanceAuditResult(
                    avoidanceAuditJson = auditObj.toString(),
                    newSystemDirective = directiveFormatted
                )
            } catch (_: Exception) {}
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
            Scheduling Slot Rejections (User Feedback): ${if (recentRejectionsJson.isNotBlank()) recentRejectionsJson else "None recorded"}

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
                storeCache(cacheKey, responseText)
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

        val cacheKey = "quicknote_${trimmed.lowercase().hashCode()}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            try {
                val obj = JSONObject(extractCleanJson(cached))
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val tagsList = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                return QuickNoteSynthesis(
                    extractedTaskTitle = obj.optString("extractedTaskTitle", firstLine),
                    suggestedCategory = obj.optString("suggestedCategory", "GENERAL"),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 20).coerceIn(5, 180),
                    tags = tagsList.ifEmpty { listOf("quick-note") },
                    firstActionStep = obj.optString("firstActionStep", "Open workspace and review note details")
                )
            } catch (_: Exception) {}
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            ${directiveBlock}Extract a clean, actionable task from this raw scratchpad note:
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
                storeCache(cacheKey, responseText)
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

    override suspend fun generateDailyBlueprintCoaching(
        tasks: List<TaskEntity>,
        plannedMinutes: Int,
        capacityMinutes: Int,
        peakStartHour: Int,
        peakEndHour: Int,
        apiKey: String?,
        userDirective: String
    ): DailyBlueprintCoachingResult {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (trimmedKey.isBlank() || isRateLimited()) {
            return localBlueprintCoaching(tasks, plannedMinutes, capacityMinutes, peakStartHour, peakEndHour)
        }

        val cacheKey = "blueprint_${tasks.map { it.id }.sorted().hashCode()}_${plannedMinutes}_${capacityMinutes}_${peakStartHour}_${peakEndHour}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseDailyBlueprintCoachingResponse(cached)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val tasksSummary = tasks.take(15).joinToString("\n") {
            val dur = if (it.plannedBlockMinutes > 0) it.plannedBlockMinutes else it.estimatedDurationMinutes
            "- ${it.title} (${dur}m, demand=${it.energyLevel.name}, frog=${it.isFrog})"
        }
        val prompt = """
            $directiveBlock
            You are an elite ADHD Executive Function & Chronobiology Strategist.
            Analyze the proposed Daily Blueprint for today and synthesize a high-impact, neuro-affirming coaching summary.

            CONTEXT:
            - Target Deep Work Capacity: $capacityMinutes mins
            - Planned Workload: $plannedMinutes mins (${if (capacityMinutes > 0) (plannedMinutes * 100 / capacityMinutes) else 0}% capacity)
            - Peak Circadian Alertness Window: $peakStartHour:00 - $peakEndHour:00
            - Planned Tasks:
            $tasksSummary

            GUIDELINES:
            1. STRICT BAN ON PLATITUDES: Never say "Just do it", "Stay focused", or "Eat the frog".
            2. Ground recommendations in initiation thresholds, cognitive pacing, and circadian alertness.
            3. Recommend how to sequence tasks through the peak window vs afternoon dips.

            Return ONLY a valid JSON object:
            {
               "headline": string (punchy 5-10 word strategy headline),
               "strategyBody": string (2-3 concise sentences on pacing, friction removal, and order of attack),
               "peakRecommendation": string (clear instruction for the $peakStartHour:00-$peakEndHour:00 peak)
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 400)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseDailyBlueprintCoachingResponse(responseText) ?: localBlueprintCoaching(tasks, plannedMinutes, capacityMinutes, peakStartHour, peakEndHour)
            } else {
                localBlueprintCoaching(tasks, plannedMinutes, capacityMinutes, peakStartHour, peakEndHour)
            }
        } catch (_: Exception) {
            localBlueprintCoaching(tasks, plannedMinutes, capacityMinutes, peakStartHour, peakEndHour)
        }
    }

    override suspend fun remedyRejectedSlot(
        task: TaskEntity,
        rejectedStartHour: Int,
        reasonTag: String,
        availableWindows: List<TimeSlotWindow>,
        apiKey: String?,
        userDirective: String
    ): SlotRemedyResult {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (trimmedKey.isBlank() || isRateLimited()) {
            return localSlotRemedy(task, rejectedStartHour, reasonTag, availableWindows)
        }

        val taskDur = if (task.plannedBlockMinutes > 0) task.plannedBlockMinutes else task.estimatedDurationMinutes
        val cacheKey = "remedy_${task.id}_${taskDur}_${rejectedStartHour}_${reasonTag}_${availableWindows.map { "${it.startMillis}-${it.endMillis}" }.hashCode()}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseSlotRemedyResponse(cached, availableWindows)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val windowsSummary = availableWindows.joinToString("; ") { "start: ${it.startMillis}, end: ${it.endMillis}" }
        val prompt = """
            $directiveBlock
            You are an expert ADHD Chronobiology & Executive Function Coach.
            A user rejected a scheduled time slot for a task. Propose an optimal replacement slot from the available windows and explain why.

            TASK DETAILS:
            - Title: "${task.title}"
            - Duration: $taskDur minutes
            - Energy Level: ${task.energyLevel.name}
            - Rejected Start Hour: $rejectedStartHour:00
            - Rejection Reason Tag: $reasonTag
            - Available Free Windows: $windowsSummary

            REMEDY INSTRUCTIONS:
            1. Select the single best replacement window matching the task's cognitive and energy profile.
            2. If reason was TOO_LOW_ENERGY, pick a high-alertness peak window.
            3. If reason was TOO_EARLY, shift to later in the day.
            4. If reason was NEEDS_LONGER_BLOCK, select the longest contiguous gap.
            5. If reason was ROUTINE_CONFLICT, steer clear of the conflict hour.
            6. Provide a concise, science-grounded reasoning sentence.
            7. Provide an optional 1-sentence adaptive directive patch to prevent future misplacement.

            Return ONLY a valid JSON object:
            {
               "recommendedStartMillis": long (must be within one of the available windows),
               "reasoning": string,
               "directivePatch": string or null
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 350)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseSlotRemedyResponse(responseText, availableWindows) ?: localSlotRemedy(task, rejectedStartHour, reasonTag, availableWindows)
            } else {
                localSlotRemedy(task, rejectedStartHour, reasonTag, availableWindows)
            }
        } catch (_: Exception) {
            localSlotRemedy(task, rejectedStartHour, reasonTag, availableWindows)
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
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            var connection: HttpsURLConnection? = null
            try {
                connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("x-goog-api-key", apiKey)
                    connectTimeout = 6_000
                    readTimeout = 8_000
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
            continuousFocusMinutes >= 90 || phase == EnergyPhase.RECOVERY_NEEDED -> EnergyAdvisory(
                statusTitle = "Synaptic Saturation Warning",
                actionAdvice = "You have completed 90+ minutes of continuous focus or hit cognitive fatigue. Take a 15-minute restorative walk or eye-rest break.",
                suggestedCategory = "RECOVERY",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.RECOVERY -> EnergyAdvisory(
                statusTitle = "Active Restoration in Progress",
                actionAdvice = "Reservoir is actively recharging. Disconnect from cognitive demands and breathe deeply.",
                suggestedCategory = "MINDFULNESS",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.SURGE -> EnergyAdvisory(
                statusTitle = "Hyperfocus Surge Active",
                actionAdvice = "Peak stimulant or flow surge ($usableEnergyPercent%). Unrivaled cognitive alertness: crush high-leverage friction tasks.",
                suggestedCategory = "DEEP_WORK",
                suggestedEnergyDemand = EnergyDemand.HIGH,
                powerNapRecommended = false
            )
            phase == EnergyPhase.PEAK -> EnergyAdvisory(
                statusTitle = "Biological Peak Window",
                actionAdvice = "Optimal neurochemical alertness ($usableEnergyPercent%). Ideal for Daily Frog or complex analytical challenges.",
                suggestedCategory = "DEEP_WORK",
                suggestedEnergyDemand = EnergyDemand.HIGH,
                powerNapRecommended = false
            )
            phase == EnergyPhase.REBOUND -> EnergyAdvisory(
                statusTitle = "Alertness Rebound Wave",
                actionAdvice = "Second wind alertness ($usableEnergyPercent%). Great for creative ideation, problem solving, or collaboration.",
                suggestedCategory = "CREATIVE",
                suggestedEnergyDemand = EnergyDemand.MODERATE,
                powerNapRecommended = false
            )
            phase == EnergyPhase.DIP -> EnergyAdvisory(
                statusTitle = "Circadian Post-Lunch Dip",
                actionAdvice = "Usable energy dips ($usableEnergyPercent%). Switch to routine admin chores or take a 20-minute power nap.",
                suggestedCategory = "ADMIN",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = true
            )
            phase == EnergyPhase.INERTIA -> EnergyAdvisory(
                statusTitle = "Sleep Inertia Clearance",
                actionAdvice = "Waking grogginess active ($usableEnergyPercent%). Hydrate and get morning light before starting deep work.",
                suggestedCategory = "ADMIN",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.CRASH -> EnergyAdvisory(
                statusTitle = "Acute Energy Crash Detected",
                actionAdvice = "Metabolic drop or stimulant clearance ($usableEnergyPercent%). Take a 20-minute power nap or protein fuel.",
                suggestedCategory = "MINDFULNESS",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = true
            )
            phase == EnergyPhase.WIND_DOWN -> EnergyAdvisory(
                statusTitle = "Evening Wind-Down",
                actionAdvice = "Approaching sleep cycle ($usableEnergyPercent%). Wrap up open loops, reflect, and prepare for restful sleep.",
                suggestedCategory = "ADMIN",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.TROUGH -> EnergyAdvisory(
                statusTitle = "Circadian Nadir / Sleep Window",
                actionAdvice = "Biological core temperature and alertness nadir ($usableEnergyPercent%). Cease cognitive demands and sleep.",
                suggestedCategory = "MINDFULNESS",
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

    private fun localWeeklyAudit(
        recentlyDeletedJson: String,
        recentRejectionsJson: String = ""
    ): WeeklyAvoidanceAuditResult {
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

        val routingRule = if (recentRejectionsJson.isNotBlank() && recentRejectionsJson != "None") {
            "High cognitive tasks locked to peak windows; avoid user-disliked slots ($recentRejectionsJson)."
        } else {
            "High cognitive tasks locked to morning peak (09:30-12:30)."
        }

        val directive = """
            [Adaptive System Directive — Calibrated via Local Neuro-Profile]
            - Profile: High velocity on <45m structured tasks; friction on large blocks.
            - Estimation Bias Buffer: 1.25x
            - Friction Rule: Any task >45m must be preceded by a <=3-minute activation step.
            - Circadian Routing: $routingRule
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

    fun localBlueprintCoaching(
        tasks: List<TaskEntity>,
        plannedMinutes: Int,
        capacityMinutes: Int,
        peakStartHour: Int = 9,
        peakEndHour: Int = 12
    ): DailyBlueprintCoachingResult {
        val utilRatio = if (capacityMinutes > 0) plannedMinutes.toDouble() / capacityMinutes else 0.0
        val highEnergyCount = tasks.count { it.energyLevel == EnergyLevel.HIGH || it.isFrog }
        val frogTask = tasks.firstOrNull { it.isFrog }

        val headline = when {
            utilRatio > 1.05 -> "High Cognitive Load Alert: Strategic Pacing Required"
            utilRatio in 0.7..1.05 -> "Optimal Energy Synchronization: Prime Focus Protocol Active"
            plannedMinutes > 0 -> "Light Cognitive Load: Ideal for Incremental Progress"
            else -> "Clean Slate: Ready for Daily Alignment"
        }

        val strategyBody = buildString {
            append("Planned deep focus: ${plannedMinutes}m across ${tasks.size} tasks against ${capacityMinutes}m target (${(utilRatio * 100).toInt()}% capacity). ")
            append("Protect your biological peak window ($peakStartHour:00–$peakEndHour:00) for high-friction demands. ")
            if (frogTask != null) {
                append("Tackle '${frogTask.title.take(25)}' first during morning peak alertness to prevent working memory drain.")
            } else if (highEnergyCount > 0) {
                append("Cluster your $highEnergyCount demanding tasks into morning peak before afternoon cognitive dips.")
            } else {
                append("Maintain steady cognitive pacing with 10-minute restorative breaks between intervals.")
            }
        }.trim()

        val peakRec = if (frogTask != null) {
            "Anchor '${frogTask.title.take(30)}' into $peakStartHour:00–$peakEndHour:00 peak focus window"
        } else {
            "Reserve $peakStartHour:00–$peakEndHour:00 for highest-friction analytical and deep work"
        }

        return DailyBlueprintCoachingResult(
            headline = headline,
            strategyBody = strategyBody,
            peakRecommendation = peakRec
        )
    }

    fun localSlotRemedy(
        task: TaskEntity,
        rejectedStartHour: Int,
        reasonTag: String,
        availableWindows: List<TimeSlotWindow>
    ): SlotRemedyResult {
        if (availableWindows.isEmpty()) {
            return SlotRemedyResult(
                recommendedStartMillis = task.scheduledTime ?: System.currentTimeMillis(),
                reasoning = "No alternate schedule gaps available; preserving baseline.",
                directivePatch = null
            )
        }

        val taskDuration = (if (task.plannedBlockMinutes > 0) task.plannedBlockMinutes else task.estimatedDurationMinutes).coerceIn(5, 240)
        val taskDurationMs = taskDuration * 60_000L
        val fitting = availableWindows.filter { (it.endMillis - it.startMillis) >= taskDurationMs }
            .ifEmpty { availableWindows }

        fun extractHour(ms: Long): Int {
            return if (ms < 1_000_000_000_000L) {
                ((ms / 3600_000L) % 24).toInt()
            } else {
                try {
                    java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).hour
                } catch (_: Exception) {
                    ((ms / 3600_000L) % 24).toInt()
                }
            }
        }

        val selectedWindow: TimeSlotWindow
        val reasoning: String
        val directivePatch: String?

        when (reasonTag.uppercase(Locale.ROOT)) {
            "TOO_LOW_ENERGY" -> {
                val peakWindow = fitting.firstOrNull { extractHour(it.startMillis) in 9..12 }
                val fallbackWindow = fitting.firstOrNull { extractHour(it.startMillis) != rejectedStartHour }
                    ?: fitting.first()
                selectedWindow = peakWindow ?: fallbackWindow
                val selHour = extractHour(selectedWindow.startMillis)
                reasoning = "Re-routed to biological peak energy window ($selHour:00) with maximal dopaminergic focus and alertness."
                directivePatch = "Prioritize peak circadian hours (09:00–12:00) for demanding tasks like '${task.title.take(30)}'."
            }
            "TOO_EARLY" -> {
                val laterWindow = fitting.firstOrNull { extractHour(it.startMillis) > rejectedStartHour }
                    ?: fitting.maxByOrNull { extractHour(it.startMillis) }
                    ?: fitting.first()
                selectedWindow = laterWindow
                val selHour = extractHour(selectedWindow.startMillis)
                reasoning = "Shifted later to $selHour:00 to clear morning sleep inertia and reduce initiation resistance."
                directivePatch = "Avoid scheduling high-friction tasks before ${(rejectedStartHour + 1).coerceAtMost(12)}:00."
            }
            "NEEDS_LONGER_BLOCK" -> {
                selectedWindow = fitting.maxByOrNull { it.endMillis - it.startMillis } ?: fitting.first()
                val blockMinutes = ((selectedWindow.endMillis - selectedWindow.startMillis) / 60_000L).toInt()
                reasoning = "Allocated expanded contiguous block (${blockMinutes}m) for deep uninterrupted flow."
                directivePatch = "Allocate uninterrupted focus blocks >= 60m for '${task.title.take(30)}'."
            }
            "ROUTINE_CONFLICT" -> {
                val altWindow = fitting.filter { extractHour(it.startMillis) != rejectedStartHour }
                    .maxByOrNull { kotlin.math.abs(extractHour(it.startMillis) - rejectedStartHour) }
                    ?: fitting.first()
                selectedWindow = altWindow
                val selHour = extractHour(selectedWindow.startMillis)
                reasoning = "Moved to $selHour:00 to eliminate conflict with daily personal routine."
                directivePatch = "Protect $rejectedStartHour:00 as a routine conflict buffer."
            }
            else -> {
                selectedWindow = fitting.firstOrNull { extractHour(it.startMillis) != rejectedStartHour }
                    ?: fitting.first()
                val selHour = extractHour(selectedWindow.startMillis)
                reasoning = "Re-scheduled to $selHour:00 to match task cognitive demands and availability."
                directivePatch = null
            }
        }

        return SlotRemedyResult(
            recommendedStartMillis = selectedWindow.startMillis,
            reasoning = reasoning,
            directivePatch = directivePatch
        )
    }

    private fun parseDailyBlueprintCoachingResponse(raw: String): DailyBlueprintCoachingResult? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val headline = root.optString("headline").trim()
            val strategy = root.optString("strategyBody").trim()
            val peak = root.optString("peakRecommendation").trim()
            if (headline.isBlank() || strategy.isBlank()) null
            else DailyBlueprintCoachingResult(
                headline = headline,
                strategyBody = strategy,
                peakRecommendation = peak.ifBlank { "Protect peak alertness window for demanding tasks." }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSlotRemedyResponse(raw: String, availableWindows: List<TimeSlotWindow>): SlotRemedyResult? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val recommendedMillis = root.optLong("recommendedStartMillis", -1L)
            val reasoning = root.optString("reasoning").trim()
            val directivePatch = root.optString("directivePatch").takeIf { it.isNotBlank() && it != "null" }

            val validStart = if (availableWindows.any { recommendedMillis in it.startMillis..it.endMillis }) {
                recommendedMillis
            } else {
                availableWindows.firstOrNull()?.startMillis ?: return null
            }

            if (reasoning.isBlank()) null
            else SlotRemedyResult(
                recommendedStartMillis = validStart,
                reasoning = reasoning,
                directivePatch = directivePatch
            )
        } catch (_: Exception) {
            null
        }
    }
}
