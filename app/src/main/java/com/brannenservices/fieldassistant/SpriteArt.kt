package com.brannenservices.fieldassistant

import android.graphics.*

object SpriteArt {
 private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
 private fun sprite(w:Int,h:Int,draw:(Canvas)->Unit):Bitmap{val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw(Canvas(b));return b}
 private fun r(c:Canvas,l:Int,t:Int,rr:Int,b:Int,color:Int){p.color=color;p.style=Paint.Style.FILL;c.drawRect(l.toFloat(),t.toFloat(),rr.toFloat(),b.toFloat(),p)}
 private fun poly(c:Canvas,color:Int,vararg q:Int){p.color=color;p.style=Paint.Style.FILL;val z=Path();z.moveTo(q[0].toFloat(),q[1].toFloat());var i=2;while(i<q.size){z.lineTo(q[i].toFloat(),q[i+1].toFloat());i+=2};z.close();c.drawPath(z,p)}
 private fun line(c:Canvas,color:Int,w:Float,x1:Int,y1:Int,x2:Int,y2:Int){p.color=color;p.strokeWidth=w;p.strokeCap=Paint.Cap.SQUARE;c.drawLine(x1.toFloat(),y1.toFloat(),x2.toFloat(),y2.toFloat(),p)}

 val man:Bitmap by lazy{sprite(72,104){c->
  val skin=Color.rgb(224,146,91);val hi=Color.rgb(244,174,112);val sh=Color.rgb(151,79,55);val hair=Color.rgb(58,34,27);val beard=Color.rgb(72,40,29);val tank=Color.rgb(244,239,216);val blue=Color.rgb(31,78,145);val red=Color.rgb(205,45,49)
  // mullet behind head
  r(c,16,20,23,51,hair);r(c,19,42,25,59,hair);r(c,48,25,55,55,hair);r(c,51,42,58,61,hair);r(c,18,52,23,65,hair)
  // head, ears and sunburn shading
  r(c,21,22,51,50,skin);r(c,18,30,23,43,sh);r(c,50,30,56,43,sh);r(c,24,24,48,29,hi);r(c,23,45,50,51,sh)
  // trucker cap
  r(c,18,13,48,21,Color.rgb(226,218,184));r(c,23,10,45,14,Color.rgb(235,229,201));r(c,43,17,63,21,Color.rgb(226,218,184));r(c,20,14,26,19,red)
  // sunglasses
  r(c,22,28,34,35,Color.rgb(12,16,20));r(c,38,28,50,35,Color.rgb(12,16,20));r(c,33,30,39,32,Color.rgb(71,77,78));r(c,24,29,31,30,Color.rgb(91,158,175));r(c,40,29,47,30,Color.rgb(91,158,175))
  // nose / moustache / scruffy beard
  r(c,34,34,39,39,sh);r(c,31,40,47,44,beard);r(c,26,44,51,48,beard);r(c,29,48,48,52,hair);r(c,35,43,41,45,Color.rgb(235,155,95))
  // tank top with neck and dirty shadow
  r(c,21,52,52,79,tank);r(c,27,51,46,57,skin);r(c,28,56,45,60,Color.rgb(250,247,229));r(c,24,61,28,76,Color.WHITE);r(c,47,61,51,77,Color.rgb(211,204,184));r(c,32,62,39,68,Color.rgb(213,205,182))
  // arms, forearms, hands
  r(c,13,56,23,77,skin);r(c,10,70,20,83,sh);r(c,51,56,61,75,skin);r(c,56,69,65,81,sh);r(c,9,78,18,86,skin);r(c,58,76,67,84,skin)
  // flag shorts
  r(c,18,78,55,94,blue);r(c,18,78,26,86,red);r(c,27,79,34,85,Color.WHITE);r(c,40,86,49,93,Color.WHITE);r(c,51,79,55,94,Color.rgb(23,61,119));r(c,21,87,25,91,Color.WHITE)
  // legs + flip flops
  r(c,21,94,31,102,skin);r(c,43,94,53,102,skin);r(c,18,100,33,104,Color.rgb(37,31,29));r(c,40,100,57,104,Color.rgb(37,31,29));r(c,23,98,29,100,Color.rgb(50,44,39));r(c,45,98,51,100,Color.rgb(50,44,39))
  // tattoos / chest detail
  r(c,16,61,19,65,Color.rgb(56,93,74));r(c,57,59,60,64,Color.rgb(56,93,74));r(c,34,67,37,70,Color.rgb(170,163,148))
 }}

