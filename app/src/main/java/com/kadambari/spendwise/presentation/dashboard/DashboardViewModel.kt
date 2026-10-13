package com.kadambari.spendwise.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            selectedMonth = currentYearMonth(),
            greeting = currentGreeting(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var greetingJob: Job? = null
    private var currentTransactions: List<Transaction> = emptyList()

    init {
        observeTransactions()
        scheduleNextGreetingUpdate()
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.MonthChanged -> updateSelectedMonth(event.month)
            DashboardUiEvent.Retry -> observeTransactions()
            DashboardUiEvent.Resume -> refreshGreeting()
        }
    }

    private fun observeTransactions() {
        observeJob?.cancel()
        _uiState.update { current ->
            current.copy(
                isLoading = true,
                error = null,
            )
        }

        observeJob = viewModelScope.launch {
            try {
                getTransactionsUseCase().collect { transactions ->
                    currentTransactions = transactions
                    _uiState.value = calculateDashboardState(
                        transactions = transactions,
                        selectedMonth = _uiState.value.selectedMonth,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        error = DashboardUiError("Couldn't load your dashboard. Please try again."),
                    )
                }
            }
        }
    }

    private fun updateSelectedMonth(newMonth: YearMonth) {
        _uiState.value = calculateDashboardState(
            transactions = currentTransactions,
            selectedMonth = newMonth,
            isLoading = _uiState.value.isLoading,
            error = _uiState.value.error,
        )
    }

    private fun calculateDashboardState(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        isLoading: Boolean,
        error: DashboardUiError?,
    ): DashboardUiState {
        var totalIncome = 0L
        var totalExpenses = 0L
        var monthlyIncome = 0L
        var monthlyExpenses = 0L

        val startOfMonth = selectedMonth.atDay(1)
        val endOfMonth = selectedMonth.atEndOfMonth()

        val monthlyTransactions = mutableListOf<Transaction>()

        for (transaction in transactions) {
            val amount = transaction.amount.minorUnits
            when (transaction.type) {
                TransactionType.INCOME -> totalIncome += amount
                TransactionType.EXPENSE -> totalExpenses += amount
            }

            val date = transaction.transactionDate
            if (!date.isBefore(startOfMonth) && !date.isAfter(endOfMonth)) {
                monthlyTransactions.add(transaction)
                when (transaction.type) {
                    TransactionType.INCOME -> monthlyIncome += amount
                    TransactionType.EXPENSE -> monthlyExpenses += amount
                }
            }
        }

        val overallBalance = totalIncome - totalExpenses

        val currencyCode = transactions.firstOrNull()?.amount?.currencyCode ?: CurrencyCode.GBP

        val recentTransactions = monthlyTransactions
            .sortedWith(
                compareByDescending<Transaction> { it.transactionDate }
                    .thenByDescending { it.createdAtEpochMillis },
            )
            .take(5)
            .map(::toRecentTransactionUiModel)

        return DashboardUiState(
            selectedMonth = selectedMonth,
            greeting = currentGreeting(),
            balanceMinorUnits = overallBalance,
            monthlyIncomeMinorUnits = monthlyIncome,
            monthlyExpensesMinorUnits = monthlyExpenses,
            currencyCode = currencyCode,
            recentTransactions = recentTransactions,
            isLoading = isLoading,
            error = error,
            hasNoTransactions = transactions.isEmpty(),
            hasNoMonthlyTransactions = monthlyTransactions.isEmpty(),
        )
    }

    private fun refreshGreeting() {
        val newGreeting = currentGreeting()
        if (_uiState.value.greeting != newGreeting) {
            _uiState.update { it.copy(greeting = newGreeting) }
        }
        scheduleNextGreetingUpdate()
    }

    private fun scheduleNextGreetingUpdate() {
        greetingJob?.cancel()
        greetingJob = viewModelScope.launch {
            val now = clock.instant().atZone(zoneId)
            val delayMillis = DashboardGreeting.durationUntilNextBoundary(now)
                .toMillis()
                .coerceAtLeast(1000L) + 500L
            delay(delayMillis)
            refreshGreeting()
        }
    }

    /**
     * Cancels the periodic background greeting timer.
     * Used in unit tests where [scheduleNextGreetingUpdate] would otherwise schedule indefinite future delays
     * preventing [kotlinx.coroutines.test.TestCoroutineScheduler.advanceUntilIdle] from completing.
     */
    internal fun cancelGreetingSchedule() {
        greetingJob?.cancel()
        greetingJob = null
    }

    private fun currentGreeting(): String =
        DashboardGreeting.forClock(clock, zoneId)

    private fun currentYearMonth(): YearMonth {
        val today = clock.instant().atZone(zoneId).toLocalDate()
        return YearMonth.from(today)
    }

    private fun toRecentTransactionUiModel(transaction: Transaction): DashboardRecentTransactionUiModel =
        DashboardRecentTransactionUiModel(
            id = transaction.id,
            amountMinorUnits = transaction.amount.minorUnits,
            currencyCode = transaction.amount.currencyCode,
            type = transaction.type,
            categoryId = transaction.categoryId.value,
            categoryLabel = CategoryCatalogue.find(transaction.categoryId)?.displayName
                ?: transaction.categoryId.value,
            date = transaction.transactionDate,
            note = transaction.note,
        )
}
