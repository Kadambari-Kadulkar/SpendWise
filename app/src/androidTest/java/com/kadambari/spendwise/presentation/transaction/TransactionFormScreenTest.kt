package com.kadambari.spendwise.presentation.transaction

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addModeRendersAddTitleAndEmptyFields() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            amountText = "",
            currencyCode = CurrencyCatalogue.GBP.code,
            type = TransactionType.EXPENSE,
            categoryId = null,
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithText("Add transaction").assertIsDisplayed()
        composeRule.onNodeWithText("Amount").assertIsDisplayed()
        composeRule.onNodeWithText("GBP · British Pound").assertIsDisplayed()
        composeRule.onNodeWithText("Expense").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Income").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Select category").assertIsDisplayed()
    }

    @Test
    fun editModeRendersEditTitleAndPopulatedFields() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.EDIT,
            transactionId = "tx-1",
            amountText = "42.80",
            currencyCode = CurrencyCatalogue.GBP.code,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
        )
        setScreen(
            mode = TransactionFormMode.EDIT,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithText("Edit transaction").assertIsDisplayed()
        composeRule.onNodeWithText("42.80").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test
    fun editModeRendersLoadingSpinnerWhenLoading() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.EDIT,
            transactionId = "tx-1",
            isLoading = true,
        )
        setScreen(
            mode = TransactionFormMode.EDIT,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithContentDescription("Loading transaction...").assertIsDisplayed()
    }

    @Test
    fun amountInputDispatchesAmountChangedEvent() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            amountText = "",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
            events = events,
        )

        composeRule.onNodeWithText("Amount").performTextInput("15.50")

        assertTrue(events.any { it is TransactionUiEvent.AmountChanged && it.value == "15.50" })
    }

    @Test
    fun selectingIncomeDispatchesTypeChangedEvent() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            type = TransactionType.EXPENSE,
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
            events = events,
        )

        composeRule.onNodeWithText("Income").performClick()

        assertEquals(listOf(TransactionUiEvent.TypeChanged(TransactionType.INCOME)), events)
    }

    @Test
    fun clickingCategoryOpensBottomSheetAndSelectingCategoryDispatchesEvent() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            type = TransactionType.EXPENSE,
            categoryId = null,
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
            events = events,
        )

        // Click category selector card
        composeRule.onNodeWithText("Select category").performClick()

        // Verify bottom sheet title
        composeRule.onNodeWithText("Select category").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()

        // Select "Food" category
        composeRule.onNodeWithText("Food").performClick()

        assertEquals(listOf(TransactionUiEvent.CategoryChanged("expense_food")), events)
    }

    @Test
    fun categoryBottomSheetExposesSelectionSemantics() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
        )

        // Open category bottom sheet
        composeRule.onNodeWithText("Food").performClick()

        // Assert Food is selected and Shopping is not selected in bottom sheet
        composeRule.onNodeWithText("Food").assertIsSelected()
        composeRule.onNodeWithText("Shopping").assertIsNotSelected()
    }

    @Test
    fun amountAndCategoryErrorsAreDisplayedWhenPresent() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            amountError = "Enter a valid amount.",
            categoryError = "Select a valid category.",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithText("Enter a valid amount.").assertIsDisplayed()
        composeRule.onNodeWithText("Select a valid category.").assertIsDisplayed()
    }

    @Test
    fun interactiveFieldsMeetMinimumTouchTargetHeight() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            categoryId = "expense_food",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithContentDescription("Navigate back").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Expense").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Income").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Category, Food. Tap to change category.")
            .assertHeightIsAtLeast(48.dp)
    }

    private fun setScreen(
        mode: TransactionFormMode,
        uiState: TransactionUiState,
        events: MutableList<TransactionUiEvent> = mutableListOf(),
        onNavigateBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            SpendWiseTheme {
                TransactionFormScreen(
                    mode = mode,
                    uiState = uiState,
                    onEvent = events::add,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}
