package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.analytics.AnalyticsScreen
import com.neuroflow.app.presentation.common.HyperFocusAllowlistRoute
import com.neuroflow.app.presentation.history.HistoryScreen
import com.neuroflow.app.presentation.identity.IdentityScreen
import com.neuroflow.app.presentation.logtime.LogTimeScreen
import com.neuroflow.app.presentation.notes.NotesScreen
import com.neuroflow.app.presentation.onboarding.AppGuideScreen
import com.neuroflow.app.presentation.schedule.WeeklyScheduleScreen
import com.neuroflow.app.presentation.settings.AccountSyncScreen
import com.neuroflow.app.presentation.settings.MEQQuizScreen
import com.neuroflow.app.presentation.settings.PrivacyPermissionsScreen
import com.neuroflow.app.presentation.settings.PriorityWeightsScreen
import com.neuroflow.app.presentation.settings.SettingsScreen
import com.neuroflow.app.presentation.settings.SleepLogsScreen
import android.widget.Toast
import com.neuroflow.app.presentation.v2.assistant.SmartAssistantOverlayHost
import com.neuroflow.app.presentation.v2.assistant.SmartAssistantViewModel
import com.neuroflow.app.presentation.v2.flow.FlowGroupingMode
import com.neuroflow.app.presentation.v2.flow.FlowScreenV2
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.focus.FocusScreenV2
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.presentation.v2.insights.InsightsScreenV2
import com.neuroflow.app.presentation.v2.mindmap.GoalsHubScreenV2
import com.neuroflow.app.presentation.v2.mindmap.MindmapCanvasScreen
import com.neuroflow.app.presentation.v2.mindmap.MindmapViewModel
import com.neuroflow.app.presentation.v2.plan.PlanScreenV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.presentation.v2.theme.MiniFocusSessionBar
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2
import com.neuroflow.app.presentation.v2.theme.PhaseAmbientScreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuroFlowAppV2(
    initialTaskId: String? = null,
    onInitialTaskConsumed: () -> Unit = {},
    initialOpenComposer: Boolean = false,
    onInitialComposerConsumed: () -> Unit = {},
    initialWoopTaskId: String? = null,
    onInitialWoopConsumed: () -> Unit = {},
    initialGuideOpen: Boolean = false,
    onInitialGuideConsumed: () -> Unit = {},
    initialScheduleOpen: Boolean = false,
    onInitialScheduleConsumed: () -> Unit = {},
    darkTheme: Boolean = true,
    amoledDark: Boolean = false,
    dynamicColor: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val showBottomBar = remember(currentRoute) {
        NavHubsV2.isHubRoute(currentRoute)
    }

    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null) {
            if (initialTaskId.isNotBlank()) {
                if (initialTaskId == "tab_focus") {
                    if (currentRoute.isNotEmpty() && currentRoute != NavHubsV2.Flow.route) {
                        navController.navigate(NavHubsV2.Flow.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                } else {
                    val isAlreadyOnFocus = currentRoute.startsWith("focus_v2") || currentRoute.startsWith("focus/")
                    val currentTaskId = navBackStackEntry?.arguments?.getString("taskId")
                    // Avoid redundant recreation and timer reset if already viewing this exact task (BUG-433)
                    if (!isAlreadyOnFocus || currentTaskId != initialTaskId) {
                        val currentDestId = navController.currentDestination?.id
                        navController.navigate("focus_v2/${android.net.Uri.encode(initialTaskId)}") {
                            if (isAlreadyOnFocus && currentDestId != null) {
                                popUpTo(currentDestId) {
                                    inclusive = true
                                }
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }
            onInitialTaskConsumed()
        }
    }

    LaunchedEffect(initialWoopTaskId) {
        if (initialWoopTaskId != null) {
            if (initialWoopTaskId.isNotBlank()) {
                val isAlreadyOnFocus = currentRoute.startsWith("focus_v2") || currentRoute.startsWith("focus/")
                val currentTaskId = navBackStackEntry?.arguments?.getString("taskId")
                val currentOpenWoop = navBackStackEntry?.arguments?.getBoolean("openWoop") ?: false
                // Avoid redundant recreation if already on this task with WOOP open (BUG-433)
                if (!isAlreadyOnFocus || currentTaskId != initialWoopTaskId || !currentOpenWoop) {
                    val currentDestId = navController.currentDestination?.id
                    navController.navigate("focus_v2/${android.net.Uri.encode(initialWoopTaskId)}?openWoop=true") {
                        if (isAlreadyOnFocus && currentDestId != null) {
                            popUpTo(currentDestId) {
                                inclusive = true
                            }
                        }
                        launchSingleTop = true
                    }
                }
            }
            onInitialWoopConsumed()
        }
    }

    LaunchedEffect(initialGuideOpen) {
        if (initialGuideOpen) {
            navController.navigate("app_guide") {
                launchSingleTop = true
            }
            onInitialGuideConsumed()
        }
    }

    LaunchedEffect(initialScheduleOpen) {
        if (initialScheduleOpen) {
            navController.navigate(NavHubsV2.Plan.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onInitialScheduleConsumed()
        }
    }

    LaunchedEffect(initialOpenComposer) {
        if (initialOpenComposer) {
            if (currentRoute.isNotEmpty() && currentRoute != NavHubsV2.Flow.route) {
                navController.navigate(NavHubsV2.Flow.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val isAmoled = darkTheme && amoledDark
    val context = LocalContext.current
    val activeFocusSessionState by FocusService.activeSession.collectAsState()
    val session = activeFocusSessionState
    val showMiniFocusBar = session != null && !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("focus/")
    var showToolsSheet by remember { mutableStateOf(false) }

    NeuroFlowThemeV2(darkTheme = darkTheme, amoledDark = isAmoled, dynamicColor = dynamicColor) {
        val flowViewModel: FlowViewModelV2 = hiltViewModel()
        val planViewModel: PlanViewModelV2 = hiltViewModel()
        val smartAssistantViewModel: SmartAssistantViewModel = hiltViewModel()
        val flowState by flowViewModel.uiState.collectAsState()
        val assistantState by smartAssistantViewModel.uiState.collectAsState()

        LaunchedEffect(assistantState.toastMessage) {
            val msg = assistantState.toastMessage
            if (!msg.isNullOrBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                smartAssistantViewModel.clearToastMessage()
            }
        }

        val nowMinute by produceState(System.currentTimeMillis() / 60_000L) {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                value = System.currentTimeMillis() / 60_000L
            }
        }
        val now = remember(nowMinute) { nowMinute * 60_000L }
        val livePhase = flowState.energyTimeline.minByOrNull {
            kotlin.math.abs(it.timestampMillis - now)
        }?.phase ?: flowState.circadianSummary?.currentSnapshot?.phase ?: EnergyPhase.PEAK

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (showMiniFocusBar) {
                        val currentSession = session ?: return@Column
                        MiniFocusSessionBar(
                            remainingSeconds = currentSession.remainingSeconds,
                            totalSeconds = currentSession.totalSeconds,
                            taskTitle = currentSession.taskTitle,
                            isPaused = currentSession.isPaused,
                            onPauseResumeClick = {
                                if (currentSession.isPaused) {
                                    FocusService.resume(context)
                                } else {
                                    FocusService.pause(context)
                                }
                            },
                            onCompleteClick = {
                                FocusService.complete(context, currentSession.taskId)
                            },
                            onTap = {
                                if (currentSession.taskId.isNotBlank()) {
                                    navController.navigate("focus_v2/${android.net.Uri.encode(currentSession.taskId)}") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    if (showBottomBar) {
                        HorizontalDivider(
                            color = NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.35f),
                            thickness = 0.75.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp
                        ) {
                            NavHubsV2.items.forEach { hub ->
                                val selected = currentDestination?.hierarchy?.any { it.route == hub.route } == true
                                NavigationBarItem(
                                    icon = { Icon(hub.icon, contentDescription = hub.title) },
                                    label = {
                                        Text(
                                            text = hub.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(hub.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            PhaseAmbientScreenContainer(
                phase = livePhase,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = NavHubsV2.Flow.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(NavHubsV2.Flow.route) {
                            FlowScreenV2(
                                initialOpenComposer = initialOpenComposer,
                                onInitialComposerConsumed = onInitialComposerConsumed,
                                onNavigateToSettings = { navController.navigate("settings") { launchSingleTop = true } },
                                onNavigateToMindmap = {
                                    navController.navigate(NavHubsV2.Goals.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onStartFocus = { taskId ->
                                    if (taskId.isNotBlank()) {
                                        navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onNavigateToMEQ = { navController.navigate("meq_quiz") { launchSingleTop = true } },
                                onNavigateToSleepLogs = { navController.navigate("sleep_logs") { launchSingleTop = true } },
                                onNavigateToNotes = { navController.navigate("notes") { launchSingleTop = true } },
                                onOpenToolsSheet = { showToolsSheet = true },
                                onStartDoorwayFocus = { taskId ->
                                    if (taskId.isNotBlank()) {
                                        navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}?mode=JUST_5_MIN") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                viewModel = flowViewModel
                            )
                        }

                        composable(NavHubsV2.Plan.route) {
                            PlanScreenV2(
                                onNavigateToFocus = { taskId ->
                                    if (taskId.isNotBlank()) {
                                        navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onNavigateToWeeklySchedule = { navController.navigate("weekly_schedule") { launchSingleTop = true } },
                                onNavigateToNotes = { navController.navigate("notes") { launchSingleTop = true } },
                                onNavigateToMindmap = {
                                    navController.navigate(NavHubsV2.Goals.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenToolsSheet = { showToolsSheet = true },
                                viewModel = planViewModel
                            )
                        }

                        composable(NavHubsV2.Goals.route) {
                            GoalsHubScreenV2(
                                onStartFocus = { taskId ->
                                    if (taskId.isNotBlank()) {
                                        navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        composable(NavHubsV2.Insights.route) {
                            InsightsScreenV2(
                                onNavigateToMEQ = { navController.navigate("meq_quiz") { launchSingleTop = true } },
                                onNavigateToSleepLogs = { navController.navigate("sleep_logs") { launchSingleTop = true } },
                                onNavigateToAnalytics = { navController.navigate("analytics") { launchSingleTop = true } },
                                onNavigateToHistory = { navController.navigate("history") { launchSingleTop = true } },
                                onNavigateToIdentity = { navController.navigate("identity") { launchSingleTop = true } },
                                onNavigateToNotes = { navController.navigate("notes") { launchSingleTop = true } },
                                onNavigateToSettings = { navController.navigate("settings") { launchSingleTop = true } },
                                smartAssistantViewModel = smartAssistantViewModel
                            )
                        }

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPriorityWeights = { navController.navigate("priority_weights") { launchSingleTop = true } },
                        onNavigateToHyperFocusAllowlist = { navController.navigate("hyperfocus_allowlist") { launchSingleTop = true } },
                        onNavigateToAppGuide = { navController.navigate("app_guide") { launchSingleTop = true } },
                        onNavigateToPrivacyPermissions = { navController.navigate("privacy_permissions") { launchSingleTop = true } },
                        onNavigateToMEQQuiz = { navController.navigate("meq_quiz") { launchSingleTop = true } },
                        onNavigateToSleepLogs = { navController.navigate("sleep_logs") { launchSingleTop = true } },
                        onNavigateToAccountSync = { navController.navigate("account_sync") { launchSingleTop = true } },
                        onNavigateToLogTime = { navController.navigate("log_time") { launchSingleTop = true } }
                    )
                }

                composable("meq_quiz") {
                    MEQQuizScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("sleep_logs") {
                    SleepLogsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("notes") {
                    NotesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("weekly_schedule") {
                    WeeklyScheduleScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDay = { dayMillis ->
                            planViewModel.selectDate(dayMillis)
                            navController.popBackStack()
                        },
                        onNavigateToFocus = { taskId ->
                            if (taskId.isNotBlank()) {
                                navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable("analytics") {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Analytics") },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            AnalyticsScreen(hideTopBar = true)
                        }
                    }
                }

                composable("history") {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("History") },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { navController.navigate("log_time") { launchSingleTop = true } }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Log Time"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            HistoryScreen(
                                hideTopBar = true,
                                onNavigateToLogTime = { navController.navigate("log_time") { launchSingleTop = true } }
                            )
                        }
                    }
                }

                composable("priority_weights") {
                    PriorityWeightsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("account_sync") {
                    AccountSyncScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("hyperfocus_allowlist") {
                    HyperFocusAllowlistRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("privacy_permissions") {
                    PrivacyPermissionsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("app_guide") {
                    AppGuideScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("identity") {
                    IdentityScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("log_time") {
                    LogTimeScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Parity route: "focus_v2/{taskId}" with optional openWoop and mode query parameters
                composable(
                    route = "focus_v2/{taskId}?openWoop={openWoop}&mode={mode}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("openWoop") { type = NavType.BoolType; defaultValue = false },
                        navArgument("mode") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val openWoop = backStackEntry.arguments?.getBoolean("openWoop") ?: false
                    FocusScreenV2(
                        initialOpenWoop = openWoop,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId ->
                            if (nextTaskId.isNotBlank()) {
                                val currentDestId = navController.currentDestination?.id
                                navController.navigate("focus_v2/${android.net.Uri.encode(nextTaskId)}") {
                                    if (currentDestId != null) {
                                        popUpTo(currentDestId) {
                                            inclusive = true
                                        }
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }

                // Backward-compatible fallback for legacy focus route
                composable(
                    route = "focus/{taskId}?skipped={skipped}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("skipped") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    FocusScreenV2(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId ->
                            if (nextTaskId.isNotBlank()) {
                                val currentDestId = navController.currentDestination?.id
                                navController.navigate("focus_v2/${android.net.Uri.encode(nextTaskId)}") {
                                    if (currentDestId != null) {
                                        popUpTo(currentDestId) {
                                            inclusive = true
                                        }
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable("mindmap_canvas") {
                    val mindmapViewModel: MindmapViewModel = hiltViewModel()
                    MindmapCanvasScreen(
                        viewModel = mindmapViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onTaskClick = { taskId ->
                            if (taskId.isNotBlank()) {
                                navController.navigate("focus_v2/${android.net.Uri.encode(taskId)}") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }

                    // Global Proactive Check-In & Energy Wellness Nudge Overlay (Hub screens only)
                    SmartAssistantOverlayHost(
                        state = assistantState,
                        visible = showBottomBar && !showToolsSheet,
                        onLogCheckIn = { type, ts, detail, hrs ->
                            smartAssistantViewModel.logCheckInAtTime(type, ts, detail, hrs)
                        },
                        onSnoozeCheckIn = { type, target ->
                            smartAssistantViewModel.snoozeCheckInUntil(type, target)
                        },
                        onSkipCheckIn = { type ->
                            smartAssistantViewModel.skipCheckInToday(type)
                        },
                        onDismissCheckIn = { type ->
                            smartAssistantViewModel.dismissCheckInTemporarily(type)
                        },
                        onDrinkWater = {
                            smartAssistantViewModel.logHydrationDrink()
                        },
                        onOpenEasyWins = {
                            flowViewModel.setGroupingMode(FlowGroupingMode.ENERGY_DEMAND)
                            if (currentRoute != NavHubsV2.Flow.route) {
                                navController.navigate(NavHubsV2.Flow.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onDismissWellnessNudge = {
                            smartAssistantViewModel.dismissWellnessNudge()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }

    if (showToolsSheet) {
        ToolsSheet(
            onDismiss = { showToolsSheet = false },
            onNavigate = { route ->
                showToolsSheet = false
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        )
    }
}
}

private data class ToolHubEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tools = remember {
        listOf(
            ToolHubEntry("mindmap_canvas", "Mind Map", Icons.Default.AccountTree, Color(0xFF818CF8)),
            ToolHubEntry("notes", "Notes", Icons.AutoMirrored.Filled.Notes, Color(0xFF38BDF8)),
            ToolHubEntry("weekly_schedule", "Weekly Plan", Icons.Default.DateRange, Color(0xFF10B981)),
            ToolHubEntry("sleep_logs", "Sleep Logs", Icons.Default.Bedtime, Color(0xFFA78BFA)),
            ToolHubEntry("meq_quiz", "Chronotype", Icons.Default.WbSunny, Color(0xFFF59E0B)),
            ToolHubEntry("analytics", "Analytics", Icons.Default.Analytics, Color(0xFF06B6D4)),
            ToolHubEntry("history", "History", Icons.Default.History, Color(0xFF34D399)),
            ToolHubEntry("log_time", "Log Time", Icons.Default.Schedule, Color(0xFF60A5FA)),
            ToolHubEntry("identity", "Objectives", Icons.Default.Flag, Color(0xFFFBBF24)),
            ToolHubEntry("hyperfocus_allowlist", "HyperFocus", Icons.Default.Shield, Color(0xFFE11D48)),
            ToolHubEntry("app_guide", "App Guide", Icons.Default.MenuBook, Color(0xFF94A3B8)),
            ToolHubEntry("settings", "Settings", Icons.Default.Settings, Color(0xFFCBD5E1))
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuroFlowPaletteV2.SurfaceCard,
        contentColor = NeuroFlowPaletteV2.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("tools_sheet_grid"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tools & Subsystems",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Text(
                text = "Jump directly to planning canvases, sleep calibration, focus telemetry, or app settings.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            tools.chunked(3).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTools.forEach { entry ->
                        Surface(
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, entry.accent.copy(alpha = 0.28f)),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("tools_item_${entry.route}")
                                .clickable { onNavigate(entry.route) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = entry.label,
                                    tint = entry.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = NeuroFlowPaletteV2.TextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    repeat(3 - rowTools.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

