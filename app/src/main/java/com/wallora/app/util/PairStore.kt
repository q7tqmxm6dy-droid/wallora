package com.wallora.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import kotlin.math.max

/** Reads the pictures the user picks for Pair maker and keeps the pairs they save in the app's private storage. */
object PairStore {

    private fun dir(context: Context) = File(context.filesDir, "pairs").apply { mkdirs() }

    /** Decodes a picked picture (honouring its rotation), scaled so its longer side is at most [maxSide]. */
    fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = maxSide.toFloat() / max(info.size.width, info.size.height)
            if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
        }
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()

    /** Centre-crops to the phone shape (9:16) and scales to [width] x [height]. */
    fun cover(bitmap: Bitmap, width: Int = 1080, height: Int = 1920): Bitmap {
        val target = width.toFloat() / height
        val have = bitmap.width.toFloat() / bitmap.height
        val cropW = if (have > target) (bitmap.height * target).toInt() else bitmap.width
        val cropH = if (have > target) bitmap.height else (bitmap.width / target).toInt()
        val x = (bitmap.width - cropW) / 2
        val y = (bitmap.height - cropH) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, cropW.coerceAtLeast(1), cropH.coerceAtLeast(1))
        return Bitmap.createScaledBitmap(cropped, width, height, true).also { if (cropped !== bitmap && it !== cropped) cropped.recycle() }
    }

    /** Writes the two pictures and returns their paths (light, dark). */
    fun save(context: Context, id: String, light: Bitmap, dark: Bitmap): Pair<String, String> {
        val lightFile = File(dir(context), "${id}_light.jpg")
        val darkFile = File(dir(context), "${id}_dark.jpg")
        lightFile.outputStream().use { light.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        darkFile.outputStream().use { dark.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return lightFile.absolutePath to darkFile.absolutePath
    }

    fun delete(lightPath: String, darkPath: String) {
        runCatching { File(lightPath).delete() }
        runCatching { File(darkPath).delete() }
    }
}
