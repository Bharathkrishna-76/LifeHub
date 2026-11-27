package com.example.tripsplit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tripsplit.components.TravelerMonogramBadge
import com.example.tripsplit.components.TripOutlinedButton
import com.example.tripsplit.components.TripPrimaryButton
import com.example.tripsplit.components.TripSageCard
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.components.tripCategoryIcon
import com.example.tripsplit.components.tripCategoryTint
import com.example.tripsplit.domain.DebtSimplifier
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.ExpenseShare
import com.example.tripsplit.domain.SplitMode
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.domain.WalletEntryType
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripSheet(
    defaultHostName: String,
    initialCover: DestinationCover = DestinationCover.COASTAL,
    onDismiss: () -> Unit,
    onCreateTrip: (
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
    ) -> Unit
) {
    val colors = LocalTripSplitColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var waypointsText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("25000") }
    var walletText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("₹") }
    var selectedCover by remember(initialCover) { mutableStateOf(initialCover) }
    var companionInput by remember { mutableStateOf("") }
    val travelers = remember {
        mutableStateListOf(defaultHostName.trim().ifEmpty { "You" })
    }

    val currencies = listOf("₹", "$", "€", "£", "¥")
    val sliderBudget = (budgetText.toFloatOrNull() ?: 25000f).coerceIn(0f, 200000f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        contentColor = colors.inkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create Trip",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.inkPrimary
            )

            // Responsive 2x2 Destination Cover Picker (zero clipping!)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Destination Theme", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                val coverRows = remember { DestinationCover.entries.chunked(2) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    coverRows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { cover ->
                                val selected = selectedCover == cover
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.8f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) colors.accent else colors.borderSubtle,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCover = cover }
                                        .testTag("ts_cover_${cover.name}")
                                ) {
                                    Image(
                                        painter = painterResource(id = cover.drawableRes),
                                        contentDescription = cover.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0xCC111915))
                                                )
                                            )
                                    )
                                    Text(
                                        text = cover.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.accent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TripField(
                value = title,
                onValueChange = { title = it },
                label = "Trip Name",
                placeholder = "e.g., Goa Weekend",
                testTag = "ts_create_trip_title"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = "Origin",
                    placeholder = "Bengaluru",
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_origin"
                )
                TripField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = "Destination",
                    placeholder = "North Goa",
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_destination"
                )
            }

            TripField(
                value = waypointsText,
                onValueChange = { waypointsText = it },
                label = "Stops (optional, comma-separated)",
                placeholder = "e.g., Hubli, Dudhsagar",
                testTag = "ts_create_trip_waypoints"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    placeholder = "12 Dec",
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_start"
                )
                TripField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "End Date",
                    placeholder = "17 Dec",
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_end"
                )
            }

            // Currency Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Currency", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencies.forEach { sym ->
                        val active = selectedCurrency == sym
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCurrency = sym },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) colors.inkPrimary else colors.surface,
                            border = BorderStroke(1.dp, if (active) colors.inkPrimary else colors.borderSubtle)
                        ) {
                            Text(
                                text = sym,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = TripMonoFont),
                                color = if (active) colors.surface else colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Budget ($selectedCurrency)",
                    placeholder = "40000",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_budget"
                )
                TripField(
                    value = walletText,
                    onValueChange = { walletText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Start Wallet (opt.)",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    testTag = "ts_create_trip_wallet"
                )
            }

            // Quick Budget Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quick Budget Slider", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    Text(
                        text = formatTripMoney(sliderBudget.toDouble(), selectedCurrency),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.forest
                    )
                }
                Slider(
                    value = sliderBudget,
                    onValueChange = { raw ->
                        val stepped = (raw / 1000f).roundToInt() * 1000
                        budgetText = stepped.toString()
                    },
                    valueRange = 0f..200000f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.forest,
                        inactiveTrackColor = colors.borderSubtle
                    )
                )
            }

            // Travelers builder
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Members (${travelers.size})", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TripField(
                        value = companionInput,
                        onValueChange = { companionInput = it },
                        label = "Add member name",
                        placeholder = "e.g., Arjun",
                        modifier = Modifier.weight(1f),
                        testTag = "ts_create_trip_companion_input"
                    )
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val cleaned = companionInput.trim()
                                if (cleaned.isNotEmpty() && travelers.none { it.equals(cleaned, true) }) {
                                    travelers.add(cleaned)
                                    companionInput = ""
                                }
                            }
                            .testTag("ts_create_trip_add_companion_btn"),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.forest
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add member",
                                tint = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    travelers.forEachIndexed { idx, name ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surfaceSage,
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (idx == 0) "$name (You)" else name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.inkPrimary
                                )
                                if (idx > 0) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove $name",
                                        tint = colors.inkMuted,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { travelers.removeAt(idx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TripPrimaryButton(
                text = "Create Trip",
                icon = Icons.Rounded.Check,
                enabled = title.isNotBlank() || destination.isNotBlank(),
                onClick = {
                    val budget = budgetText.toDoubleOrNull() ?: 0.0
                    val initialWallet = walletText.toDoubleOrNull() ?: 0.0
                    val stops = waypointsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreateTrip(
                        title,
                        destination,
                        origin,
                        startDate,
                        endDate,
                        budget,
                        selectedCurrency,
                        selectedCover,
                        stops,
                        travelers.toList(),
                        initialWallet
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "ts_create_trip_submit_btn"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTripExpenseSheet(
    members: List<TripMember>,
    currencySymbol: String,
    walletBalance: Double,
    existingExpense: TripExpense? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        amount: Double,
        category: TripCategory,
        paidByMemberId: String?,
        paidFromWallet: Boolean,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        locationNote: String
    ) -> Unit
) {
    val colors = LocalTripSplitColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            existingExpense?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        )
    }
    var category by remember { mutableStateOf(existingExpense?.category ?: TripCategory.DINING) }
    var paidFromWallet by remember { mutableStateOf(existingExpense?.paidFromWallet ?: false) }
    var paidByMemberId by remember {
        mutableStateOf(existingExpense?.paidByMemberId ?: members.firstOrNull()?.id)
    }
    var splitMode by remember { mutableStateOf(existingExpense?.splitMode ?: SplitMode.EQUAL) }
    var locationNote by remember { mutableStateOf(existingExpense?.locationNote ?: "") }

    val selectedParticipants = remember(members, existingExpense) {
        mutableStateMapOf<String, Boolean>().apply {
            val existingIds = existingExpense?.shares?.map { it.memberId }?.toSet()
            members.forEach { m ->
                this[m.id] = existingIds?.contains(m.id) ?: true
            }
        }
    }

    val customInputs = remember(members, existingExpense) {
        mutableStateMapOf<String, String>().apply {
            members.forEach { m ->
                val share = existingExpense?.shares?.firstOrNull { it.memberId == m.id }?.amount
                this[m.id] = share?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
            }
        }
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val activeMemberIds = members.filter { selectedParticipants[it.id] == true }.map { it.id }

    val computedShares: List<ExpenseShare> = when (splitMode) {
        SplitMode.EQUAL -> DebtSimplifier.buildEqualShares(parsedAmount, activeMemberIds)
        SplitMode.EXACT -> activeMemberIds.map { id ->
            ExpenseShare(id, DebtSimplifier.round2(customInputs[id]?.toDoubleOrNull() ?: 0.0))
        }
        SplitMode.PERCENTAGE -> activeMemberIds.map { id ->
            val pct = customInputs[id]?.toDoubleOrNull() ?: 0.0
            ExpenseShare(id, DebtSimplifier.round2(parsedAmount * pct / 100.0))
        }
    }

    val sharesSum = computedShares.sumOf { it.amount }
    val splitValid = when {
        paidFromWallet -> parsedAmount > 0.0
        splitMode == SplitMode.EQUAL -> parsedAmount > 0.0 && activeMemberIds.isNotEmpty()
        splitMode == SplitMode.EXACT -> parsedAmount > 0.0 && abs(sharesSum - parsedAmount) <= 0.05
        splitMode == SplitMode.PERCENTAGE -> {
            val pctSum = activeMemberIds.sumOf { customInputs[it]?.toDoubleOrNull() ?: 0.0 }
            parsedAmount > 0.0 && abs(pctSum - 100.0) <= 0.1
        }
        else -> false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        contentColor = colors.inkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingExpense == null) "Add Trip Expense" else "Edit Expense",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.inkPrimary
            )

            TripField(
                value = title,
                onValueChange = { title = it },
                label = "Description",
                placeholder = "e.g., Dinner at beach shack",
                testTag = "ts_expense_title_input"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Amount ($currencySymbol)",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    testTag = "ts_expense_amount_input"
                )
                TripField(
                    value = locationNote,
                    onValueChange = { locationNote = it },
                    label = "Place (opt.)",
                    placeholder = "Old Town",
                    modifier = Modifier.weight(1f),
                    testTag = "ts_expense_location_input"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TripCategory.entries.forEach { cat ->
                        val selected = category == cat
                        val tint = tripCategoryTint(cat)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { category = cat },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) tint else colors.surface,
                            border = BorderStroke(1.dp, if (selected) tint else colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = tripCategoryIcon(cat),
                                    contentDescription = null,
                                    tint = if (selected) Color.White else tint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Pay from Common Wallet Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (paidFromWallet) colors.forestSoft else colors.surface,
                border = BorderStroke(1.dp, if (paidFromWallet) colors.forest else colors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = colors.forest,
                            modifier = Modifier.size(19.dp)
                        )
                        Column {
                            Text(
                                text = "Pay from Common Wallet",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "Available: ${formatTripMoney(walletBalance, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                    }
                    Switch(
                        checked = paidFromWallet,
                        onCheckedChange = { paidFromWallet = it },
                        modifier = Modifier.testTag("ts_pay_source_wallet"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.forest
                        )
                    )
                }
            }

            if (!paidFromWallet) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Paid By", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { member ->
                            val selected = paidByMemberId == member.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { paidByMemberId = member.id },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) colors.inkPrimary else colors.surface,
                                border = BorderStroke(1.dp, if (selected) colors.inkPrimary else colors.borderSubtle)
                            ) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.surface else colors.inkPrimary,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Split Method", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SplitMode.entries.forEach { mode ->
                            val selected = splitMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { splitMode = mode },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) colors.accent else colors.surfaceMuted,
                                border = BorderStroke(1.dp, if (selected) colors.accent else colors.borderSubtle)
                            ) {
                                Text(
                                    text = mode.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else colors.inkSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    members.forEach { member ->
                        val included = selectedParticipants[member.id] == true
                        val shareValue = computedShares.firstOrNull { it.memberId == member.id }?.amount ?: 0.0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = included,
                                    onCheckedChange = { checked ->
                                        selectedParticipants[member.id] = checked
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.forest,
                                        uncheckedColor = colors.inkMuted
                                    )
                                )
                                TravelMemberMiniLabel(member = member, modifier = Modifier.weight(1f))
                                if (splitMode == SplitMode.EQUAL) {
                                    Text(
                                        text = if (included) formatTripMoney(shareValue, currencySymbol) else "—",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                        color = if (included) colors.inkPrimary else colors.inkMuted,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                } else if (included) {
                                    OutlinedTextField(
                                        value = customInputs[member.id] ?: "",
                                        onValueChange = { raw ->
                                            customInputs[member.id] = raw.filter { c -> c.isDigit() || c == '.' }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.width(88.dp),
                                        placeholder = {
                                            Text(
                                                text = if (splitMode == SplitMode.PERCENTAGE) "%" else currencySymbol,
                                                color = colors.inkMuted
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.forest,
                                            unfocusedBorderColor = colors.borderSubtle,
                                            focusedTextColor = colors.inkPrimary,
                                            unfocusedTextColor = colors.inkPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (!splitValid && parsedAmount > 0.0) {
                        val hint = when (splitMode) {
                            SplitMode.EXACT -> "Shares sum to ${formatTripMoney(sharesSum, currencySymbol)} — must equal ${formatTripMoney(parsedAmount, currencySymbol)}"
                            SplitMode.PERCENTAGE -> {
                                val pct = activeMemberIds.sumOf { customInputs[it]?.toDoubleOrNull() ?: 0.0 }
                                "Percentages sum to ${pct.toInt()}% — must equal 100%"
                            }
                            SplitMode.EQUAL -> "Select at least one member."
                        }
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TripPrimaryButton(
                text = if (existingExpense == null) "Save Expense" else "Update Expense",
                icon = Icons.Rounded.Check,
                enabled = title.isNotBlank() && splitValid,
                onClick = {
                    onSave(
                        existingExpense?.id,
                        title,
                        parsedAmount,
                        category,
                        if (paidFromWallet) null else paidByMemberId,
                        paidFromWallet,
                        splitMode,
                        if (paidFromWallet) {
                            DebtSimplifier.buildEqualShares(parsedAmount, members.map { it.id })
                        } else {
                            computedShares
                        },
                        locationNote
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "ts_save_expense_submit_btn"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpWalletSheet(
    members: List<TripMember>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSubmit: (memberId: String, amount: Double, type: WalletEntryType, note: String) -> Unit
) {
    val colors = LocalTripSplitColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.id ?: "") }
    var entryType by remember { mutableStateOf(WalletEntryType.DEPOSIT) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        contentColor = colors.inkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Trip Wallet",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.inkPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WalletEntryType.entries.forEach { type ->
                    val selected = entryType == type
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { entryType = type },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colors.forest else colors.surfaceMuted,
                        border = BorderStroke(1.dp, if (selected) colors.forest else colors.borderSubtle)
                    ) {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else colors.inkSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Member", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { member ->
                        val selected = selectedMemberId == member.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedMemberId = member.id },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) colors.inkPrimary else colors.surface,
                            border = BorderStroke(1.dp, if (selected) colors.inkPrimary else colors.borderSubtle)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.surface else colors.inkPrimary,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            TripField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Amount ($currencySymbol)",
                placeholder = "5000",
                keyboardType = KeyboardType.Decimal,
                testTag = "ts_wallet_amount_input"
            )

            TripField(
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
                placeholder = "e.g., Cash pool for taxis",
                testTag = "ts_wallet_note_input"
            )

            val amt = amountText.toDoubleOrNull() ?: 0.0
            TripPrimaryButton(
                text = if (entryType == WalletEntryType.DEPOSIT) "Add to Wallet" else "Record Refund",
                icon = Icons.Rounded.Check,
                enabled = amt > 0.0 && selectedMemberId.isNotEmpty(),
                onClick = {
                    onSubmit(selectedMemberId, amt, entryType, note)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "ts_wallet_submit_btn"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMembersSheet(
    trip: Trip,
    onDismiss: () -> Unit,
    onAddTraveler: (String) -> Unit
) {
    val colors = LocalTripSplitColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val inviteCode = remember(trip.id) {
        val raw = trip.id.replace("-", "").take(6).uppercase()
        "TS-$raw"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas,
        contentColor = colors.inkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invite Members",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.inkPrimary
            )

            // Shareable Invite Code Box
            TripSageCard(padding = 14.dp) {
                Text("Trip Invite Code", style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inviteCode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = TripMonoFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.forest
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripOutlinedButton(
                            text = if (copied) "Copied" else "Copy",
                            icon = Icons.Rounded.ContentCopy,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("TripSplit Invite Code", inviteCode))
                                copied = true
                            },
                            testTag = "ts_copy_invite_code_btn"
                        )
                        TripOutlinedButton(
                            text = "Share",
                            icon = Icons.Rounded.Share,
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my trip \"${trip.title}\" (${trip.destination}) on TripSplit! Use invite code: $inviteCode"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Trip Invite"))
                            },
                            testTag = "ts_share_invite_btn"
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.divider)

            Text(
                text = "Or add a member directly",
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkSecondary
            )

            TripField(
                value = name,
                onValueChange = { name = it },
                label = "Member Name",
                placeholder = "e.g., Meera",
                testTag = "ts_add_traveler_name_input"
            )
            TripPrimaryButton(
                text = "Add to Trip",
                icon = Icons.Rounded.Add,
                enabled = name.isNotBlank(),
                onClick = {
                    onAddTraveler(name)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "ts_add_traveler_submit_btn"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TravelMemberMiniLabel(
    member: TripMember,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TravelerMonogramBadge(member = member, size = 28.dp)
        Text(
            text = member.name,
            style = MaterialTheme.typography.titleSmall,
            color = colors.inkPrimary
        )
    }
}

@Composable
private fun TripField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String? = null
) {
    val colors = LocalTripSplitColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = colors.inkMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.forest,
            unfocusedBorderColor = colors.borderSubtle,
            focusedLabelColor = colors.forest,
            unfocusedLabelColor = colors.inkMuted,
            focusedTextColor = colors.inkPrimary,
            unfocusedTextColor = colors.inkPrimary
        )
    )
}
