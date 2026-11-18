package com.neuroflow.app.presentation.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.neuroflow.app.data.sync.BackupMeta
import com.neuroflow.app.data.sync.CloudBackupManager
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.GoogleSignInManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSyncViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val prefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val signInManager = mockk<GoogleSignInManager>(relaxed = true)
    private val cloudBackupManager = mockk<CloudBackupManager>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private lateinit var authFlow: MutableSharedFlow<GoogleAuthState>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authFlow = MutableSharedFlow(replay = 1)
        every { context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.getBoolean("auto_sync_enabled", false) } returns false
        every { sharedPrefs.edit() } returns prefsEditor
        every { prefsEditor.putBoolean(any(), any()) } returns prefsEditor
        every { signInManager.currentAuthState() } returns GoogleAuthState.SignedOut
        every { signInManager.observeAuthState() } returns authFlow
        coEvery { cloudBackupManager.listBackups() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeWithIdToken transitions authState to SignedIn and loads backups`() = runTest(testDispatcher) {
        val signedIn = GoogleAuthState.SignedIn(
            uid = "uid-123",
            displayName = "Test User",
            email = "test@example.com",
            photoUrl = null
        )
        val backups = listOf(BackupMeta("b1", 1000L, "Pixel", "1.0.0", 5, 2))
        coEvery { signInManager.signInWithGoogleToken("valid-id-token") } returns signedIn
        coEvery { cloudBackupManager.listBackups() } returns backups

        val vm = AccountSyncViewModel(context, signInManager, cloudBackupManager, workManager)
        advanceUntilIdle()

        vm.completeWithIdToken("valid-id-token")
        advanceUntilIdle()

        assertEquals(signedIn, vm.authState.value)
        assertEquals(backups, vm.availableBackups.value)
        coVerify(exactly = 1) { signInManager.signInWithGoogleToken("valid-id-token") }
    }

    @Test
    fun `completeWithIdToken propagates error state and preserves banner over SignedOut emission`() = runTest(testDispatcher) {
        val err = GoogleAuthState.Error("SHA-1 fingerprint mismatch")
        coEvery { signInManager.signInWithGoogleToken("bad-token") } returns err

        val vm = AccountSyncViewModel(context, signInManager, cloudBackupManager, workManager)
        advanceUntilIdle()

        vm.completeWithIdToken("bad-token")
        advanceUntilIdle()
        assertEquals(err, vm.authState.value)

        // Emitting SignedOut from AuthStateListener must not erase explicit Error banner
        authFlow.emit(GoogleAuthState.SignedOut)
        advanceUntilIdle()
        assertTrue(vm.authState.value is GoogleAuthState.Error)
        assertEquals("SHA-1 fingerprint mismatch", (vm.authState.value as GoogleAuthState.Error).message)
    }

    @Test
    fun `signOut resets authState backups and disables auto sync`() = runTest(testDispatcher) {
        val signedIn = GoogleAuthState.SignedIn("uid-1", "User", "u@e.com", null)
        every { signInManager.currentAuthState() } returns signedIn
        every { sharedPrefs.getBoolean("auto_sync_enabled", false) } returns true
        coEvery { cloudBackupManager.listBackups() } returns listOf(BackupMeta("b1", 10L, "dev", "1.0.0", 1, 0))

        val vm = AccountSyncViewModel(context, signInManager, cloudBackupManager, workManager)
        advanceUntilIdle()
        assertEquals(signedIn, vm.authState.value)
        assertTrue(vm.autoSyncEnabled.value)

        vm.signOut()
        advanceUntilIdle()

        assertEquals(GoogleAuthState.SignedOut, vm.authState.value)
        assertTrue(vm.availableBackups.value.isEmpty())
        assertFalse(vm.autoSyncEnabled.value)
        coVerify(exactly = 1) { signInManager.signOut() }
        verify { prefsEditor.putBoolean("auto_sync_enabled", false) }
    }
}
