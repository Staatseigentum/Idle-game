package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color

private const val S = 24

// Palette key groups. Every icon may use up to three material ramps plus the shared plaque,
// outline and accent keys, which keeps each sprite's palette small and readable.
private const val PLAQUE = "pqrs"
private const val RA = "1234"
private const val RB = "5678"
private const val RC = "abcd"

private const val OUTLINE = 'K'
private const val HIGHLIGHT = 'W'
private const val DARK = 'D'

/** Light always falls from the upper left, which is what makes the shading read consistently. */
private fun lit(cx: Float, cy: Float, reach: Float) = directionalFalloff(cx, cy, -0.72f, -0.7f, reach)

/** Starts an icon on the shared plaque background, subtly lit from the upper left. */
private fun plaque(): PixelGridBuilder {
    val g = PixelGridBuilder(S, S)
    g.rect(0, 0, S - 1, S - 1, 'p')
    g.ditherRamp('p', PLAQUE, radialFalloff(cx = 6f, cy = 4f, radius = 34f))
    return g
}

/** Wraps the subject in a dark contour, frames the plaque and assembles the final palette. */
private fun PixelGridBuilder.finish(vararg ramps: Map<Char, Color>): PixelArt {
    outlineAgainst(PLAQUE, OUTLINE)
    rectOutline(0, 0, S - 1, S - 1, OUTLINE)
    var palette: Map<Char, Color> = rampPalette(PLAQUE, EmberPalette.Panel, EmberPalette.PanelLight) +
        mapOf(
            OUTLINE to EmberPalette.Ink,
            HIGHLIGHT to EmberPalette.White,
            DARK to Color(0xFF120C08),
        )
    ramps.forEach { palette = palette + it }
    return build(palette)
}

/** Returns the pixel-art icon for a building id, or `null` for unknown ids. */
fun buildingIcon(id: String): PixelArt? = when (id) {
    "farm" -> farmIcon()
    "fishing_hut" -> fishingHutIcon()
    "woodcutter_camp" -> woodcutterCampIcon()
    "quarry" -> quarryIcon()
    "mine" -> mineIcon()
    "market_stall" -> marketStallIcon()
    "tavern" -> tavernIcon()
    "forge" -> forgeIcon()
    "trading_post" -> tradingPostIcon()
    "library" -> libraryIcon()
    "alchemist_lab" -> alchemistLabIcon()
    "wizard_tower" -> wizardTowerIcon()
    "temple" -> templeIcon()
    "treasury" -> treasuryIcon()
    "dragon_hoard" -> dragonHoardIcon()
    "celestial_observatory" -> celestialObservatoryIcon()
    else -> null
}

/** A barn: shaded roof, plank wall and a dark doorway. */
private fun farmIcon(): PixelArt {
    val g = plaque()
    g.rect(4, 12, 19, 20, '5')
    g.ditherRamp('5', RB, lit(cx = 12f, cy = 16f, reach = 20f))
    g.triangle(2, 12, 21, 12, 11, 4, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 8f, reach = 18f))
    g.rect(9, 15, 14, 20, DARK)
    g.line(11, 15, 11, 20, '7')
    return g.finish(
        rampPalette(RA, Ramps.RoofDark, Ramps.RoofLight),
        rampPalette(RB, Ramps.WoodDark, Ramps.WoodLight),
    )
}

/** A fish, shaded like a rounded body with a bright belly. */
private fun fishingHutIcon(): PixelArt {
    val g = plaque()
    g.triangle(15, 12, 21, 6, 21, 18, '1')
    g.ellipse(10, 12, 8, 6, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 8f, cy = 9f, radius = 14f))
    g.circle(6, 10, 1, HIGHLIGHT)
    g.set(6, 10, DARK)
    g.line(11, 8, 14, 11, '5')
    g.line(11, 16, 14, 13, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 11f, cy = 12f, radius = 8f))
    return g.finish(
        rampPalette(RA, Ramps.WaterDark, Ramps.WaterLight),
        rampPalette(RB, Ramps.LeafDark, Ramps.LeafLight),
    )
}

/** A felling axe: broad steel head with a flared cutting edge on a wooden haft. */
private fun woodcutterCampIcon(): PixelArt {
    val g = plaque()
    g.line(4, 21, 16, 6, '5', thickness = 3)
    g.ditherRamp('5', RB, lit(cx = 10f, cy = 14f, reach = 16f))
    g.rect(11, 4, 16, 14, '1')
    g.triangle(16, 1, 22, 9, 16, 17, '1')
    g.ditherRamp('1', RA, lit(cx = 16f, cy = 9f, reach = 16f))
    return g.finish(
        rampPalette(RA, Ramps.StoneDark, Ramps.StoneLight),
        rampPalette(RB, Ramps.WoodDark, Ramps.WoodLight),
    )
}

