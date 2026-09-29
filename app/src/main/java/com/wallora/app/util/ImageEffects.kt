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

    /**
     * @param strength 0.1 (subtle) … 1.0 (strong). Applies to blur, warm/cool and vignette.
     */
    fun apply(effect: Effect, src: Bitmap, strength: Float = 0.6f): Bitmap {
        val s = strength.coerceIn(0.05f, 1f)
        return when (effect) {
            Effect.NONE -> src
            Effect.BLUR -> blur(src, s)
            Effect.MONO -> colorMatrix(src, ColorMatrix().apply { setSaturation(0f) })
            Effect.WARM -> colorMatrix(src, channelGain(1f + 0.26f * s, 1f - 0.05f * s, 1f - 0.22f * s))
            Effect.COOL -> colorMatrix(src, channelGain(1f - 0.22f * s, 1f - 0.02f * s, 1f + 0.26f * s))
            Effect.VIGNETTE -> vignette(src, s)
        }
    }

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

    /**
     * Fast approximate blur: downscale then upscale with filtering. A larger divisor means a
     * smaller intermediate bitmap and therefore a stronger blur. Two passes at high strength.
     */
    private fun blur(src: Bitmap, strength: Float): Bitmap {
        // Quadratic so low strengths stay subtle and only the top end gets really soft.
        val divisor = (3f + 30f * strength * strength).toInt().coerceIn(2, 48)
        val passes = if (strength > 0.8f) 2 else 1
        var current = src
        repeat(passes) {
            val small = Bitmap.createScaledBitmap(
                current,
                maxOf(1, current.width / divisor),
                maxOf(1, current.height / divisor),
                true,
            )
            val out = Bitmap.createBitmap(
                current.width,
                current.height,
                current.config ?: Bitmap.Config.ARGB_8888,
            )
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

    private fun vignette(src: Bitmap, strength: Float): Bitmap {
        val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = maxOf(out.width, out.height) * (0.85f - 0.2f * strength)
        val alpha = (0.45f + 0.5f * strength).coerceIn(0f, 0.95f)
        paint.shader = RadialGradient(
            out.width / 2f,
            out.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, (alpha * 255).toInt() shl 24),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
        return out
    }
}
