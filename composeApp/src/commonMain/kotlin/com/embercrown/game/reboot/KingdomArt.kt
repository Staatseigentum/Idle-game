package com.embercrown.game.reboot

import androidx.compose.ui.graphics.Color
import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.PixelGridBuilder

/** A new restricted palette: soot, cold stone, tarnished copper, moonlight and living fire. */
object AshPalette {
    val void = Color(0xFF0B0E14)
    val night = Color(0xFF121824)
    val panel = Color(0xFF19212D)
    val panelRaised = Color(0xFF25303D)
    val edge = Color(0xFF415061)
    val stone = Color(0xFF718193)
    val ash = Color(0xFFA9B5B8)
    val bone = Color(0xFFE8DEC9)
    val muted = Color(0xFF8E9BA6)
    val flame = Color(0xFFFFA34E)
    val flameLight = Color(0xFFFFD784)
    val crimson = Color(0xFFB44A55)
    val teal = Color(0xFF78BDB4)
    val violet = Color(0xFF51415F)
}

private val worldColors = mapOf(
    'a' to Color(0xFF0B0E18), // high sky
    'b' to Color(0xFF151A2B),
    'c' to Color(0xFF242237),
    'd' to Color(0xFF35283B), // horizon
    's' to Color(0xFF586175), // stars
    'S' to Color(0xFFE6DFC9),
    'r' to Color(0xFF5B2939), // moon corona
    'R' to Color(0xFFB44A55),
    'Q' to Color(0xFFE88973),
    'm' to Color(0xFF161C29), // mountains
    'M' to Color(0xFF252B38),
    'f' to Color(0xFF1A2230), // fir trees
    'F' to Color(0xFF2A3740),
    'g' to Color(0xFF1C2227), // ground
    'G' to Color(0xFF293039),
    'h' to Color(0xFF3D4347),
    'o' to Color(0xFF090D13), // outlines
    'k' to Color(0xFF28313D), // masonry shadow
    'K' to Color(0xFF4A5664),
    'L' to Color(0xFF718193),
    'B' to Color(0xFFA3A9A4),
    'w' to Color(0xFF27202B), // dark roof
    'W' to Color(0xFF5F3E46),
    't' to Color(0xFF3C3036), // timber
    'T' to Color(0xFF836057),
    'e' to Color(0xFF9B442D), // fire
    'E' to Color(0xFFFFA34E),
    'Y' to Color(0xFFFFD784),
    'p' to Color(0xFF413655), // occult glyph
    'P' to Color(0xFF9E84B1),
    'v' to Color(0xFF677E80), // mist
    'V' to Color(0x887E9A9A), // drifting translucent fog
    'z' to Color(0xFF39404F), // smoke
)

