package com.kadambari.spendwise.presentation.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val TRANSACTIONS_TITLE = "Transactions"
private const val ADD_TRANSACTION_LABEL = "Add transaction"
private const val LOADING_TRANSACTIONS_LABEL = "Loading transactions..."
private const val LIST_ERROR_TITLE = "We couldn't load your transactions."
private const val LIST_ERROR_SUPPORTING_TEXT = "Please try again."
private const val EMPTY_TITLE = "No transactions yet."
private const val EMPTY_SUPPORTING_TEXT =
    "Add your first income or expense to start tracking your money."
private const val FILTERED_EMPTY_TITLE = "No transactions match your filters."
private const val FILTERED_EMPTY_SUPPORTING_TEXT =
    "Try adjusting your search or clearing filters to see your transactions."

@Composable
fun TransactionListRoute(
    viewModel: TransactionViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransactionUiEffect.Deleted -> {
                    snackbarHostState.showSnackbar(
                        message = "Transaction deleted",
                    )
                }
                else -> Unit
            }
        }
    }

    TransactionListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = { event ->
            when (event) {
                TransactionUiEvent.StartAdd -> onNavigateToAdd()
                is TransactionUiEvent.StartEdit -> onNavigateToEdit(event.transactionId)
                else -> viewModel.onEvent(event)
            }
        },
    )
}

@Composable
fun TransactionRoute(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    onEffect: (TransactionUiEffect) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is TransactionUiEffect.Deleted) {
                snackbarHostState.showSnackbar(
                    message = "Transaction deleted",
                )
            }
            onEffect(effect)
        }
    }

    TransactionListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = { event ->
            when (event) {
                TransactionUiEvent.StartAdd -> onNavigateToAdd()
                is TransactionUiEvent.StartEdit -> onNavigateToEdit(event.transactionId)
                else -> viewModel.onEvent(event)
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TransactionListScreen(
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    LaunchedEffect(uiState.deleteError) {
        val error = uiState.deleteError
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error.message,
            )
            onEvent(TransactionUiEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text(TRANSACTIONS_TITLE) },
                actions = {
                    IconButton(
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                        onClick = { onEvent(TransactionUiEvent.StartAdd) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = ADD_TRANSACTION_LABEL,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        TransactionListContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )

        if (uiState.pendingDeleteId != null) {
            TransactionDeleteConfirmationDialog(
                onConfirm = { onEvent(TransactionUiEvent.DeleteConfirmed) },
                onDismiss = { onEvent(TransactionUiEvent.DeleteCancelled) },
            )
        }
    }
}

@Composable
private fun TransactionListContent(
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTransactions = uiState.transactions.isNotEmpty()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    when {
        uiState.isListLoading && !hasTransactions && uiState.listError == null -> {
            TransactionLoadingState(modifier = modifier)
        }

        uiState.listError != null && !hasTransactions -> {
            TransactionListErrorState(
                modifier = modifier,
                onRetry = { onEvent(TransactionUiEvent.RetryList) },
            )
        }

        !uiState.isListLoading && uiState.listError == null && !hasTransactions -> {
            TransactionEmptyState(
                modifier = modifier,
                onAddTransaction = { onEvent(TransactionUiEvent.StartAdd) },
            )
        }

        else -> {
            Column(modifier = modifier) {
                TransactionSearchBarAndFilter(
                    searchQuery = uiState.filter.searchQuery,
                    hasActiveFilters = uiState.hasActiveFilters,
                    onSearchQueryChange = { onEvent(TransactionUiEvent.SearchQueryChanged(it)) },
                    onOpenFilters = { showFilterSheet = true },
                )

                if (uiState.hasActiveFilters) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = SpendWiseDimens.screenHorizontalPadding,
                                end = SpendWiseDimens.screenHorizontalPadding,
                                bottom = SpendWiseDimens.space4,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Filtered results",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = { onEvent(TransactionUiEvent.ClearFilters) },
                        ) {
                            Text("Clear all")
                        }
                    }
                }

                if (uiState.listError != null) {
                    TransactionInlineListError(
                        onRetry = { onEvent(TransactionUiEvent.RetryList) },
                    )
                }

                if (uiState.isListLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = LOADING_TRANSACTIONS_LABEL
                            },
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (uiState.filteredTransactions.isEmpty()) {
                    TransactionFilteredEmptyState(
                        modifier = Modifier.weight(1f),
                        onClearFilters = { onEvent(TransactionUiEvent.ClearFilters) },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            vertical = SpendWiseDimens.space8,
                        ),
                    ) {
                        items(
                            items = uiState.filteredTransactions,
                            key = { transaction -> transaction.id },
                        ) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                isDeleting = uiState.deletingTransactionId == transaction.id,
                                onEdit = {
                                    onEvent(TransactionUiEvent.StartEdit(transaction.id))
                                },
                                onDelete = {
                                    onEvent(TransactionUiEvent.DeleteRequested(transaction.id))
                                },
                            )
                        }
                    }
                }
            }

            if (showFilterSheet) {
                TransactionFilterBottomSheet(
                    uiState = uiState,
                    onEvent = onEvent,
                    onDismiss = { showFilterSheet = false },
                )
            }
        }
    }
}

