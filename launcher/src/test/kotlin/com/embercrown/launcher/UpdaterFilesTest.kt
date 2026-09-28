package com.embercrown.launcher

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class UpdaterFilesTest {
    @Test
    fun `cache path follows each platform convention`() {
        val root = File(System.getProperty("java.io.tmpdir")).absoluteFile
        val home = File(root, "players/ember")
        val local = File(root, "local")
        val xdg = File(root, "xdg")
        assertEquals(File(local, "Embercrown/app"),
            userAppDirectory("Windows 11", home, mapOf("LOCALAPPDATA" to local.absolutePath)))
        assertEquals(File(home, "Library/Application Support/Embercrown/app"),
            userAppDirectory("Mac OS X", home, emptyMap()))
        assertEquals(File(xdg, "embercrown/app"),
            userAppDirectory("Linux", home, mapOf("XDG_DATA_HOME" to xdg.absolutePath)))
        assertEquals(File(home, ".local/share/embercrown/app"),
            userAppDirectory("Linux", home, mapOf("XDG_DATA_HOME" to "relative")))
    }

    @Test
    fun `verified update backs up installed game and can roll back`() = inTempDirectory { dir ->
        val game = File(dir, "Embercrown.jar")
        val state = File(dir, "installed.properties")
        makeGameJar(game, 1)
        writeInstalledVersion(state, Version.parseOrNull("0.3.0")!!)
        val download = File(dir, "update.part")
        makeGameJar(download, 2)
        val asset = assetFor(download)

        assertTrue(installVerifiedJar(download, game, state, asset, Version.parseOrNull("0.4.0")!!))
        assertTrue(isGameJar(game))
        assertEquals(2, gameJarMarker(game))
        assertTrue(state.readText().contains("0.4.0"))

        assertTrue(restorePreviousJar(game, state))
        assertEquals(1, gameJarMarker(game))
        assertTrue(state.readText().contains("0.3.0"))
    }

    @Test
    fun `invalid download cannot replace installed game`() = inTempDirectory { dir ->
        val game = File(dir, "Embercrown.jar")
        val state = File(dir, "installed.properties")
        makeGameJar(game, 1)
        writeInstalledVersion(state, Version.parseOrNull("0.3.0")!!)
        val download = File(dir, "update.part")
        makeGameJar(download, 2)
        val badDigest = assetFor(download).copy(digest = "sha256:${"0".repeat(64)}")

        assertFailsWith<IllegalArgumentException> {
            installVerifiedJar(download, game, state, badDigest, Version.parseOrNull("0.4.0")!!)
        }
        assertEquals(1, gameJarMarker(game))
        assertTrue(state.readText().contains("0.3.0"))
        assertFalse(File(dir, "Embercrown.previous.jar").exists())
    }

    private fun assetFor(file: File): Asset {
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        return Asset("Embercrown-windows.jar", "https://example.test/download", file.length(), "sha256:$hash")
    }

    private fun makeGameJar(file: File, marker: Int) {
        JarOutputStream(file.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry("com/embercrown/game/MainKt.class"))
            jar.write(marker)
            jar.closeEntry()
        }
    }

    private fun gameJarMarker(file: File): Int = java.util.jar.JarFile(file).use { jar ->
        jar.getInputStream(jar.getJarEntry("com/embercrown/game/MainKt.class")).use { it.read() }
    }

    private fun inTempDirectory(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("embercrown-updater-test").toFile()
        try { block(dir) } finally { dir.deleteRecursively() }
    }
}
