package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.PhysiologicalEventLog
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.domain.service.computeLatestSafeStimulantCutoffMinute
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

enum class EnergyBoosterType(
    val emoji: String,
    val title: String,
    val defaultDurationMinutes: Int,
    val supportsBreakTimer: Boolean
) {
    HYDRATION("💧", "Hydrate", 5, false),
    BALANCED_SNACK("🥜", "Balanced Snack", 10, false),
    MEAL_REFUEL("🥗", "Balanced Meal", 25, false),
    SUNLIGHT_WALK("☀️", "Sunlight Walk", 12, false),
    REST_BREAK("🌿", "Rest / NSDR", 10, true),
    POWER_NAP("💤", "Power Nap", 20, true),
    SHORT_WORKOUT("🏃", "Movement Burst", 15, false),
    SMART_CAFFEINE("☕", "Smart Caffeine", 5, false),
    SLEEP_WIND_DOWN("🌙", "Sleep Wind-Down", 20, false)
}

data class EnergyBoosterRecommendation(
    val id: String,
    val type: EnergyBoosterType,
    val title: String,
    val emoji: String,
    val durationMinutes: Int,
    val predictedBoostPoints: Int,
    val boostBadgeLabel: String,
    val reason: String,
    val priorityScore: Double,
    val supportsTimer: Boolean = type.supportsBreakTimer
)

data class EnergyGraphBoosterMarker(
    val id: String,
    val timestampMillis: Long,
    val emoji: String,
    val label: String,
    val boostSummary: String
)

data class EnergyBoosterNudgeProposal(
    val recommendation: EnergyBoosterRecommendation,
    val headline: String,
    val body: String,
    val currentEnergyPercent: Int
)

data class EnergyBoostDeltaBanner(
    val boosterEmoji: String,
    val boosterTitle: String,
    val previousEnergyPercent: Int,
    val newEnergyPercent: Int,
    val deltaPoints: Int,
    val createdAtMillis: Long
)

/**
 * Pure-Kotlin domain engine for NeuroFlow's Dynamic Energy Booster & Live Recovery Curve system:
 * 1. Maps quick physiological logs + rest breaks into unified [CognitiveBlockWindow]s.
 * 2. Generates on-curve [EnergyGraphBoosterMarker]s for completed boosters.
 * 3. Contextually recommends & ranks real-life energy boosters across sleep, nutrition, hydration,
 *    sunlight/walks, rest breaks, movement bursts, and circadian-safe caffeine timing.
 * 4. Evaluates timely, non-intrusive assistant nudges when energy dips or bedtime approaches.
 */
object EnergyBoosterEngine {

    const val BOOSTER_NUDGE_COOLDOWN_MILLIS = 45L * 60_000L

    fun mapPhysiologicalLogToBlock(
        log: PhysiologicalEventLog,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CognitiveBlockWindow? {
        val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
        val minOfDay = logZdt.hour * 60 + logZdt.minute
        val duration = log.durationMinutes.coerceIn(5, 240)
        return when (log.type) {
            PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.MEAL,
                intensity = log.intensity,
                label = "Quick Logged Meal #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.EXERCISE,
                intensity = log.intensity,
                label = "Quick Logged Workout #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.SNACK -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.SNACK,
                intensity = log.intensity,
                label = "Balanced Snack #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.HYDRATION -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.HYDRATION,
                intensity = log.intensity,
                label = "Hydration #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.SUNLIGHT_WALK -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.SUNLIGHT_WALK,
                intensity = log.intensity,
                label = "Sunlight Walk #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.REST_BREAK -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.REST_BREAK,
                intensity = log.intensity,
                label = "Rest Break #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.WIND_DOWN -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.WIND_DOWN,
                intensity = log.intensity,
                label = "Sleep Wind-Down #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            PhysiologicalEventType.NAP -> null
        }
    }

    /**
     * Unifies quick physiological logs AND rest break logs into [CognitiveBlockWindow]s for
     * Borbély + physiological impulse evaluation. Deduplicates rest breaks that occur within
     * 2 minutes of a quick-logged REST_BREAK to prevent double counting.
     */
    fun buildBoosterCognitiveBlocks(
        prefs: UserPreferences,
        dayStartMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CognitiveBlockWindow> {
        val windowStart = dayStartMillis - 8 * 3_600_000L
        val windowEnd = dayStartMillis + 26 * 3_600_000L

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in windowStart..windowEnd }
        val physBlocks = physLogs.mapNotNull { mapPhysiologicalLogToBlock(it, zoneId) }

        val existingRestTimestamps = physLogs
            .filter { it.type == PhysiologicalEventType.REST_BREAK }
            .map { it.timestampMillis }

        val restBreakBlocks = SmartAssistantCodecs.parseRestBreakLogs(prefs.restBreakLogsJson)
            .filter { it.completedAtMillis in windowStart..windowEnd }
            .filter { log ->
                existingRestTimestamps.none { abs(it - log.completedAtMillis) <= 2 * 60_000L }
            }
            .map { log ->
                val zdt = Instant.ofEpochMilli(log.completedAtMillis).atZone(zoneId)
                val minOfDay = zdt.hour * 60 + zdt.minute
                val duration = log.durationMinutes.coerceIn(5, 60)
                CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.REST_BREAK,
                    intensity = 1.0,
                    label = "Rest Break ${duration}m #${log.completedAtMillis}",
                    explicitStartMillis = log.completedAtMillis,
                    explicitEndMillis = log.completedAtMillis + duration * 60_000L
                )
            }

        return physBlocks + restBreakBlocks
    }

