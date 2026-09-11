package com.brannenservices.fieldassistant

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.*
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(1024, 1024)
        window.decorView.systemUiVisibility = 5894
        setContentView(GameView(this))
    }

    class GameView(activity: Activity) : View(activity) {
        data class Bird(
            var x: Float,
            var hp: Int,
            val max: Int,
            val boss: Boolean = false,
            var hit: Int = -1,
            var flash: Float = 0f,
            var dir: Int = -1
        )

        data class Beer(val x: Float, var taken: Boolean = false)

        private val W = 640
        private val H = 360
        private val G = 278f
        private val WORLD = 3000f

        private val frame = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        private val c = Canvas(frame)
        private val p = Paint().apply { isAntiAlias = false }
        private val pix = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }

        private var px = 105f
        private var py = G
        private var vy = 0f
        private var cam = 0f
        private var face = 1
        private var joy = 0f
        private var joyId = -1
        private var started = false
        private var dead = false
        private var won = false
        private var hp = 100
        private var score = 0
        private var cans = 0
        private var attack = 0f
        private var attackId = 0
        private var hurt = 0f
        private var t = 0f
        private var last = System.nanoTime()
        private var combo = 0
        private var comboT = 0f
        private var wave = 1
        private var msg = ""
        private var msgT = 0f

        private val birds = mutableListOf(
            Bird(470f, 2, 2),
            Bird(760f, 2, 2),
            Bird(1080f, 3, 3),
            Bird(1450f, 3, 3),
            Bird(1880f, 4, 4),
            Bird(2470f, 12, 12, true)
        )

        private val beers = mutableListOf(
            Beer(650f),
            Beer(1330f),
            Beer(2050f)
        )

        private val obstacles = floatArrayOf(900f, 1510f, 2300f)

        override fun onDraw(out: Canvas) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceAtMost(.04f)
            last = now

            if (started && !dead && !won) update(dt)
            drawGame()
            out.drawBitmap(frame, null, Rect(0, 0, width, height), pix)
            postInvalidateOnAnimation()
        }

        private fun update(dt: Float) {
            t += dt
            attack = max(0f, attack - dt)
            hurt = max(0f, hurt - dt)
            comboT = max(0f, comboT - dt)
            msgT = max(0f, msgT - dt)
            if (comboT <= 0f) combo = 0

            if (abs(joy) > .12f) {
                face = if (joy > 0f) 1 else -1
                val old = px
                var next = (px + joy * 220f * dt).coerceIn(30f, WORLD - 40f)

                if (py > G - 42f) {
                    for (obstacle in obstacles) {
                        if (old < obstacle - 35f && next >= obstacle - 35f) {
                            next = obstacle - 36f
                            if (hurt <= 0f) hazard(-1)
                        } else if (old > obstacle + 35f && next <= obstacle + 35f) {
                            next = obstacle + 36f
                            if (hurt <= 0f) hazard(1)
                        }
                    }
                }
                px = next
            }

            vy += 780f * dt
            py += vy * dt
            if (py > G) {
                py = G
                vy = 0f
            }

            cam += (px - 235f - cam) * min(1f, dt * 7f)
            cam = cam.coerceIn(0f, WORLD - W)
            wave = if (px < 1000f) 1 else if (px < 2000f) 2 else 3

            birds.forEach { b ->
                b.flash = max(0f, b.flash - dt)
                if (b.hp <= 0) return@forEach

                val dx = px - b.x
                b.dir = if (dx > 0f) 1 else -1

                if (abs(dx) < (if (b.boss) 390f else 260f)) {
                    b.x += b.dir * (if (b.boss) 78f else 55f) * dt
                }

                if (attack > 0f && b.hit != attackId) {
                    val d = (b.x - px) * face
                    if (d in 5f..105f && py > G - 90f) {
                        b.hp--
                        b.hit = attackId
                        b.flash = .1f
                        b.x += face * (if (b.boss) 20f else 38f)
                        combo++
                        comboT = 1.3f
                        score += (if (b.boss) 250 else 100) * (1 + (combo - 1) / 3)
                        say(if (combo > 2) "$combo HIT COMBO!" else "BONK!")
                        if (b.hp <= 0) score += if (b.boss) 1800 else 300
                    }
                }

                if (abs(b.x - px) < (if (b.boss) 48f else 31f) &&
                    py > G - 55f && hurt <= 0f
                ) {
                    hp = max(0, hp - (if (b.boss) 18 else 10))
                    hurt = .7f
                    px += if (dx > 0f) -55f else 55f
                    if (hp <= 0) dead = true
                }
            }

            beers.forEach {
                if (!it.taken && abs(it.x - px) < 34f && py > G - 70f) {
                    it.taken = true
                    cans++
                    hp = min(100, hp + 25)
                    score += 200
                    say("COLD ONE ACQUIRED!")
                }
            }

            if (birds.last().hp <= 0 && px > 2780f) won = true
        }

        private fun hazard(push: Int) {
            hp = max(0, hp - 8)
            hurt = .55f
            px = (px + push * 16f).coerceIn(30f, WORLD - 40f)
            say("WATCH THE COOLER!")
            if (hp <= 0) dead = true
        }

        private fun say(s: String) {
            msg = s
            msgT = 1.1f
        }

        private fun col(v: Int) {
            p.color = v
            p.style = Paint.Style.FILL
            p.shader = null
        }

        private fun r(l: Float, top: Float, rr: Float, bottom: Float, v: Int) {
            col(v)
            c.drawRect(l, top, rr, bottom, p)
        }

        private fun text(
            s: String,
            x: Float,
            y: Float,
            z: Float,
            v: Int,
            center: Boolean = false
        ) {
            col(v)
            p.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            p.textSize = z
            p.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            c.drawText(s, x, y, p)
            p.textAlign = Paint.Align.LEFT
        }

        private fun drawGame() {
            sky()
            farCity()
            marina()
            dock()
            props()
            beers.filter { !it.taken }.forEach { beer(it) }
            birds.filter { it.hp > 0 }.forEach { bird(it) }
            man()
            foreground()
            hud()
            controls()

            if (!started) panel("POOL NOODLE PANIC", "TAP TO START")
            if (dead) panel("WELL, THAT WENT BAD", "TAP TO TRY AGAIN")
            if (won) panel("LEVEL COMPLETE", "ALPHA FLAMINGO DEFEATED")

            if (msgT > 0f) {
                r(205f, 304f, 435f, 327f, Color.rgb(14, 13, 29))
                text(msg, 320f, 320f, 10f, Color.YELLOW, true)
            }
        }

        private fun sky() {
            p.shader = LinearGradient(
                0f, 38f, 0f, 218f,
                Color.rgb(50, 31, 105),
                Color.rgb(250, 91, 72),
                Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, 640f, 218f, p)
            p.shader = null

            for (i in 0..9) {
                val x = (i * 83 - (cam * .025f).toInt() % 83).toFloat()
                r(x, 77f + (i % 4) * 12, x + 45, 80f + (i % 4) * 12, Color.rgb(119, 51, 104))
                r(x + 15, 82f + (i % 4) * 12, x + 64, 85f + (i % 4) * 12, Color.rgb(177, 57, 94))
            }

            col(Color.rgb(255, 194, 70))
            c.drawCircle(478f, 151f, 43f, p)
            for (i in 0..5) {
                r(438f + i * 5, 150f + i * 6, 519f - i * 7, 153f + i * 6, Color.rgb(255, 221, 105))
            }
        }

        private fun farCity() {
            val off = -(cam * .08f % 900f)
            for (k in 0..1) {
                val b = off + k * 900f
                r(b + 50, 153f, b + 83, 205f, Color.rgb(50, 45, 76))
                r(b + 92, 167f, b + 118, 205f, Color.rgb(57, 47, 77))
                r(b + 130, 142f, b + 166, 205f, Color.rgb(49, 43, 69))
                for (x in 55..160 step 12) {
                    for (y in 151..196 step 11) {
                        if ((x + y) % 3 != 0) {
                            r(b + x, y.toFloat(), b + x + 4, y + 5f, Color.rgb(241, 151, 75))
                        }
                    }
                }
            }
            for (i in -1..6) palm(i * 135f - (cam * .12f % 135f) + 35f)
        }

        private fun palm(x: Float) {
            r(x - 2, 124f, x + 3, 218f, Color.rgb(32, 30, 43))
            for (i in -3..3) {
                val q = Path()
                q.moveTo(x, 126f)
                q.lineTo(x + i * 22, 108f + abs(i) * 5)
                q.lineTo(x + i * 11, 132f)
                q.close()
                col(if (i % 2 == 0) Color.rgb(19, 67, 58) else Color.rgb(28, 83, 64))
                c.drawPath(q, p)
            }
        }

        private fun marina() {
            r(0f, 190f, 640f, 224f, Color.rgb(18, 82, 122))
            for (i in 0..10) {
                val x = i * 70f - (cam * .18f % 70f)
                r(x, 204f + (i % 3) * 5, x + 39, 206f + (i % 3) * 5, Color.rgb(52, 142, 159))
                r(x + 18, 216f - (i % 2) * 5, x + 58, 218f - (i % 2) * 5, Color.rgb(241, 128, 84))
            }
            sailboat(520f - (cam * .16f % 900f), 181f)
            tiki(120f - cam * .34f)
        }

        private fun sailboat(x: Float, y: Float) {
            if (x !in -100f..740f) return
            r(x, y, x + 45, y + 5, Color.rgb(42, 38, 55))
            r(x + 20, y - 41, x + 22, y, Color.rgb(54, 42, 50))
            val q = Path()
            q.moveTo(x + 22, y - 39)
            q.lineTo(x + 22, y - 4)
            q.lineTo(x + 42, y - 5)
            q.close()
            col(Color.rgb(238, 214, 174))
            c.drawPath(q, p)
            r(x + 4, y + 5, x + 41, y + 9, Color.rgb(24, 36, 52))
        }

        private fun tiki(x: Float) {
            if (x !in -180f..720f) return
            r(x, 154f, x + 126, 224f, Color.rgb(79, 48, 35))
            r(x + 7, 168f, x + 119, 218f, Color.rgb(108, 64, 39))
            for (i in 0..5) {
                r(x + 12 + i * 18, 181f, x + 17 + i * 18, 215f, Color.rgb(49, 32, 29))
            }
            val q = Path()
            q.moveTo(x - 12, 160f)
            q.lineTo(x + 65, 127f)
            q.lineTo(x + 139, 160f)
            q.close()
            col(Color.rgb(72, 48, 32))
            c.drawPath(q, p)
            r(x + 18, 166f, x + 108, 182f, Color.rgb(205, 149, 80))
            text("SALTY FLAMINGO", x + 63, 177f, 7f, Color.rgb(45, 29, 26), true)
        }

        private fun dock() {
            r(0f, 224f, 640f, 279f, Color.rgb(73, 49, 38))
            for (i in 0..12) {
                val x = i * 55f - (cam * .68f % 55f)
                r(
                    x, 224f, x + 50, 274f,
                    if (i % 2 == 0) Color.rgb(145, 91, 52) else Color.rgb(121, 72, 46)
                )
                r(x, 228f, x + 50, 232f, Color.rgb(194, 127, 67))
                r(x + 3, 267f, x + 49, 272f, Color.rgb(72, 45, 37))
            }

            r(0f, 279f, 640f, 360f, Color.rgb(7, 55, 82))
            for (i in 0..11) {
                val x = i * 63f - (cam * .38f % 63f)
                r(x, 292f + (i % 3) * 13, x + 39, 295f + (i % 3) * 13, Color.rgb(26, 112, 140))
                r(x + 15, 331f - (i % 2) * 13, x + 58, 334f - (i % 2) * 13, Color.rgb(18, 90, 124))
                r(x + 29, 346f - (i % 4) * 7, x + 52, 349f - (i % 4) * 7, Color.rgb(198, 102, 72))
            }
        }

        private fun props() {
            sign(360f, "GOOD BEER", "BAD IDEAS")
            sign(1660f, "NO WAKE", "JUST CHAOS")
            sign(2750f, "BEWARE", "GATORS")

            obstacles.forEach { wx ->
                val x = wx - cam
                if (x in -50f..690f) {
                    c.drawBitmap(SpriteArt.cooler, null, RectF(x - 28, G - 28, x + 28, G + 13), pix)
                }
            }

            val ex = 2860f - cam
            if (ex in -80f..720f) {
                c.drawBitmap(SpriteArt.exit, null, RectF(ex - 40, G - 100, ex + 40, G), pix)
                text("EXIT", ex, G - 104, 10f, Color.YELLOW, true)
            }
        }

        private fun foreground() {
            for (wx in floatArrayOf(540f, 1320f, 2140f)) {
                val x = wx - cam
                if (x in -40f..680f) {
                    r(x - 7, 264f, x + 8, 345f, Color.rgb(47, 35, 31))
                    r(x - 11, 260f, x + 12, 270f, Color.rgb(92, 60, 39))
                    r(x - 9, 263f, x + 10, 267f, Color.rgb(160, 102, 53))
                }
            }
        }

        private fun sign(wx: Float, a: String, b: String) {
            val x = wx - cam
            if (x !in -100f..740f) return
            r(x - 4, 177f, x + 4, 225f, Color.rgb(62, 38, 27))
            r(x - 51, 155f, x + 51, 181f, Color.rgb(163, 104, 52))
            r(x - 48, 158f, x + 48, 178f, Color.rgb(205, 145, 73))
            text(a, x, 168f, 8f, Color.rgb(37, 27, 26), true)
            text(b, x, 176f, 8f, Color.rgb(37, 27, 26), true)
        }

        private fun man() {
            if (hurt > 0f && (hurt * 12f).toInt() % 2 == 0) return

            val x = px - cam
            val airborne = py < G - 1f
            val moving = abs(joy) > .12f && !airborne
            val walkFrame = ((t * 6f).toInt() % AnimatedRaster.manWalk.size).coerceAtLeast(0)
            val bmp = when {
                airborne -> AnimatedRaster.manJump
                moving -> AnimatedRaster.manWalk[walkFrame]
                else -> SpriteArt.man
            }

            // Let the real raster frames provide the gait. Only idle breathing remains.
            val bob = if (!moving && !airborne) sin(t * 2.4f) * .4f else 0f
            val bodyH = if (airborne) 108f else 112f
            val bodyW = bodyH * bmp.width.toFloat() / bmp.height.toFloat()
            val dst = RectF(
                x - bodyW / 2f,
                py - bodyH + bob,
                x + bodyW / 2f,
                py + bob
            )

            c.save()
            c.scale(face.toFloat(), 1f, x, py)
            if (airborne) c.rotate((-vy / 330f).coerceIn(-1f, 1f) * 1.5f, x, py - 50f)
            c.drawBitmap(bmp, null, dst, pix)
            c.restore()

            // Frame-specific hand anchors keep the pool noodle attached to the fist.
            val walkHandX = floatArrayOf(28f, 18f, 18f, 24f)
            val walkHandY = floatArrayOf(48f, 62f, 62f, 54f)
            val anchorX: Float
            val anchorY: Float
            when {
                airborne -> {
                    // Jump art grips the noodle on the back/left hand.
                    anchorX = -22f
                    anchorY = 52f
                }
                moving -> {
                    anchorX = walkHandX[walkFrame]
                    anchorY = walkHandY[walkFrame]
                }
                else -> {
                    anchorX = 31f
                    anchorY = 43f
                }
            }

            val handX = x + face * anchorX
            val handY = py - anchorY + bob
            val phase = (1f - attack / .2f).coerceIn(0f, 1f)
            val deg = when {
                attack > 0f -> if (face > 0) -38f + phase * 92f else 218f - phase * 92f
                airborne -> if (face > 0) -108f else -72f
                else -> if (face > 0) -18f else 198f
            }

            val angle = Math.toRadians(deg.toDouble())
            val ux = cos(angle).toFloat()
            val uy = sin(angle).toFloat()
            val sx = handX - face * 3f
            val sy = handY + 2f
            val ex = handX + ux * 78f
            val ey = handY + uy * 78f

            col(Color.rgb(11, 92, 166))
            p.strokeWidth = 13f
            p.strokeCap = Paint.Cap.ROUND
            c.drawLine(sx, sy, ex, ey, p)

            col(Color.rgb(54, 188, 255))
            p.strokeWidth = 9f
            c.drawLine(sx, sy - 1f, ex, ey - 1f, p)

            col(Color.rgb(142, 225, 255))
            p.strokeWidth = 2f
            c.drawLine(sx + ux * 8f, sy - 3f, ex - ux * 8f, ey - 3f, p)

            col(Color.rgb(226, 145, 91))
            c.drawCircle(handX, handY, 5.5f, p)
            p.strokeCap = Paint.Cap.BUTT
        }

        private fun bird(b: Bird) {
            val x = b.x - cam
            if (x !in -120f..760f) return

            val scale = if (b.boss) 1.48f else 1f
            val moving = abs(px - b.x) < (if (b.boss) 390f else 260f)
            val frameRate = if (b.boss) 5f else 5.5f
            val frameIndex = (((t * frameRate) + b.x * .01f).toInt() % AnimatedRaster.flWalk.size)
                .coerceAtLeast(0)
            val bmp = if (moving) AnimatedRaster.flWalk[frameIndex] else SpriteArt.flamingo
            val bob = if (moving) 0f else sin(t * 2.1f + b.x * .01f) * .5f
            val h = 96f * scale
            val w = h * bmp.width.toFloat() / bmp.height.toFloat()
            val dst = RectF(x - w / 2f, G - h + bob, x + w / 2f, G + bob)

            c.save()
            c.scale(b.dir.toFloat(), 1f, x, G)
            if (b.flash > 0f) {
                p.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                c.drawBitmap(bmp, null, dst, p)
                p.colorFilter = null
            } else {
                c.drawBitmap(bmp, null, dst, pix)
            }
            c.restore()

            if (b.boss) {
                text("ALPHA", x, G - h - 7f, 8f, Color.YELLOW, true)
                r(x - 35, G - h - 3, x + 35, G - h + 2, Color.rgb(55, 25, 30))
                r(x - 35, G - h - 3, x - 35 + 70f * b.hp / b.max, G - h + 2, Color.RED)
            }
        }

        private fun beer(q: Beer) {
            val x = q.x - cam
            if (x !in -30f..670f) return
            val y = G - 38f + sin(t * 5f + q.x) * 3f
            c.drawBitmap(SpriteArt.beer, null, RectF(x - 13, y - 24, x + 13, y + 20), pix)
        }

        private fun hud() {
            r(0f, 0f, 640f, 38f, Color.rgb(12, 11, 29))
            text("HOLD MY BEER", 12f, 14f, 10f, Color.YELLOW)

            r(12f, 21f, 184f, 31f, Color.rgb(55, 45, 54))
            r(
                15f, 24f, 15f + 166f * hp / 100f, 28f,
                if (hp > 30) Color.rgb(42, 215, 70) else Color.RED
            )

            text("FLORIDA MAN", 320f, 17f, 18f, Color.YELLOW, true)
            text("POOL NOODLE PANIC  •  WAVE $wave/3", 320f, 30f, 7f, Color.WHITE, true)
            text("SCORE $score", 520f, 14f, 8f, Color.WHITE)
            text("COLD ONES x $cans", 520f, 28f, 8f, Color.WHITE)
            if (combo > 1) text("COMBO x$combo", 455f, 48f, 9f, Color.YELLOW)
        }

        private fun controls() {
            col(Color.argb(175, 10, 20, 35))
            c.drawCircle(48f, 321f, 31f, p)

            col(Color.rgb(25, 80, 158))
            c.drawCircle(555f, 321f, 25f, p)

            col(Color.rgb(190, 30, 38))
            c.drawCircle(610f, 321f, 25f, p)

            text("JUMP", 555f, 325f, 8f, Color.WHITE, true)
            text("WHACK", 610f, 325f, 8f, Color.WHITE, true)
        }

        private fun panel(a: String, b: String) {
            r(102f, 126f, 538f, 220f, Color.argb(235, 11, 11, 26))
            r(107f, 131f, 533f, 215f, Color.rgb(36, 27, 60))
            text(a, 320f, 165f, 20f, Color.YELLOW, true)
            text(b, 320f, 193f, 10f, Color.WHITE, true)
        }

        private fun reset() {
            px = 105f
            py = G
            vy = 0f
            cam = 0f
            face = 1
            joy = 0f
            joyId = -1
            hp = 100
            score = 0
            cans = 0
            attack = 0f
            attackId = 0
            hurt = 0f
            combo = 0
            comboT = 0f
            wave = 1
            msg = ""
            msgT = 0f
            dead = false
            won = false

            birds.clear()
            birds.addAll(
                listOf(
                    Bird(470f, 2, 2),
                    Bird(760f, 2, 2),
                    Bird(1080f, 3, 3),
                    Bird(1450f, 3, 3),
                    Bird(1880f, 4, 4),
                    Bird(2470f, 12, 12, true)
                )
            )

            beers.clear()
            beers.addAll(listOf(Beer(650f), Beer(1330f), Beer(2050f)))
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val i = e.actionIndex
                    val id = e.getPointerId(i)
                    val x = e.getX(i) / width * W
                    val y = e.getY(i) / height * H

                    if (!started || dead || won) {
                        if (dead || won) reset()
                        started = true
                        return true
                    }

                    if (x < 130f && y > 270f) {
                        joyId = id
                        joy = ((x - 48f) / 48f).coerceIn(-1f, 1f)
                    } else if (x > 585f && y > 285f) {
                        attack = .2f
                        attackId++
                    } else if (x > 525f && y > 285f && py >= G - 1f) {
                        vy = -330f
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (joyId >= 0) {
                        for (i in 0 until e.pointerCount) {
                            if (e.getPointerId(i) == joyId) {
                                val x = e.getX(i) / width * W
                                joy = ((x - 48f) / 48f).coerceIn(-1f, 1f)
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (e.getPointerId(e.actionIndex) == joyId) {
                        joyId = -1
                        joy = 0f
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    joyId = -1
                    joy = 0f
                }
            }
            return true
        }
    }
}
