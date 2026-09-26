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

    suspend fun loadBitmap(context: Context, wallpaper: Wallpaper): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resId = wallpaper.resId
                if (resId != null) {
                    BitmapFactory.decodeResource(context.resources, resId)
                } else {
                    val url = wallpaper.imageUrl ?: return@runCatching null
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 20_000
                    }
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
}
