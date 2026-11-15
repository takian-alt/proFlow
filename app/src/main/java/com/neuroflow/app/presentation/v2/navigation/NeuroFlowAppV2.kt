package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.neuroflow.app.presentation.v2.flow.FlowScreenV2
import com.neuroflow.app.presentation.v2.focus.FocusScreenV2
import com.neuroflow.app.presentation.v2.insights.InsightsScreenV2
import com.neuroflow.app.presentation.v2.plan.PlanScreenV2
import com.neuroflow.app.presentation.v2.plan.PlanViewModelV2
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.presentation.v2.theme.MiniFocusSessionBar
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2
import com.neuroflow.app.presentation.v2.theme.PhaseAmbientScreenContainer
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import com.neuroflow.app.presentation.v2.mindmap.MindmapCanvasScreen
import com.neuroflow.app.presentation.v2.mindmap.MindmapViewModel

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

    val isAmoled = darkTheme && (amoledDark || MaterialTheme.colorScheme.background == Color.Black)
    val activeFocusSessionState by FocusService.activeSession.collectAsState()
    val session = activeFocusSessionState
    val showMiniFocusBar = session != null && !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("focus/")

    NeuroFlowThemeV2(darkTheme = darkTheme, amoledDark = isAmoled, dynamicColor = dynamicColor) {
        val flowViewModel: FlowViewModelV2 = hiltViewModel()
        val planViewModel: PlanViewModelV2 = hiltViewModel()
        val flowState by flowViewModel.uiState.collectAsState()
        val now = System.currentTimeMillis()
        val livePhase = flowState.energyTimeline.minByOrNull {
            kotlin.math.abs(it.timestampMillis - now)
        }?.phase ?: flowState.circadianSummary?.currentSnapshot?.phase ?: EnergyPhase.PEAK

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                androidx.compose.foundation.layout.Column {
                    if (showMiniFocusBar) {
                        val currentSession = session ?: return@Column
                        MiniFocusSessionBar(
                            remainingSeconds = currentSession.remainingSeconds,
                            totalSeconds = currentSession.totalSeconds,
                            taskTitle = currentSession.taskTitle,
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            NavHubsV2.items.forEach { hub ->
                                val selected = currentDestination?.hierarchy?.any { it.route == hub.route } == true
                                NavigationBarItem(
                                    icon = { Icon(hub.icon, contentDescription = hub.title) },
                                    label = { Text(hub.title) },
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
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
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
                        onNavigateToMindmap = { navController.navigate("mindmap_canvas") { launchSingleTop = true } },
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
                        onNavigateToMindmap = { navController.navigate("mindmap_canvas") { launchSingleTop = true } },
                        viewModel = planViewModel
                    )
                }

                composable(NavHubsV2.Insights.route) {
                    InsightsScreenV2(
                        onNavigateToMEQ = { navController.navigate("meq_quiz") { launchSingleTop = true } },
                        onNavigateToSleepLogs = { navController.navigate("sleep_logs") { launchSingleTop = true } },
                        onNavigateToAnalytics = { navController.navigate("analytics") { launchSingleTop = true } },
                        onNavigateToHistory = { navController.navigate("history") { launchSingleTop = true } },
                        onNavigateToIdentity = { navController.navigate("identity") { launchSingleTop = true } }
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

                // Parity route: "focus_v2/{taskId}" with optional openWoop query parameter
                composable(
                    route = "focus_v2/{taskId}?openWoop={openWoop}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("openWoop") { type = NavType.BoolType; defaultValue = false }
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
        }
    }
}
}

