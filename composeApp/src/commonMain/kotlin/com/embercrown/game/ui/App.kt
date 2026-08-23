package com.embercrown.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.embercrown.game.BuildInfo
import com.embercrown.game.audio.SfxId
import com.embercrown.game.game.AchievementDefinition
import com.embercrown.game.game.AgeDefinition
import com.embercrown.game.game.AppGraph
import com.embercrown.game.game.BuildingDefinition
import com.embercrown.game.game.ChronicleEntry
import com.embercrown.game.game.ChronicleEntryType
import com.embercrown.game.game.DynastyPath
import com.embercrown.game.game.HeirTraitDefinition
import com.embercrown.game.game.DYNASTY_SWITCH_COOLDOWN_SECONDS
import com.embercrown.game.game.GameEvent
import com.embercrown.game.game.GameState
import com.embercrown.game.game.MILESTONE_LEVELS
import com.embercrown.game.game.MIN_LIFETIME_GOLD_FOR_VERFALL
import com.embercrown.game.game.MIN_VERFALL_COUNT_FOR_WIEDERGEBURT
import com.embercrown.game.game.OfflineReport
import com.embercrown.game.game.QuestDefinition
import com.embercrown.game.game.RatssaalUpgradeDefinition
import com.embercrown.game.game.SynergyDefinition
import com.embercrown.game.game.WonderDefinition
import com.embercrown.game.game.achievementMultiplier
import com.embercrown.game.game.academyMultiplier
import com.embercrown.game.game.academyUpgradeCost
import com.embercrown.game.game.bulkBuildingCost
import com.embercrown.game.game.buildingProduction
import com.embercrown.game.game.chroniclePointsForVerfall
import com.embercrown.game.game.clickGain
import com.embercrown.game.game.influenceProduction
import com.embercrown.game.game.isBuildingUnlocked
import com.embercrown.game.game.legacyUpgradeCost
import com.embercrown.game.game.maxAffordableQuantity
import com.embercrown.game.game.milestoneMultiplier
import com.embercrown.game.game.nowEpochSeconds
import com.embercrown.game.game.sagenForWiedergeburt
import com.embercrown.game.i18n.LocalAppLocale
import com.embercrown.game.game.totalProduction
import com.embercrown.game.ui.pixelart.EmberPalette
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelButton
import com.embercrown.game.ui.pixelart.PixelEdge
import com.embercrown.game.ui.pixelart.PixelFit
import com.embercrown.game.ui.pixelart.PixelSegmentedBar
import com.embercrown.game.ui.pixelart.PixelSwitch
import com.embercrown.game.ui.pixelart.ageCrestArt
import com.embercrown.game.ui.pixelart.ageSceneArt
import com.embercrown.game.ui.pixelart.buildingIcon
import com.embercrown.game.ui.pixelart.chroniclePointsIcon
import com.embercrown.game.ui.pixelart.corrupted
import com.embercrown.game.ui.pixelart.einflussIcon
import com.embercrown.game.ui.pixelart.goldIcon
import com.embercrown.game.ui.pixelart.heirEmblemIcon
import com.embercrown.game.ui.pixelart.omenIcon
import com.embercrown.game.ui.pixelart.pixelEdgeLine
import com.embercrown.game.ui.pixelart.Ramps
import com.embercrown.game.ui.pixelart.pixelFrame
import com.embercrown.game.ui.pixelart.sagenIcon
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.academy_card_description
import com.embercrown.game.resources.academy_level_label
import com.embercrown.game.resources.academy_study_button
import com.embercrown.game.resources.academy_title
import com.embercrown.game.resources.academy_upgrade_button
import com.embercrown.game.resources.age_ladder_progress_label
import com.embercrown.game.resources.age_stage_progress_label
import com.embercrown.game.resources.ages_title
import com.embercrown.game.resources.app_name
import com.embercrown.game.resources.buildings_title
import com.embercrown.game.resources.building_synergy_hint
import com.embercrown.game.resources.buy_multiplier_label
import com.embercrown.game.resources.chronicle_card_title
import com.embercrown.game.resources.chronicle_empty_hint
import com.embercrown.game.resources.chronicle_points_label
import com.embercrown.game.resources.click_for_gold_label
import com.embercrown.game.resources.currencies_label
import com.embercrown.game.resources.currency_abbr_achievements
import com.embercrown.game.resources.currency_abbr_chronicle
import com.embercrown.game.resources.currency_abbr_influence
import com.embercrown.game.resources.currency_abbr_legends
import com.embercrown.game.resources.dragon_buff_badge
import com.embercrown.game.resources.dynasty_path_activate_button
import com.embercrown.game.resources.dynasty_path_invest_button
import com.embercrown.game.resources.dynasty_path_level_label
import com.embercrown.game.resources.dynasty_switch_cooldown_hint
import com.embercrown.game.resources.dynasty_switch_hint
import com.embercrown.game.resources.dynasty_title
import com.embercrown.game.resources.einfluss_label
import com.embercrown.game.resources.footer_new_label
import com.embercrown.game.resources.gold_label
import com.embercrown.game.resources.heir_card_title
import com.embercrown.game.resources.heir_name_and_trait
import com.embercrown.game.resources.heir_none_hint
import com.embercrown.game.resources.legends_label
import com.embercrown.game.resources.locked_group_from
import com.embercrown.game.resources.locked_group_label
import com.embercrown.game.resources.locked_section_title
import com.embercrown.game.resources.milestone_hint_label
import com.embercrown.game.resources.next_age_label
import com.embercrown.game.resources.offline_flavor_long
import com.embercrown.game.resources.offline_flavor_medium
import com.embercrown.game.resources.offline_flavor_short
import com.embercrown.game.resources.offline_report_body
import com.embercrown.game.resources.offline_report_dismiss_button
import com.embercrown.game.resources.offline_report_title
import com.embercrown.game.resources.per_click_label
import com.embercrown.game.resources.production_label
import com.embercrown.game.resources.quest_claim_button
import com.embercrown.game.resources.quest_reward_label
import com.embercrown.game.resources.quests_empty_hint
import com.embercrown.game.resources.quests_title
import com.embercrown.game.resources.ratssaal_buy_button
import com.embercrown.game.resources.ratssaal_card_description
import com.embercrown.game.resources.ratssaal_owned_label
import com.embercrown.game.resources.ratssaal_title
import com.embercrown.game.resources.rail_chronicle_label
import com.embercrown.game.resources.reign_tab_achievements
import com.embercrown.game.resources.reign_tab_realm
import com.embercrown.game.resources.reign_tab_system
import com.embercrown.game.resources.reign_title
import com.embercrown.game.resources.reward_yield_label
import com.embercrown.game.resources.rules_offline_progress_description
import com.embercrown.game.resources.rules_offline_progress_label
import com.embercrown.game.resources.system_export_button
import com.embercrown.game.resources.system_language_english
import com.embercrown.game.resources.system_language_german
import com.embercrown.game.resources.system_language_label
import com.embercrown.game.resources.system_language_system
import com.embercrown.game.resources.system_mute_label
import com.embercrown.game.resources.system_volume_label
import com.embercrown.game.resources.system_import_button
import com.embercrown.game.resources.system_import_button_short
import com.embercrown.game.resources.system_import_error
import com.embercrown.game.resources.system_save_button_short
import com.embercrown.game.resources.tab_holdings
import com.embercrown.game.resources.tab_reign
import com.embercrown.game.resources.verfall_button
import com.embercrown.game.resources.verfall_corruption_label
import com.embercrown.game.resources.verfall_description
import com.embercrown.game.resources.verfall_footer_yield_label
import com.embercrown.game.resources.verfall_locked_hint
import com.embercrown.game.resources.verfall_survived_short
import com.embercrown.game.resources.verfall_title
import com.embercrown.game.resources.verfalls_label
import com.embercrown.game.resources.wiedergeburt_button
import com.embercrown.game.resources.wiedergeburt_locked_hint
import com.embercrown.game.resources.wiedergeburt_title
import com.embercrown.game.resources.wonder_build_button
import com.embercrown.game.resources.wonder_built_label
import com.embercrown.game.resources.wonder_locked_hint
import com.embercrown.game.resources.wonders_title
import com.embercrown.game.update.autoUpdateIfNeeded
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The four flat destinations the phone footer nav switches between — replacing the old
 * two-tab-plus-six-reign-sub-tab hierarchy entirely.
 */
private enum class PhoneDestination(val labelRes: StringResource) {
    HOLDINGS(Res.string.tab_holdings),
    REIGN(Res.string.tab_reign),
    ACHIEVEMENTS(Res.string.reign_tab_achievements),
    SYSTEM(Res.string.reign_tab_system),
}

