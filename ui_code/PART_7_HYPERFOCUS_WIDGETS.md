# neuroFlow UI Codebase — HyperFocus Lock Engine, Gateways & Home Screen Widgets

**Description:** HyperFocusHomeScreen/Activity, BlockingOverlay, GateTaskScreen, PermissionSetup, and Glance App Widgets.  
**Generated for Vice Head Review:** 2026-10-20 16:59:40  
**Files:** 26  
**Lines of Code:** 4,560  
**Uncompressed Size:** 184.9 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushyperfocusactivitykt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusViewModel.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushyperfocusviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusgategatetaskenginekt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusgategatetaskscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GatewayScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusgategatewayscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/EndSessionGateScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushomeendsessiongatescreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomeactivitykt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomescreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeViewModel.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomeviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/AllowlistSettingsScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreensallowlistsettingsscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/BlockingOverlayScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreensblockingoverlayscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/CodeEntrySection.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreenscodeentrysectionkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HomeRoleSetupScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreenshomerolesetupscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HyperFocusActivationSheet.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreenshyperfocusactivationsheetkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PermissionSetupScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreenspermissionsetupscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PlanningPromptScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreensplanningpromptscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardSection.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreensrewardsectionkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardsScreen.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocusscreensrewardsscreenkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/AllowlistEditor.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocussurfaceallowlisteditorkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/FocusSurfaceModel.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocussurfacefocussurfacemodelkt)
- [`app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/SystemExempt.kt`](#appsrcmainjavacomneuroflowappfeaturehyperfocussurfacesystemexemptkt)
- [`app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskReceiver.kt`](#appsrcmainjavacomneuroflowapppresentationwidgetcompletetaskreceiverkt)
- [`app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt`](#appsrcmainjavacomneuroflowapppresentationwidgetcompletetaskwidgetactionkt)
- [`app/src/main/java/com/neuroflow/app/presentation/widget/FocusWidgetProvider.kt`](#appsrcmainjavacomneuroflowapppresentationwidgetfocuswidgetproviderkt)
- [`app/src/main/java/com/neuroflow/app/presentation/widget/NextActionWidgetProvider.kt`](#appsrcmainjavacomneuroflowapppresentationwidgetnextactionwidgetproviderkt)
- [`app/src/main/java/com/neuroflow/app/presentation/widget/QuickCaptureWidgetProvider.kt`](#appsrcmainjavacomneuroflowapppresentationwidgetquickcapturewidgetproviderkt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushyperfocusactivitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt`
- **Lines of Code:** 179
- **Size:** 8,138 bytes (7.9 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskScreen
import com.neuroflow.app.feature.hyperfocus.gate.GatewayScreen
import com.neuroflow.app.feature.hyperfocus.screens.AllowlistSettingsScreen
import com.neuroflow.app.feature.hyperfocus.screens.BlockingOverlayScreen
import com.neuroflow.app.feature.hyperfocus.screens.HyperFocusActivationSheet
import com.neuroflow.app.feature.hyperfocus.screens.PlanningPromptScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class HyperFocusActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    // Read by the composition, so a reused instance can show the app the newest intent named.
    private var blockedPackage by mutableStateOf("")

    override fun onResume() {
        super.onResume()
        DeviceOwnerKioskManager.syncLockTaskMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The enforcer starts this activity with SINGLE_TOP, so an open gate or gateway does not get
        // torn down when a bounce arrives — it gets a new intent. Without setIntent the composition
        // keeps reading the original extras and shows the wrong app, or the wrong screen, indefinitely.
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val controller = navController ?: return
        controller.navigate(intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_OVERLAY) {
            popUpTo(controller.graph.id) { inclusive = true }
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntent(intent)

        // Block back gesture on Android 13+ only on the blocking overlay
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
            ) {
                val currentRoute = navController?.currentBackStackEntry?.destination?.route
                if (currentRoute == DESTINATION_OVERLAY) {
                    // Consume back gesture on blocking overlay so user can't return to blocked app
                } else {
                    if (navController?.popBackStack() == false) {
                        finish()
                    }
                }
            }
        }

        setContent {
            val controller = rememberNavController().also { navController = it }
            val startDestination =
                intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_OVERLAY
            val viewModel: HyperFocusViewModel = hiltViewModel()
            var showActivationSheet by remember { mutableStateOf(false) }

            // The gate logs which app it was opened for, and the log row is written by a screen two
            // navigations away. Naming the app here, before anything is rendered, is what survives it.
            // Unconditional: a bounce always names the app it bounced, and the black home names
            // nothing — leaving the previous bounce's name in the store would log a pass for an app
            // the user never asked for.
            if (startDestination == DESTINATION_GATE) {
                LaunchedEffect(blockedPackage) { viewModel.beginGate(blockedPackage) }
            }

            NavHost(navController = controller, startDestination = startDestination) {
                composable(DESTINATION_OVERLAY) {
                    BlockingOverlayScreen(blockedPackage, viewModel, controller)
                }
                composable(DESTINATION_PLANNING) {
                    PlanningPromptScreen(
                        viewModel = viewModel,
                        onDismiss = {
                            controller.popBackStack()
                        }
                    )
                }
                composable(DESTINATION_GATE) {
                    GateTaskScreen(
                        viewModel = viewModel,
                        onPassed = { controller.navigate(DESTINATION_GATEWAY) },
                        onBack = { finish() }
                    )
                }
                composable(DESTINATION_GATEWAY) {
                    GatewayScreen(
                        viewModel = viewModel,
                        // Straight into the app: the route would need a field to carry the package the
                        // gateway chose, and launchApp already finishes this activity.
                        onLaunch = { packageName -> launchApp(packageName) },
                        onBack = { finish() }
                    )
                }
                composable(DESTINATION_LAUNCH) {
                    // Transparent screen — immediately launches the granted app and finishes
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    LaunchedEffect(Unit) {
                        launchApp(blockedPackage)
                    }
                }
                // The same editor the More menu opens, reached with no session and no launcher to hand
                // off to — the state where it is the only useful thing on the screen. Back is finish():
                // the gesture callback above already eats the system back.
                composable(DESTINATION_SETUP) {
                    AllowlistSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() },
                        onStartSession = { showActivationSheet = true }
                    )
                }
            }
            if (showActivationSheet) {
                HyperFocusActivationSheet(
                    viewModel = viewModel,
                    onDismiss = { showActivationSheet = false }
                )
            }
        }
    }

    fun launchApp(packageName: String) {
        if (packageName.isBlank()) {
            finish()
            return
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        }
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_START_DESTINATION = "start_destination"

        // No destination string literal outside this companion: a typo in a route name is a blank
        // screen at runtime, not a compile error. SETUP is named here and routed by Task 16.
        const val DESTINATION_OVERLAY = "blocking_overlay"
        const val DESTINATION_PLANNING = "planning_prompt"
        const val DESTINATION_GATE = "gate"
        const val DESTINATION_GATEWAY = "gateway"
        const val DESTINATION_LAUNCH = "launch_app"
        const val DESTINATION_SETUP = "setup"
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushyperfocusviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusViewModel.kt`
- **Lines of Code:** 462
- **Size:** 20,498 bytes (20.0 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.AllowlistMigrationRunner
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateGrantSource
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.ActivationDecision
import com.neuroflow.app.feature.hyperfocus.domain.ActivationRejection
import com.neuroflow.app.feature.hyperfocus.domain.RewardEngine
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.domain.decideActivation
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import com.neuroflow.app.feature.hyperfocus.surface.AllowlistDraft
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import com.neuroflow.app.feature.hyperfocus.surface.applyDraft
import com.neuroflow.app.feature.hyperfocus.surface.editableApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HyperFocusProgress(
    val completedSinceActivation: Int,
    val totalTarget: Int,
    val currentTier: RewardTier,
    val fraction: Float,
    val tasksToNextTier: Int
)

@HiltViewModel
class HyperFocusViewModel @Inject constructor(
    private val hyperFocusManager: HyperFocusManager,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val unlockCodeRepository: UnlockCodeRepository,
    private val taskRepository: TaskRepository,
    private val gateRequestLogger: GateRequestLogger,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
    private val allowlistMigrationRunner: AllowlistMigrationRunner,
) : ViewModel() {

    val hyperFocusPrefs: StateFlow<HyperFocusPreferences> = hyperFocusDataStore.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HyperFocusPreferences())

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    val progress: StateFlow<HyperFocusProgress> = combine(
        hyperFocusDataStore.flow,
        taskRepository.observeCompletedTasks()
    ) { prefs, completedTasks ->
        if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) {
            return@combine HyperFocusProgress(
                completedSinceActivation = 0,
                totalTarget = 0,
                currentTier = RewardTier.NONE,
                fraction = 0f,
                tasksToNextTier = 0
            )
        }

        val completedSinceActivation = if (prefs.lockedTaskIds.isNotEmpty()) {
            completedTasks.count { it.id in prefs.lockedTaskIds }
                .coerceAtLeast(0)
        } else {
            (completedTasks.size - prefs.tasksCompletedAtActivation).coerceAtLeast(0)
        }
        val fraction = completedSinceActivation.toFloat() / prefs.dailyTaskTarget.coerceAtLeast(1)
        val currentTier = RewardEngine.computeTier(completedSinceActivation, prefs.dailyTaskTarget, prefs.emergencyUsed)
        val tasksToNextTier = RewardEngine.tasksToNextTier(completedSinceActivation, prefs.dailyTaskTarget, prefs.emergencyUsed)
        HyperFocusProgress(
            completedSinceActivation = completedSinceActivation,
            totalTarget = prefs.dailyTaskTarget,
            currentTier = currentTier,
            fraction = fraction.coerceIn(0f, 1f),
            tasksToNextTier = tasksToNextTier
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HyperFocusProgress(0, 0, RewardTier.NONE, 0f, 1)
        )

    val isPassActive: StateFlow<Boolean> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.isPassActive(prefs, System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val passSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.passSecondsRemaining(prefs, System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessionSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        if (!prefs.isActive || prefs.sessionMode != HyperFocusSessionMode.TIME_BASED) {
            return@combine null
        }
        val endsAt = prefs.sessionEndsAtMillis ?: return@combine null
        ((endsAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val claimedCodeToShow: MutableStateFlow<String?> = MutableStateFlow(null)
    // Which tier the currently shown code belongs to
    val claimedCodeTier: MutableStateFlow<RewardTier?> = MutableStateFlow(null)

    // Emits the result of the last submitCode call so the UI can react
    val submitCodeResult: MutableStateFlow<com.neuroflow.app.feature.hyperfocus.domain.UnlockResult?> =
        MutableStateFlow(null)

    // Count of unused codes per tier — refreshed whenever prefs change
    val rewardCounts: MutableStateFlow<Map<RewardTier, Int>> = MutableStateFlow(emptyMap())

    init {
        viewModelScope.launch {
            hyperFocusDataStore.flow.collect { prefs ->
                val sessionId = prefs.sessionId ?: return@collect
                val counts = RewardTier.entries
                    .filter { it != RewardTier.NONE }
                    .associateWith { tier ->
                        unlockCodeRepository.countUnusedByTier(sessionId, tier)
                    }
                rewardCounts.value = counts
            }
        }

        viewModelScope.launch {
            combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs -> prefs }
                .collect { prefs ->
                    if (!prefs.isActive || prefs.sessionMode != HyperFocusSessionMode.TIME_BASED) return@collect
                    val endsAt = prefs.sessionEndsAtMillis ?: return@collect
                    if (endsAt <= System.currentTimeMillis()) {
                        hyperFocusManager.deactivate()
                    }
                }
        }
    }

    val lockoutSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        val expiresAt = prefs.lockoutExpiresAt ?: return@combine null
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        if (remaining > 0) remaining else null
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepository.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTaskCount: StateFlow<Int> = taskRepository.observeActiveTasks()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- Gate, gateway, emergency (spec 5.2 and 5.3) ----

    /**
     * One engine for the whole ViewModel, so the task the screen renders is the task the answer is
     * checked against, and a rotation re-reads the same stimulus instead of rolling a new one.
     */
    val gateEngine = GateTaskEngine()
    val gateTask = MutableStateFlow(gateEngine.newTask())

    private val _gatePassed = MutableStateFlow(false)
    val gatePassed: StateFlow<Boolean> = _gatePassed

    private data class GatewayCatalog(
        val entries: List<AppCatalogRepository.AppEntry> = emptyList(),
        val exempt: Set<String> = emptySet()
    )

    /** Swept once per ViewModel, not once per tick: both of these are cross-process calls. */
    private val gatewayCatalog = MutableStateFlow(GatewayCatalog())

    /**
     * Hidden = launchable and not in the resolved allowlist. The exempt set is read with the catalog
     * because it is stable for the life of this screen, while the pass underneath the allowlist is
     * not — so the filtering re-runs on every prefs change and the sweeping never does.
     */
    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> = combine(
        hyperFocusDataStore.flow,
        gatewayCatalog
    ) { prefs, catalog ->
        if (!prefs.isActive) return@combine emptyList()
        val keep = FocusSurfaceModel.resolvedAllowlist(
            prefs, catalog.exempt, BuildConfig.APPLICATION_ID, System.currentTimeMillis()
        )
        catalog.entries.filter { it.packageName !in keep }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * E1: an unspent budget keeps ticking whether or not an app holds it, which is exactly why this
     * is not `passSecondsRemaining` — that reads null the moment the granted app leaves the
     * foreground, and the gateway needs to say how much of the window the code bought is left.
     */
    val gateBudgetSecondsRemaining: StateFlow<Long?> = combine(
        tickerFlow,
        hyperFocusDataStore.flow
    ) { _, prefs ->
        val now = System.currentTimeMillis()
        prefs.activePassExpiresAt?.takeIf { it > now }?.let { (it - now) / 1000 }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed && it.sessionMode != HyperFocusSessionMode.TIME_BASED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val entries = appCatalogRepository.launchableApps()
            val exempt = systemExempt.packages()
            gatewayCatalog.value = GatewayCatalog(entries, exempt)
        }
    }

    /** Whoever opens the gate names the app it was opened for; the log row outlives the navigation. */
    fun beginGate(packageName: String) {
        viewModelScope.launch {
            hyperFocusDataStore.update {
                it.copy(pendingGatePackage = packageName.takeIf { name -> name.isNotBlank() })
            }
        }
    }

    /**
     * Returns the decision synchronously, before the logging coroutine runs, so the screen can tally
     * a failed attempt on the click that failed it. Reading `gatePassed` instead would race the
     * launch and count a pass as a mismatch.
     */
    fun submitGateAnswer(answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long): Boolean {
        val task = gateTask.value
        val passed = gateEngine.checkAnswer(task, answer) || gateEngine.shouldAutoPass(mismatchCount)
        val willLog = passed && gateEngine.allowsSubmit(elapsedMillis, reason)
        viewModelScope.launch {
            if (willLog) {
                val prefs = hyperFocusDataStore.current()
                gateRequestLogger.log(
                    packageName = prefs.pendingGatePackage.orEmpty(),
                    statedReason = reason.trim(),
                    taskType = task.type.name,
                    mismatchCount = mismatchCount,
                    elapsedMillis = elapsedMillis,
                    grantSource = GateGrantSource.REWARD_CODE
                )
            }
            _gatePassed.value = willLog
        }
        return willLog
    }

    /** The gate is single-shot: consuming it shuts the route and rolls the next visit's stimulus. */
    fun consumeGatePass() {
        _gatePassed.value = false
        gateTask.value = gateEngine.newTask()
    }

    fun grantPassFor(packageName: String, onGranted: () -> Unit = {}) {
        viewModelScope.launch {
            // No fallback tier. Minutes are only ever what a code already bought (E1), so with no
            // budget there is nothing to spend and the gateway has to stay shut. The deadline is read
            // from the store, not from gateBudgetSecondsRemaining: that flow only carries a value
            // while a screen is subscribed to it, and an unsubscribed read would refuse a live budget.
            val now = System.currentTimeMillis()
            val budgetSeconds = hyperFocusDataStore.current().activePassExpiresAt
                ?.takeIf { it > now }
                ?.let { (it - now) / 1000 }
                ?: return@launch
            val minutes = (budgetSeconds / 60).coerceAtLeast(1L).toInt()
            hyperFocusManager.grantPass(packageName, minutes)
            // Launch on the written state, not on the request: the enforcer reads the store, so an
            // app opened before the grant lands gets bounced straight back to this screen.
            if (hyperFocusDataStore.current().activePassPackage == packageName) onGranted()
        }
    }

    fun grantEmergencyFor(packageName: String) {
        viewModelScope.launch {
            val before = hyperFocusDataStore.current()
            hyperFocusManager.triggerEmergencyBypass(packageName)
            // The manager refuses a second emergency in a session; logging a grant that never
            // happened would make the audit row claim an access the user did not get.
            if (before.emergencyUsed) return@launch
            gateRequestLogger.logEmergencyGrant(packageName)
        }
    }

    val selfPackage: String = BuildConfig.APPLICATION_ID

    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage.asStateFlow()

    fun clearActivationMessage() {
        _activationMessage.value = null
    }

    fun saveAllowlist(essential: Set<String>, allowed: Set<String>) {
        viewModelScope.launch {
            hyperFocusDataStore.update { applyDraft(it, AllowlistDraft(essential, allowed)) }
        }
    }

    /** The editor lists what the user may pin; `STATIC` keeps the machinery off that list but leaves
     *  the dialers on it. */
    suspend fun loadEditableApps(): List<AppCatalogRepository.AppEntry> {
        val all = appCatalogRepository.launchableApps()
        val keep = editableApps(
            all = all.map { it.packageName }.toSet(),
            systemExempt = SystemExempt.STATIC,
            selfPackage = selfPackage
        )
        return all.filter { it.packageName in keep }
    }

    private fun rejectionText(reason: ActivationRejection): String = when (reason) {
        ActivationRejection.AlreadyActive -> "A session is already running."
        ActivationRejection.EmptySurface -> "Pin at least one app as essential first."
    }

    fun activate(selectedTaskIds: Set<String>) {
        viewModelScope.launch {
            // Before the guard, not after: an install whose tiers were never derived would otherwise
            // be refused with "pin at least one app as essential" on a screen that has nothing to pin.
            allowlistMigrationRunner.runIfNeeded()
            val prefs = hyperFocusDataStore.current()
            when (val decision = decideActivation(prefs, SystemExempt.STATIC, selfPackage)) {
                is ActivationDecision.Rejected -> _activationMessage.value = rejectionText(decision.reason)
                ActivationDecision.Allowed -> {
                    val activeIds = taskRepository.getActiveTasks().map { it.id }.toSet()
                    val normalized = selectedTaskIds.intersect(activeIds)
                    if (normalized.isEmpty()) {
                        _activationMessage.value = "Pick at least one task to lock."
                    } else {
                        hyperFocusManager.activate(
                            dailyTaskTarget = normalized.size,
                            lockedTaskIds = normalized
                        )
                    }
                }
            }
        }
    }

    fun activateTimed(durationMinutes: Int) {
        viewModelScope.launch {
            allowlistMigrationRunner.runIfNeeded()
            val prefs = hyperFocusDataStore.current()
            when (val decision = decideActivation(prefs, SystemExempt.STATIC, selfPackage)) {
                is ActivationDecision.Rejected -> _activationMessage.value = rejectionText(decision.reason)
                ActivationDecision.Allowed -> hyperFocusManager.activateTimed(durationMinutes)
            }
        }
    }

    fun addTasksToActiveSession(taskIds: Set<String>) {
        viewModelScope.launch {
            hyperFocusManager.addTasksToSession(taskIds)
        }
    }

    fun claimRewardForTier(tier: RewardTier) {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) return@launch
            val sessionId = prefs.sessionId ?: return@launch
            val currentProgress = progress.value

            val canClaimTier = RewardEngine.isTierEarned(
                tier = tier,
                completedSinceActivation = currentProgress.completedSinceActivation,
                totalTarget = currentProgress.totalTarget,
                emergencyUsed = prefs.emergencyUsed
            )
            if (!canClaimTier) return@launch

            // If there's already a pending code for this same tier, just show it again
            if (prefs.pendingCodeId != null && claimedCodeTier.value == tier) {
                val existing = unlockCodeRepository.getCodeById(prefs.pendingCodeId)
                claimedCodeToShow.value = existing
                return@launch
            }

            // Issue a new code for the requested tier
            val result = unlockCodeRepository.getClaimableCode(sessionId, tier) ?: return@launch
            val (codeId, plaintext) = result

            hyperFocusDataStore.update { it.copy(pendingCodeId = codeId) }
            claimedCodeTier.value = tier
            claimedCodeToShow.value = plaintext

            // Refresh counts
            val counts = RewardTier.entries
                .filter { it != RewardTier.NONE }
                .associateWith { t -> unlockCodeRepository.countUnusedByTier(sessionId, t) }
            rewardCounts.value = counts
        }
    }

    fun dismissClaimedCode() {
        claimedCodeToShow.value = null
        claimedCodeTier.value = null
    }

    fun submitCode(entered: String) {
        viewModelScope.launch {
            val result = hyperFocusManager.submitCode(entered)
            submitCodeResult.value = result
        }
    }

    fun clearSubmitCodeResult() {
        submitCodeResult.value = null
    }

    fun completePlanning(tomorrowTaskTitles: List<String>) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val tomorrowMidnight = cal.timeInMillis
            val now = System.currentTimeMillis()
            
            tomorrowTaskTitles.filter { it.isNotBlank() }.forEach { title ->
                taskRepository.insert(
                    com.neuroflow.app.data.local.entity.TaskEntity(
                        title = title.trim(),
                        scheduledDate = tomorrowMidnight,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            hyperFocusManager.completePlanning()
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusgategatetaskenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt`
- **Lines of Code:** 74
- **Size:** 2,885 bytes (2.8 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import kotlin.random.Random

enum class GateTaskType { TRANSCRIBE, COUNT_BACK, RESTATED }

data class GateTask(
    val type: GateTaskType,
    val prompt: String,
    val expected: List<String>
)

/**
 * Effort-only cognition gate. It can always be passed: mismatches are reported, never fatal,
 * and after three of them the task auto-passes. What it costs is seconds and attention.
 */
class GateTaskEngine(
    private val random: Random = Random.Default
) {
    fun newTask(): GateTask = when (GateTaskType.entries.random(random)) {
        GateTaskType.TRANSCRIBE ->
            single(TRANSCRIPTIONS, "Type exactly what you see", GateTaskType.TRANSCRIBE)
        GateTaskType.RESTATED ->
            single(SENTENCES, "Retype this sentence word for word, including punctuation", GateTaskType.RESTATED)
        GateTaskType.COUNT_BACK -> countBack()
    }

    private fun single(pool: List<String>, instruction: String, type: GateTaskType): GateTask {
        val value = pool[random.nextInt(pool.size)]
        return GateTask(type, "$instruction:\n\n$value", listOf(value))
    }

    private fun countBack(): GateTask {
        var start = random.nextInt(100, 1000)
        while (start % 100 == 0) start = random.nextInt(100, 1000)
        val values = (0 until 4).map { (start - it * 7).toString() }
        return GateTask(
            GateTaskType.COUNT_BACK,
            "Start at $start and count back by 7. Enter the four values, starting at $start.",
            values
        )
    }

    fun checkAnswer(task: GateTask, answer: List<String>): Boolean =
        task.expected.indices.all { i ->
            task.expected[i].trim().equals(answer.getOrNull(i)?.trim().orEmpty(), ignoreCase = true)
        }

    fun isReasonValid(reason: String): Boolean = reason.trim().length >= MIN_REASON_CHARS

    fun shouldAutoPass(mismatchCount: Int): Boolean = mismatchCount >= MAX_MISMATCHES

    fun allowsSubmit(elapsedMillis: Long, reason: String): Boolean =
        isReasonValid(reason) && elapsedMillis >= MIN_ELAPSED_MILLIS

    companion object {
        const val MAX_MISMATCHES = 3
        const val MIN_ELAPSED_MILLIS = 12_000L
        const val MIN_REASON_CHARS = 8

        private val TRANSCRIPTIONS = listOf(
            "quiet desk", "left hinge", "amber frost", "plain route", "cold anchor",
            "dim margin", "slow orbit", "brick valve", "firm slate", "gray punch"
        )
        private val SENTENCES = listOf(
            "The train leaves before the shop opens.",
            "Nine files went to the wrong folder again.",
            "She paid for lunch with the last note in her coat.",
            "A short walk clears more than an hour of staring.",
            "The ladder leaned on the window, not the wall.",
            "Every alarm I keep is one I chose to miss."
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusgategatetaskscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskScreen.kt`
- **Lines of Code:** 222
- **Size:** 8,714 bytes (8.5 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import kotlinx.coroutines.delay

/**
 * The cognitive gate. Nothing here is a puzzle the user can fail: mismatches are tallied, three of
 * them auto-pass, and the only hard floor is twelve seconds of attention plus a written reason.
 */
@Composable
fun GateTaskScreen(
    onPassed: () -> Unit,
    onBack: () -> Unit,
    viewModel: HyperFocusViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val task by viewModel.gateTask.collectAsStateWithLifecycle()
    val passed by viewModel.gatePassed.collectAsStateWithLifecycle()

    // The engine's own auto-pass threshold, read from the single instance this ViewModel checks
    // answers against. A local copy of the number would let the tally's ceiling drift from the
    // condition that actually opens the gate.
    val maxMismatches = GateTaskEngine.MAX_MISMATCHES

    var answers by remember(task) { mutableStateOf(task.expected.map { "" }) }
    var mismatches by remember(task) { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(1000L)
        }
    }

    LaunchedEffect(passed) {
        if (passed) {
            viewModel.consumeGatePass()
            onPassed()
        }
    }

    val holdSeconds = ((GateTaskEngine.MIN_ELAPSED_MILLIS - elapsed) / 1000).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Before you open it",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF333333),
                    contentColor = Color.White
                )
            ) {
                Text("Back", fontSize = 14.sp)
            }
        }

        Text(
            text = task.prompt,
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp))
                .padding(16.dp)
        )

        if (task.expected.size == 1) {
            OutlinedTextField(
                value = answers.firstOrNull().orEmpty(),
                onValueChange = { answers = listOf(it) },
                label = { Text("Your answer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                task.expected.forEachIndexed { index, _ ->
                    OutlinedTextField(
                        value = answers.getOrElse(index) { "" },
                        onValueChange = { value ->
                            answers = answers.mapIndexed { i, old ->
                                if (i == index) value else old
                            }
                        },
                        label = { Text("${index + 1}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Why are you opening this app?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (viewModel.gateEngine.allowsSubmit(elapsedMillis = elapsed, reason = reason)) {
                        viewModel.submitGateAnswer(answers, reason, mismatches, elapsed)
                    }
                }
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A tally, not a bar: filling it never locks the control, because the submit guard reads
            // only elapsed time and the reason. Mismatches open the gate early through
            // GateTaskEngine.shouldAutoPass, which is the ViewModel's decision, not this one.
            Text(
                text = "Mismatches: $mismatches/$maxMismatches",
                color = if (mismatches >= maxMismatches) Color(0xFFFFB74D) else Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
            if (holdSeconds > 0) {
                Text(
                    text = "Hold for $holdSeconds more second${if (holdSeconds == 1L) "" else "s"}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = if (reason.trim().length < GateTaskEngine.MIN_REASON_CHARS)
                        "Write a reason to continue" else "Ready",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
        }

        Button(
            onClick = {
                val submitted = answers
                if (!viewModel.submitGateAnswer(submitted, reason, mismatches, elapsed)) {
                    mismatches += 1
                }
            },
            enabled = viewModel.gateEngine.allowsSubmit(elapsedMillis = elapsed, reason = reason),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color(0xFF777777)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusgategatewayscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GatewayScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GatewayScreen.kt`
- **Lines of Code:** 170
- **Size:** 6,280 bytes (6.1 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.screens.CodeEntrySection

/**
 * The gateway: everything the home screen hides, listed once per ViewModel rather than once per
 * tick, because re-enumerating the launcher's app list is a cross-process call.
 */
@Composable
fun GatewayScreen(
    onLaunch: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HyperFocusViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val budgetSeconds by viewModel.gateBudgetSecondsRemaining.collectAsStateWithLifecycle()
    val prefs by viewModel.hyperFocusPrefs.collectAsStateWithLifecycle()
    val activePackage = prefs.activePassPackage

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Apps behind the gate",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF80CBC4))
            ) {
                Text("Home", fontSize = 14.sp)
            }
        }

        Text(
            text = gatewayBudgetLabel(budgetSeconds),
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp
        )

        if (hiddenApps.isEmpty()) {
            Text(
                text = "Nothing is hidden. Every app you can launch is on the home screen.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )
        } else {
            hiddenApps.forEach { entry ->
                GatewayRow(
                    entry = entry,
                    isActive = entry.packageName == activePackage,
                    onOpen = {
                        viewModel.grantPassFor(entry.packageName) { onLaunch(entry.packageName) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter a reward code",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        CodeEntrySection(
            viewModel = viewModel,
            onCodeAccepted = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * No `enabled` flag tied to the budget: the row is always tappable and the ViewModel refuses when
 * there is nothing to spend. A second source of truth in the UI would disagree with the first the
 * moment the budget expired while this screen was open.
 */
@Composable
private fun GatewayRow(
    entry: AppCatalogRepository.AppEntry,
    isActive: Boolean,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF69F0AE) else Color(0xFF333333),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.label, color = Color.White, fontSize = 15.sp)
            Text(
                text = if (isActive) "Pass granted from here" else "Spends the budget above",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        }
        Button(
            onClick = onOpen,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black
            )
        ) {
            Text(if (isActive) "Again" else "Open")
        }
    }
}

/**
 * The gateway's money line. Reads the unspent budget, which keeps ticking between passes (E1). The
 * FULL tier's deadline is Int.MAX_VALUE minutes away, so a plain minute counter would print
 * 35791394m — that value is a window with no timer, and it is said that way.
 */
internal fun gatewayBudgetLabel(budgetSeconds: Long?): String {
    val seconds =
        budgetSeconds ?: return "No budget yet. Enter a reward code, or use Emergency on the home screen."
    if (seconds >= NO_TIMER_SECONDS) return "Budget remaining: no timer on this pass"
    return "Budget remaining: ${seconds / 60}m ${seconds % 60}s"
}

private const val NO_TIMER_SECONDS = 24L * 60 * 60
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushomeendsessiongatescreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/EndSessionGateScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/EndSessionGateScreen.kt`
- **Lines of Code:** 187
- **Size:** 7,314 bytes (7.1 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.gate.GateTask
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import kotlinx.coroutines.delay

/**
 * The exit gate. It costs the same attention as opening a hidden app, because ending the session
 * removes every block on the device at once. `onSubmit` returns whether the attempt ended the
 * session, so a mismatch is tallied on the click that failed it.
 */
@Composable
fun EndSessionGateScreen(
    task: GateTask,
    onSubmit: (answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answers by remember(task) { mutableStateOf(task.expected.map { "" }) }
    var mismatches by remember(task) { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(1_000L)
        }
    }

    val holdSeconds = ((GateTaskEngine.MIN_ELAPSED_MILLIS - elapsed) / 1000).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Before you end it",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ending the session lifts every block on this device, now and for the rest of " +
                "the time you set aside.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp
        )

        Text(
            text = task.prompt,
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp))
                .padding(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            task.expected.forEachIndexed { index, _ ->
                OutlinedTextField(
                    value = answers.getOrElse(index) { "" },
                    onValueChange = { value ->
                        answers = answers.mapIndexed { i, old -> if (i == index) value else old }
                    },
                    label = { Text("${index + 1}") },
                    singleLine = task.expected.size > 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index == task.expected.lastIndex) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (reasonValid(reason) && elapsed >= GateTaskEngine.MIN_ELAPSED_MILLIS) {
                                if (!onSubmit(answers, reason, mismatches, elapsed)) mismatches += 1
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Why are you ending the session?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mismatches: $mismatches/${GateTaskEngine.MAX_MISMATCHES}",
                color = if (mismatches >= GateTaskEngine.MAX_MISMATCHES) Color(0xFFFFB74D) else Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
            Text(
                text = when {
                    holdSeconds > 0 -> "Hold for $holdSeconds more second${if (holdSeconds == 1L) "" else "s"}"
                    !reasonValid(reason) -> "Write a reason to continue"
                    else -> "Ready"
                },
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
        }

        Button(
            onClick = {
                if (!onSubmit(answers, reason, mismatches, elapsed)) mismatches += 1
            },
            enabled = reasonValid(reason) && elapsed >= GateTaskEngine.MIN_ELAPSED_MILLIS,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color(0xFF777777)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End the session")
        }

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keep it running")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun reasonValid(reason: String): Boolean =
    reason.trim().length >= GateTaskEngine.MIN_REASON_CHARS
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomeactivitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt`
- **Lines of Code:** 148
- **Size:** 6,568 bytes (6.4 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neuroflow.app.feature.hyperfocus.HyperFocusActivity
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class HyperFocusHomeActivity : ComponentActivity() {

    // A field, not a call inside onResume: hiltViewModel() needs the activity's
    // LocalViewModelStoreOwner and has none when invoked from a lifecycle callback body.
    private val viewModel: HyperFocusHomeViewModel by viewModels()
    private val handoffGuard = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resolveHandoff()
        setContent {
            val state by viewModel.state.collectAsState()
            val target by viewModel.handoffTarget.collectAsState()
            val resolved by viewModel.handoffResolved.collectAsState()
            val hiddenApps by viewModel.hiddenApps.collectAsState()
            val emergencyAvailable by viewModel.emergencyAvailable.collectAsState()
            val endTask by viewModel.endGateTask.collectAsState()
            var showEndConfirmation by remember { mutableStateOf(false) }
            // The hand-off is driven off the collected pair, not off onResume: resolveHandoff()
            // is a coroutine, so on a cold start the target arrives after onResume has finished.
            LaunchedEffect(state, target) {
                if (state is HomeSurfaceState.HandOff && target != null) handOffIfAllowed()
            }
            if (showEndConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showEndConfirmation = false },
                    title = {
                        androidx.compose.material3.Text(
                            text = "End HyperFocus Session?",
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    },
                    text = {
                        androidx.compose.material3.Text(
                            text = "Ending the session will deactivate HyperFocus and unblock your apps.",
                            color = androidx.compose.ui.graphics.Color(0xFFAAAAAA)
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showEndConfirmation = false
                                viewModel.endSessionImmediately()
                            }
                        ) {
                            androidx.compose.material3.Text(
                                "End Session",
                                color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)
                            )
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showEndConfirmation = false }
                        ) {
                            androidx.compose.material3.Text(
                                "Cancel",
                                color = androidx.compose.ui.graphics.Color(0xFF80CBC4)
                            )
                        }
                    },
                    containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
                )
            }
            HyperFocusHomeScreen(
                state = state,
                canHandOff = target != null,
                handoffResolved = resolved,
                hiddenApps = hiddenApps,
                emergencyAvailable = emergencyAvailable,
                onOpenEssential = viewModel::open,
                onOpenGate = { openHyperFocus(HyperFocusActivity.DESTINATION_GATE) },
                onEmergencyOpen = viewModel::emergencyOpen,
                onDeactivate = { showEndConfirmation = true },
                onOpenAllowlistEditor = { openHyperFocus(HyperFocusActivity.DESTINATION_SETUP) },
                onOpenHomeAppSettings = viewModel::openHomeAppSettings
            )
        }
    }

    override fun onResume() {
        super.onResume()
        DeviceOwnerKioskManager.syncLockTaskMode(this)
        handoffGuard.set(false)
        viewModel.resolveHandoff()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        handoffGuard.set(false)
    }

    private fun openHyperFocus(startDestination: String) {
        startActivity(
            Intent(this, HyperFocusActivity::class.java)
                .putExtra(HyperFocusActivity.EXTRA_START_DESTINATION, startDestination)
        )
    }

    private fun handOffIfAllowed() {
        val target = viewModel.handoffTarget.value ?: return
        if (target.packageName == packageName) return
        if (!handoffGuard.compareAndSet(false, true)) return
        runCatching {
            // setComponent is load-bearing: a bare HOME intent re-resolves through the
            // persistent-preferred rule, which pins HOME at *this* package, and the hand-off would
            // deliver the intent straight back here and loop.
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            // New task, not clear task: CLEAR_TASK wipes the launcher's stack, so every hand-off
            // cold-starts it and the swap is visible as a rebuild rather than a resume.
            // This window has no business staying behind the launcher it just resumed.
            finish()
        }.onFailure {
            handoffGuard.set(false)
            Log.w(TAG, "Hand-off to ${componentKey(target)} failed", it)
        }
    }

    companion object {
        private const val TAG = "HyperFocusHome"
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomescreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeScreen.kt`
- **Lines of Code:** 276
- **Size:** 10,681 bytes (10.4 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.screens.EmergencyControl

@Composable
fun HyperFocusHomeScreen(
    state: HomeSurfaceState,
    canHandOff: Boolean,
    handoffResolved: Boolean,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    emergencyAvailable: Boolean,
    onOpenEssential: (String) -> Unit,
    onOpenGate: () -> Unit,
    onEmergencyOpen: (String) -> Unit,
    onDeactivate: () -> Unit,
    onOpenAllowlistEditor: () -> Unit,
    onOpenHomeAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        HomeSurfaceState.HandOff ->
            if (showsOffPlaceholder(handoffResolved, canHandOff)) OffPlaceholder(
                onOpenAllowlistEditor = onOpenAllowlistEditor,
                onOpenHomeAppSettings = onOpenHomeAppSettings
            )
            // When a hand-off target exists the activity is already finishing; painting a
            // black frame here only makes the swap visible. While the resolution is still in
            // flight there is nothing true to say, so the frame stays plain.
            else Box(modifier = modifier.fillMaxSize().background(Color.Black))

        is HomeSurfaceState.Active -> ActiveSurface(
            state = state,
            hiddenApps = hiddenApps,
            emergencyAvailable = emergencyAvailable,
            onOpenEssential = onOpenEssential,
            onOpenGate = onOpenGate,
            onEmergencyOpen = onEmergencyOpen,
            onDeactivate = onDeactivate,
            modifier = modifier
        )
    }
}

@Composable
private fun ActiveSurface(
    state: HomeSurfaceState.Active,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    emergencyAvailable: Boolean,
    onOpenEssential: (String) -> Unit,
    onOpenGate: () -> Unit,
    onEmergencyOpen: (String) -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF050505))) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            contentPadding = PaddingValues(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    state = state,
                    hiddenCount = hiddenApps.size,
                    onDeactivate = onDeactivate
                )
            }
            items(state.essentials, key = { it.packageName }) { tile ->
                HomeTileView(tile = tile, onClick = { onOpenEssential(tile.packageName) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Button(
                        onClick = onOpenGate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2E2E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hiddenApps.isEmpty()) "Open the gate"
                            else "Open the gate (${hiddenApps.size} hidden)",
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    EmergencyControl(
                        available = emergencyAvailable,
                        hiddenApps = hiddenApps,
                        onConfirm = onEmergencyOpen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    state: HomeSurfaceState.Active,
    hiddenCount: Int,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = state.passSecondsRemaining
    val remainingLabel = when {
        seconds == null -> "Essentials Only"
        state.passPackage == null -> "${seconds / 60}m ${seconds % 60}s pass"
        else -> "${seconds / 60}m ${seconds % 60}s (${state.passPackage})"
    }
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        com.neuroflow.app.presentation.v2.settings.HyperFocusLauncherStatusHeader(
            sessionModeLabel = "HyperFocus Active",
            remainingTasksOrMinutesLabel = remainingLabel,
            earnedUnlockMinutes = ((seconds ?: 0L) / 60L).toInt(),
            cloudBackupSynced = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (hiddenCount == 0) "No app is hidden right now."
            else "$hiddenCount app${if (hiddenCount == 1) "" else "s"} behind the gate.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedButton(
                onClick = onDeactivate,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
            ) {
                Text("End session")
            }
        }
    }
}

@Composable
private fun HomeTileView(tile: HomeTile, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(tile.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(tile.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = tile.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(56.dp)
            )
        } else {
            // A package uninstalled mid-session throws here, and a throwing icon lookup inside a grid
            // item would crash the whole home rather than the one tile (spec 8, "uninstalled mid-pass").
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = tile.label.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = tile.label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun OffPlaceholder(
    onOpenAllowlistEditor: () -> Unit,
    onOpenHomeAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().background(Color(0xFF050505))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HyperFocus is off",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "No other home app on this device could be found, so this screen is not " +
                    "pretending to be one. Choose your home app, or start a session.",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenHomeAppSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Set my home app")
                }
                Button(
                    onClick = onOpenAllowlistEditor,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E2E2E),
                        contentColor = Color.White
                    )
                ) {
                    Text("Edit allowlist")
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocushomehyperfocushomeviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeViewModel.kt`
- **Lines of Code:** 189
- **Size:** 7,858 bytes (7.7 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateGrantSource
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class HyperFocusHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val hyperFocusManager: HyperFocusManager,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
    private val gateRequestLogger: GateRequestLogger,
    private val realHomeResolver: RealHomeResolver
) : ViewModel() {

    private val tick = MutableStateFlow(System.currentTimeMillis())
    private val labels = MutableStateFlow<Map<String, String>>(emptyMap())
    private val entries = MutableStateFlow<List<AppCatalogRepository.AppEntry>>(emptyList())

    /** One engine, so the stimulus the screen shows is the one the answer is checked against. */
    private val endGateEngine = GateTaskEngine()
    val endGateTask = MutableStateFlow(endGateEngine.newTask())
    private var storedHome: String? = null
    private val _handoffTarget = MutableStateFlow<ComponentName?>(null)
    val handoffTarget: StateFlow<ComponentName?> = _handoffTarget.asStateFlow()

    /** False until a resolution has actually run, so the surface never claims there is no launcher. */
    private val _handoffResolved = MutableStateFlow(false)
    val handoffResolved: StateFlow<Boolean> = _handoffResolved.asStateFlow()

    val state: StateFlow<HomeSurfaceState> =
        combine(hyperFocusDataStore.flow, tick, labels) { prefs, now, known ->
            homeSurfaceStateOf(
                prefs = prefs,
                labels = known,
                // STATIC, not packages(): the grid must hide never-gated machinery, but the dialers
                // are in packages() and must stay visible tiles (spec 6.2 + 6.4).
                systemExempt = SystemExempt.STATIC,
                selfPackage = context.packageName,
                nowMillis = now
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSurfaceState.HandOff)

    init {
        viewModelScope.launch {
            while (isActive) {
                tick.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val all = appCatalogRepository.launchableApps()
            entries.value = all
            labels.value = all.associate { it.packageName to it.label }
        }
    }

    /** Hidden = launchable and not in the resolved allowlist: what the gate and the emergency spend. */
    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> =
        combine(hyperFocusDataStore.flow, entries) { prefs, all ->
            if (!prefs.isActive) emptyList()
            else {
                val keep = FocusSurfaceModel.resolvedAllowlist(
                    prefs, systemExempt.packages(), context.packageName, System.currentTimeMillis()
                )
                all.filter { it.packageName !in keep }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Called from the activity's `onCreate` and `onResume`; cheap and idempotent. */
    fun resolveHandoff() {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            val known = prefs.realHomeComponent
            storedHome = known
            val target = realHomeResolver.handoff(known)
            _handoffTarget.value = target
            _handoffResolved.value = true
            if (known == null && target != null) {
                hyperFocusDataStore.update { it.copy(realHomeComponent = componentKey(target)) }
            }
        }
    }

    fun open(packageName: String) {
        viewModelScope.launch { appCatalogRepository.launch(packageName) }
    }

    /**
     * Ungated by F4, so it is reachable straight from the home — but it is still a grant, and the
     * gateway logs its emergency. A home that grants without writing the row would leave the audit
     * looking like the user never left the black screen.
     */
    fun emergencyOpen(packageName: String) {
        viewModelScope.launch {
            val before = hyperFocusDataStore.current()
            hyperFocusManager.triggerEmergencyBypass(packageName)
            if (!before.emergencyUsed) gateRequestLogger.logEmergencyGrant(packageName)
            appCatalogRepository.launch(packageName)
        }
    }

    /**
     * Ending a session is the largest exit available: it removes every block on the device at once.
     * A tap that did it outright made the whole surface negotiable, so it now asks for the same
     * thing the gateway does — attention and a written reason — and only then lets it through.
     */
    fun submitEndAttempt(
        answer: List<String>,
        reason: String,
        mismatchCount: Int,
        elapsedMillis: Long
    ): Boolean {
        val task = endGateTask.value
        val passed = endGateEngine.checkAnswer(task, answer) || endGateEngine.shouldAutoPass(mismatchCount)
        val willEnd = passed && endGateEngine.allowsSubmit(elapsedMillis, reason)
        if (willEnd) {
            viewModelScope.launch {
                gateRequestLogger.log(
                    packageName = "",
                    statedReason = reason.trim(),
                    taskType = task.type.name,
                    mismatchCount = mismatchCount,
                    elapsedMillis = elapsedMillis,
                    grantSource = GateGrantSource.ABANDONED
                )
                hyperFocusManager.deactivate()
            }
        }
        return willEnd
    }

    fun endSessionImmediately() {
        viewModelScope.launch {
            gateRequestLogger.log(
                packageName = "",
                statedReason = "Direct end session",
                taskType = "DIRECT",
                mismatchCount = 0,
                elapsedMillis = 0,
                grantSource = GateGrantSource.ABANDONED
            )
            hyperFocusManager.deactivate()
        }
    }

    fun openHomeAppSettings() {
        runCatching {
            context.startActivity(
                Intent("android.settings.HOME_SETTINGS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreensallowlistsettingsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/AllowlistSettingsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/AllowlistSettingsScreen.kt`
- **Lines of Code:** 242
- **Size:** 10,241 bytes (10.0 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.domain.ActivationDecision
import com.neuroflow.app.feature.hyperfocus.domain.ActivationRejection
import com.neuroflow.app.feature.hyperfocus.domain.decideActivation
import com.neuroflow.app.feature.hyperfocus.surface.AllowlistTier
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import com.neuroflow.app.feature.hyperfocus.surface.draftFrom
import com.neuroflow.app.feature.hyperfocus.surface.toggleTier

/**
 * The whole allowlist in one screen: what sits on the black home, what is reachable through the gate,
 * and what needs a pass. The edits happen on a draft so the counts at the foot describe the list the
 * user is arranging rather than whatever was saved last.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowlistSettingsScreen(
    viewModel: HyperFocusViewModel,
    onNavigateBack: () -> Unit,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val activationMessage by viewModel.activationMessage.collectAsState()
    var draft by remember { mutableStateOf(draftFrom(prefs)) }
    var edited by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppCatalogRepository.AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    // The prefs flow starts from an empty default and the stored tiers arrive a frame later, so an
    // untouched draft has to follow them — otherwise the editor opens blank over a real allowlist and
    // Save writes it away.
    LaunchedEffect(prefs) { if (!edited) draft = draftFrom(prefs) }

    LaunchedEffect(Unit) { apps = viewModel.loadEditableApps() }

    val trimmed = query.trim()
    val visible = remember(apps, trimmed) {
        val sorted = apps.sortedBy { it.label.lowercase() }
        if (trimmed.isEmpty()) sorted else sorted.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
                it.packageName.contains(trimmed, ignoreCase = true)
        }
    }

    val essentialCount = draft.essential.size
    val allowedCount = draft.allowed.size
    val gatedCount = (apps.size - essentialCount - allowedCount).coerceAtLeast(0)
    val decision = decideActivation(prefs, SystemExempt.STATIC, viewModel.selfPackage)
    // Only the empty surface is actionable from this screen — the user is standing on the editor that
    // fixes it. "Already running" is not a reason to grey out a button on a screen that can't see the
    // session.
    val startBlockedReason = if (decision == ActivationDecision.Rejected(ActivationRejection.EmptySurface))
        "Pin at least one app as essential." else null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("HyperFocus") },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Essential apps stay on the black home. Allowed apps are one gate away. " +
                    "Everything else needs a pass before it will open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(visible, key = { it.packageName }) { app ->
                    AppTierRow(
                        label = app.label,
                        packageName = app.packageName,
                        tier = when (app.packageName) {
                            in draft.essential -> AllowlistTier.Essential
                            in draft.allowed -> AllowlistTier.Allowed
                            else -> null
                        },
                        onTier = { tier ->
                            edited = true
                            draft = toggleTier(draft, app.packageName, tier)
                        }
                    )
                    HorizontalDivider()
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$essentialCount on the home · $allowedCount behind the gate · $gatedCount need a pass",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Exempt system apps and ${viewModel.selfPackage} are always reachable " +
                            "and are not listed here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            activationMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.saveAllowlist(draft.essential, draft.allowed) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Button(
                    onClick = onStartSession,
                    enabled = startBlockedReason == null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Start session") }
            }
            startBlockedReason?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AppTierRow(
    label: String,
    packageName: String,
    tier: AllowlistTier?,
    onTier: (AllowlistTier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AllowlistTier.entries.forEach { candidate ->
            FilterChip(
                selected = tier == candidate,
                onClick = { onTier(candidate) },
                label = { Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreensblockingoverlayscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/BlockingOverlayScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/BlockingOverlayScreen.kt`
- **Lines of Code:** 687
- **Size:** 31,059 bytes (30.3 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.MainActivity
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import androidx.navigation.NavController
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.HyperFocusActivity
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun BlockingOverlayScreen(
    blockedPackage: String,
    viewModel: HyperFocusViewModel,
    navController: NavController
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val isPassActive by viewModel.isPassActive.collectAsState()
    val passSecondsRemaining by viewModel.passSecondsRemaining.collectAsState()
    val sessionSecondsRemaining by viewModel.sessionSecondsRemaining.collectAsState()

    val haptic = LocalHapticFeedback.current
    var showEmergencyDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTimeBasedSession = prefs.sessionMode == HyperFocusSessionMode.TIME_BASED
    
    val appName = remember(blockedPackage) { context.labelForPackage(blockedPackage) }

    // A pass names one app (D3). The card has to name the app the pass holds, not the app the
    // enforcer bounced — those differ the moment a pass is live and the user opens something else.
    val passAppName = remember(prefs.activePassPackage, blockedPackage) {
        context.labelForPackage(prefs.activePassPackage ?: blockedPackage)
    }

    // Check if this is a messenger app
    val isMessengerApp = remember(blockedPackage) {
        val messengerPackages = listOf(
            "com.facebook.orca",
            "com.facebook.mlite",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "org.thoughtcrime.securesms"
        )
        blockedPackage in messengerPackages
    }

    // Supportive focus guidance
    val blockedAppQuotes = listOf(
        "Notice the urge to switch. Take a slow inhale before deciding.",
        "Your goals are meaningful and worthy of this focused block.",
        "This moment builds neural momentum. Stay present.",
        "Your focus is your deepest creative superpower.",
        "Remember why you activated HyperFocus. You are doing important work.",
        "Every mindful choice strengthens cognitive resilience.",
        "Your future self will appreciate your intentionality.",
        "Urges are temporary waves. Ground yourself in your intention.",
        "You are capable of deep, deliberate work.",
        "This boundary protects what truly matters to you today.",
        "A mindful pause is a victory for cognitive clarity.",
        "Your attention is your most valuable cognitive asset.",
        "Reflect on your chosen direction and breathe.",
        "Deep focus creates quiet mastery.",
        "Honoring this boundary preserves your focus state.",
        "Your attention is safe in this sanctuary.",
        "Take a slow breath: Is this switch aligned with your intention?",
        "Mindful pause: choose what aligns best with your core mission."
    )

    val selectedQuote = remember(blockedPackage) {
        blockedAppQuotes.getOrNull(Random.nextInt(maxOf(1, blockedAppQuotes.size))) 
            ?: "Your focus is worth more than this."
    }

    // Animation state
    val lockScale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        lockScale.animateTo(1f, animationSpec = tween(600, easing = LinearEasing))
    }

    val messengerGlow = remember { Animatable(0.5f) }
    LaunchedEffect(isMessengerApp) {
        if (isMessengerApp) {
            while (true) {
                messengerGlow.animateTo(1f, animationSpec = tween(1000))
                messengerGlow.animateTo(0.5f, animationSpec = tween(1000))
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dopamine-Safe HyperFocus Gate Card
            val nextReward = when (progress.currentTier) {
                com.neuroflow.app.domain.model.RewardTier.NONE -> com.neuroflow.app.domain.model.RewardTier.MICRO
                com.neuroflow.app.domain.model.RewardTier.MICRO -> com.neuroflow.app.domain.model.RewardTier.PARTIAL
                com.neuroflow.app.domain.model.RewardTier.PARTIAL -> com.neuroflow.app.domain.model.RewardTier.EARNED
                else -> com.neuroflow.app.domain.model.RewardTier.FULL
            }
            val activeTask = activeTasks.firstOrNull()
            com.neuroflow.app.presentation.v2.settings.DopamineSafeBlockingGateCard(
                blockedAppName = appName,
                completedTasksInSession = progress.completedSinceActivation,
                nextRewardTier = nextReward,
                topTaskTitle = activeTask?.title ?: "Current Focus Goal",
                topTaskFirstStep = "Work on 1 micro-step for just 2 minutes",
                onStartTwoMinuteStepClick = {
                    val preferredTaskId = prefs.lockedTaskIds.firstOrNull { lockedId ->
                        activeTasks.any { it.id == lockedId }
                    } ?: activeTask?.id

                    val focusIntent = Intent(context, MainActivity::class.java).apply {
                        if (!preferredTaskId.isNullOrBlank()) {
                            action = "com.procus.ACTION_OPEN_FOCUS"
                            putExtra("task_id", preferredTaskId)
                        }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(focusIntent)
                    (context as? Activity)?.finish()
                }
            )


            // ============ LARGE MOTIVATIONAL QUOTE (MAIN FOCUS) ============
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                color = if (isMessengerApp) Color(0xFF2D1F1F) else Color(0xFF1A1A2E),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"",
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedQuote,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "\"",
                        color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ============ TOP ALERT (IF MESSENGER) ============
            if (isMessengerApp) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFFF6B6B).copy(alpha = messengerGlow.value),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF2D1F1F)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Messenger Alert",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Messenger Quota Active",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High distraction alert - stay focused!",
                                color = Color(0xFFFF9999),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ============ TAMPER DETECTION WARNING ============
            if (prefs.isTamperDetected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFB71C1C),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF3E1B1B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Tamper Warning",
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Tamper detected",
                                color = Color(0xFFB71C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = prefs.tamperReason ?: "Unusual activity detected",
                                color = Color(0xFFD32F2F),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (isTimeBasedSession) {
                // ============ SESSION TIMER SECTION ============
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Focus Timer",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatSeconds(sessionSecondsRemaining),
                            color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apps stay blocked until this timer ends.",
                            color = Color(0xFFB0B0B0),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ============ PROGRESS SECTION ============
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Today's Progress",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                            trackColor = Color(0xFF2D2D44),
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${progress.completedSinceActivation} / ${progress.totalTarget}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "tasks completed",
                                    color = Color(0xFFB0B0B0),
                                    fontSize = 12.sp
                                )
                            }

                            if (progress.currentTier != RewardTier.FULL) {
                                val tiers = RewardTier.entries
                                val nextIndex = tiers.indexOf(progress.currentTier) + 1
                                val nextTier = if (nextIndex < tiers.size) tiers[nextIndex] else null
                                val unlockLabel = when {
                                    nextTier == null || nextTier == RewardTier.FULL -> "full unlock"
                                    else -> "${nextTier.unlockMinutes}min unlock"
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${progress.tasksToNextTier}",
                                        color = Color(0xFF4A90E2),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "more → $unlockLabel",
                                        color = Color(0xFFB0B0B0),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF1B5E20),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    color = Color(0xFF1B5E20)
                                ) {
                                    Text(
                                        text = "✓ Full Reward",
                                        color = Color(0xFF69F0AE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ============ PASS STATUS (IF ACTIVE) ============
            if (isPassActive) {
                val timeLabel = passSecondsRemaining?.let { secs ->
                    "${secs / 60}m ${secs % 60}s"
                } ?: ""
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color(0xFF69F0AE),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF1B5E20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔓 $passAppName is open",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Only that app. Everything else is still closed.",
                            color = Color(0xFFA5D6A7),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$timeLabel remaining",
                            color = Color(0xFF81C784),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (blockedPackage == prefs.activePassPackage) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(HyperFocusActivity.DESTINATION_LAUNCH)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color(0xFF69F0AE)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open $appName →", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back to home", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // ============ ACTION BUTTONS (LOCKED) ============
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val isPlanningPending =
                            prefs.state == com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING

                        if (isPlanningPending) {
                            navController.navigate(HyperFocusActivity.DESTINATION_PLANNING)
                            return@Button
                        }

                        val preferredTaskId = prefs.lockedTaskIds.firstOrNull { lockedId ->
                            activeTasks.any { it.id == lockedId }
                        } ?: activeTasks.firstOrNull()?.id

                        val focusIntent = Intent(context, MainActivity::class.java).apply {
                            if (!preferredTaskId.isNullOrBlank()) {
                                action = "com.procus.ACTION_OPEN_FOCUS"
                                putExtra("task_id", preferredTaskId)
                            }
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(focusIntent)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMessengerApp) Color(0xFFFF6B6B) else Color(0xFF4A90E2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (prefs.state == com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING)
                            "Unlock apps / Finish session →"
                        else
                            "Go back to tasks",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (prefs.currentTier > RewardTier.NONE &&
                    prefs.state != com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.beginGate(blockedPackage)
                            navController.navigate(HyperFocusActivity.DESTINATION_GATE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open the gate", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (prefs.sessionMode == HyperFocusSessionMode.TASK_BASED) {
                    if (!prefs.emergencyUsed) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEmergencyDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Emergency Access (10 mins)", color = Color(0xFFB0B0B0))
                        }
                    } else {
                        Text(
                            text = "Emergency access already used for this session",
                            color = Color(0xFFB0B0B0),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB0B0B0),
            title = {
                Text("Emergency Access", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will grant you 10 minutes of access to $appName.\n\n" +
                    "⚠️ WARNING: If you use this emergency bypass, you will lose your next intermediate rewards and must complete ALL daily tasks to earn any further unlocks."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEmergencyDialog = false
                        viewModel.grantEmergencyFor(blockedPackage)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Use Emergency", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showEmergencyDialog = false
                }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

private fun Context.labelForPackage(packageName: String): String = try {
    val info = packageManager.getApplicationInfo(packageName, 0)
    packageManager.getApplicationLabel(info).toString()
} catch (e: Exception) {
    packageName
}

private fun formatSeconds(seconds: Long?): String {
    val safe = (seconds ?: 0L).coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreenscodeentrysectionkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/CodeEntrySection.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/CodeEntrySection.kt`
- **Lines of Code:** 234
- **Size:** 9,743 bytes (9.5 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.domain.UnlockResult
import kotlin.math.roundToInt

/**
 * A reward code and nothing else: no navigation, no route, no assumption about what a successful
 * entry unlocks. Under D3 a code buys a budget, so what happens next belongs to whoever embedded
 * this — the gateway spends it there and then, and nothing may offer it before the gate.
 */
@Composable
fun CodeEntrySection(
    viewModel: HyperFocusViewModel,
    onCodeAccepted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val lockoutSecondsRemaining by viewModel.lockoutSecondsRemaining.collectAsState()
    val submitResult by viewModel.submitCodeResult.collectAsState()

    val chars = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Shake animation offset
    val shakeOffset = remember { Animatable(0f) }

    val isLocked = lockoutSecondsRemaining != null
    val allFilled = chars.all { it.isNotEmpty() }
    val attemptsRemaining = (3 - prefs.wrongCodeAttempts).coerceAtLeast(0)

    val haptic = LocalHapticFeedback.current

    // React to submit result
    LaunchedEffect(submitResult) {
        when (submitResult) {
            is UnlockResult.Success -> {
                viewModel.clearSubmitCodeResult()
                onCodeAccepted()
            }
            is UnlockResult.InvalidCode -> {
                viewModel.clearSubmitCodeResult()
                // Shake the input boxes
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        0f at 0
                        -16f at 50
                        16f at 100
                        -12f at 150
                        12f at 200
                        -8f at 250
                        8f at 300
                        0f at 400
                    }
                )
                // Clear the entered chars
                for (i in chars.indices) chars[i] = ""
                focusRequesters[0].requestFocus()
            }
            is UnlockResult.Lockout, null -> { /* nothing */ }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Title
        Text(
            text = "Enter your unlock code",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Code boxes or lockout countdown
        if (isLocked) {
            val secs = lockoutSecondsRemaining!!
            Text(
                text = "Try again in ${secs / 60}:${String.format("%02d", secs % 60)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                chars.forEachIndexed { index, value ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (value.isNotEmpty()) 2.dp else 1.dp,
                            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.size(width = 48.dp, height = 58.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { newVal ->
                                val char = newVal.uppercase().lastOrNull()?.toString() ?: ""
                                chars[index] = char
                                if (char.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequesters[index])
                                .onKeyEvent { event ->
                                    if (event.key == Key.Backspace && chars[index].isEmpty() && index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                        chars[index - 1] = ""
                                        true
                                    } else false
                                },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    innerTextField()
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Attempts remaining
            Text(
                text = "$attemptsRemaining of 3 attempts remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Confirm button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.submitCode(chars.joinToString(""))
                // The caller reacts through onCodeAccepted, which the result effect above invokes
            },
            enabled = allFilled && !isLocked,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Confirm", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Help text
        Text(
            text = "Don't have a code? Complete a task to earn one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Request focus on first box when screen appears
    LaunchedEffect(Unit) {
        if (!isLocked) {
            focusRequesters[0].requestFocus()
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreenshomerolesetupscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HomeRoleSetupScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HomeRoleSetupScreen.kt`
- **Lines of Code:** 151
- **Size:** 5,424 bytes (5.3 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.util.HomeRoleUtil
import kotlinx.coroutines.delay

/**
 * Shown instead of the activation controls while the Home button still opens another launcher.
 * Without this the black home can be started and never seen, and the session can only be escaped
 * by pressing Home and living with whatever the real launcher shows.
 */
@Composable
fun HomeRoleSetupScreen(
    onGranted: () -> Unit,
    onSkip: () -> Unit = onGranted
) {
    val context = LocalContext.current

    var holdsRole by remember { mutableStateOf(HomeRoleUtil.holdsHomeRole(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            holdsRole = HomeRoleUtil.holdsHomeRole(context)
            if (holdsRole) {
                onGranted()
                break
            }
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (holdsRole) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (holdsRole) Color(0xFF69F0AE) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "Home app",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Decides what the Home button opens",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Set NeuroFlow as your home app",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The black home appears when you press Home. You can set NeuroFlow as " +
                    "your home app, or continue with background app blocking.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_HOME_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose my home app →")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (holdsRole) "✓ NeuroFlow is your home app!"
                else "Waiting for you to choose NeuroFlow...",
                color = if (holdsRole) Color(0xFF69F0AE) else Color.Gray,
                fontSize = 13.sp,
                fontWeight = if (holdsRole) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue without changing home app", color = Color(0xFF80CBC4), fontSize = 14.sp)
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreenshyperfocusactivationsheetkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HyperFocusActivationSheet.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HyperFocusActivationSheet.kt`
- **Lines of Code:** 410
- **Size:** 17,069 bytes (16.7 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.home.renderableEssentials
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperFocusActivationSheet(
    viewModel: HyperFocusViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var accessibilityEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        accessibilityEnabled = com.neuroflow.app.feature.hyperfocus.util.AccessibilityUtil
            .isAppBlockingServiceEnabled(context)
    }

    var homeRoleHeld by remember { mutableStateOf(false) }
    LaunchedEffect(accessibilityEnabled) {
        if (accessibilityEnabled) {
            homeRoleHeld = com.neuroflow.app.feature.hyperfocus.util.HomeRoleUtil.holdsHomeRole(context)
        }
    }

    var skippedHomeRole by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (!accessibilityEnabled) {
            PermissionSetupScreen(onBothGranted = { accessibilityEnabled = true })
        } else if (!homeRoleHeld && !skippedHomeRole) {
            HomeRoleSetupScreen(
                onGranted = { homeRoleHeld = true },
                onSkip = { skippedHomeRole = true }
            )
        } else {
            ActivationSheetContent(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ActivationSheetContent(
    viewModel: HyperFocusViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTasks by viewModel.activeTasks.collectAsState()
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val activationMessage by viewModel.activationMessage.collectAsState()
    val hasActiveTasks = activeTasks.isNotEmpty()

    val selectedTaskIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(activeTasks) {
        val activeIds = activeTasks.map { it.id }.toSet()
        val stale = selectedTaskIds.keys.filter { it !in activeIds }
        stale.forEach { selectedTaskIds.remove(it) }
        activeTasks.forEach { task ->
            if (selectedTaskIds[task.id] == null) {
                selectedTaskIds[task.id] = false
            }
        }
    }

    var taskSearchQuery by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    var sessionMode by remember { mutableStateOf(HyperFocusSessionMode.TASK_BASED) }
    val durationOptions = remember { listOf(15, 25, 45, 60, 90, 120) }
    var selectedDurationMinutes by remember { mutableStateOf(25) }

    val filteredTasks = remember(taskSearchQuery, activeTasks) {
        if (taskSearchQuery.isBlank()) activeTasks
        else activeTasks.filter { task ->
            task.title.contains(taskSearchQuery, ignoreCase = true) ||
                task.description.contains(taskSearchQuery, ignoreCase = true)
        }
    }

    val haptic = LocalHapticFeedback.current
    val selectedTaskCount = selectedTaskIds.count { it.value }
    // Counted the way the home renders it, so this number and the grid that appears after activation
    // cannot disagree — and so the button cannot enable on a tier of nothing but Settings.
    val essentialCount = renderableEssentials(prefs, SystemExempt.STATIC, context.packageName).size
    val allowedCount = prefs.allowedPackages.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Activate Hyper Focus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Warning banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                    "⚠️ Selected apps will be blocked for your chosen duration."
                } else {
                    "⚠️ Selected apps will be blocked until you complete your daily tasks."
                },
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(14.dp)
            )
        }

        Text(
            text = "Focus mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sessionMode == HyperFocusSessionMode.TASK_BASED,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sessionMode = HyperFocusSessionMode.TASK_BASED
                },
                shape = RoundedCornerShape(12.dp),
                label = { Text("Task-based") }
            )
            FilterChip(
                selected = sessionMode == HyperFocusSessionMode.TIME_BASED,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sessionMode = HyperFocusSessionMode.TIME_BASED
                },
                shape = RoundedCornerShape(12.dp),
                label = { Text("Time-based") }
            )
        }

        if (sessionMode == HyperFocusSessionMode.TASK_BASED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Select tasks for this session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Daily task target: $selectedTaskCount task(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Block duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.forEach { minutes ->
                    FilterChip(
                        selected = selectedDurationMinutes == minutes,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDurationMinutes = minutes
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("${minutes}m") }
                    )
                }
            }
        }

        if (sessionMode == HyperFocusSessionMode.TASK_BASED) {
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks in Hyper Focus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$selectedTaskCount selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = taskSearchQuery,
                onValueChange = { taskSearchQuery = it },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredTasks.isEmpty()) {
                Text(
                    text = "No active tasks found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredTasks.forEach { task ->
                    val isChecked = selectedTaskIds[task.id] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTaskIds[task.id] = !isChecked
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTaskIds[task.id] = checked
                            }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // Confirmation field
        OutlinedTextField(
            value = confirmText,
            onValueChange = { confirmText = it },
            label = { Text("Type FOCUS to confirm") },
            supportingText = { Text("This action is hard to reverse during a session.") },
            singleLine = true,
            isError = confirmText.isNotEmpty() && confirmText != "FOCUS",
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // The tiers are global (B2): this sheet confirms a session, it no longer chooses apps.
        Text(
            text = if (essentialCount == 0) {
                "No essential apps pinned. Pin at least one app as essential in the allowlist editor " +
                    "before starting — a session whose home has no icons has nowhere to hand you back to."
            } else {
                "Blocking everything except $essentialCount essential apps and the " +
                    "${prefs.allowedPackages.size} you allowed behind the gate. Hidden apps need a pass."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (essentialCount == 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                    viewModel.activateTimed(selectedDurationMinutes)
                } else {
                    val selectedTasks = selectedTaskIds.filter { it.value }.keys.toSet()
                    viewModel.activate(selectedTasks)
                }
                onDismiss()
            },
            enabled = confirmText == "FOCUS" && essentialCount > 0 &&
                (sessionMode == HyperFocusSessionMode.TIME_BASED || (hasActiveTasks && selectedTaskCount > 0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            val label = if (sessionMode == HyperFocusSessionMode.TIME_BASED) {
                "Activate ${selectedDurationMinutes}m Focus Lock"
            } else {
                "Activate Hyper Focus 🔒"
            }
            Text(label, fontWeight = FontWeight.Bold)
        }


        // A rejection is a fact about the screen the user is standing on, so it renders here rather
        // than as a toast the sheet has already dismissed.
        activationMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // No tasks warning
        if (sessionMode == HyperFocusSessionMode.TASK_BASED && !hasActiveTasks) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "⚠️ You need at least one active task before activating Hyper Focus.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreenspermissionsetupscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PermissionSetupScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PermissionSetupScreen.kt`
- **Lines of Code:** 205
- **Size:** 7,263 bytes (7.1 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import android.app.AppOpsManager
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PermissionSetupScreen(onBothGranted: () -> Unit) {
    val context = LocalContext.current

    fun isAccessibilityEnabled(): Boolean {
        return com.neuroflow.app.feature.hyperfocus.util.AccessibilityUtil
            .isAppBlockingServiceEnabled(context)
    }

    fun isUsageStatsGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    var accessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled()) }
    var usageStatsGranted by remember { mutableStateOf(isUsageStatsGranted()) }

    // Poll every second for both permissions
    LaunchedEffect(Unit) {
        while (true) {
            accessibilityGranted = isAccessibilityEnabled()
            usageStatsGranted = isUsageStatsGranted()
            if (accessibilityGranted && usageStatsGranted) {
                onBothGranted()
                break
            }
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Two Permissions Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hyper Focus needs these to detect and block distracting apps.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Permission 1 — Accessibility
            PermissionRow(
                label = "Accessibility Service",
                description = "Detects when a blocked app is opened",
                granted = accessibilityGranted
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!accessibilityGranted) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Accessibility →")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find \"${context.applicationInfo.loadLabel(context.packageManager)}\" and enable it.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permission 2 — Usage Stats
            PermissionRow(
                label = "Usage Access",
                description = "Monitors which app is in the foreground",
                granted = usageStatsGranted
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!usageStatsGranted) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Usage Access →")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find \"${context.applicationInfo.loadLabel(context.packageManager)}\" and enable it.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (accessibilityGranted && usageStatsGranted) {
                Text(
                    text = "✓ All permissions granted!",
                    color = Color(0xFF69F0AE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Waiting for permissions...",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, description: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.Check else Icons.Default.Circle,
            contentDescription = null,
            tint = if (granted) Color(0xFF69F0AE) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreensplanningpromptscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PlanningPromptScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/PlanningPromptScreen.kt`
- **Lines of Code:** 139
- **Size:** 5,465 bytes (5.3 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel

@Composable
fun PlanningPromptScreen(viewModel: HyperFocusViewModel, onDismiss: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val taskTitles = remember { mutableStateListOf("") }
    val extraTitles = remember { mutableStateListOf<String>() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎉 All Tasks Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    viewModel.completePlanning(emptyList())
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close and Unlock"
                    )
                }
            }

            Text(
                text = "Plan tasks for tomorrow (optional), or finish now to unlock all your apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            taskTitles.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { taskTitles[index] = it },
                    label = { Text("Task ${index + 1} (optional)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            extraTitles.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { extraTitles[index] = it },
                    label = { Text("Task ${taskTitles.size + index + 1}") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            TextButton(onClick = { extraTitles.add("") }) {
                Text("+ Add another task", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.completePlanning((taskTitles + extraTitles).filter { it.isNotBlank() })
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                val hasAnyTask = (taskTitles + extraTitles).any { it.isNotBlank() }
                Text(
                    text = if (hasAnyTask) "Save Tasks & Unlock Apps" else "Unlock Apps & Finish",
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = {
                    viewModel.completePlanning(emptyList())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip & Unlock Apps Now", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreensrewardsectionkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardSection.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardSection.kt`
- **Lines of Code:** 197
- **Size:** 8,928 bytes (8.7 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.domain.RewardEngine

@Composable
fun RewardSection(viewModel: HyperFocusViewModel, modifier: Modifier = Modifier) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val rewardCounts by viewModel.rewardCounts.collectAsState()
    val claimedCode by viewModel.claimedCodeToShow.collectAsState()
    val claimedTier by viewModel.claimedCodeTier.collectAsState()

    if (!prefs.isActive) return

    Column(
        modifier = modifier
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rewards",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Text(
            text = "${progress.completedSinceActivation} / ${progress.totalTarget} tasks complete",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // One row per tier (skip NONE and FULL for display purposes — FULL shown separately)
        val displayTiers = listOf(
            RewardTier.MICRO  to "2 min unlock",
            RewardTier.PARTIAL to "10 min unlock",
            RewardTier.EARNED  to "30 min unlock",
            RewardTier.FULL    to "Full unlock"
        )

        displayTiers.forEach { (tier, label) ->
            val isEarned = RewardEngine.isTierEarned(
                tier = tier,
                completedSinceActivation = progress.completedSinceActivation,
                totalTarget = progress.totalTarget,
                emergencyUsed = prefs.emergencyUsed
            )
            val isReachable = RewardEngine.isTierReachable(
                tier = tier,
                totalTarget = progress.totalTarget,
                emergencyUsed = prefs.emergencyUsed
            )
            val hasUnclaimedCode = (rewardCounts[tier] ?: 0) > 0
            // Hide the FULL tier claim button once planning is pending (code already claimed)
            val isPlanningPending = prefs.state == com.neuroflow.app.domain.model.HyperFocusState.FULL_REWARD_PENDING
            val isAvailable = isReachable && isEarned && hasUnclaimedCode && !(tier == RewardTier.FULL && isPlanningPending)
            val isPendingThisTier = claimedCode != null && claimedTier == tier

            val haptic = LocalHapticFeedback.current

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isPendingThisTier || (isEarned && !hasUnclaimedCode) ->
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    isEarned ->
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else ->
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isEarned)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    !isReachable -> "Not available for current target"
                                    !isEarned -> "Not yet earned"
                                    !hasUnclaimedCode -> "Already used"
                                    else -> "Earned — ready to claim"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.claimRewardForTier(tier)
                            },
                            enabled = isAvailable,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(if (isPendingThisTier) "Show" else "Claim")
                        }
                    }

                    // Show the claimed code inline under this tier row
                    if (isPendingThisTier && claimedCode != null) {
                        val code = claimedCode ?: return@Column
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your code:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = 4.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.dismissClaimedCode()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocusscreensrewardsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/RewardsScreen.kt`
- **Lines of Code:** 55
- **Size:** 2,108 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onNavigateBack: () -> Unit,
    viewModel: HyperFocusViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards") },
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
        }
    ) { innerPadding ->
        RewardSection(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocussurfaceallowlisteditorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/AllowlistEditor.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/AllowlistEditor.kt`
- **Lines of Code:** 31
- **Size:** 1,556 bytes (1.5 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

enum class AllowlistTier { Essential, Allowed }

data class AllowlistDraft(val essential: Set<String>, val allowed: Set<String>)

fun draftFrom(prefs: HyperFocusPreferences): AllowlistDraft =
    AllowlistDraft(essential = prefs.essentialPackages, allowed = prefs.allowedPackages)

fun applyDraft(prefs: HyperFocusPreferences, draft: AllowlistDraft): HyperFocusPreferences =
    prefs.copy(essentialPackages = draft.essential, allowedPackages = draft.allowed)

/**
 * The two tiers are exclusive: a package is either always on the home surface or only reachable
 * through the gateway. Both at once would make the gateway's list disagree with the home's grid
 * about the same app.
 */
fun toggleTier(draft: AllowlistDraft, packageName: String, tier: AllowlistTier): AllowlistDraft =
    when (tier) {
        AllowlistTier.Essential ->
            if (packageName in draft.essential) draft.copy(essential = draft.essential - packageName)
            else draft.copy(essential = draft.essential + packageName, allowed = draft.allowed - packageName)
        AllowlistTier.Allowed ->
            if (packageName in draft.allowed) draft.copy(allowed = draft.allowed - packageName)
            else draft.copy(allowed = draft.allowed + packageName, essential = draft.essential - packageName)
    }

fun editableApps(all: Set<String>, systemExempt: Set<String>, selfPackage: String): Set<String> =
    all - systemExempt - selfPackage
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocussurfacefocussurfacemodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/FocusSurfaceModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/FocusSurfaceModel.kt`
- **Lines of Code:** 43
- **Size:** 1,536 bytes (1.5 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

/**
 * The single source of truth for "may this package be used right now".
 * Everything that gates an app reads resolvedAllowlist(); nothing computes its own view.
 */
object FocusSurfaceModel {

    val DEFAULT_ESSENTIAL_SEED: Set<String> = setOf(
        "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
        "com.android.mms", "com.google.android.apps.messaging",
        "com.android.contacts",
        "com.android.camera", "com.google.android.Camera",
        "com.android.deskclock", "com.google.android.deskclock",
        BuildConfig.APPLICATION_ID
    )

    fun resolvedAllowlist(
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Set<String> = buildSet {
        addAll(prefs.essentialPackages)
        addAll(prefs.allowedPackages)
        addAll(systemExempt)
        add(selfPackage)
        val pass = prefs.activePassPackage
        val deadline = prefs.activePassExpiresAt
        if (pass != null && deadline != null && deadline > nowMillis) add(pass)
    }

    fun isGated(
        packageName: String,
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Boolean = packageName !in resolvedAllowlist(prefs, systemExempt, selfPackage, nowMillis)
}
```

---

<a id="appsrcmainjavacomneuroflowappfeaturehyperfocussurfacesystemexemptkt"></a>
### File: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/SystemExempt.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/SystemExempt.kt`
- **Lines of Code:** 54
- **Size:** 1,950 bytes (1.9 KB)

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.neuroflow.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Packages that must never be gated: gating them locks the user out of their own device.
 * Read fresh because the enabled keyboard changes without our knowing.
 */
@Singleton
class SystemExempt @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun packages(): Set<String> = withContext(Dispatchers.Default) {
        buildSet {
            addAll(STATIC)
            addAll(CALL_PATH)
            add(BuildConfig.APPLICATION_ID)
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.enabledInputMethodList?.forEach { add(it.packageName) }
        }
    }

    companion object {
        val STATIC: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.server.telecom",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter"
        )

        /**
         * The call path: never gated under any user configuration (spec 6.2), but unlike STATIC it
         * stays a home-screen icon (spec 6.4), so it is deliberately not in STATIC.
         */
        val CALL_PATH: Set<String> = setOf(
            "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
            "com.google.android.dialer"
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationwidgetcompletetaskreceiverkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskReceiver.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskReceiver.kt`
- **Lines of Code:** 46
- **Size:** 1,622 bytes (1.6 KB)

```kotlin
package com.neuroflow.app.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Priority 3 (ADR-003): Interactive Home Screen Widget Task Completion
 *
 * BroadcastReceiver executing zero-app-launch 1-tap task completion for RemoteViews widget.
 * Updates Room v22 and refreshes widget in <150ms.
 */
class CompleteTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION_COMPLETE_TASK) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CompleteTaskWidgetAction.execute(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.neuroflow.app.action.COMPLETE_TASK_WIDGET"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationwidgetcompletetaskwidgetactionkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt`
- **Lines of Code:** 41
- **Size:** 1,450 bytes (1.4 KB)

```kotlin
package com.neuroflow.app.presentation.widget

import android.content.Context
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Priority 3 (ADR-003): Interactive Home Screen Widget Action
 *
 * Encapsulates the task completion action callback for widget interactions,
 * executing Room v22 update under NonCancellable + Dispatchers.IO and triggering
 * immediate widget state refresh.
 */
object CompleteTaskWidgetAction {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetActionEntryPoint {
        fun taskRepository(): TaskRepository
    }

    suspend fun execute(context: Context, taskId: String): Boolean {
        if (taskId.isBlank()) return false
        return withContext(NonCancellable + Dispatchers.IO) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetActionEntryPoint::class.java
            )
            val success = entryPoint.taskRepository().markTaskCompleted(taskId)
            FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
            success
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationwidgetfocuswidgetproviderkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/widget/FocusWidgetProvider.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/widget/FocusWidgetProvider.kt`
- **Lines of Code:** 50
- **Size:** 2,197 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.worker.FocusWidgetUpdateWorker

class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Trigger an immediate update via WorkManager
        val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueue(request)

        // Set up the Start button tap intent for each widget instance
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_focus)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_FOCUS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, widgetId, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle Start tap — launch MainActivity
        if (intent.action == "com.neuroflow.app.WIDGET_START") {
            val taskId = intent.getStringExtra("taskId")
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                taskId?.let { putExtra("taskId", it) }
            }
            context.startActivity(launchIntent)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationwidgetnextactionwidgetproviderkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/widget/NextActionWidgetProvider.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/widget/NextActionWidgetProvider.kt`
- **Lines of Code:** 37
- **Size:** 1,542 bytes (1.5 KB)

```kotlin
package com.neuroflow.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.worker.FocusWidgetUpdateWorker

class NextActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueue(request)

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_next_action)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_FOCUS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_action_container, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationwidgetquickcapturewidgetproviderkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/widget/QuickCaptureWidgetProvider.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/widget/QuickCaptureWidgetProvider.kt`
- **Lines of Code:** 31
- **Size:** 1,270 bytes (1.2 KB)

```kotlin
package com.neuroflow.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R

class QuickCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_NEW_TASK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_capture_button, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
```

---

