package com.wallora.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

/** The colour names offered by the Discover colour filter, in display order, with a representative swatch. */
object WallpaperColors {
    val all: List<Pair<String, Int>> = listOf(
        "red" to 0xFFE53935.toInt(), "orange" to 0xFFFB8C00.toInt(), "yellow" to 0xFFFDD835.toInt(),
        "green" to 0xFF43A047.toInt(), "teal" to 0xFF00ACC1.toInt(), "blue" to 0xFF1E88E5.toInt(),
        "purple" to 0xFF8E24AA.toInt(), "pink" to 0xFFEC407A.toInt(), "brown" to 0xFF6D4C41.toInt(),
        "black" to 0xFF111111.toInt(), "gray" to 0xFF9E9E9E.toInt(), "white" to 0xFFF5F5F5.toInt(),
    )
}

/**
 * Names the dominant colours of an image. The rules mirror scripts/gen_color_index.py, which pre-computes
 * the bundled wallpapers; this one tags admin uploads so cloud wallpapers can be filtered by colour too.
 */
object DominantColor {

    fun bucket(argb: Int): String {
        val hsv = FloatArray(3); Color.colorToHSV(argb, hsv)
        val h = hsv[0]; val s = hsv[1]; val v = hsv[2]
        return when {
            v < 0.20f -> "black"
            s < 0.14f -> if (v > 0.82f) "white" else "gray"
            h >= 15 && h < 40 && v < 0.55f && s > 0.30f -> "brown"
            h < 15 || h >= 345 -> "red"
            h < 40 -> "orange"
            h < 65 -> "yellow"
            h < 160 -> "green"
            h < 200 -> "teal"
            h < 255 -> "blue"
            h < 300 -> "purple"
            else -> "pink"
        }
    }

    fun of(bitmap: Bitmap): List<String> {
        val small = Bitmap.createScaledBitmap(bitmap, 48, 86, true)
        val counts = HashMap<String, Int>()
        val pixels = IntArray(small.width * small.height); small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        pixels.forEach { counts.merge(bucket(it), 1, Int::plus) }
        val total = pixels.size.toFloat(); val ranked = counts.entries.sortedByDescending { it.value }
        return ranked.filter { it.value / total >= 0.18f }.take(2).map { it.key }.ifEmpty { listOf(ranked.first().key) }
    }

    fun of(bytes: ByteArray): List<String> {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return emptyList()
        return of(bmp).also { bmp.recycle() }
    }
}
