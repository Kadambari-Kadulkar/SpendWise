package com.kadambari.spendwise.data.repository

import com.kadambari.spendwise.data.local.dao.TransactionDao
import com.kadambari.spendwise.data.mapper.toDomain
import com.kadambari.spendwise.data.mapper.toEntity
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTransaction(id: String): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction): Boolean =
        transactionDao.updateTransaction(transaction.toEntity()) > 0

    override suspend fun deleteTransaction(id: String): Boolean =
        transactionDao.deleteTransaction(id) > 0
}
