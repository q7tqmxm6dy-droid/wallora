package com.wallora.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * A standard 320x50 banner that takes up no space until it actually has an ad to show.
 *
 * An AdView with nothing to display still reserves its height, which leaves a blank strip above
 * the tab bar. That happens on a no-fill, when offline, and -- relevant right now -- whenever the
 * AdMob account cannot serve. Collapsing to zero height until [AdListener.onAdLoaded] fires means
 * a failed request is invisible instead of looking like a rendering bug.
 *
 * The view is created once by [AndroidView] and reused across recompositions, so scrolling the
 * wallpaper grid never re-requests an ad (which would waste impressions and risk an
 * "invalid traffic" flag).
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdsConfig.ADS_ENABLED) return

    var hasAd by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasAd) Modifier else Modifier.height(0.dp)),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdsConfig.bannerUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        hasAd = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        hasAd = false
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
