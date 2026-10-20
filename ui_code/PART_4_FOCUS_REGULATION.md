# neuroFlow UI Codebase — Focus Deep Work Cockpit & Audio Soundscape Synthesizer

**Description:** FocusScreenV2, FocusViewModelV2, FocusService, NeuralSoundscapePlayer/Generator, Solfeggio bell, and regulation sheets.  
**Generated for Vice Head Review:** 2026-10-20 16:59:40  
**Files:** 10  
**Lines of Code:** 3,478  
**Uncompressed Size:** 139.4 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusAndRegulationUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusfocusandregulationupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusCompletionNotifier.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusfocuscompletionnotifierkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusfocusscreenv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusfocusviewmodelv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusaudioneuralsoundscapegeneratorkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusaudioneuralsoundscapeplayerkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/SoundscapeType.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusaudiosoundscapetypekt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/components/KeepScreenOnEffect.kt`](#appsrcmainjavacomneuroflowapppresentationv2focuscomponentskeepscreenoneffectkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusenginefocustimingcalculatorkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`](#appsrcmainjavacomneuroflowapppresentationv2focusservicefocusservicekt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationv2focusfocusandregulationupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusAndRegulationUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusAndRegulationUpgradesV2.kt`
- **Lines of Code:** 511
- **Size:** 19,943 bytes (19.5 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.engine.WoopEngine
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlinx.coroutines.delay

/**
 * 1. Zen Micro-Step Spotlight Card for FocusScreenV2:
 * Shows ONLY the active micro-step in bold typography to prevent ADHD working-memory overload.
 */
@Composable
fun ZenMicroStepSpotlightCard(
    currentStepIndex: Int,
    totalSteps: Int,
    currentStepTitle: String,
    currentStepEstimatedMinutes: Int,
    onCompleteCurrentStep: () -> Unit,
    onSkipStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "STEP ${currentStepIndex + 1} OF ${totalSteps.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "~${currentStepEstimatedMinutes}m target",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = currentStepTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteCurrentStep()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowPaletteV2.OptimalEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Complete Step",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { onSkipStep() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Step",
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Interactive Physiological Sigh Guide Sheet (Double Inhale + Long Exhale)
 */
@Composable
fun InteractivePhysiologicalSighSheet(
    totalCycles: Int = 3,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var cycle by remember { mutableIntStateOf(1) }
    var breathStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(cycle, breathStage) {
        when (breathStage) {
            0 -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(2400L)
                breathStage = 1
            }
            1 -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(1100L)
                breathStage = 2
            }
            2 -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(5200L)
                if (cycle < totalCycles) {
                    cycle++
                    breathStage = 0
                } else {
                    onCompleted()
                }
            }
        }
    }

    val targetScale = when (breathStage) {
        0 -> 0.76f
        1 -> 1.0f
        else -> 0.34f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = when (breathStage) {
                0 -> 2300
                1 -> 950
                else -> 5000
            },
            easing = FastOutSlowInEasing
        ),
        label = "sigh_lung_scale"
    )
    val stageLabel = when (breathStage) {
        0 -> "1. Deep Nasal Inhale..."
        1 -> "2. Quick Top-Off Sniff In!"
        else -> "3. Long, Slow Mouth Exhale..."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PHYSIOLOGICAL SIGH DECOMPRESSION",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.ReboundCyan,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cycle $cycle of $totalCycles",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            val reboundCyan = NeuroFlowPaletteV2.ReboundCyan
            val dipIndigo = NeuroFlowPaletteV2.DipIndigo
            Canvas(modifier = Modifier.size(190.dp)) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = reboundCyan.copy(alpha = 0.10f),
                    radius = maxRadius
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            reboundCyan.copy(alpha = 0.55f),
                            dipIndigo.copy(alpha = 0.20f)
                        )
                    ),
                    radius = maxRadius * animatedScale
                )
            }
            Text(
                text = stageLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Two inhales maximally inflate alveoli; the extended exhale triggers vagal nerve slowing and clears acute executive friction.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 3. One-Tap Haptic Affordance Rating Sheet (AffordanceRatingSheet.kt)
 */
@Composable
fun UpgradedAffordanceRatingSheet(
    taskTitle: String,
    onRatingSelected: (Float) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(
        Triple(-2.0f, "😫", "Much Harder"),
        Triple(-1.0f, "😓", "Draining"),
        Triple(0.0f, "🎯", "As Expected"),
        Triple(1.0f, "✨", "Smooth"),
        Triple(2.0f, "⚡", "Energizing")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CALIBRATE YOUR RHYTHM",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.PeakAmber,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How did \"$taskTitle\" feel compared to what you expected?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1 tap updates your personal circadian curve and future task scheduling.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (score, emoji, label) ->
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRatingSelected(score)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Skip for now",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .clickable { onSkip() }
                .padding(8.dp)
        )
    }
}

/**
 * 4. Guided WOOP Obstacle & If-Then Plan Sheet (WoopPromptSheet.kt)
 */
