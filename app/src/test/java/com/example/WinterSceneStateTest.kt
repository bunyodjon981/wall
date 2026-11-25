package com.example

import com.example.model.SnowIntensity
import com.example.simulation.WinterSceneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinterSceneStateTest {

    @Test
    fun snowAccumulatesOverTimeFromTop() {
        val state = WinterSceneState()
        state.intensity = SnowIntensity.HEAVY
        val initialAvg = state.snowHeights.average()

        repeat(240) {
            state.update(0.03f)
        }

        val afterAvg = state.snowHeights.average()
        assertTrue("Snow should accumulate or trigger plow over time", afterAvg != initialAvg)
    }

    @Test
    fun userFingerBrushReducesSnowAndCreatesParticles() {
        val state = WinterSceneState()
        for (i in state.snowHeights.indices) {
            state.snowHeights[i] = 0.7f
        }
        val beforeCenter = state.snowHeights[WinterSceneState.SNOW_COLUMNS / 2]

        state.brushSnowAt(xNorm = 0.5f, yNorm = 0.85f)

        val afterCenter = state.snowHeights[WinterSceneState.SNOW_COLUMNS / 2]
        assertTrue("Brushing snow with finger should lower snow height", afterCenter < beforeCenter)
        assertTrue("Brushing snow should emit splash particles", state.particles.isNotEmpty())
    }

    @Test
    fun snowplowDrivesAcrossAndClearsSnow() {
        val state = WinterSceneState()
        for (i in state.snowHeights.indices) {
            state.snowHeights[i] = 0.8f
        }
        state.triggerSnowplow()
        assertTrue("Plow should be active after trigger", state.plow.active)

        val initialSum = state.snowHeights.sum()
        // Run enough simulation steps for plow to cross the screen
        repeat(400) {
            state.update(0.03f)
        }

        assertFalse("Plow should finish crossing screen", state.plow.active)
        assertTrue("Plow should reduce total snow across screen", state.snowHeights.sum() < initialSum)
    }

    @Test
    fun kidsPlayAndThrowSnowballs() {
        val state = WinterSceneState()
        assertEquals(3, state.kids.size)

        var sawSnowball = false
        repeat(200) {
            state.update(0.03f)
            if (state.snowballs.isNotEmpty()) {
                sawSnowball = true
            }
        }
        assertTrue("3D kids should throw snowballs at each other", sawSnowball)
    }
}
