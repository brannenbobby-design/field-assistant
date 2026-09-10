package com.brannenservices.fieldassistant

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.graphics.*
import kotlin.math.max

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(FloridaGameView())
    }

    inner class FloridaGameView : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var playerX = 180f
        private var playerY = 0f
        private var vy = 0f
        private var ground = 0f
        private var attacking = false
        private var score = 0
        private var health = 100
        private var started = false
        private var flamingoX = 1000f
        private var flamingoHp = 3
        private var last = System.nanoTime()

        override fun onDraw(c: Canvas) {
            val now = System.nanoTime(); val dt = ((now-last)/1e9f).coerceAtMost(.04f); last=now
            ground = height * .78f
            if (playerY == 0f) playerY = ground
            if (started) update(dt)

            c.drawColor(Color.rgb(67,190,225))
            p.color=Color.rgb(255,236,170); c.drawCircle(width*.82f,height*.18f,70f,p)
            p.color=Color.rgb(30,150,85); c.drawRect(0f,ground+55,width.toFloat(),height.toFloat(),p)
            p.color=Color.rgb(229,205,151); c.drawRect(0f,ground,width.toFloat(),ground+55,p)

            // Florida Man: tank top, shorts, mullet and flip-flops.
            p.color=Color.rgb(245,190,145); c.drawCircle(playerX,playerY-125,31f,p)
            p.color=Color.rgb(100,55,25); c.drawRect(playerX-34,playerY-155,playerX+22,playerY-139,p); c.drawRect(playerX-34,playerY-145,playerX-23,playerY-92,p)
            p.color=Color.WHITE; c.drawRect(playerX-28,playerY-96,playerX+28,playerY-34,p)
            p.color=Color.rgb(45,75,155); c.drawRect(playerX-30,playerY-34,playerX+30,playerY+8,p)
            p.color=Color.rgb(245,190,145); c.drawRect(playerX-24,playerY+8,playerX-9,playerY+43,p); c.drawRect(playerX+9,playerY+8,playerX+24,playerY+43,p)
            p.color=Color.BLACK; c.drawRect(playerX-30,playerY+40,playerX-5,playerY+46,p); c.drawRect(playerX+5,playerY+40,playerX+30,playerY+46,p)

            // Pool noodle weapon.
            p.strokeWidth=18f; p.strokeCap=Paint.Cap.ROUND; p.color=Color.MAGENTA
            val reach=if(attacking) 105f else 55f; c.drawLine(playerX+20,playerY-75,playerX+20+reach,playerY-105,p)

            // Rabid flamingo.
            if(flamingoHp>0){
                p.style=Paint.Style.STROKE; p.strokeWidth=13f; p.color=Color.rgb(255,70,130)
                c.drawLine(flamingoX,ground-25,flamingoX+5,ground-100,p); c.drawCircle(flamingoX+22,ground-128,28f,p)
                c.drawLine(flamingoX+43,ground-132,flamingoX+78,ground-145,p); c.drawLine(flamingoX,ground-25,flamingoX-12,ground+30,p); c.drawLine(flamingoX+8,ground-25,flamingoX+25,ground+30,p)
                p.style=Paint.Style.FILL; p.color=Color.RED; c.drawCircle(flamingoX+28,ground-137,5f,p)
            }

            p.typeface=Typeface.MONOSPACE; p.textSize=34f; p.color=Color.WHITE
            c.drawText("HOLD MY BEER: $health%",28f,50f,p); c.drawText("SCORE $score",width-230f,50f,p)
            p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.DEFAULT_BOLD; p.textSize=48f; p.color=Color.rgb(255,240,50)
            c.drawText("FLORIDA MAN",width/2f,100f,p)
            p.textSize=25f; p.color=Color.WHITE; c.drawText("POOL NOODLE PANIC",width/2f,135f,p)

            // touch zones
            p.color=Color.argb(90,0,0,0); c.drawCircle(85f,height-90f,62f,p); c.drawCircle(225f,height-90f,62f,p); c.drawCircle(width-225f,height-90f,62f,p); c.drawCircle(width-85f,height-90f,62f,p)
            p.textSize=35f; p.color=Color.WHITE; c.drawText("<",85f,height-78f,p); c.drawText(">",225f,height-78f,p); c.drawText("JUMP",width-225f,height-82f,p); c.drawText("WHACK",width-85f,height-82f,p)
            p.textAlign=Paint.Align.LEFT

            if(!started){ p.color=Color.argb(190,0,0,0); c.drawRect(0f,height*.32f,width.toFloat(),height*.62f,p); p.textAlign=Paint.Align.CENTER; p.textSize=38f;p.color=Color.WHITE;c.drawText("TAP TO START THE BAD DECISIONS",width/2f,height*.48f,p);p.textAlign=Paint.Align.LEFT }
            invalidate()
        }

        private fun update(dt:Float){
            vy += 1250f*dt; playerY += vy*dt
            if(playerY>ground){playerY=ground;vy=0f}
            if(flamingoHp>0){
                flamingoX -= 80f*dt
                if(attacking && flamingoX-playerX in 35f..145f){flamingoHp--;score+=100;flamingoX+=120f;attacking=false}
                if(flamingoX-playerX<45f){health=max(0,health-1);flamingoX+=180f}
                if(flamingoHp<=0){score+=500}
            }
        }

        override fun onTouchEvent(e:MotionEvent):Boolean{
            if(e.action==MotionEvent.ACTION_DOWN){
                if(!started){started=true;return true}
                when{
                    e.x<width*.15f -> playerX=max(45f,playerX-35f)
                    e.x<width*.35f -> playerX=(playerX+35f).coerceAtMost(width-50f)
                    e.x>width*.82f -> attacking=true
                    e.x>width*.62f && playerY>=ground -> vy=-600f
                }
            }
            if(e.action==MotionEvent.ACTION_UP) attacking=false
            return true
        }
    }
}
