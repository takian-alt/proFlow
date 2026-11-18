package com.neuroflow.app.presentation.settings

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.neuroflow.app.R
import com.neuroflow.app.data.sync.BackupMeta
import com.neuroflow.app.data.sync.BackupStatus
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.GoogleSignInManager
import com.neuroflow.app.data.sync.RestoreStatus
import com.neuroflow.app.worker.CloudSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AccountSyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signInManager: GoogleSignInManager,
    private val cloudBackupManager: CloudBackupManager,
    private val workManager: WorkManager
) : ViewModel() {

    private companion object {
        const val TAG = "AccountSyncAuth"
    }

    private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Loading)
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _restoreStatus = MutableStateFlow<RestoreStatus>(RestoreStatus.Idle)
    val restoreStatus: StateFlow<RestoreStatus> = _restoreStatus.asStateFlow()

    private val _availableBackups = MutableStateFlow<List<BackupMeta>>(emptyList())
    val availableBackups: StateFlow<List<BackupMeta>> = _availableBackups.asStateFlow()

    private val syncPrefs by lazy {
        context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE)
    }

    private val _autoSyncEnabled = MutableStateFlow(false)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private var hasAttemptedSilentSignIn = false

    init {
        _autoSyncEnabled.value = syncPrefs.getBoolean("auto_sync_enabled", false)
        refreshAuthState()
        observeAuthChanges()
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            signInManager.observeAuthState().collect { observed ->
                // Preserve explicit Error banner until user retries or signs in
                if (observed is GoogleAuthState.SignedOut && _authState.value is GoogleAuthState.Error) {
                    return@collect
                }
                _authState.value = observed
                if (observed is GoogleAuthState.SignedIn) {
                    loadBackupList()
                }
            }
        }
    }

    fun refreshAuthState() {
        _authState.value = signInManager.currentAuthState()
        if (_authState.value is GoogleAuthState.SignedIn) {
            loadBackupList()
        }
    }

    /**
     * Silent sign-in check for returning users via single-option GetGoogleIdOption.
     * Fails silently back to SignedOut if no authorized credential is stored on device.
     */
    fun attemptSilentSignInIfSignedOut(activityContext: Context) {
        if (hasAttemptedSilentSignIn) return
        if (_authState.value is GoogleAuthState.SignedIn) return
        hasAttemptedSilentSignIn = true

        val clientId = resolveWebClientId() ?: return
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val response = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )
                completeCredentialSignIn(response.credential)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Silent Google Sign-In dismissed: ${e.message}", e)
            } catch (_: NoCredentialException) {
                // Expected on first run before user selects an account interactively.
            } catch (e: Exception) {
                Log.w(TAG, "Silent Google Sign-In skipped: ${e.message}")
            }
        }
    }

    /**
     * Interactive Google Sign-In triggered from UI button using GetSignInWithGoogleOption
     * (single credential option per request for Android 14-16 bottom-sheet responsiveness).
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_authState.value is GoogleAuthState.Loading) return
        val clientId = resolveWebClientId()
        if (clientId == null) {
            _authState.value = GoogleAuthState.Error(
                "Google Sign-In configuration missing (default_web_client_id). Verify google-services.json includes an OAuth Web Client ID."
            )
            return
        }

        val credentialManager = CredentialManager.create(activityContext)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            _authState.value = GoogleAuthState.Loading
            try {
                val response = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )
                completeCredentialSignIn(response.credential)
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _authState.value = GoogleAuthState.SignedOut
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credentials returned: ${e.message}", e)
                _authState.value = GoogleAuthState.Error(
                    "No Google account is signed in on this device or Google Play Services couldn't provide a credential. Add a Google account in device Settings and try again."
                )
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse Google ID token", e)
                _authState.value = GoogleAuthState.Error("Received an invalid Google ID token. Please try again.")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager sign-in failed: ${e.type} - ${e.errorMessage}", e)
                val raw = "${e.type} ${e.errorMessage?.toString().orEmpty()} ${e.message.orEmpty()}"
                val friendly = when {
                    raw.contains("reauth", ignoreCase = true) ||
                        raw.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                        raw.contains("28444") ||
                        raw.contains("16:") ||
                        raw.contains("10:") ->
                        "Google Sign-In is rejected by Firebase/OAuth configuration. Ensure the Google provider is enabled and your debug/release SHA-1 fingerprint is registered in the Firebase project console."
                    raw.contains("network", ignoreCase = true) ->
                        "Network error while contacting Google Play Services. Check your internet connection and retry."
                    else -> e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Google Sign-In failed (${e.type}). Please try again."
                }
                _authState.value = GoogleAuthState.Error(friendly)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                _authState.value = GoogleAuthState.Error(e.message ?: "Unknown sign-in error")
            }
        }
    }

    /**
     * Direct helper for unit testing or token exchange.
     */
    internal suspend fun completeWithIdToken(idToken: String) {
        val state = signInManager.signInWithGoogleToken(idToken)
        _authState.value = state
        if (state is GoogleAuthState.SignedIn) {
            loadBackupList()
        }
    }

    private suspend fun completeCredentialSignIn(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            completeWithIdToken(googleIdTokenCredential.idToken)
        } else {
            Log.e(TAG, "Unexpected credential type returned: ${credential.type}")
            _authState.value = GoogleAuthState.Error("Unexpected credential type returned by Google Sign-In.")
        }
    }

    private fun resolveWebClientId(): String? = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e(TAG, "Missing R.string.default_web_client_id", e)
        null
    }

    fun signOut() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                }.onFailure { err ->
                    Log.w(TAG, "Failed to clear credential state: ${err.message}")
                }
                signInManager.signOut()
                CloudSyncWorker.cancel(workManager)
                syncPrefs.edit().putBoolean("auto_sync_enabled", false).apply()
            }
            _authState.value = GoogleAuthState.SignedOut
            _backupStatus.value = BackupStatus.Idle
            _restoreStatus.value = RestoreStatus.Idle
            _availableBackups.value = emptyList()
            _autoSyncEnabled.value = false
        }
    }

    fun backupNow() {
        if (_backupStatus.value is BackupStatus.InProgress) return
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress()
            _backupStatus.value = cloudBackupManager.backup()
            if (_backupStatus.value is BackupStatus.Success) {
                loadBackupList()
            }
        }
    }

    fun restoreLatest() {
        if (_restoreStatus.value is RestoreStatus.InProgress) return
        viewModelScope.launch {
            _restoreStatus.value = RestoreStatus.InProgress()
            _restoreStatus.value = cloudBackupManager.restore()
        }
    }

    fun restoreFromBackup(backupId: String) {
        if (_restoreStatus.value is RestoreStatus.InProgress) return
        viewModelScope.launch {
            _restoreStatus.value = RestoreStatus.InProgress()
            _restoreStatus.value = cloudBackupManager.restoreFromBackup(backupId)
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            cloudBackupManager.deleteBackup(backupId)
            loadBackupList()
        }
    }

    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        syncPrefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        if (enabled) CloudSyncWorker.schedule(workManager)
        else CloudSyncWorker.cancel(workManager)
    }

    fun clearBackupStatus() { _backupStatus.value = BackupStatus.Idle }
    fun clearRestoreStatus() { _restoreStatus.value = RestoreStatus.Idle }

    private fun loadBackupList() {
        viewModelScope.launch {
            _availableBackups.value = cloudBackupManager.listBackups()
        }
    }
}
