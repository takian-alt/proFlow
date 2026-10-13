package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

fun getPtsCategoryColor(category: PtsCategory): Color {
    return when (category) {
        PtsCategory.MINDFULNESS -> Color(0xFF81C784) // Green
        PtsCategory.EXERCISE -> Color(0xFFFF8A65)    // Deep orange / coral
        PtsCategory.MEAL -> Color(0xFFFFD54F)        // Amber / gold
        PtsCategory.FAMILY -> Color(0xFFBA68C8)      // Purple
        PtsCategory.BREAK -> Color(0xFF4DD0E1)       // Cyan
        PtsCategory.WORK -> Color(0xFF5C6BC0)        // Indigo
        PtsCategory.STUDY -> Color(0xFF26A69A)       // Teal
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
        PtsCategory.WORK -> "💼"
        PtsCategory.STUDY -> "📚"
        PtsCategory.CUSTOM -> "🔒"
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

fun formatCustomDays(weekdayBitmask: Int): String {
    val dayNames = listOf(
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat"
    )
    val activeDays = dayNames.filter { (calDay, _) ->
        val bit = 1 shl (calDay - 1)
        (weekdayBitmask and bit) != 0
    }.map { it.second }

    val weekdaysSet = setOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val weekendsSet = setOf("Sun", "Sat")
    val activeSet = activeDays.toSet()

    return when {
        activeDays.isEmpty() -> "No days selected"
        activeDays.size == 7 -> "Every day"
        activeSet == weekdaysSet -> "Weekdays"
        activeSet == weekendsSet -> "Weekends"
        activeDays.size == 1 -> {
            val fullDay = when (activeDays.first()) {
                "Sun" -> "Sunday"
                "Mon" -> "Monday"
                "Tue" -> "Tuesday"
                "Wed" -> "Wednesday"
                "Thu" -> "Thursday"
                "Fri" -> "Friday"
                "Sat" -> "Saturday"
                else -> activeDays.first()
            }
            "Every $fullDay"
        }
        else -> "Every ${activeDays.joinToString(", ")}"
    }
}

fun formatRecurrenceLabel(slot: ProtectedTimeSlot): String {
    return when (slot.recurrence) {
        PtsRecurrence.DAILY -> "Daily"
        PtsRecurrence.WEEKDAYS -> "Weekdays"
        PtsRecurrence.WEEKENDS -> "Weekends"
        PtsRecurrence.CUSTOM -> formatCustomDays(slot.weekdayBitmask)
        PtsRecurrence.SPECIFIC_DATE -> {
            if (slot.dateMillis != null) {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(slot.dateMillis))
            } else {
                "One-time"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedSlotBottomSheet(
    protectedSlots: List<ProtectedTimeSlot>,
    initialHour: Int = 12,
    initialMinute: Int = 0,
    initialDate: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onSaveSlot: (ProtectedTimeSlot) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onToggleSlot: (String) -> Unit
) {
    var isAddingNew by remember { mutableStateOf(false) }
    var editingSlotId by remember { mutableStateOf<String?>(null) }

    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PtsCategory.WORK) }
    var selectedDateMillis by remember { mutableStateOf(initialDate) }
    var startHour by remember { mutableStateOf(initialHour) }
    var startMinute by remember { mutableStateOf(initialMinute) }
    var endHour by remember { mutableStateOf((initialHour + 1).coerceAtMost(23)) }
    var endMinute by remember { mutableStateOf(initialMinute) }
    var recurrence by remember { mutableStateOf(PtsRecurrence.DAILY) }
    var selectedWeekdayBitmask by remember { mutableStateOf(0b1111111) }
    var recoveryBufferMinutes by remember { mutableStateOf(0) }

    // Helper to populate form for adding or editing
    fun startEditing(slot: ProtectedTimeSlot?) {
        if (slot != null) {
            editingSlotId = slot.id
            label = slot.label
            category = slot.category
            startHour = slot.startMinuteOfDay / 60
            startMinute = slot.startMinuteOfDay % 60
            val endTotal = slot.startMinuteOfDay + slot.durationMinutes
            endHour = (endTotal / 60) % 24
            endMinute = endTotal % 60
            recurrence = slot.recurrence
            selectedWeekdayBitmask = slot.weekdayBitmask
            recoveryBufferMinutes = slot.recoveryBufferMinutes
            selectedDateMillis = slot.dateMillis ?: initialDate
        } else {
            editingSlotId = null
            label = ""
            category = PtsCategory.WORK
            selectedDateMillis = initialDate
            startHour = initialHour
            startMinute = initialMinute
            endHour = (initialHour + 1).coerceAtMost(23)
            endMinute = initialMinute
            recurrence = PtsRecurrence.DAILY
            selectedWeekdayBitmask = 0b1111111
            recoveryBufferMinutes = 0
        }
        isAddingNew = true
    }

    // Dialog picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startMinuteOfDay = startHour * 60 + startMinute
    val endMinuteOfDay = endHour * 60 + endMinute
    val durationMinutes = endMinuteOfDay - startMinuteOfDay
    val isDurationValid = durationMinutes >= 15
    val isCustomDaysValid = recurrence != PtsRecurrence.CUSTOM || selectedWeekdayBitmask != 0
    val canSave = isDurationValid && isCustomDaysValid

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
                        text = when {
                            editingSlotId != null -> "Edit Protected Window"
                            isAddingNew -> "New Protected Window"
                            else -> "Protected Time Slots"
                        },
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
                    text = "Locks fixed blocks for work, study, gym, mindfulness, meals, and recovery so auto-scheduling never overbooks them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { startEditing(null) },
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
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(protectedSlots, key = { it.id }) { slot ->
                            ProtectedSlotItem(
                                slot = slot,
                                onToggle = { onToggleSlot(slot.id) },
                                onEdit = { startEditing(slot) },
                                onDelete = { onDeleteSlot(slot.id) }
                            )
                        }
                    }
                }
            } else {
                // Add / Edit slot form (scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Activity Label") },
                            placeholder = { Text("e.g. Deep Work, Gym, Lunch, Family") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick suggestion chips
                        val labelSuggestions = listOf(
                            "Deep Work" to PtsCategory.WORK,
                            "Gym / Exercise" to PtsCategory.EXERCISE,
                            "Lunch / Meal" to PtsCategory.MEAL,
                            "Mindfulness" to PtsCategory.MINDFULNESS,
                            "Study" to PtsCategory.STUDY,
                            "Family Time" to PtsCategory.FAMILY,
                            "Rest Break" to PtsCategory.BREAK
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            labelSuggestions.forEach { (sugLabel, sugCat) ->
                                SuggestionChip(
                                    onClick = {
                                        label = sugLabel
                                        category = sugCat
                                    },
                                    label = { Text(sugLabel, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Category selection
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PtsCategory.values().forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = {
                                        Text(
                                            "${getPtsCategoryEmoji(cat)} ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Calendar Date selection (if SPECIFIC_DATE)
                    if (recurrence == PtsRecurrence.SPECIFIC_DATE) {
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Select Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        val dateText = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Tap to change date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Start Time & End Time pickers
                    Column {
                        Text("Timing & Duration", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start time card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showStartTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            formatTime(startHour, startMinute),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // End time card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showEndTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            formatTime(endHour, endMinute),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Duration presets
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                15 to "+15m",
                                30 to "+30m",
                                45 to "+45m",
                                60 to "+1h",
                                90 to "+1.5h",
                                120 to "+2h",
                                180 to "+3h"
                            ).forEach { (mins, lbl) ->
                                AssistChip(
                                    onClick = {
                                        val newEnd = (startHour * 60 + startMinute + mins).coerceAtMost(1439)
                                        endHour = newEnd / 60
                                        endMinute = newEnd % 60
                                    },
                                    label = { Text(lbl, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Calculated duration or validation message
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!isDurationValid) {
                            Text(
                                text = "End time must be at least 15 minutes after start time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            val durationFormatted = if (durationMinutes >= 60) {
                                val hrs = durationMinutes / 60
                                val mins = durationMinutes % 60
                                if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
                            } else {
                                "${durationMinutes}m"
                            }
                            Text(
                                text = "⏱️ Duration: $durationFormatted ($durationMinutes min)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Recurrence
                    Column {
                        Text("Recurrence", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        val recurrenceOptions = listOf(
                            PtsRecurrence.DAILY to "Daily",
                            PtsRecurrence.WEEKDAYS to "Weekdays",
                            PtsRecurrence.WEEKENDS to "Weekends",
                            PtsRecurrence.CUSTOM to "Custom Days",
                            PtsRecurrence.SPECIFIC_DATE to "Specific Date"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recurrenceOptions.forEach { (rec, title) ->
                                FilterChip(
                                    selected = recurrence == rec,
                                    onClick = { recurrence = rec },
                                    label = { Text(title, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Days of week selector if CUSTOM (Starts Sunday to Saturday, responsive & scroll-safe)
                        if (recurrence == PtsRecurrence.CUSTOM) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick preset pills: All, Weekdays, Weekends, Clear
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { selectedWeekdayBitmask = 0b1111111 },
                                    label = { Text("All Days", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        selectedWeekdayBitmask = (1 shl (Calendar.MONDAY - 1)) or
                                                (1 shl (Calendar.TUESDAY - 1)) or
                                                (1 shl (Calendar.WEDNESDAY - 1)) or
                                                (1 shl (Calendar.THURSDAY - 1)) or
                                                (1 shl (Calendar.FRIDAY - 1))
                                    },
                                    label = { Text("Weekdays", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        selectedWeekdayBitmask = (1 shl (Calendar.SUNDAY - 1)) or
                                                (1 shl (Calendar.SATURDAY - 1))
                                    },
                                    label = { Text("Weekends", fontSize = 10.sp) }
                                )
                                SuggestionChip(
                                    onClick = { selectedWeekdayBitmask = 0 },
                                    label = { Text("Clear", fontSize = 10.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val daysOfWeek = listOf(
                                "Sun" to Calendar.SUNDAY,
                                "Mon" to Calendar.MONDAY,
                                "Tue" to Calendar.TUESDAY,
                                "Wed" to Calendar.WEDNESDAY,
                                "Thu" to Calendar.THURSDAY,
                                "Fri" to Calendar.FRIDAY,
                                "Sat" to Calendar.SATURDAY
                            )

                            // 7 day circular toggles, scrollable container ensures Sunday or Saturday are never clipped
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                daysOfWeek.forEach { (name, calDay) ->
                                    val bit = 1 shl (calDay - 1)
                                    val isSelected = (selectedWeekdayBitmask and bit) != 0

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 44.dp)
                                            .clickable {
                                                selectedWeekdayBitmask = selectedWeekdayBitmask xor bit
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatCustomDays(selectedWeekdayBitmask),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedWeekdayBitmask != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (recurrence == PtsRecurrence.SPECIFIC_DATE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Applies only on the selected calendar date.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Recovery buffer
                    Column {
                        Text("Post-Activity Recovery Buffer: $recoveryBufferMinutes min", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 15, 30, 45).forEach { buf ->
                                FilterChip(
                                    selected = recoveryBufferMinutes == buf,
                                    onClick = { recoveryBufferMinutes = buf },
                                    label = { Text(if (buf == 0) "None" else "${buf}m") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isAddingNew = false
                                editingSlotId = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = canSave,
                            onClick = {
                                val savedSlot = ProtectedTimeSlot(
                                    id = editingSlotId ?: UUID.randomUUID().toString(),
                                    label = if (label.isNotBlank()) label.trim() else "${getPtsCategoryEmoji(category)} ${category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    startMinuteOfDay = startMinuteOfDay,
                                    durationMinutes = durationMinutes,
                                    recurrence = recurrence,
                                    weekdayBitmask = if (recurrence == PtsRecurrence.CUSTOM) selectedWeekdayBitmask else 0b1111111,
                                    category = category,
                                    recoveryBufferMinutes = recoveryBufferMinutes,
                                    isEnabled = if (editingSlotId != null) (protectedSlots.find { it.id == editingSlotId }?.isEnabled ?: true) else true,
                                    dateMillis = if (recurrence == PtsRecurrence.SPECIFIC_DATE) selectedDateMillis else null
                                )
                                onSaveSlot(savedSlot)
                                isAddingNew = false
                                editingSlotId = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (editingSlotId != null) "Update Block" else "Save Block")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    startHour = timePickerState.hour
                    startMinute = timePickerState.minute
                    if (endHour * 60 + endMinute <= startHour * 60 + startMinute) {
                        val newEnd = (startHour * 60 + startMinute + 60).coerceAtMost(1439)
                        endHour = newEnd / 60
                        endMinute = newEnd % 60
                    }
                    showStartTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    endHour = timePickerState.hour
                    endMinute = timePickerState.minute
                    showEndTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProtectedSlotItem(
    slot: ProtectedTimeSlot,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getPtsCategoryColor(slot.category)
    val startH = slot.startMinuteOfDay / 60
    val startM = slot.startMinuteOfDay % 60
    val endMinuteOfDay = slot.startMinuteOfDay + slot.durationMinutes
    val endH = (endMinuteOfDay / 60) % 24
    val endM = endMinuteOfDay % 60
    val timeRangeStr = "${formatTime(startH, startM)} - ${formatTime(endH, endM)}"
    val recurrenceStr = formatRecurrenceLabel(slot)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (slot.isEnabled) categoryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (slot.isEnabled) categoryColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = categoryColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(getPtsCategoryEmoji(slot.category), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (slot.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeRangeStr • ${slot.durationMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (slot.isEnabled) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = recurrenceStr + if (slot.recoveryBufferMinutes > 0) " • +${slot.recoveryBufferMinutes}m buffer" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Switch(
                    checked = slot.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}
