package com.wallora.app.live

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.wallora.app.data.PreferencesRepository
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Live wallpaper with two modes:
 *  - **slideshow**: crossfades through the catalogue with a gentle Ken Burns zoom
 *  - **gradient**: a lightweight animated colour gradient (no bitmaps)
 *
 * Interval and category filter are configurable from the app's Settings screen.
 */
class WalloraLiveWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = SlideEngine()

    private inner class SlideEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private var order: List<Wallpaper> = emptyList()
        private var index = 0
        private var current: Bitmap? = null
        private var next: Bitmap? = null
        private var progress = 0f
        private var visible = false
        private var lastFrame = 0L

        private var mode = "slideshow"
        private var holdMs = 10_000L
        private var categories: Set<String> = emptySet()

        private val frameDelayMs = 33L

        private val loop = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                val delta = (now - lastFrame).coerceAtMost(120L)
                lastFrame = now
                if (mode == "slideshow") {
                    progress += delta.toFloat() / holdMs
                    if (progress >= 1f) {
                        progress = 0f
                        advance()
                    }
                }
                draw(now)
                handler.postDelayed(this, frameDelayMs)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            refreshSettings()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                refreshSettings() // pick up changed settings when you come back home
                start()
            } else {
                stop()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stop()
        }

        override fun onDestroy() {
            stop()
            scope.cancel()
            current?.recycle(); next?.recycle()
            current = null; next = null
            super.onDestroy()
        }

        private fun refreshSettings() {
            scope.launch {
                val (liveMode, seconds, cats) = PreferencesRepository(applicationContext).readLiveConfig()
                val modeChanged = liveMode != mode
                mode = liveMode
                holdMs = (seconds.coerceIn(3, 300)) * 1000L
                categories = cats
                if (modeChanged || mode == "slideshow") reloadQueue()
            }
        }

        private fun reloadQueue() {
            if (mode == "gradient") {
                current?.recycle(); current = null
                next?.recycle(); next = null
                return
            }
            val list = WallpaperCatalog.wallpapers.filter {
                categories.isEmpty() || it.categoryId in categories
            }
            if (list.isEmpty()) return
            order = list.shuffled()
            index = 0
            current?.recycle()
            next?.recycle()
            current = decode(order.getOrNull(0)?.resId)
            next = decode(order.getOrNull(1)?.resId)
            index = 2
            progress = 0f
        }

        private fun start() {
            if (!visible) return
            lastFrame = SystemClock.uptimeMillis()
            handler.removeCallbacks(loop)
            handler.post(loop)
        }

        private fun stop() {
            handler.removeCallbacks(loop)
        }

        private fun advance() {
            if (order.isEmpty()) return
            current?.recycle()
            current = next
            next = decode(order[index % order.size].resId)
            index += 1
        }

        private fun decode(resId: Int?): Bitmap? {
            resId ?: return null
            return runCatching { BitmapFactory.decodeResource(resources, resId) }.getOrNull()
        }

        private fun draw(now: Long) {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val w = canvas.width
                val h = canvas.height
                if (mode == "gradient") {
                    drawGradient(canvas, w, h, now / 1000f)
                } else {
                    drawSlideshow(canvas, w, h)
                }
            } catch (_: Throwable) {
                // surface vanished mid-frame; skip
            } finally {
                if (canvas != null) runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun drawSlideshow(canvas: Canvas, w: Int, h: Int) {
            canvas.drawColor(Color.BLACK)
            next?.let { bitmap ->
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                paint.alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
                drawFitted(canvas, bitmap, w, h, 1.02f + 0.08f * progress, paint)
            }
            current?.let { bitmap ->
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                paint.alpha = ((1f - progress).coerceIn(0f, 1f) * 255).toInt()
                drawFitted(canvas, bitmap, w, h, 1.10f - 0.08f * progress, paint)
            }
        }

        /** Lightweight animated gradient — no bitmaps, very low battery cost. */
        private fun drawGradient(canvas: Canvas, w: Int, h: Int, t: Float) {
            val hue = (t * 12f) % 360f
            val c1 = Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.96f))
            val c2 = Color.HSVToColor(floatArrayOf((hue + 55f) % 360f, 0.68f, 0.72f))
            val c3 = Color.HSVToColor(floatArrayOf((hue + 130f) % 360f, 0.70f, 0.42f))

            val base = Paint()
            base.shader = LinearGradient(
                -w * 0.25f, 0f, w * 1.25f, h.toFloat(),
                intArrayOf(c1, c2, c3), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)

            val glowX = w * (0.5f + 0.35f * sin(t * 0.21f))
            val glowY = h * (0.45f + 0.30f * cos(t * 0.17f))
            val glow = Paint()
            glow.shader = RadialGradient(
                glowX, glowY, w * 0.85f,
                Color.argb(110, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)

            val shade = Paint()
            val ry = h * (0.85f + 0.12f * cos(t * 0.13f))
            shade.shader = RadialGradient(
                w * 0.5f, ry, w * 0.9f,
                Color.argb(90, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), shade)
        }

        /** Centre-crop the bitmap to fill the surface, scaled for the Ken Burns effect. */
        private fun drawFitted(
            canvas: Canvas,
            bitmap: Bitmap,
            width: Int,
            height: Int,
            scale: Float,
            paint: Paint,
        ) {
            if (bitmap.isRecycled || width == 0 || height == 0) return
            val factor = maxOf(
                width.toFloat() / bitmap.width,
                height.toFloat() / bitmap.height,
            ) * scale
            val dx = (width - bitmap.width * factor) / 2f
            val dy = (height - bitmap.height * factor) / 2f
            val matrix = Matrix()
            matrix.postScale(factor, factor)
            matrix.postTranslate(dx, dy)
            canvas.drawBitmap(bitmap, matrix, paint)
        }
    }
}