 val flamingo:Bitmap by lazy{sprite(72,92){c->
  val pink=Color.rgb(244,66,130);val hot=Color.rgb(255,103,157);val dark=Color.rgb(199,39,96);val shadow=Color.rgb(158,34,82);val leg=Color.rgb(207,61,111)
  // tail and body feather silhouette
  poly(c,dark,10,51,18,43,31,42,45,47,53,56,47,67,28,72,14,65,5,58)
  r(c,15,49,48,66,pink);r(c,20,45,41,69,pink);r(c,11,54,20,63,hot)
  // layered wing feathers
  poly(c,hot,17,50,29,45,43,50,38,57,24,60);poly(c,dark,20,57,39,53,46,59,35,65,21,64);r(c,26,51,39,55,Color.rgb(255,133,173))
  // curved neck made in chunky pixel steps
  r(c,42,30,50,57,pink);r(c,45,22,53,40,pink);r(c,49,15,57,29,pink);r(c,52,10,61,20,pink)
  // head and angry brow
  r(c,51,6,65,19,pink);r(c,55,4,63,8,hot);r(c,54,8,59,12,Color.YELLOW);r(c,55,8,59,10,Color.rgb(255,238,67));r(c,53,6,61,8,dark);r(c,59,9,66,14,Color.rgb(239,221,183));r(c,64,10,72,15,Color.rgb(29,28,34));r(c,67,12,72,16,Color.rgb(12,15,19))
  // legs with knees and feet
  r(c,20,65,24,84,leg);r(c,22,80,26,88,leg);r(c,39,65,43,83,leg);r(c,37,80,41,88,leg);r(c,15,87,27,91,Color.rgb(43,31,39));r(c,35,87,48,91,Color.rgb(43,31,39));r(c,18,84,24,87,shadow);r(c,39,83,45,87,shadow)
  // feather pixels
  r(c,13,57,17,60,Color.rgb(255,153,187));r(c,30,47,34,50,Color.rgb(255,153,187));r(c,37,61,41,64,shadow)
 }}

 val beer:Bitmap by lazy{sprite(24,38){c->r(c,5,3,19,35,Color.rgb(229,229,214));r(c,5,11,19,29,Color.rgb(28,100,184));r(c,7,0,17,4,Color.rgb(172,172,161));r(c,8,14,16,24,Color.rgb(244,196,49));r(c,10,16,14,22,Color.WHITE);r(c,6,5,18,8,Color.rgb(248,248,231))}}
 val cooler:Bitmap by lazy{sprite(48,36){c->r(c,3,10,45,34,Color.rgb(181,36,43));r(c,0,5,48,13,Color.rgb(240,235,211));r(c,5,14,43,19,Color.rgb(220,62,56));r(c,7,20,12,31,Color.rgb(145,29,34));r(c,36,19,41,30,Color.rgb(104,28,31));r(c,11,2,37,6,Color.rgb(215,210,190));r(c,14,0,34,3,Color.rgb(120,115,108))}}
 val exit:Bitmap by lazy{sprite(68,82){c->r(c,31,25,38,82,Color.rgb(75,47,29));r(c,3,5,64,34,Color.rgb(202,139,65));r(c,7,9,60,30,Color.rgb(236,185,88));r(c,10,12,56,27,Color.rgb(244,205,117));poly(c,Color.rgb(42,31,29),15,17,43,17,43,12,58,20,43,29,43,24,15,24);r(c,29,35,40,40,Color.rgb(116,74,39))}}
}