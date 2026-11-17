package com.neuroflow.app.data.local

import android.content.Context
import androidx.room.Room
import com.neuroflow.app.data.local.entity.FocusSessionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.sync.BackupPayload
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.FirestoreBackupRepository
import com.neuroflow.app.data.sync.GoogleSignInManager
import com.neuroflow.app.data.sync.RestoreStatus
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesAndBackupRoundTripTest {

    private lateinit var context: Context
    private lateinit var db: NeuroFlowDatabase
    private lateinit var prefsDataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsDataStore = UserPreferencesDataStore(context, db.protectedSlotDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `UserPreferencesDataStore persists and reads wakeUpHour wakeTimeHour and wakeTimeMinute cleanly`() = runBlocking {
        prefsDataStore.updatePreferences {
            it.copy(wakeTimeHour = 6, wakeTimeMinute = 45, workDayEnd = 21)
        }
        val afterTimeEdit = prefsDataStore.preferencesFlow.first()
        assertEquals(6, afterTimeEdit.wakeUpHour)
        assertEquals(6, afterTimeEdit.wakeTimeHour)
        assertEquals(45, afterTimeEdit.wakeTimeMinute)
        assertEquals(21, afterTimeEdit.workDayEnd)

        prefsDataStore.updatePreferences {
            it.copy(wakeUpHour = 9, wakeTimeMinute = 15)
        }
        val afterLegacyHourEdit = prefsDataStore.preferencesFlow.first()
        assertEquals(9, afterLegacyHourEdit.wakeUpHour)
        assertEquals(9, afterLegacyHourEdit.wakeTimeHour)
        assertEquals(15, afterLegacyHourEdit.wakeTimeMinute)
    }

    @Test
    fun `CloudBackupManager restore with database transaction atomically restores FocusSessionEntity and preferences`() = runBlocking {
        val firestoreRepo = mockk<FirestoreBackupRepository>(relaxed = true)
        val signInManager = mockk<GoogleSignInManager>(relaxed = true)
        every { signInManager.getUid() } returns "uid-roundtrip-1"

        val backedUpTask = TaskEntity(
            id = "task-backup-1",
            title = "Backed Up Task",
            quadrant = Quadrant.DO_FIRST,
            energyLevel = EnergyLevel.HIGH
        )
        val backedUpFocusSession = FocusSessionEntity(
            id = "fs-backup-1",
            taskId = "task-backup-1",
            startedAt = 1_760_000_000_000L,
            endedAt = 1_760_001_500_000L,
            durationSeconds = 1500,
            interruptionCount = 2,
            energyAtStart = 78,
            wasCompleted = true
        )

        val payload = BackupPayload(
            tasks = listOf(backedUpTask),
            goals = emptyList(),
            timeSessions = emptyList(),
            woopData = emptyList(),
            ulyssesContracts = emptyList(),
            sleepLogs = emptyList(),
            energyPredictions = emptyList(),
            hyperFocusSessions = emptyList(),
            preferences = mapOf(
                "wakeUpHour" to "5",
                "wakeTimeMinute" to "35",
                "workDayEnd" to "19"
            ),
            focusSessions = listOf(backedUpFocusSession)
        )
        coEvery { firestoreRepo.downloadLatestBackup("uid-roundtrip-1") } returns Result.success(payload)

        val manager = CloudBackupManager(
            taskDao = db.taskDao(),
            goalDao = db.goalDao(),
            timeSessionDao = db.timeSessionDao(),
            woopDao = db.woopDao(),
            ulyssesContractDao = db.ulyssesContractDao(),
            sleepLogDao = db.sleepLogDao(),
            energyPredictionDao = db.energyPredictionDao(),
            hyperFocusSessionDao = db.hyperFocusSessionDao(),
            noteDao = db.noteDao(),
            firestoreRepo = firestoreRepo,
            signInManager = signInManager,
            preferencesDataStore = prefsDataStore,
            context = context,
            protectedSlotDao = db.protectedSlotDao(),
            taskMicroStepDao = db.taskMicroStepDao(),
            quickNoteDao = db.quickNoteDao(),
            focusSessionDao = db.focusSessionDao(),
            mindmapDao = db.mindmapDao(),
            database = db
        )

        val status = manager.restore()
        assertTrue(status is RestoreStatus.Success)

        val restoredTasks = db.taskDao().getAllTasks()
        assertEquals(1, restoredTasks.size)
        assertEquals("task-backup-1", restoredTasks[0].id)

        val restoredFocusSessions = db.focusSessionDao().getAll()
        assertEquals(1, restoredFocusSessions.size)
        assertEquals("fs-backup-1", restoredFocusSessions[0].id)
        assertEquals(1500, restoredFocusSessions[0].durationSeconds)
        assertEquals(78, restoredFocusSessions[0].energyAtStart)

        val restoredPrefs = prefsDataStore.preferencesFlow.first()
        assertEquals(5, restoredPrefs.wakeUpHour)
        assertEquals(5, restoredPrefs.wakeTimeHour)
        assertEquals(35, restoredPrefs.wakeTimeMinute)
        assertEquals(19, restoredPrefs.workDayEnd)
    }
}
