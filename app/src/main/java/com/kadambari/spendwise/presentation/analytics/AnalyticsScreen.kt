package com.kadambari.spendwise.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kadambari.spendwise.domain.model.AnalyticsPeriod
import com.kadambari.spendwise.presentation.designsystem.theme.SpendWiseDimens

private const val ANALYTICS_TITLE = "Analytics"
private const val LOADING_ANALYTICS_LABEL = "Loading analytics..."
private const val ERROR_TITLE = "We couldn't load your analytics."
private const val RETRY_LABEL = "Retry"
private const val ADD_TRANSACTION_LABEL = "Add transaction"

/**
 * Stateful entry point for the SpendWise Analytics destination.
 *
 * Acquires [AnalyticsViewModel] via Hilt, observes [AnalyticsUiState] as Compose state,
 * and delegates UI events and navigation callbacks to [AnalyticsScreen].
 */
@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    AnalyticsScreen(
        uiState = uiState,
        onPeriodSelected = { period ->
            viewModel.onEvent(AnalyticsUiEvent.PeriodSelected(period))
        },
        onRetry = {
            viewModel.onEvent(AnalyticsUiEvent.Retry)
        },
        onNavigateToAdd = onNavigateToAdd,
    )
}

/**
 * Stateless screen Composable for SpendWise Analytics.
 *
 * Handles screen structure, top bar, period selector, date range row, and state branching
 * across Loading, Error, 3-tier Empty, and Populated states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    onRetry: () -> Unit,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ANALYTICS_TITLE,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                AnalyticsLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.error != null -> {
                AnalyticsErrorState(
                    errorMessage = uiState.error.message,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.emptyType == AnalyticsEmptyType.NO_TRANSACTIONS_OVERALL ||
                uiState.emptyType == AnalyticsEmptyType.CURRENCY_MISMATCH -> {
                AnalyticsEmptyState(
                    emptyType = uiState.emptyType,
                    onAddTransaction = onNavigateToAdd,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.emptyType == AnalyticsEmptyType.NO_PERIOD_TRANSACTIONS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(
                            horizontal = SpendWiseDimens.screenHorizontalPadding,
                            vertical = SpendWiseDimens.space12,
                        ),
                ) {
                    AnalyticsPeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = onPeriodSelected,
                    )

                    Spacer(modifier = Modifier.height(SpendWiseDimens.space12))

                    AnalyticsDateRangeRow(
                        periodDateRangeFormatted = uiState.periodDateRangeFormatted,
                        comparison = uiState.comparison,
                    )

                    Spacer(modifier = Modifier.height(SpendWiseDimens.space16))

                    AnalyticsEmptyState(
                        emptyType = uiState.emptyType,
                        onAddTransaction = onNavigateToAdd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }

            else -> {
                AnalyticsPopulatedContent(
                    uiState = uiState,
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

/**
 * Populated content container for the Analytics screen.
 */
@Composable
private fun AnalyticsPopulatedContent(
    uiState: AnalyticsUiState,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
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
        AnalyticsPeriodSelector(
            selectedPeriod = uiState.selectedPeriod,
            onPeriodSelected = onPeriodSelected,
        )

        Spacer(modifier = Modifier.height(SpendWiseDimens.space12))

        AnalyticsDateRangeRow(
            periodDateRangeFormatted = uiState.periodDateRangeFormatted,
            comparison = uiState.comparison,
        )

        Spacer(modifier = Modifier.height(SpendWiseDimens.sectionSpacing))

        // TEMPORARY PLACEHOLDER FOR STAGE 2C-1:
        // Displays screen header, period selector, and date range row.
        // Summary cards, trend chart, and expense categories will be implemented in Stage 2C-2 and Stage 2C-3.
    }
}

/**
 * Period selector control supporting This Month, Last Month, and Last 3 Months.
 *
 * Enforces a visual height of 40dp and a 48dp minimum interactive touch target via
 * [Modifier.minimumInteractiveComponentSize].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsPeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = listOf(
        AnalyticsPeriod.THIS_MONTH to "This Month",
        AnalyticsPeriod.LAST_MONTH to "Last Month",
        AnalyticsPeriod.LAST_THREE_MONTHS to "Last 3 Months",
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        periods.forEachIndexed { index, (period, label) ->
            val isSelected = selectedPeriod == period
            SegmentedButton(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = periods.size,
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .height(40.dp)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "$label period${if (isSelected) ", selected" else ""}"
                    },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Date range row displaying the reporting period and comparison baseline.
 *
 * Displays clean text without an info icon, matching the approved mock-up.
 */
@Composable
fun AnalyticsDateRangeRow(
    periodDateRangeFormatted: String,
    comparison: ComparisonUiModel?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = periodDateRangeFormatted,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (comparison != null) {
            Text(
                text = comparison.baselineDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnalyticsLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(SpendWiseDimens.screenHorizontalPadding)
            .semantics(mergeDescendants = true) {
                contentDescription = LOADING_ANALYTICS_LABEL
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space16))
        Text(
            text = LOADING_ANALYTICS_LABEL,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AnalyticsErrorState(
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
            Text(RETRY_LABEL)
        }
    }
}

@Composable
private fun AnalyticsEmptyState(
    emptyType: AnalyticsEmptyType,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, supportingText) = when (emptyType) {
        AnalyticsEmptyType.NO_TRANSACTIONS_OVERALL -> {
            "No transactions yet" to "Add your first income or expense to start seeing financial insights."
        }
        AnalyticsEmptyType.CURRENCY_MISMATCH -> {
            "No GBP transactions" to "Analytics currently supports GBP transactions. Add transactions in GBP to view analytics."
        }
        AnalyticsEmptyType.NO_PERIOD_TRANSACTIONS -> {
            "No activity in this period" to "No transactions were recorded for the selected period. Switch periods or add a transaction."
        }
        AnalyticsEmptyType.NONE -> {
            "" to ""
        }
    }

    Column(
        modifier = modifier.padding(SpendWiseDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(SpendWiseDimens.space8))
        Text(
            text = supportingText,
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
