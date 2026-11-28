package com.example.update

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AdaptivePairLayout
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.LocalLifeHubColors

private const val DEFAULT_APK_RELEASES_URL =
    "https://github.com/Bharathkrishna-76/LifeHub/releases/latest"

/**
 * Resolves the freshest direct HTTPS APK download link from Firebase Remote Config,
 * falling back to the public latest GitHub Releases page if Remote Config has not synced yet.
 */
fun resolveReferApkDownloadUrl(config: RemoteUpdateConfig?): String {
    val remoteUrl = config?.apkDownloadUrl?.trim().orEmpty()
    if (remoteUrl.isNotEmpty() &&
        ApkSecurityVerifier.validateDownloadUrl(remoteUrl) is UrlValidationResult.Valid
    ) {
        return remoteUrl
    }
    return DEFAULT_APK_RELEASES_URL
}

/**
 * Material 3 "Refer This App" section for Profile & Settings allowing users to share,
 * copy, or test the direct APK download link so friends can download and install LifeHub.
 */
@Composable
fun ReferAppCard(
    updateManager: AppUpdateManager,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateState by updateManager.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current

    val downloadUrl = remember(updateState.config?.apkDownloadUrl) {
        resolveReferApkDownloadUrl(updateState.config)
    }

    val copiedToast = stringResource(R.string.refer_app_copied_toast)
    val noBrowserToast = stringResource(R.string.refer_app_no_browser_toast)
    val shareChooserTitle = stringResource(R.string.refer_app_chooser_title)
    val shareSubject = stringResource(R.string.refer_app_share_subject)
    val shareBody = stringResource(R.string.refer_app_share_message, downloadUrl)

    fun copyDownloadLink() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("LifeHub APK Link", downloadUrl))
        onShowMessage(copiedToast)
    }

    fun shareDownloadLink() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(shareIntent, shareChooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
            .onFailure { copyDownloadLink() }
    }

    fun openDownloadLink() {
        if (!ApkSecurityVerifier.validateReferUrl(downloadUrl)) {
            onShowMessage(noBrowserToast)
            return
        }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(viewIntent) }
            .onFailure { onShowMessage(noBrowserToast) }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, extended.accentMint.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("refer_app_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = extended.accentMint.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CardGiftcard,
                            contentDescription = stringResource(R.string.refer_app_icon_cd),
                            tint = extended.accentMint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.refer_app_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.refer_app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.mutedText
                    )
                }
            }

            // Clickable direct APK download URL pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { copyDownloadLink() }
                    .testTag("refer_app_link_preview")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = extended.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = downloadUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = { shareDownloadLink() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("refer_app_share_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.refer_app_share_button),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
                AdaptivePairLayout(
                    stackVertically = metrics.shouldStackTightPairs,
                    horizontalSpacing = 10.dp,
                    verticalSpacing = 10.dp,
                    first = { btnMod ->
                        OutlinedButton(
                            onClick = { copyDownloadLink() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = btnMod
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("refer_app_copy_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.refer_app_copy_button),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    second = { btnMod ->
                        OutlinedButton(
                            onClick = { openDownloadLink() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = btnMod
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("refer_app_open_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.refer_app_open_button),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
            }
        }
    }
}
