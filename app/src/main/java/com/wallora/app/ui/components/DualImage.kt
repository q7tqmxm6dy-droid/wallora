package com.wallora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wallora.app.data.Wallpaper

/** The left or right half of whatever it clips. */
private class HalfShape(private val left: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val half = size.width / 2f
        return Outline.Rectangle(
            if (left) Rect(0f, 0f, half, size.height) else Rect(half, 0f, size.width, size.height),
        )
    }
}

/**
 * A wallpaper drawn the way Wallora shows every light/dark pair: one picture, split down the middle, the
 * left half taken from the light version and the right half from the dark one. Both halves are scaled and
 * cropped identically, so the scene lines up and only the lighting changes at the seam.
 *
 * A wallpaper with a single image is drawn normally, using the variant that matches [dark].
 */
@Composable
fun WallpaperImage(
    wallpaper: Wallpaper,
    dark: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = wallpaper.title,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (!wallpaper.hasDarkVariant) {
        AsyncImage(
            model = wallpaper.model(dark),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
        return
    }
    Box(modifier) {
        AsyncImage(
            model = wallpaper.model(false),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize().clip(HalfShape(left = true)),
        )
        AsyncImage(
            model = wallpaper.model(true),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize().clip(HalfShape(left = false)),
        )
        // A hairline at the seam so the two versions read as a pair rather than a rendering glitch.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.White.copy(alpha = 0.55f)),
        )
    }
}
