package com.embercrown.game.reboot

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.embercrown.game.audio.SfxId
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class ThronePage(val label: StringResource, val discovery: String? = null) {
    KINGDOM(Res.string.reboot_tab_kingdom),
    BUILDINGS(Res.string.reboot_tab_buildings, "buildings"),
    MARCHES(Res.string.march_tab, "marches"),
    REALM(Res.string.reboot_left_realm, "realm"),
    POWER(Res.string.reboot_left_power, "power"),
    CROWN(Res.string.reboot_left_crown, "crown"),
    CHRONICLE(Res.string.reboot_tab_chronicle, "chronicle"),
    SYSTEM(Res.string.reboot_tab_settings),
}

internal fun visibleThronePages(state: RebootState): List<ThronePage> {
    val known = discoveredSystems(state)
    return ThronePage.entries.filter { it.discovery == null || it.discovery in known }
}

internal fun nextThroneIntroduction(state: RebootState): String? {
    if (state.tutorialStep < TUTORIAL_DONE || !state.tutorialAcknowledged) return null
    val known = discoveredSystems(state)
    val seen = state.introductionsSeen ?: known
    return listOf("buildings", "chronicle", "realm", "beacon", "marches", "power", "crown")
        .firstOrNull { it in known && it !in seen }
}

private fun discoveryHint(id: String): StringResource = when (id) {
    "buildings" -> Res.string.throne_reveal_buildings
    "realm" -> Res.string.throne_reveal_realm
    "marches" -> Res.string.throne_reveal_marches
    "power" -> Res.string.throne_reveal_power
    "crown" -> Res.string.throne_reveal_crown
    "chronicle" -> Res.string.throne_reveal_chronicle
    else -> Res.string.throne_reveal_beacon
}

/** Only reached buildings and the next silhouette are shown; the full catalogue unfolds with play. */
internal fun visibleThroneBuildings(state: RebootState): List<RebootBuilding> {
    if (state.tutorialStep in 1..4) return listOf(RebootBuildings.byId("coalpit"))
    val firstLocked = RebootBuildings.all.firstOrNull {
        state.lifetimeEmbers < it.unlockAt && state.level(it.id) == 0
    }
    return RebootBuildings.all.filter {
        state.level(it.id) > 0 || state.lifetimeEmbers >= it.unlockAt || it == firstLocked
    }
}

