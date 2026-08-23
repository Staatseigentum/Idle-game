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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.embercrown.game.resources.academy_level_label
import com.embercrown.game.resources.academy_study_button
import com.embercrown.game.resources.academy_title
import com.embercrown.game.resources.academy_upgrade_button
import com.embercrown.game.resources.ages_title
import com.embercrown.game.resources.app_name
import com.embercrown.game.resources.buildings_title
import com.embercrown.game.resources.building_synergy_hint
import com.embercrown.game.resources.buy_multiplier_label
import com.embercrown.game.resources.chronicle_card_title
import com.embercrown.game.resources.chronicle_empty_hint
import com.embercrown.game.resources.chronicle_points_label
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
import com.embercrown.game.resources.quest_claim_button
import com.embercrown.game.resources.quest_reward_label
import com.embercrown.game.resources.quests_empty_hint
import com.embercrown.game.resources.quests_title
import com.embercrown.game.resources.ratssaal_buy_button
import com.embercrown.game.resources.ratssaal_owned_label
import com.embercrown.game.resources.ratssaal_title
import com.embercrown.game.resources.reign_tab_achievements
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
                        if (maxWidth >= 700.dp) {
                            DesktopLayout(state = state, multiplier = multiplier, onMultiplierChange = { multiplier = it })
                        } else {
                            PhoneLayout(state = state, multiplier = multiplier, onMultiplierChange = { multiplier = it })
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
private fun PhoneLayout(state: GameState, multiplier: BuyMultiplier, onMultiplierChange: (BuyMultiplier) -> Unit) {
    var destination by remember { mutableStateOf(PhoneDestination.HOLDINGS) }

    Column(modifier = Modifier.fillMaxSize()) {
        CurrencyHeaderPhone(state)
        UpdateBanner(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        SceneBanner(
            state = state,
            height = 132.dp,
            nameFontSize = 15.sp,
            showChronicle = false,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        ProgressRow(state = state, horizontalPadding = 12.dp, verticalPadding = 9.dp, showAbsoluteValues = false)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (destination) {
                PhoneDestination.HOLDINGS ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        BuildingsSection(state = state, multiplier = multiplier, onMultiplierChange = onMultiplierChange, columns = 1)
                    }
                PhoneDestination.REIGN ->
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuestsCard(state)
                        VerfallCard(state)
                        AcademyCard(state)
                        DynastyCard(state)
                        HeirCard(state)
                        RatssaalCard(state)
                        WondersCard(state)
                        ChronicleCard(state)
                    }
                PhoneDestination.ACHIEVEMENTS ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        AchievementsBlock(state)
                    }
                PhoneDestination.SYSTEM ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        RulesSystemBlock(state)
                    }
            }
        }

        FooterNav(selected = destination, onSelect = { destination = it }, state = state)
    }
}

