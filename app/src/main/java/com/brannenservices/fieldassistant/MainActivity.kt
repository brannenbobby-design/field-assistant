package com.brannenservices.fieldassistant

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var resultText: TextView
    private lateinit var progress: ProgressBar

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scanSelectedFolder(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshUsbStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshUsbStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(44, 64, 44, 44)
            setBackgroundColor(0xFF0B0D10.toInt())
        }

        val title = TextView(this).apply {
            text = "PHOTO RESCUE"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Personal SD Card Recovery Tool"
            textSize = 16f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        }

        status = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }

        val detect = Button(this).apply {
            text = "CHECK USB / SD ADAPTER"
            setOnClickListener { refreshUsbStatus() }
        }

        val quickScan = Button(this).apply {
            text = "QUICK SCAN VISIBLE PHOTOS"
            setOnClickListener {
                status.text = "Choose the SD card or a folder on it"
                folderPicker.launch(null)
            }
        }

        val deepScan = Button(this).apply {
            text = "DEEP RECOVERY SCAN — NEXT"
            isEnabled = false
        }

        progress = ProgressBar(this).apply {
            visibility = ProgressBar.GONE
            setPadding(0, 24, 0, 12)
        }

        resultText = TextView(this).apply {
            text = "V0.1 is read-only. It will not write anything to the SD card."
            textSize = 16f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.START
            setPadding(0, 20, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(detect)
        root.addView(quickScan)
        root.addView(deepScan)
        root.addView(progress)
        root.addView(resultText)
        setContentView(root)
    }

    private fun refreshUsbStatus() {
        val manager = getSystemService(USB_SERVICE) as UsbManager
        val devices = manager.deviceList.values.toList()
        if (devices.isEmpty()) {
            status.text = "No USB storage adapter detected"
            return
        }

        val details = devices.joinToString("\n") { device ->
            "USB device: ${device.deviceName}  VID:${device.vendorId} PID:${device.productId}"
        }
        status.text = "USB device detected"
        resultText.text = details + "\n\nNext: choose the SD card for a safe quick scan."
    }

    private fun scanSelectedFolder(uri: Uri) {
        val root = DocumentFile.fromTreeUri(this, uri)
        if (root == null || !root.canRead()) {
            status.text = "Could not read that location"
            return
        }

        progress.visibility = ProgressBar.VISIBLE
        status.text = "Scanning visible files…"
        resultText.text = ""

        Thread {
            val stats = ScanStats()
            walk(root, stats)
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                status.text = "Quick scan complete"
                resultText.text = buildString {
                    append("Photos found: ${stats.images}\n")
                    append("JPEG: ${stats.jpeg}\n")
                    append("PNG: ${stats.png}\n")
                    append("Other files checked: ${stats.other}\n")
                    append("Total visible files checked: ${stats.total}\n\n")
                    append("This quick scan proves Android can read the card. Deleted-photo recovery requires the deep raw-sector scanner, which is the next engine to build.")
                }
            }
        }.start()
    }

    private fun walk(file: DocumentFile, stats: ScanStats) {
        if (file.isDirectory) {
            file.listFiles().forEach { walk(it, stats) }
            return
        }

        stats.total++
        val name = file.name?.lowercase(Locale.US).orEmpty()
        when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> {
                stats.images++
                stats.jpeg++
            }
            name.endsWith(".png") -> {
                stats.images++
                stats.png++
            }
            else -> stats.other++
        }
    }

    private data class ScanStats(
        var total: Int = 0,
        var images: Int = 0,
        var jpeg: Int = 0,
        var png: Int = 0,
        var other: Int = 0
    )
}
