package com.wallora.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.wallora.app.data.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Loads the full-resolution bitmap for a wallpaper, bundled or remote. */
object WallpaperImages {

    /**
     * @param dark when true, prefer the wallpaper's dark-mode variant (falling back to
     *             the default image when it has none).
     */
    suspend fun loadBitmap(context: Context, wallpaper: Wallpaper, dark: Boolean): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resId = if (dark) (wallpaper.darkResId ?: wallpaper.resId) else wallpaper.resId
                if (resId != null) {
                    BitmapFactory.decodeResource(context.resources, resId)
                } else {
                    val url = (if (dark) wallpaper.darkImageUrl ?: wallpaper.imageUrl else wallpaper.imageUrl)
                        ?: return@runCatching null
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 20_000
                    }
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
}
