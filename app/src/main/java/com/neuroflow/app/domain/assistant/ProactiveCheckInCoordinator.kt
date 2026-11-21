package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.PhysiologicalEventLog
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.domain.service.StimulantDoseBout
import java.time.Instant
import java.time.ZoneId

object ProactiveCheckInCoordinator {

    private const val PROMPT_SPACING_COOLDOWN_MS = 20 * 60_000L

    /**
     * Evaluates whether a proactive in-app check-in prompt is currently due.
     *
     * Rules:
     * - Never interrupts an active focus timer session.
     * - Respects master switch and per-log enabled flags.
     * - Prompts after [CheckInConfigEntry.defaultMinuteOfDay] for PENDING items,
     *   or immediately after [CheckInDailyRecord.snoozedUntilMillis] for SNOOZED items.
     * - Enforces a 20-minute spacing cooldown between consecutive non-snoozed prompts.
     */
    fun evaluateNextDueCheckIn(
        prefs: UserPreferences,
        nowMillis: Long,
        isFocusSessionActive: Boolean,
        sleepLogs: List<SleepLogEntity> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CheckInConfigEntry? {
        if (!prefs.checkInsMasterEnabled || isFocusSessionActive) return null

        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val epochDay = zdt.toLocalDate().toEpochDay()
        val nowMinuteOfDay = zdt.hour * 60 + zdt.minute
        val dayStartMillis = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

        val configs = SmartAssistantCodecs.parseCheckInConfigs(prefs.checkInConfigJson)
        val records = SmartAssistantCodecs.parseDailyCheckInState(prefs.dailyCheckInStateJson, epochDay)
        val physLogsToday = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis >= dayStartMillis }
        val stimLogsToday = parseStimulantLogs(prefs.stimulantLogsJson)
            .filter { it.timestampMillis >= dayStartMillis }
        val hasSleepLogToday = sleepLogs.any { it.endAt >= dayStartMillis - 4 * 3600_000L }

        val inCooldown = prefs.lastCheckInPromptAtMillis > 0L &&
            (nowMillis - prefs.lastCheckInPromptAtMillis) in 0 until PROMPT_SPACING_COOLDOWN_MS

        // 1. Check matured snoozes first (user explicitly asked to be re-prompted after a chosen time)
        val maturedSnooze = configs
            .filter { it.enabled }
            .firstOrNull { cfg ->
                val rec = records[cfg.type]
                rec?.status == CheckInDailyStatus.SNOOZED &&
                    rec.snoozedUntilMillis != null &&
                    nowMillis >= rec.snoozedUntilMillis
            }
        if (maturedSnooze != null) return maturedSnooze

        if (inCooldown) return null

        // 2. Check pending enabled check-ins whose configured clock time has passed today
        val dueCandidates = configs
            .filter { it.enabled && nowMinuteOfDay >= it.defaultMinuteOfDay }
            .filter { cfg ->
                val rec = records[cfg.type]
                if (rec != null && rec.status != CheckInDailyStatus.PENDING) {
                    return@filter false
                }
                // Avoid re-asking if already logged via manual quick-log earlier today
                !isAlreadyCoveredByTelemetry(
                    type = cfg.type,
                    defaultMinuteOfDay = cfg.defaultMinuteOfDay,
                    physLogsToday = physLogsToday,
                    stimLogsToday = stimLogsToday,
                    hasSleepLogToday = hasSleepLogToday,
                    zoneId = zoneId
                )
            }

        // Prioritize the most recently passed window (within the last 4 hours) so morning items
        // don't hijack an evening session when opening the app late.
        val recentDue = dueCandidates
            .filter { (nowMinuteOfDay - it.defaultMinuteOfDay) <= 240 }
            .maxByOrNull { it.defaultMinuteOfDay }

        return recentDue ?: dueCandidates.maxByOrNull { it.defaultMinuteOfDay }
    }

