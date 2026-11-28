package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Logs redacted Firestore diagnostics strictly in debug builds (never logging user email or
 * provider PII, and suppressing verbose logs completely in release builds).
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val code = (rootError as? FirebaseFirestoreException)?.code?.name ?: rootError.javaClass.simpleName

    if (!BuildConfig.DEBUG) {
        return code
    }

    val isSignedIn = try {
        FirebaseAuth.getInstance().currentUser != null
    } catch (_: Exception) {
        false
    }

    val errorInfoJson = JSONObject().apply {
        put("code", code)
        put("operationType", operationType.value)
        put("signedIn", isSignedIn)
        put("pathSegment", path?.substringAfterLast("/") ?: "unknown")
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Maps internal exceptions to safe, non-leaky user-facing messages so release UI banners never
 * expose raw Firestore paths, internal class names, or auth details.
 */
fun toUserSafeErrorMessage(error: Throwable?): String? {
    if (error == null) return null
    val causes = generateSequence(error) { it.cause }
    val fsErr = causes.firstOrNull { it is FirebaseFirestoreException } as? FirebaseFirestoreException
    if (fsErr != null) {
        return when (fsErr.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Your session needs verification. Please sign in again."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Unable to reach cloud sync right now. Your changes are saved locally."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Cloud sync limit reached temporarily. Please try again later."
            else -> "Could not sync changes right now. Please try again."
        }
    }
    if (error is IllegalArgumentException) {
        return error.message?.take(120) ?: "Please check your input and try again."
    }
    return "Something went wrong. Please try again."
}
