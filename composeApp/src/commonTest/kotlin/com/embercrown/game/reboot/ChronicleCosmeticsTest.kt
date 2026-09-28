package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChronicleCosmeticsTest {
    @Test
    fun earnedLooksAreSelectableAndOnlyChangePresentation() {
        val state = RebootState(levels = mapOf("coalpit" to 10),
            chronicleEntries = setOf("beacon", "first_mastery", "march_conquer"))
        val locked = selectCosmetic(state, "flame", "ghostfire")
        assertEquals(state, locked)
        val selected = selectCosmetic(state, "flame", "moonfire")
        assertEquals("moonfire", cosmeticStyle(selected, "flame"))
        assertNotEquals(state.cosmeticStyles, selected.cosmeticStyles)
        assertEquals(production(state), production(selected))
        assertEquals(tapYield(state), tapYield(selected))
        assertEquals(ritualReward(state), ritualReward(selected))
        assertEquals(state, selectCosmetic(state, "unknown", "moonfire"))
    }

    @Test
    fun trophyShelfCanBeCuratedAndCleared() {
        val ids = CrownChronicle.all.take(5).map { it.id }
        val state = RebootState(chronicleEntries = ids.toSet())
        assertEquals(ids.take(4), featuredTrophies(state))
        val withFifth = toggleFeaturedTrophy(state, ids[4])
        assertEquals(listOf(ids[1], ids[2], ids[3], ids[4]), featuredTrophies(withFifth))
        val cleared = ids.drop(1).fold(withFifth) { current, id -> toggleFeaturedTrophy(current, id) }
        assertTrue(featuredTrophies(cleared).isEmpty())
        assertEquals(cleared, toggleFeaturedTrophy(cleared, "unearned"))
    }

    @Test
    fun cosmeticsAndTrophiesSurviveRitualAndOldSavesStillLoad() {
        val state = RebootState(
            chronicleEntries = setOf("beacon", "first_mastery"),
            cosmeticStyles = mapOf("flame" to "moonfire", "banner" to "master"),
            featuredTrophies = listOf("beacon"),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
            levels = mapOf("eclipsethrone" to 1),
            gloom = 75.0,
        )
        val next = performAshRitual(state, now = 100L)
        assertEquals("moonfire", cosmeticStyle(next, "flame"))
        assertEquals("master", cosmeticStyle(next, "banner"))
        assertEquals(listOf("beacon"), featuredTrophies(next))
        assertEquals(next, Json.decodeFromString<RebootState>(Json.encodeToString(next)))
        val old = Json.decodeFromString<RebootState>("""{"chronicleEntries":["beacon"]}""")
        assertEquals("ember", cosmeticStyle(old, "flame"))
        assertEquals(listOf("beacon"), featuredTrophies(old))
    }

    @Test
    fun unlockedLooksActuallyChangePixelArt() {
        val ember = ashKingdomMotionArt(emptyMap(), 0, false, 0)
        val moonfire = ashKingdomMotionArt(emptyMap(), 0, false, 0, flameLook = "moonfire")
        assertNotEquals(ember.colorAt(78, 90), moonfire.colorAt(78, 90))

        val blood = ashKingdomArt(emptyMap(), 0, false)
        val storm = ashKingdomArt(emptyMap(), 0, false, skyLook = "storm")
        assertNotEquals(blood.colorAt(0, 0), storm.colorAt(0, 0))

        val iron = lostMarchesArt(emptySet())
        val gilded = lostMarchesArt(emptySet(), frameLook = "gilded")
        assertNotEquals(iron.colorAt(1, 1), gilded.colorAt(1, 1))
    }
}
