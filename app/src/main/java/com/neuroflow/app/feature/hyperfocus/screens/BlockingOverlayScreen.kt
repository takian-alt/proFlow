package com.neuroflow.app.feature.hyperfocus.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.MainActivity
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import androidx.navigation.NavController
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.HyperFocusActivity
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun BlockingOverlayScreen(
    blockedPackage: String,
    viewModel: HyperFocusViewModel,
    navController: NavController
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val isPassActive by viewModel.isPassActive.collectAsState()
    val passSecondsRemaining by viewModel.passSecondsRemaining.collectAsState()
    val sessionSecondsRemaining by viewModel.sessionSecondsRemaining.collectAsState()

    val haptic = LocalHapticFeedback.current
    var showEmergencyDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTimeBasedSession = prefs.sessionMode == HyperFocusSessionMode.TIME_BASED
    
    val appName = remember(blockedPackage) { context.labelForPackage(blockedPackage) }

    // A pass names one app (D3). The card has to name the app the pass holds, not the app the
    // enforcer bounced — those differ the moment a pass is live and the user opens something else.
    val passAppName = remember(prefs.activePassPackage, blockedPackage) {
        context.labelForPackage(prefs.activePassPackage ?: blockedPackage)
    }

    // Check if this is a messenger app
    val isMessengerApp = remember(blockedPackage) {
        val messengerPackages = listOf(
            "com.facebook.orca",
            "com.facebook.mlite",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "org.thoughtcrime.securesms"
        )
        blockedPackage in messengerPackages
    }

    // Supportive focus guidance
    val blockedAppQuotes = listOf(
        "Notice the urge to switch. Take a slow inhale before deciding.",
        "Your goals are meaningful and worthy of this focused block.",
        "This moment builds neural momentum. Stay present.",
        "Your focus is your deepest creative superpower.",
        "Remember why you activated HyperFocus. You are doing important work.",
        "Every mindful choice strengthens cognitive resilience.",
        "Your future self will appreciate your intentionality.",
        "Urges are temporary waves. Ground yourself in your intention.",
        "You are capable of deep, deliberate work.",
        "This boundary protects what truly matters to you today.",
        "A mindful pause is a victory for cognitive clarity.",
        "Your attention is your most valuable cognitive asset.",
        "Reflect on your chosen direction and breathe.",
        "Deep focus creates quiet mastery.",
        "Honoring this boundary preserves your focus state.",
        "Your attention is safe in this sanctuary.",
        "Take a slow breath: Is this switch aligned with your intention?",
        "Mindful pause: choose what aligns best with your core mission."
    )

    val selectedQuote = remember(blockedPackage) {
        blockedAppQuotes.getOrNull(Random.nextInt(maxOf(1, blockedAppQuotes.size))) 
            ?: "Your focus is worth more than this."
    }

    // Animation state
    val lockScale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        lockScale.animateTo(1f, animationSpec = tween(600, easing = LinearEasing))
    }

    val messengerGlow = remember { Animatable(0.5f) }
    LaunchedEffect(isMessengerApp) {
        if (isMessengerApp) {
            while (true) {
                messengerGlow.animateTo(1f, animationSpec = tween(1000))
                messengerGlow.animateTo(0.5f, animationSpec = tween(1000))
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ============ LOCK ICON & HEADING ============
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(lockScale.value)
                        .background(
                            color = if (isMessengerApp) Color(0xFF2D1F1F) else Color(0xFF1A1A2E),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "HYPER FOCUS ACTIVE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = appName,
                    color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            // ============ LARGE MOTIVATIONAL QUOTE (MAIN FOCUS) ============
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                color = if (isMessengerApp) Color(0xFF2D1F1F) else Color(0xFF1A1A2E),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"",
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedQuote,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "\"",
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ============ TOP ALERT (IF MESSENGER) ============
            if (isMessengerApp) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFFF6B6B).copy(alpha = messengerGlow.value),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF2D1F1F)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Messenger Alert",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Messenger Quota Active",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High distraction alert - stay focused!",
                                color = Color(0xFFFF9999),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ============ TAMPER DETECTION WARNING ============
            if (prefs.isTamperDetected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFB71C1C),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF3E1B1B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Tamper Warning",
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Tamper detected",
                                color = Color(0xFFB71C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = prefs.tamperReason ?: "Unusual activity detected",
                                color = Color(0xFFD32F2F),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (isTimeBasedSession) {
                // ============ SESSION TIMER SECTION ============
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Focus Timer",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatSeconds(sessionSecondsRemaining),
                            color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apps stay blocked until this timer ends.",
                            color = Color(0xFFB0B0B0),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ============ PROGRESS SECTION ============
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Today's Progress",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                            trackColor = Color(0xFF2D2D44),
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${progress.completedSinceActivation} / ${progress.totalTarget}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "tasks completed",
                                    color = Color(0xFFB0B0B0),
                                    fontSize = 12.sp
                                )
                            }

                            if (progress.currentTier != RewardTier.FULL) {
                                val tiers = RewardTier.entries
                                val nextIndex = tiers.indexOf(progress.currentTier) + 1
                                val nextTier = if (nextIndex < tiers.size) tiers[nextIndex] else null
                                val unlockLabel = when {
                                    nextTier == null || nextTier == RewardTier.FULL -> "full unlock"
                                    else -> "${nextTier.unlockMinutes}min unlock"
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${progress.tasksToNextTier}",
                                        color = Color(0xFF4A90E2),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "more → $unlockLabel",
                                        color = Color(0xFFB0B0B0),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF1B5E20),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    color = Color(0xFF1B5E20)
                                ) {
                                    Text(
                                        text = "✓ Full Reward",
                                        color = Color(0xFF69F0AE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ============ PASS STATUS (IF ACTIVE) ============
            if (isPassActive) {
                val timeLabel = passSecondsRemaining?.let { secs ->
                    "${secs / 60}m ${secs % 60}s"
                } ?: ""
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color(0xFF69F0AE),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF1B5E20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔓 $passAppName is open",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Only that app. Everything else is still closed.",
                            color = Color(0xFFA5D6A7),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$timeLabel remaining",
                            color = Color(0xFF81C784),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (blockedPackage == prefs.activePassPackage) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(HyperFocusActivity.DESTINATION_LAUNCH)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color(0xFF69F0AE)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open $appName →", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back to home", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // ============ ACTION BUTTONS (LOCKED) ============
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val isPlanningPending =
                            prefs.state == com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING

                        if (isPlanningPending) {
                            navController.navigate(HyperFocusActivity.DESTINATION_PLANNING)
                            return@Button
                        }

                        val preferredTaskId = prefs.lockedTaskIds.firstOrNull { lockedId ->
                            activeTasks.any { it.id == lockedId }
                        } ?: activeTasks.firstOrNull()?.id

                        val focusIntent = Intent(context, MainActivity::class.java).apply {
                            if (!preferredTaskId.isNullOrBlank()) {
                                action = "com.procus.ACTION_OPEN_FOCUS"
                                putExtra("task_id", preferredTaskId)
                            }
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(focusIntent)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (prefs.state == com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING)
                            "Unlock apps / Finish session →"
                        else
                            "Go back to tasks",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (prefs.currentTier > RewardTier.NONE &&
                    prefs.state != com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.beginGate(blockedPackage)
                            navController.navigate(HyperFocusActivity.DESTINATION_GATE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open the gate", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (prefs.sessionMode == HyperFocusSessionMode.TASK_BASED) {
                    if (!prefs.emergencyUsed) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEmergencyDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Emergency Access (10 mins)", color = Color(0xFFB0B0B0))
                        }
                    } else {
                        Text(
                            text = "Emergency access already used for this session",
                            color = Color(0xFFB0B0B0),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0B0B0),
            title = {
                Text("Emergency Access", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will grant you 10 minutes of access to $appName.\n\n" +
                    "⚠️ WARNING: If you use this emergency bypass, you will lose your next intermediate rewards and must complete ALL daily tasks to earn any further unlocks."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEmergencyDialog = false
                        viewModel.grantEmergencyFor(blockedPackage)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Use Emergency", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showEmergencyDialog = false
                }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

private fun Context.labelForPackage(packageName: String): String = try {
    val info = packageManager.getApplicationInfo(packageName, 0)
    packageManager.getApplicationLabel(info).toString()
} catch (e: Exception) {
    packageName
}

private fun formatSeconds(seconds: Long?): String {
    val safe = (seconds ?: 0L).coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
