package com.neuroflow.app.feature.hyperfocus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// Exactly one DataStore instance may be live per file per process, and launcher_prefs is now written
// by the launcher and read here, so the delegate lives outside presentation/launcher and survives the
// Task 17 deletion instead of being duplicated.
internal val Context.launcherPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_prefs"
)

/** Reads what the launcher wrote before the allowlist existed. Nothing here is ever written back. */
@Singleton
class LegacyLauncherPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun distractionScoresSnapshot(): Map<String, Int> =
        context.launcherPreferencesDataStore.data.first()[KEY_DISTRACTION_SCORES]
            ?.let(::parseLegacyScores)
            ?: emptyMap()

    // Frozen copy of PinnedAppsDataStore.parseDistractionScores (private there, deleted in Task 17):
    // same wire format, so a map the launcher wrote today reads back identically here.
    private fun parseLegacyScores(json: String): Map<String, Int> = try {
        val obj = JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getInt(it) }
    } catch (_: Exception) {
        emptyMap()
    }

    private companion object {
        val KEY_DISTRACTION_SCORES = stringPreferencesKey("distraction_scores")
    }
}
