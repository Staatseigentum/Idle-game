package com.embercrown.game.reboot

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.audio.SfxId
import com.embercrown.game.i18n.LocalAppLocale
import com.embercrown.game.resources.*
import com.embercrown.game.ui.formatAmount
import com.embercrown.game.ui.pixelFontFamily
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelFit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Page(val label: StringResource) {
    KINGDOM(Res.string.reboot_tab_kingdom),
    MARCHES(Res.string.march_tab),
    BUILDINGS(Res.string.reboot_tab_buildings),
    RITUAL(Res.string.reboot_tab_ritual),
    CHRONICLE(Res.string.reboot_tab_chronicle),
    SYSTEM(Res.string.reboot_tab_settings),
}

internal enum class BuildMode { ONE, TEN, MAX }
private enum class DesktopShelf { BUILDINGS, CHRONICLE, SYSTEM }
private enum class LeftTab(val label: StringResource) {
    GOALS(Res.string.reboot_left_goals),
    REALM(Res.string.reboot_left_realm),
    POWER(Res.string.reboot_left_power),
    CROWN(Res.string.reboot_left_crown),
}

@Composable
fun RebootApp() {
    val state by RebootGraph.engine.state.collectAsState()
    val uiSettings by RebootGraph.uiSettings.state.collectAsState()
    val audio = remember { RebootGraph.audio }
    LaunchedEffect(uiSettings) { audio.applySettings(uiSettings) }
    LaunchedEffect(audio) {
        audio.applySettings(RebootGraph.uiSettings.state.value)
        audio.start()
        try {
            RebootGraph.engine.soundEvents.collect { audio.play(it) }
        } finally {
            audio.pause()
        }
    }
    var confirmRitual by remember { mutableStateOf(false) }
    var ritualRunning by remember { mutableStateOf(false) }
    var ritualYield by remember { mutableIntStateOf(0) }
    var ritualBefore by remember { mutableStateOf<RebootState?>(null) }
    var buildMode by remember { mutableStateOf(BuildMode.ONE) }
    var showOmen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(state.tutorialStep) {
        if (state.tutorialStep in 2..4) buildMode = BuildMode.ONE
    }
    LaunchedEffect(state.pendingOmenId) {
        if (state.pendingOmenId != null) showOmen = true
    }
    CompositionLocalProvider(LocalAppLocale provides uiSettings.language) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AshPalette.void)
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (state.prestigePending && !ritualRunning) {
                    PrestigeScreen(state, onContinue = RebootGraph.engine::beginNextReign)
                } else {
                    ThroneRoom(state = state, compact = maxWidth < 1020.dp,
                        buildMode = buildMode, onBuildMode = { buildMode = it },
                        onOmen = { showOmen = true }, onRitual = { confirmRitual = true },
                        onReset = { confirmReset = true })
                }
            }
            if (!state.prestigePending && !ritualRunning && state.tutorialStep != 0) {
                GloomAtmosphere(state.gloom)
            }
            if (confirmRitual) RitualConfirmation(
                reward = ritualReward(state),
                onCancel = { confirmRitual = false },
                onConfirm = {
                    confirmRitual = false
                    ritualYield = ritualReward(state)
                    ritualBefore = state
                    RebootGraph.audio.play(SfxId.ASH_RITUAL_CHARGE)
                    ritualRunning = true
                },
            )
            if (ritualRunning) RitualTransition(
                before = ritualBefore ?: state,
                reward = ritualYield,
                onBurn = { RebootGraph.audio.play(SfxId.ASH_RITUAL_FIRE) },
                onMidpoint = RebootGraph.engine::performRitual,
                onRebirth = { RebootGraph.audio.play(SfxId.ASH_RITUAL_REBIRTH) },
                onFinished = { ritualRunning = false; ritualBefore = null },
            )
            if (showOmen && state.pendingOmenId != null && !ritualRunning) OmenDialog(
                state = state,
                onLater = { showOmen = false },
                onChoose = { option ->
                    RebootGraph.engine.chooseOmen(option)
                    showOmen = false
                },
            )
            if (state.offlineSeconds >= 60) OfflineReport(
                state = state,
                onContinue = RebootGraph.engine::dismissOfflineReport,
            )
            ChronicleToast(
                earned = state.chronicleEntries,
                hold = ritualRunning || state.prestigePending || showOmen || state.offlineSeconds >= 60 || state.tutorialStep < TUTORIAL_DONE,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )
            if (state.tutorialStep == 0) TutorialWelcome()
            if (confirmReset) ResetConfirmation(
                onCancel = { confirmReset = false },
                onConfirm = {
                    RebootGraph.engine.resetForTutorial()
                    confirmReset = false
                    confirmRitual = false
                    ritualRunning = false
                    ritualBefore = null
                    showOmen = false
                    buildMode = BuildMode.ONE
                },
            )
        }
    }
}

