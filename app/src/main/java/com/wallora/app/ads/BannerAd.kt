package com.wallora.app.ads

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.wallora.app.billing.ProManager

/**
 * A standard 320x50 banner that takes up no space until it actually has an ad to show.
 *
 * An AdView with nothing to display still reserves its height, which leaves a blank strip above
 * the tab bar. That happens on a no-fill, when offline, and -- relevant right now -- whenever the
 * AdMob account cannot serve. Collapsing to zero height until [AdListener.onAdLoaded] fires means
 * a failed request is invisible instead of looking like a rendering bug.
 *
 * The view is created once and kept for as long as this composable stays in the tree, so scrolling
 * the grid or opening a wallpaper and coming back never re-requests an ad (which would waste
 * impressions and risk an "invalid traffic" flag). Callers therefore hide it with [visible] rather
 * than removing it from the composition. It is paused, resumed and destroyed with the lifecycle,
 * and not created at all for Wallora Pro users.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier, visible: Boolean = true) {
    if (!AdsConfig.ADS_ENABLED) return
    val proState by ProManager.state.collectAsStateWithLifecycle()
    if (proState.isPro) return

    var hasAd by remember { mutableStateOf(false) }
    val adView = remember { mutableStateOf<AdView?>(null) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.value?.pause()
                Lifecycle.Event.ON_RESUME -> adView.value?.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.value?.destroy()
            adView.value = null
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasAd && visible) Modifier else Modifier.height(0.dp)),
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
                adView.value = this
                // Ads must not be requested before the consent flow has finished.
                AdsGate.whenReady { loadAd(AdRequest.Builder().build()) }
            }
        },
        update = { it.visibility = if (visible) View.VISIBLE else View.GONE },
    )
}
