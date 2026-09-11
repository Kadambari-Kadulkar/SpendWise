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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens
import java.time.LocalDate
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

@Composable
fun TransactionRoute(
    viewModel: TransactionViewModel = hiltViewModel(),
    onEffect: (TransactionUiEffect) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            onEffect(effect)
        }
    }

    TransactionListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TransactionListScreen(
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
    }
}

@Composable
private fun TransactionListContent(
    uiState: TransactionUiState,
    onEvent: (TransactionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTransactions = uiState.transactions.isNotEmpty()

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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        vertical = SpendWiseDimens.space8,
                    ),
                ) {
                    items(
                        items = uiState.transactions,
                        key = { transaction -> transaction.id },
                    ) { transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            onClick = {
                                onEvent(TransactionUiEvent.StartEdit(transaction.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionListItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountText = TransactionAmountFormatter.format(
        amountMinorUnits = transaction.amountMinorUnits,
        currencyCode = transaction.currencyCode,
        type = transaction.type,
    )
    val typeText = transaction.type.displayLabel()
    val dateText = transaction.date.toDisplayText()
    val accessibilityDescription = buildString {
        append("Edit ")
        append(transaction.categoryLabel)
        append(", ")
        append(typeText)
        append(", ")
        append(amountText)
        append(", dated ")
        append(dateText)
        transaction.note?.let { note ->
            append(", note: ")
            append(note)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = accessibilityDescription
                }
                .padding(
                    horizontal = SpendWiseDimens.screenHorizontalPadding,
                    vertical = SpendWiseDimens.listItemVerticalPadding,
                ),
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
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

private fun LocalDate.toDisplayText(): String = format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.UK),
)
