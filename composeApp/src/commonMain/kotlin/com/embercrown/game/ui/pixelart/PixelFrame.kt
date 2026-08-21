package com.embercrown.game.ui.pixelart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
): Modifier = this
    .background(EmberPalette.Shadow)
    .padding(borderWidth)
    .background(fill)
    .drawBehind {
        val light = if (raised) EmberPalette.BevelLight else EmberPalette.Shadow
        val dark = if (raised) EmberPalette.Shadow else EmberPalette.BevelLight
        val s = 1.5.dp.toPx()
        drawLine(light, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = s)
        drawLine(light, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = s)
        drawLine(dark, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = s)
        drawLine(dark, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = s)
    }

/** A pixel-framed clickable button that sinks its bevel while pressed. */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = EmberPalette.Accent,
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val effectiveFill = if (enabled) containerColor else containerColor.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .pixelFrame(fill = effectiveFill, raised = enabled && !pressed, borderWidth = 2.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** A chunky, segmented pixel progress bar (distinct blocks with a gap, not a smooth fill). */
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
