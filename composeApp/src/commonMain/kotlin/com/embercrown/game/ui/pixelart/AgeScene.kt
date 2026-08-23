package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Layout is authored in coarse units and multiplied by [U] to reach the final grid. Raising [U]
 * buys a finer dither (the gradient steps land closer together) at no draw cost, because the art
 * is rasterized to a bitmap once and blitted.
 */
// U=3 (was 2) for a finer dither; SCENE_H=54 (was 36) brings the canvas from a 2.22:1 banner
// crop down to ~1.48:1 — close to what the redesign's Contain-fit stage box actually offers, so
// the art fills the frame instead of floating letterboxed in a mostly-empty one. HORIZON keeps
// the same ~75% sky / 25% ground split; the settlement itself (wallHeight/towerHeight below)
// stays at its old absolute height, so the extra sky reads as a grander, more distant vista
// rather than a stretched settlement.
private const val U = 3
private const val SCENE_W = 80 * U
private const val SCENE_H = 54 * U
private const val HORIZON = 41 * U

// Palette key groups, all mutually distinct so materials never collide.
private const val SKY = "12345678"
private const val GLOW = "qrstuv"
private const val HILL = "wxy"
private const val GROUND = "ABCD"
private const val WALL = "EFGH"
private const val ROOF = "IJKL"
private const val TOWER = "MNOP"
private const val TOWER_ROOF = "abcd"
private const val STAR = '*'
private const val STAR_DIM = '+'
private const val DOOR = '#'
private const val WINDOW = '@'

private enum class RoofStyle { TENT, PEAK, WALLED }

private data class SceneParams(
    val baseWidth: Int,
    val wallHeight: Int,
    val towerCount: Int,
    val towerHeight: Int,
    val roofStyle: RoofStyle,
    val night: Boolean,
    val starCount: Int,
    val windows: Boolean,
    val glow: Boolean,
    val radiant: Boolean,
)

private fun paramsFor(index: Int): SceneParams = when {
    index <= 1 -> SceneParams(20, 6, 0, 0, RoofStyle.TENT, false, 0, false, false, false)
    index <= 3 -> SceneParams(26, 8, 0, 0, RoofStyle.PEAK, false, 0, false, false, false)
    index <= 5 -> SceneParams(32, 9, 0, 0, RoofStyle.PEAK, false, 20, true, false, false)
    index <= 7 -> SceneParams(38, 9, 2, 7, RoofStyle.WALLED, false, 34, true, false, false)
    index <= 9 -> SceneParams(42, 10, 2, 9, RoofStyle.WALLED, false, 52, true, false, false)
    index <= 11 -> SceneParams(46, 10, 3, 10, RoofStyle.WALLED, false, 76, true, false, false)
    index <= 13 -> SceneParams(48, 10, 3, 11, RoofStyle.WALLED, true, 110, true, false, false)
    index <= 15 -> SceneParams(50, 10, 4, 11, RoofStyle.WALLED, true, 150, true, true, false)
    else -> SceneParams(52, 10, 5, 11, RoofStyle.WALLED, true, 190, true, true, true)
}

/** Integer hash — scatters stars properly; plain modulo strides line them up into diagonals. */
private fun hash(n: Int): Int {
    var h = n * 374761393 + 668265263
    h = (h xor (h shr 13)) * 1274126177
    return (h xor (h shr 16)) and 0x7FFFFFFF
}

/**
 * Builds a wide landscape whose settlement, sky and mood shift as `index` climbs from the first
 * age (a lone tent at dusk) to the last (a radiant citadel under a full starfield). Surfaces are
 * shaded across multi-step dithered ramps, and each element is lit according to its own form:
 * walls grade vertically, roofs split left-to-right, and towers get a cylindrical falloff around
 * their own axis, which is what keeps the architecture from looking like one diagonal smear.
 */
