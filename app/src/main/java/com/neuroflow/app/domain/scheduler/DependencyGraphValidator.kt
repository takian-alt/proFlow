package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.service.SchedulableTask

/**
 * ADR-020: Cycle-Safe Dependency Graph Validator & Sanitizer.
 *
 * Ensures that circular dependencies (e.g. A -> B -> A) never cause Kahn's topological sort
 * in `TaskSchedulerService.scheduleDay` to silently drop tasks.
 */
object DependencyGraphValidator {

    data class SanitizedDagResult(
        val tasks: List<SchedulableTask>,
        val brokenCycleEdges: List<Pair<String, String>>
    )

    /**
     * Detects and breaks any cycle in [tasks]' prerequisite graph before Kahn's algorithm runs.
     * Guarantees 100% of input tasks reach the priority queue during `scheduleDay`.
     */
    fun sanitizeDag(tasks: List<SchedulableTask>): SanitizedDagResult {
        if (tasks.size <= 1) {
            val sanitized = tasks.map { task ->
                val cleanPrereqs = task.prerequisiteIds.filter { it != task.id }.toSet()
                task.copy(prerequisiteIds = cleanPrereqs)
            }
            return SanitizedDagResult(sanitized, emptyList())
        }

        // BUG-330 Fix: If a task has a continuation part present in tasks (_part or #part),
        // ensure any other downstream task waiting on the parent task also waits for the continuation part.
        val continuationPartMap = tasks
            .filter { it.id.contains("_part") || it.id.contains("#part") }
            .groupBy { it.parentTaskId ?: it.id.substringBefore("#part").substringBefore("_part") }
            .mapValues { (_, parts) -> parts.maxByOrNull { it.id }!! }

        val chainedTasks = if (continuationPartMap.isNotEmpty()) {
            tasks.map { task ->
                var updatedPrereqs = task.prerequisiteIds
                for ((parent, finalPart) in continuationPartMap) {
                    if (task.id != finalPart.id && task.id != parent && parent in updatedPrereqs) {
                        updatedPrereqs = updatedPrereqs + finalPart.id
                    }
                }
                if (updatedPrereqs != task.prerequisiteIds) {
                    task.copy(prerequisiteIds = updatedPrereqs)
                } else {
                    task
                }
            }
        } else {
            tasks
        }

        val taskMap = chainedTasks.associateBy { it.id }
        val adj = mutableMapOf<String, MutableSet<String>>() // prereq -> dependent
        val inDegree = mutableMapOf<String, Int>()
        val prereqMap = mutableMapOf<String, MutableSet<String>>()

        for (t in chainedTasks) {
            val validPrereqs = t.prerequisiteIds
                .filter { it != t.id && it in taskMap }
                .toMutableSet()
            prereqMap[t.id] = validPrereqs
            inDegree[t.id] = validPrereqs.size
            for (p in validPrereqs) {
                adj.getOrPut(p) { mutableSetOf() }.add(t.id)
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.forEach { (id, deg) -> if (deg == 0) queue.add(id) }

        val visited = mutableSetOf<String>()
        val brokenEdges = mutableListOf<Pair<String, String>>()

        while (visited.size < chainedTasks.size) {
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (!visited.add(curr)) continue
                for (dep in adj[curr].orEmpty()) {
                    val newDeg = (inDegree[dep] ?: 1) - 1
                    inDegree[dep] = newDeg
                    if (newDeg == 0 && dep !in visited) {
                        queue.add(dep)
                    }
                }
            }

            if (visited.size < chainedTasks.size) {
                // Cycle detected among unvisited nodes!
                // Deterministically select the highest-leverage unvisited task and sever its unvisited incoming edges.
                val victim = chainedTasks
                    .filter { it.id !in visited }
                    .maxWithOrNull(
                        compareBy<SchedulableTask> { it.isFrog }
                            .thenBy { it.priority }
                            .thenBy { it.impactScore }
                            .thenByDescending { it.id }
                    ) ?: break

                val unresolvedPrereqs = prereqMap[victim.id].orEmpty().filter { it !in visited }
                for (p in unresolvedPrereqs) {
                    prereqMap[victim.id]?.remove(p)
                    adj[p]?.remove(victim.id)
                    brokenEdges.add(p to victim.id)
                }
                inDegree[victim.id] = 0
                queue.add(victim.id)
            }
        }

        val severedPairs = brokenEdges.toSet()
        val sanitizedTasks = chainedTasks.map { task ->
            // BUG-246 Fix: preserve all external / cross-day prerequisites not in taskMap.
            // Only sever direct self-references and edges specifically broken during cycle elimination.
            val cleanPrereqs = task.prerequisiteIds
                .filter { it != task.id && (it to task.id) !in severedPairs }
                .toSet()
            task.copy(
                prerequisiteIds = cleanPrereqs,
                parentTaskId = task.parentTaskId
            )
        }

        return SanitizedDagResult(sanitizedTasks, brokenEdges)
    }

    /**
     * Checks whether adding [candidatePrerequisiteId] as a prerequisite of [taskId]
     * would introduce a cycle in [existingDepsByTask].
     */
    fun wouldCreateCycle(
        taskId: String,
        candidatePrerequisiteId: String,
        existingDepsByTask: Map<String, Set<String>>
    ): Boolean {
        if (taskId == candidatePrerequisiteId) return true
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(candidatePrerequisiteId)

        while (stack.isNotEmpty()) {
            val curr = stack.removeLast()
            if (curr == taskId) return true
            if (visited.add(curr)) {
                existingDepsByTask[curr]?.forEach { next -> stack.add(next) }
            }
        }
        return false
    }
}
