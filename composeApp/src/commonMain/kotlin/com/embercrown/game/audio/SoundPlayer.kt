package com.embercrown.game.audio

/**
 * Plays short one-shot SFX. Every [SfxId] is backed by a small pool of platform playback
 * handles (≈3, round-robined on [play]) — a single handle would cut off its own tail when the
 * same effect fires twice in quick succession (e.g. rapid tapping), on every platform actual.
 */
expect class SoundPlayer() {
    fun preload(clips: Map<SfxId, PcmClip>)
    fun play(id: SfxId)
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
}
