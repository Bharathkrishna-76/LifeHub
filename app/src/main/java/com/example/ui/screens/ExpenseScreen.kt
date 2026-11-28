package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportAndShareHelper
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.ChampagneGold
import com.example.ui.components.ChampagneGoldLight
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.FilterActionButton
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.PremiumDarkVaultCard
import com.example.ui.components.SavingActionButton
import com.example.ui.components.SearchAction
import com.example.ui.components.ShareActionButton
import com.example.ui.components.StealthCarbonElevated
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.viewmodel.LifeHubUiState
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Module 2: Money Hub — Adaptive Luxury Vault (Obsidian Carbon in Dark / Champagne Ivory in Light).
 * Houses 3 integrated ledgers with 0 extra Firestore reads:
 *   0. Daily — Daily Activity & Monthly/Yearly Reports (completely unaffected by Trip/Event spending)
 *   1. Trips — Separate Trip & Event Expense Tracker grouped by trip/event name
 *   2. Bills & EMI — Bill, EMI, Rent & Subscription reminders synced with WorkManager
 */
private enum class LocalDateRangeFilter(val label: String, val days: Int?) {
    ALL_MONTH("Full Month", null),
    TODAY("Today", 1),
    LAST_7_DAYS("7 Days", 7),
    LAST_14_DAYS("14 Days", 14)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    state: LifeHubUiState,
    initialSheetType: TransactionType? = null,
    initialSubTab: Int? = null,
    initialTripSheet: Boolean = false,
    onConsumeInitialSheet: () -> Unit = {},
    onShiftMonth: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadOlder: () -> Unit,
    onAddTransaction: (String, String, Double, TransactionType, ExpenseCategory, Long, String, Boolean, String, () -> Unit) -> Unit,
    onUpdateTransaction: (TransactionItem, String, Double, TransactionType, ExpenseCategory, String, Boolean, String, () -> Unit) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddReminder: (String, String, ReminderCategory, Double, Long, Boolean, () -> Unit) -> Unit,
    onToggleReminderPaid: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    val context = LocalContext.current
    val extended = LocalLifeHubColors.current
    val isDark = extended.isDark
    val symbol = state.settings.currencySymbol

