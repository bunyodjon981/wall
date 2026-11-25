package com.example.simulation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.WinterWallpaperItem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class WinterSceneRenderer(private val context: Context) {

    private var cachedWallpaperId: String? = null
    private var cachedBitmap: Bitmap? = null
    private var lastWidth = 0f
    private var lastHeight = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val backPath = Path()
    private val destRect = RectF()
    private val tmpRect = RectF()

    private var skyShader: LinearGradient? = null
    private var snowShader: LinearGradient? = null
    private var snowShaderHeight = 0f

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        wallpaper: WinterWallpaperItem,
        state: WinterSceneState
    ) {
        if (width <= 0f || height <= 0f) return
        ensureAssets(wallpaper, width, height)

        // 1. Scenic Winter Backdrop
        drawBackdrop(canvas, width, height, wallpaper, state)

        // 2. Far & Mid Layer Falling Snow
        drawFlakes(canvas, width, height, density, state, maxLayer = 1)

        // 3. Back Snowdrift Ridge (3D depth layer behind kids)
        drawBackSnowbank(canvas, width, height, state)

        // 4. 3D-Shaded Children Playing & Throwing Snowballs
        for (kid in state.kids) {
            draw3DKid(canvas, width, height, density, state, kid)
        }

        // 5. Flying 3D Snowballs + Shadows
        drawSnowballs(canvas, width, height, density, state)

        // 6. Animated Snowplow Truck (Qor tozalash mashinasi)
        if (state.plow.active) {
            drawSnowplowTruck(canvas, width, height, density, state)
        }

        // 7. Foreground Accumulated Snow Layer + Twinkling Sparkles
        drawFrontSnowbank(canvas, width, height, density, state)

        // 8. Splash Particles & Foreground Crystal Snowflakes
        drawParticles(canvas, width, height, density, state)
        drawFlakes(canvas, width, height, density, state, minLayer = 2, maxLayer = 2)

        // 9. Interactive Finger Touch Ripple
        if (state.touchRippleAlpha > 0.01f && state.touchRippleXNorm >= 0f) {
            val rx = state.touchRippleXNorm * width
            val ry = state.touchRippleYNorm * height
            val rad = (1f - state.touchRippleAlpha) * 44f * density + 12f * density
            strokePaint.color = Color.argb((state.touchRippleAlpha * 165).toInt(), 210, 245, 255)
            strokePaint.strokeWidth = 2.5f * density
            canvas.drawCircle(rx, ry, rad, strokePaint)
        }
    }

    private fun ensureAssets(wallpaper: WinterWallpaperItem, width: Float, height: Float) {
        if (cachedWallpaperId != wallpaper.id) {
            cachedWallpaperId = wallpaper.id
            cachedBitmap?.recycle()
            cachedBitmap = wallpaper.drawableRes?.let { resId ->
                BitmapFactory.decodeResource(context.resources, resId)
            }
            skyShader = LinearGradient(
                0f, 0f, 0f, height,
                wallpaper.skyTopColor, wallpaper.skyBottomColor, Shader.TileMode.CLAMP
            )
        }
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height
            skyShader = LinearGradient(
                0f, 0f, 0f, height,
                wallpaper.skyTopColor, wallpaper.skyBottomColor, Shader.TileMode.CLAMP
            )
            snowShader = LinearGradient(
                0f, height * 0.72f, 0f, height,
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFEAF4FF.toInt(), 0xFFCDE3F7.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            snowShaderHeight = height
        }
    }

    private fun drawBackdrop(
        canvas: Canvas,
        width: Float,
        height: Float,
        wallpaper: WinterWallpaperItem,
        state: WinterSceneState
    ) {
        val bmp = cachedBitmap
        if (bmp != null && !bmp.isRecycled) {
            // Center-crop scale to fill full screen cleanly
            val scale = max(width / bmp.width, height / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val left = (width - drawW) * 0.5f
            val top = (height - drawH) * 0.5f
            destRect.set(left, top, left + drawW, top + drawH)
            paint.reset()
            paint.isFilterBitmap = true
            canvas.drawBitmap(bmp, null, destRect, paint)

            // Subtle atmospheric gradient at very bottom so snowbank blends seamlessly
            paint.shader = LinearGradient(
                0f, height * 0.62f, 0f, height,
                Color.TRANSPARENT,
                Color.argb(110, 18, 38, 68),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, height * 0.62f, width, height, paint)
            paint.shader = null
        } else {
            // Procedural winter starlight & pine ridge backdrop for themes #5 and #6
            paint.reset()
            paint.isAntiAlias = true
            paint.shader = skyShader
            canvas.drawRect(0f, 0f, width, height, paint)
            paint.shader = null

            // Twinkling stars
            for (i in 0 until 45) {
                val sx = ((i * 197 + 31) % 1000) / 1000f * width
                val sy = ((i * 313 + 73) % 620) / 1000f * height
                val twinkle = 0.45f + 0.55f * sin(state.elapsedTime * (1.2f + (i % 5) * 0.4f) + i)
                paint.color = Color.argb((twinkle * 210).toInt(), 240, 248, 255)
                canvas.drawCircle(sx, sy, (1.1f + (i % 3) * 0.7f) * (width / 400f), paint)
            }

            // Glowing Moon
            val moonX = width * 0.78f
            val moonY = height * 0.17f
            val moonR = width * 0.085f
            paint.shader = RadialGradient(
                moonX, moonY, moonR * 2.8f,
                Color.argb(95, 210, 240, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(moonX, moonY, moonR * 2.8f, paint)
            paint.shader = null
            paint.color = 0xFFF8FAFC.toInt()
            canvas.drawCircle(moonX, moonY, moonR, paint)

            // Distant snowy mountain ridge
            path.reset()
            path.moveTo(0f, height * 0.78f)
            path.lineTo(width * 0.18f, height * 0.56f)
            path.lineTo(width * 0.42f, height * 0.67f)
            path.lineTo(width * 0.68f, height * 0.50f)
            path.lineTo(width * 0.90f, height * 0.63f)
            path.lineTo(width, height * 0.58f)
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            paint.color = wallpaper.hillBackColor
            canvas.drawPath(path, paint)

            // Foreground pine tree silhouettes & warm cottage glow
            path.reset()
            path.moveTo(0f, height * 0.80f)
            path.quadTo(width * 0.35f, height * 0.69f, width * 0.65f, height * 0.76f)
            path.quadTo(width * 0.85f, height * 0.80f, width, height * 0.73f)
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            paint.color = wallpaper.hillFrontColor
            canvas.drawPath(path, paint)

            // Silhouetted snow-dusted pine trees
            val treePositions = floatArrayOf(0.08f, 0.19f, 0.31f, 0.72f, 0.85f, 0.94f)
            for ((idx, txNorm) in treePositions.withIndex()) {
                val tx = txNorm * width
                val ty = height * (0.72f + (idx % 2) * 0.03f)
                val th = height * (0.14f + (idx % 3) * 0.03f)
                drawStylizedPine(canvas, tx, ty, th, wallpaper.accentGlowColor)
            }
        }
    }

    private fun drawStylizedPine(canvas: Canvas, cx: Float, baseY: Float, h: Float, glowColor: Int) {
        val w = h * 0.42f
        paint.color = 0xFF0C223D.toInt()
        for (tier in 0..2) {
            val tierBottom = baseY - tier * (h * 0.25f)
            val tierTop = tierBottom - h * 0.42f
            val tierW = w * (1f - tier * 0.22f)
            path.reset()
            path.moveTo(cx, tierTop)
            path.lineTo(cx - tierW, tierBottom)
            path.lineTo(cx + tierW, tierBottom)
            path.close()
            canvas.drawPath(path, paint)

            // Snow cap on branches
            paint.color = 0xFFDCEEFF.toInt()
            path.reset()
            path.moveTo(cx, tierTop)
            path.lineTo(cx - tierW * 0.52f, tierTop + h * 0.14f)
            path.quadTo(cx, tierTop + h * 0.17f, cx + tierW * 0.52f, tierTop + h * 0.14f)
            path.close()
            canvas.drawPath(path, paint)
            paint.color = 0xFF0C223D.toInt()
        }
        // Subtle twinkle ornament light
        paint.color = glowColor
        canvas.drawCircle(cx, baseY - h * 0.45f, w * 0.09f, paint)
    }

    private fun drawFlakes(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState,
        minLayer: Int = 0,
        maxLayer: Int = 2
    ) {
        paint.reset()
        paint.isAntiAlias = true
        val activeCount = state.intensity.flakeCount.coerceAtMost(state.flakes.size)
        for (i in 0 until activeCount) {
            val f = state.flakes[i]
            if (f.layer < minLayer || f.layer > maxLayer) continue
            val fx = f.xNorm * width
            val fy = f.yNorm * height
            val r = f.radiusDp * density

            if (f.layer == 2) {
                // Soft glow + 6-pointed ice crystal snowflake
                paint.color = Color.argb((f.alpha * 0.35f).toInt(), 190, 235, 255)
                canvas.drawCircle(fx, fy, r * 1.4f, paint)

                strokePaint.color = Color.argb(f.alpha, 255, 255, 255)
                strokePaint.strokeWidth = max(1.1f, r * 0.22f)
                val rad = f.rotation * PI.toFloat() / 180f
                for (arm in 0 until 3) {
                    val ang = rad + arm * (PI.toFloat() / 3f)
                    val dx = cos(ang) * r
                    val dy = sin(ang) * r
                    canvas.drawLine(fx - dx, fy - dy, fx + dx, fy + dy, strokePaint)
                }
            } else {
                paint.color = Color.argb(f.alpha, 245, 251, 255)
                canvas.drawCircle(fx, fy, r, paint)
            }
        }
    }

    private fun drawBackSnowbank(
        canvas: Canvas,
        width: Float,
        height: Float,
        state: WinterSceneState
    ) {
        backPath.reset()
        val cols = WinterSceneState.SNOW_COLUMNS
        val step = width / (cols - 1)
        backPath.moveTo(0f, height)
        for (i in 0 until cols) {
            val x = i * step
            val y = state.getSnowSurfaceYNorm(i.toFloat() / (cols - 1)) * height - height * 0.022f
            if (i == 0) {
                backPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * step
                val prevY = state.getSnowSurfaceYNorm((i - 1).toFloat() / (cols - 1)) * height - height * 0.022f
                backPath.quadTo(prevX, prevY, (prevX + x) * 0.5f, (prevY + y) * 0.5f)
            }
        }
        backPath.lineTo(width, height)
        backPath.close()
        paint.reset()
        paint.isAntiAlias = true
        paint.color = 0xFFB8D4EE.toInt()
        canvas.drawPath(backPath, paint)
    }

    private fun drawFrontSnowbank(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState
    ) {
        path.reset()
        val cols = WinterSceneState.SNOW_COLUMNS
        val step = width / (cols - 1)
        path.moveTo(0f, height)
        for (i in 0 until cols) {
            val x = i * step
            val y = state.getSnowSurfaceYNorm(i.toFloat() / (cols - 1)) * height
            if (i == 0) {
                path.lineTo(x, y)
            } else {
                val prevX = (i - 1) * step
                val prevY = state.getSnowSurfaceYNorm((i - 1).toFloat() / (cols - 1)) * height
                path.quadTo(prevX, prevY, (prevX + x) * 0.5f, (prevY + y) * 0.5f)
            }
        }
        path.lineTo(width, height)
        path.close()

        paint.reset()
        paint.isAntiAlias = true
        paint.shader = snowShader
        canvas.drawPath(path, paint)
        paint.shader = null

        // Crisp highlight rim along top of snowdrift
        strokePaint.color = Color.argb(235, 255, 255, 255)
        strokePaint.strokeWidth = 2.4f * density
        path.reset()
        for (i in 0 until cols) {
            val x = i * step
            val y = state.getSnowSurfaceYNorm(i.toFloat() / (cols - 1)) * height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, strokePaint)

        // Twinkling ice sparkles on the snow surface
        for (sp in state.sparkles) {
            val glint = max(0f, sin(sp.phase))
            if (glint > 0.25f) {
                val sx = sp.xNorm * width
                val sy = state.getSnowSurfaceYNorm(sp.xNorm) * height + 8f * density
                val r = sp.sizeDp * density * glint
                paint.color = Color.argb((glint * 220).toInt(), 255, 255, 255)
                canvas.drawCircle(sx, sy, r * 0.55f, paint)
                strokePaint.color = Color.argb((glint * 200).toInt(), 210, 245, 255)
                strokePaint.strokeWidth = 1.1f * density
                canvas.drawLine(sx - r * 1.4f, sy, sx + r * 1.4f, sy, strokePaint)
                canvas.drawLine(sx, sy - r * 1.4f, sx, sy + r * 1.4f, strokePaint)
            }
        }
    }

    private fun draw3DKid(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState,
        kid: WinterSceneState.WinterKid
    ) {
        val baseScale = min(width, height) / 410f * kid.scaleFactor
        val cx = kid.xNorm * width
        val snowY = state.getSnowSurfaceYNorm(kid.xNorm) * height + 4f * baseScale
        val crouchY = kid.crouchAmount * 12f * baseScale
        val groundY = snowY - kid.jumpOffsetDp * density * 0.8f

        // 1. Soft 3D contact shadow on the snow
        paint.reset()
        paint.isAntiAlias = true
        paint.color = Color.argb(85, 65, 105, 150)
        tmpRect.set(cx - 18f * baseScale, snowY - 4f * baseScale, cx + 18f * baseScale, snowY + 6f * baseScale)
        canvas.drawOval(tmpRect, paint)

        canvas.save()
        canvas.translate(cx, groundY + crouchY)

        // Legs & winter boots (stride animation)
        val stride = if (kid.action == WinterSceneState.KidAction.WALKING) sin(kid.walkPhase) * 7f * baseScale else 0f
        drawBootAndLeg(canvas, -6.5f * baseScale + stride * 0.5f, -15f * baseScale, baseScale, kid.coatShadowColor)
        drawBootAndLeg(canvas, 6.5f * baseScale - stride * 0.5f, -15f * baseScale, baseScale, kid.coatShadowColor)

        // 3D-shaded Puffer Coat Torso (RadialGradient for volumetric depth)
        val torsoCenterY = -28f * baseScale
        val torsoRx = 14.5f * baseScale
        val torsoRy = 16.5f * baseScale
        paint.shader = RadialGradient(
            -4f * baseScale * kid.facing, torsoCenterY - 5f * baseScale, torsoRy * 1.4f,
            lightenColor(kid.coatMainColor, 0.25f),
            kid.coatShadowColor,
            Shader.TileMode.CLAMP
        )
        tmpRect.set(-torsoRx, torsoCenterY - torsoRy, torsoRx, torsoCenterY + torsoRy)
        canvas.drawRoundRect(tmpRect, 12f * baseScale, 12f * baseScale, paint)
        paint.shader = null

        // Coat zipper highlight
        strokePaint.color = Color.argb(110, 255, 255, 255)
        strokePaint.strokeWidth = 1.6f * baseScale
        canvas.drawLine(0f, torsoCenterY - 11f * baseScale, 0f, torsoCenterY + 13f * baseScale, strokePaint)

        // Back arm
        drawArm(
            canvas = canvas,
            shoulderX = -10f * baseScale * kid.facing,
            shoulderY = torsoCenterY - 6f * baseScale,
            angleDeg = -kid.armSwing * 0.45f,
            baseScale = baseScale,
            coatColor = kid.coatShadowColor,
            mittenColor = kid.hatColor
        )

        // Fluttering Winter Scarf
        val flutter = sin(state.elapsedTime * 5.5f + kid.id) * 4f * baseScale
        paint.color = kid.scarfColor
        tmpRect.set(-12f * baseScale, torsoCenterY - 16f * baseScale, 12f * baseScale, torsoCenterY - 8f * baseScale)
        canvas.drawRoundRect(tmpRect, 5f * baseScale, 5f * baseScale, paint)
        path.reset()
        val scarfDir = -kid.facing.toFloat()
        path.moveTo(scarfDir * 4f * baseScale, torsoCenterY - 11f * baseScale)
        path.quadTo(
            scarfDir * 18f * baseScale, torsoCenterY - 7f * baseScale + flutter,
            scarfDir * 22f * baseScale, torsoCenterY + 4f * baseScale + flutter
        )
        path.lineTo(scarfDir * 15f * baseScale, torsoCenterY + 7f * baseScale + flutter)
        path.quadTo(
            scarfDir * 11f * baseScale, torsoCenterY - 2f * baseScale + flutter * 0.5f,
            scarfDir * 1f * baseScale, torsoCenterY - 8f * baseScale
        )
        path.close()
        canvas.drawPath(path, paint)

        // 3D Head with rosy cheeks & blinking eyes
        val headY = torsoCenterY - 27f * baseScale
        val headR = 12.5f * baseScale
        paint.shader = RadialGradient(
            -3f * baseScale * kid.facing, headY - 3f * baseScale, headR * 1.45f,
            0xFFFFE5D0.toInt(), 0xFFF0B48C.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, headY, headR, paint)
        paint.shader = null

        // Rosy cheeks
        paint.color = Color.argb(120, 244, 114, 128)
        val lookOffset = kid.facing * 2.5f * baseScale
        canvas.drawCircle(lookOffset - 5.5f * baseScale, headY + 3.2f * baseScale, 2.8f * baseScale, paint)
        canvas.drawCircle(lookOffset + 5.5f * baseScale, headY + 3.2f * baseScale, 2.8f * baseScale, paint)

        // Eyes & smile
        val isBlinking = kid.blinkTimer < 0f
        paint.color = 0xFF1E293B.toInt()
        if (isBlinking) {
            strokePaint.color = 0xFF1E293B.toInt()
            strokePaint.strokeWidth = 1.6f * baseScale
            canvas.drawLine(lookOffset - 5f * baseScale, headY - 1f * baseScale, lookOffset - 2f * baseScale, headY - 1f * baseScale, strokePaint)
            canvas.drawLine(lookOffset + 2f * baseScale, headY - 1f * baseScale, lookOffset + 5f * baseScale, headY - 1f * baseScale, strokePaint)
        } else {
            canvas.drawCircle(lookOffset - 3.6f * baseScale, headY - 1.2f * baseScale, 1.7f * baseScale, paint)
            canvas.drawCircle(lookOffset + 3.6f * baseScale, headY - 1.2f * baseScale, 1.7f * baseScale, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(lookOffset - 3.1f * baseScale, headY - 1.8f * baseScale, 0.65f * baseScale, paint)
            canvas.drawCircle(lookOffset + 4.1f * baseScale, headY - 1.8f * baseScale, 0.65f * baseScale, paint)
        }
        // Cheerful smile
        strokePaint.color = 0xFF9F4A3F.toInt()
        strokePaint.strokeWidth = 1.5f * baseScale
        tmpRect.set(lookOffset - 3.5f * baseScale, headY + 1.5f * baseScale, lookOffset + 3.5f * baseScale, headY + 6.2f * baseScale)
        canvas.drawArc(tmpRect, 15f, 150f, false, strokePaint)

        // Knit Winter Hat + Fluffy Pom-Pom
        paint.shader = RadialGradient(
            -3f * baseScale, headY - 11f * baseScale, headR * 1.4f,
            lightenColor(kid.hatColor, 0.25f), darkenColor(kid.hatColor, 0.28f), Shader.TileMode.CLAMP
        )
        tmpRect.set(-headR * 1.06f, headY - headR * 1.28f, headR * 1.06f, headY + headR * 0.25f)
        canvas.drawArc(tmpRect, 180f, 180f, true, paint)
        paint.shader = null
        // Hat rim
        paint.color = Color.WHITE
        tmpRect.set(-headR * 1.12f, headY - 6.5f * baseScale, headR * 1.12f, headY - 1.5f * baseScale)
        canvas.drawRoundRect(tmpRect, 3.5f * baseScale, 3.5f * baseScale, paint)
        // Pom-pom
        canvas.drawCircle(0f, headY - headR * 1.32f, 4.2f * baseScale, paint)

        // Front Throwing Arm (+ snowball in hand during windup!)
        drawArm(
            canvas = canvas,
            shoulderX = 9.5f * baseScale * kid.facing,
            shoulderY = torsoCenterY - 6f * baseScale,
            angleDeg = kid.armSwing * kid.facing,
            baseScale = baseScale,
            coatColor = kid.coatMainColor,
            mittenColor = kid.hatColor,
            holdingSnowball = kid.action == WinterSceneState.KidAction.WINDUP
        )

        canvas.restore()
    }

    private fun drawBootAndLeg(canvas: Canvas, x: Float, topY: Float, s: Float, legColor: Int) {
        paint.color = legColor
        tmpRect.set(x - 4.2f * s, topY, x + 4.2f * s, -2.5f * s)
        canvas.drawRoundRect(tmpRect, 3.5f * s, 3.5f * s, paint)

        paint.color = 0xFF1F2937.toInt()
        tmpRect.set(x - 5.2f * s, -5.5f * s, x + 5.8f * s, 0.5f * s)
        canvas.drawRoundRect(tmpRect, 3f * s, 3f * s, paint)
    }

    private fun drawArm(
        canvas: Canvas,
        shoulderX: Float,
        shoulderY: Float,
        angleDeg: Float,
        baseScale: Float,
        coatColor: Int,
        mittenColor: Int,
        holdingSnowball: Boolean = false
    ) {
        canvas.save()
        canvas.translate(shoulderX, shoulderY)
        canvas.rotate(angleDeg)
        paint.color = coatColor
        tmpRect.set(-3.8f * baseScale, 0f, 3.8f * baseScale, 14f * baseScale)
        canvas.drawRoundRect(tmpRect, 3.5f * baseScale, 3.5f * baseScale, paint)
        paint.color = mittenColor
        canvas.drawCircle(0f, 15.5f * baseScale, 4.0f * baseScale, paint)
        if (holdingSnowball) {
            paint.color = Color.WHITE
            canvas.drawCircle(0f, 19.5f * baseScale, 4.4f * baseScale, paint)
        }
        canvas.restore()
    }

    private fun drawSnowballs(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState
    ) {
        for (sb in state.snowballs) {
            val sx = sb.currentXNorm * width
            val sy = sb.currentYNorm * height
            val groundY = state.getSnowSurfaceYNorm(sb.currentXNorm) * height

            // Ground shadow below arcing snowball
            val heightAboveGround = max(0f, groundY - sy)
            val shadowScale = (1f - heightAboveGround / (height * 0.25f)).coerceIn(0.35f, 1f)
            paint.reset()
            paint.isAntiAlias = true
            paint.color = Color.argb((75 * shadowScale).toInt(), 60, 100, 145)
            val r = sb.radiusDp * density
            tmpRect.set(sx - r * 1.2f * shadowScale, groundY - 2f * density, sx + r * 1.2f * shadowScale, groundY + 3f * density)
            canvas.drawOval(tmpRect, paint)

            // 3D-shaded snowball sphere (slightly larger at peak of arc for perspective depth!)
            val depthScale = 1f + sin(sb.progress.coerceIn(0f, 1f) * PI.toFloat()) * 0.28f
            val finalR = r * depthScale
            paint.shader = RadialGradient(
                sx - finalR * 0.35f, sy - finalR * 0.35f, finalR * 1.45f,
                0xFFFFFFFF.toInt(), 0xFFB8DCF5.toInt(), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(sx, sy, finalR, paint)
            paint.shader = null
        }
    }

    private fun drawSnowplowTruck(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState
    ) {
        val plow = state.plow
        val scale = min(width, height) / 380f
        val truckX = plow.xNorm * width
        val snowY = state.getSnowSurfaceYNorm(plow.xNorm.coerceIn(0f, 1f)) * height + 8f * scale
        val bounceY = sin(plow.bouncePhase) * 1.6f * scale

        canvas.save()
        canvas.translate(truckX, snowY + bounceY)
        canvas.scale(plow.direction.toFloat(), 1f)

        // 1. Volumetric Headlight Beam illuminating snow ahead
        paint.reset()
        paint.isAntiAlias = true
        paint.shader = LinearGradient(
            42f * scale, -18f * scale, 130f * scale, 5f * scale,
            Color.argb(135, 255, 232, 140), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(40f * scale, -20f * scale)
        path.lineTo(135f * scale, -34f * scale)
        path.lineTo(135f * scale, 10f * scale)
        path.lineTo(40f * scale, -8f * scale)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null

        // 2. Contact Shadow
        paint.color = Color.argb(95, 50, 85, 125)
        tmpRect.set(-48f * scale, -4f * scale, 56f * scale, 7f * scale)
        canvas.drawOval(tmpRect, paint)

        // 3. Truck Bed / Salt Hopper (dark steel + bright winter orange)
        paint.shader = LinearGradient(
            0f, -44f * scale, 0f, -12f * scale,
            0xFFF97316.toInt(), 0xFFC2410C.toInt(), Shader.TileMode.CLAMP
        )
        tmpRect.set(-44f * scale, -42f * scale, -4f * scale, -14f * scale)
        canvas.drawRoundRect(tmpRect, 5f * scale, 5f * scale, paint)
        paint.shader = null

        // Diagonal safety stripes on bed
        paint.color = 0xFFFEF08A.toInt()
        tmpRect.set(-40f * scale, -23f * scale, -8f * scale, -17f * scale)
        canvas.drawRoundRect(tmpRect, 2f * scale, 2f * scale, paint)

        // 4. Exhaust Stack
        paint.color = 0xFF94A3B8.toInt()
        tmpRect.set(-10f * scale, -56f * scale, -5f * scale, -38f * scale)
        canvas.drawRoundRect(tmpRect, 2f * scale, 2f * scale, paint)

        // 5. Cab (Crimson 3D-shaded municipal truck cab)
        paint.shader = LinearGradient(
            0f, -48f * scale, 0f, -12f * scale,
            0xFFEF4444.toInt(), 0xFFB91C1C.toInt(), Shader.TileMode.CLAMP
        )
        tmpRect.set(-4f * scale, -48f * scale, 34f * scale, -12f * scale)
        canvas.drawRoundRect(tmpRect, 7f * scale, 7f * scale, paint)
        // Hood nose
        tmpRect.set(22f * scale, -32f * scale, 44f * scale, -12f * scale)
        canvas.drawRoundRect(tmpRect, 6f * scale, 6f * scale, paint)
        paint.shader = null

        // Windshield & Side Window
        paint.shader = LinearGradient(
            0f, -44f * scale, 24f * scale, -28f * scale,
            0xFFBAE6FD.toInt(), 0xFF38BDF8.toInt(), Shader.TileMode.CLAMP
        )
        tmpRect.set(4f * scale, -43f * scale, 28f * scale, -29f * scale)
        canvas.drawRoundRect(tmpRect, 4f * scale, 4f * scale, paint)
        paint.shader = null

        // Headlight lens
        paint.color = 0xFFFFF9C4.toInt()
        canvas.drawCircle(42f * scale, -19f * scale, 3.8f * scale, paint)

        // Flashing Amber Roof Beacon
        val beaconGlow = 0.45f + 0.55f * abs(sin(plow.beaconPhase))
        paint.shader = RadialGradient(
            14f * scale, -52f * scale, 18f * scale * beaconGlow + 4f * scale,
            Color.argb((beaconGlow * 210).toInt(), 255, 195, 20),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(14f * scale, -52f * scale, 18f * scale * beaconGlow + 4f * scale, paint)
        paint.shader = null
        paint.color = 0xFFFBBF24.toInt()
        tmpRect.set(10f * scale, -55f * scale, 18f * scale, -48f * scale)
        canvas.drawRoundRect(tmpRect, 3f * scale, 3f * scale, paint)

        // 6. Heavy-Duty Angled Front Snowplow Blade
        // Hydraulic support arm
        strokePaint.color = 0xFF334155.toInt()
        strokePaint.strokeWidth = 4f * scale
        canvas.drawLine(34f * scale, -16f * scale, 50f * scale, -11f * scale, strokePaint)

        // Curved yellow plow blade
        paint.shader = LinearGradient(
            44f * scale, -32f * scale, 62f * scale, 3f * scale,
            0xFFFACC15.toInt(), 0xFFCA8A04.toInt(), Shader.TileMode.CLAMP
        )
        path.reset()
        path.moveTo(46f * scale, -30f * scale)
        path.quadTo(64f * scale, -15f * scale, 59f * scale, 3f * scale)
        path.lineTo(48f * scale, 3f * scale)
        path.quadTo(53f * scale, -14f * scale, 41f * scale, -26f * scale)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null

        // Rolling white snow wave pushed in front of the blade
        paint.color = Color.argb(235, 250, 253, 255)
        canvas.drawCircle(60f * scale, -2f * scale, 6.5f * scale, paint)
        canvas.drawCircle(65f * scale, 0f * scale, 4.5f * scale, paint)

        // 7. Rotating Rugged Tires
        drawWheel(canvas, -28f * scale, -10f * scale, 10.5f * scale, plow.wheelAngle)
        drawWheel(canvas, 22f * scale, -10f * scale, 10.5f * scale, plow.wheelAngle)

        canvas.restore()
    }

    private fun drawWheel(canvas: Canvas, cx: Float, cy: Float, r: Float, angleDeg: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = 0xFF1E293B.toInt()
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = 0xFF94A3B8.toInt()
        canvas.drawCircle(cx, cy, r * 0.48f, paint)
        strokePaint.color = 0xFF334155.toInt()
        strokePaint.strokeWidth = r * 0.16f
        val rad = angleDeg * PI.toFloat() / 180f
        for (i in 0 until 2) {
            val a = rad + i * (PI.toFloat() * 0.5f)
            val dx = cos(a) * r * 0.42f
            val dy = sin(a) * r * 0.42f
            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, strokePaint)
        }
    }

    private fun drawParticles(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        state: WinterSceneState
    ) {
        paint.reset()
        paint.isAntiAlias = true
        for (p in state.particles) {
            val alpha = ((p.life / p.maxLife).coerceIn(0f, 1f) * 235).toInt()
            paint.color = Color.argb(alpha, 248, 252, 255)
            canvas.drawCircle(p.xNorm * width, p.yNorm * height, p.radiusDp * density, paint)
        }
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = min(255, (Color.red(color) + (255 - Color.red(color)) * factor).toInt())
        val g = min(255, (Color.green(color) + (255 - Color.green(color)) * factor).toInt())
        val b = min(255, (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt())
        return Color.rgb(r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = max(0, (Color.red(color) * (1f - factor)).toInt())
        val g = max(0, (Color.green(color) * (1f - factor)).toInt())
        val b = max(0, (Color.blue(color) * (1f - factor)).toInt())
        return Color.rgb(r, g, b)
    }
}
