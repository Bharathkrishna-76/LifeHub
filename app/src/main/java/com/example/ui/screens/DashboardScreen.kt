package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportAndShareHelper
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.CountUpCurrencyText
import com.example.ui.components.ErrorBanner
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.FrostedGlassPanel
import com.example.ui.components.HolographicCard
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.ProgressRing
import com.example.ui.components.ShareActionButton
import com.example.ui.components.SpinningRefreshAction
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.viewmodel.LifeHubUiState
import kotlin.math.roundToInt

/**
 * Module 1: Dashboard — FROSTED GLASS UI (Specular Multi-Layer Glassmorphism).
 * Translucent luminous panels over a vivid aurora gradient mesh, expand/collapse summary, favorite pin, and quick share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: LifeHubUiState,
    onQuickAddExpense: () -> Unit,
    onQuickAddIncome: () -> Unit,
    onQuickAddTrip: () -> Unit,
    onNavigateBillsAndEmi: () -> Unit,
    onNavigateHabits: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigatePlan: () -> Unit,
    onNavigateMoneyScore: () -> Unit,
    onNavigateWrapped: () -> Unit,
    onNavigateAchievements: () -> Unit,
    onNavigateProfile: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val symbol = state.settings.currencySymbol
    val todayKey = remember { DateAndFormatUtils.currentDateKey() }

    var entered by remember { mutableStateOf(false) }
    var expandBreakdown by remember { mutableStateOf(false) }
    var pinnedGoalFavorite by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { entered = true }

    val doneHabitsToday = state.habits.count { it.isDoneOn(todayKey) }
    val habitProgress = if (state.habits.isEmpty()) 0f else doneHabitsToday.toFloat() / state.habits.size
    val bestStreak = state.habits.maxOfOrNull { it.currentStreak } ?: 0
    val budget = state.settings.monthlyBudget.coerceAtLeast(1.0)
    val budgetRatio = (state.totalExpense / budget).toFloat().coerceIn(0f, 1f)
    val animatedBudgetBar by animateFloatAsState(
        targetValue = budgetRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "budget_bar"
    )
    val savingsRatePct = if (state.totalIncome > 0.0) {
        (((state.totalIncome - state.totalExpense).coerceAtLeast(0.0) / state.totalIncome) * 100).roundToInt()
    } else 0
    val topGoal = state.goals.firstOrNull { !it.completed } ?: state.goals.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .drawBehind {
                // Vivid Frosted-Glass Aurora Mesh Orbs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.accentViolet.copy(alpha = if (extended.isDark) 0.34f else 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.06f),
                        radius = size.width * 0.88f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.06f),
                    radius = size.width * 0.88f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.accentMint.copy(alpha = if (extended.isDark) 0.26f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.1f, size.height * 0.32f),
                        radius = size.width * 0.8f
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.32f),
                    radius = size.width * 0.8f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extended.accentCoral.copy(alpha = if (extended.isDark) 0.2f else 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.72f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.72f),
                    radius = size.width * 0.7f
                )
            }
    ) {
        PullToRefreshBox(
            isRefreshing = state.isSyncingMonth,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Frosted Glass Greeting Strip with Action Icons
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FROSTED GLASS HUB • ${DateAndFormatUtils.formatMonthLabel(state.selectedMonthKey).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.accentMint
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Hey, ${state.profile.displayName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = cs.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpinningRefreshAction(
                            isSpinning = state.isSyncingMonth,
                            tint = cs.onSurfaceVariant,
                            onClick = onRefresh,
                            modifier = Modifier.testTag("dashboard_refresh_button")
                        )
                        ShareActionButton(
                            tint = cs.onSurfaceVariant,
                            onClick = {
                                ExportAndShareHelper.shareMonthlyWrappedCard(context, state.wrappedSummary, symbol)
                            }
                        )
                        MoreOptionsAction(
                            options = listOf(
                                "Monthly Wrapped" to onNavigateWrapped,
                                "Money Score" to onNavigateMoneyScore,
                                "Achievements" to onNavigateAchievements,
                                "Settings & Export" to onNavigateProfile
                            )
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                item(key = "error") {
                    ErrorBanner(
                        message = state.errorMessage,
                        onDismiss = onDismissError,
                        onRetry = onRefresh
                    )
                }
            }

            // Hero Frosted Glass Balance Card with Expand/Collapse Cashflow Details
            item(key = "hero_balance") {
                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically { it / 5 }
                ) {
                    FrostedGlassPanel(
                        accentGlow = extended.accentMint,
                        cornerRadius = 30.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_balance_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Balance",
                                style = MaterialTheme.typography.labelLarge,
                                color = extended.mutedText
                            )
                            Surface(
                                onClick = onNavigateWrapped,
                                shape = RoundedCornerShape(50),
                                color = extended.accentViolet.copy(alpha = 0.24f),
                                border = BorderStroke(1.dp, extended.accentViolet.copy(alpha = 0.55f)),
                                modifier = Modifier.testTag("dashboard_wrapped_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Monthly Wrapped",
                                        tint = extended.accentViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Holo Wrapped",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = extended.accentViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        CountUpCurrencyText(
                            targetAmount = state.netBalance,
                            currencySymbol = symbol,
                            style = MaterialTheme.typography.displayLarge,
                            color = cs.onSurface
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BalanceDeltaChip(
                                label = "Income",
                                amount = DateAndFormatUtils.formatCurrency(state.totalIncome, symbol, compact = true),
                                icon = Icons.Rounded.ArrowDownward,
                                accent = extended.accentMint,
                                modifier = Modifier.weight(1f)
                            )
                            BalanceDeltaChip(
                                label = "Expenses",
                                amount = DateAndFormatUtils.formatCurrency(state.totalExpense, symbol, compact = true),
                                icon = Icons.Rounded.ArrowUpward,
                                accent = extended.accentCoral,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Expand/Collapse Cashflow Insight Drawer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expandBreakdown = !expandBreakdown }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expandBreakdown) "Hide quick telemetry" else "Expand budget & savings telemetry",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.mutedText
                            )
                            ExpandCollapseChevron(expanded = expandBreakdown, tint = extended.accentMint)
                        }

                        AnimatedVisibility(
                            visible = expandBreakdown,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { animatedBudgetBar },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = if (budgetRatio > 0.9f) extended.accentCoral else extended.accentMint,
                                    trackColor = cs.surfaceContainerHigh
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Budget used: ${(budgetRatio * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.mutedText
                                    )
                                    Text(
                                        text = "Savings rate: $savingsRatePct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.accentMint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frosted Glass Quick Actions Carousel
            item(key = "quick_actions") {
                val actions = listOf(
                    QuickActionSpec("Add Expense", Icons.Rounded.Add, extended.accentCoral, "qa_expense", onQuickAddExpense),
                    QuickActionSpec("Trip Expense", Icons.Rounded.FlightTakeoff, extended.accentAmber, "qa_trip", onQuickAddTrip),
                    QuickActionSpec("Bills & EMI", Icons.Rounded.NotificationsActive, extended.accentViolet, "qa_bills", onNavigateBillsAndEmi),
                    QuickActionSpec("Add Income", Icons.Rounded.ArrowDownward, extended.accentMint, "qa_income", onQuickAddIncome),
                    QuickActionSpec("Cyber Habits", Icons.Rounded.LocalFireDepartment, extended.accentAmber, "qa_habits", onNavigateHabits),
                    QuickActionSpec("3D Goals", Icons.Rounded.Savings, extended.accentCyan, "qa_goals", onNavigateGoals),
                    QuickActionSpec("Kanban Plan", Icons.Rounded.TaskAlt, extended.accentViolet, "qa_tasks", onNavigatePlan),
                    QuickActionSpec("Holo Badges", Icons.Rounded.EmojiEvents, extended.accentLime, "qa_badges", onNavigateAchievements)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(actions, key = { it.tag }) { action ->
                        Surface(
                            onClick = action.onClick,
                            shape = RoundedCornerShape(18.dp),
                            color = extended.glassSurface,
                            border = BorderStroke(1.dp, action.color.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag(action.tag)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(action.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.label,
                                        tint = action.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Bento Row: Monthly Spend + Net Savings
            item(key = "bento_finance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FrostedGlassPanel(
                        accentGlow = extended.accentCoral,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Monthly Spend",
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.mutedText
                        )
                        Spacer(Modifier.height(6.dp))
                        CountUpCurrencyText(
                            targetAmount = state.totalExpense,
                            currencySymbol = symbol,
                            style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface,
                            compact = true
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedBudgetBar },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (budgetRatio > 0.9f) extended.accentCoral else extended.accentMint,
                            trackColor = cs.surfaceContainerHigh
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${(budgetRatio * 100).roundToInt()}% of ${DateAndFormatUtils.formatCurrency(budget, symbol, compact = true)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.mutedText
                        )
                    }

                    FrostedGlassPanel(
                        accentGlow = extended.accentMint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Net Savings",
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.mutedText
                        )
                        Spacer(Modifier.height(6.dp))
                        CountUpCurrencyText(
                            targetAmount = state.netBalance.coerceAtLeast(0.0),
                            currencySymbol = symbol,
                            style = MaterialTheme.typography.titleLarge,
                            color = extended.accentMint,
                            compact = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = extended.accentMint.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "$savingsRatePct% saved rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.accentMint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Money Score + Habit Ring Glass Pair
            item(key = "score_and_habits") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FrostedGlassPanel(
                        accentGlow = extended.accentCyan,
                        onClick = onNavigateMoneyScore,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_money_score_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Money Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.mutedText
                            )
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = extended.accentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                progress = state.moneyScore.totalScore / 100f,
                                color = extended.accentCyan,
                                strokeWidth = 7.dp,
                                size = 54.dp
                            ) {
                                Text(
                                    text = "${state.moneyScore.totalScore}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = cs.onSurface
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = state.moneyScore.tierLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = extended.accentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Holo index",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                        }
                    }

                    FrostedGlassPanel(
                        accentGlow = extended.accentLime,
                        onClick = onNavigateHabits,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_habits_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Habit Rings",
                                style = MaterialTheme.typography.labelMedium,
                                color = extended.mutedText
                            )
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = extended.accentAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                progress = habitProgress,
                                color = extended.accentLime,
                                strokeWidth = 7.dp,
                                size = 54.dp
                            ) {
                                Text(
                                    text = "$doneHabitsToday/${state.habits.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = cs.onSurface
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${bestStreak}d Streak",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = extended.accentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "CyberFit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                        }
                    }
                }
            }

            // Pinned Goal Card with Favorite Toggle
            item(key = "goals_spotlight") {
                FrostedGlassPanel(
                    accentGlow = extended.accentViolet,
                    onClick = onNavigateGoals,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_goals_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PINNED 3D VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.accentViolet
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = topGoal?.title ?: "Launch your first savings goal",
                                style = MaterialTheme.typography.titleLarge,
                                color = cs.onSurface
                            )
                        }
                        FavoriteToggleIcon(
                            isFavorite = pinnedGoalFavorite,
                            onToggle = { pinnedGoalFavorite = !pinnedGoalFavorite }
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open goals",
                            tint = extended.mutedText
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (topGoal != null) {
                        LinearProgressIndicator(
                            progress = { topGoal.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = extended.accentViolet,
                            trackColor = cs.surfaceContainerHigh
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${DateAndFormatUtils.formatCurrency(topGoal.savedAmount, symbol)} saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )
                            Text(
                                text = "${(topGoal.progressFraction * 100).roundToInt()}% of ${DateAndFormatUtils.formatCurrency(topGoal.targetAmount, symbol)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = extended.accentViolet
                            )
                        }
                    } else {
                        Text(
                            text = "Save for a New Phone, Vacation, Laptop, or Bike in tactile 3D vaults.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.mutedText
                        )
                    }
                }
            }

            // Holographic Achievements XP Banner
            item(key = "xp_strip") {
                HolographicCard(
                    onClick = onNavigateAchievements,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_achievements_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(extended.accentAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = "Achievements",
                                    tint = extended.accentAmber
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Level ${state.playerLevel} • ${state.totalXp} XP",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.achievements.count { it.unlocked }} of ${state.achievements.size} holographic badges unlocked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.mutedText
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = extended.accentMint
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
        }
    }
}

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun BalanceDeltaChip(
    label: String,
    amount: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
