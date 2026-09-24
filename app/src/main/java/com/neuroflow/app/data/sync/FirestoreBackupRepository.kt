package com.neuroflow.app.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusSessionEntity
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// BackupPayload — all local data packed into a single snapshot
// ---------------------------------------------------------------------------
data class BackupPayload(
    val tasks: List<TaskEntity>,
    val goals: List<GoalEntity>,
    val timeSessions: List<TimeSessionEntity>,
    val woopData: List<WoopEntity>,
    val ulyssesContracts: List<UlyssesContractEntity>,
    val sleepLogs: List<SleepLogEntity>,
    val energyPredictions: List<EnergyPredictionEntity>,
    val hyperFocusSessions: List<HyperFocusSessionEntity>,
    val preferences: Map<String, String>,  // DataStore key-value pairs
    val notes: List<NoteEntity> = emptyList()
)

// ---------------------------------------------------------------------------
// FirestoreBackupRepository
// ---------------------------------------------------------------------------
@Singleton
class FirestoreBackupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // -----------------------------------------------------------------------
    // Firestore path helpers
    // -----------------------------------------------------------------------

    /** users/{uid}/backups */
    private fun backupsRef(uid: String) =
        firestore.collection("users").document(uid).collection("backups")

    /** users/{uid}/backups/{backupId} */
    private fun backupDocRef(uid: String, backupId: String) =
        backupsRef(uid).document(backupId)

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Uploads a complete backup snapshot to Firestore.
     * Structure:
     *   users/{uid}/backups/{backupId}/meta        — single document
     *   users/{uid}/backups/{backupId}/tasks        — sub-collection
     *   users/{uid}/backups/{backupId}/goals        — sub-collection
     *   ... (all entity types)
     *   users/{uid}/backups/{backupId}/preferences — single document
     *
     * @return [Result] wrapping the new backupId on success.
     */
    suspend fun uploadBackup(uid: String, payload: BackupPayload): Result<String> {
        return try {
            val backupId = UUID.randomUUID().toString()
            val backupRef = backupDocRef(uid, backupId)

            // --- meta document -------------------------------------------
            val meta = mapOf(
                "backupId" to backupId,
                "createdAt" to System.currentTimeMillis(),
                "taskCount" to payload.tasks.size,
                "goalCount" to payload.goals.size,
                "timeSessionCount" to payload.timeSessions.size,
                "woopCount" to payload.woopData.size,
                "ulyssesCount" to payload.ulyssesContracts.size,
                "sleepLogCount" to payload.sleepLogs.size,
                "energyPredictionCount" to payload.energyPredictions.size,
                "hyperFocusCount" to payload.hyperFocusSessions.size,
                "noteCount" to payload.notes.size
            )

            // 1. Write document directly at users/{uid}/backups/{backupId} so it is never a virtual document
            backupRef.set(meta).await()
            // Backward-compat: also write to meta/info sub-doc
            backupRef.collection("meta").document("info").set(meta).await()

            // 2. Index in user root document for instant retrieval
            val userDocRef = firestore.collection("users").document(uid)
            val currentDoc = userDocRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val existingList = (currentDoc.get("backups") as? List<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()
            existingList.removeAll { it["backupId"] == backupId }
            existingList.add(0, meta)
            val trimmedList = existingList.take(5)

            userDocRef.set(
                mapOf(
                    "latestBackupId" to backupId,
                    "updatedAt" to System.currentTimeMillis(),
                    "backups" to trimmedList
                ),
                SetOptions.merge()
            ).await()

            // --- entity sub-collections ----------------------------------
            uploadCollection(backupRef.collection("tasks"), payload.tasks.map { it.toMap() })
            uploadCollection(backupRef.collection("goals"), payload.goals.map { it.toMap() })
            uploadCollection(backupRef.collection("timeSessions"), payload.timeSessions.map { it.toMap() })
            uploadCollection(backupRef.collection("woopData"), payload.woopData.map { it.toMap() })
            uploadCollection(backupRef.collection("ulyssesContracts"), payload.ulyssesContracts.map { it.toMap() })
            uploadCollection(backupRef.collection("sleepLogs"), payload.sleepLogs.map { it.toMap() })
            uploadCollection(backupRef.collection("hyperFocusSessions"), payload.hyperFocusSessions.map { it.toMap() })
            uploadCollection(backupRef.collection("energyPredictions"), payload.energyPredictions.map { it.toMap() })
            uploadCollection(backupRef.collection("notes"), payload.notes.map { it.toMap() })

            // --- preferences (single document) ---------------------------
            if (payload.preferences.isNotEmpty()) {
                backupRef.collection("preferences").document("data")
                    .set(payload.preferences.toMap()).await()
            }

            Result.success(backupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the most recent backup and deserialises it into a [BackupPayload].
     * Returns `null` payload when no backups exist yet.
     */
    suspend fun downloadLatestBackup(uid: String): Result<BackupPayload?> {
        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            var targetBackupId = userDoc.getString("latestBackupId")

            if (targetBackupId == null) {
                val snapshots = backupsRef(uid).get().await()
                if (snapshots.isEmpty) return Result.success(null)
                targetBackupId = snapshots.documents.maxByOrNull {
                    it.getLong("createdAt") ?: 0L
                }?.id
            }

            if (targetBackupId == null) return Result.success(null)
            val payload = downloadBackupById(uid, targetBackupId)
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all available backup metadata entries, ordered newest first.
     */
    suspend fun listBackups(uid: String): Result<List<BackupMeta>> {
        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val indexList = userDoc.get("backups") as? List<Map<String, Any?>>
            if (!indexList.isNullOrEmpty()) {
                val metas = indexList.mapNotNull { map ->
                    val bid = map["backupId"] as? String ?: return@mapNotNull null
                    BackupMeta(
                        backupId = bid,
                        createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                        deviceId = map["deviceId"] as? String ?: "",
                        appVersion = map["appVersion"] as? String ?: "",
                        taskCount = (map["taskCount"] as? Number)?.toInt() ?: 0,
                        goalCount = (map["goalCount"] as? Number)?.toInt() ?: 0
                    )
                }
                return Result.success(metas)
            }

            // Fallback: query collection directly
            val snapshots = backupsRef(uid).get().await()
            val metas = snapshots.documents.mapNotNull { doc ->
                var createdAt = doc.getLong("createdAt")
                var taskCount = doc.getLong("taskCount")?.toInt()
                var goalCount = doc.getLong("goalCount")?.toInt()

                if (createdAt == null) {
                    val sub = doc.reference.collection("meta").document("info").get().await()
                    if (sub.exists()) {
                        createdAt = sub.getLong("createdAt")
                        taskCount = sub.getLong("taskCount")?.toInt()
                        goalCount = sub.getLong("goalCount")?.toInt()
                    }
                }

                BackupMeta(
                    backupId = doc.getString("backupId") ?: doc.id,
                    createdAt = createdAt ?: 0L,
                    deviceId = doc.getString("deviceId") ?: "",
                    appVersion = doc.getString("appVersion") ?: "",
                    taskCount = taskCount ?: 0,
                    goalCount = goalCount ?: 0
                )
            }.sortedByDescending { it.createdAt }

            Result.success(metas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prunes old backups, keeping only the [keepCount] most recent ones.
     */
    suspend fun pruneOldBackups(uid: String, keepCount: Int = 3) {
        try {
            val allMetas = listBackups(uid).getOrNull() ?: return
            if (allMetas.size <= keepCount) return

            val toDelete = allMetas.drop(keepCount)
            for (meta in toDelete) {
                deleteBackup(uid, meta.backupId)
            }
        } catch (_: Exception) {
            // Best-effort pruning — do not propagate
        }
    }

    /**
     * Deletes a specific backup document and all its sub-collections.
     */
    suspend fun deleteBackup(uid: String, backupId: String): Result<Unit> {
        return try {
            val backupRef = backupDocRef(uid, backupId)
            val subCollections = listOf(
                "meta", "tasks", "goals", "timeSessions", "woopData",
                "ulyssesContracts", "sleepLogs", "hyperFocusSessions",
                "energyPredictions", "notes", "preferences"
            )
            for (colName in subCollections) {
                deleteCollection(backupRef.collection(colName))
            }
            backupRef.delete().await()

            // Update user document index
            val userDocRef = firestore.collection("users").document(uid)
            val userDoc = userDocRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val existingList = (userDoc.get("backups") as? List<Map<String, Any?>>)?.toMutableList()
            if (existingList != null) {
                existingList.removeAll { it["backupId"] == backupId }
                val newLatest = existingList.firstOrNull()?.get("backupId") as? String
                userDocRef.set(
                    mapOf(
                        "latestBackupId" to newLatest,
                        "backups" to existingList
                    ),
                    SetOptions.merge()
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Writes a list of maps to a Firestore sub-collection in batches of 500.
     * Each document uses the map's "id" / "taskId" field as its Firestore document ID,
     * falling back to a random UUID.
     */
    private suspend fun uploadCollection(
        colRef: com.google.firebase.firestore.CollectionReference,
        items: List<Map<String, Any?>>
    ) {
        if (items.isEmpty()) return

        val chunks = items.chunked(500)
        for (chunk in chunks) {
            val batch: WriteBatch = firestore.batch()
            for (item in chunk) {
                val docId = (item["id"] as? String)
                    ?: (item["taskId"] as? String)
                    ?: UUID.randomUUID().toString()
                batch.set(colRef.document(docId), item)
            }
            batch.commit().await()
        }
    }

    /**
     * Deletes all documents in a collection in batches of 500.
     */
    private suspend fun deleteCollection(
        colRef: com.google.firebase.firestore.CollectionReference,
        batchSize: Int = 500
    ) {
        var snapshot = colRef.limit(batchSize.toLong()).get().await()
        while (snapshot.documents.isNotEmpty()) {
            val batch: WriteBatch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (snapshot.documents.size < batchSize) break
            snapshot = colRef.limit(batchSize.toLong()).get().await()
        }
    }

    /**
     * Downloads and deserialises a specific backup by its ID.
     * Public so [CloudBackupManager] can request a specific historical snapshot.
     */
    suspend fun downloadBackup(uid: String, backupId: String): Result<BackupPayload?> {
        return try {
            val payload = downloadBackupById(uid, backupId)
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private suspend fun downloadBackupById(uid: String, backupId: String): BackupPayload {
        val backupRef = backupDocRef(uid, backupId)

        val tasks = backupRef.collection("tasks").get().await()
            .documents.mapNotNull { it.data?.toTaskEntity() }

        val goals = backupRef.collection("goals").get().await()
            .documents.mapNotNull { it.data?.toGoalEntity() }

        val timeSessions = backupRef.collection("timeSessions").get().await()
            .documents.mapNotNull { it.data?.toTimeSessionEntity() }

        val woopData = backupRef.collection("woopData").get().await()
            .documents.mapNotNull { it.data?.toWoopEntity() }

        val ulyssesContracts = backupRef.collection("ulyssesContracts").get().await()
            .documents.mapNotNull { it.data?.toUlyssesContractEntity() }

        val sleepLogs = backupRef.collection("sleepLogs").get().await()
            .documents.mapNotNull { it.data?.toSleepLogEntity() }

        val hyperFocusSessions = backupRef.collection("hyperFocusSessions").get().await()
            .documents.mapNotNull { it.data?.toHyperFocusSessionEntity() }

        val energyPredictions = backupRef.collection("energyPredictions").get().await()
            .documents.mapNotNull { it.data?.toEnergyPredictionEntity() }

        val notes = backupRef.collection("notes").get().await()
            .documents.mapNotNull { it.data?.toNoteEntity() }

        @Suppress("UNCHECKED_CAST")
        val preferences = backupRef.collection("preferences").document("data")
            .get().await().data?.mapValues { it.value?.toString() ?: "" }
            ?.toMap() ?: emptyMap()

        return BackupPayload(
            tasks = tasks,
            goals = goals,
            timeSessions = timeSessions,
            woopData = woopData,
            ulyssesContracts = ulyssesContracts,
            sleepLogs = sleepLogs,
            energyPredictions = energyPredictions,
            hyperFocusSessions = hyperFocusSessions,
            preferences = preferences,
            notes = notes
        )
    }
}

// ===========================================================================


// Serialization — toMap() extension functions (reflection-free)
// ===========================================================================

// ---------------------------------------------------------------------------
// TaskEntity
// ---------------------------------------------------------------------------
private fun TaskEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "quadrant" to quadrant.name,
    "priority" to priority.name,
    "tags" to tags,
    "deadlineDate" to deadlineDate,
    "deadlineTime" to deadlineTime,
    "scheduledDate" to scheduledDate,
    "scheduledTime" to scheduledTime,
    "isScheduleLocked" to isScheduleLocked,
    "estimatedDurationMinutes" to estimatedDurationMinutes,
    "recurrence" to recurrence.name,
    "recurrenceIntervalDays" to recurrenceIntervalDays,
    "reminderFlags" to reminderFlags,
    "impactScore" to impactScore,
    "valueScore" to valueScore,
    "effortScore" to effortScore,
    "parentTaskId" to parentTaskId,
    "blockingTaskIds" to blockingTaskIds,
    "dependsOnTaskIds" to dependsOnTaskIds,
    "waitingFor" to waitingFor,
    "totalTimeTrackedMinutes" to totalTimeTrackedMinutes.toDouble(),
    "sessionCount" to sessionCount,
    "lastSessionDurationMinutes" to lastSessionDurationMinutes?.toDouble(),
    "actualDurationMinutes" to actualDurationMinutes?.toDouble(),
    "estimationErrorMape" to estimationErrorMape?.toDouble(),
    "estimationErrorSmape" to estimationErrorSmape?.toDouble(),
    "status" to status.name,
    "completedAt" to completedAt,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "energyLevel" to energyLevel.name,
    "taskType" to taskType.name,
    "contextTag" to contextTag,
    "goalId" to goalId,
    "ifThenPlan" to ifThenPlan,
    "isHabitual" to isHabitual,
    "habitStreak" to habitStreak,
    "isFrog" to isFrog,
    "postponeCount" to postponeCount,
    "focusModePoints" to focusModePoints,
    "enjoymentScore" to enjoymentScore,
    "isPublicCommitment" to isPublicCommitment,
    "isAnxietyTask" to isAnxietyTask,
    "goalRiskLevel" to goalRiskLevel,
    "isAutoScheduled" to isAutoScheduled,
    "lastAutoScheduledAt" to lastAutoScheduledAt,
    "habitDate" to habitDate,
    "affectiveForecastError" to affectiveForecastError?.toDouble(),
    "woopPromptShown" to woopPromptShown,
    "distractionScore" to distractionScore.toDouble(),
    "schedulingCategory" to schedulingCategory
)

private fun Map<String, Any?>.toTaskEntity(): TaskEntity? = runCatching {
    TaskEntity(
        id = str("id") ?: return null,
        title = str("title") ?: return null,
        description = str("description") ?: "",
        quadrant = enumOf<Quadrant>("quadrant") ?: Quadrant.SCHEDULE,
        priority = enumOf<Priority>("priority") ?: Priority.MEDIUM,
        tags = str("tags") ?: "",
        deadlineDate = long("deadlineDate"),
        deadlineTime = long("deadlineTime"),
        scheduledDate = long("scheduledDate"),
        scheduledTime = long("scheduledTime"),
        isScheduleLocked = bool("isScheduleLocked") ?: false,
        estimatedDurationMinutes = int("estimatedDurationMinutes") ?: 0,
        recurrence = enumOf<Recurrence>("recurrence") ?: Recurrence.NONE,
        recurrenceIntervalDays = int("recurrenceIntervalDays") ?: 1,
        reminderFlags = int("reminderFlags") ?: 0,
        impactScore = int("impactScore") ?: 50,
        valueScore = int("valueScore") ?: 50,
        effortScore = int("effortScore") ?: 50,
        parentTaskId = str("parentTaskId"),
        blockingTaskIds = str("blockingTaskIds") ?: "",
        dependsOnTaskIds = str("dependsOnTaskIds") ?: "",
        waitingFor = str("waitingFor") ?: "",
        totalTimeTrackedMinutes = float("totalTimeTrackedMinutes") ?: 0f,
        sessionCount = int("sessionCount") ?: 0,
        lastSessionDurationMinutes = float("lastSessionDurationMinutes"),
        actualDurationMinutes = float("actualDurationMinutes"),
        estimationErrorMape = float("estimationErrorMape"),
        estimationErrorSmape = float("estimationErrorSmape"),
        status = enumOf<TaskStatus>("status") ?: TaskStatus.ACTIVE,
        completedAt = long("completedAt"),
        createdAt = long("createdAt") ?: System.currentTimeMillis(),
        updatedAt = long("updatedAt") ?: System.currentTimeMillis(),
        energyLevel = enumOf<EnergyLevel>("energyLevel") ?: EnergyLevel.MEDIUM,
        taskType = enumOf<TaskType>("taskType") ?: TaskType.ANALYTICAL,
        contextTag = str("contextTag") ?: "",
        goalId = str("goalId"),
        ifThenPlan = str("ifThenPlan") ?: "",
        isHabitual = bool("isHabitual") ?: false,
        habitStreak = int("habitStreak") ?: 0,
        isFrog = bool("isFrog") ?: false,
        postponeCount = int("postponeCount") ?: 0,
        focusModePoints = int("focusModePoints") ?: 0,
        enjoymentScore = int("enjoymentScore") ?: 50,
        isPublicCommitment = bool("isPublicCommitment") ?: false,
        isAnxietyTask = bool("isAnxietyTask") ?: false,
        goalRiskLevel = int("goalRiskLevel") ?: 0,
        isAutoScheduled = bool("isAutoScheduled") ?: false,
        lastAutoScheduledAt = long("lastAutoScheduledAt"),
        habitDate = long("habitDate"),
        affectiveForecastError = float("affectiveForecastError"),
        woopPromptShown = bool("woopPromptShown") ?: false,
        distractionScore = float("distractionScore") ?: -1f,
        schedulingCategory = str("schedulingCategory")
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// GoalEntity
// ---------------------------------------------------------------------------
private fun GoalEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "targetDate" to targetDate,
    "identityStatement" to identityStatement,
    "completedTaskCount" to completedTaskCount,
    "isActive" to isActive,
    "createdAt" to createdAt
)

private fun Map<String, Any?>.toGoalEntity(): GoalEntity? = runCatching {
    GoalEntity(
        id = str("id") ?: return null,
        title = str("title") ?: return null,
        description = str("description") ?: "",
        targetDate = long("targetDate"),
        identityStatement = str("identityStatement") ?: "",
        completedTaskCount = int("completedTaskCount") ?: 0,
        isActive = bool("isActive") ?: true,
        createdAt = long("createdAt") ?: System.currentTimeMillis()
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// TimeSessionEntity
// ---------------------------------------------------------------------------
private fun TimeSessionEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "taskId" to taskId,
    "startedAt" to startedAt,
    "endedAt" to endedAt,
    "pausedAt" to pausedAt,
    "totalPausedMs" to totalPausedMs,
    "durationMinutes" to durationMinutes.toDouble(),
    "pauseResumeCount" to pauseResumeCount,
    "appSwitchCount" to appSwitchCount,
    "interruptionBurstCount" to interruptionBurstCount,
    "sessionType" to sessionType,
    "pomodoroNumber" to pomodoroNumber,
    "notes" to notes
)

private fun Map<String, Any?>.toTimeSessionEntity(): TimeSessionEntity? = runCatching {
    TimeSessionEntity(
        id = str("id") ?: return null,
        taskId = str("taskId") ?: return null,
        startedAt = long("startedAt") ?: return null,
        endedAt = long("endedAt"),
        pausedAt = long("pausedAt"),
        totalPausedMs = long("totalPausedMs") ?: 0L,
        durationMinutes = float("durationMinutes") ?: 0f,
        pauseResumeCount = int("pauseResumeCount") ?: 0,
        appSwitchCount = int("appSwitchCount") ?: 0,
        interruptionBurstCount = int("interruptionBurstCount") ?: 0,
        sessionType = str("sessionType") ?: "MANUAL",
        pomodoroNumber = int("pomodoroNumber") ?: 0,
        notes = str("notes") ?: ""
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// WoopEntity
// ---------------------------------------------------------------------------
private fun WoopEntity.toMap(): Map<String, Any?> = mapOf(
    "taskId" to taskId,
    "wish" to wish,
    "outcome" to outcome,
    "obstacle" to obstacle,
    "plan" to plan,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

private fun Map<String, Any?>.toWoopEntity(): WoopEntity? = runCatching {
    WoopEntity(
        taskId = str("taskId") ?: return null,
        wish = str("wish") ?: "",
        outcome = str("outcome") ?: "",
        obstacle = str("obstacle") ?: "",
        plan = str("plan") ?: "",
        createdAt = long("createdAt") ?: System.currentTimeMillis(),
        updatedAt = long("updatedAt") ?: System.currentTimeMillis()
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// UlyssesContractEntity
// ---------------------------------------------------------------------------
private fun UlyssesContractEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "taskId" to taskId,
    "deadlineAt" to deadlineAt,
    "consequence" to consequence,
    "outcome" to outcome?.name,
    "createdAt" to createdAt
)

private fun Map<String, Any?>.toUlyssesContractEntity(): UlyssesContractEntity? = runCatching {
    UlyssesContractEntity(
        id = str("id") ?: return null,
        taskId = str("taskId") ?: return null,
        deadlineAt = long("deadlineAt") ?: return null,
        consequence = str("consequence") ?: "",
        outcome = str("outcome")?.let {
            runCatching { com.neuroflow.app.data.local.entity.ContractOutcome.valueOf(it) }.getOrNull()
        },
        createdAt = long("createdAt") ?: System.currentTimeMillis()
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// SleepLogEntity
// ---------------------------------------------------------------------------
private fun SleepLogEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "startAt" to startAt,
    "endAt" to endAt,
    "durationMinutes" to durationMinutes,
    "source" to source,
    "notes" to notes,
    "createdAt" to createdAt
)

private fun Map<String, Any?>.toSleepLogEntity(): SleepLogEntity? = runCatching {
    SleepLogEntity(
        id = str("id") ?: return null,
        startAt = long("startAt") ?: return null,
        endAt = long("endAt") ?: return null,
        durationMinutes = int("durationMinutes") ?: return null,
        source = str("source") ?: "MANUAL",
        notes = str("notes") ?: "",
        createdAt = long("createdAt") ?: System.currentTimeMillis()
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// EnergyPredictionEntity
// ---------------------------------------------------------------------------
private fun EnergyPredictionEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "predictedAtMillis" to predictedAtMillis,
    "predictedAtDayOfWeek" to predictedAtDayOfWeek,
    "predictedAtHourOfDay" to predictedAtHourOfDay,
    "peakDetectionAgeMillis" to peakDetectionAgeMillis,
    "sleepLogAgeMillis" to sleepLogAgeMillis,
    "sessionDataAgeMillis" to sessionDataAgeMillis,
    "baselineRawEnergy" to baselineRawEnergy.toDouble(),
    "peakScore" to peakScore.toDouble(),
    "fatiguePenalty" to fatiguePenalty.toDouble(),
    "sleepPressurePoints" to sleepPressurePoints,
    "fatiguePercent" to fatiguePercent,
    "momentAdjustment" to momentAdjustment.toDouble(),
    "momentConfidence" to momentConfidence.toDouble(),
    "momentSupportScore" to momentSupportScore.toDouble(),
    "momentPressureScore" to momentPressureScore.toDouble(),
    "adjustedRawEnergy" to adjustedRawEnergy.toDouble(),
    "usableEnergy" to usableEnergy,
    "chronotype" to chronotype,
    "wakeUpHour" to wakeUpHour,
    "peakMinuteOfDay" to peakMinuteOfDay,
    "peakConfidence" to peakConfidence.toDouble(),
    "recentFocusMinutes" to recentFocusMinutes.toDouble(),
    "recentInterruptionCount" to recentInterruptionCount,
    "recentAppSwitchCount" to recentAppSwitchCount,
    "activeTaskCount" to activeTaskCount,
    "notificationCount" to notificationCount,
    "createdAtMillis" to createdAtMillis
)

private fun Map<String, Any?>.toEnergyPredictionEntity(): EnergyPredictionEntity? = runCatching {
    EnergyPredictionEntity(
        id = str("id") ?: return null,
        predictedAtMillis = long("predictedAtMillis") ?: return null,
        predictedAtDayOfWeek = int("predictedAtDayOfWeek") ?: return null,
        predictedAtHourOfDay = int("predictedAtHourOfDay") ?: return null,
        peakDetectionAgeMillis = long("peakDetectionAgeMillis") ?: return null,
        sleepLogAgeMillis = long("sleepLogAgeMillis") ?: return null,
        sessionDataAgeMillis = long("sessionDataAgeMillis") ?: return null,
        baselineRawEnergy = float("baselineRawEnergy") ?: return null,
        peakScore = float("peakScore") ?: return null,
        fatiguePenalty = float("fatiguePenalty") ?: return null,
        sleepPressurePoints = int("sleepPressurePoints") ?: return null,
        fatiguePercent = int("fatiguePercent") ?: return null,
        momentAdjustment = float("momentAdjustment") ?: return null,
        momentConfidence = float("momentConfidence") ?: return null,
        momentSupportScore = float("momentSupportScore") ?: return null,
        momentPressureScore = float("momentPressureScore") ?: return null,
        adjustedRawEnergy = float("adjustedRawEnergy") ?: return null,
        usableEnergy = int("usableEnergy") ?: return null,
        chronotype = str("chronotype") ?: return null,
        wakeUpHour = int("wakeUpHour") ?: return null,
        peakMinuteOfDay = int("peakMinuteOfDay") ?: return null,
        peakConfidence = float("peakConfidence") ?: return null,
        recentFocusMinutes = float("recentFocusMinutes") ?: return null,
        recentInterruptionCount = int("recentInterruptionCount") ?: return null,
        recentAppSwitchCount = int("recentAppSwitchCount") ?: return null,
        activeTaskCount = int("activeTaskCount") ?: return null,
        notificationCount = int("notificationCount") ?: return null,
        createdAtMillis = long("createdAtMillis") ?: System.currentTimeMillis()
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// HyperFocusSessionEntity
// ---------------------------------------------------------------------------
private fun HyperFocusSessionEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "startedAt" to startedAt,
    "state" to state.name,
    "blockedPackages" to blockedPackages,
    "dailyTaskTarget" to dailyTaskTarget,
    "tasksCompletedAtStart" to tasksCompletedAtStart,
    "currentTier" to currentTier.name,
    "fullyUnlockedAt" to fullyUnlockedAt,
    "endedAt" to endedAt
)

private fun Map<String, Any?>.toHyperFocusSessionEntity(): HyperFocusSessionEntity? = runCatching {
    HyperFocusSessionEntity(
        id = str("id") ?: return null,
        startedAt = long("startedAt") ?: return null,
        state = str("state")?.let {
            runCatching { HyperFocusState.valueOf(it) }.getOrNull()
        } ?: HyperFocusState.INACTIVE,
        blockedPackages = str("blockedPackages") ?: "[]",
        dailyTaskTarget = int("dailyTaskTarget") ?: 0,
        tasksCompletedAtStart = int("tasksCompletedAtStart") ?: 0,
        currentTier = str("currentTier")?.let {
            runCatching { RewardTier.valueOf(it) }.getOrNull()
        } ?: RewardTier.NONE,
        fullyUnlockedAt = long("fullyUnlockedAt"),
        endedAt = long("endedAt")
    )
}.getOrNull()

// ---------------------------------------------------------------------------
// NoteEntity
// ---------------------------------------------------------------------------
private fun NoteEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "content" to content,
    "colorHex" to colorHex,
    "isPinned" to isPinned,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "tags" to tags
)

private fun Map<String, Any?>.toNoteEntity(): NoteEntity? = runCatching {
    NoteEntity(
        id = str("id") ?: return null,
        title = str("title") ?: "",
        content = str("content") ?: "",
        colorHex = str("colorHex") ?: "#FFFFFF",
        isPinned = bool("isPinned") ?: false,
        createdAt = long("createdAt") ?: System.currentTimeMillis(),
        updatedAt = long("updatedAt") ?: System.currentTimeMillis(),
        tags = str("tags") ?: ""
    )
}.getOrNull()

// ===========================================================================
// Map accessor helpers — safe typed reads from Firestore Map<String, Any?>
// ===========================================================================

private fun Map<String, Any?>.str(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.long(key: String): Long? =
    when (val v = this[key]) {
        is Long -> v
        is Int -> v.toLong()
        is Double -> v.toLong()
        is Number -> v.toLong()
        else -> null
    }

private fun Map<String, Any?>.int(key: String): Int? =
    when (val v = this[key]) {
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is Number -> v.toInt()
        else -> null
    }

private fun Map<String, Any?>.float(key: String): Float? =
    when (val v = this[key]) {
        is Float -> v
        is Double -> v.toFloat()
        is Long -> v.toFloat()
        is Int -> v.toFloat()
        is Number -> v.toFloat()
        else -> null
    }

private fun Map<String, Any?>.bool(key: String): Boolean? =
    this[key] as? Boolean

private inline fun <reified T : Enum<T>> Map<String, Any?>.enumOf(key: String): T? =
    str(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
