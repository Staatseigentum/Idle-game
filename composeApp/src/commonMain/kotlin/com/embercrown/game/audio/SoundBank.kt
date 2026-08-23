package com.embercrown.game.audio

enum class SfxId { TAP, PURCHASE, DENY, ACHIEVEMENT, AGE_UP, DRAGON, OMEN }

/** Short chiptune-style one-shots, synthesized once at first use — no external audio assets. */
object SoundBank {
    val clips: Map<SfxId, PcmClip> by lazy {
        mapOf(
            SfxId.TAP to tapClip(),
            SfxId.PURCHASE to purchaseClip(),
            SfxId.DENY to denyClip(),
            SfxId.ACHIEVEMENT to achievementClip(),
            SfxId.AGE_UP to ageUpClip(),
            SfxId.DRAGON to dragonClip(),
            SfxId.OMEN to omenClip(),
        )
    }

    private fun tone(freqHz: Double, durationSeconds: Double, amplitude: Double = 0.5): ShortArray =
        applyDecayEnvelope(squareWave(freqHz, durationSeconds, amplitude), decaySeconds = durationSeconds * 0.6)

    private fun tapClip(): PcmClip = PcmClip(tone(1046.5, 0.07, amplitude = 0.4))

    private fun purchaseClip(): PcmClip = PcmClip(concat(tone(659.3, 0.06), tone(987.8, 0.09)))

    private fun denyClip(): PcmClip {
        val buzz = squareWave(110.0, 0.14, amplitude = 0.35)
        val hiss = noiseBurst(0.14, amplitude = 0.12)
        return PcmClip(applyDecayEnvelope(mix(buzz, hiss), decaySeconds = 0.1))
    }

    private fun achievementClip(): PcmClip =
        PcmClip(concat(tone(523.3, 0.08), tone(659.3, 0.08), tone(784.0, 0.08), tone(1046.5, 0.16, amplitude = 0.55)))

    private fun ageUpClip(): PcmClip =
        PcmClip(
            concat(
                tone(392.0, 0.09),
                tone(523.3, 0.09),
                tone(659.3, 0.09),
                tone(784.0, 0.09),
                tone(1046.5, 0.28, amplitude = 0.55),
            ),
        )

    private fun dragonClip(): PcmClip {
        val swoop = concat(
            tone(196.0, 0.08, amplitude = 0.35),
            tone(246.9, 0.08, amplitude = 0.4),
            tone(329.6, 0.10, amplitude = 0.45),
            tone(440.0, 0.14, amplitude = 0.5),
        )
        val wind = applyDecayEnvelope(noiseBurst(0.4, amplitude = 0.18), decaySeconds = 0.35)
        return PcmClip(mix(swoop, wind))
    }

    /** A descending, unsettled two-tone with a faint hiss underneath — the Verfall Omen's sighting cue. */
    private fun omenClip(): PcmClip {
        val motif = concat(tone(233.1, 0.12, amplitude = 0.32), tone(174.6, 0.18, amplitude = 0.3))
        val hiss = applyDecayEnvelope(noiseBurst(0.3, amplitude = 0.08), decaySeconds = 0.28)
        return PcmClip(mix(motif, hiss))
    }
}
