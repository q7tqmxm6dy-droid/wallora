package com.wallora.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wallora.app.ui.WalloraApp
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.screens.OnboardingScreen
import com.wallora.app.ui.theme.WalloraTheme
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private companion object {
        const val REQUEST_POST_NOTIFICATIONS = 1001
    }

    private val sharedImage = mutableStateOf<Uri?>(null)
    private val startRoute = mutableStateOf<String?>(null)

    /**
     * Android 13+ gates notifications behind a runtime permission. Without it the new-wallpaper
     * alerts are silently dropped, so ask on first launch. Safe to call repeatedly: the system
     * only shows the dialog while the user hasn't decided.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureSharedImage(intent)
        captureShortcutRoute(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            val viewModel: WallpaperViewModel = viewModel()
            val darkModePreference by viewModel.darkMode.collectAsStateWithLifecycle()
            val darkTheme = darkModePreference ?: isSystemInDarkTheme()

            WalloraTheme(darkTheme = darkTheme) {
                val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
                when (onboardingDone) {
                    null -> Unit // still reading the stored preference
                    false -> OnboardingScreen(onDone = { viewModel.completeOnboarding() })
                    true -> {
                        WalloraApp(
                            viewModel = viewModel,
                            darkTheme = darkTheme,
                            darkModePreference = darkModePreference,
                            onSetDarkMode = { viewModel.setDarkMode(it) },
                            startRoute = startRoute.value,
                            onStartRouteHandled = { startRoute.value = null },
                        )
                        val incoming = sharedImage.value
                        if (incoming != null) {
                            SharedImageDialog(uri = incoming, onDismiss = { sharedImage.value = null })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        captureSharedImage(intent)
        captureShortcutRoute(intent)
    }

    /** App-icon shortcuts pass the destination screen in an extra. */
    private fun captureShortcutRoute(intent: android.content.Intent?) {
        intent?.getStringExtra("route")?.let { startRoute.value = it }
    }

    private fun captureSharedImage(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
        ) {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
            if (uri != null) sharedImage.value = uri
        }
    }
}

/** Shown when another app shares an image with Wallora. */
@Composable
private fun SharedImageDialog(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun apply(target: WallpaperTarget) {
        busy = true
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            val applied = bitmap != null && WallpaperSetter.apply(context, bitmap, target)
            bitmap?.recycle()
            Toast.makeText(
                context,
                if (applied) "Wallpaper applied" else "Couldn't set wallpaper",
                Toast.LENGTH_SHORT,
            ).show()
            busy = false
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_image)) },
        text = { Text(stringResource(R.string.set_shared_q)) },
        confirmButton = {
            TextButton(onClick = { apply(WallpaperTarget.HOME) }, enabled = !busy) {
                Text(stringResource(R.string.target_home))
            }
        },
        dismissButton = {
            TextButton(onClick = { apply(WallpaperTarget.LOCK) }, enabled = !busy) {
                Text(stringResource(R.string.target_lock))
            }
        },
    )
}