    fun buildTodayLogbookEntries(
        prefs: UserPreferences,
        nowMillis: Long,
        sleepLogs: List<SleepLogEntity> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyLogbookEntry> {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val epochDay = zdt.toLocalDate().toEpochDay()
        val dayStartMillis = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

        val configs = SmartAssistantCodecs.parseCheckInConfigs(prefs.checkInConfigJson)
        val records = SmartAssistantCodecs.parseDailyCheckInState(prefs.dailyCheckInStateJson, epochDay)
        val physLogsToday = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis >= dayStartMillis }
        val stimLogsToday = parseStimulantLogs(prefs.stimulantLogsJson)
            .filter { it.timestampMillis >= dayStartMillis }
        val latestSleep = sleepLogs
            .filter { it.endAt >= dayStartMillis - 4 * 3600_000L }
            .maxByOrNull { it.endAt }

        return configs.map { cfg ->
            val rec = records[cfg.type]
            if (rec != null && rec.status != CheckInDailyStatus.PENDING) {
                DailyLogbookEntry(
                    type = cfg.type,
                    status = rec.status,
                    loggedAtMillis = rec.loggedAtMillis,
                    snoozedUntilMillis = rec.snoozedUntilMillis,
                    detailLabel = rec.detailLabel,
                    defaultMinuteOfDay = cfg.defaultMinuteOfDay
                )
            } else {
                val inferredTs = findMatchingTelemetryTimestamp(
                    type = cfg.type,
                    defaultMinuteOfDay = cfg.defaultMinuteOfDay,
                    physLogsToday = physLogsToday,
                    stimLogsToday = stimLogsToday,
                    latestSleep = latestSleep,
                    zoneId = zoneId
                )
                if (inferredTs != null) {
                    DailyLogbookEntry(
                        type = cfg.type,
                        status = CheckInDailyStatus.LOGGED,
                        loggedAtMillis = inferredTs,
                        defaultMinuteOfDay = cfg.defaultMinuteOfDay
                    )
                } else {
                    DailyLogbookEntry(
                        type = cfg.type,
                        status = CheckInDailyStatus.PENDING,
                        defaultMinuteOfDay = cfg.defaultMinuteOfDay
                    )
                }
            }
        }
    }

    private fun isAlreadyCoveredByTelemetry(
        type: CheckInLogType,
        defaultMinuteOfDay: Int,
        physLogsToday: List<PhysiologicalEventLog>,
        stimLogsToday: List<StimulantDoseBout>,
        hasSleepLogToday: Boolean,
        zoneId: ZoneId
    ): Boolean {
        return when (type) {
            CheckInLogType.SLEEP_RECAP -> hasSleepLogToday
            CheckInLogType.CAFFEINE -> stimLogsToday.isNotEmpty()
            CheckInLogType.WORKOUT -> physLogsToday.any { it.type == PhysiologicalEventType.WORKOUT }
            CheckInLogType.NAP -> physLogsToday.any { it.type == PhysiologicalEventType.NAP }
            CheckInLogType.BREAKFAST, CheckInLogType.LUNCH, CheckInLogType.SNACKS, CheckInLogType.DINNER -> {
                physLogsToday.any { log ->
                    if (log.type != PhysiologicalEventType.MEAL) return@any false
                    val z = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
                    val m = z.hour * 60 + z.minute
                    kotlin.math.abs(m - defaultMinuteOfDay) <= 135
                }
            }
        }
    }

    private fun findMatchingTelemetryTimestamp(
        type: CheckInLogType,
        defaultMinuteOfDay: Int,
        physLogsToday: List<PhysiologicalEventLog>,
        stimLogsToday: List<StimulantDoseBout>,
        latestSleep: SleepLogEntity?,
        zoneId: ZoneId
    ): Long? {
        return when (type) {
            CheckInLogType.SLEEP_RECAP -> latestSleep?.endAt
            CheckInLogType.CAFFEINE -> stimLogsToday.maxByOrNull { it.timestampMillis }?.timestampMillis
            CheckInLogType.WORKOUT -> physLogsToday.filter { it.type == PhysiologicalEventType.WORKOUT }
                .maxByOrNull { it.timestampMillis }?.timestampMillis
            CheckInLogType.NAP -> physLogsToday.filter { it.type == PhysiologicalEventType.NAP }
                .maxByOrNull { it.timestampMillis }?.timestampMillis
            CheckInLogType.BREAKFAST, CheckInLogType.LUNCH, CheckInLogType.SNACKS, CheckInLogType.DINNER -> {
                physLogsToday.filter { it.type == PhysiologicalEventType.MEAL }
                    .firstOrNull { log ->
                        val z = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
                        val m = z.hour * 60 + z.minute
                        kotlin.math.abs(m - defaultMinuteOfDay) <= 135
                    }?.timestampMillis
            }
        }
    }
}
