package com.brannenservices.fieldassistant

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
    companion object {
        private const val ACTION_USB_PERMISSION = "com.brannenservices.photorescue.USB_PERMISSION"
    }

    private lateinit var status: TextView
    private lateinit var resultText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var usbManager: UsbManager

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_USB_PERMISSION) return
            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (device != null && granted) {
                status.text = "USB access granted"
                showUsbDiagnostics(device)
            } else {
                status.text = "USB access was not granted"
            }
        }
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scanSelectedFolder(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        registerUsbReceiver()
        buildUi()
        refreshUsbStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshUsbStatus()
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbPermissionReceiver, filter)
        }
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

        val diagnostic = Button(this).apply {
            text = "RUN USB DIAGNOSTIC"
            setOnClickListener { runUsbDiagnostic() }
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
            text = "V0.2 is read-only. It will not write anything to the SD card."
            textSize = 15f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.START
            setPadding(0, 20, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(detect)
        root.addView(diagnostic)
        root.addView(quickScan)
        root.addView(deepScan)
        root.addView(progress)
        root.addView(resultText)
        setContentView(root)
    }

    private fun refreshUsbStatus() {
        val devices = usbManager.deviceList.values.toList()
        if (devices.isEmpty()) {
            status.text = "No USB adapter detected"
            resultText.text = "Connect the microSD USB adapter to the phone, then press CHECK USB / SD ADAPTER."
            return
        }

        val massStorageCount = devices.count { hasMassStorageInterface(it) }
        status.text = if (massStorageCount > 0) {
            "USB storage device detected"
        } else {
            "USB device detected — storage interface not confirmed"
        }
        resultText.text = devices.joinToString("\n\n") { basicDeviceSummary(it) }
    }

    private fun runUsbDiagnostic() {
        val devices = usbManager.deviceList.values.toList()
        if (devices.isEmpty()) {
            status.text = "No USB adapter detected"
            return
        }

        val target = devices.firstOrNull { hasMassStorageInterface(it) } ?: devices.first()
        if (!usbManager.hasPermission(target)) {
            status.text = "Requesting USB access…"
            requestUsbPermission(target)
            return
        }
        showUsbDiagnostics(target)
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun showUsbDiagnostics(device: UsbDevice) {
        val hasPermission = usbManager.hasPermission(device)
        val massInterfaces = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .filter { it.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE }

        status.text = if (massInterfaces.isNotEmpty()) {
            "Mass-storage interface confirmed"
        } else {
            "Mass-storage interface not found"
        }

        resultText.text = buildString {
            append("PHOTO RESCUE USB DIAGNOSTIC\n\n")
            append("Device name: ${device.deviceName}\n")
            append("Vendor ID: ${device.vendorId}\n")
            append("Product ID: ${device.productId}\n")
            append("USB device class: ${device.deviceClass}\n")
            append("USB permission: ${if (hasPermission) "GRANTED" else "NOT GRANTED"}\n")
            append("Interfaces: ${device.interfaceCount}\n")
            append("Mass-storage interfaces: ${massInterfaces.size}\n\n")

            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                append("Interface $i\n")
                append("  class=${intf.interfaceClass} subclass=${intf.interfaceSubclass} protocol=${intf.interfaceProtocol}\n")
                append("  endpoints=${intf.endpointCount}\n")
                for (e in 0 until intf.endpointCount) {
                    val endpoint = intf.getEndpoint(e)
                    val direction = if (endpoint.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                    val type = when (endpoint.type) {
                        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                        UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                        else -> endpoint.type.toString()
                    }
                    append("    EP$e: $direction $type maxPacket=${endpoint.maxPacketSize}\n")
                }
                append("\n")
            }

            if (massInterfaces.isNotEmpty()) {
                append("RESULT: This adapter exposes USB Mass Storage to Android. That is the interface we need to attempt raw block reads for deleted-photo recovery.\n")
            } else {
                append("RESULT: Android can see the USB device, but it is not exposing a standard Mass Storage interface. Raw recovery may need a different adapter.\n")
            }
            append("\nDiagnostic only — no data was written to the SD card.")
        }
    }

    private fun basicDeviceSummary(device: UsbDevice): String {
        val storage = if (hasMassStorageInterface(device)) "Mass storage: YES" else "Mass storage: not confirmed"
        return "USB device: ${device.deviceName}\nVID:${device.vendorId} PID:${device.productId}\n$storage\nPermission: ${if (usbManager.hasPermission(device)) "granted" else "not granted"}"
    }

    private fun hasMassStorageInterface(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
        }
        return device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE
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
                    append("The quick scan reads files Android already knows about. Deleted-photo recovery will use the raw USB mass-storage path tested by RUN USB DIAGNOSTIC.")
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
