package com.kadambari.spendwise.data.repository

import com.kadambari.spendwise.data.local.dao.TransactionDao
import com.kadambari.spendwise.data.local.database.TransactionEntity
import com.kadambari.spendwise.data.mapper.toEntity
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.LinkedHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {

    private lateinit var dao: FakeTransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeTransactionDao()
        repository = TransactionRepositoryImpl(dao)
    }

    @Test
    fun `insert maps the domain transaction and passes it to the DAO`() = runBlocking {
        val transaction = expenseTransaction()

        repository.insertTransaction(transaction)

        assertEquals(transaction.toEntity(), dao.insertedEntity)
    }

    @Test
    fun `get maps an existing entity back to a domain transaction`() = runBlocking {
        val transaction = expenseTransaction()
        dao.seed(transaction.toEntity())

        val result = repository.getTransaction(transaction.id)

        assertEquals(transaction, result)
    }

    @Test
    fun `get returns null when the transaction does not exist`() = runBlocking {
        assertNull(repository.getTransaction("missing-transaction"))
    }

    @Test
    fun `update returns true when the DAO updates a row`() = runBlocking {
        val original = expenseTransaction()
        val updated = original.update(
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(1250L, CurrencyCode.GBP),
                type = TransactionType.EXPENSE,
                categoryId = CategoryId.of("expense_food"),
                transactionDate = original.transactionDate,
                note = "Updated note",
            ),
            clock = Clock.fixed(
                Instant.parse("2026-09-01T13:00:00Z"),
                LONDON_ZONE,
            ),
            zoneId = LONDON_ZONE,
        )
        dao.seed(original.toEntity())

        assertTrue(repository.updateTransaction(updated))
        assertEquals(updated.toEntity(), dao.updatedEntity)
    }

    @Test
    fun `update returns false when the DAO updates no rows`() = runBlocking {
        assertFalse(repository.updateTransaction(expenseTransaction()))
    }

    @Test
    fun `delete returns true when the DAO deletes a row`() = runBlocking {
        val transaction = expenseTransaction()
        dao.seed(transaction.toEntity())

        assertTrue(repository.deleteTransaction(transaction.id))
        assertEquals(transaction.id, dao.deletedId)
    }

    @Test
    fun `delete returns false when the DAO deletes no rows`() = runBlocking {
        assertFalse(repository.deleteTransaction("missing-transaction"))
    }

    @Test
    fun `observe maps the DAO flow to domain transactions`() = runBlocking {
        val transaction = expenseTransaction()
        dao.seed(transaction.toEntity())

        val result = repository.observeTransactions().first()

        assertEquals(listOf(transaction), result)
    }

    @Test
    fun `observe maps multiple entities and preserves their order`() = runBlocking {
        val firstTransaction = expenseTransaction()
        val secondTransaction = incomeTransaction()
        dao.seed(firstTransaction.toEntity(), secondTransaction.toEntity())

        val result = repository.observeTransactions().first()

        assertEquals(listOf(firstTransaction, secondTransaction), result)
    }

    @Test
    fun `repository mapping preserves all transaction fields`() = runBlocking {
        val transaction = expenseTransaction(note = "Complete field check")
        dao.seed(transaction.toEntity())

        val result = requireNotNull(repository.getTransaction(transaction.id))

        assertEquals(transaction.id, result.id)
        assertEquals(transaction.amount, result.amount)
        assertEquals(transaction.amount.currencyCode, result.amount.currencyCode)
        assertEquals(transaction.type, result.type)
        assertEquals(transaction.categoryId, result.categoryId)
        assertEquals(transaction.transactionDate, result.transactionDate)
        assertEquals(transaction.note, result.note)
        assertEquals(transaction.createdAtEpochMillis, result.createdAtEpochMillis)
        assertEquals(transaction.updatedAtEpochMillis, result.updatedAtEpochMillis)
    }

    private fun expenseTransaction(note: String? = "Lunch"): Transaction =
        Transaction.create(
            id = "transaction-expense",
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(1025L, CurrencyCode.GBP),
                type = TransactionType.EXPENSE,
                categoryId = CategoryId.of("expense_food"),
                transactionDate = LocalDate.of(2026, 9, 1),
                note = note,
            ),
            clock = Clock.fixed(CREATED_AT, LONDON_ZONE),
            zoneId = LONDON_ZONE,
        )

    private fun incomeTransaction(): Transaction =
        Transaction.create(
            id = "transaction-income",
            draft = TransactionDraft(
                amount = Money.fromMinorUnits(320000L, CurrencyCode.GBP),
                type = TransactionType.INCOME,
                categoryId = CategoryId.of("income_salary"),
                transactionDate = LocalDate.of(2026, 8, 31),
                note = "Salary",
            ),
            clock = Clock.fixed(CREATED_AT, LONDON_ZONE),
            zoneId = LONDON_ZONE,
        )

    private class FakeTransactionDao : TransactionDao {
        private val entities = LinkedHashMap<String, TransactionEntity>()
        private val observedEntities = MutableStateFlow<List<TransactionEntity>>(emptyList())

        var insertedEntity: TransactionEntity? = null
            private set
        var updatedEntity: TransactionEntity? = null
            private set
        var deletedId: String? = null
            private set

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            insertedEntity = transaction
            entities[transaction.id] = transaction
            publish()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity): Int {
            if (transaction.id !in entities) return 0

            updatedEntity = transaction
            entities[transaction.id] = transaction
            publish()
            return 1
        }

        override suspend fun deleteTransaction(id: String): Int {
            if (entities.remove(id) == null) return 0

            deletedId = id
            publish()
            return 1
        }

        override suspend fun getTransactionById(id: String): TransactionEntity? =
            entities[id]

        override fun observeTransactions(): Flow<List<TransactionEntity>> = observedEntities

        fun seed(vararg transactions: TransactionEntity) {
            transactions.forEach { entities[it.id] = it }
            publish()
        }

        private fun publish() {
            observedEntities.value = entities.values.toList()
        }
    }

    private companion object {
        val LONDON_ZONE: ZoneId = ZoneId.of("Europe/London")
        val CREATED_AT: Instant = Instant.parse("2026-09-01T12:00:00Z")
    }
}
