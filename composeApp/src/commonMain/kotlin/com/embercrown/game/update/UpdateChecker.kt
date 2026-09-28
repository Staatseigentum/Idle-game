package com.embercrown.game.update

import com.embercrown.game.BuildInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Performs a plain HTTP GET, returning the body or `null` on any failure. */
expect suspend fun httpGetOrNull(url: String): String?

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0,
    val digest: String? = null,
)

/** A release newer than the running build, with the assets a user could install. */
data class AvailableUpdate(
    val version: Version,
    val displayName: String,
    val releaseUrl: String,
    val assets: List<UpdateAsset>,
)

data class UpdateAsset(val name: String, val downloadUrl: String, val size: Long,
                       val digest: String? = null)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Asks GitHub for the newest published release of [repo] and reports it only when it is
 * strictly newer than [currentVersion]. Every failure path — no network, rate limiting,
 * malformed payload, unparseable tag — returns `null`, because a broken update check must
 * never interrupt play.
 */
suspend fun checkForUpdate(
    repo: String = BuildInfo.GITHUB_REPO,
    currentVersion: String = BuildInfo.VERSION,
): AvailableUpdate? {
    val current = Version.parseOrNull(currentVersion) ?: return null
    val body = httpGetOrNull("https://api.github.com/repos/$repo/releases/latest") ?: return null

    val release = runCatching { json.decodeFromString<GithubRelease>(body) }.getOrNull() ?: return null
    if (release.draft || release.prerelease) return null

    val latest = Version.parseOrNull(release.tagName) ?: return null
    if (latest <= current) return null

    return AvailableUpdate(
        version = latest,
        displayName = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
        releaseUrl = release.htmlUrl,
        assets = release.assets.map { UpdateAsset(it.name, it.browserDownloadUrl, it.size, it.digest) },
    )
}
