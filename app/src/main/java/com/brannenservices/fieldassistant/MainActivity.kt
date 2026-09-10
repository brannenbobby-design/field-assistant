package com.brannenservices.fieldassistant

import android.app.PendingIntent
import android.content.*
import android.content.ContentValues
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
 companion object { const val ACTION_USB_PERMISSION="com.brannenservices.photorescue.USB_PERMISSION"; const val MAX_JPEG_BYTES=40*1024*1024; val READ_STEPS=intArrayOf(128,64,32,16,8,1) }
 private lateinit var status:TextView; private lateinit var resultText:TextView; private lateinit var progress:ProgressBar; private lateinit var usbManager:UsbManager; private lateinit var deepButton:Button; private lateinit var cancelButton:Button
 @Volatile private var scanCancelled=false; @Volatile private var scanRunning=false; private val tags=AtomicInteger(1000)
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,x:Intent?){if(x?.action!=ACTION_USB_PERMISSION)return;val d=if(Build.VERSION.SDK_INT>=33)x.getParcelableExtra(UsbManager.EXTRA_DEVICE,UsbDevice::class.java)else @Suppress("DEPRECATION") x.getParcelableExtra(UsbManager.EXTRA_DEVICE);if(d!=null&&x.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false))showUsb(d)else status.text="USB access was not granted"}}
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){u->if(u!=null){contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);quickScan(u)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);usbManager=getSystemService(USB_SERVICE) as UsbManager;val f=IntentFilter(ACTION_USB_PERMISSION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED)else @Suppress("DEPRECATION") registerReceiver(receiver,f);buildUi();refresh()}
 override fun onResume(){super.onResume();refresh()}
 override fun onDestroy(){scanCancelled=true;try{unregisterReceiver(receiver)}catch(_:Exception){};super.onDestroy()}
 private fun buildUi(){
  val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,42,28,28);setBackgroundColor(0xFF0B0D10.toInt())}
  fun button(t:String,a:()->Unit):Button=Button(this).apply{text=t;setOnClickListener{a()}}
  r.addView(TextView(this).apply{text="PHOTO RESCUE";textSize=30f;setTextColor(-1);gravity=Gravity.CENTER})
  r.addView(TextView(this).apply{text="Personal SD Card Recovery Tool";textSize=16f;setTextColor(0xFF9CA3AF.toInt());gravity=Gravity.CENTER})
  status=TextView(this).apply{textSize=18f;setTextColor(-1);gravity=Gravity.CENTER;setPadding(0,18,0,16)};r.addView(status)
  r.addView(button("CHECK USB / SD ADAPTER",{refresh()}))
  r.addView(button("RUN USB DIAGNOSTIC",{diagnostic()}))
  r.addView(button("SAFE USB CLAIM TEST — READ ONLY",{safeClaimTest()}))
  r.addView(button("STABILIZED RAW PREFLIGHT — READ ONLY",{rawTest()}))
  deepButton=button("DEEP RECOVERY SCAN — JPEG",{startScan()});r.addView(deepButton)
  cancelButton=button("CANCEL DEEP SCAN",{scanCancelled=true}).apply{isEnabled=false};r.addView(cancelButton)
  r.addView(button("QUICK SCAN VISIBLE PHOTOS",{picker.launch(null)}))
  progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=1000;visibility=ProgressBar.GONE};r.addView(progress,LinearLayout.LayoutParams(-1,-2))
  resultText=TextView(this).apply{text="V0.8 stabilized USB transport. Source SD card remains read-only.";textSize=14f;setTextColor(0xFF9CA3AF.toInt());setPadding(0,14,0,0)};r.addView(resultText)
  setContentView(r)
 }
 private fun mass(d:UsbDevice):UsbInterface?=(0 until d.interfaceCount).map{d.getInterface(it)}.firstOrNull{it.interfaceClass==UsbConstants.USB_CLASS_MASS_STORAGE&&it.interfaceProtocol==0x50}
 private fun eps(i:UsbInterface):Pair<UsbEndpoint,UsbEndpoint>?{var input:UsbEndpoint?=null;var output:UsbEndpoint?=null;for(n in 0 until i.endpointCount){val e=i.getEndpoint(n);if(e.type==UsbConstants.USB_ENDPOINT_XFER_BULK){if(e.direction==UsbConstants.USB_DIR_IN)input=e else output=e}};return if(input!=null&&output!=null)Pair(input!!,output!!)else null}
 private fun target()=usbManager.deviceList.values.firstOrNull{mass(it)!=null}
 private fun refresh(){val d=target();status.text=if(d==null)"No USB storage device detected"else"USB storage device detected";if(d!=null)showUsb(d)}
 private fun request(d:UsbDevice){val p=PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);usbManager.requestPermission(d,p)}
 private fun diagnostic(){val d=target()?:run{status.text="No mass-storage adapter detected";return};if(!usbManager.hasPermission(d)){request(d);return};showUsb(d)}
 private fun showUsb(d:UsbDevice){status.text="Mass-storage interface confirmed";resultText.text="Device ${d.deviceName}\nVID ${d.vendorId} PID ${d.productId}\nPermission ${usbManager.hasPermission(d)}\nMass storage: YES"}
 data class Cap(val blocks:Long,val size:Int)
 private fun nextTag()=tags.incrementAndGet()
 private fun capacity(c:UsbDeviceConnection,intf:UsbInterface,i:UsbEndpoint,o:UsbEndpoint):Cap{val d=scsi(c,intf,i,o,byteArrayOf(0x25,0,0,0,0,0,0,0,0,0),8);val b=ByteBuffer.wrap(d).order(ByteOrder.BIG_ENDIAN);return Cap((b.int.toLong() and 0xffffffffL)+1,(b.int.toLong() and 0xffffffffL).toInt())}
 private fun safeClaimTest(){val d=target()?:return;if(!usbManager.hasPermission(d)){request(d);return};progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=true;thread{val s=try{safeProbe(d)}catch(e:Exception){"SAFE CLAIM TEST FAILED\n${e.javaClass.simpleName}: ${e.message}"};runOnUiThread{progress.visibility=ProgressBar.GONE;progress.isIndeterminate=false;status.text=if(s.startsWith("SAFE CLAIM SUCCESS"))"Safe USB claim succeeded"else"Safe USB claim did not succeed";resultText.text=s}}}
 private fun safeProbe(d:UsbDevice):String{val intf=mass(d)?:error("No interface");val e=eps(intf)?:error("No endpoints");val c=usbManager.openDevice(d)?:error("Open failed");var claimed=false;try{claimed=c.claimInterface(intf,false);if(!claimed)return "SAFE CLAIM REFUSED\nAndroid/system storage currently owns the mass-storage interface.\nNo forced disconnect was attempted.\nNo read or write command was sent.";val cap=capacity(c,intf,e.first,e.second);val s=readExact(c,intf,e.first,e.second,0,1,cap.size);return "SAFE CLAIM SUCCESS\nBlocks: ${cap.blocks}\nBlock size: ${cap.size}\nSignature: ${sig(s)}\nInterface claimed without force.\nNo write command sent."}finally{if(claimed)try{c.releaseInterface(intf)}catch(_:Exception){};c.close()}}
 private fun rawTest(){val d=target()?:return;if(!usbManager.hasPermission(d)){request(d);return};progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=true;thread{val s=try{preflight(d)}catch(e:Exception){"PREFLIGHT FAILED\n${e.javaClass.simpleName}: ${e.message}"};runOnUiThread{progress.visibility=ProgressBar.GONE;progress.isIndeterminate=false;status.text=if(s.startsWith("PREFLIGHT SUCCESS"))"Raw transport preflight passed"else"Raw preflight stopped";resultText.text=s}}}
 private fun preflight(d:UsbDevice):String{val intf=mass(d)?:error("No interface");val e=eps(intf)?:error("No endpoints");val c=usbManager.openDevice(d)?:error("Open failed");var claimed=false;try{claimed=c.claimInterface(intf,true);if(!claimed)error("Claim failed");SystemClock.sleep(250);val cap=capacity(c,intf,e.first,e.second);var stable=0;var sample=ByteArray(0);for(step in READ_STEPS){try{sample=readExact(c,intf,e.first,e.second,0,step.coerceAtMost(cap.blocks.toInt()),cap.size);stable=step;break}catch(_:Exception){botReset(c,intf,e.first,e.second);SystemClock.sleep(150)}};if(stable==0)error("No stable read size");return "PREFLIGHT SUCCESS\nBlocks: ${cap.blocks}\nBlock size: ${cap.size}\nStable read: $stable sectors (${stable*cap.size/1024} KiB)\nSignature: ${sig(sample)}\nRead-only commands only.\nInterface released after test."}finally{if(claimed)try{c.releaseInterface(intf)}catch(_:Exception){};c.close()}}
 private fun startScan(){if(scanRunning)return;val d=target()?:return;if(!usbManager.hasPermission(d)){request(d);return};scanCancelled=false;scanRunning=true;deepButton.isEnabled=false;cancelButton.isEnabled=true;progress.visibility=ProgressBar.VISIBLE;progress.progress=0;window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);thread{val s=try{scan(d)}catch(e:Exception){"DEEP SCAN FAILED\n${e.javaClass.simpleName}: ${e.message}"};runOnUiThread{scanRunning=false;deepButton.isEnabled=true;cancelButton.isEnabled=false;progress.visibility=ProgressBar.GONE;window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);status.text=if(scanCancelled)"Deep scan cancelled"else if(s.startsWith("DEEP SCAN COMPLETE"))"Deep scan complete"else"Deep scan stopped";resultText.text=s}}}
 private fun scan(d:UsbDevice):String{val intf=mass(d)?:error("No interface");val e=eps(intf)?:error("No endpoints");val c=usbManager.openDevice(d)?:error("Open failed");var claimed=false;var rec=0;var cand=0;var rej=0;var bytes=0L;var jpeg:ByteArrayOutputStream?=null;var prev=-1;try{claimed=c.claimInterface(intf,true);if(!claimed)error("Claim failed");SystemClock.sleep(250);val cap=capacity(c,intf,e.first,e.second);var readStep=READ_STEPS.first();var lba=0L;while(lba<cap.blocks&&!scanCancelled){var x:ByteArray?=null;var used=readStep;var idx=READ_STEPS.indexOf(readStep).coerceAtLeast(0);while(x==null&&idx<READ_STEPS.size){used=minOf(READ_STEPS[idx].toLong(),cap.blocks-lba).toInt();try{x=readExact(c,intf,e.first,e.second,lba,used,cap.size);readStep=READ_STEPS[idx]}catch(ex:Exception){botReset(c,intf,e.first,e.second);SystemClock.sleep(100);idx++}};val data=x?:error("Unable to read sector $lba even at 1-sector mode");for(k in data.indices){val v=data[k].toInt() and 255;val cur=jpeg;if(cur==null){if(prev==0xFF&&v==0xD8&&k+1<data.size&&(data[k+1].toInt() and 255)==0xFF){jpeg=ByteArrayOutputStream(256*1024);jpeg!!.write(0xFF);jpeg!!.write(0xD8);cand++}}else{if(!(cur.size()==2&&v==0xD8))cur.write(v);if(cur.size()>MAX_JPEG_BYTES){jpeg=null;rej++}else if(prev==0xFF&&v==0xD9){val a=cur.toByteArray();if(valid(a)&&save(a,rec+1))rec++ else rej++;jpeg=null}};prev=v};lba+=used;bytes+=data.size;val p=((lba*1000)/cap.blocks).toInt();if(lba%8192L<used||lba>=cap.blocks){val rr=rec;val rs=readStep;runOnUiThread{progress.progress=p;status.text="Deep scan ${String.format(Locale.US,"%.1f",p/10.0)}% — $rr JPEGs — ${rs*cap.size/1024} KiB reads"}}};val g=bytes/(1024.0*1024*1024);return if(scanCancelled)"DEEP SCAN CANCELLED\nScanned ${String.format(Locale.US,"%.2f",g)} GiB\nRecovered: $rec"else"DEEP SCAN COMPLETE\nScanned ${String.format(Locale.US,"%.2f",g)} GiB\nJPEG candidates: $cand\nRecovered JPEGs: $rec\nRejected/incomplete: $rej\nSaved to Pictures/PhotoRescue."}finally{if(claimed)try{c.releaseInterface(intf)}catch(_:Exception){};c.close()}}
 private fun sig(a:ByteArray)=if(a.size>=512)String.format("%02X%02X",a[510].toInt() and 255,a[511].toInt() and 255)else"n/a"
 private fun readExact(c:UsbDeviceConnection,intf:UsbInterface,i:UsbEndpoint,o:UsbEndpoint,lba:Long,n:Int,bs:Int):ByteArray{val q=ByteArray(10);q[0]=0x28;q[2]=((lba ushr 24) and 255).toByte();q[3]=((lba ushr 16) and 255).toByte();q[4]=((lba ushr 8) and 255).toByte();q[5]=(lba and 255).toByte();q[7]=((n ushr 8) and 255).toByte();q[8]=(n and 255).toByte();return scsi(c,intf,i,o,q,n*bs)}
 private fun scsi(c:UsbDeviceConnection,intf:UsbInterface,i:UsbEndpoint,o:UsbEndpoint,cdb:ByteArray,len:Int):ByteArray{var last:Exception?=null;repeat(2){try{return scsiOnce(c,i,o,cdb,len,nextTag())}catch(e:Exception){last=e;botReset(c,intf,i,o);SystemClock.sleep(100)}};throw last?:IllegalStateException("USB command failed")}
 private fun bulkOutExact(c:UsbDeviceConnection,e:UsbEndpoint,a:ByteArray){var off=0;while(off<a.size){val n=c.bulkTransfer(e,a,off,a.size-off,5000);if(n<=0)error("USB OUT failed at $off/${a.size}");off+=n}}
 private fun bulkInExact(c:UsbDeviceConnection,e:UsbEndpoint,a:ByteArray,off0:Int,len:Int,timeout:Int){var off=off0;val end=off0+len;while(off<end){val n=c.bulkTransfer(e,a,off,end-off,timeout);if(n<=0)error("USB IN failed at ${off-off0}/$len");off+=n}}
 private fun scsiOnce(c:UsbDeviceConnection,i:UsbEndpoint,o:UsbEndpoint,cdb:ByteArray,len:Int,tag:Int):ByteArray{val w=ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN);w.putInt(0x43425355);w.putInt(tag);w.putInt(len);w.put(0x80.toByte());w.put(0);w.put(cdb.size.toByte());w.put(cdb);while(w.position()<31)w.put(0);bulkOutExact(c,o,w.array());val d=ByteArray(len);var got=0;while(got<len){val chunk=minOf(16384,len-got);bulkInExact(c,i,d,got,chunk,10000);got+=chunk};val s=ByteArray(13);bulkInExact(c,i,s,0,13,5000);val b=ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN);if(b.int!=0x53425355)error("Bad CSW signature");if(b.int!=tag)error("CSW tag mismatch");val residue=b.int;val st=b.get().toInt() and 255;if(st!=0||residue!=0)error("SCSI status=$st residue=$residue");return d}
 private fun botReset(c:UsbDeviceConnection,intf:UsbInterface,i:UsbEndpoint,o:UsbEndpoint){c.controlTransfer(0x21,0xFF,0,intf.id,null,0,1000);c.controlTransfer(0x02,1,0,i.address,null,0,1000);c.controlTransfer(0x02,1,0,o.address,null,0,1000)}
 private fun valid(a:ByteArray):Boolean{if(a.size<4096||a[0]!=0xFF.toByte()||a[1]!=0xD8.toByte())return false;val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(a,0,a.size,o);return o.outWidth>0&&o.outHeight>0}
 private fun save(a:ByteArray,n:Int):Boolean{var u:Uri?=null;return try{val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"Recovered_${String.format(Locale.US,"%05d",n)}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PhotoRescue");put(MediaStore.Images.Media.IS_PENDING,1)};u=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?:return false;contentResolver.openOutputStream(u)?.use{it.write(a)}?:return false;v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);contentResolver.update(u,v,null,null);true}catch(_:Exception){u?.let{try{contentResolver.delete(it,null,null)}catch(_:Exception){}};false}}
 private fun quickScan(u:Uri){val root=DocumentFile.fromTreeUri(this,u)?:return;progress.visibility=ProgressBar.VISIBLE;progress.isIndeterminate=true;thread{val s=Stats();walk(root,s);runOnUiThread{progress.visibility=ProgressBar.GONE;progress.isIndeterminate=false;status.text="Quick scan complete";resultText.text="Photos found: ${s.img}\nJPEG: ${s.jpg}\nPNG: ${s.png}\nTotal visible files: ${s.all}"}}}
 private fun walk(f:DocumentFile,s:Stats){if(f.isDirectory){f.listFiles().forEach{walk(it,s)};return};s.all++;val n=f.name?.lowercase(Locale.US).orEmpty();if(n.endsWith(".jpg")||n.endsWith(".jpeg")){s.img++;s.jpg++}else if(n.endsWith(".png")){s.img++;s.png++}}
 data class Stats(var all:Int=0,var img:Int=0,var jpg:Int=0,var png:Int=0)
}
