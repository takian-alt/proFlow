package com.neuroflow.app.presentation.launcher.hyperfocus.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusProgress
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusPreferences

@Composable
fun HyperFocusStatusBar(
    prefs: HyperFocusPreferences,
    progress: HyperFocusProgress,
    unlockSecondsRemaining: Long?,
    onRewardsClick: () -> Unit,
    onPlanningClick: () -> Unit
) {
    if (!prefs.isActive) {
        Spacer(Modifier.height(0.dp))
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            unlockSecondsRemaining != null -> {
                val minutes = unlockSecondsRemaining / 60
                val seconds = unlockSecondsRemaining % 60
                Text(
                    text = "🔓 UNLOCKED — ${minutes}m ${seconds}s remaining",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            prefs.state == HyperFocusState.FULLY_UNLOCKED -> {
                Text(
                    text = "All done! Set tomorrow's tasks →",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onPlanningClick() }
                )
            }

            else -> {
                Text(
                    text = "🔒 FOCUS",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(4.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Rewards →",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onRewardsClick() }
                )
            }
        }
    }
}
