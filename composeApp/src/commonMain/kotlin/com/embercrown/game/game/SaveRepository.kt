package com.embercrown.game.game

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SaveRepository(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): GameState {
        val raw: String? = settings[KEY]
        if (raw.isNullOrBlank()) return GameState()
        return runCatching { json.decodeFromString(GameState.serializer(), raw) }
            .getOrElse { GameState() }
    }

    fun save(state: GameState) {
        settings[KEY] = json.encodeToString(state)
    }

    fun serialize(state: GameState): String = json.encodeToString(state)

    fun deserialize(raw: String): GameState? =
        runCatching { json.decodeFromString(GameState.serializer(), raw) }.getOrNull()

    companion object {
        private const val KEY = "embercrown_game_state_v1"
    }
}
