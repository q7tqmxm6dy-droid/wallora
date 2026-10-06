package com.wallora.app.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.ads.InterstitialAds
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.ui.WallpaperViewModel
import com.wallora.app.ui.theme.HeartRed
import com.wallora.app.util.Analytics
import com.wallora.app.util.Effect
import com.wallora.app.util.ImageEffects
import com.wallora.app.util.ImageSaver
import com.wallora.app.util.ShareUtils
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    wallpaperId: String,
    viewModel: WallpaperViewModel,
    dark: Boolean,
    onBack: () -> Unit,
) {
    // Swipe through the catalogue: the detail screen starts at the tapped wallpaper
    // and horizontal swipes move to its neighbours.
    val catalogue by viewModel.wallpapers.collectAsStateWithLifecycle()
    val startIndex = remember(wallpaperId, catalogue.size) {
        catalogue.indexOfFirst { it.id == wallpaperId }.coerceAtLeast(0)
    }
    var pageIndex by remember { mutableIntStateOf(startIndex) }
    val wallpaper = catalogue.getOrNull(pageIndex)
        ?: viewModel.wallpaper(wallpaperId)
        ?: return
    var swipeDrag by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = wallpaper.id in favorites
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTargetDialog by remember { mutableStateOf(false) }
    var showCollections by remember { mutableStateOf(false) }
    var showLockPreview by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var busy by remember { mutableStateOf(false) }
    var effect by remember { mutableStateOf(Effect.NONE) }
    var strength by remember { mutableStateOf(0.6f) }
    var previewImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(wallpaper.id) {
        effect = Effect.NONE
        strength = 0.6f
        zoom = 1f
        Analytics.log(context, Analytics.Events.OPEN_WALLPAPER, mapOf("id" to wallpaper.id, "remote" to wallpaper.remote))
        viewModel.recordViewed(wallpaper.id)
    }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    /** Loads the wallpaper and applies the selected look. */
    suspend fun loadProcessed(): Bitmap? {
        val raw = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return null
        val processed = ImageEffects.apply(effect, raw, strength)
        if (processed !== raw) raw.recycle()
        return processed
    }

    // Real processed preview, so what you see is exactly what gets applied
    // (Modifier.blur only works on Android 12+, hence rendering it ourselves).
    LaunchedEffect(wallpaper.id, dark, effect, strength) {
        delay(120) // debounce slider drags
        val bitmap = withContext(Dispatchers.Default) {
            val raw = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return@withContext null
            val half = Bitmap.createScaledBitmap(
                raw,
                maxOf(1, raw.width / 2),
                maxOf(1, raw.height / 2),
                true,
            )
            raw.recycle()
            val processed = ImageEffects.apply(effect, half, strength)
            if (processed !== half) half.recycle()
            processed
        }
        previewImage = bitmap?.asImageBitmap()
    }

    fun applyTarget(target: WallpaperTarget) {
        showTargetDialog = false
        busy = true
        scope.launch {
            val bitmap = loadProcessed()
            val applied = bitmap != null && WallpaperSetter.apply(context, bitmap, target)
            bitmap?.recycle()
            Analytics.log(
                context,
                Analytics.Events.WALLPAPER_SET,
                mapOf("id" to wallpaper.id, "target" to target.name, "ok" to applied),
            )
            notify(if (applied) "Wallpaper applied" else "Couldn't set wallpaper")
            if (applied) {
                viewModel.rememberApplied(wallpaper.id, dark)
                // Interstitial after a completed action, never on launch, and rate limited.
                InterstitialAds.maybeShowAfterSet(context as? Activity, PreferencesRepository(context))
            }
            busy = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(wallpaper.id, catalogue.size) {
                coroutineScope {
                    launch {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeDrag < -140f && pageIndex < catalogue.lastIndex) {
                                    pageIndex += 1
                                } else if (swipeDrag > 140f && pageIndex > 0) {
                                    pageIndex -= 1
                                }
                                swipeDrag = 0f
                            },
                            onHorizontalDrag = { _, amount -> swipeDrag += amount },
                        )
                    }
                    launch {
                        detectTapGestures(
                            onDoubleTap = { zoom = if (zoom > 1f) 1f else 2.5f },
                        )
                    }
                }
            },
    ) {
        val preview = previewImage
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoom),
            )
        } else {
            AsyncImage(
                model = wallpaper.model(dark),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoom),
            )
        }

        if (showLockPreview) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val now = java.time.LocalTime.now()
                Text(
                    text = "%02d:%02d".format(now.hour, now.minute),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Thin,
                )
                Text(
                    text = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(20.dp))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = "Swipe up to unlock",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { showLockPreview = !showLockPreview },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.lock_screen_preview),
                    tint = if (showLockPreview) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
            IconButton(
                onClick = { zoom = if (zoom > 1f) 1f else 2.5f },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Text(
                    text = if (zoom > 1f) "1x" else "2x",
                    color = if (zoom > 1f) MaterialTheme.colorScheme.primary else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    viewModel.toggleFavorite(wallpaper.id)
                    Analytics.log(
                        context,
                        Analytics.Events.FAVORITE_TOGGLED,
                        mapOf("id" to wallpaper.id, "favorite" to !isFavorite),
                    )
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.toggle_favorite),
                    tint = if (isFavorite) HeartRed else Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = wallpaper.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!wallpaper.credit.isNullOrBlank()) {
                Text(
                    text = buildString {
                        append("Photo: ")
                        append(wallpaper.credit)
                        wallpaper.license?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            TextButton(
                onClick = { showCollections = true },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.add_to_collection), color = Color.White)
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(Effect.entries.toList()) { option ->
                    FilterChip(
                        selected = effect == option,
                        onClick = { effect = option },
                        label = { Text(effectLabel(option)) },
                    )
                }
            }
            if (effect != Effect.NONE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.strength),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(
                        value = strength,
                        onValueChange = { strength = it },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Text(
                        text = "${(strength * 100).roundToInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showTargetDialog = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_wallpaper), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.set_wallpaper))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            val bitmap = loadProcessed()
                            val saved = bitmap != null && ImageSaver.saveToGallery(context, bitmap, wallpaper.id)
                            bitmap?.recycle()
                            Analytics.log(
                                context,
                                Analytics.Events.WALLPAPER_SAVED,
                                mapOf("id" to wallpaper.id, "ok" to saved),
                            )
                            notify(if (saved) "Saved to Pictures/Wallora" else "Couldn't save image")
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(painterResource(R.drawable.ic_download), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_save))
                }
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            val bitmap = loadProcessed()
                            if (bitmap != null) {
                                ShareUtils.share(context, bitmap, wallpaper.id)
                                bitmap.recycle()
                            } else {
                                notify("Couldn't load image")
                            }
                            Analytics.log(
                                context,
                                Analytics.Events.WALLPAPER_SHARED,
                                mapOf("id" to wallpaper.id),
                            )
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 190.dp),
        )
    }

    if (showCollections) {
        val collections by viewModel.collections.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showCollections = false },
            title = { Text(stringResource(R.string.add_to_collection)) },
            text = {
                Column {
                    if (collections.isEmpty()) {
                        Text(stringResource(R.string.detail_no_collections))
                    } else {
                        collections.keys.sorted().forEach { name ->
                            val checked = wallpaper.id in (collections[name] ?: emptyList())
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleInCollection(name, wallpaper.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { viewModel.toggleInCollection(name, wallpaper.id) },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollections = false }) { Text(stringResource(R.string.action_done)) }
            },
        )
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text(stringResource(R.string.set_wallpaper_dialog)) },
            text = {
                Column {
                    TargetOption(
                        rememberVectorPainter(Icons.Filled.Home),
                        stringResource(R.string.target_home),
                    ) {
                        applyTarget(WallpaperTarget.HOME)
                    }
                    TargetOption(
                        rememberVectorPainter(Icons.Filled.Lock),
                        stringResource(R.string.target_lock),
                    ) {
                        applyTarget(WallpaperTarget.LOCK)
                    }
                    TargetOption(
                        painterResource(R.drawable.ic_smartphone),
                        stringResource(R.string.target_both),
                    ) {
                        applyTarget(WallpaperTarget.BOTH)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun effectLabel(effect: Effect): String = stringResource(
    when (effect) {
        Effect.NONE -> R.string.effect_none
        Effect.BLUR -> R.string.effect_blur
        Effect.MONO -> R.string.effect_mono
        Effect.WARM -> R.string.effect_warm
        Effect.COOL -> R.string.effect_cool
        Effect.VIGNETTE -> R.string.effect_vignette
    },
)

@Composable
private fun TargetOption(painter: Painter, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
