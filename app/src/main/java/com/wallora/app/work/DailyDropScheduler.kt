package com.wallora.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Schedules / cancels the morning "Today's Drop" notification. */
object DailyDropScheduler {

    private const val UNIQUE_NAME = "wallora_daily_drop"

    /** Roughly when the notification lands; WorkManager may run it a little later to save battery. */
    private val DROP_TIME: LocalTime = LocalTime.of(9, 0)

    fun sync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(DROP_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyDropWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        // KEEP: re-syncing on every launch must not push the morning slot back.
        workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
