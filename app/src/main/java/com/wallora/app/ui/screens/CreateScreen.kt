package com.wallora.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.theme.Pink
import com.wallora.app.ui.theme.VioletDark
import com.wallora.app.util.GenStyle
import com.wallora.app.util.ImageSaver
import com.wallora.app.util.ShareUtils
import com.wallora.app.util.WallpaperGenerator
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Make a wallpaper on the phone: pick a style and palette, shuffle, then set, download or share it. */
@Composable
fun CreateScreen(viewModel: WallpaperViewModel, onOpenPro: () -> Unit, onOpenRoadmap: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var styleIndex by rememberSaveable { mutableIntStateOf(0) }
    var paletteIndex by rememberSaveable { mutableIntStateOf(0) }
    var seed by rememberSaveable { mutableIntStateOf(1) }
    var grain by rememberSaveable { mutableFloatStateOf(0.35f) }
    var busy by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showSize by remember { mutableStateOf(false) }
    val style = GenStyle.entries[styleIndex]
    val palette = WallpaperGenerator.palettes[paletteIndex]

    var preview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(style, paletteIndex, seed, grain) {
        preview = withContext(Dispatchers.Default) { WallpaperGenerator.render(style, palette, seed, grain, 360, 640) }
    }

    val msgApplied = stringResource(R.string.wallpaper_applied)
    val msgSetFailed = stringResource(R.string.wallpaper_set_failed)
    val msgSaved = stringResource(R.string.saved_to_pictures)
    val msgSaveFailed = stringResource(R.string.save_failed)

    fun full(width: Int, height: Int, block: suspend (Bitmap) -> Unit) {
        busy = true
        scope.launch {
            val bmp = withContext(Dispatchers.Default) { WallpaperGenerator.render(style, palette, seed, grain, width, height) }
            block(bmp)
            bmp.recycle()
            busy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.nav_create), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.create_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Card(Modifier.height(430.dp).aspectRatio(9f / 16f), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    preview?.let { Image(it.asImageBitmap(), contentDescription = stringResource(R.string.create_preview), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
            }

            Text(stringResource(R.string.create_style), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GenStyle.entries.toList()) { s ->
                    FilterChip(selected = s == style, onClick = { styleIndex = s.ordinal }, label = {
                        Text(stringResource(when (s) { GenStyle.AURA -> R.string.gen_aura; GenStyle.GRADIENT -> R.string.gen_gradient; GenStyle.WAVES -> R.string.gen_waves; GenStyle.ORBS -> R.string.gen_orbs }))
                    })
                }
            }
            Text(stringResource(R.string.create_palette), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(WallpaperGenerator.palettes.indices.toList()) { i ->
                    val colors = WallpaperGenerator.palettes[i].colors
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.sweepGradient(colors.map { Color(it) }))
                            .border(if (i == paletteIndex) 3.dp else 1.dp, if (i == paletteIndex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { paletteIndex = i },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.create_grain), style = MaterialTheme.typography.titleSmall)
                Slider(value = grain, onValueChange = { grain = it }, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            }
            OutlinedButton(onClick = { seed += 1 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_shuffle)) }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showTarget = true }, enabled = !busy, modifier = Modifier.weight(1.4f)) { Text(stringResource(R.string.set_wallpaper)) }
                OutlinedButton(onClick = { showSize = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }
                OutlinedButton(
                    onClick = { full(1080, 1920) { ShareUtils.share(context, it, "wallora_create_$seed") } },
                    enabled = !busy, modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_share)) }
            }

            // The prompt-based generator is not built yet; it is labelled, not faked.
            Card(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.rm_ai), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.create_ai_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }

    if (showTarget) {
        AlertDialog(
            onDismissRequest = { showTarget = false },
            title = { Text(stringResource(R.string.set_wallpaper_dialog)) },
            text = {
                Column {
                    listOf(WallpaperTarget.HOME to R.string.target_home, WallpaperTarget.LOCK to R.string.target_lock, WallpaperTarget.BOTH to R.string.target_both).forEach { (target, label) ->
                        TextButton(onClick = {
                            showTarget = false
                            full(1080, 1920) { bmp ->
                                val ok = withContext(Dispatchers.Default) { WallpaperSetter.apply(context, bmp, target) }
                                snackbar.showSnackbar(if (ok) msgApplied else msgSetFailed)
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTarget = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showSize) {
        AlertDialog(
            onDismissRequest = { showSize = false },
            title = { Text(stringResource(R.string.quality_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        showSize = false
                        full(1080, 1920) { bmp ->
                            val ok = withContext(Dispatchers.IO) { ImageSaver.saveToGallery(context, bmp, "wallora_create_$seed") }
                            snackbar.showSnackbar(if (ok) msgSaved else msgSaveFailed)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_size_hd)) }
                    TextButton(onClick = {
                        showSize = false
                        if (!isPro) onOpenPro() else full(2160, 3840) { bmp ->
                            val ok = withContext(Dispatchers.IO) { ImageSaver.saveToGallery(context, bmp, "wallora_create_${seed}_4k") }
                            snackbar.showSnackbar(if (ok) msgSaved else msgSaveFailed)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (isPro) R.string.create_size_4k else R.string.create_size_4k_pro)) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSize = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
