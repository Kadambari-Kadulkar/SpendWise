package com.kadambari.spendwise.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of a domain transaction.
 *
 * Domain value objects are represented by their stable persistence values so
 * the Room schema stays explicit and the domain layer remains Room-free.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val type: String,
    val categoryId: String,
    /** ISO-8601 calendar date, for example 2026-09-01. */
    val transactionDate: String,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
