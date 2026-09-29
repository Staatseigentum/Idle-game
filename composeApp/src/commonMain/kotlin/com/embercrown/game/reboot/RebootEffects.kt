package com.embercrown.game.reboot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.reboot_ritual_breaks
import com.embercrown.game.resources.reboot_ritual_ashes
import com.embercrown.game.resources.reboot_ritual_fallen
import com.embercrown.game.resources.reboot_ritual_reborn
import com.embercrown.game.resources.reboot_ritual_reward
import com.embercrown.game.ui.pixelFontFamily
import com.embercrown.game.ui.pixelart.PixelArtImage
import com.embercrown.game.ui.pixelart.PixelFit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Twelve square sparks burst from the exact point touched, then arc upwards and vanish. */
@Composable
fun TapSparks(sequence: Int, position: Offset, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(sequence) {
        if (sequence > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(780, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier) {
        val p = progress.value
        if (sequence <= 0 || p >= 1f) return@Canvas
        val pixel = 4.dp.toPx()
        val directions = arrayOf(
            Offset(-1f, -1f), Offset(0f, -1.4f), Offset(1f, -1f),
            Offset(-1.5f, -0.1f), Offset(1.5f, -0.1f),
            Offset(-1.2f, 0.8f), Offset(1.2f, 0.8f), Offset(0f, 0.7f),
        )
        directions.forEachIndexed { index, direction ->
            val travel = (22f + index * 3f).dp.toPx() * p
            val x = position.x + direction.x * travel
            val y = position.y + direction.y * travel - p * p * 17.dp.toPx()
            drawRect(
                color = (if (index % 3 == 0) AshPalette.flameLight else AshPalette.flame)
                    .copy(alpha = (1f - p).coerceIn(0f, 1f)),
                topLeft = Offset(x, y),
                size = Size(pixel, pixel),
            )
        }
    }
}

/** A seven-second pixel-art short: the actual kingdom burns, its crown breaks, and the next reign is rebuilt. */
@Composable
fun RitualTransition(before: RebootState, reward: Int, onBurn: () -> Unit, onMidpoint: () -> Unit,
                     onRebirth: () -> Unit, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        kotlinx.coroutines.coroutineScope {
            launch {
                delay(1_450)
                onBurn()
                delay(1_650)
                onMidpoint()
                delay(1_820)
                onRebirth()
            }
            progress.animateTo(7.5f, tween(7_500, easing = LinearEasing))
        }
        onFinished()
    }
    val seconds = progress.value
    val frame = (seconds * 18f).toInt()
    val overlay = remember(frame) { ritualCinematicArt(frame / 18f, frame) }
    val oldScene = remember(before) {
        ashKingdomArt(before.levels, before.gloom.toInt(), before.beaconSeconds > 0,
            before.buildingUpgrades, before.conqueredRegions, before.specializations,
            before.districtLevels, before.eclipseSiegeStage == 3,
            before.cosmeticStyles["flame"] ?: "ember", before.cosmeticStyles["banner"] ?: "ash",
            before.cosmeticStyles["sky"] ?: "blood")
    }
    val newScene = remember { ashKingdomArt(emptyMap(), 8, false, skyLook = "dawn") }
    val viewportAlpha = ((seconds - 7.05f) / 0.45f).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().background(AshPalette.void).clickable { }) {
        val sceneWidth = minOf(maxWidth, maxHeight * 1.33f)
        val sceneHeight = sceneWidth * 0.75f
        val shake = if (seconds in 2.6f..3.55f) (1f - kotlin.math.abs(seconds - 3.1f) / 0.5f).coerceIn(0f, 1f) else 0f
        Box(
            Modifier.width(sceneWidth).height(sceneHeight).align(Alignment.Center)
                .graphicsLayer {
                    translationX = if (frame % 2 == 0) shake * 8.dp.toPx() else -shake * 8.dp.toPx()
                    translationY = if (frame % 3 == 0) shake * 5.dp.toPx() else -shake * 3.dp.toPx()
                },
        ) {
            if (seconds < 3.55f) PixelArtImage(oldScene, Modifier.fillMaxSize(), PixelFit.Contain)
            if (seconds >= 4.25f) PixelArtImage(newScene,
                Modifier.fillMaxSize().alpha(((seconds - 4.25f) / 1.35f).coerceIn(0f, 1f)), PixelFit.Contain)
            PixelArtImage(overlay, Modifier.fillMaxSize(), PixelFit.Contain)
            // A brief white-hot impact separates the dying realm from the ash-black pause.
            val flash = if (seconds in 3.08f..3.34f) (1f - (seconds - 3.08f) / 0.26f) else 0f
            if (flash > 0f) Box(Modifier.fillMaxSize().background(AshPalette.flameLight.copy(alpha = flash * 0.94f)))
            if (seconds in 3.55f..4.35f) {
                val darkness = (1f - kotlin.math.abs(seconds - 3.95f) / 0.4f).coerceIn(0f, 1f)
                Box(Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = darkness * 0.87f)))
            }
        }
        val heading = when {
            seconds < 1.6f -> Res.string.reboot_ritual_fallen
            seconds < 3.4f -> Res.string.reboot_ritual_breaks
            seconds < 4.65f -> Res.string.reboot_ritual_ashes
            else -> Res.string.reboot_ritual_reborn
        }
        Text(stringResource(heading), Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            color = if (seconds >= 4.4f) AshPalette.flameLight else AshPalette.bone,
            fontFamily = pixelFontFamily(), fontSize = 12.sp)
        if (seconds >= 4.8f) Text(stringResource(Res.string.reboot_ritual_reward, reward),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 29.dp)
                .alpha(((seconds - 4.8f) / 0.55f).coerceIn(0f, 1f)),
            color = AshPalette.flameLight, fontFamily = pixelFontFamily(), fontSize = 10.sp)
        if (viewportAlpha > 0f) Box(Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = viewportAlpha)))
    }
}
