package com.brannen.aamirrorprobe

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class CarActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var image: ImageView
    private lateinit var message: TextView
    private var lastBytes: ByteArray? = null

    private val refresh = object : Runnable {
        override fun run() {
            val bytes = CaptureService.latestJpeg.get()
            if (bytes != null && bytes !== lastBytes) {
                lastBytes = bytes
                image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                message.visibility = View.GONE
            } else if (!CaptureService.running) {
                message.text = "Start AA Mirror Probe on the phone and approve screen capture first."
                message.visibility = View.VISIBLE
            }
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        image = ImageView(this).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        message = TextView(this).apply {
            text = "Waiting for phone capture…"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        setContentView(FrameLayout(this).apply {
            addView(image, FrameLayout.LayoutParams(-1, -1))
            addView(message, FrameLayout.LayoutParams(-1, -1))
        })
    }

    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
}
