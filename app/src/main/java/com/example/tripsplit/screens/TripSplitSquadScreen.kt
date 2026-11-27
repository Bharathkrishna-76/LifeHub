package com.example.tripsplit.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripsplit.components.TravelerMonogramBadge
import com.example.tripsplit.components.TripExpandableCard
import com.example.tripsplit.components.TripOutlinedButton
import com.example.tripsplit.components.TripPrimaryButton
import com.example.tripsplit.components.TripSageCard
import com.example.tripsplit.components.TripSectionHeader
import com.example.tripsplit.components.TripSurfaceCard
import com.example.tripsplit.components.TripUnderlineTabBar
import com.example.tripsplit.components.formatTripDate
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.domain.DebtTransfer
import com.example.tripsplit.domain.WalletEntryType
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import com.example.tripsplit.viewmodel.TripSplitUiState

@Composable
fun TripSplitSquadScreen(
    uiState: TripSplitUiState,
    onInviteMembersClick: () -> Unit,
    onRemoveTraveler: (String) -> Unit,
    onSettleTransfer: (DebtTransfer) -> Unit,
    onDeleteSettlement: (String) -> Unit,
    onTopUpWalletClick: () -> Unit,
    onDeleteWalletEntry: (String) -> Unit,
    onCreateTripClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    val trip = uiState.activeTrip
    val metrics = uiState.metrics

    if (trip == null || metrics == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.canvas)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create a trip to manage squad & wallet",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TripPrimaryButton(
                text = "Create Trip",
                icon = Icons.Rounded.Add,
                onClick = onCreateTripClick,
                testTag = "ts_squad_create_trip_btn"
            )
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val membersById = remember(uiState.members) { uiState.members.associateBy { it.id } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Squad & Wallet",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "${trip.title} · ${uiState.members.size} member${if (uiState.members.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
                TripOutlinedButton(
                    text = "Invite",
                    icon = Icons.Rounded.GroupAdd,
                    onClick = onInviteMembersClick,
                    testTag = "ts_squad_add_traveler_btn"
                )
            }
        }

        item {
            TripUnderlineTabBar(
                tabs = listOf("Balances & Settle", "Common Wallet"),
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                testTagPrefix = "ts_squad_tab"
            )
        }

        if (selectedTabIndex == 0) {
            item {
                TripSectionHeader(title = "Who Owes Whom")
            }

            if (metrics.simplifiedDebts.isEmpty()) {
                item {
                    TripSageCard(padding = 14.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.forest,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "All balances settled",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = "No pending transfers right now.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    metrics.simplifiedDebts,
                    key = { "${it.fromMember.id}_${it.toMember.id}" }
                ) { transfer ->
                    WhoOwesWhomRowCard(
                        transfer = transfer,
                        currencySymbol = trip.currencySymbol,
                        onSettleClick = { onSettleTransfer(transfer) }
                    )
                }
            }

            item {
                TripSectionHeader(title = "Member Balances (${uiState.members.size})")
            }

            items(metrics.memberBalances, key = { it.member.id }) { bal ->
                val netColor = when {
                    bal.netBalance > 0.01 -> colors.forest
                    bal.netBalance < -0.01 -> colors.accent
                    else -> colors.inkMuted
                }
                TripExpandableCard(
                    testTag = "ts_member_accordion_${bal.member.id}",
                    header = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TravelerMonogramBadge(member = bal.member, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bal.member.name + if (bal.member.isSelf) " (You)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.inkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        bal.netBalance > 0.01 -> "Gets back"
                                        bal.netBalance < -0.01 -> "Owes group"
                                        else -> "Settled"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkMuted
                                )
                            }
                            Text(
                                text = formatTripMoney(bal.netBalance, trip.currencySymbol, showSign = true),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = TripMonoFont,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = netColor,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Paid", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(bal.totalPaidDirect, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.inkPrimary
                            )
                        }
                        Column {
                            Text("Share", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(bal.totalShareDirect, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.inkPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Wallet In", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(bal.walletContributed, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.forest
                            )
                        }
                    }
                    if (!bal.member.isSelf) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { onRemoveTraveler(bal.member.id) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("ts_remove_member_${bal.member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = "Remove member",
                                tint = colors.danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", color = colors.danger)
                        }
                    }
                }
            }

            if (uiState.settlements.isNotEmpty()) {
                item {
                    TripSectionHeader(title = "Settlement History")
                }
                item {
                    TripSurfaceCard(padding = 0.dp) {
                        uiState.settlements.forEachIndexed { idx, record ->
                            if (idx > 0) {
                                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                            }
                            val fromName = membersById[record.fromMemberId]?.name ?: "Member"
                            val toName = membersById[record.toMemberId]?.name ?: "Member"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$fromName paid $toName",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.inkPrimary
                                    )
                                    Text(
                                        text = "${record.note} · ${formatTripDate(record.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.inkMuted
                                    )
                                }
                                Text(
                                    text = formatTripMoney(record.amount, trip.currencySymbol),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                    color = colors.forest
                                )
                                IconButton(
                                    onClick = { onDeleteSettlement(record.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Undo,
                                        contentDescription = "Undo settlement",
                                        tint = colors.inkMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // COMMON WALLET TAB
            item {
                val poolRatio = if (metrics.walletDeposits > 0.0) {
                    (metrics.walletBalance / metrics.walletDeposits).toFloat().coerceIn(0f, 1f)
                } else 0f

                TripSageCard(padding = 16.dp, cornerRadius = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = colors.forest,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Common Wallet Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTripMoney(metrics.walletBalance, trip.currencySymbol),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = TripMonoFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.inkPrimary
                            )
                        }
                        TripPrimaryButton(
                            text = "Top Up",
                            icon = Icons.Rounded.Add,
                            onClick = onTopUpWalletClick,
                            testTag = "ts_wallet_tab_top_up_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(poolRatio)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.forest)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deposited", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(metrics.walletDeposits, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.forest
                            )
                        }
                        Column {
                            Text("Pool Spent", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(metrics.walletExpenses, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.accent
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Refunded", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            Text(
                                text = formatTripMoney(metrics.walletRefunds, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.inkPrimary
                            )
                        }
                    }
                }
            }

            item {
                TripSectionHeader(title = "Member Contributions")
            }

            item {
                TripSurfaceCard(padding = 0.dp) {
                    metrics.memberBalances.forEachIndexed { idx, bal ->
                        if (idx > 0) {
                            HorizontalDivider(color = colors.divider, thickness = 1.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TravelerMonogramBadge(member = bal.member, size = 30.dp)
                                Text(
                                    text = bal.member.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.inkPrimary
                                )
                            }
                            Text(
                                text = formatTripMoney(bal.walletContributed, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.forest
                            )
                        }
                    }
                }
            }

            item {
                TripSectionHeader(title = "Wallet Activity")
            }

            if (uiState.walletEntries.isEmpty()) {
                item {
                    TripSurfaceCard(padding = 14.dp) {
                        Text(
                            text = "No deposits logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Collect group cash for shared taxis, tolls, and meals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                    }
                }
            } else {
                items(uiState.walletEntries, key = { it.id }) { entry ->
                    val memberName = membersById[entry.memberId]?.name ?: "Member"
                    val isDeposit = entry.type == WalletEntryType.DEPOSIT
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDeposit) colors.forestSoft else colors.accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDeposit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                    contentDescription = entry.type.label,
                                    tint = if (isDeposit) colors.forest else colors.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$memberName · ${entry.type.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = "${entry.note} · ${formatTripDate(entry.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkMuted
                                )
                            }
                            Text(
                                text = (if (isDeposit) "+" else "-") + formatTripMoney(entry.amount, trip.currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = if (isDeposit) colors.forest else colors.accent
                            )
                            IconButton(
                                onClick = { onDeleteWalletEntry(entry.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete wallet entry",
                                    tint = colors.inkMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
