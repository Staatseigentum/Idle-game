package com.embercrown.game

import com.embercrown.game.game.nowEpochSeconds
import com.embercrown.game.reboot.RebootState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

/** Developer-only fixture for repeatable store screenshots; never touches the regular save node. */
fun main(args: Array<String>) {
    val profile = args.firstOrNull() ?: error("Pass one isolated profile name")
    val fresh = args.getOrNull(1) == "fresh"
    require(args.size in 1..2 && (args.size == 1 || fresh))
    require(Regex("[A-Za-z0-9_-]{1,48}").matches(profile))
    val profileNode = Preferences.userRoot().node("com/embercrown/game/profiles/$profile")
    val state = if (fresh) RebootState(
        tutorialStep = 0,
        tutorialAcknowledged = false,
        lastPlayedEpochSeconds = nowEpochSeconds(),
    ) else RebootState(
        embers = 420_000.0,
        lifetimeEmbers = 950_000.0,
        levels = mapOf("coalpit" to 18, "hollowmill" to 7, "belltower" to 3, "moonforge" to 1),
        buildingUpgrades = mapOf("coalpit" to 1, "hollowmill" to 1),
        claimedMilestones = setOf("spark", "watch"),
        chronicleEntries = setOf("kindled", "hundred_taps", "first_mastery", "bells", "march_scout"),
        gloom = 28.0,
        relics = 4,
        relicUpgrades = mapOf("cinderheart" to 1),
        totalTaps = 160,
        expeditionsCompleted = 2,
        fragments = mapOf("forest" to 5),
        tutorialAcknowledged = true,
        runSeconds = 1_800.0,
        playedSeconds = 1_800.0,
        lastPlayedEpochSeconds = nowEpochSeconds(),
    )
    profileNode.put("embercrown_ash_kingdom_v1", Json.encodeToString(state))
    profileNode.put("embercrown_language_v1", "en")
    profileNode.flush()
    println("Seeded ${if (fresh) "fresh" else "showcase"} screenshot profile: $profile")
}
