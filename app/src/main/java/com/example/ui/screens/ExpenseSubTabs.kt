package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.TransactionItem
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PremiumDarkVaultCard
import com.example.ui.components.SavingActionButton
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.viewmodel.LifeHubUiState
import java.util.UUID

/**
 * Palette bundle for the adaptive Private Ledger (Champagne Ivory in Light, Obsidian Vault in Dark).
 */
internal data class VaultPalette(
    val isDark: Boolean,
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val gold: Color,
    val mint: Color,
    val coral: Color,
    val rowBg: Color,
    val rowBorder: Color,
    val pillBg: Color
)

/**
 * Dedicated Separate Trips & Events Ledger inside Money tab.
 * Expenses logged here are isolated from Daily Totals, Monthly Budget, and Net Balance.
 */
internal fun LazyListScope.tripsAndEventsSection(
    state: LifeHubUiState,
    palette: VaultPalette,
    symbol: String,
    selectedEventFilter: String?,
    onSelectEventFilter: (String?) -> Unit,
    onAddTripExpense: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val trips = state.allTripEventTransactions
    val filteredTrips = if (selectedEventFilter == null) {
        trips
    } else {
        trips.filter { it.eventName.ifBlank { "General Trip / Event" } == selectedEventFilter }
    }
    val filteredTotal = filteredTrips.sumOf { it.amount }
    val eventGroups = state.tripEventBreakdown

    item(key = "trips_summary_card") {
        PremiumDarkVaultCard(
            accentGold = palette.gold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trips_summary_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.gold.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = "Trips and events",
                            tint = palette.gold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SEPARATE TRIPS & EVENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.settings.separateTripEventLedger) {
                                "Excluded from Daily budget & totals"
                            } else {
                                "Currently included in Daily totals"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedEventFilter == null) "ALL TRIPS SPEND" else selectedEventFilter.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = DateAndFormatUtils.formatCurrency(filteredTotal, symbol),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = palette.gold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TRIPS / ENTRIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${eventGroups.size} trips • ${filteredTrips.size} items",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = palette.ink,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (eventGroups.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all_trips_chip") {
                        FilterChip(
                            selected = selectedEventFilter == null,
                            onClick = { onSelectEventFilter(null) },
                            label = { Text("All Trips") }
                        )
                    }
                    items(eventGroups, key = { it.first }) { (eventTitle, eventTotal) ->
                        FilterChip(
                            selected = selectedEventFilter == eventTitle,
                            onClick = {
                                onSelectEventFilter(if (selectedEventFilter == eventTitle) null else eventTitle)
                            },
                            label = {
                                Text("$eventTitle (${DateAndFormatUtils.formatCurrency(eventTotal, symbol, compact = true)})")
                            }
                        )
                    }
                }
            }
        }
    }

    if (filteredTrips.isEmpty()) {
        item(key = "empty_trips") {
            EmptyStateCard(
                icon = Icons.Rounded.Luggage,
                title = "No trip or event expenses yet",
                subtitle = "Going on a trip, wedding, or festival? Tap + to track it separately without disturbing your daily expenses.",
                actionLabel = "Add Trip Expense",
                onAction = onAddTripExpense,
                accentColor = palette.gold
            )
        }
    } else {
        items(filteredTrips, key = { "trip_${it.id}" }) { tx ->
            SwipeableTransactionRow(
                transaction = tx,
                currencySymbol = symbol,
                palette = palette,
                showEventBadge = true,
                onClick = { onEditTransaction(tx) },
                onDelete = { onDeleteTransaction(tx.id) }
            )
        }
    }
}

/**
 * Dedicated Reminders, EMI, Rent & Subscriptions hub directly inside the Money tab.
 * Uses the existing Room + Firestore + WorkManager `state.reminders` (0 extra Firestore reads).
 */
internal fun LazyListScope.billsAndEmiSection(
    state: LifeHubUiState,
    palette: VaultPalette,
    symbol: String,
    selectedCategory: ReminderCategory?,
    onSelectCategory: (ReminderCategory?) -> Unit,
    onAddReminderClick: () -> Unit,
    onTogglePaid: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    val allReminders = state.reminders
    val unpaidReminders = allReminders.filter { !it.paid }
    val pendingTotal = unpaidReminders.sumOf { it.amount }
    val dueSoonCount = unpaidReminders.count { DateAndFormatUtils.daysUntil(it.dueDateEpochMs) <= 5 }
    val paidCount = allReminders.count { it.paid }

    val filtered = rememberFilteredReminders(allReminders, selectedCategory)

    item(key = "bills_emi_summary") {
        PremiumDarkVaultCard(
            accentGold = palette.gold,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("money_bills_summary_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.coral.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Bills and EMI reminders",
                            tint = palette.coral,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "REMINDERS • EMI • SUBSCRIPTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Local alert notifications & monthly tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "PENDING DUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = DateAndFormatUtils.formatCurrency(pendingTotal, symbol, compact = true),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = palette.coral,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DUE SOON",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$dueSoonCount",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = palette.gold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "PAID",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$paidCount / ${allReminders.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = palette.mint,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    item(key = "bills_category_filters") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "rem_all") {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All (${allReminders.size})") }
                )
            }
            items(ReminderCategory.entries, key = { it.name }) { cat ->
                val count = allReminders.count { it.category == cat }
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                    label = { Text("${cat.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.gold.copy(alpha = 0.2f),
                        selectedLabelColor = palette.gold
                    )
                )
            }
        }
    }

    if (filtered.isEmpty()) {
        item(key = "empty_money_reminders") {
            EmptyStateCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "No Bills, EMIs, or Subscriptions",
                subtitle = "Add your monthly EMIs, house rent, Netflix/Spotify subscriptions, or utility bills so you never miss a due date.",
                actionLabel = "Add EMI / Bill",
                onAction = onAddReminderClick,
                accentColor = palette.gold
            )
        }
    } else {
        items(filtered, key = { "money_rem_${it.id}" }) { reminder ->
            MoneyReminderCard(
                reminder = reminder,
                currencySymbol = symbol,
                palette = palette,
                onTogglePaid = { onTogglePaid(reminder) },
                onDelete = { onDeleteReminder(reminder.id) }
            )
        }
    }
}

