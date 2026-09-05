package com.kadambari.spendwise.domain.usecase

import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.repository.TransactionRepository
import java.time.Clock
import java.time.ZoneId
import java.util.UUID

class AddTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {

    suspend operator fun invoke(draft: TransactionDraft): Transaction {
        val transaction = Transaction.create(
            id = UUID.randomUUID().toString(),
            draft = draft,
            clock = clock,
            zoneId = zoneId,
        )

        repository.insertTransaction(transaction)
        return transaction
    }
}
