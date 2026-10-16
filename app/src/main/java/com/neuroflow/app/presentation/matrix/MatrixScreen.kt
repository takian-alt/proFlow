package com.neuroflow.app.presentation.matrix

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.TaskScoringEngine
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.presentation.common.EnergyInsight
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantLabel
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(
    onNavigateToQuadrant: (String) -> Unit,
    onNavigateToFocus: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToOnHold: () -> Unit = {},
    initialNewTaskOpen: Boolean = false,
    onInitialNewTaskConsumed: () -> Unit = {},
    hideTopBar: Boolean = false,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var newTaskQuadrant by remember { mutableStateOf<Quadrant?>(null) }

    androidx.compose.runtime.LaunchedEffect(initialNewTaskOpen) {
        if (initialNewTaskOpen) {
            newTaskQuadrant = null
            showNewTaskSheet = true
            onInitialNewTaskConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Matrix",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                actions = {
                    IconButton(
                        onClick = {
                            uiState.topScoredTaskId?.let { onNavigateToFocus(it) }
                        },
                        enabled = uiState.topScoredTaskId != null
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Focus Mode")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newTaskQuadrant = null
                    showNewTaskSheet = true
                },
                icon = { Icon(Icons.Filled.Add, "New Task") },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        ThreeQuadrantMatrixPage(
            uiState = uiState,
            onNavigateToQuadrant = onNavigateToQuadrant,
            onNavigateToFocus = onNavigateToFocus,
            onAddTaskForQuadrant = { quadrant ->
                newTaskQuadrant = quadrant
                showNewTaskSheet = true
            },
            onNavigateToOnHold = onNavigateToOnHold,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (showNewTaskSheet) {
        NewTaskSheet(
            onDismiss = {
                showNewTaskSheet = false
                newTaskQuadrant = null
            },
            onSave = { task ->
                viewModel.insertTask(task)
                showNewTaskSheet = false
                newTaskQuadrant = null
            },
            prefilledQuadrant = newTaskQuadrant,
            availableTasks = uiState.allActiveTasks
        )
    }
}

/**
 * 3-Quadrant Matrix Page
 * 1. Workload balance indicator & header
 * 2. DO FIRST (Hard tasks) — Hero card with live task previews, frog tags, and direct Focus launch
 * 3. SCHEDULE & DELEGATE — Sleek split cards with mini task previews and quick-add actions
 * 4. Micro-interactions: Bouncy spring press physics, pulsing priority indicators, and smooth gradients
 * 5. On Hold / Planning discovery banner at bottom
 */
@Composable
private fun ThreeQuadrantMatrixPage(
    uiState: MatrixUiState,
    onNavigateToQuadrant: (String) -> Unit,
    onNavigateToFocus: (String) -> Unit,
    onAddTaskForQuadrant: (Quadrant) -> Unit,
    onNavigateToOnHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    val doFirstTasks = remember(uiState.tasksByQuadrant, uiState.preferences) {
        TaskScoringEngine.sortedByScore(
            uiState.tasksByQuadrant[Quadrant.DO_FIRST].orEmpty(),
            uiState.preferences
        )
    }
    val scheduleTasks = remember(uiState.tasksByQuadrant, uiState.preferences) {
        TaskScoringEngine.sortedByScore(
            uiState.tasksByQuadrant[Quadrant.SCHEDULE].orEmpty(),
            uiState.preferences
        )
    }
    val delegateTasks = remember(uiState.tasksByQuadrant, uiState.preferences) {
        TaskScoringEngine.sortedByScore(
            uiState.tasksByQuadrant[Quadrant.DELEGATE].orEmpty(),
            uiState.preferences
        )
    }

    val totalActive = doFirstTasks.size + scheduleTasks.size + delegateTasks.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP HEADER & WORKLOAD BALANCE BAR
        MatrixHeaderSection(
            totalActive = totalActive,
            doFirstCount = doFirstTasks.size,
            scheduleCount = scheduleTasks.size,
            delegateCount = delegateTasks.size
        )

        // 2. QUADRANT 1: DO FIRST (Hero Card)
        MatrixHeroQuadrantCard(
            title = "DO FIRST",
            badgeText = "CRITICAL & URGENT",
            subtitle = "High impact • Address immediately in peak focus",
            tasks = doFirstTasks,
            accentColor = if (isDark) NeuroFlowColors.DoFirstTextDark else NeuroFlowColors.DoFirstText,
            containerGradient = if (isDark) {
                listOf(Color(0xFF381519), Color(0xFF260D10))
            } else {
                listOf(Color(0xFFFFF0F2), Color(0xFFFFE3E6))
            },
            onCardClick = { onNavigateToQuadrant("DO_FIRST") },
            onTaskClick = onNavigateToFocus,
            onAddTask = { onAddTaskForQuadrant(Quadrant.DO_FIRST) }
        )

        // 3. QUADRANTS 2 & 3: SCHEDULE & DELEGATE (Split Row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SCHEDULE CARD
            MatrixCompactQuadrantCard(
                title = "SCHEDULE",
                tagText = "DEEP WORK",
                icon = Icons.Filled.CalendarMonth,
                tasks = scheduleTasks,
                accentColor = if (isDark) NeuroFlowColors.ScheduleTextDark else NeuroFlowColors.ScheduleText,
                containerGradient = if (isDark) {
                    listOf(Color(0xFF0F291E), Color(0xFF071B12))
                } else {
                    listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                },
                onCardClick = { onNavigateToQuadrant("SCHEDULE") },
                onTaskClick = onNavigateToFocus,
                onAddTask = { onAddTaskForQuadrant(Quadrant.SCHEDULE) },
                modifier = Modifier.weight(1f)
            )

            // DELEGATE CARD
            MatrixCompactQuadrantCard(
                title = "DELEGATE",
                tagText = "QUICK WINS",
                icon = Icons.Filled.Groups,
                tasks = delegateTasks,
                accentColor = if (isDark) NeuroFlowColors.DelegateTextDark else NeuroFlowColors.DelegateText,
                containerGradient = if (isDark) {
                    listOf(Color(0xFF2E2207), Color(0xFF1D1503))
                } else {
                    listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                },
                onCardClick = { onNavigateToQuadrant("DELEGATE") },
                onTaskClick = onNavigateToFocus,
                onAddTask = { onAddTaskForQuadrant(Quadrant.DELEGATE) },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. ON HOLD / PLANNING SUBTLE BANNER
        MatrixOnHoldBanner(
            onHoldCount = uiState.quadrantCounts[Quadrant.ELIMINATE] ?: 0,
            onClick = onNavigateToOnHold
        )

        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
private fun MatrixHeaderSection(
    totalActive: Int,
    doFirstCount: Int,
    scheduleCount: Int,
    delegateCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Priority Matrix",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$totalActive active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Proportional Distribution Bar (if active tasks exist)
            if (totalActive > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        if (doFirstCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(doFirstCount.toFloat())
                                    .fillMaxHeight()
                                    .background(NeuroFlowColors.DoFirstText)
                            )
                        }
                        if (scheduleCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(scheduleCount.toFloat())
                                    .fillMaxHeight()
                                    .background(NeuroFlowColors.ScheduleText)
                            )
                        }
                        if (delegateCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(delegateCount.toFloat())
                                    .fillMaxHeight()
                                    .background(NeuroFlowColors.DelegateText)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MatrixLegendItem(
                            color = NeuroFlowColors.DoFirstText,
                            label = "Do First",
                            count = doFirstCount
                        )
                        MatrixLegendItem(
                            color = NeuroFlowColors.ScheduleText,
                            label = "Schedule",
                            count = scheduleCount
                        )
                        MatrixLegendItem(
                            color = NeuroFlowColors.DelegateText,
                            label = "Delegate",
                            count = delegateCount
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixLegendItem(color: Color, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MatrixHeroQuadrantCard(
    title: String,
    badgeText: String,
    subtitle: String,
    tasks: List<TaskEntity>,
    accentColor: Color,
    containerGradient: List<Color>,
    onCardClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "heroCardScale"
    )

    // Breathing pulse for the hero bolt icon
    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseScale by if (tasks.isNotEmpty()) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "firePulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(containerGradient))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = accentColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${tasks.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View all",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Task list previews or empty state
            if (tasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.take(2).forEach { task ->
                        MatrixHeroTaskPreviewItem(
                            task = task,
                            accentColor = accentColor,
                            onStartFocus = { onTaskClick(task.id) }
                        )
                    }

                    if (tasks.size > 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "+${tasks.size - 2} more in Do First →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "✨ All critical tasks clear",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onAddTask,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixHeroTaskPreviewItem(
    task: TaskEntity,
    accentColor: Color,
    onStartFocus: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "heroItemScale"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onStartFocus
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (task.isFrog) {
                    Text("🐸", fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.estimatedDurationMinutes > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${task.estimatedDurationMinutes}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (task.deadlineDate != null && task.deadlineDate > 0L) {
                            val isToday = remember(task.deadlineDate) {
                                val cal1 = Calendar.getInstance()
                                val cal2 = Calendar.getInstance().apply { timeInMillis = task.deadlineDate }
                                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                            }
                            if (isToday) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE53935).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Due Today",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = onStartFocus,
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start Focus",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MatrixCompactQuadrantCard(
    title: String,
    tagText: String,
    icon: ImageVector,
    tasks: List<TaskEntity>,
    accentColor: Color,
    containerGradient: List<Color>,
    onCardClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "compactCardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCardClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(containerGradient))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${tasks.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor
                )
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor.copy(alpha = 0.8f)
                )
            }

            // Tasks Preview or Empty State
            if (tasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.take(2).forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTaskClick(task.id) }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (task.isFrog) {
                                        Text("🐸", fontSize = 11.sp)
                                    }
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (task.estimatedDurationMinutes > 0) {
                                    Text(
                                        text = "${task.estimatedDurationMinutes}m",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (tasks.size > 2) {
                        Text(
                            text = "+${tasks.size - 2} more →",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No tasks pending",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    FilledTonalButton(
                        onClick = onAddTask,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixOnHoldBanner(
    onHoldCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "onHoldBannerScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PauseCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "On Hold / Backlog Ideas",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (onHoldCount > 0) "$onHoldCount ideas on hold • Review anytime" else "Store deferred ideas and somatic breaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
