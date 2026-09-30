package com.embercrown.game.reboot

import com.embercrown.game.audio.SfxId
import com.embercrown.game.game.UiSettingsStore
import com.embercrown.game.game.createSettings
import com.embercrown.game.game.nowEpochSeconds
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/** This save remains separate from the released 0.2.x game. New fields have defaults for reboot saves. */
private const val SAVE_KEY = "embercrown_ash_kingdom_v1"
private const val OFFLINE_CAP_SECONDS = 8 * 60 * 60L
const val CROWN_TARGET = 1_200_000_000_000.0
const val TUTORIAL_DONE = 5

data class RebootBuilding(
    val id: String,
    val baseCost: Double,
    val growth: Double,
    val output: Double,
    val unlockAt: Double,
)

object RebootBuildings {
    val all = listOf(
        RebootBuilding("coalpit", 15.0, 1.18, 0.20, 0.0),
        RebootBuilding("emberorchard", 85.0, 1.18, 0.52, 30.0),
        RebootBuilding("hollowmill", 210.0, 1.19, 1.20, 70.0),
        RebootBuilding("lanternwatch", 600.0, 1.19, 3.4, 230.0),
        RebootBuilding("belltower", 1_100.0, 1.20, 8.0, 650.0),
        RebootBuilding("ashmarket", 3_800.0, 1.20, 24.0, 2_300.0),
        RebootBuilding("moonforge", 9_500.0, 1.21, 60.0, 6_000.0),
        RebootBuilding("scoutlodge", 34_000.0, 1.21, 185.0, 20_000.0),
        RebootBuilding("bonelibrary", 80_000.0, 1.22, 460.0, 50_000.0),
        RebootBuilding("citadel", 700_000.0, 1.23, 3_500.0, 450_000.0),
        RebootBuilding("shadowfoundry", 2_800_000.0, 1.23, 11_000.0, 1_600_000.0),
        RebootBuilding("emberwell", 6_500_000.0, 1.24, 26_000.0, 4_000_000.0),
        RebootBuilding("gravegarden", 60_000_000.0, 1.25, 210_000.0, 40_000_000.0),
        RebootBuilding("soulharbor", 650_000_000.0, 1.26, 1_700_000.0, 400_000_000.0),
        RebootBuilding("stormspire", 7_000_000_000.0, 1.27, 14_000_000.0, 4_000_000_000.0),
        RebootBuilding("courtobservatory", 29_000_000_000.0, 1.27, 47_000_000.0, 16_000_000_000.0),
        RebootBuilding("wyrmroost", 90_000_000_000.0, 1.28, 120_000_000.0, 50_000_000_000.0),
        RebootBuilding("eclipsethrone", 600_000_000_000.0, 1.29, 1_100_000_000.0, 400_000_000_000.0),
    )

    fun byId(id: String): RebootBuilding = all.first { it.id == id }
}

data class BuildingMastery(val minimumLevel: Int, val costMultiplier: Double, val outputMultiplier: Double)

val buildingMasteries = listOf(
    BuildingMastery(5, 12.0, 2.5),
    BuildingMastery(20, 170.0, 3.0),
    BuildingMastery(50, 2_600.0, 4.0),
)

data class RebootMilestone(val id: String, val target: Double, val relicReward: Int)

object RebootMilestones {
    val all = listOf(
        RebootMilestone("spark", 2_000.0, 1),
        RebootMilestone("watch", 150_000.0, 1),
        RebootMilestone("forge", 12_000_000.0, 2),
        RebootMilestone("march", 1_000_000_000.0, 3),
        RebootMilestone("sky", 90_000_000_000.0, 4),
        RebootMilestone("crown", CROWN_TARGET, 6),
    )
}

data class RelicPower(val id: String, val maxRank: Int, val baseCost: Int)

object RelicPowers {
    val all = listOf(
        RelicPower("cinderheart", 5, 1),
        RelicPower("foundation", 5, 1),
        RelicPower("nightward", 5, 1),
        RelicPower("sovereignhand", 5, 1),
        RelicPower("beaconkeeper", 5, 2),
    )
}

