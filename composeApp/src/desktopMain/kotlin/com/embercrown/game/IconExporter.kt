package com.embercrown.game

import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.appIcon
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Dev-only harness: rasterizes `appIcon()` into the icon files `nativeDistributions` points at
 * (`icon.ico`, `icon.icns`, `icon.png`). Nothing rasterized is checked into git — these are
 * regenerated fresh on every build, same as the rest of the pixel-art layer. Run with:
 * `./gradlew :composeApp:generateIcons --args="<output-dir>"`.
 */
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/generated/icons").apply { mkdirs() }
    val art = appIcon()

    val pngCache = mutableMapOf<Int, ByteArray>()
    fun pngFor(size: Int): ByteArray = pngCache.getOrPut(size) { renderToSize(art, size).toPngBytes() }

    writeIco(listOf(16, 32, 48, 256).map { it to pngFor(it) }, File(outDir, "icon.ico"))

    writeIcns(
        listOf(
            "ic11" to pngFor(32), // 16pt@2x
            "ic12" to pngFor(64), // 32pt@2x
            "ic07" to pngFor(128),
            "ic13" to pngFor(256), // 128pt@2x
            "ic08" to pngFor(256),
            "ic14" to pngFor(512), // 256pt@2x
            "ic09" to pngFor(512),
            "ic10" to pngFor(1024), // 512pt@2x
        ),
        File(outDir, "icon.icns"),
    )

    File(outDir, "icon.png").writeBytes(pngFor(512))

    println("Wrote app icon files to ${outDir.absolutePath}")
}

/** Nearest-neighbor rasterization of [art] to an exact [size]x[size] bitmap, up- or downscaling. */
internal fun renderToSize(art: PixelArt, size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until size) {
        val sy = (y * art.height / size).coerceIn(0, art.height - 1)
        for (x in 0 until size) {
            val sx = (x * art.width / size).coerceIn(0, art.width - 1)
            val color = art.colorAt(sx, sy) ?: continue
            val argb = (color.alpha * 255).toInt().shl(24) or
                (color.red * 255).toInt().shl(16) or
                (color.green * 255).toInt().shl(8) or
                (color.blue * 255).toInt()
            image.setRGB(x, y, argb)
        }
    }
    return image
}

internal fun BufferedImage.toPngBytes(): ByteArray {
    val out = ByteArrayOutputStream()
    ImageIO.write(this, "png", out)
    return out.toByteArray()
}
