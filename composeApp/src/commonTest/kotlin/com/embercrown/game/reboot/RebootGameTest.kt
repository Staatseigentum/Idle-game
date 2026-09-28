package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RebootGameTest {
    @Test
    fun buildingsAndSealsFormAnOrderedCampaign() {
        assertEquals(12, RebootBuildings.all.size)
        assertEquals(6, RebootMilestones.all.size)
        assertEquals(CROWN_TARGET, RebootMilestones.all.last().target)
        assertTrue(RebootBuildings.all.zipWithNext().all { (a, b) -> a.baseCost < b.baseCost && a.unlockAt < b.unlockAt })
        assertTrue(RebootMilestones.all.zipWithNext().all { (a, b) -> a.target < b.target })
    }

    @Test
    fun bundleCostAndMaxPurchaseAgree() {
        val coalpit = RebootBuildings.byId("coalpit")
        val state = RebootState(embers = 250.0, lifetimeEmbers = 250.0)
        val count = maxAffordableBuildings(coalpit, state)
        assertTrue(count > 1)
        assertTrue(buildingBundleCost(coalpit, state, count) <= state.embers)
        assertTrue(buildingBundleCost(coalpit, state, count + 1) > state.embers)
    }

    @Test
    fun masteryNeedsOwnedBuildingsAndBoostsOnlyThatBuilding() {
        val coalpit = RebootBuildings.byId("coalpit")
        val before = RebootState(embers = 1_000.0, levels = mapOf("coalpit" to 4))
        assertFalse(canMaster(coalpit, before))
        val eligible = before.copy(levels = mapOf("coalpit" to 5))
        assertTrue(canMaster(coalpit, eligible))
        val mastered = eligible.copy(buildingUpgrades = mapOf("coalpit" to 1))
        assertEquals(buildingProduction(coalpit, eligible) * 2.5, buildingProduction(coalpit, mastered))
    }

    @Test
    fun relicPowersAreSpendableAndPersistentFieldsSurviveSerialization() {
        val power = RelicPowers.all.first { it.id == "cinderheart" }
        val state = RebootState(
            embers = 250.0,
            levels = mapOf("coalpit" to 10),
            buildingUpgrades = mapOf("coalpit" to 1),
            claimedMilestones = setOf("spark"),
            relics = 3,
            relicUpgrades = mapOf("cinderheart" to 2, "foundation" to 1),
        )
        assertEquals(3, relicPowerCost(power, state))
        assertTrue(production(state) > production(state.copy(relicUpgrades = emptyMap())))
        assertEquals(state, Json.decodeFromString<RebootState>(Json.encodeToString(state)))
    }

    @Test
    fun ritualCannotShortCircuitTheFirstRun() {
        val early = RebootState(lifetimeEmbers = 2_000.0, gloom = 100.0)
        assertFalse(canRitual(early))
        val final = early.copy(
            lifetimeEmbers = CROWN_TARGET,
            levels = mapOf("eclipsethrone" to 1),
            claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
        )
        assertTrue(canRitual(final))
        assertEquals(28, ritualReward(final))
        assertFalse(canRitual(final.copy(gloom = 64.9)))
    }

    @Test
    fun omenWaitsForLivePlayAndOffersDifferentConsequences() {
        val ready = RebootState(
            lifetimeEmbers = 150_000.0,
            levels = mapOf("coalpit" to 20),
            omenCountdownSeconds = 1,
            gloom = 50.0,
        )
        val offline = advanceReboot(ready, 300.0, active = false, now = 10L)
        assertEquals(null, offline.pendingOmenId)
        assertEquals(1, offline.omenCountdownSeconds)
        assertEquals(0.0, offline.playedSeconds)

        val pending = advanceReboot(ready, 1.0, active = true, now = 10L)
        assertEquals("caravan", pending.pendingOmenId)
        val supplies = resolveOmen(pending, 0)
        val shelter = resolveOmen(pending, 1)
        assertTrue(supplies.embers > pending.embers)
        assertEquals(0, supplies.omenEffectSeconds)
        assertEquals("ward", shelter.omenEffectId)
        assertTrue(shelter.gloom < pending.gloom)
        assertEquals(1, shelter.omensResolved)
        assertTrue("omen_one" in shelter.chronicleEntries)
        assertEquals(pending, resolveOmen(pending, 9))
    }

    @Test
    fun edictsHaveTradeoffsAndCannotBeInstantlySwitched() {
        val base = RebootState(levels = mapOf("coalpit" to 20))
        val harvest = enactEdict(base, "harvest")
        val ward = enactEdict(base, "ward")
        val rally = enactEdict(base, "rally")
        assertTrue(production(harvest) > production(base))
        assertTrue(production(ward) < production(base))
        assertTrue(tapYield(rally) > tapYield(base))
        assertEquals(harvest, enactEdict(harvest, "ward"))
        val harvestTick = advanceReboot(harvest, 10.0, active = true, now = 10L)
        val wardTick = advanceReboot(ward, 10.0, active = true, now = 10L)
        assertTrue(harvestTick.gloom > wardTick.gloom)
    }

    @Test
    fun chronicleIsEarnedOnceWithoutChangingRitualRewards() {
        val state = withChronicle(RebootState(levels = mapOf("coalpit" to 1, "belltower" to 1), totalTaps = 100))
        assertEquals(3, state.chronicleEntries.size)
        assertEquals(state, withChronicle(state))
        assertEquals(ritualReward(state.copy(chronicleEntries = emptySet())), ritualReward(state))
        assertEquals(state, Json.decodeFromString<RebootState>(Json.encodeToString(state)))
    }

    @Test
    fun offlineReportIsNotStoredAndOfflineDoesNotConsumeOmenTime() {
        val state = RebootState(
            levels = mapOf("coalpit" to 10),
            lifetimeEmbers = 150_000.0,
            omenCountdownSeconds = 30,
            offlineEmbers = 1_000.0,
            offlineSeconds = 3_600,
        )
        val decoded = Json.decodeFromString<RebootState>(Json.encodeToString(state))
        assertEquals(0L, decoded.offlineSeconds)
        assertEquals(0.0, decoded.offlineEmbers)
        val advanced = advanceReboot(decoded, 60.0, active = false, now = 100L)
        assertEquals(30, advanced.omenCountdownSeconds)
        assertEquals(0.0, advanced.runSeconds)
        assertTrue(advanced.embers > decoded.embers)
    }

    @Test
    fun firstReignHasAReasonableActiveLength() {
        var state = RebootState()
        var ritualSecond = -1
        for (second in 1..14_400) {
            if (second <= 1_200) {
                val tap = tapYield(state)
                state = state.copy(embers = state.embers + tap, lifetimeEmbers = state.lifetimeEmbers + tap)
            }
            state = advanceReboot(state, 1.0, active = true, now = second.toLong())
            var seal = nextMilestone(state)
            while (seal != null && state.lifetimeEmbers >= seal.target) {
                state = withChronicle(state.copy(
                    claimedMilestones = state.claimedMilestones + seal.id,
                    relics = state.relics + seal.relicReward,
                ))
                seal = nextMilestone(state)
            }
            val power = RelicPowers.all.first()
            while (state.relicRank(power.id) < power.maxRank && state.relics >= relicPowerCost(power, state)) {
                state = state.copy(
                    relics = state.relics - relicPowerCost(power, state),
                    relicUpgrades = state.relicUpgrades + (power.id to (state.relicRank(power.id) + 1)),
                )
            }
            if ("crown" in state.claimedMilestones) {
                val throne = RebootBuildings.byId("eclipsethrone")
                if (state.level(throne.id) == 0 && state.embers >= buildingCost(throne, 0, state)) {
                    state = state.copy(
                        embers = state.embers - buildingCost(throne, 0, state),
                        levels = state.levels + (throne.id to 1),
                    )
                }
            } else {
                var purchases = 0
                while (purchases < 12) {
                    val choices = buildList {
                        RebootBuildings.all.forEach { building ->
                            if (state.lifetimeEmbers < building.unlockAt) return@forEach
                            val cost = buildingCost(building, state.level(building.id), state)
                            if (state.embers >= cost) {
                                val after = state.copy(levels = state.levels + (building.id to (state.level(building.id) + 1)))
                                add(Triple(building.id, false, (buildingProduction(building, after) - buildingProduction(building, state)) / cost))
                            }
                            if (canMaster(building, state)) {
                                val after = state.copy(buildingUpgrades = state.buildingUpgrades + (building.id to (state.mastery(building.id) + 1)))
                                add(Triple(building.id, true, (buildingProduction(building, after) - buildingProduction(building, state)) / masteryCost(building, state)))
                            }
                        }
                    }
                    val choice = choices.maxByOrNull { it.third } ?: break
                    val building = RebootBuildings.byId(choice.first)
                    state = if (choice.second) state.copy(
                        embers = state.embers - masteryCost(building, state),
                        buildingUpgrades = state.buildingUpgrades + (building.id to (state.mastery(building.id) + 1)),
                    ) else state.copy(
                        embers = state.embers - buildingCost(building, state.level(building.id), state),
                        levels = state.levels + (building.id to (state.level(building.id) + 1)),
                    )
                    purchases++
                }
            }
            if (canRitual(state)) {
                ritualSecond = second
                break
            }
        }
        assertTrue(ritualSecond in 7_200..14_400,
            "First reign took $ritualSecond seconds; earned=${state.lifetimeEmbers}, seals=${state.claimedMilestones.size}, throne=${state.level("eclipsethrone")}")
    }
}
