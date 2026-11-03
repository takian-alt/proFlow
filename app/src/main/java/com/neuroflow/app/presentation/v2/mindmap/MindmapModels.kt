package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.ui.geometry.Offset
import com.neuroflow.app.domain.model.Priority

sealed interface MindmapNode {
    val id: String
    val title: String
    val x: Float
    val y: Float
}

data class MindmapGoalNode(
    override val id: String,
    override val title: String,
    override val x: Float,
    override val y: Float,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0
) : MindmapNode {
    val progress: Float
        get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
}

data class MindmapTaskNode(
    override val id: String,
    override val title: String,
    override val x: Float,
    override val y: Float,
    val priority: Priority = Priority.MEDIUM,
    val deadlineDate: Long? = null,
    val isCompleted: Boolean = false,
    val goalId: String? = null,
    val dependsOnTaskIds: List<String> = emptyList()
) : MindmapNode

enum class MindmapEdgeType {
    GOAL_LINK,
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

data class MindmapUiState(
    val isLoading: Boolean = true,
    val showCompleted: Boolean = false,
    val goals: List<MindmapGoalNode> = emptyList(),
    val tasks: List<MindmapTaskNode> = emptyList(),
    val edges: List<MindmapEdgeUi> = emptyList(),
    val connectionPrompt: Pair<MindmapNode, MindmapNode>? = null,
    val connectingSourceNodeId: String? = null
)
