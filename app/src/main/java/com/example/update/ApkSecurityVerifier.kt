package com.example.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
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
 * - HTTPS-only URLs pinned to the official LifeHub GitHub Releases repository (and GitHub's signed CDN redirects)
 * - Port restriction (default HTTPS 443 only), no embedded credentials, no path traversal
 * - File confinement inside app-private cache/updates directory
 * - Valid ZIP/APK header, size bounds, and optional SHA-256 checksum verification
 * - Exact package name match, strict anti-downgrade versionCode check, and SHA-256 signing
 *   certificate match against the currently installed application.
 */
object ApkSecurityVerifier {

    private const val MIN_APK_BYTES = 50L * 1024L // 50 KB
    private const val MAX_APK_BYTES = 250L * 1024L * 1024L // 250 MB

    /**
     * Pinned official GitHub repository prefix (lowercase) to prevent arbitrary-repo hijacking
     * even if Remote Config is misconfigured or manipulated.
     */
    const val TRUSTED_GITHUB_REPO_PATH_PREFIX = "/bharathkrishna-76/lifehub/"
    private const val TRUSTED_RELEASES_DOWNLOAD_PREFIX =
        "${TRUSTED_GITHUB_REPO_PATH_PREFIX}releases/download/"
    private const val TRUSTED_RELEASES_LATEST_PATH =
        "${TRUSTED_GITHUB_REPO_PATH_PREFIX}releases/latest"

    private val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")

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

        if (uri.port != -1 && uri.port != 443) {
            return UrlValidationResult.Invalid("Download URL must use standard HTTPS port 443.")
        }

        if (!uri.userInfo.isNullOrEmpty()) {
            return UrlValidationResult.Invalid("Download URL must not contain embedded credentials.")
        }

        val host = uri.host?.lowercase(Locale.US)?.trimEnd('.')
            ?: return UrlValidationResult.Invalid("Download URL is missing a valid host.")

        val allowedHosts = if (isRedirectHop) TRUSTED_REDIRECT_HOSTS else TRUSTED_INITIAL_HOSTS
        if (host !in allowedHosts) {
            return UrlValidationResult.Invalid(
                "Blocked untrusted download host ($host). Updates must be hosted on official GitHub Releases."
            )
        }

        val rawPath = uri.rawPath.orEmpty()
        val path = uri.path.orEmpty()
        if (path.contains("..") || rawPath.contains("%2e", ignoreCase = true) || path.contains("\\")) {
            return UrlValidationResult.Invalid("Download URL contains an unsafe path.")
        }

        if (!isRedirectHop) {
            val lowerPath = path.lowercase(Locale.US)
            if (!lowerPath.startsWith(TRUSTED_RELEASES_DOWNLOAD_PREFIX) || !lowerPath.endsWith(".apk")) {
                return UrlValidationResult.Invalid(
                    "Download link must point directly to an official LifeHub .apk asset on GitHub Releases."
                )
            }
        }

        return UrlValidationResult.Valid(trimmed)
    }

    /**
     * Validates URLs opened externally from the Refer This App card so only official HTTPS
     * LifeHub GitHub Releases links can be launched.
     */
    fun validateReferUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (validateDownloadUrl(trimmed, isRedirectHop = false) is UrlValidationResult.Valid) {
            return true
        }
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)?.trimEnd('.')
        val path = uri.path.orEmpty().lowercase(Locale.US).trimEnd('/')
        return scheme == "https" &&
            (uri.port == -1 || uri.port == 443) &&
            uri.userInfo.isNullOrEmpty() &&
            host == "github.com" &&
            path == TRUSTED_RELEASES_LATEST_PATH
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

    fun computeFileSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyDownloadedApk(
        context: Context,
        apkFile: File,
        installedVersionCode: Long,
        expectedSha256: String = "",
        expectedMinVersionCode: Long? = null
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

        // Optional cryptographic SHA-256 digest verification when published by CI/Remote Config
        val cleanExpectedHash = expectedSha256.trim().lowercase(Locale.US)
        if (cleanExpectedHash.isNotEmpty()) {
            if (!SHA256_HEX_REGEX.matches(cleanExpectedHash)) {
                return ApkVerificationResult.Rejected("Update integrity hash format is invalid.")
            }
            val actualHash = try {
                computeFileSha256(apkFile)
            } catch (_: Exception) {
                return ApkVerificationResult.Rejected("Could not compute update file checksum.")
            }
            val match = MessageDigest.isEqual(
                actualHash.toByteArray(Charsets.US_ASCII),
                cleanExpectedHash.toByteArray(Charsets.US_ASCII)
            )
            if (!match) {
                return ApkVerificationResult.Rejected(
                    "Security check failed: downloaded APK checksum does not match the official release."
                )
            }
        }

        val pm = context.packageManager
        val archiveInfo = getArchivePackageInfo(pm, apkFile.absolutePath)
            ?: return ApkVerificationResult.Rejected("Unable to read downloaded APK. The file may be corrupted.")

        // 1. Exact package name verification
        if (archiveInfo.packageName != context.packageName) {
            return ApkVerificationResult.Rejected(
                "Security check failed: APK package does not match this app."
            )
        }

        // 2. Anti-downgrade & expected target version check
        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode <= installedVersionCode) {
            return ApkVerificationResult.Rejected(
                "Downloaded APK (build $archiveVersionCode) is not newer than your installed version (build $installedVersionCode)."
            )
        }
        if (expectedMinVersionCode != null && archiveVersionCode < expectedMinVersionCode) {
            return ApkVerificationResult.Rejected(
                "Downloaded APK (build $archiveVersionCode) does not match the announced release build ($expectedMinVersionCode)."
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
