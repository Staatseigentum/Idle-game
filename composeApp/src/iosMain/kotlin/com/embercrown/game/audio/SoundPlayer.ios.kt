package com.embercrown.game.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

private const val POOL_SIZE = 3

// Unverified on this machine: Kotlin/Native iOS compilation requires Xcode on macOS, which this
// Windows environment doesn't have. Written carefully against the standard AVAudioPlayer/NSData
// cinterop patterns, but treat as untested until built on a Mac.
@OptIn(ExperimentalForeignApi::class)
actual class SoundPlayer actual constructor() {
    private class Pool(val players: List<AVAudioPlayer>) {
        var next = 0
    }

    private val pools = mutableMapOf<SfxId, Pool>()
    private var volume: Float = 0.7f
    private var muted: Boolean = false

    actual fun preload(clips: Map<SfxId, PcmClip>) {
        clips.forEach { (id, clip) ->
            val data = clip.toWavBytes().toNSData()
            val instances = (0 until POOL_SIZE).mapNotNull {
                AVAudioPlayer(data = data, error = null).also { player ->
                    player.volume = volume
                    player.prepareToPlay()
                }
            }
            if (instances.isNotEmpty()) pools[id] = Pool(instances)
        }
    }

    actual fun play(id: SfxId) {
        if (muted) return
        val pool = pools[id] ?: return
        val player = pool.players[pool.next]
        pool.next = (pool.next + 1) % pool.players.size
        player.stop()
        player.currentTime = 0.0
        player.play()
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        pools.values.forEach { pool -> pool.players.forEach { it.volume = this.volume } }
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.convert())
}
