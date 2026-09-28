package com.embercrown.game.reboot

import com.embercrown.game.audio.AshAudioBank
import com.embercrown.game.audio.SfxId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AshAudioBankTest {
    @Test
    fun allAshActionsHaveAudibleClips() {
        val expected = listOf(
            SfxId.ASH_TAP, SfxId.ASH_BUILD, SfxId.ASH_MASTERY, SfxId.ASH_RELIC,
            SfxId.ASH_BEACON, SfxId.ASH_MARCH, SfxId.ASH_RITUAL, SfxId.ASH_OMEN,
            SfxId.ASH_GUIDE, SfxId.ASH_PAGE,
        )
        assertEquals(expected.toSet(), AshAudioBank.effects.keys)
        expected.forEach { id ->
            val clip = AshAudioBank.effects.getValue(id)
            assertEquals(22_050, clip.sampleRateHz)
            assertTrue(clip.samples.size in 900..30_000, "$id duration")
            assertTrue(clip.samples.any { it != 0.toShort() }, "$id is silent")
            assertTrue(kotlin.math.abs(clip.samples.last().toInt()) < 400, "$id ends abruptly")
        }
    }

    @Test
    fun ambienceIsQuietAndLoopSafe() {
        val ambience = AshAudioBank.ambience
        assertEquals(16 * 22_050, ambience.samples.size)
        assertEquals(0, ambience.samples.first().toInt())
        assertTrue(kotlin.math.abs(ambience.samples.last().toInt()) < 10)
        assertTrue(ambience.samples.any { it != 0.toShort() })
    }
}
