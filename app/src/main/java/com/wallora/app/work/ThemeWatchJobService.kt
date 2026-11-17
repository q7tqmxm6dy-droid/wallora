package com.wallora.app.work

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wakes up the moment the phone's dark-mode setting changes, even if Wallora was closed, then swaps the wallpaper to
 * the light or dark version. A launcher such as Nova does not tell apps about the change, and a closed app gets no
 * configuration broadcast, so this watches the setting itself. It re-arms after every run. The 15-minute
 * [ThemeSyncWorker] stays as the safety net (for example for dark mode that switches itself at sunset).
 *
 * Uses JobScheduler directly rather than WorkManager: WorkManager reschedules its jobs whenever the app process starts,
 * which cancelled this job right after the change woke the app.
 */
class ThemeWatchJobService : JobService() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            delay(1500) // let the new mode reach the app's resources
            val outcome = runCatching { ThemeApplier.applyIfChanged(applicationContext) }
            Log.d("WalloraTheme", "dark-mode change seen, outcome=${outcome.getOrNull() ?: outcome.exceptionOrNull()}")
            jobFinished(params, false)
            arm(applicationContext, force = true)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return true // run again
    }

    companion object {
        private const val JOB_ID = 4201

        /** Starts watching the dark-mode setting. A no-op when already watching, unless [force]. */
        fun arm(context: Context, force: Boolean = false) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            if (!force && scheduler.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, ThemeWatchJobService::class.java))
                .addTriggerContentUri(JobInfo.TriggerContentUri(Settings.Secure.getUriFor("ui_night_mode"), 0))
                .addTriggerContentUri(JobInfo.TriggerContentUri(Settings.Secure.getUriFor("ui_night_mode_last_computed"), 0))
                .setTriggerContentUpdateDelay(0) // the system's default waits 10 s and up to 2 min
                .setTriggerContentMaxDelay(0)
                .build()
            runCatching { scheduler.schedule(job) }
        }

        fun disarm(context: Context) {
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler)?.cancel(JOB_ID)
        }
    }
}
