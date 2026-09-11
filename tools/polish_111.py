from pathlib import Path

path = Path("app/src/main/java/com/brannenservices/fieldassistant/MainActivity.kt")
s = path.read_text()

def section(start, end, replacement):
    global s
    a = s.index(start)
    b = s.index(end, a)
    s = s[:a] + replacement + s[b:]

# Per-flamingo entrance/animation state.
section(
    "        data class Bird(\n",
    "        data class Beer",
    """        data class Bird(
            var x: Float,
            var hp: Int,
            val max: Int,
            val boss: Boolean = false,
            var hit: Int = -1,
            var flash: Float = 0f,
            var dir: Int = -1,
            var animT: Float = 0f,
            var seen: Boolean = false,
            var enterT: Float = 0f
        )

"""
)

needle = "        private val WORLD = 3000f\n"
assert s.count(needle) == 1
s = s.replace(
    needle,
    needle + """
        private val ATTACK_DURATION = .34f
        private val ATTACK_ACTIVE_START = .28f
        private val ATTACK_ACTIVE_END = .70f
        private val NOODLE_LENGTH = 84f
        private val walkHandX = floatArrayOf(28f, 18f, 18f, 24f)
        private val walkHandY = floatArrayOf(48f, 62f, 62f, 54f)
""",
    1
)

section(
    "        private fun update(dt: Float) {\n",
    "        private fun hazard(push: Int) {",
    """        private fun update(dt: Float) {
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

                val dx = px - b.x
                b.dir = if (dx > 0f) 1 else -1

                if (abs(dx) < (if (b.boss) 390f else 260f)) {
                    b.x += b.dir * (if (b.boss) 78f else 55f) * dt
                }

                val progress = attackProgress()
                if (attack > 0f &&
                    progress in ATTACK_ACTIVE_START..ATTACK_ACTIVE_END &&
                    b.hit != attackId
                ) {
                    val anchor = playerHandAnchor()
                    val handX = px + face * anchor[0]
                    val handY = py - anchor[1]
                    val angle = attackNoodleAngle(progress, face)
                    val curve = noodleCurve(handX, handY, angle, attackNoodleBend(progress, face))
                    val midX = .25f * curve[0] + .5f * curve[2] + .25f * curve[4]
                    val midY = .25f * curve[1] + .5f * curve[3] + .25f * curve[5]
                    val targetY = G - if (b.boss) 62f else 48f
                    val hitRadius = if (b.boss) 43f else 30f
                    val hitDistance = min(
                        pointSegmentDistance(b.x, targetY, curve[0], curve[1], midX, midY),
                        pointSegmentDistance(b.x, targetY, midX, midY, curve[4], curve[5])
                    )

                    if (hitDistance <= hitRadius) {
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

                if (abs(b.x - px) < (if (b.boss) 48f else 31f) && py > G - 55f && hurt <= 0f) {
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

        private fun attackProgress(): Float {
            return if (attack <= 0f) 1f else (1f - attack / ATTACK_DURATION).coerceIn(0f, 1f)
        }

        private fun smoothStep(v: Float): Float {
            val x = v.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }

        private fun currentWalkFrame(): Int {
            return ((t * 6f).toInt() % AnimatedRaster.manWalk.size).coerceAtLeast(0)
        }

        private fun playerHandAnchor(): FloatArray {
            val airborne = py < G - 1f
            val moving = abs(joy) > .12f && !airborne
            return when {
                airborne -> floatArrayOf(-22f, 52f)
                moving -> {
                    val frameIndex = currentWalkFrame()
                    floatArrayOf(walkHandX[frameIndex], walkHandY[frameIndex])
                }
                else -> floatArrayOf(31f, 43f)
            }
        }

        private fun attackNoodleAngle(progress: Float, facing: Int): Float {
            val local = when {
                progress < .18f -> {
                    val q = smoothStep(progress / .18f)
                    -18f + (-66f + 18f) * q
                }
                progress < .72f -> {
                    val q = smoothStep((progress - .18f) / .54f)
                    -66f + (56f + 66f) * q
                }
                else -> {
                    val q = smoothStep((progress - .72f) / .28f)
                    56f + (-18f - 56f) * q
                }
            }
            return if (facing > 0) local else 180f - local
        }

        private fun attackNoodleBend(progress: Float, facing: Int): Float {
            val swing = ((progress - .18f) / .54f).coerceIn(0f, 1f)
            val flex = sin(swing * Math.PI).toFloat() * 9f
            return flex * if (facing > 0) 1f else -1f
        }

        private fun noodleCurve(handX: Float, handY: Float, angleDeg: Float, bend: Float): FloatArray {
            val angle = Math.toRadians(angleDeg.toDouble())
            val ux = cos(angle).toFloat()
            val uy = sin(angle).toFloat()
            val nx = -uy
            val ny = ux
            val endX = handX + ux * NOODLE_LENGTH
            val endY = handY + uy * NOODLE_LENGTH
            val controlX = handX + ux * (NOODLE_LENGTH * .52f) + nx * bend
            val controlY = handY + uy * (NOODLE_LENGTH * .52f) + ny * bend
            return floatArrayOf(handX, handY, controlX, controlY, endX, endY)
        }

        private fun pointSegmentDistance(qx: Float, qy: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
            val dx = bx - ax
            val dy = by - ay
            val lenSq = dx * dx + dy * dy
            if (lenSq <= .001f) return hypot(qx - ax, qy - ay)
            val u = (((qx - ax) * dx + (qy - ay) * dy) / lenSq).coerceIn(0f, 1f)
            val cx = ax + dx * u
            val cy = ay + dy * u
            return hypot(qx - cx, qy - cy)
        }

"""
)

