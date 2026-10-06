package com.kadambari.spendwise.presentation.transaction

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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
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
    fun `edit action exposes distinct semantics and dispatches edit`() {
        val events = mutableListOf<TransactionUiEvent>()
        setScreen(
            TransactionUiState(
                transactions = listOf(expenseItem()),
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

    private fun setScreen(
        state: TransactionUiState,
        events: MutableList<TransactionUiEvent> = mutableListOf(),
    ) {
        composeRule.setContent {
            SpendWiseTheme {
                TransactionListScreen(
                    uiState = state,
                    onEvent = events::add,
                )
            }
        }
    }

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
}
