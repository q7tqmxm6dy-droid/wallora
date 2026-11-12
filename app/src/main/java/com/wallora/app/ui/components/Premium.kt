package com.wallora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.data.Category
import com.wallora.app.data.Wallpaper
import com.wallora.app.ui.theme.Pink
import com.wallora.app.ui.theme.VioletDark

/** A soft violet-to-pink bloom behind a screen header: depth without a flat colour block. */
@Composable
fun GlowBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                Brush.verticalGradient(
                    listOf(VioletDark.copy(alpha = 0.22f), Pink.copy(alpha = 0.08f), Color.Transparent),
                ),
            ),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** A portrait card for horizontal rows. */
@Composable
fun PosterCard(
    wallpaper: Wallpaper,
    dark: Boolean,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspect: Float = 9f / 14f,
) {
    val source = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .width(width)
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box {
            WallpaperImage(
                wallpaper = wallpaper,
                dark = dark,
                modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
            Text(
                wallpaper.title,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp),
            )
            if (wallpaper.uhd) UhdBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        }
    }
}

/** Top-ten card: a large rank numeral over the wallpaper. */
@Composable
fun RankedCard(
    rank: Int,
    wallpaper: Wallpaper,
    dark: Boolean,
    onClick: () -> Unit,
) {
    Box(Modifier.width(168.dp)) {
        PosterCard(wallpaper, dark, 168.dp, onClick, aspect = 9f / 14f)
        Text(
            text = rank.toString(),
            color = Color.White,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 26.dp),
        )
    }
}

/** The big carousel card on Home. */
@Composable
fun HeroCard(
    wallpaper: Wallpaper,
    categoryLabel: String,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            WallpaperImage(
                wallpaper = wallpaper,
                dark = dark,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(
                    categoryLabel.uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.6f, androidx.compose.ui.unit.TextUnitType.Sp),
                )
                Text(wallpaper.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (wallpaper.uhd) UhdBadge(Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }
}

@Composable
fun CategoryTile(
    category: Category,
    cover: Wallpaper?,
    count: Int,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = cover?.model(dark),
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))))
            Text(category.emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.TopStart).padding(14.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(category.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    pluralStringResource(R.plurals.wallpaper_count, count, count),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
