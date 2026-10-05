package com.kadambari.spendwise.presentation.transaction

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import java.time.LocalDate
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
            date = LocalDate.of(2026, 10, 5),
            noteText = "",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(
                form = formState,
                maxSelectableDate = LocalDate.of(2026, 10, 5),
            ),
        )

        composeRule.onNodeWithText("Add transaction").assertIsDisplayed()
        composeRule.onNodeWithText("Amount").assertIsDisplayed()
        composeRule.onNodeWithText("GBP · British Pound").assertIsDisplayed()
        composeRule.onNodeWithText("Expense").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Income").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Select category").assertIsDisplayed()
        composeRule.onNodeWithText("5 October 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Note (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("0/500").assertIsDisplayed()
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
            date = LocalDate.of(2026, 9, 20),
            noteText = "Dinner with friends",
        )
        setScreen(
            mode = TransactionFormMode.EDIT,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithText("Edit transaction").assertIsDisplayed()
        composeRule.onNodeWithText("42.80").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("20 September 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Dinner with friends").assertIsDisplayed()
        composeRule.onNodeWithText("19/500").assertIsDisplayed()
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
    fun dateFieldRendersAndClickOpensDatePicker() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            date = LocalDate.of(2026, 10, 5),
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(
                form = formState,
                maxSelectableDate = LocalDate.of(2026, 10, 5),
            ),
        )

        composeRule.onNodeWithText("5 October 2026").assertIsDisplayed()
        composeRule.onNodeWithText("5 October 2026").performClick()

        // Dialog confirm/dismiss buttons are displayed
        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun selectingValidDateFromDatePickerDispatchesDateChanged() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            date = LocalDate.of(2026, 10, 5),
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(
                form = formState,
                maxSelectableDate = LocalDate.of(2026, 10, 5),
            ),
            events = events,
        )

        // Open DatePicker dialog
        composeRule.onNodeWithText("5 October 2026").performClick()

        // Select day 4 in current month
        composeRule.onNodeWithText("4").performClick()

        // Confirm selection
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(
            listOf(TransactionUiEvent.DateChanged(LocalDate.of(2026, 10, 4))),
            events,
        )
    }

    @Test
    fun datePickerRespectsMaxSelectableDate() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            date = LocalDate.of(2026, 10, 5),
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(
                form = formState,
                maxSelectableDate = LocalDate.of(2026, 10, 5),
            ),
            events = events,
        )

        // Open DatePicker dialog
        composeRule.onNodeWithText("5 October 2026").performClick()

        // Day 6 (future date beyond maxSelectableDate) is not enabled / unselectable
        composeRule.onNodeWithText("6").assertIsNotEnabled()

        // Attempting to click day 6 and confirming does not change date to day 6
        composeRule.onNodeWithText("6").performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertTrue(events.none { it is TransactionUiEvent.DateChanged && it.value == LocalDate.of(2026, 10, 6) })
    }

    @Test
    fun noteFieldRendersAndInputDispatchesNoteChanged() {
        val events = mutableListOf<TransactionUiEvent>()
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            noteText = "",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
            events = events,
        )

        composeRule.onNodeWithText("Note (optional)").performTextInput("Coffee")

        assertTrue(events.any { it is TransactionUiEvent.NoteChanged && it.value == "Coffee" })
    }

    @Test
    fun noteFieldEnforces500CharacterLimit() {
        val events = mutableListOf<TransactionUiEvent>()
        val existing500Chars = "a".repeat(500)
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            noteText = existing500Chars,
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
            events = events,
        )

        composeRule.onNodeWithText("500/500").assertIsDisplayed()
        composeRule.onNodeWithText("Note (optional)").performTextInput("extra")

        // No new NoteChanged event is dispatched beyond 500 characters
        assertTrue(events.none { it is TransactionUiEvent.NoteChanged && it.value.length > 500 })
    }

    @Test
    fun allFieldErrorsAreDisplayedWhenPresent() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            amountError = "Enter a valid amount.",
            categoryError = "Select a valid category.",
            dateError = "Choose today or an earlier date.",
            noteError = "Note must be 500 characters or fewer.",
        )
        setScreen(
            mode = TransactionFormMode.ADD,
            uiState = TransactionUiState(form = formState),
        )

        composeRule.onNodeWithText("Enter a valid amount.").assertIsDisplayed()
        composeRule.onNodeWithText("Select a valid category.").assertIsDisplayed()
        composeRule.onNodeWithText("Choose today or an earlier date.").assertIsDisplayed()
        composeRule.onNodeWithText("Note must be 500 characters or fewer.").assertIsDisplayed()
    }

    @Test
    fun interactiveFieldsMeetMinimumTouchTargetHeight() {
        val formState = TransactionFormUiState(
            mode = TransactionFormMode.ADD,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
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
        composeRule.onNodeWithContentDescription("Transaction date, 5 October 2026. Tap to change date.")
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
