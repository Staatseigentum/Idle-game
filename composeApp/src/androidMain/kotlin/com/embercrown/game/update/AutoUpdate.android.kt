package com.embercrown.game.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.embercrown.game.game.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and verifies the release APK in the background, then opens Android's installer.
 * Android may first require a one-time "install unknown apps" grant for Embercrown and will
 * present its own installation confirmation. The game never bypasses those system protections.
 */
actual suspend fun applyUpdateAutomatically(update: AvailableUpdate) {
    val asset = update.assets.firstOrNull {
        it.name.contains("android", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
    } ?: return

    val apk = withContext(Dispatchers.IO) {
        val target = File(appContext.cacheDir, "update-${update.version}.apk")
        runCatching {
            downloadTo(asset.downloadUrl, target)
            verifyApk(target, asset)
            target
        }.onFailure {
            target.delete()
            println("[updater] Android update failed: ${it.message}")
        }.getOrNull()
    }
    if (apk != null) runCatching { installOrRequestPermission(apk) }.onFailure {
        apk.delete()
        println("[updater] Android installer failed: ${it.message}")
    }
}

private var pendingApk: File? = null

/** After returning from the system's per-app source-permission screen, continue automatically. */
fun resumePendingUpdateInstall() {
    val apk = pendingApk ?: return
    if (!apk.isFile) {
        pendingApk = null
        return
    }
    if (Build.VERSION.SDK_INT >= 26 && !appContext.packageManager.canRequestPackageInstalls()) return
    pendingApk = null
    runCatching { installApk(apk) }.onFailure {
        apk.delete()
        println("[updater] Android installer failed: ${it.message}")
    }
}

private fun installOrRequestPermission(apk: File) {
    if (Build.VERSION.SDK_INT >= 26 && !appContext.packageManager.canRequestPackageInstalls()) {
        pendingApk = apk
        val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(settings)
        return
    }
    installApk(apk)
}

private fun installApk(apk: File) {
    val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    appContext.startActivity(intent)
}

/** GitHub's asset metadata guards the package installer from a partial or altered download. */
private fun verifyApk(apk: File, asset: UpdateAsset) {
    require(apk.isFile && asset.size > 0 && apk.length() == asset.size) {
        "APK download size mismatch"
    }
    asset.digest?.takeIf { it.isNotBlank() }?.let { expected ->
        require(expected.startsWith("sha256:") && expected.length == 71) { "unsupported APK digest" }
        val hash = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        val hex = "0123456789abcdef"
        val actual = buildString(64) {
            hash.digest().forEach { byte ->
                val value = byte.toInt() and 0xff
                append(hex[value ushr 4])
                append(hex[value and 15])
            }
        }
        require(expected.removePrefix("sha256:").equals(actual, ignoreCase = true)) { "APK SHA-256 mismatch" }
    }
}

/** Downloads [url] to [target], following GitHub's redirect from the API host to its CDN. */
private fun downloadTo(url: String, target: File) {
    var current = url
    repeat(5) {
        val endpoint = URL(current)
        require(endpoint.protocol == "https") { "update URLs must use HTTPS" }
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Embercrown-UpdateCheck")
        }
        try {
            val code = connection.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location") ?: error("redirect without a Location header")
                current = location
                return@repeat
            }
            if (code !in 200..299) error("HTTP $code for $current")
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        } finally {
            connection.disconnect()
        }
    }
    error("too many redirects for $url")
}
