package com.neuroflow.app.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neuroflow.app.presentation.analytics.AnalyticsScreen
import com.neuroflow.app.presentation.focus.FocusScreen
import com.neuroflow.app.presentation.focus.MiniWoopReflectionScreen
import com.neuroflow.app.presentation.history.HistoryScreen
import com.neuroflow.app.presentation.identity.IdentityScreen
import com.neuroflow.app.presentation.logtime.LogTimeScreen
import com.neuroflow.app.presentation.matrix.MatrixScreen
import com.neuroflow.app.presentation.matrix.QuadrantDetailScreen
import com.neuroflow.app.presentation.onboarding.AppGuideScreen
import com.neuroflow.app.presentation.schedule.ScheduleScreen
import com.neuroflow.app.presentation.settings.SettingsScreen
import com.neuroflow.app.presentation.settings.SleepLogsScreen
import com.neuroflow.app.presentation.settings.MEQQuizScreen
import com.neuroflow.app.presentation.settings.PrivacyPermissionsScreen
import com.neuroflow.app.presentation.settings.PriorityWeightsScreen
import com.neuroflow.app.presentation.settings.AccountSyncScreen
import com.neuroflow.app.feature.hyperfocus.screens.AllowlistSettingsScreen
import com.neuroflow.app.feature.hyperfocus.screens.HyperFocusActivationSheet
import com.neuroflow.app.feature.hyperfocus.screens.RewardsScreen
import com.neuroflow.app.presentation.notes.NotesScreen
import com.neuroflow.app.presentation.matrix.OnHoldPlanningScreen
import com.neuroflow.app.presentation.schedule.WeeklyScheduleScreen
import com.neuroflow.app.presentation.today.TodayScreen
import com.neuroflow.app.presentation.plan.PlanScreen
import com.neuroflow.app.presentation.progress.ProgressScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Today : Screen("today", "Today", Icons.Filled.Today)
    data object Plan : Screen("plan", "Plan", Icons.Filled.CalendarMonth)
    data object Progress : Screen("progress", "Progress", Icons.Filled.TrendingUp)

    data object Matrix : Screen("matrix", "Matrix", Icons.Filled.GridView)
    data object Schedule : Screen("schedule", "Schedule", Icons.Filled.CalendarMonth)
    data object History : Screen("history", "History", Icons.Filled.History)
    data object Analytics : Screen("analytics", "Analytics", Icons.Filled.Analytics)
    data object QuadrantDetail : Screen("quadrant/{quadrantName}", "Quadrant", null)
    data object Focus : Screen("focus/{taskId}?skipped={skipped}", "Focus", null)
    data object Settings : Screen("settings", "Settings", null)
    data object SleepLogs : Screen("sleep_logs", "Sleep Logs", null)
    data object MEQQuiz : Screen("meq_quiz", "Chronotype Quiz", null)
    data object PriorityWeights : Screen("priority_weights", "Priority Weights", null)
    data object PrivacyPermissions : Screen("privacy_permissions", "Privacy & Permissions", null)
    data object AppGuide : Screen("app_guide", "App Guide", null)
    data object LogTime : Screen("log_time", "Log Time", Icons.Filled.Timer)
    data object Identity : Screen("identity", "Identity", Icons.Filled.Psychology)
    data object HyperFocusAllowlist : Screen("hyperfocus_allowlist", "HyperFocus", null)
    data object Rewards : Screen("rewards", "Rewards", null)
    data object MiniWoop : Screen("mini_woop/{taskId}", "WOOP Reflection", null)
    data object AccountSync : Screen("account_sync", "Account & Cloud Sync", null)
    data object Notes : Screen("notes", "Notes", Icons.AutoMirrored.Filled.Notes)
    data object OnHold : Screen("on_hold", "On Hold / Planning", Icons.Filled.PauseCircle)
    data object WeeklySchedule : Screen("weekly_schedule", "Weekly Schedule", Icons.Filled.CalendarViewWeek)
}

