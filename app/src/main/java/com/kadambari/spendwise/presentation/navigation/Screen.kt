package com.kadambari.spendwise.presentation.navigation

sealed class Screen (val route: String){
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Analytics : Screen("analytics")
    data object Budget : Screen("budget")
    data object Settings : Screen("settings")
}