/** Stacked quarried blocks with dark mortar seams. */
private fun quarryIcon(): PixelArt {
    val g = plaque()
    g.rect(2, 13, 21, 20, '1')
    g.rect(5, 6, 18, 12, '1')
    g.ditherRamp('1', RA, lit(cx = 12f, cy = 13f, reach = 24f))
    g.line(2, 12, 21, 12, DARK)
    g.line(11, 13, 11, 20, DARK)
    g.line(11, 6, 11, 11, DARK)
    return g.finish(rampPalette(RA, Ramps.StoneDark, Ramps.StoneLight))
}

/** A mine mouth cut into a hillside, framed with timber and fronted by rails. */
private fun mineIcon(): PixelArt {
    val g = plaque()
    g.triangle(1, 20, 22, 20, 11, 4, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 14f, reach = 22f))
    g.ellipse(11, 18, 6, 7, DARK)
    g.rect(5, 18, 17, 20, DARK)
    g.rect(4, 11, 6, 20, '5')
    g.rect(16, 11, 18, 20, '5')
    g.rect(4, 10, 18, 12, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 15f, reach = 16f))
    g.line(8, 20, 8, 21, '7')
    g.line(14, 20, 14, 21, '7')
    return g.finish(
        rampPalette(RA, Color(0xFF2A1D12), Color(0xFF9C7348)),
        rampPalette(RB, Ramps.WoodDark, Ramps.WoodLight),
    )
}

/** A market stall: striped awning over a wooden counter. */
private fun marketStallIcon(): PixelArt {
    val g = plaque()
    g.rect(4, 13, 19, 20, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 16f, reach = 16f))
    // Alternating stripe columns get their own ramp so both tones stay shaded, not flat.
    g.rect(2, 5, 21, 11, '1')
    var sx = 2
    while (sx <= 21) {
        g.rect(sx, 5, sx + 1, 12, 'a')
        sx += 4
    }
    var hx = 4
    while (hx <= 21) {
        g.set(hx, 12, '1')
        g.set(hx + 1, 12, '1')
        hx += 4
    }
    g.ditherRamp('1', RA, linearFalloff(from = 13f, to = 4f))
    g.ditherRamp('a', RC, linearFalloff(from = 13f, to = 4f))
    g.rect(4, 11, 5, 14, '7')
    g.rect(18, 11, 19, 14, '7')
    return g.finish(
        rampPalette(RA, Color(0xFF6B1A14), Color(0xFFE08258)),
        rampPalette(RB, Ramps.WoodDark, Ramps.WoodLight),
        rampPalette(RC, Color(0xFF6E5A38), Color(0xFFF6E4B8)),
    )
}

/** A brimming tankard: amber ale, white foam head and a looped handle. */
private fun tavernIcon(): PixelArt {
    val g = plaque()
    g.ring(17, 14, 5, 3, '1')
    g.rect(5, 9, 16, 20, '1')
    g.ditherRamp('1', RA, lit(cx = 10f, cy = 14f, reach = 16f))
    g.rect(5, 5, 16, 9, '5')
    g.ellipse(8, 5, 4, 3, '5')
    g.ellipse(14, 5, 4, 3, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 9f, cy = 4f, radius = 14f))
    g.line(7, 11, 7, 19, '4')
    return g.finish(
        rampPalette(RA, Ramps.GoldDark, Ramps.GoldLight),
        rampPalette(RB, Ramps.PaperDark, Ramps.PaperLight),
    )
}

/** An anvil under a rising flame. */
private fun forgeIcon(): PixelArt {
    val g = plaque()
    // Outer flame plus a hotter inner core, each on its own ramp so the fire reads as fire.
    g.triangle(12, 0, 17, 9, 7, 9, '5')
    g.ellipse(12, 8, 5, 3, '5')
    g.ditherRamp('5', RB, linearFalloff(from = 11f, to = 2f))
    g.triangle(12, 4, 14, 10, 10, 10, 'a')
    g.ditherRamp('a', RC, linearFalloff(from = 11f, to = 4f))
    g.rect(5, 12, 19, 15, '1')
    g.triangle(5, 12, 2, 14, 5, 16, '1')
    g.rect(9, 15, 14, 18, '1')
    g.rect(6, 18, 18, 20, '1')
    g.ditherRamp('1', RA, lit(cx = 12f, cy = 16f, reach = 18f))
    return g.finish(
        rampPalette(RA, Ramps.StoneDark, Ramps.StoneLight),
        rampPalette(RB, Color(0xFF8A2606), Color(0xFFF08A22)),
        rampPalette(RC, Color(0xFFE87A18), Color(0xFFFFF4C8)),
    )
}

