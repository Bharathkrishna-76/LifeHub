package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOutUser
import com.example.data.export.ExportAndShareHelper
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionItem
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.BackArrowAction
import com.example.ui.components.Extruded3DButton
import com.example.ui.components.FrostedGlassPanel
import com.example.ui.components.ShareActionButton
import com.example.ui.components.SpinningRefreshAction
import com.example.ui.components.rememberResponsiveMetrics
import com.example.LifeHubApplication
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.viewmodel.LifeHubUiState
import com.example.update.AppUpdateSettingsCard
import com.example.update.ReferAppCard
import kotlinx.coroutines.launch

/**
 * Module 10: Profile & Settings Studio.
 * Frosted Glass identity banner, tactile 3D save button, spinning sync refresh, CSV export, and Sign Out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: LifeHubUiState,
    onBack: () -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateBudgetAndCurrency: (Double, String) -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onUpdateSeparateTripMode: (Boolean) -> Unit,
    onForceRefresh: () -> Unit,
    onFullCloudResync: () -> Unit,
    onFetchTransactionsForExport: suspend () -> List<TransactionItem>,
    onShowMessage: (String) -> Unit,
    onBeforeSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current

    var budgetInput by remember(state.settings.monthlyBudget) {
        mutableStateOf(state.settings.monthlyBudget.toInt().toString())
    }
    var selectedCurrency by remember(state.settings.currencySymbol) {
        mutableStateOf(state.settings.currencySymbol)
    }
    val currencies = listOf("₹", "$", "€", "£")

    fun exportCsvNow() {
        scope.launch {
            val allTx = onFetchTransactionsForExport()
            if (allTx.isEmpty()) {
                onShowMessage("No cached transactions to export yet.")
            } else {
                val res = ExportAndShareHelper.exportTransactionsCsv(
                    context,
                    allTx,
                    state.settings.currencySymbol
                )
                if (res.isFailure) {
                    onShowMessage("Export failed: ${res.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Settings",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    BackArrowAction(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back")
                    )
                },
                actions = {
                    SpinningRefreshAction(
                        isSpinning = state.isSyncingMonth,
                        onClick = onForceRefresh
                    )
                    ShareActionButton(onClick = { exportCsvNow() })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("profile_screen"),
                contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Frosted Glass Identity Card
            item(key = "user_card") {
                FrostedGlassPanel(
                    accentGlow = NeonMint,
                    cornerRadius = 26.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initials = state.profile.displayName
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString("")
                            .ifBlank { "LH" }
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonMint, CyberViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.profile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.profile.email.isNotBlank()) {
                                Text(
                                    text = state.profile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Level ${state.playerLevel} • ${state.totalXp} XP • Money Score ${state.moneyScore.totalScore}",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.accentMint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Theme Settings
            item(key = "theme_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = extended.accentViolet)
                            Spacer(Modifier.width(10.dp))
                            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = state.settings.themeMode == mode,
                                    onClick = { onUpdateTheme(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                                    modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Budget & Currency Preferences
            item(key = "budget_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Budget & Currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currencies.forEach { sym ->
                                FilterChip(
                                    selected = selectedCurrency == sym,
                                    onClick = { selectedCurrency = sym },
                                    label = { Text(sym) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { if (it.all { c -> c.isDigit() }) budgetInput = it.take(9) },
                            label = { Text("Monthly Budget Target ($selectedCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Extruded3DButton(
                            label = "Save Preferences",
                            topColor = NeonMint,
                            sideColor = Color(0xFF068258),
                            contentColor = Color(0xFF042015),
                            onClick = {
                                val parsed = budgetInput.toDoubleOrNull() ?: 25000.0
                                onUpdateBudgetAndCurrency(parsed, selectedCurrency)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Notifications + Data Export + Sync
            item(key = "data_tools") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cs.surface,
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = extended.accentAmber)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Reminder Notifications", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Local WorkManager bill & EMI alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.mutedText
                                    )
                                }
                            }
                            Switch(
                                checked = state.settings.notificationsEnabled,
                                onCheckedChange = onUpdateNotifications
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, tint = extended.accentCyan)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Separate Trip & Event Ledger", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Exclude trip/event expenses from daily budget & totals",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.mutedText
                                    )
                                }
                            }
                            Switch(
                                checked = state.settings.separateTripEventLedger,
                                onCheckedChange = onUpdateSeparateTripMode,
                                modifier = Modifier.testTag("separate_trip_ledger_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = { exportCsvNow() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export Data (CSV)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        val lastSyncLabel = if (state.lastSyncedEpochMs > 0L) {
                            DateAndFormatUtils.formatDateTime(state.lastSyncedEpochMs)
                        } else "Not synced yet"
                        val syncStatusText = buildString {
                            append(if (state.isOnline) "Online" else "Offline")
                            append(" • ${state.allCachedTransactionsCount} cached")
                            if (state.pendingSyncCount > 0) {
                                append(" • ${state.pendingSyncCount} pending upload")
                            }
                            append("\nLast sync: $lastSyncLabel")
                        }
                        Text(
                            text = syncStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.mutedText
                        )

                        OutlinedButton(
                            onClick = onForceRefresh,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("smart_delta_sync_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Smart Delta Sync", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onFullCloudResync,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("full_cloud_resync_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Full Cloud Re-sync", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Refer This App (Direct APK Download Link Sharing)
            item(key = "refer_app") {
                val updateManager = remember(context) {
                    (context.applicationContext as LifeHubApplication).container.appUpdateManager
                }
                ReferAppCard(
                    updateManager = updateManager,
                    onShowMessage = onShowMessage
                )
            }

            // App Updates (Firebase Remote Config + GitHub Releases)
            item(key = "app_updates") {
                val updateManager = remember(context) {
                    (context.applicationContext as LifeHubApplication).container.appUpdateManager
                }
                AppUpdateSettingsCard(updateManager = updateManager)
            }

            // Sign Out
            item(key = "sign_out") {
                Button(
                    onClick = {
                        onBeforeSignOut()
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.errorContainer,
                        contentColor = cs.onErrorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
