package com.wallora.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallora.app.R
import com.wallora.app.live.LiveScenes
import com.wallora.app.work.ThemeApplier

/** The same animated scene the live wallpaper draws, playing inside the app. [night] eases the day and night looks into each other. */
@Composable
fun ScenePreview(sceneId: String, night: Boolean, modifier: Modifier = Modifier) {
    val scene = remember(sceneId) { LiveScenes.create(sceneId) }
    val nightLevel by animateFloatAsState(if (night) 1f else 0f, tween(1200), label = "night")
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sceneId) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = (it - start) / 1_000_000_000f }
    }
    Canvas(modifier.clip(RoundedCornerShape(18.dp))) {
        drawIntoCanvas { scene.draw(it.nativeCanvas, size.width.toInt(), size.height.toInt(), time, nightLevel) }
    }
}

/** Scene choice and a live preview that can be flipped between the day and the night look. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScenePicker(selected: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var previewNight by remember { mutableStateOf(ThemeApplier.isDark(context)) }
    val labels = mapOf(
        LiveScenes.RUNWAY to R.string.scene_runway,
        LiveScenes.RAIN to R.string.scene_rain,
        LiveScenes.STARS to R.string.scene_stars,
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.pref_scene), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiveScenes.ids.forEach { id ->
                FilterChip(selected = selected == id, onClick = { onPick(id) }, label = { Text(stringResource(labels.getValue(id))) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ScenePreview(selected, previewNight, Modifier.size(width = 150.dp, height = 266.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.scene_preview_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(selected = !previewNight, onClick = { previewNight = false }, label = { Text(stringResource(R.string.scene_day)) })
                FilterChip(selected = previewNight, onClick = { previewNight = true }, label = { Text(stringResource(R.string.scene_night)) })
            }
        }
    }
}
