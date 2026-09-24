package com.neuroflow.app.presentation.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.TagChip
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnHoldPlanningScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFocus: (String) -> Unit = {},
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onHoldTasks = remember(uiState.tasksByQuadrant) {
        uiState.tasksByQuadrant[Quadrant.ELIMINATE]?.filter { it.status == TaskStatus.ACTIVE } ?: emptyList()
    }

    var quickIdeaText by remember { mutableStateOf("") }
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun addQuickIdea() {
        if (quickIdeaText.isNotBlank()) {
            val task = TaskEntity(
                title = quickIdeaText.trim(),
                quadrant = Quadrant.ELIMINATE,
                priority = Priority.LOW,
                impactScore = 50,
                status = TaskStatus.ACTIVE
            )
            viewModel.insertTask(task)
            quickIdeaText = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("On Hold & Planning", fontWeight = FontWeight.Bold)
                        if (onHoldTasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeuroFlowColors.EliminateText.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${onHoldTasks.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = NeuroFlowColors.EliminateText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewTaskSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Plan / Task") },
                text = { Text("Detailed Task") },
                containerColor = NeuroFlowColors.Purple,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Idea Dumping Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = "Dump Idea",
                        tint = NeuroFlowColors.EliminateText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = quickIdeaText,
                        onValueChange = { quickIdeaText = it },
                        placeholder = {
                            Text(
                                "Dump an idea or plan to consider later...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addQuickIdea() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { addQuickIdea() },
                        enabled = quickIdeaText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = "Add Idea",
                            tint = if (quickIdeaText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (onHoldTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = NeuroFlowColors.EliminateText.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No tasks or ideas on hold",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Use this space for planning, dumping thoughts, and keeping ideas you'll organize later. Move them to Do First, Schedule, or Delegate whenever you're ready.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(onHoldTasks, key = { it.id }) { task ->
                        OnHoldTaskCard(
                            task = task,
                            onComplete = { viewModel.completeTask(task) },
                            onEdit = { editingTask = task },
                            onDelete = { viewModel.deleteTask(task) },
                            onMoveTo = { targetQuadrant ->
                                viewModel.moveTask(task, targetQuadrant)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val targetName = when (targetQuadrant) {
                                        Quadrant.DO_FIRST -> "Do First"
                                        Quadrant.SCHEDULE -> "Schedule"
                                        Quadrant.DELEGATE -> "Delegate"
                                        Quadrant.ELIMINATE -> "On Hold"
                                    }
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Moved \"${task.title}\" to $targetName",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.moveTask(task, Quadrant.ELIMINATE)
                                    }
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showNewTaskSheet) {
        NewTaskSheet(
            onDismiss = { showNewTaskSheet = false },
            onSave = { task ->
                viewModel.insertTask(task.copy(quadrant = Quadrant.ELIMINATE))
                showNewTaskSheet = false
            },
            prefilledQuadrant = Quadrant.ELIMINATE,
            availableTasks = uiState.allActiveTasks
        )
    }

    editingTask?.let { task ->
        NewTaskSheet(
            onDismiss = { editingTask = null },
            onSave = { updated ->
                viewModel.updateTask(updated)
                editingTask = null
            },
            editTask = task,
            availableTasks = uiState.allActiveTasks
        )
    }
}

@Composable
fun OnHoldTaskCard(
    task: TaskEntity,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveTo: (Quadrant) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (task.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            task.tags.split(",").filter { it.isNotBlank() }.take(3).forEach { tag ->
                                TagChip(tag.trim())
                            }
                        }
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Promotion / Move Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Move to:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Move to Do First
                SuggestionChip(
                    onClick = { onMoveTo(Quadrant.DO_FIRST) },
                    label = { Text("Do First", style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = NeuroFlowColors.DoFirstText.copy(alpha = 0.12f),
                        labelColor = NeuroFlowColors.DoFirstText
                    ),
                    border = null
                )
                // Move to Schedule
                SuggestionChip(
                    onClick = { onMoveTo(Quadrant.SCHEDULE) },
                    label = { Text("Schedule", style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = NeuroFlowColors.ScheduleText.copy(alpha = 0.12f),
                        labelColor = NeuroFlowColors.ScheduleText
                    ),
                    border = null
                )
                // Move to Delegate
                SuggestionChip(
                    onClick = { onMoveTo(Quadrant.DELEGATE) },
                    label = { Text("Delegate", style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = NeuroFlowColors.DelegateText.copy(alpha = 0.12f),
                        labelColor = NeuroFlowColors.DelegateText
                    ),
                    border = null
                )
            }
        }
    }
}
