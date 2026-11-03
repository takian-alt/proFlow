package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindmapCanvasScreen(
    viewModel: MindmapViewModel,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Canvas transformations
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Node layout tracking for hit testing connectors
    val nodeRects = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    // Action Sheet & Composer states
    var selectedTaskForAction by remember { mutableStateOf<MindmapTaskNode?>(null) }
    var showComposerForCreate by remember { mutableStateOf(false) }
    var editingTaskEntity by remember { mutableStateOf<TaskEntity?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)

        // The interactive infinite canvas gesture container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = zoomScale
                        val newScale = (zoomScale * zoom).coerceIn(0.35f, 2.5f)
                        zoomScale = newScale
                        panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + pan
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
            // 1. Connection Wires Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background subtle ambient grid
                val gridSize = 60f
                val width = size.width.coerceAtLeast(4000f)
                val height = size.height.coerceAtLeast(4000f)
                var gx = -2000f
                while (gx < width) {
                    var gy = -2000f
                    while (gy < height) {
                        drawCircle(
                            color = Color(0xFF1E2028),
                            radius = 1.5f,
                            center = Offset(gx, gy)
                        )
                        gy += gridSize
                    }
                    gx += gridSize
                }

                // Render established edges
                state.edges.forEach { edge ->
                    val wireColor = when {
                        edge.isCompleted -> Color(0xFF333333)
                        edge.edgeType == MindmapEdgeType.GOAL_LINK -> Color(0xFF6366F1).copy(alpha = 0.85f)
                        edge.edgeType == MindmapEdgeType.DEPENDENCY -> Color(0xFFF59E0B).copy(alpha = 0.85f)
                        else -> Color(0xFF10B981).copy(alpha = 0.7f)
                    }

                    val path = Path().apply {
                        moveTo(edge.start.x + 90f, edge.start.y + 40f)
                        val control1 = Offset(edge.start.x + 160f, edge.start.y + 40f)
                        val control2 = Offset(edge.end.x - 70f, edge.end.y + 40f)
                        cubicTo(control1.x, control1.y, control2.x, control2.y, edge.end.x, edge.end.y + 40f)
                    }

                    drawPath(
                        path = path,
                        color = wireColor,
                        style = Stroke(
                            width = if (edge.edgeType == MindmapEdgeType.DEPENDENCY) 3.5f else 2.5f,
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Draw established connection wires (Bézier curves)
                state.edges.forEach { edge ->
                    val wireColor = when (edge.edgeType) {
                        MindmapEdgeType.GOAL_LINK -> Color(0xFF6366F1) // Indigo
                        MindmapEdgeType.DEPENDENCY -> Color(0xFFF59E0B) // Amber
                        MindmapEdgeType.VISUAL -> Color(0xFF38BDF8) // Cyan
                    }

                    val finalColor = if (edge.isCompleted) Color(0xFF334155).copy(alpha = 0.5f) else wireColor.copy(alpha = 0.85f)
                    val p1 = edge.start
                    val p2 = edge.end

                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        val cx1 = (p1.x + p2.x) / 2
                        val cy1 = p1.y
                        val cx2 = (p1.x + p2.x) / 2
                        val cy2 = p2.y
                        cubicTo(cx1, cy1, cx2, cy2, p2.x, p2.y)
                    }

                    drawPath(
                        path = path,
                        color = finalColor,
                        style = Stroke(width = if (edge.edgeType == MindmapEdgeType.DEPENDENCY) 3.5f else 2.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Goal Hub Nodes Layer
            state.goals.forEach { goal ->
                GoalHubNodeView(
                    goal = goal,
                    isConnectingSource = state.connectingSourceNodeId == goal.id,
                    onDrag = { newPos -> viewModel.onNodeDrag(goal.id, newPos) },
                    onDragEnd = { finalPos -> viewModel.onNodeDragEnd(goal.id, "GOAL", finalPos) },
                    onClick = { viewModel.handleNodeClick(goal.id) { /* Goal tapped */ } },
                    onToggleConnect = { viewModel.toggleConnectMode(goal.id) },
                    onPositionChanged = { rect -> nodeRects[goal.id] = rect }
                )
            }

            // 3. Task Nodes Layer
            state.tasks.forEach { task ->
                TaskNodeCardView(
                    task = task,
                    isConnectingSource = state.connectingSourceNodeId == task.id,
                    onToggleComplete = { viewModel.onToggleTaskCompletion(task.id) },
                    onClick = { viewModel.handleNodeClick(task.id) { selectedTaskForAction = task } },
                    onDrag = { newPos -> viewModel.onNodeDrag(task.id, newPos) },
                    onDragEnd = { finalPos -> viewModel.onNodeDragEnd(task.id, "TASK", finalPos) },
                    onToggleConnect = { viewModel.toggleConnectMode(task.id) },
                    onPositionChanged = { rect -> nodeRects[task.id] = rect }
                )
            }
        }
    }

        // 4. Immersive Floating Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF141720).copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mind Map Planner",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle Completed Tasks
                IconButton(
                    onClick = { viewModel.toggleShowCompleted() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (state.showCompleted) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFF141720).copy(alpha = 0.85f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (state.showCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Completed",
                        tint = if (state.showCompleted) Color(0xFF818CF8) else Color(0xFF94A3B8)
                    )
                }

                // Recenter & Reset Zoom
                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF141720).copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Recenter Map",
                        tint = Color.White
                    )
                }
            }
        }

        // 5. Connect Mode Banner (3B: Two-step connect state)
        AnimatedVisibility(
            visible = state.connectingSourceNodeId != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1B4B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF818CF8)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(18.dp)
                    )
                    val sourceNode = (state.goals as List<MindmapNode> + state.tasks).find { it.id == state.connectingSourceNodeId }
                    Text(
                        text = "Tap target node to connect with '${sourceNode?.title?.take(18) ?: "Node"}'",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { viewModel.cancelConnectMode() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 6. Floating "Add Task" Action Button (1A: Bottom Left)
        FloatingActionButton(
            onClick = { showComposerForCreate = true },
            containerColor = Color(0xFF6366F1),
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(20.dp))
                Text(text = "New Task", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        // 7. Floating Zoom & Map Controls (Bottom Right)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141720).copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262A36)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Zoom Out
                IconButton(
                    onClick = {
                        val oldScale = zoomScale
                        val newScale = (zoomScale / 1.25f).coerceAtLeast(0.35f)
                        panOffset = screenCenter - (screenCenter - panOffset) * (newScale / oldScale)
                        zoomScale = newScale
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Level Badge (Tap to reset to 100%)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val oldScale = zoomScale
                            val newScale = 1f
                            panOffset = screenCenter - (screenCenter - panOffset) * (newScale / oldScale)
                            zoomScale = newScale
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(zoomScale * 100).roundToInt()}%",
                        color = Color(0xFF818CF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Zoom In
                IconButton(
                    onClick = {
                        val oldScale = zoomScale
                        val newScale = (zoomScale * 1.25f).coerceAtMost(2.5f)
                        panOffset = screenCenter - (screenCenter - panOffset) * (newScale / oldScale)
                        zoomScale = newScale
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 8. Task Context Action Bottom Sheet (2B)
        val taskForAction = selectedTaskForAction
        if (taskForAction != null) {
            TaskActionBottomSheet(
                task = taskForAction,
                onDismiss = { selectedTaskForAction = null },
                onEdit = {
                    val taskId = taskForAction.id
                    selectedTaskForAction = null
                    coroutineScope.launch {
                        val entity = viewModel.getTaskEntity(taskId)
                        if (entity != null) {
                            editingTaskEntity = entity
                        }
                    }
                },
                onStartFocus = {
                    val taskId = taskForAction.id
                    selectedTaskForAction = null
                    onTaskClick(taskId)
                },
                onDisconnect = {
                    viewModel.disconnectNode(taskForAction.id)
                    selectedTaskForAction = null
                },
                onDelete = {
                    viewModel.deleteTask(taskForAction.id)
                    selectedTaskForAction = null
                }
            )
        }

        // 9. Smart Task Composer (Create Mode)
        if (showComposerForCreate) {
            SmartTaskComposerSheetV2(
                onDismiss = { showComposerForCreate = false },
                mode = SheetMode.Create,
                onSave = { newTask ->
                    showComposerForCreate = false
                    // Spawn at the center of the current canvas viewport
                    val spawnX = -panOffset.x / zoomScale + (screenWidth / 2f) / zoomScale - 110f
                    val spawnY = -panOffset.y / zoomScale + (screenHeight / 2f) / zoomScale - 35f
                    viewModel.addTask(newTask, Offset(spawnX, spawnY))
                }
            )
        }

        // 10. Smart Task Composer (Edit Mode)
        editingTaskEntity?.let { entity ->
            SmartTaskComposerSheetV2(
                onDismiss = { editingTaskEntity = null },
                mode = SheetMode.Edit(entity),
                onSave = { updated ->
                    editingTaskEntity = null
                    viewModel.updateTask(updated)
                }
            )
        }

        // 11. Connection Type Selection Modal Sheet
        state.connectionPrompt?.let { prompt ->
            ConnectionPromptDialog(
                source = prompt.first,
                target = prompt.second,
                onConfirm = { type -> viewModel.confirmConnection(prompt.first, prompt.second, type) },
                onDismiss = { viewModel.dismissConnectionPrompt() }
            )
        }
    }
}

@Composable
private fun GoalHubNodeView(
    goal: MindmapGoalNode,
    isConnectingSource: Boolean,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onClick: () -> Unit,
    onToggleConnect: () -> Unit,
    onPositionChanged: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    var localOffset by remember(goal.x, goal.y) { mutableStateOf(Offset(goal.x, goal.y)) }

    val borderColor = if (isConnectingSource) Color(0xFF818CF8) else Color(0xFF6366F1)
    val borderWidth = if (isConnectingSource) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt()) }
            .onGloballyPositioned { layoutCoordinates ->
                onPositionChanged(
                    androidx.compose.ui.geometry.Rect(
                        left = localOffset.x,
                        top = localOffset.y,
                        right = localOffset.x + layoutCoordinates.size.width,
                        bottom = localOffset.y + layoutCoordinates.size.height
                    )
                )
            }
            .width(180.dp)
            .background(Color(0xFF13151B), RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dedicated Drag Handle
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag Goal",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(20.dp)
                        .pointerInput(goal.id) {
                            detectDragGestures(
                                onDragEnd = { onDragEnd(localOffset) }
                            ) {
 change, dragAmount ->
                                change.consume()
                                localOffset += dragAmount
                                onDrag(localOffset)
                            }
                        }
                )

                Text(
                    text = "GOAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF818CF8)
                )

                // 3B Tap-to-Connect Dot
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isConnectingSource) Color(0xFF818CF8) else Color(0xFF6366F1))
                        .clickable { onToggleConnect() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF222634)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${goal.completedTasks}/${goal.totalTasks} Done",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun TaskNodeCardView(
    task: MindmapTaskNode,
    isConnectingSource: Boolean,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onToggleConnect: () -> Unit,
    onPositionChanged: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    var localOffset by remember(task.x, task.y) { mutableStateOf(Offset(task.x, task.y)) }

    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color(0xFFF59E0B)
        Priority.MEDIUM -> Color(0xFF6366F1)
        Priority.LOW -> Color(0xFF10B981)
    }

    val cardBg = if (task.isCompleted) Color(0xFF16181F) else Color(0xFF13151B)
    val borderColor = if (isConnectingSource) {
        Color(0xFF818CF8)
    } else if (task.isCompleted) {
        Color(0xFF2A2E3B)
    } else {
        priorityColor.copy(alpha = 0.6f)
    }
    val borderWidth = if (isConnectingSource) 2.5.dp else 1.5.dp
    val textColor = if (task.isCompleted) Color(0xFF64748B) else Color(0xFFF8FAFC)

    Box(
        modifier = Modifier
            .offset { IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt()) }
            .onGloballyPositioned { layoutCoordinates ->
                onPositionChanged(
                    androidx.compose.ui.geometry.Rect(
                        left = localOffset.x,
                        top = localOffset.y,
                        right = localOffset.x + layoutCoordinates.size.width,
                        bottom = localOffset.y + layoutCoordinates.size.height
                    )
                )
            }
            .width(200.dp)
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Checkbox for task completion (turns node gray)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (task.isCompleted) Color(0xFF10B981) else Color(0xFF222634))
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title
            Text(
                text = task.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Dedicated Drag Handle
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag Handle",
                tint = Color(0xFF64748B),
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput(task.id) {
                        detectDragGestures(
                            onDragEnd = { onDragEnd(localOffset) }
                        ) { change, dragAmount ->
                            change.consume()
                            localOffset += dragAmount
                            onDrag(localOffset)
                        }
                    }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 3B Tap-to-Connect Dot
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isConnectingSource) Color(0xFF818CF8) else priorityColor)
                    .clickable { onToggleConnect() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskActionBottomSheet(
    task: MindmapTaskNode,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartFocus: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141720),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = task.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Edit Task
            ListItem(
                headlineContent = { Text("Edit Task Details", color = Color.White) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onEdit() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Start Focus Session
            ListItem(
                headlineContent = { Text("Start Focus Session", color = Color.White) },
                leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981)) },
                modifier = Modifier.clickable { onStartFocus() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Disconnect All Wires
            ListItem(
                headlineContent = { Text("Disconnect Connections", color = Color(0xFFF59E0B)) },
                leadingContent = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFFF59E0B)) },
                modifier = Modifier.clickable { onDisconnect() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Delete Task
            ListItem(
                headlineContent = { Text("Delete Task", color = Color(0xFFEF4444)) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                modifier = Modifier.clickable { onDelete() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun ConnectionPromptDialog(
    source: MindmapNode,
    target: MindmapNode,
    onConfirm: (MindmapEdgeType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13151B),
        title = {
            Text(
                text = "Connect Tasks",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Connecting '${source.title}' to '${target.title}'. Select relationship type:",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onConfirm(MindmapEdgeType.DEPENDENCY) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Prerequisite (Dependency)", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onConfirm(MindmapEdgeType.VISUAL) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Visual Mind Map Wire Only", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
