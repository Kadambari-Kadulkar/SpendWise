package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.TransactionType
import java.time.LocalDate

sealed interface TransactionUiEvent {
    data object StartAdd : TransactionUiEvent
    data class StartEdit(val transactionId: String) : TransactionUiEvent

    data class AmountChanged(val value: String) : TransactionUiEvent
    data class TypeChanged(val value: TransactionType) : TransactionUiEvent
    data class CategoryChanged(val value: String?) : TransactionUiEvent
    data class DateChanged(val value: LocalDate?) : TransactionUiEvent
    data class NoteChanged(val value: String) : TransactionUiEvent

    data object SaveClicked : TransactionUiEvent

    data class DeleteRequested(val transactionId: String) : TransactionUiEvent
    data object DeleteConfirmed : TransactionUiEvent
    data object DeleteCancelled : TransactionUiEvent

    data object RetryList : TransactionUiEvent
    data object ClearError : TransactionUiEvent
}

/** One-shot results for navigation/snackbar handling outside the ViewModel. */
sealed interface TransactionUiEffect {
    data class Saved(val transactionId: String) : TransactionUiEffect
    data class Deleted(val transactionId: String) : TransactionUiEffect
}
