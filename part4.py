import os

files = {
"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/HomeScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.ui.navigation.Screen
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {
    val bills by viewModel.allBills.collectAsState()
    val recentBill = bills.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Electricity Calculator",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track and estimate your energy cost",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            if (recentBill != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("CURRENT MONTH", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${recentBill.unitsConsumed} kWh",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Estimated Bill", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${String.format("%.2f", recentBill.totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Data Available")
                        Text("Calculate your first bill to see insights.")
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { navController.navigate(Screen.Calculator.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Calculate Bill")
                }
                FilledTonalButton(
                    onClick = { navController.navigate(Screen.Appliances.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Appliances")
                }
            }
        }

        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                QuickActionItem(icon = Icons.Default.Speed, label = "Meter", onClick = { navController.navigate(Screen.MeterReadings.route) })
                QuickActionItem(icon = Icons.Default.Settings, label = "Tariff", onClick = { navController.navigate(Screen.TariffSettings.route) })
                QuickActionItem(icon = Icons.Default.Calculate, label = "Power", onClick = { navController.navigate(Screen.PowerCalculator.route) })
                QuickActionItem(icon = Icons.Default.Insights, label = "Analytics", onClick = { navController.navigate(Screen.Analytics.route) })
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
""",

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/CalculatorScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nh.electricitybillcalculator.data.local.entity.BillCalculationEntity
import com.nh.electricitybillcalculator.domain.calculator.CalculationEngine
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel
import com.nh.electricitybillcalculator.util.ShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(navController: NavController, viewModel: MainViewModel) {
    val slabs by viewModel.allSlabs.collectAsState()
    
    var previousReading by remember { mutableStateOf("") }
    var currentReading by remember { mutableStateOf("") }
    var fixedCharge by remember { mutableStateOf("0") }
    var duty by remember { mutableStateOf("0") }
    var other by remember { mutableStateOf("0") }
    var discount by remember { mutableStateOf("0") }
    
    var resultBill by remember { mutableStateOf<BillCalculationEntity?>(null) }
    var errorText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bill Calculator") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                OutlinedTextField(
                    value = previousReading,
                    onValueChange = { previousReading = it },
                    label = { Text("Previous Meter Reading") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentReading,
                    onValueChange = { currentReading = it },
                    label = { Text("Current Meter Reading") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText.isNotEmpty(),
                    supportingText = { if (errorText.isNotEmpty()) Text(errorText) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fixedCharge,
                        onValueChange = { fixedCharge = it },
                        label = { Text("Fixed Charge (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = duty,
                        onValueChange = { duty = it },
                        label = { Text("Duty (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = other,
                        onValueChange = { other = it },
                        label = { Text("Other (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Discount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val prev = previousReading.toDoubleOrNull() ?: 0.0
                        val curr = currentReading.toDoubleOrNull() ?: 0.0
                        
                        if (curr < prev) {
                            errorText = "Current reading cannot be lower than previous reading."
                            return@Button
                        }
                        errorText = ""
                        
                        val units = CalculationEngine.calculateUnits(prev, curr)
                        val energy = CalculationEngine.calculateSlabEnergyCharge(units, slabs)
                        val fCharge = fixedCharge.toDoubleOrNull() ?: 0.0
                        val eDuty = duty.toDoubleOrNull() ?: 0.0
                        val oCharge = other.toDoubleOrNull() ?: 0.0
                        val disc = discount.toDoubleOrNull() ?: 0.0
                        
                        val total = CalculationEngine.calculateTotal(energy, fCharge, eDuty, oCharge, disc)
                        
                        resultBill = BillCalculationEntity(
                            dateMillis = System.currentTimeMillis(),
                            previousReading = prev,
                            currentReading = curr,
                            unitsConsumed = units,
                            energyCharge = energy,
                            fixedCharge = fCharge,
                            electricityDuty = eDuty,
                            otherCharges = oCharge,
                            discount = disc,
                            totalAmount = total
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (resultBill != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Result", style = MaterialTheme.typography.titleLarge)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Energy Consumption: ${resultBill!!.unitsConsumed} Units")
                            Text("Energy Charge: ₹${String.format("%.2f", resultBill!!.energyCharge)}")
                            Text("Fixed Charge: ₹${String.format("%.2f", resultBill!!.fixedCharge)}")
                            Text("Electricity Duty: ₹${String.format("%.2f", resultBill!!.electricityDuty)}")
                            Text("Other Charges: ₹${String.format("%.2f", resultBill!!.otherCharges)}")
                            Text("Discount: -₹${String.format("%.2f", resultBill!!.discount)}")
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Estimated Total: ₹${String.format("%.2f", resultBill!!.totalAmount)}", style = MaterialTheme.typography.headlineSmall)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = {
                                    viewModel.insertBill(resultBill!!)
                                    scope.launch { snackbarHostState.showSnackbar("Calculation Saved") }
                                }) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save")
                                }
                                FilledTonalButton(onClick = { ShareUtil.shareBillText(context, resultBill!!) }) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share")
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

"app/src/main/java/com/nh/electricitybillcalculator/ui/screens/TariffSettingsScreen.kt": """package com.nh.electricitybillcalculator.ui.screens

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
import com.nh.electricitybillcalculator.data.local.entity.TariffSlabEntity
import com.nh.electricitybillcalculator.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffSettingsScreen(navController: NavController, viewModel: MainViewModel) {
    val slabs by viewModel.allSlabs.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tariff Settings") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newMin = if (slabs.isNotEmpty()) slabs.maxOf { it.maxUnits } + 1 else 0.0
                viewModel.insertSlab(TariffSlabEntity(minUnits = newMin, maxUnits = newMin + 50, rate = 0.0))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Slab")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(slabs) { slab ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val maxStr = if (slab.maxUnits == -1.0) "And Above" else "${slab.maxUnits} Units"
                            Text("${slab.minUnits} – $maxStr", style = MaterialTheme.typography.titleMedium)
                            Text("₹${slab.rate} / unit", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.deleteSlab(slab) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
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

print("Part 4 created successfully!")
