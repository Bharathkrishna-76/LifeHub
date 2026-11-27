package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.engine.AchievementEngine
import com.example.domain.model.AchievementItem
import com.example.ui.components.BackArrowAction
import com.example.ui.components.CyberCyan
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.FilterActionButton
import com.example.ui.components.HolographicCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LimePulse
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber

private enum class BadgeFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Holo Unlocked"),
    LOCKED("Locked"),
    SHOWCASED("Favorites")
}

/**
 * Module 9: Achievements Screen — HOLOGRAPHIC COLLECTIBLE TROPHY VAULT.
 * Animated iridescent foil cards for Player Level & unlocked badges, showcase favorite toggles,
 * and toggleable filter chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<AchievementItem>,
    totalXp: Int,
    playerLevel: Int,
    onBack: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val inkColor = if (isDark) Color.White else cs.onSurface
    val mutedColor = if (isDark) Color(0xFFB5C0DC) else extended.mutedText
    val cyanAccent = extended.accentCyan
    val amberAccent = extended.accentAmber
    val limeAccent = extended.accentLime
    val lockedBg = if (isDark) Color(0xFF121524) else Color.White
    val lockedBorder = if (isDark) Color.White.copy(alpha = 0.12f) else cs.outline.copy(alpha = 0.35f)
    val chipUnselectedBg = if (isDark) Color(0xFF171A2E) else cs.surfaceVariant

    val xpInLevel = AchievementEngine.xpIntoCurrentLevel(totalXp)
    val levelProgress = xpInLevel / 500f
    var showFilters by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(BadgeFilter.ALL) }
    var favoriteCodes by remember { mutableStateOf(setOf<String>()) }

    val filtered = remember(achievements, activeFilter, favoriteCodes) {
        val base = when (activeFilter) {
            BadgeFilter.ALL -> achievements
            BadgeFilter.UNLOCKED -> achievements.filter { it.unlocked }
            BadgeFilter.LOCKED -> achievements.filter { !it.unlocked }
            BadgeFilter.SHOWCASED -> achievements.filter { it.code in favoriteCodes }
        }
        base.sortedByDescending { it.code in favoriteCodes }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF070914), Color(0xFF110F27), Color(0xFF080A14))
                    } else {
                        listOf(Color(0xFFF5F7FC), Color(0xFFEEEBFF), Color(0xFFF4F8FB))
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "HOLOGRAPHIC VAULT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = cyanAccent
                            )
                            Text("Achievements & XP", color = inkColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        BackArrowAction(
                            onClick = onBack,
                            tint = inkColor,
                            modifier = Modifier.testTag("achievements_back")
                        )
                    },
                    actions = {
                        FilterActionButton(
                            hasActiveFilter = activeFilter != BadgeFilter.ALL,
                            tint = inkColor,
                            onClick = { showFilters = !showFilters }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("achievements_grid"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player XP Level Holographic Hero Banner
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HolographicCard(
                        cornerRadius = 28.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PLAYER RANK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = amberAccent
                                )
                                Text(
                                    text = "Level $playerLevel",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = inkColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = limeAccent.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = limeAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$totalXp XP",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = limeAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = amberAccent,
                            trackColor = if (isDark) Color.White.copy(alpha = 0.14f) else cs.outline.copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "$xpInLevel / 500 XP to Level ${playerLevel + 1} • ${achievements.count { it.unlocked }}/${achievements.size} unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )

                        AnimatedVisibility(
                            visible = showFilters,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(BadgeFilter.entries, key = { it.name }) { filter ->
                                    FilterChip(
                                        selected = activeFilter == filter,
                                        onClick = { activeFilter = filter },
                                        label = { Text(filter.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = cyanAccent.copy(alpha = 0.22f),
                                            selectedLabelColor = cyanAccent,
                                            containerColor = chipUnselectedBg,
                                            labelColor = mutedColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                items(filtered, key = { it.code }) { badge ->
                    val isFav = badge.code in favoriteCodes
                    if (badge.unlocked) {
                        HolographicCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BadgeCardInnerContent(
                                badge = badge,
                                isFavorite = isFav,
                                onToggleFav = {
                                    favoriteCodes = if (isFav) favoriteCodes - badge.code else favoriteCodes + badge.code
                                }
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = lockedBg,
                            border = BorderStroke(1.dp, lockedBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                BadgeCardInnerContent(
                                    badge = badge,
                                    isFavorite = isFav,
                                    onToggleFav = {
                                        favoriteCodes = if (isFav) favoriteCodes - badge.code else favoriteCodes + badge.code
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCardInnerContent(
    badge: AchievementItem,
    isFavorite: Boolean,
    onToggleFav: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val inkColor = if (isDark) Color.White else cs.onSurface
    val mutedColor = if (isDark) Color(0xFFA6B1CD) else extended.mutedText
    val accent = if (badge.unlocked) extended.accentMint else mutedColor
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FavoriteToggleIcon(
                isFavorite = isFavorite,
                activeColor = extended.accentAmber,
                onToggle = onToggleFav
            )
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (badge.unlocked) extended.accentAmber.copy(alpha = 0.22f)
                    else if (isDark) Color.White.copy(alpha = 0.08f) else cs.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (badge.unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = badge.title,
                tint = if (badge.unlocked) extended.accentAmber else mutedColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.titleSmall,
            color = inkColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = badge.description,
            style = MaterialTheme.typography.bodySmall,
            color = mutedColor,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accent.copy(alpha = 0.16f)
        ) {
            Text(
                text = "+${badge.xpReward} XP • ${badge.progressLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
