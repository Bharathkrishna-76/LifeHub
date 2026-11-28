package com.example.tripsplit.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.data.TripSplitDatabase
import com.example.tripsplit.data.TripSplitPreferences
import com.example.tripsplit.data.TripSplitRepository
import com.example.tripsplit.domain.DebtSimplifier
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.ExpenseShare
import com.example.tripsplit.domain.SettlementRecord
import com.example.tripsplit.domain.SplitMode
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.domain.TripSplitThemePreference
import com.example.tripsplit.domain.TripSummaryMetrics
import com.example.tripsplit.domain.WalletEntry
import com.example.tripsplit.domain.WalletEntryType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TripCardOverview(
    val trip: Trip,
    val memberCount: Int,
    val totalSpent: Double,
    val expenseCount: Int
)

data class TripSplitUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val themePreference: TripSplitThemePreference = TripSplitThemePreference.LIGHT,
    val favoriteTripIds: Set<String> = emptySet(),
    val allTrips: List<TripCardOverview> = emptyList(),
    val activeTrip: Trip? = null,
    val members: List<TripMember> = emptyList(),
    val expenses: List<TripExpense> = emptyList(),
    val walletEntries: List<WalletEntry> = emptyList(),
    val settlements: List<SettlementRecord> = emptyList(),
    val metrics: TripSummaryMetrics? = null,
    val statusBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripSplitViewModel(
    private val repository: TripSplitRepository,
    private val preferences: TripSplitPreferences
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    private val tripsWithSummaries = combine(
        repository.observeTrips(),
        repository.observeAllMembers(),
        repository.observeAllExpenses()
    ) { trips, allMembers, allExpenses ->
        val membersByTrip = allMembers.groupBy { it.tripId }
        val expensesByTrip = allExpenses.groupBy { it.tripId }
        trips.map { trip ->
            val tripExpenses = expensesByTrip[trip.id].orEmpty()
            TripCardOverview(
                trip = trip,
                memberCount = membersByTrip[trip.id]?.size ?: 0,
                totalSpent = DebtSimplifier.round2(tripExpenses.sumOf { it.amount }),
                expenseCount = tripExpenses.size
            )
        }
    }

    private val activeTripFlow = combine(
        repository.observeTrips(),
        preferences.activeTripId
    ) { trips, savedId ->
        trips.firstOrNull { it.id == savedId } ?: trips.firstOrNull()
    }

    private val activeTripDetailsFlow = activeTripFlow.flatMapLatest { trip ->
        if (trip == null) {
            flowOf(ActiveTripBundle(null, emptyList(), emptyList(), emptyList(), emptyList(), null))
        } else {
            combine(
                repository.observeMembers(trip.id),
                repository.observeExpenses(trip.id),
                repository.observeWalletEntries(trip.id),
                repository.observeSettlements(trip.id)
            ) { members, expenses, walletEntries, settlements ->
                val metrics = DebtSimplifier.computeMetrics(
                    trip = trip,
                    members = members,
                    expenses = expenses,
                    walletEntries = walletEntries,
                    settlements = settlements
                )
                ActiveTripBundle(trip, members, expenses, walletEntries, settlements, metrics)
            }
        }
    }

    private val metaFlow = combine(
        preferences.themePreference,
        preferences.favoriteTripIds,
        _isRefreshing,
        _statusBanner
    ) { theme, favs, refreshing, banner ->
        UiMeta(theme, favs, refreshing, banner)
    }

    val uiState: StateFlow<TripSplitUiState> = combine(
        metaFlow,
        tripsWithSummaries,
        activeTripDetailsFlow
    ) { meta, overviews, bundle ->
        TripSplitUiState(
            isLoading = false,
            isRefreshing = meta.isRefreshing,
            themePreference = meta.theme,
            favoriteTripIds = meta.favoriteTripIds,
            allTrips = overviews,
            activeTrip = bundle.trip,
            members = bundle.members,
            expenses = bundle.expenses,
            walletEntries = bundle.walletEntries,
            settlements = bundle.settlements,
            metrics = bundle.metrics,
            statusBanner = meta.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TripSplitUiState(themePreference = preferences.themePreference.value)
    )

    fun toggleTheme() {
        preferences.toggleLightDark()
    }

    fun selectTrip(tripId: String) {
        preferences.setActiveTripId(tripId)
    }

    fun toggleFavoriteTrip(tripId: String) {
        preferences.toggleFavoriteTrip(tripId)
    }

    fun refreshDashboard() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(450L)
            _isRefreshing.value = false
            _statusBanner.value = "Travel Nest ledger up to date"
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun buildShareSummaryText(): String {
        val state = uiState.value
        val trip = state.activeTrip ?: return "Planning a trip with Travel Nest!"
        val metrics = state.metrics ?: return "Trip: ${trip.title} (${trip.destination})"
        val sym = trip.currencySymbol
        return buildString {
            appendLine("Travel Nest Summary — ${trip.title}")
            appendLine("Route: ${trip.origin} -> ${trip.destination} (${trip.startDateLabel} - ${trip.endDateLabel})")
            appendLine("Total Spent: ${formatTripMoney(metrics.totalSpent, sym)} / Budget: ${formatTripMoney(metrics.totalBudget, sym)}")
            appendLine("Common Wallet Balance: ${formatTripMoney(metrics.walletBalance, sym)}")
            if (metrics.simplifiedDebts.isEmpty()) {
                appendLine("Settlements: Everyone is all settled up!")
            } else {
                appendLine("Who Owes Whom:")
                metrics.simplifiedDebts.forEach { t ->
                    appendLine("• ${t.fromMember.name} -> ${t.toMember.name}: ${formatTripMoney(t.amount, sym)}")
                }
            }
        }.trim()
    }

    fun createTrip(
        title: String,
        destination: String,
        origin: String,
        startDateLabel: String,
        endDateLabel: String,
        totalBudget: Double,
        currencySymbol: String,
        cover: DestinationCover,
        waypoints: List<String>,
        travelerNames: List<String>,
        initialWalletDeposit: Double
    ) {
        val cleanTitle = title.trim().take(80).ifEmpty { destination.trim().take(80).ifEmpty { "New Trip" } }
        val cleanDest = destination.trim().take(80).ifEmpty { cleanTitle }
        val cleanOrigin = origin.trim().take(80).ifEmpty { "Home Base" }
        val safeBudget = if (totalBudget.isFinite()) totalBudget.coerceIn(0.0, 100_000_000.0) else 0.0
        val safeDeposit = if (initialWalletDeposit.isFinite()) initialWalletDeposit.coerceIn(0.0, 100_000_000.0) else 0.0
        val tripId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val cleanedTravelers = travelerNames
            .map { it.trim().take(50) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(50)
            .ifEmpty { listOf("You") }

        val trip = Trip(
            id = tripId,
            title = cleanTitle,
            destination = cleanDest,
            origin = cleanOrigin,
            startDateLabel = startDateLabel.trim().take(32).ifEmpty { "Flexible" },
            endDateLabel = endDateLabel.trim().take(32).ifEmpty { "Open" },
            totalBudget = safeBudget,
            currencySymbol = currencySymbol.trim().take(4).ifBlank { "₹" },
            cover = cover,
            waypoints = waypoints.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(20),
            createdAt = now
        )

        val members = cleanedTravelers.mapIndexed { idx, name ->
            TripMember(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                name = name,
                colorIndex = idx % 6,
                isSelf = idx == 0
            )
        }

        viewModelScope.launch {
            repository.createTripWithMembers(trip, members)
            if (safeDeposit > 0.0 && members.isNotEmpty()) {
                val perMember = DebtSimplifier.round2(safeDeposit / members.size)
                members.forEachIndexed { idx, member ->
                    val depositAmt = if (idx == 0) {
                        DebtSimplifier.round2(safeDeposit - perMember * (members.size - 1))
                    } else {
                        perMember
                    }
                    if (depositAmt > 0.0) {
                        repository.upsertWalletEntry(
                            WalletEntry(
                                id = UUID.randomUUID().toString(),
                                tripId = tripId,
                                memberId = member.id,
                                amount = depositAmt,
                                type = WalletEntryType.DEPOSIT,
                                note = "Initial trip pool",
                                createdAt = now + idx
                            )
                        )
                    }
                }
            }
            preferences.setActiveTripId(tripId)
            _statusBanner.value = "Trip \"$cleanTitle\" ready for departure"
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
            if (preferences.activeTripId.value == tripId) {
                preferences.setActiveTripId(null)
            }
            _statusBanner.value = "Trip removed"
        }
    }

    fun addTraveler(name: String) {
        val trip = uiState.value.activeTrip ?: return
        val trimmed = name.trim().take(50)
        if (trimmed.isEmpty() || uiState.value.members.size >= 50) return
        val existingCount = uiState.value.members.size
        val member = TripMember(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            name = trimmed,
            colorIndex = existingCount % 6,
            isSelf = existingCount == 0
        )
        viewModelScope.launch {
            repository.upsertMember(member)
            _statusBanner.value = "$trimmed joined ${trip.title}"
        }
    }

    fun removeTraveler(memberId: String) {
        val hasExpenses = uiState.value.expenses.any { exp ->
            exp.paidByMemberId == memberId || exp.shares.any { it.memberId == memberId }
        }
        if (hasExpenses) {
            _statusBanner.value = "Cannot remove traveler with logged expenses"
            return
        }
        viewModelScope.launch {
            repository.deleteMember(memberId)
            _statusBanner.value = "Traveler removed"
        }
    }

    fun saveExpense(
        existingId: String? = null,
        title: String,
        amount: Double,
        category: TripCategory,
        paidByMemberId: String?,
        paidFromWallet: Boolean,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        locationNote: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        val cleanTitle = title.trim().take(80)
        if (!amount.isFinite() || amount <= 0.0 || amount > 100_000_000.0 || cleanTitle.isBlank()) return

        val finalShares = if (shares.isEmpty()) {
            DebtSimplifier.buildEqualShares(amount, uiState.value.members.map { it.id })
        } else {
            shares.filter { it.amount.isFinite() && it.amount >= 0.0 }
        }

        val expense = TripExpense(
            id = existingId ?: UUID.randomUUID().toString(),
            tripId = trip.id,
            title = cleanTitle,
            amount = DebtSimplifier.round2(amount),
            category = category,
            paidByMemberId = if (paidFromWallet) null else paidByMemberId,
            paidFromWallet = paidFromWallet,
            splitMode = splitMode,
            shares = finalShares,
            dateMillis = System.currentTimeMillis(),
            locationNote = locationNote.trim().take(120)
        )
        viewModelScope.launch {
            repository.upsertExpense(expense)
            _statusBanner.value = if (paidFromWallet) {
                "Logged from Common Wallet"
            } else {
                "Expense split across ${finalShares.size} traveler${if (finalShares.size == 1) "" else "s"}"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _statusBanner.value = "Expense removed"
        }
    }

    fun addWalletEntry(
        memberId: String,
        amount: Double,
        type: WalletEntryType,
        note: String
    ) {
        val trip = uiState.value.activeTrip ?: return
        if (!amount.isFinite() || amount <= 0.0 || amount > 100_000_000.0) return
        val entry = WalletEntry(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            memberId = memberId,
            amount = DebtSimplifier.round2(amount),
            type = type,
            note = note.trim().take(120).ifEmpty { type.label },
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.upsertWalletEntry(entry)
            _statusBanner.value = if (type == WalletEntryType.DEPOSIT) {
                "Common Wallet topped up"
            } else {
                "Wallet refund recorded"
            }
        }
    }

    fun deleteWalletEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteWalletEntry(entryId)
            _statusBanner.value = "Wallet entry removed"
        }
    }

    fun recordSettlement(
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        note: String = "Settled up"
    ) {
        val trip = uiState.value.activeTrip ?: return
        if (!amount.isFinite() || amount <= 0.0 || amount > 100_000_000.0 || fromMemberId == toMemberId) return
        val settlement = SettlementRecord(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            amount = DebtSimplifier.round2(amount),
            note = note.trim().take(120).ifEmpty { "Settled up" },
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.upsertSettlement(settlement)
            _statusBanner.value = "Settlement recorded"
        }
    }

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            repository.deleteSettlement(settlementId)
            _statusBanner.value = "Settlement undone"
        }
    }

    private data class UiMeta(
        val theme: TripSplitThemePreference,
        val favoriteTripIds: Set<String>,
        val isRefreshing: Boolean,
        val banner: String?
    )

    private data class ActiveTripBundle(
        val trip: Trip?,
        val members: List<TripMember>,
        val expenses: List<TripExpense>,
        val walletEntries: List<WalletEntry>,
        val settlements: List<SettlementRecord>,
        val metrics: TripSummaryMetrics?
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = TripSplitDatabase.getInstance(context)
            val repo = TripSplitRepository(db.tripSplitDao())
            val prefs = TripSplitPreferences.getInstance(context)
            return TripSplitViewModel(repo, prefs) as T
        }
    }
}
