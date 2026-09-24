package com.neuroflow.app.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    fun currentAuthState(): GoogleAuthState {
        val user = auth.currentUser ?: return GoogleAuthState.SignedOut
        return GoogleAuthState.SignedIn(
            uid = user.uid,
            displayName = user.displayName ?: "User",
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun signInWithGoogleToken(idToken: String): GoogleAuthState {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return GoogleAuthState.Error("Sign-in failed")
            GoogleAuthState.SignedIn(
                uid = user.uid,
                displayName = user.displayName ?: "User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        } catch (e: Exception) {
            GoogleAuthState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun getUid(): String? = auth.currentUser?.uid
}
