package com.kadambari.spendwise.presentation.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CategoryDefinition
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TransactionFormRoute(
    mode: TransactionFormMode,
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    transactionId: String? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mode, transactionId) {
        when (mode) {
            TransactionFormMode.ADD -> viewModel.onEvent(TransactionUiEvent.StartAdd)
            TransactionFormMode.EDIT -> {
                if (transactionId != null) {
                    viewModel.onEvent(TransactionUiEvent.StartEdit(transactionId))
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TransactionUiEffect.Saved -> onNavigateBack()
                else -> Unit
            }
        }
    }

    TransactionFormScreen(
        mode = mode,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TransactionFormScreen(
    mode: TransactionFormMode,
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (mode) {
        TransactionFormMode.ADD -> "Add transaction"
        TransactionFormMode.EDIT -> "Edit transaction"
    }

    val form = uiState.form

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (form == null || (mode == TransactionFormMode.EDIT && form.isLoading)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Loading transaction..."
                    },
                )
            }
        } else {
            TransactionFormContent(
                formState = form,
                maxSelectableDate = uiState.maxSelectableDate,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = SpendWiseDimens.screenHorizontalPadding,
                        vertical = SpendWiseDimens.screenVerticalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun TransactionFormContent(
    formState: TransactionFormUiState,
    maxSelectableDate: LocalDate?,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoryPickerOpen by remember { mutableStateOf(false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SpendWiseDimens.sectionSpacing),
    ) {
        // 1. Amount Field
        AmountInputField(
            amountText = formState.amountText,
            amountError = formState.amountError,
            onAmountChange = { onEvent(TransactionUiEvent.AmountChanged(it)) },
        )

        // 2. Read-only GBP Currency Display
        CurrencyDisplayField()

        // 3. Income / Expense Selection
        IncomeExpenseSelector(
            selectedType = formState.type,
            onTypeSelected = { onEvent(TransactionUiEvent.TypeChanged(it)) },
        )

        // 4. Category Selector
        CategorySelectorField(
            categoryId = formState.categoryId,
            categoryError = formState.categoryError,
            onClick = { isCategoryPickerOpen = true },
        )

        // 5. Date Selector
        DateSelectorField(
            date = formState.date,
            dateError = formState.dateError,
            onClick = { isDatePickerOpen = true },
        )

        // 6. Note Field
        NoteInputField(
            noteText = formState.noteText,
            noteError = formState.noteError,
            onNoteChange = { onEvent(TransactionUiEvent.NoteChanged(it)) },
        )

        if (isCategoryPickerOpen) {
            CategoryPickerBottomSheet(
                transactionType = formState.type,
                selectedCategoryId = formState.categoryId,
                onCategorySelected = { categoryId ->
                    onEvent(TransactionUiEvent.CategoryChanged(categoryId))
                    isCategoryPickerOpen = false
                },
                onDismiss = { isCategoryPickerOpen = false },
            )
        }

        if (isDatePickerOpen) {
            TransactionDatePickerDialog(
                initialDate = formState.date,
                maxSelectableDate = maxSelectableDate,
                onDateSelected = { selectedDate ->
                    onEvent(TransactionUiEvent.DateChanged(selectedDate))
                    isDatePickerOpen = false
                },
                onDismiss = { isDatePickerOpen = false },
            )
        }
    }
}

@Composable
private fun AmountInputField(
    amountText: String,
    amountError: String?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Amount") },
        placeholder = { Text("0.00") },
        prefix = { Text("£") },
        singleLine = true,
        isError = amountError != null,
        supportingText = amountError?.let { error ->
            {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun CurrencyDisplayField(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Currency: British Pound (GBP), read-only"
            },
    ) {
        Text(
            text = "Currency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space4))
        Text(
            text = "GBP · British Pound",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun IncomeExpenseSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Transaction type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SpendWiseDimens.space8),
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpendWiseDimens.minimumTouchTarget),
        ) {
            val isExpenseSelected = selectedType == TransactionType.EXPENSE
            val isIncomeSelected = selectedType == TransactionType.INCOME

            SegmentedButton(
                selected = isExpenseSelected,
                onClick = { onTypeSelected(TransactionType.EXPENSE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    SegmentedButtonDefaults.Icon(active = isExpenseSelected) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(SpendWiseDimens.iconSmall),
                        )
                    }
                },
                label = { Text("Expense") },
            )

            SegmentedButton(
                selected = isIncomeSelected,
                onClick = { onTypeSelected(TransactionType.INCOME) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    SegmentedButtonDefaults.Icon(active = isIncomeSelected) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(SpendWiseDimens.iconSmall),
                        )
                    }
                },
                label = { Text("Income") },
            )
        }
    }
}

