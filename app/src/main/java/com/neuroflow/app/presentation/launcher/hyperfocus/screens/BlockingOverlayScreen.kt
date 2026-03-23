package com.neuroflow.app.presentation.launcher.hyperfocus.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusViewModel

@Composable
fun BlockingOverlayScreen(
    blockedPackage: String,
    viewModel: HyperFocusViewModel,
    navController: NavController
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isUnlockActive by viewModel.isUnlockActive.collectAsState()
    val unlockSecondsRemaining by viewModel.unlockSecondsRemaining.collectAsState()

    val context = LocalContext.current
    val appName = remember(blockedPackage) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(blockedPackage, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            blockedPackage
        }
    }

    // Compute next tier unlock minutes
    val nextTierUnlockMinutes = remember(progress.currentTier) {
        val tiers = RewardTier.entries
        val nextIndex = tiers.indexOf(progress.currentTier) + 1
        if (nextIndex < tiers.size) tiers[nextIndex].unlockMinutes else 0
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Lock icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. "HYPER FOCUS ACTIVE" heading
            Text(
                text = "HYPER FOCUS ACTIVE",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Blocked app name
            Text(
                text = appName,
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Subtitle
            Text(
                text = "Your focus is worth more than this.",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Progress bar
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Tasks complete label
            Text(
                text = "${progress.completedSinceActivation} / ${progress.totalTarget} tasks complete",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 7. Next tier hint (hidden when FULL)
            if (progress.currentTier != RewardTier.FULL) {
                Text(
                    text = "${progress.tasksToNextTier} more task(s) → unlock for ${nextTierUnlockMinutes}min",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. Unlock active banner
            if (isUnlockActive) {
                val timeLabel = unlockSecondsRemaining?.let { secs ->
                    "${secs / 60}m ${secs % 60}s"
                } ?: ""
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1B5E20)
                ) {
                    Text(
                        text = "🔓 UNLOCKED — $timeLabel remaining",
                        color = Color(0xFF69F0AE),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                // 9 & 10. Action buttons
                Button(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Focus Mode")
                }

                if (progress.currentTier > RewardTier.NONE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("code_entry") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter unlock code", color = Color.White)
                    }
                }
            }
        }
    }
}
