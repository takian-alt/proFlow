package com.neuroflow.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {

    private fun createInMemoryDb(initialVersion: Int, setupSql: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // In-memory database
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
    fun testMigration11To12CreatesSleepLogsTableAndIndices() {
        val db = createInMemoryDb(11) { database ->
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
        }
        try {
            // Execute migration 11 -> 12
            MIGRATION_11_12.migrate(db)

            // Verify sleep_logs table exists and accepts insertion
            db.execSQL("""
                INSERT INTO sleep_logs (id, startAt, endAt, durationMinutes, source, notes, createdAt)
                VALUES ('log_1', 10000, 20000, 166, 'MANUAL', 'Good sleep', 20000)
            """.trimIndent())

            val cursor = db.query("SELECT id, durationMinutes, source FROM sleep_logs WHERE id = 'log_1'")
            assertTrue(cursor.moveToFirst())
            assertEquals("log_1", cursor.getString(0))
            assertEquals(166, cursor.getInt(1))
            assertEquals("MANUAL", cursor.getString(2))
            cursor.close()

            // Verify indices
            val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'sleep_logs'")
            val indexNames = mutableListOf<String>()
            while (indexCursor.moveToNext()) {
                indexNames.add(indexCursor.getString(0))
            }
            indexCursor.close()

            assertTrue(indexNames.contains("index_sleep_logs_startAt"))
            assertTrue(indexNames.contains("index_sleep_logs_endAt"))
        } finally {
            db.close()
        }
    }

    @Test
    fun testMigration18To19PreservesExistingUserNotes() {
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
        }
        try {
            // Insert pre-existing user note in version 18
            db.execSQL("""
                INSERT INTO notes (id, title, content, colorHex, isPinned, createdAt, updatedAt, tags)
                VALUES ('note_1', 'Critical User Note', 'Do not delete this!', '#112233', 1, 12345, 12345, 'work')
            """.trimIndent())

            // Run non-destructive migration 18 -> 19
            MIGRATION_18_19.migrate(db)

            // Verify the user note was NOT destroyed
            val cursor = db.query("SELECT title, content, colorHex, isPinned FROM notes WHERE id = 'note_1'")
            assertTrue("User note must be preserved across migration 18->19", cursor.moveToFirst())
            assertEquals("Critical User Note", cursor.getString(0))
            assertEquals("Do not delete this!", cursor.getString(1))
            assertEquals("#112233", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun testMigration17To19PreservesExistingUserNotes() {
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
            db.execSQL("""
                INSERT INTO notes (id, title, content, colorHex, isPinned, createdAt, updatedAt, tags)
                VALUES ('note_v17', 'V17 User Note', 'Preserved across jump', '#445566', 0, 5000, 5000, 'personal')
            """.trimIndent())

            // Run non-destructive migration 17 -> 19
            MIGRATION_17_19.migrate(db)

            val cursor = db.query("SELECT title, content FROM notes WHERE id = 'note_v17'")
            assertTrue("User note must be preserved across migration 17->19", cursor.moveToFirst())
            assertEquals("V17 User Note", cursor.getString(0))
            assertEquals("Preserved across jump", cursor.getString(1))
            cursor.close()
        } finally {
            db.close()
        }
    }
}
