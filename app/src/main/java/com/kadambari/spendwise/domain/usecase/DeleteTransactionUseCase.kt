package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(id: String): Boolean =
        repository.deleteTransaction(id)
}