private val EmberGold = EmberPalette.Gold
private val EmberGoldBright = EmberPalette.GoldBright
private val EmberBackground = EmberPalette.Background
private val EmberPanel = EmberPalette.Panel
private val EmberPanelLight = EmberPalette.PanelLight
private val EmberHeaderBar = EmberPalette.HeaderBar
private val EmberRail = EmberPalette.Rail
private val EmberAccent = EmberPalette.Accent
private val EmberAccentBright = EmberPalette.AccentBright
private val EmberDim = EmberPalette.Dim
private val EmberWhite = EmberPalette.White
private val LockedTextColor = Color(0xFF4A3A28)

private val EmbercrownColors = darkColorScheme(
    primary = EmberGold,
    background = EmberBackground,
    surface = EmberPanel,
    onPrimary = Color.Black,
    onBackground = EmberGold,
    onSurface = EmberGold,
)

@Composable
fun App() {
    val uiSettings by AppGraph.uiSettings.state.collectAsState()
    // Fires once per launch, independent of UpdateBanner: on desktop this is normally a no-op
    // (the launcher already updated the jar before this process started), on Android it
    // downloads and installs a newer build with no confirmation of our own.
    LaunchedEffect(Unit) { autoUpdateIfNeeded() }
    LaunchedEffect(uiSettings.masterVolume, uiSettings.muted) {
        AppGraph.soundPlayer.setVolume(uiSettings.masterVolume)
        AppGraph.soundPlayer.setMuted(uiSettings.muted)
    }
    // Starts once and loops forever at the app root, same reasoning as the root-level event
    // collector below: PhoneLayout's destinations dispose/recreate on tab switch, so playback
    // can't live there without cutting out and restarting every time the user changes tabs.
    // Both loops start together — musicPlayer and corruptedMusicPlayer are volume-crossfaded
    // against each other below (keyed on state.corruption) rather than swapped, so both must
    // always be playing.
    LaunchedEffect(Unit) {
        AppGraph.musicPlayer.play()
        AppGraph.corruptedMusicPlayer.play()
    }
    CompositionLocalProvider(LocalAppLocale provides uiSettings.language) {
        key(uiSettings.language) {
            MaterialTheme(colorScheme = EmbercrownColors) {
                Surface(color = EmberBackground) {
                    val state by AppGraph.engine.state.collectAsState()
                    // Continuous crossfade, not a discrete swap: as Verfall corruption rises,
                    // musicPlayer fades out while corruptedMusicPlayer fades in — both loops
                    // already playing (see the LaunchedEffect above), so this only ever touches
                    // setVolume, which every platform's MusicPlayer already implements.
                    LaunchedEffect(state.corruption, uiSettings.masterVolume, uiSettings.muted) {
                        val t = state.corruption.toFloat()
                        AppGraph.musicPlayer.setVolume(uiSettings.masterVolume * (1f - t))
                        AppGraph.musicPlayer.setMuted(uiSettings.muted)
                        AppGraph.corruptedMusicPlayer.setVolume(uiSettings.masterVolume * t)
                        AppGraph.corruptedMusicPlayer.setMuted(uiSettings.muted)
                    }
                    var multiplier by remember { mutableStateOf(BuyMultiplier.X1) }
                    var achievementToastId by remember { mutableStateOf<String?>(null) }
                    var ageUpCelebration by remember { mutableStateOf(false) }
                    var blightAnimationRunning by remember { mutableStateOf(false) }

                    // Single root-level collector: drives sound for every event, plus the two
                    // animation states (toast, celebration) that need to survive phone-nav tab
                    // switches, which dispose/recreate the tab composables that fired them.
                    LaunchedEffect(Unit) {
                        AppGraph.engine.events.collect { event ->
                            sfxFor(event)?.let { AppGraph.soundPlayer.play(it) }
                            when (event) {
                                is GameEvent.AchievementUnlocked -> achievementToastId = event.id
                                is GameEvent.AgeAdvanced -> ageUpCelebration = true
                                else -> {}
                            }
                        }
                    }
                    LaunchedEffect(achievementToastId) {
                        if (achievementToastId != null) {
                            delay(2500)
                            achievementToastId = null
                        }
                    }
                    LaunchedEffect(ageUpCelebration) {
                        if (ageUpCelebration) {
                            delay(900)
                            ageUpCelebration = false
                        }
                    }

                    val offlineReport by AppGraph.engine.offlineReport.collectAsState()

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isDesktop = maxWidth >= 700.dp
                        val onTriggerVerfall: () -> Unit = { if (!blightAnimationRunning) blightAnimationRunning = true }
                        VerfallCollapseOverlay(
                            trigger = blightAnimationRunning,
                            state = state,
                            columns = if (isDesktop) 30 else 13,
                            rows = if (isDesktop) 17 else 24,
                            showRip = isDesktop,
                            titleFontSize = if (isDesktop) 34.sp else 17.sp,
                            yieldFontSize = if (isDesktop) 22.sp else 14.sp,
                            onFinished = { blightAnimationRunning = false },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (isDesktop) {
                                DesktopLayout(state = state, multiplier = multiplier, onMultiplierChange = { multiplier = it }, onTriggerVerfall = onTriggerVerfall)
                            } else {
                                PhoneLayout(state = state, multiplier = multiplier, onMultiplierChange = { multiplier = it }, onTriggerVerfall = onTriggerVerfall)
                            }
                        }
                        AgeUpCelebration(visible = ageUpCelebration, modifier = Modifier.matchParentSize())
                        AchievementToast(achievementId = achievementToastId, modifier = Modifier.align(Alignment.TopCenter))
                        OfflineReportDialog(report = offlineReport, modifier = Modifier.matchParentSize())
                    }
                }
            }
        }
    }
}

private fun sfxFor(event: GameEvent): SfxId? = when (event) {
    is GameEvent.GoldTapped -> SfxId.TAP
    is GameEvent.PurchaseSucceeded -> SfxId.PURCHASE
    is GameEvent.PurchaseDenied -> SfxId.DENY
    is GameEvent.BuildingMilestoneReached -> SfxId.ACHIEVEMENT
    GameEvent.AcademyStudySucceeded -> SfxId.PURCHASE
    GameEvent.AcademyStudyDenied -> SfxId.DENY
    is GameEvent.AchievementUnlocked -> SfxId.ACHIEVEMENT
    is GameEvent.AgeAdvanced -> SfxId.AGE_UP
    GameEvent.DragonBuffActivated -> SfxId.DRAGON
    is GameEvent.RatssaalUpgradePurchased -> SfxId.PURCHASE
    is GameEvent.RatssaalUpgradeDenied -> SfxId.DENY
    is GameEvent.QuestClaimed -> SfxId.ACHIEVEMENT
    is GameEvent.QuestClaimDenied -> SfxId.DENY
    GameEvent.DynastySwitchDenied -> SfxId.DENY
    // No dedicated sting yet — audio for the corruption system is a deliberate follow-up, not
    // part of this pass (see the CorruptionPeaked doc comment in GameEngine.kt).
    GameEvent.CorruptionPeaked -> null
    is GameEvent.WonderBuilt -> SfxId.PURCHASE
    is GameEvent.WonderBuildDenied -> SfxId.DENY
    GameEvent.OmenBuffActivated -> SfxId.OMEN
    GameEvent.OmenMalusActivated -> SfxId.OMEN
}

@Composable
private fun AgeUpCelebration(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(600)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(EmberGoldBright.copy(alpha = 0.35f), Color.Transparent)),
            ),
        )
    }
}

@Composable
private fun AchievementToast(achievementId: String?, modifier: Modifier = Modifier) {
    // AnimatedVisibility keeps rendering its content through the exit fade, after
    // achievementId has already gone back to null — so the last non-null id is remembered
    // separately, purely so the toast doesn't go blank mid-fade-out.
    var lastId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(achievementId) {
        if (achievementId != null) lastId = achievementId
    }
    AnimatedVisibility(
        visible = achievementId != null,
        modifier = modifier.padding(top = 12.dp),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = fadeOut(),
    ) {
        val achievement = lastId?.let { AchievementDefinition.byId(it) }
        if (achievement != null) {
            Row(
                modifier = Modifier
                    .pixelFrame(fill = EmberAccent, bevelLight = EmberAccentBright)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(achievement.nameRes), color = EmberWhite, fontSize = 12.sp, fontFamily = pixelFontFamily())
            }
        }
    }
}

/** A flavor line for the offline report, purely ephemeral — not written into the persisted Chronicle. */
private fun offlineFlavorLine(report: OfflineReport): StringResource = when {
    report.elapsedSeconds < 600L -> Res.string.offline_flavor_short
    report.elapsedSeconds < 7200L -> Res.string.offline_flavor_medium
    else -> Res.string.offline_flavor_long
}

