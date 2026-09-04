package com.kadambari.spendwise.data.mapper

import com.kadambari.spendwise.data.local.database.TransactionEntity
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {

    @Test
    fun transactionRoundTripsThroughEntityWithExactPersistedValues() {
        val creationInstant = Instant.parse("2026-09-01T12:00:00Z")
        val updatedInstant = Instant.parse("2026-09-01T13:00:00Z")
        val transaction = Transaction.create(
            id = "transaction-1",
            draft = TransactionDraft(
                amount = Money.fromDecimal(BigDecimal("10.25"), CurrencyCode.GBP),
                type = TransactionType.EXPENSE,
                categoryId = CategoryId.of("expense_food"),
                transactionDate = LocalDate.parse("2026-08-31"),
                note = "Lunch",
            ),
            clock = Clock.fixed(creationInstant, ZoneId.of("Europe/London")),
            zoneId = ZoneId.of("Europe/London"),
        ).update(
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(1025L, CurrencyCode.GBP),
                type = TransactionType.EXPENSE,
                categoryId = CategoryId.of("expense_food"),
                transactionDate = LocalDate.parse("2026-08-31"),
                note = "Updated lunch",
            ),
            clock = Clock.fixed(updatedInstant, ZoneId.of("Europe/London")),
            zoneId = ZoneId.of("Europe/London"),
        )

        val entity: TransactionEntity = transaction.toEntity()
        val restored = entity.toDomain()

        assertEquals("transaction-1", entity.id)
        assertEquals(1025L, entity.amountMinorUnits)
        assertEquals("GBP", entity.currencyCode)
        assertEquals("EXPENSE", entity.type)
        assertEquals("expense_food", entity.categoryId)
        assertEquals("2026-08-31", entity.transactionDate)
        assertEquals("Updated lunch", entity.note)
        assertEquals(creationInstant.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(updatedInstant.toEpochMilli(), entity.updatedAtEpochMillis)
        assertEquals(transaction, restored)
    }
}
