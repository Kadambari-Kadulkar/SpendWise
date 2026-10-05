package com.kadambari.spendwise.presentation.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.kadambari.spendwise.domain.model.CategoryId
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens

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
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoryPickerOpen by remember { mutableStateOf(false) }

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
