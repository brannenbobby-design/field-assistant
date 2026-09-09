package com.brannenservices.fieldassistant

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    companion object { private const val ACTION_USB_PERMISSION = "com.brannenservices.photorescue.USB_PERMISSION" }
    private lateinit var status: TextView
    private lateinit var resultText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var usbManager: UsbManager

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_USB_PERMISSION) return
            val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) showUsbDiagnostics(device) else status.text = "USB access was not granted"
        }
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scanSelectedFolder(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); usbManager = getSystemService(USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(usbPermissionReceiver, filter)
        buildUi(); refreshUsbStatus()
    }
    override fun onResume() { super.onResume(); refreshUsbStatus() }
    override fun onDestroy() { unregisterReceiver(usbPermissionReceiver); super.onDestroy() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(32,48,32,32); setBackgroundColor(0xFF0B0D10.toInt()) }
        fun button(label:String, action:()->Unit)=Button(this).apply { text=label; setOnClickListener{action()} }
        root.addView(TextView(this).apply{text="PHOTO RESCUE";textSize=30f;setTextColor(0xFFFFFFFF.toInt());gravity=Gravity.CENTER})
        root.addView(TextView(this).apply{text="Personal SD Card Recovery Tool";textSize=16f;setTextColor(0xFF9CA3AF.toInt());gravity=Gravity.CENTER;setPadding(0,8,0,20)})
        status=TextView(this).apply{textSize=18f;setTextColor(0xFFFFFFFF.toInt());gravity=Gravity.CENTER;setPadding(0,8,0,18)}; root.addView(status)
        root.addView(button("CHECK USB / SD ADAPTER"){refreshUsbStatus()})
        root.addView(button("RUN USB DIAGNOSTIC"){runUsbDiagnostic()})
        root.addView(button("TEST RAW READ — READ ONLY"){runRawReadProbe()})
        root.addView(button("QUICK SCAN VISIBLE PHOTOS"){status.text="Choose the SD card or a folder on it";folderPicker.launch(null)})
        progress=ProgressBar(this).apply{visibility=ProgressBar.GONE;setPadding(0,20,0,10)};root.addView(progress)
        resultText=TextView(this).apply{text="V0.3 raw-read test. No USB write commands are implemented.";textSize=14f;setTextColor(0xFF9CA3AF.toInt());setPadding(0,16,0,0)};root.addView(resultText)
        setContentView(root)
    }

    private fun refreshUsbStatus(){
        val d=usbManager.deviceList.values.toList(); if(d.isEmpty()){status.text="No USB adapter detected";return}
        status.text=if(d.any{massInterface(it)!=null})"USB storage device detected" else "USB device detected — storage interface not confirmed"
        resultText.text=d.joinToString("\n\n"){"USB device: ${it.deviceName}\nVID:${it.vendorId} PID:${it.productId}\nMass storage: ${if(massInterface(it)!=null)"YES" else "NO"}\nPermission: ${if(usbManager.hasPermission(it))"granted" else "not granted"}"}
    }

    private fun targetDevice()=usbManager.deviceList.values.firstOrNull{massInterface(it)!=null}
    private fun runUsbDiagnostic(){
        val d=targetDevice()?:run{status.text="No mass-storage adapter detected";return}
        if(!usbManager.hasPermission(d)){requestUsbPermission(d);return};showUsbDiagnostics(d)
    }
    private fun requestUsbPermission(d:UsbDevice){status.text="Requesting USB access…";val pi=PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);usbManager.requestPermission(d,pi)}
    private fun showUsbDiagnostics(d:UsbDevice){
        val i=massInterface(d);status.text=if(i!=null)"Mass-storage interface confirmed" else "Mass-storage interface not found"
        resultText.text=buildString{append("Device ${d.deviceName}\nVID ${d.vendorId} PID ${d.productId}\nPermission ${usbManager.hasPermission(d)}\n");if(i!=null){append("Interface class=${i.interfaceClass} subclass=${i.interfaceSubclass} protocol=${i.interfaceProtocol}\n");for(n in 0 until i.endpointCount){val e=i.getEndpoint(n);append("EP$n ${if(e.direction==UsbConstants.USB_DIR_IN)"IN" else "OUT"} type=${e.type} maxPacket=${e.maxPacketSize}\n")}};append("\nDiagnostic only — no data written.")}
    }

    private fun massInterface(d:UsbDevice):UsbInterface?=(0 until d.interfaceCount).map{d.getInterface(it)}.firstOrNull{it.interfaceClass==UsbConstants.USB_CLASS_MASS_STORAGE && it.interfaceProtocol==0x50}
    private fun bulkEndpoints(i:UsbInterface):Pair<UsbEndpoint,UsbEndpoint>?{
        var input:UsbEndpoint?=null;var output:UsbEndpoint?=null
        for(n in 0 until i.endpointCount){val e=i.getEndpoint(n);if(e.type==UsbConstants.USB_ENDPOINT_XFER_BULK){if(e.direction==UsbConstants.USB_DIR_IN)input=e else output=e}}
        return if(input!=null&&output!=null) Pair(input!!,output!!) else null
    }

    private fun runRawReadProbe(){
        val d=targetDevice()?:run{status.text="No compatible USB mass-storage device";return}
        if(!usbManager.hasPermission(d)){requestUsbPermission(d);return}
        progress.visibility=ProgressBar.VISIBLE;status.text="Testing raw read…"
        thread {
            val report=try{rawProbe(d)}catch(e:Exception){"RAW READ FAILED\n${e.javaClass.simpleName}: ${e.message}"}
            runOnUiThread{progress.visibility=ProgressBar.GONE;status.text=if(report.startsWith("RAW READ SUCCESS"))"Raw sector access confirmed" else "Raw read test failed";resultText.text=report}
        }
    }

    private fun rawProbe(d:UsbDevice):String{
        val intf=massInterface(d)?:error("Bulk-only mass-storage interface missing")
        val eps=bulkEndpoints(intf)?:error("Bulk IN/OUT endpoints missing")
        val conn=usbManager.openDevice(d)?:error("Could not open USB device")
        try{
            if(!conn.claimInterface(intf,true)) error("Could not claim mass-storage interface")
            try{ conn.controlTransfer(0x21,0xFF,0,intf.id,null,0,1000) }catch(_:Exception){}
            val capacity=scsi(conn,eps.first,eps.second,byteArrayOf(0x25,0,0,0,0,0,0,0,0,0),8,1)
            if(capacity.size<8) error("READ CAPACITY returned ${capacity.size} bytes")
            val bb=ByteBuffer.wrap(capacity).order(ByteOrder.BIG_ENDIAN)
            val lastLba=bb.int.toLong() and 0xffffffffL; val blockSize=bb.int.toLong() and 0xffffffffL
            if(blockSize<=0 || blockSize>65536) error("Invalid block size $blockSize")
            val read10=byteArrayOf(0x28,0,0,0,0,0,0,0,1,0)
            val sector=scsi(conn,eps.first,eps.second,read10,blockSize.toInt(),2)
            if(sector.size<blockSize.toInt()) error("READ(10) returned ${sector.size} bytes")
            val sig=if(sector.size>=512) String.format("%02X%02X",sector[510].toInt() and 255,sector[511].toInt() and 255) else "n/a"
            val gib=((lastLba+1)*blockSize)/(1024.0*1024.0*1024.0)
            return "RAW READ SUCCESS\n\nBlocks: ${lastLba+1}\nBlock size: $blockSize bytes\nCapacity: ${String.format(Locale.US,"%.2f",gib)} GiB\nSector 0 bytes read: ${sector.size}\nBoot/MBR signature @510: $sig\n\nPhoto Rescue successfully read raw block 0 directly through USB Mass Storage. No write command was sent to the SD card."
        }finally{try{conn.releaseInterface(intf)}catch(_:Exception){};conn.close()}
    }

    private fun scsi(conn:UsbDeviceConnection,input:UsbEndpoint,output:UsbEndpoint,cdb:ByteArray,dataLen:Int,tag:Int):ByteArray{
        val cbw=ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN)
        cbw.putInt(0x43425355);cbw.putInt(tag);cbw.putInt(dataLen);cbw.put(0x80.toByte());cbw.put(0);cbw.put(cdb.size.toByte());cbw.put(cdb);while(cbw.position()<31)cbw.put(0)
        val out=conn.bulkTransfer(output,cbw.array(),31,5000);if(out!=31)error("CBW transfer failed ($out)")
        val data=ByteArray(dataLen);var got=0
        while(got<dataLen){val temp=ByteArray(dataLen-got);val n=conn.bulkTransfer(input,temp,temp.size,5000);if(n<=0)break;System.arraycopy(temp,0,data,got,n);got+=n}
        val csw=ByteArray(13);val cswN=conn.bulkTransfer(input,csw,13,5000);if(cswN!=13)error("CSW transfer failed ($cswN)")
        val c=ByteBuffer.wrap(csw).order(ByteOrder.LITTLE_ENDIAN);if(c.int!=0x53425355)error("Invalid CSW signature");c.int;c.int;val stat=c.get().toInt() and 255;if(stat!=0)error("SCSI command failed, status=$stat")
        return data.copyOf(got)
    }

    private fun scanSelectedFolder(uri:Uri){val root=DocumentFile.fromTreeUri(this,uri);if(root==null||!root.canRead()){status.text="Could not read that location";return};progress.visibility=ProgressBar.VISIBLE;thread{val s=ScanStats();walk(root,s);runOnUiThread{progress.visibility=ProgressBar.GONE;status.text="Quick scan complete";resultText.text="Photos found: ${s.images}\nJPEG: ${s.jpeg}\nPNG: ${s.png}\nTotal visible files: ${s.total}"}}}
    private fun walk(f:DocumentFile,s:ScanStats){if(f.isDirectory){f.listFiles().forEach{walk(it,s)};return};s.total++;val n=f.name?.lowercase(Locale.US).orEmpty();when{n.endsWith(".jpg")||n.endsWith(".jpeg")->{s.images++;s.jpeg++};n.endsWith(".png")->{s.images++;s.png++}}}
    private data class ScanStats(var total:Int=0,var images:Int=0,var jpeg:Int=0,var png:Int=0)
}
