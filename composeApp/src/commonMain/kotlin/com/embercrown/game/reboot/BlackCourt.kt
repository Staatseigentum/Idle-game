package com.embercrown.game.reboot

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.floor

/** Patrols are a short, guaranteed defensive action, distinct from fragment expeditions. */
data class PatrolRoute(
    val id: String,
    val unlockAt: Double,
    val emberCost: Double,
    val seconds: Int,
    val gloomCleared: Double,
    val regionId: String,
    val fragments: Int,
)

object PatrolRoutes {
    val all = listOf(
        PatrolRoute("hearth", 20_000.0, 2_500.0, 65, 18.0, "forest", 2),
        PatrolRoute("border", 12_000_000.0, 300_000.0, 105, 27.0, "fen", 3),
        PatrolRoute("eclipse", 16_000_000_000.0, 800_000_000.0, 150, 36.0, "blackpass", 4),
    )
}

@Serializable
data class CrownPatrol(val routeId: String, val remainingSeconds: Int)

fun patrolDuration(route: PatrolRoute, state: RebootState): Int = ceil(route.seconds *
    (if (state.specializations["scoutlodge"] == "utility") 0.8 else 1.0) *
    (if (state.relicSetId == "wayfarer") 0.9 else 1.0) *
    (if ("watchfires" in state.completedTrials) 0.9 else 1.0)).toInt().coerceAtLeast(10)

fun beginPatrol(state: RebootState, routeId: String): RebootState {
    val route = PatrolRoutes.all.firstOrNull { it.id == routeId } ?: return state
    if (state.patrol != null || state.level("scoutlodge") == 0 ||
        state.lifetimeEmbers < route.unlockAt || state.embers < route.emberCost) return state
    return state.copy(
        embers = state.embers - route.emberCost,
        patrol = CrownPatrol(route.id, patrolDuration(route, state)),
    )
}

fun finishPatrol(state: RebootState): RebootState {
    val patrol = state.patrol ?: return state
    if (patrol.remainingSeconds > 0) return state
    val route = PatrolRoutes.all.firstOrNull { it.id == patrol.routeId } ?: return state.copy(patrol = null)
    return withChronicle(state.copy(
        patrol = null,
        patrolsCompleted = state.patrolsCompleted + 1,
        runPatrols = state.runPatrols + 1,
        lastPatrolReward = route.fragments,
        gloom = (state.gloom - route.gloomCleared).coerceAtLeast(0.0),
        fragments = state.fragments + (route.regionId to
            (state.fragments[route.regionId] ?: 0) + route.fragments),
    ))
}

object CourtTactics {
    val all = listOf("ward", "guile", "strike")
}

data class CourtLord(
    val id: String,
    val emberCost: Double,
    val tactic: String,
    val requirement: (RebootState) -> Boolean,
)

/** Each lord asks for a different preparation. Costs alone never win a court encounter. */
object BlackCourt {
    val all = listOf(
        CourtLord("gatekeeper", 150_000_000_000.0, "ward") {
            "glassfields" in it.conqueredRegions && it.level("lanternwatch") >= 10
        },
        CourtLord("mirror", 350_000_000_000.0, "guile") {
            "blackpass" in it.conqueredRegions && hasArtifact(it, "marsh_lantern")
        },
        CourtLord("marshal", 700_000_000_000.0, "strike") {
            "court" in it.conqueredRegions && it.runPatrols >= 3 && it.level("shadowfoundry") >= 5
        },
        CourtLord("oracle", 1_100_000_000_000.0, "guile") {
            it.level("courtobservatory") >= 5 && (it.fragments["court"] ?: 0) >= 10
        },
        CourtLord("sovereign", 1_800_000_000_000.0, "ward") {
            it.level("eclipsethrone") >= 3 && it.gloom <= 35.0 && it.equippedArtifacts.size == 2
        },
    )
}

fun courtCost(lord: CourtLord, state: RebootState): Double = floor(lord.emberCost *
    (if (state.specializations["shadowfoundry"] == "utility") 0.85 else 1.0))

fun canDefeatCourtLord(state: RebootState, id: String): Boolean {
    val index = BlackCourt.all.indexOfFirst { it.id == id }
    if (index < 0 || state.eclipseSiegeStage < 3 || id in state.courtVictories) return false
    val lord = BlackCourt.all[index]
    return (index == 0 || BlackCourt.all[index - 1].id in state.courtVictories) &&
        state.courtTactic == lord.tactic && lord.requirement(state) && state.embers >= courtCost(lord, state)
}

fun defeatCourtLord(state: RebootState, id: String): RebootState {
    if (!canDefeatCourtLord(state, id)) return state
    val lord = BlackCourt.all.first { it.id == id }
    val reward = 6 + BlackCourt.all.indexOf(lord) * 3 +
        (if (state.specializations["courtobservatory"] == "utility") 3 else 0)
    return withChronicle(state.copy(
        embers = state.embers - courtCost(lord, state),
        courtVictories = state.courtVictories + id,
        relics = state.relics + reward,
        gloom = (state.gloom - 16.0).coerceAtLeast(0.0),
    ))
}

/** The blueprint is a player-authored target, not a free rebuild after prestige. */
fun buildFromBlueprint(state: RebootState): RebootState {
    var current = state
    for (building in RebootBuildings.all) {
        if (current.lifetimeEmbers < building.unlockAt) continue
        val target = (state.blueprintLevels[building.id] ?: 0).coerceIn(0, 1_000)
        val remaining = (target - current.level(building.id)).coerceAtLeast(0)
        if (remaining == 0) continue
        val count = minOf(remaining, maxAffordableBuildings(building, current))
        if (count == 0) continue
        val cost = buildingBundleCost(building, current, count)
        current = current.copy(
            embers = current.embers - cost,
            levels = current.levels + (building.id to (current.level(building.id) + count)),
        )
    }
    return withChronicle(withTutorialProgress(current))
}

/** Explicitly communicates the real production penalty used by the economy. */
fun gloomPenaltyPercent(state: RebootState): Int =
    floor(state.gloom.coerceIn(0.0, 100.0) * 0.35).toInt()
