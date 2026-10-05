package com.kadambari.spendwise.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.kadambari.spendwise.presentation.transaction.TransactionFormMode
import com.kadambari.spendwise.presentation.transaction.TransactionFormRoute
import com.kadambari.spendwise.presentation.transaction.TransactionListRoute
import com.kadambari.spendwise.presentation.transaction.TransactionViewModel

@Composable
fun SpendWiseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        navigation(
            route = Screen.Transactions.route,
            startDestination = Screen.TransactionList.route
        ) {
            composable(Screen.TransactionList.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Transactions.route)
                }
                val viewModel: TransactionViewModel = hiltViewModel(parentEntry)
                TransactionListRoute(
                    viewModel = viewModel,
                    onNavigateToAdd = {
                        navController.navigate(Screen.TransactionAdd.route)
                    },
                    onNavigateToEdit = { transactionId ->
                        navController.navigate(Screen.TransactionEdit.createRoute(transactionId))
                    }
                )
            }
            composable(Screen.TransactionAdd.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Transactions.route)
                }
                val viewModel: TransactionViewModel = hiltViewModel(parentEntry)
                TransactionFormRoute(
                    mode = TransactionFormMode.ADD,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.TransactionEdit.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId").orEmpty()
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Transactions.route)
                }
                val viewModel: TransactionViewModel = hiltViewModel(parentEntry)
                TransactionFormRoute(
                    mode = TransactionFormMode.EDIT,
                    transactionId = transactionId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }
        composable(Screen.Budget.route) {
            BudgetScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Dashboard")
    }
}

@Composable
fun AnalyticsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Analytics")
    }
}

@Composable
fun BudgetScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Budget")
    }
}

@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings")
    }
}
