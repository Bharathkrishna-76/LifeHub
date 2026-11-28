package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.toUserSafeErrorMessage
import com.example.data.repository.DuplicateWriteIgnoredException
import com.example.data.repository.LifeHubRepository
import com.example.domain.engine.AchievementEngine
import com.example.domain.engine.MoneyScoreCalculator
import com.example.domain.model.AchievementItem
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.GoalPreset
import com.example.domain.model.HabitItem
import com.example.domain.model.MoneyScoreBreakdown
import com.example.domain.model.MonthlyTrendPoint
import com.example.domain.model.MonthlyWrappedSummary
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TaskCadence
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.domain.model.UserSettings
import com.example.domain.model.YearlyReportSummary
import com.example.domain.util.DateAndFormatUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LifeHubUiState(
    val userId: String = "",
    val profile: UserProfile = UserProfile(),
    val settings: UserSettings = UserSettings(),
    val selectedMonthKey: String = DateAndFormatUtils.currentMonthKey(),
    val transactions: List<TransactionItem> = emptyList(),
    val dailyTransactions: List<TransactionItem> = emptyList(),
    val tripEventTransactions: List<TransactionItem> = emptyList(),
    val allTripEventTransactions: List<TransactionItem> = emptyList(),
    val tripEventTotalExpense: Double = 0.0,
    val tripEventBreakdown: List<Pair<String, Double>> = emptyList(),
    val allCachedTransactionsCount: Int = 0,
    val yearlyReport: YearlyReportSummary = YearlyReportSummary(
        year = DateAndFormatUtils.currentMonthKey().substringBefore("-").toIntOrNull() ?: 2026,
        totalIncome = 0.0,
        totalExpense = 0.0,
        netSavings = 0.0,
        cachedMonthsCount = 0,
        transactionCount = 0,
        topCategory = null,
        topCategoryAmount = 0.0,
        categoryTotals = emptyList(),
        monthlyBreakdown = emptyList()
    ),
    val habits: List<HabitItem> = emptyList(),
    val goals: List<SavingsGoalItem> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val reminders: List<ReminderItem> = emptyList(),
    val achievements: List<AchievementItem> = emptyList(),
    val moneyScore: MoneyScoreBreakdown = MoneyScoreCalculator.calculate(emptyList(), 25000.0, emptyList(), emptyList()),
    val wrappedSummary: MonthlyWrappedSummary = MonthlyWrappedSummary(
        monthLabel = DateAndFormatUtils.formatMonthLabel(DateAndFormatUtils.currentMonthKey()),
        totalSpending = 0.0,
        totalIncome = 0.0,
        totalSavings = 0.0,
        topCategory = null,
        topCategoryAmount = 0.0,
        bestSavingMonthLabel = DateAndFormatUtils.formatMonthLabel(DateAndFormatUtils.currentMonthKey()),
        bestSavingAmount = 0.0,
        longestStreak = 0,
        moneyScore = 45,
        moneyTier = "Starter",
        unlockedAchievementCount = 0,
        totalXp = 0
    ),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalXp: Int = 0,
    val playerLevel: Int = 1,
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0,
    val lastSyncedEpochMs: Long = 0L,
    val isInitialLoading: Boolean = false,
    val isSyncingMonth: Boolean = false,
    val isPaginating: Boolean = false,
    val hasMoreTransactions: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val celebratedGoal: SavingsGoalItem? = null
)

private data class SyncFlags(
    val isInitialLoading: Boolean = false,
    val isSyncingMonth: Boolean = false,
    val isPaginating: Boolean = false,
    val hasMoreTransactions: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val celebratedGoal: SavingsGoalItem? = null
)

private data class SyncStatusMeta(
    val isOnline: Boolean,
    val pendingSyncCount: Int,
    val lastSyncedEpochMs: Long,
    val allCachedTransactions: List<TransactionItem>,
    val flags: SyncFlags
)

