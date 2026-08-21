package com.embercrown.game

import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Writes a Windows `.ico` using PNG-compressed entries — supported since Vista, so there's no
 * need to also encode legacy raw BMP/DIB frames.
 */
fun writeIco(images: List<Pair<Int, ByteArray>>, file: File) {
    val sorted = images.sortedBy { it.first }
    val headerSize = 6 + sorted.size * 16
    var offset = headerSize
    val offsets = sorted.map { (_, png) -> val o = offset; offset += png.size; o }

    val out = ByteArrayOutputStream()
    fun u16(v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }
    fun u32(v: Int) {
        repeat(4) { i -> out.write((v shr (8 * i)) and 0xFF) }
    }

    u16(0) // reserved
    u16(1) // type: icon
    u16(sorted.size)
    sorted.forEachIndexed { i, (size, png) ->
        out.write(if (size >= 256) 0 else size) // width, 0 means 256
        out.write(if (size >= 256) 0 else size) // height, 0 means 256
        out.write(0) // color count
        out.write(0) // reserved
        u16(1) // planes
        u16(32) // bit count
        u32(png.size)
        u32(offsets[i])
    }
    sorted.forEach { (_, png) -> out.write(png) }
    file.writeBytes(out.toByteArray())
}

/**
 * Writes a macOS `.icns` from PNG-compressed entries — the same OSType chunk layout that
 * `iconutil -c icns` produces, so no macOS-only tool is needed to build it.
 */
fun writeIcns(entries: List<Pair<String, ByteArray>>, file: File) {
    fun beU32(out: ByteArrayOutputStream, v: Int) {
        out.write((v shr 24) and 0xFF)
        out.write((v shr 16) and 0xFF)
        out.write((v shr 8) and 0xFF)
        out.write(v and 0xFF)
    }

    val body = ByteArrayOutputStream()
    for ((type, png) in entries) {
        body.write(type.toByteArray(Charsets.US_ASCII))
        beU32(body, 8 + png.size)
        body.write(png)
    }

    val out = ByteArrayOutputStream()
    out.write("icns".toByteArray(Charsets.US_ASCII))
    beU32(out, 8 + body.size())
    out.write(body.toByteArray())
    file.writeBytes(out.toByteArray())
}
