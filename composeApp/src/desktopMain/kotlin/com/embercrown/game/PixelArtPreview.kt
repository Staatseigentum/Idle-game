package com.embercrown.game

import com.embercrown.game.game.AgeDefinition
import com.embercrown.game.game.BuildingDefinition
import com.embercrown.game.reboot.RebootBuildings
import com.embercrown.game.reboot.ashBuildingIcon
import com.embercrown.game.reboot.ashKingdomArt
import com.embercrown.game.ui.pixelart.PixelArt
import com.embercrown.game.ui.pixelart.ageSceneArt
import com.embercrown.game.ui.pixelart.buildingIcon
import com.embercrown.game.ui.pixelart.chroniclePointsIcon
import com.embercrown.game.ui.pixelart.goldIcon
import com.embercrown.game.ui.pixelart.sagenIcon
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Dev-only harness: renders every sprite to PNG contact sheets so the art can be reviewed
 * without playing far enough into the game to unlock it. Run with:
 * `./gradlew :composeApp:renderPixelArt --args="<output-dir>"`.
 */
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/pixel-art-preview").apply { mkdirs() }

    val scale = 6
    writeSheet(
        file = File(outDir, "icons.png"),
        tiles = BuildingDefinition.all.mapNotNull { buildingIcon(it.id) } +
            listOf(goldIcon(), chroniclePointsIcon(), sagenIcon()),
        columns = 5,
        scale = scale,
        gap = 4,
    )

    AgeDefinition.all.indices.step(3).forEach { index ->
        writeArt(File(outDir, "age_$index.png"), ageSceneArt(index, AgeDefinition.all.size), scale = 5)
    }
    writeArt(
        File(outDir, "age_last.png"),
        ageSceneArt(AgeDefinition.all.lastIndex, AgeDefinition.all.size),
        scale = 5,
    )

    writeSheet(
        file = File(outDir, "reboot_buildings.png"),
        tiles = RebootBuildings.all.map { ashBuildingIcon(it.id, frame = 3) },
        columns = 4,
        scale = 6,
        gap = 4,
    )
    writeArt(File(outDir, "reboot_beginning.png"), ashKingdomArt(emptyMap(), 10, false), scale = 5)
    writeArt(
        File(outDir, "reboot_complete.png"),
        ashKingdomArt(RebootBuildings.all.associate { it.id to 1 }, 70, true,
            RebootBuildings.all.associate { it.id to 2 },
            marchRanks = RebootBuildings.all.associate { it.id to 2 }),
        scale = 5,
    )

    println("Wrote pixel-art preview to ${outDir.absolutePath}")
}

private fun writeArt(file: File, art: PixelArt, scale: Int) {
    val image = BufferedImage(art.width * scale, art.height * scale, BufferedImage.TYPE_INT_ARGB)
    blit(image, art, 0, 0, scale)
    ImageIO.write(image, "png", file)
}

private fun writeSheet(file: File, tiles: List<PixelArt>, columns: Int, scale: Int, gap: Int) {
    if (tiles.isEmpty()) return
    val tileW = tiles.maxOf { it.width }
    val tileH = tiles.maxOf { it.height }
    val rows = (tiles.size + columns - 1) / columns
    val image = BufferedImage(
        columns * (tileW * scale + gap) + gap,
        rows * (tileH * scale + gap) + gap,
        BufferedImage.TYPE_INT_ARGB,
    )
    tiles.forEachIndexed { i, art ->
        val col = i % columns
        val row = i / columns
        blit(image, art, gap + col * (tileW * scale + gap), gap + row * (tileH * scale + gap), scale)
    }
    ImageIO.write(image, "png", file)
}

/** Nearest-neighbour copy of one [PixelArt] into [image], compositing over what is already there. */
private fun blit(image: BufferedImage, art: PixelArt, offsetX: Int, offsetY: Int, scale: Int) {
    for (y in 0 until art.height) for (x in 0 until art.width) {
        val color = art.colorAt(x, y) ?: continue
        val argb = (color.alpha * 255).toInt().shl(24) or
            (color.red * 255).toInt().shl(16) or
            (color.green * 255).toInt().shl(8) or
            (color.blue * 255).toInt()
        for (dy in 0 until scale) for (dx in 0 until scale) {
            val px = offsetX + x * scale + dx
            val py = offsetY + y * scale + dy
            if (px < image.width && py < image.height) image.setRGB(px, py, argb)
        }
    }
}
