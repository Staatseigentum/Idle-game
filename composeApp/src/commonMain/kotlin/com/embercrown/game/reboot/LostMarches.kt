package com.embercrown.game.reboot

import kotlinx.serialization.Serializable
import kotlin.math.ceil

/** The marches are optional during the first reign and become a second progression loop later. */
data class LostRegion(
    val id: String,
    val unlockAt: Double,
    val expeditionCost: Double,
    val expeditionSeconds: Int,
    val siegeCost: Double,
    val defenderId: String,
    val defenderLevel: Int,
    val x: Int,
    val y: Int,
)

object LostMarches {
    val all = listOf(
        LostRegion("forest", 150_000.0, 25_000.0, 90, 350_000.0, "belltower", 3, 37, 72),
        LostRegion("fen", 12_000_000.0, 2_000_000.0, 150, 30_000_000.0, "moonforge", 3, 100, 83),
        LostRegion("coast", 1_000_000_000.0, 160_000_000.0, 210, 2_500_000_000.0, "soulharbor", 1, 50, 35),
        LostRegion("ruins", 90_000_000_000.0, 14_000_000_000.0, 270, 200_000_000_000.0, "citadel", 5, 119, 38),
    )

    fun byId(id: String): LostRegion? = all.firstOrNull { it.id == id }
}

/** One expedition at a time; its end time is advanced by the same live/offline clock as buildings. */
@Serializable
data class MarchExpedition(
    val regionId: String,
    val roleId: String,
    val daring: Boolean,
    val remainingSeconds: Int,
    val sequence: Int,
)

fun expeditionDuration(region: LostRegion, state: RebootState, roleId: String): Int {
    val role = when (roleId) { "scout" -> 0.7; "occultist" -> 1.25; else -> 1.0 }
    val pathfinder = if (state.specializations["moonforge"] == "utility") 0.8 else 1.0
    val relicSet = if (state.relicSetId == "wayfarer") 0.85 else 1.0
    val outpost = 1.0 - (state.outpostLevels[region.id] ?: 0) * 0.05
    val compass = if (hasArtifact(state, "tide_compass")) 0.8 else 1.0
    val trial = if (state.activeTrialId == "marches") 1.4 else 1.0
    return ceil(region.expeditionSeconds * role * pathfinder * relicSet * outpost * compass * trial)
        .toInt().coerceAtLeast(10)
}

fun beginExpedition(state: RebootState, regionId: String, roleId: String, daring: Boolean): RebootState {
    val region = LostMarches.byId(regionId) ?: return state
    if (roleId !in setOf("scout", "warden", "occultist") || state.expedition != null ||
        state.lifetimeEmbers < region.unlockAt || state.embers < region.expeditionCost) return state
    return state.copy(
        embers = state.embers - region.expeditionCost,
        expedition = MarchExpedition(regionId, roleId, daring,
            expeditionDuration(region, state, roleId), state.expeditionsCompleted),
    )
}

fun expeditionReward(expedition: MarchExpedition, state: RebootState): Int {
    val base = when {
        !expedition.daring && expedition.roleId == "scout" -> 2
        !expedition.daring && expedition.roleId == "warden" -> 4
        !expedition.daring -> 3
        (expedition.sequence + if (expedition.roleId == "occultist") 1 else 0) %
            (if (expedition.roleId == "occultist") 4 else 3) == 0 -> 1
        else -> 6
    }
    return base + (if (state.specializations["bonelibrary"] == "utility") 1 else 0) +
        (if (state.relicSetId == "wayfarer") 1 else 0) +
        (if (hasArtifact(state, "tide_compass")) 1 else 0) +
        (if ("marches" in state.completedTrials) 1 else 0)
}

fun finishExpedition(state: RebootState): RebootState {
    val expedition = state.expedition ?: return state
    if (expedition.remainingSeconds > 0) return state
    val found = expeditionReward(expedition, state)
    return withChronicle(state.copy(
        expedition = null,
        fragments = state.fragments + (expedition.regionId to (state.fragments[expedition.regionId] ?: 0) + found),
        expeditionsCompleted = state.expeditionsCompleted + 1,
        runExpeditions = state.runExpeditions + 1,
        lastExpeditionRegion = expedition.regionId,
        lastExpeditionReward = found,
    ))
}

fun siegeFragmentCost(state: RebootState): Int =
    if (state.specializations["citadel"] == "utility") 5 else 6

fun canConquer(state: RebootState, region: LostRegion): Boolean =
    region.id !in state.conqueredRegions && state.lifetimeEmbers >= region.unlockAt &&
        state.level(region.defenderId) >= region.defenderLevel &&
        (state.fragments[region.id] ?: 0) >= siegeFragmentCost(state) &&
        state.embers >= region.siegeCost

fun conquerRegion(state: RebootState, id: String): RebootState {
    val region = LostMarches.byId(id) ?: return state
    if (!canConquer(state, region)) return state
    return withChronicle(state.copy(
        embers = state.embers - region.siegeCost,
        fragments = state.fragments + (id to ((state.fragments[id] ?: 0) - siegeFragmentCost(state))),
        conqueredRegions = state.conqueredRegions + id,
        relics = state.relics + 2,
        gloom = (state.gloom - 12.0).coerceAtLeast(0.0),
    ))
}

val marchSpecializations = mapOf(
    "coalpit" to "forest", "moonforge" to "fen",
    "bonelibrary" to "coast", "citadel" to "ruins",
)

fun canSpecialize(state: RebootState, buildingId: String): Boolean {
    val regionId = marchSpecializations[buildingId] ?: return false
    return buildingId !in state.specializations && state.mastery(buildingId) >= 1 &&
        (state.fragments[regionId] ?: 0) >= 3
}

fun specializeBuilding(state: RebootState, buildingId: String, path: String): RebootState {
    if (path !in setOf("industry", "utility") || !canSpecialize(state, buildingId)) return state
    val regionId = marchSpecializations.getValue(buildingId)
    return withChronicle(state.copy(
        fragments = state.fragments + (regionId to (state.fragments.getValue(regionId) - 3)),
        specializations = state.specializations + (buildingId to path),
    ))
}

fun equipRelicSet(state: RebootState, id: String): RebootState =
    if (state.reign < 2 || state.relicSetCooldownSeconds > 0 ||
        id !in setOf("emberguard", "wayfarer", "nightveil") || state.relicSetId == id) state
    else state.copy(relicSetId = id, relicSetCooldownSeconds = 120)
