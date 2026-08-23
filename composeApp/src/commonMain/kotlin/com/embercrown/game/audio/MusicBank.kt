package com.embercrown.game.audio

/**
 * A single ambient background theme, looped forever by [MusicPlayer]. Built the same way as
 * [SoundBank]'s one-shots (concatenated decay-enveloped tones) rather than a raw sustained
 * waveform: every note fades toward silence before the next starts, so the wrap-around point
 * from the last note back to the first is quiet on both sides and doesn't need real crossfade
 * math to avoid an audible click.
 */
object MusicBank {
    fun theme(): PcmClip = PcmClip(concat(*loopNotes()))

    private fun lead(freqHz: Double, durationSeconds: Double, amplitude: Double = 0.16): ShortArray =
        applyDecayEnvelope(sineWave(freqHz, durationSeconds, amplitude), decaySeconds = durationSeconds * 0.85)

    private fun bass(freqHz: Double, durationSeconds: Double, amplitude: Double = 0.11): ShortArray =
        applyDecayEnvelope(sineWave(freqHz, durationSeconds, amplitude), decaySeconds = durationSeconds * 0.9)

    // A slow four-bar wander through a C-minor i-VI-III-VII progression (Cm, Ab, Eb, Bb), one
    // arpeggiated triad per bar at 0.55s/note (~8.8s loop total) with a root-note bass under beat 1.
    private fun loopNotes(): Array<ShortArray> {
        val beat = 0.55
        fun bar(root: Double, third: Double, fifth: Double): List<ShortArray> = listOf(
            mix(lead(root, beat), bass(root / 2, beat)),
            lead(third, beat),
            lead(fifth, beat),
            lead(third, beat),
        )
        val bars = bar(130.81, 155.56, 196.00) + // Cm: C3-Eb3-G3
            bar(103.83, 130.81, 155.56) + // Ab: Ab2-C3-Eb3
            bar(155.56, 196.00, 233.08) + // Eb: Eb3-G3-Bb3
            bar(116.54, 146.83, 174.61) // Bb: Bb2-D3-F3
        return bars.toTypedArray()
    }
}
