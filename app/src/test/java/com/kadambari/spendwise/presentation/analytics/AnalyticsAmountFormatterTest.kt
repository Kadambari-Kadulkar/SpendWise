package com.kadambari.spendwise.presentation.analytics

import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsAmountFormatterTest {

    @Test
    fun `formatNetCashFlow formats positive cash flow with plus sign`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = 125000L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("+£1,250.00", result)
    }

    @Test
    fun `formatNetCashFlow formats small positive cash flow with plus sign`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = 1L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("+£0.01", result)
    }

    @Test
    fun `formatNetCashFlow formats negative cash flow with minus sign`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = -4550L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("-£45.50", result)
    }

    @Test
    fun `formatNetCashFlow formats large negative cash flow with grouping separators`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = -100000000L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("-£1,000,000.00", result)
    }

    @Test
    fun `formatNetCashFlow formats zero cash flow neutrally without sign`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£0.00", result)
    }

    @Test
    fun `formatNetCashFlow safely formats Long MIN_VALUE without overflow`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = Long.MIN_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("-£92,233,720,368,547,758.08", result)
    }

    @Test
    fun `formatNetCashFlow safely formats Long MAX_VALUE without overflow`() {
        val result = AnalyticsAmountFormatter.formatNetCashFlow(
            amountMinorUnits = Long.MAX_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("+£92,233,720,368,547,758.07", result)
    }

    @Test
    fun `formatSignedTotal formats income with plus sign`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = 350000L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.INCOME,
            locale = Locale.UK,
        )
        assertEquals("+£3,500.00", result)
    }

    @Test
    fun `formatSignedTotal formats zero income with plus sign`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.INCOME,
            locale = Locale.UK,
        )
        assertEquals("+£0.00", result)
    }

    @Test
    fun `formatSignedTotal formats expense with minus sign`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = 225000L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
            locale = Locale.UK,
        )
        assertEquals("-£2,250.00", result)
    }

    @Test
    fun `formatSignedTotal formats zero expense with minus sign`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
            locale = Locale.UK,
        )
        assertEquals("-£0.00", result)
    }

    @Test
    fun `formatSignedTotal safely takes absolute value of negative minor units`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = -5000L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
            locale = Locale.UK,
        )
        assertEquals("-£50.00", result)
    }

    @Test
    fun `formatSignedTotal safely formats Long MIN_VALUE expense without overflow`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = Long.MIN_VALUE,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
            locale = Locale.UK,
        )
        assertEquals("-£92,233,720,368,547,758.08", result)
    }

    @Test
    fun `formatSignedTotal safely formats Long MIN_VALUE income without overflow`() {
        val result = AnalyticsAmountFormatter.formatSignedTotal(
            amountMinorUnits = Long.MIN_VALUE,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.INCOME,
            locale = Locale.UK,
        )
        assertEquals("+£92,233,720,368,547,758.08", result)
    }

    @Test
    fun `formatExpenseDifference formats positive difference as increased expenses with plus sign`() {
        val result = AnalyticsAmountFormatter.formatExpenseDifference(
            differenceMinorUnits = 4550L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("+£45.50", result)
    }

    @Test
    fun `formatExpenseDifference formats negative difference as decreased expenses with minus sign`() {
        val result = AnalyticsAmountFormatter.formatExpenseDifference(
            differenceMinorUnits = -12000L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("-£120.00", result)
    }

    @Test
    fun `formatExpenseDifference formats zero difference neutrally without sign`() {
        val result = AnalyticsAmountFormatter.formatExpenseDifference(
            differenceMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£0.00", result)
    }

    @Test
    fun `formatExpenseDifference safely formats Long MIN_VALUE difference without overflow`() {
        val result = AnalyticsAmountFormatter.formatExpenseDifference(
            differenceMinorUnits = Long.MIN_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("-£92,233,720,368,547,758.08", result)
    }

    @Test
    fun `formatExpenseDifference safely formats Long MAX_VALUE difference without overflow`() {
        val result = AnalyticsAmountFormatter.formatExpenseDifference(
            differenceMinorUnits = Long.MAX_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("+£92,233,720,368,547,758.07", result)
    }

    @Test
    fun `formatAmount formats unsigned monetary amounts with grouping`() {
        val result = AnalyticsAmountFormatter.formatAmount(
            amountMinorUnits = 123456789L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£1,234,567.89", result)
    }

    @Test
    fun `formatAmount formats zero unsigned amount`() {
        val result = AnalyticsAmountFormatter.formatAmount(
            amountMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£0.00", result)
    }

    @Test
    fun `formatAmount safely formats Long MIN_VALUE without overflow`() {
        val result = AnalyticsAmountFormatter.formatAmount(
            amountMinorUnits = Long.MIN_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£92,233,720,368,547,758.08", result)
    }

    @Test
    fun `formatAmount safely formats Long MAX_VALUE without overflow`() {
        val result = AnalyticsAmountFormatter.formatAmount(
            amountMinorUnits = Long.MAX_VALUE,
            currencyCode = CurrencyCode.GBP,
            locale = Locale.UK,
        )
        assertEquals("£92,233,720,368,547,758.07", result)
    }

    @Test
    fun `formatPercentage formats proportion with one decimal place and percent symbol`() {
        val result = AnalyticsAmountFormatter.formatPercentage(
            percentage = BigDecimal("27.6"),
            locale = Locale.UK,
        )
        assertEquals("27.6%", result)
    }

    @Test
    fun `formatPercentage formats zero percentage`() {
        val result = AnalyticsAmountFormatter.formatPercentage(
            percentage = BigDecimal("0.0"),
            locale = Locale.UK,
        )
        assertEquals("0.0%", result)
    }

    @Test
    fun `formatPercentage formats 100 percent`() {
        val result = AnalyticsAmountFormatter.formatPercentage(
            percentage = BigDecimal("100.0"),
            locale = Locale.UK,
        )
        assertEquals("100.0%", result)
    }

    @Test
    fun `formatPercentage applies HALF_UP rounding when precision exceeds one decimal place`() {
        assertEquals(
            "33.4%",
            AnalyticsAmountFormatter.formatPercentage(BigDecimal("33.35"), Locale.UK),
        )
        assertEquals(
            "33.3%",
            AnalyticsAmountFormatter.formatPercentage(BigDecimal("33.34"), Locale.UK),
        )
    }

    @Test
    fun `formatPercentageDelta returns null when delta is null without inventing percentage`() {
        val result = AnalyticsAmountFormatter.formatPercentageDelta(
            delta = null,
            locale = Locale.UK,
        )
        assertNull(result)
    }

    @Test
    fun `formatPercentageDelta formats positive delta with plus sign`() {
        val result = AnalyticsAmountFormatter.formatPercentageDelta(
            delta = BigDecimal("12.5"),
            locale = Locale.UK,
        )
        assertEquals("+12.5%", result)
    }

    @Test
    fun `formatPercentageDelta formats negative delta with minus sign`() {
        val result = AnalyticsAmountFormatter.formatPercentageDelta(
            delta = BigDecimal("-8.0"),
            locale = Locale.UK,
        )
        assertEquals("-8.0%", result)
    }

    @Test
    fun `formatPercentageDelta formats negative 100 percent delta`() {
        val result = AnalyticsAmountFormatter.formatPercentageDelta(
            delta = BigDecimal("-100.0"),
            locale = Locale.UK,
        )
        assertEquals("-100.0%", result)
    }

    @Test
    fun `formatPercentageDelta formats zero delta neutrally without sign`() {
        val result = AnalyticsAmountFormatter.formatPercentageDelta(
            delta = BigDecimal("0.0"),
            locale = Locale.UK,
        )
        assertEquals("0.0%", result)
    }

    @Test
    fun `formatPercentageDelta applies HALF_UP rounding`() {
        assertEquals(
            "+12.6%",
            AnalyticsAmountFormatter.formatPercentageDelta(BigDecimal("12.55"), Locale.UK),
        )
        assertEquals(
            "-8.1%",
            AnalyticsAmountFormatter.formatPercentageDelta(BigDecimal("-8.05"), Locale.UK),
        )
    }

    @Test
    fun `formatDateRange formats single day date without dash`() {
        val date = LocalDate.of(2026, 10, 1)
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = date,
            endDate = date,
            locale = Locale.UK,
        )
        assertEquals("1 Oct 2026", result)
    }

    @Test
    fun `formatDateRange formats intra-month date range with en-dash`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 16),
            locale = Locale.UK,
        )
        assertEquals("1 Oct 2026 – 16 Oct 2026", result)
    }

    @Test
    fun `formatDateRange formats full month range`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            locale = Locale.UK,
        )
        assertEquals("1 Sept 2026 – 30 Sept 2026", result)
    }

    @Test
    fun `formatDateRange formats cross-month date range`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 10, 16),
            locale = Locale.UK,
        )
        assertEquals("1 Aug 2026 – 16 Oct 2026", result)
    }

    @Test
    fun `formatDateRange formats cross-year date range`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2025, 11, 1),
            endDate = LocalDate.of(2026, 1, 31),
            locale = Locale.UK,
        )
        assertEquals("1 Nov 2025 – 31 Jan 2026", result)
    }

    @Test
    fun `formatDateRange formats leap year February end date`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2024, 2, 1),
            endDate = LocalDate.of(2024, 2, 29),
            locale = Locale.UK,
        )
        assertEquals("1 Feb 2024 – 29 Feb 2024", result)
    }

    @Test
    fun `formatDateRange formats standard non-leap February end date`() {
        val result = AnalyticsAmountFormatter.formatDateRange(
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 2, 28),
            locale = Locale.UK,
        )
        assertEquals("1 Feb 2026 – 28 Feb 2026", result)
    }
}
