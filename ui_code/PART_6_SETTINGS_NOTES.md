# neuroFlow UI Codebase — Settings Hub, Sleep Logs, MEQ Quiz & Notes Workspace

**Description:** SettingsScreen/ViewModel, MEQQuiz, SleepLogs, PriorityWeights, NotesScreen/Editor, and QuickNotesBottomSheet.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 33  
**Lines of Code:** 8,906  
**Uncompressed Size:** 379.2 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/common/EnergyInsight.kt`](#appsrcmainjavacomneuroflowapppresentationcommonenergyinsightkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/FrogConfetti.kt`](#appsrcmainjavacomneuroflowapppresentationcommonfrogconfettikt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/HyperFocusAllowlistRoute.kt`](#appsrcmainjavacomneuroflowapppresentationcommonhyperfocusallowlistroutekt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/ManualTimeLogSheet.kt`](#appsrcmainjavacomneuroflowapppresentationcommonmanualtimelogsheetkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/NewChapterCard.kt`](#appsrcmainjavacomneuroflowapppresentationcommonnewchaptercardkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationcommontasktagviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/TimeUtils.kt`](#appsrcmainjavacomneuroflowapppresentationcommontimeutilskt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/TopGoalsRefillCard.kt`](#appsrcmainjavacomneuroflowapppresentationcommontopgoalsrefillcardkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/UlyssesContractSheet.kt`](#appsrcmainjavacomneuroflowapppresentationcommonulyssescontractsheetkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/WeeklyPlanUtils.kt`](#appsrcmainjavacomneuroflowapppresentationcommonweeklyplanutilskt)
- [`app/src/main/java/com/neuroflow/app/presentation/identity/IdentityScreen.kt`](#appsrcmainjavacomneuroflowapppresentationidentityidentityscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/identity/IdentityViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationidentityidentityviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeScreen.kt`](#appsrcmainjavacomneuroflowapppresentationlogtimelogtimescreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationlogtimelogtimeviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/notes/NoteColors.kt`](#appsrcmainjavacomneuroflowapppresentationnotesnotecolorskt)
- [`app/src/main/java/com/neuroflow/app/presentation/notes/NoteEditorScreen.kt`](#appsrcmainjavacomneuroflowapppresentationnotesnoteeditorscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/notes/NotesScreen.kt`](#appsrcmainjavacomneuroflowapppresentationnotesnotesscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationnotesnotesviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/onboarding/AppGuideScreen.kt`](#appsrcmainjavacomneuroflowapppresentationonboardingappguidescreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/onboarding/OnboardingScreen.kt`](#appsrcmainjavacomneuroflowapppresentationonboardingonboardingscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingsaccountsyncscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationsettingsaccountsyncviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingsmeqquizscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationsettingsmeqquizviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/PriorityWeightsScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingspriorityweightsscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/PrivacyPermissionsScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingsprivacypermissionsscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingssettingsscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/SettingsViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationsettingssettingsviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/settings/SleepLogsScreen.kt`](#appsrcmainjavacomneuroflowapppresentationsettingssleeplogsscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/common/SecondaryScreensUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2commonsecondaryscreensupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt`](#appsrcmainjavacomneuroflowapppresentationv2notesquicknotesbottomsheetkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/settings/HyperFocusNotesAndSettingsUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2settingshyperfocusnotesandsettingsupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/settings/SubScreensAndHyperFocusUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2settingssubscreensandhyperfocusupgradesv2kt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationcommonenergyinsightkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/EnergyInsight.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/EnergyInsight.kt`
- **Lines of Code:** 287
- **Size:** 11,400 bytes (11.1 KB)

```kotlin
package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.model.EffectivePeakProfile
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import java.util.Calendar
import kotlin.math.abs

object EnergyInsight {

    fun isMorningType(chronotype: String?): Boolean {
        return chronotype == "MODERATE_MORNING" || chronotype == "DEFINITE_MORNING"
    }

    fun effectivePeakMinuteOfDay(prefs: UserPreferences): Int {
        return if (
            prefs.quizPeakEnabled &&
            prefs.effectivePeakMinuteOfDay in 0 until (24 * 60)
        ) {
            prefs.effectivePeakMinuteOfDay
        } else {
            (prefs.peakEnergyStart.coerceIn(0, 23) * 60)
        }
    }

    fun detectedPeakMinuteOfDayOrNull(prefs: UserPreferences): Int? {
        return prefs.detectedPeakMinuteOfDay.takeIf { it in 0 until (24 * 60) }
    }

    fun confidencePercent(prefs: UserPreferences): Int {
        return (prefs.peakDetectionConfidence.coerceIn(0f, 1f) * 100f).toInt()
    }

    fun minuteLabel(minuteOfDay: Int): String {
        val normalized = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
        val hour = normalized / 60
        val minute = normalized % 60
        val amPm = if (hour < 12) "am" else "pm"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d%s", displayHour, minute, amPm)
    }

    fun timingHintForNow(targetMinuteOfDay: Int, now: Calendar = Calendar.getInstance()): String {
        val nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val minutesToPeak = minuteDistance(nowMinute, targetMinuteOfDay)
        return if (minutesToPeak <= 10) {
            "Peak zone now"
        } else {
            "Peak around ${minuteLabel(targetMinuteOfDay)}"
        }
    }

    fun confidenceTier(confidence: Float): String {
        val safe = confidence.coerceIn(0f, 1f)
        return when {
            safe >= 0.8f -> "High"
            safe >= 0.6f -> "Moderate"
            else -> "Low"
        }
    }

    fun ageLabel(ageMillis: Long): String {
        val safe = ageMillis.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        return when {
            minutes < 1L -> "<1m"
            minutes < 60L -> "${minutes}m"
            else -> {
                val hours = minutes / 60L
                val rem = minutes % 60L
                if (rem == 0L) "${hours}h" else "${hours}h ${rem}m"
            }
        }
    }

    fun freshnessLabel(ageMillis: Long): String {
        val safe = ageMillis.coerceAtLeast(0L)
        return when {
            safe <= 2 * 60_000L -> "Fresh"
            safe <= 10 * 60_000L -> "Aging"
            else -> "Stale"
        }
    }

    fun stabilityScore(
        momentConfidence: Float,
        peakConfidence: Float,
        freshnessAgeMillis: Long
    ): Int {
        val safeMoment = momentConfidence.coerceIn(0f, 1f)
        val safePeak = peakConfidence.coerceIn(0f, 1f)
        val freshnessFactor = when {
            freshnessAgeMillis <= 2 * 60_000L -> 1f
            freshnessAgeMillis <= 10 * 60_000L -> 0.82f
            freshnessAgeMillis <= 30 * 60_000L -> 0.62f
            else -> 0.45f
        }
        val composite = (
            safeMoment * 0.45f +
                safePeak * 0.35f +
                freshnessFactor * 0.20f
            ).coerceIn(0f, 1f)
        return (composite * 100f).toInt().coerceIn(0, 100)
    }

    fun confidenceTierRationale(
        momentConfidence: Float,
        peakConfidence: Float,
        freshnessAgeMillis: Long
    ): String {
        val tier = confidenceTier(((momentConfidence + peakConfidence) / 2f).coerceIn(0f, 1f))
        val freshness = freshnessLabel(freshnessAgeMillis).lowercase()
        return "$tier confidence • peak ${(peakConfidence.coerceIn(0f, 1f) * 100f).toInt()}% • " +
            "moment ${(momentConfidence.coerceIn(0f, 1f) * 100f).toInt()}% • signals $freshness"
    }

    fun backtestFitLabel(errorMinutes: Float?): String {
        if (errorMinutes == null) return "Backtest fit: pending"
        val rounded = errorMinutes.toInt().coerceAtLeast(0)
        val quality = when {
            rounded <= 30 -> "Strong"
            rounded <= 75 -> "Moderate"
            else -> "Weak"
        }
        return "Backtest fit: $quality (${rounded}m error)"
    }

    fun whyEnergyChanged(momentAdjustment: Float, momentSummary: String): String {
        val direction = when {
            momentAdjustment >= 4f -> "Energy moved up from supportive context"
            momentAdjustment <= -4f -> "Energy moved down from pressure signals"
            else -> "Energy stayed close to baseline"
        }
        val summary = momentSummary.ifBlank { "Context signals are limited." }
        return "$direction. $summary"
    }

    fun whatToDoNow(
        availableEnergy: Int,
        fatigueZone: FatigueZone,
        hasRecentData: Boolean
    ): String {
        if (!hasRecentData) {
            return "Signals look stale. Refresh context and avoid major priority changes until data updates."
        }
        return when {
            availableEnergy >= 75 && fatigueZone <= FatigueZone.MODERATE -> {
                "Do deep work now while capacity is high."
            }
            availableEnergy >= 50 -> {
                "Push meaningful progress on medium-complexity tasks."
            }
            availableEnergy >= 30 -> {
                "Use this window for admin tasks and interruption cleanup."
            }
            else -> {
                "Take a short recovery break, then restart with a small low-friction task."
            }
        }
    }

    fun taskEnergyMismatchHint(taskEnergyLevel: EnergyLevel, availableEnergy: Int): String? {
        val score = availableEnergy.coerceIn(0, 100)
        return when (taskEnergyLevel) {
            EnergyLevel.HIGH -> if (score < 60) {
                "This task needs high energy, but your current score is low. Consider a shorter warm-up task first."
            } else {
                null
            }

            EnergyLevel.MEDIUM -> if (score < 35) {
                "This medium-energy task may feel heavy right now. Break it into a smaller first step."
            } else {
                null
            }

            EnergyLevel.LOW -> if (score > 75) {
                "You have surplus energy now. A higher-energy task could create better momentum."
            } else {
                null
            }
        }
    }

    fun profileSummary(profile: EffectivePeakProfile?): String {
        if (profile == null) return "Baseline profile"
        val windows = profile.windows.joinToString(separator = ", ") { w ->
            "${(w.durationMinutes / 60f)}h"
        }
        return "${profile.profileType.name.lowercase().replaceFirstChar { it.uppercase() }} profile • windows $windows"
    }

    fun profileConfidenceLine(profile: EffectivePeakProfile?): String {
        if (profile == null) return "Confidence unavailable"
        val c = profile.confidence
        return "Confidence ${confidenceTier(c.overall)} (${(c.overall * 100).toInt()}%) • " +
            "sleep ${(c.sleepCoverage * 100).toInt()}% • wake ${(c.wakeConsistency * 100).toInt()}% • behavior ${(c.behaviorPerformance * 100).toInt()}%"
    }

    fun windowConfidencePercent(profile: EffectivePeakProfile?, windowIndex: Int): Int {
        val fallback = profile?.confidence?.overall ?: 0.5f
        val value = profile?.windowConfidences?.getOrNull(windowIndex)?.coerceIn(0f, 1f) ?: fallback
        return (value * 100f).toInt().coerceIn(0, 100)
    }

    fun windowConfidenceTier(profile: EffectivePeakProfile?, windowIndex: Int): String {
        val fallback = profile?.confidence?.overall ?: 0.5f
        val value = profile?.windowConfidences?.getOrNull(windowIndex)?.coerceIn(0f, 1f) ?: fallback
        return confidenceTier(value)
    }

    fun confidenceImprovementActions(profile: EffectivePeakProfile?): List<String> {
        if (profile == null) {
            return listOf("Complete MEQ and log sleep for at least 5 nights to calibrate peak windows.")
        }

        if (profile.confidenceGatedAbstention) {
            val reason = profile.abstentionReason.ifBlank { "insufficient confidence for personalized prediction" }
            return listOf(
                "Prediction is currently withheld: $reason.",
                "Log sleep daily for 7 nights and keep wake time inside a 30-minute band.",
                "Complete at least one 20+ minute focus session during each suggested peak window for 5 days.",
                "Keep manual profile overrides off during this recovery period so the model can re-calibrate."
            )
        }

        val actions = mutableListOf<String>()
        val c = profile.confidence
        val peak2 = profile.windowConfidences.getOrNull(1) ?: c.overall
        val peak3 = profile.windowConfidences.getOrNull(2) ?: c.overall

        if (c.sleepCoverage < 0.7f) {
            actions += "Log sleep daily for 7 nights so the model can stabilize fatigue and peak timing."
        }
        if (c.wakeConsistency < 0.72f) {
            actions += "Keep wake time within +/-30 minutes for the next week to reduce timing drift."
        }
        if (c.behaviorPerformance < 0.7f) {
            actions += "Complete at least one focused task in each suggested peak window for 5 days."
        }
        if (peak2 < 0.65f || peak3 < 0.6f) {
            actions += "For Peak 2/3, schedule medium or light tasks there and avoid frequent app switching."
        }

        val backtestError = profile.weeklyBacktestErrorMinutes
        if (backtestError != null && backtestError > 75f) {
            actions += "Backtest error is high; keep manual overrides off for a few days so adaptive tuning can catch up."
        }
        if (profile.driftStatus != "stable") {
            actions += "Drift is detected; shift planned deep-work start by 30-60 minutes and reassess after 3 days."
        }

        if (actions.isEmpty()) {
            actions += "Confidence is stable. Keep current sleep logging and wake-time consistency to maintain it."
        }

        return actions.take(4)
    }

    fun adaptiveHint(profile: EffectivePeakProfile?): String {
        return profile?.explanation ?: "Using baseline peak estimate."
    }

    fun backtestSummary(profile: EffectivePeakProfile?): String {
        return backtestFitLabel(profile?.weeklyBacktestErrorMinutes)
    }

    fun profileModeLabel(manualOverrideEnabled: Boolean, profile: EffectivePeakProfile?): String {
        return when {
            manualOverrideEnabled -> "Profile mode: Manual override"
            profile?.confidenceGatedAbstention == true -> "Profile mode: Confidence-gated abstention (safe baseline)"
            profile?.adaptiveFreezeMode == true -> "Profile mode: Adaptive freeze safety fallback"
            profile != null -> "Profile mode: Adaptive ${profile.profileType.name.lowercase()}"
            else -> "Profile mode: Baseline"
        }
    }

    private fun minuteDistance(a: Int, b: Int): Int {
        val raw = abs(a - b)
        return minOf(raw, (24 * 60) - raw)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonfrogconfettikt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/FrogConfetti.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/FrogConfetti.kt`
- **Lines of Code:** 95
- **Size:** 3,232 bytes (3.2 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Float,
    val color: Color,
    val isFrogEmoji: Boolean
)

@Composable
fun FrogConfettiOverlay(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFF4CAF50), // Frog Green
            Color(0xFF81C784), // Light Green
            Color(0xFFFFD54F), // Gold
            Color(0xFF9C27B0), // NeuroFlow Purple
            Color(0xFFE91E63)  // Accent Pink
        )
        List(40) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = -Random.nextFloat() * 200f,
                speedY = Random.nextFloat() * 800f + 500f,
                speedX = (Random.nextFloat() - 0.5f) * 200f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors.random(),
                isFrogEmoji = it % 6 == 0 // Every 6th particle is a 🐸
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val currentProgress = progress.value
            val alpha = (1f - (currentProgress - 0.7f).coerceAtLeast(0f) / 0.3f).coerceIn(0f, 1f)

            particles.forEach { p ->
                val x = (p.initialX * canvasWidth + p.speedX * currentProgress).coerceIn(0f, canvasWidth)
                val y = p.initialY + p.speedY * currentProgress

                if (y in -50f..canvasHeight + 50f) {
                    if (p.isFrogEmoji) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "🐸",
                            x,
                            y,
                            android.graphics.Paint().apply {
                                textSize = p.size * 2.2f
                                this.alpha = (alpha * 255).toInt()
                            }
                        )
                    } else {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size / 2f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonhyperfocusallowlistroutekt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/HyperFocusAllowlistRoute.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/HyperFocusAllowlistRoute.kt`
- **Lines of Code:** 29
- **Size:** 1,066 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.screens.AllowlistSettingsScreen
import com.neuroflow.app.feature.hyperfocus.screens.HyperFocusActivationSheet

@Composable
fun HyperFocusAllowlistRoute(onNavigateBack: () -> Unit) {
    val viewModel: HyperFocusViewModel = hiltViewModel()
    var showActivationSheet by remember { mutableStateOf(false) }

    AllowlistSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onStartSession = { showActivationSheet = true }
    )
    if (showActivationSheet) {
        HyperFocusActivationSheet(
            viewModel = viewModel,
            onDismiss = { showActivationSheet = false }
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonmanualtimelogsheetkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/ManualTimeLogSheet.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/ManualTimeLogSheet.kt`
- **Lines of Code:** 135
- **Size:** 5,583 bytes (5.5 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet for manually logging time spent on a task.
 *
 * Shows hour + minute number pickers. Pre-fills from [prefillMinutes] when provided
 * (e.g. the task's estimated duration). Calls [onConfirm] with the total minutes
 * entered, or [onSkip] if the user wants to complete without logging time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTimeLogSheet(
    taskTitle: String,
    prefillMinutes: Int = 0,
    onConfirm: (Float) -> Unit,
    onSkip: () -> Unit
) {
    var hours by remember { mutableIntStateOf(prefillMinutes / 60) }
    var minutes by remember { mutableIntStateOf(prefillMinutes % 60) }

    val totalMinutes = hours * 60 + minutes

    ModalBottomSheet(onDismissRequest = onSkip) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏱ Log Time Spent", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                taskTitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(Modifier.height(24.dp))

            // Hour + minute pickers side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hours
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hours", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { if (hours > 0) hours-- },
                            modifier = Modifier.size(40.dp)
                        ) { Text("−", fontSize = 18.sp) }
                        Text(
                            "$hours",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { if (hours < 23) hours++ },
                            modifier = Modifier.size(40.dp)
                        ) { Text("+", fontSize = 18.sp) }
                    }
                }

                Spacer(Modifier.width(24.dp))
                Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(24.dp))

                // Minutes
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { if (minutes > 0) minutes -= 5 else if (hours > 0) { hours--; minutes = 55 } },
                            modifier = Modifier.size(40.dp)
                        ) { Text("−", fontSize = 18.sp) }
                        Text(
                            minutes.toString().padStart(2, '0'),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { if (minutes < 55) minutes += 5 else { minutes = 0; if (hours < 23) hours++ } },
                            modifier = Modifier.size(40.dp)
                        ) { Text("+", fontSize = 18.sp) }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (totalMinutes > 0) {
                Text(
                    "= $totalMinutes minutes total",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) { Text("Skip") }
                Button(
                    onClick = { onConfirm(totalMinutes.toFloat()) },
                    modifier = Modifier.weight(1f),
                    enabled = totalMinutes > 0
                ) { Text("Log & Done") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonnewchaptercardkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/NewChapterCard.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/NewChapterCard.kt`
- **Lines of Code:** 60
- **Size:** 2,174 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewChapterCard(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var intentText by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌅 New Chapter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "A fresh start is a great time to set your intention for the week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = intentText,
                onValueChange = { intentText = it },
                label = { Text("What do you commit to this week?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Skip", fontSize = 16.sp)
                }
                Button(
                    onClick = { onConfirm(intentText.trim()) },
                    enabled = intentText.isNotBlank()
                ) {
                    Text("Confirm", fontSize = 16.sp)
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommontasktagviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt`
- **Lines of Code:** 50
- **Size:** 1,597 bytes (1.6 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskTagViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val tags: StateFlow<List<String>> = userPreferencesDataStore.preferencesFlow
        .map { prefs -> prefs.tagCatalog }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTags(newTags: Collection<String>) {
        val cleaned = normalizeTags(newTags)
        if (cleaned.isEmpty()) return

        viewModelScope.launch {
            userPreferencesDataStore.mergeTagCatalog(cleaned)
        }
    }

    fun removeTag(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            userPreferencesDataStore.removeTagFromCatalog(cleaned)
        }
    }

    fun seedFromTasks(taskTags: Collection<String>) {
        addTags(taskTags)
    }

    private fun normalizeTags(tags: Collection<String>): List<String> {
        return tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommontimeutilskt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/TimeUtils.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/TimeUtils.kt`
- **Lines of Code:** 125
- **Size:** 4,286 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

fun formatRelativeTime(millis: Long, hasTime: Boolean, now: Long = System.currentTimeMillis()): String {
    if (!hasTime) {
        val zone = ZoneId.systemDefault()
        val nowDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val targetDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val diffDays = ChronoUnit.DAYS.between(nowDate, targetDate).toInt()
        return when {
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Tomorrow"
            diffDays == -1 -> "Yesterday"
            diffDays > 1 -> "In $diffDays days"
            else -> "Overdue by ${kotlin.math.abs(diffDays)} days"
        }
    }

    val diff = millis - now
    val absDiff = kotlin.math.abs(diff)

    if (absDiff < 60_000L) {
        return if (diff < 0) "Overdue" else "Right now"
    }

    val totalMinutes = absDiff / 60_000
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60

    if (diff < 0) {
        val parts = mutableListOf<String>()
        if (days > 0) parts += "$days day${if (days > 1) "s" else ""}"
        if (hours > 0) parts += "$hours hr${if (hours > 1) "s" else ""}"
        if (minutes > 0) parts += "$minutes min${if (minutes > 1) "s" else ""}"
        val timeStr = parts.joinToString(" ").ifBlank { "now" }
        return "Overdue by $timeStr"
    }

    val parts = mutableListOf<String>()
    if (days > 0) parts += "$days day${if (days > 1) "s" else ""}"
    if (hours > 0) parts += "$hours hr${if (hours > 1) "s" else ""}"
    if (minutes > 0 && days == 0L) parts += "$minutes min${if (minutes > 1) "s" else ""}"

    val timeStr = parts.joinToString(" ").ifBlank { "now" }
    return when {
        timeStr == "now" -> "Right now"
        else -> "In $timeStr"
    }
}

fun formatFullDate(millis: Long, hasTime: Boolean): String {
    val formatStr = if (hasTime) "MMM d, h:mm a" else "MMM d, yyyy"
    val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
    return sdf.format(Date(millis))
}

/**
 * A live session outranks the calendar: the block is happening, so the card must stop announcing
 * when it was going to start.
 */
fun timeBlockLabel(isRecurring: Boolean, isTracking: Boolean): String = when {
    isTracking -> "🎯 FOCUSING NOW"
    isRecurring -> "🔁 RECURRENCE TIME"
    else -> "📅 SCHEDULED"
}

fun timeBlockHeadline(
    anchorMillis: Long,
    hasTime: Boolean,
    isTracking: Boolean,
    now: Long = System.currentTimeMillis()
): String = if (isTracking) "Focusing now" else formatRelativeTime(anchorMillis, hasTime, now)

@Composable
fun TagChip(tag: String) {
    val chipColors = listOf(
        Color(0xFF1C1F28),
        Color(0xFF222736),
        Color(0xFF2A2E3B),
        Color(0xFF1F232E),
        Color(0xFF252936),
        Color(0xFF181B22)
    )
    val colorIndex = tag.hashCode().let { kotlin.math.abs(it) } % chipColors.size
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipColors[colorIndex],
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

fun formatDurationFloat(minutes: Float): String {
    val totalMinutes = minutes.toInt()
    return when {
        totalMinutes < 60 -> "${String.format(Locale.getDefault(), "%.1f", minutes)} min"
        else -> {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            "${h}h ${m}m"
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommontopgoalsrefillcardkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/TopGoalsRefillCard.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/TopGoalsRefillCard.kt`
- **Lines of Code:** 104
- **Size:** 4,020 bytes (3.9 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

enum class GoalPeriod { WEEKLY, YEARLY }

@Composable
fun TopGoalsRefillCard(
    period: GoalPeriod,
    existingGoals: List<String> = emptyList(),
    onConfirm: (List<String>) -> Unit,
    onSkip: () -> Unit
) {
    val label = if (period == GoalPeriod.YEARLY) "year" else "week"
    // Use existingGoals as key so fields populate correctly even if prefs load after first frame
    val goals = remember(existingGoals) {
        mutableStateListOf(
            existingGoals.getOrElse(0) { "" },
            existingGoals.getOrElse(1) { "" },
            existingGoals.getOrElse(2) { "" }
        )
    }
    // FocusRequesters are stable — safe to create once
    val focusRequesters = remember { List(3) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "What are your top 3 goals for this $label?",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "These will be pinned in your Plan hub as a daily reminder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            goals.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { goals[index] = it },
                    label = { Text("Goal ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[index]),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index < 2) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            // Safe: move focus via FocusManager, not direct requestFocus
                            focusManager.moveFocus(FocusDirection.Down)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            if (goals.any { it.isNotBlank() }) onConfirm(goals.map { it.trim() })
                        }
                    )
                )
                if (index < 2) Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip for now")
                }
                Button(
                    onClick = { onConfirm(goals.map { it.trim() }) },
                    enabled = goals.any { it.isNotBlank() }
                ) {
                    Text("Save goals")
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonulyssescontractsheetkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/UlyssesContractSheet.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/UlyssesContractSheet.kt`
- **Lines of Code:** 177
- **Size:** 8,269 bytes (8.1 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.worker.UlyssesEvaluatorWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UlyssesContractSheet(
    taskRepository: TaskRepository,
    contractRepository: UlyssesContractRepository,
    workManager: WorkManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTaskId by remember { mutableStateOf("") }
    var selectedTaskTitle by remember { mutableStateOf("") }
    var consequence by remember { mutableStateOf("") }
    var deadlineMillis by remember { mutableStateOf(System.currentTimeMillis() + 86_400_000L) }
    var showTaskPicker by remember { mutableStateOf(false) }
    val activeTasks by taskRepository.observeActiveTasks().collectAsStateWithLifecycle(initialValue = emptyList())

    val now = System.currentTimeMillis()
    val deadlineIsValid = deadlineMillis > now

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("⚔️ Ulysses Contract", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Commit to completing a task by a deadline with a personal consequence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Task picker
            OutlinedButton(
                onClick = { showTaskPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedTaskTitle.isNotBlank()) selectedTaskTitle else "Select a task...")
            }

            if (showTaskPicker) {
                activeTasks.forEach { task ->
                    TextButton(
                        onClick = {
                            selectedTaskId = task.id
                            selectedTaskTitle = task.title
                            showTaskPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(task.title, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline display (simplified — shows current deadline + +1 day / +1 week buttons)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Text("Deadline: ${sdf.format(Date(deadlineMillis))}", style = MaterialTheme.typography.bodyMedium)
            if (!deadlineIsValid) {
                Text(
                    "Deadline must be in the future.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { deadlineMillis += 86_400_000L }) { Text("+1 day") }
                OutlinedButton(onClick = { deadlineMillis += 7 * 86_400_000L }) { Text("+1 week") }
                OutlinedButton(onClick = { if (deadlineMillis - 86_400_000L > System.currentTimeMillis()) deadlineMillis -= 86_400_000L }) { Text("-1 day") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = consequence,
                onValueChange = { consequence = it },
                label = { Text("Consequence if I fail...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            val formattedDeadline = remember(deadlineMillis) {
                SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(deadlineMillis))
            }

            if (selectedTaskId.isNotBlank() && consequence.isNotBlank() && deadlineIsValid) {
                Spacer(modifier = Modifier.height(16.dp))
                com.neuroflow.app.presentation.v2.common.UpgradedUlyssesContractCard(
                    taskTitle = selectedTaskTitle,
                    commitmentStatement = "I commit unconditionally to finishing this task before the deadline.",
                    deadlineFormatted = formattedDeadline,
                    consequenceText = consequence,
                    onSignContractClick = {
                        scope.launch {
                            val contract = UlyssesContractEntity(
                                taskId = selectedTaskId,
                                deadlineAt = deadlineMillis,
                                consequence = consequence
                            )
                            contractRepository.insert(contract)
                            val delay = (deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                            val request = OneTimeWorkRequestBuilder<UlyssesEvaluatorWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(workDataOf("contractId" to contract.id))
                                .addTag("ulysses_eval_${contract.id}")
                                .build()
                            workManager.enqueue(request)
                            onDismiss()
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedTaskId.isBlank() || consequence.isBlank() || !deadlineIsValid) return@Button
                            scope.launch {
                                val contract = UlyssesContractEntity(
                                    taskId = selectedTaskId,
                                    deadlineAt = deadlineMillis,
                                    consequence = consequence
                                )
                                contractRepository.insert(contract)
                                val delay = (deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                                val request = OneTimeWorkRequestBuilder<UlyssesEvaluatorWorker>()
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(workDataOf("contractId" to contract.id))
                                    .addTag("ulysses_eval_${contract.id}")
                                    .build()
                                workManager.enqueue(request)
                                onDismiss()
                            }
                        },
                        enabled = selectedTaskId.isNotBlank() && consequence.isNotBlank() && deadlineIsValid
                    ) {
                        Text("Commit")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonweeklyplanutilskt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/WeeklyPlanUtils.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/WeeklyPlanUtils.kt`
- **Lines of Code:** 77
- **Size:** 2,927 bytes (2.9 KB)

```kotlin
package com.neuroflow.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduleAnchorMillis
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.util.Calendar

fun getWeekDays(referenceMillis: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    }

    val days = mutableListOf<Long>()
    for (i in 0 until 7) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return days
}

fun isSameCalendarDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun isTaskOnDay(task: TaskEntity, dayStart: Long, dayEnd: Long): Boolean {
    val anchor = task.effectiveScheduleAnchorMillis()
    if (anchor != null && anchor in dayStart until dayEnd) return true
    val deadline = task.deadlineDate
    if (anchor == null && deadline != null && deadline in dayStart until dayEnd) return true
    return false
}

fun isProtectedSlotOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
    return slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, dayMillis)
}

@Composable
fun getQuadrantBgColor(quadrant: Quadrant): Color {
    return when (quadrant) {
        Quadrant.DO_FIRST  -> NeuroFlowColors.DoFirstBg
        Quadrant.SCHEDULE  -> NeuroFlowColors.ScheduleBg
        Quadrant.DELEGATE  -> NeuroFlowColors.DelegateBg
        Quadrant.ELIMINATE -> NeuroFlowColors.EliminateBg
    }
}

@Composable
fun getQuadrantTextColor(quadrant: Quadrant): Color {
    return when (quadrant) {
        Quadrant.DO_FIRST  -> NeuroFlowColors.DoFirstText
        Quadrant.SCHEDULE  -> NeuroFlowColors.ScheduleText
        Quadrant.DELEGATE  -> NeuroFlowColors.DelegateText
        Quadrant.ELIMINATE -> NeuroFlowColors.EliminateText
    }
}

fun getQuadrantLabel(quadrant: Quadrant): String = when (quadrant) {
    Quadrant.DO_FIRST  -> "DO FIRST"
    Quadrant.SCHEDULE  -> "SCHEDULE"
    Quadrant.DELEGATE  -> "DELEGATE"
    Quadrant.ELIMINATE -> "ON HOLD"
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationidentityidentityscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityScreen.kt`
- **Lines of Code:** 189
- **Size:** 6,961 bytes (6.8 KB)

```kotlin
package com.neuroflow.app.presentation.identity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityScreen(
    onNavigateBack: () -> Unit,
    viewModel: IdentityViewModel = hiltViewModel()
) {
    val affirmations by viewModel.affirmations.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for duplicate rejection events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.duplicateEvent.collect {
            snackbarHostState.showSnackbar("Already in your list")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Affirmations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add affirmation")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val prefs by viewModel.preferences.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                com.neuroflow.app.presentation.v2.common.UpgradedIdentityAndGoalsCard(
                    identityStatement = prefs.identityLabel,
                    activeGoals = prefs.weeklyGoals,
                    onEditIdentityClick = { showDialog = true },
                    onRefillGoalsClick = { showDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (affirmations.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧠", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No affirmations yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + to add your first subliminal message",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "One of these will appear each time you complete a task.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(affirmations) { index, affirmation ->
                            AffirmationItem(
                                text = affirmation,
                                onDelete = { viewModel.removeAffirmation(index) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddAffirmationDialog(
            value = inputText,
            onValueChange = { inputText = it },
            onConfirm = {
                val text = inputText
                viewModel.addAffirmation(text)
                // Always close the dialog; snackbar will appear if it was a duplicate
                inputText = ""
                showDialog = false
            },
            onDismiss = {
                inputText = ""
                showDialog = false
            }
        )
    }
}

@Composable
private fun AffirmationItem(text: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddAffirmationDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Affirmation") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("e.g. I am focused and unstoppable") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationidentityidentityviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityViewModel.kt`
- **Lines of Code:** 57
- **Size:** 2,252 bytes (2.2 KB)

```kotlin
package com.neuroflow.app.presentation.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val preferences = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.neuroflow.app.data.local.UserPreferences())

    val affirmations = preferencesDataStore.preferencesFlow
        .map { it.affirmations }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emits true when a duplicate was rejected
    private val _duplicateEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val duplicateEvent = _duplicateEvent.asSharedFlow()

    fun addAffirmation(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            var isDuplicate = false
            preferencesDataStore.updatePreferences { prefs ->
                if (prefs.affirmations.any { it.equals(trimmed, ignoreCase = true) }) {
                    isDuplicate = true
                    return@updatePreferences prefs
                }
                prefs.copy(affirmations = prefs.affirmations + trimmed)
            }
            if (isDuplicate) _duplicateEvent.tryEmit(Unit)
        }
    }

    fun removeAffirmation(index: Int) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                if (index < 0 || index >= prefs.affirmations.size) return@updatePreferences prefs
                prefs.copy(affirmations = prefs.affirmations.toMutableList().also { it.removeAt(index) })
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationlogtimelogtimescreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeScreen.kt`
- **Lines of Code:** 102
- **Size:** 3,844 bytes (3.8 KB)

```kotlin
package com.neuroflow.app.presentation.logtime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.presentation.common.ManualTimeLogSheet

/**
 * Standalone screen accessible from the drawer.
 * Lists all active tasks; tapping one opens [ManualTimeLogSheet] to log time
 * without completing the task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTimeScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogTimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var loggedTaskIds by remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Time") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.allActiveTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active tasks to log time for.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.allActiveTasks, key = { it.id }) { task ->
                    val justLogged = task.id in loggedTaskIds
                    ListItem(
                        headlineContent = {
                            Text(task.title, fontWeight = FontWeight.Medium)
                        },
                        supportingContent = {
                            val est = task.estimatedDurationMinutes
                            val hint = if (est > 0) "Est. ${est}m" else "No estimate"
                            Text(if (justLogged) "✓ Time logged" else hint)
                        },
                        trailingContent = {
                            TextButton(onClick = { selectedTask = task }) {
                                Text(if (justLogged) "Log more" else "Log")
                            }
                        },
                        modifier = Modifier.clickable { selectedTask = task }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    selectedTask?.let { task ->
        ManualTimeLogSheet(
            taskTitle = task.title,
            prefillMinutes = task.estimatedDurationMinutes,
            onConfirm = { minutes ->
                viewModel.logTimeOnly(task, minutes)
                loggedTaskIds = loggedTaskIds + task.id
                selectedTask = null
            },
            onSkip = { selectedTask = null }
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationlogtimelogtimeviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeViewModel.kt`
- **Lines of Code:** 60
- **Size:** 2,266 bytes (2.2 KB)

```kotlin
package com.neuroflow.app.presentation.logtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogTimeUiState(
    val allActiveTasks: List<TaskEntity> = emptyList()
)

@HiltViewModel
class LogTimeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<LogTimeUiState> = taskRepository.observeActiveTasks()
        .map { LogTimeUiState(allActiveTasks = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogTimeUiState()
        )

    fun logTimeOnly(task: TaskEntity, manualMinutes: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (manualMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = manualMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository.insert(session)
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + manualMinutes,
                    sessionCount = task.sessionCount + 1,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + manualMinutes.toInt())
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationnotesnotecolorskt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/notes/NoteColors.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/notes/NoteColors.kt`
- **Lines of Code:** 74
- **Size:** 2,057 bytes (2.0 KB)

```kotlin
package com.neuroflow.app.presentation.notes

import androidx.compose.ui.graphics.Color

data class NoteColorItem(
    val id: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)

object NoteColors {
    val items = listOf(
        NoteColorItem(
            id = "DEFAULT",
            name = "Default",
            lightColor = Color(0xFFF7F7F9),
            darkColor = Color(0xFF1E1E24)
        ),
        NoteColorItem(
            id = "BUTTER",
            name = "Butter",
            lightColor = Color(0xFFFFF9C4),
            darkColor = Color(0xFF383318)
        ),
        NoteColorItem(
            id = "PEACH",
            name = "Peach",
            lightColor = Color(0xFFFFE0B2),
            darkColor = Color(0xFF3A2818)
        ),
        NoteColorItem(
            id = "CORAL",
            name = "Coral",
            lightColor = Color(0xFFFFCDD2),
            darkColor = Color(0xFF3B1E22)
        ),
        NoteColorItem(
            id = "MINT",
            name = "Mint",
            lightColor = Color(0xFFC8E6C9),
            darkColor = Color(0xFF18331E)
        ),
        NoteColorItem(
            id = "TEAL",
            name = "Teal",
            lightColor = Color(0xFFB2DFDB),
            darkColor = Color(0xFF143230)
        ),
        NoteColorItem(
            id = "SKY",
            name = "Sky",
            lightColor = Color(0xFFBBDEFB),
            darkColor = Color(0xFF182A3E)
        ),
        NoteColorItem(
            id = "LAVENDER",
            name = "Lavender",
            lightColor = Color(0xFFE1BEE7),
            darkColor = Color(0xFF32193A)
        ),
        NoteColorItem(
            id = "ROSE",
            name = "Rose",
            lightColor = Color(0xFFF8BBD0),
            darkColor = Color(0xFF381928)
        )
    )

    fun getColor(colorId: String?, isDark: Boolean): Color {
        val found = items.find { it.id.equals(colorId, ignoreCase = true) } ?: items.first()
        return if (isDark) found.darkColor else found.lightColor
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationnotesnoteeditorscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/notes/NoteEditorScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/notes/NoteEditorScreen.kt`
- **Lines of Code:** 264
- **Size:** 11,211 bytes (10.9 KB)

```kotlin
package com.neuroflow.app.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, colorId: String, isPinned: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var selectedColorId by remember { mutableStateOf(note.colorHex.ifBlank { "DEFAULT" }) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    val currentBgColor by animateColorAsState(
        targetValue = NoteColors.getColor(selectedColorId, isDark),
        animationSpec = tween(250),
        label = "bgColorAnim"
    )

    fun doSave() {
        onSave(title, content, selectedColorId, isPinned)
    }

    BackHandler {
        doSave()
    }

    Scaffold(
        containerColor = currentBgColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        doSave()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and Back"
                        )
                    }
                },
                actions = {
                    // Pin toggle
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete button (if not brand new unsaved note)
                    if (onDelete != null && (note.title.isNotBlank() || note.content.isNotBlank())) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = currentBgColor,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Color palette circles
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NoteColors.items) { item ->
                            val itemColor = if (isDark) item.darkColor else item.lightColor
                            val isSelected = item.id.equals(selectedColorId, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(itemColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorId = item.id },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = item.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date information timestamp
                    val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                    val createdTime = dateFormat.format(Date(note.createdAt))
                    val editedTime = dateFormat.format(Date(note.updatedAt))
                    val dateLabel = if (note.createdAt == note.updatedAt) {
                        "Created $createdTime"
                    } else {
                        "Created $createdTime • Edited $editedTime"
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Note Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Note Content
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Start typing your note...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            lineHeight = 24.sp
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 240.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete note?") },
            text = { Text("This will permanently delete this note. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationnotesnotesscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/notes/NotesScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/notes/NotesScreen.kt`
- **Lines of Code:** 278
- **Size:** 12,849 bytes (12.5 KB)

```kotlin
package com.neuroflow.app.presentation.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    if (uiState.isEditing && uiState.editingNote != null) {
        NoteEditorScreen(
            note = uiState.editingNote!!,
            onDismiss = { viewModel.closeEditor() },
            onSave = { title, content, colorId, isPinned ->
                viewModel.saveNote(title, content, colorId, isPinned)
            },
            onDelete = {
                viewModel.deleteNote(uiState.editingNote!!)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search notes...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Notes",
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.totalNoteCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        "${uiState.totalNoteCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search Notes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openNewNote() },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Note") },
                text = { Text("New Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        if (uiState.notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "No notes match '${uiState.searchQuery}'"
                        else "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "Try searching for different keywords"
                        else "Tap '+ New Note' to capture your ideas, thoughts, and reflections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val pinnedNotes = remember(uiState.notes) { uiState.notes.filter { it.isPinned } }
            val otherNotes = remember(uiState.notes) { uiState.notes.filterNot { it.isPinned } }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            "PINNED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isDark = isDark,
                            onClick = { viewModel.openEditNote(note) },
                            onExtractTask = { viewModel.createTaskFromNote(note) }
                        )
                    }
                }

                if (otherNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                "OTHERS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isDark = isDark,
                            onClick = { viewModel.openEditNote(note) },
                            onExtractTask = { viewModel.createTaskFromNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    isDark: Boolean,
    onClick: () -> Unit,
    onExtractTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawCardBg = NoteColors.getColor(note.colorHex, isDark)
    val accentColor = if (note.colorHex == "DEFAULT" || isDark) {
        com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2.ReboundCyan
    } else {
        rawCardBg
    }
    val formattedDate = remember(note.updatedAt) {
        val nowCal = java.util.Calendar.getInstance()
        val noteCal = java.util.Calendar.getInstance().apply { timeInMillis = note.updatedAt }
        val isSameDay = nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == noteCal.get(java.util.Calendar.DAY_OF_YEAR)
        val isYesterday = nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) - noteCal.get(java.util.Calendar.DAY_OF_YEAR) == 1
        when {
            isSameDay -> "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
            isYesterday -> "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
            nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) ->
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updatedAt))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
        }
    }

    com.neuroflow.app.presentation.v2.common.UpgradedNoteCardV2(
        title = note.title,
        contentPreview = note.content,
        noteAccentColor = accentColor,
        updatedAtFormatted = formattedDate,
        onNoteClick = onClick,
        onExtractTaskWithAiClick = onExtractTask,
        modifier = modifier
    )
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationnotesnotesviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt`
- **Lines of Code:** 144
- **Size:** 4,497 bytes (4.4 KB)

```kotlin
package com.neuroflow.app.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val editingNote: NoteEntity? = null,
    val isEditing: Boolean = false,
    val totalNoteCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    val uiState: StateFlow<NotesUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            noteRepository.searchNotes(query)
        },
        _searchQuery,
        _editingNote,
        _isEditing,
        noteRepository.noteCount
    ) { notes, query, editing, isEdit, count ->
        NotesUiState(
            notes = notes,
            searchQuery = query,
            editingNote = editing,
            isEditing = isEdit,
            totalNoteCount = count
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openNewNote() {
        _editingNote.value = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "",
            content = "",
            colorHex = "DEFAULT",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _isEditing.value = true
    }

    fun openEditNote(note: NoteEntity) {
        _editingNote.value = note
        _isEditing.value = true
    }

    fun closeEditor() {
        _editingNote.value = null
        _isEditing.value = false
    }

    fun saveNote(title: String, content: String, colorHex: String, isPinned: Boolean) {
        val current = _editingNote.value ?: return
        if (title.isBlank() && content.isBlank()) {
            // Don't save blank notes
            closeEditor()
            return
        }
        viewModelScope.launch {
            val updated = current.copy(
                title = title.trim(),
                content = content.trim(),
                colorHex = colorHex,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.saveNote(updated)
            closeEditor()
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.togglePin(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            if (_editingNote.value?.id == note.id) {
                closeEditor()
            }
        }
    }

    fun updateColor(note: NoteEntity, colorHex: String) {
        viewModelScope.launch {
            noteRepository.updateColor(note, colorHex)
        }
    }

    fun createTaskFromNote(note: NoteEntity) {
        viewModelScope.launch {
            val taskTitle = note.title.ifBlank { note.content.take(40).trim() }.ifBlank { "Task from Note" }
            val taskDescription = note.content.trim()
            val newTask = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = taskTitle,
                description = taskDescription,
                estimatedDurationMinutes = 25,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.insert(newTask)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationonboardingappguidescreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/onboarding/AppGuideScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/onboarding/AppGuideScreen.kt`
- **Lines of Code:** 790
- **Size:** 45,867 bytes (44.8 KB)

```kotlin
package com.neuroflow.app.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

enum class GuideCategory(val label: String, val emoji: String) {
    ALL("All Topics", "🌐"),
    QUICK_START("Quick Start", "🚀"),
    CIRCADIAN_FLOW("Flow & Circadian", "⚡"),
    PHARMACOKINETICS("Stimulants & PK", "☕"),
    SCHEDULING_PLAN("Plan & Calendar", "🤖"),
    EXECUTIVE_ADHD("ADHD & Executive", "🧠"),
    FOCUS_DSP("Focus & Audio DSP", "🎯"),
    AI_NLP("AI & Smart Tasks", "🪄"),
    VAULT_CHRONO("Vault & Chronotype", "🗃️")
}

data class GuideSectionItem(
    val id: String,
    val category: GuideCategory,
    val emoji: String,
    val tag: String,
    val title: String,
    val summary: String,
    val accentColor: Color,
    val keyPoints: List<Pair<String, String>>,
    val proTip: String? = null,
    val scienceNote: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGuideScreen(onNavigateBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GuideCategory.ALL) }
    var expandedIds by remember { mutableStateOf(setOf("quick_start")) }

    val guideItems = remember {
        listOf(
            GuideSectionItem(
                id = "quick_start",
                category = GuideCategory.QUICK_START,
                emoji = "🚀",
                tag = "3-MINUTE PROTOCOL",
                title = "Quick Start: Zero-Friction Setup",
                summary = "The essential daily activation sequence to get productive in under 3 minutes.",
                accentColor = Color(0xFF6366F1),
                keyPoints = listOf(
                    "1. The Rule of 3-5 Tasks" to "Limit your active queue to 3–5 items maximum. Overflowing backlogs trigger executive overwhelm and freeze responses. Park all other ideas in the Vault Icebox until today's queue is cleared.",
                    "2. Mark Today's 🐸 Frog" to "Select one high-impact, high-resistance task as your daily Frog. Completing it early capitalizes on peak morning cognitive bandwidth before decision fatigue accumulates.",
                    "3. Read Biological Weather" to "Glance at the Circadian Hero Barometer on the Flow Cockpit. If you're in a Peak window, dive straight into deep work. If in a Dip, switch to low-friction organizing or admin.",
                    "4. Single-Task Sprint" to "Launch Focus Mode for 25–45 minutes with procedural background noise. The display is kept awake automatically so your micro-steps and timer remain in sight.",
                    "5. Celebrate, Log & Decompress" to "Mark the task done or record offline minutes. Rest for 5–15 minutes before switching to the next item to allow neurochemical replenishment."
                ),
                proTip = "If a task triggers avoidance or dread, commit to working for just 5 minutes, or tap 'Break Down' to generate a 2-minute doorway step.",
                scienceNote = "Overcoming the activation energy barrier (E_a) of the first micro-step is the single most effective intervention for executive dysfunction."
            ),
            GuideSectionItem(
                id = "circadian_cockpit",
                category = GuideCategory.CIRCADIAN_FLOW,
                emoji = "⚡",
                tag = "BORBÉLY TWO-PROCESS MODEL",
                title = "Flow Cockpit & Circadian Energy Barometer",
                summary = "Lab-grade biophysical energy modeling combining homeostatic sleep pressure and double-harmonic circadian rhythm.",
                accentColor = Color(0xFF10B981),
                keyPoints = listOf(
                    "Process S (Sleep Pressure)" to "Tracks adenosine accumulation from waking (S_0 -> 1.0). Discharges logarithmically during nocturnal sleep and daytime power naps (S_nap).",
                    "Process C (Circadian Wave)" to "Biological double-harmonic alertness wave (φ = t_wake + 4.5h) modeling your natural morning peak and afternoon post-prandial dip.",
                    "⚡ PEAK (75–100%)" to "Maximum synaptic plasticity. Strictly reserved for deep analytical thinking, strategic architecture, and creative breakthroughs.",
                    "📈 REBOUND (55–74%)" to "Secondary alertness wave in the late afternoon. Optimal for collaborative execution, meetings, and tactical progress.",
                    "☕ DIP (35–54%)" to "Biological circadian trough (13:30–15:30). Switch to light admin, organization, or take a 15–20 minute restorative power nap.",
                    "🧘 RECOVERY NEEDED (<35%)" to "Neural exhaustion detected. Take a 15m NSDR (Non-Sleep Deep Rest) session or a walk outside before resuming high-stakes work.",
                    "🌙 WIND-DOWN (<20% or >21:30)" to "Melatonin secretion rising. Execute Evening Shutdown to close open loops, archive completed wins, and queue tomorrow's frog.",
                    "Fast Kalman Circadian Learner" to "The app passively observes your actual completion timestamps to refine your personalized peak hours over time without manual quizzes."
                ),
                proTip = "Never schedule high-friction analytical tasks during your Dip (13:30–15:30). Protecting this window prevents chronic burnout.",
                scienceNote = "Based on Borbély's Two-Process Model of Sleep Regulation. Continuous non-threshold modeling eliminates artificial hourly drop-offs."
            ),
            GuideSectionItem(
                id = "stimulant_pk",
                category = GuideCategory.PHARMACOKINETICS,
                emoji = "☕",
                tag = "BATEMAN TWO-COMPARTMENT PK",
                title = "Stimulant Pharmacokinetics & Crash Guard",
                summary = "Two-compartment differential equation modeling for caffeine and prescription ADHD medications with crash warnings.",
                accentColor = Color(0xFFF59E0B),
                keyPoints = listOf(
                    "Bateman Differential Model" to "Computes absorption rate (k_a) and elimination half-life (t_half) to track real-time active plasma concentration and adenosine masking over 24 hours.",
                    "+ ☕ Coffee (100mg)" to "Standard caffeine pharmacokinetic curve (t_half = 5.0h, k_a = 2.1). Reaches peak alertness at ~45 minutes.",
                    "+ 🍵 Green Tea (50mg)" to "L-Theanine modulated caffeine (t_half = 3.5h, k_a = 1.6). Promotes relaxed alertness with smoother onset and reduced jitter.",
                    "+ 💊 Meds IR (10mg)" to "Rapid-acting prescription stimulant (t_half = 4.0h, k_a = 2.5). Sharp alertness rise with pronounced wear-off boundary.",
                    "+ 💊 Meds XR (20mg)" to "Extended-release dual-bead kinetic curve (t_half = 7.0h, k_a = 1.2). Sustained therapeutic plasma level across 8–10 hours.",
                    "Live Plasma & 4-Second Undo" to "Displays active plasma concentration (~X mg active in plasma) on the Cockpit. A 4-second non-destructive Undo snackbar and [Clear doses] affordance ensure effortless error correction.",
                    "⚡ Rebound Crash Guard" to "When wear-off and adenosine rebound coincide, an amber banner warns you against tackling high-friction tasks during neurochemical depletion.",
                    "Protected Slot Keyword Auto-Detection" to "Calendar slots labeled 'Coffee', 'Caffeine', 'Meds', 'Vyvanse', or 'Adderall' are automatically converted into pharmacokinetic bouts."
                ),
                proTip = "Avoid high-dose stimulants after 14:00 to prevent Slow-Wave Sleep (SWS) suppression, even if you feel able to fall asleep on schedule.",
                scienceNote = "Adenosine continues accumulating in the brain while caffeine blocks receptor sites. When caffeine detaches, rebound drowsiness occurs naturally."
            ),
            GuideSectionItem(
                id = "social_jetlag",
                category = GuideCategory.CIRCADIAN_FLOW,
                emoji = "🌅",
                tag = "CHRONOBIOLOGY & TELEMETRY",
                title = "Social Jetlag & Wearable Sleep Telemetry",
                summary = "Detecting weekend sleep schedule drift and normalizing wearable sleep metrics for circadian precision.",
                accentColor = Color(0xFF818CF8),
                keyPoints = listOf(
                    "Social Jetlag (Δφ_SJL)" to "Measures the circular divergence between workday sleep midpoint (MSW) and weekend sleep midpoint (MSF).",
                    "🌅 Weekend Drift Badge" to "When weekend sleep drift exceeds 1.0 hour, the Cockpit displays an amber Weekend Drift alert recommending 10–15 minutes of outdoor sunlight exposure upon waking.",
                    "Stage-Weighted Telemetry" to "Processes wearable sleep logs with lab-grade stage efficiency (d_eff = 1.15 Deep + 1.05 REM + 0.85 Light) and automatically coalesces fragmented wake episodes (WASO <= 45m).",
                    "Daytime Power Naps" to "Daytime naps between 10m and 180m discharge homeostatic pressure (ΔS) with an initial 20m sleep inertia envelope without corrupting nocturnal circadian anchors."
                ),
                proTip = "Morning sunlight within 30 minutes of waking triggers SCN photopigment melanopsin, resetting your master circadian clock and accelerating evening melatonin onset.",
                scienceNote = "A 2-hour weekend sleep midpoint delay produces biological jetlag equivalent to flying across two time zones every single Monday."
            ),
            GuideSectionItem(
                id = "math_scheduling",
                category = GuideCategory.SCHEDULING_PLAN,
                emoji = "🤖",
                tag = "4-VECTOR OPTIMIZATION",
                title = "Mathematical Auto-Scheduling & Calendar Sync",
                summary = "Non-saturating algorithmic schedule generation with two-way Google Calendar synchronization and ultradian buffers.",
                accentColor = Color(0xFF38BDF8),
                keyPoints = listOf(
                    "4-Vector Task Scoring Engine" to "Prioritization Formula: Score = 100 × [0.30 U + 0.30 I + 0.25 E + 0.15 M], balancing Urgency (U), Impact (I), Circadian Energy Match (E), and Momentum (M).",
                    "Demand-Elastic Utility Damping λ(τ)" to "Logarithmically scales utility with duration, preventing huge 3-hour projects from starving shorter high-leverage tasks.",
                    "Calendar Sync & Decompression Buffers" to "Two-way sync via Android CalendarContract. External meetings are imported as busy windows, and meetings ≥ 45m automatically receive a 5-minute cognitive decompression buffer.",
                    "Ultradian Auto-Chunking" to "Tasks ≥ 60m placed into fragmented schedule slots are automatically split into linked sub-tasks ('🔗 Part 1/2' & '🔗 Part 2/2') chained by DAG dependencies.",
                    "🔒 Schedule Locking" to "Tap 'Lock Schedule' on any task to pin it to an exact time slot. Auto-scheduling will route all other tasks around locked anchors.",
                    "Protected Recurring Slots" to "Configure non-negotiable blocks for meals, gym, or family time in Settings that are guaranteed never to be booked over."
                ),
                proTip = "Tap 'Auto Schedule Day' in the Plan hub each morning. The engine will place your deep work during biological peaks and admin during dips.",
                scienceNote = "Ultradian rhythm research shows human focus peaks in 90-minute waves, after which synaptic efficiency declines rapidly without restorative rest."
            ),
            GuideSectionItem(
                id = "adhd_executive",
                category = GuideCategory.EXECUTIVE_ADHD,
                emoji = "🧠",
                tag = "NEURODIVERGENT ERGONOMICS",
                title = "ADHD Executive Hardening & Behavioral Science",
                summary = "Science-backed tools to defeat task paralysis, time blindness, dread resistance, and hyperbolic discounting.",
                accentColor = Color(0xFF8B5CF6),
                keyPoints = listOf(
                    "Implementation Intentions (WOOP)" to "Formulate Wish, Outcome, Obstacle, and If-Then Plan before starting work. Research demonstrates this doubles execution follow-through rates.",
                    "Doorway Micro-Steps (≤ 4m)" to "Decomposes daunting tasks into a microscopic 2–4 minute first physical action (e.g. 'Open document and write title'), drastically lowering activation energy.",
                    "⚡ Dopamine Warm-Up Badges" to "When a task has been postponed ≥ 3 times, a Dopamine Warm-Up / Paired Primer badge appears, granting momentum bonuses upon starting.",
                    "Parkinson's Law Urgency Ignition" to "Non-linear priority scaling triggers when deadlines enter the [0.25h, 6.0h] window, overcoming ADHD time-blindness naturally.",
                    "Zeigarnik Completion Pull" to "Leverages psychological closure inertia with sinusoidal pull peaking at 60–80% sub-task progress: M_Zeigarnik(p) = 30p + 10 sin(πp).",
                    "Context-Switching Entropy Matrix" to "Awards +8.5 point bonuses for batching similar categories within 45m and penalizes jarring context jumps (-8.5 pts).",
                    "Zero-Guilt Rollover" to "When ≥ 3 tasks become overdue, the Cockpit provides a zero-shame rollover card to batch-reschedule or archive without guilt."
                ),
                proTip = "When feeling stuck, do not force yourself to complete the entire project. Complete only Step 1 (the Doorway Step). Action generates momentum.",
                scienceNote = "Hyperbolic discounting causes distant deadlines to be cognitively discounted. Nonlinear urgency ignition restores salience before crisis mode."
            ),
            GuideSectionItem(
                id = "focus_audio_dsp",
                category = GuideCategory.FOCUS_DSP,
                emoji = "🎯",
                tag = "DEEP WORK COCKPIT",
                title = "Focus Mode, Soundscapes & Ergonomics",
                summary = "Uninterrupted attention shielding with pure-Kotlin DSP soundscapes, display wake lock, and solfeggio chimes.",
                accentColor = Color(0xFF6366F1),
                keyPoints = listOf(
                    "Display Wake Lock (FLAG_KEEP_SCREEN_ON)" to "The display remains awake throughout active focus sprints, keeping timers and micro-steps visible without needing to touch the phone.",
                    "Pure Kotlin DSP Audio Synthesizer" to "Real-time streaming acoustic synthesis over AudioTrack (16-bit PCM, 44.1kHz stereo) with 400ms raised-cosine anti-click envelopes.",
                    "🌊 Pink & Brown Noise" to "Attenuates environmental distractions, speech chatter, and office noise for seamless focus immersion.",
                    "🧠 Alpha (10Hz) & Gamma (40Hz) Binaural Beats" to "Promotes neural entrainment and cognitive binding for sustained analytical depth.",
                    "🌧️ Procedural Rain & Forest Soundscapes" to "Natural stochastic acoustic masks designed to calm sympathetic nervous system hyperactivity.",
                    "Dual-Harmonic Solfeggio Chimes" to "Procedural 528Hz + 792Hz harmonic bell tone with double-pulse tactile vibration upon timer expiration.",
                    "Interruption Shield" to "Gentle 3-second breathing interstitial when attempting to leave early, protecting your hyperfocus state.",
                    "Manual Time Logging" to "Work performed away from your phone can be retroactively logged to maintain accurate XP, stats, and estimation calibration."
                ),
                proTip = "Pair Brown Noise with 40Hz Gamma binaural beats through stereo headphones for complex coding, math, or deep analytical writing.",
                scienceNote = "40Hz gamma entrainment stimulates auditory cortex resonance and has been linked to enhanced working memory and cognitive binding."
            ),
            GuideSectionItem(
                id = "smart_ai_nlp",
                category = GuideCategory.AI_NLP,
                emoji = "🪄",
                tag = "PRIVACY-FIRST AI & NLP",
                title = "Smart Task Creation & Gemini AI BYOK",
                summary = "Natural language task syntax parsing and offline-resilient AI task decomposition.",
                accentColor = Color(0xFF10B981),
                keyPoints = listOf(
                    "Natural Language Heuristic Parser" to "Type naturally: 'Draft budget tomorrow 3pm 45m #deepwork 🐸' — dates, times, durations, hashtags, and frog flags are extracted instantly.",
                    "Archetype-Driven Decomposition" to "Classifies tasks into 6 core archetypes (Technical, Writing, Admin, Creative, Research, Everyday) and builds structured micro-step hierarchies.",
                    "Pure BYOK (Bring Your Own Key)" to "Zero hardcoded keys or shared servers. Add your Gemini API key in Settings; it is encrypted locally in DataStore and never transmitted elsewhere.",
                    "Resilient Cascade & TTL Caching" to "Features an in-memory 20-minute cache and 60s backoff guard. If network fails or no key is configured, neuroFlow degrades seamlessly to local heuristic decomposition with zero errors.",
                    "Adaptive Behavioral Directives" to "Audits avoided or deleted tasks and synthesizes a concise coaching directive to refine future task decompositions."
                ),
                proTip = "Add hashtags like #deepwork, #offline, #admin, or #quickwin to automatically map tasks into dedicated energy zones and context profiles.",
                scienceNote = "Closed-loop meta-prompting injects persistent coaching directives into task enrichment prompts to maintain alignment with minimal token footprint."
            ),
            GuideSectionItem(
                id = "vault_chronotype",
                category = GuideCategory.VAULT_CHRONO,
                emoji = "🗃️",
                tag = "KNOWLEDGE & CALIBRATION",
                title = "The Vault, Scratchpad & Chronotype Profiles",
                summary = "Icebox backlog, quick thought capture, and circadian chronotype tuning.",
                accentColor = Color(0xFFF59E0B),
                keyPoints = listOf(
                    "Quick Scratchpad Notes" to "Long-press the floating action button (+) or tap the Notes icon in the top bar to record thoughts instantly without breaking focus.",
                    "The Icebox (Zero-Distraction Backlog)" to "Park non-urgent ideas and someday/maybe tasks. Keeps your active Cockpit uncluttered until you are ready to pull them into Plan.",
                    "MEQ Chronotype Assessment" to "Complete the validated Morningness-Eveningness Questionnaire (MEQ) to calibrate your biological phase anchors (Morning Lark, Intermediate, Night Owl).",
                    "Manual Peak Energy Tuning" to "Prefer manual control? Enable manual peak anchor in Settings to lock your preferred cognitive focus hours.",
                    "Evening Shutdown Ritual" to "Review completed tasks, log remaining thoughts into scratchpad, and trigger shutdown to transition smoothly into wind-down."
                ),
                proTip = "Review your Vault scratchpad at the end of the day during Evening Shutdown to convert fleeting notes into structured tasks.",
                scienceNote = "Externalizing open loops into a trusted capture system reduces cognitive tension and prevents bedtime rumination."
            )
        )
    }

    val filteredItems = remember(searchQuery, selectedCategory, guideItems) {
        guideItems.filter { item ->
            val matchesCategory = selectedCategory == GuideCategory.ALL || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.tag.contains(searchQuery, ignoreCase = true) ||
                item.summary.contains(searchQuery, ignoreCase = true) ||
                item.keyPoints.any { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "neuroFlow V2 Manual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                        Text(
                            text = "Biological High Performance & Executive Guide",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuroFlowPaletteV2.SurfaceCard,
                    titleContentColor = NeuroFlowPaletteV2.TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Modern Hero Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = NeuroFlowPaletteV2.SurfaceCard,
                border = BorderStroke(
                    1.dp,
                    NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowThemeV2Colors.ElectricIndigo, 0.85f)
                ),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧬", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "System Architecture & Biophysics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aligning task execution with Borbély circadian oscillations, Bateman stimulant pharmacokinetics, and neurodivergent executive ergonomics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(
                        "Search algorithms, biophysics, features...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeuroFlowPaletteV2.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = NeuroFlowPaletteV2.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeuroFlowPaletteV2.SurfaceCard,
                    unfocusedContainerColor = NeuroFlowPaletteV2.SurfaceCard,
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    unfocusedBorderColor = NeuroFlowPaletteV2.BorderSubtle,
                    focusedTextColor = NeuroFlowPaletteV2.TextPrimary,
                    unfocusedTextColor = NeuroFlowPaletteV2.TextPrimary
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuideCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f) else NeuroFlowPaletteV2.SurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo else NeuroFlowPaletteV2.BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(category.emoji, fontSize = 13.sp)
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeuroFlowPaletteV2.TextPrimary else NeuroFlowPaletteV2.TextSecondary
                            )
                        }
                    }
                }
            }

            // Results count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredItems.size} Module${if (filteredItems.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )

                if (expandedIds.size < filteredItems.size) {
                    TextButton(
                        onClick = { expandedIds = filteredItems.map { it.id }.toSet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Expand All",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            fontSize = 12.sp
                        )
                    }
                } else if (expandedIds.isNotEmpty()) {
                    TextButton(
                        onClick = { expandedIds = emptySet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Collapse All",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Empty search state
            if (filteredItems.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowPaletteV2.SurfaceCard,
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching modules found",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeuroFlowPaletteV2.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or switching categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                }
            }

            // Guide Module Cards
            filteredItems.forEach { item ->
                val isExpanded = expandedIds.contains(item.id)
                ModernGuideCard(
                    item = item,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedIds = if (isExpanded) {
                            expandedIds - item.id
                        } else {
                            expandedIds + item.id
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Mindset Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NeuroFlowPaletteV2.SurfaceCard,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "The Core Operating Law",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Consistency beats intensity. One focused sprint in biological sync per day creates compounding neurological momentum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModernGuideCard(
    item: GuideSectionItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_chevron_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        border = BorderStroke(
            1.dp,
            if (isExpanded) {
                NeuroFlowPaletteV2.specularBorderBrush(item.accentColor, 0.75f)
            } else {
                Brush.verticalGradient(listOf(NeuroFlowPaletteV2.BorderSubtle, NeuroFlowPaletteV2.BorderSubtle))
            }
        ),
        tonalElevation = if (isExpanded) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = item.accentColor,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isExpanded) item.accentColor else NeuroFlowPaletteV2.TextTertiary,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                lineHeight = 18.sp
            )

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        color = item.accentColor.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Key Points
                    item.keyPoints.forEachIndexed { index, (pointTitle, pointDesc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(item.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = item.accentColor
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pointTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowPaletteV2.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pointDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Pro-Tip Callout
                    item.proTip?.let { tip ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = item.accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("💡", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRO-TIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = item.accentColor,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeuroFlowPaletteV2.TextPrimary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Science / Mathematical Callout
                    item.scienceNote?.let { science ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔬", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BIOPHYSICAL BASIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeuroFlowPaletteV2.TextTertiary,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = science,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeuroFlowPaletteV2.TextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationonboardingonboardingscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/onboarding/OnboardingScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/onboarding/OnboardingScreen.kt`
- **Lines of Code:** 332
- **Size:** 13,411 bytes (13.1 KB)

```kotlin
package com.neuroflow.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

data class OnboardingData(
    val wakeUpHour: Int = 7,
    val sleepHour: Int = 23,
    val manualChronotype: String = "MODERATE_MORNING",
    val firstTask: String = "",
    val identityLabel: String = "Focused Professional",
    val topGoal: String = ""
)

@Composable
fun OnboardingScreen(
    onComplete: (OnboardingData) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf(OnboardingData()) }
    val totalSteps = 3

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                repeat(totalSteps) { index ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(if (index == currentStep) 12.dp else 8.dp)
                    ) {}
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> WakeSleepTimeStep(
                        wakeHour = data.wakeUpHour,
                        sleepHour = data.sleepHour,
                        onWakeHourChange = { data = data.copy(wakeUpHour = it) },
                        onSleepHourChange = { data = data.copy(sleepHour = it) }
                    )
                    1 -> ChronotypeDiscoveryStep(
                        selectedChronotype = data.manualChronotype,
                        onSelect = { data = data.copy(manualChronotype = it) }
                    )
                    2 -> FirstTaskStep(
                        value = data.firstTask,
                        onValueChange = { data = data.copy(firstTask = it) }
                    )
                }
            }

            // Navigation buttons (>= 48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Back", fontSize = 16.sp)
                    }
                } else {
                    TextButton(
                        onClick = { onComplete(data) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Skip", fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onComplete(data)
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .semantics { role = Role.Button },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (currentStep == totalSteps - 1) "Enter Flow" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeSleepTimeStep(
    wakeHour: Int,
    sleepHour: Int,
    onWakeHourChange: (Int) -> Unit,
    onSleepHourChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Circadian Anchors ⏰",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "When do you typically wake and sleep? This calibrates your Borbély Two-Process energy and sleep pressure model.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Wake Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌅 Wake Up Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatHour(wakeHour),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = wakeHour.toFloat(),
                    onValueChange = { onWakeHourChange(it.toInt()) },
                    valueRange = 4f..11f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sleep Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌙 Sleep Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatHour(sleepHour),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sleepHour.toFloat(),
                    onValueChange = { onSleepHourChange(it.toInt()) },
                    valueRange = 20f..26f, // 20:00 (8 PM) to 26:00 (2 AM)
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ChronotypeDiscoveryStep(
    selectedChronotype: String,
    onSelect: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Chronotype Discovery 🧬",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select when your cognitive focus naturally peaks. We'll protect high-demand tasks during this biological window.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        listOf(
            Triple("🌅 Morning Lark", "MODERATE_MORNING", "Peak focus: 9 AM – 12 PM · Optimal for analytical sprints"),
            Triple("☀️ Intermediate Bear", "INTERMEDIATE", "Peak focus: 12 PM – 5 PM · Steady afternoon cognitive stamina"),
            Triple("🌙 Night Owl", "MODERATE_EVENING", "Peak focus: 5 PM – 10 PM · Peak creative flow in late evening")
        ).forEach { (label, value, subtitle) ->
            val isSelected = selectedChronotype == value
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                onClick = { onSelect(value) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstTaskStep(value: String, onValueChange: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Immediate Next Action 🐸",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Eliminate startup friction right now. What is the single highest-leverage task or frog you want to conquer first?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("First Action / Frog") },
            placeholder = { Text("e.g. Finish quarterly project proposal 45m #work") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "💡 Pro tip: Add duration (e.g. 45m) and tags (@work) — our smart engine will auto-schedule it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatHour(hour: Int): String {
    val h = if (hour >= 24) hour - 24 else hour
    return when {
        h == 0 -> "12:00 AM"
        h < 12 -> "$h:00 AM"
        h == 12 -> "12:00 PM"
        else -> "${h - 12}:00 PM"
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingsaccountsyncscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncScreen.kt`
- **Lines of Code:** 412
- **Size:** 17,119 bytes (16.7 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.neuroflow.app.data.sync.BackupMeta
import com.neuroflow.app.data.sync.BackupStatus
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.RestoreStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountSyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val restoreStatus by viewModel.restoreStatus.collectAsStateWithLifecycle()
    val availableBackups by viewModel.availableBackups.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result)
    }

    fun launchGoogleSignIn() {
        val activity = context.findActivity() ?: return
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.neuroflow.app.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        client.signOut().addOnCompleteListener {
            signInLauncher.launch(client.signInIntent)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Cloud Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val state = authState) {
                is GoogleAuthState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is GoogleAuthState.SignedOut, is GoogleAuthState.Error -> {
                    item { SignedOutCard(
                        error = (state as? GoogleAuthState.Error)?.message,
                        onSignIn = { launchGoogleSignIn() }
                    ) }
                }

                is GoogleAuthState.SignedIn -> {
                    item { ProfileCard(state = state, onSignOut = { viewModel.signOut() }) }

                    item {
                        BackupRestoreCard(
                            backupStatus = backupStatus,
                            restoreStatus = restoreStatus,
                            onBackupNow = { viewModel.backupNow() },
                            onRestoreLatest = { viewModel.restoreLatest() },
                            onClearBackupStatus = { viewModel.clearBackupStatus() },
                            onClearRestoreStatus = { viewModel.clearRestoreStatus() }
                        )
                    }

                    item {
                        AutoSyncToggleCard(
                            enabled = autoSyncEnabled,
                            onToggle = { viewModel.setAutoSync(it) }
                        )
                    }

                    if (availableBackups.isNotEmpty()) {
                        item {
                            Text(
                                "Backup History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(availableBackups) { meta ->
                            BackupHistoryItem(
                                meta = meta,
                                onRestore = { viewModel.restoreFromBackup(meta.backupId) },
                                onDelete = { viewModel.deleteBackup(meta.backupId) }
                            )
                        }
                    }


                    item {
                        Text(
                            "Your data is stored privately in your Google account and is never shared. Backups include tasks, goals, sessions, sleep logs, WOOP data, energy predictions, and settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedOutCard(error: String?, onSignIn: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Cloud,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Cloud Backup & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with Google to back up your tasks, goals, sessions, and settings to your private cloud storage. Access your data on any device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google")
            }
        }
    }
}

@Composable
private fun ProfileCard(state: GoogleAuthState.SignedIn, onSignOut: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.photoUrl != null) {
                AsyncImage(
                    model = state.photoUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(state.displayName, fontWeight = FontWeight.SemiBold)
                Text(state.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Composable
private fun BackupRestoreCard(
    backupStatus: BackupStatus,
    restoreStatus: RestoreStatus,
    onBackupNow: () -> Unit,
    onRestoreLatest: () -> Unit,
    onClearBackupStatus: () -> Unit,
    onClearRestoreStatus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            // Backup status banner
            when (backupStatus) {
                is BackupStatus.InProgress -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is BackupStatus.Success -> StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    message = "Backup saved • ${formatTimestamp(backupStatus.timestamp)}",
                    isError = false,
                    onDismiss = onClearBackupStatus
                )
                is BackupStatus.Error -> StatusBanner(
                    icon = Icons.Filled.Error,
                    message = backupStatus.message,
                    isError = true,
                    onDismiss = onClearBackupStatus
                )
                else -> Unit
            }

            // Restore status banner
            when (restoreStatus) {
                is RestoreStatus.InProgress -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is RestoreStatus.Success -> StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    message = "Restored ${restoreStatus.itemCount} items • ${formatTimestamp(restoreStatus.timestamp)}",
                    isError = false,
                    onDismiss = onClearRestoreStatus
                )
                is RestoreStatus.Error -> StatusBanner(
                    icon = Icons.Filled.Error,
                    message = restoreStatus.message,
                    isError = true,
                    onDismiss = onClearRestoreStatus
                )
                else -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBackupNow,
                    enabled = backupStatus !is BackupStatus.InProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back up now")
                }
                OutlinedButton(
                    onClick = onRestoreLatest,
                    enabled = restoreStatus !is RestoreStatus.InProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
}

@Composable
private fun AutoSyncToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(

        onClick = { onToggle(!enabled) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-sync", fontWeight = FontWeight.Medium)
                Text("Back up automatically once a day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun BackupHistoryItem(
    meta: BackupMeta,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Backup") },
            text = { Text("Are you sure you want to delete the backup from ${formatTimestamp(meta.createdAt)}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTimestamp(meta.createdAt), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${meta.taskCount} tasks · ${meta.goalCount} goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRestore) { Text("Restore") }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete backup",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


@Composable
private fun StatusBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, isError: Boolean, onDismiss: () -> Unit) {
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = containerColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = contentColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = contentColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingsaccountsyncviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncViewModel.kt`
- **Lines of Code:** 168
- **Size:** 6,823 bytes (6.7 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.neuroflow.app.R
import com.neuroflow.app.data.sync.BackupMeta
import com.neuroflow.app.data.sync.BackupStatus
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.GoogleSignInManager
import com.neuroflow.app.data.sync.RestoreStatus
import com.neuroflow.app.worker.CloudSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signInManager: GoogleSignInManager,
    private val cloudBackupManager: CloudBackupManager,
    private val workManager: WorkManager
) : ViewModel() {

    private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Loading)
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _restoreStatus = MutableStateFlow<RestoreStatus>(RestoreStatus.Idle)
    val restoreStatus: StateFlow<RestoreStatus> = _restoreStatus.asStateFlow()

    private val _availableBackups = MutableStateFlow<List<BackupMeta>>(emptyList())
    val availableBackups: StateFlow<List<BackupMeta>> = _availableBackups.asStateFlow()

    private val syncPrefs by lazy {
        context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE)
    }

    private val _autoSyncEnabled = MutableStateFlow(false)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    init {
        _autoSyncEnabled.value = syncPrefs.getBoolean("auto_sync_enabled", false)
        refreshAuthState()
    }

    fun refreshAuthState() {
        _authState.value = signInManager.currentAuthState()
        if (_authState.value is GoogleAuthState.SignedIn) {
            loadBackupList()
        }
    }


    /** Call this from the Activity after receiving the Google Sign-In result. */
    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _authState.value = GoogleAuthState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    _authState.value = GoogleAuthState.Error("No ID token returned. Ensure Google Provider is enabled in Firebase Console.")
                    return@launch
                }
                val state = signInManager.signInWithGoogleToken(idToken)
                _authState.value = state
                if (state is GoogleAuthState.SignedIn) {
                    loadBackupList()
                }
            } catch (e: ApiException) {
                val errorMsg = when (e.statusCode) {
                    10 -> "Sign-in failed (Code 10: DEVELOPER_ERROR). Please check:\n1. 'Google' provider is Enabled in Firebase Authentication -> Sign-in method.\n2. SHA-1 fingerprint matches Firebase console."
                    12500 -> "Sign-in failed (Code 12500). Please update Google Play Services on your device."
                    12501 -> "Sign-in was cancelled."
                    7 -> "Sign-in failed (Code 7: Network Error). Please check internet connection."
                    else -> "Sign-in failed (Code ${e.statusCode}): ${e.message ?: "ApiException"}"
                }
                _authState.value = GoogleAuthState.Error(errorMsg)
            } catch (e: Exception) {
                _authState.value = GoogleAuthState.Error(e.message ?: "Unknown sign-in error")
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            signInManager.signOut()
            runCatching {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            }
            CloudSyncWorker.cancel(workManager)
            syncPrefs.edit().putBoolean("auto_sync_enabled", false).apply()
            _authState.value = GoogleAuthState.SignedOut
            _backupStatus.value = BackupStatus.Idle
            _restoreStatus.value = RestoreStatus.Idle
            _availableBackups.value = emptyList()
            _autoSyncEnabled.value = false
        }
    }


    fun backupNow() {
        if (_backupStatus.value is BackupStatus.InProgress) return
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress()
            _backupStatus.value = cloudBackupManager.backup()
            if (_backupStatus.value is BackupStatus.Success) {
                loadBackupList()
            }
        }
    }

    fun restoreLatest() {
        if (_restoreStatus.value is RestoreStatus.InProgress) return
        viewModelScope.launch {
            _restoreStatus.value = RestoreStatus.InProgress()
            _restoreStatus.value = cloudBackupManager.restore()
        }
    }

    fun restoreFromBackup(backupId: String) {
        if (_restoreStatus.value is RestoreStatus.InProgress) return
        viewModelScope.launch {
            _restoreStatus.value = RestoreStatus.InProgress()
            _restoreStatus.value = cloudBackupManager.restoreFromBackup(backupId)
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            cloudBackupManager.deleteBackup(backupId)
            loadBackupList()
        }
    }

    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        syncPrefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        if (enabled) CloudSyncWorker.schedule(workManager)
        else CloudSyncWorker.cancel(workManager)
    }


    fun clearBackupStatus() { _backupStatus.value = BackupStatus.Idle }
    fun clearRestoreStatus() { _restoreStatus.value = RestoreStatus.Idle }

    private fun loadBackupList() {
        viewModelScope.launch {
            _availableBackups.value = cloudBackupManager.listBackups()
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingsmeqquizscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizScreen.kt`
- **Lines of Code:** 302
- **Size:** 11,046 bytes (10.8 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MEQQuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: MEQQuizViewModel = hiltViewModel()
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chronotype Quiz") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (quizState.isComplete) {
                QuizCompletionScreen(
                    chronotypeName = quizState.detectedChronotype ?: "Unknown",
                    meqScore = quizState.detectedMEQScore ?: 0,
                    onReset = { viewModel.resetQuiz() },
                    onBack = onNavigateBack
                )
            } else if (quizState.questions.isNotEmpty()) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { (quizState.currentQuestionIndex + 1).toFloat() / quizState.questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = NeuroFlowColors.Purple
                )
                
                Text(
                    "Question ${quizState.currentQuestionIndex + 1} of ${quizState.questions.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current question
                val currentQuestion = quizState.questions[quizState.currentQuestionIndex]
                
                // Question text
                Text(
                    currentQuestion.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Answer options
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentQuestion.options.forEachIndexed { idx, option ->
                        AnswerOptionCard(
                            text = option,
                            isSelected = currentQuestion.selectedAnswer == idx,
                            onClick = { viewModel.selectAnswer(idx) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.previousQuestion() },
                        enabled = quizState.currentQuestionIndex > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        enabled = currentQuestion.selectedAnswer >= 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
                    ) {
                        Text(
                            if (quizState.currentQuestionIndex == quizState.questions.size - 1) 
                                "Complete" 
                            else 
                                "Next"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                NeuroFlowColors.Purple.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder() 
        else 
            null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = NeuroFlowColors.Purple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuizCompletionScreen(
    chronotypeName: String,
    meqScore: Int,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🎉",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "Quiz Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Chronotype result
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NeuroFlowColors.Purple.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Your Chronotype",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatChronotypeName(chronotypeName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowColors.Purple,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "MEQ Score: $meqScore",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Text(
            "Your energy levels and focus patterns will be optimized based on this result.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retake Quiz")
            }
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    }
}

private fun formatChronotypeName(chronotype: String): String {
    return when (chronotype) {
        "DEFINITE_MORNING" -> "Definite Morning ⏰"
        "MODERATE_MORNING" -> "Moderate Morning 🌤️"
        "INTERMEDIATE" -> "Intermediate ☀️"
        "MODERATE_EVENING" -> "Moderate Evening 🌙"
        "DEFINITE_EVENING" -> "Definite Evening 🌃"
        else -> chronotype
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingsmeqquizviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizViewModel.kt`
- **Lines of Code:** 298
- **Size:** 13,363 bytes (13.0 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.engine.MEQChronotypeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * State for an individual quiz question
 */
data class MEQQuizQuestion(
    val number: Int,
    val text: String,
    val options: List<String>,
    val selectedAnswer: Int = -1  // -1 means no selection
)

/**
 * State for the entire quiz
 */
data class MEQQuizState(
    val currentQuestionIndex: Int = 0,
    val questions: List<MEQQuizQuestion> = emptyList(),
    val isComplete: Boolean = false,
    val detectedChronotype: String? = null,  // Enum name like "DEFINITE_MORNING"
    val detectedMEQScore: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MEQQuizViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _quizState = MutableStateFlow(MEQQuizState())
    val quizState: StateFlow<MEQQuizState> = _quizState.asStateFlow()

    private val _preferences = MutableStateFlow<UserPreferences?>(null)
    val preferences = _preferences.asStateFlow()

    init {
        // Load preferences and initialize quiz
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.quizProgress == new.quizProgress &&
                    old.wakeUpHour == new.wakeUpHour
                }
                .collect { prefs ->
                    _preferences.value = prefs
                    // Initialize quiz on first load
                    if (_quizState.value.questions.isEmpty()) {
                        initializeQuiz(prefs)
                    }
                }
        }
    }

    private fun initializeQuiz(prefs: UserPreferences) {
        val questions = createMEQQuestions()
        
        // Check if there's existing progress to restore
        val restoredState = if (prefs.quizProgress.isNotEmpty() && prefs.quizProgress != "{}") {
            restoreProgress(questions, prefs.quizProgress)
        } else {
            MEQQuizState(questions = questions)
        }
        
        _quizState.value = restoredState
    }

    private fun createMEQQuestions(): List<MEQQuizQuestion> {
        return listOf(
            MEQQuizQuestion(1, "What time would you choose to wake up if you were entirely free to plan your day?", 
                listOf("5:00-6:30 AM", "6:30-7:45 AM", "7:45-9:45 AM", "9:45-11:00 AM")),
            MEQQuizQuestion(2, "How easy do you find getting up in the morning (with no alarm)?",
                listOf("Not at all easy", "Slightly easy", "Fairly easy", "Very easy")),
            MEQQuizQuestion(3, "How alert do you feel during the first 30 minutes after waking?",
                listOf("Not at all alert", "Slightly alert", "Fairly alert", "Very alert")),
            MEQQuizQuestion(4, "How is your appetite during the first 30 minutes after waking?",
                listOf("Very poor", "Fairly poor", "Fairly good", "Very good")),
            MEQQuizQuestion(5, "How do you feel physically during the first 30 minutes after waking?",
                listOf("Very tired", "Fairly tired", "Fairly fresh", "Very fresh")),
            MEQQuizQuestion(6, "What time of day do you feel your peak of good mood and energy?",
                listOf("5:00-7:30 AM", "7:30-10:00 AM", "10:00 AM-5:00 PM", "5:00-10:00 PM")),
            MEQQuizQuestion(7, "At what time in the evening do you feel tired enough to go to bed?",
                listOf("8:00-9:00 PM", "9:00-10:15 PM", "10:15-12:30 AM", "12:30-1:45 AM")),
            MEQQuizQuestion(8, "How would you describe your usual sleeping pattern?",
                listOf("Sleep well during early night, wake too early", "Sleep well throughout", "Take a long time to fall asleep", "Wake several times during night")),
            MEQQuizQuestion(9, "Do you own an alarm clock?",
                listOf("No, never use one", "Yes, but seldom need it", "Yes, sometimes need it", "Yes, always need it")),
            MEQQuizQuestion(10, "How would you describe yourself?",
                listOf("Definitely a morning person", "More a morning than evening person", "More an evening than morning person", "Definitely an evening person")),
            MEQQuizQuestion(11, "If you had to do 2 hours of physically hard work, which time would you choose?",
                listOf("7:00-9:00 AM", "9:00-11:00 AM", "3:00-5:00 PM", "7:00-9:00 PM")),
            MEQQuizQuestion(12, "At what time in the day do you think you reach your peak of mental ability?",
                listOf("5:00-7:30 AM", "7:30-10:00 AM", "10:00 AM-5:00 PM", "5:00-10:00 PM")),
            MEQQuizQuestion(13, "When you have decided to get up at a specific time, how dependent are you on an alarm clock?",
                listOf("Not dependent", "Slightly dependent", "Fairly dependent", "Very dependent")),
            MEQQuizQuestion(14, "How easy do you find making decisions the first half-hour after waking?",
                listOf("Very difficult", "Fairly difficult", "Fairly easy", "Very easy")),
            MEQQuizQuestion(15, "How easy is it for you to get up in the morning when you have no plans that day?",
                listOf("Very difficult", "Fairly difficult", "Fairly easy", "Very easy")),
            MEQQuizQuestion(16, "Suppose you intend to be in bed by 10:30 PM. How often do you actually get to bed around that time?",
                listOf("Never", "Rarely", "Sometimes", "Always")),
            MEQQuizQuestion(17, "If you went to bed at 11:00 PM, how tired would you be?",
                listOf("Very tired", "Fairly tired", "Not very tired", "Not at all tired")),
            MEQQuizQuestion(18, "For some reason you have gone to bed at 2:00 AM. How would you manage?",
                listOf("Would find it difficult", "Would rather not", "Wouldn't be too bad", "Would manage quite well")),
            MEQQuizQuestion(19, "How often do you feel energized/alert during the late afternoon?",
                listOf("Rarely/never", "Sometimes", "Often", "Very often"))
        )
    }

    private fun restoreProgress(
        questions: List<MEQQuizQuestion>,
        progressJson: String
    ): MEQQuizState {
        return try {
            val json = JSONObject(progressJson)
            val currentIndex = json.optInt("currentIndex", 0)
            val answers = json.optJSONObject("answers") ?: JSONObject()
            
            val restoredQuestions = questions.mapIndexed { idx, q ->
                val answerIdx = answers.optInt(idx.toString(), -1)
                q.copy(selectedAnswer = answerIdx)
            }
            
            MEQQuizState(
                currentQuestionIndex = currentIndex,
                questions = restoredQuestions
            )
        } catch (e: Exception) {
            MEQQuizState(questions = questions)
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _quizState.value
        if (currentState.questions.isEmpty()) return
        
        val updatedQuestions = currentState.questions.toMutableList()
        updatedQuestions[currentState.currentQuestionIndex] = 
            updatedQuestions[currentState.currentQuestionIndex].copy(selectedAnswer = answerIndex)
        
        viewModelScope.launch {
            // Save progress to preferences
            saveProgress(currentState.currentQuestionIndex, updatedQuestions)
        }
        
        _quizState.value = currentState.copy(questions = updatedQuestions)
    }

    fun nextQuestion() {
        val currentState = _quizState.value
        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            val nextIndex = currentState.currentQuestionIndex + 1
            _quizState.value = currentState.copy(currentQuestionIndex = nextIndex)
            viewModelScope.launch {
                saveProgress(nextIndex, _quizState.value.questions)
            }
        } else {
            // Quiz completed
            completeQuiz()
        }
    }

    fun previousQuestion() {
        val currentState = _quizState.value
        if (currentState.currentQuestionIndex > 0) {
            val previousIndex = currentState.currentQuestionIndex - 1
            _quizState.value = currentState.copy(currentQuestionIndex = previousIndex)
            viewModelScope.launch {
                saveProgress(previousIndex, _quizState.value.questions)
            }
        }
    }

    fun skipQuestion() {
        nextQuestion()
    }

    private fun mapSelectedAnswersToMeqScores(questions: List<MEQQuizQuestion>): List<Int> {
        // 4-option per-question mapping to MEQ-like morningness score (higher = more morning type).
        val scoringTables = listOf(
            intArrayOf(4, 3, 2, 1), // Q1
            intArrayOf(1, 2, 3, 4), // Q2
            intArrayOf(1, 2, 3, 4), // Q3
            intArrayOf(1, 2, 3, 4), // Q4
            intArrayOf(1, 2, 3, 4), // Q5
            intArrayOf(4, 3, 2, 1), // Q6
            intArrayOf(4, 3, 2, 1), // Q7
            intArrayOf(4, 3, 1, 2), // Q8
            intArrayOf(4, 3, 2, 1), // Q9
            intArrayOf(4, 3, 2, 1), // Q10
            intArrayOf(4, 3, 2, 1), // Q11
            intArrayOf(4, 3, 2, 1), // Q12
            intArrayOf(4, 3, 2, 1), // Q13
            intArrayOf(1, 2, 3, 4), // Q14
            intArrayOf(1, 2, 3, 4), // Q15
            intArrayOf(1, 2, 3, 4), // Q16
            intArrayOf(4, 3, 2, 1), // Q17
            intArrayOf(4, 3, 2, 1), // Q18
            intArrayOf(4, 3, 2, 1)  // Q19
        )

        return questions.mapIndexed { idx, q ->
            val selected = q.selectedAnswer
            if (selected !in 0..3) {
                0
            } else {
                scoringTables.getOrNull(idx)?.getOrNull(selected) ?: 0
            }
        }
    }

    private fun completeQuiz() {
        val currentState = _quizState.value
        val answerScores = mapSelectedAnswersToMeqScores(currentState.questions)
        
        // Calculate MEQ score and chronotype directly
        val meqResult = MEQChronotypeDetector.detect(answerScores)
        val chronotype = meqResult.chronotype
        val meqScore = meqResult.totalScore
        val prefs = _preferences.value
        val wakeUpHour = prefs?.wakeUpHour ?: 7
        val offsetHours = when (chronotype) {
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING -> 0f
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING -> 2.5f
            MEQChronotypeDetector.Chronotype.INTERMEDIATE -> 2.5f
            MEQChronotypeDetector.Chronotype.MODERATE_EVENING -> 5.5f
            MEQChronotypeDetector.Chronotype.DEFINITE_EVENING -> 5.5f
        }
        val peakMinuteOfDay = (((wakeUpHour * 60) + (offsetHours * 60f).toInt()) % (24 * 60))
        val peakHourOfDay = peakMinuteOfDay / 60
        val peakWindowEndHourOfDay = (peakHourOfDay + 3) % 24
        val confidence = meqResult.confidence
        
        _quizState.value = currentState.copy(
            isComplete = true,
            detectedMEQScore = meqScore,
            detectedChronotype = chronotype.name
        )
        
        // Save quiz result to preferences
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(
                    quizChronotype = chronotype.name,
                    detectedPeakStart = peakHourOfDay,
                    detectedPeakEnd = peakWindowEndHourOfDay,
                    detectedPeakMinuteOfDay = peakMinuteOfDay,
                    peakDetectionConfidence = confidence,
                    effectivePeakStart = peakHourOfDay,
                    effectivePeakEnd = peakWindowEndHourOfDay,
                    effectivePeakMinuteOfDay = peakMinuteOfDay,
                    meqScore = meqScore,
                    quizProgress = "{}"  // Clear progress on completion
                )
            }
        }
    }

    private suspend fun saveProgress(currentIndex: Int, questions: List<MEQQuizQuestion>) {
        val progressJson = JSONObject().apply {
            put("currentIndex", currentIndex)
            val answers = JSONObject()
            questions.forEachIndexed { idx, q ->
                answers.put(idx.toString(), q.selectedAnswer)
            }
            put("answers", answers)
        }.toString()
        
        preferencesDataStore.updatePreferences { prefs ->
            prefs.copy(quizProgress = progressJson)
        }
    }

    fun resetQuiz() {
        viewModelScope.launch {
            val questions = createMEQQuestions()
            _quizState.value = MEQQuizState(questions = questions)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(quizProgress = "{}")
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingspriorityweightsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/PriorityWeightsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/PriorityWeightsScreen.kt`
- **Lines of Code:** 350
- **Size:** 14,559 bytes (14.2 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityWeightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Weights & Strategy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Fine-tune the mathematical prioritization formula or choose a tailored 1-click strategy preset.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── STRATEGY PRESETS ─────────────────────────────────────────────
            Text(
                "Strategy Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    title = "⚖ Balanced",
                    description = "Default science-backed blend",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 1.0f,
                                weightDeadlineUrgency = 1.0f,
                                weightPriorityLevel = 1.0f,
                                weightDuration = 1.0f,
                                weightImpact = 1.0f,
                                weightFocusMode = 1.0f,
                                weightCircadianMatch = 1.0f,
                                weightFrogBonus = 1.0f,
                                weightPlanBoost = 1.0f,
                                weightSessionProgress = 1.0f,
                                weightEnjoyment = 1.0f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "🚨 Deadline Driven",
                    description = "Urgency & critical deadlines first",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 1.4f,
                                weightDeadlineUrgency = 1.8f,
                                weightPriorityLevel = 1.4f,
                                weightDuration = 0.8f,
                                weightImpact = 0.8f,
                                weightFocusMode = 0.8f,
                                weightCircadianMatch = 0.7f,
                                weightFrogBonus = 0.7f,
                                weightPlanBoost = 0.8f,
                                weightSessionProgress = 0.8f,
                                weightEnjoyment = 0.5f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "⚡ Momentum / ADHD",
                    description = "Quick wins, frogs & plans first",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 0.8f,
                                weightDeadlineUrgency = 0.8f,
                                weightPriorityLevel = 0.8f,
                                weightDuration = 1.8f,
                                weightImpact = 1.0f,
                                weightFocusMode = 1.2f,
                                weightCircadianMatch = 1.4f,
                                weightFrogBonus = 1.4f,
                                weightPlanBoost = 1.6f,
                                weightSessionProgress = 1.8f,
                                weightEnjoyment = 1.5f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "🏛 Pure Eisenhower",
                    description = "Strict Quadrant & Impact ranking",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 2.0f,
                                weightDeadlineUrgency = 1.2f,
                                weightPriorityLevel = 1.6f,
                                weightDuration = 0.5f,
                                weightImpact = 1.6f,
                                weightFocusMode = 0.5f,
                                weightCircadianMatch = 0.5f,
                                weightFrogBonus = 0.5f,
                                weightPlanBoost = 0.5f,
                                weightSessionProgress = 0.5f,
                                weightEnjoyment = 0.5f
                            )
                        }
                    }
                )
            }

            HorizontalDivider()

            // ── CORE PRIORITY WEIGHTS ─────────────────────────────────────────
            Text(
                "Core Priority Weights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WeightSlider(
                label = "Quadrant (DO FIRST > SCHEDULE > DELEGATE)",
                value = prefs.weightQuadrant,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightQuadrant = it) } }
            )

            WeightSlider(
                label = "Deadline Urgency (exponential proximity boost)",
                value = prefs.weightDeadlineUrgency,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightDeadlineUrgency = it) } }
            )

            WeightSlider(
                label = "Priority Level (High > Medium > Low)",
                value = prefs.weightPriorityLevel,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightPriorityLevel = it) } }
            )

            WeightSlider(
                label = "Quick-Win Duration (shorter tasks boost)",
                value = prefs.weightDuration,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightDuration = it) } }
            )

            WeightSlider(
                label = "Strategic Impact & Value",
                value = prefs.weightImpact,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightImpact = it) } }
            )

            WeightSlider(
                label = "Focus Mode & Effort Alignment",
                value = prefs.weightFocusMode,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightFocusMode = it) } }
            )

            HorizontalDivider()

            // ── BEHAVIORAL & MOTIVATION HEURISTICS ────────────────────────────
            Text(
                "Behavioral & Motivation Heuristics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WeightSlider(
                label = "Circadian Energy Match (Task Type vs Time-of-Day)",
                value = prefs.weightCircadianMatch,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightCircadianMatch = it) } }
            )

            WeightSlider(
                label = "Eat the Frog Bonus (Hardest task morning boost)",
                value = prefs.weightFrogBonus,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightFrogBonus = it) } }
            )

            WeightSlider(
                label = "Implementation Plan Boost (If-Then concrete steps)",
                value = prefs.weightPlanBoost,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightPlanBoost = it) } }
            )

            WeightSlider(
                label = "Progress Momentum (Started tasks boost)",
                value = prefs.weightSessionProgress,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightSessionProgress = it) } }
            )

            WeightSlider(
                label = "Enjoyment & Motivation (Temptation bundling)",
                value = prefs.weightEnjoyment,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightEnjoyment = it) } }
            )

            // Reset to defaults
            OutlinedButton(
                onClick = {
                    viewModel.updatePreferences { p ->
                        p.copy(
                            weightQuadrant = 1.0f,
                            weightDeadlineUrgency = 1.0f,
                            weightPriorityLevel = 1.0f,
                            weightDuration = 1.0f,
                            weightImpact = 1.0f,
                            weightFocusMode = 1.0f,
                            weightCircadianMatch = 1.0f,
                            weightFrogBonus = 1.0f,
                            weightPlanBoost = 1.0f,
                            weightSessionProgress = 1.0f,
                            weightEnjoyment = 1.0f
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All to Defaults")
            }

            // Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.PurpleLight)
            ) {
                Text(
                    text = "💡 Tip: Increase weights for factors that matter most to your workflow. If you prefer pure urgency over energy matching, lower the Circadian and Enjoyment weights.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = NeuroFlowColors.PurpleDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeightSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowColors.Purple
                ) {
                    Text(
                        "${String.format("%.1f", value)}x",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = { onValueChange(it) },
                valueRange = 0f..2f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = NeuroFlowColors.Purple,
                    activeTrackColor = NeuroFlowColors.Purple
                )
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingsprivacypermissionsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/PrivacyPermissionsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/PrivacyPermissionsScreen.kt`
- **Lines of Code:** 181
- **Size:** 8,702 bytes (8.5 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPermissionsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Permissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Why Play Protect may warn users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "neuroFlow is a focus tool that can also act as your home screen. It asks for powerful Android permissions so it can show your apps, track focus, block distractions, and keep reminders working. Those permissions can look risky in a sideloaded app even when the app is legitimate.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }

            InfoRow(
                icon = Icons.Filled.Search,
                title = "Installed apps",
                body = "Needed so the allowlist picker can list your apps and so the black home can resolve their icons.",
            )
            InfoRow(
                icon = Icons.Filled.Visibility,
                title = "Usage access",
                body = "Needed to tell which app you are using during a focus session, which is how distraction scoring and blocking decide what to gate.",
            )
            InfoRow(
                icon = Icons.Filled.Lock,
                title = "Accessibility service",
                body = "Needed only for Hyper Focus app blocking and on-screen protection while focus is active.",
            )
            InfoRow(
                icon = Icons.Filled.BugReport,
                title = "Notifications, alarms, boot",
                body = "Needed for reminders, focus timers, and restarting scheduled background work after reboot.",
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trust checklist", fontWeight = FontWeight.Bold)
                    Text("Only install from a source you trust. Review the permissions before enabling them. If you do not use focus blocking, accessibility can stay off.")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sideloaded install note", fontWeight = FontWeight.Bold)
                    Text(
                        "Because this app is not in Play Store, Android may show a stronger warning during install. That does not automatically mean the app is harmful, but you should only continue if you trust the source and understand the permissions above.",
                        lineHeight = 20.sp
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What we do not do", fontWeight = FontWeight.Bold)
                    Text("We do not sell personal data. We do not need an account. We do not upload your contacts or task data for ads.")
                    HorizontalDivider()
                    Text("If you want the safest setup", fontWeight = FontWeight.Bold)
                    Text("Leave optional permissions off until you need them. Reminders and logging still work, but focus blocking and the black home will be limited.")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingssettingsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt`
- **Lines of Code:** 1,601
- **Size:** 75,557 bytes (73.8 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.presentation.common.EnergyInsight
import com.neuroflow.app.presentation.common.TaskTagViewModel
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPriorityWeights: () -> Unit,
    onNavigateToHyperFocusAllowlist: () -> Unit = {},
    onNavigateToAppGuide: () -> Unit = {},
    onNavigateToPrivacyPermissions: () -> Unit = {},
    onNavigateToMEQQuiz: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToAccountSync: () -> Unit = {},
    onNavigateToLogTime: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val circadianSummary by viewModel.circadianSummary.collectAsStateWithLifecycle()
    val aiTestStatus by viewModel.aiTestStatus.collectAsStateWithLifecycle()
    val isTestingAi by viewModel.isTestingAi.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearTelemetryDialog by remember { mutableStateOf(false) }
    var showDistractingAppsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Categorized Settings Hub (Chronobiology, Schedulers, HyperFocus, AI & Cloud Sync)
            com.neuroflow.app.presentation.v2.settings.CategorizedSettingsHubV2(
                chronotypeSummary = prefs.quizChronotype ?: "Anchor: ${prefs.wakeUpHour}:00 AM",
                schedulerSummary = "Auto-Schedule & Prioritization Weights",
                hyperFocusSummary = "App blocking, emergency exits, rewards",
                aiAndSyncSummary = if (prefs.geminiApiKey.isNotBlank()) "Gemini Flash BYOK Active" else "Offline Heuristics Active",
                onOpenChronobiologyAndSleep = onNavigateToSleepLogs,
                onOpenSchedulerAndWeights = onNavigateToPriorityWeights,
                onOpenHyperFocusAndPermissions = onNavigateToHyperFocusAllowlist,
                onOpenAiAndCloudSync = onNavigateToAccountSync
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Appearance & Theming
            SettingsSection("Appearance") {
                Text(
                    "Theme Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        AppTheme.SYSTEM to "System",
                        AppTheme.LIGHT to "Light",
                        AppTheme.DARK to "Dark"
                    )
                    themes.forEach { (themeOption, label) ->
                        val isSelected = prefs.theme == themeOption
                        OutlinedButton(
                            onClick = { viewModel.setTheme(themeOption) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    label = "Pure Black AMOLED",
                    description = "Use pitch-black backgrounds for OLED battery savings",
                    checked = prefs.amoledDarkEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(amoledDarkEnabled = it) } }
                )
            }

            // AI & Task Decomposition
            SettingsSection("Gemini Flash AI Engine") {
                SettingsToggleRow(
                    label = "Gemini Flash AI Services",
                    description = "Uses gemini-flash-latest for task enrichment, circadian slot matching, and weekly avoidance audits",
                    checked = prefs.geminiAiEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(geminiAiEnabled = it) } }
                )
                if (prefs.geminiAiEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Engine status badge
                    val isKeyConfigured = prefs.geminiApiKey.isNotBlank()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isKeyConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKeyConfigured) "● Active: gemini-flash-latest (Personal BYOK Key)" else "○ Offline Mode: Built-in Neuroscience Heuristics Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    var apiKey by rememberSaveable(prefs.geminiApiKey) { mutableStateOf(prefs.geminiApiKey) }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            viewModel.updatePreferences { p -> p.copy(geminiApiKey = it.trim()) }
                        },
                        label = { Text("Gemini API Key (BYOK)") },
                        placeholder = { Text("Enter your Gemini API key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Stored 100% privately on-device. Uses gemini-flash-latest with single-call enrichment and 20-min TTL caching to conserve quota.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testGeminiConnection() },
                            enabled = !isTestingAi && prefs.geminiApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test AI Connection", fontSize = 11.sp)
                            }
                        }
                    }

                    if (!aiTestStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val isSuccess = aiTestStatus?.startsWith("✅") == true
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiTestStatus ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (prefs.adaptiveSystemDirective.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🧠 Calibrated Adaptive Directive",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prefs.adaptiveSystemDirective,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Profile & Identity
            SettingsSection("Profile & Identity") {

                // Sync local state when prefs load from DataStore (avoids stale initial value)
                var identityLabel by remember { mutableStateOf(prefs.identityLabel) }
                LaunchedEffect(prefs.identityLabel) { identityLabel = prefs.identityLabel }
                OutlinedTextField(
                    value = identityLabel,
                    onValueChange = {
                        identityLabel = it
                        viewModel.updatePreferences { p -> p.copy(identityLabel = it) }
                    },
                    label = { Text("I am a...") },
                    placeholder = { Text("e.g. Deep Worker, Consistent Learner") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                var topGoal by remember { mutableStateOf(prefs.topGoal) }
                LaunchedEffect(prefs.topGoal) { topGoal = prefs.topGoal }
                OutlinedTextField(
                    value = topGoal,
                    onValueChange = {
                        topGoal = it
                        viewModel.updatePreferences { p -> p.copy(topGoal = it) }
                    },
                    label = { Text("My top goal") },
                    placeholder = { Text("What are you working towards?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsNumberRow("Default Wake Hour", prefs.wakeUpHour, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(wakeUpHour = it) }
                }
                SettingsNumberRow("Default Sleep Hour", prefs.sleepHour, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(sleepHour = it) }
                }
                SettingsNumberRow("Peak Energy Start", prefs.peakEnergyStart, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(peakEnergyStart = it) }
                }
                SettingsNumberRow("Peak Energy End", prefs.peakEnergyEnd, 0, 23, formatHour = true) {
                    // Must be after peak start
                    viewModel.updatePreferences { p -> p.copy(peakEnergyEnd = maxOf(it, p.peakEnergyStart + 1)) }
                }
                if (prefs.peakEnergyEnd <= prefs.peakEnergyStart) {
                    Text(
                        "Peak end must be after peak start",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // Work Schedule
            SettingsCompactBox(title = "Work Schedule", collapsedByDefault = true) {
                SettingsNumberRow("Work Day Start", prefs.workDayStart, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(workDayStart = it) }
                }
                SettingsNumberRow("Work Day End", prefs.workDayEnd, 0, 23, formatHour = true) {
                    // Must be after work start
                    viewModel.updatePreferences { p -> p.copy(workDayEnd = maxOf(it, p.workDayStart + 1)) }
                }
                if (prefs.workDayEnd <= prefs.workDayStart) {
                    Text(
                        "Work end must be after work start",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        "Work window: ${hourLabel(prefs.workDayStart)} – ${hourLabel(prefs.workDayEnd)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                SettingsNumberRow("Pomodoro (minutes)", prefs.defaultPomodoroMinutes, 5, 60) {
                    viewModel.updatePreferences { p -> p.copy(defaultPomodoroMinutes = it) }
                }
                SettingsNumberRow("Break (minutes)", prefs.defaultBreakMinutes, 1, 30) {
                    viewModel.updatePreferences { p -> p.copy(defaultBreakMinutes = it) }
                }
            }

            // Focus Behaviour + Neuro Booster automation
            SettingsSection("Focus Behaviour") {
                SettingsToggleRow(
                    label = "WOOP planning prompt",
                    description = "Show the WOOP sheet when opening a task for the first time",
                    checked = prefs.woopEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(woopEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Auto-start tracker",
                    description = "Automatically starts tracking after 8 seconds on the focus screen",
                    checked = prefs.autoTrackerEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(autoTrackerEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Do Not Disturb (DND) during focus",
                    description = "Automatically mute notifications during active focus sprints",
                    checked = prefs.dndEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(dndEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsDeepWorkTargetRow(
                    minutes = prefs.dailyDeepWorkTargetMinutes,
                    onChange = { target ->
                        viewModel.updatePreferences { p -> p.copy(dailyDeepWorkTargetMinutes = target) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToLogTime() },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Manual Time Logging", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Log past offline sessions and deep work time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Open →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            SettingsSection("Task Tags Catalog") {
                val tagViewModel: TaskTagViewModel = hiltViewModel()
                val tags by tagViewModel.tags.collectAsStateWithLifecycle()
                var newTagInput by remember { mutableStateOf("") }

                Text(
                    "Manage your custom task tags and labels used across planning filters.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Add tag (e.g. #deepwork)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                tagViewModel.addTags(listOf(newTagInput.trim()))
                                newTagInput = ""
                            }
                        },
                        enabled = newTagInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add")
                    }
                }

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(tag, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { tagViewModel.removeTag(tag) }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            SettingsSection("Distraction Tracking") {
                DistractionPermissionRow()
                Spacer(modifier = Modifier.height(10.dp))
                DistractingAppsRow(
                    count = prefs.distractingPackages.size,
                    onOpen = {
                        viewModel.loadLaunchableApps()
                        showDistractingAppsDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "The apps you flag here are what the Today screen weighs against your deep-work time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(distractingPackages = BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES)
                        }
                    }
                ) {
                    Text("Reset to default list", fontSize = 12.sp)
                }
            }

            SettingsSection("Notifications") {
                SettingsToggleRow(
                    label = "Daily plan notification",
                    description = "Send your top-3 planning notification each morning",
                    checked = prefs.dailyPlanNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(dailyPlanNotificationsEnabled = it) }
                    }
                )
                if (prefs.dailyPlanNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsNumberRow(
                        label = "Daily plan hour",
                        value = prefs.dailyPlanNotificationHour,
                        min = 0,
                        max = 23,
                        formatHour = true
                    ) {
                        viewModel.updatePreferences { p -> p.copy(dailyPlanNotificationHour = it) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Streak check notification",
                    description = "Warn when your streak is at risk in the evening",
                    checked = prefs.streakNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(streakNotificationsEnabled = it) }
                    }
                )
                if (prefs.streakNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsNumberRow(
                        label = "Streak check hour",
                        value = prefs.streakCheckNotificationHour,
                        min = 0,
                        max = 23,
                        formatHour = true
                    ) {
                        viewModel.updatePreferences { p -> p.copy(streakCheckNotificationHour = it) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Autonomy nudge notifications",
                    description = "Allow 2-hour nudges for untouched tasks",
                    checked = prefs.autonomyNudgeNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(autonomyNudgeNotificationsEnabled = it) }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Deadline reminder notifications",
                    description = "Allow 15m/30m/1h/1d deadline reminders",
                    checked = prefs.deadlineReminderNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(deadlineReminderNotificationsEnabled = it) }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Deadline escalation alerts",
                    description = "Allow schedule-to-do-first escalation notifications",
                    checked = prefs.deadlineEscalationNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(deadlineEscalationNotificationsEnabled = it) }
                    }
                )
            }

            // Features → Energy
            SettingsSection("Energy") {
                SettingsToggleRow(
                    label = "Use MEQ quiz peak",
                    description = "When off, manual Peak Energy Start/End are used even if quiz results exist",
                    checked = prefs.quizPeakEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updatePreferences { p -> p.copy(quizPeakEnabled = enabled) }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onNavigateToMEQQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (prefs.quizChronotype != null) "Retake MEQ Chronotype Quiz" else "Take MEQ Chronotype Quiz")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (prefs.quizPeakEnabled) {
                        "Scoring source: MEQ quiz peak when available"
                    } else {
                        "Scoring source: manual peak window"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val peakStart = circadianSummary?.peakWindowStartHour ?: prefs.peakEnergyStart
                val peakEnd = circadianSummary?.peakWindowEndHour ?: prefs.peakEnergyEnd
                val dipStart = circadianSummary?.dipWindowStartHour ?: 14
                val dipEnd = circadianSummary?.dipWindowEndHour ?: 16
                val peakScore = circadianSummary?.peakEnergyScore ?: 80
                val deepWorkMins = circadianSummary?.recommendedDeepWorkMinutes ?: 180

                Text(
                    text = "Effective peak window: ${EnergyInsight.minuteLabel(peakStart * 60)} – ${EnergyInsight.minuteLabel(peakEnd * 60)} ($peakScore score)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Afternoon dip window: ${EnergyInsight.minuteLabel(dipStart * 60)} – ${EnergyInsight.minuteLabel(dipEnd * 60)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recommended deep work capacity: ${deepWorkMins}m",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!prefs.quizPeakEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tip: turn quiz peak on to use personalized morning adjustments from sleep behavior.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Profile Controls", collapsedByDefault = true) {
                    SettingsToggleRow(
                        label = "Manual profile override",
                        description = "Replace adaptive profile with your custom anchor, windows, and amplitudes",
                        checked = prefs.manualPeakProfileEnabled,
                        onCheckedChange = {
                            viewModel.updatePreferences { p -> p.copy(manualPeakProfileEnabled = it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Profile type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO", "WORKDAY", "WEEKEND").forEach { mode ->
                            FilterChip(
                                selected = prefs.manualPeakProfileType == mode,
                                enabled = prefs.manualPeakProfileEnabled,
                                onClick = { viewModel.updatePreferences { p -> p.copy(manualPeakProfileType = mode) } },
                                label = { Text(mode.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                            )
                        }
                    }

                    val selectedChronotype = prefs.quizChronotype ?: prefs.manualChronotype
                    val chronotypePreset = manualProfilePresetForChronotype(
                        chronotype = selectedChronotype,
                        wakeUpHour = prefs.wakeUpHour
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Chronotype presets", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Current preset: ${chronotypePreset.label}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = prefs.manualPeakProfileEnabled,
                            onClick = {
                                val preset = manualProfilePresetForChronotype(
                                    chronotype = "INTERMEDIATE",
                                    wakeUpHour = prefs.wakeUpHour
                                )
                                viewModel.updatePreferences { p ->
                                    p.copy(
                                        manualPeakProfileType = "AUTO",
                                        manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                        manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                        manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                        manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                        manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                        manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                        manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                        manualPeakWindow1Amplitude = preset.window1Amplitude,
                                        manualPeakWindow2Amplitude = preset.window2Amplitude,
                                        manualPeakWindow3Amplitude = preset.window3Amplitude
                                    )
                                }
                            }
                        ) {
                            Text("Apply Intermediate")
                        }
                        OutlinedButton(
                            enabled = prefs.manualPeakProfileEnabled,
                            onClick = {
                                val preset = manualProfilePresetForChronotype(
                                    chronotype = "DEFINITE_EVENING",
                                    wakeUpHour = prefs.wakeUpHour
                                )
                                viewModel.updatePreferences { p ->
                                    p.copy(
                                        manualPeakProfileType = "AUTO",
                                        manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                        manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                        manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                        manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                        manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                        manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                        manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                        manualPeakWindow1Amplitude = preset.window1Amplitude,
                                        manualPeakWindow2Amplitude = preset.window2Amplitude,
                                        manualPeakWindow3Amplitude = preset.window3Amplitude
                                    )
                                }
                            }
                        ) {
                            Text("Apply Night Owl")
                        }
                    }
                    Text(
                        text = "Presets use wake-up anchored peaks. You can edit each window start offset below.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val anchorHour = (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
                    val anchorMinute = (prefs.manualPeakAnchorMinuteOfDay % 60).coerceIn(0, 59)
                    SettingsNumberRow(
                        "Anchor Hour",
                        anchorHour,
                        0,
                        23,
                        formatHour = true,
                        enabled = prefs.manualPeakProfileEnabled
                    ) { hour ->
                        viewModel.updatePreferences { p ->
                            p.copy(manualPeakAnchorMinuteOfDay = hour * 60 + anchorMinute)
                        }
                    }
                    SettingsNumberRow(
                        "Anchor Minute",
                        anchorMinute,
                        0,
                        59,
                        enabled = prefs.manualPeakProfileEnabled
                    ) { minute ->
                        viewModel.updatePreferences { p ->
                            p.copy(manualPeakAnchorMinuteOfDay = anchorHour * 60 + minute)
                        }
                    }
                    SettingsNumberRow(
                        "Window 1 Offset (min)",
                        prefs.manualPeakWindow1StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow1StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 2 Offset (min)",
                        prefs.manualPeakWindow2StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow2StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 3 Offset (min)",
                        prefs.manualPeakWindow3StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow3StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 1 Duration (min)",
                        prefs.manualPeakWindow1DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow1DurationMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 2 Duration (min)",
                        prefs.manualPeakWindow2DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow2DurationMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 3 Duration (min)",
                        prefs.manualPeakWindow3DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow3DurationMinutes = it) }
                    }
                    SettingsFloatSliderRow(
                        label = "Window 1 Amplitude",
                        value = prefs.manualPeakWindow1Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow1Amplitude = it) } }
                    )
                    SettingsFloatSliderRow(
                        label = "Window 2 Amplitude",
                        value = prefs.manualPeakWindow2Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow2Amplitude = it) } }
                    )
                    SettingsFloatSliderRow(
                        label = "Window 3 Amplitude",
                        value = prefs.manualPeakWindow3Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow3Amplitude = it) } }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Profile shape preview", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProfileWindowPreviewRow(
                        label = "W1",
                        startOffsetMinutes = prefs.manualPeakWindow1StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow1DurationMinutes,
                        amplitude = prefs.manualPeakWindow1Amplitude
                    )
                    ProfileWindowPreviewRow(
                        label = "W2",
                        startOffsetMinutes = prefs.manualPeakWindow2StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow2DurationMinutes,
                        amplitude = prefs.manualPeakWindow2Amplitude
                    )
                    ProfileWindowPreviewRow(
                        label = "W3",
                        startOffsetMinutes = prefs.manualPeakWindow3StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow3DurationMinutes,
                        amplitude = prefs.manualPeakWindow3Amplitude
                    )
                    OutlinedButton(
                        enabled = prefs.manualPeakProfileEnabled,
                        onClick = {
                            val preset = manualProfilePresetForChronotype(
                                chronotype = selectedChronotype,
                                wakeUpHour = prefs.wakeUpHour
                            )
                            viewModel.updatePreferences { p ->
                                p.copy(
                                    manualPeakProfileType = "AUTO",
                                    manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                    manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                    manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                    manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                    manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                    manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                    manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                    manualPeakWindow1Amplitude = preset.window1Amplitude,
                                    manualPeakWindow2Amplitude = preset.window2Amplitude,
                                    manualPeakWindow3Amplitude = preset.window3Amplitude
                                )
                            }
                        }
                    ) {
                        Text("Reset to chronotype defaults")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.updatePreferences { p ->
                                p.copy(
                                    morningTuneSleepWeight = 0.30f,
                                    morningTuneWakeWeight = 0.25f,
                                    morningTuneBehaviorWeight = 0.25f,
                                    morningTuneBaseWeight = 0.20f,
                                    morningTuneUpdatedAtMillis = 0L
                                )
                            }
                        }
                    ) {
                        Text("Reset auto-tune weights")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Moment Sensitivity", collapsedByDefault = true) {
                    Text(
                        "Tune how strongly live context signals affect moment energy adjustments.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingsFloatSliderRow(
                        label = "Interruption sensitivity",
                        value = prefs.momentInterruptionSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentInterruptionSensitivity = it) }
                        }
                    )
                    SettingsFloatSliderRow(
                        label = "Notification sensitivity",
                        value = prefs.momentNotificationSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentNotificationSensitivity = it) }
                        }
                    )
                    SettingsFloatSliderRow(
                        label = "Task pressure sensitivity",
                        value = prefs.momentTaskPressureSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentTaskPressureSensitivity = it) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Telemetry & Privacy", collapsedByDefault = true) {
                    SettingsToggleRow(
                        label = "Energy telemetry (local only)",
                        description = "Store prediction snapshots on-device for calibration and diagnostics",
                        checked = prefs.energyTelemetryEnabled,
                        onCheckedChange = {
                            viewModel.updatePreferences { p -> p.copy(energyTelemetryEnabled = it) }
                        }
                    )
                    if (prefs.energyTelemetryEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Retention (days)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 14, 30, 60, 90).forEach { days ->
                                FilterChip(
                                    selected = prefs.energyTelemetryRetentionDays == days,
                                    onClick = {
                                        viewModel.updatePreferences { p -> p.copy(energyTelemetryRetentionDays = days) }
                                    },
                                    label = { Text("$days") }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Telemetry remains on this device. It is never required for live scoring.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearTelemetryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeuroFlowColors.TrackingRed)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, "Clear telemetry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Energy Telemetry")
                    }
                }
            }

            // User Guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToAppGuide
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("neuroFlow V2 Operating Manual", fontWeight = FontWeight.Bold)
                        Text(
                            "Circadian biophysics, auto-scheduling & executive protocols",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, "Navigate")
                }
            }

            // Privacy & Permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToPrivacyPermissions
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Privacy & Permissions", fontWeight = FontWeight.Bold)
                        Text(
                            "Why the app asks for powerful Android permissions",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, "Navigate")
                }
            }

            // Data
            SettingsSection("Data") {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeuroFlowColors.TrackingRed)
                ) {
                    Icon(Icons.Filled.DeleteForever, "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Data")
                }
            }

            // About
            SettingsSection("About") {
                Text("neuroFlow v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDistractingAppsDialog) {
        DistractingAppsDialog(
            initialSelected = prefs.distractingPackages,
            viewModel = viewModel,
            onDismiss = { showDistractingAppsDialog = false },
            onSave = { packages ->
                viewModel.updatePreferences { p -> p.copy(distractingPackages = packages) }
                showDistractingAppsDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = {
                Text(
                    "This will permanently delete all app data on this device and reset backup state so old data is not restored after reinstall. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) { Text("Delete", color = NeuroFlowColors.TrackingRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearTelemetryDialog) {
        AlertDialog(
            onDismissRequest = { showClearTelemetryDialog = false },
            title = { Text("Clear Energy Telemetry?") },
            text = {
                Text("This removes saved energy predictions used for diagnostics and calibration. Live scoring will continue.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearEnergyTelemetry()
                    showClearTelemetryDialog = false
                }) { Text("Clear", color = NeuroFlowColors.TrackingRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearTelemetryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDeepWorkTargetRow(minutes: Int, onChange: (Int) -> Unit) {
    val presets = listOf(
        60 to "1 h",
        90 to "1.5 h",
        120 to "2 h",
        180 to "3 h",
        240 to "4 h",
        300 to "5 h",
        360 to "6 h"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Daily deep-work target", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            "The Today card measures your logged focus blocks against this and says whether another one still fits.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            presets.forEach { (value, label) ->
                FilterChip(
                    selected = minutes == value,
                    onClick = { onChange(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun DistractionPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(BehavioralSupportService.hasUsagePermission(context)) }

    // The grant happens in the system settings page, which never calls back into this
    // composition — poll until it flips, the same way the Hyper Focus permission screen does.
    LaunchedEffect(granted) {
        while (!granted) {
            delay(1000)
            granted = BehavioralSupportService.hasUsagePermission(context)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Usage access", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                if (granted) "Granted — today's screen time is readable"
                else "Not granted — distraction readings stay empty",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Usage access granted",
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Button(
                onClick = { BehavioralSupportService.openUsagePermissionSettings(context) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DistractingAppsRow(count: Int, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Distracting apps", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$count flagged", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "Edit distracting apps")
    }
}

@Composable
private fun DistractingAppsDialog(
    initialSelected: Set<String>,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val apps by viewModel.launchableApps.collectAsStateWithLifecycle()
    val appsLoading by viewModel.launchableAppsLoading.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(initialSelected) }
    var query by remember { mutableStateOf("") }
    var flaggedOnly by remember { mutableStateOf(false) }

    val visible = apps.filter { app ->
        val matchesQuery = query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        val matchesFlagFilter = !flaggedOnly || app.packageName in selected
        matchesQuery && matchesFlagFilter
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                    Text(
                        "Distracting apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${selected.size}")
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSave(selected) }) { Text("Save") }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true,
                    placeholder = { Text("Search installed apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = flaggedOnly,
                        onClick = { flaggedOnly = !flaggedOnly },
                        label = { Text("Flagged only", fontSize = 12.sp) }
                    )
                    Text(
                        "Apps you don't see here can still stay in the list.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (apps.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (appsLoading) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Reading installed apps…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Couldn't read the installed apps.", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadLaunchableApps() }) {
                                Text("Try again")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visible, key = { it.packageName }) { app ->
                            AppPickRow(
                                packageName = app.packageName,
                                label = app.label,
                                checked = app.packageName in selected,
                                onToggle = {
                                    selected = if (app.packageName in selected) selected - app.packageName
                                    else selected + app.packageName
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickRow(
    packageName: String,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(72, 72).asImageBitmap()
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

private data class ManualProfilePreset(
    val label: String,
    val anchorMinuteOfDay: Int,
    val window1StartOffsetMinutes: Int,
    val window2StartOffsetMinutes: Int,
    val window3StartOffsetMinutes: Int,
    val window1DurationMinutes: Int,
    val window2DurationMinutes: Int,
    val window3DurationMinutes: Int,
    val window1Amplitude: Float,
    val window2Amplitude: Float,
    val window3Amplitude: Float
)

private fun manualProfilePresetForChronotype(
    chronotype: String?,
    wakeUpHour: Int
): ManualProfilePreset {
    val wakeMinute = wakeUpHour.coerceIn(0, 23) * 60
    fun anchored(offsetMinutes: Int): Int = (wakeMinute + offsetMinutes).mod(24 * 60)

    return when (chronotype) {
        "INTERMEDIATE" -> ManualProfilePreset(
            label = "Intermediate",
            anchorMinuteOfDay = anchored(150), // +2.5h
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 480,
            window3StartOffsetMinutes = 720,
            window1DurationMinutes = 210,      // 3.5h
            window2DurationMinutes = 150,      // 2.5h
            window3DurationMinutes = 60,       // 1h
            window1Amplitude = 1.0f,
            window2Amplitude = 0.78f,
            window3Amplitude = 0.58f
        )

        "MODERATE_EVENING", "DEFINITE_EVENING" -> ManualProfilePreset(
            label = "Night Owl",
            anchorMinuteOfDay = anchored(330), // +5.5h
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 330,
            window3StartOffsetMinutes = 600,
            window1DurationMinutes = 180,      // 3h
            window2DurationMinutes = 240,      // 4h
            window3DurationMinutes = 60,       // 1h
            window1Amplitude = 1.0f,
            window2Amplitude = 0.85f,
            window3Amplitude = 0.6f
        )

        else -> ManualProfilePreset(
            label = "Morning",
            anchorMinuteOfDay = anchored(0),
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 570,
            window3StartOffsetMinutes = 810,
            window1DurationMinutes = 210,
            window2DurationMinutes = 150,
            window3DurationMinutes = 60,
            window1Amplitude = 1.0f,
            window2Amplitude = 0.8f,
            window3Amplitude = 0.6f
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsCompactBox(
    title: String,
    collapsedByDefault: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title, collapsedByDefault) { mutableStateOf(!collapsedByDefault) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(2.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingsNumberRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    formatHour: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit
) {
    val displayValue = if (formatHour) hourLabel(value) else "$value"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (enabled && value > min) onValueChange(value - 1) }, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Filled.Remove, "Decrease", modifier = Modifier.size(18.dp))
            }
            Text(displayValue, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { if (enabled && value < max) onValueChange(value + 1) }, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Filled.Add, "Increase", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun hourLabel(hour: Int) = when {
    hour == 0  -> "12 am"
    hour < 12  -> "$hour am"
    hour == 12 -> "12 pm"
    else       -> "${hour - 12} pm"
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsFloatSliderRow(
    label: String,
    value: Float,
    enabled: Boolean = true,
    minValue: Float = 0.2f,
    maxValue: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    val safe = value.coerceIn(minValue, maxValue)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("%.2f", safe), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = safe,
            enabled = enabled,
            onValueChange = { onValueChange(it.coerceIn(minValue, maxValue)) },
            valueRange = minValue..maxValue
        )
    }
}

@Composable
private fun ProfileWindowPreviewRow(
    label: String,
    startOffsetMinutes: Int,
    durationMinutes: Int,
    amplitude: Float
) {
    val widthWeight = durationMinutes.coerceIn(30, 360).toFloat() / 360f
    val alpha = amplitude.coerceIn(0.2f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label +${startOffsetMinutes}m", fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthWeight)
                    .padding(end = 4.dp)
                    .height(8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = RoundedCornerShape(4.dp)
                ) {}
            }
        }
        Text(String.format("%.2f", alpha), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingssettingsviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsViewModel.kt`
- **Lines of Code:** 326
- **Size:** 14,212 bytes (13.9 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.worker.scheduleNotificationWorkers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepPressureRepository: SleepPressureRepository,
    private val energyMetricsRepository: EnergyMetricsRepository,
    private val databaseCleaner: DatabaseCleaner,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val geminiAiOnlineService: com.neuroflow.app.domain.service.GeminiAiOnlineService,
    val passiveSleepSyncRepository: com.neuroflow.app.domain.repository.PassiveSleepSyncRepository? = null
) : ViewModel() {

    // Test convenience constructor
    constructor(
        context: Context,
        preferencesDataStore: UserPreferencesDataStore,
        sleepPressureRepository: SleepPressureRepository,
        energyMetricsRepository: EnergyMetricsRepository,
        databaseCleaner: DatabaseCleaner,
        circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
        sleepTelemetryRepository: SleepTelemetryRepository? = null
    ) : this(
        context,
        preferencesDataStore,
        sleepPressureRepository,
        energyMetricsRepository,
        databaseCleaner,
        circadianEnergyService,
        sleepTelemetryRepository,
        com.neuroflow.app.domain.service.DefaultGeminiAiOnlineService(),
        null
    )

    val isPassiveSyncing = MutableStateFlow(false)
    val passiveSyncStatus = MutableStateFlow<String?>(null)

    fun syncWearableSleep() {
        viewModelScope.launch {
            val repo = passiveSleepSyncRepository
            if (repo == null) {
                passiveSyncStatus.value = "Health Connect integration not active on this device"
                return@launch
            }
            isPassiveSyncing.value = true
            try {
                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
                val result = repo.syncPassiveSleep(sevenDaysAgo, now)
                passiveSyncStatus.value = when (result) {
                    is com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult.Success ->
                        if (result.importedLogs.isNotEmpty()) "Imported ${result.importedLogs.size} wearable sleep logs"
                        else "Wearable data is already synchronized"
                    is com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult.PermissionRequired ->
                        "Permission required: ${result.missingPermissions.firstOrNull() ?: "READ_SLEEP"}"
                    is com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult.ProviderUnavailable ->
                        "Health Provider: ${result.reason}"
                    is com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult.Failure ->
                        "Sync failed: ${result.throwable.message}"
                }
            } catch (e: Exception) {
                passiveSyncStatus.value = "Sync error: ${e.message}"
            } finally {
                isPassiveSyncing.value = false
            }
        }
    }

    fun clearPassiveSyncStatus() {
        passiveSyncStatus.value = null
    }

    data class LaunchableApp(val packageName: String, val label: String)

    private val _aiTestStatus = MutableStateFlow<String?>(null)
    val aiTestStatus: StateFlow<String?> = _aiTestStatus.asStateFlow()

    private val _isTestingAi = MutableStateFlow(false)
    val isTestingAi: StateFlow<Boolean> = _isTestingAi.asStateFlow()

    fun testGeminiConnection() {
        val key = preferences.value.geminiApiKey
        if (key.isBlank()) {
            _aiTestStatus.value = "⚠️ Please enter an API key first."
            return
        }
        viewModelScope.launch {
            _isTestingAi.value = true
            _aiTestStatus.value = "Testing connection to Gemini API..."
            try {
                val res = geminiAiOnlineService.testConnection(key)
                _aiTestStatus.value = res.fold(
                    onSuccess = { "✅ $it" },
                    onFailure = { "❌ ${it.message ?: "Connection failed"}" }
                )
            } catch (e: Exception) {
                _aiTestStatus.value = "❌ ${e.message}"
            } finally {
                _isTestingAi.value = false
            }
        }
    }

    private val _launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchableApps: StateFlow<List<LaunchableApp>> = _launchableApps.asStateFlow()

    private val _launchableAppsLoading = MutableStateFlow(false)
    val launchableAppsLoading: StateFlow<Boolean> = _launchableAppsLoading.asStateFlow()

    /**
     * Enumerating + labelling every launcher activity is PackageManager binder work over
     * hundreds of apps, so it only happens when the distracting-app picker is opened. A failed or
     * empty read leaves the list unloadable, so it is retried on the next open instead of being
     * remembered as "already loaded".
     */
    fun loadLaunchableApps() {
        if (_launchableApps.value.isNotEmpty() || _launchableAppsLoading.value) return
        _launchableAppsLoading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                val packages = pm.queryIntentActivities(intent, 0)
                    .asSequence()
                    .map { it.activityInfo.packageName }
                    .filter { it != context.packageName }
                    .distinct()
                    .map { pkg ->
                        val label = runCatching {
                            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                        }.getOrDefault(pkg)
                        LaunchableApp(pkg, label)
                    }
                    .sortedBy { it.label.lowercase() }
                    .toList()
                _launchableApps.value = packages
            } finally {
                _launchableAppsLoading.value = false
            }
        }
    }

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val sleepLogs: StateFlow<List<SleepLogEntity>> = sleepPressureRepository.observeSleepLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val circadianSummary: StateFlow<DailyCircadianSummary?> = combine(
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.wakeUpHour == new.wakeUpHour
        },
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
    ) { prefs, sleepLogs ->
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = Instant.ofEpochMilli(now).atZone(zoneId)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (anchor.toInstant().toEpochMilli() > now) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

        val naps = signal?.daytimeNaps ?: emptyList()
        if (naps.isNotEmpty()) {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId,
                naps = naps
            )
        } else {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId
            )
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sleepLogInputError = MutableStateFlow<String?>(null)
    val sleepLogInputError: StateFlow<String?> = _sleepLogInputError.asStateFlow()

    init {
        refreshSleepPressureNow()
    }

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            // Transform the value the store hands us inside its atomic edit rather than
            // rebuilding from `preferences.value`: that mirror lags behind writes made by
            // other screens, and `updatePreferences` persists every key it knows about.
            var committed: UserPreferences? = null
            var previous: UserPreferences? = null
            preferencesDataStore.updatePreferences { current ->
                val updated = update(current)
                previous = current
                committed = updated
                updated
            }
            val after = committed ?: return@launch
            val before = previous ?: return@launch

            val scheduleChanged =
                before.dailyPlanNotificationsEnabled != after.dailyPlanNotificationsEnabled ||
                    before.streakNotificationsEnabled != after.streakNotificationsEnabled ||
                    before.deadlineEscalationNotificationsEnabled != after.deadlineEscalationNotificationsEnabled ||
                    before.dailyPlanNotificationHour != after.dailyPlanNotificationHour ||
                    before.streakCheckNotificationHour != after.streakCheckNotificationHour

            if (scheduleChanged) {
                scheduleNotificationWorkers(context, after)
            }

            if (before.autonomyNudgeNotificationsEnabled && !after.autonomyNudgeNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag(AutonomyNudgeEngine.globalTag())
            }
            if (before.deadlineReminderNotificationsEnabled && !after.deadlineReminderNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag("task_reminder_all")
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        updatePreferences { it.copy(theme = theme) }
    }

    fun refreshSleepPressureNow() {
        viewModelScope.launch {
            sleepPressureRepository.refreshCurrentPressure()
        }
    }

    fun addSleepLog(startAtMillis: Long, endAtMillis: Long) {
        viewModelScope.launch {
            when (sleepPressureRepository.addSleepLog(startAtMillis, endAtMillis)) {
                is SleepPressureRepository.AddSleepLogResult.Added -> {
                    _sleepLogInputError.value = null
                }
                SleepPressureRepository.AddSleepLogResult.OverlapsExisting -> {
                    _sleepLogInputError.value = "This sleep period overlaps an existing sleep log."
                }
                is SleepPressureRepository.AddSleepLogResult.TooLong -> {
                    _sleepLogInputError.value = "Sleep log is too long. Max allowed is 16 hours."
                }
            }
        }
    }

    fun clearSleepLogInputError() {
        _sleepLogInputError.value = null
    }

    fun deleteSleepLog(logId: String) {
        viewModelScope.launch {
            sleepPressureRepository.deleteSleepLog(logId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            databaseCleaner.clearAllDataAndInvalidateBackup()
            scheduleNotificationWorkers(context, UserPreferences())
            WorkManager.getInstance(context).cancelAllWorkByTag(AutonomyNudgeEngine.globalTag())
            WorkManager.getInstance(context).cancelAllWorkByTag("task_reminder_all")
            _sleepLogInputError.value = null
        }
    }

    fun clearEnergyTelemetry() {
        viewModelScope.launch {
            energyMetricsRepository.clearAllPredictions()
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationsettingssleeplogsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/settings/SleepLogsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/settings/SleepLogsScreen.kt`
- **Lines of Code:** 423
- **Size:** 16,649 bytes (16.3 KB)

```kotlin
package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()
    val passiveSyncStatus by viewModel.passiveSyncStatus.collectAsStateWithLifecycle()
    val isPassiveSyncing by viewModel.isPassiveSyncing.collectAsStateWithLifecycle()
    val sleepLogInputError by viewModel.sleepLogInputError.collectAsStateWithLifecycle()

    var logDateMillis by rememberSaveable { mutableStateOf(todayUtcDateMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var logStartHour by remember { mutableStateOf(23) }
    var logStartMinute by remember { mutableStateOf(0) }
    var wakeHour by remember { mutableStateOf(7) }
    var wakeMinute by remember { mutableStateOf(0) }
    var seededFromSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSleepPressureNow()
    }

    LaunchedEffect(prefs.sleepHour, prefs.wakeUpHour) {
        if (!seededFromSettings) {
            logStartHour = prefs.sleepHour
            wakeHour = prefs.wakeUpHour
            seededFromSettings = true
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = logDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    logDateMillis = datePickerState.selectedDateMillis ?: logDateMillis
                    showDatePicker = false
                    viewModel.clearSleepLogInputError()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val fatigueZone = FatigueZone.fromPressurePoints(prefs.sleepPressurePoints)
            val sleepSnapshot = remember(prefs.sleepPressurePoints) {
                com.neuroflow.app.domain.repository.SleepPressureRepository.Snapshot(
                    pressurePoints = prefs.sleepPressurePoints,
                    fatiguePercent = FatigueZone.fatiguePercent(prefs.sleepPressurePoints),
                    fatigueZone = fatigueZone,
                    refreshedAtMillis = System.currentTimeMillis()
                )
            }
            com.neuroflow.app.presentation.v2.settings.UpgradedSleepPressureOverviewCard(
                snapshot = sleepSnapshot,
                hasHealthConnectSync = viewModel.passiveSleepSyncRepository != null,
                onSyncHealthConnectClick = { viewModel.syncWearableSleep() }
            )

            val currentStatus = passiveSyncStatus
            if (currentStatus != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearPassiveSyncStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            SleepLogsSection("Add Sleep Log") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sleep date", fontSize = 14.sp)
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(formatDateOnly(logDateMillis))
                    }
                }
                SleepNumberRow("Start hour", logStartHour, 0, 23, formatHour = true) {
                    logStartHour = it
                    viewModel.clearSleepLogInputError()
                }
                SleepNumberRow("Start minute", logStartMinute, 0, 59) {
                    logStartMinute = it
                    viewModel.clearSleepLogInputError()
                }
                SleepNumberRow("Wake hour", wakeHour, 0, 23, formatHour = true) {
                    wakeHour = it
                    viewModel.clearSleepLogInputError()
                }
                SleepNumberRow("Wake minute", wakeMinute, 0, 59) {
                    wakeMinute = it
                    viewModel.clearSleepLogInputError()
                }
                Spacer(modifier = Modifier.height(8.dp))

                val previewStartAt = sleepLogStartMillis(logDateMillis, logStartHour, logStartMinute)
                val previewEndAt = sleepLogEndMillis(previewStartAt, wakeHour, wakeMinute)
                val previewDurationMinutes = ((previewEndAt - previewStartAt) / 60_000L).toInt()
                Text(
                    "Calculated sleep: $previewDurationMinutes minutes",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Max allowed per log: 16 hours",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Start: ${formatDateTime(previewStartAt)}  |  Wake: ${formatDateTime(previewEndAt)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val startAt = sleepLogStartMillis(logDateMillis, logStartHour, logStartMinute)
                        val endAt = sleepLogEndMillis(startAt, wakeHour, wakeMinute)
                        viewModel.addSleepLog(startAt, endAt)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Sleep Log")
                }

                sleepLogInputError?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            SleepLogsSection("Saved Logs") {
                if (sleepLogs.isEmpty()) {
                    Text(
                        "No sleep logs yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sleepLogs.take(30).forEach { log ->
                        SleepLogRow(
                            log = log,
                            onDelete = { viewModel.deleteSleepLog(log.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepLogsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SleepNumberRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    formatHour: Boolean = false,
    onValueChange: (Int) -> Unit
) {
    val displayValue = if (formatHour) hourLabel(value) else "$value"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onValueChange(value - 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, "Decrease", modifier = Modifier.size(18.dp))
            }
            Text(
                displayValue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            IconButton(onClick = { if (value < max) onValueChange(value + 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, "Increase", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SleepLogRow(
    log: SleepLogEntity,
    onDelete: () -> Unit
) {
    val recovery = FatigueZone.recoveryForSleepMinutes(log.durationMinutes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${formatDateTime(log.startAt)} (${log.durationMinutes}m)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Recovery: $recovery pts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete sleep log")
        }
    }
}

private fun hourLabel(hour: Int) = when {
    hour == 0 -> "12 am"
    hour < 12 -> "$hour am"
    hour == 12 -> "12 pm"
    else -> "${hour - 12} pm"
}

private fun sleepLogStartMillis(dateMillis: Long, startHour: Int, startMinute: Int): Long {
    val selectedDate = Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()

    return selectedDate
        .atTime(startHour.coerceIn(0, 23), startMinute.coerceIn(0, 59))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun sleepLogEndMillis(startAtMillis: Long, wakeHour: Int, wakeMinute: Int): Long {
    val zoneId = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startAtMillis).atZone(zoneId)
    var wake = start.toLocalDate()
        .atTime(wakeHour.coerceIn(0, 23), wakeMinute.coerceIn(0, 59))
        .atZone(zoneId)

    if (!wake.toInstant().isAfter(start.toInstant())) {
        wake = wake.plusDays(1)
    }

    return wake.toInstant().toEpochMilli()
}

private fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDateOnly(millis: Long): String {
    val date = Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private fun todayUtcDateMillis(): Long {
    return LocalDate.now(ZoneId.systemDefault())
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}

private fun fatigueZoneColor(zone: FatigueZone) = when (zone) {
    FatigueZone.RESTED -> NeuroFlowColors.ScheduleText
    FatigueZone.MODERATE -> NeuroFlowColors.DelegateText
    FatigueZone.HIGH -> NeuroFlowColors.MapeBad
    FatigueZone.CRITICAL -> NeuroFlowColors.TrackingRed
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2commonsecondaryscreensupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/common/SecondaryScreensUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/common/SecondaryScreensUpgradesV2.kt`
- **Lines of Code:** 434
- **Size:** 18,268 bytes (17.8 KB)

```kotlin
package com.neuroflow.app.presentation.v2.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

@Composable
fun UpgradedUlyssesContractCard(
    taskTitle: String,
    commitmentStatement: String,
    deadlineFormatted: String,
    consequenceText: String,
    onSignContractClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.5.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.PeakAmber, 1f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = NeuroFlowPaletteV2.PeakAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ULYSSES PRE-COMMITMENT PACT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.PeakAmber,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"$commitmentStatement\"",
                style = MaterialTheme.typography.bodyMedium,
                color = NeuroFlowPaletteV2.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeuroFlowPaletteV2.SurfaceElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LOCKED DEADLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                    Text(
                        text = deadlineFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IF MISSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                    Text(
                        text = consequenceText.ifBlank { "Mandatory schedule lock" },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.RecoveryRose,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSignContractClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeuroFlowPaletteV2.PeakAmber,
                    contentColor = Color(0xFF0A0B0E)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Seal Ulysses Contract",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

@Composable
fun UpgradedIdentityAndGoalsCard(
    identityStatement: String,
    activeGoals: List<String>,
    onEditIdentityClick: () -> Unit,
    onRefillGoalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IDENTITY & NORTH-STAR GOALS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        letterSpacing = 0.9.sp
                    )
                }
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.clickable { onEditIdentityClick() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = identityStatement.ifBlank { "I am someone who protects my peak focus hours and finishes what matters." },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (activeGoals.isEmpty()) {
                Surface(
                    color = NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRefillGoalsClick() }
                ) {
                    Text(
                        text = "+ Set your Top 3 Weekly Focus Goals",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.PeakAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                activeGoals.take(3).forEachIndexed { index, goal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeuroFlowPaletteV2.DipIndigo
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradedHistorySessionRow(
    taskTitle: String,
    dateAndTimeText: String,
    durationMinutes: Float,
    estimatedMinutes: Int,
    interruptionCount: Int,
    affectiveForecastError: Float?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush()),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateAndTimeText · Est ${estimatedMinutes}m → Actual ${durationMinutes.roundToInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (interruptionCount == 0) {
                    Surface(
                        color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Unbroken",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${interruptionCount}x Int",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.RecoveryRose,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (affectiveForecastError != null) {
                    val sign = if (affectiveForecastError > 0f) "+" else ""
                    Surface(
                        color = NeuroFlowPaletteV2.SurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$sign${"%.1f".format(affectiveForecastError)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.PeakAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradedNoteCardV2(
    title: String,
    contentPreview: String,
    noteAccentColor: Color,
    updatedAtFormatted: String,
    onNoteClick: () -> Unit,
    onExtractTaskWithAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(noteAccentColor, 0.85f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNoteClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = noteAccentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onExtractTaskWithAiClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddTask,
                            contentDescription = "Extract Task",
                            tint = noteAccentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "To Task",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = noteAccentColor
                        )
                    }
                }
            }
            if (contentPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = contentPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = updatedAtFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowPaletteV2.TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2notesquicknotesbottomsheetkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt`
- **Lines of Code:** 292
- **Size:** 14,106 bytes (13.8 KB)

```kotlin
package com.neuroflow.app.presentation.v2.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotesBottomSheet(
    notes: List<NoteEntity>,
    onSaveNote: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenFullNotes: () -> Unit = {},
    onCreateTask: ((title: String, description: String) -> Unit)? = null,
    onSynthesizeNote: (suspend (String) -> com.neuroflow.app.domain.service.QuickNoteSynthesis)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var synthesis by remember { mutableStateOf<com.neuroflow.app.domain.service.QuickNoteSynthesis?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(content, title) {
        if (content.isNotBlank()) {
            val full = if (title.isNotBlank()) "$title: $content" else content
            if (onSynthesizeNote != null) {
                synthesis = onSynthesizeNote(full)
            } else {
                val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(full)
                synthesis = com.neuroflow.app.domain.service.QuickNoteSynthesis(
                    extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { content.take(35) } },
                    suggestedCategory = inferred.taskType.name,
                    estimatedMinutes = inferred.durationMinutes,
                    tags = inferred.contextTags,
                    firstActionStep = "Review note: ${content.take(20)}..."
                )
            }
        } else {
            synthesis = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.SlateBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📝 Scratchpad & Working Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "Jot down ideas, research findings, and brain dumps without distraction",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onOpenFullNotes()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Full Notes Workspace",
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }
            }

            // Quick Composer Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Note Title (Optional)...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                        )
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Write your thoughts or draft actions here...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (content.isNotBlank()) {
                                    onSaveNote(title, content)
                                    title = ""
                                    content = ""
                                }
                            },
                            enabled = content.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Note", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Note AI Task Extraction Preview
            synthesis?.let { syn ->
                com.neuroflow.app.presentation.v2.settings.QuickNoteAiSynthesizerCard(
                    synthesis = syn,
                    onConvertToTaskClick = { finalSyn ->
                        if (onCreateTask != null) {
                            onCreateTask(finalSyn.extractedTaskTitle, content)
                            onSaveNote(finalSyn.extractedTaskTitle, content)
                        } else {
                            onSaveNote(finalSyn.extractedTaskTitle, content)
                        }
                        title = ""
                        content = ""
                    }
                )
            }

            // Recent Notes List
            if (notes.isNotEmpty()) {
                Text(
                    text = "Recent Notes (${notes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val dateFormatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                            .format(Date(note.updatedAt))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenFullNotes()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled Note" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeuroFlowThemeV2Colors.TextPrimary
                                    )
                                    if (note.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = NeuroFlowThemeV2Colors.WarmAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    maxLines = 3
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextTertiary,
                                        fontSize = 10.sp
                                    )
                                    if (onCreateTask != null) {
                                        Surface(
                                            modifier = Modifier.clickable {
                                                onCreateTask(note.title, note.content)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚡ Convert to Task",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2settingshyperfocusnotesandsettingsupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/settings/HyperFocusNotesAndSettingsUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/settings/HyperFocusNotesAndSettingsUpgradesV2.kt`
- **Lines of Code:** 335
- **Size:** 13,404 bytes (13.1 KB)

```kotlin
package com.neuroflow.app.presentation.v2.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

@Composable
fun DopamineSafeBlockingGateCard(
    blockedAppName: String,
    completedTasksInSession: Int,
    nextRewardTier: RewardTier,
    topTaskTitle: String,
    topTaskFirstStep: String,
    onStartTwoMinuteStepClick: () -> Unit,
    onRedeemEarnedPassClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tasksNeeded = (nextRewardTier.taskThreshold - completedTasksInSession).coerceAtLeast(1)

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.5.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.DipIndigo, 1f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "HYPERFOCUS DOPAMINE SHIELD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$blockedAppName is paused right now",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Complete $tasksNeeded more task(s) to unlock a ${nextRewardTier.unlockMinutes}-minute ${nextRewardTier.name} reward pass.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = NeuroFlowPaletteV2.SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "NEXT MICRO-STEP (2 MIN):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.OptimalEmerald
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topTaskFirstStep.ifBlank { topTaskTitle },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartTwoMinuteStepClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeuroFlowPaletteV2.OptimalEmerald,
                    contentColor = Color(0xFF0A0B0E)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Do 2-Min Starter Step Now",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            if (onRedeemEarnedPassClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Or redeem an earned reward pass",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier
                        .clickable { onRedeemEarnedPassClick() }
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun QuickNoteAiSynthesizerCard(
    synthesis: QuickNoteSynthesis,
    onConvertToTaskClick: (QuickNoteSynthesis) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "AI TASK EXTRACTION (${synthesis.estimatedMinutes}m · ${synthesis.suggestedCategory})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = synthesis.extractedTaskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Text(
                    text = "Starter: ${synthesis.firstActionStep}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = NeuroFlowPaletteV2.ReboundCyan,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onConvertToTaskClick(synthesis) }
            ) {
                Text(
                    text = "+ Add Task",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun CategorizedSettingsHubV2(
    chronotypeSummary: String,
    schedulerSummary: String,
    hyperFocusSummary: String,
    aiAndSyncSummary: String,
    onOpenChronobiologyAndSleep: () -> Unit,
    onOpenSchedulerAndWeights: () -> Unit,
    onOpenHyperFocusAndPermissions: () -> Unit,
    onOpenAiAndCloudSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        SettingsCategoryRowCard(
            icon = Icons.Default.Bedtime,
            accent = NeuroFlowPaletteV2.PeakAmber,
            title = "Chronobiology, MEQ & Sleep Logs",
            subtitle = chronotypeSummary,
            onClick = onOpenChronobiologyAndSleep
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.Tune,
            accent = NeuroFlowPaletteV2.ReboundCyan,
            title = "Auto-Scheduler, Protected Slots & Weights",
            subtitle = schedulerSummary,
            onClick = onOpenSchedulerAndWeights
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.LockClock,
            accent = NeuroFlowPaletteV2.DipIndigo,
            title = "HyperFocus Gate, Allowlist & Kiosk Mode",
            subtitle = hyperFocusSummary,
            onClick = onOpenHyperFocusAndPermissions
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.CloudSync,
            accent = NeuroFlowPaletteV2.OptimalEmerald,
            title = "Gemini AI Coach, Calendar & Cloud Backup",
            subtitle = aiAndSyncSummary,
            onClick = onOpenAiAndCloudSync
        )
    }
}

@Composable
private fun SettingsCategoryRowCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(accent, 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeuroFlowPaletteV2.TextTertiary
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2settingssubscreensandhyperfocusupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/settings/SubScreensAndHyperFocusUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/settings/SubScreensAndHyperFocusUpgradesV2.kt`
- **Lines of Code:** 355
- **Size:** 14,730 bytes (14.4 KB)

```kotlin
package com.neuroflow.app.presentation.v2.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

@Composable
fun UpgradedSleepPressureOverviewCard(
    snapshot: SleepPressureRepository.Snapshot,
    hasHealthConnectSync: Boolean,
    onSyncHealthConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.DipIndigo, 0.8f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.DipIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOMEOSTATIC SLEEP PRESSURE (PROCESS S)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Surface(
                    color = if (hasHealthConnectSync)
                        NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f)
                    else
                        NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onSyncHealthConnectClick() }
                ) {
                    Text(
                        text = if (hasHealthConnectSync) "Health Connect Active" else "Sync Wearable",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (hasHealthConnectSync) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowPaletteV2.PeakAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SleepStatColumn("Fatigue Zone", snapshot.adjustedFatigueZone.label, NeuroFlowPaletteV2.PeakAmber)
                SleepStatColumn("7d Debt Mult", "${"%.2f".format(snapshot.chronicRestrictionMultiplier)}x", NeuroFlowPaletteV2.RecoveryRose)
                SleepStatColumn(
                    "Sleep Inertia",
                    if (snapshot.isSleepInertiaActive) "Active (-${(snapshot.sleepInertiaPenalty * 100).roundToInt()}%)" else "Cleared",
                    NeuroFlowPaletteV2.ReboundCyan
                )
            }
        }
    }
}

@Composable
private fun SleepStatColumn(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeuroFlowPaletteV2.TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
    }
}

@Composable
fun ProtectedSlotPhysiologicalPreviewBar(
    selectedCategory: PtsCategory,
    onSelectCategory: (PtsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        Triple(PtsCategory.MEAL, "Meal (-8.5% Dip)", Icons.Default.Restaurant),
        Triple(PtsCategory.EXERCISE, "Exercise (+9.5% Boost)", Icons.Default.FitnessCenter),
        Triple(PtsCategory.MINDFULNESS, "NSDR (2.2x Recovery)", Icons.Default.SelfImprovement)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PHYSIOLOGICAL CURVE IMPACT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NeuroFlowPaletteV2.TextSecondary,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (category, label, icon) ->
                val isSelected = selectedCategory == category
                Surface(
                    color = if (isSelected) NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.16f) else NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) NeuroFlowPaletteV2.ReboundCyan else NeuroFlowPaletteV2.BorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectCategory(category) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) NeuroFlowPaletteV2.ReboundCyan else NeuroFlowPaletteV2.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) NeuroFlowPaletteV2.ReboundCyan else NeuroFlowPaletteV2.TextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradedPriorityWeightsTunerCard(
    weights: PriorityWeightProfile,
    onUpdateWeights: (PriorityWeightProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush()),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AUTO-SCHEDULER SCORING WEIGHTS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.PeakAmber,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            WeightSliderRow(
                label = "Deadline Urgency",
                value = weights.urgencyWeight.toFloat(),
                accent = NeuroFlowPaletteV2.RecoveryRose,
                onValueChange = { onUpdateWeights(weights.copy(urgencyWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Strategic Impact & Frog",
                value = weights.impactWeight.toFloat(),
                accent = NeuroFlowPaletteV2.PeakAmber,
                onValueChange = { onUpdateWeights(weights.copy(impactWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Circadian Energy Match",
                value = weights.circadianMatchWeight.toFloat(),
                accent = NeuroFlowPaletteV2.ReboundCyan,
                onValueChange = { onUpdateWeights(weights.copy(circadianMatchWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Low-Friction Momentum",
                value = weights.momentumWeight.toFloat(),
                accent = NeuroFlowPaletteV2.OptimalEmerald,
                onValueChange = { onUpdateWeights(weights.copy(momentumWeight = it.toDouble())) }
            )
        }
    }
}

@Composable
private fun WeightSliderRow(
    label: String,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Text(
                text = "${"%.1f".format(value)}x",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.2f..2.5f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            )
        )
    }
}

@Composable
fun HyperFocusLauncherStatusHeader(
    sessionModeLabel: String,
    remainingTasksOrMinutesLabel: String,
    earnedUnlockMinutes: Int,
    cloudBackupSynced: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.8f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sessionModeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = remainingTasksOrMinutesLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+${earnedUnlockMinutes}m Earned",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.OptimalEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (cloudBackupSynced) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = NeuroFlowPaletteV2.OptimalEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
```

---

