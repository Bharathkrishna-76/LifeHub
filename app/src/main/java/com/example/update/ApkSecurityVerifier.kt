package com.example.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

sealed interface UrlValidationResult {
    data class Valid(val normalizedUrl: String) : UrlValidationResult
    data class Invalid(val reason: String) : UrlValidationResult
}

sealed interface ApkVerificationResult {
    data class Verified(
        val packageName: String,
        val versionCode: Long,
        val versionName: String
    ) : ApkVerificationResult
    data class Rejected(val reason: String) : ApkVerificationResult
}

/**
 * Enforces strict security checks for remote APK downloads and local APK verification:
 * - HTTPS-only URLs hosted on trusted GitHub Releases domains (including every redirect hop)
 * - File confinement inside app-private cache/updates directory
 * - Valid ZIP/APK header and minimum/maximum file size bounds
 * - Exact package name match, strict versionCode increase (anti-downgrade), and SHA-256
 *   signing certificate match against the currently installed application.
 */
object ApkSecurityVerifier {

    private const val MIN_APK_BYTES = 50L * 1024L // 50 KB
    private const val MAX_APK_BYTES = 250L * 1024L * 1024L // 250 MB

    /**
     * Trusted HTTPS hosts for GitHub Releases and their signed CDN redirect targets.
     */
    private val TRUSTED_INITIAL_HOSTS = setOf(
        "github.com"
    )

    private val TRUSTED_REDIRECT_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com"
    )

    fun validateDownloadUrl(rawUrl: String, isRedirectHop: Boolean = false): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult.Invalid("Update download link is not configured.")
        }

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return UrlValidationResult.Invalid("Update download URL is malformed.")
        }

        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme != "https") {
            return UrlValidationResult.Invalid("Only secure HTTPS download links are allowed.")
        }

        if (!uri.userInfo.isNullOrEmpty()) {
            return UrlValidationResult.Invalid("Download URL must not contain embedded credentials.")
        }

        val host = uri.host?.lowercase(Locale.US)?.trimEnd('.')
            ?: return UrlValidationResult.Invalid("Download URL is missing a valid host.")

        val allowedHosts = if (isRedirectHop) TRUSTED_REDIRECT_HOSTS else TRUSTED_INITIAL_HOSTS
        if (host !in allowedHosts) {
            return UrlValidationResult.Invalid(
                "Blocked untrusted download host ($host). Updates must be hosted on GitHub Releases."
            )
        }

        val path = uri.path.orEmpty()
        if (!isRedirectHop) {
            // Require standard GitHub Releases asset download path: /<owner>/<repo>/releases/download/<tag>/<file>.apk
            val lowerPath = path.lowercase(Locale.US)
            if (!lowerPath.contains("/releases/download/") || !lowerPath.endsWith(".apk")) {
                return UrlValidationResult.Invalid(
                    "Download link must point directly to an .apk asset on GitHub Releases."
                )
            }
        }

        return UrlValidationResult.Valid(trimmed)
    }

    fun getUpdatesDirectory(context: Context): File {
        return File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
    }

    fun isConfinedToUpdatesDir(context: Context, file: File): Boolean {
        return try {
            val baseCanonical = getUpdatesDirectory(context).canonicalPath
            val targetCanonical = file.canonicalPath
            targetCanonical.startsWith(baseCanonical + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    fun verifyDownloadedApk(
        context: Context,
        apkFile: File,
        installedVersionCode: Long
    ): ApkVerificationResult {
        if (!isConfinedToUpdatesDir(context, apkFile)) {
            return ApkVerificationResult.Rejected("Update file path failed security confinement check.")
        }
        if (!apkFile.exists() || !apkFile.isFile) {
            return ApkVerificationResult.Rejected("Downloaded update file is missing.")
        }

        val fileSize = apkFile.length()
        if (fileSize < MIN_APK_BYTES || fileSize > MAX_APK_BYTES) {
            return ApkVerificationResult.Rejected("Downloaded APK appears incomplete or corrupted.")
        }

        // Verify standard ZIP/APK magic header (PK\x03\x04)
        if (!hasZipMagicHeader(apkFile)) {
            return ApkVerificationResult.Rejected("Downloaded file is not a valid Android APK package.")
        }

        val pm = context.packageManager
        val archiveInfo = getArchivePackageInfo(pm, apkFile.absolutePath)
            ?: return ApkVerificationResult.Rejected("Unable to read downloaded APK. The file may be corrupted.")

        // 1. Exact package name verification
        if (archiveInfo.packageName != context.packageName) {
            return ApkVerificationResult.Rejected(
                "Security check failed: APK package (${archiveInfo.packageName}) does not match this app."
            )
        }

        // 2. Anti-downgrade version check
        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode <= installedVersionCode) {
            return ApkVerificationResult.Rejected(
                "Downloaded APK (build $archiveVersionCode) is not newer than your installed version (build $installedVersionCode)."
            )
        }

        // 3. Signing certificate compatibility check
        val installedInfo = getInstalledPackageInfo(pm, context.packageName)
            ?: return ApkVerificationResult.Rejected("Could not read current app signing certificate.")

        val installedCertDigests = extractSigningCertSha256Set(installedInfo)
        val archiveCertDigests = extractSigningCertSha256Set(archiveInfo)

        if (installedCertDigests.isEmpty() || archiveCertDigests.isEmpty()) {
            return ApkVerificationResult.Rejected("Unable to verify APK signing certificate.")
        }

        val signaturesMatch = installedCertDigests.any { it in archiveCertDigests }
        if (!signaturesMatch) {
            return ApkVerificationResult.Rejected(
                "Signing certificate mismatch. This APK was signed with a different key and cannot safely upgrade your installed app."
            )
        }

        return ApkVerificationResult.Verified(
            packageName = archiveInfo.packageName,
            versionCode = archiveVersionCode,
            versionName = archiveInfo.versionName ?: archiveVersionCode.toString()
        )
    }

    fun getInstalledVersion(context: Context): Pair<Long, String> {
        return try {
            val info = getInstalledPackageInfo(context.packageManager, context.packageName)
            val code = info?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 1L
            val name = info?.versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
            code to name
        } catch (_: Exception) {
            1L to "1.0.0"
        }
    }

    private fun hasZipMagicHeader(file: File): Boolean {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4)
                val read = raf.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() && // 'P'
                    header[1] == 0x4B.toByte() && // 'K'
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfo(pm: PackageManager, apkPath: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkPath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(pm: PackageManager, packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSigningCertSha256Set(packageInfo: PackageInfo): Set<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures ?: emptyArray()
        }

        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.reset()
            md.digest(sig.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
        }.toSet()
    }
}
