package com.embercrown.game.reboot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OmenBanner(state: RebootState, onOpen: () -> Unit) {
    val omen = AshOmens.byId(state.pendingOmenId) ?: return
    val title = omenTitle(omen.id)
    Column(
        modifier = Modifier.fillMaxWidth().background(AshPalette.violet)
            .border(1.dp, AshPalette.flameLight).clickable(onClick = onOpen).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Label(stringResource(Res.string.reboot_omen_waiting), AshPalette.flameLight, 8)
        Body(stringResource(title), AshPalette.bone)
        Body(stringResource(Res.string.reboot_omen_open), AshPalette.ash)
    }
}

@Composable
internal fun EdictPanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.reboot_edicts), AshPalette.flameLight, 9)
        if (state.edictCooldownSeconds > 0) {
            Body(stringResource(Res.string.reboot_edict_cooldown, state.edictCooldownSeconds), AshPalette.muted)
        }
        listOf("balance", "harvest", "ward", "rally").forEach { id ->
            val title = when (id) {
                "balance" -> Res.string.reboot_edict_balance
                "harvest" -> Res.string.reboot_edict_harvest
                "ward" -> Res.string.reboot_edict_ward
                else -> Res.string.reboot_edict_rally
            }
            val description = when (id) {
                "balance" -> Res.string.reboot_edict_balance_desc
                "harvest" -> Res.string.reboot_edict_harvest_desc
                "ward" -> Res.string.reboot_edict_ward_desc
                else -> Res.string.reboot_edict_rally_desc
            }
            val active = state.edictId == id
            Column(
                modifier = Modifier.fillMaxWidth().background(AshPalette.panel)
                    .border(1.dp, if (active) AshPalette.flame else AshPalette.edge).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Label(stringResource(title), if (active) AshPalette.flameLight else AshPalette.bone, 8)
                Body(stringResource(description), AshPalette.muted)
                AshButton(
                    label = if (active) stringResource(Res.string.reboot_edict_active)
                    else stringResource(Res.string.reboot_edict_enact),
                    enabled = !active && state.edictCooldownSeconds == 0,
                    color = AshPalette.flame,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { RebootGraph.engine.setEdict(id) },
                )
            }
        }
    }
}

@Composable
internal fun OmenDialog(state: RebootState, onLater: () -> Unit, onChoose: (Int) -> Unit) {
    val omen = AshOmens.byId(state.pendingOmenId) ?: return
    val title = omenTitle(omen.id)
    val flavor = when (omen.id) {
        "caravan" -> Res.string.reboot_omen_caravan_story
        "bell" -> Res.string.reboot_omen_bell_story
        else -> Res.string.reboot_omen_pyre_story
    }
    val second = when (omen.id) {
        "caravan" -> Res.string.reboot_omen_caravan_second
        "bell" -> Res.string.reboot_omen_bell_second
        else -> Res.string.reboot_omen_pyre_second
    }
    val secondEffect = when (omen.id) {
        "caravan" -> Res.string.reboot_omen_caravan_effect
        "bell" -> Res.string.reboot_omen_bell_effect
        else -> Res.string.reboot_omen_pyre_effect
    }
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.94f)).clickable { onLater() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 370.dp).fillMaxWidth(0.92f).border(2.dp, AshPalette.flame)
                .background(AshPalette.panel).clickable { }.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Label(stringResource(Res.string.reboot_omen_waiting), AshPalette.flameLight, 9)
            Text(stringResource(title), color = AshPalette.bone)
            Body(stringResource(flavor), AshPalette.ash)
            Rule()
            Body(stringResource(Res.string.reboot_omen_first), AshPalette.bone)
            Body(stringResource(Res.string.reboot_omen_first_effect, formatAmount(omenWindfall(state))), AshPalette.teal)
            AshButton(
                label = stringResource(Res.string.reboot_omen_take), enabled = true,
                color = AshPalette.teal, modifier = Modifier.fillMaxWidth(), onClick = { onChoose(0) },
            )
            Rule()
            Body(stringResource(second), AshPalette.bone)
            Body(stringResource(secondEffect), AshPalette.flameLight)
            AshButton(
                label = stringResource(Res.string.reboot_omen_accept), enabled = true,
                modifier = Modifier.fillMaxWidth(), onClick = { onChoose(1) },
            )
            AshButton(
                label = stringResource(Res.string.reboot_omen_later), enabled = true,
                color = AshPalette.panelRaised, modifier = Modifier.fillMaxWidth(), onClick = onLater,
            )
        }
    }
}

