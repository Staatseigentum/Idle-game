package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color
import com.embercrown.game.game.HeirEffectType

private const val S = 24
private const val RAMP = "1234"
private const val OUTLINE = 'K'
private const val MARK = 'M'

/**
 * A small heraldic emblem for a crowned Heir — deliberately simple (a shield + one mark), not a
 * full character portrait, which this project has no generator for. Color and mark vary by
 * [HeirEffectType] so each trait family reads as visually distinct at a glance.
 */
fun heirEmblemIcon(effectType: HeirEffectType): PixelArt {
    val g = PixelGridBuilder(S, S)
    g.rect(5, 4, 18, 14, '1')
    g.triangle(5, 14, 18, 14, 11, 21, '1')
    g.ditherRamp('1', RAMP, linearFalloff(from = 21f, to = 4f))

    when (effectType) {
        HeirEffectType.PRODUCTION -> g.circle(11, 11, 3, MARK)
        HeirEffectType.BUILDING_COST -> g.rect(9, 9, 13, 13, MARK)
        HeirEffectType.CORRUPTION_RATE -> g.ring(11, 11, 4, 2, MARK)
        HeirEffectType.CHRONICLE_POINTS -> g.line(8, 8, 14, 14, MARK, thickness = 2)
        HeirEffectType.INFLUENCE -> g.ellipse(11, 11, 4, 2, MARK)
        HeirEffectType.CLICK_GAIN -> g.triangle(8, 14, 14, 14, 11, 7, MARK)
    }

    val (dark, light) = rampFor(effectType)
    g.outlineAgainst(".", OUTLINE)
    return g.build(
        rampPalette(RAMP, dark, light) + mapOf(MARK to EmberPalette.White, OUTLINE to EmberPalette.Ink),
    )
}

private fun rampFor(effectType: HeirEffectType): Pair<Color, Color> = when (effectType) {
    HeirEffectType.PRODUCTION -> Ramps.GoldDark to Ramps.GoldLight
    HeirEffectType.BUILDING_COST -> Ramps.WoodDark to Ramps.WoodLight
    HeirEffectType.CORRUPTION_RATE -> Ramps.ArcaneDark to Ramps.ArcaneLight
    HeirEffectType.CHRONICLE_POINTS -> Ramps.PaperDark to Ramps.PaperLight
    HeirEffectType.INFLUENCE -> Ramps.ClothDark to Ramps.ClothLight
    HeirEffectType.CLICK_GAIN -> Ramps.FlameDark to Ramps.FlameLight
}