@Composable
private fun DesktopKingdom(state: RebootState, buildMode: BuildMode, onBuildMode: (BuildMode) -> Unit,
                           onOmen: () -> Unit, onRitual: () -> Unit, onReset: () -> Unit) {
    var shelf by remember { mutableStateOf(DesktopShelf.BUILDINGS) }
    var leftTab by remember { mutableStateOf(LeftTab.GOALS) }
    var showMarches by remember { mutableStateOf(false) }
    val leftScroll = rememberScrollState()
    val buildingsScroll = rememberScrollState()
    val chronicleScroll = rememberScrollState()
    LaunchedEffect(state.reign) {
        showMarches = false
        shelf = DesktopShelf.BUILDINGS
        leftTab = LeftTab.GOALS
        leftScroll.scrollTo(0)
        buildingsScroll.scrollTo(0)
    }
    LaunchedEffect(state.tutorialStep) {
        if (state.tutorialStep in 1..4) {
            showMarches = false
            shelf = DesktopShelf.BUILDINGS
            leftTab = LeftTab.GOALS
            buildingsScroll.animateScrollTo(0)
        }
    }
    LaunchedEffect(leftTab) { leftScroll.scrollTo(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        KingdomHeader(state, compact = false, onOmen = onOmen)
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.width(286.dp).fillMaxHeight().background(AshPalette.night)
                    .border(width = 1.dp, color = AshPalette.edge.copy(alpha = 0.55f)),
            ) {
                LeftTabSelector(leftTab, canRitual(state)) { selected ->
                    if (selected != leftTab) RebootGraph.audio.play(SfxId.ASH_PAGE)
                    leftTab = selected
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(leftScroll).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (leftTab) {
                        LeftTab.GOALS -> {
                            TutorialGuide(state)
                            ContextualGuide(state, onRitual = onRitual)
                            Label(stringResource(Res.string.reboot_goal), AshPalette.flame, 9)
                            GoalText(state)
                            MilestonePanel(state)
                            Rule()
                            RoyalOrdersPanel(state)
                        }
                        LeftTab.REALM -> {
                            OmenBanner(state, onOmen)
                            CrownDistrictsPanel(state)
                            Rule()
                            PatrolPanel(state)
                            Rule()
                            EdictPanel(state)
                        }
                        LeftTab.POWER -> {
                            CrownArtifactsPanel(state)
                            Rule()
                            CrownTrialsPanel(state)
                        }
                        LeftTab.CROWN -> {
                            BlackCourtPanel(state)
                            Rule()
                            RitualPanel(state, onRitual)
                            Rule()
                            Body(stringResource(Res.string.reboot_lore))
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(modifier = Modifier.fillMaxWidth().background(AshPalette.panel).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AshButton(stringResource(Res.string.march_kingdom), true,
                        color = if (!showMarches) AshPalette.flame else AshPalette.panelRaised,
                        modifier = Modifier.weight(1f), onClick = {
                            if (showMarches) RebootGraph.audio.play(SfxId.ASH_PAGE)
                            showMarches = false
                        })
                    AshButton(stringResource(Res.string.march_tab), true,
                        color = if (showMarches) AshPalette.flame else AshPalette.panelRaised,
                        modifier = Modifier.weight(1f), onClick = {
                            if (!showMarches) RebootGraph.audio.play(SfxId.ASH_PAGE)
                            showMarches = true
                        })
                }
                if (showMarches) {
                    MarchesPanel(state, modifier = Modifier.weight(1f).fillMaxWidth())
                } else {
                    KingdomScene(state, modifier = Modifier.weight(1f).fillMaxWidth(),
                        highlight = state.tutorialStep == 1)
                    SceneActionBar(state)
                    TrophyShelf(state)
                }
            }

            Column(
                modifier = Modifier.width(386.dp).fillMaxHeight().background(AshPalette.night)
                    .border(width = 1.dp, color = AshPalette.edge.copy(alpha = 0.55f)),
            ) {
                Row(modifier = Modifier.fillMaxWidth().background(AshPalette.panel).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AshButton(
                        label = stringResource(Res.string.reboot_tab_buildings), enabled = true,
                        color = if (shelf == DesktopShelf.BUILDINGS) AshPalette.flame else AshPalette.panelRaised,
                        modifier = Modifier.weight(1f), onClick = {
                            if (shelf != DesktopShelf.BUILDINGS) RebootGraph.audio.play(SfxId.ASH_PAGE)
                            shelf = DesktopShelf.BUILDINGS
                        },
                    )
                    AshButton(
                        label = stringResource(Res.string.reboot_tab_chronicle), enabled = true,
                        color = if (shelf == DesktopShelf.CHRONICLE) AshPalette.flame else AshPalette.panelRaised,
                        modifier = Modifier.weight(1f), onClick = {
                            if (shelf != DesktopShelf.CHRONICLE) RebootGraph.audio.play(SfxId.ASH_PAGE)
                            shelf = DesktopShelf.CHRONICLE
                        },
                    )
                    AshButton(
                        label = stringResource(Res.string.reboot_tab_settings), enabled = true,
                        color = if (shelf == DesktopShelf.SYSTEM) AshPalette.flame else AshPalette.panelRaised,
                        modifier = Modifier.weight(1f), onClick = {
                            if (shelf != DesktopShelf.SYSTEM) RebootGraph.audio.play(SfxId.ASH_PAGE)
                            shelf = DesktopShelf.SYSTEM
                        },
                    )
                }
                if (shelf == DesktopShelf.SYSTEM) {
                    SystemPanel(onReset)
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(if (shelf == DesktopShelf.CHRONICLE) chronicleScroll else buildingsScroll)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (shelf == DesktopShelf.CHRONICLE) ChroniclePanel(state) else {
                            BuildModeSelector(buildMode, onBuildMode, guided = state.tutorialStep in 2..4)
                            RebootBuildings.all.forEach { StructureCard(state, it, buildMode, compact = false) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftTabSelector(selected: LeftTab, ritualReady: Boolean, onSelect: (LeftTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AshPalette.panel).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LeftTab.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tab ->
                    AshButton(
                        label = stringResource(tab.label), enabled = true,
                        color = when {
                            selected == tab -> AshPalette.flame
                            tab == LeftTab.CROWN && ritualReady -> AshPalette.crimson
                            else -> AshPalette.panelRaised
                        },
                        pulse = tab == LeftTab.CROWN && ritualReady && selected != tab,
                        modifier = Modifier.weight(1f), onClick = { onSelect(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileKingdom(state: RebootState, buildMode: BuildMode, onBuildMode: (BuildMode) -> Unit,
                          onOmen: () -> Unit, onRitual: () -> Unit, onReset: () -> Unit) {
    var page by remember { mutableStateOf(Page.KINGDOM) }
    LaunchedEffect(state.reign) { page = Page.KINGDOM }
    LaunchedEffect(state.tutorialStep) {
        when (state.tutorialStep) {
            1 -> page = Page.KINGDOM
            in 2..4 -> page = Page.BUILDINGS
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        KingdomHeader(state, compact = true, onOmen = onOmen)
        TutorialGuide(state, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        ContextualGuide(state, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            onRitual = onRitual)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = page, modifier = Modifier.fillMaxSize(), animationSpec = tween(220)) { currentPage ->
            when (currentPage) {
                Page.KINGDOM -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    KingdomScene(state, modifier = Modifier.fillMaxWidth().height(320.dp),
                        highlight = state.tutorialStep == 1)
                    SceneActionBar(state)
                    TrophyShelf(state)
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Label(stringResource(Res.string.reboot_goal), AshPalette.flame, 9)
                        GoalText(state)
                        OmenBanner(state, onOmen)
                        MilestonePanel(state)
                        Rule()
                        RoyalOrdersPanel(state)
                        Rule()
                        CrownDistrictsPanel(state)
                        PatrolPanel(state)
                        EdictPanel(state)
                    }
                }
                Page.MARCHES -> MarchesPanel(state, modifier = Modifier.fillMaxSize())
                Page.BUILDINGS -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "title") {
                        Label(stringResource(Res.string.reboot_buildings), AshPalette.bone, 10)
                    }
                    item(key = "build-mode") {
                        BuildModeSelector(buildMode, onBuildMode, guided = state.tutorialStep in 2..4)
                    }
                    items(RebootBuildings.all, key = { it.id }) { building ->
                        StructureCard(state, building, buildMode, compact = true)
                    }
                }
                Page.RITUAL -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Label(stringResource(Res.string.reboot_ritual), AshPalette.crimson, 13)
                    Body(stringResource(Res.string.reboot_lore))
                    MilestonePanel(state)
                    Rule()
                    CrownArtifactsPanel(state)
                    Rule()
                    CrownTrialsPanel(state)
                    Rule()
                    BlackCourtPanel(state)
                    Rule()
                    RitualPanel(state, onRitual)
                }
                Page.CHRONICLE -> MobileChroniclePanel(state)
                Page.SYSTEM -> SystemPanel(onReset)
            }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(AshPalette.panel)
                .border(width = 1.dp, color = AshPalette.edge.copy(alpha = 0.65f)),
        ) {
            Page.entries.chunked(3).forEach { rowPages ->
                Row(Modifier.fillMaxWidth()) {
                    rowPages.forEach { option ->
                        Column(
                            modifier = Modifier.weight(1f)
                                .background(if (page == option) AshPalette.panelRaised else Color.Transparent)
                                .clickable {
                                    if (page != option) RebootGraph.audio.play(SfxId.ASH_PAGE)
                                    page = option
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.fillMaxWidth().height(3.dp)
                                .background(if (page == option) AshPalette.flame else AshPalette.panel))
                            Spacer(Modifier.height(10.dp))
                            Label(
                                stringResource(option.label),
                                if (page == option) AshPalette.flameLight else AshPalette.muted,
                                7,
                            )
                            Spacer(Modifier.height(11.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KingdomHeader(state: RebootState, compact: Boolean, onOmen: () -> Unit) {
    val visual = gloomVisuals(state.gloom)
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(lerp(AshPalette.night, Color(0xFF301B28), visual.pressure * 0.65f))
            .border(width = 1.dp, color = lerp(AshPalette.edge.copy(alpha = 0.55f),
                AshPalette.crimson, visual.pressure))
            .padding(horizontal = if (compact) 13.dp else 20.dp, vertical = if (compact) 11.dp else 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Label("EMBERCROWN", AshPalette.flameLight, if (compact) 13 else 16)
                Spacer(Modifier.height(5.dp))
                Label(stringResource(Res.string.reboot_subtitle), AshPalette.muted, 7)
            }
            if (state.pendingOmenId != null) {
                AshButton(label = stringResource(Res.string.reboot_omen_short), enabled = true,
                    color = AshPalette.violet, modifier = Modifier.width(75.dp), onClick = onOmen)
                Spacer(Modifier.width(8.dp))
            }
            Label(stringResource(Res.string.reboot_reign, state.reign), AshPalette.ash, if (compact) 7 else 9)
        }
        Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceBlock(stringResource(Res.string.reboot_embers), formatAmount(state.embers),
                AshPalette.flameLight, Modifier.weight(1f), compact)
            ResourceBlock(stringResource(if (compact) Res.string.reboot_rate_short else Res.string.reboot_rate),
                formatAmount(production(state)),
                AshPalette.teal, Modifier.weight(1f), compact)
            ResourceBlock(stringResource(Res.string.reboot_relics), state.relics.toString(),
                AshPalette.ash, Modifier.weight(0.75f), compact)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(Res.string.reboot_gloom),
                lerp(AshPalette.crimson, AshPalette.flameLight, visual.pressure), 7)
            Spacer(Modifier.width(10.dp))
            GloomBar(state.gloom, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Label("${state.gloom.toInt()}%",
                lerp(AshPalette.ash, AshPalette.flameLight, visual.pressure), 7)
        }
        if (visual.stage >= 2) {
            Spacer(Modifier.height(6.dp))
            val warning = when (visual.stage) {
                2 -> Res.string.reboot_gloom_veil
                3 -> Res.string.reboot_gloom_shadows
                4 -> Res.string.reboot_gloom_crown
                else -> Res.string.reboot_gloom_last_light
            }
            Label(stringResource(warning), AshPalette.flameLight, 7)
        }
        Spacer(Modifier.height(9.dp))
        BeaconQuickAction(state, compact)
        if (state.omenEffectSeconds > 0) {
            Spacer(Modifier.height(7.dp))
            Label(stringResource(Res.string.reboot_omen_active, state.omenEffectSeconds), AshPalette.teal, 7)
        }
    }
}

@Composable
private fun ResourceBlock(label: String, value: String, color: Color,
                          modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier = modifier.background(AshPalette.panel).border(1.dp, AshPalette.edge.copy(alpha = 0.65f))
        .padding(horizontal = if (compact) 7.dp else 12.dp, vertical = if (compact) 8.dp else 10.dp)) {
        Label(label, AshPalette.muted, 7)
        Spacer(Modifier.height(6.dp))
        Label(value, color, if (compact) 10 else 13)
    }
}

@Composable
internal fun GloomBar(gloom: Double, modifier: Modifier = Modifier) {
    val smooth by animateFloatAsState(gloom.toFloat(), animationSpec = tween(850))
    val pressure = gloomVisuals(gloom).pressure
    val filledColor = lerp(AshPalette.crimson, Color(0xFFFF644D), pressure)
    Row(modifier = modifier.height(9.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(20) { index ->
            val fill = (smooth / 5f - index).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(lerp(AshPalette.panelRaised, filledColor, fill)),
            )
        }
    }
}

@Composable
internal fun KingdomScene(state: RebootState, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val gloomBand = (state.gloom / 10).toInt()
    val flameLook = cosmeticStyle(state, "flame")
    val bannerLook = cosmeticStyle(state, "banner")
    val skyLook = cosmeticStyle(state, "sky")
    val art = remember(state.levels, state.buildingUpgrades, state.marchUpgradeRanks, state.conqueredRegions,
        state.specializations, state.districtLevels, state.eclipseSiegeStage, gloomBand,
        state.beaconSeconds > 0, flameLook, bannerLook, skyLook) {
        ashKingdomArt(state.levels, gloomBand * 10, state.beaconSeconds > 0,
            state.buildingUpgrades, state.conqueredRegions, state.specializations,
            state.districtLevels, state.eclipseSiegeStage == 3, flameLook, bannerLook, skyLook,
            state.marchUpgradeRanks)
    }
    var tapCount by remember { mutableIntStateOf(0) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }
    val yieldRise = remember { Animatable(1f) }
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            yieldRise.snapTo(0f)
            yieldRise.animateTo(1f, tween(750))
        }
    }
    Box(
        modifier = modifier.clipToBounds().background(AshPalette.void)
            .border(if (highlight) 3.dp else 0.dp,
                tutorialHighlightColor(highlight, AshPalette.edge))
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    tapPosition = position
                    RebootGraph.engine.gather()
                    tapCount++
                }
            },
    ) {
        PixelArtImage(art, modifier = Modifier.fillMaxSize(), fit = PixelFit.Cover)
        KingdomMotionLayer(state, gloomBand, flameLook, bannerLook, skyLook)
        TapSparks(tapCount, tapPosition, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                .background(AshPalette.void.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Label(stringResource(Res.string.reboot_reign, state.reign), AshPalette.bone, 8)
        }
        if (tapCount > 0 && yieldRise.value < 1f) {
            Box(
                modifier = Modifier.align(Alignment.Center)
                    .graphicsLayer {
                        translationY = -yieldRise.value * 52.dp.toPx()
                        alpha = (1f - yieldRise.value).coerceIn(0f, 1f)
                    }
                    .background(AshPalette.void.copy(alpha = 0.78f)).padding(12.dp),
            ) {
                Label("+${formatAmount(tapYield(state))}", AshPalette.flameLight, 14)
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(AshPalette.void.copy(alpha = 0.86f)).padding(15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Label(stringResource(Res.string.reboot_gather), AshPalette.flameLight, 10)
        }
    }
}

/** Keep the 8 fps art clock inside its own recomposition scope, away from tap and economy UI. */
@Composable
private fun KingdomMotionLayer(state: RebootState, gloomBand: Int, flameLook: String,
                               bannerLook: String, skyLook: String) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(125)
            frame = (frame + 1) % 96
        }
    }
    val motion = remember(state.levels, state.buildingUpgrades, state.marchUpgradeRanks, state.specializations,
        state.districtLevels, state.eclipseSiegeStage, gloomBand, state.beaconSeconds > 0,
        flameLook, bannerLook, skyLook, frame) {
        ashKingdomMotionArt(state.levels, gloomBand * 10, state.beaconSeconds > 0, frame,
            state.buildingUpgrades, state.specializations, state.districtLevels,
            state.eclipseSiegeStage == 3, flameLook, bannerLook, skyLook, state.marchUpgradeRanks)
    }
    PixelArtImage(motion, modifier = Modifier.fillMaxSize(), fit = PixelFit.Cover)
}

@Composable
internal fun SceneActionBar(state: RebootState) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AshPalette.panel).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Body(stringResource(Res.string.reboot_gather_hint), modifier = Modifier.weight(1f))
        Label("+${formatAmount(tapYield(state))}", AshPalette.flameLight, 10)
    }
}

@Composable
internal fun BuildModeSelector(selected: BuildMode, onSelect: (BuildMode) -> Unit, guided: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BuildMode.entries.forEach { mode ->
            AshButton(
                label = when (mode) {
                    BuildMode.ONE -> "x1"
                    BuildMode.TEN -> "x10"
                    BuildMode.MAX -> stringResource(Res.string.reboot_max)
                },
                enabled = !guided || mode == BuildMode.ONE,
                color = if (selected == mode) AshPalette.flame else AshPalette.panelRaised,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
internal fun GoalText(state: RebootState) {
    val next = nextMilestone(state)
    Body(
        when {
            canRitual(state) -> stringResource(Res.string.reboot_goal_ritual)
            state.level("coalpit") == 0 -> stringResource(Res.string.reboot_goal_first)
            next != null -> stringResource(Res.string.reboot_goal_milestone, formatAmount(next.target))
            else -> stringResource(Res.string.reboot_goal_throne)
        },
        color = AshPalette.bone,
    )
}

@Composable
internal fun MilestonePanel(state: RebootState) {
    val next = nextMilestone(state) ?: return
    val name = when (next.id) {
        "spark" -> Res.string.reboot_seal_spark
        "watch" -> Res.string.reboot_seal_watch
        "forge" -> Res.string.reboot_seal_forge
        "march" -> Res.string.reboot_seal_march
        "sky" -> Res.string.reboot_seal_sky
        else -> Res.string.reboot_seal_crown
    }
    val ready = state.lifetimeEmbers >= next.target
    val progress by animateFloatAsState((state.lifetimeEmbers / next.target).toFloat().coerceIn(0f, 1f), tween(450))
    Column(
        modifier = Modifier.fillMaxWidth().background(AshPalette.panel)
            .border(1.dp, if (ready) AshPalette.flame else AshPalette.edge).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(stringResource(Res.string.reboot_seal_progress, state.claimedMilestones.size, RebootMilestones.all.size), AshPalette.flameLight, 8)
        Body(stringResource(name), AshPalette.bone)
        Box(Modifier.fillMaxWidth().height(9.dp).background(AshPalette.panelRaised)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(AshPalette.flame))
        }
        Body("${formatAmount(state.lifetimeEmbers)} / ${formatAmount(next.target)}", AshPalette.teal)
        Body(stringResource(Res.string.reboot_seal_bonus), AshPalette.muted)
        AshButton(
            label = stringResource(Res.string.reboot_claim_seal, next.relicReward),
            enabled = ready,
            pulse = ready,
            modifier = Modifier.fillMaxWidth(),
            onClick = { RebootGraph.engine.claimMilestone(next.id) },
        )
    }
}

@Composable
internal fun RelicForgePanel(state: RebootState) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Label(stringResource(Res.string.reboot_relic_forge), AshPalette.flameLight, 9)
        Body(stringResource(if (state.reign == 2) Res.string.prestige_first_rank_hint
            else Res.string.reboot_relic_forge_hint), AshPalette.muted)
        RelicPowers.all.forEach { power ->
            val nameAndDesc = when (power.id) {
                "cinderheart" -> Res.string.reboot_power_cinderheart to Res.string.reboot_power_cinderheart_desc
                "foundation" -> Res.string.reboot_power_foundation to Res.string.reboot_power_foundation_desc
                "nightward" -> Res.string.reboot_power_nightward to Res.string.reboot_power_nightward_desc
                "sovereignhand" -> Res.string.reboot_power_sovereignhand to Res.string.reboot_power_sovereignhand_desc
                else -> Res.string.reboot_power_beaconkeeper to Res.string.reboot_power_beaconkeeper_desc
            }
            val rank = state.relicRank(power.id)
            val cost = relicPowerCost(power, state)
            val rankLimit = relicPowerRankLimit(power, state)
            Column(
                modifier = Modifier.fillMaxWidth().background(AshPalette.panel)
                    .border(1.dp, if (rank == power.maxRank) AshPalette.teal else AshPalette.edge)
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Label(stringResource(nameAndDesc.first), AshPalette.bone, 8)
                Body(stringResource(nameAndDesc.second), AshPalette.muted)
                Body(stringResource(Res.string.reboot_rank, rank, power.maxRank), AshPalette.teal)
                AshButton(
                    label = when {
                        rank == power.maxRank -> stringResource(Res.string.reboot_maxed)
                        rank >= rankLimit -> stringResource(Res.string.prestige_rank_locked)
                        else -> stringResource(Res.string.reboot_relic_cost, cost)
                    },
                    enabled = state.prestigePending && rank < rankLimit && state.relics >= cost,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { RebootGraph.engine.buyRelicPower(power.id) },
                )
            }
        }
    }
}

@Composable
internal fun StructureCard(state: RebootState, building: RebootBuilding, mode: BuildMode, compact: Boolean) {
    val name = when (building.id) {
        "coalpit" -> Res.string.reboot_coalpit to Res.string.reboot_coalpit_desc
        "emberorchard" -> Res.string.reboot_emberorchard to Res.string.reboot_emberorchard_desc
        "hollowmill" -> Res.string.reboot_hollowmill to Res.string.reboot_hollowmill_desc
        "lanternwatch" -> Res.string.reboot_lanternwatch to Res.string.reboot_lanternwatch_desc
        "belltower" -> Res.string.reboot_belltower to Res.string.reboot_belltower_desc
        "ashmarket" -> Res.string.reboot_ashmarket to Res.string.reboot_ashmarket_desc
        "moonforge" -> Res.string.reboot_moonforge to Res.string.reboot_moonforge_desc
        "scoutlodge" -> Res.string.reboot_scoutlodge to Res.string.reboot_scoutlodge_desc
        "bonelibrary" -> Res.string.reboot_bonelibrary to Res.string.reboot_bonelibrary_desc
        "citadel" -> Res.string.reboot_citadel to Res.string.reboot_citadel_desc
        "shadowfoundry" -> Res.string.reboot_shadowfoundry to Res.string.reboot_shadowfoundry_desc
        "emberwell" -> Res.string.reboot_emberwell to Res.string.reboot_emberwell_desc
        "gravegarden" -> Res.string.reboot_gravegarden to Res.string.reboot_gravegarden_desc
        "soulharbor" -> Res.string.reboot_soulharbor to Res.string.reboot_soulharbor_desc
        "stormspire" -> Res.string.reboot_stormspire to Res.string.reboot_stormspire_desc
        "courtobservatory" -> Res.string.reboot_courtobservatory to Res.string.reboot_courtobservatory_desc
        "wyrmroost" -> Res.string.reboot_wyrmroost to Res.string.reboot_wyrmroost_desc
        else -> Res.string.reboot_eclipsethrone to Res.string.reboot_eclipsethrone_desc
    }
    val level = state.level(building.id)
    val unlocked = state.lifetimeEmbers >= building.unlockAt
    val maxAffordable = if (unlocked) maxAffordableBuildings(building, state) else 0
    val amount = when (mode) {
        BuildMode.ONE -> 1
        BuildMode.TEN -> 10
        BuildMode.MAX -> maxAffordable.coerceAtLeast(1)
    }
    val cost = buildingBundleCost(building, state, amount)
    val affordable = maxAffordable >= amount
    val mastery = state.mastery(building.id)
    val tutorialTarget = building.id == "coalpit" && state.tutorialStep in 2..4
    val tutorialMastery = building.id == "coalpit" && state.tutorialStep == 4
    val purchaseFlash = remember(building.id) { Animatable(0f) }
    var previousLevel by remember(building.id) { mutableIntStateOf(level) }
    LaunchedEffect(level) {
        if (level > previousLevel) {
            purchaseFlash.snapTo(1f)
            purchaseFlash.animateTo(0f, tween(650))
        }
        previousLevel = level
    }
    Column(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = 1f + purchaseFlash.value * 0.025f
            scaleY = 1f + purchaseFlash.value * 0.025f
        }.background(lerp(AshPalette.panel, AshPalette.flame.copy(alpha = 0.28f), purchaseFlash.value))
            .border(if (tutorialTarget) 2.dp else 1.dp,
                tutorialHighlightColor(tutorialTarget,
                    if (affordable && unlocked) AshPalette.flame.copy(alpha = 0.65f) else AshPalette.edge))
            .padding(if (compact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (tutorialTarget) Label(stringResource(Res.string.tutorial_focus), AshPalette.flameLight, 7)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (compact) 50.dp else 60.dp).background(AshPalette.void)
            .border(1.dp, if (unlocked) AshPalette.edge else AshPalette.panelRaised),
            contentAlignment = Alignment.Center) {
            AnimatedBuildingIcon(building.id, unlocked)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(name.first), color = if (unlocked) AshPalette.bone else AshPalette.muted,
                fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(name.second), color = AshPalette.muted, fontSize = 10.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (unlocked) "${stringResource(Res.string.reboot_level, level)} · ${stringResource(Res.string.reboot_income, formatAmount(buildingProduction(building, state)))}"
                else stringResource(Res.string.reboot_locked, formatAmount(building.unlockAt)),
                color = if (unlocked) AshPalette.teal else AshPalette.muted, fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(7.dp))
        Column(horizontalAlignment = Alignment.End) {
          Label(if (unlocked) "x$amount" else "—", AshPalette.flameLight, 7)
          Spacer(Modifier.height(5.dp))
          AshButton(
            label = if (unlocked) formatAmount(cost) else "—",
            enabled = unlocked && affordable,
            modifier = Modifier.width(if (compact) 75.dp else 84.dp),
            onClick = { RebootGraph.engine.build(building.id, amount) },
          )
        }
      }
      if (unlocked && mastery < buildingMasteries.size) {
        Rule()
        val tier = buildingMasteries[mastery]
        val masteryProgress = (level.toFloat() / tier.minimumLevel).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(4.dp).background(AshPalette.void)) {
            Box(Modifier.fillMaxWidth(masteryProgress).fillMaxHeight().background(AshPalette.teal))
        }
        Row(modifier = Modifier.border(if (tutorialMastery) 2.dp else 0.dp,
                tutorialHighlightColor(tutorialMastery, AshPalette.edge))
            .padding(if (tutorialMastery) 5.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(Res.string.reboot_mastery, mastery + 1, formatAmount(tier.outputMultiplier)), AshPalette.teal, 7)
                Body(stringResource(Res.string.reboot_mastery_requirement, tier.minimumLevel), AshPalette.muted)
            }
            Spacer(Modifier.width(6.dp))
            AshButton(
                label = formatAmount(masteryCost(building, state)),
                enabled = canMaster(building, state),
                color = AshPalette.teal,
                modifier = Modifier.width(if (compact) 75.dp else 84.dp),
                onClick = { RebootGraph.engine.masterBuilding(building.id) },
            )
        }
      } else if (unlocked) {
        Label(stringResource(Res.string.reboot_mastered), AshPalette.teal, 7)
      }
      val marchRegionId = marchUpgradeRegions[building.id]
      if (unlocked && marchRegionId != null && marchRegionId in state.conqueredRegions) {
        Rule()
        val marchRank = state.marchUpgradeRanks[building.id] ?: 0
        Label(stringResource(Res.string.march_upgrade_title, marchRank, MAX_MARCH_UPGRADE_RANK,
            formatAmount(marchUpgradeMultiplier(state, building.id))), AshPalette.flameLight, 7)
        if (marchRank < MAX_MARCH_UPGRADE_RANK) {
            val fragmentCost = marchUpgradeFragmentCost(state, building.id)
            Body(stringResource(Res.string.march_upgrade_effect), AshPalette.muted)
            AshButton(
                label = stringResource(Res.string.march_upgrade_buy,
                    formatAmount(marchUpgradeCost(state, building)), fragmentCost),
                enabled = canBuyMarchUpgrade(state, building),
                color = AshPalette.flame,
                modifier = Modifier.fillMaxWidth(),
                onClick = { RebootGraph.engine.buyMarchUpgrade(building.id) },
            )
        } else {
            Body(stringResource(Res.string.march_upgrade_maxed), AshPalette.teal)
        }
      }
    }
}

/** Each visible icon ticks independently; price calculations in its card do not run per frame. */
@Composable
private fun AnimatedBuildingIcon(id: String, unlocked: Boolean) {
    var frame by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, unlocked) {
        if (unlocked) while (true) {
            delay(250)
            frame = (frame + 1) % 16
        }
    }
    val art = remember(id, frame) { ashBuildingIcon(id, frame) }
    PixelArtImage(art, modifier = Modifier.fillMaxSize().padding(3.dp))
}

@Composable
internal fun BeaconQuickAction(state: RebootState, compact: Boolean) {
    val cost = beaconCost(state)
    val ready = state.beaconSeconds == 0 && state.beaconCooldownSeconds == 0 && state.embers >= cost
    val status = when {
        state.beaconSeconds > 0 -> stringResource(Res.string.reboot_beacon_active, state.beaconSeconds)
        state.beaconCooldownSeconds > 0 -> stringResource(Res.string.reboot_beacon_cooldown, state.beaconCooldownSeconds)
        else -> stringResource(Res.string.reboot_beacon_ready)
    }
    val action = if (state.beaconSeconds > 0) stringResource(Res.string.reboot_beacon_burning)
        else stringResource(Res.string.reboot_beacon_cost, formatAmount(cost))
    val pulse = ready && state.gloom >= 18.0 && state.beaconsLit == 0 &&
        state.tutorialStep >= TUTORIAL_DONE
    val frame = Modifier.fillMaxWidth().background(AshPalette.panel)
        .border(1.dp, if (ready && state.gloom >= 55.0) AshPalette.flame else AshPalette.edge)
        .padding(7.dp)
    if (compact) {
        Column(frame, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Label(stringResource(Res.string.reboot_beacon_short), AshPalette.flameLight, 8)
            Body(status, if (state.beaconSeconds > 0) AshPalette.teal else AshPalette.muted)
            AshButton(action, ready, color = AshPalette.flame, pulse = pulse,
                modifier = Modifier.fillMaxWidth(), onClick = RebootGraph.engine::stokeBeacon)
        }
    } else {
        Row(frame, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(stringResource(Res.string.reboot_beacon_short), AshPalette.flameLight, 8)
                Body(status, if (state.beaconSeconds > 0) AshPalette.teal else AshPalette.muted)
            }
            AshButton(action, ready, color = AshPalette.flame, pulse = pulse,
                modifier = Modifier.width(246.dp), onClick = RebootGraph.engine::stokeBeacon)
        }
    }
}

@Composable
internal fun RitualPanel(state: RebootState, onRitual: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(stringResource(Res.string.reboot_ritual), AshPalette.crimson, 10)
        Body(
            if (canRitual(state)) stringResource(Res.string.reboot_ritual_ready, ritualReward(state))
            else stringResource(Res.string.reboot_ritual_locked),
            color = AshPalette.bone,
        )
        Body(stringResource(Res.string.reboot_relic_bonus))
        AshButton(
            label = stringResource(Res.string.reboot_ritual),
            enabled = canRitual(state),
            color = AshPalette.crimson,
            pulse = canRitual(state),
            modifier = Modifier.fillMaxWidth(),
            onClick = onRitual,
        )
    }
}

