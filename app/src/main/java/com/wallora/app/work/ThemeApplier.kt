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
 * The day/night switch for the wallpaper the user set: it is replaced by its light or dark version, in the same place
 * (home, lock or both). Wallpapers with only one image are left alone.
 *
 * Two modes: **auto** follows the phone's light/dark mode (also in the background, see [ThemeSyncWorker]), and
 * **light** / **dark** keep that version until the user changes it.
 */
object ThemeApplier {

    enum class Outcome { APPLIED, UNCHANGED, NO_WALLPAPER, SINGLE_IMAGE, FAILED }

    /** Which version should be on the phone: the chosen one, or the phone's own day/night when the mode is auto. */
    suspend fun effectiveDark(context: Context): Boolean = when (PreferencesRepository(context).readDayNightMode()) {
        PreferencesRepository.MODE_LIGHT -> false
        PreferencesRepository.MODE_DARK -> true
        else -> isDark(context)
    }

    /** The automatic path: swaps the wallpaper only when the version that should show has changed. */
    suspend fun applyIfChanged(context: Context): Outcome = apply(context, force = false)

    /** The manual path: puts the right version on the phone right now. */
    suspend fun applyNow(context: Context): Outcome = apply(context, force = true)

    private suspend fun apply(context: Context, force: Boolean): Outcome {
        val prefs = PreferencesRepository(context)
        val (id, lastDark) = prefs.readAppliedTheme()
        if (id == null) return Outcome.NO_WALLPAPER

        val dark = effectiveDark(context)
        if (!force && lastDark == dark) return Outcome.UNCHANGED

        val wallpaper = resolve(id) ?: return Outcome.NO_WALLPAPER
        if (!wallpaper.hasDarkVariant) {
            prefs.rememberThemeVariant(dark) // one image for both modes: nothing to swap
            return Outcome.SINGLE_IMAGE
        }

        val bitmap = WallpaperImages.loadBitmap(context, wallpaper, dark) ?: return Outcome.FAILED
        val target = runCatching { WallpaperTarget.valueOf(prefs.readAppliedTarget() ?: "HOME") }
            .getOrDefault(WallpaperTarget.HOME)
        val applied = WallpaperSetter.apply(context, bitmap, target)
        bitmap.recycle()
        if (!applied) return Outcome.FAILED
        prefs.rememberThemeVariant(dark)
        return Outcome.APPLIED
    }

    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Bundled first (works offline), then the cloud catalogue. */
    private suspend fun resolve(id: String): Wallpaper? {
        WallpaperCatalog.wallpaper(id)?.let { return it }
        return runCatching { RemoteWallpaperRepository().wallpapers().first().firstOrNull { it.id == id } }.getOrNull()
    }
}
