package com.brannenservices.fieldassistant

import android.graphics.*

object SpriteArt {
 private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
 private fun sprite(w:Int,h:Int,draw:(Canvas)->Unit):Bitmap{val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw(Canvas(b));return b}
 private fun r(c:Canvas,l:Int,t:Int,rr:Int,b:Int,color:Int){p.color=color;p.style=Paint.Style.FILL;c.drawRect(l.toFloat(),t.toFloat(),rr.toFloat(),b.toFloat(),p)}
 val man:Bitmap get()=ApprovedRaster.floridaMan
 val flamingo:Bitmap get()=ApprovedRaster.flamingo
 val beer:Bitmap by lazy{sprite(30,44){c->
  r(c,6,3,24,41,Color.rgb(220,220,207));r(c,7,10,23,35,Color.rgb(34,103,181));r(c,8,1,22,5,Color.rgb(166,166,158));r(c,9,14,21,31,Color.rgb(239,193,46));r(c,12,16,18,27,Color.WHITE);r(c,8,6,23,9,Color.rgb(248,246,225));r(c,10,36,21,39,Color.rgb(145,145,139))
 }}
 val cooler:Bitmap by lazy{sprite(58,42){c->
  r(c,4,12,54,40,Color.rgb(174,35,42));r(c,1,6,57,15,Color.rgb(239,235,215));r(c,7,16,51,22,Color.rgb(218,61,55));r(c,9,24,15,37,Color.rgb(137,28,33));r(c,43,23,49,37,Color.rgb(103,27,30));r(c,14,3,45,7,Color.rgb(207,204,189));r(c,18,1,41,4,Color.rgb(111,108,102));r(c,20,20,39,31,Color.rgb(230,226,209));r(c,23,22,36,28,Color.rgb(57,57,58))
 }}
 val exit:Bitmap by lazy{sprite(76,92){c->
  r(c,34,28,42,92,Color.rgb(72,45,28));r(c,3,5,72,39,Color.rgb(155,95,48));r(c,7,9,68,35,Color.rgb(220,163,82));r(c,11,13,64,31,Color.rgb(241,203,116));p.color=Color.rgb(38,29,27);p.typeface=Typeface.DEFAULT_BOLD;p.textSize=13f;p.textAlign=Paint.Align.CENTER;c.drawText("EXIT",38f,27f,p);r(c,31,40,45,45,Color.rgb(111,70,38))
 }}
}