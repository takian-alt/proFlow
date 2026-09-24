package com.neuroflow.app.data.sync

sealed class GoogleAuthState {
    object SignedOut : GoogleAuthState()
    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String?
    ) : GoogleAuthState()
    object Loading : GoogleAuthState()
    data class Error(val message: String) : GoogleAuthState()
}
