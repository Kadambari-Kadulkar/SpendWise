package com.kadambari.spendwise.presentation.dashboard

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadingStateIsDisplayed() {
        setScreen(DashboardUiState(selectedMonth = YearMonth.of(2026, 10), isLoading = true))

        composeRule
            .onNodeWithContentDescription("Loading dashboard...")
            .assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetryDispatchesEventWithAdequateTouchTarget() {
        val events = mutableListOf<DashboardUiEvent>()
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                isLoading = false,
                error = DashboardUiError("Network error"),
            ),
            onEvent = { events.add(it) },
        )

        composeRule.onNodeWithText("We couldn't load your dashboard.").assertIsDisplayed()
        composeRule.onNodeWithText("Network error").assertIsDisplayed()
        composeRule
            .onNodeWithText("Retry")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf(DashboardUiEvent.Retry), events)
    }

    @Test
    fun noTransactionsEmptyStateRendersAndAddTransactionHasAdequateTouchTarget() {
        var addClicked = false
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                isLoading = false,
                hasNoTransactions = true,
                hasNoMonthlyTransactions = true,
            ),
            onNavigateToAdd = { addClicked = true },
        )

        composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
        composeRule
            .onNodeWithText("Add your first income or expense to start tracking your money.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Add transaction")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(addClicked)
    }

    @Test
    fun noTransactionsInSelectedMonthShowsMonthSpecificEmptyStateWithAdequateTouchTarget() {
        var addClicked = false
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 50000L,
                monthlyIncomeMinorUnits = 0L,
                monthlyExpensesMinorUnits = 0L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = true,
            ),
            onNavigateToAdd = { addClicked = true },
        )

        composeRule.onNodeWithText("Your Balance").assertIsDisplayed()
        composeRule.onNodeWithText("£500.00").assertIsDisplayed()
        composeRule.onNodeWithText("No transactions for October 2026").assertIsDisplayed()
        composeRule
            .onNodeWithText("Transactions added for this month will appear here.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Add transaction")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(addClicked)
    }

    @Test
    fun dashboardContentRendersFinancialValuesGreetingAndTotals() {
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 248050L,
                monthlyIncomeMinorUnits = 320000L,
                monthlyExpensesMinorUnits = 71950L,
                currencyCode = CurrencyCode.GBP,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
                recentTransactions = listOf(
                    DashboardRecentTransactionUiModel(
                        id = "tx1",
                        amountMinorUnits = 4280L,
                        currencyCode = CurrencyCode.GBP,
                        type = TransactionType.EXPENSE,
                        categoryId = "expense_food",
                        categoryLabel = "Groceries",
                        date = LocalDate.of(2026, 10, 5),
                        note = "Weekly shop",
                    ),
                    DashboardRecentTransactionUiModel(
                        id = "tx2",
                        amountMinorUnits = 320000L,
                        currencyCode = CurrencyCode.GBP,
                        type = TransactionType.INCOME,
                        categoryId = "income_salary",
                        categoryLabel = "Salary",
                        date = LocalDate.of(2026, 10, 1),
                        note = null,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Good morning").assertIsDisplayed()
        composeRule.onNodeWithText("Your Balance").assertIsDisplayed()
        composeRule.onNodeWithText("£2,480.50").assertIsDisplayed()
        composeRule.onNodeWithText("Income").assertIsDisplayed()
        composeRule.onNodeWithText("+£3,200.00").assertIsDisplayed()
        composeRule.onNodeWithText("Expenses").assertIsDisplayed()
        composeRule.onNodeWithText("-£719.50").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selected month, October 2026.").assertIsDisplayed()
    }

    @Test
    fun monthSelectorHasMergedSemanticsButtonRoleAndAdequateTouchTarget() {
        val events = mutableListOf<DashboardUiEvent>()
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 10000L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
            ),
            onEvent = { events.add(it) },
        )

        // Click month selector with merged semantics and button role
        composeRule
            .onNodeWithContentDescription("Selected month, October 2026.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        // Month picker dialog opens showing months
        composeRule.onNodeWithText("2026").assertIsDisplayed()
    }

    @Test
    fun monthPickerNavigatesYearsAndSelectsMonthWithAccessibleSemantics() {
        val events = mutableListOf<DashboardUiEvent>()
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 10000L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
            ),
            onEvent = { events.add(it) },
        )

        // Open dialog
        composeRule.onNodeWithContentDescription("Selected month, October 2026.").performClick()

        // Verify previous year button has accessible description and adequate touch target
        composeRule
            .onNodeWithContentDescription("Previous year")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithText("2025").assertIsDisplayed()

        // Verify next year button has accessible description and adequate touch target
        composeRule
            .onNodeWithContentDescription("Next year")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithText("2026").assertIsDisplayed()

        // Selected month has accessibility semantics indicating selected state
        composeRule.onNodeWithContentDescription("October, selected").assertIsDisplayed()

        // Select September
        composeRule
            .onNodeWithContentDescription("September")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 9))), events)
    }

    @Test
    fun monthPickerDismissesOnCancel() {
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 10000L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Selected month, October 2026.").performClick()
        composeRule.onNodeWithText("2026").assertIsDisplayed()

        composeRule
            .onNodeWithText("Cancel")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithText("2026").assertDoesNotExist()
    }

    @Test
    fun seeAllButtonHasAdequateTouchTargetAndNavigates() {
        var seeAllClicked = false
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 10000L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
            ),
            onNavigateToTransactions = { seeAllClicked = true },
        )

        composeRule
            .onNodeWithText("See all")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertTrue(seeAllClicked)
    }

    @Test
    fun topBarAddButtonHasAdequateTouchTargetAndNavigates() {
        var addClicked = false
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                balanceMinorUnits = 10000L,
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
            ),
            onNavigateToAdd = { addClicked = true },
        )

        composeRule
            .onNodeWithContentDescription("Add transaction")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        assertTrue(addClicked)
    }

    @Test
    fun recentTransactionRowHasMergedSemanticsAndButtonRoleWithNote() {
        var clickedId: String? = null
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
                recentTransactions = listOf(
                    DashboardRecentTransactionUiModel(
                        id = "tx1",
                        amountMinorUnits = 4280L,
                        currencyCode = CurrencyCode.GBP,
                        type = TransactionType.EXPENSE,
                        categoryId = "expense_food",
                        categoryLabel = "Groceries",
                        date = LocalDate.of(2026, 10, 5),
                        note = "Weekly shop",
                    ),
                ),
            ),
            onNavigateToEdit = { clickedId = it },
        )

        val expectedDescription =
            "Edit transaction. Groceries, expense, minus £42.80, dated 5 October 2026, note: Weekly shop."

        composeRule
            .onNodeWithContentDescription(expectedDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals("tx1", clickedId)
    }

    @Test
    fun recentTransactionRowHasMergedSemanticsAndButtonRoleWithoutNote() {
        var clickedId: String? = null
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                isLoading = false,
                hasNoTransactions = false,
                hasNoMonthlyTransactions = false,
                recentTransactions = listOf(
                    DashboardRecentTransactionUiModel(
                        id = "tx2",
                        amountMinorUnits = 320000L,
                        currencyCode = CurrencyCode.GBP,
                        type = TransactionType.INCOME,
                        categoryId = "income_salary",
                        categoryLabel = "Salary",
                        date = LocalDate.of(2026, 10, 1),
                        note = null,
                    ),
                ),
            ),
            onNavigateToEdit = { clickedId = it },
        )

        val expectedDescription =
            "Edit transaction. Salary, income, plus £3,200.00, dated 1 October 2026."

        composeRule
            .onNodeWithContentDescription(expectedDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals("tx2", clickedId)
    }

    @Test
    fun dashboardRendersDynamicGreetingFromUiState() {
        setScreen(
            state = DashboardUiState(
                selectedMonth = YearMonth.of(2026, 10),
                greeting = "Welcome back",
                isLoading = false,
                hasNoTransactions = true,
                hasNoMonthlyTransactions = true,
            ),
        )

        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    private fun setScreen(
        state: DashboardUiState,
        onEvent: (DashboardUiEvent) -> Unit = {},
        onNavigateToAdd: () -> Unit = {},
        onNavigateToTransactions: () -> Unit = {},
        onNavigateToEdit: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            SpendWiseTheme {
                DashboardScreen(
                    uiState = state,
                    onEvent = onEvent,
                    onNavigateToAdd = onNavigateToAdd,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToEdit = onNavigateToEdit,
                )
            }
        }
    }
}
