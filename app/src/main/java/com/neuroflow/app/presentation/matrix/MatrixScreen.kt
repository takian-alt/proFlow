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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
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
import com.neuroflow.app.domain.engine.SleepPressureDetector
import com.neuroflow.app.domain.engine.TaskScoringEngine
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.presentation.common.EnergyInsight
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantLabel
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToQuadrant: (String) -> Unit,
    onNavigateToFocus: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit = {},
    onNavigateToOnHold: () -> Unit = {},
    initialNewTaskOpen: Boolean = false,
    onInitialNewTaskConsumed: () -> Unit = {},
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var newTaskQuadrant by remember { mutableStateOf<Quadrant?>(null) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(initialNewTaskOpen) {
        if (initialNewTaskOpen) {
            newTaskQuadrant = null
            showNewTaskSheet = true
            onInitialNewTaskConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Minimalist segmented switcher: [ Home ] [ Matrix ]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabPill(
                            selected = pagerState.currentPage == 0,
                            title = "Home",
                            icon = Icons.Filled.Home,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                        TabPill(
                            selected = pagerState.currentPage == 1,
                            title = "Matrix",
                            icon = Icons.Filled.GridView,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, "Menu")
                    }
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            if (page == 0) {
                // PAGE 0: Minimalist Home Screen with 3 animated horizontal sections & task add
                MinimalistHomePage(
                    uiState = uiState,
                    onNavigateToFocus = {
                        val taskId = uiState.topScoredTaskId
                        if (taskId != null) {
                            onNavigateToFocus(taskId)
                        } else {
                            val candidate = uiState.allActiveTasks.firstOrNull { it.quadrant != Quadrant.ELIMINATE }
                                ?: uiState.allActiveTasks.firstOrNull()
                            if (candidate != null) {
                                onNavigateToFocus(candidate.id)
                            } else {
                                newTaskQuadrant = null
                                showNewTaskSheet = true
                            }
                        }
                    },
                    onNavigateToNotes = onNavigateToNotes,
                    onNavigateToOnHold = onNavigateToOnHold,
                    onAddTask = {
                        newTaskQuadrant = null
                        showNewTaskSheet = true
                    },
                    onSwipeToMatrix = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
            } else {
                // PAGE 1: Matrix Screen (3 Quadrants with live previews, workload balance bar & tactile spring animations)
                ThreeQuadrantMatrixPage(
                    uiState = uiState,
                    onNavigateToQuadrant = onNavigateToQuadrant,
                    onNavigateToFocus = onNavigateToFocus,
                    onAddTaskForQuadrant = { quadrant ->
                        newTaskQuadrant = quadrant
                        showNewTaskSheet = true
                    },
                    onNavigateToOnHold = onNavigateToOnHold,
                    onSwipeToHome = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
            }
        }
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

@Composable
private fun TabPill(
    selected: Boolean,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabPillScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "tabContent"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
            Text(
                title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                color = contentColor
            )
        }
    }
}

/**
 * PAGE 0: Minimalist Home Screen
 * Displays:
 * 1. Compact Energy strip
 * 2. 3 sections horizontally with beautiful animations: Focus, Note, On Hold/Planning
 * 3. Prominent Task Add button
 * 4. Next Up / Top Focus Task preview
 * 5. Swipe left hint for 3-quadrant matrix
 */
@Composable
private fun MinimalistHomePage(
    uiState: MatrixUiState,
    onNavigateToFocus: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToOnHold: () -> Unit,
    onAddTask: () -> Unit,
    onSwipeToMatrix: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimal Energy Strip
        uiState.energy?.let { energy ->
            MatrixEnergyStrip(energy)
        }

        // Section Title
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // 3 SECTIONS HORIZONTALLY WITH BEAUTIFUL ANIMATIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Focus Section Button
            AnimatedFeatureTile(
                title = "Focus",
                subtitle = if (uiState.topScoredTask != null) "Ready" else "Timer",
                icon = Icons.Filled.PlayArrow,
                gradient = listOf(Color(0xFF7C4DFF), Color(0xFF651FFF)),
                hasPulse = true,
                badgeText = uiState.topScoredTask?.let { "1 Up" },
                onClick = onNavigateToFocus,
                modifier = Modifier.weight(1f)
            )

            // 2. Note Section Button
            AnimatedFeatureTile(
                title = "Notes",
                subtitle = if (uiState.noteCount > 0) "${uiState.noteCount} notes" else "Write",
                icon = Icons.AutoMirrored.Filled.Notes,
                gradient = listOf(Color(0xFFFFB300), Color(0xFFFB8C00)),
                hasPulse = false,
                badgeText = if (uiState.noteCount > 0) "${uiState.noteCount}" else null,
                onClick = onNavigateToNotes,
                modifier = Modifier.weight(1f)
            )

            // 3. On Hold / Planning Section Button
            val onHoldCount = uiState.quadrantCounts[Quadrant.ELIMINATE] ?: 0
            AnimatedFeatureTile(
                title = "On Hold",
                subtitle = if (onHoldCount > 0) "$onHoldCount ideas" else "Planning",
                icon = Icons.Filled.PauseCircle,
                gradient = listOf(Color(0xFFE57373), Color(0xFFE53935)),
                hasPulse = false,
                badgeText = if (onHoldCount > 0) "$onHoldCount" else null,
                onClick = onNavigateToOnHold,
                modifier = Modifier.weight(1f)
            )
        }

        // TASK ADD BUTTON (PROMINENT MINIMALIST BAR)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddTask),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "Add New Task",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Set priority, energy, schedule or ideas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // TOP SCORED TASK / NEXT UP CARD
        uiState.topScoredTask?.let { topTask ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Next Focus Priority",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TopPriorityCard(
                    task = topTask,
                    onStartFocus = onNavigateToFocus
                )
            }
        }

        // SWIPE LEFT CUE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSwipeToMatrix),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Swipe left for Tasks & Matrix (3)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

