package com.example.tripsplit.domain

import androidx.annotation.DrawableRes
import com.example.R

enum class TripSplitThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

enum class DestinationCover(
    val label: String,
    @DrawableRes val drawableRes: Int,
    val accentHex: Long
) {
    COASTAL("Amalfi Coast", R.drawable.img_ts_coastal_amalfi_1795718909160, 0xFF0D9488),
    ALPINE("Alpine Peaks", R.drawable.img_ts_alpine_peaks_1795718909667, 0xFF1D4ED8),
    KYOTO("Kyoto Lanterns", R.drawable.img_ts_kyoto_streets_1795718910348, 0xFFE85D2A),
    DESERT("Desert Camp", R.drawable.img_ts_desert_camp_1795718910873, 0xFFD97706);

    companion object {
        fun fromName(name: String): DestinationCover =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COASTAL
    }
}

enum class TripCategory(
    val label: String,
    val colorHex: Long
) {
    STAY("Stay & Lodging", 0xFF0B4F6C),
    DINING("Food & Dining", 0xFFE85D2A),
    TRANSPORT("Transit & Fuel", 0xFF0D9488),
    FLIGHTS("Flights & Rail", 0xFF2563EB),
    ACTIVITIES("Experiences", 0xFFD97706),
    SHOPPING("Souvenirs", 0xFF7C3AED),
    OTHER("Misc", 0xFF64748B);

    companion object {
        fun fromName(name: String): TripCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}

enum class SplitMode(val label: String) {
    EQUAL("Equal Split"),
    EXACT("Exact Amounts"),
    PERCENTAGE("Percentages");

    companion object {
        fun fromName(name: String): SplitMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EQUAL
    }
}

enum class WalletEntryType(val label: String) {
    DEPOSIT("Pool Deposit"),
    REFUND("Pool Refund");

    companion object {
        fun fromName(name: String): WalletEntryType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEPOSIT
    }
}

data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val origin: String,
    val startDateLabel: String,
    val endDateLabel: String,
    val totalBudget: Double,
    val currencySymbol: String,
    val cover: DestinationCover,
    val waypoints: List<String>,
    val createdAt: Long
)

data class TripMember(
    val id: String,
    val tripId: String,
    val name: String,
    val colorIndex: Int,
    val isSelf: Boolean
)

data class ExpenseShare(
    val memberId: String,
    val amount: Double
)

data class TripExpense(
    val id: String,
    val tripId: String,
    val title: String,
    val amount: Double,
    val category: TripCategory,
    val paidByMemberId: String?,     // null when paidFromWallet == true
    val paidFromWallet: Boolean,
    val splitMode: SplitMode,
    val shares: List<ExpenseShare>,
    val dateMillis: Long,
    val locationNote: String
)

data class WalletEntry(
    val id: String,
    val tripId: String,
    val memberId: String,
    val amount: Double,
    val type: WalletEntryType,
    val note: String,
    val createdAt: Long
)

data class SettlementRecord(
    val id: String,
    val tripId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Double,
    val note: String,
    val createdAt: Long
)

data class MemberBalance(
    val member: TripMember,
    val totalPaidDirect: Double,
    val totalShareDirect: Double,
    val walletContributed: Double,
    val netBalance: Double   // Positive = owed money; Negative = owes money
)

data class DebtTransfer(
    val fromMember: TripMember,
    val toMember: TripMember,
    val amount: Double
)

data class CategorySpend(
    val category: TripCategory,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class TripSummaryMetrics(
    val totalSpent: Double,
    val totalBudget: Double,
    val remainingBudget: Double,
    val budgetProgress: Float,
    val isOverBudget: Boolean,
    val walletDeposits: Double,
    val walletRefunds: Double,
    val walletExpenses: Double,
    val walletBalance: Double,
    val memberBalances: List<MemberBalance>,
    val simplifiedDebts: List<DebtTransfer>,
    val categoryBreakdown: List<CategorySpend>,
    val averagePerPerson: Double
)
