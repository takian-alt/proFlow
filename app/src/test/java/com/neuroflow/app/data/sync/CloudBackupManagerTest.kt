package com.neuroflow.app.data.sync

import android.content.Context
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.local.entity.QuickNoteEntity
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

    "CloudBackupManager with quickNoteDao backs up and restores quick notes properly (BUG-128)" {
        val qNoteDao = mockk<QuickNoteDao>(relaxed = true)
        val testManager = CloudBackupManager(
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
            context = context,
            quickNoteDao = qNoteDao
        )

        every { signInManager.getUid() } returns "user-123"
        coEvery { taskDao.getAllTasks() } returns emptyList()
        every { goalDao.observeAll() } returns flowOf(emptyList())
        coEvery { timeSessionDao.getAll() } returns emptyList()
        every { ulyssesContractDao.observeActive() } returns flowOf(emptyList())
        every { ulyssesContractDao.observeArchived() } returns flowOf(emptyList())
        coEvery { sleepLogDao.getAll() } returns emptyList()
        coEvery { energyPredictionDao.getRecent(any()) } returns emptyList()
        every { preferencesDataStore.preferencesFlow } returns flowOf(UserPreferences())
        coEvery { qNoteDao.getAll() } returns listOf(QuickNoteEntity(id = "qn-1", content = "Test Quick Note"))
        coEvery { firestoreRepo.uploadBackup("user-123", any()) } returns Result.success("backup-qn")
        coEvery { firestoreRepo.pruneOldBackups("user-123", any()) } returns Unit

        val backupRes = testManager.backup()
        backupRes.shouldBeInstanceOf<BackupStatus.Success>()
        coVerify(exactly = 1) {
            firestoreRepo.uploadBackup("user-123", match { payload ->
                payload.quickNotes.size == 1 && payload.quickNotes.first().id == "qn-1"
            })
        }

        val restorePayload = BackupPayload(
            tasks = emptyList(),
            goals = emptyList(),
            timeSessions = emptyList(),
            woopData = emptyList(),
            ulyssesContracts = emptyList(),
            sleepLogs = emptyList(),
            energyPredictions = emptyList(),
            hyperFocusSessions = emptyList(),
            preferences = emptyMap(),
            quickNotes = listOf(QuickNoteEntity(id = "qn-restored", content = "Restored Note"))
        )
        coEvery { firestoreRepo.downloadLatestBackup("user-123") } returns Result.success(restorePayload)
        val restoreRes = testManager.restore()
        restoreRes.shouldBeInstanceOf<RestoreStatus.Success>()
        coVerify(exactly = 1) { qNoteDao.insert(match { it.id == "qn-restored" }) }
    }

    "TaskEntity with plannedBlockMinutes is preserved across cloud backup and restore (BUG-142)" {
        every { signInManager.getUid() } returns "user-123"
        val originalTask = TaskEntity(
            id = "task-block-1",
            title = "Task with Auto-Scheduler Block",
            plannedBlockMinutes = 45
        )
        val payload = BackupPayload(
            tasks = listOf(originalTask),
            goals = emptyList(),
            timeSessions = emptyList(),
            woopData = emptyList(),
            ulyssesContracts = emptyList(),
            sleepLogs = emptyList(),
            energyPredictions = emptyList(),
            hyperFocusSessions = emptyList(),
            preferences = emptyMap()
        )
        coEvery { firestoreRepo.downloadLatestBackup("user-123") } returns Result.success(payload)
        coEvery { taskDao.insert(any()) } returns Unit

        val result = manager.restore()
        result.shouldBeInstanceOf<RestoreStatus.Success>()
        coVerify(exactly = 1) {
            taskDao.insert(match {
                it.id == "task-block-1" && it.plannedBlockMinutes == 45
            })
        }
    }

    "BUG-151: Notification and auto-scheduling preferences preserved across cloud backup and restore" {
        val originalPrefs = UserPreferences(
            dailyPlanNotificationsEnabled = false,
            streakNotificationsEnabled = false,
            autonomyNudgeNotificationsEnabled = false,
            deadlineReminderNotificationsEnabled = false,
            deadlineEscalationNotificationsEnabled = false,
            dailyPlanNotificationHour = 9,
            streakCheckNotificationHour = 22,
            autoSchedulingEnabled = false,
            isAutoScheduleToggleOn = true,
            autoSchedulingHorizonDays = 5,
            autoSchedulingBreakAfterCognitiveMinutes = 120,
            autoSchedulingBreakDurationMinutes = 20,
            woopEnabled = false,
            autoTrackerEnabled = true,
            firstAppOpenMillis = 1700000000000L
        )

        val backupMap = originalPrefs.toBackupMap()
        backupMap["dailyPlanNotificationsEnabled"] shouldBe "false"
        backupMap["streakNotificationsEnabled"] shouldBe "false"
        backupMap["autonomyNudgeNotificationsEnabled"] shouldBe "false"
        backupMap["deadlineReminderNotificationsEnabled"] shouldBe "false"
        backupMap["deadlineEscalationNotificationsEnabled"] shouldBe "false"
        backupMap["dailyPlanNotificationHour"] shouldBe "9"
        backupMap["streakCheckNotificationHour"] shouldBe "22"
        backupMap["autoSchedulingEnabled"] shouldBe "false"
        backupMap["isAutoScheduleToggleOn"] shouldBe "true"
        backupMap["autoSchedulingHorizonDays"] shouldBe "5"
        backupMap["autoSchedulingBreakAfterCognitiveMinutes"] shouldBe "120"
        backupMap["autoSchedulingBreakDurationMinutes"] shouldBe "20"
        backupMap["woopEnabled"] shouldBe "false"
        backupMap["autoTrackerEnabled"] shouldBe "true"
        backupMap["firstAppOpenMillis"] shouldBe "1700000000000"

        val defaultPrefs = UserPreferences()
        val restoredPrefs = defaultPrefs.fromBackupMap(backupMap)

        restoredPrefs.dailyPlanNotificationsEnabled shouldBe false
        restoredPrefs.streakNotificationsEnabled shouldBe false
        restoredPrefs.autonomyNudgeNotificationsEnabled shouldBe false
        restoredPrefs.deadlineReminderNotificationsEnabled shouldBe false
        restoredPrefs.deadlineEscalationNotificationsEnabled shouldBe false
        restoredPrefs.dailyPlanNotificationHour shouldBe 9
        restoredPrefs.streakCheckNotificationHour shouldBe 22
        restoredPrefs.autoSchedulingEnabled shouldBe false
        restoredPrefs.isAutoScheduleToggleOn shouldBe true
        restoredPrefs.autoSchedulingHorizonDays shouldBe 5
        restoredPrefs.autoSchedulingBreakAfterCognitiveMinutes shouldBe 120
        restoredPrefs.autoSchedulingBreakDurationMinutes shouldBe 20
        restoredPrefs.woopEnabled shouldBe false
        restoredPrefs.autoTrackerEnabled shouldBe true
        restoredPrefs.firstAppOpenMillis shouldBe 1700000000000L
    }

    "BUG-204: recentlyDeletedTasksJson avoidance telemetry preserved across cloud backup and restore" {
        val sampleDeletedJson = """[{"title":"Avoided Tax Filing","category":"WORK","postponeCount":4,"durationMinutes":60,"deletedAt":1729000000000}]"""
        val originalPrefs = UserPreferences(
            adaptiveSystemDirective = "Focus on small wins",
            weeklyAiAuditJson = """{"score":85}""",
            recentlyDeletedTasksJson = sampleDeletedJson,
            stimulantLogsJson = """[{"dose":20}]""",
            quickPhysiologicalLogsJson = """[{"type":"HR"}]"""
        )

        val backupMap = originalPrefs.toBackupMap()
        backupMap["recentlyDeletedTasksJson"] shouldBe sampleDeletedJson

        val defaultPrefs = UserPreferences()
        val restoredPrefs = defaultPrefs.fromBackupMap(backupMap)

        restoredPrefs.recentlyDeletedTasksJson shouldBe sampleDeletedJson
        restoredPrefs.adaptiveSystemDirective shouldBe "Focus on small wins"
    }
})
