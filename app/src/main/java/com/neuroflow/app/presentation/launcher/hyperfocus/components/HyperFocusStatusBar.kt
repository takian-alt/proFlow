package com.neuroflow.app.presentation.launcher.hyperfocus.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusProgress
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusPreferences
import kotlinx.coroutines.delay

@Composable
fun HyperFocusStatusBar(
    prefs: HyperFocusPreferences,
    progress: HyperFocusProgress,
    unlockSecondsRemaining: Long?,
    sessionSecondsRemaining: Long?,
    onRewardsClick: () -> Unit,
    onPlanningClick: () -> Unit
) {
    if (!prefs.isActive) {
        Spacer(Modifier.height(0.dp))
        return
    }

    val context = LocalContext.current

    // Poll accessibility state every 3 seconds while session is active
    var accessibilityOk by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            accessibilityOk = com.neuroflow.app.presentation.launcher.hyperfocus.util.AccessibilityUtil
                .isAppBlockingServiceEnabled(context)
            delay(3_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Warning banner — shown when accessibility is off
        if (!accessibilityOk) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚠️ Blocking disabled — tap to fix",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fix →",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Normal status bar in a floating pill container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    unlockSecondsRemaining != null -> {
                        val minutes = unlockSecondsRemaining / 60
                        val seconds = unlockSecondsRemaining % 60
                        Text(
                            text = "🔓 UNLOCKED — ${minutes}m ${seconds}s remaining",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    prefs.sessionMode == HyperFocusSessionMode.TIME_BASED -> {
                        val remaining = sessionSecondsRemaining ?: 0L
                        val hours = remaining / 3600
                        val minutes = (remaining % 3600) / 60
                        val seconds = remaining % 60
                        val label = if (hours > 0) {
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        }

                        Text(
                            text = "🔒 FOCUS TIMER — $label remaining",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    prefs.state == HyperFocusState.FULL_REWARD_PENDING -> {
                        Text(
                            text = "🎉 All done! Add 3 tasks for tomorrow to claim your reward →",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { onPlanningClick() }
                        )
                    }

                    prefs.state == HyperFocusState.FULLY_UNLOCKED -> {
                        Text(
                            text = "🔓 Apps unlocked — great work today!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    else -> {
                        Text(
                            text = "🔒 FOCUS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (prefs.sessionMode == HyperFocusSessionMode.TASK_BASED) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${prefs.lockedTaskIds.size} tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Rewards →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRewardsClick() }
                        )
                    }
                }
            }
        }
    }
}
