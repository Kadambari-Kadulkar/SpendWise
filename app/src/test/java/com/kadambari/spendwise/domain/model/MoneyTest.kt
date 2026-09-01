package com.kadambari.spendwise.domain.model

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test
    fun `GBP 10 point 00 converts to 1000 minor units`() {
        val money = Money.fromDecimal(
            amount = BigDecimal("10.00"),
            currencyCode = CurrencyCode.GBP
        )

        assertEquals(1000L, money.minorUnits)
        assertEquals(CurrencyCode.GBP, money.currencyCode)
    }

    @Test
    fun `GBP 0 point 10 converts to 10 minor units`() {
        val money = Money.fromDecimal(
            amount = BigDecimal("0.10"),
            currencyCode = CurrencyCode.GBP
        )

        assertEquals(10L, money.minorUnits)
    }

    @Test
    fun `zero decimal amount is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Money.fromDecimal(BigDecimal("0.00"), CurrencyCode.GBP)
        }
    }

    @Test
    fun `zero minor unit amount is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Money.fromMinorUnits(0L, CurrencyCode.GBP)
        }
    }

    @Test
    fun `negative amount is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Money.fromDecimal(BigDecimal("-1.00"), CurrencyCode.GBP)
        }
    }

    @Test
    fun `more than two GBP fractional digits are rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Money.fromDecimal(BigDecimal("10.001"), CurrencyCode.GBP)
        }
    }

    @Test
    fun `minor unit overflow is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Money.fromDecimal(BigDecimal("92233720368547758.08"), CurrencyCode.GBP)
        }
    }

    @Test
    fun `Long maximum minor unit value is accepted`() {
        val money = Money.fromMinorUnits(Long.MAX_VALUE, CurrencyCode.GBP)

        assertEquals(Long.MAX_VALUE, money.minorUnits)
    }
}
