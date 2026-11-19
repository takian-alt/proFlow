package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.service.BehavioralObservation
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.exp

/**
 * ADR-031: Local Bayesian Prior & Closed-Loop Rejection / Reinforcement Learning Store.
 *
 * Persists user 👍 / 👎 scheduling feedback by canonical task category, weekday/weekend
 * day-type, and clock hour, applying an exponential 7-day half-life decay, reason-specific
 * spatial/directional kernels (TOO_EARLY, TOO_LATE, TOO_LOW_ENERGY, NEEDS_LONGER_BLOCK,
 * ROUTINE_CONFLICT, WRONG_CONTEXT), and positive Bayesian hour reinforcement.
 */
object SchedulingFeedbackStore {
    private const val FOURTEEN_DAYS_MILLIS = 14 * 86_400_000L
    private const val SEVEN_DAYS_HALF_LIFE_MILLIS = 7 * 86_400_000.0
    private const val OPPOSITE_DAY_TYPE_TRANSFER_WEIGHT = 0.35

    fun isWeekendDay(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val dow = Instant.ofEpochMilli(epochMillis).atZone(zoneId).dayOfWeek
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
    }

    /**
     * Maps both domain TaskCategory names (HARD_WORK, ROUTINE, etc.) and service
     * TaskCategory names (DEEP_WORK, ADMIN, etc.) onto a unified alias family so
     * feedback recorded from any layer informs TaskSchedulerService accurately.
     */
    internal fun categoryAliases(rawCategory: String): List<String> =
        when (rawCategory.trim().uppercase()) {
            "DEEP_WORK", "HARD_WORK", "ANALYTICAL" -> listOf("DEEP_WORK", "HARD_WORK", "ANALYTICAL")
            "ADMIN", "ROUTINE" -> listOf("ADMIN", "ROUTINE")
            "EXERCISE", "PHYSICAL" -> listOf("EXERCISE", "PHYSICAL")
            "GENERAL", "FLEXIBLE" -> listOf("GENERAL", "FLEXIBLE")
            else -> listOf(rawCategory.trim().uppercase())
        }

    fun recordFeedback(
        currentJson: String,
        category: String,
        hour: Int,
        isNegative: Boolean,
        timestampMillis: Long = System.currentTimeMillis(),
        reasonTag: String? = null,
        isWeekend: Boolean? = null
    ): String {
        val root = try {
            JSONObject(if (currentJson.isBlank()) "{}" else currentJson)
        } catch (_: Exception) {
            JSONObject()
        }
        val cutoff = timestampMillis - FOURTEEN_DAYS_MILLIS

        // Global 14-day pruning across all categories and hours to prevent preference JSON bloat
        val prunedRoot = JSONObject()
        for (catKey in root.keys()) {
            val catObj = root.optJSONObject(catKey) ?: continue
            val cleanCatObj = JSONObject()
            for (hKey in catObj.keys()) {
                val arr = catObj.optJSONArray(hKey) ?: continue
                val cleanArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ts = item.optLong("ts", 0L)
                    if (ts >= cutoff) {
                        cleanArr.put(item)
                    }
                }
                if (cleanArr.length() > 0) {
                    cleanCatObj.put(hKey, cleanArr)
                }
            }
            if (cleanCatObj.length() > 0) {
                prunedRoot.put(catKey, cleanCatObj)
            }
        }

        val safeCategory = category.trim().uppercase()
        val catObj = prunedRoot.optJSONObject(safeCategory) ?: JSONObject()
        val hourKey = hour.coerceIn(0, 23).toString()
        val arr = catObj.optJSONArray(hourKey) ?: JSONArray()

