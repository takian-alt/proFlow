package com.neuroflow.app.data.local

import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemsRoadmapSeeder @Inject constructor(
    private val database: NeuroFlowDatabase
) {
    suspend fun seedRoadmap(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val goals = mutableListOf<GoalEntity>()
            val tasks = mutableListOf<TaskEntity>()

            fun getCal(year: Int, month: Int, day: Int): Long {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.clear()
                cal.set(year, month - 1, day, 12, 0, 0)
                return cal.timeInMillis
            }

            val goalStage1 = GoalEntity(
                id = "goal_stage_1",
                title = "Stage 1: Memory, Linux & Core Backend",
                description = "Systems Engineering Roadmap: Months 1\u20136 \u00b7 100% production deliverables.",
                targetDate = getCal(2027, 3, 28),
                identityStatement = "I am a high-performance Systems Engineer building rigorous low-level, async, and distributed backends.",
                isActive = true
            )
            goals.add(goalStage1)

            val monthTask_s1_m1 = TaskEntity(
                id = "task_s1_m1",
                goalId = "goal_stage_1",
                title = "Month 1: Raw C & OS Memory Mechanics",
                description = "Curriculum milestone for Month 1: Raw C & OS Memory Mechanics",
                scheduledDate = getCal(2026, 11, 1),
                deadlineDate = getCal(2026, 12, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,month1",
                dependsOnTaskIds = "",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s1_m1)

            val task_s1_m1_i1 = TaskEntity(
                id = "task_s1_m1_item1",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m1",
                title = "LEARN: Virtual Memory, Stack vs. Heap & Pointer Arithmetic",
                description = "How the CPU and OS kernel map virtual memory pages to physical RAM (brk/mmap), how stack frames grow/shrink in O(1) via the stack pointer, and how C struct alignment/padding works in bytes.",
                scheduledDate = getCal(2026, 11, 5),
                deadlineDate = getCal(2026, 11, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m1_i1)

            val task_s1_m1_i2 = TaskEntity(
                id = "task_s1_m1_item2",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m1",
                title = "BUILD: Project 1 \u2014 Custom Arena & malloc Allocator in C (c-mem-engine)",
                description = "Write a 300-line C17 library implementing a free-list heap allocator (my_malloc, my_free) and a bump/arena allocator, verified with zero memory leaks under Valgrind and gdb.",
                scheduledDate = getCal(2026, 11, 15),
                deadlineDate = getCal(2026, 11, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m1_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m1_i2)

            val task_s1_m1_i3 = TaskEntity(
                id = "task_s1_m1_item3",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m1",
                title = "LEARN: POSIX File Descriptors & Process Lifecycle",
                description = "How the Linux kernel tracks open files/sockets via integer indices in a per-process file descriptor table, and how fork(), execve(), pipe(), and mmap() operate across the user-space/kernel-space boundary.",
                scheduledDate = getCal(2026, 11, 15),
                deadlineDate = getCal(2026, 11, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m1_i3)

            val task_s1_m1_i4 = TaskEntity(
                id = "task_s1_m1_item4",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m1",
                title = "BUILD: Project 2 \u2014 Minimal Linux Shell & Memory-Mapped File Searcher (posix-sh)",
                description = "Build a C command-line shell that spawns piped child processes (ls | grep) and includes a built-in fast text search command using mmap() instead of fread().",
                scheduledDate = getCal(2026, 11, 15),
                deadlineDate = getCal(2026, 11, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m1_item1,task_s1_m1_item3",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m1_i4)

            val monthTask_s1_m2 = TaskEntity(
                id = "task_s1_m2",
                goalId = "goal_stage_1",
                title = "Months 2\u20133: Java DSA & Modern JVM Internals",
                description = "Curriculum milestone for Months 2\u20133: Java DSA & Modern JVM Internals",
                scheduledDate = getCal(2026, 12, 1),
                deadlineDate = getCal(2026, 12, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,month2",
                dependsOnTaskIds = "task_s1_m1_item4",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s1_m2)

            val task_s1_m2_i1 = TaskEntity(
                id = "task_s1_m2_item1",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m2",
                title = "LEARN: Asymptotic Complexity, CPU Cache Locality & Tree/Graph Layouts",
                description = "Why contiguous array structures (ArrayList, ArrayDeque, array-backed binary heaps) beat pointer-chasing nodes (LinkedList) on modern CPU L1/L2 caches, alongside Graph traversals (BFS/DFS/Dijkstra) and Dynamic Programming.",
                scheduledDate = getCal(2026, 12, 5),
                deadlineDate = getCal(2026, 12, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m2_i1)

            val task_s1_m2_i2 = TaskEntity(
                id = "task_s1_m2_item2",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m2",
                title = "BUILD: Project 3 \u2014 Zero-Dependency DSA & Graph Benchmark Suite in Java 25 (jvm-dsa-bench)",
                description = "Implement your own generic HashMap (with open addressing), Min-Heap, Trie, and Graph router in Java 25 LTS, tested with JUnit 5 and benchmarked for allocation overhead.",
                scheduledDate = getCal(2026, 12, 15),
                deadlineDate = getCal(2026, 12, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m2_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m2_i2)

            val task_s1_m2_i3 = TaskEntity(
                id = "task_s1_m2_item3",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m2",
                title = "LEARN: JVM Memory Model & Virtual Threads (Project Loom)",
                description = "How the JVM splits Heap generations (Eden, Survivor, Tenured) and how Virtual Threads mount/unmount continuations onto a small pool of OS carrier threads whenever blocking I/O occurs.",
                scheduledDate = getCal(2026, 12, 15),
                deadlineDate = getCal(2026, 12, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m2_i3)

            val task_s1_m2_i4 = TaskEntity(
                id = "task_s1_m2_item4",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m2",
                title = "BUILD: Project 4 \u2014 High-Concurrency TCP Task Server in Java 25 (loom-server)",
                description = "Build a raw socket/HTTP server handling 50,000 concurrent simulated connections using Executors.newVirtualThreadPerTaskExecutor(), Records, and Pattern Matching.",
                scheduledDate = getCal(2026, 12, 15),
                deadlineDate = getCal(2026, 12, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m2_item1,task_s1_m2_item3",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m2_i4)

            val monthTask_s1_m3 = TaskEntity(
                id = "task_s1_m3",
                goalId = "goal_stage_1",
                title = "Months 4\u20136: Rust Core & Local Persistence",
                description = "Curriculum milestone for Months 4\u20136: Rust Core & Local Persistence",
                scheduledDate = getCal(2027, 2, 1),
                deadlineDate = getCal(2027, 3, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,month3",
                dependsOnTaskIds = "task_s1_m2_item4",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s1_m3)

            val task_s1_m3_i1 = TaskEntity(
                id = "task_s1_m3_item1",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m3",
                title = "LEARN: Compile-Time Ownership, Affine Types & Borrow Checking",
                description = "How Rust tracks variable lifetimes at compile time without a garbage collector, enforcing aliasing XOR mutability (either many &T readers or one &mut T writer) to prevent use-after-free and iterator invalidation.",
                scheduledDate = getCal(2027, 2, 5),
                deadlineDate = getCal(2027, 2, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m3_i1)

            val task_s1_m3_i2 = TaskEntity(
                id = "task_s1_m3_item2",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m3",
                title = "BUILD: Project 5 \u2014 Safe Ring Buffer & In-Memory Cache in Rust (rust-collections)",
                description = "Port your earlier C data structures into safe Rust using generics, traits (Iterator, Drop), and Option/Result without writing unsafe.",
                scheduledDate = getCal(2027, 2, 15),
                deadlineDate = getCal(2027, 2, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m3_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m3_i2)

            val task_s1_m3_i3 = TaskEntity(
                id = "task_s1_m3_item3",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m3",
                title = "LEARN: Binary Serialization, CLI Parsing & Embedded B-Tree Storage (SQLite)",
                description = "How data structures serialize into compact bytes (serde), how OS signals work in Linux daemons, and how SQLite executes ACID transactions on a single local disk file using Write-Ahead Logging (WAL mode).",
                scheduledDate = getCal(2027, 2, 15),
                deadlineDate = getCal(2027, 2, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,linux,c,jvm,stage1,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s1_m3_i3)

            val task_s1_m3_i4 = TaskEntity(
                id = "task_s1_m3_item4",
                goalId = "goal_stage_1",
                parentTaskId = "task_s1_m3",
                title = "BUILD: Project 6 \u2014 Fast Linux Productivity & Process Tracker CLI (flow-cli)",
                description = "Build a release-grade Linux CLI tool using clap, serde, and rusqlite (in WAL mode) that logs task sessions, tracks active Linux process focus time, and queries statistics in less than 2 milliseconds.",
                scheduledDate = getCal(2027, 2, 15),
                deadlineDate = getCal(2027, 2, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,linux,c,jvm,stage1,build,project",
                dependsOnTaskIds = "task_s1_m3_item1,task_s1_m3_item3",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s1_m3_i4)

            val goalStage2 = GoalEntity(
                id = "goal_stage_2",
                title = "Stage 2: Production Backends & Async Systems",
                description = "Systems Engineering Roadmap: Months 7\u201312 \u00b7 100% production deliverables.",
                targetDate = getCal(2027, 10, 28),
                identityStatement = "I am a high-performance Systems Engineer building rigorous low-level, async, and distributed backends.",
                isActive = true
            )
            goals.add(goalStage2)

            val monthTask_s2_m1 = TaskEntity(
                id = "task_s2_m1",
                goalId = "goal_stage_2",
                title = "Months 7\u20138: Relational Database Internals & Containers",
                description = "Curriculum milestone for Months 7\u20138: Relational Database Internals & Containers",
                scheduledDate = getCal(2027, 5, 1),
                deadlineDate = getCal(2027, 6, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,month1",
                dependsOnTaskIds = "task_s1_m3_item4",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s2_m1)

            val task_s2_m1_i1 = TaskEntity(
                id = "task_s2_m1_item1",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m1",
                title = "LEARN: B-Tree Indexing, MVCC Concurrency & Query Execution Plans",
                description = "How PostgreSQL stores 8KB heap pages on disk, uses Multi-Version Concurrency Control (MVCC) so readers never block writers, and chooses between Sequential Scans, Index Scans, and Hash Joins (EXPLAIN ANALYZE).",
                scheduledDate = getCal(2027, 5, 5),
                deadlineDate = getCal(2027, 5, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,database,async,rust,stage2,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s2_m1_i1)

            val task_s2_m1_i2 = TaskEntity(
                id = "task_s2_m1_item2",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m1",
                title = "BUILD: Project 7 \u2014 Containerized High-Load Database Lab (pg-perf-lab)",
                description = "Spin up PostgreSQL in Podman/Docker, seed 5 million rows of task/telemetry data, write Flyway SQL migrations, and optimize slow queries from 3,000ms down to less than 5ms using composite and partial indexes.",
                scheduledDate = getCal(2027, 5, 15),
                deadlineDate = getCal(2027, 5, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,build,project",
                dependsOnTaskIds = "task_s2_m1_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s2_m1_i2)

            val monthTask_s2_m2 = TaskEntity(
                id = "task_s2_m2",
                goalId = "goal_stage_2",
                title = "Months 9\u201310: Enterprise Java Backend Architecture",
                description = "Curriculum milestone for Months 9\u201310: Enterprise Java Backend Architecture",
                scheduledDate = getCal(2027, 7, 1),
                deadlineDate = getCal(2027, 8, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,month2",
                dependsOnTaskIds = "task_s2_m1_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s2_m2)

            val task_s2_m2_i1 = TaskEntity(
                id = "task_s2_m2_item1",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m2",
                title = "LEARN: Dependency Injection, Connection Pooling & Stateless Auth",
                description = "How Spring Boot wires application layers, why opening a TCP database connection per request kills throughput (solved by HikariCP pools), and how JWT + Redis caching secures and accelerates REST APIs.",
                scheduledDate = getCal(2027, 7, 5),
                deadlineDate = getCal(2027, 7, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,database,async,rust,stage2,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s2_m2_i1)

            val task_s2_m2_i2 = TaskEntity(
                id = "task_s2_m2_item2",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m2",
                title = "BUILD: Project 8 \u2014 Cloud Sync & Auth Backend in Spring Boot (sync-api-java)",
                description = "Build a containerized Spring Boot 3+ API with PostgreSQL, Redis rate-limiting/caching, JWT authentication, and deterministic conflict-resolution endpoints for syncing offline app data.",
                scheduledDate = getCal(2027, 7, 15),
                deadlineDate = getCal(2027, 7, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,build,project",
                dependsOnTaskIds = "task_s2_m2_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s2_m2_i2)

            val monthTask_s2_m3 = TaskEntity(
                id = "task_s2_m3",
                goalId = "goal_stage_2",
                title = "Months 11\u201312: Async Rust & Lock-Free Concurrency",
                description = "Curriculum milestone for Months 11\u201312: Async Rust & Lock-Free Concurrency",
                scheduledDate = getCal(2027, 9, 1),
                deadlineDate = getCal(2027, 10, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,month3",
                dependsOnTaskIds = "task_s2_m2_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s2_m3)

            val task_s2_m3_i1 = TaskEntity(
                id = "task_s2_m3_item1",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m3",
                title = "LEARN: Non-Blocking Event Loops (epoll), State Machines & Atomic Reference Counting",
                description = "How Tokio compiles async/await functions into zero-allocation state machines multiplexed over Linux epoll, and how Arc<RwLock<T>> and MPSC channels share state safely across CPU cores.",
                scheduledDate = getCal(2027, 9, 5),
                deadlineDate = getCal(2027, 9, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,backend,database,async,rust,stage2,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s2_m3_i1)

            val task_s2_m3_i2 = TaskEntity(
                id = "task_s2_m3_item2",
                goalId = "goal_stage_2",
                parentTaskId = "task_s2_m3",
                title = "BUILD: Project 9 \u2014 Real-Time Async Telemetry & WebSocket Engine in Rust (axum-realtime)",
                description = "Build a high-concurrency backend using Tokio, Axum, and SQLx (compile-time checked SQL) that streams live state updates over WebSockets while consuming less than 25 MB of RAM, then benchmark its p99 latency against Project 8.",
                scheduledDate = getCal(2027, 9, 15),
                deadlineDate = getCal(2027, 9, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,backend,database,async,rust,stage2,build,project",
                dependsOnTaskIds = "task_s2_m3_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s2_m3_i2)

            val goalStage3 = GoalEntity(
                id = "goal_stage_3",
                title = "Stage 3: Cross-Platform UI & Distributed Infra",
                description = "Systems Engineering Roadmap: Months 13\u201318 \u00b7 100% production deliverables.",
                targetDate = getCal(2028, 4, 28),
                identityStatement = "I am a high-performance Systems Engineer building rigorous low-level, async, and distributed backends.",
                isActive = true
            )
            goals.add(goalStage3)

            val monthTask_s3_m1 = TaskEntity(
                id = "task_s3_m1",
                goalId = "goal_stage_3",
                title = "Months 13\u201314: Native Desktop Shells & Reactive UI",
                description = "Curriculum milestone for Months 13\u201314: Native Desktop Shells & Reactive UI",
                scheduledDate = getCal(2027, 11, 1),
                deadlineDate = getCal(2027, 12, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,month1",
                dependsOnTaskIds = "task_s2_m3_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s3_m1)

            val task_s3_m1_i1 = TaskEntity(
                id = "task_s3_m1_item1",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m1",
                title = "LEARN: IPC Message Passing & Native Webview Rendering",
                description = "Why Electron apps consume 500MB+ of RAM (bundling a full Chromium instance + Node.js) versus how Tauri v2 pairs a compiled Rust binary with the OS's existing system webview via typed Inter-Process Communication (IPC).",
                scheduledDate = getCal(2027, 11, 5),
                deadlineDate = getCal(2027, 11, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s3_m1_i1)

            val task_s3_m1_i2 = TaskEntity(
                id = "task_s3_m1_item2",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m1",
                title = "BUILD: Project 10 \u2014 Cross-Platform Desktop Productivity App (tauri-workstation)",
                description = "Wrap your Rust SQLite core from Project 6 into a native Linux/Windows desktop application using Tauri v2, TypeScript, and React, featuring sub-40MB RAM usage and instant local state rendering.",
                scheduledDate = getCal(2027, 11, 15),
                deadlineDate = getCal(2027, 11, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,build,project",
                dependsOnTaskIds = "task_s3_m1_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s3_m1_i2)

            val monthTask_s3_m2 = TaskEntity(
                id = "task_s3_m2",
                goalId = "goal_stage_3",
                title = "Months 15\u201316: Cross-Language Bridges (FFI & gRPC)",
                description = "Curriculum milestone for Months 15\u201316: Cross-Language Bridges (FFI & gRPC)",
                scheduledDate = getCal(2028, 1, 1),
                deadlineDate = getCal(2028, 2, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,month2",
                dependsOnTaskIds = "task_s3_m1_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s3_m2)

            val task_s3_m2_i1 = TaskEntity(
                id = "task_s3_m2_item1",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m2",
                title = "LEARN: C Application Binary Interface (ABI), Foreign Function Interfaces & Protobuf",
                description = "How two different languages call each other in the same memory space via C ABI (extern \"C\" + Java's Foreign Function & Memory API / UniFFI), and how gRPC serializes structs into compact binary wire frames (Protobuf) over HTTP/2 across servers.",
                scheduledDate = getCal(2028, 1, 5),
                deadlineDate = getCal(2028, 1, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s3_m2_i1)

            val task_s3_m2_i2 = TaskEntity(
                id = "task_s3_m2_item2",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m2",
                title = "BUILD: Project 11 \u2014 Polyglot Rust-to-Java/Kotlin Native Bridge (native-core-ffi)",
                description = "Compile a compute-heavy Rust scheduling/analytics module into a native .so shared library and invoke it directly from Java/Kotlin with zero network overhead, plus expose a tonic gRPC interface for remote microservices.",
                scheduledDate = getCal(2028, 1, 15),
                deadlineDate = getCal(2028, 1, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,build,project",
                dependsOnTaskIds = "task_s3_m2_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s3_m2_i2)

            val monthTask_s3_m3 = TaskEntity(
                id = "task_s3_m3",
                goalId = "goal_stage_3",
                title = "Months 17\u201318: Event-Driven Pipelines & Automated CI/CD",
                description = "Curriculum milestone for Months 17\u201318: Event-Driven Pipelines & Automated CI/CD",
                scheduledDate = getCal(2028, 3, 1),
                deadlineDate = getCal(2028, 4, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,month3",
                dependsOnTaskIds = "task_s3_m2_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s3_m3)

            val task_s3_m3_i1 = TaskEntity(
                id = "task_s3_m3_item1",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m3",
                title = "LEARN: Append-Only Distributed Logs (Kafka) & Reproducible Build Pipelines",
                description = "How Apache Kafka decouples producers and consumers using partitioned, sequential disk writes, and how GitHub Actions automates static analysis, testing, and multi-stage container builds.",
                scheduledDate = getCal(2028, 3, 5),
                deadlineDate = getCal(2028, 3, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s3_m3_i1)

            val task_s3_m3_i2 = TaskEntity(
                id = "task_s3_m3_item2",
                goalId = "goal_stage_3",
                parentTaskId = "task_s3_m3",
                title = "BUILD: Project 12 \u2014 Event-Driven Analytics Pipeline & CI/CD Harness (event-stream-infra)",
                description = "Connect your Spring Boot and Rust Axum services via Kafka/Redpanda so user events process asynchronously, backed by a GitHub Actions pipeline running cargo clippy, cargo test, gradle test, and container builds on every push.",
                scheduledDate = getCal(2028, 3, 15),
                deadlineDate = getCal(2028, 3, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,desktop,ffi,distributed,kafka,stage3,build,project",
                dependsOnTaskIds = "task_s3_m3_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s3_m3_i2)

            val goalStage4 = GoalEntity(
                id = "goal_stage_4",
                title = "Stage 4: Systems Internals & AI Integration",
                description = "Systems Engineering Roadmap: Months 19\u201324 \u00b7 100% production deliverables.",
                targetDate = getCal(2028, 10, 28),
                identityStatement = "I am a high-performance Systems Engineer building rigorous low-level, async, and distributed backends.",
                isActive = true
            )
            goals.add(goalStage4)

            val monthTask_s4_m1 = TaskEntity(
                id = "task_s4_m1",
                goalId = "goal_stage_4",
                title = "Months 19\u201320: Storage Engine & Kernel Sandbox Internals",
                description = "Curriculum milestone for Months 19\u201320: Storage Engine & Kernel Sandbox Internals",
                scheduledDate = getCal(2028, 5, 1),
                deadlineDate = getCal(2028, 6, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,month1",
                dependsOnTaskIds = "task_s3_m3_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s4_m1)

            val task_s4_m1_i1 = TaskEntity(
                id = "task_s4_m1_item1",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m1",
                title = "LEARN: Log-Structured Merge (LSM) Trees & Linux Kernel Isolation",
                description = "Why write-heavy databases use append-only MemTables + SSTables + Compaction (LSM-Tree) instead of random-access B-Trees, and how Linux namespaces (what a process can see) and cgroups (how much CPU/RAM it can use) form the basis of Docker containers.",
                scheduledDate = getCal(2028, 5, 5),
                deadlineDate = getCal(2028, 5, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s4_m1_i1)

            val task_s4_m1_i2 = TaskEntity(
                id = "task_s4_m1_item2",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m1",
                title = "BUILD: Project 13 \u2014 Embedded Key-Value Storage Engine or Linux Container Sandbox in Rust (lsm-kv / mini-box)",
                description = "Build either a crash-safe key-value store from scratch using a Write-Ahead Log and mmap SSTables, or a 500-line Linux container runner using raw clone(), chroot, and cgroups v2.",
                scheduledDate = getCal(2028, 5, 15),
                deadlineDate = getCal(2028, 5, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,build,project",
                dependsOnTaskIds = "task_s4_m1_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s4_m1_i2)

            val monthTask_s4_m2 = TaskEntity(
                id = "task_s4_m2",
                goalId = "goal_stage_4",
                title = "Months 21\u201322: Local AI Inference, Vector Search & MCP",
                description = "Curriculum milestone for Months 21\u201322: Local AI Inference, Vector Search & MCP",
                scheduledDate = getCal(2028, 7, 1),
                deadlineDate = getCal(2028, 8, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,month2",
                dependsOnTaskIds = "task_s4_m1_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s4_m2)

            val task_s4_m2_i1 = TaskEntity(
                id = "task_s4_m2_item1",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m2",
                title = "LEARN: Vector Embeddings (HNSW), ONNX Graph Execution & Model Context Protocol (MCP)",
                description = "How semantic meaning is represented as high-dimensional float vectors searched via Approximate Nearest Neighbor (pgvector HNSW indexes), how quantized models run locally on CPU/GPU via ONNX Runtime (ort), and how MCP standardizes LLM tool calling.",
                scheduledDate = getCal(2028, 7, 5),
                deadlineDate = getCal(2028, 7, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s4_m2_i1)

            val task_s4_m2_i2 = TaskEntity(
                id = "task_s4_m2_item2",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m2",
                title = "BUILD: Project 14 \u2014 Local-First AI Context & MCP Server in Rust (rust-mcp-rag)",
                description = "Build an MCP server and local RAG engine in Rust using ort (or candle) for local embedding generation and PostgreSQL pgvector / SQLite-vec to let AI agents query and schedule your local tasks privately.",
                scheduledDate = getCal(2028, 7, 15),
                deadlineDate = getCal(2028, 7, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,build,project",
                dependsOnTaskIds = "task_s4_m2_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s4_m2_i2)

            val monthTask_s4_m3 = TaskEntity(
                id = "task_s4_m3",
                goalId = "goal_stage_4",
                title = "Months 23\u201324: Distributed Observability & Production Polish",
                description = "Curriculum milestone for Months 23\u201324: Distributed Observability & Production Polish",
                scheduledDate = getCal(2028, 9, 1),
                deadlineDate = getCal(2028, 10, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,month3",
                dependsOnTaskIds = "task_s4_m2_item2",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 65,
                impactScore = 80
            )
            tasks.add(monthTask_s4_m3)

            val task_s4_m3_i1 = TaskEntity(
                id = "task_s4_m3_item1",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m3",
                title = "LEARN: Distributed Tracing, Tail Latency Profiling & Flamegraphs",
                description = "How OpenTelemetry propagates trace IDs across microservices, how Prometheus scrapes time-series counters/histograms, and how CPU flamegraphs (perf / cargo-flamegraph) pinpoint exact bottleneck functions.",
                scheduledDate = getCal(2028, 9, 5),
                deadlineDate = getCal(2028, 9, 25),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.MEDIUM,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,learn",
                isAutoScheduled = true,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.ANALYTICAL,
                effortScore = 50,
                impactScore = 70
            )
            tasks.add(task_s4_m3_i1)

            val task_s4_m3_i2 = TaskEntity(
                id = "task_s4_m3_item2",
                goalId = "goal_stage_4",
                parentTaskId = "task_s4_m3",
                title = "BUILD: Project 15 \u2014 Full-Stack Telemetry & Capstone Portfolio Release (capstone-system)",
                description = "Instrument your flagship projects with OpenTelemetry, Prometheus, and Grafana dashboards; generate CPU/memory flamegraphs; and publish architecture decision records (ADRs) and reproducible benchmarks on GitHub.",
                scheduledDate = getCal(2028, 9, 15),
                deadlineDate = getCal(2028, 9, 28),
                quadrant = Quadrant.SCHEDULE,
                priority = Priority.HIGH,
                tags = "systems,storage,ai,rag,mcp,observability,stage4,build,project",
                dependsOnTaskIds = "task_s4_m3_item1",
                isFrog = true,
                isAutoScheduled = true,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.ANALYTICAL,
                effortScore = 85,
                impactScore = 95
            )
            tasks.add(task_s4_m3_i2)

            for (goal in goals) {
                database.goalDao().insert(goal)
            }
            for (task in tasks) {
                database.taskDao().insert(task)
            }

            Result.success(Pair(goals.size, tasks.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
