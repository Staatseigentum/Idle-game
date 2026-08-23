package com.embercrown.game.audio

/**
 * Plays a single looping background track. Distinct from [SoundPlayer] (no round-robin pool —
 * one voice, loops forever until [stop]) since background music needs seamless native looping
 * rather than repeated one-shot triggering.
 */
expect class MusicPlayer() {
    fun preload(clip: PcmClip)
    fun play()
    fun stop()
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
}
