from pathlib import Path

path = Path("app/src/main/java/com/brannenservices/fieldassistant/MainActivity.kt")
s = path.read_text()

def replace_once(old, new, label):
    global s
    n = s.count(old)
    if n != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {n}")
    s = s.replace(old, new, 1)

def replace_section(start, end, replacement, label):
    global s
    a = s.find(start)
    if a < 0:
        raise RuntimeError(f"{label}: start marker not found")
    b = s.find(end, a)
    if b < 0:
        raise RuntimeError(f"{label}: end marker not found")
    s = s[:a] + replacement + s[b:]

replace_once(
'''        private val ATTACK_DURATION = .34f
        private val ATTACK_ACTIVE_START = .28f
        private val ATTACK_ACTIVE_END = .70f
        private val NOODLE_LENGTH = 84f
        private val walkHandX = floatArrayOf(28f, 18f, 18f, 24f)
        private val walkHandY = floatArrayOf(48f, 62f, 62f, 54f)
''',
'''        private val PUNCH_ONE_DURATION = .24f
        private val PUNCH_TWO_DURATION = .32f
        private val PUNCH_CHAIN_WINDOW = .34f
''',
"combat constants"
)

replace_once(
'''        private val pix = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }
''',
'''        private val pix = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }
        private val jumpNoNoodle by lazy {
            BitmapFactory.decodeResource(resources, R.drawable.man_jump_nonoodle)
        }
''',
"jump resource"
)

replace_once(
'''        private var attack = 0f
        private var attackId = 0
        private var hurt = 0f
''',
'''        private var attack = 0f
        private var attackDuration = PUNCH_ONE_DURATION
        private var attackId = 0
        private var punchStep = 0
        private var punchChainT = 0f
        private var queuedSecond = false
        private var hurt = 0f
''',
"combat state"
)

replace_section(
"        private fun update(dt: Float) {\n",
"        private fun attackProgress(): Float {",
'''        private fun update(dt: Float) {
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
                if (attack > 0f && punchIsActive(progress) && b.hit != attackId) {
                    val fist = punchFistPoint(px, py, face, punchStep, progress)
                    val targetY = G - if (b.boss) 62f else 48f
                    val hitRadius = if (b.boss) 46f else 31f
                    val hitDistance = hypot(b.x - fist[0], targetY - fist[1])

                    if (hitDistance <= hitRadius) {
                        b.hp--
                        b.hit = attackId
                        b.flash = .1f
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

''',
"update block"
)

replace_section(
"        private fun attackProgress(): Float {\n",
"        private fun hazard(push: Int) {",
'''        private fun attackProgress(): Float {
            return if (attack <= 0f) 1f else
                (1f - attack / attackDuration).coerceIn(0f, 1f)
        }

        private fun currentWalkFrame(): Int {
            return ((t * 6f).toInt() % AnimatedRaster.manWalk.size).coerceAtLeast(0)
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

        private fun punchExtension(progress: Float): Float {
            return max(0f, sin(progress.coerceIn(0f, 1f) * Math.PI).toFloat())
        }

        private fun punchFistPoint(
            originX: Float,
            originY: Float,
            facing: Int,
            step: Int,
            progress: Float
        ): FloatArray {
            val ext = punchExtension(progress)
            return if (step == 2) {
                floatArrayOf(
                    originX + facing * (28f + 68f * ext),
                    originY - 58f + 9f * (1f - ext)
                )
            } else {
                floatArrayOf(
                    originX + facing * (28f + 54f * ext),
                    originY - 53f
                )
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
            return if (step == 2) {
                floatArrayOf(
                    originX - facing * 2f, originY - 62f,
                    originX + facing * (12f + 22f * ext), originY - 70f + 12f * ext,
                    originX + facing * (28f + 68f * ext), originY - 58f + 9f * (1f - ext)
                )
            } else {
                floatArrayOf(
                    originX + facing * 5f, originY - 56f,
                    originX + facing * (16f + 18f * ext), originY - 54f,
                    originX + facing * (28f + 54f * ext), originY - 53f
                )
            }
        }

''',
"attack helpers"
)

replace_section(
"        private fun man() {\n",
"        private fun bird(b: Bird) {",
'''        private fun man() {
            if (hurt > 0f && (hurt * 12f).toInt() % 2 == 0) return

            val x = px - cam
            val airborne = py < G - 1f
            val attacking = attack > 0f
            val moving = abs(joy) > .12f && !airborne && !attacking
            val walkFrame = currentWalkFrame()
            val bmp = when {
                airborne -> jumpNoNoodle
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

            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND

            p.color = Color.rgb(82, 43, 30)
            p.strokeWidth = 15f
            c.drawLine(shoulderX, shoulderY, elbowX, elbowY, p)
            c.drawLine(elbowX, elbowY, fistX, fistY, p)

            p.color = Color.rgb(226, 145, 91)
            p.strokeWidth = 10f
            c.drawLine(shoulderX, shoulderY, elbowX, elbowY, p)
            c.drawLine(elbowX, elbowY, fistX, fistY, p)

            p.style = Paint.Style.FILL
            col(Color.rgb(82, 43, 30))
            c.drawCircle(fistX, fistY, if (step == 2) 10f else 9f, p)
            col(Color.rgb(238, 158, 101))
            c.drawCircle(fistX, fistY, if (step == 2) 7.5f else 6.5f, p)

            r(
                fistX - 1.5f,
                fistY - 5f,
                fistX + 1.5f,
                fistY - 2f,
                Color.rgb(255, 192, 129)
            )
            r(
                fistX - 1.5f,
                fistY,
                fistX + 1.5f,
                fistY + 3f,
                Color.rgb(181, 103, 67)
            )

            if (step == 2 && punchExtension(progress) > .72f) {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.color = Color.argb(180, 255, 228, 145)
                p.strokeWidth = 3f
                c.drawLine(
                    fistX - facing * 27f, fistY - 14f,
                    fistX - facing * 10f, fistY - 5f,
                    p
                )
                p.style = Paint.Style.FILL
                p.strokeCap = Paint.Cap.BUTT
            }
        }

''',
"player render"
)

replace_once(
'''            text("WHACK", 610f, 325f, 8f, Color.WHITE, true)
''',
'''            text("PUNCH", 610f, 325f, 8f, Color.WHITE, true)
''',
"button label"
)

replace_once(
'''            attack = 0f
            attackId = 0
            hurt = 0f
''',
'''            attack = 0f
            attackDuration = PUNCH_ONE_DURATION
            attackId = 0
            punchStep = 0
            punchChainT = 0f
            queuedSecond = false
            hurt = 0f
''',
"reset combat"
)

replace_once(
'''                    } else if (x > 585f && y > 285f && attack <= 0f) {
                        attack = ATTACK_DURATION
                        attackId++
                    } else if (x > 525f && y > 285f && py >= G - 1f) {
                        vy = -330f
''',
'''                    } else if (x > 585f && y > 285f) {
                        handlePunchPress()
                    } else if (x in 525f..585f && y > 285f && py >= G - 1f) {
                        vy = -330f
''',
"touch controls"
)

path.write_text(s)
print("Applied build 113 fist-combo combat pass")
