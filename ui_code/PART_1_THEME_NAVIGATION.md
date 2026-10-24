# neuroFlow UI Codebase — Theme Engine, Design Tokens, Typography & Navigation Hubs

**Description:** Foundational theme palette (Obsidian, AMOLED, Light), dynamic tokens, typography, and root navigation topology.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 7  
**Lines of Code:** 1,521  
**Uncompressed Size:** 59.8 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`](#appsrcmainjavacomneuroflowapppresentationcommonthemecolorkt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt`](#appsrcmainjavacomneuroflowapppresentationcommonthemethemekt)
- [`app/src/main/java/com/neuroflow/app/presentation/common/theme/Type.kt`](#appsrcmainjavacomneuroflowapppresentationcommonthemetypekt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NavHubsV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2navigationnavhubsv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2navigationneuroflowappv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowComponentsV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2themeneuroflowcomponentsv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2themeneuroflowthemev2kt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationcommonthemecolorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`
- **Lines of Code:** 54
- **Size:** 1,832 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.presentation.common.theme

import androidx.compose.ui.graphics.Color

object NeuroFlowColors {
    // Primary
    val Purple          = Color(0xFF5C5FA8)
    val PurpleLight     = Color(0xFFEEEEFF)
    val PurpleDark      = Color(0xFF3D3F7A)
    val LightBackground = Color(0xFFF5F3EE)
    val LightSurface    = Color(0xFFFFFFFF)
    val LightSurfaceAlt = Color(0xFFF0F2F8)
    val LightText       = Color(0xFF1B1F2A)
    val LightTextMuted  = Color(0xFF5F6B7A)
    val LightBorder     = Color(0xFFE3E7EF)

    // Quadrant colors
    val DoFirstBg       = Color(0xFF1C1F28)
    val DoFirstText     = Color(0xFFF43F5E)
    val DoFirstTextDark = Color(0xFFF43F5E)
    val ScheduleBg      = Color(0xFFDFF4E6)
    val ScheduleText    = Color(0xFF2E7D32)
    val ScheduleTextDark = Color(0xFF81C784)
    val DelegateBg      = Color(0xFFFFF1C7)
    val DelegateText    = Color(0xFFF57F17)
    val DelegateTextDark = Color(0xFFFFB74D)
    val EliminateBg     = Color(0xFFE8EDF5)
    val EliminateText   = Color(0xFF424242)

    // Focus mode background
    val FocusBg         = Color(0xFFE8F5E9)

    // Status colors
    val DeadlineCard    = Color(0xFFFFEBEE)
    val ScheduledCard   = Color(0xFFE3F2FD)
    val PomodoroCard    = Color(0xFFF3E5F5)

    // Tracking badges
    val TrackingRed     = Color(0xFFF43F5E)
    val LockedGray      = Color(0xFF424242)

    // MAPE color grades
    val MapeGood        = Color(0xFF2E7D32)
    val MapeMedium      = Color(0xFF1565C0)
    val MapeBad         = Color(0xFFF59E0B)

    // Dark theme colors
    val DarkBackground  = Color(0xFF121212)
    val DarkSurface     = Color(0xFF1E1E1E)
    val DarkSurfaceAlt  = Color(0xFF2C2C2C)
    val DarkText        = Color(0xFFE1E1E1)
    val DarkTextMuted   = Color(0xFFA0A0A0)
    val DarkBorder      = Color(0xFF383838)
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonthemethemekt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt`
- **Lines of Code:** 115
- **Size:** 4,194 bytes (4.1 KB)

```kotlin
package com.neuroflow.app.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = NeuroFlowColors.Purple,
    onPrimary = Color.White,
    primaryContainer = NeuroFlowColors.PurpleLight,
    onPrimaryContainer = NeuroFlowColors.PurpleDark,
    secondary = NeuroFlowColors.ScheduleText,
    onSecondary = Color.White,
    secondaryContainer = NeuroFlowColors.ScheduleBg,
    onSecondaryContainer = NeuroFlowColors.ScheduleText,
    tertiary = NeuroFlowColors.DelegateText,
    onTertiary = Color.White,
    tertiaryContainer = NeuroFlowColors.DelegateBg,
    onTertiaryContainer = NeuroFlowColors.DelegateText,
    error = NeuroFlowColors.DoFirstText,
    onError = Color.White,
    errorContainer = NeuroFlowColors.DoFirstBg,
    onErrorContainer = NeuroFlowColors.DoFirstText,
    background = NeuroFlowColors.LightBackground,
    onBackground = NeuroFlowColors.LightText,
    surface = NeuroFlowColors.LightSurface,
    onSurface = NeuroFlowColors.LightText,
    surfaceVariant = NeuroFlowColors.LightSurfaceAlt,
    onSurfaceVariant = NeuroFlowColors.LightTextMuted,
    outline = NeuroFlowColors.LightTextMuted,
    outlineVariant = NeuroFlowColors.LightBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = NeuroFlowColors.Purple,
    onPrimary = Color.White,
    primaryContainer = NeuroFlowColors.PurpleDark,
    onPrimaryContainer = NeuroFlowColors.PurpleLight,
    secondary = NeuroFlowColors.ScheduleTextDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F291E),
    onSecondaryContainer = NeuroFlowColors.ScheduleTextDark,
    tertiary = NeuroFlowColors.DelegateTextDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2E2207),
    onTertiaryContainer = NeuroFlowColors.DelegateTextDark,
    error = NeuroFlowColors.DoFirstTextDark,
    onError = Color.Black,
    errorContainer = Color(0xFF381519),
    onErrorContainer = NeuroFlowColors.DoFirstTextDark,
    background = NeuroFlowColors.DarkBackground,
    onBackground = NeuroFlowColors.DarkText,
    surface = NeuroFlowColors.DarkSurface,
    onSurface = NeuroFlowColors.DarkText,
    surfaceVariant = NeuroFlowColors.DarkSurfaceAlt,
    onSurfaceVariant = NeuroFlowColors.DarkTextMuted,
    outline = NeuroFlowColors.DarkTextMuted,
    outlineVariant = NeuroFlowColors.DarkBorder,
)