@Composable
internal fun SystemPanel(onReset: () -> Unit) {
    val uiSettings by RebootGraph.uiSettings.state.collectAsState()
    val state by RebootGraph.engine.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Label(stringResource(Res.string.reboot_tab_settings), AshPalette.bone, 12)
        Body(stringResource(Res.string.reboot_saved))
        BlueprintPanel(state)
        GuideLibrary(state)
        ThroneDiscoveryLibrary(state)
        Label(stringResource(Res.string.reboot_language), AshPalette.flameLight, 9)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(null to stringResource(Res.string.reboot_language_system), "de" to "DE", "en" to "EN").forEach { (code, name) ->
                AshButton(
                    label = name,
                    enabled = true,
                    color = if (uiSettings.language == code) AshPalette.flame else AshPalette.panelRaised,
                    modifier = Modifier.weight(1f),
                    onClick = { RebootGraph.uiSettings.setLanguage(code) },
                )
            }
        }
        Rule()
        Label(stringResource(Res.string.reboot_audio), AshPalette.flameLight, 9)
        AshButton(
            label = stringResource(Res.string.reboot_audio_mute) + " · " +
                stringResource(if (uiSettings.muted) Res.string.reboot_audio_on else Res.string.reboot_audio_off),
            enabled = true,
            color = if (uiSettings.muted) AshPalette.crimson else AshPalette.panelRaised,
            modifier = Modifier.fillMaxWidth(),
            onClick = { RebootGraph.uiSettings.setMuted(!uiSettings.muted) },
        )
        AudioLevelPicker(stringResource(Res.string.reboot_audio_music), uiSettings.musicVolume,
            RebootGraph.uiSettings::setMusicVolume)
        AudioLevelPicker(stringResource(Res.string.reboot_audio_effects), uiSettings.effectsVolume,
            RebootGraph.uiSettings::setEffectsVolume)
        AshButton(
            label = stringResource(Res.string.reboot_audio_test), enabled = true,
            color = AshPalette.teal, modifier = Modifier.fillMaxWidth(),
            onClick = { RebootGraph.audio.play(SfxId.ASH_RELIC) },
        )
        Rule()
        AshButton(
            label = stringResource(Res.string.reboot_save), enabled = true,
            modifier = Modifier.fillMaxWidth(), onClick = RebootGraph.engine::save,
        )
        Rule()
        AshButton(
            label = stringResource(Res.string.tutorial_reset), enabled = true,
            color = AshPalette.crimson, modifier = Modifier.fillMaxWidth(), onClick = onReset,
        )
    }
}

