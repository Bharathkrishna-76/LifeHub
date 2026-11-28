package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.LifeHubApplication
import com.example.domain.model.TransactionType
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.DynamicIslandBar
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HabitScreen
import com.example.ui.screens.MoneyScoreScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WrappedScreen
import com.example.ui.viewmodel.LifeHubViewModel
import kotlinx.coroutines.launch

object LifeHubRoutes {
    const val DASHBOARD = "dashboard"
    const val EXPENSES = "expenses"
    const val HABITS = "habits"
    const val GOALS = "goals"
    const val PLANNER = "planner"
    const val MONEY_SCORE = "money_score"
    const val WRAPPED = "wrapped"
    const val ACHIEVEMENTS = "achievements"
    const val PROFILE = "profile"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val primaryTabs = listOf(
    NavTab(LifeHubRoutes.DASHBOARD, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(LifeHubRoutes.EXPENSES, "Money", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_money"),
    NavTab(LifeHubRoutes.HABITS, "Habits", Icons.Rounded.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "nav_habits"),
    NavTab(LifeHubRoutes.GOALS, "Goals", Icons.Rounded.Savings, Icons.Outlined.Savings, "nav_goals"),
    NavTab(LifeHubRoutes.PLANNER, "Plan", Icons.Rounded.Checklist, Icons.Outlined.Checklist, "nav_plan")
)

@Composable
fun LifeHubMainShell(
    currentUserId: String,
    onSwitchToTripSplit: (() -> Unit)? = null
) {
    val viewModel: LifeHubViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY] as? LifeHubApplication) {
                    "LifeHubApplication missing from CreationExtras"
                }
                LifeHubViewModel(
                    repository = app.container.repository,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LifeHubRoutes.DASHBOARD
    val showChrome = currentRoute in primaryTabs.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingTxSheetType by remember { mutableStateOf<TransactionType?>(null) }
    var pendingMoneySubTab by remember { mutableStateOf<Int?>(null) }
    var pendingTripSheet by remember { mutableStateOf(false) }
    var islandMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val initials = state.profile.displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "LH" }

    val islandTitle = when (currentRoute) {
        LifeHubRoutes.DASHBOARD -> "LifeHub"
        LifeHubRoutes.EXPENSES -> "Vault"
        LifeHubRoutes.HABITS -> "CyberFit"
        LifeHubRoutes.GOALS -> "3D Vaults"
        LifeHubRoutes.PLANNER -> "Flow"
        else -> "LifeHub"
    }

    val islandMetric = when (currentRoute) {
        LifeHubRoutes.HABITS -> "${state.habits.maxOfOrNull { it.currentStreak } ?: 0}d 🔥"
        LifeHubRoutes.GOALS -> "${state.goals.count { it.completed }}/${state.goals.size}"
        LifeHubRoutes.PLANNER -> "${state.tasks.count { it.status.name != "DONE" }} open"
        else -> DateAndFormatUtils.formatCurrency(
            state.netBalance,
            state.settings.currencySymbol,
            compact = true
        )
    }

    val unpaidReminders = state.reminders.count { !it.paid }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showChrome && !useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                            if (onSwitchToTripSplit != null) {
                                Spacer(Modifier.height(10.dp))
                                TripSplitRailSwitchButton(onClick = onSwitchToTripSplit)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Persistent morphing Dynamic Island Style bar across primary tabs
                    if (showChrome) {
                        DynamicIslandBar(
                            title = islandTitle,
                            pillMetric = islandMetric,
                            unreadAlertsCount = unpaidReminders,
                            isSyncing = state.isSyncingMonth,
                            menuOpen = islandMenuOpen,
                            initials = initials,
                            onToggleMenu = { islandMenuOpen = !islandMenuOpen },
                            onRefresh = { viewModel.refreshAll() },
                            onOpenNotifications = {
                                pendingMoneySubTab = 2
                                navigateTopTab(LifeHubRoutes.EXPENSES)
                            },
                            onOpenProfile = { navController.navigate(LifeHubRoutes.PROFILE) },
                            onOpenSettings = { navController.navigate(LifeHubRoutes.PROFILE) },
                            onOpenWrapped = { navController.navigate(LifeHubRoutes.WRAPPED) },
                            onOpenMoneyScore = { navController.navigate(LifeHubRoutes.MONEY_SCORE) },
                            onOpenAchievements = { navController.navigate(LifeHubRoutes.ACHIEVEMENTS) },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (showChrome) Modifier.widthIn(max = 840.dp) else Modifier
                                )
                        ) {
                        NavHost(
                            navController = navController,
                            startDestination = LifeHubRoutes.DASHBOARD
                        ) {
                            composable(LifeHubRoutes.DASHBOARD) {
                                DashboardScreen(
                                    state = state,
                                    onQuickAddExpense = {
                                        pendingTxSheetType = TransactionType.EXPENSE
                                        pendingMoneySubTab = 0
                                        pendingTripSheet = false
                                        navigateTopTab(LifeHubRoutes.EXPENSES)
                                    },
                                    onQuickAddIncome = {
                                        pendingTxSheetType = TransactionType.INCOME
                                        pendingMoneySubTab = 0
                                        pendingTripSheet = false
                                        navigateTopTab(LifeHubRoutes.EXPENSES)
                                    },
                                    onQuickAddTrip = {
                                        pendingMoneySubTab = 1
                                        pendingTripSheet = true
                                        navigateTopTab(LifeHubRoutes.EXPENSES)
                                    },
                                    onNavigateBillsAndEmi = {
                                        pendingMoneySubTab = 2
                                        pendingTripSheet = false
                                        navigateTopTab(LifeHubRoutes.EXPENSES)
                                    },
                                    onNavigateHabits = { navigateTopTab(LifeHubRoutes.HABITS) },
                                    onNavigateGoals = { navigateTopTab(LifeHubRoutes.GOALS) },
                                    onNavigatePlan = { navigateTopTab(LifeHubRoutes.PLANNER) },
                                    onNavigateMoneyScore = { navController.navigate(LifeHubRoutes.MONEY_SCORE) },
                                    onNavigateWrapped = { navController.navigate(LifeHubRoutes.WRAPPED) },
                                    onNavigateAchievements = { navController.navigate(LifeHubRoutes.ACHIEVEMENTS) },
                                    onNavigateProfile = { navController.navigate(LifeHubRoutes.PROFILE) },
                                    onRefresh = { viewModel.refreshAll() },
                                    onDismissError = { viewModel.clearError() }
                                )
                            }

                            composable(LifeHubRoutes.EXPENSES) {
                                ExpenseScreen(
                                    state = state,
                                    initialSheetType = pendingTxSheetType,
                                    initialSubTab = pendingMoneySubTab,
                                    initialTripSheet = pendingTripSheet,
                                    onConsumeInitialSheet = {
                                        pendingTxSheetType = null
                                        pendingMoneySubTab = null
                                        pendingTripSheet = false
                                    },
                                    onShiftMonth = { viewModel.shiftMonth(it) },
                                    onRefresh = { viewModel.refreshAll() },
                                    onLoadOlder = { viewModel.loadOlderTransactions() },
                                    onAddTransaction = { docId, title, amt, type, cat, ts, note, isTrip, eventName, done ->
                                        viewModel.addTransaction(docId, title, amt, type, cat, ts, note, isTrip, eventName, done)
                                    },
                                    onUpdateTransaction = { existing, title, amt, type, cat, note, isTrip, eventName, done ->
                                        viewModel.updateTransaction(existing, title, amt, type, cat, note, isTrip, eventName, done)
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                    onAddReminder = { docId, title, cat, amt, dueMs, recurring, done ->
                                        viewModel.addReminder(docId, title, cat, amt, dueMs, recurring, done)
                                    },
                                    onToggleReminderPaid = { viewModel.toggleReminderPaid(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) }
                                )
                            }

                            composable(LifeHubRoutes.HABITS) {
                                HabitScreen(
                                    state = state,
                                    onAddHabit = { docId, title, iconKey, colorHex, target, done ->
                                        viewModel.addHabit(docId, title, iconKey, colorHex, target, done)
                                    },
                                    onToggleHabitToday = { habit, dateKey ->
                                        viewModel.toggleHabitToday(habit, dateKey)
                                    },
                                    onDeleteHabit = { viewModel.deleteHabit(it) }
                                )
                            }

                            composable(LifeHubRoutes.GOALS) {
                                GoalsScreen(
                                    state = state,
                                    onAddGoal = { docId, title, preset, target, initial, targetMonth, done ->
                                        viewModel.addGoal(docId, title, preset, target, initial, targetMonth, done)
                                    },
                                    onContribute = { goal, amt, done ->
                                        viewModel.contributeToGoal(goal, amt, done)
                                    },
                                    onDeleteGoal = { viewModel.deleteGoal(it) },
                                    onDismissCelebration = { viewModel.dismissCelebration() }
                                )
                            }

                            composable(LifeHubRoutes.PLANNER) {
                                PlannerScreen(
                                    state = state,
                                    onAddTask = { docId, title, cadence, priority, dueMs, recurring, notes, done ->
                                        viewModel.addTask(docId, title, cadence, priority, dueMs, recurring, notes, done)
                                    },
                                    onMoveTaskStatus = { task, status ->
                                        viewModel.moveTaskStatus(task, status)
                                    },
                                    onDeleteTask = { viewModel.deleteTask(it) },
                                    onAddReminder = { docId, title, cat, amt, dueMs, recurring, done ->
                                        viewModel.addReminder(docId, title, cat, amt, dueMs, recurring, done)
                                    },
                                    onToggleReminderPaid = { viewModel.toggleReminderPaid(it) },
                                    onDeleteReminder = { viewModel.deleteReminder(it) }
                                )
                            }

                            composable(LifeHubRoutes.MONEY_SCORE) {
                                MoneyScoreScreen(
                                    moneyScore = state.moneyScore,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(LifeHubRoutes.WRAPPED) {
                                WrappedScreen(
                                    wrapped = state.wrappedSummary,
                                    currencySymbol = state.settings.currencySymbol,
                                    onClose = { navController.popBackStack() },
                                    onShareMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }

                            composable(LifeHubRoutes.ACHIEVEMENTS) {
                                AchievementsScreen(
                                    achievements = state.achievements,
                                    totalXp = state.totalXp,
                                    playerLevel = state.playerLevel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(LifeHubRoutes.PROFILE) {
                                ProfileScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onUpdateTheme = { viewModel.updateThemeMode(it) },
                                    onUpdateBudgetAndCurrency = { budget, sym ->
                                        viewModel.updateBudgetAndCurrency(budget, sym)
                                    },
                                    onUpdateNotifications = { viewModel.updateNotificationsEnabled(it) },
                                    onUpdateSeparateTripMode = { viewModel.updateSeparateTripEventLedger(it) },
                                    onForceRefresh = { viewModel.refreshAll() },
                                    onFullCloudResync = { viewModel.forceFullResync() },
                                    onFetchTransactionsForExport = { viewModel.getCachedTransactionsForExport() },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onBeforeSignOut = { viewModel.clearCachesOnSignOut() }
                                )
                            }
                        }
                        if (showChrome && !useNavRail && onSwitchToTripSplit != null) {
                            TripSplitCompactLauncherPill(
                                onClick = onSwitchToTripSplit,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp, bottom = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TripSplitCompactLauncherPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("switch_to_tripsplit_mode_button"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16241E),
        border = BorderStroke(1.dp, Color(0xFFC8552B)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = stringResource(R.string.ts_mode_switch_to_tripsplit_cd),
                tint = Color(0xFFE06E43),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.ts_mode_switch_to_tripsplit),
                color = Color(0xFFFBF9F5),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TripSplitRailSwitchButton(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("switch_to_tripsplit_mode_button"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16241E),
        border = BorderStroke(1.dp, Color(0xFFC8552B))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = stringResource(R.string.ts_mode_switch_to_tripsplit_cd),
                tint = Color(0xFFE06E43),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Trips",
                color = Color(0xFFFBF9F5),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
