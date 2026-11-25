package com.example.simulation

import com.example.model.SnowIntensity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class WinterSceneState {
    companion object {
        const val SNOW_COLUMNS = 96
        const val MAX_FLAKES = 250
        const val MAX_DRIFT_RATIO = 0.19f
        const val BASE_SNOW_RATIO = 0.055f
    }

    data class Snowflake(
        var xNorm: Float,
        var yNorm: Float,
        var speedY: Float,
        var radiusDp: Float,
        var layer: Int, // 0 = far, 1 = mid, 2 = foreground crystal
        var swayAmp: Float,
        var swayFreq: Float,
        var swayPhase: Float,
        var rotation: Float,
        var rotSpeed: Float,
        var alpha: Int
    )

    enum class KidAction { WALKING, SCOOPING, WINDUP, THROWING, HIT_REACT }

    data class WinterKid(
        val id: Int,
        var xNorm: Float,
        var targetXNorm: Float,
        var facing: Int, // +1 right, -1 left
        val coatMainColor: Int,
        val coatShadowColor: Int,
        val hatColor: Int,
        val scarfColor: Int,
        var walkPhase: Float,
        var action: KidAction,
        var actionTimer: Float,
        var targetKidIndex: Int,
        var crouchAmount: Float,
        var armSwing: Float,
        var jumpOffsetDp: Float,
        var blinkTimer: Float,
        var scaleFactor: Float
    )

    data class ThrownSnowball(
        var startXNorm: Float,
        var startYNorm: Float,
        var targetXNorm: Float,
        var targetYNorm: Float,
        var progress: Float,
        var duration: Float,
        var arcHeightNorm: Float,
        var targetKidIndex: Int,
        var radiusDp: Float,
        var currentXNorm: Float = startXNorm,
        var currentYNorm: Float = startYNorm
    )

    data class SnowParticle(
        var xNorm: Float,
        var yNorm: Float,
        var vxNorm: Float,
        var vyNorm: Float,
        var radiusDp: Float,
        var life: Float,
        var maxLife: Float
    )

    data class SnowSparkle(
        var xNorm: Float,
        var phase: Float,
        var speed: Float,
        var sizeDp: Float
    )

    data class SnowplowTruck(
        var active: Boolean = false,
        var xNorm: Float = -0.22f,
        var direction: Int = 1, // +1 left->right, -1 right->left
        var speedNorm: Float = 0.14f,
        var wheelAngle: Float = 0f,
        var beaconPhase: Float = 0f,
        var bouncePhase: Float = 0f,
        var autoCooldown: Float = 6.5f,
        var nextDirection: Int = 1
    )

    val snowHeights = FloatArray(SNOW_COLUMNS) { 0.22f }
    val flakes = ArrayList<Snowflake>(MAX_FLAKES)
    val kids = mutableListOf<WinterKid>()
    val snowballs = mutableListOf<ThrownSnowball>()
    val particles = mutableListOf<SnowParticle>()
    val sparkles = mutableListOf<SnowSparkle>()
    val plow = SnowplowTruck()

    var intensity: SnowIntensity = SnowIntensity.NORMAL
    var elapsedTime: Float = 0f
    var windOffset: Float = 0f
    var touchRippleXNorm: Float = -1f
    var touchRippleYNorm: Float = -1f
    var touchRippleAlpha: Float = 0f

    private val rng = Random(2026)

    init {
        repeat(MAX_FLAKES) { idx ->
            val layer = when {
                idx % 7 == 0 -> 2
                idx % 3 == 0 -> 1
                else -> 0
            }
            flakes.add(createFlake(layer = layer, randomY = true))
        }
        repeat(26) {
            sparkles.add(
                SnowSparkle(
                    xNorm = rng.nextFloat(),
                    phase = rng.nextFloat() * (2f * PI.toFloat()),
                    speed = 1.6f + rng.nextFloat() * 2.2f,
                    sizeDp = 2.0f + rng.nextFloat() * 2.5f
                )
            )
        }
        resetCharacters()
    }

    private fun resetCharacters() {
        kids.clear()
        kids.add(
            WinterKid(
                id = 0,
                xNorm = 0.22f,
                targetXNorm = 0.28f,
                facing = 1,
                coatMainColor = 0xFFE63946.toInt(),
                coatShadowColor = 0xFF9E1C28.toInt(),
                hatColor = 0xFFFFB703.toInt(),
                scarfColor = 0xFF38BDF8.toInt(),
                walkPhase = 0f,
                action = KidAction.SCOOPING,
                actionTimer = 0.7f,
                targetKidIndex = 1,
                crouchAmount = 0f,
                armSwing = 0f,
                jumpOffsetDp = 0f,
                blinkTimer = 2.5f,
                scaleFactor = 1.0f
            )
        )
        kids.add(
            WinterKid(
                id = 1,
                xNorm = 0.52f,
                targetXNorm = 0.48f,
                facing = -1,
                coatMainColor = 0xFF2563EB.toInt(),
                coatShadowColor = 0xFF173B8F.toInt(),
                hatColor = 0xFFF43F5E.toInt(),
                scarfColor = 0xFFFDE047.toInt(),
                walkPhase = 1.8f,
                action = KidAction.WALKING,
                actionTimer = 1.3f,
                targetKidIndex = 0,
                crouchAmount = 0f,
                armSwing = 0f,
                jumpOffsetDp = 0f,
                blinkTimer = 1.4f,
                scaleFactor = 0.94f
            )
        )
        kids.add(
            WinterKid(
                id = 2,
                xNorm = 0.78f,
                targetXNorm = 0.74f,
                facing = -1,
                coatMainColor = 0xFF10B981.toInt(),
                coatShadowColor = 0xFF067350.toInt(),
                hatColor = 0xFF7C3AED.toInt(),
                scarfColor = 0xFFFB7185.toInt(),
                walkPhase = 3.2f,
                action = KidAction.WINDUP,
                actionTimer = 0.5f,
                targetKidIndex = 1,
                crouchAmount = 0f,
                armSwing = 0f,
                jumpOffsetDp = 0f,
                blinkTimer = 3.7f,
                scaleFactor = 1.03f
            )
        )
    }

    private fun createFlake(layer: Int, randomY: Boolean): Snowflake {
        val speed = when (layer) {
            0 -> 0.06f + rng.nextFloat() * 0.045f
            1 -> 0.11f + rng.nextFloat() * 0.06f
            else -> 0.15f + rng.nextFloat() * 0.07f
        }
        val radius = when (layer) {
            0 -> 1.6f + rng.nextFloat() * 1.4f
            1 -> 2.8f + rng.nextFloat() * 1.8f
            else -> 4.6f + rng.nextFloat() * 2.8f
        }
        return Snowflake(
            xNorm = rng.nextFloat(),
            yNorm = if (randomY) rng.nextFloat() * 0.9f else -0.02f - rng.nextFloat() * 0.06f,
            speedY = speed,
            radiusDp = radius,
            layer = layer,
            swayAmp = 0.008f + rng.nextFloat() * 0.015f,
            swayFreq = 1.1f + rng.nextFloat() * 1.8f,
            swayPhase = rng.nextFloat() * (2f * PI.toFloat()),
            rotation = rng.nextFloat() * 360f,
            rotSpeed = (rng.nextFloat() - 0.5f) * 90f,
            alpha = when (layer) {
                0 -> 145 + rng.nextInt(55)
                1 -> 195 + rng.nextInt(50)
                else -> 225 + rng.nextInt(30)
            }
        )
    }

    fun triggerSnowplow() {
        if (plow.active) return
        plow.active = true
        plow.direction = plow.nextDirection
        plow.nextDirection = -plow.nextDirection
        plow.xNorm = if (plow.direction > 0) -0.24f else 1.24f
    }

    fun clearAllSnow() {
        for (i in snowHeights.indices) {
            snowHeights[i] = 0.08f
        }
        repeat(35) {
            spawnParticle(
                xNorm = rng.nextFloat(),
                yNorm = 0.85f + rng.nextFloat() * 0.12f,
                vx = (rng.nextFloat() - 0.5f) * 0.25f,
                vy = -0.15f - rng.nextFloat() * 0.22f,
                radiusDp = 2.5f + rng.nextFloat() * 3.5f,
                maxLife = 0.7f
            )
        }
    }

    fun getSnowSurfaceYNorm(xNorm: Float): Float {
        val clamped = xNorm.coerceIn(0f, 1f)
        val exactIdx = clamped * (SNOW_COLUMNS - 1)
        val idx0 = exactIdx.toInt().coerceIn(0, SNOW_COLUMNS - 1)
        val idx1 = (idx0 + 1).coerceIn(0, SNOW_COLUMNS - 1)
        val frac = exactIdx - idx0
        val h = snowHeights[idx0] * (1f - frac) + snowHeights[idx1] * frac
        return 1f - (BASE_SNOW_RATIO + h * MAX_DRIFT_RATIO)
    }

    fun brushSnowAt(xNorm: Float, yNorm: Float) {
        touchRippleXNorm = xNorm
        touchRippleYNorm = yNorm
        touchRippleAlpha = 1f

        if (yNorm > 0.62f) {
            // User is clearing accumulated snow with finger!
            val centerCol = (xNorm * (SNOW_COLUMNS - 1)).toInt()
            val brushCols = 7
            var clearedAny = false
            for (c in (centerCol - brushCols)..(centerCol + brushCols)) {
                if (c in 0 until SNOW_COLUMNS) {
                    val dist = abs(c - centerCol).toFloat() / brushCols
                    val falloff = max(0f, 1f - dist * dist)
                    if (snowHeights[c] > 0.05f) {
                        snowHeights[c] = max(0.05f, snowHeights[c] - 0.18f * falloff)
                        clearedAny = true
                    }
                }
            }
            val burstCount = if (clearedAny) 8 else 4
            repeat(burstCount) {
                spawnParticle(
                    xNorm = xNorm + (rng.nextFloat() - 0.5f) * 0.04f,
                    yNorm = min(yNorm, getSnowSurfaceYNorm(xNorm)),
                    vx = (rng.nextFloat() - 0.5f) * 0.28f,
                    vy = -0.12f - rng.nextFloat() * 0.20f,
                    radiusDp = 2.2f + rng.nextFloat() * 3.2f,
                    maxLife = 0.65f
                )
            }
        } else {
            // User tapped sky/midground -> toss an interactive snowball toward nearest kid!
            if (snowballs.size < 8 && kids.isNotEmpty()) {
                val targetIdx = kids.indices.minByOrNull { abs(kids[it].xNorm - xNorm) } ?: 0
                val targetKid = kids[targetIdx]
                val targetY = getSnowSurfaceYNorm(targetKid.xNorm) - 0.07f
                snowballs.add(
                    ThrownSnowball(
                        startXNorm = xNorm,
                        startYNorm = yNorm,
                        targetXNorm = targetKid.xNorm,
                        targetYNorm = targetY,
                        progress = 0f,
                        duration = 0.55f,
                        arcHeightNorm = 0.07f,
                        targetKidIndex = targetIdx,
                        radiusDp = 5.5f
                    )
                )
            }
        }
    }

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsedTime += dt
        windOffset = sin(elapsedTime * 0.45f) * 0.025f

        if (touchRippleAlpha > 0f) {
            touchRippleAlpha = max(0f, touchRippleAlpha - dt * 2.2f)
        }

        updateFlakes(dt)
        smoothSnowHeights(dt)
        updatePlow(dt)
        updateKids(dt)
        updateSnowballs(dt)
        updateParticles(dt)
        for (s in sparkles) {
            s.phase += dt * s.speed
        }
    }

    private fun updateFlakes(dt: Float) {
        val activeCount = intensity.flakeCount.coerceAtMost(flakes.size)
        val speedMul = intensity.speedMultiplier
        val accRate = intensity.accumulationRate

        for (i in 0 until activeCount) {
            val f = flakes[i]
            f.yNorm += f.speedY * speedMul * dt
            f.xNorm += (sin(elapsedTime * f.swayFreq + f.swayPhase) * f.swayAmp + windOffset) * dt
            f.rotation = (f.rotation + f.rotSpeed * dt) % 360f

            if (f.xNorm < -0.04f) f.xNorm += 1.08f
            if (f.xNorm > 1.04f) f.xNorm -= 1.08f

            val surfaceY = getSnowSurfaceYNorm(f.xNorm.coerceIn(0f, 1f))
            if (f.yNorm >= surfaceY) {
                // Accumulate snow at this column!
                val col = (f.xNorm.coerceIn(0f, 1f) * (SNOW_COLUMNS - 1)).toInt()
                val deposit = (0.0034f + f.radiusDp * 0.00055f) * accRate
                snowHeights[col] = min(1.0f, snowHeights[col] + deposit)
                if (col > 0) snowHeights[col - 1] = min(1.0f, snowHeights[col - 1] + deposit * 0.45f)
                if (col < SNOW_COLUMNS - 1) snowHeights[col + 1] = min(1.0f, snowHeights[col + 1] + deposit * 0.45f)

                // Respawn at the very top of screen
                val fresh = createFlake(f.layer, randomY = false)
                f.xNorm = fresh.xNorm
                f.yNorm = fresh.yNorm
                f.speedY = fresh.speedY
                f.swayPhase = fresh.swayPhase
            }
        }
    }

    private fun smoothSnowHeights(dt: Float) {
        // Gentle avalanche relaxation so snow forms soft smooth drifts
        val blend = min(0.45f, dt * 3.5f)
        for (pass in 0..1) {
            for (i in 1 until SNOW_COLUMNS - 1) {
                val avg = (snowHeights[i - 1] + snowHeights[i] * 2f + snowHeights[i + 1]) * 0.25f
                snowHeights[i] += (avg - snowHeights[i]) * blend
            }
            snowHeights[0] += (snowHeights[1] - snowHeights[0]) * blend * 0.5f
            snowHeights[SNOW_COLUMNS - 1] += (snowHeights[SNOW_COLUMNS - 2] - snowHeights[SNOW_COLUMNS - 1]) * blend * 0.5f
        }
    }

    private fun updatePlow(dt: Float) {
        plow.beaconPhase += dt * 7.5f
        if (!plow.active) {
            plow.autoCooldown = max(0f, plow.autoCooldown - dt)
            if (plow.autoCooldown <= 0f) {
                var sum = 0f
                for (h in snowHeights) sum += h
                val avgHeight = sum / SNOW_COLUMNS
                if (avgHeight > 0.45f) {
                    triggerSnowplow()
                }
            }
            return
        }

        plow.xNorm += plow.direction * plow.speedNorm * dt
        plow.wheelAngle += plow.direction * dt * 420f
        plow.bouncePhase += dt * 14f

        // Front blade position in normalized coordinates
        val bladeX = plow.xNorm + plow.direction * 0.11f
        val bladeMin = (min(bladeX - 0.04f, bladeX + 0.04f)).coerceIn(0f, 1f)
        val bladeMax = (max(bladeX - 0.04f, bladeX + 0.04f)).coerceIn(0f, 1f)
        val colStart = (bladeMin * (SNOW_COLUMNS - 1)).toInt()
        val colEnd = (bladeMax * (SNOW_COLUMNS - 1)).toInt()

        var shavedAmount = 0f
        for (c in colStart..colEnd) {
            val current = snowHeights[c]
            val clearedTarget = 0.07f
            if (current > clearedTarget) {
                val shave = min(current - clearedTarget, dt * 1.45f)
                snowHeights[c] = current - shave
                shavedAmount += shave
            }
        }

        // Spray snow particles off the plow blade!
        if (shavedAmount > 0.001f || rng.nextFloat() < 0.55f) {
            val bladeSurfaceY = getSnowSurfaceYNorm(bladeX.coerceIn(0f, 1f))
            repeat(if (shavedAmount > 0.01f) 3 else 1) {
                spawnParticle(
                    xNorm = bladeX.coerceIn(0f, 1f) + plow.direction * 0.015f,
                    yNorm = bladeSurfaceY - 0.01f,
                    vx = plow.direction * (0.14f + rng.nextFloat() * 0.16f),
                    vy = -0.14f - rng.nextFloat() * 0.18f,
                    radiusDp = 2.4f + rng.nextFloat() * 3.0f,
                    maxLife = 0.65f
                )
            }
        }

        // Exhaust smoke puffs
        if (rng.nextFloat() < dt * 8f) {
            val stackX = plow.xNorm - plow.direction * 0.03f
            val stackY = getSnowSurfaceYNorm(plow.xNorm.coerceIn(0f, 1f)) - 0.11f
            spawnParticle(
                xNorm = stackX,
                yNorm = stackY,
                vx = -plow.direction * 0.04f + (rng.nextFloat() - 0.5f) * 0.02f,
                vy = -0.07f - rng.nextFloat() * 0.04f,
                radiusDp = 3.2f + rng.nextFloat() * 2.6f,
                maxLife = 0.9f
            )
        }

        if ((plow.direction > 0 && plow.xNorm > 1.28f) || (plow.direction < 0 && plow.xNorm < -0.28f)) {
            plow.active = false
            plow.autoCooldown = 10f
        }
    }

    private fun updateKids(dt: Float) {
        for (i in kids.indices) {
            val kid = kids[i]
            kid.blinkTimer -= dt
            if (kid.blinkTimer <= -0.14f) {
                kid.blinkTimer = 2.2f + rng.nextFloat() * 3.0f
            }

            kid.actionTimer -= dt
            when (kid.action) {
                KidAction.WALKING -> {
                    kid.crouchAmount = max(0f, kid.crouchAmount - dt * 4f)
                    kid.jumpOffsetDp = max(0f, kid.jumpOffsetDp - dt * 24f)
                    val dx = kid.targetXNorm - kid.xNorm
                    if (abs(dx) > 0.012f) {
                        val step = 0.095f * dt * if (dx > 0) 1f else -1f
                        kid.xNorm = (kid.xNorm + step).coerceIn(0.11f, 0.89f)
                        kid.facing = if (dx > 0) 1 else -1
                        kid.walkPhase += dt * 10f
                        kid.armSwing = sin(kid.walkPhase) * 28f
                    } else {
                        kid.armSwing *= 0.85f
                    }
                    if (kid.actionTimer <= 0f) {
                        kid.action = KidAction.SCOOPING
                        kid.actionTimer = 0.65f
                        // Pick another kid to throw at
                        val others = kids.indices.filter { it != i }
                        kid.targetKidIndex = others.random(rng)
                        kid.facing = if (kids[kid.targetKidIndex].xNorm >= kid.xNorm) 1 else -1
                    }
                }
                KidAction.SCOOPING -> {
                    kid.crouchAmount = min(1f, kid.crouchAmount + dt * 4.5f)
                    kid.armSwing = -40f * kid.crouchAmount
                    if (kid.actionTimer <= 0f) {
                        // Little snow scoop puff
                        val surfaceY = getSnowSurfaceYNorm(kid.xNorm)
                        repeat(4) {
                            spawnParticle(
                                xNorm = kid.xNorm + kid.facing * 0.02f,
                                yNorm = surfaceY - 0.01f,
                                vx = (rng.nextFloat() - 0.5f) * 0.09f,
                                vy = -0.07f - rng.nextFloat() * 0.06f,
                                radiusDp = 2.0f + rng.nextFloat() * 1.8f,
                                maxLife = 0.45f
                            )
                        }
                        kid.action = KidAction.WINDUP
                        kid.actionTimer = 0.36f
                    }
                }
                KidAction.WINDUP -> {
                    kid.crouchAmount = max(0f, kid.crouchAmount - dt * 5f)
                    kid.facing = if (kids[kid.targetKidIndex].xNorm >= kid.xNorm) 1 else -1
                    kid.armSwing = -115f
                    if (kid.actionTimer <= 0f) {
                        kid.action = KidAction.THROWING
                        kid.actionTimer = 0.32f
                        launchSnowball(fromIdx = i, toIdx = kid.targetKidIndex)
                    }
                }
                KidAction.THROWING -> {
                    kid.armSwing = 65f
                    if (kid.actionTimer <= 0f) {
                        kid.action = KidAction.WALKING
                        kid.actionTimer = 1.6f + rng.nextFloat() * 1.6f
                        val minZone = when (i) {
                            0 -> 0.12f
                            1 -> 0.38f
                            else -> 0.64f
                        }
                        val maxZone = when (i) {
                            0 -> 0.34f
                            1 -> 0.60f
                            else -> 0.88f
                        }
                        kid.targetXNorm = minZone + rng.nextFloat() * (maxZone - minZone)
                    }
                }
                KidAction.HIT_REACT -> {
                    kid.jumpOffsetDp = sin((0.55f - kid.actionTimer).coerceAtLeast(0f) / 0.55f * PI.toFloat()) * 14f
                    kid.armSwing = 45f * sin(elapsedTime * 18f)
                    if (kid.actionTimer <= 0f) {
                        kid.jumpOffsetDp = 0f
                        kid.action = KidAction.SCOOPING
                        kid.actionTimer = 0.55f
                    }
                }
            }
        }
    }

    private fun launchSnowball(fromIdx: Int, toIdx: Int) {
        val thrower = kids.getOrNull(fromIdx) ?: return
        val target = kids.getOrNull(toIdx) ?: return
        val startX = thrower.xNorm + thrower.facing * 0.028f
        val startY = getSnowSurfaceYNorm(thrower.xNorm) - 0.085f
        val targetX = target.xNorm + (rng.nextFloat() - 0.5f) * 0.025f
        val targetY = getSnowSurfaceYNorm(target.xNorm) - 0.065f
        val dist = abs(targetX - startX)
        snowballs.add(
            ThrownSnowball(
                startXNorm = startX,
                startYNorm = startY,
                targetXNorm = targetX,
                targetYNorm = targetY,
                progress = 0f,
                duration = (0.48f + dist * 0.55f).coerceIn(0.45f, 0.95f),
                arcHeightNorm = 0.07f + dist * 0.14f,
                targetKidIndex = toIdx,
                radiusDp = 5.2f
            )
        )
    }

    private fun updateSnowballs(dt: Float) {
        val iter = snowballs.iterator()
        while (iter.hasNext()) {
            val sb = iter.next()
            sb.progress += dt / sb.duration
            val t = sb.progress.coerceIn(0f, 1f)
            val linearX = sb.startXNorm + (sb.targetXNorm - sb.startXNorm) * t
            val linearY = sb.startYNorm + (sb.targetYNorm - sb.startYNorm) * t
            val arc = 4f * t * (1f - t) * sb.arcHeightNorm
            sb.currentXNorm = linearX
            sb.currentYNorm = linearY - arc

            if (sb.progress >= 1f) {
                // Impact burst!
                repeat(14) {
                    spawnParticle(
                        xNorm = sb.currentXNorm,
                        yNorm = sb.currentYNorm,
                        vx = (rng.nextFloat() - 0.5f) * 0.28f,
                        vy = -0.10f - rng.nextFloat() * 0.20f,
                        radiusDp = 2.0f + rng.nextFloat() * 2.8f,
                        maxLife = 0.55f
                    )
                }
                val hitKid = kids.getOrNull(sb.targetKidIndex)
                if (hitKid != null && abs(hitKid.xNorm - sb.currentXNorm) < 0.08f) {
                    hitKid.action = KidAction.HIT_REACT
                    hitKid.actionTimer = 0.55f
                    hitKid.targetKidIndex = kids.indices.filter { it != sb.targetKidIndex }.random(rng)
                }
                iter.remove()
            }
        }
    }

    private fun spawnParticle(
        xNorm: Float,
        yNorm: Float,
        vx: Float,
        vy: Float,
        radiusDp: Float,
        maxLife: Float
    ) {
        if (particles.size > 140) return
        particles.add(
            SnowParticle(
                xNorm = xNorm,
                yNorm = yNorm,
                vxNorm = vx,
                vyNorm = vy,
                radiusDp = radiusDp,
                life = maxLife,
                maxLife = maxLife
            )
        )
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.vyNorm += 0.42f * dt // gravity
            p.xNorm += p.vxNorm * dt
            p.yNorm += p.vyNorm * dt
        }
    }
}
