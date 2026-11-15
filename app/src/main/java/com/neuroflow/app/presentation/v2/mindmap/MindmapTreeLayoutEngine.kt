package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.ui.geometry.Offset
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object MindmapTreeLayoutEngine {

    const val GOAL_WIDTH = 190f
    const val GOAL_HEIGHT = 90f
    const val TASK_WIDTH = 210f
    const val TASK_HEIGHT = 65f

    const val HORIZONTAL_GAP = 70f
    const val SIBLING_GAP = 22f
    const val TREE_GAP = 90f

    const val RADIAL_BASE_RADIUS = 280f
    const val RADIAL_STEP_RADIUS = 220f

    enum class BranchDirection {
        LEFT,
        RIGHT
    }

    data class TreeNode(
        val id: String,
        val isGoal: Boolean,
        val width: Float,
        val height: Float,
        val children: MutableList<TreeNode> = mutableListOf(),
        var depth: Int = 0
    )

    /**
     * Estimates dynamic height for a task node based on title character count / estimated lines
     * and presence of badges/tags. With TASK_WIDTH = 210f, approximately 18 characters fit per line.
     */
    fun estimateTaskHeight(title: String, hasBadges: Boolean = false): Float {
        val charCount = title.trim().length
        val estimatedLines = when {
            charCount <= 18 -> 1
            charCount <= 36 -> 2
            charCount <= 54 -> 3
            else -> 4
        }
        val lineHeight = 18f
        val baseHeight = 44f
        var height = baseHeight + (estimatedLines * lineHeight)
        if (hasBadges) {
            height += 20f
        }
        return max(TASK_HEIGHT, height)
    }

    fun estimateTaskHeight(task: TaskEntity): Float {
        val hasBadges = task.tags.isNotBlank() ||
            task.deadlineDate != null ||
            task.priority == Priority.HIGH ||
            task.dependsOnTaskIds.isNotBlank()
        return estimateTaskHeight(task.title, hasBadges)
    }

    fun estimateGoalHeight(goal: GoalEntity): Float {
        val charCount = goal.title.trim().length
        val extraLines = when {
            charCount <= 22 -> 0
            charCount <= 44 -> 1
            else -> 2
        }
        return GOAL_HEIGHT + extraLines * 18f
    }

    /**
     * Computes clean, non-overlapping coordinates for all visible goals and tasks.
     * In HORIZONTAL mode, branches partition symmetrically (half right, half left).
     * In RADIAL mode, arbitrary N-depth hierarchies project outward with concentric radii.
     * Descendants of collapsed nodes are excluded.
     */
    fun computeLayout(
        goals: List<GoalEntity>,
        tasks: List<TaskEntity>,
        collapsedNodeIds: Set<String>,
        layoutMode: MindmapLayoutMode = MindmapLayoutMode.HORIZONTAL,
        rootPositions: Map<String, Offset> = emptyMap()
    ): Map<String, Offset> {
        val goalIds = goals.map { it.id }.toSet()
        val taskIds = tasks.map { it.id }.toSet()

        // 1. Build hierarchy adjacency
        // Map from parentId -> List<TaskEntity>
        val taskChildrenMap = mutableMapOf<String, MutableList<TaskEntity>>()
        val goalChildrenMap = mutableMapOf<String, MutableList<TaskEntity>>()
        val standaloneRootTasks = mutableListOf<TaskEntity>()

        tasks.forEach { task ->
            val parentId = task.parentTaskId
            val goalId = task.goalId

            if (parentId != null && taskIds.contains(parentId)) {
                taskChildrenMap.getOrPut(parentId) { mutableListOf() }.add(task)
            } else if (goalId != null && goalIds.contains(goalId)) {
                goalChildrenMap.getOrPut(goalId) { mutableListOf() }.add(task)
            } else {
                standaloneRootTasks.add(task)
            }
        }

        // 2. Identify all hidden descendants of collapsed nodes
        val hiddenTaskIds = mutableSetOf<String>()
        fun hideSubtree(taskId: String) {
            if (hiddenTaskIds.add(taskId)) {
                taskChildrenMap[taskId].orEmpty().forEach { hideSubtree(it.id) }
            }
        }

        collapsedNodeIds.forEach { collapsedId ->
            if (goalIds.contains(collapsedId)) {
                goalChildrenMap[collapsedId].orEmpty().forEach { hideSubtree(it.id) }
            } else if (taskIds.contains(collapsedId)) {
                taskChildrenMap[collapsedId].orEmpty().forEach { hideSubtree(it.id) }
            }
        }

        // 3. Build Tree representation with cycle detection
        val visited = mutableSetOf<String>()
        visited.addAll(hiddenTaskIds)

        fun buildTaskSubtree(task: TaskEntity, currentDepth: Int, path: Set<String>): TreeNode? {
            if (task.id in path) return null // Break cycle
            val node = TreeNode(
                id = task.id,
                isGoal = false,
                width = TASK_WIDTH,
                height = estimateTaskHeight(task),
                depth = currentDepth
            )
            visited.add(task.id)

            // If collapsed, don't include children in layout tree
            if (!collapsedNodeIds.contains(task.id)) {
                val children = taskChildrenMap[task.id].orEmpty()
                for (child in children) {
                    val childNode = buildTaskSubtree(child, currentDepth + 1, path + task.id)
                    if (childNode != null) {
                        node.children.add(childNode)
                    }
                }
            }
            return node
        }

        val rootTrees = mutableListOf<TreeNode>()

        // Add Goals as roots
        goals.forEach { goal ->
            val goalNode = TreeNode(
                id = goal.id,
                isGoal = true,
                width = GOAL_WIDTH,
                height = estimateGoalHeight(goal),
                depth = 0
            )
            visited.add(goal.id)

            if (!collapsedNodeIds.contains(goal.id)) {
                val directTasks = goalChildrenMap[goal.id].orEmpty()
                for (task in directTasks) {
                    val childNode = buildTaskSubtree(task, 1, setOf(goal.id))
                    if (childNode != null) {
                        goalNode.children.add(childNode)
                    }
                }
            }
            rootTrees.add(goalNode)
        }

        // Add Standalone Tasks as roots
        standaloneRootTasks.forEach { task ->
            if (task.id !in visited) {
                val taskNode = buildTaskSubtree(task, 0, emptySet())
                if (taskNode != null) {
                    rootTrees.add(taskNode)
                }
            }
        }

        // Also catch any orphan tasks that weren't visited due to disconnected parent chains
        tasks.forEach { task ->
            if (task.id !in visited) {
                val taskNode = buildTaskSubtree(task, 0, emptySet())
                if (taskNode != null) {
                    rootTrees.add(taskNode)
                }
            }
        }

        // 4. Compute Coordinates based on layout mode
        val positions = mutableMapOf<String, Offset>()

        when (layoutMode) {
            MindmapLayoutMode.HORIZONTAL -> {
                var currentTopY = 40f
                val globalMaxLeftSpan = rootTrees.maxOfOrNull { root ->
                    val (_, leftChildren) = partitionRootChildren(root)
                    if (leftChildren.isEmpty()) 0f else leftChildren.maxOf { computeSubtreeDepthWidth(it) } + HORIZONTAL_GAP
                } ?: 0f
                val defaultStartX = maxOf(40f, 40f + globalMaxLeftSpan)

                rootTrees.forEach { root ->
                    val (rightChildren, leftChildren) = partitionRootChildren(root)
                    val rightHeight = if (rightChildren.isEmpty()) 0f else {
                        rightChildren.map { computeSubtreeHeight(it) }.sum() + (rightChildren.size - 1) * SIBLING_GAP
                    }
                    val leftHeight = if (leftChildren.isEmpty()) 0f else {
                        leftChildren.map { computeSubtreeHeight(it) }.sum() + (leftChildren.size - 1) * SIBLING_GAP
                    }
                    val totalTreeHeight = maxOf(root.height, rightHeight, leftHeight)

                    val customRootPos = rootPositions[root.id]
                    val startX = customRootPos?.x ?: defaultStartX
                    val startY = customRootPos?.y ?: currentTopY

                    layoutBiDirectionalTree(
                        root = root,
                        rightChildren = rightChildren,
                        leftChildren = leftChildren,
                        rightHeight = rightHeight,
                        leftHeight = leftHeight,
                        startX = startX,
                        startY = startY,
                        totalTreeHeight = totalTreeHeight,
                        positions = positions
                    )

                    currentTopY = max(currentTopY + totalTreeHeight + TREE_GAP, startY + totalTreeHeight + TREE_GAP)
                }
            }
            MindmapLayoutMode.RADIAL -> {
                var radialCenterY = 300f
                rootTrees.forEach { root ->
                    val customPos = rootPositions[root.id]
                    val centerX = customPos?.x ?: 500f
                    val centerY = customPos?.y ?: radialCenterY

                    layoutRadialTree(
                        root = root,
                        centerX = centerX,
                        centerY = centerY,
                        positions = positions
                    )
                    radialCenterY += 900f
                }
            }
        }

        return positions
    }

    /**
     * Splits root children symmetrically into Right and Left branches.
     * For N children, roughly half extend right and half extend left.
     */
    fun partitionRootChildren(root: TreeNode): Pair<List<TreeNode>, List<TreeNode>> {
        if (root.children.isEmpty()) return Pair(emptyList(), emptyList())
        val rightCount = (root.children.size + 1) / 2
        val rightChildren = root.children.take(rightCount)
        val leftChildren = root.children.drop(rightCount)
        return Pair(rightChildren, leftChildren)
    }

    /**
     * Recursively calculates the vertical height required for the subtree rooted at `node`.
     * If the node has no children (or was collapsed when building), its height is its own height.
     * Otherwise it is max(node.height, sum(child heights) + gaps).
     */
    fun computeSubtreeHeight(node: TreeNode): Float {
        if (node.children.isEmpty()) {
            return node.height
        }
        val childrenHeights = node.children.map { computeSubtreeHeight(it) }
        val totalChildrenH = childrenHeights.sum() + (node.children.size - 1) * SIBLING_GAP
        return max(node.height, totalChildrenH)
    }

    /**
     * Calculates the horizontal depth span (width + gaps) of a subtree flowing in one direction.
     */
    fun computeSubtreeDepthWidth(node: TreeNode): Float {
        if (node.children.isEmpty()) return node.width
        val maxChildSpan = node.children.maxOf { computeSubtreeDepthWidth(it) }
        return node.width + HORIZONTAL_GAP + maxChildSpan
    }

    /**
     * Symmetrically lays out a root tree with right-flowing and left-flowing branches.
     */
    private fun layoutBiDirectionalTree(
        root: TreeNode,
        rightChildren: List<TreeNode>,
        leftChildren: List<TreeNode>,
        rightHeight: Float,
        leftHeight: Float,
        startX: Float,
        startY: Float,
        totalTreeHeight: Float,
        positions: MutableMap<String, Offset>
    ) {
        val centerY = startY + (totalTreeHeight / 2f)
        val rootY = centerY - (root.height / 2f)
        positions[root.id] = Offset(startX, rootY)

        // Position Right-hand children & their outward-flowing subtrees (+X)
        if (rightChildren.isNotEmpty()) {
            var currentRightTop = centerY - (rightHeight / 2f)
            val nextRightX = startX + root.width + HORIZONTAL_GAP
            rightChildren.forEach { child ->
                val childH = computeSubtreeHeight(child)
                layoutDirectedSubtree(
                    node = child,
                    x = nextRightX,
                    topY = currentRightTop,
                    availableHeight = childH,
                    direction = BranchDirection.RIGHT,
                    positions = positions
                )
                currentRightTop += childH + SIBLING_GAP
            }
        }

        // Position Left-hand children & their outward-flowing subtrees (-X)
        if (leftChildren.isNotEmpty()) {
            var currentLeftTop = centerY - (leftHeight / 2f)
            leftChildren.forEach { child ->
                val childH = computeSubtreeHeight(child)
                val nextLeftX = startX - HORIZONTAL_GAP - child.width
                layoutDirectedSubtree(
                    node = child,
                    x = nextLeftX,
                    topY = currentLeftTop,
                    availableHeight = childH,
                    direction = BranchDirection.LEFT,
                    positions = positions
                )
                currentLeftTop += childH + SIBLING_GAP
            }
        }
    }

    /**
     * Recursively lays out a subtree flowing strictly in the given horizontal direction.
     */
    private fun layoutDirectedSubtree(
        node: TreeNode,
        x: Float,
        topY: Float,
        availableHeight: Float,
        direction: BranchDirection,
        positions: MutableMap<String, Offset>
    ) {
        val centerY = topY + (availableHeight / 2f)
        val nodeY = centerY - (node.height / 2f)
        positions[node.id] = Offset(x, nodeY)

        if (node.children.isEmpty()) return

        val childHeights = node.children.map { computeSubtreeHeight(it) }
        val totalChildrenH = childHeights.sum() + (node.children.size - 1) * SIBLING_GAP
        var currentChildTop = topY + (availableHeight - totalChildrenH) / 2f

        node.children.forEachIndexed { index, child ->
            val childH = childHeights[index]
            val nextX = when (direction) {
                BranchDirection.RIGHT -> x + node.width + HORIZONTAL_GAP
                BranchDirection.LEFT -> x - HORIZONTAL_GAP - child.width
            }
            layoutDirectedSubtree(
                node = child,
                x = nextX,
                topY = currentChildTop,
                availableHeight = childH,
                direction = direction,
                positions = positions
            )
            currentChildTop += childH + SIBLING_GAP
        }
    }

    /**
     * Positions `root` and its descendants radially around (centerX, centerY)
     * supporting arbitrary N-depth hierarchies with concentric radii and bounded angular arcs.
     */
    fun layoutRadialTree(
        root: TreeNode,
        centerX: Float,
        centerY: Float,
        positions: MutableMap<String, Offset>
    ) {
        positions[root.id] = Offset(centerX, centerY)
        if (root.children.isEmpty()) return

        val totalLeaves = root.children.sumOf { countLeaves(it) }
        val childCount = root.children.size
        var currentAngle = 0.0

        root.children.forEach { child ->
            val childLeaves = countLeaves(child)
            val arc = if (childCount == 1) {
                2.0 * Math.PI
            } else {
                (childLeaves.toDouble() / totalLeaves) * 2.0 * Math.PI
            }
            val midAngle = currentAngle + (arc / 2.0)

            val childX = centerX + RADIAL_BASE_RADIUS * cos(midAngle).toFloat()
            val childY = centerY + RADIAL_BASE_RADIUS * sin(midAngle).toFloat()
            positions[child.id] = Offset(childX, childY)

            // Recurse to depth 2 and beyond
            layoutRadialDescendants(
                node = child,
                depth = 2,
                startAngle = currentAngle,
                endAngle = currentAngle + arc,
                centerX = centerX,
                centerY = centerY,
                positions = positions
            )

            currentAngle += arc
        }
    }

    /**
     * Recursive radial layout for descendants at arbitrary depth N.
     * Projects outward along concentric radii (radiusN = base + (depth - 1) * step)
     * within the bounded angular arc of the parent.
     */
    private fun layoutRadialDescendants(
        node: TreeNode,
        depth: Int,
        startAngle: Double,
        endAngle: Double,
        centerX: Float,
        centerY: Float,
        positions: MutableMap<String, Offset>
    ) {
        if (node.children.isEmpty()) return

        val radius = RADIAL_BASE_RADIUS + (depth - 1) * RADIAL_STEP_RADIUS
        val availableArc = endAngle - startAngle
        val maxSpread = availableArc * 0.85
        val sectorStart = (startAngle + endAngle) / 2.0 - (maxSpread / 2.0)

        val totalChildLeaves = node.children.sumOf { countLeaves(it) }
        var currentSubAngle = sectorStart

        node.children.forEach { subChild ->
            val subChildLeaves = countLeaves(subChild)
            val subArc = (subChildLeaves.toDouble() / totalChildLeaves) * maxSpread
            val midSubAngle = currentSubAngle + (subArc / 2.0)

            val childX = centerX + radius * cos(midSubAngle).toFloat()
            val childY = centerY + radius * sin(midSubAngle).toFloat()
            positions[subChild.id] = Offset(childX, childY)

            // Recurse to depth + 1
            layoutRadialDescendants(
                node = subChild,
                depth = depth + 1,
                startAngle = currentSubAngle,
                endAngle = currentSubAngle + subArc,
                centerX = centerX,
                centerY = centerY,
                positions = positions
            )

            currentSubAngle += subArc
        }
    }

    fun countLeaves(node: TreeNode): Int {
        if (node.children.isEmpty()) return 1
        return node.children.sumOf { countLeaves(it) }
    }
}
