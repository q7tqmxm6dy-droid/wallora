package com.wallora.app.live

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.wallora.app.R
import com.wallora.app.data.WallpaperCatalog

/**
 * A live wallpaper that slowly crossfades through the bundled catalogue with a gentle
 * Ken Burns zoom. Pauses when it isn't visible so it doesn't drain the battery.
 */
class WalloraLiveWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = SlideEngine()

    private inner class SlideEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val order = WallpaperCatalog.wallpapers.shuffled()
        private var index = 0
        private var current: Bitmap? = null
        private var next: Bitmap? = null
        private var progress = 0f
        private var visible = false
        private var lastFrame = 0L

        /** How long each wallpaper is shown, in milliseconds. */
        private val holdMs = 7_000L / 2
        private val frameDelayMs = 33L

        private val loop = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                val delta = (now - lastFrame).coerceAtMost(120L)
                lastFrame = now
                progress += delta.toFloat() / holdMs
                if (progress >= 1f) {
                    progress = 0f
                    advance()
                }
                draw()
                handler.postDelayed(this, frameDelayMs)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            current = decode(order.firstOrNull()?.resId)
            next = decode(order.getOrNull(1)?.resId)
            index = 2
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) start() else stop()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stop()
        }

        override fun onDestroy() {
            stop()
            current?.recycle(); next?.recycle()
            current = null; next = null
            super.onDestroy()
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
            current?.recycle()
            current = next
            next = decode(order[index % order.size].resId)
            index += 1
        }

        private fun decode(resId: Int?): Bitmap? {
            resId ?: return null
            return runCatching { BitmapFactory.decodeResource(resources, resId) }.getOrNull()
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val w = canvas.width
                val h = canvas.height
                canvas.drawColor(Color.BLACK)

                val nextBitmap = next
                if (nextBitmap != null) {
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                    paint.alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
                    drawFitted(canvas, nextBitmap, w, h, 1.02f + 0.08f * progress, paint)
                }
                current?.let { bitmap ->
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                    paint.alpha = ((1f - progress).coerceIn(0f, 1f) * 255).toInt()
                    drawFitted(canvas, bitmap, w, h, 1.10f - 0.08f * progress, paint)
                }
            } catch (_: Throwable) {
                // surface went away mid-frame; just skip it
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
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
