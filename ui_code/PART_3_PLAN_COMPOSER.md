# neuroFlow UI Codebase — Plan Screen, Scheduling Timeline & Smart Composer Sheet

**Description:** PlanScreenV2, PlanViewModelV2, SmartTaskComposerSheetV2, PlanAndComposerUpgradesV2, and WeeklySchedule.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 7  
**Lines of Code:** 5,690  
**Uncompressed Size:** 270.9 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/schedule/ProtectedSlotBottomSheet.kt`](#appsrcmainjavacomneuroflowapppresentationscheduleprotectedslotbottomsheetkt)
- [`app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt`](#appsrcmainjavacomneuroflowapppresentationscheduleweeklyschedulescreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationscheduleweeklyscheduleviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2composersmarttaskcomposersheetv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanAndComposerUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2planplanandcomposerupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2planplanscreenv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2planplanviewmodelv2kt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationscheduleprotectedslotbottomsheetkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/schedule/ProtectedSlotBottomSheet.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/schedule/ProtectedSlotBottomSheet.kt`
- **Lines of Code:** 896
- **Size:** 42,799 bytes (41.8 KB)

```kotlin
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
                        Spacer(modifier = Modifier.height(8.dp))
                        com.neuroflow.app.presentation.v2.settings.ProtectedSlotPhysiologicalPreviewBar(
                            selectedCategory = category,
                            onSelectCategory = { category = it }
                        )
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
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
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
```

---

<a id="appsrcmainjavacomneuroflowapppresentationscheduleweeklyschedulescreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt`
- **Lines of Code:** 587
- **Size:** 29,167 bytes (28.5 KB)

```kotlin
package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import com.neuroflow.app.presentation.common.isProtectedSlotOnDay
import com.neuroflow.app.presentation.common.isSameCalendarDay
import com.neuroflow.app.presentation.common.isTaskOnDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HOUR_HEIGHT = 64.dp
private val DAY_COLUMN_WIDTH = 110.dp
private val TIME_GUTTER_WIDTH = 52.dp

private fun formatHourLabel(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDay: (Long) -> Unit,
    onNavigateToFocus: (String) -> Unit,
    hideTopBar: Boolean = false,
    viewModel: WeeklyScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showProtectedSlotsSheet by remember { mutableStateOf(false) }
    var showIntentEditDialog by remember { mutableStateOf(false) }
    var intentInputText by remember { mutableStateOf("") }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)
    val currentMinuteOfDay = currentHour * 60 + currentMinute
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val density = LocalDensity.current

    // Auto-scroll to current time and today on initial load
    LaunchedEffect(Unit) {
        val targetHour = (currentHour - 1).coerceAtLeast(0)
        val targetY = with(density) { (targetHour * HOUR_HEIGHT.toPx()).toInt() }
        verticalScrollState.scrollTo(targetY)

        val todayIndex = uiState.weekDays.indexOfFirst { isSameCalendarDay(it, todayMillis) }
        if (todayIndex > 0) {
            val targetX = with(density) { ((todayIndex - 1).coerceAtLeast(0) * DAY_COLUMN_WIDTH.toPx()).toInt() }
            horizontalScrollState.scrollTo(targetX)
        }
    }

    val weekStartFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val weekEndFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val weekRangeText = if (uiState.weekDays.isNotEmpty()) {
        "${weekStartFormat.format(Date(uiState.weekDays.first()))} – ${weekEndFormat.format(Date(uiState.weekDays.last()))}"
    } else {
        "Weekly Schedule"
    }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Weekly Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.goToCurrentWeek()
                                val targetHour = (currentHour - 1).coerceAtLeast(0)
                                val targetY = with(density) { (targetHour * HOUR_HEIGHT.toPx()).toInt() }
                                verticalScrollState.dispatchRawDelta((targetY - verticalScrollState.value).toFloat())
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "This Week",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showProtectedSlotsSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protected Slots",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Week switcher bar & Focus banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // Navigation controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousWeek() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Week")
                        }

                        Text(
                            text = weekRangeText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.nextWeek() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next Week")
                        }
                    }

                    // Weekly Focus / Intention
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                intentInputText = uiState.weeklyIntent
                                showIntentEditDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.weeklyIntent.isNotBlank()) uiState.weeklyIntent else "Tap to set weekly focus / goal...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (uiState.weeklyIntent.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                    color = if (uiState.weeklyIntent.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit weekly goal",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timetable Header: Frozen vertically, scrolls horizontally in sync with grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Top-left corner spacer aligned with time gutter
                Box(
                    modifier = Modifier
                        .width(TIME_GUTTER_WIDTH)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // 7 Day Headers: Sunday to Saturday
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    val dayHeaderNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val dayHeaderNumFormat = SimpleDateFormat("d", Locale.getDefault())

                    uiState.weekDays.forEach { dayMillis ->
                        val isToday = isSameCalendarDay(dayMillis, todayMillis)
                        val dayDate = Date(dayMillis)

                        Surface(
                            modifier = Modifier
                                .width(DAY_COLUMN_WIDTH)
                                .height(48.dp)
                                .clickable { onNavigateToDay(dayMillis) },
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayHeaderNameFormat.format(dayDate).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = dayHeaderNumFormat.format(dayDate),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timetable Body: Left time gutter + 7 day columns, scrolls vertically & horizontally
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Left Time Axis: 24 Hours
                Column(
                    modifier = Modifier
                        .width(TIME_GUTTER_WIDTH)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    for (hour in 0 until 24) {
                        Box(
                            modifier = Modifier
                                .width(TIME_GUTTER_WIDTH)
                                .height(HOUR_HEIGHT)
                                .padding(end = 4.dp, top = 2.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = formatHourLabel(hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // 7 Day Grid Columns: Sunday to Saturday
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    uiState.weekDays.forEach { dayMillis ->
                        val isToday = isSameCalendarDay(dayMillis, todayMillis)
                        val dayStart = dayMillis
                        val dayEnd = startOfNextLocalDay(dayStart)

                        // Filter tasks and protected slots for this day
                        val dayTasks = uiState.activeTasks.filter { isTaskOnDay(it, dayStart, dayEnd) }
                        val daySlots = uiState.protectedSlots.filter { isProtectedSlotOnDay(it, dayMillis) }

                        Box(
                            modifier = Modifier
                                .width(DAY_COLUMN_WIDTH)
                                .height(HOUR_HEIGHT * 24)
                                .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.02f) else Color.Transparent)
                        ) {
                            // Hour divider lines & clickable hour cells
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (hour in 0 until 24) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(HOUR_HEIGHT)
                                            .clickable { onNavigateToDay(dayMillis) }
                                    ) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.align(Alignment.TopCenter)
                                        )
                                    }
                                }
                            }

                            // Right vertical column border
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                    .align(Alignment.CenterEnd)
                            )

                            // Render Protected Time Slots
                            daySlots.forEach { slot ->
                                val slotOffsetY = (slot.startMinuteOfDay / 60f) * HOUR_HEIGHT.value
                                val slotHeight = ((slot.durationMinutes / 60f) * HOUR_HEIGHT.value).coerceAtLeast(24f)
                                val categoryColor = getPtsCategoryColor(slot.category)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = categoryColor.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, categoryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .offset(y = slotOffsetY.dp)
                                        .height(slotHeight.dp)
                                        .clickable { showProtectedSlotsSheet = true }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = getPtsCategoryEmoji(slot.category),
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = slot.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (slotHeight >= 36f) {
                                            val startH = slot.startMinuteOfDay / 60
                                            val startM = slot.startMinuteOfDay % 60
                                            val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
                                            val endH = ((slot.startMinuteOfDay + slot.durationMinutes) / 60) % 24
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:%02d-%02d:%02d", startH, startM, endH, endM),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Black.copy(alpha = 0.75f),
                                                fontSize = 8.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Render Scheduled Tasks
                            dayTasks.forEach { task ->
                                val startMinute = task.timelineStartMinuteOfDay()
                                val durationMins = (if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 45).coerceIn(15, 360)
                                val taskOffsetY = (startMinute / 60f) * HOUR_HEIGHT.value
                                val taskHeight = ((durationMins / 60f) * HOUR_HEIGHT.value).coerceAtLeast(24f)

                                val bgColor = getQuadrantBgColor(task.quadrant)
                                val textColor = getQuadrantTextColor(task.quadrant)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = bgColor,
                                    border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .offset(y = taskOffsetY.dp)
                                        .height(taskHeight.dp)
                                        .clickable { onNavigateToFocus(task.id) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 10.sp
                                        )
                                        if (taskHeight >= 36f) {
                                            val startH = startMinute / 60
                                            val startM = startMinute % 60
                                            val endTotal = startMinute + durationMins
                                            val endH = (endTotal / 60) % 24
                                            val endM = endTotal % 60
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:%02d (%dm)", startH, startM, durationMins),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 8.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Today current time indicator
                            if (isToday) {
                                val indicatorY = (currentMinuteOfDay / 60f) * HOUR_HEIGHT.value
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = indicatorY.dp)
                                ) {
                                    // Red line across day
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.error,
                                        thickness = 2.dp,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    // Dot at start
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .align(Alignment.CenterStart)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Protected Slots Bottom Sheet
    if (showProtectedSlotsSheet) {
        ProtectedSlotBottomSheet(
            protectedSlots = uiState.protectedSlots,
            onDismiss = { showProtectedSlotsSheet = false },
            onSaveSlot = { slot -> viewModel.addProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.removeProtectedSlot(slotId) },
            onToggleSlot = { slotId -> viewModel.toggleProtectedSlot(slotId) }
        )
    }

    // Weekly Intent Edit Dialog
    if (showIntentEditDialog) {
        AlertDialog(
            onDismissRequest = { showIntentEditDialog = false },
            title = { Text("Weekly Focus / Intention") },
            text = {
                Column {
                    Text(
                        "Set your core goal or priority for this week to keep planning aligned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = intentInputText,
                        onValueChange = { intentInputText = it },
                        placeholder = { Text("e.g. Finish feature launch, deep study") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveWeeklyIntent(intentInputText.trim())
                        showIntentEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntentEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationscheduleweeklyscheduleviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleViewModel.kt`
- **Lines of Code:** 152
- **Size:** 5,079 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WeeklyScheduleUiState(
    val weekDays: List<Long> = emptyList(), // 7 days from Sunday to Saturday
    val activeTasks: List<TaskEntity> = emptyList(),
    val protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    val weeklyIntent: String = "",
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val isLoading: Boolean = false
)