@Composable
private fun CategorySelectorField(
    categoryId: String?,
    categoryError: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCategory: CategoryDefinition? = remember(categoryId) {
        if (categoryId.isNullOrBlank()) {
            null
        } else {
            CategoryCatalogue.all.find { it.id.value == categoryId }
        }
    }

    val displayCategoryName = selectedCategory?.displayName ?: "Select category"
    val isCategorySelected = selectedCategory != null
    val accessibilityDescription = if (isCategorySelected) {
        "Category, $displayCategoryName. Tap to change category."
    } else {
        "Category, not selected. Tap to select category."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = if (categoryError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = SpendWiseDimens.space4),
        )

        OutlinedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                .semantics {
                    role = Role.Button
                    contentDescription = accessibilityDescription
                },
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = if (categoryError != null) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error),
                )
            } else {
                CardDefaults.outlinedCardBorder()
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SpendWiseDimens.screenHorizontalPadding,
                        vertical = SpendWiseDimens.listItemVerticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = displayCategoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCategorySelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        categoryError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = SpendWiseDimens.screenHorizontalPadding,
                    top = SpendWiseDimens.space4,
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryPickerBottomSheet(
    transactionType: TransactionType,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = remember(transactionType) {
        CategoryCatalogue.forType(transactionType)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpendWiseDimens.screenVerticalPadding),
        ) {
            Text(
                text = "Select category",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = SpendWiseDimens.screenHorizontalPadding,
                    vertical = SpendWiseDimens.space8,
                ),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = categories,
                    key = { category -> category.id.value },
                ) { category ->
                    val isSelected = category.id.value == selectedCategoryId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                            .selectable(
                                selected = isSelected,
                                onClick = { onCategorySelected(category.id.value) },
                                role = Role.RadioButton,
                            )
                            .padding(
                                horizontal = SpendWiseDimens.screenHorizontalPadding,
                                vertical = SpendWiseDimens.listItemVerticalPadding,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun DateSelectorField(
    date: LocalDate?,
    dateError: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(date) {
        date?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.UK),
        )
    }

    val displayDate = formattedDate ?: "Select date"
    val isDateSelected = date != null
    val accessibilityDescription = if (isDateSelected) {
        "Transaction date, $displayDate. Tap to change date."
    } else {
        "Transaction date, not selected. Tap to select date."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Date",
            style = MaterialTheme.typography.labelMedium,
            color = if (dateError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = SpendWiseDimens.space4),
        )

        OutlinedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                .semantics {
                    role = Role.Button
                    contentDescription = accessibilityDescription
                },
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = if (dateError != null) {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error),
                )
            } else {
                CardDefaults.outlinedCardBorder()
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SpendWiseDimens.screenHorizontalPadding,
                        vertical = SpendWiseDimens.listItemVerticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDateSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        dateError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = SpendWiseDimens.screenHorizontalPadding,
                    top = SpendWiseDimens.space4,
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransactionDatePickerDialog(
    initialDate: LocalDate?,
    maxSelectableDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialSelectedMillis = remember(initialDate) {
        initialDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }
    val maxSelectableMillis = remember(maxSelectableDate) {
        maxSelectableDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return maxSelectableMillis == null || utcTimeMillis <= maxSelectableMillis
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
private fun NoteInputField(
    noteText: String,
    noteError: String?,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = noteText,
        onValueChange = { input ->
            if (input.length <= 500) {
                onNoteChange(input)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text("Note (optional)") },
        placeholder = { Text("Enter a note...") },
        minLines = 2,
        maxLines = 4,
        isError = noteError != null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (noteError != null) {
                    Text(
                        text = noteError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "${noteText.length}/500",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
    )
}
