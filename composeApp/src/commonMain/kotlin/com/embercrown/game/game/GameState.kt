package com.embercrown.game.game

import kotlinx.serialization.Serializable

@Serializable
data class BuildingState(
    val id: String,
    val level: Int = 0,
)

@Serializable
data class GameState(
    val gold: Double = 0.0,
    val lifetimeGold: Double = 0.0,
    val buildings: List<BuildingState> = BuildingDefinition.all.map { BuildingState(it.id) },
    val chroniclePoints: Double = 0.0,
    val lifetimeChroniclePoints: Double = 0.0,
    val academyLevel: Int = 0,
    val verfallCount: Int = 0,
    val sagen: Double = 0.0,
    val wiedergeburtCount: Int = 0,
    val activeDynastyPath: DynastyPath = DynastyPath.STEEL,
    val legacyLevels: Map<DynastyPath, Int> = emptyMap(),
    val unlockedAchievements: Set<String> = emptySet(),
    val offlineProgressEnabled: Boolean = true,
    val lastSeenEpochSeconds: Long = 0L,
) {
    fun buildingLevel(id: String): Int = buildings.first { it.id == id }.level

    val currentAge: AgeDefinition get() = AgeDefinition.currentAgeFor(lifetimeGold)
    val nextAge: AgeDefinition? get() = AgeDefinition.nextAgeFor(lifetimeGold)
}
