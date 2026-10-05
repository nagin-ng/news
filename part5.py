import os

files = {
"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/ApplianceScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/HistoryScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel
import com.nh.electricitybillcalculator.util.PdfGenerator
import com.nh.electricitybillcalculator.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: MainViewModel) {
    val bills by viewModel.allBills.collectAsState()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bill History") }) }
    ) { padding ->
        if (bills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your saved calculations will appear here.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(bills) { bill ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sdf.format(Date(bill.dateMillis)), style = MaterialTheme.typography.titleMedium)
                                Text("₹${String.format("%.2f", bill.totalAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${bill.unitsConsumed} Units")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { 
                                    val pdfFile = PdfGenerator.generatePdf(context, bill)
                                    if (pdfFile != null) {
                                        ShareUtil.sharePdf(context, pdfFile)
                                    }
                                }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                                }
                                IconButton(onClick = { viewModel.deleteBill(bill) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/SettingsScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Data Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear All Data")
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Data?") },
                text = { Text("This will permanently delete all bills, appliances, and readings.") },
                confirmButton = {
                    Button(onClick = { viewModel.clearAllData(); showClearDialog = false }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/MeterReadingScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

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
import com.nh.electricitybillcalculator.data.local.entity.MeterReadingEntity
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(navController: NavController, viewModel: MainViewModel) {
    val readings by viewModel.allReadings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meter Readings") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (readings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No meter readings added.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(readings) { reading ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(sdf.format(Date(reading.dateMillis)), style = MaterialTheme.typography.titleMedium)
                                Text("Reading: ${reading.reading}")
                                if (reading.note.isNotEmpty()) Text("Note: ${reading.note}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteReading(reading) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            var readingVal by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Reading") },
                text = {
                    Column {
                        OutlinedTextField(value = readingVal, onValueChange = { readingVal = it }, label = { Text("Reading") })
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val r = readingVal.toDoubleOrNull()
                        if (r != null) {
                            viewModel.insertReading(MeterReadingEntity(dateMillis = System.currentTimeMillis(), reading = r, note = note))
                            showAddDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/PowerCalculatorScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

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
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/AnalyticsScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

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
"""
}

for path, content in files.items():
    full_path = os.path.join('/data/data/com.termux/files/home/projects/ElectricityBillCalculatorPro', path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Part 5 created successfully!")
