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
        assertNull(form.noteError)
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

    private fun createViewModel(
        repository: FakeTransactionRepository,
        clock: Clock = Clock.fixed(ADD_INSTANT, LONDON_ZONE),
    ): TransactionViewModel = TransactionViewModel(
        getTransactionsUseCase = GetTransactionsUseCase(repository),
        getTransactionUseCase = GetTransactionUseCase(repository),
        addTransactionUseCase = AddTransactionUseCase(repository, clock, LONDON_ZONE),
        updateTransactionUseCase = UpdateTransactionUseCase(repository, clock, LONDON_ZONE),
        deleteTransactionUseCase = DeleteTransactionUseCase(repository),
        clock = clock,
        zoneId = LONDON_ZONE,
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
