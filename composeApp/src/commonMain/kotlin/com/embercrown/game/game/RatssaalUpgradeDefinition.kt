package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.ratssaal_upgrade_alte_karten
import com.embercrown.game.resources.ratssaal_upgrade_alte_karten_desc
import com.embercrown.game.resources.ratssaal_upgrade_freihandel
import com.embercrown.game.resources.ratssaal_upgrade_freihandel_desc
import com.embercrown.game.resources.ratssaal_upgrade_gesandte
import com.embercrown.game.resources.ratssaal_upgrade_gesandte_desc
import com.embercrown.game.resources.ratssaal_upgrade_wachen
import com.embercrown.game.resources.ratssaal_upgrade_wachen_desc
import com.embercrown.game.resources.ratssaal_upgrade_zunftrecht
import com.embercrown.game.resources.ratssaal_upgrade_zunftrecht_desc
import org.jetbrains.compose.resources.StringResource

data class RatssaalUpgradeDefinition(
    val id: String,
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    val cost: Double,
) {
    companion object {
        // Placeholder costs — tune against the actual Influence trickle rate once playtested.
        val all: List<RatssaalUpgradeDefinition> = listOf(
            RatssaalUpgradeDefinition("zunftrecht", Res.string.ratssaal_upgrade_zunftrecht, Res.string.ratssaal_upgrade_zunftrecht_desc, 50.0),
            RatssaalUpgradeDefinition("freihandel", Res.string.ratssaal_upgrade_freihandel, Res.string.ratssaal_upgrade_freihandel_desc, 150.0),
            RatssaalUpgradeDefinition("wachen", Res.string.ratssaal_upgrade_wachen, Res.string.ratssaal_upgrade_wachen_desc, 250.0),
            RatssaalUpgradeDefinition("gesandte", Res.string.ratssaal_upgrade_gesandte, Res.string.ratssaal_upgrade_gesandte_desc, 400.0),
            RatssaalUpgradeDefinition("alte_karten", Res.string.ratssaal_upgrade_alte_karten, Res.string.ratssaal_upgrade_alte_karten_desc, 1000.0),
        )

        fun byId(id: String): RatssaalUpgradeDefinition = all.first { it.id == id }
    }
}
