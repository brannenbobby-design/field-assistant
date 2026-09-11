package com.brannenservices.fieldassistant

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.*
import kotlin.math.*

class MainActivity : Activity() {
    data class Flamingo(var x: Float, var hp: Int, val maxHp: Int, val boss: Boolean = false, var hitId: Int = -1, var flash: Float = 0f)
    data class Pickup(val x: Float, var taken: Boolean = false)
    data class Hazard(val x: Float, val w: Float)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = 5894
        setContentView(GameView())
    }

    inner class GameView : View(this@MainActivity) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val birds = mutableListOf<Flamingo>()
        private val pickups = mutableListOf<Pickup>()
        private val hazards = mutableListOf<Hazard>()
        private val worldW = 5600f
        private var px = 220f; private var py = 0f; private var vy = 0f; private var ground = 0f; private var cam = 0f
        private var face = 1; private var joyId: Int? = null; private var joyX = 0f; private var joyY = 0f
        private var score = 0; private var hp = 100; private var beers = 0; private var started = false; private var dead = false; private var won = false
        private var attack = 0f; private var attackId = 0; private var hurt = 0f; private var time = 0f; private var last = System.nanoTime()
        private var message = ""; private var messageTime = 0f; private var combo = 0; private var comboTime = 0f; private var bestCombo = 0; private var wave = 1
        private var bossIntro = false; private var rage = false

        init { reset() }
        private fun reset() {
            px=220f; py=0f; vy=0f; cam=0f; score=0; hp=100; beers=0; dead=false; won=false; attack=0f; hurt=0f; time=0f
            combo=0; comboTime=0f; bestCombo=0; wave=1; bossIntro=false; rage=false; joyId=null; joyX=0f; joyY=0f
            birds.clear(); birds += Flamingo(900f,2,2); birds += Flamingo(1420f,2,2); birds += Flamingo(2050f,3,3); birds += Flamingo(2700f,3,3); birds += Flamingo(3350f,4,4); birds += Flamingo(4550f,10,10,true)
            pickups.clear(); pickups += Pickup(1180f); pickups += Pickup(2420f); pickups += Pickup(3600f)
            hazards.clear(); hazards += Hazard(1740f,110f); hazards += Hazard(3020f,105f); hazards += Hazard(3920f,115f)
        }

        override fun onDraw(c: Canvas) {
            val now=System.nanoTime(); val dt=((now-last)/1e9f).coerceAtMost(.04f); last=now
            ground=height*.77f; if(py==0f) py=ground
            if(started&&!dead&&!won) update(dt)
            scene(c); hud(c); controls(c); overlay(c); postInvalidateOnAnimation()
        }

        private fun update(dt: Float) {
            time+=dt; comboTime=max(0f,comboTime-dt); if(comboTime<=0f) combo=0
            val move=if(abs(joyX)<.14f) 0f else joyX
            if(move!=0f){ face=if(move>0f)1 else -1; px=(px+move*430f*dt).coerceIn(55f,worldW-80f) }
            vy+=1550f*dt; py+=vy*dt; if(py>ground){py=ground;vy=0f}
            attack=max(0f,attack-dt); hurt=max(0f,hurt-dt); messageTime=max(0f,messageTime-dt)
            cam+=(px-width*.37f-cam)*min(1f,dt*7f); cam=cam.coerceIn(0f,max(0f,worldW-width.toFloat()))
            wave=if(px<1800f)1 else if(px<3700f)2 else 3
            for(b in birds){
                b.flash=max(0f,b.flash-dt); if(b.hp<=0) continue
                val dx=px-b.x
                if(abs(dx)<(if(b.boss)800f else 520f)){
                    val fast=b.boss&&b.hp<=b.maxHp/2; val speed=if(fast)180f else if(b.boss)115f else 88f
                    b.x+=(if(dx>0f)1f else -1f)*speed*dt
                }
                if(attack>0f&&b.hitId!=attackId){
                    val signed=(b.x-px)*face.toFloat()
                    if(signed in 15f..195f&&py>ground-165f){
                        b.hp--; b.hitId=attackId; b.flash=.12f; b.x+=face.toFloat()*(if(b.boss)45f else 82f)
                        combo++; comboTime=1.45f; bestCombo=max(bestCombo,combo); score+=(if(b.boss)250 else 100)*(1+(combo-1)/3)
                        say(if(combo>=3)"$combo HIT COMBO — BAD DECISIONS MULTIPLIED" else "BONK.",.7f)
                        if(b.boss&&b.hp<=b.maxHp/2&&!rage&&b.hp>0){rage=true;say("ALPHA FLAMINGO HAS ENTERED FULL KAREN MODE.",1.7f)}
                        if(b.hp<=0) score+=if(b.boss)1800 else 300
                    }
                }
                if(abs(b.x-px)<(if(b.boss)95f else 62f)&&py>ground-95f&&hurt<=0f){
                    hp=max(0,hp-(if(b.boss)18 else 10)); hurt=.75f; combo=0; px+=if(dx>0f)-105f else 105f; say("THIS WAS A TERRIBLE PLAN.",.9f); if(hp<=0)dead=true
                }
            }
            for(h in hazards) if(abs(px-h.x)<h.w/2f+28f&&py>ground-72f&&hurt<=0f){hp=max(0,hp-8);hurt=.7f;px+=if(px<h.x)-100f else 100f;if(hp<=0)dead=true}
            for(q in pickups) if(!q.taken&&abs(q.x-px)<68f&&py>ground-130f){q.taken=true;beers++;hp=min(100,hp+25);score+=200;say("COLD ONE ACQUIRED. +25 QUESTIONABLE HEALTH.",1.2f)}
            val boss=birds.last(); if(!bossIntro&&px>4050f&&boss.hp>0){bossIntro=true;say("BOSS FIGHT: ALPHA FLAMINGO",2f)}; if(boss.hp<=0&&px>5100f)won=true
        }
        private fun say(s:String,d:Float){message=s;messageTime=d}

        private fun scene(c:Canvas){
            p.shader=LinearGradient(0f,0f,0f,ground,Color.rgb(55,62,137),Color.rgb(245,113,99),Shader.TileMode.CLAMP); c.drawRect(0f,0f,width.toFloat(),ground,p); p.shader=null
            p.color=Color.rgb(255,181,48); c.drawCircle(width*.68f,ground*.43f,height*.085f,p)
            p.color=Color.rgb(42,48,85); for(i in 0..10){val x=i*width/9f-(cam*.08f%160f);c.drawRect(x,ground*.43f,x+12f,ground*.72f,p);c.drawCircle(x+6f,ground*.42f,38f,p)}
            water(c); palms(c); house(c); fence(c)
            p.color=Color.rgb(214,173,124);c.drawRect(0f,ground,width.toFloat(),ground+height*.075f,p);p.color=Color.rgb(28,104,67);c.drawRect(0f,ground+height*.075f,width.toFloat(),height.toFloat(),p)
            sign(c,650f,"POOL NOODLE\nPANIC");sign(c,3150f,"SAME MESS\nDIFFERENT DAY")
            hazards.forEach{hazard(c,it)};pickups.filter{!it.taken}.forEach{pickup(c,it)};birds.filter{it.hp>0}.forEach{bird(c,it)};man(c);sign(c,5250f,"BACKYARD\nEXIT")
        }
        private fun water(c:Canvas){p.color=Color.rgb(27,99,143);c.drawRect(0f,ground*.58f,width.toFloat(),ground*.73f,p);p.color=Color.rgb(255,143,113);for(i in 0..8){val x=i*180f-(cam*.15f%180f);c.drawRect(x,ground*.64f,x+95f,ground*.645f,p)};p.color=Color.rgb(21,47,52);val ax=width*.76f-(cam*.05f%400f);c.drawOval(ax,ground*.66f,ax+105f,ground*.70f,p);p.color=Color.YELLOW;c.drawCircle(ax+78f,ground*.675f,4f,p)}
        private fun palms(c:Canvas){for(i in 0..5){val x=i*330f-(cam*.22f%330f)-100f;p.color=Color.rgb(72,49,39);p.strokeWidth=18f;c.drawLine(x,ground*.65f,x+35f,ground*.22f,p);p.color=Color.rgb(15,75,55);for(a in -2..2){val af=a.toFloat();c.drawOval(x+35f+af*8f-70f,ground*.22f-18f-abs(af)*8f,x+35f+af*8f+70f,ground*.22f+18f+abs(af)*5f,p)}}}
        private fun house(c:Canvas){val x=150f-(cam*.35f%1900f);p.color=Color.rgb(89,91,111);c.drawRect(x,ground*.45f,x+430f,ground*.69f,p);p.color=Color.rgb(52,45,52);val q=Path();q.moveTo(x-30f,ground*.46f);q.lineTo(x+210f,ground*.32f);q.lineTo(x+465f,ground*.46f);q.close();c.drawPath(q,p);p.color=Color.rgb(255,205,85);for(i in 0..5)c.drawCircle(x+45f+i*65f,ground*.48f,5f,p)}
        private fun fence(c:Canvas){p.color=Color.rgb(128,88,62);var x=-(cam*.62f%78f)-78f;while(x<width+78f){c.drawRect(x,ground*.53f,x+62f,ground*.73f,p);val q=Path();q.moveTo(x,ground*.53f);q.lineTo(x+31f,ground*.49f);q.lineTo(x+62f,ground*.53f);q.close();c.drawPath(q,p);x+=78f}}
        private fun sign(c:Canvas,xw:Float,s:String){val x=xw-cam;if(x !in -180f..width+180f)return;p.color=Color.rgb(78,48,29);c.drawRect(x-6f,ground-145f,x+6f,ground,p);p.color=Color.rgb(231,211,168);c.drawRect(x-105f,ground-225f,x+105f,ground-140f,p);p.color=Color.rgb(36,31,32);p.typeface=Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.CENTER;p.textSize=20f;s.split("\n").forEachIndexed{i,v->c.drawText(v,x,ground-192f+i*27f,p)};p.textAlign=Paint.Align.LEFT}
        private fun hazard(c:Canvas,h:Hazard){val x=h.x-cam;if(x !in -120f..width+120f)return;p.color=Color.rgb(178,36,38);c.drawRoundRect(x-50f,ground-58f,x+50f,ground+4f,8f,8f,p);p.color=Color.WHITE;c.drawRect(x-53f,ground-66f,x+53f,ground-52f,p);p.color=Color.rgb(90,24,24);c.drawRect(x-38f,ground-45f,x+38f,ground-30f,p)}
        private fun pickup(c:Canvas,q:Pickup){val x=q.x-cam;if(x !in -80f..width+80f)return;val y=ground-62f+sin(time*4f+q.x*.01f)*6f;p.color=Color.argb(80,255,224,60);c.drawCircle(x,y,40f,p);p.color=Color.rgb(226,226,220);c.drawRoundRect(x-18f,y-32f,x+18f,y+32f,5f,5f,p);p.color=Color.rgb(35,93,184);c.drawRect(x-18f,y-10f,x+18f,y+14f,p);p.color=Color.YELLOW;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=11f;c.drawText("BEER",x,y+5f,p);p.textAlign=Paint.Align.LEFT}
        private fun man(c:Canvas){if(hurt>0f&&(hurt*12f).toInt()%2==0)return;val x=px-cam;val y=py;p.color=Color.rgb(235,169,115);c.drawRect(x-28f,y-7f,x-9f,y+43f,p);c.drawRect(x+9f,y-7f,x+28f,y+43f,p);p.color=Color.rgb(26,86,175);c.drawRect(x-38f,y-49f,x+38f,y+5f,p);p.color=Color.WHITE;c.drawRect(x-35f,y-112f,x+35f,y-47f,p);p.color=Color.rgb(28,135,72);c.drawCircle(x,y-80f,13f,p);p.color=Color.rgb(235,169,115);c.drawCircle(x,y-148f,37f,p);p.color=Color.rgb(105,59,27);c.drawRect(x-42f,y-179f,x+28f,y-159f,p);c.drawRect(x-42f,y-162f,x-27f,y-111f,p);p.color=Color.rgb(222,189,126);c.drawRect(x-39f,y-184f,x+30f,y-169f,p);p.color=Color.rgb(35,35,38);c.drawRect(x-29f,y-154f,x-3f,y-143f,p);c.drawRect(x+3f,y-154f,x+29f,y-143f,p);c.drawRect(x-34f,y+39f,x-4f,y+47f,p);c.drawRect(x+4f,y+39f,x+34f,y+47f,p);p.strokeWidth=21f;p.strokeCap=Paint.Cap.ROUND;p.color=Color.rgb(32,154,255);val sx=x+face*26f;val sy=y-90f;if(attack>0f){val z=(1f-attack/.2f).coerceIn(0f,1f);val deg=if(face>0)-70f+z*105f else 250f-z*105f;val r=Math.toRadians(deg.toDouble());c.drawLine(sx,sy,sx+cos(r).toFloat()*172f,sy+sin(r).toFloat()*172f,p)}else c.drawLine(sx,sy,x+face*82f,y-132f,p)}
        private fun bird(c:Canvas,b:Flamingo){val x=b.x-cam;if(x !in -190f..width+190f)return;val s=if(b.boss)1.55f else 1f;val y=ground-86f*s;p.color=if(b.flash>0f)Color.WHITE else if(b.boss&&b.hp<=b.maxHp/2)Color.rgb(205,31,68) else Color.rgb(239,62,116);c.drawOval(x-48f*s,y-30f*s,x+43f*s,y+31f*s,p);val w=Path();w.moveTo(x-10f*s,y-12f*s);w.lineTo(x+20f*s,y-65f*s);w.lineTo(x+35f*s,y-8f*s);w.close();c.drawPath(w,p);p.style=Paint.Style.STROKE;p.strokeWidth=14f*s;c.drawLine(x+25f*s,y-20f*s,x+32f*s,y-94f*s,p);p.style=Paint.Style.FILL;c.drawCircle(x+49f*s,y-112f*s,25f*s,p);p.color=Color.rgb(25,25,25);val beak=Path();beak.moveTo(x+68f*s,y-113f*s);beak.lineTo(x+105f*s,y-126f*s);beak.lineTo(x+73f*s,y-101f*s);beak.close();c.drawPath(beak,p);p.color=Color.YELLOW;c.drawCircle(x+54f*s,y-118f*s,7f*s,p);p.color=Color.RED;c.drawCircle(x+56f*s,y-119f*s,3f*s,p);p.strokeWidth=8f*s;p.color=Color.rgb(45,30,40);c.drawLine(x-17f*s,y+24f*s,x-23f*s,ground+25f,p);c.drawLine(x+12f*s,y+24f*s,x+21f*s,ground+25f,p);if(b.boss){p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=23f;p.color=Color.YELLOW;c.drawText("ALPHA FLAMINGO",x,y-170f,p);p.color=Color.argb(190,0,0,0);c.drawRect(x-90f,y-158f,x+90f,y-143f,p);p.color=Color.RED;c.drawRect(x-90f,y-158f,x-90f+180f*b.hp/b.maxHp.toFloat(),y-143f,p);p.textAlign=Paint.Align.LEFT}}
        private fun hud(c:Canvas){val pad=height*.03f;p.color=Color.argb(205,9,13,24);c.drawRoundRect(pad,pad,width*.34f,pad+height*.095f,10f,10f,p);p.color=Color.rgb(255,204,43);p.typeface=Typeface.DEFAULT_BOLD;p.textSize=height*.035f;c.drawText("HOLD MY BEER",pad*1.7f,pad+height*.04f,p);p.color=Color.rgb(20,38,57);c.drawRect(pad*1.7f,pad+height*.057f,width*.31f,pad+height*.079f,p);p.color=if(hp>35)Color.rgb(54,210,57) else Color.RED;c.drawRect(pad*1.7f,pad+height*.057f,pad*1.7f+(width*.31f-pad*1.7f)*hp/100f,pad+height*.079f,p);p.textAlign=Paint.Align.CENTER;p.textSize=height*.048f;p.color=Color.rgb(255,209,45);c.drawText("FLORIDA MAN",width/2f,pad+height*.043f,p);p.textSize=height*.024f;p.color=Color.WHITE;c.drawText("POOL NOODLE PANIC • LEVEL 1 • WAVE $wave/3",width/2f,pad+height*.074f,p);p.textAlign=Paint.Align.RIGHT;p.textSize=height*.032f;c.drawText("SCORE $score",width-pad*1.5f,pad+height*.034f,p);p.textSize=height*.025f;p.color=Color.rgb(255,207,47);c.drawText("COLD ONES x $beers",width-pad*1.5f,pad+height*.068f,p);if(combo>=2){p.textAlign=Paint.Align.CENTER;p.textSize=height*.044f;p.color=Color.rgb(255,117,34);c.drawText("COMBO x$combo",width*.69f,height*.16f,p)};if(messageTime>0f&&started&&!dead&&!won){p.textAlign=Paint.Align.CENTER;p.textSize=min(27f,height*.032f);p.color=Color.WHITE;c.drawText(message,width/2f,height*.225f,p)};p.textAlign=Paint.Align.LEFT}
        private fun controls(c:Canvas){val r=min(height*.085f,78f);val y=height-r-height*.035f;val jr=min(height*.115f,96f);val cx=max(96f,height*.16f);val cy=height-max(82f,height*.14f);p.color=Color.argb(110,5,10,18);c.drawCircle(cx,cy,jr,p);p.color=Color.argb(220,28,40,55);c.drawCircle(cx+joyX*jr*.58f,cy+joyY*jr*.58f,jr*.46f,p);button(c,width-r-height*.035f-r*2.25f,y,r,"JUMP");button(c,width-r-height*.035f,y,r,"WHACK")}
        private fun button(c:Canvas,x:Float,y:Float,r:Float,s:String){p.color=Color.argb(150,5,10,18);c.drawCircle(x,y,r,p);p.color=Color.WHITE;p.typeface=Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.CENTER;p.textSize=r*.32f;c.drawText(s,x,y+p.textSize*.34f,p);p.textAlign=Paint.Align.LEFT}
        private fun overlay(c:Canvas){if(started&&!dead&&!won)return;p.color=Color.argb(210,4,8,18);c.drawRect(0f,height*.24f,width.toFloat(),height*.70f,p);p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;if(!started){p.textSize=height*.07f;p.color=Color.rgb(255,205,39);c.drawText("FLORIDA MAN",width/2f,height*.38f,p);p.textSize=height*.045f;p.color=Color.WHITE;c.drawText("LEGENDARY BAD DECISIONS",width/2f,height*.45f,p);p.textSize=height*.035f;p.color=Color.rgb(255,112,46);c.drawText("POOL NOODLE PANIC",width/2f,height*.53f,p);p.textSize=height*.027f;p.color=Color.WHITE;c.drawText("TAP TO START • SURVIVE 3 WAVES • DEFEAT THE ALPHA FLAMINGO",width/2f,height*.61f,p)}else if(dead){p.textSize=height*.07f;p.color=Color.rgb(255,80,65);c.drawText("WELL, THAT ESCALATED.",width/2f,height*.44f,p);p.textSize=height*.035f;p.color=Color.WHITE;c.drawText("BEST COMBO $bestCombo • TAP TO TRY AGAIN",width/2f,height*.55f,p)}else{p.textSize=height*.066f;p.color=Color.YELLOW;c.drawText("BACKYARD SURVIVED. SOMEHOW.",width/2f,height*.43f,p);p.textSize=height*.034f;p.color=Color.WHITE;c.drawText("SCORE $score • BEST COMBO $bestCombo • COLD ONES $beers/3",width/2f,height*.54f,p);c.drawText("TAP TO RUN IT AGAIN",width/2f,height*.62f,p)};p.textAlign=Paint.Align.LEFT}
        private fun jump(){if(py>=ground-4f)vy=-720f}
        private fun whack(){if(attack<=0f){attackId++;attack=.2f}}
        override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{val i=e.actionIndex;val id=e.getPointerId(i);val x=e.getX(i);val y=e.getY(i);if(!started){started=true;last=System.nanoTime();return true};if(dead||won){reset();started=true;last=System.nanoTime();return true};val cx=max(96f,height*.16f);val cy=height-max(82f,height*.14f);val jr=min(height*.115f,96f);if(joyId==null&&hypot(x-cx,y-cy)<=jr*1.45f){joyId=id;setJoy(x,y)}else{val r=min(height*.085f,78f);val by=height-r-height*.035f;val wx=width-r-height*.035f;val jumpX=wx-r*2.25f;if(hypot(x-wx,y-by)<=r*1.22f)whack() else if(hypot(x-jumpX,y-by)<=r*1.22f)jump()}};MotionEvent.ACTION_MOVE->{joyId?.let{id->val i=e.findPointerIndex(id);if(i>=0)setJoy(e.getX(i),e.getY(i))}};MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP->{val id=e.getPointerId(e.actionIndex);if(id==joyId){joyId=null;joyX=0f;joyY=0f}};MotionEvent.ACTION_CANCEL->{joyId=null;joyX=0f;joyY=0f}};return true}
        private fun setJoy(x:Float,y:Float){val cx=max(96f,height*.16f);val cy=height-max(82f,height*.14f);val r=min(height*.115f,96f);val dx=x-cx;val dy=y-cy;val d=hypot(dx,dy);if(d<=r||d==0f){joyX=(dx/r).coerceIn(-1f,1f);joyY=(dy/r).coerceIn(-1f,1f)}else{joyX=dx/d;joyY=dy/d}}
    }
}
