package com.embercrown.game.update

// The desktop launcher (`:launcher` module) already downloaded and swapped in a newer game jar
// before this process started, so whatever is running here is already current.
actual suspend fun applyUpdateAutomatically(update: AvailableUpdate) = Unit
