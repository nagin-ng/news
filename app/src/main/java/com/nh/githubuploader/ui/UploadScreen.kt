package com.nh.githubuploader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UploadScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uploadState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.pickZip(uri)
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) vm.pickFolder(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GitHub Uploader", style = MaterialTheme.typography.headlineMedium)

        if (vm.token.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "Pehle Settings tab me apna GitHub token daalo.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SectionTitle("1. Project chuno")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { zipLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f)
            ) { Text("ZIP file chuno") }
            OutlinedButton(
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) { Text("Folder chuno") }
        }
        val label = vm.sourceLabel
        if (label != null) {
            Text("Chuna hua: $label", style = MaterialTheme.typography.bodyMedium)
            Text(vm.sourceInfo, style = MaterialTheme.typography.bodySmall)
        }

        SectionTitle("2. GitHub repo")
        OutlinedTextField(
            value = vm.owner,
            onValueChange = vm::updateOwner,
            label = { Text("Username / org (khali = apna account)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.repo,
            onValueChange = vm::updateRepo,
            label = { Text("Repo ka naam") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.branch,
            onValueChange = vm::updateBranch,
            label = { Text("Branch (e.g. main)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = vm.message,
            onValueChange = vm::updateMessage,
            label = { Text("Commit message") },
            modifier = Modifier.fillMaxWidth()
        )

        SwitchRow(
            label = "Repo na ho to banao",
            checked = vm.createRepo,
            onCheckedChange = vm::updateCreateRepo
        )
        if (vm.createRepo) {
            SwitchRow(
                label = "Naya repo private rakho",
                checked = vm.privateRepo,
                onCheckedChange = vm::updatePrivateRepo
            )
        }
        SwitchRow(
            label = "APK build workflow add karo",
            hint = "Agar project me pehle se workflow nahi hai to Actions APK banayega.",
            checked = vm.addWorkflow,
            onCheckedChange = vm::updateAddWorkflow
        )
        SwitchRow(
            label = "Purani files hatao (mirror)",
            hint = "ON karoge to repo me jo files is project me nahi hain, wo delete ho jayengi.",
            checked = vm.mirror,
            onCheckedChange = vm::updateMirror
        )

        Button(
            onClick = vm::startUpload,
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.running) "Upload ho raha hai..." else "GitHub par upload karo")
        }

        if (state.running || state.log.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.log.takeLast(40).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        val error = state.error
        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        val repoUrl = state.repoUrl
        val commitUrl = state.commitUrl
        if (repoUrl != null && commitUrl != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Upload ho gaya!", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Agar workflow hai to build 3-6 minute me hoga. Phir Builds tab ya Releases page se APK download karo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.skipped.isNotEmpty()) {
                        Text(
                            "Skip hui: " + state.skipped.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { uriHandler.openUri("$repoUrl/actions") }) { Text("Actions") }
                        OutlinedButton(onClick = { uriHandler.openUri("$repoUrl/releases") }) { Text("Releases") }
                        OutlinedButton(onClick = { uriHandler.openUri(commitUrl) }) { Text("Commit") }
                    }
                }
            }
        }
    }
}
