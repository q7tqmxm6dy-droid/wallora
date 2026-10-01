package com.wallora.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallora.app.notify.NewWallpaperAlerts

/** Periodically checks the cloud catalogue for newly published wallpapers and announces them. */
class NewWallpaperWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        runCatching { NewWallpaperAlerts.check(applicationContext) }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