@Composable
internal fun ThroneRoom(state: RebootState, compact: Boolean, buildMode: BuildMode,
                        onBuildMode: (BuildMode) -> Unit, onOmen: () -> Unit,
                        onRitual: () -> Unit, onReset: () -> Unit) {
    var page by remember { mutableStateOf(ThronePage.KINGDOM) }
    var menuOpen by remember { mutableStateOf(false) }
    val pages = visibleThronePages(state)
    val introduction = nextThroneIntroduction(state)
    LaunchedEffect(state.reign) { page = ThronePage.KINGDOM; menuOpen = false }
    LaunchedEffect(state.tutorialStep) {
        when (state.tutorialStep) {
            1, 2 -> page = ThronePage.KINGDOM
            3, 4 -> page = ThronePage.BUILDINGS
            TUTORIAL_DONE -> if (page == ThronePage.BUILDINGS) page = ThronePage.KINGDOM
        }
    }
    LaunchedEffect(pages) { if (page !in pages) page = ThronePage.KINGDOM }
    LaunchedEffect(introduction, compact) {
        if (introduction != null && (compact || introduction in listOf("power", "chronicle"))) {
            menuOpen = true
        }
    }
    val navigate: (ThronePage) -> Unit = { target ->
        if (target in pages) {
            if (introduction != null && target.discovery == introduction)
                RebootGraph.engine.acknowledgeDiscovery(introduction)
            if (page != target) RebootGraph.audio.play(SfxId.ASH_PAGE)
            page = target
            menuOpen = false
        }
    }
    Column(Modifier.fillMaxSize().background(AshPalette.night)) {
        ThroneHeader(state, compact)
        if (!compact) ThroneNavigation(page, pages, compact = false, menuOpen = menuOpen,
            attention = introduction, onSelect = navigate, onMenu = { menuOpen = !menuOpen })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = page, animationSpec = tween(220), modifier = Modifier.fillMaxSize()) { target ->
                when (target) {
                    ThronePage.KINGDOM -> ThroneKingdom(state, compact, navigate, onOmen, onRitual)
                    ThronePage.BUILDINGS -> ThroneBuildings(state, buildMode, onBuildMode, compact)
                    ThronePage.MARCHES -> MarchesPanel(state, Modifier.fillMaxSize())
                    ThronePage.REALM -> ThroneScroll {
                        Label(stringResource(Res.string.reboot_left_realm), AshPalette.flameLight, 11)
                        OmenBanner(state, onOmen)
                        RoyalOrdersPanel(state)
                        CrownDistrictsPanel(state)
                        EdictPanel(state)
                    }
                    ThronePage.POWER -> ThroneScroll {
                        Label(stringResource(Res.string.reboot_left_power), AshPalette.flameLight, 11)
                        CrownArtifactsPanel(state)
                        CrownTrialsPanel(state)
                    }
                    ThronePage.CROWN -> ThroneScroll {
                        Label(stringResource(Res.string.reboot_left_crown), AshPalette.flameLight, 11)
                        BlackCourtPanel(state)
                        RitualPanel(state, onRitual)
                    }
                    ThronePage.CHRONICLE -> MobileChroniclePanel(state)
                    ThronePage.SYSTEM -> SystemPanel(onReset)
                }
            }
            if (introduction != null) {
                ThroneIntroductionCard(introduction,
                    onOpen = {
                        val target = ThronePage.entries.firstOrNull { it.discovery == introduction }
                        if (target == null) RebootGraph.engine.acknowledgeDiscovery(introduction)
                        navigate(target ?: ThronePage.KINGDOM)
                    },
                    onLater = {
                        RebootGraph.engine.acknowledgeDiscovery(introduction)
                        menuOpen = false
                    })
            }
        }
        if (compact) ThroneNavigation(page, pages, compact = true, menuOpen = menuOpen,
            attention = introduction, onSelect = navigate, onMenu = { menuOpen = !menuOpen })
    }
}

@Composable
private fun ThroneIntroductionCard(id: String, onOpen: () -> Unit, onLater: () -> Unit) {
    val unlocked = ThronePage.entries.firstOrNull { it.discovery == id }
    Box(Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.93f).widthIn(max = 520.dp)
            .background(AshPalette.panelRaised).border(2.dp, AshPalette.flame).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Label(stringResource(Res.string.throne_unlocked,
                unlocked?.let { stringResource(it.label) }
                    ?: stringResource(Res.string.throne_beacon)), AshPalette.flameLight, 10)
            Body(stringResource(discoveryHint(id)), AshPalette.bone)
            AshButton(stringResource(Res.string.throne_intro_open), true, pulse = true,
                modifier = Modifier.fillMaxWidth(), onClick = onOpen)
            AshButton(stringResource(Res.string.throne_intro_later), true,
                color = AshPalette.panel, modifier = Modifier.fillMaxWidth(), onClick = onLater)
        }
    }
}

@Composable
internal fun ThroneDiscoveryLibrary(state: RebootState) {
    val known = discoveredSystems(state)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(stringResource(Res.string.throne_guide_library), AshPalette.flameLight, 9)
        ThronePage.entries.filter { it.discovery in known }.forEach { page ->
            val id = page.discovery ?: return@forEach
            Label(stringResource(page.label), AshPalette.bone, 7)
            Body(stringResource(discoveryHint(id)), AshPalette.muted)
        }
        if ("beacon" in known) {
            Label(stringResource(Res.string.throne_beacon), AshPalette.bone, 7)
            Body(stringResource(discoveryHint("beacon")), AshPalette.muted)
        }
    }
}

