package com.kadambari.spendwise.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
        composable(Screen.Transactions.route) {
            TransactionScreen()
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
fun TransactionScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Transactions")
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