@Composable
private fun CurrencyHeaderPhone(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmberHeaderBar)
            .pixelEdgeLine(PixelEdge.Bottom)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelArtImage(remember { goldIcon() }, modifier = Modifier.size(26.dp))
                Text(formatAmount(state.gold), color = EmberGold, fontSize = 20.sp, fontFamily = pixelFontFamily(), modifier = Modifier.padding(start = 8.dp))
            }
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
                Text("+${formatAmount(totalProduction(state))}/s", color = EmberGoldBright, fontSize = 11.sp, fontFamily = pixelFontFamily())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconValueLabel(
                icon = { PixelArtImage(remember { chroniclePointsIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 16.dp,
                value = formatAmount(state.chroniclePoints),
                valueColor = EmberGold.copy(alpha = 0.85f),
                valueFontSize = 12.sp,
                label = stringResource(Res.string.chronicle_points_label),
                labelFontSize = 11.sp,
                modifier = Modifier.padding(end = 14.dp),
            )
            IconValueLabel(
                icon = { PixelArtImage(remember { sagenIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 16.dp,
                value = formatAmount(state.sagen),
                valueColor = EmberGold.copy(alpha = 0.85f),
                valueFontSize = 12.sp,
                label = stringResource(Res.string.legends_label),
                labelFontSize = 11.sp,
                modifier = Modifier.padding(end = 14.dp),
            )
            IconValueLabel(
                icon = { PixelArtImage(remember { einflussIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 16.dp,
                value = formatAmount(state.einfluss),
                valueColor = EmberGold.copy(alpha = 0.85f),
                valueFontSize = 12.sp,
                label = stringResource(Res.string.einfluss_label),
                labelFontSize = 11.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            ValueLabel(
                value = "${state.unlockedAchievements.size}/${AchievementDefinition.all.size}",
                label = stringResource(Res.string.reign_tab_achievements),
                valueColor = EmberGold.copy(alpha = 0.85f),
                valueFontSize = 12.sp,
                labelFontSize = 11.sp,
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
                    .height(56.dp)
                    .let { if (index < PhoneDestination.entries.lastIndex) it.pixelEdgeLine(PixelEdge.End) else it }
                    .background(if (isSelected) EmberPanelLight else Color.Transparent)
                    .clickable { onSelect(destination) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(destination.labelRes).uppercase(),
                    color = if (isSelected) EmberGoldBright else EmberGold.copy(alpha = 0.8f),
                    fontSize = 10.sp,
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
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Desktop (>= 700 dp)
// ---------------------------------------------------------------------------------------------

@Composable
private fun DesktopLayout(state: GameState, multiplier: BuyMultiplier, onMultiplierChange: (BuyMultiplier) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CurrencyHeaderDesktop(state)
        UpdateBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AgesRail(state = state, modifier = Modifier.width(196.dp).fillMaxHeight())

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SceneBanner(
                    state = state,
                    height = 220.dp,
                    nameFontSize = 18.sp,
                    showChronicle = true,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                ProgressRow(state = state, horizontalPadding = 16.dp, verticalPadding = 10.dp, showAbsoluteValues = true)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BuildingsSection(state = state, multiplier = multiplier, onMultiplierChange = onMultiplierChange, columns = 2)
                }
            }

            ReichColumn(state = state, modifier = Modifier.width(336.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun CurrencyHeaderDesktop(state: GameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(EmberHeaderBar)
            .pixelEdgeLine(PixelEdge.Bottom)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            stringResource(Res.string.app_name).uppercase(),
            color = EmberAccent,
            fontSize = 12.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.06f.em,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelArtImage(remember { goldIcon() }, modifier = Modifier.size(28.dp))
            Text(formatAmount(state.gold), color = EmberGold, fontSize = 20.sp, fontFamily = pixelFontFamily(), modifier = Modifier.padding(start = 8.dp))
            Text(
                "+${formatAmount(totalProduction(state))}/s",
                color = EmberGoldBright,
                fontSize = 11.sp,
                fontFamily = pixelFontFamily(),
                modifier = Modifier.padding(start = 4.dp),
            )
            if (state.dragonBuffTicksRemaining > 0) {
                Box(modifier = Modifier.padding(start = 10.dp).pixelFrame(fill = EmberAccent, bevelLight = EmberAccentBright)) {
                    Text(
                        stringResource(Res.string.dragon_buff_badge, state.dragonBuffTicksRemaining),
                        color = EmberWhite,
                        fontSize = 10.sp,
                        fontFamily = pixelFontFamily(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconValueLabel(
                icon = { PixelArtImage(remember { chroniclePointsIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 20.dp,
                value = formatAmount(state.chroniclePoints),
                valueColor = EmberGold,
                valueFontSize = 13.sp,
                label = stringResource(Res.string.chronicle_points_label),
                labelFontSize = 11.sp,
            )
            IconValueLabel(
                icon = { PixelArtImage(remember { sagenIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 20.dp,
                value = formatAmount(state.sagen),
                valueColor = EmberGold,
                valueFontSize = 13.sp,
                label = stringResource(Res.string.legends_label),
                labelFontSize = 11.sp,
            )
            IconValueLabel(
                icon = { PixelArtImage(remember { einflussIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 20.dp,
                value = formatAmount(state.einfluss),
                valueColor = EmberGold,
                valueFontSize = 13.sp,
                label = stringResource(Res.string.einfluss_label),
                labelFontSize = 11.sp,
            )
            ValueLabel(
                value = "${state.unlockedAchievements.size}/${AchievementDefinition.all.size}",
                label = stringResource(Res.string.reign_tab_achievements),
                valueColor = EmberGold,
                valueFontSize = 13.sp,
                labelFontSize = 11.sp,
            )
            ValueLabel(
                value = state.verfallCount.toString(),
                label = stringResource(Res.string.verfalls_label),
                valueColor = EmberGold,
                valueFontSize = 13.sp,
                labelFontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun AgesRail(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(EmberRail).pixelEdgeLine(PixelEdge.End)) {
        Text(
            stringResource(Res.string.ages_title).uppercase(),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.08f.em,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .padding(bottom = 10.dp),
        ) {
            AgeDefinition.all.forEach { age -> AgeRailRow(age = age, state = state) }
        }
    }
}

@Composable
private fun AgeRailRow(age: AgeDefinition, state: GameState) {
    val current = age.index == state.currentAge.index
    val reached = state.lifetimeGold >= age.lifetimeGoldThreshold
    val background = if (current) EmberPanelLight else Color.Transparent
    val markColor = if (current) EmberGold else if (reached) EmberPalette.BevelLight else Color.Transparent
    val nameColor = if (current) EmberGoldBright else if (reached) EmberGold.copy(alpha = 0.75f) else LockedTextColor
    val numColor = if (current) EmberGold else LockedTextColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(bottom = 2.dp)
            .background(background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(markColor))
        Row(modifier = Modifier.weight(1f).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text((age.index + 1).toString().padStart(2, '0'), color = numColor, fontSize = 8.sp, fontFamily = pixelFontFamily(), modifier = Modifier.width(16.dp))
            Text(stringResource(age.nameRes), color = nameColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReichColumn(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(EmberRail).pixelEdgeLine(PixelEdge.Start)) {
        Text(
            stringResource(Res.string.reign_title).uppercase(),
            color = EmberDim,
            fontSize = 9.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.08f.em,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuestsCard(state)
            VerfallCard(state)
            AcademyCard(state)
            DynastyCard(state)
            HeirCard(state)
            RatssaalCard(state)
            WondersCard(state)
            ChronicleCard(state)
            AchievementsBlock(state)
            RulesSystemBlock(state)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared: scene banner + progress row
// ---------------------------------------------------------------------------------------------

private data class FloatingGoldNumber(val id: Int, val amount: Double)

@Composable
private fun SceneBanner(
    state: GameState,
    height: Dp,
    nameFontSize: TextUnit,
    showChronicle: Boolean,
    modifier: Modifier = Modifier,
) {
    val age = state.currentAge
    val sidePadding = if (showChronicle) 16.dp else 12.dp
    val bottomPadding = if (showChronicle) 14.dp else 10.dp

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
        // Capped, not fillMaxWidth() alone: the age-scene art is a low-res pixel grid (~33x15)
        // designed around a ~2.2:1 aspect ratio. On a maximized/ultra-wide window the middle
        // column can get far wider than that, and PixelFit.Cover would then blow individual
        // pixel blocks up hugely while cropping most of the vertical composition away to fill
        // it — capping the banner's own width keeps it at a sane, designed crop regardless of
        // window width, with the surrounding column background framing it instead.
        modifier = modifier
            // widthIn BEFORE fillMaxWidth, not after: fillMaxWidth first would report a tight,
            // already-fixed size to its parent that a later widthIn can no longer shrink — the
            // cap only works as the outer (first) constraint, narrowing what fillMaxWidth then
            // fills completely.
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .height(height)
            .pixelEdgeLine(PixelEdge.Bottom)
            .clipToBounds()
            .clickable { onTap() },
    ) {
        // Crossfade, not a direct swap: the age scene art changes shape/palette a lot between
        // ages (see AgeScene's dawn-brown -> radiant-gold ramp), so an instant cut on age-up
        // reads as a jarring flash rather than the world visibly aging.
        // Bucketed into 40 steps rather than read as a raw Double: corruption crawls over 25
        // minutes, so re-rasterizing the full 160x72 grid every second (the tick interval) would
        // be wasted work for a change too small to see — this redraws only every ~40s of ramp.
        val corruptionBucket = (state.corruption * 40).roundToInt()
        Crossfade(targetState = age.index, animationSpec = tween(600), modifier = Modifier.fillMaxSize()) { index ->
            PixelArtImage(
                art = remember(index, corruptionBucket) {
                    val base = ageSceneArt(index, AgeDefinition.all.size)
                    if (corruptionBucket <= 0) base else base.corrupted(corruptionBucket / 40f)
                },
                modifier = Modifier.fillMaxSize(),
                fit = PixelFit.Cover,
            )
        }

        Box(modifier = Modifier.align(Alignment.Center)) {
            floatingNumbers.forEach { number ->
                FloatingGoldNumberText(number = number, onFinished = { floatingNumbers.remove(number) })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (showChronicle) 96.dp else 64.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to EmberBackground.copy(alpha = if (showChronicle) 0.8f else 0.82f),
                        1f to EmberBackground.copy(alpha = 0.97f),
                    ),
                ),
        )

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = sidePadding, bottom = bottomPadding).widthIn(max = 560.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Box(modifier = Modifier.pixelFrame(fill = EmberBackground.copy(alpha = 0.85f), borderWidth = 2.dp)) {
                    Text(
                        "${age.index + 1}/${AgeDefinition.all.size}",
                        color = EmberGold,
                        fontSize = 9.sp,
                        fontFamily = pixelFontFamily(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                Text(
                    stringResource(age.nameRes),
                    color = EmberGoldBright,
                    fontSize = nameFontSize,
                    fontFamily = pixelFontFamily(),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (showChronicle) {
                Text(
                    stringResource(age.chronicleRes),
                    color = EmberGold.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Drachenüberflug: a tappable sighting that appears briefly and, if tapped in time,
        // activates a timed production buff. Its own clickable consumes the tap so it doesn't
        // also fall through to the click-for-gold handler on the Box below it.
        if (state.dragonAvailableTicksRemaining > 0) {
            val pulse = rememberInfiniteTransition(label = "dragonPulse")
            val pulseScale by pulse.animateFloat(
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
                    .scale(pulseScale)
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
            val pulse = rememberInfiniteTransition(label = "omenPulse")
            val pulseScale by pulse.animateFloat(
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
                    .scale(pulseScale)
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

@Composable
private fun ProgressRow(state: GameState, horizontalPadding: Dp, verticalPadding: Dp, showAbsoluteValues: Boolean) {
    val next = state.nextAge ?: return
    val progress = (state.lifetimeGold / next.lifetimeGoldThreshold).toFloat().coerceIn(0f, 1f)
    val labelFontSize = if (showAbsoluteValues) 12.sp else 11.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmberHeaderBar)
            .pixelEdgeLine(PixelEdge.Bottom)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            // weight(1f) + maxLines=1/ellipsis: an unweighted wrapping Row here would claim the
            // full row width on a long age name or large numbers, leaving nothing for the "%"
            // that follows — the same trap fixed above in the Dynasty row and the switch row.
            Row(modifier = Modifier.weight(1f)) {
                Text("${stringResource(Res.string.next_age_label)} · ", color = EmberDim, fontSize = labelFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(next.nameRes), color = EmberGold, fontSize = labelFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (showAbsoluteValues) {
                    Text(
                        " · ${formatAmount(state.lifetimeGold)} / ${formatAmount(next.lifetimeGoldThreshold)}",
                        color = EmberDim,
                        fontSize = labelFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                "${(progress * 100).toInt()}%",
                color = EmberGold,
                fontSize = 10.sp,
                fontFamily = pixelFontFamily(),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        PixelSegmentedBar(progress = progress, segments = 20, modifier = Modifier.fillMaxWidth().height(10.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Buildings (shared between the phone single-column list and the desktop 2-col grid)
// ---------------------------------------------------------------------------------------------

@Composable
private fun BuildingsSection(state: GameState, multiplier: BuyMultiplier, onMultiplierChange: (BuyMultiplier) -> Unit, columns: Int) {
    var lockedExpanded by remember { mutableStateOf(false) }
    val unlocked = remember(state) { BuildingDefinition.all.filter { isBuildingUnlocked(it, state) } }
    val locked = remember(state) { BuildingDefinition.all.filter { !isBuildingUnlocked(it, state) } }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.buildings_title).uppercase(),
            color = EmberGold,
            fontSize = 11.sp,
            fontFamily = pixelFontFamily(),
            letterSpacing = 0.04f.em,
        )
        BuyMultiplierChips(selected = multiplier, onSelect = onMultiplierChange)
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (columns == 1) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            unlocked.forEach { definition -> BuildingRow(state, definition, multiplier) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            unlocked.chunked(columns).forEach { rowDefs ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowDefs.forEach { definition -> Box(modifier = Modifier.weight(1f)) { BuildingRow(state, definition, multiplier) } }
                    repeat(columns - rowDefs.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }

    if (locked.isNotEmpty()) {
        if (columns == 1) {
            val nearest = locked.minByOrNull { it.unlockAgeIndex }
            if (!lockedExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .pixelFrame(fill = EmberHeaderBar, raised = false)
                        .clickable { lockedExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.locked_group_label, locked.size), color = EmberDim, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    if (nearest != null) {
                        val nearestAgeName = stringResource(AgeDefinition.all[nearest.unlockAgeIndex].nameRes)
                        Text("${stringResource(Res.string.locked_group_from, nearestAgeName)} ›", color = EmberGold, fontSize = 11.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { lockedExpanded = false },
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    locked.forEach { definition -> LockedBuildingRow(definition) }
                }
            }
        } else {
            Text(
                stringResource(Res.string.locked_section_title).uppercase(),
                color = EmberDim,
                fontSize = 9.sp,
                fontFamily = pixelFontFamily(),
                letterSpacing = 0.08f.em,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                locked.chunked(2).forEach { rowDefs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowDefs.forEach { definition -> Box(modifier = Modifier.weight(1f)) { LockedBuildingRow(definition) } }
                        repeat(2 - rowDefs.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyMultiplierChips(selected: BuyMultiplier, onSelect: (BuyMultiplier) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BuyMultiplier.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .pixelFrame(
                        fill = if (isSelected) EmberAccent else EmberPanel,
                        borderWidth = 2.dp,
                        bevelLight = if (isSelected) EmberAccentBright else EmberPalette.BevelLight,
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            ) {
                Text(option.label, color = if (isSelected) EmberWhite else EmberGold.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = pixelFontFamily())
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
private fun BuildingRow(state: GameState, definition: BuildingDefinition, multiplier: BuyMultiplier) {
    val level = state.buildingLevel(definition.id)
    val quantity = multiplier.quantity ?: maxAffordableQuantity(definition, level, state.gold, state)
    val cost = bulkBuildingCost(definition, level, quantity.coerceAtLeast(1), state)
    val canAfford = quantity > 0 && state.gold >= cost

    val shakeX = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val iconPop = remember { Animatable(1f) }
    LaunchedEffect(definition.id) {
        AppGraph.engine.events.collect { event ->
            when {
                event is GameEvent.PurchaseSucceeded && event.buildingId == definition.id -> {
                    flash.snapTo(1f)
                    // Concurrent, not sequential: the icon pop should play alongside the button
                    // flash below, not wait for its 300ms animateTo to finish first.
                    launch {
                        iconPop.snapTo(1.25f)
                        iconPop.animateTo(1f, animationSpec = tween(200))
                    }
                    flash.animateTo(0f, animationSpec = tween(300))
                }
                event is GameEvent.PurchaseDenied && event.buildingId == definition.id -> {
                    shakeX.snapTo(0f)
                    shakeX.animateTo(8f, tween(40))
                    shakeX.animateTo(-8f, tween(80))
                    shakeX.animateTo(0f, tween(60))
                }
                event is GameEvent.BuildingMilestoneReached && event.buildingId == definition.id -> {
                    // Reuses the purchase flash/pop but bigger — a milestone crossing is a
                    // purchase that also doubled the building's output, not a separate moment.
                    launch {
                        iconPop.snapTo(1.5f)
                        iconPop.animateTo(1f, animationSpec = tween(320))
                    }
                }
            }
        }
    }

    val nextMilestone = remember(level) { MILESTONE_LEVELS.firstOrNull { it > level } }
    val levelsById = remember(state) { state.buildings.associate { it.id to it.level } }
    val incomingSynergies = remember(definition.id, levelsById) {
        SynergyDefinition.forTarget(definition.id).filter { (levelsById[it.sourceId] ?: 0) > 0 }
    }

    Row(
        modifier = Modifier.fillMaxWidth().offset(x = shakeX.value.dp).pixelFrame().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = remember(definition.id) { buildingIcon(definition.id) }
        if (icon != null) PixelArtImage(icon, modifier = Modifier.size(36.dp).scale(iconPop.value))
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(definition.nameRes),
                    color = EmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(EmberPanelLight)
                        .border(1.dp, EmberPalette.Shadow)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text("×$level", color = EmberGoldBright, fontSize = 9.sp, fontFamily = pixelFontFamily())
                }
            }
            Text("${formatAmount(buildingProduction(definition, level, state))}/s", color = EmberDim, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            if (nextMilestone != null) {
                Text(
                    stringResource(Res.string.milestone_hint_label, nextMilestone, formatAmount(milestoneMultiplier(nextMilestone))),
                    color = EmberDim.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            incomingSynergies.forEach { synergy ->
                val sourceLevel = levelsById[synergy.sourceId] ?: 0
                val bonusPct = (synergy.bonusPerLevel * sourceLevel).coerceAtMost(synergy.cap) * 100.0
                Text(
                    stringResource(Res.string.building_synergy_hint, formatAmount(bonusPct), stringResource(BuildingDefinition.byId(synergy.sourceId).nameRes)),
                    color = EmberDim.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .height(44.dp)
                .widthIn(min = 104.dp)
                .pixelFrame(
                    fill = lerp(if (canAfford) EmberAccent else EmberPanel, EmberWhite, flash.value),
                    bevelLight = if (canAfford) EmberAccentBright else EmberPanelLight,
                )
                // Always enabled, not enabled = canAfford: a disabled clickable never invokes
                // onClick, so an unaffordable tap needs to reach the engine for it to emit a
                // PurchaseDenied event (deny sound/shake). The button's affordability styling
                // (fill/text color above) is unaffected by this.
                .clickable { AppGraph.engine.buy(definition.id, multiplier.quantity) }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(Res.string.buy_multiplier_label).uppercase(),
                color = if (canAfford) EmberWhite.copy(alpha = 0.7f) else EmberDim,
                fontSize = 9.sp,
                letterSpacing = 0.06f.em,
            )
            Text(formatAmount(cost), color = if (canAfford) EmberGoldBright else EmberDim, fontSize = 11.sp, fontFamily = pixelFontFamily())
        }
    }
}

@Composable
private fun LockedBuildingRow(definition: BuildingDefinition) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EmberHeaderBar).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = remember(definition.id) { buildingIcon(definition.id) }
        if (icon != null) PixelArtImage(icon, modifier = Modifier.size(26.dp).alpha(0.35f))
        Text(
            stringResource(definition.nameRes),
            color = EmberDim,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        Text(stringResource(AgeDefinition.all[definition.unlockAgeIndex].nameRes), color = LockedTextColor, fontSize = 10.sp, fontStyle = FontStyle.Italic)
    }
}

// ---------------------------------------------------------------------------------------------
// Reich cards (Verfall / Akademie / Dynastie / Erfolge / Regeln & System)
// ---------------------------------------------------------------------------------------------

@Composable
private fun VerfallCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.verfall_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
            Text(stringResource(Res.string.verfall_survived_short, state.verfallCount), color = EmberDim, fontSize = 11.sp)
        }
        Text(
            stringResource(Res.string.verfall_description),
            color = EmberGold.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 6.dp),
        )

        val canTrigger = state.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelButton(
                onClick = { AppGraph.engine.triggerVerfall() },
                enabled = canTrigger,
                bevelLight = EmberAccentBright,
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text(stringResource(Res.string.verfall_button), fontFamily = pixelFontFamily(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (canTrigger) {
                Column(modifier = Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                    Text(stringResource(Res.string.reward_yield_label), color = EmberDim, fontSize = 10.sp)
                    Text(formatAmount(chroniclePointsForVerfall(state)), color = EmberGoldBright, fontSize = 13.sp, fontFamily = pixelFontFamily())
                }
            } else {
                Text(
                    stringResource(Res.string.verfall_locked_hint, formatAmount(MIN_LIFETIME_GOLD_FOR_VERFALL)),
                    color = EmberDim,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
            }
        }

        if (canTrigger && state.corruption > 0.0) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.verfall_corruption_label), color = EmberDim, fontSize = 10.sp)
                    Text("${(state.corruption * 100).roundToInt()}%", color = EmberAccentBright, fontSize = 10.sp, fontFamily = pixelFontFamily())
                }
                PixelSegmentedBar(
                    progress = state.corruption.toFloat(),
                    segments = 20,
                    filledColor = EmberAccentBright,
                    modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AcademyCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.academy_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
            Text(
                stringResource(Res.string.academy_level_label, state.academyLevel, formatAmount((academyMultiplier(state) - 1.0) * 100.0)),
                color = EmberGoldBright,
                fontSize = 12.sp,
            )
        }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(44.dp)
                .offset(x = shakeX.value.dp)
                .pixelFrame(fill = lerp(EmberPanelLight, EmberWhite, flash.value))
                // Always enabled — see the equivalent comment on BuildingRow's buy click.
                .clickable { AppGraph.engine.buyAcademyUpgrade() }
                .padding(horizontal = 12.dp)
                .alpha(if (affordable) 1f else 0.5f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.academy_study_button), color = EmberGold, fontSize = 11.sp, fontFamily = pixelFontFamily())
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelArtImage(remember { chroniclePointsIcon() }, modifier = Modifier.size(16.dp))
                Text(formatAmount(cost), color = EmberGoldBright, fontSize = 11.sp, fontFamily = pixelFontFamily(), modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun DynastyCard(state: GameState) {
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

    Column(modifier = Modifier.fillMaxWidth().offset(x = shakeX.value.dp).pixelFrame().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.dynasty_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
            Text(
                if (cooldownRemaining > 0) stringResource(Res.string.dynasty_switch_cooldown_hint, formatCooldown(cooldownRemaining)) else stringResource(Res.string.dynasty_switch_hint),
                color = EmberDim,
                fontSize = 11.sp,
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
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Text(stringResource(Res.string.wiedergeburt_button), fontFamily = pixelFontFamily(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
                        Text(stringResource(Res.string.reward_yield_label), color = EmberDim, fontSize = 10.sp)
                        Text(
                            formatAmount(sagenForWiedergeburt(state.lifetimeChroniclePoints)),
                            color = EmberGoldBright,
                            fontSize = 13.sp,
                            fontFamily = pixelFontFamily(),
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.wiedergeburt_title), color = EmberDim, fontSize = 12.sp)
                    Text(
                        stringResource(Res.string.wiedergeburt_locked_hint, MIN_VERFALL_COUNT_FOR_WIEDERGEBURT, state.verfallCount),
                        color = LockedTextColor,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}

/** A small heraldic block naming the current life's Heir — rolled fresh at each Wiedergeburt. */
@Composable
private fun HeirCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Text(stringResource(Res.string.heir_card_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
        val heirId = state.currentHeirId
        if (heirId == null) {
            Text(
                stringResource(Res.string.heir_none_hint),
                color = EmberDim,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            val trait = HeirTraitDefinition.byId(heirId)
            Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelArtImage(
                    art = remember(heirId) { heirEmblemIcon(trait.effectType) },
                    modifier = Modifier.size(32.dp),
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        stringResource(Res.string.heir_name_and_trait, state.currentHeirName, stringResource(trait.nameRes)),
                        color = EmberGoldBright,
                        fontSize = 13.sp,
                        fontFamily = pixelFontFamily(),
                    )
                    Text(stringResource(trait.descriptionRes), color = EmberDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
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
private fun QuestsCard(state: GameState) {
    val openQuests = remember(state.claimedQuestIds) {
        QuestDefinition.all.filter { it.id !in state.claimedQuestIds }.take(3)
    }
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Text(stringResource(Res.string.quests_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
        if (openQuests.isEmpty()) {
            Text(
                stringResource(Res.string.quests_empty_hint),
                color = EmberDim,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                openQuests.forEach { quest -> QuestRow(state, quest) }
            }
        }
    }
}

@Composable
private fun QuestRow(state: GameState, quest: QuestDefinition) {
    val fulfilled = quest.condition(state)
    Row(
        modifier = Modifier.fillMaxWidth().background(EmberHeaderBar).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(quest.nameRes),
                color = if (fulfilled) EmberGoldBright else EmberGold.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${stringResource(Res.string.quest_reward_label)}: ${formatAmount(quest.rewardGold)} ${stringResource(Res.string.gold_label)}",
                color = EmberDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier = Modifier
                .height(36.dp)
                .pixelFrame(
                    fill = if (fulfilled) EmberAccent else EmberAccent.copy(alpha = 0.35f),
                    bevelLight = EmberAccentBright,
                )
                // Always enabled — the engine decides eligibility, matching the rest of the UI.
                .clickable { AppGraph.engine.claimQuest(quest.id) }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.quest_claim_button), color = EmberWhite, fontSize = 10.sp, fontFamily = pixelFontFamily())
        }
    }
}

@Composable
private fun RatssaalCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.ratssaal_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
            IconValueLabel(
                icon = { PixelArtImage(remember { einflussIcon() }, modifier = Modifier.fillMaxSize()) },
                iconSize = 14.dp,
                value = formatAmount(state.einfluss),
                valueColor = EmberGoldBright,
                valueFontSize = 11.sp,
                label = stringResource(Res.string.einfluss_label),
                labelFontSize = 10.sp,
            )
        }
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RatssaalUpgradeDefinition.all.forEach { upgrade -> RatssaalUpgradeRow(state, upgrade) }
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

@Composable
private fun WondersCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Text(stringResource(Res.string.wonders_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WonderDefinition.all.forEach { wonder -> WonderRow(state, wonder) }
        }
    }
}

@Composable
private fun WonderRow(state: GameState, wonder: WonderDefinition) {
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

    Row(
        modifier = Modifier.fillMaxWidth().offset(x = shakeX.value.dp).background(EmberHeaderBar).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(wonder.nameRes),
                color = when {
                    owned -> EmberGoldBright
                    unlocked -> EmberGold.copy(alpha = 0.8f)
                    else -> EmberDim
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(wonder.descriptionRes),
                color = EmberDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        when {
            owned -> Text(stringResource(Res.string.wonder_built_label), color = EmberDim, fontSize = 10.sp, fontFamily = pixelFontFamily())
            !unlocked -> Text(
                stringResource(Res.string.wonder_locked_hint, stringResource(AgeDefinition.all[wonder.unlockAgeIndex].nameRes)),
                color = EmberDim,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
            )
            else -> Row(
                modifier = Modifier
                    .height(36.dp)
                    .pixelFrame(
                        fill = if (affordable) EmberAccent else EmberAccent.copy(alpha = 0.35f),
                        bevelLight = EmberAccentBright,
                    )
                    .clickable { AppGraph.engine.buyWonder(wonder.id) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.wonder_build_button, formatAmount(wonder.cost)),
                    color = EmberWhite,
                    fontSize = 10.sp,
                    fontFamily = pixelFontFamily(),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ChronicleCard(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Text(stringResource(Res.string.chronicle_card_title), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
        if (state.chronicle.isEmpty()) {
            Text(
                stringResource(Res.string.chronicle_empty_hint),
                color = EmberDim,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.chronicle.asReversed().take(12).forEach { entry ->
                    Text(chronicleEntryText(entry), color = EmberDim, fontSize = 11.sp)
                }
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

@Composable
private fun AchievementsBlock(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().pixelFrame().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.reign_tab_achievements), color = EmberGold, fontSize = 12.sp, fontFamily = pixelFontFamily())
            Text(
                "${state.unlockedAchievements.size}/${AchievementDefinition.all.size} · +${formatAmount((achievementMultiplier(state) - 1.0) * 100.0)}%",
                color = EmberGoldBright,
                fontSize = 12.sp,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AchievementDefinition.all.forEach { achievement ->
                val unlocked = achievement.id in state.unlockedAchievements
                Box(modifier = Modifier.background(if (unlocked) EmberPanelLight else EmberHeaderBar).padding(horizontal = 7.dp, vertical = 5.dp)) {
                    Text(stringResource(achievement.nameRes), color = if (unlocked) EmberGold else LockedTextColor, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RulesSystemBlock(state: GameState) {
    var exportExpanded by remember { mutableStateOf(false) }
    var importExpanded by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val uiSettings by AppGraph.uiSettings.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().pixelFrame(fill = EmberHeaderBar, raised = false).padding(12.dp)) {
        // Stacked, not a Row with the label: this panel sits in a narrow sidebar/column on
        // desktop, and 3 chips beside a label overflowed the available width, starving the
        // label's weight(1f) Text down to near-zero and wrapping it one character per line —
        // the same Row-starvation trap as the offline-progress row below, just triggered by an
        // unweighted sibling that's too wide rather than too narrow.
        Text(stringResource(Res.string.system_language_label), color = EmberGold, fontSize = 13.sp)
        LanguageChips(
            selected = uiSettings.language,
            onSelect = { AppGraph.uiSettings.setLanguage(it) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            // weight(1f) is required here, not optional: a wrapping Text with no weight is
            // measured against the Row's full width (it happily wraps to fill whatever it's
            // given), leaving nothing for the switch after it — the same trap as the Dynasty
            // path row above, just with a Text instead of a too-wide button as the offender.
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(Res.string.rules_offline_progress_label), color = EmberGold, fontSize = 13.sp)
                Text(stringResource(Res.string.rules_offline_progress_description), color = EmberDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            PixelSwitch(checked = state.offlineProgressEnabled, onCheckedChange = { AppGraph.engine.setOfflineProgressEnabled(it) })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.system_mute_label), color = EmberGold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            PixelSwitch(checked = uiSettings.muted, onCheckedChange = { AppGraph.uiSettings.setMuted(it) })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.system_volume_label), color = EmberGold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                "${(uiSettings.masterVolume * 100).roundToInt()}%",
                color = EmberGoldBright,
                fontSize = 13.sp,
                fontFamily = pixelFontFamily(),
            )
        }
        VolumeBar(
            volume = uiSettings.masterVolume,
            onVolumeChange = { AppGraph.uiSettings.setMasterVolume(it) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(18.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        .pixelFrame(fill = EmberBackground, raised = false)
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
            fontSize = 8.sp,
            fontFamily = pixelFontFamily(),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
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

@Composable
private fun IconValueLabel(
    icon: @Composable () -> Unit,
    iconSize: Dp,
    value: String,
    valueColor: Color,
    valueFontSize: TextUnit,
    label: String,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(iconSize)) { icon() }
        Text(value, color = valueColor, fontSize = valueFontSize, modifier = Modifier.padding(start = 5.dp))
        Text(label, color = EmberDim, fontSize = labelFontSize, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ValueLabel(value: String, label: String, valueColor: Color, valueFontSize: TextUnit, labelFontSize: TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = valueColor, fontSize = valueFontSize)
        Text(label, color = EmberDim, fontSize = labelFontSize, modifier = Modifier.padding(start = 4.dp))
    }
}
