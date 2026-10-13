package com.neuroflow.app.feature.hyperfocus.screens

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.home.renderableEssentials
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperFocusActivationSheet(
    viewModel: HyperFocusViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var accessibilityEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        accessibilityEnabled = com.neuroflow.app.feature.hyperfocus.util.AccessibilityUtil
            .isAppBlockingServiceEnabled(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (!accessibilityEnabled) {
            PermissionSetupScreen(onBothGranted = { accessibilityEnabled = true })
        } else {
            ActivationSheetContent(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ActivationSheetContent(
    viewModel: HyperFocusViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTasks by viewModel.activeTasks.collectAsState()
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val activationMessage by viewModel.activationMessage.collectAsState()
    val hasActiveTasks = activeTasks.isNotEmpty()

    val selectedTaskIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(activeTasks) {
        val activeIds = activeTasks.map { it.id }.toSet()
        val stale = selectedTaskIds.keys.filter { it !in activeIds }
        stale.forEach { selectedTaskIds.remove(it) }
        activeTasks.forEach { task ->
            if (selectedTaskIds[task.id] == null) {
                selectedTaskIds[task.id] = false
            }
        }
    }

    var taskSearchQuery by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    var sessionMode by remember { mutableStateOf(HyperFocusSessionMode.TASK_BASED) }
    val durationOptions = remember { listOf(15, 25, 45, 60, 90, 120) }
    var selectedDurationMinutes by remember { mutableStateOf(25) }

    val filteredTasks = remember(taskSearchQuery, activeTasks) {
        if (taskSearchQuery.isBlank()) activeTasks
        else activeTasks.filter { task ->
            task.title.contains(taskSearchQuery, ignoreCase = true) ||
                task.description.contains(taskSearchQuery, ignoreCase = true)
        }
    }

    val haptic = LocalHapticFeedback.current
    val selectedTaskCount = selectedTaskIds.count { it.value }
    // Counted the way the home renders it, so this number and the grid that appears after activation
    // cannot disagree — and so the button cannot enable on a tier of nothing but Settings.
    val essentialCount = renderableEssentials(prefs, SystemExempt.STATIC, context.packageName).size
    val allowedCount = prefs.allowedPackages.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Activate Hyper Focus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Warning banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                    "⚠️ Selected apps will be blocked for your chosen duration."
                } else {
                    "⚠️ Selected apps will be blocked until you complete your daily tasks."
                },
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(14.dp)
            )
        }

        Text(
            text = "Focus mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sessionMode == HyperFocusSessionMode.TASK_BASED,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sessionMode = HyperFocusSessionMode.TASK_BASED
                },
                shape = RoundedCornerShape(12.dp),
                label = { Text("Task-based") }
            )
            FilterChip(
                selected = sessionMode == HyperFocusSessionMode.TIME_BASED,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sessionMode = HyperFocusSessionMode.TIME_BASED
                },
                shape = RoundedCornerShape(12.dp),
                label = { Text("Time-based") }
            )
        }

        if (sessionMode == HyperFocusSessionMode.TASK_BASED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Select tasks for this session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Daily task target: $selectedTaskCount task(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Block duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.forEach { minutes ->
                    FilterChip(
                        selected = selectedDurationMinutes == minutes,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDurationMinutes = minutes
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("${minutes}m") }
                    )
                }
            }
        }

        if (sessionMode == HyperFocusSessionMode.TASK_BASED) {
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks in Hyper Focus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$selectedTaskCount selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = taskSearchQuery,
                onValueChange = { taskSearchQuery = it },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredTasks.isEmpty()) {
                Text(
                    text = "No active tasks found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredTasks.forEach { task ->
                    val isChecked = selectedTaskIds[task.id] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTaskIds[task.id] = !isChecked
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTaskIds[task.id] = checked
                            }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // Confirmation field
        OutlinedTextField(
            value = confirmText,
            onValueChange = { confirmText = it },
            label = { Text("Type FOCUS to confirm") },
            supportingText = { Text("This action is hard to reverse during a session.") },
            singleLine = true,
            isError = confirmText.isNotEmpty() && confirmText != "FOCUS",
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // The tiers are global (B2): this sheet confirms a session, it no longer chooses apps.
        Text(
            text = if (essentialCount == 0) {
                "No essential apps pinned. Pin at least one app as essential in the allowlist editor " +
                    "before starting — a session whose home has no icons has nowhere to hand you back to."
            } else {
                "Blocking everything except $essentialCount essential apps and the " +
                    "${prefs.allowedPackages.size} you allowed behind the gate. Hidden apps need a pass."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (essentialCount == 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                    viewModel.activateTimed(selectedDurationMinutes)
                } else {
                    val selectedTasks = selectedTaskIds.filter { it.value }.keys.toSet()
                    viewModel.activate(selectedTasks)
                }
                onDismiss()
            },
            enabled = confirmText == "FOCUS" && essentialCount > 0 &&
                (sessionMode == HyperFocusSessionMode.TIME_BASED || (hasActiveTasks && selectedTaskCount > 0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            val label = if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                "Activate ${selectedDurationMinutes}m Focus Lock"
            } else {
                "Activate Hyper Focus 🔒"
            }
            Text(label, fontWeight = FontWeight.Bold)
        }


        // A rejection is a fact about the screen the user is standing on, so it renders here rather
        // than as a toast the sheet has already dismissed.
        activationMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // No tasks warning
        if (sessionMode == HyperFocusSessionMode.TASK_BASED && !hasActiveTasks) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "⚠️ You need at least one active task before activating Hyper Focus.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
