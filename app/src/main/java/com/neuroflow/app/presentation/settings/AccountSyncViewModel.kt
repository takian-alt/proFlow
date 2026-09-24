package com.neuroflow.app.presentation.settings

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signInManager: GoogleSignInManager,
    private val cloudBackupManager: CloudBackupManager,
    private val workManager: WorkManager
) : ViewModel() {

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

    init {
        _autoSyncEnabled.value = syncPrefs.getBoolean("auto_sync_enabled", false)
        refreshAuthState()
    }

    fun refreshAuthState() {
        _authState.value = signInManager.currentAuthState()
        if (_authState.value is GoogleAuthState.SignedIn) {
            loadBackupList()
        }
    }


    /** Call this from the Activity after receiving the Google Sign-In result. */
    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _authState.value = GoogleAuthState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    _authState.value = GoogleAuthState.Error("No ID token returned. Ensure Google Provider is enabled in Firebase Console.")
                    return@launch
                }
                val state = signInManager.signInWithGoogleToken(idToken)
                _authState.value = state
                if (state is GoogleAuthState.SignedIn) {
                    loadBackupList()
                }
            } catch (e: ApiException) {
                val errorMsg = when (e.statusCode) {
                    10 -> "Sign-in failed (Code 10: DEVELOPER_ERROR). Please check:\n1. 'Google' provider is Enabled in Firebase Authentication -> Sign-in method.\n2. SHA-1 fingerprint matches Firebase console."
                    12500 -> "Sign-in failed (Code 12500). Please update Google Play Services on your device."
                    12501 -> "Sign-in was cancelled."
                    7 -> "Sign-in failed (Code 7: Network Error). Please check internet connection."
                    else -> "Sign-in failed (Code ${e.statusCode}): ${e.message ?: "ApiException"}"
                }
                _authState.value = GoogleAuthState.Error(errorMsg)
            } catch (e: Exception) {
                _authState.value = GoogleAuthState.Error(e.message ?: "Unknown sign-in error")
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            signInManager.signOut()
            runCatching {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            }
            CloudSyncWorker.cancel(workManager)
            syncPrefs.edit().putBoolean("auto_sync_enabled", false).apply()
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
