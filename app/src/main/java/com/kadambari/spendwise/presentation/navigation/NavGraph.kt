package com.kadambari.spendwise.presentation.navigation


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DashboardScreen(){
    Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
    {
        Text("Dashboard")
    }
}

@Composable
fun TransactionScreen(){
    Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Transaction") }
}

@Composable
fun AnalyticsScreen(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Text("Analytics")
    }
}

@Composable
fun BudgetScreen(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){Text("Budget")}
}

@Composable
fun SettingsScreen(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){Text("Settings")}
}