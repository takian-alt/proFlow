package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import java.util.UUID

fun getPtsCategoryColor(category: PtsCategory): Color {
    return when (category) {
        PtsCategory.MINDFULNESS -> Color(0xFF81C784) // Green
        PtsCategory.EXERCISE -> Color(0xFFFF8A65)    // Deep orange / coral
        PtsCategory.MEAL -> Color(0xFFFFD54F)        // Amber / gold
        PtsCategory.FAMILY -> Color(0xFFBA68C8)      // Purple
        PtsCategory.BREAK -> Color(0xFF4DD0E1)       // Cyan
        PtsCategory.CUSTOM -> Color(0xFF90A4AE)      // Blue grey
    }
}

fun getPtsCategoryEmoji(category: PtsCategory): String {
    return when (category) {
        PtsCategory.MINDFULNESS -> "🧘"
        PtsCategory.EXERCISE -> "🏋️"
        PtsCategory.MEAL -> "🍽️"
        PtsCategory.FAMILY -> "👨‍👩‍👧"
        PtsCategory.BREAK -> "☕"
        PtsCategory.CUSTOM -> "🔒"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedSlotBottomSheet(
    protectedSlots: List<ProtectedTimeSlot>,
    initialHour: Int = 12,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onSaveSlot: (ProtectedTimeSlot) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onToggleSlot: (String) -> Unit
) {
    var isAddingNew by remember { mutableStateOf(false) }

    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PtsCategory.BREAK) }
    var startHour by remember { mutableStateOf(initialHour) }
    var startMinute by remember { mutableStateOf(initialMinute) }
    var durationMinutes by remember { mutableStateOf(60) }
    var recurrence by remember { mutableStateOf(PtsRecurrence.DAILY) }
    var recoveryBufferMinutes by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAddingNew) "New Protected Window" else "Protected Time Slots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAddingNew) {
                Text(
                    text = "Locks fixed blocks for gym, mindfulness, meals, and recovery so auto-scheduling never overbooks them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        label = ""
                        category = PtsCategory.BREAK
                        startHour = initialHour
                        startMinute = initialMinute
                        durationMinutes = 60
                        recurrence = PtsRecurrence.DAILY
                        recoveryBufferMinutes = 0
                        isAddingNew = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Protected Slot")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (protectedSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No protected slots yet. Tap above to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(protectedSlots, key = { it.id }) { slot ->
                            ProtectedSlotItem(
                                slot = slot,
                                onToggle = { onToggleSlot(slot.id) },
                                onDelete = { onDeleteSlot(slot.id) }
                            )
                        }
                    }
                }
            } else {
                // Add new slot form
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Activity Label (e.g. Morning Gym, Lunch)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PtsCategory.values().forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("${getPtsCategoryEmoji(cat)} ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start Time", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(8, 12, 17, 20).forEach { h ->
                            SuggestionChip(
                                onClick = { startHour = h; startMinute = 0 },
                                label = { Text(String.format("%02d:00", h), fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = String.format("%02d", startHour),
                        onValueChange = { str -> str.toIntOrNull()?.let { startHour = it.coerceIn(0, 23) } },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = String.format("%02d", startMinute),
                        onValueChange = { str -> str.toIntOrNull()?.let { startMinute = it.coerceIn(0, 59) } },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Duration: $durationMinutes min", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30, 45, 60, 90, 120).forEach { d ->
                        FilterChip(
                            selected = durationMinutes == d,
                            onClick = { durationMinutes = d },
                            label = { Text("${d}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Recurrence", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PtsRecurrence.values().take(3).forEach { rec ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(rec.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Post-Activity Recovery Buffer: $recoveryBufferMinutes min", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 15, 30).forEach { buf ->
                        FilterChip(
                            selected = recoveryBufferMinutes == buf,
                            onClick = { recoveryBufferMinutes = buf },
                            label = { Text(if (buf == 0) "None" else "${buf}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isAddingNew = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val newSlot = ProtectedTimeSlot(
                                id = UUID.randomUUID().toString(),
                                label = if (label.isNotBlank()) label.trim() else "${getPtsCategoryEmoji(category)} ${category.name}",
                                startMinuteOfDay = startHour * 60 + startMinute,
                                durationMinutes = durationMinutes,
                                recurrence = recurrence,
                                category = category,
                                recoveryBufferMinutes = recoveryBufferMinutes,
                                isEnabled = true
                            )
                            onSaveSlot(newSlot)
                            isAddingNew = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Block")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtectedSlotItem(
    slot: ProtectedTimeSlot,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getPtsCategoryColor(slot.category)
    val startH = slot.startMinuteOfDay / 60
    val startM = slot.startMinuteOfDay % 60
    val endMinuteOfDay = slot.startMinuteOfDay + slot.durationMinutes
    val endH = (endMinuteOfDay / 60) % 24
    val endM = endMinuteOfDay % 60
    val timeRangeStr = String.format("%02d:%02d - %02d:%02d", startH, startM, endH, endM)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (slot.isEnabled) categoryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(getPtsCategoryEmoji(slot.category), fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (slot.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "$timeRangeStr • ${slot.recurrence.name.lowercase()} • ${slot.durationMinutes}m" +
                               if (slot.recoveryBufferMinutes > 0) " (+${slot.recoveryBufferMinutes}m buffer)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = slot.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
