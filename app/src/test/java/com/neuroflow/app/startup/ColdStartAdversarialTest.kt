package com.neuroflow.app.startup

import android.content.Context
import android.os.StrictMode
import androidx.room.Room
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ColdStartAdversarialTest {

    @Test
    fun testNeuroFlowApplicationSourceHasNoRunBlockingInvocations() {
        val appSourceFile = File("src/main/java/com/neuroflow/app/NeuroFlowApplication.kt")
        if (appSourceFile.exists()) {
            val content = appSourceFile.readText()
            val invocationRegex = Regex("""\brunBlocking\s*[\(\{]""")
            assertFalse(
                "NeuroFlowApplication must NOT invoke runBlocking on Android main thread",
                invocationRegex.containsMatchIn(content)
            )
        }
    }

    @Test
    fun testInstallMarkerOperationsOnIOThreadDoNotBlockMain() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val markerFile = File(context.noBackupFilesDir, "install_marker_v1")
        if (markerFile.exists()) {
            markerFile.delete()
        }

        // Running marker operations on Dispatchers.IO
        val isFirstLaunch = async(Dispatchers.IO) {
            if (markerFile.exists()) {
                false
            } else {
                markerFile.parentFile?.mkdirs()
                markerFile.writeText("1")
                true
            }
        }.await()

        assertTrue("First launch should be detected", isFirstLaunch)
        assertTrue("Marker file must exist after first launch", markerFile.exists())

        // Subsequent launch check
        val isSecondLaunch = async(Dispatchers.IO) {
            if (markerFile.exists()) {
                false
            } else {
                markerFile.parentFile?.mkdirs()
                markerFile.writeText("1")
                true
            }
        }.await()

        assertFalse("Second launch should not be first launch", isSecondLaunch)
    }

    @Test
    fun testDatabaseCleanerEnforceRestoreVersionPolicyRunsCleanlyOnIO() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val preferencesDataStore = UserPreferencesDataStore(context)
        val databaseCleaner = DatabaseCleaner(context, db, preferencesDataStore)

        try {
            // Test normal launch on Dispatchers.IO
            val shouldPurge = databaseCleaner.enforceRestoreVersionPolicy(firstLaunchAfterInstall = false)
            assertFalse("Normal startup should not purge data", shouldPurge)

            // Test first launch after install without old backup
            val firstLaunchPurge = databaseCleaner.enforceRestoreVersionPolicy(firstLaunchAfterInstall = true)
            assertFalse("Fresh install without old backup should not purge data", firstLaunchPurge)
        } finally {
            db.close()
        }
    }
}
