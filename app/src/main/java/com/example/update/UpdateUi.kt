package com.example.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.LocalLifeHubColors
import java.util.Locale

/**
 * Attaches a lifecycle observer that checks Firebase Remote Config on launch and throttled
 * foreground resumes, and automatically resumes APK installation after granting unknown-sources permission.
 */
@Composable
fun AppUpdateLifecycleEffect(
    updateManager: AppUpdateManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, updateManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                updateManager.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Renders either the mandatory blocking update screen or the optional Material 3 update dialog.
 */
@Composable
fun AppUpdateHost(
    updateManager: AppUpdateManager,
    state: UpdateUiState
) {
    if (state.isMandatoryBlocking) {
        MandatoryUpdateFullScreen(
            state = state,
            onDownloadOrUpdate = { updateManager.startDownloadAndVerify() },
            onInstall = { updateManager.installVerifiedApk() },
            onOpenPermissionSettings = { updateManager.openInstallPermissionSettings() },
            onCancelDownload = { updateManager.cancelDownload() },
            onRecheckConfig = { updateManager.checkForUpdates(manual = true) }
        )
    } else if (state.isOptionalPromptVisible) {
        OptionalUpdateDialog(
            state = state,
            onDownload = { updateManager.startDownloadAndVerify() },
            onInstall = { updateManager.installVerifiedApk() },
            onOpenPermissionSettings = { updateManager.openInstallPermissionSettings() },
            onCancelDownload = { updateManager.cancelDownload() },
            onRemindLater = { updateManager.remindMeLater() }
        )
    }
}

@Composable
private fun OptionalUpdateDialog(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onRemindLater: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isBusy = state.phase is UpdatePhase.Downloading || state.phase is UpdatePhase.Verifying

    Dialog(
        onDismissRequest = { if (!isBusy) onRemindLater() },
        properties = DialogProperties(
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = !isBusy,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 440.dp)
                .testTag("optional_update_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
            tonalElevation = 6.dp
        ) {
            UpdateCardContent(
                state = state,
                isMandatory = false,
                onDownload = onDownload,
                onInstall = onInstall,
                onOpenPermissionSettings = onOpenPermissionSettings,
                onCancelDownload = onCancelDownload,
                onRemindLater = onRemindLater,
                onRecheckConfig = null,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            )
        }
    }
}

@Composable
private fun MandatoryUpdateFullScreen(
    state: UpdateUiState,
    onDownloadOrUpdate: () -> Unit,
    onInstall: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onRecheckConfig: () -> Unit
) {
    // Prevent dismissing a mandatory update with the system Back button
    BackHandler(enabled = true) {}

    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mandatory_update_screen"),
        color = cs.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(26.dp),
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
                tonalElevation = 4.dp
            ) {
                UpdateCardContent(
                    state = state,
                    isMandatory = true,
                    onDownload = onDownloadOrUpdate,
                    onInstall = onInstall,
                    onOpenPermissionSettings = onOpenPermissionSettings,
                    onCancelDownload = onCancelDownload,
                    onRemindLater = null,
                    onRecheckConfig = onRecheckConfig,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun UpdateCardContent(
    state: UpdateUiState,
    isMandatory: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onRemindLater: (() -> Unit)?,
    onRecheckConfig: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current
    val config = state.config ?: return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header badge + app name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isMandatory) listOf(cs.error, extended.accentAmber)
                            else listOf(cs.primary, extended.accentCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = stringResource(R.string.update_icon_cd),
                    tint = cs.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_app_display_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        if (isMandatory) R.string.update_mandatory_title else R.string.update_optional_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            }
        }

        Text(
            text = stringResource(
                if (isMandatory) R.string.update_mandatory_subtitle else R.string.update_optional_subtitle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        // Installed vs Latest version strip
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.update_current_version_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        text = "v${state.installedVersionName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.update_latest_version_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "v${config.latestVersionName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
            }
        }

        // Release notes box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceContainerHigh.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_release_notes_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
                Text(
                    text = config.releaseNotes.ifBlank {
                        stringResource(R.string.update_release_notes_fallback)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        }

        // Phase-specific status / progress / error banner
        when (val phase = state.phase) {
            is UpdatePhase.Downloading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.update_status_downloading),
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        val progressLabel = buildString {
                            if (phase.progress != null) {
                                append("${(phase.progress * 100).toInt()}% • ")
                            }
                            append(formatMegabytes(phase.downloadedBytes))
                            if (phase.totalBytes > 0L) {
                                append(" / ${formatMegabytes(phase.totalBytes)}")
                            }
                        }
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (phase.progress != null) {
                        LinearProgressIndicator(
                            progress = { phase.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = cs.primary,
                            trackColor = cs.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = cs.primary,
                            trackColor = cs.surfaceVariant
                        )
                    }
                }
            }

            is UpdatePhase.Verifying -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = cs.primary
                    )
                    Text(
                        text = stringResource(R.string.update_status_verifying),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            is UpdatePhase.ReadyToInstall -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.update_status_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            is UpdatePhase.PermissionRequired -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = cs.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.update_status_permission_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSecondaryContainer
                        )
                    }
                }
            }

            is UpdatePhase.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = cs.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = phase.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onErrorContainer
                        )
                    }
                }
            }

            else -> Unit
        }

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (val phase = state.phase) {
                is UpdatePhase.Downloading -> {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_cancel_download_button")
                    ) {
                        Text(stringResource(R.string.update_btn_cancel_download))
                    }
                }

                is UpdatePhase.Verifying -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.update_status_verifying))
                    }
                }

                is UpdatePhase.ReadyToInstall -> {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_install_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.update_btn_install),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is UpdatePhase.PermissionRequired -> {
                    Button(
                        onClick = onOpenPermissionSettings,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_allow_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.update_btn_allow_install),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onInstall,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_retry_install_after_permission_button")
                    ) {
                        Text(stringResource(R.string.update_btn_install))
                    }
                }

                is UpdatePhase.Error -> {
                    Button(
                        onClick = if (phase.canRetryInstall) onInstall else onDownload,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_retry_button")
                    ) {
                        Text(
                            text = stringResource(
                                if (phase.canRetryInstall) R.string.update_btn_install else R.string.update_btn_retry
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isMandatory && onRecheckConfig != null) {
                        TextButton(
                            onClick = onRecheckConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("update_recheck_config_button")
                        ) {
                            Text(stringResource(R.string.update_settings_check_button))
                        }
                    }
                }

                else -> {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isMandatory) R.string.update_btn_update_now else R.string.update_btn_download
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isMandatory && onRemindLater != null && state.phase !is UpdatePhase.Downloading && state.phase !is UpdatePhase.Verifying) {
                TextButton(
                    onClick = onRemindLater,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("update_remind_later_button")
                ) {
                    Text(
                        text = stringResource(R.string.update_btn_remind_later),
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // Data safety reassurance footnote
        Text(
            text = stringResource(R.string.update_data_safe_note),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Clean, non-intrusive App Updates card for Profile & Settings.
 */
@Composable
fun AppUpdateSettingsCard(
    updateManager: AppUpdateManager,
    modifier: Modifier = Modifier
) {
    val state by updateManager.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = stringResource(R.string.update_icon_cd),
                    tint = extended.accentMint
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_settings_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.update_settings_version_format,
                            state.installedVersionName,
                            state.installedVersionCode
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.mutedText
                    )
                }
            }

            state.manualStatusMessage?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary
                )
            }

            OutlinedButton(
                onClick = { updateManager.checkForUpdates(manual = true) },
                enabled = !state.isManualChecking,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_check_updates_button")
            ) {
                if (state.isManualChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.update_status_checking))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.update_settings_check_button))
                }
            }
        }
    }
}

private fun formatMegabytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
