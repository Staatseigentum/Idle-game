package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LostMarchesTest {
    @Test
    fun expeditionIsOneAtATimeAndCompletesOfflineOnce() {
        val base = RebootState(embers = 200_000.0, lifetimeEmbers = 200_000.0)
        assertEquals(base, beginExpedition(base, "ruins", "warden", false))
        assertEquals(base, beginExpedition(base, "forest", "unknown", false))
        val sent = beginExpedition(base, "forest", "warden", false)
        assertEquals(175_000.0, sent.embers)
        assertEquals(sent, beginExpedition(sent, "forest", "scout", true))
        val resumed = advanceReboot(sent, 100.0, active = false, now = 100L)
        assertNull(resumed.expedition)
        assertEquals(4, resumed.fragments["forest"])
        assertEquals(1, resumed.expeditionsCompleted)
        assertEquals(0.0, resumed.playedSeconds)
        assertEquals(1, advanceReboot(resumed, 100.0, active = false, now = 200L).expeditionsCompleted)
        assertTrue("march_scout" in resumed.chronicleEntries)
    }

    @Test
    fun rolesAndRiskHaveDistinctOutcomes() {
        val region = LostMarches.all.first()
        val state = RebootState()
        assertTrue(expeditionDuration(region, state, "scout") < expeditionDuration(region, state, "warden"))
        assertTrue(expeditionDuration(region, state, "occultist") > expeditionDuration(region, state, "warden"))
        assertEquals(2, expeditionReward(MarchExpedition("forest", "scout", false, 0, 0), state))
        assertEquals(4, expeditionReward(MarchExpedition("forest", "warden", false, 0, 0), state))
        assertEquals(1, expeditionReward(MarchExpedition("forest", "warden", true, 0, 0), state))
        assertEquals(6, expeditionReward(MarchExpedition("forest", "warden", true, 0, 1), state))
    }

    @Test
    fun siegesAndSpecializationsSpendFragmentsAndCannotRepeat() {
        val region = LostMarches.all.first()
        val base = RebootState(
            embers = 500_000.0, lifetimeEmbers = 500_000.0,
            levels = mapOf("belltower" to 3, "coalpit" to 5),
            buildingUpgrades = mapOf("coalpit" to 1),
            fragments = mapOf("forest" to 9),
        )
        assertTrue(canConquer(base, region))
        assertFalse(canConquer(base.copy(levels = mapOf("belltower" to 2)), region))
        val industry = specializeBuilding(base, "coalpit", "industry")
        assertTrue(buildingProduction(RebootBuildings.byId("coalpit"), industry) >
            buildingProduction(RebootBuildings.byId("coalpit"), base))
        val guarded = specializeBuilding(base, "coalpit", "utility")
        assertEquals(6, guarded.fragments["forest"])
        assertEquals(guarded, specializeBuilding(guarded, "coalpit", "industry"))
        assertTrue(advanceReboot(guarded, 10.0, true, 10).gloom < advanceReboot(base, 10.0, true, 10).gloom)
        val won = conquerRegion(guarded, "forest")
        assertEquals(150_000.0, won.embers)
        assertEquals(0, won.fragments["forest"])
        assertEquals(2, won.relics)
        assertTrue("forest" in won.conqueredRegions)
        assertTrue(production(won) > production(guarded))
        assertEquals(won, conquerRegion(won, "forest"))
        assertTrue("march_conquer" in won.chronicleEntries)
        assertTrue("march_specialist" in won.chronicleEntries)
    }

    @Test
    fun relicSetsUnlockAfterRitualAndMarchesResetButHistoryRemains() {
        val final = RebootState(
            lifetimeEmbers = CROWN_TARGET, gloom = 85.0,
            levels = mapOf("eclipsethrone" to 1),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
            fragments = mapOf("forest" to 5), conqueredRegions = setOf("forest"),
            expedition = MarchExpedition("fen", "scout", false, 20, 4),
            expeditionsCompleted = 4,
            specializations = mapOf("coalpit" to "industry"),
        )
        assertEquals(final, equipRelicSet(final, "emberguard"))
        val newReign = performAshRitual(final, 100L)
        assertEquals(2, newReign.reign)
        assertTrue(newReign.fragments.isEmpty())
        assertTrue(newReign.conqueredRegions.isEmpty())
        assertTrue(newReign.specializations.isEmpty())
        assertNull(newReign.expedition)
        assertEquals(4, newReign.expeditionsCompleted)
        val equipped = equipRelicSet(newReign, "wayfarer")
        assertEquals("wayfarer", equipped.relicSetId)
        assertEquals(equipped, equipRelicSet(equipped, "emberguard"))
        assertEquals(equipped, Json.decodeFromString<RebootState>(Json.encodeToString(equipped)))
    }
}
