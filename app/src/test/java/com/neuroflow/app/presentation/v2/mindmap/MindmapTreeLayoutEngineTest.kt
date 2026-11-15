package com.neuroflow.app.presentation.v2.mindmap

import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class MindmapTreeLayoutEngineTest {

    @Test
    fun testNDepthHierarchyCoordinates() {
        val goal = GoalEntity(id = "goal_1", title = "Launch Product")
        val rootTask = TaskEntity(id = "task_1", title = "MVP Build", goalId = goal.id, parentTaskId = null)
        val subtask = TaskEntity(id = "task_2", title = "Auth System", goalId = goal.id, parentTaskId = rootTask.id)
        val subSubtask = TaskEntity(id = "task_3", title = "OAuth Token Refresh", goalId = goal.id, parentTaskId = subtask.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(rootTask, subtask, subSubtask),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        assertNotNull(positions["goal_1"])
        assertNotNull(positions["task_1"])
        assertNotNull(positions["task_2"])
        assertNotNull(positions["task_3"])

        val goalPos = positions["goal_1"]!!
        val task1Pos = positions["task_1"]!!
        val task2Pos = positions["task_2"]!!
        val task3Pos = positions["task_3"]!!

        // In Horizontal layout, single child extends right and x strictly increases with tree depth
        assertTrue(task1Pos.x > goalPos.x)
        assertTrue(task2Pos.x > task1Pos.x)
        assertTrue(task3Pos.x > task2Pos.x)
    }

    @Test
    fun testSiblingCollisionAvoidance() {
        val goal = GoalEntity(id = "goal_1", title = "Goal")
        val child1 = TaskEntity(id = "c1", title = "Child 1", goalId = goal.id)
        val child2 = TaskEntity(id = "c2", title = "Child 2", goalId = goal.id)
        val child3 = TaskEntity(id = "c3", title = "Child 3", goalId = goal.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(child1, child2, child3),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        val goalPos = positions["goal_1"]!!
        val pos1 = positions["c1"]!!
        val pos2 = positions["c2"]!!
        val pos3 = positions["c3"]!!

        // Bi-directional balance: 2 children on right (+X), 1 child on left (-X)
        assertTrue(pos1.x > goalPos.x)
        assertTrue(pos2.x > goalPos.x)
        assertTrue(pos3.x < goalPos.x)

        // Right siblings share the same X coordinate
        assertEquals(pos1.x, pos2.x, 0.01f)

        // Sibling Y coordinates must be separated by at least task height
        assertTrue(pos2.y >= pos1.y + MindmapTreeLayoutEngine.TASK_HEIGHT)
    }

    @Test
    fun testBiDirectionalSymmetricBranching() {
        val goal = GoalEntity(id = "root_goal", title = "Company Roadmap")
        // 4 top-level tasks: 2 should go Right, 2 should go Left
        val rightTask1 = TaskEntity(id = "r1", title = "Engineering", goalId = goal.id)
        val rightTask2 = TaskEntity(id = "r2", title = "Design", goalId = goal.id)
        val leftTask1 = TaskEntity(id = "l1", title = "Marketing", goalId = goal.id)
        val leftTask2 = TaskEntity(id = "l2", title = "Finance", goalId = goal.id)

        // Subtasks on both sides
        val subRight = TaskEntity(id = "sub_r", title = "Frontend Architecture", parentTaskId = rightTask1.id)
        val subLeft = TaskEntity(id = "sub_l", title = "Growth Strategy", parentTaskId = leftTask1.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(rightTask1, rightTask2, leftTask1, leftTask2, subRight, subLeft),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        val goalPos = positions["root_goal"]!!
        val posR1 = positions["r1"]!!
        val posR2 = positions["r2"]!!
        val posL1 = positions["l1"]!!
        val posL2 = positions["l2"]!!
        val posSubR = positions["sub_r"]!!
        val posSubL = positions["sub_l"]!!

        // Symmetrical partition: first half extends Right (+X), second half extends Left (-X)
        assertTrue(posR1.x > goalPos.x)
        assertTrue(posR2.x > goalPos.x)
        assertTrue(posL1.x < goalPos.x)
        assertTrue(posL2.x < goalPos.x)

        // Subtask under Right branch flows outwards to the Right
        assertTrue(posSubR.x > posR1.x)

        // Subtask under Left branch flows outwards to the Left (-X)
        assertTrue(posSubL.x < posL1.x)
    }

    @Test
    fun testDynamicHeightNodeSizingAndCollisionAvoidance() {
        val shortTitle = "Quick task"
        val longTitle = "Develop high-throughput Kafka event streaming pipeline with schema registry and dead-letter queues"

        val shortHeight = MindmapTreeLayoutEngine.estimateTaskHeight(shortTitle, false)
        val longHeight = MindmapTreeLayoutEngine.estimateTaskHeight(longTitle, false)
        val badgeHeight = MindmapTreeLayoutEngine.estimateTaskHeight(shortTitle, true)

        assertEquals(MindmapTreeLayoutEngine.TASK_HEIGHT, shortHeight, 0.01f)
        assertTrue(longHeight > shortHeight)
        assertTrue(badgeHeight > shortHeight)

        // Verify collision avoidance when siblings share a branch with dynamic multi-line heights
        val root = TaskEntity(id = "parent", title = "Parent Task")
        // 3 children so c_long and c_short are both placed on the right branch
        val childLong = TaskEntity(id = "c_long", title = longTitle, parentTaskId = root.id)
        val childShort = TaskEntity(id = "c_short", title = shortTitle, parentTaskId = root.id, tags = "urgent,api", priority = Priority.HIGH)
        val childLeft = TaskEntity(id = "c_left", title = "Left Branch Task", parentTaskId = root.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = emptyList(),
            tasks = listOf(root, childLong, childShort, childLeft),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        val posLong = positions["c_long"]!!
        val posShort = positions["c_short"]!!
        val posLeft = positions["c_left"]!!
        val posRoot = positions["parent"]!!

        // Long and short are right siblings; left is on the left branch
        assertTrue(posLong.x > posRoot.x)
        assertEquals(posLong.x, posShort.x, 0.01f)
        assertTrue(posLeft.x < posRoot.x)

        val expectedLongH = MindmapTreeLayoutEngine.estimateTaskHeight(childLong)
        // posShort.y must be below posLong.y by at least expectedLongH + SIBLING_GAP
        assertTrue(posShort.y >= posLong.y + expectedLongH + MindmapTreeLayoutEngine.SIBLING_GAP - 0.1f)
    }

    @Test
    fun testArbitraryNDepthRadialLayout() {
        val goal = GoalEntity(id = "goal_1", title = "Central Vision")
        val depth1 = TaskEntity(id = "d1", title = "Depth 1", goalId = goal.id)
        val depth2 = TaskEntity(id = "d2", title = "Depth 2", parentTaskId = depth1.id)
        val depth3 = TaskEntity(id = "d3", title = "Depth 3", parentTaskId = depth2.id)
        val depth4 = TaskEntity(id = "d4", title = "Depth 4", parentTaskId = depth3.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(depth1, depth2, depth3, depth4),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.RADIAL
        )

        val gPos = positions["goal_1"]!!
        val p1 = positions["d1"]!!
        val p2 = positions["d2"]!!
        val p3 = positions["d3"]!!
        val p4 = positions["d4"]!!

        val dist1 = hypot((p1.x - gPos.x).toDouble(), (p1.y - gPos.y).toDouble())
        val dist2 = hypot((p2.x - gPos.x).toDouble(), (p2.y - gPos.y).toDouble())
        val dist3 = hypot((p3.x - gPos.x).toDouble(), (p3.y - gPos.y).toDouble())
        val dist4 = hypot((p4.x - gPos.x).toDouble(), (p4.y - gPos.y).toDouble())

        // Concentric radii project outward monotonically: radiusN = radius1 + (depth - 1) * step
        assertEquals(MindmapTreeLayoutEngine.RADIAL_BASE_RADIUS.toDouble(), dist1, 1.0)
        assertEquals((MindmapTreeLayoutEngine.RADIAL_BASE_RADIUS + MindmapTreeLayoutEngine.RADIAL_STEP_RADIUS).toDouble(), dist2, 1.0)
        assertEquals((MindmapTreeLayoutEngine.RADIAL_BASE_RADIUS + 2 * MindmapTreeLayoutEngine.RADIAL_STEP_RADIUS).toDouble(), dist3, 1.0)
        assertEquals((MindmapTreeLayoutEngine.RADIAL_BASE_RADIUS + 3 * MindmapTreeLayoutEngine.RADIAL_STEP_RADIUS).toDouble(), dist4, 1.0)

        assertTrue(dist4 > dist3)
        assertTrue(dist3 > dist2)
        assertTrue(dist2 > dist1)
    }

    @Test
    fun testBranchFoldingHidesDescendants() {
        val goal = GoalEntity(id = "goal_1", title = "Goal")
        val parentTask = TaskEntity(id = "p1", title = "Parent", goalId = goal.id)
        val subtask = TaskEntity(id = "s1", title = "Subtask", parentTaskId = parentTask.id, goalId = goal.id)
        val subSubtask = TaskEntity(id = "ss1", title = "Sub-subtask", parentTaskId = subtask.id, goalId = goal.id)

        // Collapse the parent task
        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(parentTask, subtask, subSubtask),
            collapsedNodeIds = setOf(parentTask.id),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        assertNotNull(positions["goal_1"])
        assertNotNull(positions["p1"])
        // Descendants of collapsed parent must not be in layout positions
        assertFalse(positions.containsKey("s1"))
        assertFalse(positions.containsKey("ss1"))
    }

    @Test
    fun testCyclePreventionDoesNotHang() {
        // Cyclic dependency: A -> B -> A
        val taskA = TaskEntity(id = "task_a", title = "A", parentTaskId = "task_b")
        val taskB = TaskEntity(id = "task_b", title = "B", parentTaskId = "task_a")

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = emptyList(),
            tasks = listOf(taskA, taskB),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.HORIZONTAL
        )

        // Should safely terminate and place at least one node without crashing or infinite recursion
        assertTrue(positions.isNotEmpty())
    }

    @Test
    fun testRadialLayoutCoordinates() {
        val goal = GoalEntity(id = "goal_1", title = "Central Goal")
        val child1 = TaskEntity(id = "c1", title = "Child 1", goalId = goal.id)
        val child2 = TaskEntity(id = "c2", title = "Child 2", goalId = goal.id)

        val positions = MindmapTreeLayoutEngine.computeLayout(
            goals = listOf(goal),
            tasks = listOf(child1, child2),
            collapsedNodeIds = emptySet(),
            layoutMode = MindmapLayoutMode.RADIAL
        )

        val goalPos = positions["goal_1"]!!
        val pos1 = positions["c1"]!!
        val pos2 = positions["c2"]!!

        // Radial children should be positioned outward from center
        val dist1 = hypot((pos1.x - goalPos.x).toDouble(), (pos1.y - goalPos.y).toDouble())
        val dist2 = hypot((pos2.x - goalPos.x).toDouble(), (pos2.y - goalPos.y).toDouble())

        assertTrue(dist1 > 200.0)
        assertTrue(dist2 > 200.0)
    }
}
