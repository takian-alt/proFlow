package com.neuroflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.FocusSessionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.ProtectedSlotDao
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import com.neuroflow.app.data.local.entity.FocusSessionEntity
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.ProtectedSlotEntity
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.feature.hyperfocus.data.GateRequestDao
import com.neuroflow.app.feature.hyperfocus.data.GateRequestEntity
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionEntity
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeDao
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceIntervalDays INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN valueScore INTEGER NOT NULL DEFAULT 50")
        db.execSQL("ALTER TABLE tasks ADD COLUMN effortScore INTEGER NOT NULL DEFAULT 50")
        db.execSQL("ALTER TABLE tasks ADD COLUMN waitingFor TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE time_sessions ADD COLUMN pausedAt INTEGER")
        db.execSQL("ALTER TABLE time_sessions ADD COLUMN totalPausedMs INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN taskType TEXT NOT NULL DEFAULT 'ANALYTICAL'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN enjoymentScore INTEGER NOT NULL DEFAULT 50")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isPublicCommitment INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isAnxietyTask INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN goalRiskLevel INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN dependsOnTaskIds TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN habitDate INTEGER")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN affectiveForecastError REAL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN woopPromptShown INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS woop_data (
                taskId TEXT NOT NULL PRIMARY KEY,
                wish TEXT NOT NULL DEFAULT '',
                outcome TEXT NOT NULL DEFAULT '',
                obstacle TEXT NOT NULL DEFAULT '',
                plan TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ulysses_contracts (
                id TEXT NOT NULL PRIMARY KEY,
                taskId TEXT NOT NULL,
                deadlineAt INTEGER NOT NULL,
                consequence TEXT NOT NULL,
                outcome TEXT,
                createdAt INTEGER NOT NULL
            )
        """)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN distractionScore REAL NOT NULL DEFAULT -1")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS unlock_codes (
                id TEXT NOT NULL PRIMARY KEY,
                encryptedCode TEXT NOT NULL,
                tier TEXT NOT NULL,
                sessionId TEXT NOT NULL,
                isUsed INTEGER NOT NULL DEFAULT 0,
                usedAt INTEGER,
                unlockedUntil INTEGER
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS hyperfocus_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                startedAt INTEGER NOT NULL,
                state TEXT NOT NULL,
                blockedPackages TEXT NOT NULL,
                dailyTaskTarget INTEGER NOT NULL,
                tasksCompletedAtStart INTEGER NOT NULL,
                currentTier TEXT NOT NULL,
                fullyUnlockedAt INTEGER,
                endedAt INTEGER
            )
        """)
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE unlock_codes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sleep_logs (
                id TEXT NOT NULL PRIMARY KEY,
                startAt INTEGER NOT NULL,
                endAt INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                source TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_logs_startAt ON sleep_logs(startAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_logs_endAt ON sleep_logs(endAt)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE time_sessions ADD COLUMN pauseResumeCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE time_sessions ADD COLUMN appSwitchCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE time_sessions ADD COLUMN interruptionBurstCount INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Phase 2 telemetry: energy prediction audit trail and backtesting data
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS energy_predictions (
                id TEXT NOT NULL PRIMARY KEY,
                predictedAtMillis INTEGER NOT NULL,
                predictedAtDayOfWeek INTEGER NOT NULL,
                predictedAtHourOfDay INTEGER NOT NULL,
                peakDetectionAgeMillis INTEGER NOT NULL,
                sleepLogAgeMillis INTEGER NOT NULL,
                sessionDataAgeMillis INTEGER NOT NULL,
                baselineRawEnergy REAL NOT NULL,
                peakScore REAL NOT NULL,
                fatiguePenalty REAL NOT NULL,
                sleepPressurePoints INTEGER NOT NULL,
                fatiguePercent INTEGER NOT NULL,
                momentAdjustment REAL NOT NULL,
                momentConfidence REAL NOT NULL,
                momentSupportScore REAL NOT NULL,
                momentPressureScore REAL NOT NULL,
                adjustedRawEnergy REAL NOT NULL,
                usableEnergy INTEGER NOT NULL,
                chronotype TEXT NOT NULL,
                wakeUpHour INTEGER NOT NULL,
                peakMinuteOfDay INTEGER NOT NULL,
                peakConfidence REAL NOT NULL,
                recentFocusMinutes REAL NOT NULL,
                recentInterruptionCount INTEGER NOT NULL,
                recentAppSwitchCount INTEGER NOT NULL,
                activeTaskCount INTEGER NOT NULL,
                notificationCount INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN isAutoScheduled INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add lastAutoScheduledAt column for replanning cooldown mechanism
        db.execSQL("ALTER TABLE tasks ADD COLUMN lastAutoScheduledAt INTEGER")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add explicit scheduling category — nullable TEXT so existing tasks default to null
        // (category is inferred at runtime from tags/taskType when null)
        db.execSQL("ALTER TABLE tasks ADD COLUMN schedulingCategory TEXT")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notes` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `tags` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_notes_updatedAt")
        db.execSQL("DROP INDEX IF EXISTS index_notes_isPinned")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notes` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `tags` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_17_19 = object : Migration(17, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_notes_updatedAt")
        db.execSQL("DROP INDEX IF EXISTS index_notes_isPinned")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notes` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `tags` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `plannedBlockMinutes` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `gate_requests` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT,
                `packageName` TEXT NOT NULL,
                `appLabel` TEXT NOT NULL,
                `statedReason` TEXT NOT NULL,
                `taskType` TEXT NOT NULL,
                `mismatchCount` INTEGER NOT NULL,
                `elapsedMillis` INTEGER NOT NULL,
                `grantSource` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create task_micro_steps table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_micro_steps` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `estimatedMinutes` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_micro_steps_task_id_sort` ON `task_micro_steps` (`taskId`, `sortOrder`)")

        // 2. Create focus_sessions table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `focus_sessions` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `durationSeconds` INTEGER NOT NULL,
                `interruptionCount` INTEGER NOT NULL,
                `energyAtStart` INTEGER NOT NULL,
                `wasCompleted` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_started_at` ON `focus_sessions` (`startedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_task_id` ON `focus_sessions` (`taskId`)")

        // 3. Create quick_notes table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quick_notes` (
                `id` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `convertedTaskId` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 4. Create protected_slots table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `protected_slots` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `startHour` INTEGER NOT NULL,
                `startMinute` INTEGER NOT NULL,
                `durationMinutes` INTEGER NOT NULL,
                `daysOfWeekMask` INTEGER NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 5. Create composite indices on existing tables
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status_scheduledDate` ON `tasks` (`status`, `scheduledDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status_deadlineDate` ON `tasks` (`status`, `deadlineDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentTaskId` ON `tasks` (`parentTaskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_energy_predictions_predictedAtMillis` ON `energy_predictions` (`predictedAtMillis`)")

        // 6. Migrate legacy time_sessions to focus_sessions
        db.execSQL(
            """
            INSERT OR IGNORE INTO `focus_sessions` (`id`, `taskId`, `startedAt`, `endedAt`, `durationSeconds`, `interruptionCount`, `energyAtStart`, `wasCompleted`)
            SELECT s.id, s.taskId, s.startedAt, s.endedAt, CAST(ROUND(s.durationMinutes * 60) AS INTEGER), (s.pauseResumeCount + s.appSwitchCount + s.interruptionBurstCount), 50, CASE WHEN s.endedAt IS NOT NULL THEN 1 ELSE 0 END
            FROM `time_sessions` s
            WHERE s.taskId IN (SELECT id FROM `tasks`)
            """.trimIndent()
        )

        // 7. Extract legacy micro-steps from tasks.ifThenPlan
        val cursor = db.query("SELECT id, ifThenPlan FROM `tasks` WHERE `ifThenPlan` IS NOT NULL AND `ifThenPlan` != ''")
        cursor.use { c ->
            while (c.moveToNext()) {
                val taskId = c.getString(0)
                val ifThenPlan = c.getString(1)
                val steps = StepByStepPlanCodec.parse(ifThenPlan)
                steps.forEachIndexed { index, step ->
                    val stepId = java.util.UUID.randomUUID().toString()
                    val isCompleted = if (step.completed) 1 else 0
                    val stmt = db.compileStatement(
                        "INSERT INTO `task_micro_steps` (`id`, `taskId`, `title`, `sortOrder`, `isCompleted`, `estimatedMinutes`) VALUES (?, ?, ?, ?, ?, 5)"
                    )
                    stmt.bindString(1, stepId)
                    stmt.bindString(2, taskId)
                    stmt.bindString(3, step.text)
                    stmt.bindLong(4, index.toLong())
                    stmt.bindLong(5, isCompleted.toLong())
                    stmt.executeInsert()
                }
            }
        }

        // 8. Prune energy_predictions older than 14 days
        val cutoffMillis = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        db.execSQL("DELETE FROM `energy_predictions` WHERE `predictedAtMillis` < $cutoffMillis")
    }
}

@Database(
    entities = [
        TaskEntity::class,
        TimeSessionEntity::class,
        GoalEntity::class,
        WoopEntity::class,
        UlyssesContractEntity::class,
        UnlockCodeEntity::class,
        HyperFocusSessionEntity::class,
        SleepLogEntity::class,
        EnergyPredictionEntity::class,
        NoteEntity::class,
        GateRequestEntity::class,
        TaskMicroStepEntity::class,
        FocusSessionEntity::class,
        QuickNoteEntity::class,
        ProtectedSlotEntity::class
    ],
    version = 22,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NeuroFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timeSessionDao(): TimeSessionDao
    abstract fun goalDao(): GoalDao
    abstract fun woopDao(): WoopDao
    abstract fun ulyssesContractDao(): UlyssesContractDao
    abstract fun unlockCodeDao(): UnlockCodeDao
    abstract fun hyperFocusSessionDao(): HyperFocusSessionDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun energyPredictionDao(): EnergyPredictionDao
    abstract fun noteDao(): NoteDao
    abstract fun gateRequestDao(): GateRequestDao
    abstract fun taskMicroStepDao(): TaskMicroStepDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun protectedSlotDao(): ProtectedSlotDao
}
