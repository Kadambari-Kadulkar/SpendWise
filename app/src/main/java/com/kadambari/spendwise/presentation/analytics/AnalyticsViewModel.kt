package com.kadambari.spendwise.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.domain.model.AnalyticsReport
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.PeriodComparison
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.usecase.GetAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AnalyticsUiState(
            selectedPeriod = AnalyticsPeriod.THIS_MONTH,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeAnalytics(AnalyticsPeriod.THIS_MONTH)
    }

    fun onEvent(event: AnalyticsUiEvent) {
        when (event) {
            is AnalyticsUiEvent.PeriodSelected -> onPeriodSelected(event.period)
            AnalyticsUiEvent.Retry -> onRetry()
        }
    }

    private fun onPeriodSelected(newPeriod: AnalyticsPeriod) {
        val current = _uiState.value
        if (newPeriod == current.selectedPeriod && !current.isLoading && current.error == null) {
            return
        }
        observeAnalytics(newPeriod)
    }

    private fun onRetry() {
        observeAnalytics(_uiState.value.selectedPeriod)
    }

    private fun observeAnalytics(period: AnalyticsPeriod) {
        observeJob?.cancel()

        _uiState.value = AnalyticsUiState(
            selectedPeriod = period,
            isLoading = true,
            error = null,
        )

        observeJob = viewModelScope.launch {
            try {
                getAnalyticsUseCase(
                    period = period,
                    reportingCurrency = CurrencyCode.GBP,
                ).collect { report ->
                    _uiState.value = toUiState(report)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        error = AnalyticsUiError("Couldn't load your analytics. Please try again."),
                    )
                }
            }
        }
    }

    private fun toUiState(report: AnalyticsReport): AnalyticsUiState {
        val emptyType = when {
            report.hasNoTransactionsOverall -> AnalyticsEmptyType.NO_TRANSACTIONS_OVERALL
            report.hasCurrencyMismatchInPeriod -> AnalyticsEmptyType.CURRENCY_MISMATCH
            report.hasNoPeriodTransactions -> AnalyticsEmptyType.NO_PERIOD_TRANSACTIONS
            else -> AnalyticsEmptyType.NONE
        }

        val categoryBreakdown = report.categoryBreakdown.map { item ->
            val categoryId = item.categoryId.value
            val categoryLabel = CategoryCatalogue.find(item.categoryId)?.displayName ?: categoryId
            val progressRatio = (item.percentage.toFloat() / 100f).coerceIn(0f, 1f)

            CategorySpendingUiModel(
                categoryId = categoryId,
                categoryLabel = categoryLabel,
                amountMinorUnits = item.amountMinorUnits,
                amountFormatted = AnalyticsAmountFormatter.formatAmount(item.amountMinorUnits, report.currencyCode),
                percentage = item.percentage,
                percentageFormatted = AnalyticsAmountFormatter.formatPercentage(item.percentage),
                progressRatio = progressRatio,
            )
        }

        val trendBuckets = report.trendBuckets.map { bucket ->
            TrendBucketUiModel(
                label = bucket.label,
                incomeMinorUnits = bucket.incomeMinorUnits,
                expenseMinorUnits = bucket.expenseMinorUnits,
                incomeFormatted = AnalyticsAmountFormatter.formatAmount(bucket.incomeMinorUnits, report.currencyCode),
                expenseFormatted = AnalyticsAmountFormatter.formatAmount(bucket.expenseMinorUnits, report.currencyCode),
                isWhollyFuture = bucket.isWhollyFuture,
                isPartialCurrent = bucket.isPartialCurrent,
            )
        }

        val comparisonUiModel = report.comparison?.let { comparison ->
            toComparisonUiModel(comparison, report.period, report.currencyCode)
        }

        return AnalyticsUiState(
            selectedPeriod = report.period,
            periodDateRangeFormatted = AnalyticsAmountFormatter.formatDateRange(
                startDate = report.dateRange.startDate,
                endDate = report.dateRange.endDate,
            ),
            totalIncomeMinorUnits = report.totalIncomeMinorUnits,
            totalIncomeFormatted = AnalyticsAmountFormatter.formatSignedTotal(
                amountMinorUnits = report.totalIncomeMinorUnits,
                currencyCode = report.currencyCode,
                type = TransactionType.INCOME,
            ),
            totalExpensesMinorUnits = report.totalExpensesMinorUnits,
            totalExpensesFormatted = AnalyticsAmountFormatter.formatSignedTotal(
                amountMinorUnits = report.totalExpensesMinorUnits,
                currencyCode = report.currencyCode,
                type = TransactionType.EXPENSE,
            ),
            netCashFlowMinorUnits = report.netCashFlowMinorUnits,
            netCashFlowFormatted = AnalyticsAmountFormatter.formatNetCashFlow(
                amountMinorUnits = report.netCashFlowMinorUnits,
                currencyCode = report.currencyCode,
            ),
            currencyCode = report.currencyCode,
            categoryBreakdown = categoryBreakdown,
            trendBuckets = trendBuckets,
            comparison = comparisonUiModel,
            isLoading = false,
            error = null,
            emptyType = emptyType,
        )
    }

    private fun toComparisonUiModel(
        comparison: PeriodComparison,
        period: AnalyticsPeriod,
        currencyCode: CurrencyCode,
    ): ComparisonUiModel {
        val baselineDescription = when (period) {
            AnalyticsPeriod.THIS_MONTH -> "vs same period last month"
            AnalyticsPeriod.LAST_MONTH -> "vs previous month"
            AnalyticsPeriod.LAST_THREE_MONTHS -> "vs previous 3 months"
        }

        val diff = comparison.expenseDifferenceMinorUnits
        val differenceFormatted = AnalyticsAmountFormatter.formatExpenseDifference(diff, currencyCode)
        val absAmountFormatted = AnalyticsAmountFormatter.formatAmount(diff, currencyCode)
        val delta = comparison.percentageDelta
        val deltaFormatted = AnalyticsAmountFormatter.formatPercentageDelta(delta)

        val isIncrease = diff > 0L
        val isDecrease = diff < 0L
        val isUnchanged = diff == 0L

        val summaryText = when {
            diff == 0L && delta == null -> {
                "No expenses recorded in this period or the previous period."
            }
            diff > 0L && delta == null -> {
                "Spent $absAmountFormatted (no expenses recorded in previous period)."
            }
            diff < 0L && delta != null -> {
                "Spent $absAmountFormatted less than previous period ($deltaFormatted)."
            }
            diff > 0L && delta != null -> {
                "Spent $absAmountFormatted more than previous period ($deltaFormatted)."
            }
            else -> {
                "Spending unchanged from previous period (0.0%)."
            }
        }

        return ComparisonUiModel(
            baselineDescription = baselineDescription,
            expenseDifferenceMinorUnits = diff,
            differenceFormatted = differenceFormatted,
            percentageDelta = delta,
            percentageDeltaFormatted = deltaFormatted,
            isExpenseIncrease = isIncrease,
            isExpenseDecrease = isDecrease,
            isUnchanged = isUnchanged,
            summaryText = summaryText,
        )
    }
}