    val palette = remember(isDark, extended) {
        VaultPalette(
            isDark = isDark,
            bg = if (isDark) Color(0xFF07080C) else Color(0xFFF9F6EF),
            ink = if (isDark) Color.White else Color(0xFF18140F),
            muted = if (isDark) Color(0xFF9BA6C1) else Color(0xFF5D574C),
            gold = if (isDark) ChampagneGold else ChampagneGoldLight,
            mint = extended.accentMint,
            coral = extended.accentCoral,
            rowBg = if (isDark) StealthCarbonElevated else Color.White,
            rowBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5DEC9),
            pillBg = if (isDark) Color(0xFF1D212C) else Color(0xFFF3ECE0)
        )
    }

    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Daily, 1 = Trips, 2 = Bills & EMI
    var searchVisible by remember { mutableStateOf(false) }
    var filtersVisible by remember { mutableStateOf(true) }
    var analyticsExpanded by remember { mutableStateOf(true) }
    var showYearlyReport by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var dateRangeFilter by remember { mutableStateOf(LocalDateRangeFilter.ALL_MONTH) }
    var selectedTripEventFilter by remember { mutableStateOf<String?>(null) }
    var selectedReminderCategory by remember { mutableStateOf<ReminderCategory?>(null) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showAddReminderSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var sheetDefaultType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var sheetDefaultTripMode by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Local reminders still persist cleanly even if notification permission is declined */ }

    LaunchedEffect(initialSheetType, initialSubTab, initialTripSheet) {
        var consumed = false
        if (initialSubTab != null) {
            activeSubTab = initialSubTab.coerceIn(0, 2)
            consumed = true
        }
        if (initialTripSheet) {
            activeSubTab = 1
            editingTransaction = null
            sheetDefaultType = TransactionType.EXPENSE
            sheetDefaultTripMode = true
            showAddSheet = true
            consumed = true
        } else if (initialSheetType != null) {
            activeSubTab = 0
            editingTransaction = null
            sheetDefaultType = initialSheetType
            sheetDefaultTripMode = false
            showAddSheet = true
            consumed = true
        }
        if (consumed) onConsumeInitialSheet()
    }

    val filteredDailyTransactions by remember(
        state.dailyTransactions,
        searchQuery,
        typeFilter,
        categoryFilter,
        dateRangeFilter
    ) {
        derivedStateOf {
            val nowMs = System.currentTimeMillis()
            val minEpoch = dateRangeFilter.days?.let { days ->
                nowMs - (days.toLong() * 86_400_000L)
            }
            state.dailyTransactions.filter { tx ->
                val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.label.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true)
                val matchesType = typeFilter == null || tx.type == typeFilter
                val matchesCat = categoryFilter == null || tx.category == categoryFilter
                val matchesDate = minEpoch == null || tx.timestampEpochMs >= minEpoch
                matchesSearch && matchesType && matchesCat && matchesDate
            }
        }
    }

    val categoryBreakdown by remember(filteredDailyTransactions) {
        derivedStateOf {
            val expenses = filteredDailyTransactions.filter { it.type == TransactionType.EXPENSE }
            val total = expenses.sumOf { it.amount }.coerceAtLeast(1.0)
            expenses.groupBy { it.category }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amount }
                    Triple(cat, sum, (sum / total).toFloat())
                }
                .sortedByDescending { it.second }
        }
    }

    val listState = rememberLazyListState()
    val shouldPaginate by remember(activeSubTab, state.hasMoreTransactions, state.isPaginating) {
        derivedStateOf {
            if (activeSubTab != 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            state.hasMoreTransactions && !state.isPaginating && totalItems > 10 && lastVisible >= totalItems - 4
        }
    }
    LaunchedEffect(shouldPaginate) {
        if (shouldPaginate) onLoadOlder()
    }

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeSubTab) {
                        2 -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showAddReminderSheet = true
                        }
                        1 -> {
                            editingTransaction = null
                            sheetDefaultType = TransactionType.EXPENSE
                            sheetDefaultTripMode = true
                            showAddSheet = true
                        }
                        else -> {
                            editingTransaction = null
                            sheetDefaultType = TransactionType.EXPENSE
                            sheetDefaultTripMode = false
                            showAddSheet = true
                        }
                    }
                },
                containerColor = palette.gold,
                contentColor = if (isDark) Color(0xFF141006) else Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = when (activeSubTab) {
                        1 -> "Add trip or event expense"
                        2 -> "Add bill or EMI reminder"
                        else -> "Add daily transaction"
                    }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

            PullToRefreshBox(
                isRefreshing = state.isSyncingMonth,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("expense_list"),
                    contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header + 3-Way Money Hub Sub-Tab Switcher (Daily | Trips | Bills & EMI)
                    item(key = "fintech_header") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isDark) "PRIVATE VAULT • MONEY HUB" else "CHAMPAGNE VAULT • MONEY HUB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.gold,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = when (activeSubTab) {
                                            1 -> "Trips & Events"
                                            2 -> "Bills, EMI & Subs"
                                            else -> "Private Ledger"
                                        },
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = palette.ink,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activeSubTab == 0) {
                                    SearchAction(
                                        active = searchVisible,
                                        tint = palette.ink,
                                        onClick = {
                                            searchVisible = !searchVisible
                                            if (!searchVisible) searchQuery = ""
                                        }
                                    )
                                    FilterActionButton(
                                        hasActiveFilter = typeFilter != null || categoryFilter != null || dateRangeFilter != LocalDateRangeFilter.ALL_MONTH,
                                        tint = palette.ink,
                                        onClick = { filtersVisible = !filtersVisible }
                                    )
                                }
                                ShareActionButton(
                                    tint = palette.gold,
                                    onClick = {
                                        if (state.transactions.isNotEmpty()) {
                                            ExportAndShareHelper.exportTransactionsCsv(context, state.transactions, symbol)
                                        }
                                    }
                                )
                                MoreOptionsAction(
                                    tint = palette.muted,
                                    options = listOf(
                                        "Add Daily Expense" to {
                                            editingTransaction = null
                                            sheetDefaultType = TransactionType.EXPENSE
                                            sheetDefaultTripMode = false
                                            showAddSheet = true
                                        },
                                        "Add Daily Income" to {
                                            editingTransaction = null
                                            sheetDefaultType = TransactionType.INCOME
                                            sheetDefaultTripMode = false
                                            showAddSheet = true
                                        },
                                        "Add Trip / Event Expense" to {
                                            editingTransaction = null
                                            sheetDefaultType = TransactionType.EXPENSE
                                            sheetDefaultTripMode = true
                                            showAddSheet = true
                                        },
                                        "Add EMI / Subscription" to {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            showAddReminderSheet = true
                                        },
                                        "Clear Filters" to {
                                            searchQuery = ""
                                            typeFilter = null
                                            categoryFilter = null
                                            dateRangeFilter = LocalDateRangeFilter.ALL_MONTH
                                            selectedTripEventFilter = null
                                            selectedReminderCategory = null
                                        }
                                    )
                                )
                            }
                        }

                        // 3-Way Segmented Hub Tabs: Daily | Trips | Bills & EMI
                        val unpaidCount = state.reminders.count { !it.paid }
                        val tripCount = state.allTripEventTransactions.size
                        val tabLabels = listOf(
                            "Daily",
                            if (tripCount > 0) "Trips ($tripCount)" else "Trips",
                            if (unpaidCount > 0) "Bills ($unpaidCount)" else "Bills & EMI"
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("money_subtab_row")
                        ) {
                            tabLabels.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = activeSubTab == index,
                                    onClick = { activeSubTab = index },
                                    shape = SegmentedButtonDefaults.itemShape(index, tabLabels.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = palette.gold.copy(alpha = if (isDark) 0.24f else 0.16f),
                                        activeContentColor = palette.gold,
                                        inactiveContainerColor = palette.rowBg,
                                        inactiveContentColor = palette.muted,
                                        activeBorderColor = palette.gold,
                                        inactiveBorderColor = palette.rowBorder
                                    ),
                                    modifier = Modifier.testTag("money_subtab_$index")
                                ) {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeSubTab) {
                    1 -> {
                        tripsAndEventsSection(
                            state = state,
                            palette = palette,
                            symbol = symbol,
                            selectedEventFilter = selectedTripEventFilter,
                            onSelectEventFilter = { selectedTripEventFilter = it },
                            onAddTripExpense = {
                                editingTransaction = null
                                sheetDefaultType = TransactionType.EXPENSE
                                sheetDefaultTripMode = true
                                showAddSheet = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                sheetDefaultType = tx.type
                                sheetDefaultTripMode = tx.isTripOrEvent
                                showAddSheet = true
                            },
                            onDeleteTransaction = onDeleteTransaction
                        )
                    }

                    2 -> {
                        billsAndEmiSection(
                            state = state,
                            palette = palette,
                            symbol = symbol,
                            selectedCategory = selectedReminderCategory,
                            onSelectCategory = { selectedReminderCategory = it },
                            onAddReminderClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                showAddReminderSheet = true
                            },
                            onTogglePaid = onToggleReminderPaid,
                            onDeleteReminder = onDeleteReminder
                        )
                    }

                    else -> {
                        // TAB 0: DAILY ACTIVITY VAULT
                        item(key = "daily_vault_card") {
                            PremiumDarkVaultCard(modifier = Modifier.fillMaxWidth()) {
                                val periodLabel: @Composable (Modifier) -> Unit = { labelMod ->
                                    Column(modifier = labelMod) {
                                        Text(
                                            text = "STATEMENT PERIOD",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.gold.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (state.settings.separateTripEventLedger && state.allTripEventTransactions.isNotEmpty()) {
                                            Text(
                                                text = "Daily only • Trips excluded",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.muted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                val monthStepper: @Composable () -> Unit = {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = palette.pillBg,
                                        border = BorderStroke(1.dp, palette.gold.copy(alpha = 0.35f))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onShiftMonth(-1) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("month_prev_button")
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                                    contentDescription = "Previous month",
                                                    tint = palette.gold
                                                )
                                            }
                                            Text(
                                                text = DateAndFormatUtils.formatMonthLabel(state.selectedMonthKey),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                color = palette.ink,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            IconButton(
                                                onClick = { onShiftMonth(1) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("month_next_button")
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = "Next month",
                                                    tint = palette.gold
                                                )
                                            }
                                        }
                                    }
                                }

                                if (metrics.shouldStackTightPairs) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        periodLabel(Modifier.fillMaxWidth())
                                        monthStepper()
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        periodLabel(Modifier.weight(1f))
                                        Spacer(Modifier.width(8.dp))
                                        monthStepper()
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LedgerMetricColumn(
                                        label = "INFLOW",
                                        value = DateAndFormatUtils.formatCurrency(state.totalIncome, symbol, compact = true),
                                        color = palette.mint,
                                        mutedColor = palette.muted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LedgerMetricColumn(
                                        label = "OUTFLOW",
                                        value = DateAndFormatUtils.formatCurrency(state.totalExpense, symbol, compact = true),
                                        color = palette.coral,
                                        mutedColor = palette.muted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LedgerMetricColumn(
                                        label = "NET VAULT",
                                        value = DateAndFormatUtils.formatCurrency(state.netBalance, symbol, compact = true),
                                        color = palette.gold,
                                        mutedColor = palette.muted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Expandable/Collapsible Monthly & Yearly Local Reports Card (0 Firestore reads!)
                        if (categoryBreakdown.isNotEmpty() || state.yearlyReport.transactionCount > 0) {
                            item(key = "category_analysis") {
                                val activeBreakdown = if (showYearlyReport) state.yearlyReport.categoryTotals else categoryBreakdown
                                PremiumDarkVaultCard(
                                    accentGold = palette.gold.copy(alpha = 0.65f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { analyticsExpanded = !analyticsExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showYearlyReport) "YEARLY REPORT • ${state.yearlyReport.year}" else "CATEGORY ALLOCATION",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.gold,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                onClick = { showYearlyReport = !showYearlyReport },
                                                shape = RoundedCornerShape(10.dp),
                                                color = palette.gold.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, palette.gold.copy(alpha = 0.4f)),
                                                modifier = Modifier.testTag("toggle_yearly_report_button")
                                            ) {
                                                Text(
                                                    text = if (showYearlyReport) "Monthly" else "Yearly",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.gold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            ExpandCollapseChevron(expanded = analyticsExpanded, tint = palette.gold)
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = analyticsExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (showYearlyReport) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    LedgerMetricColumn(
                                                        label = "YR INFLOW",
                                                        value = DateAndFormatUtils.formatCurrency(state.yearlyReport.totalIncome, symbol, compact = true),
                                                        color = palette.mint,
                                                        mutedColor = palette.muted,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    LedgerMetricColumn(
                                                        label = "YR OUTFLOW",
                                                        value = DateAndFormatUtils.formatCurrency(state.yearlyReport.totalExpense, symbol, compact = true),
                                                        color = palette.coral,
                                                        mutedColor = palette.muted,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    LedgerMetricColumn(
                                                        label = "YR NET (${state.yearlyReport.cachedMonthsCount}m)",
                                                        value = DateAndFormatUtils.formatCurrency(state.yearlyReport.netSavings, symbol, compact = true),
                                                        color = palette.gold,
                                                        mutedColor = palette.muted,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            if (activeBreakdown.isNotEmpty()) {
                                                val donutChart: @Composable () -> Unit = {
                                                    Box(
                                                        modifier = Modifier.size(100.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Canvas(modifier = Modifier.size(100.dp)) {
                                                            val stroke = 13.dp.toPx()
                                                            val inset = stroke / 2f
                                                            val arcSize = Size(size.width - stroke, size.height - stroke)
                                                            var startAngle = -90f
                                                            activeBreakdown.forEach { (cat, _, fraction) ->
                                                                val sweep = (fraction * 360f).coerceAtLeast(4f)
                                                                drawArc(
                                                                    color = cat.accentColor,
                                                                    startAngle = startAngle,
                                                                    sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                                                                    useCenter = false,
                                                                    topLeft = Offset(inset, inset),
                                                                    size = arcSize,
                                                                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                                                )
                                                                startAngle += sweep
                                                            }
                                                        }
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "${activeBreakdown.size}",
                                                                style = MaterialTheme.typography.titleLarge.copy(
                                                                    fontFamily = JetBrainsMonoFontFamily
                                                                ),
                                                                color = palette.ink,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                            Text(
                                                                text = "cats",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = palette.muted,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                                val legendColumn: @Composable (Modifier) -> Unit = { legendMod ->
                                                    Column(
                                                        modifier = legendMod,
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        activeBreakdown.take(4).forEach { (cat, amount, fraction) ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(8.dp)
                                                                            .clip(CircleShape)
                                                                            .background(cat.accentColor)
                                                                    )
                                                                    Spacer(Modifier.width(8.dp))
                                                                    Text(
                                                                        text = cat.label,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = palette.ink,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                Spacer(Modifier.width(8.dp))
                                                                Text(
                                                                    text = "${DateAndFormatUtils.formatCurrency(amount, symbol, compact = true)} (${(fraction * 100).roundToInt()}%)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = palette.gold,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                if (metrics.shouldStackTightPairs) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        donutChart()
                                                        legendColumn(Modifier.fillMaxWidth())
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        donutChart()
                                                        Spacer(Modifier.width(16.dp))
                                                        legendColumn(Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsible Search & Filter Chips
                        item(key = "search_filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnimatedVisibility(visible = searchVisible) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search merchant, category, note…") },
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("expense_search_input")
                                    )
                                }

                                AnimatedVisibility(visible = filtersVisible) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(LocalDateRangeFilter.entries, key = { it.name }) { range ->
                                                FilterChip(
                                                    selected = dateRangeFilter == range,
                                                    onClick = { dateRangeFilter = range },
                                                    label = { Text(range.label) }
                                                )
                                            }
                                        }
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            item {
                                                FilterChip(
                                                    selected = typeFilter == null && categoryFilter == null,
                                                    onClick = {
                                                        typeFilter = null
                                                        categoryFilter = null
                                                    },
                                                    label = { Text("All") }
                                                )
                                            }
                                            item {
                                                FilterChip(
                                                    selected = typeFilter == TransactionType.EXPENSE,
                                                    onClick = {
                                                        typeFilter = if (typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                                    },
                                                    label = { Text("Expenses") }
                                                )
                                            }
                                            item {
                                                FilterChip(
                                                    selected = typeFilter == TransactionType.INCOME,
                                                    onClick = {
                                                        typeFilter = if (typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                                    },
                                                    label = { Text("Income") }
                                                )
                                            }
                                            items(ExpenseCategory.expenseCategories, key = { it.name }) { cat ->
                                                FilterChip(
                                                    selected = categoryFilter == cat,
                                                    onClick = {
                                                        categoryFilter = if (categoryFilter == cat) null else cat
                                                    },
                                                    label = { Text(cat.label) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = cat.accentColor.copy(alpha = 0.2f),
                                                        selectedLabelColor = cat.accentColor
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state.isSyncingMonth) {
                            item(key = "syncing_indicator") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.gold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Syncing ${DateAndFormatUtils.formatMonthLabel(state.selectedMonthKey)}…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted
                                    )
                                }
                            }
                        }

                        if (filteredDailyTransactions.isEmpty()) {
                            item(key = "empty_tx") {
                                EmptyStateCard(
                                    icon = Icons.Rounded.ReceiptLong,
                                    title = "Daily ledger is empty",
                                    subtitle = "Tap + to log a daily expense or income for ${DateAndFormatUtils.formatMonthLabel(state.selectedMonthKey)}. Use the Trips tab for separate travel spending.",
                                    actionLabel = "Add Entry",
                                    onAction = {
                                        editingTransaction = null
                                        sheetDefaultType = TransactionType.EXPENSE
                                        sheetDefaultTripMode = false
                                        showAddSheet = true
                                    },
                                    accentColor = palette.gold
                                )
                            }
                        } else {
                            items(filteredDailyTransactions, key = { it.id }) { tx ->
                                SwipeableTransactionRow(
                                    transaction = tx,
                                    currencySymbol = symbol,
                                    palette = palette,
                                    showEventBadge = false,
                                    onClick = {
                                        editingTransaction = tx
                                        sheetDefaultType = tx.type
                                        sheetDefaultTripMode = tx.isTripOrEvent
                                        showAddSheet = true
                                    },
                                    onDelete = { onDeleteTransaction(tx.id) }
                                )
                            }

                            if (state.hasMoreTransactions) {
                                item(key = "pagination_footer") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.isPaginating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = palette.gold
                                            )
                                        } else {
                                            OutlinedButton(
                                                onClick = onLoadOlder,
                                                border = BorderStroke(1.dp, palette.gold.copy(alpha = 0.5f)),
                                                modifier = Modifier.testTag("load_older_transactions_button")
                                            ) {
                                                Text("Load Older Transactions", color = palette.gold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        val editing = editingTransaction
        AddTransactionSheet(
            existing = editing,
            defaultType = editing?.type ?: sheetDefaultType,
            defaultIsTripOrEvent = editing?.isTripOrEvent ?: sheetDefaultTripMode,
            existingTripNames = state.tripEventBreakdown.map { it.first },
            currencySymbol = symbol,
            isSaving = state.isSaving,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { docId, title, amount, type, category, timestamp, note, isTrip, eventName ->
                if (editing != null) {
                    onUpdateTransaction(editing, title, amount, type, category, note, isTrip, eventName) {
                        showAddSheet = false
                        editingTransaction = null
                    }
                } else {
                    onAddTransaction(docId, title, amount, type, category, timestamp, note, isTrip, eventName) {
                        showAddSheet = false
                    }
                }
            }
        )
    }

    if (showAddReminderSheet) {
        AddMoneyReminderSheet(
            currencySymbol = symbol,
            isSaving = state.isSaving,
            onDismiss = { showAddReminderSheet = false },
            onSave = { docId, title, cat, amount, dueMs, recurring ->
                onAddReminder(docId, title, cat, amount, dueMs, recurring) {
                    showAddReminderSheet = false
                }
            }
        )
    }
}

@Composable
private fun LedgerMetricColumn(
    label: String,
    value: String,
    color: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableTransactionRow(
    transaction: TransactionItem,
    currencySymbol: String,
    palette: VaultPalette,
    showEventBadge: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    cs.error
                } else cs.errorContainer.copy(alpha = 0.4f),
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete transaction",
                    tint = Color.White
                )
            }
        }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = palette.rowBg,
            border = BorderStroke(1.dp, palette.rowBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_row_${transaction.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(category = transaction.category, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (showEventBadge || transaction.isTripOrEvent) {
                            Surface(
                                shape = CircleShape,
                                color = palette.gold.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = transaction.eventName.ifBlank { "Trip" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.gold,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(transaction.category.label)
                            append(" • ")
                            append(DateAndFormatUtils.formatShortDate(transaction.timestampEpochMs))
                            if (transaction.note.isNotBlank()) {
                                append(" • ")
                                append(transaction.note)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                val isIncome = transaction.type == TransactionType.INCOME
                val sign = if (isIncome) "+" else "-"
                Text(
                    text = "$sign${DateAndFormatUtils.formatCurrency(transaction.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (isIncome) palette.mint else palette.gold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    existing: TransactionItem?,
    defaultType: TransactionType,
    defaultIsTripOrEvent: Boolean,
    existingTripNames: List<String>,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, TransactionType, ExpenseCategory, Long, String, Boolean, String) -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val goldAccent = if (extended.isDark) ChampagneGold else ChampagneGoldLight
    val clientDocId = remember(existing?.id) {
        existing?.id ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(20)}"
    }
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: defaultType) }
    var isTripOrEvent by remember(existing?.id, defaultIsTripOrEvent) {
        mutableStateOf(existing?.isTripOrEvent ?: defaultIsTripOrEvent)
    }
    var eventName by remember(existing?.id) { mutableStateOf(existing?.eventName ?: "") }
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var amountText by remember(existing?.id) {
        mutableStateOf(
            existing?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var selectedCategory by remember(existing?.id, defaultIsTripOrEvent) {
        mutableStateOf(
            existing?.category ?: when {
                defaultType == TransactionType.INCOME -> ExpenseCategory.SALARY
                defaultIsTripOrEvent -> ExpenseCategory.TRAVEL
                else -> ExpenseCategory.FOOD
            }
        )
    }

    val categories = if (type == TransactionType.INCOME) {
        ExpenseCategory.incomeCategories
    } else {
        ExpenseCategory.expenseCategories
    }

    val suggestedTrips = remember(existingTripNames) {
        (existingTripNames + listOf("Weekend Trip", "Vacation", "Wedding", "Festival", "Business Trip"))
            .distinct()
            .take(8)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when {
                    existing != null -> "Edit Transaction"
                    isTripOrEvent -> "Log Trip / Event Expense"
                    else -> "Log Vault Transaction"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { idx, itemType ->
                    SegmentedButton(
                        selected = type == itemType,
                        onClick = {
                            type = itemType
                            selectedCategory = if (itemType == TransactionType.INCOME) {
                                isTripOrEvent = false
                                ExpenseCategory.SALARY
                            } else {
                                if (isTripOrEvent) ExpenseCategory.TRAVEL else ExpenseCategory.FOOD
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size),
                        modifier = Modifier.testTag("tx_type_${itemType.name.lowercase()}")
                    ) {
                        Text(itemType.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Separate Trip / Event Expense Toggle (Excluded from Daily Tracker!)
            if (type == TransactionType.EXPENSE) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTripOrEvent) goldAccent.copy(alpha = 0.14f) else cs.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isTripOrEvent) goldAccent.copy(alpha = 0.55f) else cs.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FlightTakeoff,
                                    contentDescription = null,
                                    tint = if (isTripOrEvent) goldAccent else cs.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Separate Trip / Event",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Won't disturb your daily expenses or budget",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isTripOrEvent,
                                onCheckedChange = { enabled ->
                                    isTripOrEvent = enabled
                                    if (enabled && selectedCategory == ExpenseCategory.FOOD) {
                                        selectedCategory = ExpenseCategory.TRAVEL
                                    }
                                },
                                modifier = Modifier.testTag("tx_trip_toggle")
                            )
                        }

                        AnimatedVisibility(visible = isTripOrEvent) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(suggestedTrips, key = { it }) { preset ->
                                        FilterChip(
                                            selected = eventName.equals(preset, ignoreCase = true),
                                            onClick = { eventName = preset },
                                            label = { Text(preset) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = eventName,
                                    onValueChange = { eventName = it.take(60) },
                                    label = { Text("Trip / Event Name (e.g., Goa Trip, Sister's Wedding)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("tx_event_name_input")
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text(if (isTripOrEvent) "Expense Title (e.g., Hotel, Flight, Fuel)" else "Title (e.g., Matcha Latte, Salary)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text("Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.name }) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) cat.accentColor.copy(alpha = 0.22f) else cs.surfaceVariant,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) cat.accentColor else cs.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) cat.accentColor else cs.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(240) },
                label = { Text("Note (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            SavingActionButton(
                label = when {
                    existing != null -> "Update Entry"
                    isTripOrEvent -> "Save to Trips Ledger"
                    else -> "Save to Vault"
                },
                isSaving = isSaving,
                enabled = title.isNotBlank() && parsedAmount > 0.0,
                onClick = {
                    val cleanEvent = if (isTripOrEvent) eventName.trim().ifBlank { "General Trip / Event" } else ""
                    onSave(
                        clientDocId,
                        title.trim(),
                        parsedAmount,
                        type,
                        selectedCategory,
                        existing?.timestampEpochMs ?: System.currentTimeMillis(),
                        note.trim(),
                        isTripOrEvent,
                        cleanEvent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_tx_button")
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
