package com.kadambari.spendwise.presentation.dashboard

import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.repository.TransactionRepository
import com.kadambari.spendwise.domain.usecase.GetTransactionsUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestRule = MainDispatcherRule()

    @Test
    fun `initial state starts loading and defaults selected month to current month from clock and zone`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedFlows.add(emptyFlow())
        }
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.selectedMonth)
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, viewModel.uiState.value.greeting)
        assertEquals(0L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyIncomeMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyExpensesMinorUnits)
        assertTrue(viewModel.uiState.value.recentTransactions.isEmpty())
        assertTrue(viewModel.uiState.value.hasNoTransactions)
        assertTrue(viewModel.uiState.value.hasNoMonthlyTransactions)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `empty transaction flow finishes loading with empty state`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyIncomeMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyExpensesMinorUnits)
        assertTrue(viewModel.uiState.value.recentTransactions.isEmpty())
        assertTrue(viewModel.uiState.value.hasNoTransactions)
        assertTrue(viewModel.uiState.value.hasNoMonthlyTransactions)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `overall balance calculates correctly for income only`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createIncome(id = "tx1", amountMinorUnits = 5000L, date = LocalDate.of(2026, 10, 1)),
                createIncome(id = "tx2", amountMinorUnits = 3000L, date = LocalDate.of(2026, 9, 15)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals(8000L, viewModel.uiState.value.balanceMinorUnits)
    }

    @Test
    fun `overall balance calculates correctly for expenses only yielding negative balance`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createExpense(id = "tx1", amountMinorUnits = 2500L, date = LocalDate.of(2026, 10, 1)),
                createExpense(id = "tx2", amountMinorUnits = 1500L, date = LocalDate.of(2026, 8, 20)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals(-4000L, viewModel.uiState.value.balanceMinorUnits)
    }

    @Test
    fun `overall balance calculates total income minus total expenses across all months`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createIncome(id = "tx1", amountMinorUnits = 10000L, date = LocalDate.of(2026, 10, 5)),
                createExpense(id = "tx2", amountMinorUnits = 3500L, date = LocalDate.of(2026, 10, 6)),
                createIncome(id = "tx3", amountMinorUnits = 5000L, date = LocalDate.of(2026, 9, 1)),
                createExpense(id = "tx4", amountMinorUnits = 2000L, date = LocalDate.of(2026, 8, 15)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        // Total income: 15000, Total expense: 5500 => Balance: 9500
        assertEquals(9500L, viewModel.uiState.value.balanceMinorUnits)
    }

    @Test
    fun `overall balance remains unchanged when selected month changes`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createIncome(id = "tx1", amountMinorUnits = 10000L, date = LocalDate.of(2026, 10, 5)),
                createExpense(id = "tx2", amountMinorUnits = 4000L, date = LocalDate.of(2026, 9, 5)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals(6000L, viewModel.uiState.value.balanceMinorUnits)

        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 9)))
        assertEquals(6000L, viewModel.uiState.value.balanceMinorUnits)

        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 1)))
        assertEquals(6000L, viewModel.uiState.value.balanceMinorUnits)
    }

    @Test
    fun `monthly income filters transactions strictly to selected month including month boundaries`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createIncome(id = "tx_oct_first", amountMinorUnits = 1000L, date = LocalDate.of(2026, 10, 1)),
                createIncome(id = "tx_oct_mid", amountMinorUnits = 2000L, date = LocalDate.of(2026, 10, 15)),
                createIncome(id = "tx_oct_last", amountMinorUnits = 3000L, date = LocalDate.of(2026, 10, 31)),
                createIncome(id = "tx_sep_last", amountMinorUnits = 4000L, date = LocalDate.of(2026, 9, 30)),
                createIncome(id = "tx_aug_first", amountMinorUnits = 5000L, date = LocalDate.of(2026, 8, 1)),
                createExpense(id = "tx_oct_exp", amountMinorUnits = 500L, date = LocalDate.of(2026, 10, 15)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        // October income: 1000 + 2000 + 3000 = 6000
        assertEquals(6000L, viewModel.uiState.value.monthlyIncomeMinorUnits)

        // Switch to September 2026: 4000
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 9)))
        assertEquals(4000L, viewModel.uiState.value.monthlyIncomeMinorUnits)

        // Switch to August 2026: 5000
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 8)))
        assertEquals(5000L, viewModel.uiState.value.monthlyIncomeMinorUnits)

        // Switch to July 2026 (no transactions): 0
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 7)))
        assertEquals(0L, viewModel.uiState.value.monthlyIncomeMinorUnits)
    }

    @Test
    fun `monthly expenses filters transactions strictly to selected month including month boundaries`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(
                createExpense(id = "tx_oct_first", amountMinorUnits = 1200L, date = LocalDate.of(2026, 10, 1)),
                createExpense(id = "tx_oct_mid", amountMinorUnits = 800L, date = LocalDate.of(2026, 10, 15)),
                createExpense(id = "tx_oct_last", amountMinorUnits = 500L, date = LocalDate.of(2026, 10, 31)),
                createExpense(id = "tx_sep_last", amountMinorUnits = 1500L, date = LocalDate.of(2026, 9, 30)),
                createExpense(id = "tx_aug_first", amountMinorUnits = 2200L, date = LocalDate.of(2026, 8, 1)),
                createIncome(id = "tx_oct_inc", amountMinorUnits = 5000L, date = LocalDate.of(2026, 10, 15)),
            )
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        // October expenses: 1200 + 800 + 500 = 2500
        assertEquals(2500L, viewModel.uiState.value.monthlyExpensesMinorUnits)

        // Switch to September 2026: 1500
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 9)))
        assertEquals(1500L, viewModel.uiState.value.monthlyExpensesMinorUnits)

        // Switch to August 2026: 2200
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 8)))
        assertEquals(2200L, viewModel.uiState.value.monthlyExpensesMinorUnits)

        // Switch to July 2026 (no transactions): 0
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 7)))
        assertEquals(0L, viewModel.uiState.value.monthlyExpensesMinorUnits)
    }

    @Test
    fun `recent transactions only contains selected-month transactions ordered by date descending then createdAt descending`() = runTest {
        val txOct1 = createExpense(id = "tx1", amountMinorUnits = 100L, date = LocalDate.of(2026, 10, 1), creationInstant = Instant.parse("2026-10-01T10:00:00Z"))
        val txOct5Early = createIncome(id = "tx2", amountMinorUnits = 200L, date = LocalDate.of(2026, 10, 5), creationInstant = Instant.parse("2026-10-05T09:00:00Z"))
        val txOct5Late = createExpense(id = "tx3", amountMinorUnits = 300L, date = LocalDate.of(2026, 10, 5), creationInstant = Instant.parse("2026-10-05T14:00:00Z"))
        val txOct10 = createIncome(id = "tx4", amountMinorUnits = 400L, date = LocalDate.of(2026, 10, 10), creationInstant = Instant.parse("2026-10-10T10:00:00Z"))
        val txSep20 = createIncome(id = "tx5", amountMinorUnits = 500L, date = LocalDate.of(2026, 9, 20), creationInstant = Instant.parse("2026-09-20T10:00:00Z"))

        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(txOct1, txOct5Early, txOct5Late, txOct10, txSep20)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val recent = viewModel.uiState.value.recentTransactions
        assertEquals(4, recent.size)
        // Ordered: txOct10 (Oct 10), txOct5Late (Oct 5, 14:00), txOct5Early (Oct 5, 09:00), txOct1 (Oct 1)
        assertEquals("tx4", recent[0].id)
        assertEquals("tx3", recent[1].id)
        assertEquals("tx2", recent[2].id)
        assertEquals("tx1", recent[3].id)
    }

    @Test
    fun `recent transactions limits results to maximum of five transactions`() = runTest {
        val txs = (1..8).map { i ->
            createExpense(
                id = "tx_$i",
                amountMinorUnits = (i * 100).toLong(),
                date = LocalDate.of(2026, 10, i),
                creationInstant = Instant.parse("2026-10-0${i}T10:00:00Z"),
            )
        }
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = txs
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val recent = viewModel.uiState.value.recentTransactions
        assertEquals(5, recent.size)
        assertEquals("tx_8", recent[0].id)
        assertEquals("tx_7", recent[1].id)
        assertEquals("tx_6", recent[2].id)
        assertEquals("tx_5", recent[3].id)
        assertEquals("tx_4", recent[4].id)
    }

    @Test
    fun `recent transactions maps fields and category labels correctly`() = runTest {
        val tx = createExpense(
            id = "tx_food",
            amountMinorUnits = 4280L,
            categoryId = "expense_food",
            date = LocalDate.of(2026, 10, 5),
            note = "Groceries",
            creationInstant = Instant.parse("2026-10-05T12:00:00Z"),
        )
        val repository = FakeTransactionRepository().apply {
            observedTransactions.value = listOf(tx)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val recent = viewModel.uiState.value.recentTransactions.single()
        assertEquals("tx_food", recent.id)
        assertEquals(4280L, recent.amountMinorUnits)
        assertEquals(CurrencyCode.GBP, recent.currencyCode)
        assertEquals(TransactionType.EXPENSE, recent.type)
        assertEquals("expense_food", recent.categoryId)
        assertEquals("Food", recent.categoryLabel)
        assertEquals(LocalDate.of(2026, 10, 5), recent.date)
        assertEquals("Groceries", recent.note)
    }

    @Test
    fun `empty state distinguishes between no transactions overall and no transactions in selected month`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        // Case A: No transactions in database
        assertTrue(viewModel.uiState.value.hasNoTransactions)
        assertTrue(viewModel.uiState.value.hasNoMonthlyTransactions)

        // Add a transaction in September 2026
        repository.observedTransactions.value = listOf(
            createIncome(
                id = "tx_sep",
                amountMinorUnits = 1000L,
                date = LocalDate.of(2026, 9, 10),
                creationInstant = Instant.parse("2026-09-10T10:00:00Z"),
            ),
        )
        advanceUntilIdle()

        // Selected month is October 2026 -> transactions exist overall, but October is empty
        assertFalse(viewModel.uiState.value.hasNoTransactions)
        assertTrue(viewModel.uiState.value.hasNoMonthlyTransactions)

        // Switch to September 2026 -> transactions exist overall and in September
        viewModel.onEvent(DashboardUiEvent.MonthChanged(YearMonth.of(2026, 9)))
        assertFalse(viewModel.uiState.value.hasNoTransactions)
        assertFalse(viewModel.uiState.value.hasNoMonthlyTransactions)
    }

    @Test
    fun `reactive observation updates calculations when transactions are inserted updated or deleted`() = runTest {
        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        assertEquals(0L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyIncomeMinorUnits)

        // 1. Insert transaction
        val tx1 = createIncome(
            id = "tx1",
            amountMinorUnits = 5000L,
            date = LocalDate.of(2026, 10, 2),
            creationInstant = Instant.parse("2026-10-02T10:00:00Z"),
        )
        repository.observedTransactions.value = listOf(tx1)
        advanceUntilIdle()

        assertEquals(5000L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(5000L, viewModel.uiState.value.monthlyIncomeMinorUnits)
        assertEquals(1, viewModel.uiState.value.recentTransactions.size)

        // 2. Update transaction amount
        val tx1Updated = createIncome(
            id = "tx1",
            amountMinorUnits = 7500L,
            date = LocalDate.of(2026, 10, 2),
            creationInstant = Instant.parse("2026-10-02T10:00:00Z"),
        )
        repository.observedTransactions.value = listOf(tx1Updated)
        advanceUntilIdle()

        assertEquals(7500L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(7500L, viewModel.uiState.value.monthlyIncomeMinorUnits)
        assertEquals(7500L, viewModel.uiState.value.recentTransactions.single().amountMinorUnits)

        // 3. Delete transaction
        repository.observedTransactions.value = emptyList()
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(0L, viewModel.uiState.value.monthlyIncomeMinorUnits)
        assertTrue(viewModel.uiState.value.recentTransactions.isEmpty())
        assertTrue(viewModel.uiState.value.hasNoTransactions)
        assertTrue(viewModel.uiState.value.hasNoMonthlyTransactions)
    }

    @Test
    fun `flow error exposes user-friendly error message and hides technical details`() = runTest {
        val repository = FakeTransactionRepository().apply {
            observedFlows.add(flow { throw IllegalStateException("Fatal SQLite disk I/O error at native level") })
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        val error = viewModel.uiState.value.error
        assertEquals("Couldn't load your dashboard. Please try again.", error?.message)
        assertFalse(error?.message?.contains("SQLite", ignoreCase = true) == true)
        assertFalse(error?.message?.contains("native", ignoreCase = true) == true)
    }

    @Test
    fun `retry restarts observation and recovers on subsequent success`() = runTest {
        val tx = createIncome(
            id = "tx1",
            amountMinorUnits = 5000L,
            date = LocalDate.of(2026, 10, 2),
            creationInstant = Instant.parse("2026-10-02T10:00:00Z"),
        )
        val repository = FakeTransactionRepository().apply {
            observedFlows.add(flow { throw RuntimeException("Network / IO error") })
            observedFlows.add(observedTransactions)
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        assertEquals("Couldn't load your dashboard. Please try again.", viewModel.uiState.value.error?.message)

        repository.observedTransactions.value = listOf(tx)
        viewModel.onEvent(DashboardUiEvent.Retry)

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(5000L, viewModel.uiState.value.balanceMinorUnits)
        assertEquals(5000L, viewModel.uiState.value.monthlyIncomeMinorUnits)
    }

    @Test
    fun `greeting initializes correctly for different times of day`() = runTest {
        val repository = FakeTransactionRepository()

        // 08:00 BST -> Good morning
        val morningClock = Clock.fixed(Instant.parse("2026-10-07T07:00:00Z"), TEST_ZONE)
        val vmMorning = createViewModel(repository, clock = morningClock)
        assertEquals(DashboardGreeting.GOOD_MORNING, vmMorning.uiState.value.greeting)

        // 13:00 BST -> Good afternoon
        val afternoonClock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), TEST_ZONE)
        val vmAfternoon = createViewModel(repository, clock = afternoonClock)
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, vmAfternoon.uiState.value.greeting)

        // 18:00 BST -> Good evening
        val eveningClock = Clock.fixed(Instant.parse("2026-10-07T17:00:00Z"), TEST_ZONE)
        val vmEvening = createViewModel(repository, clock = eveningClock)
        assertEquals(DashboardGreeting.GOOD_EVENING, vmEvening.uiState.value.greeting)

        // 22:00 BST -> Welcome back
        val nightClock = Clock.fixed(Instant.parse("2026-10-07T21:00:00Z"), TEST_ZONE)
        val vmNight = createViewModel(repository, clock = nightClock)
        assertEquals(DashboardGreeting.WELCOME_BACK, vmNight.uiState.value.greeting)
    }

    @Test
    fun `Resume event updates greeting when clock advances across boundary`() = runTest {
        val repository = FakeTransactionRepository()
        // Start at 11:59 BST -> Good morning
        val mutableClock = MutableClock(Instant.parse("2026-10-07T10:59:00Z"))
        val viewModel = createViewModel(repository, clock = mutableClock)

        assertEquals(DashboardGreeting.GOOD_MORNING, viewModel.uiState.value.greeting)

        // Advance to 12:01 BST -> Good afternoon
        mutableClock.advanceTo(Instant.parse("2026-10-07T11:01:00Z"))
        viewModel.onEvent(DashboardUiEvent.Resume)

        assertEquals(DashboardGreeting.GOOD_AFTERNOON, viewModel.uiState.value.greeting)
        viewModel.cancelGreetingSchedule()
    }

    @Test
    fun `greeting timer triggers transition when time elapses to next boundary`() = runTest {
        val repository = FakeTransactionRepository()
        // Start just before the 12:00 BST boundary at 11:59:59 BST (10:59:59 UTC) -> "Good morning"
        // Next boundary is 12:00:00 BST.
        // DashboardGreeting.durationUntilNextBoundary is exactly 1 second.
        // Production delay: duration.toMillis().coerceAtLeast(1000L) + 500L = 1500L ms.
        val mutableClock = MutableClock(Instant.parse("2026-10-07T10:59:59Z"))
        val viewModel = createViewModel(repository, clock = mutableClock, cancelGreetingSchedule = false)

        assertEquals(DashboardGreeting.GOOD_MORNING, viewModel.uiState.value.greeting)

        // Advance the clock past the boundary to 12:00:01 BST (11:00:01 UTC)
        mutableClock.advanceTo(Instant.parse("2026-10-07T11:00:01Z"))

        // Advance virtual coroutine time past the production scheduled delay (1500ms)
        testScheduler.advanceTimeBy(1500L)
        testScheduler.runCurrent()

        // Verify that the timer coroutine woke up, recomputed greeting with current clock, and updated state
        assertEquals(DashboardGreeting.GOOD_AFTERNOON, viewModel.uiState.value.greeting)

        // Cancel the subsequent scheduled timer before test completes to cleanly finish coroutine scope
        viewModel.cancelGreetingSchedule()
    }

    private class MutableClock(
        private var currentInstant: Instant,
        private val zone: ZoneId = TEST_ZONE,
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zoneId: ZoneId): Clock = MutableClock(currentInstant, zoneId)
        override fun instant(): Instant = currentInstant

        fun advanceTo(newInstant: Instant) {
            currentInstant = newInstant
        }
    }

    private fun createViewModel(
        repository: TransactionRepository,
        clock: Clock = Clock.fixed(TEST_INSTANT, TEST_ZONE),
        zoneId: ZoneId = TEST_ZONE,
        cancelGreetingSchedule: Boolean = true,
    ): DashboardViewModel {
        val viewModel = DashboardViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(repository),
            clock = clock,
            zoneId = zoneId,
        )
        if (cancelGreetingSchedule) {
            viewModel.cancelGreetingSchedule()
        }
        return viewModel
    }

    private fun createIncome(
        id: String,
        amountMinorUnits: Long,
        categoryId: String = "income_salary",
        date: LocalDate,
        note: String? = null,
        creationInstant: Instant = Instant.parse("2026-10-01T12:00:00Z"),
    ): Transaction = createTransaction(
        id = id,
        amountMinorUnits = amountMinorUnits,
        type = TransactionType.INCOME,
        categoryId = categoryId,
        date = date,
        note = note,
        creationInstant = creationInstant,
    )

    private fun createExpense(
        id: String,
        amountMinorUnits: Long,
        categoryId: String = "expense_food",
        date: LocalDate,
        note: String? = null,
        creationInstant: Instant = Instant.parse("2026-10-01T12:00:00Z"),
    ): Transaction = createTransaction(
        id = id,
        amountMinorUnits = amountMinorUnits,
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        date = date,
        note = note,
        creationInstant = creationInstant,
    )

    private fun createTransaction(
        id: String,
        amountMinorUnits: Long,
        type: TransactionType,
        categoryId: String,
        date: LocalDate,
        note: String?,
        creationInstant: Instant,
    ): Transaction {
        val clockInstant = if (creationInstant.atZone(TEST_ZONE).toLocalDate().isBefore(date)) {
            date.atTime(12, 0).atZone(TEST_ZONE).toInstant()
        } else {
            creationInstant
        }

        return Transaction.create(
            id = id,
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(amountMinorUnits, CurrencyCode.GBP),
                type = type,
                categoryId = CategoryId.of(categoryId),
                transactionDate = date,
                note = note,
            ),
            clock = Clock.fixed(clockInstant, TEST_ZONE),
            zoneId = TEST_ZONE,
        )
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = mutableMapOf<String, Transaction>()
        val observedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
        val observedFlows = ArrayDeque<Flow<List<Transaction>>>()

        override fun observeTransactions(): Flow<List<Transaction>> =
            if (observedFlows.isEmpty()) {
                observedTransactions
            } else {
                observedFlows.removeFirst()
            }

        override suspend fun getTransaction(id: String): Transaction? = transactions[id]

        override suspend fun insertTransaction(transaction: Transaction) {
            transactions[transaction.id] = transaction
        }

        override suspend fun updateTransaction(transaction: Transaction): Boolean {
            transactions[transaction.id] = transaction
            return true
        }

        override suspend fun deleteTransaction(id: String): Boolean {
            return transactions.remove(id) != null
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
        val TEST_ZONE: ZoneId = ZoneId.of("Europe/London")
        val TEST_INSTANT: Instant = Instant.parse("2026-10-07T12:00:00Z")
    }
}
