package com.embercrown.game.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

const val DEFAULT_SAMPLE_RATE = 44100

fun sineWave(freqHz: Double, durationSeconds: Double, amplitude: Double = 0.5, sampleRateHz: Int = DEFAULT_SAMPLE_RATE): ShortArray {
    val count = (durationSeconds * sampleRateHz).toInt()
    return ShortArray(count) { i ->
        val t = i.toDouble() / sampleRateHz
        (sin(2.0 * PI * freqHz * t) * amplitude * Short.MAX_VALUE).toInt().toShort()
    }
}

fun squareWave(freqHz: Double, durationSeconds: Double, amplitude: Double = 0.5, sampleRateHz: Int = DEFAULT_SAMPLE_RATE): ShortArray {
    val count = (durationSeconds * sampleRateHz).toInt()
    val period = sampleRateHz / freqHz
    return ShortArray(count) { i ->
        val phase = (i % period) / period
        val v = if (phase < 0.5) amplitude else -amplitude
        (v * Short.MAX_VALUE).toInt().toShort()
    }
}

fun noiseBurst(durationSeconds: Double, amplitude: Double = 0.4, sampleRateHz: Int = DEFAULT_SAMPLE_RATE, random: Random = Random.Default): ShortArray {
    val count = (durationSeconds * sampleRateHz).toInt()
    return ShortArray(count) {
        (random.nextDouble(-1.0, 1.0) * amplitude * Short.MAX_VALUE).toInt().toShort()
    }
}

/** Ramps amplitude down to 0 over the last [decaySeconds] of [samples] so tones don't end in an audible click. */
fun applyDecayEnvelope(samples: ShortArray, decaySeconds: Double, sampleRateHz: Int = DEFAULT_SAMPLE_RATE): ShortArray {
    val decaySamples = (decaySeconds * sampleRateHz).toInt().coerceIn(0, samples.size)
    if (decaySamples == 0) return samples
    val start = samples.size - decaySamples
    return ShortArray(samples.size) { i ->
        if (i < start) {
            samples[i]
        } else {
            val progress = (i - start).toDouble() / decaySamples
            (samples[i] * (1.0 - progress)).toInt().toShort()
        }
    }
}

fun concat(vararg parts: ShortArray): ShortArray {
    val result = ShortArray(parts.sumOf { it.size })
    var offset = 0
    for (part in parts) {
        part.copyInto(result, offset)
        offset += part.size
    }
    return result
}

/** Mixes [a] and [b] sample-by-sample (the shorter is treated as silence past its end), clamped against overflow. */
fun mix(a: ShortArray, b: ShortArray): ShortArray {
    val size = maxOf(a.size, b.size)
    return ShortArray(size) { i ->
        val av = if (i < a.size) a[i].toInt() else 0
        val bv = if (i < b.size) b[i].toInt() else 0
        (av + bv).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
