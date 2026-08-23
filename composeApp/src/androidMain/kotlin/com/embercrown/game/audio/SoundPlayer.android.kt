package com.embercrown.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

private const val POOL_SIZE = 3

actual class SoundPlayer actual constructor() {
    private class Pool(val tracks: List<AudioTrack>) {
        var next = 0
    }

    private val pools = mutableMapOf<SfxId, Pool>()
    private var volume: Float = 0.7f
    private var muted: Boolean = false

    actual fun preload(clips: Map<SfxId, PcmClip>) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        clips.forEach { (id, clip) ->
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(clip.sampleRateHz)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val bufferSizeBytes = clip.samples.size * 2
            val instances = (0 until POOL_SIZE).mapNotNull {
                runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(attributes)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(bufferSizeBytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                        .also { track ->
                            track.write(clip.samples, 0, clip.samples.size)
                            track.setVolume(volume)
                        }
                }.getOrNull()
            }
            if (instances.isNotEmpty()) pools[id] = Pool(instances)
        }
    }

    actual fun play(id: SfxId) {
        if (muted) return
        val pool = pools[id] ?: return
        val track = pool.tracks[pool.next]
        pool.next = (pool.next + 1) % pool.tracks.size
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        pools.values.forEach { pool -> pool.tracks.forEach { it.setVolume(this.volume) } }
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
    }
}
