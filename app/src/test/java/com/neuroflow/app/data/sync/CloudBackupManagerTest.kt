package com.neuroflow.app.data.sync

import android.content.Context
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class CloudBackupManagerTest : StringSpec({

    val taskDao = mockk<TaskDao>(relaxed = true)
    val goalDao = mockk<GoalDao>(relaxed = true)
    val timeSessionDao = mockk<TimeSessionDao>(relaxed = true)
    val woopDao = mockk<WoopDao>(relaxed = true)
    val ulyssesContractDao = mockk<UlyssesContractDao>(relaxed = true)
    val sleepLogDao = mockk<SleepLogDao>(relaxed = true)
    val energyPredictionDao = mockk<EnergyPredictionDao>(relaxed = true)
    val hyperFocusSessionDao = mockk<HyperFocusSessionDao>(relaxed = true)
    val noteDao = mockk<NoteDao>(relaxed = true)
    val firestoreRepo = mockk<FirestoreBackupRepository>(relaxed = true)
    val signInManager = mockk<GoogleSignInManager>(relaxed = true)
    val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    val context = mockk<Context>(relaxed = true)

    val manager = CloudBackupManager(
        taskDao = taskDao,
        goalDao = goalDao,
        timeSessionDao = timeSessionDao,
        woopDao = woopDao,
        ulyssesContractDao = ulyssesContractDao,
        sleepLogDao = sleepLogDao,
        energyPredictionDao = energyPredictionDao,
        hyperFocusSessionDao = hyperFocusSessionDao,
        noteDao = noteDao,
        firestoreRepo = firestoreRepo,
        signInManager = signInManager,
        preferencesDataStore = preferencesDataStore,
        context = context
    )

    "backup returns Error when user is not signed in" {
        every { signInManager.getUid() } returns null

        val result = manager.backup()
        result.shouldBeInstanceOf<BackupStatus.Error>()
    }

    "restore returns Error when user is not signed in" {
        every { signInManager.getUid() } returns null

        val result = manager.restore()
        result.shouldBeInstanceOf<RestoreStatus.Error>()
    }

    "backup successfully uploads payload and returns Success" {
        every { signInManager.getUid() } returns "user-123"
        coEvery { taskDao.getAllTasks() } returns listOf(TaskEntity(id = "task-1", title = "Test Task"))
        every { goalDao.observeAll() } returns flowOf(emptyList())
        coEvery { timeSessionDao.getAll() } returns emptyList()
        every { ulyssesContractDao.observeActive() } returns flowOf(emptyList())
        every { ulyssesContractDao.observeArchived() } returns flowOf(emptyList())
        coEvery { sleepLogDao.getAll() } returns emptyList()
        coEvery { energyPredictionDao.getRecent(any()) } returns emptyList()
        every { preferencesDataStore.preferencesFlow } returns flowOf(UserPreferences(identityLabel = "Scholar"))
        coEvery { firestoreRepo.uploadBackup("user-123", any()) } returns Result.success("backup-abc")
        coEvery { firestoreRepo.pruneOldBackups("user-123", any()) } returns Unit

        val result = manager.backup()
        result.shouldBeInstanceOf<BackupStatus.Success>()
        (result as BackupStatus.Success).backupId shouldBe "backup-abc"
        coVerify(exactly = 1) { firestoreRepo.uploadBackup("user-123", any()) }
    }

    "restore successfully saves payload to DAOs and returns Success" {
        every { signInManager.getUid() } returns "user-123"
        val payload = BackupPayload(
            tasks = listOf(TaskEntity(id = "task-restored", title = "Restored Task")),
            goals = emptyList(),
            timeSessions = emptyList(),
            woopData = emptyList(),
            ulyssesContracts = emptyList(),
            sleepLogs = emptyList(),
            energyPredictions = emptyList(),
            hyperFocusSessions = emptyList(),
            preferences = mapOf("identityLabel" to "Restored Label")
        )
        coEvery { firestoreRepo.downloadLatestBackup("user-123") } returns Result.success(payload)
        coEvery { taskDao.insert(any()) } returns Unit

        val result = manager.restore()
        result.shouldBeInstanceOf<RestoreStatus.Success>()
        (result as RestoreStatus.Success).itemCount shouldBe 1
        coVerify(exactly = 1) { taskDao.insert(match { it.id == "task-restored" }) }
    }

    "the deep work target and flagged apps survive a backup round trip" {
        val prefs = UserPreferences(
            dailyDeepWorkTargetMinutes = 180,
            distractingPackages = setOf("com.instagram.android", "com.discord")
        )

        val restored = UserPreferences().fromBackupMap(prefs.toBackupMap())

        restored.dailyDeepWorkTargetMinutes shouldBe 180
        restored.distractingPackages shouldBe prefs.distractingPackages
    }

    "a backup saved before these settings existed keeps the live values" {
        val live = UserPreferences(
            dailyDeepWorkTargetMinutes = 300,
            distractingPackages = setOf("com.discord")
        )

        val restored = live.fromBackupMap(mapOf("identityLabel" to "from an old backup"))

        restored.dailyDeepWorkTargetMinutes shouldBe 300
        restored.distractingPackages shouldBe setOf("com.discord")
    }

    "unflagging every app restores as none, not as the defaults" {
        val cleared = UserPreferences(distractingPackages = emptySet()).toBackupMap()

        cleared["distractingPackages"] shouldBe ""
        UserPreferences().fromBackupMap(cleared).distractingPackages shouldBe emptySet()
    }
})
