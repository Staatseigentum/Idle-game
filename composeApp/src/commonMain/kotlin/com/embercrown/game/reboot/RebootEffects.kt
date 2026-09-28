package com.embercrown.game.reboot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.reboot_ritual_fallen
import com.embercrown.game.resources.reboot_ritual_reward
import com.embercrown.game.ui.pixelFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
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

/** The realm burns away before the fresh reign is revealed. The save changes at the dark midpoint. */
@Composable
fun RitualTransition(reward: Int, onMidpoint: () -> Unit, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                delay(1_080)
                onMidpoint()
            }
            progress.animateTo(1f, tween(2_400, easing = LinearEasing))
        }
        onFinished()
    }
    val p = progress.value
    val veil = when {
        p < 0.45f -> (p / 0.45f * 0.96f)
        p < 0.66f -> 0.96f
        else -> ((1f - p) / 0.34f * 0.96f).coerceIn(0f, 0.96f)
    }
    Box(
        modifier = Modifier.fillMaxSize().background(AshPalette.void.copy(alpha = veil)).clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val block = 12.dp.toPx()
            repeat(170) { index ->
                val x = ((index * 83 + index * index * 17) % size.width.toInt().coerceAtLeast(1)).toFloat()
                val startY = size.height + (index * 41 % 220)
                val speed = 0.45f + index % 7 * 0.12f
                val y = startY - p * size.height * 2.2f * speed
                if (y in -block..size.height) {
                    val opacity = ((1f - p) * 0.9f).coerceIn(0f, 1f)
                    val color = when (index % 4) {
                        0 -> AshPalette.flameLight
                        1 -> AshPalette.crimson
                        else -> AshPalette.flame
                    }
                    drawRect(color.copy(alpha = opacity), Offset(x, y), Size(block, block * (1 + index % 3)))
                }
            }
        }
        if (p in 0.25f..0.88f) {
            Column(
                modifier = Modifier.background(AshPalette.void.copy(alpha = 0.88f))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .alpha((minOf((p - 0.25f) * 6f, (0.88f - p) * 7f)).coerceIn(0f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.reboot_ritual_fallen), color = AshPalette.bone,
                    fontFamily = pixelFontFamily(), fontSize = 11.sp,
                )
                Text(
                    stringResource(Res.string.reboot_ritual_reward, reward), color = AshPalette.flameLight,
                    fontFamily = pixelFontFamily(), fontSize = 9.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
