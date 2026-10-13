package com.kadambari.spendwise.presentation.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kadambari.spendwise.domain.model.CategoryCatalogue
import com.kadambari.spendwise.domain.model.CategoryDefinition
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.components.CategoryIconBadge
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        contentDescription = "Loading transaction..."
                    },
                )
            }
        } else {
            TransactionFormContent(
                formState = form,
                isSaving = uiState.isSaving,
                formError = uiState.formError,
                maxSelectableDate = uiState.maxSelectableDate,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = SpendWiseDimens.screenHorizontalPadding,
                        vertical = SpendWiseDimens.space12,
                    ),
            )
        }
    }
}

@Composable
private fun TransactionFormContent(
    formState: TransactionFormUiState,
    isSaving: Boolean,
    formError: TransactionUiError?,
    maxSelectableDate: LocalDate?,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoryPickerOpen by remember { mutableStateOf(false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SpendWiseDimens.space16),
    ) {
        // 1. Hero Amount Input Card
        AmountInputField(
            amountText = formState.amountText,
            amountError = formState.amountError,
            onAmountChange = { onEvent(TransactionUiEvent.AmountChanged(it)) },
        )

        // 2. Read-only GBP Currency Display
        CurrencyDisplayField()

        // 3. Income / Expense Type Selection
        IncomeExpenseSelector(
            selectedType = formState.type,
            onTypeSelected = { onEvent(TransactionUiEvent.TypeChanged(it)) },
        )

        // 4. Details Group Card: Category & Date
        FormDetailsCard(
            categoryId = formState.categoryId,
            categoryError = formState.categoryError,
            transactionType = formState.type,
            date = formState.date,
            dateError = formState.dateError,
            onCategoryClick = { isCategoryPickerOpen = true },
            onDateClick = { isDatePickerOpen = true },
        )

        // 5. Note Field
        NoteInputField(
            noteText = formState.noteText,
            noteError = formState.noteError,
            onNoteChange = { onEvent(TransactionUiEvent.NoteChanged(it)) },
        )

        // Form-level error banner
        if (formError != null) {
            FormErrorBanner(message = formError.message)
        }

        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))

        // 6. Primary Save Action
        SaveButton(
            isSaving = isSaving,
            onClick = { onEvent(TransactionUiEvent.SaveClicked) },
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
        prefix = {
            Text(
                text = "£",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        isError = amountError != null,
        supportingText = amountError?.let { error ->
            {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun CurrencyDisplayField(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Currency: British Pound (GBP), read-only"
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "GBP · British Pound",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
                label = {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    )
                },
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
                label = {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    )
                },
            )
        }
    }
}

@Composable
private fun FormDetailsCard(
    categoryId: String?,
    categoryError: String?,
    transactionType: TransactionType,
    date: LocalDate?,
    dateError: String?,
    onCategoryClick: () -> Unit,
    onDateClick: () -> Unit,
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
    val categoryAccessibilityDescription = if (isCategorySelected) {
        "Category, $displayCategoryName. Tap to change category."
    } else {
        "Category, not selected. Tap to select category."
    }

    val formattedDate = remember(date) {
        date?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.UK),
        )
    }
    val displayDate = formattedDate ?: "Select date"
    val isDateSelected = date != null
    val dateAccessibilityDescription = if (isDateSelected) {
        "Transaction date, $displayDate. Tap to change date."
    } else {
        "Transaction date, not selected. Tap to select date."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(
                    if (categoryError != null || dateError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                    },
                ),
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                        .clickable(onClick = onCategoryClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = categoryAccessibilityDescription
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        CategoryIconBadge(
                            categoryId = categoryId,
                            transactionType = transactionType,
                            size = 36.dp,
                            iconSize = 18.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (categoryError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayCategoryName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = if (isCategorySelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )

                // Date Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                        .clickable(onClick = onDateClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = dateAccessibilityDescription
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = if (isDateSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (categoryError != null) {
            Text(
                text = categoryError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = SpendWiseDimens.space4),
            )
        }

        if (dateError != null) {
            Text(
                text = dateError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = SpendWiseDimens.space4),
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpendWiseDimens.screenVerticalPadding),
        ) {
            Text(
                text = "Select category",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = SpendWiseDimens.screenHorizontalPadding,
                    vertical = SpendWiseDimens.space12,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryIconBadge(
                                categoryId = category.id.value,
                                transactionType = category.transactionType,
                                size = 36.dp,
                                iconSize = 18.dp,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
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
        shape = RoundedCornerShape(16.dp),
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
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun FormErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.error),
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(
                horizontal = SpendWiseDimens.screenHorizontalPadding,
                vertical = SpendWiseDimens.listItemVerticalPadding,
            ),
        )
    }
}

@Composable
private fun SaveButton(
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SpendWiseDimens.minimumTouchTarget),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpendWiseDimens.iconSmall),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.size(SpendWiseDimens.space8))
            Text(
                text = "Saving…",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        } else {
            Text(
                text = "Save",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
