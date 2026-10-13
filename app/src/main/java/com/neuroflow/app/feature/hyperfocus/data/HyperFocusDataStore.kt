package com.neuroflow.app.feature.hyperfocus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.domain.model.RewardTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hyperFocusDataStore: DataStore<Preferences> by preferencesDataStore(name = "hyperfocus_prefs")

data class HyperFocusPreferences(
    val isActive: Boolean = false,
    val sessionId: String? = null,
    val state: HyperFocusState = HyperFocusState.INACTIVE,
    val blockedPackages: Set<String> = emptySet(),
    // --- Allowlist model (B2): global tiers, identical across sessions ---
    val essentialPackages: Set<String> = emptySet(),
    val allowedPackages: Set<String> = emptySet(),
    // --- Per-app pass (D3 + E1): exactly one granted package, wall-clock budget ---
    val activePassPackage: String? = null,
    val activePassExpiresAt: Long? = null,
    // The user's real launcher, resolved once so the off state can hand Home back.
    val realHomeComponent: String? = null,
    val currentTier: RewardTier = RewardTier.NONE,
    val activeUnlockExpiresAt: Long? = null,
    val tasksCompletedAtActivation: Int = 0,
    val dailyTaskTarget: Int = 0,
    val lastServiceHeartbeat: Long = 0L,
    val wrongCodeAttempts: Int = 0,
    val lockoutExpiresAt: Long? = null,
    // Whether tamper condition was observed (accessibility toggled, launcher changed, uninstall attempt, etc.)
    val isTamperDetected: Boolean = false,
    val tamperReason: String? = null,
    val tamperDetectedAt: Long? = null,
    // ID of the code currently shown to the user but not yet used.
    // Prevents claiming a new code while one is already pending.
    val pendingCodeId: String? = null,
    // Snapshot of task IDs that were active when the session started.
    // Only completions of these tasks count toward rewards — prevents spam-adding new tasks.
    val lockedTaskIds: Set<String> = emptySet(),
    // Tracks if an emergency bypass was used this session, canceling intermediate rewards
    val emergencyUsed: Boolean = false,
    // Session mode determines whether focus is task-driven or time-driven.
    val sessionMode: HyperFocusSessionMode = HyperFocusSessionMode.TASK_BASED,
    // Used for time-based sessions.
    val sessionDurationMinutes: Int? = null,
    val sessionEndsAtMillis: Long? = null
)

interface HyperFocusDataStore {
    val flow: Flow<HyperFocusPreferences>
    suspend fun update(transform: (HyperFocusPreferences) -> HyperFocusPreferences)
    suspend fun updateHeartbeat(timestamp: Long)
    fun getHeartbeat(): Long = 0L
    suspend fun current(): HyperFocusPreferences
}

