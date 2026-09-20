package com.neuroflow.app.presentation.launcher.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.launcher.domain.ClockStyle
import com.neuroflow.app.presentation.launcher.theme.LocalLauncherTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Display current date and time with minute-boundary updates.
 *
 * Updates once per minute aligned to the minute boundary using:
 * delay(60_000 - (System.currentTimeMillis() % 60_000))
 *
 * Supports Digital, Minimal, Stacked, and Smart Pill clock styles from LauncherTheme.
 *
 * @param modifier Modifier for the display container
 */
@Composable
fun DateTimeDisplay(
    modifier: Modifier = Modifier
) {
    val theme = LocalLauncherTheme.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update time at minute boundaries
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            // Calculate delay to next minute boundary
            val delayMs = 60_000 - (currentTime % 60_000)
            delay(delayMs)
        }
    }

    val calendar = remember(currentTime) {
        Calendar.getInstance().apply {
            timeInMillis = currentTime
        }
    }

    when (theme.clockStyle) {
        ClockStyle.DIGITAL -> DigitalClock(calendar, modifier)
        ClockStyle.MINIMAL -> MinimalClock(calendar, modifier)
        ClockStyle.STACKED -> StackedClock(calendar, modifier)
        ClockStyle.SMART_PILL -> SmartPillClock(calendar, modifier)
    }
}

/**
 * Digital clock style: full date and time display.
 */
@Composable
private fun DigitalClock(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val timeText = remember(calendar) { timeFormat.format(calendar.time) }
    val dateText = remember(calendar) { dateFormat.format(calendar.time) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .testTag("date_time_display"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launchClockApp(context)
            }
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launchCalendarApp(context)
            }
        )
    }
}

/**
 * Minimal clock style: time only, compact display.
 */
@Composable
private fun MinimalClock(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val timeText = remember(calendar) { timeFormat.format(calendar.time) }

    Text(
        text = timeText,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 48.sp,
            fontWeight = FontWeight.Light
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(16.dp)
            .testTag("date_time_display")
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launchClockApp(context)
            }
    )
}

/**
 * Stacked clock style: Android 14/15 lockscreen-style large stacked hours/minutes.
 */
@Composable
private fun StackedClock(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hourFormat = remember { SimpleDateFormat("hh", Locale.getDefault()) }
    val minuteFormat = remember { SimpleDateFormat("mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val hourText = remember(calendar) { hourFormat.format(calendar.time) }
    val minuteText = remember(calendar) { minuteFormat.format(calendar.time) }
    val dateText = remember(calendar) { dateFormat.format(calendar.time) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .testTag("date_time_display"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launchClockApp(context)
            }
        ) {
            Text(
                text = hourText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 72.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = minuteText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 72.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launchCalendarApp(context)
            }
        )
    }
}

/**
 * Smart Pill clock style: Compact at-a-glance header pill.
 */
@Composable
private fun SmartPillClock(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val timeText = remember(calendar) { timeFormat.format(calendar.time) }
    val dateText = remember(calendar) { dateFormat.format(calendar.time) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("date_time_display"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        launchClockApp(context)
                    }
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        launchCalendarApp(context)
                    }
                )
            }
        }
    }
}

private fun launchClockApp(context: Context) {
    try {
        val clockIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(clockIntent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (ignored: Exception) {}
    }
}

private fun launchCalendarApp(context: Context) {
    try {
        val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(calendarIntent)
    } catch (e: Exception) {
        try {
            val builder = android.net.Uri.parse("content://com.android.calendar/time").buildUpon()
            val viewIntent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(viewIntent)
        } catch (ignored: Exception) {}
    }
}