/** A cart wheel: light rim, darker spokes so they stay legible, and a gold hub. */
private fun tradingPostIcon(): PixelArt {
    val g = plaque()
    // Spokes are a separate, darker ramp — same-tone spokes vanish into the rim at this size.
    g.line(12, 4, 12, 20, 'a', thickness = 2)
    g.line(4, 12, 20, 12, 'a', thickness = 2)
    g.line(6, 6, 18, 18, 'a', thickness = 2)
    g.line(18, 6, 6, 18, 'a', thickness = 2)
    g.ditherRamp('a', RC, lit(cx = 12f, cy = 12f, reach = 20f))
    g.ring(12, 12, 11, 8, '1')
    g.ditherRamp('1', RA, lit(cx = 12f, cy = 12f, reach = 22f))
    g.circle(12, 12, 3, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 10f, cy = 10f, radius = 6f))
    return g.finish(
        rampPalette(RA, Color(0xFF5A3418), Color(0xFFD3A067)),
        rampPalette(RB, Ramps.GoldDark, Ramps.GoldLight),
        rampPalette(RC, Color(0xFF20120A), Color(0xFF6E4526)),
    )
}

/** A stack of three bound books with bright page edges. */
private fun libraryIcon(): PixelArt {
    val g = plaque()
    g.rect(3, 16, 20, 20, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 18f, reach = 16f))
    g.rect(4, 11, 19, 15, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 13f, reach = 16f))
    g.rect(6, 6, 18, 10, 'a')
    g.ditherRamp('a', RC, lit(cx = 12f, cy = 8f, reach = 14f))
    // Page blocks catch the light on the right-hand edge of each volume.
    g.line(19, 17, 19, 19, HIGHLIGHT)
    g.line(18, 12, 18, 14, HIGHLIGHT)
    g.line(17, 7, 17, 9, HIGHLIGHT)
    return g.finish(
        rampPalette(RA, Ramps.ClothDark, Ramps.ClothLight),
        rampPalette(RB, Ramps.WaterDark, Ramps.WaterLight),
        rampPalette(RC, Ramps.LeafDark, Ramps.LeafLight),
    )
}

/** A round-bottomed flask of glowing green reagent, mid-bubble. */
private fun alchemistLabIcon(): PixelArt {
    val g = plaque()
    g.rect(10, 4, 13, 10, '1')
    g.ellipse(11, 15, 8, 7, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 7f, cy = 11f, radius = 16f))
    g.ellipse(11, 17, 6, 4, '5')
    g.rect(5, 16, 17, 20, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 8f, cy = 15f, radius = 12f))
    g.rect(10, 2, 13, 4, 'a')
    g.ditherRamp('a', RC, lit(cx = 11f, cy = 3f, reach = 4f))
    g.set(9, 13, HIGHLIGHT)
    g.set(13, 11, HIGHLIGHT)
    return g.finish(
        rampPalette(RA, Color(0xFF2C3A40), Color(0xFFBFE3EC)),
        rampPalette(RB, Ramps.LeafDark, Ramps.LeafLight),
        rampPalette(RC, Ramps.WoodDark, Ramps.WoodLight),
    )
}

/** A slender tower under a conical arcane roof, its window lit from within. */
private fun wizardTowerIcon(): PixelArt {
    val g = plaque()
    g.rect(7, 10, 16, 21, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 15f, reach = 12f))
    g.triangle(4, 11, 19, 11, 11, 1, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 6f, reach = 14f))
    g.rect(10, 14, 13, 18, DARK)
    g.rect(10, 14, 13, 15, 'a')
    g.ditherRamp('a', RC, radialFalloff(cx = 11f, cy = 14f, radius = 4f))
    g.set(3, 4, HIGHLIGHT)
    g.set(20, 7, HIGHLIGHT)
    return g.finish(
        rampPalette(RA, Ramps.StoneDark, Ramps.StoneLight),
        rampPalette(RB, Ramps.ArcaneDark, Ramps.ArcaneLight),
        rampPalette(RC, Ramps.GoldDark, Ramps.GoldLight),
    )
}

