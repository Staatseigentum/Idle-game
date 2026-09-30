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
        val won = conquerRegion(guarded, "forest", 0.0)
        assertEquals(150_000.0, won.embers)
        assertEquals(0, won.fragments["forest"])
        assertEquals(2, won.relics)
        assertTrue("forest" in won.conqueredRegions)
        assertTrue(production(won) > production(guarded))
        assertEquals(won, conquerRegion(won, "forest", 0.0))
        assertTrue("march_conquer" in won.chronicleEntries)
        assertTrue("march_specialist" in won.chronicleEntries)
    }

    @Test
    fun siegeFailureHasVisibleRegionSpecificCostsAndImprovesNextAttempt() {
        val region = LostMarches.byId("forest")!!
        val base = RebootState(embers = 1_000_000.0, lifetimeEmbers = 1_000_000.0,
            levels = mapOf("belltower" to 3), fragments = mapOf("forest" to 10), gloom = 90.0)
        assertEquals(.80, siegeWinChance(base, region), .0001)
        val lost = conquerRegion(base, "forest", .99)
        assertTrue(lost.conqueredRegions.isEmpty())
        assertEquals(580_000.0, lost.embers)
        assertEquals(9, lost.fragments["forest"])
        assertEquals(100.0, lost.gloom)
        assertEquals(0, lost.relics)
        assertEquals(10, lost.lastSiegeResult?.gloomGained)
        assertEquals(1, lost.siegeFailures["forest"])
        assertEquals(.88, siegeWinChance(lost, region), .0001)
        val reloaded = Json.decodeFromString<RebootState>(Json.encodeToString(lost))
        assertEquals(lost, reloaded)
        val won = conquerRegion(lost, "forest", .0)
        assertEquals(230_000.0, won.embers)
        assertEquals(3, won.fragments["forest"])
        assertEquals(2, won.relics)
        assertTrue(won.lastSiegeResult?.won == true)
        assertEquals(won, conquerRegion(won, "forest", .99))
    }

    @Test
    fun everyRegionHasItsOwnSiegeRiskAndNoBalanceGoesNegative() {
        assertEquals(7, LostMarches.all.map { it.failureGloom }.toSet().size)
        LostMarches.all.forEach { region ->
            val base = RebootState(embers = region.siegeCost, lifetimeEmbers = region.unlockAt,
                levels = mapOf(region.defenderId to region.defenderLevel),
                fragments = mapOf(region.id to 6), gloom = 70.0)
            val lost = conquerRegion(base, region.id, .99)
            assertEquals(0.0, lost.embers)
            assertEquals(6 - region.failureFragments, lost.fragments[region.id])
            assertEquals((70.0 + region.failureGloom).coerceAtMost(100.0), lost.gloom)
            assertFalse(region.id in lost.conqueredRegions)
        }
    }

    @Test
    fun recoveredMarchUnlocksThreeBuildingForgeRanksForThisReign() {
        assertEquals(RebootBuildings.all.map { it.id }.toSet(), marchUpgradeRegions.keys)
        val coalpit = RebootBuildings.byId("coalpit")
        val locked = RebootState(embers = 2_000_000.0, levels = mapOf("coalpit" to 10),
            fragments = mapOf("forest" to 20))
        assertFalse(canBuyMarchUpgrade(locked, coalpit))
        assertEquals(locked, buyMarchUpgrade(locked, "coalpit"))
        var current = locked.copy(conqueredRegions = setOf("forest"))
        val originalOutput = buildingProduction(coalpit, current)
        repeat(MAX_MARCH_UPGRADE_RANK) { rank ->
            assertTrue(canBuyMarchUpgrade(current, coalpit))
            val next = buyMarchUpgrade(current, "coalpit")
            assertEquals(rank + 1, next.marchUpgradeRanks["coalpit"])
            assertEquals(1.35, buildingProduction(coalpit, next) / buildingProduction(coalpit, current), .0001)
            current = next
        }
        assertFalse(canBuyMarchUpgrade(current, coalpit))
        assertEquals(current, buyMarchUpgrade(current, "coalpit"))
        assertTrue(buildingProduction(coalpit, current) > originalOutput * 2)
        val restored = Json.decodeFromString<RebootState>(Json.encodeToString(current))
        assertEquals(current.marchUpgradeRanks, restored.marchUpgradeRanks)
        assertEquals(current, restored)
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
            siegeFailures = mapOf("fen" to 2),
            lastSiegeResult = SiegeResult("fen", false, 10.0, 1, 20),
            marchUpgradeRanks = mapOf("coalpit" to 2),
        )
        assertEquals(final, equipRelicSet(final, "emberguard"))
        val newReign = performAshRitual(final, 100L)
        assertEquals(2, newReign.reign)
        assertTrue(newReign.fragments.isEmpty())
        assertTrue(newReign.conqueredRegions.isEmpty())
        assertTrue(newReign.specializations.isEmpty())
        assertTrue(newReign.siegeFailures.isEmpty())
        assertNull(newReign.lastSiegeResult)
        assertTrue(newReign.marchUpgradeRanks.isEmpty())
        assertNull(newReign.expedition)
        assertEquals(4, newReign.expeditionsCompleted)
        val equipped = equipRelicSet(newReign, "wayfarer")
        assertEquals("wayfarer", equipped.relicSetId)
        assertEquals("emberguard", equipRelicSet(equipped, "emberguard").relicSetId)
        val resumed = finishPrestige(equipped)
        assertEquals(resumed, equipRelicSet(resumed, "emberguard"))
        assertEquals(equipped, Json.decodeFromString<RebootState>(Json.encodeToString(equipped)))
    }
}
