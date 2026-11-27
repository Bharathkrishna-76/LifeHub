package com.example.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed interface ApkDownloadResult {
    data class Success(val apkFile: File) : ApkDownloadResult
    data class Failure(val message: String) : ApkDownloadResult
}

/**
 * Streams an APK securely over HTTPS into the app-private cache/updates/ directory.
 * Manually inspects every redirect hop against ApkSecurityVerifier so downloads cannot
 * be redirected to HTTP or untrusted hosts.
 */
class ApkDownloader(private val context: Context) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_REDIRECTS = 5
        private const val MAX_APK_BYTES = 250L * 1024L * 1024L // 250 MB
        private const val BUFFER_SIZE = 16 * 1024
    }

    suspend fun downloadApk(
        downloadUrl: String,
        targetVersionCode: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long, progress: Float?) -> Unit
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        val initialValidation = ApkSecurityVerifier.validateDownloadUrl(downloadUrl, isRedirectHop = false)
        if (initialValidation is UrlValidationResult.Invalid) {
            return@withContext ApkDownloadResult.Failure(initialValidation.reason)
        }

        val updatesDir = ApkSecurityVerifier.getUpdatesDirectory(context)
        val safeFileName = "lifehub-update-$targetVersionCode.apk"
        val finalFile = File(updatesDir, safeFileName)
        val partFile = File(updatesDir, "$safeFileName.part")

        if (!ApkSecurityVerifier.isConfinedToUpdatesDir(context, finalFile) ||
            !ApkSecurityVerifier.isConfinedToUpdatesDir(context, partFile)
        ) {
            return@withContext ApkDownloadResult.Failure("Invalid local download destination.")
        }

        var connection: HttpsURLConnection? = null
        try {
            if (partFile.exists()) partFile.delete()

            connection = openValidatedHttpsConnection((initialValidation as UrlValidationResult.Valid).normalizedUrl)
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_APK_BYTES) {
                return@withContext ApkDownloadResult.Failure("Update package exceeds maximum allowed size.")
            }

            var downloaded = 0L
            var lastReportedBytes = 0L
            onProgress(0L, contentLength, if (contentLength > 0L) 0f else null)

            BufferedInputStream(connection.inputStream, BUFFER_SIZE).use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) {
                            throw IOException("Update download exceeded maximum allowed size.")
                        }
                        output.write(buffer, 0, read)

                        if (downloaded - lastReportedBytes >= 64 * 1024 || downloaded == contentLength) {
                            lastReportedBytes = downloaded
                            val fraction = if (contentLength > 0L) {
                                (downloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else null
                            onProgress(downloaded, contentLength, fraction)
                        }
                    }
                    output.fd.sync()
                }
            }

            if (contentLength > 0L && downloaded != contentLength) {
                partFile.delete()
                return@withContext ApkDownloadResult.Failure(
                    "Download was interrupted before completion. Please try again."
                )
            }

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            onProgress(downloaded, if (contentLength > 0L) contentLength else downloaded, 1f)
            ApkDownloadResult.Success(finalFile)
        } catch (ce: CancellationException) {
            partFile.delete()
            throw ce
        } catch (e: IOException) {
            partFile.delete()
            ApkDownloadResult.Failure(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Network error while downloading the update. Check your connection and retry."
            )
        } catch (e: SecurityException) {
            partFile.delete()
            ApkDownloadResult.Failure(e.message ?: "Blocked unsafe download response.")
        } catch (_: Exception) {
            partFile.delete()
            ApkDownloadResult.Failure("Could not download the update. Please try again.")
        } finally {
            connection?.disconnect()
        }
    }

    fun cleanupOldApks(installedVersionCode: Long) {
        runCatching {
            val updatesDir = ApkSecurityVerifier.getUpdatesDirectory(context)
            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".part")) {
                    file.delete()
                } else if (file.name.startsWith("lifehub-update-") && file.name.endsWith(".apk")) {
                    val codePart = file.name
                        .removePrefix("lifehub-update-")
                        .removeSuffix(".apk")
                        .toLongOrNull()
                    if (codePart == null || codePart <= installedVersionCode) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun openValidatedHttpsConnection(initialUrl: String): HttpsURLConnection {
        var currentUrl = initialUrl
        for (hop in 0..MAX_REDIRECTS) {
            val url = URL(currentUrl)
            val rawConn = url.openConnection()
            if (rawConn !is HttpsURLConnection) {
                throw SecurityException("Only HTTPS connections are allowed for app updates.")
            }

            rawConn.instanceFollowRedirects = false
            rawConn.connectTimeout = CONNECT_TIMEOUT_MS
            rawConn.readTimeout = READ_TIMEOUT_MS
            rawConn.requestMethod = "GET"
            rawConn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream")
            rawConn.connect()

            val code = rawConn.responseCode
            if (code in 300..399) {
                val location = rawConn.getHeaderField("Location")
                rawConn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Server returned redirect without destination.")
                }
                val resolvedUrl = URL(url, location).toString()
                val hopValidation = ApkSecurityVerifier.validateDownloadUrl(resolvedUrl, isRedirectHop = true)
                if (hopValidation is UrlValidationResult.Invalid) {
                    throw SecurityException(hopValidation.reason)
                }
                currentUrl = (hopValidation as UrlValidationResult.Valid).normalizedUrl
                continue
            }

            if (code != HttpURLConnection.HTTP_OK) {
                rawConn.disconnect()
                throw IOException("Update server returned HTTP $code. Check the GitHub Release APK link.")
            }

            return rawConn
        }
        throw IOException("Too many redirects while downloading update.")
    }
}
