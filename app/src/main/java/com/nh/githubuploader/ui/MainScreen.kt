package com.nh.githubuploader.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Upload("Upload", Icons.Filled.Share),
    Builds("Builds", Icons.Filled.PlayArrow),
    Settings("Settings", Icons.Filled.Settings)
}

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = Tab.entries

    Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (tab) {
            0 -> UploadScreen(vm, contentModifier)
            1 -> BuildsScreen(vm, contentModifier)
            else -> SettingsScreen(vm, contentModifier)
        }
    }
}