@Serializable
data class RebootState(
    val embers: Double = 0.0,
    /** Earned within the current reign; retained by old saves under the same field name. */
    val lifetimeEmbers: Double = 0.0,
    val levels: Map<String, Int> = emptyMap(),
    val buildingUpgrades: Map<String, Int> = emptyMap(),
    val claimedMilestones: Set<String> = emptySet(),
    val chronicleEntries: Set<String> = emptySet(),
    val cosmeticStyles: Map<String, String> = emptyMap(),
    val featuredTrophies: List<String>? = null,
    val gloom: Double = 8.0,
    /** Unspent permanent relics. */
    val relics: Int = 0,
    val relicUpgrades: Map<String, Int> = emptyMap(),
    /** Saved between sessions so the post-ritual choice cannot be skipped by restarting. */
    val prestigePending: Boolean = false,
    val prestigeEarned: Int = 0,
    val reign: Int = 1,
    val beaconSeconds: Int = 0,
    val beaconCooldownSeconds: Int = 0,
    val beaconsLit: Int = 0,
    val totalTaps: Int = 0,
    val runTaps: Int = 0,
    val runBeacons: Int = 0,
    val runExpeditions: Int = 0,
    val claimedOrders: Set<String> = emptySet(),
    val districtLevels: Map<String, Int> = emptyMap(),
    val outpostLevels: Map<String, Int> = emptyMap(),
    val craftedArtifacts: Set<String> = emptySet(),
    val equippedArtifacts: Set<String> = emptySet(),
    val activeTrialId: String? = null,
    val nextTrialId: String? = null,
    val completedTrials: Set<String> = emptySet(),
    val eclipseSiegeStage: Int = 0,
    val autoStokeSeconds: Double = 0.0,
    val omensResolved: Int = 0,
    val omenSequence: Int = 0,
    val omenCountdownSeconds: Int = 240,
    val pendingOmenId: String? = null,
    val omenEffectId: String? = null,
    val omenEffectSeconds: Int = 0,
    val edictId: String = "balance",
    val edictCooldownSeconds: Int = 0,
    val fragments: Map<String, Int> = emptyMap(),
    val conqueredRegions: Set<String> = emptySet(),
    val siegeFailures: Map<String, Int> = emptyMap(),
    val lastSiegeResult: SiegeResult? = null,
    val marchUpgradeRanks: Map<String, Int> = emptyMap(),
    val expedition: MarchExpedition? = null,
    val expeditionsCompleted: Int = 0,
    val lastExpeditionRegion: String? = null,
    val lastExpeditionReward: Int = 0,
    val pendingMarchEventId: String? = null,
    val resolvedMarchEvents: Set<String> = emptySet(),
    val runMarchEvents: Int = 0,
    val specializations: Map<String, String> = emptyMap(),
    val patrol: CrownPatrol? = null,
    val patrolsCompleted: Int = 0,
    val runPatrols: Int = 0,
    val lastPatrolReward: Int = 0,
    val courtVictories: Set<String> = emptySet(),
    val courtTactic: String = "ward",
    val blueprintLevels: Map<String, Int> = emptyMap(),
    val guideSeen: Set<String> = emptySet(),
    /** Systems stay discovered after an Ash Ritual, even when buildings reset. */
    val discoveredSystems: Set<String> = emptySet(),
    /** Null migrates pre-Throne saves without replaying every new-system introduction. */
    val introductionsSeen: Set<String>? = null,
    val relicSetId: String = "none",
    val relicSetCooldownSeconds: Int = 0,
    /** Existing saves default to completed; only a genuinely new save starts at the welcome screen. */
    val tutorialStep: Int = TUTORIAL_DONE,
    val tutorialAcknowledged: Boolean = true,
    val runSeconds: Double = 0.0,
    val playedSeconds: Double = 0.0,
    val lastPlayedEpochSeconds: Long = 0,
    @Transient val offlineEmbers: Double = 0.0,
    @Transient val offlineSeconds: Long = 0L,
    @Transient val offlineGloom: Double = 0.0,
    @Transient val offlinePatrols: Int = 0,
) {
    fun level(id: String): Int = levels[id] ?: 0
    fun mastery(id: String): Int = buildingUpgrades[id] ?: 0
    fun relicRank(id: String): Int = relicUpgrades[id] ?: 0
}

