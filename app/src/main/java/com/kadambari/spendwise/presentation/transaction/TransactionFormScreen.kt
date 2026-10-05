package com.kadambari.spendwise.presentation.transaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
