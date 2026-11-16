package com.wallora.app.ui.screens

import android.app.WallpaperManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.work.ThemeApplier
import com.wallora.app.work.ThemeSyncScheduler
import kotlinx.coroutines.launch

/**
 * Day & night wallpaper: Auto follows the phone's light/dark mode, Light and Dark keep that version. Choosing a mode
 * changes the wallpaper on the phone straight away, and the preview shows the version that is on the phone now.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayNightSection(viewModel: WallpaperViewModel, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by viewModel.dayNightMode.collectAsStateWithLifecycle()
    val appliedId by viewModel.appliedWallpaperId.collectAsStateWithLifecycle()
    val appliedDark by viewModel.appliedDark.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val applied = remember(appliedId, wallpapers) { wallpapers.firstOrNull { it.id == appliedId } }
    var busy by remember { mutableStateOf(false) }
    // A live wallpaper on the home screen follows the setting by itself, so the card describes that instead of a still picture.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val liveActive = remember(resumes, mode, appliedDark, appliedId) { WallpaperManager.getInstance(context).wallpaperInfo != null }

    val msgLight = stringResource(R.string.daynight_done_light)
    val msgDark = stringResource(R.string.daynight_done_dark)
    val msgNone = stringResource(R.string.daynight_none)
    val msgSingle = stringResource(R.string.daynight_single_msg)
    val msgFailed = stringResource(R.string.daynight_failed)
    val msgLive = stringResource(R.string.daynight_live_msg)

    fun choose(newMode: String) {
        if (busy) return
        scope.launch {
            busy = true
            PreferencesRepository(context).setDayNightMode(newMode)
            ThemeSyncScheduler.sync(context, newMode == PreferencesRepository.MODE_AUTO)
            val outcome = ThemeApplier.applyNow(context)
            busy = false
            val message = when (outcome) {
                ThemeApplier.Outcome.NO_WALLPAPER -> msgNone
                ThemeApplier.Outcome.SINGLE_IMAGE -> msgSingle
                ThemeApplier.Outcome.LIVE -> msgLive
                ThemeApplier.Outcome.FAILED -> msgFailed
                else -> if (ThemeApplier.effectiveDark(context)) msgDark else msgLight
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.pref_match_theme), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.pref_match_theme_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!liveActive) {
                Box(
                    Modifier
                        .size(width = 72.dp, height = 128.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (applied != null) {
                        AsyncImage(
                            model = applied.model(appliedDark == true),
                            contentDescription = applied.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 72.dp, height = 128.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (liveActive) {
                    Text(
                        stringResource(R.string.daynight_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.daynight_live_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.daynight_live_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (applied != null) {
                    Text(
                        stringResource(R.string.daynight_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(applied.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            when {
                                !applied.hasDarkVariant -> R.string.daynight_version_single
                                appliedDark == true -> R.string.daynight_version_dark
                                else -> R.string.daynight_version_light
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        stringResource(R.string.daynight_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                PreferencesRepository.MODE_AUTO to R.string.theme_auto,
                PreferencesRepository.MODE_LIGHT to R.string.theme_light,
                PreferencesRepository.MODE_DARK to R.string.theme_dark,
            ).forEach { (value, labelRes) ->
                FilterChip(
                    selected = mode == value,
                    onClick = { choose(value) },
                    enabled = !busy,
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
    }
}
