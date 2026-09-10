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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    private data class Flamingo(
        var x: Float,
        var hp: Int,
        val maxHp: Int,
        val boss: Boolean = false,
        var lastHitAttack: Int = -1,
        var hitFlash: Float = 0f
    )

    private enum class PickupType { COLD_ONE }

    private data class Pickup(
        val x: Float,
        val type: PickupType,
        var collected: Boolean = false
    )

    private enum class ActionControl { JUMP, WHACK }

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
        private val actionPointers = mutableMapOf<Int, ActionControl>()
        private val worldWidth = 5200f

        private var joystickPointerId: Int? = null
        private var joystickX = 0f
        private var joystickY = 0f

        private var playerX = 220f
        private var playerY = 0f
        private var velocityY = 0f
        private var groundY = 0f
        private var cameraX = 0f
        private var facing = 1

        private var score = 0
        private var health = 100
        private var coldOnes = 0
        private var started = false
        private var gameOver = false
        private var levelWon = false

        private var attackTimer = 0f
        private var attackNumber = 0
        private var hurtCooldown = 0f
        private var lastFrame = System.nanoTime()

        private var messageText = ""
        private var messageTimer = 0f
        private var bossIntroShown = false

        private val flamingos = mutableListOf<Flamingo>()
        private val pickups = mutableListOf<Pickup>()

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
            drawMessage(canvas)
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
            coldOnes = 0
            gameOver = false
            levelWon = false
            attackTimer = 0f
            attackNumber = 0
            hurtCooldown = 0f
            messageText = ""
            messageTimer = 0f
            bossIntroShown = false
            joystickPointerId = null
            joystickX = 0f
            joystickY = 0f
            actionPointers.clear()

            flamingos.clear()
            flamingos += Flamingo(980f, 2, 2)
            flamingos += Flamingo(1480f, 2, 2)
            flamingos += Flamingo(2100f, 3, 3)
            flamingos += Flamingo(2780f, 3, 3)
            flamingos += Flamingo(3420f, 3, 3)
            flamingos += Flamingo(4300f, 9, 9, boss = true)

            pickups.clear()
            pickups += Pickup(1225f, PickupType.COLD_ONE)
            pickups += Pickup(2450f, PickupType.COLD_ONE)
            pickups += Pickup(3600f, PickupType.COLD_ONE)
        }

        private fun updateGame(dt: Float) {
            val move = if (abs(joystickX) < 0.14f) 0f else joystickX
            if (move != 0f) {
                facing = if (move > 0f) 1 else -1
                playerX = (playerX + move * 420f * dt).coerceIn(55f, worldWidth - 90f)
            }

            velocityY += 1550f * dt
            playerY += velocityY * dt
            if (playerY > groundY) {
                playerY = groundY
                velocityY = 0f
            }

            attackTimer = max(0f, attackTimer - dt)
            hurtCooldown = max(0f, hurtCooldown - dt)
            messageTimer = max(0f, messageTimer - dt)

            val playerScreenTarget = width * 0.37f
            val desiredCamera = playerX - playerScreenTarget
            cameraX += (desiredCamera - cameraX) * min(1f, dt * 7f)
            cameraX = cameraX.coerceIn(0f, max(0f, worldWidth - width))

            for (enemy in flamingos) {
                enemy.hitFlash = max(0f, enemy.hitFlash - dt)
                if (enemy.hp <= 0) continue

                val distance = playerX - enemy.x
                val chaseRange = if (enemy.boss) 760f else 520f
                if (abs(distance) < chaseRange) {
                    val direction = if (distance > 0f) 1f else -1f
                    val speed = if (enemy.boss) 110f else 84f
                    enemy.x += direction * speed * dt
                }

                if (attackTimer > 0f && enemy.lastHitAttack != attackNumber) {
                    val signedDistance = (enemy.x - playerX) * facing
                    val verticalOk = playerY >= groundY - 165f
                    if (signedDistance in 20f..190f && verticalOk) {
                        enemy.hp--
                        enemy.lastHitAttack = attackNumber
                        enemy.hitFlash = 0.13f
                        enemy.x += facing * if (enemy.boss) 42f else 78f
                        score += if (enemy.boss) 250 else 100
                        showMessage(if (enemy.boss) "THAT ACTUALLY HURT IT." else "BONK.", 0.55f)
                        if (enemy.hp <= 0) {
                            score += if (enemy.boss) 1500 else 250
                            showMessage(
                                if (enemy.boss) "THE LAWN ORNAMENT HAS BEEN DEFEATED." else "FLAMINGO PROBLEM TEMPORARILY SOLVED.",
                                if (enemy.boss) 1.8f else 0.85f
                            )
                        }
                    }
                }

                val collisionDistance = if (enemy.boss) 92f else 62f
                if (
                    abs(enemy.x - playerX) < collisionDistance &&
                    playerY > groundY - 95f &&
                    hurtCooldown <= 0f
                ) {
                    health = max(0, health - if (enemy.boss) 18 else 10)
                    hurtCooldown = 0.75f
                    playerX = (playerX - if (distance > 0f) 90f else -90f)
                        .coerceIn(55f, worldWidth - 90f)
                    showMessage("THIS WAS A TERRIBLE PLAN.", 0.85f)
                    if (health <= 0) gameOver = true
                }
            }

            for (pickup in pickups) {
                if (pickup.collected) continue
                if (abs(pickup.x - playerX) < 68f && playerY > groundY - 130f) {
                    pickup.collected = true
                    when (pickup.type) {
                        PickupType.COLD_ONE -> {
                            coldOnes++
                            health = min(100, health + 25)
                            score += 200
                            showMessage("COLD ONE ACQUIRED. QUESTIONABLE MEDICAL BENEFIT.", 1.35f)
                        }
                    }
                }
            }

            val boss = flamingos.lastOrNull()
            if (!bossIntroShown && playerX > 3820f && boss != null && boss.hp > 0) {
                bossIntroShown = true
                showMessage("OH GOOD. AN ALPHA FLAMINGO.", 2.1f)
            }

            val bossDefeated = boss?.hp == 0
            if (bossDefeated && playerX > 4650f) levelWon = true
        }

        private fun showMessage(text: String, seconds: Float) {
            messageText = text
            messageTimer = seconds
        }

        private fun drawWorld(canvas: Canvas) {
            val sky = LinearGradient(
                0f, 0f, 0f, groundY,
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

            drawSectionSigns(canvas)

            for (pickup in pickups) {
                if (!pickup.collected) drawPickup(canvas, pickup)
            }

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
                    canvas.drawLine(
                        x,
                        groundY - 255f,
                        x + i * 48f,
                        groundY - 300f + abs(i) * 15f,
                        paint
                    )
                }
            }
        }

        private fun drawFence(canvas: Canvas) {
            paint.color = Color.rgb(238, 238, 220)
            val spacing = 95f
            var x = -((cameraX * 0.78f) % spacing) - spacing
            while (x < width + spacing) {
                canvas.drawRect(x, groundY - 145f, x + 72f, groundY - 15f, paint)
                val path = android.graphics.Path().apply {
                    moveTo(x, groundY - 145f)
                    lineTo(x + 36f, groundY - 182f)
                    lineTo(x + 72f, groundY - 145f)
                    close()
                }
                canvas.drawPath(path, paint)
                x += spacing
            }
        }

        private fun drawPool(canvas: Canvas) {
            val x = 2550f - cameraX
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

        private fun drawSectionSigns(canvas: Canvas) {
            drawSmallSign(canvas, 760f, "POOL RULE #1", "NO FLAMINGOS")
            drawSmallSign(canvas, 3100f, "HOA NOTICE", "THIS IS PROBABLY FINE")
        }

        private fun drawSmallSign(canvas: Canvas, worldX: Float, top: String, bottom: String) {
            val x = worldX - cameraX
            if (x < -180f || x > width + 180f) return
            paint.color = Color.rgb(105, 72, 42)
            canvas.drawRect(x - 5f, groundY - 125f, x + 5f, groundY, paint)
            paint.color = Color.rgb(245, 239, 204)
            canvas.drawRect(x - 105f, groundY - 185f, x + 105f, groundY - 118f, paint)
            paint.color = Color.rgb(40, 40, 40)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 18f
            canvas.drawText(top, x, groundY - 157f, paint)
            paint.textSize = 14f
            canvas.drawText(bottom, x, groundY - 134f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun drawPickup(canvas: Canvas, pickup: Pickup) {
            val x = pickup.x - cameraX
            if (x < -100f || x > width + 100f) return
            val y = groundY - 58f

            when (pickup.type) {
                PickupType.COLD_ONE -> {
                    paint.color = Color.argb(90, 255, 244, 150)
                    canvas.drawCircle(x, y, 42f, paint)
                    paint.color = Color.rgb(220, 226, 232)
                    canvas.drawRoundRect(x - 20f, y - 34f, x + 20f, y + 34f, 7f, 7f, paint)
                    paint.color = Color.rgb(55, 133, 200)
                    canvas.drawRect(x - 20f, y - 9f, x + 20f, y + 15f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.DEFAULT_BOLD
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = 12f
                    canvas.drawText("COLD", x, y + 6f, paint)
                    paint.textAlign = Paint.Align.LEFT
                }
            }
        }

        private fun drawPlayer(canvas: Canvas) {
            val x = playerX - cameraX
            val y = playerY
            if (hurtCooldown > 0f && ((hurtCooldown * 12).toInt() % 2 == 0)) return

            paint.color = Color.rgb(242, 181, 133)
            canvas.drawRect(x - 23f, y - 3f, x - 8f, y + 42f, paint)
            canvas.drawRect(x + 8f, y - 3f, x + 23f, y + 42f, paint)
            paint.color = Color.rgb(35, 35, 35)
            canvas.drawRect(x - 31f, y + 39f, x - 4f, y + 46f, paint)
            canvas.drawRect(x + 4f, y + 39f, x + 31f, y + 46f, paint)

            paint.color = Color.rgb(42, 71, 148)
            canvas.drawRect(x - 34f, y - 44f, x + 34f, y + 7f, paint)
            paint.color = Color.WHITE
            canvas.drawRect(x - 31f, y - 110f, x + 31f, y - 44f, paint)

            paint.color = Color.rgb(242, 181, 133)
            canvas.drawRect(x - 44f, y - 102f, x - 29f, y - 48f, paint)
            canvas.drawRect(x + 29f, y - 102f, x + 44f, y - 48f, paint)
            canvas.drawCircle(x, y - 142f, 34f, paint)

            paint.color = Color.rgb(91, 50, 24)
            canvas.drawRect(x - 37f, y - 174f, x + 25f, y - 158f, paint)
            canvas.drawRect(x - 39f, y - 161f, x - 25f, y - 108f, paint)

            paint.color = Color.BLACK
            canvas.drawRect(x - 25f, y - 150f, x - 4f, y - 141f, paint)
            canvas.drawRect(x + 4f, y - 150f, x + 25f, y - 141f, paint)

            paint.strokeWidth = 20f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(255, 47, 165)
            val attackReach = if (attackTimer > 0f) 165f else 72f
            val startX = x + facing * 25f
            val endX = x + facing * attackReach
            canvas.drawLine(
                startX,
                y - 92f,
                endX,
                y - if (attackTimer > 0f) 105f else 128f,
                paint
            )
        }

        private fun drawFlamingo(canvas: Canvas, enemy: Flamingo) {
            val x = enemy.x - cameraX
            if (x < -180f || x > width + 180f) return

            val scale = if (enemy.boss) 1.55f else 1f
            val bodyY = groundY - 92f * scale
            val flashing = enemy.hitFlash > 0f

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 13f * scale
            paint.color = if (flashing) {
                Color.WHITE
            } else if (enemy.boss) {
                Color.rgb(232, 36, 95)
            } else {
                Color.rgb(255, 92, 147)
            }

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
            paint.textSize = height * 0.034f
            canvas.drawText("SCORE $score", width - pad * 1.7f, pad + height * 0.038f, paint)
            paint.textSize = height * 0.026f
            canvas.drawText("COLD ONES $coldOnes/3", width - pad * 1.7f, pad + height * 0.073f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun drawMessage(canvas: Canvas) {
            if (messageTimer <= 0f || !started || gameOver || levelWon) return
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = min(28f, height * 0.034f)
            val textWidth = paint.measureText(messageText)
            paint.color = Color.argb(175, 0, 0, 0)
            canvas.drawRoundRect(
                width / 2f - textWidth / 2f - 24f,
                height * 0.13f,
                width / 2f + textWidth / 2f + 24f,
                height * 0.13f + 48f,
                16f,
                16f,
                paint
            )
            paint.color = Color.WHITE
            canvas.drawText(messageText, width / 2f, height * 0.13f + 32f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun joystickCenterX(): Float = max(96f, height * 0.16f)
        private fun joystickCenterY(): Float = height - max(82f, height * 0.14f)
        private fun joystickRadius(): Float = min(height * 0.115f, 96f)

        private fun drawControls(canvas: Canvas) {
            drawJoystick(canvas)

            val r = min(height * 0.085f, 78f)
            val y = height - r - height * 0.035f
            val whackX = width - r - height * 0.035f
            val jumpX = whackX - r * 2.25f
            drawButton(canvas, jumpX, y, r, "JUMP", 0.36f)
            drawButton(canvas, whackX, y, r, "WHACK", 0.31f)
        }

        private fun drawJoystick(canvas: Canvas) {
            val cx = joystickCenterX()
            val cy = joystickCenterY()
            val radius = joystickRadius()

            paint.color = Color.argb(105, 0, 0, 0)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = Color.argb(190, 255, 255, 255)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.style = Paint.Style.FILL

            val knobTravel = radius * 0.58f
            val knobX = cx + joystickX * knobTravel
            val knobY = cy + joystickY * knobTravel
            paint.color = Color.argb(205, 35, 35, 35)
            canvas.drawCircle(knobX, knobY, radius * 0.46f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.WHITE
            canvas.drawCircle(knobX, knobY, radius * 0.46f, paint)
            paint.style = Paint.Style.FILL
        }

        private fun drawButton(
            canvas: Canvas,
            x: Float,
            y: Float,
            r: Float,
            label: String,
            textScale: Float
        ) {
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
                    canvas.drawText(
                        "GET TO THE BACKYARD EXIT. TRY NOT TO GET MURDERED BY LAWN ORNAMENTS.",
                        width / 2f,
                        height * 0.53f,
                        paint
                    )
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
                    canvas.drawText("POOL NOODLE PANIC CLEAR • SCORE $score", width / 2f, height * 0.53f, paint)
                    canvas.drawText("COLD ONES FOUND: $coldOnes/3", width / 2f, height * 0.59f, paint)
                    canvas.drawText("TAP TO RUN IT AGAIN", width / 2f, height * 0.65f, paint)
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

        private fun isInsideJoystick(x: Float, y: Float): Boolean {
            val dx = x - joystickCenterX()
            val dy = y - joystickCenterY()
            return hypot(dx, dy) <= joystickRadius() * 1.45f
        }

        private fun updateJoystick(x: Float, y: Float) {
            val radius = joystickRadius()
            val dx = x - joystickCenterX()
            val dy = y - joystickCenterY()
            val distance = hypot(dx, dy)
            if (distance <= radius || distance == 0f) {
                joystickX = (dx / radius).coerceIn(-1f, 1f)
                joystickY = (dy / radius).coerceIn(-1f, 1f)
            } else {
                joystickX = (dx / distance).coerceIn(-1f, 1f)
                joystickY = (dy / distance).coerceIn(-1f, 1f)
            }
        }

        private fun actionControlAt(x: Float, y: Float): ActionControl? {
            val r = min(height * 0.085f, 78f)
            val buttonY = height - r - height * 0.035f
            val whackX = width - r - height * 0.035f
            val jumpX = whackX - r * 2.25f
            val hitRadius = r * 1.22f

            return when {
                hypot(x - whackX, y - buttonY) <= hitRadius -> ActionControl.WHACK
                hypot(x - jumpX, y - buttonY) <= hitRadius -> ActionControl.JUMP
                else -> null
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val index = event.actionIndex
                    val id = event.getPointerId(index)
                    val x = event.getX(index)
                    val y = event.getY(index)

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

                    if (joystickPointerId == null && isInsideJoystick(x, y)) {
                        joystickPointerId = id
                        updateJoystick(x, y)
                    } else {
                        val control = actionControlAt(x, y)
                        if (control != null) {
                            actionPointers[id] = control
                            if (control == ActionControl.JUMP) jump()
                            if (control == ActionControl.WHACK) attack()
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val id = joystickPointerId
                    if (id != null) {
                        val index = event.findPointerIndex(id)
                        if (index >= 0) updateJoystick(event.getX(index), event.getY(index))
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val id = event.getPointerId(event.actionIndex)
                    if (id == joystickPointerId) {
                        joystickPointerId = null
                        joystickX = 0f
                        joystickY = 0f
                    }
                    actionPointers.remove(id)
                }

                MotionEvent.ACTION_CANCEL -> {
                    joystickPointerId = null
                    joystickX = 0f
                    joystickY = 0f
                    actionPointers.clear()
                }
            }
            return true
        }
    }
}
