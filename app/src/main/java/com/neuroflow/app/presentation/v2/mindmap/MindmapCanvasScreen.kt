package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.ConnectionType
import com.neuroflow.app.data.repository.NodeConnection
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindmapCanvasScreen(
    viewModel: MindmapViewModel,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Canvas transformations
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Search bar visibility
    var isSearchExpanded by remember { mutableStateOf(false) }
    var currentSearchIndex by remember { mutableIntStateOf(0) }

    // Action Sheet & Composer states
    var selectedTaskForAction by remember { mutableStateOf<MindmapTaskNode?>(null) }
    var selectedGoalForAction by remember { mutableStateOf<MindmapGoalNode?>(null) }
    var showComposerForCreate by remember { mutableStateOf(false) }
    var editingTaskEntity by remember { mutableStateOf<TaskEntity?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090D))
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)

        // Helper function: Smoothly animate pan and zoom to target coordinates
        fun animateViewportTo(targetPan: Offset, targetZoom: Float = zoomScale) {
            coroutineScope.launch {
                val startPan = panOffset
                val startZoom = zoomScale
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) { progress, _ ->
                    panOffset = Offset(
                        startPan.x + (targetPan.x - startPan.x) * progress,
                        startPan.y + (targetPan.y - startPan.y) * progress
                    )
                    zoomScale = startZoom + (targetZoom - startZoom) * progress
                }
            }
        }

        // Helper function: Fit all visible nodes into view
        fun fitNodesToScreen() {
            val allVisible = state.goals + state.tasks
            if (allVisible.isEmpty()) {
                animateViewportTo(Offset.Zero, 1f)
                return
            }

            val minX = allVisible.minOf { it.x }
            val minY = allVisible.minOf { it.y }
            val maxX = allVisible.maxOf { it.x + it.width }
            val maxY = allVisible.maxOf { it.y + it.height }

            val contentWidthPx = (maxX - minX) * density
            val contentHeightPx = (maxY - minY) * density
            val contentCenterPx = Offset(((minX + maxX) / 2f) * density, ((minY + maxY) / 2f) * density)

            val paddingPx = 54f * density
            val availWidth = (screenWidth - paddingPx * 2).coerceAtLeast(100f)
            val availHeight = (screenHeight - paddingPx * 2 - 120f * density).coerceAtLeast(100f)

            val targetScale = minOf(availWidth / contentWidthPx, availHeight / contentHeightPx).coerceIn(0.28f, 1.35f)
            val targetPan = screenCenter - contentCenterPx * targetScale

            animateViewportTo(targetPan, targetScale)
        }

        // Helper function: Center on a specific node
        fun centerOnNode(node: MindmapNode) {
            val nodeCenterPx = Offset((node.x + node.width / 2f) * density, (node.y + node.height / 2f) * density)
            val targetPan = screenCenter - nodeCenterPx * zoomScale
            animateViewportTo(targetPan, zoomScale)
        }

        // 1. Ultra-Fast Infinite Ambient Grid Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseGridPx = 32f * density
            val scaledGrid = baseGridPx * zoomScale
            if (scaledGrid > 14f) {
                val startX = (panOffset.x % scaledGrid + scaledGrid) % scaledGrid
                val startY = (panOffset.y % scaledGrid + scaledGrid) % scaledGrid
                var gx = startX
                while (gx < size.width) {
                    var gy = startY
                    while (gy < size.height) {
                        drawCircle(
                            color = Color(0xFF1E2230),
                            radius = 1.35f,
                            center = Offset(gx, gy)
                        )
                        gy += scaledGrid
                    }
                    gx += scaledGrid
                }
            }
        }

        // 2. The Interactive Infinite Canvas Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = zoomScale
                        val newScale = (zoomScale * zoom).coerceIn(0.25f, 2.8f)
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
                // Connection Wires Layer (Organic SimpleMind-Style Bézier Splines)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nodeMap = (state.goals as List<MindmapNode> + state.tasks).associateBy { it.id }

                    state.edges.forEach { edge ->
                        val src = nodeMap[edge.sourceId] ?: return@forEach
                        val tgt = nodeMap[edge.targetId] ?: return@forEach

                        val isSubtask = edge.edgeType == MindmapEdgeType.SUBTASK_LINK
                        val isGoal = edge.edgeType == MindmapEdgeType.GOAL_LINK
                        val isDep = edge.edgeType == MindmapEdgeType.DEPENDENCY

                        val wireColor = when (edge.edgeType) {
                            MindmapEdgeType.GOAL_LINK -> Color(0xFF6366F1) // Indigo
                            MindmapEdgeType.SUBTASK_LINK -> Color(0xFF818CF8) // Soft Indigo
                            MindmapEdgeType.DEPENDENCY -> Color(0xFFF59E0B) // Amber
                            MindmapEdgeType.VISUAL -> Color(0xFF38BDF8) // Cyan
                        }

                        val strokeWidth = when {
                            isGoal -> 3.2f
                            isSubtask -> 2.6f
                            isDep -> 2.8f
                            else -> 2.0f
                        }

                        val finalColor = if (edge.isCompleted) {
                            Color(0xFF334155).copy(alpha = 0.45f)
                        } else {
                            wireColor.copy(alpha = if (isSubtask) 0.88f else 0.95f)
                        }

                        val pathEffect = if (isDep) {
                            PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                        } else null

                        // Compute clean attachment points
                        val srcCenterPx = Offset((src.x + src.width / 2f) * density, (src.y + src.height / 2f) * density)
                        val tgtCenterPx = Offset((tgt.x + tgt.width / 2f) * density, (tgt.y + tgt.height / 2f) * density)

                        val isTreeLink = edge.edgeType == MindmapEdgeType.GOAL_LINK || edge.edgeType == MindmapEdgeType.SUBTASK_LINK
                        val p1: Offset
                        val p2: Offset

                        if (state.layoutMode == MindmapLayoutMode.HORIZONTAL && isTreeLink) {
                            if (tgt.x >= src.x + src.width * 0.7f) {
                                // Target is cleanly to the right
                                p1 = Offset((src.x + src.width) * density, (src.y + src.height / 2f) * density)
                                p2 = Offset(tgt.x * density, (tgt.y + tgt.height / 2f) * density)
                            } else if (tgt.x + tgt.width <= src.x + src.width * 0.3f) {
                                // Target is cleanly to the left
                                p1 = Offset(src.x * density, (src.y + src.height / 2f) * density)
                                p2 = Offset((tgt.x + tgt.width) * density, (tgt.y + tgt.height / 2f) * density)
                            } else {
                                // Vertical or overlapping: attach via border intersection
                                p1 = calculateBorderIntersection(src, tgtCenterPx, density)
                                p2 = calculateBorderIntersection(tgt, srcCenterPx, density)
                            }
                        } else {
                            // Radial mode OR cross-branch dependencies / visual edges: border-to-border attachment
                            p1 = calculateBorderIntersection(src, tgtCenterPx, density)
                            p2 = calculateBorderIntersection(tgt, srcCenterPx, density)
                        }

                        // Organic Bézier Spline
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val cx1: Float
                        val cy1: Float
                        val cx2: Float
                        val cy2: Float

                        if (state.layoutMode == MindmapLayoutMode.HORIZONTAL && isTreeLink && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            val curvature = maxOf(28f * density, kotlin.math.abs(dx) * 0.45f)
                            cx1 = if (dx >= 0) p1.x + curvature else p1.x - curvature
                            cy1 = p1.y
                            cx2 = if (dx >= 0) p2.x - curvature else p2.x + curvature
                            cy2 = p2.y
                        } else {
                            cx1 = p1.x + dx * 0.35f
                            cy1 = p1.y
                            cx2 = p2.x - dx * 0.35f
                            cy2 = p2.y
                        }

                        val path = Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(cx1, cy1, cx2, cy2, p2.x, p2.y)
                        }

                        drawPath(
                            path = path,
                            color = finalColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = pathEffect
                            )
                        )

                        // Subtle directional flow indicator / arrow on connector wire
                        val tMid = 0.55f
                        val oneMinusT = 1f - tMid

                        val mx = oneMinusT * oneMinusT * oneMinusT * p1.x +
                            3f * oneMinusT * oneMinusT * tMid * cx1 +
                            3f * oneMinusT * tMid * tMid * cx2 +
                            tMid * tMid * tMid * p2.x
                        val my = oneMinusT * oneMinusT * oneMinusT * p1.y +
                            3f * oneMinusT * oneMinusT * tMid * cy1 +
                            3f * oneMinusT * tMid * tMid * cy2 +
                            tMid * tMid * tMid * p2.y
                        val mid = Offset(mx, my)

                        val dax = 3f * oneMinusT * oneMinusT * (cx1 - p1.x)
                        val dbx = 6f * oneMinusT * tMid * (cx2 - cx1)
                        val dcx = 3f * tMid * tMid * (p2.x - cx2)
                        val tanX = dax + dbx + dcx

                        val day = 3f * oneMinusT * oneMinusT * (cy1 - p1.y)
                        val dby = 6f * oneMinusT * tMid * (cy2 - cy1)
                        val dcy = 3f * tMid * tMid * (p2.y - cy2)
                        val tanY = day + dby + dcy

                        val angle = atan2(tanY.toDouble(), tanX.toDouble()).toFloat()
                        val arrowLen = 7.5f
                        val wingAngle = 0.55f

                        val w1 = Offset(
                            mid.x - (arrowLen * cos((angle - wingAngle).toDouble())).toFloat(),
                            mid.y - (arrowLen * sin((angle - wingAngle).toDouble())).toFloat()
                        )
                        val w2 = Offset(
                            mid.x - (arrowLen * cos((angle + wingAngle).toDouble())).toFloat(),
                            mid.y - (arrowLen * sin((angle + wingAngle).toDouble())).toFloat()
                        )

                        val arrowPath = Path().apply {
                            moveTo(w1.x, w1.y)
                            lineTo(mid.x, mid.y)
                            lineTo(w2.x, w2.y)
                        }
                        drawPath(
                            path = arrowPath,
                            color = finalColor,
                            style = Stroke(
                                width = strokeWidth * 0.95f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Terminal joint dots
                        drawCircle(color = finalColor, radius = strokeWidth * 1.25f, center = p1)
                        drawCircle(color = finalColor, radius = strokeWidth * 1.25f, center = p2)
                    }
                }

                // Goal Hub Nodes Layer
                state.goals.forEach { goal ->
                    GoalHubNodeView(
                        goal = goal,
                        density = density,
                        zoomScale = zoomScale,
                        isConnectingSource = state.connectingSourceNodeId == goal.id,
                        isHighlighted = goal.isHighlighted,
                        onDrag = { newPos -> viewModel.onNodeDrag(goal.id, newPos) },
                        onDragEnd = { finalPos -> viewModel.onNodeDragEnd(goal.id, "GOAL", finalPos) },
                        onClick = {
                            viewModel.handleNodeClick(goal.id) {
                                selectedGoalForAction = goal
                            }
                        },
                        onToggleConnect = { viewModel.toggleConnectMode(goal.id) },
                        onToggleCollapse = { viewModel.toggleCollapse(goal.id) },
                        onSproutChild = { viewModel.openInlineSprout(goal, SproutMode.CHILD) }
                    )
                }

                // Task Nodes Layer
                state.tasks.forEach { task ->
                    TaskNodeCardView(
                        task = task,
                        density = density,
                        zoomScale = zoomScale,
                        isActiveStartingNode = state.activeStartingNodeId == task.id,
                        isConnectingSource = state.connectingSourceNodeId == task.id,
                        isHighlighted = task.isHighlighted,
                        onToggleComplete = { viewModel.onToggleTaskCompletion(task.id) },
                        onClick = {
                            viewModel.handleNodeClick(task.id) {
                                selectedTaskForAction = task
                            }
                        },
                        onDrag = { newPos -> viewModel.onNodeDrag(task.id, newPos) },
                        onDragEnd = { finalPos -> viewModel.onNodeDragEnd(task.id, "TASK", finalPos) },
                        onToggleConnect = { viewModel.toggleConnectMode(task.id) },
                        onToggleCollapse = { viewModel.toggleCollapse(task.id) },
                        onSproutChild = { viewModel.openInlineSprout(task, SproutMode.CHILD) }
                    )
                }
            }
        }

        // 3. Immersive Floating Top Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF141720).copy(alpha = 0.92f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mind Map Planner",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.layoutMode == MindmapLayoutMode.HORIZONTAL) "Tree Layout (Bi-directional)" else "Radial Map",
                            color = Color(0xFF818CF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Search Button
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) {
                                viewModel.setSearchQuery("")
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSearchExpanded || state.searchQuery.isNotBlank()) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF141720).copy(alpha = 0.92f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Nodes",
                            tint = if (isSearchExpanded || state.searchQuery.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }

                    // Auto-Layout / Format Map Button
                    IconButton(
                        onClick = {
                            viewModel.applyAutoLayout()
                            fitNodesToScreen()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF141720).copy(alpha = 0.92f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Auto Layout",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // Switch Layout Mode (Horizontal Tree <-> Radial Map)
                    IconButton(
                        onClick = { viewModel.toggleLayoutMode() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF141720).copy(alpha = 0.92f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Switch Layout Mode",
                            tint = Color(0xFFA78BFA)
                        )
                    }

                    // Toggle Completed Tasks
                    IconButton(
                        onClick = { viewModel.toggleShowCompleted() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (state.showCompleted) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFF141720).copy(alpha = 0.92f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (state.showCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Completed",
                            tint = if (state.showCompleted) Color(0xFF818CF8) else Color(0xFF94A3B8)
                        )
                    }

                    // Fit to Screen Button
                    IconButton(
                        onClick = { fitNodesToScreen() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF141720).copy(alpha = 0.92f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = "Fit to Screen",
                            tint = Color.White
                        )
                    }
                }
            }

            // Expandable Search & Match Cycling Bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141720).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262A36)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search nodes, tags...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (state.highlightedNodeIds.isNotEmpty()) {
                            val matches = (state.goals as List<MindmapNode> + state.tasks).filter { it.id in state.highlightedNodeIds }
                            Text(
                                text = "${matches.size} found",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (matches.isNotEmpty()) {
                                        currentSearchIndex = (currentSearchIndex + 1) % matches.size
                                        centerOnNode(matches[currentSearchIndex])
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Match", tint = Color(0xFF38BDF8))
                            }
                        }
                    }
                }
            }
        }

        // 4. Drag-to-Reparent Dynamic Feedback Banner
        AnimatedVisibility(
            visible = state.dropTargetNodeId != null && state.draggingNodeId != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isSearchExpanded) 115.dp else 65.dp)
        ) {
            val targetNode = (state.goals as List<MindmapNode> + state.tasks).find { it.id == state.dropTargetNodeId }
            val draggedNode = (state.goals as List<MindmapNode> + state.tasks).find { it.id == state.draggingNodeId }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF064E3B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Release to reparent '${draggedNode?.title?.take(16)}' under '${targetNode?.title?.take(16)}'",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Connect Mode Banner
        AnimatedVisibility(
            visible = state.connectingSourceNodeId != null && state.dropTargetNodeId == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isSearchExpanded) 115.dp else 65.dp)
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

        // 6. Dual Stacked Floating Action Buttons: New Goal & New Root Task (Bottom Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // New Goal FAB (Emerald)
            FloatingActionButton(
                onClick = { viewModel.openGoalCreateDialog() },
                containerColor = Color(0xFF059669),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Flag, contentDescription = "New Goal", modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = "+ New Goal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // New Root Task FAB (Indigo)
            FloatingActionButton(
                onClick = { showComposerForCreate = true },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(18.dp))
                    Text(text = "+ Root Task", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 7. Floating "Target Active Step" Button (Bottom Center)
        if (state.activeStartingNodeId != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val activeNode = state.tasks.find { it.id == state.activeStartingNodeId }
                        if (activeNode != null) {
                            centerOnNode(activeNode)
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF064E3B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Target Active Step",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Target Active Step",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 8. Floating Zoom Controls (Bottom Right)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141720).copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262A36)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        val oldScale = zoomScale
                        val newScale = (zoomScale / 1.25f).coerceAtLeast(0.25f)
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

                IconButton(
                    onClick = {
                        val oldScale = zoomScale
                        val newScale = (zoomScale * 1.25f).coerceAtMost(2.8f)
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

        // 9. Inline Branch Sprouting Modal Dialog (Subtask or Sibling)
        state.inlineSproutParentNode?.let { node ->
            InlineSproutDialog(
                referenceNode = node,
                mode = state.inlineSproutMode,
                onDismiss = { viewModel.dismissInlineSprout() },
                onConfirm = { title, priority, createAnother ->
                    if (state.inlineSproutMode == SproutMode.CHILD) {
                        viewModel.spawnChildTask(node.id, title, priority)
                        if (createAnother) {
                            viewModel.openInlineSprout(node, SproutMode.CHILD)
                        }
                    } else {
                        viewModel.spawnSiblingTask(node.id, title, priority)
                        if (createAnother) {
                            viewModel.openInlineSprout(node, SproutMode.SIBLING)
                        }
                    }
                }
            )
        }

        // 10. Task Context Action Bottom Sheet
        val taskForAction = selectedTaskForAction
        if (taskForAction != null) {
            TaskActionBottomSheet(
                task = taskForAction,
                onDismiss = { selectedTaskForAction = null },
                onAddSubtask = {
                    selectedTaskForAction = null
                    viewModel.openInlineSprout(taskForAction, SproutMode.CHILD)
                },
                onAddSibling = {
                    selectedTaskForAction = null
                    viewModel.openInlineSprout(taskForAction, SproutMode.SIBLING)
                },
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
                onToggleConnect = {
                    viewModel.toggleConnectMode(taskForAction.id)
                    selectedTaskForAction = null
                },
                onDisconnect = {
                    val task = taskForAction
                    selectedTaskForAction = null
                    viewModel.openDisconnectPicker(task)
                },
                onDelete = {
                    viewModel.deleteTask(taskForAction.id)
                    selectedTaskForAction = null
                }
            )
        }

        // 11. Goal Context Action Bottom Sheet
        val goalForAction = selectedGoalForAction
        if (goalForAction != null) {
            GoalActionBottomSheet(
                goal = goalForAction,
                onDismiss = { selectedGoalForAction = null },
                onAddTask = {
                    selectedGoalForAction = null
                    viewModel.openInlineSprout(goalForAction, SproutMode.CHILD)
                },
                onEditGoal = {
                    val g = goalForAction
                    selectedGoalForAction = null
                    viewModel.openGoalEditDialog(g)
                },
                onToggleCollapse = {
                    viewModel.toggleCollapse(goalForAction.id)
                    selectedGoalForAction = null
                },
                onTargetFirstTask = {
                    selectedGoalForAction = null
                    val firstTask = state.tasks.find { it.goalId == goalForAction.id && !it.isCompleted }
                    if (firstTask != null) {
                        centerOnNode(firstTask)
                    }
                },
                onDeleteGoal = {
                    val gId = goalForAction.id
                    selectedGoalForAction = null
                    viewModel.deleteGoal(gId)
                }
            )
        }

        val composerPrefs by viewModel.userPreferences.collectAsState()

        // 12. Smart Task Composer (Create Mode)
        if (showComposerForCreate) {
            SmartTaskComposerSheetV2(
                onDismiss = { showComposerForCreate = false },
                mode = SheetMode.Create,
                activeTasks = state.allRawTasks,
                preferences = composerPrefs,
                onSave = { newTask ->
                    showComposerForCreate = false
                    val spawnX = -panOffset.x / (zoomScale * density) + (screenWidth / 2f) / (zoomScale * density) - 105f
                    val spawnY = -panOffset.y / (zoomScale * density) + (screenHeight / 2f) / (zoomScale * density) - 35f
                    viewModel.addTask(newTask, Offset(spawnX, spawnY))
                }
            )
        }

        // 13. Smart Task Composer (Edit Mode)
        editingTaskEntity?.let { entity ->
            SmartTaskComposerSheetV2(
                onDismiss = { editingTaskEntity = null },
                mode = SheetMode.Edit(entity),
                activeTasks = state.allRawTasks,
                preferences = composerPrefs,
                onSave = { updated ->
                    editingTaskEntity = null
                    viewModel.updateTask(updated)
                }
            )
        }

        // 14. Connection Type Selection Modal Sheet
        state.connectionPrompt?.let { prompt ->
            ConnectionPromptDialog(
                source = prompt.first,
                target = prompt.second,
                onConfirm = { type -> viewModel.confirmConnection(prompt.first, prompt.second, type) },
                onDismiss = { viewModel.dismissConnectionPrompt() }
            )
        }

        // 15. Disconnect Individual Node Picker Bottom Sheet
        state.disconnectTargetNode?.let { targetTask ->
            DisconnectPickerBottomSheet(
                task = targetTask,
                connections = state.disconnectConnections,
                onDismiss = { viewModel.dismissDisconnectPicker() },
                onRemoveConnection = { conn -> viewModel.removeSingleConnection(targetTask.id, conn) },
                onDisconnectAll = {
                    viewModel.disconnectNode(targetTask.id)
                    viewModel.dismissDisconnectPicker()
                }
            )
        }

        // 16. Goal Creation Dialog
        if (state.isGoalCreateOpen) {
            GoalCreationDialog(
                onDismiss = { viewModel.dismissGoalCreateDialog() },
                onConfirm = { title, desc, targetDate ->
                    viewModel.createGoal(title, desc, targetDate)
                }
            )
        }

        // 17. Goal Edit Dialog
        state.editingGoalNode?.let { goalNode ->
            GoalEditDialog(
                goal = goalNode,
                onDismiss = { viewModel.dismissGoalEditDialog() },
                onConfirm = { title, desc, targetDate ->
                    viewModel.updateGoal(goalNode.id, title, desc, targetDate)
                }
            )
        }
    }
}

private fun calculateBorderIntersection(node: MindmapNode, targetCenter: Offset, density: Float): Offset {
    val cx = (node.x + node.width / 2f) * density
    val cy = (node.y + node.height / 2f) * density
    val halfW = (node.width / 2f) * density
    val halfH = (node.height / 2f) * density

    val dx = targetCenter.x - cx
    val dy = targetCenter.y - cy
    if (kotlin.math.abs(dx) < 0.001f && kotlin.math.abs(dy) < 0.001f) {
        return Offset(cx, cy)
    }

    val tx = if (kotlin.math.abs(dx) > 0.001f) halfW / kotlin.math.abs(dx) else Float.MAX_VALUE
    val ty = if (kotlin.math.abs(dy) > 0.001f) halfH / kotlin.math.abs(dy) else Float.MAX_VALUE
    val t = minOf(tx, ty)

    return Offset(cx + dx * t, cy + dy * t)
}

@Composable
private fun GoalHubNodeView(
    goal: MindmapGoalNode,
    density: Float,
    zoomScale: Float = 1f,
    isConnectingSource: Boolean,
    isHighlighted: Boolean = false,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onClick: () -> Unit,
    onToggleConnect: () -> Unit,
    onToggleCollapse: () -> Unit,
    onSproutChild: () -> Unit
) {
    var isDraggingLocally by remember { mutableStateOf(false) }
    var localOffset by remember { mutableStateOf(Offset(goal.x, goal.y)) }
    LaunchedEffect(goal.x, goal.y) {
        if (!isDraggingLocally) {
            localOffset = Offset(goal.x, goal.y)
        }
    }

    val borderColor = when {
        goal.isDropTarget -> Color(0xFF10B981)
        isHighlighted -> Color(0xFF38BDF8)
        isConnectingSource -> Color(0xFF818CF8)
        else -> Color(0xFF6366F1)
    }
    val borderWidth = if (goal.isDropTarget || isConnectingSource || isHighlighted) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .offset { IntOffset((localOffset.x * density).roundToInt(), (localOffset.y * density).roundToInt()) }
            .width(goal.width.dp)
            .background(Color(0xFF13151B), RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag Handle
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag Goal",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(20.dp)
                        .pointerInput(goal.id) {
                            detectDragGestures(
                                onDragStart = { isDraggingLocally = true },
                                onDragEnd = {
                                    isDraggingLocally = false
                                    onDragEnd(localOffset)
                                },
                                onDragCancel = { isDraggingLocally = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val effectiveScale = zoomScale.coerceAtLeast(0.1f)
                                localOffset += Offset(
                                    dragAmount.x / (effectiveScale * density),
                                    dragAmount.y / (effectiveScale * density)
                                )
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Inline Sprout '+' Button
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                            .border(1.dp, Color(0xFF6366F1), CircleShape)
                            .clickable { onSproutChild() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task to Goal",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Tap-to-Connect Dot
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF222634)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goal.completedTasks}/${goal.totalTasks} Done",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                // Branch Folding (+ / -) Toggle Badge
                if (goal.hasChildren) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (goal.isCollapsed) Color(0xFF6366F1) else Color(0xFF222634))
                            .clickable { onToggleCollapse() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (goal.isCollapsed) "+${goal.childCount}" else "−",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (goal.isCollapsed) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskNodeCardView(
    task: MindmapTaskNode,
    density: Float,
    zoomScale: Float = 1f,
    isActiveStartingNode: Boolean = false,
    isConnectingSource: Boolean,
    isHighlighted: Boolean = false,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onToggleConnect: () -> Unit,
    onToggleCollapse: () -> Unit,
    onSproutChild: () -> Unit
) {
    var isDraggingLocally by remember { mutableStateOf(false) }
    var localOffset by remember { mutableStateOf(Offset(task.x, task.y)) }
    LaunchedEffect(task.x, task.y) {
        if (!isDraggingLocally) {
            localOffset = Offset(task.x, task.y)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color(0xFFF59E0B)
        Priority.MEDIUM -> Color(0xFF6366F1)
        Priority.LOW -> Color(0xFF10B981)
    }

    val cardBg = when {
        task.isDropTarget -> Color(0xFF064E3B).copy(alpha = 0.5f)
        isActiveStartingNode -> Color(0xFF064E3B).copy(alpha = 0.35f * pulseGlow)
        task.isCompleted -> Color(0xFF14161E)
        else -> Color(0xFF13151B)
    }

    val borderColor = when {
        task.isDropTarget -> Color(0xFF10B981)
        isHighlighted -> Color(0xFF38BDF8)
        isConnectingSource -> Color(0xFF818CF8)
        isActiveStartingNode -> Color(0xFF10B981).copy(alpha = pulseGlow)
        task.isCompleted -> Color(0xFF2A2E3B)
        task.depth > 1 -> Color(0xFF818CF8).copy(alpha = 0.45f)
        else -> priorityColor.copy(alpha = 0.65f)
    }

    val borderWidth = if (task.isDropTarget || isConnectingSource || isActiveStartingNode || isHighlighted) 2.5.dp else 1.5.dp
    val textColor = if (task.isCompleted) Color(0xFF64748B) else Color(0xFFF8FAFC)

    Box(
        modifier = Modifier
            .offset { IntOffset((localOffset.x * density).roundToInt(), (localOffset.y * density).roundToInt()) }
            .width(task.width.dp)
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Active starting node beacon badge
            if (isActiveStartingNode) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = pulseGlow)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Text(
                            text = "▶ START HERE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Drop target indicator banner if active
            if (task.isDropTarget) {
                Text(
                    text = "DROP TO REPARENT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Checkbox for task completion
                Box(
                    modifier = Modifier
                        .size(20.dp)
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
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title
                Text(
                    text = task.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Action cluster: Sprout (+), Branch Collapse, Drag Handle, Connect Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Branch Collapse/Expand Toggle Badge
                    if (task.hasChildren) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (task.isCollapsed) Color(0xFF6366F1) else Color(0xFF1E2230))
                                .border(1.dp, if (task.isCollapsed) Color(0xFF818CF8) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { onToggleCollapse() }
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (task.isCollapsed) "+${task.childCount}" else "−",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCollapsed) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Inline Branch Sprout (+) Button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                            .border(1.dp, Color(0xFF4F46E5), CircleShape)
                            .clickable { onSproutChild() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sprout Subtask",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    // Drag Handle
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Handle",
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(18.dp)
                            .pointerInput(task.id) {
                                detectDragGestures(
                                    onDragStart = { isDraggingLocally = true },
                                    onDragEnd = {
                                        isDraggingLocally = false
                                        onDragEnd(localOffset)
                                    },
                                    onDragCancel = { isDraggingLocally = false }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val effectiveScale = zoomScale.coerceAtLeast(0.1f)
                                    localOffset += Offset(
                                        dragAmount.x / (effectiveScale * density),
                                        dragAmount.y / (effectiveScale * density)
                                    )
                                    onDrag(localOffset)
                                }
                            }
                    )

                    // Tap-to-Connect Dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isConnectingSource) Color(0xFF818CF8) else priorityColor)
                            .clickable { onToggleConnect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }

            // Dynamic badges row (priority, deadline, tags, dependencies)
            if (task.tags.isNotBlank() || task.deadlineDate != null || task.priority == Priority.HIGH || task.dependsOnTaskIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.priority == Priority.HIGH) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("HIGH", fontSize = 8.5.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (task.deadlineDate != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("DUE", fontSize = 8.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Medium)
                        }
                    }
                    if (task.dependsOnTaskIds.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("⛓️ DEP", fontSize = 8.5.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                        }
                    }
                    if (task.tags.isNotBlank()) {
                        val firstTag = task.tags.split(",").firstOrNull { it.isNotBlank() }?.trim()
                        if (firstTag != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF818CF8).copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("#$firstTag", fontSize = 8.5.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineSproutDialog(
    referenceNode: MindmapNode,
    mode: SproutMode,
    onDismiss: () -> Unit,
    onConfirm: (title: String, priority: Priority, createAnother: Boolean) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141720),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (mode == SproutMode.CHILD) "Add Subtask" else "Add Sibling Task",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mode == SproutMode.CHILD) "Under: ${referenceNode.title.take(28)}" else "Next to: ${referenceNode.title.take(28)}",
                            color = Color(0xFF818CF8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = { Text(if (mode == SproutMode.CHILD) "Subtask title..." else "Sibling task title...", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF2A2E3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (titleText.isNotBlank()) {
                                onConfirm(titleText, priority, false)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Priority selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            Priority.HIGH -> Color(0xFFF59E0B)
                            Priority.MEDIUM -> Color(0xFF6366F1)
                            Priority.LOW -> Color(0xFF10B981)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) pColor.copy(alpha = 0.25f) else Color(0xFF1E2230))
                                .border(1.dp, if (isSelected) pColor else Color(0xFF2A2E3B), RoundedCornerShape(8.dp))
                                .clickable { priority = p }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) pColor else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (titleText.isNotBlank()) {
                                onConfirm(titleText, priority, true)
                                titleText = ""
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF818CF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Another", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (titleText.isNotBlank()) {
                                onConfirm(titleText, priority, false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (mode == SproutMode.CHILD) "Add Subtask" else "Add Sibling", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskActionBottomSheet(
    task: MindmapTaskNode,
    onDismiss: () -> Unit,
    onAddSubtask: () -> Unit,
    onAddSibling: () -> Unit,
    onEdit: () -> Unit,
    onStartFocus: () -> Unit,
    onToggleConnect: () -> Unit,
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

            // Start Focus Session
            ListItem(
                headlineContent = { Text("Start Focus Session", color = Color.White) },
                leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981)) },
                modifier = Modifier.clickable { onStartFocus() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Add Subtask
            ListItem(
                headlineContent = { Text("Add Subtask", color = Color(0xFF818CF8)) },
                leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onAddSubtask() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Add Sibling Task
            ListItem(
                headlineContent = { Text("Add Sibling Task", color = Color(0xFF818CF8)) },
                leadingContent = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onAddSibling() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Edit Task
            ListItem(
                headlineContent = { Text("Edit Task Details", color = Color.White) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onEdit() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Connect to Node
            ListItem(
                headlineContent = { Text("Connect to Node", color = Color(0xFFF59E0B)) },
                leadingContent = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFFF59E0B)) },
                modifier = Modifier.clickable { onToggleConnect() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Disconnect Individual Connections
            ListItem(
                headlineContent = { Text("Disconnect Connections...", color = Color(0xFFF59E0B)) },
                supportingContent = { Text("Manage or remove individual links/dependencies", color = Color(0xFF94A3B8), fontSize = 11.5.sp) },
                leadingContent = { Icon(Icons.Default.LinkOff, contentDescription = null, tint = Color(0xFFF59E0B)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalActionBottomSheet(
    goal: MindmapGoalNode,
    onDismiss: () -> Unit,
    onAddTask: () -> Unit,
    onToggleCollapse: () -> Unit,
    onTargetFirstTask: () -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit
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
                text = goal.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${goal.completedTasks}/${goal.totalTasks} Tasks Completed (${(goal.progress * 100).roundToInt()}%)",
                color = Color(0xFF818CF8),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Add Task to Goal
            ListItem(
                headlineContent = { Text("Add Task to Goal", color = Color(0xFF818CF8)) },
                leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onAddTask() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Edit Goal
            ListItem(
                headlineContent = { Text("Edit Goal Details", color = Color.White) },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF818CF8)) },
                modifier = Modifier.clickable { onEditGoal() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Toggle Expand / Collapse
            ListItem(
                headlineContent = { Text(if (goal.isCollapsed) "Expand All Branches" else "Collapse All Branches", color = Color.White) },
                leadingContent = {
                    Icon(
                        if (goal.isCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                        contentDescription = null,
                        tint = Color(0xFF818CF8)
                    )
                },
                modifier = Modifier.clickable { onToggleCollapse() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Target Next Task in Goal
            ListItem(
                headlineContent = { Text("Target Next Step in Goal", color = Color(0xFF10B981)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981)) },
                modifier = Modifier.clickable { onTargetFirstTask() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Delete Goal
            ListItem(
                headlineContent = { Text("Delete Goal", color = Color(0xFFEF4444)) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                modifier = Modifier.clickable { onDeleteGoal() },
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
                    onClick = { onConfirm(MindmapEdgeType.SUBTASK_LINK) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Make '${target.title.take(14)}' a Subtask", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisconnectPickerBottomSheet(
    task: MindmapTaskNode,
    connections: List<NodeConnection>,
    onDismiss: () -> Unit,
    onRemoveConnection: (NodeConnection) -> Unit,
    onDisconnectAll: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Disconnect Links",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "For '${task.title.take(30)}'",
                        color = Color(0xFF818CF8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active connections on this task.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = "Tap any link to remove it individually:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(connections, key = { "${it.type}_${it.otherNodeId}_${it.edgeId}" }) { conn ->
                        val (typeBadge, badgeColor) = when (conn.type) {
                            ConnectionType.PARENT_LINK -> "PARENT" to Color(0xFF6366F1)
                            ConnectionType.GOAL_LINK -> "GOAL" to Color(0xFF10B981)
                            ConnectionType.DEPENDENCY -> "PREREQUISITE" to Color(0xFFF59E0B)
                            ConnectionType.REVERSE_DEPENDENCY -> "PREREQ FOR" to Color(0xFFF59E0B)
                            ConnectionType.CHILD_LINK -> "SUBTASK" to Color(0xFFA78BFA)
                            ConnectionType.VISUAL_EDGE -> "VISUAL WIRE" to Color(0xFF38BDF8)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B1E2B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3346)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = typeBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = conn.label,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                TextButton(
                                    onClick = { onRemoveConnection(conn) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LinkOff,
                                            contentDescription = "Disconnect",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDisconnectAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect All Connections", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GoalCreationDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, targetDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141720),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = "New Goal Hub",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Goal Title (e.g. Launch Product, Master System)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF2A2E3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description / Objective (optional)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF2A2E3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Target Horizon:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val horizons = listOf(
                        "1 Mo" to (30L * 24 * 3600 * 1000),
                        "3 Mo" to (90L * 24 * 3600 * 1000),
                        "6 Mo" to (180L * 24 * 3600 * 1000),
                        "1 Yr" to (365L * 24 * 3600 * 1000)
                    )
                    horizons.forEach { (label, duration) ->
                        val isSelected = targetDate != null && kotlin.math.abs(targetDate!! - (System.currentTimeMillis() + duration)) < 100000L
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF1E2230),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFF2A2E3B)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    targetDate = if (isSelected) null else System.currentTimeMillis() + duration
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFF94A3B8),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), description.trim(), targetDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Goal", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalEditDialog(
    goal: MindmapGoalNode,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, targetDate: Long?) -> Unit
) {
    var title by remember(goal.id) { mutableStateOf(goal.title) }
    var description by remember(goal.id) { mutableStateOf("") }
    var targetDate by remember(goal.id) { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141720),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF818CF8)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF818CF8).copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = "Edit Goal Details",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Goal Title...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF2A2E3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description / Objective (optional)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF2A2E3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), description.trim(), targetDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
