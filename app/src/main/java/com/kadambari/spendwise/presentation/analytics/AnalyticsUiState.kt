package com.kadambari.spendwise.presentation.analytics

import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.domain.model.CurrencyCode
import java.math.BigDecimal

/**
 * Three-tier empty state classification for SpendWise Analytics.
 *
 * Evaluated in strict order of specificity:
 * 1. [NO_TRANSACTIONS_OVERALL]: No transactions recorded across the entire application.
 * 2. [CURRENCY_MISMATCH]: Transactions exist in the period, but none in the reporting currency (GBP).
 * 3. [NO_PERIOD_TRANSACTIONS]: No transactions exist within the selected period's date range.
 * 4. [NONE]: Populated state with matching transactions.
 */
enum class AnalyticsEmptyType {
    NONE,
    NO_TRANSACTIONS_OVERALL,
    CURRENCY_MISMATCH,
    NO_PERIOD_TRANSACTIONS,
}

/**
 * Presentation error representation for Analytics operations.
 */
data class AnalyticsUiError(
    val message: String,
)

/**
 * Presentation model for a single category's spending in the selected period.
 */
data class CategorySpendingUiModel(
    val categoryId: String,
    val categoryLabel: String,
    val amountMinorUnits: Long,
    val amountFormatted: String,
    val percentage: BigDecimal,
    val percentageFormatted: String,
    val progressRatio: Float,
)

/**
 * Presentation model for an income/expense trend bucket along the chronological chart axis.
 */
data class TrendBucketUiModel(
    val label: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long,
    val incomeFormatted: String,
    val expenseFormatted: String,
    val isWhollyFuture: Boolean = false,
    val isPartialCurrent: Boolean = false,
)

/**
 * Presentation model for comparing expenses against the preceding comparison baseline.
 */
data class ComparisonUiModel(
    val baselineDescription: String,
    val expenseDifferenceMinorUnits: Long,
    val differenceFormatted: String,
    val percentageDelta: BigDecimal?,
    val percentageDeltaFormatted: String?,
    val isExpenseIncrease: Boolean,
    val isExpenseDecrease: Boolean,
    val isUnchanged: Boolean,
    val summaryText: String,
)

/**
 * Immutable UI state for the SpendWise Analytics screen.
 */
data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val periodDateRangeFormatted: String = "",
    val totalIncomeMinorUnits: Long = 0L,
    val totalIncomeFormatted: String = "",
    val totalExpensesMinorUnits: Long = 0L,
    val totalExpensesFormatted: String = "",
    val netCashFlowMinorUnits: Long = 0L,
    val netCashFlowFormatted: String = "",
    val currencyCode: CurrencyCode = CurrencyCode.GBP,
    val categoryBreakdown: List<CategorySpendingUiModel> = emptyList(),
    val trendBuckets: List<TrendBucketUiModel> = emptyList(),
    val comparison: ComparisonUiModel? = null,
    val isLoading: Boolean = true,
    val error: AnalyticsUiError? = null,
    val emptyType: AnalyticsEmptyType = AnalyticsEmptyType.NONE,
)
