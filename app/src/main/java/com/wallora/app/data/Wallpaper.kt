package com.wallora.app.data

import androidx.annotation.DrawableRes

/**
 * A wallpaper, optionally with a **dark-mode variant**.
 *
 * The base fields (`resId` / `imageUrl`) are the default image; `darkResId` /
 * `darkImageUrl` hold an optional variant shown when the phone/app is in dark mode.
 * If there is no dark variant the default image is used for both themes.
 */
data class Wallpaper(
    val id: String,
    val title: String,
    val categoryId: String,
    @get:DrawableRes val resId: Int? = null,
    val imageUrl: String? = null,
    @get:DrawableRes val darkResId: Int? = null,
    val darkImageUrl: String? = null,
    val remote: Boolean = false,
    /** Attribution for real photos (CC-BY etc.). */
    val credit: String? = null,
    val license: String? = null,
    /** Admin controls: featured sorts first, order breaks ties, publishAt hides until then. */
    val featured: Boolean = false,
    val order: Long = 0,
    val tags: List<String> = emptyList(),
) {
    val hasDarkVariant: Boolean get() = darkImageUrl != null || darkResId != null

    /** Whatever Coil should load: a URL string or a drawable resource id. */
    fun model(dark: Boolean): Any? =
        if (dark) (darkImageUrl ?: darkResId ?: imageUrl ?: resId)
        else (imageUrl ?: resId)
}

/** A browsable group of wallpapers. */
data class Category(
    val id: String,
    val title: String,
)
