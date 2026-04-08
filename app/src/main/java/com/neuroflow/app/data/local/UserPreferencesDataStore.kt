package com.neuroflow.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.neuroflow.app.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val wakeUpHour: Int = 7,
    val peakEnergyStart: Int = 9,
    val peakEnergyEnd: Int = 12,
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val defaultPomodoroMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val identityLabel: String = "",
    val topGoal: String = "",
    val dailyStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val longestStreak: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val weightQuadrant: Float = 1.0f,
    val weightDeadlineUrgency: Float = 1.0f,
    val weightPriorityLevel: Float = 1.0f,
    val weightDuration: Float = 1.0f,
    val weightImpact: Float = 1.0f,
    val weightFocusMode: Float = 1.0f,
    val onboardingCompleted: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val weeklyIntent: String = "",
    val weeklyIntentIsoWeek: Int = 0,
    val weeklyIntentIsoYear: Int = 0,
    val lastFreshStartShownWeek: Int = 0,
    val lastFreshStartShownYear: Int = 0,
    val lastAppOpenMillis: Long = 0L,
    // Dynamic peak energy detection
    val detectedPeakStart: Int = -1,      // -1 = not yet detected
    val detectedPeakEnd: Int = -1,
    val peakDetectionConfidence: Float = 0f,  // 0.0–1.0
    // Blended effective peak (manual lerp'd with detected, based on confidence)
    val effectivePeakStart: Int = -1,     // -1 = use peakEnergyStart
    val effectivePeakEnd: Int = -1,       // -1 = use peakEnergyEnd
    // Subliminal affirmations — stored as JSON array string
    val affirmations: List<String> = emptyList(),
    // Persistent task tag catalog shown in task creation and history filters
    val tagCatalog: List<String> = emptyList(),
    // Top 3 goals for the year (JSON array)
    val yearlyGoals: List<String> = emptyList(),
    // Top 3 goals for the current week (JSON array)
    val weeklyGoals: List<String> = emptyList(),
    // Tracks when we last showed the yearly goals refill prompt
    val lastYearlyGoalShownYear: Int = 0,
    // Tracks when we last showed the weekly goals refill prompt (ISO week + year)
    val lastWeeklyGoalShownWeek: Int = 0,
    val lastWeeklyGoalShownYear: Int = 0,
    // Focus behaviour toggles
    val woopEnabled: Boolean = true,
    val autoTrackerEnabled: Boolean = false,
    // Notification preferences
    val dailyPlanNotificationsEnabled: Boolean = true,
    val streakNotificationsEnabled: Boolean = true,
    val autonomyNudgeNotificationsEnabled: Boolean = true,
    val deadlineReminderNotificationsEnabled: Boolean = true,
    val deadlineEscalationNotificationsEnabled: Boolean = true,
    val dailyPlanNotificationHour: Int = 7,
    val streakCheckNotificationHour: Int = 21,
    val userGuidePromptShown: Boolean = false,
    // Left page quick note
    val leftPageQuickNote: String = ""
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val WAKE_UP_HOUR = intPreferencesKey("wake_up_hour")
        val PEAK_ENERGY_START = intPreferencesKey("peak_energy_start")
        val PEAK_ENERGY_END = intPreferencesKey("peak_energy_end")
        val WORK_DAY_START = intPreferencesKey("work_day_start")
        val WORK_DAY_END = intPreferencesKey("work_day_end")
        val DEFAULT_POMODORO_MINUTES = intPreferencesKey("default_pomodoro_minutes")
        val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val IDENTITY_LABEL = stringPreferencesKey("identity_label")
        val TOP_GOAL = stringPreferencesKey("top_goal")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val TOTAL_TASKS_COMPLETED = intPreferencesKey("total_tasks_completed")
        val TOTAL_FOCUS_MINUTES = intPreferencesKey("total_focus_minutes")
        val WEIGHT_QUADRANT = floatPreferencesKey("weight_quadrant")
        val WEIGHT_DEADLINE_URGENCY = floatPreferencesKey("weight_deadline_urgency")
        val WEIGHT_PRIORITY_LEVEL = floatPreferencesKey("weight_priority_level")
        val WEIGHT_DURATION = floatPreferencesKey("weight_duration")
        val WEIGHT_IMPACT = floatPreferencesKey("weight_impact")
        val WEIGHT_FOCUS_MODE = floatPreferencesKey("weight_focus_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME = stringPreferencesKey("theme")
        val WEEKLY_INTENT = stringPreferencesKey("weekly_intent")
        val WEEKLY_INTENT_ISO_WEEK = intPreferencesKey("weekly_intent_iso_week")
        val WEEKLY_INTENT_ISO_YEAR = intPreferencesKey("weekly_intent_iso_year")
        val LAST_FRESH_START_SHOWN_WEEK = intPreferencesKey("last_fresh_start_shown_week")
        val LAST_FRESH_START_SHOWN_YEAR = intPreferencesKey("last_fresh_start_shown_year")
        val LAST_APP_OPEN_MILLIS = longPreferencesKey("last_app_open_millis")
        val DETECTED_PEAK_START = intPreferencesKey("detected_peak_start")
        val DETECTED_PEAK_END = intPreferencesKey("detected_peak_end")
        val PEAK_DETECTION_CONFIDENCE = floatPreferencesKey("peak_detection_confidence")
        val EFFECTIVE_PEAK_START = intPreferencesKey("effective_peak_start")
        val EFFECTIVE_PEAK_END = intPreferencesKey("effective_peak_end")
        val AFFIRMATIONS = stringPreferencesKey("affirmations")
        val TAG_CATALOG = stringPreferencesKey("tag_catalog")
        val YEARLY_GOALS = stringPreferencesKey("yearly_goals")
        val WEEKLY_GOALS = stringPreferencesKey("weekly_goals")
        val LAST_YEARLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_yearly_goal_shown_year")
        val LAST_WEEKLY_GOAL_SHOWN_WEEK = intPreferencesKey("last_weekly_goal_shown_week")
        val LAST_WEEKLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_weekly_goal_shown_year")
        val WOOP_ENABLED = booleanPreferencesKey("woop_enabled")
        val AUTO_TRACKER_ENABLED = booleanPreferencesKey("auto_tracker_enabled")
        val DAILY_PLAN_NOTIFICATIONS_ENABLED = booleanPreferencesKey("daily_plan_notifications_enabled")
        val STREAK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("streak_notifications_enabled")
        val AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("autonomy_nudge_notifications_enabled")
        val DEADLINE_REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_reminder_notifications_enabled")
        val DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_escalation_notifications_enabled")
        val DAILY_PLAN_NOTIFICATION_HOUR = intPreferencesKey("daily_plan_notification_hour")
        val STREAK_CHECK_NOTIFICATION_HOUR = intPreferencesKey("streak_check_notification_hour")
        val USER_GUIDE_PROMPT_SHOWN = booleanPreferencesKey("user_guide_prompt_shown")
        val LEFT_PAGE_QUICK_NOTE = stringPreferencesKey("left_page_quick_note")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
            peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
            peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
            workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
            workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
            defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
            defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
            identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
            topGoal = prefs[Keys.TOP_GOAL] ?: "",
            dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
            totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
            weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
            weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
            weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
            weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
            weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
            weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
            } catch (_: Exception) { AppTheme.SYSTEM },
            weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
            weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
            weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
            lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
            lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
            lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
            detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
            detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
            peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
            effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
            effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
            affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
            tagCatalog = parseAffirmations(prefs[Keys.TAG_CATALOG]),
            yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
            weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
            lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
            lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
            lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
            woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
            autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
            dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
            streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
            autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
            deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
            deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
            dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
            streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
            userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
            leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: ""
        )
    }

    private fun parseAffirmations(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun encodeAffirmations(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = UserPreferences(
                wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
                peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
                peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
                workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
                workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
                defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
                defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
                identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
                topGoal = prefs[Keys.TOP_GOAL] ?: "",
                dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
                lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
                totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
                weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
                weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
                weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
                weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
                weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
                weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                theme = try {
                    AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
                } catch (_: Exception) { AppTheme.SYSTEM },
                weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
                weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
                weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
                lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
                lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
                lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
                detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
                detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
                peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
                effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
                effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
                affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
                tagCatalog = parseAffirmations(prefs[Keys.TAG_CATALOG]),
                yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
                weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
                lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
                lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
                lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
                woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
                autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
                dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
                streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
                autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
                deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
                deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
                dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
                streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
                userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
                leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: ""
            )
            val updated = update(current)
            prefs[Keys.WAKE_UP_HOUR] = updated.wakeUpHour
            prefs[Keys.PEAK_ENERGY_START] = updated.peakEnergyStart
            prefs[Keys.PEAK_ENERGY_END] = updated.peakEnergyEnd
            prefs[Keys.WORK_DAY_START] = updated.workDayStart
            prefs[Keys.WORK_DAY_END] = updated.workDayEnd
            prefs[Keys.DEFAULT_POMODORO_MINUTES] = updated.defaultPomodoroMinutes
            prefs[Keys.DEFAULT_BREAK_MINUTES] = updated.defaultBreakMinutes
            prefs[Keys.IDENTITY_LABEL] = updated.identityLabel
            prefs[Keys.TOP_GOAL] = updated.topGoal
            prefs[Keys.DAILY_STREAK] = updated.dailyStreak
            prefs[Keys.LAST_ACTIVE_DATE] = updated.lastActiveDate
            prefs[Keys.LONGEST_STREAK] = updated.longestStreak
            prefs[Keys.TOTAL_TASKS_COMPLETED] = updated.totalTasksCompleted
            prefs[Keys.TOTAL_FOCUS_MINUTES] = updated.totalFocusMinutes
            prefs[Keys.WEIGHT_QUADRANT] = updated.weightQuadrant
            prefs[Keys.WEIGHT_DEADLINE_URGENCY] = updated.weightDeadlineUrgency
            prefs[Keys.WEIGHT_PRIORITY_LEVEL] = updated.weightPriorityLevel
            prefs[Keys.WEIGHT_DURATION] = updated.weightDuration
            prefs[Keys.WEIGHT_IMPACT] = updated.weightImpact
            prefs[Keys.WEIGHT_FOCUS_MODE] = updated.weightFocusMode
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.WEEKLY_INTENT] = updated.weeklyIntent
            prefs[Keys.WEEKLY_INTENT_ISO_WEEK] = updated.weeklyIntentIsoWeek
            prefs[Keys.WEEKLY_INTENT_ISO_YEAR] = updated.weeklyIntentIsoYear
            prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] = updated.lastFreshStartShownWeek
            prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] = updated.lastFreshStartShownYear
            prefs[Keys.LAST_APP_OPEN_MILLIS] = updated.lastAppOpenMillis
            prefs[Keys.DETECTED_PEAK_START] = updated.detectedPeakStart
            prefs[Keys.DETECTED_PEAK_END] = updated.detectedPeakEnd
            prefs[Keys.PEAK_DETECTION_CONFIDENCE] = updated.peakDetectionConfidence
            prefs[Keys.EFFECTIVE_PEAK_START] = updated.effectivePeakStart
            prefs[Keys.EFFECTIVE_PEAK_END] = updated.effectivePeakEnd
            prefs[Keys.AFFIRMATIONS] = encodeAffirmations(updated.affirmations)
            prefs[Keys.TAG_CATALOG] = encodeAffirmations(updated.tagCatalog)
            prefs[Keys.YEARLY_GOALS] = encodeAffirmations(updated.yearlyGoals)
            prefs[Keys.WEEKLY_GOALS] = encodeAffirmations(updated.weeklyGoals)
            prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] = updated.lastYearlyGoalShownYear
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] = updated.lastWeeklyGoalShownWeek
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] = updated.lastWeeklyGoalShownYear
            prefs[Keys.WOOP_ENABLED] = updated.woopEnabled
            prefs[Keys.AUTO_TRACKER_ENABLED] = updated.autoTrackerEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] = updated.dailyPlanNotificationsEnabled
            prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] = updated.streakNotificationsEnabled
            prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] = updated.autonomyNudgeNotificationsEnabled
            prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] = updated.deadlineReminderNotificationsEnabled
            prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] = updated.deadlineEscalationNotificationsEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] = updated.dailyPlanNotificationHour.coerceIn(0, 23)
            prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] = updated.streakCheckNotificationHour.coerceIn(0, 23)
            prefs[Keys.USER_GUIDE_PROMPT_SHOWN] = updated.userGuidePromptShown
            prefs[Keys.LEFT_PAGE_QUICK_NOTE] = updated.leftPageQuickNote
        }
    }

    suspend fun mergeTagCatalog(tags: Collection<String>) {
        val cleaned = tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }
        if (cleaned.isEmpty()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = mergeTags(prefs.tagCatalog, cleaned))
        }
    }

    suspend fun removeTagFromCatalog(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = prefs.tagCatalog.filterNot { it.equals(cleaned, ignoreCase = true) })
        }
    }

    private fun mergeTags(existing: List<String>, incoming: Collection<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String) {
            val key = tag.lowercase()
            if (seen.add(key)) {
                merged += tag
            }
        }

        existing.forEach(::addTag)
        incoming.forEach(::addTag)
        return merged.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
}