@Composable
private fun OfflineReportDialog(report: OfflineReport?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = report != null,
        modifier = modifier,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(EmberBackground.copy(alpha = 0.75f)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { AppGraph.engine.acknowledgeOfflineReport() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .pixelFrame(fill = EmberPanel, bevelLight = EmberPanelLight)
                    .padding(20.dp)
                    // Swallows taps so tapping inside the card doesn't fall through to the
                    // full-screen scrim's dismiss handler above.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.offline_report_title), color = EmberGoldBright, fontSize = 14.sp, fontFamily = pixelFontFamily())
                if (report != null) {
                    Text(
                        stringResource(Res.string.offline_report_body, formatOfflineDuration(report.elapsedSeconds), formatAmount(report.goldGained)),
                        color = EmberGold.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        stringResource(offlineFlavorLine(report)),
                        color = EmberDim,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                PixelButton(
                    onClick = { AppGraph.engine.acknowledgeOfflineReport() },
                    bevelLight = EmberAccentBright,
                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 16.dp),
                ) {
                    Text(stringResource(Res.string.offline_report_dismiss_button), fontFamily = pixelFontFamily(), fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatOfflineDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}

private fun formatCooldown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------------------------
// Phone (< 700 dp)
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhoneLayout(
    state: GameState,
    multiplier: BuyMultiplier,
    onMultiplierChange: (BuyMultiplier) -> Unit,
    onTriggerVerfall: () -> Unit,
) {
    var destination by remember { mutableStateOf(PhoneDestination.HOLDINGS) }

    Column(modifier = Modifier.fillMaxSize()) {
        CurrencyHeaderPhone(state)
        UpdateBanner(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        SceneStage(
            state = state,
            modifier = Modifier.fillMaxWidth().height(230.dp),
            clickHintFontSize = 10.sp,
            tipBarWidth = 220.dp,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (destination) {
                PhoneDestination.HOLDINGS ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        BuildingsSection(state = state, multiplier = multiplier, onMultiplierChange = onMultiplierChange, compact = true)
                    }
                PhoneDestination.REIGN ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        RealmTabContent(state)
                    }
                PhoneDestination.ACHIEVEMENTS ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        AchievementsTabContent(state)
                    }
                PhoneDestination.SYSTEM ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        SystemTabContent(state)
                    }
            }
        }

        VerfallFooterStrip(state = state, onTriggerVerfall = onTriggerVerfall, compact = true)
        FooterNav(selected = destination, onSelect = { destination = it }, state = state)
    }
}

@Composable
private fun CurrencyHeaderPhone(state: GameState) {
    val next = state.nextAge
    val progress = next?.let { (state.lifetimeGold / it.lifetimeGoldThreshold).toFloat().coerceIn(0f, 1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmberHeaderBar)
            .pixelEdgeLine(PixelEdge.Bottom)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Text(stringResource(Res.string.gold_label).uppercase(), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.12f.em)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(formatAmount(state.gold), color = EmberGoldBright, fontSize = 26.sp, fontFamily = pixelFontFamily())
            if (state.dragonBuffTicksRemaining > 0) {
                Box(modifier = Modifier.pixelFrame(fill = EmberAccent, bevelLight = EmberAccentBright)) {
                    Text(
                        stringResource(Res.string.dragon_buff_badge, state.dragonBuffTicksRemaining),
                        color = EmberWhite,
                        fontSize = 9.sp,
                        fontFamily = pixelFontFamily(),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            } else {
                Text("+${formatAmount(totalProduction(state))}/s", color = EmberGold, fontSize = 11.sp, fontFamily = pixelFontFamily())
            }
        }
        if (next != null && progress != null) {
            Text(
                "${stringResource(state.currentAge.nameRes).uppercase()} > ${stringResource(next.nameRes).uppercase()} · ${(progress * 100).toInt()}%",
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 14.dp),
            )
            PixelSegmentedBar(progress = progress, segments = 20, modifier = Modifier.fillMaxWidth().height(9.dp).padding(top = 8.dp))
        }
    }
}

@Composable
private fun VerfallFooterStrip(
    state: GameState,
    onTriggerVerfall: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val canTrigger = state.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (!compact) it.height(74.dp) else it }
            .background(EmberBackground)
            .pixelEdgeLine(PixelEdge.Top)
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canTrigger) {
            val template = stringResource(Res.string.verfall_footer_yield_label)
            val amountText = formatAmount(chroniclePointsForVerfall(state))
            val parts = template.split("%1\$s")
            val annotated = buildAnnotatedString {
                append(parts.getOrElse(0) { "" })
                withStyle(SpanStyle(color = EmberGoldBright)) { append(amountText) }
                append(parts.getOrElse(1) { "" })
            }
            Text(
                annotated,
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                lineHeight = 1.8.em,
                modifier = Modifier.widthIn(max = 200.dp).weight(1f, fill = false),
            )
        } else {
            Text(
                stringResource(Res.string.verfall_locked_hint, formatAmount(MIN_LIFETIME_GOLD_FOR_VERFALL)),
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                lineHeight = 1.6.em,
                modifier = Modifier.widthIn(max = 200.dp).weight(1f, fill = false),
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .pixelFrame(
                    fill = if (canTrigger) EmberAccent else EmberPanel,
                    bevelLight = if (canTrigger) EmberAccentBright else EmberPalette.BevelLight,
                )
                .clickable(enabled = canTrigger) { onTriggerVerfall() }
                .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 11.dp else 14.dp),
        ) {
            Text(
                stringResource(Res.string.verfall_button).uppercase(),
                color = if (canTrigger) EmberWhite else EmberDim,
                fontSize = if (compact) 9.sp else 10.sp,
                fontFamily = pixelFontFamily(),
                letterSpacing = 0.1f.em,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FooterNav(selected: PhoneDestination, onSelect: (PhoneDestination) -> Unit, state: GameState) {
    val reignBadge = remember(state) {
        var n = 0
        if (state.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL) n++
        if (state.chroniclePoints >= academyUpgradeCost(state.academyLevel)) n++
        DynastyPath.entries.forEach { path ->
            val level = state.legacyLevels[path] ?: 0
            if (state.activeDynastyPath == path && state.sagen >= legacyUpgradeCost(level)) n++
        }
        n
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelEdgeLine(PixelEdge.Top)
            .background(EmberHeaderBar),
    ) {
        PhoneDestination.entries.forEachIndexed { index, destination ->
            val isSelected = destination == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .let { if (index < PhoneDestination.entries.lastIndex) it.pixelEdgeLine(PixelEdge.End) else it }
                    .background(if (isSelected) EmberPanel else Color.Transparent)
                    .clickable { onSelect(destination) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(destination.labelRes).uppercase(),
                    color = if (isSelected) EmberGoldBright else EmberGold.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = pixelFontFamily(),
                )
                val sub = when (destination) {
                    PhoneDestination.HOLDINGS -> BuildingDefinition.all.size.toString()
                    PhoneDestination.REIGN -> if (reignBadge > 0) "$reignBadge " + stringResource(Res.string.footer_new_label) else ""
                    PhoneDestination.ACHIEVEMENTS -> "${state.unlockedAchievements.size}/${AchievementDefinition.all.size}"
                    PhoneDestination.SYSTEM -> BuildInfo.VERSION
                }
                if (sub.isNotEmpty()) {
                    Text(
                        sub,
                        color = if (destination == PhoneDestination.REIGN && reignBadge > 0) EmberAccent else EmberDim,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Desktop (>= 700 dp)
// ---------------------------------------------------------------------------------------------

private enum class ReichTab(val labelRes: StringResource) {
    REALM(Res.string.reign_tab_realm),
    ACHIEVEMENTS(Res.string.reign_tab_achievements),
    SYSTEM(Res.string.reign_tab_system),
}

@Composable
private fun DesktopLayout(
    state: GameState,
    multiplier: BuyMultiplier,
    onMultiplierChange: (BuyMultiplier) -> Unit,
    onTriggerVerfall: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CurrencyHeaderDesktop(state)
        UpdateBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AgesRail(state = state, modifier = Modifier.width(214.dp).fillMaxHeight())
            SceneStage(state = state, modifier = Modifier.weight(1f).fillMaxHeight())
            ReichColumn(
                state = state,
                multiplier = multiplier,
                onMultiplierChange = onMultiplierChange,
                onTriggerVerfall = onTriggerVerfall,
                modifier = Modifier.width(520.dp).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HeaderMiniStat(color: Color, value: String, abbrRes: StringResource) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color))
        Text(value, color = EmberGoldBright, fontSize = 11.sp, fontFamily = pixelFontFamily(), modifier = Modifier.padding(start = 6.dp), maxLines = 1)
        Text(stringResource(abbrRes), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun CurrencyHeaderDesktop(state: GameState) {
    val next = state.nextAge
    Row(modifier = Modifier.fillMaxWidth().height(92.dp).background(EmberHeaderBar).pixelEdgeLine(PixelEdge.Bottom)) {
        Column(
            modifier = Modifier.width(300.dp).fillMaxHeight().pixelEdgeLine(PixelEdge.End).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.gold_label).uppercase(), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.12f.em)
            Text(
                formatAmount(state.gold),
                color = EmberGoldBright,
                fontSize = 30.sp,
                fontFamily = pixelFontFamily(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().pixelEdgeLine(PixelEdge.End).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.production_label), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily())
            if (state.dragonBuffTicksRemaining > 0) {
                Text(
                    stringResource(Res.string.dragon_buff_badge, state.dragonBuffTicksRemaining),
                    color = EmberGold,
                    fontSize = 17.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text("${formatAmount(totalProduction(state))} /s", color = EmberGold, fontSize = 17.sp, fontFamily = pixelFontFamily(), maxLines = 1)
            }
            Text(
                stringResource(Res.string.per_click_label, formatAmount(clickGain(state))),
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                modifier = Modifier.padding(top = 7.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().pixelEdgeLine(PixelEdge.End).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "${stringResource(state.currentAge.nameRes).uppercase()}${if (next != null) " > ${stringResource(next.nameRes).uppercase()}" else ""}",
                color = EmberGold,
                fontSize = 10.sp,
                fontFamily = pixelFontFamily(),
                letterSpacing = 0.1f.em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (next != null) {
                val progress = (state.lifetimeGold / next.lifetimeGoldThreshold).toFloat().coerceIn(0f, 1f)
                PixelSegmentedBar(
                    progress = progress,
                    segments = 20,
                    filledColor = EmberGold,
                    emptyColor = EmberPalette.Panel,
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(11.dp),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            if (next != null) {
                val remaining = (next.lifetimeGoldThreshold - state.lifetimeGold).coerceAtLeast(0.0)
                Text(
                    stringResource(Res.string.age_stage_progress_label, state.currentAge.index + 1, AgeDefinition.all.size, formatAmount(remaining)),
                    color = EmberDim,
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.dragonBuffTicksRemaining > 0) {
                    Box(modifier = Modifier.padding(end = 10.dp).pixelFrame(fill = EmberAccent, bevelLight = EmberAccentBright)) {
                        Text(
                            stringResource(Res.string.dragon_buff_badge, state.dragonBuffTicksRemaining),
                            color = EmberWhite,
                            fontSize = 9.sp,
                            fontFamily = pixelFontFamily(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
                HeaderMiniStat(Ramps.PaperLight, formatAmount(state.chroniclePoints), Res.string.currency_abbr_chronicle)
                Spacer(modifier = Modifier.width(14.dp))
                HeaderMiniStat(Ramps.ArcaneLight, formatAmount(state.sagen), Res.string.currency_abbr_legends)
                Spacer(modifier = Modifier.width(14.dp))
                HeaderMiniStat(Ramps.ClothLight, formatAmount(state.einfluss), Res.string.currency_abbr_influence)
                Spacer(modifier = Modifier.width(14.dp))
                HeaderMiniStat(
                    EmberGold,
                    "${state.unlockedAchievements.size}/${AchievementDefinition.all.size}",
                    Res.string.currency_abbr_achievements,
                )
            }
        }
    }
}

@Composable
private fun AgesRail(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(EmberRail).pixelEdgeLine(PixelEdge.End)) {
        Text(
            stringResource(Res.string.age_ladder_progress_label, state.currentAge.index + 1, AgeDefinition.all.size),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.1f.em,
            modifier = Modifier.fillMaxWidth().pixelEdgeLine(PixelEdge.Bottom).padding(horizontal = 14.dp, vertical = 11.dp),
        )
        // Portrait crest for the current age — distinct from the wide ageSceneArt landscape on
        // the stage, this is the small heraldic shield that grows more ornate as ages climb.
        val crestCorruptionBucket = (state.corruption * 40).roundToInt()
        Box(modifier = Modifier.fillMaxWidth().pixelEdgeLine(PixelEdge.Bottom).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            PixelArtImage(
                art = remember(state.currentAge.index, crestCorruptionBucket) {
                    val base = ageCrestArt(state.currentAge.index, AgeDefinition.all.size)
                    if (crestCorruptionBucket <= 0) base else base.corrupted(crestCorruptionBucket / 40f)
                },
                modifier = Modifier.height(132.dp).width(104.dp),
                fit = PixelFit.Contain,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
        ) {
            AgeDefinition.all.forEach { age -> AgeRailRow(age = age, state = state) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmberBackground)
                .pixelEdgeLine(PixelEdge.Top)
                .padding(14.dp),
        ) {
            Text(
                stringResource(Res.string.rail_chronicle_label, stringResource(state.currentAge.nameRes).uppercase()),
                color = EmberAccent,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                letterSpacing = 0.1f.em,
            )
            Text(
                stringResource(state.currentAge.chronicleRes),
                color = EmberDim,
                fontSize = 10.sp,
                fontFamily = pixelFontFamily(),
                lineHeight = 1.85.em,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun AgeRailRow(age: AgeDefinition, state: GameState) {
    val current = age.index == state.currentAge.index
    val reached = state.lifetimeGold >= age.lifetimeGoldThreshold
    val background = if (current) EmberPanel else Color.Transparent
    val markColor = if (current) EmberGold else if (reached) EmberPalette.BevelLight else Color(0xFF1F1811)
    val nameColor = if (current) EmberGoldBright else if (reached) EmberGold.copy(alpha = 0.72f) else LockedTextColor

    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp).background(background).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(11.dp).background(markColor))
        Text(
            stringResource(age.nameRes),
            color = nameColor,
            fontSize = 10.sp,
            fontFamily = pixelFontFamily(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 9.dp),
        )
    }
}

@Composable
private fun ReichColumn(
    state: GameState,
    multiplier: BuyMultiplier,
    onMultiplierChange: (BuyMultiplier) -> Unit,
    onTriggerVerfall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(ReichTab.REALM) }
    // BuildingsHeader, the tab bar and the footer are fixed; only the middle (building list +
    // selected tab content) scrolls — a long building list must never push the tab bar or the
    // Verfall button below the window's visible bounds.
    Column(modifier = modifier.background(EmberRail).pixelEdgeLine(PixelEdge.Start)) {
        BuildingsHeader(multiplier = multiplier, onMultiplierChange = onMultiplierChange, compact = false)

        Row(modifier = Modifier.fillMaxWidth().pixelEdgeLine(PixelEdge.Bottom)) {
            ReichTab.entries.forEach { entry ->
                val isSelected = entry == tab
                Box(
                    modifier = Modifier
                        .background(if (isSelected) EmberPanel else Color.Transparent)
                        .clickable { tab = entry }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                ) {
                    Text(
                        stringResource(entry.labelRes).uppercase(),
                        color = if (isSelected) EmberGoldBright else EmberDim,
                        fontSize = 10.sp,
                        fontFamily = pixelFontFamily(),
                        letterSpacing = 0.08f.em,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            BuildingsList(state = state, multiplier = multiplier, compact = false)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                when (tab) {
                    ReichTab.REALM -> RealmTabContent(state)
                    ReichTab.ACHIEVEMENTS -> AchievementsTabContent(state)
                    ReichTab.SYSTEM -> SystemTabContent(state)
                }
            }
        }

        VerfallFooterStrip(state = state, onTriggerVerfall = onTriggerVerfall, modifier = Modifier)
    }
}

// ---------------------------------------------------------------------------------------------
// Shared: the click-for-gold stage
// ---------------------------------------------------------------------------------------------

private data class FloatingGoldNumber(val id: Int, val amount: Double)

@Composable
private fun SceneStage(
    state: GameState,
    modifier: Modifier = Modifier,
    clickHintFontSize: TextUnit = 11.sp,
    tipBarWidth: Dp = 280.dp,
) {
    val age = state.currentAge

    val tapScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val floatingNumbers = remember { mutableStateListOf<FloatingGoldNumber>() }
    var nextFloatingId by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        AppGraph.engine.events.filterIsInstance<GameEvent.GoldTapped>().collect { event ->
            floatingNumbers.add(FloatingGoldNumber(id = nextFloatingId++, amount = event.amount))
        }
    }
    fun onTap() {
        AppGraph.engine.click()
        scope.launch {
            tapScale.animateTo(1.12f, animationSpec = tween(60))
            tapScale.animateTo(1f, animationSpec = tween(140))
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0D0A07))
            .clipToBounds()
            .clickable { onTap() },
    ) {
        // Bucketed into 40 steps rather than read as a raw Double: corruption crawls over 25
        // minutes, so re-rasterizing the full grid every second (the tick interval) would be
        // wasted work for a change too small to see — this redraws only every ~40s of ramp.
        val corruptionBucket = (state.corruption * 40).roundToInt()
        Crossfade(
            targetState = age.index,
            animationSpec = tween(600),
            // No forced aspectRatio and no width cap: either one re-introduces letterboxing
            // against whatever the real stage box happens to be at the current window size, on
            // top of whatever PixelFit.Contain already does against the art's own aspect below —
            // independent boxings compounding into a small image floating in a mostly-empty
            // frame (a max-width cap alone still starves the box's height on a tall/maximized
            // window, since Contain then pillarboxes the other axis). Filling the actual stage
            // box and leaving Contain as the sole fit decision keeps the art as large as the
            // real space allows, however that space is shaped.
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .border(4.dp, EmberPalette.Shadow),
        ) { index ->
            PixelArtImage(
                art = remember(index, corruptionBucket) {
                    val base = ageSceneArt(index, AgeDefinition.all.size)
                    if (corruptionBucket <= 0) base else base.corrupted(corruptionBucket / 40f)
                },
                modifier = Modifier.fillMaxSize(),
                fit = PixelFit.Contain,
            )
        }

        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.radialGradient(0f to EmberGold.copy(alpha = 0.16f), 0.62f to Color.Transparent),
            ),
        )

        Box(modifier = Modifier.align(Alignment.Center)) {
            floatingNumbers.forEach { number ->
                FloatingGoldNumberText(number = number, onFinished = { floatingNumbers.remove(number) })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xFF0D0A07).copy(alpha = 0.96f))),
        )

        val pulse = rememberInfiniteTransition(label = "clickHintPulse")
        val pulseAlpha by pulse.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1900), repeatMode = RepeatMode.Reverse),
            label = "clickHintAlpha",
        )
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.click_for_gold_label),
                color = EmberGold,
                fontSize = clickHintFontSize,
                fontFamily = pixelFontFamily(),
                letterSpacing = 0.14f.em,
                modifier = Modifier.alpha(pulseAlpha),
            )
            PixelSegmentedBar(
                progress = 0f,
                segments = 16,
                filledColor = EmberAccent,
                emptyColor = Color(0xFF1F1811),
                modifier = Modifier.padding(top = 11.dp).width(tipBarWidth).height(8.dp),
            )
        }

        // Drachenüberflug: a tappable sighting that appears briefly and, if tapped in time,
        // activates a timed production buff. Its own clickable consumes the tap so it doesn't
        // also fall through to the click-for-gold handler on the Box below it.
        if (state.dragonAvailableTicksRemaining > 0) {
            val dragonPulse = rememberInfiniteTransition(label = "dragonPulse")
            val dragonPulseScale by dragonPulse.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
                label = "dragonPulseScale",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(40.dp)
                    .scale(dragonPulseScale)
                    .pixelFrame(fill = EmberAccent, bevelLight = EmberAccentBright)
                    .clickable { AppGraph.engine.tapDragonEvent() },
                contentAlignment = Alignment.Center,
            ) {
                val dragonIcon = remember { buildingIcon("dragon_hoard") }
                if (dragonIcon != null) PixelArtImage(dragonIcon, modifier = Modifier.size(28.dp))
            }
        }

        // Verfall Omen: same tappable-sighting pattern as the Dragon above, but only rolls while
        // corruption is building, and its effect is a coin flip (short buff OR short malus) rather
        // than always positive — placed at the opposite corner so the two never overlap.
        if (state.omenAvailableTicksRemaining > 0) {
            val omenPulse = rememberInfiniteTransition(label = "omenPulse")
            val omenPulseScale by omenPulse.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
                label = "omenPulseScale",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(40.dp)
                    .scale(omenPulseScale)
                    .pixelFrame(fill = EmberPanel, bevelLight = Ramps.CorruptionLight)
                    .clickable { AppGraph.engine.tapOmenEvent() },
                contentAlignment = Alignment.Center,
            ) {
                PixelArtImage(remember { omenIcon() }, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun FloatingGoldNumberText(number: FloatingGoldNumber, onFinished: () -> Unit) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(number.id) {
        launch { offsetY.animateTo(-32f, animationSpec = tween(700)) }
        launch { alpha.animateTo(0f, animationSpec = tween(700)) }
        delay(700)
        onFinished()
    }
    Text(
        "+${formatAmount(number.amount)}",
        color = EmberGoldBright,
        fontSize = 13.sp,
        fontFamily = pixelFontFamily(),
        modifier = Modifier.offset(y = offsetY.value.dp).alpha(alpha.value),
    )
}

// ---------------------------------------------------------------------------------------------
// Buildings (flat list, shared between phone and the desktop Reich column)
// ---------------------------------------------------------------------------------------------

/** Full Holdings block (fixed header + list) — used as-is on phone, where the whole destination
 * is already one scrollable column. Desktop's [ReichColumn] instead calls [BuildingsHeader] and
 * [BuildingsList] separately, so only the list scrolls alongside the Reich tabs below it and the
 * fixed header/footer never get pushed off-screen by a long building list. */
@Composable
private fun BuildingsSection(state: GameState, multiplier: BuyMultiplier, onMultiplierChange: (BuyMultiplier) -> Unit, compact: Boolean) {
    BuildingsHeader(multiplier = multiplier, onMultiplierChange = onMultiplierChange, compact = compact)
    BuildingsList(state = state, multiplier = multiplier, compact = compact)
}

@Composable
private fun BuildingsHeader(multiplier: BuyMultiplier, onMultiplierChange: (BuyMultiplier) -> Unit, compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pixelEdgeLine(PixelEdge.Bottom)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.buildings_title).uppercase(),
            color = EmberGold,
            fontSize = if (compact) 10.sp else 11.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.1f.em,
        )
        BuyMultiplierChips(selected = multiplier, onSelect = onMultiplierChange, compact = compact)
    }
}

@Composable
private fun BuildingsList(state: GameState, multiplier: BuyMultiplier, compact: Boolean) {
    val unlocked = remember(state) { BuildingDefinition.all.filter { isBuildingUnlocked(it, state) } }
    val locked = remember(state) { BuildingDefinition.all.filter { !isBuildingUnlocked(it, state) } }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        unlocked.forEach { definition -> BuildingRow(state, definition, multiplier, compact) }
        locked.forEach { definition -> LockedBuildingRow(definition, compact) }
    }
}

@Composable
private fun BuyMultiplierChips(selected: BuyMultiplier, onSelect: (BuyMultiplier) -> Unit, compact: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BuyMultiplier.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .background(if (isSelected) EmberAccent else EmberPanel)
                    .clickable { onSelect(option) }
                    .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 5.dp),
            ) {
                Text(
                    option.label,
                    color = if (isSelected) EmberWhite else EmberGold.copy(alpha = 0.75f),
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontFamily = pixelFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun LanguageChips(selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val options: List<Pair<String?, StringResource>> = listOf(
        null to Res.string.system_language_system,
        "en" to Res.string.system_language_english,
        "de" to Res.string.system_language_german,
    )
    // Each chip carries weight(1f) so the row always fits its container, even the narrow
    // desktop sidebar column — unlike BuyMultiplierChips (4 short digits, always has room),
    // "System"/"English"/"German" are wide enough to overflow a fixed-intrinsic-width Row there.
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (code, labelRes) ->
            val isSelected = code == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pixelFrame(
                        fill = if (isSelected) EmberAccent else EmberPanel,
                        borderWidth = 2.dp,
                        bevelLight = if (isSelected) EmberAccentBright else EmberPalette.BevelLight,
                    )
                    .clickable { onSelect(code) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(labelRes),
                    color = if (isSelected) EmberWhite else EmberGold.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A tappable 5-step volume control built on [PixelSegmentedBar] rather than a new slider widget. */
@Composable
private fun VolumeBar(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    // Continuous drag, not just discrete taps: dragging the finger/cursor along the bar tracks
    // the exact x position every frame so the user can dial in and see a precise level, not just
    // snap between a handful of fixed steps.
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                fun update(x: Float) = onVolumeChange((x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                detectTapGestures { offset -> update(offset.x) }
            }
            .pointerInput(Unit) {
                fun update(x: Float) = onVolumeChange((x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                detectHorizontalDragGestures(onHorizontalDrag = { change, _ ->
                    change.consume()
                    update(change.position.x)
                })
            },
    ) {
        PixelSegmentedBar(progress = volume, segments = 20, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ThinProgressTrack(progress: Float, filledColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(EmberPalette.Panel)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(filledColor))
    }
}

@Composable
private fun BuildingRow(state: GameState, definition: BuildingDefinition, multiplier: BuyMultiplier, compact: Boolean) {
    val level = state.buildingLevel(definition.id)
    val quantity = multiplier.quantity ?: maxAffordableQuantity(definition, level, state.gold, state)
    val cost = bulkBuildingCost(definition, level, quantity.coerceAtLeast(1), state)
    val canAfford = quantity > 0 && state.gold >= cost
    val nextMilestone = remember(level) { MILESTONE_LEVELS.firstOrNull { it > level } }
    val milestoneProgress = remember(level, nextMilestone) {
        if (nextMilestone == null) 1f else {
            val prevMilestone = MILESTONE_LEVELS.lastOrNull { it <= level } ?: 0
            val span = (nextMilestone - prevMilestone).coerceAtLeast(1)
            ((level - prevMilestone).toFloat() / span).coerceIn(0f, 1f)
        }
    }

    val shakeX = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(definition.id) {
        AppGraph.engine.events.collect { event ->
            when {
                event is GameEvent.PurchaseSucceeded && event.buildingId == definition.id -> {
                    flash.snapTo(1f)
                    flash.animateTo(0f, animationSpec = tween(300))
                }
                event is GameEvent.PurchaseDenied && event.buildingId == definition.id -> {
                    shakeX.snapTo(0f)
                    shakeX.animateTo(8f, tween(40))
                    shakeX.animateTo(-8f, tween(80))
                    shakeX.animateTo(0f, tween(60))
                }
                else -> {}
            }
        }
    }

    val marker = if (canAfford) EmberGold else if (level > 0) EmberPalette.BevelLight else EmberPalette.Panel
    val rowHeight = if (compact) 40.dp else 48.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeX.value.dp)
            .background(lerp(Color.Transparent, EmberPalette.White.copy(alpha = 0.14f), flash.value))
            .clickable { AppGraph.engine.buy(definition.id, multiplier.quantity) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(rowHeight).background(marker))
        if (!compact) {
            Text(
                "×$level",
                color = if (level > 0) EmberGold else LockedTextColor,
                fontSize = 11.sp,
                fontFamily = pixelFontFamily(),
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp).width(34.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (compact) 10.dp else 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (compact) "$level ${stringResource(definition.nameRes)}" else stringResource(definition.nameRes),
                    color = if (level > 0) EmberGoldBright else EmberDim,
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    formatAmount(cost),
                    color = if (canAfford) EmberGold else EmberDim,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                )
            }
            if (compact) {
                ThinProgressTrack(
                    progress = milestoneProgress,
                    filledColor = if (canAfford) EmberGold else EmberAccent,
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(3.dp),
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThinProgressTrack(
                        progress = milestoneProgress,
                        filledColor = if (canAfford) EmberGold else EmberAccent,
                        modifier = Modifier.weight(1f).height(3.dp),
                    )
                    Text(
                        "${(milestoneProgress * 100).toInt()}%",
                        color = EmberDim,
                        fontSize = 9.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 10.dp).width(52.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedBuildingRow(definition: BuildingDefinition, compact: Boolean) {
    val rowHeight = if (compact) 40.dp else 48.dp
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(rowHeight).background(EmberPalette.Panel))
        if (!compact) Box(modifier = Modifier.width(46.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (compact) 10.dp else 0.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(definition.nameRes),
                color = LockedTextColor,
                fontSize = if (compact) 11.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(AgeDefinition.all[definition.unlockAgeIndex].nameRes),
                color = LockedTextColor,
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Reich cards (Verfall / Akademie / Dynastie / Erfolge / Regeln & System)
// ---------------------------------------------------------------------------------------------

private fun Modifier.reichCardShell(shakeX: Float = 0f, flash: Float = 0f): Modifier = this
    .fillMaxWidth()
    .offset(x = shakeX.dp)
    .background(lerp(EmberBackground, EmberPalette.White, flash))
    .border(2.dp, EmberPalette.Panel)
    .padding(horizontal = 14.dp, vertical = 12.dp)

/** The single card shape reused by every Reich-tab entry: dot+title left, meta right, a
 * description line, then a [footer] slot (buy bar+button, a plain label, or nothing). */
@Composable
private fun ReichCardBlock(
    title: String,
    meta: String?,
    description: String,
    modifier: Modifier = Modifier,
    dotColor: Color = EmberGold,
    shakeX: Float = 0f,
    flash: Float = 0f,
    footer: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.reichCardShell(shakeX = shakeX, flash = flash)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Box(modifier = Modifier.size(9.dp).background(dotColor))
                Text(
                    title,
                    color = EmberGold,
                    fontSize = 11.sp,
                    fontFamily = pixelFontFamily(),
                    letterSpacing = 0.06f.em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            if (meta != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(meta, color = EmberDim, fontSize = 10.sp, fontFamily = pixelFontFamily(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(description, color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), lineHeight = 1.8.em, modifier = Modifier.padding(top = 9.dp))
        Box(modifier = Modifier.padding(top = 11.dp)) { footer() }
    }
}

@Composable
private fun ReichCardBuyFooter(progress: Float, buttonLabel: String, buttonEnabled: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PixelSegmentedBar(
            progress = progress,
            segments = 14,
            filledColor = EmberGold,
            emptyColor = EmberPalette.Panel,
            modifier = Modifier.weight(1f).height(8.dp),
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .pixelFrame(
                    fill = if (buttonEnabled) EmberAccent else EmberPanel,
                    bevelLight = if (buttonEnabled) EmberAccentBright else EmberPalette.BevelLight,
                )
                // Always enabled — see the equivalent comment on BuildingRow's buy click: the
                // engine itself decides eligibility and fires the deny event for shake feedback.
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(buttonLabel, color = if (buttonEnabled) EmberWhite else EmberGold, fontSize = 9.sp, fontFamily = pixelFontFamily(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** REICH tab: currency grid, Blight explainer, then one card per system (Akademie, Dynastie,
 * Ratssaal, Wunder, Aufträge, Erbe) — all sharing [ReichCardBlock]'s visual pattern. */
@Composable
private fun RealmTabContent(state: GameState) {
    Text(stringResource(Res.string.currencies_label), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.1f.em)
    Spacer(modifier = Modifier.height(10.dp))

    val cells = remember(state) {
        listOf(
            CurrencyCellData(formatAmount(state.chroniclePoints), Res.string.currency_abbr_chronicle, Ramps.PaperLight, ringHighlight = true),
            CurrencyCellData(formatAmount(state.sagen), Res.string.currency_abbr_legends, Ramps.ArcaneLight, ringHighlight = false),
            CurrencyCellData(formatAmount(state.einfluss), Res.string.currency_abbr_influence, Ramps.ClothLight, ringHighlight = false),
            CurrencyCellData("${state.unlockedAchievements.size}/${AchievementDefinition.all.size}", Res.string.currency_abbr_achievements, EmberGold, ringHighlight = false),
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.forEach { cell ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(EmberBackground)
                    .border(2.dp, if (cell.ringHighlight) EmberPalette.BevelLight else EmberPalette.Panel)
                    .padding(horizontal = 10.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.size(9.dp).background(cell.dot))
                Text(
                    cell.value,
                    color = EmberGoldBright,
                    fontSize = 13.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 9.dp),
                )
                Text(stringResource(cell.abbrRes), color = EmberDim, fontSize = 8.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.08f.em, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }

    Text(
        stringResource(Res.string.verfall_description),
        color = EmberDim,
        fontSize = 10.sp,
        fontFamily = pixelFontFamily(),
        lineHeight = 1.8.em,
        modifier = Modifier.padding(top = 16.dp),
    )

    Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AcademyReichCard(state)
        DynastyReichCard(state)
        RatssaalReichCard(state)
        WonderDefinition.all.forEach { wonder -> WonderReichCard(state, wonder) }
        QuestsReichCards(state)
        HeirReichCard(state)
    }
}

@Composable
private fun AcademyReichCard(state: GameState) {
    val cost = academyUpgradeCost(state.academyLevel)
    val affordable = state.chroniclePoints >= cost
    val shakeX = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        AppGraph.engine.events.collect { event ->
            when (event) {
                GameEvent.AcademyStudySucceeded -> {
                    flash.snapTo(1f)
                    flash.animateTo(0f, animationSpec = tween(300))
                }
                GameEvent.AcademyStudyDenied -> {
                    shakeX.snapTo(0f)
                    shakeX.animateTo(8f, tween(40))
                    shakeX.animateTo(-8f, tween(80))
                    shakeX.animateTo(0f, tween(60))
                }
                else -> {}
            }
        }
    }
    ReichCardBlock(
        title = stringResource(Res.string.academy_title).uppercase(),
        meta = stringResource(Res.string.academy_level_label, state.academyLevel, formatAmount((academyMultiplier(state) - 1.0) * 100.0)),
        description = stringResource(Res.string.academy_card_description),
        shakeX = shakeX.value,
        flash = flash.value,
    ) {
        ReichCardBuyFooter(
            progress = (state.chroniclePoints / cost).toFloat().coerceIn(0f, 1f),
            buttonLabel = stringResource(Res.string.academy_study_button).uppercase(),
            buttonEnabled = affordable,
            onClick = { AppGraph.engine.buyAcademyUpgrade() },
        )
    }
}

@Composable
private fun DynastyReichCard(state: GameState) {
    // Ticks once a second purely to recompute the switch cooldown countdown below — the rest of
    // the card only depends on `state`, which already recomposes on its own.
    var now by remember { mutableStateOf(nowEpochSeconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = nowEpochSeconds()
        }
    }
    val cooldownRemaining = (state.lastDynastySwitchEpochSeconds + DYNASTY_SWITCH_COOLDOWN_SECONDS - now).coerceAtLeast(0L)

    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        AppGraph.engine.events.filterIsInstance<GameEvent.DynastySwitchDenied>().collect {
            shakeX.snapTo(0f)
            shakeX.animateTo(8f, tween(40))
            shakeX.animateTo(-8f, tween(80))
            shakeX.animateTo(0f, tween(60))
        }
    }

    Column(modifier = Modifier.reichCardShell(shakeX = shakeX.value)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Box(modifier = Modifier.size(9.dp).background(EmberGold))
                Text(
                    stringResource(Res.string.dynasty_title).uppercase(),
                    color = EmberGold,
                    fontSize = 11.sp,
                    fontFamily = pixelFontFamily(),
                    letterSpacing = 0.06f.em,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (cooldownRemaining > 0) stringResource(Res.string.dynasty_switch_cooldown_hint, formatCooldown(cooldownRemaining)) else stringResource(Res.string.dynasty_switch_hint),
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DynastyPath.entries.forEach { path -> DynastyPathRow(state, path, cooldownActive = cooldownRemaining > 0) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .pixelEdgeLine(PixelEdge.Top)
                .padding(top = 10.dp),
        ) {
            val canTrigger = state.verfallCount >= MIN_VERFALL_COUNT_FOR_WIEDERGEBURT
            if (canTrigger) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PixelButton(
                        onClick = { AppGraph.engine.triggerWiedergeburt() },
                        bevelLight = EmberAccentBright,
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                        Text(stringResource(Res.string.wiedergeburt_button), fontFamily = pixelFontFamily(), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                        Text(stringResource(Res.string.reward_yield_label), color = EmberDim, fontSize = 9.sp)
                        Text(
                            formatAmount(sagenForWiedergeburt(state.lifetimeChroniclePoints)),
                            color = EmberGoldBright,
                            fontSize = 12.sp,
                            fontFamily = pixelFontFamily(),
                        )
                    }
                }
            } else {
                Text(
                    stringResource(Res.string.wiedergeburt_locked_hint, MIN_VERFALL_COUNT_FOR_WIEDERGEBURT, state.verfallCount),
                    color = LockedTextColor,
                    fontSize = 9.sp,
                    fontFamily = pixelFontFamily(),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 1.6.em,
                )
            }
        }
    }
}

@Composable
private fun RatssaalReichCard(state: GameState) {
    Column(modifier = Modifier.reichCardShell()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Box(modifier = Modifier.size(9.dp).background(EmberGold))
                Text(
                    stringResource(Res.string.ratssaal_title).uppercase(),
                    color = EmberGold,
                    fontSize = 11.sp,
                    fontFamily = pixelFontFamily(),
                    letterSpacing = 0.06f.em,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("${state.ratssaalUpgrades.size}/${RatssaalUpgradeDefinition.all.size}", color = EmberDim, fontSize = 10.sp, fontFamily = pixelFontFamily())
        }
        Text(
            stringResource(Res.string.ratssaal_card_description),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            lineHeight = 1.8.em,
            modifier = Modifier.padding(top = 9.dp),
        )
        Column(modifier = Modifier.padding(top = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RatssaalUpgradeDefinition.all.forEach { upgrade -> RatssaalUpgradeRow(state, upgrade) }
        }
    }
}

@Composable
private fun WonderReichCard(state: GameState, wonder: WonderDefinition) {
    val owned = wonder.id in state.ownedWonders
    val unlocked = state.currentAge.index >= wonder.unlockAgeIndex
    val affordable = state.gold >= wonder.cost

    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(wonder.id) {
        AppGraph.engine.events.collect { event ->
            if (event is GameEvent.WonderBuildDenied && event.id == wonder.id) {
                shakeX.snapTo(0f)
                shakeX.animateTo(8f, tween(40))
                shakeX.animateTo(-8f, tween(80))
                shakeX.animateTo(0f, tween(60))
            }
        }
    }

    ReichCardBlock(
        title = stringResource(wonder.nameRes).uppercase(),
        meta = when {
            owned -> stringResource(Res.string.wonder_built_label)
            !unlocked -> stringResource(AgeDefinition.all[wonder.unlockAgeIndex].nameRes)
            else -> formatAmount(wonder.cost)
        },
        description = stringResource(wonder.descriptionRes),
        dotColor = if (owned) EmberGoldBright else if (unlocked) EmberGold else EmberPalette.Panel,
        shakeX = shakeX.value,
    ) {
        if (!owned && unlocked) {
            ReichCardBuyFooter(
                progress = (state.gold / wonder.cost).toFloat().coerceIn(0f, 1f),
                buttonLabel = stringResource(Res.string.wonder_build_button, formatAmount(wonder.cost)).uppercase(),
                buttonEnabled = affordable,
                onClick = { AppGraph.engine.buyWonder(wonder.id) },
            )
        }
    }
}

@Composable
private fun QuestsReichCards(state: GameState) {
    val openQuests = remember(state.claimedQuestIds) { QuestDefinition.all.filter { it.id !in state.claimedQuestIds }.take(3) }
    if (openQuests.isEmpty()) {
        Text(
            stringResource(Res.string.quests_empty_hint),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            fontStyle = FontStyle.Italic,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            openQuests.forEach { quest -> QuestReichCard(state, quest) }
        }
    }
}

@Composable
private fun QuestReichCard(state: GameState, quest: QuestDefinition) {
    val fulfilled = quest.condition(state)
    ReichCardBlock(
        title = stringResource(quest.nameRes).uppercase(),
        meta = null,
        description = "${stringResource(Res.string.quest_reward_label)}: ${formatAmount(quest.rewardGold)} ${stringResource(Res.string.gold_label)}",
        dotColor = if (fulfilled) EmberGoldBright else EmberPalette.Panel,
    ) {
        ReichCardBuyFooter(
            progress = if (fulfilled) 1f else 0f,
            buttonLabel = stringResource(Res.string.quest_claim_button).uppercase(),
            buttonEnabled = fulfilled,
            onClick = { AppGraph.engine.claimQuest(quest.id) },
        )
    }
}

/** A small heraldic block naming the current life's Heir — rolled fresh at each Wiedergeburt. */
@Composable
private fun HeirReichCard(state: GameState) {
    val heirId = state.currentHeirId
    if (heirId == null) {
        ReichCardBlock(
            title = stringResource(Res.string.heir_card_title).uppercase(),
            meta = null,
            description = stringResource(Res.string.heir_none_hint),
        )
    } else {
        val trait = HeirTraitDefinition.byId(heirId)
        ReichCardBlock(
            title = state.currentHeirName.uppercase(),
            meta = stringResource(trait.nameRes),
            description = stringResource(trait.descriptionRes),
            dotColor = EmberGoldBright,
        )
    }
}

@Composable
private fun DynastyPathRow(state: GameState, path: DynastyPath, cooldownActive: Boolean) {
    val level = state.legacyLevels[path] ?: 0
    val isActive = state.activeDynastyPath == path
    val cost = legacyUpgradeCost(level)

    Row(
        modifier = Modifier.fillMaxWidth().background(if (isActive) EmberPanelLight else EmberHeaderBar).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed height, not fillMaxHeight: this Row sits inside a verticalScroll ancestor, which
        // measures its content with an unbounded max height — fillMaxHeight there resolves to
        // that unbounded height and blows the row up, wrapping the sibling name text one
        // character per line to fit the near-zero width left over.
        Box(modifier = Modifier.width(3.dp).height(34.dp).background(if (isActive) EmberGold else Color.Transparent))
        Column(modifier = Modifier.weight(1f, fill = true).padding(start = 8.dp)) {
            Text(
                stringResource(path.nameRes),
                color = if (isActive) EmberGoldBright else EmberGold.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.dynasty_path_level_label, level, formatAmount(path.bonusPerLevel * level * 100.0)),
                color = EmberDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (isActive) {
            val affordable = state.sagen >= cost
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .pixelFrame(
                        fill = if (affordable) EmberAccent else EmberAccent.copy(alpha = 0.35f),
                        bevelLight = EmberAccentBright,
                    )
                    .clickable(enabled = affordable) { AppGraph.engine.buyLegacyUpgrade(path) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatAmount(cost)} ${stringResource(Res.string.legends_label)}",
                    color = EmberWhite,
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .pixelFrame(fill = if (cooldownActive) EmberPanelLight.copy(alpha = 0.5f) else EmberPanelLight)
                    // Always enabled — see the equivalent comment on BuildingRow's buy click;
                    // the engine itself gates the cooldown and fires a deny event/shake.
                    .clickable { AppGraph.engine.setActiveDynastyPath(path) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.dynasty_path_activate_button),
                    color = if (cooldownActive) EmberDim else EmberGold,
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun RatssaalUpgradeRow(state: GameState, upgrade: RatssaalUpgradeDefinition) {
    val owned = upgrade.id in state.ratssaalUpgrades
    val affordable = state.einfluss >= upgrade.cost

    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(upgrade.id) {
        AppGraph.engine.events.collect { event ->
            if (event is GameEvent.RatssaalUpgradeDenied && event.id == upgrade.id) {
                shakeX.snapTo(0f)
                shakeX.animateTo(8f, tween(40))
                shakeX.animateTo(-8f, tween(80))
                shakeX.animateTo(0f, tween(60))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().offset(x = shakeX.value.dp).background(EmberHeaderBar).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(upgrade.nameRes),
                color = if (owned) EmberGoldBright else EmberGold.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(upgrade.descriptionRes),
                color = EmberDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (owned) {
            Text(stringResource(Res.string.ratssaal_owned_label), color = EmberDim, fontSize = 10.sp, fontFamily = pixelFontFamily())
        } else {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .pixelFrame(
                        fill = if (affordable) EmberAccent else EmberAccent.copy(alpha = 0.35f),
                        bevelLight = EmberAccentBright,
                    )
                    .clickable { AppGraph.engine.buyRatssaalUpgrade(upgrade.id) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.ratssaal_buy_button, formatAmount(upgrade.cost)),
                    color = EmberWhite,
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Resolves one [ChronicleEntry] into display text, looking up nested resources (achievement/age names) by id. */
@Composable
private fun chronicleEntryText(entry: ChronicleEntry): String {
    val type = ChronicleEntryType.byKind(entry.kind)
    val args = when (entry.kind) {
        "achievement_unlocked" -> listOf(stringResource(AchievementDefinition.byId(entry.args[0]).nameRes))
        "age_advanced" -> listOf(stringResource(AgeDefinition.all[entry.args[0].toInt()].nameRes))
        else -> entry.args
    }
    return stringResource(type.templateRes, *args.toTypedArray())
}

/** ERFOLGE tab: a 2-column achievements grid, then the Chronicle entry list below it. */
@Composable
private fun AchievementsTabContent(state: GameState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(stringResource(Res.string.reign_tab_achievements).uppercase(), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.1f.em)
        Text(
            "${state.unlockedAchievements.size}/${AchievementDefinition.all.size} · +${formatAmount((achievementMultiplier(state) - 1.0) * 100.0)}%",
            color = EmberGoldBright,
            fontSize = 10.sp,
            fontFamily = pixelFontFamily(),
        )
    }
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        AchievementDefinition.all.chunked(2).forEach { rowDefs ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                rowDefs.forEach { achievement ->
                    val unlocked = achievement.id in state.unlockedAchievements
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(EmberBackground)
                            .border(2.dp, if (unlocked) EmberPalette.BevelLight else EmberPalette.Panel)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(9.dp).background(if (unlocked) EmberGold else EmberPalette.Panel))
                        Text(
                            stringResource(achievement.nameRes),
                            color = if (unlocked) EmberGold else LockedTextColor,
                            fontSize = 9.sp,
                            fontFamily = pixelFontFamily(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 9.dp),
                        )
                    }
                }
                repeat(2 - rowDefs.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
    Text(
        stringResource(Res.string.chronicle_card_title).uppercase(),
        color = EmberDim,
        fontSize = 9.sp,
        fontFamily = pixelFontFamily(),
        letterSpacing = 0.1f.em,
        modifier = Modifier.padding(top = 16.dp),
    )
    if (state.chronicle.isEmpty()) {
        Text(
            stringResource(Res.string.chronicle_empty_hint),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 11.dp),
        )
    } else {
        Column(modifier = Modifier.padding(top = 11.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            state.chronicle.asReversed().take(12).forEach { entry ->
                Row {
                    Box(modifier = Modifier.padding(top = 4.dp).size(7.dp).background(EmberAccent))
                    Text(
                        chronicleEntryText(entry),
                        color = EmberDim,
                        fontSize = 9.sp,
                        fontFamily = pixelFontFamily(),
                        lineHeight = 1.8.em,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** SYSTEM tab: language, offline/mute switches, volume, save/export/import, version. */
@Composable
private fun SystemTabContent(state: GameState) {
    var exportExpanded by remember { mutableStateOf(false) }
    var importExpanded by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val uiSettings by AppGraph.uiSettings.state.collectAsState()

    Text(stringResource(Res.string.system_language_label).uppercase(), color = EmberDim, fontSize = 9.sp, fontFamily = pixelFontFamily(), letterSpacing = 0.1f.em)
    LanguageChips(
        selected = uiSettings.language,
        onSelect = { AppGraph.uiSettings.setLanguage(it) },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )

    Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // weight(1f) is required here, not optional: a wrapping Text with no weight is
            // measured against the Row's full width, leaving nothing for the switch after it.
            Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
                Text(stringResource(Res.string.rules_offline_progress_label), color = EmberGold, fontSize = 10.sp, fontFamily = pixelFontFamily())
                Text(
                    stringResource(Res.string.rules_offline_progress_description),
                    color = EmberDim,
                    fontSize = 9.sp,
                    fontFamily = pixelFontFamily(),
                    lineHeight = 1.7.em,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            PixelSwitch(checked = state.offlineProgressEnabled, onCheckedChange = { AppGraph.engine.setOfflineProgressEnabled(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.system_mute_label), color = EmberGold, fontSize = 10.sp, fontFamily = pixelFontFamily(), modifier = Modifier.weight(1f))
            PixelSwitch(checked = uiSettings.muted, onCheckedChange = { AppGraph.uiSettings.setMuted(it) })
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.system_volume_label).uppercase(), color = EmberGold, fontSize = 10.sp, fontFamily = pixelFontFamily())
        Text("${(uiSettings.masterVolume * 100).roundToInt()}%", color = EmberGoldBright, fontSize = 10.sp, fontFamily = pixelFontFamily())
    }
    VolumeBar(
        volume = uiSettings.masterVolume,
        onVolumeChange = { AppGraph.uiSettings.setMasterVolume(it) },
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(12.dp),
    )

    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SecondaryPixelButton(label = stringResource(Res.string.system_save_button_short), modifier = Modifier.weight(1f)) {
            AppGraph.engine.persistNow()
        }
        SecondaryPixelButton(label = stringResource(Res.string.system_export_button), modifier = Modifier.weight(1f)) {
            exportExpanded = !exportExpanded
            importExpanded = false
        }
        SecondaryPixelButton(label = stringResource(Res.string.system_import_button_short), modifier = Modifier.weight(1f)) {
            importExpanded = !importExpanded
            exportExpanded = false
        }
    }

    if (exportExpanded) {
        SelectionContainer {
            Text(
                AppGraph.engine.exportSave(),
                color = EmberGold.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 140.dp)
                    .background(EmberBackground)
                    .padding(10.dp),
            )
        }
    }

    if (importExpanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it; importMessage = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                textStyle = TextStyle(fontSize = 11.sp, color = EmberGold),
            )
            PixelButton(
                onClick = {
                    val success = AppGraph.engine.importSave(importText)
                    importMessage = if (success) null else "error"
                    if (success) {
                        importText = ""
                        importExpanded = false
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(Res.string.system_import_button), fontFamily = pixelFontFamily())
            }
            if (importMessage == "error") {
                Text(stringResource(Res.string.system_import_error), color = EmberAccent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }

    Text(
        "${stringResource(Res.string.app_name)} ${BuildInfo.VERSION}",
        color = LockedTextColor,
        fontSize = 9.sp,
        fontFamily = pixelFontFamily(),
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun SecondaryPixelButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, animationSpec = tween(80))
    Box(
        modifier = modifier
            .scale(scale)
            .height(36.dp)
            .pixelFrame(fill = EmberPanelLight)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = EmberGold, fontSize = 9.sp, fontFamily = pixelFontFamily(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------------------------
// Small shared atoms
// ---------------------------------------------------------------------------------------------

private data class CurrencyCellData(val value: String, val abbrRes: StringResource, val dot: Color, val ringHighlight: Boolean)
