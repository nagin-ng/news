package com.nh.bundleconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF0D0D0D)
private val Red = Color(0xFFC0001D)
private val Gold = Color(0xFFD4AF37)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Screen() }
    }
}

@Composable
fun Screen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var args by remember { mutableStateOf("version") }
    var out by remember { mutableStateOf("Phase 1: bundletool ko app ke andar run karke test karo.") }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NH AAB Converter", color = Gold, fontSize = 22.sp)
        Text("Phase 1 - bundletool on-device test", color = Color.Gray, fontSize = 13.sp)
        OutlinedTextField(
            value = args, onValueChange = { args = it },
            label = { Text("bundletool args (e.g. version, help)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Gold, unfocusedBorderColor = Red,
                focusedLabelColor = Gold, unfocusedLabelColor = Color.Gray
            )
        )
        Button(
            onClick = {
                busy = true; out = "Running..."
                scope.launch {
                    out = withContext(Dispatchers.IO) {
                        BundleRunner.run(ctx, args.trim().split(Regex("\\s+")).filter { it.isNotEmpty() })
                    }
                    busy = false
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Running..." else "RUN BUNDLETOOL", color = Color.White) }
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF1A1A1A)).padding(10.dp)) {
            Text(out, color = Color(0xFFE0E0E0), fontSize = 12.sp,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}
