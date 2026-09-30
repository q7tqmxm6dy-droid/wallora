package com.wallora.app.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.live.WalloraLiveWallpaper
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.work.RotationScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val dailyRotation by viewModel.dailyRotation.collectAsStateWithLifecycle()
    val rotationTarget by viewModel.rotationTarget.collectAsStateWithLifecycle()
    val autoTheme by viewModel.autoThemeApply.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val liveMode by viewModel.liveMode.collectAsStateWithLifecycle()
    val liveInterval by viewModel.liveInterval.collectAsStateWithLifecycle()
    val liveCategories by viewModel.liveCategories.collectAsStateWithLifecycle()
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingRow(
                title = "Daily rotation",
                subtitle = "Set a fresh wallpaper automatically every day",
                checked = dailyRotation,
                onCheckedChange = { enabled ->
                    viewModel.setDailyRotation(enabled)
                    RotationScheduler.sync(context, enabled)
                },
            )
            if (dailyRotation) {
                Text(
                    text = "Apply to",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HOME" to "Home", "LOCK" to "Lock", "BOTH" to "Both").forEach { (id, label) ->
                        FilterChip(
                            selected = rotationTarget == id,
                            onClick = { viewModel.setRotationTarget(id) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            SettingRow(
                title = "Match phone theme",
                subtitle = "When the phone switches dark or light, re-apply the matching variant " +
                    "of your current wallpaper",
                checked = autoTheme,
                onCheckedChange = viewModel::setAutoThemeApply,
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text("Live wallpaper", style = MaterialTheme.typography.titleSmall)
            Text(
                "Slideshow cycles your wallpapers with a slow crossfade; the animated gradient " +
                    "is lighter on battery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                listOf("slideshow" to "Slideshow", "gradient" to "Animated gradient").forEach { (id, label) ->
                    FilterChip(
                        selected = liveMode == id,
                        onClick = { viewModel.setLiveMode(id) },
                        label = { Text(label) },
                    )
                }
            }

            if (liveMode == "slideshow") {
                Text(
                    "Show each for",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    listOf(5, 10, 30, 60).forEach { seconds ->
                        FilterChip(
                            selected = liveInterval == seconds,
                            onClick = { viewModel.setLiveInterval(seconds) },
                            label = { Text("${seconds}s") },
                        )
                    }
                }

                Text(
                    "Categories",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    FilterChip(
                        selected = liveCategories.isEmpty(),
                        onClick = { viewModel.clearLiveCategories() },
                        label = { Text("All") },
                    )
                    allCategories.forEach { category ->
                        FilterChip(
                            selected = category.id in liveCategories,
                            onClick = { viewModel.toggleLiveCategory(category.id) },
                            label = { Text(category.title) },
                        )
                    }
                }
            }

            TextButton(
                onClick = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, WalloraLiveWallpaper::class.java),
                        )
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Open live wallpaper picker")
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Recently viewed", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${history.size} wallpapers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    enabled = history.isNotEmpty(),
                ) { Text("Clear") }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(
                "Wallora — wallpapers follow your phone's light/dark theme.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) { Text(actionLabel) }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
