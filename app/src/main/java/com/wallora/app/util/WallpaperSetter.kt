package com.wallora.app.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap

enum class WallpaperTarget { HOME, LOCK, BOTH }

/** Applies a bitmap as the device wallpaper. */
object WallpaperSetter {

    fun apply(context: Context, bitmap: Bitmap, target: WallpaperTarget): Boolean {
        val manager = WallpaperManager.getInstance(context)
        return try {
            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            manager.setBitmap(bitmap, null, true, flags)
            true
        } catch (t: Throwable) {
            Analytics.recordError(t)
            false
        }
    }
}
