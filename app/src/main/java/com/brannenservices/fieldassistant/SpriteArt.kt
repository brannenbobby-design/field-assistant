package com.brannenservices.fieldassistant

import android.graphics.*

object SpriteArt {
 private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
 private fun sprite(w:Int,h:Int,draw:(Canvas)->Unit):Bitmap{val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);draw(Canvas(b));return b}
 private fun r(c:Canvas,l:Int,t:Int,rr:Int,b:Int,color:Int){p.color=color;p.style=Paint.Style.FILL;c.drawRect(l.toFloat(),t.toFloat(),rr.toFloat(),b.toFloat(),p)}
 private fun poly(c:Canvas,color:Int,vararg q:Int){p.color=color;p.style=Paint.Style.FILL;val z=Path();z.moveTo(q[0].toFloat(),q[1].toFloat());var i=2;while(i<q.size){z.lineTo(q[i].toFloat(),q[i+1].toFloat());i+=2};z.close();c.drawPath(z,p)}
 private fun manFrame(step:Int,attack:Boolean=false,jump:Boolean=false)=sprite(112,136){c->
  val skin=Color.rgb(218,132,78);val hi=Color.rgb(248,179,108);val sh=Color.rgb(132,67,47);val hair=Color.rgb(55,31,22);val beard=Color.rgb(75,40,25);val tank=Color.rgb(241,233,204);val blue=Color.rgb(31,75,145);val red=Color.rgb(200,43,46);val ink=Color.rgb(35,74,64)
  val bob=if(step%2==1)2 else 0; val stride=if(jump)8 else if(step==1)5 else if(step==2)-5 else 0
  poly(c,hair,25,25+bob,34,15+bob,71,17+bob,79,30+bob,75,69+bob,65,75+bob,62,59+bob,29,65+bob,23,51+bob)
  poly(c,skin,34,25+bob,69,25+bob,75,34+bob,72,55+bob,63,65+bob,40,64+bob,31,54+bob,30,35+bob);r(c,29,36+bob,35,51+bob,sh);r(c,70,36+bob,77,51+bob,sh);r(c,36,27+bob,66,32+bob,hi)
  poly(c,Color.rgb(226,219,188),27,17+bob,37,8+bob,65,9+bob,74,19+bob,70,25+bob,29,25+bob);r(c,34,12+bob,47,21+bob,Color.rgb(39,101,143));r(c,47,12+bob,67,21+bob,Color.rgb(235,229,203));r(c,65,20+bob,88,25+bob,Color.rgb(226,219,188));r(c,40,14+bob,45,18+bob,red)
  r(c,35,32+bob,50,41+bob,Color.rgb(10,14,17));r(c,56,32+bob,70,41+bob,Color.rgb(10,14,17));r(c,49,35+bob,57,37+bob,Color.rgb(57,53,48));r(c,37,33+bob,47,35+bob,Color.rgb(76,151,171));r(c,58,33+bob,67,35+bob,Color.rgb(76,151,171));r(c,49,40+bob,55,48+bob,sh)
  r(c,40,49+bob,65,53+bob,beard);r(c,34,53+bob,69,59+bob,beard);r(c,39,59+bob,65,64+bob,hair);r(c,47,52+bob,58,55+bob,hi);r(c,47,58+bob,60,60+bob,Color.rgb(37,25,22))
  poly(c,tank,34,65+bob,43,61+bob,65,61+bob,75,67+bob,77,104+bob,29,104+bob,30,70+bob);r(c,40,64+bob,68,69+bob,Color.rgb(255,248,222));r(c,34,75+bob,38,99+bob,Color.WHITE);r(c,70,75+bob,74,101+bob,Color.rgb(190,183,164));poly(c,sh,44,62+bob,64,62+bob,60,70+bob,48,70+bob)
  r(c,45,76+bob,62,80+bob,Color.rgb(63,51,42));r(c,49,83+bob,59,96+bob,Color.rgb(230,220,191));r(c,51,84+bob,57,94+bob,red);r(c,53,85+bob,56,91+bob,Color.WHITE);r(c,45,98+bob,63,101+bob,Color.rgb(63,51,42))
  if(attack){poly(c,skin,32,68+bob,14,76+bob,7,91+bob,15,99+bob,31,86+bob);poly(c,skin,73,68+bob,91,69+bob,105,76+bob,101,85+bob,79,82+bob)}else{poly(c,skin,31,68+bob,20,72+bob,15,91+bob,22,105+bob,33,99+bob,37,80+bob);poly(c,skin,73,68+bob,86,72+bob,92,90+bob,86,104+bob,75,99+bob,70,80+bob)}
  r(c,21,78+bob,25,84+bob,ink);r(c,18,87+bob,24,91+bob,ink);r(c,82,78+bob,87,83+bob,ink);r(c,86,88+bob,90,94+bob,ink)
  r(c,29,103+bob,78,120+bob,blue);r(c,30,104+bob,47,119+bob,Color.rgb(46,94,170));r(c,31,105+bob,39,111+bob,Color.WHITE);r(c,40,112+bob,47,119+bob,Color.WHITE);r(c,51,104+bob,78,109+bob,red);r(c,51,110+bob,78,115+bob,Color.WHITE);r(c,51,116+bob,78,120+bob,red);r(c,50,103+bob,53,120+bob,Color.rgb(18,49,99))
  val lx=31+stride;val rx=59-stride;r(c,lx,120+bob,lx+13,131+bob,skin);r(c,rx,120+bob,rx+13,131+bob,skin);r(c,lx-2,129+bob,lx+16,135+bob,Color.rgb(32,29,27));r(c,rx-2,129+bob,rx+17,135+bob,Color.rgb(32,29,27));r(c,lx+2,127+bob,lx+11,130+bob,Color.rgb(31,72,101));r(c,rx+2,127+bob,rx+11,130+bob,Color.rgb(31,72,101))
 }
 val manIdle=manFrame(0);val manWalk1=manFrame(1);val manWalk2=manFrame(2);val manAttack=manFrame(0,true);val manJump=manFrame(0,false,true);val man=manIdle
 private fun flamingoFrame(step:Int,attack:Boolean=false)=sprite(104,124){c->
  val pink=Color.rgb(239,70,129);val light=Color.rgb(255,151,181);val mid=Color.rgb(247,101,150);val dark=Color.rgb(191,42,94);val deep=Color.rgb(137,34,76);val leg=Color.rgb(203,67,113);val black=Color.rgb(25,24,30);val bob=if(step==1)2 else 0
  poly(c,dark,9,70+bob,19,57+bob,39,52+bob,63,57+bob,73,68+bob,67,84+bob,49,93+bob,26,92+bob,12,83+bob,5,75+bob);poly(c,pink,14,66+bob,25,56+bob,45,54+bob,65,61+bob,68,74+bob,57,86+bob,35,89+bob,18,81+bob)
  poly(c,light,18,64+bob,33,57+bob,54,60+bob,62,68+bob,49,72+bob,29,71+bob);poly(c,mid,20,71+bob,41,65+bob,60,69+bob,55,79+bob,35,83+bob,18,78+bob);poly(c,dark,30,79+bob,51,73+bob,59,79+bob,47,87+bob,28,85+bob)
  val lean=if(attack)8 else 0;r(c,61+lean,40+bob,72+lean,68+bob,pink);r(c,65+lean,28+bob,76+lean,49+bob,pink);r(c,70+lean,18+bob,81+lean,36+bob,pink);r(c,74+lean,12+bob,87+lean,25+bob,pink);r(c,62+lean,43+bob,66+lean,64+bob,dark);r(c,67+lean,30+bob,71+lean,47+bob,dark);r(c,72+lean,20+bob,76+lean,34+bob,dark)
  poly(c,pink,74+lean,11+bob,81+lean,6+bob,94+lean,8+bob,100+lean,15+bob,95+lean,25+bob,81+lean,26+bob,74+lean,21+bob);poly(c,deep,76+lean,11+bob,88+lean,9+bob,94+lean,13+bob,82+lean,15+bob);r(c,83+lean,13+bob,89+lean,19+bob,Color.rgb(255,237,91));r(c,85+lean,14+bob,89+lean,18+bob,black);poly(c,Color.rgb(238,214,179),94+lean,14+bob,104,16+bob,104,22+bob,95+lean,24+bob,89+lean,20+bob);poly(c,black,100,16+bob,104,16+bob,104,25+bob,95+lean,25+bob,95+lean,21+bob)
  val stride=if(step==1)6 else if(step==2)-5 else 0;r(c,29+stride,88+bob,34+stride,110+bob,leg);r(c,32+stride,106+bob,37+stride,119+bob,leg);r(c,52-stride,86+bob,57-stride,108+bob,leg);r(c,49-stride,104+bob,54-stride,119+bob,leg);r(c,21+stride,117+bob,38+stride,122+bob,black);r(c,46-stride,117+bob,64-stride,122+bob,black)
 }
 val flamingoIdle=flamingoFrame(0);val flamingoWalk1=flamingoFrame(1);val flamingoWalk2=flamingoFrame(2);val flamingoAttack=flamingoFrame(0,true);val flamingo=flamingoIdle
 val beer:Bitmap by lazy{sprite(24,38){c->r(c,5,3,19,35,Color.rgb(229,229,214));r(c,5,11,19,29,Color.rgb(28,100,184));r(c,7,0,17,4,Color.rgb(172,172,161));r(c,8,14,16,24,Color.rgb(244,196,49));r(c,10,16,14,22,Color.WHITE);r(c,6,5,18,8,Color.rgb(248,248,231))}}
 val cooler:Bitmap by lazy{sprite(48,36){c->r(c,3,10,45,34,Color.rgb(181,36,43));r(c,0,5,48,13,Color.rgb(240,235,211));r(c,5,14,43,19,Color.rgb(220,62,56));r(c,7,20,12,31,Color.rgb(145,29,34));r(c,36,19,41,30,Color.rgb(104,28,31));r(c,11,2,37,6,Color.rgb(215,210,190));r(c,14,0,34,3,Color.rgb(120,115,108))}}
 val exit:Bitmap by lazy{sprite(68,82){c->r(c,31,25,38,82,Color.rgb(75,47,29));r(c,3,5,64,34,Color.rgb(202,139,65));r(c,7,9,60,30,Color.rgb(236,185,88));r(c,10,12,56,27,Color.rgb(244,205,117));poly(c,Color.rgb(42,31,29),15,17,43,17,43,12,58,20,43,29,43,24,15,24);r(c,29,35,40,40,Color.rgb(116,74,39))}}
}