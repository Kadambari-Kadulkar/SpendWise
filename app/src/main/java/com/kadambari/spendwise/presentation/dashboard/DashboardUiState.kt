package com.kadambari.spendwise.presentation.dashboard

import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Presentation state for the SpendWise Dashboard.
 *
 * Exposes overall financial balance, selected-month totals, recent transactions,
 * loading/error state, and explicit empty-state indicators.
 */
data class DashboardUiState(
    val selectedMonth: YearMonth,
    val greeting: String = DashboardGreeting.GOOD_MORNING,
    val balanceMinorUnits: Long = 0L,
    val monthlyIncomeMinorUnits: Long = 0L,
    val monthlyExpensesMinorUnits: Long = 0L,
    val currencyCode: CurrencyCode = CurrencyCode.GBP,
    val recentTransactions: List<DashboardRecentTransactionUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: DashboardUiError? = null,
    val hasNoTransactions: Boolean = true,
    val hasNoMonthlyTransactions: Boolean = true,
)

/**
 * Presentation model for a recent transaction item displayed on the Dashboard.
 */
data class DashboardRecentTransactionUiModel(
    val id: String,
    val amountMinorUnits: Long,
    val currencyCode: CurrencyCode,
    val type: TransactionType,
    val categoryId: String,
    val categoryLabel: String,
    val date: LocalDate,
    val note: String?,
)

/**
 * User-facing error representation for dashboard operations.
 */
data class DashboardUiError(
    val message: String,
)
