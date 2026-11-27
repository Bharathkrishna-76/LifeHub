package com.example.update

import java.io.File

/**
 * Exact Firebase Remote Config parameter keys controlling centralized APK updates.
 */
object RemoteUpdateKeys {
    const val LATEST_VERSION_CODE = "latest_version_code"
    const val LATEST_VERSION_NAME = "latest_version_name"
    const val APK_DOWNLOAD_URL = "apk_download_url"
    const val RELEASE_NOTES = "release_notes"
    const val UPDATE_ENABLED = "update_enabled"
    const val MINIMUM_VERSION_CODE = "minimum_version_code"
}

/**
 * Snapshot of centralized update configuration retrieved from Firebase Remote Config
 * (or loaded from the local DataStore cache when offline).
 */
data class RemoteUpdateConfig(
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0.0",
    val apkDownloadUrl: String = "",
    val releaseNotes: String = "",
    val updateEnabled: Boolean = false,
    val minimumVersionCode: Long = 1L,
    val fetchedAtEpochMs: Long = 0L
)

enum class UpdateRequirement {
    NONE,
    OPTIONAL,
    MANDATORY
}

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object Available : UpdatePhase
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float? // null when Content-Length is indeterminate
    ) : UpdatePhase
    data object Verifying : UpdatePhase
    data class ReadyToInstall(val apkFile: File) : UpdatePhase
    data class PermissionRequired(val apkFile: File) : UpdatePhase
    data class Error(
        val message: String,
        val canRetryInstall: Boolean = false,
        val verifiedApkFile: File? = null
    ) : UpdatePhase
}

data class UpdateUiState(
    val installedVersionCode: Long = 1L,
    val installedVersionName: String = "1.0.0",
    val config: RemoteUpdateConfig? = null,
    val requirement: UpdateRequirement = UpdateRequirement.NONE,
    val phase: UpdatePhase = UpdatePhase.Idle,
    val isDialogVisible: Boolean = false,
    val isManualChecking: Boolean = false,
    val manualStatusMessage: String? = null
) {
    val isMandatoryBlocking: Boolean
        get() = requirement == UpdateRequirement.MANDATORY && config != null

    val isOptionalPromptVisible: Boolean
        get() = requirement == UpdateRequirement.OPTIONAL && isDialogVisible && config != null
}
