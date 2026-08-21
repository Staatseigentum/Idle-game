package com.embercrown.game.ui.pixelart

private const val S = 32

private const val PLAQUE = "pqrs"
private const val GOLD = "1234"
private const val GEM = "5678"

private const val OUTLINE = 'K'

/**
 * The app/window/installer icon: a gemmed gold crown on the shared plaque background, lit from
 * the upper left like every other sprite. Geometry mirrors the proven `sagenIcon()` crown
 * (scaled from its 24-cell grid), swapping its cloth-pink gems for ember-flame ones to tie the
 * "Embercrown" name to the art. Rendered to raster icon files by `IconExporter` in
 * desktopMain — no PNG is checked in.
 */
fun appIcon(): PixelArt {
    val g = PixelGridBuilder(S, S)

    // A large icon is viewed much closer/bigger than an in-game sprite, so every fill here uses
    // a high dither `sharpness` — flat, confident bands with only a thin blended seam, rather
    // than the fine all-over dithering that reads as noise at installer/desktop icon sizes.
    g.rect(0, 0, S - 1, S - 1, 'p')
    g.ditherRamp('p', PLAQUE, radialFalloff(cx = 8f, cy = 6f, radius = 40f), sharpness = 6f)

    g.rect(5, 20, 25, 27, '1')
    g.triangle(5, 21, 12, 21, 8, 8, '1')
    g.triangle(12, 21, 20, 21, 16, 4, '1')
    g.triangle(20, 21, 27, 21, 24, 8, '1')
    g.ditherRamp('1', GOLD, radialFalloff(cx = 11f, cy = 11f, radius = 29f), sharpness = 6f)

    // Each gem is shaded from its own center, like a polished cabochon, rather than one falloff
    // shared across all four — sharing one would wash the nearer gems out to the ramp's palest
    // step. Marker chars 'e'/'f'/'g'/'h' are dithered away one at a time so they can't collide
    // with each other or with GEM's own ramp characters.
    g.circle(8, 8, 2, 'e')
    g.ditherRamp('e', GEM, radialFalloff(cx = 8f, cy = 7f, radius = 4f), sharpness = 6f)
    g.circle(16, 5, 3, 'f')
    g.ditherRamp('f', GEM, radialFalloff(cx = 16f, cy = 4f, radius = 5f), sharpness = 6f)
    g.circle(24, 8, 2, 'g')
    g.ditherRamp('g', GEM, radialFalloff(cx = 24f, cy = 7f, radius = 4f), sharpness = 6f)
    g.circle(16, 23, 3, 'h')
    g.ditherRamp('h', GEM, radialFalloff(cx = 16f, cy = 22f, radius = 5f), sharpness = 6f)

    g.outlineAgainst(PLAQUE, OUTLINE)
    g.rectOutline(0, 0, S - 1, S - 1, OUTLINE)

    return g.build(
        rampPalette(PLAQUE, EmberPalette.Panel, EmberPalette.PanelLight) +
            rampPalette(GOLD, Ramps.GoldDark, Ramps.GoldLight) +
            rampPalette(GEM, Ramps.FlameDark, Ramps.FlameLight) +
            mapOf(OUTLINE to EmberPalette.Ink),
    )
}
