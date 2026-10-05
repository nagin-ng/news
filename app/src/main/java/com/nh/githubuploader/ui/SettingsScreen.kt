package com.nh.githubuploader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private const val TOKEN_PAGE =
    "https://github.com/settings/tokens/new?description=GitHub%20Uploader&scopes=repo,workflow"

@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("GitHub token")
        OutlinedTextField(
            value = vm.token,
            onValueChange = vm::updateToken,
            label = { Text("Personal access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = vm::verifyToken,
            enabled = !vm.verifying,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Token check karo") }

        val status = vm.tokenStatus
        if (status != null) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
        }

        SectionTitle("Token kaise banaye")
        Text(
            "1. Neeche wala button dabao, GitHub me login karo.\n" +
                "2. Scopes 'repo' aur 'workflow' pehle se selected honge. Expiration chuno aur 'Generate token' dabao.\n" +
                "3. Token copy karke upar paste karo. Wo sirf ek baar dikhta hai.\n\n" +
                "'repo' se files upload hoti hain, 'workflow' se build workflow file push ho paati hai.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(
            onClick = { uriHandler.openUri(TOKEN_PAGE) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("GitHub par token banao") }

        Text(
            "Token is phone me encrypted storage me save hota hai. Kisi ke saath share mat karna.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
