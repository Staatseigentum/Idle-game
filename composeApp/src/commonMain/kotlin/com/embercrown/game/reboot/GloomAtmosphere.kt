package com.embercrown.game.reboot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

internal data class GloomVisuals(val stage: Int, val pressure: Float)

/** Gloom advances in clear visual chapters, while its actual pressure stays continuous. */
internal fun gloomVisuals(gloom: Double): GloomVisuals {
    val safe = gloom.coerceIn(0.0, 100.0)
    val stage = when {
        safe < 15.0 -> 0
        safe < 35.0 -> 1
        safe < 55.0 -> 2
        safe < 75.0 -> 3
        safe < 90.0 -> 4
        else -> 5
    }
    return GloomVisuals(stage, ((safe - 15.0) / 85.0).toFloat().coerceIn(0f, 1f))
}

/** Pointer-transparent, bitmap-free corruption shared by phone and desktop. */
@Composable
internal fun GloomAtmosphere(gloom: Double) {
    val bucket = (gloom.coerceIn(0.0, 100.0) / 5).toInt()
    val visual = remember(bucket) { gloomVisuals(bucket * 5.0) }
    if (visual.stage == 0) return

    Box(Modifier.fillMaxSize()) {
        // The animated detail is isolated from the static geometry and the game UI.
        Canvas(Modifier.fillMaxSize()) { drawGloomScars(visual) }
        if (visual.stage >= 2) GloomPulse(visual)
    }
}

private fun DrawScope.drawGloomScars(visual: GloomVisuals) {
    val pressure = visual.pressure
    val w = size.width
    val h = size.height
    val unit = min(w, h)
    val depth = min(unit * (0.12f + pressure * 0.24f), w * 0.24f)
    val accent = lerp(AshPalette.violet, AshPalette.crimson,
        ((pressure - 0.18f) / 0.82f).coerceIn(0f, 1f))
    val shadow = Color(0xFF03040B)
    val edgeAlpha = 0.17f + pressure * 0.48f

    // The realm loses light first; the centre stays bright enough to read and interact with.
    drawRect(Color(0xFF130A17).copy(alpha = 0.018f + pressure * 0.105f))
    drawRect(Brush.horizontalGradient(listOf(shadow.copy(alpha = edgeAlpha), Color.Transparent),
        startX = 0f, endX = depth), size = Size(depth, h))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, shadow.copy(alpha = edgeAlpha)),
        startX = w - depth, endX = w), Offset(w - depth, 0f), Size(depth, h))
    drawRect(Brush.verticalGradient(listOf(accent.copy(alpha = edgeAlpha * 0.61f), Color.Transparent),
        startY = 0f, endY = depth * 0.8f), size = Size(w, depth * 0.8f))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, shadow.copy(alpha = edgeAlpha * 0.77f)),
        startY = h - depth * 0.7f, endY = h), Offset(0f, h - depth * 0.7f), Size(w, depth * 0.7f))

    // Stepped silhouettes grow out of both sides like a living, pixelated siege.
    if (visual.stage >= 2) {
        repeat(2) { side ->
            val path = Path().apply {
                val fromLeft = side == 0
                moveTo(if (fromLeft) 0f else w, 0f)
                for (segment in 0..32) {
                    val y = h * segment / 32f
                    val irregular = ((segment * 17 + segment * segment * 7 + side * 11) % 13) / 13f
                    val fang = if (segment % 7 in 2..3) 0.32f else 0f
                    val intrusion = depth * (0.13f + pressure * (0.19f + irregular * 0.23f + fang))
                    val x = if (fromLeft) intrusion else w - intrusion
                    lineTo(x, y)
                    if (segment < 32) lineTo(x, h * (segment + 1) / 32f)
                }
                lineTo(if (fromLeft) 0f else w, h)
                close()
            }
            drawPath(path, shadow.copy(alpha = (pressure - 0.1f).coerceAtLeast(0f) * 0.48f))
        }
    }

    // Slivers of the old world glitch out in horizontal, rigid pixel strips.
    val count = 5 + visual.stage * 7
    repeat(count) { i ->
        val y = h * ((i * 37 + i * i * 11 + 17) % 99) / 100f
        val length = depth * (0.16f + i % 5 * 0.105f)
        val thickness = (1 + i % 3) * (1.5f + pressure * 2.5f).dp.toPx()
        val x = if (i % 2 == 0) 0f else w - length
        val tint = if (i % 4 == 0) accent else shadow
        drawRect(tint.copy(alpha = 0.10f + pressure * 0.36f), Offset(x, y), Size(length, thickness))
        if (visual.stage >= 4 && i % 3 == 0) {
            drawRect(AshPalette.crimson.copy(alpha = pressure * 0.38f),
                Offset(x, y + thickness), Size(length * 0.43f, 1.dp.toPx()))
        }
    }

    if (visual.stage >= 3) {
        // Cracked crimson veins fork along the margins, but never across central controls.
        val vein = AshPalette.crimson.copy(alpha = (pressure - 0.3f) * 0.58f)
        repeat(8) { i ->
            val left = i % 2 == 0
            val edge = if (left) 0f else w
            val direction = if (left) 1f else -1f
            val y = h * (0.085f + i * 0.116f)
            val a = Offset(edge, y)
            val b = Offset(edge + direction * depth * (0.21f + i % 3 * 0.08f), y + unit * 0.042f)
            val c = Offset(b.x + direction * depth * 0.12f, b.y - unit * 0.018f)
            drawLine(vein, a, b, strokeWidth = (1f + pressure * 2f).dp.toPx(), cap = StrokeCap.Square)
            drawLine(vein, b, c, strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Square)
            drawLine(vein.copy(alpha = vein.alpha * 0.65f), b,
                Offset(b.x - direction * depth * 0.05f, b.y + unit * 0.046f),
                strokeWidth = 1.dp.toPx())
        }
    }

    if (visual.stage >= 4) {
        val glyphColor = accent.copy(alpha = (pressure - 0.48f) * 0.55f)
        val radius = min(unit * 0.052f, depth * 0.47f)
        listOf(Offset(depth * 0.47f, h * 0.24f), Offset(w - depth * 0.47f, h * 0.72f),
            Offset(depth * 0.53f, h * 0.8f), Offset(w - depth * 0.55f, h * 0.19f)).forEachIndexed { i, centre ->
            drawCircle(glyphColor, radius, centre, style = Stroke(width = 1.5f.dp.toPx()))
            drawCircle(glyphColor.copy(alpha = glyphColor.alpha * 0.55f), radius * 0.72f,
                centre, style = Stroke(width = 1.dp.toPx()))
            drawLine(glyphColor, Offset(centre.x - radius * 0.68f, centre.y),
                Offset(centre.x + radius * 0.68f, centre.y), strokeWidth = 1.dp.toPx())
            drawLine(glyphColor, Offset(centre.x, centre.y - radius * 0.7f),
                Offset(centre.x + (if (i % 2 == 0) 1 else -1) * radius * 0.36f,
                    centre.y + radius * 0.65f), strokeWidth = 1.dp.toPx())
        }
    }

    if (visual.stage == 5) {
        // A broken red crown around the viewport marks the point of near-collapse.
        val rim = AshPalette.crimson.copy(alpha = 0.45f)
        val line = 3.dp.toPx()
        repeat(2) { side ->
            val x = if (side == 0) 0f else w - line
            for (i in 0..10) if (i % 4 != 2) {
                drawRect(rim, Offset(x, h * i / 11f), Size(line, h / 11f - 2.dp.toPx()))
            }
        }
        for (i in 0..19) if (i % 5 != 3) {
            drawRect(rim, Offset(w * i / 20f, 0f), Size(w / 20f - 2.dp.toPx(), line))
            drawRect(rim.copy(alpha = 0.28f), Offset(w * i / 20f, h - line),
                Size(w / 20f - 2.dp.toPx(), line))
        }
    }
}

