package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface InstallLaunchResult {
    data object Launched : InstallLaunchResult
    data object NeedsUnknownSourcesPermission : InstallLaunchResult
    data class Failed(val message: String) : InstallLaunchResult
}

/**
 * Launches Android's official system package installer via FileProvider without silent
 * installation or broad storage permissions.
 */
object ApkInstaller {

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return try {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchSystemInstaller(context: Context, apkFile: File): InstallLaunchResult {
        if (!ApkSecurityVerifier.isConfinedToUpdatesDir(context, apkFile) || !apkFile.exists()) {
            return InstallLaunchResult.Failed("Update APK file could not be found. Please download again.")
        }

        if (!canInstallPackages(context)) {
            return InstallLaunchResult.NeedsUnknownSourcesPermission
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            InstallLaunchResult.Launched
        } catch (_: Exception) {
            InstallLaunchResult.Failed("Could not open Android's package installer on this device.")
        }
    }
}
