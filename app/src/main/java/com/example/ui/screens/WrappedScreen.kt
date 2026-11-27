package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.export.ExportAndShareHelper
import com.example.domain.model.MonthlyWrappedSummary
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.CloseAction
import com.example.ui.components.Extruded3DButton
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.HolographicCard
import com.example.ui.components.ShareActionButton
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LimePulse
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber
import kotlinx.coroutines.launch

/**
 * Module 8: Monthly Wrapped — HOLOGRAPHIC STORY EXPERIENCE + PNG POSTER SHARE.
 * Shimmering prismatic foil story frames, favorite chapter reactions, and tactile 3D share action.
 */
@Composable
fun WrappedScreen(
    wrapped: MonthlyWrappedSummary,
    currencySymbol: String,
    onClose: () -> Unit,
    onShareMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val inkColor = if (isDark) Color.White else cs.onSurface
    val scope = rememberCoroutineScope()
    val slideCount = 5
    val pagerState = rememberPagerState(pageCount = { slideCount })
    var favoritedPages by remember { mutableStateOf(setOf<Int>()) }

    fun shareWrappedPoster() {
        val res = ExportAndShareHelper.shareMonthlyWrappedCard(context, wrapped, currencySymbol)
        if (res.isFailure) {
            onShareMessage("Could not share image: ${res.exceptionOrNull()?.localizedMessage}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF090B11) else Color(0xFFF4F6FB))
            .testTag("wrapped_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_wrapped_bg),
            contentDescription = "Monthly Wrapped artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xB3090B11),
                                Color(0x73090B11),
                                Color(0xE6090B11)
                            )
                        } else {
                            listOf(
                                Color(0xDCF5F7FC),
                                Color(0xB8EEF2FA),
                                Color(0xF0F5F7FC)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Story progress segments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(slideCount) { index ->
                    val active = index <= pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) extended.accentMint
                                else if (isDark) Color.White.copy(alpha = 0.25f) else cs.outline.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Top Story Bar with Favorite, Share, and Close Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = extended.accentMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${wrapped.monthLabel.uppercase()} WRAPPED",
                        style = MaterialTheme.typography.labelMedium,
                        color = inkColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentFav = pagerState.currentPage in favoritedPages
                    FavoriteToggleIcon(
                        isFavorite = currentFav,
                        activeColor = extended.accentCoral,
                        onToggle = {
                            val page = pagerState.currentPage
                            favoritedPages = if (currentFav) favoritedPages - page else favoritedPages + page
                        }
                    )
                    ShareActionButton(
                        onClick = { shareWrappedPoster() },
                        tint = inkColor
                    )
                    CloseAction(
                        onClick = onClose,
                        tint = inkColor,
                        modifier = Modifier.testTag("wrapped_close_button")
                    )
                }
            }

            // Story Pager with Holographic Foil Frames
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0 -> HolographicWrappedCard(
                            kicker = "CHAPTER 01 • CASHFLOW",
                            accent = extended.accentMint,
                            headline = DateAndFormatUtils.formatCurrency(wrapped.totalSpending, currencySymbol),
                            subtitle = "Total Spending in ${wrapped.monthLabel}",
                            footerHighlight = "Net Savings: ${DateAndFormatUtils.formatCurrency(wrapped.totalSavings, currencySymbol)}"
                        )
                        1 -> HolographicWrappedCard(
                            kicker = "CHAPTER 02 • TOP VIBE",
                            accent = extended.accentCoral,
                            headline = wrapped.topCategory?.label ?: "Zero Spend",
                            subtitle = "Your #1 spending category this month",
                            footerHighlight = if (wrapped.topCategory != null) {
                                "${DateAndFormatUtils.formatCurrency(wrapped.topCategoryAmount, currencySymbol)} spent here"
                            } else "Log an expense to reveal your vibe!"
                        )
                        2 -> HolographicWrappedCard(
                            kicker = "CHAPTER 03 • MOMENTUM",
                            accent = extended.accentAmber,
                            headline = "${wrapped.longestStreak} Days",
                            subtitle = "Longest Habit Streak 🔥",
                            footerHighlight = "Best Saving Peak: ${DateAndFormatUtils.formatCurrency(wrapped.bestSavingAmount, currencySymbol)} (${wrapped.bestSavingMonthLabel})"
                        )
                        3 -> HolographicWrappedCard(
                            kicker = "CHAPTER 04 • MONEY SCORE",
                            accent = extended.accentCyan,
                            headline = "${wrapped.moneyScore} / 100",
                            subtitle = "Rank: ${wrapped.moneyTier}",
                            footerHighlight = "Calculated from savings, budget, habits, goals & spend control"
                        )
                        else -> HolographicWrappedCard(
                            kicker = "CHAPTER 05 • LEVEL UP",
                            accent = extended.accentLime,
                            headline = "${wrapped.unlockedAchievementCount} Badges",
                            subtitle = "${wrapped.totalXp} Total XP Earned",
                            footerHighlight = "Share your personalized ${wrapped.monthLabel} Wrapped poster!"
                        )
                    }
                }
            }

            // Bottom Controls: Next Slide + Tactile 3D Share Poster CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < slideCount - 1) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White.copy(alpha = 0.16f) else cs.surface,
                            contentColor = inkColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Next Slide", fontWeight = FontWeight.Bold)
                    }
                }

                Extruded3DButton(
                    label = "Share Poster",
                    icon = Icons.Rounded.Share,
                    topColor = LimePulse,
                    sideColor = Color(0xFF6B9900),
                    contentColor = Color(0xFF0A1400),
                    onClick = { shareWrappedPoster() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_wrapped_button")
                )
            }
        }
    }
}

@Composable
private fun HolographicWrappedCard(
    kicker: String,
    accent: Color,
    headline: String,
    subtitle: String,
    footerHighlight: String
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val isDark = extended.isDark
    val inkColor = if (isDark) Color.White else cs.onSurface
    val subColor = if (isDark) Color(0xFFD6DEEE) else extended.mutedText

    HolographicCard(
        cornerRadius = 32.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.18f)
            ) {
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = inkColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleLarge,
                color = subColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color.White.copy(alpha = 0.08f) else accent.copy(alpha = 0.10f)
            ) {
                Text(
                    text = footerHighlight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
