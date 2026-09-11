from pathlib import Path

path = Path("app/src/main/java/com/brannenservices/fieldassistant/MainActivity.kt")
src = path.read_text()

man_start = src.index("        private fun man() {")
bird_start = src.index("        private fun bird(b: Bird) {")
beer_start = src.index("        private fun beer(q: Beer) {")

new_man = r'''        private fun man() {
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

'''

new_bird = r'''        private fun bird(b: Bird) {
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

'''

src = src[:man_start] + new_man + new_bird + src[beer_start:]
path.write_text(src)
