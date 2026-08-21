package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.achievement_academy_initiate
import com.embercrown.game.resources.achievement_academy_master
import com.embercrown.game.resources.achievement_first_gold
import com.embercrown.game.resources.achievement_first_verfall
import com.embercrown.game.resources.achievement_hundred_gold
import com.embercrown.game.resources.achievement_hundred_levels
import com.embercrown.game.resources.achievement_reach_embercrown
import com.embercrown.game.resources.achievement_reach_kingdom
import com.embercrown.game.resources.achievement_reach_village
import com.embercrown.game.resources.achievement_ten_verfalls
import org.jetbrains.compose.resources.StringResource

data class AchievementDefinition(
    val id: String,
    val nameRes: StringResource,
    val condition: (GameState) -> Boolean,
) {
    companion object {
        val all: List<AchievementDefinition> = listOf(
            AchievementDefinition("first_gold", Res.string.achievement_first_gold) { it.lifetimeGold > 0.0 },
            AchievementDefinition("hundred_gold", Res.string.achievement_hundred_gold) { it.lifetimeGold >= 100.0 },
            AchievementDefinition("reach_village", Res.string.achievement_reach_village) { it.currentAge.index >= 2 },
            AchievementDefinition("reach_kingdom", Res.string.achievement_reach_kingdom) { it.currentAge.index >= 8 },
            AchievementDefinition("reach_embercrown", Res.string.achievement_reach_embercrown) { it.currentAge.index >= 17 },
            AchievementDefinition("first_verfall", Res.string.achievement_first_verfall) { it.verfallCount >= 1 },
            AchievementDefinition("ten_verfalls", Res.string.achievement_ten_verfalls) { it.verfallCount >= 10 },
            AchievementDefinition("academy_initiate", Res.string.achievement_academy_initiate) { it.academyLevel >= 1 },
            AchievementDefinition("academy_master", Res.string.achievement_academy_master) { it.academyLevel >= 10 },
            AchievementDefinition("hundred_levels", Res.string.achievement_hundred_levels) { s -> s.buildings.sumOf { it.level } >= 100 },
        )

        fun byId(id: String): AchievementDefinition = all.first { it.id == id }
    }
}
