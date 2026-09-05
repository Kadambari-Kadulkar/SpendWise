package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.time.LocalDate

/**
 * Durable state required to render the transaction feature.
 *
 * List, form, and delete state deliberately remain flat for the current feature scope. The UI
 * can derive empty and success states from the loading/error fields rather
 * than handling a large sealed state hierarchy.
 */
data class TransactionUiState(
    val transactions: List<TransactionListItemUiModel> = emptyList(),
    val isListLoading: Boolean = true,
    val listError: TransactionUiError? = null,
    val form: TransactionFormUiState? = null,
    val isSaving: Boolean = false,
    val formError: TransactionUiError? = null,
    val pendingDeleteId: String? = null,
    val deletingTransactionId: String? = null,
    val deleteError: TransactionUiError? = null,
)

/** Presentation representation of a domain transaction for a future UI. */
data class TransactionListItemUiModel(
    val id: String,
    val amountMinorUnits: Long,
    val currencyCode: CurrencyCode,
    val type: TransactionType,
    val categoryId: String,
    val categoryLabel: String,
    val date: LocalDate,
    val note: String?,
)

enum class TransactionUiErrorKind {
    VALIDATION,
    NOT_FOUND,
    OPERATION,
}

data class TransactionUiError(
    val message: String,
    val kind: TransactionUiErrorKind,
)
