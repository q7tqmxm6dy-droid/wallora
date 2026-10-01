package com.wallora.app

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.RemoteWallpaperRepository
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.util.Notifications
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import com.wallora.app.work.RotationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Watches the system light/dark setting and, when "match phone theme" is enabled,
 * re-applies the variant of the current wallpaper that matches the new theme.
 * Also makes sure the daily rotation job is scheduled after a reboot/update.
 */
class WalloraApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * A standalone callback rather than the Application implementing ComponentCallbacks itself.
     *
     * `Application.onConfigurationChanged` dispatches to every registered callback, so registering
     * `this` and also overriding `onConfigurationChanged` makes the override call `super`, which
     * dispatches straight back into the override — infinite recursion and a StackOverflowError on
     * any theme, rotation or locale change.
     */
    private val themeWatcher = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            scope.launch { applyThemeIfChanged() }
        }

        override fun onLowMemory() = Unit
    }

    override fun onCreate() {
        super.onCreate()
        registerComponentCallbacks(themeWatcher)
        Notifications.ensureChannel(this)
        scope.launch { ensureRotationScheduled() }
        scope.launch { applyThemeIfChanged() }
    }

    private suspend fun ensureRotationScheduled() {
        val prefs = PreferencesRepository(this)
        val (enabled, _, _) = prefs.readRotationConfig()
        RotationScheduler.sync(this, enabled)
    }

    private suspend fun applyThemeIfChanged() {
        val prefs = PreferencesRepository(this)
        val (id, lastDark, auto) = prefs.readAppliedTheme()
        if (!auto || id == null) return

        val dark = isDark()
        if (lastDark == dark) return

        val wallpaper = resolve(id) ?: return
        val bitmap = WallpaperImages.loadBitmap(this, wallpaper, dark) ?: return
        val target = runCatching {
            WallpaperTarget.valueOf(prefs.readRotationConfig().third)
        }.getOrDefault(WallpaperTarget.HOME)

        val applied = WallpaperSetter.apply(this, bitmap, target)
        bitmap.recycle()
        if (applied) prefs.rememberApplied(id, dark)
    }

    /** Bundled first (works offline), then the cloud catalogue. */
    private suspend fun resolve(id: String): Wallpaper? {
        WallpaperCatalog.wallpaper(id)?.let { return it }
        return runCatching {
            RemoteWallpaperRepository().wallpapers().first().firstOrNull { it.id == id }
        }.getOrNull()
    }

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