@Composable
internal fun OfflineReport(state: RebootState, onContinue: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.94f)).clickable(onClick = onContinue),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 390.dp).fillMaxWidth(0.9f).border(2.dp, AshPalette.teal)
                .background(AshPalette.panel).clickable { }.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Label(stringResource(Res.string.reboot_offline_title), AshPalette.flameLight, 10)
            Body(stringResource(Res.string.reboot_offline_time, formatPlaytime(state.offlineSeconds.toDouble())), AshPalette.bone)
            Label(stringResource(Res.string.reboot_offline_earned, formatAmount(state.offlineEmbers)), AshPalette.teal, 10)
            Body(stringResource(Res.string.reboot_offline_gloom, state.offlineGloom.toInt()), AshPalette.ash)
            if (state.offlineSeconds >= 8 * 60 * 60) {
                Body(stringResource(Res.string.reboot_offline_cap), AshPalette.muted)
            }
            AshButton(
                label = stringResource(Res.string.reboot_offline_continue), enabled = true,
                modifier = Modifier.fillMaxWidth(), onClick = onContinue,
            )
        }
    }
}

@Composable
internal fun ChroniclePanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Label(stringResource(Res.string.reboot_chronicle), AshPalette.flameLight, 11)
        Body(stringResource(Res.string.reboot_chronicle_hint, state.chronicleEntries.size / 3), AshPalette.bone)
        StatCard(state)
        ProductionLedger(state)
        Label(stringResource(Res.string.reboot_chronicle_entries, state.chronicleEntries.size, CrownChronicle.all.size), AshPalette.flameLight, 9)
        CrownChronicle.all.forEach { entry ->
            val earned = entry.id in state.chronicleEntries
            val (title, hint) = chronicleText(entry.id)
            Column(
                modifier = Modifier.fillMaxWidth().background(if (earned) AshPalette.panelRaised else AshPalette.panel)
                    .border(1.dp, if (earned) AshPalette.flame else AshPalette.edge).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Label(stringResource(title), if (earned) AshPalette.flameLight else AshPalette.muted, 8)
                Body(stringResource(hint), if (earned) AshPalette.bone else AshPalette.muted)
            }
        }
    }
}

@Composable
private fun StatCard(state: RebootState) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, AshPalette.edge).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Label(stringResource(Res.string.reboot_stats), AshPalette.teal, 9)
        StatLine(stringResource(Res.string.reboot_stat_run), formatPlaytime(state.runSeconds))
        StatLine(stringResource(Res.string.reboot_stat_total), formatPlaytime(state.playedSeconds))
        StatLine(stringResource(Res.string.reboot_stat_taps), state.totalTaps.toString())
        StatLine(stringResource(Res.string.reboot_stat_omens), state.omensResolved.toString())
        StatLine(stringResource(Res.string.reboot_stat_beacons), state.beaconsLit.toString())
        StatLine(stringResource(Res.string.reboot_stat_buildings), state.levels.values.sum().toString())
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Body(label, AshPalette.muted, Modifier.weight(1f))
        Body(value, AshPalette.bone)
    }
}

@Composable
private fun ProductionLedger(state: RebootState) {
    val owned = RebootBuildings.all.filter { state.level(it.id) > 0 }
    val total = rawProduction(state)
    Column(
        modifier = Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, AshPalette.edge).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(stringResource(Res.string.reboot_ledger), AshPalette.teal, 9)
        Body(stringResource(Res.string.reboot_ledger_total, formatAmount(production(state))), AshPalette.bone)
        if (owned.isEmpty()) Body(stringResource(Res.string.reboot_ledger_empty))
        owned.sortedByDescending { buildingProduction(it, state) }.forEach { building ->
            val output = buildingProduction(building, state)
            val fraction = if (total > 0.0) (output / total).toFloat().coerceIn(0f, 1f) else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(stringResource(buildingTitle(building.id)), AshPalette.ash, Modifier.weight(1f))
                Body(formatAmount(output), AshPalette.teal)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).background(AshPalette.panelRaised)) {
                Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(AshPalette.teal))
            }
        }
    }
}

