package com.embercrown.game.reboot

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {
    @Test
    fun guidedStepsFollowRealActionsAndRewardOnlyOnce() {
        val fresh = RebootState(tutorialStep = 1, tutorialAcknowledged = false)
        assertEquals(1, withTutorialProgress(fresh).tutorialStep)
        val ready = withTutorialProgress(fresh.copy(embers = 15.0, lifetimeEmbers = 15.0))
        assertEquals(2, ready.tutorialStep)
        val built = withTutorialProgress(ready.copy(embers = 0.0, levels = mapOf("coalpit" to 1)))
        assertEquals(3, built.tutorialStep)
        assertEquals(80.0, built.embers)
        assertEquals(95.0, built.lifetimeEmbers)
        assertEquals(built, withTutorialProgress(built))
        val leveled = withTutorialProgress(built.copy(levels = mapOf("coalpit" to 5)))
        assertEquals(4, leveled.tutorialStep)
        val mastered = withTutorialProgress(leveled.copy(buildingUpgrades = mapOf("coalpit" to 1)))
        assertEquals(TUTORIAL_DONE, mastered.tutorialStep)
        assertFalse(mastered.tutorialAcknowledged)
    }

    @Test
    fun oldSavesDoNotSuddenlyEnterTutorial() {
        val legacy = RebootState(levels = mapOf("coalpit" to 15))
        assertEquals(TUTORIAL_DONE, legacy.tutorialStep)
        assertEquals(legacy, withTutorialProgress(legacy))
        val loaded = Json.decodeFromString<RebootState>("""{"levels":{"coalpit":15}}""")
        assertEquals(TUTORIAL_DONE, loaded.tutorialStep)
        assertTrue(loaded.tutorialAcknowledged)
    }

    @Test
    fun pausedGuideSurvivesSerialization() {
        val state = RebootState(embers = 200.0, tutorialStep = 4, tutorialAcknowledged = false)
        assertEquals(state, Json.decodeFromString<RebootState>(Json.encodeToString(state)))
    }

    @Test
    fun welcomeDoesNotAccumulateGloomOrOfflineTime() {
        val welcome = RebootState(tutorialStep = 0, tutorialAcknowledged = false)
        val advanced = advanceReboot(welcome, 3_600.0, active = false, now = 100L)
        assertEquals(8.0, advanced.gloom)
        assertEquals(0.0, advanced.embers)
        assertEquals(0.0, advanced.playedSeconds)
        assertEquals(100L, advanced.lastPlayedEpochSeconds)
    }

    @Test
    fun throneRoomRevealsSystemsOnlyAfterTheirStoryTriggers() {
        val fresh = RebootState(tutorialStep = 1, tutorialAcknowledged = false)
        assertEquals(listOf(ThronePage.KINGDOM, ThronePage.SYSTEM), visibleThronePages(fresh))
        val pit = withDiscoveries(fresh.copy(levels = mapOf("coalpit" to 1), tutorialStep = 3))
        assertTrue(ThronePage.BUILDINGS in visibleThronePages(pit))
        assertFalse(ThronePage.MARCHES in visibleThronePages(pit))
        val scouts = withDiscoveries(pit.copy(levels = pit.levels + ("scoutlodge" to 1)))
        assertTrue(ThronePage.MARCHES in visibleThronePages(scouts))
        val nextReign = scouts.copy(levels = emptyMap(), tutorialStep = TUTORIAL_DONE)
        assertTrue(ThronePage.MARCHES in visibleThronePages(nextReign))
    }

    @Test
    fun buildingListShowsOnlyCurrentChapterAndNextDiscovery() {
        val fresh = RebootState(tutorialStep = 1, tutorialAcknowledged = false)
        assertEquals(listOf("coalpit"), visibleThroneBuildings(fresh).map { it.id })
        val afterGuide = fresh.copy(tutorialStep = TUTORIAL_DONE, lifetimeEmbers = 100.0)
        assertEquals(listOf("coalpit", "emberorchard", "hollowmill", "lanternwatch"),
            visibleThroneBuildings(afterGuide).map { it.id })
    }

    @Test
    fun newSystemIntroductionsGuidePlayersButDoNotSpamLegacySaves() {
        val newPlayer = withDiscoveries(RebootState(tutorialStep = TUTORIAL_DONE,
            tutorialAcknowledged = true, introductionsSeen = emptySet(),
            discoveredSystems = setOf("buildings"), claimedMilestones = setOf("spark")))
        assertEquals("buildings", nextThroneIntroduction(newPlayer))
        assertEquals("realm", nextThroneIntroduction(newPlayer.copy(introductionsSeen = setOf("buildings"))))
        val legacy = withDiscoveries(RebootState(levels = mapOf("coalpit" to 10),
            claimedMilestones = setOf("spark")))
        assertNull(nextThroneIntroduction(legacy))
    }

    @Test
    fun gloomWaitsUntilTheGuidedOpeningIsOver() {
        val guide = RebootState(tutorialStep = 4, levels = mapOf("coalpit" to 5), gloom = 8.0)
        assertEquals(8.0, advanceReboot(guide, 120.0, active = true, now = 120).gloom)
    }
}
