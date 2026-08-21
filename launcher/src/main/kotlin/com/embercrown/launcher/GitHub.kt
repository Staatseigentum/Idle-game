package com.embercrown.launcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
internal data class Release(
    @SerialName("tag_name") val tagName: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<Asset> = emptyList(),
) {
    /**
     * Picks the game jar matching [osToken] and never the launcher's own jar.
     *
     * Compose Desktop uber jars embed the Skiko native library for the OS they were built on, so
     * there is one jar per platform and grabbing the wrong one yields an app that cannot start.
     * If nothing matches this OS we return null rather than installing an incompatible build.
     */
    fun pickGameJar(osToken: String = currentOsToken()): Asset? {
        val jars = assets.filter { it.name.endsWith(".jar", ignoreCase = true) }
            .filterNot { it.name.contains("launcher", ignoreCase = true) }
        return jars.firstOrNull { it.name.contains(osToken, ignoreCase = true) }
    }
}

/** The token embedded in release asset names for the platform this launcher runs on. */
internal fun currentOsToken(): String {
    val os = System.getProperty("os.name").orEmpty()
    return when {
        os.startsWith("Windows", ignoreCase = true) -> "windows"
        os.startsWith("Mac", ignoreCase = true) -> "macos"
        else -> "linux"
    }
}

@Serializable
internal data class Asset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

internal object GitHub {
    private val json = Json { ignoreUnknownKeys = true }

    fun latestRelease(repo: String): Release? {
        val body = get("https://api.github.com/repos/$repo/releases/latest") ?: return null
        val release = runCatching { json.decodeFromString<Release>(body) }
            .onFailure { log("could not read the release response: ${it.message}") }
            .getOrNull() ?: return null
        if (release.draft || release.prerelease) {
            log("newest release ${release.tagName} is a draft/prerelease, ignoring")
            return null
        }
        return release
    }

    fun download(url: String, target: File, onProgress: (done: Long, total: Long) -> Unit) {
        open(url).use { connection ->
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
            }
        }
    }

    private fun get(url: String): String? = runCatching {
        open(url).use { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
    }.onFailure {
        // Surfaced rather than swallowed: "no release information" on its own leaves a player
        // with no idea whether they are offline, rate limited or hitting a wrong repo name.
        log("could not reach GitHub: ${it.message ?: it::class.simpleName}")
    }.getOrNull()

    /** Opens a connection, following the redirect GitHub uses for asset downloads. */
    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(5) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Embercrown-Launcher")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = connection.responseCode
            // HttpURLConnection will not follow a redirect that changes host, which is exactly
            // what release downloads do (api.github.com -> objects.githubusercontent.com).
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) error("redirect without a Location header")
                current = location
                return@repeat
            }
            if (code !in 200..299) {
                connection.disconnect()
                error("HTTP $code for $current")
            }
            return connection
        }
        error("too many redirects for $url")
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }
}