fun buildingCost(building: RebootBuilding, level: Int, state: RebootState): Double =
    floor(building.baseCost * building.growth.pow(level.coerceAtLeast(0)) *
        (1.0 - state.relicRank("foundation") * 0.04) *
        (if ("architect" in state.completedTrials) 0.95 else 1.0) *
        (when (state.activeTrialId) { "cinders" -> 1.25; "architect" -> 1.15; else -> 1.0 }))

fun buildingBundleCost(building: RebootBuilding, state: RebootState, count: Int): Double {
    if (count <= 0) return 0.0
    return (0 until count).sumOf { buildingCost(building, state.level(building.id) + it, state) }
}

fun maxAffordableBuildings(building: RebootBuilding, state: RebootState): Int {
    var count = 0
    var remaining = state.embers
    while (count < 1_000) {
        val cost = buildingCost(building, state.level(building.id) + count, state)
        if (!cost.isFinite() || cost <= 0.0 || remaining < cost) break
        remaining -= cost
        count++
    }
    return count
}

fun masteryCost(building: RebootBuilding, state: RebootState): Double {
    val tier = state.mastery(building.id)
    return if (tier in buildingMasteries.indices) floor(building.baseCost * buildingMasteries[tier].costMultiplier) else Double.POSITIVE_INFINITY
}

fun canMaster(building: RebootBuilding, state: RebootState): Boolean {
    val tier = state.mastery(building.id)
    return tier in buildingMasteries.indices &&
        state.level(building.id) >= buildingMasteries[tier].minimumLevel &&
        state.embers >= masteryCost(building, state)
}

fun buildingProduction(building: RebootBuilding, state: RebootState): Double {
    val level = state.level(building.id)
    val masteryMultiplier = buildingMasteries.take(state.mastery(building.id)).fold(1.0) { value, tier ->
        value * tier.outputMultiplier
    }
    return building.output * level * (1.0 + level / 20.0) * masteryMultiplier *
        districtMultiplier(building.id, state) *
        (if (state.specializations[building.id] == "industry") 1.4 else 1.0) *
        marchUpgradeMultiplier(state, building.id)
}

fun rawProduction(state: RebootState): Double = RebootBuildings.all.sumOf { buildingProduction(it, state) }

fun production(state: RebootState): Double = rawProduction(state) *
    (1.0 + state.relicRank("cinderheart") * 0.18) *
    (if (state.reign == 1) 1.30 else 1.15).pow(state.claimedMilestones.size) *
    (if (state.beaconSeconds > 0) 1.25 + state.relicRank("beaconkeeper") * 0.10 else 1.0) *
    (if (state.omenEffectSeconds > 0) when (state.omenEffectId) {
        "ward" -> 1.35
        "pyre" -> 2.0
        else -> 1.0
    } else 1.0) *
    (when (state.edictId) {
        "harvest" -> 1.25
        "ward" -> 0.87
        "rally" -> 0.88
        else -> 1.0
    }) *
    (1.0 + state.conqueredRegions.size * 0.05) *
    (if (state.relicSetId == "emberguard") 1.18 else 1.0) *
    (1.0 + state.claimedOrders.size * 0.02) *
    outpostMultiplier(state) *
    (if (hasArtifact(state, "cinder_crown")) 1.18 else 1.0) *
    (if (state.eclipseSiegeStage == 3) 1.20 else 1.0) *
    (1.0 + state.courtVictories.size * 0.05) *
    (if ("night" in state.completedTrials) 1.10 else 1.0) *
    (1.0 - state.gloom.coerceIn(0.0, 100.0) * 0.0035)

fun tapYield(state: RebootState): Double = (1.0 + rawProduction(state) * 0.12) *
    (1.0 + state.relicRank("sovereignhand") * 0.5) *
    (if (state.omenEffectId == "frenzy" && state.omenEffectSeconds > 0) 5.0 else 1.0) *
    (if (state.edictId == "rally") 2.5 else 1.0)

fun beaconCost(state: RebootState): Double = maxOf(20.0, floor(rawProduction(state) * 20.0 *
    (if (state.specializations["emberorchard"] == "utility") 0.85 else 1.0)))

fun relicPowerCost(power: RelicPower, state: RebootState): Int {
    val next = state.relicRank(power.id) + 1
    return power.baseCost * next * (next + 1) / 2
}