@Composable
private fun ThroneHeader(state: RebootState, compact: Boolean) {
    val known = discoveredSystems(state)
    Column(Modifier.fillMaxWidth().background(AshPalette.panel)
        .border(1.dp, AshPalette.edge).padding(horizontal = if (compact) 10.dp else 18.dp,
            vertical = if (compact) 9.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("EMBERCROWN", AshPalette.flameLight, 10)
                Spacer(Modifier.weight(1f))
                Label(stringResource(Res.string.reboot_reign, state.reign), AshPalette.ash, 7)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ThroneStat(stringResource(Res.string.reboot_embers), formatAmount(state.embers),
                    AshPalette.flameLight, Modifier.weight(1f), true)
                ThroneStat(stringResource(Res.string.reboot_rate_short), formatAmount(production(state)),
                    AshPalette.teal, Modifier.weight(1f), true)
                if ("crown" in known || state.relics > 0) {
                    ThroneStat(stringResource(Res.string.reboot_relics), state.relics.toString(),
                        AshPalette.ash, Modifier.weight(0.7f), true)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Label("EMBERCROWN", AshPalette.flameLight, 14)
                    Label(stringResource(Res.string.reboot_subtitle), AshPalette.muted, 7)
                }
                Spacer(Modifier.weight(1f))
                ThroneStat(stringResource(Res.string.reboot_embers), formatAmount(state.embers),
                    AshPalette.flameLight, Modifier.width(210.dp), false)
                ThroneStat(stringResource(Res.string.reboot_rate), formatAmount(production(state)),
                    AshPalette.teal, Modifier.width(210.dp), false)
                if ("crown" in known || state.relics > 0) {
                    ThroneStat(stringResource(Res.string.reboot_relics), state.relics.toString(),
                        AshPalette.ash, Modifier.width(120.dp), false)
                }
                Label(stringResource(Res.string.reboot_reign, state.reign), AshPalette.ash, 7)
            }
        }
        if ("beacon" in known) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(Res.string.reboot_gloom), AshPalette.crimson, 7)
                GloomBar(state.gloom, Modifier.weight(1f))
                Label("${state.gloom.toInt()}%", AshPalette.ash, 7)
            }
        }
    }
}

@Composable
private fun ThroneStat(label: String, value: String, color: Color, modifier: Modifier, compact: Boolean) {
    Column(modifier.background(AshPalette.night).border(1.dp, AshPalette.edge)
        .padding(horizontal = if (compact) 7.dp else 12.dp, vertical = 7.dp)) {
        Label(label, AshPalette.muted, 7)
        Spacer(Modifier.height(4.dp))
        Label(value, color, if (compact) 9 else 12)
    }
}

@Composable
private fun ThroneNavigation(selected: ThronePage, pages: List<ThronePage>, compact: Boolean,
                             menuOpen: Boolean, attention: String?,
                             onSelect: (ThronePage) -> Unit, onMenu: () -> Unit) {
    val primary = if (compact) listOf(ThronePage.KINGDOM, ThronePage.BUILDINGS, ThronePage.MARCHES)
        else listOf(ThronePage.KINGDOM, ThronePage.BUILDINGS, ThronePage.MARCHES,
            ThronePage.REALM, ThronePage.CROWN)
    val main = primary.filter { it in pages }
    val secondary = pages.filter { it !in main }
    Column(Modifier.fillMaxWidth().background(AshPalette.panel).animateContentSize()
        .border(1.dp, AshPalette.edge).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (menuOpen) {
            secondary.chunked(if (compact) 3 else 5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { option ->
                        AshButton(stringResource(option.label), true,
                            color = if (selected == option || option.discovery == attention)
                                AshPalette.flame else AshPalette.panelRaised,
                            pulse = option.discovery == attention,
                            modifier = Modifier.weight(1f), onClick = { onSelect(option) })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            main.forEach { option ->
                AshButton(stringResource(option.label), true,
                    color = if (selected == option || option.discovery == attention)
                        AshPalette.flame else AshPalette.panelRaised,
                    pulse = option.discovery == attention,
                    modifier = if (compact) Modifier.weight(1f) else Modifier.width(150.dp),
                    onClick = { onSelect(option) })
            }
            AshButton(stringResource(Res.string.throne_more), true,
                color = if (menuOpen || selected in secondary || secondary.any { it.discovery == attention })
                    AshPalette.flame else AshPalette.panelRaised,
                pulse = secondary.any { it.discovery == attention },
                modifier = if (compact) Modifier.weight(1f) else Modifier.width(110.dp), onClick = onMenu)
        }
    }
}

@Composable
private fun ThroneKingdom(state: RebootState, compact: Boolean,
                          navigate: (ThronePage) -> Unit, onOmen: () -> Unit, onRitual: () -> Unit) {
    if (compact) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            KingdomScene(state, Modifier.fillMaxWidth().height(300.dp), highlight = state.tutorialStep == 1)
            ThroneNextAction(state, compact = true, navigate, onOmen, onRitual)
            SceneActionBar(state)
            TrophyShelf(state)
        }
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                KingdomScene(state, Modifier.weight(1f).fillMaxWidth(), highlight = state.tutorialStep == 1)
                SceneActionBar(state)
                TrophyShelf(state)
            }
            Column(Modifier.width(370.dp).fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(10.dp)) {
                ThroneNextAction(state, compact = false, navigate, onOmen, onRitual)
            }
        }
    }
}

