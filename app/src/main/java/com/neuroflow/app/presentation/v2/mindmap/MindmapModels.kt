package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.ui.geometry.Offset
import com.neuroflow.app.data.repository.NodeConnection
import com.neuroflow.app.domain.model.Priority

sealed interface MindmapNode {
    val id: String
    val title: String
    val x: Float
    val y: Float
    val width: Float
    val height: Float
    val isDropTarget: Boolean
    val isCollapsed: Boolean
    val childCount: Int
    val isHighlighted: Boolean
}

data class MindmapGoalNode(
    override val id: String,
    override val title: String,
    override val x: Float,
    override val y: Float,
    override val width: Float = 190f,
    override val height: Float = 90f,
    val description: String = "",
    val targetDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val tags: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    override val isCollapsed: Boolean = false,
    override val childCount: Int = 0,
    override val isDropTarget: Boolean = false,
    override val isHighlighted: Boolean = false
) : MindmapNode {
    val progress: Float
        get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val hasChildren: Boolean get() = childCount > 0
}

data class MindmapTaskNode(
    override val id: String,
    override val title: String,
    override val x: Float,
    override val y: Float,
    override val width: Float = 210f,
    override val height: Float = 65f,
    val priority: Priority = Priority.MEDIUM,
    val deadlineDate: Long? = null,
    val tags: String = "",
    val isCompleted: Boolean = false,
    val goalId: String? = null,
    val parentTaskId: String? = null,
    val dependsOnTaskIds: List<String> = emptyList(),
    override val isCollapsed: Boolean = false,
    override val childCount: Int = 0,
    val depth: Int = 0,
    override val isDropTarget: Boolean = false,
    override val isHighlighted: Boolean = false
) : MindmapNode {
    val hasChildren: Boolean get() = childCount > 0
}

enum class MindmapEdgeType {
    GOAL_LINK,
    SUBTASK_LINK,
    DEPENDENCY,
    VISUAL
}

data class MindmapEdgeUi(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val start: Offset,
    val end: Offset,
    val edgeType: MindmapEdgeType = MindmapEdgeType.VISUAL,
    val isCompleted: Boolean = false
)

enum class MindmapLayoutMode {
    HORIZONTAL,
    RADIAL
}

enum class SproutMode {
    CHILD,
    SIBLING
}

enum class GoalWorkspaceMode {
    OUTLINE,
    MINDMAP
}

data class GoalSummaryUi(
    val goal: com.neuroflow.app.data.local.entity.GoalEntity,
    val totalTasks: Int,
    val completedTasks: Int,
    val nextActionableTask: com.neuroflow.app.data.local.entity.TaskEntity?
) {
    val progress: Float
        get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
}

data class MindmapUiState(
    val isLoading: Boolean = true,
    val showCompleted: Boolean = false,
    val layoutMode: MindmapLayoutMode = MindmapLayoutMode.HORIZONTAL,
    val goals: List<MindmapGoalNode> = emptyList(),
    val tasks: List<MindmapTaskNode> = emptyList(),
    val edges: List<MindmapEdgeUi> = emptyList(),
    val connectionPrompt: Pair<MindmapNode, MindmapNode>? = null,
    val connectingSourceNodeId: String? = null,
    val collapsedNodeIds: Set<String> = emptySet(),
    val draggingNodeId: String? = null,
    val dropTargetNodeId: String? = null,
    val inlineSproutParentNode: MindmapNode? = null,
    val inlineSproutMode: SproutMode = SproutMode.CHILD,
    val activeStartingNodeId: String? = null,
    val searchQuery: String = "",
    val highlightedNodeIds: Set<String> = emptySet(),
    val disconnectTargetNode: MindmapTaskNode? = null,
    val disconnectConnections: List<NodeConnection> = emptyList(),
    val isGoalCreateOpen: Boolean = false,
    val editingGoalNode: MindmapGoalNode? = null,
    val selectedGoalId: String? = null,
    val workspaceMode: GoalWorkspaceMode = GoalWorkspaceMode.OUTLINE,
    val showAllGoalsInMap: Boolean = false,
    val goalSummaries: List<GoalSummaryUi> = emptyList(),
    val allRawTasks: List<com.neuroflow.app.data.local.entity.TaskEntity> = emptyList(),
    val breakdownPreview: List<com.neuroflow.app.domain.assistant.GoalBreakdownMilestone>? = null,
    val selectedTagFilter: String? = null,
    val actionMessage: String? = null
)