/** One fully mastered power is possible at the first Prestige; more need another reign. */
fun relicPowerRankLimit(power: RelicPower, state: RebootState): Int =
    if (state.reign < 3 && RelicPowers.all.any {
            it.id != power.id && state.relicRank(it.id) >= it.maxRank
        }) minOf(4, power.maxRank) else power.maxRank

fun purchaseRelicPower(state: RebootState, power: RelicPower): RebootState {
    val cost = relicPowerCost(power, state)
    if (!state.prestigePending || state.relicRank(power.id) >= relicPowerRankLimit(power, state) ||
        state.relics < cost) return state
    return state.copy(relics = state.relics - cost,
        relicUpgrades = state.relicUpgrades + (power.id to (state.relicRank(power.id) + 1)))
}

fun finishPrestige(state: RebootState): RebootState =
    if (state.prestigePending) state.copy(prestigePending = false) else state

fun nextMilestone(state: RebootState): RebootMilestone? =
    RebootMilestones.all.firstOrNull { it.id !in state.claimedMilestones }

fun ritualReward(state: RebootState): Int =
    8 + state.claimedMilestones.size * 2 +
        floor(sqrt(state.lifetimeEmbers / CROWN_TARGET) * 8).toInt() +
        (if (hasArtifact(state, "eclipse_sigil")) 5 else 0) +
        (if (state.eclipseSiegeStage == 3) 6 else 0) +
        (if (state.activeTrialId != null) 3 else 0)

fun canRitual(state: RebootState): Boolean =
    !state.prestigePending && "crown" in state.claimedMilestones && state.level("eclipsethrone") > 0 &&
        trialGoalMet(state)

/** One-time discoveries are stored; existing saves also infer them from their progress. */
fun discoveredSystems(state: RebootState): Set<String> = buildSet {
    addAll(state.discoveredSystems)
    // Legacy pre-Throne saves have no discovery history. New saves carry exactly what they found.
    if (state.reign > 1 && state.discoveredSystems.isEmpty())
        addAll(listOf("buildings", "realm", "marches", "power", "crown", "chronicle", "beacon"))
    if (state.level("coalpit") > 0) add("buildings")
    if (state.claimedMilestones.isNotEmpty() || state.claimedOrders.isNotEmpty()) add("realm")
    if (state.level("scoutlodge") > 0 || state.expeditionsCompleted > 0 || state.conqueredRegions.isNotEmpty()) add("marches")
    if (state.level("bonelibrary") > 0 || state.conqueredRegions.isNotEmpty() ||
        state.craftedArtifacts.isNotEmpty() || state.completedTrials.isNotEmpty()) add("power")
    if (state.level("citadel") > 0 || state.courtVictories.isNotEmpty() || canRitual(state)) add("crown")
    if (state.chronicleEntries.isNotEmpty()) add("chronicle")
    if (state.beaconsLit > 0 || (state.tutorialStep >= TUTORIAL_DONE && state.gloom >= 18.0)) add("beacon")
}

fun withDiscoveries(state: RebootState): RebootState {
    val known = discoveredSystems(state)
    val seen = state.introductionsSeen ?: if (state.tutorialStep in 0..4) emptySet() else known
    return state.copy(discoveredSystems = known,
        introductionsSeen = if (state.tutorialStep in 3..4) seen + "buildings" else seen)
}

/** Tutorial rewards and stage changes live in the state transition, never in composition. */
fun withTutorialProgress(state: RebootState): RebootState {
    var current = state
    while (true) {
        current = when {
            current.tutorialStep == 1 && current.lifetimeEmbers >= 15.0 -> current.copy(tutorialStep = 2)
            current.tutorialStep == 2 && current.level("coalpit") >= 1 -> current.copy(
                tutorialStep = 3,
                embers = current.embers + 80.0,
                lifetimeEmbers = current.lifetimeEmbers + 80.0,
            )
            current.tutorialStep == 3 && current.level("coalpit") >= 5 -> current.copy(tutorialStep = 4)
            current.tutorialStep == 4 && current.mastery("coalpit") >= 1 -> current.copy(tutorialStep = TUTORIAL_DONE)
            else -> return current
        }
    }
}

