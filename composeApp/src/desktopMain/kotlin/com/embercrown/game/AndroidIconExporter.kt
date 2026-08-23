package com.embercrown.game

import com.embercrown.game.ui.pixelart.appIcon
import java.io.File

/**
 * Dev-only harness: rasterizes `appIcon()` into Android launcher icon PNGs at every mipmap
 * density. Output lands in a `res/mipmap-*` tree that's wired in as an extra Android resource
 * source dir (see `generateAndroidIcons` in build.gradle.kts) — nothing here is checked into
 * git, same as the desktop icons from `IconExporter`. Run with:
 * `./gradlew :composeApp:generateAndroidIcons --args="<output-dir>"`.
 */
fun main(args: Array<String>) {
    val resDir = File(args.firstOrNull() ?: "build/generated/androidRes")
    val art = appIcon()

    val densities = mapOf(
        "mipmap-mdpi" to 48,
        "mipmap-hdpi" to 72,
        "mipmap-xhdpi" to 96,
        "mipmap-xxhdpi" to 144,
        "mipmap-xxxhdpi" to 192,
    )
    for ((folder, size) in densities) {
        val dir = File(resDir, folder).apply { mkdirs() }
        val png = renderToSize(art, size).toPngBytes()
        File(dir, "ic_launcher.png").writeBytes(png)
        File(dir, "ic_launcher_round.png").writeBytes(png)
    }

    println("Wrote Android launcher icons to ${resDir.absolutePath}")
}
