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
            var dir: Int = -1,
            var animT: Float = 0f,
            var seen: Boolean = false,
            var enterT: Float = 0f,
            var attackT: Float = 0f,
            var attackHit: Boolean = false
        )

        data class Beer(val x: Float, var taken: Boolean = false)

        private val W = 640
        private val H = 360
        private val G = 278f
        private val WORLD = 3000f

        private val PUNCH_ONE_DURATION = .24f
        private val PUNCH_TWO_DURATION = .32f
        private val PUNCH_CHAIN_WINDOW = .34f

        private val frame = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        private val c = Canvas(frame)
        private val p = Paint().apply { isAntiAlias = false }
        private val pix = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }
        private val jumpNoNoodle by lazy {
            BitmapFactory.decodeResource(resources, R.drawable.man_jump_nonoodle)
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
        private var attackDuration = PUNCH_ONE_DURATION
        private var attackId = 0
        private var punchStep = 0
        private var punchChainT = 0f
        private var queuedSecond = false
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

            val wasAttacking = attack > 0f
            attack = max(0f, attack - dt)
            punchChainT = max(0f, punchChainT - dt)
            hurt = max(0f, hurt - dt)
            comboT = max(0f, comboT - dt)
            msgT = max(0f, msgT - dt)
            if (comboT <= 0f) combo = 0

            if (wasAttacking && attack <= 0f) {
                when (punchStep) {
                    1 -> {
                        if (queuedSecond) {
                            startPunch(2)
                        } else {
                            punchChainT = PUNCH_CHAIN_WINDOW
                        }
                    }
                    2 -> {
                        punchStep = 0
                        punchChainT = 0f
                        queuedSecond = false
                    }
                }
            }
            if (attack <= 0f && punchStep == 1 && punchChainT <= 0f && !queuedSecond) {
                punchStep = 0
            }

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
                val birdAttackDuration = if (b.boss) .62f else .52f
                val oldBirdAttack = b.attackT
                b.attackT = max(0f, b.attackT - dt)
                if (oldBirdAttack > 0f && b.attackT <= 0f) {
                    b.attackHit = false
                }
                if (b.hp <= 0) return@forEach

                val screenX = b.x - cam
                if (!b.seen && screenX in -50f..690f) {
                    b.seen = true
                    b.animT = 0f
                    b.enterT = 0f
                }
                if (b.seen) {
                    b.animT += dt
                    b.enterT += dt
                }

                var dx = px - b.x
                b.dir = if (dx > 0f) 1 else -1
                val aggroRange = if (b.boss) 390f else 260f
                val strikeRange = if (b.boss) 78f else 61f

                if (b.attackT <= 0f && abs(dx) <= strikeRange && py > G - 55f && hurt <= 0f) {
                    b.attackT = birdAttackDuration
                    b.attackHit = false
                } else if (b.attackT <= 0f && abs(dx) < aggroRange) {
                    b.x += b.dir * (if (b.boss) 78f else 55f) * dt
                    dx = px - b.x
                }

                if (b.attackT > 0f) {
                    val birdAttackProgress =
                        (1f - b.attackT / birdAttackDuration).coerceIn(0f, 1f)
                    if (!b.attackHit && birdAttackProgress in .52f..72f) {
                        b.attackHit = true
                        val contactRange = if (b.boss) 75f else 55f
                        if (abs(b.x - px) <= contactRange && py > G - 55f && hurt <= 0f) {
                            hp = max(0, hp - (if (b.boss) 18 else 10))
                            hurt = .7f
                            px += if (dx > 0f) -55f else 55f
                            say(if (b.boss) "ALPHA PECK!" else "FLAMINGO PECK!")
                            if (hp <= 0) dead = true
                        }
                    }
                }

                val progress = attackProgress()
                if (attack > 0f && punchIsActive(progress) && b.hit != attackId) {
                    val fist = punchFistPoint(px, py, face, punchStep, progress)
                    val targetY = G - if (b.boss) 62f else 48f
                    val hitRadius = if (b.boss) 46f else 31f
                    val hitDistance = hypot(b.x - fist[0], targetY - fist[1])

                    if (hitDistance <= hitRadius) {
                        b.hp--
                        b.hit = attackId
                        b.flash = .14f
                        b.attackT = 0f
                        b.attackHit = false
                        val knockback = if (punchStep == 2) {
                            if (b.boss) 28f else 52f
                        } else {
                            if (b.boss) 16f else 32f
                        }
                        b.x += face * knockback
                        combo++
                        comboT = 1.3f
                        score += (if (b.boss) 250 else 100) * (1 + (combo - 1) / 3)
                        say(if (punchStep == 2) "WHAM!" else "POW!")
                        if (b.hp <= 0) score += if (b.boss) 1800 else 300
                    }
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

        private fun attackProgress(): Float {
            return if (attack <= 0f) 1f else
                (1f - attack / attackDuration).coerceIn(0f, 1f)
        }

        private fun currentWalkFrame(): Int {
            return ((t * 6f).toInt() % AnimatedRaster.manWalk.size).coerceAtLeast(0)
        }

        private fun smoothStep(v: Float): Float {
            val x = v.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }

        private fun startPunch(step: Int) {
            punchStep = step
            attackDuration = if (step == 2) PUNCH_TWO_DURATION else PUNCH_ONE_DURATION
            attack = attackDuration
            attackId++
            if (step == 2) {
                queuedSecond = false
                punchChainT = 0f
            }
        }

        private fun handlePunchPress() {
            if (attack > 0f) {
                if (punchStep == 1 && attackProgress() >= .22f) {
                    queuedSecond = true
                }
                return
            }
            if (punchStep == 1 && punchChainT > 0f) {
                startPunch(2)
            } else {
                queuedSecond = false
                startPunch(1)
            }
        }

        private fun punchIsActive(progress: Float): Boolean {
            return when (punchStep) {
                1 -> progress in .28f..68f
                2 -> progress in .24f..72f
                else -> false
            }
        }

        private fun mix(a: Float, b: Float, amount: Float): Float {
            return a + (b - a) * amount.coerceIn(0f, 1f)
        }

        private fun punchExtension(progress: Float): Float {
            val q = progress.coerceIn(0f, 1f)
            return when {
                q < .16f -> 0f
                q < .50f -> smoothStep((q - .16f) / .34f)
                else -> 1f - smoothStep((q - .50f) / .50f)
            }
        }

        private fun punchArmPoints(
            originX: Float,
            originY: Float,
            facing: Int,
            step: Int,
            progress: Float
        ): FloatArray {
            val ext = punchExtension(progress)
            val shoulderX = originX + facing * if (step == 2) 15f else 18f
            val shoulderY = originY - if (step == 2) 61f else 58f

            return if (step == 2) {
                val elbowX = originX + facing * mix(27f, 41f, ext)
                val elbowY = originY + mix(-72f, -63f, ext)
                val fistX = originX + facing * mix(20f, 65f, ext)
                val fistY = originY + mix(-55f, -58f, ext)
                floatArrayOf(shoulderX, shoulderY, elbowX, elbowY, fistX, fistY)
            } else {
                val elbowX = originX + facing * mix(25f, 39f, ext)
                val elbowY = originY + mix(-45f, -57f, ext)
                val fistX = originX + facing * mix(33f, 61f, ext)
                val fistY = originY + mix(-42f, -55f, ext)
                floatArrayOf(shoulderX, shoulderY, elbowX, elbowY, fistX, fistY)
            }
        }

        private fun punchFistPoint(
            originX: Float,
            originY: Float,
            facing: Int,
            step: Int,
            progress: Float
        ): FloatArray {
            val pts = punchArmPoints(originX, originY, facing, step, progress)
            return floatArrayOf(pts[4], pts[5])
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
            val attacking = attack > 0f
            val moving = abs(joy) > .12f && !airborne && !attacking
            val walkFrame = currentWalkFrame()
            val bmp = when {
                airborne -> jumpNoNoodle
                attacking -> AnimatedRaster.manWalk[if (punchStep == 2) 2 else 1]
                moving -> AnimatedRaster.manWalk[walkFrame]
                else -> SpriteArt.man
            }

            val bob = if (!moving && !airborne && !attacking) sin(t * 2.4f) * .4f else 0f
            val bodyH = if (airborne) 108f else 112f
            val bodyW = bodyH * bmp.width.toFloat() / bmp.height.toFloat()
            val punchLean = if (attacking) {
                face * punchExtension(attackProgress()) * if (punchStep == 2) 5f else 2.5f
            } else 0f
            val dst = RectF(
                x - bodyW / 2f + punchLean,
                py - bodyH + bob,
                x + bodyW / 2f + punchLean,
                py + bob
            )

            c.save()
            c.scale(face.toFloat(), 1f, x, py)
            if (airborne) c.rotate((-vy / 330f).coerceIn(-1f, 1f) * 1.5f, x, py - 50f)
            c.drawBitmap(bmp, null, dst, pix)
            c.restore()

            if (attacking) {
                drawPunch(x, py + bob, face, punchStep, attackProgress())
            }
        }

        private fun drawPunch(
            originX: Float,
            originY: Float,
            facing: Int,
            step: Int,
            progress: Float
        ) {
            val pts = punchArmPoints(originX, originY, facing, step, progress)
            val shoulderX = pts[0]
            val shoulderY = pts[1]
            val elbowX = pts[2]
            val elbowY = pts[3]
            val fistX = pts[4]
            val fistY = pts[5]
            val ext = punchExtension(progress)

            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND

            p.color = Color.rgb(82, 43, 30)
            p.strokeWidth = 12f
            c.drawLine(shoulderX, shoulderY, elbowX, elbowY, p)
            c.drawLine(elbowX, elbowY, fistX, fistY, p)

            p.color = Color.rgb(226, 145, 91)
            p.strokeWidth = 8f
            c.drawLine(shoulderX, shoulderY, elbowX, elbowY, p)
            c.drawLine(elbowX, elbowY, fistX, fistY, p)

            p.style = Paint.Style.FILL
            col(Color.rgb(82, 43, 30))
            c.drawCircle(fistX, fistY, if (step == 2) 7.5f else 7f, p)
            col(Color.rgb(238, 158, 101))
            c.drawCircle(fistX, fistY, if (step == 2) 5.5f else 5f, p)

            if (ext > .82f) {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.color = Color.argb(
                    ((ext - .82f) / .18f * 150f).toInt().coerceIn(0, 150),
                    255, 228, 145
                )
                p.strokeWidth = 2f
                val trail = if (step == 2) 18f else 13f
                c.drawLine(
                    fistX - facing * trail, fistY - 4f,
                    fistX - facing * 5f, fistY - 1f,
                    p
                )
            }

            p.style = Paint.Style.FILL
            p.strokeCap = Paint.Cap.BUTT
        }

        private fun bird(b: Bird) {
            val rawX = b.x - cam
            if (rawX !in -120f..760f) return

            val scale = if (b.boss) 1.48f else 1f
            val moving =
                b.attackT <= 0f && abs(px - b.x) < (if (b.boss) 390f else 260f)
            val introDuration = .24f
            val entryProgress = (b.enterT / introDuration).coerceIn(0f, 1f)

            val frameIndex = when {
                !b.seen -> 0
                b.enterT < introDuration ->
                    ((entryProgress * AnimatedRaster.flWalk.size).toInt())
                        .coerceIn(0, AnimatedRaster.flWalk.lastIndex)
                b.attackT > 0f -> {
                    val duration = if (b.boss) .62f else .52f
                    val attackProgress =
                        (1f - b.attackT / duration).coerceIn(0f, 1f)
                    when {
                        attackProgress < .28f -> 1
                        attackProgress < .72f -> 2
                        else -> 0
                    }
                }
                moving -> {
                    val frameRate = if (b.boss) 6f else 6.8f
                    ((b.animT * frameRate).toInt() % AnimatedRaster.flWalk.size)
                        .coerceAtLeast(0)
                }
                else -> ((b.animT * 1.25f).toInt() % 2).coerceAtLeast(0)
            }
            val bmp = AnimatedRaster.flWalk[frameIndex]

            val entryOffset = if (b.seen && b.enterT < introDuration) {
                -b.dir * (1f - smoothStep(entryProgress)) * 7f
            } else 0f

            val attackDuration = if (b.boss) .62f else .52f
            val attackProgress = if (b.attackT > 0f) {
                (1f - b.attackT / attackDuration).coerceIn(0f, 1f)
            } else 0f
            val attackDrive = if (b.attackT > 0f) {
                sin(attackProgress * Math.PI).toFloat().coerceAtLeast(0f)
            } else 0f

            val hitKick = if (b.flash > 0f) -b.dir * 4f else 0f
            val lunge = b.dir * attackDrive * if (b.boss) 19f else 14f
            val x = rawX + entryOffset + lunge + hitKick
            val bob = when {
                b.attackT > 0f -> attackDrive * 3f
                moving -> sin(b.animT * 13f) * .7f
                else -> sin(b.animT * 2.1f) * .35f
            }

            val h = 96f * scale
            val w = h * bmp.width.toFloat() / bmp.height.toFloat()
            val dst = RectF(x - w / 2f, G - h + bob, x + w / 2f, G + bob)

            c.save()
            c.scale(b.dir.toFloat(), 1f, x, G)
            if (b.attackT > 0f) {
                c.rotate(-b.dir * attackDrive * 7f, x, G - h * .45f)
                c.scale(
                    1f + attackDrive * .045f,
                    1f - attackDrive * .03f,
                    x,
                    G
                )
            }
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
            text("PUNCH", 610f, 325f, 8f, Color.WHITE, true)
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
            attackDuration = PUNCH_ONE_DURATION
            attackId = 0
            punchStep = 0
            punchChainT = 0f
            queuedSecond = false
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
                        handlePunchPress()
                    } else if (x in 525f..585f && y > 285f && py >= G - 1f) {
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
