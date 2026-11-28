package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.HabitItem
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.CyberAmberLight
import com.example.ui.components.CyberCyan
import com.example.ui.components.CyberCyanLight
import com.example.ui.components.CyberMagenta
import com.example.ui.components.CyberMagentaLight
import com.example.ui.components.CyberYellow
import com.example.ui.components.CyberpunkGridBackdrop
import com.example.ui.components.CyberpunkPanel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.FilterActionButton
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.SavingActionButton
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.viewmodel.LifeHubUiState
import java.util.UUID
import kotlin.math.roundToInt

private enum class CyberHabitFilter(val label: String) {
    ALL("ALL PROTOCOLS"),
    PENDING("PENDING"),
    DONE("LOCKED IN"),
    PINNED("FAVORITES")
}

/**
 * Module 3: Habit Tracker — CYBERPUNK UI ("CYBERFIT // NEON TERMINAL").
 * Chamfered tech corners, neon cyan/magenta/yellow perspective grid backdrop, glowing activity rings,
 * collapsible 7-day heatmap matrix, favorite pinning, and filter/more action controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    state: LifeHubUiState,
    onAddHabit: (String, String, String, String, Int, () -> Unit) -> Unit,
    onToggleHabitToday: (HabitItem, String) -> Unit,
    onDeleteHabit: (String) -> Unit
) {
    val todayKey = remember { DateAndFormatUtils.currentDateKey() }
    val last7Days = remember { DateAndFormatUtils.lastNDaysKeys(7) }
    var showAddSheet by remember { mutableStateOf(false) }
    var heatmapExpanded by remember { mutableStateOf(true) }
    var showFilterDrawer by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(CyberHabitFilter.ALL) }
    var pinnedHabitIds by remember { mutableStateOf(setOf<String>()) }

    val doneToday = state.habits.count { it.isDoneOn(todayKey) }
    val dailyProgress = if (state.habits.isEmpty()) 0f else doneToday.toFloat() / state.habits.size
    val weeklyProgress = if (state.habits.isEmpty()) 0f else {
        state.habits.map { habit ->
            val hits = habit.completedDates.count { it in last7Days }
            (hits.toFloat() / habit.targetDaysPerWeek.coerceAtLeast(1)).coerceAtMost(1f)
        }.average().toFloat()
    }
    val maxCurrentStreak = state.habits.maxOfOrNull { it.currentStreak } ?: 0
    val longestStreak = state.habits.maxOfOrNull { it.longestStreak } ?: 0
    val momentumProgress = (maxCurrentStreak / 7f).coerceIn(0f, 1f)

    val displayedHabits = remember(state.habits, activeFilter, pinnedHabitIds, todayKey) {
        val base = when (activeFilter) {
            CyberHabitFilter.ALL -> state.habits
            CyberHabitFilter.PENDING -> state.habits.filter { !it.isDoneOn(todayKey) }
            CyberHabitFilter.DONE -> state.habits.filter { it.isDoneOn(todayKey) }
            CyberHabitFilter.PINNED -> state.habits.filter { it.id in pinnedHabitIds }
        }
        base.sortedByDescending { it.id in pinnedHabitIds }
    }

    val extended = LocalLifeHubColors.current
    val isDark = extended.isDark
    val cyberBg = if (isDark) Color(0xFF060512) else Color(0xFFF2F6FC)
    val cyberInk = if (isDark) Color.White else Color(0xFF0F172A)
    val cyberMuted = if (isDark) Color(0xFF8C94B5) else Color(0xFF52607A)
    val cyberCyan = if (isDark) CyberCyan else CyberCyanLight
    val cyberMagenta = if (isDark) CyberMagenta else CyberMagentaLight
    val cyberYellow = if (isDark) CyberYellow else CyberAmberLight
    val chipUnselectedBg = if (isDark) Color(0xFF15122D) else Color(0xFFE7EFFA)
    val chipUnselectedInk = if (isDark) Color(0xFFB3BBD4) else Color(0xFF334155)
    val emptyHeatCell = if (isDark) Color(0xFF171430) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cyberBg)
    ) {
        CyberpunkGridBackdrop(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = cyberYellow,
                    contentColor = if (isDark) Color(0xFF08070F) else Color.White,
                    shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
                    modifier = Modifier.testTag("add_habit_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add habit")
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
                        .testTag("habit_list"),
                    contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cyberpunk Terminal Header + Neon Activity Rings
                    item(key = "cyber_hero") {
                        CyberpunkPanel(
                            neonAccent = cyberCyan,
                            secondaryAccent = cyberMagenta,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CYBERFIT // BIO-TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = cyberCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Habit Protocol",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = cyberInk,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                                        color = cyberYellow.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, cyberYellow)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocalFireDepartment,
                                                contentDescription = "Streak flame",
                                                tint = cyberYellow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "${maxCurrentStreak}D",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                color = cyberYellow,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    FilterActionButton(
                                        hasActiveFilter = activeFilter != CyberHabitFilter.ALL,
                                        tint = cyberCyan,
                                        onClick = { showFilterDrawer = !showFilterDrawer }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (metrics.shouldStackTightPairs) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CyberTripleRings(
                                        outerProgress = dailyProgress,
                                        middleProgress = weeklyProgress,
                                        innerProgress = momentumProgress,
                                        outerColor = cyberCyan,
                                        middleColor = cyberMagenta,
                                        innerColor = cyberYellow,
                                        modifier = Modifier.size(116.dp)
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CyberMetricRow("SYNCED TODAY", "$doneToday / ${state.habits.size}", cyberCyan, cyberMuted)
                                        CyberMetricRow("WEEKLY OVERCLOCK", "${(weeklyProgress * 100).roundToInt()}%", cyberMagenta, cyberMuted)
                                        CyberMetricRow("PEAK STREAK", "$longestStreak DAYS", cyberYellow, cyberMuted)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CyberTripleRings(
                                        outerProgress = dailyProgress,
                                        middleProgress = weeklyProgress,
                                        innerProgress = momentumProgress,
                                        outerColor = cyberCyan,
                                        middleColor = cyberMagenta,
                                        innerColor = cyberYellow,
                                        modifier = Modifier.size(if (metrics.isNarrowPhone) 118.dp else 134.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CyberMetricRow("SYNCED TODAY", "$doneToday / ${state.habits.size}", cyberCyan, cyberMuted)
                                        CyberMetricRow("WEEKLY OVERCLOCK", "${(weeklyProgress * 100).roundToInt()}%", cyberMagenta, cyberMuted)
                                        CyberMetricRow("PEAK STREAK", "$longestStreak DAYS", cyberYellow, cyberMuted)
                                    }
                                }
                            }

                        AnimatedVisibility(
                            visible = showFilterDrawer,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(CyberHabitFilter.entries, key = { it.name }) { filter ->
                                    val selected = activeFilter == filter
                                    FilterChip(
                                        selected = selected,
                                        onClick = { activeFilter = filter },
                                        label = {
                                            Text(
                                                filter.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                )
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = cyberCyan.copy(alpha = 0.22f),
                                            selectedLabelColor = cyberCyan,
                                            containerColor = chipUnselectedBg,
                                            labelColor = chipUnselectedInk
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable 7-Day Neon Matrix Heatmap
                item(key = "cyber_heatmap") {
                    CyberpunkPanel(
                        neonAccent = cyberMagenta,
                        secondaryAccent = cyberCyan,
                        onClick = { heatmapExpanded = !heatmapExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "7-DAY NEURAL HEATMAP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = cyberMagenta
                                )
                                Text(
                                    text = "Consistency Grid",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cyberInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ExpandCollapseChevron(expanded = heatmapExpanded, tint = cyberCyan)
                        }

                        AnimatedVisibility(
                            visible = heatmapExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                last7Days.forEach { dayKey ->
                                    val count = state.habits.count { it.isDoneOn(dayKey) }
                                    val ratio = if (state.habits.isEmpty()) 0f else count.toFloat() / state.habits.size
                                    val isToday = dayKey == todayKey
                                    val cellShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = DateAndFormatUtils.dayShortLetter(dayKey),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = if (isToday) cyberYellow else cyberMuted,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = 38.dp)
                                                .aspectRatio(1f)
                                                .clip(cellShape)
                                                .background(
                                                    when {
                                                        ratio >= 0.85f -> cyberCyan
                                                        ratio >= 0.45f -> cyberMagenta.copy(alpha = 0.65f)
                                                        ratio > 0f -> cyberCyan.copy(alpha = 0.25f)
                                                        else -> emptyHeatCell
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isToday) cyberYellow else cyberCyan.copy(alpha = 0.28f),
                                                    cellShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayKey.takeLast(2),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = when {
                                                    ratio >= 0.85f -> if (isDark) Color(0xFF060512) else Color.White
                                                    ratio >= 0.45f -> Color.White
                                                    else -> cyberInk
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cyberpunk Habit Cards
                if (displayedHabits.isEmpty()) {
                    item(key = "empty_habits") {
                        EmptyStateCard(
                            icon = Icons.Rounded.FitnessCenter,
                            title = if (state.habits.isEmpty()) "Initialize your daily streak" else "No habits in this filter",
                            subtitle = if (state.habits.isEmpty()) {
                                "Deploy daily cyber-routines like Hydrate, Read 20m, Workout, or Zero-Spend Day."
                            } else "Switch filter chips above or pin a favorite protocol.",
                            actionLabel = if (state.habits.isEmpty()) "Deploy Habit" else "Show All",
                            onAction = {
                                if (state.habits.isEmpty()) showAddSheet = true
                                else activeFilter = CyberHabitFilter.ALL
                            },
                            accentColor = cyberCyan
                        )
                    }
                } else {
                    items(displayedHabits, key = { it.id }) { habit ->
                        val isPinned = habit.id in pinnedHabitIds
                        CyberHabitCard(
                            habit = habit,
                            todayKey = todayKey,
                            last7Days = last7Days,
                            isPinned = isPinned,
                            isDark = isDark,
                            inkColor = cyberInk,
                            mutedColor = cyberMuted,
                            cyberCyan = cyberCyan,
                            cyberMagenta = cyberMagenta,
                            cyberYellow = cyberYellow,
                            onToggle = { onToggleHabitToday(habit, todayKey) },
                            onTogglePin = {
                                pinnedHabitIds = if (isPinned) pinnedHabitIds - habit.id else pinnedHabitIds + habit.id
                            },
                            onDelete = { onDeleteHabit(habit.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddHabitSheet(
            isSaving = state.isSaving,
            onDismiss = { showAddSheet = false },
            onSave = { docId, title, iconKey, colorHex, target ->
                onAddHabit(docId, title, iconKey, colorHex, target) {
                    showAddSheet = false
                }
            }
        )
    }
}

@Composable
private fun CyberTripleRings(
    outerProgress: Float,
    middleProgress: Float,
    innerProgress: Float,
    outerColor: Color,
    middleColor: Color,
    innerColor: Color,
    modifier: Modifier = Modifier
) {
    val p1 by animateFloatAsState(outerProgress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "cr1")
    val p2 by animateFloatAsState(middleProgress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "cr2")
    val p3 by animateFloatAsState(innerProgress.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "cr3")

    Canvas(modifier = modifier) {
        val stroke = 10.dp.toPx()
        val gap = 6.dp.toPx()

        fun drawNeonRing(inset: Float, progress: Float, color: Color) {
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = color.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Outer glow halo
            drawArc(
                color = color.copy(alpha = 0.28f),
                startAngle = -90f,
                sweepAngle = (360f * progress).coerceAtLeast(if (progress > 0f) 10f else 0f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke + 5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (360f * progress).coerceAtLeast(if (progress > 0f) 10f else 0f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        drawNeonRing(stroke / 2f + 2.dp.toPx(), p1, outerColor)
        drawNeonRing(stroke / 2f + 2.dp.toPx() + stroke + gap, p2, middleColor)
        drawNeonRing(stroke / 2f + 2.dp.toPx() + (stroke + gap) * 2, p3, innerColor)
    }
}

@Composable
private fun CyberMetricRow(label: String, value: String, color: Color, mutedColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(26.dp)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CyberHabitCard(
    habit: HabitItem,
    todayKey: String,
    last7Days: List<String>,
    isPinned: Boolean,
    isDark: Boolean,
    inkColor: Color,
    mutedColor: Color,
    cyberCyan: Color,
    cyberMagenta: Color,
    cyberYellow: Color,
    onToggle: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val isDoneToday = habit.isDoneOn(todayKey)
    val accentColor = remember(habit.colorHex) { parseHexOrDefault(habit.colorHex) }
    val checkBg by animateColorAsState(
        targetValue = if (isDoneToday) accentColor else accentColor.copy(alpha = 0.14f),
        label = "cyber_habit_check"
    )
    val checkShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    val inactiveBarColor = if (isDark) Color(0xFF1F1B3B) else Color(0xFFDDE6F2)

    CyberpunkPanel(
        neonAccent = if (isDoneToday) accentColor else cyberCyan.copy(alpha = 0.7f),
        secondaryAccent = if (isPinned) cyberYellow else cyberMagenta,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // One-tap cyber check-in node (48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(checkShape)
                    .background(checkBg)
                    .border(1.5.dp, accentColor, checkShape)
                    .clickable(onClick = onToggle)
                    .testTag("habit_toggle_${habit.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDoneToday) Icons.Rounded.Check else habitIconForKey(habit.iconKey),
                    contentDescription = if (isDoneToday) "Completed today" else "Complete habit",
                    tint = if (isDoneToday) Color(0xFF060512) else accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = inkColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = cyberYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${habit.currentStreak}D STREAK // ${habit.targetDaysPerWeek}X/WK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FavoriteToggleIcon(
                isFavorite = isPinned,
                activeColor = cyberYellow,
                onToggle = onTogglePin
            )

            MoreOptionsAction(
                tint = cyberCyan,
                options = listOf(
                    (if (isDoneToday) "Undo Today" else "Mark Complete") to onToggle,
                    (if (isPinned) "Unpin Habit" else "Pin to Top") to onTogglePin,
                    "Delete Habit" to onDelete
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Segmented Neon 7-day Telemetry Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            last7Days.forEach { d ->
                val hit = habit.isDoneOn(d)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .background(
                            if (hit) Brush.horizontalGradient(listOf(accentColor, cyberYellow))
                            else Brush.horizontalGradient(listOf(inactiveBarColor, inactiveBarColor))
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit
) {
    val clientDocId = remember { "hab_${UUID.randomUUID().toString().replace("-", "").take(18)}" }
    var title by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("fitness") }
    var selectedColor by remember { mutableStateOf("#00F0FF") }
    var targetDays by remember { mutableFloatStateOf(5f) }

    val iconOptions = listOf(
        "fitness" to "Workout",
        "water" to "Hydrate",
        "book" to "Read",
        "mind" to "Meditate",
        "sun" to "Early Rise"
    )
    val colorOptions = listOf("#00F0FF", "#FF008C", "#FCEE0A", "#00E59B", "#B8FF3B", "#8B5CF6")

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
                text = "Deploy Cyber Habit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Habit Name (e.g., 30 min Workout, No Coffee Spend)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input")
            )

            Text("Protocol Icon", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(iconOptions, key = { it.first }) { (key, label) ->
                    val selected = selectedIcon == key
                    Surface(
                        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                        color = if (selected) CyberCyan.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (selected) CyberCyan else Color.Transparent),
                        modifier = Modifier
                            .clip(CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                            .clickable { selectedIcon = key }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(habitIconForKey(key), contentDescription = label, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Text("Neon Channel", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(colorOptions, key = { it }) { hex ->
                    val color = parseHexOrDefault(hex)
                    val selected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected color",
                                    tint = Color(0xFF060512),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Weekly Target: ${targetDays.roundToInt()} days / week",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = targetDays,
                    onValueChange = { targetDays = it },
                    valueRange = 1f..7f,
                    steps = 5
                )
            }

            SavingActionButton(
                label = "Activate Habit",
                isSaving = isSaving,
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(clientDocId, title.trim(), selectedIcon, selectedColor, targetDays.roundToInt())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_habit_button")
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun habitIconForKey(key: String): ImageVector = when (key) {
    "water" -> Icons.Rounded.WaterDrop
    "book" -> Icons.Rounded.MenuBook
    "mind" -> Icons.Rounded.SelfImprovement
    "sun" -> Icons.Rounded.WbSunny
    else -> Icons.Rounded.FitnessCenter
}

private fun parseHexOrDefault(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    NeonMint
}
