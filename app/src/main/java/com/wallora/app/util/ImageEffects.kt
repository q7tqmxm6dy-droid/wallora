package com.wallora.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader

/** Optional look applied before a wallpaper is set, saved or shared. */
enum class Effect(val label: String) {
    NONE("None"),
    BLUR("Blur"),
    MONO("B&W"),
    WARM("Warm"),
    COOL("Cool"),
    VIGNETTE("Vignette"),
}

object ImageEffects {

    fun apply(effect: Effect, src: Bitmap): Bitmap = when (effect) {
        Effect.NONE -> src
        Effect.BLUR -> blur(src)
        Effect.MONO -> colorMatrix(src, ColorMatrix().apply { setSaturation(0f) })
        Effect.WARM -> colorMatrix(src, channelGain(1.14f, 0.97f, 0.86f))
        Effect.COOL -> colorMatrix(src, channelGain(0.88f, 0.99f, 1.16f))
        Effect.VIGNETTE -> vignette(src)
    }

    /** Compose-side approximation of the same look, for the on-screen preview. */
    private fun channelGain(r: Float, g: Float, b: Float) = ColorMatrix(
        floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    private fun colorMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Fast approximate blur: downscale then upscale with filtering (two passes). */
    private fun blur(src: Bitmap): Bitmap {
        var current = src
        repeat(2) {
            val small = Bitmap.createScaledBitmap(
                current,
                maxOf(1, current.width / 22),
                maxOf(1, current.height / 22),
                true,
            )
            val out = Bitmap.createBitmap(current.width, current.height, current.config ?: Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(
                small,
                null,
                Rect(0, 0, current.width, current.height),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
            small.recycle()
            if (current !== src) current.recycle()
            current = out
        }
        return current
    }

    private fun vignette(src: Bitmap): Bitmap {
        val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = maxOf(out.width, out.height) * 0.72f
        paint.shader = RadialGradient(
            out.width / 2f,
            out.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, 0xB3000000.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
        return out
    }
}
