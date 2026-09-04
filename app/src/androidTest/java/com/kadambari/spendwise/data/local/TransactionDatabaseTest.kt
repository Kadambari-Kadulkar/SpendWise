package com.kadambari.spendwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kadambari.spendwise.data.local.database.SpendWiseDatabase
import com.kadambari.spendwise.data.local.database.TransactionEntity
import com.kadambari.spendwise.data.mapper.toDomain
import com.kadambari.spendwise.data.mapper.toEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDatabaseTest {

    private lateinit var database: SpendWiseDatabase

    private val firstEntity = TransactionEntity(
        id = "transaction-1",
        amountMinorUnits = 12345L,
        currencyCode = "GBP",
        type = "EXPENSE",
        categoryId = "expense_food",
        transactionDate = "2026-09-01",
        note = null,
        createdAtEpochMillis = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli(),
        updatedAtEpochMillis = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli(),
    )

    private val secondEntity = TransactionEntity(
        id = "transaction-2",
        amountMinorUnits = 10L,
        currencyCode = "GBP",
        type = "INCOME",
        categoryId = "income_salary",
        transactionDate = "2026-08-31",
        note = "Salary",
        createdAtEpochMillis = Instant.parse("2026-09-01T09:00:00Z").toEpochMilli(),
        updatedAtEpochMillis = Instant.parse("2026-09-01T09:00:00Z").toEpochMilli(),
    )

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTransactionById() = runBlocking {
        val dao = database.transactionDao()

        dao.insertTransaction(firstEntity)

        assertEquals(firstEntity, dao.getTransactionById(firstEntity.id))
    }

    @Test
    fun observeTransactionsEmitsInsertedTransactionsAndSupportsMultipleRows() = runBlocking {
        val dao = database.transactionDao()

        assertTrue(dao.observeTransactions().first().isEmpty())

        dao.insertTransaction(firstEntity)
        dao.insertTransaction(secondEntity)

        assertEquals(
            listOf(firstEntity, secondEntity),
            dao.observeTransactions().first(),
        )
    }

    @Test
    fun updateTransactionChangesTheExistingRow() = runBlocking {
        val dao = database.transactionDao()
        dao.insertTransaction(firstEntity)
        val updatedEntity = firstEntity.copy(
            amountMinorUnits = 999L,
            note = "Updated note",
            updatedAtEpochMillis = Instant.parse("2026-09-01T13:00:00Z").toEpochMilli(),
        )

        assertEquals(1, dao.updateTransaction(updatedEntity))
        assertEquals(updatedEntity, dao.getTransactionById(firstEntity.id))
    }

    @Test
    fun deleteTransactionRemovesTheExistingRow() = runBlocking {
        val dao = database.transactionDao()
        dao.insertTransaction(firstEntity)

        assertEquals(1, dao.deleteTransaction(firstEntity.id))
        assertNull(dao.getTransactionById(firstEntity.id))
    }

    @Test
    fun persistencePreservesAllExplicitFieldsExactly() = runBlocking {
        val dao = database.transactionDao()
        val entity = firstEntity.copy(
            amountMinorUnits = Long.MAX_VALUE - 10,
            currencyCode = "GBP",
            type = "EXPENSE",
            categoryId = "expense_food",
            transactionDate = "2026-09-01",
            note = "Exact persisted value",
        )

        dao.insertTransaction(entity)

        assertEquals(entity, dao.getTransactionById(entity.id))
    }

    @Test
    fun persistencePreservesNullableNote() = runBlocking {
        val dao = database.transactionDao()
        dao.insertTransaction(firstEntity)

        assertNull(dao.getTransactionById(firstEntity.id)?.note)
    }

    @Test
    fun transactionMapsToEntityAndBackWithoutChangingDomainValues() {
        val creationInstant = Instant.parse("2026-09-01T12:00:00Z")
        val transaction = Transaction.create(
            id = "mapped-transaction",
            draft = TransactionDraft(
                amount = Money.fromDecimal(BigDecimal("10.25"), CurrencyCode.GBP),
                type = TransactionType.EXPENSE,
                categoryId = CategoryId.of("expense_food"),
                transactionDate = LocalDate.parse("2026-09-01"),
                note = "Mapped note",
            ),
            clock = Clock.fixed(creationInstant, ZoneId.of("Europe/London")),
            zoneId = ZoneId.of("Europe/London"),
        )

        val entity = transaction.toEntity()
        val restored = entity.toDomain()

        assertEquals("mapped-transaction", entity.id)
        assertEquals(1025L, entity.amountMinorUnits)
        assertEquals("GBP", entity.currencyCode)
        assertEquals("EXPENSE", entity.type)
        assertEquals("expense_food", entity.categoryId)
        assertEquals("2026-09-01", entity.transactionDate)
        assertEquals("Mapped note", entity.note)
        assertEquals(creationInstant.toEpochMilli(), entity.createdAtEpochMillis)
        assertEquals(creationInstant.toEpochMilli(), entity.updatedAtEpochMillis)
        assertEquals(transaction, restored)
    }
}
