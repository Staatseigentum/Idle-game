package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.heir_trait_diligent
import com.embercrown.game.resources.heir_trait_diligent_desc
import com.embercrown.game.resources.heir_trait_frugal
import com.embercrown.game.resources.heir_trait_frugal_desc
import com.embercrown.game.resources.heir_trait_sociable
import com.embercrown.game.resources.heir_trait_sociable_desc
import com.embercrown.game.resources.heir_trait_strong
import com.embercrown.game.resources.heir_trait_strong_desc
import com.embercrown.game.resources.heir_trait_vigilant
import com.embercrown.game.resources.heir_trait_vigilant_desc
import com.embercrown.game.resources.heir_trait_wise
import com.embercrown.game.resources.heir_trait_wise_desc
import org.jetbrains.compose.resources.StringResource

enum class HeirEffectType { PRODUCTION, BUILDING_COST, CORRUPTION_RATE, CHRONICLE_POINTS, INFLUENCE, CLICK_GAIN }

/** One trait a newly crowned Heir can carry — rolled fresh at each Wiedergeburt, not upgraded/leveled. */
data class HeirTraitDefinition(
    val id: String,
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    val effectType: HeirEffectType,
    val magnitude: Double,
) {
    companion object {
        // Placeholder magnitudes, same order as a single Ratssaal upgrade — tune once playtested.
        val all: List<HeirTraitDefinition> = listOf(
            HeirTraitDefinition("diligent", Res.string.heir_trait_diligent, Res.string.heir_trait_diligent_desc, HeirEffectType.PRODUCTION, 0.08),
            HeirTraitDefinition("frugal", Res.string.heir_trait_frugal, Res.string.heir_trait_frugal_desc, HeirEffectType.BUILDING_COST, -0.08),
            HeirTraitDefinition("vigilant", Res.string.heir_trait_vigilant, Res.string.heir_trait_vigilant_desc, HeirEffectType.CORRUPTION_RATE, -0.15),
            HeirTraitDefinition("wise", Res.string.heir_trait_wise, Res.string.heir_trait_wise_desc, HeirEffectType.CHRONICLE_POINTS, 0.10),
            HeirTraitDefinition("sociable", Res.string.heir_trait_sociable, Res.string.heir_trait_sociable_desc, HeirEffectType.INFLUENCE, 0.20),
            HeirTraitDefinition("strong", Res.string.heir_trait_strong, Res.string.heir_trait_strong_desc, HeirEffectType.CLICK_GAIN, 0.15),
        )

        fun byId(id: String): HeirTraitDefinition = all.first { it.id == id }
    }
}
