package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Validates that the current Firebase user has not been deleted, disabled, or had their session
 * revoked on the server. Returns `false` only on confirmed invalid/revoked user errors so offline
 * usage continues smoothly when the device temporarily has no internet connectivity.
 */
suspend fun validateActiveSession(user: FirebaseUser): Boolean {
    return try {
        user.reload().await()
        true
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: FirebaseAuthInvalidUserException) {
        if (BuildConfig.DEBUG) {
            Log.w("Auth", "Active Firebase session is no longer valid: ${e.errorCode}")
        }
        false
    } catch (_: Exception) {
        // Network timeout or offline state — do not evict a valid offline session
        true
    }
}

/**
 * Silent auto sign-in on cold start using GetGoogleIdOption as the sole option in the request.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                if (Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption as the sole option in the request.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Sign-in is currently unavailable. Please try again later.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                if (Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                } else {
                    onAuthError("Could not complete sign-in. Please try again.")
                }
            } else {
                onAuthError("Unable to verify Google account credential. Please try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            val rawMsg = e.message.orEmpty()
            if (BuildConfig.DEBUG) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed")
            }
            val looksLikeConfigError = rawMsg.contains("28444", ignoreCase = true) ||
                rawMsg.contains("developer", ignoreCase = true) ||
                rawMsg.contains("reauth", ignoreCase = true) ||
                rawMsg.contains("not found", ignoreCase = true) ||
                rawMsg.contains("16:", ignoreCase = true)
            if (looksLikeConfigError) {
                onAuthError("Google Sign-In could not verify this app build. Please install an official signed release.")
            } else {
                onAuthCancelled()
            }
        } catch (_: NoCredentialException) {
            if (BuildConfig.DEBUG) {
                Log.w("Auth", "No Google credential available")
            }
            onAuthError("No Google account is available on this device. Please add a Google account and try again.")
        } catch (e: GetCredentialException) {
            if (BuildConfig.DEBUG) {
                Log.e("Auth", "Google Sign-In credential error: ${e.type}")
            }
            onAuthError("Google Sign-In could not be completed. Please check your connection and try again.")
        } catch (_: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("Auth", "Google Sign-In failed")
            }
            onAuthError("Sign in failed. Please try again.")
        }
    }
}

/**
 * Safe sign-out wrapping local account cleanup + Credential Manager clear + Firebase sign-out in NonCancellable.
 */
fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeFirebaseSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeFirebaseSignOut()
            } catch (_: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w("Auth", "Pre-signout cleanup encountered an error")
                }
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w("Auth", "Failed to clear credential state")
                }
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
