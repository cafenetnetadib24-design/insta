package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DownloadViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.VideoDownloaderTheme

class MainActivity : ComponentActivity() {

    private val downloadViewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            handleIncomingIntent(intent)
        } catch (e: Exception) {
            // Log intent error safely
        }

        setContent {
            VideoDownloaderTheme {
                MainScreen(viewModel = downloadViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            handleIncomingIntent(intent)
        } catch (e: Exception) {
            // Log intent error safely
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            val type = intent.type

            if (Intent.ACTION_SEND == action && type != null && type.startsWith("text/")) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    downloadViewModel.handleSharedUrl(sharedText)
                }
            }
        } catch (e: Exception) {
            // Ignore intent handling exceptions
        }
    }
}
