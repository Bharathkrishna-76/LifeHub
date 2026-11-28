package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoneyScoreCalculator
import com.example.domain.model.AchievementItem
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.GoalPreset
import com.example.domain.model.HabitItem
import com.example.domain.model.MonthlyTrendPoint
import com.example.domain.model.MonthlyWrappedSummary
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TaskCadence
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.model.ThemeMode
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.domain.model.UserSettings
import com.example.domain.model.YearlyReportSummary
import com.example.domain.util.DateAndFormatUtils
import com.example.tripsplit.domain.DebtSimplifier
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.ExpenseShare
import com.example.tripsplit.domain.SplitMode
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.domain.TripSplitThemePreference
import com.example.tripsplit.domain.WalletEntry
import com.example.tripsplit.domain.WalletEntryType
import com.example.tripsplit.screens.TripSplitHomeScreen
import com.example.tripsplit.screens.TripSplitLedgerScreen
import com.example.tripsplit.screens.TripSplitSquadScreen
import com.example.tripsplit.screens.TripSplitTripsScreen
import com.example.tripsplit.theme.TripSplitTheme
import com.example.tripsplit.viewmodel.TripCardOverview
import com.example.tripsplit.viewmodel.TripSplitUiState
import com.example.ui.components.ResponsiveMetrics
import com.example.ui.components.ScreenWidthTier
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HabitScreen
import com.example.ui.screens.LifeHubSplashScreen
import com.example.ui.screens.MoneyScoreScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.WrappedScreen
import com.example.ui.theme.LifeHubTheme
import com.example.ui.viewmodel.LifeHubUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Multi-configuration responsive UI verification suite running locally on Robolectric JVM.
 * Verifies every LifeHub and TripSplit screen across narrow phones (320dp, 360dp),
 * standard phones (393dp, 432dp), landscape compact-height phones (640x360dp), tablets (840dp),
 * multiple densities (mdpi/xhdpi/xxhdpi), and default + enlarged system font scales (1.0f, 1.35f, 1.4f).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w432dp-h932dp-xxhdpi")
class ResponsiveUiRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private data class ViewportSpec(
        val name: String,
        val widthDp: Dp,
        val heightDp: Dp,
        val density: Float,
        val fontScale: Float
    )

    private val matrixSpecs = listOf(
        ViewportSpec("320dp_small_mdpi_default_font", 320.dp, 568.dp, 1.0f, 1.0f),
        ViewportSpec("320dp_small_mdpi_large_font_1.4x", 320.dp, 568.dp, 1.0f, 1.4f),
        ViewportSpec("360dp_compact_xhdpi_default_font", 360.dp, 640.dp, 2.0f, 1.0f),
        ViewportSpec("360dp_compact_xhdpi_large_font_1.35x", 360.dp, 640.dp, 2.0f, 1.35f),
        ViewportSpec("393dp_medium_xxhdpi_default_font", 393.dp, 851.dp, 3.0f, 1.0f),
        ViewportSpec("432dp_large_xxhdpi_default_font", 432.dp, 932.dp, 3.0f, 1.0f),
        ViewportSpec("640x360_landscape_xhdpi_compact_height", 640.dp, 360.dp, 2.0f, 1.0f),
        ViewportSpec("840x1024_tablet_xhdpi_expanded", 840.dp, 1024.dp, 2.0f, 1.0f)
    )

    private fun sampleLifeHubState(): LifeHubUiState {
        val monthKey = DateAndFormatUtils.currentMonthKey()
        val todayKey = DateAndFormatUtils.currentDateKey()
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionItem(
                id = "tx1",
                userId = "u1",
                title = "Monthly Apartment Rent & Maintenance Charges",
                amount = 24500.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.BILLS,
                monthKey = monthKey,
                timestampEpochMs = now - 3600_000L,
                note = "Auto debit"
            ),
            TransactionItem(
                id = "tx2",
                userId = "u1",
                title = "Organic Grocery & Fresh Produce Supermarket",
                amount = 6420.50,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.FOOD,
                monthKey = monthKey,
                timestampEpochMs = now - 7200_000L
            ),
            TransactionItem(
                id = "tx3",
                userId = "u1",
                title = "Senior Engineering Monthly Salary Deposit",
                amount = 185000.0,
                type = TransactionType.INCOME,
                category = ExpenseCategory.SALARY,
                monthKey = monthKey,
                timestampEpochMs = now - 86400_000L
            )
        )
        val tripTxs = listOf(
            TransactionItem(
                id = "tx_trip_1",
                userId = "u1",
                title = "Coastal Resort Beachfront Villa Booking",
                amount = 18500.0,
                type = TransactionType.EXPENSE,
                category = ExpenseCategory.TRAVEL,
                monthKey = monthKey,
                timestampEpochMs = now - 5000_000L,
                isTripOrEvent = true,
                eventName = "Goa Winter Reunion"
            )
        )
        val habits = listOf(
            HabitItem(
                id = "h1",
                userId = "u1",
                title = "Morning 5km Endurance Run & Stretching",
                iconKey = "fitness",
                colorHex = "#10B981",
                targetDaysPerWeek = 6,
                currentStreak = 14,
                longestStreak = 21,
                completedDates = listOf(todayKey)
            ),
            HabitItem(
                id = "h2",
                userId = "u1",
                title = "Deep Work Architecture Reading 45 min",
                iconKey = "book",
                colorHex = "#8B5CF6",
                targetDaysPerWeek = 5,
                currentStreak = 9,
                longestStreak = 12,
                completedDates = emptyList()
            )
        )
        val goals = listOf(
            SavingsGoalItem(
                id = "g1",
                userId = "u1",
                title = "International Japan Autumn Trip Emergency Reserve",
                category = GoalPreset.VACATION,
                targetAmount = 250000.0,
                savedAmount = 142500.0,
                completed = false,
                targetMonth = "2026-12"
            )
        )
        val tasks = listOf(
            TaskItem(
                id = "t1",
                userId = "u1",
                title = "Review quarterly tax deductions and investment allocation",
                cadence = TaskCadence.WEEKLY,
                priority = TaskPriority.HIGH,
                status = TaskStatus.TODO,
                dueDateEpochMs = now + 86400_000L,
                recurring = true
            ),
            TaskItem(
                id = "t2",
                userId = "u1",
                title = "Pay broadband internet and electricity bill",
                cadence = TaskCadence.DAILY,
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.IN_PROGRESS,
                dueDateEpochMs = now + 43200_000L,
                recurring = false
            )
        )
        val reminders = listOf(
            ReminderItem(
                id = "r1",
                userId = "u1",
                title = "Home Loan Equated Monthly Installment HDFC Bank",
                category = ReminderCategory.EMI,
                amount = 42850.0,
                dueDateEpochMs = now + 172800_000L,
                recurringMonthly = true,
                paid = false
            )
        )
        val achievements = listOf(
            AchievementItem("a1", "Budget Guardian", "Stay under monthly budget for 3 consecutive months", 250, true, "Unlocked"),
            AchievementItem("a2", "Streak Legend", "Maintain a 14-day habit streak without misses", 300, true, "Unlocked"),
            AchievementItem("a3", "Goal Crusher", "Complete 5 major savings targets", 500, false, "2 / 5")
        )
        val score = MoneyScoreCalculator.calculate(txs, 50000.0, habits, goals)
        return LifeHubUiState(
            userId = "u1",
            profile = UserProfile("u1", "Aarav Krishnamurthy", "aarav@example.com", "", 50000.0, "₹", 1840),
            settings = UserSettings(ThemeMode.DARK, true, "₹", 50000.0, true),
            selectedMonthKey = monthKey,
            transactions = txs + tripTxs,
            dailyTransactions = txs,
            tripEventTransactions = tripTxs,
            allTripEventTransactions = tripTxs,
            tripEventTotalExpense = 18500.0,
            tripEventBreakdown = listOf("Goa Winter Reunion" to 18500.0),
            allCachedTransactionsCount = 4,
            yearlyReport = YearlyReportSummary(
                year = 2026,
                totalIncome = 740000.0,
                totalExpense = 210450.0,
                netSavings = 529550.0,
                cachedMonthsCount = 4,
                transactionCount = 48,
                topCategory = ExpenseCategory.BILLS,
                topCategoryAmount = 98000.0,
                categoryTotals = listOf(
                    Triple(ExpenseCategory.BILLS, 98000.0, 0.68f),
                    Triple(ExpenseCategory.FOOD, 46200.0, 0.32f)
                ),
                monthlyBreakdown = listOf(MonthlyTrendPoint(monthKey, "Nov", 185000.0, 30920.5))
            ),
            habits = habits,
            goals = goals,
            tasks = tasks,
            reminders = reminders,
            achievements = achievements,
            moneyScore = score,
            wrappedSummary = MonthlyWrappedSummary(
                monthLabel = DateAndFormatUtils.formatMonthLabel(monthKey),
                totalSpending = 30920.50,
                totalIncome = 185000.0,
                totalSavings = 154079.50,
                topCategory = ExpenseCategory.BILLS,
                topCategoryAmount = 24500.0,
                bestSavingMonthLabel = "November 2026",
                bestSavingAmount = 154079.50,
                longestStreak = 21,
                moneyScore = score.totalScore,
                moneyTier = score.tierLabel,
                unlockedAchievementCount = 2,
                totalXp = 1840
            ),
            totalIncome = 185000.0,
            totalExpense = 30920.50,
            netBalance = 154079.50,
            totalXp = 1840,
            playerLevel = 5
        )
    }

    private fun sampleTripSplitState(): TripSplitUiState {
        val now = System.currentTimeMillis()
        val trip = Trip(
            id = "trip-goa-2026",
            title = "Coastal Amalfi & North Goa Reunion Expedition",
            destination = "Candolim & Assagao Beachfront",
            origin = "Bengaluru International",
            startDateLabel = "12 Dec",
            endDateLabel = "19 Dec",
            totalBudget = 120000.0,
            currencySymbol = "₹",
            cover = DestinationCover.COASTAL,
            waypoints = listOf("Hubli", "Dudhsagar Falls", "Panjim"),
            createdAt = now - 86400_000L
        )
        val members = listOf(
            TripMember("m1", trip.id, "Aarav Krishnamurthy", 0, true),
            TripMember("m2", trip.id, "Siddharth Venkataraman", 1, false),
            TripMember("m3", trip.id, "Priya Chandrasekhar", 2, false)
        )
        val expenses = listOf(
            TripExpense(
                id = "e1",
                tripId = trip.id,
                title = "Beachfront Cliffside Villa Three Nights Stay",
                amount = 48600.0,
                category = TripCategory.STAY,
                paidByMemberId = "m1",
                paidFromWallet = false,
                splitMode = SplitMode.EQUAL,
                shares = DebtSimplifier.buildEqualShares(48600.0, members.map { it.id }),
                dateMillis = now - 7200_000L,
                locationNote = "Anjuna Hilltop"
            ),
            TripExpense(
                id = "e2",
                tripId = trip.id,
                title = "Seafood Sunset Tasting Dinner & Drinks",
                amount = 14250.0,
                category = TripCategory.DINING,
                paidByMemberId = null,
                paidFromWallet = true,
                splitMode = SplitMode.EQUAL,
                shares = DebtSimplifier.buildEqualShares(14250.0, members.map { it.id }),
                dateMillis = now - 3600_000L,
                locationNote = "Vagator"
            )
        )
        val wallets = listOf(
            WalletEntry("w1", trip.id, "m1", 15000.0, WalletEntryType.DEPOSIT, "Initial taxi & meal pool", now - 20000_000L),
            WalletEntry("w2", trip.id, "m2", 15000.0, WalletEntryType.DEPOSIT, "Shared cash contribution", now - 19000_000L)
        )
        val metrics = DebtSimplifier.computeMetrics(trip, members, expenses, wallets, emptyList())
        return TripSplitUiState(
            isLoading = false,
            isRefreshing = false,
            themePreference = TripSplitThemePreference.LIGHT,
            favoriteTripIds = setOf(trip.id),
            allTrips = listOf(TripCardOverview(trip, members.size, metrics.totalSpent, expenses.size)),
            activeTrip = trip,
            members = members,
            expenses = expenses,
            walletEntries = wallets,
            settlements = emptyList(),
            metrics = metrics
        )
    }

    /**
     * Checks that the rendered tree has a valid viewport box and that no visible interactive
     * tagged element overflows horizontally beyond the viewport bounds.
     */
    private fun assertWithinViewport(
        spec: ViewportSpec,
        requiredTags: List<String> = emptyList()
    ) {
        val viewportNode = composeTestRule.onNodeWithTag("test_viewport_root").fetchSemanticsNode()
        val rootBounds = viewportNode.boundsInRoot
        val maxRight = rootBounds.right + 4f
        val minLeft = rootBounds.left - 4f

        assertTrue(
            "[${spec.name}] Viewport width must be positive, got ${rootBounds.width}",
            rootBounds.width > 0f
        )

        // Recursively verify every non-horizontally-scrollable visible node stays inside viewport width
        fun checkSubtree(node: SemanticsNode) {
            if (node.config.contains(SemanticsProperties.HorizontalScrollAxisRange)) {
                // Horizontally scrollable carousels/rows (LazyRow, HorizontalPager, Kanban lanes)
                // intentionally place off-screen items outside horizontal bounds; verify container itself.
                val b = node.boundsInRoot
                if (b.width > 0f && b.height > 0f) {
                    assertTrue(
                        "[${spec.name}] Horizontal scroll container overflows left (${b.left} < $minLeft)",
                        b.left >= minLeft
                    )
                    assertTrue(
                        "[${spec.name}] Horizontal scroll container overflows right (${b.right} > $maxRight)",
                        b.right <= maxRight
                    )
                }
                return
            }
            val b = node.boundsInRoot
            if (b.width > 0f && b.height > 0f && b.bottom > rootBounds.top && b.top < rootBounds.bottom) {
                assertTrue(
                    "[${spec.name}] Node id=${node.id} overflows left (${b.left} < $minLeft)",
                    b.left >= minLeft
                )
                assertTrue(
                    "[${spec.name}] Node id=${node.id} overflows right (${b.right} > $maxRight)",
                    b.right <= maxRight
                )
            }
            node.children.forEach { checkSubtree(it) }
        }
        checkSubtree(viewportNode)

        requiredTags.forEach { tag ->
            val node: SemanticsNode = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            val bounds = node.boundsInRoot
            assertTrue(
                "[${spec.name}] Element '$tag' collapsed horizontally (width=${bounds.width})",
                bounds.width > 0f
            )
            assertTrue(
                "[${spec.name}] Element '$tag' collapsed vertically (height=${bounds.height})",
                bounds.height > 0f
            )
            assertTrue(
                "[${spec.name}] Element '$tag' overflows left boundary (left=${bounds.left} < $minLeft)",
                bounds.left >= minLeft
            )
            assertTrue(
                "[${spec.name}] Element '$tag' overflows right boundary (right=${bounds.right} > $maxRight)",
                bounds.right <= maxRight
            )
        }
    }

    private fun advanceAndSettle() {
        composeTestRule.mainClock.advanceTimeBy(400L)
        composeTestRule.waitForIdle()
    }

    @Test
    fun responsiveMetrics_breakpointsAndStackingRulesAreAccurate() {
        val narrowPhone = ResponsiveMetrics(320.dp, 568.dp, 1.0f)
        assertEquals(ScreenWidthTier.COMPACT, narrowPhone.widthTier)
        assertTrue(narrowPhone.isNarrowPhone)
        assertTrue(narrowPhone.shouldStackTightPairs)
        assertEquals(12.dp, narrowPhone.horizontalPadding)

        val standardPhone = ResponsiveMetrics(393.dp, 851.dp, 1.0f)
        assertEquals(ScreenWidthTier.COMPACT, standardPhone.widthTier)
        assertFalse(standardPhone.isNarrowPhone)
        assertFalse(standardPhone.shouldStackTightPairs)
        assertEquals(16.dp, standardPhone.horizontalPadding)

        val largeFontPhone = ResponsiveMetrics(393.dp, 851.dp, 1.35f)
        assertTrue(largeFontPhone.isLargeFont)
        assertTrue(largeFontPhone.shouldStackTightPairs)

        val landscapePhone = ResponsiveMetrics(640.dp, 360.dp, 1.0f)
        assertEquals(ScreenWidthTier.MEDIUM, landscapePhone.widthTier)
        assertTrue(landscapePhone.isLandscape)
        assertTrue(landscapePhone.isCompactHeight)

        val tablet = ResponsiveMetrics(840.dp, 1024.dp, 1.0f)
        assertEquals(ScreenWidthTier.EXPANDED, tablet.widthTier)
        assertEquals(24.dp, tablet.horizontalPadding)
        assertEquals(840.dp, tablet.contentMaxWidth)
    }

    @Test
    fun splashScreen_rendersCleanlyAcrossAllDeviceConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        var activeSpec by mutableStateOf(matrixSpecs.first())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                LifeHubTheme(themeMode = ThemeMode.DARK) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        LifeHubSplashScreen()
                    }
                }
            }
        }

        matrixSpecs.forEach { spec ->
            activeSpec = spec
            advanceAndSettle()
            assertWithinViewport(spec, listOf("splash_loading_screen"))
        }
    }

    @Test
    fun dashboardScreen_rendersWithinViewportAcrossAllConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        val uiState = sampleLifeHubState()
        var activeSpec by mutableStateOf(matrixSpecs.first())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                LifeHubTheme(themeMode = ThemeMode.DARK) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        DashboardScreen(
                            state = uiState,
                            onQuickAddExpense = {},
                            onQuickAddIncome = {},
                            onQuickAddTrip = {},
                            onNavigateBillsAndEmi = {},
                            onNavigateHabits = {},
                            onNavigateGoals = {},
                            onNavigatePlan = {},
                            onNavigateMoneyScore = {},
                            onNavigateWrapped = {},
                            onNavigateAchievements = {},
                            onNavigateProfile = {},
                            onRefresh = {},
                            onDismissError = {}
                        )
                    }
                }
            }
        }

        matrixSpecs.forEach { spec ->
            activeSpec = spec
            advanceAndSettle()
            assertWithinViewport(
                spec,
                listOf("dashboard_list", "dashboard_balance_card", "dashboard_refresh_button")
            )
        }
    }

    @Test
    fun expenseScreen_rendersWithinViewportAcrossAllConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        val uiState = sampleLifeHubState()
        var activeSpec by mutableStateOf(matrixSpecs.first())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                LifeHubTheme(themeMode = ThemeMode.DARK) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        ExpenseScreen(
                            state = uiState,
                            onShiftMonth = {},
                            onRefresh = {},
                            onLoadOlder = {},
                            onAddTransaction = { _, _, _, _, _, _, _, _, _, _ -> },
                            onUpdateTransaction = { _, _, _, _, _, _, _, _, _ -> },
                            onDeleteTransaction = {},
                            onAddReminder = { _, _, _, _, _, _, _ -> },
                            onToggleReminderPaid = {},
                            onDeleteReminder = {}
                        )
                    }
                }
            }
        }

        matrixSpecs.forEach { spec ->
            activeSpec = spec
            advanceAndSettle()
            assertWithinViewport(spec, listOf("add_transaction_fab"))
        }
    }

    @Test
    fun habitGoalsPlannerScreens_renderWithinViewportAcrossAllConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        val uiState = sampleLifeHubState()
        var activeSpec by mutableStateOf(matrixSpecs.first())
        var activeTab by mutableStateOf(0)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                LifeHubTheme(themeMode = ThemeMode.DARK) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        when (activeTab) {
                            0 -> HabitScreen(
                                state = uiState,
                                onAddHabit = { _, _, _, _, _, _ -> },
                                onToggleHabitToday = { _, _ -> },
                                onDeleteHabit = {}
                            )
                            1 -> GoalsScreen(
                                state = uiState,
                                onAddGoal = { _, _, _, _, _, _, _ -> },
                                onContribute = { _, _, _ -> },
                                onDeleteGoal = {},
                                onDismissCelebration = {}
                            )
                            else -> PlannerScreen(
                                state = uiState,
                                onAddTask = { _, _, _, _, _, _, _, _ -> },
                                onMoveTaskStatus = { _, _ -> },
                                onDeleteTask = {},
                                onAddReminder = { _, _, _, _, _, _, _ -> },
                                onToggleReminderPaid = {},
                                onDeleteReminder = {}
                            )
                        }
                    }
                }
            }
        }

        listOf(
            0 to listOf("add_habit_fab"),
            1 to listOf("add_goal_fab"),
            2 to listOf("planner_add_fab", "planner_tab_tasks", "planner_tab_reminders")
        ).forEach { (tabIndex, tags) ->
            activeTab = tabIndex
            matrixSpecs.forEach { spec ->
                activeSpec = spec
                advanceAndSettle()
                assertWithinViewport(spec, tags)
            }
        }
    }

    @Test
    fun secondaryScreens_moneyScoreWrappedAchievements_renderWithinViewportAcrossAllConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        val uiState = sampleLifeHubState()
        var activeSpec by mutableStateOf(matrixSpecs.first())
        var activeScreen by mutableStateOf(0)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                LifeHubTheme(themeMode = ThemeMode.DARK) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        when (activeScreen) {
                            0 -> MoneyScoreScreen(
                                moneyScore = uiState.moneyScore,
                                onBack = {}
                            )
                            1 -> WrappedScreen(
                                wrapped = uiState.wrappedSummary,
                                currencySymbol = "₹",
                                onClose = {},
                                onShareMessage = {}
                            )
                            else -> AchievementsScreen(
                                achievements = uiState.achievements,
                                totalXp = uiState.totalXp,
                                playerLevel = uiState.playerLevel,
                                onBack = {}
                            )
                        }
                    }
                }
            }
        }

        (0..2).forEach { screenIdx ->
            activeScreen = screenIdx
            matrixSpecs.forEach { spec ->
                activeSpec = spec
                advanceAndSettle()
                assertWithinViewport(spec)
                composeTestRule.onRoot().fetchSemanticsNode()
            }
        }
    }

    @Test
    fun tripSplitScreens_renderWithinViewportAcrossAllConfigurations() {
        composeTestRule.mainClock.autoAdvance = false
        val tsState = sampleTripSplitState()
        var activeSpec by mutableStateOf(matrixSpecs.first())
        var activeScreen by mutableStateOf(0)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(activeSpec.density, activeSpec.fontScale)) {
                TripSplitTheme(themePreference = TripSplitThemePreference.LIGHT) {
                    Box(
                        modifier = Modifier
                            .size(activeSpec.widthDp, activeSpec.heightDp)
                            .testTag("test_viewport_root")
                    ) {
                        when (activeScreen) {
                            0 -> TripSplitHomeScreen(
                                uiState = tsState,
                                travelerFirstName = "Aarav",
                                onCreateTripClick = {},
                                onAddExpenseClick = {},
                                onTopUpWalletClick = {},
                                onInviteMembersClick = {},
                                onOpenTripsTab = {},
                                onOpenExpensesTab = {},
                                onOpenSquadTab = {},
                                onSelectTrip = {},
                                onSettleTransfer = {},
                                onEditExpense = {},
                                onToggleFavorite = {},
                                onShareTripSummary = {},
                                onRefresh = {}
                            )
                            1 -> TripSplitLedgerScreen(
                                uiState = tsState,
                                onAddExpenseClick = {},
                                onEditExpense = {},
                                onDeleteExpense = {},
                                onCreateTripClick = {}
                            )
                            2 -> TripSplitSquadScreen(
                                uiState = tsState,
                                onInviteMembersClick = {},
                                onRemoveTraveler = {},
                                onSettleTransfer = {},
                                onDeleteSettlement = {},
                                onTopUpWalletClick = {},
                                onDeleteWalletEntry = {},
                                onCreateTripClick = {}
                            )
                            else -> TripSplitTripsScreen(
                                uiState = tsState,
                                onSelectTrip = {},
                                onCreateTripClick = {},
                                onDeleteTrip = {},
                                onToggleFavorite = {}
                            )
                        }
                    }
                }
            }
        }

        val screenRequiredTags = listOf(
            0 to listOf("ts_home_create_trip_button", "ts_featured_trip_card", "ts_quick_add_expense", "ts_quick_add_traveler"),
            1 to listOf("ts_ledger_sort_btn", "ts_ledger_add_expense_btn"),
            2 to listOf("ts_squad_add_traveler_btn", "ts_squad_tab_0", "ts_squad_tab_1"),
            3 to listOf("ts_trips_create_btn", "ts_trips_filter_all", "ts_trips_filter_fav")
        )

        screenRequiredTags.forEach { (screenIdx, tags) ->
            activeScreen = screenIdx
            matrixSpecs.forEach { spec ->
                activeSpec = spec
                advanceAndSettle()
                assertWithinViewport(spec, tags)
            }
        }
    }
}