@Composable
private fun AudioLevelPicker(label: String, selected: Float, onSelect: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(label, AshPalette.bone, 8)
            Label("${(selected * 100).toInt()}%", AshPalette.teal, 8)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                AshButton(
                    label = "${(level * 100).toInt()}%", enabled = true,
                    color = if (kotlin.math.abs(selected - level) < 0.01f) AshPalette.flame else AshPalette.panelRaised,
                    modifier = Modifier.weight(1f), onClick = { onSelect(level) },
                )
            }
        }
    }
}

@Composable
private fun RitualConfirmation(reward: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.9f)).clickable { onCancel() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(0.92f).border(2.dp, AshPalette.crimson)
                .background(AshPalette.panel).clickable { }.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Label(stringResource(Res.string.reboot_ritual), AshPalette.flameLight, 12)
            Body(stringResource(Res.string.reboot_ritual_confirm, reward), AshPalette.bone)
            Body(stringResource(Res.string.guide_ritual_preview), AshPalette.muted)
            AshButton(
                label = stringResource(Res.string.reboot_confirm), enabled = true,
                color = AshPalette.crimson, modifier = Modifier.fillMaxWidth(), onClick = onConfirm,
            )
            AshButton(
                label = stringResource(Res.string.reboot_cancel), enabled = true,
                color = AshPalette.panelRaised, modifier = Modifier.fillMaxWidth(), onClick = onCancel,
            )
        }
    }
}

@Composable
internal fun AshButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AshPalette.flame,
    pulse: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.955f else 1f, tween(120))
    val pulseAmount = if (pulse && enabled) {
        val transition = rememberInfiniteTransition()
        val glow by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        )
        glow
    } else 0f
    val targetColor = if (enabled) color.copy(alpha = 0.75f + pulseAmount * 0.25f) else AshPalette.edge
    val borderColor by animateColorAsState(targetColor, tween(220))
    Box(
        modifier = modifier.height(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, borderColor)
            .background(if (enabled) color.copy(alpha = 0.18f) else AshPalette.panelRaised.copy(alpha = 0.45f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (enabled) AshPalette.bone else AshPalette.muted,
            fontFamily = pixelFontFamily(), fontSize = 8.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun Rule() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AshPalette.edge.copy(alpha = 0.55f)))
}

@Composable
internal fun Label(text: String, color: Color, size: Int) {
    Text(text, color = color, fontFamily = pixelFontFamily(), fontSize = size.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun Body(text: String, color: Color = AshPalette.muted, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 13.sp, lineHeight = 19.sp, modifier = modifier)
}
