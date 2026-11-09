package com.wallora.app.ads

import com.wallora.app.BuildConfig

/**
 * AdMob identifiers, all in one place.
 *
 * Debug builds serve Google's official test ads (with Google's sample app id, set in
 * `src/debug/res/values/admob.xml`), so the ad layout can be tried safely: test ads can never count as
 * real impressions, which is what gets AdMob accounts suspended for invalid traffic.
 *
 * Release builds show no ads at all, because AdMob had not accepted the account when the first release
 * went out. When it does: set [ADS_ENABLED] to `true`, keep [USE_TEST_ADS] as `BuildConfig.DEBUG`, and
 * check that the real app id in `res/values/strings.xml` and the unit ids below match the AdMob console.
 *
 * Real ids look like `ca-app-pub-################/##########`, app ids use `~` instead of `/`.
 */
object AdsConfig {

    /**
     * Master switch. When false no banner view is created and no ad is ever requested, so nothing is
     * charged, nothing is collected and no blank space is left in the layout; the consent flow and the ad
     * SDK never start, and the Pro screen and Settings drop their ad wording.
     */
    val ADS_ENABLED: Boolean = BuildConfig.DEBUG

    /** Debug builds always use Google's test units; never put real units in a build you tap yourself. */
    val USE_TEST_ADS: Boolean = BuildConfig.DEBUG

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    // ---- replace these two when going live ------------------------------------------------
    private const val REAL_BANNER = "ca-app-pub-1868881840290691/1885192575"
    private const val REAL_INTERSTITIAL = "ca-app-pub-1868881840290691/4858840271"
    // ---------------------------------------------------------------------------------------

    val bannerUnitId: String get() = if (USE_TEST_ADS) TEST_BANNER else REAL_BANNER

    val interstitialUnitId: String get() = if (USE_TEST_ADS) TEST_INTERSTITIAL else REAL_INTERSTITIAL
}
