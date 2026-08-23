package com.embercrown.game.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

// Unverified on this machine: Kotlin/Native iOS compilation requires Xcode on macOS, which this
// Windows environment doesn't have. Written against the same AVAudioPlayer pattern as
// SoundPlayer.ios.kt, with numberOfLoops = -1 for infinite native looping.
@OptIn(ExperimentalForeignApi::class)
actual class MusicPlayer actual constructor() {
    private var player: AVAudioPlayer? = null
    private var volume: Float = 0.5f
    private var muted: Boolean = false
    private var playRequested = false

    actual fun preload(clip: PcmClip) {
        val data = clip.toWavBytes().toNSData()
        val p = AVAudioPlayer(data = data, error = null)
        p.numberOfLoops = -1
        p.volume = if (muted) 0f else volume
        p.prepareToPlay()
        player = p
        if (playRequested && !muted) p.play()
    }

    actual fun play() {
        playRequested = true
        if (!muted) player?.play()
    }

    actual fun stop() {
        playRequested = false
        player?.stop()
        player?.currentTime = 0.0
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        if (!muted) player?.volume = this.volume
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
        val p = player ?: return
        if (muted) {
            p.volume = 0f
        } else {
            p.volume = volume
            if (playRequested) p.play()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.convert())
}
