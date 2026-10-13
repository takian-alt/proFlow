package com.neuroflow.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationV20ToV21Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NeuroFlowDatabase::class.java,
        emptyList()
    )

    @Test
    fun migration20to21CreatesUsableGateRequests() {
        helper.createDatabase(DATABASE_NAME, 20).close()

        // runMigrationsAndValidate already fails if the migrated table differs from the exported
        // 21.json; the insert below is the behaviour on top of that structural check.
        helper.runMigrationsAndValidate(DATABASE_NAME, 21, true, MIGRATION_20_21).use { db ->
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='gate_requests'")
            cursor.use { assertTrue(it.moveToFirst()) }

            db.execSQL(
                "INSERT INTO `gate_requests` (`id`, `sessionId`, `packageName`, `appLabel`, " +
                    "`statedReason`, `taskType`, `mismatchCount`, `elapsedMillis`, `grantSource`, " +
                    "`createdAtMillis`) VALUES ('req-1', NULL, 'com.instagram.android', 'Instagram', " +
                    "'need a reply', 'TRANSCRIBE', 2, 45000, 'ABANDONED', 1760000000000)"
            )
            val count = db.query("SELECT COUNT(*) FROM `gate_requests`")
            count.use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "neuroflow_database"
    }
}
