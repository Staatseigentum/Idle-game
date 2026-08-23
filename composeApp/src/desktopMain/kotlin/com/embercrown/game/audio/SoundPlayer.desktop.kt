package com.embercrown.game.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

private const val POOL_SIZE = 3

actual class SoundPlayer actual constructor() {
    private class Pool(val clips: List<Clip>) {
        var next = 0
    }

    private val pools = mutableMapOf<SfxId, Pool>()
    private var volume: Float = 0.7f
    private var muted: Boolean = false

    actual fun preload(clips: Map<SfxId, PcmClip>) {
        clips.forEach { (id, clip) ->
            val format = AudioFormat(clip.sampleRateHz.toFloat(), 16, 1, true, false)
            val bytes = ByteArray(clip.samples.size * 2)
            for (i in clip.samples.indices) {
                val v = clip.samples[i].toInt()
                bytes[i * 2] = (v and 0xFF).toByte()
                bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            val instances = (0 until POOL_SIZE).mapNotNull {
                runCatching {
                    val line = AudioSystem.getClip()
                    line.open(format, bytes, 0, bytes.size)
                    line
                }.getOrNull()
            }
            if (instances.isNotEmpty()) {
                pools[id] = Pool(instances)
                applyVolume(instances)
            }
        }
    }

    actual fun play(id: SfxId) {
        if (muted) return
        val pool = pools[id] ?: return
        val clip = pool.clips[pool.next]
        pool.next = (pool.next + 1) % pool.clips.size
        clip.stop()
        clip.framePosition = 0
        clip.start()
    }

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        pools.values.forEach { applyVolume(it.clips) }
    }

    actual fun setMuted(muted: Boolean) {
        this.muted = muted
    }

    private fun applyVolume(clips: List<Clip>) {
        val db = if (volume <= 0f) -80f else (20.0 * log10(volume.toDouble())).toFloat()
        clips.forEach { clip ->
            runCatching {
                val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                control.value = db.coerceIn(control.minimum, control.maximum)
            }
        }
    }
}
