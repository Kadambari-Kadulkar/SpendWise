package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCatalogue
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestRule = MainDispatcherRule()

    @Test
    fun `initial state starts list loading`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedFlows.add(emptyFlow())
        }
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isListLoading)
    }

    @Test
    fun `empty list completes loading with empty state`() = runTest {
        val viewModel = createViewModel(FakeTransactionRepository())

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isListLoading)
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertNull(viewModel.uiState.value.listError)
    }

    @Test
    fun `populated list is mapped into presentation models`() = runTest {
        val transaction = expenseTransaction()
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(transaction)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val item = viewModel.uiState.value.transactions.single()
        assertEquals(transaction.id, item.id)
        assertEquals(transaction.amount.minorUnits, item.amountMinorUnits)
        assertEquals(transaction.amount.currencyCode, item.currencyCode)
        assertEquals(transaction.type, item.type)
        assertEquals(transaction.categoryId.value, item.categoryId)
        assertEquals("Food", item.categoryLabel)
        assertEquals(transaction.transactionDate, item.date)
        assertEquals(transaction.note, item.note)
    }

    @Test
    fun `list error is exposed and retry restarts observation`() = runTest {
        val transaction = expenseTransaction()
        val repository = FakeTransactionRepository().apply {
            observedFlows.add(flow<List<Transaction>> { throw IllegalStateException("database") })
            observedFlows.add(observedTransactions)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isListLoading)
        assertEquals(TransactionUiErrorKind.OPERATION, viewModel.uiState.value.listError?.kind)
        assertEquals(
            "Couldn't load your transactions. Please try again.",
            viewModel.uiState.value.listError?.message,
        )

        repository.observedTransactions.value = listOf(transaction)
        viewModel.onEvent(TransactionUiEvent.RetryList)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.listError)
        assertEquals(listOf(transaction.id), viewModel.uiState.value.transactions.map { it.id })
        assertEquals(2, repository.observeCallCount)
    }

    @Test
    fun `start add initializes expense default currency and deterministic today`() = runTest {
        val clock = Clock.fixed(ADD_INSTANT, LONDON_ZONE)
        val viewModel = createViewModel(FakeTransactionRepository(), clock)

        viewModel.onEvent(TransactionUiEvent.StartAdd)

        val form = requireNotNull(viewModel.uiState.value.form)
        assertEquals(TransactionFormMode.ADD, form.mode)
        assertNull(form.transactionId)
        assertEquals(TransactionType.EXPENSE, form.type)
        assertEquals(CurrencyCatalogue.GBP.code, form.currencyCode)
        assertEquals(ADD_TODAY, form.date)
        assertEquals(ADD_TODAY, viewModel.uiState.value.maxSelectableDate)
        assertNull(form.noteError)
    }

    @Test
    fun `max selectable date uses the injected clock and zone`() = runTest {
        val losAngelesZone = ZoneId.of("America/Los_Angeles")
        val clock = Clock.fixed(
            Instant.parse("2026-09-05T00:30:00Z"),
            losAngelesZone,
        )
        val viewModel = createViewModel(
            repository = FakeTransactionRepository(),
            clock = clock,
            zoneId = losAngelesZone,
        )

        assertEquals(
            LocalDate.of(2026, 9, 4),
            viewModel.uiState.value.maxSelectableDate,
        )
    }

    @Test
    fun `field changes update shared form state and type change clears incompatible category`() = runTest {
        val viewModel = createViewModel(FakeTransactionRepository())

        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.TypeChanged(TransactionType.INCOME))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("income_salary"))
        viewModel.onEvent(TransactionUiEvent.AmountChanged("12.50"))
        viewModel.onEvent(TransactionUiEvent.DateChanged(LocalDate.of(2026, 9, 4)))
        viewModel.onEvent(TransactionUiEvent.NoteChanged("Lunch"))

        val form = requireNotNull(viewModel.uiState.value.form)
        assertEquals("12.50", form.amountText)
        assertEquals(TransactionType.INCOME, form.type)
        assertEquals("income_salary", form.categoryId)
        assertEquals(LocalDate.of(2026, 9, 4), form.date)
        assertEquals("Lunch", form.noteText)
        assertNull(form.amountError)
        assertNull(form.categoryError)
        assertNull(form.dateError)
        assertNull(form.noteError)
    }

    @Test
    fun `start edit loads transaction into shared form state`() = runTest {
        val transaction = expenseTransaction()
        val repository = FakeTransactionRepository().apply {
            transactions[transaction.id] = transaction
        }
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.StartEdit(transaction.id))
        assertTrue(requireNotNull(viewModel.uiState.value.form).isLoading)

        advanceUntilIdle()

        val form = requireNotNull(viewModel.uiState.value.form)
        assertFalse(form.isLoading)
        assertEquals(TransactionFormMode.EDIT, form.mode)
        assertEquals(transaction.id, form.transactionId)
        assertEquals("10.25", form.amountText)
        assertEquals(transaction.amount.currencyCode, form.currencyCode)
        assertEquals(transaction.type, form.type)
        assertEquals(transaction.categoryId.value, form.categoryId)
        assertEquals(transaction.transactionDate, form.date)
        assertEquals(transaction.note, form.noteText)
    }

    @Test
    fun `start edit exposes not found without discarding edit identity`() = runTest {
        val viewModel = createViewModel(FakeTransactionRepository())

        viewModel.onEvent(TransactionUiEvent.StartEdit("missing"))
        advanceUntilIdle()

        val form = requireNotNull(viewModel.uiState.value.form)
        assertFalse(form.isLoading)
        assertEquals("missing", form.transactionId)
        assertEquals(TransactionUiErrorKind.NOT_FOUND, viewModel.uiState.value.formError?.kind)
    }

    @Test
    fun `valid add persists draft and emits a non sticky saved effect`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository, Clock.fixed(ADD_INSTANT, LONDON_ZONE))
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("12.50"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.NoteChanged("Lunch"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        val inserted = requireNotNull(repository.insertedTransaction)
        assertEquals(1250L, inserted.amount.minorUnits)
        assertEquals(CurrencyCode.GBP, inserted.amount.currencyCode)
        assertEquals(TransactionType.EXPENSE, inserted.type)
        assertEquals(CategoryId.of("expense_food"), inserted.categoryId)
        assertEquals(ADD_TODAY, inserted.transactionDate)
        assertEquals("Lunch", inserted.note)
        assertEquals(TransactionUiEffect.Saved(inserted.id), effect.await())
        assertNull(viewModel.uiState.value.form)
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.effects.replayCache.isEmpty())
    }

    @Test
    fun `valid edit updates the existing transaction ID and emits saved effect`() = runTest {
        val original = expenseTransaction()
        val repository = FakeTransactionRepository().apply {
            transactions[original.id] = original
        }
        val updateClock = Clock.fixed(UPDATE_INSTANT, LONDON_ZONE)
        val viewModel = createViewModel(repository, updateClock)

        viewModel.onEvent(TransactionUiEvent.StartEdit(original.id))
        advanceUntilIdle()
        viewModel.onEvent(TransactionUiEvent.AmountChanged("20.50"))
        viewModel.onEvent(TransactionUiEvent.NoteChanged("Updated"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        val updated = requireNotNull(repository.updatedTransaction)
        assertEquals(original.id, updated.id)
        assertEquals(2050L, updated.amount.minorUnits)
        assertEquals("Updated", updated.note)
        assertEquals(TransactionUiEffect.Saved(original.id), effect.await())
    }

    @Test
    fun `update returning false stops saving preserves form and emits no saved effect`() = runTest {
        val original = expenseTransaction()
        val repository = FakeTransactionRepository().apply {
            transactions[original.id] = original
            updateResult = false
        }
        val viewModel = createViewModel(
            repository = repository,
            clock = Clock.fixed(UPDATE_INSTANT, LONDON_ZONE),
        )

        viewModel.onEvent(TransactionUiEvent.StartEdit(original.id))
        advanceUntilIdle()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.form != null)
        assertEquals(original.id, viewModel.uiState.value.form?.transactionId)
        assertEquals(TransactionUiErrorKind.NOT_FOUND, viewModel.uiState.value.formError?.kind)
        assertFalse(effect.isCompleted)
        effect.cancel()
    }

    @Test
    fun `domain validation failure preserves entered form values`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository, Clock.fixed(ADD_INSTANT, LONDON_ZONE))
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateChanged(ADD_TODAY.plusDays(1)))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertNull(repository.insertedTransaction)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(TransactionUiErrorKind.VALIDATION, viewModel.uiState.value.formError?.kind)
        assertEquals("10.00", viewModel.uiState.value.form?.amountText)
        assertEquals(ADD_TODAY.plusDays(1), viewModel.uiState.value.form?.date)
    }

    @Test
    fun `amount validation uses stable presentation copy`() = runTest {
        val viewModel = createViewModel(
            FakeTransactionRepository(),
            Clock.fixed(ADD_INSTANT, LONDON_ZONE),
        )
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.001"))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)

        val form = requireNotNull(viewModel.uiState.value.form)
        assertEquals(
            "Enter an amount with no more than two decimal places.",
            form.amountError,
        )
        assertFalse(form.amountError.orEmpty().contains("GBP amounts support"))
    }

    @Test
    fun `domain category validation uses stable presentation copy`() = runTest {
        val viewModel = createViewModel(
            FakeTransactionRepository(),
            Clock.fixed(ADD_INSTANT, LONDON_ZONE),
        )
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("income_salary"))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            "Select a valid category for this transaction type.",
            viewModel.uiState.value.formError?.message,
        )
        assertFalse(
            viewModel.uiState.value.formError?.message.orEmpty().contains("income_salary"),
        )
    }

    @Test
    fun `domain note validation uses stable presentation copy`() = runTest {
        val viewModel = createViewModel(
            FakeTransactionRepository(),
            Clock.fixed(ADD_INSTANT, LONDON_ZONE),
        )
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.NoteChanged("x".repeat(TransactionDraft.MAX_NOTE_LENGTH + 1)))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            "Note must be 500 characters or fewer.",
            viewModel.uiState.value.formError?.message,
        )
        assertFalse(
            viewModel.uiState.value.formError?.message.orEmpty().contains("Transaction note must"),
        )
    }

    @Test
    fun `domain date validation uses stable presentation copy`() = runTest {
        val viewModel = createViewModel(
            FakeTransactionRepository(),
            Clock.fixed(ADD_INSTANT, LONDON_ZONE),
        )
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateChanged(ADD_TODAY.plusDays(1)))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            "Choose today or an earlier date.",
            viewModel.uiState.value.formError?.message,
        )
        assertFalse(
            viewModel.uiState.value.formError?.message.orEmpty()
                .contains("Transaction date must"),
        )
    }

    @Test
    fun `save failure preserves entered form values`() = runTest {
        val repository = FakeTransactionRepository().apply {
            insertException = IllegalStateException("database")
        }
        val viewModel = createViewModel(repository, Clock.fixed(ADD_INSTANT, LONDON_ZONE))
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(TransactionUiErrorKind.OPERATION, viewModel.uiState.value.formError?.kind)
        assertEquals(
            "Unable to save the transaction.",
            viewModel.uiState.value.formError?.message,
        )
        assertEquals("10.00", viewModel.uiState.value.form?.amountText)
        assertEquals("expense_food", viewModel.uiState.value.form?.categoryId)
        assertTrue(viewModel.effects.replayCache.isEmpty())
    }

    @Test
    fun `delete requires confirmation and cancellation does not call use case`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))

        assertEquals("transaction-1", viewModel.uiState.value.pendingDeleteId)
        assertTrue(repository.deleteCalls.isEmpty())

        viewModel.onEvent(TransactionUiEvent.DeleteCancelled)

        assertNull(viewModel.uiState.value.pendingDeleteId)
        assertTrue(repository.deleteCalls.isEmpty())
    }

    @Test
    fun `successful delete clears progress and emits deleted effect`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteResult = true
        }
        val viewModel = createViewModel(repository)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf("transaction-1"), repository.deleteCalls)
        assertNull(viewModel.uiState.value.deletingTransactionId)
        assertEquals(TransactionUiEffect.Deleted("transaction-1"), effect.await())
        assertTrue(viewModel.effects.replayCache.isEmpty())
    }

    @Test
    fun `delete returning false exposes not found error without effect`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteResult = false
        }
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingTransactionId)
        assertEquals(TransactionUiErrorKind.NOT_FOUND, viewModel.uiState.value.deleteError?.kind)
        assertTrue(viewModel.effects.replayCache.isEmpty())
    }

    @Test
    fun `delete exception exposes operation error without effect`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteException = IllegalStateException("database")
        }
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingTransactionId)
        assertEquals(TransactionUiErrorKind.OPERATION, viewModel.uiState.value.deleteError?.kind)
        assertEquals(
            "Unable to delete the transaction.",
            viewModel.uiState.value.deleteError?.message,
        )
        assertTrue(viewModel.effects.replayCache.isEmpty())
    }

    @Test
    fun `clear error resets delete error`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteResult = false
        }
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(TransactionUiErrorKind.NOT_FOUND, viewModel.uiState.value.deleteError?.kind)

        viewModel.onEvent(TransactionUiEvent.ClearError)

        assertNull(viewModel.uiState.value.deleteError)
    }

    @Test
    fun `duplicate save is ignored while the first save is in progress`() = runTest {
        val repository = FakeTransactionRepository().apply {
            insertStarted = CompletableDeferred()
            releaseInsert = CompletableDeferred()
        }
        val viewModel = createViewModel(repository, Clock.fixed(ADD_INSTANT, LONDON_ZONE))
        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("10.00"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))

        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()
        repository.insertStarted?.await()

        viewModel.onEvent(TransactionUiEvent.SaveClicked)

        assertEquals(1, repository.insertCalls)
        repository.releaseInsert?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `duplicate delete is ignored while the first delete is in progress`() = runTest {
        val repository = FakeTransactionRepository().apply {
            deleteStarted = CompletableDeferred()
            releaseDelete = CompletableDeferred()
            deleteResult = true
        }
        val viewModel = createViewModel(repository)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()
        repository.deleteStarted?.await()

        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        viewModel.onEvent(TransactionUiEvent.DeleteRequested("transaction-1"))

        assertEquals(1, repository.deleteCalls.size)
        repository.releaseDelete?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `search filter with empty or blank query returns all transactions`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Coffee")
        val t2 = expenseTransaction(id = "2", note = "Tea")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged(""))
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("   "))
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
        assertFalse(viewModel.uiState.value.hasActiveFilters)
    }

    @Test
    fun `search filter matches category label`() = runTest {
        val t1 = customTransaction(id = "1", type = TransactionType.EXPENSE, categoryId = "expense_food", note = "Snack")
        val t2 = customTransaction(id = "2", type = TransactionType.INCOME, categoryId = "income_salary", note = "Bonus")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Food"))
        val filtered = viewModel.uiState.value.filteredTransactions
        assertEquals(1, filtered.size)
        assertEquals("1", filtered.single().id)
        assertEquals(2, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun `search filter matches note`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Tesco grocery shopping")
        val t2 = expenseTransaction(id = "2", note = "Bus ticket")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Tesco"))
        val filtered = viewModel.uiState.value.filteredTransactions
        assertEquals(1, filtered.size)
        assertEquals("1", filtered.single().id)
    }

    @Test
    fun `search filter is case-insensitive and matches substrings with trimmed input`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Weekly Sainsbury Groceries")
        val t2 = expenseTransaction(id = "2", note = "Cinema")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("  sainsbury  "))
        val filtered = viewModel.uiState.value.filteredTransactions
        assertEquals(1, filtered.size)
        assertEquals("1", filtered.single().id)
    }

    @Test
    fun `search filter with no match returns empty filtered list but preserves source transactions`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Dinner")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Electric"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())
        assertEquals(1, viewModel.uiState.value.transactions.size)
    }

    @Test
    fun `search filter does not match amount, currency, date or transaction id`() = runTest {
        val t1 = expenseTransaction(id = "transaction-123", note = "Lunch")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("transaction-123"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("1025"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("GBP"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("2026"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())
    }

    @Test
    fun `type filter selects all, income, or expense transactions`() = runTest {
        val income = incomeTransaction(id = "1")
        val expense = expenseTransaction(id = "2")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(income, expense)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.INCOME))
        assertEquals(listOf("1"), viewModel.uiState.value.filteredTransactions.map { it.id })

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        assertEquals(listOf("2"), viewModel.uiState.value.filteredTransactions.map { it.id })

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(null))
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
    }

    @Test
    fun `changing type filter coordinates category filter selection`() = runTest {
        val viewModel = createViewModel(FakeTransactionRepository())
        advanceUntilIdle()

        // 1. Expense category -> change to Income -> category cleared
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertEquals("expense_food", viewModel.uiState.value.filter.categoryId)

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.INCOME))
        assertEquals(TransactionType.INCOME, viewModel.uiState.value.filter.type)
        assertNull(viewModel.uiState.value.filter.categoryId)

        // 2. Income category -> change to Expense -> category cleared
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("income_salary"))
        assertEquals("income_salary", viewModel.uiState.value.filter.categoryId)

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.filter.type)
        assertNull(viewModel.uiState.value.filter.categoryId)

        // 3. Expense category -> change to All -> category retained
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertEquals("expense_food", viewModel.uiState.value.filter.categoryId)

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(null))
        assertNull(viewModel.uiState.value.filter.type)
        assertEquals("expense_food", viewModel.uiState.value.filter.categoryId)
    }

    @Test
    fun `category filter selects specific category or all categories`() = runTest {
        val t1 = customTransaction(id = "1", type = TransactionType.EXPENSE, categoryId = "expense_food")
        val t2 = customTransaction(id = "2", type = TransactionType.EXPENSE, categoryId = "expense_transport")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertEquals(listOf("1"), viewModel.uiState.value.filteredTransactions.map { it.id })

        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_bills"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged(null))
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
    }

    @Test
    fun `date filter handles dateFrom, dateTo, range, and inclusive boundaries`() = runTest {
        val day1 = ADD_TODAY.minusDays(2)
        val day2 = ADD_TODAY.minusDays(1)
        val day3 = ADD_TODAY

        val t1 = expenseTransaction(id = "1", date = day1)
        val t2 = expenseTransaction(id = "2", date = day2)
        val t3 = expenseTransaction(id = "3", date = day3)

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2, t3)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        // dateFrom only (inclusive)
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day2))
        assertEquals(listOf("2", "3"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // dateTo only (inclusive)
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(null))
        viewModel.onEvent(TransactionUiEvent.DateToChanged(day2))
        assertEquals(listOf("1", "2"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // date range (inclusive on both ends)
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day1))
        viewModel.onEvent(TransactionUiEvent.DateToChanged(day2))
        assertEquals(listOf("1", "2"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // date range excluding all
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day3.plusDays(1)))
        viewModel.onEvent(TransactionUiEvent.DateToChanged(day3.plusDays(2)))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())
    }

    @Test
    fun `combined filter criteria use AND semantics`() = runTest {
        val day1 = ADD_TODAY.minusDays(1)
        val day2 = ADD_TODAY

        val t1 = customTransaction(id = "1", type = TransactionType.EXPENSE, categoryId = "expense_food", date = day1, note = "Tesco groceries")
        val t2 = customTransaction(id = "2", type = TransactionType.EXPENSE, categoryId = "expense_food", date = day2, note = "Lunch cafe")
        val t3 = customTransaction(id = "3", type = TransactionType.EXPENSE, categoryId = "expense_transport", date = day2, note = "Tesco petrol")
        val t4 = customTransaction(id = "4", type = TransactionType.INCOME, categoryId = "income_freelance", date = day2, note = "Tesco client contract")

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2, t3, t4)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        // Search + Type
        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Tesco"))
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        assertEquals(listOf("1", "3"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Search + Category
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(null))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertEquals(listOf("1"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Search + Date
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged(null))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day2))
        assertEquals(listOf("3", "4"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Type + Category
        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged(""))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(null))
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertEquals(listOf("1", "2"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Type + Date
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged(null))
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.INCOME))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day2))
        assertEquals(listOf("4"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Category + Date
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(null))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day2))
        assertEquals(listOf("2"), viewModel.uiState.value.filteredTransactions.map { it.id })

        // Search + Type + Category + Date
        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("groceries"))
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(day1))
        viewModel.onEvent(TransactionUiEvent.DateToChanged(day1))
        assertEquals(listOf("1"), viewModel.uiState.value.filteredTransactions.map { it.id })
    }

    @Test
    fun `clear filters resets all filter fields and restores filteredTransactions`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Coffee")
        val t2 = incomeTransaction(id = "2")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1, t2)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Coffee"))
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(ADD_TODAY))
        viewModel.onEvent(TransactionUiEvent.DateToChanged(ADD_TODAY))

        assertTrue(viewModel.uiState.value.hasActiveFilters)
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)

        viewModel.onEvent(TransactionUiEvent.ClearFilters)

        val state = viewModel.uiState.value
        assertEquals("", state.filter.searchQuery)
        assertNull(state.filter.type)
        assertNull(state.filter.categoryId)
        assertNull(state.filter.dateFrom)
        assertNull(state.filter.dateTo)
        assertFalse(state.hasActiveFilters)
        assertEquals(2, state.filteredTransactions.size)
    }

    @Test
    fun `hasActiveFilters correctly derives active filter state`() = runTest {
        val viewModel = createViewModel(FakeTransactionRepository())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasActiveFilters)

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("test"))
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged(""))

        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(TransactionType.EXPENSE))
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        viewModel.onEvent(TransactionUiEvent.TransactionTypeFilterChanged(null))

        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged("expense_food"))
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        viewModel.onEvent(TransactionUiEvent.CategoryFilterChanged(null))

        viewModel.onEvent(TransactionUiEvent.DateFromChanged(ADD_TODAY))
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        viewModel.onEvent(TransactionUiEvent.DateFromChanged(null))

        viewModel.onEvent(TransactionUiEvent.DateToChanged(ADD_TODAY))
        assertTrue(viewModel.uiState.value.hasActiveFilters)
        viewModel.onEvent(TransactionUiEvent.DateToChanged(null))

        assertFalse(viewModel.uiState.value.hasActiveFilters)
    }

    @Test
    fun `underlying transaction flow emission recalculates filtered transactions automatically`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Coffee")
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(t1)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Coffee"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)

        val t2 = expenseTransaction(id = "2", note = "Another Coffee")
        val t3 = expenseTransaction(id = "3", note = "Sandwich")
        repository.observedTransactions.value = listOf(t1, t2, t3)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.transactions.size)
        assertEquals(listOf("1", "2"), viewModel.uiState.value.filteredTransactions.map { it.id })
    }

    @Test
    fun `adding a matching transaction makes it appear in filtered results`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Lunch"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.StartAdd)
        viewModel.onEvent(TransactionUiEvent.AmountChanged("15.50"))
        viewModel.onEvent(TransactionUiEvent.CategoryChanged("expense_food"))
        viewModel.onEvent(TransactionUiEvent.DateChanged(ADD_TODAY))
        viewModel.onEvent(TransactionUiEvent.NoteChanged("Quick Lunch"))
        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        val saved = repository.insertedTransaction!!
        repository.observedTransactions.value = listOf(saved)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("Quick Lunch", viewModel.uiState.value.filteredTransactions.single().note)
    }

    @Test
    fun `editing a transaction updates its presence in filtered results`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Lunch")
        val repository = FakeTransactionRepository().apply {
            transactions["1"] = t1
            observedTransactions.value = listOf(t1)
            updateResult = true
        }
        val viewModel = createViewModel(
            repository = repository,
            clock = Clock.fixed(UPDATE_INSTANT, LONDON_ZONE),
        )
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Dinner"))
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())

        viewModel.onEvent(TransactionUiEvent.StartEdit("1"))
        advanceUntilIdle()
        viewModel.onEvent(TransactionUiEvent.NoteChanged("Family Dinner"))
        viewModel.onEvent(TransactionUiEvent.SaveClicked)
        advanceUntilIdle()

        val updated = repository.updatedTransaction!!
        repository.observedTransactions.value = listOf(updated)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("Family Dinner", viewModel.uiState.value.filteredTransactions.single().note)
    }

    @Test
    fun `deleting a transaction removes it from filtered results`() = runTest {
        val t1 = expenseTransaction(id = "1", note = "Coffee")
        val repository = FakeTransactionRepository().apply {
            transactions["1"] = t1
            observedTransactions.value = listOf(t1)
            deleteResult = true
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(TransactionUiEvent.SearchQueryChanged("Coffee"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)

        viewModel.onEvent(TransactionUiEvent.DeleteRequested("1"))
        viewModel.onEvent(TransactionUiEvent.DeleteConfirmed)
        advanceUntilIdle()

        repository.observedTransactions.value = emptyList()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())
    }

    private fun createViewModel(
        repository: FakeTransactionRepository,
        clock: Clock = Clock.fixed(ADD_INSTANT, LONDON_ZONE),
        zoneId: ZoneId = LONDON_ZONE,
    ): TransactionViewModel = TransactionViewModel(
        getTransactionsUseCase = GetTransactionsUseCase(repository),
        getTransactionUseCase = GetTransactionUseCase(repository),
        addTransactionUseCase = AddTransactionUseCase(repository, clock, zoneId),
        updateTransactionUseCase = UpdateTransactionUseCase(repository, clock, zoneId),
        deleteTransactionUseCase = DeleteTransactionUseCase(repository),
        clock = clock,
        zoneId = zoneId,
    )

    private fun expenseTransaction(
        id: String = "transaction-1",
        date: LocalDate = ADD_TODAY,
        note: String? = "Lunch",
    ): Transaction = Transaction.create(
        id = id,
        draft = TransactionDraft(
            amount = Money.fromMinorUnits(1025L, CurrencyCode.GBP),
            type = TransactionType.EXPENSE,
            categoryId = CategoryId.of("expense_food"),
            transactionDate = date,
            note = note,
        ),
        clock = Clock.fixed(CREATED_INSTANT, LONDON_ZONE),
        zoneId = LONDON_ZONE,
    )

    private fun incomeTransaction(
        id: String = "transaction-income-1",
        date: LocalDate = ADD_TODAY,
        categoryId: String = "income_salary",
        note: String? = "Monthly salary",
    ): Transaction = Transaction.create(
        id = id,
        draft = TransactionDraft(
            amount = Money.fromMinorUnits(300000L, CurrencyCode.GBP),
            type = TransactionType.INCOME,
            categoryId = CategoryId.of(categoryId),
            transactionDate = date,
            note = note,
        ),
        clock = Clock.fixed(CREATED_INSTANT, LONDON_ZONE),
        zoneId = LONDON_ZONE,
    )

    private fun customTransaction(
        id: String,
        type: TransactionType,
        categoryId: String,
        date: LocalDate = ADD_TODAY,
        note: String? = null,
        amountMinorUnits: Long = 1000L,
    ): Transaction = Transaction.create(
        id = id,
        draft = TransactionDraft(
            amount = Money.fromMinorUnits(amountMinorUnits, CurrencyCode.GBP),
            type = type,
            categoryId = CategoryId.of(categoryId),
            transactionDate = date,
            note = note,
        ),
        clock = Clock.fixed(CREATED_INSTANT, LONDON_ZONE),
        zoneId = LONDON_ZONE,
    )

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = mutableMapOf<String, Transaction>()
        val observedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
        val observedFlows = ArrayDeque<Flow<List<Transaction>>>()
        val deleteCalls = mutableListOf<String>()

        var observeCallCount = 0
            private set
        var insertedTransaction: Transaction? = null
            private set
        var updatedTransaction: Transaction? = null
            private set
        var insertCalls = 0
            private set
        var insertException: Exception? = null
        var deleteException: Exception? = null
        var updateResult = true
        var deleteResult = false
        var insertStarted: CompletableDeferred<Unit>? = null
        var releaseInsert: CompletableDeferred<Unit>? = null
        var deleteStarted: CompletableDeferred<Unit>? = null
        var releaseDelete: CompletableDeferred<Unit>? = null

        override fun observeTransactions(): Flow<List<Transaction>> {
            observeCallCount += 1
            return if (observedFlows.isEmpty()) {
                observedTransactions
            } else {
                observedFlows.removeFirst()
            }
        }

        override suspend fun getTransaction(id: String): Transaction? = transactions[id]

        override suspend fun insertTransaction(transaction: Transaction) {
            insertCalls += 1
            insertStarted?.complete(Unit)
            releaseInsert?.await()
            insertException?.let { throw it }
            insertedTransaction = transaction
            transactions[transaction.id] = transaction
        }

        override suspend fun updateTransaction(transaction: Transaction): Boolean {
            updatedTransaction = transaction
            if (updateResult) {
                transactions[transaction.id] = transaction
            }
            return updateResult
        }

        override suspend fun deleteTransaction(id: String): Boolean {
            deleteCalls += id
            deleteStarted?.complete(Unit)
            releaseDelete?.await()
            deleteException?.let { throw it }
            if (deleteResult) {
                transactions.remove(id)
            }
            return deleteResult
        }
    }

    private class MainDispatcherRule : TestWatcher() {
        private val dispatcher = StandardTestDispatcher()

        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        val LONDON_ZONE: ZoneId = ZoneId.of("Europe/London")
        val CREATED_INSTANT: Instant = Instant.parse("2026-09-05T12:00:00Z")
        val ADD_INSTANT: Instant = Instant.parse("2026-09-05T12:00:00Z")
        val UPDATE_INSTANT: Instant = Instant.parse("2026-09-05T13:00:00Z")
        val ADD_TODAY: LocalDate = LocalDate.of(2026, 9, 5)
    }
}
