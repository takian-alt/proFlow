package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.StimulantDoseBout
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class WellnessNotificationCategory {
    HYDRATION,
    BREAK,
    RECOVERY,
    WIND_DOWN
}

enum class WellnessNotificationActionType {
    LOG_HYDRATION,
    LOG_REST_BREAK,
    OPEN_FLOW
}

data class ProactiveWellnessNotificationPayload(
    val category: WellnessNotificationCategory,
    val title: String,
    val shortBody: String,
    val expandedBody: String,
    val primaryActionType: WellnessNotificationActionType,
    val primaryActionLabel: String,
    val recommendedDurationMinutes: Int,
    val usableEnergyPercent: Int,
    val phaseLabel: String
)

data class ProactiveWellnessDecision(
    val shouldNotify: Boolean,
    val payload: ProactiveWellnessNotificationPayload? = null,
    val suppressReason: String? = null
)

/**
 * Pure-Kotlin context-aware decision engine for background WorkManager wellness notifications.
 *
 * Evaluates live circadian energy, sleep pressure, burnout load, hydration gap, active focus
 * sessions, waking hours, and shared anti-spam cooldowns to emit at most one actionable nudge
 * covering hydration, breaks, energy recovery, or pre-bedtime wind-down.
 */
object ProactiveWellnessNudgePlanner {

    const val MIN_NOTIFICATION_COOLDOWN_MS = 45L * 60_000L
    const val WIND_DOWN_WINDOW_MINUTES = 75
    private const val RECOVERY_ENERGY_THRESHOLD = 38
    private const val DIP_BREAK_ENERGY_THRESHOLD = 62

    fun isWithinWakingWindow(hourOfDay: Int, wakeUpHour: Int, sleepHour: Int): Boolean {
        val h = hourOfDay.coerceIn(0, 23)
        val wake = wakeUpHour.coerceIn(0, 23)
        val sleep = sleepHour.coerceIn(0, 23)
        if (wake == sleep) return true
        return if (wake < sleep) {
            h in wake until sleep
        } else {
            h >= wake || h < sleep
        }
    }

    fun minutesUntilBedtime(hourOfDay: Int, minuteOfHour: Int, sleepHour: Int): Int {
        val nowMin = hourOfDay.coerceIn(0, 23) * 60 + minuteOfHour.coerceIn(0, 59)
        val sleepMin = sleepHour.coerceIn(0, 23) * 60
        return (sleepMin - nowMin + 1440) % 1440
    }

    fun minutesSinceWake(hourOfDay: Int, minuteOfHour: Int, wakeUpHour: Int, wakeTimeMinute: Int): Int {
        val nowMin = hourOfDay.coerceIn(0, 23) * 60 + minuteOfHour.coerceIn(0, 59)
        val wakeMin = wakeUpHour.coerceIn(0, 23) * 60 + wakeTimeMinute.coerceIn(0, 59)
        return (nowMin - wakeMin + 1440) % 1440
    }

    fun formatPhaseLabel(phase: EnergyPhase): String = when (phase) {
        EnergyPhase.PEAK -> "Peak Focus"
        EnergyPhase.SURGE -> "Energy Surge"
        EnergyPhase.MORNING_RAMP -> "Morning Ramp"
        EnergyPhase.REBOUND -> "Afternoon Rebound"
        EnergyPhase.DIP -> "Circadian Dip"
        EnergyPhase.WIND_DOWN -> "Evening Wind-Down"
        EnergyPhase.TROUGH -> "Sleep Trough"
        EnergyPhase.RECOVERY -> "Active Recovery"
        EnergyPhase.RECOVERY_NEEDED -> "Recovery Needed"
        EnergyPhase.INERTIA -> "Wake Inertia"
        EnergyPhase.CRASH -> "Energy Crash"
    }

