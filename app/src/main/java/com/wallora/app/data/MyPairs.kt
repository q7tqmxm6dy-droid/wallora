package com.wallora.app.data

import java.io.File

/**
 * A light/dark pair the user made in Pair maker. The pictures live in the app's private storage; this is the small record that
 * remembers them. It becomes a normal [Wallpaper] (with a light and a dark version) so every screen can show it.
 */
data class MyPair(val id: String, val title: String, val lightPath: String, val darkPath: String) {

    fun toWallpaper(): Wallpaper = Wallpaper(
        id = id,
        title = title,
        categoryId = CATEGORY_ID,
        imageUrl = File(lightPath).toURI().toString().replace("file:/", "file:///"),
        darkImageUrl = File(darkPath).toURI().toString().replace("file:/", "file:///"),
    )

    companion object {
        const val ID_PREFIX = "mine_"
        const val CATEGORY_ID = "mine"

        /** Free users can keep this many pairs; Pro has no limit. */
        const val FREE_LIMIT = 2
    }
}
