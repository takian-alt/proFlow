package com.neuroflow.app.feature.hyperfocus.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.neuroflow.app.feature.hyperfocus.util.HomeRoleUtil
import kotlinx.coroutines.delay

/**
 * Shown instead of the activation controls while the Home button still opens another launcher.
 * Without this the black home can be started and never seen, and the session can only be escaped
 * by pressing Home and living with whatever the real launcher shows.
 */
@Composable
fun HomeRoleSetupScreen(
    onGranted: () -> Unit,
    onSkip: () -> Unit = onGranted
) {
    val context = LocalContext.current

    var holdsRole by remember { mutableStateOf(HomeRoleUtil.holdsHomeRole(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            holdsRole = HomeRoleUtil.holdsHomeRole(context)
            if (holdsRole) {
                onGranted()
                break
            }
            delay(1000)
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
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (holdsRole) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (holdsRole) Color(0xFF69F0AE) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Home app",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Decides what the Home button opens",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Set NeuroFlow as your home app",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The black home appears when you press Home. You can set NeuroFlow as " +
                    "your home app, or continue with background app blocking.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_HOME_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose my home app →")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (holdsRole) "✓ NeuroFlow is your home app!"
                else "Waiting for you to choose NeuroFlow...",
                color = if (holdsRole) Color(0xFF69F0AE) else Color.Gray,
                fontSize = 13.sp,
                fontWeight = if (holdsRole) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue without changing home app", color = Color(0xFF80CBC4), fontSize = 14.sp)
            }
        }
    }
}
