package com.embercrown.game.update

// checkForUpdate() already short-circuits to null on iOS (see HttpGet.ios.kt), so this is
// unreachable in practice. Kept as a no-op only because `expect`/`actual` requires one.
actual suspend fun applyUpdateAutomatically(update: AvailableUpdate) = Unit
