package com.kadambari.spendwise.presentation.transaction

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.repository.TransactionRepository
import com.kadambari.spendwise.domain.usecase.AddTransactionUseCase
import com.kadambari.spendwise.domain.usecase.DeleteTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionsUseCase
import com.kadambari.spendwise.domain.usecase.UpdateTransactionUseCase
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `initial loading state is announced and displayed`() {
        setScreen(TransactionUiState(isListLoading = true))

        composeRule
            .onNodeWithText("Loading transactions...")
            .assertIsDisplayed()
    }

    @Test
    fun `empty state shows supporting copy and dispatches add`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(TransactionUiState(isListLoading = false), events)

        composeRule.onNodeWithText("No transactions yet.").assertIsDisplayed()
        composeRule
            .onNodeWithText("Add transaction")
            .performClick()

        assertEquals(listOf(TransactionUiEvent.StartAdd), events)
    }

    @Test
    fun `populated list renders income expense amount and optional note`() {
        val expense = expenseItem()
        val income = incomeItem()
        setScreen(
            TransactionUiState(
                transactions = listOf(expense, income),
                filteredTransactions = listOf(expense, income),
                isListLoading = false,
            ),
        )

        composeRule.onNodeWithText("Food", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Lunch", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("-£12.50", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Salary", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("+£500.00", useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("Income · 5 September 2026", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `search field is displayed when transactions exist and typing text dispatches SearchQueryChanged`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
            events,
        )

        composeRule
            .onNodeWithContentDescription("Search transactions by category or note")
            .assertIsDisplayed()
            .performTextInput("Lunch")

        assertTrue(events.any { it is TransactionUiEvent.SearchQueryChanged && it.value == "Lunch" })
    }

    @Test
    fun `search clear button dispatches empty query`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                filter = TransactionFilterUiState(searchQuery = "Coffee"),
                isListLoading = false,
            ),
            events,
        )

        composeRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf(TransactionUiEvent.SearchQueryChanged("")), events)
    }

    @Test
    fun `filter action button is displayed and opens filter bottom sheet`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Filter transactions")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.onNodeWithText("Filter transactions").assertIsDisplayed()
        composeRule.onNodeWithText("Transaction type").assertIsDisplayed()
        composeRule.onNodeWithText("Category").assertIsDisplayed()
        composeRule.onNodeWithText("Date range").assertIsDisplayed()
    }

    @Test
    fun `filter action indicates active filters and active banner allows clear all`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                filter = TransactionFilterUiState(type = TransactionType.EXPENSE),
                isListLoading = false,
            ),
            events,
        )

        composeRule
            .onNodeWithContentDescription("Filter transactions, active")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Filtered results").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").performClick()

        assertEquals(listOf(TransactionUiEvent.ClearFilters), events)
    }

    @Test
    fun `filter sheet type selection dispatches TransactionTypeFilterChanged`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions").performClick()
        composeRule.onNodeWithText("Income").performClick()

        assertTrue(events.contains(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.INCOME)))
    }

    @Test
    fun `filter sheet category selection dispatches CategoryFilterChanged`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions").performClick()
        composeRule
            .onNodeWithContentDescription("Category filter, currently All categories. Tap to change category.")
            .performClick()
        composeRule.onNodeWithText("Food").performClick()

        assertTrue(events.contains(TransactionUiEvent.CategoryFilterChanged("expense_food")))
    }

    @Test
    fun `filter sheet date From selector opens and dispatches DateFromChanged`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                maxSelectableDate = LocalDate.of(2026, 9, 5),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions").performClick()
        composeRule
            .onNodeWithContentDescription("From date, not set. Tap to select date.")
            .performClick()

        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()

        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertTrue(events.any { it is TransactionUiEvent.DateFromChanged && it.value == LocalDate.of(2026, 9, 4) })
    }

    @Test
    fun `filter sheet date To selector opens and dispatches DateToChanged`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                maxSelectableDate = LocalDate.of(2026, 9, 5),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions").performClick()
        composeRule
            .onNodeWithContentDescription("To date, not set. Tap to select date.")
            .performClick()

        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()

        composeRule.onNodeWithText("3").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertTrue(events.any { it is TransactionUiEvent.DateToChanged && it.value == LocalDate.of(2026, 9, 3) })
    }

    @Test
    fun `filter sheet clear date buttons have minimum 48dp touch target and dispatch null dates`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                filter = TransactionFilterUiState(
                    dateFrom = LocalDate.of(2026, 9, 1),
                    dateTo = LocalDate.of(2026, 9, 5),
                ),
                maxSelectableDate = LocalDate.of(2026, 9, 5),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions, active").performClick()

        composeRule
            .onNodeWithContentDescription("Clear From date")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        assertTrue(events.contains(TransactionUiEvent.DateFromChanged(null)))

        composeRule
            .onNodeWithContentDescription("Clear To date")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        assertTrue(events.contains(TransactionUiEvent.DateToChanged(null)))
    }

    @Test
    fun `filter sheet clear all filters button dispatches ClearFilters`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                filter = TransactionFilterUiState(type = TransactionType.EXPENSE),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithContentDescription("Filter transactions, active").performClick()
        composeRule.onNodeWithText("Clear all filters").performClick()

        assertTrue(events.contains(TransactionUiEvent.ClearFilters))
    }

    @Test
    fun `filtered empty state is shown when transactions exist but filtered list is empty`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = emptyList(),
                filter = TransactionFilterUiState(searchQuery = "Nonexistent"),
                isListLoading = false,
            ),
            events,
        )

        composeRule.onNodeWithText("No transactions match your filters.").assertIsDisplayed()
        composeRule.onNodeWithText("Try adjusting your search or clearing filters to see your transactions.").assertIsDisplayed()
        composeRule.onNodeWithText("Clear filters").performClick()

        assertEquals(listOf(TransactionUiEvent.ClearFilters), events)
    }

    @Test
    fun `edit action exposes distinct semantics and dispatches edit`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
            events,
        )

        composeRule
            .onNodeWithContentDescription("Edit Food", substring = true)
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            )
            .assertContentDescriptionEquals(
                "Edit Food, expense, -£12.50, dated 5 September 2026, note: Lunch",
            )
            .performClick()

        assertEquals(
            listOf(TransactionUiEvent.StartEdit("expense-1")),
            events,
        )
    }

    @Test
    fun `delete action exposes distinct semantics with minimum touch target and dispatches delete requested`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
            ),
            events,
        )

        composeRule
            .onNodeWithContentDescription("Delete Food transaction, -£12.50")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            )
            .performClick()

        assertEquals(
            listOf(TransactionUiEvent.DeleteRequested("expense-1")),
            events,
        )
    }

    @Test
    fun `confirmation dialog is displayed when pending delete is set`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                pendingDeleteId = "expense-1",
            ),
        )

        composeRule.onNodeWithText("Delete transaction?").assertIsDisplayed()
        composeRule
            .onNodeWithText("Are you sure you want to delete this transaction? This action cannot be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Delete").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun `confirmation dialog cancel button dispatches delete cancelled`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                pendingDeleteId = "expense-1",
            ),
            events,
        )

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf(TransactionUiEvent.DeleteCancelled), events)
    }

    @Test
    fun `confirmation dialog delete button dispatches delete confirmed`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                pendingDeleteId = "expense-1",
            ),
            events,
        )

        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(listOf(TransactionUiEvent.DeleteConfirmed), events)
    }

    @Test
    fun `delete button is disabled when transaction is actively deleting`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                deletingTransactionId = "expense-1",
            ),
        )

        composeRule
            .onNodeWithContentDescription("Delete Food transaction, -£12.50")
            .assertIsNotEnabled()
    }

    @Test
    fun `add action exposes content description and dispatches start add`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(TransactionUiState(isListLoading = false), events)

        composeRule
            .onNodeWithContentDescription("Add transaction")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(listOf(TransactionUiEvent.StartAdd), events)
    }

    @Test
    fun `list error shows stable copy and retry dispatches retry`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                isListLoading = false,
                listError = TransactionUiError(
                    message = "RoomException: internal database detail",
                    kind = TransactionUiErrorKind.OPERATION,
                ),
            ),
            events,
        )

        composeRule
            .onNodeWithText("We couldn't load your transactions.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(listOf(TransactionUiEvent.RetryList), events)
    }

    @Test
    fun `existing transactions remain visible when list error is present`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                listError = TransactionUiError(
                    message = "technical detail",
                    kind = TransactionUiErrorKind.OPERATION,
                ),
            ),
        )

        composeRule.onNodeWithText("Food", useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("We couldn't load your transactions.")
            .assertIsDisplayed()
    }

    @Test
    fun `delete error displays stable snackbar message and clears error`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                deleteError = TransactionUiError(
                    message = "Unable to delete the transaction.",
                    kind = TransactionUiErrorKind.OPERATION,
                ),
            ),
            events,
        )

        composeRule
            .onNodeWithText("Unable to delete the transaction.")
            .assertIsDisplayed()
        assertEquals(listOf(TransactionUiEvent.ClearError), events)
    }

    @Test
    fun `delete error does not display raw technical exception`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                deleteError = TransactionUiError(
                    message = "Unable to delete the transaction.",
                    kind = TransactionUiErrorKind.OPERATION,
                ),
            ),
        )

        composeRule.onNodeWithText("SQLiteException", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("RoomDatabase", substring = true).assertDoesNotExist()
    }

    @Test
    fun `existing transactions remain visible when delete error is present`() {
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
                filteredTransactions = listOf(expenseItem()),
                isListLoading = false,
                deleteError = TransactionUiError(
                    message = "Unable to delete the transaction.",
                    kind = TransactionUiErrorKind.OPERATION,
                ),
            ),
        )

        composeRule.onNodeWithText("Food", useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("Unable to delete the transaction.")
            .assertIsDisplayed()
    }

    @Test
    fun `deleted effect handled by route displays transaction deleted snackbar`() {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        composeRule.setContent {
            SpendWiseTheme {
                TransactionListRoute(
                    viewModel = viewModel,
                    onNavigateToAdd = {},
                    onNavigateToEdit = {},
                )
            }
        }

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("expense-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)

        composeRule.onNodeWithText("Transaction deleted").assertIsDisplayed()
    }

    private fun setScreen(
        state: TransactionUiState,
        events: MutableList<TransactionUiEvent> = mutableListOf(),
        snackbarHostState: SnackbarHostState = SnackbarHostState(),
    ) {
        composeRule.setContent {
            SpendWiseTheme {
                TransactionListScreen(
                    uiState = state,
                    onEvent = events::add,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    private fun createViewModel(
        repository: TransactionRepository = FakeTransactionRepository(),
        clock: Clock = Clock.systemDefaultZone(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): TransactionViewModel = TransactionViewModel(
        getTransactionsUseCase = GetTransactionsUseCase(repository),
        getTransactionUseCase = GetTransactionUseCase(repository),
        addTransactionUseCase = AddTransactionUseCase(repository, clock, zoneId),
        updateTransactionUseCase = UpdateTransactionUseCase(repository, clock, zoneId),
        deleteTransactionUseCase = DeleteTransactionUseCase(repository),
        clock = clock,
        zoneId = zoneId,
    )

    private fun expenseItem(): TransactionListItemUiModel =
        TransactionListItemUiModel(
            id = "expense-1",
            amountMinorUnits = 1250L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
            categoryId = CategoryId.of("expense_food").value,
            categoryLabel = "Food",
            date = LocalDate.of(2026, 9, 5),
            note = "Lunch",
        )

    private fun incomeItem(): TransactionListItemUiModel =
        TransactionListItemUiModel(
            id = "income-1",
            amountMinorUnits = 50000L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.INCOME,
            categoryId = CategoryId.of("income_salary").value,
            categoryLabel = "Salary",
            date = LocalDate.of(2026, 9, 5),
            note = null,
        )

    private class FakeTransactionRepository(
        private val transactionsFlow: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList()),
    ) : TransactionRepository {
        var deleteResult: Boolean = true

        override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow
        override suspend fun getTransaction(id: String): Transaction? = null
        override suspend fun insertTransaction(transaction: Transaction) {}
        override suspend fun updateTransaction(transaction: Transaction): Boolean = true
        override suspend fun deleteTransaction(id: String): Boolean = deleteResult
    }
}
