package com.wallora.app.ads

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * The consent flow Google requires before serving personalised ads to users in the EEA/UK.
 *
 * Ads must not be requested until this completes. [onFinished] is always called, including when
 * the user has no choice to make (outside the EEA), when the form fails to load, or when the
 * device is offline -- the app should never depend on this succeeding to be usable.
 */
object AdsConsent {

    fun request(activity: Activity, onFinished: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val information = UserMessagingPlatform.getConsentInformation(activity)
        information.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onFinished() }
            },
            {
                // Consent lookup failed (offline, for example). Continue anyway; ads simply
                // will not be personalised.
                onFinished()
            },
        )
    }

    /**
     * Whether the user must be able to change their mind later.
     *
     * Google requires an in-app route back to the consent form wherever the form applies, so
     * Settings shows a row that calls [showPrivacyOptions] when this is true.
     */
    fun privacyOptionsRequired(activity: Activity): Boolean =
        runCatching {
            UserMessagingPlatform.getConsentInformation(activity)
                .privacyOptionsRequirementStatus ==
                com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }.getOrDefault(false)

    /** Re-opens the consent form so the user can change their advertising choice. */
    fun showPrivacyOptions(activity: Activity) {
        runCatching { UserMessagingPlatform.showPrivacyOptionsForm(activity) { } }
    }
}