@Composable
private fun ThroneNextAction(state: RebootState, compact: Boolean,
                             navigate: (ThronePage) -> Unit, onOmen: () -> Unit, onRitual: () -> Unit) {
    val known = discoveredSystems(state)
    val tutorial = state.tutorialStep in 1..4 || !state.tutorialAcknowledged
    Column(Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, AshPalette.flame)
        .padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(stringResource(Res.string.throne_next_step), AshPalette.flameLight, 9)
        if (tutorial) {
            TutorialGuide(state)
            when (state.tutorialStep) {
                1 -> AshButton(stringResource(Res.string.reboot_gather), true,
                    modifier = Modifier.fillMaxWidth(), onClick = RebootGraph.engine::gather)
                2 -> AshButton(stringResource(Res.string.throne_build_first),
                    state.embers >= buildingCost(RebootBuildings.byId("coalpit"), 0, state),
                    modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.build("coalpit") })
                3, 4 -> AshButton(stringResource(Res.string.throne_open_buildings), true,
                    modifier = Modifier.fillMaxWidth(), onClick = { navigate(ThronePage.BUILDINGS) })
            }
        } else {
            GoalText(state)
            when {
                canRitual(state) -> AshButton(stringResource(Res.string.reboot_tab_ritual), true,
                    color = AshPalette.crimson, pulse = true, modifier = Modifier.fillMaxWidth(), onClick = onRitual)
                state.level("coalpit") == 0 -> AshButton(stringResource(Res.string.throne_build_first),
                    state.embers >= buildingCost(RebootBuildings.byId("coalpit"), 0, state),
                    modifier = Modifier.fillMaxWidth(), onClick = { RebootGraph.engine.build("coalpit") })
                else -> {
                    val next = nextMilestone(state)
                    if (next != null) {
                        val progress = (state.lifetimeEmbers / next.target).toFloat().coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(8.dp).background(AshPalette.panelRaised)) {
                            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(AshPalette.flame))
                        }
                        Body("${formatAmount(state.lifetimeEmbers)} / ${formatAmount(next.target)}", AshPalette.teal)
                        Body(stringResource(Res.string.throne_seal_hint), AshPalette.muted)
                        if (progress >= 1f) {
                            AshButton(stringResource(Res.string.reboot_claim_seal, next.relicReward), true,
                                pulse = true, modifier = Modifier.fillMaxWidth(),
                                onClick = { RebootGraph.engine.claimMilestone(next.id) })
                        }
                    }
                    AshButton(stringResource(Res.string.throne_open_buildings), true,
                        modifier = Modifier.fillMaxWidth(), onClick = { navigate(ThronePage.BUILDINGS) })
                }
            }
        }
        if ("beacon" in known && !tutorial) BeaconQuickAction(state, compact)
        if (state.pendingOmenId != null) OmenBanner(state, onOmen)
        ContextualGuide(state, onRitual)
    }
}

@Composable
private fun ThroneBuildings(state: RebootState, buildMode: BuildMode,
                            onBuildMode: (BuildMode) -> Unit, compact: Boolean) {
    val buildings = visibleThroneBuildings(state)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item(key = "title") { Label(stringResource(Res.string.reboot_tab_buildings), AshPalette.flameLight, 11) }
        item(key = "guide") { if (state.tutorialStep in 3..4) TutorialGuide(state) }
        item(key = "mode") { BuildModeSelector(buildMode, onBuildMode, guided = state.tutorialStep in 2..4) }
        items(buildings, key = { it.id }) { building -> StructureCard(state, building, buildMode, compact) }
    }
}

@Composable
private fun ThroneScroll(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
