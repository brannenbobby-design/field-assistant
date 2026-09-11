package com.brannenservices.fieldassistant

import android.graphics.*

object SpriteArt {
 private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
 private fun sprite(w:Int,h:Int,draw:(Canvas)->Unit):Bitmap{val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw(Canvas(b));return b}
 private fun r(c:Canvas,l:Int,t:Int,rr:Int,b:Int,color:Int){p.color=color;p.style=Paint.Style.FILL;c.drawRect(l.toFloat(),t.toFloat(),rr.toFloat(),b.toFloat(),p)}
 private fun poly(c:Canvas,color:Int,vararg q:Int){p.color=color;p.style=Paint.Style.FILL;val z=Path();z.moveTo(q[0].toFloat(),q[1].toFloat());var i=2;while(i<q.size){z.lineTo(q[i].toFloat(),q[i+1].toFloat());i+=2};z.close();c.drawPath(z,p)}
 private fun dot(c:Canvas,x:Int,y:Int,color:Int){r(c,x,y,x+2,y+2,color)}

 val man:Bitmap by lazy{sprite(96,128){c->
  val skin=Color.rgb(220,137,82);val skinHi=Color.rgb(249,181,111);val skinMid=Color.rgb(198,108,68);val skinSh=Color.rgb(135,70,50);val hair=Color.rgb(50,30,24);val hairHi=Color.rgb(91,50,31);val beard=Color.rgb(70,39,28);val tank=Color.rgb(242,236,211);val tankHi=Color.rgb(255,250,228);val tankSh=Color.rgb(194,187,169);val blue=Color.rgb(28,70,139);val blueHi=Color.rgb(49,96,172);val red=Color.rgb(198,42,48);val ink=Color.rgb(41,67,62)
  // back hair / mullet with broken silhouette
  poly(c,hair,20,24,27,17,62,18,70,27,69,62,62,70,57,55,25,60,20,51)
  r(c,21,32,26,63,hairHi);r(c,65,34,72,66,hair);r(c,24,57,30,75,hair);r(c,61,56,68,78,hairHi)
  // neck shadow
  r(c,37,55,59,69,skinSh);r(c,40,54,57,65,skin)
  // face silhouette + ears
  poly(c,skin,28,24,61,24,67,32,66,51,58,61,36,61,27,53,25,34)
  r(c,23,34,29,49,skinSh);r(c,65,34,72,49,skinSh);r(c,29,26,59,31,skinHi);r(c,30,50,62,58,skinMid)
  // nose / cheek highlights
  r(c,45,37,51,46,skinSh);r(c,48,38,54,44,skinMid);r(c,31,39,35,46,skinHi);r(c,58,39,62,45,skinHi)
  // cap, mesh crown, brim and Florida patch
  poly(c,Color.rgb(226,219,188),23,15,31,8,56,8,65,17,62,23,25,23)
  r(c,30,9,55,13,Color.rgb(239,232,204));r(c,27,14,39,21,Color.rgb(44,108,145));r(c,39,14,59,21,Color.rgb(232,226,198));r(c,56,18,78,23,Color.rgb(226,219,188));r(c,33,12,42,17,Color.rgb(238,235,211));r(c,35,13,40,16,red)
  // sunglasses with reflected sunset
  r(c,29,30,44,38,Color.rgb(9,13,17));r(c,50,30,64,38,Color.rgb(9,13,17));r(c,43,32,51,34,Color.rgb(52,48,46));r(c,31,31,41,33,Color.rgb(78,153,171));r(c,52,31,61,33,Color.rgb(78,153,171));r(c,32,34,40,36,Color.rgb(27,57,66));r(c,53,34,61,36,Color.rgb(27,57,66))
  // eyebrows, beard, moustache, mouth
  r(c,29,27,43,30,hair);r(c,51,27,64,30,hair);r(c,35,45,58,49,beard);r(c,29,49,63,55,beard);r(c,34,55,59,60,hair);r(c,40,47,51,50,Color.rgb(241,158,95));r(c,42,52,54,54,Color.rgb(32,23,22));r(c,44,55,54,57,Color.rgb(160,83,56));dot(c,31,53,hairHi);dot(c,60,51,hairHi)
  // shoulders / muscular arms
  poly(c,skin,25,64,15,68,11,86,16,100,27,96,31,78);poly(c,skin,64,64,76,67,84,83,80,99,68,96,62,77)
  r(c,13,76,19,91,skinHi);r(c,17,91,27,104,skinMid);r(c,76,75,82,91,skinHi);r(c,68,91,79,104,skinMid);r(c,14,98,25,108,skin);r(c,72,98,84,108,skin)
  // arm tattoos
  r(c,18,72,21,77,ink);r(c,15,80,20,83,ink);r(c,20,84,23,89,ink);r(c,73,72,77,76,ink);r(c,77,80,80,85,ink);r(c,71,87,76,90,ink)
  // tank silhouette, folds, neck opening
  poly(c,tank,29,62,37,59,58,59,66,64,68,98,27,98,27,67)
  r(c,34,61,61,66,tankHi);poly(c,skinSh,38,60,57,60,54,67,42,67);r(c,31,70,35,94,tankHi);r(c,61,70,65,96,tankSh);r(c,43,72,48,94,Color.rgb(226,219,196));r(c,51,77,55,91,Color.rgb(205,198,177));dot(c,39,82,Color.rgb(177,168,149));dot(c,57,69,Color.rgb(177,168,149))
  // shirt graphic: HOLD MY BEER / tiny can
  r(c,39,72,57,75,Color.rgb(72,58,49));r(c,43,78,54,90,Color.rgb(235,225,195));r(c,45,79,52,88,red);r(c,47,80,50,86,Color.WHITE);r(c,40,92,57,94,Color.rgb(72,58,49))
  // shorts with flag pattern, seams, belt shadow
  r(c,26,97,68,115,blue);r(c,27,98,67,102,Color.rgb(22,55,110));r(c,27,102,43,114,blueHi);r(c,27,102,35,108,Color.WHITE);r(c,36,109,43,114,Color.WHITE);r(c,45,102,68,107,red);r(c,45,108,68,113,Color.WHITE);r(c,45,113,68,116,red);r(c,46,99,49,115,Color.rgb(19,48,99));r(c,31,100,35,103,Color.rgb(234,225,202));dot(c,30,106,Color.WHITE);dot(c,39,104,Color.WHITE)
  // legs, knees, flip flops
  r(c,29,115,42,125,skin);r(c,53,115,65,125,skin);r(c,30,115,41,119,skinHi);r(c,54,115,64,119,skinHi);r(c,28,123,43,128,Color.rgb(36,31,29));r(c,51,123,68,128,Color.rgb(36,31,29));r(c,31,121,39,124,Color.rgb(32,67,96));r(c,55,121,63,124,Color.rgb(32,67,96));r(c,34,122,37,126,Color.rgb(22,50,70));r(c,58,122,61,126,Color.rgb(22,50,70))
 }}

