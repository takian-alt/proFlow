package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
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
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlin.math.roundToInt

@Composable
fun MindmapCanvasScreen(
    viewModel: MindmapViewModel,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Canvas transformations
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Dynamic active connecting wire state
    var wireStartNodeId by remember { mutableStateOf<String?>(null) }
    var wireCurrentDragOffset by remember { mutableStateOf<Offset?>(null) }

    // Node layout tracking for hit testing connectors
    val nodeRects = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

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

                // Render live dragging connection wire
                val startId = wireStartNodeId
                val currentDrag = wireCurrentDragOffset
                if (startId != null && currentDrag != null) {
                    val startNode = (state.goals as List<MindmapNode> + state.tasks).find { it.id == startId }
                    if (startNode != null) {
                        val liveStart = Offset(startNode.x + 120f, startNode.y + 40f)
                        val livePath = Path().apply {
                            moveTo(liveStart.x, liveStart.y)
                            val cx1 = (liveStart.x + currentDrag.x) / 2
                            val cy1 = liveStart.y
                            val cx2 = (liveStart.x + currentDrag.x) / 2
                            val cy2 = currentDrag.y
                            cubicTo(cx1, cy1, cx2, cy2, currentDrag.x, currentDrag.y)
                        }
                        drawPath(
                            path = livePath,
                            color = Color(0xFF818CF8),
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // 2. Goal Hub Nodes Layer
            state.goals.forEach { goal ->
                GoalHubNodeView(
                    goal = goal,
                    onDrag = { newPos -> viewModel.onNodeDrag(goal.id, newPos) },
                    onDragEnd = { finalPos -> viewModel.onNodeDragEnd(goal.id, "GOAL", finalPos) },
                    onStartWire = { wireStartNodeId = goal.id },
                    onUpdateWire = { wireCurrentDragOffset = it },
                    onEndWire = { releaseVirtualPos ->
                        val hit = nodeRects.entries.firstOrNull { it.value.contains(releaseVirtualPos) && it.key != goal.id }
                        if (hit != null) {
                            viewModel.onConnectNodes(goal.id, hit.key)
                        }
                        wireStartNodeId = null
                        wireCurrentDragOffset = null
                    },
                    onPositionChanged = { rect -> nodeRects[goal.id] = rect }
                )
            }

            // 3. Task Nodes Layer
            state.tasks.forEach { task ->
                TaskNodeCardView(
                    task = task,
                    onToggleComplete = { viewModel.onToggleTaskCompletion(task.id) },
                    onClick = { onTaskClick(task.id) },
                    onDrag = { newPos -> viewModel.onNodeDrag(task.id, newPos) },
                    onDragEnd = { finalPos -> viewModel.onNodeDragEnd(task.id, "TASK", finalPos) },
                    onStartWire = { wireStartNodeId = task.id },
                    onUpdateWire = { wireCurrentDragOffset = it },
                    onEndWire = { releaseVirtualPos ->
                        val hit = nodeRects.entries.firstOrNull { it.value.contains(releaseVirtualPos) && it.key != task.id }
                        if (hit != null) {
                            viewModel.onConnectNodes(task.id, hit.key)
                        }
                        wireStartNodeId = null
                        wireCurrentDragOffset = null
                    },
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

        // 5. Floating Zoom & Map Controls (Bottom Right)
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

        // 6. Connection Type Selection Modal Sheet
        val prompt = state.connectionPrompt
        if (prompt != null) {
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
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onStartWire: () -> Unit,
    onUpdateWire: (Offset) -> Unit,
    onEndWire: (Offset) -> Unit,
    onPositionChanged: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    var localOffset by remember(goal.x, goal.y) { mutableStateOf(Offset(goal.x, goal.y)) }

    Box(
        modifier = Modifier
            .offset { IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt()) }
            .onGloballyPositioned { layoutCoordinates ->
                val pos = layoutCoordinates.localToRoot(Offset.Zero)
                onPositionChanged(
                    androidx.compose.ui.geometry.Rect(
                        left = localOffset.x,
                        top = localOffset.y,
                        right = localOffset.x + layoutCoordinates.size.width,
                        bottom = localOffset.y + layoutCoordinates.size.height
                    )
                )
            }
            .pointerInput(goal.id) {
                detectDragGestures(
                    onDragEnd = { onDragEnd(localOffset) }
                ) { change, dragAmount ->
                    change.consume()
                    localOffset += dragAmount
                    onDrag(localOffset)
                }
            }
            .width(180.dp)
            .background(Color(0xFF13151B), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GOAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF818CF8)
                )
                // Connector Port
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFF6366F1), CircleShape)
                        .pointerInput(goal.id) {
                            detectDragGestures(
                                onDragStart = { onStartWire() },
                                onDragEnd = { onEndWire(localOffset) }
                            ) { change, dragAmount ->
                                change.consume()
                                onUpdateWire(localOffset + change.position)
                            }
                        }
                )
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
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onStartWire: () -> Unit,
    onUpdateWire: (Offset) -> Unit,
    onEndWire: (Offset) -> Unit,
    onPositionChanged: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    var localOffset by remember(task.x, task.y) { mutableStateOf(Offset(task.x, task.y)) }

    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color(0xFFF59E0B)
        Priority.MEDIUM -> Color(0xFF6366F1)
        Priority.LOW -> Color(0xFF10B981)
    }

    val cardBg = if (task.isCompleted) Color(0xFF16181F) else Color(0xFF13151B)
    val borderColor = if (task.isCompleted) Color(0xFF2A2E3B) else priorityColor.copy(alpha = 0.6f)
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
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragEnd = { onDragEnd(localOffset) }
                ) { change, dragAmount ->
                    change.consume()
                    localOffset += dragAmount
                    onDrag(localOffset)
                }
            }
            .width(190.dp)
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
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

            // Connector handle dot on the right
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(priorityColor, CircleShape)
                    .pointerInput(task.id) {
                        detectDragGestures(
                            onDragStart = { onStartWire() },
                            onDragEnd = { onEndWire(localOffset) }
                        ) { change, dragAmount ->
                            change.consume()
                            onUpdateWire(localOffset + change.position)
                        }
                    }
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
