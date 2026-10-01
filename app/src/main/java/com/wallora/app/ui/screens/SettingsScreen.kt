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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.live.WalloraLiveWallpaper
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.work.RotationScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WallpaperViewModel,
    darkModePreference: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
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
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Text(stringResource(R.string.pref_theme), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                val themeOptions: List<Pair<Int, Boolean?>> = listOf(
                    R.string.theme_auto to null,
                    R.string.theme_light to false,
                    R.string.theme_dark to true,
                )
                themeOptions.forEach { (labelRes, value) ->
                    FilterChip(
                        selected = darkModePreference == value,
                        onClick = { onSetDarkMode(value) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            SettingRow(
                title = stringResource(R.string.pref_daily_rotation),
                subtitle = stringResource(R.string.pref_daily_rotation_sub),
                checked = dailyRotation,
                onCheckedChange = { enabled ->
                    viewModel.setDailyRotation(enabled)
                    RotationScheduler.sync(context, enabled)
                },
            )
            if (dailyRotation) {
                Text(
                    text = stringResource(R.string.pref_apply_to),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "HOME" to R.string.target_home,
                        "LOCK" to R.string.target_lock,
                        "BOTH" to R.string.target_both,
                    ).forEach { (id, labelRes) ->
                        FilterChip(
                            selected = rotationTarget == id,
                            onClick = { viewModel.setRotationTarget(id) },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            SettingRow(
                title = stringResource(R.string.pref_match_theme),
                subtitle = stringResource(R.string.pref_match_theme_sub),
                checked = autoTheme,
                onCheckedChange = viewModel::setAutoThemeApply,
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(stringResource(R.string.pref_live_wallpaper), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.pref_live_wallpaper_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                listOf(
                    "slideshow" to R.string.live_slideshow,
                    "gradient" to R.string.live_gradient,
                ).forEach { (id, labelRes) ->
                    FilterChip(
                        selected = liveMode == id,
                        onClick = { viewModel.setLiveMode(id) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            if (liveMode == "slideshow") {
                Text(
                    stringResource(R.string.pref_show_each_for),
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
                    stringResource(R.string.pref_categories),
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
                        label = { Text(stringResource(R.string.category_all)) },
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
                Text(stringResource(R.string.open_live_picker))
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.recently_viewed), style = MaterialTheme.typography.titleSmall)
                    Text(
                        pluralStringResource(R.plurals.wallpaper_count, history.size, history.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    enabled = history.isNotEmpty(),
                ) { Text(stringResource(R.string.action_clear)) }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text(
                stringResource(R.string.settings_footer),
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
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
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