/** The static scene is rasterized only when a building, the beacon or the gloom changes. */
fun ashKingdomArt(levels: Map<String, Int>, gloom: Int, beaconLit: Boolean,
                  masteries: Map<String, Int> = emptyMap(), conquered: Set<String> = emptySet(),
                  specializations: Map<String, String> = emptyMap()): PixelArt {
    val g = PixelGridBuilder(160, 120)
    for (y in 0..82) {
        val band = when {
            y < 28 -> 'a'
            y < 55 -> 'b'
            y < 70 -> 'c'
            else -> 'd'
        }
        g.rect(0, y, 159, y, band)
    }
    for (i in 0 until 82) {
        val x = (i * 47 + i * i * 13) % 158 + 1
        val y = (i * 29 + i * i * 7) % 66 + 2
        if ((i + gloom) % 5 != 0) g.set(x, y, 's')
    }
    g.circle(121, 28, 15, 'r')
    g.circle(121, 28, 11, 'R')
    g.circle(118, 24, 4, 'Q')
    g.circle(126, 32, 2, 'r')
    g.triangle(0, 77, 42, 77, 23, 44, 'm')
    g.triangle(20, 77, 88, 77, 57, 52, 'M')
    g.triangle(75, 79, 160, 79, 141, 39, 'm')
    g.triangle(93, 78, 150, 78, 115, 56, 'M')
    // Forest silhouettes frame the central ruin without hiding its upgrades.
    for (i in 0 until 9) {
        val leftX = i * 9 - 4
        val rightX = 166 - i * 9
        val height = 19 + (i * 11 % 19)
        fir(g, leftX, 92, height, if (i % 2 == 0) 'f' else 'F')
        fir(g, rightX, 92, height, if (i % 2 == 0) 'F' else 'f')
    }
    g.rect(0, 88, 159, 119, 'g')
    g.triangle(0, 90, 90, 90, 35, 84, 'G')
    g.triangle(78, 91, 159, 91, 142, 83, 'G')
    g.rect(45, 96, 115, 101, 'h')
    g.rect(39, 101, 121, 104, 'k')
    // Castle starts as a ruined hearth. Its silhouette grows with every reign.
    g.rect(58, 63, 103, 96, 'o')
    g.rect(60, 65, 101, 94, 'k')
    g.rect(62, 67, 99, 91, 'K')
    for (x in 65..95 step 9) {
        g.rect(x, 73, x + 1, 91, 'L')
        g.rect(x + 3, 72, x + 4, 88, 'k')
    }
    g.rect(57, 60, 104, 64, 'o')
    for (x in 59..101 step 7) g.rect(x, 56, x + 4, 61, 'K')
    g.rect(73, 83, 87, 96, 'o')
    g.rect(75, 85, 85, 95, 'w')
    g.rect(78, 89, 82, 95, 'E')
    g.rect(79, 87, 81, 91, 'Y')
    if (beaconLit) {
        g.circle(80, 56, 8, 'e')
        g.circle(80, 54, 5, 'E')
        g.circle(80, 52, 2, 'Y')
    } else {
        g.rect(77, 54, 83, 58, 'e')
        g.rect(79, 52, 81, 56, 'E')
    }
    // Each purchased structure changes the skyline at its own world position.
    if ((levels["coalpit"] ?: 0) > 0) {
        g.rect(15, 90, 39, 101, 'o'); g.rect(17, 91, 37, 99, 't')
        g.triangle(13, 91, 41, 91, 27, 80, 'w')
        g.rect(21, 95, 32, 99, 'e'); g.rect(24, 94, 29, 98, 'E')
        g.rect(34, 80, 38, 89, 'K')
    }
    if ((levels["hollowmill"] ?: 0) > 0) {
        g.rect(125, 76, 142, 99, 'o'); g.rect(127, 78, 140, 98, 'K')
        g.triangle(123, 78, 144, 78, 134, 66, 'w')
        g.circle(134, 81, 2, 'o')
    }
    if ((levels["belltower"] ?: 0) > 0) {
        g.rect(39, 53, 54, 90, 'o'); g.rect(41, 55, 52, 89, 'K')
        g.triangle(38, 55, 55, 55, 47, 43, 'w')
        g.rect(45, 58, 49, 70, 'o')
        g.rect(44, 74, 50, 77, 'L')
    }
    if ((levels["moonforge"] ?: 0) > 0) {
        g.rect(107, 83, 124, 98, 'o'); g.rect(109, 85, 122, 97, 'K')
        g.triangle(105, 84, 126, 84, 115, 71, 'W')
        g.rect(111, 90, 119, 96, 'e'); g.rect(113, 89, 117, 95, 'E')
        g.rect(121, 68, 124, 81, 'o')
    }
    if ((levels["bonelibrary"] ?: 0) > 0) {
        g.rect(32, 72, 56, 90, 'o'); g.rect(34, 74, 54, 88, 'B')
        g.triangle(30, 73, 58, 73, 44, 64, 'p')
        for (x in 38..50 step 6) g.rect(x, 78, x + 2, 86, 'p')
    }
    if ((levels["citadel"] ?: 0) > 0) {
        for (x in listOf(57, 96)) {
            g.rect(x, 35, x + 9, 66, 'o'); g.rect(x + 2, 37, x + 7, 64, 'L')
            g.triangle(x - 3, 36, x + 12, 36, x + 4, 23, 'w')
            g.rect(x + 3, 45, x + 5, 49, 'E')
        }
        g.rect(75, 44, 86, 57, 'o'); g.rect(77, 46, 84, 55, 'K')
        g.triangle(73, 45, 88, 45, 80, 33, 'W')
    }
    if ((levels["emberwell"] ?: 0) > 0) {
        g.circle(10, 98, 9, 'o'); g.circle(10, 98, 6, 'K')
        g.circle(10, 98, 4, 'e'); g.circle(10, 98, 2, 'E')
        g.rect(4, 89, 16, 91, 'B')
    }
    if ((levels["gravegarden"] ?: 0) > 0) {
        g.rect(144, 95, 158, 101, 'p')
        for (x in listOf(146, 152, 157)) {
            g.rect(x, 87, x + 2, 95, 'B'); g.circle(x + 1, 86, 2, 'B')
        }
        g.rect(149, 83, 150, 90, 'f')
    }
    if ((levels["soulharbor"] ?: 0) > 0) {
        g.rect(0, 80, 16, 86, 'K'); g.rect(3, 70, 5, 81, 'o')
        g.triangle(6, 69, 17, 78, 6, 78, 'B')
        g.rect(0, 86, 18, 89, 't'); g.set(13, 82, 'P')
    }
    if ((levels["stormspire"] ?: 0) > 0) {
        g.rect(108, 49, 119, 83, 'o'); g.rect(110, 51, 117, 81, 'K')
        g.triangle(106, 50, 121, 50, 113, 31, 'w')
        g.rect(112, 23, 114, 33, 'B'); g.set(113, 22, 'P')
    }
    if ((levels["wyrmroost"] ?: 0) > 0) {
        g.rect(143, 68, 157, 84, 'o'); g.rect(145, 70, 155, 82, 'W')
        g.triangle(138, 69, 159, 69, 149, 55, 'w')
        g.triangle(138, 66, 146, 62, 145, 73, 'R')
        g.triangle(150, 64, 159, 60, 154, 73, 'R')
        g.rect(148, 67, 151, 70, 'Y')
    }
    if ((levels["eclipsethrone"] ?: 0) > 0) {
        g.circle(80, 22, 15, 'p'); g.circle(80, 22, 11, 'R'); g.circle(80, 22, 8, 'a')
        g.rect(72, 37, 88, 48, 'o'); g.rect(74, 39, 86, 46, 'L')
        g.triangle(70, 38, 90, 38, 80, 27, 'W')
        g.rect(79, 36, 81, 42, 'Y')
    }
    // Mastery illuminates a structure's crest, so upgrades are visible in the kingdom too.
    val crests = mapOf(
        "coalpit" to (27 to 80), "hollowmill" to (134 to 66), "belltower" to (47 to 43),
        "moonforge" to (115 to 72), "bonelibrary" to (44 to 64), "citadel" to (80 to 34),
        "emberwell" to (10 to 87), "gravegarden" to (150 to 82), "soulharbor" to (9 to 69),
        "stormspire" to (113 to 21), "wyrmroost" to (149 to 54), "eclipsethrone" to (80 to 22),
    )
    for ((id, crest) in crests) {
        val tier = masteries[id] ?: 0
        if (tier > 0 && (levels[id] ?: 0) > 0) {
            g.circle(crest.first, crest.second, tier + 1, if (tier >= 3) 'Y' else 'P')
            g.set(crest.first, crest.second, 'E')
        }
    }
    specializations.forEach { (id, path) ->
        crests[id]?.let { (x, y) ->
            g.ring(x, y, 5, 3, if (path == "industry") 'E' else 'P')
            g.set(x, y, if (path == "industry") 'Y' else 'B')
        }
    }
    // Each liberated border raises an illuminated banner in the main kingdom skyline.
    val borderBanners = mapOf("forest" to (23 to 76), "fen" to (139 to 78),
        "coast" to (8 to 65), "ruins" to (94 to 52))
    borderBanners.forEach { (id, position) ->
        if (id in conquered) {
            val (x, y) = position
            g.rect(x, y, x + 1, y + 12, 'B')
            g.rect(x + 2, y + 1, x + 8, y + 4, 'E')
            g.rect(x + 2, y + 2, x + 5, y + 3, 'Y')
        }
    }
    // Soot, worn masonry and orange sparks give the scene a lived-in pixel texture.
    for (i in 0 until 112) {
        val x = (i * 53 + i * i * 3) % 160
        val y = 93 + (i * 17 % 27)
        g.set(x, y, if (i % 7 == 0) 'E' else if (i % 3 == 0) 'h' else 'G')
    }
    if (gloom >= 55) {
        for (i in 0 until gloom / 2) {
            val x = (i * 71 + i * i * 5) % 160
            val y = 74 + (i * 19 % 40)
            g.set(x, y, 'p')
        }
    }
    return g.build(worldColors)
}

