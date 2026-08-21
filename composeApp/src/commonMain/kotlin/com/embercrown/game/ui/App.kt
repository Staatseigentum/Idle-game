package com.embercrown.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.game.AchievementDefinition
import com.embercrown.game.game.AgeDefinition
import com.embercrown.game.game.AppGraph
import com.embercrown.game.game.BuildingDefinition
import com.embercrown.game.game.DynastyPath
import com.embercrown.game.game.GameState
import com.embercrown.game.game.MIN_LIFETIME_GOLD_FOR_VERFALL
import com.embercrown.game.game.MIN_VERFALL_COUNT_FOR_WIEDERGEBURT
import com.embercrown.game.game.achievementMultiplier
import com.embercrown.game.game.academyMultiplier
import com.embercrown.game.game.academyUpgradeCost
import com.embercrown.game.game.bulkBuildingCost
import com.embercrown.game.game.buildingProduction
import com.embercrown.game.game.chroniclePointsForVerfall
import com.embercrown.game.game.isBuildingUnlocked
import com.embercrown.game.game.legacyUpgradeCost
import com.embercrown.game.game.maxAffordableQuantity
import com.embercrown.game.game.sagenForWiedergeburt
import com.embercrown.game.game.totalProduction
import com.embercrown.game.ui.pixelart.EmberPalette
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelButton
import com.embercrown.game.ui.pixelart.PixelFit
import com.embercrown.game.ui.pixelart.ageSceneArt
import com.embercrown.game.ui.pixelart.buildingIcon
import com.embercrown.game.ui.pixelart.chroniclePointsIcon
import com.embercrown.game.ui.pixelart.goldIcon
import com.embercrown.game.ui.pixelart.PixelSegmentedBar
import com.embercrown.game.ui.pixelart.pixelFrame
import com.embercrown.game.ui.pixelart.sagenIcon
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.academy_level_label
import com.embercrown.game.resources.academy_title
import com.embercrown.game.resources.academy_upgrade_button
import com.embercrown.game.resources.achievements_progress_label
import com.embercrown.game.resources.buildings_title
import com.embercrown.game.resources.buy_button
import com.embercrown.game.resources.chronicle_points_label
import com.embercrown.game.resources.gold_label
import com.embercrown.game.resources.level_label
import com.embercrown.game.resources.locked_until_label
import com.embercrown.game.resources.per_second_label
import com.embercrown.game.resources.progress_to_next_age
import com.embercrown.game.resources.dynasty_path_activate_button
import com.embercrown.game.resources.dynasty_path_active_label
import com.embercrown.game.resources.dynasty_path_invest_button
import com.embercrown.game.resources.dynasty_path_level_label
import com.embercrown.game.resources.dynasty_switch_hint
import com.embercrown.game.resources.legends_label
import com.embercrown.game.resources.reign_tab_academy
import com.embercrown.game.resources.reign_tab_achievements
import com.embercrown.game.resources.reign_tab_dynasty
import com.embercrown.game.resources.reign_tab_rules
import com.embercrown.game.resources.reign_tab_system
import com.embercrown.game.resources.reign_tab_verfall
import com.embercrown.game.resources.rules_offline_progress_description
import com.embercrown.game.resources.rules_offline_progress_label
import com.embercrown.game.resources.system_export_hint
import com.embercrown.game.resources.system_export_title
import com.embercrown.game.resources.system_import_button
import com.embercrown.game.resources.system_import_error
import com.embercrown.game.resources.system_import_hint
import com.embercrown.game.resources.system_import_success
import com.embercrown.game.resources.system_import_title
import com.embercrown.game.resources.system_reset_button
import com.embercrown.game.resources.system_reset_confirm_button
import com.embercrown.game.resources.system_reset_description
import com.embercrown.game.resources.system_reset_title
import com.embercrown.game.resources.system_save_button
import com.embercrown.game.resources.tab_holdings
import com.embercrown.game.resources.tab_reign
import com.embercrown.game.resources.tap_button
import com.embercrown.game.resources.verfall_button
import com.embercrown.game.resources.verfall_count_label
import com.embercrown.game.resources.verfall_description
import com.embercrown.game.resources.verfall_locked_hint
import com.embercrown.game.resources.verfall_reward_preview
import com.embercrown.game.resources.verfall_title
import com.embercrown.game.resources.wiedergeburt_button
import com.embercrown.game.resources.wiedergeburt_count_label
import com.embercrown.game.resources.wiedergeburt_description
import com.embercrown.game.resources.wiedergeburt_locked_hint
import com.embercrown.game.resources.wiedergeburt_reward_preview
import com.embercrown.game.resources.wiedergeburt_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class AppTab(val labelRes: StringResource) {
    HOLDINGS(Res.string.tab_holdings),
    REIGN(Res.string.tab_reign),
}

