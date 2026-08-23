package com.embercrown.game.audio

/** Mono 16-bit PCM samples at [sampleRateHz]. */
data class PcmClip(val samples: ShortArray, val sampleRateHz: Int = 44100)

/** Wraps [PcmClip.samples] in a standard 44-byte RIFF/WAVE header (mono, 16-bit PCM, little-endian). */
fun PcmClip.toWavBytes(): ByteArray {
    val dataSize = samples.size * 2
    val bytes = ByteArray(44 + dataSize)

    fun writeAscii(offset: Int, text: String) {
        for (i in text.indices) bytes[offset + i] = text[i].code.toByte()
    }

    fun writeIntLE(offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    fun writeShortLE(offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    val byteRate = sampleRateHz * 2
    writeAscii(0, "RIFF")
    writeIntLE(4, 36 + dataSize)
    writeAscii(8, "WAVE")
    writeAscii(12, "fmt ")
    writeIntLE(16, 16) // fmt chunk size
    writeShortLE(20, 1) // PCM
    writeShortLE(22, 1) // mono
    writeIntLE(24, sampleRateHz)
    writeIntLE(28, byteRate)
    writeShortLE(32, 2) // block align (1 channel * 16 bit / 8)
    writeShortLE(34, 16) // bits per sample
    writeAscii(36, "data")
    writeIntLE(40, dataSize)

    for (i in samples.indices) {
        val sample = samples[i].toInt()
        val offset = 44 + i * 2
        bytes[offset] = (sample and 0xFF).toByte()
        bytes[offset + 1] = ((sample shr 8) and 0xFF).toByte()
    }
    return bytes
}
