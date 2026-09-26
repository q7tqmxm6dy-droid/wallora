package com.wallora.app.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Thin, crash-proof wrappers around Firebase Analytics and Crashlytics. */
object Analytics {

    object Events {
        const val WALLPAPER_SET = "wallpaper_set"
        const val WALLPAPER_SAVED = "wallpaper_saved"
        const val WALLPAPER_SHARED = "wallpaper_shared"
        const val FAVORITE_TOGGLED = "favorite_toggled"
        const val OPEN_WALLPAPER = "open_wallpaper"
        const val OPEN_CATEGORY = "open_category"
        const val ADMIN_UPLOAD = "admin_upload"
    }

    fun log(context: Context, event: String, params: Map<String, Any> = emptyMap()) {
        runCatching {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putString(key, value.toString())
                        else -> putString(key, value.toString())
                    }
                }
            }
            analytics.logEvent(event, bundle)
        }
    }

    fun recordError(throwable: Throwable) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
    }
}