fun getSundayWeekDays(referenceMillis: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromSunday = currentDayOfWeek - Calendar.SUNDAY
        add(Calendar.DAY_OF_YEAR, -daysFromSunday)
    }

    val days = mutableListOf<Long>()
    for (i in 0 until 7) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return days
}

@HiltViewModel
class WeeklyScheduleViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val currentReferenceMillis = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(
        WeeklyScheduleUiState(
            weekDays = getSundayWeekDays()
        )
    )
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    init {
        observeWeekDays()
        observeTasks()
        observePreferences()
    }

    private fun observeWeekDays() {
        viewModelScope.launch {
            currentReferenceMillis.collect { refMillis ->
                _uiState.update { it.copy(weekDays = getSundayWeekDays(refMillis)) }
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeActiveTasks().collect { tasks ->
                _uiState.update { it.copy(activeTasks = tasks) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.protectedTimeSlots == new.protectedTimeSlots &&
                    old.weeklyIntent == new.weeklyIntent &&
                    old.workDayStart == new.workDayStart &&
                    old.workDayEnd == new.workDayEnd
                }
                .collect { prefs ->
                    _uiState.update {
                        it.copy(
                            protectedSlots = prefs.protectedTimeSlots,
                            weeklyIntent = prefs.weeklyIntent,
                            workDayStart = prefs.workDayStart,
                            workDayEnd = prefs.workDayEnd
                        )
                    }
                }
        }
    }

    fun nextWeek() {
        val next = Calendar.getInstance().apply {
            timeInMillis = currentReferenceMillis.value
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis
        currentReferenceMillis.value = next
    }

    fun previousWeek() {
        val prev = Calendar.getInstance().apply {
            timeInMillis = currentReferenceMillis.value
            add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis
        currentReferenceMillis.value = prev
    }

    fun goToCurrentWeek() {
        currentReferenceMillis.value = System.currentTimeMillis()
    }

    fun saveWeeklyIntent(intent: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(weeklyIntent = intent) }
        }
    }

    fun addProtectedSlot(slot: ProtectedTimeSlot) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.filterNot { it.id == slot.id } + slot
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }

    fun removeProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.filterNot { it.id == slotId }
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }

    fun toggleProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.map {
                if (it.id == slotId) it.copy(isEnabled = !it.isEnabled) else it
            }
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2composersmarttaskcomposersheetv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt`
- **Lines of Code:** 1,102
- **Size:** 56,387 bytes (55.1 KB)

```kotlin
package com.neuroflow.app.presentation.v2.composer

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.v2.theme.LocalNeuroFlowColors
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SheetMode {
    data object Create : SheetMode
    data class Edit(val task: TaskEntity) : SheetMode
}

data class ComposerTagPreset(
    val tag: String,
    val emoji: String,
    val shortBoost: String,
    val category: String,
    val energyDemand: String,
    val biologicalWindow: String,
    val boostDescription: String,
    val fragmentationTolerance: String
)

