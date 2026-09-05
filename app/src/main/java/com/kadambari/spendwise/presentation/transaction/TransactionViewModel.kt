package com.kadambari.spendwise.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.CurrencyCatalogue
import com.kadambari.spendwise.domain.model.DomainValidationException
import com.kadambari.spendwise.domain.model.Money
import com.kadambari.spendwise.domain.model.Transaction
import com.kadambari.spendwise.domain.model.TransactionDraft
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.domain.usecase.AddTransactionUseCase
import com.kadambari.spendwise.domain.usecase.DeleteTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionUseCase
import com.kadambari.spendwise.domain.usecase.GetTransactionsUseCase
import com.kadambari.spendwise.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TransactionUiEffect>()
    val effects: SharedFlow<TransactionUiEffect> = _effects.asSharedFlow()

    private var listJob: Job? = null
    private var editJob: Job? = null

    init {
        observeTransactions()
    }

    fun onEvent(event: TransactionUiEvent) {
        when (event) {
            TransactionUiEvent.StartAdd -> startAdd()
            is TransactionUiEvent.StartEdit -> startEdit(event.transactionId)
            is TransactionUiEvent.AmountChanged -> updateAmount(event.value)
            is TransactionUiEvent.TypeChanged -> updateType(event.value)
            is TransactionUiEvent.CategoryChanged -> updateCategory(event.value)
            is TransactionUiEvent.DateChanged -> updateDate(event.value)
            is TransactionUiEvent.NoteChanged -> updateNote(event.value)
            TransactionUiEvent.SaveClicked -> save()
            is TransactionUiEvent.DeleteRequested -> requestDelete(event.transactionId)
            TransactionUiEvent.DeleteConfirmed -> confirmDelete()
            TransactionUiEvent.DeleteCancelled -> cancelDelete()
            TransactionUiEvent.RetryList -> observeTransactions()
            TransactionUiEvent.ClearError -> clearErrors()
        }
    }

    private fun observeTransactions() {
        listJob?.cancel()
        _uiState.update { current ->
            current.copy(
                isListLoading = true,
                listError = null,
            )
        }

        listJob = viewModelScope.launch {
            try {
                getTransactionsUseCase()
                    .map { transactions -> transactions.map(::toUiModel) }
                    .collect { transactions ->
                        _uiState.update { current ->
                            current.copy(
                                transactions = transactions,
                                isListLoading = false,
                                listError = null,
                            )
                        }
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isListLoading = false,
                        listError = operationError("Unable to load transactions."),
                    )
                }
            }
        }
    }

    private fun startAdd() {
        val current = _uiState.value
        if (current.isSaving) {
            return
        }

        editJob?.cancel()
        _uiState.update { state ->
            state.copy(
                form = TransactionFormUiState(
                    mode = TransactionFormMode.ADD,
                    date = today(),
                ),
                formError = null,
            )
        }
    }

    private fun startEdit(transactionId: String) {
        val current = _uiState.value
        if (current.isSaving) {
            return
        }

        editJob?.cancel()
        _uiState.update { state ->
            state.copy(
                form = TransactionFormUiState(
                    mode = TransactionFormMode.EDIT,
                    transactionId = transactionId,
                    isLoading = true,
                ),
                formError = null,
            )
        }

        editJob = viewModelScope.launch {
            try {
                val transaction = getTransactionUseCase(transactionId)
                if (transaction == null) {
                    _uiState.update { state ->
                        state.copy(
                            form = state.form?.copy(isLoading = false),
                            formError = notFoundError("Transaction could not be found."),
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            form = transaction.toFormState(),
                            formError = null,
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { state ->
                    state.copy(
                        form = state.form?.copy(isLoading = false),
                        formError = operationError("Unable to load the transaction."),
                    )
                }
            }
        }
    }

    private fun updateAmount(value: String) {
        updateForm { form -> form.copy(amountText = value, amountError = null) }
    }

    private fun updateType(value: TransactionType) {
        updateForm { form ->
            form.copy(
                type = value,
                categoryId = null,
                categoryError = null,
            )
        }
    }

    private fun updateCategory(value: String?) {
        updateForm { form -> form.copy(categoryId = value, categoryError = null) }
    }

    private fun updateDate(value: LocalDate?) {
        updateForm { form -> form.copy(date = value, dateError = null) }
    }

    private fun updateNote(value: String) {
        updateForm { form -> form.copy(noteText = value, noteError = null) }
    }

    private fun updateForm(transform: (TransactionFormUiState) -> TransactionFormUiState) {
        if (_uiState.value.isSaving) {
            return
        }

        _uiState.update { current ->
            current.form?.let { form ->
                current.copy(
                    form = transform(form),
                    formError = null,
                )
            } ?: current
        }
    }

    private fun save() {
        val current = _uiState.value
        val form = current.form ?: return

        if (current.isSaving || form.isLoading) {
            return
        }

        when (val result = form.toDraft()) {
            is DraftBuildResult.Invalid -> {
                _uiState.update { state ->
                    state.copy(
                        form = result.form,
                        formError = null,
                    )
                }
            }

            is DraftBuildResult.Valid -> {
                _uiState.update { state ->
                    state.copy(
                        isSaving = true,
                        formError = null,
                    )
                }
                saveDraft(form, result.draft)
            }
        }
    }

    private fun saveDraft(
        form: TransactionFormUiState,
        draft: TransactionDraft,
    ) {
        viewModelScope.launch {
            try {
                val transactionId = when (form.mode) {
                    TransactionFormMode.ADD -> addTransactionUseCase(draft).id
                    TransactionFormMode.EDIT -> {
                        val id = form.transactionId
                        if (id == null) {
                            failSave(operationError("Unable to save the transaction."))
                            return@launch
                        }

                        if (!updateTransactionUseCase(id, draft)) {
                            failSave(notFoundError("Transaction could not be found."))
                            return@launch
                        }
                        id
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        form = null,
                        isSaving = false,
                        formError = null,
                    )
                }
                _effects.emit(TransactionUiEffect.Saved(transactionId))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DomainValidationException) {
                failSave(validationError(exception))
            } catch (_: Exception) {
                failSave(operationError("Unable to save the transaction."))
            }
        }
    }

    private fun failSave(error: TransactionUiError) {
        _uiState.update { state ->
            state.copy(
                isSaving = false,
                formError = error,
            )
        }
    }

    private fun requestDelete(transactionId: String) {
        val current = _uiState.value
        if (current.isSaving || current.deletingTransactionId != null) {
            return
        }

        if (transactionId.isBlank()) {
            _uiState.update { state ->
                state.copy(deleteError = operationError("Unable to delete the transaction."))
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                pendingDeleteId = transactionId,
                deleteError = null,
            )
        }
    }

    private fun confirmDelete() {
        val transactionId = _uiState.value.pendingDeleteId ?: return
        if (_uiState.value.deletingTransactionId != null) {
            return
        }

        _uiState.update { state ->
            state.copy(
                pendingDeleteId = null,
                deletingTransactionId = transactionId,
                deleteError = null,
            )
        }

        viewModelScope.launch {
            try {
                if (deleteTransactionUseCase(transactionId)) {
                    _uiState.update { state ->
                        state.copy(deletingTransactionId = null)
                    }
                    _effects.emit(TransactionUiEffect.Deleted(transactionId))
                } else {
                    _uiState.update { state ->
                        state.copy(
                            deletingTransactionId = null,
                            deleteError = notFoundError("Transaction could not be found."),
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { state ->
                    state.copy(
                        deletingTransactionId = null,
                        deleteError = operationError("Unable to delete the transaction."),
                    )
                }
            }
        }
    }

    private fun cancelDelete() {
        if (_uiState.value.deletingTransactionId != null) {
            return
        }

        _uiState.update { state ->
            state.copy(pendingDeleteId = null, deleteError = null)
        }
    }

    private fun clearErrors() {
        _uiState.update { state ->
            state.copy(
                listError = null,
                formError = null,
                deleteError = null,
                form = state.form?.copy(
                    amountError = null,
                    categoryError = null,
                    dateError = null,
                    noteError = null,
                ),
            )
        }
    }

    private fun today(): LocalDate = clock.instant().atZone(zoneId).toLocalDate()

    private fun toUiModel(transaction: Transaction): TransactionListItemUiModel =
        TransactionListItemUiModel(
            id = transaction.id,
            amountMinorUnits = transaction.amount.minorUnits,
            currencyCode = transaction.amount.currencyCode,
            type = transaction.type,
            categoryId = transaction.categoryId.value,
            categoryLabel = CategoryCatalogue.find(transaction.categoryId)?.displayName
                ?: transaction.categoryId.value,
            date = transaction.transactionDate,
            note = transaction.note,
        )

    private fun Transaction.toFormState(): TransactionFormUiState {
        val currencyDefinition = CurrencyCatalogue.requireSupported(amount.currencyCode)
        val amountText = BigDecimal.valueOf(
            amount.minorUnits,
            currencyDefinition.fractionDigits,
        ).setScale(currencyDefinition.fractionDigits).toPlainString()

        return TransactionFormUiState(
            mode = TransactionFormMode.EDIT,
            transactionId = id,
            amountText = amountText,
            currencyCode = amount.currencyCode,
            type = type,
            categoryId = categoryId.value,
            date = transactionDate,
            noteText = note.orEmpty(),
        )
    }

    private fun TransactionFormUiState.toDraft(): DraftBuildResult {
        val amountInput = amountText.trim()
        if (amountInput.isEmpty()) {
            return DraftBuildResult.Invalid(copy(amountError = "Enter an amount."))
        }

        val decimalAmount = amountInput.toBigDecimalOrNull()
            ?: return DraftBuildResult.Invalid(
                copy(amountError = "Enter a valid amount."),
            )

        val money = try {
            Money.fromDecimal(decimalAmount, currencyCode)
        } catch (exception: DomainValidationException) {
            return DraftBuildResult.Invalid(
                copy(amountError = exception.message ?: "Enter a valid amount."),
            )
        }

        val rawCategoryId = categoryId?.trim().orEmpty()
        if (rawCategoryId.isEmpty()) {
            return DraftBuildResult.Invalid(copy(categoryError = "Select a category."))
        }

        val typedCategoryId = try {
            CategoryId.of(rawCategoryId)
        } catch (exception: DomainValidationException) {
            return DraftBuildResult.Invalid(
                copy(categoryError = exception.message ?: "Select a valid category."),
            )
        }

        val selectedDate = date
            ?: return DraftBuildResult.Invalid(copy(dateError = "Select a date."))

        return DraftBuildResult.Valid(
            TransactionDraft(
                amount = money,
                type = type,
                categoryId = typedCategoryId,
                transactionDate = selectedDate,
                note = noteText,
            ),
        )
    }

    private fun validationError(exception: DomainValidationException): TransactionUiError =
        TransactionUiError(
            message = exception.message ?: "Please check the transaction details.",
            kind = TransactionUiErrorKind.VALIDATION,
        )

    private fun operationError(message: String): TransactionUiError =
        TransactionUiError(message = message, kind = TransactionUiErrorKind.OPERATION)

    private fun notFoundError(message: String): TransactionUiError =
        TransactionUiError(message = message, kind = TransactionUiErrorKind.NOT_FOUND)

    private sealed interface DraftBuildResult {
        data class Valid(val draft: TransactionDraft) : DraftBuildResult
        data class Invalid(val form: TransactionFormUiState) : DraftBuildResult
    }
}
