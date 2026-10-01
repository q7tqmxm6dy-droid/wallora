package com.wallora.app.notify

import android.content.Context
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.RemoteWallpaperRepository
import com.wallora.app.util.Notifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects wallpapers that have appeared in the cloud catalogue since the last check and announces
 * them with a local notification.
 *
 * This is deliberately server-free: it works on the Spark plan with no Cloud Functions deployed.
 * The same code runs when the app starts (so new items are announced promptly) and from a periodic
 * worker (so it also works while the app is closed).
 *
 * A Firestore `onCreate` trigger pushing to the [TOPIC] via FCM can be layered on later without
 * changing the app: [WalloraMessagingService] already turns such a push into a notification.
 */
object NewWallpaperAlerts {

    /** FCM topic a server-side trigger would publish new wallpapers to. */
    const val TOPIC = "new_wallpapers"

    /** @return the ids that were newly discovered (empty when there is nothing to announce). */
    suspend fun check(context: Context): Set<String> {
        val prefs = PreferencesRepository(context)
        if (!prefs.newAlertsEnabled()) return emptySet()

        // An offline or unconfigured client emits an empty list rather than failing, so an empty
        // result must be treated as "unknown", never as "every wallpaper was deleted".
        val remote = runCatching {
            withTimeoutOrNull(FETCH_TIMEOUT_MS) { RemoteWallpaperRepository().wallpapers().first() }
        }.getOrNull().orEmpty()
        if (remote.isEmpty()) return emptySet()

        val currentIds = remote.map { it.id }.toSet()

        if (!prefs.alertsSeeded()) {
            // First ever check: adopt the current catalogue silently so a fresh install doesn't
            // announce all 86 wallpapers at once.
            prefs.seedKnownWallpapers(currentIds)
            return emptySet()
        }

        val new = currentIds - prefs.knownWallpaperIds()
        if (new.isNotEmpty()) {
            prefs.rememberKnownWallpapers(new)
            Notifications.showNewWallpapers(context, new.size)
        }
        return new
    }

    private const val FETCH_TIMEOUT_MS = 20_000L
}