@Composable
fun NeuroFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (amoledDark && darkTheme) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
    } else {
        baseColorScheme
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeuroFlowTypography,
            content = content
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationcommonthemetypekt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/common/theme/Type.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/common/theme/Type.kt`
- **Lines of Code:** 20
- **Size:** 825 bytes (0.8 KB)

```kotlin
package com.neuroflow.app.presentation.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NeuroFlowTypography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge     = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge      = TextStyle(fontSize = 16.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp),
    labelSmall     = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2navigationnavhubsv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NavHubsV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NavHubsV2.kt`
- **Lines of Code:** 17
- **Size:** 732 bytes (0.7 KB)

```kotlin
package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavHubsV2(val route: String, val title: String, val icon: ImageVector) {
    data object Flow : NavHubsV2("flow_v2", "Flow", Icons.Default.Today)
    data object Plan : NavHubsV2("plan_v2", "Plan", Icons.Default.CalendarMonth)
    data object Insights : NavHubsV2("insights_v2", "Vault", Icons.Default.TrendingUp)

    companion object {
        val items = listOf(Flow, Plan, Insights)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2navigationneuroflowappv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`
- **Lines of Code:** 400
- **Size:** 18,946 bytes (18.5 KB)

```kotlin
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
    amoledDark: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route.orEmpty()
    val showBottomBar = remember(currentRoute) {
        !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("hyperfocus_v2") && !currentRoute.startsWith("focus/")
    }

    LaunchedEffect(initialTaskId) {
        if (!initialTaskId.isNullOrBlank()) {
            navController.navigate("focus_v2/$initialTaskId")
            onInitialTaskConsumed()
        }
    }

    LaunchedEffect(initialWoopTaskId) {
        if (!initialWoopTaskId.isNullOrBlank()) {
            navController.navigate("focus_v2/$initialWoopTaskId?openWoop=true")
            onInitialWoopConsumed()
        }
    }

    LaunchedEffect(initialGuideOpen) {
        if (initialGuideOpen) {
            navController.navigate("app_guide")
            onInitialGuideConsumed()
        }
    }

    LaunchedEffect(initialScheduleOpen) {
        if (initialScheduleOpen) {
            navController.navigate(NavHubsV2.Plan.route)
            onInitialScheduleConsumed()
        }
    }

    val isAmoled = darkTheme && (amoledDark || MaterialTheme.colorScheme.background == Color.Black)
    val activeFocusSessionState by FocusService.activeSession.collectAsState()
    val session = activeFocusSessionState
    val showMiniFocusBar = session != null && !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("focus/")

    NeuroFlowThemeV2(darkTheme = darkTheme, amoledDark = isAmoled) {
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
                    if (showMiniFocusBar && session != null) {
                        MiniFocusSessionBar(
                            remainingSeconds = session.remainingSeconds,
                            totalSeconds = session.totalSeconds,
                            taskTitle = session.taskTitle,
                            onTap = {
                                navController.navigate("focus_v2/${session.taskId}")
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
                        onNavigateToSettings = { navController.navigate("settings") },
                        onStartFocus = { taskId -> navController.navigate("focus_v2/$taskId") },
                        onNavigateToMEQ = { navController.navigate("meq_quiz") },
                        onNavigateToSleepLogs = { navController.navigate("sleep_logs") },
                        onNavigateToNotes = { navController.navigate("notes") }
                    )
                }

                composable(NavHubsV2.Plan.route) {
                    PlanScreenV2(
                        onNavigateToFocus = { taskId -> navController.navigate("focus_v2/$taskId") },
                        onNavigateToWeeklySchedule = { navController.navigate("weekly_schedule") },
                        onNavigateToNotes = { navController.navigate("notes") },
                        viewModel = planViewModel
                    )
                }

                composable(NavHubsV2.Insights.route) {
                    InsightsScreenV2(
                        onNavigateToMEQ = { navController.navigate("meq_quiz") },
                        onNavigateToSleepLogs = { navController.navigate("sleep_logs") },
                        onNavigateToAnalytics = { navController.navigate("analytics") },
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToIdentity = { navController.navigate("identity") }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPriorityWeights = { navController.navigate("priority_weights") },
                        onNavigateToHyperFocusAllowlist = { navController.navigate("hyperfocus_allowlist") },
                        onNavigateToAppGuide = { navController.navigate("app_guide") },
                        onNavigateToPrivacyPermissions = { navController.navigate("privacy_permissions") },
                        onNavigateToMEQQuiz = { navController.navigate("meq_quiz") },
                        onNavigateToSleepLogs = { navController.navigate("sleep_logs") },
                        onNavigateToAccountSync = { navController.navigate("account_sync") },
                        onNavigateToLogTime = { navController.navigate("log_time") }
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
                        onNavigateToFocus = { taskId -> navController.navigate("focus_v2/$taskId") }
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
                        Box(modifier = Modifier.padding(innerPadding)) {
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
                                    IconButton(onClick = { navController.navigate("log_time") }) {
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
                        Box(modifier = Modifier.padding(innerPadding)) {
                            HistoryScreen(
                                hideTopBar = true,
                                onNavigateToLogTime = { navController.navigate("log_time") }
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
                            navController.navigate("focus_v2/$nextTaskId") {
                                popUpTo(NavHubsV2.Flow.route)
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
                            navController.navigate("focus_v2/$nextTaskId") {
                                popUpTo(NavHubsV2.Flow.route)
                            }
                        }
                    )
                }
            }
        }
    }
}
}

```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2themeneuroflowcomponentsv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowComponentsV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowComponentsV2.kt`
- **Lines of Code:** 541
- **Size:** 21,494 bytes (21.0 KB)

```kotlin
package com.neuroflow.app.presentation.v2.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.inference.InferredTaskMetadata
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.service.EnergyPhase
import kotlin.math.roundToInt

object NeuroFlowPaletteV2 {
    val BackgroundDeep: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.background
    val SurfaceCard: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surface
    val SurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surfaceElevated
    val BorderSubtle: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.border.copy(alpha = 0.55f)

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textTertiary

    val PeakAmber: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyModerateAmber
    val ReboundCyan: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.primaryIndigo
    val DipIndigo: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.hyperFocusViolet
    val WindDownSlate: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textSecondary
    val RecoveryRose: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyLowCoral
    val OptimalEmerald: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyPeakMint

    @Composable
    @ReadOnlyComposable
    fun accentForPhase(phase: EnergyPhase): Color = when (phase) {
        EnergyPhase.PEAK -> OptimalEmerald
        EnergyPhase.REBOUND -> ReboundCyan
        EnergyPhase.DIP -> PeakAmber
        EnergyPhase.WIND_DOWN -> DipIndigo
        EnergyPhase.RECOVERY_NEEDED -> RecoveryRose
    }

    /**
     * Top-lit specular border brush that makes cards look tactile in Dark, AMOLED, and Light themes.
     */
    @Composable
    @ReadOnlyComposable
    fun specularBorderBrush(accent: Color = BorderSubtle, alphaBoost: Float = 1f): Brush {
        val isDark = LocalNeuroFlowPalette.current.isDark
        val topHighlight = if (isDark) {
            accent.copy(alpha = (0.42f * alphaBoost).coerceIn(0f, 1f))
        } else {
            accent.copy(alpha = (0.28f * alphaBoost).coerceIn(0f, 1f))
        }
        val bottomShadow = BorderSubtle.copy(alpha = if (isDark) 0.16f else 0.35f)
        return Brush.verticalGradient(listOf(topHighlight, bottomShadow))
    }
}

/**
 * 1. Phase-Adaptive Ambient Backdrop
 */
@Composable
fun PhaseAmbientScreenContainer(
    phase: EnergyPhase,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalNeuroFlowPalette.current.isDark
    val targetAccent by animateColorAsState(
        targetValue = NeuroFlowPaletteV2.accentForPhase(phase),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "phase_ambient_color"
    )

    val topAlpha = if (isDark) 0.14f else 0.08f
    val midAlpha = if (isDark) 0.04f else 0.02f

    Box(
        modifier = modifier
            .background(NeuroFlowPaletteV2.BackgroundDeep)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        targetAccent.copy(alpha = topAlpha),
                        targetAccent.copy(alpha = midAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 760f
                )
            )
    ) {
        content()
    }
}

/**
 * 2. Daily Cognitive Capacity Meter
 */
@Composable
fun CognitiveBudgetMeterCard(
    scheduledMinutesToday: Int,
    recommendedCapacityMinutes: Int,
    conflictCount: Int = 0,
    onAutoBalanceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeCapacity = recommendedCapacityMinutes.coerceAtLeast(60)
    val ratio = (scheduledMinutesToday.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 1.25f)
    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "capacity_progress"
    )

    val isOverbooked = scheduledMinutesToday > safeCapacity
    val barColor = when {
        isOverbooked || conflictCount > 0 -> NeuroFlowPaletteV2.RecoveryRose
        ratio >= 0.85f -> NeuroFlowPaletteV2.PeakAmber
        else -> NeuroFlowPaletteV2.OptimalEmerald
    }

    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .border(1.dp, NeuroFlowPaletteV2.specularBorderBrush(barColor, 0.75f), shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOverbooked || conflictCount > 0) Icons.Default.WarningAmber else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOverbooked) "COGNITIVE OVERLOAD RISK" else "DAILY COGNITIVE BUDGET",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.TextSecondary,
                    letterSpacing = 0.9.sp
                )
            }
            Text(
                text = "${scheduledMinutesToday}m / ${safeCapacity}m",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.75f), barColor)
                        )
                    )
            )
        }

        if ((isOverbooked || conflictCount > 0) && onAutoBalanceClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val warningText = when {
                    conflictCount > 0 -> "$conflictCount schedule overlap(s) detected."
                    else -> "Booked ${scheduledMinutesToday - safeCapacity}m past your high-focus budget."
                }
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = barColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, barColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onAutoBalanceClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = barColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Auto-Rebalance",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Live NLP Inference Token Strip
 */
@Composable
fun LiveNlpInferenceStrip(
    inferred: InferredTaskMetadata,
    rawInputNotEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = rawInputNotEmpty,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCalibrated = !inferred.isDurationExplicit && kotlin.math.abs(inferred.calibrationFactorApplied - 1.0f) >= 0.08f
            val durationLabel = if (isCalibrated) {
                "⏱ ${inferred.durationMinutes}m (${(inferred.calibrationFactorApplied * 100).roundToInt()}% calibrated)"
            } else {
                "⏱ ${inferred.durationMinutes}m"
            }
            NlpTokenChip(
                text = durationLabel,
                accent = NeuroFlowPaletteV2.ReboundCyan
            )

            val (energyText, energyColor) = when (inferred.energyDemand) {
                EnergyLevel.HIGH -> "⚡ High Energy" to NeuroFlowPaletteV2.PeakAmber
                EnergyLevel.MEDIUM -> "⚡ Med Energy" to NeuroFlowPaletteV2.ReboundCyan
                EnergyLevel.LOW -> "⚡ Low Energy" to NeuroFlowPaletteV2.OptimalEmerald
            }
            NlpTokenChip(text = energyText, accent = energyColor)

            if (inferred.isFrog) {
                NlpTokenChip(text = "🐸 Daily Frog", accent = NeuroFlowPaletteV2.PeakAmber)
            }

            if (inferred.priority == Priority.HIGH) {
                NlpTokenChip(text = "🔥 P1 High", accent = NeuroFlowPaletteV2.RecoveryRose)
            }

            inferred.contextTags.forEach { tag ->
                NlpTokenChip(text = "#$tag", accent = NeuroFlowPaletteV2.TextSecondary)
            }
        }
    }
}

@Composable
private fun NlpTokenChip(
    text: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

/**
 * 4. Persistent Mini-Focus Session Bar with Live Progress Track & Breathing Pulse
 */
@Composable
fun MiniFocusSessionBar(
    remainingSeconds: Long,
    totalSeconds: Long,
    taskTitle: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = (remainingSeconds / 60).coerceAtLeast(0)
    val secs = (remainingSeconds % 60).coerceAtLeast(0)
    val timeFormatted = "%02d:%02d".format(mins, secs)
    val progress = if (totalSeconds > 0) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    MiniFocusSessionBar(
        activeTaskTitle = taskTitle,
        remainingTimeFormatted = timeFormatted,
        currentMicroStepTitle = null,
        progress = progress,
        onReturnToFocusClick = onTap,
        modifier = modifier
    )
}

@Composable
fun MiniFocusSessionBar(
    activeTaskTitle: String,
    remainingTimeFormatted: String,
    currentMicroStepTitle: String?,
    onReturnToFocusClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    val shape = RoundedCornerShape(18.dp)

    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = shape,
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.OptimalEmerald, 0.9f)
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(shape)
            .clickable { onReturnToFocusClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.28f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.OptimalEmerald)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = activeTaskTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentMicroStepTitle.isNullOrBlank()) {
                            Text(
                                text = "Step: $currentMicroStepTitle",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowPaletteV2.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = remainingTimeFormatted,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Return to Focus",
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        NeuroFlowPaletteV2.ReboundCyan,
                                        NeuroFlowPaletteV2.OptimalEmerald
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2themeneuroflowthemev2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`
- **Lines of Code:** 374
- **Size:** 13,229 bytes (12.9 KB)

```kotlin
package com.neuroflow.app.presentation.v2.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme

@Immutable
data class NeuroFlowSemanticPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primaryIndigo: Color,
    val energyPeakMint: Color,
    val energyModerateAmber: Color,
    val energyLowCoral: Color,
    val hyperFocusViolet: Color
)

val DarkSemanticPalette = NeuroFlowSemanticPalette(
    isDark = true,
    background = Color(0xFF0A0B0E),
    surface = Color(0xFF13151B),
    surfaceElevated = Color(0xFF1C2029),
    border = Color(0xFF2A2E3B),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    primaryIndigo = Color(0xFF6366F1),
    energyPeakMint = Color(0xFF10B981),
    energyModerateAmber = Color(0xFFF59E0B),
    energyLowCoral = Color(0xFFF43F5E),
    hyperFocusViolet = Color(0xFF8B5CF6)
)

val AmoledSemanticPalette = DarkSemanticPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0D12),
    surfaceElevated = Color(0xFF141720),
    border = Color(0xFF222634),
    primaryIndigo = Color(0xFF818CF8),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF78869B)
)

val LightSemanticPalette = NeuroFlowSemanticPalette(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    border = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF64748B),
    primaryIndigo = Color(0xFF4F46E5),
    energyPeakMint = Color(0xFF059669),
    energyModerateAmber = Color(0xFFD97706),
    energyLowCoral = Color(0xFFE11D48),
    hyperFocusViolet = Color(0xFF7C3AED)
)

val LocalNeuroFlowPalette = staticCompositionLocalOf { DarkSemanticPalette }

/**
 * Extended semantic color tokens for ADR-008 UI/UX components.
 */
@Immutable
data class NeuroFlowExtendedColors(
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val borderMuted: Color,
    val energyPeak: Color,
    val energyPeakContainer: Color,
    val energyModerate: Color,
    val energyModerateContainer: Color,
    val energyLow: Color,
    val energyLowContainer: Color,
    val energyRecovery: Color,
    val energyRecoveryContainer: Color,
    val energyDepleted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val glowColor: Color,
)

val DarkExtendedColors = NeuroFlowExtendedColors(
    surfaceElevated = Color(0xFF161A22),
    surfaceSubtle = Color(0xFF0F1218),
    borderMuted = Color(0xFF21262D),
    energyPeak = Color(0xFF00E699),
    energyPeakContainer = Color(0xFF003820),
    energyModerate = Color(0xFFFFB800),
    energyModerateContainer = Color(0xFF382800),
    energyLow = Color(0xFFFF6B6B),
    energyLowContainer = Color(0xFF381010),
    energyRecovery = Color(0xFF818CF8),
    energyRecoveryContainer = Color(0xFF1E1B4B),
    energyDepleted = Color(0xFF6B7280),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textTertiary = Color(0xFF484F58),
    glowColor = Color(0x3300E699),
)

val AmoledExtendedColors = DarkExtendedColors.copy(
    surfaceSubtle = Color(0xFF000000),
    surfaceElevated = Color(0xFF0B0D12),
    borderMuted = Color(0xFF222634),
)

val LightExtendedColors = NeuroFlowExtendedColors(
    surfaceElevated = Color(0xFFF6F8FA),
    surfaceSubtle = Color(0xFFFFFFFF),
    borderMuted = Color(0xFFD0D7DE),
    energyPeak = Color(0xFF059669),
    energyPeakContainer = Color(0xFFD1FAE5),
    energyModerate = Color(0xFFD97706),
    energyModerateContainer = Color(0xFFFEF3C7),
    energyLow = Color(0xFFE11D48),
    energyLowContainer = Color(0xFFFFE4E6),
    energyRecovery = Color(0xFF6366F1),
    energyRecoveryContainer = Color(0xFFEEF2FF),
    energyDepleted = Color(0xFF9CA3AF),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76),
    textTertiary = Color(0xFF8C959F),
    glowColor = Color(0x1A059669),
)

val LocalNeuroFlowColors = staticCompositionLocalOf { DarkExtendedColors }

fun phaseAccentColor(phase: EnergyPhase, colors: NeuroFlowExtendedColors): Color = when (phase) {
    EnergyPhase.PEAK -> colors.energyPeak
    EnergyPhase.REBOUND -> colors.energyPeak.copy(alpha = 0.85f)
    EnergyPhase.DIP -> colors.energyModerate
    EnergyPhase.WIND_DOWN -> colors.energyDepleted
    EnergyPhase.RECOVERY_NEEDED -> colors.energyRecovery
}

@Composable
@ReadOnlyComposable
fun phaseAccentColor(phase: EnergyPhase): Color = phaseAccentColor(phase, LocalNeuroFlowColors.current)

val NeuroFlowTypographyV2 = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Reactive semantic token bridge: existing V2 composables referencing NeuroFlowThemeV2Colors
 * automatically adapt to Light, Dark, and AMOLED modes without breaking call sites.
 */
object NeuroFlowThemeV2Colors {
    // Static base tokens (for non-composable JVM tests, DrawScopes, or outside CompositionLocal)
    val StaticObsidian = Color(0xFF0A0B0E)
    val StaticSlateSurface = Color(0xFF13151B)
    val StaticElectricIndigo = Color(0xFF6366F1)
    val StaticTextPrimary = Color(0xFFF8FAFC)
    val StaticTextSecondary = Color(0xFF94A3B8)

    val current: NeuroFlowSemanticPalette
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current

    val Obsidian: Color
        @Composable @ReadOnlyComposable get() = current.background
    val SlateSurface: Color
        @Composable @ReadOnlyComposable get() = current.surface
    val SlateSurfaceAlt: Color
        @Composable @ReadOnlyComposable get() = current.surfaceElevated
    val SlateBorder: Color
        @Composable @ReadOnlyComposable get() = current.border
    val SlateDivider: Color
        @Composable @ReadOnlyComposable get() = current.border.copy(alpha = 0.6f)

    // Bioluminescent Accents
    val ElectricIndigo: Color
        @Composable @ReadOnlyComposable get() = current.primaryIndigo
    val ElectricIndigoDark = Color(0xFF4F46E5)
    val ElectricIndigoGlow: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyRecovery

    val BioMint: Color
        @Composable @ReadOnlyComposable get() = current.energyPeakMint
    val SageGreen: Color
        @Composable @ReadOnlyComposable get() = current.energyPeakMint
    val SageGreenDark = Color(0xFF059669)
    val SageGreenContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyPeakContainer

    val SolarAmber: Color
        @Composable @ReadOnlyComposable get() = current.energyModerateAmber
    val WarmAmber: Color
        @Composable @ReadOnlyComposable get() = current.energyModerateAmber
    val WarmAmberContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyModerateContainer

    val PulseCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val MutedCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val RoseCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val RoseCoralContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyLowContainer
    val DeepViolet: Color
        @Composable @ReadOnlyComposable get() = current.hyperFocusViolet

    // Neutral Typography
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = current.textTertiary
}

@Composable
fun NeuroFlowThemeV2(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledDark: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicSupported = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val palette = when {
        !darkTheme -> LightSemanticPalette
        amoledDark -> AmoledSemanticPalette
        else -> DarkSemanticPalette
    }

    val extendedColors = when {
        !darkTheme -> LightExtendedColors
        amoledDark -> AmoledExtendedColors
        else -> DarkExtendedColors
    }

    val defaultDark = darkColorScheme(
        primary = palette.primaryIndigo,
        onPrimary = Color.White,
        primaryContainer = palette.surfaceElevated,
        onPrimaryContainer = palette.textPrimary,
        secondary = palette.energyPeakMint,
        onSecondary = Color.Black,
        secondaryContainer = if (amoledDark) Color(0xFF042F2E) else Color(0xFF064E3B),
        onSecondaryContainer = Color(0xFF6EE7B7),
        tertiary = palette.energyModerateAmber,
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF78350F),
        onTertiaryContainer = Color(0xFFFDE68A),
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        error = palette.energyLowCoral,
        onError = Color.White
    )

    val defaultLight = lightColorScheme(
        primary = palette.primaryIndigo,
        onPrimary = Color.White,
        primaryContainer = palette.surfaceElevated,
        onPrimaryContainer = palette.primaryIndigo,
        secondary = palette.energyPeakMint,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1FAE5),
        onSecondaryContainer = Color(0xFF065F46),
        tertiary = palette.energyModerateAmber,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF92400E),
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        error = palette.energyLowCoral,
        onError = Color.White
    )

    val colorScheme = when {
        dynamicSupported && darkTheme -> dynamicDarkColorScheme(context)
        dynamicSupported && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> defaultDark
        else -> defaultLight
    }

    CompositionLocalProvider(
        LocalNeuroFlowPalette provides palette,
        LocalNeuroFlowColors provides extendedColors,
        LocalIsDarkTheme provides darkTheme // LocalIsDarkTheme provides true
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeuroFlowTypographyV2,
            content = content
        )
    }
}
```

---

