package com.embercrown.game.game

data class SynergyDefinition(
    val sourceId: String,
    val targetId: String,
    val bonusPerLevel: Double,
    val cap: Double,
) {
    companion object {
        val all: List<SynergyDefinition> = listOf(
            SynergyDefinition("farm", "tavern", 0.01, 1.0),
            SynergyDefinition("fishing_hut", "market_stall", 0.01, 1.0),
            SynergyDefinition("woodcutter_camp", "forge", 0.01, 1.0),
            SynergyDefinition("quarry", "temple", 0.01, 1.0),
            SynergyDefinition("mine", "treasury", 0.01, 1.0),
            SynergyDefinition("library", "wizard_tower", 0.01, 1.0),
            SynergyDefinition("alchemist_lab", "celestial_observatory", 0.01, 1.0),
        )

        fun forTarget(targetId: String): List<SynergyDefinition> = all.filter { it.targetId == targetId }
    }
}
