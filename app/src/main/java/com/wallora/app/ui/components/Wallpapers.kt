package com.wallora.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.data.Curation
import com.wallora.app.data.Wallpaper
import com.wallora.app.ui.theme.HeartRed

/** Fades and lifts a tile in as it first appears, staggered by [index], so grids feel alive without a jank cost. */
fun Modifier.appear(index: Int): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay((index % 8) * 45L); shown = true }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "appear")
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 36f
        val s = 0.94f + 0.06f * progress
        scaleX = s; scaleY = s
    }
}

/** Gently shrinks while pressed. */
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.965f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) pressedScale else 1f, tween(110), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

@Composable
fun UhdBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.uhd_badge),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    dark: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    aspect: Float = 9f / 16f,
) {
    val source = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box {
            WallpaperImage(
                wallpaper = wallpaper,
                dark = dark,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))),
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.32f), CircleShape),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
                    ),
                    tint = if (isFavorite) HeartRed else Color.White,
                )
            }
            if (wallpaper.uhd) UhdBadge(Modifier.align(Alignment.BottomStart).padding(10.dp))
            if (wallpaper.featured) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.featured),
                    tint = Color(0xFFFFC107),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

/** Pinterest-style masonry: two (or more, on wide screens) columns of cards with varied heights. */
@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    favorites: Set<String>,
    dark: Boolean,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    emptyText: String? = null,
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) {
            item(span = StaggeredGridItemSpan.FullLine) { header() }
        }
        if (wallpapers.isEmpty() && emptyText != null) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        itemsIndexed(items = wallpapers, key = { _, w -> w.id }) { index, wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                isFavorite = wallpaper.id in favorites,
                dark = dark,
                onOpen = { onOpen(wallpaper.id) },
                onToggleFavorite = { onToggleFavorite(wallpaper.id) },
                aspect = Curation.cardRatio(wallpaper.id),
                modifier = Modifier.animateItem().appear(index),
            )
        }
    }
}
