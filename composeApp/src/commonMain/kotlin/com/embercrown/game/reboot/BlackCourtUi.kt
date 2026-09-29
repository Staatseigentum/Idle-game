package com.embercrown.game.reboot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class GuideChapter(val id: String, val title: StringResource, val body: StringResource)

private val guideChapters = listOf(
    GuideChapter("gloom", Res.string.guide_gloom_title, Res.string.guide_gloom_body),
    GuideChapter("patrol", Res.string.guide_patrol_title, Res.string.guide_patrol_body),
    GuideChapter("ritual", Res.string.guide_ritual_title, Res.string.guide_ritual_body),
)

@androidx.compose.runtime.Composable
internal fun ContextualGuide(state: RebootState, onRitual: () -> Unit, modifier: Modifier = Modifier) {
    if (state.tutorialStep < TUTORIAL_DONE || !state.tutorialAcknowledged) return
    val chapter = guideChapters.firstOrNull { guide ->
        guide.id !in state.guideSeen && when (guide.id) {
            "gloom" -> state.gloom >= 18.0 && state.level("coalpit") >= 5
            "patrol" -> state.level("scoutlodge") >= 1
            else -> canRitual(state) && state.reign == 1
        }
    } ?: return
    Column(
        modifier = modifier.fillMaxWidth().background(AshPalette.panelRaised)
            .border(2.dp, tutorialHighlightColor(true, AshPalette.flame)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(stringResource(Res.string.guide_chapter), AshPalette.flameLight, 8)
        Label(stringResource(chapter.title), AshPalette.bone, 9)
        Body(stringResource(chapter.body), AshPalette.ash)
        when (chapter.id) {
            "gloom" -> {
                Body(stringResource(Res.string.guide_gloom_penalty, gloomPenaltyPercent(state)), AshPalette.teal)
                Body(stringResource(if (state.beaconsLit == 0) Res.string.guide_gloom_action
                    else Res.string.guide_gloom_done), AshPalette.flameLight)
                if (state.beaconsLit == 0) AshButton(stringResource(Res.string.guide_try_beacon),
                    state.beaconCooldownSeconds == 0 && state.embers >= beaconCost(state),
                    pulse = true, modifier = Modifier.fillMaxWidth(),
                    onClick = RebootGraph.engine::stokeBeacon)
            }
            "patrol" -> {
                Body(if (state.patrolsCompleted == 0)
                    stringResource(Res.string.guide_patrol_action)
                    else stringResource(Res.string.guide_patrol_done, state.lastPatrolReward), AshPalette.flameLight)
                if (state.patrolsCompleted == 0) AshButton(stringResource(Res.string.guide_send_patrol),
                    state.patrol == null && state.embers >= PatrolRoutes.all.first().emberCost,
                    pulse = true, modifier = Modifier.fillMaxWidth(),
                    onClick = { RebootGraph.engine.startPatrol("hearth") })
            }
            "ritual" -> {
                Body(stringResource(Res.string.guide_ritual_reward, ritualReward(state)), AshPalette.teal)
                AshButton(stringResource(Res.string.guide_review_ritual), true,
                    color = AshPalette.crimson, pulse = true, modifier = Modifier.fillMaxWidth(),
                    onClick = onRitual)
            }
        }
        AshButton(stringResource(Res.string.guide_understood), true,
            color = AshPalette.panel, modifier = Modifier.fillMaxWidth(),
            onClick = { RebootGraph.engine.acknowledgeGuide(chapter.id) })
    }
}

@androidx.compose.runtime.Composable
internal fun GuideLibrary(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.guide_library), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.guide_library_hint))
        guideChapters.forEach { chapter ->
            Column(Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, AshPalette.edge).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(stringResource(chapter.title), AshPalette.bone, 8)
                Body(stringResource(chapter.body))
            }
        }
        Body(stringResource(Res.string.guide_gloom_penalty, gloomPenaltyPercent(state)), AshPalette.teal)
    }
}

