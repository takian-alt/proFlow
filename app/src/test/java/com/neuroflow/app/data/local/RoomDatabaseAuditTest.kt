package com.neuroflow.app.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.neuroflow.app.data.local.entity.FocusSessionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseAuditTest {

    private lateinit var db: NeuroFlowDatabase

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun createRawDb(initialVersion: Int, setupSql: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(initialVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        setupSql(db)
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test
    fun testMigration1To2IdempotencyWhenColumnAlreadyExists() {
        // BUG-268: 1.json already had recurrenceIntervalDays in Room schema
        val rawDb = createRawDb(1) { database ->
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    recurrenceIntervalDays INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
        }

        try {
            // Must NOT throw "duplicate column name: recurrenceIntervalDays"
            MIGRATION_1_2.migrate(rawDb)

            rawDb.execSQL("INSERT INTO tasks (id, title, description, recurrenceIntervalDays) VALUES ('t1', 'Task 1', 'Desc', 7)")
            val cursor = rawDb.query("SELECT recurrenceIntervalDays FROM tasks WHERE id = 't1'")
            assertTrue(cursor.moveToFirst())
            assertEquals(7, cursor.getInt(0))
            cursor.close()
        } finally {
            rawDb.close()
        }
    }

    @Test
    fun testMigration1To2AddsMissingColumnWhenAbsent() {
        val rawDb = createRawDb(1) { database ->
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
        }

        try {
            MIGRATION_1_2.migrate(rawDb)

            rawDb.execSQL("INSERT INTO tasks (id, title, description) VALUES ('t2', 'Task 2', 'Desc')")
            val cursor = rawDb.query("SELECT recurrenceIntervalDays FROM tasks WHERE id = 't2'")
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            cursor.close()
        } finally {
            rawDb.close()
        }
    }

    @Test
    fun testMigration21To22CalculatesElapsedDurationWhenDurationMinutesZero() {
        // BUG-271: If durationMinutes was 0.0f but endedAt was recorded, durationSeconds should not be 0
        val rawDb = createRawDb(21) { database ->
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `quadrant` TEXT NOT NULL DEFAULT 'SCHEDULE',
                    `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                    `tags` TEXT NOT NULL DEFAULT '',
                    `deadlineDate` INTEGER,
                    `deadlineTime` INTEGER,
                    `scheduledDate` INTEGER,
                    `scheduledTime` INTEGER,
                    `parentTaskId` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    `ifThenPlan` TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `time_sessions` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `taskId` TEXT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER,
                    `pausedAt` INTEGER,
                    `totalPausedMs` INTEGER NOT NULL DEFAULT 0,
                    `durationMinutes` REAL NOT NULL DEFAULT 0,
                    `pauseResumeCount` INTEGER NOT NULL DEFAULT 0,
                    `appSwitchCount` INTEGER NOT NULL DEFAULT 0,
                    `interruptionBurstCount` INTEGER NOT NULL DEFAULT 0,
                    `sessionType` TEXT NOT NULL DEFAULT 'POMODORO_25',
                    `pomodoroNumber` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `energy_predictions` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `predictedAtMillis` INTEGER NOT NULL
                )
            """.trimIndent())
        }

        try {
            rawDb.execSQL("INSERT INTO tasks (id, title, description) VALUES ('task_calc', 'Deep Work', '')")
            // 30 minute session (1800 seconds) where durationMinutes remained 0.0f
            rawDb.execSQL("""
                INSERT INTO time_sessions (id, taskId, startedAt, endedAt, totalPausedMs, durationMinutes)
                VALUES ('ts_calc_1', 'task_calc', 1000000, 2800000, 0, 0.0)
            """.trimIndent())

            MIGRATION_21_22.migrate(rawDb)

            val cursor = rawDb.query("SELECT durationSeconds, wasCompleted FROM focus_sessions WHERE id = 'ts_calc_1'")
            assertTrue(cursor.moveToFirst())
            assertEquals(1800, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
            cursor.close()
        } finally {
            rawDb.close()
        }
    }

    @Test
    fun testSessionRepositorySyncFocusSessionDoesNotCrashOnDeletedOrUntrackedTask() = runBlocking {
        // BUG-269: foreign key constraint failure handled gracefully
        val sessionRepo = SessionRepository(
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        val untrackedSession = TimeSessionEntity(
            id = "sess_no_task",
            taskId = "non_existent_deleted_task_id",
            startedAt = 10000L,
            endedAt = 70000L,
            durationMinutes = 1.0f
        )

        // Must not throw SQLiteConstraintException
        sessionRepo.insert(untrackedSession)
        sessionRepo.update(untrackedSession.copy(notes = "Updated note"))

        val retrieved = db.timeSessionDao().getByTaskId("non_existent_deleted_task_id")
        assertEquals(1, retrieved.size)
        assertEquals("Updated note", retrieved[0].notes)
    }

    @Test
    fun testSessionRepositoryDeleteAllClearsBothTables() = runBlocking {
        val sessionRepo = SessionRepository(
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        db.taskDao().insert(TaskEntity(id = "task_del_all", title = "Task"))
        db.timeSessionDao().insert(TimeSessionEntity(id = "ts_1", taskId = "task_del_all", startedAt = 1000L))
        db.focusSessionDao().insert(FocusSessionEntity(id = "fs_1", taskId = "task_del_all", startedAt = 1000L))

        assertEquals(1, db.timeSessionDao().getAll().size)
        assertEquals(1, db.focusSessionDao().observeAll().let {
            val list = mutableListOf<FocusSessionEntity>()
            // verify not empty
            db.focusSessionDao().getOpenSessions()
        }.size)

        sessionRepo.deleteAll()

        assertEquals(0, db.timeSessionDao().getAll().size)
        assertEquals(0, db.focusSessionDao().getOpenSessions().size)
    }

    @Test
    fun testTaskRepositoryGetMicroStepsFallbackToIfThenPlan() = runBlocking {
        // BUG-272: tasks created via ifThenPlan lazily provide micro-steps for scheduler
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = mockk(relaxed = true),
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null
        )

        val taskWithPlan = TaskEntity(
            id = "task_with_plan",
            title = "Task with IfThenPlan",
            ifThenPlan = "[x] First Step\n- [ ] Second Step\n3. Third Step"
        )
        taskRepo.insert(taskWithPlan)

        // Micro-step DAO is initially empty for this task
        val initialDaoSteps = db.taskMicroStepDao().getByTaskId("task_with_plan")
        assertTrue(initialDaoSteps.isEmpty())

        // Calling getMicroSteps lazily parses and seeds the steps
        val steps = taskRepo.getMicroSteps("task_with_plan")
        assertEquals(3, steps.size)
        assertEquals("First Step", steps[0].title)
        assertTrue(steps[0].isCompleted)
        assertEquals("Second Step", steps[1].title)
        assertTrue(!steps[1].isCompleted)
        assertEquals("3. Third Step", steps[2].title)
        assertTrue(!steps[2].isCompleted)

        // Now cached in Room
        val cachedDaoSteps = db.taskMicroStepDao().getByTaskId("task_with_plan")
        assertEquals(3, cachedDaoSteps.size)
    }

    @Test
    fun testFullSequentialMigrationChainFromV1ToV22() {
        val rawDb = createRawDb(1) { database ->
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    quadrant TEXT NOT NULL DEFAULT 'SCHEDULE',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    tags TEXT NOT NULL DEFAULT '',
                    deadlineDate INTEGER,
                    deadlineTime INTEGER,
                    scheduledDate INTEGER,
                    scheduledTime INTEGER,
                    isScheduleLocked INTEGER NOT NULL DEFAULT 0,
                    estimatedDurationMinutes INTEGER NOT NULL DEFAULT 0,
                    recurrence TEXT NOT NULL DEFAULT 'NONE',
                    recurrenceIntervalDays INTEGER NOT NULL DEFAULT 1,
                    reminderFlags INTEGER NOT NULL DEFAULT 0,
                    impactScore INTEGER NOT NULL DEFAULT 50,
                    parentTaskId TEXT,
                    blockingTaskIds TEXT NOT NULL DEFAULT '',
                    totalTimeTrackedMinutes REAL NOT NULL DEFAULT 0,
                    sessionCount INTEGER NOT NULL DEFAULT 0,
                    lastSessionDurationMinutes REAL,
                    actualDurationMinutes REAL,
                    estimationErrorMape REAL,
                    estimationErrorSmape REAL,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    completedAt INTEGER,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    energyLevel TEXT NOT NULL DEFAULT 'MEDIUM',
                    contextTag TEXT NOT NULL DEFAULT '',
                    goalId TEXT,
                    ifThenPlan TEXT NOT NULL DEFAULT '',
                    isHabitual INTEGER NOT NULL DEFAULT 0,
                    habitStreak INTEGER NOT NULL DEFAULT 0,
                    isFrog INTEGER NOT NULL DEFAULT 0,
                    postponeCount INTEGER NOT NULL DEFAULT 0,
                    focusModePoints INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS time_sessions (
                    id TEXT NOT NULL PRIMARY KEY,
                    taskId TEXT NOT NULL,
                    startedAt INTEGER NOT NULL,
                    endedAt INTEGER,
                    durationMinutes REAL NOT NULL DEFAULT 0,
                    sessionType TEXT NOT NULL DEFAULT 'POMODORO_25',
                    pomodoroNumber INTEGER NOT NULL DEFAULT 0,
                    notes TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    targetDate INTEGER,
                    identityStatement TEXT NOT NULL DEFAULT '',
                    completedTaskCount INTEGER NOT NULL DEFAULT 0,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }

        try {
            // Seed V1 data
            rawDb.execSQL("INSERT INTO tasks (id, title, description, ifThenPlan) VALUES ('t_chain', 'Chain Task', 'Desc', '- [ ] Step 1\n- [x] Step 2')")
            rawDb.execSQL("INSERT INTO time_sessions (id, taskId, startedAt, endedAt, durationMinutes) VALUES ('s_chain', 't_chain', 1000, 2500, 25.0)")
            rawDb.execSQL("INSERT INTO goals (id, title) VALUES ('g_chain', 'Chain Goal')")

            // Execute full migration chain
            MIGRATION_1_2.migrate(rawDb)
            MIGRATION_2_3.migrate(rawDb)
            MIGRATION_3_4.migrate(rawDb)
            MIGRATION_4_5.migrate(rawDb)
            MIGRATION_5_6.migrate(rawDb)
            MIGRATION_6_7.migrate(rawDb)
            MIGRATION_7_8.migrate(rawDb)
            MIGRATION_8_9.migrate(rawDb)
            MIGRATION_9_10.migrate(rawDb)
            MIGRATION_10_11.migrate(rawDb)
            MIGRATION_11_12.migrate(rawDb)
            MIGRATION_12_13.migrate(rawDb)
            MIGRATION_13_14.migrate(rawDb)
            MIGRATION_14_15.migrate(rawDb)
            MIGRATION_15_16.migrate(rawDb)
            MIGRATION_16_17.migrate(rawDb)
            MIGRATION_17_18.migrate(rawDb)
            MIGRATION_18_19.migrate(rawDb)
            MIGRATION_19_20.migrate(rawDb)
            MIGRATION_20_21.migrate(rawDb)
            MIGRATION_21_22.migrate(rawDb)

            // Verify micro steps extracted
            val msCursor = rawDb.query("SELECT COUNT(*) FROM task_micro_steps WHERE taskId = 't_chain'")
            assertTrue(msCursor.moveToFirst())
            assertEquals(2, msCursor.getInt(0))
            msCursor.close()

            // Verify focus sessions migrated
            val fsCursor = rawDb.query("SELECT COUNT(*) FROM focus_sessions WHERE taskId = 't_chain'")
            assertTrue(fsCursor.moveToFirst())
            assertEquals(1, fsCursor.getInt(0))
            fsCursor.close()

            // Verify new tables accept inserts
            rawDb.execSQL("INSERT INTO quick_notes (id, content, isPinned, createdAt, updatedAt) VALUES ('qn_1', 'Content', 0, 1000, 1000)")
            rawDb.execSQL("INSERT INTO protected_slots (id, title, startHour, startMinute, durationMinutes, daysOfWeekMask, isEnabled) VALUES ('ps_1', 'Slot', 9, 0, 60, 127, 1)")
            rawDb.execSQL("INSERT INTO gate_requests (id, packageName, appLabel, statedReason, taskType, mismatchCount, elapsedMillis, grantSource, createdAtMillis) VALUES ('gr_1', 'pkg', 'App', 'reason', 'TYPE', 0, 0, 'CODE', 1000)")

            val qnCursor = rawDb.query("SELECT COUNT(*) FROM quick_notes")
            assertTrue(qnCursor.moveToFirst())
            assertEquals(1, qnCursor.getInt(0))
            qnCursor.close()
        } finally {
            rawDb.close()
        }
    }

    @Test
    fun testTypeConvertersNullResilience() {
        // BUG-307: Null string inputs to enum converters must not throw NullPointerException
        val converters = Converters()
        assertEquals(com.neuroflow.app.domain.model.Quadrant.SCHEDULE, converters.toQuadrant(null))
        assertEquals(com.neuroflow.app.domain.model.Priority.MEDIUM, converters.toPriority(null))
        assertEquals(com.neuroflow.app.domain.model.TaskStatus.ACTIVE, converters.toTaskStatus(null))
        assertEquals(com.neuroflow.app.domain.model.Recurrence.NONE, converters.toRecurrence(null))
        assertEquals(com.neuroflow.app.domain.model.EnergyLevel.MEDIUM, converters.toEnergyLevel(null))
        assertEquals(com.neuroflow.app.domain.model.TaskType.ANALYTICAL, converters.toTaskType(null))
        assertEquals(com.neuroflow.app.domain.model.RewardTier.NONE, converters.toRewardTier(null))
        assertEquals(com.neuroflow.app.domain.model.HyperFocusState.INACTIVE, converters.toHyperFocusState(null))
    }

    @Test
    fun testTaskRepositoryDeleteCascadesSubtasksAndCleansPart2Dependencies() = runBlocking {
        // BUG-308: Deleting task must cascade delete subtasks, clean up dependencies on Part 2, and purge open time sessions
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val parentTask = TaskEntity(id = "parent_task_1", title = "Parent Task")
        val part2Task = TaskEntity(id = "parent_task_1_part2", title = "Parent Task (Part 2/2)", parentTaskId = "parent_task_1")
        val subtask = TaskEntity(id = "subtask_1", title = "Subtask 1", parentTaskId = "parent_task_1")
        val otherTask = TaskEntity(
            id = "other_task_1",
            title = "Other Task",
            dependsOnTaskIds = "parent_task_1_part2,subtask_1,other_valid_task"
        )
        val validTask = TaskEntity(id = "other_valid_task", title = "Valid Prereq")

        taskRepo.insert(parentTask)
        taskRepo.insert(part2Task)
        taskRepo.insert(subtask)
        taskRepo.insert(otherTask)
        taskRepo.insert(validTask)

        // Open session on parent task
        db.timeSessionDao().insert(
            TimeSessionEntity(
                id = "open_sess_parent",
                taskId = "parent_task_1",
                startedAt = 1000L,
                endedAt = null
            )
        )

        assertEquals(1, db.timeSessionDao().getOpenSessions().size)
        assertEquals(2, db.taskDao().getSubtasks("parent_task_1").size)

        // Delete parent task
        taskRepo.delete(parentTask)

        // Parent, part2, and subtasks must all be deleted
        assertEquals(null, db.taskDao().getById("parent_task_1"))
        assertEquals(null, db.taskDao().getById("parent_task_1_part2"))
        assertEquals(0, db.taskDao().getSubtasks("parent_task_1").size)

        // Open session on parent task must be purged
        assertEquals(0, db.timeSessionDao().getOpenSessions().size)

        // otherTask's dependsOnTaskIds must have parent_task_1_part2 and subtask_1 removed
        val updatedOther = db.taskDao().getById("other_task_1")
        assertNotNull(updatedOther)
        assertEquals("other_valid_task", updatedOther?.dependsOnTaskIds)
    }

    @Test
    fun testTaskRepositorySplitTaskIntoSessionsPreservesPrerequisitesAndSchedules() = runBlocking {
        // BUG-309: splitTaskIntoSessions must retain Part 1 prerequisites/schedules and redirect downstream dependencies
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val parentTask = TaskEntity(
            id = "task_to_split",
            title = "Large Project Task",
            estimatedDurationMinutes = 90,
            dependsOnTaskIds = "prereq_alpha",
            scheduledDate = 1700000000000L,
            scheduledTime = 36000000L,
            waitingFor = "Architect sign-off"
        )
        val downstreamTask = TaskEntity(
            id = "downstream_task",
            title = "Review Large Project",
            dependsOnTaskIds = "task_to_split"
        )

        taskRepo.insert(parentTask)
        taskRepo.insert(downstreamTask)

        val parts = taskRepo.splitTaskIntoSessions(parentTask, sessionDurationMinutes = 45)
        assertEquals(2, parts.size)

        // Part 1 must retain prerequisites and scheduling metadata
        val part1 = parts[0]
        assertEquals("prereq_alpha", part1.dependsOnTaskIds)
        assertEquals(1700000000000L, part1.scheduledDate)
        assertEquals(36000000L, part1.scheduledTime)
        assertEquals("Architect sign-off", part1.waitingFor)

        // Part 2 must depend on Part 1
        val part2 = parts[1]
        assertEquals(part1.id, part2.dependsOnTaskIds)

        // downstreamTask must have its dependency redirected to the final session part (Part 2)
        val updatedDownstream = db.taskDao().getById("downstream_task")
        assertNotNull(updatedDownstream)
        assertEquals(part2.id, updatedDownstream?.dependsOnTaskIds)
    }

    @Test
    fun testTaskDaoUpsertPreservesMicroStepsAndFocusSessionsWithoutCascadeDeletion() = runBlocking {
        // BUG-345: With foreign keys enabled, @Upsert prevents SQLite INSERT OR REPLACE from cascade-deleting micro-steps and focus sessions
        val task = TaskEntity(id = "task_upsert_test", title = "Original Task")
        db.taskDao().insert(task)

        val step = com.neuroflow.app.data.local.entity.TaskMicroStepEntity(
            id = "step_1",
            taskId = "task_upsert_test",
            title = "Substep 1",
            sortOrder = 0
        )
        db.taskMicroStepDao().insert(step)

        val session = FocusSessionEntity(
            id = "sess_1",
            taskId = "task_upsert_test",
            startedAt = 1000L
        )
        db.focusSessionDao().insert(session)

        assertEquals(1, db.taskMicroStepDao().getByTaskId("task_upsert_test").size)
        assertEquals(1, db.focusSessionDao().getOpenSessions().size)

        // Call taskDao.insert (now @Upsert) with updated title
        db.taskDao().insert(task.copy(title = "Updated Task Title", impactScore = 90))

        // Child records must NOT be cascade-deleted!
        assertEquals(1, db.taskMicroStepDao().getByTaskId("task_upsert_test").size)
        assertEquals(1, db.focusSessionDao().getOpenSessions().size)
        assertEquals("Updated Task Title", db.taskDao().getById("task_upsert_test")?.title)
    }

    @Test
    fun testTaskDaoObserveTasksForDateIncludesDatelessTimedTasksWithAbsoluteTimestamps() = runBlocking {
        // BUG-346: Dateless tasks with absolute scheduledTime must be returned by observeTasksForDate and getTasksForDate
        val dayStart = 1730000000000L // e.g. Oct 27
        val dayEnd = dayStart + 86_400_000L
        val timedTask = TaskEntity(
            id = "dateless_timed_task",
            title = "Afternoon Meeting",
            scheduledDate = null,
            scheduledTime = dayStart + 14 * 3600_000L // 14:00 today
        )
        db.taskDao().insert(timedTask)

        val tasks = db.taskDao().getTasksForDate(dayStart, dayEnd)
        assertEquals(1, tasks.size)
        assertEquals("dateless_timed_task", tasks[0].id)
    }

    @Test
    fun testTaskRepositoryDeleteCascadesHashPartContinuationTasks() = runBlocking {
        // BUG-347: Deleting parent task must cascade delete #part continuation tasks and clean dependencies
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val parent = TaskEntity(id = "base_task", title = "Base Task")
        val part1 = TaskEntity(id = "base_task#part1", title = "Base Task (Part 1/2)")
        val part2 = TaskEntity(id = "base_task#part2", title = "Base Task (Part 2/2)", parentTaskId = "base_task")
        val downstream = TaskEntity(id = "downstream_hash", title = "Follow-up", dependsOnTaskIds = "base_task#part2")

        taskRepo.insert(parent)
        taskRepo.insert(part1)
        taskRepo.insert(part2)
        taskRepo.insert(downstream)

        taskRepo.delete(parent)

        // All parts must be deleted
        assertEquals(null, db.taskDao().getById("base_task"))
        assertEquals(null, db.taskDao().getById("base_task#part1"))
        assertEquals(null, db.taskDao().getById("base_task#part2"))

        // Downstream dependency on base_task#part2 must be cleaned up
        val updatedDownstream = db.taskDao().getById("downstream_hash")
        assertNotNull(updatedDownstream)
        assertEquals("", updatedDownstream?.dependsOnTaskIds)
    }

    @Test
    fun testTaskRepositorySplitTaskIntoSessionsAdvancesRecurrenceForRecurringHabits() = runBlocking {
        // BUG-348: Splitting a recurring task into sessions must advance recurrence and spawn next occurrence
        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = workManager,
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val recurringTask = TaskEntity(
            id = "recurring_habit_to_split",
            title = "Morning Deep Work",
            estimatedDurationMinutes = 90,
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = 1730000000000L,
            habitStreak = 5
        )
        taskRepo.insert(recurringTask)

        val sessions = taskRepo.splitTaskIntoSessions(recurringTask, sessionDurationMinutes = 45)
        assertEquals(2, sessions.size)

        // Parent must be marked COMPLETED
        val parent = db.taskDao().getById("recurring_habit_to_split")
        assertNotNull(parent)
        assertEquals(com.neuroflow.app.domain.model.TaskStatus.COMPLETED, parent?.status)

        // Next recurring active instance must be spawned in tasks
        val allTasks = db.taskDao().getAllTasks()
        val spawned = allTasks.firstOrNull { it.id != "recurring_habit_to_split" && it.id !in sessions.map { s -> s.id } && it.status == com.neuroflow.app.domain.model.TaskStatus.ACTIVE }
        assertNotNull(spawned)
        assertEquals("Morning Deep Work", spawned?.title)
        assertEquals(com.neuroflow.app.domain.model.Recurrence.DAILY, spawned?.recurrence)
        assertEquals(6, spawned?.habitStreak)
    }

    @Test
    fun testTaskRepositoryCompleteAndRecurPreservesScheduledDateWhenScheduledTimeAbsolute() = runBlocking {
        // BUG-349: If scheduledDate is null but scheduledTime >= ONE_DAY_MILLIS, nextScheduledDate must not be dropped to null
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val now = 1730000000000L // e.g. Oct 27
        val dayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val timedRecurring = TaskEntity(
            id = "timed_recurring_task",
            title = "Daily Standup",
            scheduledDate = null,
            scheduledTime = dayStart + 10 * 3600_000L, // absolute epoch timestamp at 10:00 AM
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = dayStart
        )
        taskRepo.insert(timedRecurring)

        val nextId = taskRepo.completeAndRecur(timedRecurring, now)
        assertNotNull(nextId)

        val spawned = db.taskDao().getById(nextId!!)
        assertNotNull(spawned)
        // scheduledDate must be populated for tomorrow instead of being dropped to null
        assertNotNull(spawned?.scheduledDate)
        assertEquals(spawned?.habitDate, spawned?.scheduledDate)
        // scheduledTime must be normalized to intraday millisecond offset (10:00 AM = 36,000,000 ms)
        assertEquals(10 * 3600_000L, spawned?.scheduledTime)
    }

    @Test
    fun testFocusSessionDaoDeleteByTaskIdAndOpenSessions() = runBlocking {
        // BUG-350: FocusSessionDao deleteByTaskId and deleteOpenSessionsByTaskId
        val sessionRepo = SessionRepository(
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        db.taskDao().insert(TaskEntity(id = "task_fs_test", title = "Task"))
        val openSess = FocusSessionEntity(id = "fs_open", taskId = "task_fs_test", startedAt = 1000L, endedAt = null)
        val closedSess = FocusSessionEntity(id = "fs_closed", taskId = "task_fs_test", startedAt = 1000L, endedAt = 2000L, durationSeconds = 1000)

        db.focusSessionDao().insert(openSess)
        db.focusSessionDao().insert(closedSess)

        assertEquals(1, db.focusSessionDao().getOpenSessions().size)

        // Delete open sessions
        sessionRepo.deleteOpenSessionsByTaskId("task_fs_test")
        assertEquals(0, db.focusSessionDao().getOpenSessions().size)
        assertNotNull(db.focusSessionDao().getById("fs_closed"))

        // Delete all by task ID
        sessionRepo.deleteByTaskId("task_fs_test")
        assertEquals(null, db.focusSessionDao().getById("fs_closed"))
    }

    @Test
    fun testCompleteAndRecurClosesOpenTimeSessionAndPreservesTrackedMinutes() = runBlocking {
        // BUG-393: completeAndRecur must close open time sessions and preserve tracked minutes
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val task = TaskEntity(id = "task_open_sess", title = "Task with Open Session")
        taskRepo.insert(task)

        val now = 1730000000000L
        val startedAt = now - (15 * 60_000L) // 15 mins ago
        db.timeSessionDao().insert(
            TimeSessionEntity(
                id = "sess_to_close",
                taskId = "task_open_sess",
                startedAt = startedAt,
                endedAt = null,
                durationMinutes = 0f
            )
        )

        assertEquals(1, db.timeSessionDao().getOpenSessions().size)

        // Complete the task
        taskRepo.completeAndRecur(task, now)

        val completed = db.taskDao().getById("task_open_sess")
        assertNotNull(completed)
        assertEquals(com.neuroflow.app.domain.model.TaskStatus.COMPLETED, completed?.status)

        // Open session must now be closed and trackedMinutes captured
        assertEquals(0, db.timeSessionDao().getOpenSessions().size)
        val closedSession = db.timeSessionDao().getByTaskId("task_open_sess").firstOrNull()
        assertNotNull(closedSession)
        assertEquals(now, closedSession?.endedAt)
        assertEquals(15.0f, closedSession?.durationMinutes)
    }

    @Test
    fun testDeleteContinuationPartWithNaturalPartInSlugRestoresParent() = runBlocking {
        // BUG-394: Task IDs with 'part' in natural slug must not corrupt parentId
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val parent = TaskEntity(
            id = "car_parts_catalog",
            title = "Car Parts Catalog (Part 1/2)",
            plannedBlockMinutes = 60
        )
        val part2 = TaskEntity(
            id = "car_parts_catalog#part2",
            title = "Car Parts Catalog (Part 2/2)",
            parentTaskId = null // tests fallback slug extraction
        )

        taskRepo.insert(parent)
        taskRepo.insert(part2)

        // Delete Part 2 directly
        taskRepo.delete(part2)

        // Parent should NOT be corrupted to "car" and its title/plannedBlockMinutes must be restored
        val restoredParent = db.taskDao().getById("car_parts_catalog")
        assertNotNull(restoredParent)
        assertEquals("Car Parts Catalog", restoredParent?.title)
        assertEquals(0, restoredParent?.plannedBlockMinutes)
    }

    @Test
    fun testCompleteAndRecurDoesNotAdvanceRecurrenceOnPart2Continuation() = runBlocking {
        // BUG-395: Part 2 continuation tasks (#part2, _part2, or (Part 2/2)) must NEVER advance recurrence
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val habitPart2 = TaskEntity(
            id = "morning_habit#part2",
            title = "Morning Habit (Part 2/2)",
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = 1730000000000L
        )
        taskRepo.insert(habitPart2)

        val result = taskRepo.completeAndRecur(habitPart2, 1730000000000L)
        // Must return null and NOT spawn next day's recurring occurrence
        assertEquals(null, result)
        assertEquals(1, db.taskDao().getAllTasks().size)
    }

    @Test
    fun testSplitTaskIntoSessionsClonesMicroStepsToPart1() = runBlocking {
        // BUG-396: splitTaskIntoSessions must clone micro-steps to Part 1
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao()
        )

        val parent = TaskEntity(
            id = "large_task_with_steps",
            title = "Large Architectural Design",
            estimatedDurationMinutes = 90
        )
        taskRepo.insert(parent)

        // Seed 3 micro-steps in task_micro_steps
        val steps = listOf(
            com.neuroflow.app.data.local.entity.TaskMicroStepEntity(
                id = "step_0",
                taskId = "large_task_with_steps",
                title = "Draft Interface",
                sortOrder = 0,
                isCompleted = true
            ),
            com.neuroflow.app.data.local.entity.TaskMicroStepEntity(
                id = "step_1",
                taskId = "large_task_with_steps",
                title = "Wire Database",
                sortOrder = 1,
                isCompleted = false
            )
        )
        db.taskMicroStepDao().insertAll(steps)

        val sessions = taskRepo.splitTaskIntoSessions(parent, sessionDurationMinutes = 45)
        assertEquals(2, sessions.size)
        val part1Id = sessions[0].id

        // Micro-steps must be cloned to Part 1 with identical completion states
        val part1Steps = db.taskMicroStepDao().getByTaskId(part1Id)
        assertEquals(2, part1Steps.size)
        assertEquals("Draft Interface", part1Steps[0].title)
        assertTrue(part1Steps[0].isCompleted)
        assertEquals("Wire Database", part1Steps[1].title)
        assertTrue(!part1Steps[1].isCompleted)
    }

    @Test
    fun testGetSessionsSinceFiltersHistoricalSessionsViaSql() = runBlocking {
        // BUG-397: getSessionsSince filters at the SQL level
        val sessionRepo = SessionRepository(
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        db.taskDao().insert(TaskEntity(id = "task_since", title = "Task Since"))
        db.timeSessionDao().insert(TimeSessionEntity(id = "s1", taskId = "task_since", startedAt = 1000L, pauseResumeCount = 1))
        db.timeSessionDao().insert(TimeSessionEntity(id = "s2", taskId = "task_since", startedAt = 5000L, pauseResumeCount = 2))
        db.timeSessionDao().insert(TimeSessionEntity(id = "s3", taskId = "task_since", startedAt = 10000L, pauseResumeCount = 3))

        val sessionsSince = db.timeSessionDao().getSessionsSince(4000L)
        assertEquals(2, sessionsSince.size)
        assertEquals("s3", sessionsSince[0].id)
        assertEquals("s2", sessionsSince[1].id)

        val summary = sessionRepo.getInterruptionSummary(4000L)
        assertEquals(5, summary.totalPauseResumeCount)
    }

    @Test
    fun testCompleteAndRecurSuppressesRecurrenceOnMultiPartContinuations() = runBlocking {
        // BUG-427: Multi-part continuation tasks (#part3, _part3, (Part 3/3)) must NEVER advance recurrence
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        val habitPart3 = TaskEntity(
            id = "study_habit#part3",
            title = "Study Japanese (Part 3/3)",
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = 1730000000000L
        )
        taskRepo.insert(habitPart3)

        val result = taskRepo.completeAndRecur(habitPart3, 1730000000000L)
        assertEquals(null, result)
        assertEquals(1, db.taskDao().getAllTasks().size)
    }

    @Test
    fun testTaskRepositoryDeletePurgesMultiPartContinuationsAndRemovesDependencies() = runBlocking {
        // BUG-428: Deleting parent task must cascade delete #part3+ continuation parts and clean downstream dependencies
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        val parent = TaskEntity(id = "multi_part_task", title = "Multi Part Task")
        val part1 = TaskEntity(id = "multi_part_task#part1", title = "Multi Part Task (Part 1/3)")
        val part2 = TaskEntity(id = "multi_part_task#part2", title = "Multi Part Task (Part 2/3)")
        val part3 = TaskEntity(id = "multi_part_task#part3", title = "Multi Part Task (Part 3/3)")
        val downstream = TaskEntity(
            id = "downstream_dep",
            title = "Downstream Dependent",
            dependsOnTaskIds = "multi_part_task#part3,other_valid_dep"
        )
        val validDep = TaskEntity(id = "other_valid_dep", title = "Valid Dep")

        taskRepo.insert(parent)
        taskRepo.insert(part1)
        taskRepo.insert(part2)
        taskRepo.insert(part3)
        taskRepo.insert(downstream)
        taskRepo.insert(validDep)

        // Open focus session on Part 3
        db.focusSessionDao().insert(
            FocusSessionEntity(
                id = "fs_part3",
                taskId = "multi_part_task#part3",
                startedAt = 1000L
            )
        )
        assertEquals(1, db.focusSessionDao().getOpenSessions().size)

        // Delete parent task
        taskRepo.delete(parent)

        // All parts including part 3 must be deleted
        assertEquals(null, db.taskDao().getById("multi_part_task"))
        assertEquals(null, db.taskDao().getById("multi_part_task#part1"))
        assertEquals(null, db.taskDao().getById("multi_part_task#part2"))
        assertEquals(null, db.taskDao().getById("multi_part_task#part3"))

        // Downstream dependency on multi_part_task#part3 must be removed
        val updatedDownstream = db.taskDao().getById("downstream_dep")
        assertNotNull(updatedDownstream)
        assertEquals("other_valid_dep", updatedDownstream?.dependsOnTaskIds)

        // Open focus session on part 3 must be deleted
        assertEquals(0, db.focusSessionDao().getOpenSessions().size)
    }

    @Test
    fun testTaskDaoObserveTasksForDateExcludesRescheduledRecurringHabitsFromToday() = runBlocking {
        // BUG-429: Rescheduling a recurring habit to tomorrow must exclude it from today's query
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        val todayStart = 1730000000000L
        val todayEnd = todayStart + 86_400_000L
        val tomorrowStart = todayEnd
        val tomorrowEnd = tomorrowStart + 86_400_000L

        val habit = TaskEntity(
            id = "habit_rescheduled",
            title = "Morning Meditation",
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = tomorrowStart // Explicitly scheduled for tomorrow
        )
        taskRepo.insert(habit)

        // Must NOT appear on today's schedule
        val todayTasks = db.taskDao().getTasksForDate(todayStart, todayEnd)
        assertEquals(0, todayTasks.size)

        // Must appear on tomorrow's schedule
        val tomorrowTasks = db.taskDao().getTasksForDate(tomorrowStart, tomorrowEnd)
        assertEquals(1, tomorrowTasks.size)
        assertEquals("habit_rescheduled", tomorrowTasks[0].id)
    }

    @Test
    fun testSessionRepositorySyncFocusSessionPreservesExistingEnergyAtStart() = runBlocking {
        // BUG-430: Session updates must preserve existing recorded energyAtStart rather than clobbering to 50
        val sessionRepo = SessionRepository(
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        db.taskDao().insert(TaskEntity(id = "task_energy_test", title = "Energy Task"))
        db.focusSessionDao().insert(
            FocusSessionEntity(
                id = "sess_energy_1",
                taskId = "task_energy_test",
                startedAt = 1000L,
                energyAtStart = 85
            )
        )

        val timeSession = TimeSessionEntity(
            id = "sess_energy_1",
            taskId = "task_energy_test",
            startedAt = 1000L,
            endedAt = 2000L,
            durationMinutes = 15f
        )
        sessionRepo.update(timeSession)

        val focusSession = db.focusSessionDao().getById("sess_energy_1")
        assertNotNull(focusSession)
        assertEquals(85, focusSession?.energyAtStart)
        assertEquals(15 * 60, focusSession?.durationSeconds)
        assertTrue(focusSession?.wasCompleted == true)
    }

    @Test
    fun testSplitTaskIntoSessionsClonesMicroStepsFromIfThenPlanAndClearsOnSubsequentParts() = runBlocking {
        // BUG-431: splitTaskIntoSessions clones steps from ifThenPlan to Part 1 and clears on Part 2..N
        val taskRepo = TaskRepository(
            taskDao = db.taskDao(),
            hyperFocusManager = dagger.Lazy { mockk<HyperFocusManager>(relaxed = true) },
            workManager = mockk(relaxed = true),
            taskMicroStepDao = db.taskMicroStepDao(),
            calendarSyncRepository = null,
            timeSessionDao = db.timeSessionDao(),
            focusSessionDao = db.focusSessionDao()
        )

        val parent = TaskEntity(
            id = "task_to_split_ifthen",
            title = "Architectural Refactoring",
            estimatedDurationMinutes = 90,
            ifThenPlan = "[ ] Define Models\n[x] Run Benchmarks"
        )
        taskRepo.insert(parent)

        // Open focus session on parent task
        db.timeSessionDao().insert(
            TimeSessionEntity(
                id = "ts_split_open",
                taskId = "task_to_split_ifthen",
                startedAt = 1000L
            )
        )
        db.focusSessionDao().insert(
            FocusSessionEntity(
                id = "ts_split_open",
                taskId = "task_to_split_ifthen",
                startedAt = 1000L
            )
        )

        val sessions = taskRepo.splitTaskIntoSessions(parent, sessionDurationMinutes = 45)
        assertEquals(2, sessions.size)

        // Part 1 must have micro-steps cloned in task_micro_steps
        val part1Steps = db.taskMicroStepDao().getByTaskId(sessions[0].id)
        assertEquals(2, part1Steps.size)
        assertEquals("Define Models", part1Steps[0].title)
        assertTrue(!part1Steps[0].isCompleted)
        assertEquals("Run Benchmarks", part1Steps[1].title)
        assertTrue(part1Steps[1].isCompleted)

        // Part 1 preserves ifThenPlan, Part 2 has empty ifThenPlan
        assertEquals("[ ] Define Models\n[x] Run Benchmarks", sessions[0].ifThenPlan)
        assertEquals("", sessions[1].ifThenPlan)

        // Open focus sessions on parent task must be closed in both tables
        assertEquals(0, db.timeSessionDao().getOpenSessions().size)
        assertEquals(0, db.focusSessionDao().getOpenSessions().size)
        val closedFocusSession = db.focusSessionDao().getById("ts_split_open")
        assertNotNull(closedFocusSession)
        assertTrue(closedFocusSession?.wasCompleted == true)
    }
}
