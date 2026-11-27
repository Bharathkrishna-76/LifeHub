package com.example.tripsplit.domain

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

object DebtSimplifier {

    private const val EPSILON = 0.01

    fun computeMetrics(
        trip: Trip,
        members: List<TripMember>,
        expenses: List<TripExpense>,
        walletEntries: List<WalletEntry>,
        settlements: List<SettlementRecord>
    ): TripSummaryMetrics {
        val totalSpent = expenses.sumOf { it.amount }
        val totalBudget = trip.totalBudget.coerceAtLeast(0.0)
        val remainingBudget = totalBudget - totalSpent
        val budgetProgress = if (totalBudget > 0.0) {
            (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        val isOverBudget = totalBudget > 0.0 && totalSpent > totalBudget

        val walletDeposits = walletEntries
            .filter { it.type == WalletEntryType.DEPOSIT }
            .sumOf { it.amount }
        val walletRefunds = walletEntries
            .filter { it.type == WalletEntryType.REFUND }
            .sumOf { it.amount }
        val walletExpenses = expenses
            .filter { it.paidFromWallet }
            .sumOf { it.amount }
        val walletBalance = round2(walletDeposits - walletExpenses - walletRefunds)

        val paidMap = mutableMapOf<String, Double>()
        val shareMap = mutableMapOf<String, Double>()
        val netMap = mutableMapOf<String, Double>()
        val walletContribMap = mutableMapOf<String, Double>()

        members.forEach { m ->
            paidMap[m.id] = 0.0
            shareMap[m.id] = 0.0
            netMap[m.id] = 0.0
            walletContribMap[m.id] = 0.0
        }

        walletEntries.forEach { entry ->
            val sign = if (entry.type == WalletEntryType.DEPOSIT) 1.0 else -1.0
            walletContribMap[entry.memberId] = (walletContribMap[entry.memberId] ?: 0.0) + sign * entry.amount
        }

        // Peer-to-peer balances from direct member-paid expenses
        expenses.filter { !it.paidFromWallet && it.paidByMemberId != null }.forEach { exp ->
            val payerId = exp.paidByMemberId!!
            paidMap[payerId] = (paidMap[payerId] ?: 0.0) + exp.amount
            netMap[payerId] = (netMap[payerId] ?: 0.0) + exp.amount

            exp.shares.forEach { share ->
                shareMap[share.memberId] = (shareMap[share.memberId] ?: 0.0) + share.amount
                netMap[share.memberId] = (netMap[share.memberId] ?: 0.0) - share.amount
            }
        }

        // Apply recorded peer-to-peer settlements:
        // When fromMember pays toMember, fromMember's debt shrinks (+amount) and toMember's receivable shrinks (-amount).
        settlements.forEach { record ->
            netMap[record.fromMemberId] = (netMap[record.fromMemberId] ?: 0.0) + record.amount
            netMap[record.toMemberId] = (netMap[record.toMemberId] ?: 0.0) - record.amount
        }

        val memberBalances = members.map { member ->
            val rawNet = round2(netMap[member.id] ?: 0.0)
            MemberBalance(
                member = member,
                totalPaidDirect = round2(paidMap[member.id] ?: 0.0),
                totalShareDirect = round2(shareMap[member.id] ?: 0.0),
                walletContributed = round2(walletContribMap[member.id] ?: 0.0),
                netBalance = if (abs(rawNet) < EPSILON) 0.0 else rawNet
            )
        }.sortedByDescending { it.netBalance }

        val simplifiedDebts = simplifyDebts(memberBalances)

        val categoryBreakdown = expenses
            .groupBy { it.category }
            .map { (category, list) ->
                val sum = round2(list.sumOf { it.amount })
                CategorySpend(
                    category = category,
                    amount = sum,
                    percentage = if (totalSpent > 0.0) (sum / totalSpent).toFloat().coerceIn(0f, 1f) else 0f,
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }

        val avgPerPerson = if (members.isNotEmpty()) round2(totalSpent / members.size) else 0.0

        return TripSummaryMetrics(
            totalSpent = round2(totalSpent),
            totalBudget = round2(totalBudget),
            remainingBudget = round2(remainingBudget),
            budgetProgress = budgetProgress,
            isOverBudget = isOverBudget,
            walletDeposits = round2(walletDeposits),
            walletRefunds = round2(walletRefunds),
            walletExpenses = round2(walletExpenses),
            walletBalance = walletBalance,
            memberBalances = memberBalances,
            simplifiedDebts = simplifiedDebts,
            categoryBreakdown = categoryBreakdown,
            averagePerPerson = avgPerPerson
        )
    }

    /**
     * Greedy minimum-transfer debt simplification:
     * Matches largest debtor against largest creditor iteratively until all nets settle.
     */
    fun simplifyDebts(balances: List<MemberBalance>): List<DebtTransfer> {
        val creditors = balances
            .filter { it.netBalance > EPSILON }
            .map { it.member to it.netBalance }
            .sortedByDescending { it.second }
            .toMutableList()

        val debtors = balances
            .filter { it.netBalance < -EPSILON }
            .map { it.member to abs(it.netBalance) }
            .sortedByDescending { it.second }
            .toMutableList()

        val transfers = mutableListOf<DebtTransfer>()
        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val (creditor, creditAmt) = creditors[cIdx]
            val (debtor, debtAmt) = debtors[dIdx]

            val transfer = round2(min(creditAmt, debtAmt))
            if (transfer >= EPSILON) {
                transfers += DebtTransfer(
                    fromMember = debtor,
                    toMember = creditor,
                    amount = transfer
                )
            }

            val newCredit = round2(creditAmt - transfer)
            val newDebt = round2(debtAmt - transfer)

            if (newCredit < EPSILON) {
                cIdx++
            } else {
                creditors[cIdx] = creditor to newCredit
            }

            if (newDebt < EPSILON) {
                dIdx++
            } else {
                debtors[dIdx] = debtor to newDebt
            }
        }

        return transfers
    }

    fun buildEqualShares(totalAmount: Double, participantIds: List<String>): List<ExpenseShare> {
        if (participantIds.isEmpty()) return emptyList()
        val count = participantIds.size
        val base = kotlin.math.floor((totalAmount / count) * 100.0) / 100.0
        val remainder = round2(totalAmount - base * count)
        return participantIds.mapIndexed { index, memberId ->
            val share = if (index == 0) round2(base + remainder) else base
            ExpenseShare(memberId = memberId, amount = share)
        }
    }

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
