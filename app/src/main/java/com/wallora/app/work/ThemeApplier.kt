package com.wallora.app.work

import android.content.Context
import android.content.res.Configuration
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.RemoteWallpaperRepository
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import kotlinx.coroutines.flow.first

/**
 * The day/night switch: when the phone moves between light and dark mode, the wallpaper the user set is replaced
 * by its matching version, in the same place (home, lock or both). Wallpapers with only one image are left alone.
 *
 * Runs when the app starts, when the app sees the phone's mode change, and from [ThemeSyncWorker] so it also
 * happens while the app is closed.
 */
object ThemeApplier {

    suspend fun applyIfChanged(context: Context) {
        val prefs = PreferencesRepository(context)
        val (id, lastDark, auto) = prefs.readAppliedTheme()
        if (!auto || id == null) return

        val dark = isDark(context)
        if (lastDark == dark) return

        val wallpaper = resolve(id)
        if (wallpaper == null) return
        if (!wallpaper.hasDarkVariant) {
            prefs.rememberThemeVariant(dark) // one image for both modes: nothing to swap
            return
        }

        val bitmap = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return
        val target = runCatching { WallpaperTarget.valueOf(prefs.readAppliedTarget() ?: "HOME") }
            .getOrDefault(WallpaperTarget.HOME)
        val applied = WallpaperSetter.apply(context, bitmap, target)
        bitmap.recycle()
        if (applied) prefs.rememberThemeVariant(dark)
    }

    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Bundled first (works offline), then the cloud catalogue. */
    private suspend fun resolve(id: String): Wallpaper? {
        WallpaperCatalog.wallpaper(id)?.let { return it }
        return runCatching { RemoteWallpaperRepository().wallpapers().first().firstOrNull { it.id == id } }.getOrNull()
    }
}