fun ageSceneArt(index: Int, ageCount: Int): PixelArt {
    val t = index / (ageCount - 1).coerceAtLeast(1).toDouble()
    val p = paramsFor(index)
    val g = PixelGridBuilder(SCENE_W, SCENE_H)

    val skyTop: Color
    val skyHorizon: Color
    if (p.night) {
        val nt = ((t - 0.7) / 0.3).coerceIn(0.0, 1.0)
        skyTop = lerpColor(Color(0xFF0D0A20), Color(0xFF05040E), nt)
        skyHorizon = lerpColor(Color(0xFF3A2A52), Color(0xFF1E1636), nt)
    } else {
        val dt = (t / 0.7).coerceIn(0.0, 1.0)
        skyTop = lerpColor(Color(0xFF1C1208), Color(0xFF120C22), dt)
        skyHorizon = lerpColor(Color(0xFF5A3316), Color(0xFF43305C), dt)
    }

    // Sky: an eight-step vertical ramp at full smoothness. Across an area this large the step
    // count is what matters — with too few steps the dither seams read as hard scanline stripes.
    g.rect(0, 0, SCENE_W - 1, HORIZON, '1')
    g.ditherRamp('1', SKY, linearFalloff(from = 0f, to = HORIZON.toFloat()), sharpness = 1f)

    val baseWidth = p.baseWidth * U
    val wallHeight = p.wallHeight * U
    val towerHeight = p.towerHeight * U
    val towerRoofH = 4 * U
    val towerHalf = 2 * U

    val wallBottom = HORIZON
    val wallX0 = (SCENE_W - baseWidth) / 2
    val wallX1 = wallX0 + baseWidth - 1
    val centerX = (wallX0 + wallX1) / 2
    val wallTop = wallBottom - wallHeight
    val towerTop = wallTop - towerHeight
    val towers = if (p.towerCount > 0) towerPositions(wallX0, wallX1, p.towerCount) else emptyList()

    if (p.glow) {
        val radius = (if (p.radiant) 24 else 17) * U
        val cy = wallTop - 2 * U
        g.circle(centerX, cy, radius, 'q')
        g.ditherRamp(
            'q',
            GLOW,
            radialFalloff(cx = centerX.toFloat(), cy = cy.toFloat(), radius = radius.toFloat()),
            sharpness = 1f,
        )
    }

    for (i in 0 until p.starCount) {
        val sx = hash(i * 2 + index * 7919) % SCENE_W
        val sy = hash(i * 2 + 1 + index * 7919) % (HORIZON - 3 * U)
        g.set(sx, sy, if (hash(i + 31) % 4 == 0) STAR else STAR_DIM)
    }

    // Daytime skies got noticeably taller with the wider HORIZON headroom above — a few flat
    // cloud puffs (three overlapping ellipses each) keep that space from reading as empty.
    val cloudColor = lerpColor(skyHorizon, Color(0xFFFFD9A0), 0.45)
    if (!p.night) {
        for (i in 0 until 3) {
            val cx = (hash(index * 5081 + i * 2) % (SCENE_W - 20 * U)) + 10 * U
            val cy = (hash(index * 5081 + i * 2 + 1) % (HORIZON / 2)) + 4 * U
            g.ellipse(cx, cy, 7 * U, 2 * U, 'z')
            g.ellipse(cx - 5 * U, cy + U, 4 * U, 2 * U, 'z')
            g.ellipse(cx + 5 * U, cy + U, 4 * U, 2 * U, 'z')
        }
    }

    g.triangle(-6 * U, HORIZON, 26 * U, HORIZON, 10 * U, HORIZON - 9 * U, 'w')
    g.triangle(16 * U, HORIZON, 48 * U, HORIZON, 32 * U, HORIZON - 6 * U, 'w')
    g.triangle(52 * U, HORIZON, 86 * U, HORIZON, 68 * U, HORIZON - 11 * U, 'w')
    g.ditherRamp('w', HILL, linearFalloff(from = HORIZON.toFloat(), to = (HORIZON - 12 * U).toFloat()))

    g.rect(0, HORIZON + 1, SCENE_W - 1, SCENE_H - 1, 'A')
    g.ditherRamp('A', GROUND, linearFalloff(from = (SCENE_H - 1).toFloat(), to = HORIZON.toFloat()))

    // --- Settlement -------------------------------------------------------------------
    when (p.roofStyle) {
        RoofStyle.TENT -> {
            g.triangle(wallX0, wallBottom, wallX1, wallBottom, centerX, wallBottom - wallHeight - 5 * U, 'I')
            g.triangle(centerX - U, wallBottom, centerX + U, wallBottom, centerX, wallBottom - 4 * U, DOOR)
        }
        RoofStyle.PEAK -> {
            g.rect(wallX0, wallTop, wallX1, wallBottom, 'E')
            g.triangle(wallX0 - 2 * U, wallTop, wallX1 + 2 * U, wallTop, centerX, wallTop - wallHeight / 2 - 3 * U, 'I')
            g.rect(centerX - 2 * U, wallBottom - 5 * U, centerX + 2 * U - 1, wallBottom, DOOR)
        }
        RoofStyle.WALLED -> {
            g.rect(wallX0, wallTop, wallX1, wallBottom, 'E')
            var mx = wallX0
            while (mx <= wallX1) {
                g.rect(mx, wallTop - 2 * U, mx + 2 * U - 1, wallTop, 'E')
                mx += 4 * U
            }
            g.rect(centerX - 3 * U, wallBottom - 7 * U, centerX + 3 * U - 1, wallBottom, DOOR)
            towers.forEach { tx ->
                g.rect(tx - towerHalf, towerTop, tx + towerHalf, wallTop, 'M')
                g.triangle(tx - 2 * towerHalf, towerTop, tx + 2 * towerHalf, towerTop, tx, towerTop - towerRoofH, 'a')
            }
        }
    }

    // Walls grade from shadow at the base to sunlight along the battlements.
    g.ditherRamp('E', WALL, linearFalloff(from = wallBottom.toFloat(), to = (wallTop - 2 * U).toFloat()))
    // The main roof takes a straight left-to-right sunlit split.
    g.ditherRamp('I', ROOF, linearFalloff(from = wallX1.toFloat(), to = wallX0.toFloat(), vertical = false))

    if (towers.isNotEmpty()) {
        // Each tower is shaded around its own axis, so every one reads as a lit cylinder.
        val cylindrical: (Int, Int) -> Float = { x, _ ->
            val nearest = towers.minByOrNull { abs(it - x) } ?: x
            (0.5f - (x - nearest).toFloat() / (2f * towerHalf)).coerceIn(0f, 1f)
        }
        g.ditherRamp('M', TOWER, cylindrical)
        g.ditherRamp('a', TOWER_ROOF, cylindrical)
    }

    if (p.windows && p.roofStyle != RoofStyle.TENT) {
        var wx = wallX0 + 3 * U
        while (wx <= wallX1 - 3 * U) {
            if (wx < centerX - 4 * U || wx > centerX + 3 * U) {
                g.rect(wx, wallTop + 3 * U, wx + U, wallTop + 4 * U, WINDOW)
            }
            wx += 6 * U
        }
        towers.forEach { tx ->
            g.rect(tx - U, towerTop + 3 * U, tx, towerTop + 4 * U, WINDOW)
        }
    }

    val wallBase = lerpColor(Color(0xFF6E4A24), Color(0xFF5C5C68), t)
    val roofBase = lerpColor(Color(0xFF5E2018), Color(0xFF9A7328), t)
    val towerBase = lerpColor(Color(0xFF4A4046), Color(0xFF6E6E7C), t)
    val groundBase = lerpColor(Color(0xFF223012), Color(0xFF2C2824), t)
    val windowGlow = lerpColor(Color(0xFFE8A63C), Color(0xFFFFE9A8), t)

    // Highlights lean toward a warm sunlit tone instead of white, which keeps the materials
    // saturated — lightening straight to white is what makes shaded pixel art look chalky.
    val sunlight = Color(0xFFFFD9A0)
    val palette = rampPalette(SKY, skyTop, skyHorizon) +
        rampPalette(GLOW, skyHorizon, if (p.radiant) Color(0xFFE8B45C) else Color(0xFF7A5A8C)) +
        rampPalette(HILL, lerpColor(skyHorizon, Color.Black, 0.78), lerpColor(skyHorizon, Color.Black, 0.5)) +
        rampPalette(GROUND, lerpColor(groundBase, Color.Black, 0.5), groundBase) +
        rampPalette(WALL, lerpColor(wallBase, Color.Black, 0.62), lerpColor(wallBase, sunlight, 0.3)) +
        rampPalette(ROOF, lerpColor(roofBase, Color.Black, 0.62), lerpColor(roofBase, sunlight, 0.34)) +
        rampPalette(TOWER, lerpColor(towerBase, Color.Black, 0.66), lerpColor(towerBase, sunlight, 0.32)) +
        rampPalette(TOWER_ROOF, lerpColor(roofBase, Color.Black, 0.66), lerpColor(roofBase, sunlight, 0.3)) +
        mapOf(
            STAR to EmberPalette.White,
            STAR_DIM to lerpColor(skyHorizon, EmberPalette.White, 0.45),
            DOOR to Color(0xFF0E0A10),
            WINDOW to windowGlow,
            'z' to cloudColor,
        )
    return g.build(palette)
}

private fun towerPositions(x0: Int, x1: Int, count: Int): List<Int> {
    if (count <= 1) return listOf((x0 + x1) / 2)
    val step = (x1 - x0).toDouble() / (count - 1)
    return (0 until count).map { i -> (x0 + step * i).toInt() }
}