@Composable
internal fun ChronicleToast(earned: Set<String>, hold: Boolean, modifier: Modifier = Modifier) {
    var seen by remember { mutableStateOf(earned) }
    val queue = remember { mutableStateListOf<String>() }
    var showing by remember { mutableStateOf<String?>(null) }
    val currentlyHeld by rememberUpdatedState(hold)
    LaunchedEffect(earned) {
        val fresh = CrownChronicle.all.map { it.id }.filter { it in earned && it !in seen }
        seen = earned
        queue.addAll(fresh.take(3))
    }
    LaunchedEffect(Unit) {
        while (true) {
            if (queue.isEmpty() || currentlyHeld) {
                delay(150)
            } else {
                showing = queue.removeAt(0)
                delay(2_600)
                showing = null
            }
        }
    }
    AnimatedVisibility(
        visible = showing != null && !hold,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val id = showing ?: return@AnimatedVisibility
        val (title, _) = chronicleText(id)
        Column(
            modifier = Modifier.width(280.dp).background(AshPalette.panel)
                .border(2.dp, AshPalette.flame).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label(stringResource(Res.string.reboot_chronicle_unlocked), AshPalette.flameLight, 8)
            Body(stringResource(title), AshPalette.bone)
        }
    }
}

private fun formatPlaytime(seconds: Double): String {
    val minutes = seconds.toLong() / 60
    return "${minutes / 60}h ${minutes % 60}m"
}

private fun omenTitle(id: String): StringResource = when (id) {
    "caravan" -> Res.string.reboot_omen_caravan
    "bell" -> Res.string.reboot_omen_bell
    else -> Res.string.reboot_omen_pyre
}

private fun buildingTitle(id: String): StringResource = when (id) {
    "coalpit" -> Res.string.reboot_coalpit
    "hollowmill" -> Res.string.reboot_hollowmill
    "belltower" -> Res.string.reboot_belltower
    "moonforge" -> Res.string.reboot_moonforge
    "bonelibrary" -> Res.string.reboot_bonelibrary
    "citadel" -> Res.string.reboot_citadel
    "emberwell" -> Res.string.reboot_emberwell
    "gravegarden" -> Res.string.reboot_gravegarden
    "soulharbor" -> Res.string.reboot_soulharbor
    "stormspire" -> Res.string.reboot_stormspire
    "wyrmroost" -> Res.string.reboot_wyrmroost
    else -> Res.string.reboot_eclipsethrone
}

private fun chronicleText(id: String): Pair<StringResource, StringResource> = when (id) {
    "kindled" -> Res.string.reboot_chron_kindled to Res.string.reboot_chron_kindled_hint
    "hundred_taps" -> Res.string.reboot_chron_taps to Res.string.reboot_chron_taps_hint
    "first_mastery" -> Res.string.reboot_chron_mastery to Res.string.reboot_chron_mastery_hint
    "bells" -> Res.string.reboot_chron_bells to Res.string.reboot_chron_bells_hint
    "beacon" -> Res.string.reboot_chron_beacon to Res.string.reboot_chron_beacon_hint
    "citadel" -> Res.string.reboot_chron_citadel to Res.string.reboot_chron_citadel_hint
    "three_seals" -> Res.string.reboot_chron_seals to Res.string.reboot_chron_seals_hint
    "omen_one" -> Res.string.reboot_chron_omen to Res.string.reboot_chron_omen_hint
    "omen_five" -> Res.string.reboot_chron_omens to Res.string.reboot_chron_omens_hint
    "well" -> Res.string.reboot_chron_well to Res.string.reboot_chron_well_hint
    "storm" -> Res.string.reboot_chron_storm to Res.string.reboot_chron_storm_hint
    "wyrm" -> Res.string.reboot_chron_wyrm to Res.string.reboot_chron_wyrm_hint
    "crown" -> Res.string.reboot_chron_crown to Res.string.reboot_chron_crown_hint
    "march_scout" -> Res.string.march_chron_scout to Res.string.march_chron_scout_hint
    "march_conquer" -> Res.string.march_chron_conquer to Res.string.march_chron_conquer_hint
    "march_specialist" -> Res.string.march_chron_specialist to Res.string.march_chron_specialist_hint
    "march_all" -> Res.string.march_chron_all to Res.string.march_chron_all_hint
    else -> Res.string.reboot_chron_ritual to Res.string.reboot_chron_ritual_hint
}
