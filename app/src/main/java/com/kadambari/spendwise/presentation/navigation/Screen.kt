package com.kadambari.spendwise.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object TransactionList : Screen("transactions/list")
    data object TransactionAdd : Screen("transactions/add")
    data object TransactionEdit : Screen("transactions/edit/{transactionId}") {
        fun createRoute(transactionId: String): String = "transactions/edit/$transactionId"
    }
    data object Analytics : Screen("analytics")
    data object Budget : Screen("budget")
    data object Settings : Screen("settings")
}