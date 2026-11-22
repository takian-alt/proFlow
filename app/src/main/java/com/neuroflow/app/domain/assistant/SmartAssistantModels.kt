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
    val pastTimeQuestion: String,
    val primaryYesLabel: String,
    val secondaryLaterLabel: String,
    val skipLabel: String,
    val supportsIntensity: Boolean = false,
    val supportsStimulantSubtype: Boolean = false,
    val supportsDuration: Boolean = false
) {
    SLEEP_RECAP(
        displayName = "Sleep",
        emoji = "🌙",
        defaultMinuteOfDay = 8 * 60 + 30,
        promptQuestion = "Ready to log last night's sleep?",
        pastTimeQuestion = "When did you go to sleep & wake up?",
        primaryYesLabel = "Log sleep",
        secondaryLaterLabel = "Remind later",
        skipLabel = "Skip today"
    ),
    BREAKFAST(
        displayName = "Breakfast",
        emoji = "🍳",
        defaultMinuteOfDay = 9 * 60 + 30,
        promptQuestion = "Have you had breakfast yet?",
        pastTimeQuestion = "What time did you eat breakfast?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Not yet",
        skipLabel = "Skipping breakfast",
        supportsIntensity = true
    ),
    CAFFEINE(
        displayName = "Caffeine",
        emoji = "☕",
        defaultMinuteOfDay = 10 * 60 + 30,
        promptQuestion = "Had coffee, tea, or a focus aid?",
        pastTimeQuestion = "What did you have and when?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Not yet",
        skipLabel = "No caffeine today",
        supportsStimulantSubtype = true
    ),
    LUNCH(
        displayName = "Lunch",
        emoji = "🍽️",
        defaultMinuteOfDay = 13 * 60 + 30,
        promptQuestion = "Have you had lunch yet?",
        pastTimeQuestion = "What time did you eat lunch?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Not yet",
        skipLabel = "Skipping lunch",
        supportsIntensity = true
    ),
    NAP(
        displayName = "Nap",
        emoji = "😴",
        defaultMinuteOfDay = 15 * 60,
        promptQuestion = "Did you take a nap or short rest?",
        pastTimeQuestion = "When did you nap and for how long?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Planning one",
        skipLabel = "No nap today",
        supportsDuration = true
    ),
    SNACKS(
        displayName = "Snack",
        emoji = "🍎",
        defaultMinuteOfDay = 16 * 60 + 30,
        promptQuestion = "Had an afternoon snack?",
        pastTimeQuestion = "What time did you have a snack?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Not yet",
        skipLabel = "No snacks today",
        supportsIntensity = true
    ),
    WORKOUT(
        displayName = "Workout",
        emoji = "🏃",
        defaultMinuteOfDay = 18 * 60 + 30,
        promptQuestion = "Did you get a workout or walk in?",
        pastTimeQuestion = "When did you work out?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Later today",
        skipLabel = "Rest day",
        supportsIntensity = true,
        supportsDuration = true
    ),
    DINNER(
        displayName = "Dinner",
        emoji = "🥗",
        defaultMinuteOfDay = 20 * 60 + 30,
        promptQuestion = "Have you had dinner yet?",
        pastTimeQuestion = "What time did you eat dinner?",
        primaryYesLabel = "Yes, log it",
        secondaryLaterLabel = "Not yet",
        skipLabel = "Skipping dinner",
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
    val isTodayFullFallback: Boolean = false,
    val includesRecoveryBuffer: Boolean = false,
    val recoveryBufferMinutes: Int = 0,
    val burnoutSafetyBadge: String? = null
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

enum class RestBreakSource {
    MANUAL_CONFIRM,
    BREAK_TIMER
}

data class RestBreakLog(
    val id: String,
    val completedAtMillis: Long,
    val durationMinutes: Int,
    val source: RestBreakSource = RestBreakSource.MANUAL_CONFIRM
)

enum class BurnoutRiskLevel {
    CALM,
    APPROACHING,
    HIGH
}

data class BurnoutRiskState(
    val riskScore: Float = 0f,
    val riskLevel: BurnoutRiskLevel = BurnoutRiskLevel.CALM,
    val unbrokenLoadMinutes: Int = 0,
    val consecutiveHeavyTasks: Int = 0,
    val minutesSinceLastRest: Int? = null,
    val lastRestAtMillis: Long = 0L,
    val lastRestDurationMinutes: Int = 0,
    val recommendedRestMinutes: Int = 15,
    val reasonSummary: String = "",
    val usableEnergy: Int = 75
) {
    val isElevated: Boolean get() = riskLevel != BurnoutRiskLevel.CALM
}

data class RestCheckInProposal(
    val riskState: BurnoutRiskState,
    val title: String,
    val message: String,
    val recommendedRestMinutes: Int = 15,
    val followUpMinutes: Int = 25
)

data class ActiveBreakTimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startedAtMillis: Long = 0L
) {
    val isActive: Boolean get() = totalSeconds > 0 && remainingSeconds > 0
    val elapsedMinutes: Int get() = ((totalSeconds - remainingSeconds) / 60).coerceAtLeast(0)
    val plannedMinutes: Int get() = (totalSeconds / 60).coerceAtLeast(1)
}

object OvertimeAssistantPolicy {
    /**
     * Never prompt the user to enable Overtime during normal morning/daytime hours
     * simply because a task has a deadline tonight — regular daytime hours are still ahead.
     * Show the assistant only once the workday ends, after hitting the daily deep-work target,
     * or while Overtime Mode is already active.
     */
    fun shouldShowAssistantCard(
        isEveningShutdownTime: Boolean,
        dailyTargetReached: Boolean,
        isOvertimeActive: Boolean,
        eveningShutdownDismissed: Boolean
    ): Boolean {
        if (eveningShutdownDismissed) return false
        return isOvertimeActive || isEveningShutdownTime || dailyTargetReached
    }
}

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

    fun parseRestBreakLogs(json: String?): List<RestBreakLog> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<RestBreakLog>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val completedAt = obj.optLong("completedAtMillis", 0L)
                val duration = obj.optInt("durationMinutes", 0)
                if (completedAt <= 0L || duration <= 0) continue
                val id = obj.optString("id").ifBlank { "rest_$completedAt" }
                val source = runCatching {
                    RestBreakSource.valueOf(obj.optString("source", RestBreakSource.MANUAL_CONFIRM.name))
                }.getOrDefault(RestBreakSource.MANUAL_CONFIRM)
                result.add(
                    RestBreakLog(
                        id = id,
                        completedAtMillis = completedAt,
                        durationMinutes = duration.coerceIn(1, 240),
                        source = source
                    )
                )
            }
            result.sortedByDescending { it.completedAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeRestBreakLogs(logs: List<RestBreakLog>): String {
        val arr = JSONArray()
        logs.sortedByDescending { it.completedAtMillis }
            .take(30)
            .forEach { log ->
                arr.put(
                    JSONObject().apply {
                        put("id", log.id)
                        put("completedAtMillis", log.completedAtMillis)
                        put("durationMinutes", log.durationMinutes.coerceIn(1, 240))
                        put("source", log.source.name)
                    }
                )
            }
        return arr.toString()
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

    fun formatDurationCompact(durationMinutes: Int): String {
        val clamped = durationMinutes.coerceAtLeast(0)
        val hours = clamped / 60
        val mins = clamped % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Resolves a chosen bedtime (hour:minute) and wake time (hour:minute) into exact epoch millis
     * for the most recent completed sleep window relative to [nowMillis].
     * Handles cross-midnight bedtimes (e.g. 11:00 PM -> 7:00 AM), post-midnight bedtimes
     * (e.g. 1:30 AM -> 8:00 AM), and prevents future wake timestamps.
     */
    fun resolveLastNightSleepInterval(
        bedHour: Int,
        bedMinute: Int,
        wakeHour: Int,
        wakeMinute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = nowZdt.toLocalDate()
        var wakeZdt = today
            .atTime(wakeHour.coerceIn(0, 23), wakeMinute.coerceIn(0, 59))
            .atZone(zoneId)
        if (wakeZdt.toInstant().toEpochMilli() > nowMillis + 10 * 60_000L) {
            wakeZdt = wakeZdt.minusDays(1)
        }
        var bedZdt = wakeZdt.toLocalDate()
            .atTime(bedHour.coerceIn(0, 23), bedMinute.coerceIn(0, 59))
            .atZone(zoneId)
        if (!bedZdt.isBefore(wakeZdt)) {
            bedZdt = bedZdt.minusDays(1)
        }
        return bedZdt.toInstant().toEpochMilli() to wakeZdt.toInstant().toEpochMilli()
    }

    fun currentEpochDay(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()
    }

    fun isSameLocalDay(epochDay: Long, date: LocalDate): Boolean = epochDay == date.toEpochDay()
}