/** Sparse transparent layer: only moving pixels are painted each frame. */
fun ashKingdomMotionArt(levels: Map<String, Int>, gloom: Int, beaconLit: Boolean, frame: Int,
                        masteries: Map<String, Int> = emptyMap(), specializations: Map<String, String> = emptyMap()): PixelArt {
    val g = PixelGridBuilder(160, 120)
    for (i in 0 until 20) {
        val x = (i * 47 + i * i * 13) % 158 + 1
        val y = (i * 29 + i * i * 7) % 66 + 2
        if ((i * 3 + frame / 3) % 13 < 3) g.set(x, y, 'S')
    }
    if (frame % 96 in 9..18) {
        val drift = (frame % 96 - 9) * 3
        g.line(10 + drift, 18, 18 + drift, 14, 'S')
    }
    g.circle(118, 24, if (frame % 16 < 8) 4 else 3, 'Q')
    // The beacon and the central hearth flicker independently.
    val flameShift = frame % 4 - 1
    g.rect(78 + flameShift, 89, 82 + flameShift, 94, 'E')
    g.rect(79 - flameShift / 2, 86 - frame % 2, 81 - flameShift / 2, 90, 'Y')
    val beaconHeight = if (beaconLit) 10 else 5
    g.rect(79 + frame % 3 - 1, 57 - beaconHeight, 81 + frame % 3 - 1, 57, 'Y')
    repeat(7) { spark ->
        val rise = (frame * 2 + spark * 9) % 28
        g.set(77 + (spark * 7 + frame) % 8, 84 - rise, if (spark % 3 == 0) 'Y' else 'E')
    }
    if ((levels["coalpit"] ?: 0) > 0) repeat(4) { puff ->
        val rise = (frame * 2 + puff * 7) % 25
        g.circle(36 + (puff + frame / 4) % 4 - 2, 78 - rise, 2 + rise / 10, 'z')
    }
    if ((levels["hollowmill"] ?: 0) > 0) {
        val directions = listOf(0 to -14, 10 to -10, 14 to 0, 10 to 10, 0 to 14, -10 to 10, -14 to 0, -10 to -10)
        val phase = frame / 2 % 8
        repeat(4) { blade ->
            val (dx, dy) = directions[(phase + blade * 2) % 8]
            g.line(134, 81, 134 + dx, 81 + dy, 'T', 2)
            g.set(134 + dx, 81 + dy, 'B')
        }
        g.circle(134, 81, 2, 'o')
    }
    if ((levels["belltower"] ?: 0) > 0) {
        val swing = when (frame / 3 % 4) { 0 -> -2; 2 -> 2; else -> 0 }
        g.rect(46 + swing, 59, 48 + swing, 68, 'B')
    }
    if ((levels["moonforge"] ?: 0) > 0) repeat(5) { spark ->
        val rise = (frame * 3 + spark * 5) % 20
        g.set(113 + (spark * 3 + frame) % 8, 87 - rise, if (spark % 2 == 0) 'Y' else 'E')
    }
    if ((levels["bonelibrary"] ?: 0) > 0) g.set(40 + frame / 5 % 3 * 6, 81, 'P')
    if ((levels["citadel"] ?: 0) > 0) {
        val banner = if (frame / 3 % 2 == 0) 5 else 3
        g.rect(64, 38, 64 + banner, 40, 'R')
        g.rect(102, 38, 102 + banner, 40, 'R')
    }
    if ((levels["emberwell"] ?: 0) > 0) {
        g.circle(10, 98, if (frame % 8 < 4) 3 else 2, 'E')
        g.set(8 + frame % 5, 91 - frame % 7, 'Y')
    }
    if ((levels["gravegarden"] ?: 0) > 0) repeat(3) { i ->
        g.set(146 + i * 5, 82 - (frame + i * 3) % 6, if (frame % 2 == 0) 'P' else 'V')
    }
    if ((levels["soulharbor"] ?: 0) > 0) {
        val sail = frame / 3 % 3
        g.triangle(6, 69, 17 + sail, 78, 6, 78, 'B')
        g.set(10 + frame % 7, 81, 'P')
    }
    if ((levels["stormspire"] ?: 0) > 0 && frame % 16 < 8) {
        g.line(113, 22, 107 + frame % 4, 35, 'P')
        g.line(113, 22, 120 - frame % 3, 36, 'S')
    }
    if ((levels["wyrmroost"] ?: 0) > 0) {
        val wing = if (frame / 3 % 2 == 0) 8 else 4
        g.line(149, 65, 139, 65 - wing, 'R', 2)
        g.line(150, 65, 159, 65 - wing, 'R', 2)
        g.set(151 + frame % 4, 72 - frame % 5, 'E')
    }
    if ((levels["eclipsethrone"] ?: 0) > 0) {
        g.circle(80, 22, if (frame % 16 < 8) 10 else 9, 'P')
        g.circle(80, 22, 7, 'a')
        g.set(80, 22, 'Y')
    }
    val specializedCrests = mapOf("coalpit" to (27 to 80), "moonforge" to (115 to 72),
        "bonelibrary" to (44 to 64), "citadel" to (80 to 34))
    specializations.forEach { (id, path) ->
        specializedCrests[id]?.let { (x, y) ->
            val rise = frame % 12
            g.set(x - 2 + frame % 5, y - 4 - rise, if (path == "industry") 'Y' else 'P')
        }
    }
    if (masteries.isNotEmpty()) repeat(8) { i ->
        val x = (i * 39 + frame * 3) % 160
        val y = 48 + (i * 11 + frame) % 48
        if (i < masteries.values.sum()) g.set(x, y, 'P')
    }
    if (gloom >= 55) repeat(gloom / 3) { i ->
        val x = (i * 71 + i * i * 5 + frame / 2) % 160
        val y = 74 + (i * 19 % 40)
        g.set(x, y, 'p')
    }
    // Mist crosses in front of the earth but leaves the buildings legible.
    repeat(7) { i ->
        val x = (i * 31 + frame * 2) % 185 - 25
        g.rect(x, 105 + i % 3, x + 17, 106 + i % 3, 'V')
    }
    return g.build(worldColors)
}

