package com.embercrown.game.ui.pixelart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A small bitmap: `cells[y][x]` holds a palette key, `'.'` means transparent. */
class PixelArt(val width: Int, val height: Int, private val cells: Array<CharArray>, val palette: Map<Char, Color>) {
    fun colorAt(x: Int, y: Int): Color? {
        val key = cells[y][x]
        if (key == '.') return null
        return palette[key]
    }
}

/** Ordered 4x4 Bayer matrix — the threshold pattern behind every dithered gradient here. */
private val BAYER_4X4 = arrayOf(
    intArrayOf(0, 8, 2, 10),
    intArrayOf(12, 4, 14, 6),
    intArrayOf(3, 11, 1, 9),
    intArrayOf(15, 7, 13, 5),
)

/** Builds a [PixelArt] using simple shape primitives instead of hand-typed ASCII rows. */
class PixelGridBuilder(private val width: Int, private val height: Int) {
    private val cells = Array(height) { CharArray(width) { '.' } }

    fun set(x: Int, y: Int, c: Char): PixelGridBuilder {
        if (x in 0 until width && y in 0 until height) cells[y][x] = c
        return this
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Char): PixelGridBuilder {
        for (y in max(0, min(y0, y1))..min(height - 1, max(y0, y1))) {
            for (x in max(0, min(x0, x1))..min(width - 1, max(x0, x1))) cells[y][x] = c
        }
        return this
    }

    fun rectOutline(x0: Int, y0: Int, x1: Int, y1: Int, c: Char): PixelGridBuilder {
        for (x in x0..x1) { set(x, y0, c); set(x, y1, c) }
        for (y in y0..y1) { set(x0, y, c); set(x1, y, c) }
        return this
    }

    fun circle(cx: Int, cy: Int, r: Int, c: Char): PixelGridBuilder {
        for (y in (cy - r)..(cy + r)) for (x in (cx - r)..(cx + r)) {
            val dx = x - cx
            val dy = y - cy
            if (dx * dx + dy * dy <= r * r) set(x, y, c)
        }
        return this
    }

    /** An axis-aligned filled ellipse — the workhorse for rounded organic shapes. */
    fun ellipse(cx: Int, cy: Int, rx: Int, ry: Int, c: Char): PixelGridBuilder {
        for (y in (cy - ry)..(cy + ry)) for (x in (cx - rx)..(cx + rx)) {
            val dx = (x - cx).toFloat() / rx
            val dy = (y - cy).toFloat() / ry
            if (dx * dx + dy * dy <= 1f) set(x, y, c)
        }
        return this
    }

    fun ring(cx: Int, cy: Int, rOuter: Int, rInner: Int, c: Char): PixelGridBuilder {
        for (y in (cy - rOuter)..(cy + rOuter)) for (x in (cx - rOuter)..(cx + rOuter)) {
            val d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (d2 <= rOuter * rOuter && d2 >= rInner * rInner) set(x, y, c)
        }
        return this
    }

    fun triangle(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int, c: Char): PixelGridBuilder {
        fun sign(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int) = (ax - cx) * (by - cy) - (bx - cx) * (ay - cy)
        val minX = minOf(x0, x1, x2)
        val maxX = maxOf(x0, x1, x2)
        val minY = minOf(y0, y1, y2)
        val maxY = maxOf(y0, y1, y2)
        for (y in minY..maxY) for (x in minX..maxX) {
            val d1 = sign(x, y, x0, y0, x1, y1)
            val d2 = sign(x, y, x1, y1, x2, y2)
            val d3 = sign(x, y, x2, y2, x0, y0)
            val hasNeg = d1 < 0 || d2 < 0 || d3 < 0
            val hasPos = d1 > 0 || d2 > 0 || d3 > 0
            if (!(hasNeg && hasPos)) set(x, y, c)
        }
        return this
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Char, thickness: Int = 1): PixelGridBuilder {
        var xa = x0
        var ya = y0
        val dx = abs(x1 - x0)
        val sx = if (x0 < x1) 1 else -1
        val dy = -abs(y1 - y0)
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        val half = thickness / 2
        while (true) {
            for (ox in -half..half) for (oy in -half..half) set(xa + ox, ya + oy, c)
            if (xa == x1 && ya == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; xa += sx }
            if (e2 <= dx) { err += dx; ya += sy }
        }
        return this
    }

