package com.neuroflow.app.data.sync

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    fun observeAuthState(): Flow<GoogleAuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val state = if (user == null) {
                GoogleAuthState.SignedOut
            } else {
                GoogleAuthState.SignedIn(
                    uid = user.uid,
                    displayName = user.displayName ?: "User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString()
                )
            }
            trySend(state)
        }
        trySend(currentAuthState())
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogleToken(idToken: String): GoogleAuthState {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return GoogleAuthState.Error("Firebase sign-in returned no user.")
            GoogleAuthState.SignedIn(
                uid = user.uid,
                displayName = user.displayName ?: "User",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        } catch (e: FirebaseNetworkException) {
            GoogleAuthState.Error("Network error while signing in to Firebase. Check your internet connection.")
        } catch (e: FirebaseAuthException) {
            val hint = when (e.errorCode) {
                "ERROR_INVALID_CREDENTIAL",
                "ERROR_INVALID_CUSTOM_TOKEN",
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Ensure the Google Sign-In provider is enabled and the app's SHA-1 fingerprint is registered in Firebase Authentication."
                else -> e.localizedMessage ?: "Authentication error (${e.errorCode})."
            }
            GoogleAuthState.Error(hint)
        } catch (e: Exception) {
            GoogleAuthState.Error(e.message ?: "Unknown Firebase sign-in error")
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun getUid(): String? = auth.currentUser?.uid
}
