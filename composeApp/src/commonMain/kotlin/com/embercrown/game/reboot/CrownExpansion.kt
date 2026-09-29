package com.embercrown.game.reboot

import kotlin.math.floor
import kotlin.math.pow

/** Orders give the first reign directed goals without introducing another currency. */
data class RoyalOrder(val id: String, val reward: Double, val ready: (RebootState) -> Boolean)

object RoyalOrders {
    val all = listOf(
        RoyalOrder("first_sparks", 150.0) { it.runTaps >= 20 },
        RoyalOrder("coal_line", 650.0) { it.level("coalpit") >= 10 },
        RoyalOrder("mill_shift", 1_600.0) { it.level("hollowmill") >= 5 },
        RoyalOrder("first_master", 4_000.0) { it.mastery("coalpit") >= 1 },
        RoyalOrder("bell_watch", 18_000.0) { it.level("belltower") >= 3 },
        RoyalOrder("beacon_watch", 35_000.0) { it.runBeacons >= 2 },
        RoyalOrder("first_march", 110_000.0) { it.runExpeditions >= 1 },
        RoyalOrder("first_banner", 500_000.0) { it.conqueredRegions.isNotEmpty() },
        RoyalOrder("first_patrol", 90_000.0) { it.runPatrols >= 1 },
        RoyalOrder("glass_road", 3_000_000.0) { "glassfields" in it.conqueredRegions },
        RoyalOrder("first_choice", 5_000_000.0) { it.runMarchEvents >= 1 },
        RoyalOrder("night_caravan", 28_000_000.0) { it.runPatrols >= 5 },
        RoyalOrder("black_pass", 8_000_000_000.0) { "blackpass" in it.conqueredRegions },
        RoyalOrder("court_watch", 60_000_000_000.0) { it.level("courtobservatory") >= 3 },
        RoyalOrder("last_oath", 200_000_000_000.0) { it.courtVictories.isNotEmpty() },
    )
}

fun royalOrderReward(state: RebootState, order: RoyalOrder): Double =
    maxOf(order.reward, minOf(order.reward * 3.0, rawProduction(state) * 30.0)) *
        (if (state.specializations["ashmarket"] == "utility") 1.2 else 1.0)

fun claimRoyalOrder(state: RebootState, id: String): RebootState {
    val order = RoyalOrders.all.firstOrNull { it.id == id } ?: return state
    if (id in state.claimedOrders || !order.ready(state)) return state
    val reward = royalOrderReward(state, order)
    return withChronicle(state.copy(
        embers = state.embers + reward,
        lifetimeEmbers = state.lifetimeEmbers + reward,
        claimedOrders = state.claimedOrders + id,
    ))
}

/** Districts reset at the ritual, making the visual city a build within each reign. */
data class CrownDistrict(val id: String, val baseCost: Double, val unlockAt: Double, val buildings: Set<String>)

object CrownDistricts {
    const val MAX_LEVEL = 3
    val all = listOf(
        CrownDistrict("hearth", 300.0, 100.0, setOf("coalpit", "emberorchard", "hollowmill")),
        CrownDistrict("bell", 12_000.0, 7_000.0, setOf("lanternwatch", "belltower", "ashmarket", "moonforge")),
        CrownDistrict("bone", 1_000_000.0, 500_000.0, setOf("scoutlodge", "bonelibrary", "citadel", "shadowfoundry")),
        CrownDistrict("storm", 10_000_000_000.0, 5_000_000_000.0, setOf("stormspire", "courtobservatory", "wyrmroost")),
    )
}

fun districtCost(district: CrownDistrict, state: RebootState): Double =
    floor(district.baseCost * 4.0.pow(state.districtLevels[district.id] ?: 0))

fun upgradeDistrict(state: RebootState, id: String): RebootState {
    val district = CrownDistricts.all.firstOrNull { it.id == id } ?: return state
    val level = state.districtLevels[id] ?: 0
    val cost = districtCost(district, state)
    if (level >= CrownDistricts.MAX_LEVEL || state.lifetimeEmbers < district.unlockAt || state.embers < cost) return state
    return withChronicle(state.copy(embers = state.embers - cost,
        districtLevels = state.districtLevels + (id to level + 1)))
}

fun districtMultiplier(buildingId: String, state: RebootState): Double =
    1.0 + CrownDistricts.all.filter { buildingId in it.buildings }
        .sumOf { (state.districtLevels[it.id] ?: 0) * 0.08 }

/** Permanent outposts turn conquests and surplus fragments into lasting progress. */
fun outpostFragmentCost(state: RebootState, regionId: String): Int = 3 + (state.outpostLevels[regionId] ?: 0) * 2

