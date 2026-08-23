package com.embercrown.game.game

import com.embercrown.game.resources.Res
import com.embercrown.game.resources.chronicle_entry_achievement_unlocked
import com.embercrown.game.resources.chronicle_entry_age_advanced
import com.embercrown.game.resources.chronicle_entry_blight_embraced
import com.embercrown.game.resources.chronicle_entry_corruption_peaked
import com.embercrown.game.resources.chronicle_entry_rebirth
import org.jetbrains.compose.resources.StringResource

/** The text template behind one [ChronicleEntry.kind] — `%1$s`/`%2$s` placeholders only, filled from [ChronicleEntry.args]. */
data class ChronicleEntryType(val kind: String, val templateRes: StringResource) {
    companion object {
        val all: List<ChronicleEntryType> = listOf(
            ChronicleEntryType("age_advanced", Res.string.chronicle_entry_age_advanced),
            ChronicleEntryType("blight_embraced", Res.string.chronicle_entry_blight_embraced),
            ChronicleEntryType("rebirth", Res.string.chronicle_entry_rebirth),
            ChronicleEntryType("achievement_unlocked", Res.string.chronicle_entry_achievement_unlocked),
            ChronicleEntryType("corruption_peaked", Res.string.chronicle_entry_corruption_peaked),
        )

        fun byKind(kind: String): ChronicleEntryType = all.first { it.kind == kind }
    }
}
