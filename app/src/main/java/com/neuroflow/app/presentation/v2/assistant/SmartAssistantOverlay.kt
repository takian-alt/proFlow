package com.neuroflow.app.presentation.v2.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.assistant.ActiveBreakTimerState
import com.neuroflow.app.domain.assistant.BurnoutRiskLevel
import com.neuroflow.app.domain.assistant.CheckInLogType
import com.neuroflow.app.domain.assistant.RestCheckInProposal
import com.neuroflow.app.domain.assistant.SmartAssistantCodecs
import com.neuroflow.app.domain.assistant.WellnessNudgeProposal
import com.neuroflow.app.domain.assistant.WellnessNudgeType
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

private enum class CheckInPromptStep {
    ASK,
    PICK_PAST_TIME,
    PICK_FUTURE_TIME
}

private enum class SleepTimePickerTarget {
    BEDTIME,
    WAKE_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCheckInPopupCard(
    type: CheckInLogType,
    onLogAtTime: (occurredAtMillis: Long, detail: String, durationMinutes: Int?) -> Unit,
    onSnoozeUntil: (targetMillis: Long) -> Unit,
    onSkipToday: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onLogSleep: (bedtimeMillis: Long, wakeMillis: Long) -> Unit = { _, _ -> },
    defaultBedHour: Int = 23,
    defaultWakeHour: Int = 7,
    initialStep: Int = 0 // 0 = ASK, 1 = PICK_PAST_TIME (used when editing from Vault Logbook)
) {
    var step by remember(type, initialStep) {
        mutableStateOf(if (initialStep == 1) CheckInPromptStep.PICK_PAST_TIME else CheckInPromptStep.ASK)
    }
    var selectedDetail by remember(type) {
        mutableStateOf(
            when {
                type.supportsStimulantSubtype -> "Coffee"
                type.supportsIntensity -> "Normal"
                else -> "Normal"
            }
        )
    }
    var selectedDurationMinutes by remember(type) {
        mutableIntStateOf(
            when (type) {
                CheckInLogType.NAP -> 20
                CheckInLogType.WORKOUT -> 45
                else -> 30
            }
        )
    }

    // Sleep bedtime & wake time state seeded from user schedule defaults
    var bedHour by remember(type, defaultBedHour) { mutableIntStateOf(defaultBedHour.coerceIn(0, 23)) }
    var bedMinute by remember(type) { mutableIntStateOf(0) }
    var wakeHour by remember(type, defaultWakeHour) { mutableIntStateOf(defaultWakeHour.coerceIn(0, 23)) }
    var wakeMinute by remember(type) { mutableIntStateOf(0) }
    var activeSleepPicker by remember(type) { mutableStateOf<SleepTimePickerTarget?>(null) }

    // Non-sleep explicit past timestamp & validation state (never frozen stale)
    var selectedPastMillis by remember(type) { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedPastChipIndex by remember(type) { mutableIntStateOf(0) } // 0=Just now, 1..4=offsets, -1=custom
    var timeValidationError by remember(type, step) { mutableStateOf<String?>(null) }

    var showExactTimePicker by remember { mutableStateOf(false) }
    var exactPickerIsFuture by remember { mutableStateOf(false) }

    val zoneId = remember { ZoneId.systemDefault() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("smart_checkin_popup_card"),
        shape = RoundedCornerShape(22.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.25.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(type.emoji, fontSize = 20.sp)
                    Column {
                        Text(
                            text = "SMART CHECK-IN · ${type.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = when (step) {
                                CheckInPromptStep.ASK -> type.promptQuestion
                                CheckInPromptStep.PICK_PAST_TIME -> type.pastTimeQuestion
                                CheckInPromptStep.PICK_FUTURE_TIME -> "When should I check back in?"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("checkin_dismiss_btn")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss check-in",
                        tint = NeuroFlowThemeV2Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            when (step) {
                CheckInPromptStep.ASK -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedPastMillis = System.currentTimeMillis()
                                selectedPastChipIndex = 0
                                timeValidationError = null
                                step = CheckInPromptStep.PICK_PAST_TIME
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("checkin_yes_btn")
                        ) {
                            Text(type.primaryYesLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                timeValidationError = null
                                step = CheckInPromptStep.PICK_FUTURE_TIME
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("checkin_not_yet_btn")
                        ) {
                            Text(
                                type.secondaryLaterLabel,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                        }
                    }
                    TextButton(
                        onClick = onSkipToday,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp)
                            .testTag("checkin_skip_btn")
                    ) {
                        Text(
                            text = type.skipLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }

                CheckInPromptStep.PICK_PAST_TIME -> {
                    if (type == CheckInLogType.SLEEP_RECAP) {
                        // Dedicated Bedtime + Wake Time flow
                        val freshNow = System.currentTimeMillis()
                        val (bedMillis, wakeMillis) = SmartAssistantCodecs.resolveLastNightSleepInterval(
                            bedHour = bedHour,
                            bedMinute = bedMinute,
                            wakeHour = wakeHour,
                            wakeMinute = wakeMinute,
                            nowMillis = freshNow,
                            zoneId = zoneId
                        )
                        val durationMinutes = ((wakeMillis - bedMillis) / 60_000L).toInt()
                        val isValidSleep = durationMinutes in 15..SleepPressureRepository.MAX_SLEEP_LOG_DURATION_MINUTES

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SleepTimeSelectorTile(
                                label = "Bedtime",
                                formattedTime = SmartAssistantCodecs.formatMinuteOfDay(bedHour * 60 + bedMinute),
                                icon = Icons.Default.Bedtime,
                                accentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                onClick = { activeSleepPicker = SleepTimePickerTarget.BEDTIME },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checkin_sleep_bedtime_btn")
                            )
                            SleepTimeSelectorTile(
                                label = "Wake Time",
                                formattedTime = SmartAssistantCodecs.formatMinuteOfDay(wakeHour * 60 + wakeMinute),
                                icon = Icons.Default.WbSunny,
                                accentColor = NeuroFlowThemeV2Colors.WarmAmber,
                                onClick = { activeSleepPicker = SleepTimePickerTarget.WAKE_TIME },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checkin_sleep_waketime_btn")
                            )
                        }

                        // Live duration summary or validation error
                        val bedLabel = SmartAssistantCodecs.formatTimestampShort(bedMillis, zoneId)
                        val wakeLabel = SmartAssistantCodecs.formatTimestampShort(wakeMillis, zoneId)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isValidSleep) {
                                NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f)
                            } else {
                                NeuroFlowThemeV2Colors.RoseCoral.copy(alpha = 0.14f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isValidSleep) {
                                    NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f)
                                } else {
                                    NeuroFlowThemeV2Colors.RoseCoral.copy(alpha = 0.45f)
                                }
                            )
                        ) {
                            Text(
                                text = if (isValidSleep) {
                                    "Last night · $bedLabel → $wakeLabel (${SmartAssistantCodecs.formatDurationCompact(durationMinutes)})"
                                } else {
                                    "Sleep duration must be between 15m and 16h — adjust bedtime or wake time."
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isValidSleep) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.RoseCoral
                            )
                        }

                        Button(
                            onClick = {
                                val (finalBed, finalWake) = SmartAssistantCodecs.resolveLastNightSleepInterval(
                                    bedHour = bedHour,
                                    bedMinute = bedMinute,
                                    wakeHour = wakeHour,
                                    wakeMinute = wakeMinute,
                                    nowMillis = System.currentTimeMillis(),
                                    zoneId = zoneId
                                )
                                onLogSleep(finalBed, finalWake)
                            },
                            enabled = isValidSleep,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("checkin_save_sleep_btn")
                        ) {
                            Text(
                                text = "Save Sleep Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Optional intensity or stimulant type selector
                        if (type.supportsIntensity) {
                            val options = if (type == CheckInLogType.WORKOUT) {
                                listOf("Light", "Normal", "Intense")
                            } else {
                                listOf("Light", "Normal", "Heavy")
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                options.forEach { opt ->
                                    FilterChip(
                                        selected = selectedDetail == opt,
                                        onClick = { selectedDetail = opt },
                                        label = { Text(opt, fontSize = 12.sp) },
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    )
                                }
                            }
                        } else if (type.supportsStimulantSubtype) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                listOf("Coffee", "Tea", "Med IR", "Med XR").forEach { opt ->
                                    FilterChip(
                                        selected = selectedDetail == opt,
                                        onClick = { selectedDetail = opt },
                                        label = { Text(opt, fontSize = 12.sp) },
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    )
                                }
                            }
                        }

                        // Optional duration selector for Nap & Workout
                        if (type.supportsDuration) {
                            val durationOptions = if (type == CheckInLogType.NAP) {
                                listOf(15, 20, 30, 45, 60)
                            } else {
                                listOf(20, 30, 45, 60)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Length:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                durationOptions.forEach { mins ->
                                    FilterChip(
                                        selected = selectedDurationMinutes == mins,
                                        onClick = { selectedDurationMinutes = mins },
                                        label = { Text("${mins}m", fontSize = 12.sp) },
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    )
                                }
                            }
                        }

                        // Quick past-time selection chips + Pick exact clock time
                        val timePills = listOf(
                            "Just now" to 0L,
                            "15m ago" to 15 * 60_000L,
                            "30m ago" to 30 * 60_000L,
                            "1h ago" to 60 * 60_000L,
                            "2h ago" to 120 * 60_000L
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timePills.forEachIndexed { idx, (label, offsetMs) ->
                                FilterChip(
                                    selected = selectedPastChipIndex == idx,
                                    onClick = {
                                        selectedPastChipIndex = idx
                                        selectedPastMillis = System.currentTimeMillis() - offsetMs
                                        timeValidationError = null
                                    },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    exactPickerIsFuture = false
                                    showExactTimePicker = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("checkin_pick_exact_time_btn")
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick time", fontSize = 12.sp)
                            }
                        }

                        timeValidationError?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.RoseCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val selectedTimeText = SmartAssistantCodecs.formatTimestampShort(selectedPastMillis, zoneId)
                        Button(
                            onClick = {
                                val tsToSave = if (selectedPastChipIndex == 0) {
                                    System.currentTimeMillis()
                                } else {
                                    selectedPastMillis
                                }
                                onLogAtTime(
                                    tsToSave,
                                    selectedDetail,
                                    if (type.supportsDuration) selectedDurationMinutes else null
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("checkin_confirm_log_btn")
                        ) {
                            Text(
                                text = "Log ${type.displayName} at $selectedTimeText",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (initialStep == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    timeValidationError = null
                                    step = CheckInPromptStep.PICK_FUTURE_TIME
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Remind later", fontSize = 12.sp, color = NeuroFlowThemeV2Colors.TextSecondary)
                            }
                            TextButton(
                                onClick = onSkipToday,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(type.skipLabel, fontSize = 12.sp, color = NeuroFlowThemeV2Colors.RoseCoral)
                            }
                        }
                    }
                }

                CheckInPromptStep.PICK_FUTURE_TIME -> {
                    val snoozePills = listOf(
                        "In 30m" to 30 * 60_000L,
                        "In 1h" to 60 * 60_000L,
                        "In 2h" to 120 * 60_000L
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snoozePills.forEach { (label, delayMs) ->
                            FilledTonalButton(
                                onClick = { onSnoozeUntil(System.currentTimeMillis() + delayMs) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                exactPickerIsFuture = true
                                showExactTimePicker = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("checkin_pick_future_time_btn")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick time", fontSize = 12.sp)
                        }
                    }

                    timeValidationError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.RoseCoral,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Dedicated Bedtime / Wake Time M3 TimePicker dialog
    activeSleepPicker?.let { target ->
        val initialH = if (target == SleepTimePickerTarget.BEDTIME) bedHour else wakeHour
        val initialM = if (target == SleepTimePickerTarget.BEDTIME) bedMinute else wakeMinute
        val timePickerState = rememberTimePickerState(
            initialHour = initialH,
            initialMinute = initialM
        )
        AlertDialog(
            onDismissRequest = { activeSleepPicker = null },
            title = {
                Text(
                    text = if (target == SleepTimePickerTarget.BEDTIME) {
                        "When did you go to sleep?"
                    } else {
                        "When did you wake up?"
                    },
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
                        if (target == SleepTimePickerTarget.BEDTIME) {
                            bedHour = timePickerState.hour
                            bedMinute = timePickerState.minute
                        } else {
                            wakeHour = timePickerState.hour
                            wakeMinute = timePickerState.minute
                        }
                        activeSleepPicker = null
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSleepPicker = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Non-sleep exact clock TimePicker dialog (past log time or future reminder)
    if (showExactTimePicker) {
        val freshNowMillis = System.currentTimeMillis()
        val seedMillis = if (exactPickerIsFuture) {
            freshNowMillis + 60 * 60_000L
        } else {
            selectedPastMillis
        }
        val seedZdt = Instant.ofEpochMilli(seedMillis).atZone(zoneId)
        val timePickerState = rememberTimePickerState(
            initialHour = seedZdt.hour,
            initialMinute = seedZdt.minute
        )
        AlertDialog(
            onDismissRequest = { showExactTimePicker = false },
            title = {
                Text(
                    text = if (exactPickerIsFuture) "Remind me at..." else type.pastTimeQuestion,
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
                        val confirmNow = System.currentTimeMillis()
                        val todayDate = Instant.ofEpochMilli(confirmNow).atZone(zoneId).toLocalDate()
                        val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val pickedMillis = todayDate.atTime(pickedTime).atZone(zoneId).toInstant().toEpochMilli()
                        showExactTimePicker = false
                        if (exactPickerIsFuture) {
                            if (pickedMillis <= confirmNow + 2 * 60_000L) {
                                timeValidationError = "Pick a time later today to set a reminder."
                            } else {
                                timeValidationError = null
                                onSnoozeUntil(pickedMillis)
                            }
                        } else {
                            if (pickedMillis > confirmNow + 60_000L) {
                                timeValidationError = "That time hasn't happened yet today — choose an earlier time or tap Remind later."
                            } else {
                                selectedPastMillis = pickedMillis
                                selectedPastChipIndex = -1
                                timeValidationError = null
                            }
                        }
                    }
                ) {
                    Text(if (exactPickerIsFuture) "Set Reminder" else "Use Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SleepTimeSelectorTile(
    label: String,
    formattedTime: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
        }
    }
}

@Composable
fun WellnessNudgePopupCard(
    nudge: WellnessNudgeProposal,
    onDrinkWater: () -> Unit,
    onOpenEasyWins: () -> Unit,
    onDismissLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (nudge.type) {
        WellnessNudgeType.HYDRATION, WellnessNudgeType.LONG_FOCUS_BREAK -> NeuroFlowPaletteV2.ReboundCyan
        WellnessNudgeType.LOW_ENERGY_REST, WellnessNudgeType.CRASH_EASY_WIN -> NeuroFlowThemeV2Colors.WarmAmber
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("wellness_nudge_popup_card"),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.25.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(nudge.emoji, fontSize = 20.sp)
                    Column {
                        Text(
                            text = "ENERGY & FOCUS CARE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = nudge.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
                IconButton(onClick = onDismissLater, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss wellness nudge",
                        tint = NeuroFlowThemeV2Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = nudge.message,
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (nudge.type == WellnessNudgeType.CRASH_EASY_WIN) {
                            onOpenEasyWins()
                            onDismissLater()
                        } else {
                            onDrinkWater()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = NeuroFlowPaletteV2.onAccentColor(accent)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(nudge.primaryActionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        when (nudge.type) {
                            WellnessNudgeType.CRASH_EASY_WIN -> onDrinkWater()
                            WellnessNudgeType.LOW_ENERGY_REST -> {
                                onOpenEasyWins()
                                onDismissLater()
                            }
                            else -> onDismissLater()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(
                        nudge.secondaryActionLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun BurnoutRestCheckInCard(
    proposal: RestCheckInProposal,
    onLogRestBreak: (Int) -> Unit,
    onStartBreakTimer: (Int) -> Unit,
    onNotYet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDurationPicker by remember(proposal.title, proposal.riskState.unbrokenLoadMinutes) {
        mutableStateOf(false)
    }
    val accent = if (proposal.riskState.riskLevel == BurnoutRiskLevel.HIGH) {
        NeuroFlowPaletteV2.RecoveryRose
    } else {
        NeuroFlowThemeV2Colors.WarmAmber
    }
    val restAccent = NeuroFlowPaletteV2.OptimalEmerald

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("burnout_rest_checkin_card"),
        shape = RoundedCornerShape(22.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.25.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "BURNOUT GUARD · REST CHECK-IN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = proposal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onNotYet,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ask again in ${proposal.followUpMinutes} minutes",
                        tint = NeuroFlowThemeV2Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = proposal.message,
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDurationPicker = !showDurationPicker },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = restAccent,
                        contentColor = NeuroFlowPaletteV2.onAccentColor(restAccent)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("rest_checkin_yes_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Yes, I rested", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { onStartBreakTimer(proposal.recommendedRestMinutes) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, restAccent.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = restAccent),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("rest_checkin_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "Start ${proposal.recommendedRestMinutes}m Break",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(visible = showDurationPicker) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tap how long you rested to log & reset burnout load:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = restAccent.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, restAccent.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("rest_duration_chip_$mins")
                                    .clickable { onLogRestBreak(mins) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = restAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onNotYet,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("rest_checkin_not_yet_button")
                ) {
                    Text(
                        text = "Not yet · ask in ${proposal.followUpMinutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeuroFlowThemeV2Colors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveBreakTimerPill(
    timer: ActiveBreakTimerState,
    onTogglePause: () -> Unit,
    onFinishEarly: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = timer.remainingSeconds / 60
    val secs = timer.remainingSeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val accent = NeuroFlowPaletteV2.OptimalEmerald

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("active_break_timer_pill"),
        shape = RoundedCornerShape(18.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.25.dp, accent.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (timer.isPaused) NeuroFlowThemeV2Colors.WarmAmber else accent, CircleShape)
                )
                Column {
                    Text(
                        text = if (timer.isPaused) "BREAK PAUSED" else "RECHARGE BREAK TIMER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "$timeText remaining",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("break_timer_pause_button")
                ) {
                    Icon(
                        imageVector = if (timer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (timer.isPaused) "Resume break timer" else "Pause break timer",
                        tint = accent
                    )
                }
                FilledTonalButton(
                    onClick = onFinishEarly,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent.copy(alpha = 0.2f),
                        contentColor = accent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("break_timer_finish_button")
                ) {
                    Text("Log ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("break_timer_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel break timer",
                        tint = NeuroFlowThemeV2Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnergyBoosterNudgeCard(
    proposal: com.neuroflow.app.domain.assistant.EnergyBoosterNudgeProposal,
    onCompleteBooster: (com.neuroflow.app.domain.assistant.EnergyBoosterRecommendation) -> Unit,
    onStartTimer: (Int) -> Unit,
    onSnoozeLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rec = proposal.recommendation
    val emerald = NeuroFlowPaletteV2.OptimalEmerald
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("energy_booster_nudge_card"),
        shape = RoundedCornerShape(22.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.25.dp, emerald.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = rec.emoji, fontSize = 22.sp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ENERGY COACH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = emerald,
                                letterSpacing = 0.8.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = emerald.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = rec.boostBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = emerald,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = proposal.headline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onSnoozeLater,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("energy_booster_nudge_dismiss_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Snooze energy suggestion",
                        tint = NeuroFlowThemeV2Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = proposal.body,
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onCompleteBooster(rec) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emerald,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("energy_booster_nudge_done_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log ${rec.title}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                if (rec.supportsTimer) {
                    FilledTonalButton(
                        onClick = { onStartTimer(rec.durationMinutes) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = emerald.copy(alpha = 0.16f),
                            contentColor = emerald
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("energy_booster_nudge_timer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${rec.durationMinutes}m", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSnoozeLater,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("energy_booster_nudge_later_btn")
                    ) {
                        Text(
                            text = "Later",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartAssistantOverlayHost(
    state: SmartAssistantUiState,
    visible: Boolean,
    onLogCheckIn: (CheckInLogType, Long, String, Int?) -> Unit,
    onLogSleep: (bedtimeMillis: Long, wakeMillis: Long) -> Unit,
    onSnoozeCheckIn: (CheckInLogType, Long) -> Unit,
    onSkipCheckIn: (CheckInLogType) -> Unit,
    onDismissCheckIn: (CheckInLogType) -> Unit,
    onDrinkWater: () -> Unit,
    onOpenEasyWins: () -> Unit,
    onDismissWellnessNudge: () -> Unit,
    modifier: Modifier = Modifier,
    onLogRestBreak: (Int) -> Unit = {},
    onStartBreakTimer: (Int) -> Unit = {},
    onSnoozeRestCheckIn: () -> Unit = {},
    onTogglePauseBreakTimer: () -> Unit = {},
    onFinishBreakTimerEarly: () -> Unit = {},
    onCancelBreakTimer: () -> Unit = {},
    onCompleteEnergyBoosterNudge: (com.neuroflow.app.domain.assistant.EnergyBoosterRecommendation) -> Unit = {},
    onSnoozeEnergyBoosterNudge: () -> Unit = {}
) {
    val due = state.dueCheckIn
    val restCheckIn = state.activeRestCheckIn
    val boosterNudge = state.activeEnergyBoosterNudge
    val nudge = state.activeWellnessNudge
    val hasBreakTimer = state.breakTimer.isActive
    val hasCard = visible && (hasBreakTimer || due != null || restCheckIn != null || boosterNudge != null || nudge != null)

    AnimatedVisibility(
        visible = hasCard,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (hasBreakTimer) {
                ActiveBreakTimerPill(
                    timer = state.breakTimer,
                    onTogglePause = onTogglePauseBreakTimer,
                    onFinishEarly = onFinishBreakTimerEarly,
                    onCancel = onCancelBreakTimer
                )
            }
            if (due != null) {
                SmartCheckInPopupCard(
                    type = due.type,
                    onLogAtTime = { ts, detail, dur -> onLogCheckIn(due.type, ts, detail, dur) },
                    onLogSleep = onLogSleep,
                    onSnoozeUntil = { target -> onSnoozeCheckIn(due.type, target) },
                    onSkipToday = { onSkipCheckIn(due.type) },
                    onDismiss = { onDismissCheckIn(due.type) },
                    defaultBedHour = state.preferences.sleepHour,
                    defaultWakeHour = state.preferences.wakeUpHour
                )
            } else if (restCheckIn != null && !hasBreakTimer) {
                BurnoutRestCheckInCard(
                    proposal = restCheckIn,
                    onLogRestBreak = onLogRestBreak,
                    onStartBreakTimer = onStartBreakTimer,
                    onNotYet = onSnoozeRestCheckIn
                )
            } else if (boosterNudge != null && !hasBreakTimer) {
                EnergyBoosterNudgeCard(
                    proposal = boosterNudge,
                    onCompleteBooster = onCompleteEnergyBoosterNudge,
                    onStartTimer = onStartBreakTimer,
                    onSnoozeLater = onSnoozeEnergyBoosterNudge
                )
            } else if (nudge != null && !hasBreakTimer) {
                WellnessNudgePopupCard(
                    nudge = nudge,
                    onDrinkWater = onDrinkWater,
                    onOpenEasyWins = onOpenEasyWins,
                    onDismissLater = onDismissWellnessNudge
                )
            }
        }
    }
}
