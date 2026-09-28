package com.embercrown.game.reboot

import androidx.compose.ui.graphics.Color
import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.PixelGridBuilder

private val mapPalette = mapOf(
    'a' to Color(0xFF0D1520), 'b' to Color(0xFF172331), 'c' to Color(0xFF20303A),
    'd' to Color(0xFF304047), 'e' to Color(0xFF4A514C), 'f' to Color(0xFF657169),
    'w' to Color(0xFF142B3A), 'W' to Color(0xFF285064), 'r' to Color(0xFF49323D),
    'R' to Color(0xFF714152), 'o' to Color(0xFF0A0E15), 't' to Color(0xFF78BDB4),
    'T' to Color(0xFFC5DBCD), 'x' to Color(0xFF9C5360), 'X' to Color(0xFFFFA34E),
    'Y' to Color(0xFFFFD784), 'v' to Color(0x887E9A9A),
)

/** All terrain and markers are drawn at native pixel resolution, then scaled without filtering. */
fun lostMarchesArt(conquered: Set<String>): PixelArt {
    val g = PixelGridBuilder(160, 112)
    g.rect(0, 0, 159, 111, 'a')
    g.ellipse(82, 63, 78, 54, 'b')
    g.ellipse(77, 61, 68, 46, 'c')
    g.ellipse(80, 60, 58, 39, 'd')
    g.ellipse(79, 59, 45, 31, 'e')
    g.ellipse(81, 60, 26, 19, 'f')
    for (i in 0..25) {
        val x = (i * 37 + 9) % 160
        val y = (i * 31 + 7) % 112
        g.rect(x, y, x + 2, y + 1, 'W')
    }
    g.line(80, 57, 37, 72, 'r', 3)
    g.line(80, 57, 100, 83, 'r', 3)
    g.line(80, 57, 50, 35, 'r', 3)
    g.line(80, 57, 119, 38, 'r', 3)
    // Irregular woods, marsh pools, coast and mountain shards establish four distinct silhouettes.
    for (i in 0..13) {
        val x = 15 + i * 4
        val y = 64 + (i * 7 % 18)
        g.triangle(x - 3, y + 4, x + 3, y + 4, x, y - 5, 'o')
        g.triangle(x - 2, y + 2, x + 2, y + 2, x, y - 4, 'c')
    }
    for (i in 0..9) {
        val x = 82 + i * 5
        val y = 79 + (i * 11 % 14)
        g.ellipse(x, y, 4, 2, 'w')
        g.set(x, y, 'W')
    }
    g.line(24, 28, 69, 26, 'W', 2)
    g.line(26, 34, 67, 32, 'W', 2)
    for (i in 0..5) {
        val x = 100 + i * 7
        g.triangle(x - 7, 53, x + 7, 53, x, 22 - i % 3 * 4, 'b')
        g.triangle(x - 5, 52, x + 5, 52, x, 27 - i % 3 * 4, 'r')
    }
    g.rect(76, 53, 84, 65, 'o')
    g.rect(78, 54, 82, 63, 'f')
    g.triangle(74, 54, 86, 54, 80, 45, 'r')
    g.rect(79, 56, 81, 59, 'X')
    LostMarches.all.forEach { region ->
        val claimed = region.id in conquered
        g.circle(region.x, region.y, 7, 'o')
        g.circle(region.x, region.y, 5, if (claimed) 't' else 'R')
        g.rect(region.x - 2, region.y - 2, region.x + 2, region.y + 2,
            if (claimed) 'T' else 'x')
        g.set(region.x, region.y, if (claimed) 'Y' else 'X')
    }
    g.rectOutline(1, 1, 158, 110, 'f')
    return g.build(mapPalette)
}

fun lostMarchesMotionArt(frame: Int, expedition: MarchExpedition?): PixelArt {
    val g = PixelGridBuilder(160, 112)
    for (i in 0..13) {
        val x = (i * 43 + frame * (1 + i % 2)) % 170 - 5
        val y = 19 + i * 7 % 78
        g.rect(x, y, x + 6, y + 1, 'v')
    }
    LostMarches.all.forEachIndexed { index, region ->
        if ((frame + index * 3) % 12 < 6) {
            g.set(region.x, region.y - 6, 'Y')
            g.set(region.x + 6, region.y, 'X')
        }
    }
    expedition?.let { running ->
        LostMarches.byId(running.regionId)?.let { region ->
            val p = (frame % 16) / 16f
            val x = (80 + (region.x - 80) * p).toInt()
            val y = (57 + (region.y - 57) * p).toInt()
            g.rect(x - 1, y - 1, x + 1, y + 1, 'Y')
        }
    }
    return g.build(mapPalette)
}
