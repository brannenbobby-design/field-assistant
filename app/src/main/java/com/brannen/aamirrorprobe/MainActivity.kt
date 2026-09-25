package com.brannen.aamirrorprobe

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val CAPTURE = 77
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40,40,40,40)
            setBackgroundColor(Color.rgb(16,18,20))
        }
        val title = TextView(this).apply {
            text = "AA MIRROR PROBE"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = if (CaptureService.running)
                "Screen capture is running. Open AA Mirror Probe on Uconnect while parked."
            else
                "Start capture here on the phone, approve Android's prompt, then open AA Mirror Probe on Uconnect."
            textSize = 19f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(20,30,20,30)
        }
        val btn = Button(this).apply {
            text = "START SCREEN CAPTURE"
            setOnClickListener {
                val m = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(m.createScreenCaptureIntent(), CAPTURE)
            }
        }
        val note = TextView(this).apply {
            text = "Android will show its normal screen-sharing warning. After approval, leave this app and open whatever you want to test. Android Auto will still enforce its parked-only rules."
            textSize = 15f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(20,30,20,0)
        }
        box.addView(title)
        box.addView(status)
        box.addView(btn)
        box.addView(note)
        setContentView(box)
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==CAPTURE) {
            if(resultCode==RESULT_OK && data != null) {
                val service = Intent(this, CaptureService::class.java).apply {
                    putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CaptureService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(service)
                status.text = "Capture approved and starting. Now open AA Mirror Probe on Uconnect."
            } else {
                status.text = "Screen-capture permission was denied."
            }
        }
    }
}
