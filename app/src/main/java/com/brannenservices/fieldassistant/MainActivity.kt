package com.brannenservices.fieldassistant

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var handsFreeButton: Button
    private var handsFreeRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestMic()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
            setBackgroundColor(0xFF0B0D10.toInt())
        }

        val title = TextView(this).apply {
            text = "FIELD ASSISTANT"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val setupAccessibility = Button(this).apply {
            text = "ENABLE ACCESSIBILITY CONTROL"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val testTextMode = Button(this).apply {
            text = "TEST CHATGPT TEXT MODE"
            textSize = 20f
            setOnClickListener {
                sendTextToChatGpt("Reply with exactly: Field Assistant text mode is working.")
            }
        }

        handsFreeButton = Button(this).apply {
            text = "START HANDS-FREE MODE"
            setOnClickListener { toggleHandsFree() }
        }

        root.addView(title)
        root.addView(status)
        root.addView(setupAccessibility)
        root.addView(testTextMode)
        root.addView(handsFreeButton)
        setContentView(root)
    }

    private fun sendTextToChatGpt(message: String) {
        if (!isAccessibilityEnabled()) {
            status.text = "Enable Field Assistant Control first"
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        getSharedPreferences(FieldAccessibilityService.PREFS, MODE_PRIVATE)
            .edit()
            .putString(FieldAccessibilityService.KEY_PENDING_TEXT, message)
            .apply()

        val launch = packageManager.getLaunchIntentForPackage(FieldAccessibilityService.CHATGPT_PACKAGE)
        if (launch == null) {
            status.text = "ChatGPT app not found"
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        status.text = "Sending through ChatGPT text chat…"
    }

    private fun toggleHandsFree() {
        if (!handsFreeRunning) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestMic()
                status.text = "Microphone permission is required"
                return
            }
            ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
            handsFreeRunning = true
            handsFreeButton.text = "STOP HANDS-FREE MODE"
            status.text = "Hands-free service active"
        } else {
            stopService(Intent(this, WakeWordService::class.java))
            handsFreeRunning = false
            handsFreeButton.text = "START HANDS-FREE MODE"
            status.text = "Hands-free service stopped"
        }
    }

    private fun requestMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, FieldAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun updateStatus() {
        status.text = if (isAccessibilityEnabled()) {
            "Accessibility control enabled"
        } else {
            "Accessibility control needs to be enabled"
        }
    }
}
