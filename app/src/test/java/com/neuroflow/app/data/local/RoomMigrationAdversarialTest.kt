package com.neuroflow.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomMigrationAdversarialTest {

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

    @Test
    fun testMigration11To12PreservesPreExistingDataAndCreatesSleepLogsWithIndices() {
        val db = createInMemoryDb(11) { database ->
            // Schema up to v11
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    quadrant TEXT NOT NULL DEFAULT 'SCHEDULE',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    status TEXT NOT NULL DEFAULT 'ACTIVE'
                )
            """.trimIndent())
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS time_sessions (
                    id TEXT NOT NULL PRIMARY KEY,
                    taskId TEXT NOT NULL,
                    startedAt INTEGER NOT NULL,
                    durationMinutes INTEGER NOT NULL DEFAULT 25
                )
            """.trimIndent())
        }

        try {
            // Seed 20 tasks and 10 time sessions before migration
            for (i in 1..20) {
                db.execSQL(
                    "INSERT INTO tasks (id, title, description, quadrant, priority, status) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf("task_$i", "Title $i", "Description with 'quotes' & symbols #$i", "DO_FIRST", "HIGH", "ACTIVE")
                )
            }
            for (i in 1..10) {
                db.execSQL(
                    "INSERT INTO time_sessions (id, taskId, startedAt, durationMinutes) VALUES (?, ?, ?, ?)",
                    arrayOf("sess_$i", "task_$i", 1700000000L + i * 3600, 25 + i)
                )
            }

            // Execute migration 11 -> 12
            MIGRATION_11_12.migrate(db)

            // Verify pre-existing tasks are 100% intact
            val taskCursor = db.query("SELECT COUNT(*) FROM tasks")
            assertTrue(taskCursor.moveToFirst())
            assertEquals(20, taskCursor.getInt(0))
            taskCursor.close()

            // Verify pre-existing time_sessions are 100% intact
            val sessCursor = db.query("SELECT COUNT(*) FROM time_sessions")
            assertTrue(sessCursor.moveToFirst())
            assertEquals(10, sessCursor.getInt(0))
            sessCursor.close()

            // Verify sleep_logs table structure and column metadata
            val pragmaCursor = db.query("PRAGMA table_info(sleep_logs)")
            val columns = mutableMapOf<String, String>()
            while (pragmaCursor.moveToNext()) {
                val name = pragmaCursor.getString(1)
                val type = pragmaCursor.getString(2)
                columns[name] = type
            }
            pragmaCursor.close()

            assertEquals("TEXT", columns["id"])
            assertEquals("INTEGER", columns["startAt"])
            assertEquals("INTEGER", columns["endAt"])
            assertEquals("INTEGER", columns["durationMinutes"])
            assertEquals("TEXT", columns["source"])
            assertEquals("TEXT", columns["notes"])
            assertEquals("INTEGER", columns["createdAt"])

            // Verify both indices exist
            val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'sleep_logs'")
            val indexNames = mutableSetOf<String>()
            while (indexCursor.moveToNext()) {
                indexNames.add(indexCursor.getString(0))
            }
            indexCursor.close()

            assertTrue("Missing index_sleep_logs_startAt", indexNames.contains("index_sleep_logs_startAt"))
            assertTrue("Missing index_sleep_logs_endAt", indexNames.contains("index_sleep_logs_endAt"))

            // Stress-test inserting edge case sleep log rows
            db.execSQL("""
                INSERT INTO sleep_logs (id, startAt, endAt, durationMinutes, source, notes, createdAt)
                VALUES ('log_empty_notes', 1000, 2000, 16, 'AUTO', '', 2000)
            """.trimIndent())

            db.execSQL("""
                INSERT INTO sleep_logs (id, startAt, endAt, durationMinutes, source, notes, createdAt)
                VALUES ('log_unicode', 3000, 5000, 33, 'FITBIT', '😴 Deep rem sleep with emoji & symbols: <xml></xml>', 5000)
            """.trimIndent())

            val sleepCursor = db.query("SELECT id, notes FROM sleep_logs WHERE id = 'log_unicode'")
            assertTrue(sleepCursor.moveToFirst())
            assertEquals("log_unicode", sleepCursor.getString(0))
            assertTrue(sleepCursor.getString(1).contains("Deep rem sleep"))
            sleepCursor.close()

            // Test migration idempotency (running MIGRATION_11_12 again should not crash)
            MIGRATION_11_12.migrate(db)

            val countAfterReapply = db.query("SELECT COUNT(*) FROM sleep_logs")
            assertTrue(countAfterReapply.moveToFirst())
            assertEquals(2, countAfterReapply.getInt(0))
            countAfterReapply.close()

        } finally {
            db.close()
        }
    }

    @Test
    fun testMigration18To19PreservesFiftyDiverseUserNotes() {
        val db = createInMemoryDb(18) { database ->
            database.execSQL("""
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
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_updatedAt ON notes(updatedAt)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isPinned ON notes(isPinned)")
        }

        try {
            // Seed 50 realistic and stress-testing notes
            for (i in 1..50) {
                val title = "Note Title #$i - Important"
                val content = if (i % 2 == 0) {
                    "Markdown content:\n# Heading\n- Item 1\n- Item 2\n\n```json\n{\"id\": $i}\n```"
                } else {
                    "Single line note with emojis 🚀🔥🧠 and quotes 'single' and \"double\" #$i"
                }
                val isPinned = if (i % 3 == 0) 1 else 0
                val tags = if (i % 4 == 0) "" else "work,urgent,project$i"

                db.execSQL(
                    "INSERT INTO notes (id, title, content, colorHex, isPinned, createdAt, updatedAt, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf("note_$i", title, content, "#FF5722", isPinned, 1600000000L + i, 1600000000L + i * 2, tags)
                )
            }

            // Run migration 18 -> 19
            MIGRATION_18_19.migrate(db)

            // Verify count remains exactly 50
            val countCursor = db.query("SELECT COUNT(*) FROM notes")
            assertTrue(countCursor.moveToFirst())
            assertEquals(50, countCursor.getInt(0))
            countCursor.close()

            // Verify data integrity for each note
            for (i in 1..50) {
                val cursor = db.query(
                    "SELECT title, content, colorHex, isPinned, tags FROM notes WHERE id = ?",
                    arrayOf("note_$i")
                )
                assertTrue("Note note_$i was dropped or lost!", cursor.moveToFirst())
                assertEquals("Note Title #$i - Important", cursor.getString(0))
                assertTrue(cursor.getString(1).contains(i.toString()))
                assertEquals("#FF5722", cursor.getString(2))
                assertEquals(if (i % 3 == 0) 1 else 0, cursor.getInt(3))
                cursor.close()
            }

            // Verify dropped legacy indices
            val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'notes'")
            val indexNames = mutableSetOf<String>()
            while (indexCursor.moveToNext()) {
                indexNames.add(indexCursor.getString(0))
            }
            indexCursor.close()

            assertTrue("index_notes_updatedAt should have been dropped", !indexNames.contains("index_notes_updatedAt"))
            assertTrue("index_notes_isPinned should have been dropped", !indexNames.contains("index_notes_isPinned"))

            // Verify new note insertion works smoothly after migration
            db.execSQL(
                "INSERT INTO notes (id, title, content, colorHex, isPinned, createdAt, updatedAt, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("note_post_migration", "Post-Migration Note", "Successfully created after v19 migration", "#4CAF50", 1, 1700000000L, 1700000000L, "test")
            )

            val newCursor = db.query("SELECT COUNT(*) FROM notes")
            assertTrue(newCursor.moveToFirst())
            assertEquals(51, newCursor.getInt(0))
            newCursor.close()

        } finally {
            db.close()
        }
    }

    @Test
    fun testMigration17To19PreservesExistingUserNotesAcrossVersionJump() {
        val db = createInMemoryDb(17) { database ->
            database.execSQL("""
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

        try {
            for (i in 1..25) {
                db.execSQL(
                    "INSERT INTO notes (id, title, content, colorHex, isPinned, createdAt, updatedAt, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf("v17_note_$i", "V17 Title $i", "V17 Content $i", "#00BCD4", 0, 1000L * i, 1000L * i, "v17")
                )
            }

            // Run migration 17 -> 19 (jumping over 18)
            MIGRATION_17_19.migrate(db)

            val countCursor = db.query("SELECT COUNT(*) FROM notes")
            assertTrue(countCursor.moveToFirst())
            assertEquals(25, countCursor.getInt(0))
            countCursor.close()

            val sampleCursor = db.query("SELECT title, content FROM notes WHERE id = 'v17_note_15'")
            assertTrue(sampleCursor.moveToFirst())
            assertEquals("V17 Title 15", sampleCursor.getString(0))
            assertEquals("V17 Content 15", sampleCursor.getString(1))
            sampleCursor.close()

        } finally {
            db.close()
        }
    }
}
