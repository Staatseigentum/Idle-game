package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.building_alchemist_lab
import com.embercrown.game.resources.building_celestial_observatory
import com.embercrown.game.resources.building_dragon_hoard
import com.embercrown.game.resources.building_farm
import com.embercrown.game.resources.building_fishing_hut
import com.embercrown.game.resources.building_forge
import com.embercrown.game.resources.building_library
import com.embercrown.game.resources.building_market_stall
import com.embercrown.game.resources.building_mine
import com.embercrown.game.resources.building_quarry
import com.embercrown.game.resources.building_tavern
import com.embercrown.game.resources.building_temple
import com.embercrown.game.resources.building_trading_post
import com.embercrown.game.resources.building_treasury
import com.embercrown.game.resources.building_wizard_tower
import com.embercrown.game.resources.building_woodcutter_camp
import org.jetbrains.compose.resources.StringResource
import kotlin.math.pow

data class BuildingDefinition(
    val id: String,
    val nameRes: StringResource,
    val baseCost: Double,
    val costGrowth: Double,
    val baseProduction: Double,
    val unlockAgeIndex: Int,
) {
    companion object {
        private const val COST_GROWTH = 1.15

        // id, nameRes, unlockAgeIndex — cost/production are derived from tier position (see `all`).
        private val definitions = listOf(
            Triple("farm", Res.string.building_farm, 0),
            Triple("fishing_hut", Res.string.building_fishing_hut, 0),
            Triple("woodcutter_camp", Res.string.building_woodcutter_camp, 1),
            Triple("quarry", Res.string.building_quarry, 2),
            Triple("mine", Res.string.building_mine, 3),
            Triple("market_stall", Res.string.building_market_stall, 4),
            Triple("tavern", Res.string.building_tavern, 5),
            Triple("forge", Res.string.building_forge, 6),
            Triple("trading_post", Res.string.building_trading_post, 7),
            Triple("library", Res.string.building_library, 8),
            Triple("alchemist_lab", Res.string.building_alchemist_lab, 9),
            Triple("wizard_tower", Res.string.building_wizard_tower, 11),
            Triple("temple", Res.string.building_temple, 12),
            Triple("treasury", Res.string.building_treasury, 13),
            Triple("dragon_hoard", Res.string.building_dragon_hoard, 15),
            Triple("celestial_observatory", Res.string.building_celestial_observatory, 17),
        )

        val all: List<BuildingDefinition> = definitions.mapIndexed { tier, (id, nameRes, unlockAgeIndex) ->
            val baseCost = 10.0 * 6.0.pow(tier)
            BuildingDefinition(
                id = id,
                nameRes = nameRes,
                baseCost = baseCost,
                costGrowth = COST_GROWTH,
                baseProduction = baseCost * 0.01,
                unlockAgeIndex = unlockAgeIndex,
            )
        }

        fun byId(id: String): BuildingDefinition = all.first { it.id == id }
    }
}
