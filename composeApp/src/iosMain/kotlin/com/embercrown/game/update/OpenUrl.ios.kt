package com.embercrown.game.update

/**
 * No-op on iOS: [httpGetOrNull] never reports an update there, so nothing ever asks to open a
 * release page. Updates arrive through the App Store.
 */
actual fun openUrl(url: String) = Unit