@androidx.compose.runtime.Composable
internal fun PatrolPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.patrol_title), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.patrol_intro))
        if (state.level("scoutlodge") == 0) {
            Body(stringResource(Res.string.patrol_locked))
            return@Column
        }
        state.patrol?.let { patrol ->
            Body(stringResource(Res.string.patrol_active, stringResource(patrolTitle(patrol.routeId)),
                patrol.remainingSeconds), AshPalette.teal)
        }
        if (state.runPatrols > 0) {
            Body(stringResource(Res.string.patrol_result, state.lastPatrolReward), AshPalette.teal)
        }
        PatrolRoutes.all.forEach { route ->
            if (state.lifetimeEmbers >= route.unlockAt) {
                Column(Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp,
                    if (state.patrol == null && state.embers >= route.emberCost) AshPalette.flame else AshPalette.edge)
                    .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label(stringResource(patrolTitle(route.id)), AshPalette.bone, 8)
                    Body(stringResource(Res.string.patrol_reward, route.gloomCleared.toInt(), route.fragments,
                        stringResource(marchTitle(route.regionId))), AshPalette.muted)
                    Body(stringResource(Res.string.patrol_duration, patrolDuration(route, state)), AshPalette.muted)
                    AshButton(stringResource(Res.string.patrol_launch, formatAmount(route.emberCost)),
                        state.patrol == null && state.embers >= route.emberCost,
                        pulse = state.patrolsCompleted == 0 && route.id == "hearth",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { RebootGraph.engine.startPatrol(route.id) })
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
internal fun BlackCourtPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Label(stringResource(Res.string.court_title), AshPalette.crimson, 10)
        Body(stringResource(Res.string.court_intro))
        Body(stringResource(Res.string.court_progress, state.courtVictories.size, BlackCourt.all.size), AshPalette.teal)
        if (state.eclipseSiegeStage < 3) {
            Body(stringResource(Res.string.court_locked))
            return@Column
        }
        Label(stringResource(Res.string.court_tactic), AshPalette.flameLight, 8)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CourtTactics.all.forEach { tactic ->
                AshButton(stringResource(tacticTitle(tactic)), true,
                    color = if (state.courtTactic == tactic) AshPalette.flame else AshPalette.panelRaised,
                    modifier = Modifier.weight(1f),
                    onClick = { RebootGraph.engine.setCourtTactic(tactic) })
            }
        }
        val next = BlackCourt.all.firstOrNull { it.id !in state.courtVictories }
        if (next == null) {
            Label(stringResource(Res.string.court_complete), AshPalette.teal, 9)
            return@Column
        }
        Column(Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp,
            if (canDefeatCourtLord(state, next.id)) AshPalette.flame else AshPalette.edge).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Label(stringResource(courtTitle(next.id)), AshPalette.bone, 9)
            Body(stringResource(courtRequirement(next.id)))
            Body(stringResource(Res.string.court_required_tactic, stringResource(tacticTitle(next.tactic))), AshPalette.teal)
            AshButton(stringResource(Res.string.court_challenge, formatAmount(courtCost(next, state))),
                canDefeatCourtLord(state, next.id), pulse = canDefeatCourtLord(state, next.id),
                color = AshPalette.crimson, modifier = Modifier.fillMaxWidth(),
                onClick = { RebootGraph.engine.challengeCourt(next.id) })
        }
    }
}

@androidx.compose.runtime.Composable
internal fun BlueprintPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.blueprint_title), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.blueprint_hint))
        Body(stringResource(Res.string.blueprint_saved, state.blueprintLevels.size), AshPalette.teal)
        AshButton(stringResource(Res.string.blueprint_record), state.levels.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(), onClick = RebootGraph.engine::saveBlueprint)
        AshButton(stringResource(Res.string.blueprint_build), state.blueprintLevels.isNotEmpty(),
            color = AshPalette.teal, modifier = Modifier.fillMaxWidth(),
            onClick = RebootGraph.engine::buildBlueprint)
    }
}

private fun patrolTitle(id: String): StringResource = when (id) {
    "hearth" -> Res.string.patrol_hearth
    "border" -> Res.string.patrol_border
    else -> Res.string.patrol_eclipse
}

private fun tacticTitle(id: String): StringResource = when (id) {
    "ward" -> Res.string.court_ward
    "guile" -> Res.string.court_guile
    else -> Res.string.court_strike
}

private fun courtTitle(id: String): StringResource = when (id) {
    "gatekeeper" -> Res.string.court_gatekeeper
    "mirror" -> Res.string.court_mirror
    "marshal" -> Res.string.court_marshal
    "oracle" -> Res.string.court_oracle
    else -> Res.string.court_sovereign
}

private fun courtRequirement(id: String): StringResource = when (id) {
    "gatekeeper" -> Res.string.court_gatekeeper_need
    "mirror" -> Res.string.court_mirror_need
    "marshal" -> Res.string.court_marshal_need
    "oracle" -> Res.string.court_oracle_need
    else -> Res.string.court_sovereign_need
}
