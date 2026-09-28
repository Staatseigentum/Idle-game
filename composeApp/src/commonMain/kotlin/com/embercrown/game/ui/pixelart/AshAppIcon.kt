package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color

/** A broken iron crown around the last ember, silhouetted against the blood moon. */
fun appIcon(): PixelArt {
    val g = PixelGridBuilder(32, 32)
    g.rect(0, 0, 31, 31, 'n')
    g.rect(2, 2, 29, 29, 's')
    g.circle(16, 14, 10, 'r')
    g.circle(14, 11, 4, 'R')
    g.rect(4, 25, 27, 29, 'o')
    g.triangle(5, 20, 10, 21, 7, 10, 'o')
    g.triangle(11, 20, 17, 20, 14, 7, 'o')
    g.triangle(20, 20, 27, 20, 26, 8, 'o')
    g.rect(6, 20, 26, 26, 'o')
    g.rect(8, 22, 24, 24, 'i')
    g.set(11, 22, 'a')
    g.set(21, 22, 'a')
    g.triangle(12, 22, 20, 22, 16, 12, 'e')
    g.triangle(14, 22, 18, 22, 16, 16, 'Y')
    g.rectOutline(0, 0, 31, 31, 'o')
    return g.build(
        mapOf(
            'n' to Color(0xFF0B0E14),
            's' to Color(0xFF19212D),
            'r' to Color(0xFF5B2939),
            'R' to Color(0xFFB44A55),
            'o' to Color(0xFF090D13),
            'i' to Color(0xFF718193),
            'a' to Color(0xFFA9B5B8),
            'e' to Color(0xFFFFA34E),
            'Y' to Color(0xFFFFD784),
        ),
    )
}
