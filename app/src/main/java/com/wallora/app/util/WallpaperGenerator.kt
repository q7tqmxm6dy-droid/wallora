package com.wallora.app.util

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class GenStyle { AURA, GRADIENT, WAVES, ORBS }

/** A named colour set: the first colour is the darkest and anchors the background. */
data class GenPalette(val name: String, val colors: List<Int>)

/**
 * Draws wallpapers on the phone with plain Canvas calls, so creating one needs no network, no account and no cost.
 *
 * Everything is placed in proportion to the output size and driven by one seeded [Random], so the small preview
 * and the full-size render of the same settings are the same picture.
 */
object WallpaperGenerator {

    val palettes: List<GenPalette> = listOf(
        GenPalette("Sunset", listOf(0xFF2B1055.toInt(), 0xFF7B2F8E.toInt(), 0xFFFF6B6B.toInt(), 0xFFFFB86B.toInt())),
        GenPalette("Ocean", listOf(0xFF021B3A.toInt(), 0xFF0B5FA5.toInt(), 0xFF22C1C3.toInt(), 0xFFB8F2E6.toInt())),
        GenPalette("Aurora", listOf(0xFF04122B.toInt(), 0xFF1B6B5A.toInt(), 0xFF3DDC97.toInt(), 0xFF8E7DFF.toInt())),
        GenPalette("Bubblegum", listOf(0xFF3A1C5B.toInt(), 0xFFD65DB1.toInt(), 0xFFFF9AC1.toInt(), 0xFFFFE3F1.toInt())),
        GenPalette("Ember", listOf(0xFF140808.toInt(), 0xFF7A1F1F.toInt(), 0xFFE8542B.toInt(), 0xFFFFC857.toInt())),
        GenPalette("Mono", listOf(0xFF050505.toInt(), 0xFF2A2D34.toInt(), 0xFF6B7280.toInt(), 0xFFE5E7EB.toInt())),
        GenPalette("Forest", listOf(0xFF07140D.toInt(), 0xFF1E5631.toInt(), 0xFF6FBF73.toInt(), 0xFFE9F5B8.toInt())),
        GenPalette("Candy", listOf(0xFF1B1B3A.toInt(), 0xFF5B5BD6.toInt(), 0xFF45D9C6.toInt(), 0xFFFFD6A5.toInt())),
    )

    fun render(style: GenStyle, palette: GenPalette, seed: Int, grain: Float, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rng = Random(seed * 7919 + style.ordinal * 104729)
        when (style) {
            GenStyle.AURA -> aura(canvas, palette, rng, width, height)
            GenStyle.GRADIENT -> gradient(canvas, palette, rng, width, height)
            GenStyle.WAVES -> waves(canvas, palette, rng, width, height)
            GenStyle.ORBS -> orbs(canvas, palette, rng, width, height)
        }
        if (grain > 0f) grain(canvas, grain, seed, width, height)
        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Float) = Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun background(canvas: Canvas, c: List<Int>, w: Int, h: Int) {
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(c[0], c[1]), null, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    /** Big soft colour blooms, the "aura" look. */
    private fun aura(canvas: Canvas, p: GenPalette, rng: Random, w: Int, h: Int) {
        background(canvas, p.colors, w, h)
        repeat(6) { i ->
            val color = p.colors[1 + i % (p.colors.size - 1)]
            val cx = rng.nextFloat() * w; val cy = rng.nextFloat() * h; val r = w * (0.55f + rng.nextFloat() * 0.6f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy, r, intArrayOf(withAlpha(color, 0.9f), withAlpha(color, 0f)), null, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, r, paint)
        }
    }

    /** A smooth multi-stop sweep at a random angle with a soft highlight. */
    private fun gradient(canvas: Canvas, p: GenPalette, rng: Random, w: Int, h: Int) {
        val angle = rng.nextFloat() * 2 * PI.toFloat(); val len = (w + h) / 2f
        val cx = w / 2f; val cy = h / 2f
        val paint = Paint().apply {
            shader = LinearGradient(cx - cos(angle) * len, cy - sin(angle) * len, cx + cos(angle) * len, cy + sin(angle) * len, p.colors.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        val hx = rng.nextFloat() * w; val hy = rng.nextFloat() * h * 0.6f
        canvas.drawCircle(hx, hy, w * 0.8f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(hx, hy, w * 0.8f, intArrayOf(withAlpha(Color.WHITE, 0.28f), withAlpha(Color.WHITE, 0f)), null, Shader.TileMode.CLAMP)
        })
    }

    /** Layered translucent waves, receding from the horizon. */
    private fun waves(canvas: Canvas, p: GenPalette, rng: Random, w: Int, h: Int) {
        background(canvas, p.colors, w, h)
        val layers = 7
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val base = h * (0.30f + 0.62f * t); val amp = h * (0.02f + 0.045f * (1 - t) + rng.nextFloat() * 0.02f)
            val f1 = 2 * PI.toFloat() * (1.1f + rng.nextFloat() * 1.4f) / w; val f2 = f1 * (1.8f + rng.nextFloat()); val ph = rng.nextFloat() * 6.28f
            val path = Path().apply {
                moveTo(0f, h.toFloat()); var x = 0f
                while (x <= w) { lineTo(x, base + amp * sin(x * f1 + ph) + amp * 0.4f * sin(x * f2 + ph * 2)); x += w / 90f }
                lineTo(w.toFloat(), h.toFloat()); close()
            }
            val color = p.colors[1 + i % (p.colors.size - 1)]
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, base - amp, 0f, h.toFloat(), withAlpha(color, 0.55f + 0.3f * t), withAlpha(p.colors[0], 0.85f), Shader.TileMode.CLAMP)
            })
        }
    }

    /** Glossy spheres floating in the dark, each with a glow and a highlight. */
    private fun orbs(canvas: Canvas, p: GenPalette, rng: Random, w: Int, h: Int) {
        background(canvas, p.colors, w, h)
        repeat(7) { i ->
            val color = p.colors[1 + i % (p.colors.size - 1)]
            val r = w * (0.09f + rng.nextFloat() * 0.2f); val cx = rng.nextFloat() * w; val cy = h * (0.12f + rng.nextFloat() * 0.76f)
            canvas.drawCircle(cx, cy, r * 2.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy, r * 2.4f, intArrayOf(withAlpha(color, 0.35f), withAlpha(color, 0f)), null, Shader.TileMode.CLAMP)
            })
            canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.25f, intArrayOf(Color.WHITE, color, p.colors[0]), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            })
        }
    }

    /** Fine film grain: a small noise tile repeated over the image. */
    private fun grain(canvas: Canvas, amount: Float, seed: Int, w: Int, h: Int) {
        val size = 128; val rng = Random(seed)
        val pixels = IntArray(size * size) { val v = rng.nextInt(256); Color.argb(255, v, v, v) }
        val tile = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            alpha = (amount * 70).toInt().coerceIn(0, 120)
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        tile.recycle()
    }
}
