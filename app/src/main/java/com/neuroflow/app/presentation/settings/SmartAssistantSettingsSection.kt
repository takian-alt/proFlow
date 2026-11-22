package com.neuroflow.app.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.assistant.CheckInConfigEntry
import com.neuroflow.app.domain.assistant.SmartAssistantCodecs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistantCheckInSettingsSection(
    prefs: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onSendTestWellnessNudge: (() -> Unit)? = null
) {
    val configs = remember(prefs.checkInConfigJson) {
        SmartAssistantCodecs.parseCheckInConfigs(prefs.checkInConfigJson)
    }
    var editingConfig by remember { mutableStateOf<CheckInConfigEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_assistant_settings_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Toggle for Proactive In-App Check-Ins
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Proactive In-App Check-In Popups",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ask about meals, caffeine, workouts, naps & sleep around your chosen times",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = prefs.checkInsMasterEnabled,
                onCheckedChange = { enabled ->
                    onUpdatePreferences { it.copy(checkInsMasterEnabled = enabled) }
                },
                modifier = Modifier.testTag("settings_checkins_master_switch")
            )
        }

        if (prefs.checkInsMasterEnabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = "Tap any time pill to change when the assistant asks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            configs.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(entry.type.emoji, fontSize = 16.sp)
                        Text(
                            text = entry.type.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clickable(enabled = entry.enabled) { editingConfig = entry }
                            .testTag("settings_checkin_time_${entry.type.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Edit time for ${entry.type.displayName}",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = SmartAssistantCodecs.formatMinuteOfDay(entry.defaultMinuteOfDay),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = entry.enabled,
                        onCheckedChange = { checked ->
                            val updated = configs.map {
                                if (it.type == entry.type) it.copy(enabled = checked) else it
                            }
                            onUpdatePreferences {
                                it.copy(checkInConfigJson = SmartAssistantCodecs.encodeCheckInConfigs(updated))
                            }
                        },
                        modifier = Modifier.testTag("settings_checkin_switch_${entry.type.name.lowercase()}")
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Wellness & Focus Care Nudges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Energy, Hydration & Rest Nudges",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gentle prompts to drink water, rest after long focus, or switch to Easy Wins when energy drops",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = prefs.wellnessNudgesEnabled,
                onCheckedChange = { enabled ->
                    onUpdatePreferences { it.copy(wellnessNudgesEnabled = enabled) }
                },
                modifier = Modifier.testTag("settings_wellness_nudges_switch")
            )
        }

        if (prefs.wellnessNudgesEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hydration reminder every",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(60, 90, 120).forEach { mins ->
                        FilterChip(
                            selected = prefs.hydrationIntervalMinutes == mins,
                            onClick = { onUpdatePreferences { it.copy(hydrationIntervalMinutes = mins) } },
                            label = { Text("${mins}m", fontSize = 11.sp) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Long focus break nudge after",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(40, 50, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = prefs.focusBreakThresholdMinutes == mins,
                            onClick = { onUpdatePreferences { it.copy(focusBreakThresholdMinutes = mins) } },
                            label = { Text("${mins}m", fontSize = 11.sp) }
                        )
                    }
                }
            }
            if (onSendTestWellnessNudge != null) {
                val context = LocalContext.current
                var testNudgeStatus by remember { mutableStateOf<String?>(null) }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        onSendTestWellnessNudge()
                        testNudgeStatus = "Live energy nudge sent — check your notification shade."
                    } else {
                        testNudgeStatus = "Allow Notifications in system settings to receive wellness nudges."
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Background energy checks every 30m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Silent during active Focus sessions & sleep hours · 1-tap log actions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestWellnessNudge()
                                testNudgeStatus = "Live energy nudge sent — check your notification shade."
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_test_wellness_nudge_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Send test wellness nudge",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Nudge", fontSize = 12.sp)
                    }
                }
                testNudgeStatus?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("settings_test_wellness_nudge_status")
                    )
                }
            }
        }
    }

    editingConfig?.let { target ->
        val timePickerState = rememberTimePickerState(
            initialHour = (target.defaultMinuteOfDay / 60).coerceIn(0, 23),
            initialMinute = (target.defaultMinuteOfDay % 60).coerceIn(0, 59)
        )
        AlertDialog(
            onDismissRequest = { editingConfig = null },
            title = {
                Text(
                    text = "${target.type.emoji} ${target.type.displayName} Prompt Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newMin = (timePickerState.hour * 60 + timePickerState.minute).coerceIn(0, 1439)
                        val updated = configs.map {
                            if (it.type == target.type) it.copy(defaultMinuteOfDay = newMin) else it
                        }
                        onUpdatePreferences {
                            it.copy(checkInConfigJson = SmartAssistantCodecs.encodeCheckInConfigs(updated))
                        }
                        editingConfig = null
                    }
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingConfig = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
