package com.example.update

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Centralized Automatic APK Update coordinator backed by Firebase Remote Config (Spark-tier efficient)
 * and GitHub Releases HTTPS APK downloads.
 */
class AppUpdateManager(context: Context) {

    companion object {
        // Firebase Remote Config SDK throttle for automatic checks (1 hour)
        private const val REMOTE_CONFIG_MIN_FETCH_INTERVAL_SECONDS = 3600L
        private const val REMOTE_CONFIG_FETCH_TIMEOUT_SECONDS = 12L
        private const val COROUTINE_FETCH_TIMEOUT_MS = 15_000L

        // Minimum interval between automatic foreground re-checks (4 hours)
        private const val FOREGROUND_RECHECK_INTERVAL_MS = 4L * 60L * 60L * 1000L

        // Cooldown for "Remind Me Later" on optional updates (12 hours)
        private const val REMIND_LATER_COOLDOWN_MS = 12L * 60L * 60L * 1000L

        // Cooldown between manual Settings checks (60 seconds) to prevent Firebase quota waste
        private const val MANUAL_CHECK_COOLDOWN_MS = 60L * 1000L
    }

    private val appContext = context.applicationContext
    private val cacheStore = UpdateCacheStore(appContext)
    private val downloader = ApkDownloader(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val installedVersion: Pair<Long, String> = ApkSecurityVerifier.getInstalledVersion(appContext)
    private val installedVersionCode: Long = installedVersion.first
    private val installedVersionName: String = installedVersion.second

    private val _uiState = MutableStateFlow(
        UpdateUiState(
            installedVersionCode = installedVersionCode,
            installedVersionName = installedVersionName
        )
    )
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    @Volatile
    private var remoteConfigInitialized = false
    private var hasCheckedThisSession = false
    private var dismissedInCurrentSession = false
    private var lastAutomaticCheckMs = 0L
    private var lastManualCheckMs = 0L
    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    init {
        scope.launch(Dispatchers.IO) {
            downloader.cleanupOldApks(installedVersionCode)
        }
    }

    private fun getOrInitRemoteConfig(): FirebaseRemoteConfig? {
        return try {
            val rc = Firebase.remoteConfig
            if (!remoteConfigInitialized) {
                val settings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = REMOTE_CONFIG_MIN_FETCH_INTERVAL_SECONDS
                    fetchTimeoutInSeconds = REMOTE_CONFIG_FETCH_TIMEOUT_SECONDS
                }
                rc.setConfigSettingsAsync(settings)
                rc.setDefaultsAsync(
                    mapOf(
                        RemoteUpdateKeys.LATEST_VERSION_CODE to installedVersionCode,
                        RemoteUpdateKeys.LATEST_VERSION_NAME to installedVersionName,
                        RemoteUpdateKeys.APK_DOWNLOAD_URL to "",
                        RemoteUpdateKeys.RELEASE_NOTES to "",
                        RemoteUpdateKeys.UPDATE_ENABLED to false,
                        RemoteUpdateKeys.MINIMUM_VERSION_CODE to 1L
                    )
                )
                remoteConfigInitialized = true
            }
            rc
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Called when the activity enters ON_START (app launch and returning from background).
     * Automatically resumes installation if the user just granted unknown-sources permission,
     * and performs a throttled Remote Config update check without wasting Firebase quota.
     */
    fun onAppForegrounded() {
        val currentPhase = _uiState.value.phase
        if (currentPhase is UpdatePhase.PermissionRequired) {
            if (ApkInstaller.canInstallPackages(appContext)) {
                _uiState.update { it.copy(phase = UpdatePhase.ReadyToInstall(currentPhase.apkFile)) }
                installVerifiedApk()
            }
            return
        }

        // Do not interrupt active download/verification/ready state
        if (currentPhase is UpdatePhase.Downloading ||
            currentPhase is UpdatePhase.Verifying ||
            currentPhase is UpdatePhase.ReadyToInstall
        ) {
            return
        }

        val now = System.currentTimeMillis()
        if (!hasCheckedThisSession) {
            hasCheckedThisSession = true
            lastAutomaticCheckMs = now
            checkForUpdates(manual = false)
        } else if (now - lastAutomaticCheckMs >= FOREGROUND_RECHECK_INTERVAL_MS) {
            lastAutomaticCheckMs = now
            checkForUpdates(manual = false)
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        val currentPhase = _uiState.value.phase
        if (currentPhase is UpdatePhase.Downloading || currentPhase is UpdatePhase.Verifying) {
            return
        }
        if (checkJob?.isActive == true) return

        val now = System.currentTimeMillis()
        if (manual && lastManualCheckMs > 0L && now - lastManualCheckMs < MANUAL_CHECK_COOLDOWN_MS) {
            // Re-evaluate already cached state quickly instead of hitting Firebase repeatedly
            scope.launch {
                val cached = cacheStore.loadCachedConfig()
                if (cached != null) {
                    applyConfig(cached, manual = true)
                } else {
                    _uiState.update {
                        it.copy(
                            isManualChecking = false,
                            manualStatusMessage = "Already checked moments ago. You are up to date."
                        )
                    }
                }
            }
            return
        }

        if (manual) {
            lastManualCheckMs = now
            _uiState.update {
                it.copy(
                    isManualChecking = true,
                    manualStatusMessage = null
                )
            }
        }

        checkJob = scope.launch {
            val fetchedConfig = fetchRemoteConfig(forceFresh = manual)
            if (fetchedConfig != null) {
                cacheStore.saveCachedConfig(fetchedConfig)
                applyConfig(fetchedConfig, manual = manual)
            } else {
                // Network or Firebase error — fall back safely to cached config
                val cached = cacheStore.loadCachedConfig()
                if (cached != null) {
                    val evaluation = evaluateRequirement(cached)
                    if (evaluation == UpdateRequirement.MANDATORY) {
                        applyConfig(cached, manual = manual)
                    } else if (manual && evaluation == UpdateRequirement.OPTIONAL) {
                        applyConfig(cached, manual = true)
                    } else {
                        _uiState.update {
                            it.copy(
                                isManualChecking = false,
                                manualStatusMessage = if (manual) {
                                    "Could not reach update server. Check your internet connection and try again."
                                } else null
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isManualChecking = false,
                            manualStatusMessage = if (manual) {
                                "Could not check for updates right now. Please try again later."
                            } else null
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchRemoteConfig(forceFresh: Boolean): RemoteUpdateConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val rc = getOrInitRemoteConfig() ?: return@withContext null
                withTimeout(COROUTINE_FETCH_TIMEOUT_MS) {
                    if (forceFresh) {
                        // Manual button tap fetches fresh immediately (protected by Kotlin cooldown)
                        rc.fetch(0L).await()
                        rc.activate().await()
                    } else {
                        rc.fetchAndActivate().await()
                    }
                }

                RemoteUpdateConfig(
                    latestVersionCode = rc.getLong(RemoteUpdateKeys.LATEST_VERSION_CODE),
                    latestVersionName = rc.getString(RemoteUpdateKeys.LATEST_VERSION_NAME)
                        .trim()
                        .ifEmpty { installedVersionName },
                    apkDownloadUrl = rc.getString(RemoteUpdateKeys.APK_DOWNLOAD_URL).trim(),
                    releaseNotes = rc.getString(RemoteUpdateKeys.RELEASE_NOTES).trim(),
                    updateEnabled = rc.getBoolean(RemoteUpdateKeys.UPDATE_ENABLED),
                    minimumVersionCode = rc.getLong(RemoteUpdateKeys.MINIMUM_VERSION_CODE).coerceAtLeast(1L),
                    fetchedAtEpochMs = System.currentTimeMillis()
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun evaluateRequirement(config: RemoteUpdateConfig): UpdateRequirement {
        if (!config.updateEnabled) return UpdateRequirement.NONE
        if (config.latestVersionCode <= installedVersionCode) return UpdateRequirement.NONE

        // Never block users or trigger an update flow if the configured URL is missing or untrusted
        val urlValidation = ApkSecurityVerifier.validateDownloadUrl(config.apkDownloadUrl)
        if (urlValidation is UrlValidationResult.Invalid) return UpdateRequirement.NONE

        return if (installedVersionCode < config.minimumVersionCode) {
            UpdateRequirement.MANDATORY
        } else {
            UpdateRequirement.OPTIONAL
        }
    }

    private suspend fun applyConfig(config: RemoteUpdateConfig, manual: Boolean) {
        val requirement = evaluateRequirement(config)
        if (requirement == UpdateRequirement.NONE) {
            _uiState.update {
                it.copy(
                    config = config,
                    requirement = UpdateRequirement.NONE,
                    phase = UpdatePhase.Idle,
                    isDialogVisible = false,
                    isManualChecking = false,
                    manualStatusMessage = if (manual) "You are on the latest version ($installedVersionName)." else null
                )
            }
            return
        }

        // Check if we already have a verified APK downloaded for this version
        val existingApk = withContext(Dispatchers.IO) {
            findAlreadyVerifiedApk(config.latestVersionCode)
        }
        val initialPhase = if (existingApk != null) {
            UpdatePhase.ReadyToInstall(existingApk)
        } else {
            UpdatePhase.Available
        }

        val shouldShowOptionalDialog = if (requirement == UpdateRequirement.OPTIONAL) {
            if (manual) {
                true
            } else if (dismissedInCurrentSession) {
                false
            } else {
                val (dismissedVersion, dismissedAt) = cacheStore.loadRemindLater()
                val recentlyDismissedSameVersion = dismissedVersion == config.latestVersionCode &&
                    (System.currentTimeMillis() - dismissedAt) < REMIND_LATER_COOLDOWN_MS
                !recentlyDismissedSameVersion
            }
        } else {
            false
        }

        _uiState.update {
            it.copy(
                config = config,
                requirement = requirement,
                phase = initialPhase,
                isDialogVisible = shouldShowOptionalDialog,
                isManualChecking = false,
                manualStatusMessage = if (manual) {
                    "Update v${config.latestVersionName} is available."
                } else null
            )
        }
    }

    private fun findAlreadyVerifiedApk(targetVersionCode: Long): File? {
        val candidate = File(
            ApkSecurityVerifier.getUpdatesDirectory(appContext),
            "lifehub-update-$targetVersionCode.apk"
        )
        if (!candidate.exists()) return null
        val verification = ApkSecurityVerifier.verifyDownloadedApk(
            context = appContext,
            apkFile = candidate,
            installedVersionCode = installedVersionCode
        )
        return when (verification) {
            is ApkVerificationResult.Verified -> {
                if (verification.versionCode >= targetVersionCode) candidate else {
                    candidate.delete()
                    null
                }
            }
            is ApkVerificationResult.Rejected -> {
                candidate.delete()
                null
            }
        }
    }

    fun startDownloadAndVerify() {
        val config = _uiState.value.config ?: return
        if (downloadJob?.isActive == true) return

        // If already verified on disk, go straight to install
        val currentPhase = _uiState.value.phase
        if (currentPhase is UpdatePhase.ReadyToInstall && currentPhase.apkFile.exists()) {
            installVerifiedApk()
            return
        }

        downloadJob = scope.launch {
            _uiState.update {
                it.copy(phase = UpdatePhase.Downloading(downloadedBytes = 0L, totalBytes = -1L, progress = 0f))
            }

            val downloadResult = downloader.downloadApk(
                downloadUrl = config.apkDownloadUrl,
                targetVersionCode = config.latestVersionCode,
                onProgress = { downloaded, total, progress ->
                    _uiState.update { state ->
                        if (state.phase is UpdatePhase.Downloading) {
                            state.copy(phase = UpdatePhase.Downloading(downloaded, total, progress))
                        } else state
                    }
                }
            )

            when (downloadResult) {
                is ApkDownloadResult.Failure -> {
                    _uiState.update {
                        it.copy(phase = UpdatePhase.Error(message = downloadResult.message))
                    }
                }
                is ApkDownloadResult.Success -> {
                    _uiState.update { it.copy(phase = UpdatePhase.Verifying) }

                    val verification = withContext(Dispatchers.IO) {
                        ApkSecurityVerifier.verifyDownloadedApk(
                            context = appContext,
                            apkFile = downloadResult.apkFile,
                            installedVersionCode = installedVersionCode
                        )
                    }

                    when (verification) {
                        is ApkVerificationResult.Rejected -> {
                            withContext(Dispatchers.IO) { downloadResult.apkFile.delete() }
                            _uiState.update {
                                it.copy(phase = UpdatePhase.Error(message = verification.reason))
                            }
                        }
                        is ApkVerificationResult.Verified -> {
                            _uiState.update {
                                it.copy(phase = UpdatePhase.ReadyToInstall(downloadResult.apkFile))
                            }
                            installVerifiedApk()
                        }
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(phase = UpdatePhase.Available) }
    }

    fun installVerifiedApk() {
        val apkFile = when (val phase = _uiState.value.phase) {
            is UpdatePhase.ReadyToInstall -> phase.apkFile
            is UpdatePhase.PermissionRequired -> phase.apkFile
            is UpdatePhase.Error -> phase.verifiedApkFile
            else -> null
        } ?: run {
            startDownloadAndVerify()
            return
        }

        // Re-verify before launching Android's installer in case cache was cleared or modified
        scope.launch {
            val verification = withContext(Dispatchers.IO) {
                ApkSecurityVerifier.verifyDownloadedApk(appContext, apkFile, installedVersionCode)
            }
            if (verification is ApkVerificationResult.Rejected) {
                withContext(Dispatchers.IO) { apkFile.delete() }
                _uiState.update { it.copy(phase = UpdatePhase.Error(message = verification.reason)) }
                return@launch
            }

            when (val result = ApkInstaller.launchSystemInstaller(appContext, apkFile)) {
                InstallLaunchResult.Launched -> {
                    _uiState.update { it.copy(phase = UpdatePhase.ReadyToInstall(apkFile)) }
                }
                InstallLaunchResult.NeedsUnknownSourcesPermission -> {
                    _uiState.update { it.copy(phase = UpdatePhase.PermissionRequired(apkFile)) }
                }
                is InstallLaunchResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            phase = UpdatePhase.Error(
                                message = result.message,
                                canRetryInstall = apkFile.exists(),
                                verifiedApkFile = apkFile.takeIf { f -> f.exists() }
                            )
                        )
                    }
                }
            }
        }
    }

    fun openInstallPermissionSettings() {
        val opened = ApkInstaller.openInstallPermissionSettings(appContext)
        if (!opened) {
            val currentFile = (_uiState.value.phase as? UpdatePhase.PermissionRequired)?.apkFile
            _uiState.update {
                it.copy(
                    phase = UpdatePhase.Error(
                        message = "Please open Android Settings → Apps → Special app access → Install unknown apps, allow LifeHub, then tap Install Update.",
                        canRetryInstall = currentFile != null,
                        verifiedApkFile = currentFile
                    )
                )
            }
        }
    }

    fun remindMeLater() {
        val state = _uiState.value
        if (state.requirement == UpdateRequirement.MANDATORY) return

        downloadJob?.cancel()
        downloadJob = null
        dismissedInCurrentSession = true

        val targetCode = state.config?.latestVersionCode ?: 0L
        if (targetCode > 0L) {
            scope.launch(Dispatchers.IO) {
                cacheStore.recordRemindLater(targetCode, System.currentTimeMillis())
            }
        }

        _uiState.update {
            it.copy(
                isDialogVisible = false,
                phase = if (it.phase is UpdatePhase.ReadyToInstall) it.phase else UpdatePhase.Available
            )
        }
    }

    fun clearManualStatus() {
        _uiState.update { it.copy(manualStatusMessage = null) }
    }
}
