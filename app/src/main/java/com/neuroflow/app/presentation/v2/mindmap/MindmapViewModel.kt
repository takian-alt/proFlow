package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.MindmapEdgeEntity
import com.neuroflow.app.data.local.entity.MindmapNodePositionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.MindmapRepository
import com.neuroflow.app.data.repository.NodeConnection
import com.neuroflow.app.domain.assistant.GoalBreakdownEngine
import com.neuroflow.app.domain.assistant.GoalBreakdownMilestone
import com.neuroflow.app.domain.assistant.TaskSlotRecommender
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MindmapViewModel @Inject constructor(
    private val repository: MindmapRepository,
    private val preferencesDataStore: UserPreferencesDataStore? = null
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)
    private val _layoutMode = MutableStateFlow(MindmapLayoutMode.HORIZONTAL)
    private val _connectionPrompt = MutableStateFlow<Pair<MindmapNode, MindmapNode>?>(null)
    private val _connectingSourceNodeId = MutableStateFlow<String?>(null)
    private val _collapsedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    private val _draggingNodeId = MutableStateFlow<String?>(null)
    private val _dropTargetNodeId = MutableStateFlow<String?>(null)
    private val _inlineSproutParentNode = MutableStateFlow<MindmapNode?>(null)
    private val _inlineSproutMode = MutableStateFlow(SproutMode.CHILD)
    private val _searchQuery = MutableStateFlow("")
    private val _disconnectTargetNode = MutableStateFlow<MindmapTaskNode?>(null)
    private val _disconnectConnections = MutableStateFlow<List<NodeConnection>>(emptyList())
    private val _isGoalCreateOpen = MutableStateFlow(false)
    private val _editingGoalNode = MutableStateFlow<MindmapGoalNode?>(null)
    private val _selectedGoalId = MutableStateFlow<String?>(null)
    private val _workspaceMode = MutableStateFlow(GoalWorkspaceMode.OUTLINE)
    private val _showAllGoalsInMap = MutableStateFlow(false)
    private val _breakdownPreview = MutableStateFlow<List<GoalBreakdownMilestone>?>(null)
    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    private val _actionMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(MindmapUiState())
    val uiState: StateFlow<MindmapUiState> = _uiState.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> =
        (preferencesDataStore?.preferencesFlow ?: kotlinx.coroutines.flow.flowOf(UserPreferences()))
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), UserPreferences())

    // Local volatile cache of dragging offsets
    private val temporaryPositions = mutableMapOf<String, Offset>()

    // Cache of latest data from repository
    private var cachedGoals: List<GoalEntity> = emptyList()
    private var cachedTasks: List<TaskEntity> = emptyList()
    private var cachedPositions: List<MindmapNodePositionEntity> = emptyList()
    private var cachedEdges: List<MindmapEdgeEntity> = emptyList()
    private var hasInitializedDefaultCollapse = false

    private data class RepoData(
        val goals: List<GoalEntity>,
        val tasks: List<TaskEntity>,
        val positions: List<MindmapNodePositionEntity>,
        val edges: List<MindmapEdgeEntity>,
        val showCompleted: Boolean
    )

    init {
        viewModelScope.launch {
            val repositoryDataFlow = _showCompleted
                .flatMapLatest { showComp ->
                    combine(
                        repository.observeGoals(),
                        repository.observeTasks(showComp),
                        repository.observePositions(),
                        repository.observeEdges()
                    ) { goals, tasks, positions, edges ->
                        RepoData(goals, tasks, positions, edges, showComp)
                    }
                }

            val workspaceStateFlow = combine(
                combine(_selectedGoalId, _workspaceMode, _showAllGoalsInMap) { id, mode, all -> Triple(id, mode, all) },
                combine(_breakdownPreview, _selectedTagFilter, _actionMessage) { bp, tag, msg -> Triple(bp, tag, msg) }
            ) { (selectedGoalId, workspaceMode, showAll), (breakdown, tagFilter, msg) ->
                WorkspacePrefs(selectedGoalId, workspaceMode, showAll, breakdown, tagFilter, msg)
            }

            combine(
                combine(
                    repositoryDataFlow,
                    _collapsedNodeIds,
                    _layoutMode,
                    _searchQuery,
                    _inlineSproutMode
                ) { repo, collapsed, mode, query, sproutMode ->
                    CanvasInputs(repo, collapsed, mode, query, sproutMode)
                },
                workspaceStateFlow
            ) { canvas, workspace ->
                val repo = canvas.repo
                cachedGoals = repo.goals
                cachedTasks = repo.tasks
                cachedPositions = repo.positions
                cachedEdges = repo.edges

                val effectiveCollapsed = if (!hasInitializedDefaultCollapse && (repo.goals.isNotEmpty() || repo.tasks.isNotEmpty())) {
                    hasInitializedDefaultCollapse = true
                    val defaults = computeDefaultCollapsedNodeIds(repo.goals, repo.tasks)
                    _collapsedNodeIds.value = defaults
                    defaults
                } else {
                    canvas.collapsed
                }

                computeUiState(
                    goals = repo.goals,
                    tasks = repo.tasks,
                    positions = repo.positions,
                    edges = repo.edges,
                    showCompleted = repo.showCompleted,
                    collapsedNodeIds = effectiveCollapsed,
                    layoutMode = canvas.mode,
                    searchQuery = canvas.query,
                    inlineSproutMode = canvas.sproutMode,
                    selectedGoalId = workspace.selectedGoalId,
                    workspaceMode = workspace.workspaceMode,
                    showAllGoalsInMap = workspace.showAllGoalsInMap,
                    breakdownPreview = workspace.breakdownPreview,
                    selectedTagFilter = workspace.selectedTagFilter,
                    actionMessage = workspace.actionMessage
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private data class WorkspacePrefs(
        val selectedGoalId: String?,
        val workspaceMode: GoalWorkspaceMode,
        val showAllGoalsInMap: Boolean,
        val breakdownPreview: List<GoalBreakdownMilestone>?,
        val selectedTagFilter: String?,
        val actionMessage: String?
    )

    private data class CanvasInputs(
        val repo: RepoData,
        val collapsed: Set<String>,
        val mode: MindmapLayoutMode,
        val query: String,
        val sproutMode: SproutMode
    )

    internal fun computeDefaultCollapsedNodeIds(goals: List<GoalEntity>, tasks: List<TaskEntity>): Set<String> {
        val collapsed = mutableSetOf<String>()
        // 1. Only auto-collapse legacy curriculum boilerplate stages (Stage 2..4); never auto-collapse user-created goals!
        goals.forEach { goal ->
            val isLegacyLaterStage = goal.id.startsWith("goal_stage_", ignoreCase = true) &&
                !goal.id.equals("goal_stage_1", ignoreCase = true)
            if (isLegacyLaterStage) {
                collapsed.add(goal.id)
            }
        }

        // 2. Subtrees: Only auto-collapse legacy curriculum month subtrees beyond Month 1
        val parentIds = tasks.mapNotNull { it.parentTaskId }.toSet()
        tasks.forEach { task ->
            if (task.id in parentIds) {
                val isLegacyLaterMonth = task.id.startsWith("task_s", ignoreCase = true) &&
                    !task.id.equals("task_s1_m1", ignoreCase = true)
                if (isLegacyLaterMonth) {
                    collapsed.add(task.id)
                }
            }
        }
        return collapsed
    }

    internal fun findActiveStartingNodeId(tasks: List<TaskEntity>): String? {
        val uncompleted = tasks.filter { it.status != TaskStatus.COMPLETED }
        if (uncompleted.isEmpty()) return null

        val taskMap = tasks.associateBy { it.id }
        val parentIds = tasks.mapNotNull { it.parentTaskId }.toSet()
        // Actionable leaf tasks
        val actionableTasks = uncompleted.filter { it.id !in parentIds }
        val candidatePool = actionableTasks.ifEmpty { uncompleted }

        val prereqsMet = candidatePool.filter { task ->
            val prereqIds = task.dependsOnTaskIds.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            prereqIds.all { pId ->
                val pTask = taskMap[pId]
                pTask == null || pTask.status == TaskStatus.COMPLETED
            }
        }

        val pool = prereqsMet.ifEmpty { candidatePool }
        val curriculumTasks = pool.filter { com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.isCurriculumTask(it) }
        val finalPool = curriculumTasks.ifEmpty { pool }

        val sorted = finalPool.sortedWith(
            compareBy<TaskEntity> { task ->
                when {
                    task.id.startsWith("task_s1_m1") || task.parentTaskId == "task_s1_m1" -> 0
                    task.tags.contains("month1", ignoreCase = true) -> 0
                    task.tags.contains("stage1", ignoreCase = true) -> 1
                    task.tags.contains("stage2", ignoreCase = true) -> 2
                    task.tags.contains("stage3", ignoreCase = true) -> 3
                    task.tags.contains("stage4", ignoreCase = true) -> 4
                    else -> 5
                }
            }.thenBy { task ->
                task.scheduledDate ?: Long.MAX_VALUE
            }.thenBy { task ->
                task.id
            }
        )

        return sorted.firstOrNull()?.id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    fun toggleLayoutMode() {
        val newMode = if (_layoutMode.value == MindmapLayoutMode.HORIZONTAL) {
            MindmapLayoutMode.RADIAL
        } else {
            MindmapLayoutMode.HORIZONTAL
        }
        applyAutoLayout(newMode)
    }

    fun toggleCollapse(nodeId: String) {
        _collapsedNodeIds.update { current ->
            if (current.contains(nodeId)) {
                current - nodeId
            } else {
                current + nodeId
            }
        }
    }

    fun collapseAll() {
        val allParentIds = mutableSetOf<String>()
        cachedGoals.forEach { allParentIds.add(it.id) }
        val taskParents = cachedTasks.mapNotNull { it.parentTaskId }.toSet()
        allParentIds.addAll(taskParents)
        _collapsedNodeIds.value = allParentIds
    }

    fun expandAll() {
        _collapsedNodeIds.value = emptySet()
    }

    fun resetToDefaultCollapse() {
        _collapsedNodeIds.value = computeDefaultCollapsedNodeIds(cachedGoals, cachedTasks)
    }

    fun openInlineSprout(parentNode: MindmapNode, mode: SproutMode = SproutMode.CHILD) {
        _inlineSproutParentNode.value = parentNode
        _inlineSproutMode.value = mode
        _uiState.update { it.copy(inlineSproutParentNode = parentNode, inlineSproutMode = mode) }
    }

    fun dismissInlineSprout() {
        _inlineSproutParentNode.value = null
        _uiState.update { it.copy(inlineSproutParentNode = null) }
    }

    fun spawnChildTask(parentId: String, title: String, priority: Priority = Priority.MEDIUM) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            // Uncollapse parent so the new child is visible
            _collapsedNodeIds.update { it - parentId }
            val newTask = repository.spawnChildTask(parentId, cleanTitle, priority)
            dismissInlineSprout()

            // Repack layout so new child gets clean position without collision
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun spawnSiblingTask(referenceTaskId: String, title: String, priority: Priority = Priority.MEDIUM) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            val refTask = repository.getTaskById(referenceTaskId)
            refTask?.parentTaskId?.let { pId ->
                _collapsedNodeIds.update { it - pId }
            }
            refTask?.goalId?.let { gId ->
                _collapsedNodeIds.update { it - gId }
            }
            repository.spawnSiblingTask(referenceTaskId, cleanTitle, priority)
            dismissInlineSprout()
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun reparentTask(childId: String, newParentId: String) {
        if (childId == newParentId) return
        viewModelScope.launch {
            repository.reparentTask(childId, newParentId)
            temporaryPositions.clear()
            _collapsedNodeIds.update { it - newParentId }
            recomputeAndPersistLayout()
        }
    }

    fun openDisconnectPicker(task: MindmapTaskNode) {
        viewModelScope.launch {
            val connections = repository.getConnectionsForNode(task.id)
            _disconnectTargetNode.value = task
            _disconnectConnections.value = connections
            _uiState.update {
                it.copy(
                    disconnectTargetNode = task,
                    disconnectConnections = connections
                )
            }
        }
    }

    fun dismissDisconnectPicker() {
        _disconnectTargetNode.value = null
        _disconnectConnections.value = emptyList()
        _uiState.update {
            it.copy(
                disconnectTargetNode = null,
                disconnectConnections = emptyList()
            )
        }
    }

    fun removeSingleConnection(nodeId: String, connection: NodeConnection) {
        viewModelScope.launch {
            repository.removeSingleConnection(nodeId, connection)
            temporaryPositions.clear()
            val updated = repository.getConnectionsForNode(nodeId)
            _disconnectConnections.value = updated
            _uiState.update { it.copy(disconnectConnections = updated) }
            recomputeAndPersistLayout()
        }
    }

    fun openGoalCreateDialog() {
        _isGoalCreateOpen.value = true
        _uiState.update { it.copy(isGoalCreateOpen = true) }
    }

    fun dismissGoalCreateDialog() {
        _isGoalCreateOpen.value = false
        _uiState.update { it.copy(isGoalCreateOpen = false) }
    }

    fun openGoalEditDialog(goal: MindmapGoalNode) {
        _editingGoalNode.value = goal
        _uiState.update { it.copy(editingGoalNode = goal) }
    }

    fun dismissGoalEditDialog() {
        _editingGoalNode.value = null
        _uiState.update { it.copy(editingGoalNode = null) }
    }

    fun createGoal(
        title: String,
        description: String = "",
        targetDate: Long? = null,
        priority: Priority = Priority.MEDIUM,
        tags: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            repository.createGoal(
                title = cleanTitle,
                description = description,
                targetDate = targetDate,
                priority = priority,
                tags = tags
            )
            dismissGoalCreateDialog()
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun updateGoal(
        goalId: String,
        title: String,
        description: String = "",
        targetDate: Long? = null,
        priority: Priority = Priority.MEDIUM,
        tags: String = "",
        syncTagsToExistingTasks: Boolean = false
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(
                existing.copy(
                    title = cleanTitle,
                    description = description,
                    targetDate = targetDate,
                    priority = priority,
                    tags = tags
                )
            )
            if (syncTagsToExistingTasks && tags.isNotBlank()) {
                repository.syncGoalTagsToTasks(goalId, tags)
            }
            dismissGoalEditDialog()
            temporaryPositions.clear()
            recomputeAndPersistLayout()
            _actionMessage.value = "Updated goal '$cleanTitle'"
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
            temporaryPositions.clear()
            _collapsedNodeIds.update { it - goalId }
            if (_selectedGoalId.value == goalId) {
                _selectedGoalId.value = null
            }
            dismissGoalEditDialog()
            recomputeAndPersistLayout()
        }
    }

    // ─── Paper-Fast Goals & Outline Workspace Actions ─────────────────────

    fun selectGoal(goalId: String) {
        _selectedGoalId.value = goalId
        _workspaceMode.value = GoalWorkspaceMode.OUTLINE
        _collapsedNodeIds.update { it - goalId }
    }

    fun clearSelectedGoal() {
        _selectedGoalId.value = null
        _breakdownPreview.value = null
    }

    fun setWorkspaceMode(mode: GoalWorkspaceMode) {
        _workspaceMode.value = mode
    }

    fun toggleShowAllGoalsInMap() {
        _showAllGoalsInMap.value = !_showAllGoalsInMap.value
    }

    fun setGoalTagFilter(tag: String?) {
        _selectedTagFilter.value = tag?.takeIf { it.isNotBlank() }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun quickCreateGoalAndOpen(
        title: String,
        priority: Priority = Priority.MEDIUM,
        tags: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            val goal = repository.createGoal(
                title = cleanTitle,
                priority = priority,
                tags = tags
            )
            _collapsedNodeIds.update { it - goal.id }
            _selectedGoalId.value = goal.id
            _workspaceMode.value = GoalWorkspaceMode.OUTLINE
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun quickAddOutlineStep(
        goalId: String,
        title: String,
        parentTaskId: String? = null,
        priority: Priority? = null,
        tags: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            val targetParent = parentTaskId ?: goalId
            _collapsedNodeIds.update { it - goalId - targetParent }
            repository.spawnChildTask(targetParent, cleanTitle, priority, tags)
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun cycleTaskPriority(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val next = when (task.priority) {
                Priority.LOW -> Priority.MEDIUM
                Priority.MEDIUM -> Priority.HIGH
                Priority.HIGH -> Priority.LOW
            }
            repository.updateTaskPriority(taskId, next)
        }
    }

    fun setTaskPriority(taskId: String, priority: Priority) {
        viewModelScope.launch {
            repository.updateTaskPriority(taskId, priority)
        }
    }

    fun setTaskTags(taskId: String, tags: String) {
        viewModelScope.launch {
            repository.updateTaskTags(taskId, tags)
        }
    }

    fun indentOutlineTask(taskId: String, newParentTaskId: String) {
        if (taskId == newParentTaskId) return
        viewModelScope.launch {
            repository.reparentTask(taskId, newParentTaskId)
            _collapsedNodeIds.update { it - newParentTaskId }
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun outdentOutlineTask(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val parentId = task.parentTaskId ?: return@launch
            val parent = repository.getTaskById(parentId)
            val newParentId = parent?.parentTaskId ?: task.goalId ?: return@launch
            repository.reparentTask(taskId, newParentId)
            temporaryPositions.clear()
            recomputeAndPersistLayout()
        }
    }

    fun requestSmartBreakdown(goalId: String) {
        val goal = cachedGoals.firstOrNull { it.id == goalId } ?: return
        _breakdownPreview.value = GoalBreakdownEngine.generateLocalBreakdown(
            goalTitle = goal.title,
            goalDescription = goal.description
        )
    }

    fun dismissSmartBreakdown() {
        _breakdownPreview.value = null
    }

    fun applySmartBreakdown(goalId: String, milestones: List<GoalBreakdownMilestone>) {
        if (milestones.isEmpty()) {
            _breakdownPreview.value = null
            return
        }
        viewModelScope.launch {
            var createdCount = 0
            milestones.forEach { milestone ->
                val parentTask = repository.spawnChildTask(goalId, milestone.title, Priority.HIGH)
                createdCount++
                milestone.subtasks.forEach { sub ->
                    repository.spawnChildTask(parentTask.id, sub, Priority.MEDIUM)
                    createdCount++
                }
            }
            _collapsedNodeIds.update { it - goalId }
            _breakdownPreview.value = null
            temporaryPositions.clear()
            recomputeAndPersistLayout()
            _actionMessage.value = "Added $createdCount structured steps to goal"
        }
    }

    fun pushTaskToToday(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val now = System.currentTimeMillis()
            val prefs = runCatching { preferencesDataStore?.preferencesFlow?.first() }.getOrNull() ?: UserPreferences()
            val slots = TaskSlotRecommender.recommendSlots(
                durationMinutes = task.estimatedDurationMinutes.coerceAtLeast(15),
                energyLevel = task.energyLevel,
                targetDateMillis = null,
                deadlineMillis = task.deadlineDate,
                existingTasks = cachedTasks,
                energyTimeline = emptyList(),
                prefs = prefs,
                nowMillis = now
            )
            val chosen = slots.firstOrNull()
            val updated = if (chosen != null) {
                task.copy(
                    scheduledDate = chosen.dayStartMillis,
                    scheduledTime = chosen.timeOfDayOffsetMillis,
                    updatedAt = now
                )
            } else {
                task.copy(
                    scheduledDate = startOfLocalDay(now),
                    updatedAt = now
                )
            }
            repository.updateTask(updated)
            val slotLabel = chosen?.let { "${it.dayLabel} ${it.timeRangeLabel}" } ?: "Today"
            _actionMessage.value = "Scheduled '${task.title}' → $slotLabel"
        }
    }

    /**
     * Auto-schedules up to [maxSteps] unscheduled actionable steps under a goal into open
     * energy-matched slots, ordered by step priority (HIGH -> MEDIUM -> LOW) and respecting
     * existing schedule commitments without overlap.
     */
    fun autoScheduleGoalNextSteps(goalId: String, maxSteps: Int = 3) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId)
            val allActive = cachedTasks.filter { it.status != TaskStatus.COMPLETED }
            val parentIds = allActive.mapNotNull { it.parentTaskId }.toSet()
            val goalCandidates = allActive
                .filter { it.goalId == goalId && it.scheduledDate == null }
                .let { list ->
                    val leaves = list.filter { it.id !in parentIds }
                    leaves.ifEmpty { list }
                }
                .sortedWith(
                    compareBy<TaskEntity> { priorityRank(it.priority) }
                        .thenBy { it.createdAt }
                )
                .take(maxSteps)

            if (goalCandidates.isEmpty()) {
                _actionMessage.value = "All active steps under this goal are already scheduled"
                return@launch
            }

            val now = System.currentTimeMillis()
            val prefs = runCatching { preferencesDataStore?.preferencesFlow?.first() }.getOrNull() ?: UserPreferences()
            val workingTasks = cachedTasks.toMutableList()
            var scheduledCount = 0

            for (task in goalCandidates) {
                val slots = TaskSlotRecommender.recommendSlots(
                    durationMinutes = task.estimatedDurationMinutes.coerceAtLeast(25),
                    energyLevel = task.energyLevel,
                    targetDateMillis = null,
                    deadlineMillis = task.deadlineDate ?: goal?.targetDate,
                    existingTasks = workingTasks,
                    energyTimeline = emptyList(),
                    prefs = prefs,
                    nowMillis = now,
                    excludeTaskId = task.id
                )
                val chosen = slots.firstOrNull() ?: continue
                val updated = task.copy(
                    scheduledDate = chosen.dayStartMillis,
                    scheduledTime = chosen.timeOfDayOffsetMillis,
                    isAutoScheduled = true,
                    lastAutoScheduledAt = now,
                    updatedAt = now
                )
                repository.updateTask(updated)
                val idx = workingTasks.indexOfFirst { it.id == task.id }
                if (idx >= 0) workingTasks[idx] = updated else workingTasks.add(updated)
                scheduledCount++
            }

            _actionMessage.value = if (scheduledCount > 0) {
                "Auto-scheduled $scheduledCount prioritized step${if (scheduledCount == 1) "" else "s"} into open slots"
            } else {
                "No open energy slots found — adjust work hours or enable Overtime"
            }
        }
    }

    private fun priorityRank(priority: Priority): Int = when (priority) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
    }

    fun applyAutoLayout(mode: MindmapLayoutMode? = null) {
        if (mode != null) {
            _layoutMode.value = mode
        }
        temporaryPositions.clear()
        viewModelScope.launch {
            recomputeAndPersistLayout()
        }
    }

    private suspend fun recomputeAndPersistLayout() {
        val autoPositions = MindmapTreeLayoutEngine.computeLayout(
            goals = cachedGoals,
            tasks = cachedTasks,
            collapsedNodeIds = _collapsedNodeIds.value,
            layoutMode = _layoutMode.value
        )
        val positionEntities = autoPositions.map { (entityId, pos) ->
            val entityType = if (cachedGoals.any { it.id == entityId }) "GOAL" else "TASK"
            MindmapNodePositionEntity(
                entityId = entityId,
                entityType = entityType,
                x = pos.x,
                y = pos.y,
                updatedAt = System.currentTimeMillis()
            )
        }
        repository.savePositions(positionEntities)
    }

    fun onNodeDrag(entityId: String, offset: Offset) {
        temporaryPositions[entityId] = offset
        val current = _uiState.value

        // Hit-test for drop targets (drag-to-reparent)
        val allVisibleNodes = (current.goals as List<MindmapNode> + current.tasks)
        val descendants = getDescendantIds(entityId, current.tasks)

        val candidate = allVisibleNodes.firstOrNull { target ->
            if (target.id == entityId) return@firstOrNull false
            if (target.id in descendants) return@firstOrNull false // Cycle avoidance

            val draggingCenter = Offset(offset.x + 100f, offset.y + 35f)
            val targetCenter = Offset(target.x + 100f, target.y + 35f)
            val dx = draggingCenter.x - targetCenter.x
            val dy = draggingCenter.y - targetCenter.y
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            dist < 110f // Hover detection threshold
        }

        val targetId = candidate?.id
        _draggingNodeId.value = entityId
        _dropTargetNodeId.value = targetId

        val newGoals = current.goals.map {
            val isTarget = it.id == targetId
            if (it.id == entityId) it.copy(x = offset.x, y = offset.y, isDropTarget = isTarget)
            else it.copy(isDropTarget = isTarget)
        }
        val newTasks = current.tasks.map {
            val isTarget = it.id == targetId
            if (it.id == entityId) it.copy(x = offset.x, y = offset.y, isDropTarget = isTarget)
            else it.copy(isDropTarget = isTarget)
        }

        _uiState.update { state ->
            state.copy(
                draggingNodeId = entityId,
                dropTargetNodeId = targetId,
                goals = newGoals,
                tasks = newTasks,
                edges = recalculateEdges(newGoals, newTasks, state.edges)
            )
        }
    }

    fun onNodeDragEnd(entityId: String, entityType: String, finalOffset: Offset) {
        val targetId = _dropTargetNodeId.value
        _draggingNodeId.value = null
        _dropTargetNodeId.value = null

        if (targetId != null) {
            // Drag-to-reparent drop!
            reparentTask(entityId, targetId)
        } else {
            temporaryPositions[entityId] = finalOffset
            viewModelScope.launch {
                repository.savePosition(entityId, entityType, finalOffset.x, finalOffset.y)
            }
        }
    }

    fun onToggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
        }
    }

    fun onConnectNodes(sourceId: String, targetId: String) {
        if (sourceId == targetId) return
        val current = _uiState.value
        val sourceNode = (current.goals as List<MindmapNode> + current.tasks).find { it.id == sourceId } ?: return
        val targetNode = (current.goals as List<MindmapNode> + current.tasks).find { it.id == targetId } ?: return

        if (sourceNode is MindmapGoalNode && targetNode is MindmapTaskNode) {
            // Direct Goal -> Task link
            viewModelScope.launch {
                repository.linkTaskToGoal(targetNode.id, sourceNode.id)
            }
            return
        }

        // Task -> Task or other connections trigger confirmation prompt
        _connectionPrompt.value = Pair(sourceNode, targetNode)
        _uiState.update { it.copy(connectionPrompt = Pair(sourceNode, targetNode)) }
    }

    fun confirmConnection(source: MindmapNode, target: MindmapNode, type: MindmapEdgeType) {
        viewModelScope.launch {
            when (type) {
                MindmapEdgeType.GOAL_LINK -> {
                    if (source is MindmapGoalNode && target is MindmapTaskNode) {
                        repository.linkTaskToGoal(target.id, source.id)
                    } else if (target is MindmapGoalNode && source is MindmapTaskNode) {
                        repository.linkTaskToGoal(source.id, target.id)
                    }
                }
                MindmapEdgeType.SUBTASK_LINK -> {
                    repository.reparentTask(target.id, source.id)
                }
                MindmapEdgeType.DEPENDENCY -> {
                    repository.addTaskDependency(target.id, source.id)
                }
                MindmapEdgeType.VISUAL -> {
                    repository.createEdge(source.id, target.id, "VISUAL")
                }
            }
            dismissConnectionPrompt()
        }
    }

    fun dismissConnectionPrompt() {
        _connectionPrompt.value = null
        _uiState.update { it.copy(connectionPrompt = null) }
    }

    fun toggleConnectMode(nodeId: String) {
        if (_connectingSourceNodeId.value == nodeId) {
            _connectingSourceNodeId.value = null
        } else {
            _connectingSourceNodeId.value = nodeId
        }
        _uiState.update { it.copy(connectingSourceNodeId = _connectingSourceNodeId.value) }
    }

    fun cancelConnectMode() {
        _connectingSourceNodeId.value = null
        _uiState.update { it.copy(connectingSourceNodeId = null) }
    }

    fun handleNodeClick(nodeId: String, onActionSheet: (String) -> Unit) {
        val sourceId = _connectingSourceNodeId.value
        if (sourceId != null) {
            if (sourceId != nodeId) {
                onConnectNodes(sourceId, nodeId)
            }
            cancelConnectMode()
        } else {
            onActionSheet(nodeId)
        }
    }

    fun addTask(task: TaskEntity, position: Offset) {
        viewModelScope.launch {
            repository.addTask(task, position.x, position.y)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun disconnectNode(nodeId: String) {
        viewModelScope.launch {
            repository.disconnectNode(nodeId)
        }
    }

    suspend fun getTaskEntity(taskId: String): TaskEntity? {
        return repository.getTaskById(taskId)
    }

    private fun getDescendantIds(nodeId: String, allTasks: List<MindmapTaskNode>): Set<String> {
        val descendants = mutableSetOf<String>()
        fun collect(id: String) {
            val directChildren = allTasks.filter { it.parentTaskId == id }
            for (child in directChildren) {
                if (descendants.add(child.id)) {
                    collect(child.id)
                }
            }
        }
        collect(nodeId)
        return descendants
    }

    private fun computeUiState(
        goals: List<GoalEntity>,
        tasks: List<TaskEntity>,
        positions: List<MindmapNodePositionEntity>,
        edges: List<MindmapEdgeEntity>,
        showCompleted: Boolean,
        collapsedNodeIds: Set<String>,
        layoutMode: MindmapLayoutMode,
        searchQuery: String = "",
        inlineSproutMode: SproutMode = SproutMode.CHILD,
        selectedGoalId: String? = null,
        workspaceMode: GoalWorkspaceMode = GoalWorkspaceMode.OUTLINE,
        showAllGoalsInMap: Boolean = false,
        breakdownPreview: List<GoalBreakdownMilestone>? = null,
        selectedTagFilter: String? = null,
        actionMessage: String? = null
    ): MindmapUiState {
        // Compute goal summaries across ALL goals for the Goals Overview screen (sorted by Goal Priority first)
        val parentTaskIdsAll = tasks.mapNotNull { it.parentTaskId }.toSet()
        val goalSummaries = goals
            .sortedWith(
                compareBy<GoalEntity> { priorityRank(it.priority) }
                    .thenByDescending { it.createdAt }
            )
            .map { goal ->
                val goalTasks = tasks.filter { it.goalId == goal.id }
                val completedCount = goalTasks.count { it.status == TaskStatus.COMPLETED }
                val activeGoalTasks = goalTasks.filter { it.status != TaskStatus.COMPLETED }
                val leafActive = activeGoalTasks.filter { it.id !in parentTaskIdsAll }
                val nextAction = (leafActive.ifEmpty { activeGoalTasks })
                    .sortedWith(
                        compareBy<TaskEntity> { priorityRank(it.priority) }
                            .thenBy { it.scheduledDate == null }
                            .thenBy { it.scheduledDate ?: Long.MAX_VALUE }
                            .thenBy { it.createdAt }
                    )
                    .firstOrNull()
                GoalSummaryUi(
                    goal = goal,
                    totalTasks = goalTasks.size,
                    completedTasks = completedCount,
                    nextActionableTask = nextAction
                )
            }

        // Scope Mindmap Canvas to the active selected goal unless user explicitly toggled "All Goals"
        val scopedGoals = if (selectedGoalId != null && !showAllGoalsInMap) {
            goals.filter { it.id == selectedGoalId }
        } else {
            goals
        }
        val scopedTasks = if (selectedGoalId != null && !showAllGoalsInMap) {
            tasks.filter { it.goalId == selectedGoalId }
        } else {
            tasks
        }

        val posMap = positions.associateBy { it.entityId }
        val taskMap = scopedTasks.associateBy { it.id }
        val goalMap = scopedGoals.associateBy { it.id }

        // Find hidden descendants from collapsed nodes
        val hiddenTaskIds = mutableSetOf<String>()
        fun hideSubtree(taskId: String) {
            if (hiddenTaskIds.add(taskId)) {
                scopedTasks.filter { it.parentTaskId == taskId }.forEach { hideSubtree(it.id) }
            }
        }

        collapsedNodeIds.forEach { collapsedId ->
            if (goalMap.containsKey(collapsedId)) {
                scopedTasks.filter { it.goalId == collapsedId && (it.parentTaskId == null || !taskMap.containsKey(it.parentTaskId)) }
                    .forEach { hideSubtree(it.id) }
            } else if (taskMap.containsKey(collapsedId)) {
                scopedTasks.filter { it.parentTaskId == collapsedId }.forEach { hideSubtree(it.id) }
            }
        }

        val visibleTasks = scopedTasks.filter { it.id !in hiddenTaskIds }

        // Compute auto layout coordinates as fallback/default
        val autoPositions = MindmapTreeLayoutEngine.computeLayout(
            goals = scopedGoals,
            tasks = visibleTasks,
            collapsedNodeIds = collapsedNodeIds,
            layoutMode = layoutMode
        )

        // Compute search matches
        val trimmedQuery = searchQuery.trim()
        val highlightedIds = if (trimmedQuery.isNotBlank()) {
            val matchingGoals = scopedGoals.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) ||
                    it.tags.contains(trimmedQuery, ignoreCase = true)
            }.map { it.id }.toSet()
            val matchingTasks = visibleTasks.filter {
                it.title.contains(trimmedQuery, ignoreCase = true) || it.tags.contains(trimmedQuery, ignoreCase = true)
            }.map { it.id }.toSet()
            matchingGoals + matchingTasks
        } else {
            emptySet()
        }

        // 1. Build Goal Nodes
        val goalNodes = scopedGoals.mapIndexed { index, goal ->
            val savedPos = temporaryPositions[goal.id] ?: posMap[goal.id]?.let { Offset(it.x, it.y) } ?: autoPositions[goal.id]
            val x = savedPos?.x ?: (index * 420f)
            val y = savedPos?.y ?: (index * 200f)

            val associatedTasks = scopedTasks.filter { it.goalId == goal.id }
            val directTasks = scopedTasks.filter { it.goalId == goal.id && (it.parentTaskId == null || !taskMap.containsKey(it.parentTaskId)) }

            MindmapGoalNode(
                id = goal.id,
                title = goal.title,
                x = x,
                y = y,
                width = MindmapTreeLayoutEngine.GOAL_WIDTH,
                height = MindmapTreeLayoutEngine.estimateGoalHeight(goal),
                description = goal.description,
                targetDate = goal.targetDate,
                priority = goal.priority,
                tags = goal.tags,
                totalTasks = associatedTasks.size,
                completedTasks = associatedTasks.count { it.status == TaskStatus.COMPLETED },
                isCollapsed = collapsedNodeIds.contains(goal.id),
                childCount = directTasks.size,
                isDropTarget = _dropTargetNodeId.value == goal.id,
                isHighlighted = highlightedIds.contains(goal.id)
            )
        }

        // 2. Build Task Nodes with recursive depth
        fun computeDepth(task: TaskEntity, visited: Set<String> = emptySet()): Int {
            if (task.id in visited) return 0
            val parentId = task.parentTaskId
            if (parentId != null && taskMap.containsKey(parentId)) {
                return computeDepth(taskMap[parentId]!!, visited + task.id) + 1
            }
            if (task.goalId != null && goalMap.containsKey(task.goalId)) {
                return 1
            }
            return 0
        }

        val taskNodes = visibleTasks.map { task ->
            val savedPos = temporaryPositions[task.id] ?: posMap[task.id]?.let { Offset(it.x, it.y) } ?: autoPositions[task.id]
            val x = savedPos?.x ?: 200f
            val y = savedPos?.y ?: 100f

            val depList = task.dependsOnTaskIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val directSubtasks = tasks.filter { it.parentTaskId == task.id }

            MindmapTaskNode(
                id = task.id,
                title = task.title,
                x = x,
                y = y,
                width = MindmapTreeLayoutEngine.TASK_WIDTH,
                height = MindmapTreeLayoutEngine.estimateTaskHeight(task),
                priority = task.priority,
                deadlineDate = task.deadlineDate,
                tags = task.tags,
                isCompleted = task.status == TaskStatus.COMPLETED,
                goalId = task.goalId,
                parentTaskId = task.parentTaskId,
                dependsOnTaskIds = depList,
                isCollapsed = collapsedNodeIds.contains(task.id),
                childCount = directSubtasks.size,
                depth = computeDepth(task),
                isDropTarget = _dropTargetNodeId.value == task.id,
                isHighlighted = highlightedIds.contains(task.id)
            )
        }

        // 3. Synthesize Edges
        val nodeMap = (goalNodes as List<MindmapNode> + taskNodes).associateBy { it.id }
        val edgeList = mutableListOf<MindmapEdgeUi>()
        val seenPairs = mutableSetOf<Pair<String, String>>()

        taskNodes.forEach { task ->
            // Subtask parent-child edge
            if (task.parentTaskId != null && nodeMap.containsKey(task.parentTaskId)) {
                val parentNode = nodeMap[task.parentTaskId]!!
                if (seenPairs.add(Pair(parentNode.id, task.id))) {
                    edgeList.add(
                        MindmapEdgeUi(
                            id = "sub_${parentNode.id}_${task.id}",
                            sourceId = parentNode.id,
                            targetId = task.id,
                            start = Offset(parentNode.x, parentNode.y),
                            end = Offset(task.x, task.y),
                            edgeType = MindmapEdgeType.SUBTASK_LINK,
                            isCompleted = task.isCompleted
                        )
                    )
                }
            } else if (task.goalId != null && nodeMap.containsKey(task.goalId)) {
                // Goal -> Root Task edge
                val goal = nodeMap[task.goalId]!!
                if (seenPairs.add(Pair(goal.id, task.id))) {
                    edgeList.add(
                        MindmapEdgeUi(
                            id = "goal_${goal.id}_${task.id}",
                            sourceId = goal.id,
                            targetId = task.id,
                            start = Offset(goal.x, goal.y),
                            end = Offset(task.x, task.y),
                            edgeType = MindmapEdgeType.GOAL_LINK,
                            isCompleted = task.isCompleted
                        )
                    )
                }
            }

            // Task dependencies
            task.dependsOnTaskIds.forEach { depId ->
                if (nodeMap.containsKey(depId)) {
                    val parentTask = nodeMap[depId]!!
                    val isDepCompleted = (parentTask as? MindmapTaskNode)?.isCompleted == true
                    if (seenPairs.add(Pair(depId, task.id))) {
                        edgeList.add(
                            MindmapEdgeUi(
                                id = "dep_${depId}_${task.id}",
                                sourceId = depId,
                                targetId = task.id,
                                start = Offset(parentTask.x, parentTask.y),
                                end = Offset(task.x, task.y),
                                edgeType = MindmapEdgeType.DEPENDENCY,
                                isCompleted = isDepCompleted && task.isCompleted
                            )
                        )
                    }
                }
            }
        }

        // Custom saved edges
        edges.forEach { edge ->
            val src = nodeMap[edge.sourceId]
            val tgt = nodeMap[edge.targetId]
            if (src != null && tgt != null) {
                if (seenPairs.add(Pair(edge.sourceId, edge.targetId))) {
                    edgeList.add(
                        MindmapEdgeUi(
                            id = edge.id,
                            sourceId = edge.sourceId,
                            targetId = edge.targetId,
                            start = Offset(src.x, src.y),
                            end = Offset(tgt.x, tgt.y),
                            edgeType = when (edge.edgeType) {
                                "DEPENDENCY" -> MindmapEdgeType.DEPENDENCY
                                "GOAL_LINK" -> MindmapEdgeType.GOAL_LINK
                                "SUBTASK_LINK" -> MindmapEdgeType.SUBTASK_LINK
                                else -> MindmapEdgeType.VISUAL
                            },
                            isCompleted = (tgt as? MindmapTaskNode)?.isCompleted == true
                        )
                    )
                }
            }
        }

        val activeStartingId = findActiveStartingNodeId(tasks)

        return MindmapUiState(
            isLoading = false,
            showCompleted = showCompleted,
            layoutMode = layoutMode,
            goals = goalNodes,
            tasks = taskNodes,
            edges = edgeList,
            connectionPrompt = _connectionPrompt.value,
            connectingSourceNodeId = _connectingSourceNodeId.value,
            collapsedNodeIds = collapsedNodeIds,
            draggingNodeId = _draggingNodeId.value,
            dropTargetNodeId = _dropTargetNodeId.value,
            inlineSproutParentNode = _inlineSproutParentNode.value,
            inlineSproutMode = inlineSproutMode,
            activeStartingNodeId = activeStartingId,
            searchQuery = searchQuery,
            highlightedNodeIds = highlightedIds,
            disconnectTargetNode = _disconnectTargetNode.value,
            disconnectConnections = _disconnectConnections.value,
            isGoalCreateOpen = _isGoalCreateOpen.value,
            editingGoalNode = _editingGoalNode.value,
            selectedGoalId = selectedGoalId,
            workspaceMode = workspaceMode,
            showAllGoalsInMap = showAllGoalsInMap,
            goalSummaries = goalSummaries,
            allRawTasks = tasks,
            breakdownPreview = breakdownPreview,
            selectedTagFilter = selectedTagFilter,
            actionMessage = actionMessage
        )
    }

    private fun recalculateEdges(
        goals: List<MindmapGoalNode>,
        tasks: List<MindmapTaskNode>,
        existingEdges: List<MindmapEdgeUi>
    ): List<MindmapEdgeUi> {
        val nodeMap = (goals as List<MindmapNode> + tasks).associateBy { it.id }
        return existingEdges.mapNotNull { edge ->
            val src = nodeMap[edge.sourceId] ?: return@mapNotNull null
            val tgt = nodeMap[edge.targetId] ?: return@mapNotNull null
            edge.copy(
                start = Offset(src.x, src.y),
                end = Offset(tgt.x, tgt.y)
            )
        }
    }
}
