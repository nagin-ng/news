package com.nh.electricitybillcalculator.ui.screens

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
