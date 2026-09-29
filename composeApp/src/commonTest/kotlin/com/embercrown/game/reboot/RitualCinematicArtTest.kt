package com.embercrown.game.reboot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RitualCinematicArtTest {
    @Test
    fun eachBeatHasDistinctVisiblePixelArt() {
        val frames = listOf(0f to 0, 2.4f to 43, 3.5f to 63, 5.7f to 102)
            .map { (seconds, frame) -> ritualCinematicArt(seconds, frame) }
        frames.forEach { art ->
            assertEquals(160, art.width)
            assertEquals(120, art.height)
            assertTrue((0 until art.height).sumOf { y ->
                (0 until art.width).count { x -> art.colorAt(x, y) != null }
            } > 20)
        }
        val signatures = frames.map { art ->
            (0 until art.height).fold(1) { acc, y ->
                (0 until art.width).fold(acc) { row, x -> 31 * row + (art.colorAt(x, y)?.hashCode() ?: 0) }
            }
        }
        assertEquals(signatures.size, signatures.toSet().size)
    }
}
