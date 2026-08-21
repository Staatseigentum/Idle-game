package com.embercrown.game.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppGraph {
    val engine: GameEngine by lazy {
        GameEngine(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            repository = SaveRepository(createSettings()),
        )
    }
}
