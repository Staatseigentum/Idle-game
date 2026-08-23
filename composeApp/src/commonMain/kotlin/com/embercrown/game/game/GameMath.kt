package com.embercrown.game.game

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

const val ACADEMY_BONUS_PER_LEVEL = 0.10
const val ACHIEVEMENT_BONUS_PER_UNLOCK = 0.02
const val MIN_LIFETIME_GOLD_FOR_VERFALL = 10_000.0
const val MIN_VERFALL_COUNT_FOR_WIEDERGEBURT = 5
const val DYNASTY_SWITCH_COOLDOWN_SECONDS = 300L
const val DRAGON_BUFF_MULTIPLIER = 7.0
const val CORRUPTION_RAMP_SECONDS = 1500.0
const val CORRUPTION_MALUS_MAX = 0.15
const val OMEN_BUFF_MULTIPLIER = 1.2
const val OMEN_MALUS_MULTIPLIER = 0.85

/** Building levels at which that building's own production permanently doubles again. */
val MILESTONE_LEVELS = listOf(10, 25, 50, 100, 200, 500, 1000)

fun milestoneMultiplier(level: Int): Double =
    2.0.pow(MILESTONE_LEVELS.count { level >= it })

/** -5% holding costs while the "freihandel" Ratssaal upgrade is owned. */
private fun costDiscount(state: GameState): Double =
    if ("freihandel" in state.ratssaalUpgrades) 0.95 else 1.0

fun buildingCost(definition: BuildingDefinition, level: Int, state: GameState? = null): Double =
    definition.baseCost * definition.costGrowth.pow(level) * (state?.let(::costDiscount) ?: 1.0) *
        (state?.let { heirMultiplier(it, HeirEffectType.BUILDING_COST) } ?: 1.0)

/** Total cost to buy [quantity] levels starting from [level] (geometric series). */
fun bulkBuildingCost(definition: BuildingDefinition, level: Int, quantity: Int, state: GameState? = null): Double {
    if (quantity <= 0) return 0.0
    val r = definition.costGrowth
    val firstCost = buildingCost(definition, level, state)
    return firstCost * (r.pow(quantity) - 1.0) / (r - 1.0)
}

/** How many levels of [definition] can be bought with [availableGold] starting from [level]. */
fun maxAffordableQuantity(definition: BuildingDefinition, level: Int, availableGold: Double, state: GameState? = null): Int {
    if (availableGold <= 0.0) return 0
    val r = definition.costGrowth
    val firstCost = buildingCost(definition, level, state)
    if (availableGold < firstCost) return 0
    // Solve availableGold >= firstCost * (r^n - 1) / (r - 1) for n.
    val n = ln(availableGold * (r - 1.0) / firstCost + 1.0) / ln(r)
    return floor(n).toInt().coerceAtLeast(1)
}

/**
 * A building's own output, including its milestone doubling. Pass [state] to additionally fold in
 * synergy bonuses from other buildings — omitted where only the raw per-level rate is needed.
 */
fun buildingProduction(definition: BuildingDefinition, level: Int, state: GameState? = null): Double {
    val ownProduction = level * definition.baseProduction * milestoneMultiplier(level)
    val synergy = state?.let { synergyMultiplier(definition.id, it.buildings.associate { b -> b.id to b.level }) } ?: 1.0
    return ownProduction * synergy
}

/** Bonus multiplier on [targetId]'s production from other buildings' levels, capped per source. */
fun synergyMultiplier(targetId: String, levelsById: Map<String, Int>): Double =
    SynergyDefinition.forTarget(targetId).fold(1.0) { acc, synergy ->
        acc * (1.0 + (synergy.bonusPerLevel * (levelsById[synergy.sourceId] ?: 0)).coerceAtMost(synergy.cap))
    }

/** Bonus multiplier from [path], but only while it is the active Dynasty Path — otherwise it's "asleep" (1.0). */
fun legacyBonus(state: GameState, path: DynastyPath): Double {
    val level = state.legacyLevels[path] ?: 0
    if (level <= 0 || state.activeDynastyPath != path) return 1.0
    return 1.0 + path.bonusPerLevel * level
}

fun academyMultiplier(state: GameState): Double =
    1.0 + ACADEMY_BONUS_PER_LEVEL * state.academyLevel * legacyBonus(state, DynastyPath.MAGIC)

fun achievementMultiplier(state: GameState): Double =
    1.0 + ACHIEVEMENT_BONUS_PER_UNLOCK * state.unlockedAchievements.size

/** +15% while the "zunftrecht" Ratssaal upgrade is owned. */
fun ratssaalProductionMultiplier(state: GameState): Double =
    if ("zunftrecht" in state.ratssaalUpgrades) 1.15 else 1.0