@Composable
private fun GloomPulse(visual: GloomVisuals) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(visual.stage) {
        while (true) {
            delay(if (visual.stage >= 4) 170L else 290L)
            frame = (frame + 1) % 120
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val pressure = visual.pressure
        val w = size.width
        val h = size.height
        val margin = min(w, h) * (0.07f + pressure * 0.13f)
        val pulse = ((sin(frame * 0.18f) + 1f) * 0.5f).coerceIn(0f, 1f)
        val glow = AshPalette.crimson.copy(alpha = (0.11f + pulse * 0.33f) * pressure)
        val count = 11 + visual.stage * 8

        // Embers and ash drift vertically against the ruined edges; fixed integer seeds avoid
        // allocation and random flicker, while changing only this small transparent canvas.
        repeat(count) { i ->
            val drift = (frame * (1 + i % 3) + i * 23) % 127
            val y = h * ((i * 41 + 113 - drift + 127) % 127) / 127f
            val edge = margin * (0.08f + (i * 17 % 29) / 34f)
            val x = if (i % 2 == 0) edge else w - edge
            val sideFlicker = 0.45f + ((frame + i * 7) % 12) / 24f
            val tone = if (i % 5 == 0) AshPalette.flame else AshPalette.crimson
            val pixel = (if (i % 7 == 0) 4f else 2f).dp.toPx()
            drawRect(tone.copy(alpha = pressure * sideFlicker * 0.7f),
                Offset(x, y), Size(pixel, pixel * (1f + i % 3)))
        }

        if (visual.stage >= 4) {
            // The scars breathe: only their highlights animate, not the dark geometry itself.
            repeat(6) { i ->
                val y = h * (0.12f + i * 0.145f)
                val x = if (i % 2 == 0) margin * 0.15f else w - margin * 0.15f
                val direction = if (i % 2 == 0) 1f else -1f
                val reach = margin * (0.31f + pulse * 0.18f)
                drawLine(glow, Offset(x, y), Offset(x + direction * reach, y + margin * 0.2f),
                    strokeWidth = (1f + pulse * 2f).dp.toPx(), cap = StrokeCap.Square)
                drawRect(glow, Offset(x + direction * reach, y + margin * 0.2f),
                    Size(3.dp.toPx(), 3.dp.toPx()))
            }
            val band = (frame * 7 % 100) / 100f * h
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = glow.alpha * 0.21f),
                Color.Transparent), startY = band - 14.dp.toPx(), endY = band + 14.dp.toPx()),
                Offset(0f, band - 14.dp.toPx()), Size(w, 28.dp.toPx()))
        }
        if (visual.stage == 5) {
            val edge = AshPalette.crimson.copy(alpha = 0.16f + pulse * 0.27f)
            val thickness = (3f + pulse * 4f).dp.toPx()
            drawRect(edge, Offset.Zero, Size(thickness, h))
            drawRect(edge, Offset(w - thickness, 0f), Size(thickness, h))
        }
    }
}
