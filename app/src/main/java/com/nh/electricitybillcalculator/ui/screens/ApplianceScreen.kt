package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.data.local.entity.ApplianceEntity
import com.nh.electricitybillcalculator.domain.calculator.CalculationEngine
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceScreen(navController: NavController, viewModel: MainViewModel) {
    val appliances by viewModel.allAppliances.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Appliance Calculator") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Appliance")
            }
        }
    ) { padding ->
        if (appliances.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No appliances added.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(appliances) { appliance ->
                    val dailyKwh = CalculationEngine.calculateApplianceDailyKwh(appliance.watts, appliance.quantity, appliance.hoursPerDay)
                    val monthlyKwh = CalculationEngine.calculateApplianceMonthlyKwh(dailyKwh, appliance.daysPerMonth)
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appliance.name, style = MaterialTheme.typography.titleMedium)
                                Text("${appliance.watts}W x ${appliance.quantity} | ${appliance.hoursPerDay} hrs/day")
                                Text("Monthly: ${String.format("%.2f", monthlyKwh)} kWh", color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteAppliance(appliance) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddApplianceDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { viewModel.insertAppliance(it); showAddDialog = false }
            )
        }
    }
}

@Composable
fun AddApplianceDialog(onDismiss: () -> Unit, onAdd: (ApplianceEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var watts by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var hours by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Appliance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = watts, onValueChange = { watts = it }, label = { Text("Watts") })
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") })
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Hours/Day") })
                OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Days/Month") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = watts.toDoubleOrNull() ?: 0.0
                val q = quantity.toIntOrNull() ?: 1
                val h = hours.toDoubleOrNull() ?: 0.0
                val d = days.toIntOrNull() ?: 30
                if (name.isNotBlank() && w > 0) {
                    onAdd(ApplianceEntity(name = name, watts = w, quantity = q, hoursPerDay = h, daysPerMonth = d))
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
