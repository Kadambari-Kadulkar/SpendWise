package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val repository: TransactionRepository,
) {

    operator fun invoke(): Flow<List<Transaction>> =
        repository.observeTransactions()
}
