package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.repository.TransactionRepository

class GetTransactionUseCase(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(id: String): Transaction? =
        repository.getTransaction(id)
}