@Singleton
class HyperFocusDataStoreImpl @Inject constructor(
    private val context: Context
) : HyperFocusDataStore {

    private object Keys {
        val PREFS_JSON = stringPreferencesKey("hyperfocus_prefs_json")
    }

    private val serviceHeartbeat = java.util.concurrent.atomic.AtomicLong(0L)

    override val flow: Flow<HyperFocusPreferences> = context.hyperFocusDataStore.data.map { prefs ->
        val base = prefs[Keys.PREFS_JSON]?.let { deserialize(it) } ?: HyperFocusPreferences()
        val hb = serviceHeartbeat.get()
        if (hb > 0L) base.copy(lastServiceHeartbeat = hb) else base
    }

    override suspend fun update(transform: (HyperFocusPreferences) -> HyperFocusPreferences) {
        context.hyperFocusDataStore.edit { prefs ->
            val current = prefs[Keys.PREFS_JSON]?.let { deserialize(it) } ?: HyperFocusPreferences()
            val hb = serviceHeartbeat.get()
            val currentWithHb = if (hb > 0L) current.copy(lastServiceHeartbeat = hb) else current
            val updated = transform(currentWithHb)
            if (updated.lastServiceHeartbeat != currentWithHb.lastServiceHeartbeat) {
                serviceHeartbeat.set(updated.lastServiceHeartbeat)
            }
            prefs[Keys.PREFS_JSON] = serialize(updated)
        }
    }

    override suspend fun updateHeartbeat(timestamp: Long) {
        serviceHeartbeat.set(timestamp)
    }

    override fun getHeartbeat(): Long = serviceHeartbeat.get()

    override suspend fun current(): HyperFocusPreferences {
        val base = flow.first()
        val hb = serviceHeartbeat.get()
        return if (hb > 0L) base.copy(lastServiceHeartbeat = hb) else base
    }

    private fun serialize(prefs: HyperFocusPreferences): String {
        val json = JSONObject()
        json.put("isActive", prefs.isActive)
        json.put("sessionId", prefs.sessionId ?: JSONObject.NULL)
        json.put("state", prefs.state.name)
        val packagesArray = JSONArray()
        prefs.blockedPackages.forEach { packagesArray.put(it) }
        json.put("blockedPackages", packagesArray)
        val essentialArray = JSONArray()
        prefs.essentialPackages.forEach { essentialArray.put(it) }
        json.put("essentialPackages", essentialArray)
        val allowedArray = JSONArray()
        prefs.allowedPackages.forEach { allowedArray.put(it) }
        json.put("allowedPackages", allowedArray)
        json.put("activePassPackage", prefs.activePassPackage ?: JSONObject.NULL)
        json.put("activePassExpiresAt", prefs.activePassExpiresAt ?: JSONObject.NULL)
        json.put("realHomeComponent", prefs.realHomeComponent ?: JSONObject.NULL)
        json.put("currentTier", prefs.currentTier.name)
        json.put("activeUnlockExpiresAt", prefs.activeUnlockExpiresAt ?: JSONObject.NULL)
        json.put("tasksCompletedAtActivation", prefs.tasksCompletedAtActivation)
        json.put("dailyTaskTarget", prefs.dailyTaskTarget)
        json.put("lastServiceHeartbeat", prefs.lastServiceHeartbeat)
        json.put("wrongCodeAttempts", prefs.wrongCodeAttempts)
        json.put("lockoutExpiresAt", prefs.lockoutExpiresAt ?: JSONObject.NULL)
        json.put("isTamperDetected", prefs.isTamperDetected)
        json.put("tamperReason", prefs.tamperReason ?: JSONObject.NULL)
        json.put("tamperDetectedAt", prefs.tamperDetectedAt ?: JSONObject.NULL)
        json.put("pendingCodeId", prefs.pendingCodeId ?: JSONObject.NULL)
        val lockedTasksArray = JSONArray()
        prefs.lockedTaskIds.forEach { lockedTasksArray.put(it) }
        json.put("lockedTaskIds", lockedTasksArray)
        json.put("emergencyUsed", prefs.emergencyUsed)
        json.put("sessionMode", prefs.sessionMode.name)
        json.put("sessionDurationMinutes", prefs.sessionDurationMinutes ?: JSONObject.NULL)
        json.put("sessionEndsAtMillis", prefs.sessionEndsAtMillis ?: JSONObject.NULL)
        return json.toString()
    }

    private fun deserialize(json: String): HyperFocusPreferences {
        return try {
            val obj = JSONObject(json)
            fun stringSet(key: String): Set<String> = buildSet {
                val arr = obj.optJSONArray(key)
                if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
            }
            val blockedPackages = stringSet("blockedPackages")
            HyperFocusPreferences(
                isActive = obj.optBoolean("isActive", false),
                sessionId = if (obj.isNull("sessionId")) null else obj.optString("sessionId", null.toString()).takeIf { it != "null" },
                state = try { HyperFocusState.valueOf(obj.optString("state", HyperFocusState.INACTIVE.name)) } catch (_: Exception) { HyperFocusState.INACTIVE },
                blockedPackages = blockedPackages,
                essentialPackages = stringSet("essentialPackages"),
                allowedPackages = stringSet("allowedPackages"),
                activePassPackage = if (obj.isNull("activePassPackage")) null
                    else obj.optString("activePassPackage").takeIf { it.isNotEmpty() },
                activePassExpiresAt = if (obj.isNull("activePassExpiresAt")) null
                    else obj.optLong("activePassExpiresAt").takeIf { it > 0L },
                realHomeComponent = if (obj.isNull("realHomeComponent")) null
                    else obj.optString("realHomeComponent").takeIf { it.isNotEmpty() },
                currentTier = try { RewardTier.valueOf(obj.optString("currentTier", RewardTier.NONE.name)) } catch (_: Exception) { RewardTier.NONE },
                activeUnlockExpiresAt = if (obj.isNull("activeUnlockExpiresAt")) null else obj.optLong("activeUnlockExpiresAt"),
                tasksCompletedAtActivation = obj.optInt("tasksCompletedAtActivation", 0),
                dailyTaskTarget = obj.optInt("dailyTaskTarget", 0),
                lastServiceHeartbeat = obj.optLong("lastServiceHeartbeat", 0L),
                wrongCodeAttempts = obj.optInt("wrongCodeAttempts", 0),
                lockoutExpiresAt = if (obj.isNull("lockoutExpiresAt")) null else obj.optLong("lockoutExpiresAt"),
                isTamperDetected = obj.optBoolean("isTamperDetected", false),
                tamperReason = if (obj.isNull("tamperReason")) null else obj.optString("tamperReason"),
                tamperDetectedAt = if (obj.isNull("tamperDetectedAt")) null else obj.optLong("tamperDetectedAt"),
                pendingCodeId = if (obj.isNull("pendingCodeId")) null else obj.optString("pendingCodeId").takeIf { it != "null" },
                lockedTaskIds = stringSet("lockedTaskIds"),
                emergencyUsed = obj.optBoolean("emergencyUsed", false),
                sessionMode = try {
                    HyperFocusSessionMode.valueOf(
                        obj.optString("sessionMode", HyperFocusSessionMode.TASK_BASED.name)
                    )
                } catch (_: Exception) {
                    HyperFocusSessionMode.TASK_BASED
                },
                sessionDurationMinutes = if (obj.isNull("sessionDurationMinutes")) null
                    else obj.optInt("sessionDurationMinutes").takeIf { it > 0 },
                sessionEndsAtMillis = if (obj.isNull("sessionEndsAtMillis")) null
                    else obj.optLong("sessionEndsAtMillis").takeIf { it > 0L }
            )
        } catch (_: Exception) {
            HyperFocusPreferences()
        }
    }
}
