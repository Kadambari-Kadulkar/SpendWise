package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.PeriodDateRange
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAnalyticsUseCaseTest {

    private val londonZone = ZoneId.of("Europe/London")
    private val defaultClock = Clock.fixed(
        Instant.parse("2026-10-15T12:00:00Z"),
        londonZone,
    )

    @Test
    fun `empty repository sets all-time empty flags correctly`() = runBlocking {
        val repository = FakeTransactionRepository()
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertTrue(report.hasNoTransactionsOverall)
        assertTrue(report.hasNoPeriodTransactions)
        assertFalse(report.hasCurrencyMismatchInPeriod)
        assertEquals(0L, report.totalIncomeMinorUnits)
        assertEquals(0L, report.totalExpensesMinorUnits)
        assertEquals(0L, report.netCashFlowMinorUnits)
        assertTrue(report.categoryBreakdown.isEmpty())
        assertEquals(0L, report.comparison?.expenseDifferenceMinorUnits)
        assertNull(report.comparison?.percentageDelta)
    }

    @Test
    fun `transactions outside period produce period empty state`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 8, 15),
                        amountMinorUnits = 5000L,
                        type = TransactionType.EXPENSE,
                    )
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertFalse(report.hasNoTransactionsOverall)
        assertTrue(report.hasNoPeriodTransactions)
        assertFalse(report.hasCurrencyMismatchInPeriod)
        assertEquals(0L, report.totalIncomeMinorUnits)
        assertEquals(0L, report.totalExpensesMinorUnits)
    }

    @Test
    fun `transactions in period but mismatched currency produces currency mismatch state`() = runBlocking {
        val eurCurrency = CurrencyCode.of("EUR")
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createRawTransaction(
                        date = LocalDate.of(2026, 10, 10),
                        amountMinorUnits = 5000L,
                        currency = eurCurrency,
                        type = TransactionType.EXPENSE,
                    )
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH, CurrencyCode.GBP).first()

        assertFalse(report.hasNoTransactionsOverall)
        assertTrue(report.hasNoPeriodTransactions)
        assertTrue(report.hasCurrencyMismatchInPeriod)
        assertEquals(0L, report.totalIncomeMinorUnits)
        assertEquals(0L, report.totalExpensesMinorUnits)
    }

    @Test
    fun `mixed currencies in period aggregate only matching currency and set all empty flags false`() = runBlocking {
        val usdCurrency = CurrencyCode.of("USD")
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = 3000L,
                        currency = CurrencyCode.GBP,
                        type = TransactionType.EXPENSE,
                    ),
                    createRawTransaction(
                        date = LocalDate.of(2026, 10, 6),
                        amountMinorUnits = 7000L,
                        currency = usdCurrency,
                        type = TransactionType.EXPENSE,
                    ),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH, CurrencyCode.GBP).first()

        assertFalse(report.hasNoTransactionsOverall)
        assertFalse(report.hasNoPeriodTransactions)
        assertFalse(report.hasCurrencyMismatchInPeriod)
        assertEquals(3000L, report.totalExpensesMinorUnits)
    }

    @Test
    fun `date range boundaries are inclusive and stop at today for THIS_MONTH`() = runBlocking {
        val today = LocalDate.of(2026, 10, 15)
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 9, 30), amountMinorUnits = 100L),
                    createTransaction(date = LocalDate.of(2026, 10, 1), amountMinorUnits = 1000L),
                    createTransaction(date = today, amountMinorUnits = 2000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(PeriodDateRange(LocalDate.of(2026, 10, 1), today), report.dateRange)
        assertEquals(3000L, report.totalExpensesMinorUnits)
    }

    @Test
    fun `LAST_MONTH covers complete previous month and compares with month minus two`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 8, 10), amountMinorUnits = 1500L),
                    createTransaction(date = LocalDate.of(2026, 9, 1), amountMinorUnits = 2000L),
                    createTransaction(date = LocalDate.of(2026, 9, 30), amountMinorUnits = 3000L),
                    createTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 5000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.LAST_MONTH).first()

        assertEquals(PeriodDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), report.dateRange)
        assertEquals(5000L, report.totalExpensesMinorUnits)
        assertEquals(1500L, report.totalExpensesMinorUnits - report.comparison!!.expenseDifferenceMinorUnits)
        assertEquals(3500L, report.comparison!!.expenseDifferenceMinorUnits)
    }

    @Test
    fun `LAST_THREE_MONTHS covers two months prior through today`() = runBlocking {
        val today = LocalDate.of(2026, 10, 15)
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 7, 31), amountMinorUnits = 999L),
                    createTransaction(date = LocalDate.of(2026, 8, 1), amountMinorUnits = 1000L),
                    createTransaction(date = LocalDate.of(2026, 9, 15), amountMinorUnits = 2000L),
                    createTransaction(date = today, amountMinorUnits = 3000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.LAST_THREE_MONTHS).first()

        assertEquals(PeriodDateRange(LocalDate.of(2026, 8, 1), today), report.dateRange)
        assertEquals(6000L, report.totalExpensesMinorUnits)
    }

    @Test
    fun `comparison spending increase computes positive delta and percentage`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 9, 5), amountMinorUnits = 5000L),
                    createTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 10000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(5000L, report.comparison!!.expenseDifferenceMinorUnits)
        assertEquals(BigDecimal("100.0"), report.comparison!!.percentageDelta)
    }

    @Test
    fun `comparison spending decrease computes negative delta and percentage`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 9, 5), amountMinorUnits = 10000L),
                    createTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 5000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(-5000L, report.comparison!!.expenseDifferenceMinorUnits)
        assertEquals(BigDecimal("-50.0"), report.comparison!!.percentageDelta)
    }

    @Test
    fun `comparison spending unchanged computes zero delta and zero percentage`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 9, 5), amountMinorUnits = 5000L),
                    createTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 5000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(0L, report.comparison!!.expenseDifferenceMinorUnits)
        assertEquals(BigDecimal("0.0"), report.comparison!!.percentageDelta)
    }

    @Test
    fun `comparison with zero previous expenses produces null percentageDelta`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 5000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(5000L, report.comparison!!.expenseDifferenceMinorUnits)
        assertNull(report.comparison!!.percentageDelta)
    }

    @Test
    fun `comparison when both periods have zero expenses produces zero difference and null percentageDelta`() = runBlocking {
        val repository = FakeTransactionRepository()
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(0L, report.comparison!!.expenseDifferenceMinorUnits)
        assertNull(report.comparison!!.percentageDelta)
    }

    @Test
    fun `safe BigDecimal rounding adheres to HALF_UP to one decimal place`() = runBlocking {
        val categoryA = CategoryId.of("expense_food")
        val categoryB = CategoryId.of("expense_bills")
        val categoryC = CategoryId.of("expense_transport")

        // 1 / 3 = 33.3%, 2 / 3 = 66.7%
        val repository1 = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 2), amountMinorUnits = 1000L, categoryId = categoryA),
                    createTransaction(date = LocalDate.of(2026, 10, 3), amountMinorUnits = 2000L, categoryId = categoryB),
                )
            )
        }
        val useCase1 = GetAnalyticsUseCase(repository1, defaultClock, londonZone)
        val report1 = useCase1(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(BigDecimal("66.7"), report1.categoryBreakdown[0].percentage)
        assertEquals(BigDecimal("33.3"), report1.categoryBreakdown[1].percentage)

        // 1 / 95 = 1.0526...% -> rounds HALF_UP to 1.1%
        val repository2 = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 2), amountMinorUnits = 100L, categoryId = categoryA),
                    createTransaction(date = LocalDate.of(2026, 10, 3), amountMinorUnits = 9400L, categoryId = categoryC),
                )
            )
        }
        val useCase2 = GetAnalyticsUseCase(repository2, defaultClock, londonZone)
        val report2 = useCase2(AnalyticsPeriod.THIS_MONTH).first()

        val item1 = report2.categoryBreakdown.first { it.categoryId == categoryA }
        assertEquals(BigDecimal("1.1"), item1.percentage)
    }

    @Test
    fun `large Long inputs do not suffer intermediate 64-bit integer overflow during percentage calculation`() = runBlocking {
        val largeAmount = 5_000_000_000_000_000_00L // Exceeds Long.MAX_VALUE / 100
        val totalAmount = 10_000_000_000_000_000_00L
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 2), amountMinorUnits = largeAmount, categoryId = CategoryId.of("expense_food")),
                    createTransaction(date = LocalDate.of(2026, 10, 3), amountMinorUnits = totalAmount - largeAmount, categoryId = CategoryId.of("expense_bills")),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(BigDecimal("50.0"), report.categoryBreakdown[0].percentage)
        assertEquals(BigDecimal("50.0"), report.categoryBreakdown[1].percentage)
    }

    @Test
    fun `trend buckets for THIS_MONTH correctly flags partial current and wholly future buckets`() = runBlocking {
        // Today is October 15, 2026 -> falls into Bucket 3 (15–21)
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 3), amountMinorUnits = 1000L),
                    createTransaction(date = LocalDate.of(2026, 10, 10), amountMinorUnits = 2000L),
                    createTransaction(date = LocalDate.of(2026, 10, 15), amountMinorUnits = 3000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(5, report.trendBuckets.size)

        // Bucket 1 (1–7)
        val b1 = report.trendBuckets[0]
        assertEquals("1–7", b1.label)
        assertEquals(1000L, b1.expenseMinorUnits)
        assertFalse(b1.isPartialCurrent)
        assertFalse(b1.isWhollyFuture)

        // Bucket 2 (8–14)
        val b2 = report.trendBuckets[1]
        assertEquals("8–14", b2.label)
        assertEquals(2000L, b2.expenseMinorUnits)
        assertFalse(b2.isPartialCurrent)
        assertFalse(b2.isWhollyFuture)

        // Bucket 3 (15–21) -> Contains today (Oct 15)
        val b3 = report.trendBuckets[2]
        assertEquals("15–21", b3.label)
        assertEquals(3000L, b3.expenseMinorUnits)
        assertTrue(b3.isPartialCurrent)
        assertFalse(b3.isWhollyFuture)

        // Bucket 4 (22–28) -> Wholly future
        val b4 = report.trendBuckets[3]
        assertEquals("22–28", b4.label)
        assertEquals(0L, b4.expenseMinorUnits)
        assertFalse(b4.isPartialCurrent)
        assertTrue(b4.isWhollyFuture)

        // Bucket 5 (29+) -> Wholly future
        val b5 = report.trendBuckets[4]
        assertEquals("29+", b5.label)
        assertEquals(0L, b5.expenseMinorUnits)
        assertFalse(b5.isPartialCurrent)
        assertTrue(b5.isWhollyFuture)
    }

    @Test
    fun `day one of month trend bucket behavior`() = runBlocking {
        val dayOneClock = Clock.fixed(
            Instant.parse("2026-10-01T10:00:00Z"),
            londonZone,
        )
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 1), amountMinorUnits = 500L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, dayOneClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        val b1 = report.trendBuckets[0]
        assertEquals("1–7", b1.label)
        assertEquals(500L, b1.expenseMinorUnits)
        assertTrue(b1.isPartialCurrent)
        assertFalse(b1.isWhollyFuture)

        for (i in 1..4) {
            assertTrue(report.trendBuckets[i].isWhollyFuture)
            assertFalse(report.trendBuckets[i].isPartialCurrent)
            assertEquals(0L, report.trendBuckets[i].expenseMinorUnits)
        }
    }

    @Test
    fun `month end day trend bucket behavior`() = runBlocking {
        val monthEndClock = Clock.fixed(
            Instant.parse("2026-10-31T20:00:00Z"),
            londonZone,
        )
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 31), amountMinorUnits = 750L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, monthEndClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        for (i in 0..3) {
            assertFalse(report.trendBuckets[i].isPartialCurrent)
            assertFalse(report.trendBuckets[i].isWhollyFuture)
        }

        val b5 = report.trendBuckets[4]
        assertEquals("29+", b5.label)
        assertEquals(750L, b5.expenseMinorUnits)
        assertTrue(b5.isPartialCurrent)
        assertFalse(b5.isWhollyFuture)
    }

    @Test
    fun `non-leap February in LAST_MONTH omits 29+ bucket producing exactly 4 buckets`() = runBlocking {
        // March 2025 -> previous month is February 2025 (28 days)
        val march2025Clock = Clock.fixed(
            Instant.parse("2025-03-10T12:00:00Z"),
            londonZone,
        )
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2025, 2, 5), amountMinorUnits = 100L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, march2025Clock, londonZone)

        val report = useCase(AnalyticsPeriod.LAST_MONTH).first()

        assertEquals(4, report.trendBuckets.size)
        assertEquals("1–7", report.trendBuckets[0].label)
        assertEquals("8–14", report.trendBuckets[1].label)
        assertEquals("15–21", report.trendBuckets[2].label)
        assertEquals("22–28", report.trendBuckets[3].label)
        report.trendBuckets.forEach {
            assertFalse(it.isPartialCurrent)
            assertFalse(it.isWhollyFuture)
        }
    }

    @Test
    fun `leap-year February in LAST_MONTH includes 29+ bucket producing exactly 5 buckets`() = runBlocking {
        // March 2024 -> previous month is February 2024 (29 days, leap year)
        val march2024Clock = Clock.fixed(
            Instant.parse("2024-03-10T12:00:00Z"),
            londonZone,
        )
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2024, 2, 29), amountMinorUnits = 500L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, march2024Clock, londonZone)

        val report = useCase(AnalyticsPeriod.LAST_MONTH).first()

        assertEquals(5, report.trendBuckets.size)
        assertEquals("1–7", report.trendBuckets[0].label)
        assertEquals("8–14", report.trendBuckets[1].label)
        assertEquals("15–21", report.trendBuckets[2].label)
        assertEquals("22–28", report.trendBuckets[3].label)
        assertEquals("29+", report.trendBuckets[4].label)
        assertEquals(500L, report.trendBuckets[4].expenseMinorUnits)
        report.trendBuckets.forEach {
            assertFalse(it.isPartialCurrent)
            assertFalse(it.isWhollyFuture)
        }
    }

    @Test
    fun `LAST_THREE_MONTHS across year boundary generates 3 chronological monthly buckets`() = runBlocking {
        // January 15, 2026 -> 3 months: Nov 2025, Dec 2025, Jan 2026
        val jan2026Clock = Clock.fixed(
            Instant.parse("2026-01-15T12:00:00Z"),
            londonZone,
        )
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2025, 11, 20), amountMinorUnits = 1000L),
                    createTransaction(date = LocalDate.of(2025, 12, 25), amountMinorUnits = 2000L),
                    createTransaction(date = LocalDate.of(2026, 1, 10), amountMinorUnits = 3000L),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, jan2026Clock, londonZone)

        val report = useCase(AnalyticsPeriod.LAST_THREE_MONTHS).first()

        assertEquals(3, report.trendBuckets.size)
        assertEquals("Nov", report.trendBuckets[0].label)
        assertEquals(1000L, report.trendBuckets[0].expenseMinorUnits)
        assertFalse(report.trendBuckets[0].isPartialCurrent)

        assertEquals("Dec", report.trendBuckets[1].label)
        assertEquals(2000L, report.trendBuckets[1].expenseMinorUnits)
        assertFalse(report.trendBuckets[1].isPartialCurrent)

        assertEquals("Jan", report.trendBuckets[2].label)
        assertEquals(3000L, report.trendBuckets[2].expenseMinorUnits)
        assertTrue(report.trendBuckets[2].isPartialCurrent)
    }

    @Test
    fun `income accumulation overflow via Math addExact throws ArithmeticException`() {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = Long.MAX_VALUE - 100L,
                        type = TransactionType.INCOME,
                    ),
                    createTransaction(
                        date = LocalDate.of(2026, 10, 6),
                        amountMinorUnits = 200L,
                        type = TransactionType.INCOME,
                    ),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        assertThrows(ArithmeticException::class.java) {
            runBlocking {
                useCase(AnalyticsPeriod.THIS_MONTH).first()
            }
        }
    }

    @Test
    fun `expense accumulation overflow via Math addExact throws ArithmeticException`() {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = Long.MAX_VALUE - 100L,
                        type = TransactionType.EXPENSE,
                    ),
                    createTransaction(
                        date = LocalDate.of(2026, 10, 6),
                        amountMinorUnits = 200L,
                        type = TransactionType.EXPENSE,
                    ),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        assertThrows(ArithmeticException::class.java) {
            runBlocking {
                useCase(AnalyticsPeriod.THIS_MONTH).first()
            }
        }
    }

    @Test
    fun `category accumulation overflow via Math addExact throws ArithmeticException`() {
        val catId = CategoryId.of("expense_food")
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = Long.MAX_VALUE / 2 + 100L,
                        categoryId = catId,
                        type = TransactionType.EXPENSE,
                    ),
                    createTransaction(
                        date = LocalDate.of(2026, 10, 6),
                        amountMinorUnits = Long.MAX_VALUE / 2 + 100L,
                        categoryId = catId,
                        type = TransactionType.EXPENSE,
                    ),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        assertThrows(ArithmeticException::class.java) {
            runBlocking {
                useCase(AnalyticsPeriod.THIS_MONTH).first()
            }
        }
    }

    @Test
    fun `extreme non-negative totals calculate net cash flow safely without overflow`() = runBlocking {
        // I = Long.MAX_VALUE, E = 0L -> Net = Long.MAX_VALUE
        val repository1 = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = Long.MAX_VALUE,
                        type = TransactionType.INCOME,
                    )
                )
            )
        }
        val useCase1 = GetAnalyticsUseCase(repository1, defaultClock, londonZone)
        val report1 = useCase1(AnalyticsPeriod.THIS_MONTH).first()
        assertEquals(Long.MAX_VALUE, report1.netCashFlowMinorUnits)

        // I = 0L, E = Long.MAX_VALUE -> Net = -Long.MAX_VALUE
        val repository2 = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = Long.MAX_VALUE,
                        type = TransactionType.EXPENSE,
                    )
                )
            )
        }
        val useCase2 = GetAnalyticsUseCase(repository2, defaultClock, londonZone)
        val report2 = useCase2(AnalyticsPeriod.THIS_MONTH).first()
        assertEquals(-Long.MAX_VALUE, report2.netCashFlowMinorUnits)
    }

    @Test
    fun `category breakdown filters to expenses, sorts descending by amount, and preserves categoryId`() = runBlocking {
        val catFood = CategoryId.of("expense_food")
        val catBills = CategoryId.of("expense_bills")
        val catShopping = CategoryId.of("expense_shopping")
        val catCustom = CategoryId.of("expense_custom")

        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(date = LocalDate.of(2026, 10, 2), amountMinorUnits = 1000L, categoryId = catFood, type = TransactionType.EXPENSE),
                    createTransaction(date = LocalDate.of(2026, 10, 3), amountMinorUnits = 5000L, categoryId = catBills, type = TransactionType.EXPENSE),
                    createTransaction(date = LocalDate.of(2026, 10, 4), amountMinorUnits = 2500L, categoryId = catShopping, type = TransactionType.EXPENSE),
                    createRawTransaction(date = LocalDate.of(2026, 10, 5), amountMinorUnits = 1500L, categoryId = catCustom, type = TransactionType.EXPENSE),
                    createTransaction(date = LocalDate.of(2026, 10, 6), amountMinorUnits = 9000L, categoryId = CategoryId.of("income_salary"), type = TransactionType.INCOME),
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(4, report.categoryBreakdown.size)
        assertEquals(catBills, report.categoryBreakdown[0].categoryId)
        assertEquals(5000L, report.categoryBreakdown[0].amountMinorUnits)

        assertEquals(catShopping, report.categoryBreakdown[1].categoryId)
        assertEquals(2500L, report.categoryBreakdown[1].amountMinorUnits)

        assertEquals(catCustom, report.categoryBreakdown[2].categoryId)
        assertEquals(1500L, report.categoryBreakdown[2].amountMinorUnits)

        assertEquals(catFood, report.categoryBreakdown[3].categoryId)
        assertEquals(1000L, report.categoryBreakdown[3].amountMinorUnits)
    }

    @Test
    fun `income-only period produces positive income, zero expenses, and zero category breakdown`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = 5000L,
                        type = TransactionType.INCOME,
                    )
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(5000L, report.totalIncomeMinorUnits)
        assertEquals(0L, report.totalExpensesMinorUnits)
        assertEquals(5000L, report.netCashFlowMinorUnits)
        assertTrue(report.categoryBreakdown.isEmpty())
        assertFalse(report.hasNoTransactionsOverall)
        assertFalse(report.hasNoPeriodTransactions)
    }

    @Test
    fun `expense-only period produces zero income, positive expenses, and negative net cash flow`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            setTransactions(
                listOf(
                    createTransaction(
                        date = LocalDate.of(2026, 10, 5),
                        amountMinorUnits = 4000L,
                        type = TransactionType.EXPENSE,
                    )
                )
            )
        }
        val useCase = GetAnalyticsUseCase(repository, defaultClock, londonZone)

        val report = useCase(AnalyticsPeriod.THIS_MONTH).first()

        assertEquals(0L, report.totalIncomeMinorUnits)
        assertEquals(4000L, report.totalExpensesMinorUnits)
        assertEquals(-4000L, report.netCashFlowMinorUnits)
        assertEquals(1, report.categoryBreakdown.size)
        assertEquals(BigDecimal("100.0"), report.categoryBreakdown[0].percentage)
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
        date: LocalDate,
        amountMinorUnits: Long,
        type: TransactionType = TransactionType.EXPENSE,
        currency: CurrencyCode = CurrencyCode.GBP,
        categoryId: CategoryId = if (type == TransactionType.EXPENSE) CategoryId.of("expense_food") else CategoryId.of("income_salary"),
    ): Transaction {
        val money = createRawMoney(amountMinorUnits, currency)
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
            UUID.randomUUID().toString(),
            money,
            type,
            categoryId.value,
            date,
            null,
            1000L,
            1000L,
        )
    }

    private fun createTransaction(
        date: LocalDate,
        amountMinorUnits: Long,
        type: TransactionType = TransactionType.EXPENSE,
        currency: CurrencyCode = CurrencyCode.GBP,
        categoryId: CategoryId = if (type == TransactionType.EXPENSE) CategoryId.of("expense_food") else CategoryId.of("income_salary"),
        clock: Clock = Clock.fixed(date.atTime(12, 0).atZone(londonZone).toInstant(), londonZone),
    ): Transaction {
        return Transaction.create(
            id = UUID.randomUUID().toString(),
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(amountMinorUnits, currency),
                type = type,
                categoryId = categoryId,
                transactionDate = date,
                note = null,
            ),
            clock = clock,
            zoneId = londonZone,
        )
    }

    private class FakeTransactionRepository : TransactionRepository {
        private val state = MutableStateFlow<List<Transaction>>(emptyList())

        fun setTransactions(transactions: List<Transaction>) {
            state.value = transactions
        }

        override fun observeTransactions(): Flow<List<Transaction>> = state

        override suspend fun getTransaction(id: String): Transaction? =
            state.value.find { it.id == id }

        override suspend fun insertTransaction(transaction: Transaction) {
            state.value = state.value + transaction
        }

        override suspend fun updateTransaction(transaction: Transaction): Boolean {
            val exists = state.value.any { it.id == transaction.id }
            if (exists) {
                state.value = state.value.map { if (it.id == transaction.id) transaction else it }
            }
            return exists
        }

        override suspend fun deleteTransaction(id: String): Boolean {
            val exists = state.value.any { it.id == id }
            if (exists) {
                state.value = state.value.filterNot { it.id == id }
            }
            return exists
        }
    }
}
