package com.kadambari.spendwise.domain.model

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionTest {
    private val londonZone = ZoneId.of("Europe/London")
    private val transactionInstant = Instant.parse("2026-09-01T12:00:00Z")
    private val transactionClock = Clock.fixed(transactionInstant, londonZone)
    private val today = LocalDate.of(2026, 9, 1)

    @Test
    fun `valid transaction is accepted`() {
        val transaction = Transaction.create(
            id = "transaction-1",
            draft = expenseDraft(transactionDate = today),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals("transaction-1", transaction.id)
        assertEquals(1000L, transaction.amount.minorUnits)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(CategoryId.of("expense_food"), transaction.categoryId)
        assertEquals(today, transaction.transactionDate)
        assertEquals(transactionInstant.toEpochMilli(), transaction.createdAtEpochMillis)
        assertEquals(transaction.createdAtEpochMillis, transaction.updatedAtEpochMillis)
    }

    @Test
    fun `today transaction date is accepted`() {
        val transaction = Transaction.create(
            id = "transaction-today",
            draft = expenseDraft(transactionDate = today),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals(today, transaction.transactionDate)
    }

    @Test
    fun `past transaction date is accepted`() {
        val pastDate = today.minusDays(1)

        val transaction = Transaction.create(
            id = "transaction-past",
            draft = expenseDraft(transactionDate = pastDate),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals(pastDate, transaction.transactionDate)
    }

    @Test
    fun `future transaction date is rejected`() {
        assertThrows(DomainValidationException::class.java) {
            Transaction.create(
                id = "transaction-future",
                draft = expenseDraft(transactionDate = today.plusDays(1)),
                clock = transactionClock,
                zoneId = londonZone
            )
        }
    }

    @Test
    fun `today is evaluated using the supplied timezone`() {
        val losAngelesZone = ZoneId.of("America/Los_Angeles")
        val earlyMorningUtc = Clock.fixed(
            Instant.parse("2026-09-01T00:30:00Z"),
            losAngelesZone
        )

        val transaction = Transaction.create(
            id = "transaction-timezone",
            draft = expenseDraft(transactionDate = LocalDate.of(2026, 8, 31)),
            clock = earlyMorningUtc,
            zoneId = losAngelesZone
        )

        assertEquals(LocalDate.of(2026, 8, 31), transaction.transactionDate)

        assertThrows(DomainValidationException::class.java) {
            Transaction.create(
                id = "transaction-timezone-future",
                draft = expenseDraft(transactionDate = today),
                clock = earlyMorningUtc,
                zoneId = losAngelesZone
            )
        }
    }

    @Test
    fun `category and transaction type must match`() {
        val incomeDraft = expenseDraft(
            transactionDate = today,
            type = TransactionType.INCOME
        )

        assertThrows(DomainValidationException::class.java) {
            Transaction.create(
                id = "transaction-mismatch",
                draft = incomeDraft,
                clock = transactionClock,
                zoneId = londonZone
            )
        }
    }

    @Test
    fun `valid category and transaction type combination is accepted`() {
        val transaction = Transaction.create(
            id = "transaction-income",
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(320000, CurrencyCode.GBP),
                type = TransactionType.INCOME,
                categoryId = CategoryId.of("income_salary"),
                transactionDate = today
            ),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals(TransactionType.INCOME, transaction.type)
        assertEquals(CategoryId.of("income_salary"), transaction.categoryId)
    }

    @Test
    fun `blank note is normalised to null`() {
        val transaction = Transaction.create(
            id = "transaction-note",
            draft = expenseDraft(transactionDate = today, note = "   "),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals(null, transaction.note)
    }

    @Test
    fun `note at the maximum length is accepted`() {
        val note = "a".repeat(TransactionDraft.MAX_NOTE_LENGTH)

        val transaction = Transaction.create(
            id = "transaction-note-max",
            draft = expenseDraft(transactionDate = today, note = note),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertEquals(note, transaction.note)
    }

    @Test
    fun `note longer than the maximum length is rejected`() {
        val note = "a".repeat(TransactionDraft.MAX_NOTE_LENGTH + 1)

        assertThrows(DomainValidationException::class.java) {
            Transaction.create(
                id = "transaction-note-too-long",
                draft = expenseDraft(transactionDate = today, note = note),
                clock = transactionClock,
                zoneId = londonZone
            )
        }
    }

    @Test
    fun `update preserves ID and createdAt and changes updatedAt`() {
        val original = Transaction.create(
            id = "transaction-update",
            draft = expenseDraft(transactionDate = today, note = "Original"),
            clock = transactionClock,
            zoneId = londonZone
        )
        val updateClock = Clock.fixed(
            Instant.parse("2026-09-01T13:00:00Z"),
            londonZone
        )

        val updated = original.update(
            draft = expenseDraft(transactionDate = today, note = "Updated"),
            clock = updateClock,
            zoneId = londonZone
        )

        assertEquals(original.id, updated.id)
        assertEquals(original.createdAtEpochMillis, updated.createdAtEpochMillis)
        assertTrue(updated.updatedAtEpochMillis > original.updatedAtEpochMillis)
        assertEquals("Updated", updated.note)
    }

    @Test
    fun `update with a non advancing timestamp is rejected`() {
        val original = Transaction.create(
            id = "transaction-stale-update",
            draft = expenseDraft(transactionDate = today),
            clock = transactionClock,
            zoneId = londonZone
        )

        assertThrows(DomainValidationException::class.java) {
            original.update(
                draft = expenseDraft(transactionDate = today),
                clock = transactionClock,
                zoneId = londonZone
            )
        }
    }

    @Test
    fun `transaction ID must not be blank`() {
        assertThrows(DomainValidationException::class.java) {
            Transaction.create(
                id = "   ",
                draft = expenseDraft(transactionDate = today),
                clock = transactionClock,
                zoneId = londonZone
            )
        }
    }

    private fun expenseDraft(
        transactionDate: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
        note: String? = null
    ): TransactionDraft = TransactionDraft(
        amount = Money.fromMinorUnits(1000, CurrencyCode.GBP),
        type = type,
        categoryId = CategoryId.of("expense_food"),
        transactionDate = transactionDate,
        note = note
    )
}
