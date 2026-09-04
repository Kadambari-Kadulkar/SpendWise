package com.kadambari.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kadambari.spendwise.data.local.database.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * Fails on an existing ID rather than silently replacing persisted data.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /**
     * Returns the number of rows updated. A result of zero means the ID did
     * not exist.
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    /**
     * Returns the number of rows deleted. A result of zero means the ID did
     * not exist.
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    /**
     * Dates are stored in ISO-8601 form, so lexicographic ordering is also
     * chronological ordering. The timestamp tie-breaker keeps the result
     * deterministic for transactions on the same date.
     */
    @Query(
        """
        SELECT * FROM transactions
        ORDER BY transactionDate DESC, createdAtEpochMillis DESC
        """,
    )
    fun observeTransactions(): Flow<List<TransactionEntity>>
}
