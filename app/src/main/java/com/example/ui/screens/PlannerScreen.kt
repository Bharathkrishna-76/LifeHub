package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.TaskCadence
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.util.DateAndFormatUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandCollapseChevron
import com.example.ui.components.FavoriteToggleIcon
import com.example.ui.components.FilterActionButton
import com.example.ui.components.MoreOptionsAction
import com.example.ui.components.SavingActionButton
import com.example.ui.components.SearchAction
import com.example.ui.components.rememberResponsiveMetrics
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLifeHubColors
import com.example.ui.theme.NeonMint
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.LifeHubUiState
import java.util.UUID

/**
 * Modules 5 & 6: Task Manager & Reminders Hub — DYNAMIC ISLAND CAPSULE + EDITORIAL KANBAN UI.
 * Features a morphing Dynamic Island Filter/Search Capsule, collapsible Kanban lanes,
 * favorite/star task pinning, and contextual MoreOptionsAction menus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    state: LifeHubUiState,
    onAddTask: (String, String, TaskCadence, TaskPriority, Long, Boolean, String, () -> Unit) -> Unit,
    onMoveTaskStatus: (TaskItem, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddReminder: (String, String, ReminderCategory, Double, Long, Boolean, () -> Unit) -> Unit,
    onToggleReminderPaid: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val extended = LocalLifeHubColors.current
    val symbol = state.settings.currencySymbol
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Kanban Tasks, 1 = Bill/EMI Reminders
    var cadenceFilter by remember { mutableStateOf<TaskCadence?>(null) }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var reminderCategoryFilter by remember { mutableStateOf<ReminderCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var islandDrawerOpen by remember { mutableStateOf(false) }
    var starredIds by remember { mutableStateOf(setOf<String>()) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddReminderSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled gracefully by WorkManager worker */ }
    )

    val hasActiveFilter = cadenceFilter != null || priorityFilter != null ||
        reminderCategoryFilter != null || searchQuery.isNotBlank()

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) {
                        showAddTaskSheet = true
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        showAddReminderSheet = true
                    }
                },
                containerColor = cs.onBackground,
                contentColor = cs.background,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("planner_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = if (activeTab == 0) "Add task" else "Add reminder")
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val metrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.horizontalPadding, vertical = 10.dp)
            ) {
                // Morphing Dynamic Island Workspace Filter Capsule
                val capsuleBg = if (extended.isDark) Color(0xFF0A0D16) else Color.White
                val capsuleInk = if (extended.isDark) Color.White else cs.onSurface
                val capsuleMuted = if (extended.isDark) Color(0xFFB5C0D9) else extended.mutedText
                val tabAccent = if (activeTab == 0) extended.accentCyan else extended.accentAmber
                Surface(
                    shape = RoundedCornerShape(if (islandDrawerOpen) 26.dp else 50.dp),
                    color = capsuleBg,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                extended.accentCyan.copy(alpha = 0.55f),
                                extended.accentViolet.copy(alpha = 0.55f),
                                extended.accentMint.copy(alpha = 0.55f)
                            )
                        )
                    ),
                    shadowElevation = if (extended.isDark) 6.dp else 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                        .testTag("planner_dynamic_island")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { islandDrawerOpen = !islandDrawerOpen },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(tabAccent)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (activeTab == 0) "KANBAN FLOW" else "BILL RADAR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = tabAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (activeTab == 0) {
                                            "${state.tasks.count { it.status != TaskStatus.DONE }} active • ${state.tasks.count { it.status == TaskStatus.DONE }} shipped"
                                        } else {
                                            "${state.reminders.count { !it.paid }} upcoming alerts"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = capsuleMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SearchAction(
                                active = islandDrawerOpen && searchQuery.isNotBlank(),
                                tint = capsuleInk,
                                onClick = { islandDrawerOpen = !islandDrawerOpen }
                            )
                            FilterActionButton(
                                hasActiveFilter = hasActiveFilter,
                                tint = capsuleInk,
                                onClick = { islandDrawerOpen = !islandDrawerOpen }
                            )
                            IconButton(onClick = { islandDrawerOpen = !islandDrawerOpen }) {
                                ExpandCollapseChevron(expanded = islandDrawerOpen, tint = capsuleMuted)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = islandDrawerOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        if (activeTab == 0) "Search tasks or notes…" else "Search bills, EMIs, rent…",
                                        color = capsuleMuted
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("planner_search_input")
                            )
                            if (activeTab == 0) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = priorityFilter == null,
                                            onClick = { priorityFilter = null },
                                            label = { Text("Any Priority") }
                                        )
                                    }
                                    items(TaskPriority.entries, key = { it.name }) { p ->
                                        FilterChip(
                                            selected = priorityFilter == p,
                                            onClick = { priorityFilter = if (priorityFilter == p) null else p },
                                            label = { Text(p.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("planner_tab_tasks")
                ) {
                    Text(
                        text = "Tasks (${state.tasks.size})",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("planner_tab_reminders")
                ) {
                    Text(
                        text = "Reminders (${state.reminders.count { !it.paid }})",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = activeTab, label = "planner_tab_switch") { tab ->
                if (tab == 0) {
                    KanbanTasksSection(
                        tasks = state.tasks,
                        cadenceFilter = cadenceFilter,
                        priorityFilter = priorityFilter,
                        searchQuery = searchQuery,
                        starredIds = starredIds,
                        onSelectCadence = { cadenceFilter = it },
                        onToggleStar = { id ->
                            starredIds = if (id in starredIds) starredIds - id else starredIds + id
                        },
                        onMoveStatus = onMoveTaskStatus,
                        onDeleteTask = onDeleteTask,
                        onAddClick = { showAddTaskSheet = true }
                    )
                } else {
                    RemindersSection(
                        reminders = state.reminders,
                        categoryFilter = reminderCategoryFilter,
                        searchQuery = searchQuery,
                        starredIds = starredIds,
                        currencySymbol = symbol,
                        onSelectCategory = { reminderCategoryFilter = it },
                        onToggleStar = { id ->
                            starredIds = if (id in starredIds) starredIds - id else starredIds + id
                        },
                        onTogglePaid = onToggleReminderPaid,
                        onDeleteReminder = onDeleteReminder,
                        onAddClick = { showAddReminderSheet = true }
                    )
                }
            }
            }
        }
    }

    if (showAddTaskSheet) {
        AddTaskSheet(
            isSaving = state.isSaving,
            onDismiss = { showAddTaskSheet = false },
            onSave = { docId, title, cadence, priority, dueMs, recurring, notes ->
                onAddTask(docId, title, cadence, priority, dueMs, recurring, notes) {
                    showAddTaskSheet = false
                }
            }
        )
    }

    if (showAddReminderSheet) {
        AddReminderSheet(
            currencySymbol = symbol,
            isSaving = state.isSaving,
            onDismiss = { showAddReminderSheet = false },
            onSave = { docId, title, cat, amt, dueMs, recurring ->
                onAddReminder(docId, title, cat, amt, dueMs, recurring) {
                    showAddReminderSheet = false
                }
            }
        )
    }
}

@Composable
private fun KanbanTasksSection(
    tasks: List<TaskItem>,
    cadenceFilter: TaskCadence?,
    priorityFilter: TaskPriority?,
    searchQuery: String,
    starredIds: Set<String>,
    onSelectCadence: (TaskCadence?) -> Unit,
    onToggleStar: (String) -> Unit,
    onMoveStatus: (TaskItem, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val filtered = remember(tasks, cadenceFilter, priorityFilter, searchQuery, starredIds) {
        tasks.filter { task ->
            val matchCadence = cadenceFilter == null || task.cadence == cadenceFilter
            val matchPriority = priorityFilter == null || task.priority == priorityFilter
            val matchQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.notes.contains(searchQuery, ignoreCase = true)
            matchCadence && matchPriority && matchQuery
        }.sortedByDescending { it.id in starredIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = cadenceFilter == null,
                onClick = { onSelectCadence(null) },
                label = { Text("All") }
            )
            TaskCadence.entries.forEach { cadence ->
                FilterChip(
                    selected = cadenceFilter == cadence,
                    onClick = { onSelectCadence(if (cadenceFilter == cadence) null else cadence) },
                    label = { Text(cadence.label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.TaskAlt,
                title = "Clear board",
                subtitle = "Organize daily and weekly priorities across To Do, In Progress, and Done.",
                actionLabel = "Add First Task",
                onAction = onAddClick
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val laneWidth = (maxWidth * 0.86f).coerceIn(248.dp, 320.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TaskStatus.entries.forEach { status ->
                        val columnTasks = filtered.filter { it.status == status }
                        KanbanColumn(
                            status = status,
                            tasks = columnTasks,
                            starredIds = starredIds,
                            onToggleStar = onToggleStar,
                            onMoveStatus = onMoveStatus,
                            onDeleteTask = onDeleteTask,
                            modifier = Modifier.width(laneWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    status: TaskStatus,
    tasks: List<TaskItem>,
    starredIds: Set<String>,
    onToggleStar: (String) -> Unit,
    onMoveStatus: (TaskItem, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(true) }
    val headerAccent = when (status) {
        TaskStatus.TODO -> SolarAmber
        TaskStatus.IN_PROGRESS -> ElectricCyan
        TaskStatus.DONE -> NeonMint
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = cs.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, headerAccent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(headerAccent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = headerAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${tasks.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = headerAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    ExpandCollapseChevron(expanded = expanded)
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val isStarred = task.id in starredIds
                        KanbanTaskCard(
                            task = task,
                            isStarred = isStarred,
                            onToggleStar = { onToggleStar(task.id) },
                            onMovePrev = {
                                val prev = when (task.status) {
                                    TaskStatus.DONE -> TaskStatus.IN_PROGRESS
                                    TaskStatus.IN_PROGRESS -> TaskStatus.TODO
                                    TaskStatus.TODO -> null
                                }
                                if (prev != null) onMoveStatus(task, prev)
                            },
                            onMoveNext = {
                                val next = when (task.status) {
                                    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                                    TaskStatus.IN_PROGRESS -> TaskStatus.DONE
                                    TaskStatus.DONE -> null
                                }
                                if (next != null) onMoveStatus(task, next)
                            },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanTaskCard(
    task: TaskItem,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> HotCoral
        TaskPriority.MEDIUM -> SolarAmber
        TaskPriority.LOW -> NeonMint
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        border = BorderStroke(
            width = if (isStarred) 1.5.dp else 1.dp,
            color = if (isStarred) SolarAmber.copy(alpha = 0.65f) else cs.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = task.priority.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cs.surfaceVariant
                    ) {
                        Text(
                            text = task.cadence.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FavoriteToggleIcon(
                        isFavorite = isStarred,
                        activeColor = SolarAmber,
                        onToggle = onToggleStar
                    )
                    MoreOptionsAction(
                        options = buildList {
                            if (task.status != TaskStatus.DONE) add("Advance Stage" to onMoveNext)
                            if (task.status != TaskStatus.TODO) add("Move Back" to onMovePrev)
                            add((if (isStarred) "Unstar Task" else "Star Priority") to onToggleStar)
                            add("Delete Task" to onDelete)
                        }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.recurring) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = "Recurring",
                            tint = CyberViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = DateAndFormatUtils.formatShortDate(task.dueDateEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }

                Row {
                    if (task.status != TaskStatus.TODO) {
                        IconButton(onClick = onMovePrev, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Move stage back",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (task.status != TaskStatus.DONE) {
                        IconButton(onClick = onMoveNext, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Advance task stage",
                                tint = NeonMint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(
    reminders: List<ReminderItem>,
    categoryFilter: ReminderCategory?,
    searchQuery: String,
    starredIds: Set<String>,
    currencySymbol: String,
    onSelectCategory: (ReminderCategory?) -> Unit,
    onToggleStar: (String) -> Unit,
    onTogglePaid: (ReminderItem) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val extended = LocalLifeHubColors.current
    val cs = MaterialTheme.colorScheme
    val filtered = remember(reminders, categoryFilter, searchQuery, starredIds) {
        reminders.filter { rem ->
            val matchCat = categoryFilter == null || rem.category == categoryFilter
            val matchSearch = searchQuery.isBlank() ||
                rem.title.contains(searchQuery, ignoreCase = true) ||
                rem.category.label.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }.sortedByDescending { it.id in starredIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All") }
                )
            }
            items(ReminderCategory.entries, key = { it.name }) { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { onSelectCategory(if (categoryFilter == cat) null else cat) },
                    label = { Text(cat.label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "No upcoming bills or EMIs",
                subtitle = "Schedule Rent, EMI, Bill, or Subscription alerts backed by WorkManager local notifications.",
                actionLabel = "Add Reminder",
                onAction = onAddClick,
                accentColor = extended.accentAmber
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(filtered, key = { it.id }) { rem ->
                    val isStarred = rem.id in starredIds
                    val daysLeft = DateAndFormatUtils.daysUntil(rem.dueDateEpochMs)
                    val statusText = when {
                        rem.paid -> "Paid"
                        daysLeft < 0 -> "Overdue by ${-daysLeft}d"
                        daysLeft == 0 -> "Due today"
                        else -> "Due in ${daysLeft}d"
                    }
                    val statusColor = when {
                        rem.paid -> extended.accentMint
                        daysLeft <= 1 -> HotCoral
                        else -> SolarAmber
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cs.surface,
                        border = BorderStroke(
                            width = if (isStarred) 1.5.dp else 1.dp,
                            color = if (isStarred) HotCoral.copy(alpha = 0.6f) else cs.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            val remMetrics = rememberResponsiveMetrics(maxWidth = maxWidth, maxHeight = maxHeight)
                            val stackMeta = remMetrics.isNarrowPhone || remMetrics.isLargeFont
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = rem.paid,
                                        onCheckedChange = { onTogglePaid(rem) }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rem.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            textDecoration = if (rem.paid) TextDecoration.LineThrough else null,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${rem.category.label} • ${DateAndFormatUtils.formatShortDate(rem.dueDateEpochMs)}" +
                                                if (rem.recurringMonthly) " • Monthly" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!stackMeta) {
                                        Spacer(Modifier.width(6.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = DateAndFormatUtils.formatCurrency(rem.amount, currencySymbol),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    FavoriteToggleIcon(
                                        isFavorite = isStarred,
                                        activeColor = HotCoral,
                                        onToggle = { onToggleStar(rem.id) }
                                    )
                                    MoreOptionsAction(
                                        options = listOf(
                                            (if (rem.paid) "Mark Unpaid" else "Mark Paid") to { onTogglePaid(rem) },
                                            "Delete Reminder" to { onDeleteReminder(rem.id) }
                                        )
                                    )
                                }
                                if (stackMeta) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp, end = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateAndFormatUtils.formatCurrency(rem.amount, currencySymbol),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            maxLines = 1,
                                            softWrap = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, TaskCadence, TaskPriority, Long, Boolean, String) -> Unit
) {
    val clientDocId = remember { "task_${UUID.randomUUID().toString().replace("-", "").take(18)}" }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var cadence by remember { mutableStateOf(TaskCadence.DAILY) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var recurring by remember { mutableStateOf(false) }
    var daysFromNow by remember { mutableIntStateOf(1) }

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
            Text("Create Task", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("Task Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCadence.entries.forEach { item ->
                    FilterChip(
                        selected = cadence == item,
                        onClick = { cadence = item },
                        label = { Text(item.label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { item ->
                    FilterChip(
                        selected = priority == item,
                        onClick = { priority = item },
                        label = { Text("${item.label} Priority") }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Today", 1 to "Tomorrow", 3 to "+3 Days", 7 to "Next Week").forEach { (d, lbl) ->
                    FilterChip(
                        selected = daysFromNow == d,
                        onClick = { daysFromNow = d },
                        label = { Text(lbl) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring Task", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = recurring, onCheckedChange = { recurring = it })
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(300) },
                label = { Text("Notes (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            SavingActionButton(
                label = "Add to Board",
                isSaving = isSaving,
                enabled = title.isNotBlank(),
                onClick = {
                    val dueMs = System.currentTimeMillis() + daysFromNow * 86_400_000L
                    onSave(clientDocId, title.trim(), cadence, priority, dueMs, recurring, notes.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_task_button")
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, ReminderCategory, Double, Long, Boolean) -> Unit
) {
    val clientDocId = remember { "rem_${UUID.randomUUID().toString().replace("-", "").take(18)}" }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReminderCategory.BILL) }
    var recurringMonthly by remember { mutableStateOf(true) }
    var daysFromNow by remember { mutableIntStateOf(3) }

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
            Text("Schedule Reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Reminder Title (e.g., Wi-Fi Bill, Rent, Spotify)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                label = { Text("Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "Tomorrow", 3 to "In 3 Days", 7 to "In 7 Days", 30 to "In 30 Days").forEach { (d, lbl) ->
                    FilterChip(
                        selected = daysFromNow == d,
                        onClick = { daysFromNow = d },
                        label = { Text(lbl) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Repeat Monthly", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = recurringMonthly, onCheckedChange = { recurringMonthly = it })
            }

            SavingActionButton(
                label = "Schedule Notification",
                isSaving = isSaving,
                enabled = title.isNotBlank(),
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val dueMs = System.currentTimeMillis() + daysFromNow * 86_400_000L
                    onSave(clientDocId, title.trim(), category, amt, dueMs, recurringMonthly)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_reminder_button")
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
