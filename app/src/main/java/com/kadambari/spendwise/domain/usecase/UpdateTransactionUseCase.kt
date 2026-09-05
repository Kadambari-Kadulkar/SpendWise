package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.repository.TransactionRepository
import java.time.Clock
import java.time.ZoneId

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {

    suspend operator fun invoke(
        id: String,
        draft: TransactionDraft,
    ): Boolean {
        val existingTransaction = repository.getTransaction(id)
            ?: return false

        val updatedTransaction = existingTransaction.update(
            draft = draft,
            clock = clock,
            zoneId = zoneId,
        )

        return repository.updateTransaction(updatedTransaction)
    }
}
