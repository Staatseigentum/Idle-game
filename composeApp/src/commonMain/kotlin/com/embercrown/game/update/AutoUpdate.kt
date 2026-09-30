package com.embercrown.game.update

/**
 * Starts installing [update] immediately, subject to Android's required system approval. A no-op wherever
 * "immediately" isn't possible or meaningful: desktop already runs the latest build by the time
 * this executes, because the launcher downloads and swaps in a newer jar *before* the game
 * process even starts (see the `:launcher` module), and iOS cannot self-update at all.
 */
expect suspend fun applyUpdateAutomatically(update: AvailableUpdate)

/** Checks GitHub once and, if a newer release exists, applies it without asking. */
suspend fun autoUpdateIfNeeded() {
    val update = checkForUpdate() ?: return
    applyUpdateAutomatically(update)
}