private fun rememberFilteredReminders(
    reminders: List<ReminderItem>,
    selectedCategory: ReminderCategory?
): List<ReminderItem> {
    val base = if (selectedCategory == null) reminders else reminders.filter { it.category == selectedCategory }
    return base.sortedWith(compareBy<ReminderItem> { it.paid }.thenBy { it.dueDateEpochMs })
}

@Composable
private fun MoneyReminderCard(
    reminder: ReminderItem,
    currencySymbol: String,
    palette: VaultPalette,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val daysUntil = DateAndFormatUtils.daysUntil(reminder.dueDateEpochMs)
    val accent = when (reminder.category) {
        ReminderCategory.EMI -> extended.accentCoral
        ReminderCategory.SUBSCRIPTION -> extended.accentViolet
        ReminderCategory.RENT -> extended.accentAmber
        ReminderCategory.BILL -> extended.accentCyan
    }
    val statusText = when {
        reminder.paid -> "Paid"
        daysUntil < 0 -> "Overdue ${-daysUntil}d"
        daysUntil == 0 -> "Due today"
        daysUntil == 1 -> "Due tomorrow"
        else -> "In ${daysUntil}d"
    }
    val statusColor = when {
        reminder.paid -> palette.mint
        daysUntil <= 2 -> palette.coral
        else -> palette.gold
    }

    Surface(
        onClick = onTogglePaid,
        shape = RoundedCornerShape(18.dp),
        color = palette.rowBg,
        border = BorderStroke(1.dp, if (reminder.paid) palette.rowBorder else accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("money_reminder_row_${reminder.id}")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
            val compactRow = metrics.isNarrowPhone || metrics.isLargeFont

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onTogglePaid,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (reminder.paid) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (reminder.paid) "Mark unpaid" else "Mark paid",
                            tint = if (reminder.paid) palette.mint else accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = reminderCategoryIcon(reminder.category),
                            contentDescription = reminder.category.label,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (reminder.paid) palette.muted else palette.ink,
                            textDecoration = if (reminder.paid) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${reminder.category.label} • ${DateAndFormatUtils.formatShortDate(reminder.dueDateEpochMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (reminder.recurringMonthly) {
                                Icon(
                                    imageVector = Icons.Rounded.Autorenew,
                                    contentDescription = "Monthly recurring",
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    if (!compactRow) {
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = DateAndFormatUtils.formatCurrency(reminder.amount, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (reminder.paid) palette.muted else palette.ink,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete reminder",
                            tint = palette.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (compactRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, end = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateAndFormatUtils.formatCurrency(reminder.amount, currencySymbol),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = if (reminder.paid) palette.muted else palette.ink,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun reminderCategoryIcon(category: ReminderCategory): ImageVector = when (category) {
    ReminderCategory.EMI -> Icons.Rounded.AccountBalance
    ReminderCategory.RENT -> Icons.Rounded.HomeWork
    ReminderCategory.SUBSCRIPTION -> Icons.Rounded.Subscriptions
    ReminderCategory.BILL -> Icons.Rounded.ReceiptLong
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddMoneyReminderSheet(
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, ReminderCategory, Double, Long, Boolean) -> Unit
) {
    val clientDocId = remember { "rem_${UUID.randomUUID().toString().replace("-", "").take(18)}" }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReminderCategory.EMI) }
    var amountText by remember { mutableStateOf("") }
    var dueInDays by remember { mutableStateOf(5) }
    var recurringMonthly by remember { mutableStateOf(true) }

    val quickPresets = listOf(
        "Bike / Car EMI" to ReminderCategory.EMI,
        "House Rent" to ReminderCategory.RENT,
        "Netflix / OTT" to ReminderCategory.SUBSCRIPTION,
        "Electricity Bill" to ReminderCategory.BILL,
        "Credit Card Bill" to ReminderCategory.BILL
    )

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
                text = "Add Bill, EMI or Subscription",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPresets, key = { it.first }) { (presetTitle, presetCat) ->
                    FilterChip(
                        selected = title == presetTitle,
                        onClick = {
                            title = presetTitle
                            category = presetCat
                        },
                        label = { Text(presetTitle) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Title (e.g., Home Loan EMI, Spotify, Rent)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("money_reminder_title_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

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
                    .testTag("money_reminder_amount_input")
            )

            Text("Due Date", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(0 to "Today", 1 to "Tomorrow", 3 to "3 Days", 5 to "5 Days", 10 to "10 Days", 30 to "30 Days")
                items(options, key = { it.first }) { (days, label) ->
                    FilterChip(
                        selected = dueInDays == days,
                        onClick = { dueInDays = days },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Repeat Every Month", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Auto-tracks recurring EMIs & subscriptions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = recurringMonthly,
                    onCheckedChange = { recurringMonthly = it }
                )
            }

            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            SavingActionButton(
                label = "Schedule Reminder",
                isSaving = isSaving,
                enabled = title.isNotBlank() && parsedAmount > 0.0,
                onClick = {
                    val dueEpoch = System.currentTimeMillis() + dueInDays.toLong() * 86_400_000L
                    onSave(clientDocId, title.trim(), category, parsedAmount, dueEpoch, recurringMonthly)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_money_reminder_button")
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