    fun evaluate(
        prefs: UserPreferences,
        snapshot: CircadianSnapshot,
        burnoutRisk: BurnoutRiskState,
        recommendations: List<EnergyBoosterRecommendation>,
        stimulants: List<StimulantDoseBout>,
        naps: List<DaytimeNapBout>,
        continuousFocusMinutes: Int,
        nowMillis: Long,
        isFocusSessionActive: Boolean,
        isHyperFocusActive: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        ignoreCooldownForPreview: Boolean = false
    ): ProactiveWellnessDecision {
        if (!prefs.wellnessNudgesEnabled && !ignoreCooldownForPreview) {
            return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "wellness_nudges_disabled")
        }
        if ((isFocusSessionActive || isHyperFocusActive) && !ignoreCooldownForPreview) {
            return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "focus_session_active")
        }

        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val minsToBed = minutesUntilBedtime(zdt.hour, zdt.minute, prefs.sleepHour)
        val isPreBedWindDownWindow = minsToBed in 15..WIND_DOWN_WINDOW_MINUTES
        val inWakingWindow = isWithinWakingWindow(zdt.hour, prefs.wakeUpHour, prefs.sleepHour)

        if (!inWakingWindow && !isPreBedWindDownWindow && !ignoreCooldownForPreview) {
            return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "quiet_hours")
        }

        if (!ignoreCooldownForPreview) {
            if (nowMillis < prefs.snoozedEnergyBoosterUntilMillis) {
                return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "snoozed")
            }
            val lastInteraction = maxOf(
                prefs.lastWellnessNudgeDismissedAtMillis,
                prefs.lastEnergyBoosterNudgeAtMillis,
                prefs.lastRecoveryAlertSentAtMillis
            )
            if (lastInteraction > 0L && (nowMillis - lastInteraction) in 0 until MIN_NOTIFICATION_COOLDOWN_MS) {
                return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "cooldown_active")
            }
        }

        val usableEnergy = snapshot.usableEnergy.coerceIn(5, 100)
        val sleepPressurePct = (snapshot.homeostaticPressureS * 100.0).roundToInt().coerceIn(0, 100)
        val phaseLabel = formatPhaseLabel(snapshot.phase)
        val topRec = recommendations.firstOrNull()

        // 1. Pre-bedtime Sleep Wind-Down recovery nudge
        if (
            (isPreBedWindDownWindow || snapshot.phase == EnergyPhase.WIND_DOWN) &&
            (topRec?.type == EnergyBoosterType.SLEEP_WIND_DOWN || usableEnergy <= 55)
        ) {
            val shortBody = "Bedtime is ~${minsToBed}m away ($usableEnergy% energy). Start winding down to protect tomorrow's peak."
            val expandedBody = "Your circadian phase is $phaseLabel ($usableEnergy% usable energy, $sleepPressurePct% sleep pressure). Dimming screens and stepping away for 15 minutes now deepens overnight recovery and raises tomorrow's morning peak."
            return ProactiveWellnessDecision(
                shouldNotify = true,
                payload = ProactiveWellnessNotificationPayload(
                    category = WellnessNotificationCategory.WIND_DOWN,
                    title = "🌙 Time to wind down ($usableEnergy% energy)",
                    shortBody = shortBody,
                    expandedBody = expandedBody,
                    primaryActionType = WellnessNotificationActionType.LOG_REST_BREAK,
                    primaryActionLabel = "Log Wind-Down ✓",
                    recommendedDurationMinutes = 15,
                    usableEnergyPercent = usableEnergy,
                    phaseLabel = phaseLabel
                )
            )
        }

        // 2. Energy Recovery nudge (crash, recovery-needed, low battery, or high burnout strain)
        val restCooldownRemaining = EnergyBoosterEngine.remainingCooldownMinutes(
            type = EnergyBoosterType.REST_BREAK,
            prefs = prefs,
            stimulants = stimulants,
            naps = naps,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val needsRecovery = snapshot.phase == EnergyPhase.CRASH ||
            snapshot.phase == EnergyPhase.RECOVERY_NEEDED ||
            usableEnergy <= RECOVERY_ENERGY_THRESHOLD ||
            burnoutRisk.riskLevel == BurnoutRiskLevel.HIGH

        if (needsRecovery && (restCooldownRemaining == 0 || ignoreCooldownForPreview)) {
            val recRestMins = burnoutRisk.recommendedRestMinutes.coerceIn(10, 20)
            val napCandidate = recommendations.firstOrNull { it.type == EnergyBoosterType.POWER_NAP }
            val (title, shortBody, expandedBody, actionLabel, duration) = if (napCandidate != null && minsToBed >= 360) {
                Tuple5(
                    "💤 Energy recovery needed ($usableEnergy%)",
                    "A 20m power nap or ${recRestMins}m NSDR rest can clear sleep pressure (${sleepPressurePct}%).",
                    "Energy dipped to $usableEnergy% ($phaseLabel) with $sleepPressurePct% sleep pressure. ${napCandidate.reason}. Tap below to log a quick rest or open Flow to see your recovery curve.",
                    "Log ${recRestMins}m Rest ✓",
                    recRestMins
                )
            } else {
                val loadClause = if (burnoutRisk.unbrokenLoadMinutes >= 45) {
                    "after ${burnoutRisk.unbrokenLoadMinutes}m of unbroken load"
                } else {
                    "during your $phaseLabel phase"
                }
                Tuple5(
                    "🔋 Recharge your battery ($usableEnergy%)",
                    "Step away for ${recRestMins}m $loadClause to restore focus without burning out.",
                    "Your usable energy is at $usableEnergy% ($phaseLabel). Taking a ${recRestMins}-minute eyes-off-screen rest or quiet reset now discharges cognitive fatigue and lifts your upcoming curve.",
                    "Log ${recRestMins}m Rest ✓",
                    recRestMins
                )
            }
            return ProactiveWellnessDecision(
                shouldNotify = true,
                payload = ProactiveWellnessNotificationPayload(
                    category = WellnessNotificationCategory.RECOVERY,
                    title = title,
                    shortBody = shortBody,
                    expandedBody = expandedBody,
                    primaryActionType = WellnessNotificationActionType.LOG_REST_BREAK,
                    primaryActionLabel = actionLabel,
                    recommendedDurationMinutes = duration,
                    usableEnergyPercent = usableEnergy,
                    phaseLabel = phaseLabel
                )
            )
        }

        // 3. Proactive Break nudge (sustained focus stretch, approaching burnout, or circadian dip)
        val breakThreshold = prefs.focusBreakThresholdMinutes.coerceIn(25, 120)
        val needsBreak = continuousFocusMinutes >= breakThreshold ||
            burnoutRisk.riskLevel == BurnoutRiskLevel.APPROACHING ||
            (snapshot.phase == EnergyPhase.DIP && usableEnergy < DIP_BREAK_ENERGY_THRESHOLD)

        if (needsBreak && (restCooldownRemaining == 0 || ignoreCooldownForPreview)) {
            val walkCandidate = recommendations.firstOrNull { it.type == EnergyBoosterType.SUNLIGHT_WALK }
            val breakMinutes = burnoutRisk.recommendedRestMinutes.coerceIn(10, 15)
            val (title, shortBody, expandedBody) = when {
                continuousFocusMinutes >= breakThreshold -> Triple(
                    "🌿 Time for a ${breakMinutes}m focus break",
                    "You've logged ${continuousFocusMinutes}m straight (energy $usableEnergy%). Stretch, hydrate & rest your eyes.",
                    "Sustained focus (${continuousFocusMinutes}m without a reset) steadily drains executive sharpness. A ${breakMinutes}-minute break right now keeps your energy ($usableEnergy%) steady for your next sprint."
                )
                burnoutRisk.riskLevel == BurnoutRiskLevel.APPROACHING -> Triple(
                    "🌿 Short breather advised ($usableEnergy% energy)",
                    "${burnoutRisk.reasonSummary}. A ${breakMinutes}m pause keeps momentum sustainable.",
                    "${burnoutRisk.reasonSummary}. Stepping away for $breakMinutes minutes clears synaptic fatigue before burnout builds."
                )
                else -> Triple(
                    "☀️ Beat the afternoon dip ($usableEnergy%)",
                    walkCandidate?.let { "Try a ${it.durationMinutes}m sunlight walk or ${breakMinutes}m break for ${it.boostBadgeLabel}." }
                        ?: "Your circadian dip is here — a ${breakMinutes}m walk or rest speeds your rebound.",
                    "You're in your $phaseLabel window ($usableEnergy% energy). A brief ${breakMinutes}-minute walk, stretch, or screen-free break helps your curve rebound faster."
                )
            }
            return ProactiveWellnessDecision(
                shouldNotify = true,
                payload = ProactiveWellnessNotificationPayload(
                    category = WellnessNotificationCategory.BREAK,
                    title = title,
                    shortBody = shortBody,
                    expandedBody = expandedBody,
                    primaryActionType = WellnessNotificationActionType.LOG_REST_BREAK,
                    primaryActionLabel = "Take ${breakMinutes}m Break ✓",
                    recommendedDurationMinutes = breakMinutes,
                    usableEnergyPercent = usableEnergy,
                    phaseLabel = phaseLabel
                )
            )
        }

        // 4. Context-aware Hydration nudge
        val hydrationCooldownRemaining = EnergyBoosterEngine.remainingCooldownMinutes(
            type = EnergyBoosterType.HYDRATION,
            prefs = prefs,
            stimulants = stimulants,
            naps = naps,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val hydrationIntervalMinutes = prefs.hydrationIntervalMinutes.coerceIn(30, 180)
        val hydrationIntervalMs = hydrationIntervalMinutes * 60_000L
        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        val lastHydrationAt = maxOf(
            prefs.lastHydrationLoggedAtMillis,
            physLogs.filter { it.type == PhysiologicalEventType.HYDRATION }
                .maxOfOrNull { it.timestampMillis } ?: 0L
        )
        val minsAwake = minutesSinceWake(zdt.hour, zdt.minute, prefs.wakeUpHour, prefs.wakeTimeMinute)
        val hydrationOverdue = if (lastHydrationAt > 0L) {
            (nowMillis - lastHydrationAt) >= hydrationIntervalMs
        } else {
            continuousFocusMinutes >= 15 || minsAwake >= hydrationIntervalMinutes
        }

        if (hydrationOverdue && (hydrationCooldownRemaining == 0 || ignoreCooldownForPreview)) {
            val minsSinceWater = if (lastHydrationAt > 0L) {
                ((nowMillis - lastHydrationAt) / 60_000L).toInt().coerceAtLeast(hydrationIntervalMinutes)
            } else {
                hydrationIntervalMinutes
            }
            val (title, shortBody, expandedBody) = when {
                usableEnergy >= 70 -> Triple(
                    "💧 Protect your $usableEnergy% peak — hydrate",
                    "It's been ~${minsSinceWater}m since water. A quick glass keeps your $phaseLabel sharp.",
                    "You're running at $usableEnergy% usable energy ($phaseLabel). Drinking water every ${hydrationIntervalMinutes}m preserves working memory and delays afternoon fatigue."
                )
                usableEnergy >= 45 -> Triple(
                    "💧 Quick energy lift ($usableEnergy%) — drink water",
                    "Mild dehydration accelerates mid-day dips. Grab a glass of water for a clean focus lift.",
                    "It's been ~${minsSinceWater}m since your last logged water (energy $usableEnergy%, $phaseLabel). Hydrating now gives a gentle +5% alertness boost and updates your live energy curve."
                )
                else -> Triple(
                    "💧 Hydration check ($usableEnergy% energy)",
                    "Low battery + dehydration hits hard. Grab water now to steady your focus.",
                    "Your energy is at $usableEnergy% ($phaseLabel) and it's been ~${minsSinceWater}m since water. A quick glass of water helps stabilize alertness right away."
                )
            }
            return ProactiveWellnessDecision(
                shouldNotify = true,
                payload = ProactiveWellnessNotificationPayload(
                    category = WellnessNotificationCategory.HYDRATION,
                    title = title,
                    shortBody = shortBody,
                    expandedBody = expandedBody,
                    primaryActionType = WellnessNotificationActionType.LOG_HYDRATION,
                    primaryActionLabel = "Drank Water ✓",
                    recommendedDurationMinutes = 5,
                    usableEnergyPercent = usableEnergy,
                    phaseLabel = phaseLabel
                )
            )
        }

        // 5. Explicit Settings Preview / Test Fallback when energy & hydration are all healthy
        if (ignoreCooldownForPreview) {
            return ProactiveWellnessDecision(
                shouldNotify = true,
                payload = ProactiveWellnessNotificationPayload(
                    category = WellnessNotificationCategory.HYDRATION,
                    title = "💧 Live Wellness Check ($usableEnergy% energy)",
                    shortBody = "Phase: $phaseLabel · Background nudges are active. Tap to log water or open Flow.",
                    expandedBody = "Your live energy is $usableEnergy% ($phaseLabel, $sleepPressurePct% sleep pressure). Proactive WorkManager nudges will alert you for hydration every ${hydrationIntervalMinutes}m, breaks after ${breakThreshold}m of focus, and recovery when energy drops.",
                    primaryActionType = WellnessNotificationActionType.LOG_HYDRATION,
                    primaryActionLabel = "Drank Water ✓",
                    recommendedDurationMinutes = 5,
                    usableEnergyPercent = usableEnergy,
                    phaseLabel = phaseLabel
                )
            )
        }

        return ProactiveWellnessDecision(shouldNotify = false, suppressReason = "energy_and_hydration_healthy")
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}
