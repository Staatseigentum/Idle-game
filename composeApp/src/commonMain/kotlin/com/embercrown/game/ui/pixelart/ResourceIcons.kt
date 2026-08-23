package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color

private const val S = 24

private const val RA = "1234"
private const val RB = "5678"
private const val RC = "abcd"

private const val OUTLINE = 'K'
private const val HIGHLIGHT = 'W'

/** Contours the sprite against empty space and assembles its palette. */
private fun PixelGridBuilder.finishFree(vararg ramps: Map<Char, Color>): PixelArt {
    outlineAgainst(".", OUTLINE)
    var palette: Map<Char, Color> = mapOf(OUTLINE to EmberPalette.Ink, HIGHLIGHT to EmberPalette.White)
    ramps.forEach { palette = palette + it }
    return build(palette)
}

/** A struck gold coin: shaded rim, domed face and an embossed crown. */
fun goldIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.circle(12, 12, 10, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 7f, cy = 7f, radius = 22f))
    g.circle(12, 12, 8, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 8f, radius = 17f))
    // Embossed crown, shaded a touch darker so it reads as struck into the metal.
    g.rect(8, 14, 16, 16, 'a')
    g.triangle(8, 14, 11, 14, 9, 9, 'a')
    g.triangle(11, 14, 14, 14, 12, 7, 'a')
    g.triangle(14, 14, 17, 14, 15, 9, 'a')
    g.ditherRamp('a', RC, radialFalloff(cx = 9f, cy = 10f, radius = 12f))
    g.set(8, 8, HIGHLIGHT)
    g.set(9, 7, HIGHLIGHT)
    return g.finishFree(
        rampPalette(RA, Ramps.GoldDark, Ramps.GoldLight),
        rampPalette(RB, Color(0xFF4A2C08), Color(0xFFC79A44)),
        rampPalette(RC, Color(0xFF3E2606), Color(0xFF9A7220)),
    )
}

/** A ruled parchment scroll with rolled ends and a wax seal, used for Chronicle Points. */
fun chroniclePointsIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.rect(4, 5, 19, 19, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 8f, radius = 22f))
    g.ellipse(11, 4, 9, 3, '5')
    g.ellipse(11, 20, 9, 3, '5')
    g.ditherRamp('5', RB, linearFalloff(from = 23f, to = 0f))
    g.rect(7, 9, 16, 9, 'a')
    g.rect(7, 12, 16, 12, 'a')
    g.rect(7, 15, 13, 15, 'a')
    g.ditherRamp('a', RC, linearFalloff(from = 18f, to = 6f))
    g.circle(16, 16, 2, 'z')
    return g.finishFree(
        rampPalette(RA, Ramps.PaperDark, Ramps.PaperLight),
        rampPalette(RB, Ramps.WoodDark, Ramps.WoodLight),
        rampPalette(RC, Color(0xFF6B5A3A), Color(0xFFAE9A66)),
        mapOf('z' to Color(0xFF9C2B2B)),
    )
}

/** A gemmed crown, used for Sagen (Legends). */
fun sagenIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.rect(4, 15, 19, 20, '1')
    g.triangle(4, 16, 9, 16, 6, 6, '1')
    g.triangle(9, 16, 15, 16, 12, 3, '1')
    g.triangle(15, 16, 20, 16, 18, 6, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 8f, radius = 22f))
    g.circle(6, 6, 1, '5')
    g.circle(12, 4, 1, '5')
    g.circle(18, 6, 1, '5')
    g.circle(12, 17, 2, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 10f, cy = 5f, radius = 16f))
    return g.finishFree(
        rampPalette(RA, Ramps.GoldDark, Ramps.GoldLight),
        rampPalette(RB, Ramps.ClothDark, Ramps.ClothLight),
    )
}

/** A watching, dark eye in the corruption palette — the tappable sighting for a Verfall Omen. */
fun omenIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.ellipse(12, 12, 10, 6, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 9f, radius = 20f))
    g.circle(12, 12, 5, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 10f, cy = 10f, radius = 10f))
    g.circle(12, 12, 2, 'z')
    g.set(11, 11, HIGHLIGHT)
    return g.finishFree(
        rampPalette(RA, Ramps.CorruptionDark, Ramps.CorruptionLight),
        rampPalette(RB, Color(0xFF120014), Color(0xFF6B2A78)),
        mapOf('z' to EmberPalette.Ink),
    )
}

/** A wax-sealed signet ring, used for Einfluss (Influence). */
fun einflussIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.ring(12, 12, 10, 7, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 8f, radius = 20f))
    g.circle(12, 12, 5, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 10f, cy = 10f, radius = 10f))
    g.set(11, 11, HIGHLIGHT)
    return g.finishFree(
        rampPalette(RA, Ramps.ArcaneDark, Ramps.ArcaneLight),
        rampPalette(RB, Color(0xFF3A1E5E), Color(0xFFB98EE8)),
    )
}
