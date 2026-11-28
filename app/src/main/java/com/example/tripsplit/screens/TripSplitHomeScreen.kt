package com.example.tripsplit.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.tripsplit.components.CompactTripRouteStrip
import com.example.tripsplit.components.DestinationThemeGrid
import com.example.tripsplit.components.InitialsAvatar
import com.example.tripsplit.components.TravelerMonogramBadge
import com.example.tripsplit.components.TripPrimaryButton
import com.example.tripsplit.components.TripSageCard
import com.example.tripsplit.components.TripSectionHeader
import com.example.tripsplit.components.TripShimmerBlock
import com.example.tripsplit.components.TripStatusBadge
import com.example.tripsplit.components.TripSurfaceCard
import com.example.tripsplit.components.formatTripDate
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.components.travelerBadgeColor
import com.example.tripsplit.domain.DebtTransfer
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripSummaryMetrics
import com.example.tripsplit.domain.WalletEntryType
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import com.example.tripsplit.viewmodel.TripCardOverview
import com.example.tripsplit.viewmodel.TripSplitUiState
import com.example.ui.components.rememberResponsiveMetrics
import java.util.Calendar
import kotlin.math.abs

private data class RecentActivityItem(
    val id: String,
    val actorName: String,
    val actorColorIndex: Int,
    val description: String,
    val timestampMillis: Long,
    val amount: Double,
    val isPositive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSplitHomeScreen(
    uiState: TripSplitUiState,
    travelerFirstName: String,
    onCreateTripClick: (DestinationCover?) -> Unit,
    onAddExpenseClick: () -> Unit,
    onTopUpWalletClick: () -> Unit,
    onInviteMembersClick: () -> Unit,
    onOpenTripsTab: () -> Unit,
    onOpenExpensesTab: () -> Unit,
    onOpenSquadTab: () -> Unit,
    onSelectTrip: (String) -> Unit,
    onSettleTransfer: (DebtTransfer) -> Unit,
    onEditExpense: (TripExpense) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShareTripSummary: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    val greetingLine = remember(travelerFirstName) { buildCompactGreeting(travelerFirstName) }

    if (uiState.isLoading) {
        HomeShimmerLoading(modifier = modifier)
        return
    }

    val activeTrip = uiState.activeTrip
    val metrics = uiState.metrics

    if (activeTrip == null || metrics == null) {
        TripSplitEmptyHome(
            greetingLine = greetingLine,
            onCreateTripClick = onCreateTripClick,
            modifier = modifier
        )
        return
    }

    val membersById = remember(uiState.members) { uiState.members.associateBy { it.id } }
    val selfBalance = remember(metrics.memberBalances) {
        metrics.memberBalances.firstOrNull { it.member.isSelf } ?: metrics.memberBalances.firstOrNull()
    }
    val recentActivities = remember(uiState.expenses, uiState.walletEntries, uiState.settlements, membersById) {
        buildList {
            uiState.expenses.forEach { exp ->
                val payer = exp.paidByMemberId?.let { membersById[it] }
                val actor = if (exp.paidFromWallet) "Wallet" else (payer?.name ?: "Member")
                add(
                    RecentActivityItem(
                        id = "exp_${exp.id}",
                        actorName = actor,
                        actorColorIndex = payer?.colorIndex ?: 0,
                        description = if (exp.paidFromWallet) "${exp.title} (Wallet)" else "$actor · ${exp.title}",
                        timestampMillis = exp.dateMillis,
                        amount = exp.amount
                    )
                )
            }
            uiState.walletEntries.forEach { w ->
                val member = membersById[w.memberId]
                val name = member?.name ?: "Member"
                add(
                    RecentActivityItem(
                        id = "wal_${w.id}",
                        actorName = name,
                        actorColorIndex = member?.colorIndex ?: 1,
                        description = if (w.type == WalletEntryType.DEPOSIT) "$name added to Trip Wallet" else "$name wallet refund",
                        timestampMillis = w.createdAt,
                        amount = w.amount,
                        isPositive = w.type == WalletEntryType.DEPOSIT
                    )
                )
            }
            uiState.settlements.forEach { s ->
                val from = membersById[s.fromMemberId]
                val to = membersById[s.toMemberId]
                add(
                    RecentActivityItem(
                        id = "set_${s.id}",
                        actorName = from?.name ?: "Member",
                        actorColorIndex = from?.colorIndex ?: 2,
                        description = "${from?.name ?: "Member"} settled with ${to?.name ?: "Member"}",
                        timestampMillis = s.createdAt,
                        amount = s.amount,
                        isPositive = true
                    )
                )
            }
        }.sortedByDescending { it.timestampMillis }.take(5)
    }

    val isFavorite = uiState.favoriteTripIds.contains(activeTrip.id)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val responsive = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = responsive.horizontalPadding,
                    end = responsive.horizontalPadding,
                    top = 8.dp,
                    bottom = 92.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. Small Single-Line Greeting + Inline New Trip action
            item(key = "greeting_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = greetingLine,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCreateTripClick(null) }
                            .testTag("ts_home_create_trip_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = colors.accentSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.ts_action_create_trip),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.accent
                            )
                        }
                    }
                }
            }

            // 2. Compact Active Trip Card (horizontal editorial thumbnail + budget progress)
            item(key = "active_trip_card") {
                CompactActiveTripCard(
                    trip = activeTrip,
                    memberCount = uiState.members.size,
                    metrics = metrics,
                    isFavorite = isFavorite,
                    isNarrowPhone = responsive.isNarrowPhone,
                    stackBudgetRow = responsive.shouldStackTightPairs,
                    onClick = onOpenTripsTab,
                    onToggleFavorite = { onToggleFavorite(activeTrip.id) },
                    onShare = onShareTripSummary
                )
            }

            // 3. Adaptive Quick Actions Grid (2x2 on narrow/large-font screens, 4-column otherwise)
            item(key = "quick_actions_grid") {
                if (responsive.shouldStackTightPairs) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactQuickActionButton(
                                label = "Add Expense",
                                icon = Icons.Rounded.ReceiptLong,
                                iconTint = colors.accent,
                                iconBg = colors.accentSoft,
                                onClick = onAddExpenseClick,
                                testTag = "ts_quick_add_expense",
                                modifier = Modifier.weight(1f)
                            )
                            CompactQuickActionButton(
                                label = "Settle Up",
                                icon = Icons.Rounded.Handshake,
                                iconTint = colors.forest,
                                iconBg = colors.forestSoft,
                                onClick = onOpenSquadTab,
                                testTag = "ts_quick_settle_up",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactQuickActionButton(
                                label = "Trip Wallet",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconTint = colors.forest,
                                iconBg = colors.forestSoft,
                                onClick = onTopUpWalletClick,
                                testTag = "ts_quick_top_up_wallet",
                                modifier = Modifier.weight(1f)
                            )
                            CompactQuickActionButton(
                                label = "Invite",
                                icon = Icons.Rounded.GroupAdd,
                                iconTint = colors.inkPrimary,
                                iconBg = colors.surfaceMuted,
                                onClick = onInviteMembersClick,
                                testTag = "ts_quick_add_traveler",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactQuickActionButton(
                            label = "Add Expense",
                            icon = Icons.Rounded.ReceiptLong,
                            iconTint = colors.accent,
                            iconBg = colors.accentSoft,
                            onClick = onAddExpenseClick,
                            testTag = "ts_quick_add_expense",
                            modifier = Modifier.weight(1f)
                        )
                        CompactQuickActionButton(
                            label = "Settle Up",
                            icon = Icons.Rounded.Handshake,
                            iconTint = colors.forest,
                            iconBg = colors.forestSoft,
                            onClick = onOpenSquadTab,
                            testTag = "ts_quick_settle_up",
                            modifier = Modifier.weight(1f)
                        )
                        CompactQuickActionButton(
                            label = "Trip Wallet",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconTint = colors.forest,
                            iconBg = colors.forestSoft,
                            onClick = onTopUpWalletClick,
                            testTag = "ts_quick_top_up_wallet",
                            modifier = Modifier.weight(1f)
                        )
                        CompactQuickActionButton(
                            label = "Invite",
                            icon = Icons.Rounded.GroupAdd,
                            iconTint = colors.inkPrimary,
                            iconBg = colors.surfaceMuted,
                            onClick = onInviteMembersClick,
                            testTag = "ts_quick_add_traveler",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Compact 2x2 Financial Overview Block
            item(key = "financial_overview") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripSectionHeader(title = stringResource(R.string.ts_section_budget_spend))
                    FinancialOverviewGridCard(
                        metrics = metrics,
                        yourShare = selfBalance?.totalShareDirect ?: metrics.averagePerPerson,
                        yourNetBalance = selfBalance?.netBalance ?: 0.0,
                        currencySymbol = activeTrip.currencySymbol
                    )
                }
            }

            // 5. Pending Settlements Strip (only when someone owes someone)
            if (metrics.simplifiedDebts.isNotEmpty()) {
                item(key = "pending_settlements") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripSectionHeader(
                            title = stringResource(R.string.ts_section_who_owes_whom),
                            actionLabel = "All",
                            actionTestTag = "ts_home_open_settlements",
                            onActionClick = onOpenSquadTab
                        )
                        TripSurfaceCard(padding = 0.dp) {
                            metrics.simplifiedDebts.take(3).forEachIndexed { idx, transfer ->
                                if (idx > 0) {
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                }
                                CompactSettlementRow(
                                    transfer = transfer,
                                    currencySymbol = activeTrip.currencySymbol,
                                    onSettle = { onSettleTransfer(transfer) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Compact Route Strip
            item(key = "route_strip") {
                CompactTripRouteStrip(trip = activeTrip)
            }

            // 7. Your Trips Preview List
            if (uiState.allTrips.isNotEmpty()) {
                item(key = "your_trips_preview") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripSectionHeader(
                            title = "Your Trips",
                            actionLabel = "View All",
                            actionTestTag = "ts_home_view_all_trips",
                            onActionClick = onOpenTripsTab
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.allTrips.take(3).forEach { overview ->
                                CompactTripListItem(
                                    overview = overview,
                                    isActive = overview.trip.id == activeTrip.id,
                                    onClick = { onSelectTrip(overview.trip.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 8. Recent Activity Feed (unified flat bordered list)
            item(key = "recent_activity") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripSectionHeader(
                        title = stringResource(R.string.ts_section_recent_expenses),
                        actionLabel = stringResource(R.string.ts_action_view_expenses),
                        actionTestTag = "ts_home_view_all_expenses",
                        onActionClick = onOpenExpensesTab
                    )
                    if (recentActivities.isEmpty()) {
                        TripSageCard(padding = 16.dp) {
                            Text(
                                text = stringResource(R.string.ts_no_expenses_yet),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log your first shared meal, stay, or wallet deposit to see activity here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                    } else {
                        TripSurfaceCard(padding = 0.dp) {
                            recentActivities.forEachIndexed { idx, act ->
                                if (idx > 0) {
                                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (act.id.startsWith("exp_")) {
                                                val rawId = act.id.removePrefix("exp_")
                                                uiState.expenses.firstOrNull { it.id == rawId }?.let(onEditExpense)
                                            } else {
                                                onOpenSquadTab()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InitialsAvatar(
                                        name = act.actorName,
                                        backgroundColor = travelerBadgeColor(act.actorColorIndex),
                                        size = 34.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = act.description,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.inkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatTripDate(act.timestampMillis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.inkMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTripMoney(act.amount, activeTrip.currencySymbol),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = TripMonoFont,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (act.isPositive) colors.forest else colors.inkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CompactActiveTripCard(
    trip: Trip,
    memberCount: Int,
    metrics: TripSummaryMetrics,
    isFavorite: Boolean,
    isNarrowPhone: Boolean,
    stackBudgetRow: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    val progressColor = when {
        metrics.isOverBudget -> colors.danger
        metrics.budgetProgress > 0.8f -> colors.warning
        else -> colors.forest
    }
    val thumbSize = if (isNarrowPhone) 72.dp else 88.dp

    TripSurfaceCard(
        modifier = Modifier.testTag("ts_featured_trip_card"),
        padding = 12.dp,
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = trip.cover.drawableRes),
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TripStatusBadge(
                        text = "Active Trip",
                        backgroundColor = colors.forestSoft,
                        contentColor = colors.forest,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onToggleFavorite)
                                .testTag("ts_hero_favorite_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(R.string.ts_favorite_trip_cd),
                                tint = if (isFavorite) colors.accent else colors.inkMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onShare)
                                .testTag("ts_hero_share_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.ts_share_trip_cd),
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = trip.destination,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = colors.inkMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$memberCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = colors.inkMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${trip.startDateLabel} – ${trip.endDateLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        val budgetText = if (metrics.totalBudget > 0.0) {
            "Budget ${formatTripMoney(metrics.totalBudget, trip.currencySymbol)} (${(metrics.budgetProgress * 100).toInt()}%)"
        } else {
            "No budget limit"
        }

        if (stackBudgetRow) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Spent ${formatTripMoney(metrics.totalSpent, trip.currencySymbol)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = budgetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (metrics.isOverBudget) colors.danger else colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent ${formatTripMoney(metrics.totalSpent, trip.currencySymbol)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = budgetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (metrics.isOverBudget) colors.danger else colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surfaceMuted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(metrics.budgetProgress.coerceAtLeast(if (metrics.totalSpent > 0) 0.03f else 0f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(progressColor)
            )
        }
    }
}

@Composable
private fun CompactQuickActionButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialOverviewGridCard(
    metrics: TripSummaryMetrics,
    yourShare: Double,
    yourNetBalance: Double,
    currencySymbol: String
) {
    val colors = LocalTripSplitColors.current
    val balanceLabel = when {
        yourNetBalance > 0.01 -> "You Are Owed"
        yourNetBalance < -0.01 -> "You Owe"
        else -> "Your Balance"
    }
    val balanceColor = when {
        yourNetBalance > 0.01 -> colors.forest
        yourNetBalance < -0.01 -> colors.accent
        else -> colors.inkPrimary
    }

    TripSurfaceCard(padding = 0.dp, cornerRadius = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            StatCell(
                label = "Total Expenses",
                value = formatTripMoney(metrics.totalSpent, currencySymbol),
                valueColor = colors.inkPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.divider)
            )
            StatCell(
                label = "Your Share",
                value = formatTripMoney(yourShare, currencySymbol),
                valueColor = colors.inkPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            StatCell(
                label = balanceLabel,
                value = if (abs(yourNetBalance) < 0.01) "Settled" else formatTripMoney(abs(yourNetBalance), currencySymbol),
                valueColor = balanceColor,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.divider)
            )
            StatCell(
                label = "Common Wallet",
                value = formatTripMoney(metrics.walletBalance, currencySymbol),
                valueColor = colors.forest,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = TripMonoFont,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTripListItem(
    overview: TripCardOverview,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    val trip = overview.trip
    val progress = if (trip.totalBudget > 0.0) {
        (overview.totalSpent / trip.totalBudget).toFloat().coerceIn(0f, 1f)
    } else 0f

    TripSurfaceCard(
        padding = 12.dp,
        cornerRadius = 12.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = trip.cover.drawableRes),
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        TripStatusBadge(
                            text = "Active",
                            backgroundColor = colors.forestSoft,
                            contentColor = colors.forest
                        )
                    }
                }
                Text(
                    text = "${trip.destination} · ${trip.startDateLabel} – ${trip.endDateLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTripMoney(overview.totalSpent, trip.currencySymbol),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                color = colors.inkPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.surfaceMuted)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (overview.totalSpent > trip.totalBudget && trip.totalBudget > 0) colors.danger else colors.forest)
            )
        }
    }
}

@Composable
private fun CompactSettlementRow(
    transfer: DebtTransfer,
    currencySymbol: String,
    onSettle: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TravelerMonogramBadge(member = transfer.fromMember, size = 32.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transfer.fromMember.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "owes",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = transfer.toMember.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = formatTripMoney(transfer.amount, currencySymbol),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSettle)
                .testTag("ts_settle_${transfer.fromMember.id}_${transfer.toMember.id}"),
            shape = RoundedCornerShape(8.dp),
            color = colors.forestSoft
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Settle",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.forest,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun WhoOwesWhomRowCard(
    transfer: DebtTransfer,
    currencySymbol: String,
    onSettleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TripSurfaceCard(modifier = modifier, padding = 0.dp, cornerRadius = 12.dp) {
        CompactSettlementRow(
            transfer = transfer,
            currencySymbol = currencySymbol,
            onSettle = onSettleClick
        )
    }
}

@Composable
private fun HomeShimmerLoading(modifier: Modifier = Modifier) {
    val colors = LocalTripSplitColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TripShimmerBlock(height = 36.dp)
        TripShimmerBlock(height = 148.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                TripShimmerBlock(height = 68.dp, modifier = Modifier.weight(1f))
            }
        }
        TripShimmerBlock(height = 136.dp)
    }
}

@Composable
private fun TripSplitEmptyHome(
    greetingLine: String,
    onCreateTripClick: (DestinationCover?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = greetingLine,
                style = MaterialTheme.typography.titleLarge,
                color = colors.inkPrimary
            )
        }

        // Clean compact warm-sage welcome card (no giant hero!)
        item {
            TripSageCard(padding = 18.dp, cornerRadius = 16.dp) {
                TripStatusBadge(
                    text = "Travel Finance",
                    backgroundColor = colors.forestSoft,
                    contentColor = colors.forest
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.ts_empty_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ts_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                TripPrimaryButton(
                    text = stringResource(R.string.ts_empty_cta),
                    icon = Icons.Rounded.Add,
                    onClick = { onCreateTripClick(null) },
                    testTag = "ts_empty_create_first_trip_btn"
                )
            }
        }

        // Unclipped 2x2 Responsive Destination Theme Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TripSectionHeader(title = "Choose a Destination Theme")
                DestinationThemeGrid(
                    onSelectDestination = { cover -> onCreateTripClick(cover) }
                )
            }
        }
    }
}

private fun buildCompactGreeting(firstName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val prefix = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }
    val safeName = firstName.trim().ifEmpty { "Traveler" }
    return "$prefix, $safeName"
}
