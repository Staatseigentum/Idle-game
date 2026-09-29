package com.embercrown.game.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Original, asset-free soundscape for the Ash Kingdom. PCM keeps every platform in sync. */
object AshAudioBank {
    private const val RATE = 22_050

    val effects: Map<SfxId, PcmClip> by lazy {
        mapOf(
            SfxId.ASH_TAP to clip(mix(note(740.0, 0.075, 0.16), crackle(0.075, 0.055))),
            SfxId.ASH_BUILD to clip(mix(
                mix(note(196.0, 0.29, 0.25), note(783.99, 0.25, 0.14)),
                crackle(0.12, 0.095),
            )),
            SfxId.ASH_MASTERY to clip(concat(
                note(392.0, 0.12, 0.20), note(523.25, 0.13, 0.21),
                mix(note(783.99, 0.39, 0.24), note(1567.98, 0.39, 0.09)),
            )),
            SfxId.ASH_RELIC to clip(concat(
                note(523.25, 0.13, 0.19), note(622.25, 0.13, 0.18),
                mix(note(932.33, 0.43, 0.21), note(466.16, 0.43, 0.12)),
            )),
            SfxId.ASH_BEACON to clip(mix(
                sweep(170.0, 690.0, 0.52, 0.19), crackle(0.42, 0.06),
            )),
            SfxId.ASH_MARCH to clip(concat(
                mix(note(110.0, 0.17, 0.23), crackle(0.17, 0.07)),
                note(164.81, 0.18, 0.22), note(220.0, 0.22, 0.18),
            )),
            SfxId.ASH_RITUAL_CHARGE to clip(mix(
                sweep(55.0, 740.0, 1.05, 0.20), note(110.0, 1.05, 0.09),
            )),
            SfxId.ASH_RITUAL_FIRE to clip(mix(
                sweep(110.0, 520.0, 1.18, 0.16),
                mix(crackle(1.18, 0.13), note(55.0, 1.18, 0.12)),
            )),
            SfxId.ASH_RITUAL to clip(mix(
                sweep(680.0, 48.0, 1.28, 0.29),
                mix(note(46.25, 1.28, 0.23), crackle(1.0, 0.16)),
            )),
            SfxId.ASH_RITUAL_REBIRTH to clip(concat(
                mix(note(196.0, 0.25, 0.16), note(392.0, 0.25, 0.11)),
                mix(note(261.63, 0.29, 0.18), note(523.25, 0.29, 0.12)),
                mix(note(392.0, 0.52, 0.18), note(783.99, 0.52, 0.15)),
            )),
            SfxId.ASH_OMEN to clip(mix(
                mix(note(185.0, 0.52, 0.16), note(261.63, 0.52, 0.13)),
                crackle(0.27, 0.04),
            )),
            SfxId.ASH_GUIDE to clip(concat(
                note(392.0, 0.11, 0.13), note(523.25, 0.18, 0.15),
            )),
            SfxId.ASH_PAGE to clip(note(440.0, 0.045, 0.095)),
        )
    }

    /** A restrained C-minor / Ab / Eb / Bb lament; the loop breathes through silence at its seam. */
    val ambience: PcmClip by lazy {
        val bars = doubleArrayOf(65.41, 51.91, 77.78, 58.27)
        val bells = doubleArrayOf(
            392.00, 311.13, 261.63, 311.13,
            311.13, 261.63, 207.65, 261.63,
            466.16, 392.00, 311.13, 392.00,
            349.23, 293.66, 233.08, 293.66,
        )
        val seconds = 16.0
        val samples = ShortArray((RATE * seconds).toInt()) { index ->
            val time = index.toDouble() / RATE
            val bar = (time / 4.0).toInt().coerceIn(0, 3)
            val root = bars[bar]
            val beat = (time / 1.0).toInt().coerceIn(0, 15)
            val bellAge = time - beat
            val bellEnvelope = (bellAge / 0.025).coerceIn(0.0, 1.0) * exp(-bellAge * 2.8)
            val drone = sin(2.0 * PI * root * time) * 0.073 +
                sin(2.0 * PI * (root / 2.0) * time) * 0.065 +
                sin(2.0 * PI * (root * 1.5) * time) * 0.027
            val bell = (sin(2.0 * PI * bells[beat] * bellAge) +
                sin(2.0 * PI * bells[beat] * 2.003 * bellAge) * 0.22) * 0.085 * bellEnvelope
            val seamFade = minOf(1.0, time / 0.30, (seconds - time) / 0.55).coerceAtLeast(0.0)
            ((drone + bell) * seamFade * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        PcmClip(samples, RATE)
    }

    private fun clip(samples: ShortArray) = PcmClip(samples, RATE)

    private fun note(frequency: Double, seconds: Double, amplitude: Double): ShortArray {
        val count = (seconds * RATE).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / RATE
            val envelope = (t / 0.006).coerceIn(0.0, 1.0) *
                ((seconds - t) / seconds).coerceIn(0.0, 1.0).let { it * it }
            ((sin(2.0 * PI * frequency * t) +
                sin(2.0 * PI * frequency * 2.01 * t) * 0.23) *
                amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun sweep(start: Double, end: Double, seconds: Double, amplitude: Double): ShortArray {
        val count = (seconds * RATE).toInt()
        return ShortArray(count) { i ->
            val progress = i.toDouble() / count
            val phase = 2.0 * PI * seconds *
                (start * progress + (end - start) * progress * progress / 2.0)
            val envelope = sin(PI * progress).coerceAtLeast(0.0)
            (sin(phase) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun crackle(seconds: Double, amplitude: Double): ShortArray {
        val count = (seconds * RATE).toInt()
        val random = Random(0xA5C0)
        var previous = 0.0
        return ShortArray(count) { i ->
            val t = i.toDouble() / count
            previous = previous * 0.74 + random.nextDouble(-1.0, 1.0) * 0.26
            (previous * amplitude * (1.0 - t) * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
