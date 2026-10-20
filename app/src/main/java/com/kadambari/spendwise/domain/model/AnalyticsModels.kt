package com.kadambari.spendwise.domain.model

import java.math.BigDecimal
import java.time.LocalDate

enum class AnalyticsPeriod {
    THIS_MONTH,
    LAST_MONTH,
    LAST_THREE_MONTHS
}

data class PeriodDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class CategorySpending(
    val categoryId: CategoryId,
    val amountMinorUnits: Long,
    val percentage: BigDecimal
)

data class TrendBucket(
    val label: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long,
    val isWhollyFuture: Boolean = false,
    val isPartialCurrent: Boolean = false
)

data class PeriodComparison(
    val expenseDifferenceMinorUnits: Long,
    val percentageDelta: BigDecimal?
)

data class AnalyticsReport(
    val period: AnalyticsPeriod,
    val dateRange: PeriodDateRange,
    val totalIncomeMinorUnits: Long,
    val totalExpensesMinorUnits: Long,
    val netCashFlowMinorUnits: Long,
    val categoryBreakdown: List<CategorySpending>,
    val trendBuckets: List<TrendBucket>,
    val comparison: PeriodComparison?,
    val currencyCode: CurrencyCode,
    val hasNoTransactionsOverall: Boolean,
    val hasNoPeriodTransactions: Boolean,
    val hasCurrencyMismatchInPeriod: Boolean
)