 val flamingo:Bitmap by lazy{sprite(96,120){c->
  val pink=Color.rgb(238,71,128);val light=Color.rgb(255,147,178);val mid=Color.rgb(247,99,148);val dark=Color.rgb(193,44,96);val deep=Color.rgb(139,36,78);val leg=Color.rgb(202,67,113);val black=Color.rgb(27,25,31)
  // tail + rounded body silhouette
  poly(c,dark,10,69,18,57,35,51,58,55,68,65,63,82,47,91,26,91,12,82,5,74)
  poly(c,pink,14,65,23,55,43,53,61,60,64,73,55,84,34,88,17,80)
  // layered wing with individual feather rows
  poly(c,light,17,64,31,56,51,59,58,67,47,70,28,70);poly(c,mid,20,70,39,64,57,68,53,77,34,81,18,77);poly(c,dark,28,78,48,72,56,78,45,85,27,84)
  r(c,24,61,35,64,Color.rgb(255,184,198));r(c,35,58,47,62,Color.rgb(255,169,190));r(c,31,70,43,73,Color.rgb(224,62,119));r(c,41,76,51,80,deep);dot(c,20,73,Color.rgb(255,177,194));dot(c,50,65,Color.rgb(255,187,200))
  // S neck with shaded inside edge
  r(c,57,39,68,66,pink);r(c,61,27,72,48,pink);r(c,66,17,77,35,pink);r(c,70,12,82,24,pink);r(c,58,42,62,62,dark);r(c,63,28,67,45,dark);r(c,68,18,72,32,dark);r(c,73,13,77,20,light)
  // expressive head, eyebrow and eye
  poly(c,pink,70,10,77,5,89,7,94,14,90,24,77,25,70,20)
  r(c,75,7,85,10,light);poly(c,deep,72,10,84,8,90,12,79,14);r(c,79,12,85,18,Color.rgb(255,236,91));r(c,81,13,85,17,black);r(c,77,9,89,12,deep)
  // hooked two-tone beak
  poly(c,Color.rgb(238,214,179),88,13,96,15,96,21,89,23,84,19);poly(c,black,93,15,96,15,96,23,89,24,89,20);r(c,92,20,96,25,Color.rgb(12,14,18))
  // legs / knees / claws
  r(c,27,86,32,108,leg);r(c,30,104,35,116,leg);r(c,49,84,54,106,leg);r(c,46,102,51,116,leg);r(c,28,100,35,105,dark);r(c,46,99,53,104,dark);r(c,20,114,36,119,black);r(c,43,114,60,119,black);r(c,19,117,25,120,black);r(c,56,117,63,120,black)
  // body outline accents
  r(c,12,75,16,82,deep);r(c,18,84,29,88,deep);r(c,52,80,59,84,deep)
 }}

 val beer:Bitmap by lazy{sprite(24,38){c->r(c,5,3,19,35,Color.rgb(229,229,214));r(c,5,11,19,29,Color.rgb(28,100,184));r(c,7,0,17,4,Color.rgb(172,172,161));r(c,8,14,16,24,Color.rgb(244,196,49));r(c,10,16,14,22,Color.WHITE);r(c,6,5,18,8,Color.rgb(248,248,231))}}
 val cooler:Bitmap by lazy{sprite(48,36){c->r(c,3,10,45,34,Color.rgb(181,36,43));r(c,0,5,48,13,Color.rgb(240,235,211));r(c,5,14,43,19,Color.rgb(220,62,56));r(c,7,20,12,31,Color.rgb(145,29,34));r(c,36,19,41,30,Color.rgb(104,28,31));r(c,11,2,37,6,Color.rgb(215,210,190));r(c,14,0,34,3,Color.rgb(120,115,108))}}
 val exit:Bitmap by lazy{sprite(68,82){c->r(c,31,25,38,82,Color.rgb(75,47,29));r(c,3,5,64,34,Color.rgb(202,139,65));r(c,7,9,60,30,Color.rgb(236,185,88));r(c,10,12,56,27,Color.rgb(244,205,117));poly(c,Color.rgb(42,31,29),15,17,43,17,43,12,58,20,43,29,43,24,15,24);r(c,29,35,40,40,Color.rgb(116,74,39))}}
}