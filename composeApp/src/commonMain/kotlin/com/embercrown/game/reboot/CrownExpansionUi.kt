package com.embercrown.game.reboot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun ExpansionCard(color: Color = AshPalette.edge, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, color).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), content = content,
    )
}

@Composable
internal fun RoyalOrdersPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.exp_orders), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.exp_orders_hint))
        RoyalOrders.all.filter { it.id !in state.claimedOrders }.take(3).forEach { order ->
            val ready = order.ready(state)
            ExpansionCard(if (ready) AshPalette.flame else AshPalette.edge) {
                Body(stringResource(orderTitle(order.id)), AshPalette.bone)
                Label(stringResource(Res.string.exp_order_reward, formatAmount(royalOrderReward(state, order))), AshPalette.teal, 8)
                AshButton(
                    label = stringResource(Res.string.exp_order_claim), enabled = ready, pulse = ready,
                    modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.claimOrder(order.id) },
                )
            }
        }
        if (state.claimedOrders.size == RoyalOrders.all.size) {
            Label(stringResource(Res.string.exp_order_done), AshPalette.teal, 8)
        }
    }
}

@Composable
internal fun CrownDistrictsPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.exp_districts), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.exp_districts_hint))
        CrownDistricts.all.forEachIndexed { index, district ->
            if (index > 0 && state.lifetimeEmbers < CrownDistricts.all[index - 1].unlockAt) return@forEachIndexed
            val level = state.districtLevels[district.id] ?: 0
            val cost = districtCost(district, state)
            val open = state.lifetimeEmbers >= district.unlockAt
            ExpansionCard(if (level == CrownDistricts.MAX_LEVEL) AshPalette.teal else AshPalette.edge) {
                Label(stringResource(districtTitle(district.id)), AshPalette.bone, 8)
                Body(stringResource(districtDescription(district.id)))
                Label(stringResource(Res.string.exp_district_level, level), AshPalette.teal, 8)
                if (!open) Body(stringResource(Res.string.exp_district_unlock, formatAmount(district.unlockAt)))
                AshButton(
                    label = if (level == CrownDistricts.MAX_LEVEL) stringResource(Res.string.exp_district_max)
                        else stringResource(Res.string.exp_district_cost, formatAmount(cost)),
                    enabled = open && level < CrownDistricts.MAX_LEVEL && state.embers >= cost,
                    modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.improveDistrict(district.id) },
                )
            }
        }
    }
}

@Composable
internal fun BorderOutpostPanel(state: RebootState, region: LostRegion) {
    if (region.id !in state.conqueredRegions) return
    val level = state.outpostLevels[region.id] ?: 0
    val fragmentCost = outpostFragmentCost(state, region.id)
    val emberCost = outpostEmberCost(state, region)
    ExpansionCard(if (level >= 3) AshPalette.teal else AshPalette.edge) {
        Label(stringResource(Res.string.exp_outpost_title), AshPalette.flameLight, 8)
        Body(stringResource(Res.string.exp_outpost_hint))
        Label(stringResource(Res.string.exp_outpost_level, level), AshPalette.teal, 8)
        AshButton(
            label = if (level >= 3) stringResource(Res.string.exp_outpost_max)
                else stringResource(Res.string.exp_outpost_cost, formatAmount(emberCost), fragmentCost),
            enabled = level < 3 && state.embers >= emberCost &&
                (state.fragments[region.id] ?: 0) >= fragmentCost,
            modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.improveOutpost(region.id) },
        )
    }
}

@Composable
internal fun CrownArtifactsPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.exp_artifacts), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.exp_artifacts_hint))
        Label(stringResource(Res.string.exp_artifact_slots, state.equippedArtifacts.size), AshPalette.teal, 8)
        CrownArtifacts.all.forEach { artifact ->
            val crafted = artifact.id in state.craftedArtifacts
            val equipped = artifact.id in state.equippedArtifacts
            val conquered = artifact.regionId in state.conqueredRegions
            ExpansionCard(if (equipped) AshPalette.teal else AshPalette.edge) {
                Label(stringResource(artifactTitle(artifact.id)), AshPalette.bone, 8)
                Body(stringResource(artifactDescription(artifact.id)))
                if (crafted) {
                    AshButton(
                        label = stringResource(if (equipped) Res.string.exp_artifact_unequip else Res.string.exp_artifact_equip),
                        enabled = equipped || state.equippedArtifacts.size < 2,
                        color = if (equipped) AshPalette.teal else AshPalette.flame,
                        modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.equipArtifact(artifact.id) },
                    )
                } else {
                    if (!conquered) Body(stringResource(Res.string.exp_artifact_locked,
                        stringResource(marchTitle(artifact.regionId))))
                    AshButton(
                        label = stringResource(Res.string.exp_artifact_cost, artifact.fragmentCost, artifact.relicCost),
                        enabled = conquered && state.relics >= artifact.relicCost &&
                            (state.fragments[artifact.regionId] ?: 0) >= artifact.fragmentCost,
                        modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.forgeArtifact(artifact.id) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun CrownTrialsPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.exp_trials), AshPalette.crimson, 9)
        Body(stringResource(Res.string.exp_trials_hint))
        if (state.reign < 2) {
            Body(stringResource(Res.string.exp_trial_locked))
            return@Column
        }
        state.activeTrialId?.let { active ->
            Body(stringResource(Res.string.exp_trial_active, stringResource(trialTitle(active))), AshPalette.flameLight)
            if (!trialGoalMet(state)) Body(stringResource(when (active) {
                "marches" -> Res.string.exp_trial_marches_goal
                "watchfires" -> Res.string.exp_trial_watchfires_goal
                else -> Res.string.exp_trial_architect_goal
            }))
        }
        Body(stringResource(Res.string.exp_trial_next,
            state.nextTrialId?.let { stringResource(trialTitle(it)) } ?: stringResource(Res.string.exp_trial_none)), AshPalette.teal)
        CrownTrials.all.forEach { id ->
            ExpansionCard(if (state.nextTrialId == id) AshPalette.flame else AshPalette.edge) {
                Label(stringResource(trialTitle(id)), AshPalette.bone, 8)
                Body(stringResource(trialDescription(id)))
                if (id in state.completedTrials) Label(stringResource(Res.string.exp_trial_completed), AshPalette.teal, 8)
                AshButton(
                    label = stringResource(Res.string.exp_trial_choose), enabled = state.nextTrialId != id,
                    modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.queueTrial(id) },
                )
            }
        }
        AshButton(stringResource(Res.string.exp_trial_none), state.nextTrialId != null,
            modifier = Modifier.fillMaxWidth(), color = AshPalette.panelRaised,
            onClick = { RebootGraph.engine.queueTrial(null) })
    }
}

