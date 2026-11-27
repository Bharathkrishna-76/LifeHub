package com.example.tripsplit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripsplit.domain.CategorySpend
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont

/**
 * Compact flat sage route strip showing Origin -> Waypoints -> Destination cleanly.
 */
@Composable
fun CompactTripRouteStrip(
    trip: Trip,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    TripSageCard(modifier = modifier, padding = 12.dp, cornerRadius = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = null,
                tint = colors.forest,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = trip.origin,
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
            ) {
                val y = size.height / 2f
                drawLine(
                    color = colors.forest.copy(alpha = 0.45f),
                    start = Offset(4f, y),
                    end = Offset(size.width - 4f, y),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
                drawCircle(color = colors.forest, radius = 3.5f, center = Offset(4f, y))
                drawCircle(color = colors.accent, radius = 4f, center = Offset(size.width - 4f, y))
            }
            Text(
                text = trip.destination,
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
        }
        if (trip.waypoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Stops: ${trip.waypoints.joinToString(" · ")}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean flat category spending breakdown with interactive expandable rows.
 */
@Composable
fun SpendingBreakdownChartCard(
    breakdown: List<CategorySpend>,
    currencySymbol: String,
    expenses: List<TripExpense> = emptyList(),
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    var expandedCategory by remember { mutableStateOf<TripCategory?>(null) }

    TripSurfaceCard(modifier = modifier, padding = 14.dp) {
        if (breakdown.isEmpty()) {
            Text(
                text = "Expenses by category will appear here once logged.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
            return@TripSurfaceCard
        }

        // Crisp 8dp segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceMuted),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            breakdown.forEach { slice ->
                Box(
                    modifier = Modifier
                        .weight(slice.percentage.coerceAtLeast(0.03f))
                        .fillMaxHeight()
                        .background(tripCategoryTint(slice.category))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            breakdown.forEach { slice ->
                val tint = tripCategoryTint(slice.category)
                val isExpanded = expandedCategory == slice.category
                val animatedWidth by animateFloatAsState(
                    targetValue = slice.percentage,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "cat_${slice.category.name}"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            expandedCategory = if (isExpanded) null else slice.category
                        }
                        .testTag("ts_breakdown_cat_${slice.category.name}")
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = tripCategoryIcon(slice.category),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = slice.category.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "(${slice.count})",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatTripMoney(slice.amount, currencySymbol),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TripMonoFont),
                                color = colors.inkPrimary
                            )
                            Text(
                                text = "${(slice.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.surfaceMuted)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedWidth.coerceAtLeast(0.03f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(tint)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded && expenses.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val matching = expenses.filter { it.category == slice.category }.take(4)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceMuted)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            matching.forEach { exp ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = exp.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.inkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatTripMoney(exp.amount, currencySymbol),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.inkPrimary
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