private fun fir(g: PixelGridBuilder, x: Int, bottom: Int, height: Int, color: Char) {
    g.rect(x - 1, bottom - 7, x + 1, bottom, 'o')
    g.triangle(x - 7, bottom - 5, x + 7, bottom - 5, x, bottom - height, color)
    g.triangle(x - 5, bottom - height / 2, x + 5, bottom - height / 2, x, bottom - height - 5, color)
}

fun ashBuildingIcon(id: String, frame: Int = 0): PixelArt {
    val g = PixelGridBuilder(32, 32)
    g.rect(0, 0, 31, 31, 'a')
    g.rect(2, 2, 29, 29, 'b')
    for (x in 4..28 step 5) g.set(x, 27, 'G')
    when (id) {
        "coalpit" -> {
            g.triangle(6, 19, 26, 19, 16, 6, 'w')
            g.rect(8, 19, 24, 26, 't')
            g.rect(13, 20, 19, 27, 'e')
            g.rect(15 + frame % 3 - 1, 17 - frame % 2, 17 + frame % 3 - 1, 23, 'Y')
            g.set(12 + frame % 5, 13 - frame % 4, 'z')
        }
        "hollowmill" -> {
            g.rect(10, 10, 22, 27, 'K')
            g.triangle(8, 11, 24, 11, 16, 4, 'W')
            val blades = listOf(0 to -11, 8 to -8, 11 to 0, 8 to 8, 0 to 11, -8 to 8, -11 to 0, -8 to -8)
            repeat(4) { blade ->
                val (dx, dy) = blades[(frame + blade * 2) % 8]
                g.line(16, 16, 16 + dx, 16 + dy, 'T')
            }
            g.circle(16, 16, 1, 'o')
        }
        "belltower" -> {
            g.rect(10, 9, 22, 28, 'K')
            g.triangle(8, 9, 24, 9, 16, 3, 'w')
            g.rect(13, 12, 19, 21, 'o')
            g.rect(14 + frame / 2 % 3 - 1, 15, 18 + frame / 2 % 3 - 1, 20, 'B')
            g.set(16, 22, 'E')
        }
        "moonforge" -> {
            g.rect(6, 17, 26, 27, 'K')
            g.triangle(4, 17, 28, 17, 16, 7, 'W')
            g.rect(11, 21, 21, 28, 'e')
            g.rect(14 + frame % 3 - 1, 17 - frame % 2, 18, 24, 'Y')
            g.set(10 + frame % 11, 12 - frame % 6, 'E')
        }
        "bonelibrary" -> {
            g.rect(5, 12, 27, 27, 'B')
            g.triangle(4, 12, 28, 12, 16, 4, 'p')
            for (x in 9..23 step 7) g.rect(x, 15, x + 3, 24, 'p')
            g.set(10 + frame % 3 * 7, 19, 'P')
        }
        "citadel" -> {
            g.rect(6, 14, 26, 28, 'K')
            g.rect(5, 10, 11, 25, 'L'); g.rect(21, 10, 27, 25, 'L')
            g.triangle(4, 11, 12, 11, 8, 3, 'w')
            g.triangle(20, 11, 28, 11, 24, 3, 'w')
            g.rect(14, 19, 18, 27, 'E')
            g.rect(21, 8, 21 + frame % 3, 9, 'R')
        }
        "emberwell" -> {
            g.circle(16, 19, 10, 'K'); g.circle(16, 19, 7, 'o')
            g.circle(16, 19, 5, 'e'); g.circle(16, 19, if (frame % 4 < 2) 3 else 2, 'Y')
            g.rect(6, 10, 26, 12, 'B')
        }
        "gravegarden" -> {
            for (x in listOf(7, 15, 23)) {
                g.rect(x, 14, x + 3, 25, 'B'); g.circle(x + 1, 13, 2, 'B')
            }
            g.set(10 + frame % 11, 11 - frame % 5, 'P')
        }
        "soulharbor" -> {
            g.rect(4, 23, 28, 26, 't'); g.rect(10, 5, 12, 24, 'K')
            g.triangle(13, 7, 25 + frame / 4 % 3, 21, 13, 21, 'B')
            g.set(8 + frame % 12, 27, 'P')
        }
        "stormspire" -> {
            g.rect(11, 13, 21, 27, 'K'); g.triangle(9, 13, 23, 13, 16, 4, 'w')
            g.rect(15, 2, 17, 7, 'B')
            if (frame % 8 < 5) g.line(16, 3, 23, 18, 'P')
        }
        "wyrmroost" -> {
            g.triangle(5, 19, 27, 19, 16, 7, 'w'); g.rect(9, 19, 23, 27, 'W')
            val lift = frame / 3 % 3
            g.line(16, 14, 4, 9 - lift, 'R', 2); g.line(16, 14, 28, 9 - lift, 'R', 2)
            g.set(18, 17, 'Y')
        }
        "eclipsethrone" -> {
            g.circle(16, 13, 9, 'R'); g.circle(16, 13, 6, 'a')
            g.rect(10, 19, 22, 27, 'L'); g.triangle(8, 19, 24, 19, 16, 14, 'W')
            g.set(16, 13, if (frame % 4 < 2) 'Y' else 'P')
        }
    }
    g.rectOutline(0, 0, 31, 31, 'o')
    return g.build(worldColors)
}
