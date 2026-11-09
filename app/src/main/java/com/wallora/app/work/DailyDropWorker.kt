package com.wallora.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallora.app.R
import com.wallora.app.data.Curation
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.WallpaperCatalog
import com.wallora.app.util.Analytics
import com.wallora.app.util.Notifications
import java.time.LocalDate

/** Each morning, names one hand-picked wallpaper ("Today's Drop"); tapping it opens that wallpaper. */
class DailyDropWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext)
        if (!prefs.dailyDropAlertsEnabled()) return Result.success()
        val day = LocalDate.now().toEpochDay()
        // Bundled art only, so the wallpaper is always there when the notification is tapped.
        val pick = Curation.dailyDrop(WallpaperCatalog.wallpapers, day, 1).firstOrNull() ?: return Result.success()
        if (!prefs.claimDropDay(day)) return Result.success()
        Notifications.show(
            applicationContext,
            applicationContext.getString(R.string.drop_notif_title, pick.title),
            applicationContext.getString(R.string.drop_notif_body),
            route = "detail:${pick.id}",
            id = Notifications.DAILY_DROP_ID,
        )
        Analytics.log(applicationContext, "daily_drop_notified", mapOf("id" to pick.id))
        return Result.success()
    }
}
