package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color

/**
 * A small heraldic shield portrait for the current age, shown in the Leiter rail next to the
 * age list. Distinct from [ageSceneArt] (a wide landscape) — this is a portrait-aspect crest
 * that grows more ornate as the age climbs: a bare field with a single ember at first, gaining a
 * metal border, a fuller flame charge, a crown, and finally a radiant halo with corner gems.
 */
private const val CU = 3
private const val CREST_W = 30 * CU
private const val CREST_H = 38 * CU

private const val FIELD = "ABCD"
private const val BORDER = "EFG"
private const val FLAME = "HIJK"
private const val CROWN = "LMN"
private const val GLOW = "opqr"
private const val GEM = '*'
private const val OUTLINE = '#'

private fun bucketFor(index: Int): Int = when {
    index <= 2 -> 0
    index <= 5 -> 1
    index <= 9 -> 2
    index <= 13 -> 3
    else -> 4
}

fun ageCrestArt(index: Int, ageCount: Int): PixelArt {
    val t = index / (ageCount - 1).coerceAtLeast(1).toDouble()
    val bucket = bucketFor(index)
    val g = PixelGridBuilder(CREST_W, CREST_H)

    val x0 = 3 * CU
    val x1 = CREST_W - 1 - 3 * CU
    val centerX = (x0 + x1) / 2
    val shieldTop = 8 * CU
    val rectBottom = 26 * CU
    val tipY = 37 * CU

    // Radiant backdrop for the final bucket, drawn first so the shield silhouette sits on top.
    if (bucket == 4) {
        val r = 16 * CU
        val gy = shieldTop + 6 * CU
        g.circle(centerX, gy, r, 'o')
        g.ditherRamp('o', GLOW, radialFalloff(cx = centerX.toFloat(), cy = gy.toFloat(), radius = r.toFloat()), sharpness = 1f)
    }

    fun shield(left: Int, right: Int, top: Int, mid: Int, tip: Int, c: Char) {
        val cx = (left + right) / 2
        g.rect(left, top, right, mid, c)
        g.triangle(left, mid, right, mid, cx, tip, c)
    }

    // Border trim (bucket >= 1): a slightly larger shield in the border tone, with the field
    // shield inset on top of it — the same nested-fill outline trick used elsewhere.
    if (bucket >= 1) {
        val b = CU
        shield(x0 - b, x1 + b, shieldTop - b, rectBottom, tipY + b, 'E')
    }
    shield(x0, x1, shieldTop, rectBottom, tipY, 'A')

    // Central flame charge — a single ember at bucket 0, growing into a fuller blaze by the
    // final bucket. Ties the crest to the game's own ember/throne theme rather than an
    // arbitrary charge.
    val flameScale = when (bucket) {
        0 -> 0.32f
        1 -> 0.55f
        2 -> 0.78f
        3 -> 1.0f
        else -> 1.25f
    }
    val flameH = (14 * CU * flameScale).toInt().coerceAtLeast(2 * CU)
    val flameW = (9 * CU * flameScale).toInt().coerceAtLeast(2 * CU)
    val flameBaseY = rectBottom - 2 * CU
    g.triangle(centerX - flameW / 2, flameBaseY, centerX + flameW / 2, flameBaseY, centerX, flameBaseY - flameH, 'H')

    // Crown, from bucket 3: three simple points plus a base band above the shield.
    if (bucket >= 3) {
        val crownY1 = shieldTop - CU
        val crownY0 = crownY1 - 5 * CU
        val points = 3
        val pointW = (x1 - x0) / points
        for (i in 0 until points) {
            val px = x0 + pointW * i + pointW / 2
            g.triangle(px - pointW / 2, crownY1, px + pointW / 2, crownY1, px, crownY0, 'L')
        }
        g.rect(x0, crownY1 - CU, x1, crownY1, 'L')
    }

    // Corner gems, final bucket only.
    if (bucket == 4) {
        g.set(x0 + CU, shieldTop + CU, GEM)
        g.set(x1 - CU, shieldTop + CU, GEM)
    }

    g.outlineAgainst(".", OUTLINE)

    g.ditherRamp('A', FIELD, linearFalloff(from = tipY.toFloat(), to = shieldTop.toFloat()))
    if (bucket >= 1) g.ditherRamp('E', BORDER, linearFalloff(from = tipY.toFloat(), to = shieldTop.toFloat()))
    g.ditherRamp('H', FLAME, linearFalloff(from = flameBaseY.toFloat(), to = (flameBaseY - flameH).toFloat()))
    if (bucket >= 3) g.ditherRamp('L', CROWN, linearFalloff(from = (shieldTop - CU).toFloat(), to = (shieldTop - 6 * CU).toFloat()))

    val fieldBase = lerpColor(Color(0xFF4A2C18), Color(0xFF3A1F4A), t)
    val borderBase = lerpColor(Color(0xFF6B5A3E), Color(0xFFC9A24A), t)
    val crownBase = lerpColor(Color(0xFF8A7548), Color(0xFFF4D28C), t)
    val sunlight = Color(0xFFFFD9A0)

    val palette: Map<Char, Color> =
        rampPalette(FIELD, lerpColor(fieldBase, Color.Black, 0.55), lerpColor(fieldBase, sunlight, 0.32)) +
            rampPalette(BORDER, lerpColor(borderBase, Color.Black, 0.5), lerpColor(borderBase, Color.White, 0.3)) +
            rampPalette(FLAME, Ramps.FlameDark, Ramps.FlameLight) +
            rampPalette(CROWN, lerpColor(crownBase, Color.Black, 0.5), lerpColor(crownBase, Color.White, 0.35)) +
            rampPalette(GLOW, Color(0xFF2A1030), Color(0xFFE8B45C)) +
            mapOf(OUTLINE to Color(0xFF0E0A10), GEM to EmberPalette.White)
    return g.build(palette)
}
