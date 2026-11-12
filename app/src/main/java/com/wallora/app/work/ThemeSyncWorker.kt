package com.wallora.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Checks the phone's light/dark mode in the background and swaps the wallpaper to its matching version. */
class ThemeSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { ThemeApplier.applyIfChanged(applicationContext) }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}

/** Schedules / cancels [ThemeSyncWorker]. Fifteen minutes is WorkManager's shortest period; the check itself is trivial. */
object ThemeSyncScheduler {

    private const val UNIQUE_NAME = "wallora_theme_sync"

    fun sync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ThemeSyncWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
