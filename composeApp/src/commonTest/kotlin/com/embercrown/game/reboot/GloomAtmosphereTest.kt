package com.embercrown.game.reboot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GloomAtmosphereTest {
    @Test
    fun visualStagesGrowWithGloom() {
        val boundaries = listOf(0.0 to 0, 14.9 to 0, 15.0 to 1, 34.9 to 1,
            35.0 to 2, 54.9 to 2, 55.0 to 3, 74.9 to 3,
            75.0 to 4, 89.9 to 4, 90.0 to 5, 100.0 to 5)
        boundaries.forEach { (gloom, stage) -> assertEquals(stage, gloomVisuals(gloom).stage) }
    }

    @Test
    fun pressureIsBoundedAndMonotonic() {
        val pressures = (-10..110 step 5).map { gloomVisuals(it.toDouble()).pressure }
        assertEquals(0f, pressures.first())
        assertEquals(1f, pressures.last())
        assertTrue(pressures.zipWithNext().all { (first, second) -> first <= second })
    }
}