@Composable
private fun TransactionSearchBarAndFilter(
    searchQuery: String,
    hasActiveFilters: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SpendWiseDimens.screenHorizontalPadding,
                vertical = SpendWiseDimens.space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpendWiseDimens.space8),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = "Search transactions by category or note"
                },
            placeholder = { Text("Search category or note...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                        )
                    }
                }
            },
            singleLine = true,
        )

        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onOpenFilters,
                modifier = Modifier
                    .size(SpendWiseDimens.minimumTouchTarget)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (hasActiveFilters) {
                            "Filter transactions, active"
                        } else {
                            "Filter transactions"
                        }
                    },
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = null,
                    tint = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (hasActiveFilters) {
                Badge(
                    modifier = Modifier
                        .padding(top = SpendWiseDimens.space4, end = SpendWiseDimens.space4)
                        .size(SpendWiseDimens.space8),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionFilterBottomSheet(
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SpendWiseDimens.screenHorizontalPadding,
                    end = SpendWiseDimens.screenHorizontalPadding,
                    bottom = SpendWiseDimens.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(SpendWiseDimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filter transactions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FilterTypeSelector(
                selectedType = uiState.filter.type,
                onTypeSelected = { onEvent(TransactionUiEvent.TransactionTypeFilterChanged(it)) },
            )

            FilterCategorySelector(
                selectedCategoryId = uiState.filter.categoryId,
                selectedType = uiState.filter.type,
                onCategorySelected = { onEvent(TransactionUiEvent.CategoryFilterChanged(it)) },
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Date range",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = SpendWiseDimens.space8),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpendWiseDimens.space12),
                ) {
                    FilterDateField(
                        label = "From",
                        date = uiState.filter.dateFrom,
                        onClick = { showFromDatePicker = true },
                        onClear = { onEvent(TransactionUiEvent.DateFromChanged(null)) },
                        modifier = Modifier.weight(1f),
                    )

                    FilterDateField(
                        label = "To",
                        date = uiState.filter.dateTo,
                        onClick = { showToDatePicker = true },
                        onClear = { onEvent(TransactionUiEvent.DateToChanged(null)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (uiState.hasActiveFilters) {
                Button(
                    onClick = {
                        onEvent(TransactionUiEvent.ClearFilters)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SpendWiseDimens.minimumTouchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Clear all filters")
                }
            }
        }
    }

    if (showFromDatePicker) {
        val maxForFrom = if (uiState.filter.dateTo != null &&
            (uiState.maxSelectableDate == null || uiState.filter.dateTo <= uiState.maxSelectableDate)
        ) {
            uiState.filter.dateTo
        } else {
            uiState.maxSelectableDate
        }

        TransactionFilterDatePickerDialog(
            initialDate = uiState.filter.dateFrom ?: uiState.filter.dateTo ?: uiState.maxSelectableDate,
            minSelectableDate = null,
            maxSelectableDate = maxForFrom,
            onDateSelected = { date ->
                onEvent(TransactionUiEvent.DateFromChanged(date))
                showFromDatePicker = false
            },
            onDismiss = { showFromDatePicker = false },
        )
    }

    if (showToDatePicker) {
        TransactionFilterDatePickerDialog(
            initialDate = uiState.filter.dateTo ?: uiState.filter.dateFrom ?: uiState.maxSelectableDate,
            minSelectableDate = uiState.filter.dateFrom,
            maxSelectableDate = uiState.maxSelectableDate,
            onDateSelected = { date ->
                onEvent(TransactionUiEvent.DateToChanged(date))
                showToDatePicker = false
            },
            onDismiss = { showToDatePicker = false },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FilterTypeSelector(
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        null to "All",
        TransactionType.INCOME to "Income",
        TransactionType.EXPENSE to "Expense",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Transaction type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpendWiseDimens.space4),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (type, label) ->
                val isSelected = selectedType == type
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    modifier = Modifier.semantics {
                        role = Role.RadioButton
                        contentDescription = "$label type filter${if (isSelected) ", selected" else ""}"
                    },
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FilterCategorySelector(
    selectedCategoryId: String?,
    selectedType: TransactionType?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val categories = remember(selectedType) {
        if (selectedType != null) {
            CategoryCatalogue.forType(selectedType)
        } else {
            CategoryCatalogue.all
        }
    }

    val selectedCategoryName = remember(selectedCategoryId) {
        if (selectedCategoryId == null) {
            "All categories"
        } else {
            CategoryCatalogue.find(CategoryId.of(selectedCategoryId))?.displayName
                ?: selectedCategoryId
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpendWiseDimens.space4),
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedCategoryName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .semantics {
                        role = Role.DropdownList
                        contentDescription = "Category filter, currently $selectedCategoryName. Tap to change category."
                    },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("All categories") },
                    onClick = {
                        onCategorySelected(null)
                        expanded = false
                    },
                    leadingIcon = if (selectedCategoryId == null) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                )

                categories.forEach { category ->
                    val isSelected = category.id.value == selectedCategoryId
                    DropdownMenuItem(
                        text = { Text(category.displayName) },
                        onClick = {
                            onCategorySelected(category.id.value)
                            expanded = false
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDateField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(date) {
        date?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.UK),
        )
    }
    val displayDate = formattedDate ?: "Any"
    val isDateSelected = date != null

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpendWiseDimens.space4),
        )

        OutlinedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                .semantics {
                    role = Role.Button
                    contentDescription = if (isDateSelected) {
                        "$label date, $displayDate. Tap to change date."
                    } else {
                        "$label date, not set. Tap to select date."
                    }
                },
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SpendWiseDimens.space12,
                        vertical = SpendWiseDimens.space8,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDateSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (isDateSelected) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear $label date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(SpendWiseDimens.space16),
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(SpendWiseDimens.space16),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionFilterDatePickerDialog(
    initialDate: LocalDate?,
    minSelectableDate: LocalDate?,
    maxSelectableDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialSelectedMillis = remember(initialDate) {
        initialDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }
    val minSelectableMillis = remember(minSelectableDate) {
        minSelectableDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }
    val maxSelectableMillis = remember(maxSelectableDate) {
        maxSelectableDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val afterMin = minSelectableMillis == null || utcTimeMillis >= minSelectableMillis
                val beforeMax = maxSelectableMillis == null || utcTimeMillis <= maxSelectableMillis
                return afterMin && beforeMax
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let { utcMillis ->
                        Instant.ofEpochMilli(utcMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    if (selectedDate != null) {
                        onDateSelected(selectedDate)
                    }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionListItemUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false,
) {
    val amountText = TransactionAmountFormatter.format(
        amountMinorUnits = transaction.amountMinorUnits,
        currencyCode = transaction.currencyCode,
        type = transaction.type,
    )
    val typeText = transaction.type.displayLabel()
    val accessibilityTypeText = transaction.type.accessibilityLabel()
    val dateText = transaction.date.toDisplayText()
    val editAccessibilityDescription = buildString {
        append("Edit ")
        append(transaction.categoryLabel)
        append(", ")
        append(accessibilityTypeText)
        append(", ")
        append(amountText)
        append(", dated ")
        append(dateText)
        transaction.note?.let { note ->
            append(", note: ")
            append(note)
        }
    }
    val deleteAccessibilityDescription =
        "Delete ${transaction.categoryLabel} transaction, $amountText"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SpendWiseDimens.screenHorizontalPadding,
                    end = SpendWiseDimens.space8,
                    top = SpendWiseDimens.listItemVerticalPadding,
                    bottom = SpendWiseDimens.listItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                    .clickable(
                        onClick = onEdit,
                        role = Role.Button,
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = editAccessibilityDescription
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = SpendWiseDimens.space12),
                    ) {
                        Text(
                            text = transaction.categoryLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                            Spacer(modifier = Modifier.height(SpendWiseDimens.space4))
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = amountText,
                        modifier = Modifier.weight(0.5f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                }

                Text(
                    text = "$typeText · $dateText",
                    modifier = Modifier.padding(top = SpendWiseDimens.space8),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier
                    .size(SpendWiseDimens.minimumTouchTarget)
                    .semantics {
                        role = Role.Button
                    },
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = deleteAccessibilityDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TransactionDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete transaction?") },
        text = {
            Text("Are you sure you want to delete this transaction? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TransactionLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(SpendWiseDimens.screenHorizontalPadding)
            .semantics {
                contentDescription = LOADING_TRANSACTIONS_LABEL
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(SpendWiseDimens.space16))
        Text(
            text = LOADING_TRANSACTIONS_LABEL,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TransactionEmptyState(
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit,
) {
    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = EMPTY_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Text(
            text = EMPTY_SUPPORTING_TEXT,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space24))
        Button(onClick = onAddTransaction) {
            Text(ADD_TRANSACTION_LABEL)
        }
    }
}

@Composable
private fun TransactionFilteredEmptyState(
    modifier: Modifier = Modifier,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = FILTERED_EMPTY_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Text(
            text = FILTERED_EMPTY_SUPPORTING_TEXT,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space24))
        Button(onClick = onClearFilters) {
            Text("Clear filters")
        }
    }
}

@Composable
private fun TransactionListErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = LIST_ERROR_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Text(
            text = LIST_ERROR_SUPPORTING_TEXT,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space24))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun TransactionInlineListError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(
            horizontal = SpendWiseDimens.screenHorizontalPadding,
            vertical = SpendWiseDimens.space12,
        ),
    ) {
        Text(
            text = LIST_ERROR_TITLE,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = LIST_ERROR_SUPPORTING_TEXT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
}

private fun TransactionType.accessibilityLabel(): String = when (this) {
    TransactionType.INCOME -> "income"
    TransactionType.EXPENSE -> "expense"
}

private fun LocalDate.toDisplayText(): String = format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.UK),
)