/**
 * Animated Tile for the 3 horizontal sections: Focus, Note, On Hold
 */
@Composable
private fun AnimatedFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    hasPulse: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Subtle breathing pulse for Focus
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (hasPulse) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAnim"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush = Brush.verticalGradient(gradient))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Top priority task card shown on Minimalist Home
 */
@Composable
private fun TopPriorityCard(
    task: TaskEntity,
    onStartFocus: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isFrog) {
                        Text("🐸 ", fontSize = 14.sp)
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeuroFlowColors.DoFirstText.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Do First",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowColors.DoFirstText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (task.estimatedDurationMinutes > 0) {
                        Text(
                            "${task.estimatedDurationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(
                onClick = onStartFocus,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Focus", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * PAGE 1: 3-Quadrant Matrix Page
 * Upgraded with:
 * 1. Workload balance indicator & header with Home shortcut
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
    onSwipeToHome: () -> Unit
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
        modifier = Modifier
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
            delegateCount = delegateTasks.size,
            onSwipeToHome = onSwipeToHome
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
    delegateCount: Int,
    onSwipeToHome: () -> Unit
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to Home Button
                val homeInteraction = remember { MutableInteractionSource() }
                val isHomePressed by homeInteraction.collectIsPressedAsState()
                val homeScale by animateFloatAsState(
                    targetValue = if (isHomePressed) 0.93f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "homeBtnScale"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .scale(homeScale)
                        .clickable(
                            interactionSource = homeInteraction,
                            indication = null,
                            onClick = onSwipeToHome
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Home",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Home",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Header Title & Active Count Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun MatrixEnergyStrip(energy: EnergyScoreRepository.EnergyUiModel) {
    fun normalizeMinuteOfDay(minuteOfDay: Int): Int {
        val normalized = minuteOfDay % (24 * 60)
        return if (normalized < 0) normalized + (24 * 60) else normalized
    }

    fun minuteIsInsideWindow(minute: Int, startMinute: Int, durationMinutes: Int): Boolean {
        if (durationMinutes <= 0) return false
        val safeMinute = normalizeMinuteOfDay(minute)
        val safeStart = normalizeMinuteOfDay(startMinute)
        val endExclusive = safeStart + durationMinutes
        return if (endExclusive <= 24 * 60) {
            safeMinute in safeStart until endExclusive
        } else {
            safeMinute >= safeStart || safeMinute < (endExclusive % (24 * 60))
        }
    }

    fun minutesUntil(targetMinute: Int, nowMinute: Int): Int {
        val safeTarget = normalizeMinuteOfDay(targetMinute)
        val safeNow = normalizeMinuteOfDay(nowMinute)
        val forward = safeTarget - safeNow
        return if (forward >= 0) forward else forward + (24 * 60)
    }

    fun fmtMinuteOfDay(minuteOfDay: Int): String {
        val normalized = normalizeMinuteOfDay(minuteOfDay)
        val hour = normalized / 60
        val minute = normalized % 60
        val amPm = if (hour < 12) "am" else "pm"
        val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
        return String.format("%d:%02d%s", displayHour, minute, amPm)
    }

    fun formatMinutes(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    val fatigueColor = when (energy.fatigueZone) {
        SleepPressureDetector.FatigueZone.RESTED -> Color(0xFF43A047)
        SleepPressureDetector.FatigueZone.MODERATE -> Color(0xFFF9A825)
        SleepPressureDetector.FatigueZone.HIGH -> Color(0xFFF57C00)
        SleepPressureDetector.FatigueZone.CRITICAL -> Color(0xFFE53935)
    }

    data class PeakWindowUi(
        val index: Int,
        val startMinute: Int,
        val endMinute: Int,
        val isActiveNow: Boolean,
        val minutesUntilStart: Int
    )

    val nowMinuteOfDay = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
    val profileWindows = energy.effectivePeakProfile?.let { profile ->
        profile.windows.mapIndexed { index, window ->
            val startMinute = normalizeMinuteOfDay(profile.anchorMinuteOfDay + window.startMinuteOffset)
            val endMinute = normalizeMinuteOfDay(startMinute + window.durationMinutes)
            val activeNow = minuteIsInsideWindow(
                minute = nowMinuteOfDay,
                startMinute = startMinute,
                durationMinutes = window.durationMinutes
            )
            PeakWindowUi(
                index = index,
                startMinute = startMinute,
                endMinute = endMinute,
                isActiveNow = activeNow,
                minutesUntilStart = if (activeNow) 0 else minutesUntil(startMinute, nowMinuteOfDay)
            )
        }
    }.orEmpty()
    val activeWindow = profileWindows.firstOrNull { it.isActiveNow }
    val nextWindow = if (activeWindow != null) {
        profileWindows.filterNot { it.isActiveNow }.minByOrNull { it.minutesUntilStart }
    } else {
        profileWindows.minByOrNull { it.minutesUntilStart }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            NeuroFlowColors.Purple.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚡", fontSize = 14.sp)
                    Text("Energy Score", fontWeight = FontWeight.Bold)
                }
                Text(
                    "${energy.availableEnergy}/100",
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowColors.Purple
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (energy.availableEnergy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeuroFlowColors.Purple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (activeWindow != null) {
                Text(
                    text = "Now in Peak ${activeWindow.index + 1} (${fmtMinuteOfDay(activeWindow.startMinute)}–${fmtMinuteOfDay(activeWindow.endMinute)})",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            } else if (nextWindow != null) {
                Text(
                    text = "Next Peak in ${formatMinutes(nextWindow.minutesUntilStart)} (${fmtMinuteOfDay(nextWindow.startMinute)})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
