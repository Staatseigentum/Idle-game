package com.embercrown.game.reboot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.prestige_balance
import com.embercrown.game.resources.prestige_continue
import com.embercrown.game.resources.prestige_howto
import com.embercrown.game.resources.prestige_reward
import com.embercrown.game.resources.prestige_subtitle
import com.embercrown.game.resources.prestige_title
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelFit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** A saved, full-screen decision between reigns. The kingdom clock is paused until Continue. */
@Composable
internal fun PrestigeScreen(state: RebootState, onContinue: () -> Unit) {
    val scene = remember { ashKingdomArt(emptyMap(), 8, false, skyLook = "dawn") }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(125)
            frame = (frame + 1) % 96
        }
    }
    val motion = remember(frame) { ashKingdomMotionArt(emptyMap(), 8, false, frame, skyLook = "dawn") }
    Column(Modifier.fillMaxSize().background(AshPalette.void)) {
        Column(Modifier.fillMaxWidth().background(AshPalette.night)
            .border(1.dp, AshPalette.edge).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(stringResource(Res.string.prestige_title), AshPalette.flameLight, 12)
            Body(stringResource(Res.string.prestige_subtitle, state.reign), AshPalette.bone)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 850.dp) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(0.95f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PrestigeScene(scene, motion, Modifier.fillMaxWidth().height(290.dp))
                        PrestigeSummary(state)
                        RelicSetPanel(state)
                    }
                    Column(Modifier.weight(1.05f).fillMaxHeight().verticalScroll(rememberScrollState())
                        .background(AshPalette.night).border(1.dp, AshPalette.edge)
                        .padding(14.dp)) {
                        RelicForgePanel(state)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrestigeScene(scene, motion, Modifier.fillMaxWidth().height(205.dp))
                    PrestigeSummary(state)
                    RelicForgePanel(state)
                    RelicSetPanel(state)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(AshPalette.night)
            .border(1.dp, AshPalette.edge).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                Label(stringResource(Res.string.prestige_balance, state.relics), AshPalette.teal, 9)
            }
            AshButton(stringResource(Res.string.prestige_continue, state.reign), true,
                color = AshPalette.flame, modifier = Modifier.weight(1.2f), onClick = onContinue)
        }
    }
}

@Composable
private fun PrestigeScene(scene: com.embercrown.game.ui.pixelart.PixelArt,
                          motion: com.embercrown.game.ui.pixelart.PixelArt, modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds().background(AshPalette.night).border(2.dp, AshPalette.flame)) {
        PixelArtImage(scene, Modifier.fillMaxSize(), PixelFit.Cover)
        PixelArtImage(motion, Modifier.fillMaxSize(), PixelFit.Cover)
    }
}

@Composable
private fun PrestigeSummary(state: RebootState) {
    Column(Modifier.fillMaxWidth().background(AshPalette.panel).border(1.dp, AshPalette.flame)
        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(stringResource(Res.string.prestige_reward, state.prestigeEarned), AshPalette.flameLight, 9)
        Body(stringResource(Res.string.prestige_howto), AshPalette.bone)
    }
}
