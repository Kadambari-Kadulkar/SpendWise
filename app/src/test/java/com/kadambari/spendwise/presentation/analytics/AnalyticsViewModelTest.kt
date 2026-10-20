package com.kadambari.spendwise.presentation.analytics

import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.repository.TransactionRepository
import com.kadambari.spendwise.domain.usecase.GetAnalyticsUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.ArrayDeque
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestRule = MainDispatcherRule()

    @Test
    fun `initial state starts loading and defaults to THIS_MONTH`() = runTest {
        val repository = FakeTransactionRepository().apply {
            setFailingFlow(emptyFlow())
        }
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(AnalyticsPeriod.THIS_MONTH, viewModel.uiState.value.selectedPeriod)
        assertNull(viewModel.uiState.value.error)
        assertEquals(AnalyticsEmptyType.NONE, viewModel.uiState.value.emptyType)
    }

    @Test
    fun `empty transaction repository produces NO_TRANSACTIONS_OVERALL empty state`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(AnalyticsEmptyType.NO_TRANSACTIONS_OVERALL, viewModel.uiState.value.emptyType)
        assertEquals(0L, viewModel.uiState.value.totalIncomeMinorUnits)
        assertEquals(0L, viewModel.uiState.value.totalExpensesMinorUnits)
        assertEquals(0L, viewModel.uiState.value.netCashFlowMinorUnits)
        assertEquals("£0.00", viewModel.uiState.value.netCashFlowFormatted)
        assertTrue(viewModel.uiState.value.categoryBreakdown.isEmpty())
        assertNull(viewModel.uiState.value.error)
        assertEquals(
            "No expenses recorded in this period or the previous period.",
            viewModel.uiState.value.comparison?.summaryText,
        )
    }

    @Test
    fun `populated transactions for THIS_MONTH are correctly mapped`() = runTest {
        val income = createTransaction(
            id = "tx-inc",
            amountMinorUnits = 350000L,
            type = TransactionType.INCOME,
            categoryId = "income_salary",
            date = LocalDate.of(2026, 10, 5),
        )
        val expense = createTransaction(
            id = "tx-exp",
            amountMinorUnits = 120000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 8),
        )
        val priorMonthExpense = createTransaction(
            id = "tx-prior",
            amountMinorUnits = 100000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 9, 8),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(income, expense, priorMonthExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(AnalyticsEmptyType.NONE, state.emptyType)
        assertEquals(350000L, state.totalIncomeMinorUnits)
        assertEquals("+£3,500.00", state.totalIncomeFormatted)
        assertEquals(120000L, state.totalExpensesMinorUnits)
        assertEquals("-£1,200.00", state.totalExpensesFormatted)
        assertEquals(230000L, state.netCashFlowMinorUnits)
        assertEquals("+£2,300.00", state.netCashFlowFormatted)
        assertEquals("1 Oct 2026 – 16 Oct 2026", state.periodDateRangeFormatted)

        // Category breakdown
        assertEquals(1, state.categoryBreakdown.size)
        val category = state.categoryBreakdown.single()
        assertEquals("expense_food", category.categoryId)
        assertEquals("Food", category.categoryLabel)
        assertEquals(120000L, category.amountMinorUnits)
        assertEquals("£1,200.00", category.amountFormatted)
        assertEquals("100.0%", category.percentageFormatted)
        assertEquals(1.0f, category.progressRatio, 0.001f)

        // Comparison
        val comparison = state.comparison
        assertNotNull(comparison)
        assertEquals("vs same period last month", comparison!!.baselineDescription)
        assertEquals(20000L, comparison.expenseDifferenceMinorUnits)
        assertEquals("+£200.00", comparison.differenceFormatted)
        assertEquals("+20.0%", comparison.percentageDeltaFormatted)
        assertTrue(comparison.isExpenseIncrease)
        assertFalse(comparison.isExpenseDecrease)
        assertFalse(comparison.isUnchanged)
        assertEquals("Spent £200.00 more than previous period (+20.0%).", comparison.summaryText)
    }

    @Test
    fun `period selection updates selected period and loads LAST_MONTH report`() = runTest {
        val lastMonthExpense = createTransaction(
            id = "tx-last-month",
            amountMinorUnits = 80000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_shopping",
            date = LocalDate.of(2026, 9, 15),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(lastMonthExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(AnalyticsPeriod.LAST_MONTH))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalyticsPeriod.LAST_MONTH, state.selectedPeriod)
        assertEquals("1 Sept 2026 – 30 Sept 2026", state.periodDateRangeFormatted)
        assertEquals(80000L, state.totalExpensesMinorUnits)
        assertEquals("-£800.00", state.totalExpensesFormatted)
        assertEquals("vs previous month", state.comparison?.baselineDescription)
    }

    @Test
    fun `period selection updates selected period and loads LAST_THREE_MONTHS report`() = runTest {
        val augExpense = createTransaction(
            id = "tx-aug",
            amountMinorUnits = 50000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_bills",
            date = LocalDate.of(2026, 8, 10),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(augExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(AnalyticsPeriod.LAST_THREE_MONTHS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalyticsPeriod.LAST_THREE_MONTHS, state.selectedPeriod)
        assertEquals("1 Aug 2026 – 16 Oct 2026", state.periodDateRangeFormatted)
        assertEquals(3, state.trendBuckets.size)
        assertEquals("vs previous 3 months", state.comparison?.baselineDescription)
    }

    @Test
    fun `duplicate period selection does not restart observation`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        val initialCallCount = repository.observeCallCount

        viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(AnalyticsPeriod.THIS_MONTH))
        advanceUntilIdle()

        assertEquals(initialCallCount, repository.observeCallCount)
    }

    @Test
    fun `switching period clears stale data while new period loads`() = runTest {
        val income = createTransaction(
            id = "tx-inc",
            amountMinorUnits = 350000L,
            type = TransactionType.INCOME,
            categoryId = "income_salary",
            date = LocalDate.of(2026, 10, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(income)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        assertEquals(350000L, viewModel.uiState.value.totalIncomeMinorUnits)

        // Set delayed flow before switching period
        repository.setFailingFlow(emptyFlow())
        viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(AnalyticsPeriod.LAST_MONTH))

        // State immediately after selection should be in clean loading state
        val state = viewModel.uiState.value
        assertEquals(AnalyticsPeriod.LAST_MONTH, state.selectedPeriod)
        assertTrue(state.isLoading)
        assertEquals(0L, state.totalIncomeMinorUnits)
        assertTrue(state.categoryBreakdown.isEmpty())
        assertNull(state.comparison)
    }

    @Test
    fun `currency mismatch in period maps to CURRENCY_MISMATCH empty state`() = runTest {
        val eurTransaction = createRawTransaction(
            id = "tx-eur",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
            currencyCode = CurrencyCode.of("EUR"),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(eurTransaction)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals(AnalyticsEmptyType.CURRENCY_MISMATCH, viewModel.uiState.value.emptyType)
    }

    @Test
    fun `no transactions in period maps to NO_PERIOD_TRANSACTIONS empty state`() = runTest {
        val olderTransaction = createTransaction(
            id = "tx-older",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 5, 1),
            currencyCode = CurrencyCode.GBP,
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(olderTransaction)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals(AnalyticsEmptyType.NO_PERIOD_TRANSACTIONS, viewModel.uiState.value.emptyType)
    }

    @Test
    fun `zero-baseline comparison with zero current expenses maps correctly`() = runTest {
        val income1 = createTransaction(
            id = "tx-inc1",
            amountMinorUnits = 10000L,
            type = TransactionType.INCOME,
            categoryId = "income_salary",
            date = LocalDate.of(2026, 10, 5),
        )
        val income2 = createTransaction(
            id = "tx-inc2",
            amountMinorUnits = 10000L,
            type = TransactionType.INCOME,
            categoryId = "income_salary",
            date = LocalDate.of(2026, 9, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(income1, income2)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("No expenses recorded in this period or the previous period.", comparison!!.summaryText)
        assertNull(comparison.percentageDelta)
        assertTrue(comparison.isUnchanged)
    }

    @Test
    fun `zero-baseline comparison with positive current expenses maps correctly`() = runTest {
        val expense = createTransaction(
            id = "tx-exp",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(expense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Spent £50.00 (no expenses recorded in previous period).", comparison!!.summaryText)
        assertNull(comparison.percentageDelta)
        assertTrue(comparison.isExpenseIncrease)
    }

    @Test
    fun `comparison with zero current expenses and positive baseline expenses maps to minus 100 percent`() = runTest {
        val priorExpense = createTransaction(
            id = "tx-prior",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 9, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(priorExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Spent £50.00 less than previous period (-100.0%).", comparison!!.summaryText)
        assertEquals("-100.0%", comparison.percentageDeltaFormatted)
        assertTrue(comparison.isExpenseDecrease)
    }

    @Test
    fun `standard comparison with decreased expenses maps correctly`() = runTest {
        val currentExpense = createTransaction(
            id = "tx-curr",
            amountMinorUnits = 7000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
        )
        val priorExpense = createTransaction(
            id = "tx-prior",
            amountMinorUnits = 10000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 9, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(currentExpense, priorExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Spent £30.00 less than previous period (-30.0%).", comparison!!.summaryText)
        assertTrue(comparison.isExpenseDecrease)
    }

    @Test
    fun `standard comparison with unchanged expenses maps correctly`() = runTest {
        val currentExpense = createTransaction(
            id = "tx-curr",
            amountMinorUnits = 10000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
        )
        val priorExpense = createTransaction(
            id = "tx-prior",
            amountMinorUnits = 10000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 9, 5),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(currentExpense, priorExpense)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertNotNull(comparison)
        assertEquals("Spending unchanged from previous period (0.0%).", comparison!!.summaryText)
        assertTrue(comparison.isUnchanged)
    }

    @Test
    fun `category breakdown is sorted descending by amount with resolved display names`() = runTest {
        val food = createTransaction(
            id = "tx-food",
            amountMinorUnits = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 2),
        )
        val shopping = createTransaction(
            id = "tx-shopping",
            amountMinorUnits = 15000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_shopping",
            date = LocalDate.of(2026, 10, 3),
        )
        val bills = createTransaction(
            id = "tx-bills",
            amountMinorUnits = 10000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_bills",
            date = LocalDate.of(2026, 10, 4),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(food, shopping, bills)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val categories = viewModel.uiState.value.categoryBreakdown
        assertEquals(3, categories.size)
        assertEquals("expense_shopping", categories[0].categoryId)
        assertEquals("Shopping", categories[0].categoryLabel)
        assertEquals("£150.00", categories[0].amountFormatted)
        assertEquals(0.5f, categories[0].progressRatio, 0.001f)

        assertEquals("expense_bills", categories[1].categoryId)
        assertEquals("Bills", categories[1].categoryLabel)
        assertEquals("£100.00", categories[1].amountFormatted)

        assertEquals("expense_food", categories[2].categoryId)
        assertEquals("Food", categories[2].categoryLabel)
        assertEquals("£50.00", categories[2].amountFormatted)
    }

    @Test
    fun `trend buckets format income and expenses and preserve current and future flags`() = runTest {
        val tx = createTransaction(
            id = "tx-curr",
            amountMinorUnits = 2500L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 3),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(tx)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val buckets = viewModel.uiState.value.trendBuckets
        assertEquals(5, buckets.size) // 1-7, 8-14, 15-21, 22-28, 29-31
        assertEquals("£25.00", buckets[0].expenseFormatted)
        assertEquals("£0.00", buckets[0].incomeFormatted)

        // Today is Oct 16 (in bucket index 2: 15-21)
        assertTrue(buckets[2].isPartialCurrent)
        assertFalse(buckets[2].isWhollyFuture)

        // Bucket index 3 (22-28) is wholly future
        assertTrue(buckets[3].isWhollyFuture)
        assertFalse(buckets[3].isPartialCurrent)
    }

    @Test
    fun `upstream flow failure exposes user-facing error`() = runTest {
        val repository = FakeTransactionRepository().apply {
            setFailingFlow(flow { throw IllegalStateException("Database read failure") })
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Couldn't load your analytics. Please try again.", viewModel.uiState.value.error?.message)
    }

    @Test
    fun `retry restarts observation and recovers`() = runTest {
        val repository = FakeTransactionRepository().apply {
            setFailingFlow(flow { throw IllegalStateException("Database read failure") })
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(1, repository.observeCallCount)

        // Clear failure and provide valid transaction
        val income = createTransaction(
            id = "tx-inc",
            amountMinorUnits = 350000L,
            type = TransactionType.INCOME,
            categoryId = "income_salary",
            date = LocalDate.of(2026, 10, 5),
        )
        repository.clearFailingFlow()
        repository.observedTransactions.value = listOf(income)

        viewModel.onEvent(AnalyticsUiEvent.Retry)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(2, repository.observeCallCount)
        assertEquals("+£3,500.00", viewModel.uiState.value.totalIncomeFormatted)
    }

    @Test
    fun `live updates from repository update ui state without manual reload`() = runTest {
        val food = createTransaction(
            id = "tx-food",
            amountMinorUnits = 2000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 2),
        )

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(food)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        assertEquals("-£20.00", viewModel.uiState.value.totalExpensesFormatted)
        assertEquals(1, viewModel.uiState.value.categoryBreakdown.size)

        // Live update: add another transaction to shared flow
        val shopping = createTransaction(
            id = "tx-shopping",
            amountMinorUnits = 3000L,
            type = TransactionType.EXPENSE,
            categoryId = "expense_shopping",
            date = LocalDate.of(2026, 10, 4),
        )
        repository.observedTransactions.value = listOf(food, shopping)

        advanceUntilIdle()

        assertEquals("-£50.00", viewModel.uiState.value.totalExpensesFormatted)
        assertEquals(2, viewModel.uiState.value.categoryBreakdown.size)
    }

    @Test
    fun `cancellation is propagated and not converted to ordinary ui error`() = runTest {
        val repository = FakeTransactionRepository().apply {
            setFailingFlow(flow { throw CancellationException("Scope cancelled") })
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `switching period cancels previous active collection and starts observing new period`() = runTest {
        val firstCollectionStarted = CompletableDeferred<Unit>()
        val firstCollectionCancelled = CompletableDeferred<Boolean>()
        val secondCollectionStarted = CompletableDeferred<Unit>()

        val firstFlow = flow<List<Transaction>> {
            try {
                emit(emptyList())
                firstCollectionStarted.complete(Unit)
                awaitCancellation()
            } finally {
                firstCollectionCancelled.complete(!currentCoroutineContext().isActive)
            }
        }

        val secondFlow = flow<List<Transaction>> {
            secondCollectionStarted.complete(Unit)
            emit(emptyList())
        }

        val repository = FakeTransactionRepository().apply {
            enqueueFlow(firstFlow)
            enqueueFlow(secondFlow)
        }
        val viewModel = createViewModel(repository)

        // 1. Advance to start initial collection for THIS_MONTH
        advanceUntilIdle()

        assertTrue("Initial collection should have started", firstCollectionStarted.isCompleted)
        assertFalse("Initial collection should still be active, not cancelled", firstCollectionCancelled.isCompleted)
        assertEquals(AnalyticsPeriod.THIS_MONTH, viewModel.uiState.value.selectedPeriod)
        assertEquals(1, repository.observeCallCount)

        // 2. Switch period to LAST_MONTH
        viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(AnalyticsPeriod.LAST_MONTH))
        advanceUntilIdle()

        // 3. Verify cancellation and replacement
        assertTrue("First collection should have run its finally block upon cancellation", firstCollectionCancelled.isCompleted)
        assertTrue("First collection should be cancelled rather than completed normally", firstCollectionCancelled.await())
        assertTrue("Replacement collection for LAST_MONTH should have started", secondCollectionStarted.isCompleted)
        assertEquals(AnalyticsPeriod.LAST_MONTH, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, repository.observeCallCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    private fun createViewModel(repository: TransactionRepository): AnalyticsViewModel {
        val useCase = GetAnalyticsUseCase(
            repository = repository,
            clock = Clock.fixed(TEST_INSTANT, TEST_ZONE),
            zoneId = TEST_ZONE,
        )
        return AnalyticsViewModel(useCase)
    }

    private fun createTransaction(
        id: String,
        amountMinorUnits: Long,
        type: TransactionType,
        categoryId: String,
        date: LocalDate,
        currencyCode: CurrencyCode = CurrencyCode.GBP,
        note: String? = null,
        creationInstant: Instant = TEST_INSTANT,
    ): Transaction {
        val clockInstant = if (creationInstant.atZone(TEST_ZONE).toLocalDate().isBefore(date)) {
            date.atTime(12, 0).atZone(TEST_ZONE).toInstant()
        } else {
            creationInstant
        }

        return Transaction.create(
            id = id,
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(amountMinorUnits, currencyCode),
                type = type,
                categoryId = CategoryId.of(categoryId),
                transactionDate = date,
                note = note,
            ),
            clock = Clock.fixed(clockInstant, TEST_ZONE),
            zoneId = TEST_ZONE,
        )
    }

    private fun createRawMoney(minorUnits: Long, currency: CurrencyCode): Money {
        val constructor = Money::class.java.getDeclaredConstructor(
            Long::class.javaPrimitiveType,
            String::class.java,
        )
        constructor.isAccessible = true
        return constructor.newInstance(minorUnits, currency.value)
    }

    private fun createRawTransaction(
        id: String,
        amountMinorUnits: Long,
        type: TransactionType,
        categoryId: String,
        date: LocalDate,
        currencyCode: CurrencyCode,
    ): Transaction {
        val money = createRawMoney(amountMinorUnits, currencyCode)
        val constructor = Transaction::class.java.getDeclaredConstructor(
            String::class.java,
            Money::class.java,
            TransactionType::class.java,
            String::class.java,
            LocalDate::class.java,
            String::class.java,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        )
        constructor.isAccessible = true
        return constructor.newInstance(
            id,
            money,
            type,
            categoryId,
            date,
            null,
            1000L,
            1000L,
        )
    }

    private class FakeTransactionRepository : TransactionRepository {
        val observedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
        private val flowQueue = ArrayDeque<Flow<List<Transaction>>>()
        private var failingFlow: Flow<List<Transaction>>? = null
        var observeCallCount = 0
            private set

        fun enqueueFlow(flow: Flow<List<Transaction>>) {
            flowQueue.add(flow)
        }

        fun setFailingFlow(flow: Flow<List<Transaction>>) {
            this.failingFlow = flow
        }

        fun clearFailingFlow() {
            this.failingFlow = null
        }

        override fun observeTransactions(): Flow<List<Transaction>> {
            observeCallCount++
            return if (flowQueue.isNotEmpty()) {
                flowQueue.removeFirst()
            } else {
                failingFlow ?: observedTransactions
            }
        }

        override suspend fun getTransaction(id: String): Transaction? = null
        override suspend fun insertTransaction(transaction: Transaction) {}
        override suspend fun updateTransaction(transaction: Transaction): Boolean = false
        override suspend fun deleteTransaction(id: String): Boolean = false
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
        val TEST_ZONE: ZoneId = ZoneId.of("Europe/London")
        val TEST_INSTANT: Instant = Instant.parse("2026-10-16T12:00:00Z")
    }
}
