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
                val out = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, 70, out)
                latestJpeg.set(out.toByteArray())
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
