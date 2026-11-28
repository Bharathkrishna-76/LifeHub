package com.example.tripsplit.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripsplit.components.SpendingBreakdownChartCard
import com.example.tripsplit.components.SwipeableTripExpenseRow
import com.example.tripsplit.components.TimelineDateHeader
import com.example.tripsplit.components.TripPrimaryButton
import com.example.tripsplit.components.TripSageCard
import com.example.tripsplit.components.TripSectionHeader
import com.example.tripsplit.components.TripSurfaceCard
import com.example.tripsplit.components.formatTripDate
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import com.example.tripsplit.viewmodel.TripSplitUiState
import com.example.ui.components.rememberResponsiveMetrics

private enum class ExpenseSourceFilter(val label: String) {
    ALL("All"),
    SPLIT("Member Paid"),
    WALLET("Wallet")
}

private enum class ExpenseSortOrder(val label: String) {
    NEWEST("Newest"),
    HIGHEST("Highest"),
    LOWEST("Lowest"),
    CATEGORY("Category")
}

@Composable
fun TripSplitLedgerScreen(
    uiState: TripSplitUiState,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (TripExpense) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onCreateTripClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    val trip = uiState.activeTrip

    if (trip == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.canvas)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create a trip to track expenses",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TripPrimaryButton(
                text = "Create Trip",
                icon = Icons.Rounded.Add,
                onClick = onCreateTripClick,
                testTag = "ts_ledger_create_trip_btn"
            )
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TripCategory?>(null) }
    var sourceFilter by remember { mutableStateOf(ExpenseSourceFilter.ALL) }
    var sortOrder by remember { mutableStateOf(ExpenseSortOrder.NEWEST) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val membersById = remember(uiState.members) { uiState.members.associateBy { it.id } }

    val filteredExpenses = remember(uiState.expenses, searchQuery, selectedCategory, sourceFilter, sortOrder) {
        val base = uiState.expenses.filter { exp ->
            val matchesQuery = searchQuery.isBlank() ||
                exp.title.contains(searchQuery, ignoreCase = true) ||
                exp.locationNote.contains(searchQuery, ignoreCase = true) ||
                exp.category.label.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || exp.category == selectedCategory
            val matchesSource = when (sourceFilter) {
                ExpenseSourceFilter.ALL -> true
                ExpenseSourceFilter.SPLIT -> !exp.paidFromWallet
                ExpenseSourceFilter.WALLET -> exp.paidFromWallet
            }
            matchesQuery && matchesCat && matchesSource
        }
        when (sortOrder) {
            ExpenseSortOrder.NEWEST -> base.sortedByDescending { it.dateMillis }
            ExpenseSortOrder.HIGHEST -> base.sortedByDescending { it.amount }
            ExpenseSortOrder.LOWEST -> base.sortedBy { it.amount }
            ExpenseSortOrder.CATEGORY -> base.sortedBy { it.category.label }
        }
    }

    val groupedByDate = remember(filteredExpenses) {
        filteredExpenses.groupBy { formatTripDate(it.dateMillis) }
    }
    val filteredSum = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val responsive = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.horizontalPadding,
                end = responsive.horizontalPadding,
                top = 8.dp,
                bottom = 92.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ledger",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${trip.title} · ${filteredExpenses.size} item${if (filteredExpenses.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { sortMenuOpen = true }
                                .testTag("ts_ledger_sort_btn"),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = "Sort expenses",
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sortOrder.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.inkPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                            containerColor = colors.surface
                        ) {
                            ExpenseSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            color = if (sortOrder == order) colors.accent else colors.inkPrimary,
                                            fontWeight = if (sortOrder == order) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Compact Summary Row (stacks cleanly on narrow/large-font screens)
            item {
                TripSageCard(padding = 14.dp) {
                    if (responsive.shouldStackTightPairs) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Filtered Total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary
                                )
                                Text(
                                    text = formatTripMoney(filteredSum, trip.currencySymbol),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontFamily = TripMonoFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.inkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TripPrimaryButton(
                                text = "Add Expense",
                                icon = Icons.Rounded.Add,
                                onClick = onAddExpenseClick,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "ts_ledger_add_expense_btn"
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Filtered Total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkSecondary
                                )
                                Text(
                                    text = formatTripMoney(filteredSum, trip.currencySymbol),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontFamily = TripMonoFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.inkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TripPrimaryButton(
                                text = "Add Expense",
                                icon = Icons.Rounded.Add,
                                onClick = onAddExpenseClick,
                                testTag = "ts_ledger_add_expense_btn"
                            )
                        }
                    }
                }
            }

        // Clean Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ts_ledger_search_input"),
                placeholder = { Text("Search expenses or stops...", color = colors.inkMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.inkMuted,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = colors.inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.forest,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary
                )
            )
        }

        // Source Segmented Selector
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceMuted,
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ExpenseSourceFilter.entries.forEach { filter ->
                        val selected = sourceFilter == filter
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { sourceFilter = filter }
                                .testTag("ts_source_filter_${filter.name}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colors.surface else Color.Transparent,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.inkPrimary else colors.inkMuted,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(
                    label = "All",
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
                TripCategory.entries.forEach { cat ->
                    CategoryChip(
                        label = cat.label,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                    )
                }
            }
        }

        // Category Breakdown Chart (when expenses exist)
        val breakdown = uiState.metrics?.categoryBreakdown.orEmpty()
        if (breakdown.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null) {
            item(key = "ledger_breakdown") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripSectionHeader(title = "Spending Breakdown")
                    SpendingBreakdownChartCard(
                        breakdown = breakdown,
                        currencySymbol = trip.currencySymbol,
                        expenses = uiState.expenses
                    )
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                TripSurfaceCard(padding = 16.dp) {
                    Text(
                        text = "No matching expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust filters or tap Add Expense to log a trip bill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
            }
        } else {
            groupedByDate.forEach { (dateLabel, dayItems) ->
                item(key = "timeline_$dateLabel") {
                    val daySum = dayItems.sumOf { it.amount }
                    TimelineDateHeader(
                        dateLabel = dateLabel,
                        dayTotalFormatted = formatTripMoney(daySum, trip.currencySymbol)
                    )
                }
                items(dayItems, key = { it.id }) { expense ->
                    SwipeableTripExpenseRow(
                        expense = expense,
                        membersById = membersById,
                        currencySymbol = trip.currencySymbol,
                        onClick = { onEditExpense(expense) },
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.accent else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.borderSubtle),
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else colors.inkSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