fun performAshRitual(state: RebootState, now: Long = nowEpochSeconds()): RebootState {
    if (!canRitual(state)) return state
    return withChronicle(RebootState(
        relics = state.relics + ritualReward(state),
        relicUpgrades = state.relicUpgrades,
        prestigePending = true,
        prestigeEarned = ritualReward(state),
        chronicleEntries = state.chronicleEntries,
        cosmeticStyles = state.cosmeticStyles,
        featuredTrophies = state.featuredTrophies,
        beaconsLit = state.beaconsLit,
        totalTaps = state.totalTaps,
        omensResolved = state.omensResolved,
        omenSequence = state.omenSequence,
        expeditionsCompleted = state.expeditionsCompleted,
        outpostLevels = state.outpostLevels,
        patrolsCompleted = state.patrolsCompleted,
        courtVictories = state.courtVictories,
        blueprintLevels = state.blueprintLevels,
        guideSeen = state.guideSeen,
        discoveredSystems = discoveredSystems(state),
        introductionsSeen = state.introductionsSeen,
        craftedArtifacts = state.craftedArtifacts,
        equippedArtifacts = state.equippedArtifacts,
        completedTrials = state.completedTrials + listOfNotNull(state.activeTrialId),
        activeTrialId = state.nextTrialId,
        playedSeconds = state.playedSeconds,
        reign = state.reign + 1,
        lastPlayedEpochSeconds = now,
    ))
}

/** Shared clock for live and offline play. Only foreground time advances decisions and playtime. */
fun advanceReboot(s: RebootState, seconds: Double, active: Boolean, now: Long = nowEpochSeconds()): RebootState {
    if (s.tutorialStep == 0 || s.prestigePending) return s.copy(lastPlayedEpochSeconds = now)
    var current = s
    var remaining = seconds
    // Integrate offline progress in small steps so gloom and short beacon bursts are respected.
    while (remaining > 0.0) {
        val step = min(remaining, min(60.0, min(
            if (current.beaconSeconds > 0) current.beaconSeconds.toDouble() else 60.0,
            if (current.omenEffectSeconds > 0) current.omenEffectSeconds.toDouble() else 60.0,
        )))
        val autoProgress = if ("cinders" in current.completedTrials) current.autoStokeSeconds + step else 0.0
        val autoTicks = floor(autoProgress / 5.0).toInt()
        val gained = production(current) * step + autoTicks * tapYield(current)
        val raw = rawProduction(current)
        val expeditionBefore = current.expedition
        val expeditionAfter = expeditionBefore?.copy(
            remainingSeconds = (expeditionBefore.remainingSeconds - step.toInt()).coerceAtLeast(0))
        val patrolAfter = current.patrol?.copy(
            remainingSeconds = (current.patrol.remainingSeconds - step.toInt()).coerceAtLeast(0))
        val nextCountdown = if (active && current.pendingOmenId == null && current.lifetimeEmbers >= 150_000.0)
            (current.omenCountdownSeconds - step.toInt()).coerceAtLeast(0) else current.omenCountdownSeconds
        current = current.copy(
            embers = current.embers + gained,
            lifetimeEmbers = current.lifetimeEmbers + gained,
            autoStokeSeconds = autoProgress - autoTicks * 5.0,
            gloom = (current.gloom + (if (current.tutorialStep in 1..4) 0.0 else step) *
                (0.11 + raw / (raw + 200.0) * 0.08) *
                (1.0 - current.relicRank("nightward") * 0.12) *
                (if (current.specializations["coalpit"] == "utility") 0.9 else 1.0) *
                (if (current.specializations["lanternwatch"] == "utility") 0.9 else 1.0) *
                (if (current.relicSetId == "nightveil") 0.8 else 1.0) *
                (if (hasArtifact(current, "marsh_lantern")) 0.75 else 1.0) *
                (if (current.activeTrialId == "night") 1.50 else 1.0) *
                (if (current.activeTrialId == "watchfires") 1.25 else 1.0) *
                (if (current.omenEffectId == "ward" && current.omenEffectSeconds > 0) 0.65 else 1.0) *
                (when (current.edictId) { "harvest" -> 1.35; "ward" -> 0.55; else -> 1.0 })).coerceAtMost(100.0),
            beaconSeconds = (current.beaconSeconds - step.toInt()).coerceAtLeast(0),
            beaconCooldownSeconds = (current.beaconCooldownSeconds - step.toInt()).coerceAtLeast(0),
            omenEffectSeconds = (current.omenEffectSeconds - step.toInt()).coerceAtLeast(0),
            edictCooldownSeconds = (current.edictCooldownSeconds - step.toInt()).coerceAtLeast(0),
            relicSetCooldownSeconds = (current.relicSetCooldownSeconds - step.toInt()).coerceAtLeast(0),
            expedition = expeditionAfter,
            patrol = patrolAfter,
            omenCountdownSeconds = nextCountdown,
            pendingOmenId = if (nextCountdown == 0 && active) AshOmens.next(current.omenSequence).id else current.pendingOmenId,
            runSeconds = current.runSeconds + if (active) step else 0.0,
            playedSeconds = current.playedSeconds + if (active) step else 0.0,
        )
        if (expeditionAfter != null && expeditionAfter.remainingSeconds == 0) {
            current = finishExpedition(current)
        }
        if (patrolAfter != null && patrolAfter.remainingSeconds == 0) {
            current = finishPatrol(current)
        }
        remaining -= step
    }
    return withDiscoveries(withChronicle(withTutorialProgress(current.copy(lastPlayedEpochSeconds = now))))
}