    /**
     * Shades every cell holding [maskChar] across a multi-step color [ramp] (dark→light chars),
     * dithering between each adjacent pair so the transition reads as a smooth gradient rather
     * than visible bands. [t] returns the local brightness (`0f..1f`) per cell.
     *
     * This is the core of the art style: four or five tones per material, blended by an ordered
     * Bayer pattern, exactly how hand-drawn pixel art fakes gradients on a limited palette.
     */
    fun ditherRamp(
        maskChar: Char,
        ramp: String,
        t: (x: Int, y: Int) -> Float,
        sharpness: Float = 2.4f,
    ): PixelGridBuilder {
        if (ramp.length < 2) return this
        for (y in 0 until height) for (x in 0 until width) {
            if (cells[y][x] != maskChar) continue
            val p = t(x, y).coerceIn(0f, 1f) * (ramp.length - 1)
            val lo = p.toInt().coerceAtMost(ramp.length - 2)
            // Pushing frac toward 0 or 1 confines the checkerboard to a narrow seam between
            // bands, leaving broad areas solid. Flat dithering everywhere reads as noise;
            // sharpness = 1f keeps the full, smooth blend for large areas like the sky.
            val frac = (((p - lo) - 0.5f) * sharpness + 0.5f).coerceIn(0f, 1f)
            val threshold = BAYER_4X4[y % 4][x % 4] / 16f
            cells[y][x] = if (frac > threshold) ramp[lo + 1] else ramp[lo]
        }
        return this
    }

    /**
     * Draws [outlineChar] into every background cell (a char listed in [backgroundChars]) that
     * touches a non-background cell, wrapping subjects in a dark contour the way sprite art does.
     * Runs off a snapshot so freshly drawn outline pixels don't cascade outward.
     */
    fun outlineAgainst(backgroundChars: String, outlineChar: Char): PixelGridBuilder {
        val snapshot = Array(height) { cells[it].copyOf() }
        fun isBg(x: Int, y: Int): Boolean {
            if (x !in 0 until width || y !in 0 until height) return true
            return snapshot[y][x] in backgroundChars
        }
        for (y in 0 until height) for (x in 0 until width) {
            if (!isBg(x, y)) continue
            val touchesSubject = !isBg(x - 1, y) || !isBg(x + 1, y) || !isBg(x, y - 1) || !isBg(x, y + 1)
            if (touchesSubject) cells[y][x] = outlineChar
        }
        return this
    }

    fun build(palette: Map<Char, Color>): PixelArt = PixelArt(width, height, cells, palette)
}

/** Radial falloff, `1f` at the center fading to `0f` at/beyond `radius`. */
fun radialFalloff(cx: Float, cy: Float, radius: Float): (Int, Int) -> Float = { x, y ->
    val dx = x + 0.5f - cx
    val dy = y + 0.5f - cy
    val d = kotlin.math.sqrt(dx * dx + dy * dy)
    (1f - d / radius).coerceIn(0f, 1f)
}

/** Linear gradient along Y (or X when [vertical] is false), `0f` at [from] to `1f` at [to]. */
fun linearFalloff(from: Float, to: Float, vertical: Boolean = true): (Int, Int) -> Float = { x, y ->
    val p = if (vertical) y + 0.5f else x + 0.5f
    ((p - from) / (to - from)).coerceIn(0f, 1f)
}

/** Directional light: brightest where the surface faces (dx, dy), used for lit walls and roofs. */
fun directionalFalloff(originX: Float, originY: Float, dirX: Float, dirY: Float, reach: Float): (Int, Int) -> Float =
    { x, y ->
        val d = (x + 0.5f - originX) * dirX + (y + 0.5f - originY) * dirY
        (0.5f + d / reach).coerceIn(0f, 1f)
    }

/** Builds `ramp.length` evenly spaced colors from [dark] to [light], keyed by the ramp's chars. */
fun rampPalette(ramp: String, dark: Color, light: Color): Map<Char, Color> =
    ramp.mapIndexed { i, c -> c to lerpColor(dark, light, i.toDouble() / (ramp.length - 1)) }.toMap()

/** Linear-interpolates two colors; `t` in `0f..1f`. */
fun lerpColor(a: Color, b: Color, t: Double): Color {
    val ct = t.coerceIn(0.0, 1.0).toFloat()
    return Color(
        red = a.red + (b.red - a.red) * ct,
        green = a.green + (b.green - a.green) * ct,
        blue = a.blue + (b.blue - a.blue) * ct,
        alpha = a.alpha + (b.alpha - a.alpha) * ct,
    )
}

