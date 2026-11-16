package com.wallora.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** Turns a daytime picture into a believable night version: darker, cooler, a little less colourful, with darker corners. */
object NightGrade {

    /** [strength] 0 leaves the picture as it is, 1 is the darkest night look. The source is not modified. */
    fun apply(source: Bitmap, strength: Float): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        val dim = 1f - 0.62f * s
        val red = dim * (1f - 0.24f * s)
        val green = dim * (1f - 0.10f * s)
        val blue = dim * (1f + 0.08f * s)
        val colour = ColorMatrix().apply { setSaturation(1f - 0.25f * s) }
        colour.postConcat(
            ColorMatrix(
                floatArrayOf(
                    red, 0f, 0f, 0f, 0f,
                    0f, green, 0f, 0f, 0f,
                    0f, 0f, blue, 0f, 7f * s,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(colour) })

        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val vignette = Paint().apply {
            shader = RadialGradient(
                w / 2f, h * 0.45f, maxOf(w, h) * 0.75f,
                Color.TRANSPARENT, Color.argb((150 * s).toInt(), 0, 0, 12), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignette)
        return out
    }
}
