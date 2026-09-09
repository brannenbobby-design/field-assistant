package com.brannenservices.fieldassistant

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.hardware.usb.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.brannenservices.photorescue.USB_PERMISSION"
        private const val MAX_JPEG_BYTES = 40 * 1024 * 1024
        private const val SECTORS_PER_READ = 512
    }

    private lateinit var status: TextView
    private lateinit var resultText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var usbManager: UsbManager
    private lateinit var deepButton: Button
    private lateinit var cancelButton: Button
    @Volatile private var scanCancelled = false
    @Volatile private var scanRunning = false

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_USB_PERMISSION) return
            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                showUsbDiagnostics(device)
            } else status.text = "USB access was not granted"
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
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(usbPermissionReceiver, filter)
        buildUi()
        refreshUsbStatus()
    }

    override fun onResume() { super.onResume(); refreshUsbStatus() }
    override fun onDestroy() { scanCancelled = true; unregisterReceiver(usbPermissionReceiver); super.onDestroy() }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 42, 28, 28)
            setBackgroundColor(0xFF0B0D10.toInt())
        }
        fun actionButton(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
        root.addView(TextView(this).apply { text="PHOTO RESCUE"; textSize=30f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="Personal SD Card Recovery Tool"; textSize=16f; setTextColor(0xFF9CA3AF.toInt()); gravity=Gravity.CENTER; setPadding(0,8,0,18) })
        status = TextView(this).apply { textSize=18f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER; setPadding(0,8,0,16) }
        root.addView(status)
        root.addView(actionButton("CHECK USB / SD ADAPTER") { refreshUsbStatus() })
        root.addView(actionButton("RUN USB DIAGNOSTIC") { runUsbDiagnostic() })
        root.addView(actionButton("TEST RAW READ — READ ONLY") { runRawReadProbe() })
        deepButton = actionButton("DEEP RECOVERY SCAN — JPEG") { startDeepScan() }
        root.addView(deepButton)
        cancelButton = actionButton("CANCEL DEEP SCAN") { scanCancelled = true }.apply { isEnabled = false }
        root.addView(cancelButton)
        root.addView(actionButton("QUICK SCAN VISIBLE PHOTOS") { status.text="Choose the SD card or a folder on it"; folderPicker.launch(null) })
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max=1000; progress=0; visibility=ProgressBar.GONE; setPadding(0,18,0,8) }
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        resultText = TextView(this).apply { text="V0.4 deep recovery. USB path is read-only; recovered files are saved to the phone."; textSize=14f; setTextColor(0xFF9CA3AF.toInt()); setPadding(0,14,0,0) }
        root.addView(resultText)
        setContentView(root)
    }

    private fun refreshUsbStatus() {
        val devices = usbManager.deviceList.values.toList()
        if (devices.isEmpty()) { status.text="No USB adapter detected"; return }
        status.text = if (devices.any { massInterface(it) != null }) "USB storage device detected" else "USB device detected — storage interface not confirmed"
        resultText.text = devices.joinToString("\n\n") { "USB device: ${it.deviceName}\nVID:${it.vendorId} PID:${it.productId}\nMass storage: ${if(massInterface(it)!=null)"YES" else "NO"}\nPermission: ${if(usbManager.hasPermission(it))"granted" else "not granted"}" }
    }

    private fun targetDevice() = usbManager.deviceList.values.firstOrNull { massInterface(it) != null }

    private fun runUsbDiagnostic() {
        val d = targetDevice() ?: run { status.text="No mass-storage adapter detected"; return }
        if (!usbManager.hasPermission(d)) { requestUsbPermission(d); return }
        showUsbDiagnostics(d)
    }

    private fun requestUsbPermission(d: UsbDevice) {
        status.text="Requesting USB access…"
        val pi = PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        usbManager.requestPermission(d,pi)
    }

    private fun showUsbDiagnostics(d: UsbDevice) {
        val i = massInterface(d)
        status.text = if (i != null) "Mass-storage interface confirmed" else "Mass-storage interface not found"
        resultText.text = buildString {
            append("Device ${d.deviceName}\nVID ${d.vendorId} PID ${d.productId}\nPermission ${usbManager.hasPermission(d)}\n")
            if (i != null) {
                append("Interface class=${i.interfaceClass} subclass=${i.interfaceSubclass} protocol=${i.interfaceProtocol}\n")
                for (n in 0 until i.endpointCount) {
                    val e=i.getEndpoint(n)
                    append("EP$n ${if(e.direction==UsbConstants.USB_DIR_IN)"IN" else "OUT"} type=${e.type} maxPacket=${e.maxPacketSize}\n")
                }
            }
            append("\nDiagnostic only — no data written.")
        }
    }

    private fun massInterface(d: UsbDevice): UsbInterface? = (0 until d.interfaceCount).map { d.getInterface(it) }.firstOrNull {
        it.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE && it.interfaceProtocol == 0x50
    }

    private fun bulkEndpoints(i: UsbInterface): Pair<UsbEndpoint,UsbEndpoint>? {
        var input: UsbEndpoint? = null; var output: UsbEndpoint? = null
        for (n in 0 until i.endpointCount) {
            val e=i.getEndpoint(n)
            if (e.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (e.direction == UsbConstants.USB_DIR_IN) input=e else output=e
            }
        }
        return if (input != null && output != null) Pair(input!!,output!!) else null
    }

    private fun runRawReadProbe() {
        val d=targetDevice() ?: run { status.text="No compatible USB mass-storage device"; return }
        if (!usbManager.hasPermission(d)) { requestUsbPermission(d); return }
        progress.visibility=ProgressBar.VISIBLE; progress.isIndeterminate=true; status.text="Testing raw read…"
        thread {
            val report=try { rawProbe(d) } catch(e:Exception) { "RAW READ FAILED\n${e.javaClass.simpleName}: ${e.message}" }
            runOnUiThread { progress.visibility=ProgressBar.GONE; progress.isIndeterminate=false; status.text=if(report.startsWith("RAW READ SUCCESS"))"Raw sector access confirmed" else "Raw read test failed"; resultText.text=report }
        }
    }

    private data class Capacity(val blocks: Long, val blockSize: Int)

    private fun readCapacity(conn: UsbDeviceConnection, input: UsbEndpoint, output: UsbEndpoint, tag: Int): Capacity {
        val data = scsi(conn,input,output,byteArrayOf(0x25,0,0,0,0,0,0,0,0,0),8,tag)
        if (data.size < 8) error("READ CAPACITY returned ${data.size} bytes")
        val bb=ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val last=bb.int.toLong() and 0xffffffffL
        val size=(bb.int.toLong() and 0xffffffffL).toInt()
        if (size <= 0 || size > 65536) error("Invalid block size $size")
        return Capacity(last+1,size)
    }

    private fun rawProbe(d: UsbDevice): String {
        val intf=massInterface(d) ?: error("Bulk-only mass-storage interface missing")
        val eps=bulkEndpoints(intf) ?: error("Bulk IN/OUT endpoints missing")
        val conn=usbManager.openDevice(d) ?: error("Could not open USB device")
        try {
            if (!conn.claimInterface(intf,true)) error("Could not claim mass-storage interface")
            val cap=readCapacity(conn,eps.first,eps.second,1)
            val sector=readBlocks(conn,eps.first,eps.second,0,1,cap.blockSize,2)
            val sig=if(sector.size>=512) String.format("%02X%02X",sector[510].toInt() and 255,sector[511].toInt() and 255) else "n/a"
            val gib=(cap.blocks*cap.blockSize)/(1024.0*1024.0*1024.0)
            return "RAW READ SUCCESS\n\nBlocks: ${cap.blocks}\nBlock size: ${cap.blockSize} bytes\nCapacity: ${String.format(Locale.US,"%.2f",gib)} GiB\nSector 0 bytes read: ${sector.size}\nBoot/MBR signature @510: $sig\n\nRaw block 0 read successfully. No USB write command was sent."
        } finally { try { conn.releaseInterface(intf) } catch(_:Exception){}; conn.close() }
    }

    private fun startDeepScan() {
        if (scanRunning) return
        val d=targetDevice() ?: run { status.text="No compatible USB mass-storage device"; return }
        if (!usbManager.hasPermission(d)) { requestUsbPermission(d); return }
        scanCancelled=false; scanRunning=true
        deepButton.isEnabled=false; cancelButton.isEnabled=true
        progress.visibility=ProgressBar.VISIBLE; progress.isIndeterminate=false; progress.progress=0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        status.text="Starting deep JPEG recovery…"
        resultText.text="Scanning raw SD-card sectors. Recovered JPEGs will be saved to Pictures/PhotoRescue on this phone.\n\nDo not unplug the card during the scan."
        thread {
            val report=try { deepScan(d) } catch(e:Exception) { "DEEP SCAN FAILED\n${e.javaClass.simpleName}: ${e.message}" }
            runOnUiThread {
                scanRunning=false; deepButton.isEnabled=true; cancelButton.isEnabled=false
                progress.visibility=ProgressBar.GONE; window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                status.text=if(scanCancelled)"Deep scan cancelled" else if(report.startsWith("DEEP SCAN COMPLETE"))"Deep scan complete" else "Deep scan stopped"
                resultText.text=report
            }
        }
    }

    private fun deepScan(d: UsbDevice): String {
        val intf=massInterface(d) ?: error("Mass-storage interface missing")
        val eps=bulkEndpoints(intf) ?: error("Bulk endpoints missing")
        val conn=usbManager.openDevice(d) ?: error("Could not open USB device")
        var recovered=0; var candidates=0; var rejected=0; var bytesScanned=0L
        var tag=100
        var jpeg: ByteArrayOutputStream? = null
        var previous=-1
        try {
            if (!conn.claimInterface(intf,true)) error("Could not claim mass-storage interface")
            val cap=readCapacity(conn,eps.first,eps.second,tag++)
            var lba=0L
            while (lba < cap.blocks && !scanCancelled) {
                val count=minOf(SECTORS_PER_READ.toLong(),cap.blocks-lba).toInt()
                val chunk=readBlocks(conn,eps.first,eps.second,lba,count,cap.blockSize,tag++)
                if (chunk.size != count*cap.blockSize) error("Short read at block $lba")
                var idx=0
                while (idx < chunk.size) {
                    val b=chunk[idx].toInt() and 255
                    val current=jpeg
                    if (current == null) {
                        if (previous == 0xFF && b == 0xD8 && idx+1 < chunk.size && (chunk[idx+1].toInt() and 255) == 0xFF) {
                            jpeg=ByteArrayOutputStream(256*1024)
                            jpeg!!.write(0xFF); jpeg!!.write(0xD8)
                            candidates++
                        }
                    } else {
                        current.write(b)
                        if (current.size() > MAX_JPEG_BYTES) {
                            jpeg=null; rejected++
                        } else if (previous == 0xFF && b == 0xD9) {
                            val data=current.toByteArray()
                            if (isValidJpeg(data)) {
                                if (saveRecoveredJpeg(data,recovered+1)) recovered++ else rejected++
                            } else rejected++
                            jpeg=null
                        }
                    }
                    previous=b; idx++
                }
                lba += count
                bytesScanned += chunk.size
                if ((lba / SECTORS_PER_READ) % 32L == 0L || lba >= cap.blocks) {
                    val p=((lba*1000L)/cap.blocks).toInt().coerceIn(0,1000)
                    val pct=p/10.0
                    val rec=recovered
                    runOnUiThread {
                        progress.progress=p
                        status.text="Deep scan ${String.format(Locale.US,"%.1f",pct)}% — $rec JPEGs recovered"
                    }
                }
                if (tag == Int.MAX_VALUE) tag=100
            }
            val gib=bytesScanned/(1024.0*1024.0*1024.0)
            return if (scanCancelled) {
                "DEEP SCAN CANCELLED\n\nScanned: ${String.format(Locale.US,"%.2f",gib)} GiB\nJPEG candidates: $candidates\nRecovered: $recovered\nRejected/incomplete: $rejected\n\nRecovered files already saved remain in Pictures/PhotoRescue."
            } else {
                "DEEP SCAN COMPLETE\n\nScanned: ${String.format(Locale.US,"%.2f",gib)} GiB\nJPEG candidates: $candidates\nRecovered JPEGs: $recovered\nRejected/incomplete: $rejected\n\nSaved to Pictures/PhotoRescue on this phone.\n\nThis first carving pass can include both existing and deleted JPEGs. The next stage will add filesystem-aware classification and duplicate filtering."
            }
        } finally { try { conn.releaseInterface(intf) } catch(_:Exception){}; conn.close() }
    }

    private fun readBlocks(conn: UsbDeviceConnection,input: UsbEndpoint,output: UsbEndpoint,lba: Long,count: Int,blockSize: Int,tag: Int): ByteArray {
        require(count in 1..65535)
        val cdb=ByteArray(10)
        cdb[0]=0x28
        cdb[2]=((lba ushr 24) and 255).toByte(); cdb[3]=((lba ushr 16) and 255).toByte(); cdb[4]=((lba ushr 8) and 255).toByte(); cdb[5]=(lba and 255).toByte()
        cdb[7]=((count ushr 8) and 255).toByte(); cdb[8]=(count and 255).toByte()
        return scsi(conn,input,output,cdb,count*blockSize,tag)
    }

    private fun scsi(conn: UsbDeviceConnection,input: UsbEndpoint,output: UsbEndpoint,cdb: ByteArray,dataLen: Int,tag: Int): ByteArray {
        val cbw=ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN)
        cbw.putInt(0x43425355); cbw.putInt(tag); cbw.putInt(dataLen); cbw.put(0x80.toByte()); cbw.put(0); cbw.put(cdb.size.toByte()); cbw.put(cdb); while(cbw.position()<31) cbw.put(0)
        val out=conn.bulkTransfer(output,cbw.array(),31,5000); if(out!=31) error("CBW transfer failed ($out)")
        val data=ByteArray(dataLen); var got=0
        while(got<dataLen) {
            val n=conn.bulkTransfer(input,data,got,dataLen-got,10000)
            if(n<=0) break
            got+=n
        }
        val csw=ByteArray(13); val cswN=conn.bulkTransfer(input,csw,13,5000); if(cswN!=13) error("CSW transfer failed ($cswN)")
        val c=ByteBuffer.wrap(csw).order(ByteOrder.LITTLE_ENDIAN); if(c.int!=0x53425355) error("Invalid CSW signature"); c.int; c.int; val stat=c.get().toInt() and 255; if(stat!=0) error("SCSI command failed, status=$stat")
        return data.copyOf(got)
    }

    private fun isValidJpeg(data: ByteArray): Boolean {
        if (data.size < 4096 || data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()) return false
        val opts=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(data,0,data.size,opts)
        return opts.outWidth > 0 && opts.outHeight > 0
    }

    private fun saveRecoveredJpeg(data: ByteArray, number: Int): Boolean {
        val name="Recovered_${String.format(Locale.US,"%05d",number)}.jpg"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values=ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME,name)
                    put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES + "/PhotoRescue")
                    put(MediaStore.Images.Media.IS_PENDING,1)
                }
                val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: return false
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(data) } ?: return false
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING,0); contentResolver.update(uri,values,null,null)
                    true
                } catch(e:Exception) { contentResolver.delete(uri,null,null); false }
            } else {
                val dir=File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"PhotoRescue").apply { mkdirs() }
                FileOutputStream(File(dir,name)).use { it.write(data) }
                true
            }
        } catch(_:Exception) { false }
    }

    private fun scanSelectedFolder(uri: Uri) {
        val root=DocumentFile.fromTreeUri(this,uri)
        if(root==null||!root.canRead()){status.text="Could not read that location";return}
        progress.visibility=ProgressBar.VISIBLE; progress.isIndeterminate=true
        thread {
            val s=ScanStats(); walk(root,s)
            runOnUiThread { progress.visibility=ProgressBar.GONE; progress.isIndeterminate=false; status.text="Quick scan complete"; resultText.text="Photos found: ${s.images}\nJPEG: ${s.jpeg}\nPNG: ${s.png}\nTotal visible files: ${s.total}" }
        }
    }

    private fun walk(f: DocumentFile,s: ScanStats) {
        if(f.isDirectory){f.listFiles().forEach{walk(it,s)};return}
        s.total++; val n=f.name?.lowercase(Locale.US).orEmpty()
        when { n.endsWith(".jpg")||n.endsWith(".jpeg")->{s.images++;s.jpeg++}; n.endsWith(".png")->{s.images++;s.png++} }
    }

    private data class ScanStats(var total:Int=0,var images:Int=0,var jpeg:Int=0,var png:Int=0)
}
