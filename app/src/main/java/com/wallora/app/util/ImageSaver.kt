package com.wallora.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore

/** Saves a wallpaper bitmap into the device's Pictures/Wallora folder. */
object ImageSaver {

    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Wallora")
        }
        return try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            true
        } catch (t: Throwable) {
            Analytics.recordError(t)
            false
        }
    }
}
