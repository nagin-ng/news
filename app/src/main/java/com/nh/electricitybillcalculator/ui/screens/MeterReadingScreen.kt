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
