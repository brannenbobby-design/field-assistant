package com.brannenservices.fieldassistant

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    private data class Flamingo(
        var x: Float,
        var hp: Int,
        val maxHp: Int,
        val boss: Boolean = false,
        var lastHitAttack: Int = -1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(FloridaGameView())
    }

    inner class FloridaGameView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pointers = mutableMapOf<Int, Control>()
        private val worldWidth = 5200f

        private var playerX = 220f
        private var playerY = 0f
        private var velocityY = 0f
        private var groundY = 0f
        private var cameraX = 0f
        private var facing = 1

        private var score = 0
        private var health = 100
        private var started = false
        private var gameOver = false
        private var levelWon = false

        private var attackTimer = 0f
        private var attackNumber = 0
        private var hurtCooldown = 0f
        private var lastFrame = System.nanoTime()

        private val flamingos = mutableListOf<Flamingo>()

        private enum class Control { LEFT, RIGHT, JUMP, WHACK }

        init {
            resetGame()
        }

        override fun onDraw(canvas: Canvas) {
            val now = System.nanoTime()
            val dt = ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.04f)
            lastFrame = now

            groundY = height * 0.77f
            if (playerY == 0f) playerY = groundY

            if (started && !gameOver && !levelWon) updateGame(dt)

            drawWorld(canvas)
            drawHud(canvas)
            drawControls(canvas)
            drawOverlay(canvas)

            postInvalidateOnAnimation()
        }

        private fun resetGame() {
            playerX = 220f
            playerY = 0f
            velocityY = 0f
            cameraX = 0f
            facing = 1
            score = 0
            health = 100
            gameOver = false
            levelWon = false
            attackTimer = 0f
            attackNumber = 0
            hurtCooldown = 0f
            pointers.clear()

            flamingos.clear()
            flamingos += Flamingo(980f, 2, 2)
            flamingos += Flamingo(1480f, 2, 2)
            flamingos += Flamingo(2100f, 3, 3)
            flamingos += Flamingo(2780f, 3, 3)
            flamingos += Flamingo(3420f, 3, 3)
            flamingos += Flamingo(4300f, 9, 9, boss = true)
        }

        private fun updateGame(dt: Float) {
            val movingLeft = pointers.values.any { it == Control.LEFT }
            val movingRight = pointers.values.any { it == Control.RIGHT }

            var move = 0f
            if (movingLeft) move -= 1f
            if (movingRight) move += 1f

            if (move != 0f) {
                facing = if (move > 0f) 1 else -1
                playerX = (playerX + move * 390f * dt).coerceIn(55f, worldWidth - 90f)
            }

            velocityY += 1550f * dt
            playerY += velocityY * dt
            if (playerY > groundY) {
                playerY = groundY
                velocityY = 0f
            }

            attackTimer = max(0f, attackTimer - dt)
            hurtCooldown = max(0f, hurtCooldown - dt)

            val playerScreenTarget = width * 0.37f
            val desiredCamera = playerX - playerScreenTarget
            cameraX += (desiredCamera - cameraX) * min(1f, dt * 7f)
            cameraX = cameraX.coerceIn(0f, max(0f, worldWidth - width))

            for (enemy in flamingos) {
                if (enemy.hp <= 0) continue

                val distance = playerX - enemy.x
                val chaseRange = if (enemy.boss) 760f else 520f
                if (abs(distance) < chaseRange) {
                    val direction = if (distance > 0f) 1f else -1f
                    val speed = if (enemy.boss) 105f else 82f
                    enemy.x += direction * speed * dt
                }

                if (attackTimer > 0f && enemy.lastHitAttack != attackNumber) {
                    val signedDistance = (enemy.x - playerX) * facing
                    val verticalOk = playerY >= groundY - 165f
                    if (signedDistance in 20f..190f && verticalOk) {
                        enemy.hp--
                        enemy.lastHitAttack = attackNumber
                        enemy.x += facing * 70f
                        score += if (enemy.boss) 250 else 100
                        if (enemy.hp <= 0) score += if (enemy.boss) 1500 else 250
                    }
                }

                val collisionDistance = if (enemy.boss) 92f else 62f
                if (abs(enemy.x - playerX) < collisionDistance && playerY > groundY - 95f && hurtCooldown <= 0f) {
                    health = max(0, health - if (enemy.boss) 18 else 10)
                    hurtCooldown = 0.75f
                    playerX = (playerX - facing * 90f).coerceIn(55f, worldWidth - 90f)
                    if (health <= 0) gameOver = true
                }
            }

            val bossDefeated = flamingos.lastOrNull()?.hp == 0
            if (bossDefeated && playerX > 4650f) levelWon = true
        }

        private fun drawWorld(canvas: Canvas) {
            val sky = LinearGradient(
                0f,
                0f,
                0f,
                groundY,
                Color.rgb(42, 174, 224),
                Color.rgb(136, 224, 242),
                Shader.TileMode.CLAMP
            )
            paint.shader = sky
            canvas.drawRect(0f, 0f, width.toFloat(), groundY, paint)
            paint.shader = null

            paint.color = Color.rgb(255, 235, 140)
            canvas.drawCircle(width * 0.84f, height * 0.16f, height * 0.075f, paint)

            drawParallaxPalms(canvas)
            drawFence(canvas)
            drawPool(canvas)

            paint.color = Color.rgb(232, 208, 157)
            canvas.drawRect(0f, groundY, width.toFloat(), groundY + height * 0.07f, paint)
            paint.color = Color.rgb(36, 146, 78)
            canvas.drawRect(0f, groundY + height * 0.07f, width.toFloat(), height.toFloat(), paint)

            for (enemy in flamingos) {
                if (enemy.hp > 0) drawFlamingo(canvas, enemy)
            }

            drawPlayer(canvas)
            drawFinishSign(canvas)
        }

        private fun drawParallaxPalms(canvas: Canvas) {
            val palmPositions = floatArrayOf(500f, 1750f, 3150f, 4700f)
            for (worldX in palmPositions) {
                val x = worldX - cameraX * 0.45f
                if (x < -180f || x > width + 180f) continue
                paint.color = Color.rgb(124, 81, 43)
                canvas.drawRect(x - 11f, groundY - 265f, x + 11f, groundY, paint)
                paint.strokeWidth = 20f
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = Color.rgb(22, 126, 63)
                for (i in -2..2) {
                    canvas.drawLine(x, groundY - 255f, x + i * 48f, groundY - 300f + abs(i) * 15f, paint)
                }
            }
        }

        private fun drawFence(canvas: Canvas) {
            paint.color = Color.rgb(238, 238, 220)
            val spacing = 95f
            var x = -((cameraX * 0.78f) % spacing) - spacing
            while (x < width + spacing) {
                canvas.drawRect(x, groundY - 145f, x + 72f, groundY - 15f, paint)
                canvas.drawPath(android.graphics.Path().apply {
                    moveTo(x, groundY - 145f)
                    lineTo(x + 36f, groundY - 182f)
                    lineTo(x + 72f, groundY - 145f)
                    close()
                }, paint)
                x += spacing
            }
        }

        private fun drawPool(canvas: Canvas) {
            val poolWorldX = 2550f
            val x = poolWorldX - cameraX
            if (x > -700f && x < width + 700f) {
                paint.color = Color.rgb(53, 184, 224)
                canvas.drawOval(x - 480f, groundY + 18f, x + 480f, groundY + 105f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 14f
                paint.color = Color.WHITE
                canvas.drawOval(x - 480f, groundY + 18f, x + 480f, groundY + 105f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        private fun drawPlayer(canvas: Canvas) {
            val x = playerX - cameraX
            val y = playerY
            val flash = hurtCooldown > 0f && ((hurtCooldown * 12).toInt() % 2 == 0)
            if (flash) return

            // Legs and flip-flops
            paint.color = Color.rgb(242, 181, 133)
            canvas.drawRect(x - 23f, y - 3f, x - 8f, y + 42f, paint)
            canvas.drawRect(x + 8f, y - 3f, x + 23f, y + 42f, paint)
            paint.color = Color.rgb(35, 35, 35)
            canvas.drawRect(x - 31f, y + 39f, x - 4f, y + 46f, paint)
            canvas.drawRect(x + 4f, y + 39f, x + 31f, y + 46f, paint)

            // Shorts and tank top
            paint.color = Color.rgb(42, 71, 148)
            canvas.drawRect(x - 34f, y - 44f, x + 34f, y + 7f, paint)
            paint.color = Color.WHITE
            canvas.drawRect(x - 31f, y - 110f, x + 31f, y - 44f, paint)

            // Arms
            paint.color = Color.rgb(242, 181, 133)
            canvas.drawRect(x - 44f, y - 102f, x - 29f, y - 48f, paint)
            canvas.drawRect(x + 29f, y - 102f, x + 44f, y - 48f, paint)

            // Head and mullet
            canvas.drawCircle(x, y - 142f, 34f, paint)
            paint.color = Color.rgb(91, 50, 24)
            canvas.drawRect(x - 37f, y - 174f, x + 25f, y - 158f, paint)
            canvas.drawRect(x - 39f, y - 161f, x - 25f, y - 108f, paint)

            // Sunglasses
            paint.color = Color.BLACK
            canvas.drawRect(x - 25f, y - 150f, x - 4f, y - 141f, paint)
            canvas.drawRect(x + 4f, y - 150f, x + 25f, y - 141f, paint)

            // Pool noodle
            paint.strokeWidth = 20f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(255, 47, 165)
            val attackReach = if (attackTimer > 0f) 165f else 72f
            val startX = x + facing * 25f
            val endX = x + facing * attackReach
            canvas.drawLine(startX, y - 92f, endX, y - if (attackTimer > 0f) 105f else 128f, paint)
        }

        private fun drawFlamingo(canvas: Canvas, enemy: Flamingo) {
            val x = enemy.x - cameraX
            if (x < -180f || x > width + 180f) return

            val scale = if (enemy.boss) 1.55f else 1f
            val bodyY = groundY - 92f * scale

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 13f * scale
            paint.color = if (enemy.boss) Color.rgb(232, 36, 95) else Color.rgb(255, 92, 147)
            canvas.drawOval(
                x - 38f * scale,
                bodyY - 27f * scale,
                x + 38f * scale,
                bodyY + 27f * scale,
                paint
            )
            canvas.drawLine(x + 18f * scale, bodyY - 18f * scale, x + 28f * scale, bodyY - 92f * scale, paint)
            canvas.drawCircle(x + 46f * scale, bodyY - 111f * scale, 22f * scale, paint)
            canvas.drawLine(x + 65f * scale, bodyY - 112f * scale, x + 95f * scale, bodyY - 124f * scale, paint)
            canvas.drawLine(x - 12f * scale, bodyY + 20f * scale, x - 22f * scale, groundY + 28f, paint)
            canvas.drawLine(x + 12f * scale, bodyY + 20f * scale, x + 28f * scale, groundY + 28f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.RED
            canvas.drawCircle(x + 50f * scale, bodyY - 116f * scale, 5f * scale, paint)

            if (enemy.boss) {
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 24f
                paint.color = Color.rgb(255, 244, 80)
                canvas.drawText("ALPHA FLAMINGO", x, bodyY - 170f, paint)
                val barW = 170f
                paint.color = Color.argb(180, 0, 0, 0)
                canvas.drawRect(x - barW / 2, bodyY - 158f, x + barW / 2, bodyY - 145f, paint)
                paint.color = Color.rgb(232, 48, 48)
                canvas.drawRect(
                    x - barW / 2,
                    bodyY - 158f,
                    x - barW / 2 + barW * (enemy.hp.toFloat() / enemy.maxHp),
                    bodyY - 145f,
                    paint
                )
                paint.textAlign = Paint.Align.LEFT
            }
        }

        private fun drawFinishSign(canvas: Canvas) {
            val x = 4860f - cameraX
            if (x < -150f || x > width + 150f) return
            paint.color = Color.rgb(122, 80, 41)
            canvas.drawRect(x - 8f, groundY - 185f, x + 8f, groundY, paint)
            paint.color = Color.rgb(245, 224, 149)
            canvas.drawRect(x - 125f, groundY - 250f, x + 125f, groundY - 175f, paint)
            paint.color = Color.BLACK
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 26f
            canvas.drawText("BACKYARD EXIT", x, groundY - 205f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun drawHud(canvas: Canvas) {
            val pad = height * 0.035f
            val hudHeight = height * 0.095f
            paint.color = Color.argb(150, 0, 0, 0)
            canvas.drawRoundRect(pad, pad, width * 0.35f, pad + hudHeight, 18f, 18f, paint)

            paint.typeface = Typeface.MONOSPACE
            paint.textSize = height * 0.037f
            paint.color = Color.WHITE
            canvas.drawText("HOLD MY BEER", pad * 1.7f, pad + hudHeight * 0.43f, paint)

            val meterLeft = pad * 1.7f
            val meterTop = pad + hudHeight * 0.57f
            val meterRight = width * 0.32f
            val meterBottom = pad + hudHeight * 0.82f
            paint.color = Color.rgb(65, 65, 65)
            canvas.drawRect(meterLeft, meterTop, meterRight, meterBottom, paint)
            paint.color = if (health > 35) Color.rgb(245, 194, 48) else Color.rgb(230, 55, 48)
            canvas.drawRect(
                meterLeft,
                meterTop,
                meterLeft + (meterRight - meterLeft) * health / 100f,
                meterBottom,
                paint
            )

            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = height * 0.052f
            paint.color = Color.rgb(255, 242, 65)
            canvas.drawText("FLORIDA MAN", width / 2f, pad + height * 0.045f, paint)
            paint.textSize = height * 0.025f
            paint.color = Color.WHITE
            canvas.drawText("POOL NOODLE PANIC", width / 2f, pad + height * 0.076f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = height * 0.038f
            canvas.drawText("SCORE  $score", width - pad * 1.7f, pad + height * 0.05f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun drawControls(canvas: Canvas) {
            val r = min(height * 0.085f, 78f)
            val y = height - r - height * 0.035f
            val leftX = r + height * 0.035f
            val rightX = leftX + r * 2.25f
            val whackX = width - r - height * 0.035f
            val jumpX = whackX - r * 2.25f

            drawButton(canvas, leftX, y, r, "◀")
            drawButton(canvas, rightX, y, r, "▶")
            drawButton(canvas, jumpX, y, r, "JUMP", 0.36f)
            drawButton(canvas, whackX, y, r, "WHACK", 0.31f)
        }

        private fun drawButton(canvas: Canvas, x: Float, y: Float, r: Float, label: String, textScale: Float = 0.55f) {
            paint.color = Color.argb(125, 0, 0, 0)
            canvas.drawCircle(x, y, r, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.argb(190, 255, 255, 255)
            canvas.drawCircle(x, y, r, paint)
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = r * textScale
            paint.color = Color.WHITE
            canvas.drawText(label, x, y + paint.textSize * 0.34f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun drawOverlay(canvas: Canvas) {
            if (started && !gameOver && !levelWon) return

            paint.color = Color.argb(190, 0, 0, 0)
            canvas.drawRect(0f, height * 0.26f, width.toFloat(), height * 0.68f, paint)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = Color.WHITE

            when {
                !started -> {
                    paint.textSize = height * 0.064f
                    canvas.drawText("TAP TO START THE BAD DECISIONS", width / 2f, height * 0.44f, paint)
                    paint.textSize = height * 0.032f
                    paint.color = Color.rgb(255, 238, 82)
                    canvas.drawText("GET TO THE BACKYARD EXIT. TRY NOT TO GET MURDERED BY LAWN ORNAMENTS.", width / 2f, height * 0.53f, paint)
                }
                gameOver -> {
                    paint.textSize = height * 0.075f
                    paint.color = Color.rgb(255, 90, 70)
                    canvas.drawText("WELL, THAT ESCALATED.", width / 2f, height * 0.44f, paint)
                    paint.textSize = height * 0.038f
                    paint.color = Color.WHITE
                    canvas.drawText("TAP TO TRY ANOTHER BAD IDEA", width / 2f, height * 0.54f, paint)
                }
                levelWon -> {
                    paint.textSize = height * 0.073f
                    paint.color = Color.rgb(255, 238, 82)
                    canvas.drawText("BACKYARD SURVIVED. SOMEHOW.", width / 2f, height * 0.43f, paint)
                    paint.textSize = height * 0.038f
                    paint.color = Color.WHITE
                    canvas.drawText("POOL NOODLE PANIC CLEAR  •  SCORE $score", width / 2f, height * 0.53f, paint)
                    canvas.drawText("TAP TO RUN IT AGAIN", width / 2f, height * 0.60f, paint)
                }
            }
            paint.textAlign = Paint.Align.LEFT
        }

        private fun jump() {
            if (playerY >= groundY - 4f) velocityY = -720f
        }

        private fun attack() {
            if (attackTimer <= 0f) {
                attackNumber++
                attackTimer = 0.20f
            }
        }

        private fun controlAt(x: Float, y: Float): Control? {
            if (y < height * 0.68f) return null
            return when {
                x < width * 0.12f -> Control.LEFT
                x < width * 0.28f -> Control.RIGHT
                x > width * 0.86f -> Control.WHACK
                x > width * 0.70f -> Control.JUMP
                else -> null
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val index = event.actionIndex
                    val id = event.getPointerId(index)

                    if (!started) {
                        started = true
                        lastFrame = System.nanoTime()
                        return true
                    }

                    if (gameOver || levelWon) {
                        resetGame()
                        started = true
                        lastFrame = System.nanoTime()
                        return true
                    }

                    val control = controlAt(event.getX(index), event.getY(index))
                    if (control != null) {
                        pointers[id] = control
                        if (control == Control.JUMP) jump()
                        if (control == Control.WHACK) attack()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val id = event.getPointerId(event.actionIndex)
                    pointers.remove(id)
                }

                MotionEvent.ACTION_CANCEL -> pointers.clear()
            }
            return true
        }
    }
}
