package com.embercrown.game.ui.pixelart

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A chunky, square-cornered retro border: a solid outer line around a filled inner area
 * with a light/dark bevel at the inner edge. Used everywhere instead of rounded panels.
 */
fun Modifier.pixelFrame(
    fill: Color = EmberPalette.Panel,
    raised: Boolean = true,
    borderWidth: Dp = 2.dp,
    bevelLight: Color = EmberPalette.BevelLight,
): Modifier = this
    .background(EmberPalette.Shadow)
    .padding(borderWidth)
    .background(fill)
    .drawBehind {
        val light = if (raised) bevelLight else EmberPalette.Shadow
        val dark = if (raised) EmberPalette.Shadow else bevelLight
        val s = 1.5.dp.toPx()
        drawLine(light, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = s)
        drawLine(light, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = s)
        drawLine(dark, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = s)
        drawLine(dark, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = s)
    }

/** Which side of the box to draw a single flat 2px rule on — used for header/rail dividers. */
enum class PixelEdge { Top, Bottom, Start, End }

/** A single flat rule on one edge only — the "border-bottom: 2px solid" chrome lines used
 * throughout the redesigned layout (header bars, rails, footer nav), as opposed to the full
 * four-sided bevel of [pixelFrame]. */
fun Modifier.pixelEdgeLine(
    edge: PixelEdge,
    color: Color = EmberPalette.Shadow,
    thickness: Dp = 2.dp,
): Modifier = this.drawBehind {
    val s = thickness.toPx()
    when (edge) {
        PixelEdge.Top -> drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = s)
        PixelEdge.Bottom -> drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = s)
        PixelEdge.Start -> drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = s)
        PixelEdge.End -> drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = s)
    }
}

/** A pixel-framed clickable button that sinks its bevel while pressed. */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = EmberPalette.Accent,
    contentColor: Color = Color.White,
    bevelLight: Color = EmberPalette.BevelLight,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val effectiveFill = if (enabled) containerColor else containerColor.copy(alpha = 0.35f)
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, animationSpec = tween(80))

    Row(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .pixelFrame(fill = effectiveFill, raised = enabled && !pressed, borderWidth = 2.dp, bevelLight = bevelLight)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * A chunky pixel-styled on/off switch (52×26 dp), replacing Material's [androidx.compose.material3.Switch]
 * to match the retro chrome everywhere else — filled Accent + right-aligned knob when on, filled
 * PanelLight + left-aligned knob when off.
 */
@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Explicit x offset instead of contentAlignment(CenterStart/CenterEnd): alignment swaps the
    // knob position instantly on recomposition, an offset can be animated between the two.
    val knobOffset by animateDpAsState(if (checked) 28.dp else 0.dp, animationSpec = tween(150))
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 26.dp)
            .clickable { onCheckedChange(!checked) }
            .pixelFrame(fill = if (checked) EmberPalette.Accent else EmberPalette.PanelLight, borderWidth = 2.dp)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(width = 20.dp, height = 18.dp)
                .background(EmberPalette.GoldBright),
        )
    }
}

/** A chunky, segmented pixel progress bar (distinct blocks with a gap, not a smooth fill).
 * Segment width is always derived by dividing the given [modifier]'s width evenly, so the bar
 * stays responsive at any container width instead of overflowing a resizable window. */
@Composable
fun PixelSegmentedBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 16,
    filledColor: Color = EmberPalette.Gold,
    emptyColor: Color = EmberPalette.Dim.copy(alpha = 0.4f),
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val gap = 2.dp.toPx()
        val segmentW = (size.width - gap * (segments - 1)) / segments
        val filledSegments = (clamped * segments).toInt()
        val partial = clamped * segments - filledSegments
        for (i in 0 until segments) {
            val x = i * (segmentW + gap)
            val color = when {
                i < filledSegments -> filledColor
                i == filledSegments && partial > 0.15f -> filledColor.copy(alpha = 0.5f)
                else -> emptyColor
            }
            drawRect(color = EmberPalette.Shadow, topLeft = Offset(x, 0f), size = Size(segmentW, size.height))
            val inset = 1.dp.toPx()
            drawRect(
                color = color,
                topLeft = Offset(x + inset, inset),
                size = Size((segmentW - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
            )
        }
    }
}
