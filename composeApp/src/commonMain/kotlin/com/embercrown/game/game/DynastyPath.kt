package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.dynasty_path_cunning
import com.embercrown.game.resources.dynasty_path_cunning_desc
import com.embercrown.game.resources.dynasty_path_magic
import com.embercrown.game.resources.dynasty_path_magic_desc
import com.embercrown.game.resources.dynasty_path_steel
import com.embercrown.game.resources.dynasty_path_steel_desc
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class DynastyPath(val nameRes: StringResource, val descriptionRes: StringResource, val bonusPerLevel: Double) {
    STEEL(Res.string.dynasty_path_steel, Res.string.dynasty_path_steel_desc, 0.05),
    MAGIC(Res.string.dynasty_path_magic, Res.string.dynasty_path_magic_desc, 0.15),
    CUNNING(Res.string.dynasty_path_cunning, Res.string.dynasty_path_cunning_desc, 0.20),
}
