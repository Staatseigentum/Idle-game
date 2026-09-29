package com.embercrown.game.reboot

import androidx.compose.ui.graphics.Color
import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.PixelGridBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hand-built, transparent 160x120 frames for the Ash Ritual. The kingdom beneath is real game art. */
internal fun ritualCinematicArt(seconds: Float, frame: Int): PixelArt {
    val g = PixelGridBuilder(160, 120)
    val palette = mapOf(
        'o' to Color(0xFF301824), 'r' to Color(0xFF812F3A),
        'f' to Color(0xFFDB5B36), 'F' to Color(0xFFFF9B46), 'Y' to Color(0xFFFFE1A0),
        's' to Color(0xFFB79B8E), 'S' to Color(0xFFDEE1CA),
        'v' to Color(0xFF5C4968), 'V' to Color(0xFF9B7384),
    )
    val t = seconds.coerceIn(0f, 7.5f)
    // The seven crown-seals gather around the city and draw glowing spokes toward its hearth.
    if (t < 3.1f) {
        val charge = (t / 1.7f).coerceIn(0f, 1f)
        repeat(7) { i ->
            val angle = 2.0 * PI * i / 7.0 - PI / 2
            val radius = 78f - charge * 42f + sin(frame * 0.19 + i).toFloat() * 2f
            val x = (80 + cos(angle) * radius).toInt()
            val y = (63 + sin(angle) * radius * 0.64).toInt()
            if ((frame + i) % 4 != 0) {
                g.ring(x, y, 4, 2, if (i % 2 == 0) 'F' else 'r')
                g.set(x, y, 'Y')
            }
            if (t > 0.65f) {
                val ray = ((t - 0.65f) / 1.3f).coerceIn(0f, 1f)
                val endX = (x + (80 - x) * ray).toInt()
                val endY = (y + (75 - y) * ray).toInt()
                g.line(x, y, endX, endY, if (i % 2 == 0) 'f' else 'v')
            }
        }
    }
    // Flames ignite at named buildings, propagate across rooftops, and shed individual embers.
    if (t in 0.8f..3.45f) {
        val burn = ((t - 0.8f) / 2.35f).coerceIn(0f, 1f)
        val ignition = listOf(
            26 to 88, 47 to 51, 80 to 58, 115 to 77, 135 to 76,
            10 to 78, 150 to 70, 93 to 40, 56 to 89, 122 to 87,
        )
        ignition.forEachIndexed { index, (x, y) ->
            val age = ((burn * 1.5f - index * 0.075f).coerceIn(0f, 1f))
            if (age > 0f) {
                val width = 2 + (age * 10).toInt()
                val height = 3 + (age * 17).toInt()
                g.ellipse(x, y + 3, width, 3 + (age * 4).toInt(), 'r')
                val flicker = (frame + index * 3) % 5 - 2
                g.triangle(x - width, y + 2, x + width, y + 2, x + flicker, y - height, 'f')
                g.triangle(x - width / 2, y + 2, x + width / 2, y + 2,
                    x - 2 + flicker, y - height * 2 / 3, 'F')
                g.triangle(x - width / 3, y + 1, x + width / 3, y + 1,
                    x + flicker, y - height / 2, 'Y')
                if (age > 0.28f) {
                    repeat(3) { puff ->
                        val drift = (frame / 3 + puff * 5 + index * 2) % 17
                        val smokeX = x + (puff - 1) * 4 + drift / 4
                        val smokeY = y - height - drift * 2
                        g.ellipse(smokeX, smokeY, 2 + puff % 2, 1 + puff % 2,
                            if (puff % 2 == 0) 'v' else 's')
                    }
                }
            }
        }
        if (t > 2.05f) {
            // The blood moon itself fractures when the crown can no longer hold the realm.
            g.line(112, 18, 120, 28, 'o', 2)
            g.line(120, 28, 125, 43, 'o', 2)
            g.line(120, 28, 133, 23, 'o')
        }
        val ground = (burn * 29).toInt()
        repeat(42) { i ->
            val x = (i * 43 + i * i * 11) % 160
            val rise = ((frame * (1 + i % 3) + i * 17) % 32)
            val y = 111 - ground - rise
            if (y in 12..112) g.set(x, y, if (i % 5 == 0) 'Y' else if (i % 2 == 0) 'F' else 's')
        }
        if (burn > 0.6f) {
            // Pixel chunks slide off the citadel instead of a flat fullscreen explosion.
            repeat(20) { i ->
                val x = 59 + i * 3
                val drop = ((burn - 0.6f) * 34).toInt() + i % 4
                g.rect(x, 58 + drop, x + 2, 59 + drop, if (i % 3 == 0) 'o' else 'r')
            }
        }
    }
    // A large, legible crown forms in front of the castle and cracks into separate shards.
    if (t in 1.35f..4.15f) {
        val scale = if (t < 1.95f) ((t - 1.35f) / 0.6f).coerceIn(0f, 1f) else 1f
        val shatter = ((t - 3.02f) / 1.05f).coerceIn(0f, 1f)
        if (scale > 0f) {
            val lift = (1f - scale) * 25f
            val spread = (shatter * 45).toInt()
            val fall = (shatter * shatter * 28).toInt()
            fun shard(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int, drift: Int, c: Char) {
                g.triangle(x0 + drift, (y0 + lift).toInt() + fall,
                    x1 + drift, (y1 + lift).toInt() + fall,
                    x2 + drift, (y2 + lift).toInt() + fall, c)
            }
            shard(58, 44, 64, 58, 76, 58, -spread, 'Y')
            shard(70, 55, 80, 36, 88, 55, -spread / 3, 'F')
            shard(84, 58, 97, 42, 103, 58, spread, 'Y')
            g.rect(61 - spread, 58 + fall, 75 - spread, 63 + fall, 'F')
            g.rect(78, 58 + fall + spread / 3, 88, 63 + fall + spread / 3, 'Y')
            g.rect(91 + spread, 58 + fall, 100 + spread, 63 + fall, 'F')
            if (t in 2.15f..3.25f) {
                val crack = ((t - 2.15f) * 16).toInt()
                g.line(81, 40, 77, 40 + crack, 'o', 2)
                g.line(77, 40 + crack, 83, 45 + crack, 'o', 2)
            }
        }
    }
    if (t in 3.02f..4.4f) {
        val blast = ((t - 3.02f) / 1.38f).coerceIn(0f, 1f)
        repeat(96) { i ->
            val a = 2.0 * PI * i / 96.0
            val radius = blast * (88 + i % 7 * 3)
            val x = (80 + cos(a) * radius).toInt()
            val y = (61 + sin(a) * radius * 0.75).toInt()
            g.rect(x, y, x + i % 3, y + i % 2, if (i % 4 == 0) 'Y' else 'F')
        }
    }
    // Ash falls, then the same particles reverse and build a new ring over the new realm.
    if (t in 3.5f..7.5f) {
        val rebirth = ((t - 4.35f) / 2.3f).coerceIn(0f, 1f)
        repeat(105) { i ->
            val x = (i * 37 + i * i * 3 + (sin(frame * 0.15 + i).toFloat() * 3).toInt()) % 160
            val drift = if (rebirth < 0.1f) frame + i * 7 else (frame / 2 + i * 7)
            val y = (drift % 134) - 8
            if (y in 0..119 && (i % 4 != 0 || rebirth > 0.3f))
                g.set(x, y, if (rebirth > 0.55f && i % 3 == 0) 'Y' else 's')
        }
        if (rebirth > 0.08f) {
            val r = (rebirth * 32).toInt().coerceAtLeast(1)
            g.ring(80, 57, r, (r - 1).coerceAtLeast(0), if (rebirth > 0.6f) 'Y' else 'F')
            repeat(12) { i ->
                val a = 2.0 * PI * i / 12.0 + frame * 0.025
                val x = (80 + cos(a) * r).toInt()
                val y = (57 + sin(a) * r * 0.7).toInt()
                g.set(x, y, 'Y')
            }
            val flame = (rebirth * 17).toInt()
            g.triangle(76, 87, 84, 87, 80, 87 - flame, 'F')
            g.triangle(78, 86, 82, 86, 80, 85 - flame / 2, 'Y')
        }
    }
    return g.build(palette)
}
