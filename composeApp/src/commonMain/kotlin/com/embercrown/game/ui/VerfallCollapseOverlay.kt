package com.embercrown.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.embercrown.game.game.AppGraph
import com.embercrown.game.game.GameState
import com.embercrown.game.game.chroniclePointsForVerfall
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.chronicle_points_label
import com.embercrown.game.resources.realm_fallen_title
import com.embercrown.game.ui.pixelart.EmberPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.random.Random

private const val TotalDurationMs = 4400f

private val EatKeyframes = listOf(0f to 1f, .82f to 1f, 1f to 0f)
private val VignetteKeyframes = listOf(0f to 0f, .25f to .5f, .6f to .85f, .88f to .85f, 1f to 0f)
private val FlashKeyframes = listOf(0f to 0f, .3f to 0f, .34f to .55f, .48f to 0f, 1f to 0f)
private val FallenInAlphaKeyframes = listOf(0f to 0f, .14f to 0f, .26f to 1f, .78f to 1f, 1f to 0f)
private val FallenInScaleKeyframes = listOf(0f to 2.4f, .14f to 2.4f, .26f to 1f, .78f to 1f, 1f to 1f)
private val YieldInAlphaKeyframes = listOf(0f to 0f, .42f to 0f, .56f to 1f, .84f to 1f, 1f to 0f)
private val YieldInTranslateYKeyframes = listOf(0f to 14f, .42f to 14f, .56f to 0f, .84f to 0f, 1f to -8f)
private val RipTopKeyframes = listOf(0f to 0f, .3f to 0f, .62f to -30f, 1f to 0f)
private val RipBottomKeyframes = listOf(0f to 0f, .3f to 0f, .62f to 30f, 1f to 0f)
private val ShakeXKeyframes = listOf(0f to 0f, .1f to -6f, .2f to 5f, .3f to -7f, .4f to 6f, .5f to -4f, .6f to 4f, .7f to -3f, .8f to 2f, .9f to -1f, 1f to 0f)
private val ShakeYKeyframes = listOf(0f to 0f, .1f to 3f, .2f to -4f, .3f to -2f, .4f to 4f, .5f to 2f, .6f to -3f, .7f to 1f, .8f to 2f, .9f to -1f, 1f to 0f)

private val PlagueColors = listOf(
    Color(0xFF4A1D52),
    Color(0xFF3A1440),
    Color(0xFF2A0B30),
    Color(0xFF120014),
    Color(0xFF5C2A63),
)

/** Linearly interpolates between CSS-keyframe-style stops `(fraction 0..1, value)`. */
private fun keyframeValue(fraction: Float, stops: List<Pair<Float, Float>>): Float {
    if (fraction <= stops.first().first) return stops.first().second
    if (fraction >= stops.last().first) return stops.last().second
    for (i in 0 until stops.size - 1) {
        val (f0, v0) = stops[i]
        val (f1, v1) = stops[i + 1]
        if (fraction in f0..f1) {
            val local = if (f1 > f0) (fraction - f0) / (f1 - f0) else 0f
            return v0 + (v1 - v0) * local
        }
    }
    return stops.last().second
}

private fun shakeOffset(t: Float): Offset {
    if (t >= 1650f) return Offset.Zero
    val localFrac = (t % 550f) / 550f
    return Offset(keyframeValue(localFrac, ShakeXKeyframes), keyframeValue(localFrac, ShakeYKeyframes))
}

/**
 * The "die Seuche frisst das Reich" collapse sequence: a single 4.4s [Animatable] progress value
 * drives every layer (window shake, the corrupted-block grid, flash, vignette, screen rip, title,
 * yield readout) from the exact timing the design handoff's CSS `@keyframes` describe, so all
 * layers stay in lockstep instead of drifting as independent animations.
 *
 * [content] is the normal game UI, wrapped so the shake offset applies to it; the collapse
 * layers themselves carry no pointer input (matching the design's `pointer-events: none`), so
 * they never intercept taps.
 */
