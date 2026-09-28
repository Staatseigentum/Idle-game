package com.embercrown.launcher

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import java.util.jar.JarFile

private const val GAME_ENTRY = "com/embercrown/game/MainKt.class"

/** Never write inside a packaged app bundle or Program Files. */
internal fun userAppDirectory(osName: String, home: File, env: Map<String, String>): File = when {
    osName.startsWith("Windows", ignoreCase = true) -> {
        val local = env["LOCALAPPDATA"]?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(home, "AppData/Local")
        File(local, "Embercrown/app")
    }
    osName.startsWith("Mac", ignoreCase = true) -> File(home, "Library/Application Support/Embercrown/app")
    else -> {
        val xdg = env["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isAbsolute }
            ?: File(home, ".local/share")
        File(xdg, "embercrown/app")
    }
}

/** The SHA-256 from GitHub's asset metadata protects against truncated and altered downloads. */
internal fun verifyDownloadedJar(file: File, asset: Asset) {
    require(file.isFile && file.length() > 0L) { "downloaded game jar is empty" }
    require(asset.size > 0L && file.length() == asset.size) {
        "download size mismatch (expected ${asset.size}, got ${file.length()})"
    }
    val digest = asset.digest
    if (!digest.isNullOrBlank()) {
        require(digest.startsWith("sha256:") && digest.length == 71) { "unsupported asset digest" }
        val actual = sha256(file)
        require(actual.equals(digest.removePrefix("sha256:"), ignoreCase = true)) { "SHA-256 mismatch" }
    }
    require(isGameJar(file)) { "download is not an Embercrown game jar" }
}

internal fun isGameJar(file: File): Boolean = file.isFile && runCatching {
    JarFile(file).use { jar ->
        val entry = jar.getJarEntry(GAME_ENTRY) ?: return@use false
        jar.getInputStream(entry).use { input -> input.read() >= 0 }
    }
}.getOrDefault(false)

private fun sha256(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val bytes = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(bytes)
            if (read < 0) break
            md.update(bytes, 0, read)
        }
    }
    val alphabet = "0123456789abcdef"
    return buildString(64) {
        for (byte in md.digest()) {
            val unsigned = byte.toInt() and 0xff
            append(alphabet[unsigned ushr 4])
            append(alphabet[unsigned and 15])
        }
    }
}

internal fun atomicReplace(source: File, target: File) {
    try {
        Files.move(source.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING)
    }
}

internal fun writeInstalledVersion(stateFile: File, version: Version) {
    val temp = File(stateFile.parentFile, "${stateFile.name}.${UUID.randomUUID()}.part")
    try {
        Properties().apply { setProperty("version", version.toString()) }
            .let { properties -> temp.outputStream().use { properties.store(it, "Embercrown installed build") } }
        atomicReplace(temp, stateFile)
    } finally {
        Files.deleteIfExists(temp.toPath())
    }
}

/** Keep the previous working jar until the replacement has launched successfully. */
internal fun installVerifiedJar(download: File, gameJar: File, stateFile: File, asset: Asset,
                                version: Version): Boolean {
    verifyDownloadedJar(download, asset)
    val previousJar = File(gameJar.parentFile, "Embercrown.previous.jar")
    val previousState = File(stateFile.parentFile, "installed.previous.properties")
    val hadWorkingBuild = isGameJar(gameJar)
    if (hadWorkingBuild) {
        Files.copy(gameJar.toPath(), previousJar.toPath(), REPLACE_EXISTING)
        if (stateFile.isFile) Files.copy(stateFile.toPath(), previousState.toPath(), REPLACE_EXISTING)
        else Files.deleteIfExists(previousState.toPath())
    }
    try {
        atomicReplace(download, gameJar)
        writeInstalledVersion(stateFile, version)
    } catch (error: Exception) {
        if (hadWorkingBuild) restorePreviousJar(gameJar, stateFile)
        throw error
    }
    return hadWorkingBuild
}

internal fun restorePreviousJar(gameJar: File, stateFile: File): Boolean {
    val previousJar = File(gameJar.parentFile, "Embercrown.previous.jar")
    if (!isGameJar(previousJar)) return false
    val temp = File(gameJar.parentFile, "Embercrown.restore.${UUID.randomUUID()}.part")
    try {
        Files.copy(previousJar.toPath(), temp.toPath(), REPLACE_EXISTING)
        atomicReplace(temp, gameJar)
        val previousState = File(stateFile.parentFile, "installed.previous.properties")
        if (previousState.isFile) Files.copy(previousState.toPath(), stateFile.toPath(), REPLACE_EXISTING)
        else Files.deleteIfExists(stateFile.toPath())
    } finally {
        Files.deleteIfExists(temp.toPath())
    }
    return true
}
