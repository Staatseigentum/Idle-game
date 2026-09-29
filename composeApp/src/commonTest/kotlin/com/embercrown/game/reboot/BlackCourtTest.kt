package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlackCourtTest {
    @Test
    fun oldSavesReceiveSafeDefaultsForNewSystems() {
        val old = Json.decodeFromString<RebootState>("""{"embers":1250.0,"levels":{"coalpit":7}}""")
        assertEquals(null, old.patrol)
        assertEquals(0, old.patrolsCompleted)
        assertTrue(old.courtVictories.isEmpty())
        assertTrue(old.blueprintLevels.isEmpty())
        assertEquals(old, Json.decodeFromString<RebootState>(Json.encodeToString(old)))
    }

    @Test
    fun patrolsRequireLodgeAndFinishOfflineWithGuaranteedReward() {
        val base = RebootState(embers = 10_000.0, lifetimeEmbers = 25_000.0,
            levels = mapOf("coalpit" to 10), gloom = 45.0)
        assertEquals(base, beginPatrol(base, "hearth"))
        val ready = base.copy(levels = base.levels + ("scoutlodge" to 1))
        val started = beginPatrol(ready, "hearth")
        assertTrue(started.patrol != null)
        assertEquals(ready.embers - 2_500.0, started.embers)
        assertEquals(started, beginPatrol(started, "hearth"))
        val finished = advanceReboot(started, 65.0, active = false, now = 100L)
        assertEquals(null, finished.patrol)
        assertEquals(1, finished.patrolsCompleted)
        assertEquals(2, finished.fragments["forest"])
        assertTrue(finished.gloom < ready.gloom)
        assertEquals(0.0, finished.playedSeconds)
        assertTrue("patrol_one" in finished.chronicleEntries)
    }

    @Test
    fun courtNeedsSequenceTacticsAndPreparationNotJustEmbers() {
        val prepared = RebootState(
            embers = 5_000_000_000_000.0,
            lifetimeEmbers = 5_000_000_000_000.0,
            eclipseSiegeStage = 3,
            conqueredRegions = setOf("glassfields", "blackpass", "court"),
            craftedArtifacts = setOf("marsh_lantern", "cinder_crown"),
            equippedArtifacts = setOf("marsh_lantern", "cinder_crown"),
            levels = mapOf("lanternwatch" to 10, "shadowfoundry" to 5,
                "courtobservatory" to 5, "eclipsethrone" to 3),
            runPatrols = 3,
            fragments = mapOf("court" to 10),
            gloom = 30.0,
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
        )
        assertFalse(canDefeatCourtLord(prepared, "mirror"))
        assertFalse(canDefeatCourtLord(prepared.copy(courtTactic = "strike"), "gatekeeper"))
        var current = prepared
        BlackCourt.all.forEach { lord ->
            current = current.copy(courtTactic = lord.tactic)
            assertTrue(canDefeatCourtLord(current, lord.id), "${lord.id} should be ready")
            current = defeatCourtLord(current, lord.id)
            assertTrue(lord.id in current.courtVictories)
        }
        assertTrue("sovereign" in current.chronicleEntries)
        val next = performAshRitual(current, now = 200L)
        assertEquals(BlackCourt.all.map { it.id }.toSet(), next.courtVictories)
        assertTrue(next.levels.isEmpty())
    }

    @Test
    fun blueprintOnlyBuysAffordableOwnedTargetsAndSurvivesRitual() {
        val base = RebootState(embers = 300.0, lifetimeEmbers = 300.0,
            blueprintLevels = mapOf("coalpit" to 10, "emberorchard" to 4))
        val built = buildFromBlueprint(base)
        assertTrue(built.level("coalpit") > 0)
        assertTrue(built.embers >= 0.0)
        assertTrue(built.level("coalpit") <= 10)
        assertTrue(built.level("emberorchard") <= 4)
        assertEquals(built, buildFromBlueprint(built))
        val ready = built.copy(levels = built.levels + ("eclipsethrone" to 1),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet())
        assertEquals(base.blueprintLevels, performAshRitual(ready, 10L).blueprintLevels)
    }

    @Test
    fun newRegionOffersOneRealChoicePerReign() {
        val base = RebootState(embers = 5_000_000.0, lifetimeEmbers = 5_000_000.0,
            gloom = 55.0)
        val started = beginExpedition(base, "glassfields", "warden", false)
        assertTrue(started.expedition != null)
        val returned = advanceReboot(started, 120.0, active = false, now = 100L)
        assertEquals("glassfields", returned.pendingMarchEventId)
        assertEquals(returned, beginExpedition(returned, "glassfields", "warden", false))
        val salvage = resolveMarchEvent(returned, 0)
        val ward = resolveMarchEvent(returned, 1)
        assertEquals(returned.fragments.getValue("glassfields") + 3, salvage.fragments["glassfields"])
        assertTrue(ward.gloom < returned.gloom)
        assertTrue(ward.beaconSeconds > 0)
        assertEquals(null, salvage.pendingMarchEventId)
        assertTrue("march_choice" in salvage.chronicleEntries)
        assertEquals(salvage, resolveMarchEvent(salvage, 0))
    }

    @Test
    fun newTrialsHaveExplicitCompletionGoals() {
        val base = RebootState(reign = 2, activeTrialId = "watchfires", runPatrols = 2)
        assertFalse(trialGoalMet(base))
        assertTrue(trialGoalMet(base.copy(runPatrols = 3)))
        val stone = base.copy(activeTrialId = "architect",
            districtLevels = CrownDistricts.all.associate { it.id to 2 })
        assertTrue(trialGoalMet(stone))
        assertFalse(trialGoalMet(stone.copy(districtLevels = mapOf("hearth" to 2))))
        assertTrue(buildingCost(RebootBuildings.byId("coalpit"), 0,
            base.copy(completedTrials = setOf("architect"))) <
            buildingCost(RebootBuildings.byId("coalpit"), 0, base))
    }
}
