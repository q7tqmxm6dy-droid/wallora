package com.wallora.app.data

import androidx.annotation.DrawableRes

/**
 * A wallpaper. It is either **bundled** (a drawable resource that ships in the APK)
 * or **remote** (an image URL hosted in Firebase Cloud Storage and described by a
 * Firestore document).
 */
data class Wallpaper(
    val id: String,
    val title: String,
    val categoryId: String,
    @get:DrawableRes val resId: Int? = null,
    val imageUrl: String? = null,
    val remote: Boolean = false,
) {
    /** Whatever Coil should load: a URL string or a drawable resource id. */
    val model: Any? get() = imageUrl ?: resId
}

/** A browsable group of wallpapers. */
data class Category(
    val id: String,
    val title: String,
)
