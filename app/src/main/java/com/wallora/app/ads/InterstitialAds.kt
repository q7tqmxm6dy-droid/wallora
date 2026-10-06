package com.wallora.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.wallora.app.data.PreferencesRepository

/**
 * A full-screen ad shown after a wallpaper is set -- never on launch.
 *
 * Showing an unexpected full-screen ad when the app opens is the classic way to get flagged
 * under Play's disruptive-ads policy, and it is the fastest way to make the app feel hostile.
 * Tying it to a completed action keeps it predictable: the user got what they asked for, then
 * the ad appears.
 *
 * Two caps apply, both persisted:
 *  - at most one ad per [SHOW_EVERY] sets, so it never fires on consecutive actions;
 *  - at least [MIN_GAP_MS] between ads, so a burst of sets cannot chain them.
 */
object InterstitialAds {

    private const val SHOW_EVERY = 3
    private const val MIN_GAP_MS = 90_000L

    private var ad: InterstitialAd? = null
    private var loading = false

    /** Fetches an ad ahead of time so showing one never makes the user wait. */
    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            AdsConfig.interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            },
        )
    }

    /**
     * Counts the set, then shows an ad only if both caps allow it.
     *
     * [activity] may be null (the host context may not be an Activity); the ad is simply skipped.
     * Must be called on the main thread.
     */
    suspend fun maybeShowAfterSet(activity: Activity?, prefs: PreferencesRepository) {
        val host = activity ?: return

        val count = prefs.interstitialCount() + 1
        prefs.setInterstitialCount(count)

        if (count % SHOW_EVERY != 0) {
            preload(host)
            return
        }
        val now = System.currentTimeMillis()
        if (now - prefs.lastInterstitialMs() < MIN_GAP_MS) {
            preload(host)
            return
        }

        val ready = ad ?: run { preload(host); return }

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(host)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                preload(host)
            }
        }
        ad = null
        prefs.setLastInterstitialMs(now)
        ready.show(host)
    }
}
