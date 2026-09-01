package com.kadambari.spendwise.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class TransactionType {
    INCOME,
    EXPENSE
}

/**
 * User-entered transaction values before an ID and system timestamps exist.
 */
data class TransactionDraft(
    val amount: Money,
    val type: TransactionType,
    val categoryId: CategoryId,
    val transactionDate: LocalDate,
    val note: String? = null
) {
    companion object {
        const val MAX_NOTE_LENGTH = 500
    }
}

/**
 * Filters supported by the future transaction list/repository boundary.
 */
data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: CategoryId? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null
) {
    init {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw DomainValidationException(
                "Transaction filter start date must not be after its end date."
            )
        }
    }

    val normalisedQuery: String
        get() = query.trim()
}

/**
 * A persisted transaction domain model.
 *
 * Instances are created through [create] and updated through [update] so
 * callers cannot accidentally replace the ID or creation timestamp while
 * editing.
 */
data class Transaction private constructor(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val categoryId: CategoryId,
    val transactionDate: LocalDate,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    companion object {
        fun create(
            id: String,
            draft: TransactionDraft,
            clock: Clock,
            zoneId: ZoneId
        ): Transaction {
            validateId(id)

            val now = clock.instant()
            val normalisedNote = validateDraft(
                draft = draft,
                today = now.atZone(zoneId).toLocalDate()
            )
            val timestamp = now.toEpochMilli()

            return Transaction(
                id = id,
                amount = draft.amount,
                type = draft.type,
                categoryId = draft.categoryId,
                transactionDate = draft.transactionDate,
                note = normalisedNote,
                createdAtEpochMillis = timestamp,
                updatedAtEpochMillis = timestamp
            )
        }
    }

    /**
     * Creates an edited transaction while preserving identity and creation
     * time. The update clock must produce a later timestamp.
     */
    fun update(
        draft: TransactionDraft,
        clock: Clock,
        zoneId: ZoneId
    ): Transaction {
        val now = clock.instant()
        val normalisedNote = validateDraft(
            draft = draft,
            today = now.atZone(zoneId).toLocalDate()
        )
        val newUpdatedAt = now.toEpochMilli()

        if (newUpdatedAt <= updatedAtEpochMillis) {
            throw DomainValidationException(
                "Updated timestamp must be later than the existing timestamp."
            )
        }

        return Transaction(
            id = id,
            amount = draft.amount,
            type = draft.type,
            categoryId = draft.categoryId,
            transactionDate = draft.transactionDate,
            note = normalisedNote,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = newUpdatedAt
        )
    }
}

private fun validateId(id: String) {
    if (id.isBlank()) {
        throw DomainValidationException("Transaction ID must not be blank.")
    }
}

private fun validateDraft(
    draft: TransactionDraft,
    today: LocalDate
): String? {
    CurrencyCatalogue.requireSupported(draft.amount.currencyCode)

    val category = CategoryCatalogue.requireCategory(draft.categoryId)
    if (category.transactionType != draft.type) {
        throw DomainValidationException(
            "Category ${draft.categoryId.value} is not valid for ${draft.type.name.lowercase()}."
        )
    }

    if (draft.transactionDate.isAfter(today)) {
        throw DomainValidationException(
            "Transaction date must be today or in the past."
        )
    }

    val normalisedNote = draft.note
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    if (normalisedNote != null &&
        normalisedNote.length > TransactionDraft.MAX_NOTE_LENGTH
    ) {
        throw DomainValidationException(
            "Transaction note must be ${TransactionDraft.MAX_NOTE_LENGTH} " +
                "characters or fewer."
        )
    }

    return normalisedNote
}