private enum class ReignSubTab(val labelRes: StringResource) {
    VERFALL(Res.string.reign_tab_verfall),
    DYNASTY(Res.string.reign_tab_dynasty),
    ACADEMY(Res.string.reign_tab_academy),
    ACHIEVEMENTS(Res.string.reign_tab_achievements),
    RULES(Res.string.reign_tab_rules),
    SYSTEM(Res.string.reign_tab_system),
}

private val EmberGold = EmberPalette.Gold
private val EmberBackground = EmberPalette.Background
private val EmberPanel = EmberPalette.Panel
private val EmberPanelLight = EmberPalette.PanelLight
private val EmberAccent = EmberPalette.Accent
private val EmberDim = EmberPalette.Dim

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
    MaterialTheme(colorScheme = EmbercrownColors) {
        Surface(color = EmberBackground) {
            val state by AppGraph.engine.state.collectAsState()
            var multiplier by remember { mutableStateOf(BuyMultiplier.X1) }
            var tab by remember { mutableStateOf(AppTab.HOLDINGS) }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val wide = maxWidth >= 700.dp
                if (wide) {
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        AgesSidebar(state = state, modifier = Modifier.width(220.dp).fillMaxHeight())
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            MainContent(state, multiplier, tab, onMultiplierChange = { multiplier = it }, onTabChange = { tab = it })
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        AgesStrip(state = state)
                        MainContent(
                            state,
                            multiplier,
                            tab,
                            onMultiplierChange = { multiplier = it },
                            onTabChange = { tab = it },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    state: GameState,
    multiplier: BuyMultiplier,
    tab: AppTab,
    onMultiplierChange: (BuyMultiplier) -> Unit,
    onTabChange: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ResourceBar(gold = state.gold, perSecond = totalProduction(state))
        CentralScene(state = state)
        TabRow(selected = tab, onSelect = onTabChange)

        when (tab) {
            AppTab.HOLDINGS -> HoldingsTab(state = state, multiplier = multiplier, onMultiplierChange = onMultiplierChange)
            AppTab.REIGN -> ReignPanel(state = state)
        }
    }
}

@Composable
private fun HoldingsTab(
    state: GameState,
    multiplier: BuyMultiplier,
    onMultiplierChange: (BuyMultiplier) -> Unit,
) {
    Column {
        BuyMultiplierRow(selected = multiplier, onSelect = onMultiplierChange)

        Text(
            text = stringResource(Res.string.buildings_title),
            color = EmberGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BuildingDefinition.all) { definition ->
                val level = state.buildingLevel(definition.id)
                val unlocked = isBuildingUnlocked(definition, state)
                if (unlocked) {
                    val quantity = multiplier.quantity ?: maxAffordableQuantity(definition, level, state.gold)
                    val cost = bulkBuildingCost(definition, level, quantity.coerceAtLeast(1))
                    BuildingRow(
                        buildingId = definition.id,
                        name = stringResource(definition.nameRes),
                        level = level,
                        production = buildingProduction(definition, level),
                        cost = cost,
                        canAfford = quantity > 0 && state.gold >= cost,
                        onBuy = { AppGraph.engine.buy(definition.id, multiplier.quantity) },
                    )
                } else {
                    LockedBuildingRow(
                        buildingId = definition.id,
                        name = stringResource(definition.nameRes),
                        unlockAgeName = stringResource(AgeDefinition.all[definition.unlockAgeIndex].nameRes),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabRow(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        AppTab.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .pixelFrame(fill = if (isSelected) EmberAccent else EmberPanel, borderWidth = 2.dp)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    stringResource(option.labelRes),
                    color = if (isSelected) Color.White else EmberGold.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = pixelFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun ReignPanel(state: GameState) {
    var reignTab by remember { mutableStateOf(ReignSubTab.VERFALL) }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReignSubTab.entries.forEach { option ->
                val isSelected = option == reignTab
                Box(
                    modifier = Modifier
                        .pixelFrame(fill = if (isSelected) EmberAccent else EmberPanelLight, borderWidth = 2.dp)
                        .clickable { reignTab = option }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        stringResource(option.labelRes),
                        color = if (isSelected) Color.White else EmberGold.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = pixelFontFamily(),
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (reignTab) {
                ReignSubTab.VERFALL -> VerfallSection(state)
                ReignSubTab.DYNASTY -> DynastySection(state)
                ReignSubTab.ACADEMY -> AcademySection(state)
                ReignSubTab.ACHIEVEMENTS -> AchievementsSection(state)
                ReignSubTab.RULES -> RulesSection(state)
                ReignSubTab.SYSTEM -> SystemSection(state)
            }
        }
    }
}

@Composable
private fun VerfallSection(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .pixelFrame()
            .padding(16.dp),
    ) {
        Text(stringResource(Res.string.verfall_title), color = EmberGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = pixelFontFamily())
        Text(
            stringResource(Res.string.verfall_description),
            color = EmberGold.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Text(
            stringResource(Res.string.verfall_count_label, state.verfallCount),
            color = EmberGold.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        val canTrigger = state.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL
        if (canTrigger) {
            Text(
                stringResource(Res.string.verfall_reward_preview, formatAmount(chroniclePointsForVerfall(state))),
                color = EmberGold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        } else {
            Text(
                stringResource(Res.string.verfall_locked_hint, formatAmount(MIN_LIFETIME_GOLD_FOR_VERFALL)),
                color = EmberDim,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        PixelButton(
            onClick = { AppGraph.engine.triggerVerfall() },
            enabled = canTrigger,
        ) {
            Text(stringResource(Res.string.verfall_button), fontFamily = pixelFontFamily())
        }
    }
}

@Composable
private fun DynastySection(state: GameState) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelFrame()
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.wiedergeburt_title), color = EmberGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = pixelFontFamily())
            Text(
                stringResource(Res.string.wiedergeburt_description),
                color = EmberGold.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Text(
                stringResource(Res.string.wiedergeburt_count_label, state.wiedergeburtCount),
                color = EmberGold.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            val canTrigger = state.verfallCount >= MIN_VERFALL_COUNT_FOR_WIEDERGEBURT
            if (canTrigger) {
                Text(
                    stringResource(Res.string.wiedergeburt_reward_preview, formatAmount(sagenForWiedergeburt(state.lifetimeChroniclePoints))),
                    color = EmberGold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            } else {
                Text(
                    stringResource(Res.string.wiedergeburt_locked_hint, MIN_VERFALL_COUNT_FOR_WIEDERGEBURT, state.verfallCount),
                    color = EmberDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            PixelButton(
                onClick = { AppGraph.engine.triggerWiedergeburt() },
                enabled = canTrigger,
            ) {
                Text(stringResource(Res.string.wiedergeburt_button), fontFamily = pixelFontFamily())
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        ) {
            PixelArtImage(remember { sagenIcon() }, modifier = Modifier.size(24.dp))
            Text(
                "${formatAmount(state.sagen)} ${stringResource(Res.string.legends_label)}",
                color = EmberGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(
            stringResource(Res.string.dynasty_switch_hint),
            color = EmberGold.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        DynastyPath.entries.forEach { path ->
            val level = state.legacyLevels[path] ?: 0
            val isActive = state.activeDynastyPath == path
            val cost = legacyUpgradeCost(level)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pixelFrame(fill = if (isActive) EmberAccent.copy(alpha = 0.18f) else EmberPanel)
                    .padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(path.nameRes), color = EmberGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (isActive) {
                        Text(
                            stringResource(Res.string.dynasty_path_active_label),
                            color = EmberAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    stringResource(path.descriptionRes),
                    color = EmberGold.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
                Text(
                    stringResource(Res.string.dynasty_path_level_label, level, formatAmount(path.bonusPerLevel * level * 100.0)),
                    color = EmberGold.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(
                        onClick = { AppGraph.engine.buyLegacyUpgrade(path) },
                        enabled = state.sagen >= cost,
                    ) {
                        Text(stringResource(Res.string.dynasty_path_invest_button, formatAmount(cost)), fontFamily = pixelFontFamily())
                    }
                    if (!isActive) {
                        PixelButton(
                            onClick = { AppGraph.engine.setActiveDynastyPath(path) },
                            containerColor = EmberPanelLight,
                            contentColor = EmberGold,
                        ) {
                            Text(stringResource(Res.string.dynasty_path_activate_button), fontFamily = pixelFontFamily())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademySection(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .pixelFrame()
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.academy_title), color = EmberGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = pixelFontFamily())
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelArtImage(remember { chroniclePointsIcon() }, modifier = Modifier.size(20.dp))
                Text(
                    "${formatAmount(state.chroniclePoints)} ${stringResource(Res.string.chronicle_points_label)}",
                    color = EmberGold.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Text(
            stringResource(
                Res.string.academy_level_label,
                state.academyLevel,
                formatAmount((academyMultiplier(state) - 1.0) * 100.0),
            ),
            color = EmberGold.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )

        val cost = academyUpgradeCost(state.academyLevel)
        PixelButton(
            onClick = { AppGraph.engine.buyAcademyUpgrade() },
            enabled = state.chroniclePoints >= cost,
        ) {
            Text(stringResource(Res.string.academy_upgrade_button, formatAmount(cost)), fontFamily = pixelFontFamily())
        }
    }
}

@Composable
private fun AchievementsSection(state: GameState) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            stringResource(
                Res.string.achievements_progress_label,
                state.unlockedAchievements.size,
                AchievementDefinition.all.size,
                formatAmount((achievementMultiplier(state) - 1.0) * 100.0),
            ),
            color = EmberGold.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 480.dp)) {
            items(AchievementDefinition.all) { achievement ->
                val unlocked = achievement.id in state.unlockedAchievements
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pixelFrame(fill = if (unlocked) EmberPanel else EmberPanel.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        stringResource(achievement.nameRes),
                        color = if (unlocked) EmberGold else EmberDim,
                        fontSize = 14.sp,
                        fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesSection(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .pixelFrame()
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.rules_offline_progress_label), color = EmberGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.rules_offline_progress_description),
                    color = EmberGold.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = state.offlineProgressEnabled,
                onCheckedChange = { AppGraph.engine.setOfflineProgressEnabled(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = EmberAccent),
            )
        }
    }
}

@Composable
private fun SystemSection(state: GameState) {
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var resetArmed by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        PixelButton(
            onClick = { AppGraph.engine.persistNow() },
        ) {
            Text(stringResource(Res.string.system_save_button), fontFamily = pixelFontFamily())
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .pixelFrame()
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.system_export_title), color = EmberGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(Res.string.system_export_hint),
                color = EmberGold.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            SelectionContainer {
                Text(
                    AppGraph.engine.exportSave(),
                    color = EmberGold.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .pixelFrame(fill = EmberBackground, raised = false)
                        .padding(10.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .pixelFrame()
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.system_import_title), color = EmberGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(Res.string.system_import_hint),
                color = EmberGold.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it; importMessage = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = EmberGold),
            )
            PixelButton(
                onClick = {
                    val success = AppGraph.engine.importSave(importText)
                    importMessage = if (success) null else "error"
                    if (success) importText = ""
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(Res.string.system_import_button), fontFamily = pixelFontFamily())
            }
            if (importMessage == "error") {
                Text(
                    stringResource(Res.string.system_import_error),
                    color = EmberAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .pixelFrame()
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.system_reset_title), color = EmberGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(Res.string.system_reset_description),
                color = EmberGold.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            PixelButton(
                onClick = {
                    if (resetArmed) {
                        AppGraph.engine.hardReset()
                        resetArmed = false
                    } else {
                        resetArmed = true
                    }
                },
            ) {
                Text(stringResource(if (resetArmed) Res.string.system_reset_confirm_button else Res.string.system_reset_button), fontFamily = pixelFontFamily())
            }
        }
    }
}

@Composable
private fun CentralScene(state: GameState) {
    val age = state.currentAge
    val next = state.nextAge
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.2f)
            .padding(vertical = 12.dp)
            .pixelFrame()
            .clipToBounds()
            .clickable { AppGraph.engine.click() },
    ) {
        // The landscape is the backdrop, edge to edge. Text is banded to the top and bottom so
        // the middle of the frame stays a clean view of the settlement, each band sitting on its
        // own soft scrim rather than one gradient washing out the whole picture.
        PixelArtImage(
            art = remember(age.index) { ageSceneArt(age.index, AgeDefinition.all.size) },
            modifier = Modifier.fillMaxSize(),
            fit = PixelFit.Cover,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(84.dp)
                .background(
                    Brush.verticalGradient(
                        0f to EmberBackground.copy(alpha = 0.88f),
                        0.7f to EmberBackground.copy(alpha = 0.35f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to EmberBackground.copy(alpha = 0.72f),
                        1f to EmberBackground.copy(alpha = 0.96f),
                    ),
                ),
        )

        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.pixelFrame(fill = EmberBackground.copy(alpha = 0.8f), raised = false, borderWidth = 2.dp)) {
                Text(
                    text = "${age.index + 1} / ${AgeDefinition.all.size}",
                    color = EmberGold,
                    fontSize = 11.sp,
                    fontFamily = pixelFontFamily(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(
                text = stringResource(age.nameRes),
                color = EmberGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = pixelFontFamily(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(age.chronicleRes),
                color = EmberGold.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.tap_button),
                color = EmberPalette.GoldBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = pixelFontFamily(),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            if (next != null) {
                val progress = (state.lifetimeGold / next.lifetimeGoldThreshold).toFloat().coerceIn(0f, 1f)
                PixelSegmentedBar(
                    progress = progress,
                    modifier = Modifier.width(240.dp).height(12.dp),
                )
                Text(
                    text = stringResource(
                        Res.string.progress_to_next_age,
                        formatAmount(state.lifetimeGold),
                        formatAmount(next.lifetimeGoldThreshold),
                    ),
                    color = EmberGold.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BuyMultiplierRow(selected: BuyMultiplier, onSelect: (BuyMultiplier) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BuyMultiplier.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .pixelFrame(fill = if (isSelected) EmberAccent else EmberPanel, borderWidth = 2.dp)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    option.label,
                    color = if (isSelected) Color.White else EmberGold.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = pixelFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun ResourceBar(gold: Double, perSecond: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelFrame()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelArtImage(remember { goldIcon() }, modifier = Modifier.size(34.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(stringResource(Res.string.gold_label), color = EmberGold.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = pixelFontFamily())
                Text(formatAmount(gold), color = EmberGold, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = pixelFontFamily())
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(Res.string.per_second_label, formatAmount(perSecond)),
                color = EmberGold.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun BuildingRow(
    buildingId: String,
    name: String,
    level: Int,
    production: Double,
    cost: Double,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelFrame()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            val icon = remember(buildingId) { buildingIcon(buildingId) }
            if (icon != null) {
                PixelArtImage(icon, modifier = Modifier.size(44.dp))
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(name, color = EmberGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${stringResource(Res.string.level_label, level)}  ·  ${formatAmount(production)}/s",
                    color = EmberGold.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
        PixelButton(
            onClick = onBuy,
            enabled = canAfford,
        ) {
            Text(stringResource(Res.string.buy_button, formatAmount(cost)), fontFamily = pixelFontFamily(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun LockedBuildingRow(buildingId: String, name: String, unlockAgeName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelFrame(fill = EmberPanel.copy(alpha = 0.5f))
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = remember(buildingId) { buildingIcon(buildingId) }
            if (icon != null) {
                PixelArtImage(icon, modifier = Modifier.size(44.dp).alpha(0.35f))
            }
            Text(name, color = EmberDim, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
        }
        Text(
            stringResource(Res.string.locked_until_label, unlockAgeName),
            color = EmberDim,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun AgesSidebar(state: GameState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(AgeDefinition.all) { age ->
            AgeEntry(age = age, state = state, horizontal = false)
        }
    }
}

@Composable
private fun AgesStrip(state: GameState) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AgeDefinition.all) { age ->
            AgeEntry(age = age, state = state, horizontal = true)
        }
    }
}

@Composable
private fun AgeEntry(age: AgeDefinition, state: GameState, horizontal: Boolean) {
    val reached = state.lifetimeGold >= age.lifetimeGoldThreshold
    val isCurrent = age.index == state.currentAge.index
    val background = if (isCurrent) EmberAccent.copy(alpha = 0.3f) else EmberPanelLight
    val textColor = if (reached) EmberGold else EmberDim

    Box(
        modifier = Modifier
            .let { if (horizontal) it.width(120.dp) else it.fillMaxWidth() }
            .pixelFrame(fill = background, borderWidth = if (isCurrent) 3.dp else 2.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(age.nameRes),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
