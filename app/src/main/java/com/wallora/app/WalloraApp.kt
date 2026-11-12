package com.wallora.app

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.messaging.FirebaseMessaging
import com.wallora.app.billing.ProManager
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.RemoteWallpaperRepository
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.notify.NewWallpaperAlerts
import com.wallora.app.util.Notifications
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget
import com.wallora.app.work.DailyDropScheduler
import com.wallora.app.work.NewWallpaperScheduler
import com.wallora.app.work.RotationScheduler
import com.wallora.app.work.ThemeApplier
import com.wallora.app.work.ThemeSyncScheduler
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
class WalloraApp : Application(), ImageLoaderFactory {

    /**
     * One shared image loader: a quarter of the app's memory for decoded bitmaps and a 256 MB disk cache, so cloud
     * wallpapers load instantly the second time and scrolling a large grid stays smooth.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(256L * 1024 * 1024).build() }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()


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
        ProManager.start(this)
        Notifications.ensureChannel(this)
        scope.launch { ensureRotationScheduled() }
        scope.launch { ensureAlertsScheduled() }
        scope.launch { ensureDailyDropScheduled() }
        scope.launch { applyThemeIfChanged() }
        scope.launch { ensureThemeSyncScheduled() }
        // Announce anything published since the last check, without waiting for the periodic job.
        scope.launch { runCatching { NewWallpaperAlerts.check(this@WalloraApp) } }
        subscribeToNewWallpaperTopic()
    }

    /**
     * Best effort: lets a server-side Firestore trigger reach this device over FCM. A no-op when
     * Play services are missing, and harmless while no trigger is deployed (the local check above
     * is what actually drives notifications today).
     */
    private fun subscribeToNewWallpaperTopic() {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(NewWallpaperAlerts.TOPIC) }
    }

    private suspend fun ensureAlertsScheduled() {
        NewWallpaperScheduler.sync(this, PreferencesRepository(this).newAlertsEnabled())
    }

    private suspend fun ensureDailyDropScheduled() {
        DailyDropScheduler.sync(this, PreferencesRepository(this).dailyDropAlertsEnabled())
    }

    private suspend fun ensureRotationScheduled() {
        val prefs = PreferencesRepository(this)
        val (enabled, _, _) = prefs.readRotationConfig()
        RotationScheduler.sync(this, enabled)
    }

    private suspend fun applyThemeIfChanged() = ThemeApplier.applyIfChanged(this)

    private suspend fun ensureThemeSyncScheduled() {
        ThemeSyncScheduler.sync(this, PreferencesRepository(this).readAppliedTheme().third)
    }
}
