package com.embercrown.game.update

/**
 * iOS deliberately never reports an update.
 *
 * Apps distributed through the App Store may not update themselves, and a GitHub release
 * asset cannot be installed on a stock device, so surfacing an update banner there would
 * only offer the player something they cannot act on. Updates arrive via the App Store.
 */
actual suspend fun httpGetOrNull(url: String): String? = null
