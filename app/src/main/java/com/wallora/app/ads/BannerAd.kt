package com.wallora.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A standard 320x50 banner.
 *
 * The view is created once by [AndroidView] and reused across recompositions, so scrolling the
 * wallpaper grid never re-requests an ad (which would both waste impressions and risk an
 * "invalid traffic" flag).
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdsConfig.bannerUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
