package com.wallora.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.data.MyPair
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.components.SplitBitmap
import com.wallora.app.ui.components.WallpaperImage
import com.wallora.app.util.NightGrade
import com.wallora.app.util.PairStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pair maker: turns the user's own pictures into a light + dark wallpaper that switches with day and night. The dark version
 * can be a second picture or an automatic night version of the first, and the split preview updates as they choose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PairMakerScreen(
    viewModel: WallpaperViewModel,
    onOpenPair: (String) -> Unit,
    onOpenPro: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val myPairs by viewModel.myPairs.collectAsStateWithLifecycle()

    var lightUri by remember { mutableStateOf<Uri?>(null) }
    var darkUri by remember { mutableStateOf<Uri?>(null) }
    var auto by rememberSaveable { mutableStateOf(true) }
    var strength by rememberSaveable { mutableFloatStateOf(0.7f) }
    var title by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<com.wallora.app.data.Wallpaper?>(null) }

    var lightPreview by remember { mutableStateOf<Bitmap?>(null) }
    var darkPreview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(lightUri) {
        lightPreview = lightUri?.let { uri ->
            withContext(Dispatchers.Default) { PairStore.decode(context, uri, 1280)?.let { PairStore.cover(it, 360, 640) } }
        }
    }
    LaunchedEffect(lightPreview, darkUri, auto, strength) {
        val light = lightPreview
        darkPreview = when {
            light == null -> null
            auto -> withContext(Dispatchers.Default) { NightGrade.apply(light, strength) }
            else -> darkUri?.let { uri -> withContext(Dispatchers.Default) { PairStore.decode(context, uri, 1280)?.let { PairStore.cover(it, 360, 640) } } }
        }
    }

    val pickLight = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) lightUri = uri }
    val pickDark = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) darkUri = uri }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    val msgSaved = stringResource(R.string.pm_saved)
    val msgFailed = stringResource(R.string.pm_failed)
    val msgPickFirst = stringResource(R.string.pm_pick_first)
    val defaultName = stringResource(R.string.pm_default_name)

    fun save() {
        val light = lightUri
        if (light == null) { scope.launch { snackbar.showSnackbar(msgPickFirst) }; return }
        if (!isPro && myPairs.size >= MyPair.FREE_LIMIT) { onOpenPro(); return }
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) {
                val lightFull = PairStore.decode(context, light, 2200)?.let { PairStore.cover(it) } ?: return@withContext null
                val darkFull = if (auto) {
                    NightGrade.apply(lightFull, strength)
                } else {
                    darkUri?.let { PairStore.decode(context, it, 2200) }?.let { PairStore.cover(it) }
                } ?: return@withContext null
                lightFull to darkFull
            }
            val saved = ok != null && viewModel.addMyPair(title.ifBlank { defaultName }, ok.first, ok.second)
            ok?.first?.recycle(); ok?.second?.recycle()
            busy = false
            if (saved) {
                lightUri = null; darkUri = null; title = ""
                snackbar.showSnackbar(msgSaved)
            } else {
                snackbar.showSnackbar(msgFailed)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                Text(stringResource(R.string.pm_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.pm_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))

            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Card(Modifier.height(400.dp).aspectRatio(9f / 16f), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    val l = lightPreview
                    val d = darkPreview
                    if (l != null && d != null) {
                        SplitBitmap(l, d, Modifier.fillMaxSize())
                    } else if (l != null) {
                        androidx.compose.foundation.Image(
                            bitmap = l.asImageBitmap(), contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.pm_pick_first), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.pm_preview_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Text(stringResource(R.string.pm_light), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            OutlinedButton(onClick = { pickLight.launch(imageOnly) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (lightUri == null) R.string.pm_choose else R.string.pm_change))
            }

            Text(stringResource(R.string.pm_dark), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = auto, onClick = { auto = true }, label = { Text(stringResource(R.string.pm_auto)) })
                FilterChip(selected = !auto, onClick = { auto = false }, label = { Text(stringResource(R.string.pm_other)) })
            }
            if (auto) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pm_strength), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = strength, onValueChange = { strength = it }, valueRange = 0.2f..1f, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                }
            } else {
                OutlinedButton(onClick = { pickDark.launch(imageOnly) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(stringResource(if (darkUri == null) R.string.pm_choose else R.string.pm_change))
                }
            }

            OutlinedTextField(
                value = title, onValueChange = { title = it.take(40) }, singleLine = true,
                label = { Text(stringResource(R.string.pm_name)) }, placeholder = { Text(defaultName) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            if (!isPro) {
                Text(
                    stringResource(R.string.pm_limit, MyPair.FREE_LIMIT), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                )
            }
            Button(onClick = ::save, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text(stringResource(R.string.pm_save)) }

            Text(stringResource(R.string.pm_my_pairs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            if (myPairs.isEmpty()) {
                Text(stringResource(R.string.pm_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(myPairs, key = { it.id }) { pair ->
                        Column(Modifier.size(width = 110.dp, height = 220.dp)) {
                            Box(Modifier.size(width = 110.dp, height = 180.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenPair(pair.id) }) {
                                WallpaperImage(wallpaper = pair, dark = false, modifier = Modifier.fillMaxSize())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pair.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f))
                                IconButton(onClick = { toDelete = pair }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.height(32.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }

    toDelete?.let { pair ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.pm_delete_title)) },
            text = { Text(stringResource(R.string.pm_delete_body)) },
            confirmButton = {
                TextButton(onClick = { toDelete = null; scope.launch { viewModel.deleteMyPair(pair.id) } }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