        val newEntry = JSONObject().apply {
            put("ts", timestampMillis)
            put("neg", isNegative)
            if (!reasonTag.isNullOrBlank()) {
                put("tag", reasonTag.trim().uppercase())
            }
            if (isWeekend != null) {
                put("wknd", isWeekend)
            }
        }
        arr.put(newEntry)
        catObj.put(hourKey, arr)
        prunedRoot.put(safeCategory, catObj)
        return prunedRoot.toString()
    }

    /**
     * Calculates the non-negative rejection penalty [0.0..30.0] for a category and hour [0..23],
     * bridging domain & service category aliases, day-type (weekday vs. weekend), and reason kernels.
     */
    fun computePenalty(
        json: String,
        category: String,
        hour: Int,
        nowMillis: Long = System.currentTimeMillis(),
        targetIsWeekend: Boolean? = null
    ): Double {
        return computeComponents(json, category, hour, nowMillis, targetIsWeekend).penalty
    }

    /**
     * Computes the signed Bayesian feedback utility delta [-30.0..+12.0] for candidate slot scoring:
     * negative rejections push slots away; positive approvals actively pull matching categories
     * toward proven preferred hours.
     */
    fun computeAffinityDelta(
        json: String,
        category: String,
        hour: Int,
        nowMillis: Long = System.currentTimeMillis(),
        targetIsWeekend: Boolean? = null
    ): Double {
        val components = computeComponents(json, category, hour, nowMillis, targetIsWeekend)
        return (components.positiveAffinity - components.penalty).coerceIn(-30.0, 12.0)
    }

    /**
     * Computes a structural preference strength [0.0..1.0] for longer unfragmented blocks
     * based on recent NEEDS_LONGER_BLOCK feedback for this category family.
     */
    fun computeLongerBlockPreference(
        json: String,
        category: String,
        nowMillis: Long = System.currentTimeMillis(),
        targetIsWeekend: Boolean? = null
    ): Double {
        if (json.isBlank() || json == "{}") return 0.0
        val root = try { JSONObject(json) } catch (_: Exception) { return 0.0 }
        val aliases = categoryAliases(category).distinct().toSet()
        var weightedSignals = 0.0

        for (rawCatKey in root.keys()) {
            val catKey = rawCatKey.trim().uppercase()
            if (catKey !in aliases) continue
            val catObj = root.optJSONObject(rawCatKey) ?: continue
            for (hKey in catObj.keys()) {
                val arr = catObj.optJSONArray(hKey) ?: continue
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    if (!item.optBoolean("neg", true)) continue
                    val tag = item.optString("tag", "").trim().uppercase()
                    if (tag != "NEEDS_LONGER_BLOCK") continue
                    val ts = item.optLong("ts", 0L)
                    val dt = (nowMillis - ts).coerceAtLeast(0L)
                    if (dt > FOURTEEN_DAYS_MILLIS) continue
                    val decay = exp(-dt / SEVEN_DAYS_HALF_LIFE_MILLIS)
                    val dayTypeWeight = computeDayTypeWeight(item, targetIsWeekend)
                    weightedSignals += decay * dayTypeWeight
                }
            }
        }
        if (weightedSignals <= 0.0) return 0.0
        return (1.0 - exp(-weightedSignals * 0.85)).coerceIn(0.0, 1.0)
    }

    /**
     * Emits positive weighted observations from recent thumbs-up high-focus placements so
     * [com.neuroflow.app.domain.service.AdaptiveCircadianLearner] shifts peak circadian focus
     * based on explicit user approvals, not only task completions.
     */
    fun extractApprovedCircadianObservations(
        json: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<BehavioralObservation> {
        if (json.isBlank() || json == "{}") return emptyList()
        val root = try { JSONObject(json) } catch (_: Exception) { return emptyList() }
        val highFocusKeys = setOf("DEEP_WORK", "HARD_WORK", "ANALYTICAL", "CREATIVE")
        val observations = mutableListOf<Pair<Long, BehavioralObservation>>()
        for (cat in root.keys()) {
            if (cat.uppercase() !in highFocusKeys) continue
            val catObj = root.optJSONObject(cat) ?: continue
            for (hKey in catObj.keys()) {
                val hour = hKey.toIntOrNull()?.coerceIn(0, 23) ?: continue
                val arr = catObj.optJSONArray(hKey) ?: continue
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ts = item.optLong("ts", 0L)
                    if (nowMillis - ts > FOURTEEN_DAYS_MILLIS) continue
                    val isNeg = item.optBoolean("neg", true)
                    if (!isNeg) {
                        val decay = exp(-(nowMillis - ts).coerceAtLeast(0L) / SEVEN_DAYS_HALF_LIFE_MILLIS)
                        observations.add(
                            ts to BehavioralObservation(
                                minuteOfDay = hour * 60 + 30,
                                productivityRating = 0.9,
                                weight = (0.75 * decay).coerceIn(0.15, 0.85)
                            )
                        )
                    }
                }
            }
        }
        return observations.sortedBy { it.first }.map { it.second }
    }

    data class FeedbackComponents(
        val penalty: Double,
        val positiveAffinity: Double
    )

    private fun computeDayTypeWeight(item: JSONObject, targetIsWeekend: Boolean?): Double {
        if (targetIsWeekend == null || !item.has("wknd")) return 1.0
        val recordedIsWeekend = item.optBoolean("wknd", targetIsWeekend)
        return if (recordedIsWeekend == targetIsWeekend) 1.0 else OPPOSITE_DAY_TYPE_TRANSFER_WEIGHT
    }

    private fun computeComponents(
        json: String,
        category: String,
        targetHour: Int,
        nowMillis: Long,
        targetIsWeekend: Boolean? = null
    ): FeedbackComponents {
        if (json.isBlank() || json == "{}") return FeedbackComponents(0.0, 0.0)
        val root = try { JSONObject(json) } catch (_: Exception) { return FeedbackComponents(0.0, 0.0) }
        val safeTargetHour = targetHour.coerceIn(0, 23)
        val aliases = categoryAliases(category).distinct().toSet()
        val universalTags = setOf("ROUTINE_CONFLICT", "TOO_EARLY", "TOO_LATE")

        var netNegative = 0.0
        var netPositive = 0.0

        for (rawCatKey in root.keys()) {
            val catKey = rawCatKey.trim().uppercase()
            val isSameCategory = catKey in aliases
            val categoryTransferWeight = if (isSameCategory) 1.0 else 0.40

            val catObj = root.optJSONObject(rawCatKey) ?: continue
            for (hKey in catObj.keys()) {
                val recordedHour = hKey.toIntOrNull()?.coerceIn(0, 23) ?: continue
                val arr = catObj.optJSONArray(hKey) ?: continue
                val hourDiff = abs(safeTargetHour - recordedHour)
                if (hourDiff > 4) continue

                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ts = item.optLong("ts", 0L)
                    val dt = (nowMillis - ts).coerceAtLeast(0L)
                    if (dt > FOURTEEN_DAYS_MILLIS) continue
                    val decay = exp(-dt / SEVEN_DAYS_HALF_LIFE_MILLIS)
                    val isNeg = item.optBoolean("neg", true)
                    val tag = item.optString("tag", "").trim().uppercase()

                    if (!isSameCategory && (!isNeg || tag !in universalTags)) {
                        continue
                    }

                    val dayTypeWeight = computeDayTypeWeight(item, targetIsWeekend)

                    if (isNeg) {
                        val kernelWeight = when {
                            safeTargetHour == recordedHour -> when (tag) {
                                "ROUTINE_CONFLICT" -> 14.5
                                "TOO_LOW_ENERGY", "TOO_LATE" -> 13.5
                                "TOO_EARLY" -> 13.0
                                "WRONG_CONTEXT" -> 12.0
                                // NEEDS_LONGER_BLOCK primarily acts via structural headroom & split-resistance;
                                // keep a modest hour nudge so cramped windows at that hour are gently disfavored.
                                "NEEDS_LONGER_BLOCK" -> 5.5
                                else -> 10.0
                            }
                            tag == "TOO_EARLY" && safeTargetHour < recordedHour && (recordedHour - safeTargetHour) <= 3 ->
                                6.0 * exp(-(recordedHour - safeTargetHour) * 0.4)
                            tag == "TOO_LATE" && safeTargetHour > recordedHour && (safeTargetHour - recordedHour) <= 4 ->
                                6.5 * exp(-(safeTargetHour - recordedHour) * 0.4)
                            (tag == "TOO_LOW_ENERGY" || tag == "WRONG_CONTEXT") && hourDiff == 1 ->
                                4.5
                            tag.isBlank() && hourDiff == 1 ->
                                2.5
                            else -> 0.0
                        }
                        if (kernelWeight > 0.0) {
                            netNegative += kernelWeight * decay * categoryTransferWeight * dayTypeWeight
                        }
                    } else if (isSameCategory) {
                        val posRelief = when (hourDiff) {
                            0 -> 6.0 * decay * dayTypeWeight
                            1 -> 2.0 * decay * dayTypeWeight
                            else -> 0.0
                        }
                        if (posRelief > 0.0) {
                            netNegative -= posRelief
                            netPositive += when (hourDiff) {
                                0 -> 7.0 * decay * dayTypeWeight
                                1 -> 2.5 * decay * dayTypeWeight
                                else -> 0.0
                            }
                        }
                    }
                }
            }
        }

        val clampedPenalty = netNegative.coerceIn(0.0, 30.0)
        // Net positive affinity only emerges when rejections don't dominate this hour
        val clampedPositive = if (clampedPenalty > 2.0) 0.0 else netPositive.coerceIn(0.0, 12.0)
        return FeedbackComponents(penalty = clampedPenalty, positiveAffinity = clampedPositive)
    }

    /**
     * Synthesizes recent user rejections and approvals into a concise, deterministic summary for LLM context.
     */
    fun formatSummaryForLlm(json: String, nowMillis: Long = System.currentTimeMillis()): String {
        if (json.isBlank() || json == "{}") return "None"
        val root = try { JSONObject(json) } catch (_: Exception) { return "None" }
        data class SummaryEntry(val isDislike: Boolean, val hour: Int, val strength: Int, val text: String)
        val entries = mutableListOf<SummaryEntry>()

        for (cat in root.keys()) {
            val catObj = root.optJSONObject(cat) ?: continue
            for (hKey in catObj.keys()) {
                val hour = hKey.toIntOrNull() ?: continue
                val arr = catObj.optJSONArray(hKey) ?: continue
                var negCount = 0
                var posCount = 0
                val tags = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val ts = item.optLong("ts", 0L)
                    if ((nowMillis - ts) < FOURTEEN_DAYS_MILLIS) {
                        if (item.optBoolean("neg", true)) {
                            negCount++
                            val tag = item.optString("tag").takeIf { it.isNotBlank() }
                            if (tag != null) tags.add(tag)
                        } else {
                            posCount++
                        }
                    }
                }
                val ampm = if (hour < 12) "${if (hour == 0) 12 else hour}AM" else "${if (hour == 12) 12 else hour - 12}PM"
                if (negCount > 0) {
                    val tagSuffix = if (tags.isNotEmpty()) " (reasons: ${tags.distinct().joinToString()})" else ""
                    entries.add(SummaryEntry(true, hour, negCount, "$cat at $ampm (disliked $negCount times$tagSuffix)"))
                } else if (posCount > 0) {
                    entries.add(SummaryEntry(false, hour, posCount, "$cat at $ampm (approved $posCount times)"))
                }
            }
        }
        return if (entries.isEmpty()) {
            "None"
        } else {
            entries
                .sortedWith(compareByDescending<SummaryEntry> { it.isDislike }.thenByDescending { it.strength }.thenBy { it.hour })
                .joinToString("; ") { it.text }
        }
    }
}