    /**
     * Builds on-curve markers for today's completed real-life energy boosters (hydration, snacks,
     * meals, walks, workouts, rest breaks, naps, caffeine, and wind-down).
     */
    fun buildTodayGraphMarkers(
        prefs: UserPreferences,
        naps: List<DaytimeNapBout>,
        stimulants: List<StimulantDoseBout>,
        dayStartMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<EnergyGraphBoosterMarker> {
        val dayEndMillis = dayStartMillis + 24 * 3_600_000L
        val markers = mutableListOf<EnergyGraphBoosterMarker>()

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in dayStartMillis..dayEndMillis }

        physLogs.forEach { log ->
            val (emoji, label, boost) = when (log.type) {
                PhysiologicalEventType.HYDRATION -> Triple("💧", "Hydrated", "+6% alertness")
                PhysiologicalEventType.SNACK -> Triple("🥜", "Balanced Snack", "+8% steady fuel")
                PhysiologicalEventType.MEAL -> Triple("🍽️", "Meal Logged", "Digestion curve")
                PhysiologicalEventType.SUNLIGHT_WALK -> Triple("☀️", "Sunlight Walk", "+9% boost")
                PhysiologicalEventType.WORKOUT -> Triple("🏃", "Workout", "+12% post-workout")
                PhysiologicalEventType.REST_BREAK -> Triple("🌿", "${log.durationMinutes.coerceAtLeast(5)}m Rest", "+10% recovery")
                PhysiologicalEventType.NAP -> Triple("💤", "${log.durationMinutes.coerceAtLeast(10)}m Nap", "Sleep pressure cleared")
                PhysiologicalEventType.WIND_DOWN -> Triple("🌙", "Wind-Down", "Sleep protection")
            }
            markers.add(
                EnergyGraphBoosterMarker(
                    id = "phys_${log.type.name}_${log.timestampMillis}",
                    timestampMillis = log.timestampMillis,
                    emoji = emoji,
                    label = label,
                    boostSummary = boost
                )
            )
        }

        // Add rest breaks logged via Rest Check-In / Break Timer (deduplicated against physLogs)
        SmartAssistantCodecs.parseRestBreakLogs(prefs.restBreakLogsJson)
            .filter { it.completedAtMillis in dayStartMillis..dayEndMillis }
            .forEach { rest ->
                val alreadyMapped = markers.any {
                    it.emoji == "🌿" && abs(it.timestampMillis - rest.completedAtMillis) <= 2 * 60_000L
                }
                if (!alreadyMapped) {
                    markers.add(
                        EnergyGraphBoosterMarker(
                            id = "rest_${rest.completedAtMillis}",
                            timestampMillis = rest.completedAtMillis,
                            emoji = "🌿",
                            label = "${rest.durationMinutes}m Rest",
                            boostSummary = "+10% recovery"
                        )
                    )
                }
            }

        // Add daytime naps not already represented via quickPhysLogs
        naps.filter { it.startMillis in dayStartMillis..dayEndMillis }.forEach { nap ->
            val alreadyMapped = markers.any {
                it.emoji == "💤" && abs(it.timestampMillis - nap.startMillis) <= 5 * 60_000L
            }
            if (!alreadyMapped) {
                markers.add(
                    EnergyGraphBoosterMarker(
                        id = "nap_${nap.startMillis}",
                        timestampMillis = nap.startMillis,
                        emoji = "💤",
                        label = "${nap.durationMinutes.coerceAtLeast(10)}m Nap",
                        boostSummary = "Sleep pressure cleared"
                    )
                )
            }
        }

        // Add caffeine / stimulant doses
        stimulants.filter { it.timestampMillis in dayStartMillis..dayEndMillis }.forEach { dose ->
            val label = when (dose.type) {
                StimulantType.CAFFEINE -> "Coffee"
                StimulantType.GREEN_TEA_L_THEANINE -> "Green Tea"
                StimulantType.PRESCRIPTION_STIMULANT_IR -> "Rx IR"
                StimulantType.PRESCRIPTION_STIMULANT_XR -> "Rx XR"
            }
            markers.add(
                EnergyGraphBoosterMarker(
                    id = "stim_${dose.type.name}_${dose.timestampMillis}",
                    timestampMillis = dose.timestampMillis,
                    emoji = if (dose.type == StimulantType.GREEN_TEA_L_THEANINE) "🍵" else "☕",
                    label = label,
                    boostSummary = "+${dose.doseMg.toInt()}mg caffeine"
                )
            )
        }

        return markers
            .sortedBy { it.timestampMillis }
            .fold(mutableListOf()) { acc, marker ->
                // Keep markers readable by avoiding overlapping identical-emoji spikes within 10 minutes
                val duplicate = acc.lastOrNull()?.let {
                    it.emoji == marker.emoji && abs(it.timestampMillis - marker.timestampMillis) < 10 * 60_000L
                } ?: false
                if (!duplicate) acc.add(marker)
                acc
            }
    }

