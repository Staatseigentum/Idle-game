package com.embercrown.launcher

import java.io.File
import java.net.URLClassLoader
import java.util.Properties
import kotlin.system.exitProcess

/**
 * Embercrown's desktop bootstrap.
 *
 * On every start it asks GitHub for the newest release, downloads the game jar when it is newer
 * than the installed one, and then launches the game. Anything that goes wrong on the network
 * side is non-fatal: as long as a game jar is already present the launcher still starts it, so a
 * player without internet is never locked out of their save.
 *
 * Layout next to the launcher jar:
 * ```
 * Embercrown-Launcher.jar
 * app/
 *   Embercrown.jar
 *   installed.properties   (version = 0.0.1)
 * ```
 */
private object Launcher

private const val GAME_JAR_NAME = "Embercrown.jar"
private const val STATE_FILE_NAME = "installed.properties"
private const val VERSION_KEY = "version"

fun main(args: Array<String>) {
    val offline = args.contains("--offline")
    val installDir = resolveInstallDir()
    val appDir = File(installDir, "app").apply { mkdirs() }
    val gameJar = File(appDir, GAME_JAR_NAME)
    val stateFile = File(appDir, STATE_FILE_NAME)

    val installed = readInstalledVersion(stateFile)
    log("install dir: ${installDir.absolutePath}")
    log("installed version: ${installed ?: "none"}")

    if (offline) {
        log("--offline given, skipping update check")
    } else {
        runCatching { updateIfNeeded(installed, gameJar, stateFile) }
            .onFailure { log("update check failed (${it.message ?: it::class.simpleName}), continuing with installed build") }
    }

    if (!gameJar.isFile) {
        log("ERROR: no game jar at ${gameJar.absolutePath} and it could not be downloaded.")
        log("Check your connection, or download the jar manually from")
        log("  https://github.com/${LauncherInfo.GITHUB_REPO}/releases/latest")
        exitProcess(1)
    }

    launchGame(gameJar)
    // Reached only if the game returned control without exiting the JVM itself.
    exitProcess(0)
}

/** Downloads and installs a newer release, if GitHub reports one. */
private fun updateIfNeeded(installed: Version?, gameJar: File, stateFile: File) {
    val release = GitHub.latestRelease(LauncherInfo.GITHUB_REPO)
    if (release == null) {
        log("no release information available")
        return
    }
    val latest = Version.parseOrNull(release.tagName)
    if (latest == null) {
        log("release tag '${release.tagName}' is not a version, skipping")
        return
    }
    if (installed != null && latest <= installed && gameJar.isFile) {
        log("up to date ($installed)")
        return
    }

    val asset = release.pickGameJar()
    if (asset == null) {
        log("release $latest has no jar for ${currentOsToken()}, keeping current build")
        return
    }

    log("updating to $latest (${asset.name}, ${asset.size / 1024} KiB)")
    // Download beside the target first so a failed or partial transfer can never replace a
    // working install.
    val temp = File(gameJar.parentFile, "${GAME_JAR_NAME}.part")
    temp.delete()
    GitHub.download(asset.downloadUrl, temp) { done, total ->
        if (total > 0) logProgress(done, total)
    }
    println()

    if (temp.length() <= 0) {
        temp.delete()
        error("downloaded file was empty")
    }
    if (!temp.renameTo(gameJar)) {
        gameJar.delete()
        check(temp.renameTo(gameJar)) { "could not move downloaded jar into place" }
    }
    writeInstalledVersion(stateFile, latest)
    log("updated to $latest")
}

/**
 * Loads the game jar into this JVM and runs it directly, rather than shelling out to a `java`/
 * `javaw` binary.
 *
 * That used to spawn a child process, but a jpackage app image's bundled runtime (under
 * `runtime/`, used once this launcher itself ships as a native installer) deliberately has no
 * standalone `java`/`javaw` executable to spawn — jpackage strips it, since the image is meant
 * to be entered only through the app's own native launcher exe. Class-loading the jar in-process
 * sidesteps that entirely and works identically for the plain `java -jar Embercrown-Launcher.jar`
 * fallback. The game's own `main()` blocks until its window closes (Compose Desktop's
 * `application {}` runs the event loop on the calling thread), so this returns only once the
 * player has quit.
 */
private fun launchGame(gameJar: File) {
    // When this launcher itself runs from a jpackage app image, jpackage injects
    // -Dskiko.library.path and -Dcompose.application.resources.dir pointing at *this app's own*
    // directory (it has no idea a second Compose app is about to be loaded into the same JVM).
    // Left in place, the game's Skiko/resource loading would look for its native library and
    // bundled resources next to the launcher's jars instead of inside its own uber jar, and fail.
    // Clearing them makes the game fall back to its normal standalone behavior: extracting Skiko
    // from its own jar and reading its own bundled compose resources.
    System.clearProperty("skiko.library.path")
    System.clearProperty("compose.application.resources.dir")

    // A platform-only parent isolates the game's bundled kotlin-stdlib/kotlinx-serialization
    // from this launcher's own copies on the classpath, avoiding a version clash between them.
    val classLoader = URLClassLoader(arrayOf(gameJar.toURI().toURL()), ClassLoader.getPlatformClassLoader())
    val mainClass = Class.forName("com.embercrown.game.MainKt", true, classLoader)
    val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
    log("starting ${gameJar.name}")
    Thread.currentThread().contextClassLoader = classLoader
    try {
        mainMethod.invoke(null, arrayOf<String>())
    } catch (e: java.lang.reflect.InvocationTargetException) {
        throw e.cause ?: e
    }
}

/** The directory the launcher jar itself lives in, falling back to the working directory. */
private fun resolveInstallDir(): File {
    val fromJar = runCatching {
        val source = Launcher::class.java.protectionDomain?.codeSource?.location ?: return@runCatching null
        val file = File(source.toURI())
        if (file.isFile) file.parentFile else file
    }.getOrNull()
    return fromJar ?: File(System.getProperty("user.dir"))
}

private fun readInstalledVersion(stateFile: File): Version? {
    if (!stateFile.isFile) return null
    return runCatching {
        val properties = Properties()
        stateFile.inputStream().use { properties.load(it) }
        properties.getProperty(VERSION_KEY)?.let { Version.parseOrNull(it) }
    }.getOrNull()
}

private fun writeInstalledVersion(stateFile: File, version: Version) {
    runCatching {
        val properties = Properties()
        properties.setProperty(VERSION_KEY, version.toString())
        stateFile.outputStream().use { properties.store(it, "Embercrown installed build") }
    }
}

internal fun log(message: String) = println("[launcher] $message")

private var lastPercent = -1

private fun logProgress(done: Long, total: Long) {
    val percent = ((done * 100) / total).toInt()
    if (percent == lastPercent) return
    lastPercent = percent
    print("\r[launcher] downloading $percent%")
    System.out.flush()
}