@Composable
fun VerfallCollapseOverlay(
    trigger: Boolean,
    state: GameState,
    columns: Int,
    rows: Int,
    showRip: Boolean,
    titleFontSize: TextUnit,
    yieldFontSize: TextUnit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }
    var yieldPreview by remember { mutableStateOf(0.0) }

    LaunchedEffect(trigger) {
        if (trigger) {
            yieldPreview = chroniclePointsForVerfall(state)
            running = true
            progress.snapTo(0f)
            launch {
                delay(2600)
                AppGraph.engine.triggerVerfall()
            }
            progress.animateTo(TotalDurationMs, animationSpec = tween(TotalDurationMs.toInt(), easing = LinearEasing))
            running = false
            onFinished()
        }
    }

    val t = progress.value
    val frac = (t / TotalDurationMs).coerceIn(0f, 1f)
    val shake = shakeOffset(t)

    val cellRandoms = remember(trigger) {
        if (trigger) List(columns * rows) { Random.nextFloat() * 220f } else emptyList()
    }

    Box(modifier = modifier) {
        Box(modifier = Modifier.offset(x = shake.x.dp, y = shake.y.dp)) {
            content()
        }

        if (running) {
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val cellW = size.width / columns
                val cellH = size.height / rows
                val eatOut = keyframeValue(frac, EatKeyframes)
                for (row in 0 until rows) {
                    for (col in 0 until columns) {
                        val idx = row * columns + col
                        val nx = if (columns > 1) col / (columns - 1).toFloat() else 0f
                        val ny = if (rows > 1) row / (rows - 1).toFloat() else 0f
                        val distToEdge = minOf(nx, 1f - nx, ny, 1f - ny)
                        val delay = (distToEdge / 0.5f).coerceIn(0f, 1f) * 2100f + cellRandoms.getOrElse(idx) { 0f }
                        val localT = (t - delay).coerceAtLeast(0f)
                        val raw = (localT / 500f).coerceIn(0f, 1f)
                        val stepped = floor(raw * 2f) / 2f
                        if (stepped <= 0f) continue
                        val cellAlpha = stepped * eatOut
                        if (cellAlpha <= 0f) continue
                        val cellScale = 0.25f + stepped * 0.75f
                        val w = cellW * cellScale
                        val h = cellH * cellScale
                        val cx = col * cellW + cellW / 2f
                        val cy = row * cellH + cellH / 2f
                        drawRect(
                            color = PlagueColors[idx % PlagueColors.size].copy(alpha = cellAlpha),
                            topLeft = Offset(cx - w / 2f, cy - h / 2f),
                            size = Size(w, h),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().background(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.2f to Color.Transparent,
                        1f to Color(0xFF120014).copy(alpha = keyframeValue(frac, VignetteKeyframes)),
                    ),
                ),
            )

            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
                    .background(Color(0xFFCBA8F2).copy(alpha = keyframeValue(frac, FlashKeyframes))),
            )

            if (showRip) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .offset(y = keyframeValue(frac, RipTopKeyframes).dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF120014).copy(alpha = 0.55f), Color.Transparent))),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .offset(y = keyframeValue(frac, RipBottomKeyframes).dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF120014).copy(alpha = 0.55f)))),
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val titleAlpha = keyframeValue(frac, FallenInAlphaKeyframes)
                val titleScale = keyframeValue(frac, FallenInScaleKeyframes)
                Text(
                    stringResource(Res.string.realm_fallen_title),
                    color = Color(0xFFCBA8F2),
                    fontSize = titleFontSize,
                    fontFamily = pixelFontFamily(),
                    letterSpacing = 0.12f.em,
                    modifier = Modifier.alpha(titleAlpha).scale(titleScale),
                )
                val yieldAlpha = keyframeValue(frac, YieldInAlphaKeyframes)
                val yieldOffsetY = keyframeValue(frac, YieldInTranslateYKeyframes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 26.dp).alpha(yieldAlpha).offset(y = yieldOffsetY.dp),
                ) {
                    Box(modifier = Modifier.size(14.dp).background(Color(0xFFFBF3DC)))
                    Text(
                        "+${formatAmount(yieldPreview)} ${stringResource(Res.string.chronicle_points_label).uppercase()}",
                        color = EmberPalette.GoldBright,
                        fontSize = yieldFontSize,
                        fontFamily = pixelFontFamily(),
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }
        }
    }
}
