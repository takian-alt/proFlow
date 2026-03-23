package com.neuroflow.app.presentation.launcher.hyperfocus.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PermissionSetupScreen(onBothGranted: () -> Unit) {
    val context = LocalContext.current

    fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // Match full component name to avoid false positives
        return enabled.contains(
            "${context.packageName}/.presentation.launcher.hyperfocus.service.AppBlockingService",
            ignoreCase = true
        ) || enabled.contains("AppBlockingService", ignoreCase = true)
    }

    // If already granted on first render, call onBothGranted immediately
    LaunchedEffect(Unit) {
        if (isAccessibilityEnabled()) {
            onBothGranted()
        }
    }

    // Poll for accessibility grant
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (isAccessibilityEnabled()) {
                onBothGranted()
                break
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
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "One Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Hyper Focus needs the Accessibility Service to detect and block distracting apps while you work.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find \"${context.applicationInfo.loadLabel(context.packageManager)}\" in the list and enable it.",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Waiting for permission...",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
