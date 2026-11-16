package com.wallora.app.live

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * An animated, original wallpaper scene drawn in code (no video, no bitmaps), so it is tiny, works offline and is ours to publish.
 *
 * Every scene has a day look and a night look, like a light/dark wallpaper pair: [night] runs from 0 (day) to 1 (night) and the
 * caller eases it, so the sky visibly fades when the phone changes between day and night.
 */
interface LiveScene {
    /** [t] is seconds since the scene started; [w] and [h] are the surface size in pixels. */
    fun draw(canvas: Canvas, w: Int, h: Int, t: Float, night: Float)
}

object LiveScenes {
    const val RUNWAY = "runway"
    const val RAIN = "rain"
    const val STARS = "stars"
    val ids = listOf(RUNWAY, RAIN, STARS)

    fun create(id: String?): LiveScene = when (id) {
        RAIN -> CityRainScene()
        STARS -> StarfallScene()
        else -> RunwayScene()
    }
}

/** Blends a day colour into a night colour. */
private fun mix(day: Long, night: Long, f: Float): Int {
    val k = f.coerceIn(0f, 1f)
    val a = day.toInt()
    val b = night.toInt()
    fun ch(x: Int, y: Int) = (x + (y - x) * k).roundToInt().coerceIn(0, 255)
    return Color.argb(ch(Color.alpha(a), Color.alpha(b)), ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
}

private fun withAlpha(color: Int, alpha: Float): Int =
    Color.argb((Color.alpha(color) * alpha.coerceIn(0f, 1f)).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

private fun paint() = Paint(Paint.ANTI_ALIAS_FLAG)

// =====================================================================================================================
// Runway: a car waits at the end of a runway while a jet crosses the sky
// =====================================================================================================================
private class RunwayScene : LiveScene {
    private val fill = paint()
    private val path = Path()
    private val rect = RectF()

    private class Tower(val x: Float, val w: Float, val h: Float, val windows: List<Triple<Float, Float, Float>>)

    private val towers: List<Tower> = run {
        val rnd = Random(11)
        var x = -0.02f
        buildList {
            while (x < 1.02f) {
                val w = 0.035f + rnd.nextFloat() * 0.055f
                val h = 0.025f + rnd.nextFloat() * 0.1f
                val windows = List(14) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 6.28f) }
                add(Tower(x, w, h, windows))
                x += w * 0.92f
            }
        }
    }

    override fun draw(canvas: Canvas, w: Int, h: Int, t: Float, night: Float) {
        val W = w.toFloat()
        val H = h.toFloat()
        val cx = W / 2f
        val hy = H * 0.50f

        // sky
        fill.shader = LinearGradient(
            0f, 0f, 0f, hy,
            intArrayOf(mix(0xFF6FA8E8, 0xFF070B1A, night), mix(0xFFBFD9F2, 0xFF1B2440, night), mix(0xFFFFD9B0, 0xFF6B3F3A, night)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, W, hy, fill)
        fill.shader = null

        // a few faint stars that only appear at night
        if (night > 0.05f) {
            val rnd = Random(5)
            fill.color = Color.WHITE
            repeat(40) {
                val sx = rnd.nextFloat() * W
                val sy = rnd.nextFloat() * hy * 0.6f
                val tw = 0.5f + 0.5f * sin(t * (0.8f + rnd.nextFloat()) + it)
                fill.alpha = (110 * night * tw).roundToInt()
                canvas.drawCircle(sx, sy, 1.4f + rnd.nextFloat() * 1.2f, fill)
            }
            fill.alpha = 255
        }

        drawPlane(canvas, W, H, t, night)

        // skyline on the horizon, with lit windows at night
        for (tower in towers) {
            val bx = tower.x * W
            val bw = tower.w * W
            val bh = tower.h * H
            fill.color = mix(0xFF9FB1CC, 0xFF0C1020, night)
            canvas.drawRect(bx, hy - bh, bx + bw, hy, fill)
            if (night > 0.05f) {
                for ((ox, oy, ph) in tower.windows) {
                    val glow = (0.55f + 0.45f * sin(t * 0.7f + ph)) * night
                    fill.color = withAlpha(0xFFFFD27A.toInt(), glow * 0.9f)
                    canvas.drawRect(bx + ox * (bw - 4f), hy - bh + oy * (bh - 6f), bx + ox * (bw - 4f) + 3f, hy - bh + oy * (bh - 6f) + 4f, fill)
                }
            }
        }
        // haze where the sky meets the ground
        fill.shader = LinearGradient(
            0f, hy - H * 0.05f, 0f, hy + H * 0.02f,
            intArrayOf(Color.TRANSPARENT, mix(0xCCFFE3C4, 0x886B3F3A, night), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, hy - H * 0.05f, W, hy + H * 0.02f, fill)
        fill.shader = null

        // ground and runway
        fill.shader = LinearGradient(
            0f, hy, 0f, H, intArrayOf(mix(0xFF7A8090, 0xFF171821, night), mix(0xFF565B68, 0xFF08090E, night)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, hy, W, H, fill)
        fill.shader = null

        val topHalf = W * 0.012f
        val botHalf = W * 0.80f
        fun half(y: Float) = topHalf + (botHalf - topHalf) * ((y - hy) / (H - hy))
        path.reset()
        path.moveTo(cx - topHalf, hy); path.lineTo(cx + topHalf, hy); path.lineTo(cx + botHalf, H); path.lineTo(cx - botHalf, H); path.close()
        fill.color = mix(0xFF454A57, 0xFF0C0D13, night)
        canvas.drawPath(path, fill)

        // centre dashes and edge lights rushing towards the viewer
        val count = 14
        for (i in 0 until count) {
            val z = ((i + t * 0.9f) % count) / count
            val y = hy + (H - hy) * z.pow(2.2f)
            val z2 = (z + 0.028f).coerceAtMost(1f)
            val y2 = hy + (H - hy) * z2.pow(2.2f)
            val hw = half(y) * 0.016f
            fill.color = mix(0xFFFFFFFF, 0xFFE9E4D2, night)
            fill.alpha = (60 + 195 * z).roundToInt().coerceAtMost(255)
            path.reset()
            path.moveTo(cx - hw * 0.6f, y); path.lineTo(cx + hw * 0.6f, y); path.lineTo(cx + hw, y2); path.lineTo(cx - hw, y2); path.close()
            canvas.drawPath(path, fill)
            fill.alpha = 255

            val r = 1.2f + 7f * z
            for (side in intArrayOf(-1, 1)) {
                val lx = cx + side * half(y) * 0.97f
                fill.color = withAlpha(0xFFFFB347.toInt(), 0.35f + 0.65f * night)
                canvas.drawCircle(lx, y, r, fill)
                if (night > 0.05f) {
                    fill.color = withAlpha(0xFFFFB347.toInt(), 0.22f * night)
                    canvas.drawCircle(lx, y, r * 3.2f, fill)
                }
            }
        }

        drawCar(canvas, W, H, hy, t, night)
    }

    private fun drawPlane(canvas: Canvas, W: Float, H: Float, t: Float, night: Float) {
        val phase = (t / 26f) % 1f
        val px = W * (-0.15f + 1.3f * phase)
        val py = H * 0.2f + sin(phase * 2f * PI.toFloat()) * H * 0.012f
        val s = W * 0.09f
        // contrail by day
        if (night < 0.95f) {
            fill.shader = LinearGradient(px - s * 6f, py, px - s, py, Color.TRANSPARENT, withAlpha(Color.WHITE, 0.75f * (1f - night)), Shader.TileMode.CLAMP)
            rect.set(px - s * 6f, py - s * 0.05f, px - s, py + s * 0.05f)
            canvas.drawRect(rect, fill)
            fill.shader = null
        }
        fill.color = mix(0xFF2A3345, 0xFF05060B, night)
        rect.set(px - s, py - s * 0.12f, px + s, py + s * 0.12f)
        canvas.drawOval(rect, fill)
        path.reset()  // wing
        path.moveTo(px - s * 0.1f, py); path.lineTo(px - s * 0.5f, py + s * 0.5f); path.lineTo(px - s * 0.28f, py + s * 0.5f); path.lineTo(px + s * 0.22f, py); path.close()
        canvas.drawPath(path, fill)
        path.reset()  // tail fin
        path.moveTo(px - s * 0.82f, py - s * 0.02f); path.lineTo(px - s * 1.0f, py - s * 0.4f); path.lineTo(px - s * 0.66f, py - s * 0.02f); path.close()
        canvas.drawPath(path, fill)
        if (night > 0.2f) {
            val blink = sin(t * 5f) > 0.55f
            fill.color = withAlpha(0xFFFF3B3B.toInt(), if (blink) night else night * 0.25f)
            canvas.drawCircle(px - s * 0.45f, py + s * 0.48f, s * 0.045f, fill)
            fill.color = withAlpha(Color.WHITE, if (sin(t * 7f) > 0.9f) night else 0f)
            canvas.drawCircle(px - s * 0.98f, py - s * 0.36f, s * 0.06f, fill)
        }
    }

    private fun drawCar(canvas: Canvas, W: Float, H: Float, hy: Float, t: Float, night: Float) {
        val cx = W / 2f
        val carW = W * 0.56f
        val carH = carW * 0.36f
        val baseY = H * 0.9f + sin(t * 3.1f) * 1.2f
        val lightY = baseY - carH * 0.3f

        // headlight beams on the runway
        if (night > 0.05f) {
            for (side in intArrayOf(-1, 1)) {
                val lx = cx + side * carW * 0.3f
                fill.shader = LinearGradient(lx, lightY, cx, hy + (H - hy) * 0.25f, withAlpha(0xFFFFF1CF.toInt(), 0.32f * night), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                path.reset()
                path.moveTo(lx - carW * 0.06f, lightY); path.lineTo(lx + carW * 0.06f, lightY)
                path.lineTo(cx + side * carW * 0.05f + carW * 0.06f, hy + (H - hy) * 0.25f); path.lineTo(cx + side * carW * 0.05f - carW * 0.06f, hy + (H - hy) * 0.25f); path.close()
                canvas.drawPath(path, fill)
                fill.shader = null
            }
        }
        // body: a low generic coupe seen from the front, no badge or grille
        fill.color = mix(0xFF2B2F3A, 0xFF07080C, night)
        rect.set(cx - carW / 2f, baseY - carH * 0.55f, cx + carW / 2f, baseY)
        canvas.drawRoundRect(rect, carH * 0.22f, carH * 0.22f, fill)
        path.reset()
        path.moveTo(cx - carW * 0.34f, baseY - carH * 0.5f); path.lineTo(cx - carW * 0.2f, baseY - carH * 0.98f)
        path.lineTo(cx + carW * 0.2f, baseY - carH * 0.98f); path.lineTo(cx + carW * 0.34f, baseY - carH * 0.5f); path.close()
        canvas.drawPath(path, fill)
        if (night > 0.05f) {   // a soft rim light along the roof and shoulders so the dark car still reads against the dark runway
            fill.style = Paint.Style.STROKE
            fill.strokeWidth = 2.5f
            fill.color = withAlpha(0xFF9FB4FF.toInt(), 0.38f * night)
            path.reset()
            path.moveTo(cx - carW * 0.34f, baseY - carH * 0.5f); path.lineTo(cx - carW * 0.2f, baseY - carH * 0.98f)
            path.lineTo(cx + carW * 0.2f, baseY - carH * 0.98f); path.lineTo(cx + carW * 0.34f, baseY - carH * 0.5f)
            canvas.drawPath(path, fill)
            rect.set(cx - carW / 2f, baseY - carH * 0.55f, cx + carW / 2f, baseY)
            canvas.drawRoundRect(rect, carH * 0.22f, carH * 0.22f, fill)
            fill.style = Paint.Style.FILL
        }
        fill.color = mix(0xFF4A5468, 0xFF161A26, night)   // windscreen
        path.reset()
        path.moveTo(cx - carW * 0.28f, baseY - carH * 0.52f); path.lineTo(cx - carW * 0.17f, baseY - carH * 0.9f)
        path.lineTo(cx + carW * 0.17f, baseY - carH * 0.9f); path.lineTo(cx + carW * 0.28f, baseY - carH * 0.52f); path.close()
        canvas.drawPath(path, fill)
        fill.color = mix(0xFF14161C, 0xFF000000, night)   // wheels
        canvas.drawRoundRect(cx - carW * 0.47f, baseY - carH * 0.12f, cx - carW * 0.33f, baseY + carH * 0.06f, 6f, 6f, fill)
        canvas.drawRoundRect(cx + carW * 0.33f, baseY - carH * 0.12f, cx + carW * 0.47f, baseY + carH * 0.06f, 6f, 6f, fill)
        // headlights
        for (side in intArrayOf(-1, 1)) {
            val lx = cx + side * carW * 0.3f
            if (night > 0.05f) {
                fill.shader = RadialGradient(lx, lightY, carW * 0.3f, withAlpha(0xFFFFF4D6.toInt(), 0.7f * night), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(lx, lightY, carW * 0.3f, fill)
                fill.shader = null
            }
            fill.color = mix(0xFFE8EDF5, 0xFFFFF7E0, night)
            rect.set(lx - carW * 0.085f, lightY - carH * 0.045f, lx + carW * 0.085f, lightY + carH * 0.045f)
            canvas.drawRoundRect(rect, carH * 0.04f, carH * 0.04f, fill)
        }
    }
}

// =====================================================================================================================
// City rain: layered skyline in the rain, neon at night, overcast by day
// =====================================================================================================================
private class CityRainScene : LiveScene {
    private val fill = paint()
    private val line = paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private class Building(val x: Float, val w: Float, val h: Float, val neon: Int, val phase: Float, val hasSign: Boolean)
    private class Drop(val x: Float, val y: Float, val speed: Float, val len: Float)

    private val layers: List<List<Building>> = List(3) { layer ->
        val rnd = Random(30 + layer)
        var x = 0f
        buildList {
            while (x < 1.0f) {
                val w = 0.07f + rnd.nextFloat() * 0.09f
                val neon = intArrayOf(0xFF2DE2E6.toInt(), 0xFFFF3EA5.toInt(), 0xFFFFB347.toInt())[rnd.nextInt(3)]
                add(Building(x, w, 0.08f + rnd.nextFloat() * (0.2f + 0.04f * (2 - layer)), neon, rnd.nextFloat() * 6.28f, rnd.nextFloat() < 0.45f))
                x += w
            }
        }
    }
    private val drops: List<Drop> = run {
        val rnd = Random(99)
        List(130) { Drop(rnd.nextFloat(), rnd.nextFloat(), 0.9f + rnd.nextFloat() * 0.9f, 0.025f + rnd.nextFloat() * 0.03f) }
    }

    override fun draw(canvas: Canvas, w: Int, h: Int, t: Float, night: Float) {
        val W = w.toFloat()
        val H = h.toFloat()

        fill.shader = LinearGradient(
            0f, 0f, 0f, H * 0.8f, intArrayOf(mix(0xFFB4BDC9, 0xFF090B1B, night), mix(0xFFDDE2E9, 0xFF2A1740, night)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, W, H, fill)
        fill.shader = null

        val baseY = floatArrayOf(H * 0.62f, H * 0.72f, H * 0.84f)
        val speed = floatArrayOf(4f, 9f, 16f)
        val dayColor = longArrayOf(0xFF9AA6B8, 0xFF7C879A, 0xFF626D80)
        val nightColor = longArrayOf(0xFF161B38, 0xFF0F132A, 0xFF080A16)
        for (layer in 0..2) {
            val offset = (t * speed[layer]) % W
            for (shift in intArrayOf(0, 1)) {
                val dx = shift * W - offset
                for (b in layers[layer]) {
                    val bx = b.x * W + dx
                    val bw = b.w * W
                    val bh = b.h * H
                    if (bx > W || bx + bw < 0f) continue
                    fill.color = mix(dayColor[layer], nightColor[layer], night)
                    canvas.drawRect(bx, baseY[layer] - bh, bx + bw, baseY[layer], fill)
                    if (night > 0.05f) {
                        val flicker = (0.65f + 0.35f * sin(t * 1.3f + b.phase)) * night
                        // a column of lit windows and, on some buildings, a neon sign
                        fill.color = withAlpha(0xFFFFD27A.toInt(), 0.5f * flicker)
                        var wy = baseY[layer] - bh + 10f
                        while (wy < baseY[layer] - 8f) {
                            canvas.drawRect(bx + bw * 0.2f, wy, bx + bw * 0.34f, wy + 6f, fill)
                            canvas.drawRect(bx + bw * 0.6f, wy, bx + bw * 0.74f, wy + 6f, fill)
                            wy += 22f
                        }
                        if (b.hasSign && layer > 0) {
                            fill.color = withAlpha(b.neon, 0.85f * flicker)
                            canvas.drawRect(bx + bw * 0.12f, baseY[layer] - bh * 0.55f, bx + bw * 0.88f, baseY[layer] - bh * 0.55f + 5f, fill)
                            fill.color = withAlpha(b.neon, 0.16f * flicker)
                            canvas.drawRect(bx, baseY[layer] - bh * 0.55f - 14f, bx + bw, baseY[layer] - bh * 0.55f + 20f, fill)
                        }
                    }
                }
            }
        }

        // wet street
        fill.shader = LinearGradient(0f, H * 0.84f, 0f, H, mix(0xFF6C7686, 0xFF0A0B16, night), mix(0xFF4C5566, 0xFF05060C, night), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, H * 0.84f, W, H, fill)
        fill.shader = null
        if (night > 0.05f) {
            val colors = intArrayOf(0xFF2DE2E6.toInt(), 0xFFFF3EA5.toInt(), 0xFFFFB347.toInt())
            for (i in 0 until 6) {
                val ry = H * (0.86f + 0.025f * i) + sin(t * 0.8f + i) * 2f
                fill.color = withAlpha(colors[i % 3], 0.18f * night)
                canvas.drawRect(W * (0.1f + 0.13f * i), ry, W * (0.1f + 0.13f * i + 0.2f), ry + 5f, fill)
            }
        }

        // rain
        line.strokeWidth = 2.2f
        line.color = withAlpha(mix(0xFF5B6678, 0xFF9CC8FF, night), 0.55f)
        for (d in drops) {
            val y = ((d.y + t * d.speed * 0.9f) % 1.1f) * H - d.len * H
            val x = d.x * W + (y / H) * W * 0.06f
            canvas.drawLine(x, y, x - W * 0.012f, y + d.len * H, line)
        }
    }
}

// =====================================================================================================================
// Starfall: a starry night with shooting stars; by day a bright sky with drifting clouds
// =====================================================================================================================
private class StarfallScene : LiveScene {
    private val fill = paint()
    private val line = paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private class Star(val x: Float, val y: Float, val r: Float, val speed: Float, val phase: Float)
    private class Cloud(val x: Float, val y: Float, val size: Float, val speed: Float)

    private val stars: List<Star> = run {
        val rnd = Random(3)
        List(150) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.85f, 0.8f + rnd.nextFloat() * 1.8f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f) }
    }
    private val clouds: List<Cloud> = run {
        val rnd = Random(8)
        List(6) { Cloud(rnd.nextFloat(), 0.1f + rnd.nextFloat() * 0.55f, 0.16f + rnd.nextFloat() * 0.16f, 0.004f + rnd.nextFloat() * 0.006f) }
    }

    override fun draw(canvas: Canvas, w: Int, h: Int, t: Float, night: Float) {
        val W = w.toFloat()
        val H = h.toFloat()

        fill.shader = LinearGradient(
            0f, 0f, 0f, H,
            intArrayOf(mix(0xFF7FB5EE, 0xFF03040C, night), mix(0xFFB7DAF7, 0xFF101B3C, night), mix(0xFFEAF5FF, 0xFF2B2150, night)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, W, H, fill)
        fill.shader = null

        // sun by day, moon by night
        if (night < 0.95f) {
            fill.shader = RadialGradient(W * 0.78f, H * 0.16f, W * 0.55f, withAlpha(0xFFFFF4C2.toInt(), 0.85f * (1f - night)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawCircle(W * 0.78f, H * 0.16f, W * 0.55f, fill)
            fill.shader = null
        }
        if (night > 0.05f) {
            fill.shader = RadialGradient(W * 0.25f, H * 0.14f, W * 0.3f, withAlpha(0xFFBFD4FF.toInt(), 0.35f * night), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawCircle(W * 0.25f, H * 0.14f, W * 0.3f, fill)
            fill.shader = null
            fill.color = withAlpha(0xFFF4F1E4.toInt(), night)
            canvas.drawCircle(W * 0.25f, H * 0.14f, W * 0.045f, fill)
            fill.color = withAlpha(mix(0xFF7FB5EE, 0xFF0A1030, 1f), night)
            canvas.drawCircle(W * 0.25f + W * 0.02f, H * 0.14f - W * 0.006f, W * 0.04f, fill)
        }

        if (night > 0.02f) {
            fill.color = Color.WHITE
            for (s in stars) {
                val tw = 0.45f + 0.55f * sin(t * s.speed + s.phase)
                fill.alpha = (230 * night * tw).roundToInt().coerceIn(0, 255)
                canvas.drawCircle(s.x * W, s.y * H, s.r, fill)
            }
            fill.alpha = 255

            // a shooting star every few seconds
            val cycle = 7.5f
            val k = (t % cycle) / 1.1f
            if (k in 0f..1f) {
                val sx = W * (0.95f - 0.6f * k) - (t / cycle).toInt() % 3 * W * 0.1f
                val sy = H * (0.05f + 0.34f * k)
                line.strokeWidth = 3f
                line.shader = LinearGradient(sx, sy, sx + W * 0.2f, sy - H * 0.1f, withAlpha(Color.WHITE, night * (1f - k)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawLine(sx, sy, sx + W * 0.2f, sy - H * 0.1f, line)
                line.shader = null
            }
        }

        // clouds by day
        if (night < 0.98f) {
            for (c in clouds) {
                val cx = ((c.x - t * c.speed) % 1.3f + 1.3f) % 1.3f * W - W * 0.15f
                val cy = c.y * H
                val s = c.size * W
                fill.color = withAlpha(Color.WHITE, 0.85f * (1f - night))
                canvas.drawCircle(cx, cy, s * 0.5f, fill)
                canvas.drawCircle(cx + s * 0.45f, cy + s * 0.08f, s * 0.38f, fill)
                canvas.drawCircle(cx - s * 0.45f, cy + s * 0.1f, s * 0.34f, fill)
                canvas.drawCircle(cx + s * 0.15f, cy - s * 0.18f, s * 0.36f, fill)
            }
        }

        // soft ground glow at the bottom
        fill.shader = LinearGradient(0f, H * 0.82f, 0f, H, Color.TRANSPARENT, mix(0x66FFFFFF, 0x663A2B6B, night), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, H * 0.82f, W, H, fill)
        fill.shader = null
    }
}
