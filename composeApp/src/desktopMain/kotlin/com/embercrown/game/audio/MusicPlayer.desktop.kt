package com.embercrown.game.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

actual class MusicPlayer actual constructor() {
    private var clip: Clip? = null
    private var volume: Float = 0.5f
    private var muted: Boolean = false
    private var playRequested = false

    actual fun preload(clip: PcmClip) {
        val format = AudioFormat(clip.sampleRateHz.toFloat(), 16, 1, true, false)
        val bytes = ByteArray(clip.samples.size * 2)
        for (i in clip.samples.indices) {
            val v = clip.samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        runCatching {
            val line = AudioSystem.getClip()
            line.open(format, bytes, 0, bytes.size)
            this.clip = line
            applyVolume()
            if (playRequested && !muted) startLoop()
        }
    }

    actual fun play() {
        playRequested = true
        if (!muted) startLoop()
    }

    actual fun stop() {
        playRequested = false
        runCatching { clip?.stop() }
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        applyVolume()
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
        val c = clip ?: return
        if (muted) runCatching { c.stop() } else if (playRequested) startLoop()
    }

    private fun startLoop() {
        val c = clip ?: return
        runCatching {
            if (!c.isRunning) {
                c.framePosition = 0
                c.loop(Clip.LOOP_CONTINUOUSLY)
            }
        }
    }

    private fun applyVolume() {
        val c = clip ?: return
        val db = if (volume <= 0f) -80f else (20.0 * log10(volume.toDouble())).toFloat()
        runCatching {
            val gain = when {
                c.isControlSupported(FloatControl.Type.MASTER_GAIN) ->
                    c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                c.isControlSupported(FloatControl.Type.VOLUME) ->
                    c.getControl(FloatControl.Type.VOLUME) as FloatControl
                else -> null
            }
            if (gain != null) {
                gain.value = (if (gain.type == FloatControl.Type.VOLUME) volume else db)
                    .coerceIn(gain.minimum, gain.maximum)
            }
        }
    }
}
