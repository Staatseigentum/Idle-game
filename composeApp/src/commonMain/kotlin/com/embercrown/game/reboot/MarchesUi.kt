package com.embercrown.game.reboot

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelFit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MarchesPanel(state: RebootState, modifier: Modifier = Modifier) {
    var role by remember { mutableStateOf("warden") }
    var daring by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(160)
            frame = (frame + 1) % 240
        }
    }
    Column(
        modifier = modifier.background(AshPalette.night).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Label(stringResource(Res.string.march_title), AshPalette.flameLight, 11)
        Body(stringResource(Res.string.march_intro), AshPalette.ash)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // The art is 160x112 pixels. Keep that ratio on every screen so all four regions stay visible.
            val mapWidth = maxWidth.coerceAtMost(760.dp)
            Box(Modifier.width(mapWidth).height(mapWidth * 0.7f).clipToBounds()
                .border(1.dp, AshPalette.edge).background(AshPalette.void)) {
                Crossfade(targetState = state.conqueredRegions, animationSpec = tween(650)) { liberated ->
                    PixelArtImage(remember(liberated) { lostMarchesArt(liberated) },
                        Modifier.fillMaxSize(), fit = PixelFit.Contain)
                }
                PixelArtImage(remember(frame, state.expedition?.regionId) { lostMarchesMotionArt(frame, state.expedition) },
                    Modifier.fillMaxSize(), fit = PixelFit.Contain)
            }
        }
        state.expedition?.let { active ->
            Body(stringResource(Res.string.march_active, stringResource(marchTitle(active.regionId)),
                active.remainingSeconds), AshPalette.teal)
        }
        state.lastExpeditionRegion?.let { id ->
            Body(stringResource(Res.string.march_result, state.lastExpeditionReward,
                stringResource(marchTitle(id))), AshPalette.flameLight)
        }
        Rule()
        Label(stringResource(Res.string.march_role), AshPalette.teal, 8)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("scout", "warden", "occultist").forEach { option ->
                val title = when (option) {
                    "scout" -> Res.string.march_scout
                    "warden" -> Res.string.march_warden
                    else -> Res.string.march_occultist
                }
                AshButton(stringResource(title), true,
                    color = if (role == option) AshPalette.teal else AshPalette.panelRaised,
                    modifier = Modifier.weight(1f), onClick = { role = option })
            }
        }
        Label(stringResource(Res.string.march_approach), AshPalette.teal, 8)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AshButton(stringResource(Res.string.march_safe), true,
                color = if (!daring) AshPalette.teal else AshPalette.panelRaised,
                modifier = Modifier.weight(1f), onClick = { daring = false })
            AshButton(stringResource(Res.string.march_daring), true,
                color = if (daring) AshPalette.crimson else AshPalette.panelRaised,
                modifier = Modifier.weight(1f), onClick = { daring = true })
        }
        Body(stringResource(if (daring) Res.string.march_daring_desc else Res.string.march_safe_desc))
        LostMarches.all.forEach { region -> MarchRegionCard(state, region, role, daring) }
        Rule()
        RelicSetPanel(state)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun MarchRegionCard(state: RebootState, region: LostRegion, role: String, daring: Boolean) {
    val open = state.lifetimeEmbers >= region.unlockAt
    val conquered = region.id in state.conqueredRegions
    val title = stringResource(marchTitle(region.id))
    val buildingId = marchSpecializations.entries.first { it.value == region.id }.key
    Column(
        modifier = Modifier.fillMaxWidth().background(if (conquered) AshPalette.panelRaised else AshPalette.panel)
            .border(1.dp, if (conquered) AshPalette.teal else if (open) AshPalette.flame else AshPalette.edge)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(title, if (conquered) AshPalette.teal else AshPalette.flameLight, 9)
        Body(stringResource(marchDescription(region.id)), AshPalette.ash)
        if (!open) {
            Body(stringResource(Res.string.march_unlock, formatAmount(region.unlockAt)))
            return@Column
        }
        Label(stringResource(Res.string.march_fragments, state.fragments[region.id] ?: 0), AshPalette.teal, 8)
        if (conquered) Label(stringResource(Res.string.march_reclaimed), AshPalette.teal, 8)
        Body(stringResource(Res.string.march_duration, expeditionDuration(region, state, role)))
        AshButton(
            label = stringResource(Res.string.march_expedition, formatAmount(region.expeditionCost)),
            enabled = state.expedition == null && state.embers >= region.expeditionCost,
            modifier = Modifier.fillMaxWidth(),
            onClick = { RebootGraph.engine.startExpedition(region.id, role, daring) },
        )
        if (!conquered) {
            Rule()
            Body(stringResource(Res.string.march_siege_need, siegeFragmentCost(state),
                stringResource(marchBuildingTitle(region.defenderId)), region.defenderLevel))
            Body(stringResource(Res.string.march_siege_reward))
            AshButton(
                label = stringResource(Res.string.march_siege, formatAmount(region.siegeCost)),
                enabled = canConquer(state, region), color = AshPalette.crimson,
                modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.siege(region.id) },
            )
        }
        Rule()
        Label(stringResource(Res.string.march_specialize, stringResource(marchBuildingTitle(buildingId))),
            AshPalette.flameLight, 8)
        val chosen = state.specializations[buildingId]
        if (chosen != null) {
            Body(stringResource(if (chosen == "industry") Res.string.march_specialized_industry
                else Res.string.march_specialized_utility), AshPalette.teal)
        } else {
            Body(stringResource(Res.string.march_specialize_need))
            Body(stringResource(Res.string.march_industry), AshPalette.bone)
            AshButton(stringResource(Res.string.march_choose_path), canSpecialize(state, buildingId),
                modifier = Modifier.fillMaxWidth(), color = AshPalette.flame,
                onClick = { RebootGraph.engine.specialize(buildingId, "industry") })
            Body(stringResource(marchUtility(buildingId)), AshPalette.bone)
            AshButton(stringResource(Res.string.march_choose_path), canSpecialize(state, buildingId),
                modifier = Modifier.fillMaxWidth(), color = AshPalette.teal,
                onClick = { RebootGraph.engine.specialize(buildingId, "utility") })
        }
    }
}

