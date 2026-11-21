package com.neuroflow.app.domain.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class CheckInLogType(
    val displayName: String,
    val emoji: String,
    val defaultMinuteOfDay: Int,
    val promptQuestion: String,
    val skipLabel: String,
    val supportsIntensity: Boolean = false,
    val supportsStimulantSubtype: Boolean = false
) {
    SLEEP_RECAP(
        displayName = "Sleep Recap",
        emoji = "🌙",
        defaultMinuteOfDay = 8 * 60 + 30,
        promptQuestion = "How did you sleep last night?",
        skipLabel = "Skip sleep log today"
    ),
    BREAKFAST(
        displayName = "Breakfast",
        emoji = "🍳",
        defaultMinuteOfDay = 9 * 60 + 30,
        promptQuestion = "Did you have breakfast?",
        skipLabel = "No breakfast today",
        supportsIntensity = true
    ),
    CAFFEINE(
        displayName = "Caffeine / Focus Aid",
        emoji = "☕",
        defaultMinuteOfDay = 10 * 60 + 30,
        promptQuestion = "Did you have coffee, tea, or a focus aid?",
        skipLabel = "No caffeine today",
        supportsStimulantSubtype = true
    ),
    LUNCH(
        displayName = "Lunch",
        emoji = "🍽️",
        defaultMinuteOfDay = 13 * 60 + 30,
        promptQuestion = "Did you have lunch?",
        skipLabel = "No lunch today",
        supportsIntensity = true
    ),
    NAP(
        displayName = "Afternoon Nap",
        emoji = "😴",
        defaultMinuteOfDay = 15 * 60,
        promptQuestion = "Did you take a quick nap or rest?",
        skipLabel = "No nap today"
    ),
    SNACKS(
        displayName = "Snacks",
        emoji = "🍎",
        defaultMinuteOfDay = 16 * 60 + 30,
        promptQuestion = "Did you have an afternoon snack?",
        skipLabel = "No snacks today",
        supportsIntensity = true
    ),
    WORKOUT(
        displayName = "Workout",
        emoji = "🏃",
        defaultMinuteOfDay = 18 * 60 + 30,
        promptQuestion = "Did you get a workout or walk in?",
        skipLabel = "No workout today",
        supportsIntensity = true
    ),
    DINNER(
        displayName = "Dinner",
        emoji = "🥗",
        defaultMinuteOfDay = 20 * 60 + 30,
        promptQuestion = "Did you have dinner?",
        skipLabel = "No dinner today",
        supportsIntensity = true
    )
}

data class CheckInConfigEntry(
    val type: CheckInLogType,
    val enabled: Boolean = true,
    val defaultMinuteOfDay: Int = type.defaultMinuteOfDay
)

enum class CheckInDailyStatus {
    PENDING,
    LOGGED,
    SNOOZED,
    SKIPPED
}

data class CheckInDailyRecord(
    val type: CheckInLogType,
    val status: CheckInDailyStatus = CheckInDailyStatus.PENDING,
    val loggedAtMillis: Long? = null,
    val snoozedUntilMillis: Long? = null,
    val detailLabel: String = ""
)

data class DailyLogbookEntry(
    val type: CheckInLogType,
    val status: CheckInDailyStatus,
    val loggedAtMillis: Long? = null,
    val snoozedUntilMillis: Long? = null,
    val detailLabel: String = "",
    val defaultMinuteOfDay: Int = type.defaultMinuteOfDay
)

enum class WellnessNudgeType {
    HYDRATION,
    LONG_FOCUS_BREAK,
    LOW_ENERGY_REST,
    CRASH_EASY_WIN
}

data class WellnessNudgeProposal(
    val type: WellnessNudgeType,
    val title: String,
    val message: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
    val emoji: String
)

data class RecommendedSlotProposal(
    val startMillis: Long,
    val endMillis: Long,
    val dateMillis: Long,
    val timeOfDayOffsetMillis: Long,
    val matchScore: Int,
    val predictedEnergyPercent: Int,
    val reasonLabel: String,
    val isOvertimeSlot: Boolean = false,
    val energyMatchLabel: String = "$predictedEnergyPercent% energy",
    val gapContextLabel: String = "",
    val gapDurationMinutes: Int = 0,
    val isTodayFullFallback: Boolean = false
) {
    val dayStartMillis: Long get() = dateMillis
    val reason: String get() = reasonLabel
    val dayLabel: String
        get() {
            val zone = ZoneId.systemDefault()
            val slotDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            return when (slotDate) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> "${slotDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, ${slotDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${slotDate.dayOfMonth}"
            }
        }
    val shortStartTimeLabel: String
        get() = SmartAssistantCodecs.formatTimestampShort(startMillis, ZoneId.systemDefault())
    val timeRangeLabel: String
        get() {
            val zone = ZoneId.systemDefault()
            return "${SmartAssistantCodecs.formatTimestampShort(startMillis, zone)} – ${SmartAssistantCodecs.formatTimestampShort(endMillis, zone)}"
        }
}

