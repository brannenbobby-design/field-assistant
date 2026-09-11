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
'''            var animT: Float = 0f,
            var seen: Boolean = false,
            var enterT: Float = 0f
''',
'''            var animT: Float = 0f,
            var seen: Boolean = false,
            var enterT: Float = 0f,
            var attackT: Float = 0f,
            var attackHit: Boolean = false
''',
"bird combat state"
)

replace_section(
'''            birds.forEach { b ->
''',
'''            beers.forEach {
''',
'''            birds.forEach { b ->
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

''',
"bird update"
)

replace_section(
'''        private fun punchExtension(progress: Float): Float {
''',
'''        private fun hazard(push: Int) {
''',
'''        private fun mix(a: Float, b: Float, amount: Float): Float {
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

''',
"punch geometry"
)

replace_once(
'''            val bmp = when {
                airborne -> jumpNoNoodle
                moving -> AnimatedRaster.manWalk[walkFrame]
                else -> SpriteArt.man
            }
''',
'''            val bmp = when {
                airborne -> jumpNoNoodle
                attacking -> AnimatedRaster.manWalk[if (punchStep == 2) 2 else 1]
                moving -> AnimatedRaster.manWalk[walkFrame]
                else -> SpriteArt.man
            }
''',
"punch stance sprite"
)

replace_section(
'''        private fun drawPunch(
''',
'''        private fun bird(b: Bird) {
''',
'''        private fun drawPunch(
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

''',
"punch drawing"
)

replace_section(
'''        private fun bird(b: Bird) {
''',
'''        private fun beer(q: Beer) {
''',
'''        private fun bird(b: Bird) {
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

''',
"flamingo rendering"
)

path.write_text(s)