/** The engine owns all progression and saves every successful purchase/claim. */
class RebootEngine {
    private val settings = createSettings()
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(load())
    val state = _state.asStateFlow()
    private val _soundEvents = MutableSharedFlow<SfxId>(extraBufferCapacity = 32)
    val soundEvents = _soundEvents

    /** Emit once only after a successful CAS, including when the foreground clock races a click. */
    private fun mutate(sound: SfxId, change: (RebootState) -> RebootState) {
        while (true) {
            val previous = _state.value
            val next = withDiscoveries(change(previous))
            if (next == previous) return
            if (_state.compareAndSet(previous, next)) {
                _soundEvents.tryEmit(sound)
                if (next.tutorialStep > previous.tutorialStep &&
                    previous.tutorialStep in 1 until TUTORIAL_DONE && sound != SfxId.ASH_GUIDE) {
                    _soundEvents.tryEmit(SfxId.ASH_GUIDE)
                }
                return
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                delay(1_000)
                while (true) {
                    val previous = _state.value
                    val next = advanceReboot(previous, 1.0, active = true)
                    if (!_state.compareAndSet(previous, next)) continue
                    if (next.expeditionsCompleted > previous.expeditionsCompleted) _soundEvents.tryEmit(SfxId.ASH_RELIC)
                    if (next.pendingOmenId != previous.pendingOmenId && next.pendingOmenId != null) _soundEvents.tryEmit(SfxId.ASH_OMEN)
                    if (next.chronicleEntries.size > previous.chronicleEntries.size) _soundEvents.tryEmit(SfxId.ASH_RELIC)
                    break
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(15_000)
                save()
            }
        }
    }

    fun gather() {
        mutate(SfxId.ASH_TAP) { s ->
            val yield = tapYield(s)
            withChronicle(withTutorialProgress(s.copy(
                embers = s.embers + yield,
                lifetimeEmbers = s.lifetimeEmbers + yield,
                totalTaps = s.totalTaps + 1,
                runTaps = s.runTaps + 1,
            )))
        }
    }

    fun build(id: String, count: Int = 1) {
        val building = RebootBuildings.all.firstOrNull { it.id == id } ?: return
        mutate(SfxId.ASH_BUILD) { s ->
            if (s.lifetimeEmbers < building.unlockAt || count <= 0) return@mutate s
            val amount = min(count, maxAffordableBuildings(building, s))
            if (amount == 0) return@mutate s
            val price = buildingBundleCost(building, s, amount)
            withChronicle(withTutorialProgress(s.copy(embers = s.embers - price,
                levels = s.levels + (id to (s.level(id) + amount)))))
        }
        save()
    }

    fun masterBuilding(id: String) {
        val building = RebootBuildings.all.firstOrNull { it.id == id } ?: return
        mutate(SfxId.ASH_MASTERY) { s ->
            if (!canMaster(building, s)) return@mutate s
            withChronicle(withTutorialProgress(s.copy(
                embers = s.embers - masteryCost(building, s),
                buildingUpgrades = s.buildingUpgrades + (id to (s.mastery(id) + 1)),
            )))
        }
        save()
    }

    fun claimMilestone(id: String) {
        val milestone = RebootMilestones.all.firstOrNull { it.id == id } ?: return
        mutate(SfxId.ASH_RELIC) { s ->
            if (milestone.id in s.claimedMilestones || s.lifetimeEmbers < milestone.target) return@mutate s
            withChronicle(s.copy(
                claimedMilestones = s.claimedMilestones + milestone.id,
                relics = s.relics + milestone.relicReward,
            ))
        }
        save()
    }

    fun claimOrder(id: String) {
        mutate(SfxId.ASH_RELIC) { claimRoyalOrder(it, id) }
        save()
    }

    fun chooseCosmetic(category: String, id: String) {
        mutate(SfxId.ASH_PAGE) { selectCosmetic(it, category, id) }
        save()
    }

    fun featureTrophy(id: String) {
        mutate(SfxId.ASH_PAGE) { toggleFeaturedTrophy(it, id) }
        save()
    }

    fun improveDistrict(id: String) {
        mutate(SfxId.ASH_BUILD) { upgradeDistrict(it, id) }
        save()
    }

    fun improveOutpost(id: String) {
        mutate(SfxId.ASH_MARCH) { upgradeOutpost(it, id) }
        save()
    }

    fun forgeArtifact(id: String) {
        mutate(SfxId.ASH_RELIC) { craftArtifact(it, id) }
        save()
    }

    fun equipArtifact(id: String) {
        mutate(SfxId.ASH_RELIC) { toggleArtifact(it, id) }
        save()
    }

    fun queueTrial(id: String?) {
        mutate(SfxId.ASH_PAGE) { selectNextTrial(it, id) }
        save()
    }

    fun assaultEclipse() {
        mutate(SfxId.ASH_MARCH) { advanceEclipseSiege(it) }
        save()
    }

    fun buyRelicPower(id: String) {
        val power = RelicPowers.all.firstOrNull { it.id == id } ?: return
        mutate(SfxId.ASH_RELIC) { purchaseRelicPower(it, power) }
        save()
    }

    fun beginNextReign() {
        mutate(SfxId.ASH_PAGE) { finishPrestige(it) }
        save()
    }

    fun chooseOmen(option: Int) {
        if (option !in 0..1) return
        mutate(SfxId.ASH_OMEN) { resolveOmen(it, option) }
        save()
    }

    fun setEdict(id: String) {
        mutate(SfxId.ASH_RELIC) { enactEdict(it, id) }
        save()
    }

    fun startExpedition(regionId: String, roleId: String, daring: Boolean) {
        mutate(SfxId.ASH_MARCH) { beginExpedition(it, regionId, roleId, daring) }
        save()
    }

    fun siege(regionId: String) {
        val roll = Random.nextDouble()
        mutate(SfxId.ASH_MARCH) { conquerRegion(it, regionId, roll) }
        save()
    }

    fun buyMarchUpgrade(buildingId: String) {
        mutate(SfxId.ASH_MASTERY) { buyMarchUpgrade(it, buildingId) }
        save()
    }

    fun chooseMarchEvent(option: Int) {
        mutate(SfxId.ASH_OMEN) { resolveMarchEvent(it, option) }
        save()
    }

    fun startPatrol(route: String) {
        mutate(SfxId.ASH_MARCH) { beginPatrol(it, route) }
        save()
    }

    fun setCourtTactic(id: String) {
        mutate(SfxId.ASH_PAGE) { if (id in CourtTactics.all) it.copy(courtTactic = id) else it }
        save()
    }

    fun challengeCourt(id: String) {
        mutate(SfxId.ASH_MARCH) { defeatCourtLord(it, id) }
        save()
    }

    fun saveBlueprint() {
        mutate(SfxId.ASH_PAGE) { it.copy(blueprintLevels = it.levels.filterValues { level -> level > 0 }) }
        save()
    }

    fun buildBlueprint() {
        mutate(SfxId.ASH_BUILD) { buildFromBlueprint(it) }
        save()
    }

    fun acknowledgeGuide(id: String) {
        mutate(SfxId.ASH_PAGE) { it.copy(guideSeen = it.guideSeen + id) }
        save()
    }

    fun specialize(buildingId: String, path: String) {
        mutate(SfxId.ASH_MASTERY) { specializeBuilding(it, buildingId, path) }
        save()
    }

    fun setRelicSet(id: String) {
        mutate(SfxId.ASH_RELIC) { equipRelicSet(it, id) }
        save()
    }

    fun stokeBeacon() {
        mutate(SfxId.ASH_BEACON) { s ->
            val cost = beaconCost(s)
            if (s.beaconCooldownSeconds > 0 || s.embers < cost) return@mutate s
            withChronicle(s.copy(
                embers = s.embers - cost,
                gloom = (s.gloom - 24.0).coerceAtLeast(0.0),
                beaconSeconds = 30 + s.relicRank("beaconkeeper") * 6,
                beaconCooldownSeconds = 45,
                beaconsLit = s.beaconsLit + 1,
                runBeacons = s.runBeacons + 1,
            ))
        }
        save()
    }

    fun performRitual() {
        mutate(SfxId.ASH_RITUAL) { performAshRitual(it) }
        save()
    }

    private fun load(): RebootState {
        val raw: String? = settings[SAVE_KEY]
        val saved = raw?.let { runCatching { json.decodeFromString<RebootState>(it) }.getOrNull() }
            ?: RebootState(tutorialStep = 0, tutorialAcknowledged = false)
        if (saved.tutorialStep == 0) return withDiscoveries(saved.copy(lastPlayedEpochSeconds = nowEpochSeconds()))
        if (saved.prestigePending) return withDiscoveries(saved.copy(lastPlayedEpochSeconds = nowEpochSeconds()))
        val elapsed = (nowEpochSeconds() - saved.lastPlayedEpochSeconds).coerceIn(0L, OFFLINE_CAP_SECONDS)
        if (saved.lastPlayedEpochSeconds <= 0 || elapsed <= 0) return withDiscoveries(withChronicle(saved))
        val resumed = advanceReboot(saved, elapsed.toDouble(), active = false)
        return if (elapsed >= 60) resumed.copy(
            offlineEmbers = resumed.embers - saved.embers,
            offlineSeconds = elapsed,
            offlineGloom = resumed.gloom - saved.gloom,
            offlinePatrols = resumed.patrolsCompleted - saved.patrolsCompleted,
        ) else resumed
    }

    fun save() {
        settings[SAVE_KEY] = json.encodeToString(_state.value.copy(lastPlayedEpochSeconds = nowEpochSeconds()))
    }

    fun beginTutorial() {
        mutate(SfxId.ASH_GUIDE) { s -> if (s.tutorialStep == 0) withTutorialProgress(s.copy(tutorialStep = 1)) else s }
        save()
    }

    fun skipTutorial() {
        mutate(SfxId.ASH_PAGE) { it.copy(tutorialStep = TUTORIAL_DONE, tutorialAcknowledged = true) }
        save()
    }

    fun acknowledgeTutorial() {
        mutate(SfxId.ASH_PAGE) { it.copy(tutorialAcknowledged = true) }
        save()
    }

    fun acknowledgeDiscovery(id: String) {
        mutate(SfxId.ASH_GUIDE) { state ->
            if (id !in discoveredSystems(state)) state
            else state.copy(introductionsSeen = (state.introductionsSeen ?: emptySet()) + id)
        }
        save()
    }

    /** Only the reboot save key is replaced; language and old 0.2.x saves remain untouched. */
    fun resetForTutorial() {
        settings.remove(SAVE_KEY)
        _state.value = RebootState(tutorialStep = 0, tutorialAcknowledged = false,
            lastPlayedEpochSeconds = nowEpochSeconds())
        save()
    }

    fun dismissOfflineReport() {
        _state.update { it.copy(offlineEmbers = 0.0, offlineSeconds = 0L,
            offlineGloom = 0.0, offlinePatrols = 0) }
    }
}

object RebootGraph {
    val engine: RebootEngine by lazy { RebootEngine() }
    val uiSettings: UiSettingsStore by lazy { UiSettingsStore(createSettings()) }
    private val audioDelegate = lazy { RebootAudio() }
    val audio: RebootAudio get() = audioDelegate.value
    fun pauseAudioIfInitialized() { if (audioDelegate.isInitialized()) audioDelegate.value.pause() }
    fun resumeAudioIfInitialized() { if (audioDelegate.isInitialized()) audioDelegate.value.start() }
}
