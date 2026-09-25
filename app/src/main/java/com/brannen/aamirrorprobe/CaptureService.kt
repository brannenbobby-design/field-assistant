package com.brannen.aamirrorprobe

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var lastFrameAt = 0L

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "screen_capture"
        val latestJpeg = AtomicReference<ByteArray?>(null)
        @Volatile var running = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(42, Notification.Builder(this, CHANNEL)
            .setContentTitle("AA Mirror Probe")
            .setContentText("Phone screen capture is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build())

        if (projection == null && intent != null) {
            val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val data = if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
            if (code == Activity.RESULT_OK && data != null) startProjection(code, data)
        }
        return START_NOT_STICKY
    }

    private fun startProjection(code: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(code, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, Handler(mainLooper))

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        thread = HandlerThread("capture-frames").also { it.start() }
        val handler = Handler(thread!!.looper)
        reader?.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val now = System.currentTimeMillis()
                if (now - lastFrameAt < 250) return@setOnImageAvailableListener
                lastFrameAt = now
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val paddedWidth = width + (rowStride - pixelStride * width) / pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                val active = cropLetterbox(cropped)
                val out = ByteArrayOutputStream()
                active.compress(Bitmap.CompressFormat.JPEG, 75, out)
                latestJpeg.set(out.toByteArray())
                if (active !== cropped) active.recycle()
                cropped.recycle()
                padded.recycle()
            } finally {
                image.close()
            }
        }, handler)

        projection?.createVirtualDisplay(
            "AA-Mirror-Capture", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, handler
        )
        running = true
    }

    /**
     * MediaProjection keeps the dimensions it had when capture started. If the
     * phone rotates later, Android letterboxes the live screen inside that old
     * canvas. Remove only large, near-black outer bands; leave ordinary dark
     * app backgrounds alone.
     */
    private fun cropLetterbox(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val step = (minOf(w, h) / 240).coerceAtLeast(4)

        fun rowHasContent(y: Int): Boolean {
            var bright = 0
            var samples = 0
            var x = 0
            while (x < w) {
                val c = source.getPixel(x, y)
                val max = maxOf((c shr 16) and 255, (c shr 8) and 255, c and 255)
                if (max > 22) bright++
                samples++
                x += step
            }
            return bright >= maxOf(2, samples / 45)
        }

        fun colHasContent(x: Int, top: Int, bottom: Int): Boolean {
            var bright = 0
            var samples = 0
            var y = top
            while (y <= bottom) {
                val c = source.getPixel(x, y)
                val max = maxOf((c shr 16) and 255, (c shr 8) and 255, c and 255)
                if (max > 22) bright++
                samples++
                y += step
            }
            return bright >= maxOf(2, samples / 45)
        }

        var top = 0
        while (top < h - step && !rowHasContent(top)) top += step
        var bottom = h - 1
        while (bottom > top + step && !rowHasContent(bottom)) bottom -= step
        var left = 0
        while (left < w - step && !colHasContent(left, top, bottom)) left += step
        var right = w - 1
        while (right > left + step && !colHasContent(right, top, bottom)) right -= step

        val removedX = left + (w - 1 - right)
        val removedY = top + (h - 1 - bottom)
        if (removedX < w / 12 && removedY < h / 12) return source

        val margin = step * 2
        left = (left - margin).coerceAtLeast(0)
        top = (top - margin).coerceAtLeast(0)
        right = (right + margin).coerceAtMost(w - 1)
        bottom = (bottom + margin).coerceAtMost(h - 1)
        val cropW = right - left + 1
        val cropH = bottom - top + 1
        if (cropW < w / 4 || cropH < h / 4) return source
        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }

    override fun onDestroy() {
        running = false
        latestJpeg.set(null)
        reader?.close()
        projection?.stop()
        thread?.quitSafely()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Screen capture", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
