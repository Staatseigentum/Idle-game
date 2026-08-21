package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.age_camp
import com.embercrown.game.resources.age_continental_realm
import com.embercrown.game.resources.age_county
import com.embercrown.game.resources.age_duchy
import com.embercrown.game.resources.age_embercrown
import com.embercrown.game.resources.age_empire
import com.embercrown.game.resources.age_eternal_realm
import com.embercrown.game.resources.age_free_city
import com.embercrown.game.resources.age_hamlet
import com.embercrown.game.resources.age_kingdom
import com.embercrown.game.resources.age_legendary_realm
import com.embercrown.game.resources.age_market_town
import com.embercrown.game.resources.age_realm_of_dreams
import com.embercrown.game.resources.age_realm_of_stars
import com.embercrown.game.resources.age_shadow_realm
import com.embercrown.game.resources.age_town
import com.embercrown.game.resources.age_united_kingdom
import com.embercrown.game.resources.age_village
import com.embercrown.game.resources.chronicle_camp
import com.embercrown.game.resources.chronicle_continental_realm
import com.embercrown.game.resources.chronicle_county
import com.embercrown.game.resources.chronicle_duchy
import com.embercrown.game.resources.chronicle_embercrown
import com.embercrown.game.resources.chronicle_empire
import com.embercrown.game.resources.chronicle_eternal_realm
import com.embercrown.game.resources.chronicle_free_city
import com.embercrown.game.resources.chronicle_hamlet
import com.embercrown.game.resources.chronicle_kingdom
import com.embercrown.game.resources.chronicle_legendary_realm
import com.embercrown.game.resources.chronicle_market_town
import com.embercrown.game.resources.chronicle_realm_of_dreams
import com.embercrown.game.resources.chronicle_realm_of_stars
import com.embercrown.game.resources.chronicle_shadow_realm
import com.embercrown.game.resources.chronicle_town
import com.embercrown.game.resources.chronicle_united_kingdom
import com.embercrown.game.resources.chronicle_village
import org.jetbrains.compose.resources.StringResource
import kotlin.math.pow

data class AgeDefinition(
    val index: Int,
    val nameRes: StringResource,
    val chronicleRes: StringResource,
    val lifetimeGoldThreshold: Double,
) {
    companion object {
        val all: List<AgeDefinition> = listOf(
            Res.string.age_camp to Res.string.chronicle_camp,
            Res.string.age_hamlet to Res.string.chronicle_hamlet,
            Res.string.age_village to Res.string.chronicle_village,
            Res.string.age_market_town to Res.string.chronicle_market_town,
            Res.string.age_town to Res.string.chronicle_town,
            Res.string.age_free_city to Res.string.chronicle_free_city,
            Res.string.age_county to Res.string.chronicle_county,
            Res.string.age_duchy to Res.string.chronicle_duchy,
            Res.string.age_kingdom to Res.string.chronicle_kingdom,
            Res.string.age_united_kingdom to Res.string.chronicle_united_kingdom,
            Res.string.age_empire to Res.string.chronicle_empire,
            Res.string.age_continental_realm to Res.string.chronicle_continental_realm,
            Res.string.age_legendary_realm to Res.string.chronicle_legendary_realm,
            Res.string.age_realm_of_dreams to Res.string.chronicle_realm_of_dreams,
            Res.string.age_shadow_realm to Res.string.chronicle_shadow_realm,
            Res.string.age_realm_of_stars to Res.string.chronicle_realm_of_stars,
            Res.string.age_eternal_realm to Res.string.chronicle_eternal_realm,
            Res.string.age_embercrown to Res.string.chronicle_embercrown,
        ).mapIndexed { index, (nameRes, chronicleRes) ->
            AgeDefinition(
                index = index,
                nameRes = nameRes,
                chronicleRes = chronicleRes,
                lifetimeGoldThreshold = if (index == 0) 0.0 else 100.0 * 8.0.pow(index - 1),
            )
        }

        fun currentAgeFor(lifetimeGold: Double): AgeDefinition =
            all.last { lifetimeGold >= it.lifetimeGoldThreshold }

        fun nextAgeFor(lifetimeGold: Double): AgeDefinition? =
            all.firstOrNull { lifetimeGold < it.lifetimeGoldThreshold }
    }
}
