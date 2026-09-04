package com.kadambari.spendwise.data.mapper

import com.kadambari.spendwise.data.local.database.TransactionEntity
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Explicit mapping keeps Room persistence details out of the domain layer.
 *
 * No Room type converters are required: the entity intentionally exposes
 * primitive Room-friendly values. LocalDate is persisted as ISO-8601 text,
 * while the domain value objects and enum are persisted through their stable
 * String values here.
 */
fun TransactionEntity.toDomain(): Transaction {
    val transactionDate = LocalDate.parse(transactionDate)
    val transactionType = parseTransactionType(type)
    val currency = CurrencyCode.of(currencyCode)
    val category = CategoryId.of(categoryId)
    val amount = Money.fromMinorUnits(
        minorUnits = amountMinorUnits,
        currencyCode = currency,
    )

    /*
     * Transaction's constructor is intentionally private. Recreate the
     * object through its domain factory so the normal invariants remain in
     * force, then restore the exact persisted timestamps with the generated
     * data-class copy operation. The adjusted instant only prevents a valid
     * historical row from being rejected because its creation timestamp is
     * earlier than the stored transaction date in UTC; persisted timestamps
     * are restored unchanged immediately afterwards.
     */
    val creationInstant = Instant.ofEpochMilli(createdAtEpochMillis)
    val dateAtUtcStart = transactionDate.atStartOfDay(ZoneOffset.UTC).toInstant()
    val validationInstant = if (creationInstant >= dateAtUtcStart) {
        creationInstant
    } else {
        dateAtUtcStart
    }
    val rehydrated = Transaction.create(
        id = id,
        draft = TransactionDraft(
            amount = amount,
            type = transactionType,
            categoryId = category,
            transactionDate = transactionDate,
            note = note,
        ),
        clock = Clock.fixed(validationInstant, ZoneOffset.UTC),
        zoneId = ZoneOffset.UTC,
    )

    return rehydrated.copy(
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinorUnits = amount.minorUnits,
    currencyCode = amount.currencyCode.value,
    type = type.name,
    categoryId = categoryId.value,
    transactionDate = transactionDate.toString(),
    note = note,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun parseTransactionType(value: String): TransactionType =
    runCatching { TransactionType.valueOf(value) }
        .getOrElse { throw IllegalArgumentException("Unknown transaction type: $value", it) }