@Composable
fun UpgradedWoopPromptSheet(
    taskTitle: String,
    dreadedTaskInsight: String?,
    onSaveWoopPlan: (wish: String, outcome: String, obstacle: String, ifThenPlan: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetObstacles = listOf(
        "I feel overwhelmed by how big it is",
        "I get distracted by my phone",
        "I don't know where to start",
        "I'm worried it won't be perfect"
    )
    var obstacle by remember { mutableStateOf(presetObstacles.first()) }
    var thenAction by remember { mutableStateOf("open the file and work for just 2 minutes.") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(22.dp)
    ) {
        Text(
            text = "WOOP FRICTION SHIELD",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.ReboundCyan,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Unblock \"$taskTitle\"",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (!dreadedTaskInsight.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "🏆 $dreadedTaskInsight",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.OptimalEmerald,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "1. Tap your main internal obstacle:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        presetObstacles.forEach { preset ->
            val selected = obstacle == preset
            Surface(
                color = if (selected) NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.22f) else NeuroFlowPaletteV2.SurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) NeuroFlowPaletteV2.DipIndigo else NeuroFlowPaletteV2.BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { obstacle = preset }
            ) {
                Text(
                    text = preset,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "2. Your automatic If-Then reflex:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = thenAction,
            onValueChange = { thenAction = it },
            prefix = {
                Text(
                    text = WoopEngine.generateIfThenPlan(obstacle),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.ReboundCyan
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val fullPlan = WoopEngine.generateIfThenPlan(obstacle) + thenAction.trim()
                onSaveWoopPlan(
                    "Complete $taskTitle",
                    "Feel relieved and build momentum",
                    obstacle,
                    fullPlan
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeuroFlowPaletteV2.ReboundCyan,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = "Lock In If-Then Plan",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusfocuscompletionnotifierkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusCompletionNotifier.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusCompletionNotifier.kt`
- **Lines of Code:** 124
- **Size:** 4,768 bytes (4.7 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class FocusCompletionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 1.6
        private const val FREQ_FUNDAMENTAL = 528.0 // Solfeggio C5 warm tone
        private const val FREQ_FIFTH = 792.0       // Perfect fifth G5 harmonic
    }

    fun playCompletionChimeAndHaptics(scope: CoroutineScope) {
        triggerHapticPulse()
        scope.launch(Dispatchers.Default) {
            playSynthesizedBell()
        }
    }

    private fun triggerHapticPulse() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            val timings = longArrayOf(0L, 120L, 100L, 220L)
            val effect = if (vibrator.hasAmplitudeControl()) {
                val amplitudes = intArrayOf(0, 160, 0, 220)
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            vibrator.vibrate(effect)
        }
    }

    private suspend fun playSynthesizedBell() {
        val audioManager = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
        var focusRequest: AudioFocusRequest? = null
        var track: AudioTrack? = null

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            focusRequest = req
            audioManager?.requestAudioFocus(req)

            val numSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Fast 15ms attack, smooth exponential decay
                val attack = (t / 0.015).coerceAtMost(1.0)
                val decay = exp(-t * 3.2)
                val envelope = attack * decay

                val wave = 0.70 * sin(2.0 * PI * FREQ_FUNDAMENTAL * t) +
                           0.30 * sin(2.0 * PI * FREQ_FIFTH * t)
                pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.45).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            delay((DURATION_SECONDS * 1000).toLong() + 100L)
        } catch (_: Throwable) {
            // Guard against audio hardware unavailability on headless CI or restricted audio HALs
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
            focusRequest?.let { req ->
                runCatching { audioManager?.abandonAudioFocusRequest(req) }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusfocusscreenv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`
- **Lines of Code:** 1,028
- **Size:** 49,502 bytes (48.3 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.presentation.v2.focus.components.KeepScreenOnEffect
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreenV2(
    onNavigateBack: () -> Unit,
    onNavigateToNextTask: (String) -> Unit = {},
    viewModel: FocusViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showParkThoughtSheet by remember { mutableStateOf(false) }
    var showSighSheet by remember { mutableStateOf(false) }
    var newThoughtText by remember { mutableStateOf("") }
    var newStepText by remember { mutableStateOf("") }
    var showWoopDetails by remember { mutableStateOf(false) }
    var showWoopPromptSheet by remember { mutableStateOf(false) }

    KeepScreenOnEffect(enabled = state.isSessionActive && !state.isPaused)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenExit()
        }
    }

    Scaffold(
        containerColor = NeuroFlowThemeV2Colors.Obsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.task?.title ?: "Focus Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 1
                        )
                        if (state.task?.isFrog == true) {
                            Text("🐸 Daily Frog (High Biological Leverage)", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.SageGreen)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showSighSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.SageGreen
                        ),
                        modifier = Modifier.padding(end = 8.dp).heightIn(min = 40.dp)
                    ) {
                        Text("🫁 60s Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeuroFlowThemeV2Colors.Obsidian)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Chips
            if (!state.isRunning && !state.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusTimerModeV2.entries.forEach { mode ->
                        FilterChip(
                            selected = state.timerMode == mode,
                            onClick = { viewModel.selectTimerMode(mode) },
                            label = { Text(mode.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            // Hero Task Card: Full prominence for Task Title, Frog status, and Next Micro-Step
            HeroTaskCard(
                task = state.task,
                nextStep = if (state.isRunning && !state.isPaused) null else state.microSteps.firstOrNull { !it.completed }
            )

            // Bioluminescent Radial Timer Ring (focused strictly on digits & flow status)
            RadialTimerGauge(
                timerMode = state.timerMode,
                remainingSeconds = state.remainingSeconds,
                elapsedSeconds = state.elapsedSeconds,
                isRunning = state.isRunning,
                isPaused = state.isPaused
            )

            // Primary Control Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button (min 64dp)
                Button(
                    onClick = {
                        if (!state.isRunning) {
                            viewModel.startTimer()
                        } else if (state.isPaused) {
                            viewModel.resumeTimer()
                        } else {
                            viewModel.pauseTimer()
                        }
                    },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                ) {
                    Icon(
                        imageVector = if (state.isRunning && !state.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning && !state.isPaused) "Pause" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Complete Task Button (touch target >= 48dp)
                Button(
                    onClick = { viewModel.completeTask() },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.SageGreenDark)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Park Thought Button (touch target >= 48dp)
                OutlinedIconButton(
                    onClick = { showParkThoughtSheet = true },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.WarmAmber)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Park Thought", tint = NeuroFlowThemeV2Colors.WarmAmber)
                }
            }

            // Neural Soundscapes Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Active Animated Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "🎧 Neural Soundscapes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            if (state.selectedSoundscape != SoundscapeType.NONE) {
                                Text(
                                    state.selectedSoundscape.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val isAudioActive = state.selectedSoundscape != SoundscapeType.NONE && state.isRunning && !state.isPaused
                        ActiveAudioIndicator(isActive = isAudioActive)
                    }

                    // Soundscape Selector Chips (Off, 40Hz Gamma 🧠, Deep Brown 🌊, Pink Rain 🌧️)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoundscapeType.entries.forEach { type ->
                            val isSelected = state.selectedSoundscape == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSoundscape(type) },
                                label = {
                                    Text(
                                        type.shortChipLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    // Headphone hint for 40Hz Gamma waves
                    AnimatedVisibility(visible = state.selectedSoundscape == SoundscapeType.GAMMA_40HZ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Stereo headphones recommended for 40 Hz binaural entrainment",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                    }

                    // Tactile Volume Slider (0% to 100%) with 48dp touch target
                    AnimatedVisibility(visible = state.selectedSoundscape != SoundscapeType.NONE) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Acoustic Volume",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                                Text(
                                    "${(state.soundscapeVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.soundscapeVolume <= 0.01f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                                    contentDescription = "Volume Down",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = state.soundscapeVolume,
                                    onValueChange = { viewModel.setSoundscapeVolume(it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        activeTrackColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                        inactiveTrackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume Up",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // WOOP Intention Card
            val woop = state.woopData
            if (woop != null && (woop.wish.isNotBlank() || woop.obstacle.isNotBlank() || woop.plan.isNotBlank())) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWoopDetails = !showWoopDetails },
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(18.dp))
                                Text("WOOP Mental Anchor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                            }
                            Text(if (showWoopDetails) "Hide" else "Show", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                        }

                        if (showWoopDetails) {
                            if (woop.wish.isNotBlank()) Text("Wish: ${woop.wish}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextPrimary)
                            if (woop.obstacle.isNotBlank()) Text("Obstacle: ${woop.obstacle}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.RoseCoral)
                            if (woop.plan.isNotBlank()) Text("Plan: ${woop.plan}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.SageGreen)
                        } else {
                            val summary = woop.plan.ifBlank { woop.wish }
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary, maxLines = 1)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWoopPromptSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Set WOOP Anchor",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                                Text(
                                    "Pre-commit Wish, Obstacle, and If-Then execution plan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showWoopPromptSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Set WOOP Anchor",
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Active Focus Mode: Zen Micro-Step Spotlight Card (or multi-step overview when paused/ready)
            val activeMicroSteps = state.microSteps
            val firstIncompleteIndex = activeMicroSteps.indices.firstOrNull { idx ->
                !activeMicroSteps[idx].completed && idx !in state.skippedMicroStepIndices
            } ?: activeMicroSteps.indexOfFirst { !it.completed }.takeIf { it != -1 } ?: 0
            val currentMicroStep = activeMicroSteps.getOrNull(firstIncompleteIndex)

            if (state.isRunning && !state.isPaused && currentMicroStep != null) {
                ZenMicroStepSpotlightCard(
                    currentStepIndex = firstIncompleteIndex,
                    totalSteps = activeMicroSteps.size,
                    currentStepTitle = currentMicroStep.text,
                    currentStepEstimatedMinutes = (state.task?.estimatedDurationMinutes ?: 25) / activeMicroSteps.size.coerceAtLeast(1),
                    onCompleteCurrentStep = { viewModel.toggleMicroStep(firstIncompleteIndex) },
                    onSkipStep = { viewModel.skipCurrentMicroStep(firstIncompleteIndex) }
                )
            } else {
                // Micro-Steps Decomposition Checklist
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val completedCount = state.microSteps.count { it.completed }
                        val totalSteps = state.microSteps.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪜 Micro-Steps Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (totalSteps > 0) {
                                Text("$completedCount/$totalSteps Done", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (totalSteps > 0) {
                            LinearProgressIndicator(
                                progress = { completedCount.toFloat() / totalSteps },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = NeuroFlowThemeV2Colors.SageGreen,
                                trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.autoBreakDownTask() },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Break It Down (Auto-Scaffold 3 Steps) 🪄", fontSize = 12.sp)
                            }
                        }

                    state.microSteps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleMicroStep(index) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = step.completed,
                                onCheckedChange = { viewModel.toggleMicroStep(index) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeuroFlowThemeV2Colors.SageGreen,
                                    uncheckedColor = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            )
                            Text(
                                text = step.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (step.completed) NeuroFlowThemeV2Colors.TextTertiary else NeuroFlowThemeV2Colors.TextPrimary,
                                textDecoration = if (step.completed) TextDecoration.LineThrough else null
                            )
                        }
                    }

                    // Add Step Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newStepText,
                            onValueChange = { newStepText = it },
                            placeholder = { Text("Add next small action...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.addMicroStep(newStepText)
                                newStepText = ""
                            },
                            enabled = newStepText.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add step", tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Executive Transition Buffer (Decompression Zone)
    LaunchedEffect(state.showDecompressionZone) {
        if (state.showDecompressionZone) {
            viewModel.startDecompressionTicker()
        }
    }

    if (state.showDecompressionZone) {
        val mins = state.decompressionRemainingSeconds / 60
        val secs = state.decompressionRemainingSeconds % 60
        val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDecompressionZone() },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🧠 Executive Decompression Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.SageGreen
                )

                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.decompressionRemainingSeconds > 0) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SageGreen
                )

                Text(
                    text = if (state.decompressionRemainingSeconds > 0)
                        "Cortisol & sympathetic arousal are resetting. Take 3 minutes of passive rest before your next cognitive sprint to consolidate working memory."
                    else
                        "Rest restored. Your prefrontal cortex has recharged for your next flow state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (state.isDecompressionSighActive) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "🫁 Physiological Sigh Active: ${state.decompressionSighRemainingSeconds}s",
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.SageGreen
                            )
                            Text(
                                "Inhale deeply through nose... top-off inhale... slow long exhale through mouth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (state.decompressionRemainingSeconds > 0) {
                    FilledTonalButton(
                        onClick = { viewModel.triggerDecompressionSigh() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.SageGreen
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("🫁 Guided 60s Physiological Sigh", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = { viewModel.readyNowBypassDecompression() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(
                        if (state.decompressionRemainingSeconds > 0) "⚡ Ready Now (Skip Buffer)" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Completion Celebration Overlay
    if (state.isCompleted && !state.showDecompressionZone) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = {
                Text("🎉 Focus Session Completed!", fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.SageGreen)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outstanding focus. You conquered '${state.task?.title}' and earned +${state.pointsEarned} dopamine points.", style = MaterialTheme.typography.bodyMedium)
                    if (state.nextTaskId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Next in queue: ${state.nextTaskTitle}", fontWeight = FontWeight.SemiBold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                }
            },
            confirmButton = {
                if (state.nextTaskId != null) {
                    Button(
                        onClick = { onNavigateToNextTask(state.nextTaskId!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                    ) {
                        Text("Start Next Task")
                    }
                } else {
                    Button(onClick = onNavigateBack) {
                        Text("Return to Flow")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("Close")
                }
            }
        )
    }

    // Thought Parking Lot Bottom Sheet
    if (showParkThoughtSheet) {
        ModalBottomSheet(
            onDismissRequest = { showParkThoughtSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("💡 Distraction Parking Lot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Jot down your intrusive thought so your working memory stays clear. It will be saved as a low-energy task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )

                OutlinedTextField(
                    value = newThoughtText,
                    onValueChange = { newThoughtText = it },
                    placeholder = { Text("e.g. Call dentist, check order status...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.parkIntrusiveThought(newThoughtText)
                        newThoughtText = ""
                        showParkThoughtSheet = false
                    },
                    enabled = newThoughtText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.WarmAmber)
                ) {
                    Text("Park Thought & Resume Flow", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSighSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSighSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            InteractivePhysiologicalSighSheet(
                totalCycles = 3,
                onCompleted = { showSighSheet = false }
            )
        }
    }

    if (state.showAffordanceRating && !state.affordanceRatingSubmitted) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissAffordanceRating() },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            UpgradedAffordanceRatingSheet(
                taskTitle = state.task?.title ?: "Focus Session",
                onRatingSelected = { rating -> viewModel.submitAffordanceRating(rating) },
                onSkip = { viewModel.dismissAffordanceRating() }
            )
        }
    }

    if (showWoopPromptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWoopPromptSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            UpgradedWoopPromptSheet(
                taskTitle = state.task?.title ?: "Focus Session",
                dreadedTaskInsight = null,
                onSaveWoopPlan = { wish, outcome, obstacle, ifThenPlan ->
                    viewModel.saveWoop(wish, outcome, obstacle, ifThenPlan)
                    showWoopPromptSheet = false
                },
                onDismiss = { showWoopPromptSheet = false }
            )
        }
    }
}

@Composable
fun RadialTimerGauge(
    timerMode: FocusTimerModeV2,
    remainingSeconds: Long,
    elapsedSeconds: Long,
    isRunning: Boolean,
    isPaused: Boolean
) {
    val totalSeconds = if (timerMode == FocusTimerModeV2.FLOW_STOPWATCH) 3600L else timerMode.durationSeconds
    val currentSeconds = if (timerMode == FocusTimerModeV2.FLOW_STOPWATCH) elapsedSeconds else remainingSeconds
    val progress = (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val minutes = currentSeconds / 60
    val seconds = currentSeconds % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .size(230.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
        val ringColor = NeuroFlowThemeV2Colors.ElectricIndigo
        val ringGlow = NeuroFlowThemeV2Colors.ElectricIndigoGlow

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated progress ring
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        ringColor,
                        ringGlow,
                        ringColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = NeuroFlowThemeV2Colors.TextPrimary,
                letterSpacing = 1.sp
            )
            val statusLabel = when {
                !isRunning -> "READY"
                isPaused -> "PAUSED"
                else -> "FLOW IN PROGRESS"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    !isRunning -> NeuroFlowThemeV2Colors.TextSecondary
                    isPaused -> NeuroFlowThemeV2Colors.WarmAmber
                    else -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
                }
            )
        }
    }
}

@Composable
fun ActiveAudioIndicator(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioBars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(22.dp)
    ) {
        val barColor = if (isActive) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
        val barHeights = if (isActive) listOf(h1, h2, h3, h4) else listOf(4f, 4f, 4f, 4f)
        barHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(barColor, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
fun HeroTaskCard(
    task: TaskEntity?,
    nextStep: StepByStepPlanStep?,
    modifier: Modifier = Modifier
) {
    if (task == null) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.isFrog) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeuroFlowThemeV2Colors.SageGreenDark.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen)
                    ) {
                        Text(
                            "🐸 Daily Frog",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.SageGreen
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                ) {
                    Text(
                        "${task.estimatedDurationMinutes}m",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary,
                maxLines = 3
            )

            if (nextStep != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎯", fontSize = 12.sp)
                        Text(
                            text = "Next: ${nextStep.text}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusfocusviewmodelv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
- **Lines of Code:** 749
- **Size:** 28,478 bytes (27.8 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.presentation.v2.focus.audio.NeuralSoundscapePlayer
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.v2.focus.engine.FocusTimingCalculator
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.neuroflow.app.data.local.entity.startOfLocalDay
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

enum class FocusTimerModeV2(val durationSeconds: Long, val title: String) {
    JUST_5_MIN(300L, "5m Micro-Start"),
    POMODORO_25(1500L, "25m Pomodoro"),
    ULTRADIAN_90(5400L, "90m Ultradian Block"),
    FLOW_STOPWATCH(0L, "Flow Stopwatch")
}

data class FocusUiStateV2(
    val task: TaskEntity? = null,
    val timerMode: FocusTimerModeV2 = FocusTimerModeV2.POMODORO_25,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val remainingSeconds: Long = 1500L,
    val activeSessionId: String? = null,
    val startedAtMillis: Long = 0L,
    val accumulatedPauseMillis: Long = 0L,
    val pausedAtMillis: Long? = null,
    val woopData: WoopEntity? = null,
    val microSteps: List<StepByStepPlanStep> = emptyList(),
    val isCompleted: Boolean = false,
    val pointsEarned: Int = 0,
    val nextTaskId: String? = null,
    val nextTaskTitle: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val showAffordanceRating: Boolean = false,
    val affordanceRatingSubmitted: Boolean = false,
    val selectedSoundscape: SoundscapeType = SoundscapeType.NONE,
    val soundscapeVolume: Float = 0.7f,
    val showDecompressionZone: Boolean = false,
    val decompressionRemainingSeconds: Int = 180,
    val isDecompressionSighActive: Boolean = false,
    val decompressionSighRemainingSeconds: Int = 0,
    val isDecompressionBypassed: Boolean = false,
    val skippedMicroStepIndices: Set<Int> = emptySet()
) {
    val isSessionActive: Boolean get() = isRunning
}

@HiltViewModel
class FocusViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val woopRepository: WoopRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    @ApplicationContext private val context: Context? = null,
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    private val focusCompletionNotifier: FocusCompletionNotifier? = null
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(FocusUiStateV2())
    val uiState: StateFlow<FocusUiStateV2> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var pauseCount: Int = 0
    private var hasNotifiedCompletion = false

    val soundscapePlayer: NeuralSoundscapePlayer by lazy {
        NeuralSoundscapePlayer(context)
    }

    init {
        loadTask()
        observePreferences()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId)
            val woop = woopRepository.getByTaskId(taskId)
            val steps = task?.let { StepByStepPlanCodec.parse(it.ifThenPlan) } ?: emptyList()

            // Find next task
            val activeTasks = taskRepository.getActiveTasks()
            val next = activeTasks.firstOrNull { it.id != taskId }

            val openSession = sessionRepository.getOpenSessionForTask(taskId)?.takeIf { it.id.isNotBlank() && it.startedAt > 0L }
            if (openSession != null) {
                val startedAt = openSession.startedAt
                val accPause = openSession.totalPausedMs
                val pausedAt = openSession.pausedAt
                val isPaused = pausedAt != null
                val isRunning = !isPaused
                pauseCount = openSession.pauseResumeCount

                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAt,
                    accumulatedPauseMillis = accPause,
                    pausedAtMillis = pausedAt,
                    nowMillis = now,
                    targetDurationSeconds = _uiState.value.timerMode.durationSeconds
                )

                _uiState.update {
                    it.copy(
                        task = task,
                        woopData = woop,
                        microSteps = steps,
                        nextTaskId = next?.id,
                        nextTaskTitle = next?.title,
                        activeSessionId = openSession.id,
                        startedAtMillis = startedAt,
                        accumulatedPauseMillis = accPause,
                        pausedAtMillis = pausedAt,
                        isRunning = isRunning,
                        isPaused = isPaused,
                        elapsedSeconds = timing.elapsedSeconds,
                        remainingSeconds = timing.remainingSeconds,
                        isLoading = false
                    )
                }

                if (isRunning) {
                    startTicker()
                    FocusService.startService(
                        context = context,
                        taskId = taskId,
                        taskTitle = task?.title ?: "Deep Focus",
                        durationSeconds = _uiState.value.timerMode.durationSeconds,
                        startedAtMillis = startedAt
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        task = task,
                        woopData = woop,
                        microSteps = steps,
                        nextTaskId = next?.id,
                        nextTaskTitle = next?.title,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.dndEnabled == new.dndEnabled &&
                    old.totalFocusMinutes == new.totalFocusMinutes
                }
                .collect { prefs ->
                    _uiState.update { it.copy(preferences = prefs) }
                }
        }
    }

    fun selectTimerMode(mode: FocusTimerModeV2) {
        if (_uiState.value.isRunning) return
        hasNotifiedCompletion = false
        _uiState.update {
            it.copy(
                timerMode = mode,
                remainingSeconds = mode.durationSeconds,
                elapsedSeconds = 0L,
                startedAtMillis = 0L,
                accumulatedPauseMillis = 0L,
                pausedAtMillis = null
            )
        }
    }

    fun selectSoundscape(type: SoundscapeType) {
        _uiState.update { it.copy(selectedSoundscape = type) }
        if (_uiState.value.isRunning && !_uiState.value.isPaused) {
            if (type == SoundscapeType.NONE) {
                soundscapePlayer.pauseWithFadeOut()
            } else {
                soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
                soundscapePlayer.play(type)
            }
        }
    }

    fun setSoundscapeVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(soundscapeVolume = clamped) }
        soundscapePlayer.setVolume(clamped)
    }

    fun onScreenExit() {
        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.pauseWithFadeOut()
        }
    }

    fun saveWoop(wish: String, outcome: String, obstacle: String, plan: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = woopRepository.getByTaskId(taskId)
            val entity = existing?.copy(
                wish = wish,
                outcome = outcome,
                obstacle = obstacle,
                plan = plan,
                updatedAt = now
            ) ?: WoopEntity(
                taskId = taskId,
                wish = wish,
                outcome = outcome,
                obstacle = obstacle,
                plan = plan,
                createdAt = now,
                updatedAt = now
            )
            woopRepository.upsert(entity)
            _uiState.update { it.copy(woopData = entity) }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        if (_uiState.value.isPaused) {
            resumeTimer()
            return
        }

        val now = System.currentTimeMillis()
        val sessionId = _uiState.value.activeSessionId ?: UUID.randomUUID().toString()
        hasNotifiedCompletion = false

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                activeSessionId = sessionId,
                startedAtMillis = now,
                accumulatedPauseMillis = 0L,
                pausedAtMillis = null
            )
        }

        viewModelScope.launch {
            val existingSession = sessionRepository.getOpenSessionForTask(taskId)
            if (existingSession == null) {
                sessionRepository.insert(
                    TimeSessionEntity(
                        id = sessionId,
                        taskId = taskId,
                        startedAt = now
                    )
                )
            }
        }

        enableDndIfPermitted()
        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(_uiState.value.selectedSoundscape)
        }

        FocusService.startService(
            context = context,
            taskId = taskId,
            taskTitle = _uiState.value.task?.title ?: "Deep Focus",
            durationSeconds = _uiState.value.timerMode.durationSeconds,
            startedAtMillis = now
        )

        startTicker()
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                if (!isActive || !_uiState.value.isRunning || _uiState.value.isPaused) break
                tickSecond()
            }
        }
    }

    fun tickSecond() {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return
        var countdownHitZero = false
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            if (!current.isRunning || current.isPaused) return@update current
            val timing = FocusTimingCalculator.calculate(
                startedAtMillis = current.startedAtMillis,
                accumulatedPauseMillis = current.accumulatedPauseMillis,
                pausedAtMillis = current.pausedAtMillis,
                nowMillis = now,
                targetDurationSeconds = current.timerMode.durationSeconds
            )
            val elapsed = maxOf(current.elapsedSeconds + 1, timing.elapsedSeconds)
            val isStopwatch = current.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
            val remaining = if (isStopwatch) 0L else maxOf(0L, current.timerMode.durationSeconds - elapsed)
            val isExpired = (!isStopwatch) && (elapsed >= current.timerMode.durationSeconds)

            if (isExpired) {
                countdownHitZero = true
            }
            current.copy(
                elapsedSeconds = elapsed,
                remainingSeconds = remaining
            )
        }
        if (countdownHitZero) {
            soundscapePlayer.pauseWithFadeOut()
            if (!hasNotifiedCompletion) {
                hasNotifiedCompletion = true
                focusCompletionNotifier?.playCompletionChimeAndHaptics(viewModelScope)
            }
        }
    }

    fun pauseTimer() {
        if (!_uiState.value.isRunning) return
        val now = System.currentTimeMillis()
        val currentPausedAt = _uiState.value.pausedAtMillis
        val newPausedAt = FocusTimingCalculator.onPause(currentPausedAt, now)
        pauseCount++

        timerJob?.cancel()
        timerJob = null
        disableDnd()
        soundscapePlayer.pauseWithFadeOut()

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = _uiState.value.startedAtMillis,
            accumulatedPauseMillis = _uiState.value.accumulatedPauseMillis,
            pausedAtMillis = newPausedAt,
            nowMillis = now,
            targetDurationSeconds = _uiState.value.timerMode.durationSeconds
        )

        val elapsed = maxOf(_uiState.value.elapsedSeconds, timing.elapsedSeconds)
        val isStopwatch = _uiState.value.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
        val remaining = if (isStopwatch) 0L else maxOf(0L, _uiState.value.timerMode.durationSeconds - elapsed)

        _uiState.update {
            it.copy(
                isPaused = true,
                isRunning = false,
                pausedAtMillis = newPausedAt,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining
            )
        }

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        pausedAt = newPausedAt,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
                        durationMinutes = elapsed / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }
        }

        FocusService.pauseService(context)
    }

    fun resumeTimer() {
        if (!_uiState.value.isPaused) {
            startTimer()
            return
        }

        val now = System.currentTimeMillis()
        val pausedAt = _uiState.value.pausedAtMillis
        val currentAcc = _uiState.value.accumulatedPauseMillis
        val newAcc = if (pausedAt != null) {
            val (resAcc, _) = FocusTimingCalculator.onResume(pausedAt, currentAcc, now)
            resAcc
        } else {
            currentAcc
        }

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = _uiState.value.startedAtMillis,
            accumulatedPauseMillis = newAcc,
            pausedAtMillis = null,
            nowMillis = now,
            targetDurationSeconds = _uiState.value.timerMode.durationSeconds
        )

        val elapsed = maxOf(_uiState.value.elapsedSeconds, timing.elapsedSeconds)
        val isStopwatch = _uiState.value.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
        val remaining = if (isStopwatch) 0L else maxOf(0L, _uiState.value.timerMode.durationSeconds - elapsed)

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                pausedAtMillis = null,
                accumulatedPauseMillis = newAcc,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining
            )
        }

        enableDndIfPermitted()
        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(_uiState.value.selectedSoundscape)
        }

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        pausedAt = null,
                        totalPausedMs = newAcc,
                        durationMinutes = timing.elapsedSeconds / 60f
                    )
                )
            }
        }

        FocusService.resumeService(context)
        startTicker()
    }

    fun completeTask() {
        timerJob?.cancel()
        timerJob = null
        disableDnd()
        soundscapePlayer.pauseWithFadeOut()
        FocusService.stopService(context)

        val now = System.currentTimeMillis()
        val durationSec = _uiState.value.elapsedSeconds
        val actualMinutes = max(1, (durationSec / 60).toInt())

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        endedAt = now,
                        pausedAt = null,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
                        durationMinutes = durationSec / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }

            val currentTask = _uiState.value.task ?: return@launch
            val updatedTask = currentTask.copy(
                totalTimeTrackedMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes
            )
            taskRepository.update(updatedTask)

            val points = 50 + if (updatedTask.isFrog) 50 else 0 + (updatedTask.impactScore / 2)
            taskRepository.completeAndRecur(updatedTask, now)

            preferencesDataStore.updatePreferences { prefs ->
                val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                val newCompleted = prefs.totalTasksCompleted + 1
                val lastActive = prefs.lastActiveDate
                val todayStart = startOfLocalDay(now)
                val isNewDay = lastActive < todayStart
                val isConsecutive = lastActive >= todayStart - 86_400_000L && isNewDay
                val newStreak = when {
                    isConsecutive -> prefs.dailyStreak + 1
                    isNewDay -> 1
                    else -> prefs.dailyStreak
                }
                val longest = maxOf(prefs.longestStreak, newStreak)
                prefs.copy(
                    totalFocusMinutes = newFocusMinutes,
                    totalTasksCompleted = newCompleted,
                    dailyStreak = newStreak,
                    longestStreak = longest,
                    lastActiveDate = now
                )
            }

            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isCompleted = true,
                    showAffordanceRating = true,
                    pointsEarned = points,
                    showDecompressionZone = true,
                    decompressionRemainingSeconds = 180,
                    isDecompressionSighActive = false,
                    decompressionSighRemainingSeconds = 0,
                    isDecompressionBypassed = false
                )
            }
        }
    }

    fun submitAffordanceRating(rating: Float) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(
                task.copy(
                    affectiveForecastError = rating,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(showAffordanceRating = false, affordanceRatingSubmitted = true) }
        }
    }

    fun dismissAffordanceRating() {
        _uiState.update { it.copy(showAffordanceRating = false) }
    }

    private var decompressionJob: Job? = null

    fun startDecompressionTicker() {
        decompressionJob?.cancel()
        decompressionJob = viewModelScope.launch {
            while (isActive && _uiState.value.showDecompressionZone && _uiState.value.decompressionRemainingSeconds > 0) {
                delay(1000)
                if (!isActive || !_uiState.value.showDecompressionZone) break
                tickDecompressionSecond()
            }
        }
    }

    fun tickDecompressionSecond() {
        _uiState.update { current ->
            if (!current.showDecompressionZone) return@update current
            val newRemaining = maxOf(0, current.decompressionRemainingSeconds - 1)
            val newSighRemaining = if (current.isDecompressionSighActive) maxOf(0, current.decompressionSighRemainingSeconds - 1) else 0
            val newSighActive = current.isDecompressionSighActive && newSighRemaining > 0
            current.copy(
                decompressionRemainingSeconds = newRemaining,
                isDecompressionSighActive = newSighActive,
                decompressionSighRemainingSeconds = newSighRemaining
            )
        }
    }

    fun triggerDecompressionSigh() {
        _uiState.update {
            it.copy(
                isDecompressionSighActive = true,
                decompressionSighRemainingSeconds = 60
            )
        }
    }

    fun readyNowBypassDecompression() {
        decompressionJob?.cancel()
        decompressionJob = null
        _uiState.update {
            it.copy(
                decompressionRemainingSeconds = 0,
                isDecompressionSighActive = false,
                decompressionSighRemainingSeconds = 0,
                isDecompressionBypassed = true,
                showDecompressionZone = false
            )
        }
    }

    fun dismissDecompressionZone() {
        decompressionJob?.cancel()
        decompressionJob = null
        _uiState.update { it.copy(showDecompressionZone = false) }
    }

    fun autoBreakDownTask() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val generated = behavioralSupportService.generateMicroStepsWithAi(
                taskTitle = task.title,
                estimatedMinutes = task.estimatedDurationMinutes,
                apiKey = prefs.geminiApiKey,
                useAi = prefs.geminiAiEnabled
            )
            val microStepEntities = generated.mapIndexed { index, suggestion ->
                TaskMicroStepEntity(
                    taskId = task.id,
                    title = suggestion.title,
                    sortOrder = index,
                    estimatedMinutes = suggestion.estimatedMinutes
                )
            }
            val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
            val serialized = StepByStepPlanCodec.serialize(planSteps)
            _uiState.update { it.copy(microSteps = planSteps) }
            taskRepository.deleteMicroStepsByTaskId(task.id)
            taskRepository.insertMicroSteps(microStepEntities)
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun enableDndIfPermitted() {
        try {
            if (!_uiState.value.preferences.dndEnabled || context == null) return
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (_: Exception) {}
    }

    private fun disableDnd() {
        try {
            if (context == null) return
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: Exception) {}
    }

    private fun updateSessionRecord(isOpen: Boolean) {
        val duration = _uiState.value.elapsedSeconds
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        endedAt = if (isOpen) null else now,
                        pausedAt = _uiState.value.pausedAtMillis,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
                        durationMinutes = duration / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }
        }
    }

    fun toggleMicroStep(index: Int) {
        val currentSteps = _uiState.value.microSteps
        if (index !in currentSteps.indices) return

        val updated = currentSteps.toMutableList()
        val step = updated[index]
        updated[index] = step.copy(completed = !step.completed)

        val serialized = StepByStepPlanCodec.serialize(updated)
        _uiState.update { it.copy(microSteps = updated, skippedMicroStepIndices = it.skippedMicroStepIndices - index) }

        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun skipCurrentMicroStep(index: Int) {
        _uiState.update { current ->
            val nextSkipped = current.skippedMicroStepIndices + index
            val remainingIncomplete = current.microSteps.indices.filter { !current.microSteps[it].completed }
            if (remainingIncomplete.all { it in nextSkipped }) {
                current.copy(skippedMicroStepIndices = setOf(index))
            } else {
                current.copy(skippedMicroStepIndices = nextSkipped)
            }
        }
    }

    fun addMicroStep(stepText: String) {
        if (stepText.isBlank()) return
        val currentSteps = _uiState.value.microSteps
        val updated = currentSteps + StepByStepPlanStep(stepText.trim(), false)
        val serialized = StepByStepPlanCodec.serialize(updated)

        _uiState.update { it.copy(microSteps = updated) }

        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun parkIntrusiveThought(thoughtText: String) {
        if (thoughtText.isBlank()) return
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = thoughtText.trim(),
                quadrant = Quadrant.DELEGATE,
                energyLevel = EnergyLevel.LOW,
                estimatedDurationMinutes = 10,
                impactScore = 30
            )
            taskRepository.insert(newTask)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        decompressionJob?.cancel()
        disableDnd()
        soundscapePlayer.release()
        FocusService.stopService(context)
        if (_uiState.value.isRunning && !_uiState.value.isCompleted) {
            updateSessionRecord(isOpen = false)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusaudioneuralsoundscapegeneratorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`
- **Lines of Code:** 166
- **Size:** 6,883 bytes (6.7 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin DSP Audio Generator for Neural Soundscapes.
 *
 * Implements 3 neuroscience-backed audio streams:
 * 1. 40 Hz Gamma Binaural Beats (200 Hz carrier Left, 240 Hz Right) for prefrontal cortex synchrony.
 * 2. Deep Brown Noise (1/f² stochastic acoustic masking using stereo leaky random walk).
 * 3. Flow Pink Noise (Paul Kellet 1/f filter approximation for calming spectral density).
 *
 * Frame-interpolated smooth volume attenuation and fade-out prevent audio pops and clicks.
 * Pure JVM Kotlin code without Android framework dependencies (100% unit-testable).
 */
class NeuralSoundscapeGenerator(
    val sampleRate: Int = 44100
) {
    companion object {
        const val CARRIER_HZ = 200.0
        const val GAMMA_BEAT_HZ = 40.0
        const val LEFT_FREQ = CARRIER_HZ
        const val RIGHT_FREQ = CARRIER_HZ + GAMMA_BEAT_HZ // 240.0 Hz
    }

    // Phase accumulators for binaural carriers
    private var phaseLeft = 0.0
    private var phaseRight = 0.0

    // Brownian motion state (independent stereo leaky random walks)
    private var brownLeft = 0.0f
    private var brownRight = 0.0f

    // Kellet 1/f Pink noise filter state (independent stereo poles)
    private var b0L = 0f; private var b1L = 0f; private var b2L = 0f; private var b3L = 0f; private var b4L = 0f; private var b5L = 0f; private var b6L = 0f
    private var b0R = 0f; private var b1R = 0f; private var b2R = 0f; private var b3R = 0f; private var b4R = 0f; private var b5R = 0f; private var b6R = 0f

    // Volume tracking for smooth anti-pop frame interpolation
    var currentVolume: Float = 0.0f
        private set

    /**
     * Resets generator internal filter states, phases, and volume state.
     */
    fun reset() {
        phaseLeft = 0.0
        phaseRight = 0.0
        brownLeft = 0.0f
        brownRight = 0.0f
        b0L = 0f; b1L = 0f; b2L = 0f; b3L = 0f; b4L = 0f; b5L = 0f; b6L = 0f
        b0R = 0f; b1R = 0f; b2R = 0f; b3R = 0f; b4R = 0f; b5R = 0f; b6R = 0f
        currentVolume = 0.0f
    }

    /**
     * Sets volume immediately without interpolation (useful for test setup).
     */
    fun setVolumeInstant(volume: Float) {
        currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
    }

    /**
     * Generates stereo 16-bit PCM frames into [outputBuffer] (interleaved L, R).
     * [outputBuffer] size must be at least [numFrames] * 2.
     * [volumeMultiplier] targets a range of 0.0f .. 1.0f.
     * Linearly interpolates volume across [numFrames] to eliminate audio pops and clicks.
     */
    fun generateFrames(
        type: SoundscapeType,
        outputBuffer: ShortArray,
        numFrames: Int,
        volumeMultiplier: Float = 0.7f
    ) {
        require(numFrames >= 0) { "numFrames cannot be negative: $numFrames" }
        require(outputBuffer.size >= numFrames * 2) {
            "outputBuffer size (${outputBuffer.size}) is smaller than required frames (${numFrames * 2})"
        }

        if (numFrames == 0) return

        if (type == SoundscapeType.NONE) {
            outputBuffer.fill(0, 0, numFrames * 2)
            currentVolume = 0.0f
            return
        }

        val safeVolume = if (volumeMultiplier.isNaN()) 0f else volumeMultiplier
        val targetVol = safeVolume.coerceIn(0f, 1f)
        val startVol = if (currentVolume.isNaN()) 0f else currentVolume

        // If target is 0 and current is 0, silence
        if (targetVol <= 0.00001f && startVol <= 0.00001f) {
            outputBuffer.fill(0, 0, numFrames * 2)
            currentVolume = 0.0f
            return
        }

        val stepLeft = 2.0 * PI * LEFT_FREQ / sampleRate
        val stepRight = 2.0 * PI * RIGHT_FREQ / sampleRate
        val volDelta = targetVol - startVol

        for (i in 0 until numFrames) {
            // Frame-interpolated volume attenuation / ramp
            val fraction = (i + 1).toFloat() / numFrames
            val vol = (startVol + volDelta * fraction).coerceIn(0f, 1f)

            val leftSampleFloat: Float
            val rightSampleFloat: Float

            when (type) {
                SoundscapeType.GAMMA_40HZ -> {
                    leftSampleFloat = sin(phaseLeft).toFloat()
                    rightSampleFloat = sin(phaseRight).toFloat()
                    phaseLeft += stepLeft
                    phaseRight += stepRight
                    if (phaseLeft >= 2.0 * PI) phaseLeft -= 2.0 * PI
                    if (phaseRight >= 2.0 * PI) phaseRight -= 2.0 * PI
                }
                SoundscapeType.BROWN_NOISE -> {
                    val whiteL = Random.nextFloat() * 2f - 1f
                    val whiteR = Random.nextFloat() * 2f - 1f
                    brownLeft = (brownLeft + (0.02f * whiteL)) / 1.02f
                    brownRight = (brownRight + (0.02f * whiteR)) / 1.02f
                    leftSampleFloat = (brownLeft * 3.2f).coerceIn(-1f, 1f)
                    rightSampleFloat = (brownRight * 3.2f).coerceIn(-1f, 1f)
                }
                SoundscapeType.PINK_NOISE -> {
                    val wL = Random.nextFloat() * 2f - 1f
                    val wR = Random.nextFloat() * 2f - 1f
                    b0L = 0.99886f * b0L + wL * 0.0555179f
                    b1L = 0.99332f * b1L + wL * 0.0750759f
                    b2L = 0.96900f * b2L + wL * 0.1538520f
                    b3L = 0.86650f * b3L + wL * 0.3104856f
                    b4L = 0.55000f * b4L + wL * 0.5329522f
                    b5L = -0.7616f * b5L - wL * 0.0168980f
                    leftSampleFloat = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + wL * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6L = wL * 0.115926f

                    b0R = 0.99886f * b0R + wR * 0.0555179f
                    b1R = 0.99332f * b1R + wR * 0.0750759f
                    b2R = 0.96900f * b2R + wR * 0.1538520f
                    b3R = 0.86650f * b3R + wR * 0.3104856f
                    b4R = 0.55000f * b4R + wR * 0.5329522f
                    b5R = -0.7616f * b5R - wR * 0.0168980f
                    rightSampleFloat = ((b0R + b1R + b2R + b3R + b4R + b5R + b6R + wR * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6R = wR * 0.115926f
                }
                SoundscapeType.NONE -> {
                    leftSampleFloat = 0f
                    rightSampleFloat = 0f
                }
            }

            // Convert to 16-bit signed PCM
            val leftPcm = (leftSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            val rightPcm = (rightSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()

            outputBuffer[2 * i] = leftPcm
            outputBuffer[2 * i + 1] = rightPcm
        }

        currentVolume = targetVol
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusaudioneuralsoundscapeplayerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`
- **Lines of Code:** 377
- **Size:** 13,214 bytes (12.9 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Streaming AudioTrack player for Neural Soundscapes.
 *
 * Encapsulates:
 * - Real-time PCM stereo streaming on a dedicated playback thread
 * - Audio focus management with [AudioManager.OnAudioFocusChangeListener]
 * - Smooth anti-pop volume adjustment and frame-interpolated fade-out
 * - Leak-proof release and lifecycle cleanup
 */
class NeuralSoundscapePlayer(
    private val context: Context? = null,
    val generator: NeuralSoundscapeGenerator = NeuralSoundscapeGenerator(SAMPLE_RATE)
) {
    companion object {
        private const val TAG = "NeuralSoundscapePlayer"
        const val SAMPLE_RATE = 44100
        const val CHUNK_FRAMES = 1024 // ~23.2ms latency per synthesis buffer
        const val FADE_DURATION_MS = 400L
        const val FADE_OUT_DURATION_MS = 400L
    }

    private val audioManager: AudioManager? by lazy {
        try {
            context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to get AudioManager: ${t.message}")
            null
        }
    }

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    var currentType: SoundscapeType = SoundscapeType.NONE
        private set

    @Volatile
    var currentVolume: Float = 0.7f
        private set

    private val isPlaying = AtomicBoolean(false)
    private val isFadingOut = AtomicBoolean(false)
    private var wasPlayingBeforeFocusLoss = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = false
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying.get()) {
                    wasPlayingBeforeFocusLoss = true
                    pauseWithFadeOut()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    play(currentType)
                }
            }
        }
    }

    private var audioFocusRequest: Any? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseWithFadeOut()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private fun registerNoisyReceiver() {
        if (context != null && !isNoisyReceiverRegistered) {
            try {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(noisyReceiver, filter)
                isNoisyReceiverRegistered = true
            } catch (t: Throwable) {
                Log.w(TAG, "Could not register noisy receiver: ${t.message}")
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (context != null && isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (t: Throwable) {
                Log.w(TAG, "Could not unregister noisy receiver: ${t.message}")
            } finally {
                isNoisyReceiverRegistered = false
            }
        }
    }

    /**
     * Starts or switches active soundscape stream.
     */
    @Synchronized
    fun play(type: SoundscapeType) {
        if (type == SoundscapeType.NONE) {
            pauseWithFadeOut()
            currentType = SoundscapeType.NONE
            return
        }

        currentType = type
        isFadingOut.set(false)

        if (isPlaying.get()) {
            // Already streaming: generator seamlessly updates synthesis stream
            return
        }

        requestAudioFocus()
        registerNoisyReceiver()
        initAudioTrackIfNeeded()

        try {
            audioTrack?.play()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack play failed: ${t.message}")
            unregisterNoisyReceiver()
            return
        }

        isPlaying.set(true)
        startPlaybackThread()
    }

    /**
     * Updates target soundscape volume (0.0f .. 1.0f).
     */
    @Synchronized
    fun setVolume(volume: Float) {
        currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
    }

    /**
     * Initiates smooth fade-out over [FADE_OUT_DURATION_MS] and then halts playback.
     */
    @Synchronized
    fun pauseWithFadeOut() {
        if (!isPlaying.get() || isFadingOut.get()) return
        isFadingOut.set(true)
    }

    /**
     * Stops playback immediately, flushes buffers, and abandons AudioFocus.
     */
    @Synchronized
    fun stop() {
        isPlaying.set(false)
        isFadingOut.set(false)
        stopPlaybackThread()
        unregisterNoisyReceiver()

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack pause/flush failed: ${t.message}")
        }
        abandonAudioFocus()
    }

    /**
     * Disposes AudioTrack and releases all resources.
     */
    @Synchronized
    fun release() {
        stop()
        unregisterNoisyReceiver()
        try {
            audioTrack?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack release failed: ${t.message}")
        } finally {
            audioTrack = null
        }
        generator.reset()
    }

    private fun initAudioTrackIfNeeded() {
        if (audioTrack != null) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, CHUNK_FRAMES * 2 * 2 * 2)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack init failed: ${t.message}")
            audioTrack = null
        }
    }

    private fun startPlaybackThread() {
        stopPlaybackThread()
        playbackThread = thread(name = "NeuralSoundscapeAudio", isDaemon = true) {
            val buffer = ShortArray(CHUNK_FRAMES * 2)
            var fadeStartVol = currentVolume
            var fadeStartTime = 0L
            val fadeInStartTime = System.currentTimeMillis()

            try {
                while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
                    val track = audioTrack
                    if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                        break
                    }

                    val type = currentType
                    if (type == SoundscapeType.NONE) {
                        break
                    }

                    val vol: Float
                    if (isFadingOut.get()) {
                        if (fadeStartTime == 0L) {
                            fadeStartTime = System.currentTimeMillis()
                            fadeStartVol = generator.currentVolume
                        }
                        val elapsed = System.currentTimeMillis() - fadeStartTime
                        val fraction = (elapsed.toFloat() / FADE_OUT_DURATION_MS).coerceIn(0f, 1f)
                        // Click-free raised cosine fade-out envelope
                        val cosineFadeFactor = (0.5 * (1.0 + kotlin.math.cos(kotlin.math.PI * fraction))).toFloat()
                        vol = (fadeStartVol * cosineFadeFactor).coerceAtLeast(0f)

                        generator.generateFrames(type, buffer, CHUNK_FRAMES, vol)

                        if (fraction >= 1f || vol <= 0.0001f) {
                            // Fade completed cleanly
                            try {
                                track.write(buffer, 0, buffer.size)
                                track.pause()
                                track.flush()
                            } catch (_: Throwable) {}
                            isPlaying.set(false)
                            isFadingOut.set(false)
                            abandonAudioFocus()
                            break
                        }
                    } else {
                        fadeStartTime = 0L
                        val baseVol = currentVolume
                        val elapsedIn = System.currentTimeMillis() - fadeInStartTime
                        val inFraction = (elapsedIn.toFloat() / FADE_DURATION_MS).coerceIn(0f, 1f)
                        // Click-free raised cosine fade-in envelope
                        val cosineFadeIn = if (inFraction < 1.0f) {
                            (0.5 * (1.0 - kotlin.math.cos(kotlin.math.PI * inFraction))).toFloat()
                        } else {
                            1.0f
                        }
                        vol = (baseVol * cosineFadeIn).coerceIn(0f, 1f)
                        generator.generateFrames(type, buffer, CHUNK_FRAMES, vol)
                    }

                    val written = track.write(buffer, 0, buffer.size)
                    if (written < 0) {
                        Log.w(TAG, "AudioTrack write returned: $written")
                        break
                    }
                }
            } finally {
                isPlaying.set(false)
                isFadingOut.set(false)
                abandonAudioFocus()
                try {
                    audioTrack?.stop()
                } catch (_: Throwable) {}
            }
        }
    }

    private fun stopPlaybackThread() {
        val thread = playbackThread
        playbackThread = null
        if (thread != null && thread.isAlive) {
            thread.interrupt()
            try {
                thread.join(200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequest = request
                am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not request audio focus: ${t.message}")
            true
        }
    }

    private fun abandonAudioFocus() {
        unregisterNoisyReceiver()
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let {
                    am.abandonAudioFocusRequest(it)
                }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not abandon audio focus: ${t.message}")
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusaudiosoundscapetypekt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/SoundscapeType.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/SoundscapeType.kt`
- **Lines of Code:** 36
- **Size:** 1,000 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.audio

/**
 * Neuroscience-backed soundscape categories for auditory stimulation during deep work.
 */
enum class SoundscapeType(
    val displayName: String,
    val subtitle: String,
    val emoji: String = "",
    val shortChipLabel: String = displayName
) {
    NONE(
        displayName = "Off",
        subtitle = "Silence",
        emoji = "",
        shortChipLabel = "Off"
    ),
    GAMMA_40HZ(
        displayName = "40 Hz Gamma",
        subtitle = "Binaural Prefrontal Synchrony",
        emoji = "🧠",
        shortChipLabel = "40Hz Gamma 🧠"
    ),
    BROWN_NOISE(
        displayName = "Deep Brown Noise",
        subtitle = "1/f² Stochastic Acoustic Masking",
        emoji = "🌊",
        shortChipLabel = "Deep Brown 🌊"
    ),
    PINK_NOISE(
        displayName = "Flow Pink Noise / Rain",
        subtitle = "1/f Calming Spectral Density",
        emoji = "🌧️",
        shortChipLabel = "Pink Rain 🌧️"
    )
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focuscomponentskeepscreenoneffectkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/components/KeepScreenOnEffect.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/components/KeepScreenOnEffect.kt`
- **Lines of Code:** 31
- **Size:** 1,009 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        val window = context.findActivity()?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusenginefocustimingcalculatorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`
- **Lines of Code:** 45
- **Size:** 1,839 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.engine

data class CalculatedTiming(
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val isExpired: Boolean
)

object FocusTimingCalculator {
    fun calculate(
        startedAtMillis: Long,
        accumulatedPauseMillis: Long,
        pausedAtMillis: Long?,
        nowMillis: Long,
        targetDurationSeconds: Long
    ): CalculatedTiming {
        if (startedAtMillis <= 0L) {
            val isStopwatch = targetDurationSeconds <= 0L
            val remainingSeconds = if (isStopwatch) 0L else maxOf(0L, targetDurationSeconds)
            return CalculatedTiming(0L, remainingSeconds, false)
        }
        val safeNow = maxOf(nowMillis, startedAtMillis)
        val effectiveElapsedMillis = if (pausedAtMillis != null) {
            val safePause = maxOf(pausedAtMillis, startedAtMillis)
            maxOf(0L, safePause - startedAtMillis - accumulatedPauseMillis)
        } else {
            maxOf(0L, safeNow - startedAtMillis - accumulatedPauseMillis)
        }
        val elapsedSeconds = effectiveElapsedMillis / 1000L
        val isStopwatch = targetDurationSeconds <= 0L
        val remainingSeconds = if (isStopwatch) 0L else maxOf(0L, targetDurationSeconds - elapsedSeconds)
        val isExpired = (!isStopwatch) && (elapsedSeconds >= targetDurationSeconds)
        return CalculatedTiming(elapsedSeconds, remainingSeconds, isExpired)
    }

    fun onPause(currentPausedAt: Long?, nowMillis: Long): Long {
        return currentPausedAt ?: nowMillis
    }

    fun onResume(pausedAtMillis: Long, accumulatedPauseMillis: Long, nowMillis: Long): Pair<Long, Long?> {
        val pauseDuration = maxOf(0L, nowMillis - pausedAtMillis)
        val newAccumulated = accumulatedPauseMillis + pauseDuration
        return Pair(newAccumulated, null)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2focusservicefocusservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`
- **Lines of Code:** 411
- **Size:** 16,127 bytes (15.7 KB)

```kotlin
package com.neuroflow.app.presentation.v2.focus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neuroflow.app.MainActivity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.presentation.v2.focus.engine.FocusTimingCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FocusService : Service() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var sessionRepository: SessionRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    private var taskId: String = ""
    private var taskTitle: String = "Deep Focus"
    private var durationSeconds: Long = 1500L
    private var startedAtMillis: Long = 0L
    private var accumulatedPauseMillis: Long = 0L
    private var pausedAtMillis: Long? = null
    private var isPaused: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            // Defensive: Headless execution / unit test environments
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START -> {
                taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: taskId
                taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: taskTitle
                durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                startedAtMillis = intent.getLongExtra(EXTRA_STARTED_AT_MILLIS, System.currentTimeMillis())
                accumulatedPauseMillis = 0L
                pausedAtMillis = null
                isPaused = false
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = durationSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = false
                )
                updateNotification()
                startTicker()
            }
            ACTION_PAUSE -> {
                if (!isPaused) {
                    val now = System.currentTimeMillis()
                    pausedAtMillis = FocusTimingCalculator.onPause(pausedAtMillis, now)
                    isPaused = true
                    _activeSession.value = _activeSession.value?.copy(isPaused = true)
                    updateNotification()
                }
            }
            ACTION_RESUME -> {
                if (isPaused) {
                    val now = System.currentTimeMillis()
                    val currentPause = pausedAtMillis ?: now
                    val (newAcc, _) = FocusTimingCalculator.onResume(currentPause, accumulatedPauseMillis, now)
                    accumulatedPauseMillis = newAcc
                    pausedAtMillis = null
                    isPaused = false
                    _activeSession.value = _activeSession.value?.copy(isPaused = false)
                    updateNotification()
                    startTicker()
                }
            }
            ACTION_COMPLETE -> {
                handleCompleteAndStop(taskId)
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (!isActive) break
                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAtMillis,
                    accumulatedPauseMillis = accumulatedPauseMillis,
                    pausedAtMillis = pausedAtMillis,
                    nowMillis = now,
                    targetDurationSeconds = durationSeconds
                )
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = timing.remainingSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = isPaused
                )
                if (!isPaused) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val notif = buildNotification()
            nm.notify(NOTIFICATION_ID, notif)
        } catch (_: Throwable) {
            // Defensive: headless / unit test
        }
    }

    private fun buildNotification(): Notification {
        val now = System.currentTimeMillis()
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = startedAtMillis,
            accumulatedPauseMillis = accumulatedPauseMillis,
            pausedAtMillis = pausedAtMillis,
            nowMillis = now,
            targetDurationSeconds = durationSeconds
        )

        val isStopwatch = durationSeconds <= 0L
        val totalSec = if (isStopwatch) 0 else durationSeconds.toInt()
        val elapsedSec = timing.elapsedSeconds.toInt()

        val formattedTime = formatTime(if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds)
        val contentText = if (isStopwatch) {
            if (isPaused) "$formattedTime elapsed (Paused)" else "$formattedTime elapsed"
        } else {
            if (isPaused) "$formattedTime remaining (Paused)" else "$formattedTime remaining"
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.procus.ACTION_OPEN_FOCUS"
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action1PendingIntent = if (isPaused) {
            val resumeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_RESUME }
            PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            val pauseIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val action1Title = if (isPaused) "Resume" else "Pause"
        val action1Icon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        val completeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_COMPLETE }
        val completePendingIntent = PendingIntent.getService(
            this,
            2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (taskTitle.isNotBlank()) taskTitle else "Deep Focus"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(totalSec, elapsedSec, isStopwatch)
            .setContentIntent(contentPendingIntent)
            .addAction(action1Icon, action1Title, action1PendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .build()
    }

    private fun formatTime(seconds: Long): String {
        val safe = maxOf(0L, seconds)
        val minutes = safe / 60
        val secs = safe % 60
        val mStr = minutes.toString().padStart(2, '0')
        val sStr = secs.toString().padStart(2, '0')
        return "$mStr:$sStr"
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live focus session countdown and controls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(false)
                    setSound(null, null)
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(channel)
            }
        } catch (_: Throwable) {
            // Defensive: headless / unit test
        }
    }

    private fun handleCompleteAndStop(activeTaskId: String) {
        tickerJob?.cancel()
        tickerJob = null
        serviceScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    if (::taskRepository.isInitialized && ::sessionRepository.isInitialized && activeTaskId.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        val openSession = sessionRepository.getOpenSessionForTask(activeTaskId)
                        if (openSession != null) {
                            val durationMins = ((now - openSession.startedAt) / 60000f).coerceAtLeast(1f)
                            sessionRepository.update(openSession.copy(endedAt = now, durationMinutes = durationMins))
                        }
                        val task = taskRepository.getById(activeTaskId)
                        if (task != null) {
                            taskRepository.completeAndRecur(task, now)
                        }
                    }
                } catch (_: Throwable) {
                    // Defensive: protect teardown flow
                }
            }
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        tickerJob?.cancel()
        tickerJob = null
        _activeSession.value = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
        _activeSession.value = null
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        data class ActiveFocusSession(
            val taskId: String,
            val taskTitle: String,
            val remainingSeconds: Long,
            val totalSeconds: Long,
            val isPaused: Boolean
        )

        private val _activeSession = kotlinx.coroutines.flow.MutableStateFlow<ActiveFocusSession?>(null)
        val activeSession: kotlinx.coroutines.flow.StateFlow<ActiveFocusSession?> = _activeSession

        const val CHANNEL_ID = "focus_live"
        const val CHANNEL_NAME = "Active Focus Session"
        const val NOTIFICATION_ID = 3001

        const val ACTION_START = "com.neuroflow.app.focus.ACTION_START"
        const val ACTION_PAUSE = "com.neuroflow.app.focus.ACTION_PAUSE"
        const val ACTION_RESUME = "com.neuroflow.app.focus.ACTION_RESUME"
        const val ACTION_COMPLETE = "com.neuroflow.app.focus.ACTION_COMPLETE"
        const val ACTION_STOP = "com.neuroflow.app.focus.ACTION_STOP"

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
        const val EXTRA_DURATION_SECONDS = "durationSeconds"
        const val EXTRA_STARTED_AT_MILLIS = "startedAtMillis"

        fun start(
            context: Context?,
            taskId: String,
            taskTitle: String,
            durationSeconds: Long,
            startedAtMillis: Long = System.currentTimeMillis()
        ) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, taskTitle)
                    putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                    putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {}
        }

        fun startService(
            context: Context?,
            taskId: String,
            taskTitle: String,
            durationSeconds: Long,
            startedAtMillis: Long = System.currentTimeMillis()
        ) = start(context, taskId, taskTitle, durationSeconds, startedAtMillis)

        fun pause(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_PAUSE
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun pauseService(context: Context?) = pause(context)

        fun resume(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_RESUME
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun resumeService(context: Context?) = resume(context)

        fun stop(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
                context.stopService(intent)
            } catch (_: Throwable) {}
        }

        fun stopService(context: Context?) = stop(context)
    }
}
```

---

