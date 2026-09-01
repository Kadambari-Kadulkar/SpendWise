package com.kadambari.spendwise.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionFilterTest {
    @Test
    fun `query is trimmed for repository use`() {
        val filter = TransactionFilter(query = "  food  ")

        assertEquals("food", filter.normalisedQuery)
    }

    @Test
    fun `reversed date range is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            TransactionFilter(
                fromDate = LocalDate.of(2026, 9, 2),
                toDate = LocalDate.of(2026, 9, 1)
            )
        }
    }
}
