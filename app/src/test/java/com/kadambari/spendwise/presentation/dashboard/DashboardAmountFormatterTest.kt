package com.kadambari.spendwise.presentation.dashboard

import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardAmountFormatterTest {

    @Test
    fun `formatBalance formats zero as currency symbol and zeroes`() {
        val result = DashboardAmountFormatter.formatBalance(
            amountMinorUnits = 0L,
            currencyCode = CurrencyCode.GBP,
        )
        assertEquals("£0.00", result)
    }

    @Test
    fun `formatBalance formats positive amounts with grouping separators and symbol`() {
        val result = DashboardAmountFormatter.formatBalance(
            amountMinorUnits = 248050L,
            currencyCode = CurrencyCode.GBP,
        )
        assertEquals("£2,480.50", result)
    }

    @Test
    fun `formatBalance formats negative amounts with minus and currency symbol`() {
        val result = DashboardAmountFormatter.formatBalance(
            amountMinorUnits = -5000L,
            currencyCode = CurrencyCode.GBP,
        )
        assertEquals("-£50.00", result)
    }

    @Test
    fun `formatMonthlyTotal formats income with plus sign`() {
        val result = DashboardAmountFormatter.formatMonthlyTotal(
            amountMinorUnits = 320000L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.INCOME,
        )
        assertEquals("+£3,200.00", result)
    }

    @Test
    fun `formatMonthlyTotal formats expense with minus sign`() {
        val result = DashboardAmountFormatter.formatMonthlyTotal(
            amountMinorUnits = 71950L,
            currencyCode = CurrencyCode.GBP,
            type = TransactionType.EXPENSE,
        )
        assertEquals("-£719.50", result)
    }
}
