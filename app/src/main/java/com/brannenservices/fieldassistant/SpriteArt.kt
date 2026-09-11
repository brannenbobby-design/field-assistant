package com.brannenservices.fieldassistant

import android.graphics.*

object SpriteArt {
 private val paint=Paint().apply{isAntiAlias=false}
 private fun sprite(w:Int,h:Int,draw:(Canvas)->Unit):Bitmap{val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw(Canvas(b));return b}
 private fun r(c:Canvas,l:Int,t:Int,rr:Int,b:Int,color:Int){paint.color=color;c.drawRect(l.toFloat(),t.toFloat(),rr.toFloat(),b.toFloat(),paint)}
 private fun poly(c:Canvas,color:Int,vararg pts:Int){paint.color=color;val p=Path();p.moveTo(pts[0].toFloat(),pts[1].toFloat());var i=2;while(i<pts.size){p.lineTo(pts[i].toFloat(),pts[i+1].toFloat());i+=2};p.close();c.drawPath(p,paint)}
 val man:Bitmap by lazy{sprite(48,72){c->
  val skin=Color.rgb(222,143,86);val shadow=Color.rgb(158,82,55);val hair=Color.rgb(58,34,27)
  r(c,13,18,35,35,skin);r(c,10,20,15,37,hair);r(c,11,13,34,19,Color.rgb(226,215,179));r(c,30,16,43,20,Color.rgb(226,215,179));r(c,15,22,24,27,Color.BLACK);r(c,26,22,35,27,Color.BLACK);r(c,23,23,27,25,Color.DKGRAY);r(c,27,31,36,34,hair);r(c,16,35,34,39,hair)
  r(c,10,38,37,58,Color.rgb(244,239,215));r(c,14,39,33,57,Color.WHITE);r(c,7,41,13,56,skin);r(c,35,41,42,55,skin);r(c,7,52,12,58,shadow);r(c,37,51,43,57,shadow)
  r(c,11,57,37,67,Color.rgb(32,78,143));r(c,14,58,19,63,Color.WHITE);r(c,24,62,29,67,Color.WHITE);r(c,32,58,36,63,Color.rgb(210,42,46));r(c,13,67,20,72,skin);r(c,29,67,36,72,skin)
 }}
 val flamingo:Bitmap by lazy{sprite(48,64){c->
  val pink=Color.rgb(245,70,132);val dark=Color.rgb(207,43,101);r(c,10,34,35,48,pink);r(c,15,37,32,45,dark);r(c,29,19,34,39,pink);r(c,31,10,37,25,pink);r(c,29,7,42,17,pink);r(c,39,10,47,14,Color.rgb(32,29,35));r(c,34,10,37,13,Color.YELLOW);r(c,15,47,18,61,pink);r(c,29,47,32,61,pink);r(c,11,60,19,63,Color.rgb(38,31,37));r(c,28,60,36,63,Color.rgb(38,31,37));poly(c,Color.rgb(255,112,160),8,38,16,29,28,35,20,43)
 }}
 val beer:Bitmap by lazy{sprite(18,30){c->r(c,3,2,15,28,Color.rgb(225,225,210));r(c,3,9,15,22,Color.rgb(31,103,185));r(c,5,0,13,3,Color.rgb(170,170,160));r(c,6,12,12,18,Color.YELLOW)}}
 val cooler:Bitmap by lazy{sprite(38,28){c->r(c,2,7,36,27,Color.rgb(193,39,43));r(c,0,4,38,10,Color.rgb(242,235,210));r(c,5,11,33,14,Color.rgb(226,68,58));r(c,29,14,33,23,Color.rgb(112,27,31))}}
 val exit:Bitmap by lazy{sprite(56,70){c->r(c,25,18,31,70,Color.rgb(78,48,29));r(c,3,3,53,29,Color.rgb(210,147,69));r(c,7,7,49,25,Color.rgb(238,186,91));poly(c,Color.rgb(42,31,29),12,12,37,12,37,8,49,16,37,24,37,20,12,20)}}
}