@Composable
private fun RelicSetPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.march_sets), AshPalette.flameLight, 9)
        if (state.reign < 2) {
            Body(stringResource(Res.string.march_sets_locked))
            return@Column
        }
        if (state.relicSetCooldownSeconds > 0) {
            Body(stringResource(Res.string.march_sets_cooldown, state.relicSetCooldownSeconds))
        }
        listOf("emberguard", "wayfarer", "nightveil").forEach { id ->
            val title = when (id) {
                "emberguard" -> Res.string.march_set_emberguard
                "wayfarer" -> Res.string.march_set_wayfarer
                else -> Res.string.march_set_nightveil
            }
            val shortTitle = when (id) {
                "emberguard" -> Res.string.march_set_emberguard_short
                "wayfarer" -> Res.string.march_set_wayfarer_short
                else -> Res.string.march_set_nightveil_short
            }
            Body(stringResource(title), AshPalette.bone)
            AshButton(
                label = if (state.relicSetId == id) stringResource(Res.string.march_set_active)
                else stringResource(shortTitle),
                enabled = state.relicSetId != id && state.relicSetCooldownSeconds == 0,
                color = if (state.relicSetId == id) AshPalette.teal else AshPalette.panelRaised,
                modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.setRelicSet(id) },
            )
        }
    }
}

internal fun marchTitle(id: String): StringResource = when (id) {
    "forest" -> Res.string.march_forest
    "fen" -> Res.string.march_fen
    "coast" -> Res.string.march_coast
    else -> Res.string.march_ruins
}

private fun marchDescription(id: String): StringResource = when (id) {
    "forest" -> Res.string.march_forest_desc
    "fen" -> Res.string.march_fen_desc
    "coast" -> Res.string.march_coast_desc
    else -> Res.string.march_ruins_desc
}

private fun marchBuildingTitle(id: String): StringResource = when (id) {
    "belltower" -> Res.string.reboot_belltower
    "moonforge" -> Res.string.reboot_moonforge
    "soulharbor" -> Res.string.reboot_soulharbor
    "coalpit" -> Res.string.reboot_coalpit
    "bonelibrary" -> Res.string.reboot_bonelibrary
    else -> Res.string.reboot_citadel
}

private fun marchUtility(id: String): StringResource = when (id) {
    "coalpit" -> Res.string.march_utility_coalpit
    "moonforge" -> Res.string.march_utility_moonforge
    "bonelibrary" -> Res.string.march_utility_bonelibrary
    else -> Res.string.march_utility_citadel
}
