package com.wallora.app.util

import android.net.Uri
import com.wallora.app.data.Wallpaper

/** Links that open a wallpaper in the Wallora web app, so it can be shared with someone who does not have the app. */
object WebLinks {

    private const val APP_URL = "https://wallora-bc950.web.app/app/"

    /**
     * The `mood_` pictures published on the website. Keep in step with WEB_ALLOWED_UNVERIFIED in scripts/export_web_catalog.py:
     * a link to anything else would open "That wallpaper isn't available".
     */
    private val WEB_MOODS = setOf(
        "mood_01", "mood_02", "mood_03", "mood_06", "mood_08", "mood_09", "mood_16", "mood_17", "mood_20", "mood_21", "mood_22",
        "mood_23", "mood_25", "mood_26", "mood_27", "mood_28", "mood_29", "mood_32", "mood_33", "mood_35", "mood_36", "mood_37",
    )

    /** False for the cars and other pictures kept off the website, and for the user's own pairs. */
    fun isOnWeb(wallpaper: Wallpaper): Boolean = wallpaper.remote || when {
        wallpaper.id.startsWith("nightcar_") || wallpaper.id.startsWith("mine_") -> false
        wallpaper.id.startsWith("mood_") -> wallpaper.id in WEB_MOODS
        else -> true
    }

    fun linkFor(wallpaper: Wallpaper): String = "${APP_URL}#/w/${Uri.encode(wallpaper.id)}"
}
