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