    /**
     * Produces the top contextual energy-boosting recommendations right now across all 4 categories:
     * 1. Power nap & sleep wind-down
     * 2. Meals, snacks & hydration
     * 3. Rest breaks, walks & sunlight
     * 4. Short workouts & safe caffeine timing (strictly BEFORE sleep cutoff!)
     */
    fun recommendBoosters(
        prefs: UserPreferences,
        snapshot: CircadianSnapshot,
        burnoutRiskState: BurnoutRiskState,
        stimulants: List<StimulantDoseBout>,
        naps: List<DaytimeNapBout>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxResults: Int = 3
    ): List<EnergyBoosterRecommendation> {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val minuteOfDay = zdt.hour * 60 + zdt.minute
        val sleepHour = prefs.sleepHour.coerceIn(0, 23)
        val sleepMinuteOfDay = sleepHour * 60
        val minutesUntilBedtime = ((sleepMinuteOfDay - minuteOfDay + 1440) % 1440)
        val isNearBedtime = minutesUntilBedtime <= 90 || snapshot.phase == EnergyPhase.WIND_DOWN || snapshot.phase == EnergyPhase.TROUGH
        val isDaylightHours = zdt.hour in 7..17
        val safeCaffeineCutoffMin = computeLatestSafeStimulantCutoffMinute(sleepHour)
        val isBeforeSafeCaffeineCutoff = minuteOfDay < safeCaffeineCutoffMin && minutesUntilBedtime >= 390

        val dayStart = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStart - 2 * 3_600_000L)..nowMillis }
        val restLogs = SmartAssistantCodecs.parseRestBreakLogs(prefs.restBreakLogsJson)
            .filter { it.completedAtMillis in dayStart..nowMillis }

        val lastHydrationAt = maxOf(
            prefs.lastHydrationLoggedAtMillis,
            physLogs.filter { it.type == PhysiologicalEventType.HYDRATION }.maxOfOrNull { it.timestampMillis } ?: 0L
        )
        val minutesSinceHydration = if (lastHydrationAt > 0L) ((nowMillis - lastHydrationAt) / 60_000L).toInt() else 180

        val lastFuelAt = physLogs
            .filter { it.type == PhysiologicalEventType.MEAL || it.type == PhysiologicalEventType.SNACK }
            .maxOfOrNull { it.timestampMillis } ?: 0L
        val minutesSinceFuel = if (lastFuelAt > 0L) ((nowMillis - lastFuelAt) / 60_000L).toInt() else 240

        val lastWalkAt = physLogs
            .filter { it.type == PhysiologicalEventType.SUNLIGHT_WALK }
            .maxOfOrNull { it.timestampMillis } ?: 0L
        val minutesSinceWalk = if (lastWalkAt > 0L) ((nowMillis - lastWalkAt) / 60_000L).toInt() else 240

        val lastWorkoutAt = physLogs
            .filter { it.type == PhysiologicalEventType.WORKOUT }
            .maxOfOrNull { it.timestampMillis } ?: 0L
        val minutesSinceWorkout = if (lastWorkoutAt > 0L) ((nowMillis - lastWorkoutAt) / 60_000L).toInt() else 360

        val lastRestAt = maxOf(
            restLogs.maxOfOrNull { it.completedAtMillis } ?: 0L,
            physLogs.filter { it.type == PhysiologicalEventType.REST_BREAK }.maxOfOrNull { it.timestampMillis } ?: 0L
        )
        val minutesSinceRest = if (lastRestAt > 0L) ((nowMillis - lastRestAt) / 60_000L).toInt() else 120

        val hasNappedToday = naps.any { it.startMillis >= dayStart } ||
            physLogs.any { it.type == PhysiologicalEventType.NAP && it.timestampMillis >= dayStart }
        val lastCaffeineAt = stimulants.maxOfOrNull { it.timestampMillis } ?: 0L
        val minutesSinceCaffeine = if (lastCaffeineAt > 0L) ((nowMillis - lastCaffeineAt) / 60_000L).toInt() else 360
        val totalTodayCaffeineMg = stimulants.filter { it.timestampMillis >= dayStart }.sumOf { it.doseMg }
        val hasWindDownToday = physLogs.any { it.type == PhysiologicalEventType.WIND_DOWN && it.timestampMillis >= dayStart }

        val usable = snapshot.usableEnergy
        val sleepPressure = snapshot.homeostaticPressureS
        val candidates = mutableListOf<EnergyBoosterRecommendation>()

        // 1. Sleep Wind-Down (when bedtime is within 90 minutes or phase is WIND_DOWN / TROUGH)
        if (isNearBedtime && !hasWindDownToday) {
            val score = if (minutesUntilBedtime <= 60) 98.0 else 86.0
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_wind_down",
                    type = EnergyBoosterType.SLEEP_WIND_DOWN,
                    title = "Sleep Wind-Down",
                    emoji = EnergyBoosterType.SLEEP_WIND_DOWN.emoji,
                    durationMinutes = 20,
                    predictedBoostPoints = 14,
                    boostBadgeLabel = "+14% tomorrow",
                    reason = "Dim screens & wind down now to protect tonight's deep sleep",
                    priorityScore = score,
                    supportsTimer = false
                )
            )
        }

        // 2. Power Nap (afternoon dip or high sleep pressure, safe >= 6h before bedtime, max 1/day)
        if (!hasNappedToday && !isNearBedtime && minutesUntilBedtime >= 360 && (usable <= 64 || sleepPressure >= 0.48 || snapshot.phase == EnergyPhase.DIP)) {
            val isPrimeNapWindow = zdt.hour in 12..16
            val score = (if (isPrimeNapWindow) 78.0 else 58.0) +
                (if (usable < 50) 14.0 else 4.0) +
                (if (sleepPressure >= 0.55) 10.0 else 0.0)
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_power_nap",
                    type = EnergyBoosterType.POWER_NAP,
                    title = "20m Power Nap",
                    emoji = EnergyBoosterType.POWER_NAP.emoji,
                    durationMinutes = 20,
                    predictedBoostPoints = 14,
                    boostBadgeLabel = "+14% energy",
                    reason = "Clears adenosine sleep pressure without sleep inertia",
                    priorityScore = score,
                    supportsTimer = true
                )
            )
        }

        // 3. Rest / NSDR Break (high burnout, sustained focus, crash, or low energy)
        if (minutesSinceRest >= 35) {
            val burnoutBonus = when (burnoutRiskState.riskLevel) {
                BurnoutRiskLevel.HIGH -> 32.0
                BurnoutRiskLevel.APPROACHING -> 22.0
                BurnoutRiskLevel.CALM -> 0.0
            }
            val energyBonus = when {
                usable < 45 -> 18.0
                usable < 65 -> 10.0
                else -> 2.0
            }
            val restMins = if (burnoutRiskState.riskLevel == BurnoutRiskLevel.HIGH) 15 else 10
            val estGain = if (restMins >= 15) 11 else 9
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_rest_break",
                    type = EnergyBoosterType.REST_BREAK,
                    title = "${restMins}m Rest Break",
                    emoji = EnergyBoosterType.REST_BREAK.emoji,
                    durationMinutes = restMins,
                    predictedBoostPoints = estGain,
                    boostBadgeLabel = "+${estGain}% energy",
                    reason = if (burnoutRiskState.isElevated) {
                        "Eases cognitive strain and restores nervous-system focus"
                    } else {
                        "Short eyes-closed reset to rebound mental stamina"
                    },
                    priorityScore = 54.0 + burnoutBonus + energyBonus,
                    supportsTimer = true
                )
            )
        }

        // 4. Sunlight Walk (daylight hours, great for afternoon dip or screen fatigue)
        if (isDaylightHours && !isNearBedtime && minutesSinceWalk >= 90) {
            val dipBonus = if (snapshot.phase == EnergyPhase.DIP || usable < 65) 18.0 else 6.0
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_sunlight_walk",
                    type = EnergyBoosterType.SUNLIGHT_WALK,
                    title = "12m Sunlight Walk",
                    emoji = EnergyBoosterType.SUNLIGHT_WALK.emoji,
                    durationMinutes = 12,
                    predictedBoostPoints = 9,
                    boostBadgeLabel = "+9% energy",
                    reason = "Outdoor light + gentle steps reset alertness naturally",
                    priorityScore = 62.0 + dipBonus,
                    supportsTimer = false
                )
            )
        }

        // 5. Balanced Snack / Meal Refuel (when fasting >= 3h and not right before bed)
        if (!isNearBedtime && minutesSinceFuel >= 150) {
            val isMealWindow = zdt.hour in listOf(7, 8, 9, 12, 13, 14, 18, 19) && minutesSinceFuel >= 210
            if (isMealWindow) {
                candidates.add(
                    EnergyBoosterRecommendation(
                        id = "booster_balanced_snack",
                        type = EnergyBoosterType.BALANCED_SNACK,
                        title = "Protein + Fruit Refuel",
                        emoji = EnergyBoosterType.BALANCED_SNACK.emoji,
                        durationMinutes = 15,
                        predictedBoostPoints = 8,
                        boostBadgeLabel = "+8% steady",
                        reason = "Balanced fuel prevents blood-sugar crashes and brain fog",
                        priorityScore = 68.0 + (if (usable < 60) 10.0 else 0.0),
                        supportsTimer = false
                    )
                )
            } else {
                candidates.add(
                    EnergyBoosterRecommendation(
                        id = "booster_balanced_snack",
                        type = EnergyBoosterType.BALANCED_SNACK,
                        title = "Balanced Snack",
                        emoji = EnergyBoosterType.BALANCED_SNACK.emoji,
                        durationMinutes = 10,
                        predictedBoostPoints = 8,
                        boostBadgeLabel = "+8% energy",
                        reason = "Light protein + complex carbs steady afternoon focus",
                        priorityScore = 64.0 + (if (usable < 60) 12.0 else 0.0),
                        supportsTimer = false
                    )
                )
            }
        }

        // 6. Hydration (quickest micro-booster whenever gap >= 75m)
        if (minutesSinceHydration >= 75) {
            val gapBonus = when {
                minutesSinceHydration >= 180 -> 22.0
                minutesSinceHydration >= 120 -> 14.0
                else -> 6.0
            }
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_hydration",
                    type = EnergyBoosterType.HYDRATION,
                    title = "Drink Water",
                    emoji = EnergyBoosterType.HYDRATION.emoji,
                    durationMinutes = 5,
                    predictedBoostPoints = 6,
                    boostBadgeLabel = "+6% energy",
                    reason = "Even mild dehydration drops focus speed & alertness",
                    priorityScore = 58.0 + gapBonus + (if (usable < 65) 6.0 else 0.0),
                    supportsTimer = false
                )
            )
        }

        // 7. Short Movement Burst / Workout (not near bedtime, not already worked out recently)
        if (!isNearBedtime && minutesUntilBedtime >= 180 && minutesSinceWorkout >= 240 && burnoutRiskState.riskLevel != BurnoutRiskLevel.HIGH) {
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_short_workout",
                    type = EnergyBoosterType.SHORT_WORKOUT,
                    title = "15m Movement Burst",
                    emoji = EnergyBoosterType.SHORT_WORKOUT.emoji,
                    durationMinutes = 15,
                    predictedBoostPoints = 11,
                    boostBadgeLabel = "+11% after",
                    reason = "Triggers dopamine & BDNF surge for your next focus block",
                    priorityScore = 56.0 + (if (usable in 40..72) 12.0 else 0.0),
                    supportsTimer = false
                )
            )
        }

        // 8. Smart Caffeine (STRICTLY before safe sleep cutoff, < 250mg today, gap >= 150m, not in crash)
        if (
            isBeforeSafeCaffeineCutoff &&
            totalTodayCaffeineMg < 250.0 &&
            minutesSinceCaffeine >= 150 &&
            usable <= 68 &&
            snapshot.phase != EnergyPhase.CRASH
        ) {
            val cutoffFormatted = String.format(
                java.util.Locale.US,
                "%02d:%02d",
                safeCaffeineCutoffMin / 60,
                safeCaffeineCutoffMin % 60
            )
            candidates.add(
                EnergyBoosterRecommendation(
                    id = "booster_smart_caffeine",
                    type = EnergyBoosterType.SMART_CAFFEINE,
                    title = "Smart Caffeine",
                    emoji = EnergyBoosterType.SMART_CAFFEINE.emoji,
                    durationMinutes = 5,
                    predictedBoostPoints = 12,
                    boostBadgeLabel = "+12% peak",
                    reason = "Safe before $cutoffFormatted cutoff — blocks adenosine cleanly",
                    priorityScore = 60.0 + (if (usable < 55) 14.0 else 4.0),
                    supportsTimer = false
                )
            )
        }

        // Guarantee at least 2 helpful options even on a well-rested peak day
        if (candidates.size < 2 && !isNearBedtime) {
            if (candidates.none { it.type == EnergyBoosterType.HYDRATION }) {
                candidates.add(
                    EnergyBoosterRecommendation(
                        id = "booster_hydration",
                        type = EnergyBoosterType.HYDRATION,
                        title = "Drink Water",
                        emoji = EnergyBoosterType.HYDRATION.emoji,
                        durationMinutes = 5,
                        predictedBoostPoints = 6,
                        boostBadgeLabel = "+6% energy",
                        reason = "Keep hydration steady to sustain peak focus",
                        priorityScore = 45.0,
                        supportsTimer = false
                    )
                )
            }
            if (candidates.none { it.type == EnergyBoosterType.REST_BREAK }) {
                candidates.add(
                    EnergyBoosterRecommendation(
                        id = "booster_rest_break",
                        type = EnergyBoosterType.REST_BREAK,
                        title = "10m Rest Break",
                        emoji = EnergyBoosterType.REST_BREAK.emoji,
                        durationMinutes = 10,
                        predictedBoostPoints = 9,
                        boostBadgeLabel = "+9% energy",
                        reason = "Proactive micro-recovery keeps your curve high",
                        priorityScore = 44.0,
                        supportsTimer = true
                    )
                )
            }
        }

        return candidates
            .sortedByDescending { it.priorityScore }
            .take(maxResults)
    }

    /**
     * Evaluates whether the global Smart Assistant should show a proactive Energy Booster nudge card.
     * Strictly silenced during active Focus sessions, active Break timers, snooze windows, or
     * within [BOOSTER_NUDGE_COOLDOWN_MILLIS] of the last booster action/nudge.
     */
    fun evaluateAssistantNudge(
        prefs: UserPreferences,
        snapshot: CircadianSnapshot,
        recommendations: List<EnergyBoosterRecommendation>,
        nowMillis: Long,
        isFocusSessionActive: Boolean,
        isBreakTimerActive: Boolean
    ): EnergyBoosterNudgeProposal? {
        if (!prefs.wellnessNudgesEnabled) return null
        if (isFocusSessionActive || isBreakTimerActive) return null
        if (nowMillis < prefs.snoozedEnergyBoosterUntilMillis) return null
        if (prefs.lastEnergyBoosterNudgeAtMillis > 0L &&
            (nowMillis - prefs.lastEnergyBoosterNudgeAtMillis) < BOOSTER_NUDGE_COOLDOWN_MILLIS
        ) {
            return null
        }

        val top = recommendations.firstOrNull() ?: return null
        val shouldNudge = when {
            top.type == EnergyBoosterType.SLEEP_WIND_DOWN -> true
            snapshot.usableEnergy < 62 -> true
            snapshot.phase == EnergyPhase.DIP || snapshot.phase == EnergyPhase.RECOVERY_NEEDED -> true
            else -> false
        }
        if (!shouldNudge) return null

        val headline = when (top.type) {
            EnergyBoosterType.SLEEP_WIND_DOWN -> "Time to wind down for tomorrow's energy"
            EnergyBoosterType.POWER_NAP -> "Energy at ${snapshot.usableEnergy}% — recharge with a quick nap?"
            EnergyBoosterType.BALANCED_SNACK, EnergyBoosterType.MEAL_REFUEL ->
                "Energy dipping to ${snapshot.usableEnergy}% — time to refuel?"
            EnergyBoosterType.HYDRATION -> "Quick ${top.boostBadgeLabel} lift — grab a glass of water"
            EnergyBoosterType.SUNLIGHT_WALK -> "Energy at ${snapshot.usableEnergy}% — step outside for ${top.durationMinutes}m?"
            EnergyBoosterType.REST_BREAK -> "Recharge your battery (${snapshot.usableEnergy}%) with a short rest"
            EnergyBoosterType.SHORT_WORKOUT -> "Shake off the slump (${snapshot.usableEnergy}%) with a ${top.durationMinutes}m move"
            EnergyBoosterType.SMART_CAFFEINE -> "Safe caffeine window open — boost ${snapshot.usableEnergy}% energy?"
        }

        return EnergyBoosterNudgeProposal(
            recommendation = top,
            headline = headline,
            body = "${top.reason}. Logging it updates your live energy curve right away.",
            currentEnergyPercent = snapshot.usableEnergy
        )
    }

    /**
     * Converts a booster recommendation into the simulated lever inputs (naps, stimulants, or
     * physiological impulse blocks) so the user can preview its exact impact on the 24h curve.
     */
    fun toPreviewSimulationInputs(
        recommendation: EnergyBoosterRecommendation,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Triple<List<DaytimeNapBout>, List<StimulantDoseBout>, List<CognitiveBlockWindow>> {
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val minOfDay = zdt.hour * 60 + zdt.minute
        val dur = recommendation.durationMinutes.coerceIn(5, 60)
        return when (recommendation.type) {
            EnergyBoosterType.POWER_NAP -> Triple(
                listOf(DaytimeNapBout(nowMillis, nowMillis + dur * 60_000L)),
                emptyList(),
                emptyList()
            )
            EnergyBoosterType.SMART_CAFFEINE -> Triple(
                emptyList(),
                listOf(StimulantDoseBout(timestampMillis = nowMillis, doseMg = 95.0, type = StimulantType.CAFFEINE)),
                emptyList()
            )
            EnergyBoosterType.HYDRATION -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.HYDRATION,
                        intensity = 1.0,
                        label = "Preview Hydration",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
            EnergyBoosterType.BALANCED_SNACK, EnergyBoosterType.MEAL_REFUEL -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.SNACK,
                        intensity = 1.0,
                        label = "Preview Refuel",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
            EnergyBoosterType.SUNLIGHT_WALK -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.SUNLIGHT_WALK,
                        intensity = 1.0,
                        label = "Preview Sunlight Walk",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
            EnergyBoosterType.REST_BREAK -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.REST_BREAK,
                        intensity = 1.0,
                        label = "Preview Rest Break",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
            EnergyBoosterType.SHORT_WORKOUT -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.EXERCISE,
                        intensity = 0.85,
                        label = "Preview Movement Burst",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
            EnergyBoosterType.SLEEP_WIND_DOWN -> Triple(
                emptyList(),
                emptyList(),
                listOf(
                    CognitiveBlockWindow(
                        startMinuteOfDay = minOfDay,
                        endMinuteOfDay = (minOfDay + dur) % 1440,
                        category = CognitiveBlockCategory.REST_BREAK,
                        intensity = 0.75,
                        label = "Preview Wind-Down",
                        explicitStartMillis = nowMillis,
                        explicitEndMillis = nowMillis + dur * 60_000L
                    )
                )
            )
        }
    }
}
