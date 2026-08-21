package com.embercrown.game.game

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

const val ACADEMY_BONUS_PER_LEVEL = 0.10
const val ACHIEVEMENT_BONUS_PER_UNLOCK = 0.02
const val MIN_LIFETIME_GOLD_FOR_VERFALL = 10_000.0
const val MIN_VERFALL_COUNT_FOR_WIEDERGEBURT = 5

fun buildingCost(definition: BuildingDefinition, level: Int): Double =
    definition.baseCost * definition.costGrowth.pow(level)

/** Total cost to buy [quantity] levels starting from [level] (geometric series). */
fun bulkBuildingCost(definition: BuildingDefinition, level: Int, quantity: Int): Double {
    if (quantity <= 0) return 0.0
    val r = definition.costGrowth
    val firstCost = buildingCost(definition, level)
    return firstCost * (r.pow(quantity) - 1.0) / (r - 1.0)
}

/** How many levels of [definition] can be bought with [availableGold] starting from [level]. */
fun maxAffordableQuantity(definition: BuildingDefinition, level: Int, availableGold: Double): Int {
    if (availableGold <= 0.0) return 0
    val r = definition.costGrowth
    val firstCost = buildingCost(definition, level)
    if (availableGold < firstCost) return 0
    // Solve availableGold >= firstCost * (r^n - 1) / (r - 1) for n.
    val n = ln(availableGold * (r - 1.0) / firstCost + 1.0) / ln(r)
    return floor(n).toInt().coerceAtLeast(1)
}

fun buildingProduction(definition: BuildingDefinition, level: Int): Double =
    level * definition.baseProduction

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

fun totalProduction(state: GameState): Double =
    state.buildings.sumOf { buildingState ->
        buildingProduction(BuildingDefinition.byId(buildingState.id), buildingState.level)
    } * academyMultiplier(state) * achievementMultiplier(state) * legacyBonus(state, DynastyPath.STEEL)

fun clickGain(state: GameState): Double =
    1.0 + totalProduction(state) * 0.05

fun isBuildingUnlocked(definition: BuildingDefinition, state: GameState): Boolean =
    state.currentAge.index >= definition.unlockAgeIndex

fun chroniclePointsForVerfall(state: GameState): Double =
    floor(sqrt(state.lifetimeGold / 10_000.0) * legacyBonus(state, DynastyPath.CUNNING)).coerceAtLeast(0.0)

fun academyUpgradeCost(currentLevel: Int): Double =
    2.0.pow(currentLevel)

fun sagenForWiedergeburt(lifetimeChroniclePoints: Double): Double =
    floor(sqrt(lifetimeChroniclePoints / 10.0)).coerceAtLeast(0.0)

fun legacyUpgradeCost(currentLevel: Int): Double =
    2.0.pow(currentLevel)
