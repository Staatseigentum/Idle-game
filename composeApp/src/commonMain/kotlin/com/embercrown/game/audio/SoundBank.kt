package com.embercrown.game.audio

enum class SfxId { TAP, PURCHASE, DENY, ACHIEVEMENT, AGE_UP }

/** Short chiptune-style one-shots, synthesized once at first use — no external audio assets. */
object SoundBank {
    val clips: Map<SfxId, PcmClip> by lazy {
        mapOf(
            SfxId.TAP to tapClip(),
            SfxId.PURCHASE to purchaseClip(),
            SfxId.DENY to denyClip(),
            SfxId.ACHIEVEMENT to achievementClip(),
            SfxId.AGE_UP to ageUpClip(),
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
}
