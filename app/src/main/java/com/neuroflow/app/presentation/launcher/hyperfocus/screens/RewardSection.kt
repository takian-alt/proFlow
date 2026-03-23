package com.neuroflow.app.presentation.launcher.hyperfocus.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusProgress
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusPreferences

@Composable
fun RewardSection(
    prefs: HyperFocusPreferences,
    progress: HyperFocusProgress,
    claimedCode: String?,
    onClaimReward: () -> Unit,
    onDismissCode: () -> Unit
) {
    if (!prefs.isActive) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Rewards",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Current tier: ${prefs.currentTier.name}")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${progress.completedSinceActivation} / ${progress.totalTarget} tasks")
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClaimReward,
            enabled = prefs.currentTier > RewardTier.NONE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Claim Reward")
        }
        if (claimedCode != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = claimedCode,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismissCode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss")
            }
        }
    }
}
