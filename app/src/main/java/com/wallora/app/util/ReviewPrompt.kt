package com.wallora.app.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.wallora.app.data.PreferencesRepository

/**
 * Google Play's in-app review card, asked for once after the user has set a few wallpapers -- the
 * moment they have clearly enjoyed the app. Play decides whether the card really appears (it has its
 * own quota), so this never shows a button of its own and never tells the user what happened.
 */
object ReviewPrompt {

    /** Sets before the first ask; early enough to catch happy users, late enough to mean something. */
    private const val SETS_BEFORE_ASKING = 3

    suspend fun maybeAsk(activity: Activity?, prefs: PreferencesRepository) {
        val host = activity ?: return
        if (prefs.reviewAsked() || prefs.appliedCount() < SETS_BEFORE_ASKING) return
        prefs.setReviewAsked()
        runCatching {
            val manager = ReviewManagerFactory.create(host)
            val info = manager.requestReviewFlow().await()
            manager.launchReviewFlow(host, info).await()
            Analytics.log(host, "review_prompt", emptyMap())
        }
    }
}
