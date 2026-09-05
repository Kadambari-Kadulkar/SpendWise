package com.kadambari.spendwise.presentation.transaction

import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import java.time.LocalDate

enum class TransactionFormMode {
    ADD,
    EDIT,
}

/**
 * Shared Add/Edit input state. Raw amount text is retained because it is
 * input state; a domain Money value is created only when saving.
 */
data class TransactionFormUiState(
    val mode: TransactionFormMode,
    val transactionId: String? = null,
    val amountText: String = "",
    val currencyCode: CurrencyCode = CurrencyCatalogue.GBP.code,
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: String? = null,
    val date: LocalDate? = null,
    val noteText: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val dateError: String? = null,
    val noteError: String? = null,
    val isLoading: Boolean = false,
)