section(
    "        private fun man() {\n",
    "        private fun bird(b: Bird) {",
    """        private fun man() {
            if (hurt > 0f && (hurt * 12f).toInt() % 2 == 0) return

            val x = px - cam
            val airborne = py < G - 1f
            val moving = abs(joy) > .12f && !airborne
            val walkFrame = currentWalkFrame()
            val bmp = when {
                airborne -> AnimatedRaster.manJump
                moving -> AnimatedRaster.manWalk[walkFrame]
                else -> SpriteArt.man
            }

            val bob = if (!moving && !airborne) sin(t * 2.4f) * .4f else 0f
            val bodyH = if (airborne) 108f else 112f
            val bodyW = bodyH * bmp.width.toFloat() / bmp.height.toFloat()
            val dst = RectF(x - bodyW / 2f, py - bodyH + bob, x + bodyW / 2f, py + bob)

            c.save()
            c.scale(face.toFloat(), 1f, x, py)
            if (airborne) c.rotate((-vy / 330f).coerceIn(-1f, 1f) * 1.5f, x, py - 50f)
            c.drawBitmap(bmp, null, dst, pix)
            c.restore()

            val drawOverlayNoodle = !airborne || attack > 0f
            if (drawOverlayNoodle) {
                val anchor = playerHandAnchor()
                val handX = x + face * anchor[0]
                val handY = py - anchor[1] + bob
                val progress = attackProgress()
                val deg = if (attack > 0f) attackNoodleAngle(progress, face) else if (face > 0) -18f else 198f
                val bend = if (attack > 0f) attackNoodleBend(progress, face) else if (face > 0) 2.5f else -2.5f
                drawNoodle(handX, handY, deg, bend)

                col(Color.rgb(226, 145, 91))
                c.drawCircle(handX, handY, 5.5f, p)
            }
        }

        private fun drawNoodle(handX: Float, handY: Float, angleDeg: Float, bend: Float) {
            val curve = noodleCurve(handX, handY, angleDeg, bend)
            val path = Path().apply {
                moveTo(curve[0], curve[1])
                quadTo(curve[2], curve[3], curve[4], curve[5])
            }

            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.color = Color.rgb(9, 70, 132)
            p.strokeWidth = 15f
            c.drawPath(path, p)

            p.color = Color.rgb(48, 174, 244)
            p.strokeWidth = 11f
            c.drawPath(path, p)

            val angle = Math.toRadians(angleDeg.toDouble())
            val nx = -sin(angle).toFloat()
            val ny = cos(angle).toFloat()
            val highlight = Path().apply {
                moveTo(curve[0] - nx * 2.1f, curve[1] - ny * 2.1f)
                quadTo(curve[2] - nx * 2.1f, curve[3] - ny * 2.1f, curve[4] - nx * 2.1f, curve[5] - ny * 2.1f)
            }
            p.color = Color.rgb(149, 224, 255)
            p.strokeWidth = 2.5f
            c.drawPath(highlight, p)

            p.style = Paint.Style.FILL
            p.strokeCap = Paint.Cap.BUTT
            p.strokeJoin = Paint.Join.MITER

            col(Color.rgb(8, 65, 124))
            c.drawCircle(curve[4], curve[5], 7f, p)
            col(Color.rgb(45, 171, 241))
            c.drawCircle(curve[4], curve[5], 5f, p)
            col(Color.rgb(159, 229, 255))
            c.drawCircle(curve[4] - nx * 1.5f, curve[5] - ny * 1.5f, 1.7f, p)
        }

"""
)

section(
    "        private fun bird(b: Bird) {\n",
    "        private fun beer(q: Beer) {",
    """        private fun bird(b: Bird) {
            val rawX = b.x - cam
            if (rawX !in -120f..760f) return

            val scale = if (b.boss) 1.48f else 1f
            val moving = abs(px - b.x) < (if (b.boss) 390f else 260f)
            val introDuration = .30f
            val entryProgress = (b.enterT / introDuration).coerceIn(0f, 1f)

            val bmp = when {
                !b.seen -> SpriteArt.flamingo
                b.enterT < .09f -> SpriteArt.flamingo
                b.enterT < .16f -> AnimatedRaster.flWalk[0]
                b.enterT < .23f -> AnimatedRaster.flWalk[1]
                b.enterT < introDuration -> AnimatedRaster.flWalk[2]
                moving -> {
                    val frameRate = if (b.boss) 5f else 5.5f
                    val frameIndex = (((b.animT - introDuration).coerceAtLeast(0f) * frameRate).toInt() % AnimatedRaster.flWalk.size).coerceAtLeast(0)
                    AnimatedRaster.flWalk[frameIndex]
                }
                else -> SpriteArt.flamingo
            }

            val entryOffset = if (b.seen && b.enterT < introDuration) {
                -b.dir * (1f - smoothStep(entryProgress)) * 10f
            } else 0f
            val x = rawX + entryOffset
            val bob = if (!moving && b.enterT >= introDuration) sin(b.animT * 2.1f) * .5f else 0f

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

"""
)

old_touch = """                    } else if (x > 585f && y > 285f) {
                        attack = .2f
                        attackId++
"""
new_touch = """                    } else if (x > 585f && y > 285f && attack <= 0f) {
                        attack = ATTACK_DURATION
                        attackId++
"""
assert s.count(old_touch) == 1
s = s.replace(old_touch, new_touch, 1)

path.write_text(s)
print("Applied build 111 noodle mechanics and flamingo entrance polish")
