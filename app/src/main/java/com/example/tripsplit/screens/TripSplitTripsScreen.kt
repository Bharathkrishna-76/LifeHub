package com.example.tripsplit.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripsplit.components.DestinationThemeGrid
import com.example.tripsplit.components.TripConfirmDialog
import com.example.tripsplit.components.TripPrimaryButton
import com.example.tripsplit.components.TripSageCard
import com.example.tripsplit.components.TripSectionHeader
import com.example.tripsplit.components.TripStatusBadge
import com.example.tripsplit.components.TripSurfaceCard
import com.example.tripsplit.components.formatTripMoney
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.theme.LocalTripSplitColors
import com.example.tripsplit.theme.TripMonoFont
import com.example.tripsplit.viewmodel.TripCardOverview
import com.example.tripsplit.viewmodel.TripSplitUiState
import com.example.ui.components.rememberResponsiveMetrics

@Composable
fun TripSplitTripsScreen(
    uiState: TripSplitUiState,
    onSelectTrip: (String) -> Unit,
    onCreateTripClick: (DestinationCover?) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTripSplitColors.current
    var pendingDelete by remember { mutableStateOf<Trip?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }

    val displayedTrips = remember(uiState.allTrips, uiState.favoriteTripIds, favoritesOnly) {
        if (favoritesOnly) {
            uiState.allTrips.filter { uiState.favoriteTripIds.contains(it.trip.id) }
        } else {
            uiState.allTrips
        }
    }

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
                            text = "Your Trips",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.allTrips.size} trip${if (uiState.allTrips.size == 1) "" else "s"} planned",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TripPrimaryButton(
                        text = "New Trip",
                        icon = Icons.Rounded.Add,
                        onClick = { onCreateTripClick(null) },
                        testTag = "ts_trips_create_btn"
                    )
                }
            }

            if (uiState.allTrips.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripFilterChip(
                            label = "All (${uiState.allTrips.size})",
                            selected = !favoritesOnly,
                            onClick = { favoritesOnly = false },
                            testTag = "ts_trips_filter_all"
                        )
                        TripFilterChip(
                            label = "Favorites (${uiState.favoriteTripIds.size})",
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = true },
                            testTag = "ts_trips_filter_fav"
                        )
                    }
                }
            }

        if (displayedTrips.isEmpty()) {
            item {
                TripSageCard(padding = 16.dp) {
                    Text(
                        text = if (favoritesOnly) "No favorite trips yet" else "No trips created yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (favoritesOnly) {
                            "Tap the heart icon on any trip to save it here."
                        } else {
                            "Pick a destination below or tap New Trip to create your first shared travel budget."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                }
            }
            if (!favoritesOnly) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TripSectionHeader(title = "Start from a Destination")
                        DestinationThemeGrid(onSelectDestination = { cover -> onCreateTripClick(cover) })
                    }
                }
            }
        } else {
            items(displayedTrips, key = { it.trip.id }) { overview ->
                val isActive = overview.trip.id == uiState.activeTrip?.id
                val isFav = uiState.favoriteTripIds.contains(overview.trip.id)
                CleanTripCard(
                    overview = overview,
                    isActive = isActive,
                    isFavorite = isFav,
                    onSelect = { onSelectTrip(overview.trip.id) },
                    onToggleFavorite = { onToggleFavorite(overview.trip.id) },
                    onDeleteRequest = { pendingDelete = overview.trip }
                )
            }
        }
    }
    }

    pendingDelete?.let { target ->
        TripConfirmDialog(
            title = "Delete \"${target.title}\"?",
            message = "This removes all expenses, balances, and wallet entries for this trip. Your Daily Expenses tracker stays untouched.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteTrip(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun TripFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = LocalTripSplitColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.inkPrimary else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.inkPrimary else colors.borderSubtle),
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.surface else colors.inkSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CleanTripCard(
    overview: TripCardOverview,
    isActive: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val colors = LocalTripSplitColors.current
    val trip = overview.trip
    val progress = if (trip.totalBudget > 0.0) {
        (overview.totalSpent / trip.totalBudget).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("ts_trip_card_${trip.id}"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) colors.forest else colors.borderSubtle
        ),
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.6f)
            ) {
                Image(
                    painter = painterResource(id = trip.cover.drawableRes),
                    contentDescription = trip.destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x40111915),
                                    0.5f to Color.Transparent,
                                    1.0f to Color(0xCC111915)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TripStatusBadge(
                        text = if (isActive) "Active" else "Tap to open",
                        backgroundColor = if (isActive) colors.forest else Color(0xCC16241E),
                        contentColor = Color.White
                    )
                    Row {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("ts_fav_trip_${trip.id}")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (isFavorite) colors.accent else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteRequest,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("ts_delete_trip_${trip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete trip",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = trip.destination,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTripMoney(overview.totalSpent, trip.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TripMonoFont),
                        color = colors.inkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${trip.startDateLabel} – ${trip.endDateLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted,
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
                                text = "${overview.memberCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (trip.totalBudget > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "of ${formatTripMoney(trip.totalBudget, trip.currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.surfaceMuted)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (overview.totalSpent > trip.totalBudget && trip.totalBudget > 0) colors.danger else colors.forest)
                    )
                }
            }
        }
    }
}
