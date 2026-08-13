package com.kadambari.spendwise.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        label = "Dashboard",
        icon = Icons.Outlined.Home,
        contentDescription = "Open dashboard"
    ),
    BottomNavItem(
        screen = Screen.Transactions,
        label = "Transactions",
        icon = Icons.Outlined.ReceiptLong,
        contentDescription = "Open transactions"
    ),
    BottomNavItem(
        screen = Screen.Analytics,
        label = "Analytics",
        icon = Icons.Outlined.PieChart,
        contentDescription = "Open analytics"
    ),
    BottomNavItem(
        screen = Screen.Budget,
        label = "Budget",
        icon = Icons.Outlined.AccountBalanceWallet,
        contentDescription = "Open budget"
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Outlined.Settings,
        contentDescription = "Open settings"
    )
)
