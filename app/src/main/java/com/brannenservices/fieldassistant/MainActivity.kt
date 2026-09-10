package com.brannenservices.fieldassistant

import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.hardware.usb.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.brannenservices.photorescue.USB_PERMISSION"
        private const val MAX_JPEG_BYTES = 40 * 1024 * 1024
        private const val SECTORS_PER_READ = 16
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
            val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) showUsbDiagnostics(device)
            else status.text = "USB access was not granted"
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
        buildUi(); refreshUsbStatus()
    }

    override fun onResume() { super.onResume(); refreshUsbStatus() }
    override fun onDestroy() { scanCancelled = true; unregisterReceiver(usbPermissionReceiver); super.onDestroy() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(28,42,28,28); setBackgroundColor(0xFF0B0D10.toInt()) }
        fun btn(label:String, action:()->Unit)=Button(this).apply { text=label; setOnClickListener{action()} }
        root.addView(TextView(this).apply{text="PHOTO RESCUE";textSize=30f;setTextColor(0xFFFFFFFF.toInt());gravity=Gravity.CENTER})
        root.addView(TextView(this).apply{text="Personal SD Card Recovery Tool";textSize=16f;setTextColor(0xFF9CA3AF.toInt());gravity=Gravity.CENTER;setPadding(0,8,0,18)})
        status=TextView(this).apply{textSize=18f;setTextColor(0xFFFFFFFF.toInt());gravity=Gravity.CENTER;setPadding(0,8,0,16)}; root.addView(status)
        root.addView(btn("CHECK USB / SD ADAPTER"){refreshUsbStatus()})
        root.addView(btn("RUN USB DIAGNOSTIC"){runUsbDiagnostic()})
        root.addView(btn("TEST RAW READ — READ ONLY"){runRawReadProbe()})
        deepButton=btn("DEEP RECOVERY SCAN — JPEG"){startDeepScan()};root.addView(deepButton)
        cancelButton=btn("CANCEL DEEP SCAN"){scanCancelled=true}.apply{isEnabled=false};root.addView(cancelButton)
        root.addView(btn("QUICK SCAN VISIBLE PHOTOS"){status.text="Choose the SD card or a folder on it";folderPicker.launch(null)})
        progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=1000;visibility=ProgressBar.GONE};root.addView(progress,LinearLayout.LayoutParams(-1,-2))
        resultText=TextView(this).apply{text="V0.5 hardened USB transport. Source SD card remains read-only.";textSize=14f;setTextColor(0xFF9CA3AF.toInt());setPadding(0,14,0,0)};root.addView(resultText)
        setContentView(root)
    }

    private fun refreshUsbStatus(){
        val ds=usbManager.deviceList.values.toList(); if(ds.isEmpty()){status.text="No USB adapter detected";return}
        status.text=if(ds.any{massInterface(it)!=null})"USB storage device detected" else "USB device detected — storage interface not confirmed"
        resultText.text=ds.joinToString("\n\n"){"USB device: ${it.deviceName}\nVID:${it.vendorId} PID:${it.productId}\nMass storage: ${if(massInterface(it)!=null)"YES" else "NO"}\nPermission: ${if(usbManager.hasPermission(it))"granted" else "not granted"}"}
    }

    private fun targetDevice()=usbManager.deviceList.values.firstOrNull{massInterface(it)!=null}
    private fun runUsbDiagnostic(){val d=targetDevice()?:run{status.text="No mass-storage adapter detected";return};if(!usbManager.hasPermission(d)){requestUsbPermission(d);return};showUsbDiagnostics(d)}
    private fun requestUsbPermission(d:UsbDevice){status.text="Requesting USB access…";val pi=PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);usbManager.requestPermission(d,pi)}
    private fun showUsbDiagnostics(d:UsbDevice){val i=massInterface(d);status.text=if(i!=null)"Mass-storage interface confirmed" else "Mass-storage interface not found";resultText.text="Device ${d.deviceName}\nVID ${d.vendorId} PID ${d.productId}\nPermission ${usbManager.hasPermission(d)}\nMass storage: ${if(i!=null)"YES" else "NO"}"}

    private fun massInterface(d:UsbDevice):UsbInterface?=(0 until d.interfaceCount).map{d.getInterface(it)}.firstOrNull{it.interfaceClass==UsbConstants.USB_CLASS_MASS_STORAGE&&it.interfaceProtocol==0x50}
    private fun bulkEndpoints(i:UsbInterface):Pair<UsbEndpoint,UsbEndpoint>?{var input:UsbEndpoint?=null;var output:UsbEndpoint?=null;for(n in 0 until i.endpointCount){val e=i.getEndpoint(n);if(e.type==UsbConstants.USB_ENDPOINT_XFER_BULK){if(e.direction==UsbConstants.USB_DIR_IN)input=e else output=e}};return if(input!=null&&output!=null)Pair(input!!,output!!) else null}

    private data class Capacity(val blocks:Long,val blockSize:Int)
    private fun readCapacity(c:UsbDeviceConnection, i:UsbEndpoint, o:UsbEndpoint, tag:Int):Capacity{val d=scsi(c,i,o,byteArrayOf(0x25,0,0,0,0,0,0,0,0,0),8,tag);if(d.size<8)error("READ CAPACITY returned ${d.size} bytes");val b=ByteBuffer.wrap(d).order(ByteOrder.BIG_ENDIAN);val last=b.int.toLong() and 0xffffffffL;val size=(b.int.toLong() and 0xffffffffL).toInt();return Capacity(last+1,size)}

    private fun runRawReadProbe(){
        val d=targetDevice()?:run{status.text="No compatible USB mass-storage device";return};if(!usbManager.hasPermission(d)){requestUsbPermission(d);return}
        progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=true;status.text="Testing raw read…"
        thread{val r=try{rawProbe(d)}catch(e:Exception){"RAW READ FAILED\n${e.javaClass.simpleName}: ${e.message}"};runOnUiThread{progress.visibility=ProgressBar.GONE;progress.isIndeterminate=false;status.text=if(r.startsWith("RAW READ SUCCESS"))"Raw sector access confirmed" else "Raw read test failed";resultText.text=r}}
    }

    private fun rawProbe(d:UsbDevice):String{
        val intf=massInterface(d)?:error("Mass-storage interface missing");val ep=bulkEndpoints(intf)?:error("Bulk endpoints missing");val c=usbManager.openDevice(d)?:error("Could not open USB device")
        try{if(!c.claimInterface(intf,true))error("Could not claim interface");val cap=readCapacity(c,ep.first,ep.second,1);val s=readBlocks(c,ep.first,ep.second,0,1,cap.blockSize,2);val sig=if(s.size>=512)String.format("%02X%02X",s[510].toInt() and 255,s[511].toInt() and 255)else"n/a";return "RAW READ SUCCESS\nBlocks: ${cap.blocks}\nBlock size: ${cap.blockSize}\nSignature: $sig\nNo write command sent."}finally{try{c.releaseInterface(intf)}catch(_:Exception){};c.close()}
    }

    private fun startDeepScan(){
        if(scanRunning)return;val d=targetDevice()?:run{status.text="No compatible USB mass-storage device";return};if(!usbManager.hasPermission(d)){requestUsbPermission(d);return}
        scanCancelled=false;scanRunning=true;deepButton.isEnabled=false;cancelButton.isEnabled=true;progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=false;progress.progress=0;window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);status.text="Starting deep JPEG recovery…"
        thread{val r=try{deepScan(d)}catch(e:Exception){"DEEP SCAN FAILED\n${e.javaClass.simpleName}: ${e.message}"};runOnUiThread{scanRunning=false;deepButton.isEnabled=true;cancelButton.isEnabled=false;progress.visibility=ProgressBar.GONE;window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);status.text=if(scanCancelled)"Deep scan cancelled" else if(r.startsWith("DEEP SCAN COMPLETE"))"Deep scan complete" else "Deep scan stopped";resultText.text=r}}
    }

    private fun deepScan(d:UsbDevice):String{
        val intf=massInterface(d)?:error("Mass-storage interface missing");val ep=bulkEndpoints(intf)?:error("Bulk endpoints missing");val c=usbManager.openDevice(d)?:error("Could not open USB device")
        var recovered=0;var candidates=0;var rejected=0;var scanned=0L;var tag=100;var jpeg:ByteArrayOutputStream?=null;var previous=-1
        try{
            if(!c.claimInterface(intf,true))error("Could not claim interface");val cap=readCapacity(c,ep.first,ep.second,tag++);var lba=0L
            while(lba<cap.blocks&&!scanCancelled){
                val count=minOf(SECTORS_PER_READ.toLong(),cap.blocks-lba).toInt();val chunk=readBlocks(c,ep.first,ep.second,lba,count,cap.blockSize,tag++);if(chunk.size!=count*cap.blockSize)error("Short read at block $lba")
                for(idx in chunk.indices){val b=chunk[idx].toInt() and 255;val cur=jpeg;if(cur==null){if(previous==0xFF&&b==0xD8&&idx+1<chunk.size&&(chunk[idx+1].toInt() and 255)==0xFF){jpeg=ByteArrayOutputStream(256*1024);jpeg!!.write(0xFF);jpeg!!.write(0xD8);candidates++}}else{cur.write(b);if(cur.size()>MAX_JPEG_BYTES){jpeg=null;rejected++}else if(previous==0xFF&&b==0xD9){val data=cur.toByteArray();if(isValidJpeg(data)&&saveRecoveredJpeg(data,recovered+1))recovered++ else rejected++;jpeg=null}};previous=b}
                lba+=count;scanned+=chunk.size
                if((lba/SECTORS_PER_READ)%256L==0L||lba>=cap.blocks){val p=((lba*1000L)/cap.blocks).toInt().coerceIn(0,1000);val rec=recovered;runOnUiThread{progress.progress=p;status.text="Deep scan ${String.format(Locale.US,"%.1f",p/10.0)}% — $rec JPEGs recovered"}}
                if(tag>2_000_000_000)tag=100
            }
            val gib=scanned/(1024.0*1024.0*1024.0);return if(scanCancelled)"DEEP SCAN CANCELLED\nScanned ${String.format(Locale.US,"%.2f",gib)} GiB\nRecovered: $recovered" else "DEEP SCAN COMPLETE\nScanned ${String.format(Locale.US,"%.2f",gib)} GiB\nJPEG candidates: $candidates\nRecovered JPEGs: $recovered\nRejected/incomplete: $rejected\nSaved to Pictures/PhotoRescue."
        }finally{try{c.releaseInterface(intf)}catch(_:Exception){};c.close()}
    }

    private fun readBlocks(c:UsbDeviceConnection,i:UsbEndpoint,o:UsbEndpoint,lba:Long,count:Int,blockSize:Int,tag:Int):ByteArray{
        val cmd=ByteArray(10);cmd[0]=0x28;cmd[2]=((lba ushr 24) and 255).toByte();cmd[3]=((lba ushr 16) and 255).toByte();cmd[4]=((lba ushr 8) and 255).toByte();cmd[5]=(lba and 255).toByte();cmd[7]=((count ushr 8) and 255).toByte();cmd[8]=(count and 255).toByte();return scsi(c,i,o,cmd,count*blockSize,tag)
    }

    private fun scsi(c:UsbDeviceConnection,i:UsbEndpoint,o:UsbEndpoint,cdb:ByteArray,dataLen:Int,tag:Int):ByteArray{
        val cbw=ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN);cbw.putInt(0x43425355);cbw.putInt(tag);cbw.putInt(dataLen);cbw.put(0x80.toByte());cbw.put(0);cbw.put(cdb.size.toByte());cbw.put(cdb);while(cbw.position()<31)cbw.put(0)
        val sent=c.bulkTransfer(o,cbw.array(),31,5000);if(sent!=31)error("CBW transfer failed ($sent)")
        val data=ByteArray(dataLen);var got=0
        while(got<dataLen){val want=minOf(16384,dataLen-got);val n=c.bulkTransfer(i,data,got,want,10000);if(n<0)error("Data transfer failed ($n) after $got/$dataLen bytes");if(n==0)break;got+=n}
        if(got!=dataLen)error("Short data transfer $got/$dataLen")
        val csw=ByteArray(13);var n=-1;repeat(3){if(n!=13)n=c.bulkTransfer(i,csw,13,3000)};if(n!=13)error("CSW transfer failed ($n)")
        val b=ByteBuffer.wrap(csw).order(ByteOrder.LITTLE_ENDIAN);if(b.int!=0x53425355)error("Invalid CSW signature");val returnedTag=b.int;if(returnedTag!=tag)error("CSW tag mismatch");val residue=b.int;val st=b.get().toInt() and 255;if(st!=0)error("SCSI command failed status=$st residue=$residue");return data
    }

    private fun isValidJpeg(data:ByteArray):Boolean{if(data.size<4096||data[0]!=0xFF.toByte()||data[1]!=0xD8.toByte())return false;val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(data,0,data.size,o);return o.outWidth>0&&o.outHeight>0}

    private fun saveRecoveredJpeg(data:ByteArray,number:Int):Boolean{return try{val name="Recovered_${String.format(Locale.US,"%05d",number)}.jpg";val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PhotoRescue");put(MediaStore.Images.Media.IS_PENDING,1)};val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?:return false;contentResolver.openOutputStream(uri)?.use{it.write(data)}?:return false;v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);contentResolver.update(uri,v,null,null);true}catch(_:Exception){false}}

    private fun scanSelectedFolder(uri:Uri){val root=DocumentFile.fromTreeUri(this,uri);if(root==null||!root.canRead()){status.text="Could not read that location";return};progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=true;thread{val s=ScanStats();walk(root,s);runOnUiThread{progress.visibility=ProgressBar.GONE;progress.isIndeterminate=false;status.text="Quick scan complete";resultText.text="Photos found: ${s.images}\nJPEG: ${s.jpeg}\nPNG: ${s.png}\nTotal visible files: ${s.total}"}}}
    private fun walk(f:DocumentFile,s:ScanStats){if(f.isDirectory){f.listFiles().forEach{walk(it,s)};return};s.total++;val n=f.name?.lowercase(Locale.US).orEmpty();when{n.endsWith(".jpg")||n.endsWith(".jpeg")->{s.images++;s.jpeg++};n.endsWith(".png")->{s.images++;s.png++}}}
    private data class ScanStats(var total:Int=0,var images:Int=0,var jpeg:Int=0,var png:Int=0)
}
