package com.embercrown.game.update

import android.content.Intent
import android.net.Uri
import com.embercrown.game.game.appContext

actual fun openUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Started from an application context, so it needs its own task.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }
}