/** A classical facade: pediment, entablature, four fluted columns and a stepped base. */
private fun templeIcon(): PixelArt {
    val g = plaque()
    g.rect(4, 11, 6, 19, '1')
    g.rect(9, 11, 11, 19, '1')
    g.rect(13, 11, 15, 19, '1')
    g.rect(18, 11, 20, 19, '1')
    g.ditherRamp('1', RA, lit(cx = 12f, cy = 15f, reach = 20f))
    g.rect(2, 9, 21, 11, '5')
    g.rect(1, 19, 22, 21, '5')
    g.triangle(2, 9, 21, 9, 12, 2, '5')
    g.ditherRamp('5', RB, lit(cx = 12f, cy = 8f, reach = 22f))
    // Shadow line beneath the entablature, otherwise pediment and columns merge into one mass.
    g.rect(2, 11, 21, 11, DARK)
    return g.finish(
        rampPalette(RA, Color(0xFF3A3A42), Color(0xFFA8A8B2)),
        rampPalette(RB, Color(0xFF5A5A64), Color(0xFFF2F2F6)),
    )
}

/** A banded treasure chest with a gold lock. */
private fun treasuryIcon(): PixelArt {
    val g = plaque()
    // Domed lid and squared body share one char so they shade as a single silhouette.
    g.ellipse(11, 12, 9, 6, '1')
    g.rect(3, 12, 20, 20, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 13f, reach = 20f))
    g.rect(3, 11, 20, 13, '5')
    g.rect(5, 7, 7, 20, '5')
    g.rect(16, 7, 18, 20, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 13f, reach = 20f))
    g.rect(10, 11, 13, 15, '5')
    g.set(11, 13, DARK)
    g.set(12, 13, DARK)
    return g.finish(
        rampPalette(RA, Ramps.WoodDark, Ramps.WoodLight),
        rampPalette(RB, Ramps.GoldDark, Ramps.GoldLight),
    )
}

/** A hoard: a mound of discrete gold coins crowned by a cut gem. */
private fun dragonHoardIcon(): PixelArt {
    val g = plaque()
    g.ellipse(11, 19, 11, 4, '1')
    g.ditherRamp('1', RA, radialFalloff(cx = 6f, cy = 16f, radius = 18f))
    // Individual coins on a second gold ramp, each rimmed, so the pile reads as loose coins.
    listOf(3 to 16, 8 to 15, 14 to 16, 19 to 17, 11 to 12).forEach { (cx, cy) ->
        g.circle(cx, cy, 2, 'c')
        g.circle(cx, cy, 1, 'a')
    }
    g.ditherRamp('a', RC, radialFalloff(cx = 6f, cy = 12f, radius = 18f))
    g.triangle(6, 8, 17, 8, 11, 1, '5')
    g.triangle(6, 8, 17, 8, 11, 14, '5')
    g.ditherRamp('5', RB, radialFalloff(cx = 8f, cy = 6f, radius = 12f))
    g.set(9, 5, HIGHLIGHT)
    return g.finish(
        rampPalette(RA, Color(0xFF5C3A0C), Color(0xFFC79A38)),
        rampPalette(RB, Ramps.ArcaneDark, Ramps.ArcaneLight),
        rampPalette(RC, Color(0xFF8A6414), Color(0xFFFFE9A4)),
    )
}

/** An observatory dome with an open shutter and a telescope trained on the sky. */
private fun celestialObservatoryIcon(): PixelArt {
    val g = plaque()
    g.circle(11, 14, 8, '1')
    g.ditherRamp('1', RA, lit(cx = 11f, cy = 14f, reach = 18f))
    g.rect(2, 16, 21, 21, '5')
    g.ditherRamp('5', RB, lit(cx = 11f, cy = 18f, reach = 12f))
    g.rect(10, 6, 13, 16, DARK)
    g.line(11, 12, 20, 3, 'a', thickness = 3)
    g.ditherRamp('a', RC, lit(cx = 16f, cy = 7f, reach = 12f))
    g.set(3, 4, HIGHLIGHT)
    g.set(6, 2, HIGHLIGHT)
    g.set(21, 10, HIGHLIGHT)
    return g.finish(
        rampPalette(RA, Ramps.StoneDark, Ramps.StoneLight),
        rampPalette(RB, Color(0xFF2A2A30), Color(0xFF8A8A94)),
        rampPalette(RC, Ramps.GoldDark, Ramps.GoldLight),
    )
}
