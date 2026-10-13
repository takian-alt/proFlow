package com.neuroflow.app.presentation.plan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Lock
import com.neuroflow.app.presentation.matrix.MatrixScreen
import com.neuroflow.app.presentation.schedule.ProtectedSlotBottomSheet
import com.neuroflow.app.presentation.schedule.ScheduleScreen
import com.neuroflow.app.presentation.schedule.ScheduleViewModel
import com.neuroflow.app.presentation.schedule.WeeklyScheduleScreen

enum class PlanViewMode(val title: String) {
    DAY("Day"),
    WEEK("Week"),
    MATRIX("Matrix")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onOpenMoreMenu: () -> Unit,
    onNavigateToFocus: (String) -> Unit,
    onNavigateToQuadrant: (String) -> Unit,
    onNavigateToOnHold: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    initialView: PlanViewMode = PlanViewMode.DAY,
    initialDateMillis: Long? = null,
    initialNewTaskOpen: Boolean = false,
    onInitialNewTaskConsumed: () -> Unit = {}
) {
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()
    val scheduleUiState by scheduleViewModel.uiState.collectAsStateWithLifecycle()
    val enabledSlotCount = remember(scheduleUiState.protectedTimeSlots) {
        scheduleUiState.protectedTimeSlots.count { it.isEnabled }
    }
    var showProtectedSlotsSheet by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(initialView) }
    var selectedDate by remember { mutableStateOf(initialDateMillis) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    FilterChip(
                        selected = scheduleUiState.isAutoScheduleOn,
                        onClick = { scheduleViewModel.toggleAutoSchedule() },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (scheduleUiState.isAutoScheduling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = if (scheduleUiState.isAutoScheduleOn) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Text("⚡ ")
                                }
                                Text("Auto", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (scheduleUiState.isAutoScheduleOn) {
                        IconButton(
                            onClick = { scheduleViewModel.triggerAutoScheduleNow() },
                            enabled = !scheduleUiState.isAutoScheduling
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Re-schedule Now",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showProtectedSlotsSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (enabledSlotCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(enabledSlotCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protected Time Slots",
                                tint = if (enabledSlotCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onOpenMoreMenu) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Mode Switcher
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                PlanViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PlanViewMode.entries.size
                        ),
                        onClick = { selectedMode = mode },
                        selected = selectedMode == mode,
                        icon = {
                            when (mode) {
                                PlanViewMode.DAY -> Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                PlanViewMode.WEEK -> Icon(Icons.Filled.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(16.dp))
                                PlanViewMode.MATRIX -> Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        label = {
                            Text(mode.title, fontWeight = FontWeight.SemiBold)
                        }
                    )
                }
            }

            // View Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedMode) {
                    PlanViewMode.DAY -> {
                        ScheduleScreen(
                            initialDateMillis = selectedDate,
                            onNavigateToFocus = onNavigateToFocus,
                            hideTopBar = true
                        )
                    }
                    PlanViewMode.WEEK -> {
                        WeeklyScheduleScreen(
                            onNavigateBack = { selectedMode = PlanViewMode.DAY },
                            onNavigateToDay = { date ->
                                selectedDate = date
                                selectedMode = PlanViewMode.DAY
                            },
                            onNavigateToFocus = onNavigateToFocus,
                            hideTopBar = true
                        )
                    }
                    PlanViewMode.MATRIX -> {
                        MatrixScreen(
                            onNavigateToQuadrant = onNavigateToQuadrant,
                            onNavigateToFocus = onNavigateToFocus,
                            onNavigateToOnHold = onNavigateToOnHold,
                            onNavigateToSettings = onNavigateToSettings,
                            initialNewTaskOpen = initialNewTaskOpen,
                            onInitialNewTaskConsumed = onInitialNewTaskConsumed,
                            hideTopBar = true
                        )
                    }
                }
            }
        }
    }

    if (showProtectedSlotsSheet) {
        ProtectedSlotBottomSheet(
            protectedSlots = scheduleUiState.protectedTimeSlots,
            onDismiss = { showProtectedSlotsSheet = false },
            onSaveSlot = { slot -> scheduleViewModel.addProtectedSlot(slot) },
            onDeleteSlot = { slotId -> scheduleViewModel.removeProtectedSlot(slotId) },
            onToggleSlot = { slotId -> scheduleViewModel.toggleProtectedSlot(slotId) }
        )
    }
}
