package com.kadambari.spendwise.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kadambari.spendwise.domain.model.CurrencyCode
import com.kadambari.spendwise.domain.model.TransactionType
import com.kadambari.spendwise.presentation.designsystem.components.CategoryIconBadge
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseColors
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens
import com.kadambari.spendwise.presentation.transaction.TransactionAmountFormatter
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private const val DASHBOARD_TITLE = "Dashboard"
private const val ADD_TRANSACTION_LABEL = "Add transaction"
private const val LOADING_DASHBOARD_LABEL = "Loading dashboard..."
private const val ERROR_TITLE = "We couldn't load your dashboard."
private const val EMPTY_TITLE = "No transactions yet"
private const val EMPTY_SUPPORTING_TEXT =
    "Add your first income or expense to start tracking your money."

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(DashboardUiEvent.Resume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DashboardScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToTransactions = onNavigateToTransactions,
        onNavigateToEdit = onNavigateToEdit,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(
    uiState: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = DASHBOARD_TITLE,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                        onClick = onNavigateToAdd,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = ADD_TRANSACTION_LABEL,
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
        when {
            uiState.isLoading -> {
                DashboardLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.error != null -> {
                DashboardErrorState(
                    errorMessage = uiState.error.message,
                    onRetry = { onEvent(DashboardUiEvent.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.hasNoTransactions -> {
                DashboardEmptyState(
                    onAddTransaction = onNavigateToAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                DashboardContent(
                    uiState = uiState,
                    onOpenMonthPicker = { showMonthPicker = true },
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToAdd = onNavigateToAdd,
                    onNavigateToEdit = onNavigateToEdit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }

        if (showMonthPicker) {
            MonthYearPickerDialog(
                currentMonth = uiState.selectedMonth,
                onMonthSelected = { selected ->
                    onEvent(DashboardUiEvent.MonthChanged(selected))
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false },
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onOpenMonthPicker: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = SpendWiseDimens.screenHorizontalPadding,
                vertical = SpendWiseDimens.space12,
            ),
    ) {
        // 1. Greeting Header
        Text(
            text = uiState.greeting,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(SpendWiseDimens.space16))

        // 2. Cohesive Financial Overview Card
        FinancialOverviewCard(
            balanceMinorUnits = uiState.balanceMinorUnits,
            currencyCode = uiState.currencyCode,
            selectedMonth = uiState.selectedMonth,
            incomeMinorUnits = uiState.monthlyIncomeMinorUnits,
            expensesMinorUnits = uiState.monthlyExpensesMinorUnits,
            onOpenMonthPicker = onOpenMonthPicker,
        )

        Spacer(modifier = Modifier.height(SpendWiseDimens.sectionSpacing))

        // 3. Recent Transactions Section
        RecentTransactionsSection(
            recentTransactions = uiState.recentTransactions,
            hasNoMonthlyTransactions = uiState.hasNoMonthlyTransactions,
            selectedMonth = uiState.selectedMonth,
            onSeeAllClick = onNavigateToTransactions,
            onAddTransactionClick = onNavigateToAdd,
            onTransactionClick = onNavigateToEdit,
        )
    }
}

@Composable
private fun FinancialOverviewCard(
    balanceMinorUnits: Long,
    currencyCode: CurrencyCode,
    selectedMonth: YearMonth,
    incomeMinorUnits: Long,
    expensesMinorUnits: Long,
    onOpenMonthPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthYearText = remember(selectedMonth) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK))
    }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpendWiseDimens.cardPadding),
        ) {
            // Balance Top Row with Integrated Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(SpendWiseDimens.space4))
                    Text(
                        text = DashboardAmountFormatter.formatBalance(balanceMinorUnits, currencyCode),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Clean Integrated Month Selector Chip
                Surface(
                    onClick = onOpenMonthPicker,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Selected month, $monthYearText."
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = monthYearText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(SpendWiseDimens.space4))
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(SpendWiseDimens.iconSmall),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpendWiseDimens.space16))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(SpendWiseDimens.space16))

            // Dual Cashflow Overview (Income & Expense Duo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpendWiseDimens.space12),
            ) {
                // Income Column
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SpendWiseColors.incomeContainer().copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(SpendWiseColors.incomeContainer(), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = SpendWiseColors.income(),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(SpendWiseDimens.space8))
                        Column {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DashboardAmountFormatter.formatMonthlyTotal(
                                    amountMinorUnits = incomeMinorUnits,
                                    currencyCode = currencyCode,
                                    type = TransactionType.INCOME,
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SpendWiseColors.income(),
                            )
                        }
                    }
                }

                // Expenses Column
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SpendWiseColors.expenseContainer().copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(SpendWiseColors.expenseContainer(), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = SpendWiseColors.expense(),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(SpendWiseDimens.space8))
                        Column {
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DashboardAmountFormatter.formatMonthlyTotal(
                                    amountMinorUnits = expensesMinorUnits,
                                    currencyCode = currencyCode,
                                    type = TransactionType.EXPENSE,
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SpendWiseColors.expense(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    recentTransactions: List<DashboardRecentTransactionUiModel>,
    hasNoMonthlyTransactions: Boolean,
    selectedMonth: YearMonth,
    onSeeAllClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent transactions",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onSeeAllClick,
                modifier = Modifier.heightIn(min = SpendWiseDimens.minimumTouchTarget),
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                )
            }
        }

        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))

        if (hasNoMonthlyTransactions) {
            val monthText = remember(selectedMonth) {
                selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK))
            }
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SpendWiseDimens.space24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No transactions for $monthText",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(SpendWiseDimens.space4))
                    Text(
                        text = "Transactions added for this month will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(SpendWiseDimens.space16))
                    Button(
                        onClick = onAddTransactionClick,
                        modifier = Modifier.heightIn(min = SpendWiseDimens.minimumTouchTarget),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(ADD_TRANSACTION_LABEL)
                    }
                }
            }
        } else {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    recentTransactions.forEachIndexed { index, item ->
                        DashboardRecentTransactionItem(
                            transaction = item,
                            onClick = { onTransactionClick(item.id) },
                        )
                        if (index < recentTransactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRecentTransactionItem(
    transaction: DashboardRecentTransactionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountText = remember(transaction.amountMinorUnits, transaction.currencyCode, transaction.type) {
        TransactionAmountFormatter.format(
            amountMinorUnits = transaction.amountMinorUnits,
            currencyCode = transaction.currencyCode,
            type = transaction.type,
        )
    }
    val dateText = remember(transaction.date) {
        transaction.date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.UK),
        )
    }
    val dateLongText = remember(transaction.date) {
        transaction.date.toDisplayText()
    }

    val amountAccessibilityText = remember(transaction.type, amountText) {
        when (transaction.type) {
            TransactionType.INCOME -> "plus " + amountText.removePrefix("+")
            TransactionType.EXPENSE -> "minus " + amountText.removePrefix("-")
        }
    }
    val noteDescription = remember(transaction.note) {
        transaction.note
            ?.takeIf { it.isNotBlank() }
            ?.let { ", note: $it" }
            .orEmpty()
    }
    val editAccessibilityDescription = remember(
        transaction.categoryLabel,
        transaction.type,
        amountAccessibilityText,
        dateLongText,
        noteDescription,
    ) {
        "Edit transaction. " +
            "${transaction.categoryLabel}, " +
            "${transaction.type.accessibilityLabel()}, " +
            "$amountAccessibilityText, " +
            "dated $dateLongText" +
            "$noteDescription."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SpendWiseDimens.minimumTouchTarget)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = editAccessibilityDescription
            }
            .padding(
                horizontal = SpendWiseDimens.cardPadding,
                vertical = SpendWiseDimens.listItemVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Category Icon Avatar
        CategoryIconBadge(
            categoryId = transaction.categoryId,
            transactionType = transaction.type,
            size = 40.dp,
            iconSize = 20.dp,
        )

        Spacer(modifier = Modifier.width(SpendWiseDimens.space12))

        // Title and Metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = SpendWiseDimens.space12),
        ) {
            Text(
                text = transaction.categoryLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Formatted Amount
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayYear by rememberSaveable { mutableIntStateOf(currentMonth.year) }
    val months = remember {
        listOf(
            Month.JANUARY to "Jan",
            Month.FEBRUARY to "Feb",
            Month.MARCH to "Mar",
            Month.APRIL to "Apr",
            Month.MAY to "May",
            Month.JUNE to "Jun",
            Month.JULY to "Jul",
            Month.AUGUST to "Aug",
            Month.SEPTEMBER to "Sep",
            Month.OCTOBER to "Oct",
            Month.NOVEMBER to "Nov",
            Month.DECEMBER to "Dec",
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { displayYear -= 1 },
                    modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous year",
                    )
                }

                Text(
                    text = displayYear.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(
                    onClick = { displayYear += 1 },
                    modifier = Modifier.size(SpendWiseDimens.minimumTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next year",
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpendWiseDimens.space8),
            ) {
                months.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpendWiseDimens.space8),
                    ) {
                        rowMonths.forEach { (month, label) ->
                            val isSelected = currentMonth.year == displayYear && currentMonth.month == month
                            val fullMonthName = month.getDisplayName(TextStyle.FULL, Locale.UK)
                            val accessibilityText = if (isSelected) {
                                "$fullMonthName, selected"
                            } else {
                                fullMonthName
                            }

                            if (isSelected) {
                                Button(
                                    onClick = { onMonthSelected(YearMonth.of(displayYear, month)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                                        .semantics {
                                            contentDescription = accessibilityText
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(label)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onMonthSelected(YearMonth.of(displayYear, month)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = SpendWiseDimens.minimumTouchTarget)
                                        .semantics {
                                            contentDescription = accessibilityText
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = SpendWiseDimens.minimumTouchTarget),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DashboardLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(SpendWiseDimens.screenHorizontalPadding)
            .semantics(mergeDescendants = true) {
                contentDescription = LOADING_DASHBOARD_LABEL
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space16))
        Text(
            text = LOADING_DASHBOARD_LABEL,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DashboardErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = ERROR_TITLE,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space24))
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = SpendWiseDimens.minimumTouchTarget),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun DashboardEmptyState(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = EMPTY_TITLE,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
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
        Button(
            onClick = onAddTransaction,
            modifier = Modifier.heightIn(min = SpendWiseDimens.minimumTouchTarget),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(ADD_TRANSACTION_LABEL)
        }
    }
}

private fun TransactionType.accessibilityLabel(): String = when (this) {
    TransactionType.INCOME -> "income"
    TransactionType.EXPENSE -> "expense"
}

private fun LocalDate.toDisplayText(): String = format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.UK),
)
