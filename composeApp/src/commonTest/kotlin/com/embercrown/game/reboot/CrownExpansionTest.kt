package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrownExpansionTest {
    @Test
    fun existingSaveLoadsWithoutExpansionFields() {
        val oldSave = Json.decodeFromString<RebootState>(
            """{"embers":1250.0,"levels":{"coalpit":7},"reign":2}"""
        )
        assertEquals(1_250.0, oldSave.embers)
        assertEquals(7, oldSave.level("coalpit"))
        assertTrue(oldSave.claimedOrders.isEmpty())
        assertTrue(oldSave.districtLevels.isEmpty())
        assertTrue(oldSave.craftedArtifacts.isEmpty())
        assertEquals(null, oldSave.activeTrialId)
    }

    @Test
    fun ordersGiveOneRewardAndDistrictsBoostOnlyTheirBuildings() {
        val ready = RebootState(embers = 1_000.0, lifetimeEmbers = 1_000.0, runTaps = 20,
            levels = mapOf("coalpit" to 1, "belltower" to 1))
        val claimed = claimRoyalOrder(ready, "first_sparks")
        assertTrue(claimed.embers > ready.embers)
        assertEquals(claimed, claimRoyalOrder(claimed, "first_sparks"))
        assertTrue(production(claimed) > production(ready))

        val district = upgradeDistrict(ready, "hearth")
        assertEquals(1, district.districtLevels["hearth"])
        assertEquals(buildingProduction(RebootBuildings.byId("coalpit"), ready) * 1.08,
            buildingProduction(RebootBuildings.byId("coalpit"), district))
        assertEquals(buildingProduction(RebootBuildings.byId("belltower"), ready),
            buildingProduction(RebootBuildings.byId("belltower"), district))
        val poor = ready.copy(embers = 1.0)
        assertEquals(poor, upgradeDistrict(poor, "hearth"))
    }

    @Test
    fun outpostsAndArtifactsSpendFragmentsAndSurviveRitual() {
        val region = LostMarches.byId("forest")!!
        val base = RebootState(embers = 2_000_000.0, lifetimeEmbers = 2_000_000.0,
            relics = 10, conqueredRegions = setOf("forest"), fragments = mapOf("forest" to 25),
            levels = mapOf("coalpit" to 10))
        val outpost = upgradeOutpost(base, "forest")
        assertEquals(1, outpost.outpostLevels["forest"])
        assertEquals(22, outpost.fragments["forest"])
        assertTrue(expeditionDuration(region, outpost, "warden") < expeditionDuration(region, base, "warden"))
        assertTrue(production(outpost) > production(base))

        val crafted = craftArtifact(outpost, "cinder_crown")
        assertTrue("cinder_crown" in crafted.craftedArtifacts)
        assertEquals(outpost.relics - 3, crafted.relics)
        assertEquals(crafted, craftArtifact(crafted, "cinder_crown"))
        val equipped = toggleArtifact(crafted, "cinder_crown")
        assertTrue(production(equipped) > production(crafted))
        assertEquals(equipped, Json.decodeFromString<RebootState>(Json.encodeToString(equipped)))

        val ritualReady = equipped.copy(gloom = 70.0, levels = equipped.levels + ("eclipsethrone" to 1),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet())
        val next = performAshRitual(ritualReady, now = 100L)
        assertEquals(1, next.outpostLevels["forest"])
        assertTrue("cinder_crown" in next.equippedArtifacts)
        assertTrue(next.districtLevels.isEmpty())
        assertTrue(next.claimedOrders.isEmpty())
    }

    @Test
    fun trialsChangeRulesAndAwardAutomationOnlyOnCompletedRitual() {
        val ready = RebootState(reign = 2, embers = 2_000_000_000_000.0,
            lifetimeEmbers = CROWN_TARGET, gloom = 70.0,
            levels = mapOf("eclipsethrone" to 1, "coalpit" to 20),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet())
        val queued = selectNextTrial(ready, "cinders")
        val trial = performAshRitual(queued, now = 100L)
        assertEquals("cinders", trial.activeTrialId)
        assertFalse("cinders" in trial.completedTrials)
        assertTrue(buildingCost(RebootBuildings.byId("coalpit"), 0, trial) >
            buildingCost(RebootBuildings.byId("coalpit"), 0, trial.copy(activeTrialId = null)))
        val completed = performAshRitual(ready.copy(activeTrialId = "cinders"), now = 200L)
        assertTrue("cinders" in completed.completedTrials)
        val auto = finishPrestige(completed).copy(levels = mapOf("coalpit" to 5), tutorialStep = TUTORIAL_DONE)
        val earned = advanceReboot(auto, 5.0, active = false, now = 205L)
        assertTrue(earned.embers > production(auto) * 5.0)
        assertEquals(0.0, earned.autoStokeSeconds)

        val blocked = ready.copy(activeTrialId = "marches")
        assertFalse(canRitual(blocked))
        assertTrue(canRitual(blocked.copy(conqueredRegions = setOf("forest", "fen"))))
    }

    @Test
    fun eclipseSiegeNeedsDistinctMilestonesAndPaysOnce() {
        val base = RebootState(embers = 2_000_000_000_000.0,
            conqueredRegions = LostMarches.all.map { it.id }.toSet(),
            craftedArtifacts = setOf("cinder_crown"), equippedArtifacts = setOf("cinder_crown"),
            outpostLevels = mapOf("forest" to 3), levels = mapOf("eclipsethrone" to 1), gloom = 60.0)
        assertTrue(canAdvanceEclipseSiege(base))
        val first = advanceEclipseSiege(base)
        val second = advanceEclipseSiege(first)
        val final = advanceEclipseSiege(second)
        assertEquals(3, final.eclipseSiegeStage)
        assertEquals(base.relics + 6, final.relics)
        assertTrue("eclipse_siege" in final.chronicleEntries)
        assertTrue(production(final) > production(final.copy(eclipseSiegeStage = 0)))
        assertEquals(final, advanceEclipseSiege(final))
        assertFalse(canAdvanceEclipseSiege(base.copy(equippedArtifacts = emptySet())))
    }
}
