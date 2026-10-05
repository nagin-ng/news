package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController, viewModel: MainViewModel) {
    val bills by viewModel.allBills.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (bills.isEmpty()) {
                Text("No data available for analytics.")
            } else {
                val totalUnits = bills.sumOf { it.unitsConsumed }
                val totalCost = bills.sumOf { it.totalAmount }
                val avgUnits = if (bills.isNotEmpty()) totalUnits / bills.size else 0.0
                
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Bills: ${bills.size}")
                        Text("Total Units: ${String.format("%.2f", totalUnits)} kWh")
                        Text("Total Cost: ₹${String.format("%.2f", totalCost)}")
                        Text("Average Units/Bill: ${String.format("%.2f", avgUnits)} kWh")
                    }
                }
            }
        }
    }
}
