package com.embercrown.game

import com.embercrown.game.game.nowEpochSeconds
import com.embercrown.game.reboot.RebootState
import com.embercrown.game.reboot.RebootBuildings
import com.embercrown.game.reboot.RebootMilestones
import com.embercrown.game.reboot.CrownChronicle
import com.embercrown.game.reboot.CrownDistricts
import com.embercrown.game.reboot.CrownArtifacts
import com.embercrown.game.reboot.CrownTrials
import com.embercrown.game.reboot.LostMarches
import com.embercrown.game.reboot.RoyalOrders
import com.embercrown.game.reboot.TUTORIAL_DONE
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

/** Developer-only fixtures for private preview profiles; never touch the regular save node. */
fun main(args: Array<String>) {
    val profile = args.firstOrNull() ?: error("Pass one isolated profile name")
    val mode = args.getOrNull(1) ?: "showcase"
    require(args.size in 1..2 && mode in setOf("showcase", "fresh", "all-achievements", "pre-ritual"))
    require(Regex("[A-Za-z0-9_-]{1,48}").matches(profile))
    val profileNode = Preferences.userRoot().node("com/embercrown/game/profiles/$profile")
    if (mode == "all-achievements") {
        require(profileNode.get("embercrown_ash_kingdom_v1", null) == null) {
            "Refusing to overwrite an existing achievement preview profile"
        }
    }
    val state = if (mode == "pre-ritual") RebootState(
        embers = 2_400_000_000_000.0,
        lifetimeEmbers = 3_200_000_000_000.0,
        levels = RebootBuildings.all.associate { building -> building.id to when (building.id) {
            "coalpit", "emberorchard", "hollowmill", "lanternwatch" -> 24
            "eclipsethrone" -> 1
            else -> 8
        } },
        buildingUpgrades = mapOf("coalpit" to 2, "lanternwatch" to 1,
            "scoutlodge" to 1, "shadowfoundry" to 1),
        claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
        relics = 15,
        reign = 1,
        gloom = 24.0,
        omenCountdownSeconds = 3_600,
        beaconsLit = 3,
        patrolsCompleted = 2,
        runPatrols = 2,
        lastPatrolReward = 2,
        fragments = mapOf("forest" to 11, "glassfields" to 7, "fen" to 5),
        conqueredRegions = setOf("forest", "glassfields"),
        outpostLevels = mapOf("forest" to 1),
        craftedArtifacts = setOf("cinder_crown"),
        equippedArtifacts = setOf("cinder_crown"),
        specializations = mapOf("coalpit" to "industry", "scoutlodge" to "utility"),
        blueprintLevels = mapOf("coalpit" to 20, "emberorchard" to 15,
            "hollowmill" to 12, "lanternwatch" to 10),
        guideSeen = setOf("gloom", "patrol"),
        tutorialStep = TUTORIAL_DONE,
        tutorialAcknowledged = true,
        runSeconds = 10_200.0,
        playedSeconds = 10_200.0,
        lastPlayedEpochSeconds = nowEpochSeconds(),
    ) else if (mode == "fresh") RebootState(
        tutorialStep = 0,
        tutorialAcknowledged = false,
        lastPlayedEpochSeconds = nowEpochSeconds(),
    ) else if (mode == "all-achievements") RebootState(
        embers = 4_850_000_000_000.0,
        lifetimeEmbers = 6_300_000_000_000.0,
        levels = RebootBuildings.all.associate { building ->
            building.id to when (building.id) {
                "coalpit", "hollowmill", "belltower" -> 55
                "eclipsethrone" -> 3
                else -> 25
            }
        },
        buildingUpgrades = RebootBuildings.all.associate { it.id to 2 },
        claimedMilestones = RebootMilestones.all.map { it.id }.toSet(),
        chronicleEntries = CrownChronicle.all.map { it.id }.toSet(),
        cosmeticStyles = mapOf("flame" to "moonfire", "banner" to "eclipse",
            "sky" to "eclipse", "map" to "gilded"),
        featuredTrophies = listOf("march_all", "artifacts_all", "trials_all", "eclipse_siege"),
        gloom = 38.0,
        relics = 48,
        reign = 4,
        beaconsLit = 10,
        totalTaps = 280,
        omensResolved = 8,
        expeditionsCompleted = 16,
        claimedOrders = RoyalOrders.all.map { it.id }.toSet(),
        districtLevels = CrownDistricts.all.associate { it.id to CrownDistricts.MAX_LEVEL },
        outpostLevels = LostMarches.all.associate { it.id to 3 },
        craftedArtifacts = CrownArtifacts.all.map { it.id }.toSet(),
        equippedArtifacts = setOf("cinder_crown", "tide_compass"),
        completedTrials = CrownTrials.all.toSet(),
        eclipseSiegeStage = 3,
        fragments = LostMarches.all.associate { it.id to 20 },
        conqueredRegions = LostMarches.all.map { it.id }.toSet(),
        specializations = mapOf("coalpit" to "industry", "moonforge" to "utility",
            "bonelibrary" to "utility", "citadel" to "industry"),
        relicSetId = "emberguard",
        tutorialStep = TUTORIAL_DONE,
        tutorialAcknowledged = true,
        runSeconds = 8_400.0,
        playedSeconds = 32_000.0,
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
    profileNode.put("embercrown_language_v1", if (mode == "pre-ritual") "de" else "en")
    profileNode.flush()
    println("Seeded $mode preview profile: $profile")
}
