package com.brannenservices.fieldassistant

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val captureRequest = 77
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.rgb(16, 18, 20))
        }
        val title = TextView(this).apply {
            text = "AA MIRROR PROBE"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "TEST 1: If you can read this on Uconnect, Android Auto launched our full Activity."
            textSize = 19f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(20, 30, 20, 30)
        }
        val button = Button(this).apply {
            text = "REQUEST PHONE SCREEN CAPTURE"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
            }
        }
        val note = TextView(this).apply {
            text = "Prototype capability test. Use while parked. If this screen appears on Uconnect and Android grants screen-capture permission, the next build can test rendering captured frames."
            textSize = 15f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(20, 30, 20, 0)
        }
        box.addView(title); box.addView(status); box.addView(button); box.addView(note)
        setContentView(box)
    }

    @Deprecated("Deprecated in Android framework, retained for simple prototype")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest) {
            status.text = if (resultCode == RESULT_OK)
                "TEST 2 PASSED: Android granted MediaProjection screen-capture permission."
            else
                "TEST 2: Screen-capture permission was denied."
        }
    }
}
