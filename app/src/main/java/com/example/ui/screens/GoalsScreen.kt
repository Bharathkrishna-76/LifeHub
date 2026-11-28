package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.GoalPreset
import com.example.domain.model.SavingsGoalItem
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.AdaptivePairLayout
import com.example.ui.components.CloseAction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.Extruded3DButton
import com.example.ui.components.Extruded3DCard
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.FilterActionButton
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.SavingActionButton
import com.example.ui.components.goalPresetIcon
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LimePulse
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.LifeHubUiState
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class GoalFilterMode(val label: String) {
    ALL("All Vaults"),
    ACTIVE("In Progress"),
    FUNDED("Funded"),
    STARRED("Priority")
}

/**
 * Module 4: Savings Goals — TACTILE 3D UI ("3D DREAM VAULTS").
 * Extruded 3D cards with physical bottom slabs, press-depth spring physics, 3D beveled progress bars,
 * extruded 3D contribution buttons, spotlight favorites, and confetti celebration modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: LifeHubUiState,
    onAddGoal: (String, String, GoalPreset, Double, Double, String, () -> Unit) -> Unit,
    onContribute: (SavingsGoalItem, Double, () -> Unit) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onDismissCelebration: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val symbol = state.settings.currencySymbol

    var showCreateSheet by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalItem?>(null) }
    var filterMode by remember { mutableStateOf(GoalFilterMode.ALL) }
    var showFilterRow by remember { mutableStateOf(false) }
    var starredGoalIds by remember { mutableStateOf(setOf<String>()) }

    val totalSaved = state.goals.sumOf { it.savedAmount }
    val totalTarget = state.goals.sumOf { it.targetAmount }
    val fundedCount = state.goals.count { it.completed }
    val overallProgress = if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    val filteredGoals = remember(state.goals, filterMode, starredGoalIds) {
        val base = when (filterMode) {
            GoalFilterMode.ALL -> state.goals
            GoalFilterMode.ACTIVE -> state.goals.filter { !it.completed }
            GoalFilterMode.FUNDED -> state.goals.filter { it.completed }
            GoalFilterMode.STARRED -> state.goals.filter { it.id in starredGoalIds }
        }
        base.sortedByDescending { it.id in starredGoalIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = if (extended.isDark) Color(0xFF100E24) else Color(0xFFF0EEFF),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                // Tactile 3D Extruded FAB
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .padding(bottom = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF471FA3))
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(listOf(CyberViolet, Color(0xFF6B3CE6)))
                            )
                            .border(1.2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .clickable { showCreateSheet = true }
                            .testTag("add_goal_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Create savings goal",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("goals_list"),
                    contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 3D Extruded Hero Vault
                    item(key = "goals_3d_hero") {
                        Extruded3DCard(
                            faceColor = if (extended.isDark) Color(0xFF1E1A42) else Color.White,
                            depthColor = if (extended.isDark) Color(0xFF4B28A8) else Color(0xFFC5B8F6),
                            highlightColor = Color.White.copy(alpha = if (extended.isDark) 0.32f else 0.9f),
                            depthOffset = 8.dp,
                            cornerRadius = 28.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3D DREAM VAULTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.accentViolet,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Savings Goals",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = cs.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = NeonMint.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "$fundedCount FUNDED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = extended.accentMint,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    FilterActionButton(
                                        hasActiveFilter = filterMode != GoalFilterMode.ALL,
                                        tint = cs.onSurface,
                                        onClick = { showFilterRow = !showFilterRow }
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = DateAndFormatUtils.formatCurrency(totalSaved, symbol),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "backed across ${state.goals.size} vault(s) • target ${DateAndFormatUtils.formatCurrency(totalTarget, symbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.mutedText
                            )

                        Spacer(Modifier.height(12.dp))
                        Beveled3DProgressBar(
                            progress = overallProgress,
                            startColor = CyberViolet,
                            endColor = ElectricCyan
                        )

                        AnimatedVisibility(
                            visible = showFilterRow,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(GoalFilterMode.entries, key = { it.name }) { mode ->
                                    FilterChip(
                                        selected = filterMode == mode,
                                        onClick = { filterMode = mode },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredGoals.isEmpty()) {
                    item(key = "empty_goals") {
                        EmptyStateCard(
                            icon = Icons.Rounded.RocketLaunch,
                            title = if (state.goals.isEmpty()) "Fund your next big win" else "No goals match filter",
                            subtitle = if (state.goals.isEmpty()) {
                                "Launch tactile 3D vaults for a New Phone, Vacation, Laptop, or Bike."
                            } else "Clear the active filter to view all savings vaults.",
                            actionLabel = if (state.goals.isEmpty()) "Launch a Goal" else "Show All Vaults",
                            onAction = {
                                if (state.goals.isEmpty()) showCreateSheet = true
                                else filterMode = GoalFilterMode.ALL
                            },
                            accentColor = extended.accentViolet
                        )
                    }
                } else {
                    items(filteredGoals, key = { it.id }) { goal ->
                        val isStarred = goal.id in starredGoalIds
                        Tactile3DGoalCard(
                            goal = goal,
                            currencySymbol = symbol,
                            isStarred = isStarred,
                            onToggleStar = {
                                starredGoalIds = if (isStarred) starredGoalIds - goal.id else starredGoalIds + goal.id
                            },
                            onContributeClick = { contributingGoal = goal },
                            onDeleteClick = { onDeleteGoal(goal.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }

        // Goal Completion Celebration Screen Overlay
        val celebrated = state.celebratedGoal
        if (celebrated != null) {
            GoalCelebrationOverlay(
                goal = celebrated,
                currencySymbol = symbol,
                onDismiss = onDismissCelebration
            )
        }
    }

    if (showCreateSheet) {
        CreateGoalSheet(
            currencySymbol = symbol,
            isSaving = state.isSaving,
            onDismiss = { showCreateSheet = false },
            onSave = { docId, title, preset, target, initial, targetMonth ->
                onAddGoal(docId, title, preset, target, initial, targetMonth) {
                    showCreateSheet = false
                }
            }
        )
    }

    val activeContrib = contributingGoal
    if (activeContrib != null) {
        ContributeGoalSheet(
            goal = activeContrib,
            currencySymbol = symbol,
            isSaving = state.isSaving,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount ->
                onContribute(activeContrib, amount) {
                    contributingGoal = null
                }
            }
        )
    }
}

@Composable
private fun Beveled3DProgressBar(
    progress: Float,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    val anim by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bevel_3d_progress"
    )
    val isDark = LocalLifeHubColors.current.isDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFFE2E8F0))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1),
                RoundedCornerShape(50)
            )
            .padding(2.dp)
    ) {
        if (anim > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anim)
                    .height(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(startColor, endColor)))
            ) {
                // Specular 3D top highlight ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
private fun Tactile3DGoalCard(
    goal: SavingsGoalItem,
    currencySymbol: String,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onContributeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    var showBreakdown by remember { mutableStateOf(false) }

    val accent = if (goal.completed) extended.accentMint else extended.accentViolet
    val depthTone = when {
        goal.completed -> if (extended.isDark) Color(0xFF0A6648) else Color(0xFF8BE5C4)
        isStarred -> if (extended.isDark) Color(0xFF9A5B00) else Color(0xFFFFD180)
        else -> if (extended.isDark) Color(0xFF3E227D) else Color(0xFFC9C0F2)
    }
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    val animatedPct by animateFloatAsState(
        targetValue = goal.progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "goal_pct_3d"
    )

    Extruded3DCard(
        faceColor = if (extended.isDark) Color(0xFF191736) else Color.White,
        depthColor = depthTone,
        highlightColor = Color.White.copy(alpha = if (extended.isDark) 0.28f else 0.95f),
        depthOffset = 7.dp,
        cornerRadius = 24.dp,
        onClick = { showBreakdown = !showBreakdown },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Raised 3D icon cube
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(bottom = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 3.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(accent.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(15.dp))
                        .background(accent.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (goal.completed) Icons.Rounded.EmojiEvents else goalPresetIcon(goal.category),
                        contentDescription = goal.category.label,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (goal.completed) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = extended.accentMint.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.accentMint,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = buildString {
                        append(goal.category.label)
                        if (goal.targetMonth.isNotBlank()) {
                            append(" • ")
                            append(goal.targetMonth)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FavoriteToggleIcon(
                isFavorite = isStarred,
                activeColor = SolarAmber,
                onToggle = onToggleStar
            )

            MoreOptionsAction(
                options = buildList {
                    if (!goal.completed) add("Boost Vault" to onContributeClick)
                    add((if (isStarred) "Unpin Goal" else "Spotlight Goal") to onToggleStar)
                    add("Delete Goal" to onDeleteClick)
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        Beveled3DProgressBar(
            progress = animatedPct,
            startColor = if (goal.completed) NeonMint else CyberViolet,
            endColor = if (goal.completed) LimePulse else ElectricCyan
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${DateAndFormatUtils.formatCurrency(goal.savedAmount, currencySymbol)} / ${DateAndFormatUtils.formatCurrency(goal.targetAmount, currencySymbol)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (goal.completed) "Vault unlocked!"
                    else "${DateAndFormatUtils.formatCurrency(remaining, currencySymbol)} left to unlock",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(animatedPct * 100).roundToInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.width(4.dp))
                ExpandCollapseChevron(expanded = showBreakdown)
            }
        }

        AnimatedVisibility(
            visible = showBreakdown,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cs.surfaceContainerHigh.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val weeklyBoost = if (remaining > 0) remaining / 8.0 else 0.0
                    Text(
                        text = "8-week sprint pace",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${DateAndFormatUtils.formatCurrency(weeklyBoost, currencySymbol)} / wk",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (!goal.completed) {
            Spacer(Modifier.height(14.dp))
            Extruded3DButton(
                label = "Add Contribution",
                icon = Icons.Rounded.Savings,
                topColor = CyberViolet,
                sideColor = Color(0xFF471FA3),
                onClick = onContributeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_contribute_${goal.id}")
            )
        }
    }
}

@Composable
private fun GoalCelebrationOverlay(
    goal: SavingsGoalItem,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(goal.id) {
        progress.animateTo(1f, tween(durationMillis = 1800, easing = LinearEasing))
    }

    val particles = remember {
        val colors = listOf(NeonMint, CyberViolet, ElectricCyan, HotCoral, SolarAmber, LimePulse)
        List(36) { idx ->
            Triple(
                (idx * 10f),
                colors[idx % colors.size],
                0.5f + (idx % 5) * 0.14f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6070910))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            val t = progress.value
            particles.forEach { (angleDeg, color, speed) ->
                val rad = Math.toRadians(angleDeg.toDouble())
                val dist = t * size.width * 0.55f * speed
                val x = cx + (cos(rad) * dist).toFloat()
                val y = cy + (sin(rad) * dist).toFloat() + (t * t * 240f)
                drawCircle(
                    color = color.copy(alpha = (1f - t * 0.65f).coerceIn(0f, 1f)),
                    radius = 9.dp.toPx() * (1f - t * 0.4f),
                    center = Offset(x, y)
                )
            }
        }

        val extended = LocalLifeHubColors.current
        val cs = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Extruded3DCard(
                faceColor = if (extended.isDark) Color(0xFF141928) else Color.White,
                depthColor = if (extended.isDark) Color(0xFF0A7D56) else Color(0xFF8BE5C4),
                highlightColor = extended.accentMint,
                depthOffset = 9.dp,
                cornerRadius = 30.dp,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
            ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CloseAction(onClick = onDismiss, tint = cs.onSurface)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(extended.accentMint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = "Celebration",
                        tint = extended.accentMint,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "3D VAULT UNLOCKED!",
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.accentMint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You saved ${DateAndFormatUtils.formatCurrency(goal.savedAmount, currencySymbol)} and crushed this milestone!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.mutedText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Extruded3DButton(
                    label = "Claim Victory",
                    topColor = NeonMint,
                    sideColor = Color(0xFF057A52),
                    contentColor = Color(0xFF042015),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalSheet(
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, GoalPreset, Double, Double, String) -> Unit
) {
    val clientDocId = remember { "goal_${UUID.randomUUID().toString().replace("-", "").take(18)}" }
    var selectedPreset by remember { mutableStateOf(GoalPreset.PHONE) }
    var title by remember { mutableStateOf(GoalPreset.PHONE.label) }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }
    var targetMonth by remember { mutableStateOf("2027-06") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Launch 3D Savings Vault", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalPreset.entries, key = { it.key }) { preset ->
                        val selected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) CyberViolet.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (selected) CyberViolet else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedPreset = preset
                                    if (preset != GoalPreset.CUSTOM) title = preset.label
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(goalPresetIcon(preset), contentDescription = preset.label, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(preset.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) targetText = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_target_input")
                )

                AdaptivePairLayout(
                    stackVertically = metrics.shouldStackTightPairs,
                    horizontalSpacing = 10.dp,
                    verticalSpacing = 10.dp,
                    first = { fieldMod ->
                        OutlinedTextField(
                            value = initialText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) initialText = it },
                            label = { Text("Starting Saved", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = fieldMod
                        )
                    },
                    second = { fieldMod ->
                        OutlinedTextField(
                            value = targetMonth,
                            onValueChange = { targetMonth = it.take(20) },
                            label = { Text("Target Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = fieldMod
                        )
                    }
                )

                val targetAmt = targetText.toDoubleOrNull() ?: 0.0
                val initialAmt = initialText.toDoubleOrNull() ?: 0.0
                SavingActionButton(
                    label = "Create Goal",
                    isSaving = isSaving,
                    enabled = title.isNotBlank() && targetAmt > 0.0,
                    onClick = {
                        onSave(clientDocId, title.trim(), selectedPreset, targetAmt, initialAmt, targetMonth.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_goal_button")
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributeGoalSheet(
    goal: SavingsGoalItem,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val presets = listOf(500.0, 1000.0, 2500.0, 5000.0)

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
                text = "Boost ${goal.title}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets, key = { it.toInt() }) { preset ->
                    OutlinedButton(
                        onClick = { amountText = preset.toInt().toString() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "+$currencySymbol${preset.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                label = { Text("Contribution Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val amt = amountText.toDoubleOrNull() ?: 0.0
            SavingActionButton(
                label = "Add Contribution",
                isSaving = isSaving,
                enabled = amt > 0.0,
                onClick = { onConfirm(amt) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
