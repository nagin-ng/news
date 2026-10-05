package com.nh.githubuploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nh.githubuploader.ui.MainScreen
import com.nh.githubuploader.ui.theme.GitHubUploaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubUploaderTheme {
                MainScreen()
            }
        }
    }
}
