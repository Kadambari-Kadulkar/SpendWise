package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.DomainValidationException
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.repository.TransactionRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionUseCasesTest {

    @Test
    fun `add creates persists and returns a transaction using injected time`() = runBlocking {
        val repository = FakeTransactionRepository()
        val instant = Instant.parse("2026-09-01T00:30:00Z")
        val zoneId = ZoneId.of("America/Los_Angeles")
        val useCase = AddTransactionUseCase(
            repository = repository,
            clock = Clock.fixed(instant, zoneId),
            zoneId = zoneId,
        )

        val result = useCase(
            expenseDraft(transactionDate = LocalDate.of(2026, 8, 31))
        )

        assertEquals(result, repository.insertedTransaction)
        assertEquals(LocalDate.of(2026, 8, 31), result.transactionDate)
        assertEquals(instant.toEpochMilli(), result.createdAtEpochMillis)
        assertEquals(result.createdAtEpochMillis, result.updatedAtEpochMillis)
        assertTrue(result.id.isNotBlank())
        assertNotNull(UUID.fromString(result.id))
    }

    @Test
    fun `add propagates domain validation failures without persisting`() {
        val repository = FakeTransactionRepository()
        val useCase = AddTransactionUseCase(
            repository = repository,
            clock = transactionClock,
            zoneId = LONDON_ZONE,
        )

        org.junit.Assert.assertThrows(DomainValidationException::class.java) {
            runBlocking {
                useCase(
                    expenseDraft(
                        transactionDate = TODAY,
                        type = TransactionType.INCOME,
                    )
                )
            }
        }

        assertNull(repository.insertedTransaction)
    }

    @Test
    fun `add propagates repository exceptions without swallowing them`() {
        val expectedException = RuntimeException("database failure")
        val repository = FakeTransactionRepository().apply {
            insertException = expectedException
        }
        val useCase = AddTransactionUseCase(
            repository = repository,
            clock = transactionClock,
            zoneId = LONDON_ZONE,
        )

        val actualException = org.junit.Assert.assertThrows(RuntimeException::class.java) {
            runBlocking {
                useCase(expenseDraft(transactionDate = TODAY))
            }
        }

        assertSame(expectedException, actualException)
        assertEquals("database failure", actualException.message)
    }

    @Test
    fun `get transactions returns the repository flow unchanged`() = runBlocking {
        val repository = FakeTransactionRepository()
        val useCase = GetTransactionsUseCase(repository)

        val result = useCase()

        assertSame(repository.observedTransactions, result)

        val transaction = expenseTransaction()
        repository.observedTransactions.value = listOf(transaction)
        assertEquals(listOf(transaction), result.first())
        assertEquals(1, repository.observeCallCount)
    }

    @Test
    fun `get transaction forwards the ID and returns the repository result`() = runBlocking {
        val repository = FakeTransactionRepository()
        val transaction = expenseTransaction()
        repository.transactions[transaction.id] = transaction
        val useCase = GetTransactionUseCase(repository)

        assertEquals(transaction, useCase(transaction.id))
        assertEquals(listOf(transaction.id), repository.getCalls)
    }

    @Test
    fun `get transaction returns null when the transaction is missing`() = runBlocking {
        val repository = FakeTransactionRepository()
        val useCase = GetTransactionUseCase(repository)

        assertNull(useCase("missing-transaction"))
    }

    @Test
    fun `update loads the transaction applies the domain update and returns success`() = runBlocking {
        val repository = FakeTransactionRepository()
        val original = expenseTransaction()
        repository.transactions[original.id] = original
        val updatedAt = Instant.parse("2026-09-01T13:00:00Z")
        val useCase = UpdateTransactionUseCase(
            repository = repository,
            clock = Clock.fixed(updatedAt, LONDON_ZONE),
            zoneId = LONDON_ZONE,
        )

        val result = useCase(
            id = original.id,
            draft = expenseDraft(
                transactionDate = original.transactionDate,
                note = "Updated note",
            ),
        )

        val updated = requireNotNull(repository.updatedTransaction)
        assertTrue(result)
        assertEquals(listOf("get", "update"), repository.operations)
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAtEpochMillis, updated.createdAtEpochMillis)
        assertEquals(updatedAt.toEpochMilli(), updated.updatedAtEpochMillis)
        assertEquals("Updated note", updated.note)
    }

    @Test
    fun `update returns false and does not update when the transaction is missing`() = runBlocking {
        val repository = FakeTransactionRepository()
        val useCase = UpdateTransactionUseCase(
            repository = repository,
            clock = transactionClock,
            zoneId = LONDON_ZONE,
        )

        assertFalse(
            useCase(
                id = "missing-transaction",
                draft = expenseDraft(transactionDate = TODAY),
            )
        )

        assertNull(repository.updatedTransaction)
        assertEquals(listOf("get"), repository.operations)
    }

    @Test
    fun `update returns the repository result`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            updateResult = false
        }
        val original = expenseTransaction()
        repository.transactions[original.id] = original
        val useCase = UpdateTransactionUseCase(
            repository = repository,
            clock = Clock.fixed(
                Instant.parse("2026-09-01T13:00:00Z"),
                LONDON_ZONE,
            ),
            zoneId = LONDON_ZONE,
        )

        assertFalse(
            useCase(
                id = original.id,
                draft = expenseDraft(transactionDate = TODAY),
            )
        )
    }

    @Test
    fun `update propagates domain validation failures and does not persist`() {
        val repository = FakeTransactionRepository()
        val original = expenseTransaction()
        repository.transactions[original.id] = original
        val useCase = UpdateTransactionUseCase(
            repository = repository,
            clock = transactionClock,
            zoneId = LONDON_ZONE,
        )

        org.junit.Assert.assertThrows(DomainValidationException::class.java) {
            runBlocking {
                useCase(
                    id = original.id,
                    draft = expenseDraft(
                        transactionDate = TODAY,
                        type = TransactionType.INCOME,
                    ),
                )
            }
        }

        assertNull(repository.updatedTransaction)
        assertEquals(listOf("get"), repository.operations)
    }

    @Test
    fun `delete forwards the ID and returns the repository result without a pre-read`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            deleteResult = true
        }
        val useCase = DeleteTransactionUseCase(repository)

        assertTrue(useCase("transaction-to-delete"))

        assertEquals(listOf("transaction-to-delete"), repository.deleteCalls)
        assertTrue(repository.getCalls.isEmpty())
        assertEquals(listOf("delete"), repository.operations)
    }

    @Test
    fun `delete returns false when the repository reports no deleted row`() = runBlocking {
        val repository = FakeTransactionRepository().apply {
            deleteResult = false
        }
        val useCase = DeleteTransactionUseCase(repository)

        assertFalse(useCase("missing-transaction"))
    }

    private fun expenseTransaction(note: String? = "Lunch"): Transaction =
        Transaction.create(
            id = "transaction-expense",
            draft = expenseDraft(
                transactionDate = TODAY,
                note = note,
            ),
            clock = transactionClock,
            zoneId = LONDON_ZONE,
        )

    private fun expenseDraft(
        transactionDate: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
        note: String? = null,
    ): TransactionDraft = TransactionDraft(
        amount = Money.fromMinorUnits(1025L, CurrencyCode.GBP),
        type = type,
        categoryId = CategoryId.of("expense_food"),
        transactionDate = transactionDate,
        note = note,
    )

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = mutableMapOf<String, Transaction>()
        val observedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
        val getCalls = mutableListOf<String>()
        val deleteCalls = mutableListOf<String>()
        val operations = mutableListOf<String>()

        var insertedTransaction: Transaction? = null
            private set
        var insertException: RuntimeException? = null
        var updatedTransaction: Transaction? = null
            private set
        var updateResult: Boolean = true
        var deleteResult: Boolean = false
        var observeCallCount: Int = 0

        override fun observeTransactions(): Flow<List<Transaction>> {
            observeCallCount += 1
            return observedTransactions
        }

        override suspend fun getTransaction(id: String): Transaction? {
            operations += "get"
            getCalls += id
            return transactions[id]
        }

        override suspend fun insertTransaction(transaction: Transaction) {
            insertException?.let { throw it }
            insertedTransaction = transaction
            transactions[transaction.id] = transaction
        }

        override suspend fun updateTransaction(transaction: Transaction): Boolean {
            operations += "update"
            updatedTransaction = transaction
            if (updateResult) {
                transactions[transaction.id] = transaction
            }
            return updateResult
        }

        override suspend fun deleteTransaction(id: String): Boolean {
            operations += "delete"
            deleteCalls += id
            return deleteResult
        }
    }

    private companion object {
        val LONDON_ZONE: ZoneId = ZoneId.of("Europe/London")
        val TODAY: LocalDate = LocalDate.of(2026, 9, 1)
        val transactionClock: Clock = Clock.fixed(
            Instant.parse("2026-09-01T12:00:00Z"),
            LONDON_ZONE,
        )
    }
}
