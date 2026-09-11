package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionAmountFormatterTest {

    @Test
    fun `formats GBP expense with a negative sign and two decimal places`() {
        assertEquals(
            "-£12.50",
            TransactionAmountFormatter.format(
                amountMinorUnits = 1250L,
                currencyCode = CurrencyCode.GBP,
                type = TransactionType.EXPENSE,
            ),
        )
    }

    @Test
    fun `formats GBP income with a positive sign and two decimal places`() {
        assertEquals(
            "+£500.00",
            TransactionAmountFormatter.format(
                amountMinorUnits = 50000L,
                currencyCode = CurrencyCode.GBP,
                type = TransactionType.INCOME,
            ),
        )
    }

    @Test
    fun `formats large GBP amounts with grouping separators`() {
        assertEquals(
            "+£1,234,567.89",
            TransactionAmountFormatter.format(
                amountMinorUnits = 123456789L,
                currencyCode = CurrencyCode.GBP,
                type = TransactionType.INCOME,
            ),
        )
    }
}
