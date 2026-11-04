package com.wallora.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Teal,
    tertiary = Coral,
)

private val DarkColors = darkColorScheme(
    primary = VioletDark,
    onPrimary = Ink,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF2B2452),
    onPrimaryContainer = InkText,
    secondary = Pink,
    tertiary = TealDark,
    background = Ink,
    onBackground = InkText,
    surface = Ink,
    onSurface = InkText,
    surfaceVariant = InkHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    outline = androidx.compose.ui.graphics.Color(0xFF4A4660),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF2A2740),
)

@Composable
fun WalloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: the premium look is a fixed palette, not whatever the wallpaper tints the system.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
