package com.wallora.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules / cancels the periodic check for newly published wallpapers. */
object NewWallpaperScheduler {

    private const val UNIQUE_NAME = "wallora_new_wallpaper_alerts"

    /**
     * Six hours is a deliberate compromise: frequent enough that new drops feel timely, far enough
     * apart that the check costs nothing noticeable in battery. WorkManager's floor is 15 minutes.
     */
    private const val INTERVAL_HOURS = 6L

    fun sync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<NewWallpaperWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
