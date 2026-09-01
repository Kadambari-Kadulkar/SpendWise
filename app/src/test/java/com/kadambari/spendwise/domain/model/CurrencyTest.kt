package com.kadambari.spendwise.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrencyTest {
    @Test
    fun `valid GBP code is accepted and normalised`() {
        val currencyCode = CurrencyCode.of(" gbp ")

        assertEquals(CurrencyCode.GBP, currencyCode)
        assertEquals("GBP", currencyCode.value)
    }

    @Test
    fun `invalid currency code is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            CurrencyCode.of("GB")
        }
    }

    @Test
    fun `unsupported currency is rejected by the catalogue`() {
        val unsupportedCode = CurrencyCode.of("USD")

        assertThrows(DomainValidationException::class.java) {
            CurrencyCatalogue.requireSupported(unsupportedCode)
        }
    }

    @Test
    fun `GBP metadata exposes the correct fraction digits`() {
        val definition = CurrencyCatalogue.requireSupported(CurrencyCode.GBP)

        assertEquals("GBP", definition.code.value)
        assertEquals("£", definition.symbol)
        assertEquals(2, definition.fractionDigits)
    }
}
