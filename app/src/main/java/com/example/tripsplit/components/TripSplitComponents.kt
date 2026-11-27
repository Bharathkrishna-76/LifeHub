package com.example.tripsplit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatTripMoney(amount: Double, symbol: String = "₹", showSign: Boolean = false): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = 0
    }
    val absVal = abs(amount)
    val prefix = when {
        showSign && amount > 0.005 -> "+"
        amount < -0.005 -> "-"
        else -> ""
    }
    return "$prefix$symbol${nf.format(absVal)}"
}

fun formatTripDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun tripCategoryIcon(category: TripCategory): ImageVector = when (category) {
    TripCategory.STAY -> Icons.Rounded.Hotel
    TripCategory.DINING -> Icons.Rounded.Restaurant
    TripCategory.TRANSPORT -> Icons.Rounded.DirectionsTransit
    TripCategory.FLIGHTS -> Icons.Rounded.Flight
    TripCategory.ACTIVITIES -> Icons.Rounded.ConfirmationNumber
    TripCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    TripCategory.OTHER -> Icons.Rounded.Category
}

fun tripCategoryTint(category: TripCategory): Color = when (category) {
    TripCategory.STAY -> Color(0xFF2F5D50)
    TripCategory.DINING -> Color(0xFFC8552B)
    TripCategory.TRANSPORT -> Color(0xFF3E6B7E)
    TripCategory.FLIGHTS -> Color(0xFF4F5D75)
    TripCategory.ACTIVITIES -> Color(0xFFB7791F)
    TripCategory.SHOPPING -> Color(0xFF7D5A50)
    TripCategory.OTHER -> Color(0xFF64716B)
}

fun travelerBadgeColor(colorIndex: Int): Color {
    val palette = listOf(
        Color(0xFF1F6F54), // Botanical Forest
        Color(0xFFC8552B), // Terracotta
        Color(0xFF4A6B5D), // Deep Sage
        Color(0xFFB7791F), // Ochre
        Color(0xFF5D534A), // Warm Umber
        Color(0xFF3A6B78)  // Slate Teal
    )
    return palette[abs(colorIndex) % palette.size]
}

@Composable
fun TripSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalTripSplitColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .then(if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.accent
                )
            }
        }
    }
}

/**
 * Clean flat surface container with subtle 1dp warm border and minimal/zero shadow.
 */
@Composable
fun TripSurfaceCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    cornerRadius: Dp = 14.dp,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTripSplitColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = containerColor ?: colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun TripSageCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTripSplitColors.current
    TripSurfaceCard(
        modifier = modifier,
        padding = padding,
        cornerRadius = cornerRadius,
        containerColor = colors.surfaceSage,
        onClick = onClick,
        content = content
    )
}

@Composable
fun TripStatusBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TravelerMonogramBadge(
    member: TripMember,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    InitialsAvatar(
        name = member.name,
        backgroundColor = travelerBadgeColor(member.colorIndex),
        size = size,
        modifier = modifier
    )
}

@Composable
fun InitialsAvatar(
    name: String,
    backgroundColor: Color,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    val initials = name
        .trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "T" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

@Composable
fun TripPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val colors = LocalTripSplitColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TripOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val colors = LocalTripSplitColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.borderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.inkPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = colors.accent
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TripExpenseRow(
    expense: TripExpense,
    membersById: Map<String, TripMember>,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    val tint = tripCategoryTint(expense.category)
    val payerLabel = if (expense.paidFromWallet) {
        "Common Wallet"
    } else {
        val name = membersById[expense.paidByMemberId]?.name ?: "Traveler"
        "Paid by $name"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("ts_expense_row_${expense.id}"),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = if (colors.isDark) 0.24f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tripCategoryIcon(expense.category),
                    contentDescription = expense.category.label,
                    tint = tint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (expense.paidFromWallet) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = colors.forest,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "$payerLabel · ${formatTripDate(expense.dateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTripMoney(expense.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = TripMonoFont,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.inkPrimary
                )
                Text(
                    text = if (expense.paidFromWallet) "Wallet" else "Split ${expense.shares.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expense.paidFromWallet) colors.forest else colors.inkMuted
                )
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ts_delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = colors.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
