package com.embercrown.game.game

import kotlin.random.Random

/**
 * Procedural first names for a newly crowned Heir. Deliberately not localized — proper names read
 * the same in both languages, unlike the trait epithet shown alongside them (which uses the
 * trait's own [HeirTraitDefinition.nameRes]).
 */
object HeirNames {
    private val prefixes = listOf(
        "Ald", "Bram", "Cor", "Dun", "Ed", "Fenn", "Gar", "Hal", "Ing", "Jor",
        "Kael", "Lor", "Mor", "Nor", "Os", "Rand", "Syl", "Thal", "Ul", "Wyn",
    )
    private val suffixes = listOf(
        "ric", "wyn", "gard", "an", "or", "eth", "in", "ard", "iel", "mund",
    )

    fun random(random: Random = Random): String = prefixes.random(random) + suffixes.random(random)
}
