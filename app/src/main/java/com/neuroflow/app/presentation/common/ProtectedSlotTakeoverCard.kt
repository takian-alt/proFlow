package com.neuroflow.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.presentation.schedule.getPtsCategoryColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The hero position while a protected block is running. Informational by design: the block is
 * something the user already decided on, so there is nothing to complete or skip here — tapping it
 * goes to the day's timeline instead.
 */
@Composable
fun ProtectedSlotTakeoverCard(due: DueSlot, onOpenTimeline: () -> Unit) {
    val accent = getPtsCategoryColor(due.slot.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTimeline),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        ProtectedSlotTakeoverContent(due)
    }
}

/** The takeover body without a container, so hosts with their own card styling can reuse it. */
@Composable
fun ProtectedSlotTakeoverContent(due: DueSlot) {
    val accent = getPtsCategoryColor(due.slot.category)

    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = 0.2f)
            ) {
                Text(
                    text = due.statusText(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Protected time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = due.title(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = due.timeText(),
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${clockRange(due.startMillis, due.endMillis)} · Tap to see your day",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A block that has not started yet, queued ahead of the tasks in "Up Next". */
@Composable
fun ProtectedSlotQueueRow(due: DueSlot, onOpenTimeline: () -> Unit) {
    val accent = getPtsCategoryColor(due.slot.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTimeline),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.padding(7.dp),
                    tint = accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = due.title(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${due.timeText()} · ${clockRange(due.startMillis, due.endMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun clockRange(startMillis: Long, endMillis: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${format.format(Date(startMillis))}–${format.format(Date(endMillis))}"
}
