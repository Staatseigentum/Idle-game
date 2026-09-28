package com.embercrown.game.reboot

import androidx.compose.ui.graphics.Color
import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.PixelGridBuilder

/** Each discovery receives a permanent little pixel sigil in the trophy hall. */
fun chronicleTrophyArt(id: String, earned: Boolean): PixelArt {
    val index = CrownChronicle.all.indexOfFirst { it.id == id }.coerceAtLeast(0)
    val g = PixelGridBuilder(20, 20)
    g.rect(2, 2, 17, 17, 'd')
    g.rectOutline(2, 2, 17, 17, 'o')
    g.rectOutline(4, 4, 15, 15, 'm')
    g.rect(7, 16, 12, 18, 'o')
    when (index % 6) {
        0 -> {
            g.triangle(6, 13, 13, 13, 10, 5, 'l')
            g.rect(8, 12, 11, 15, 'm')
        }
        1 -> {
            g.circle(10, 10, 5, 'l')
            g.circle(10, 10, 2, 'd')
        }
        2 -> {
            g.rect(6, 11, 13, 14, 'l')
            g.rect(7, 7, 8, 11, 'm')
            g.rect(11, 5, 12, 11, 'm')
        }
        3 -> {
            g.triangle(5, 12, 15, 12, 10, 5, 'l')
            g.rect(8, 10, 12, 15, 'm')
        }
        4 -> {
            g.line(5, 10, 15, 10, 'l', 2)
            g.line(10, 5, 10, 15, 'm', 2)
        }
        else -> {
            g.circle(10, 10, 5, 'm')
            g.rect(8, 5, 11, 15, 'l')
        }
    }
    g.set(7 + index % 6, 6 + index / 6, 's')
    if (earned) {
        g.set(1, 1, 's')
        g.set(18, 3, 's')
        g.set(17, 17, 's')
    }
    val accents = listOf(AshPalette.flame, AshPalette.teal, AshPalette.crimson,
        AshPalette.violet, AshPalette.flameLight, AshPalette.ash)
    val accent = accents[index % accents.size]
    return g.build(mapOf(
        'o' to AshPalette.void,
        'd' to AshPalette.panel,
        'm' to if (earned) accent else AshPalette.edge,
        'l' to if (earned) AshPalette.flameLight else AshPalette.stone,
        's' to if (earned) Color.White else AshPalette.muted,
    ))
}
