package com.neuroflow.app.presentation.v2.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.assistant.CheckInLogType
import com.neuroflow.app.domain.assistant.SmartAssistantCodecs
import com.neuroflow.app.domain.assistant.WellnessNudgeProposal
import com.neuroflow.app.domain.assistant.WellnessNudgeType
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

private enum class CheckInPromptStep {
    ASK,
    PICK_PAST_TIME,
    PICK_FUTURE_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCheckInPopupCard(
    type: CheckInLogType,
    onLogAtTime: (occurredAtMillis: Long, detail: String, sleepHours: Float) -> Unit,
    onSnoozeUntil: (targetMillis: Long) -> Unit,
    onSkipToday: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
    var sleepHours by remember(type) { mutableFloatStateOf(7.5f) }
    var showExactTimePicker by remember { mutableStateOf(false) }
    var exactPickerIsFuture by remember { mutableStateOf(false) }

    val nowMillis = remember { System.currentTimeMillis() }
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
                                CheckInPromptStep.PICK_PAST_TIME -> if (type == CheckInLogType.SLEEP_RECAP) {
                                    "How many hours & when did you wake up?"
                                } else {
                                    "When did you have ${type.displayName.lowercase()}?"
                                }
                                CheckInPromptStep.PICK_FUTURE_TIME -> "When do you plan to? I'll check in right after."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp).testTag("checkin_dismiss_btn")
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
                            onClick = { step = CheckInPromptStep.PICK_PAST_TIME },
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
                            Text("Yes, log it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { step = CheckInPromptStep.PICK_FUTURE_TIME },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("checkin_not_yet_btn")
                        ) {
                            Text("Not yet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NeuroFlowThemeV2Colors.TextPrimary)
                        }
                    }
                    TextButton(
                        onClick = onSkipToday,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 40.dp)
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
                    } else if (type == CheckInLogType.SLEEP_RECAP) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(5.5f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f, 9.0f).forEach { hrs ->
                                FilterChip(
                                    selected = sleepHours == hrs,
                                    onClick = { sleepHours = hrs },
                                    label = { Text("${hrs}h", fontSize = 12.sp) },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                )
                            }
                        }
                    }

                    // Retroactive or current time selection pills
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
                        timePills.forEach { (label, offsetMs) ->
                            FilledTonalButton(
                                onClick = {
                                    onLogAtTime(System.currentTimeMillis() - offsetMs, selectedDetail, sleepHours)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                exactPickerIsFuture = false
                                showExactTimePicker = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick time", fontSize = 12.sp)
                        }
                    }

                    if (initialStep == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { step = CheckInPromptStep.PICK_FUTURE_TIME }) {
                                Text("Remind later", fontSize = 12.sp, color = NeuroFlowThemeV2Colors.TextSecondary)
                            }
                            TextButton(onClick = onSkipToday) {
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
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick time", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showExactTimePicker) {
        val currentZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val timePickerState = rememberTimePickerState(
            initialHour = currentZdt.hour,
            initialMinute = currentZdt.minute
        )
        AlertDialog(
            onDismissRequest = { showExactTimePicker = false },
            title = {
                Text(
                    text = if (exactPickerIsFuture) "Ask me again after..." else "What time was ${type.displayName.lowercase()}?",
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
                        val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val todayDate = currentZdt.toLocalDate()
                        var targetMillis = todayDate.atTime(pickedTime).atZone(zoneId).toInstant().toEpochMilli()
                        showExactTimePicker = false
                        if (exactPickerIsFuture) {
                            if (targetMillis <= System.currentTimeMillis()) {
                                targetMillis = System.currentTimeMillis() + 30 * 60_000L
                            }
                            onSnoozeUntil(targetMillis)
                        } else {
                            if (targetMillis > System.currentTimeMillis()) {
                                targetMillis = System.currentTimeMillis()
                            }
                            onLogAtTime(targetMillis, selectedDetail, sleepHours)
                        }
                    }
                ) {
                    Text(if (exactPickerIsFuture) "Set Reminder" else "Log Time")
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
                IconButton(onClick = onDismissLater, modifier = Modifier.size(40.dp)) {
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
fun SmartAssistantOverlayHost(
    state: SmartAssistantUiState,
    visible: Boolean,
    onLogCheckIn: (CheckInLogType, Long, String, Float) -> Unit,
    onSnoozeCheckIn: (CheckInLogType, Long) -> Unit,
    onSkipCheckIn: (CheckInLogType) -> Unit,
    onDismissCheckIn: (CheckInLogType) -> Unit,
    onDrinkWater: () -> Unit,
    onOpenEasyWins: () -> Unit,
    onDismissWellnessNudge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val due = state.dueCheckIn
    val nudge = state.activeWellnessNudge
    val hasCard = visible && (due != null || nudge != null)

    AnimatedVisibility(
        visible = hasCard,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        if (due != null) {
            SmartCheckInPopupCard(
                type = due.type,
                onLogAtTime = { ts, detail, hrs -> onLogCheckIn(due.type, ts, detail, hrs) },
                onSnoozeUntil = { target -> onSnoozeCheckIn(due.type, target) },
                onSkipToday = { onSkipCheckIn(due.type) },
                onDismiss = { onDismissCheckIn(due.type) }
            )
        } else if (nudge != null) {
            WellnessNudgePopupCard(
                nudge = nudge,
                onDrinkWater = onDrinkWater,
                onOpenEasyWins = onOpenEasyWins,
                onDismissLater = onDismissWellnessNudge
            )
        }
    }
}