@OptIn(ExperimentalCoroutinesApi::class)
class LifeHubViewModel(
    private val repository: LifeHubRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedMonthKey = MutableStateFlow(DateAndFormatUtils.currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _syncFlags = MutableStateFlow(SyncFlags())
    private var latestPersistedAchievementCodes: Set<String> = emptySet()

    private val transactionsForSelectedMonth = _selectedMonthKey.flatMapLatest { monthKey ->
        repository.observeTransactionsForMonth(currentUserId, monthKey)
    }

    private val primaryFinancialFlow = combine(
        repository.observeProfile(currentUserId),
        repository.observeSettings(),
        _selectedMonthKey,
        transactionsForSelectedMonth,
        repository.observeHabits(currentUserId)
    ) { profile, settings, monthKey, transactions, habits ->
        PrimaryData(profile, settings, monthKey, transactions, habits)
    }

    private val secondaryModulesFlow = combine(
        repository.observeGoals(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeReminders(currentUserId),
        repository.observePersistedAchievementCodes(currentUserId)
    ) { goals, tasks, reminders, persistedAch ->
        latestPersistedAchievementCodes = persistedAch
        SecondaryData(goals, tasks, reminders, persistedAch)
    }

    private val syncStatusFlow = combine(
        repository.observeOnlineStatus(),
        repository.observePendingSyncCount(currentUserId),
        repository.observeLastSyncTime(currentUserId),
        repository.observeAllCachedTransactions(currentUserId),
        _syncFlags
    ) { online, pendingCount, lastSyncMs, allCachedTx, flags ->
        SyncStatusMeta(online, pendingCount, lastSyncMs, allCachedTx, flags)
    }

    val uiState: StateFlow<LifeHubUiState> = combine(
        primaryFinancialFlow,
        secondaryModulesFlow,
        syncStatusFlow
    ) { primary, secondary, syncMeta ->
        val separateTrips = primary.settings.separateTripEventLedger
        val dailyTransactions = if (separateTrips) {
            primary.transactions.filter { !it.isTripOrEvent }
        } else {
            primary.transactions
        }
        val tripEventTransactions = primary.transactions.filter { it.isTripOrEvent }
        val allTripEventTransactions = syncMeta.allCachedTransactions
            .filter { it.isTripOrEvent }
            .sortedByDescending { it.timestampEpochMs }
        val tripEventTotalExpense = allTripEventTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val tripEventBreakdown = allTripEventTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.eventName.ifBlank { "General Trip / Event" } }
            .map { (name, list) -> name to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val totalIncome = dailyTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = dailyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        val budget = primary.settings.monthlyBudget

        val moneyScore = MoneyScoreCalculator.calculate(
            transactions = dailyTransactions,
            monthlyBudget = budget,
            habits = primary.habits,
            goals = secondary.goals
        )

        val achievements = AchievementEngine.evaluate(
            transactions = primary.transactions,
            habits = primary.habits,
            goals = secondary.goals,
            tasks = secondary.tasks,
            monthlyBudget = budget,
            moneyScore = moneyScore.totalScore,
            persistedCodes = secondary.persistedAchievementCodes
        )
        val totalXp = AchievementEngine.calculateXp(achievements)
        val playerLevel = AchievementEngine.calculateLevel(totalXp)

        val categoryTotals = dailyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val topCategoryEntry = categoryTotals.maxByOrNull { it.value }
        val longestStreak = primary.habits.maxOfOrNull { maxOf(it.currentStreak, it.longestStreak) } ?: 0
        val monthLabel = DateAndFormatUtils.formatMonthLabel(primary.monthKey)
        val positiveSavings = netBalance.coerceAtLeast(0.0)

        // Yearly & Best-Saving Month Reports computed 100% locally from Room cache (0 Firestore reads!)
        val selectedYear = primary.monthKey.substringBefore("-").toIntOrNull() ?: 2026
        val yearPrefix = "$selectedYear-"
        val yearTx = syncMeta.allCachedTransactions.filter {
            it.monthKey.startsWith(yearPrefix) && (!separateTrips || !it.isTripOrEvent)
        }
        val yearlyReport = buildLocalYearlyReport(selectedYear, yearTx)

        val bestSavingMonthPoint = yearlyReport.monthlyBreakdown
            .maxByOrNull { (it.income - it.expense).coerceAtLeast(0.0) }
        val bestSavingMonthLabel = if (bestSavingMonthPoint != null &&
            (bestSavingMonthPoint.income - bestSavingMonthPoint.expense) > positiveSavings
        ) {
            DateAndFormatUtils.formatMonthLabel(bestSavingMonthPoint.monthKey)
        } else {
            monthLabel
        }
        val bestSavingAmount = maxOf(
            positiveSavings,
            bestSavingMonthPoint?.let { (it.income - it.expense).coerceAtLeast(0.0) } ?: 0.0,
            secondary.goals.sumOf { it.savedAmount }
        )

        val wrapped = MonthlyWrappedSummary(
            monthLabel = monthLabel,
            totalSpending = totalExpense,
            totalIncome = totalIncome,
            totalSavings = positiveSavings,
            topCategory = topCategoryEntry?.key,
            topCategoryAmount = topCategoryEntry?.value ?: 0.0,
            bestSavingMonthLabel = bestSavingMonthLabel,
            bestSavingAmount = bestSavingAmount,
            longestStreak = longestStreak,
            moneyScore = moneyScore.totalScore,
            moneyTier = moneyScore.tierLabel,
            unlockedAchievementCount = achievements.count { it.unlocked },
            totalXp = totalXp
        )

        LifeHubUiState(
            userId = currentUserId,
            profile = primary.profile.copy(totalXp = totalXp),
            settings = primary.settings,
            selectedMonthKey = primary.monthKey,
            transactions = primary.transactions,
            dailyTransactions = dailyTransactions,
            tripEventTransactions = tripEventTransactions,
            allTripEventTransactions = allTripEventTransactions,
            tripEventTotalExpense = tripEventTotalExpense,
            tripEventBreakdown = tripEventBreakdown,
            allCachedTransactionsCount = syncMeta.allCachedTransactions.size,
            yearlyReport = yearlyReport,
            habits = primary.habits,
            goals = secondary.goals,
            tasks = secondary.tasks,
            reminders = secondary.reminders,
            achievements = achievements,
            moneyScore = moneyScore,
            wrappedSummary = wrapped,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            totalXp = totalXp,
            playerLevel = playerLevel,
            isOnline = syncMeta.isOnline,
            pendingSyncCount = syncMeta.pendingSyncCount,
            lastSyncedEpochMs = syncMeta.lastSyncedEpochMs,
            isInitialLoading = syncMeta.flags.isInitialLoading,
            isSyncingMonth = syncMeta.flags.isSyncingMonth,
            isPaginating = syncMeta.flags.isPaginating,
            hasMoreTransactions = syncMeta.flags.hasMoreTransactions,
            isSaving = syncMeta.flags.isSaving,
            errorMessage = syncMeta.flags.errorMessage,
            snackbarMessage = syncMeta.flags.snackbarMessage,
            celebratedGoal = syncMeta.flags.celebratedGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LifeHubUiState(userId = currentUserId)
    )

    init {
        bootstrapSessionSmart()
        observeConnectivityForOfflineFlush()
    }

    /**
     * Shows cached Room data IMMEDIATELY, then performs cooldown-gated incremental delta sync
     * in the background. On repeat launches within cooldown, costs 0 Firestore reads!
     */
    private fun bootstrapSessionSmart() {
        viewModelScope.launch {
            val monthKey = _selectedMonthKey.value
            val cachedHasMore = repository.hasMoreForMonth(monthKey)
            _syncFlags.value = _syncFlags.value.copy(
                hasMoreTransactions = cachedHasMore,
                errorMessage = null
            )

            // Flush any offline writes left over from a previous session
            repository.flushPendingSyncQueue()
            repository.syncProfileAndSettingsSmart(forceRefresh = false)
            repository.syncCoreModulesSmart(manualRefresh = false, fullReconcile = false)
            val monthResult = repository.syncTransactionsSmart(
                monthKey = monthKey,
                manualRefresh = false,
                fullReconcile = false
            )
            _syncFlags.value = _syncFlags.value.copy(
                hasMoreTransactions = repository.hasMoreForMonth(monthKey),
                errorMessage = monthResult.exceptionOrNull()?.localizedMessage
            )
        }
    }

    /**
     * Automatically flushes queued offline changes when internet connectivity returns.
     * Strictly acyclic: only writes queued local mutations and never loops back into reads.
     */
    private fun observeConnectivityForOfflineFlush() {
        viewModelScope.launch {
            repository.observeOnlineStatus()
                .distinctUntilChanged()
                .collect { online ->
                    if (online) {
                        repository.flushPendingSyncQueue()
                    }
                }
        }
    }

    fun selectMonth(monthKey: String) {
        if (monthKey == _selectedMonthKey.value) return
        _selectedMonthKey.value = monthKey
        viewModelScope.launch {
            // Room immediately emits cached month data; Firestore is only touched if month was never cached
            val cachedHasMore = repository.hasMoreForMonth(monthKey)
            _syncFlags.value = _syncFlags.value.copy(
                isSyncingMonth = true,
                hasMoreTransactions = cachedHasMore,
                errorMessage = null
            )
            val result = repository.syncTransactionsSmart(
                monthKey = monthKey,
                manualRefresh = false,
                fullReconcile = false
            )
            _syncFlags.value = _syncFlags.value.copy(
                isSyncingMonth = false,
                hasMoreTransactions = repository.hasMoreForMonth(monthKey),
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun shiftMonth(delta: Int) {
        selectMonth(DateAndFormatUtils.shiftMonthKey(_selectedMonthKey.value, delta))
    }

    /**
     * Pull-to-refresh / manual sync: flushes offline outbox + runs incremental delta queries
     * (`updatedAt > watermark`) so unchanged documents are NOT re-read.
     */
    fun refreshAll() {
        if (_syncFlags.value.isSyncingMonth) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSyncingMonth = true, errorMessage = null)
            repository.flushPendingSyncQueue()
            repository.syncProfileAndSettingsSmart(forceRefresh = true)
            repository.syncCoreModulesSmart(manualRefresh = true, fullReconcile = false)
            val res = repository.syncTransactionsSmart(
                monthKey = _selectedMonthKey.value,
                manualRefresh = true,
                fullReconcile = false
            )
            val deltaCount = res.getOrNull()?.fetchedCount ?: 0
            _syncFlags.value = _syncFlags.value.copy(
                isSyncingMonth = false,
                hasMoreTransactions = repository.hasMoreForMonth(_selectedMonthKey.value),
                errorMessage = res.exceptionOrNull()?.localizedMessage,
                snackbarMessage = if (res.isSuccess) {
                    if (deltaCount > 0) "Synced $deltaCount updated entries" else "Up to date (0 new reads)"
                } else null
            )
        }
    }

    /**
     * Explicit full reconciliation available in Profile Settings (e.g. if user deleted items on another device).
     */
    fun forceFullResync() {
        if (_syncFlags.value.isSyncingMonth) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSyncingMonth = true, errorMessage = null)
            repository.flushPendingSyncQueue()
            repository.syncProfileAndSettingsSmart(forceRefresh = true)
            repository.syncCoreModulesSmart(manualRefresh = true, fullReconcile = true)
            val res = repository.syncTransactionsSmart(
                monthKey = _selectedMonthKey.value,
                manualRefresh = true,
                fullReconcile = true
            )
            _syncFlags.value = _syncFlags.value.copy(
                isSyncingMonth = false,
                hasMoreTransactions = repository.hasMoreForMonth(_selectedMonthKey.value),
                errorMessage = toUserSafeErrorMessage(res.exceptionOrNull()),
                snackbarMessage = if (res.isSuccess) "Full cloud reconciliation complete" else null
            )
        }
    }

    fun loadOlderTransactions() {
        val currentFlags = _syncFlags.value
        if (currentFlags.isPaginating || !currentFlags.hasMoreTransactions) return
        val monthKey = _selectedMonthKey.value
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isPaginating = true)
            val result = repository.loadNextTransactionsPage(monthKey)
            _syncFlags.value = _syncFlags.value.copy(
                isPaginating = false,
                hasMoreTransactions = repository.hasMoreForMonth(monthKey),
                errorMessage = toUserSafeErrorMessage(result.exceptionOrNull())
            )
        }
    }

    fun addTransaction(
        clientDocId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        timestampEpochMs: Long,
        note: String,
        isTripOrEvent: Boolean = false,
        eventName: String = "",
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.addTransaction(
                clientDocId = clientDocId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                timestampEpochMs = timestampEpochMs,
                note = note,
                isTripOrEvent = isTripOrEvent,
                eventName = eventName
            )
            val label = if (isTripOrEvent) "Trip/Event expense" else type.name.lowercase().replaceFirstChar { it.uppercase() }
            handleMutationResult(
                res,
                if (uiState.value.isOnline) "$label saved" else "$label saved offline (queued)"
            )
            if (res.isSuccess) {
                syncNewlyUnlockedAchievements()
                onDone()
            }
        }
    }

    fun updateTransaction(
        existing: TransactionItem,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        note: String,
        isTripOrEvent: Boolean = existing.isTripOrEvent,
        eventName: String = existing.eventName,
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.updateTransaction(
                existing = existing,
                title = title,
                amount = amount,
                type = type,
                category = category,
                note = note,
                isTripOrEvent = isTripOrEvent,
                eventName = eventName
            )
            handleMutationResult(
                res,
                if (uiState.value.isOnline) "Transaction updated" else "Updated offline (queued)"
            )
            if (res.isSuccess) {
                onDone()
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val res = repository.deleteTransaction(id)
            handleMutationResult(res, "Transaction deleted")
        }
    }

    fun addHabit(
        clientDocId: String,
        title: String,
        iconKey: String,
        colorHex: String,
        targetDaysPerWeek: Int,
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.addHabit(clientDocId, title, iconKey, colorHex, targetDaysPerWeek)
            handleMutationResult(res, "Habit created")
            if (res.isSuccess) onDone()
        }
    }

    fun toggleHabitToday(habit: HabitItem, dateKey: String = DateAndFormatUtils.currentDateKey()) {
        viewModelScope.launch {
            val res = repository.toggleHabitForDate(habit, dateKey)
            if (res.isSuccess) {
                syncNewlyUnlockedAchievements()
            } else if (res.exceptionOrNull() !is DuplicateWriteIgnoredException) {
                _syncFlags.value = _syncFlags.value.copy(
                    errorMessage = toUserSafeErrorMessage(res.exceptionOrNull())
                )
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            val res = repository.deleteHabit(habitId)
            handleMutationResult(res, "Habit removed")
        }
    }

    fun addGoal(
        clientDocId: String,
        title: String,
        category: GoalPreset,
        targetAmount: Double,
        initialSaved: Double,
        targetMonth: String,
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.addGoal(clientDocId, title, category, targetAmount, initialSaved, targetMonth)
            handleMutationResult(res, "Goal launched!")
            if (res.isSuccess) {
                syncNewlyUnlockedAchievements()
                onDone()
            }
        }
    }

    fun contributeToGoal(goal: SavingsGoalItem, amount: Double, onDone: () -> Unit = {}) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.contributeToGoal(goal, amount)
            val justCompleted = res.getOrNull() == true
            _syncFlags.value = _syncFlags.value.copy(
                isSaving = false,
                errorMessage = toUserSafeErrorMessage(
                    res.exceptionOrNull()?.takeIf { it !is DuplicateWriteIgnoredException }
                ),
                snackbarMessage = if (res.isSuccess && !justCompleted) "Contribution added!" else null,
                celebratedGoal = if (justCompleted) goal.copy(
                    savedAmount = goal.savedAmount + amount,
                    completed = true
                ) else _syncFlags.value.celebratedGoal
            )
            if (res.isSuccess) {
                syncNewlyUnlockedAchievements()
                onDone()
            }
        }
    }

    fun dismissCelebration() {
        _syncFlags.value = _syncFlags.value.copy(celebratedGoal = null)
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val res = repository.deleteGoal(goalId)
            handleMutationResult(res, "Goal deleted")
        }
    }

    fun addTask(
        clientDocId: String,
        title: String,
        cadence: TaskCadence,
        priority: TaskPriority,
        dueDateEpochMs: Long,
        recurring: Boolean,
        notes: String,
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.addTask(clientDocId, title, cadence, priority, dueDateEpochMs, recurring, notes)
            handleMutationResult(res, "Task added")
            if (res.isSuccess) onDone()
        }
    }

    fun moveTaskStatus(task: TaskItem, newStatus: TaskStatus) {
        viewModelScope.launch {
            val res = repository.updateTaskStatus(task, newStatus)
            if (res.isSuccess && newStatus == TaskStatus.DONE) {
                syncNewlyUnlockedAchievements()
            } else if (res.exceptionOrNull() != null && res.exceptionOrNull() !is DuplicateWriteIgnoredException) {
                _syncFlags.value = _syncFlags.value.copy(
                    errorMessage = toUserSafeErrorMessage(res.exceptionOrNull())
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val res = repository.deleteTask(taskId)
            handleMutationResult(res, "Task deleted")
        }
    }

    fun addReminder(
        clientDocId: String,
        title: String,
        category: ReminderCategory,
        amount: Double,
        dueDateEpochMs: Long,
        recurringMonthly: Boolean,
        onDone: () -> Unit = {}
    ) {
        if (_syncFlags.value.isSaving) return
        val settings = uiState.value.settings
        viewModelScope.launch {
            _syncFlags.value = _syncFlags.value.copy(isSaving = true, errorMessage = null)
            val res = repository.addReminder(
                clientDocId = clientDocId,
                title = title,
                category = category,
                amount = amount,
                dueDateEpochMs = dueDateEpochMs,
                recurringMonthly = recurringMonthly,
                currencySymbol = settings.currencySymbol,
                notificationsEnabled = settings.notificationsEnabled
            )
            handleMutationResult(res, "Reminder scheduled")
            if (res.isSuccess) onDone()
        }
    }

    fun toggleReminderPaid(reminder: ReminderItem) {
        val settings = uiState.value.settings
        viewModelScope.launch {
            val res = repository.toggleReminderPaid(
                reminder = reminder,
                currencySymbol = settings.currencySymbol,
                notificationsEnabled = settings.notificationsEnabled
            )
            handleMutationResult(res, if (!reminder.paid) "Marked paid" else "Marked unpaid")
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            val res = repository.deleteReminder(reminderId)
            handleMutationResult(res, "Reminder deleted")
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        val updated = uiState.value.settings.copy(themeMode = mode)
        viewModelScope.launch {
            repository.saveUserSettings(updated)
        }
    }

    fun updateBudgetAndCurrency(monthlyBudget: Double, currencySymbol: String) {
        val updated = uiState.value.settings.copy(
            monthlyBudget = monthlyBudget.coerceAtLeast(0.0),
            currencySymbol = currencySymbol.ifBlank { "₹" }
        )
        viewModelScope.launch {
            val res = repository.saveUserSettings(updated)
            handleMutationResult(res, "Budget & currency updated")
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        val updated = uiState.value.settings.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            repository.saveUserSettings(updated)
        }
    }

    fun updateSeparateTripEventLedger(enabled: Boolean) {
        val updated = uiState.value.settings.copy(separateTripEventLedger = enabled)
        viewModelScope.launch {
            val res = repository.saveUserSettings(updated)
            handleMutationResult(
                res,
                if (enabled) "Trip & Event expenses isolated from Daily totals"
                else "Trip & Event expenses included in Daily totals"
            )
        }
    }

    suspend fun getCachedTransactionsForExport(): List<TransactionItem> =
        repository.getAllCachedTransactions(currentUserId)

    fun clearSnackbar() {
        _syncFlags.value = _syncFlags.value.copy(snackbarMessage = null)
    }

    fun clearError() {
        _syncFlags.value = _syncFlags.value.copy(errorMessage = null)
    }

    suspend fun wipeLocalDataOnSignOut(context: Context) {
        repository.wipeAllLocalAccountData(context)
    }

    fun clearCachesOnSignOut() {
        repository.clearSessionCaches()
    }

    /**
     * Persists ONLY genuinely newly unlocked achievements (diffed against Room cache).
     * Eliminates the previous N Firestore `.get()` reads fired on every transaction/habit write!
     */
    private fun syncNewlyUnlockedAchievements() {
        val newlyUnlocked = uiState.value.achievements
            .filter { it.unlocked && it.code !in latestPersistedAchievementCodes }
            .map { it.code to it.xpReward }
        if (newlyUnlocked.isEmpty()) return
        viewModelScope.launch {
            repository.persistNewlyUnlockedAchievements(newlyUnlocked)
        }
    }

    private fun buildLocalYearlyReport(
        year: Int,
        yearTransactions: List<TransactionItem>
    ): YearlyReportSummary {
        val income = yearTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = yearTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val expenseTx = yearTransactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpSafe = expense.coerceAtLeast(1.0)
        val catBreakdown = expenseTx
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                Triple(cat, sum, (sum / totalExpSafe).toFloat())
            }
            .sortedByDescending { it.second }

        val byMonth = yearTransactions
            .groupBy { it.monthKey }
            .entries
            .sortedBy { it.key }
            .map { (mk, list) ->
                MonthlyTrendPoint(
                    monthKey = mk,
                    shortLabel = DateAndFormatUtils.formatMonthLabel(mk).substringBefore(" "),
                    income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                )
            }

        return YearlyReportSummary(
            year = year,
            totalIncome = income,
            totalExpense = expense,
            netSavings = income - expense,
            cachedMonthsCount = byMonth.size,
            transactionCount = yearTransactions.size,
            topCategory = catBreakdown.firstOrNull()?.first,
            topCategoryAmount = catBreakdown.firstOrNull()?.second ?: 0.0,
            categoryTotals = catBreakdown,
            monthlyBreakdown = byMonth
        )
    }

    private fun handleMutationResult(result: Result<Unit>, successMsg: String) {
        val err = result.exceptionOrNull()
        if (err is DuplicateWriteIgnoredException) {
            _syncFlags.value = _syncFlags.value.copy(isSaving = false)
            return
        }
        _syncFlags.value = _syncFlags.value.copy(
            isSaving = false,
            errorMessage = toUserSafeErrorMessage(err),
            snackbarMessage = if (result.isSuccess) successMsg else null
        )
    }

    private data class PrimaryData(
        val profile: UserProfile,
        val settings: UserSettings,
        val monthKey: String,
        val transactions: List<TransactionItem>,
        val habits: List<HabitItem>
    )

    private data class SecondaryData(
        val goals: List<SavingsGoalItem>,
        val tasks: List<TaskItem>,
        val reminders: List<ReminderItem>,
        val persistedAchievementCodes: Set<String>
    )
}
