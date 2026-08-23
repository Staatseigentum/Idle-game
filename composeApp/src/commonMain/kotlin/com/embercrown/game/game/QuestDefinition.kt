package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.quest_academy_level_three
import com.embercrown.game.resources.quest_dynasty_level_five
import com.embercrown.game.resources.quest_fifty_gold
import com.embercrown.game.resources.quest_first_building
import com.embercrown.game.resources.quest_first_verfall
import com.embercrown.game.resources.quest_first_wiedergeburt
import com.embercrown.game.resources.quest_five_achievements
import com.embercrown.game.resources.quest_milestone_building
import com.embercrown.game.resources.quest_reach_age_five
import com.embercrown.game.resources.quest_reach_village
import com.embercrown.game.resources.quest_ten_levels
import com.embercrown.game.resources.quest_three_high_levels
import org.jetbrains.compose.resources.StringResource

data class QuestDefinition(
    val id: String,
    val nameRes: StringResource,
    val rewardGold: Double,
    val condition: (GameState) -> Boolean,
) {
    companion object {
        // Placeholder reward amounts — tune against the gold economy once playtested.
        val all: List<QuestDefinition> = listOf(
            QuestDefinition("first_building", Res.string.quest_first_building, 25.0) { s -> s.buildings.any { it.level >= 1 } },
            QuestDefinition("fifty_gold", Res.string.quest_fifty_gold, 20.0) { it.lifetimeGold >= 50.0 },
            QuestDefinition("reach_village", Res.string.quest_reach_village, 100.0) { it.currentAge.index >= 2 },
            QuestDefinition("ten_levels", Res.string.quest_ten_levels, 50.0) { s -> s.buildings.sumOf { it.level } >= 10 },
            QuestDefinition("milestone_building", Res.string.quest_milestone_building, 500.0) { s -> s.buildings.any { it.level >= 25 } },
            QuestDefinition("first_verfall", Res.string.quest_first_verfall, 200.0) { it.verfallCount >= 1 },
            QuestDefinition("academy_level_three", Res.string.quest_academy_level_three, 300.0) { it.academyLevel >= 3 },
            QuestDefinition("five_achievements", Res.string.quest_five_achievements, 400.0) { it.unlockedAchievements.size >= 5 },
            QuestDefinition("reach_age_five", Res.string.quest_reach_age_five, 800.0) { it.currentAge.index >= 5 },
            QuestDefinition("first_wiedergeburt", Res.string.quest_first_wiedergeburt, 1500.0) { it.wiedergeburtCount >= 1 },
            QuestDefinition("dynasty_level_five", Res.string.quest_dynasty_level_five, 1000.0) { s -> (s.legacyLevels[s.activeDynastyPath] ?: 0) >= 5 },
            QuestDefinition("three_high_levels", Res.string.quest_three_high_levels, 600.0) { s -> s.buildings.count { it.level >= 10 } >= 3 },
        )

        fun byId(id: String): QuestDefinition = all.first { it.id == id }
    }
}
