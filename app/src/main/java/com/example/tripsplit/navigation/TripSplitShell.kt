package com.example.tripsplit.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.tripsplit.components.InitialsAvatar
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.screens.AddEditTripExpenseSheet
import com.example.tripsplit.screens.CreateTripSheet
import com.example.tripsplit.screens.InviteMembersSheet
import com.example.tripsplit.screens.TopUpWalletSheet
import com.example.tripsplit.screens.TripSplitHomeScreen
import com.example.tripsplit.screens.TripSplitLedgerScreen
import com.example.tripsplit.screens.TripSplitSquadScreen
import com.example.tripsplit.screens.TripSplitTripsScreen
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripSplitTheme
import com.example.tripsplit.viewmodel.TripSplitUiState
import com.example.tripsplit.viewmodel.TripSplitViewModel

private enum class TripSplitTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    OVERVIEW("Overview", Icons.Rounded.Explore, "ts_tab_overview"),
    TRIPS("Trips", Icons.Rounded.Luggage, "ts_tab_trips"),
    EXPENSES("Ledger", Icons.Rounded.ReceiptLong, "ts_tab_expenses"),
    SQUAD("Squad", Icons.Rounded.Groups, "ts_tab_squad")
}

@Composable
fun TripSplitRoot(
    travelerFirstName: String,
    onSwitchToDailyExpenses: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TripSplitViewModel = viewModel(
        factory = TripSplitViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TripSplitTheme(themePreference = uiState.themePreference) {
        TripSplitShell(
            uiState = uiState,
            travelerFirstName = travelerFirstName,
            viewModel = viewModel,
            onSwitchToDailyExpenses = onSwitchToDailyExpenses
        )
    }
}

@Composable
private fun TripSplitShell(
    uiState: TripSplitUiState,
    travelerFirstName: String,
    viewModel: TripSplitViewModel,
    onSwitchToDailyExpenses: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(TripSplitTab.OVERVIEW) }
    var showCreateTripSheet by remember { mutableStateOf(false) }
    var preselectedCover by remember { mutableStateOf(DestinationCover.COASTAL) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<TripExpense?>(null) }
    var showTopUpWalletSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val openCreateTrip: (DestinationCover?) -> Unit = { cover ->
        preselectedCover = cover ?: DestinationCover.COASTAL
        showCreateTripSheet = true
    }

    val shareTripSummary: () -> Unit = {
        val text = viewModel.buildShareSummaryText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "TripSplit Summary")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Trip Summary"))
    }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    BackHandler(enabled = currentTab != TripSplitTab.OVERVIEW) {
        currentTab = TripSplitTab.OVERVIEW
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val isExpanded = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isExpanded) {
                TripSplitSideRail(
                    currentTab = currentTab,
                    travelerFirstName = travelerFirstName,
                    isDark = colors.isDark,
                    onSelectTab = { currentTab = it },
                    onToggleTheme = viewModel::toggleTheme,
                    onSwitchToDaily = onSwitchToDailyExpenses
                )
                VerticalDivider(color = colors.divider, thickness = 1.dp)
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = colors.canvas,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (!isExpanded) {
                        TripSplitCompactTopBar(
                            travelerFirstName = travelerFirstName,
                            isDark = colors.isDark,
                            onToggleTheme = viewModel::toggleTheme,
                            onSwitchToDaily = onSwitchToDailyExpenses
                        )
                    }
                },
                bottomBar = {
                    if (!isExpanded) {
                        TripSplitCompactBottomBar(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it }
                        )
                    }
                },
                floatingActionButton = {
                    if (uiState.activeTrip != null &&
                        (currentTab == TripSplitTab.OVERVIEW || currentTab == TripSplitTab.EXPENSES)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                editingExpense = null
                                showAddExpenseSheet = true
                            },
                            containerColor = colors.accent,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            modifier = Modifier.testTag("ts_add_expense_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.ts_fab_add_expense)
                            )
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = colors.inkPrimary,
                            contentColor = colors.surface,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tripsplit_tab_content",
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 820.dp)
                    ) { tab ->
                        when (tab) {
                            TripSplitTab.OVERVIEW -> TripSplitHomeScreen(
                                uiState = uiState,
                                travelerFirstName = travelerFirstName,
                                onCreateTripClick = openCreateTrip,
                                onAddExpenseClick = {
                                    if (uiState.activeTrip != null) {
                                        editingExpense = null
                                        showAddExpenseSheet = true
                                    } else {
                                        openCreateTrip(null)
                                    }
                                },
                                onTopUpWalletClick = { showTopUpWalletSheet = true },
                                onInviteMembersClick = { showInviteSheet = true },
                                onOpenTripsTab = { currentTab = TripSplitTab.TRIPS },
                                onOpenExpensesTab = { currentTab = TripSplitTab.EXPENSES },
                                onOpenSquadTab = { currentTab = TripSplitTab.SQUAD },
                                onSelectTrip = { id -> viewModel.selectTrip(id) },
                                onSettleTransfer = { transfer ->
                                    viewModel.recordSettlement(
                                        fromMemberId = transfer.fromMember.id,
                                        toMemberId = transfer.toMember.id,
                                        amount = transfer.amount
                                    )
                                },
                                onEditExpense = { expense ->
                                    editingExpense = expense
                                    showAddExpenseSheet = true
                                },
                                onToggleFavorite = viewModel::toggleFavoriteTrip,
                                onShareTripSummary = shareTripSummary,
                                onRefresh = viewModel::refreshDashboard
                            )

                            TripSplitTab.TRIPS -> TripSplitTripsScreen(
                                uiState = uiState,
                                onSelectTrip = { id ->
                                    viewModel.selectTrip(id)
                                    currentTab = TripSplitTab.OVERVIEW
                                },
                                onCreateTripClick = openCreateTrip,
                                onDeleteTrip = viewModel::deleteTrip,
                                onToggleFavorite = viewModel::toggleFavoriteTrip
                            )

                            TripSplitTab.EXPENSES -> TripSplitLedgerScreen(
                                uiState = uiState,
                                onAddExpenseClick = {
                                    editingExpense = null
                                    showAddExpenseSheet = true
                                },
                                onEditExpense = { expense ->
                                    editingExpense = expense
                                    showAddExpenseSheet = true
                                },
                                onDeleteExpense = viewModel::deleteExpense,
                                onCreateTripClick = { openCreateTrip(null) }
                            )

                            TripSplitTab.SQUAD -> TripSplitSquadScreen(
                                uiState = uiState,
                                onInviteMembersClick = { showInviteSheet = true },
                                onRemoveTraveler = viewModel::removeTraveler,
                                onSettleTransfer = { transfer ->
                                    viewModel.recordSettlement(
                                        fromMemberId = transfer.fromMember.id,
                                        toMemberId = transfer.toMember.id,
                                        amount = transfer.amount
                                    )
                                },
                                onDeleteSettlement = viewModel::deleteSettlement,
                                onTopUpWalletClick = { showTopUpWalletSheet = true },
                                onDeleteWalletEntry = viewModel::deleteWalletEntry,
                                onCreateTripClick = { openCreateTrip(null) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (showCreateTripSheet) {
        CreateTripSheet(
            defaultHostName = travelerFirstName,
            initialCover = preselectedCover,
            onDismiss = { showCreateTripSheet = false },
            onCreateTrip = { title, dest, origin, start, end, budget, sym, cover, stops, travelers, wallet ->
                viewModel.createTrip(
                    title = title,
                    destination = dest,
                    origin = origin,
                    startDateLabel = start,
                    endDateLabel = end,
                    totalBudget = budget,
                    currencySymbol = sym,
                    cover = cover,
                    waypoints = stops,
                    travelerNames = travelers,
                    initialWalletDeposit = wallet
                )
            }
        )
    }

    if (showAddExpenseSheet && uiState.activeTrip != null) {
        AddEditTripExpenseSheet(
            members = uiState.members,
            currencySymbol = uiState.activeTrip.currencySymbol,
            walletBalance = uiState.metrics?.walletBalance ?: 0.0,
            existingExpense = editingExpense,
            onDismiss = {
                showAddExpenseSheet = false
                editingExpense = null
            },
            onSave = { existingId, title, amount, category, paidBy, paidFromWallet, splitMode, shares, note ->
                viewModel.saveExpense(
                    existingId = existingId,
                    title = title,
                    amount = amount,
                    category = category,
                    paidByMemberId = paidBy,
                    paidFromWallet = paidFromWallet,
                    splitMode = splitMode,
                    shares = shares,
                    locationNote = note
                )
            }
        )
    }

    if (showTopUpWalletSheet && uiState.activeTrip != null) {
        TopUpWalletSheet(
            members = uiState.members,
            currencySymbol = uiState.activeTrip.currencySymbol,
            onDismiss = { showTopUpWalletSheet = false },
            onSubmit = { memberId, amount, type, note ->
                viewModel.addWalletEntry(memberId, amount, type, note)
            }
        )
    }

    if (showInviteSheet && uiState.activeTrip != null) {
        InviteMembersSheet(
            trip = uiState.activeTrip,
            onDismiss = { showInviteSheet = false },
            onAddTraveler = viewModel::addTraveler
        )
    }
}

/**
 * Clean, compact Top App Bar: logo + TripSplit brand, profile avatar, theme toggle, Daily switcher.
 */
@Composable
private fun TripSplitCompactTopBar(
    travelerFirstName: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onSwitchToDaily: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .statusBarsPadding()
            .displayCutoutPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Luggage,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.ts_brand_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .testTag("ts_theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = stringResource(R.string.ts_toggle_theme_cd),
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                InitialsAvatar(
                    name = travelerFirstName,
                    backgroundColor = colors.forest,
                    size = 28.dp
                )

                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSwitchToDaily)
                        .testTag("ts_switch_to_daily_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.ts_mode_switch_to_daily_cd),
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.ts_mode_daily),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}

/**
 * Compact, fixed Bottom Navigation Bar respecting system navigation insets with zero overlap.
 */
@Composable
private fun TripSplitCompactBottomBar(
    currentTab: TripSplitTab,
    onSelectTab: (TripSplitTab) -> Unit
) {
    val colors = LocalTripSplitColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.navBarBg)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripSplitTab.entries.forEach { tab ->
                val selected = currentTab == tab
                val tint = if (selected) colors.accent else colors.navIconInactive
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 60.dp)
                        .clickable { onSelectTab(tab) }
                        .testTag(tab.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (selected) colors.accent else Color.Transparent)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = tint,
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

@Composable
private fun TripSplitSideRail(
    currentTab: TripSplitTab,
    travelerFirstName: String,
    isDark: Boolean,
    onSelectTab: (TripSplitTab) -> Unit,
    onToggleTheme: () -> Unit,
    onSwitchToDaily: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    Column(
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(colors.navBarBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Luggage,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TripSplitTab.entries.forEach { tab ->
                val selected = currentTab == tab
                val tint = if (selected) colors.accent else colors.navIconInactive
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) colors.accentSoft else Color.Transparent)
                        .clickable { onSelectTab(tab) }
                        .testTag(tab.testTag)
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InitialsAvatar(name = travelerFirstName, backgroundColor = colors.forest, size = 30.dp)
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("ts_theme_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = stringResource(R.string.ts_toggle_theme_cd),
                    tint = colors.inkSecondary
                )
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSwitchToDaily)
                    .testTag("ts_switch_to_daily_btn"),
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceMuted
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = stringResource(R.string.ts_mode_switch_to_daily_cd),
                        tint = colors.accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(R.string.ts_mode_daily),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
