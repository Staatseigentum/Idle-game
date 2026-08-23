package com.embercrown.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual class MusicPlayer actual constructor() {
    private var track: AudioTrack? = null
    private var volume: Float = 0.5f
    private var muted: Boolean = false
    private var playRequested = false

    actual fun preload(clip: PcmClip) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(clip.sampleRateHz)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bufferSizeBytes = clip.samples.size * 2
        runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also { t ->
                    t.write(clip.samples, 0, clip.samples.size)
                    t.setLoopPoints(0, clip.samples.size, -1)
                    t.setVolume(volume)
                    track = t
                    if (playRequested && !muted) t.play()
                }
        }
    }

    actual fun play() {
        playRequested = true
        if (!muted) track?.play()
    }

    actual fun stop() {
        playRequested = false
        // pause() only, no flush(): flush() is meant for MODE_STREAM tracks and can throw on a
        // MODE_STATIC track — pausing is enough since this player only ever has one track/song.
        track?.pause()
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        track?.setVolume(this.volume)
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
        val t = track ?: return
        if (muted) {
            t.pause()
        } else if (playRequested) {
            t.play()
        }
    }
}
