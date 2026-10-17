package com.neuroflow.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomMigration21To22Test {

    private fun createInMemoryDb(initialVersion: Int, setupSql: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
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

    private fun setupVersion21Schema(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `quadrant` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `tags` TEXT NOT NULL,
                `deadlineDate` INTEGER,
                `deadlineTime` INTEGER,
                `scheduledDate` INTEGER,
                `scheduledTime` INTEGER,
                `isScheduleLocked` INTEGER NOT NULL,
                `estimatedDurationMinutes` INTEGER NOT NULL,
                `recurrence` TEXT NOT NULL,
                `recurrenceIntervalDays` INTEGER NOT NULL,
                `reminderFlags` INTEGER NOT NULL,
                `impactScore` INTEGER NOT NULL,
                `valueScore` INTEGER NOT NULL,
                `effortScore` INTEGER NOT NULL,
                `parentTaskId` TEXT,
                `blockingTaskIds` TEXT NOT NULL,
                `dependsOnTaskIds` TEXT NOT NULL,
                `waitingFor` TEXT NOT NULL,
                `totalTimeTrackedMinutes` REAL NOT NULL,
                `sessionCount` INTEGER NOT NULL,
                `lastSessionDurationMinutes` REAL,
                `actualDurationMinutes` REAL,
                `estimationErrorMape` REAL,
                `estimationErrorSmape` REAL,
                `status` TEXT NOT NULL,
                `completedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `energyLevel` TEXT NOT NULL,
                `taskType` TEXT NOT NULL,
                `contextTag` TEXT NOT NULL,
                `goalId` TEXT,
                `ifThenPlan` TEXT NOT NULL,
                `isHabitual` INTEGER NOT NULL,
                `habitStreak` INTEGER NOT NULL,
                `isFrog` INTEGER NOT NULL,
                `postponeCount` INTEGER NOT NULL,
                `focusModePoints` INTEGER NOT NULL,
                `enjoymentScore` INTEGER NOT NULL,
                `isPublicCommitment` INTEGER NOT NULL,
                `isAnxietyTask` INTEGER NOT NULL,
                `goalRiskLevel` INTEGER NOT NULL,
                `isAutoScheduled` INTEGER NOT NULL,
                `lastAutoScheduledAt` INTEGER,
                `habitDate` INTEGER,
                `affectiveForecastError` REAL,
                `woopPromptShown` INTEGER NOT NULL,
                `distractionScore` REAL NOT NULL,
                `schedulingCategory` TEXT,
                `plannedBlockMinutes` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `time_sessions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `taskId` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `pausedAt` INTEGER,
                `totalPausedMs` INTEGER NOT NULL,
                `durationMinutes` REAL NOT NULL,
                `pauseResumeCount` INTEGER NOT NULL,
                `appSwitchCount` INTEGER NOT NULL,
                `interruptionBurstCount` INTEGER NOT NULL,
                `sessionType` TEXT NOT NULL,
                `pomodoroNumber` INTEGER NOT NULL,
                `notes` TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `energy_predictions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `predictedAtMillis` INTEGER NOT NULL,
                `predictedAtDayOfWeek` INTEGER NOT NULL,
                `predictedAtHourOfDay` INTEGER NOT NULL,
                `peakDetectionAgeMillis` INTEGER NOT NULL,
                `sleepLogAgeMillis` INTEGER NOT NULL,
                `sessionDataAgeMillis` INTEGER NOT NULL,
                `baselineRawEnergy` REAL NOT NULL,
                `peakScore` REAL NOT NULL,
                `fatiguePenalty` REAL NOT NULL,
                `sleepPressurePoints` INTEGER NOT NULL,
                `fatiguePercent` INTEGER NOT NULL,
                `momentAdjustment` REAL NOT NULL,
                `momentConfidence` REAL NOT NULL,
                `momentSupportScore` REAL NOT NULL,
                `momentPressureScore` REAL NOT NULL,
                `adjustedRawEnergy` REAL NOT NULL,
                `usableEnergy` INTEGER NOT NULL,
                `chronotype` TEXT NOT NULL,
                `wakeUpHour` INTEGER NOT NULL,
                `peakMinuteOfDay` INTEGER NOT NULL,
                `peakConfidence` REAL NOT NULL,
                `recentFocusMinutes` REAL NOT NULL,
                `recentInterruptionCount` INTEGER NOT NULL,
                `recentAppSwitchCount` INTEGER NOT NULL,
                `activeTaskCount` INTEGER NOT NULL,
                `notificationCount` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL
            )
        """.trimIndent())
    }

    @Test
    fun testMigrationCreatesNormalizedChildTablesAndIndices() {
        val db = createInMemoryDb(21) { setupVersion21Schema(it) }
        try {
            MIGRATION_21_22.migrate(db)

            db.execSQL("""
                INSERT INTO `task_micro_steps` (`id`, `taskId`, `title`, `sortOrder`, `isCompleted`, `estimatedMinutes`)
                VALUES ('ms_1', 'task_1', 'First Step', 0, 0, 5)
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `focus_sessions` (`id`, `taskId`, `startedAt`, `endedAt`, `durationSeconds`, `interruptionCount`, `energyAtStart`, `wasCompleted`)
                VALUES ('fs_1', 'task_1', 1000, 2500, 1500, 0, 60, 1)
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `quick_notes` (`id`, `content`, `isPinned`, `convertedTaskId`, `createdAt`, `updatedAt`)
                VALUES ('qn_1', 'Quick scratch note', 1, NULL, 1000, 1000)
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `protected_slots` (`id`, `title`, `startHour`, `startMinute`, `durationMinutes`, `daysOfWeekMask`, `isEnabled`)
                VALUES ('ps_1', 'Morning Routine', 8, 30, 45, 127, 1)
            """.trimIndent())

            val msCursor = db.query("SELECT title, sortOrder FROM `task_micro_steps` WHERE id = 'ms_1'")
            assertTrue(msCursor.moveToFirst())
            assertEquals("First Step", msCursor.getString(0))
            assertEquals(0, msCursor.getInt(1))
            msCursor.close()

            val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index'")
            val indexNames = mutableListOf<String>()
            while (indexCursor.moveToNext()) {
                indexNames.add(indexCursor.getString(0))
            }
            indexCursor.close()

            assertTrue(indexNames.contains("index_tasks_status_scheduledDate"))
            assertTrue(indexNames.contains("index_tasks_status_deadlineDate"))
            assertTrue(indexNames.contains("index_tasks_parentTaskId"))
            assertTrue(indexNames.contains("index_energy_predictions_predictedAtMillis"))
            assertTrue(indexNames.contains("index_task_micro_steps_task_id_sort"))
            assertTrue(indexNames.contains("index_focus_sessions_started_at"))
            assertTrue(indexNames.contains("index_focus_sessions_task_id"))
        } finally {
            db.close()
        }
    }

    @Test
    fun testMigrationCopiesLegacyTimeSessionsToFocusSessions() {
        val db = createInMemoryDb(21) { setupVersion21Schema(it) }
        try {
            db.execSQL("""
                INSERT INTO `tasks` (
                    `id`, `title`, `description`, `quadrant`, `priority`, `tags`,
                    `isScheduleLocked`, `estimatedDurationMinutes`, `recurrence`, `recurrenceIntervalDays`,
                    `reminderFlags`, `impactScore`, `valueScore`, `effortScore`, `blockingTaskIds`,
                    `dependsOnTaskIds`, `waitingFor`, `totalTimeTrackedMinutes`, `sessionCount`, `status`,
                    `createdAt`, `updatedAt`, `energyLevel`, `taskType`, `contextTag`, `ifThenPlan`,
                    `isHabitual`, `habitStreak`, `isFrog`, `postponeCount`, `focusModePoints`,
                    `enjoymentScore`, `isPublicCommitment`, `isAnxietyTask`, `goalRiskLevel`,
                    `isAutoScheduled`, `woopPromptShown`, `distractionScore`, `plannedBlockMinutes`
                ) VALUES (
                    't_work', 'Code Refactor', '', 'DO_FIRST', 'HIGH', '',
                    0, 30, 'NONE', 1, 0, 50, 50, 50, '', '', '', 25.0, 1, 'ACTIVE',
                    1000, 1000, 'HIGH', 'ANALYTICAL', 'WORK', '',
                    0, 0, 0, 0, 0, 50, 0, 0, 0, 0, 0, -1.0, 30
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `time_sessions` (
                    `id`, `taskId`, `startedAt`, `endedAt`, `pausedAt`, `totalPausedMs`,
                    `durationMinutes`, `pauseResumeCount`, `appSwitchCount`, `interruptionBurstCount`,
                    `sessionType`, `pomodoroNumber`, `notes`
                ) VALUES (
                    'ts_old_1', 't_work', 5000, 6500, NULL, 0, 25.0, 2, 3, 1, 'FOCUS', 1, 'Great session'
                )
            """.trimIndent())

            MIGRATION_21_22.migrate(db)

            val cursor = db.query("SELECT id, taskId, startedAt, endedAt, durationSeconds, interruptionCount, wasCompleted FROM `focus_sessions` WHERE id = 'ts_old_1'")
            assertTrue(cursor.moveToFirst())
            assertEquals("ts_old_1", cursor.getString(0))
            assertEquals("t_work", cursor.getString(1))
            assertEquals(5000L, cursor.getLong(2))
            assertEquals(6500L, cursor.getLong(3))
            assertEquals(1500, cursor.getInt(4))
            assertEquals(6, cursor.getInt(5))
            assertEquals(1, cursor.getInt(6))
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun testMigrationExtractsMicroStepsFromTasksIfThenPlan() {
        val db = createInMemoryDb(21) { setupVersion21Schema(it) }
        try {
            db.execSQL("""
                INSERT INTO `tasks` (
                    `id`, `title`, `description`, `quadrant`, `priority`, `tags`,
                    `isScheduleLocked`, `estimatedDurationMinutes`, `recurrence`, `recurrenceIntervalDays`,
                    `reminderFlags`, `impactScore`, `valueScore`, `effortScore`, `blockingTaskIds`,
                    `dependsOnTaskIds`, `waitingFor`, `totalTimeTrackedMinutes`, `sessionCount`, `status`,
                    `createdAt`, `updatedAt`, `energyLevel`, `taskType`, `contextTag`, `ifThenPlan`,
                    `isHabitual`, `habitStreak`, `isFrog`, `postponeCount`, `focusModePoints`,
                    `enjoymentScore`, `isPublicCommitment`, `isAnxietyTask`, `goalRiskLevel`,
                    `isAutoScheduled`, `woopPromptShown`, `distractionScore`, `plannedBlockMinutes`
                ) VALUES (
                    't_plan', 'Study Chapter', '', 'SCHEDULE', 'MEDIUM', '',
                    0, 45, 'NONE', 1, 0, 50, 50, 50, '', '', '', 0.0, 0, 'ACTIVE',
                    1000, 1000, 'MEDIUM', 'ANALYTICAL', 'STUDY',
                    '[x] Read introduction\n- [ ] Summarize key formulas\n3. Practice questions',
                    0, 0, 0, 0, 0, 50, 0, 0, 0, 0, 0, -1.0, 45
                )
            """.trimIndent())

            MIGRATION_21_22.migrate(db)

            val cursor = db.query("SELECT title, sortOrder, isCompleted FROM `task_micro_steps` WHERE taskId = 't_plan' ORDER BY sortOrder ASC")
            assertTrue(cursor.moveToFirst())
            assertEquals("Read introduction", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(1, cursor.getInt(2))

            assertTrue(cursor.moveToNext())
            assertEquals("Summarize key formulas", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))

            assertTrue(cursor.moveToNext())
            assertEquals("3. Practice questions", cursor.getString(0))
            assertEquals(2, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))

            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun testMigrationPrunesStaleEnergyPredictions() {
        val db = createInMemoryDb(21) { setupVersion21Schema(it) }
        try {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            val oneDayAgo = now - (1L * 24 * 60 * 60 * 1000)

            db.execSQL("""
                INSERT INTO `energy_predictions` (
                    `id`, `predictedAtMillis`, `predictedAtDayOfWeek`, `predictedAtHourOfDay`,
                    `peakDetectionAgeMillis`, `sleepLogAgeMillis`, `sessionDataAgeMillis`,
                    `baselineRawEnergy`, `peakScore`, `fatiguePenalty`, `sleepPressurePoints`,
                    `fatiguePercent`, `momentAdjustment`, `momentConfidence`, `momentSupportScore`,
                    `momentPressureScore`, `adjustedRawEnergy`, `usableEnergy`, `chronotype`,
                    `wakeUpHour`, `peakMinuteOfDay`, `peakConfidence`, `recentFocusMinutes`,
                    `recentInterruptionCount`, `recentAppSwitchCount`, `activeTaskCount`,
                    `notificationCount`, `createdAtMillis`
                ) VALUES
                ('pred_stale', $thirtyDaysAgo, 1, 10, 0, 0, 0, 60.0, 60.0, 0.0, 20, 20, 0.0, 1.0, 0.0, 0.0, 60.0, 60, 'INTERMEDIATE', 7, 600, 1.0, 0.0, 0, 0, 0, 0, $thirtyDaysAgo),
                ('pred_fresh', $oneDayAgo, 2, 11, 0, 0, 0, 75.0, 75.0, 0.0, 15, 15, 0.0, 1.0, 0.0, 0.0, 75.0, 75, 'INTERMEDIATE', 7, 600, 1.0, 0.0, 0, 0, 0, 0, $oneDayAgo)
            """.trimIndent())

            MIGRATION_21_22.migrate(db)

            val cursor = db.query("SELECT id FROM `energy_predictions`")
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("pred_fresh", cursor.getString(0))
            cursor.close()
        } finally {
            db.close()
        }
    }
}