/** Stable per-cell noise (0f..1f) — the dissolve pattern behind [PixelArt.corrupted]. */
private fun corruptionNoise(x: Int, y: Int): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h shr 13)) * 1274126177
    h = h xor (h shr 16)
    return (h and 0x7FFFFFFF) / Int.MAX_VALUE.toFloat()
}

// Private-use codepoints so the corruption ramp's palette keys never collide with the
// synthesized per-color keys assigned below (which start at SYNTH_CHAR_BASE).
private val CORRUPTION_RAMP = String(charArrayOf(0xE000.toChar(), 0xE001.toChar(), 0xE002.toChar()))
private const val SYNTH_CHAR_BASE = 0x100

/**
 * Dissolves [progress] (0f..1f) of this art into a dark corruption ramp. Ground and the left/right
 * edges corrupt first, so the decay reads as creeping in from outside rather than a uniform fade.
 * Built purely off the public [PixelArt.colorAt] API: every surviving pixel is re-keyed into a
 * palette synthesized on the fly, so no access to the private cell grid is needed.
 */
fun PixelArt.corrupted(progress: Float): PixelArt {
    if (progress <= 0f) return this
    val p = progress.coerceIn(0f, 1f)
    val colorToChar = mutableMapOf<Color, Char>()
    var nextChar = SYNTH_CHAR_BASE
    val newCells = Array(height) { y ->
        CharArray(width) { x ->
            val base = colorAt(x, y) ?: return@CharArray '.'
            val groundBias = ((y - height * 0.55f) / (height * 0.45f)).coerceIn(0f, 1f)
            val edgeBias = 1f - (min(x, width - 1 - x).toFloat() / (width * 0.25f)).coerceIn(0f, 1f)
            val bias = max(groundBias, edgeBias) * 0.6f
            val threshold = (corruptionNoise(x, y) - bias).coerceIn(0f, 1f)
            if (threshold >= p) {
                colorToChar.getOrPut(base) { (nextChar++).toChar() }
            } else {
                val depth = (1f - threshold / p.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
                CORRUPTION_RAMP[(depth * (CORRUPTION_RAMP.length - 1)).roundToInt()]
            }
        }
    }
    val palette = colorToChar.entries.associate { (color, char) -> char to color } +
        rampPalette(CORRUPTION_RAMP, Ramps.CorruptionDark, Ramps.CorruptionLight)
    return PixelArt(width, height, newCells, palette)
}

/** Rasterizes the art once into a real bitmap so it can be blitted nearest-neighbor. */
fun PixelArt.toImageBitmap(): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val paint = Paint()
    for (y in 0 until height) for (x in 0 until width) {
        val color = colorAt(x, y) ?: continue
        paint.color = color
        canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
    }
    return bitmap
}

/** How a [PixelArtImage] fills its box: fit entirely inside it, or fill it and overflow. */
enum class PixelFit { Contain, Cover }

/**
 * Renders a [PixelArt] as crisp, nearest-neighbor scaled pixels. The art is rasterized once
 * into an [ImageBitmap] and blitted with [FilterQuality.None], so there is no blurring and no
 * seam between neighboring cells at any scale. Aspect ratio is always preserved; use
 * [PixelFit.Cover] (with `Modifier.clipToBounds()`) for edge-to-edge backdrops.
 */
@Composable
fun PixelArtImage(art: PixelArt, modifier: Modifier = Modifier, fit: PixelFit = PixelFit.Contain) {
    val bitmap = remember(art) { art.toImageBitmap() }
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val scaleX = size.width / art.width
        val scaleY = size.height / art.height
        val scale = if (fit == PixelFit.Cover) max(scaleX, scaleY) else min(scaleX, scaleY)
        val drawW = (art.width * scale).roundToInt().coerceAtLeast(1)
        val drawH = (art.height * scale).roundToInt().coerceAtLeast(1)
        drawImage(
            image = bitmap,
            srcSize = IntSize(art.width, art.height),
            dstOffset = IntOffset(((size.width - drawW) / 2f).roundToInt(), ((size.height - drawH) / 2f).roundToInt()),
            dstSize = IntSize(drawW, drawH),
            filterQuality = FilterQuality.None,
        )
    }
}
