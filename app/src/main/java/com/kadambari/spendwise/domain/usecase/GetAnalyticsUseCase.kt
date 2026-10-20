package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.domain.model.AnalyticsReport
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CategorySpending
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.PeriodComparison
import com.kadambari.spendwise.domain.model.PeriodDateRange
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.model.TrendBucket
import com.kadambari.spendwise.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAnalyticsUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {

    operator fun invoke(
        period: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
        reportingCurrency: CurrencyCode = CurrencyCode.GBP,
    ): Flow<AnalyticsReport> {
        return repository.observeTransactions().map { allTransactions ->
            val today = clock.instant().atZone(zoneId).toLocalDate()
            val currentMonth = YearMonth.from(today)

            val selectedDateRange = resolveSelectedPeriodDateRange(period, today, currentMonth)
            val comparisonDateRange = resolveComparisonDateRange(period, today, currentMonth)

            val hasNoTransactionsOverall = allTransactions.isEmpty()
            val transactionsInPeriod = allTransactions.filter { it.isWithin(selectedDateRange) }
            val matchingCurrencyInPeriod = transactionsInPeriod.filter { it.amount.currencyCode == reportingCurrency }
            val hasNoPeriodTransactions = matchingCurrencyInPeriod.isEmpty()
            val hasCurrencyMismatchInPeriod = transactionsInPeriod.isNotEmpty() && matchingCurrencyInPeriod.isEmpty()

            var totalIncomeMinorUnits = 0L
            var totalExpensesMinorUnits = 0L

            for (tx in matchingCurrencyInPeriod) {
                when (tx.type) {
                    TransactionType.INCOME -> {
                        totalIncomeMinorUnits = Math.addExact(totalIncomeMinorUnits, tx.amount.minorUnits)
                    }
                    TransactionType.EXPENSE -> {
                        totalExpensesMinorUnits = Math.addExact(totalExpensesMinorUnits, tx.amount.minorUnits)
                    }
                }
            }

            val netCashFlowMinorUnits = totalIncomeMinorUnits - totalExpensesMinorUnits

            val categoryBreakdown = computeCategoryBreakdown(
                matchingTransactions = matchingCurrencyInPeriod,
                totalExpensesMinorUnits = totalExpensesMinorUnits,
            )

            val comparison = computePeriodComparison(
                allTransactions = allTransactions,
                comparisonDateRange = comparisonDateRange,
                reportingCurrency = reportingCurrency,
                totalExpensesMinorUnits = totalExpensesMinorUnits,
            )

            val trendBuckets = buildTrendBuckets(
                period = period,
                today = today,
                currentMonth = currentMonth,
                matchingTransactions = matchingCurrencyInPeriod,
            )

            AnalyticsReport(
                period = period,
                dateRange = selectedDateRange,
                totalIncomeMinorUnits = totalIncomeMinorUnits,
                totalExpensesMinorUnits = totalExpensesMinorUnits,
                netCashFlowMinorUnits = netCashFlowMinorUnits,
                categoryBreakdown = categoryBreakdown,
                trendBuckets = trendBuckets,
                comparison = comparison,
                currencyCode = reportingCurrency,
                hasNoTransactionsOverall = hasNoTransactionsOverall,
                hasNoPeriodTransactions = hasNoPeriodTransactions,
                hasCurrencyMismatchInPeriod = hasCurrencyMismatchInPeriod,
            )
        }
    }

    private fun resolveSelectedPeriodDateRange(
        period: AnalyticsPeriod,
        today: LocalDate,
        currentMonth: YearMonth,
    ): PeriodDateRange {
        return when (period) {
            AnalyticsPeriod.THIS_MONTH -> {
                PeriodDateRange(
                    startDate = currentMonth.atDay(1),
                    endDate = today,
                )
            }
            AnalyticsPeriod.LAST_MONTH -> {
                val previousMonth = currentMonth.minusMonths(1)
                PeriodDateRange(
                    startDate = previousMonth.atDay(1),
                    endDate = previousMonth.atEndOfMonth(),
                )
            }
            AnalyticsPeriod.LAST_THREE_MONTHS -> {
                PeriodDateRange(
                    startDate = currentMonth.minusMonths(2).atDay(1),
                    endDate = today,
                )
            }
        }
    }

    private fun resolveComparisonDateRange(
        period: AnalyticsPeriod,
        today: LocalDate,
        currentMonth: YearMonth,
    ): PeriodDateRange {
        return when (period) {
            AnalyticsPeriod.THIS_MONTH -> {
                val previousMonth = currentMonth.minusMonths(1)
                val endDay = minOf(today.dayOfMonth, previousMonth.lengthOfMonth())
                PeriodDateRange(
                    startDate = previousMonth.atDay(1),
                    endDate = previousMonth.atDay(endDay),
                )
            }
            AnalyticsPeriod.LAST_MONTH -> {
                val twoMonthsAgo = currentMonth.minusMonths(2)
                PeriodDateRange(
                    startDate = twoMonthsAgo.atDay(1),
                    endDate = twoMonthsAgo.atEndOfMonth(),
                )
            }
            AnalyticsPeriod.LAST_THREE_MONTHS -> {
                val fiveMonthsAgo = currentMonth.minusMonths(5)
                val threeMonthsAgo = currentMonth.minusMonths(3)
                PeriodDateRange(
                    startDate = fiveMonthsAgo.atDay(1),
                    endDate = threeMonthsAgo.atEndOfMonth(),
                )
            }
        }
    }

    private fun computeCategoryBreakdown(
        matchingTransactions: List<Transaction>,
        totalExpensesMinorUnits: Long,
    ): List<CategorySpending> {
        val categoryTotals = mutableMapOf<CategoryId, Long>()
        for (tx in matchingTransactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val current = categoryTotals.getOrDefault(tx.categoryId, 0L)
                categoryTotals[tx.categoryId] = Math.addExact(current, tx.amount.minorUnits)
            }
        }

        return categoryTotals.entries
            .sortedWith(
                compareByDescending<Map.Entry<CategoryId, Long>> { it.value }
                    .thenBy { it.key.value }
            )
            .map { (categoryId, amountMinorUnits) ->
                val percentage = if (totalExpensesMinorUnits == 0L) {
                    BigDecimal.ZERO.setScale(1)
                } else {
                    BigDecimal.valueOf(amountMinorUnits)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalExpensesMinorUnits), 1, RoundingMode.HALF_UP)
                }
                CategorySpending(
                    categoryId = categoryId,
                    amountMinorUnits = amountMinorUnits,
                    percentage = percentage,
                )
            }
    }

    private fun computePeriodComparison(
        allTransactions: List<Transaction>,
        comparisonDateRange: PeriodDateRange,
        reportingCurrency: CurrencyCode,
        totalExpensesMinorUnits: Long,
    ): PeriodComparison {
        var comparisonExpensesMinorUnits = 0L
        for (tx in allTransactions) {
            if (tx.isWithin(comparisonDateRange) &&
                tx.amount.currencyCode == reportingCurrency &&
                tx.type == TransactionType.EXPENSE
            ) {
                comparisonExpensesMinorUnits = Math.addExact(comparisonExpensesMinorUnits, tx.amount.minorUnits)
            }
        }

        val expenseDifferenceMinorUnits = totalExpensesMinorUnits - comparisonExpensesMinorUnits

        val percentageDelta = if (comparisonExpensesMinorUnits == 0L) {
            null
        } else {
            BigDecimal.valueOf(expenseDifferenceMinorUnits)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(comparisonExpensesMinorUnits), 1, RoundingMode.HALF_UP)
        }

        return PeriodComparison(
            expenseDifferenceMinorUnits = expenseDifferenceMinorUnits,
            percentageDelta = percentageDelta,
        )
    }

    private fun buildTrendBuckets(
        period: AnalyticsPeriod,
        today: LocalDate,
        currentMonth: YearMonth,
        matchingTransactions: List<Transaction>,
    ): List<TrendBucket> {
        return when (period) {
            AnalyticsPeriod.THIS_MONTH -> {
                val monthLength = currentMonth.lengthOfMonth()
                val dayRanges = buildDayRanges(monthLength)
                dayRanges.map { (startDay, endDay, label) ->
                    val bucketStart = currentMonth.atDay(startDay)
                    val bucketEnd = currentMonth.atDay(endDay)
                    val isWhollyFuture = today.dayOfMonth < startDay
                    val isPartialCurrent = today.dayOfMonth in startDay..endDay

                    val (income, expense) = if (isWhollyFuture) {
                        0L to 0L
                    } else {
                        aggregateBucketTransactions(matchingTransactions, bucketStart, bucketEnd)
                    }

                    TrendBucket(
                        label = label,
                        incomeMinorUnits = income,
                        expenseMinorUnits = expense,
                        isWhollyFuture = isWhollyFuture,
                        isPartialCurrent = isPartialCurrent,
                    )
                }
            }
            AnalyticsPeriod.LAST_MONTH -> {
                val previousMonth = currentMonth.minusMonths(1)
                val monthLength = previousMonth.lengthOfMonth()
                val dayRanges = buildDayRanges(monthLength)
                dayRanges.map { (startDay, endDay, label) ->
                    val bucketStart = previousMonth.atDay(startDay)
                    val bucketEnd = previousMonth.atDay(endDay)
                    val (income, expense) = aggregateBucketTransactions(matchingTransactions, bucketStart, bucketEnd)

                    TrendBucket(
                        label = label,
                        incomeMinorUnits = income,
                        expenseMinorUnits = expense,
                        isWhollyFuture = false,
                        isPartialCurrent = false,
                    )
                }
            }
            AnalyticsPeriod.LAST_THREE_MONTHS -> {
                val months = listOf(
                    currentMonth.minusMonths(2),
                    currentMonth.minusMonths(1),
                    currentMonth,
                )
                val formatter = DateTimeFormatter.ofPattern("MMM", Locale.UK)
                months.map { month ->
                    val bucketStart = month.atDay(1)
                    val bucketEnd = month.atEndOfMonth()
                    val isPartialCurrent = (month == currentMonth)
                    val (income, expense) = aggregateBucketTransactions(matchingTransactions, bucketStart, bucketEnd)

                    TrendBucket(
                        label = month.format(formatter),
                        incomeMinorUnits = income,
                        expenseMinorUnits = expense,
                        isWhollyFuture = false,
                        isPartialCurrent = isPartialCurrent,
                    )
                }
            }
        }
    }

    private fun buildDayRanges(monthLength: Int): List<Triple<Int, Int, String>> {
        val ranges = mutableListOf(
            Triple(1, 7, "1–7"),
            Triple(8, 14, "8–14"),
            Triple(15, 21, "15–21"),
            Triple(22, 28, "22–28"),
        )
        if (monthLength >= 29) {
            ranges.add(Triple(29, monthLength, "29+"))
        }
        return ranges
    }

    private fun aggregateBucketTransactions(
        transactions: List<Transaction>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Pair<Long, Long> {
        var income = 0L
        var expense = 0L
        for (tx in transactions) {
            if (!tx.transactionDate.isBefore(startDate) && !tx.transactionDate.isAfter(endDate)) {
                when (tx.type) {
                    TransactionType.INCOME -> income = Math.addExact(income, tx.amount.minorUnits)
                    TransactionType.EXPENSE -> expense = Math.addExact(expense, tx.amount.minorUnits)
                }
            }
        }
        return income to expense
    }

    private fun Transaction.isWithin(range: PeriodDateRange): Boolean {
        return !transactionDate.isBefore(range.startDate) && !transactionDate.isAfter(range.endDate)
    }
}