/** ×[DRAGON_BUFF_MULTIPLIER] while a tapped dragon sighting's buff is still ticking down. */
fun dragonBuffMultiplier(state: GameState): Double =
    if (state.dragonBuffTicksRemaining > 0) DRAGON_BUFF_MULTIPLIER else 1.0

/** +10% total production while the "triumphal_road" Wonder is built. */
fun wonderProductionMultiplier(state: GameState): Double =
    if ("triumphal_road" in state.ownedWonders) 1.10 else 1.0

/** 1.0 + the current Heir's trait magnitude if it matches [type], else 1.0 (no effect outside its own domain). */
fun heirMultiplier(state: GameState, type: HeirEffectType): Double {
    val trait = state.currentHeirId?.let { HeirTraitDefinition.byId(it) } ?: return 1.0
    return if (trait.effectType == type) 1.0 + trait.magnitude else 1.0
}

/** 0.0 while Verfall isn't available yet; otherwise constant growth toward 1.0 over [CORRUPTION_RAMP_SECONDS], slowed by the "wachen" Ratssaal upgrade and the "spire" Wonder. */
fun corruptionGrowthPerSecond(state: GameState): Double {
    if (state.lifetimeGold < MIN_LIFETIME_GOLD_FOR_VERFALL) return 0.0
    val wachen = if ("wachen" in state.ratssaalUpgrades) 0.7 else 1.0
    val spire = if ("spire" in state.ownedWonders) 0.85 else 1.0
    return (1.0 / CORRUPTION_RAMP_SECONDS) * wachen * spire * heirMultiplier(state, HeirEffectType.CORRUPTION_RATE)
}

/** Up to -[CORRUPTION_MALUS_MAX] total production at full corruption. */
fun corruptionProductionMalus(state: GameState): Double = 1.0 - CORRUPTION_MALUS_MAX * state.corruption

/** ×[OMEN_BUFF_MULTIPLIER] or ×[OMEN_MALUS_MULTIPLIER] while a tapped Verfall Omen's effect is ticking down. */
fun omenMultiplier(state: GameState): Double = when {
    state.omenBuffTicksRemaining > 0 -> OMEN_BUFF_MULTIPLIER
    state.omenMalusTicksRemaining > 0 -> OMEN_MALUS_MULTIPLIER
    else -> 1.0
}

fun totalProduction(state: GameState): Double =
    state.buildings.sumOf { buildingState ->
        buildingProduction(BuildingDefinition.byId(buildingState.id), buildingState.level, state)
    } * academyMultiplier(state) * achievementMultiplier(state) * legacyBonus(state, DynastyPath.STEEL) *
        ratssaalProductionMultiplier(state) * dragonBuffMultiplier(state) * corruptionProductionMalus(state) *
        wonderProductionMultiplier(state) * heirMultiplier(state, HeirEffectType.PRODUCTION) * omenMultiplier(state)

fun clickGain(state: GameState): Double =
    1.0 + totalProduction(state) * 0.05 * heirMultiplier(state, HeirEffectType.CLICK_GAIN)

fun isBuildingUnlocked(definition: BuildingDefinition, state: GameState): Boolean =
    state.currentAge.index >= definition.unlockAgeIndex

/** Passive second currency from trade-facing buildings, earned continuously (including offline). */
fun influenceProduction(state: GameState): Double {
    val base = (state.buildingLevel("market_stall") + state.buildingLevel("trading_post")) * 0.02
    val gesandte = if ("gesandte" in state.ratssaalUpgrades) 1.25 else 1.0
    return base * academyMultiplier(state) * gesandte * heirMultiplier(state, HeirEffectType.INFLUENCE)
}

fun chroniclePointsForVerfall(state: GameState): Double {
    val alteKarten = if ("alte_karten" in state.ratssaalUpgrades) 1.20 else 1.0
    val archive = if ("archive" in state.ownedWonders) 1.15 else 1.0
    val heir = heirMultiplier(state, HeirEffectType.CHRONICLE_POINTS)
    return floor(sqrt(state.lifetimeGold / 10_000.0) * legacyBonus(state, DynastyPath.CUNNING) * alteKarten * archive * heir).coerceAtLeast(0.0)
}

fun academyUpgradeCost(currentLevel: Int): Double =
    2.0.pow(currentLevel)

fun sagenForWiedergeburt(lifetimeChroniclePoints: Double): Double =
    floor(sqrt(lifetimeChroniclePoints / 10.0)).coerceAtLeast(0.0)

fun legacyUpgradeCost(currentLevel: Int): Double =
    2.0.pow(currentLevel)
