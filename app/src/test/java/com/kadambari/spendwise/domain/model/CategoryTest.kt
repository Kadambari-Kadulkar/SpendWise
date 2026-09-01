package com.kadambari.spendwise.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CategoryTest {
    @Test
    fun `all approved income and expense categories are available`() {
        assertEquals(5, CategoryCatalogue.forType(TransactionType.INCOME).size)
        assertEquals(9, CategoryCatalogue.forType(TransactionType.EXPENSE).size)
        assertNotNull(CategoryCatalogue.find(CategoryId.of("expense_food")))
    }

    @Test
    fun `unknown category is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            CategoryCatalogue.requireCategory(CategoryId.of("expense_unknown"))
        }
    }
}