val bottomNavItems = listOf(
    Screen.Today,
    Screen.Plan,
    Screen.Progress,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuroFlowApp(
    initialTaskId: String? = null,
    initialWoopTaskId: String? = null,
    initialGuideOpen: Boolean = false,
    initialNewTaskOpen: Boolean = false,
    initialScheduleOpen: Boolean = false,
    onInitialTaskConsumed: () -> Unit = {},
    onInitialWoopTaskConsumed: () -> Unit = {},
    onInitialGuideConsumed: () -> Unit = {},
    onInitialNewTaskConsumed: () -> Unit = {},
    onInitialScheduleConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()
    val drawerViewModel: DrawerViewModel = hiltViewModel()
    val prefs by drawerViewModel.preferences.collectAsState()
    val activeTasks by drawerViewModel.activeTasks.collectAsState()
    var showGuidePrompt by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(prefs?.userGuidePromptShown) {
        if (prefs != null && !prefs!!.userGuidePromptShown) {
            showGuidePrompt = true
        }
    }

    // Edit dialog state: null = closed, YEARLY/WEEKLY = open for that period
    var editingPeriod by remember { mutableStateOf<GoalPeriod?>(null) }

    // Handle initial deep links from activity intents.
    androidx.compose.runtime.LaunchedEffect(initialTaskId, initialWoopTaskId, initialGuideOpen) {
        if (initialGuideOpen) {
            navController.navigate(Screen.AppGuide.route) {
                launchSingleTop = true
            }
            onInitialGuideConsumed()
        } else if (initialWoopTaskId != null) {
            navController.navigate("mini_woop/$initialWoopTaskId") {
                launchSingleTop = true
            }
            onInitialWoopTaskConsumed()
        } else if (initialTaskId != null) {
            navController.navigate("focus/$initialTaskId") {
                // Don't add to back stack if we're already on the start destination
                launchSingleTop = true
            }
            onInitialTaskConsumed()
        }
    }

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    // The new-task request can land on any screen, but only Today, Plan and Matrix open the sheet.
    // Without this hop the flag stays set and nothing happens until the user navigates back there.
    androidx.compose.runtime.LaunchedEffect(initialNewTaskOpen) {
        if (!initialNewTaskOpen) return@LaunchedEffect
        val sheetHosts = setOf(Screen.Today.route, Screen.Plan.route, Screen.Matrix.route)
        val alreadyOnHost = currentDestination?.hierarchy?.any { it.route in sheetHosts } == true
        if (!alreadyOnHost) {
            navController.navigate(Screen.Today.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // A protected block tapped from the launcher asks for the day it belongs to.
    androidx.compose.runtime.LaunchedEffect(initialScheduleOpen) {
        if (!initialScheduleOpen) return@LaunchedEffect
        navController.navigate(Screen.Schedule.route) {
            launchSingleTop = true
        }
        onInitialScheduleConsumed()
    }

    // Edit goals dialog
    editingPeriod?.let { period ->
        val existing = if (period == GoalPeriod.YEARLY) prefs?.yearlyGoals ?: emptyList()
                       else prefs?.weeklyGoals ?: emptyList()
        AlertDialog(
            onDismissRequest = { editingPeriod = null },
            title = { Text(if (period == GoalPeriod.YEARLY) "Yearly Goals" else "Weekly Goals") },
            text = {
                GoalsEditContent(
                    goals = existing,
                    onSave = { goals ->
                        if (period == GoalPeriod.YEARLY) drawerViewModel.saveYearlyGoals(goals)
                        else drawerViewModel.saveWeeklyGoals(goals)
                        editingPeriod = null
                    }
                )
            },
            confirmButton = {}
        )
    }

    if (showGuidePrompt) {
        AlertDialog(
            onDismissRequest = {
                showGuidePrompt = false
                drawerViewModel.markUserGuidePromptShown()
            },
            title = { Text("Welcome to proFlow") },
            text = {
                Text("Want a 2-minute walkthrough? The User Guide gives a quick setup checklist and a simple daily workflow.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showGuidePrompt = false
                    drawerViewModel.markUserGuidePromptShown()
                    navController.navigate(Screen.AppGuide.route)
                }) { Text("Open Guide") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGuidePrompt = false
                    drawerViewModel.markUserGuidePromptShown()
                }) { Text("Maybe later") }
            }
        )
    }

    if (showMoreMenuSheet) {
        MoreMenuSheet(
            onDismiss = { showMoreMenuSheet = false },
            onNavigate = { route -> navController.navigate(route) }
        )
    }
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route || (screen == Screen.Schedule && it.route?.startsWith("schedule") == true) } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
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
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Today.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Today.route) {
                    TodayScreen(
                        onOpenMoreMenu = { showMoreMenuSheet = true },
                        onNavigateToPlan = {
                            navController.navigate(Screen.Plan.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        },
                        onNavigateToSchedule = { dateMillis ->
                            navController.navigate("schedule?date=$dateMillis") {
                                launchSingleTop = true
                            }
                        },
                        initialNewTaskOpen = initialNewTaskOpen,
                        onInitialNewTaskConsumed = onInitialNewTaskConsumed
                    )
                }
                composable(Screen.Plan.route) {
                    PlanScreen(
                        onOpenMoreMenu = { showMoreMenuSheet = true },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        },
                        onNavigateToQuadrant = { quadrantName ->
                            navController.navigate("quadrant/$quadrantName")
                        },
                        onNavigateToOnHold = {
                            navController.navigate(Screen.OnHold.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        initialNewTaskOpen = initialNewTaskOpen,
                        onInitialNewTaskConsumed = onInitialNewTaskConsumed
                    )
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(
                        onOpenMoreMenu = { showMoreMenuSheet = true }
                    )
                }
                composable(Screen.Matrix.route) {
                    MatrixScreen(
                        onOpenDrawer = null,
                        onNavigateToQuadrant = { quadrantName ->
                            navController.navigate("quadrant/$quadrantName")
                        },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToOnHold = {
                            navController.navigate(Screen.OnHold.route)
                        },
                        initialNewTaskOpen = initialNewTaskOpen,
                        onInitialNewTaskConsumed = onInitialNewTaskConsumed
                    )
                }
                composable(
                    route = "schedule?date={date}",
                    arguments = listOf(
                        navArgument("date") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    val dateArg = backStackEntry.arguments?.getLong("date") ?: -1L
                    ScheduleScreen(
                        initialDateMillis = if (dateArg > 0L) dateArg else null,
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        }
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen()
                }
                composable(Screen.WeeklySchedule.route) {
                    WeeklyScheduleScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDay = { dateMillis ->
                            navController.navigate("schedule?date=$dateMillis") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        }
                    )
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen()
                }
                composable(
                    route = Screen.QuadrantDetail.route,
                    arguments = listOf(navArgument("quadrantName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val quadrantName = backStackEntry.arguments?.getString("quadrantName") ?: "DO_FIRST"
                    QuadrantDetailScreen(
                        quadrantName = quadrantName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        }
                    )
                }
                composable(
                    route = "focus/{taskId}?skipped={skipped}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("skipped") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                    val skipped = backStackEntry.arguments?.getString("skipped") ?: ""
                    FocusScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId, newSkipped ->
                            navController.navigate("focus/$nextTaskId?skipped=$newSkipped") {
                                popUpTo("focus/$taskId") { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPriorityWeights = {
                            navController.navigate(Screen.PriorityWeights.route)
                        },
                        onNavigateToHyperFocusAllowlist = {
                            navController.navigate(Screen.HyperFocusAllowlist.route)
                        },
                        onNavigateToAppGuide = {
                            navController.navigate(Screen.AppGuide.route)
                        },
                        onNavigateToPrivacyPermissions = {
                            navController.navigate(Screen.PrivacyPermissions.route)
                        },
                        onNavigateToMEQQuiz = {
                            navController.navigate(Screen.MEQQuiz.route)
                        },
                        onNavigateToSleepLogs = {
                            navController.navigate(Screen.SleepLogs.route)
                        },
                        onNavigateToAccountSync = {
                            navController.navigate(Screen.AccountSync.route)
                        }
                    )
                }

                composable(Screen.SleepLogs.route) {
                    SleepLogsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.MEQQuiz.route) {
                    MEQQuizScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.PriorityWeights.route) {
                    PriorityWeightsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.PrivacyPermissions.route) {
                    PrivacyPermissionsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.HyperFocusAllowlist.route) {
                    HyperFocusAllowlistRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Rewards.route) {
                    RewardsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.AppGuide.route) {
                    AppGuideScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.LogTime.route) {
                    LogTimeScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Identity.route) {
                    IdentityScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.MiniWoop.route,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType })
                ) {
                    MiniWoopReflectionScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.AccountSync.route) {
                    AccountSyncScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Notes.route) {
                    NotesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.OnHold.route) {
                    OnHoldPlanningScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToFocus = { taskId ->
                            navController.navigate("focus/$taskId")
                        }
                    )
                }
            }
        }
}