fun outpostEmberCost(state: RebootState, region: LostRegion): Double =
    floor(region.siegeCost * (0.15 + (state.outpostLevels[region.id] ?: 0) * 0.15))

fun upgradeOutpost(state: RebootState, regionId: String): RebootState {
    val region = LostMarches.byId(regionId) ?: return state
    val level = state.outpostLevels[regionId] ?: 0
    val fragments = outpostFragmentCost(state, regionId)
    val embers = outpostEmberCost(state, region)
    if (regionId !in state.conqueredRegions || level >= 3 ||
        (state.fragments[regionId] ?: 0) < fragments || state.embers < embers) return state
    return withChronicle(state.copy(
        embers = state.embers - embers,
        fragments = state.fragments + (regionId to (state.fragments.getValue(regionId) - fragments)),
        outpostLevels = state.outpostLevels + (regionId to level + 1),
    ))
}

fun outpostMultiplier(state: RebootState): Double = 1.05.pow(state.outpostLevels.values.sum())

/** Crafted relics are permanent, but only two may be equipped at a time. */
data class CrownArtifact(val id: String, val regionId: String, val fragmentCost: Int, val relicCost: Int)

object CrownArtifacts {
    val all = listOf(
        CrownArtifact("cinder_crown", "forest", 8, 3),
        CrownArtifact("marsh_lantern", "fen", 9, 4),
        CrownArtifact("tide_compass", "coast", 10, 5),
        CrownArtifact("eclipse_sigil", "ruins", 12, 6),
    )
}

fun craftArtifact(state: RebootState, id: String): RebootState {
    val artifact = CrownArtifacts.all.firstOrNull { it.id == id } ?: return state
    if (id in state.craftedArtifacts || artifact.regionId !in state.conqueredRegions ||
        (state.fragments[artifact.regionId] ?: 0) < artifact.fragmentCost || state.relics < artifact.relicCost) return state
    return withChronicle(state.copy(
        relics = state.relics - artifact.relicCost,
        fragments = state.fragments + (artifact.regionId to
            (state.fragments.getValue(artifact.regionId) - artifact.fragmentCost)),
        craftedArtifacts = state.craftedArtifacts + id,
    ))
}

fun toggleArtifact(state: RebootState, id: String): RebootState = when {
    id !in state.craftedArtifacts -> state
    id in state.equippedArtifacts -> state.copy(equippedArtifacts = state.equippedArtifacts - id)
    state.equippedArtifacts.size >= 2 -> state
    else -> state.copy(equippedArtifacts = state.equippedArtifacts + id)
}

fun hasArtifact(state: RebootState, id: String): Boolean = id in state.equippedArtifacts

/** Trials are chosen for the next reign and only reward a completed Ash Ritual. */
object CrownTrials {
    val all = listOf("cinders", "night", "marches", "watchfires", "architect")
}

fun selectNextTrial(state: RebootState, id: String?): RebootState =
    if (state.reign < 2 || (id != null && id !in CrownTrials.all)) state
    else state.copy(nextTrialId = id)

fun trialGoalMet(state: RebootState): Boolean = when (state.activeTrialId) {
    "marches" -> state.conqueredRegions.size >= 2
    "watchfires" -> state.runPatrols >= 3
    "architect" -> CrownDistricts.all.all { (state.districtLevels[it.id] ?: 0) >= 2 }
    else -> true
}

/** A three-beat optional endgame climax, not a forced delay before ritual. */
val eclipseSiegeCosts = listOf(100_000_000_000.0, 350_000_000_000.0, 900_000_000_000.0)

fun canAdvanceEclipseSiege(state: RebootState): Boolean {
    val stage = state.eclipseSiegeStage
    if (stage !in eclipseSiegeCosts.indices || state.embers < eclipseSiegeCosts[stage]) return false
    return when (stage) {
        0 -> LostMarches.originalIds.all { it in state.conqueredRegions } && state.equippedArtifacts.isNotEmpty()
        1 -> state.outpostLevels.values.sum() >= 3
        else -> state.level("eclipsethrone") >= 1 && state.gloom <= 70.0
    }
}

fun advanceEclipseSiege(state: RebootState): RebootState {
    if (!canAdvanceEclipseSiege(state)) return state
    val stage = state.eclipseSiegeStage
    return withChronicle(state.copy(
        embers = state.embers - eclipseSiegeCosts[stage],
        eclipseSiegeStage = stage + 1,
        relics = state.relics + if (stage == eclipseSiegeCosts.lastIndex) 6 else 0,
        gloom = if (stage == eclipseSiegeCosts.lastIndex) (state.gloom - 30.0).coerceAtLeast(0.0) else state.gloom,
    ))
}
