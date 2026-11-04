package com.wallora.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Outbound links the app exposes (policy, support, store page). */
object AppLinks {
    const val PRIVACY_POLICY = "https://wallora-bc950.web.app/privacy.html"
    const val SUPPORT_EMAIL = "ahmad.qiblawi112@gmail.com"

    fun openPrivacyPolicy(context: Context) = open(context, Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))

    fun contactSupport(context: Context, subject: String) = open(
        context,
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).putExtra(Intent.EXTRA_SUBJECT, subject),
    )

    /** Opens a prefilled email so a user can flag a wallpaper; no server is needed for it to work. */
    fun reportContent(context: Context, wallpaperId: String, title: String) = open(
        context,
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
            .putExtra(Intent.EXTRA_SUBJECT, "Report: $title")
            .putExtra(Intent.EXTRA_TEXT, "I'd like to report this wallpaper.\n\nTitle: $title\nId: $wallpaperId\nReason: "),
    )

    /** Opens the Play Store page, or the web page when no store app is installed. */
    fun rateApp(context: Context) {
        val id = context.packageName
        val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))
        if (!open(context, store)) open(context, web)
    }

    private fun open(context: Context, intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
}
