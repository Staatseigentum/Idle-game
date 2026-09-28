package com.embercrown.game.reboot

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.resources.*
import com.embercrown.game.ui.pixelFontFamily
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelFit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun tutorialHighlightColor(active: Boolean, fallback: Color): Color {
    if (!active) return fallback
    val transition = rememberInfiniteTransition()
    val glow by transition.animateFloat(0f, 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse))
    return lerp(AshPalette.flame, AshPalette.flameLight, glow)
}

@Composable
internal fun TutorialGuide(state: RebootState, modifier: Modifier = Modifier) {
    val step = state.tutorialStep
    if (step !in 1..4 && (step != TUTORIAL_DONE || state.tutorialAcknowledged)) return
    val complete = step == TUTORIAL_DONE
    Column(
        modifier = modifier.fillMaxWidth().background(AshPalette.panelRaised)
            .border(2.dp, tutorialHighlightColor(true, AshPalette.flame))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Label(if (complete) stringResource(Res.string.tutorial_complete)
            else stringResource(Res.string.tutorial_label, step), AshPalette.flameLight, 9)
        if (!complete) {
            val progress = (step - 1) / 4f
            Box(Modifier.fillMaxWidth().height(5.dp).background(AshPalette.void)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(AshPalette.flame))
            }
            Label(stringResource(Res.string.tutorial_focus), AshPalette.teal, 7)
        }
        Text(stringResource(tutorialTitle(step)), color = AshPalette.bone,
            fontFamily = pixelFontFamily(), fontSize = 10.sp, lineHeight = 16.sp)
        Body(stringResource(tutorialBody(step)), AshPalette.ash)
        when (step) {
            1 -> Label(stringResource(Res.string.tutorial_step_gather_progress,
                state.lifetimeEmbers.toInt().coerceIn(0, 15)), AshPalette.flameLight, 8)
            3 -> Label(stringResource(Res.string.tutorial_step_levels_progress,
                state.level("coalpit").coerceAtMost(5)), AshPalette.flameLight, 8)
        }
        if (complete) {
            AshButton(stringResource(Res.string.tutorial_continue), true,
                modifier = Modifier.fillMaxWidth(),
                onClick = RebootGraph.engine::acknowledgeTutorial)
        } else {
            AshButton(stringResource(Res.string.tutorial_skip), true,
                color = AshPalette.panel, modifier = Modifier.fillMaxWidth(),
                onClick = RebootGraph.engine::skipTutorial)
        }
    }
}

@Composable
internal fun TutorialWelcome() {
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.97f)).clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 470.dp).fillMaxWidth(0.92f)
                .background(AshPalette.panel).border(2.dp, AshPalette.flame)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Label("EMBERCROWN", AshPalette.flameLight, 12)
            Text(stringResource(Res.string.tutorial_welcome_title),
                color = AshPalette.bone, fontFamily = pixelFontFamily(), fontSize = 10.sp,
                lineHeight = 18.sp)
            Box(Modifier.fillMaxWidth().height(175.dp).clipToBounds().background(AshPalette.void)) {
                PixelArtImage(remember { ashKingdomArt(emptyMap(), 8, false) },
                    Modifier.fillMaxSize(), fit = PixelFit.Cover)
            }
            Body(stringResource(Res.string.tutorial_welcome_body), AshPalette.ash)
            AshButton(stringResource(Res.string.tutorial_begin), true,
                modifier = Modifier.fillMaxWidth(), onClick = RebootGraph.engine::beginTutorial)
            AshButton(stringResource(Res.string.tutorial_skip), true,
                color = AshPalette.panelRaised, modifier = Modifier.fillMaxWidth(),
                onClick = RebootGraph.engine::skipTutorial)
        }
    }
}

@Composable
internal fun ResetConfirmation(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = 0.94f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(0.9f)
                .background(AshPalette.panel).border(2.dp, AshPalette.crimson)
                .clickable { }.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Label(stringResource(Res.string.tutorial_reset_title), AshPalette.flameLight, 10)
            Body(stringResource(Res.string.tutorial_reset_body), AshPalette.bone)
            AshButton(stringResource(Res.string.tutorial_reset_confirm), true,
                color = AshPalette.crimson, modifier = Modifier.fillMaxWidth(), onClick = onConfirm)
            AshButton(stringResource(Res.string.reboot_cancel), true,
                color = AshPalette.panelRaised, modifier = Modifier.fillMaxWidth(), onClick = onCancel)
        }
    }
}

private fun tutorialTitle(step: Int): StringResource = when (step) {
    1 -> Res.string.tutorial_step_gather
    2 -> Res.string.tutorial_step_build
    3 -> Res.string.tutorial_step_levels
    4 -> Res.string.tutorial_step_mastery
    else -> Res.string.tutorial_complete
}

private fun tutorialBody(step: Int): StringResource = when (step) {
    1 -> Res.string.tutorial_step_gather_body
    2 -> Res.string.tutorial_step_build_body
    3 -> Res.string.tutorial_step_levels_body
    4 -> Res.string.tutorial_step_mastery_body
    else -> Res.string.tutorial_complete_body
}