@Composable
internal fun EclipseSiegePanel(state: RebootState) {
    val stage = state.eclipseSiegeStage
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.exp_siege), AshPalette.crimson, 10)
        Body(stringResource(Res.string.exp_siege_hint))
        if (stage >= eclipseSiegeCosts.size) {
            ExpansionCard(AshPalette.teal) { Label(stringResource(Res.string.exp_siege_victory), AshPalette.teal, 8) }
            return@Column
        }
        ExpansionCard(if (canAdvanceEclipseSiege(state)) AshPalette.flame else AshPalette.edge) {
            Label(stringResource(Res.string.exp_siege_stage, stage + 1), AshPalette.teal, 8)
            Label(stringResource(when (stage) {
                0 -> Res.string.exp_siege_breach
                1 -> Res.string.exp_siege_ward
                else -> Res.string.exp_siege_crown
            }), AshPalette.bone, 9)
            Body(stringResource(when (stage) {
                0 -> Res.string.exp_siege_need_breach
                1 -> Res.string.exp_siege_need_ward
                else -> Res.string.exp_siege_need_crown
            }))
            AshButton(
                label = stringResource(Res.string.exp_siege_assault, formatAmount(eclipseSiegeCosts[stage])),
                enabled = canAdvanceEclipseSiege(state), pulse = canAdvanceEclipseSiege(state),
                color = AshPalette.crimson, modifier = Modifier.fillMaxWidth(),
                onClick = RebootGraph.engine::assaultEclipse,
            )
        }
    }
}

private fun orderTitle(id: String): StringResource = when (id) {
    "first_sparks" -> Res.string.exp_order_first_sparks
    "coal_line" -> Res.string.exp_order_coal_line
    "mill_shift" -> Res.string.exp_order_mill_shift
    "first_master" -> Res.string.exp_order_first_master
    "bell_watch" -> Res.string.exp_order_bell_watch
    "beacon_watch" -> Res.string.exp_order_beacon_watch
    "first_march" -> Res.string.exp_order_first_march
    "first_patrol" -> Res.string.exp_order_first_patrol
    "glass_road" -> Res.string.exp_order_glass_road
    "first_choice" -> Res.string.exp_order_first_choice
    "night_caravan" -> Res.string.exp_order_night_caravan
    "black_pass" -> Res.string.exp_order_black_pass
    "court_watch" -> Res.string.exp_order_court_watch
    "last_oath" -> Res.string.exp_order_last_oath
    else -> Res.string.exp_order_first_banner
}

private fun districtTitle(id: String): StringResource = when (id) {
    "hearth" -> Res.string.exp_district_hearth
    "bell" -> Res.string.exp_district_bell
    "bone" -> Res.string.exp_district_bone
    else -> Res.string.exp_district_storm
}

private fun districtDescription(id: String): StringResource = when (id) {
    "hearth" -> Res.string.exp_district_hearth_desc
    "bell" -> Res.string.exp_district_bell_desc
    "bone" -> Res.string.exp_district_bone_desc
    else -> Res.string.exp_district_storm_desc
}

private fun artifactTitle(id: String): StringResource = when (id) {
    "cinder_crown" -> Res.string.exp_artifact_cinder_crown
    "marsh_lantern" -> Res.string.exp_artifact_marsh_lantern
    "tide_compass" -> Res.string.exp_artifact_tide_compass
    else -> Res.string.exp_artifact_eclipse_sigil
}

private fun artifactDescription(id: String): StringResource = when (id) {
    "cinder_crown" -> Res.string.exp_artifact_cinder_crown_desc
    "marsh_lantern" -> Res.string.exp_artifact_marsh_lantern_desc
    "tide_compass" -> Res.string.exp_artifact_tide_compass_desc
    else -> Res.string.exp_artifact_eclipse_sigil_desc
}

private fun trialTitle(id: String): StringResource = when (id) {
    "cinders" -> Res.string.exp_trial_cinders
    "night" -> Res.string.exp_trial_night
    "marches" -> Res.string.exp_trial_marches
    "watchfires" -> Res.string.exp_trial_watchfires
    else -> Res.string.exp_trial_architect
}

private fun trialDescription(id: String): StringResource = when (id) {
    "cinders" -> Res.string.exp_trial_cinders_desc
    "night" -> Res.string.exp_trial_night_desc
    "marches" -> Res.string.exp_trial_marches_desc
    "watchfires" -> Res.string.exp_trial_watchfires_desc
    else -> Res.string.exp_trial_architect_desc
}
