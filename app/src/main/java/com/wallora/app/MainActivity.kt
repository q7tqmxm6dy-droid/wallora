package com.wallora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wallora.app.ui.WalloraApp
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.theme.WalloraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: WallpaperViewModel = viewModel()
            val darkModePreference by viewModel.darkMode.collectAsStateWithLifecycle()
            val darkTheme = darkModePreference ?: isSystemInDarkTheme()

            WalloraTheme(darkTheme = darkTheme) {
                WalloraApp(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleDarkMode = { viewModel.setDarkMode(!darkTheme) },
                )
            }
        }
    }
}
