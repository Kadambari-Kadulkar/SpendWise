package com.kadambari.spendwise.domain.repository

import com.kadambari.spendwise.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    fun observeTransactions(): Flow<List<Transaction>>

    suspend fun getTransaction(id: String): Transaction?

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction): Boolean

    suspend fun deleteTransaction(id: String): Boolean
}
