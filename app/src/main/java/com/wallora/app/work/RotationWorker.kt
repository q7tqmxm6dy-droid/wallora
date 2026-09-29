package com.wallora.app.work

import android.content.Context
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.util.Analytics
import com.wallora.app.util.WallpaperImages
import com.wallora.app.util.WallpaperSetter
import com.wallora.app.util.WallpaperTarget

/** Changes the wallpaper once a day, cycling through the bundled catalogue. */
class RotationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext)
        val (enabled, index, targetName) = prefs.readRotationConfig()
        if (!enabled) return Result.success()

        val all = WallpaperCatalog.wallpapers
        if (all.isEmpty()) return Result.success()

        val dark = isDark()
        val wallpaper = all[index % all.size]
        val bitmap = WallpaperImages.loadBitmap(applicationContext, wallpaper, dark)
            ?: return Result.retry()

        val target = runCatching { WallpaperTarget.valueOf(targetName) }.getOrDefault(WallpaperTarget.HOME)
        val applied = WallpaperSetter.apply(applicationContext, bitmap, target)
        bitmap.recycle()

        if (applied) {
            prefs.writeRotationIndex((index + 1) % all.size)
            prefs.rememberApplied(wallpaper.id, dark)
            Analytics.log(applicationContext, "daily_rotation", mapOf("id" to wallpaper.id, "target" to targetName))
        }
        return Result.success()
    }

    private fun isDark(): Boolean =
        (applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}
