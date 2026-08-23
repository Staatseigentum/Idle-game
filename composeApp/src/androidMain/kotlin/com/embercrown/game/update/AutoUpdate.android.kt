package com.embercrown.game.update

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.embercrown.game.game.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the release APK and fires Android's own package installer on it, with no dialog of
 * our own in between. Android still shows its own "Install/Update this app?" system prompt for a
 * sideloaded APK — no app can suppress that without root/device-owner privileges — but nothing
 * on our side asks first.
 */
actual suspend fun applyUpdateAutomatically(update: AvailableUpdate) {
    val asset = update.assets.firstOrNull {
        it.name.contains("android", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
    } ?: return

    withContext(Dispatchers.IO) {
        runCatching {
            val target = File(appContext.cacheDir, "update.apk")
            downloadTo(asset.downloadUrl, target)
            installApk(target)
        }
    }
}

private fun installApk(apk: File) {
    val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    appContext.startActivity(intent)
}

/** Downloads [url] to [target], following GitHub's redirect from the API host to its CDN. */
private fun downloadTo(url: String, target: File) {
    var current = url
    repeat(5) {
        val connection = (URL(current).openConnection() as HttpURLConnection).apply {
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
