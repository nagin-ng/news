package com.nh.githubuploader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nh.githubuploader.data.RunInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun BuildsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        if (vm.token.isNotBlank() && vm.repo.isNotBlank()) vm.loadRuns()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Builds", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = vm::loadRuns, enabled = !vm.runsLoading) { Text("Refresh") }
        }

        val webUrl = vm.repoWebUrl()
        if (webUrl != null) {
            Text("Repo: ${webUrl.removePrefix("https://")}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { uriHandler.openUri("$webUrl/releases") }) { Text("Releases (APK)") }
                OutlinedButton(onClick = { uriHandler.openUri("$webUrl/actions") }) { Text("Actions") }
            }
        }

        if (vm.runsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val error = vm.runsError
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (!vm.runsLoading && error == null && vm.runs.isEmpty()) {
            Text(
                "Abhi koi build nahi mila. Pehle project upload karo.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        vm.runs.forEach { run ->
            RunCard(run = run, onClick = { if (run.url.isNotBlank()) uriHandler.openUri(run.url) })
        }

        Text(
            "APK download karne ke liye Releases page kholo: har successful build ka APK wahan milta hai.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RunCard(run: RunInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(statusText(run), style = MaterialTheme.typography.titleMedium)
            if (run.title.isNotBlank()) {
                Text(run.title, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "${run.branch}  |  ${prettyTime(run.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun statusText(run: RunInfo): String = when {
    run.status != "completed" -> when (run.status) {
        "queued", "waiting", "pending", "requested" -> "Queue me hai"
        else -> "Build chal raha hai..."
    }
    run.conclusion == "success" -> "Success"
    run.conclusion == "failure" -> "Failed"
    run.conclusion == "cancelled" -> "Cancelled"
    else -> run.conclusion.ifBlank { run.status }
}

private fun prettyTime(iso: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        input.timeZone = TimeZone.getTimeZone("UTC")
        val date = input.parse(iso)
        if (date == null) iso else SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        iso
    }
}
