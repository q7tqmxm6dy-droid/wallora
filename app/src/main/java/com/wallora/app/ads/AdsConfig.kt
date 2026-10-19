package com.wallora.app.ads

/**
 * AdMob identifiers, all in one place.
 *
 * [USE_TEST_ADS] is on, so every build serves Google's official sample ads. That is deliberate:
 * it keeps the app safe to run, screenshot and submit for review before an AdMob account exists,
 * and it means a stray tap during development can never be counted as a real impression, which is
 * what gets AdMob accounts suspended for invalid traffic.
 *
 * To go live:
 *  1. Create the app in the AdMob console and copy its **app id** into
 *     `res/values/strings.xml` (`admob_app_id`, currently Google's sample id).
 *  2. Create one **banner** and one **interstitial** ad unit, and paste their ids below.
 *  3. Set [USE_TEST_ADS] to false.
 *
 * Real ids look like `ca-app-pub-################/##########`, app ids use `~` instead of `/`.
 */
object AdsConfig {

    /**
     * Master switch. Set to false to ship with no ads at all: no banner view is created and no
     * ad is ever requested, so nothing is charged, nothing is collected and no blank space is
     * left in the layout. Useful while an AdMob account is under review.
     */
    const val ADS_ENABLED = true

    /** Keep true until real unit ids are in place. */
    const val USE_TEST_ADS = true

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    // ---- replace these two when going live ------------------------------------------------
    private const val REAL_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val REAL_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    // ---------------------------------------------------------------------------------------

    val bannerUnitId: String get() = if (USE_TEST_ADS) TEST_BANNER else REAL_BANNER

    val interstitialUnitId: String get() = if (USE_TEST_ADS) TEST_INTERSTITIAL else REAL_INTERSTITIAL
}
