package com.neuroflow.app.domain.scheduler

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp

/**
 * ADR-031: Local Bayesian Prior & Closed-Loop Rejection Learning Store.
 *
 * Persists user 👍 / 👎 scheduling feedback by task category and clock hour,
 * applying an exponential 7-day half-life decay. Disliked slots receive an
 * immediate utility penalty to steer future auto-scheduling passes away from
 * rejected time windows, with zero network overhead.
 */
object SchedulingFeedbackStore {
    private const val FOURTEEN_DAYS_MILLIS = 14 * 86_400_000L

    fun recordFeedback(
        currentJson: String,
        category: String,
        hour: Int,
        isNegative: Boolean,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val root = try {
            JSONObject(if (currentJson.isBlank()) "{}" else currentJson)
        } catch (_: Exception) {
            JSONObject()
        }
        val safeCategory = category.uppercase()
        val catObj = root.optJSONObject(safeCategory) ?: JSONObject()
        val hourKey = hour.coerceIn(0, 23).toString()
        val arr = catObj.optJSONArray(hourKey) ?: JSONArray()

        val cutoff = timestampMillis - FOURTEEN_DAYS_MILLIS
        val cleanArr = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val ts = item.optLong("ts", 0L)
            if (ts >= cutoff) {
                cleanArr.put(item)
            }
        }
        val newEntry = JSONObject().apply {
            put("ts", timestampMillis)
            put("neg", isNegative)
        }
        cleanArr.put(newEntry)
        catObj.put(hourKey, cleanArr)
        root.put(safeCategory, catObj)
        return root.toString()
    }

    /**
     * Calculates the net rejection penalty for a given category and hour of day [0..23].
     * Recency is decayed with a 7-day half-life: weight = exp(-dt / (7 days)).
     * Negative feedback adds positive penalty (up to 30.0). Positive feedback reduces penalty.
     */
    fun computePenalty(
        json: String,
        category: String,
        hour: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {
        if (json.isBlank() || json == "{}") return 0.0
        val root = try { JSONObject(json) } catch (_: Exception) { return 0.0 }
        val catObj = root.optJSONObject(category.uppercase()) ?: return 0.0
        val hourKey = hour.coerceIn(0, 23).toString()
        val arr = catObj.optJSONArray(hourKey) ?: return 0.0

        var netScore = 0.0
        val halfLifeMillis = 7 * 86_400_000.0
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val ts = item.optLong("ts", 0L)
            val isNeg = item.optBoolean("neg", true)
            val dt = (nowMillis - ts).coerceAtLeast(0L)
            val decay = exp(-dt / halfLifeMillis)
            if (isNeg) {
                netScore += 10.0 * decay
            } else {
                netScore -= 6.0 * decay
            }
        }
        return netScore.coerceIn(0.0, 30.0)
    }

    /**
     * Synthesizes recent user rejections into a concise, token-efficient summary for LLM context.
     */
    fun formatSummaryForLlm(json: String, nowMillis: Long = System.currentTimeMillis()): String {
        if (json.isBlank() || json == "{}") return "None"
        val root = try { JSONObject(json) } catch (_: Exception) { return "None" }
        val summaries = mutableListOf<String>()
        for (cat in root.keys()) {
            val catObj = root.optJSONObject(cat) ?: continue
            for (hKey in catObj.keys()) {
                val hour = hKey.toIntOrNull() ?: continue
                val arr = catObj.optJSONArray(hKey) ?: continue
                var count = 0
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    if (item.optBoolean("neg", true)) {
                        val ts = item.optLong("ts", 0L)
                        if ((nowMillis - ts) < FOURTEEN_DAYS_MILLIS) count++
                    }
                }
                if (count > 0) {
                    val ampm = if (hour < 12) "${if (hour == 0) 12 else hour}AM" else "${if (hour == 12) 12 else hour - 12}PM"
                    summaries.add("$cat at $ampm (disliked $count times)")
                }
            }
        }
        return if (summaries.isEmpty()) "None" else summaries.joinToString("; ")
    }
}
