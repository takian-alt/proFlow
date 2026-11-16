package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.plan.DailyBlueprintBottomSheet
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
    initialOpenComposer: Boolean = false,
    onInitialComposerConsumed: () -> Unit = {},
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    var showComposer by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var locallyCompletedIds by remember { mutableStateOf(emptySet<String>()) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeuroFlowThemeV2Colors.ElectricIndigo)
                    .combinedClickable(
                        onClick = { showComposer = true },
                        onLongClick = { showNotesSheet = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Quick Add Task (Long-press for Notes)",
                    tint = NeuroFlowThemeV2Colors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPanelPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPanelPadding)
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
                Column {
                    Text(
                        text = "neuroFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Biological High Performance",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToMindmap,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Mind Map Planning Canvas",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }

                    IconButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Quick Scratchpad Notes",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(
                color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.12f),
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
                onClearPhysiologicalEvents = { viewModel.clearAllPhysiologicalEvents() }
            )

            // Active Biophysical Impulse Strip (Meal, Workout, Nap, Stimulant PK ticker)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActiveBiophysicalImpulseStrip(
                    cognitiveBlocks = state.dailyCognitiveBlocks,
                    activePlasmaMg = state.activePlasmaMg
                )
            }

            // Flow Spotlight Hero Card (Takeover or Top Action)
            val nowMinute by produceState(System.currentTimeMillis() / 60_000L) {
                while (true) {
                    kotlinx.coroutines.delay(60_000L)
                    value = System.currentTimeMillis() / 60_000L
                }
            }
            val nowMillis = remember(nowMinute) { nowMinute * 60_000L }
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
            val currentPhase = state.circadianSummary?.currentSnapshot?.phase
                ?: state.energyTimeline.minByOrNull {
                    kotlin.math.abs(it.timestampMillis - nowMillis)
                }?.phase ?: EnergyPhase.PEAK

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
                        onShutdownClick = { viewModel.completeEveningShutdown() }
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
            val resetPlan = remember(state.activeTasks, state.showZeroGuiltPrompt) {
                val hasOverdue = state.showZeroGuiltPrompt || state.activeTasks.any { it.postponeCount >= 2 }
                if (hasOverdue) {
                    FreshStartEngine.buildEmergencyResetPlan(
                        activeTasks = state.activeTasks,
                        nowMillis = System.currentTimeMillis()
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

            // Evening Shutdown Ritual Card
            if (state.isEveningShutdownTime && !state.eveningShutdownDismissed) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌙", fontSize = 18.sp)
                            Text(
                                text = "Evening Shutdown Ritual",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
                        val todayCompletedCount = state.completedTasks.count {
                            val comp = it.completedAt ?: it.updatedAt
                            comp >= todayStart
                        }
                        Text(
                            text = "It's past 7:00 PM. Protect your circadian melatonin surge by closing cognitive loops. You conquered $todayCompletedCount task${if (todayCompletedCount == 1) "" else "s"} today. Time to wind down.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Button(
                            onClick = { viewModel.completeEveningShutdown() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Complete Shutdown", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            labelColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Energy-Mapped Day Stream
            EnergyTimelineStream(
                scheduledTasks = state.filteredTasks,
                onTaskClick = { task -> onStartFocus(task.id) },
                onCompleteTask = { task ->
                    if (task.isFrog) showFrogConfetti = true
                    viewModel.completeTask(task)
                },
                onDeferTask = { task -> viewModel.deferTask(task) },
                onBreakDownTask = { task -> viewModel.breakDownTask(task) },
                onEditTask = { task -> editingTask = task },
                onShiftToPeak = { task -> viewModel.shiftTaskToPeak(task) },
                timeline = state.energyTimeline
            )

            Spacer(modifier = Modifier.height(80.dp))
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
            completedTasks = state.completedTasks
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
            completedTasks = state.completedTasks
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
            onDismiss = { viewModel.dismissDailyBlueprint() }
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