@Composable
private fun GoalsEditContent(
    goals: List<String>,
    onSave: (List<String>) -> Unit
) {
    // Capture initial values only — don't reset if parent recomposes with new prefs while editing
    val edited = remember {
        androidx.compose.runtime.mutableStateListOf(
            goals.getOrElse(0) { "" },
            goals.getOrElse(1) { "" },
            goals.getOrElse(2) { "" }
        )
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column {
        edited.forEachIndexed { index, value ->
            OutlinedTextField(
                value = value,
                onValueChange = { edited[index] = it },
                label = { Text("Goal ${index + 1}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = if (index < 2) androidx.compose.ui.text.input.ImeAction.Next
                                else androidx.compose.ui.text.input.ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                )
            )
            if (index < 2) Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { onSave(edited.map { it.trim() }) }) {
                Text("Save")
            }
        }
    }
}

/**
 * The allowlist editor as a destination in the main app's graph. The activation sheet is hosted here
 * rather than inside the editor so the same editor composable can be mounted from HyperFocusActivity,
 * where a session start is a destination rather than a setting.
 */
@Composable
private fun HyperFocusAllowlistRoute(onNavigateBack: () -> Unit) {
    val viewModel: com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel = hiltViewModel()
    var showActivationSheet by remember { mutableStateOf(false) }

    AllowlistSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onStartSession = { showActivationSheet = true }
    )
    if (showActivationSheet) {
        HyperFocusActivationSheet(
            viewModel = viewModel,
            onDismiss = { showActivationSheet = false }
        )
    }
}
