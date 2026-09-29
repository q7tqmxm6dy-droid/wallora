package com.wallora.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
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
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    wallpaperId: String,
    viewModel: WallpaperViewModel,
    dark: Boolean,
    onBack: () -> Unit,
) {
    val wallpaper = viewModel.wallpaper(wallpaperId) ?: return

    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite = wallpaper.id in favorites
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTargetDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var effect by remember { mutableStateOf(Effect.NONE) }

    LaunchedEffect(wallpaper.id) {
        Analytics.log(context, Analytics.Events.OPEN_WALLPAPER, mapOf("id" to wallpaper.id, "remote" to wallpaper.remote))
        viewModel.recordViewed(wallpaper.id)
    }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    /** Loads the wallpaper and applies the selected look. */
    suspend fun loadProcessed(): Bitmap? {
        val raw = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return null
        val processed = ImageEffects.apply(effect, raw)
        if (processed !== raw) raw.recycle()
        return processed
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
            if (applied) viewModel.rememberApplied(wallpaper.id, dark)
            busy = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AsyncImage(
            model = wallpaper.model(dark),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            colorFilter = effect.previewColorFilter(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (effect == Effect.BLUR) Modifier.blur(18.dp) else Modifier),
        )
        if (effect == Effect.VIGNETTE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f),
                            ),
                            radius = 1400f,
                        ),
                    ),
            )
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    contentDescription = "Toggle favorite",
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(Effect.entries.toList()) { option ->
                    FilterChip(
                        selected = effect == option,
                        onClick = { effect = option },
                        label = { Text(option.label) },
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
                Text("Set as wallpaper")
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
                    Text("Save")
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
                    Text("Share")
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

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Set wallpaper") },
            text = {
                Column {
                    TargetOption(rememberVectorPainter(Icons.Filled.Home), "Home screen") {
                        applyTarget(WallpaperTarget.HOME)
                    }
                    TargetOption(rememberVectorPainter(Icons.Filled.Lock), "Lock screen") {
                        applyTarget(WallpaperTarget.LOCK)
                    }
                    TargetOption(painterResource(R.drawable.ic_smartphone), "Both screens") {
                        applyTarget(WallpaperTarget.BOTH)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") }
            },
        )
    }
}

/** Compose-side approximation of each look, for the on-screen preview. */
private fun Effect.previewColorFilter(): ColorFilter? {
    val matrix = when (this) {
        Effect.MONO -> ColorMatrix().apply { setToSaturation(0f) }
        Effect.WARM -> ColorMatrix(
            floatArrayOf(
                1.14f, 0f, 0f, 0f, 0f,
                0f, 0.97f, 0f, 0f, 0f,
                0f, 0f, 0.86f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        Effect.COOL -> ColorMatrix(
            floatArrayOf(
                0.88f, 0f, 0f, 0f, 0f,
                0f, 0.99f, 0f, 0f, 0f,
                0f, 0f, 1.16f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        else -> null
    }
    return matrix?.let { ColorFilter.colorMatrix(it) }
}

@Composable
private fun TargetOption(painter: Painter, label: String, onClick: () -> Unit) {    Row(
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