data class GoalBreakdownMilestone(
    val title: String,
    val subtasks: List<String>,
    val estimatedMinutesPerTask: Int = 30
)

object SmartAssistantCodecs {
    fun defaultCheckInConfigs(): List<CheckInConfigEntry> =
        CheckInLogType.entries.map { type ->
            CheckInConfigEntry(
                type = type,
                enabled = true,
                defaultMinuteOfDay = type.defaultMinuteOfDay
            )
        }

    fun parseCheckInConfigs(json: String?): List<CheckInConfigEntry> {
        val defaults = defaultCheckInConfigs().associateBy { it.type }.toMutableMap()
        if (json.isNullOrBlank()) return CheckInLogType.entries.map { defaults[it]!! }
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = runCatching { CheckInLogType.valueOf(obj.optString("type")) }.getOrNull() ?: continue
                defaults[type] = CheckInConfigEntry(
                    type = type,
                    enabled = obj.optBoolean("enabled", true),
                    defaultMinuteOfDay = obj.optInt("defaultMinuteOfDay", type.defaultMinuteOfDay).coerceIn(0, 1439)
                )
            }
            CheckInLogType.entries.map { defaults[it]!! }
        } catch (_: Exception) {
            CheckInLogType.entries.map { defaults[it]!! }
        }
    }

    fun encodeCheckInConfigs(configs: List<CheckInConfigEntry>): String {
        val arr = JSONArray()
        configs.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("type", entry.type.name)
                    put("enabled", entry.enabled)
                    put("defaultMinuteOfDay", entry.defaultMinuteOfDay.coerceIn(0, 1439))
                }
            )
        }
        return arr.toString()
    }

    fun parseDailyCheckInState(json: String?, epochDay: Long): Map<CheckInLogType, CheckInDailyRecord> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(json)
            if (root.optLong("epochDay", -1L) != epochDay) return emptyMap()
            val items = root.optJSONArray("records") ?: return emptyMap()
            val map = mutableMapOf<CheckInLogType, CheckInDailyRecord>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val type = runCatching { CheckInLogType.valueOf(obj.optString("type")) }.getOrNull() ?: continue
                val status = runCatching { CheckInDailyStatus.valueOf(obj.optString("status")) }
                    .getOrDefault(CheckInDailyStatus.PENDING)
                val loggedAt = if (obj.has("loggedAtMillis") && !obj.isNull("loggedAtMillis")) obj.optLong("loggedAtMillis") else null
                val snoozedUntil = if (obj.has("snoozedUntilMillis") && !obj.isNull("snoozedUntilMillis")) obj.optLong("snoozedUntilMillis") else null
                map[type] = CheckInDailyRecord(
                    type = type,
                    status = status,
                    loggedAtMillis = loggedAt,
                    snoozedUntilMillis = snoozedUntil,
                    detailLabel = obj.optString("detailLabel", "")
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeDailyCheckInState(epochDay: Long, records: Map<CheckInLogType, CheckInDailyRecord>): String {
        val arr = JSONArray()
        records.values.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("type", rec.type.name)
                    put("status", rec.status.name)
                    if (rec.loggedAtMillis != null) put("loggedAtMillis", rec.loggedAtMillis)
                    if (rec.snoozedUntilMillis != null) put("snoozedUntilMillis", rec.snoozedUntilMillis)
                    if (rec.detailLabel.isNotBlank()) put("detailLabel", rec.detailLabel)
                }
            )
        }
        return JSONObject().apply {
            put("epochDay", epochDay)
            put("records", arr)
        }.toString()
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 1439)
        val hour24 = clamped / 60
        val minute = clamped % 60
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (hour24 < 12) "AM" else "PM"
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
    }

    fun formatTimestampShort(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(zoneId)
        return formatMinuteOfDay(zdt.hour * 60 + zdt.minute)
    }

    fun currentEpochDay(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()
    }

    fun isSameLocalDay(epochDay: Long, date: LocalDate): Boolean = epochDay == date.toEpochDay()
}
