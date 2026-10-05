package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.domain.calculator.CalculationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerCalculatorScreen(navController: NavController) {
    var watts by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("30") }
    var rate by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Power Calculator") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = watts, onValueChange = { watts = it }, label = { Text("Power (Watts)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Hours/Day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Days") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Electricity Rate (₹/kWh)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val w = watts.toDoubleOrNull() ?: 0.0
            val h = hours.toDoubleOrNull() ?: 0.0
            val d = days.toIntOrNull() ?: 0
            val r = rate.toDoubleOrNull() ?: 0.0
            
            val dailyKwh = CalculationEngine.calculateApplianceDailyKwh(w, 1, h)
            val monthlyKwh = CalculationEngine.calculateApplianceMonthlyKwh(dailyKwh, d)
            val dailyCost = CalculationEngine.calculateApplianceMonthlyCost(dailyKwh, r)
            val monthlyCost = CalculationEngine.calculateApplianceMonthlyCost(monthlyKwh, r)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Consumption: ${String.format("%.2f", dailyKwh)} kWh")
                    Text("Total Consumption: ${String.format("%.2f", monthlyKwh)} kWh")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Daily Cost: ₹${String.format("%.2f", dailyCost)}")
                    Text("Total Cost: ₹${String.format("%.2f", monthlyCost)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
