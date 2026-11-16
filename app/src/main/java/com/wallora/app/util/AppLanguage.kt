package com.wallora.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The language Wallora is shown in, chosen in Settings. "" means follow the phone.
 *
 * Android 13+ keeps a per-app language itself ([LocaleManager]; it also appears in the phone's own Settings). Older Android has no
 * such setting, so the choice is saved here and applied by wrapping the base context in [wrap].
 */
object AppLanguage {

    /** The languages Wallora is translated into, in the order shown. Keep in step with res/xml/locales_config.xml. */
    val tags = listOf("en", "es", "pt", "tr", "hi", "ar", "he")

    private const val PREFS = "wallora_language"
    private const val KEY = "tag"

    /** A language's own name for itself ("Español", "Türkçe", "हिन्दी"), capitalised. */
    fun nativeName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    /** The chosen language tag, or "" for the phone's language. */
    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) "" else locales[0].language
        }
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
    }

    /** Applies [tag] ("" for the phone's language). The screen restarts so every text changes at once. */
    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            return // the system recreates the activity itself
        }
        activity.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
        activity.recreate()
    }

    /** Android 12 and older: gives the base context the saved language. Newer Android needs nothing. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}
