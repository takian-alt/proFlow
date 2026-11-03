package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.MindmapEdgeEntity
import com.neuroflow.app.data.local.entity.MindmapNodePositionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.data.repository.MindmapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MindmapViewModel @Inject constructor(
    private val repository: MindmapRepository
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)
    private val _connectionPrompt = MutableStateFlow<Pair<MindmapNode, MindmapNode>?>(null)
    private val _connectingSourceNodeId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(MindmapUiState())
    val uiState: StateFlow<MindmapUiState> = _uiState.asStateFlow()

    // Local volatile cache of dragging offsets
    private val temporaryPositions = mutableMapOf<String, Offset>()

    init {
        viewModelScope.launch {
            _showCompleted
                .flatMapLatest { showComp ->
                    combine(
                        repository.observeGoals(),
                        repository.observeTasks(showComp),
                        repository.observePositions(),
                        repository.observeEdges()
                    ) { goals, tasks, positions, edges ->
                        computeUiState(goals, tasks, positions, edges, showComp)
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    fun onNodeDrag(entityId: String, offset: Offset) {
        temporaryPositions[entityId] = offset
        _uiState.update { current ->
            current.copy(
                goals = current.goals.map { if (it.id == entityId) it.copy(x = offset.x, y = offset.y) else it },
                tasks = current.tasks.map { if (it.id == entityId) it.copy(x = offset.x, y = offset.y) else it },
                edges = recalculateEdges(current.goals, current.tasks, current.edges)
            )
        }
    }

    fun onNodeDragEnd(entityId: String, entityType: String, finalOffset: Offset) {
        temporaryPositions[entityId] = finalOffset
        viewModelScope.launch {
            repository.savePosition(entityId, entityType, finalOffset.x, finalOffset.y)
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
                MindmapEdgeType.DEPENDENCY -> {
                    // Source must finish before Target can begin
                    repository.addTaskDependency(target.id, source.id)
                    repository.createEdge(source.id, target.id, "DEPENDENCY")
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

    private fun computeUiState(
        goals: List<GoalEntity>,
        tasks: List<TaskEntity>,
        positions: List<MindmapNodePositionEntity>,
        edges: List<MindmapEdgeEntity>,
        showCompleted: Boolean
    ): MindmapUiState {
        val posMap = positions.associateBy { it.entityId }

        // 1. Build Goal Nodes with auto-clustering fallback
        val goalNodes = goals.mapIndexed { index, goal ->
            val savedPos = temporaryPositions[goal.id] ?: posMap[goal.id]?.let { Offset(it.x, it.y) }
            val x = savedPos?.x ?: (index * 420f)
            val y = savedPos?.y ?: 0f
            val associatedTasks = tasks.filter { it.goalId == goal.id }
            MindmapGoalNode(
                id = goal.id,
                title = goal.title,
                x = x,
                y = y,
                totalTasks = associatedTasks.size,
                completedTasks = associatedTasks.count { it.status == TaskStatus.COMPLETED }
            )
        }

        val goalMap = goalNodes.associateBy { it.id }

        // 2. Build Task Nodes
        val tasksByGoal = tasks.groupBy { it.goalId }
        val taskNodes = mutableListOf<MindmapTaskNode>()

        tasks.forEach { task ->
            val savedPos = temporaryPositions[task.id] ?: posMap[task.id]?.let { Offset(it.x, it.y) }
            val (x, y) = if (savedPos != null) {
                Pair(savedPos.x, savedPos.y)
            } else if (task.goalId != null && goalMap.containsKey(task.goalId)) {
                // Orbit around associated goal
                val parentGoal = goalMap[task.goalId]!!
                val siblings = tasksByGoal[task.goalId].orEmpty()
                val siblingIndex = siblings.indexOf(task)
                val angle = (siblingIndex * (2 * Math.PI / siblings.size.coerceAtLeast(1))).toFloat()
                val radius = 240f
                Pair(parentGoal.x + (radius * cos(angle.toDouble())).toFloat(), parentGoal.y + (radius * sin(angle.toDouble())).toFloat())
            } else {
                // Standalone cluster
                val standaloneTasks = tasksByGoal[null].orEmpty()
                val standaloneIndex = standaloneTasks.indexOf(task).coerceAtLeast(0)
                Pair((standaloneIndex % 4) * 260f - 300f, ((standaloneIndex / 4) + 1) * 180f + 250f)
            }

            val depList = task.dependsOnTaskIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            taskNodes.add(
                MindmapTaskNode(
                    id = task.id,
                    title = task.title,
                    x = x,
                    y = y,
                    priority = task.priority,
                    deadlineDate = task.deadlineDate,
                    isCompleted = task.status == TaskStatus.COMPLETED,
                    goalId = task.goalId,
                    dependsOnTaskIds = depList
                )
            )
        }

        // 3. Synthesize Edges (Natural Goal Links + Dependencies + Custom Room Edges)
        val nodeMap = (goalNodes as List<MindmapNode> + taskNodes).associateBy { it.id }
        val edgeList = mutableListOf<MindmapEdgeUi>()

        // Goal -> Task implicit edges
        taskNodes.forEach { task ->
            if (task.goalId != null && nodeMap.containsKey(task.goalId)) {
                val goal = nodeMap[task.goalId]!!
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

            // Task dependencies
            task.dependsOnTaskIds.forEach { depId ->
                if (nodeMap.containsKey(depId)) {
                    val parentTask = nodeMap[depId]!!
                    val isDepCompleted = (parentTask as? MindmapTaskNode)?.isCompleted == true
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

        // Custom saved edges
        edges.forEach { edge ->
            val src = nodeMap[edge.sourceId]
            val tgt = nodeMap[edge.targetId]
            if (src != null && tgt != null) {
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
                            else -> MindmapEdgeType.VISUAL
                        },
                        isCompleted = (tgt as? MindmapTaskNode)?.isCompleted == true
                    )
                )
            }
        }

        return MindmapUiState(
            isLoading = false,
            showCompleted = showCompleted,
            goals = goalNodes,
            tasks = taskNodes,
            edges = edgeList,
            connectionPrompt = _connectionPrompt.value,
            connectingSourceNodeId = _connectingSourceNodeId.value
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
