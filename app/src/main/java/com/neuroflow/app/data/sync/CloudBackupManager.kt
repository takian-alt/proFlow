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
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full cloud backup and restore cycle for proFlow.
 *
 * Backup: reads all Room DAOs + DataStore, builds a [BackupPayload], and uploads
 * it to Firestore via [FirestoreBackupRepository].
 *
 * Restore: downloads the latest (or a specific) [BackupPayload] from Firestore,
 * upserts every entity into the appropriate DAO, and replays preferences into
 * [UserPreferencesDataStore].
 */
@Singleton
class CloudBackupManager @Inject constructor(
    private val taskDao: TaskDao,
    private val goalDao: GoalDao,
    private val timeSessionDao: TimeSessionDao,
    private val woopDao: WoopDao,
    private val ulyssesContractDao: UlyssesContractDao,
    private val sleepLogDao: SleepLogDao,
    private val energyPredictionDao: EnergyPredictionDao,
    private val hyperFocusSessionDao: HyperFocusSessionDao,
    private val noteDao: NoteDao,
    private val firestoreRepo: FirestoreBackupRepository,
    private val signInManager: GoogleSignInManager,
    private val preferencesDataStore: UserPreferencesDataStore,
    @ApplicationContext private val context: Context
) {

    // -------------------------------------------------------------------------
    // Backup
    // -------------------------------------------------------------------------

    /**
     * Collects all local data and uploads it to Firestore.
     */
    suspend fun backup(): BackupStatus {
        val uid = signInManager.getUid()
            ?: return BackupStatus.Error("User is not signed in — backup aborted.")

        return try {
            withTimeout(25_000L) {
                val tasks = taskDao.getAllTasks()
                val goals = goalDao.observeAll().first()
                val timeSessions = timeSessionDao.getAll()
                val woops = tasks.mapNotNull { woopDao.getByTaskId(it.id) }
                val ulyssesContracts = buildList {
                    addAll(ulyssesContractDao.observeActive().first())
                    addAll(ulyssesContractDao.observeArchived().first())
                }
                val sleepLogs = sleepLogDao.getAll()
                val energyPredictions = energyPredictionDao.getRecent(limit = 500)
                val notes = noteDao.getAllNotes()

                val prefs = preferencesDataStore.preferencesFlow.first()
                val prefsMap = prefs.toBackupMap()

                val payload = BackupPayload(
                    tasks = tasks,
                    goals = goals,
                    timeSessions = timeSessions,
                    woopData = woops,
                    ulyssesContracts = ulyssesContracts,
                    sleepLogs = sleepLogs,
                    energyPredictions = energyPredictions,
                    hyperFocusSessions = emptyList(),
                    preferences = prefsMap,
                    notes = notes
                )

                firestoreRepo.uploadBackup(uid, payload).fold(
                    onSuccess = { backupId ->
                        firestoreRepo.pruneOldBackups(uid, keepCount = 3)
                        BackupStatus.Success(backupId = backupId)
                    },
                    onFailure = { e -> BackupStatus.Error(e.message ?: "Upload failed") }
                )
            }
        } catch (e: TimeoutCancellationException) {
            BackupStatus.Error("Backup timed out. Please verify that Cloud Firestore is created and enabled in Firebase Console.")
        } catch (e: Exception) {
            BackupStatus.Error(e.message ?: "Unknown error during backup")
        }
    }

    // -------------------------------------------------------------------------
    // Restore (latest)
    // -------------------------------------------------------------------------

    suspend fun restore(): RestoreStatus {
        val uid = signInManager.getUid()
            ?: return RestoreStatus.Error("User is not signed in — restore aborted.")

        return try {
            withTimeout(25_000L) {
                firestoreRepo.downloadLatestBackup(uid).fold(
                    onSuccess = { payload ->
                        if (payload == null) RestoreStatus.Error("No backup found for this account.")
                        else applyPayload(payload)
                    },
                    onFailure = { e -> RestoreStatus.Error(e.message ?: "Download failed") }
                )
            }
        } catch (e: TimeoutCancellationException) {
            RestoreStatus.Error("Restore timed out. Please verify that Cloud Firestore is created and enabled in Firebase Console.")
        } catch (e: Exception) {
            RestoreStatus.Error(e.message ?: "Unknown error during restore")
        }
    }

    // -------------------------------------------------------------------------
    // List backups
    // -------------------------------------------------------------------------

    suspend fun listBackups(): List<BackupMeta> {
        val uid = signInManager.getUid() ?: return emptyList()
        return firestoreRepo.listBackups(uid).getOrElse { emptyList() }
    }

    // -------------------------------------------------------------------------
    // Restore from specific backup
    // -------------------------------------------------------------------------

    suspend fun restoreFromBackup(backupId: String): RestoreStatus {
        val uid = signInManager.getUid()
            ?: return RestoreStatus.Error("User is not signed in — restore aborted.")

        return try {
            withTimeout(25_000L) {
                firestoreRepo.downloadBackup(uid, backupId).fold(
                    onSuccess = { payload ->
                        if (payload == null) RestoreStatus.Error("Backup '$backupId' not found.")
                        else applyPayload(payload)
                    },
                    onFailure = { e -> RestoreStatus.Error(e.message ?: "Download failed") }
                )
            }
        } catch (e: TimeoutCancellationException) {
            RestoreStatus.Error("Restore timed out. Please verify that Cloud Firestore is created and enabled in Firebase Console.")
        } catch (e: Exception) {
            RestoreStatus.Error(e.message ?: "Unknown error during restore")
        }
    }

    // -------------------------------------------------------------------------
    // Delete backup
    // -------------------------------------------------------------------------

    suspend fun deleteBackup(backupId: String): Result<Unit> {
        val uid = signInManager.getUid()
            ?: return Result.failure(Exception("User is not signed in"))
        return firestoreRepo.deleteBackup(uid, backupId)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------


    private suspend fun applyPayload(payload: BackupPayload): RestoreStatus {
        var itemCount = 0

        payload.tasks.forEach { taskDao.insert(it); itemCount++ }
        payload.goals.forEach { goalDao.insert(it); itemCount++ }
        payload.timeSessions.forEach { timeSessionDao.insert(it); itemCount++ }
        payload.woopData.forEach { woopDao.upsert(it); itemCount++ }
        payload.ulyssesContracts.forEach { ulyssesContractDao.insert(it); itemCount++ }
        payload.sleepLogs.forEach { sleepLogDao.upsert(it); itemCount++ }
        payload.energyPredictions.forEach { pred ->
            runCatching { energyPredictionDao.insert(pred); itemCount++ }
        }
        payload.hyperFocusSessions.forEach { session ->
            runCatching { hyperFocusSessionDao.insert(session); itemCount++ }
        }
        payload.notes.forEach { note ->
            runCatching { noteDao.insert(note); itemCount++ }
        }

        if (payload.preferences.isNotEmpty()) {
            preferencesDataStore.updatePreferences { current ->
                current.fromBackupMap(payload.preferences)
            }
        }

        return RestoreStatus.Success(itemCount = itemCount)
    }
}

// =============================================================================
// Extension: UserPreferences → Map<String, String>
// =============================================================================

/**
 * Serialises the core user preference fields into a flat [Map<String, String>]
 * suitable for inclusion in a [BackupPayload].
 *
 * Only the fields enumerated in the backup spec are included; complex/derived
 * fields (peak detection state, morning-tune weights, etc.) are excluded to keep
 * the payload focused on user-configurable values.
 */
internal fun UserPreferences.toBackupMap(): Map<String, String> = buildMap {
    put("wakeUpHour", wakeUpHour.toString())
    put("sleepHour", sleepHour.toString())
    put("peakEnergyStart", peakEnergyStart.toString())
    put("peakEnergyEnd", peakEnergyEnd.toString())
    put("workDayStart", workDayStart.toString())
    put("workDayEnd", workDayEnd.toString())
    put("defaultPomodoroMinutes", defaultPomodoroMinutes.toString())
    put("defaultBreakMinutes", defaultBreakMinutes.toString())
    put("dailyDeepWorkTargetMinutes", dailyDeepWorkTargetMinutes.toString())
    // Package names never contain commas, so a flat map can carry the set as-is.
    put("distractingPackages", distractingPackages.joinToString(","))
    put("identityLabel", identityLabel)
    put("topGoal", topGoal)
    put("dailyStreak", dailyStreak.toString())
    put("lastActiveDate", lastActiveDate.toString())
    put("longestStreak", longestStreak.toString())
    put("totalTasksCompleted", totalTasksCompleted.toString())
    put("totalFocusMinutes", totalFocusMinutes.toString())
    put("weightQuadrant", weightQuadrant.toString())
    put("weightDeadlineUrgency", weightDeadlineUrgency.toString())
    put("weightPriorityLevel", weightPriorityLevel.toString())
    put("weightDuration", weightDuration.toString())
    put("weightImpact", weightImpact.toString())
    put("weightFocusMode", weightFocusMode.toString())
    put("onboardingCompleted", onboardingCompleted.toString())
    put("theme", theme.name)
    put("weeklyIntent", weeklyIntent)
    put("weeklyIntentIsoWeek", weeklyIntentIsoWeek.toString())
    put("weeklyIntentIsoYear", weeklyIntentIsoYear.toString())
    put("lastFreshStartShownWeek", lastFreshStartShownWeek.toString())
    put("lastFreshStartShownYear", lastFreshStartShownYear.toString())
    put("lastAppOpenMillis", lastAppOpenMillis.toString())
}

// =============================================================================
// Extension: Map<String, String> → UserPreferences (via copy)
// =============================================================================

/**
 * Deserialises a backup map back into a [UserPreferences] by applying each
 * recognised key on top of [this] (the current live preferences).  Unknown or
 * malformed keys are silently skipped so that partial/old backups still work.
 */
internal fun UserPreferences.fromBackupMap(map: Map<String, String>): UserPreferences {
    fun str(key: String): String? = map[key]
    fun int(key: String, fallback: Int): Int = map[key]?.toIntOrNull() ?: fallback
    fun long(key: String, fallback: Long): Long = map[key]?.toLongOrNull() ?: fallback
    fun float(key: String, fallback: Float): Float = map[key]?.toFloatOrNull() ?: fallback
    fun bool(key: String, fallback: Boolean): Boolean =
        map[key]?.toBooleanStrictOrNull() ?: fallback
    // An empty saved list arrives as "" and must restore as empty, not as the default set.
    fun stringSet(key: String, fallback: Set<String>): Set<String> =
        map[key]?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: fallback

    return copy(
        wakeUpHour              = int("wakeUpHour", wakeUpHour),
        sleepHour               = int("sleepHour", sleepHour),
        peakEnergyStart         = int("peakEnergyStart", peakEnergyStart),
        peakEnergyEnd           = int("peakEnergyEnd", peakEnergyEnd),
        workDayStart            = int("workDayStart", workDayStart),
        workDayEnd              = int("workDayEnd", workDayEnd),
        defaultPomodoroMinutes  = int("defaultPomodoroMinutes", defaultPomodoroMinutes),
        defaultBreakMinutes     = int("defaultBreakMinutes", defaultBreakMinutes),
        dailyDeepWorkTargetMinutes = int("dailyDeepWorkTargetMinutes", dailyDeepWorkTargetMinutes).coerceIn(30, 720),
        distractingPackages    = stringSet("distractingPackages", distractingPackages),
        identityLabel           = str("identityLabel") ?: identityLabel,
        topGoal                 = str("topGoal") ?: topGoal,
        dailyStreak             = int("dailyStreak", dailyStreak),
        lastActiveDate          = long("lastActiveDate", lastActiveDate),
        longestStreak           = int("longestStreak", longestStreak),
        totalTasksCompleted     = int("totalTasksCompleted", totalTasksCompleted),
        totalFocusMinutes       = int("totalFocusMinutes", totalFocusMinutes),
        weightQuadrant          = float("weightQuadrant", weightQuadrant),
        weightDeadlineUrgency   = float("weightDeadlineUrgency", weightDeadlineUrgency),
        weightPriorityLevel     = float("weightPriorityLevel", weightPriorityLevel),
        weightDuration          = float("weightDuration", weightDuration),
        weightImpact            = float("weightImpact", weightImpact),
        weightFocusMode         = float("weightFocusMode", weightFocusMode),
        onboardingCompleted     = bool("onboardingCompleted", onboardingCompleted),
        theme                   = str("theme")?.let { name ->
            runCatching { com.neuroflow.app.domain.model.AppTheme.valueOf(name) }.getOrNull()
        } ?: theme,
        weeklyIntent            = str("weeklyIntent") ?: weeklyIntent,
        weeklyIntentIsoWeek     = int("weeklyIntentIsoWeek", weeklyIntentIsoWeek),
        weeklyIntentIsoYear     = int("weeklyIntentIsoYear", weeklyIntentIsoYear),
        lastFreshStartShownWeek = int("lastFreshStartShownWeek", lastFreshStartShownWeek),
        lastFreshStartShownYear = int("lastFreshStartShownYear", lastFreshStartShownYear),
        lastAppOpenMillis       = long("lastAppOpenMillis", lastAppOpenMillis)
    )
}
