package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.wonder_archive
import com.embercrown.game.resources.wonder_archive_desc
import com.embercrown.game.resources.wonder_spire
import com.embercrown.game.resources.wonder_spire_desc
import com.embercrown.game.resources.wonder_triumphal_road
import com.embercrown.game.resources.wonder_triumphal_road_desc
import org.jetbrains.compose.resources.StringResource

/** A one-time, unique mega-building bought with gold at the Age it unlocks — unlike holdings, no levels. */
data class WonderDefinition(
    val id: String,
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    val unlockAgeIndex: Int,
    val cost: Double,
) {
    companion object {
        // Ages 10, 14 and 16 are the only ones with no holding of their own (see BuildingDefinition's
        // unlockAgeIndex list) — the natural Wonder slots. Placeholder costs — tune once playtested.
        // Each Wonder's actual bonus is hardcoded by id in GameMath.kt, same convention as the
        // Ratssaal upgrades (RatssaalUpgradeDefinition also carries no bonus-magnitude field).
        val all: List<WonderDefinition> = listOf(
            WonderDefinition("triumphal_road", Res.string.wonder_triumphal_road, Res.string.wonder_triumphal_road_desc, 10, 5.0e10),
            WonderDefinition("spire", Res.string.wonder_spire, Res.string.wonder_spire_desc, 14, 2.0e14),
            WonderDefinition("archive", Res.string.wonder_archive, Res.string.wonder_archive_desc, 16, 1.0e16),
        )

        fun byId(id: String): WonderDefinition = all.first { it.id == id }
    }
}
