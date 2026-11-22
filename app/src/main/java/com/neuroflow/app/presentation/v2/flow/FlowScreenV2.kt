package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.plan.DailyBlueprintBottomSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

private val PART_NUM_ID_REGEX = Regex("""[#_]part(\d+)""")
private val PART_NUM_TITLE_REGEX = Regex("""\(Part (\d+)/""")
private val PARENT_SLUG_REGEX = Regex("""[#_]part\d+.*$""")
private val PART_PREFIX_REGEX = Regex("""([#_]part)\d+""")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlowScreenV2(
    onNavigateToSettings: () -> Unit,
    onStartFocus: (String) -> Unit,
    onNavigateToMindmap: () -> Unit = {},
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onOpenToolsSheet: () -> Unit = {},
    onStartDoorwayFocus: ((String) -> Unit)? = null,
    initialOpenComposer: Boolean = false,
    onInitialComposerConsumed: () -> Unit = {},
    activeBreakTimer: com.neuroflow.app.domain.assistant.ActiveBreakTimerState = com.neuroflow.app.domain.assistant.ActiveBreakTimerState(),
    onStartBreakTimer: ((Int) -> Unit)? = null,
    onLogRestBreak: ((Int) -> Unit)? = null,
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    var showComposer by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var showEasyWinSheet by remember { mutableStateOf(false) }
    var locallyCompletedIds by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(state.activeTasks) {
        val currentActiveIds = state.activeTasks.map { it.id }.toSet()
        locallyCompletedIds = locallyCompletedIds.intersect(currentActiveIds)
    }

    val nowMinute by produceState(System.currentTimeMillis() / 60_000L) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            value = System.currentTimeMillis() / 60_000L
        }
    }
    val nowMillis = remember(nowMinute) { nowMinute * 60_000L }

    LaunchedEffect(initialOpenComposer) {
        if (initialOpenComposer) {
            showComposer = true
            onInitialComposerConsumed()
        }
    }

    // 4-Second Non-Destructive Undo Snackbar
    LaunchedEffect(state.lastActionMessage) {
        val msg = state.lastActionMessage
        if (!msg.isNullOrBlank()) {
            val canUndo = msg.startsWith("Completed") ||
                msg.startsWith("Deferred") ||
                msg.startsWith("Stimulant logged:") ||
                msg.startsWith("Bio-event logged:")
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (canUndo) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                when {
                    msg.startsWith("Completed") -> {
                        locallyCompletedIds = emptySet()
                        viewModel.undoCompleteTask()
                    }
                    msg.startsWith("Deferred") -> viewModel.undoDeferTask()
                    msg.startsWith("Stimulant logged:") -> viewModel.undoLogStimulant()
                    msg.startsWith("Bio-event logged:") -> viewModel.undoLogPhysiologicalEvent()
                }
            } else {
                viewModel.clearActionMessage()
            }
        }
    }

    val currentPhase = state.circadianSummary?.currentSnapshot?.phase
        ?: state.energyTimeline.minByOrNull {
            kotlin.math.abs(it.timestampMillis - nowMillis)
        }?.phase ?: EnergyPhase.PEAK
    val headerAccent = NeuroFlowPaletteV2.accentForPhase(currentPhase)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 72.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    contentColor = NeuroFlowThemeV2Colors.TextPrimary,
                    actionColor = NeuroFlowThemeV2Colors.WarmAmber,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(NeuroFlowThemeV2Colors.ElectricIndigo)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .combinedClickable(
                        onClick = { showComposer = true },
                        onLongClick = { showNotesSheet = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Quick Add Task (Long-press for Notes)",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPanelPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPanelPadding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "neuroFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(headerAccent, CircleShape)
                        )
                        Text(
                            text = "Biological High Performance",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt.copy(alpha = 0.85f),
                        border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f))
                    ) {
                        IconButton(
                            onClick = onOpenToolsSheet,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tools_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Tools and Subsystems",
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt.copy(alpha = 0.85f),
                        border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f))
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeuroFlowThemeV2Colors.TextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.16f),
                thickness = 0.75.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )

            // Hero Circadian Barometer (Borbély Two-Process Model)
            val energyScore = state.energyUi?.rawEnergy ?: 50f
            val battery = state.energyUi?.availableEnergy ?: 72
            val currentEnergyLevel = when {
                battery >= 65 -> EnergyLevel.HIGH
                battery >= 35 -> EnergyLevel.MEDIUM
                else -> EnergyLevel.LOW
            }

            CircadianHeroBarometer(
                summary = state.circadianEnergySummary,
                rawEnergyScore = energyScore,
                batteryPercent = battery,
                energyZone = currentEnergyLevel,
                sleepPressurePoints = state.energyUi?.sleepPressurePoints ?: 450,
                fatigueZone = state.energyUi?.fatigueZone ?: com.neuroflow.app.domain.model.FatigueZone.RESTED,
                circadianFactor = state.energyUi?.circadianFactor ?: 0.85f,
                recommendedTask = state.recommendedTask,
                onStartRecommendedTask = { taskId -> onStartFocus(taskId) },
                onNavigateToMEQ = onNavigateToMEQ,
                onNavigateToSleepLogs = onNavigateToSleepLogs,
                dayTimeline = state.energyTimeline,
                cognitiveBlocks = state.dailyCognitiveBlocks,
                naps = state.daytimeNaps,
                isFallbackSleep = state.isFallbackSleep,
                learnedSampleCount = state.learnedSampleCount,
                stimulantDoses = state.stimulantDoses,
                isCrashWindow = state.isCrashWindow,
                activePlasmaMg = state.activePlasmaMg,
                socialJetlagLagHours = state.socialJetlagLagHours,
                predictedSwsSuppressionPercent = state.predictedSwsSuppressionPercent,
                latestSafeStimulantCutoffFormatted = state.latestSafeStimulantCutoffFormatted,
                simulationState = state.simulationState,
                onToggleSimulationMode = { active -> viewModel.toggleSimulationMode(active) },
                onToggleSimulatedLever = { lever -> viewModel.toggleSimulatedLever(lever) },
                onResetSimulation = { viewModel.resetSimulation() },
                onApplySimulationToPlan = { viewModel.applySimulationToPlan() },
                onClearStimulants = { viewModel.clearAllStimulants() },
                onRemoveStimulantDose = { dose -> viewModel.removeStimulantDose(dose.timestampMillis, dose.type) },
                onLogStimulant = { type -> viewModel.logStimulant(type) },
                onLogPhysiologicalEvent = { type -> viewModel.logPhysiologicalEvent(type) },
                onClearPhysiologicalEvents = { viewModel.clearAllPhysiologicalEvents() },
                unboostedDayTimeline = state.unboostedDayTimeline,
                currentEnergyBoostPoints = state.currentEnergyBoostPoints,
                energyBoosterRecommendations = state.energyBoosterRecommendations,
                graphBoosterMarkers = state.graphBoosterMarkers,
                previewedBoosterId = state.previewedBoosterId,
                previewedBoosterTimeline = state.previewedBoosterTimeline,
                previewedBoosterSummary = state.previewedBoosterSummary,
                energyBoostDeltaBanner = state.energyBoostDeltaBanner,
                onTogglePreviewBooster = { rec -> viewModel.togglePreviewEnergyBooster(rec) },
                onCompleteBooster = { rec -> viewModel.completeEnergyBooster(rec) },
                onStartBoosterTimer = onStartBreakTimer,
                onRefreshUnlockedSchedule = { viewModel.refreshUnlockedScheduleAfterBoost() },
                onDismissEnergyBoostDeltaBanner = { viewModel.dismissEnergyBoostDeltaBanner() }
            )

            // Active Biophysical Impulse Strip (Meal, Workout, Nap, Stimulant PK ticker)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActiveBiophysicalImpulseStrip(
                    cognitiveBlocks = state.dailyCognitiveBlocks,
                    activePlasmaMg = state.activePlasmaMg,
                    nowMillis = nowMillis
                )
            }

            // Flow Spotlight Hero Card (Takeover or Top Action)
            val activeTakeoverSlot = remember(state.preferences.protectedTimeSlots, nowMinute) {
                ProtectedSlotClock.takeoverOf(state.preferences.protectedTimeSlots, nowMillis)
            }
            val approachingSlot = remember(state.preferences.protectedTimeSlots, nowMinute) {
                ProtectedSlotClock.approaching(state.preferences.protectedTimeSlots, nowMillis, leadInMinutes = 45).firstOrNull()
            }
            val completedIds = remember(state.completedTasks, locallyCompletedIds) {
                state.completedTasks.map { it.id }.toSet() + locallyCompletedIds
            }
            val activeIds = remember(state.activeTasks, locallyCompletedIds) {
                state.activeTasks.map { it.id }.toSet() - locallyCompletedIds
            }

            val topTask = remember(state.recommendedTask, state.filteredTasks, state.selectedTag, completedIds, activeIds, currentPhase) {
                fun isTaskActionable(task: TaskEntity): Boolean {
                    val isActionableStatus = task.id !in completedIds &&
                        task.id in activeIds &&
                        task.status == TaskStatus.ACTIVE
                    val dependenciesMet = task.dependsOnTaskIds.isBlank() ||
                        task.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.all { it in completedIds }
                    val partNumMatch = PART_NUM_ID_REGEX.find(task.id)
                    val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                        ?: PART_NUM_TITLE_REGEX.find(task.title)?.groupValues?.get(1)?.toIntOrNull()
                    val isContinuation = (partNum != null && partNum >= 2) ||
                        task.id.endsWith("_part2") ||
                        task.id.contains("#part2") ||
                        task.parentTaskId != null
                    val parentSlug = task.parentTaskId ?: task.id.replace(PARENT_SLUG_REGEX, "")
                    val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeIds) ||
                        (parentSlug != task.id && parentSlug in activeIds)
                    val hasPartRegex = task.id.contains(PART_NUM_ID_REGEX)
                    val isPart1Active = if (hasPartRegex) {
                        val part1Id = task.id.replace(PART_PREFIX_REGEX, "$1" + "1")
                        part1Id != task.id && part1Id in activeIds
                    } else false
                    val isPrecedingPartActive = if (hasPartRegex && partNum != null) {
                        (1 until partNum).any { prevPart ->
                            val prevId = task.id.replace(PART_PREFIX_REGEX, "$1$prevPart")
                            prevId != task.id && prevId in activeIds
                        }
                    } else if (task.parentTaskId != null && partNum != null && partNum > 1) {
                        state.activeTasks.any { other ->
                            other.id in activeIds &&
                                other.id != task.id &&
                                (other.parentTaskId == task.parentTaskId || other.id == task.parentTaskId) &&
                                (PART_NUM_TITLE_REGEX.find(other.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1) < partNum
                        }
                    } else false
                    val isPartBlocked = isContinuation && (isParentActive || isPart1Active || isPrecedingPartActive)
                    return isActionableStatus && task.waitingFor.isBlank() && dependenciesMet && !isPartBlocked
                }

                val actionableFiltered = state.filteredTasks.filter { isTaskActionable(it) }
                if (currentPhase == EnergyPhase.WIND_DOWN) {
                    // Option 2C: When in WIND_DOWN, prioritize low-energy / non-deep tasks; subdue frogs
                    val lowStress = actionableFiltered.filter { task ->
                        task.energyLevel == EnergyLevel.LOW || (
                            task.energyLevel != EnergyLevel.HIGH &&
                            !task.isFrog &&
                            task.taskType != TaskType.ANALYTICAL &&
                            !task.tags.contains("deepwork", ignoreCase = true) &&
                            !task.contextTag.equals("deepwork", ignoreCase = true)
                        )
                    }
                    state.recommendedTask?.takeIf { isTaskActionable(it) }
                        ?: lowStress.firstOrNull { it.energyLevel == EnergyLevel.LOW }
                        ?: lowStress.firstOrNull()
                } else if (state.selectedTag.isNullOrBlank() || state.selectedTag.equals("All", ignoreCase = true)) {
                    state.recommendedTask?.takeIf { isTaskActionable(it) } ?: actionableFiltered.firstOrNull { it.isFrog } ?: actionableFiltered.firstOrNull()
                } else {
                    state.filteredTasks.firstOrNull { it.id == state.recommendedTask?.id && isTaskActionable(it) }
                        ?: actionableFiltered.firstOrNull { it.isFrog }
                        ?: actionableFiltered.firstOrNull()
                }
            }
            val easyWinTasks = remember(state.activeTasks, topTask?.id, completedIds) {
                state.activeTasks
                    .filter { task ->
                        task.status == TaskStatus.ACTIVE &&
                            task.quadrant != Quadrant.ELIMINATE &&
                            task.id != topTask?.id &&
                            task.id !in completedIds &&
                            task.waitingFor.isBlank() &&
                            (task.energyLevel == EnergyLevel.LOW || TaskBlockPlan.plannedMinutes(task) <= 15)
                    }
                    .sortedWith(
                        compareBy<TaskEntity> { TaskBlockPlan.plannedMinutes(it) }
                            .thenBy { it.energyLevel != EnergyLevel.LOW }
                            .thenBy { it.createdAt }
                    )
            }

            if (topTask != null || activeTakeoverSlot != null || currentPhase == EnergyPhase.WIND_DOWN) {
                val microSteps = remember(topTask?.ifThenPlan) {
                    topTask?.ifThenPlan?.let { com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(it) } ?: emptyList()
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlowSpotlightHeroCard(
                        activeTakeoverSlot = activeTakeoverSlot,
                        approachingSlot = approachingSlot,
                        topTaskId = topTask?.id,
                        topTaskTitle = topTask?.title,
                        topTaskFirstMicroStep = microSteps.firstOrNull()?.text,
                        topTaskMicroSteps = microSteps,
                        topTaskDurationMinutes = topTask?.let { com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(it) } ?: 25,
                        topTaskEnergyLevel = topTask?.energyLevel ?: EnergyLevel.LOW,
                        isTopTaskFrog = topTask?.isFrog ?: false,
                        currentPhase = currentPhase,
                        onStartFocusClick = { taskId -> onStartFocus(taskId) },
                        onCompleteQuickClick = { taskId ->
                            val taskToComplete = state.activeTasks.find { it.id == taskId } ?: topTask
                            if (taskToComplete != null) {
                                locallyCompletedIds = locallyCompletedIds + taskToComplete.id
                                if (taskToComplete.isFrog) showFrogConfetti = true
                                viewModel.completeTask(taskToComplete)
                            }
                        },
                        onQuickFocusClick = { onStartFocus(com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS) },
                        onShutdownClick = { viewModel.completeEveningShutdown() },
                        onToggleMicroStep = { stepIndex ->
                            topTask?.let { viewModel.toggleMicroStep(it.id, stepIndex) }
                        },
                        onDoorway2mStart = { taskId -> (onStartDoorwayFocus ?: onStartFocus)(taskId) },
                        onEasyWinClick = if (easyWinTasks.isNotEmpty() || battery < 45) {
                            { showEasyWinSheet = true }
                        } else null,
                        isLowBatterySuggested = battery < 45,
                        topTaskFatigueScore = topTask?.let { state.fatigueScoresByTaskId[it.id] },
                        isOvertimeActive = state.isOvertimeActive,
                        onEnableOvertimeClick = { viewModel.toggleOvertimeTonight() },
                        burnoutRiskState = state.burnoutRiskState,
                        activeBreakTimer = activeBreakTimer,
                        onStartBreakTimer = onStartBreakTimer,
                        onLogRestBreak = { mins ->
                            if (onLogRestBreak != null) {
                                onLogRestBreak(mins)
                            } else {
                                viewModel.logRestBreak(mins)
                            }
                        }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FreeFlowHeroCard(
                        onStartFocus = { onStartFocus(com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS) },
                        onPlanMyDay = { viewModel.openDailyBlueprint() }
                    )
                }
            }

            // Fresh Start Recovery Banner (ADHD Safety Net)
            val resetPlan = remember(state.activeTasks, state.showZeroGuiltPrompt, nowMinute) {
                val hasOverdue = state.showZeroGuiltPrompt || state.activeTasks.any { it.postponeCount >= 2 }
                if (hasOverdue) {
                    FreshStartEngine.buildEmergencyResetPlan(
                        activeTasks = state.activeTasks,
                        nowMillis = nowMillis
                    )
                } else null
            }

            if (!state.zeroGuiltDismissed && (state.showZeroGuiltPrompt || (resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty()))) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FreshStartRecoveryBanner(
                        visible = true,
                        resetPlan = resetPlan,
                        onApplyEmergencyReset = {
                            if (resetPlan != null) {
                                viewModel.applyEmergencyReset(resetPlan)
                            } else {
                                viewModel.freshStartRollover()
                            }
                        },
                        onDismiss = { viewModel.dismissZeroGuiltPrompt() }
                    )
                }
            }

            // Soft Evening & Overtime Assistant Card (only shown in evening, after hitting target, or when Overtime is active)
            val showOvertimeAssistantCard = com.neuroflow.app.domain.assistant.OvertimeAssistantPolicy.shouldShowAssistantCard(
                isEveningShutdownTime = state.isEveningShutdownTime,
                dailyTargetReached = state.dailyTargetReached,
                isOvertimeActive = state.isOvertimeActive,
                eveningShutdownDismissed = state.eveningShutdownDismissed
            )
            if (showOvertimeAssistantCard) {
                Spacer(modifier = Modifier.height(10.dp))
                val cardAccent = if (state.isOvertimeActive) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowPaletteV2.PeakAmber
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("evening_overtime_assistant_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(
                        1.dp,
                        NeuroFlowPaletteV2.specularBorderBrush(cardAccent, 0.85f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isOvertimeActive) Icons.Default.AutoAwesome else Icons.Default.ShieldMoon,
                                    contentDescription = null,
                                    tint = cardAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (state.isOvertimeActive) "Overtime Mode Active" else "Evening & Overtime Assistant",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = cardAccent
                                )
                            }
                            if (state.hasUrgentTonightTask || state.hasUrgentTomorrowMorningTask) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (state.hasUrgentTonightTask) "Due Tonight" else "Due AM",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeuroFlowPaletteV2.RecoveryRose,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(nowMillis)
                        val todayCompletedCount = state.completedTasks.count {
                            val comp = it.completedAt ?: it.updatedAt
                            comp >= todayStart
                        }
                        val message = when {
                            state.isOvertimeActive -> "Extra evening capacity & tonight's slots are unlocked. Pace yourself with short hydration breaks."
                            state.hasUrgentTonightTask -> "You still have a task due tonight. Turn on Overtime to schedule an evening slot or jump right in."
                            state.hasUrgentTomorrowMorningTask -> "You have an urgent task due before tomorrow morning. Turn on Overtime to schedule it tonight or jump right in."
                            state.dailyTargetReached -> "You hit your ${state.preferences.dailyDeepWorkTargetMinutes}m focus goal ($todayCompletedCount done). Rest up, or enable Overtime to keep pushing."
                            else -> "It's evening wind-down ($todayCompletedCount done today). Keep working with one-tap Overtime, or wrap up cleanly."
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.toggleOvertimeTonight() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardAccent,
                                    contentColor = NeuroFlowPaletteV2.onAccentColor(cardAccent)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("toggle_overtime_button")
                            ) {
                                Text(
                                    text = if (state.isOvertimeActive) "Overtime On ✓" else "Enable Overtime",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.completeEveningShutdown() },
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("wrap_up_evening_button")
                            ) {
                                Text(
                                    text = "Wrap Up",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag Filter Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableTags) { tag ->
                    val isSelected = if (tag == "All") {
                        state.selectedTag == null || state.selectedTag == "All"
                    } else {
                        state.selectedTag == tag
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTag(if (tag == "All") null else tag) },
                        label = {
                            Text(
                                tag,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            labelColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f),
                            selectedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Categorization Segmented Toggle: Priority Score | Energy Demand
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("flow_grouping_segmented_toggle")
            ) {
                SegmentedButton(
                    selected = state.groupingMode == FlowGroupingMode.PRIORITY_SCORE,
                    onClick = { viewModel.setGroupingMode(FlowGroupingMode.PRIORITY_SCORE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f),
                        activeContentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("flow_toggle_priority")
                ) {
                    Text(
                        text = "Priority Score",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                SegmentedButton(
                    selected = state.groupingMode == FlowGroupingMode.ENERGY_DEMAND,
                    onClick = { viewModel.setGroupingMode(FlowGroupingMode.ENERGY_DEMAND) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f),
                        activeContentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("flow_toggle_energy")
                ) {
                    Text(
                        text = "Energy Demand",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val visibleStreamTasks = remember(state.filteredTasks, locallyCompletedIds) {
                state.filteredTasks.filterNot { it.id in locallyCompletedIds }
            }

            // Dual-Categorized Flow Stream
            EnergyTimelineStream(
                scheduledTasks = visibleStreamTasks,
                onTaskClick = { task -> onStartFocus(task.id) },
                onCompleteTask = { task ->
                    locallyCompletedIds = locallyCompletedIds + task.id
                    if (task.isFrog) showFrogConfetti = true
                    viewModel.completeTask(task)
                },
                onDeferTask = { task -> viewModel.deferTask(task) },
                onBreakDownTask = { task -> viewModel.breakDownTask(task) },
                onEditTask = { task -> editingTask = task },
                onShiftToPeak = { task -> viewModel.shiftTaskToPeak(task) },
                timeline = state.energyTimeline,
                fatigueScores = state.fatigueScoresByTaskId,
                nowMillis = nowMillis,
                groupingMode = state.groupingMode,
                priorityScoresByTaskId = state.priorityScoresByTaskId,
                currentEnergyLevel = when {
                    battery >= 65 -> EnergyLevel.HIGH
                    battery >= 35 -> EnergyLevel.MEDIUM
                    else -> EnergyLevel.LOW
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
        }
    }

    if (showComposer) {
        SmartTaskComposerSheetV2(
            onDismiss = { showComposer = false },
            mode = SheetMode.Create,
            circadianSummary = state.circadianSummary,
            onSave = { task ->
                showComposer = false
                viewModel.addTask(task)
            },
            completedTasks = state.completedTasks,
            activeTasks = state.activeTasks,
            energyTimeline = state.energyTimeline,
            preferences = state.preferences,
            burnoutRiskState = state.burnoutRiskState
        )
    }

    editingTask?.let { task ->
        SmartTaskComposerSheetV2(
            onDismiss = { editingTask = null },
            mode = SheetMode.Edit(task),
            circadianSummary = state.circadianSummary,
            onSave = { updated ->
                editingTask = null
                viewModel.updateTask(updated)
            },
            completedTasks = state.completedTasks,
            activeTasks = state.activeTasks,
            energyTimeline = state.energyTimeline,
            preferences = state.preferences,
            burnoutRiskState = state.burnoutRiskState
        )
    }

    if (showNotesSheet) {
        QuickNotesBottomSheet(
            notes = recentNotes,
            onSaveNote = { title, content -> viewModel.saveQuickNote(content, title) },
            onDismiss = { showNotesSheet = false },
            onOpenFullNotes = onNavigateToNotes,
            onCreateTaskSynthesis = { syn, content -> viewModel.addTaskFromNote(syn, content) },
            onCreateTask = { title, desc -> viewModel.addTaskFromNote(title, desc) },
            onSynthesizeNote = { viewModel.synthesizeQuickNote(it) }
        )
    }

    if (state.dailyBlueprint != null || state.isBlueprintLoading) {
        DailyBlueprintBottomSheet(
            blueprint = state.dailyBlueprint,
            isLoading = state.isBlueprintLoading,
            includedTaskIds = state.blueprintIncludedTaskIds,
            onToggleTask = { taskId, included -> viewModel.toggleBlueprintTask(taskId, included) },
            onApply = { viewModel.applyDailyBlueprint() },
            onDismiss = { viewModel.dismissDailyBlueprint() },
            fatigueScoresByTaskId = state.fatigueScoresByTaskId,
            onRegenerate = { viewModel.openDailyBlueprint() }
        )
    }

    if (showEasyWinSheet) {
        EasyWinPickerSheet(
            easyWinTasks = run {
                val completedSet = state.completedTasks.map { it.id }.toSet() + locallyCompletedIds
                state.activeTasks
                    .filter { task ->
                        task.status == TaskStatus.ACTIVE &&
                            task.quadrant != Quadrant.ELIMINATE &&
                            task.id !in completedSet &&
                            task.waitingFor.isBlank() &&
                            (task.energyLevel == EnergyLevel.LOW || TaskBlockPlan.plannedMinutes(task) <= 15)
                    }
                    .sortedWith(
                        compareBy<TaskEntity> { TaskBlockPlan.plannedMinutes(it) }
                            .thenBy { it.energyLevel != EnergyLevel.LOW }
                    )
            },
            onStartTask = onStartFocus,
            onDoorwayStartTask = { id -> (onStartDoorwayFocus ?: onStartFocus)(id) },
            onDismiss = { showEasyWinSheet = false }
        )
    }

    FrogConfettiOverlay(trigger = showFrogConfetti)
    LaunchedEffect(showFrogConfetti) {
        if (showFrogConfetti) {
            kotlinx.coroutines.delay(2500)
            showFrogConfetti = false
        }
    }
}
