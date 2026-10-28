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
}