val SMART_TAG_PRESETS: List<ComposerTagPreset> = listOf(
    ComposerTagPreset("deepwork", "⚡", "+8.5pt Batching", "Analytical", "High (80)", "Peak Morning", "+8.5pt Attentional Batching, places in 90m peak focus window", "Low (0.20) — Protected from splits"),
    ComposerTagPreset("coding", "💻", "+8.5pt Batching", "Analytical", "High (80)", "Peak Morning", "+8.5pt Same-Context Batching, shields deep focus state", "Low (0.25) — Protected from splits"),
    ComposerTagPreset("study", "📚", "+8.5pt Batching", "Hard Work", "Medium (50)", "Morning Focus", "+8.5pt Batching, optimal memory encoding", "Moderate (0.35)"),
    ComposerTagPreset("creative", "🎨", "+15pt Rebound", "Creative", "Medium (50)", "Evening Rebound Phase", "+15pt Divergent Thinking Rebound Bonus when inhibition lowers", "Moderate (0.40)"),
    ComposerTagPreset("writing", "✍️", "+6pt Batching", "Creative", "Medium (50)", "Morning / Rebound", "+6.0pt Context Affinity, continuous narrative flow", "Moderate (0.40)"),
    ComposerTagPreset("admin", "📋", "Protects Peak", "Routine", "Low (25)", "Afternoon Post-Lunch Dip", "Zero Peak Drain — slot-filled during low biological dip", "High (0.95) — Safe to interrupt"),
    ComposerTagPreset("meetings", "🤝", "Midday Coalesce", "Routine", "Medium (50)", "Midday Plateau", "Coalesces meetings with 5m buffers, protects morning blocks", "High (0.80)"),
    ComposerTagPreset("exercise", "🏃", "+12pt Endorphin", "Physical", "High (Exertion)", "Morning / Afternoon", "+10-12pt Alertness & BDNF boost for 45m post-workout", "Flexible (0.50)"),
    ComposerTagPreset("mindfulness", "🧘", "+100pt Reset", "Mindfulness", "Low (Restorative)", "Recovery Needed / Night", "+100pt Affinity during high cognitive exhaustion / depletion", "High (0.85) — Non-split"),
    ComposerTagPreset("chores", "🧹", "Active Rest", "Physical", "Low (25)", "Flexible / Dips", "Active Rest — physical movement during circadian dips", "High (0.90)"),
    ComposerTagPreset("quickwin", "⏱️", "+15pt Momentum", "Low Friction", "Low (25)", "Immediate / Any", "+15pt Momentum Pull, low initiation barrier for ADHD activation", "High (1.00)")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartTaskComposerSheetV2(
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    mode: SheetMode = SheetMode.Create,
    circadianSummary: DailyCircadianSummary? = null,
    completedTasks: List<TaskEntity> = emptyList()
) {
    val existingTask = (mode as? SheetMode.Edit)?.task
    var rawInput by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var userDurationOverride by remember(existingTask) { mutableStateOf<Int?>(existingTask?.estimatedDurationMinutes) }
    var userEnergyOverride by remember(existingTask) { mutableStateOf<EnergyLevel?>(existingTask?.energyLevel) }
    var userTaskTypeOverride by remember(existingTask) { mutableStateOf<TaskType?>(existingTask?.taskType) }
    var userFrogOverride by remember(existingTask) { mutableStateOf<Boolean?>(existingTask?.isFrog) }
    var userPriorityOverride by remember(existingTask) { mutableStateOf<Priority?>(existingTask?.priority) }

    // Strategic criteria & scheduling state
    var impactScore by remember(existingTask) { mutableStateOf(existingTask?.impactScore?.toFloat() ?: 50f) }
    var effortScore by remember(existingTask) { mutableStateOf(existingTask?.effortScore?.toFloat() ?: 50f) }
    var userScheduledDate by remember(existingTask) { mutableStateOf<Long?>(existingTask?.scheduledDate) }
    var userScheduledTime by remember(existingTask) { mutableStateOf<Long?>(existingTask?.scheduledTime) }
    var userDeadlineDate by remember(existingTask) { mutableStateOf<Long?>(existingTask?.deadlineDate) }
    var userDeadlineTime by remember(existingTask) { mutableStateOf<Long?>(existingTask?.deadlineTime) }
    var isScheduleLocked by remember(existingTask) { mutableStateOf(existingTask?.isScheduleLocked ?: false) }
    var recurrence by remember(existingTask) { mutableStateOf(existingTask?.recurrence ?: Recurrence.NONE) }

    var isExpanded by remember { mutableStateOf(existingTask != null) }
    var showSyntaxGuide by remember { mutableStateOf(false) }
    var showTagCatalogGuide by remember { mutableStateOf(false) }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
    var selectedQuadrant by remember(existingTask) { mutableStateOf(existingTask?.quadrant ?: Quadrant.DO_FIRST) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf("deadline") }
    var timePickerTarget by remember { mutableStateOf("deadline") }

    val inferred = remember(rawInput, completedTasks) {
        TaskInferenceEngine.inferFromInput(rawInput, completedTasks)
    }
    val effectiveDuration = userDurationOverride ?: inferred.durationMinutes
    val effectiveEnergy = userEnergyOverride ?: inferred.energyDemand
    val effectiveTaskType = userTaskTypeOverride ?: inferred.taskType
    val effectiveFrog = userFrogOverride ?: inferred.isFrog
    val effectivePriority = userPriorityOverride ?: inferred.priority
    val effectiveDeadlineDate = userDeadlineDate ?: inferred.deadlineDate
    val effectiveScheduledDate = userScheduledDate ?: inferred.scheduledDate

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                rawInput = spokenText
                userDurationOverride = null
                userEnergyOverride = null
                userTaskTypeOverride = null
                userFrogOverride = null
                userPriorityOverride = null
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun toggleTagInInput(tagName: String) {
        val target = "#$tagName"
        val regex = Regex("""(?i)\b#$tagName\b""")
        rawInput = if (rawInput.contains(regex)) {
            rawInput.replace(regex, "").replace(Regex("""\s{2,}"""), " ").trim()
        } else {
            if (rawInput.isBlank()) target else "${rawInput.trim()} $target"
        }
        userDurationOverride = null
        userEnergyOverride = null
        userTaskTypeOverride = null
        userFrogOverride = null
        userPriorityOverride = null
    }

    fun handleSave() {
        val title = (if (mode is SheetMode.Edit) rawInput.trim() else inferred.sanitizedTitle.ifBlank { rawInput.trim() })
        if (title.isBlank()) return

        val taskToSave = if (existingTask != null) {
            existingTask.copy(
                title = title,
                description = description.trim(),
                estimatedDurationMinutes = effectiveDuration,
                energyLevel = effectiveEnergy,
                taskType = effectiveTaskType,
                isFrog = effectiveFrog,
                quadrant = selectedQuadrant,
                priority = effectivePriority,
                impactScore = impactScore.toInt(),
                effortScore = effortScore.toInt(),
                deadlineDate = effectiveDeadlineDate,
                deadlineTime = userDeadlineTime,
                scheduledDate = effectiveScheduledDate,
                scheduledTime = userScheduledTime,
                isScheduleLocked = isScheduleLocked,
                recurrence = recurrence,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            TaskEntity(
                title = title,
                description = description.trim(),
                estimatedDurationMinutes = effectiveDuration,
                energyLevel = effectiveEnergy,
                taskType = effectiveTaskType,
                tags = inferred.contextTags.joinToString(","),
                isFrog = effectiveFrog,
                quadrant = selectedQuadrant,
                priority = effectivePriority,
                impactScore = impactScore.toInt(),
                effortScore = effortScore.toInt(),
                deadlineDate = effectiveDeadlineDate,
                deadlineTime = userDeadlineTime,
                scheduledDate = effectiveScheduledDate,
                scheduledTime = userScheduledTime,
                isScheduleLocked = isScheduleLocked,
                recurrence = recurrence
            )
        }
        onSave(taskToSave)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (mode is SheetMode.Edit) "Edit Task" else "Smart Task Composer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    IconButton(
                        onClick = { showSyntaxGuide = !showSyntaxGuide },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Formatting Guide",
                            tint = if (showSyntaxGuide) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }

            // Syntax Guide & Inspiration Examples (Expandable)
            AnimatedVisibility(visible = showSyntaxGuide) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NeuroFlowThemeV2Colors.WarmAmber, modifier = Modifier.size(16.dp))
                            Text(
                                "Natural Language Formatting Guide",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Type naturally! The engine automatically detects durations, deadlines, priorities, and tags:",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• ⏱️ Duration: '30m', '45m', '90m', '1h 30m'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 📅 Deadline: 'tomorrow', 'Friday 10am', 'today at 3pm'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 🐸 Crucial: '!frog' (sets daily frog), '#high', '!urgent'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 🏷️ Tags & Boosts: '#deepwork', '#coding', '#creative', '#admin'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                    }
                }
            }

            // Quick Preset Starters (when input is empty)
            if (rawInput.isBlank()) {
                Text(
                    "💡 Quick Starters (Tap to try):",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Ship core patch 45m #coding #deepwork",
                        "Workout & cardio 30m #exercise tomorrow",
                        "Inbox zero 15m #admin",
                        "Roadmap brainstorm 60m #creative",
                        "Mindful reset 10m #mindfulness"
                    ).forEach { sample ->
                        Surface(
                            onClick = { rawInput = sample },
                            shape = RoundedCornerShape(50),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // NLP Input Field
            OutlinedTextField(
                value = rawInput,
                onValueChange = {
                    rawInput = it
                    userDurationOverride = null
                    userEnergyOverride = null
                    userTaskTypeOverride = null
                    userFrogOverride = null
                    userPriorityOverride = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "e.g. Design core mockups 90m #high !frog tomorrow",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextTertiary
                    )
                },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task (e.g. 'Ship quarterly patch 45m #high')")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                )
            )

            // Interactive Inferred & Override Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Duration Chip with Calibration Transparency
                val durationLabel = if (inferred.isDurationExplicit) {
                    "⏱ ${effectiveDuration}m"
                } else {
                    "⏱ ${effectiveDuration}m (${inferred.calibrationFactorApplied}x)"
                }
                FilterChip(
                    selected = true,
                    onClick = {
                        val cycles = listOf(15, 25, 45, 60, 90, 120)
                        val nextIdx = (cycles.indexOf(effectiveDuration) + 1) % cycles.size
                        userDurationOverride = cycles[nextIdx]
                    },
                    label = { Text(durationLabel) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Energy Chip
                FilterChip(
                    selected = true,
                    onClick = {
                        userEnergyOverride = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> EnergyLevel.MEDIUM
                            EnergyLevel.MEDIUM -> EnergyLevel.LOW
                            EnergyLevel.LOW -> EnergyLevel.HIGH
                        }
                    },
                    label = {
                        val label = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "⚡ High Energy"
                            EnergyLevel.MEDIUM -> "🔋 Med Energy"
                            EnergyLevel.LOW -> "☕ Low Energy"
                        }
                        Text(label)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Priority Chip (Interactive cycle)
                FilterChip(
                    selected = true,
                    onClick = {
                        userPriorityOverride = when (effectivePriority) {
                            Priority.HIGH -> Priority.MEDIUM
                            Priority.MEDIUM -> Priority.LOW
                            Priority.LOW -> Priority.HIGH
                        }
                    },
                    label = {
                        val pLabel = when (effectivePriority) {
                            Priority.HIGH -> "🔴 High Priority"
                            Priority.MEDIUM -> "🟡 Med Priority"
                            Priority.LOW -> "🟢 Low Priority"
                        }
                        Text(pLabel)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Frog Chip
                FilterChip(
                    selected = effectiveFrog,
                    onClick = { userFrogOverride = !effectiveFrog },
                    label = { Text(if (effectiveFrog) "🐸 Frog (Crucial)" else "🐸 Normal") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Context Tags Chips
                inferred.contextTags.forEach { tag ->
                    SuggestionChip(
                        onClick = { toggleTagInInput(tag) },
                        label = { Text("#$tag ✕") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Available Tags & Boosts Section (User Guidance & 1-Tap Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "🏷️ Available Tags & Boosts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        "(Tap to add)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextTertiary
                    )
                }

                TextButton(
                    onClick = { showTagCatalogGuide = !showTagCatalogGuide },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showTagCatalogGuide) "Hide Details" else "View Boost Guide",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontally Scrollable Tag Presets with Boost Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SMART_TAG_PRESETS.forEach { preset ->
                    val isSelected = rawInput.contains("#${preset.tag}", ignoreCase = true) ||
                        inferred.contextTags.any { it.equals(preset.tag, ignoreCase = true) }

                    Surface(
                        onClick = { toggleTagInInput(preset.tag) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(preset.emoji, fontSize = 12.sp)
                            Text(
                                text = "#${preset.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f) else NeuroFlowThemeV2Colors.SlateSurface
                            ) {
                                Text(
                                    text = preset.shortBoost,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Tag Catalog Guide Table (Expandable)
            AnimatedVisibility(visible = showTagCatalogGuide) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚡ Biophysical Scheduling Boost Matrix",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        SMART_TAG_PRESETS.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${p.emoji} #${p.tag}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.width(90.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${p.biologicalWindow} • ${p.energyDemand} Energy",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeuroFlowThemeV2Colors.TextPrimary
                                    )
                                    Text(
                                        text = "${p.boostDescription} (${p.fragmentationTolerance})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (p != SMART_TAG_PRESETS.last()) {
                                HorizontalDivider(color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // Inferred Scheduling Strategy Preview Banner
            val matchedPresets = SMART_TAG_PRESETS.filter { p ->
                rawInput.contains("#${p.tag}", ignoreCase = true) || inferred.contextTags.any { it.equals(p.tag, ignoreCase = true) }
            }
            if (matchedPresets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(15.dp))
                            Text(
                                "Active Scheduling Strategy:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        matchedPresets.forEach { p ->
                            Text(
                                text = "• #${p.tag}: ${p.boostDescription} • ${p.biologicalWindow}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Slot Placement Preview Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Column {
                        val peakHour = circadianSummary?.peakWindowStartHour ?: 9
                        val dipHour = circadianSummary?.dipWindowStartHour ?: 14
                        val plateauHour = ((peakHour + 5) % 24).coerceIn(13, 17)

                        fun formatHour(hour: Int): String {
                            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                            val amPm = if (hour < 12) "AM" else "PM"
                            return String.format("%02d:00 %s", h, amPm)
                        }

                        val slotRecommendation = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "Recommended Slot: ${formatHour(peakHour)} (Peak Biological Alertness)"
                            EnergyLevel.MEDIUM -> "Recommended Slot: ${formatHour(plateauHour)} (Focus Plateau)"
                            EnergyLevel.LOW -> "Recommended Slot: ${formatHour(dipHour)} (Recovery & Admin)"
                        }
                        Text(
                            text = slotRecommendation,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progressive Disclosure: Detailed Sliders & Calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp)
                    .semantics { role = Role.Button },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Detailed Criteria" else "Detailed Criteria (Calendar, Impact, Schedule, Effort)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Deadline & Calendar
                    Text("📅 Deadline Date & Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    datePickerTarget = "deadline"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                                Text(
                                    text = if (effectiveDeadlineDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(effectiveDeadlineDate)) else "Pick Due Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    timePickerTarget = "deadline"
                                    showTimePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                                Text(
                                    text = if (userDeadlineTime != null) {
                                        val h = (userDeadlineTime!! / 3_600_000L).toInt()
                                        val m = ((userDeadlineTime!! % 3_600_000L) / 60_000L).toInt()
                                        String.format("%02d:%02d", h, m)
                                    } else "Pick Due Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scheduled Slot & Calendar
                    Text("⏰ Scheduled Slot Date & Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    datePickerTarget = "scheduled"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                Text(
                                    text = if (effectiveScheduledDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(effectiveScheduledDate)) else "Pick Slot Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    timePickerTarget = "scheduled"
                                    showTimePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                Text(
                                    text = if (userScheduledTime != null) {
                                        val h = (userScheduledTime!! / 3_600_000L).toInt()
                                        val m = ((userScheduledTime!! % 3_600_000L) / 60_000L).toInt()
                                        String.format("%02d:%02d", h, m)
                                    } else "Pick Slot Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recurrence / Habit
                    Text("🔁 Recurrence / Habit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Recurrence.NONE to "None",
                            Recurrence.DAILY to "Daily",
                            Recurrence.WEEKLY to "Weekly",
                            Recurrence.MONTHLY to "Monthly"
                        ).forEach { (recOption, label) ->
                            val isSelected = recurrence == recOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { recurrence = recOption },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                    selectedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lock Schedule Toggle (Pin to Slot)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.5f) else NeuroFlowThemeV2Colors.SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isScheduleLocked = !isScheduleLocked }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isScheduleLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isScheduleLocked) "🔒 Schedule Locked (Pinned)" else "Schedule Unlocked (Flexible)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isScheduleLocked) "Auto-scheduler will never displace this task" else "Auto-scheduler can optimize slot placement",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isScheduleLocked,
                                onCheckedChange = { isScheduleLocked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NeuroFlowThemeV2Colors.WarmAmber
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Strategic Impact Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⭐ Strategic Impact: ${impactScore.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (impactScore >= 75) "Crucial" else "Standard", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.WarmAmber)
                    }
                    Slider(
                        value = impactScore,
                        onValueChange = { impactScore = it },
                        valueRange = 0f..100f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Effort Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💪 Cognitive Effort: ${effortScore.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (effortScore >= 70) "Deep Focus" else "Light", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.SageGreen)
                    }
                    Slider(
                        value = effortScore,
                        onValueChange = { effortScore = it },
                        valueRange = 0f..100f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description / Notes
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes & Objectives") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = ::handleSave,
                enabled = rawInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
            ) {
                Icon(if (mode is SheetMode.Edit) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mode is SheetMode.Edit) "Save Changes" else "Schedule Into Energy Stream",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // DatePickerDialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            if (datePickerTarget == "deadline") userDeadlineDate = selected
                            else userScheduledDate = selected
                        }
                        showDatePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // TimePickerDialog
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = timePickerState.hour * 3_600_000L + timePickerState.minute * 60_000L
                        if (timePickerTarget == "deadline") userDeadlineTime = millis
                        else userScheduledTime = millis
                        showTimePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun MetadataPill(
    icon: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = icon, fontSize = 11.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2planplanandcomposerupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanAndComposerUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanAndComposerUpgradesV2.kt`
- **Lines of Code:** 300
- **Size:** 12,945 bytes (12.6 KB)

```kotlin
package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.service.TaskEnrichmentResult
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

@Composable
fun ScheduleConflictResolverStrip(
    conflicts: List<ScheduleConflict>,
    onResolveConflictClick: (ScheduleConflict) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = conflicts.isNotEmpty(), modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            conflicts.take(2).forEach { conflict ->
                Surface(
                    color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.RecoveryRose, 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeuroFlowPaletteV2.RecoveryRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = conflict.taskTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = NeuroFlowPaletteV2.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = conflict.conflictDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary
                                )
                            }
                        }
                        if (conflict.suggestedNewStartMinute != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NeuroFlowPaletteV2.RecoveryRose,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { onResolveConflictClick(conflict) }
                            ) {
                                Text(
                                    text = "Shift to ${ConflictResolver.formatMinutesToTime(conflict.suggestedNewStartMinute)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactEisenhowerMatrixFilter(
    countsByQuadrant: Map<Quadrant, Int>,
    selectedQuadrant: Quadrant?,
    onSelectQuadrant: (Quadrant?) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrants = listOf(
        Triple(Quadrant.DO_FIRST, "Do First", NeuroFlowPaletteV2.RecoveryRose),
        Triple(Quadrant.SCHEDULE, "Schedule", NeuroFlowPaletteV2.ReboundCyan),
        Triple(Quadrant.DELEGATE, "Quick / Delegate", NeuroFlowPaletteV2.PeakAmber),
        Triple(Quadrant.ELIMINATE, "Someday / Cut", NeuroFlowPaletteV2.WindDownSlate)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            quadrants.take(2).forEach { (quad, title, accent) ->
                QuadrantFilterTile(
                    title = title,
                    count = countsByQuadrant[quad] ?: 0,
                    accent = accent,
                    isSelected = selectedQuadrant == quad,
                    onClick = { onSelectQuadrant(if (selectedQuadrant == quad) null else quad) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            quadrants.drop(2).forEach { (quad, title, accent) ->
                QuadrantFilterTile(
                    title = title,
                    count = countsByQuadrant[quad] ?: 0,
                    accent = accent,
                    isSelected = selectedQuadrant == quad,
                    onClick = { onSelectQuadrant(if (selectedQuadrant == quad) null else quad) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuadrantFilterTile(
    title: String,
    count: Int,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accent.copy(alpha = 0.16f) else NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            brush = NeuroFlowPaletteV2.specularBorderBrush(
                if (isSelected) accent else NeuroFlowPaletteV2.BorderSubtle,
                if (isSelected) 1f else 0.7f
            )
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) accent else NeuroFlowPaletteV2.TextPrimary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
            }
        }
    }
}

@Composable
fun ComposerAiEnrichmentPreviewCard(
    enrichment: TaskEnrichmentResult?,
    isLoading: Boolean,
    onApplyEnrichmentClick: (TaskEnrichmentResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLoading && enrichment == null) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoading) "BREAKING DOWN INITIATION FRICTION..." else "AI EXECUTIVE FUNCTION BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                if (enrichment != null) {
                    Surface(
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onApplyEnrichmentClick(enrichment) }
                    ) {
                        Text(
                            text = "Apply Steps",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (enrichment != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ${enrichment.activationTip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                enrichment.microSteps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. ${step.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (idx == 0) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowPaletteV2.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${step.estimatedMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2planplanscreenv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`
- **Lines of Code:** 1,703
- **Size:** 88,165 bytes (86.1 KB)

```kotlin
package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.presentation.v2.flow.TaskSlotEnergyFitBadge
import com.neuroflow.app.presentation.v2.flow.UltradianRecoveryDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreenV2(
    onNavigateToFocus: (String) -> Unit,
    onNavigateToWeeklySchedule: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    viewModel: PlanViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNotesSheet by remember { mutableStateOf(false) }
    var taskToLogTime by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToScheduleTime by remember { mutableStateOf<TaskEntity?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var isObjectivesExpanded by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var showProtectedSlotSheet by remember { mutableStateOf(false) }

    val handleCompleteTask: (TaskEntity) -> Unit = { task ->
        if (task.isFrog) showFrogConfetti = true
        viewModel.completeTask(task)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Completed: ${task.title}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.uncompleteTask(task)
            }
        }
    }

    LaunchedEffect(state.pendingDeletedTask) {
        val task = state.pendingDeletedTask
        if (task != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${task.title}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteTask()
            } else {
                viewModel.confirmDeleteTask()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userActionMessage.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Anchored Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Executive Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "Circadian Time-Blocking & Prioritization",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSearchVisible() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (state.isSearchVisible || state.searchQuery.isNotBlank()) {
                                NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            } else {
                                NeuroFlowThemeV2Colors.TextSecondary
                            }
                        )
                    }

                    IconButton(
                        onClick = onNavigateToWeeklySchedule,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Weekly Schedule",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }

                    IconButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Quick Notes",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.triggerAutoScheduleNow() },
                        enabled = !state.isAutoScheduling,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        if (state.isAutoScheduling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 2. Anchored Mode Selector Segmented Tabs (Never jumps vertically)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                PlanViewModeV2.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PlanViewModeV2.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            activeContentColor = NeuroFlowThemeV2Colors.TextPrimary,
                            inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                            inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // 3. Collapsible Search Input Bar
            AnimatedVisibility(visible = state.isSearchVisible) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search tasks, descriptions, or #tags...",
                            fontSize = 13.sp,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        unfocusedContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                        focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                )
            }

            // 4. Shared Tag Filter Strip (Shown in Timeline & Matrix modes only)
            if (state.viewMode != PlanViewModeV2.AUTOPILOT) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact inline pills for Unscheduled Tray & Active Objectives
                    if (state.unscheduledTasks.isNotEmpty()) {
                        item(key = "staging_pill") {
                            FilterChip(
                                selected = state.isStagingDrawerExpanded,
                                onClick = { viewModel.toggleStagingDrawer() },
                                label = {
                                    Text(
                                        text = "📥 Unscheduled (${state.unscheduledTasks.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = NeuroFlowThemeV2Colors.WarmAmber,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.WarmAmber
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.isStagingDrawerExpanded,
                                    borderColor = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.4f),
                                    selectedBorderColor = NeuroFlowThemeV2Colors.WarmAmber
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    item(key = "objectives_pill") {
                        FilterChip(
                            selected = isObjectivesExpanded,
                            onClick = { isObjectivesExpanded = !isObjectivesExpanded },
                            label = {
                                Text(
                                    text = if (state.activeGoals.isNotEmpty()) "🎯 Objectives (${state.activeGoals.size})" else "🎯 Objectives",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    items(state.availableTags, key = { it }) { tag ->
                        val isSelected = if (tag == "All") {
                            state.selectedTag == null || state.selectedTag == "All"
                        } else {
                            state.selectedTag == tag
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTag(if (tag == "All") null else tag) },
                            label = {
                                Text(
                                    tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Expandable Unscheduled Tray Drawer
                AnimatedVisibility(visible = state.isStagingDrawerExpanded && state.unscheduledTasks.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.unscheduledTasks, key = { it.id }) { unscheduled ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurface,
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { editingTask = unscheduled }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = unscheduled.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${unscheduled.estimatedDurationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${unscheduled.priority.name} • Tap to schedule",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.WarmAmber,
                                            fontSize = 10.sp
                                        )
                                    }
                                    val deferral = state.deferrals.firstOrNull { it.taskId == unscheduled.id }
                                    if (deferral != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "⏳ ${deferral.reasonCode.name.replace('_', ' ')}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.WarmAmber,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Objectives Drawer
                AnimatedVisibility(visible = isObjectivesExpanded) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.activeGoals.isEmpty()) {
                                Text(
                                    text = "No active objectives yet. Set a north-star goal below:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            } else {
                                state.activeGoals.forEach { goal ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.completeGoal(goal) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Complete Objective",
                                                tint = NeuroFlowThemeV2Colors.BioMint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteGoal(goal) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Objective",
                                                tint = NeuroFlowThemeV2Colors.TextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Inline Add Goal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newGoalTitle,
                                    onValueChange = { newGoalTitle = it },
                                    placeholder = { Text("New Objective...", fontSize = 12.sp, color = NeuroFlowThemeV2Colors.TextTertiary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = NeuroFlowThemeV2Colors.TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (newGoalTitle.isNotBlank()) {
                                            viewModel.addGoal(newGoalTitle)
                                            newGoalTitle = ""
                                        }
                                    },
                                    enabled = newGoalTitle.isNotBlank(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Objective",
                                        tint = if (newGoalTitle.isNotBlank()) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Mode-Scoped Content View
            when (state.viewMode) {
                PlanViewModeV2.TIMELINE -> TimelineScheduleView(
                    state = state,
                    onSelectDate = { viewModel.selectDate(it) },
                    onResolveConflict = { viewModel.resolveConflict(it) },
                    onAutoBalanceClick = { viewModel.triggerAutoScheduleNow() },
                    onStartFocus = onNavigateToFocus,
                    onCompleteTask = handleCompleteTask,
                    onDeleteTask = { viewModel.requestDeleteTask(it) },
                    onBreakDownTask = { viewModel.breakDownTask(it) },
                    onShiftToPeak = { viewModel.shiftTaskToPeak(it) },
                    onAssignTaskTime = { task -> taskToScheduleTime = task },
                    onLogPastTime = { taskToLogTime = it },
                    onEditTask = { task -> editingTask = task }
                )
                PlanViewModeV2.MATRIX -> MatrixPlanView(
                    state = state,
                    onSelectQuadrant = { viewModel.selectQuadrant(it) },
                    onStartFocus = onNavigateToFocus,
                    onMoveQuadrant = { task, q -> viewModel.moveTaskToQuadrant(task, q) },
                    onCompleteTask = handleCompleteTask,
                    onBreakDownTask = { viewModel.breakDownTask(it) },
                    onEditTask = { task -> editingTask = task }
                )
                PlanViewModeV2.AUTOPILOT -> AutopilotConfigView(
                    state = state,
                    onToggleAutoSchedule = { viewModel.toggleAutoSchedule() },
                    onTriggerNow = { viewModel.triggerAutoScheduleNow() },
                    onToggleSlot = { id, enabled -> viewModel.toggleProtectedSlot(id, enabled) },
                    onManageSlotsClick = { showProtectedSlotSheet = true }
                )
            }
        }
    }

    if (showProtectedSlotSheet) {
        com.neuroflow.app.presentation.schedule.ProtectedSlotBottomSheet(
            protectedSlots = state.protectedSlots,
            onDismiss = { showProtectedSlotSheet = false },
            onSaveSlot = { slot -> viewModel.saveProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.deleteProtectedSlot(slotId) },
            onToggleSlot = { slotId ->
                val slot = state.protectedSlots.firstOrNull { it.id == slotId }
                if (slot != null) {
                    viewModel.toggleProtectedSlot(slotId, !slot.isEnabled)
                }
            }
        )
    }

    if (showNotesSheet) {
        QuickNotesBottomSheet(
            notes = recentNotes,
            onSaveNote = { title, content -> viewModel.saveQuickNote(content, title) },
            onDismiss = { showNotesSheet = false },
            onOpenFullNotes = onNavigateToNotes,
            onCreateTask = { title, desc -> viewModel.createTaskFromNote(title, desc) },
            onSynthesizeNote = { viewModel.synthesizeQuickNote(it) }
        )
    }

    taskToLogTime?.let { task ->
        ManualTimeLogSheet(
            taskTitle = task.title,
            prefillMinutes = task.estimatedDurationMinutes,
            onConfirm = { minutes ->
                viewModel.logPastTime(task, minutes)
                taskToLogTime = null
            },
            onSkip = { taskToLogTime = null }
        )
    }

    editingTask?.let { task ->
        SmartTaskComposerSheetV2(
            onDismiss = { editingTask = null },
            mode = SheetMode.Edit(task),
            circadianSummary = state.circadianSummary,
            onSave = { updated ->
                editingTask = null
                viewModel.updateTask(updated)
            }
        )
    }

    taskToScheduleTime?.let { task ->
        val initialMinuteOfDay = task.timelineStartMinuteOfDay()
        val initialHour = initialMinuteOfDay / 60
        val initialMinute = initialMinuteOfDay % 60
        val timePickerState = rememberTimePickerState(
            initialHour = if (initialHour in 0..23) initialHour else 9,
            initialMinute = if (initialMinute in 0..59) initialMinute else 0
        )
        AlertDialog(
            onDismissRequest = { taskToScheduleTime = null },
            title = {
                Text(
                    text = "Assign Time Slot",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.assignTaskTime(task, timePickerState.hour, timePickerState.minute)
                        taskToScheduleTime = null
                    }
                ) {
                    Text("Confirm", color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToScheduleTime = null }) {
                    Text("Cancel", color = NeuroFlowThemeV2Colors.TextSecondary)
                }
            },
            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
        )
    }

    FrogConfettiOverlay(trigger = showFrogConfetti)
    LaunchedEffect(showFrogConfetti) {
        if (showFrogConfetti) {
            kotlinx.coroutines.delay(2500)
            showFrogConfetti = false
        }
    }
}

@Composable
fun DateCarouselStrip(
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit
) {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val days = remember {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            cal.timeInMillis
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { dateMillis ->
            val isSelected = isSameDay(selectedDateMillis, dateMillis)
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val dayName = when {
                isSameDay(todayCal.timeInMillis, dateMillis) -> "Today"
                isSameDay(todayCal.timeInMillis + 86400000L, dateMillis) -> "Tmrw"
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            }
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)

            Surface(
                onClick = { onSelectDate(dateMillis) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo else NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
                ),
                modifier = Modifier
                    .width(60.dp)
                    .heightIn(min = 48.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun shouldShowUltradianBufferAfter(tasks: List<TaskEntity>, index: Int): Boolean {
    if (index >= tasks.size - 1) return false
    var runSum = 0
    for (i in 0..index) {
        runSum += tasks[i].estimatedDurationMinutes
        if (i == index) {
            return runSum >= 90
        }
        if (runSum >= 90) {
            runSum = 0
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScheduleView(
    state: PlanUiStateV2,
    onSelectDate: (Long) -> Unit = {},
    onResolveConflict: (com.neuroflow.app.domain.scheduler.ScheduleConflict) -> Unit = {},
    onAutoBalanceClick: () -> Unit = {},
    onStartFocus: (String) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit = {},
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onShiftToPeak: (TaskEntity) -> Unit = {},
    onAssignTaskTime: (TaskEntity) -> Unit = {},
    onLogPastTime: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {}
) {
    val peakStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9
    val peakEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12
    val currentMinuteOfDay = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
    val listState = rememberLazyListState()

    // Scroll to the first upcoming task for today on initial load
    LaunchedEffect(state.selectedDateMillis, state.tasksForDay.size) {
        val upcomingIdx = state.tasksForDay.indexOfFirst { it.timelineStartMinuteOfDay() >= currentMinuteOfDay - 30 }
        if (upcomingIdx > 0) {
            listState.animateScrollToItem(upcomingIdx)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DateCarouselStrip(
            selectedDateMillis = state.selectedDateMillis,
            onSelectDate = onSelectDate
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 1. Daily Cognitive Budget Meter (Scoped inside scrollable timeline)
            item(key = "budget_meter") {
                val scheduledMinutes = state.tasksForDay.sumOf { it.estimatedDurationMinutes }
                val capacityMinutes = state.preferences.dailyDeepWorkTargetMinutes.takeIf { it > 0 }
                    ?: state.circadianSummary?.recommendedDeepWorkMinutes ?: 360
                com.neuroflow.app.presentation.v2.theme.CognitiveBudgetMeterCard(
                    scheduledMinutesToday = scheduledMinutes,
                    recommendedCapacityMinutes = capacityMinutes,
                    conflictCount = state.conflicts.size,
                    onAutoBalanceClick = onAutoBalanceClick
                )
            }

            // 2. Schedule Conflict Resolver Banner (Wired to move the conflicting task)
            if (state.conflicts.isNotEmpty()) {
                item(key = "conflicts_strip") {
                    ScheduleConflictResolverStrip(
                        conflicts = state.conflicts,
                        onResolveConflictClick = onResolveConflict
                    )
                }
            }

            // 3. Circadian Peak Indicator Badge
            item(key = "peak_window_badge") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 15.sp)
                        Text(
                            text = "Peak Circadian Window: ${String.format("%02d:00", peakStart)} – ${String.format("%02d:00", peakEnd)} (Optimal for Frogs & High Effort)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.WarmAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4. Scheduled Tasks
            if (state.tasksForDay.isEmpty()) {
                item(key = "empty_timeline") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurface,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📅 Clear Horizon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Text(
                                if (state.searchQuery.isNotBlank()) {
                                    "No scheduled tasks match \"${state.searchQuery}\" on this day."
                                } else {
                                    "No tasks scheduled for this day. Tap 'Auto-Plan' to schedule your backlog into your peak biological windows."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(state.tasksForDay, key = { _, it -> it.id }) { index, task ->
                    val minuteOfDay = task.timelineStartMinuteOfDay()
                    val h = minuteOfDay / 60
                    val m = minuteOfDay % 60
                    val timeLabel = String.format("%02d:%02d", h, m)
                    val isDuringPeak = h in peakStart until peakEnd

                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.55f },
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onCompleteTask(task)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onDeleteTask(task)
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.25f)
                                SwipeToDismissBoxValue.EndToStart -> NeuroFlowThemeV2Colors.RoseCoralContainer.copy(alpha = 0.5f)
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                            }
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                SwipeToDismissBoxValue.Settled -> Alignment.Center
                            }
                            val icon = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                SwipeToDismissBoxValue.Settled -> null
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                            NeuroFlowThemeV2Colors.RoseCoral
                                        } else {
                                            NeuroFlowThemeV2Colors.TextPrimary
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDuringPeak) NeuroFlowThemeV2Colors.SlateSurfaceAlt else NeuroFlowThemeV2Colors.SlateSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.5f) else NeuroFlowThemeV2Colors.SlateBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Time Pill
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(48.dp)
                                ) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                    if (isDuringPeak) {
                                        Text(
                                            "PEAK",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = NeuroFlowThemeV2Colors.WarmAmber
                                        )
                                    }
                                }

                                // Complete check
                                IconButton(
                                    onClick = { onCompleteTask(task) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Complete",
                                        tint = NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                }

                                // Title & Metadata (Tap to Edit)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditTask(task) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (task.isFrog) Text("🐸", fontSize = 14.sp)
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${task.estimatedDurationMinutes}m • ${task.energyLevel.name} • ${task.quadrant.name.replace('_', ' ')}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )

                                        val isUltradianPart = task.id.contains("#part") || task.title.contains("(Part ")
                                        if (isUltradianPart) {
                                            val partLabel = if (task.id.contains("#part2") || task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = partLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        if (task.postponeCount >= 3) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "⚡ Dopamine Warm-Up",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    val scheduledMillis = task.resolveScheduledEpochMillis() ?: (task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) })
                                    if (scheduledMillis != null && state.energyTimeline.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TaskSlotEnergyFitBadge(
                                            taskScheduledMillis = scheduledMillis,
                                            taskEnergyDemand = task.energyLevel,
                                            timeline = state.energyTimeline,
                                            onShiftToPeakClick = { onShiftToPeak(task) }
                                        )
                                    }

                                    val microSteps = remember(task.ifThenPlan) { StepByStepPlanCodec.parse(task.ifThenPlan) }
                                    if (microSteps.isNotEmpty()) {
                                        var showSteps by remember { mutableStateOf(false) }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.18f),
                                            modifier = Modifier.clickable { showSteps = !showSteps }
                                        ) {
                                            Text(
                                                text = "🪜 ${microSteps.size} micro-steps ${if (showSteps) "▲" else "▼"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (showSteps) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                microSteps.forEachIndexed { i, step ->
                                                    Text(
                                                        text = "${i + 1}. ${step.text}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = NeuroFlowThemeV2Colors.TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Focus Button
                                IconButton(
                                    onClick = { onStartFocus(task.id) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Start Focus",
                                        tint = NeuroFlowThemeV2Colors.ElectricIndigo
                                    )
                                }

                                // Consolidated Overflow Menu (AI Breakdown, Edit, Log Time)
                                var showItemMenu by remember { mutableStateOf(false) }
                                val isDecomposing = state.decomposingTaskId == task.id
                                Box {
                                    IconButton(
                                        onClick = { showItemMenu = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (isDecomposing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Task options",
                                                tint = NeuroFlowThemeV2Colors.TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showItemMenu,
                                        onDismissRequest = { showItemMenu = false },
                                        modifier = Modifier.background(NeuroFlowThemeV2Colors.SlateSurfaceAlt)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Break Down (AI Micro-Steps)", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onBreakDownTask(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Edit Task", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onEditTask(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Assign Time Slot", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onAssignTaskTime(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Log Past Time", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.History, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onLogPastTime(task)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (shouldShowUltradianBufferAfter(state.tasksForDay, index)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        UltradianRecoveryDivider()
                    }
                }
            }

            // 5. Protected Time Slots Section
            if (state.protectedSlots.any { it.isEnabled }) {
                item(key = "protected_slots_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🛡️ Protected Deep Work & Rest Slots",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                items(state.protectedSlots.filter { it.isEnabled }, key = { "slot_${it.id}" }) { slot ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    slot.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                                val startH = slot.startMinuteOfDay / 60
                                val startM = slot.startMinuteOfDay % 60
                                val endH = (slot.startMinuteOfDay + slot.durationMinutes) / 60
                                val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
                                Text(
                                    text = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, slot.category.name),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixPlanView(
    state: PlanUiStateV2,
    onSelectQuadrant: (Quadrant?) -> Unit = {},
    onStartFocus: (String) -> Unit,
    onMoveQuadrant: (TaskEntity, Quadrant) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {}
) {
    val coral = NeuroFlowThemeV2Colors.MutedCoral
    val indigoGlow = NeuroFlowThemeV2Colors.ElectricIndigoGlow
    val sage = NeuroFlowThemeV2Colors.SageGreen
    val textTertiary = NeuroFlowThemeV2Colors.TextTertiary

    val allQuadrants = remember(coral, indigoGlow, sage, textTertiary) {
        listOf(
            Quadrant.DO_FIRST to ("🎯 Do First (High Impact & Frog)" to coral),
            Quadrant.SCHEDULE to ("🔵 Schedule (High Impact, Deep Focus)" to indigoGlow),
            Quadrant.DELEGATE to ("🟢 Quick Wins / Low Energy" to sage),
            Quadrant.ELIMINATE to ("⚪ Backlog / Low Priority" to textTertiary)
        )
    }

    val countsByQuad = remember(state.tasksByQuadrant) {
        Quadrant.entries.associateWith { q -> state.tasksByQuadrant[q]?.size ?: 0 }
    }

    val visibleQuadrants = remember(allQuadrants, state.selectedQuadrant) {
        if (state.selectedQuadrant == null) {
            allQuadrants
        } else {
            allQuadrants.filter { it.first == state.selectedQuadrant }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
    ) {
        // Interactive 2x2 Quadrant Filter Grid (Now accurately reflects Matrix counts & filters view)
        item(key = "matrix_filter_grid") {
            CompactEisenhowerMatrixFilter(
                countsByQuadrant = countsByQuad,
                selectedQuadrant = state.selectedQuadrant,
                onSelectQuadrant = onSelectQuadrant
            )
        }

        items(visibleQuadrants, key = { it.first.name }) { (quadrant, info) ->
            val (title, color) = info
            val tasks = state.tasksByQuadrant[quadrant] ?: emptyList()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${tasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks in this quadrant",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        tasks.forEach { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onCompleteTask(task) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Complete",
                                            tint = NeuroFlowThemeV2Colors.TextSecondary
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onEditTask(task) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (task.isFrog) Text("🐸", fontSize = 12.sp)
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            "${task.estimatedDurationMinutes}m • ${task.energyLevel.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )

                                        val matrixSteps = remember(task.ifThenPlan) { StepByStepPlanCodec.parse(task.ifThenPlan) }
                                        if (matrixSteps.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "🪜 ${matrixSteps.size} steps",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    val isDecomposingMatrix = state.decomposingTaskId == task.id
                                    IconButton(
                                        onClick = { onBreakDownTask(task) },
                                        modifier = Modifier.size(40.dp),
                                        enabled = !isDecomposingMatrix
                                    ) {
                                        if (isDecomposingMatrix) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "Break down",
                                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val nextQ = when (quadrant) {
                                                Quadrant.DO_FIRST -> Quadrant.SCHEDULE
                                                Quadrant.SCHEDULE -> Quadrant.DELEGATE
                                                Quadrant.DELEGATE -> Quadrant.ELIMINATE
                                                Quadrant.ELIMINATE -> Quadrant.DO_FIRST
                                            }
                                            onMoveQuadrant(task, nextQ)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Cycle Quadrant",
                                            tint = NeuroFlowThemeV2Colors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onStartFocus(task.id) },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Focus",
                                            tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutopilotConfigView(
    state: PlanUiStateV2,
    onToggleAutoSchedule: () -> Unit,
    onTriggerNow: () -> Unit,
    onToggleSlot: (String, Boolean) -> Unit,
    onManageSlotsClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        item(key = "autopilot_master") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🤖 Circadian Autopilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Text(
                                "Automatically distributes your tasks across the day matched to your biological alertness and energy curves.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }
                        Switch(
                            checked = state.isAutoScheduleOn,
                            onCheckedChange = { onToggleAutoSchedule() },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = onTriggerNow,
                        enabled = !state.isAutoScheduling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                    ) {
                        if (state.isAutoScheduling) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeuroFlowThemeV2Colors.TextPrimary)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Auto-Schedule Optimization Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "autopilot_rules") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🧠 Neuroscience Auto-Scheduling Rules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                    Text(
                        "• Process C Alertness Matching: High cognitive effort and Frog tasks are assigned to peak circadian windows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text(
                        "• Protected Buffer Preservation: Deep work contracts and rest intervals are strictly respected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text(
                        "• Cognitive Fatigue Guard: Tasks exceeding 60m are scheduled with automatic recharge buffers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        }

        item(key = "protected_slots_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🛡️ Manage Protected Time Slots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
                TextButton(onClick = onManageSlotsClick) {
                    Text("+ Add Slot", color = NeuroFlowThemeV2Colors.ElectricIndigo, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.protectedSlots.isEmpty()) {
            item(key = "protected_slots_empty") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onManageSlotsClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No protected time slots configured.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("+ Create your first protected block", style = MaterialTheme.typography.labelMedium, color = NeuroFlowThemeV2Colors.ElectricIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        items(state.protectedSlots, key = { it.id }) { slot ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            slot.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        val startH = slot.startMinuteOfDay / 60
                        val startM = slot.startMinuteOfDay % 60
                        val endH = (slot.startMinuteOfDay + slot.durationMinutes) / 60
                        val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
                        Text(
                            text = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, slot.category.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    Switch(
                        checked = slot.isEnabled,
                        onCheckedChange = { onToggleSlot(slot.id, it) }
                    )
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2planplanviewmodelv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
- **Lines of Code:** 950
- **Size:** 42,815 bytes (41.8 KB)

```kotlin
package com.neuroflow.app.presentation.v2.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject

enum class PlanViewModeV2(val title: String) {
    TIMELINE("Timeline"),
    MATRIX("Matrix"),
    AUTOPILOT("AI Autopilot")
}

data class PlanUiStateV2(
    val selectedDateMillis: Long = startOfTodayMillis(),
    val viewMode: PlanViewModeV2 = PlanViewModeV2.TIMELINE,
    val tasksForDay: List<TaskEntity> = emptyList(),
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val tasksByQuadrant: Map<Quadrant, List<TaskEntity>> = emptyMap(),
    val activeGoals: List<com.neuroflow.app.data.local.entity.GoalEntity> = emptyList(),
    val protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val isAutoScheduleOn: Boolean = false,
    val isAutoScheduling: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
    val energyTimeline: List<CircadianSnapshot> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val pendingDeletedTask: TaskEntity? = null,
    val isStagingDrawerExpanded: Boolean = false,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val decomposingTaskId: String? = null,
    val selectedQuadrant: Quadrant? = null,
    val deferrals: List<DeferralRationale> = emptyList()
)

private fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@HiltViewModel
class PlanViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val workManager: WorkManager,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val quickNoteDao: QuickNoteDao? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    private val calendarSyncRepository: com.neuroflow.app.data.calendar.CalendarSyncRepository? = null,
    private val goalRepository: com.neuroflow.app.data.repository.GoalRepository? = null,
    @ApplicationContext private val context: Context? = null,
    private val geminiAiOnlineService: GeminiAiOnlineService? = null
) : ViewModel() {

    private val scheduleOrchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = taskSchedulerService,
        behavioralSupportService = behavioralSupportService,
        sleepTelemetryRepository = sleepTelemetryRepository
    )

    private val _selectedDate = MutableStateFlow(startOfTodayMillis())
    private val _viewMode = MutableStateFlow(PlanViewModeV2.TIMELINE)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isAutoScheduling = MutableStateFlow(false)
    private val _pendingDeletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _isStagingDrawerExpanded = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchVisible = MutableStateFlow(false)
    private val _decomposingTaskId = MutableStateFlow<String?>(null)
    private val _selectedQuadrant = MutableStateFlow<Quadrant?>(null)
    private val _deferrals = MutableStateFlow<List<DeferralRationale>>(emptyList())

    private val _userActionMessage = MutableSharedFlow<String>()
    val userActionMessage: SharedFlow<String> = _userActionMessage.asSharedFlow()

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    private data class PlanNavParams(
        val dateMillis: Long,
        val mode: PlanViewModeV2,
        val autoScheduling: Boolean,
        val tag: String?,
        val pendingDeletedTask: TaskEntity?,
        val isStagingDrawerExpanded: Boolean,
        val searchQuery: String,
        val isSearchVisible: Boolean,
        val decomposingTaskId: String?,
        val selectedQuadrant: Quadrant?,
        val deferrals: List<DeferralRationale> = emptyList()
    )

    val uiState: StateFlow<PlanUiStateV2> = combine(
        combine(
            combine(_selectedDate, _viewMode, _isAutoScheduling, _selectedTag) { date, mode, autoScheduling, tag ->
                listOf<Any?>(date, mode, autoScheduling, tag)
            },
            combine(_pendingDeletedTask, _isStagingDrawerExpanded, _searchQuery) { pendingTask, drawerExpanded, searchQ ->
                listOf<Any?>(pendingTask, drawerExpanded, searchQ)
            },
            combine(_isSearchVisible, _decomposingTaskId, _selectedQuadrant, _deferrals) { searchVis, decompId, selectedQuad, defs ->
                listOf<Any?>(searchVis, decompId, selectedQuad, defs)
            }
        ) { first, second, third ->
            @Suppress("UNCHECKED_CAST")
            PlanNavParams(
                dateMillis = first[0] as Long,
                mode = first[1] as PlanViewModeV2,
                autoScheduling = first[2] as Boolean,
                tag = first[3] as String?,
                pendingDeletedTask = second[0] as TaskEntity?,
                isStagingDrawerExpanded = second[1] as Boolean,
                searchQuery = second[2] as String,
                isSearchVisible = third[0] as Boolean,
                decomposingTaskId = third[1] as String?,
                selectedQuadrant = third[2] as Quadrant?,
                deferrals = third[3] as List<DeferralRationale>
            )
        },
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.workDayStart == new.workDayStart &&
                old.workDayEnd == new.workDayEnd &&
                old.autoSchedulingEnabled == new.autoSchedulingEnabled &&
                old.protectedTimeSlots == new.protectedTimeSlots &&
                old.wakeUpHour == new.wakeUpHour &&
                old.sleepHour == new.sleepHour &&
                old.sleepPressurePoints == new.sleepPressurePoints &&
                old.stimulantLogsJson == new.stimulantLogsJson &&
                old.quickPhysiologicalLogsJson == new.quickPhysiologicalLogsJson &&
                old.quizChronotype == new.quizChronotype &&
                old.manualChronotype == new.manualChronotype &&
                old.meqScore == new.meqScore &&
                old.manualPeakProfileEnabled == new.manualPeakProfileEnabled &&
                old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay
        },
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList()),
        goalRepository?.observeActiveGoals() ?: flowOf(emptyList())
    ) { params, allTasks, prefs, sleepLogs, activeGoals ->
        val dateMillis = params.dateMillis
        val mode = params.mode
        val autoScheduling = params.autoScheduling
        val tag = params.tag

        val now = System.currentTimeMillis()
        val zoneId = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(dateMillis).atZone(zoneId)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (anchor.toInstant().toEpochMilli() > dateMillis) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        val quickPhysNaps = physLogs
            .filter { it.type == com.neuroflow.app.data.local.PhysiologicalEventType.NAP }
            .map { log ->
                DaytimeNapBout(
                    startMillis = log.timestampMillis - log.durationMinutes.coerceAtLeast(10) * 60_000L,
                    endMillis = log.timestampMillis
                )
            }
        val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

        val resolvedChronotype = if (!prefs.manualChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.manualChronotype)
        } else if (!prefs.quizChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.quizChronotype)
        } else if (prefs.meqScore > 0) {
            when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
        } else {
            ChronotypeProfile.INTERMEDIATE
        }

        val allStimulants = parseStimulantLogs(prefs.stimulantLogsJson)

        val (_, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            nowMillis = now,
            stimulantDoses = allStimulants
        )

        val tagFilteredAll = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            allTasks
        } else {
            val q = tag.removePrefix("#").trim()
            allTasks.filter { it.tags.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true) }
        }

        val searchFilteredAll = if (params.searchQuery.isBlank()) {
            tagFilteredAll
        } else {
            val query = params.searchQuery.trim()
            tagFilteredAll.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.tags.contains(query, ignoreCase = true)
            }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1

        val tasksForDay = searchFilteredAll.filter { task ->
            val sDate = task.scheduledDate
            if (sDate != null) {
                sDate in dayStart..dayEnd
            } else {
                task.deadlineDate != null && task.deadlineDate in dayStart..dayEnd
            }
        }.sortedBy { it.scheduledTime ?: it.timelineStartMinuteOfDay()?.toLong() ?: Long.MAX_VALUE }

        val unscheduled = searchFilteredAll.filter { it.scheduledDate == null }

        val byQuadrant = Quadrant.entries.associateWith { q ->
            searchFilteredAll.filter { it.quadrant == q }
        }

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = tasksForDay,
            protectedSlots = prefs.protectedTimeSlots,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            dayMillis = dateMillis
        )

        val manualPeakHour = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = dateMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = resolvedChronotype,
                manualPeakStartHour = manualPeakHour,
                stimulantDoses = allStimulants
            )
        }.getOrNull()

        val energyTimeline = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dateMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 30,
                zoneId = zoneId,
                naps = naps,
                chronotype = resolvedChronotype,
                manualPeakStartHour = manualPeakHour,
                stimulantDoses = allStimulants
            )
        }.getOrDefault(emptyList())

        PlanUiStateV2(
            selectedDateMillis = dateMillis,
            viewMode = mode,
            tasksForDay = tasksForDay,
            unscheduledTasks = unscheduled,
            tasksByQuadrant = byQuadrant,
            activeGoals = activeGoals,
            protectedSlots = prefs.protectedTimeSlots,
            conflicts = conflicts,
            isAutoScheduleOn = prefs.autoSchedulingEnabled,
            isAutoScheduling = autoScheduling,
            selectedTag = tag,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
            energyTimeline = energyTimeline,
            preferences = prefs,
            isLoading = false,
            pendingDeletedTask = params.pendingDeletedTask,
            isStagingDrawerExpanded = params.isStagingDrawerExpanded,
            searchQuery = params.searchQuery,
            isSearchVisible = params.isSearchVisible,
            decomposingTaskId = params.decomposingTaskId,
            selectedQuadrant = params.selectedQuadrant,
            deferrals = params.deferrals
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlanUiStateV2()
    )

    fun setViewMode(mode: PlanViewModeV2) {
        _viewMode.value = mode
    }

    fun selectQuadrant(quadrant: Quadrant?) {
        _selectedQuadrant.value = quadrant
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun toggleAutoSchedule() {
        viewModelScope.launch {
            val current = uiState.value.isAutoScheduleOn
            val nextState = !current
            preferencesDataStore.updatePreferences {
                it.copy(
                    autoSchedulingEnabled = nextState,
                    isAutoScheduleToggleOn = nextState
                )
            }
        }
    }

    fun autoScheduleDay(dateMillis: Long = _selectedDate.value, forceReplan: Boolean = false) {
        viewModelScope.launch {
            _isAutoScheduling.value = true
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val allKnownTasks = taskRepository.getAllTasks().ifEmpty { taskRepository.getActiveTasks() }
                val allTasks = taskRepository.getActiveTasks()
                val zoneId = java.time.ZoneId.systemDefault()
                val dayDate = java.time.Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
                val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEnd = dayStart + 24 * 3600_000L - 1

                val externalBusyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
                val fixedWindows = allTasks.filter { it.isScheduleLocked }.mapNotNull { task ->
                    val sDate = task.scheduledDate ?: return@mapNotNull null
                    val sTime = task.scheduledTime ?: return@mapNotNull null
                    val start = sDate + sTime
                    val duration = task.plannedBlockMinutes.takeIf { it > 0 } ?: task.estimatedDurationMinutes
                    val end = start + duration * 60_000L
                    if (end > dayStart && start < dayEnd) com.neuroflow.app.domain.service.TimeSlotWindow(start, end) else null
                }
                val totalBusyWindows = externalBusyWindows + fixedWindows
                val nowMillis = System.currentTimeMillis()

                val candidateTasks = allTasks.filter { task ->
                    val sDate = task.scheduledDate
                    sDate == null || (sDate in dayStart..dayEnd && !task.isScheduleLocked)
                }

                val allSessions = runCatching {
                    sessionRepository?.getAllSessions().orEmpty()
                }.getOrDefault(emptyList())
                val closedSessions = allSessions.filter { it.endedAt != null }
                val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId
                val microStepCounts = candidateTasks.associate { task ->
                    val steps = taskRepository.getMicroSteps(task.id)
                    task.id to Pair(steps.count { it.isCompleted }, steps.size)
                }
                val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

                val outcome = scheduleOrchestrator.orchestrateDayScheduleWithOutcome(
                    candidateTasks = candidateTasks,
                    allKnownTasks = allKnownTasks,
                    closedSessions = closedSessions,
                    sleepLogs = sleepLogs,
                    prefs = prefs,
                    dayDate = dayDate,
                    nowMillis = nowMillis,
                    busyWindows = totalBusyWindows,
                    forceReplan = forceReplan,
                    zoneId = zoneId,
                    activeTaskId = activeTaskId,
                    microStepCounts = microStepCounts
                )
                val placements = outcome.placements
                _deferrals.value = outcome.deferrals

                val placementsByBaseId = placements.groupBy { it.task.id.substringBefore("#part") }

                for ((baseTaskId, taskPlacements) in placementsByBaseId) {
                    val primaryPlacement = taskPlacements.first()
                    val fresh = taskRepository.getById(baseTaskId) ?: continue
                    if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE || fresh.isScheduleLocked) continue
                    val calSlot = Calendar.getInstance().apply { timeInMillis = primaryPlacement.window.startMillis }
                    val startOfDay = calSlot.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val timeOffset = primaryPlacement.window.startMillis - startOfDay

                    val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
                    val titleToUse = if (isSplit && !fresh.title.contains("(Part ")) "${fresh.title} (Part 1/2)" else fresh.title
                    val updated = fresh.copy(
                        title = titleToUse,
                        scheduledDate = startOfDay,
                        scheduledTime = timeOffset,
                        isAutoScheduled = true,
                        plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                        lastAutoScheduledAt = nowMillis,
                        updatedAt = nowMillis
                    )
                    taskRepository.update(updated)
                    if (updated.isFrog) {
                        calendarSyncRepository?.upsertTaskEvent(
                            updated,
                            startMillis = primaryPlacement.window.startMillis,
                            durationMinutes = primaryPlacement.window.durationMinutes
                        )
                    }

                    val part2Placement = taskPlacements.getOrNull(1)
                    if (part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
                        val calSlot2 = Calendar.getInstance().apply { timeInMillis = part2Placement.window.startMillis }
                        val startOfDay2 = calSlot2.apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        val timeOffset2 = part2Placement.window.startMillis - startOfDay2
                        val part2Id = "${baseTaskId}_part2"
                        val existingPart2 = taskRepository.getById(part2Id)
                        val part2Title = if (!part2Placement.task.title.contains("(Part ")) "${fresh.title.substringBefore(" (Part ")} (Part 2/2)" else part2Placement.task.title
                        val part2Entity = (existingPart2 ?: fresh).copy(
                            id = part2Id,
                            title = part2Title,
                            parentTaskId = baseTaskId,
                            dependsOnTaskIds = baseTaskId,
                            estimatedDurationMinutes = part2Placement.window.durationMinutes,
                            plannedBlockMinutes = part2Placement.window.durationMinutes,
                            scheduledDate = startOfDay2,
                            scheduledTime = timeOffset2,
                            isAutoScheduled = true,
                            isFrog = false,
                            lastAutoScheduledAt = nowMillis,
                            updatedAt = nowMillis
                        )
                        if (existingPart2 != null) {
                            taskRepository.update(part2Entity)
                        } else {
                            taskRepository.insert(part2Entity)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlanViewModelV2", "Auto-schedule failed", e)
            } finally {
                _isAutoScheduling.value = false
            }
        }
    }

    fun triggerAutoScheduleNow() {
        viewModelScope.launch {
            autoScheduleDay(_selectedDate.value, forceReplan = false)
            val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
            workManager.enqueueUniqueWork("AutoScheduleNowV2", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun resolveConflict(conflict: ScheduleConflict) {
        val suggestedMinute = conflict.suggestedNewStartMinute
        val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == conflict.taskId }
            ?: uiState.value.tasksForDay.firstOrNull { it.title == conflict.taskTitle }
        if (targetTask != null && suggestedMinute != null) {
            assignTaskTime(targetTask, suggestedMinute / 60, suggestedMinute % 60)
            viewModelScope.launch {
                _userActionMessage.emit(
                    "Shifted '${targetTask.title.take(24)}' to ${ConflictResolver.formatMinutesToTime(suggestedMinute)}"
                )
            }
        } else {
            triggerAutoScheduleNow()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
        }
    }

    fun moveTaskToQuadrant(task: TaskEntity, newQuadrant: Quadrant) {
        viewModelScope.launch {
            taskRepository.update(task.copy(quadrant = newQuadrant, updatedAt = System.currentTimeMillis()))
        }
    }

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            _decomposingTaskId.value = task.id
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val generated = behavioralSupportService.generateMicroStepsWithAi(
                    taskTitle = task.title,
                    estimatedMinutes = task.estimatedDurationMinutes,
                    apiKey = prefs.geminiApiKey,
                    useAi = prefs.geminiAiEnabled
                )
                val microStepEntities = generated.mapIndexed { index, suggestion ->
                    TaskMicroStepEntity(
                        taskId = task.id,
                        title = suggestion.title,
                        sortOrder = index,
                        estimatedMinutes = suggestion.estimatedMinutes
                    )
                }
                taskRepository.deleteMicroStepsByTaskId(task.id)
                taskRepository.insertMicroSteps(microStepEntities)

                val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
                val serialized = StepByStepPlanCodec.serialize(planSteps)
                taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
                _userActionMessage.emit("Decomposed '${task.title.take(25)}' into ${generated.size} micro-steps 🪄")
            } catch (e: Exception) {
                _userActionMessage.emit("Breakdown completed via local heuristics.")
            } finally {
                _decomposingTaskId.value = null
            }
        }
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank()) return
        viewModelScope.launch {
            val trimmed = content.trim()
            quickNoteDao?.insert(
                QuickNoteEntity(
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            noteRepository?.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addTaskFromNote(title: String, content: String) = createTaskFromNote(title, content)

    fun createTaskFromNote(title: String, description: String = "") {
        viewModelScope.launch {
            val fullText = if (title.isNotBlank()) "$title: $description" else description
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val newTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = inferred.sanitizedTitle.ifBlank { title.ifBlank { "Quick Task" } },
                description = description.trim(),
                estimatedDurationMinutes = inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = inferred.contextTags.joinToString(","),
                scheduledDate = inferred.scheduledDate ?: uiState.value.selectedDateMillis,
                scheduledTime = inferred.scheduledTime,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            calendarSyncRepository?.handleTaskStateChanged(newTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            _userActionMessage.emit("Created task '${newTask.title.take(24)}' ⚡")
        }
    }

    suspend fun synthesizeQuickNote(content: String): QuickNoteSynthesis {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.synthesizeQuickNote(
            rawContent = content,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        ) ?: run {
            val inferred = TaskInferenceEngine.inferFromInput(content)
            QuickNoteSynthesis(
                extractedTaskTitle = inferred.sanitizedTitle.ifBlank { content.take(35) },
                suggestedCategory = inferred.taskType.name,
                estimatedMinutes = inferred.durationMinutes,
                tags = inferred.contextTags,
                firstActionStep = "Review scratchpad note and outline details"
            )
        }
    }

    fun addGoal(title: String, description: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            goalRepository?.insert(
                com.neuroflow.app.data.local.entity.GoalEntity(
                    title = title.trim(),
                    description = description.trim(),
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            _userActionMessage.emit("Goal added: ${title.take(20)}")
        }
    }

    fun completeGoal(goal: com.neuroflow.app.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            goalRepository?.update(goal.copy(isActive = false))
            _userActionMessage.emit("Goal completed! 🎯")
        }
    }

    fun deleteGoal(goal: com.neuroflow.app.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            goalRepository?.delete(goal)
            _userActionMessage.emit("Goal removed")
        }
    }

    fun shiftTaskToPeak(task: TaskEntity) {
        viewModelScope.launch {
            val peakHour = uiState.value.circadianSummary?.peakWindowStartHour
                ?: uiState.value.preferences.peakEnergyStart.takeIf { it > 0 } ?: 10
            val peakEndHour = uiState.value.circadianSummary?.peakWindowEndHour ?: (peakHour + 3)
            val dipEndHour = uiState.value.circadianSummary?.dipWindowEndHour ?: 16
            val usableEnergy = uiState.value.circadianSummary?.currentSnapshot?.usableEnergy ?: 60

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)

            val baseDate = task.scheduledDate ?: uiState.value.selectedDateMillis
            val baseCal = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isToday = baseCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                baseCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

            val targetDate: Long
            val targetHour: Int
            val messageLabel: String

            if (!isToday || currentHour < peakEndHour) {
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Peak Window (${peakHour}:00)"
            } else if (currentHour < 18 && usableEnergy >= 55) {
                targetDate = baseCal.timeInMillis
                targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                messageLabel = "Afternoon Rebound (${targetHour}:00)"
            } else {
                baseCal.add(Calendar.DAY_OF_YEAR, 1)
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Tomorrow's Peak (${peakHour}:00)"
            }

            val millisInDay = targetHour * 3600_000L
            val updated = task.copy(
                scheduledDate = targetDate,
                scheduledTime = millisInDay,
                isScheduleLocked = true,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            _userActionMessage.emit("Shifted '${task.title.take(20)}' to $messageLabel ⚡")
        }
    }

    fun assignTaskTime(task: TaskEntity, hour: Int, minute: Int) {
        viewModelScope.launch {
            val millisInDay = hour * 3600_000L + minute * 60_000L
            val updatedTask = task.copy(
                scheduledDate = uiState.value.selectedDateMillis,
                scheduledTime = millisInDay,
                isScheduleLocked = true,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updatedTask)
            val startMillis = uiState.value.selectedDateMillis + millisInDay
            calendarSyncRepository?.upsertTaskEvent(updatedTask, startMillis = startMillis)
        }
    }

    fun assignTaskTime(taskId: String, newStartMillis: Long) {
        val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == taskId } ?: return
        val zoneId = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(newStartMillis).atZone(zoneId)
        assignTaskTime(targetTask, zdt.hour, zdt.minute)
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
            calendarSyncRepository?.handleTaskStateChanged(
                task.copy(status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED)
            )
            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmationSuffix = if (prefs.affirmations.isNotEmpty()) {
                " • \"${prefs.affirmations.random()}\""
            } else ""
            _userActionMessage.emit("Completed: ${task.title}$affirmationSuffix")
        }
    }

    fun uncompleteTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
        }
    }

    fun toggleProtectedSlot(slotId: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                val updatedSlots = prefs.protectedTimeSlots.map { slot ->
                    if (slot.id == slotId) slot.copy(isEnabled = enabled) else slot
                }
                prefs.copy(protectedTimeSlots = updatedSlots)
            }
        }
    }

    fun saveProtectedSlot(slot: ProtectedTimeSlot) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                val existing = prefs.protectedTimeSlots
                val updated = if (existing.any { it.id == slot.id }) {
                    existing.map { if (it.id == slot.id) slot else it }
                } else {
                    existing + slot
                }
                prefs.copy(protectedTimeSlots = updated)
            }
        }
    }

    fun deleteProtectedSlot(slotId: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(protectedTimeSlots = prefs.protectedTimeSlots.filterNot { it.id == slotId })
            }
        }
    }

    suspend fun enrichTaskWithAi(taskTitle: String, durationMinutes: Int = 30): com.neuroflow.app.domain.service.TaskEnrichmentResult? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.enrichTaskWithAi(
            title = taskTitle,
            durationMinutes = durationMinutes,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        )
    }

    fun logPastTime(task: TaskEntity, minutes: Float) {
        if (minutes.isNaN() || minutes.isInfinite()) return
        val safeMinutes = minutes.coerceAtMost(1440f)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (safeMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = safeMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + safeMinutes
            val newCount = task.sessionCount + 1
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = safeMinutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + safeMinutes.toInt())
            }
        }
    }

    private var pendingDeleteJob: Job? = null

    fun requestDeleteTask(task: TaskEntity) {
        pendingDeleteJob?.cancel()
        _pendingDeletedTask.value = task
        pendingDeleteJob = viewModelScope.launch {
            delay(5000L)
            confirmDeleteTask()
        }
    }

    fun undoDeleteTask() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _pendingDeletedTask.value = null
    }

    fun confirmDeleteTask() {
        val task = _pendingDeletedTask.value ?: return
        _pendingDeletedTask.value = null
        viewModelScope.launch {
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            preferencesDataStore.recordDeletedTask(
                title = task.title,
                durationMinutes = task.estimatedDurationMinutes,
                category = task.taskType.name,
                postponeCount = task.postponeCount
            )
            taskRepository.delete(task)
        }
    }

    fun toggleStagingDrawer() {
        _isStagingDrawerExpanded.value = !_isStagingDrawerExpanded.value
    }

    fun toggleSearchVisible() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
```

---

